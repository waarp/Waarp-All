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
import org.waarp.openr66.proxy.configuration.Configuration;
import org.waarp.openr66.proxy.utils.Version;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 *
 */
public class HttpSslHandler
    extends org.waarp.openr66.protocol.http.adminssl.HttpSslHandler {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpSslHandler.class);

  private static enum REQUEST {
    Logon("Logon.html"), index("index.html"), error("error.html"),
    System("System.html");

    private final String header;

    /**
     * Constructor for a unique file
     *
     * @param uniquefile
     */
    private REQUEST(String uniquefile) {
      header = uniquefile;
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
                             Integer.toString(Configuration.configuration
                                                  .getLocalTransaction()
                                                  .getNumberLocalChannel()));
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
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXLANGXXX.toString(), lang);
    return builder.toString();
  }

  private String index() {
    final String index = REQUEST.index.readFileUnique(this);
    final StringBuilder builder = new StringBuilder(index);
    WaarpStringUtils.replaceAll(builder, REPLACEMENT.XXXHOSTIDXXX.toString(),
                                Configuration.configuration.getHOST_ID());
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
                 Long.toString(
                     Configuration.configuration.getServerChannelWriteLimit()));
    WaarpStringUtils
        .replace(builder, REPLACEMENT.XXXXSESSIONLIMITRXXX.toString(),
                 Long.toString(
                     Configuration.configuration.getServerChannelReadLimit()));
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
        } else if (act.equalsIgnoreCase("Disconnect")) {
          String logon = Logon();
          logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                                   Messages
                                       .getString("HttpSslHandler.DisActive"));
          newSession = true;
          clearSession();
          forceClose = true;
          return logon;
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
            Configuration.configuration
                .changeNetworkLimit(lglobalw, lglobalr, lsessionw, lsessionr,
                                    Configuration.configuration
                                        .getDelayLimit());
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
        logger.debug("Name=" + name + " vs " +
                     name.equals(Configuration.configuration.getADMINNAME()) +
                     " Passwd vs " + Arrays
                         .equals(password.getBytes(WaarpStringUtils.UTF8),
                                 Configuration.configuration
                                     .getSERVERADMINKEY()));
        if (name.equals(Configuration.configuration.getADMINNAME()) && Arrays
            .equals(password.getBytes(WaarpStringUtils.UTF8),
                    Configuration.configuration.getSERVERADMINKEY())) {
          authentHttp.getAuth().specialNoSessionAuth(true,
                                                     Configuration.configuration
                                                         .getHOST_ID());
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
      String logon = Logon();
      logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                               Messages.getString("HttpSslHandler.BadLogin"));
      responseContent.append(logon);
      clearSession();
      writeResponse(ctx);
    } else {
      final String index = index();
      responseContent.append(index);
      clearSession();
      admin = new DefaultCookie(
          R66SESSION + Configuration.configuration.getHOST_ID(),
          Configuration.configuration.getHOST_ID() +
          Long.toHexString(random.nextLong()));
      sessions.put(admin.value(), authentHttp);
      authentHttp.setStatus(72);
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
      find = find.substring(0, find.indexOf("."));
      try {
        req = REQUEST.valueOf(find);
      } catch (final IllegalArgumentException e1) {
        req = REQUEST.index;
        logger.debug("NotFound: " + find + ":" + uriRequest);
      }
    }
    switch (req) {
      case index:
        responseContent.append(index());
        break;
      case Logon:
        responseContent.append(index());
        break;
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
