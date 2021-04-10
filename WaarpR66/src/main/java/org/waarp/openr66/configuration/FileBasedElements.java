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

package org.waarp.openr66.configuration;

import org.waarp.common.xml.XmlDecl;
import org.waarp.common.xml.XmlType;
import org.waarp.openr66.database.data.DbHostConfiguration;

public class FileBasedElements {
  /**
   * Authentication Fields
   */
  public static final String XML_AUTHENTIFICATION_ROOT = "authent";
  /**
   * Authentication Fields
   */
  public static final String XML_AUTHENTIFICATION_ENTRY = "entry";
  /**
   * Authentication Fields
   */
  public static final String XML_AUTHENTIFICATION_HOSTID = "hostid";
  /**
   * Authentication Fields
   */
  public static final String XML_AUTHENTIFICATION_KEYFILE = "keyfile";
  /**
   * Authentication Fields
   */
  public static final String XML_AUTHENTIFICATION_KEY = "key";
  /**
   * Authentication Fields
   */
  public static final String XML_AUTHENTIFICATION_ADMIN = "admin";
  /**
   * Authentication Fields
   */
  public static final String XML_AUTHENTIFICATION_ADDRESS = "address";
  /**
   * Authentication Fields
   */
  public static final String XML_AUTHENTIFICATION_PORT = "port";
  /**
   * Authentication Fields
   */
  public static final String XML_AUTHENTIFICATION_ISSSL = "isssl";
  /**
   * Authentication Fields
   */
  public static final String XML_AUTHENTIFICATION_ISCLIENT = "isclient";
  /**
   * Authentication Fields
   */
  public static final String XML_AUTHENTIFICATION_ISACTIVE = "isactive";
  /**
   * Authentication Fields
   */
  public static final String XML_AUTHENTIFICATION_ISPROXIFIED = "isproxified";
  /**
   * Authentication Fields
   */
  public static final String XML_AUTHENTIFICATION_BASED =
      '/' + XML_AUTHENTIFICATION_ROOT + '/' + XML_AUTHENTIFICATION_ENTRY;
  /**
   * Structure of the Configuration file
   */
  private static final XmlDecl[] configAuthenticationDecls = {
      // identity
      new XmlDecl(XmlType.STRING, XML_AUTHENTIFICATION_HOSTID),
      new XmlDecl(XmlType.STRING, XML_AUTHENTIFICATION_KEYFILE),
      new XmlDecl(XmlType.STRING, XML_AUTHENTIFICATION_KEY),
      new XmlDecl(XmlType.BOOLEAN, XML_AUTHENTIFICATION_ADMIN),
      new XmlDecl(XmlType.STRING, XML_AUTHENTIFICATION_ADDRESS),
      new XmlDecl(XmlType.INTEGER, XML_AUTHENTIFICATION_PORT),
      new XmlDecl(XmlType.BOOLEAN, XML_AUTHENTIFICATION_ISSSL),
      new XmlDecl(XmlType.BOOLEAN, XML_AUTHENTIFICATION_ISCLIENT),
      new XmlDecl(XmlType.BOOLEAN, XML_AUTHENTIFICATION_ISACTIVE),
      new XmlDecl(XmlType.BOOLEAN, XML_AUTHENTIFICATION_ISPROXIFIED)
  };
  /**
   * Global Structure for Server Configuration
   */
  public static final XmlDecl[] authentElements = {
      new XmlDecl(XML_AUTHENTIFICATION_ENTRY, XmlType.XVAL,
                  XML_AUTHENTIFICATION_BASED, configAuthenticationDecls, true)
  };
  /**
   * XML_LOCALE
   */
  public static final String XML_LOCALE = "locale";
  /**
   * SERVER HOSTID
   */
  public static final String XML_SERVER_HOSTID = "hostid";
  /**
   * SERVER SSL HOSTID
   */
  public static final String XML_SERVER_SSLHOSTID = "sslhostid";
  /**
   * ADMINISTRATOR SERVER NAME (shutdown)
   */
  public static final String XML_SERVER_ADMIN = "serveradmin";
  /**
   * SERVER PASSWORD (shutdown)
   */
  public static final String XML_SERVER_PASSWD = "serverpasswd";//NOSONAR
  /**
   * SERVER PASSWORD FILE (shutdown)
   */
  public static final String XML_SERVER_PASSWD_FILE =//NOSONAR
      "serverpasswdfile";//NOSONAR
  /**
   * Authentication
   */
  public static final String XML_AUTHENTIFICATION_FILE = "authentfile";
  /**
   * SERVER PORT
   */
  public static final String XML_SERVER_PORT = "serverport";
  /**
   * SERVER SSL PORT
   */
  public static final String XML_SERVER_SSLPORT = "serversslport";
  /**
   * SERVER HTTP PORT
   */
  public static final String XML_SERVER_HTTPPORT = "serverhttpport";
  /**
   * SERVER HTTPS PORT
   */
  public static final String XML_SERVER_HTTPSPORT = "serverhttpsport";
  /**
   * SERVER PORT
   */
  public static final String XML_SERVER_ADDRESSES = "serveraddresses";
  /**
   * SERVER SSL PORT
   */
  public static final String XML_SERVER_SSL_ADDRESSES = "serverssladdresses";
  /**
   * SERVER HTTP PORT
   */
  public static final String XML_SERVER_HTTP_ADDRESSES = "serverhttpaddresses";
  /**
   * SERVER HTTPS PORT
   */
  public static final String XML_SERVER_HTTPS_ADDRESSES =
      "serverhttpsaddresses";
  /**
   * Structure of the Configuration file
   */
  private static final XmlDecl[] configNetworkServerDecls = {
      // network
      new XmlDecl(XmlType.INTEGER, XML_SERVER_PORT),
      new XmlDecl(XmlType.INTEGER, XML_SERVER_SSLPORT),
      new XmlDecl(XmlType.INTEGER, XML_SERVER_HTTPPORT),
      new XmlDecl(XmlType.INTEGER, XML_SERVER_HTTPSPORT),
      new XmlDecl(XmlType.STRING, XML_SERVER_ADDRESSES),
      new XmlDecl(XmlType.STRING, XML_SERVER_SSL_ADDRESSES),
      new XmlDecl(XmlType.STRING, XML_SERVER_HTTP_ADDRESSES),
      new XmlDecl(XmlType.STRING, XML_SERVER_HTTPS_ADDRESSES)
  };
  /**
   * SERVER SSL STOREKEY PATH
   */
  public static final String XML_PATH_KEYPATH = "keypath";
  /**
   * SERVER SSL KEY PASS
   */
  public static final String XML_PATH_KEYPASS = "keypass";
  /**
   * SERVER SSL STOREKEY PASS
   */
  public static final String XML_PATH_KEYSTOREPASS = "keystorepass";
  /**
   * SERVER SSL TRUSTSTOREKEY PATH
   */
  public static final String XML_PATH_TRUSTKEYPATH = "trustkeypath";
  /**
   * SERVER SSL TRUSTSTOREKEY PASS
   */
  public static final String XML_PATH_TRUSTKEYSTOREPASS = "trustkeystorepass";
  /**
   * SERVER SSL STOREKEY PATH ADMIN
   */
  public static final String XML_PATH_ADMIN_KEYPATH = "admkeypath";
  /**
   * SERVER SSL KEY PASS ADMIN
   */
  public static final String XML_PATH_ADMIN_KEYPASS = "admkeypass";
  /**
   * SERVER SSL STOREKEY PASS ADMIN
   */
  public static final String XML_PATH_ADMIN_KEYSTOREPASS = "admkeystorepass";
  /**
   * SERVER CRYPTO for Password
   */
  public static final String XML_PATH_CRYPTOKEY = "cryptokey";
  /**
   * Structure of the Configuration file
   */
  public static final XmlDecl[] configIdentityDecls = {
      // identity
      new XmlDecl(XmlType.STRING, XML_SERVER_HOSTID),
      new XmlDecl(XmlType.STRING, XML_SERVER_SSLHOSTID),
      new XmlDecl(XmlType.STRING, XML_PATH_CRYPTOKEY),
      new XmlDecl(XmlType.STRING, XML_AUTHENTIFICATION_FILE),
      new XmlDecl(XmlType.STRING, XML_LOCALE)
  };
  /**
   * Base Directory
   */
  public static final String XML_SERVER_HOME = "serverhome";
  /**
   * IN Directory
   */
  public static final String XML_INPATH = "in";
  /**
   * OUT Directory
   */
  public static final String XML_OUTPATH = "out";
  /**
   * ARCHIVE Directory
   */
  public static final String XML_ARCHIVEPATH = "arch";
  /**
   * WORKING Directory
   */
  public static final String XML_WORKINGPATH = "work";
  /**
   * CONFIG Directory
   */
  public static final String XML_CONFIGPATH = "conf";
  /**
   * Structure of the Configuration file
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
  /**
   * HTTP Admin Directory
   */
  public static final String XML_HTTPADMINPATH = "httpadmin";
  /**
   * HTTP Admin model (fix = 0 or responsive = 1, not mandatory since auto
   * detection)
   */
  public static final String XML_HTTPADMINMODEL = "httpmodel";
  /**
   * Use SSL for R66 connection
   */
  public static final String XML_USESSL = "usessl";
  /**
   * Use non SSL for R66 connection
   */
  public static final String XML_USENOSSL = "usenossl";
  /**
   * Use HTTP compression for R66 HTTP connection
   */
  public static final String XML_USEHTTPCOMP = "usehttpcomp";
  /**
   * SERVER SSL Use TrustStore for Client Authentication
   */
  public static final String XML_USECLIENT_AUTHENT =
      "trustuseclientauthenticate";
  /**
   * Structure of the Configuration file
   */
  public static final XmlDecl[] configSslDecls = {
      // ssl
      new XmlDecl(XmlType.STRING, XML_PATH_KEYPATH),
      new XmlDecl(XmlType.STRING, XML_PATH_KEYSTOREPASS),
      new XmlDecl(XmlType.STRING, XML_PATH_KEYPASS),
      new XmlDecl(XmlType.STRING, XML_PATH_TRUSTKEYPATH),
      new XmlDecl(XmlType.STRING, XML_PATH_TRUSTKEYSTOREPASS),
      new XmlDecl(XmlType.BOOLEAN, XML_USECLIENT_AUTHENT)
  };

  /**
   * Limit per session
   */
  public static final String XML_LIMITSESSION = "sessionlimit";
  /**
   * Limit global
   */
  public static final String XML_LIMITGLOBAL = "globallimit";
  /**
   * Delay between two checks for Limit
   */
  public static final String XML_LIMITDELAY = "delaylimit";
  /**
   * Monitoring: how long in ms to get back in monitoring
   */
  public static final String XML_MONITOR_PASTLIMIT = "pastlimit";
  /**
   * Monitoring: minimal interval in ms before redo real monitoring
   */
  public static final String XML_MONITOR_MINIMALDELAY = "minimaldelay";
  /**
   * Monitoring: snmp configuration file (if empty, no snmp support)
   */
  public static final String XML_MONITOR_SNMP_CONFIG = "snmpconfig";
  /**
   * In case of multiple OpenR66 Monitors behing a loadbalancer (ha config)
   */
  public static final String XML_MULTIPLE_MONITORS = "multiplemonitors";
  /**
   * If you need a special Business Factory, you must specify the full class
   * name here. Default is:
   * org.waarp.openr66.context.R66DefaultBusinessFactory which only logs in
   * DEBUG mode.
   */
  public static final String XML_BUSINESS_FACTORY = "businessfactory";
  /**
   * URL as http://myrest.org for PUSH REST JSON Monitor
   */
  public static final String XML_PUSH_MONITOR_URL = "pushmonitorurl";
  /**
   * End Point as /status such as URL + end point give
   * http://myrest.org/status for PUSH REST JSON Monitor
   */
  public static final String XML_PUSH_MONITOR_ENDPOINT = "pushmonitorendpoint";
  /**
   * Delay in ms between 2 attempts for PUSH REST JSON Monitor
   */
  public static final String XML_PUSH_MONITOR_DELAY = "pushmonitordelay";
  /**
   * Keep connection between 2 attempts for PUSH REST JSON Monitor
   */
  public static final String XML_PUSH_MONITOR_KEEP_CONNECTION =
      "pushmonitorkeepconnection";
  /**
   * Keep Monitor Interval Included
   */
  public static final String XML_PUSH_MONITOR_INTERVAL_INCLUDED =
          "pushmonitorintervalincluded";
  /**
   * Keep monitorTransformLongAsString
   */
  public static final String XML_PUSH_MONITOR_TRANSFORM_LONG_AS_STRING =
          "pushmonitortransformlongasstring";

  /**
   * Usage of CPU Limit
   */
  public static final String XML_CSTRT_USECPULIMIT = "usecpulimit";
  /**
   * Usage of JDK CPU Limit (True) or SysMon CPU Limit
   */
  public static final String XML_CSTRT_USECPUJDKLIMIT = "usejdkcpulimit";
  /**
   * CPU LIMIT between 0 and 1, where 1 stands for no limit
   */
  public static final String XML_CSTRT_CPULIMIT = "cpulimit";
  /**
   * Connection limit where 0 stands for no limit
   */
  public static final String XML_CSTRT_CONNLIMIT = "connlimit";
  /**
   * CPU LOW limit to apply increase of throttle
   */
  public static final String XML_CSTRT_LOWCPULIMIT = "lowcpulimit";
  /**
   * CPU HIGH limit to apply decrease of throttle, 0 meaning no throttle
   * activated
   */
  public static final String XML_CSTRT_HIGHCPULIMIT = "highcpulimit";
  /**
   * PERCENTAGE DECREASE of Bandwidth
   */
  public static final String XML_CSTRT_PERCENTDECREASE = "percentdecrease";
  /**
   * Delay between 2 checks of throttle test
   */
  public static final String XML_CSTRT_DELAYTHROTTLE = "delaythrottle";
  /**
   * Bandwidth low limit to not got below
   */
  public static final String XML_CSTRT_LIMITLOWBANDWIDTH = "limitlowbandwidth";
  /**
   * Usage of checking remote address with the DbHost definition
   */
  public static final String XML_CHECK_ADDRESS = "checkaddress";
  /**
   * Usage of checking remote address also for Client
   */
  public static final String XML_CHECK_CLIENTADDRESS = "checkclientaddress";
  /**
   * In case of No Db Client, Usage of saving TaskRunner into independent XML
   * file
   */
  public static final String XML_SAVE_TASKRUNNERNODB = "taskrunnernodb";
  /**
   * Structure of the Configuration file
   */
  private static final XmlDecl[] configClientParamDecls = {
      // client
      new XmlDecl(XmlType.BOOLEAN, XML_SAVE_TASKRUNNERNODB),
      new XmlDecl(XmlType.STRING, XML_BUSINESS_FACTORY)
  };
  /**
   * Use external Waarp Local Exec for ExecTask and ExecMoveTask
   */
  public static final String XML_USELOCALEXEC = "uselocalexec";
  /**
   * Address of Waarp Local Exec for ExecTask and ExecMoveTask
   */
  public static final String XML_LEXECADDR = "lexecaddr";
  /**
   * Port of Waarp Local Exec for ExecTask and ExecMoveTask
   */
  public static final String XML_LEXECPORT = "lexecport";
  /**
   * Structure of the Configuration file
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
      new XmlDecl(XmlType.STRING, XML_BUSINESS_FACTORY),
      new XmlDecl(XmlType.STRING, XML_PUSH_MONITOR_URL),
      new XmlDecl(XmlType.STRING, XML_PUSH_MONITOR_ENDPOINT),
      new XmlDecl(XmlType.INTEGER, XML_PUSH_MONITOR_DELAY),
      new XmlDecl(XmlType.BOOLEAN, XML_PUSH_MONITOR_KEEP_CONNECTION),
      new XmlDecl(XmlType.BOOLEAN, XML_PUSH_MONITOR_INTERVAL_INCLUDED),
      new XmlDecl(XmlType.BOOLEAN, XML_PUSH_MONITOR_TRANSFORM_LONG_AS_STRING)
  };
  /**
   * Default number of threads in pool for Server.
   */
  public static final String XML_SERVER_THREAD = "serverthread";
  /**
   * Default number of threads in pool for Client (truly concurrent).
   */
  public static final String XML_CLIENT_THREAD = "clientthread";
  /**
   * Memory Limit to use.
   */
  public static final String XML_MEMORY_LIMIT = "memorylimit";
  /**
   * Limit of number of active Runner from Commander
   */
  public static final String XML_LIMITRUNNING = "runlimit";
  /**
   * Delay between two checks for Commander
   */
  public static final String XML_DELAYCOMMANDER = "delaycommand";
  /**
   * Delay between two retry after bad connection
   */
  public static final String XML_DELAYRETRY = "delayretry";
  /**
   * Nb of milliseconds after connection is in timeout
   */
  public static final String XML_TIMEOUTCON = "timeoutcon";
  /**
   * Should a file MD5 SHA1 be computed using NIO (not recommended anymore)
   */
  public static final String XML_USENIO = "usenio";
  /**
   * What Digest to use: CRC32=0, ADLER32=1, MD5=2, MD2=3, SHA1=4, SHA256=5,
   * SHA384=6, SHA512=7 : recommended value is 7
   */
  public static final String XML_DIGEST = "digest";
  /**
   * Should a file MD5 be computed using FastMD5 (not recommended anymore)
   */
  public static final String XML_USEFASTMD5 = "usefastmd5";
  /**
   * If using Fast MD5, should we used the binary JNI library, empty meaning
   * no
   */
  public static final String XML_FASTMD5 = "fastmd5";
  /**
   * number of rank to go back when a transfer is restarted. restart is
   * gaprestart*blocksize
   */
  public static final String XML_GAPRESTART = "gaprestart";
  /**
   * Size by default of block size for receive/sending files. Should be a
   * multiple of 8192 (maximum = 64K due to
   * block limitation to 2 bytes)
   */
  public static final String XML_BLOCKSIZE = "blocksize";
  /**
   * Structure of the Configuration file
   */
  private static final XmlDecl[] configSubmitLimitDecls = {
      // limit
      new XmlDecl(XmlType.INTEGER, XML_BLOCKSIZE)
  };
  /**
   * If set to <=0, will not use Thrift support, if set >0 (preferably > 1024)
   * will enable Thrift support on the
   * TCP port specified by this number
   */
  public static final String XML_USETHRIFT = "usethrift";
  /**
   * Database Driver as of oracle, mysql, postgresql, h2
   */
  public static final String XML_DBDRIVER = "dbdriver";
  /**
   * Database Server connection string as of jdbc:type://[host:port],[failoverhost:port]
   * .../[database][?propertyName1][ =propertyValue1][&propertyName2][=propertyValue2]...
   */
  public static final String XML_DBSERVER = "dbserver";
  /**
   * Database User
   */
  public static final String XML_DBUSER = "dbuser";
  /**
   * Database Password
   */
  public static final String XML_DBPASSWD = "dbpasswd";//NOSONAR
  /**
   * Database Checking
   */
  @Deprecated
  public static final String XML_DBCHECK = "dbcheck";
  /**
   * Upgrade database
   */
  public static final String XML_DBAUTOUPGRADE = "autoUpgrade";
  /**
   * Structure of the Configuration file
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
   * Check version in protocol
   */
  public static final String XML_CHECKVERSION = "checkversion";
  /**
   * Global digest by transfer enable
   */
  public static final String XML_GLOBALDIGEST = "globaldigest";
  /**
   * Final local digest check by transfer enable
   */
  public static final String XML_LOCALDIGEST = "localdigest";
  /**
   * Structure of the Configuration file
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
      new XmlDecl(XmlType.BOOLEAN, XML_GLOBALDIGEST),
      new XmlDecl(XmlType.BOOLEAN, XML_LOCALDIGEST)
  };
  /**
   * SERVER REST interface SHA address usage (and not all available IPs)
   */
  public static final String XML_REST_ADDRESS = "restaddress";
  /**
   * SERVER HTTP(S) PORT for REST interface
   */
  public static final String XML_SERVER_REST_PORT = "restport";
  /**
   * SERVER REST interface using SSL
   */
  public static final String XML_REST_SSL = "restssl";
  /**
   * SERVER REST interface using time limit
   */
  public static final String XML_REST_TIME_LIMIT = "resttimelimit";
  /**
   * SERVER REST interface using authentication
   */
  public static final String XML_REST_AUTHENTICATED = "restauthenticated";
  /**
   * SERVER REST interface SHA Key for request checking
   */
  public static final String XML_REST_AUTH_KEY = "restsigkey";
  /**
   * SERVER REST interface signature usage (auth key usage)
   */
  public static final String XML_REST_SIGNATURE = "restsignature";
  /**
   * SERVER REST interface method
   */
  public static final String XML_REST_METHOD = "restmethod";
  /**
   * SERVER REST interface method
   */
  public static final String XML_REST_METHOD_NAME = "restname";
  /**
   * SERVER REST interface CRUD per method
   */
  public static final String XML_REST_CRUD = "restcrud";
  public static final XmlDecl[] configRestMethodDecls = {
      // Rest Method
      new XmlDecl(XmlType.STRING, XML_REST_METHOD_NAME),
      new XmlDecl(XmlType.STRING, XML_REST_CRUD)
  };
  /**
   * Structure of the Configuration file
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
      new XmlDecl(XML_REST_METHOD, XmlType.XVAL, XML_REST_METHOD,
                  configRestMethodDecls, true)
  };
  /**
   * Overall structure of the Configuration file
   */
  public static final String XML_ROOT = "/config/";
  public static final String XML_IDENTITY = "identity";
  public static final String XML_SERVER = "server";
  public static final String XML_CLIENT = "client";
  public static final String XML_DIRECTORY = "directory";
  public static final String XML_LIMIT = "limit";
  public static final String XML_NETWORK = "network";
  public static final String XML_SSL = "ssl";
  public static final String XML_DB = "db";
  /**
   * Global Structure for Submit only Client Configuration
   */
  public static final XmlDecl[] configSubmitClient = {
      new XmlDecl(XML_IDENTITY, XmlType.XVAL, XML_ROOT + XML_IDENTITY,
                  configIdentityDecls, false),
      new XmlDecl(XML_DIRECTORY, XmlType.XVAL, XML_ROOT + XML_DIRECTORY,
                  configDirectoryDecls, false),
      new XmlDecl(XML_LIMIT, XmlType.XVAL, XML_ROOT + XML_LIMIT,
                  configSubmitLimitDecls, false),
      new XmlDecl(XML_DB, XmlType.XVAL, XML_ROOT + XML_DB, configDbDecls,
                  false),
      new XmlDecl(DbHostConfiguration.XML_ALIASES, XmlType.XVAL,
                  XML_ROOT + DbHostConfiguration.XML_ALIASES + '/' +
                  DbHostConfiguration.XML_ALIAS,
                  DbHostConfiguration.configAliasDecls, true)
  };
  public static final String XML_REST = "rest";
  /**
   * Global Structure for Client Configuration
   */
  public static final XmlDecl[] configClient = {
      new XmlDecl(XML_IDENTITY, XmlType.XVAL, XML_ROOT + XML_IDENTITY,
                  configIdentityDecls, false),
      new XmlDecl(XML_CLIENT, XmlType.XVAL, XML_ROOT + XML_CLIENT,
                  configClientParamDecls, false),
      new XmlDecl(XML_SSL, XmlType.XVAL, XML_ROOT + XML_SSL, configSslDecls,
                  false),
      new XmlDecl(XML_DIRECTORY, XmlType.XVAL, XML_ROOT + XML_DIRECTORY,
                  configDirectoryDecls, false),
      new XmlDecl(XML_LIMIT, XmlType.XVAL, XML_ROOT + XML_LIMIT,
                  configLimitDecls, false),
      new XmlDecl(XML_REST, XmlType.XVAL, XML_ROOT + XML_REST, configRestDecls,
                  false),
      new XmlDecl(XML_DB, XmlType.XVAL, XML_ROOT + XML_DB, configDbDecls,
                  false),
      new XmlDecl(DbHostConfiguration.XML_BUSINESS, XmlType.STRING,
                  XML_ROOT + DbHostConfiguration.XML_BUSINESS + '/' +
                  DbHostConfiguration.XML_BUSINESSID, true),
      new XmlDecl(DbHostConfiguration.XML_ALIASES, XmlType.XVAL,
                  XML_ROOT + DbHostConfiguration.XML_ALIASES + '/' +
                  DbHostConfiguration.XML_ALIAS,
                  DbHostConfiguration.configAliasDecls, true)
  };
  /**
   * Global Structure for Server Configuration
   */
  public static final XmlDecl[] configServer = {
      new XmlDecl(XML_IDENTITY, XmlType.XVAL, XML_ROOT + XML_IDENTITY,
                  configIdentityDecls, false),
      new XmlDecl(XML_SERVER, XmlType.XVAL, XML_ROOT + XML_SERVER,
                  configServerParamDecls, false),
      new XmlDecl(XML_NETWORK, XmlType.XVAL, XML_ROOT + XML_NETWORK,
                  configNetworkServerDecls, false),
      new XmlDecl(XML_SSL, XmlType.XVAL, XML_ROOT + XML_SSL, configSslDecls,
                  false),
      new XmlDecl(XML_DIRECTORY, XmlType.XVAL, XML_ROOT + XML_DIRECTORY,
                  configDirectoryDecls, false),
      new XmlDecl(XML_LIMIT, XmlType.XVAL, XML_ROOT + XML_LIMIT,
                  configLimitDecls, false),
      new XmlDecl(XML_REST, XmlType.XVAL, XML_ROOT + XML_REST, configRestDecls,
                  true),
      new XmlDecl(XML_DB, XmlType.XVAL, XML_ROOT + XML_DB, configDbDecls,
                  false),
      new XmlDecl(DbHostConfiguration.XML_BUSINESS, XmlType.STRING,
                  XML_ROOT + DbHostConfiguration.XML_BUSINESS + '/' +
                  DbHostConfiguration.XML_BUSINESSID, true),
      new XmlDecl(DbHostConfiguration.XML_ROLES, XmlType.XVAL,
                  XML_ROOT + DbHostConfiguration.XML_ROLES + '/' +
                  DbHostConfiguration.XML_ROLE,
                  DbHostConfiguration.configRoleDecls, true),
      new XmlDecl(DbHostConfiguration.XML_ALIASES, XmlType.XVAL,
                  XML_ROOT + DbHostConfiguration.XML_ALIASES + '/' +
                  DbHostConfiguration.XML_ALIAS,
                  DbHostConfiguration.configAliasDecls, true)
  };

}