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
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.gateway.kernel.http.HttpWriteCacheEnable;
import org.waarp.openr66.client.Message;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.SpooledInformTask;
import org.waarp.openr66.database.DbConstant;
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
import org.waarp.openr66.protocol.utils.Version;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class HttpSslHandler
    extends SimpleChannelInboundHandler<FullHttpRequest> {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpSslHandler.class);
  /**
   * Session Management
   */
  private static final ConcurrentHashMap<String, R66Session> sessions =
      new ConcurrentHashMap<String, R66Session>();
  private static final ConcurrentHashMap<String, DbSession> dbSessions =
      new ConcurrentHashMap<String, DbSession>();
  private static final Random random = new Random();

  private R66Session authentHttp = new R66Session();

  private FullHttpRequest request;
  private boolean newSession = false;
  private volatile Cookie admin = null;
  private final StringBuilder responseContent = new StringBuilder();
  private String uriRequest;
  private Map<String, List<String>> params;
  private String lang = Messages.getSlocale();
  private boolean forceClose = false;
  private boolean shutdown = false;

  private static final String R66SESSION = "R66SESSION";
  private static final String I18NEXT = "i18next";

  private static enum REQUEST {
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
    private REQUEST(String uniquefile) {
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
    private REQUEST(String header, String headerBody, String body,
                    String endBody, String end) {
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
    public String readFileUnique(HttpSslHandler handler) {
      return handler.readFileHeader(
          Configuration.configuration.getHttpBasePath() + header);
    }

    public String readHeader(HttpSslHandler handler) {
      return handler.readFileHeader(
          Configuration.configuration.getHttpBasePath() + header);
    }

    public String readBodyHeader() {
      return WaarpStringUtils
          .readFile(Configuration.configuration.getHttpBasePath() + headerBody);
    }

    public String readBody() {
      return WaarpStringUtils
          .readFile(Configuration.configuration.getHttpBasePath() + body);
    }

    public String readBodyEnd() {
      return WaarpStringUtils
          .readFile(Configuration.configuration.getHttpBasePath() + endBody);
    }

    public String readEnd() {
      return WaarpStringUtils
          .readFile(Configuration.configuration.getHttpBasePath() + end);
    }
  }

  private static enum REPLACEMENT {
    XXXHOSTIDXXX, XXXADMINXXX, XXXVERSIONXXX, XXXBANDWIDTHXXX,
    XXXXSESSIONLIMITRXXX, XXXXSESSIONLIMITWXXX, XXXXCHANNELLIMITRXXX,
    XXXXCHANNELLIMITWXXX, XXXXDELAYCOMMDXXX, XXXXDELAYRETRYXXX,
    XXXXDELATRAFFICXXX, XXXLOCALXXX, XXXNETWORKXXX, XXXERRORMESGXXX,
    XXXXBUSINESSXXX, XXXXROLESXXX, XXXXALIASESXXX, XXXXOTHERXXX, XXXLIMITROWXXX,
    XXXLANGXXX, XXXCURLANGENXXX, XXXCURLANGFRXXX, XXXCURSYSLANGENXXX,
    XXXCURSYSLANGFRXXX;
  }

  public static final String sLIMITROW = "LIMITROW";
  private static final String XXXRESULTXXX = "XXXRESULTXXX";

  private int LIMITROW = 48; // better if it can
  // be divided by 4

  /**
   * The Database connection attached to this NetworkChannelReference shared
   * among all associated LocalChannels
   * in the session
   */
  private DbSession dbSession = null;
  /**
   * Does this dbSession is private and so should be closed
   */
  private boolean isPrivateDbSession = false;

  public static String hashStatus() {
    return "HttpSslHandler: [sessions: " + sessions.size() + " dbSessions: " +
           dbSessions.size() + "] ";
  }

  private String readFileHeader(String filename) {
    String value;
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
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXLOCALXXX.toString(),
                             Integer.toString(Configuration.configuration
                                                  .getLocalTransaction()
                                                  .getNumberLocalChannel()) +
                             " " + Thread.activeCount());
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXNETWORKXXX.toString(),
                             Integer.toString(DbAdmin.getNbConnection() -
                                              Configuration.getNBDBSESSION()));
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXHOSTIDXXX.toString(),
                             Configuration.configuration.getHOST_ID());
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
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXBANDWIDTHXXX.toString(),
                             Messages.getString("HttpSslHandler.IN") +
                             (trafficCounter.lastReadThroughput() >> 20) +
                             //$NON-NLS-1$
                             Messages.getString("HttpSslHandler.MOPS") +
                             //$NON-NLS-1$
                             Messages.getString("HttpSslHandler.OUT") +
                             //$NON-NLS-1$
                             (trafficCounter.lastWriteThroughput() >> 20) +
                             Messages.getString(
                                 "HttpSslHandler.MOPS")); //$NON-NLS-1$
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXLIMITROWXXX.toString(),
                             "" + getLIMITROW());
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXLANGXXX.toString(), lang);
    return builder.toString();
  }

  private String getTrimValue(String varname) {
    final List<String> varlist = params.get(varname);
    if (varlist != null && !varlist.isEmpty()) {
      String value = params.get(varname).get(0).trim();
      if (value.isEmpty()) {
        value = null;
      }
      return value;
    }
    return null;
  }

  private String getValue(String varname) {
    return params.get(varname).get(0);
  }

  private String index() {
    final String index = REQUEST.index.readFileUnique(this);
    final StringBuilder builder = new StringBuilder(index);
    WaarpStringUtils.replaceAll(builder, REPLACEMENT.XXXHOSTIDXXX.toString(),
                                Configuration.configuration.getHOST_ID());
    WaarpStringUtils.replaceAll(builder, REPLACEMENT.XXXADMINXXX.toString(),
                                Messages.getString(
                                    "HttpSslHandler.2")); //$NON-NLS-1$
    WaarpStringUtils
        .replace(builder, REPLACEMENT.XXXVERSIONXXX.toString(), Version.ID);
    return builder.toString();
  }

  private String error(String mesg) {
    final String index = REQUEST.error.readFileUnique(this);
    return index.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(), mesg);
  }

  private String unallowed(String mesg) {
    final String index = REQUEST.unallowed.readFileUnique(this);
    if (index == null || index.isEmpty()) {
      return error(mesg);
    }
    return index.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(), mesg);
  }

  private String Logon() {
    return REQUEST.Logon.readFileUnique(this);
  }

  private String Transfers() {
    return REQUEST.Transfers.readFileUnique(this);
  }

  private String resetOptionTransfer(String header, String startid,
                                     String stopid, String start, String stop,
                                     String rule, String req, boolean pending,
                                     boolean transfer, boolean error,
                                     boolean done, boolean all) {
    final StringBuilder builder = new StringBuilder(header);
    WaarpStringUtils.replace(builder, "XXXSTARTIDXXX", startid);
    WaarpStringUtils.replace(builder, "XXXSTOPIDXXX", stopid);
    WaarpStringUtils.replace(builder, "XXXSTARTXXX", start);
    WaarpStringUtils.replace(builder, "XXXSTOPXXX", stop);
    WaarpStringUtils.replace(builder, "XXXRULEXXX", rule);
    WaarpStringUtils.replace(builder, "XXXREQXXX", req);
    WaarpStringUtils.replace(builder, "XXXPENDXXX", pending? "checked" : "");
    WaarpStringUtils.replace(builder, "XXXTRANSXXX", transfer? "checked" : "");
    WaarpStringUtils.replace(builder, "XXXERRXXX", error? "checked" : "");
    WaarpStringUtils.replace(builder, "XXXDONEXXX", done? "checked" : "");
    WaarpStringUtils.replace(builder, "XXXALLXXX", all? "checked" : "");
    return builder.toString();
  }

  private String checkAuthorizedToSeeAll() {
    boolean seeAll = false;
    if (authentHttp.getAuth().isValidRole(ROLE.CONFIGADMIN)) {
      DbHostConfiguration dbhc;
      try {
        dbhc =
            new DbHostConfiguration(Configuration.configuration.getHOST_ID());
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

  private String Listing() {
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
    String body0, body, body1;
    body0 = body1 = body = "";
    String endText = "";
    final List<String> parms = params.get("ACTION");
    if (parms != null) {
      body0 = REQUEST.Listing.readBodyHeader();
      final String parm = parms.get(0);
      final boolean isNotReload = !"Reload".equalsIgnoreCase(parm);
      if ("Filter".equalsIgnoreCase(parm) || !isNotReload) {
        String startid = getTrimValue("startid");
        String stopid = getTrimValue("stopid");
        if (isNotReload && startid != null && stopid == null) {
          stopid = Long.MAX_VALUE + "";
        }
        if (isNotReload && stopid != null && startid == null) {
          startid = (DbConstant.ILLEGALVALUE + 1) + "";
        }
        String start = getValue("start");
        String stop = getValue("stop");
        final String rule = getTrimValue("rule");
        final String req = getTrimValue("req");
        boolean pending, transfer, error, done, all;
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
        Long idstart = null;
        body = REQUEST.Listing.readBody();
        final String seeAll = checkAuthorizedToSeeAll();
        DbPreparedStatement preparedStatement = null;
        try {
          preparedStatement = DbTaskRunner
              .getFilterPrepareStatement(dbSession, getLIMITROW(), false,
                                         startid, stopid, tstart, tstop, rule,
                                         req, pending, transfer, error, done,
                                         all, seeAll);
          preparedStatement.executeQuery();
          final StringBuilder builder = new StringBuilder();
          int i = 0;
          while (preparedStatement.getNext()) {
            try {
              i++;
              final DbTaskRunner taskRunner =
                  DbTaskRunner.getFromStatement(preparedStatement);
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
                                                          lcr != null? Messages
                                                              .getString(
                                                                  "HttpSslHandler.Active") :
                                                              Messages
                                                                  .getString(
                                                                      "HttpSslHandler.NotActive")));
              if (i > getLIMITROW()) {
                break;
              }
            } catch (final WaarpDatabaseException e) {
              // try to continue if possible
              logger.warn("An error occurs while accessing a Runner: {}",
                          e.getMessage());
              endText += e.getMessage() + "</BR>";
              continue;
            }
          }
          preparedStatement.realClose();
          body = builder.toString();
        } catch (final WaarpDatabaseException e) {
          if (preparedStatement != null) {
            preparedStatement.realClose();
          }
          logger.warn("OpenR66 Web Error {}", e.getMessage());
        }
        head = resetOptionTransfer(head, startid == null?
                                       (idstart != null? idstart.toString() : "") : startid,
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
    final String end = REQUEST.Listing.readEnd().replace(XXXRESULTXXX, endText);
    return head + body0 + body + body1 + end;
  }

  private String CancelRestart() {
    getParams();
    if (params == null) {
      String head = REQUEST.CancelRestart.readHeader(this);
      head =
          resetOptionTransfer(head, "", "", "", "", "", "", false, false, false,
                              false, true);
      String end;
      end = REQUEST.CancelRestart.readEnd();
      return head + end;
    }
    String head = REQUEST.CancelRestart.readHeader(this);
    String body0, body, body1;
    body0 = body1 = body = "";
    final List<String> parms = params.get("ACTION");
    final String seeAll = checkAuthorizedToSeeAll();
    if (parms != null) {
      body0 = REQUEST.CancelRestart.readBodyHeader();
      final String parm = parms.get(0);
      final boolean isNotReload = !"Reload".equalsIgnoreCase(parm);
      if ("Filter".equalsIgnoreCase(parm) || !isNotReload) {
        String startid = getTrimValue("startid");
        String stopid = getTrimValue("stopid");
        if (isNotReload && startid != null && stopid == null) {
          stopid = Long.MAX_VALUE + "";
        }
        if (isNotReload && stopid != null && startid == null) {
          startid = (DbConstant.ILLEGALVALUE + 1) + "";
        }
        String start = getValue("start");
        String stop = getValue("stop");
        final String rule = getTrimValue("rule");
        final String req = getTrimValue("req");
        boolean pending, transfer, error, done, all;
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
        body = REQUEST.CancelRestart.readBody();
        Long idstart = null;
        DbPreparedStatement preparedStatement = null;
        try {
          preparedStatement = DbTaskRunner
              .getFilterPrepareStatement(dbSession, getLIMITROW(), false,
                                         startid, stopid, tstart, tstop, rule,
                                         req, pending, transfer, error, done,
                                         all, seeAll);
          preparedStatement.executeQuery();
          final StringBuilder builder = new StringBuilder();
          int i = 0;
          while (preparedStatement.getNext()) {
            try {
              i++;
              final DbTaskRunner taskRunner =
                  DbTaskRunner.getFromStatement(preparedStatement);
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
                                                          lcr != null? Messages
                                                              .getString(
                                                                  "HttpSslHandler.Active") :
                                                              Messages
                                                                  .getString(
                                                                      "HttpSslHandler.NotActive")));
              if (i > getLIMITROW()) {
                break;
              }
            } catch (final WaarpDatabaseException e) {
              // try to continue if possible
              logger.warn("An error occurs while accessing a Runner: {}",
                          e.getMessage());
              continue;
            }
          }
          preparedStatement.realClose();
          body = builder.toString();
        } catch (final WaarpDatabaseException e) {
          if (preparedStatement != null) {
            preparedStatement.realClose();
          }
          logger.warn("OpenR66 Web Error {}", e.getMessage());
        }
        head = resetOptionTransfer(head, startid == null?
                                       (idstart != null? idstart.toString() : "") : startid,
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
        final String startid = getTrimValue("startid");
        final String stopid = getTrimValue("stopid");
        String start = getValue("start");
        String stop = getValue("stop");
        final String rule = getTrimValue("rule");
        final String req = getTrimValue("req");
        boolean pending, transfer, error, done, all;
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
                                   rule == null? "" : rule,
                                   req == null? "" : req, pending, transfer,
                                   error, done, all);
        body = REQUEST.CancelRestart.readBody();
        final StringBuilder builder = new StringBuilder();
        if (stopcommand) {
          if ("StopCleanAll".equalsIgnoreCase(parm)) {
            TransferUtils
                .cleanSelectedTransfers(dbSession, 0, builder, authentHttp,
                                        body, startid, stopid, tstart, tstop,
                                        rule, req, pending, transfer, error,
                                        seeAll);
          } else {
            TransferUtils
                .stopSelectedTransfers(dbSession, 0, builder, authentHttp, body,
                                       startid, stopid, tstart, tstop, rule,
                                       req, pending, transfer, error, seeAll);
          }
        } else {
          DbPreparedStatement preparedStatement = null;
          try {
            preparedStatement = DbTaskRunner
                .getFilterPrepareStatement(dbSession, 0, false, startid, stopid,
                                           tstart, tstop, rule, req, pending,
                                           transfer, error, done, all, seeAll);
            preparedStatement.executeQuery();
            // int i = 0;
            while (preparedStatement.getNext()) {
              try {
                // i++;
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
                                                                Messages
                                                                    .getString(
                                                                        "HttpSslHandler.Active") :
                                                                Messages
                                                                    .getString(
                                                                        "HttpSslHandler.NotActive")));
                taskRunner.setErrorExecutionStatus(last);
                /*
                 * if (i > LIMITROW) { break; }
                 */
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
            logger.warn("OpenR66 Web Error {}", e.getMessage());
          }
        }
        if (builder != null) {
          body = builder.toString();
        } else {
          body = "";
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
                reqd + " " + reqr + " " + specid);
        // stop the current transfer
        ErrorCode result;
        long lspecid;
        try {
          lspecid = Long.parseLong(specid);
        } catch (final NumberFormatException e) {
          body = "";
          body1 = REQUEST.CancelRestart.readBodyEnd();
          body1 += "<br><b>" + parm +
                   Messages.getString("HttpSslHandler.3"); //$NON-NLS-2$
          String end;
          end = REQUEST.CancelRestart.readEnd();
          return head + body0 + body + body1 + end;
        }
        DbTaskRunner taskRunner = null;
        try {
          taskRunner = new DbTaskRunner(authentHttp, null, lspecid, reqr, reqd);
        } catch (final WaarpDatabaseException e) {
        }
        if (taskRunner == null) {
          body = "";
          body1 = REQUEST.CancelRestart.readBodyEnd();
          body1 += "<br><b>" + parm +
                   Messages.getString("HttpSslHandler.3"); //$NON-NLS-2$
          String end;
          end = REQUEST.CancelRestart.readEnd();
          return head + body0 + body + body1 + end;
        }
        final ErrorCode code =
            (stop)? ErrorCode.StoppedTransfer : ErrorCode.CanceledTransfer;
        if (lcr != null) {
          final int rank = taskRunner.getRank();
          lcr.sessionNewState(R66FiniteDualStates.ERROR);
          final ErrorPacket error =
              new ErrorPacket("Transfer " + parm + " " + rank, code.getCode(),
                              ErrorPacket.FORWARDCLOSECODE);
          try {
            // XXX ChannelUtils.writeAbstractLocalPacket(lcr, error);
            // inform local instead of remote
            ChannelUtils.writeAbstractLocalPacketToLocal(lcr, error);
          } catch (final Exception e) {
          }
          result = ErrorCode.CompleteOk;
        } else {
          // Transfer is not running
          // But is the database saying the contrary
          result = ErrorCode.TransferOk;
          if (taskRunner != null) {
            if (taskRunner.stopOrCancelRunner(code)) {
              result = ErrorCode.CompleteOk;
            }
          }
        }
        if (taskRunner != null) {
          if ("CancelClean".equalsIgnoreCase(parm)) {
            TransferUtils.cleanOneTransfer(taskRunner, null, authentHttp, null);
          }
          body = REQUEST.CancelRestart.readBody();
          body = taskRunner.toSpecializedHtml(authentHttp, body, lcr != null?
              Messages.getString("HttpSslHandler.Active")
              //$NON-NLS-1$
              : Messages.getString("HttpSslHandler.NotActive")); //$NON-NLS-1$
          String tstart = taskRunner.getStart().toString();
          tstart = tstart.substring(0, tstart.length());
          String tstop = taskRunner.getStop().toString();
          tstop = tstop.substring(0, tstop.length());
          head = resetOptionTransfer(head, (taskRunner.getSpecialId() - 1) + "",
                                     (taskRunner.getSpecialId() + 1) + "",
                                     tstart, tstop, taskRunner.getRuleId(),
                                     taskRunner.getRequested(), false, false,
                                     false, false, true);
        }
        body1 = REQUEST.CancelRestart.readBodyEnd();
        body1 += "<br><b>" + (result == ErrorCode.CompleteOk?
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
        try {
          lspecid = Long.parseLong(specid);
        } catch (final NumberFormatException e) {
          body = "";
          body1 = REQUEST.CancelRestart.readBodyEnd();
          body1 += "<br><b>" + parm +
                   Messages.getString("HttpSslHandler.3"); //$NON-NLS-2$
          String end;
          end = REQUEST.CancelRestart.readEnd();
          return head + body0 + body + body1 + end;
        }
        DbTaskRunner taskRunner;
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
              Messages.getString("HttpSslHandler.Active")
              //$NON-NLS-1$
              : Messages.getString("HttpSslHandler.NotActive")); //$NON-NLS-1$
          String tstart = taskRunner.getStart().toString();
          tstart = tstart.substring(0, tstart.length());
          String tstop = taskRunner.getStop().toString();
          tstop = tstop.substring(0, tstop.length());
          head = resetOptionTransfer(head, (taskRunner.getSpecialId() - 1) + "",
                                     (taskRunner.getSpecialId() + 1) + "",
                                     tstart, tstop, taskRunner.getRuleId(),
                                     taskRunner.getRequested(), false, false,
                                     false, false, true);
        } catch (final WaarpDatabaseException e) {
          body = "";
          comment = Messages.getString("ErrorCode.17"); //$NON-NLS-1$
        }
        body1 = REQUEST.CancelRestart.readBodyEnd();
        body1 += "<br><b>" + comment + "</b>";
      } else {
        head = resetOptionTransfer(head, "", "", "", "", "", "", false, false,
                                   false, false, true);
      }
    } else {
      head =
          resetOptionTransfer(head, "", "", "", "", "", "", false, false, false,
                              false, true);
    }
    String end;
    end = REQUEST.CancelRestart.readEnd();
    return head + body0 + body + body1 + end;
  }

  private String Export() {
    getParams();
    if (params == null) {
      String body = REQUEST.Export.readFileUnique(this);
      body =
          resetOptionTransfer(body, "", "", "", "", "", "", false, false, false,
                              true, false);
      return body.replace(XXXRESULTXXX, "");
    }
    String body = REQUEST.Export.readFileUnique(this);
    String start = getValue("start");
    String stop = getValue("stop");
    final String rule = getTrimValue("rule");
    final String req = getTrimValue("req");
    boolean pending, transfer, error, done, all;
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
    final String filename = Configuration.configuration.getBaseDirectory() +
                            Configuration.configuration.getArchivePath() +
                            DirInterface.SEPARATOR +
                            Configuration.configuration.getHOST_ID() + "_" +
                            System.currentTimeMillis() + "_runners.xml";
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
        } catch (final WaarpDatabaseNoConnectionException e) {
        } catch (final WaarpDatabaseSqlException e) {
          logger.warn("Purge error: {}", e.getMessage());
        }
      }
    }
    return body.replace(XXXRESULTXXX, "Export " + (isexported?
        "<B>" + Messages.getString("HttpSslHandler.8") + //$NON-NLS-2$
        filename + Messages.getString("HttpSslHandler.9") + nbAndSpecialId.nb +
        Messages.getString("HttpSslHandler.10")
        //$NON-NLS-1$ //$NON-NLS-2$
        + purge + Messages.getString("HttpSslHandler.11") + "</B>" :
        //$NON-NLS-1$
        "<B>" + Messages.getString("HttpSslHandler.12"))) + "</B>" +
           errorMsg; //$NON-NLS-1$
  }

  private String resetOptionHosts(String header, String host, String addr,
                                  boolean ssl, boolean active) {
    final StringBuilder builder = new StringBuilder(header);
    WaarpStringUtils.replace(builder, "XXXFHOSTXXX", host);
    WaarpStringUtils.replace(builder, "XXXFADDRXXX", addr);
    WaarpStringUtils.replace(builder, "XXXFSSLXXX", ssl? "checked" : "");
    WaarpStringUtils.replace(builder, "XXXFACTIVXXX", active? "checked" : "");
    return builder.toString();
  }

  private String Hosts() {
    getParams();
    String head = REQUEST.Hosts.readHeader(this);
    String end;
    end = REQUEST.Hosts.readEnd();
    if (params == null) {
      head = resetOptionHosts(head, "", "", false, true);
      return head + end;
    }
    String body0, body, body1;
    body0 = body1 = body = "";
    final List<String> parms = params.get("ACTION");
    if (parms != null) {
      body0 = REQUEST.Hosts.readBodyHeader();
      final String parm = parms.get(0);
      if ("Create".equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        final String addr = getTrimValue("address");
        final String port = getTrimValue("port");
        final String key = getTrimValue("hostkey");
        boolean ssl, admin, isclient, isactive, isproxified;
        ssl = params.containsKey("ssl");
        admin = params.containsKey("admin");
        isclient = params.containsKey("isclient");
        isactive = params.containsKey("isactive");
        isproxified = params.containsKey("isproxified");
        if (host == null || addr == null || port == null || key == null) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.13"); //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", ssl, isactive);
          return head + body0 + body + body1 + end;
        }
        int iport;
        try {
          iport = Integer.parseInt(port);
        } catch (final NumberFormatException e1) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.14") + e1.getMessage() +
                 "</b></center></p>"; //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", ssl, isactive);
          return head + body0 + body + body1 + end;
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
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.14") + e.getMessage()
                 //$NON-NLS-1$
                 + "</b></center></p>";
          head = resetOptionHosts(head, "", "", ssl, isactive);
          return head + body0 + body + body1 + end;
        }
        head = resetOptionHosts(head, host, addr, ssl, isactive);
        body = REQUEST.Hosts.readBody();
        body = dbhost.toSpecializedHtml(authentHttp, body, false);
      } else if ("Filter".equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        final String addr = getTrimValue("address");
        final boolean ssl = params.containsKey("ssl");
        final boolean isactive = params.containsKey("active");
        head = resetOptionHosts(head, host == null? "" : host,
                                addr == null? "" : addr, ssl, isactive);
        body = REQUEST.Hosts.readBody();
        DbPreparedStatement preparedStatement = null;
        try {
          preparedStatement = DbHostAuth
              .getFilterPrepareStament(dbSession, host, addr, ssl, isactive);
          preparedStatement.executeQuery();
          final StringBuilder builder = new StringBuilder();
          int i = 0;
          while (preparedStatement.getNext()) {
            i++;
            final DbHostAuth dbhost =
                DbHostAuth.getFromStatement(preparedStatement);
            builder.append(dbhost.toSpecializedHtml(authentHttp, body, false));
            if (i > getLIMITROW()) {
              break;
            }
          }
          preparedStatement.realClose();
          body = builder.toString();
        } catch (final WaarpDatabaseException e) {
          if (preparedStatement != null) {
            preparedStatement.realClose();
          }
          logger.warn("OpenR66 Web Error {}", e.getMessage());
        }
        body1 = REQUEST.Hosts.readBodyEnd();
      } else if ("Update".equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        final String addr = getTrimValue("address");
        final String port = getTrimValue("port");
        final String key = getTrimValue("hostkey");
        boolean ssl, admin, isclient, isactive, isproxified;
        ssl = params.containsKey("ssl");
        admin = params.containsKey("admin");
        isclient = params.containsKey("isclient");
        isactive = params.containsKey("isactive");
        isproxified = params.containsKey("isproxified");
        if (host == null || addr == null || port == null || key == null) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.15"); //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", ssl, isactive);
          return head + body0 + body + body1 + end;
        }
        int iport;
        try {
          iport = Integer.parseInt(port);
        } catch (final NumberFormatException e1) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.16") + e1.getMessage() +
                 "</b></center></p>"; //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", ssl, isactive);
          return head + body0 + body + body1 + end;
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
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.16") + e.getMessage()
                 //$NON-NLS-1$
                 + "</b></center></p>";
          head = resetOptionHosts(head, "", "", ssl, isactive);
          return head + body0 + body + body1 + end;
        }
        head = resetOptionHosts(head, host, addr, ssl, isactive);
        body = REQUEST.Hosts.readBody();
        body = dbhost.toSpecializedHtml(authentHttp, body, false);
      } else if ("TestConn".equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        if (host == null || host.isEmpty()) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.17"); //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", false, true);
          return head + body0 + body + body1 + end;
        }
        DbHostAuth dbhost;
        try {
          dbhost = new DbHostAuth(host);
        } catch (final WaarpDatabaseException e) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.17") + e.getMessage()
                 //$NON-NLS-1$
                 + "</b></center></p>";
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
        result.awaitOrInterruptible(
            Configuration.configuration.getTIMEOUTCON() / 2);
        head =
            resetOptionHosts(head, "", "", dbhost.isSsl(), dbhost.isActive());
        body = REQUEST.Hosts.readBody();
        if (result.isSuccess()) {
          body = dbhost.toSpecializedHtml(authentHttp, body, false);
          body += Messages.getString("HttpSslHandler.18"); //$NON-NLS-1$
        } else {
          body = dbhost.toSpecializedHtml(authentHttp, body, false);
          body += Messages.getString("HttpSslHandler.19")//$NON-NLS-1$
                  + result.getResult().getCode().getMesg() +
                  "</b></center></p>";
        }
      } else if ("CloseConn".equalsIgnoreCase(parm)) {
        final String host = getTrimValue("host");
        if (host == null || host.isEmpty()) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.17"); //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", false, true);
          return head + body0 + body + body1 + end;
        }
        DbHostAuth dbhost;
        try {
          dbhost = new DbHostAuth(host);
        } catch (final WaarpDatabaseException e) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.17") + e.getMessage()
                 //$NON-NLS-1$
                 + "</b></center></p>";
          head = resetOptionHosts(head, "", "", false, true);
          return head + body0 + body + body1 + end;
        }
        body = REQUEST.Hosts.readBody();
        final boolean resultShutDown = NetworkTransaction
            .shuttingdownNetworkChannelsPerHostID(dbhost.getHostid());
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
        if (host == null || host.isEmpty()) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.23"); //$NON-NLS-1$
          head = resetOptionHosts(head, "", "", false, true);
          return head + body0 + body + body1 + end;
        }
        DbHostAuth dbhost;
        try {
          dbhost = new DbHostAuth(host);
        } catch (final WaarpDatabaseException e) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.24") + e.getMessage()
                 //$NON-NLS-1$
                 + "</b></center></p>";
          head = resetOptionHosts(head, "", "", false, true);
          return head + body0 + body + body1 + end;
        }
        try {
          dbhost.delete();
        } catch (final WaarpDatabaseException e) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.24") + e.getMessage()
                 //$NON-NLS-1$
                 + "</b></center></p>";
          head =
              resetOptionHosts(head, "", "", dbhost.isSsl(), dbhost.isActive());
          return head + body0 + body + body1 + end;
        }
        body0 = body1 = body = "";
        body = Messages.getString("HttpSslHandler.25") + host +
               "</b></center></p>"; //$NON-NLS-1$
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

  private void createExport(String body, StringBuilder builder, String rule,
                            int mode, int limit, int start) {
    DbPreparedStatement preparedStatement = null;
    try {
      preparedStatement = DbRule.getFilterPrepareStament(dbSession, rule, mode);
      preparedStatement.executeQuery();
      int i = 0;
      while (preparedStatement.getNext()) {
        final DbRule dbrule = DbRule.getFromStatement(preparedStatement);
        String temp = dbrule.toSpecializedHtml(authentHttp, body);
        temp = temp.replaceAll("XXXRANKXXX", "" + (start + i));
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
      logger.warn("OpenR66 Web Error {}", e.getMessage());
    }
  }

  private String resetOptionRules(String header, String rule,
                                  RequestPacket.TRANSFERMODE mode, int gmode) {
    final StringBuilder builder = new StringBuilder(header);
    WaarpStringUtils.replace(builder, "XXXRULEXXX", rule);
    if (mode != null) {
      switch (mode) {
        case RECVMODE:
          WaarpStringUtils.replace(builder, "XXXRECVXXX", "checked");
          break;
        case SENDMODE:
          WaarpStringUtils.replace(builder, "XXXSENDXXX", "checked");
          break;
        case RECVMD5MODE:
          WaarpStringUtils.replace(builder, "XXXRECVMXXX", "checked");
          break;
        case SENDMD5MODE:
          WaarpStringUtils.replace(builder, "XXXSENDMXXX", "checked");
          break;
        case RECVTHROUGHMODE:
          WaarpStringUtils.replace(builder, "XXXRECVTXXX", "checked");
          break;
        case SENDTHROUGHMODE:
          WaarpStringUtils.replace(builder, "XXXSENDTXXX", "checked");
          break;
        case RECVMD5THROUGHMODE:
          WaarpStringUtils.replace(builder, "XXXRECVMTXXX", "checked");
          break;
        case SENDMD5THROUGHMODE:
          WaarpStringUtils.replace(builder, "XXXSENDMTXXX", "checked");
          break;
        case UNKNOWNMODE:
          break;
        default:
          break;
      }
    }
    if (gmode == -1) {// All Recv
      WaarpStringUtils.replace(builder, "XXXARECVXXX", "checked");
    } else if (gmode == -2) {// All Send
      WaarpStringUtils.replace(builder, "XXXASENDXXX", "checked");
    } else if (gmode == -3) {// All
      WaarpStringUtils.replace(builder, "XXXALLXXX", "checked");
    }
    return builder.toString();
  }

  private String Rules() {
    getParams();
    String head = REQUEST.Rules.readHeader(this);
    String end;
    end = REQUEST.Rules.readEnd();
    if (params == null) {
      head = resetOptionRules(head, "", null, -3);
      return head + end;
    }
    String body0, body, body1;
    body0 = body1 = body = "";
    final List<String> parms = params.get("ACTION");
    if (parms != null) {
      body0 = REQUEST.Rules.readBodyHeader();
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
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.26") + parm + Messages
              .getString("HttpSslHandler.27"); //$NON-NLS-1$ //$NON-NLS-2$
          head = resetOptionRules(head, "", null, -3);
          return head + body0 + body + body1 + end;
        }
        int gmode = 0;

        TRANSFERMODE tmode = null;
        if (mode.equals("send")) {
          tmode = RequestPacket.TRANSFERMODE.SENDMODE;
          gmode = -2;
        } else if (mode.equals("recv")) {
          tmode = RequestPacket.TRANSFERMODE.RECVMODE;
          gmode = -1;
        } else if (mode.equals("sendmd5")) {
          tmode = RequestPacket.TRANSFERMODE.SENDMD5MODE;
          gmode = -2;
        } else if (mode.equals("recvmd5")) {
          tmode = RequestPacket.TRANSFERMODE.RECVMD5MODE;
          gmode = -1;
        } else if (mode.equals("sendth")) {
          tmode = RequestPacket.TRANSFERMODE.SENDTHROUGHMODE;
          gmode = -2;
        } else if (mode.equals("recvth")) {
          tmode = RequestPacket.TRANSFERMODE.RECVTHROUGHMODE;
          gmode = -1;
        } else if (mode.equals("sendthmd5")) {
          tmode = RequestPacket.TRANSFERMODE.SENDMD5THROUGHMODE;
          gmode = -2;
        } else if (mode.equals("recvthmd5")) {
          tmode = RequestPacket.TRANSFERMODE.RECVMD5THROUGHMODE;
          gmode = -1;
        }
        head = resetOptionRules(head, rule, tmode, gmode);
        logger.debug("Recv UpdOrInsert: " + rule + ":" + hostids + ":" +
                     tmode.ordinal() + ":" + recvp + ":" + sendp + ":" + archp +
                     ":" + workp + ":" + rpre + ":" + rpost + ":" + rerr + ":" +
                     spre + ":" + spost + ":" + serr);
        final DbRule dbrule =
            new DbRule(rule, hostids, tmode.ordinal(), recvp, sendp, archp,
                       workp, rpre, rpost, rerr, spre, spost, serr);
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
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.28") + e.getMessage()
                 //$NON-NLS-1$
                 + "</b></center></p>";
          head = resetOptionRules(head, "", null, -3);
          return head + body0 + body + body1 + end;
        }
        body = REQUEST.Rules.readBody();
        body = dbrule.toSpecializedHtml(authentHttp, body);
      } else if ("Filter".equalsIgnoreCase(parm)) {
        final String rule = getTrimValue("rule");
        final String mode = getTrimValue("mode");
        TRANSFERMODE tmode;
        int gmode = 0;
        if (mode.equals("all")) {
          gmode = -3;
        } else if (mode.equals("send")) {
          gmode = -2;
        } else if (mode.equals("recv")) {
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
                       getLIMITROW() / 4, start);
          start += getLIMITROW() / 4 + 1;
        }
        if (params.containsKey("recv")) {
          tmode = RequestPacket.TRANSFERMODE.RECVMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(body, builder, rule,
                       RequestPacket.TRANSFERMODE.RECVMODE.ordinal(),
                       getLIMITROW() / 4, start);
          start += getLIMITROW() / 4 + 1;
        }
        if (params.containsKey("sendmd5")) {
          tmode = RequestPacket.TRANSFERMODE.SENDMD5MODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(body, builder, rule,
                       RequestPacket.TRANSFERMODE.SENDMD5MODE.ordinal(),
                       getLIMITROW() / 4, start);
          start += getLIMITROW() / 4 + 1;
        }
        if (params.containsKey("recvmd5")) {
          tmode = RequestPacket.TRANSFERMODE.RECVMD5MODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(body, builder, rule,
                       RequestPacket.TRANSFERMODE.RECVMD5MODE.ordinal(),
                       getLIMITROW() / 4, start);
          start += getLIMITROW() / 4 + 1;
        }
        if (params.containsKey("sendth")) {
          tmode = RequestPacket.TRANSFERMODE.SENDTHROUGHMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(body, builder, rule,
                       RequestPacket.TRANSFERMODE.SENDTHROUGHMODE.ordinal(),
                       getLIMITROW() / 4, start);
          start += getLIMITROW() / 4 + 1;
        }
        if (params.containsKey("recvth")) {
          tmode = RequestPacket.TRANSFERMODE.RECVTHROUGHMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(body, builder, rule,
                       RequestPacket.TRANSFERMODE.RECVTHROUGHMODE.ordinal(),
                       getLIMITROW() / 4, start);
          start += getLIMITROW() / 4 + 1;
        }
        if (params.containsKey("sendthmd5")) {
          tmode = RequestPacket.TRANSFERMODE.SENDMD5THROUGHMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(body, builder, rule,
                       RequestPacket.TRANSFERMODE.SENDMD5THROUGHMODE.ordinal(),
                       getLIMITROW() / 4, start);
          start += getLIMITROW() / 4 + 1;
        }
        if (params.containsKey("recvthmd5")) {
          tmode = RequestPacket.TRANSFERMODE.RECVMD5THROUGHMODE;
          head = resetOptionRules(head, rule == null? "" : rule, tmode, gmode);
          specific = true;
          createExport(body, builder, rule,
                       RequestPacket.TRANSFERMODE.RECVMD5THROUGHMODE.ordinal(),
                       getLIMITROW() / 4, start);
          start += getLIMITROW() / 4 + 1;
        }
        if (!specific) {
          if (gmode == -1) {
            // recv
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.RECVMODE.ordinal(),
                         getLIMITROW() / 4, start);
            start += getLIMITROW() / 4 + 1;
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.RECVMD5MODE.ordinal(),
                         getLIMITROW() / 4, start);
            start += getLIMITROW() / 4 + 1;
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.RECVTHROUGHMODE.ordinal(),
                         getLIMITROW() / 4, start);
            start += getLIMITROW() / 4 + 1;
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.RECVMD5THROUGHMODE
                             .ordinal(), getLIMITROW() / 4, start);
            start += getLIMITROW() / 4 + 1;
          } else if (gmode == -2) {
            // send
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.SENDMODE.ordinal(),
                         getLIMITROW() / 4, start);
            start += getLIMITROW() / 4 + 1;
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.SENDMD5MODE.ordinal(),
                         getLIMITROW() / 4, start);
            start += getLIMITROW() / 4 + 1;
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.SENDTHROUGHMODE.ordinal(),
                         getLIMITROW() / 4, start);
            start += getLIMITROW() / 4 + 1;
            createExport(body, builder, rule,
                         RequestPacket.TRANSFERMODE.SENDMD5THROUGHMODE
                             .ordinal(), getLIMITROW() / 4, start);
            start += getLIMITROW() / 4 + 1;
          } else {
            // all
            createExport(body, builder, rule, -1, getLIMITROW(), start);
            start += getLIMITROW() + 1;
          }
        }
        body = builder.toString();
        body1 = REQUEST.Rules.readBodyEnd();
      } else if ("Delete".equalsIgnoreCase(parm)) {
        final String rule = getTrimValue("rule");
        if (rule == null || rule.isEmpty()) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.29"); //$NON-NLS-1$
          head = resetOptionRules(head, "", null, -3);
          return head + body0 + body + body1 + end;
        }
        DbRule dbrule;
        try {
          dbrule = new DbRule(rule);
        } catch (final WaarpDatabaseException e) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.30") + e.getMessage()
                 //$NON-NLS-1$
                 + "</b></center></p>";
          head = resetOptionRules(head, "", null, -3);
          return head + body0 + body + body1 + end;
        }
        try {
          dbrule.delete();
        } catch (final WaarpDatabaseException e) {
          body0 = body1 = body = "";
          body = Messages.getString("HttpSslHandler.30") + e.getMessage()
                 //$NON-NLS-1$
                 + "</b></center></p>";
          head = resetOptionRules(head, "", null, -3);
          return head + body0 + body + body1 + end;
        }
        body0 = body1 = body = "";
        body = Messages.getString("HttpSslHandler.31") + rule +
               "</b></center></p>"; //$NON-NLS-1$
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

  private String Spooled(boolean detailed) {
    // XXXSPOOLEDXXX
    final String spooled = REQUEST.Spooled.readFileUnique(this);
    String uri = null;
    if (detailed) {
      uri = "SpooledDetailed.html";
    } else {
      uri = "Spooled.html";
    }
    final QueryStringDecoder queryStringDecoder =
        new QueryStringDecoder(request.uri());
    params = queryStringDecoder.parameters();
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

  /**
   * Applied current lang to system page
   *
   * @param builder
   */
  private void langHandle(StringBuilder builder) {
    // i18n: add here any new languages
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURLANGENXXX.name(),
                             lang.equalsIgnoreCase("en")? "checked" : "");
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURLANGFRXXX.name(),
                             lang.equalsIgnoreCase("fr")? "checked" : "");
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURSYSLANGENXXX.name(),
                             Messages.getSlocale().equalsIgnoreCase("en")?
                                 "checked" : "");
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURSYSLANGFRXXX.name(),
                             Messages.getSlocale().equalsIgnoreCase("fr")?
                                 "checked" : "");
  }

  private String SystemLimitedSource() {
    final String system = REQUEST.SystemLimited.readFileUnique(this);
    if (system == null || system.isEmpty()) {
      return REQUEST.System.readFileUnique(this);
    }
    return system;
  }

  private String SystemLimited() {
    getParams();
    DbHostConfiguration config = null;
    try {
      config =
          new DbHostConfiguration(Configuration.configuration.getHOST_ID());
    } catch (final WaarpDatabaseException e2) {
      config =
          new DbHostConfiguration(Configuration.configuration.getHOST_ID(), "",
                                  "", "", "");
      try {
        config.insert();
      } catch (final WaarpDatabaseException e) {
      }
    }
    if (params == null) {
      final String system = SystemLimitedSource();
      final StringBuilder builder = new StringBuilder(system);
      replaceStringSystem(config, builder);
      langHandle(builder);
      return builder.toString();
    }
    String extraInformation = null;
    if (params.containsKey("ACTION")) {
      final List<String> action = params.get("ACTION");
      for (final String act : action) {
        if (act.equalsIgnoreCase("Language")) {
          lang = getTrimValue("change");
          extraInformation =
              Messages.getString("HttpSslHandler.LangIs") + "Web: " + lang +
              " OpenR66: " //$NON-NLS-1$
              + Messages.getSlocale();
        } else if (act.equalsIgnoreCase("Disconnect")) {
          String logon = Logon();
          logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                                   Messages
                                       .getString("HttpSslHandler.DisActive"));
          newSession = true;
          clearSession();
          forceClose = true;
          return logon;
        }
      }
    }
    final String system = SystemLimitedSource();
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
  private void replaceStringSystem(DbHostConfiguration config,
                                   StringBuilder builder) {
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXBUSINESSXXX.toString(),
                             config.getBusiness());
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXROLESXXX.toString(),
                             config.getRoles());
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXALIASESXXX.toString(),
                             config.getAliases());
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXOTHERXXX.toString(),
                             config.getOthers());
    WaarpStringUtils
        .replace(builder, REPLACEMENT.XXXXSESSIONLIMITWXXX.toString(),
                 Long.toString(
                     Configuration.configuration.getServerChannelWriteLimit()));
    WaarpStringUtils
        .replace(builder, REPLACEMENT.XXXXSESSIONLIMITRXXX.toString(),
                 Long.toString(
                     Configuration.configuration.getServerChannelReadLimit()));
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXDELATRAFFICXXX.toString(),
                             Long.toString(
                                 Configuration.configuration.getDelayLimit()));
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXDELAYCOMMDXXX.toString(),
                             Long.toString(Configuration.configuration
                                               .getDelayCommander()));
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXDELAYRETRYXXX.toString(),
                             Long.toString(
                                 Configuration.configuration.getDelayRetry()));
    WaarpStringUtils
        .replace(builder, REPLACEMENT.XXXXCHANNELLIMITWXXX.toString(),
                 Long.toString(
                     Configuration.configuration.getServerGlobalWriteLimit()));
    WaarpStringUtils
        .replace(builder, REPLACEMENT.XXXXCHANNELLIMITRXXX.toString(),
                 Long.toString(
                     Configuration.configuration.getServerGlobalReadLimit()));
    WaarpStringUtils.replace(builder, "XXXBLOCKXXX",
                             Configuration.configuration.isShutdown()?
                                 "checked" : "");
    switch (WaarpLoggerFactory.getLogLevel()) {
      case DEBUG:
        WaarpStringUtils.replace(builder, "XXXLEVEL1XXX", "checked");
        WaarpStringUtils.replace(builder, "XXXLEVEL2XXX", "");
        WaarpStringUtils.replace(builder, "XXXLEVEL3XXX", "");
        WaarpStringUtils.replace(builder, "XXXLEVEL4XXX", "");
        break;
      case INFO:
        WaarpStringUtils.replace(builder, "XXXLEVEL1XXX", "");
        WaarpStringUtils.replace(builder, "XXXLEVEL2XXX", "checked");
        WaarpStringUtils.replace(builder, "XXXLEVEL3XXX", "");
        WaarpStringUtils.replace(builder, "XXXLEVEL4XXX", "");
        break;
      case WARN:
        WaarpStringUtils.replace(builder, "XXXLEVEL1XXX", "");
        WaarpStringUtils.replace(builder, "XXXLEVEL2XXX", "");
        WaarpStringUtils.replace(builder, "XXXLEVEL3XXX", "checked");
        WaarpStringUtils.replace(builder, "XXXLEVEL4XXX", "");
        break;
      case ERROR:
        WaarpStringUtils.replace(builder, "XXXLEVEL1XXX", "");
        WaarpStringUtils.replace(builder, "XXXLEVEL2XXX", "");
        WaarpStringUtils.replace(builder, "XXXLEVEL3XXX", "");
        WaarpStringUtils.replace(builder, "XXXLEVEL4XXX", "checked");
        break;
      default:
        WaarpStringUtils.replace(builder, "XXXLEVEL1XXX", "");
        WaarpStringUtils.replace(builder, "XXXLEVEL2XXX", "");
        WaarpStringUtils.replace(builder, "XXXLEVEL3XXX", "");
        WaarpStringUtils.replace(builder, "XXXLEVEL4XXX", "");
        break;

    }
  }

  private String Logout() {
    String logon = Logon();
    logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                             Messages.getString("HttpSslHandler.Disconnected"));
    newSession = true;
    clearSession();
    forceClose = true;
    return logon;
  }

  private String System() {
    getParams();
    DbHostConfiguration config = null;
    try {
      config =
          new DbHostConfiguration(Configuration.configuration.getHOST_ID());
    } catch (final WaarpDatabaseException e2) {
      config =
          new DbHostConfiguration(Configuration.configuration.getHOST_ID(), "",
                                  "", "", "");
      try {
        config.insert();
      } catch (final WaarpDatabaseException e) {
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
    if (params.containsKey("ACTION")) {
      final List<String> action = params.get("ACTION");
      for (final String act : action) {
        if (act.equalsIgnoreCase("Language")) {
          lang = getTrimValue("change");
          final String sys = getTrimValue("changesys");
          Messages.init(new Locale(sys));
          extraInformation =
              Messages.getString("HttpSslHandler.LangIs") + "Web: " + lang +
              " OpenR66: " //$NON-NLS-1$
              + Messages.getSlocale();
        } else if (act.equalsIgnoreCase("Level")) {
          final String loglevel = getTrimValue("loglevel");
          WaarpLogLevel level = WaarpLogLevel.WARN;
          if (loglevel.equalsIgnoreCase("debug")) {
            level = WaarpLogLevel.DEBUG;
          } else if (loglevel.equalsIgnoreCase("info")) {
            level = WaarpLogLevel.INFO;
          } else if (loglevel.equalsIgnoreCase("warn")) {
            level = WaarpLogLevel.WARN;
          } else if (loglevel.equalsIgnoreCase("error")) {
            level = WaarpLogLevel.ERROR;
          }
          WaarpLoggerFactory.setLogLevel(level);
          extraInformation = Messages.getString("HttpSslHandler.LangIs") +
                             level.name(); //$NON-NLS-1$
        } else if (act.equalsIgnoreCase("ExportConfig")) {
          final String directory =
              Configuration.configuration.getBaseDirectory() +
              DirInterface.SEPARATOR +
              Configuration.configuration.getArchivePath();
          extraInformation =
              Messages.getString("HttpSslHandler.ExportDir") + directory +
              "<br>"; //$NON-NLS-1$
          final String[] filenames = ServerActions
              .staticConfigExport(dbSession, directory, true, true, true, true,
                                  true);
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
        } else if (act.equalsIgnoreCase("Disconnect")) {
          return Logout();
        } else if (act.equalsIgnoreCase("Block")) {
          final boolean block = params.containsKey("blocking");
          if (block) {
            extraInformation =
                Messages.getString("HttpSslHandler.34"); //$NON-NLS-1$
          } else {
            extraInformation =
                Messages.getString("HttpSslHandler.35"); //$NON-NLS-1$
          }
          Configuration.configuration.setShutdown(block);
        } else if (act.equalsIgnoreCase("Shutdown")) {
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
        } else if (act.equalsIgnoreCase("Restart")) {
          String error;
          if (Configuration.configuration
                  .getShutdownConfiguration().serviceFuture != null) {
            error =
                error(Messages.getString("HttpSslHandler.38")); //$NON-NLS-1$
          } else {
            error = error(Messages.getString("HttpSslHandler.39")
                          //$NON-NLS-1$
                          + (Configuration.configuration.getTIMEOUTCON() * 2 /
                             1000) + Messages
                              .getString("HttpSslHandler.40")); //$NON-NLS-1$
          }
          error = error.replace("XXXRELOADHTTPXXX",
                                "HTTP-EQUIV=\"refresh\" CONTENT=\"" +
                                (Configuration.configuration.getTIMEOUTCON() *
                                 2 / 1000) + "\"");
          WaarpShutdownHook.setRestart(true);
          newSession = true;
          clearSession();
          forceClose = true;
          shutdown = true;
          return error;
        } else if (act.equalsIgnoreCase("Validate")) {
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
        } else if (act.equalsIgnoreCase("Data.HostConfig")) {
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
        if (params.containsKey(sLIMITROW)) {
          final String snb = getTrimValue(sLIMITROW);
          if (snb != null) {
            try {
              final int old = getLIMITROW();
              setLIMITROW(Integer.parseInt(snb));
              if (getLIMITROW() < 5) {
                setLIMITROW(old);
              }
            } catch (final Exception e1) {
            }
          }
        }
      } else {
        params = null;
      }
    }
  }

  private void clearSession() {
    if (admin != null) {
      final R66Session lsession = sessions.remove(admin.value());
      final DbSession ldbsession = dbSessions.remove(admin.value());
      admin = null;
      if (lsession != null) {
        lsession.setStatus(75);
        lsession.clear();
      }
      if (ldbsession != null) {
        ldbsession.forceDisconnect();
        DbAdmin.decHttpSession();
      }
    }
  }

  private void checkAuthent(ChannelHandlerContext ctx) {
    newSession = true;
    if (request.method() == HttpMethod.GET) {
      String logon = Logon();
      logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(), "");
      responseContent.append(logon);
      clearSession();
      writeResponse(ctx);
      return;
    } else if (request.method() == HttpMethod.POST) {
      getParams();
      if (params == null) {
        String logon = Logon();
        logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                                 Messages
                                     .getString("HttpSslHandler.EmptyLogin"));
        responseContent.append(logon);
        clearSession();
        writeResponse(ctx);
        return;
      }
    }
    boolean getMenu = false;
    if (params.containsKey("Logon")) {
      String name = null, password = null;
      List<String> values = null;
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
        if ((!getMenu) && params.containsKey("passwd")) {
          values = params.get("passwd");
          if (values != null) {
            password = values.get(0);
            if (password == null || password.isEmpty()) {
              getMenu = true;
            } else {
              getMenu = false;
            }
          } else {
            getMenu = true;
          }
        } else {
          getMenu = true;
        }
      } else {
        getMenu = true;
      }
      if (!getMenu) {
        logger.debug(
            "Name? " + name.equals(Configuration.configuration.getADMINNAME()) +
            " Passwd? " + Arrays
                .equals(password.getBytes(WaarpStringUtils.UTF8),
                        Configuration.configuration.getSERVERADMINKEY()));
        if (name.equals(Configuration.configuration.getADMINNAME()) && Arrays
            .equals(password.getBytes(WaarpStringUtils.UTF8),
                    Configuration.configuration.getSERVERADMINKEY())) {
          authentHttp.getAuth().specialNoSessionAuth(true,
                                                     Configuration.configuration
                                                         .getHOST_ID());
          authentHttp.setStatus(70);
        } else {
          try {
            authentHttp.getAuth()
                       .connectionHttps(DbConstant.admin.getSession(), name,
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
          logger.debug("Still not authenticated: {}", authentHttp);
          getMenu = true;
        }
        logger.debug(
            "Identified: " + authentHttp.getAuth().isIdentified() + ":" +
            authentHttp.isAuthenticated());
      }
    } else {
      getMenu = true;
    }
    if (getMenu) {
      String logon = Logon();
      logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                               Messages.getString("HttpSslHandler.BadLogin"));
      responseContent.append(logon);
      clearSession();
      writeResponse(ctx);
    } else {
      // load DbSession
      if (dbSession != null) {
        clearSession();
        dbSession = null;
      }
      if (dbSession == null) {
        try {
          if (DbConstant.admin.isActive()) {
            dbSession = new DbSession(DbConstant.admin, false);
            DbAdmin.incHttpSession();
            isPrivateDbSession = true;
          }
        } catch (final WaarpDatabaseNoConnectionException e1) {
          // Cannot connect so use default connection
          logger.warn("Use default database connection");
          dbSession = DbConstant.admin.getSession();
        }
      }
      final String index = index();
      responseContent.append(index);
      clearSession();
      admin = new DefaultCookie(
          R66SESSION + Configuration.configuration.getHOST_ID(),
          Configuration.configuration.getHOST_ID() +
          Long.toHexString(random.nextLong()));
      sessions.put(admin.value(), authentHttp);
      authentHttp.setStatus(72);
      if (isPrivateDbSession) {
        dbSessions.put(admin.value(), dbSession);
      }
      logger.debug("CreateSession: " + uriRequest + ":{}", admin);
      writeResponse(ctx);
    }
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
        uriRequest.contains("res/") || uriRequest.contains("favicon.ico")) {
      HttpWriteCacheEnable.writeFile(request, ctx, Configuration.configuration
                                                       .getHttpBasePath() +
                                                   uriRequest, R66SESSION +
                                                               Configuration.configuration
                                                                   .getHOST_ID());
      return;
    }
    checkSession(ctx.channel());
    if (!authentHttp.isAuthenticated()) {
      logger.debug("Not Authent: " + uriRequest + ":{}", authentHttp);
      checkAuthent(ctx);
      return;
    }
    String find = uriRequest;
    if (uriRequest.charAt(0) == '/') {
      find = uriRequest.substring(1);
    }
    REQUEST req = REQUEST.index;
    if (find.length() != 0) {
      find = find.substring(0, find.indexOf("."));
      try {
        req = REQUEST.valueOf(find);
      } catch (final IllegalArgumentException e1) {
        req = REQUEST.index;
        logger.debug("NotFound: " + find + ":" + uriRequest);
      }
    }
    switch (req) {
      case CancelRestart:
        if (authentHttp.getAuth().isValidRole(ROLE.TRANSFER)) {
          responseContent.append(CancelRestart());
        } else {
          responseContent.append(unallowed(
              Messages.getString("HttpSslHandler.CancelRestartUnallowed")));
        }
        break;
      case Export:
        if (authentHttp.getAuth().isValidRole(ROLE.SYSTEM)) {
          responseContent.append(Export());
        } else {
          responseContent.append(
              unallowed(Messages.getString("HttpSslHandler.ExportUnallowed")));
        }
        break;
      case Hosts:
        if (authentHttp.getAuth().isValidRole(ROLE.CONFIGADMIN)) {
          responseContent.append(Hosts());
        } else {
          responseContent.append(
              unallowed(Messages.getString("HttpSslHandler.HostUnallowed")));
        }
        break;
      case index:
        responseContent.append(index());
        break;
      case Listing:
        responseContent.append(Listing());
        break;
      case Logon:
        responseContent.append(index());
        break;
      case Logout:
        responseContent.append(Logout());
        break;
      case Rules:
        if (authentHttp.getAuth().isValidRole(ROLE.CONFIGADMIN)) {
          responseContent.append(Rules());
        } else {
          responseContent.append(
              unallowed(Messages.getString("HttpSslHandler.RulesUnallowed")));
        }
        break;
      case System:
        if (authentHttp.getAuth().isValidRole(ROLE.SYSTEM)) {
          responseContent.append(System());
        } else {
          responseContent.append(SystemLimited());
        }
        break;
      case Transfers:
        responseContent.append(Transfers());
        break;
      case Spooled:
        responseContent.append(Spooled(false));
        break;
      case SpooledDetailed:
        responseContent.append(Spooled(true));
        break;
      default:
        responseContent.append(index());
        break;
    }
    writeResponse(ctx);
  }

  private void checkSession(Channel channel) {
    final String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
    if (cookieString != null) {
      final Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
      if (!cookies.isEmpty()) {
        for (final Cookie elt : cookies) {
          if (elt.name().equalsIgnoreCase(
              R66SESSION + Configuration.configuration.getHOST_ID())) {
            logger.debug("Found session: " + elt);
            admin = elt;
            final R66Session session = sessions.get(admin.value());
            if (session != null) {
              authentHttp = session;
              authentHttp.setStatus(73);
            } else {
              admin = null;
              continue;
            }
            final DbSession dbSession = dbSessions.get(admin.value());
            if (dbSession != null) {
              if (dbSession.isDisActive()) {
                clearSession();
              }
              this.dbSession = dbSession;
            } else {
              admin = null;
              continue;
            }
          } else if (elt.name().equalsIgnoreCase(I18NEXT)) {
            logger.debug("Found i18next: " + elt);
            lang = elt.value();
          }
        }
      }
    }
    if (admin == null) {
      logger.debug("NoSession: " + uriRequest + ":{}", admin);
    }
  }

  private void handleCookies(HttpResponse response) {
    final String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
    boolean i18nextFound = false;
    if (cookieString != null) {
      final Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
      if (!cookies.isEmpty()) {
        // Reset the sessions if necessary.
        boolean findSession = false;
        for (final Cookie cookie : cookies) {
          if (cookie.name().equalsIgnoreCase(
              R66SESSION + Configuration.configuration.getHOST_ID())) {
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
        if (!findSession) {
          if (admin != null) {
            response.headers().add(HttpHeaderNames.SET_COOKIE,
                                   ServerCookieEncoder.LAX.encode(admin));
            logger.debug("AddSession: " + uriRequest + ":{}", admin);
          }
        }
      }
    } else {
      final Cookie cookie = new DefaultCookie(I18NEXT, lang);
      response.headers().add(HttpHeaderNames.SET_COOKIE,
                             ServerCookieEncoder.LAX.encode(cookie));
      if (admin != null) {
        logger.debug("AddSession: " + uriRequest + ":{}", admin);
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
  private void writeResponse(ChannelHandlerContext ctx) {
    // Convert the response content to a ByteBuf.
    final ByteBuf buf = Unpooled
        .copiedBuffer(responseContent.toString(), WaarpStringUtils.UTF8);
    responseContent.setLength(0);

    // Decide whether to close the connection or not.
    final boolean keepAlive = HttpUtil.isKeepAlive(request);
    final boolean close = HttpHeaderValues.CLOSE.contentEqualsIgnoreCase(
        request.headers().get(HttpHeaderNames.CONNECTION)) || (!keepAlive) ||
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
  private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    responseContent.setLength(0);
    responseContent.append(error(status.toString()));
    final FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled
            .copiedBuffer(responseContent.toString(), WaarpStringUtils.UTF8));
    response.headers().add(HttpHeaderNames.CONTENT_LENGTH,
                           response.content().readableBytes());
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
    responseContent.setLength(0);
    clearSession();
    // Close the connection as soon as the error message is sent.
    ctx.writeAndFlush(response).addListener(WaarpSslUtility.SSLCLOSE);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
      throws Exception {
    final OpenR66Exception exception = OpenR66ExceptionTrappedFactory
        .getExceptionFromTrappedException(ctx.channel(), cause);
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
      return;
    }
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    final Channel channel = ctx.channel();
    Configuration.configuration.getHttpChannelGroup().add(channel);
    super.channelActive(ctx);
  }

  /**
   * @return the lIMITROW
   */
  public int getLIMITROW() {
    return LIMITROW;
  }

  /**
   * @param lIMITROW the lIMITROW to set
   */
  private void setLIMITROW(int lIMITROW) {
    LIMITROW = lIMITROW;
  }

}
