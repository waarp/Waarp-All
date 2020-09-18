/*
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 * Waarp . If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.protocol.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.traffic.TrafficCounter;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.exception.FileTransferException;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.gateway.kernel.http.HttpWriteCacheEnable;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.SpooledInformTask;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.database.DBTransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.database.data.DbTaskRunner.TASKSTEP;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.pojo.UpdatedInfo;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66Exception;
import org.waarp.openr66.protocol.exception.OpenR66ExceptionTrappedFactory;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessNoWriteBackException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Handler for HTTP information support
 */
public class HttpFormattedHandler
    extends SimpleChannelInboundHandler<FullHttpRequest> {
  private static final String OPEN_R66_WEB_ERROR = "OpenR66 Web Error {}";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpFormattedHandler.class);

  private static final String MONITORING_HEADER_HTML = "monitoring_header.html";
  private static final String MONITORING_END_HTML = "monitoring_end.html";

  private enum REQUEST {
    index("index.html"), active(MONITORING_HEADER_HTML, MONITORING_END_HTML),
    error(MONITORING_HEADER_HTML, MONITORING_END_HTML),
    done(MONITORING_HEADER_HTML, MONITORING_END_HTML),
    all(MONITORING_HEADER_HTML, MONITORING_END_HTML),
    status(MONITORING_HEADER_HTML, MONITORING_END_HTML), statusxml(""),
    statusjson("");

    private static final String MONITOR = "monitor/";
    private final String header;
    private final String end;

    /**
     * Constructor for a unique file
     *
     * @param uniquefile
     */
    REQUEST(final String uniquefile) {
      header = uniquefile;
      end = uniquefile;
    }

    /**
     * @param header
     * @param end
     */
    REQUEST(final String header, final String end) {
      this.header = header;
      this.end = end;
    }

    /**
     * Reader for a unique file
     *
     * @return the content of the unique file
     */
    public String readFileUnique(final HttpFormattedHandler handler) {
      return handler.readFileHeader(
          Configuration.configuration.getHttpBasePath() + MONITOR + header);
    }

    public String readHeader(final HttpFormattedHandler handler) {
      return handler.readFileHeader(
          Configuration.configuration.getHttpBasePath() + MONITOR + header);
    }

    public String readEnd() {
      return WaarpStringUtils.readFile(
          Configuration.configuration.getHttpBasePath() + MONITOR + end);
    }
  }

  protected enum REPLACEMENT {
    XXXHOSTIDXXX, XXXLOCACTIVEXXX, XXXNETACTIVEXXX, XXXBANDWIDTHXXX, XXXDATEXXX,
    XXXLANGXXX
  }

  public static final int LIMITROW = 60; // better if it can be divided by 4
  private static final String I18NEXT = "i18next";

  private final R66Session authentHttp = new R66Session();

  protected String lang = Messages.getSlocale();

  protected FullHttpRequest request;

  protected final StringBuilder responseContent = new StringBuilder();

  protected HttpResponseStatus status;

  protected String uriRequest;

  private static final String sINFO = "INFO";
  private static final String sNB = "NB";
  private static final String sDETAIL = "DETAIL";

  protected boolean isCurrentRequestXml;
  protected boolean isCurrentRequestJson;

  protected Map<String, List<String>> params;

  private String readFileHeader(final String filename) {
    final String value;
    try {
      value = WaarpStringUtils.readFileException(filename);
    } catch (final InvalidArgumentException e) {
      logger.error("Error while trying to open: " + filename, e);
      return "";
    } catch (final FileTransferException e) {
      logger.error("Error while trying to read: " + filename, e);
      return "";
    }
    final StringBuilder builder = new StringBuilder(value);

    WaarpStringUtils.replace(builder, REPLACEMENT.XXXDATEXXX.toString(),
                             new Date().toString());
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXLOCACTIVEXXX.toString(),
                             Integer.toString(Configuration.configuration
                                                  .getLocalTransaction()
                                                  .getNumberLocalChannel()));
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXNETACTIVEXXX.toString(),
                             Integer.toString(DbAdmin.getNbConnection()));
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXHOSTIDXXX.toString(),
                             Configuration.configuration.getHostId());
    final TrafficCounter trafficCounter =
        Configuration.configuration.getGlobalTrafficShapingHandler()
                                   .trafficCounter();
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXBANDWIDTHXXX.toString(),
                             "IN:" +
                             trafficCounter.lastReadThroughput() / 131072 +
                             "Mbits&nbsp;&nbsp;OUT:" +
                             trafficCounter.lastWriteThroughput() / 131072 +
                             "Mbits");
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXLANGXXX.toString(), lang);
    return builder.toString();
  }

  protected String getTrimValue(final String varname) {
    String value;
    try {
      value = params.get(varname).get(0).trim();
    } catch (final NullPointerException e) {
      return null;
    }
    if (value.isEmpty()) {
      value = null;
    }
    return value;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx,
                              final FullHttpRequest msg) throws Exception {
    isCurrentRequestXml = false;
    isCurrentRequestJson = false;
    status = HttpResponseStatus.OK;
    final FullHttpRequest httpRequest = this.request = msg;
    QueryStringDecoder queryStringDecoder =
        new QueryStringDecoder(httpRequest.uri());
    uriRequest = queryStringDecoder.path();
    logger.debug("Msg: {}", uriRequest);
    if (uriRequest.contains("gre/") || uriRequest.contains("img/") ||
        uriRequest.contains("res/") || uriRequest.contains("favicon.ico")) {
      HttpWriteCacheEnable.writeFile(httpRequest, ctx,
                                     Configuration.configuration
                                         .getHttpBasePath() + uriRequest,
                                     "XYZR66NOSESSION");
      return;
    }

    char cval = 'z';
    long nb = LIMITROW;
    // check the URI
    if ("/active".equalsIgnoreCase(uriRequest)) {
      cval = '0';
    } else if ("/error".equalsIgnoreCase(uriRequest)) {
      cval = '1';
    } else if ("/done".equalsIgnoreCase(uriRequest)) {
      cval = '2';
    } else if ("/all".equalsIgnoreCase(uriRequest)) {
      cval = '3';
    } else if ("/status".equalsIgnoreCase(uriRequest)) {
      cval = '4';
    } else if ("/statusxml".equalsIgnoreCase(uriRequest)) {
      cval = '5';
      nb = 0; // since it could be the default or setup by request
      isCurrentRequestXml = true;
    } else if (uriRequest.toLowerCase().startsWith("/spooled")) {
      cval = '6';
    } else if ("/statusjson".equalsIgnoreCase(uriRequest)) {
      cval = '7';
      nb = 0; // since it could be the default or setup by request
      isCurrentRequestJson = true;
    }
    // Get the params according to get or post
    if (httpRequest.method() == HttpMethod.GET) {
      params = queryStringDecoder.parameters();
    } else if (httpRequest.method() == HttpMethod.POST) {
      final ByteBuf content = httpRequest.content();
      if (content.isReadable()) {
        final String param = content.toString(WaarpStringUtils.UTF8);
        queryStringDecoder = new QueryStringDecoder("/?" + param);
      } else {
        responseContent.append(REQUEST.index.readFileUnique(this));
        writeResponse(ctx);
        return;
      }
      params = queryStringDecoder.parameters();
      boolean invalidEntry = false;
      for (final Entry<String, List<String>> paramCheck : params.entrySet()) {
        try {
          ParametersChecker.checkSanityString(paramCheck.getValue().toArray(
              ParametersChecker.ZERO_ARRAY_STRING));
        } catch (final InvalidArgumentException e) {
          logger.error(
              "Arguments incompatible with Security: " + paramCheck.getKey(),
              e);
          invalidEntry = true;
        }
      }
      if (invalidEntry) {
        for (final Entry<String, List<String>> paramCheck : params.entrySet()) {
          paramCheck.getValue().clear();
        }
        params.clear();
        logger.error("No parameter validated since security issue found");
      }
    }
    boolean getMenu = cval == 'z';
    boolean extraBoolean = false;
    if (!params.isEmpty()) {
      // if not uri, from get or post
      if (getMenu) {
        final String info = getTrimValue(sINFO);
        if (info != null) {
          getMenu = false;
          cval = info.charAt(0);
        } else {
          getMenu = true;
        }
      }
      // search the nb param
      final String snb = getTrimValue(sNB);
      if (snb != null) {
        try {
          nb = Long.parseLong(snb);
        } catch (final Exception ignored) {
          SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
        }
      }
      // search the detail param
      final String sdetail = getTrimValue(sDETAIL);
      if (sdetail != null) {
        try {
          if (Integer.parseInt(sdetail) > 0) {
            extraBoolean = true;
          }
        } catch (final Exception ignored) {
          SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
        }
      }
      final String langarg = getTrimValue("setLng");
      if (langarg != null && !langarg.isEmpty()) {
        lang = langarg;
      }
    }
    if (getMenu) {
      responseContent.append(REQUEST.index.readFileUnique(this));
    } else {
      // Use value 0=Active 1=Error 2=Done 3=All
      switch (cval) {
        case '0':
          active(ctx, (int) nb);
          break;
        case '1':
          error(ctx, (int) nb);
          break;
        case '2':
          done(ctx, (int) nb);
          break;
        case '3':
          all(ctx, (int) nb);
          break;
        case '4':
          status(ctx);
          break;
        case '5':
          statusxml(ctx, nb, extraBoolean);
          break;
        case '6':
          String name = null;
          if (params.containsKey("name")) {
            name = getTrimValue("name");
          }
          int istatus = 0;
          if (params.containsKey("status")) {
            final String statusNew = getTrimValue("status");
            try {
              istatus = Integer.parseInt(statusNew);
            } catch (final NumberFormatException e1) {
              istatus = 0;
            }
          }
          if (uriRequest.toLowerCase().startsWith("/spooleddetail")) {
            extraBoolean = true;
          }
          spooled(extraBoolean, name, istatus);
          break;
        case '7':
          statusjson(ctx, nb, extraBoolean);
          break;
        default:
          responseContent.append(REQUEST.index.readFileUnique(this));
      }
    }
    writeResponse(ctx);
  }

  /**
   * Add all runners from preparedStatement for type
   *
   * @param type
   * @param nb
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  private void addRunners(final List<Transfer> transfers, final String type,
                          final int nb) {
    responseContent
        .append("<style>td{font-size: 8pt;}</style><table border=\"2\">")
        .append("<tr><td>").append(type).append("</td>")
        .append(DbTaskRunner.headerHtml()).append("</tr>\r\n");

    int i = 0;
    for (final Transfer transfer : transfers) {
      final DbTaskRunner taskRunner = new DbTaskRunner(transfer);
      responseContent.append("<tr><td>")
                     .append(taskRunner.isSender()? "S" : "R").append("</td>");
      final LocalChannelReference lcr =
          Configuration.configuration.getLocalTransaction()
                                     .getFromRequest(taskRunner.getKey());
      responseContent.append(taskRunner.toHtml(getAuthentHttp(), lcr != null?
          Messages.getString("HttpSslHandler.Active") :
          Messages.getString("HttpSslHandler.NotActive"))).append("</tr>\r\n");
      if (nb > 0) {
        i++;
        if (i >= nb) {
          break;
        }
      }
    }
    responseContent.append("</table><br>\r\n");
  }

  /**
   * print all active transfers
   *
   * @param ctx
   * @param nb
   */
  private void active(final ChannelHandlerContext ctx, final int nb) {
    responseContent.append(REQUEST.active.readHeader(this));

    TransferDAO transferAccess = null;
    final List<Filter> filters = new ArrayList<Filter>();
    List<Transfer> transfers;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();

      // Find Status = RUNNING transfer
      filters.add(new Filter(DBTransferDAO.STEP_STATUS_FIELD, "=",
                             ErrorCode.Running.getCode()));
      filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                             Configuration.configuration.getHostId()));
      transfers = transferAccess
          .find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, nb);
      addRunners(transfers, ErrorCode.Running.getMesg(), nb);

      transfers.clear();
      filters.clear();
      // Find UpdatedInfo = INTERUPTED transfer
      filters.add(new Filter(DBTransferDAO.UPDATED_INFO_FIELD, "=",
                             UpdatedInfo.INTERRUPTED.ordinal()));
      filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                             Configuration.configuration.getHostId()));
      filters.add(new Filter(DBTransferDAO.TRANSFER_START_FIELD, "<",
                             new Timestamp(System.currentTimeMillis())));
      transfers = transferAccess
          .find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, nb);
      addRunners(transfers, UpdatedInfo.INTERRUPTED.name(), nb);

      transfers.clear();
      filters.clear();
      // Find UpdatedInfo = TOSUBMIT transfer
      filters.add(new Filter(DBTransferDAO.UPDATED_INFO_FIELD, "=",
                             UpdatedInfo.TOSUBMIT.ordinal()));
      filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                             Configuration.configuration.getHostId()));
      filters.add(new Filter(DBTransferDAO.TRANSFER_START_FIELD, "<",
                             new Timestamp(System.currentTimeMillis())));
      transfers = transferAccess
          .find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, nb);
      addRunners(transfers, UpdatedInfo.TOSUBMIT.name(), nb);

      transfers.clear();
      filters.clear();
      // Find Status = INITOK transfer
      filters.add(new Filter(DBTransferDAO.STEP_STATUS_FIELD, "=",
                             ErrorCode.InitOk.getCode()));
      filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                             Configuration.configuration.getHostId()));
      transfers = transferAccess
          .find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, nb);
      addRunners(transfers, ErrorCode.InitOk.getMesg(), nb);

      transfers.clear();
      filters.clear();
      // Find Status = PREPROCESSINGOK transfer
      filters.add(new Filter(DBTransferDAO.STEP_STATUS_FIELD, "=",
                             ErrorCode.PreProcessingOk.getCode()));
      filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                             Configuration.configuration.getHostId()));
      transfers = transferAccess
          .find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, nb);
      addRunners(transfers, ErrorCode.PreProcessingOk.getMesg(), nb);

      transfers.clear();
      filters.clear();
      // Find Status = TRANSFEROK transfer
      filters.add(new Filter(DBTransferDAO.STEP_STATUS_FIELD, "=",
                             ErrorCode.TransferOk.getCode()));
      filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                             Configuration.configuration.getHostId()));
      transfers = transferAccess
          .find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, nb);
      addRunners(transfers, ErrorCode.TransferOk.getMesg(), nb);

      transfers.clear();
      filters.clear();
      // Find Status = POSTPROCESSING transfer
      filters.add(new Filter(DBTransferDAO.STEP_STATUS_FIELD, "=",
                             ErrorCode.PostProcessingOk.getCode()));
      filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                             Configuration.configuration.getHostId()));
      transfers = transferAccess
          .find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, nb);
      addRunners(transfers, ErrorCode.PostProcessingOk.getMesg(), nb);
    } catch (final DAOConnectionException e) {
      logger.warn(OPEN_R66_WEB_ERROR, e.getMessage());
      sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
    } finally {
      DAOFactory.closeDAO(transferAccess);
    }
    responseContent.append(REQUEST.active.readEnd());
  }

  /**
   * print all transfers in error
   *
   * @param ctx
   * @param nb
   */
  private void error(final ChannelHandlerContext ctx, final int nb) {
    responseContent.append(REQUEST.error.readHeader(this));
    TransferDAO transferAccess = null;
    final List<Filter> filters = new ArrayList<Filter>();
    List<Transfer> transfers;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();

      // Find UpdatedInfo = INERROR transfer
      filters.add(new Filter(DBTransferDAO.UPDATED_INFO_FIELD, "=",
                             UpdatedInfo.INERROR.ordinal()));
      filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                             Configuration.configuration.getHostId()));
      filters.add(new Filter(DBTransferDAO.TRANSFER_START_FIELD, "<",
                             new Timestamp(System.currentTimeMillis())));
      transfers = transferAccess
          .find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, nb / 2);
      addRunners(transfers, UpdatedInfo.INERROR.name(), nb / 2);

      transfers.clear();
      filters.clear();
      // Find UpdatedInfo = INTERUPTED transfer
      filters.add(new Filter(DBTransferDAO.UPDATED_INFO_FIELD, "=",
                             UpdatedInfo.INTERRUPTED.ordinal()));
      filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                             Configuration.configuration.getHostId()));
      filters.add(new Filter(DBTransferDAO.TRANSFER_START_FIELD, "<",
                             new Timestamp(System.currentTimeMillis())));
      transfers = transferAccess
          .find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, nb / 2);
      addRunners(transfers, UpdatedInfo.INTERRUPTED.name(), nb / 2);

      transfers.clear();
      filters.clear();
      // Find GlobalStep = ERRORTASK transfer
      filters.add(new Filter(DBTransferDAO.GLOBAL_STEP_FIELD, "=",
                             Transfer.TASKSTEP.ERRORTASK.ordinal()));
      filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                             Configuration.configuration.getHostId()));
      filters.add(new Filter(DBTransferDAO.TRANSFER_START_FIELD, "<",
                             new Timestamp(System.currentTimeMillis())));
      transfers = transferAccess
          .find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, nb / 4);
      addRunners(transfers, TASKSTEP.ERRORTASK.name(), nb / 4);
    } catch (final DAOConnectionException e) {
      logger.warn(OPEN_R66_WEB_ERROR, e.getMessage());
      sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
      return;
    } finally {
      DAOFactory.closeDAO(transferAccess);
    }
    responseContent.append(REQUEST.error.readEnd());
  }

  /**
   * Print all done transfers
   *
   * @param ctx
   * @param nb
   */
  private void done(final ChannelHandlerContext ctx, final int nb) {
    responseContent.append(REQUEST.done.readHeader(this));

    TransferDAO transferAccess = null;
    final List<Transfer> transfers;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();

      final List<Filter> filters = new ArrayList<Filter>();
      filters.add(new Filter(DBTransferDAO.STEP_STATUS_FIELD, "=",
                             ErrorCode.CompleteOk.getCode()));
      filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                             Configuration.configuration.getHostId()));

      transfers = transferAccess
          .find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, nb);
      addRunners(transfers, "ALL RUNNERS: " + nb, nb);
    } catch (final DAOConnectionException e) {
      logger.warn(OPEN_R66_WEB_ERROR, e.getMessage());
      sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
      return;
    } finally {
      DAOFactory.closeDAO(transferAccess);
    }
    responseContent.append(REQUEST.done.readEnd());
  }

  /**
   * Print all nb last transfers
   *
   * @param ctx
   * @param nb
   */
  private void all(final ChannelHandlerContext ctx, final int nb) {
    responseContent.append(REQUEST.all.readHeader(this));

    TransferDAO transferAccess = null;
    final List<Transfer> transfers;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();

      final List<Filter> filters = new ArrayList<Filter>();
      filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                             Configuration.configuration.getHostId()));

      transfers = transferAccess
          .find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, nb);
      addRunners(transfers, "ALL RUNNERS: " + nb, nb);
    } catch (final DAOConnectionException e) {
      logger.warn(OPEN_R66_WEB_ERROR, e.getMessage());
      sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
      return;
    } finally {
      DAOFactory.closeDAO(transferAccess);
    }
    responseContent.append(REQUEST.all.readEnd());
  }

  /**
   * print only status
   *
   * @param ctx
   */
  private void status(final ChannelHandlerContext ctx) {
    responseContent.append(REQUEST.status.readHeader(this));

    TransferDAO transferAccess = null;
    final List<Filter> filters = new ArrayList<Filter>();
    List<Transfer> transfers;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();

      // Find UpdatedInfo = INERROR transfer
      filters.add(new Filter(DBTransferDAO.UPDATED_INFO_FIELD, "=",
                             UpdatedInfo.INERROR.ordinal()));
      filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                             Configuration.configuration.getHostId()));

      transfers = transferAccess
          .find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, 1);
      if (!transfers.isEmpty()) {
        responseContent.append("<p>Some Transfers are in ERROR</p><br>");
        status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
      }

      filters.clear();
      transfers.clear();
      // Find UpdatedInfo = INTERUPTED transfer
      filters.add(new Filter(DBTransferDAO.UPDATED_INFO_FIELD, "=",
                             UpdatedInfo.INTERRUPTED.ordinal()));
      filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                             Configuration.configuration.getHostId()));

      transfers = transferAccess
          .find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, 1);
      if (!transfers.isEmpty()) {
        responseContent.append("<p>Some Transfers are INTERRUPTED</p><br>");
        status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
      }
      filters.clear();
      transfers.clear();

      // Find GLOBAL_STEP = Error transfer
      filters.add(new Filter(DBTransferDAO.GLOBAL_STEP_FIELD, "=",
                             Transfer.TASKSTEP.ERRORTASK.ordinal()));
      filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                             Configuration.configuration.getHostId()));

      transfers = transferAccess
          .find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, 1);
      if (!transfers.isEmpty()) {
        responseContent.append("<p>Some Transfers are in ERRORTASK</p><br>");
        status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
      }
    } catch (final DAOConnectionException e) {
      logger.warn(OPEN_R66_WEB_ERROR, e.getMessage());
      sendError(ctx, HttpResponseStatus.SERVICE_UNAVAILABLE);
      return;
    } finally {
      DAOFactory.closeDAO(transferAccess);
    }
    if (status != HttpResponseStatus.INTERNAL_SERVER_ERROR) {
      responseContent.append("<p>No problem is found in Transfers</p><br>");
    }
    responseContent.append(REQUEST.status.readEnd());
  }

  /**
   * print only status
   *
   * @param ctx
   * @param nb
   */
  protected void statusxml(final ChannelHandlerContext ctx, final long nb,
                           final boolean detail) {
    Configuration.configuration.getMonitoring().run(nb, detail);
    responseContent
        .append(Configuration.configuration.getMonitoring().exportXml(detail));
  }

  /**
   * print only status
   *
   * @param ctx
   * @param nb
   */
  protected void statusjson(final ChannelHandlerContext ctx, final long nb,
                            final boolean detail) {
    Configuration.configuration.getMonitoring().run(nb, detail);
    responseContent
        .append(Configuration.configuration.getMonitoring().exportJson(detail));
  }

  private void spooled(final boolean detail, final String name,
                       final int istatus) {
    responseContent.append(REQUEST.status.readHeader(this)).append(
        "<p><table border='0' cellpadding='0' cellspacing='0' >").append(
        "<tr style='background-image:url(gre/gresm.png);background-repeat:repeat-x;background-position:left top;'><td class='col_MenuHaut'>")
                   .append(
                       "<a data-i18n='menu2.sous-menu4a' href='Spooled.html' style='display:block;width:100%;height:100%;line-height:15px;'>")
                   .append(
                       "SPOOLED DIRECTORY no detail</a></td><td></td><td><img src='gre/gre11.png' height='15' width='1' style='border: none; display: block;' alt='' /></td>")
                   .append(
                       "<td></td><td class='col_MenuHaut'><a data-i18n='menu2.sous-menu4c' href='Spooled.html?status=-1' style='display:block;width:100%;height:100%;line-height:15px;'>")
                   .append(
                       "SPOOLED DIRECTORY no detail KO</a></td><td></td><td><img src='gre/gre11.png' height='15' width='1' style='border: none; display: block;' alt='' /></td>")
                   .append(
                       "<td></td><td class='col_MenuHaut'><a data-i18n='menu2.sous-menu4d' href='Spooled.html?status=1' style='display:block;width:100%;height:100%;line-height:15px;'>")
                   .append(
                       "SPOOLED DIRECTORY no detail OK</a></td></tr><tr style='background-image:url(gre/gre11.png);background-repeat:repeat-x;background-position:left top;'>")
                   .append(
                       "<td><img src='gre/gre11.png' height='1' width='100%' style='border: none; display: block;' alt='' /></td></tr>")
                   .append(
                       "<tr style='background-image:url(gre/gresm.png);background-repeat:repeat-x;background-position:left top;'>")
                   .append(
                       "<td class='col_MenuHaut'><a data-i18n='menu2.sous-menu4b' href='SpooledDetailed.html' style='display:block;width:100%;height:100%;line-height:15px;'>")
                   .append(
                       "SPOOLED DIRECTORY detailed</a></td><td></td><td><img src='gre/gre11.png' height='15' width='1' style='border: none; display: block;' alt='' /></td>")
                   .append(
                       "<td></td><td class='col_MenuHaut'><a data-i18n='menu2.sous-menu4e' href='SpooledDetailed.html?status=-1' style='display:block;width:100%;height:100%;line-height:15px;'>")
                   .append(
                       "SPOOLED DIRECTORY detailed KO</a></td><td></td><td><img src='gre/gre11.png' height='15' width='1' style='border: none; display: block;' alt='' /></td>")
                   .append(
                       "<td></td><td class='col_MenuHaut'><a data-i18n='menu2.sous-menu4f' href='SpooledDetailed.html?status=1' style='display:block;width:100%;height:100%;line-height:15px;'>")
                   .append(
                       "SPOOLED DIRECTORY detailed OK</a></td></tr></table></p>");
    String uri;
    if (detail) {
      uri = "SpooledDetailed.html";
    } else {
      uri = "Spooled.html";
    }
    if (name != null && !name.isEmpty()) {
      // name is specified
      uri = request.uri();
      if (istatus != 0) {
        uri += "&status=" + istatus;
      }
      responseContent
          .append(SpooledInformTask.buildSpooledUniqueTable(uri, name));
    } else {
      if (istatus != 0) {
        uri += "&status=" + istatus;
      }
      responseContent
          .append(SpooledInformTask.buildSpooledTable(detail, istatus, uri));
    }
    responseContent.append(REQUEST.status.readEnd());
  }

  /**
   * Write the response
   *
   * @param ctx
   */
  protected void writeResponse(final ChannelHandlerContext ctx) {
    // Convert the response content to a ByteBuf.
    final ByteBuf buf = Unpooled
        .copiedBuffer(responseContent.toString(), WaarpStringUtils.UTF8);
    responseContent.setLength(0);
    // Decide whether to close the connection or not.
    final boolean keepAlive = HttpUtil.isKeepAlive(request);
    final boolean close = HttpHeaderValues.CLOSE.contentEqualsIgnoreCase(
        request.headers().get(HttpHeaderNames.CONNECTION)) || !keepAlive;

    // Build the response object.
    final FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buf);
    response.headers().add(HttpHeaderNames.CONTENT_LENGTH,
                           response.content().readableBytes());
    if (isCurrentRequestXml) {
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/xml");
    } else if (isCurrentRequestJson) {
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
    } else {
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
    }
    if (keepAlive) {
      response.headers()
              .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }
    if (!close) {
      // There's no need to add 'Content-Length' header
      // if this is the last response.
      response.headers().set(HttpHeaderNames.CONTENT_LENGTH,
                             String.valueOf(buf.readableBytes()));
    }

    final String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
    if (cookieString != null) {
      final Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
      boolean i18nextFound = false;
      if (!cookies.isEmpty()) {
        // Reset the cookies if necessary.
        for (final Cookie cookie : cookies) {
          if (cookie.name().equalsIgnoreCase(I18NEXT)) {
            i18nextFound = true;
            cookie.setValue(lang);
            response.headers().add(HttpHeaderNames.SET_COOKIE,
                                   ServerCookieEncoder.LAX.encode(cookie));
          } else {
            response.headers().add(HttpHeaderNames.SET_COOKIE,
                                   ServerCookieEncoder.LAX.encode(cookie));
          }
        }
        if (!i18nextFound) {
          final Cookie cookie = new DefaultCookie(I18NEXT, lang);
          response.headers().add(HttpHeaderNames.SET_COOKIE,
                                 ServerCookieEncoder.LAX.encode(cookie));
        }
      }
      if (!i18nextFound) {
        final Cookie cookie = new DefaultCookie(I18NEXT, lang);
        response.headers().add(HttpHeaderNames.SET_COOKIE,
                               ServerCookieEncoder.LAX.encode(cookie));
      }
    }

    // Write the response.
    final ChannelFuture future = ctx.writeAndFlush(response);
    // Close the connection after the write operation is done if necessary.
    if (close) {
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }

  /**
   * Send an error and close
   *
   * @param ctx
   * @param status
   */
  protected void sendError(final ChannelHandlerContext ctx,
                           final HttpResponseStatus status) {
    responseContent.setLength(0);
    responseContent.append(REQUEST.error.readHeader(this))
                   .append("OpenR66 Web Failure: ").append(status)
                   .append(REQUEST.error.readEnd());
    final ByteBuf buf = Unpooled
        .copiedBuffer(responseContent.toString(), WaarpStringUtils.UTF8);
    final FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buf);
    response.headers().add(HttpHeaderNames.CONTENT_LENGTH,
                           response.content().readableBytes());
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
    responseContent.setLength(0);
    // Close the connection as soon as the error message is sent.
    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx,
                              final Throwable cause) throws Exception {
    final OpenR66Exception exception = OpenR66ExceptionTrappedFactory
        .getExceptionFromTrappedException(ctx.channel(), cause);
    if (exception != null) {
      if (!(exception instanceof OpenR66ProtocolBusinessNoWriteBackException)) {
        if (cause instanceof IOException) {
          // Nothing to do
          return;
        }
        logger.warn("Exception in HttpHandler {}", exception.getMessage());
      }
      if (ctx.channel().isActive()) {
        sendError(ctx, HttpResponseStatus.BAD_REQUEST);
      }
    }
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx)
      throws Exception {
    super.channelInactive(ctx);
    logger.debug("Closed");
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    logger.debug("Connected");
    getAuthentHttp().getAuth().specialNoSessionAuth(false,
                                                    Configuration.configuration
                                                        .getHostId());
    super.channelActive(ctx);
    final ChannelGroup group =
        Configuration.configuration.getHttpChannelGroup();
    if (group != null) {
      group.add(ctx.channel());
    }
  }

  /**
   * @return the authentHttp
   */
  public R66Session getAuthentHttp() {
    return authentHttp;
  }
}
