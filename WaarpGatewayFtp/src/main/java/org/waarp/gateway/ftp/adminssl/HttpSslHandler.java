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
package org.waarp.gateway.ftp.adminssl;

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
import org.waarp.common.command.ReplyCode;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.file.DirInterface;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.ThreadLocalRandom;
import org.waarp.common.utility.Version;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.ftp.core.session.FtpSession;
import org.waarp.ftp.core.utils.FtpChannelUtils;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.gateway.ftp.control.FtpConstraintLimitHandler;
import org.waarp.gateway.ftp.database.DbConstantFtp;
import org.waarp.gateway.ftp.database.data.DbTransferLog;
import org.waarp.gateway.ftp.exec.AbstractExecutor;
import org.waarp.gateway.ftp.exec.AbstractExecutor.CommandExecutor;
import org.waarp.gateway.ftp.file.FileBasedAuth;
import org.waarp.gateway.kernel.http.HttpWriteCacheEnable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HttpSslHandler for FTP Gateway
 */
public class HttpSslHandler
    extends SimpleChannelInboundHandler<FullHttpRequest> {
  private static final String XXXFILEXXX = "XXXFILEXXX";
  private static final String ACTION2 = "ACTION";
  private static final String XXXRESULTXXX = "XXXRESULTXXX";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpSslHandler.class);
  /**
   * Session Management
   */
  private static final ConcurrentHashMap<String, FileBasedAuth> sessions =
      new ConcurrentHashMap<String, FileBasedAuth>();
  private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

  private final FtpSession ftpSession =
      new FtpSession(FileBasedConfiguration.fileBasedConfiguration, null);
  private FileBasedAuth authentHttp = new FileBasedAuth(ftpSession);

  private FullHttpRequest request;
  private boolean newSession;
  private Cookie admin;
  private final StringBuilder responseContent = new StringBuilder();
  private String uriRequest;
  private Map<String, List<String>> params;
  private boolean forceClose;
  private boolean shutdown;

  private static final String FTPSESSIONString = "FTPSESSION";

  private enum REQUEST {
    Logon("Logon.html"), index("index.html"), error("error.html"),
    Transfer("Transfer_head.html", "Transfer_body.html", "Transfer_end.html"),
    Rule("Rule.html"),
    User("User_head.html", "User_body.html", "User_end.html"),
    System("System.html");

    private final String header;
    private final String body;
    private final String end;

    /**
     * Constructor for a unique file
     *
     * @param uniquefile
     */
    REQUEST(final String uniquefile) {
      header = uniquefile;
      body = null;
      end = null;
    }

    /**
     * @param header
     * @param body
     * @param end
     */
    REQUEST(final String header, final String body, final String end) {
      this.header = header;
      this.body = body;
      this.end = end;
    }

    /**
     * Reader for a unique file
     *
     * @return the content of the unique file
     */
    public final String readFileUnique() {
      return WaarpStringUtils.readFile(
          FileBasedConfiguration.fileBasedConfiguration.getHttpBasePath() +
          header);
    }

    public final String readHeader() {
      return WaarpStringUtils.readFile(
          FileBasedConfiguration.fileBasedConfiguration.getHttpBasePath() +
          header);
    }

    public final String readBody() {
      return WaarpStringUtils.readFile(
          FileBasedConfiguration.fileBasedConfiguration.getHttpBasePath() +
          body);
    }

    public final String readEnd() {
      return WaarpStringUtils.readFile(
          FileBasedConfiguration.fileBasedConfiguration.getHttpBasePath() +
          end);
    }
  }

  public static final int LIMITROW = 48;// better if it can be divided by 4

  /**
   * The Database connection attached to this NetworkChannel shared among all
   * associated LocalChannels in the
   * session
   */
  private DbSession dbSession;

  private String getTrimValue(final String varname) {
    String value = params.get(varname).get(0).trim();
    if (value.length() == 0) {
      value = null;
    }
    return value;
  }

  private String index() {
    final String index = REQUEST.index.readFileUnique();
    final StringBuilder builder = new StringBuilder(index);
    WaarpStringUtils.replace(builder, "XXXLOCALXXX",
                             FileBasedConfiguration.fileBasedConfiguration.getFtpInternalConfiguration()
                                                                          .getNumberSessions() +
                             " " + Thread.activeCount());
    final TrafficCounter trafficCounter =
        FileBasedConfiguration.fileBasedConfiguration.getFtpInternalConfiguration()
                                                     .getGlobalTrafficShapingHandler()
                                                     .trafficCounter();
    WaarpStringUtils.replace(builder, "XXXBANDWIDTHXXX", "IN:" +
                                                         trafficCounter.lastReadThroughput() /
                                                         131072 +
                                                         "Mbits&nbsp;<br>&nbsp;OUT:" +
                                                         trafficCounter.lastWriteThroughput() /
                                                         131072 + "Mbits");
    WaarpStringUtils.replaceAll(builder, "XXXHOSTIDXXX",
                                FileBasedConfiguration.fileBasedConfiguration.getHostId());
    WaarpStringUtils.replaceAll(builder, "XXXADMINXXX",
                                "Administrator Connected");
    WaarpStringUtils.replace(builder, "XXXVERSIONXXX",
                             Version.fullIdentifier());
    return builder.toString();
  }

  private String error(final String mesg) {
    final String index = REQUEST.error.readFileUnique();
    return index.replace("XXXERRORMESGXXX", mesg);
  }

  private String logon() {
    return REQUEST.Logon.readFileUnique();
  }

  private String system() {
    getParams();
    final FtpConstraintLimitHandler handler =
        FileBasedConfiguration.fileBasedConfiguration.getConstraintLimitHandler();
    if (params == null) {
      final String system = REQUEST.System.readFileUnique();
      final StringBuilder builder = new StringBuilder(system);
      WaarpStringUtils.replace(builder, "XXXXCHANNELLIMITRXXX", Long.toString(
          FileBasedConfiguration.fileBasedConfiguration.getServerGlobalReadLimit()));
      WaarpStringUtils.replace(builder, "XXXXCPULXXX",
                               Double.toString(handler.getCpuLimit()));
      WaarpStringUtils.replace(builder, "XXXXCONLXXX",
                               Integer.toString(handler.getChannelLimit()));
      WaarpStringUtils.replace(builder, XXXRESULTXXX, "");
      return builder.toString();
    }
    String extraInformation = null;
    if (params.containsKey(ACTION2)) {
      final List<String> action = params.get(ACTION2);
      for (final String act : action) {
        if ("Disconnect".equalsIgnoreCase(act)) {
          final String logon = logon();
          newSession = true;
          clearSession();
          forceClose = true;
          return logon;
        } else if ("Shutdown".equalsIgnoreCase(act)) {
          final String error = error("Shutdown in progress");
          newSession = true;
          clearSession();
          forceClose = true;
          shutdown = true;
          return error;
        } else if ("Validate".equalsIgnoreCase(act)) {
          String bglobalr = getTrimValue("BGLOBR");
          long lglobal =
              FileBasedConfiguration.fileBasedConfiguration.getServerGlobalReadLimit();
          if (bglobalr != null) {
            lglobal = Long.parseLong(bglobalr);
          }
          FileBasedConfiguration.fileBasedConfiguration.changeNetworkLimit(
              lglobal, lglobal);
          bglobalr = getTrimValue("CPUL");
          double dcpu = handler.getCpuLimit();
          if (bglobalr != null) {
            dcpu = Double.parseDouble(bglobalr);
          }
          handler.setCpuLimit(dcpu);
          bglobalr = getTrimValue("CONL");
          int iconn = handler.getChannelLimit();
          if (bglobalr != null) {
            iconn = Integer.parseInt(bglobalr);
          }
          handler.setChannelLimit(iconn);
          extraInformation = "Configuration Saved";
        }
      }
    }
    final String system = REQUEST.System.readFileUnique();
    final StringBuilder builder = new StringBuilder(system);
    WaarpStringUtils.replace(builder, "XXXXCHANNELLIMITRXXX", Long.toString(
        FileBasedConfiguration.fileBasedConfiguration.getServerGlobalReadLimit()));
    WaarpStringUtils.replace(builder, "XXXXCPULXXX",
                             Double.toString(handler.getCpuLimit()));
    WaarpStringUtils.replace(builder, "XXXXCONLXXX",
                             Integer.toString(handler.getChannelLimit()));
    if (extraInformation != null) {
      WaarpStringUtils.replace(builder, XXXRESULTXXX, extraInformation);
    } else {
      WaarpStringUtils.replace(builder, XXXRESULTXXX, "");
    }
    return builder.toString();
  }

  private String rule() {
    getParams();
    if (params == null) {
      final String system = REQUEST.Rule.readFileUnique();
      final StringBuilder builder = new StringBuilder(system);
      final CommandExecutor exec = AbstractExecutor.getCommandExecutor();
      WaarpStringUtils.replace(builder, "XXXSTCXXX",
                               exec.getStorType() + ' ' + exec.pstorCMD);
      WaarpStringUtils.replace(builder, "XXXSTDXXX",
                               Long.toString(exec.getPstorDelay()));
      WaarpStringUtils.replace(builder, "XXXRTCXXX",
                               exec.getRetrType() + ' ' + exec.pretrCMD);
      WaarpStringUtils.replace(builder, "XXXRTDXXX",
                               Long.toString(exec.getPretrDelay()));
      WaarpStringUtils.replace(builder, XXXRESULTXXX, "");
      return builder.toString();
    }
    String extraInformation = null;
    if (params.containsKey(ACTION2)) {
      final List<String> action = params.get(ACTION2);
      for (final String act : action) {
        if ("Update".equalsIgnoreCase(act)) {
          final CommandExecutor exec = AbstractExecutor.getCommandExecutor();
          String bglobalr = getTrimValue("std");
          long lglobal = exec.getPstorDelay();
          if (bglobalr != null) {
            lglobal = Long.parseLong(bglobalr);
          }
          exec.setPstorDelay(lglobal);
          bglobalr = getTrimValue("rtd");
          lglobal = exec.getPretrDelay();
          if (bglobalr != null) {
            lglobal = Long.parseLong(bglobalr);
          }
          exec.setPretrDelay(lglobal);
          bglobalr = getTrimValue("stc");
          String store = exec.getStorType() + ' ' + exec.pstorCMD;
          if (bglobalr != null) {
            store = bglobalr;
          }
          bglobalr = getTrimValue("rtc");
          String retr = exec.getRetrType() + ' ' + exec.pretrCMD;
          if (bglobalr != null) {
            retr = bglobalr;
          }
          AbstractExecutor.initializeExecutor(retr, exec.getPretrDelay(), store,
                                              exec.getPstorDelay());
          extraInformation = "Configuration Saved";
        }
      }
    }
    final String system = REQUEST.Rule.readFileUnique();
    final StringBuilder builder = new StringBuilder(system);
    final CommandExecutor exec = AbstractExecutor.getCommandExecutor();
    WaarpStringUtils.replace(builder, "XXXSTCXXX",
                             exec.getStorType() + ' ' + exec.pstorCMD);
    WaarpStringUtils.replace(builder, "XXXSTDXXX",
                             Long.toString(exec.getPstorDelay()));
    WaarpStringUtils.replace(builder, "XXXRTCXXX",
                             exec.getRetrType() + ' ' + exec.pretrCMD);
    WaarpStringUtils.replace(builder, "XXXRTDXXX",
                             Long.toString(exec.getPretrDelay()));
    if (extraInformation != null) {
      WaarpStringUtils.replace(builder, XXXRESULTXXX, extraInformation);
    } else {
      WaarpStringUtils.replace(builder, XXXRESULTXXX, "");
    }
    return builder.toString();
  }

  private String transfer() {
    getParams();
    final String head = REQUEST.Transfer.readHeader();
    String end = REQUEST.Transfer.readEnd();
    String body = REQUEST.Transfer.readBody();
    if (params == null) {
      end = end.replace(XXXRESULTXXX, "");
      body = FileBasedConfiguration.fileBasedConfiguration.getHtmlTransfer(body,
                                                                           LIMITROW);
      return head + body + end;
    }
    String message = "";
    final List<String> parms = params.get(ACTION2);
    if (parms != null) {
      final String parm = parms.get(0);
      boolean purgeAll = false;
      boolean purgeCorrect = false;
      boolean delete = false;
      if ("PurgeCorrectTransferLogs".equalsIgnoreCase(parm)) {
        purgeCorrect = true;
      } else if ("PurgeAllTransferLogs".equalsIgnoreCase(parm)) {
        purgeAll = true;
      } else if ("Delete".equalsIgnoreCase(parm)) {
        delete = true;
      }
      if (purgeCorrect || purgeAll) {
        DbPreparedStatement preparedStatement = null;
        ReplyCode status = null;
        String action = "purgeAll";

        if (purgeCorrect) {
          status = ReplyCode.REPLY_226_CLOSING_DATA_CONNECTION;
          action = "purge";
        }
        try {
          preparedStatement =
              DbTransferLog.getStatusPrepareStament(dbSession, status, 0);
        } catch (final WaarpDatabaseNoConnectionException e) {
          message = "Error during " + action;
        } catch (final WaarpDatabaseSqlException e) {
          message = "Error during " + action;
        }
        if (preparedStatement != null) {
          try {
            final FileBasedConfiguration config =
                FileBasedConfiguration.fileBasedConfiguration;
            final String filename =
                config.getBaseDirectory() + DirInterface.SEPARATOR +
                config.getAdminName() + DirInterface.SEPARATOR +
                config.getHostId() + "_logs_" + System.currentTimeMillis() +
                ".xml";
            message = DbTransferLog.saveDbTransferLogFile(preparedStatement,
                                                          filename);
          } finally {
            preparedStatement.realClose();
          }
        }
      } else if (delete) {
        final String user = getTrimValue("user");
        final String acct = getTrimValue("account");
        final String specid = getTrimValue("specialid");
        final long specialId = Long.parseLong(specid);
        try {
          final DbTransferLog log =
              new DbTransferLog(dbSession, user, acct, specialId);
          final FileBasedConfiguration config =
              FileBasedConfiguration.fileBasedConfiguration;
          final String filename =
              config.getBaseDirectory() + DirInterface.SEPARATOR +
              config.getAdminName() + DirInterface.SEPARATOR +
              config.getHostId() + "_log_" + System.currentTimeMillis() +
              ".xml";
          message = log.saveDbTransferLog(filename);
        } catch (final WaarpDatabaseException e) {
          message = "Error during delete 1 Log";
        }
      } else {
        message = "No Action";
      }
      end = end.replace(XXXRESULTXXX, message);
    }
    end = end.replace(XXXRESULTXXX, "");
    body = FileBasedConfiguration.fileBasedConfiguration.getHtmlTransfer(body,
                                                                         LIMITROW);
    return head + body + end;
  }

  private String user() {
    getParams();
    final String head = REQUEST.User.readHeader();
    String end = REQUEST.User.readEnd();
    String body = REQUEST.User.readBody();
    final FileBasedConfiguration config =
        FileBasedConfiguration.fileBasedConfiguration;
    final String filedefault =
        config.getBaseDirectory() + DirInterface.SEPARATOR +
        config.getAdminName() + DirInterface.SEPARATOR + "authentication.xml";
    if (params == null) {
      end = end.replace(XXXRESULTXXX, "");
      end = end.replace(XXXFILEXXX, filedefault);
      body = FileBasedConfiguration.fileBasedConfiguration.getHtmlAuth(body);
      return head + body + end;
    }
    final List<String> parms = params.get(ACTION2);
    if (parms != null) {
      final String parm = parms.get(0);
      if ("ImportExport".equalsIgnoreCase(parm)) {
        String file = getTrimValue("file");
        final String exportImport = getTrimValue("export");
        String message = "";
        final boolean purge;
        purge = params.containsKey("purge");
        final boolean replace;
        replace = params.containsKey("replace");
        if (file == null) {
          file = filedefault;
        }
        end = end.replace(XXXFILEXXX, file);
        if ("import".equalsIgnoreCase(exportImport)) {
          if (!config.initializeAuthent(file, purge)) {
            message += "Cannot initialize Authentication from " + file;
          } else {
            message += "Initialization of Authentication OK from " + file;
            if (replace) {
              if (!config.saveAuthenticationFile(
                  config.getAuthenticationFile())) {
                message += " but cannot replace server authenticationFile";
              } else {
                message += " and replacement done";
              }
            }
          }
        } else {
          // export
          if (!config.saveAuthenticationFile(file)) {
            message += "Authentications CANNOT be saved into " + file;
          } else {
            message += "Authentications saved into " + file;
          }
        }
        end = end.replace(XXXRESULTXXX, message);
      } else {
        end = end.replace(XXXFILEXXX, filedefault);
      }
    }
    end = end.replace(XXXRESULTXXX, "");
    body = FileBasedConfiguration.fileBasedConfiguration.getHtmlAuth(body);
    return head + body + end;
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
      } else {
        params = null;
      }
    }
  }

  private void clearSession() {
    if (admin != null) {
      final FileBasedAuth auth = sessions.remove(admin.value());
      admin = null;
      if (auth != null) {
        auth.clear();
      }
    }
  }

  protected final void closeConnection() {
    if (dbSession != null &&
        dbSession != DbConstantFtp.gatewayAdmin.getSession()) {
      DbAdmin.decHttpSession();
      dbSession.disconnect();
    }
    dbSession = null;
  }

  private void checkAuthent(final ChannelHandlerContext ctx) {
    newSession = true;
    if (request.method() == HttpMethod.GET) {
      final String logon = logon();
      responseContent.append(logon);
      clearSession();
      writeResponse(ctx);
      return;
    } else if (request.method() == HttpMethod.POST) {
      getParams();
      if (params == null) {
        final String logon = logon();
        responseContent.append(logon);
        clearSession();
        writeResponse(ctx);
        return;
      }
    }
    boolean getMenu = false;
    if (params.containsKey("Logon")) {
      String name = null;
      String password = null;
      List<String> values;
      // get values
      if (params.containsKey("name")) {
        values = params.get("name");
        if (values != null) {
          name = values.get(0);
          if (name == null || name.length() == 0) {
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
          getMenu = password == null || password.length() == 0;
        } else {
          getMenu = true;
        }
      } else {
        getMenu = true;
      }
      if (!getMenu && name != null) {
        logger.debug("Name={} vs {} Pass={} vs {}", name, name.equals(
                         FileBasedConfiguration.fileBasedConfiguration.getAdminName()),
                     password,
                     FileBasedConfiguration.fileBasedConfiguration.checkPassword(
                         password));
        if (name.equals(
            FileBasedConfiguration.fileBasedConfiguration.getAdminName()) &&
            FileBasedConfiguration.fileBasedConfiguration.checkPassword(
                password)) {
          authentHttp.specialNoSessionAuth(
              FileBasedConfiguration.fileBasedConfiguration.getHostId());
        } else {
          getMenu = true;
        }
        if (!authentHttp.isIdentified()) {
          logger.info("Still not authenticated: {}", authentHttp);
          getMenu = true;
        }
      }
    } else {
      getMenu = true;
    }
    if (getMenu) {
      final String logon = logon();
      responseContent.append(logon);
      clearSession();
    } else {
      final String index = index();
      responseContent.append(index);
      clearSession();
      admin = new DefaultCookie(FTPSESSIONString,
                                FileBasedConfiguration.fileBasedConfiguration.getHostId() +
                                Long.toHexString(RANDOM.nextLong()));
      sessions.put(admin.value(), authentHttp);
      logger.debug("CreateSession: {}}:{}", uriRequest, admin);
    }
    writeResponse(ctx);
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx,
                              final FullHttpRequest msg) {
    request = msg;
    final QueryStringDecoder queryStringDecoder =
        new QueryStringDecoder(request.uri());
    uriRequest = queryStringDecoder.path();
    if (uriRequest.contains("gre/") || uriRequest.contains("img/") ||
        uriRequest.contains("res/")) {
      HttpWriteCacheEnable.writeFile(request, ctx,
                                     FileBasedConfiguration.fileBasedConfiguration.getHttpBasePath() +
                                     uriRequest, FTPSESSIONString);
      return;
    }
    checkSession();
    try {
      if (!authentHttp.isIdentified()) {
        logger.debug("Not Authent: {}}:{}", uriRequest, authentHttp);
        checkAuthent(ctx);
        return;
      }
      String find = uriRequest;
      if (uriRequest.charAt(0) == '/') {
        find = uriRequest.substring(1);
      }
      find = find.substring(0, find.indexOf('.'));
      REQUEST req = REQUEST.index;
      try {
        req = REQUEST.valueOf(find);
      } catch (final IllegalArgumentException e1) {
        req = REQUEST.index;
        logger.info("NotFound: {}:{}", find, uriRequest);
      }
      switch (req) {
        case System:
          responseContent.append(system());
          break;
        case Rule:
          responseContent.append(rule());
          break;
        case User:
          responseContent.append(user());
          break;
        case Transfer:
          responseContent.append(transfer());
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

  private void checkSession() {
    final String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
    if (cookieString != null) {
      final Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
      if (!cookies.isEmpty()) {
        for (final Cookie elt : cookies) {
          if (elt.name().equalsIgnoreCase(FTPSESSIONString)) {
            admin = elt;
            break;
          }
        }
      }
    }
    // load DbSession
    try {
      dbSession = new DbSession(DbConstantFtp.gatewayAdmin, false);
      DbAdmin.incHttpSession();
    } catch (final WaarpDatabaseNoConnectionException e1) {
      // Cannot connect so use default connection
      logger.warn("Use default database connection");
      dbSession = DbConstantFtp.gatewayAdmin.getSession();
    }
    if (admin != null) {
      final FileBasedAuth auth = sessions.get(admin.value());
      if (auth != null) {
        authentHttp = auth;
      }
    } else {
      logger.info("NoSession: {}:{}", uriRequest, admin);
    }
  }

  private void handleCookies(final HttpResponse response) {
    final String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
    if (cookieString != null) {
      final Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
      if (!cookies.isEmpty()) {
        // Reset the sessions if necessary.
        boolean findSession = false;
        for (final Cookie cookie : cookies) {
          if (cookie.name().equalsIgnoreCase(FTPSESSIONString)) {
            if (newSession) {
              findSession = false;
            } else {
              findSession = true;
              response.headers().add(HttpHeaderNames.SET_COOKIE,
                                     ServerCookieEncoder.LAX.encode(cookie));
            }
          } else {
            response.headers().add(HttpHeaderNames.SET_COOKIE,
                                   ServerCookieEncoder.LAX.encode(cookie));
          }
        }
        newSession = false;
        if (!findSession && admin != null) {
          response.headers().add(HttpHeaderNames.SET_COOKIE,
                                 ServerCookieEncoder.LAX.encode(admin));
          logger.debug("AddSession: {}:{}", uriRequest, admin);
        }
      }
    } else if (admin != null) {
      logger.debug("AddSession: {}:{}", uriRequest, admin);
      response.headers().add(HttpHeaderNames.SET_COOKIE,
                             ServerCookieEncoder.LAX.encode(admin));
    }
  }

  /**
   * Write the response
   *
   * @param ctx
   */
  private void writeResponse(final ChannelHandlerContext ctx) {
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
      FtpChannelUtils.teminateServer(
          FileBasedConfiguration.fileBasedConfiguration);
      if (!close) {
        future.addListener(WaarpSslUtility.SSLCLOSE);
      }
    }
  }

  /**
   * Send an error and close
   *
   * @param ctx
   * @param status
   */
  private void sendError(final ChannelHandlerContext ctx,
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
    clearSession();
    // Close the connection as soon as the error message is sent.
    ctx.channel().writeAndFlush(response).addListener(WaarpSslUtility.SSLCLOSE);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx,
                              final Throwable cause) {
    if (!(cause instanceof CommandAbstractException)) {
      if (cause instanceof IOException) {
        // Nothing to do
        return;
      }
      logger.warn("Exception in HttpSslHandler: {}", cause.getMessage());
    }
    if (ctx.channel().isActive()) {
      sendError(ctx, HttpResponseStatus.BAD_REQUEST);
    }
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    final Channel channel = ctx.channel();
    logger.debug("Add channel to ssl");
    FileBasedConfiguration.fileBasedConfiguration.getHttpChannelGroup()
                                                 .add(channel);
    super.channelActive(ctx);
  }
}
