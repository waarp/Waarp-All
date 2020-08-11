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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.waarp.common.command.exception.Reply421Exception;
import org.waarp.common.command.exception.Reply530Exception;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.file.DirInterface;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.Version;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.gateway.kernel.http.HttpWriteCacheEnable;
import org.waarp.openr66.client.Message;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.task.SpooledInformTask;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbHostConfiguration;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.LocalServerHandler;
import org.waarp.openr66.protocol.localhandler.ServerActions;
import org.waarp.openr66.protocol.localhandler.packet.ErrorPacket;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket.TRANSFERMODE;
import org.waarp.openr66.protocol.localhandler.packet.TestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.NbAndSpecialId;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.protocol.utils.TransferUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 */
public class HttpResponsiveSslHandler extends HttpSslHandler {
  private static final String B_CENTER_P2 = "</b></center></p>";

  private static final String FILTER2 = "Filter";

  private static final String ACTION2 = "ACTION";

  private static final String OPEN_R66_WEB_ERROR2 = "OpenR66 Web Error {}";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpResponsiveSslHandler.class);

  public static final String LISTING_PAGE = "Listing.html";

  private enum REQUEST {
    Logon("Logon.html"), Logout("Logon.html"), index("index.html"),
    error("Error.html"), unallowed("NotAllowed.html"), Listing(LISTING_PAGE),
    ListingReload(LISTING_PAGE), CancelRestart("CancelRestart.html"),
    Export("Export.html"), Hosts("Hosts.html"), Rules("Rules.html"),
    System("System.html"), SystemLimited("SystemLimited.html"),
    Spooled("Spooled.html"), SpooledDetailed("Spooled.html");

    private final String header;

    /**
     * Constructor for a unique file
     *
     * @param uniquefile
     */
    REQUEST(String uniquefile) {
      header = uniquefile;
    }

    /**
     * @param header
     * @param headerBody
     * @param body
     * @param endBody
     * @param end
     */
    REQUEST(String header, String headerBody, String body, String endBody,
            String end) {
      this.header = header;
    }

    /**
     * Reader for a unique file
     *
     * @return the content of the unique file
     */
    public String read(HttpResponsiveSslHandler handler) {
      return handler.readFileHeader(
          Configuration.configuration.getHttpBasePath() + header);
    }
  }


  public static final String LIMITROW1 = "LIMITROW";
  public static final String REFRESH = "REFRESH";
  private static final String XXXRESULTXXX = "XXXRESULTXXX";
  private static final String XXXDATAJSONXXX = "XXXDATAJSONXXX";
  private static final String XXXHOSTSIDSXXX = "XXXHOSTSIDSXXX";

  private String indexResponsive() {
    final String index = REQUEST.index.read(this);
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

  @Override
  protected String error(String mesg) {
    final String index = REQUEST.error.read(this);
    return index.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(), mesg);
  }

  private String unallowedResponsive(String mesg) {
    final String index = REQUEST.unallowed.read(this);
    if (index == null || index.isEmpty()) {
      return error(mesg);
    }
    return index.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(), mesg);
  }

  @Override
  protected String logon() {
    return REQUEST.Logon.read(this);
  }

  private String setDbTaskRunnerJsonData(String head, String errorText,
                                         String startid, String stopid,
                                         Timestamp tstart, Timestamp tstop,
                                         String rule, String req,
                                         boolean pending, boolean transfer,
                                         boolean error, boolean done,
                                         boolean all) {
    final String seeAll = checkAuthorizedToSeeAll();
    DbPreparedStatement preparedStatement = null;
    try {
      preparedStatement = DbTaskRunner
          .getFilterPrepareStatement(dbSession, getLimitRow(), false, startid,
                                     stopid, tstart, tstop, rule, req, pending,
                                     transfer, error, done, all, seeAll);
      final String json =
          DbTaskRunner.getJson(preparedStatement, getLimitRow());
      return head.replace(XXXDATAJSONXXX, json);
    } catch (final WaarpDatabaseException e) {
      if (preparedStatement != null) {
        preparedStatement.realClose();
      }
      logger.warn(OPEN_R66_WEB_ERROR2, e.getMessage());
      errorText +=
          Messages.getString("ErrorCode.17") + ": " + e.getMessage() + "<BR/>";
    } catch (final OpenR66ProtocolBusinessException e) {
      if (preparedStatement != null) {
        preparedStatement.realClose();
      }
      logger.warn(OPEN_R66_WEB_ERROR2, e.getMessage());
      errorText +=
          Messages.getString("ErrorCode.17") + ": " + e.getMessage() + "<BR/>";
    }
    return head.replace(XXXRESULTXXX, errorText);
  }

  private String listingReload() {
    final String errorText = "";
    if (params == null) {
      return getHeadNoParam(REQUEST.Listing, errorText);
    }
    final List<String> parms = params.get(ACTION2);
    String head = REQUEST.Listing.read(this);
    if (parms != null) {
      final String parm = parms.get(0);
      final boolean isNotReload = !"Reload".equalsIgnoreCase(parm);
      if (FILTER2.equalsIgnoreCase(parm) || !isNotReload) {
        head = getFilter(errorText, head, isNotReload);
      } else {
        head = resetOptionTransfer(head, "", "", "", "", "", "", false, false,
                                   false, false, true);
        head =
            setDbTaskRunnerJsonData(head, errorText, "", "", null, null, "", "",
                                    false, false, false, false, true);
      }
    } else {
      head =
          resetOptionTransfer(head, "", "", "", "", "", "", false, false, false,
                              false, true);
      head =
          setDbTaskRunnerJsonData(head, errorText, "", "", null, null, "", "",
                                  false, false, false, false, true);
    }
    return head.replace(XXXRESULTXXX, errorText).replace(XXXDATAJSONXXX, "[]");
  }

  private String getFilter(final String errorText, String head,
                           final boolean isNotReload) {
    String startid = getTrimValue("startid");
    String stopid = getTrimValue("stopid");
    if (isNotReload && startid != null && stopid == null) {
      stopid = Long.MAX_VALUE + "";
    }
    if (isNotReload && stopid != null && startid == null) {
      startid = (DbConstantR66.ILLEGALVALUE + 1) + "";
    }
    String start = getValue("start");
    String stop = getValue("stop");
    final String rule = getTrimValue("rule");
    final String req = getTrimValue("req");
    boolean pending;
    boolean transfer;
    boolean error;
    boolean done;
    boolean all;
    pending = params.containsKey("pending");
    transfer = params.containsKey("transfer");
    error = params.containsKey("error");
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
    final Long idstart = null;
    head =
        setDbTaskRunnerJsonData(head, errorText, startid, stopid, tstart, tstop,
                                rule, req, pending, transfer, error, done, all);
    head = resetOptionTransfer(head, startid == null?
                                   idstart != null? idstart.toString() : "" : startid,
                               stopid == null? "" : stopid, start, stop,
                               rule == null? "" : rule, req == null? "" : req,
                               pending, transfer, error, done, all);
    return head;
  }

  private String listing() {
    getParamsResponsive();
    return listingReload();
  }

  private String cancelRestart() {
    getParamsResponsive();
    if (params == null) {
      return getHeadNoParam(REQUEST.CancelRestart, "");
    }
    String head = REQUEST.CancelRestart.read(this);
    String errorText = "";
    final List<String> parms = params.get(ACTION2);
    final String seeAll = checkAuthorizedToSeeAll();
    if (parms != null) {
      final String parm = parms.get(0);
      final boolean isNotReload = !"Reload".equalsIgnoreCase(parm);
      if ("Search".equalsIgnoreCase(parm)) {
        head = getHeadSearchCancelRestart(head, errorText);
      } else if (FILTER2.equalsIgnoreCase(parm) || !isNotReload) {
        head = getFilter(errorText, head, isNotReload);
      } else if ("RestartAll".equalsIgnoreCase(parm) ||
                 "StopAll".equalsIgnoreCase(parm) ||
                 "StopCleanAll".equalsIgnoreCase(parm)) {
        RestartOrStopAll restartOrStopAll =
            new RestartOrStopAll(head, errorText, seeAll, parm).invoke();
        head = restartOrStopAll.getHead();
        errorText = restartOrStopAll.getErrorText();
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
        long lspecid;
        try {
          lspecid = Long.parseLong(specid);
        } catch (final NumberFormatException e) {
          errorText += "<br><b>" + parm +
                       Messages.getString("HttpSslHandler.3"); //$NON-NLS-2$
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        DbTaskRunner taskRunner = null;
        try {
          taskRunner = new DbTaskRunner(authentHttp, null, lspecid, reqr, reqd);
        } catch (final WaarpDatabaseException ignored) {
          // nothing
        }
        if (taskRunner == null) {
          errorText += "<br><b>" + parm +
                       Messages.getString("HttpSslHandler.3"); //$NON-NLS-2$
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
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
          if (taskRunner != null && taskRunner.stopOrCancelRunner(code)) {
            result = ErrorCode.CompleteOk;
          }
        }
        if (taskRunner != null) {
          if ("CancelClean".equalsIgnoreCase(parm)) {
            TransferUtils.cleanOneTransfer(taskRunner, null, authentHttp, null);
          }
          String tstart = taskRunner.getStart().toString();
          String tstop = taskRunner.getStop().toString();
          head = resetOptionTransfer(head, String
                                         .valueOf(taskRunner.getSpecialId() - 1), String.valueOf(
              taskRunner.getSpecialId() + 1), tstart, tstop,
                                     taskRunner.getRuleId(),
                                     taskRunner.getRequested(), false, false,
                                     false, false, true);
        }
        final String json = taskRunner.getJsonAsString();
        head = head.replace(XXXDATAJSONXXX, '[' + json + ']');
        errorText += "<br><b>" + (result == ErrorCode.CompleteOk?
            parm + Messages.getString("HttpSslHandler.5") :
            //$NON-NLS-2$
            parm + Messages.getString("HttpSslHandler.4")) +
                     "</b>"; //$NON-NLS-1$
      } else if ("Restart".equalsIgnoreCase(parm)) {
        // Restart
        final String specid = getValue("specid");
        final String reqd = getValue("reqd");
        final String reqr = getValue("reqr");
        long lspecid;
        if (specid == null || reqd == null || reqr == null) {
          errorText += "<br><b>" + parm +
                       Messages.getString("HttpSslHandler.3"); //$NON-NLS-2$
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        try {
          lspecid = Long.parseLong(specid);
        } catch (final NumberFormatException e) {
          errorText += "<br><b>" + parm +
                       Messages.getString("HttpSslHandler.3"); //$NON-NLS-2$
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        DbTaskRunner taskRunner;
        String comment = "";
        try {
          taskRunner = new DbTaskRunner(authentHttp, null, lspecid, reqr, reqd);
          final LocalChannelReference lcr =
              Configuration.configuration.getLocalTransaction()
                                         .getFromRequest(taskRunner.getKey());
          final R66Result finalResult =
              TransferUtils.restartTransfer(taskRunner, lcr);
          comment = (String) finalResult.getOther();
          String tstart = taskRunner.getStart().toString();
          String tstop = taskRunner.getStop().toString();
          head = resetOptionTransfer(head, String
                                         .valueOf(taskRunner.getSpecialId() - 1), String.valueOf(
              taskRunner.getSpecialId() + 1), tstart, tstop,
                                     taskRunner.getRuleId(),
                                     taskRunner.getRequested(), false, false,
                                     false, false, true);
          final String json = taskRunner.getJsonAsString();
          head = head.replace(XXXDATAJSONXXX, '[' + json + ']');
        } catch (final WaarpDatabaseException e) {
          errorText +=
              Messages.getString("ErrorCode.17") + ": " + e.getMessage() +
              "<BR/>";
        }
        errorText += "<br><b>" + comment + "</b>";
      } else {
        head = resetOptionTransfer(head, "", "", "", "", "", "", false, false,
                                   false, false, true);
        head =
            setDbTaskRunnerJsonData(head, errorText, "", "", null, null, "", "",
                                    false, false, false, false, true);
      }
    } else {
      head =
          resetOptionTransfer(head, "", "", "", "", "", "", false, false, false,
                              false, true);
      head =
          setDbTaskRunnerJsonData(head, errorText, "", "", null, null, "", "",
                                  false, false, false, false, true);
    }
    return head.replace(XXXRESULTXXX, errorText).replace(XXXDATAJSONXXX, "[]");
  }

  private String getHeadSearchCancelRestart(String head,
                                            final String errorText) {
    final String startid = getTrimValue("startid");
    final String stopid =
        startid == null? null : Long.toString(Long.parseLong(startid) + 1);
    head = setDbTaskRunnerJsonData(head, errorText, startid, stopid, null, null,
                                   null, null, false, false, false, false,
                                   true);
    head =
        resetOptionTransfer(head, startid == null? "" : startid, stopid, "", "",
                            "", "", false, false, false, false, true);
    return head;
  }

  private String getHeadNoParam(final REQUEST cancelRestart, final String s) {
    String head = cancelRestart.read(this);
    head =
        resetOptionTransfer(head, "", "", "", "", "", "", false, false, false,
                            false, true);
    head = setDbTaskRunnerJsonData(head, s, "", "", null, null, "", "", false,
                                   false, false, false, true);
    return head.replace(XXXRESULTXXX, "").replace(XXXDATAJSONXXX, "[]");
  }

  private String export() {
    getParamsResponsive();
    if (params == null) {
      String body = REQUEST.Export.read(this);
      body =
          resetOptionTransfer(body, "", "", "", "", "", "", false, false, false,
                              true, false);
      return body.replace(XXXRESULTXXX, "");
    }
    String body = REQUEST.Export.read(this);
    String start = getValue("start");
    String stop = getValue("stop");
    final String rule = getTrimValue("rule");
    final String req = getTrimValue("req");
    boolean pending;
    boolean transfer;
    boolean error;
    boolean done;
    boolean all;
    pending = params.containsKey("pending");
    transfer = params.containsKey("transfer");
    error = params.containsKey("error");
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
    final String basename =
        Configuration.configuration.getArchivePath() + DirInterface.SEPARATOR +
        Configuration.configuration.getHostId() + '_' +
        System.currentTimeMillis() + "_runners.xml";
    final String filename =
        Configuration.configuration.getBaseDirectory() + basename;
    String errorMsg = "";
    final String seeAll = checkAuthorizedToSeeAll();
    try {
      getValid = DbTaskRunner
          .getFilterPrepareStatement(dbSession, 0, // 0 means no limit
                                     true, null, null, tstart, tstop, rule, req,
                                     pending, transfer, error, done, all,
                                     seeAll);
      nbAndSpecialId = DbTaskRunner.writeXMLWriter(getValid, filename);
    } catch (final WaarpDatabaseNoConnectionException e1) {
      isexported = false;
      toPurge = false;
      logger.warn("Export error: {}", e1.getMessage());
      errorMsg = e1.getMessage();
    } catch (final WaarpDatabaseSqlException e1) {
      isexported = false;
      toPurge = false;
      logger.warn("Export error: {}", e1.getMessage());
      errorMsg = e1.getMessage();
    } catch (final OpenR66ProtocolBusinessException e) {
      isexported = false;
      toPurge = false;
      logger.warn("Export error: {}", e.getMessage());
      errorMsg = e.getMessage();
    } finally {
      if (getValid != null) {
        getValid.realClose();
      }
    }
    int purge = 0;
    if (isexported && nbAndSpecialId != null) {
      if (nbAndSpecialId.nb <= 0) {
        return body.replace(XXXRESULTXXX, Messages
            .getString("HttpSslHandler.7")); //$NON-NLS-1$
      }
      // in case of purge
      if (isexported && toPurge) {
        // purge with same filter all runners where globallasttep
        // is ALLDONE or ERROR
        // but getting the higher Special first
        final String stopId = Long.toString(nbAndSpecialId.higherSpecialId);
        try {
          purge = DbTaskRunner
              .purgeLogPrepareStatement(dbSession, null, stopId, tstart, tstop,
                                        rule, req, pending, transfer, error,
                                        done, all);
        } catch (final WaarpDatabaseNoConnectionException ignored) {
          // nothing
        } catch (final WaarpDatabaseSqlException e) {
          logger.warn("Purge error: {}", e.getMessage());
        }
      }
    }
    return body.replace(XXXRESULTXXX, "Export " + (isexported?
        "<B>" + Messages.getString("HttpSslHandler.8") + //$NON-NLS-2$
        "<A href='" + basename + "' target='_blank'>" + basename + "</A>" +
        Messages.getString("HttpSslHandler.9") //$NON-NLS-4$
        + (nbAndSpecialId != null? nbAndSpecialId.nb : 0) +
        Messages.getString("HttpSslHandler.10") + purge
        //$NON-NLS-1$
        + Messages.getString("HttpSslHandler.11") + "</B>" :
        //$NON-NLS-1$
        "<B>" + Messages.getString("HttpSslHandler.12"))) + "</B>" +
           errorMsg; //$NON-NLS-1$
  }

  private String setDbHostAuthJsonData(String head, String errorText,
                                       String host, String addr, boolean ssl,
                                       boolean isactive) {
    DbPreparedStatement preparedStatement = null;
    try {
      preparedStatement = DbHostAuth
          .getFilterPrepareStament(dbSession, host, addr, ssl, isactive);
      final String json = DbHostAuth.getJson(preparedStatement, getLimitRow());
      return head.replace(XXXDATAJSONXXX, json);
    } catch (final WaarpDatabaseException e) {
      if (preparedStatement != null) {
        preparedStatement.realClose();
      }
      logger.warn(OPEN_R66_WEB_ERROR2, e.getMessage());
      errorText +=
          Messages.getString("ErrorCode.17") + ": " + e.getMessage() + "<BR/>";
    } catch (final OpenR66ProtocolBusinessException e) {
      if (preparedStatement != null) {
        preparedStatement.realClose();
      }
      logger.warn(OPEN_R66_WEB_ERROR2, e.getMessage());
      errorText +=
          Messages.getString("ErrorCode.17") + ": " + e.getMessage() + "<BR/>";
    }
    return head.replace(XXXRESULTXXX, errorText);
  }

  private String hosts() {
    getParamsResponsive();
    String head = REQUEST.Hosts.read(this);
    String errorText = "";
    if (params == null) {
      head = resetOptionHosts(head, "", "", false, true);
      head = setDbHostAuthJsonData(head, errorText, null, null, false, true);
      return head.replace(XXXRESULTXXX, errorText)
                 .replace(XXXDATAJSONXXX, "[]");
    }
    final List<String> parms = params.get(ACTION2);
    if (parms != null) {
      final String parm = parms.get(0);
      if ("Create".equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        String addr = getTrimValue("address");
        String port = getTrimValue("port");
        final String key = getTrimValue("hostkey");
        boolean ssl;
        boolean admin;
        boolean isclient;
        boolean isactive;
        boolean isproxified;
        ssl = params.containsKey("ssl");
        admin = params.containsKey("admin");
        isclient = params.containsKey("isclient");
        isactive = params.containsKey("isactive");
        isproxified = params.containsKey("isproxified");
        if (port == null) {
          port = "-1";
        }
        if ("-1".equals(port)) {
          isclient = true;
        }
        if (isclient && addr == null) {
          addr = "0.0.0.0";
        }
        if (host == null || addr == null || key == null) {
          errorText = Messages.getString("HttpSslHandler.13"); //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", ssl, isactive);
          head = setDbHostAuthJsonData(head, errorText, null, null, ssl, true);
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        int iport = -1;
        try {
          iport = Integer.parseInt(port);
        } catch (final NumberFormatException e1) {
          errorText =
              Messages.getString("HttpSslHandler.14") + e1.getMessage() +
              B_CENTER_P2; //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", ssl, isactive);
          head = setDbHostAuthJsonData(head, errorText, null, null, ssl, true);
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        final DbHostAuth dbhost = new DbHostAuth(host, addr, iport, ssl,
                                                 key.getBytes(
                                                     WaarpStringUtils.UTF8),
                                                 admin, isclient);
        dbhost.setActive(isactive);
        dbhost.setProxified(isproxified);
        try {
          dbhost.insert();
        } catch (final WaarpDatabaseException e) {
          errorText = Messages.getString("HttpSslHandler.14") + e.getMessage()
                      //$NON-NLS-1$
                      + B_CENTER_P2;
          head = resetOptionHosts(head, "", "", ssl, isactive);
          head = setDbHostAuthJsonData(head, errorText, null, null, ssl, true);
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        head = resetOptionHosts(head, host, addr, ssl, isactive);
        final String json = dbhost.getJsonAsString();
        head = head.replace(XXXDATAJSONXXX, '[' + json + ']');
      } else if (FILTER2.equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        final String addr = getTrimValue("address");
        final boolean ssl = params.containsKey("ssl");
        final boolean isactive = params.containsKey("active");
        head = resetOptionHosts(head, host == null? "" : host,
                                addr == null? "" : addr, ssl, isactive);
        head =
            setDbHostAuthJsonData(head, errorText, host, addr, ssl, isactive);
      } else if ("Update".equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        final String addr = getTrimValue("address");
        final String port = getTrimValue("port");
        final String key = getTrimValue("hostkey");
        boolean ssl;
        boolean admin;
        boolean isclient;
        boolean isactive;
        boolean isproxified;
        ssl = params.containsKey("ssl");
        admin = params.containsKey("admin");
        isclient = params.containsKey("isclient");
        isactive = params.containsKey("isactive");
        isproxified = params.containsKey("isproxified");
        if (host == null || addr == null || port == null || key == null) {
          errorText = Messages.getString("HttpSslHandler.15"); //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", ssl, isactive);
          head =
              setDbHostAuthJsonData(head, errorText, host, addr, ssl, isactive);
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        int iport;
        try {
          iport = Integer.parseInt(port);
        } catch (final NumberFormatException e1) {
          errorText =
              Messages.getString("HttpSslHandler.16") + e1.getMessage() +
              B_CENTER_P2; //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", ssl, isactive);
          head =
              setDbHostAuthJsonData(head, errorText, host, addr, ssl, isactive);
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        final DbHostAuth dbhost = new DbHostAuth(host, addr, iport, ssl,
                                                 key.getBytes(
                                                     WaarpStringUtils.UTF8),
                                                 admin, isclient);
        dbhost.setActive(isactive);
        dbhost.setProxified(isproxified);
        try {
          if (dbhost.exist()) {
            dbhost.update();
          } else {
            dbhost.insert();
          }
        } catch (final WaarpDatabaseException e) {
          errorText = Messages.getString("HttpSslHandler.16") + e.getMessage() +
                      B_CENTER_P2; //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", ssl, isactive);
          head =
              setDbHostAuthJsonData(head, errorText, host, addr, ssl, isactive);
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        head = resetOptionHosts(head, host, addr, ssl, isactive);
        final String json = dbhost.getJsonAsString();
        head = head.replace(XXXDATAJSONXXX, '[' + json + ']');
      } else if ("TestConn".equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        if (host == null || host.isEmpty()) {
          errorText = Messages.getString("HttpSslHandler.17") + B_CENTER_P2;
          head = resetOptionHosts(head, "", "", false, true);
          head =
              setDbHostAuthJsonData(head, errorText, host, null, false, true);
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        DbHostAuth dbhost;
        try {
          dbhost = new DbHostAuth(host);
        } catch (final WaarpDatabaseException e) {
          errorText = Messages.getString("HttpSslHandler.17") + e.getMessage() +
                      B_CENTER_P2; //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", false, true);
          head =
              setDbHostAuthJsonData(head, errorText, host, null, false, true);
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
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
        final String json = dbhost.getJsonAsString();
        head = head.replace(XXXDATAJSONXXX, '[' + json + ']');
        if (result.isSuccess()) {
          errorText = Messages.getString("HttpSslHandler.18"); //$NON-NLS-1$
        } else {
          errorText = Messages.getString("HttpSslHandler.19")
                      //$NON-NLS-1$
                      + result.getResult().getCode().getMesg() + B_CENTER_P2;
        }
      } else if ("CloseConn".equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        if (host == null || host.isEmpty()) {
          errorText = Messages.getString("HttpSslHandler.17") + B_CENTER_P2;
          head = resetOptionHosts(head, "", "", false, true);
          head =
              setDbHostAuthJsonData(head, errorText, host, null, false, true);
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        DbHostAuth dbhost;
        try {
          dbhost = new DbHostAuth(host);
        } catch (final WaarpDatabaseException e) {
          errorText = Messages.getString("HttpSslHandler.17") + e.getMessage() +
                      B_CENTER_P2; //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", false, true);
          head =
              setDbHostAuthJsonData(head, errorText, host, null, false, true);
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        final boolean resultShutDown = NetworkTransaction
            .shuttingdownNetworkChannelsPerHostID(dbhost.getHostid());
        head =
            resetOptionHosts(head, "", "", dbhost.isSsl(), dbhost.isActive());
        final String json = dbhost.getJsonAsString();
        head = head.replace(XXXDATAJSONXXX, '[' + json + ']');
        if (resultShutDown) {
          errorText = Messages.getString("HttpSslHandler.21"); //$NON-NLS-1$
        } else {
          errorText = Messages.getString("HttpSslHandler.22"); //$NON-NLS-1$
        }
      } else if ("Delete".equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        if (host == null || host.isEmpty()) {
          errorText = Messages.getString("HttpSslHandler.23") + B_CENTER_P2;
          head = resetOptionHosts(head, "", "", false, true);
          head =
              setDbHostAuthJsonData(head, errorText, host, null, false, true);
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        DbHostAuth dbhost;
        try {
          dbhost = new DbHostAuth(host);
        } catch (final WaarpDatabaseException e) {
          errorText = Messages.getString("HttpSslHandler.24") + e.getMessage() +
                      B_CENTER_P2; //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", false, true);
          head =
              setDbHostAuthJsonData(head, errorText, host, null, false, true);
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        try {
          dbhost.delete();
        } catch (final WaarpDatabaseException e) {
          errorText = Messages.getString("HttpSslHandler.24") + e.getMessage() +
                      B_CENTER_P2; //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", false, true);
          head =
              setDbHostAuthJsonData(head, errorText, host, null, dbhost.isSsl(),
                                    dbhost.isActive());
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        final String json = dbhost.getJsonAsString();
        head = head.replace(XXXDATAJSONXXX, '[' + json + ']');
        errorText = Messages.getString("HttpSslHandler.25") + host +
                    B_CENTER_P2; //$NON-NLS-1$
        head = resetOptionHosts(head, "", "", false, dbhost.isActive());
        return head.replace(XXXRESULTXXX, errorText)
                   .replace(XXXDATAJSONXXX, "[]");
      } else {
        head = resetOptionHosts(head, "", "", false, true);
        head = setDbHostAuthJsonData(head, errorText, null, null, false, true);
      }
    } else {
      head = resetOptionHosts(head, "", "", false, true);
      head = setDbHostAuthJsonData(head, errorText, null, null, false, true);
    }
    return head.replace(XXXRESULTXXX, errorText).replace(XXXDATAJSONXXX, "[]");
  }

  private void createExport(HashMap<String, String> rules, String rule,
                            int mode, int limit) {
    DbPreparedStatement preparedStatement = null;
    try {
      preparedStatement = DbRule.getFilterPrepareStament(dbSession, rule, mode);
      preparedStatement.executeQuery();
      int i = 0;
      while (preparedStatement.getNext()) {
        final DbRule dbrule = DbRule.getFromStatement(preparedStatement);
        rules.put(dbrule.getIdRule(), dbrule.getJsonAsString());
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
      logger.warn(OPEN_R66_WEB_ERROR2, e.getMessage());
    }
  }

  private String createExport(String head, String errorText, String rule,
                              int limit) {
    DbPreparedStatement preparedStatement = null;
    try {
      preparedStatement = DbRule.getFilterPrepareStament(dbSession, rule, -1);
      final String json = DbRule.getJson(preparedStatement, getLimitRow());
      preparedStatement.realClose();
      return head.replace(XXXDATAJSONXXX, json);
    } catch (final WaarpDatabaseException e) {
      if (preparedStatement != null) {
        preparedStatement.realClose();
      }
      logger.warn(OPEN_R66_WEB_ERROR2, e.getMessage());
      errorText +=
          Messages.getString("ErrorCode.17") + ": " + e.getMessage() + "<BR/>";
    } catch (final OpenR66ProtocolBusinessException e) {
      if (preparedStatement != null) {
        preparedStatement.realClose();
      }
      logger.warn(OPEN_R66_WEB_ERROR2, e.getMessage());
      errorText +=
          Messages.getString("ErrorCode.17") + ": " + e.getMessage() + "<BR/>";
    }
    return head.replace(XXXRESULTXXX, errorText).replace(XXXDATAJSONXXX, "[]");
  }

  private String rules() {
    getParamsResponsive();
    String head = REQUEST.Rules.read(this);
    final StringBuilder builderHead = new StringBuilder(head);
    fillHostIds(builderHead);
    head = builderHead.toString();
    String errorText = "";
    if (params == null) {
      head = resetOptionRules(head, "", null, -3);
      head = createExport(head, errorText, null, getLimitRow());
      return head.replace(XXXRESULTXXX, errorText)
                 .replace(XXXDATAJSONXXX, "[]");
    }
    final List<String> parms = params.get(ACTION2);
    if (parms != null) {
      final String parm = parms.get(0);
      if ("Create".equalsIgnoreCase(parm) || "Update".equalsIgnoreCase(parm)) {
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
          errorText = Messages.getString("HttpSslHandler.26") + parm + Messages
              .getString("HttpSslHandler.27"); //$NON-NLS-1$ //$NON-NLS-2$
          head = resetOptionRules(head, "", null, -3);
          head = createExport(head, errorText, null, getLimitRow());
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
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
        logger.debug("Recv UpdOrInsert: " + rule + ':' + hostids + ':' +
                     (tmode != null? tmode.ordinal() : 0) + ':' + recvp + ':' +
                     sendp + ':' + archp + ':' + workp + ':' + rpre + ':' +
                     rpost + ':' + rerr + ':' + spre + ':' + spost + ':' +
                     serr);
        final DbRule dbrule =
            new DbRule(rule, hostids, (tmode != null? tmode.ordinal() : 0),
                       recvp, sendp, archp, workp, rpre, rpost, rerr, spre,
                       spost, serr);
        try {
          if ("Create".equalsIgnoreCase(parm)) {
            dbrule.insert();
          } else {
            if (dbrule.exist()) {
              dbrule.update();
            } else {
              dbrule.insert();
            }
          }
        } catch (final WaarpDatabaseException e) {
          errorText = Messages.getString("HttpSslHandler.28") + e.getMessage()
                      //$NON-NLS-1$
                      + B_CENTER_P2;
          head = resetOptionRules(head, "", null, -3);
          head = createExport(head, errorText, null, getLimitRow());
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        final String json = dbrule.getJsonAsString();
        head = head.replace(XXXDATAJSONXXX, '[' + json + ']');
      } else if (FILTER2.equalsIgnoreCase(parm)) {
        final String rule = getTrimValue("rule");
        final String mode = getTrimValue("mode");
        TRANSFERMODE tmode;
        int gmode = 0;
        if (mode != null) {
          if ("all".equals(mode)) {
            gmode = -3;
          } else if ("send".equals(mode)) {
            gmode = -2;
          } else if ("recv".equals(mode)) {
            gmode = -1;
          }
        }
        head = resetOptionRules(head, rule == null? "" : rule, null, gmode);
        final HashMap<String, String> rules = new HashMap<String, String>();
        boolean specific = false;
        if (params.containsKey("send")) {
          tmode = RequestPacket.TRANSFERMODE.SENDMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(rules, rule,
                       RequestPacket.TRANSFERMODE.SENDMODE.ordinal(),
                       getLimitRow() / 2);
        }
        if (params.containsKey("recv")) {
          tmode = RequestPacket.TRANSFERMODE.RECVMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(rules, rule,
                       RequestPacket.TRANSFERMODE.RECVMODE.ordinal(),
                       getLimitRow() / 2);
        }
        if (params.containsKey("sendmd5")) {
          tmode = RequestPacket.TRANSFERMODE.SENDMD5MODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(rules, rule,
                       RequestPacket.TRANSFERMODE.SENDMD5MODE.ordinal(),
                       getLimitRow() / 2);
        }
        if (params.containsKey("recvmd5")) {
          tmode = RequestPacket.TRANSFERMODE.RECVMD5MODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(rules, rule,
                       RequestPacket.TRANSFERMODE.RECVMD5MODE.ordinal(),
                       getLimitRow() / 2);
        }
        if (params.containsKey("sendth")) {
          tmode = RequestPacket.TRANSFERMODE.SENDTHROUGHMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(rules, rule,
                       RequestPacket.TRANSFERMODE.SENDTHROUGHMODE.ordinal(),
                       getLimitRow() / 2);
        }
        if (params.containsKey("recvth")) {
          tmode = RequestPacket.TRANSFERMODE.RECVTHROUGHMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(rules, rule,
                       RequestPacket.TRANSFERMODE.RECVTHROUGHMODE.ordinal(),
                       getLimitRow() / 2);
        }
        if (params.containsKey("sendthmd5")) {
          tmode = RequestPacket.TRANSFERMODE.SENDMD5THROUGHMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(rules, rule,
                       RequestPacket.TRANSFERMODE.SENDMD5THROUGHMODE.ordinal(),
                       getLimitRow() / 2);
        }
        if (params.containsKey("recvthmd5")) {
          tmode = RequestPacket.TRANSFERMODE.RECVMD5THROUGHMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(rules, rule,
                       RequestPacket.TRANSFERMODE.RECVMD5THROUGHMODE.ordinal(),
                       getLimitRow() / 2);
        }
        if (!specific) {
          if (gmode == -1) {
            // recv
            createExport(rules, rule,
                         RequestPacket.TRANSFERMODE.RECVMODE.ordinal(),
                         getLimitRow() / 2);
            createExport(rules, rule,
                         RequestPacket.TRANSFERMODE.RECVMD5MODE.ordinal(),
                         getLimitRow() / 2);
            createExport(rules, rule,
                         RequestPacket.TRANSFERMODE.RECVTHROUGHMODE.ordinal(),
                         getLimitRow() / 2);
            createExport(rules, rule,
                         RequestPacket.TRANSFERMODE.RECVMD5THROUGHMODE
                             .ordinal(), getLimitRow() / 2);
          } else if (gmode == -2) {
            // send
            createExport(rules, rule,
                         RequestPacket.TRANSFERMODE.SENDMODE.ordinal(),
                         getLimitRow() / 2);
            createExport(rules, rule,
                         RequestPacket.TRANSFERMODE.SENDMD5MODE.ordinal(),
                         getLimitRow() / 2);
            createExport(rules, rule,
                         RequestPacket.TRANSFERMODE.SENDTHROUGHMODE.ordinal(),
                         getLimitRow() / 2);
            createExport(rules, rule,
                         RequestPacket.TRANSFERMODE.SENDMD5THROUGHMODE
                             .ordinal(), getLimitRow() / 2);
          } else {
            // all
            createExport(rules, rule, -1, getLimitRow());
          }
        }
        final StringBuilder builder = new StringBuilder("[");
        if (!rules.isEmpty()) {
          for (final String string : rules.values()) {
            builder.append(string).append(',');
          }
          rules.clear();
          builder.setLength(builder.length() - 1);
        }
        builder.append(']');
        head = head.replace(XXXDATAJSONXXX, builder.toString());
      } else if ("Delete".equalsIgnoreCase(parm)) {
        final String rule = getTrimValue("rule");
        if (rule == null || rule.isEmpty()) {
          errorText = Messages.getString("HttpSslHandler.29"); //$NON-NLS-1$
          head = resetOptionRules(head, "", null, -3);
          head = createExport(head, errorText, null, getLimitRow());
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        DbRule dbrule;
        try {
          dbrule = new DbRule(rule);
        } catch (final WaarpDatabaseException e) {
          errorText = Messages.getString("HttpSslHandler.30") + e.getMessage()
                      //$NON-NLS-1$
                      + B_CENTER_P2;
          head = resetOptionRules(head, "", null, -3);
          head = createExport(head, errorText, null, getLimitRow());
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        try {
          dbrule.delete();
        } catch (final WaarpDatabaseException e) {
          errorText = Messages.getString("HttpSslHandler.30") + e.getMessage()
                      //$NON-NLS-1$
                      + B_CENTER_P2;
          head = resetOptionRules(head, "", null, -3);
          head = createExport(head, errorText, null, getLimitRow());
          return head.replace(XXXRESULTXXX, errorText)
                     .replace(XXXDATAJSONXXX, "[]");
        }
        errorText += Messages.getString("HttpSslHandler.31") + rule +
                     B_CENTER_P2; //$NON-NLS-1$
        head = resetOptionRules(head, "", null, -3);
        head =
            head.replace(XXXDATAJSONXXX, '[' + dbrule.getJsonAsString() + ']');
        return head.replace(XXXRESULTXXX, errorText)
                   .replace(XXXDATAJSONXXX, "[]");
      } else {
        head = resetOptionRules(head, "", null, -3);
        head = createExport(head, errorText, null, getLimitRow());
      }
    } else {
      head = resetOptionRules(head, "", null, -3);
      head = createExport(head, errorText, null, getLimitRow());
    }
    return head.replace(XXXRESULTXXX, errorText).replace(XXXDATAJSONXXX, "[]");
  }

  private String spooled(boolean detailed) {
    // XXXSPOOLEDXXX
    if (request.method() == HttpMethod.POST) {
      getParamsResponsive();
    }
    final String spooled = REQUEST.Spooled.read(this);
    String uri;
    if (detailed) {
      uri = "SpooledDetailed.html";
    } else {
      uri = "Spooled.html";
    }
    final QueryStringDecoder queryStringDecoder =
        new QueryStringDecoder(request.uri());
    if (params == null) {
      params = new HashMap<String, List<String>>();
    }
    params.putAll(queryStringDecoder.parameters());
    String name = null;
    if (params.containsKey("name")) {
      name = getTrimValue("name");
    }
    int istatus = 0;
    if (params.containsKey("status")) {
      final String status = getTrimValue("status");
      try {
        istatus = Integer.parseInt(status);
      } catch (final NumberFormatException e1) {
        istatus = 0;
      }
    }
    if (spooled.contains("XXXSPOOLEDXXX")) {
      if (name != null && !name.isEmpty()) {
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
    if (name != null && !name.isEmpty()) {
      // name is specified
      uri = request.uri();
      if (istatus != 0) {
        uri += "&status=" + istatus;
      }
      final String builder =
          SpooledInformTask.buildSpooledUniqueJson(uri, name);
      return spooled.replace(XXXDATAJSONXXX, builder);
    } else {
      if (istatus != 0) {
        uri += "&status=" + istatus;
      }
      final String builder =
          SpooledInformTask.buildSpooledJson(detailed, istatus, uri);
      return spooled.replace(XXXDATAJSONXXX, builder);
    }
  }

  /**
   * Applied current lang to system page
   *
   * @param builder
   */
  @Override
  protected void langHandle(StringBuilder builder) {
    // i18n: add here any new languages
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURLANGENXXX.name(),
                             "en".equalsIgnoreCase(lang)? "checked" : "");
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURLANGFRXXX.name(),
                             "fr".equalsIgnoreCase(lang)? "checked" : "");
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURSYSLANGENXXX.name(),
                             "en".equalsIgnoreCase(Messages.getSlocale())?
                                 "checked" : "");
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURSYSLANGFRXXX.name(),
                             "fr".equalsIgnoreCase(Messages.getSlocale())?
                                 "checked" : "");
  }

  private String systemLimitedSource() {
    final String system = REQUEST.SystemLimited.read(this);
    if (system == null || system.isEmpty()) {
      return REQUEST.System.read(this);
    }
    return system;
  }

  private void fillHostIds(StringBuilder builder) {
    final ArrayList<String> hostsList = new ArrayList<String>();
    try {
      final DbHostAuth[] hosts = DbHostAuth.getAllHosts();
      for (final DbHostAuth dbHostAuth : hosts) {
        hostsList.add(dbHostAuth.getHostid());
      }
    } catch (final WaarpDatabaseNoConnectionException ignored) {
      // nothing
    }
    final StringBuilder hostsBuilder = new StringBuilder();
    for (final String string : hostsList) {
      if (hostsBuilder.length() != 0) {
        hostsBuilder.append(',');
      }
      hostsBuilder.append('\'').append(string).append('\'');
    }
    final String hosts = "[" + hostsBuilder + ']';
    WaarpStringUtils.replace(builder, XXXHOSTSIDSXXX, hosts);
  }

  private String system() {
    getParamsResponsive();
    DbHostConfiguration config;
    try {
      config = new DbHostConfiguration(Configuration.configuration.getHostId());
    } catch (final WaarpDatabaseException e2) {
      config =
          new DbHostConfiguration(Configuration.configuration.getHostId(), "",
                                  "", "", "");
      try {
        config.insert();
      } catch (final WaarpDatabaseException ignored) {
        // nothing
      }
    }
    if (params == null) {
      final String system = REQUEST.System.read(this);
      final StringBuilder builder = new StringBuilder(system);
      replaceStringSystem(config, builder);
      langHandle(builder);
      fillHostIds(builder);
      return builder.toString();
    }
    String extraInformation = null;
    if (params.containsKey(ACTION2)) {
      final List<String> action = params.get(ACTION2);
      for (final String act : action) {
        if ("Language".equalsIgnoreCase(act)) {
          lang = getTrimValue("change");
          final String sys = getTrimValue("changesys");
          Messages.init(new Locale(sys));
          extraInformation =
              Messages.getString("HttpSslHandler.LangIs") + "Web: " + lang +
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
          } else if ("error".equalsIgnoreCase(loglevel)) {
            level = WaarpLogLevel.ERROR;
          }
          WaarpLoggerFactory.setLogLevel(level);
          extraInformation = Messages.getString("HttpSslHandler.LangIs") +
                             level.name(); //$NON-NLS-1$
        } else if ("ExportConfig".equalsIgnoreCase(act)) {
          String base = Configuration.configuration.getBaseDirectory() +
                        DirInterface.SEPARATOR;
          final String directory =
              base + Configuration.configuration.getArchivePath();
          extraInformation = Messages.getString("HttpSslHandler.ExportDir")
                             //$NON-NLS-1$
                             + Configuration.configuration.getArchivePath() +
                             "<br>";
          final String[] filenames = ServerActions
              .staticConfigExport(directory, true, true, true, true, true);
          // hosts, rules, business, alias, roles
          base = base.replace('\\', '/');
          if (filenames[0] != null) {
            filenames[0] = filenames[0].replace('\\', '/').replace(base, "");
            extraInformation +=
                "<A href='" + filenames[0] + "' target='_blank'>" +
                filenames[0] + "</A> " +
                Messages.getString("HttpSslHandler.33"); //$NON-NLS-1$
          }
          if (filenames[1] != null) {
            filenames[1] = filenames[1].replace('\\', '/').replace(base, "");
            extraInformation +=
                "<A href='" + filenames[1] + "' target='_blank'>" +
                filenames[1] + "</A> " +
                Messages.getString("HttpSslHandler.32"); //$NON-NLS-1$
          }
          if (filenames[2] != null) {
            filenames[2] = filenames[2].replace('\\', '/').replace(base, "");
            extraInformation +=
                "<A href='" + filenames[2] + "' target='_blank'>" +
                filenames[2] + "</A> " +
                Messages.getString("HttpSslHandler.44"); //$NON-NLS-1$
          }
          if (filenames[3] != null) {
            filenames[3] = filenames[3].replace('\\', '/').replace(base, "");
            extraInformation +=
                "<A href='" + filenames[3] + "' target='_blank'>" +
                filenames[3] + "</A> " +
                Messages.getString("HttpSslHandler.45"); //$NON-NLS-1$
          }
          if (filenames[4] != null) {
            filenames[4] = filenames[4].replace('\\', '/').replace(base, "");
            extraInformation +=
                "<A href='" + filenames[4] + "' target='_blank'>" +
                filenames[4] + "</A> " +
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
          String error;
          if (Configuration.configuration
                  .getShutdownConfiguration().serviceFuture != null) {
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
          if (Configuration.configuration
                  .getShutdownConfiguration().serviceFuture != null) {
            error =
                error(Messages.getString("HttpSslHandler.38")); //$NON-NLS-1$
          } else {
            error = error(Messages.getString("HttpSslHandler.39")
                          //$NON-NLS-1$
                          + Configuration.configuration.getTimeoutCon() * 2 /
                            1000 + Messages
                              .getString("HttpSslHandler.40")); //$NON-NLS-1$
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
            Configuration.configuration
                .changeNetworkLimit(lglobalw, lglobalr, lsessionw, lsessionr,
                                    delay);
            final String dcomm = getTrimValue("DCOM");
            if (dcomm != null) {
              Configuration.configuration
                  .setDelayCommander(Long.parseLong(dcomm));
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
          logger.debug("DbHC update");
          config.setBusiness(getTrimValue("BUSINESS"));
          config.setRoles(getTrimValue("ROLES"));
          config.setAliases(getTrimValue("ALIASES"));
          config.setOthers(getTrimValue("OTHER"));
          logger.debug("DbHC update {}", config.toJson());
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
    final String system = REQUEST.System.read(this);
    final StringBuilder builder = new StringBuilder(system);
    replaceStringSystem(config, builder);
    langHandle(builder);
    fillHostIds(builder);
    if (extraInformation != null) {
      builder.append(extraInformation);
    }
    return builder.toString();
  }

  private void getParamsResponsive() {
    if (request.method() == HttpMethod.GET) {
      params = null;
    } else if (request.method() == HttpMethod.POST) {
      final Map<String, List<String>> paramsOld = params;
      final ByteBuf content = request.content();
      if (content.isReadable()) {
        final String param = content.toString(WaarpStringUtils.UTF8);
        final QueryStringDecoder queryStringDecoder2 =
            new QueryStringDecoder("/?" + param);
        params = queryStringDecoder2.parameters();
        boolean invalidEntry = false;
        for (Entry<String, List<String>> paramCheck : params.entrySet()) {
          try {
            ParametersChecker.checkSanityString(paramCheck.getValue().toArray(
                ParametersChecker.ZERO_ARRAY_STRING));
          } catch (InvalidArgumentException e) {
            logger.error(
                "Arguments incompatible with Security: " + paramCheck.getKey(),
                e);
            invalidEntry = true;
          }
        }
        if (invalidEntry) {
          for (Entry<String, List<String>> paramCheck : params.entrySet()) {
            paramCheck.getValue().clear();
          }
          params.clear();
          params = null;
          logger.error("No parameter validated since security issue found");
          return;
        }
        if (params.containsKey(REFRESH)) {
          final List<String> parms = params.get(ACTION2);
          if (parms != null) {
            final String parm = parms.get(0);
            if ("Disabling".equalsIgnoreCase(parm)) {
              setRefresh(0);
            } else {
              final String snb = getTrimValue(REFRESH);
              if (snb != null) {
                try {
                  final int old = getRefresh();
                  setRefresh(Integer.parseInt(snb) * 1000);
                  if (getRefresh() < 0) {
                    setRefresh(old);
                  }
                } catch (final Exception ignored) {
                  // ignore
                }
              }
            }
          }
          params = paramsOld;
          return;
        }
        if (params.containsKey(LIMITROW1)) {
          final String snb = getTrimValue(LIMITROW1);
          if (snb != null) {
            try {
              final int old = getLimitRow();
              setLimitRow(Integer.parseInt(snb));
              if (getLimitRow() < 5) {
                setLimitRow(old);
              }
            } catch (final Exception ignored) {
              // ignore
            }
          }
        }
      } else {
        params = null;
      }
    }
  }

  private void checkAuthentResponsive(ChannelHandlerContext ctx) {
    newSession = true;
    if (request.method() == HttpMethod.GET) {
      String logon = logon();
      logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(), "");
      responseContent.append(logon);
      clearSession();
      writeResponse(ctx);
      return;
    } else if (request.method() == HttpMethod.POST) {
      getParamsResponsive();
      if (params == null) {
        String logon = logon();
        logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                                 Messages
                                     .getString("HttpSslHandler.EmptyLogin"));
        responseContent.append(logon);
        clearSession();
        writeResponse(ctx);
        return;
      }
    } else {
      sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
      return;
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
            if (name == null || name.isEmpty()) {
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
            getMenu = password == null || password.isEmpty();
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
        logger.debug(
            "Name? " + name.equals(Configuration.configuration.getAdminName()) +
            " Passwd? " + Arrays
                .equals(password.getBytes(WaarpStringUtils.UTF8),
                        Configuration.configuration.getServerAdminKey()));
        if (name.equals(Configuration.configuration.getAdminName()) && Arrays
            .equals(password.getBytes(WaarpStringUtils.UTF8),
                    Configuration.configuration.getServerAdminKey())) {
          authentHttp.getAuth().specialNoSessionAuth(true,
                                                     Configuration.configuration
                                                         .getHostId());
          authentHttp.setStatus(70);
        } else {
          try {
            authentHttp.getAuth().connectionHttps(name, FilesystemBasedDigest
                .passwdCrypt(password.getBytes(WaarpStringUtils.UTF8)));
          } catch (final Reply530Exception e1) {
            getMenu = true;
          } catch (final Reply421Exception e1) {
            getMenu = true;
          }
        }
        if (!authentHttp.isAuthenticated()) {
          authentHttp.setStatus(71);
          logger.debug("Still not authenticated: {}", authentHttp);
          getMenu = true;
        }
        logger.debug(
            "Identified: " + authentHttp.getAuth().isIdentified() + ':' +
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
      final String index = indexResponsive();
      responseContent.append(index);
      clearSession();
      admin = new DefaultCookie(
          R66SESSION + Configuration.configuration.getHostId(),
          Configuration.configuration.getHostId() +
          Long.toHexString(RANDOM.nextLong()));
      sessions.put(admin.value(), authentHttp);
      authentHttp.setStatus(72);
      logger.debug("CreateSession: " + uriRequest + ":{}", admin);
    }
    writeResponse(ctx);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg)
      throws Exception {
    final FullHttpRequest request = this.request = msg;
    final QueryStringDecoder queryStringDecoder =
        new QueryStringDecoder(request.uri());
    uriRequest = queryStringDecoder.path();
    logger.debug("Msg: " + uriRequest);
    if (uriRequest.contains("gre/") || uriRequest.contains("img/") ||
        uriRequest.contains("app/") || uriRequest.contains("css/") ||
        uriRequest.contains("js/") || uriRequest.contains("datatable/") ||
        uriRequest.contains("res/") || uriRequest.contains("favicon.ico")) {
      HttpWriteCacheEnable.writeFile(request, ctx, Configuration.configuration
                                                       .getHttpBasePath() +
                                                   uriRequest, R66SESSION +
                                                               Configuration.configuration
                                                                   .getHostId());
      return;
    }
    if (uriRequest.contains(Configuration.configuration.getArchivePath())) {
      HttpWriteCacheEnable.writeFile(request, ctx, Configuration.configuration
                                                       .getBaseDirectory() +
                                                   uriRequest, R66SESSION +
                                                               Configuration.configuration
                                                                   .getHostId());
      return;
    }
    checkSession(ctx.channel());
    try {
      if (!authentHttp.isAuthenticated()) {
        logger.debug("Not Authent: " + uriRequest + ":{}", authentHttp);
        checkAuthentResponsive(ctx);
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
          logger.debug("NotFound: " + find + ':' + uriRequest);
        }
      }
      switch (req) {
        case CancelRestart:
          if (authentHttp.getAuth().isValidRole(ROLE.TRANSFER)) {
            responseContent.append(cancelRestart());
          } else {
            responseContent.append(unallowedResponsive(
                Messages.getString("HttpSslHandler.CancelRestartUnallowed")));
          }
          break;
        case Export:
          if (authentHttp.getAuth().isValidRole(ROLE.SYSTEM)) {
            responseContent.append(export());
          } else {
            responseContent.append(unallowedResponsive(
                Messages.getString("HttpSslHandler.ExportUnallowed")));
          }
          break;
        case Hosts:
          if (authentHttp.getAuth().isValidRole(ROLE.HOST)) {
            responseContent.append(hosts());
          } else {
            responseContent.append(unallowedResponsive(
                Messages.getString("HttpSslHandler.HostUnallowed")));
          }
          break;
        case ListingReload:
          responseContent.append(listingReload());
          break;
        case Listing:
          responseContent.append(listing());
          break;
        case Logout:
          responseContent.append(logout());
          break;
        case Rules:
          if (authentHttp.getAuth().isValidRole(ROLE.RULE)) {
            responseContent.append(rules());
          } else {
            responseContent.append(unallowedResponsive(
                Messages.getString("HttpSslHandler.RulesUnallowed")));
          }
          break;
        case System:
          if (authentHttp.getAuth().isValidRole(ROLE.SYSTEM)) {
            responseContent.append(system());
          } else {
            responseContent.append(systemLimited());
          }
          break;
        case Spooled:
          responseContent.append(spooled(false));
          break;
        case SpooledDetailed:
          responseContent.append(spooled(true));
          break;
        default:
          responseContent.append(indexResponsive());
          break;
      }
      writeResponse(ctx);
    } finally {
      closeConnection();
    }
  }

  private class RestartOrStopAll {
    private final String seeAll;
    private final String parm;
    private String head;
    private String errorText;

    public RestartOrStopAll(final String head, final String errorText,
                            final String seeAll, final String parm) {
      this.head = head;
      this.errorText = errorText;
      this.seeAll = seeAll;
      this.parm = parm;
    }

    public String getHead() {
      return head;
    }

    public String getErrorText() {
      return errorText;
    }

    public RestartOrStopAll invoke() {
      final boolean stopcommand = "StopAll".equalsIgnoreCase(parm) ||
                                  "StopCleanAll".equalsIgnoreCase(parm);
      final String startid = getTrimValue("startid");
      final String stopid = getTrimValue("stopid");
      String start = getValue("start");
      String stop = getValue("stop");
      final String rule = getTrimValue("rule");
      final String req = getTrimValue("req");
      boolean pending;
      boolean transfer;
      boolean error;
      boolean done;
      boolean all;
      pending = params.containsKey("pending");
      transfer = params.containsKey("transfer");
      error = params.containsKey("error");
      done = false;
      all = false;
      if (pending && transfer && error && done) {
        all = true;
      } else if (!(pending || transfer || error || done)) {
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
                                 rule == null? "" : rule, req == null? "" : req,
                                 pending, transfer, error, done, all);
      final HashMap<String, String> map = new HashMap<String, String>();
      if (stopcommand) {
        if ("StopCleanAll".equalsIgnoreCase(parm)) {
          TransferUtils
              .cleanSelectedTransfers(dbSession, 0, map, authentHttp, head,
                                      startid, stopid, tstart, tstop, rule, req,
                                      pending, transfer, error, seeAll);
        } else {
          TransferUtils
              .stopSelectedTransfers(dbSession, 0, map, authentHttp, head,
                                     startid, stopid, tstart, tstop, rule, req,
                                     pending, transfer, error, seeAll);
        }
      } else {
        DbPreparedStatement preparedStatement = null;
        try {
          preparedStatement = DbTaskRunner
              .getFilterPrepareStatement(dbSession, 0, false, startid, stopid,
                                         tstart, tstop, rule, req, pending,
                                         transfer, error, done, all, seeAll);
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
              map.put(taskRunner.getKey(), taskRunner.getJsonAsString());
              taskRunner.setErrorExecutionStatus(last);
            } catch (final WaarpDatabaseException e) {
              // try to continue if possible
              logger.warn("An error occurs while accessing a Runner: {}",
                          e.getMessage());
              continue;
            }
          }
          preparedStatement.realClose();
        } catch (final WaarpDatabaseException e) {
          if (preparedStatement != null) {
            preparedStatement.realClose();
          }
          logger.warn(OPEN_R66_WEB_ERROR2, e.getMessage());
          errorText +=
              Messages.getString("ErrorCode.17") + ": " + e.getMessage() +
              "<BR/>";
        }
      }
      final StringBuilder builder = new StringBuilder("[");
      if (!map.isEmpty()) {
        for (final String string : map.values()) {
          builder.append(string).append(',');
        }
        map.clear();
        builder.setLength(builder.length() - 1);
      }
      builder.append(']');
      head = head.replace(XXXDATAJSONXXX, builder.toString());
      return this;
    }
  }
}
