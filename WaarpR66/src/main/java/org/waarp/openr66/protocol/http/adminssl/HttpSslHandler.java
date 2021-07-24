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
package org.waarp.openr66.protocol.http.adminssl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.traffic.TrafficCounter;
import org.waarp.common.command.exception.Reply421Exception;
import org.waarp.common.command.exception.Reply530Exception;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.exception.FileTransferException;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.file.DirInterface;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.ThreadLocalRandom;
import org.waarp.common.utility.Version;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.gateway.kernel.http.HttpWriteCacheEnable;
import org.waarp.openr66.client.Message;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.SpooledInformTask;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbHostConfiguration;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66Exception;
import org.waarp.openr66.protocol.exception.OpenR66ExceptionTrappedFactory;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessNoWriteBackException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.LocalServerHandler;
import org.waarp.openr66.protocol.localhandler.ServerActions;
import org.waarp.openr66.protocol.localhandler.packet.ErrorPacket;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket.TRANSFERMODE;
import org.waarp.openr66.protocol.localhandler.packet.TestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.NbAndSpecialId;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.protocol.utils.TransferUtils;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class HttpSslHandler
    extends SimpleChannelInboundHandler<FullHttpRequest> {
  private static final String XXXLEVEL4XXX = "XXXLEVEL4XXX";
  private static final String XXXLEVEL3XXX = "XXXLEVEL3XXX";
  private static final String XXXLEVEL2XXX = "XXXLEVEL2XXX";
  private static final String XXXLEVEL1XXX = "XXXLEVEL1XXX";
  private static final String OPEN_R66_WEB_ERROR = "OpenR66 Web Error {}";
  private static final String HTTP_SSL_HANDLER_MOPS = "HttpSslHandler.MOPS";
  private static final String HTTP_SSL_HANDLER_LANG_IS =
      "HttpSslHandler.LangIs";
  private static final String HTTP_SSL_HANDLER_17 = "HttpSslHandler.17";
  private static final String EXPORT_ERROR = "Export error: {}";
  private static final String CHECKED = "checked";
  private static final String HTTP_SSL_HANDLER_NOT_ACTIVE =
      "HttpSslHandler.NotActive";
  private static final String HTTP_SSL_HANDLER_ACTIVE = "HttpSslHandler.Active";
  private static final String AN_ERROR_OCCURS_WHILE_ACCESSING_A_RUNNER =
      "An error occurs while accessing a Runner: {}";
  private static final String ADDRESS = "address";
  private static final String CREATE = "Create";
  private static final String ERROR2 = "error";
  private static final String TRANSFER2 = "transfer";
  private static final String PENDING2 = "pending";
  private static final String START2 = "start";
  private static final String STOPID2 = "stopid";
  private static final String STARTID2 = "startid";
  private static final String FILTER = "Filter";
  private static final String ACTION = "ACTION";
  private static final String HTTP_SSL_HANDLER_3 = "HttpSslHandler.3";
  private static final String BR_B = "<br><b>";
  private static final String B_CENTER_P = "</b></center></p>";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpSslHandler.class);
  /**
   * Session Management
   */
  protected static final ConcurrentHashMap<String, R66Session> sessions =
      new ConcurrentHashMap<String, R66Session>();
  protected static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

  protected R66Session authentHttp = new R66Session(false);

  protected FullHttpRequest request;
  protected boolean newSession;
  protected Cookie admin;
  protected final StringBuilder responseContent = new StringBuilder();
  protected String uriRequest;
  protected Map<String, List<String>> params;
  protected String lang = Messages.getSlocale();
  protected boolean forceClose;
  protected boolean shutdown;

  protected static final String R66SESSION = "R66SESSION";
  protected static final String I18NEXT = "i18next";

  private enum REQUEST {
    Logon("Logon.html"), Logout("Logon.html"), index("index.html"),
    error("error.html"), unallowed("NotAllowed.html"),
    Transfers("Transfers.html"),
    Listing("Listing_head.html", "Listing_body0.html", "Listing_body.html",
            "Listing_body1.html", "Listing_end.html"),
    CancelRestart("CancelRestart_head.html", "CancelRestart_body0.html",
                  "CancelRestart_body.html", "CancelRestart_body1.html",
                  "CancelRestart_end.html"), Export("Export.html"),
    Hosts("Hosts_head.html", "Hosts_body0.html", "Hosts_body.html",
          "Hosts_body1.html", "Hosts_end.html"),
    Rules("Rules_head.html", "Rules_body0.html", "Rules_body.html",
          "Rules_body1.html", "Rules_end.html"), System("System.html"),
    SystemLimited("SystemLimited.html"), Spooled("Spooled.html"),
    SpooledDetailed("Spooled.html");

    private final String header;
    private final String headerBody;
    private final String body;
    private final String endBody;
    private final String end;

    /**
     * Constructor for a unique file
     *
     * @param uniquefile
     */
    REQUEST(final String uniquefile) {
      header = uniquefile;
      headerBody = null;
      body = null;
      endBody = null;
      end = null;
    }

    /**
     * @param header
     * @param headerBody
     * @param body
     * @param endBody
     * @param end
     */
    REQUEST(final String header, final String headerBody, final String body,
            final String endBody, final String end) {
      this.header = header;
      this.headerBody = headerBody;
      this.body = body;
      this.endBody = endBody;
      this.end = end;
    }

    /**
     * Reader for a unique file
     *
     * @return the content of the unique file
     */
    public String readFileUnique(final HttpSslHandler handler) {
      return handler.readFile(
          Configuration.configuration.getHttpBasePath() + header);
    }

    public String readHeader(final HttpSslHandler handler) {
      return handler.readFile(
          Configuration.configuration.getHttpBasePath() + header);
    }

    public String readBodyHeader() {
      return WaarpStringUtils.readFile(
          Configuration.configuration.getHttpBasePath() + headerBody);
    }

    public String readBody() {
      return WaarpStringUtils.readFile(
          Configuration.configuration.getHttpBasePath() + body);
    }

    public String readBodyEnd() {
      return WaarpStringUtils.readFile(
          Configuration.configuration.getHttpBasePath() + endBody);
    }

    public String readEnd() {
      return WaarpStringUtils.readFile(
          Configuration.configuration.getHttpBasePath() + end);
    }
  }

  protected enum REPLACEMENT {
    XXXHOSTIDXXX, XXXADMINXXX, XXXVERSIONXXX, XXXBANDWIDTHXXX,
    XXXBANDWIDTHINXXX, XXXBANDWIDTHOUTXXX, XXXXSESSIONLIMITRXXX,
    XXXXSESSIONLIMITWXXX, XXXXCHANNELLIMITRXXX, XXXXCHANNELLIMITWXXX,
    XXXXDELAYCOMMDXXX, XXXXDELAYRETRYXXX, XXXXDELATRAFFICXXX, XXXLOCALXXX,
    XXXNETWORKXXX, XXXNBTRANSFERSXXX, XXXERRORMESGXXX, XXXXBUSINESSXXX,
    XXXXROLESXXX, XXXXALIASESXXX, XXXXOTHERXXX, XXXLIMITROWXXX, XXXREFRESHXXX,
    XXXLANGXXX, XXXCURLANGENXXX, XXXCURLANGFRXXX, XXXCURSYSLANGENXXX,
    XXXCURSYSLANGFRXXX
  }

  public static final String sLIMITROW = "LIMITROW";
  static final String XXXRESULTXXX = "XXXRESULTXXX";

  private int limitRow = 48; // better if it can
  // be divided by 4
  private int refresh;

  /**
   * The Database connection attached to this NetworkChannelReference shared
   * among all associated LocalChannels
   * in the session
   */
  DbSession dbSession;

  public static String hashStatus() {
    return "HttpSslHandler: [sessions: " + sessions.size() + "] ";
  }

  String readFile(final String filename) {
    final String value;
    try {
      value = WaarpStringUtils.readFileException(filename);
    } catch (final FileTransferException e) {
      logger.error("Error while trying to read: " + filename, e);
      return "";
    }
    final StringBuilder builder = new StringBuilder(value);
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXLOCALXXX.toString(),
                             Configuration.configuration.getLocalTransaction()
                                                        .getNumberLocalChannel() +
                             " Thread(" + Thread.activeCount() + ')');
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXNETWORKXXX.toString(),
                             Integer.toString(DbAdmin.getNbConnection() -
                                              Configuration.getNbDbSession()));
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXNBTRANSFERSXXX.toString(),
                             Long.toString(
                                 Configuration.configuration.getMonitoring().nbCountAllRunningStep));
    WaarpStringUtils.replaceAll(builder, REPLACEMENT.XXXHOSTIDXXX.toString(),
                                Configuration.configuration.getHostId());
    if (authentHttp.isAuthenticated()) {
      WaarpStringUtils.replace(builder, REPLACEMENT.XXXADMINXXX.toString(),
                               Messages.getString(
                                   "HttpSslHandler.1")); //$NON-NLS-1$
    } else {
      WaarpStringUtils.replace(builder, REPLACEMENT.XXXADMINXXX.toString(),
                               Messages.getString(
                                   "HttpSslHandler.0")); //$NON-NLS-1$
    }
    final TrafficCounter trafficCounter =
        Configuration.configuration.getGlobalTrafficShapingHandler()
                                   .trafficCounter();
    final long read = trafficCounter.lastReadThroughput();
    final long write = trafficCounter.lastWriteThroughput();
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXBANDWIDTHXXX.toString(),
                             Messages.getString("HttpSslHandler.IN") +
                             (read >> 20) + //$NON-NLS-1$
                             Messages.getString(HTTP_SSL_HANDLER_MOPS) +
                             //$NON-NLS-1$
                             Messages.getString("HttpSslHandler.OUT") +
                             //$NON-NLS-1$
                             (write >> 20) + Messages.getString(
                                 HTTP_SSL_HANDLER_MOPS)); //$NON-NLS-1$
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXBANDWIDTHINXXX.toString(),
                             (read >> 20) + Messages.getString(
                                 HTTP_SSL_HANDLER_MOPS)); //$NON-NLS-1$
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXBANDWIDTHOUTXXX.toString(),
                             (write >> 20) + Messages.getString(
                                 HTTP_SSL_HANDLER_MOPS)); //$NON-NLS-1$
    WaarpStringUtils.replaceAll(builder, REPLACEMENT.XXXLIMITROWXXX.toString(),
                                String.valueOf(getLimitRow()));
    WaarpStringUtils.replaceAll(builder, REPLACEMENT.XXXREFRESHXXX.toString(),
                                String.valueOf(getRefresh() / 1000));
    WaarpStringUtils.replaceAll(builder, REPLACEMENT.XXXLANGXXX.toString(),
                                lang);
    return builder.toString();
  }

  protected final String getTrimValue(final String varname) {
    final List<String> varlist = params.get(varname);
    if (varlist != null && !varlist.isEmpty()) {
      String value = params.get(varname).get(0).trim();
      if (ParametersChecker.isEmpty(value)) {
        value = null;
      }
      return value;
    }
    return null;
  }

  final String getValue(final String varname) {
    return params.get(varname).get(0);
  }

  private String index() {
    final String index = REQUEST.index.readFileUnique(this);
    final StringBuilder builder = new StringBuilder(index);
    WaarpStringUtils.replaceAll(builder, REPLACEMENT.XXXHOSTIDXXX.toString(),
                                Configuration.configuration.getHostId());
    WaarpStringUtils.replaceAll(builder, REPLACEMENT.XXXADMINXXX.toString(),
                                Messages.getString(
                                    "HttpSslHandler.2")); //$NON-NLS-1$
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXVERSIONXXX.toString(),
                             Version.fullIdentifier());
    return builder.toString();
  }

  protected String error(final String mesg) {
    final String index = REQUEST.error.readFileUnique(this);
    return index.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(), mesg);
  }

  private String unallowed(final String mesg) {
    final String index = REQUEST.unallowed.readFileUnique(this);
    if (ParametersChecker.isEmpty(index)) {
      return error(mesg);
    }
    return index.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(), mesg);
  }

  protected String logon() {
    return REQUEST.Logon.readFileUnique(this);
  }

  private String transfers() {
    return REQUEST.Transfers.readFileUnique(this);
  }

  final String resetOptionTransfer(final String header, final String startid,
                                   final String stopid, final String start,
                                   final String stop, final String rule,
                                   final String req, final boolean pending,
                                   final boolean transfer, final boolean error,
                                   final boolean done, final boolean all) {
    final StringBuilder builder = new StringBuilder(header);
    WaarpStringUtils.replace(builder, "XXXSTARTIDXXX", startid);
    WaarpStringUtils.replace(builder, "XXXSTOPIDXXX", stopid);
    WaarpStringUtils.replace(builder, "XXXSTARTXXX", start);
    WaarpStringUtils.replace(builder, "XXXSTOPXXX", stop);
    WaarpStringUtils.replace(builder, "XXXRULEXXX", rule);
    WaarpStringUtils.replace(builder, "XXXREQXXX", req);
    WaarpStringUtils.replace(builder, "XXXPENDXXX", pending? CHECKED : "");
    WaarpStringUtils.replace(builder, "XXXTRANSXXX", transfer? CHECKED : "");
    WaarpStringUtils.replace(builder, "XXXERRXXX", error? CHECKED : "");
    WaarpStringUtils.replace(builder, "XXXDONEXXX", done? CHECKED : "");
    WaarpStringUtils.replace(builder, "XXXALLXXX", all? CHECKED : "");
    return builder.toString();
  }

  final String checkAuthorizedToSeeAll() {
    boolean seeAll = false;
    if (authentHttp.getAuth().isValidRole(ROLE.CONFIGADMIN)) {
      final DbHostConfiguration dbhc;
      try {
        dbhc = new DbHostConfiguration(Configuration.configuration.getHostId());
      } catch (final WaarpDatabaseException e) {
        return null;
      }
      seeAll = dbhc != null && dbhc.isSeeAllId(authentHttp.getAuth().getUser());
    }
    if (seeAll) {
      return "*";
    }
    return null;
  }

  private String listing0() {
    getParams();
    if (params == null) {
      String head = REQUEST.Listing.readHeader(this);
      head =
          resetOptionTransfer(head, "", "", "", "", "", "", false, false, false,
                              false, true);
      final String end = REQUEST.Listing.readEnd().replace(XXXRESULTXXX, "");
      return head + end;
    }
    String head = REQUEST.Listing.readHeader(this);
    String body0;
    String body;
    String body1;
    body0 = body1 = body = "";
    final StringBuilder endText = new StringBuilder();
    final List<String> parms = params.get(ACTION);
    if (parms != null) {
      body0 = REQUEST.Listing.readBodyHeader();
      final String parm = parms.get(0);
      final boolean isNotReload = !"Reload".equalsIgnoreCase(parm);
      if (FILTER.equalsIgnoreCase(parm) || !isNotReload) {
        String startid = getTrimValue(STARTID2);
        String stopid = getTrimValue(STOPID2);
        if (isNotReload && startid != null && stopid == null) {
          stopid = Long.MAX_VALUE + "";
        }
        if (isNotReload && stopid != null && startid == null) {
          startid = (DbConstantR66.ILLEGALVALUE + 1) + "";
        }
        String start = getValue(START2);
        String stop = getValue("stop");
        final String rule = getTrimValue("rule");
        final String req = getTrimValue("req");
        final boolean pending;
        final boolean transfer;
        final boolean error;
        final boolean done;
        boolean all;
        pending = params.containsKey(PENDING2);
        transfer = params.containsKey(TRANSFER2);
        error = params.containsKey(ERROR2);
        done = params.containsKey("done");
        all = params.containsKey("all");
        if (pending && transfer && error && done) {
          all = true;
        } else if (!(pending || transfer || error || done)) {
          all = true;
        }
        final Timestamp tstart = WaarpStringUtils.fixDate(start);
        if (tstart != null) {
          start = tstart.toString();
        }
        final Timestamp tstop = WaarpStringUtils.fixDate(stop, tstart);
        if (tstop != null) {
          stop = tstop.toString();
        }
        Long idstart = null;
        body = REQUEST.Listing.readBody();
        final String seeAll = checkAuthorizedToSeeAll();
        DbPreparedStatement preparedStatement = null;
        try {
          preparedStatement =
              DbTaskRunner.getFilterPrepareStatement(dbSession, getLimitRow(),
                                                     false, startid, stopid,
                                                     tstart, tstop, rule, req,
                                                     pending, transfer, error,
                                                     done, all, seeAll);
          preparedStatement.executeQuery();
          final StringBuilder builder = new StringBuilder();
          int i = 0;
          while (preparedStatement.getNext()) {
            try {
              i++;
              final DbTaskRunner taskRunner =
                  DbTaskRunner.getFromStatementNoRule(preparedStatement);
              if (isNotReload) {
                final long specid = taskRunner.getSpecialId();
                if (idstart == null || idstart > specid) {
                  idstart = specid;
                }
              }
              final LocalChannelReference lcr =
                  Configuration.configuration.getLocalTransaction()
                                             .getFromRequest(
                                                 taskRunner.getKey());
              builder.append(taskRunner.toSpecializedHtml(authentHttp, body,
                                                          lcr != null?
                                                              Messages.getString(
                                                                  HTTP_SSL_HANDLER_ACTIVE) :
                                                              Messages.getString(
                                                                  HTTP_SSL_HANDLER_NOT_ACTIVE)));
              if (i > getLimitRow()) {
                break;
              }
            } catch (final WaarpDatabaseException e) {
              // try to continue if possible
              logger.warn(AN_ERROR_OCCURS_WHILE_ACCESSING_A_RUNNER,
                          e.getMessage());
              endText.append(e.getMessage()).append("</BR>");
            }
          }
          preparedStatement.realClose();
          body = builder.toString();
        } catch (final WaarpDatabaseException e) {
          if (preparedStatement != null) {
            preparedStatement.realClose();
          }
          logger.warn(OPEN_R66_WEB_ERROR, e.getMessage());
        }
        head = resetOptionTransfer(head, startid == null?
                                       idstart != null? idstart.toString() : "" : startid,
                                   stopid == null? "" : stopid, start, stop,
                                   rule == null? "" : rule,
                                   req == null? "" : req, pending, transfer,
                                   error, done, all);
      } else {
        head = resetOptionTransfer(head, "", "", "", "", "", "", false, false,
                                   false, false, true);
      }
      body1 = REQUEST.Listing.readBodyEnd();
    } else {
      head =
          resetOptionTransfer(head, "", "", "", "", "", "", false, false, false,
                              false, true);
    }
    final String end =
        REQUEST.Listing.readEnd().replace(XXXRESULTXXX, endText.toString());
    return head + body0 + body + body1 + end;
  }

  private String cancelRestart0() {
    getParams();
    if (params == null) {
      String head = REQUEST.CancelRestart.readHeader(this);
      head =
          resetOptionTransfer(head, "", "", "", "", "", "", false, false, false,
                              false, true);
      final String end;
      end = REQUEST.CancelRestart.readEnd();
      return head + end;
    }
    String head = REQUEST.CancelRestart.readHeader(this);
    String body0;
    String body;
    String body1;
    body0 = body1 = body = "";
    final List<String> parms = params.get(ACTION);
    final String seeAll = checkAuthorizedToSeeAll();
    if (parms != null) {
      body0 = REQUEST.CancelRestart.readBodyHeader();
      final String parm = parms.get(0);
      final boolean isNotReload = !"Reload".equalsIgnoreCase(parm);
      if (FILTER.equalsIgnoreCase(parm) || !isNotReload) {
        String startid = getTrimValue(STARTID2);
        String stopid = getTrimValue(STOPID2);
        if (isNotReload && startid != null && stopid == null) {
          stopid = Long.MAX_VALUE + "";
        }
        if (isNotReload && stopid != null && startid == null) {
          startid = (DbConstantR66.ILLEGALVALUE + 1) + "";
        }
        String start = getValue(START2);
        String stop = getValue("stop");
        final String rule = getTrimValue("rule");
        final String req = getTrimValue("req");
        final boolean pending;
        final boolean transfer;
        final boolean error;
        final boolean done;
        boolean all;
        pending = params.containsKey(PENDING2);
        transfer = params.containsKey(TRANSFER2);
        error = params.containsKey(ERROR2);
        done = params.containsKey("done");
        all = params.containsKey("all");
        if (pending && transfer && error && done) {
          all = true;
        } else if (!(pending || transfer || error || done)) {
          all = true;
        }
        final Timestamp tstart = WaarpStringUtils.fixDate(start);
        if (tstart != null) {
          start = tstart.toString();
        }
        final Timestamp tstop = WaarpStringUtils.fixDate(stop, tstart);
        if (tstop != null) {
          stop = tstop.toString();
        }
        body = REQUEST.CancelRestart.readBody();
        Long idstart = null;
        DbPreparedStatement preparedStatement = null;
        try {
          preparedStatement =
              DbTaskRunner.getFilterPrepareStatement(dbSession, getLimitRow(),
                                                     false, startid, stopid,
                                                     tstart, tstop, rule, req,
                                                     pending, transfer, error,
                                                     done, all, seeAll);
          preparedStatement.executeQuery();
          final StringBuilder builder = new StringBuilder();
          int i = 0;
          while (preparedStatement.getNext()) {
            try {
              i++;
              final DbTaskRunner taskRunner =
                  DbTaskRunner.getFromStatementNoRule(preparedStatement);
              if (isNotReload) {
                final long specid = taskRunner.getSpecialId();
                if (idstart == null || idstart > specid) {
                  idstart = specid;
                }
              }
              final LocalChannelReference lcr =
                  Configuration.configuration.getLocalTransaction()
                                             .getFromRequest(
                                                 taskRunner.getKey());
              builder.append(taskRunner.toSpecializedHtml(authentHttp, body,
                                                          lcr != null?
                                                              Messages.getString(
                                                                  HTTP_SSL_HANDLER_ACTIVE) :
                                                              Messages.getString(
                                                                  HTTP_SSL_HANDLER_NOT_ACTIVE)));
              if (i > getLimitRow()) {
                break;
              }
            } catch (final WaarpDatabaseException e) {
              // try to continue if possible
              logger.warn(AN_ERROR_OCCURS_WHILE_ACCESSING_A_RUNNER,
                          e.getMessage());
            }
          }
          preparedStatement.realClose();
          body = builder.toString();
        } catch (final WaarpDatabaseException e) {
          if (preparedStatement != null) {
            preparedStatement.realClose();
          }
          logger.warn(OPEN_R66_WEB_ERROR, e.getMessage());
        }
        head = resetOptionTransfer(head, startid == null?
                                       idstart != null? idstart.toString() : "" : startid,
                                   stopid == null? "" : stopid, start, stop,
                                   rule == null? "" : rule,
                                   req == null? "" : req, pending, transfer,
                                   error, done, all);
        body1 = REQUEST.CancelRestart.readBodyEnd();
      } else if ("RestartAll".equalsIgnoreCase(parm) ||
                 "StopAll".equalsIgnoreCase(parm) ||
                 "StopCleanAll".equalsIgnoreCase(parm)) {
        final boolean stopcommand = "StopAll".equalsIgnoreCase(parm) ||
                                    "StopCleanAll".equalsIgnoreCase(parm);
        final String startid = getTrimValue(STARTID2);
        final String stopid = getTrimValue(STOPID2);
        String start = getValue(START2);
        String stop = getValue("stop");
        final String rule = getTrimValue("rule");
        final String req = getTrimValue("req");
        boolean pending;
        boolean transfer;
        boolean error;
        final boolean done;
        boolean all;
        pending = params.containsKey(PENDING2);
        transfer = params.containsKey(TRANSFER2);
        error = params.containsKey(ERROR2);
        done = false;
        all = false;
        if (!(pending || transfer || error)) {
          all = true;
          pending = true;
          transfer = true;
          error = true;
        }
        final Timestamp tstart = WaarpStringUtils.fixDate(start);
        if (tstart != null) {
          start = tstart.toString();
        }
        final Timestamp tstop = WaarpStringUtils.fixDate(stop, tstart);
        if (tstop != null) {
          stop = tstop.toString();
        }
        head = resetOptionTransfer(head, startid == null? "" : startid,
                                   stopid == null? "" : stopid, start, stop,
                                   rule == null? "" : rule,
                                   req == null? "" : req, pending, transfer,
                                   error, false, all);
        body = REQUEST.CancelRestart.readBody();
        final StringBuilder builder = new StringBuilder();
        if (stopcommand) {
          if ("StopCleanAll".equalsIgnoreCase(parm)) {
            TransferUtils.cleanSelectedTransfers(dbSession, 0, builder,
                                                 authentHttp, body, startid,
                                                 stopid, tstart, tstop, rule,
                                                 req, pending, transfer, error,
                                                 seeAll);
          } else {
            TransferUtils.stopSelectedTransfers(dbSession, 0, builder,
                                                authentHttp, body, startid,
                                                stopid, tstart, tstop, rule,
                                                req, pending, transfer, error,
                                                seeAll);
          }
        } else {
          DbPreparedStatement preparedStatement = null;
          try {
            preparedStatement =
                DbTaskRunner.getFilterPrepareStatement(dbSession, 0, false,
                                                       startid, stopid, tstart,
                                                       tstop, rule, req,
                                                       pending, transfer, error,
                                                       false, all, seeAll);
            preparedStatement.executeQuery();
            while (preparedStatement.getNext()) {
              try {
                final DbTaskRunner taskRunner =
                    DbTaskRunner.getFromStatement(preparedStatement);
                final LocalChannelReference lcr =
                    Configuration.configuration.getLocalTransaction()
                                               .getFromRequest(
                                                   taskRunner.getKey());
                final R66Result finalResult =
                    TransferUtils.restartTransfer(taskRunner, lcr);
                final ErrorCode result = finalResult.getCode();
                final ErrorCode last = taskRunner.getErrorInfo();
                taskRunner.setErrorExecutionStatus(result);
                builder.append(taskRunner.toSpecializedHtml(authentHttp, body,
                                                            lcr != null?
                                                                Messages.getString(
                                                                    HTTP_SSL_HANDLER_ACTIVE) :
                                                                Messages.getString(
                                                                    HTTP_SSL_HANDLER_NOT_ACTIVE)));
                taskRunner.setErrorExecutionStatus(last);
              } catch (final WaarpDatabaseException e) {
                // try to continue if possible
                logger.warn(AN_ERROR_OCCURS_WHILE_ACCESSING_A_RUNNER,
                            e.getMessage());
              }
            }
            preparedStatement.realClose();
          } catch (final WaarpDatabaseException e) {
            if (preparedStatement != null) {
              preparedStatement.realClose();
            }
            logger.warn(OPEN_R66_WEB_ERROR, e.getMessage());
          }
        }
        if (builder != null) {
          body = builder.toString();
        }
        body1 = REQUEST.CancelRestart.readBodyEnd();
      } else if ("Cancel".equalsIgnoreCase(parm) ||
                 "CancelClean".equalsIgnoreCase(parm) ||
                 "Stop".equalsIgnoreCase(parm)) {
        // Cancel or Stop
        final boolean stop = "Stop".equalsIgnoreCase(parm);
        final String specid = getValue("specid");
        final String reqd = getValue("reqd");
        final String reqr = getValue("reqr");
        final LocalChannelReference lcr =
            Configuration.configuration.getLocalTransaction().getFromRequest(
                reqd + ' ' + reqr + ' ' + specid);
        // stop the current transfer
        ErrorCode result;
        final long lspecid;
        try {
          lspecid = Long.parseLong(specid);
        } catch (final NumberFormatException e) {
          body = "";
          body1 = REQUEST.CancelRestart.readBodyEnd();
          body1 += BR_B + parm +
                   Messages.getString(HTTP_SSL_HANDLER_3); //$NON-NLS-2$
          final String end;
          end = REQUEST.CancelRestart.readEnd();
          return head + body0 + body + body1 + end;
        }
        DbTaskRunner taskRunner = null;
        try {
          taskRunner = new DbTaskRunner(authentHttp, null, lspecid, reqr, reqd);
        } catch (final WaarpDatabaseException ignored) {
          // nothing
        }
        if (taskRunner == null) {
          body = "";
          body1 = REQUEST.CancelRestart.readBodyEnd();
          body1 += BR_B + parm +
                   Messages.getString(HTTP_SSL_HANDLER_3); //$NON-NLS-2$
          final String end;
          end = REQUEST.CancelRestart.readEnd();
          return head + body0 + body + body1 + end;
        }
        final ErrorCode code =
            stop? ErrorCode.StoppedTransfer : ErrorCode.CanceledTransfer;
        if (lcr != null) {
          final int rank = taskRunner.getRank();
          lcr.sessionNewState(R66FiniteDualStates.ERROR);
          final ErrorPacket error =
              new ErrorPacket("Transfer " + parm + ' ' + rank, code.getCode(),
                              ErrorPacket.FORWARDCLOSECODE);
          try {
            // inform local instead of remote
            LocalServerHandler.channelRead0(lcr, error);
          } catch (final Exception ignored) {
            // nothing
          }
          result = ErrorCode.CompleteOk;
        } else {
          // Transfer is not running
          // But is the database saying the contrary
          result = ErrorCode.TransferOk;
          if (taskRunner.stopOrCancelRunner(code)) {
            result = ErrorCode.CompleteOk;
          }
        }
        if ("CancelClean".equalsIgnoreCase(parm)) {
          TransferUtils.cleanOneTransfer(taskRunner, null, authentHttp, null);
        }
        body = REQUEST.CancelRestart.readBody();
        body = taskRunner.toSpecializedHtml(authentHttp, body, lcr != null?
            Messages.getString(HTTP_SSL_HANDLER_ACTIVE)
            //$NON-NLS-1$
            : Messages.getString(HTTP_SSL_HANDLER_NOT_ACTIVE)); //$NON-NLS-1$
        final String tstart = taskRunner.getStart().toString();
        final String tstop = taskRunner.getStop().toString();
        head = resetOptionTransfer(head, String.valueOf(
                                       taskRunner.getSpecialId() - 1), String.valueOf(
                                       taskRunner.getSpecialId() + 1), tstart, tstop,
                                   taskRunner.getRuleId(),
                                   taskRunner.getRequested(), false, false,
                                   false, false, true);
        body1 = REQUEST.CancelRestart.readBodyEnd();
        body1 += BR_B + (result == ErrorCode.CompleteOk?
            parm + Messages.getString("HttpSslHandler.5") :
            //$NON-NLS-2$
            parm + Messages.getString("HttpSslHandler.4")) +
                 "</b>"; //$NON-NLS-1$
      } else if ("Restart".equalsIgnoreCase(parm)) {
        // Restart
        final String specid = getValue("specid");
        final String reqd = getValue("reqd");
        final String reqr = getValue("reqr");
        final long lspecid;
        try {
          lspecid = Long.parseLong(specid);
        } catch (final NumberFormatException e) {
          body = "";
          body1 = REQUEST.CancelRestart.readBodyEnd();
          body1 += BR_B + parm +
                   Messages.getString(HTTP_SSL_HANDLER_3); //$NON-NLS-2$
          final String end;
          end = REQUEST.CancelRestart.readEnd();
          return head + body0 + body + body1 + end;
        }
        final DbTaskRunner taskRunner;
        String comment;
        try {
          taskRunner = new DbTaskRunner(authentHttp, null, lspecid, reqr, reqd);
          final LocalChannelReference lcr =
              Configuration.configuration.getLocalTransaction()
                                         .getFromRequest(taskRunner.getKey());
          final R66Result finalResult =
              TransferUtils.restartTransfer(taskRunner, lcr);
          comment = (String) finalResult.getOther();
          body = REQUEST.CancelRestart.readBody();
          body = taskRunner.toSpecializedHtml(authentHttp, body, lcr != null?
              Messages.getString(HTTP_SSL_HANDLER_ACTIVE)
              //$NON-NLS-1$
              : Messages.getString(HTTP_SSL_HANDLER_NOT_ACTIVE)); //$NON-NLS-1$
          final String tstart = taskRunner.getStart().toString();
          final String tstop = taskRunner.getStop().toString();
          head = resetOptionTransfer(head, String.valueOf(
                                         taskRunner.getSpecialId() - 1), String.valueOf(
                                         taskRunner.getSpecialId() + 1), tstart, tstop,
                                     taskRunner.getRuleId(),
                                     taskRunner.getRequested(), false, false,
                                     false, false, true);
        } catch (final WaarpDatabaseException e) {
          body = "";
          comment = Messages.getString("ErrorCode.17"); //$NON-NLS-1$
        }
        body1 = REQUEST.CancelRestart.readBodyEnd();
        body1 += BR_B + comment + "</b>";
      } else {
        head = resetOptionTransfer(head, "", "", "", "", "", "", false, false,
                                   false, false, true);
      }
    } else {
      head =
          resetOptionTransfer(head, "", "", "", "", "", "", false, false, false,
                              false, true);
    }
    final String end;
    end = REQUEST.CancelRestart.readEnd();
    return head + body0 + body + body1 + end;
  }

  private String export0() {
    getParams();
    if (params == null) {
      String body = REQUEST.Export.readFileUnique(this);
      body =
          resetOptionTransfer(body, "", "", "", "", "", "", false, false, false,
                              true, false);
      return body.replace(XXXRESULTXXX, "");
    }
    String body = REQUEST.Export.readFileUnique(this);
    String start = getValue(START2);
    String stop = getValue("stop");
    final String rule = getTrimValue("rule");
    final String req = getTrimValue("req");
    final boolean pending;
    boolean transfer;
    final boolean error;
    final boolean done;
    boolean all;
    pending = params.containsKey(PENDING2);
    transfer = params.containsKey(TRANSFER2);
    error = params.containsKey(ERROR2);
    done = params.containsKey("done");
    all = params.containsKey("all");
    boolean toPurge = params.containsKey("purge");
    if (toPurge) {
      transfer = false;
    }
    if (pending && transfer && error && done) {
      all = true;
    } else if (!(pending || transfer || error || done)) {
      all = true;
    }
    final Timestamp tstart = WaarpStringUtils.fixDate(start);
    if (tstart != null) {
      start = tstart.toString();
    }
    final Timestamp tstop = WaarpStringUtils.fixDate(stop, tstart);
    if (tstop != null) {
      stop = tstop.toString();
    }
    body =
        resetOptionTransfer(body, "", "", start, stop, rule == null? "" : rule,
                            req == null? "" : req, pending, transfer, error,
                            done, all);
    boolean isexported = true;
    // clean a bit the database before exporting
    try {
      DbTaskRunner.changeFinishedToDone();
    } catch (final WaarpDatabaseNoConnectionException e2) {
      // should not be
    }
    // create export of log and optionally purge them from database
    DbPreparedStatement getValid = null;
    NbAndSpecialId nbAndSpecialId = null;
    final String filename = Configuration.configuration.getBaseDirectory() +
                            Configuration.configuration.getArchivePath() +
                            DirInterface.SEPARATOR +
                            Configuration.configuration.getHostId() + '_' +
                            System.currentTimeMillis() + "_runners.xml";
    String errorMsg = "";
    final String seeAll = checkAuthorizedToSeeAll();
    try {
      getValid = DbTaskRunner.getFilterPrepareStatement(dbSession, 0,
                                                        // 0 means no limit
                                                        true, null, null,
                                                        tstart, tstop, rule,
                                                        req, pending, transfer,
                                                        error, done, all,
                                                        seeAll);
      nbAndSpecialId = DbTaskRunner.writeXMLWriter(getValid, filename);
    } catch (final WaarpDatabaseNoConnectionException e1) {
      isexported = false;
      toPurge = false;
      logger.warn(EXPORT_ERROR, e1.getMessage());
      errorMsg = e1.getMessage();
    } catch (final WaarpDatabaseSqlException e1) {
      isexported = false;
      toPurge = false;
      logger.warn(EXPORT_ERROR, e1.getMessage());
      errorMsg = e1.getMessage();
    } catch (final OpenR66ProtocolBusinessException e) {
      isexported = false;
      toPurge = false;
      logger.warn(EXPORT_ERROR, e.getMessage());
      errorMsg = e.getMessage();
    } finally {
      if (getValid != null) {
        getValid.realClose();
      }
    }
    int purge = 0;
    if (isexported && nbAndSpecialId != null) {
      if (nbAndSpecialId.nb <= 0) {
        return body.replace(XXXRESULTXXX, Messages.getString(
            "HttpSslHandler.7")); //$NON-NLS-1$
      }
      // in case of purge
      if (toPurge) {
        // purge with same filter all runners where globallasttep
        // is ALLDONE or ERROR
        // but getting the higher Special first
        final String stopId = Long.toString(nbAndSpecialId.higherSpecialId);
        try {
          purge = DbTaskRunner.purgeLogPrepareStatement(dbSession, null, stopId,
                                                        tstart, tstop, rule,
                                                        req, pending, transfer,
                                                        error, done, all);
        } catch (final WaarpDatabaseNoConnectionException ignored) {
          // nothing
        } catch (final WaarpDatabaseSqlException e) {
          logger.warn("Purge error: {}", e.getMessage());
        }
      }
    }
    return body.replace(XXXRESULTXXX, "Export " + (isexported?
        "<B>" + Messages.getString("HttpSslHandler.8") + //$NON-NLS-2$
        filename + Messages.getString("HttpSslHandler.9") +
        (nbAndSpecialId != null? nbAndSpecialId.nb : 0) +
        Messages.getString("HttpSslHandler.10")
        //$NON-NLS-1$ //$NON-NLS-2$
        + purge + Messages.getString("HttpSslHandler.11") + "</B>" :
        //$NON-NLS-1$
        "<B>" + Messages.getString("HttpSslHandler.12"))) + "</B>" +
           errorMsg; //$NON-NLS-1$
  }

  final String resetOptionHosts(final String header, final String host,
                                final String addr, final boolean ssl,
                                final boolean active) {
    final StringBuilder builder = new StringBuilder(header);
    WaarpStringUtils.replace(builder, "XXXFHOSTXXX", host);
    WaarpStringUtils.replace(builder, "XXXFADDRXXX", addr);
    WaarpStringUtils.replace(builder, "XXXFSSLXXX", ssl? CHECKED : "");
    WaarpStringUtils.replace(builder, "XXXFACTIVXXX", active? CHECKED : "");
    return builder.toString();
  }

  private String hosts0() {
    getParams();
    String head = REQUEST.Hosts.readHeader(this);
    final String end;
    end = REQUEST.Hosts.readEnd();
    if (params == null) {
      head = resetOptionHosts(head, "", "", false, true);
      return head + end;
    }
    String body0;
    String body;
    String body1;
    body0 = body1 = body = "";
    final List<String> parms = params.get(ACTION);
    if (parms != null) {
      body0 = REQUEST.Hosts.readBodyHeader();
      final String parm = parms.get(0);
      if (CREATE.equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        final String addr = getTrimValue(ADDRESS);
        final String port = getTrimValue("port");
        final String key = getTrimValue("hostkey");
        final boolean ssl;
        final boolean adminArg;
        final boolean isclient;
        final boolean isactive;
        final boolean isproxified;
        ssl = params.containsKey("ssl");
        adminArg = params.containsKey("admin");
        isclient = params.containsKey("isclient");
        isactive = params.containsKey("isactive");
        isproxified = params.containsKey("isproxified");
        if (host == null || addr == null || port == null || key == null) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.13"); //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", ssl, isactive);
          return head + body0 + body + body1 + end;
        }
        final int iport;
        try {
          iport = Integer.parseInt(port);
        } catch (final NumberFormatException e1) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.14") + e1.getMessage() +
                 B_CENTER_P; //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", ssl, isactive);
          return head + body0 + body + body1 + end;
        }
        final DbHostAuth dbhost;
        try {
          dbhost = new DbHostAuth(host, addr, iport, ssl,
                                  key.getBytes(WaarpStringUtils.UTF8), adminArg,
                                  isclient);
          dbhost.setActive(isactive);
          dbhost.setProxified(isproxified);
          dbhost.insert();
        } catch (final WaarpDatabaseException e) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.14") + e.getMessage()
                 //$NON-NLS-1$
                 + B_CENTER_P;
          head = resetOptionHosts(head, "", "", ssl, isactive);
          return head + body0 + body + body1 + end;
        }
        head = resetOptionHosts(head, host, addr, ssl, isactive);
        body = REQUEST.Hosts.readBody();
        body = dbhost.toSpecializedHtml(authentHttp, body, false);
      } else if (FILTER.equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        final String addr = getTrimValue(ADDRESS);
        final boolean ssl = params.containsKey("ssl");
        final boolean isactive = params.containsKey("active");
        head = resetOptionHosts(head, host == null? "" : host,
                                addr == null? "" : addr, ssl, isactive);
        body = REQUEST.Hosts.readBody();
        DbPreparedStatement preparedStatement = null;
        try {
          preparedStatement =
              DbHostAuth.getFilterPrepareStament(dbSession, host, addr, ssl,
                                                 isactive);
          preparedStatement.executeQuery();
          final StringBuilder builder = new StringBuilder();
          int i = 0;
          while (preparedStatement.getNext()) {
            i++;
            final DbHostAuth dbhost =
                DbHostAuth.getFromStatement(preparedStatement);
            builder.append(dbhost.toSpecializedHtml(authentHttp, body, false));
            if (i > getLimitRow()) {
              break;
            }
          }
          preparedStatement.realClose();
          body = builder.toString();
        } catch (final WaarpDatabaseException e) {
          if (preparedStatement != null) {
            preparedStatement.realClose();
          }
          logger.warn(OPEN_R66_WEB_ERROR, e.getMessage());
        }
        body1 = REQUEST.Hosts.readBodyEnd();
      } else if ("Update".equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        final String addr = getTrimValue(ADDRESS);
        final String port = getTrimValue("port");
        final String key = getTrimValue("hostkey");
        final boolean ssl;
        final boolean adminArg;
        final boolean isclient;
        final boolean isactive;
        final boolean isproxified;
        ssl = params.containsKey("ssl");
        adminArg = params.containsKey("admin");
        isclient = params.containsKey("isclient");
        isactive = params.containsKey("isactive");
        isproxified = params.containsKey("isproxified");
        if (host == null || addr == null || port == null || key == null) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.15"); //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", ssl, isactive);
          return head + body0 + body + body1 + end;
        }
        final int iport;
        try {
          iport = Integer.parseInt(port);
        } catch (final NumberFormatException e1) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.16") + e1.getMessage() +
                 B_CENTER_P; //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", ssl, isactive);
          return head + body0 + body + body1 + end;
        }
        final DbHostAuth dbhost;
        try {
          dbhost = new DbHostAuth(host, addr, iport, ssl,
                                  key.getBytes(WaarpStringUtils.UTF8), adminArg,
                                  isclient);
          dbhost.setActive(isactive);
          dbhost.setProxified(isproxified);
          if (dbhost.exist()) {
            dbhost.update();
          } else {
            dbhost.insert();
          }
        } catch (final WaarpDatabaseException e) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.16") + e.getMessage()
                 //$NON-NLS-1$
                 + B_CENTER_P;
          head = resetOptionHosts(head, "", "", ssl, isactive);
          return head + body0 + body + body1 + end;
        }
        head = resetOptionHosts(head, host, addr, ssl, isactive);
        body = REQUEST.Hosts.readBody();
        body = dbhost.toSpecializedHtml(authentHttp, body, false);
      } else if ("TestConn".equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        if (ParametersChecker.isEmpty(host)) {
          body0 = body1 = body = "";
          body = Messages.getString(HTTP_SSL_HANDLER_17); //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", false, true);
          return head + body0 + body + body1 + end;
        }
        final DbHostAuth dbhost;
        try {
          dbhost = new DbHostAuth(host);
        } catch (final WaarpDatabaseException e) {
          body0 = body1 = body = "";
          body = Messages.getString(HTTP_SSL_HANDLER_17) + e.getMessage()
                 //$NON-NLS-1$
                 + B_CENTER_P;
          head = resetOptionHosts(head, "", "", false, true);
          return head + body0 + body + body1 + end;
        }
        final R66Future result = new R66Future(true);
        final TestPacket packet = new TestPacket("MSG", "CheckConnection", 100);
        final Message transaction = new Message(
            Configuration.configuration.getInternalRunner()
                                       .getNetworkTransaction(), result, dbhost,
            packet);
        transaction.run();
        result.awaitOrInterruptible();
        head =
            resetOptionHosts(head, "", "", dbhost.isSsl(), dbhost.isActive());
        body = REQUEST.Hosts.readBody();
        if (result.isSuccess()) {
          body = dbhost.toSpecializedHtml(authentHttp, body, false);
          body += Messages.getString("HttpSslHandler.18"); //$NON-NLS-1$
        } else {
          body = dbhost.toSpecializedHtml(authentHttp, body, false);
          body += Messages.getString("HttpSslHandler.19")//$NON-NLS-1$
                  + result.getResult().getCode().getMesg() + B_CENTER_P;
        }
      } else if ("CloseConn".equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        if (ParametersChecker.isEmpty(host)) {
          body0 = body1 = body = "";
          body = Messages.getString(HTTP_SSL_HANDLER_17); //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", false, true);
          return head + body0 + body + body1 + end;
        }
        final DbHostAuth dbhost;
        try {
          dbhost = new DbHostAuth(host);
        } catch (final WaarpDatabaseException e) {
          body0 = body1 = body = "";
          body = Messages.getString(HTTP_SSL_HANDLER_17) + e.getMessage()
                 //$NON-NLS-1$
                 + B_CENTER_P;
          head = resetOptionHosts(head, "", "", false, true);
          return head + body0 + body + body1 + end;
        }
        body = REQUEST.Hosts.readBody();
        final boolean resultShutDown =
            NetworkTransaction.shuttingdownNetworkChannelsPerHostID(
                dbhost.getHostid());
        head =
            resetOptionHosts(head, "", "", dbhost.isSsl(), dbhost.isActive());
        if (resultShutDown) {
          body = dbhost.toSpecializedHtml(authentHttp, body, false);
          body += Messages.getString("HttpSslHandler.21"); //$NON-NLS-1$
        } else {
          body = dbhost.toSpecializedHtml(authentHttp, body, false);
          body += Messages.getString("HttpSslHandler.22"); //$NON-NLS-1$
        }
      } else if ("Delete".equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        if (ParametersChecker.isEmpty(host)) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.23"); //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", false, true);
          return head + body0 + body + body1 + end;
        }
        final DbHostAuth dbhost;
        try {
          dbhost = new DbHostAuth(host);
        } catch (final WaarpDatabaseException e) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.24") + e.getMessage()
                 //$NON-NLS-1$
                 + B_CENTER_P;
          head = resetOptionHosts(head, "", "", false, true);
          return head + body0 + body + body1 + end;
        }
        try {
          dbhost.delete();
        } catch (final WaarpDatabaseException e) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.24") + e.getMessage()
                 //$NON-NLS-1$
                 + B_CENTER_P;
          head =
              resetOptionHosts(head, "", "", dbhost.isSsl(), dbhost.isActive());
          return head + body0 + body + body1 + end;
        }
        body0 = body1 = body = "";
        body = Messages.getString("HttpSslHandler.25") + host +
               B_CENTER_P; //$NON-NLS-1$
        head = resetOptionHosts(head, "", "", false, dbhost.isActive());
        return head + body0 + body + body1 + end;
      } else {
        head = resetOptionHosts(head, "", "", false, true);
      }
      body1 = REQUEST.Hosts.readBodyEnd();
    } else {
      head = resetOptionHosts(head, "", "", false, true);
    }
    return head + body0 + body + body1 + end;
  }

  private void createExport(final String body, final StringBuilder builder,
                            final String rule, final int mode, final int limit,
                            final int start) {
    DbPreparedStatement preparedStatement = null;
    try {
      preparedStatement = DbRule.getFilterPrepareStament(dbSession, rule, mode);
      preparedStatement.executeQuery();
      int i = 0;
      while (preparedStatement.getNext()) {
        final DbRule dbrule = DbRule.getFromStatement(preparedStatement);
        String temp = dbrule.toSpecializedHtml(authentHttp, body);
        temp = temp.replace("XXXRANKXXX", String.valueOf(start + i));
        builder.append(temp);
        i++;
        if (i > limit) {
          break;
        }
      }
      preparedStatement.realClose();
    } catch (final WaarpDatabaseException e) {
      if (preparedStatement != null) {
        preparedStatement.realClose();
      }
      logger.warn(OPEN_R66_WEB_ERROR, e.getMessage());
    }
  }

  final String resetOptionRules(final String header, final String rule,
                                final RequestPacket.TRANSFERMODE mode,
                                final int gmode) {
    final StringBuilder builder = new StringBuilder(header);
    WaarpStringUtils.replace(builder, "XXXRULEXXX", rule);
    if (mode != null) {
      switch (mode) {
        case RECVMODE:
          WaarpStringUtils.replace(builder, "XXXRECVXXX", CHECKED);
          break;
        case SENDMODE:
          WaarpStringUtils.replace(builder, "XXXSENDXXX", CHECKED);
          break;
        case RECVMD5MODE:
          WaarpStringUtils.replace(builder, "XXXRECVMXXX", CHECKED);
          break;
        case SENDMD5MODE:
          WaarpStringUtils.replace(builder, "XXXSENDMXXX", CHECKED);
          break;
        case RECVTHROUGHMODE:
          WaarpStringUtils.replace(builder, "XXXRECVTXXX", CHECKED);
          break;
        case SENDTHROUGHMODE:
          WaarpStringUtils.replace(builder, "XXXSENDTXXX", CHECKED);
          break;
        case RECVMD5THROUGHMODE:
          WaarpStringUtils.replace(builder, "XXXRECVMTXXX", CHECKED);
          break;
        case SENDMD5THROUGHMODE:
          WaarpStringUtils.replace(builder, "XXXSENDMTXXX", CHECKED);
          break;
        default:
          break;
      }
    }
    if (gmode == -1) {// All Recv
      WaarpStringUtils.replace(builder, "XXXARECVXXX", CHECKED);
    } else if (gmode == -2) {// All Send
      WaarpStringUtils.replace(builder, "XXXASENDXXX", CHECKED);
    } else if (gmode == -3) {// All
      WaarpStringUtils.replace(builder, "XXXALLXXX", CHECKED);
    }
    return builder.toString();
  }

  private String rules0() {
    getParams();
    String head = REQUEST.Rules.readHeader(this);
    final String end;
    end = REQUEST.Rules.readEnd();
    if (params == null) {
      head = resetOptionRules(head, "", null, -3);
      return head + end;
    }
    String body0;
    String body;
    String body1;
    body0 = body1 = body = "";
    final List<String> parms = params.get(ACTION);
    if (parms != null) {
      body0 = REQUEST.Rules.readBodyHeader();
      final String parm = parms.get(0);
      if (CREATE.equalsIgnoreCase(parm) || "Update".equalsIgnoreCase(parm)) {
        final String rule = getTrimValue("rule");
        final String hostids = getTrimValue("hostids");
        final String recvp = getTrimValue("recvp");
        final String sendp = getTrimValue("sendp");
        final String archp = getTrimValue("archp");
        final String workp = getTrimValue("workp");
        final String rpre = getTrimValue("rpre");
        final String rpost = getTrimValue("rpost");
        final String rerr = getTrimValue("rerr");
        final String spre = getTrimValue("spre");
        final String spost = getTrimValue("spost");
        final String serr = getTrimValue("serr");
        final String mode = getTrimValue("mode");
        if (rule == null || mode == null) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.26") + parm +
                 Messages.getString(
                     "HttpSslHandler.27"); //$NON-NLS-1$ //$NON-NLS-2$
          head = resetOptionRules(head, "", null, -3);
          return head + body0 + body + body1 + end;
        }
        int gmode = 0;

        TRANSFERMODE tmode = null;
        if ("send".equals(mode)) {
          tmode = RequestPacket.TRANSFERMODE.SENDMODE;
          gmode = -2;
        } else if ("recv".equals(mode)) {
          tmode = RequestPacket.TRANSFERMODE.RECVMODE;
          gmode = -1;
        } else if ("sendmd5".equals(mode)) {
          tmode = RequestPacket.TRANSFERMODE.SENDMD5MODE;
          gmode = -2;
        } else if ("recvmd5".equals(mode)) {
          tmode = RequestPacket.TRANSFERMODE.RECVMD5MODE;
          gmode = -1;
        } else if ("sendth".equals(mode)) {
          tmode = RequestPacket.TRANSFERMODE.SENDTHROUGHMODE;
          gmode = -2;
        } else if ("recvth".equals(mode)) {
          tmode = RequestPacket.TRANSFERMODE.RECVTHROUGHMODE;
          gmode = -1;
        } else if ("sendthmd5".equals(mode)) {
          tmode = RequestPacket.TRANSFERMODE.SENDMD5THROUGHMODE;
          gmode = -2;
        } else if ("recvthmd5".equals(mode)) {
          tmode = RequestPacket.TRANSFERMODE.RECVMD5THROUGHMODE;
          gmode = -1;
        }
        head = resetOptionRules(head, rule, tmode, gmode);
        if (logger.isDebugEnabled()) {
          logger.debug("Recv UpdOrInsert: " + rule + ':' + hostids + ':' +
                       (tmode != null? tmode.ordinal() : 0) + ':' + recvp +
                       ':' + sendp + ':' + archp + ':' + workp + ':' + rpre +
                       ':' + rpost + ':' + rerr + ':' + spre + ':' + spost +
                       ':' + serr);
        }
        final DbRule dbrule;
        try {
          dbrule =
              new DbRule(rule, hostids, (tmode != null? tmode.ordinal() : 0),
                         recvp, sendp, archp, workp, rpre, rpost, rerr, spre,
                         spost, serr);
          if (CREATE.equalsIgnoreCase(parm)) {
            dbrule.insert();
          } else {
            if (dbrule.exist()) {
              dbrule.update();
            } else {
              dbrule.insert();
            }
          }
        } catch (final WaarpDatabaseException e) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.28") + e.getMessage()
                 //$NON-NLS-1$
                 + B_CENTER_P;
          head = resetOptionRules(head, "", null, -3);
          return head + body0 + body + body1 + end;
        }
        body = REQUEST.Rules.readBody();
        body = dbrule.toSpecializedHtml(authentHttp, body);
      } else if (FILTER.equalsIgnoreCase(parm)) {
        final String rule = getTrimValue("rule");
        final String mode = getTrimValue("mode");
        TRANSFERMODE tmode;
        int gmode = 0;
        if ("all".equals(mode)) {
          gmode = -3;
        } else if ("send".equals(mode)) {
          gmode = -2;
        } else if ("recv".equals(mode)) {
          gmode = -1;
        }
        head = resetOptionRules(head, rule == null? "" : rule, null, gmode);
        body = REQUEST.Rules.readBody();
        final StringBuilder builder = new StringBuilder();
        boolean specific = false;
        int start = 1;
        if (params.containsKey("send")) {
          tmode = RequestPacket.TRANSFERMODE.SENDMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(body, builder, rule,
                       RequestPacket.TRANSFERMODE.SENDMODE.ordinal(),
                       getLimitRow() / 4, start);
          start += getLimitRow() / 4 + 1;
        }
        if (params.containsKey("recv")) {
          tmode = RequestPacket.TRANSFERMODE.RECVMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(body, builder, rule,
                       RequestPacket.TRANSFERMODE.RECVMODE.ordinal(),
                       getLimitRow() / 4, start);
          start += getLimitRow() / 4 + 1;
        }
        if (params.containsKey("sendmd5")) {
          tmode = RequestPacket.TRANSFERMODE.SENDMD5MODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(body, builder, rule,
                       RequestPacket.TRANSFERMODE.SENDMD5MODE.ordinal(),
                       getLimitRow() / 4, start);
          start += getLimitRow() / 4 + 1;
        }
        if (params.containsKey("recvmd5")) {
          tmode = RequestPacket.TRANSFERMODE.RECVMD5MODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(body, builder, rule,
                       RequestPacket.TRANSFERMODE.RECVMD5MODE.ordinal(),
                       getLimitRow() / 4, start);
          start += getLimitRow() / 4 + 1;
        }
        if (params.containsKey("sendth")) {
          tmode = RequestPacket.TRANSFERMODE.SENDTHROUGHMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(body, builder, rule,
                       RequestPacket.TRANSFERMODE.SENDTHROUGHMODE.ordinal(),
                       getLimitRow() / 4, start);
          start += getLimitRow() / 4 + 1;
        }
        if (params.containsKey("recvth")) {
          tmode = RequestPacket.TRANSFERMODE.RECVTHROUGHMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(body, builder, rule,
                       RequestPacket.TRANSFERMODE.RECVTHROUGHMODE.ordinal(),
                       getLimitRow() / 4, start);
          start += getLimitRow() / 4 + 1;
        }
        if (params.containsKey("sendthmd5")) {
          tmode = RequestPacket.TRANSFERMODE.SENDMD5THROUGHMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(body, builder, rule,
                       RequestPacket.TRANSFERMODE.SENDMD5THROUGHMODE.ordinal(),
                       getLimitRow() / 4, start);
          start += getLimitRow() / 4 + 1;
        }
        if (params.containsKey("recvthmd5")) {
          tmode = RequestPacket.TRANSFERMODE.RECVMD5THROUGHMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(body, builder, rule,
                       RequestPacket.TRANSFERMODE.RECVMD5THROUGHMODE.ordinal(),
                       getLimitRow() / 4, start);
          start += getLimitRow() / 4 + 1;
        }
        if (!specific) {
          if (gmode == -1) {
            // recv
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.RECVMODE.ordinal(),
                         getLimitRow() / 4, start);
            start += getLimitRow() / 4 + 1;
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.RECVMD5MODE.ordinal(),
                         getLimitRow() / 4, start);
            start += getLimitRow() / 4 + 1;
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.RECVTHROUGHMODE.ordinal(),
                         getLimitRow() / 4, start);
            start += getLimitRow() / 4 + 1;
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.RECVMD5THROUGHMODE.ordinal(),
                         getLimitRow() / 4, start);
            start += getLimitRow() / 4 + 1;
          } else if (gmode == -2) {
            // send
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.SENDMODE.ordinal(),
                         getLimitRow() / 4, start);
            start += getLimitRow() / 4 + 1;
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.SENDMD5MODE.ordinal(),
                         getLimitRow() / 4, start);
            start += getLimitRow() / 4 + 1;
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.SENDTHROUGHMODE.ordinal(),
                         getLimitRow() / 4, start);
            start += getLimitRow() / 4 + 1;
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.SENDMD5THROUGHMODE.ordinal(),
                         getLimitRow() / 4, start);
            start += getLimitRow() / 4 + 1;
          } else {
            // all
            createExport(body, builder, rule, -1, getLimitRow(), start);
            start += getLimitRow() + 1;
          }
        }
        body = builder.toString();
        body1 = REQUEST.Rules.readBodyEnd();
      } else if ("Delete".equalsIgnoreCase(parm)) {
        final String rule = getTrimValue("rule");
        if (ParametersChecker.isEmpty(rule)) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.29"); //$NON-NLS-1$
          head = resetOptionRules(head, "", null, -3);
          return head + body0 + body + body1 + end;
        }
        final DbRule dbrule;
        try {
          dbrule = new DbRule(rule);
        } catch (final WaarpDatabaseException e) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.30") + e.getMessage()
                 //$NON-NLS-1$
                 + B_CENTER_P;
          head = resetOptionRules(head, "", null, -3);
          return head + body0 + body + body1 + end;
        }
        try {
          dbrule.delete();
        } catch (final WaarpDatabaseException e) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.30") + e.getMessage()
                 //$NON-NLS-1$
                 + B_CENTER_P;
          head = resetOptionRules(head, "", null, -3);
          return head + body0 + body + body1 + end;
        }
        body0 = body1 = body = "";
        body = Messages.getString("HttpSslHandler.31") + rule +
               B_CENTER_P; //$NON-NLS-1$
        head = resetOptionRules(head, "", null, -3);
        return head + body0 + body + body1 + end;
      } else {
        head = resetOptionRules(head, "", null, -3);
      }
      body1 = REQUEST.Rules.readBodyEnd();
    } else {
      head = resetOptionRules(head, "", null, -3);
    }
    return head + body0 + body + body1 + end;
  }

  private String spooled0(final boolean detailed) {
    // XXXSPOOLEDXXX
    final String spooled = REQUEST.Spooled.readFileUnique(this);
    String uri;
    if (detailed) {
      uri = "SpooledDetailed.html";
    } else {
      uri = "Spooled.html";
    }
    final QueryStringDecoder queryStringDecoder =
        new QueryStringDecoder(request.uri());
    params = queryStringDecoder.parameters();
    String name = null;
    if (params != null && params.containsKey("name")) {
      name = getTrimValue("name");
    }
    int istatus = 0;
    if (params != null && params.containsKey("status")) {
      final String status = getTrimValue("status");
      try {
        istatus = Integer.parseInt(status);
      } catch (final NumberFormatException e1) {
        istatus = 0;
      }
    }
    if (ParametersChecker.isNotEmpty(name)) {
      // name is specified
      uri = request.uri();
      if (istatus != 0) {
        uri += "&status=" + istatus;
      }
      final StringBuilder builder =
          SpooledInformTask.buildSpooledUniqueTable(uri, name);
      return spooled.replace("XXXSPOOLEDXXX", builder.toString());
    } else {
      if (istatus != 0) {
        uri += "&status=" + istatus;
      }
      final StringBuilder builder =
          SpooledInformTask.buildSpooledTable(detailed, istatus, uri);
      return spooled.replace("XXXSPOOLEDXXX", builder.toString());
    }
  }

  /**
   * Applied current lang to system page
   *
   * @param builder
   */
  protected void langHandle(final StringBuilder builder) {
    // i18n: add here any new languages
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURLANGENXXX.name(),
                             "en".equalsIgnoreCase(lang)? CHECKED : "");
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURLANGFRXXX.name(),
                             "fr".equalsIgnoreCase(lang)? CHECKED : "");
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURSYSLANGENXXX.name(),
                             "en".equalsIgnoreCase(Messages.getSlocale())?
                                 CHECKED : "");
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURSYSLANGFRXXX.name(),
                             "fr".equalsIgnoreCase(Messages.getSlocale())?
                                 CHECKED : "");
  }

  private String systemLimitedSource0() {
    final String system = REQUEST.SystemLimited.readFileUnique(this);
    if (ParametersChecker.isEmpty(system)) {
      return REQUEST.System.readFileUnique(this);
    }
    return system;
  }

  final String systemLimited() {
    getParams();
    DbHostConfiguration config = null;
    try {
      config = new DbHostConfiguration(Configuration.configuration.getHostId());
    } catch (final WaarpDatabaseException e2) {
      try {
        config =
            new DbHostConfiguration(Configuration.configuration.getHostId(), "",
                                    "", "", "");
        config.insert();
      } catch (final WaarpDatabaseException ignored) {
        // nothing
      }
    }
    if (params == null) {
      final String system = systemLimitedSource0();
      final StringBuilder builder = new StringBuilder(system);
      replaceStringSystem(config, builder);
      langHandle(builder);
      return builder.toString();
    }
    String extraInformation = null;
    if (params.containsKey(ACTION)) {
      final List<String> action = params.get(ACTION);
      for (final String act : action) {
        if ("Language".equalsIgnoreCase(act)) {
          lang = getTrimValue("change");
          extraInformation =
              Messages.getString(HTTP_SSL_HANDLER_LANG_IS) + "Web: " + lang +
              " OpenR66: " //$NON-NLS-1$
              + Messages.getSlocale();
        } else if ("Disconnect".equalsIgnoreCase(act)) {
          String logon = logon();
          logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                                   Messages.getString(
                                       "HttpSslHandler.DisActive"));
          newSession = true;
          clearSession();
          forceClose = true;
          return logon;
        }
      }
    }
    final String system = systemLimitedSource0();
    final StringBuilder builder = new StringBuilder(system);
    replaceStringSystem(config, builder);
    langHandle(builder);
    if (extraInformation != null) {
      builder.append(extraInformation);
    }
    return builder.toString();
  }

  /**
   * @param config
   * @param builder
   */
  final void replaceStringSystem(final DbHostConfiguration config,
                                 final StringBuilder builder) {
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXBUSINESSXXX.toString(),
                             config.getBusiness());
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXROLESXXX.toString(),
                             config.getRoles());
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXALIASESXXX.toString(),
                             config.getAliases());
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXOTHERXXX.toString(),
                             config.getOthers());
    WaarpStringUtils.replace(builder,
                             REPLACEMENT.XXXXSESSIONLIMITWXXX.toString(),
                             Long.toString(
                                 Configuration.configuration.getServerChannelWriteLimit()));
    WaarpStringUtils.replace(builder,
                             REPLACEMENT.XXXXSESSIONLIMITRXXX.toString(),
                             Long.toString(
                                 Configuration.configuration.getServerChannelReadLimit()));
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXDELATRAFFICXXX.toString(),
                             Long.toString(
                                 Configuration.configuration.getDelayLimit()));
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXDELAYCOMMDXXX.toString(),
                             Long.toString(
                                 Configuration.configuration.getDelayCommander()));
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXDELAYRETRYXXX.toString(),
                             Long.toString(
                                 Configuration.configuration.getDelayRetry()));
    WaarpStringUtils.replace(builder,
                             REPLACEMENT.XXXXCHANNELLIMITWXXX.toString(),
                             Long.toString(
                                 Configuration.configuration.getServerGlobalWriteLimit()));
    WaarpStringUtils.replace(builder,
                             REPLACEMENT.XXXXCHANNELLIMITRXXX.toString(),
                             Long.toString(
                                 Configuration.configuration.getServerGlobalReadLimit()));
    WaarpStringUtils.replace(builder, "XXXBLOCKXXX",
                             Configuration.configuration.isShutdown()? CHECKED :
                                 "");
    switch (WaarpLoggerFactory.getLogLevel()) {
      case DEBUG:
        WaarpStringUtils.replace(builder, XXXLEVEL1XXX, CHECKED);
        WaarpStringUtils.replace(builder, XXXLEVEL2XXX, "");
        WaarpStringUtils.replace(builder, XXXLEVEL3XXX, "");
        WaarpStringUtils.replace(builder, XXXLEVEL4XXX, "");
        break;
      case INFO:
        WaarpStringUtils.replace(builder, XXXLEVEL1XXX, "");
        WaarpStringUtils.replace(builder, XXXLEVEL2XXX, CHECKED);
        WaarpStringUtils.replace(builder, XXXLEVEL3XXX, "");
        WaarpStringUtils.replace(builder, XXXLEVEL4XXX, "");
        break;
      case WARN:
        WaarpStringUtils.replace(builder, XXXLEVEL1XXX, "");
        WaarpStringUtils.replace(builder, XXXLEVEL2XXX, "");
        WaarpStringUtils.replace(builder, XXXLEVEL3XXX, CHECKED);
        WaarpStringUtils.replace(builder, XXXLEVEL4XXX, "");
        break;
      case ERROR:
        WaarpStringUtils.replace(builder, XXXLEVEL1XXX, "");
        WaarpStringUtils.replace(builder, XXXLEVEL2XXX, "");
        WaarpStringUtils.replace(builder, XXXLEVEL3XXX, "");
        WaarpStringUtils.replace(builder, XXXLEVEL4XXX, CHECKED);
        break;
      default:
        WaarpStringUtils.replace(builder, XXXLEVEL1XXX, "");
        WaarpStringUtils.replace(builder, XXXLEVEL2XXX, "");
        WaarpStringUtils.replace(builder, XXXLEVEL3XXX, "");
        WaarpStringUtils.replace(builder, XXXLEVEL4XXX, "");
        break;

    }
  }

  final String logout() {
    String logon = logon();
    logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                             Messages.getString("HttpSslHandler.Disconnected"));
    newSession = true;
    clearSession();
    forceClose = true;
    return logon;
  }

  private String system0() {
    getParams();
    DbHostConfiguration config = null;
    try {
      config = new DbHostConfiguration(Configuration.configuration.getHostId());
    } catch (final WaarpDatabaseException e2) {
      try {
        config =
            new DbHostConfiguration(Configuration.configuration.getHostId(), "",
                                    "", "", "");
        config.insert();
      } catch (final WaarpDatabaseException ignored) {
        // nothing
      }
    }
    if (params == null) {
      final String system = REQUEST.System.readFileUnique(this);
      final StringBuilder builder = new StringBuilder(system);
      replaceStringSystem(config, builder);
      langHandle(builder);
      return builder.toString();
    }
    String extraInformation = null;
    if (params.containsKey(ACTION)) {
      final List<String> action = params.get(ACTION);
      for (final String act : action) {
        if ("Language".equalsIgnoreCase(act)) {
          lang = getTrimValue("change");
          final String sys = getTrimValue("changesys");
          Messages.init(new Locale(sys));
          extraInformation =
              Messages.getString(HTTP_SSL_HANDLER_LANG_IS) + "Web: " + lang +
              " OpenR66: " //$NON-NLS-1$
              + Messages.getSlocale();
        } else if ("Level".equalsIgnoreCase(act)) {
          final String loglevel = getTrimValue("loglevel");
          WaarpLogLevel level = WaarpLogLevel.WARN;
          if ("debug".equalsIgnoreCase(loglevel)) {
            level = WaarpLogLevel.DEBUG;
          } else if ("info".equalsIgnoreCase(loglevel)) {
            level = WaarpLogLevel.INFO;
          } else if ("warn".equalsIgnoreCase(loglevel)) {
            level = WaarpLogLevel.WARN;
          } else if (ERROR2.equalsIgnoreCase(loglevel)) {
            level = WaarpLogLevel.ERROR;
          }
          WaarpLoggerFactory.setLogLevel(level);
          extraInformation = Messages.getString(HTTP_SSL_HANDLER_LANG_IS) +
                             level.name(); //$NON-NLS-1$
        } else if ("ExportConfig".equalsIgnoreCase(act)) {
          final String directory =
              Configuration.configuration.getBaseDirectory() +
              DirInterface.SEPARATOR +
              Configuration.configuration.getArchivePath();
          extraInformation =
              Messages.getString("HttpSslHandler.ExportDir") + directory +
              "<br>"; //$NON-NLS-1$
          final String[] filenames =
              ServerActions.staticConfigExport(directory, true, true, true,
                                               true, true);
          // hosts, rules, business, alias, roles
          if (filenames[0] != null) {
            extraInformation +=
                Messages.getString("HttpSslHandler.33"); //$NON-NLS-1$
          }
          if (filenames[1] != null) {
            extraInformation +=
                Messages.getString("HttpSslHandler.32"); //$NON-NLS-1$
          }
          if (filenames[2] != null) {
            extraInformation +=
                Messages.getString("HttpSslHandler.44"); //$NON-NLS-1$
          }
          if (filenames[3] != null) {
            extraInformation +=
                Messages.getString("HttpSslHandler.45"); //$NON-NLS-1$
          }
          if (filenames[4] != null) {
            extraInformation +=
                Messages.getString("HttpSslHandler.46"); //$NON-NLS-1$
          }
        } else if ("Disconnect".equalsIgnoreCase(act)) {
          return logout();
        } else if ("Block".equalsIgnoreCase(act)) {
          final boolean block = params.containsKey("blocking");
          if (block) {
            extraInformation =
                Messages.getString("HttpSslHandler.34"); //$NON-NLS-1$
          } else {
            extraInformation =
                Messages.getString("HttpSslHandler.35"); //$NON-NLS-1$
          }
          Configuration.configuration.setShutdown(block);
        } else if ("Shutdown".equalsIgnoreCase(act)) {
          final String error;
          if (Configuration.configuration.getShutdownConfiguration().serviceFuture !=
              null) {
            error =
                error(Messages.getString("HttpSslHandler.38")); //$NON-NLS-1$
          } else {
            error =
                error(Messages.getString("HttpSslHandler.37")); //$NON-NLS-1$
          }
          WaarpShutdownHook.setRestart(false);
          newSession = true;
          clearSession();
          forceClose = true;
          shutdown = true;
          return error;
        } else if ("Restart".equalsIgnoreCase(act)) {
          String error;
          if (Configuration.configuration.getShutdownConfiguration().serviceFuture !=
              null) {
            error =
                error(Messages.getString("HttpSslHandler.38")); //$NON-NLS-1$
          } else {
            error = error(Messages.getString("HttpSslHandler.39")
                          //$NON-NLS-1$
                          + Configuration.configuration.getTimeoutCon() * 2 /
                            1000 + Messages.getString(
                "HttpSslHandler.40")); //$NON-NLS-1$
          }
          error = error.replace("XXXRELOADHTTPXXX",
                                "HTTP-EQUIV=\"refresh\" CONTENT=\"" +
                                Configuration.configuration.getTimeoutCon() *
                                2 / 1000 + '"');
          WaarpShutdownHook.setRestart(true);
          newSession = true;
          clearSession();
          forceClose = true;
          shutdown = true;
          return error;
        } else if ("Validate".equalsIgnoreCase(act)) {
          final String bsessionr = getTrimValue("BSESSR");
          long lsessionr =
              Configuration.configuration.getServerChannelReadLimit();
          long lglobalr;
          long lsessionw;
          long lglobalw;
          long delay;
          try {
            if (bsessionr != null) {
              lsessionr = (Long.parseLong(bsessionr) / 10) * 10;
            }
            final String bglobalr = getTrimValue("BGLOBR");
            lglobalr = Configuration.configuration.getServerGlobalReadLimit();
            if (bglobalr != null) {
              lglobalr = (Long.parseLong(bglobalr) / 10) * 10;
            }
            final String bsessionw = getTrimValue("BSESSW");
            lsessionw =
                Configuration.configuration.getServerChannelWriteLimit();
            if (bsessionw != null) {
              lsessionw = (Long.parseLong(bsessionw) / 10) * 10;
            }
            final String bglobalw = getTrimValue("BGLOBW");
            lglobalw = Configuration.configuration.getServerGlobalWriteLimit();
            if (bglobalw != null) {
              lglobalw = (Long.parseLong(bglobalw) / 10) * 10;
            }
            final String dtra = getTrimValue("DTRA");
            delay = Configuration.configuration.getDelayLimit();
            if (dtra != null) {
              delay = (Long.parseLong(dtra) / 10) * 10;
              if (delay < 100) {
                delay = 100;
              }
            }
            Configuration.configuration.changeNetworkLimit(lglobalw, lglobalr,
                                                           lsessionw, lsessionr,
                                                           delay);
            final String dcomm = getTrimValue("DCOM");
            if (dcomm != null) {
              Configuration.configuration.setDelayCommander(
                  Long.parseLong(dcomm));
              if (Configuration.configuration.getDelayCommander() <= 100) {
                Configuration.configuration.setDelayCommander(100);
              }
              Configuration.configuration.reloadCommanderDelay();
            }
            final String dret = getTrimValue("DRET");
            if (dret != null) {
              Configuration.configuration.setDelayRetry(Long.parseLong(dret));
              if (Configuration.configuration.getDelayRetry() <= 1000) {
                Configuration.configuration.setDelayRetry(1000);
              }
            }
            extraInformation =
                Messages.getString("HttpSslHandler.41"); //$NON-NLS-1$
          } catch (final NumberFormatException e) {
            extraInformation =
                Messages.getString("HttpSslHandler.42"); //$NON-NLS-1$
          }
        } else if ("HostConfig".equalsIgnoreCase(act)) {
          config.setBusiness(getTrimValue("BUSINESS"));
          config.setRoles(getTrimValue("ROLES"));
          config.setAliases(getTrimValue("ALIASES"));
          config.setOthers(getTrimValue("OTHER"));
          try {
            config.update();
            extraInformation =
                Messages.getString("HttpSslHandler.41"); //$NON-NLS-1$
          } catch (final WaarpDatabaseException e) {
            extraInformation =
                Messages.getString("HttpSslHandler.43"); //$NON-NLS-1$
          }
        }
      }
    }
    final String system = REQUEST.System.readFileUnique(this);
    final StringBuilder builder = new StringBuilder(system);
    replaceStringSystem(config, builder);
    langHandle(builder);
    if (extraInformation != null) {
      builder.append(extraInformation);
    }
    return builder.toString();
  }

  private void getParams() {
    if (request.method() == HttpMethod.GET) {
      params = null;
    } else if (request.method() == HttpMethod.POST) {
      final ByteBuf content = request.content();
      if (content.isReadable()) {
        final String param = content.toString(WaarpStringUtils.UTF8);
        final QueryStringDecoder queryStringDecoder2 =
            new QueryStringDecoder("/?" + param);
        params = queryStringDecoder2.parameters();
        boolean invalidEntry = false;
        for (final Entry<String, List<String>> paramCheck : params.entrySet()) {
          try {
            ParametersChecker.checkSanityString(paramCheck.getValue().toArray(
                ParametersChecker.ZERO_ARRAY_STRING));
          } catch (final InvalidArgumentException e) {
            logger.error(
                "Arguments incompatible with Security: " + paramCheck.getKey() +
                ": {}", e.getMessage());
            invalidEntry = true;
          }
        }
        if (invalidEntry) {
          for (final Entry<String, List<String>> paramCheck : params.entrySet()) {
            paramCheck.getValue().clear();
          }
          params.clear();
          params = null;
          logger.error("No parameter validated since security issue found");
          return;
        }
        if (params.containsKey(sLIMITROW)) {
          final String snb = getTrimValue(sLIMITROW);
          if (snb != null) {
            try {
              final int old = getLimitRow();
              setLimitRow(Integer.parseInt(snb));
              if (getLimitRow() < 5) {
                setLimitRow(old);
              }
            } catch (final Exception ignored) {
              // nothing
            }
          }
        }
      } else {
        params = null;
      }
    }
  }

  protected void clearSession() {
    if (admin != null) {
      final R66Session lsession = sessions.remove(admin.value());
      admin = null;
      if (lsession != null) {
        lsession.setStatus(75);
        lsession.clear();
      }
    }
  }

  private void checkAuthent(final ChannelHandlerContext ctx) {
    newSession = true;
    if (request.method() == HttpMethod.GET) {
      String logon = logon();
      logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(), "");
      responseContent.append(logon);
      clearSession();
      writeResponse(ctx);
      return;
    } else if (request.method() == HttpMethod.POST) {
      getParams();
      if (params == null) {
        String logon = logon();
        logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                                 Messages.getString(
                                     "HttpSslHandler.EmptyLogin"));
        responseContent.append(logon);
        clearSession();
        writeResponse(ctx);
        return;
      }
    }
    boolean getMenu = false;
    if (params != null && params.containsKey("Logon")) {
      String name = null;
      String password = null;
      List<String> values;
      if (!params.isEmpty()) {
        // get values
        if (params.containsKey("name")) {
          values = params.get("name");
          if (values != null) {
            name = values.get(0);
            if (ParametersChecker.isEmpty(name)) {
              getMenu = true;
            }
          }
        } else {
          getMenu = true;
        }
        // search the nb param
        if (!getMenu && params.containsKey("passwd")) {
          values = params.get("passwd");
          if (values != null) {
            password = values.get(0);
            getMenu = ParametersChecker.isEmpty(password);
          } else {
            getMenu = true;
          }
        } else {
          getMenu = true;
        }
      } else {
        getMenu = true;
      }
      if (!getMenu && name != null) {
        logger.debug("Name? {} Passwd? {}",
                     name.equals(Configuration.configuration.getAdminName()),
                     Arrays.equals(password.getBytes(WaarpStringUtils.UTF8),
                                   Configuration.configuration.getServerAdminKey()));
        if (name.equals(Configuration.configuration.getAdminName()) &&
            Arrays.equals(password.getBytes(WaarpStringUtils.UTF8),
                          Configuration.configuration.getServerAdminKey())) {
          authentHttp.getAuth().specialNoSessionAuth(true,
                                                     Configuration.configuration.getHostId());
          authentHttp.setStatus(70);
        } else {
          try {
            authentHttp.getAuth().connectionHttps(name,
                                                  FilesystemBasedDigest.passwdCrypt(
                                                      password.getBytes(
                                                          WaarpStringUtils.UTF8)));
          } catch (final Reply530Exception e1) {
            getMenu = true;
          } catch (final Reply421Exception e1) {
            getMenu = true;
          }
        }
        if (!authentHttp.isAuthenticated()) {
          authentHttp.setStatus(71);
          logger.info("Still not authenticated: {}", authentHttp);
          getMenu = true;
        }
        logger.debug("Identified: {}:{}", authentHttp.getAuth().isIdentified(),
                     authentHttp.isAuthenticated());
      }
    } else {
      getMenu = true;
    }
    if (getMenu) {
      String logon = logon();
      logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                               Messages.getString("HttpSslHandler.BadLogin"));
      responseContent.append(logon);
      clearSession();
    } else {
      final String index = index();
      responseContent.append(index);
      clearSession();
      admin = new DefaultCookie(
          R66SESSION + Configuration.configuration.getHostId(),
          Configuration.configuration.getHostId() +
          Long.toHexString(RANDOM.nextLong()));
      sessions.put(admin.value(), authentHttp);
      authentHttp.setStatus(72);
      logger.debug("CreateSession: {}:{}", uriRequest, admin);
    }
    writeResponse(ctx);
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx,
                              final FullHttpRequest msg) {
    final FullHttpRequest httpRequest = this.request = msg;
    final QueryStringDecoder queryStringDecoder =
        new QueryStringDecoder(httpRequest.uri());
    uriRequest = queryStringDecoder.path();
    logger.debug("Msg: {}", uriRequest);
    if (uriRequest.contains("gre/") || uriRequest.contains("img/") ||
        uriRequest.contains("res/") || uriRequest.contains("favicon.ico")) {
      HttpWriteCacheEnable.writeFile(httpRequest, ctx,
                                     Configuration.configuration.getHttpBasePath() +
                                     uriRequest, R66SESSION +
                                                 Configuration.configuration.getHostId());
      return;
    }
    checkSession(ctx.channel());
    try {
      if (!authentHttp.isAuthenticated()) {
        logger.debug("Not Authent: {}:{}", uriRequest, authentHttp);
        checkAuthent(ctx);
        return;
      }
      String find = uriRequest;
      if (uriRequest.charAt(0) == '/') {
        find = uriRequest.substring(1);
      }
      REQUEST req = REQUEST.index;
      if (find.length() != 0) {
        find = find.substring(0, find.indexOf('.'));
        try {
          req = REQUEST.valueOf(find);
        } catch (final IllegalArgumentException e1) {
          req = REQUEST.index;
          logger.info("NotFound: {}:{}", find, uriRequest);
        }
      }
      switch (req) {
        case CancelRestart:
          if (authentHttp.getAuth().isValidRole(ROLE.TRANSFER)) {
            responseContent.append(cancelRestart0());
          } else {
            responseContent.append(unallowed(
                Messages.getString("HttpSslHandler.CancelRestartUnallowed")));
          }
          break;
        case Export:
          if (authentHttp.getAuth().isValidRole(ROLE.SYSTEM)) {
            responseContent.append(export0());
          } else {
            responseContent.append(unallowed(
                Messages.getString("HttpSslHandler.ExportUnallowed")));
          }
          break;
        case Hosts:
          if (authentHttp.getAuth().isValidRole(ROLE.HOST)) {
            responseContent.append(hosts0());
          } else {
            responseContent.append(
                unallowed(Messages.getString("HttpSslHandler.HostUnallowed")));
          }
          break;
        case Listing:
          responseContent.append(listing0());
          break;
        case Logout:
          responseContent.append(logout());
          break;
        case Rules:
          if (authentHttp.getAuth().isValidRole(ROLE.RULE)) {
            responseContent.append(rules0());
          } else {
            responseContent.append(
                unallowed(Messages.getString("HttpSslHandler.RulesUnallowed")));
          }
          break;
        case System:
          if (authentHttp.getAuth().isValidRole(ROLE.SYSTEM)) {
            responseContent.append(system0());
          } else {
            responseContent.append(systemLimited());
          }
          break;
        case Transfers:
          responseContent.append(transfers());
          break;
        case Spooled:
          responseContent.append(spooled0(false));
          break;
        case SpooledDetailed:
          responseContent.append(spooled0(true));
          break;
        default:
          responseContent.append(index());
          break;
      }
      writeResponse(ctx);
    } finally {
      closeConnection();
    }
  }

  protected final void checkSession(final Channel channel) {
    final String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
    if (cookieString != null) {
      final Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
      if (!cookies.isEmpty()) {
        for (final Cookie elt : cookies) {
          if (elt.name().equalsIgnoreCase(
              R66SESSION + Configuration.configuration.getHostId())) {
            logger.debug("Found session: {}", elt);
            admin = elt;
            final R66Session session = sessions.get(admin.value());
            if (session != null) {
              authentHttp = session;
              authentHttp.setStatus(73);
            } else {
              admin = null;
            }
          } else if (elt.name().equalsIgnoreCase(I18NEXT)) {
            logger.debug("Found i18next: {}", elt);
            lang = elt.value();
          }
        }
      }
    }
    try {
      dbSession = new DbSession(DbConstantR66.admin, false);
      DbAdmin.incHttpSession();
    } catch (final WaarpDatabaseNoConnectionException e1) {
      // Cannot connect so use default connection
      logger.warn("Use default database connection");
      dbSession = DbConstantR66.admin.getSession();
    }
    if (admin == null) {
      logger.debug("NoSession: {}:{}", uriRequest, admin);
    }
  }

  protected final void closeConnection() {
    if (dbSession != null && dbSession != DbConstantR66.admin.getSession()) {
      DbAdmin.decHttpSession();
      dbSession.disconnect();
    }
    dbSession = null;
  }

  private void handleCookies(final HttpResponse response) {
    final String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
    boolean i18nextFound = false;
    if (cookieString != null) {
      final Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
      if (!cookies.isEmpty()) {
        // Reset the sessions if necessary.
        boolean findSession = false;
        for (final Cookie cookie : cookies) {
          if (cookie.name().equalsIgnoreCase(
              R66SESSION + Configuration.configuration.getHostId())) {
            if (newSession) {
              findSession = false;
            } else {
              findSession = true;
              response.headers().add(HttpHeaderNames.SET_COOKIE,
                                     ServerCookieEncoder.LAX.encode(cookie));
            }
          } else if (cookie.name().equalsIgnoreCase(I18NEXT)) {
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
        newSession = false;
        if (!findSession && admin != null) {
          response.headers().add(HttpHeaderNames.SET_COOKIE,
                                 ServerCookieEncoder.LAX.encode(admin));
          logger.debug("AddSession: {}:{}", uriRequest, admin);
        }
      }
    } else {
      final Cookie cookie = new DefaultCookie(I18NEXT, lang);
      response.headers().add(HttpHeaderNames.SET_COOKIE,
                             ServerCookieEncoder.LAX.encode(cookie));
      if (admin != null) {
        logger.debug("AddSession: {}:{}", uriRequest, admin);
        response.headers().add(HttpHeaderNames.SET_COOKIE,
                               ServerCookieEncoder.LAX.encode(admin));
      }
    }
  }

  /**
   * Write the response
   *
   * @param ctx
   */
  protected final void writeResponse(final ChannelHandlerContext ctx) {
    // Convert the response content to a ByteBuf.
    final ByteBuf buf = Unpooled.copiedBuffer(responseContent.toString(),
                                              WaarpStringUtils.UTF8);
    responseContent.setLength(0);

    // Decide whether to close the connection or not.
    final boolean keepAlive = HttpUtil.isKeepAlive(request);
    final boolean close = HttpHeaderValues.CLOSE.contentEqualsIgnoreCase(
        request.headers().get(HttpHeaderNames.CONNECTION)) || !keepAlive ||
                          forceClose;

    // Build the response object.
    final FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                                    buf);
    response.headers().add(HttpHeaderNames.CONTENT_LENGTH,
                           response.content().readableBytes());
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
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

    handleCookies(response);

    // Write the response.
    final ChannelFuture future = ctx.writeAndFlush(response);
    // Close the connection after the write operation is done if necessary.
    if (close) {
      future.addListener(WaarpSslUtility.SSLCLOSE);
    }
    if (shutdown) {
      ChannelUtils.startShutdown();
    }
  }

  /**
   * Send an error and close
   *
   * @param ctx
   * @param status
   */
  final void sendError(final ChannelHandlerContext ctx,
                       final HttpResponseStatus status) {
    responseContent.setLength(0);
    responseContent.append(error(status.toString()));
    final FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status,
                                    Unpooled.copiedBuffer(
                                        responseContent.toString(),
                                        WaarpStringUtils.UTF8));
    response.headers().add(HttpHeaderNames.CONTENT_LENGTH,
                           response.content().readableBytes());
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
    responseContent.setLength(0);
    clearSession();
    // Close the connection as soon as the error message is sent.
    ctx.writeAndFlush(response).addListener(WaarpSslUtility.SSLCLOSE);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx,
                              final Throwable cause) {
    final OpenR66Exception exception =
        OpenR66ExceptionTrappedFactory.getExceptionFromTrappedException(
            ctx.channel(), cause);
    if (exception != null) {
      if (!(exception instanceof OpenR66ProtocolBusinessNoWriteBackException)) {
        if (cause instanceof IOException) {
          // Nothing to do
          return;
        }
        logger.warn("Exception in HttpSslHandler {}", exception.getMessage());
      }
      if (ctx.channel().isActive()) {
        sendError(ctx, HttpResponseStatus.BAD_REQUEST);
      }
    } else {
      // Nothing to do
    }
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    final Channel channel = ctx.channel();
    Configuration.configuration.getHttpChannelGroup().add(channel);
    super.channelActive(ctx);
  }

  /**
   * @return the lIMITROW
   */
  public final int getLimitRow() {
    return limitRow;
  }

  /**
   * @param lIMITROW the lIMITROW to set
   */
  final void setLimitRow(final int lIMITROW) {
    limitRow = lIMITROW;
  }

  /**
   * @return the rEFRESH
   */
  public final int getRefresh() {
    return refresh;
  }

  /**
   * @param rEFRESH the rEFRESH to set
   */
  final void setRefresh(final int rEFRESH) {
    refresh = rEFRESH;
  }

}
