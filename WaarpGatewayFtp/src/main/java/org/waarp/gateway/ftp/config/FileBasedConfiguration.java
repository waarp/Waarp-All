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

package org.waarp.gateway.ftp.config;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.ChannelGroupFutureListener;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.traffic.AbstractTrafficShapingHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.waarp.common.crypto.Des;
import org.waarp.common.crypto.ssl.WaarpSecureKeyStore;
import org.waarp.common.crypto.ssl.WaarpSslContextFactory;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.exception.CryptoException;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.file.AbstractDir;
import org.waarp.common.file.DirInterface;
import org.waarp.common.file.FileParameterInterface;
import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.common.file.filesystembased.specific.FilesystemBasedDirJdkAbstract;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.common.utility.WaarpThreadFactory;
import org.waarp.common.xml.XmlDecl;
import org.waarp.common.xml.XmlHash;
import org.waarp.common.xml.XmlType;
import org.waarp.common.xml.XmlUtil;
import org.waarp.common.xml.XmlValue;
import org.waarp.ftp.core.config.FtpConfiguration;
import org.waarp.ftp.core.control.BusinessHandler;
import org.waarp.ftp.core.control.ftps.FtpsInitializer;
import org.waarp.ftp.core.data.handler.DataBusinessHandler;
import org.waarp.ftp.core.exception.FtpNoConnectionException;
import org.waarp.ftp.core.exception.FtpUnknownFieldException;
import org.waarp.gateway.ftp.adminssl.HttpSslInitializer;
import org.waarp.gateway.ftp.control.FtpConstraintLimitHandler;
import org.waarp.gateway.ftp.database.DbConstantFtp;
import org.waarp.gateway.ftp.database.data.DbTransferLog;
import org.waarp.gateway.ftp.database.model.DbModelFactoryFtp;
import org.waarp.gateway.ftp.exec.AbstractExecutor;
import org.waarp.gateway.ftp.exec.LocalExecClient;
import org.waarp.gateway.ftp.file.SimpleAuth;
import org.waarp.gateway.ftp.snmp.FtpMonitoring;
import org.waarp.gateway.ftp.snmp.FtpPrivateMib;
import org.waarp.gateway.ftp.snmp.FtpVariableFactory;
import org.waarp.snmp.SnmpConfiguration;
import org.waarp.snmp.WaarpMOFactory;
import org.waarp.snmp.WaarpSnmpAgent;

import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FtpConfiguration based on a XML file
 */
public class FileBasedConfiguration extends FtpConfiguration {
  private static final String ERROR_DURING_WRITE_AUTHENTICATION_FILE =
      "Error during Write Authentication file";

  private static final String UNABLE_TO_FIND_LOCAL_EXEC_ADDRESS_IN_CONFIG_FILE =
      "Unable to find LocalExec Address in Config file";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FileBasedConfiguration.class);

  /**
   * SERVER HOSTID
   */
  private static final String XML_SERVER_HOSTID = "hostid";
  /**
   * Authentication
   */
  private static final String XML_AUTHENTIFICATION_FILE = "authentfile";
  /**
   * SERVER CRYPTO for Password
   */
  private static final String XML_PATH_CRYPTOKEY = "cryptokey";

  /**
   * Structure of the Configuration file
   */
  private static final XmlDecl[] configIdentityDecls = {
      // identity
      new XmlDecl(XmlType.STRING, XML_SERVER_HOSTID),
      new XmlDecl(XmlType.STRING, XML_PATH_CRYPTOKEY),
      new XmlDecl(XmlType.STRING, XML_AUTHENTIFICATION_FILE)
  };
  /**
   * Use HTTP compression for R66 HTTP connection
   */
  private static final String XML_USEHTTPCOMP = "usehttpcomp";
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
   * ADMINISTRATOR SERVER NAME (shutdown)
   */
  private static final String XML_SERVER_ADMIN = "serveradmin";
  /**
   * SERVER PASSWORD (shutdown)
   */
  private static final String XML_SERVER_PASSWD = "serverpasswd"; //NOSONAR
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
   * HTTP Admin Directory
   */
  private static final String XML_HTTPADMINPATH = "httpadmin";
  /**
   * Monitoring: snmp configuration file (if empty, no snmp support)
   */
  private static final String XML_MONITOR_SNMP_CONFIG = "snmpconfig";

  /**
   * Structure of the Configuration file
   */
  private static final XmlDecl[] configServerParamDecls = {
      // server
      new XmlDecl(XmlType.BOOLEAN, XML_USELOCALEXEC),
      new XmlDecl(XmlType.STRING, XML_LEXECADDR),
      new XmlDecl(XmlType.INTEGER, XML_LEXECPORT),
      new XmlDecl(XmlType.STRING, XML_SERVER_ADMIN),
      new XmlDecl(XmlType.STRING, XML_SERVER_PASSWD),
      new XmlDecl(XmlType.BOOLEAN, XML_USEHTTPCOMP),
      new XmlDecl(XmlType.STRING, XML_HTTPADMINPATH),
      new XmlDecl(XmlType.STRING, XML_PATH_ADMIN_KEYPATH),
      new XmlDecl(XmlType.STRING, XML_PATH_ADMIN_KEYSTOREPASS),
      new XmlDecl(XmlType.STRING, XML_PATH_ADMIN_KEYPASS),
      new XmlDecl(XmlType.STRING, XML_MONITOR_SNMP_CONFIG)
  };
  /**
   * SERVER PORT
   */
  private static final String XML_SERVER_PORT = "serverport";
  /**
   * SERVER ADDRESS if any
   */
  private static final String XML_SERVER_ADDRESS = "serveraddress";
  /**
   * RANGE of PORT for Passive Mode
   */
  private static final String XML_RANGE_PORT_MIN = "portmin";

  /**
   * RANGE of PORT for Passive Mode
   */
  private static final String XML_RANGE_PORT_MAX = "portmax";
  /**
   * SERVER HTTP PORT MONITORING
   */
  private static final String XML_SERVER_HTTP_PORT = "serverhttpport";
  /**
   * SERVER HTTPS PORT ADMINISTRATION
   */
  private static final String XML_SERVER_HTTPS_PORT = "serverhttpsport";

  /**
   * Structure of the Configuration file
   */
  private static final XmlDecl[] configNetworkServerDecls = {
      // network
      new XmlDecl(XmlType.INTEGER, XML_SERVER_PORT),
      new XmlDecl(XmlType.STRING, XML_SERVER_ADDRESS),
      new XmlDecl(XmlType.INTEGER, XML_RANGE_PORT_MIN),
      new XmlDecl(XmlType.INTEGER, XML_RANGE_PORT_MAX),
      new XmlDecl(XmlType.INTEGER, XML_SERVER_HTTP_PORT),
      new XmlDecl(XmlType.INTEGER, XML_SERVER_HTTPS_PORT)
  };
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
  private static final String XML_DBPASSWD = "dbpasswd";//NOSONAR
  /**
   * Structure of the Configuration file
   */
  private static final XmlDecl[] configDbDecls = {
      // db
      new XmlDecl(XmlType.STRING, XML_DBDRIVER),
      new XmlDecl(XmlType.STRING, XML_DBSERVER),
      new XmlDecl(XmlType.STRING, XML_DBUSER),
      new XmlDecl(XmlType.STRING, XML_DBPASSWD)
  };
  /**
   * Allow PASSIVE = -1 / ACTIVE = 1 / Both = 0
   */
  private static final String XML_ACTIVE_OR_PASSIVE = "activepassive";
  /**
   * Should a file be deleted when a Store like command is aborted
   */
  private static final String XML_DELETEONABORT = "deleteonabort";
  /**
   * Default number of threads in pool for Server.
   */
  private static final String XML_SERVER_THREAD = "serverthread";

  /**
   * Default number of threads in pool for Client.
   */
  private static final String XML_CLIENT_THREAD = "clientthread";
  /**
   * Memory Limit to use.
   */
  private static final String XML_MEMORY_LIMIT = "memorylimit";

  /**
   * Limit for Session
   */
  private static final String XML_LIMITSESSION = "sessionlimit";

  /**
   * Limit for Global
   */
  private static final String XML_LIMITGLOBAL = "globallimit";
  /**
   * Delay between two checks for Limit
   */
  private static final String XML_LIMITDELAY = "delaylimit";
  /**
   * Nb of milliseconds after connection is in timeout
   */
  private static final String XML_TIMEOUTCON = "timeoutcon";
  /**
   * Size by default of block size for receive/sending files. Should be a
   * multiple of 8192 (maximum = 64K due to
   * block limitation to 2 bytes)
   */
  private static final String XML_BLOCKSIZE = "blocksize";
  /**
   * Should a file MD5 SHA1 be computed using NIO
   */
  private static final String XML_USENIO = "usenio";

  /**
   * Should a file MD5 be computed using FastMD5
   */
  private static final String XML_USEFASTMD5 = "usefastmd5";

  /**
   * If using Fast MD5, should we used the binary JNI library, empty meaning
   * no
   */
  private static final String XML_FASTMD5 = "fastmd5";
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
   * CPU HIGH limit to apply decrease of throttle, 0 meaning no throttle
   * activated
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
   * Structure of the Configuration file
   */
  private static final XmlDecl[] configLimitDecls = {
      // limit
     new XmlDecl(XmlType.INTEGER, XML_ACTIVE_OR_PASSIVE),
      new XmlDecl(XmlType.BOOLEAN, XML_DELETEONABORT),
      new XmlDecl(XmlType.LONG, XML_LIMITSESSION),
      new XmlDecl(XmlType.LONG, XML_LIMITGLOBAL),
      new XmlDecl(XmlType.LONG, XML_LIMITDELAY),
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
      new XmlDecl(XmlType.BOOLEAN, XML_USEFASTMD5),
      new XmlDecl(XmlType.STRING, XML_FASTMD5),
      new XmlDecl(XmlType.INTEGER, XML_BLOCKSIZE)
  };

  /**
   * RETRIEVE COMMAND
   */
  public static final String XML_RETRIEVE_COMMAND = "retrievecmd";

  /**
   * STORE COMMAND
   */
  public static final String XML_STORE_COMMAND = "storecmd";

  /**
   * DELAY RETRIEVE COMMAND
   */
  public static final String XML_DELAYRETRIEVE_COMMAND = "retrievedelay";

  /**
   * DELAY STORE COMMAND
   */
  public static final String XML_DELAYSTORE_COMMAND = "storedelay";
  /**
   * Structure of the Configuration file
   */
  private static final XmlDecl[] configExecDecls = {
      // Exec
      new XmlDecl(XmlType.STRING, XML_RETRIEVE_COMMAND),
      new XmlDecl(XmlType.LONG, XML_DELAYRETRIEVE_COMMAND),
      new XmlDecl(XmlType.STRING, XML_STORE_COMMAND),
      new XmlDecl(XmlType.LONG, XML_DELAYSTORE_COMMAND)
  };
  /**
   * Base Directory
   */
  private static final String XML_SERVER_HOME = "serverhome";
  /**
   * Structure of the Configuration file
   */
  private static final XmlDecl[] configDirectoryDecls = {
      // directory
      new XmlDecl(XmlType.STRING, XML_SERVER_HOME)
  };
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
   * SERVER SSL Use TrustStore for Client Authentication
   */
  private static final String XML_USECLIENT_AUTHENT =
      "trustuseclientauthenticate";
  /**
   * SERVER SSL Use Implicit FTPS
   */
  private static final String XML_IMPLICIT_FTPS = "useimplicitftps";
  /**
   * SERVER SSL Use Explicit FTPS
   */
  private static final String XML_EXPLICIT_FTPS = "useexplicitftps";

  /**
   * Structure of the Configuration file
   */
  private static final XmlDecl[] configSslDecls = {
      // ssl
      new XmlDecl(XmlType.STRING, XML_PATH_KEYPATH),
      new XmlDecl(XmlType.STRING, XML_PATH_KEYSTOREPASS),
      new XmlDecl(XmlType.STRING, XML_PATH_KEYPASS),
      new XmlDecl(XmlType.STRING, XML_PATH_TRUSTKEYPATH),
      new XmlDecl(XmlType.STRING, XML_PATH_TRUSTKEYSTOREPASS),
      new XmlDecl(XmlType.BOOLEAN, XML_USECLIENT_AUTHENT),
      new XmlDecl(XmlType.BOOLEAN, XML_IMPLICIT_FTPS),
      new XmlDecl(XmlType.BOOLEAN, XML_EXPLICIT_FTPS)
  };
  /**
   * Overall structure of the Configuration file
   */
  private static final String XML_ROOT = "/config/";
  private static final String XML_IDENTITY = "identity";
  private static final String XML_SERVER = "server";
  private static final String XML_DIRECTORY = "directory";
  private static final String XML_LIMIT = "limit";
  private static final String XML_NETWORK = "network";
  private static final String XML_EXEC = "exec";
  private static final String XML_DB = "db";
  private static final String XML_SSL = "ssl";
  /**
   * Global Structure for Server Configuration
   */
  private static final XmlDecl[] configServer = {
      new XmlDecl(XML_IDENTITY, XmlType.XVAL, XML_ROOT + XML_IDENTITY,
                  configIdentityDecls, false),
      new XmlDecl(XML_SERVER, XmlType.XVAL, XML_ROOT + XML_SERVER,
                  configServerParamDecls, false),
      new XmlDecl(XML_NETWORK, XmlType.XVAL, XML_ROOT + XML_NETWORK,
                  configNetworkServerDecls, false),
      new XmlDecl(XML_EXEC, XmlType.XVAL, XML_ROOT + XML_EXEC, configExecDecls,
                  false),
      new XmlDecl(XML_DIRECTORY, XmlType.XVAL, XML_ROOT + XML_DIRECTORY,
                  configDirectoryDecls, false),
      new XmlDecl(XML_LIMIT, XmlType.XVAL, XML_ROOT + XML_LIMIT,
                  configLimitDecls, false),
      new XmlDecl(XML_DB, XmlType.XVAL, XML_ROOT + XML_DB, configDbDecls,
                  false),
      new XmlDecl(XML_SSL, XmlType.XVAL, XML_ROOT + XML_SSL, configSslDecls,
                  false)
  };

  /**
   * Authentication Fields
   */
  private static final String XML_AUTHENTIFICATION_ROOT = "authent";
  /**
   * Authentication Fields
   */
  private static final String XML_AUTHENTIFICATION_ENTRY = "entry";
  /**
   * Authentication Fields
   */
  private static final String XML_AUTHENTIFICATION_BASED =
      '/' + XML_AUTHENTIFICATION_ROOT + '/' + XML_AUTHENTIFICATION_ENTRY;

  /**
   * Authentication Fields
   */
  private static final String XML_AUTHENTICATION_USER = "user";

  /**
   * Authentication Fields
   */
  private static final String XML_AUTHENTICATION_PASSWD = "passwd";//NOSONAR
  /**
   * Authentication Fields
   */
  private static final String XML_AUTHENTICATION_PASSWDFILE = //NOSONAR
      "passwdfile";//NOSONAR

  /**
   * Authentication Fields
   */
  private static final String XML_AUTHENTICATION_ACCOUNT = "account";

  /**
   * Authentication Fields
   */
  private static final String XML_AUTHENTICATION_ADMIN = "admin";
  /**
   * Structure of the Configuration file
   */
  private static final XmlDecl[] configAuthenticationDecls = {
      // identity
      new XmlDecl(XmlType.STRING, XML_AUTHENTICATION_USER),
      new XmlDecl(XmlType.STRING, XML_AUTHENTICATION_PASSWDFILE),
      new XmlDecl(XmlType.STRING, XML_AUTHENTICATION_PASSWD),
      new XmlDecl(XML_AUTHENTICATION_ACCOUNT, XmlType.STRING,
                  XML_AUTHENTICATION_ACCOUNT, true),
      new XmlDecl(XmlType.BOOLEAN, XML_AUTHENTICATION_ADMIN),
      // Exec
      new XmlDecl(XmlType.STRING, XML_RETRIEVE_COMMAND),
      new XmlDecl(XmlType.LONG, XML_DELAYRETRIEVE_COMMAND),
      new XmlDecl(XmlType.STRING, XML_STORE_COMMAND),
      new XmlDecl(XmlType.LONG, XML_DELAYSTORE_COMMAND)
  };
  /**
   * Global Structure for Server Configuration
   */
  private static final XmlDecl[] authentElements = {
      new XmlDecl(XML_AUTHENTIFICATION_ENTRY, XmlType.XVAL,
                  XML_AUTHENTIFICATION_BASED, configAuthenticationDecls, true)
  };

  /**
   * RANGE of PORT for Passive Mode
   */
  private static final String RANGE_PORT = "FTP_RANGE_PORT";
  /**
   * Use to access directly the configuration
   */
  public static FileBasedConfiguration fileBasedConfiguration;
  /**
   * All authentications
   */
  private final ConcurrentHashMap<String, SimpleAuth> authentications =
      new ConcurrentHashMap<String, SimpleAuth>();

  /**
   * File containing the authentications
   */
  private String authenticationFile;

  /**
   * Default HTTP server port
   */
  private int serverHttpsPort = 8067;
  /**
   * Http Admin base
   */
  private String httpBasePath = "src/main/admin/";
  /**
   * Does this server will try to compress HTTP connections
   */
  private boolean useHttpCompression;

  /**
   * Does this server will use Waarp LocalExec Daemon for Execute
   */
  private boolean useLocalExec;

  /**
   * Crypto Key
   */
  private Des cryptoKey;
  /**
   * Server Administration Key
   */
  private byte[] serverAdminKey;
  /**
   * FTP server ID
   */
  private String hostId = "noId";
  /**
   * Admin name Id
   */
  private String adminName = "noAdmin";
  /**
   * Limit on CPU and Connection
   */
  private FtpConstraintLimitHandler constraintLimitHandler;

  /**
   * List of all Http Channels to enable the close call on them using Netty
   * ChannelGroup
   */
  private ChannelGroup httpChannelGroup;

  /**
   * Server Group for HTTP
   */
  private EventLoopGroup serverGroup;

  /**
   * Worker Group for HTTP
   */
  private EventLoopGroup workerGroup;

  /**
   * ThreadPoolExecutor for Http and Https Server
   */
  private EventExecutorGroup httpExecutor;
  /**
   * Monitoring: snmp configuration file (empty means no snmp support)
   */
  private String snmpConfig;
  /**
   * SNMP Agent (if any)
   */
  private WaarpSnmpAgent agentSnmp;
  /**
   * Associated MIB
   */
  private FtpPrivateMib ftpMib;
  /**
   * Monitoring object
   */
  private FtpMonitoring monitoring;

  /**
   * @param classtype
   * @param businessHandler class that will be used for
   *     BusinessHandler
   * @param dataBusinessHandler class that will be used for
   *     DataBusinessHandler
   * @param fileParameter the FileParameter to use
   */
  public FileBasedConfiguration(final Class<?> classtype,
                                final Class<? extends BusinessHandler> businessHandler,
                                final Class<? extends DataBusinessHandler> dataBusinessHandler,
                                final FileParameterInterface fileParameter) {
    super(classtype, businessHandler, dataBusinessHandler, fileParameter);
    computeNbThreads();
  }

  private static XmlHash hashConfig;

  private boolean loadIdentity() {
    final XmlValue value = hashConfig.get(XML_SERVER_HOSTID);
    if (value != null && !value.isEmpty()) {
      setHostId(value.getString());
    } else {
      logger.error("Unable to find Host ID in Config file");
      return false;
    }
    return setCryptoKey();
  }

  private boolean loadAuthentication() {
    // if no database, must load authentication from file
    final XmlValue value = hashConfig.get(XML_AUTHENTIFICATION_FILE);
    if (value != null && !value.isEmpty()) {
      setAuthenticationFile(value.getString());
      return initializeAuthent(getAuthenticationFile(), false);
    } else {
      logger.warn("Unable to find Authentication file in Config file");
      return false;
    }
  }

  private boolean loadServerParam() {
    XmlValue value = hashConfig.get(XML_USEHTTPCOMP);
    if (value != null && !value.isEmpty()) {
      setUseHttpCompression(value.getBoolean());
    }
    value = hashConfig.get(XML_USELOCALEXEC);
    if (value != null && !value.isEmpty()) {
      setUseLocalExec(value.getBoolean());
      if (isUseLocalExec()) {
        value = hashConfig.get(XML_LEXECADDR);
        final String saddr;
        final InetAddress addr;
        if (value != null && !value.isEmpty()) {
          saddr = value.getString();
          try {
            addr = InetAddress.getByName(saddr);
          } catch (final UnknownHostException e) {
            logger.error(UNABLE_TO_FIND_LOCAL_EXEC_ADDRESS_IN_CONFIG_FILE);
            return false;
          }
        } else {
          logger.warn(UNABLE_TO_FIND_LOCAL_EXEC_ADDRESS_IN_CONFIG_FILE);
          try {
            addr = InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
          } catch (final UnknownHostException e) {
            logger.error(UNABLE_TO_FIND_LOCAL_EXEC_ADDRESS_IN_CONFIG_FILE);
            return false;
          }
        }
        value = hashConfig.get(XML_LEXECPORT);
        final int port;
        if (value != null && !value.isEmpty()) {
          port = value.getInteger();
        } else {
          port = 9999;
        }
        LocalExecClient.setAddress(new InetSocketAddress(addr, port));
      }
    }
    value = hashConfig.get(XML_SERVER_ADMIN);
    if (value != null && !value.isEmpty()) {
      setAdminName(value.getString());
    } else {
      logger.error("Unable to find Administrator name in Config file");
      return false;
    }
    if (getCryptoKey() == null && !setCryptoKey()) {
      logger.error("Unable to find Crypto Key in Config file");
      return false;
    }
    final String passwd;
    value = hashConfig.get(XML_SERVER_PASSWD);
    if (value != null && !value.isEmpty()) {
      passwd = value.getString();
    } else {
      logger.error("Unable to find Password in Config file");
      return false;
    }
    final byte[] decodedByteKeys;
    try {
      decodedByteKeys = getCryptoKey().decryptHexInBytes(passwd);
    } catch (final Exception e) {
      logger.error(
          "Unable to Decrypt Server Password in Config file from: " + passwd,
          e);
      return false;
    }
    setSERVERKEY(decodedByteKeys);
    value = hashConfig.get(XML_HTTPADMINPATH);
    if (value == null || value.isEmpty()) {
      logger.error("Unable to find Http Admin Base in Config file");
      return false;
    }
    final String path = value.getString();
    if (path == null || path.length() == 0) {
      logger.warn(
          "Unable to set correct Http Admin Base in Config file. No HTTPS support will be used.");
      setHttpBasePath(null);
    } else {
      final File file = new File(path);
      if (!file.isDirectory()) {
        logger.error("Http Admin is not a directory in Config file");
        return false;
      }
      try {
        setHttpBasePath(AbstractDir.normalizePath(file.getCanonicalPath()) +
                        DirInterface.SEPARATOR);
      } catch (final IOException e1) {
        logger.error("Unable to set Http Admin Path in Config file");
        return false;
      }
    }
    if (getHttpBasePath() != null) {
      // Key for HTTPS
      value = hashConfig.get(XML_PATH_ADMIN_KEYPATH);
      if (value != null && !value.isEmpty()) {
        final String keypath = value.getString();
        if (keypath == null || keypath.length() == 0) {
          logger.error("Bad Key Path");
          return false;
        }
        value = hashConfig.get(XML_PATH_ADMIN_KEYSTOREPASS);
        if (value == null || value.isEmpty()) {
          logger.error("Unable to find KeyStore Passwd");
          return false;
        }
        final String keystorepass = value.getString();
        if (keystorepass == null || keystorepass.length() == 0) {
          logger.error("Bad KeyStore Passwd");
          return false;
        }
        value = hashConfig.get(XML_PATH_ADMIN_KEYPASS);
        if (value == null || value.isEmpty()) {
          logger.error("Unable to find Key Passwd");
          return false;
        }
        final String keypass = value.getString();
        if (keypass == null || keypass.length() == 0) {
          logger.error("Bad Key Passwd");
          return false;
        }
        try {
          HttpSslInitializer.waarpSecureKeyStore =
              new WaarpSecureKeyStore(keypath, keystorepass, keypass);
        } catch (final CryptoException e) {
          logger.error("Bad SecureKeyStore construction for AdminSsl");
          return false;
        }
        // No client authentication
        HttpSslInitializer.waarpSecureKeyStore.initEmptyTrustStore();
        HttpSslInitializer.waarpSslContextFactory =
            new WaarpSslContextFactory(HttpSslInitializer.waarpSecureKeyStore,
                                       true);
      }
    }
    value = hashConfig.get(XML_MONITOR_SNMP_CONFIG);
    if (value != null && !value.isEmpty()) {
      setSnmpConfig(value.getString());
      logger.warn("SNMP configuration file: " + getSnmpConfig());
      final File snmpfile = new File(getSnmpConfig());
      if (snmpfile.canRead()) {
        if (!SnmpConfiguration.setConfigurationFromXml(snmpfile)) {
          logger.warn("Bad SNMP configuration file: " + getSnmpConfig());
          setSnmpConfig(null);
        }
      } else {
        logger.warn("Cannot read SNMP configuration file: " + getSnmpConfig());
        setSnmpConfig(null);
      }
    } else {
      logger.warn("NO SNMP configuration file");
    }
    return true;
  }

  private boolean loadDirectory() {
    final XmlValue value = hashConfig.get(XML_SERVER_HOME);
    if (value == null || value.isEmpty()) {
      logger.error("Unable to find Home in Config file");
      return false;
    }
    final String path = value.getString();
    final File file = new File(path);
    if (!file.isDirectory()) {
      logger.error("Home is not a directory in Config file");
      return false;
    }
    try {
      setBaseDirectory(AbstractDir.normalizePath(file.getCanonicalPath()));
    } catch (final IOException e1) {
      logger.error("Unable to set Home in Config file: " + path);
      return false;
    }
    return true;
  }

  private boolean loadLimit() {
    XmlValue value = hashConfig.get(XML_LIMITGLOBAL);
    if (value != null && !value.isEmpty()) {
      serverGlobalReadLimit = value.getLong();
      if (serverGlobalReadLimit <= 0) {
        serverGlobalReadLimit = 0;
      }
      serverGlobalWriteLimit = serverGlobalReadLimit;
      logger.info("Global Limit: {}", serverGlobalReadLimit);
    }
    value = hashConfig.get(XML_LIMITSESSION);
    if (value != null && !value.isEmpty()) {
      serverChannelReadLimit = value.getLong();
      if (serverChannelReadLimit <= 0) {
        serverChannelReadLimit = 0;
      }
      serverChannelWriteLimit = serverChannelReadLimit;
      logger.info("SessionInterface Limit: {}", serverChannelReadLimit);
    }
    delayLimit = AbstractTrafficShapingHandler.DEFAULT_CHECK_INTERVAL;
    value = hashConfig.get(XML_LIMITDELAY);
    if (value != null && !value.isEmpty()) {
      delayLimit = (value.getLong() / 10) * 10;
      if (delayLimit <= 0) {
        delayLimit = 0;
      }
      logger.info("Delay Limit: {}", delayLimit);
    }
    boolean useCpuLimit = false;
    boolean useCpuLimitJDK = false;
    double cpulimit = 1.0;
    value = hashConfig.get(XML_CSTRT_USECPULIMIT);
    if (value != null && !value.isEmpty()) {
      useCpuLimit = value.getBoolean();
      value = hashConfig.get(XML_CSTRT_USECPUJDKLIMIT);
      if (value != null && !value.isEmpty()) {
        useCpuLimitJDK = value.getBoolean();
      }
      value = hashConfig.get(XML_CSTRT_CPULIMIT);
      if (value != null && !value.isEmpty()) {
        cpulimit = value.getDouble();
      }
    }
    int connlimit = 0;
    value = hashConfig.get(XML_CSTRT_CONNLIMIT);
    if (value != null && !value.isEmpty()) {
      connlimit = value.getInteger();
    }
    double lowcpuLimit = 0;
    double highcpuLimit = 0;
    double percentageDecrease = 0;
    long delay = 1000000;
    long limitLowBandwidth = 4096;
    value = hashConfig.get(XML_CSTRT_LOWCPULIMIT);
    if (value != null && !value.isEmpty()) {
      lowcpuLimit = value.getDouble();
    }
    value = hashConfig.get(XML_CSTRT_HIGHCPULIMIT);
    if (value != null && !value.isEmpty()) {
      highcpuLimit = value.getDouble();
    }
    value = hashConfig.get(XML_CSTRT_PERCENTDECREASE);
    if (value != null && !value.isEmpty()) {
      percentageDecrease = value.getDouble();
    }
    value = hashConfig.get(XML_CSTRT_DELAYTHROTTLE);
    if (value != null && !value.isEmpty()) {
      delay = (value.getLong() / 10) * 10;
    }
    value = hashConfig.get(XML_CSTRT_LIMITLOWBANDWIDTH);
    if (value != null && !value.isEmpty()) {
      limitLowBandwidth = value.getLong();
    }
    value = hashConfig.get(XML_TIMEOUTCON);
    if (value != null && !value.isEmpty()) {
      setTimeoutCon((value.getLong() / 10) * 10);
    }
    if (highcpuLimit > 0) {
      setConstraintLimitHandler(
          new FtpConstraintLimitHandler(getTimeoutCon(), useCpuLimit,
                                        useCpuLimitJDK, cpulimit, connlimit,
                                        lowcpuLimit, highcpuLimit,
                                        percentageDecrease, null, delay,
                                        limitLowBandwidth));
    } else {
      setConstraintLimitHandler(
          new FtpConstraintLimitHandler(getTimeoutCon(), useCpuLimit,
                                        useCpuLimitJDK, cpulimit, connlimit));
    }
    value = hashConfig.get(XML_SERVER_THREAD);
    if (value != null && !value.isEmpty()) {
      setServerThread(value.getInteger());
    }
    value = hashConfig.get(XML_CLIENT_THREAD);
    if (value != null && !value.isEmpty()) {
      setClientThread(value.getInteger());
    }
    if (getServerThread() == 0 || getClientThread() == 0) {
      computeNbThreads();
    }
    value = hashConfig.get(XML_MEMORY_LIMIT);
    if (value != null && !value.isEmpty()) {
      long lvalue = value.getLong();
      if (lvalue > Integer.MAX_VALUE) {
        lvalue = Integer.MAX_VALUE;
      }
      setMaxGlobalMemory((int) lvalue);
    }
    ((FilesystemBasedFileParameterImpl) getFileParameter()).deleteOnAbort =
        false;
    value = hashConfig.get(XML_USENIO);
    if (value != null && !value.isEmpty()) {
      FilesystemBasedFileParameterImpl.useNio = value.getBoolean();
    }
    value = hashConfig.get(XML_USEFASTMD5);
    if (value != null && !value.isEmpty()) {
      FilesystemBasedDigest.setUseFastMd5(value.getBoolean());
    }
    value = hashConfig.get(XML_BLOCKSIZE);
    if (value != null && !value.isEmpty()) {
      setBlocksize(value.getInteger());
    }
    value = hashConfig.get(XML_DELETEONABORT);
    if (value != null && !value.isEmpty()) {
      setDeleteOnAbort(value.getBoolean());
    }
    value = hashConfig.get(XML_ACTIVE_OR_PASSIVE);
    if (value != null && !value.isEmpty()) {
      setActivePassiveMode(value.getInteger());
    }
    // We use Apache Commons IO
    FilesystemBasedDirJdkAbstract.ueApacheCommonsIo = true;
    return true;
  }

  private boolean loadNetworkServer() {
    XmlValue value = hashConfig.get(XML_SERVER_PORT);
    final int port;
    if (value != null && !value.isEmpty()) {
      port = value.getInteger();
    } else {
      port = 21;
    }
    setServerPort(port);
    value = hashConfig.get(XML_SERVER_ADDRESS);
    String address = null;
    if (value != null && !value.isEmpty()) {
      address = value.getString();
    }
    setServerAddress(address);
    int min = 100;
    int max = 65535;
    value = hashConfig.get(XML_RANGE_PORT_MIN);
    if (value != null && !value.isEmpty()) {
      min = value.getInteger();
    }
    value = hashConfig.get(XML_RANGE_PORT_MAX);
    if (value != null && !value.isEmpty()) {
      max = value.getInteger();
    }
    logger.warn("Passive Port range Min: " + min + " Max: " + max);
    final CircularIntValue rangePort = new CircularIntValue(min, max);
    setRangePort(rangePort);
    value = hashConfig.get(XML_SERVER_HTTPS_PORT);
    int httpsport = 8067;
    if (value != null && !value.isEmpty()) {
      httpsport = value.getInteger();
    }
    serverHttpsPort = httpsport;
    return true;
  }

  /**
   * Set the Crypto Key from the Document
   *
   * @return True if OK
   */
  private boolean setCryptoKey() {
    final XmlValue value = hashConfig.get(XML_PATH_CRYPTOKEY);
    if (value == null || value.isEmpty()) {
      logger.error("Unable to find CryptoKey in Config file");
      return false;
    }
    final String filename = value.getString();
    final File key = new File(filename);
    final Des des = new Des();
    try {
      des.setSecretKey(key);
    } catch (final CryptoException e) {
      logger.error("Unable to load CryptoKey from Config file");
      return false;
    } catch (final IOException e) {
      logger.error("Unable to load CryptoKey from Config file");
      return false;
    }
    cryptoKey = des;
    return true;
  }

  /**
   * @return True if the global Exec parameters are correctly loaded
   */
  private boolean loadExec() {
    // Specific Exec command options
    XmlValue value = hashConfig.get(XML_RETRIEVE_COMMAND);
    if (value == null || value.isEmpty()) {
      logger.error("Unable to find Retrieve Command in Config file");
      return false;
    }
    final String retrieve = value.getString();
    value = hashConfig.get(XML_DELAYRETRIEVE_COMMAND);
    long retrievedelay = 0;
    if (value != null && !value.isEmpty()) {
      retrievedelay = (value.getLong() / 10) * 10;
    }
    value = hashConfig.get(XML_STORE_COMMAND);
    if (value == null || value.isEmpty()) {
      logger.error("Unable to find Store Command in Config file");
      return false;
    }
    final String store = value.getString();
    value = hashConfig.get(XML_DELAYSTORE_COMMAND);
    long storedelay = 0;
    if (value != null && !value.isEmpty()) {
      storedelay = (value.getLong() / 10) * 10;
    }
    AbstractExecutor
        .initializeExecutor(retrieve, retrievedelay, store, storedelay);
    return true;
  }

  /**
   * Load database parameter
   *
   * @return True if OK
   */
  private boolean loadDatabase() {
    XmlValue value = hashConfig.get(XML_DBDRIVER);
    if (value == null || value.isEmpty()) {
      logger.error("Unable to find DBDriver in Config file");
      DbConstantFtp.gatewayAdmin = new DbAdmin(); // no database support
    } else {
      final String dbdriver = value.getString();
      value = hashConfig.get(XML_DBSERVER);
      if (value == null || value.isEmpty()) {
        logger.error("Unable to find DBServer in Config file");
        return false;
      }
      final String dbserver = value.getString();
      value = hashConfig.get(XML_DBUSER);
      if (value == null || value.isEmpty()) {
        logger.error("Unable to find DBUser in Config file");
        return false;
      }
      final String dbuser = value.getString();
      value = hashConfig.get(XML_DBPASSWD);
      if (value == null || value.isEmpty()) {
        logger.error("Unable to find DBPassword in Config file");
        return false;
      }
      final String dbpasswd = value.getString();
      if (dbdriver == null || dbserver == null || dbuser == null ||
          dbpasswd == null || dbdriver.length() == 0 ||
          dbserver.length() == 0 || dbuser.length() == 0 ||
          dbpasswd.length() == 0) {
        logger.error("Unable to find Correct DB data in Config file");
        return false;
      }
      try {
        DbConstantFtp.gatewayAdmin = DbModelFactoryFtp
            .initialize(dbdriver, dbserver, dbuser, dbpasswd, true);
        org.waarp.common.database.DbConstant.admin = DbConstantFtp.gatewayAdmin;
      } catch (final WaarpDatabaseNoConnectionException e2) {
        logger.error("Unable to Connect to DB", e2);
        return false;
      }
    }
    return true;
  }

  protected boolean loadSsl() {
    // StoreKey for Server
    XmlValue value = hashConfig.get(XML_PATH_KEYPATH);
    if (value == null || value.isEmpty()) {
      logger.info("Unable to find Key Path");
      getFtpInternalConfiguration().setUsingNativeSsl(false);
      getFtpInternalConfiguration().setAcceptAuthProt(false);
      return true;
    } else {
      final String keypath = value.getString();
      if (keypath == null || keypath.length() == 0) {
        logger.error("Bad Key Path");
        return false;
      }
      value = hashConfig.get(XML_PATH_KEYSTOREPASS);
      if (value == null || value.isEmpty()) {
        logger.error("Unable to find KeyStore Passwd");
        return false;
      }
      final String keystorepass = value.getString();
      if (keystorepass == null || keystorepass.length() == 0) {
        logger.error("Bad KeyStore Passwd");
        return false;
      }
      value = hashConfig.get(XML_PATH_KEYPASS);
      if (value == null || value.isEmpty()) {
        logger.error("Unable to find Key Passwd");
        return false;
      }
      final String keypass = value.getString();
      if (keypass == null || keypass.length() == 0) {
        logger.error("Bad Key Passwd");
        return false;
      }
      try {
        FtpsInitializer.waarpSecureKeyStore =
            new WaarpSecureKeyStore(keypath, keystorepass, keypass);
      } catch (final CryptoException e) {
        logger.error("Bad SecureKeyStore construction");
        return false;
      }

    }
    // TrustedKey for OpenR66 server
    value = hashConfig.get(XML_PATH_TRUSTKEYPATH);
    if (value == null || value.isEmpty()) {
      logger.info("Unable to find TRUST Key Path");
      FtpsInitializer.waarpSecureKeyStore.initEmptyTrustStore();
    } else {
      final String keypath = value.getString();
      if (keypath == null || keypath.length() == 0) {
        logger.error("Bad TRUST Key Path");
        return false;
      }
      value = hashConfig.get(XML_PATH_TRUSTKEYSTOREPASS);
      if (value == null || value.isEmpty()) {
        logger.error("Unable to find TRUST KeyStore Passwd");
        return false;
      }
      final String keystorepass = value.getString();
      if (keystorepass == null || keystorepass.length() == 0) {
        logger.error("Bad TRUST KeyStore Passwd");
        return false;
      }
      boolean useClientAuthent = false;
      value = hashConfig.get(XML_USECLIENT_AUTHENT);
      if (value != null && !value.isEmpty()) {
        useClientAuthent = value.getBoolean();
      }
      try {
        FtpsInitializer.waarpSecureKeyStore
            .initTrustStore(keypath, keystorepass, useClientAuthent);
      } catch (final CryptoException e) {
        logger.error("Bad TrustKeyStore construction");
        return false;
      }
    }
    FtpsInitializer.waarpSslContextFactory =
        new WaarpSslContextFactory(FtpsInitializer.waarpSecureKeyStore);
    boolean useImplicit = false;
    value = hashConfig.get(XML_IMPLICIT_FTPS);
    if (value != null && !value.isEmpty()) {
      useImplicit = value.getBoolean();
    }
    boolean useExplicit = false;
    value = hashConfig.get(XML_EXPLICIT_FTPS);
    if (value != null && !value.isEmpty()) {
      useExplicit = value.getBoolean();
    }
    if (useImplicit && useExplicit) {
      logger.error("Only one of IMPLICIT or EXPLICIT could be True");
      return false;
    }
    if (!useImplicit && !useExplicit) {
      logger.error(
          "Since all SecureStore are specified, one of IMPLICIT or EXPLICIT should be True");
      logger.warn("FTPS support will be ignored...");
      getFtpInternalConfiguration().setUsingNativeSsl(false);
      getFtpInternalConfiguration().setAcceptAuthProt(false);
      return true;
    }
    getFtpInternalConfiguration().setUsingNativeSsl(useImplicit);
    getFtpInternalConfiguration().setAcceptAuthProt(useExplicit);
    return true;
  }

  /**
   * Initiate the configuration from the xml file for server
   *
   * @param filename
   *
   * @return True if OK
   */
  public boolean setConfigurationServerFromXml(final String filename) {
    final Document document;
    // Open config file
    try {
      document = XmlUtil.getNewSaxReader().read(filename);
    } catch (final DocumentException e) {
      logger.error("Unable to read the XML Config file: " + filename, e);
      return false;
    }
    if (document == null) {
      logger.error("Unable to read the XML Config file: " + filename);
      return false;
    }
    XmlValue[] configuration = XmlUtil.read(document, configServer);
    hashConfig = new XmlHash(configuration);
    // Now read the configuration
    if (!loadIdentity()) {
      logger.error("Cannot load Identity");
      return false;
    }
    if (!loadDatabase()) {
      logger.error("Cannot load Database configuration");
      return false;
    }
    if (!loadServerParam()) {
      logger.error("Cannot load Server Parameters");
      return false;
    }
    if (!loadDirectory()) {
      logger.error("Cannot load Directory configuration");
      return false;
    }
    if (!loadLimit()) {
      logger.error("Cannot load Limit configuration");
      return false;
    }
    if (!loadNetworkServer()) {
      logger.error("Cannot load Network configuration");
      return false;
    }
    if (!loadExec()) {
      logger.error("Cannot load Exec configuration");
      return false;
    }
    // if no database, must load authentication from file
    if (!loadAuthentication()) {
      logger.error("Cannot load Authentication configuration");
      return false;
    }
    if (!loadSsl()) {
      // ignore and continue => No SSL
      getFtpInternalConfiguration().setUsingNativeSsl(false);
      getFtpInternalConfiguration().setAcceptAuthProt(false);
    }
    hashConfig.clear();
    hashConfig = null;
    configuration = null;
    logger.debug("File based configuration loaded");
    return true;
  }

  /**
   * Configure HTTPS
   */
  public void configureHttps() {
    logger.debug("Start HTTPS");
    // Now start the HTTPS support
    // Configure the server.
    /*
     * Bootstrap for Https server
     */
    final ServerBootstrap httpsBootstrap = new ServerBootstrap();
    httpExecutor = new NioEventLoopGroup(getServerThread() * 10,
                                         new WaarpThreadFactory(
                                             "HttpExecutor"));
    serverGroup = new NioEventLoopGroup(getServerThread(),
                                        new WaarpThreadFactory("HTTP_Server"));
    workerGroup = new NioEventLoopGroup(getServerThread() * 10,
                                        new WaarpThreadFactory("HTTP_Worker"));
    WaarpNettyUtil
        .setServerBootstrap(httpsBootstrap, serverGroup, workerGroup,
                            (int) getTimeoutCon());

    // Configure the pipeline factory.
    httpsBootstrap.childHandler(new HttpSslInitializer(isUseHttpCompression()));
    httpChannelGroup =
        new DefaultChannelGroup("HttpOpenR66", httpExecutor.next());

    // Bind and start to accept incoming connections.
    logger.warn("Start Https Support on port: " + serverHttpsPort + " with " +
                (isUseHttpCompression()? "" : "no") + " compression support");
    final ChannelFuture future =
        httpsBootstrap.bind(new InetSocketAddress(serverHttpsPort));
    if (WaarpNettyUtil.awaitIsSuccessOfInterrupted(future)) {
      httpChannelGroup.add(future.channel());
    }
  }

  /**
   * Configure ConstraintLimitHandler
   */
  public void configureConstraint() {
    logger.debug("Configure constraints");
    getConstraintLimitHandler().setHandler(
        getFtpInternalConfiguration().getGlobalTrafficShapingHandler());
  }

  /**
   * Configure LocalExec
   */
  public void configureLExec() {
    if (isUseLocalExec()) {
      logger.debug("Start LExec");
      LocalExecClient.initialize(getClientThread(), getMaxGlobalMemory());
    }
  }

  /**
   * Configure the SNMP support if needed
   *
   * @throws FtpNoConnectionException
   */
  public void configureSnmp() throws FtpNoConnectionException {
    logger.debug("Start SNMP");
    setMonitoring(new FtpMonitoring(null));
    if (getSnmpConfig() != null) {
      final int snmpPortShow = getServerPort();
      setFtpMib(new FtpPrivateMib(snmpPortShow));
      WaarpMOFactory.setFactory(new FtpVariableFactory());
      setAgentSnmp(
          new WaarpSnmpAgent(new File(getSnmpConfig()), getMonitoring(),
                             getFtpMib()));
      try {
        getAgentSnmp().start();
        logger.debug("SNMP configured");
      } catch (final IOException e) {
        getMonitoring().releaseResources();
        setMonitoring(null);
        setFtpMib(null);
        setAgentSnmp(null);
        throw new FtpNoConnectionException("AgentSnmp Error while starting", e);
      }
    }
  }

  /**
   * @param serverkey the SERVERADMINKEY to set
   */
  public void setSERVERKEY(final byte[] serverkey) {
    serverAdminKey = serverkey;
  }

  /**
   * Check the password for Shutdown
   *
   * @param password
   *
   * @return True if the password is OK
   */
  @Override
  public boolean checkPassword(final String password) {
    if (password == null) {
      return false;
    }
    return Arrays
        .equals(serverAdminKey, password.getBytes(WaarpStringUtils.UTF8));
  }

  /**
   * Initialize Authentication from current authenticationFile
   *
   * @param filename the filename from which authentication will be
   *     loaded
   * @param purge if True, the current authentications are totally
   *     replaced
   *     by the new ones
   *
   * @return True if OK
   */
  @SuppressWarnings("unchecked")
  public boolean initializeAuthent(final String filename, final boolean purge) {
    logger.debug("Load authent");
    final Document document;
    try {
      document = XmlUtil.getNewSaxReader().read(filename);
    } catch (final DocumentException e) {
      logger
          .error("Unable to read the XML Authentication file: " + filename, e);
      return false;
    }
    if (document == null) {
      logger.error("Unable to read the XML Authentication file: " + filename);
      return false;
    }
    final XmlValue[] configurationXml = XmlUtil.read(document, authentElements);
    XmlHash hashConfigXml = new XmlHash(configurationXml);

    XmlValue value = hashConfigXml.get(XML_AUTHENTIFICATION_ENTRY);
    final List<XmlValue[]> list = (List<XmlValue[]>) value.getList();
    final ConcurrentHashMap<String, SimpleAuth> newAuthents =
        new ConcurrentHashMap<String, SimpleAuth>();
    for (final XmlValue[] xmlValues : list) {
      hashConfigXml = new XmlHash(xmlValues);
      value = hashConfigXml.get(XML_AUTHENTICATION_USER);
      if (value == null || value.isEmpty()) {
        logger.error("Unable to find a User in Config file");
        continue;
      }
      final String user = value.getString();
      value = hashConfigXml.get(XML_AUTHENTICATION_ACCOUNT);
      if (value == null || value.isEmpty()) {
        logger.error("Unable to find a Account in Config file: " + user);
        continue;
      }
      final String[] account;
      final List<String> listaccount = (List<String>) value.getList();
      if (!listaccount.isEmpty()) {
        account = new String[listaccount.size()];
        int i = 0;
        for (final String s : listaccount) {
          account[i] = s;
          final File directory =
              new File(getBaseDirectory() + '/' + user + '/' + account[i]);
          directory.mkdirs();//NOSONAR
          i++;
        }
      } else {
        logger.error("Unable to find a Account in Config file: " + user);
        continue;
      }
      value = hashConfigXml.get(XML_AUTHENTICATION_ADMIN);
      boolean isAdmin = false;
      if (value != null && !value.isEmpty()) {
        isAdmin = value.getBoolean();
      }
      String retrcmd = null;
      long retrdelay = 0;
      String storcmd = null;
      long stordelay = 0;
      value = hashConfigXml.get(XML_RETRIEVE_COMMAND);
      if (value != null && !value.isEmpty()) {
        retrcmd = value.getString();
      }
      value = hashConfigXml.get(XML_DELAYRETRIEVE_COMMAND);
      if (value != null && !value.isEmpty()) {
        retrdelay = (value.getLong() / 10) * 10;
      }
      value = hashConfigXml.get(XML_STORE_COMMAND);
      if (value != null && !value.isEmpty()) {
        storcmd = value.getString();
      }
      value = hashConfigXml.get(XML_DELAYSTORE_COMMAND);
      if (value != null && !value.isEmpty()) {
        stordelay = (value.getLong() / 10) * 10;
      }
      final String passwd;
      value = hashConfigXml.get(XML_AUTHENTICATION_PASSWDFILE);
      if (value != null && !value.isEmpty()) {
        // load key from file
        final File key = new File(value.getString());
        if (!key.canRead()) {
          logger
              .error("Cannot read key for user " + user + ':' + key.getName());
          continue;
        }
        try {
          final byte[] byteKeys = getCryptoKey().decryptHexFile(key);
          passwd = new String(byteKeys, WaarpStringUtils.UTF8);
        } catch (final Exception e2) {
          logger.error("Cannot read key for user " + user, e2);
          continue;
        }
      } else {
        value = hashConfigXml.get(XML_AUTHENTICATION_PASSWD);
        if (value != null && !value.isEmpty()) {
          final String encrypted = value.getString();
          final byte[] byteKeys;
          try {
            byteKeys = getCryptoKey().decryptHexInBytes(encrypted);
            passwd = new String(byteKeys, WaarpStringUtils.UTF8);
          } catch (final Exception e) {
            logger.error("Unable to Decrypt Key for user " + user, e);
            continue;
          }
        } else {
          logger.error("Unable to find Password in Config file");
          // DO NOT Allow empty key
          continue;
        }
      }
      final SimpleAuth auth =
          new SimpleAuth(user, passwd, account, storcmd, stordelay, retrcmd,
                         retrdelay);
      auth.setAdmin(isAdmin);
      newAuthents.put(user, auth);
      hashConfigXml.clear();
    }
    hashConfigXml.clear();
    if (purge) {
      authentications.clear();
    }
    authentications.putAll(newAuthents);
    newAuthents.clear();
    return true;
  }

  /**
   * Export the Authentication to the original files
   *
   * @param filename the filename where the authentication will be
   *     exported
   *
   * @return True if successful
   */
  public boolean saveAuthenticationFile(final String filename) {
    final Document document = XmlUtil.createEmptyDocument();
    final XmlValue[] roots = new XmlValue[1];
    final XmlValue root = new XmlValue(authentElements[0]);
    roots[0] = root;
    final Enumeration<SimpleAuth> auths = authentications.elements();
    while (auths.hasMoreElements()) {
      final SimpleAuth auth = auths.nextElement();
      final XmlValue[] values = new XmlValue[configAuthenticationDecls.length];
      for (int i = 0; i < configAuthenticationDecls.length; i++) {
        values[i] = new XmlValue(configAuthenticationDecls[i]);
      }
      try {
        values[0].setFromString(auth.getUser());
        // PasswdFile: none values[1].setFromString()
        values[2].setFromString(auth.getPassword());
        // Accounts
        final String[] accts = auth.getAccounts();
        for (final String string : accts) {
          values[3].addFromString(string);
        }
        values[4].setValue(auth.isAdmin());
        values[5].setFromString(auth.getRetrCmd());
        values[6].setValue(auth.getRetrDelay());
        values[7].setFromString(auth.getStorCmd());
        values[8].setValue(auth.getStorDelay());
      } catch (final InvalidArgumentException e1) {
        logger.error(ERROR_DURING_WRITE_AUTHENTICATION_FILE, e1);
        return false;
      } catch (final InvalidObjectException e) {
        logger.error(ERROR_DURING_WRITE_AUTHENTICATION_FILE, e);
        return false;
      }
      try {
        root.addValue(values);
      } catch (final InvalidObjectException e) {
        logger.error(ERROR_DURING_WRITE_AUTHENTICATION_FILE, e);
        return false;
      }
    }
    XmlUtil.write(document, roots);
    try {
      XmlUtil.saveDocument(filename, document);
    } catch (final IOException e1) {
      logger.error("Cannot write to file: " + filename + " since {}",
                   e1.getMessage());
      return false;
    }
    return true;
  }

  /**
   * @param user
   *
   * @return the SimpleAuth if any for this user
   */
  public SimpleAuth getSimpleAuth(final String user) {
    return authentications.get(user);
  }

  /**
   * @param format Format in HTML to use as ouput format
   *
   * @return the Html String containing the table of all Authentication
   *     entries
   */
  public String getHtmlAuth(final String format) {
    final String result;
    final StringBuilder builder = new StringBuilder();
    /*
     * XXXUSERXXX XXXPWDXXX XXXACTSXXX XXXADMXXX XXXSTCXXX XXXSTDXXX XXXRTCXXX XXXRTDXXX
     */
    final Enumeration<SimpleAuth> simpleAuths = authentications.elements();
    SimpleAuth auth;
    while (simpleAuths.hasMoreElements()) {
      auth = simpleAuths.nextElement();
      String newElt = format.replace("XXXUSERXXX", auth.getUser());
      newElt = newElt.replace("XXXPWDXXX", auth.getPassword());
      if (auth.getStorCmd() != null) {
        newElt = newElt.replace("XXXSTCXXX", auth.getStorCmd());
      } else {
        newElt = newElt.replace("XXXSTCXXX", "");
      }
      if (auth.getRetrCmd() != null) {
        newElt = newElt.replace("XXXRTCXXX", auth.getRetrCmd());
      } else {
        newElt = newElt.replace("XXXRTCXXX", "");
      }
      newElt = newElt.replace("XXXSTDXXX", Long.toString(auth.getStorDelay()));
      newElt = newElt.replace("XXXRTDXXX", Long.toString(auth.getRetrDelay()));
      newElt = newElt.replace("XXXADMXXX", Boolean.toString(auth.isAdmin()));
      if (auth.getAccounts() != null) {
        final StringBuilder accts = new StringBuilder();
        for (int i = 0; i < auth.getAccounts().length - 1; i++) {
          accts.append(auth.getAccounts()[i]).append(", ");
        }
        accts.append(auth.getAccounts()[auth.getAccounts().length - 1]);
        newElt = newElt.replace("XXXACTSXXX", accts.toString());
      } else {
        newElt = newElt.replace("XXXACTSXXX", "No Account");
      }
      builder.append(newElt);
    }
    result = builder.toString();
    return result;
  }

  /**
   * Only available with Database support for Waarp
   *
   * @param format Format in HTML to use as ouput format
   * @param limit number of TransferLog to populate
   *
   * @return the Html String containing the table of all Transfer entries
   */
  public String getHtmlTransfer(final String format, final int limit) {
    final String result;
    final StringBuilder builder = new StringBuilder();
    /*
     * XXXIDXXX XXXUSERXXX XXXACCTXXX XXXFILEXXX XXXMODEXXX XXXSTATUSXXX XXXINFOXXX XXXUPINFXXX XXXSTARTXXX
     * XXXSTOPXXX
     */
    DbPreparedStatement preparedStatement = null;
    try {
      try {
        preparedStatement = DbTransferLog
            .getStatusPrepareStament(DbConstantFtp.gatewayAdmin.getSession(),
                                     null, limit);
        preparedStatement.executeQuery();
      } catch (final WaarpDatabaseNoConnectionException e) {
        return "";
      } catch (final WaarpDatabaseSqlException e) {
        return "";
      }
      try {
        while (preparedStatement.getNext()) {
          final DbTransferLog log =
              DbTransferLog.getFromStatement(preparedStatement);
          String newElt =
              format.replace("XXXIDXXX", Long.toString(log.getSpecialId()));
          newElt = newElt.replace("XXXUSERXXX", log.getUser());
          newElt = newElt.replace("XXXACCTXXX", log.getAccount());
          newElt = newElt.replace("XXXFILEXXX", log.getFilename());
          newElt = newElt.replace("XXXMODEXXX", log.getMode());
          newElt = newElt.replace("XXXSTATUSXXX", log.getErrorInfo().getMesg());
          newElt = newElt.replace("XXXINFOXXX", log.getInfotransf());
          newElt = newElt.replace("XXXUPINFXXX", log.getUpdatedInfo().name());
          newElt = newElt.replace("XXXSTARTXXX", log.getStart().toString());
          newElt = newElt.replace("XXXSTOPXXX", log.getStop().toString());
          builder.append(newElt);
        }
      } catch (final WaarpDatabaseNoConnectionException e) {
        return "";
      } catch (final WaarpDatabaseSqlException e) {
        return "";
      }
      result = builder.toString();
      return result;
    } finally {
      if (preparedStatement != null) {
        preparedStatement.realClose();
      }
    }
  }

  /**
   * @see FtpConfiguration#getNextRangePort()
   */
  @Override
  public int getNextRangePort() {
    try {
      return ((CircularIntValue) getProperty(RANGE_PORT)).getNext();
    } catch (final FtpUnknownFieldException e) {
      return -1;
    }
  }

  /**
   * @param rangePort the range of available ports for Passive
   *     connections
   */
  private void setRangePort(final CircularIntValue rangePort) {
    setProperty(RANGE_PORT, rangePort);
  }

  /**
   * @return the httpPipelineExecutor
   */
  public EventExecutorGroup getHttpPipelineExecutor() {
    return httpExecutor;
  }

  /**
   * @return the httpChannelGroup
   */
  public ChannelGroup getHttpChannelGroup() {
    return httpChannelGroup;
  }

  /**
   * Finalize resources attached to handlers
   */
  private static class GgChannelGroupFutureListener
      implements ChannelGroupFutureListener {
    final EventExecutorGroup executorWorker;
    final String name;

    private GgChannelGroupFutureListener(final String name,
                                         final EventExecutorGroup executorWorker) {
      this.name = name;
      this.executorWorker = executorWorker;
    }

    @Override
    public void operationComplete(final ChannelGroupFuture future) {
      if (executorWorker != null) {
        executorWorker.shutdownGracefully();
      }
      logger.info("Done with shutdown {}", name);
    }
  }

  @Override
  public void releaseResources() {
    logger.debug("Release resources");
    super.releaseResources();
    if (httpChannelGroup != null) {
      final int result = httpChannelGroup.size();
      logger.debug("HttpChannelGroup: {}", result);
      httpChannelGroup.close().addListener(
          new GgChannelGroupFutureListener("HttpChannelGroup", workerGroup));
    }
    if (httpExecutor != null) {
      httpExecutor.shutdownGracefully();
    }
    if (isUseLocalExec()) {
      LocalExecClient.releaseResources();
    }
    if (getConstraintLimitHandler() != null) {
      getConstraintLimitHandler().release();
    }
    if (getAgentSnmp() != null) {
      getAgentSnmp().stop();
    }
    if (workerGroup != null) {
      workerGroup.shutdownGracefully();
    }
    if (serverGroup != null) {
      serverGroup.shutdownGracefully();
    }
    DbAdmin.closeAllConnection();
  }

  @Override
  public void inShutdownProcess() {
    if (getFtpMib() != null) {
      getFtpMib().notifyStartStop("Shutdown in progress for " + getHostId(),
                                  "Gives extra seconds: " + getTimeoutCon());
    }
  }

  /**
   * @return the authenticationFile
   */
  public String getAuthenticationFile() {
    return authenticationFile;
  }

  /**
   * @param authenticationFile the authenticationFile to set
   */
  public void setAuthenticationFile(final String authenticationFile) {
    this.authenticationFile = authenticationFile;
  }

  /**
   * @return the httpBasePath
   */
  public String getHttpBasePath() {
    return httpBasePath;
  }

  /**
   * @param httpBasePath the httpBasePath to set
   */
  public void setHttpBasePath(final String httpBasePath) {
    this.httpBasePath = httpBasePath;
  }

  /**
   * @return the useHttpCompression
   */
  public boolean isUseHttpCompression() {
    return useHttpCompression;
  }

  /**
   * @param useHttpCompression the useHttpCompression to set
   */
  public void setUseHttpCompression(final boolean useHttpCompression) {
    this.useHttpCompression = useHttpCompression;
  }

  /**
   * @return the useLocalExec
   */
  public boolean isUseLocalExec() {
    return useLocalExec;
  }

  /**
   * @param useLocalExec the useLocalExec to set
   */
  public void setUseLocalExec(final boolean useLocalExec) {
    this.useLocalExec = useLocalExec;
  }

  /**
   * @return the cryptoKey
   */
  public Des getCryptoKey() {
    return cryptoKey;
  }

  /**
   * @param cryptoKey the cryptoKey to set
   */
  public void setCryptoKey(final Des cryptoKey) {
    this.cryptoKey = cryptoKey;
  }

  /**
   * @return the hostId
   */
  public String getHostId() {
    return hostId;
  }

  /**
   * @param hostId the hostId to set
   */
  public void setHostId(final String hostId) {
    this.hostId = hostId;
  }

  /**
   * @return the adminName
   */
  public String getAdminName() {
    return adminName;
  }

  /**
   * @param adminName the adminName to set
   */
  public void setAdminName(final String adminName) {
    this.adminName = adminName;
  }

  /**
   * @return the constraintLimitHandler
   */
  public FtpConstraintLimitHandler getConstraintLimitHandler() {
    return constraintLimitHandler;
  }

  /**
   * @param constraintLimitHandler the constraintLimitHandler to set
   */
  public void setConstraintLimitHandler(
      final FtpConstraintLimitHandler constraintLimitHandler) {
    this.constraintLimitHandler = constraintLimitHandler;
  }

  /**
   * @return the snmpConfig
   */
  public String getSnmpConfig() {
    return snmpConfig;
  }

  /**
   * @param snmpConfig the snmpConfig to set
   */
  public void setSnmpConfig(final String snmpConfig) {
    this.snmpConfig = snmpConfig;
  }

  /**
   * @return the agentSnmp
   */
  public WaarpSnmpAgent getAgentSnmp() {
    return agentSnmp;
  }

  /**
   * @param agentSnmp the agentSnmp to set
   */
  public void setAgentSnmp(final WaarpSnmpAgent agentSnmp) {
    this.agentSnmp = agentSnmp;
  }

  /**
   * @return the ftpMib
   */
  public FtpPrivateMib getFtpMib() {
    return ftpMib;
  }

  /**
   * @param ftpMib the ftpMib to set
   */
  public void setFtpMib(final FtpPrivateMib ftpMib) {
    this.ftpMib = ftpMib;
  }

  /**
   * @return the monitoring
   */
  public FtpMonitoring getMonitoring() {
    return monitoring;
  }

  /**
   * @param monitoring the monitoring to set
   */
  public void setMonitoring(final FtpMonitoring monitoring) {
    this.monitoring = monitoring;
  }
}
