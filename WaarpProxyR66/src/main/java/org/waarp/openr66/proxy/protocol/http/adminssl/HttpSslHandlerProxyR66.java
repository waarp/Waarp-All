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
package org.waarp.openr66.proxy.protocol.http.adminssl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.traffic.TrafficCounter;
import org.waarp.common.exception.FileTransferException;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.gateway.kernel.http.HttpWriteCacheEnable;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.http.adminssl.HttpSslHandler;
import org.waarp.openr66.proxy.utils.Version;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.waarp.openr66.protocol.configuration.Configuration.*;

/**
 *
 */
public class HttpSslHandlerProxyR66 extends HttpSslHandler {
  private static final String XXXLEVEL4XXX2 = "XXXLEVEL4XXX";
  private static final String XXXLEVEL3XXX2 = "XXXLEVEL3XXX";
  private static final String XXXLEVEL2XXX2 = "XXXLEVEL2XXX";
  private static final String XXXLEVEL1XXX2 = "XXXLEVEL1XXX";
  private static final String CHECKED2 = "checked";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpSslHandlerProxyR66.class);

  private enum REQUEST {
    Logon("Logon.html"), index("index.html"), error("error.html"),
    System("System.html");

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
     * Reader for a unique file
     *
     * @return the content of the unique file
     */
    public String readFileUnique(HttpSslHandlerProxyR66 handler) {
      return handler.readFileHeader(configuration.getHttpBasePath() + header);
    }
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
                             configuration.getLocalTransaction()
                                          .getNumberLocalChannel() + " " +
                             Thread.activeCount());
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXNETWORKXXX.toString(),
                             Integer.toString(
                                 configuration.getLocalTransaction()
                                              .getNumberLocalChannel()));
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXHOSTIDXXX.toString(),
                             configuration.getHostId());
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
        configuration.getGlobalTrafficShapingHandler().trafficCounter();
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
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXLANGXXX.toString(), lang);
    return builder.toString();
  }

  private String index() {
    final String index = REQUEST.index.readFileUnique(this);
    final StringBuilder builder = new StringBuilder(index);
    WaarpStringUtils.replaceAll(builder, REPLACEMENT.XXXHOSTIDXXX.toString(),
                                configuration.getHostId());
    WaarpStringUtils.replaceAll(builder, REPLACEMENT.XXXADMINXXX.toString(),
                                "Administrator Connected");
    WaarpStringUtils
        .replace(builder, REPLACEMENT.XXXVERSIONXXX.toString(), Version.ID);
    return builder.toString();
  }

  /**
   * @param builder
   */
  private void replaceStringSystem(StringBuilder builder) {
    WaarpStringUtils
        .replace(builder, REPLACEMENT.XXXXSESSIONLIMITWXXX.toString(),
                 Long.toString(configuration.getServerChannelWriteLimit()));
    WaarpStringUtils
        .replace(builder, REPLACEMENT.XXXXSESSIONLIMITRXXX.toString(),
                 Long.toString(configuration.getServerChannelReadLimit()));
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXDELAYCOMMDXXX.toString(),
                             Long.toString(configuration.getDelayCommander()));
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXXDELAYRETRYXXX.toString(),
                             Long.toString(configuration.getDelayRetry()));
    WaarpStringUtils
        .replace(builder, REPLACEMENT.XXXXCHANNELLIMITWXXX.toString(),
                 Long.toString(configuration.getServerGlobalWriteLimit()));
    WaarpStringUtils
        .replace(builder, REPLACEMENT.XXXXCHANNELLIMITRXXX.toString(),
                 Long.toString(configuration.getServerGlobalReadLimit()));
    WaarpStringUtils.replace(builder, "XXXBLOCKXXX",
                             configuration.isShutdown()? CHECKED2 : "");
    switch (WaarpLoggerFactory.getLogLevel()) {
      case DEBUG:
        WaarpStringUtils.replace(builder, XXXLEVEL1XXX2, CHECKED2);
        WaarpStringUtils.replace(builder, XXXLEVEL2XXX2, "");
        WaarpStringUtils.replace(builder, XXXLEVEL3XXX2, "");
        WaarpStringUtils.replace(builder, XXXLEVEL4XXX2, "");
        break;
      case INFO:
        WaarpStringUtils.replace(builder, XXXLEVEL1XXX2, "");
        WaarpStringUtils.replace(builder, XXXLEVEL2XXX2, CHECKED2);
        WaarpStringUtils.replace(builder, XXXLEVEL3XXX2, "");
        WaarpStringUtils.replace(builder, XXXLEVEL4XXX2, "");
        break;
      case WARN:
        WaarpStringUtils.replace(builder, XXXLEVEL1XXX2, "");
        WaarpStringUtils.replace(builder, XXXLEVEL2XXX2, "");
        WaarpStringUtils.replace(builder, XXXLEVEL3XXX2, CHECKED2);
        WaarpStringUtils.replace(builder, XXXLEVEL4XXX2, "");
        break;
      case ERROR:
        WaarpStringUtils.replace(builder, XXXLEVEL1XXX2, "");
        WaarpStringUtils.replace(builder, XXXLEVEL2XXX2, "");
        WaarpStringUtils.replace(builder, XXXLEVEL3XXX2, "");
        WaarpStringUtils.replace(builder, XXXLEVEL4XXX2, CHECKED2);
        break;
      default:
        WaarpStringUtils.replace(builder, XXXLEVEL1XXX2, "");
        WaarpStringUtils.replace(builder, XXXLEVEL2XXX2, "");
        WaarpStringUtils.replace(builder, XXXLEVEL3XXX2, "");
        WaarpStringUtils.replace(builder, XXXLEVEL4XXX2, "");
        break;

    }
  }

  private String System() {
    getParams();
    if (params == null) {
      final String system = REQUEST.System.readFileUnique(this);
      final StringBuilder builder = new StringBuilder(system);
      replaceStringSystem(builder);
      langHandle(builder);
      return builder.toString();
    }
    String extraInformation = null;
    if (params.containsKey("ACTION")) {
      final List<String> action = params.get("ACTION");
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
        } else if ("Disconnect".equalsIgnoreCase(act)) {
          String logon = logon();
          logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                                   Messages
                                       .getString("HttpSslHandler.DisActive"));
          newSession = true;
          clearSession();
          forceClose = true;
          return logon;
        } else if ("Shutdown".equalsIgnoreCase(act)) {
          String error;
          if (configuration.getShutdownConfiguration().serviceFuture != null) {
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
          if (configuration.getShutdownConfiguration().serviceFuture != null) {
            error =
                error(Messages.getString("HttpSslHandler.38")); //$NON-NLS-1$
          } else {
            error = error(Messages.getString("HttpSslHandler.39")
                          //$NON-NLS-1$
                          + configuration.getTimeoutCon() * 2 / 1000 + Messages
                              .getString("HttpSslHandler.40")); //$NON-NLS-1$
          }
          error = error.replace("XXXRELOADHTTPXXX",
                                "HTTP-EQUIV=\"refresh\" CONTENT=\"" +
                                configuration.getTimeoutCon() * 2 / 1000 + '"');
          WaarpShutdownHook.setRestart(true);
          newSession = true;
          clearSession();
          forceClose = true;
          shutdown = true;
          return error;
        } else if ("Validate".equalsIgnoreCase(act)) {
          final String bsessionr = getTrimValue("BSESSR");
          long lsessionr = configuration.getServerChannelReadLimit();
          long lglobalr;
          long lsessionw;
          long lglobalw;
          try {
            if (bsessionr != null) {
              lsessionr = (Long.parseLong(bsessionr) / 10) * 10;
            }
            final String bglobalr = getTrimValue("BGLOBR");
            lglobalr = configuration.getServerGlobalReadLimit();
            if (bglobalr != null) {
              lglobalr = (Long.parseLong(bglobalr) / 10) * 10;
            }
            final String bsessionw = getTrimValue("BSESSW");
            lsessionw = configuration.getServerChannelWriteLimit();
            if (bsessionw != null) {
              lsessionw = (Long.parseLong(bsessionw) / 10) * 10;
            }
            final String bglobalw = getTrimValue("BGLOBW");
            lglobalw = configuration.getServerGlobalWriteLimit();
            if (bglobalw != null) {
              lglobalw = (Long.parseLong(bglobalw) / 10) * 10;
            }
            configuration
                .changeNetworkLimit(lglobalw, lglobalr, lsessionw, lsessionr,
                                    configuration.getDelayLimit());
            final String dcomm = getTrimValue("DCOM");
            if (dcomm != null) {
              configuration.setDelayCommander(Long.parseLong(dcomm));
              if (configuration.getDelayCommander() <= 100) {
                configuration.setDelayCommander(100);
              }
              configuration.reloadCommanderDelay();
            }
            final String dret = getTrimValue("DRET");
            if (dret != null) {
              configuration.setDelayRetry(Long.parseLong(dret));
              if (configuration.getDelayRetry() <= 1000) {
                configuration.setDelayRetry(1000);
              }
            }
            extraInformation =
                Messages.getString("HttpSslHandler.41"); //$NON-NLS-1$
          } catch (final NumberFormatException e) {
            extraInformation =
                Messages.getString("HttpSslHandler.42"); //$NON-NLS-1$
          }
        }
      }
    }
    final String system = REQUEST.System.readFileUnique(this);
    final StringBuilder builder = new StringBuilder(system);
    replaceStringSystem(builder);
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
      } else {
        params = null;
      }
    }
  }

  private void clearSession() {
    if (admin != null) {
      final R66Session lsession = sessions.remove(admin.value());
      admin = null;
      if (lsession != null) {
        lsession.setStatus(75);
        lsession.clear();
      }
    }
  }

  private void checkAuthent(ChannelHandlerContext ctx) {
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
      if (!getMenu) {
        logger.debug("Name=" + name + " vs " +
                     name.equals(configuration.getAdminName()) + " Passwd vs " +
                     Arrays.equals(password.getBytes(WaarpStringUtils.UTF8),
                                   configuration.getServerAdminKey()));
        if (name.equals(configuration.getAdminName()) && Arrays
            .equals(password.getBytes(WaarpStringUtils.UTF8),
                    configuration.getServerAdminKey())) {
          authentHttp.getAuth()
                     .specialNoSessionAuth(true, configuration.getHostId());
          authentHttp.setStatus(70);
        } else {
          getMenu = true;
        }
        if (!authentHttp.isAuthenticated()) {
          authentHttp.setStatus(71);
          logger.debug("Still not authenticated: {}", authentHttp);
          getMenu = true;
        }
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
      admin = new DefaultCookie(R66SESSION + configuration.getHostId(),
                                configuration.getHostId() +
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
        uriRequest.contains("res/") || uriRequest.contains("favicon.ico")) {
      HttpWriteCacheEnable
          .writeFile(request, ctx, configuration.getHttpBasePath() + uriRequest,
                     R66SESSION + configuration.getHostId());
      ctx.flush();
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
      find = find.substring(0, find.indexOf('.'));
      try {
        req = REQUEST.valueOf(find);
      } catch (final IllegalArgumentException e1) {
        req = REQUEST.index;
        logger.debug("NotFound: " + find + ':' + uriRequest);
      }
    }
    switch (req) {
      case System:
        responseContent.append(System());
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
          if (elt.name()
                 .equalsIgnoreCase(R66SESSION + configuration.getHostId())) {
            logger.debug("Found session: " + elt);
            admin = elt;
            final R66Session session = sessions.get(admin.value());
            if (session != null) {
              authentHttp = session;
              authentHttp.setStatus(73);
            } else {
              admin = null;
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
}
