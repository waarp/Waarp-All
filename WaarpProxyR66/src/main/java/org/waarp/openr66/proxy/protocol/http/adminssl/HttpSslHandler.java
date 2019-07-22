/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.proxy.protocol.http.adminssl;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.exception.FileTransferException;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.gateway.kernel.http.HttpWriteCacheEnable;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66Exception;
import org.waarp.openr66.protocol.exception.OpenR66ExceptionTrappedFactory;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessNoWriteBackException;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66ShutdownHook;
import org.waarp.openr66.proxy.configuration.Configuration;
import org.waarp.openr66.proxy.utils.Version;

/**
 * @author Frederic Bregier
 * 
 */
public class HttpSslHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(HttpSslHandler.class);
    /**
     * Session Management
     */
    private static final ConcurrentHashMap<String, R66Session> sessions = new ConcurrentHashMap<String, R66Session>();
    private static final Random random = new Random();

    private R66Session authentHttp = new R66Session();

    private FullHttpRequest request;
    private boolean newSession = false;
    private volatile Cookie admin = null;
    private final StringBuilder responseContent = new StringBuilder();
    private String uriRequest;
    private Map<String, List<String>> params;
    private String lang = Messages.getSlocale();
    private volatile boolean forceClose = false;
    private volatile boolean shutdown = false;

    private static final String R66SESSION = "R66SESSION";
    private static final String I18NEXT = "i18next";

    private static enum REQUEST {
        Logon("Logon.html"),
        index("index.html"),
        error("error.html"),
        System("System.html");

        private String header;

        /**
         * Constructor for a unique file
         * 
         * @param uniquefile
         */
        private REQUEST(String uniquefile) {
            this.header = uniquefile;
        }

        /**
         * Reader for a unique file
         * 
         * @return the content of the unique file
         */
        public String readFileUnique(HttpSslHandler handler) {
            return handler.readFileHeader(Configuration.configuration.getHttpBasePath() + this.header);
        }
    }

    private static enum REPLACEMENT {
        XXXHOSTIDXXX, XXXADMINXXX, XXXVERSIONXXX, XXXBANDWIDTHXXX,
        XXXXSESSIONLIMITRXXX, XXXXSESSIONLIMITWXXX,
        XXXXCHANNELLIMITRXXX, XXXXCHANNELLIMITWXXX,
        XXXXDELAYCOMMDXXX, XXXXDELAYRETRYXXX,
        XXXLOCALXXX, XXXNETWORKXXX,
        XXXERRORMESGXXX,
        XXXLANGXXX, XXXCURLANGENXXX, XXXCURLANGFRXXX, XXXCURSYSLANGENXXX, XXXCURSYSLANGFRXXX;
    }

    static final int LIMITROW = 48; // better if it can
                                           // be divided by 4

    private String readFileHeader(String filename) {
        String value;
        try {
            value = WaarpStringUtils.readFileException(filename);
        } catch (InvalidArgumentException e) {
            logger.error("Error while trying to open: " + filename, e);
            return "";
        } catch (FileTransferException e) {
            logger.error("Error while trying to read: " + filename, e);
            return "";
        }
        StringBuilder builder = new StringBuilder(value);
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXLOCALXXX.toString(),
                Integer.toString(
                        Configuration.configuration.getLocalTransaction().
                                getNumberLocalChannel()) + " " + Thread.activeCount());
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXNETWORKXXX.toString(),
                Integer.toString(
                        Configuration.configuration.getLocalTransaction().
                                getNumberLocalChannel()));
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXHOSTIDXXX.toString(),
                Configuration.configuration.getHOST_ID());
        if (authentHttp.isAuthenticated()) {
            WaarpStringUtils.replace(builder, REPLACEMENT.XXXADMINXXX.toString(),
                    Messages.getString("HttpSslHandler.1")); //$NON-NLS-1$
        } else {
            WaarpStringUtils.replace(builder, REPLACEMENT.XXXADMINXXX.toString(),
                    Messages.getString("HttpSslHandler.0")); //$NON-NLS-1$
        }
        TrafficCounter trafficCounter =
                Configuration.configuration.getGlobalTrafficShapingHandler().trafficCounter();
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXBANDWIDTHXXX.toString(),
                Messages.getString("HttpSslHandler.IN") + (trafficCounter.lastReadThroughput() >> 20) + //$NON-NLS-1$
                Messages.getString("HttpSslHandler.MOPS") + //$NON-NLS-1$
                Messages.getString("HttpSslHandler.OUT") + //$NON-NLS-1$
                (trafficCounter.lastWriteThroughput() >> 20) +
                Messages.getString("HttpSslHandler.MOPS")); //$NON-NLS-1$
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXLANGXXX.toString(), lang);
        return builder.toString();
    }

    private String getTrimValue(String varname) {
        String value = params.get(varname).get(0).trim();
        if (value.isEmpty()) {
            value = null;
        }
        return value;
    }

    private String index() {
        String index = REQUEST.index.readFileUnique(this);
        StringBuilder builder = new StringBuilder(index);
        WaarpStringUtils.replaceAll(builder, REPLACEMENT.XXXHOSTIDXXX.toString(),
                Configuration.configuration.getHOST_ID());
        WaarpStringUtils.replaceAll(builder, REPLACEMENT.XXXADMINXXX.toString(),
                "Administrator Connected");
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXVERSIONXXX.toString(),
                Version.ID);
        return builder.toString();
    }

    private String error(String mesg) {
        String index = REQUEST.error.readFileUnique(this);
        return index.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                mesg);
    }

    private String Logon() {
        return REQUEST.Logon.readFileUnique(this);
    }

    /**
     * Applied current lang to system page
     * 
     * @param builder
     */
    private void langHandle(StringBuilder builder) {
        // i18n: add here any new languages
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURLANGENXXX.name(), lang.equalsIgnoreCase("en") ? "checked"
                : "");
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURLANGFRXXX.name(), lang.equalsIgnoreCase("fr") ? "checked"
                : "");
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURSYSLANGENXXX.name(),
                Messages.getSlocale().equalsIgnoreCase("en") ? "checked" : "");
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXCURSYSLANGFRXXX.name(),
                Messages.getSlocale().equalsIgnoreCase("fr") ? "checked" : "");
    }

    /**
     * @param builder
     */
    private void replaceStringSystem(StringBuilder builder) {
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXXSESSIONLIMITWXXX.toString(),
                Long.toString(Configuration.configuration.getServerChannelWriteLimit()));
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXXSESSIONLIMITRXXX.toString(),
                Long.toString(Configuration.configuration.getServerChannelReadLimit()));
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXXDELAYCOMMDXXX.toString(),
                Long.toString(Configuration.configuration.getDelayCommander()));
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXXDELAYRETRYXXX.toString(),
                Long.toString(Configuration.configuration.getDelayRetry()));
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXXCHANNELLIMITWXXX.toString(),
                Long.toString(Configuration.configuration.getServerGlobalWriteLimit()));
        WaarpStringUtils.replace(builder, REPLACEMENT.XXXXCHANNELLIMITRXXX.toString(),
                Long.toString(Configuration.configuration.getServerGlobalReadLimit()));
        WaarpStringUtils.replace(builder, "XXXBLOCKXXX", Configuration.configuration.isShutdown() ? "checked" : "");
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
            String system = REQUEST.System.readFileUnique(this);
            StringBuilder builder = new StringBuilder(system);
            replaceStringSystem(builder);
            langHandle(builder);
            return builder.toString();
        }
        String extraInformation = null;
        if (params.containsKey("ACTION")) {
            List<String> action = params.get("ACTION");
            for (String act : action) {
                if (act.equalsIgnoreCase("Language")) {
                    lang = getTrimValue("change");
                    String sys = getTrimValue("changesys");
                    Messages.init(new Locale(sys));
                    extraInformation = Messages.getString("HttpSslHandler.LangIs") + "Web: " + lang + " OpenR66: " + Messages.getSlocale(); //$NON-NLS-1$
                } else if (act.equalsIgnoreCase("Level")) {
                    String loglevel = getTrimValue("loglevel");
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
                    extraInformation = Messages.getString("HttpSslHandler.LangIs") + level.name(); //$NON-NLS-1$
                } else if (act.equalsIgnoreCase("Disconnect")) {
                    String logon = Logon();
                    logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                            Messages.getString("HttpSslHandler.DisActive"));
                    newSession = true;
                    clearSession();
                    forceClose = true;
                    return logon;
                } else if (act.equalsIgnoreCase("Shutdown")) {
                    String error;
                    if (Configuration.configuration.getShutdownConfiguration().serviceFuture != null) {
                        error = error(Messages.getString("HttpSslHandler.38")); //$NON-NLS-1$
                    } else {
                        error = error(Messages.getString("HttpSslHandler.37")); //$NON-NLS-1$
                    }
                    R66ShutdownHook.setRestart(false);
                    newSession = true;
                    clearSession();
                    forceClose = true;
                    shutdown = true;
                    return error;
                } else if (act.equalsIgnoreCase("Restart")) {
                    String error;
                    if (Configuration.configuration.getShutdownConfiguration().serviceFuture != null) {
                        error = error(Messages.getString("HttpSslHandler.38")); //$NON-NLS-1$
                    } else {
                        error = error(Messages.getString("HttpSslHandler.39") + (Configuration.configuration.getTIMEOUTCON() * 2 / 1000) + Messages.getString("HttpSslHandler.40")); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    error = error.replace("XXXRELOADHTTPXXX", "HTTP-EQUIV=\"refresh\" CONTENT=\""
                            + (Configuration.configuration.getTIMEOUTCON() * 2 / 1000) + "\"");
                    R66ShutdownHook.setRestart(true);
                    newSession = true;
                    clearSession();
                    forceClose = true;
                    shutdown = true;
                    return error;
                } else if (act.equalsIgnoreCase("Validate")) {
                    String bsessionr = getTrimValue("BSESSR");
                    long lsessionr = Configuration.configuration.getServerChannelReadLimit();
                    long lglobalr;
                    long lsessionw;
                    long lglobalw;
                    try {
                        if (bsessionr != null) {
                            lsessionr = (Long.parseLong(bsessionr) / 10) * 10;
                        }
                        String bglobalr = getTrimValue("BGLOBR");
                        lglobalr = Configuration.configuration.getServerGlobalReadLimit();
                        if (bglobalr != null) {
                            lglobalr = (Long.parseLong(bglobalr) / 10) * 10;
                        }
                        String bsessionw = getTrimValue("BSESSW");
                        lsessionw = Configuration.configuration.getServerChannelWriteLimit();
                        if (bsessionw != null) {
                            lsessionw = (Long.parseLong(bsessionw) / 10) * 10;
                        }
                        String bglobalw = getTrimValue("BGLOBW");
                        lglobalw = Configuration.configuration.getServerGlobalWriteLimit();
                        if (bglobalw != null) {
                            lglobalw = (Long.parseLong(bglobalw) / 10) * 10;
                        }
                        Configuration.configuration.changeNetworkLimit(
                                lglobalw, lglobalr, lsessionw, lsessionr,
                                Configuration.configuration.getDelayLimit());
                        String dcomm = getTrimValue("DCOM");
                        if (dcomm != null) {
                            Configuration.configuration.setDelayCommander(Long.parseLong(dcomm));
                            if (Configuration.configuration.getDelayCommander() <= 100) {
                                Configuration.configuration.setDelayCommander(100);
                            }
                            Configuration.configuration.reloadCommanderDelay();
                        }
                        String dret = getTrimValue("DRET");
                        if (dret != null) {
                            Configuration.configuration.setDelayRetry(Long.parseLong(dret));
                            if (Configuration.configuration.getDelayRetry() <= 1000) {
                                Configuration.configuration.setDelayRetry(1000);
                            }
                        }
                        extraInformation = Messages.getString("HttpSslHandler.41"); //$NON-NLS-1$
                    } catch (NumberFormatException e) {
                        extraInformation = Messages.getString("HttpSslHandler.42"); //$NON-NLS-1$
                    }
                }
            }
        }
        String system = REQUEST.System.readFileUnique(this);
        StringBuilder builder = new StringBuilder(system);
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
            ByteBuf content = request.content();
            if (content.isReadable()) {
                String param = content.toString(WaarpStringUtils.UTF8);
                QueryStringDecoder queryStringDecoder2 = new QueryStringDecoder("/?" + param);
                params = queryStringDecoder2.parameters();
            } else {
                params = null;
            }
        }
    }

    private void clearSession() {
        if (admin != null) {
            R66Session lsession = sessions.remove(admin.value());
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
            logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                    "");
            responseContent.append(logon);
            clearSession();
            writeResponse(ctx);
            return;
        } else if (request.method() == HttpMethod.POST) {
            getParams();
            if (params == null) {
                String logon = Logon();
                logon = logon.replaceAll(REPLACEMENT.XXXERRORMESGXXX.toString(),
                        Messages.getString("HttpSslHandler.EmptyLogin"));
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
                logger.debug("Name=" + name + " vs "
                        + name.equals(Configuration.configuration.getADMINNAME()) +
                        " Passwd vs " + Arrays.equals(password.getBytes(WaarpStringUtils.UTF8),
                                Configuration.configuration.getSERVERADMINKEY()));
                if (name.equals(Configuration.configuration.getADMINNAME()) &&
                        Arrays.equals(password.getBytes(WaarpStringUtils.UTF8),
                                Configuration.configuration.getSERVERADMINKEY())) {
                    authentHttp.getAuth().specialNoSessionAuth(true,
                            Configuration.configuration.getHOST_ID());
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
            String index = index();
            responseContent.append(index);
            clearSession();
            admin = new DefaultCookie(R66SESSION + Configuration.configuration.getHOST_ID(),
                    Configuration.configuration.getHOST_ID() +
                            Long.toHexString(random.nextLong()));
            sessions.put(admin.value(), this.authentHttp);
            authentHttp.setStatus(72);
            logger.debug("CreateSession: " + uriRequest + ":{}", admin);
            writeResponse(ctx);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        FullHttpRequest request = this.request = msg;
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(request.uri());
        uriRequest = queryStringDecoder.path();
        logger.debug("Msg: " + uriRequest);
        if (uriRequest.contains("gre/") || uriRequest.contains("img/") ||
                uriRequest.contains("res/") || uriRequest.contains("favicon.ico")) {
            HttpWriteCacheEnable.writeFile(request,
                    ctx, Configuration.configuration.getHttpBasePath() + uriRequest,
                    R66SESSION + Configuration.configuration.getHOST_ID());
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
        find = find.substring(0, find.indexOf("."));
        REQUEST req = REQUEST.index;
        try {
            req = REQUEST.valueOf(find);
        } catch (IllegalArgumentException e1) {
            req = REQUEST.index;
            logger.debug("NotFound: " + find + ":" + uriRequest);
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
        String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
        if (cookieString != null) {
            Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
            if (!cookies.isEmpty()) {
                for (Cookie elt : cookies) {
                    if (elt.name().equalsIgnoreCase(R66SESSION + Configuration.configuration.getHOST_ID())) {
                        logger.debug("Found session: " + elt);
                        admin = elt;
                        R66Session session = sessions.get(admin.value());
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

    private void handleCookies(HttpResponse response) {
        String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
        boolean i18nextFound = false;
        if (cookieString != null) {
            Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
            if (!cookies.isEmpty()) {
                // Reset the sessions if necessary.
                boolean findSession = false;
                for (Cookie cookie : cookies) {
                    if (cookie.name().equalsIgnoreCase(R66SESSION + Configuration.configuration.getHOST_ID())) {
                        if (newSession) {
                            findSession = false;
                        } else {
                            findSession = true;
                            response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
                        }
                    } else if (cookie.name().equalsIgnoreCase(I18NEXT)) {
                        i18nextFound = true;
                        cookie.setValue(lang);
                        response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
                    } else {
                        response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
                    }
                }
                if (!i18nextFound) {
                    Cookie cookie = new DefaultCookie(I18NEXT, lang);
                    response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
                }
                newSession = false;
                if (!findSession) {
                    if (admin != null) {
                        response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(admin));
                        logger.debug("AddSession: " + uriRequest + ":{}", admin);
                    }
                }
            }
        } else {
            Cookie cookie = new DefaultCookie(I18NEXT, lang);
            response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
            if (admin != null) {
                logger.debug("AddSession: " + uriRequest + ":{}", admin);
                response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(admin));
            }
        }
    }

    /**
     * Write the response
     * 
     */
    private void writeResponse(ChannelHandlerContext ctx) {
        // Convert the response content to a ByteBuf.
        ByteBuf buf = Unpooled.copiedBuffer(responseContent.toString(),
                WaarpStringUtils.UTF8);
        responseContent.setLength(0);

        // Decide whether to close the connection or not.
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        boolean close = HttpHeaderValues.CLOSE.contentEqualsIgnoreCase(request
                .headers().get(HttpHeaderNames.CONNECTION)) ||
                (!keepAlive) || forceClose;

        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buf);
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
        if (keepAlive) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }
        if (!close) {
            // There's no need to add 'Content-Length' header
            // if this is the last response.
            response.headers().set(HttpHeaderNames.CONTENT_LENGTH,
                    String.valueOf(buf.readableBytes()));
        }

        handleCookies(response);

        // Write the response.
        ChannelFuture future = ctx.writeAndFlush(response);
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
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status,
                Unpooled.copiedBuffer(responseContent.toString(),
                        WaarpStringUtils.UTF8));
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
        responseContent.setLength(0);
        clearSession();
        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(WaarpSslUtility.SSLCLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        OpenR66Exception exception = OpenR66ExceptionTrappedFactory
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
        Channel channel = ctx.channel();
        Configuration.configuration.getHttpChannelGroup().add(channel);
        super.channelActive(ctx);
    }
}
