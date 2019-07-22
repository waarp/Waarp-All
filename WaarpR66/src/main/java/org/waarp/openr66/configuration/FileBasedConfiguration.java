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
package org.waarp.openr66.configuration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import io.netty.handler.traffic.AbstractTrafficShapingHandler;
import org.waarp.common.crypto.Des;
import org.waarp.common.crypto.ssl.WaarpSecureKeyStore;
import org.waarp.common.crypto.ssl.WaarpSslContextFactory;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.DbRequest;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.database.model.DbType;
import org.waarp.common.database.ConnectionFactory;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.exception.CryptoException;
import org.waarp.common.file.DirInterface;
import org.waarp.common.file.filesystembased.FilesystemBasedDirImpl;
import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.role.RoleDefault;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.common.xml.XmlDecl;
import org.waarp.common.xml.XmlHash;
import org.waarp.common.xml.XmlRootHash;
import org.waarp.common.xml.XmlType;
import org.waarp.common.xml.XmlUtil;
import org.waarp.common.xml.XmlValue;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.openr66.context.R66BusinessFactoryInterface;
import org.waarp.openr66.context.authentication.R66Auth;
import org.waarp.openr66.context.task.localexec.LocalExecClient;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbConfiguration;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbHostConfiguration;
import org.waarp.openr66.database.model.DbModelFactory;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.configuration.R66SystemProperties;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.http.adminssl.HttpResponsiveSslHandler;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler.RESTHANDLERS;
import org.waarp.openr66.protocol.networkhandler.R66ConstraintLimitHandler;
import org.waarp.openr66.protocol.networkhandler.ssl.NetworkSslServerInitializer;
import org.waarp.openr66.protocol.utils.FileUtils;
import org.waarp.openr66.server.ServerInitDatabase;
import org.waarp.snmp.SnmpConfiguration;

/**
 * File Based Configuration
 *
 * @author frederic bregier
 *
 */
public class FileBasedConfiguration {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(FileBasedConfiguration.class);

    /**
     * XML_LOCALE
     */
    private static final String XML_LOCALE = "locale";

    /**
     * SERVER HOSTID
     */
    private static final String XML_SERVER_HOSTID = "hostid";

    /**
     * SERVER SSL HOSTID
     */
    private static final String XML_SERVER_SSLHOSTID = "sslhostid";

    /**
     * ADMINISTRATOR SERVER NAME (shutdown)
     */
    private static final String XML_SERVER_ADMIN = "serveradmin";

    /**
     * SERVER PASSWORD (shutdown)
     */
    private static final String XML_SERVER_PASSWD = "serverpasswd";
    /**
     * SERVER PASSWORD FILE (shutdown)
     */
    private static final String XML_SERVER_PASSWD_FILE = "serverpasswdfile";
    /**
     * Authentication
     */
    private static final String XML_AUTHENTIFICATION_FILE = "authentfile";

    /**
     * SERVER PORT
     */
    private static final String XML_SERVER_PORT = "serverport";

    /**
     * SERVER SSL PORT
     */
    private static final String XML_SERVER_SSLPORT = "serversslport";

    /**
     * SERVER HTTP PORT
     */
    private static final String XML_SERVER_HTTPPORT = "serverhttpport";

    /**
     * SERVER HTTPS PORT
     */
    private static final String XML_SERVER_HTTPSPORT = "serverhttpsport";

    /**
     * SERVER SSL STOREKEY PATH
     */
    private static final String XML_PATH_KEYPATH = "keypath";

    /**
     * SERVER SSL KEY PASS
     */
    private static final String XML_PATH_KEYPASS = "keypass";

    /**
     * SERVER SSL STOREKEY PASS
     */
    private static final String XML_PATH_KEYSTOREPASS = "keystorepass";

    /**
     * SERVER SSL TRUSTSTOREKEY PATH
     */
    private static final String XML_PATH_TRUSTKEYPATH = "trustkeypath";

    /**
     * SERVER SSL TRUSTSTOREKEY PASS
     */
    private static final String XML_PATH_TRUSTKEYSTOREPASS = "trustkeystorepass";

    /**
     * SERVER SSL STOREKEY PATH ADMIN
     */
    private static final String XML_PATH_ADMIN_KEYPATH = "admkeypath";

    /**
     * SERVER SSL KEY PASS ADMIN
     */
    private static final String XML_PATH_ADMIN_KEYPASS = "admkeypass";

    /**
     * SERVER SSL STOREKEY PASS ADMIN
     */
    private static final String XML_PATH_ADMIN_KEYSTOREPASS = "admkeystorepass";

    /**
     * SERVER CRYPTO for Password
     */
    private static final String XML_PATH_CRYPTOKEY = "cryptokey";
    /**
     * Base Directory
     */
    private static final String XML_SERVER_HOME = "serverhome";

    /**
     * IN Directory
     */
    private static final String XML_INPATH = "in";

    /**
     * OUT Directory
     */
    private static final String XML_OUTPATH = "out";

    /**
     * ARCHIVE Directory
     */
    private static final String XML_ARCHIVEPATH = "arch";

    /**
     * WORKING Directory
     */
    private static final String XML_WORKINGPATH = "work";

    /**
     * CONFIG Directory
     */
    private static final String XML_CONFIGPATH = "conf";

    /**
     * HTTP Admin Directory
     */
    private static final String XML_HTTPADMINPATH = "httpadmin";
    /**
     * HTTP Admin model (fix = 0 or responsive = 1, not mandatory since auto detection)
     */
    private static final String XML_HTTPADMINMODEL = "httpmodel";
    /**
     * Use SSL for R66 connection
     */
    private static final String XML_USESSL = "usessl";

    /**
     * Use non SSL for R66 connection
     */
    private static final String XML_USENOSSL = "usenossl";

    /**
     * Use HTTP compression for R66 HTTP connection
     */
    private static final String XML_USEHTTPCOMP = "usehttpcomp";

    /**
     * SERVER SSL Use TrustStore for Client Authentication
     */
    private static final String XML_USECLIENT_AUTHENT = "trustuseclientauthenticate";

    /**
     * Limit per session
     */
    private static final String XML_LIMITSESSION = "sessionlimit";

    /**
     * Limit global
     */
    private static final String XML_LIMITGLOBAL = "globallimit";

    /**
     * Delay between two checks for Limit
     */
    private static final String XML_LIMITDELAY = "delaylimit";
    /**
     * Monitoring: how long in ms to get back in monitoring
     */
    private static final String XML_MONITOR_PASTLIMIT = "pastlimit";
    /**
     * Monitoring: minimal interval in ms before redo real monitoring
     */
    private static final String XML_MONITOR_MINIMALDELAY = "minimaldelay";
    /**
     * Monitoring: snmp configuration file (if empty, no snmp support)
     */
    private static final String XML_MONITOR_SNMP_CONFIG = "snmpconfig";
    /**
     * In case of multiple OpenR66 Monitors behing a loadbalancer (ha config)
     */
    private static final String XML_MULTIPLE_MONITORS = "multiplemonitors";
    /**
     * If you need a special Business Factory, you must specify the full class name here.
     * Default is: org.waarp.openr66.context.R66DefaultBusinessFactory which only logs in DEBUG mode.
     */
    private static final String XML_BUSINESS_FACTORY = "businessfactory";
    /**
     * Usage of CPU Limit
     */
    private static final String XML_CSTRT_USECPULIMIT = "usecpulimit";

    /**
     * Usage of JDK CPU Limit (True) or SysMon CPU Limit
     */
    private static final String XML_CSTRT_USECPUJDKLIMIT = "usejdkcpulimit";

    /**
     * CPU LIMIT between 0 and 1, where 1 stands for no limit
     */
    private static final String XML_CSTRT_CPULIMIT = "cpulimit";
    /**
     * Connection limit where 0 stands for no limit
     */
    private static final String XML_CSTRT_CONNLIMIT = "connlimit";
    /**
     * CPU LOW limit to apply increase of throttle
     */
    private static final String XML_CSTRT_LOWCPULIMIT = "lowcpulimit";
    /**
     * CPU HIGH limit to apply decrease of throttle, 0 meaning no throttle activated
     */
    private static final String XML_CSTRT_HIGHCPULIMIT = "highcpulimit";
    /**
     * PERCENTAGE DECREASE of Bandwidth
     */
    private static final String XML_CSTRT_PERCENTDECREASE = "percentdecrease";
    /**
     * Delay between 2 checks of throttle test
     */
    private static final String XML_CSTRT_DELAYTHROTTLE = "delaythrottle";
    /**
     * Bandwidth low limit to not got below
     */
    private static final String XML_CSTRT_LIMITLOWBANDWIDTH = "limitlowbandwidth";
    /**
     * Usage of checking remote address with the DbHost definition
     */
    private static final String XML_CHECK_ADDRESS = "checkaddress";
    /**
     * Usage of checking remote address also for Client
     */
    private static final String XML_CHECK_CLIENTADDRESS = "checkclientaddress";

    /**
     * In case of No Db Client, Usage of saving TaskRunner into independent XML file
     */
    private static final String XML_SAVE_TASKRUNNERNODB = "taskrunnernodb";

    /**
     * Use external Waarp Local Exec for ExecTask and ExecMoveTask
     */
    private static final String XML_USELOCALEXEC = "uselocalexec";

    /**
     * Address of Waarp Local Exec for ExecTask and ExecMoveTask
     */
    private static final String XML_LEXECADDR = "lexecaddr";

    /**
     * Port of Waarp Local Exec for ExecTask and ExecMoveTask
     */
    private static final String XML_LEXECPORT = "lexecport";

    /**
     * Default number of threads in pool for Server.
     */
    private static final String XML_SERVER_THREAD = "serverthread";

    /**
     * Default number of threads in pool for Client (truly concurrent).
     */
    private static final String XML_CLIENT_THREAD = "clientthread";

    /**
     * Memory Limit to use.
     */
    private static final String XML_MEMORY_LIMIT = "memorylimit";

    /**
     * Limit of number of active Runner from Commander
     */
    private static final String XML_LIMITRUNNING = "runlimit";

    /**
     * Delay between two checks for Commander
     */
    private static final String XML_DELAYCOMMANDER = "delaycommand";

    /**
     * Delay between two retry after bad connection
     */
    private static final String XML_DELAYRETRY = "delayretry";

    /**
     * Nb of milliseconds after connection is in timeout
     */
    private static final String XML_TIMEOUTCON = "timeoutcon";

    /**
     * Should a file MD5 SHA1 be computed using NIO
     */
    private static final String XML_USENIO = "usenio";

    /**
     * What Digest to use: CRC32=0, ADLER32=1, MD5=2, MD2=3, SHA1=4, SHA256=5, SHA384=6, SHA512=7
     */
    private static final String XML_DIGEST = "digest";
    /**
     * Should a file MD5 be computed using FastMD5
     */
    private static final String XML_USEFASTMD5 = "usefastmd5";

    /**
     * If using Fast MD5, should we used the binary JNI library, empty meaning no
     */
    private static final String XML_FASTMD5 = "fastmd5";

    /**
     * number of rank to go back when a transfer is restarted. restart is gaprestart*blocksize
     */
    private static final String XML_GAPRESTART = "gaprestart";
    /**
     * Size by default of block size for receive/sending files. Should be a multiple of 8192
     * (maximum = 64K due to block limitation to 2 bytes)
     */
    private static final String XML_BLOCKSIZE = "blocksize";
    /**
     * If set to <=0, will not use Thrift support, if set >0 (preferably > 1024) will enable
     * Thrift support on the TCP port specified by this number
     */
    private static final String XML_USETHRIFT = "usethrift";
    /**
     * Database Driver as of oracle, mysql, postgresql, h2
     */
    private static final String XML_DBDRIVER = "dbdriver";

    /**
     * Database Server connection string as of jdbc:type://[host:port],[failoverhost:port]
     * .../[database][?propertyName1][ =propertyValue1][&propertyName2][=propertyValue2]...
     */
    private static final String XML_DBSERVER = "dbserver";

    /**
     * Database User
     */
    private static final String XML_DBUSER = "dbuser";

    /**
     * Database Password
     */
    private static final String XML_DBPASSWD = "dbpasswd";

    /**
     * Database Checking
     */
    @Deprecated
    private static final String XML_DBCHECK = "dbcheck";

    /**
     * Upgrade database
     */
    private static final String XML_DBAUTOUPGRADE = "autoUpgrade";

    /**
     * Check version in protocol
     */
    private static final String XML_CHECKVERSION = "checkversion";
    /**
     * Global digest by transfer enable
     */
    private static final String XML_GLOBALDIGEST = "globaldigest";

    /**
     * SERVER REST interface SHA address usage (and not all available IPs)
     */
    private static final String XML_REST_ADDRESS = "restaddress";

    /**
     * SERVER HTTP(S) PORT for REST interface
     */
    private static final String XML_SERVER_REST_PORT = "restport";

    /**
     * SERVER REST interface using SSL
     */
    private static final String XML_REST_SSL = "restssl";

    /**
     * SERVER REST interface using time limit
     */
    private static final String XML_REST_TIME_LIMIT = "resttimelimit";

    /**
     * SERVER REST interface using authentication
     */
    private static final String XML_REST_AUTHENTICATED = "restauthenticated";

    /**
     * SERVER REST interface SHA Key for request checking
     */
    private static final String XML_REST_AUTH_KEY = "restsigkey";

    /**
     * SERVER REST interface signature usage (auth key usage)
     */
    private static final String XML_REST_SIGNATURE = "restsignature";

    /**
     * SERVER REST interface method
     */
    private static final String XML_REST_METHOD = "restmethod";
    /**
     * SERVER REST interface method
     */
    private static final String XML_REST_METHOD_NAME = "restname";
    /**
     * SERVER REST interface CRUD per method
     */
    private static final String XML_REST_CRUD = "restcrud";

    /**
     * Structure of the Configuration file
     *
     */
    private static final XmlDecl[] configIdentityDecls = {
            // identity
            new XmlDecl(XmlType.STRING, XML_SERVER_HOSTID),
            new XmlDecl(XmlType.STRING, XML_SERVER_SSLHOSTID),
            new XmlDecl(XmlType.STRING, XML_PATH_CRYPTOKEY),
            new XmlDecl(XmlType.STRING, XML_AUTHENTIFICATION_FILE),
            new XmlDecl(XmlType.STRING, XML_LOCALE)
    };
    /**
     * Structure of the Configuration file
     *
     */
    private static final XmlDecl[] configServerParamDecls = {
            // server
            new XmlDecl(XmlType.BOOLEAN, XML_USESSL),
            new XmlDecl(XmlType.BOOLEAN, XML_USENOSSL),
            new XmlDecl(XmlType.BOOLEAN, XML_USEHTTPCOMP),
            new XmlDecl(XmlType.BOOLEAN, XML_USELOCALEXEC),
            new XmlDecl(XmlType.STRING, XML_LEXECADDR),
            new XmlDecl(XmlType.INTEGER, XML_LEXECPORT),
            new XmlDecl(XmlType.BOOLEAN, XML_CHECK_ADDRESS),
            new XmlDecl(XmlType.BOOLEAN, XML_CHECK_CLIENTADDRESS),
            new XmlDecl(XmlType.STRING, XML_SERVER_ADMIN),
            new XmlDecl(XmlType.STRING, XML_SERVER_PASSWD),
            new XmlDecl(XmlType.STRING, XML_SERVER_PASSWD_FILE),
            new XmlDecl(XmlType.STRING, XML_HTTPADMINPATH),
            new XmlDecl(XmlType.INTEGER, XML_HTTPADMINMODEL),
            new XmlDecl(XmlType.STRING, XML_PATH_ADMIN_KEYPATH),
            new XmlDecl(XmlType.STRING, XML_PATH_ADMIN_KEYSTOREPASS),
            new XmlDecl(XmlType.STRING, XML_PATH_ADMIN_KEYPASS),
            new XmlDecl(XmlType.LONG, XML_MONITOR_PASTLIMIT),
            new XmlDecl(XmlType.LONG, XML_MONITOR_MINIMALDELAY),
            new XmlDecl(XmlType.STRING, XML_MONITOR_SNMP_CONFIG),
            new XmlDecl(XmlType.INTEGER, XML_MULTIPLE_MONITORS),
            new XmlDecl(XmlType.STRING, XML_BUSINESS_FACTORY)
    };
    /**
     * Structure of the Configuration file
     *
     */
    private static final XmlDecl[] configNetworkServerDecls = {
            // network
            new XmlDecl(XmlType.INTEGER, XML_SERVER_PORT),
            new XmlDecl(XmlType.INTEGER, XML_SERVER_SSLPORT),
            new XmlDecl(XmlType.INTEGER, XML_SERVER_HTTPPORT),
            new XmlDecl(XmlType.INTEGER, XML_SERVER_HTTPSPORT)
    };

    /**
     * Structure of the Configuration file
     *
     */
    private static final XmlDecl[] configSslDecls = {
            // ssl
            new XmlDecl(XmlType.STRING, XML_PATH_KEYPATH),
            new XmlDecl(XmlType.STRING, XML_PATH_KEYSTOREPASS),
            new XmlDecl(XmlType.STRING, XML_PATH_KEYPASS),
            new XmlDecl(XmlType.STRING, XML_PATH_TRUSTKEYPATH),
            new XmlDecl(XmlType.STRING, XML_PATH_TRUSTKEYSTOREPASS),
            new XmlDecl(XmlType.BOOLEAN, XML_USECLIENT_AUTHENT)
    };
    /**
     * Structure of the Configuration file
     *
     */
    private static final XmlDecl[] configDbDecls = {
            // db
            new XmlDecl(XmlType.STRING, XML_DBDRIVER),
            new XmlDecl(XmlType.STRING, XML_DBSERVER),
            new XmlDecl(XmlType.STRING, XML_DBUSER),
            new XmlDecl(XmlType.STRING, XML_DBPASSWD),
            new XmlDecl(XmlType.BOOLEAN, XML_DBCHECK),
            new XmlDecl(XmlType.BOOLEAN, XML_DBAUTOUPGRADE),
            new XmlDecl(XmlType.BOOLEAN, XML_SAVE_TASKRUNNERNODB)
    };

    /**
     * Structure of the Configuration file
     *
     */
    private static final XmlDecl[] configLimitDecls = {
            // limit
            new XmlDecl(XmlType.LONG, XML_LIMITSESSION),
            new XmlDecl(XmlType.LONG, XML_LIMITGLOBAL),
            new XmlDecl(XmlType.LONG, XML_LIMITDELAY),
            new XmlDecl(XmlType.INTEGER, XML_LIMITRUNNING),
            new XmlDecl(XmlType.LONG, XML_DELAYCOMMANDER),
            new XmlDecl(XmlType.LONG, XML_DELAYRETRY),
            new XmlDecl(XmlType.INTEGER, XML_SERVER_THREAD),
            new XmlDecl(XmlType.INTEGER, XML_CLIENT_THREAD),
            new XmlDecl(XmlType.LONG, XML_MEMORY_LIMIT),
            new XmlDecl(XmlType.BOOLEAN, XML_CSTRT_USECPULIMIT),
            new XmlDecl(XmlType.BOOLEAN, XML_CSTRT_USECPUJDKLIMIT),
            new XmlDecl(XmlType.DOUBLE, XML_CSTRT_CPULIMIT),
            new XmlDecl(XmlType.INTEGER, XML_CSTRT_CONNLIMIT),
            new XmlDecl(XmlType.DOUBLE, XML_CSTRT_LOWCPULIMIT),
            new XmlDecl(XmlType.DOUBLE, XML_CSTRT_HIGHCPULIMIT),
            new XmlDecl(XmlType.DOUBLE, XML_CSTRT_PERCENTDECREASE),
            new XmlDecl(XmlType.LONG, XML_CSTRT_LIMITLOWBANDWIDTH),
            new XmlDecl(XmlType.LONG, XML_CSTRT_DELAYTHROTTLE),
            new XmlDecl(XmlType.LONG, XML_TIMEOUTCON),
            new XmlDecl(XmlType.BOOLEAN, XML_USENIO),
            new XmlDecl(XmlType.INTEGER, XML_DIGEST),
            new XmlDecl(XmlType.BOOLEAN, XML_USEFASTMD5),
            new XmlDecl(XmlType.STRING, XML_FASTMD5),
            new XmlDecl(XmlType.INTEGER, XML_GAPRESTART),
            new XmlDecl(XmlType.INTEGER, XML_BLOCKSIZE),
            new XmlDecl(XmlType.INTEGER, XML_USETHRIFT),
            new XmlDecl(XmlType.BOOLEAN, XML_CHECKVERSION),
            new XmlDecl(XmlType.BOOLEAN, XML_GLOBALDIGEST)
    };
    /**
     * Structure of the Configuration file
     *
     */
    private static final XmlDecl[] configSubmitLimitDecls = {
            // limit
            new XmlDecl(XmlType.INTEGER, XML_BLOCKSIZE)
    };
    /**
     * Structure of the Configuration file
     *
     */
    private static final XmlDecl[] configClientParamDecls = {
            // client
            new XmlDecl(XmlType.BOOLEAN, XML_SAVE_TASKRUNNERNODB),
            new XmlDecl(XmlType.STRING, XML_BUSINESS_FACTORY)
    };
    /**
     * Structure of the Configuration file
     *
     */
    private static final XmlDecl[] configDirectoryDecls = {
            // directory
            new XmlDecl(XmlType.STRING, XML_SERVER_HOME),
            new XmlDecl(XmlType.STRING, XML_INPATH),
            new XmlDecl(XmlType.STRING, XML_OUTPATH),
            new XmlDecl(XmlType.STRING, XML_ARCHIVEPATH),
            new XmlDecl(XmlType.STRING, XML_WORKINGPATH),
            new XmlDecl(XmlType.STRING, XML_CONFIGPATH)
    };

    public static final XmlDecl[] configRestMethodDecls = {
            // Rest Method
            new XmlDecl(XmlType.STRING, XML_REST_METHOD_NAME),
            new XmlDecl(XmlType.STRING, XML_REST_CRUD)
    };
    /**
     * Overall structure of the Configuration file
     */
    private static final String XML_ROOT = "/config/";
    private static final String XML_IDENTITY = "identity";
    private static final String XML_SERVER = "server";
    private static final String XML_CLIENT = "client";
    private static final String XML_DIRECTORY = "directory";
    private static final String XML_LIMIT = "limit";
    private static final String XML_NETWORK = "network";
    private static final String XML_SSL = "ssl";
    private static final String XML_DB = "db";
    private static final String XML_REST = "rest";
    /**
     * Structure of the Configuration file
     *
     */
    private static final XmlDecl[] configRestDecls = {
            // Rest support configuration
            new XmlDecl(XmlType.STRING, XML_REST_ADDRESS),
            new XmlDecl(XmlType.INTEGER, XML_SERVER_REST_PORT),
            new XmlDecl(XmlType.BOOLEAN, XML_REST_SSL),
            new XmlDecl(XmlType.BOOLEAN, XML_REST_AUTHENTICATED),
            new XmlDecl(XmlType.LONG, XML_REST_TIME_LIMIT),
            new XmlDecl(XmlType.BOOLEAN, XML_REST_SIGNATURE),
            new XmlDecl(XmlType.STRING, XML_REST_AUTH_KEY),
            new XmlDecl(XML_REST_METHOD, XmlType.XVAL, XML_REST_METHOD, configRestMethodDecls, true)
    };

    /**
     * Global Structure for Server Configuration
     */
    private static final XmlDecl[] configServer = {
            new XmlDecl(XML_IDENTITY, XmlType.XVAL, XML_ROOT + XML_IDENTITY, configIdentityDecls,
                    false),
            new XmlDecl(XML_SERVER, XmlType.XVAL, XML_ROOT + XML_SERVER, configServerParamDecls,
                    false),
            new XmlDecl(XML_NETWORK, XmlType.XVAL, XML_ROOT + XML_NETWORK,
                    configNetworkServerDecls, false),
            new XmlDecl(XML_SSL, XmlType.XVAL, XML_ROOT + XML_SSL, configSslDecls, false),
            new XmlDecl(XML_DIRECTORY, XmlType.XVAL, XML_ROOT + XML_DIRECTORY,
                    configDirectoryDecls, false),
            new XmlDecl(XML_LIMIT, XmlType.XVAL, XML_ROOT + XML_LIMIT, configLimitDecls, false),
            new XmlDecl(XML_REST, XmlType.XVAL, XML_ROOT + XML_REST, configRestDecls, true),
            new XmlDecl(XML_DB, XmlType.XVAL, XML_ROOT + XML_DB, configDbDecls, false),
            new XmlDecl(DbHostConfiguration.XML_BUSINESS, XmlType.STRING, XML_ROOT + DbHostConfiguration.XML_BUSINESS
                    + "/"
                    + DbHostConfiguration.XML_BUSINESSID, true),
            new XmlDecl(DbHostConfiguration.XML_ROLES, XmlType.XVAL, XML_ROOT + DbHostConfiguration.XML_ROLES + "/"
                    + DbHostConfiguration.XML_ROLE, DbHostConfiguration.configRoleDecls, true),
            new XmlDecl(DbHostConfiguration.XML_ALIASES, XmlType.XVAL, XML_ROOT + DbHostConfiguration.XML_ALIASES + "/"
                    + DbHostConfiguration.XML_ALIAS, DbHostConfiguration.configAliasDecls, true)
    };
    /**
     * Global Structure for Client Configuration
     */
    private static final XmlDecl[] configClient = {
            new XmlDecl(XML_IDENTITY, XmlType.XVAL, XML_ROOT + XML_IDENTITY, configIdentityDecls,
                    false),
            new XmlDecl(XML_CLIENT, XmlType.XVAL, XML_ROOT + XML_CLIENT, configClientParamDecls,
                    false),
            new XmlDecl(XML_SSL, XmlType.XVAL, XML_ROOT + XML_SSL, configSslDecls, false),
            new XmlDecl(XML_DIRECTORY, XmlType.XVAL, XML_ROOT + XML_DIRECTORY,
                    configDirectoryDecls, false),
            new XmlDecl(XML_LIMIT, XmlType.XVAL, XML_ROOT + XML_LIMIT, configLimitDecls, false),
            new XmlDecl(XML_REST, XmlType.XVAL, XML_ROOT + XML_REST, configRestDecls, false),
            new XmlDecl(XML_DB, XmlType.XVAL, XML_ROOT + XML_DB, configDbDecls, false),
            new XmlDecl(DbHostConfiguration.XML_BUSINESS, XmlType.STRING, XML_ROOT + DbHostConfiguration.XML_BUSINESS
                    + "/"
                    + DbHostConfiguration.XML_BUSINESSID, true),
            new XmlDecl(DbHostConfiguration.XML_ALIASES, XmlType.XVAL, XML_ROOT + DbHostConfiguration.XML_ALIASES + "/"
                    + DbHostConfiguration.XML_ALIAS, DbHostConfiguration.configAliasDecls, true)
    };
    /**
     * Global Structure for Submit only Client Configuration
     */
    private static final XmlDecl[] configSubmitClient = {
            new XmlDecl(XML_IDENTITY, XmlType.XVAL, XML_ROOT + XML_IDENTITY, configIdentityDecls,
                    false),
            new XmlDecl(XML_DIRECTORY, XmlType.XVAL, XML_ROOT + XML_DIRECTORY,
                    configDirectoryDecls, false),
            new XmlDecl(XML_LIMIT, XmlType.XVAL, XML_ROOT + XML_LIMIT, configSubmitLimitDecls,
                    false),
            new XmlDecl(XML_DB, XmlType.XVAL, XML_ROOT + XML_DB, configDbDecls, false),
            new XmlDecl(DbHostConfiguration.XML_ALIASES, XmlType.XVAL, XML_ROOT + DbHostConfiguration.XML_ALIASES + "/"
                    + DbHostConfiguration.XML_ALIAS, DbHostConfiguration.configAliasDecls, true)
    };
    private static XmlValue[] configuration = null;
    private static XmlRootHash hashRootConfig = null;

    /**
     * Load the locale from configuration file
     *
     * @param hashConfig
     */
    private static void loadLocale(XmlHash hashConfig) {
        XmlValue value = hashConfig.get(XML_LOCALE);
        if (value != null && (!value.isEmpty())) {
            String locale = value.getString();
            if (locale == null || locale.isEmpty()) {
                return;
            }
            Messages.init(new Locale(locale));
        }
    }

    /**
     *
     * @param config
     * @return True if the identity of the server is correctly loaded
     */
    private static boolean loadIdentity(Configuration config) {
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_IDENTITY));
        try {
            loadLocale(hashConfig);
            XmlValue value = hashConfig.get(XML_SERVER_HOSTID);
            if (value != null && (!value.isEmpty())) {
                config.setHOST_ID(value.getString());
            } else {
                logger.error(Messages.getString("FileBasedConfiguration.NotFoundConfig") + "Host ID"); //$NON-NLS-1$
                return false;
            }
            value = hashConfig.get(XML_SERVER_SSLHOSTID);
            if (value != null && (!value.isEmpty())) {
                config.setHOST_SSLID(value.getString());
            } else {
                logger.warn(Messages.getString("FileBasedConfiguration.SSLIDNotFound")); //$NON-NLS-1$
                config.setUseSSL(false);
                config.setHOST_SSLID(null);
            }
            value = hashConfig.get(XML_AUTHENTIFICATION_FILE);
            if (value != null && (!value.isEmpty())) {
                config.setAUTH_FILE(value.getString());
            }
            return setCryptoKey(config, hashConfig);
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
    }

    /**
     *
     * @param config
     * @return True if the authentication of partners is correctly loaded
     */
    private static boolean loadAuthentication(Configuration config) {
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_IDENTITY));
        try {
            if (config.isSaveTaskRunnerWithNoDb()) {
                // if no database, must load authentication from file
                XmlValue value = hashConfig.get(XML_AUTHENTIFICATION_FILE);
                if (value != null && (!value.isEmpty())) {
                    String fileauthent = value.getString();
                    if (!AuthenticationFileBasedConfiguration
                            .loadAuthentication(config, fileauthent)) {
                        return false;
                    }
                } else {
                    logger.warn(Messages.getString("FileBasedConfiguration.NotFoundConfig") + "Authentication file"); //$NON-NLS-1$
                    return false;
                }
            }
            return true;
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
    }

    /**
     *
     * @param config
     * @return True if the server parameters are correctly loaded
     */
    private static boolean loadServerParam(Configuration config) {
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_SERVER));
        try {
            XmlValue value = hashConfig.get(XML_USESSL);
            if (value != null && (!value.isEmpty())) {
                config.setUseSSL(value.getBoolean());
            }
            value = hashConfig.get(XML_USENOSSL);
            if (value != null && (!value.isEmpty())) {
                config.setUseNOSSL(value.getBoolean());
            }
            value = hashConfig.get(XML_USEHTTPCOMP);
            if (value != null && (!value.isEmpty())) {
                config.setUseHttpCompression(value.getBoolean());
            }
            value = hashConfig.get(XML_USELOCALEXEC);
            if (value != null && (!value.isEmpty())) {
                config.setUseLocalExec(value.getBoolean());
                if (config.isUseLocalExec()) {
                    value = hashConfig.get(XML_LEXECADDR);
                    String saddr;
                    InetAddress addr;
                    if (value != null && (!value.isEmpty())) {
                        saddr = value.getString();
                        try {
                            addr = InetAddress.getByName(saddr);
                        } catch (UnknownHostException e) {
                            logger.error(Messages.getString("FileBasedConfiguration.NotFoundConfig") + "LocalExec Address"); //$NON-NLS-1$
                            return false;
                        }
                    } else {
                        logger.warn(Messages.getString("FileBasedConfiguration.NotFoundConfig") + "LocalExec Address"); //$NON-NLS-1$
                        try {
                            addr = InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
                        } catch (UnknownHostException e) {
                            logger.error(Messages.getString("FileBasedConfiguration.NotFoundConfig") + "LocalExec Address"); //$NON-NLS-1$
                            return false;
                        }
                    }
                    value = hashConfig.get(XML_LEXECPORT);
                    int port;
                    if (value != null && (!value.isEmpty())) {
                        port = value.getInteger();
                    } else {
                        port = 9999;
                    }
                    LocalExecClient.address = new InetSocketAddress(addr, port);
                }
            }
            value = hashConfig.get(XML_CHECK_ADDRESS);
            if (value != null && (!value.isEmpty())) {
                config.setCheckRemoteAddress(value.getBoolean());
            }
            value = hashConfig.get(XML_CHECK_CLIENTADDRESS);
            if (value != null && (!value.isEmpty())) {
                config.setCheckClientAddress(value.getBoolean());
            }
            value = hashConfig.get(XML_SERVER_ADMIN);
            if (value != null && (!value.isEmpty())) {
                config.setADMINNAME(value.getString());
            } else {
                logger.error(Messages.getString("FileBasedConfiguration.NotFoundConfig") + "Administrator name"); //$NON-NLS-1$
                return false;
            }
            if (config.getCryptoKey() == null) {
                XmlHash hashConfig2 = new XmlHash(hashRootConfig.get(XML_IDENTITY));
                try {
                    if (!setCryptoKey(config, hashConfig2)) {
                        logger.error(Messages.getString("FileBasedConfiguration.NotFoundConfig") + "Crypto Key"); //$NON-NLS-1$
                        return false;
                    }
                } finally {
                    hashConfig2.clear();
                    hashConfig2 = null;
                }
            }
            byte[] decodedByteKeys = null;
            value = hashConfig.get(XML_SERVER_PASSWD_FILE);
            if (value == null || (value.isEmpty())) {
                String passwd;
                value = hashConfig.get(XML_SERVER_PASSWD);
                if (value != null && (!value.isEmpty())) {
                    passwd = value.getString();
                } else {
                    logger.error(Messages.getString("FileBasedConfiguration.NotFoundConfig") + "Password"); //$NON-NLS-1$
                    return false;
                }
                try {
                    decodedByteKeys =
                            config.getCryptoKey().decryptHexInBytes(passwd);
                } catch (Exception e) {
                    logger.error(
                            "Unable to Decrypt Server Password in Config file from: " +
                                    passwd, e);
                    return false;
                }
            } else {
                String skey = value.getString();
                // load key from file
                config.setServerKeyFile(skey);
                File key = new File(skey);
                if (!key.canRead()) {
                    logger.error("Unable to read Password in Config file from " + skey);
                    return false;
                }
                try {
                    decodedByteKeys = config.getCryptoKey().decryptHexFile(key);
                } catch (Exception e2) {
                    logger.error(
                            "Unable to Decrypt Server Password in Config file from: " +
                                    skey, e2);
                    return false;
                }
            }
            config.setSERVERKEY(decodedByteKeys);
            value = hashConfig.get(XML_HTTPADMINPATH);
            if (value == null || (value.isEmpty())) {
                logger.error(Messages.getString("FileBasedConfiguration.NotFoundConfig") + "Http Admin Base"); //$NON-NLS-1$
                return false;
            }
            String path = value.getString();
            if (path == null || path.isEmpty()) {
                logger.error(Messages.getString("FileBasedConfiguration.NoSetConfig") + "Http Admin Base"); //$NON-NLS-1$
                return false;
            }
            File file = new File(path);
            if (!file.isDirectory()) {
                logger.error(Messages.getString("FileBasedConfiguration.NotDirectory") + "Http Admin Base"); //$NON-NLS-1$
                return false;
            }
            try {
                config.setHttpBasePath(FilesystemBasedDirImpl.normalizePath(file.getCanonicalPath()) +
                        DirInterface.SEPARATOR);
            } catch (IOException e1) {
                logger.error(Messages.getString("FileBasedConfiguration.NoSetConfig") + "Http Admin Path"); //$NON-NLS-1$
                return false;
            }
            value = hashConfig.get(XML_HTTPADMINMODEL);
            // 0 = standard, 1 = responsive (preferred default)
            int model = (! new File(file, HttpResponsiveSslHandler.LISTING_PAGE).isFile()) ? 0 : 1;
            if (value != null && (!value.isEmpty())) {
                model = value.getInteger();
            }
            config.setHttpModel(model);

            // Key for HTTPS
            value = hashConfig.get(XML_PATH_ADMIN_KEYPATH);
            if (value != null && (!value.isEmpty())) {
                String keypath = value.getString();
                if ((keypath == null) || (keypath.isEmpty())) {
                    logger.error("Bad Key Path");
                    return false;
                }
                value = hashConfig.get(XML_PATH_ADMIN_KEYSTOREPASS);
                if (value == null || (value.isEmpty())) {
                    logger.error("Unable to find: " + "KeyStore Passwd");
                    return false;
                }
                String keystorepass = value.getString();
                if ((keystorepass == null) || (keystorepass.isEmpty())) {
                    logger.error("Bad KeyStore Passwd");
                    return false;
                }
                value = hashConfig.get(XML_PATH_ADMIN_KEYPASS);
                if (value == null || (value.isEmpty())) {
                    logger.error("Unable to find :" + "Key Passwd");
                    return false;
                }
                String keypass = value.getString();
                if ((keypass == null) || (keypass.isEmpty())) {
                    logger.error("Bad Key Passwd");
                    return false;
                }
                try {
                    Configuration.setWaarpSecureKeyStore(new WaarpSecureKeyStore(keypath, keystorepass,
                            keypass));
                } catch (CryptoException e) {
                    logger.error("Bad SecureKeyStore construction for AdminSsl");
                    return false;
                }
                // No client authentication
                Configuration.getWaarpSecureKeyStore().initEmptyTrustStore();
                Configuration.setWaarpSslContextFactory(new WaarpSslContextFactory(
                        Configuration.getWaarpSecureKeyStore(), true));
            }
            value = hashConfig.get(XML_MONITOR_PASTLIMIT);
            if (value != null && (!value.isEmpty())) {
                config.setPastLimit((value.getLong() / 10) * 10);
            }
            value = hashConfig.get(XML_MONITOR_MINIMALDELAY);
            if (value != null && (!value.isEmpty())) {
                config.setMinimalDelay((value.getLong() / 10) * 10);
            }
            value = hashConfig.get(XML_MONITOR_SNMP_CONFIG);
            if (value != null && (!value.isEmpty())) {
                config.setSnmpConfig(value.getString());
                File snmpfile = new File(config.getSnmpConfig());
                if (snmpfile.canRead()) {
                    if (!SnmpConfiguration.setConfigurationFromXml(snmpfile)) {
                        config.setSnmpConfig(null);
                    }
                } else {
                    config.setSnmpConfig(null);
                }
            }
            value = hashConfig.get(XML_MULTIPLE_MONITORS);
            if (value != null && (!value.isEmpty())) {
                config.setMultipleMonitors(value.getInteger());
                if (config.getMultipleMonitors() > 1) {
                    logger.warn(Messages.getString("FileBasedConfiguration.MMOn") //$NON-NLS-1$
                            + config.getMultipleMonitors()
                            + Messages.getString("FileBasedConfiguration.MMOn2")); //$NON-NLS-1$
                } else {
                    config.setMultipleMonitors(1);
                    if (config.isWarnOnStartup()) {
                        logger.warn(Messages.getString("FileBasedConfiguration.MMOff")); //$NON-NLS-1$
                    } else {
                        logger.info(Messages.getString("FileBasedConfiguration.MMOff")); //$NON-NLS-1$
                    }
                }
            } else {
                config.setMultipleMonitors(1);
                if (config.isWarnOnStartup()) {
                    logger.warn(Messages.getString("FileBasedConfiguration.MMOff")); //$NON-NLS-1$
                } else {
                    logger.info(Messages.getString("FileBasedConfiguration.MMOff")); //$NON-NLS-1$
                }
            }
            value = hashConfig.get(XML_BUSINESS_FACTORY);
            if (value != null && (!value.isEmpty())) {
                try {
                    config.setR66BusinessFactory((R66BusinessFactoryInterface) Class.forName(value.getString()).newInstance());
                } catch (Exception e) {
                    logger.error("Bad Business Factory class", e);
                    return false;
                }
            }
            return true;
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
    }

    /**
     *
     * @param config
     * @return True if the client parameters are correctly loaded
     */
    private static boolean loadClientParam(Configuration config) {
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_CLIENT));
        try {
            XmlValue value = hashConfig.get(XML_SAVE_TASKRUNNERNODB);
            if (DbConstant.admin == null || DbConstant.admin.getTypeDriver() == DbType.none) {
                if (value != null && (!value.isEmpty())) {
                    config.setSaveTaskRunnerWithNoDb(value.getBoolean());
                    logger.info(Messages.getString("FileBasedConfiguration.NoDB")); //$NON-NLS-1$
                    if (DbConstant.admin == null) {
                        DbConstant.admin = new DbAdmin(); // no database support
                        DbConstant.noCommitAdmin = DbConstant.admin;
                    }
                }
            }
            value = hashConfig.get(XML_BUSINESS_FACTORY);
            if (value != null && (!value.isEmpty())) {
                try {
                    config.setR66BusinessFactory((R66BusinessFactoryInterface) Class.forName(value.getString()).newInstance());
                } catch (Exception e) {
                    logger.error("Bad Business Factory class", e);
                    return false;
                }
            }
            return true;
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
    }

    /**
     *
     * @param config
     * @return True if the directory parameters are correctly loaded
     */
    private static boolean loadDirectory(Configuration config) {
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_DIRECTORY));
        try {
            XmlValue value = hashConfig.get(XML_SERVER_HOME);
            if (value == null || (value.isEmpty())) {
                logger.error(Messages.getString("FileBasedConfiguration.NotFoundConfig") + "Home"); //$NON-NLS-1$
                return false;
            }
            String path = value.getString();
            File file = new File(path);
            if (!file.isDirectory()) {
                logger.error(Messages.getString("FileBasedConfiguration.NotDirectory") + "Home"); //$NON-NLS-1$
                return false;
            }
            try {
                config.setBaseDirectory(FilesystemBasedDirImpl
                        .normalizePath(file.getCanonicalPath()));
            } catch (IOException e1) {
                logger.error(Messages.getString("FileBasedConfiguration.NoSetConfig") + "Home"); //$NON-NLS-1$
                return false;
            }
            try {
                config.setConfigPath(FilesystemBasedDirImpl
                        .normalizePath(getSubPath(config, XML_CONFIGPATH)));
            } catch (OpenR66ProtocolSystemException e2) {
                logger.error(Messages.getString("FileBasedConfiguration.NoSetConfig") + "Config"); //$NON-NLS-1$
                return false;
            }
            try {
                config.setInPath(FilesystemBasedDirImpl
                        .normalizePath(getSubPath(config, XML_INPATH)));
            } catch (OpenR66ProtocolSystemException e2) {
                logger.error(Messages.getString("FileBasedConfiguration.NoSetConfig") + "In"); //$NON-NLS-1$
                return false;
            }
            try {
                config.setOutPath(FilesystemBasedDirImpl
                        .normalizePath(getSubPath(config, XML_OUTPATH)));
            } catch (OpenR66ProtocolSystemException e2) {
                logger.error(Messages.getString("FileBasedConfiguration.NoSetConfig") + "Out"); //$NON-NLS-1$
                return false;
            }
            try {
                config.setWorkingPath(FilesystemBasedDirImpl
                        .normalizePath(getSubPath(config, XML_WORKINGPATH)));
            } catch (OpenR66ProtocolSystemException e2) {
                logger.error(Messages.getString("FileBasedConfiguration.NoSetConfig") + "Working"); //$NON-NLS-1$
                return false;
            }
            try {
                config.setArchivePath(FilesystemBasedDirImpl
                        .normalizePath(getSubPath(config, XML_ARCHIVEPATH)));
            } catch (OpenR66ProtocolSystemException e2) {
                logger.error(Messages.getString("FileBasedConfiguration.NoSetConfig") + "Archive"); //$NON-NLS-1$
                return false;
            }
            return true;
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
    }

    private static boolean alreadySetLimit = false;

    /**
     *
     * @param config
     * @param updateLimit
     * @return True if the limit configuration is correctly loaded
     */
    private static boolean loadLimit(Configuration config, boolean updateLimit) {
        if (alreadySetLimit) {
            return true;
        }
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_LIMIT));
        try {
            XmlValue value = hashConfig.get(XML_LIMITGLOBAL);
            if (value != null && (!value.isEmpty())) {
                config.setServerGlobalReadLimit(value.getLong());
                if (config.getServerGlobalReadLimit() <= 0) {
                    config.setServerGlobalReadLimit(0);
                }
                config.setServerGlobalWriteLimit(config.getServerGlobalReadLimit());
                logger.info("Global Limit: {}",
                        config.getServerGlobalReadLimit());
            }
            value = hashConfig.get(XML_LIMITSESSION);
            if (value != null && (!value.isEmpty())) {
                config.setServerChannelReadLimit(value.getLong());
                if (config.getServerChannelReadLimit() <= 0) {
                    config.setServerChannelReadLimit(0);
                }
                config.setServerChannelWriteLimit(config.getServerChannelReadLimit());
                logger.info("SessionInterface Limit: {}",
                        config.getServerChannelReadLimit());
            }
            config.setAnyBandwidthLimitation((config.getServerGlobalReadLimit() > 0 || config.getServerGlobalWriteLimit() > 0 ||
                    config.getServerChannelReadLimit() > 0 || config.getServerChannelWriteLimit() > 0));
            config.setDelayLimit(AbstractTrafficShapingHandler.DEFAULT_CHECK_INTERVAL);
            value = hashConfig.get(XML_LIMITDELAY);
            if (value != null && (!value.isEmpty())) {
                config.setDelayLimit((value.getLong() / 10) * 10);
                if (config.getDelayLimit() <= 0) {
                    config.setDelayLimit(0);
                }
                logger.info("Delay Limit: {}",
                        config.getDelayLimit());
            }
            value = hashConfig.get(XML_LIMITRUNNING);
            if (value != null && (!value.isEmpty())) {
                config.setRUNNER_THREAD(value.getInteger());
            }
            logger.info("Limit of Runner: {}",
                    config.getRUNNER_THREAD());
            value = hashConfig.get(XML_DELAYCOMMANDER);
            if (value != null && (!value.isEmpty())) {
                config.setDelayCommander((value.getLong() / 10) * 10);
                if (config.getDelayCommander() <= 100) {
                    config.setDelayCommander(100);
                }
                logger.info("Delay Commander: {}",
                        config.getDelayCommander());
            }
            value = hashConfig.get(XML_DELAYRETRY);
            if (value != null && (!value.isEmpty())) {
                config.setDelayRetry((value.getLong() / 10) * 10);
                if (config.getDelayRetry() <= 1000) {
                    config.setDelayRetry(1000);
                }
                logger.info("Delay Retry: {}",
                        config.getDelayRetry());
            }
            if (DbConstant.admin.isActive() && updateLimit) {
                value = hashConfig.get(XML_SERVER_HOSTID);
                if (value != null && (!value.isEmpty())) {
                    config.setHOST_ID(value.getString());
                    DbConfiguration configuration = new DbConfiguration(
                            config.getHOST_ID(),
                            config.getServerGlobalReadLimit(),
                            config.getServerGlobalWriteLimit(),
                            config.getServerChannelReadLimit(),
                            config.getServerChannelWriteLimit(),
                            config.getDelayLimit());
                    configuration.changeUpdatedInfo(UpdatedInfo.TOSUBMIT);
                    try {
                        if (configuration.exist()) {
                            configuration.update();
                        } else {
                            configuration.insert();
                        }
                    } catch (WaarpDatabaseException e) {
                    }
                }
            }
            boolean useCpuLimit = false;
            boolean useCpuLimitJDK = false;
            double cpulimit = 1.0;
            value = hashConfig.get(XML_CSTRT_USECPULIMIT);
            if (value != null && (!value.isEmpty())) {
                useCpuLimit = value.getBoolean();
                value = hashConfig.get(XML_CSTRT_USECPUJDKLIMIT);
                if (value != null && (!value.isEmpty())) {
                    useCpuLimitJDK = value.getBoolean();
                }
                value = hashConfig.get(XML_CSTRT_CPULIMIT);
                if (value != null && (!value.isEmpty())) {
                    cpulimit = value.getDouble();
                    if (cpulimit > 0.99) {
                        cpulimit = 1.0;
                    }
                }
            }
            int connlimit = 0;
            value = hashConfig.get(XML_CSTRT_CONNLIMIT);
            if (value != null && (!value.isEmpty())) {
                connlimit = value.getInteger();
                if (connlimit < 100) {
                    connlimit = 0;
                }
            }
            double lowcpuLimit = 0.0;
            double highcpuLimit = 0.0;
            double percentageDecrease = 0;
            long delay = 1000000;
            long limitLowBandwidth = R66ConstraintLimitHandler.LOWBANDWIDTH_DEFAULT;
            value = hashConfig.get(XML_CSTRT_LOWCPULIMIT);
            if (value != null && (!value.isEmpty())) {
                lowcpuLimit = value.getDouble();
            }
            value = hashConfig.get(XML_CSTRT_HIGHCPULIMIT);
            if (value != null && (!value.isEmpty())) {
                highcpuLimit = value.getDouble();
                if (highcpuLimit < 0.1) {
                    highcpuLimit = 0.0;
                }
            }
            value = hashConfig.get(XML_CSTRT_PERCENTDECREASE);
            if (value != null && (!value.isEmpty())) {
                percentageDecrease = value.getDouble();
            }
            value = hashConfig.get(XML_CSTRT_DELAYTHROTTLE);
            if (value != null && (!value.isEmpty())) {
                delay = (value.getLong() / 10) * 10;
                if (delay < 100) {
                    delay = 100;
                }
            }
            value = hashConfig.get(XML_CSTRT_LIMITLOWBANDWIDTH);
            if (value != null && (!value.isEmpty())) {
                limitLowBandwidth = value.getLong();
            }
            if (useCpuLimit || highcpuLimit > 0) {
                if (highcpuLimit > 0) {
                    logger.debug("full setup of ContraintLimitHandler");
                    config.setConstraintLimitHandler(new R66ConstraintLimitHandler(useCpuLimit, useCpuLimitJDK, cpulimit, connlimit,
                            lowcpuLimit, highcpuLimit, percentageDecrease, null, delay,
                            limitLowBandwidth));
                } else {
                    logger.debug("partial setup of ContraintLimitHandler");
                    config.setConstraintLimitHandler(new R66ConstraintLimitHandler(useCpuLimit, useCpuLimitJDK, cpulimit, connlimit));
                }
            } else {
                logger.debug("No setup of ContraintLimitHandler");
                config.setConstraintLimitHandler(new R66ConstraintLimitHandler(false, false, 1.0, connlimit));
            }
            value = hashConfig.get(XML_SERVER_THREAD);
            if (value != null && (!value.isEmpty())) {
                config.setSERVER_THREAD(value.getInteger());
            }
            value = hashConfig.get(XML_CLIENT_THREAD);
            if (value != null && (!value.isEmpty())) {
                config.setCLIENT_THREAD(value.getInteger());
            }
            value = hashConfig.get(XML_MEMORY_LIMIT);
            if (value != null && (!value.isEmpty())) {
                config.setMaxGlobalMemory(value.getLong());
            }
            Configuration.getFileParameter().deleteOnAbort = false;
            value = hashConfig.get(XML_USENIO);
            if (value != null && (!value.isEmpty())) {
                FilesystemBasedFileParameterImpl.useNio = value.getBoolean();
            }
            value = hashConfig.get(XML_DIGEST);
            if (value != null && (!value.isEmpty())) {
                try {
                    int val = value.getInteger();
                    if (val < 0 || val >= DigestAlgo.values().length) {
                        val = 0;
                    }
                    config.setDigest(DigestAlgo.values()[val]);
                } catch (IllegalArgumentException e) {
                    // might be String
                    String val = value.getString();
                    config.setDigest(PartnerConfiguration.getDigestAlgo(val));
                }
            }
            logger.info("DigestAlgo used: {}", config.getDigest());
            value = hashConfig.get(XML_USEFASTMD5);
            if (value != null && (!value.isEmpty())) {
                FilesystemBasedDigest.setUseFastMd5(value.getBoolean());
            } else {
                FilesystemBasedDigest.setUseFastMd5(false);
            }
            value = hashConfig.get(XML_GAPRESTART);
            if (value != null && (!value.isEmpty())) {
                Configuration.setRANKRESTART(value.getInteger());
                if (Configuration.getRANKRESTART() <= 0) {
                    Configuration.setRANKRESTART(1);
                }
            }
            value = hashConfig.get(XML_BLOCKSIZE);
            if (value != null && (!value.isEmpty())) {
                config.setBLOCKSIZE(value.getInteger());
            }
            value = hashConfig.get(XML_USETHRIFT);
            if (value != null && (!value.isEmpty())) {
                config.setThriftport(value.getInteger());
            }
            value = hashConfig.get(XML_TIMEOUTCON);
            if (value != null && (!value.isEmpty())) {
                config.setTIMEOUTCON((value.getLong() / 10) * 10);
                config.getShutdownConfiguration().timeout = config.getTIMEOUTCON();
            }
            value = hashConfig.get(XML_CHECKVERSION);
            if (value != null && (!value.isEmpty())) {
                config.setExtendedProtocol(value.getBoolean());
                logger.info("ExtendedProtocol= " + config.isExtendedProtocol());
            }
            value = hashConfig.get(XML_GLOBALDIGEST);
            if (value != null && (!value.isEmpty())) {
                config.setGlobalDigest(value.getBoolean());
            }
            alreadySetLimit = true;
            return true;
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
    }

    /**
     *
     * @param config
     * @return True if the SSL configuration is correctly loaded
     */
    private static boolean loadSsl(Configuration config) {
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_SSL));
        try {
            // StoreKey for Server
            XmlValue value = hashConfig.get(XML_PATH_KEYPATH);
            if (value == null || (value.isEmpty())) {
                logger.info("Unable to find Key Path");
                try {
                    NetworkSslServerInitializer.setWaarpSecureKeyStore(new WaarpSecureKeyStore("secret", "secret"));
                } catch (CryptoException e) {
                    logger.error("Bad SecureKeyStore construction");
                    return false;
                }
            } else {
                String keypath = value.getString();
                if ((keypath == null) || (keypath.isEmpty())) {
                    logger.error("Bad Key Path");
                    return false;
                }
                value = hashConfig.get(XML_PATH_KEYSTOREPASS);
                if (value == null || (value.isEmpty())) {
                    logger.error("Unable to find KeyStore Passwd");
                    return false;
                }
                String keystorepass = value.getString();
                if ((keystorepass == null) || (keystorepass.isEmpty())) {
                    logger.error("Bad KeyStore Passwd");
                    return false;
                }
                value = hashConfig.get(XML_PATH_KEYPASS);
                if (value == null || (value.isEmpty())) {
                    logger.error("Unable to find Key Passwd");
                    return false;
                }
                String keypass = value.getString();
                if ((keypass == null) || (keypass.isEmpty())) {
                    logger.error("Bad Key Passwd");
                    return false;
                }
                try {
                    NetworkSslServerInitializer.setWaarpSecureKeyStore(new WaarpSecureKeyStore(keypath, keystorepass,
                            keypass));
                } catch (CryptoException e) {
                    logger.error("Bad SecureKeyStore construction");
                    return false;
                }
            }
            // TrustedKey for OpenR66 server
            value = hashConfig.get(XML_PATH_TRUSTKEYPATH);
            if (value == null || (value.isEmpty())) {
                logger.info("Unable to find TRUST Key Path");
                NetworkSslServerInitializer.getWaarpSecureKeyStore().initEmptyTrustStore();
            } else {
                String keypath = value.getString();
                if ((keypath == null) || (keypath.isEmpty())) {
                    logger.error("Bad TRUST Key Path");
                    return false;
                }
                value = hashConfig.get(XML_PATH_TRUSTKEYSTOREPASS);
                if (value == null || (value.isEmpty())) {
                    logger.error("Unable to find TRUST KeyStore Passwd");
                    return false;
                }
                String keystorepass = value.getString();
                if ((keystorepass == null) || (keystorepass.isEmpty())) {
                    logger.error("Bad TRUST KeyStore Passwd");
                    return false;
                }
                boolean useClientAuthent = false;
                value = hashConfig.get(XML_USECLIENT_AUTHENT);
                if (value != null && (!value.isEmpty())) {
                    useClientAuthent = value.getBoolean();
                }
                try {
                    NetworkSslServerInitializer.getWaarpSecureKeyStore().initTrustStore(keypath,
                            keystorepass, useClientAuthent);
                } catch (CryptoException e) {
                    logger.error("Bad TrustKeyStore construction");
                    return false;
                }
            }
            NetworkSslServerInitializer.setWaarpSslContextFactory(new WaarpSslContextFactory(
                    NetworkSslServerInitializer.getWaarpSecureKeyStore()));
            return true;
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
    }

    /**
     *
     * @param config
     * @return True if the network configuration is correctly loaded
     */
    private static boolean loadNetworkServer(Configuration config) {
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_NETWORK));
        try {
            XmlValue value = hashConfig.get(XML_SERVER_PORT);
            int port = 6666;
            if (value != null && (!value.isEmpty())) {
                port = value.getInteger();
            } else {
                port = 6666;
            }
            config.setSERVER_PORT(port);
            value = hashConfig.get(XML_SERVER_SSLPORT);
            int sslport = 6667;
            if (value != null && (!value.isEmpty())) {
                sslport = value.getInteger();
            } else {
                sslport = 6667;
            }
            config.setSERVER_SSLPORT(sslport);
            value = hashConfig.get(XML_SERVER_HTTPPORT);
            int httpport = 8066;
            if (value != null && (!value.isEmpty())) {
                httpport = value.getInteger();
            }
            config.setSERVER_HTTPPORT(httpport);
            value = hashConfig.get(XML_SERVER_HTTPSPORT);
            int httpsport = 8067;
            if (value != null && (!value.isEmpty())) {
                httpsport = value.getInteger();
            }
            config.setSERVER_HTTPSPORT(httpsport);
            return true;
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
    }

    /**
     *
     * @param configuration
     * @return True if the REST configuration is correctly loaded
     */
    @SuppressWarnings("unchecked")
    private static boolean loadRest(Configuration configuration) {
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_REST));
        try {
            XmlValue valueRest = hashConfig.get(XML_REST);
            if (valueRest != null && (valueRest.getList() != null)) {
                for (XmlValue[] xml : (List<XmlValue[]>) valueRest.getList()) {
                    RestConfiguration config = new RestConfiguration();
                    XmlHash subHash = new XmlHash(xml);
                    XmlValue value = subHash.get(XML_SERVER_REST_PORT);
                    int restPort = -1;
                    if (value != null && (!value.isEmpty())) {
                        restPort = value.getInteger();
                    }
                    config.REST_PORT = restPort;
                    if (config.REST_PORT > 0) {
                        value = subHash.get(XML_REST_ADDRESS);
                        String restAddress = null;
                        if (value != null && (!value.isEmpty())) {
                            restAddress = value.getString();
                        }
                        config.REST_ADDRESS = restAddress;
                        value = subHash.get(XML_REST_SSL);
                        boolean restSsl = false;
                        if (value != null && (!value.isEmpty())) {
                            restSsl = value.getBoolean();
                        }
                        config.REST_SSL = restSsl;
                        value = subHash.get(XML_REST_AUTHENTICATED);
                        boolean restAuthent = false;
                        if (value != null && (!value.isEmpty())) {
                            restAuthent = value.getBoolean();
                        }
                        config.REST_AUTHENTICATED = restAuthent;
                        value = subHash.get(XML_REST_SIGNATURE);
                        boolean restSignature = true;
                        if (value != null && (!value.isEmpty())) {
                            restSignature = value.getBoolean();
                        }
                        config.REST_SIGNATURE = restSignature;
                        if (config.REST_SIGNATURE || config.REST_AUTHENTICATED) {
                            XmlValue valueKey = subHash.get(XML_REST_AUTH_KEY);
                            if (valueKey != null && (!valueKey.isEmpty())) {
                                String fileKey = valueKey.getString();
                                File file = new File(fileKey);
                                if (!file.canRead()) {
                                    file = new File(configuration.getConfigPath() + FilesystemBasedDirImpl.SEPARATOR + fileKey);
                                    if (!file.canRead()) {
                                        logger.error("Unable to find REST Key in Config file");
                                        return false;
                                    }
                                    (fileKey) = configuration.getConfigPath() + FilesystemBasedDirImpl.SEPARATOR + fileKey;
                                }
                                try {
                                    config.initializeKey(file);
                                } catch (CryptoException e) {
                                    logger.error("Unable to load REST Key from Config file: " + fileKey, e);
                                    return false;
                                } catch (IOException e) {
                                    logger.error("Unable to load REST Key from Config file: " + fileKey, e);
                                    return false;
                                }
                            }
                        }
                        value = subHash.get(XML_REST_TIME_LIMIT);
                        long restTimeLimit = -1;
                        if (value != null && (!value.isEmpty())) {
                            restTimeLimit = value.getLong();
                        }
                        config.REST_TIME_LIMIT = restTimeLimit;

                        XmlValue valueMethod = subHash.get(XML_REST_METHOD);
                        if (valueMethod != null && (valueMethod.getList() != null)) {
                            boolean found = false;
                            config.RESTHANDLERS_CRUD = new byte[RESTHANDLERS.values().length];
                            for (XmlValue[] xmlmethod : (List<XmlValue[]>) valueMethod.getList()) {
                                XmlHash subHashMethod = new XmlHash(xmlmethod);
                                value = subHashMethod.get(XML_REST_METHOD_NAME);
                                String name = null;
                                if (value != null && (!value.isEmpty())) {
                                    name = value.getString();
                                } else {
                                    logger.warn("Restmethod entry ignore since name is empty");
                                    continue;
                                }
                                value = subHashMethod.get(XML_REST_CRUD);
                                String crud = null;
                                if (value != null && (!value.isEmpty())) {
                                    crud = value.getString().toUpperCase();
                                } else {
                                    logger.warn("Restmethod entry ignore since crud field is empty");
                                    continue;
                                }
                                found = true;
                                byte def = 0x0;
                                def |= (crud.contains("C") ? RestConfiguration.CRUD.CREATE.mask : 0);
                                def |= (crud.contains("R") ? RestConfiguration.CRUD.READ.mask : 0);
                                def |= (crud.contains("U") ? RestConfiguration.CRUD.UPDATE.mask : 0);
                                def |= (crud.contains("D") ? RestConfiguration.CRUD.DELETE.mask : 0);
                                if (name.equalsIgnoreCase("all")) {
                                    for (int i = 0; i < config.RESTHANDLERS_CRUD.length; i++) {
                                        config.RESTHANDLERS_CRUD[i] = def;
                                    }
                                    // No more restmethod since ALL was selected
                                    break;
                                } else {
                                    String[] handlers = name.split(" |\\|");
                                    for (String string : handlers) {
                                        RESTHANDLERS handler = RESTHANDLERS.valueOf(string);
                                        config.RESTHANDLERS_CRUD[handler.ordinal()] = def;
                                    }
                                }
                            }
                            if (!found) {
                                // no METHOD !!!
                                logger.error("No active METHOD defined for REST in Config file: " + config);
                                return false;
                            }
                        } else {
                            // no METHOD !!!
                            logger.error("No METHOD defined for REST in Config file");
                            return false;
                        }
                        Configuration.configuration.getRestConfigurations().add(config);
                        logger.info(config.toString());
                    }
                }
            }
            return true;
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
    }

    /**
     * Set the Crypto Key from the Document
     *
     * @param config
     * @return True if OK
     */
    private static boolean setCryptoKey(Configuration config, XmlHash hashConfig) {
        XmlValue value = hashConfig.get(XML_PATH_CRYPTOKEY);
        if (value == null || (value.isEmpty())) {
            logger.error("Unable to find CryptoKey in Config file");
            return false;
        }
        String filename = value.getString();
        config.setCryptoFile(filename);
        File key = new File(filename);
        Des des = new Des();
        try {
            des.setSecretKey(key);
        } catch (CryptoException e) {
            logger.error("Unable to load CryptoKey from Config file");
            return false;
        } catch (IOException e) {
            logger.error("Unable to load CryptoKey from Config file");
            return false;
        }
        config.setCryptoKey(des);
        return true;
    }

    /**
     * Load data from database or from files if not connected
     *
     * @param config
     * @return True if OK
     */
    private static boolean loadFromDatabase(Configuration config) {
        if (!config.isSaveTaskRunnerWithNoDb()) {
            // load from database the limit to apply
            try {
                DbConfiguration configuration = new DbConfiguration(config.getHOST_ID());
                configuration.updateConfiguration();
            } catch (WaarpDatabaseException e) {
                logger.info(Messages.getString("FileBasedConfiguration.NoBandwidth") + e.getMessage()); //$NON-NLS-1$
            }
        } else {
            if (config.getBaseDirectory() != null &&
                    config.getConfigPath() != null) {
                // load Rules from files
                File dirConfig = new File(
                        config.getBaseDirectory() +
                                config.getConfigPath());
                if (dirConfig.isDirectory()) {
                    try {
                        RuleFileBasedConfiguration.importRules(dirConfig);
                    } catch (OpenR66ProtocolSystemException e) {
                        logger.error(Messages.getString("FileBasedConfiguration.NoRule"), e); //$NON-NLS-1$
                        return false;
                    } catch (WaarpDatabaseException e) {
                        logger.error(Messages.getString("FileBasedConfiguration.NoRule"), e); //$NON-NLS-1$
                        return false;
                    }
                } else {
                    logger.error("Config Directory is not a directory: " +
                            config.getBaseDirectory() +
                            config.getConfigPath());
                    return false;
                }
            }
            // load if possible the limit to apply
            loadLimit(config, false);
        }
        return true;
    }

    public static boolean autoupgrade = false;

    /**
     * Load database parameter
     *
     * @param config
     * @param initdb
     * @return True if OK
     */
    private static boolean loadDatabase(Configuration config, boolean initdb) {
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_DB));
        try {
            XmlValue value = hashConfig.get(XML_SAVE_TASKRUNNERNODB);
            if (value != null && (!value.isEmpty()) && value.getBoolean()) {
                config.setSaveTaskRunnerWithNoDb(value.getBoolean());
                logger.info(Messages.getString("FileBasedConfiguration.NoDB")); //$NON-NLS-1$
                DbConstant.admin = new DbAdmin(); // no database support
                DbConstant.noCommitAdmin = DbConstant.admin;
                DAOFactory.initialize();
                return true;
            }
            value = hashConfig.get(XML_DBDRIVER);
            if (value == null || (value.isEmpty())) {
                if (config.isWarnOnStartup()) {
                    logger.warn(Messages.getString("FileBasedConfiguration.NoDB")); //$NON-NLS-1$
                } else {
                    logger.info(Messages.getString("FileBasedConfiguration.NoDB")); //$NON-NLS-1$
                }
                DbConstant.admin = new DbAdmin(); // no database support
                DbConstant.noCommitAdmin = DbConstant.admin;
                DAOFactory.initialize();
            } else {
                String dbdriver = value.getString();
                value = hashConfig.get(XML_DBSERVER);
                if (value == null || (value.isEmpty())) {
                    logger.error(Messages.getString("FileBasedConfiguration.NotFoundConfig") + "DBServer"); //$NON-NLS-1$
                    return false;
                }
                String dbserver = value.getString();
                value = hashConfig.get(XML_DBUSER);
                if (value == null || (value.isEmpty())) {
                    logger.error(Messages.getString("FileBasedConfiguration.NotFoundConfig") + "DBUser"); //$NON-NLS-1$
                    return false;
                }
                String dbuser = value.getString();
                value = hashConfig.get(XML_DBPASSWD);
                if (value == null || (value.isEmpty())) {
                    logger.error(Messages.getString("FileBasedConfiguration.NotFoundConfig") + "DBPassword"); //$NON-NLS-1$
                    return false;
                }
                String dbpasswd = value.getString();
                if (dbdriver == null || dbserver == null || dbuser == null ||
                        dbpasswd == null || dbdriver.isEmpty() ||
                        dbserver.isEmpty() || dbuser.isEmpty() ||
                        dbpasswd.isEmpty()) {
                    logger.error(Messages.getString("FileBasedConfiguration.NotFoundConfig") + "Correct DB data"); //$NON-NLS-1$
                    return false;
                }
                try {
                    DbConstant.admin =
                            DbModelFactory.initialize(dbdriver, dbserver, dbuser, dbpasswd,
                                    true);
		            // New way of initializing database services
                    try {
                        ConnectionFactory.initialize(dbserver, dbuser, dbpasswd);
		            } catch (UnsupportedOperationException e) {
		                logger.error(e);
			            return false;
                    } catch (SQLException e) {
                        logger.error("Cannot create ConnectionFactory", e);
			            return false;
                    }
                    DAOFactory.initialize(ConnectionFactory.getInstance());

                    if (config.getMultipleMonitors() > 1) {
                        DbConstant.noCommitAdmin =
                                DbModelFactory.initialize(dbdriver, dbserver, dbuser, dbpasswd,
                                        true);
                        Configuration.setNBDBSESSION(Configuration.getNBDBSESSION() + 1);
                        DbConstant.noCommitAdmin.getSession().setAutoCommit(false);
                    } else {
                        DbConstant.noCommitAdmin = DbConstant.admin;
                    }
                    logger.info("Database connection: Admin:" + (DbConstant.admin != null) + " NoCommitAdmin:"
                            + (DbConstant.noCommitAdmin != null));

                    try {
                        logger.info("DefaultTransactionIsolation: " +
                                DbConstant.admin.getSession().getConn().getMetaData().getDefaultTransactionIsolation() +
                                " MaxConnections: " +
                                DbConstant.admin.getSession().getConn().getMetaData().getMaxConnections() +
                                " MaxStatements: " +
                                DbConstant.admin.getSession().getConn().getMetaData().getMaxStatements());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } catch (WaarpDatabaseNoConnectionException e2) {
                    logger.error(Messages.getString("Database.CannotConnect"), e2); //$NON-NLS-1$
                    return false;
                }
                // Check if the database is ready (initdb already done before)
                DbRequest request = null;
                if (!initdb) {
                    try {
                        request = new DbRequest(DbConstant.admin.getSession());
                        try {
                            request.select("SELECT * FROM " + DbConfiguration.table);
                        } catch (WaarpDatabaseSqlException e) {
                            logger.warn(Messages.getString("Database.DbNotInitiated"), e); //$NON-NLS-1$
                            return true;
                        } finally {
                            request.close();
                        }
                    } catch (WaarpDatabaseNoConnectionException e1) {
                    // ignore
                    /* TODO: Why Ignore?
                     * throwing bad configuration seems better
                     */
                    }
                }
                // TODO to remove when <dbcheck> is drop from config file
                value = hashConfig.get(XML_DBCHECK);
                if (value != null && (!value.isEmpty())) {
                    logger.warn("<{}> is deprecated in configuration file "
                                    + "use <{}> instead",
                            XML_DBCHECK, XML_DBAUTOUPGRADE);
                    autoupgrade = value.getBoolean();
                } else {
                    // Keep this part
                    value = hashConfig.get(XML_DBAUTOUPGRADE);
                    if (value != null && (!value.isEmpty())) {
                        autoupgrade = value.getBoolean();
                    }
                }
                if (autoupgrade && !initdb) {
                    // Check if the database is up to date
                    try {
                        if (!ServerInitDatabase.upgradedb()) {
                            return false;
                        }
                    } catch (WaarpDatabaseException e) {
                        logger.error(e);
                        return false;
                    }
                }
            }
            return true;
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
    }

    /**
     * Load white list for Business if any
     *
     * @param config
     */
    private static void loadBusinessWhiteList(Configuration config) {
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(DbHostConfiguration.XML_BUSINESS));
        try {
            XmlValue value = hashConfig.get(DbHostConfiguration.XML_BUSINESS);
            if (value != null && (value.getList() != null)) {
                @SuppressWarnings("unchecked")
                List<String> ids = (List<String>) value.getList();
                if (ids != null) {
                    for (String sval : ids) {
                        if (sval.isEmpty()) {
                            continue;
                        }
                        logger.info("Business Allow: " + sval);
                        config.getBusinessWhiteSet().add(sval.trim());
                    }
                    ids.clear();
                    ids = null;
                }
            }
            loadAliases(config);
            // now check in DB
            if (DbConstant.admin != null) {
                try {
                    DbHostConfiguration hostconfiguration = new DbHostConfiguration(config.getHOST_ID());
                    if (hostconfiguration != null) {
                        DbHostConfiguration.updateHostConfiguration(config, hostconfiguration);
                    }
                } catch (WaarpDatabaseException e) {
                    // ignore
                }
            }
            setSelfVersion(config);
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
    }

    /**
     * Load the aliases configuration
     *
     * @param config
     */
    @SuppressWarnings("unchecked")
    private static void loadAliases(Configuration config) {
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(DbHostConfiguration.XML_ALIASES));
        try {
            XmlValue value = hashConfig.get(DbHostConfiguration.XML_ALIASES);
            if (value != null && (value.getList() != null)) {
                for (XmlValue[] xml : (List<XmlValue[]>) value.getList()) {
                    XmlHash subHash = new XmlHash(xml);
                    value = subHash.get(DbHostConfiguration.XML_REALID);
                    if (value == null || (value.isEmpty())) {
                        continue;
                    }
                    String refHostId = value.getString();
                    value = subHash.get(DbHostConfiguration.XML_ALIASID);
                    if (value == null || (value.isEmpty())) {
                        continue;
                    }
                    String aliasset = value.getString();
                    String[] alias = aliasset.split(" |\\|");
                    for (String namealias : alias) {
                        config.getAliases().put(namealias, refHostId);
                    }
                    config.getReverseAliases().put(refHostId, alias);
                    logger.info("Aliases for: " + refHostId + " = " + aliasset);
                }
            }
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
    }

    /**
     * Add the local host in Versions
     *
     * @param config
     */
    private static void setSelfVersion(Configuration config) {
        if (config.getHOST_ID() != null) {
            config.getVersions().putIfAbsent(config.getHOST_ID(), new PartnerConfiguration(config.getHOST_ID()));
        }
        if (config.getHOST_SSLID() != null) {
            config.getVersions().putIfAbsent(config.getHOST_SSLID(), new PartnerConfiguration(config.getHOST_SSLID()));
        }
        logger.debug("Partners: {}", config.getVersions());
    }

    /**
     * Load Role list if any
     *
     * @param config
     */
    @SuppressWarnings("unchecked")
    private static void loadRolesList(Configuration config) {
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(DbHostConfiguration.XML_ROLES));
        try {
            XmlValue value = hashConfig.get(DbHostConfiguration.XML_ROLES);
            if (value != null && (value.getList() != null)) {
                for (XmlValue[] xml : (List<XmlValue[]>) value.getList()) {
                    XmlHash subHash = new XmlHash(xml);
                    value = subHash.get(DbHostConfiguration.XML_ROLEID);
                    if (value == null || (value.isEmpty())) {
                        continue;
                    }
                    String refHostId = value.getString();
                    value = subHash.get(DbHostConfiguration.XML_ROLESET);
                    if (value == null || (value.isEmpty())) {
                        continue;
                    }
                    String roleset = value.getString();
                    String[] roles = roleset.split(" |\\|");
                    RoleDefault newrole = new RoleDefault();
                    for (String role : roles) {
                        try {
                            RoleDefault.ROLE roletype = RoleDefault.ROLE.valueOf(role.toUpperCase());
                            if (roletype == ROLE.NOACCESS) {
                                // reset
                                newrole.setRole(roletype);
                            } else {
                                newrole.addRole(roletype);
                            }
                        } catch (IllegalArgumentException e) {
                            // ignore
                        }
                    }
                    logger.info("New Role: " + refHostId + ":" + newrole);
                    config.getRoles().put(refHostId, newrole);
                }
            }
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
    }

    /**
     *
     * @param config
     * @param fromXML
     * @return the new subpath
     * @throws OpenR66ProtocolSystemException
     */
    private static String getSubPath(Configuration config, String fromXML)
            throws OpenR66ProtocolSystemException {
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_DIRECTORY));
        try {
            XmlValue value = hashConfig.get(fromXML);
            if (value == null || (value.isEmpty())) {
                logger.error(Messages.getString("FileBasedConfiguration.NoXmlPath") + fromXML); //$NON-NLS-1$
                throw new OpenR66ProtocolSystemException(
                        Messages.getString("FileBasedConfiguration.NoXmlPath") + fromXML); //$NON-NLS-1$
            }

            String path = value.getString();
            if (path == null || path.isEmpty()) {
                throw new OpenR66ProtocolSystemException(
                        Messages.getString("FileBasedConfiguration.NotCorrectPath") + fromXML); //$NON-NLS-1$
            }
            path = DirInterface.SEPARATOR + path;
            String newpath = config.getBaseDirectory() + path;
            File file = new File(newpath);
            if (!file.isDirectory()) {
                FileUtils.createDir(file);
            }
            return path;
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
    }

    /**
     * Load minimalistic Limit configuration
     *
     * @param config
     * @param filename
     * @return True if OK
     */
    public static boolean setConfigurationLoadLimitFromXml(Configuration config, String filename) {
        Document document = null;
        alreadySetLimit = false;
        // Open config file
        try {
            document = new SAXReader().read(filename);
        } catch (DocumentException e) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename, e); //$NON-NLS-1$
            return false;
        }
        if (document == null) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename); //$NON-NLS-1$
            return false;
        }
        configuration = XmlUtil.read(document, configServer);
        hashRootConfig = new XmlRootHash(configuration);
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_IDENTITY));
        try {
            loadLocale(hashConfig);
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
        if (!loadLimit(config, true)) {
            logger.error(Messages.getString("FileBasedConfiguration.NoLimit") + filename); //$NON-NLS-1$
            return false;
        }
        hashRootConfig.clear();
        hashRootConfig = null;
        configuration = null;
        return true;
    }

    /**
     * Load configuration for init database
     *
     * @param config
     * @param filename
     * @return True if OK
     */
    public static boolean setConfigurationInitDatabase(Configuration config,
               String filename, boolean initdb) {
        Document document = null;
        // Open config file
        try {
            document = new SAXReader().read(filename);
        } catch (DocumentException e) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename, e); //$NON-NLS-1$
            return false;
        }
        if (document == null) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename); //$NON-NLS-1$
            return false;
        }
        configuration = XmlUtil.read(document, configServer);
        hashRootConfig = new XmlRootHash(configuration);
        if (!loadIdentity(config)) {
            logger.error("Cannot load Identity");
            return false;
        }
        if (!loadDatabase(config, initdb)) {
            logger.error("Cannot load Database configuration");
            return false;
        }
        if (!loadDirectory(config)) {
            logger.error("Cannot load Directory configuration");
            return false;
        }
        if (!loadLimit(config, false)) {
            logger.error("Cannot load Limit configuration");
            return false;
        }
        if (config.isSaveTaskRunnerWithNoDb()) {
            // if no database, must load authentication from file
            if (!loadAuthentication(config)) {
                logger.error("Cannot load Authentication configuration");
                return false;
            }
        }
        hashRootConfig.clear();
        hashRootConfig = null;
        configuration = null;
        return true;
    }

    /**
     * Load minimalistic configuration
     *
     * @param config
     * @param filename
     * @return True if OK
     */
    public static boolean setConfigurationServerMinimalFromXml(Configuration config, String filename) {
        if (!SystemPropertyUtil.get(
                R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK, "" )
                .equals("")) {
            logger.warn("{} is deprecated in system properties use {} instead",
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK,
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE);
            FileBasedConfiguration.autoupgrade = SystemPropertyUtil.getBoolean(
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK, false);
        } else {
            FileBasedConfiguration.autoupgrade = SystemPropertyUtil.getBoolean(
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE, false);
        }

        Document document = null;
        // Open config file
        try {
            document = new SAXReader().read(filename);
        } catch (DocumentException e) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename, e); //$NON-NLS-1$
            return false;
        }
        if (document == null) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename); //$NON-NLS-1$
            return false;
        }
        configuration = XmlUtil.read(document, configServer);
        hashRootConfig = new XmlRootHash(configuration);
        if (!loadIdentity(config)) {
            logger.error("Cannot load Identity");
            return false;
        }
        if (!loadDatabase(config, false)) {
            logger.error("Cannot load Database configuration");
            return false;
        }
        if (!loadDirectory(config)) {
            logger.error("Cannot load Directory configuration");
            return false;
        }
        if (!loadLimit(config, false)) {
            logger.error("Cannot load Limit configuration");
            return false;
        }
        if (config.isSaveTaskRunnerWithNoDb()) {
            // if no database, must load authentication from file
            if (!loadAuthentication(config)) {
                logger.error("Cannot load Authentication configuration");
                return false;
            }
        }
        config.setHOST_AUTH(R66Auth.getServerAuth(
                DbConstant.admin.getSession(), config.getHOST_ID()));
        if (config.getHOST_AUTH() == null &&
                config.isUseNOSSL()) {
            logger.error("Cannot find Authentication for current host");
            return false;
        }
        if (config.getHOST_SSLID() != null) {
            config.setHOST_SSLAUTH(R66Auth.getServerAuth(
                    DbConstant.admin.getSession(),
                    config.getHOST_SSLID()));
            if (config.getHOST_SSLAUTH() == null &&
                    config.isUseSSL()) {
                logger.error("Cannot find SSL Authentication for current host");
                return false;
            }
        }
        hashRootConfig.clear();
        hashRootConfig = null;
        configuration = null;
        return true;
    }

    /**
     * Initiate the configuration from the xml file for server shutdown
     *
     * @param config
     * @param filename
     * @return True if OK
     */
    public static boolean setConfigurationServerShutdownFromXml(Configuration config,
            String filename) {
        if (!SystemPropertyUtil.get(
                R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK, "" )
                .equals("")) {
            logger.warn("{} is deprecated in system properties use {} instead",
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK,
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE);
            FileBasedConfiguration.autoupgrade = SystemPropertyUtil.getBoolean(
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK, false);
        } else {
            FileBasedConfiguration.autoupgrade = SystemPropertyUtil.getBoolean(
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE, false);
        }

        Document document = null;
        // Open config file
        try {
            document = new SAXReader().read(filename);
        } catch (DocumentException e) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename, e); //$NON-NLS-1$
            return false;
        }
        if (document == null) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename); //$NON-NLS-1$
            return false;
        }
        configuration = XmlUtil.read(document, configServer);
        hashRootConfig = new XmlRootHash(configuration);
        // Now read the configuration
        if (!loadIdentity(config)) {
            logger.error("Cannot load Identity");
            return false;
        }
        if (!loadDatabase(config, false)) {
            logger.error("Cannot load Database configuration");
            return false;
        }
        if (!loadServerParam(config)) {
            logger.error("Cannot load Server Parameters");
            return false;
        }
        if (!loadDirectory(config)) {
            logger.error("Cannot load Directory configuration");
            return false;
        }
        if (!loadLimit(config, false)) {
            logger.error("Cannot load Limit configuration");
            return false;
        }
        if (config.isUseSSL()) {
            if (!loadSsl(config)) {
                logger.error("Cannot load SSL configuration");
                return false;
            }
        }
        if (!loadNetworkServer(config)) {
            logger.error("Cannot load Network configuration");
            return false;
        }
        if (config.isSaveTaskRunnerWithNoDb()) {
            // if no database, must load authentication from file
            if (!loadAuthentication(config)) {
                logger.error("Cannot load Authentication configuration");
                return false;
            }
        }
        config.setHOST_AUTH(R66Auth.getServerAuth(
                DbConstant.admin.getSession(), config.getHOST_ID()));
        if (config.getHOST_AUTH() == null &&
                config.isUseNOSSL()) {
            logger.error("Cannot find Authentication for current host");
            return false;
        }
        if (config.getHOST_SSLID() != null) {
            config.setHOST_SSLAUTH(R66Auth.getServerAuth(
                    DbConstant.admin.getSession(),
                    config.getHOST_SSLID()));
            if (config.getHOST_SSLAUTH() == null &&
                    config.isUseSSL()) {
                logger.error("Cannot find SSL Authentication for current host");
                return false;
            }
        }
        loadBusinessWhiteList(config);
        hashRootConfig.clear();
        hashRootConfig = null;
        configuration = null;
        return true;
    }

    /**
     * Initiate the configuration from the xml file for server
     *
     * @param config
     * @param filename
     * @return True if OK
     */
    public static boolean setConfigurationServerFromXml(Configuration config, String filename) {
        Document document = null;
        // Open config file
        try {
            document = new SAXReader().read(filename);
        } catch (DocumentException e) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename, e); //$NON-NLS-1$
            return false;
        }
        if (document == null) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename); //$NON-NLS-1$
            return false;
        }
        configuration = XmlUtil.read(document, configServer);
        hashRootConfig = new XmlRootHash(configuration);
        // Now read the configuration
        if (!loadIdentity(config)) {
            logger.error("Cannot load Identity");
            return false;
        }
        if (!loadDatabase(config, false)) {
            logger.error("Cannot load Database configuration");
            return false;
        }
        if (!loadServerParam(config)) {
            logger.error("Cannot load Server Parameters");
            return false;
        }
        if (!loadDirectory(config)) {
            logger.error("Cannot load Directory configuration");
            return false;
        }
        if (!loadLimit(config, false)) {
            logger.error("Cannot load Limit configuration");
            return false;
        }
        if (config.isUseSSL()) {
            if (!loadSsl(config)) {
                logger.error("Cannot load SSL configuration");
                return false;
            }
        }
        if (!loadNetworkServer(config)) {
            logger.error("Cannot load Network configuration");
            return false;
        }
        if (!loadRest(config)) {
            logger.error("Cannot load REST configuration");
            return false;
        }
        if (!loadFromDatabase(config)) {
            logger.error("Cannot load configuration from Database");
            return false;
        }
        if (config.isSaveTaskRunnerWithNoDb()) {
            // if no database, must load authentication from file
            if (!loadAuthentication(config)) {
                logger.error("Cannot load Authentication configuration");
                return false;
            }
        }
        config.setHOST_AUTH(R66Auth.getServerAuth(
                DbConstant.admin.getSession(), config.getHOST_ID()));
        if (config.getHOST_AUTH() == null &&
                config.isUseNOSSL()) {
            logger.error("Cannot find Authentication for current host");
            return false;
        }
        if (config.getHOST_SSLID() != null) {
            config.setHOST_SSLAUTH(R66Auth.getServerAuth(
                    DbConstant.admin.getSession(),
                    config.getHOST_SSLID()));
            if (config.getHOST_SSLAUTH() == null &&
                    config.isUseSSL()) {
                logger.error("Cannot find SSL Authentication for current host");
                return false;
            }
        }
        loadBusinessWhiteList(config);
        loadRolesList(config);
        hashRootConfig.clear();
        hashRootConfig = null;
        configuration = null;
        return true;
    }

    /**
     * Initiate the configuration from the xml file for database client
     *
     * @param config
     * @param filename
     * @return True if OK
     */
    public static boolean setClientConfigurationFromXml(Configuration config, String filename) {
        if (!SystemPropertyUtil.get(
                R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK, "" )
                .equals("")) {
            logger.warn("{} is deprecated in system properties use {} instead",
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK,
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE);
            FileBasedConfiguration.autoupgrade = SystemPropertyUtil.getBoolean(
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK, false);
        } else {
            FileBasedConfiguration.autoupgrade = SystemPropertyUtil.getBoolean(
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE, false);
        }

        Document document = null;
        // Open config file
        try {
            document = new SAXReader().read(filename);
        } catch (DocumentException e) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename, e); //$NON-NLS-1$
            return false;
        }
        if (document == null) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename); //$NON-NLS-1$
            return false;
        }
        configuration = XmlUtil.read(document, configClient);
        hashRootConfig = new XmlRootHash(configuration);
        // Client enables SSL by default but could be reverted later on
        config.setUseSSL(true);
        if (!loadIdentity(config)) {
            logger.error("Cannot load Identity");
            return false;
        }
        if (!loadDatabase(config, false)) {
            logger.error("Cannot load Database configuration");
            return false;
        }
        logger.info("Is Client connected to database: " + DbConstant.admin.isActive());
        if (!loadClientParam(config)) {
            logger.error("Cannot load Client Parameters");
            return false;
        }
        if (!loadLimit(config, false)) {
            logger.error("Cannot load Limit configuration");
            return false;
        }
        if (config.isUseSSL()) {
            if (!loadSsl(config)) {
                logger.error("Cannot load SSL configuration");
                return false;
            }
        }
        if (!loadFromDatabase(config)) {
            logger.error("Cannot load configuration from Database");
            return false;
        }
        if (config.isSaveTaskRunnerWithNoDb()) {
            // if no database, must load authentication from file
            if (!loadAuthentication(config)) {
                logger.error("Cannot load Authentication configuration");
                return false;
            }
        }
        try {
            config.setHOST_AUTH(new DbHostAuth(config.getHOST_ID()));
        } catch (WaarpDatabaseException e) {
            logger.error("Cannot find Authentication for current host", e);
            return false;
        }
        if (config.getHOST_AUTH() == null) {
            logger.error("Cannot find Authentication for current host");
            return false;
        }
        if (config.getHOST_SSLID() != null) {
            config.setHOST_SSLAUTH(R66Auth.getServerAuth(
                    DbConstant.admin.getSession(),
                    config.getHOST_SSLID()));
            if (config.getHOST_SSLAUTH() == null) {
                logger.error("Cannot find SSL Authentication for current host");
                return false;
            }
        }
        loadBusinessWhiteList(config);
        hashRootConfig.clear();
        hashRootConfig = null;
        configuration = null;
        return true;
    }

    /**
     * Initiate the configuration from the xml file for submit database client
     *
     * @param config
     * @param filename
     * @return True if OK
     */
    public static boolean setSubmitClientConfigurationFromXml(Configuration config, String filename) {
        if (!SystemPropertyUtil.get(
                R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK, "" )
                .equals("")) {
            logger.warn("{} is deprecated in system properties use {} instead",
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK,
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE);
            FileBasedConfiguration.autoupgrade = SystemPropertyUtil.getBoolean(
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK, false);
        } else {
            FileBasedConfiguration.autoupgrade = SystemPropertyUtil.getBoolean(
                    R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE, false);
        }

        Document document = null;
        // Open config file
        try {
            document = new SAXReader().read(filename);
        } catch (DocumentException e) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename, e); //$NON-NLS-1$
            return false;
        }
        if (document == null) {
            logger.error(Messages.getString("FileBasedConfiguration.CannotReadXml") + filename); //$NON-NLS-1$
            return false;
        }
        configuration = XmlUtil.read(document, configSubmitClient);
        hashRootConfig = new XmlRootHash(configuration);
        // Client enables SSL by default but could be reverted later on
        config.setUseSSL(true);
        if (!loadIdentity(config)) {
            logger.error("Cannot load Identity");
            return false;
        }
        if (!loadDatabase(config, false)) {
            logger.error("Cannot load Database configuration");
            return false;
        }
        if (!loadDirectory(config)) {
            logger.error("Cannot load Directory configuration");
            return false;
        }
        XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_LIMIT));
        try {
            XmlValue value = hashConfig.get(XML_BLOCKSIZE);
            if (value != null && (!value.isEmpty())) {
                config.setBLOCKSIZE(value.getInteger());
            }
        } finally {
            hashConfig.clear();
            hashConfig = null;
        }
        config.setHOST_AUTH(R66Auth.getServerAuth(
                DbConstant.admin.getSession(), config.getHOST_ID()));
        if (config.getHOST_AUTH() == null) {
            logger.error("Cannot find Authentication for current host");
            return false;
        }
        if (config.getHOST_SSLID() != null) {
            config.setHOST_SSLAUTH(R66Auth.getServerAuth(
                    DbConstant.admin.getSession(),
                    config.getHOST_SSLID()));
            if (config.getHOST_SSLAUTH() == null) {
                logger.error("Cannot find SSL Authentication for current host");
                return false;
            }
        }
        loadBusinessWhiteList(config);
        hashRootConfig.clear();
        hashRootConfig = null;
        configuration = null;
        return true;
    }
}
