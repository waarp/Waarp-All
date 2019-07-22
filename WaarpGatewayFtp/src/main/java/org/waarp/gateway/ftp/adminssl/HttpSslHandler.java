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
package org.waarp.gateway.ftp.adminssl;

import java.io.IOException;
import java.util.List;
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

import org.waarp.common.command.ReplyCode;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.ftp.core.file.FtpDir;
import org.waarp.ftp.core.session.FtpSession;
import org.waarp.ftp.core.utils.FtpChannelUtils;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.gateway.ftp.control.FtpConstraintLimitHandler;
import org.waarp.gateway.ftp.database.DbConstant;
import org.waarp.gateway.ftp.database.data.DbTransferLog;
import org.waarp.gateway.ftp.file.FileBasedAuth;
import org.waarp.gateway.ftp.utils.Version;
import org.waarp.gateway.kernel.exec.AbstractExecutor;
import org.waarp.gateway.kernel.exec.AbstractExecutor.CommandExecutor;
import org.waarp.gateway.kernel.http.HttpWriteCacheEnable;

/**
 * @author Frederic Bregier
 * @author Bruno Carlin
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
    private static final ConcurrentHashMap<String, FileBasedAuth> sessions = new ConcurrentHashMap<String, FileBasedAuth>();
    private static final ConcurrentHashMap<String, DbSession> dbSessions = new ConcurrentHashMap<String, DbSession>();
    private static final Random random = new Random();

    private FtpSession ftpSession =
            new FtpSession(FileBasedConfiguration.fileBasedConfiguration,
                    null);
    private FileBasedAuth authentHttp =
            new FileBasedAuth(ftpSession);

    private FullHttpRequest request;
    private volatile boolean newSession = false;
    private volatile Cookie admin = null;
    private final StringBuilder responseContent = new StringBuilder();
    private String uriRequest;
    private Map<String, List<String>> params;
    private QueryStringDecoder queryStringDecoder;
    private volatile boolean forceClose = false;
    private volatile boolean shutdown = false;

    private static final String FTPSESSION = "FTPSESSION";

    private static enum REQUEST {
        Logon("Logon.html"),
        index("index.html"),
        error("error.html"),
        Transfer("Transfer_head.html", "Transfer_body.html", "Transfer_end.html"),
        Rule("Rule.html"),
        User("User_head.html", "User_body.html", "User_end.html"),
        System("System.html");

        private String header;
        private String body;
        private String end;

        /**
         * Constructor for a unique file
         * 
         * @param uniquefile
         */
        private REQUEST(String uniquefile) {
            this.header = uniquefile;
            this.body = null;
            this.end = null;
        }

        /**
         * @param header
         * @param body
         * @param end
         */
        private REQUEST(String header, String body, String end) {
            this.header = header;
            this.body = body;
            this.end = end;
        }

        /**
         * Reader for a unique file
         * 
         * @return the content of the unique file
         */
        public String readFileUnique() {
            return WaarpStringUtils
                    .readFile(FileBasedConfiguration.fileBasedConfiguration.httpBasePath
                            + this.header);
        }

        public String readHeader() {
            return WaarpStringUtils
                    .readFile(FileBasedConfiguration.fileBasedConfiguration.httpBasePath
                            + this.header);
        }

        public String readBody() {
            return WaarpStringUtils
                    .readFile(FileBasedConfiguration.fileBasedConfiguration.httpBasePath
                            + this.body);
        }

        public String readEnd() {
            return WaarpStringUtils
                    .readFile(FileBasedConfiguration.fileBasedConfiguration.httpBasePath + this.end);
        }
    }

    public static final int LIMITROW = 48;// better if it can be divided by 4

    /**
     * The Database connection attached to this NetworkChannel shared among all associated
     * LocalChannels in the session
     */
    private DbSession dbSession = null;
    /**
     * Does this dbSession is private and so should be closed
     */
    private volatile boolean isPrivateDbSession = false;

    private String getTrimValue(String varname) {
        String value = params.get(varname).get(0).trim();
        if (value.length() == 0) {
            value = null;
        }
        return value;
    }

    private String index() {
        String index = REQUEST.index.readFileUnique();
        StringBuilder builder = new StringBuilder(index);
        WaarpStringUtils.replace(builder, "XXXLOCALXXX",
                Integer.toString(
                        FileBasedConfiguration.fileBasedConfiguration.
                                getFtpInternalConfiguration().getNumberSessions())
                        + " " + Thread.activeCount());
        TrafficCounter trafficCounter =
                FileBasedConfiguration.fileBasedConfiguration.getFtpInternalConfiguration()
                        .getGlobalTrafficShapingHandler().trafficCounter();
        WaarpStringUtils.replace(builder, "XXXBANDWIDTHXXX",
                "IN:" + (trafficCounter.lastReadThroughput() / 131072) +
                        "Mbits&nbsp;<br>&nbsp;OUT:" +
                        (trafficCounter.lastWriteThroughput() / 131072) + "Mbits");
        WaarpStringUtils.replaceAll(builder, "XXXHOSTIDXXX",
                FileBasedConfiguration.fileBasedConfiguration.HOST_ID);
        WaarpStringUtils.replaceAll(builder, "XXXADMINXXX",
                "Administrator Connected");
        WaarpStringUtils.replace(builder, "XXXVERSIONXXX",
                Version.ID);
        return builder.toString();
    }

    private String error(String mesg) {
        String index = REQUEST.error.readFileUnique();
        return index.replaceAll("XXXERRORMESGXXX",
                mesg);
    }

    private String Logon() {
        return REQUEST.Logon.readFileUnique();
    }

    private String System() {
        getParams();
        FtpConstraintLimitHandler handler =
                FileBasedConfiguration.fileBasedConfiguration.constraintLimitHandler;
        if (params == null) {
            String system = REQUEST.System.readFileUnique();
            StringBuilder builder = new StringBuilder(system);
            WaarpStringUtils.replace(builder, "XXXXCHANNELLIMITRXXX",
                    Long.toString(FileBasedConfiguration.fileBasedConfiguration
                            .getServerGlobalReadLimit()));
            WaarpStringUtils.replace(builder, "XXXXCPULXXX",
                    Double.toString(handler.getCpuLimit()));
            WaarpStringUtils.replace(builder, "XXXXCONLXXX",
                    Integer.toString(handler.getChannelLimit()));
            WaarpStringUtils.replace(builder, "XXXRESULTXXX", "");
            return builder.toString();
        }
        String extraInformation = null;
        if (params.containsKey("ACTION")) {
            List<String> action = params.get("ACTION");
            for (String act : action) {
                if (act.equalsIgnoreCase("Disconnect")) {
                    String logon = Logon();
                    newSession = true;
                    clearSession();
                    forceClose = true;
                    return logon;
                } else if (act.equalsIgnoreCase("Shutdown")) {
                    String error = error("Shutdown in progress");
                    newSession = true;
                    clearSession();
                    forceClose = true;
                    shutdown = true;
                    return error;
                } else if (act.equalsIgnoreCase("Validate")) {
                    String bglobalr = getTrimValue("BGLOBR");
                    long lglobal = FileBasedConfiguration.fileBasedConfiguration
                            .getServerGlobalReadLimit();
                    if (bglobalr != null) {
                        lglobal = Long.parseLong(bglobalr);
                    }
                    FileBasedConfiguration.fileBasedConfiguration.changeNetworkLimit(lglobal,
                            lglobal);
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
        String system = REQUEST.System.readFileUnique();
        StringBuilder builder = new StringBuilder(system);
        WaarpStringUtils.replace(builder, "XXXXCHANNELLIMITRXXX",
                Long.toString(FileBasedConfiguration.fileBasedConfiguration
                        .getServerGlobalReadLimit()));
        WaarpStringUtils.replace(builder, "XXXXCPULXXX",
                Double.toString(handler.getCpuLimit()));
        WaarpStringUtils.replace(builder, "XXXXCONLXXX",
                Integer.toString(handler.getChannelLimit()));
        if (extraInformation != null) {
            WaarpStringUtils.replace(builder, "XXXRESULTXXX", extraInformation);
        } else {
            WaarpStringUtils.replace(builder, "XXXRESULTXXX", "");
        }
        return builder.toString();
    }

    private String Rule() {
        getParams();
        if (params == null) {
            String system = REQUEST.Rule.readFileUnique();
            StringBuilder builder = new StringBuilder(system);
            CommandExecutor exec = AbstractExecutor.getCommandExecutor();
            WaarpStringUtils.replace(builder, "XXXSTCXXX",
                    exec.getStorType() + " " + exec.pstorCMD);
            WaarpStringUtils.replace(builder, "XXXSTDXXX",
                    Long.toString(exec.pstorDelay));
            WaarpStringUtils.replace(builder, "XXXRTCXXX",
                    exec.getRetrType() + " " + exec.pretrCMD);
            WaarpStringUtils.replace(builder, "XXXRTDXXX",
                    Long.toString(exec.pretrDelay));
            WaarpStringUtils.replace(builder, "XXXRESULTXXX", "");
            return builder.toString();
        }
        String extraInformation = null;
        if (params.containsKey("ACTION")) {
            List<String> action = params.get("ACTION");
            for (String act : action) {
                if (act.equalsIgnoreCase("Update")) {
                    CommandExecutor exec = AbstractExecutor.getCommandExecutor();
                    String bglobalr = getTrimValue("std");
                    long lglobal = exec.pstorDelay;
                    if (bglobalr != null) {
                        lglobal = Long.parseLong(bglobalr);
                    }
                    exec.pstorDelay = lglobal;
                    bglobalr = getTrimValue("rtd");
                    lglobal = exec.pretrDelay;
                    if (bglobalr != null) {
                        lglobal = Long.parseLong(bglobalr);
                    }
                    exec.pretrDelay = lglobal;
                    bglobalr = getTrimValue("stc");
                    String store = exec.getStorType() + " " + exec.pstorCMD;
                    if (bglobalr != null) {
                        store = bglobalr;
                    }
                    bglobalr = getTrimValue("rtc");
                    String retr = exec.getRetrType() + " " + exec.pretrCMD;
                    if (bglobalr != null) {
                        retr = bglobalr;
                    }
                    AbstractExecutor.initializeExecutor(retr, exec.pretrDelay,
                            store, exec.pstorDelay);
                    extraInformation = "Configuration Saved";
                }
            }
        }
        String system = REQUEST.Rule.readFileUnique();
        StringBuilder builder = new StringBuilder(system);
        CommandExecutor exec = AbstractExecutor.getCommandExecutor();
        WaarpStringUtils.replace(builder, "XXXSTCXXX",
                exec.getStorType() + " " + exec.pstorCMD);
        WaarpStringUtils.replace(builder, "XXXSTDXXX",
                Long.toString(exec.pstorDelay));
        WaarpStringUtils.replace(builder, "XXXRTCXXX",
                exec.getRetrType() + " " + exec.pretrCMD);
        WaarpStringUtils.replace(builder, "XXXRTDXXX",
                Long.toString(exec.pretrDelay));
        if (extraInformation != null) {
            WaarpStringUtils.replace(builder, "XXXRESULTXXX", extraInformation);
        } else {
            WaarpStringUtils.replace(builder, "XXXRESULTXXX", "");
        }
        return builder.toString();
    }

    private String Transfer() {
        getParams();
        String head = REQUEST.Transfer.readHeader();
        String end = REQUEST.Transfer.readEnd();
        String body = REQUEST.Transfer.readBody();
        if (params == null || (!DbConstant.gatewayAdmin.isActive())) {
            end = end.replace("XXXRESULTXXX", "");
            body = FileBasedConfiguration.fileBasedConfiguration.getHtmlTransfer(body, LIMITROW);
            return head + body + end;
        }
        String message = "";
        List<String> parms = params.get("ACTION");
        if (parms != null) {
            String parm = parms.get(0);
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
                            DbTransferLog.getStatusPrepareStament(dbSession,
                                    status, 0);
                } catch (WaarpDatabaseNoConnectionException e) {
                    message = "Error during " + action;
                } catch (WaarpDatabaseSqlException e) {
                    message = "Error during " + action;
                }
                if (preparedStatement != null) {
                    try {
                        FileBasedConfiguration config = FileBasedConfiguration.fileBasedConfiguration;
                        String filename =
                                config.getBaseDirectory() +
                                        FtpDir.SEPARATOR + config.ADMINNAME + FtpDir.SEPARATOR +
                                        config.HOST_ID + "_logs_" + System.currentTimeMillis()
                                        + ".xml";
                        message = DbTransferLog.saveDbTransferLogFile(preparedStatement, filename);
                    } finally {
                        preparedStatement.realClose();
                    }
                }
            } else if (delete) {
                String user = getTrimValue("user");
                String acct = getTrimValue("account");
                String specid = getTrimValue("specialid");
                long specialId = Long.parseLong(specid);
                try {
                    DbTransferLog log = new DbTransferLog(dbSession, user, acct, specialId);
                    FileBasedConfiguration config = FileBasedConfiguration.fileBasedConfiguration;
                    String filename =
                            config.getBaseDirectory() +
                                    FtpDir.SEPARATOR + config.ADMINNAME + FtpDir.SEPARATOR +
                                    config.HOST_ID + "_log_" + System.currentTimeMillis() + ".xml";
                    message = log.saveDbTransferLog(filename);
                } catch (WaarpDatabaseException e) {
                    message = "Error during delete 1 Log";
                }
            } else {
                message = "No Action";
            }
            end = end.replace("XXXRESULTXXX", message);
        }
        end = end.replace("XXXRESULTXXX", "");
        body = FileBasedConfiguration.fileBasedConfiguration.getHtmlTransfer(body, LIMITROW);
        return head + body + end;
    }

    private String User() {
        getParams();
        String head = REQUEST.User.readHeader();
        String end = REQUEST.User.readEnd();
        String body = REQUEST.User.readBody();
        FileBasedConfiguration config = FileBasedConfiguration.fileBasedConfiguration;
        String filedefault = config.getBaseDirectory() +
                FtpDir.SEPARATOR + config.ADMINNAME +
                FtpDir.SEPARATOR + "authentication.xml";
        if (params == null) {
            end = end.replace("XXXRESULTXXX", "");
            end = end.replace("XXXFILEXXX", filedefault);
            body = FileBasedConfiguration.fileBasedConfiguration.getHtmlAuth(body);
            return head + body + end;
        }
        List<String> parms = params.get("ACTION");
        if (parms != null) {
            String parm = parms.get(0);
            if ("ImportExport".equalsIgnoreCase(parm)) {
                String file = getTrimValue("file");
                String exportImport = getTrimValue("export");
                String message = "";
                boolean purge = false;
                purge = params.containsKey("purge");
                boolean replace = false;
                replace = params.containsKey("replace");
                if (file == null) {
                    file = filedefault;
                }
                end = end.replace("XXXFILEXXX", file);
                if (exportImport.equalsIgnoreCase("import")) {
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
                end = end.replace("XXXRESULTXXX", message);
            } else {
                end = end.replace("XXXFILEXXX", filedefault);
            }
        }
        end = end.replace("XXXRESULTXXX", "");
        body = FileBasedConfiguration.fileBasedConfiguration.getHtmlAuth(body);
        return head + body + end;
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
            FileBasedAuth auth = sessions.remove(admin.value());
            DbSession ldbsession = dbSessions.remove(admin.value());
            admin = null;
            if (auth != null) {
                auth.clear();
            }
            if (ldbsession != null) {
                ldbsession.disconnect();
                DbAdmin.decHttpSession();
            }
        }
    }

    private void checkAuthent(ChannelHandlerContext ctx) {
        newSession = true;
        if (request.method() == HttpMethod.GET) {
            String logon = Logon();
            responseContent.append(logon);
            clearSession();
            writeResponse(ctx);
            return;
        } else if (request.method() == HttpMethod.POST) {
            getParams();
            if (params == null) {
                String logon = Logon();
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
                        if (name == null || name.length() == 0) {
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
                        if (password == null || password.length() == 0) {
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
                        + name.equals(FileBasedConfiguration.fileBasedConfiguration.ADMINNAME) +
                        " Passwd=" + password + " vs " +
                        FileBasedConfiguration.fileBasedConfiguration.checkPassword(password));
                if (name.equals(FileBasedConfiguration.fileBasedConfiguration.ADMINNAME) &&
                        FileBasedConfiguration.fileBasedConfiguration.checkPassword(password)) {
                    authentHttp
                            .specialNoSessionAuth(FileBasedConfiguration.fileBasedConfiguration.HOST_ID);
                } else {
                    getMenu = true;
                }
                if (!authentHttp.isIdentified()) {
                    logger.debug("Still not authenticated: {}", authentHttp);
                    getMenu = true;
                }
                // load DbSession
                if (this.dbSession == null) {
                    try {
                        if (DbConstant.gatewayAdmin.isActive()) {
                            this.dbSession = new DbSession(DbConstant.gatewayAdmin, false);
                            DbAdmin.incHttpSession();
                            this.isPrivateDbSession = true;
                        }
                    } catch (WaarpDatabaseNoConnectionException e1) {
                        // Cannot connect so use default connection
                        logger.warn("Use default database connection");
                        this.dbSession = DbConstant.gatewayAdmin.getSession();
                    }
                }
            }
        } else {
            getMenu = true;
        }
        if (getMenu) {
            String logon = Logon();
            responseContent.append(logon);
            clearSession();
            writeResponse(ctx);
        } else {
            String index = index();
            responseContent.append(index);
            clearSession();
            admin = new DefaultCookie(FTPSESSION,
                    FileBasedConfiguration.fileBasedConfiguration.HOST_ID +
                            Long.toHexString(random.nextLong()));
            sessions.put(admin.value(), this.authentHttp);
            if (this.isPrivateDbSession) {
                dbSessions.put(admin.value(), dbSession);
            }
            logger.debug("CreateSession: " + uriRequest + ":{}", admin);
            writeResponse(ctx);
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        this.request = msg;
        queryStringDecoder = new QueryStringDecoder(request.uri());
        uriRequest = queryStringDecoder.path();
        if (uriRequest.contains("gre/") || uriRequest.contains("img/") ||
                uriRequest.contains("res/")) {
            HttpWriteCacheEnable.writeFile(request,
                    ctx,
                    FileBasedConfiguration.fileBasedConfiguration.httpBasePath + uriRequest,
                    FTPSESSION);
            return;
        }
        checkSession(ctx.channel());
        if (!authentHttp.isIdentified()) {
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
            case Rule:
                responseContent.append(Rule());
                break;
            case User:
                responseContent.append(User());
                break;
            case Transfer:
                responseContent.append(Transfer());
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
                    if (elt.name().equalsIgnoreCase(FTPSESSION)) {
                        admin = elt;
                        break;
                    }
                }
            }
        }
        if (admin != null) {
            FileBasedAuth auth = sessions.get(admin.value());
            if (auth != null) {
                authentHttp = auth;
            }
            DbSession dbSession = dbSessions.get(admin.value());
            if (dbSession != null) {
                this.dbSession = dbSession;
            }
        } else {
            logger.debug("NoSession: " + uriRequest + ":{}", admin);
        }
    }

    private void handleCookies(HttpResponse response) {
        String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
        if (cookieString != null) {
            Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
            if (!cookies.isEmpty()) {
                // Reset the sessions if necessary.
                boolean findSession = false;
                for (Cookie cookie : cookies) {
                    if (cookie.name().equalsIgnoreCase(FTPSESSION)) {
                        if (newSession) {
                            findSession = false;
                        } else {
                            findSession = true;
                            response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
                        }
                    } else {
                        response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(cookie));
                    }
                }
                newSession = false;
                if (!findSession) {
                    if (admin != null) {
                        response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(admin));
                        logger.debug("AddSession: " + uriRequest + ":{}", admin);
                    }
                }
            }
        } else if (admin != null) {
            logger.debug("AddSession: " + uriRequest + ":{}", admin);
            response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX.encode(admin));
        }
    }

    /**
     * Write the response
     * 
     * @param ctx
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
            /*
             * Thread thread = new Thread( new FtpChannelUtils(
             * FileBasedConfiguration.fileBasedConfiguration)); thread.setDaemon(true);
             * thread.setName("Shutdown Thread"); thread.start();
             */
            FtpChannelUtils.teminateServer(FileBasedConfiguration.fileBasedConfiguration);
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
    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        responseContent.setLength(0);
        responseContent.append(error(status.toString()));
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer(responseContent.toString(), WaarpStringUtils.UTF8));
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
        clearSession();
        // Close the connection as soon as the error message is sent.
        ctx.channel().writeAndFlush(response).addListener(WaarpSslUtility.SSLCLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        Throwable e1 = cause;
        if (!(e1 instanceof CommandAbstractException)) {
            if (e1 instanceof IOException) {
                // Nothing to do
                return;
            }
            logger.warn("Exception in HttpSslHandler", e1);
        }
        if (ctx.channel().isActive()) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        logger.debug("Add channel to ssl");
        FileBasedConfiguration.fileBasedConfiguration.getHttpChannelGroup().add(channel);
        super.channelActive(ctx);
    }
}
