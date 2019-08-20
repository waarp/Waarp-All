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

import io.netty.handler.traffic.AbstractTrafficShapingHandler;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.waarp.common.cpu.WaarpConstraintLimitHandler;
import org.waarp.common.crypto.Des;
import org.waarp.common.crypto.ssl.WaarpSecureKeyStore;
import org.waarp.common.crypto.ssl.WaarpSslContextFactory;
import org.waarp.common.database.ConnectionFactory;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.DbRequest;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.database.model.DbType;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.exception.CryptoException;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.file.AbstractDir;
import org.waarp.common.file.DirInterface;
import org.waarp.common.file.FileUtils;
import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.role.RoleDefault;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.common.xml.XmlHash;
import org.waarp.common.xml.XmlRootHash;
import org.waarp.common.xml.XmlUtil;
import org.waarp.common.xml.XmlValue;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.openr66.context.R66BusinessFactoryInterface;
import org.waarp.openr66.context.authentication.R66Auth;
import org.waarp.openr66.context.task.localexec.LocalExecClient;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.database.data.DbConfiguration;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbHostConfiguration;
import org.waarp.openr66.database.model.DbModelFactoryR66;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.configuration.R66SystemProperties;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.http.adminssl.HttpResponsiveSslHandler;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler.RESTHANDLERS;
import org.waarp.openr66.protocol.networkhandler.R66ConstraintLimitHandler;
import org.waarp.openr66.protocol.networkhandler.ssl.NetworkSslServerInitializer;
import org.waarp.openr66.server.ServerInitDatabase;
import org.waarp.snmp.SnmpConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.waarp.common.database.DbConstant.*;
import static org.waarp.openr66.configuration.FileBasedElements.*;

/**
 * File Based Configuration
 */
public class FileBasedConfiguration {
  private static final String CANNOT_FIND_SSL_AUTHENTICATION_FOR_CURRENT_HOST =
      "Cannot find SSL Authentication for current host";

  private static final String CANNOT_FIND_AUTHENTICATION_FOR_CURRENT_HOST =
      "Cannot find Authentication for current host";

  private static final String IS_DEPRECATED_IN_SYSTEM_PROPERTIES_USE_INSTEAD =
      "{} is deprecated in system properties use {} instead";

  private static final String CANNOT_LOAD_AUTHENTICATION_CONFIGURATION =
      "Cannot load Authentication configuration";

  private static final String CANNOT_LOAD_LIMIT_CONFIGURATION =
      "Cannot load Limit configuration";

  private static final String CANNOT_LOAD_DIRECTORY_CONFIGURATION =
      "Cannot load Directory configuration";

  private static final String CANNOT_LOAD_DATABASE_CONFIGURATION =
      "Cannot load Database configuration";

  private static final String CANNOT_LOAD_IDENTITY = "Cannot load Identity";

  private static final String FILE_BASED_CONFIGURATION_CANNOT_READ_XML =
      "FileBasedConfiguration.CannotReadXml";

  private static final String FILE_BASED_CONFIGURATION_NO_SET_CONFIG =
      "FileBasedConfiguration.NoSetConfig";

  private static final String FILE_BASED_CONFIGURATION_NOT_FOUND_CONFIG =
      "FileBasedConfiguration.NotFoundConfig";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FileBasedConfiguration.class);

  private static XmlValue[] configuration;

  private static XmlRootHash hashRootConfig;

  protected FileBasedConfiguration() {
  }

  public static void setXmlRootHash(XmlRootHash xmlRootHash) {
    hashRootConfig = xmlRootHash;
  }

  /**
   * Load the locale from configuration file
   *
   * @param hashConfig
   */
  private static void loadLocale(XmlHash hashConfig) {
    final XmlValue value = hashConfig.get(XML_LOCALE);
    if (value != null && !value.isEmpty()) {
      final String locale = value.getString();
      if (locale == null || locale.isEmpty()) {
        return;
      }
      Messages.init(new Locale(locale));
    }
  }

  /**
   * @param config
   * @param hashRootConfig
   *
   * @return True if the identity of the server is correctly loaded
   */
  public static boolean loadIdentity(Configuration config,
                                     XmlRootHash hashRootConfig) {
    XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_IDENTITY));
    try {
      loadLocale(hashConfig);
      XmlValue value = hashConfig.get(XML_SERVER_HOSTID);
      if (value != null && !value.isEmpty()) {
        config.setHostId(value.getString());
      } else {
        logger.error(
            Messages.getString(FILE_BASED_CONFIGURATION_NOT_FOUND_CONFIG) +
            "Host ID"); //$NON-NLS-1$
        return false;
      }
      value = hashConfig.get(XML_SERVER_SSLHOSTID);
      if (value != null && !value.isEmpty()) {
        config.setHostSslId(value.getString());
      } else {
        logger.warn(Messages.getString(
            "FileBasedConfiguration.SSLIDNotFound")); //$NON-NLS-1$
        config.setUseSSL(false);
        config.setHostSslId(null);
      }
      value = hashConfig.get(XML_AUTHENTIFICATION_FILE);
      if (value != null && !value.isEmpty()) {
        config.setAuthFile(value.getString());
      }
      return setCryptoKey(config, hashConfig);
    } finally {
      hashConfig.clear();
      hashConfig = null;
    }
  }

  /**
   * @param config
   *
   * @return True if the authentication of partners is correctly loaded
   */
  private static boolean loadAuthentication(Configuration config) {
    XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_IDENTITY));
    try {
      if (config.isSaveTaskRunnerWithNoDb()) {
        // if no database, must load authentication from file
        final XmlValue value = hashConfig.get(XML_AUTHENTIFICATION_FILE);
        if (value != null && !value.isEmpty()) {
          final String fileauthent = value.getString();
          return AuthenticationFileBasedConfiguration
              .loadAuthentication(config, fileauthent);
        } else {
          logger.warn(
              Messages.getString(FILE_BASED_CONFIGURATION_NOT_FOUND_CONFIG) +
              "Authentication file"); //$NON-NLS-1$
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
   * @param config
   *
   * @return True if the server parameters are correctly loaded
   */
  private static boolean loadServerParam(Configuration config) {
    XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_SERVER));
    try {
      if (!loadServerConfig(config, hashConfig)) {
        return false;
      }
      XmlValue value;
      value = hashConfig.get(XML_USELOCALEXEC);
      if (value != null && !value.isEmpty()) {
        config.setUseLocalExec(value.getBoolean());
        if (config.isUseLocalExec()) {
          value = hashConfig.get(XML_LEXECADDR);
          String saddr;
          InetAddress addr;
          if (value != null && !value.isEmpty()) {
            saddr = value.getString();
            try {
              addr = InetAddress.getByName(saddr);
            } catch (final UnknownHostException e) {
              logger.error(Messages.getString(
                  FILE_BASED_CONFIGURATION_NOT_FOUND_CONFIG) +
                           "LocalExec Address"); //$NON-NLS-1$
              return false;
            }
          } else {
            logger.warn(
                Messages.getString(FILE_BASED_CONFIGURATION_NOT_FOUND_CONFIG) +
                "LocalExec Address"); //$NON-NLS-1$
            try {
              addr = InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 });
            } catch (final UnknownHostException e) {
              logger.error(Messages.getString(
                  FILE_BASED_CONFIGURATION_NOT_FOUND_CONFIG) +
                           "LocalExec Address"); //$NON-NLS-1$
              return false;
            }
          }
          value = hashConfig.get(XML_LEXECPORT);
          int port;
          if (value != null && !value.isEmpty()) {
            port = value.getInteger();
          } else {
            port = 9999;
          }
          LocalExecClient.address = new InetSocketAddress(addr, port);
        }
      }
      value = hashConfig.get(XML_CHECK_ADDRESS);
      if (value != null && !value.isEmpty()) {
        config.setCheckRemoteAddress(value.getBoolean());
      }
      value = hashConfig.get(XML_CHECK_CLIENTADDRESS);
      if (value != null && !value.isEmpty()) {
        config.setCheckClientAddress(value.getBoolean());
      }
      value = hashConfig.get(XML_MULTIPLE_MONITORS);
      if (value != null && !value.isEmpty()) {
        config.setMultipleMonitors(value.getInteger());
        if (config.getMultipleMonitors() > 1) {
          logger.warn(Messages.getString("FileBasedConfiguration.MMOn")
                      //$NON-NLS-1$
                      + config.getMultipleMonitors() + Messages.getString(
              "FileBasedConfiguration.MMOn2")); //$NON-NLS-1$
        } else {
          config.setMultipleMonitors(1);
          if (config.isWarnOnStartup()) {
            logger.warn(Messages.getString(
                "FileBasedConfiguration.MMOff")); //$NON-NLS-1$
          } else {
            logger.info(Messages.getString(
                "FileBasedConfiguration.MMOff")); //$NON-NLS-1$
          }
        }
      } else {
        config.setMultipleMonitors(1);
        if (config.isWarnOnStartup()) {
          logger.warn(
              Messages.getString("FileBasedConfiguration.MMOff")); //$NON-NLS-1$
        } else {
          logger.info(
              Messages.getString("FileBasedConfiguration.MMOff")); //$NON-NLS-1$
        }
      }
      value = hashConfig.get(XML_BUSINESS_FACTORY);
      if (value != null && !value.isEmpty()) {
        try {
          ParametersChecker.checkSanityString(value.getString());
        } catch (InvalidArgumentException e) {
          logger.error("Bad Business Factory class", e);
          return false;
        }
        try {
          config.setR66BusinessFactory((R66BusinessFactoryInterface) Class
              .forName(value.getString())//NOSONAR
              .newInstance());//NOSONAR
        } catch (final Exception e) {
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

  public static boolean loadServerConfig(final Configuration config,
                                         final XmlHash hashConfig) {
    XmlValue value = hashConfig.get(XML_USESSL);
    if (value != null && !value.isEmpty()) {
      config.setUseSSL(value.getBoolean());
    }
    value = hashConfig.get(XML_USENOSSL);
    if (value != null && !value.isEmpty()) {
      config.setUseNOSSL(value.getBoolean());
    }
    value = hashConfig.get(XML_USEHTTPCOMP);
    if (value != null && !value.isEmpty()) {
      config.setUseHttpCompression(value.getBoolean());
    }
    value = hashConfig.get(XML_SERVER_ADMIN);
    if (value != null && !value.isEmpty()) {
      config.setAdminName(value.getString());
    } else {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_NOT_FOUND_CONFIG) +
          "Administrator name"); //$NON-NLS-1$
      return false;
    }
    if (config.getCryptoKey() == null) {
      XmlHash hashConfig2 = new XmlHash(hashRootConfig.get(XML_IDENTITY));
      try {
        if (!setCryptoKey(config, hashConfig2)) {
          logger.error(
              Messages.getString(FILE_BASED_CONFIGURATION_NOT_FOUND_CONFIG) +
              "Crypto Key"); //$NON-NLS-1$
          return false;
        }
      } finally {
        hashConfig2.clear();
        hashConfig2 = null;
      }
    }
    byte[] decodedByteKeys;
    value = hashConfig.get(XML_SERVER_PASSWD_FILE);
    if (value == null || value.isEmpty()) {
      String passwd;
      value = hashConfig.get(XML_SERVER_PASSWD);
      if (value != null && !value.isEmpty()) {
        passwd = value.getString();
      } else {
        logger.error(
            Messages.getString(FILE_BASED_CONFIGURATION_NOT_FOUND_CONFIG) +
            "Password"); //$NON-NLS-1$
        return false;
      }
      try {
        decodedByteKeys = config.getCryptoKey().decryptHexInBytes(passwd);
      } catch (final Exception e) {
        logger.error(
            "Unable to Decrypt Server Password in Config file from: " + passwd,
            e);
        return false;
      }
    } else {
      final String skey = value.getString();
      // load key from file
      config.setServerKeyFile(skey);
      final File key = new File(skey);
      if (!key.canRead()) {
        logger.error("Unable to read Password in Config file from " + skey);
        return false;
      }
      try {
        decodedByteKeys = config.getCryptoKey().decryptHexFile(key);
      } catch (final Exception e2) {
        logger.error(
            "Unable to Decrypt Server Password in Config file from: " + skey,
            e2);
        return false;
      }
    }
    config.setSERVERKEY(decodedByteKeys);
    value = hashConfig.get(XML_HTTPADMINPATH);
    if (value == null || value.isEmpty()) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_NOT_FOUND_CONFIG) +
          "Http Admin Base"); //$NON-NLS-1$
      return false;
    }
    final String path = value.getString();
    if (path == null || path.isEmpty()) {
      logger.error(Messages.getString(FILE_BASED_CONFIGURATION_NO_SET_CONFIG) +
                   "Http Admin Base"); //$NON-NLS-1$
      return false;
    }
    final File file = new File(path);
    if (!file.isDirectory()) {
      logger.error(Messages.getString("FileBasedConfiguration.NotDirectory") +
                   "Http Admin Base"); //$NON-NLS-1$
      return false;
    }
    try {
      config.setHttpBasePath(
          AbstractDir.normalizePath(file.getCanonicalPath()) +
          DirInterface.SEPARATOR);
    } catch (final IOException e1) {
      logger.error(Messages.getString(FILE_BASED_CONFIGURATION_NO_SET_CONFIG) +
                   "Http Admin Path"); //$NON-NLS-1$
      return false;
    }
    value = hashConfig.get(XML_HTTPADMINMODEL);
    // 0 = standard, 1 = responsive (preferred default)
    int model =
        !new File(file, HttpResponsiveSslHandler.LISTING_PAGE).isFile()? 0 : 1;
    if (value != null && !value.isEmpty()) {
      model = value.getInteger();
    }
    config.setHttpModel(model);

    // Key for HTTPS
    value = hashConfig.get(XML_PATH_ADMIN_KEYPATH);
    if (value != null && !value.isEmpty()) {
      final String keypath = value.getString();
      if (keypath == null || keypath.isEmpty()) {
        logger.error("Bad Key Path");
        return false;
      }
      value = hashConfig.get(XML_PATH_ADMIN_KEYSTOREPASS);
      if (value == null || value.isEmpty()) {
        logger.error("Unable to find: " + "KeyStore Passwd");
        return false;
      }
      final String keystorepass = value.getString();
      if (keystorepass == null || keystorepass.isEmpty()) {
        logger.error("Bad KeyStore Passwd");
        return false;
      }
      value = hashConfig.get(XML_PATH_ADMIN_KEYPASS);
      if (value == null || value.isEmpty()) {
        logger.error("Unable to find :" + "Key Passwd");
        return false;
      }
      final String keypass = value.getString();
      if (keypass == null || keypass.isEmpty()) {
        logger.error("Bad Key Passwd");
        return false;
      }
      try {
        Configuration.setWaarpSecureKeyStore(
            new WaarpSecureKeyStore(keypath, keystorepass, keypass));
      } catch (final CryptoException e) {
        logger.error("Bad SecureKeyStore construction for AdminSsl");
        return false;
      }
      // No client authentication
      Configuration.getWaarpSecureKeyStore().initEmptyTrustStore();
      Configuration.setWaarpSslContextFactory(
          new WaarpSslContextFactory(Configuration.getWaarpSecureKeyStore(),
                                     true));
    }
    value = hashConfig.get(XML_MONITOR_PASTLIMIT);
    if (value != null && !value.isEmpty()) {
      config.setPastLimit((value.getLong() / 10) * 10);
    }
    value = hashConfig.get(XML_MONITOR_MINIMALDELAY);
    if (value != null && !value.isEmpty()) {
      config.setMinimalDelay((value.getLong() / 10) * 10);
    }
    value = hashConfig.get(XML_MONITOR_SNMP_CONFIG);
    if (value != null && !value.isEmpty()) {
      config.setSnmpConfig(value.getString());
      final File snmpfile = new File(config.getSnmpConfig());
      if (snmpfile.canRead()) {
        if (!SnmpConfiguration.setConfigurationFromXml(snmpfile)) {
          config.setSnmpConfig(null);
        }
      } else {
        config.setSnmpConfig(null);
      }
    }
    return true;
  }

  /**
   * @param config
   *
   * @return True if the client parameters are correctly loaded
   */
  private static boolean loadClientParam(Configuration config) {
    XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_CLIENT));
    try {
      XmlValue value = hashConfig.get(XML_SAVE_TASKRUNNERNODB);
      if (admin == null || admin.getTypeDriver() == DbType.none) {
        if (value != null && !value.isEmpty()) {
          config.setSaveTaskRunnerWithNoDb(value.getBoolean());
          logger.info(
              Messages.getString("FileBasedConfiguration.NoDB")); //$NON-NLS-1$
          if (admin == null) {
            admin = new DbAdmin(); // no database support
            noCommitAdmin = admin;
          }
        }
      }
      value = hashConfig.get(XML_BUSINESS_FACTORY);
      if (value != null && !value.isEmpty()) {
        try {
          ParametersChecker.checkSanityString(value.getString());
        } catch (InvalidArgumentException e) {
          logger.error("Bad Business Factory class", e);
          return false;
        }
        try {
          config.setR66BusinessFactory((R66BusinessFactoryInterface) Class
              .forName(value.getString())//NOSONAR
              .newInstance());//NOSONAR
        } catch (final Exception e) {
          logger.error("Bad Business Factory class", e);
          return false;
        }
      }
      return true;
    } finally {
      hashConfig.clear();
    }
  }

  /**
   * @param config
   *
   * @return True if the directory parameters are correctly loaded
   */
  private static boolean loadDirectory(Configuration config) {
    XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_DIRECTORY));
    try {
      if (loadMinimalDirectory(config, hashConfig)) {
        return false;
      }
      try {
        config.setInPath(
            AbstractDir.normalizePath(getSubPath(config, XML_INPATH)));
      } catch (final OpenR66ProtocolSystemException e2) {
        logger.error(
            Messages.getString(FILE_BASED_CONFIGURATION_NO_SET_CONFIG) +
            "In"); //$NON-NLS-1$
        return false;
      }
      try {
        config.setOutPath(
            AbstractDir.normalizePath(getSubPath(config, XML_OUTPATH)));
      } catch (final OpenR66ProtocolSystemException e2) {
        logger.error(
            Messages.getString(FILE_BASED_CONFIGURATION_NO_SET_CONFIG) +
            "Out"); //$NON-NLS-1$
        return false;
      }
      try {
        config.setWorkingPath(
            AbstractDir.normalizePath(getSubPath(config, XML_WORKINGPATH)));
      } catch (final OpenR66ProtocolSystemException e2) {
        logger.error(
            Messages.getString(FILE_BASED_CONFIGURATION_NO_SET_CONFIG) +
            "Working"); //$NON-NLS-1$
        return false;
      }
      return true;
    } finally {
      hashConfig.clear();
      hashConfig = null;
    }
  }

  public static boolean loadMinimalDirectory(final Configuration config,
                                             final XmlHash hashConfig) {
    final XmlValue value = hashConfig.get(XML_SERVER_HOME);
    if (value == null || value.isEmpty()) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_NOT_FOUND_CONFIG) +
          "Home"); //$NON-NLS-1$
      return true;
    }
    final String path = value.getString();
    final File file = new File(path);
    if (!file.isDirectory()) {
      logger.error(Messages.getString("FileBasedConfiguration.NotDirectory") +
                   "Home"); //$NON-NLS-1$
      return true;
    }
    try {
      config
          .setBaseDirectory(AbstractDir.normalizePath(file.getCanonicalPath()));
    } catch (final IOException e1) {
      logger.error(Messages.getString(FILE_BASED_CONFIGURATION_NO_SET_CONFIG) +
                   "Home"); //$NON-NLS-1$
      return true;
    }
    try {
      config.setConfigPath(
          AbstractDir.normalizePath(getSubPath(config, XML_CONFIGPATH)));
    } catch (final OpenR66ProtocolSystemException e2) {
      logger.error(Messages.getString(FILE_BASED_CONFIGURATION_NO_SET_CONFIG) +
                   "Config"); //$NON-NLS-1$
      return true;
    }
    try {
      config.setArchivePath(
          AbstractDir.normalizePath(getSubPath(config, XML_ARCHIVEPATH)));
    } catch (final OpenR66ProtocolSystemException e2) {
      logger.error(Messages.getString(FILE_BASED_CONFIGURATION_NO_SET_CONFIG) +
                   "Archive"); //$NON-NLS-1$
      return true;
    }
    return false;
  }

  private static boolean alreadySetLimit;

  /**
   * @param config
   * @param updateLimit
   *
   * @return True if the limit configuration is correctly loaded
   */
  private static boolean loadLimit(Configuration config, boolean updateLimit) {
    if (alreadySetLimit) {
      return true;
    }
    XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_LIMIT));
    try {
      loadCommonLimit(config, hashConfig, updateLimit);
      XmlValue value;
      value = hashConfig.get(XML_LIMITRUNNING);
      if (value != null && !value.isEmpty()) {
        config.setRunnerThread(value.getInteger());
      }
      logger.info("Limit of Runner: {}", config.getRunnerThread());
      value = hashConfig.get(XML_DELAYCOMMANDER);
      if (value != null && !value.isEmpty()) {
        config.setDelayCommander((value.getLong() / 10) * 10);
        if (config.getDelayCommander() <= 100) {
          config.setDelayCommander(100);
        }
        logger.info("Delay Commander: {}", config.getDelayCommander());
      }
      value = hashConfig.get(XML_DIGEST);
      if (value != null && !value.isEmpty()) {
        try {
          int val = value.getInteger();
          if (val < 0 || val >= DigestAlgo.values().length) {
            val = 0;
          }
          config.setDigest(DigestAlgo.values()[val]);
        } catch (final IllegalArgumentException e) {
          // might be String
          final String val = value.getString();
          config.setDigest(PartnerConfiguration.getDigestAlgo(val));
        }
      }
      logger.info("DigestAlgo used: {}", config.getDigest());
      value = hashConfig.get(XML_USEFASTMD5);
      if (value != null && !value.isEmpty()) {
        FilesystemBasedDigest.setUseFastMd5(value.getBoolean());
      } else {
        FilesystemBasedDigest.setUseFastMd5(false);
      }
      value = hashConfig.get(XML_GAPRESTART);
      if (value != null && !value.isEmpty()) {
        Configuration.setRankRestart(value.getInteger());
        if (Configuration.getRankRestart() <= 0) {
          Configuration.setRankRestart(1);
        }
      }
      value = hashConfig.get(XML_BLOCKSIZE);
      if (value != null && !value.isEmpty()) {
        config.setBlockSize(value.getInteger());
      }
      value = hashConfig.get(XML_USETHRIFT);
      if (value != null && !value.isEmpty()) {
        config.setThriftport(value.getInteger());
      }
      value = hashConfig.get(XML_CHECKVERSION);
      if (value != null && !value.isEmpty()) {
        config.setExtendedProtocol(value.getBoolean());
        logger.info("ExtendedProtocol= " + config.isExtendedProtocol());
      }
      value = hashConfig.get(XML_GLOBALDIGEST);
      if (value != null && !value.isEmpty()) {
        config.setGlobalDigest(value.getBoolean());
      }
      alreadySetLimit = true;
      return true;
    } finally {
      hashConfig.clear();
      hashConfig = null;
    }
  }

  public static void loadCommonLimit(final Configuration config,
                                     final XmlHash hashConfig,
                                     boolean updateLimit) {
    XmlValue value = hashConfig.get(XML_LIMITGLOBAL);
    if (value != null && !value.isEmpty()) {
      config.setServerGlobalReadLimit(value.getLong());
      if (config.getServerGlobalReadLimit() <= 0) {
        config.setServerGlobalReadLimit(0);
      }
      config.setServerGlobalWriteLimit(config.getServerGlobalReadLimit());
      logger.info("Global Limit: {}", config.getServerGlobalReadLimit());
    }
    value = hashConfig.get(XML_LIMITSESSION);
    if (value != null && !value.isEmpty()) {
      config.setServerChannelReadLimit(value.getLong());
      if (config.getServerChannelReadLimit() <= 0) {
        config.setServerChannelReadLimit(0);
      }
      config.setServerChannelWriteLimit(config.getServerChannelReadLimit());
      logger.info("SessionInterface Limit: {}",
                  config.getServerChannelReadLimit());
    }
    config.setAnyBandwidthLimitation(config.getServerGlobalReadLimit() > 0 ||
                                     config.getServerGlobalWriteLimit() > 0 ||
                                     config.getServerChannelReadLimit() > 0 ||
                                     config.getServerChannelWriteLimit() > 0);
    config.setDelayLimit(AbstractTrafficShapingHandler.DEFAULT_CHECK_INTERVAL);
    value = hashConfig.get(XML_LIMITDELAY);
    if (value != null && !value.isEmpty()) {
      config.setDelayLimit((value.getLong() / 10) * 10);
      if (config.getDelayLimit() <= 0) {
        config.setDelayLimit(0);
      }
      logger.info("Delay Limit: {}", config.getDelayLimit());
    }
    value = hashConfig.get(XML_DELAYRETRY);
    if (value != null && !value.isEmpty()) {
      config.setDelayRetry((value.getLong() / 10) * 10);
      if (config.getDelayRetry() <= 1000) {
        config.setDelayRetry(1000);
      }
      logger.info("Delay Retry: {}", config.getDelayRetry());
    }
    // FIXME always true since change for DbAdmin
    if (admin.isActive() && updateLimit) {
      value = hashConfig.get(XML_SERVER_HOSTID);
      if (value != null && !value.isEmpty()) {
        config.setHostId(value.getString());
        final DbConfiguration configuration =
            new DbConfiguration(config.getHostId(),
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
        } catch (final WaarpDatabaseException ignored) {
          // nothing
        }
      }
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
        if (cpulimit > 0.99) {
          cpulimit = 1.0;
        }
      }
    }
    int connlimit = 0;
    value = hashConfig.get(XML_CSTRT_CONNLIMIT);
    if (value != null && !value.isEmpty()) {
      connlimit = value.getInteger();
      if (connlimit < 100) {
        connlimit = 0;
      }
    }
    double lowcpuLimit = 0.0;
    double highcpuLimit = 0.0;
    double percentageDecrease = 0;
    long delay = 1000000;
    long limitLowBandwidth = WaarpConstraintLimitHandler.LOWBANDWIDTH_DEFAULT;
    value = hashConfig.get(XML_CSTRT_LOWCPULIMIT);
    if (value != null && !value.isEmpty()) {
      lowcpuLimit = value.getDouble();
    }
    value = hashConfig.get(XML_CSTRT_HIGHCPULIMIT);
    if (value != null && !value.isEmpty()) {
      highcpuLimit = value.getDouble();
      if (highcpuLimit < 0.1) {
        highcpuLimit = 0.0;
      }
    }
    value = hashConfig.get(XML_CSTRT_PERCENTDECREASE);
    if (value != null && !value.isEmpty()) {
      percentageDecrease = value.getDouble();
    }
    value = hashConfig.get(XML_CSTRT_DELAYTHROTTLE);
    if (value != null && !value.isEmpty()) {
      delay = (value.getLong() / 10) * 10;
      if (delay < 100) {
        delay = 100;
      }
    }
    value = hashConfig.get(XML_CSTRT_LIMITLOWBANDWIDTH);
    if (value != null && !value.isEmpty()) {
      limitLowBandwidth = value.getLong();
    }
    if (config.getConstraintLimitHandler() != null) {
      config.getConstraintLimitHandler().release();
    }
    if (useCpuLimit || highcpuLimit > 0) {
      if (highcpuLimit > 0) {
        logger.debug("full setup of ContraintLimitHandler");
        config.setConstraintLimitHandler(
            new R66ConstraintLimitHandler(useCpuLimit, useCpuLimitJDK, cpulimit,
                                          connlimit, lowcpuLimit, highcpuLimit,
                                          percentageDecrease, null, delay,
                                          limitLowBandwidth));
      } else {
        logger.debug("partial setup of ContraintLimitHandler");
        config.setConstraintLimitHandler(
            new R66ConstraintLimitHandler(useCpuLimit, useCpuLimitJDK, cpulimit,
                                          connlimit));
      }
    } else {
      logger.debug("No setup of ContraintLimitHandler");
      config.setConstraintLimitHandler(
          new R66ConstraintLimitHandler(false, false, 1.0, connlimit));
    }
    value = hashConfig.get(XML_SERVER_THREAD);
    if (value != null && !value.isEmpty()) {
      config.setServerThread(value.getInteger());
    }
    value = hashConfig.get(XML_CLIENT_THREAD);
    if (value != null && !value.isEmpty()) {
      config.setClientThread(value.getInteger());
    }
    value = hashConfig.get(XML_MEMORY_LIMIT);
    if (value != null && !value.isEmpty()) {
      config.setMaxGlobalMemory(value.getLong());
    }
    Configuration.getFileParameter().deleteOnAbort = false;
    value = hashConfig.get(XML_USENIO);
    if (value != null && !value.isEmpty()) {
      FilesystemBasedFileParameterImpl.useNio = value.getBoolean();
    }
    value = hashConfig.get(XML_TIMEOUTCON);
    if (value != null && !value.isEmpty()) {
      config.setTimeoutCon((value.getLong() / 10) * 10);
      config.getShutdownConfiguration().timeout = config.getTimeoutCon();
    }
  }

  /**
   * @param config
   * @param hashRootConfig
   *
   * @return True if the SSL configuration is correctly loaded
   */
  public static boolean loadSsl(Configuration config,
                                XmlRootHash hashRootConfig) {
    XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_SSL));
    try {
      // StoreKey for Server
      XmlValue value = hashConfig.get(XML_PATH_KEYPATH);
      if (value == null || value.isEmpty()) {
        logger.info("Unable to find Key Path");
        try {
          NetworkSslServerInitializer.setWaarpSecureKeyStore(
              new WaarpSecureKeyStore("secret", "secret"));//NOSONAR
        } catch (final CryptoException e) {
          logger.error("Bad SecureKeyStore construction");
          return false;
        }
      } else {
        final String keypath = value.getString();
        if (keypath == null || keypath.isEmpty()) {
          logger.error("Bad Key Path");
          return false;
        }
        value = hashConfig.get(XML_PATH_KEYSTOREPASS);
        if (value == null || value.isEmpty()) {
          logger.error("Unable to find KeyStore Passwd");
          return false;
        }
        final String keystorepass = value.getString();
        if (keystorepass == null || keystorepass.isEmpty()) {
          logger.error("Bad KeyStore Passwd");
          return false;
        }
        value = hashConfig.get(XML_PATH_KEYPASS);
        if (value == null || value.isEmpty()) {
          logger.error("Unable to find Key Passwd");
          return false;
        }
        final String keypass = value.getString();
        if (keypass == null || keypass.isEmpty()) {
          logger.error("Bad Key Passwd");
          return false;
        }
        try {
          NetworkSslServerInitializer.setWaarpSecureKeyStore(
              new WaarpSecureKeyStore(keypath, keystorepass, keypass));
        } catch (final CryptoException e) {
          logger.error("Bad SecureKeyStore construction");
          return false;
        }
      }
      // TrustedKey for OpenR66 server
      value = hashConfig.get(XML_PATH_TRUSTKEYPATH);
      if (value == null || value.isEmpty()) {
        logger.info("Unable to find TRUST Key Path");
        NetworkSslServerInitializer.getWaarpSecureKeyStore()
                                   .initEmptyTrustStore();
      } else {
        final String keypath = value.getString();
        if (keypath == null || keypath.isEmpty()) {
          logger.error("Bad TRUST Key Path");
          return false;
        }
        value = hashConfig.get(XML_PATH_TRUSTKEYSTOREPASS);
        if (value == null || value.isEmpty()) {
          logger.error("Unable to find TRUST KeyStore Passwd");
          return false;
        }
        final String keystorepass = value.getString();
        if (keystorepass == null || keystorepass.isEmpty()) {
          logger.error("Bad TRUST KeyStore Passwd");
          return false;
        }
        boolean useClientAuthent = false;
        value = hashConfig.get(XML_USECLIENT_AUTHENT);
        if (value != null && !value.isEmpty()) {
          useClientAuthent = value.getBoolean();
        }
        try {
          NetworkSslServerInitializer.getWaarpSecureKeyStore()
                                     .initTrustStore(keypath, keystorepass,
                                                     useClientAuthent);
        } catch (final CryptoException e) {
          logger.error("Bad TrustKeyStore construction");
          return false;
        }
      }
      NetworkSslServerInitializer.setWaarpSslContextFactory(
          new WaarpSslContextFactory(
              NetworkSslServerInitializer.getWaarpSecureKeyStore()));
      return true;
    } finally {
      hashConfig.clear();
      hashConfig = null;
    }
  }

  /**
   * @param config
   *
   * @return True if the network configuration is correctly loaded
   */
  private static boolean loadNetworkServer(Configuration config) {
    XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_NETWORK));
    try {
      XmlValue value = hashConfig.get(XML_SERVER_PORT);
      int port = 6666;
      if (value != null && !value.isEmpty()) {
        port = value.getInteger();
      } else {
        port = 6666;
      }
      config.setServerPort(port);
      value = hashConfig.get(XML_SERVER_SSLPORT);
      int sslport = 6667;
      if (value != null && !value.isEmpty()) {
        sslport = value.getInteger();
      } else {
        sslport = 6667;
      }
      config.setServerSslPort(sslport);
      value = hashConfig.get(XML_SERVER_HTTPPORT);
      int httpport = 8066;
      if (value != null && !value.isEmpty()) {
        httpport = value.getInteger();
      }
      config.setServerHttpport(httpport);
      value = hashConfig.get(XML_SERVER_HTTPSPORT);
      int httpsport = 8067;
      if (value != null && !value.isEmpty()) {
        httpsport = value.getInteger();
      }
      config.setServerHttpsPort(httpsport);
      return true;
    } finally {
      hashConfig.clear();
      hashConfig = null;
    }
  }

  /**
   * @param configuration
   *
   * @return True if the REST configuration is correctly loaded
   */
  @SuppressWarnings("unchecked")
  private static boolean loadRest(Configuration configuration) {
    XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_REST));
    try {
      final XmlValue valueRest = hashConfig.get(XML_REST);
      if (valueRest != null && valueRest.getList() != null) {
        for (final XmlValue[] xml : (Iterable<XmlValue[]>) valueRest
            .getList()) {
          final RestConfiguration config = new RestConfiguration();
          final XmlHash subHash = new XmlHash(xml);
          XmlValue value = subHash.get(XML_SERVER_REST_PORT);
          int restPort = -1;
          if (value != null && !value.isEmpty()) {
            restPort = value.getInteger();
          }
          config.setRestPort(restPort);
          if (config.getRestPort() > 0) {
            value = subHash.get(XML_REST_ADDRESS);
            String restAddress = null;
            if (value != null && !value.isEmpty()) {
              restAddress = value.getString();
            }
            config.setRestAddress(restAddress);
            value = subHash.get(XML_REST_SSL);
            boolean restSsl = false;
            if (value != null && !value.isEmpty()) {
              restSsl = value.getBoolean();
            }
            config.setRestSsl(restSsl);
            value = subHash.get(XML_REST_AUTHENTICATED);
            boolean restAuthent = false;
            if (value != null && !value.isEmpty()) {
              restAuthent = value.getBoolean();
            }
            config.setRestAuthenticated(restAuthent);
            value = subHash.get(XML_REST_SIGNATURE);
            boolean restSignature = true;
            if (value != null && !value.isEmpty()) {
              restSignature = value.getBoolean();
            }
            config.setRestSignature(restSignature);
            if (config.isRestSignature() || config.isRestAuthenticated()) {
              final XmlValue valueKey = subHash.get(XML_REST_AUTH_KEY);
              if (valueKey != null && !valueKey.isEmpty()) {
                String fileKey = valueKey.getString();
                File file = new File(fileKey);
                if (!file.canRead()) {
                  file = new File(
                      configuration.getConfigPath() + DirInterface.SEPARATOR +
                      fileKey);
                  if (!file.canRead()) {
                    logger.error("Unable to find REST Key in Config file");
                    return false;
                  }
                  fileKey =
                      configuration.getConfigPath() + DirInterface.SEPARATOR +
                      fileKey;
                }
                try {
                  config.initializeKey(file);
                } catch (final CryptoException e) {
                  logger.error(
                      "Unable to load REST Key from Config file: " + fileKey,
                      e);
                  return false;
                } catch (final IOException e) {
                  logger.error(
                      "Unable to load REST Key from Config file: " + fileKey,
                      e);
                  return false;
                }
              }
            }
            value = subHash.get(XML_REST_TIME_LIMIT);
            long restTimeLimit = -1;
            if (value != null && !value.isEmpty()) {
              restTimeLimit = value.getLong();
            }
            config.setRestTimeLimit(restTimeLimit);

            final XmlValue valueMethod = subHash.get(XML_REST_METHOD);
            if (valueMethod != null && valueMethod.getList() != null) {
              boolean found = false;
              config
                  .setResthandlersCrud(new byte[RESTHANDLERS.values().length]);
              for (final XmlValue[] xmlmethod : (Iterable<XmlValue[]>) valueMethod
                  .getList()) {
                final XmlHash subHashMethod = new XmlHash(xmlmethod);
                value = subHashMethod.get(XML_REST_METHOD_NAME);
                String name;
                if (value != null && !value.isEmpty()) {
                  name = value.getString();
                } else {
                  logger.warn("Restmethod entry ignore since name is empty");
                  continue;
                }
                value = subHashMethod.get(XML_REST_CRUD);
                String crud;
                if (value != null && !value.isEmpty()) {
                  crud = value.getString().toUpperCase();
                } else {
                  logger.warn(
                      "Restmethod entry ignore since crud field is empty");
                  continue;
                }
                found = true;
                byte def = 0x0;
                def |=
                    crud.contains("C")? RestConfiguration.CRUD.CREATE.mask : 0;
                def |= crud.contains("R")? RestConfiguration.CRUD.READ.mask : 0;
                def |=
                    crud.contains("U")? RestConfiguration.CRUD.UPDATE.mask : 0;
                def |=
                    crud.contains("D")? RestConfiguration.CRUD.DELETE.mask : 0;
                if ("all".equalsIgnoreCase(name)) {
                  Arrays.fill(config.getResthandlersCrud(), def);
                  // No more restmethod since ALL was selected
                  break;
                } else {
                  final String[] handlers = name.split(" |\\|");
                  for (final String string : handlers) {
                    final RESTHANDLERS handler = RESTHANDLERS.valueOf(string);
                    config.getResthandlersCrud()[handler.ordinal()] = def;
                  }
                }
              }
              if (!found) {
                // no METHOD !!!
                logger.error(
                    "No active METHOD defined for REST in Config file: " +
                    config);
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
   *
   * @return True if OK
   */
  private static boolean setCryptoKey(Configuration config,
                                      XmlHash hashConfig) {
    final XmlValue value = hashConfig.get(XML_PATH_CRYPTOKEY);
    if (value == null || value.isEmpty()) {
      logger.error("Unable to find CryptoKey in Config file");
      return false;
    }
    final String filename = value.getString();
    config.setCryptoFile(filename);
    final File key = new File(filename);
    final Des des = new Des();
    try {
      des.setSecretKey(key);
    } catch (final CryptoException e) {
      logger.error("Unable to load CryptoKey from Config file {}", filename);
      return false;
    } catch (final IOException e) {
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
   *
   * @return True if OK
   */
  private static boolean loadFromDatabase(Configuration config) {
    if (!config.isSaveTaskRunnerWithNoDb()) {
      // load from database the limit to apply
      try {
        final DbConfiguration configuration =
            new DbConfiguration(config.getHostId());
        configuration.updateConfiguration();
      } catch (final WaarpDatabaseException e) {
        logger.info(Messages.getString("FileBasedConfiguration.NoBandwidth") +
                    e.getMessage()); //$NON-NLS-1$
      }
    } else {
      if (config.getBaseDirectory() != null && config.getConfigPath() != null) {
        // load Rules from files
        final File dirConfig =
            new File(config.getBaseDirectory() + config.getConfigPath());
        if (dirConfig.isDirectory()) {
          try {
            RuleFileBasedConfiguration.importRules(dirConfig);
          } catch (final OpenR66ProtocolSystemException e) {
            logger.error(Messages.getString("FileBasedConfiguration.NoRule"),
                         e); //$NON-NLS-1$
            return false;
          } catch (final WaarpDatabaseException e) {
            logger.error(Messages.getString("FileBasedConfiguration.NoRule"),
                         e); //$NON-NLS-1$
            return false;
          }
        } else {
          logger.error("Config Directory is not a directory: " +
                       config.getBaseDirectory() + config.getConfigPath());
          return false;
        }
      }
      // load if possible the limit to apply
      loadLimit(config, false);
    }
    return true;
  }

  public static boolean autoupgrade;

  /**
   * Load database parameter
   *
   * @param config
   * @param initdb
   *
   * @return True if OK
   */
  private static boolean loadDatabase(Configuration config, boolean initdb) {
    XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_DB));
    try {
      XmlValue value = hashConfig.get(XML_SAVE_TASKRUNNERNODB);
      if (value != null && !value.isEmpty() && value.getBoolean()) {
        config.setSaveTaskRunnerWithNoDb(value.getBoolean());
        logger.info(
            Messages.getString("FileBasedConfiguration.NoDB")); //$NON-NLS-1$
        admin = new DbAdmin(); // no database support
        noCommitAdmin = admin;
        DAOFactory.initialize();
        return true;
      }
      value = hashConfig.get(XML_DBDRIVER);
      if (value == null || value.isEmpty()) {
        if (config.isWarnOnStartup()) {
          logger.warn(Messages.getString("FileBasedConfiguration.NoDB"));
          //$NON-NLS-1$
        } else {
          logger.info(
              Messages.getString("FileBasedConfiguration.NoDB")); //$NON-NLS-1$
        }
        admin = new DbAdmin(); // no database support
        noCommitAdmin = admin;
        DAOFactory.initialize();
      } else {
        final String dbdriver = value.getString();
        value = hashConfig.get(XML_DBSERVER);
        if (value == null || value.isEmpty()) {
          logger.error(
              Messages.getString(FILE_BASED_CONFIGURATION_NOT_FOUND_CONFIG) +
              "DBServer"); //$NON-NLS-1$
          return false;
        }
        final String dbserver = value.getString();
        value = hashConfig.get(XML_DBUSER);
        if (value == null || value.isEmpty()) {
          logger.error(
              Messages.getString(FILE_BASED_CONFIGURATION_NOT_FOUND_CONFIG) +
              "DBUser"); //$NON-NLS-1$
          return false;
        }
        final String dbuser = value.getString();
        value = hashConfig.get(XML_DBPASSWD);
        if (value == null || value.isEmpty()) {
          logger.error(
              Messages.getString(FILE_BASED_CONFIGURATION_NOT_FOUND_CONFIG) +
              "DBPassword"); //$NON-NLS-1$
          return false;
        }
        final String dbpasswd = value.getString();
        if (dbdriver == null || dbserver == null || dbuser == null ||
            dbpasswd == null || dbdriver.isEmpty() || dbserver.isEmpty() ||
            dbuser.isEmpty() || dbpasswd.isEmpty()) {
          logger.error(
              Messages.getString(FILE_BASED_CONFIGURATION_NOT_FOUND_CONFIG) +
              "Correct DB data"); //$NON-NLS-1$
          return false;
        }
        try {
          admin = DbModelFactoryR66
              .initialize(dbdriver, dbserver, dbuser, dbpasswd, true);
          // New way of initializing database services
          try {
            ConnectionFactory.initialize(dbserver, dbuser, dbpasswd);
          } catch (final UnsupportedOperationException e) {
            logger.error(e);
            return false;
          } catch (final SQLException e) {
            logger.error("Cannot create ConnectionFactory", e);
            return false;
          }
          DAOFactory.initialize(ConnectionFactory.getInstance());

          if (config.getMultipleMonitors() > 1) {
            noCommitAdmin = DbModelFactoryR66
                .initialize(dbdriver, dbserver, dbuser, dbpasswd, true);
            Configuration.setNbDbSession(Configuration.getNbDbSession() + 1);
            noCommitAdmin.getSession().setAutoCommit(false);
          } else {
            noCommitAdmin = admin;
          }
          logger.info("Database connection: Admin:" + (admin != null) +
                      " NoCommitAdmin:" + (noCommitAdmin != null));

          try {
            logger.info("DefaultTransactionIsolation: " +
                        admin.getSession().getConn().getMetaData()
                             .getDefaultTransactionIsolation() +
                        " MaxConnections: " +
                        admin.getSession().getConn().getMetaData()
                             .getMaxConnections() + " MaxStatements: " +
                        admin.getSession().getConn().getMetaData()
                             .getMaxStatements());
          } catch (final SQLException e) {
            SysErrLogger.FAKE_LOGGER.syserr(e);
          }
        } catch (final WaarpDatabaseNoConnectionException e2) {
          logger.error(Messages.getString("Database.CannotConnect"),
                       e2); //$NON-NLS-1$
          return false;
        }
        // Check if the database is ready (initdb already done before)
        DbRequest request;
        if (!initdb) {
          try {
            request = new DbRequest(admin.getSession());
            try {
              request.select("SELECT * FROM " + DbConfiguration.table);
            } catch (final WaarpDatabaseSqlException e) {
              logger.warn(Messages.getString("Database.DbNotInitiated"),
                          e); //$NON-NLS-1$
              return true;
            } finally {
              request.close();
            }
          } catch (final WaarpDatabaseNoConnectionException e1) {
            // ignore
            /*
             * TODO: Why Ignore? throwing bad configuration seems better
             */
          }
        }
        // TODO to remove when <dbcheck> is drop from config file
        value = hashConfig.get(XML_DBCHECK);
        if (value != null && !value.isEmpty()) {
          logger.warn(
              "<{}> is deprecated in configuration file " + "use <{}> instead",
              XML_DBCHECK, XML_DBAUTOUPGRADE);
          autoupgrade = value.getBoolean();
        } else {
          // Keep this part
          value = hashConfig.get(XML_DBAUTOUPGRADE);
          if (value != null && !value.isEmpty()) {
            autoupgrade = value.getBoolean();
          }
        }
        if (autoupgrade && !initdb) {
          // Check if the database is up to date
          try {
            if (!ServerInitDatabase.upgradedb()) {
              return false;
            }
          } catch (final WaarpDatabaseException e) {
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
    XmlHash hashConfig =
        new XmlHash(hashRootConfig.get(DbHostConfiguration.XML_BUSINESS));
    try {
      final XmlValue value = hashConfig.get(DbHostConfiguration.XML_BUSINESS);
      if (value != null && value.getList() != null) {
        @SuppressWarnings("unchecked")
        List<String> ids = (List<String>) value.getList();
        if (ids != null) {
          for (final String sval : ids) {
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
      if (admin != null) {
        try {
          final DbHostConfiguration hostconfiguration =
              new DbHostConfiguration(config.getHostId());
          if (hostconfiguration != null) {
            DbHostConfiguration
                .updateHostConfiguration(config, hostconfiguration);
          }
        } catch (final WaarpDatabaseException e) {
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
    XmlHash hashConfig =
        new XmlHash(hashRootConfig.get(DbHostConfiguration.XML_ALIASES));
    try {
      XmlValue value = hashConfig.get(DbHostConfiguration.XML_ALIASES);
      if (value != null && value.getList() != null) {
        for (final XmlValue[] xml : (Iterable<XmlValue[]>) value.getList()) {
          final XmlHash subHash = new XmlHash(xml);
          value = subHash.get(DbHostConfiguration.XML_REALID);
          if (value == null || value.isEmpty()) {
            continue;
          }
          final String refHostId = value.getString();
          value = subHash.get(DbHostConfiguration.XML_ALIASID);
          if (value == null || value.isEmpty()) {
            continue;
          }
          final String aliasset = value.getString();
          final String[] alias = aliasset.split(" |\\|");
          for (final String namealias : alias) {
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
    if (config.getHostId() != null) {
      config.getVersions().putIfAbsent(config.getHostId(),
                                       new PartnerConfiguration(
                                           config.getHostId()));
    }
    if (config.getHostSslId() != null) {
      config.getVersions().putIfAbsent(config.getHostSslId(),
                                       new PartnerConfiguration(
                                           config.getHostSslId()));
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
    XmlHash hashConfig =
        new XmlHash(hashRootConfig.get(DbHostConfiguration.XML_ROLES));
    try {
      XmlValue value = hashConfig.get(DbHostConfiguration.XML_ROLES);
      if (value != null && value.getList() != null) {
        for (final XmlValue[] xml : (Iterable<XmlValue[]>) value.getList()) {
          final XmlHash subHash = new XmlHash(xml);
          value = subHash.get(DbHostConfiguration.XML_ROLEID);
          if (value == null || value.isEmpty()) {
            continue;
          }
          final String refHostId = value.getString();
          value = subHash.get(DbHostConfiguration.XML_ROLESET);
          if (value == null || value.isEmpty()) {
            continue;
          }
          final String roleset = value.getString();
          final String[] roles = roleset.split(" |\\|");
          final RoleDefault newrole = new RoleDefault();
          for (final String role : roles) {
            try {
              final RoleDefault.ROLE roletype =
                  RoleDefault.ROLE.valueOf(role.toUpperCase());
              if (roletype == ROLE.NOACCESS) {
                // reset
                newrole.setRole(roletype);
              } else {
                newrole.addRole(roletype);
              }
            } catch (final IllegalArgumentException e) {
              // ignore
            }
          }
          logger.info("New Role: " + refHostId + ':' + newrole);
          config.getRoles().put(refHostId, newrole);
        }
      }
    } finally {
      hashConfig.clear();
      hashConfig = null;
    }
  }

  /**
   * @param config
   * @param fromXML
   *
   * @return the new subpath
   *
   * @throws OpenR66ProtocolSystemException
   */
  private static String getSubPath(Configuration config, String fromXML)
      throws OpenR66ProtocolSystemException {
    XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_DIRECTORY));
    try {
      final XmlValue value = hashConfig.get(fromXML);
      if (value == null || value.isEmpty()) {
        logger.error(Messages.getString("FileBasedConfiguration.NoXmlPath") +
                     fromXML); //$NON-NLS-1$
        throw new OpenR66ProtocolSystemException(
            Messages.getString("FileBasedConfiguration.NoXmlPath") +
            fromXML); //$NON-NLS-1$
      }

      String path = value.getString();
      if (path == null || path.isEmpty()) {
        throw new OpenR66ProtocolSystemException(
            Messages.getString("FileBasedConfiguration.NotCorrectPath") +
            fromXML); //$NON-NLS-1$
      }
      path = DirInterface.SEPARATOR + path;
      final String newpath = config.getBaseDirectory() + path;
      final File file = new File(newpath);
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
   *
   * @return True if OK
   */
  public static boolean setConfigurationLoadLimitFromXml(Configuration config,
                                                         String filename) {
    Document document;
    alreadySetLimit = false;
    // Open config file
    try {
      document = new SAXReader().read(filename);
    } catch (final DocumentException e) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_CANNOT_READ_XML) +
          filename, e); //$NON-NLS-1$
      return false;
    }
    if (document == null) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_CANNOT_READ_XML) +
          filename); //$NON-NLS-1$
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
      logger.error(Messages.getString("FileBasedConfiguration.NoLimit") +
                   filename); //$NON-NLS-1$
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
   *
   * @return True if OK
   */
  public static boolean setConfigurationInitDatabase(Configuration config,
                                                     String filename,
                                                     boolean initdb) {
    Document document;
    // Open config file
    try {
      document = new SAXReader().read(filename);
    } catch (final DocumentException e) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_CANNOT_READ_XML) +
          filename, e); //$NON-NLS-1$
      return false;
    }
    if (document == null) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_CANNOT_READ_XML) +
          filename); //$NON-NLS-1$
      return false;
    }
    configuration = XmlUtil.read(document, configServer);
    hashRootConfig = new XmlRootHash(configuration);
    if (!loadIdentity(config, hashRootConfig)) {
      logger.error(CANNOT_LOAD_IDENTITY);
      return false;
    }
    if (!loadDatabase(config, initdb)) {
      logger.error(CANNOT_LOAD_DATABASE_CONFIGURATION);
      return false;
    }
    if (!loadDirectory(config)) {
      logger.error(CANNOT_LOAD_DIRECTORY_CONFIGURATION);
      return false;
    }
    if (!loadLimit(config, false)) {
      logger.error(CANNOT_LOAD_LIMIT_CONFIGURATION);
      return false;
    }
    if (config.isSaveTaskRunnerWithNoDb()) {
      // if no database, must load authentication from file
      if (!loadAuthentication(config)) {
        logger.error(CANNOT_LOAD_AUTHENTICATION_CONFIGURATION);
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
   *
   * @return True if OK
   */
  public static boolean setConfigurationServerMinimalFromXml(
      Configuration config, String filename) {
    if (!SystemPropertyUtil
        .get(R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK, "")
        .isEmpty()) {
      logger.warn(IS_DEPRECATED_IN_SYSTEM_PROPERTIES_USE_INSTEAD,
                  R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK,
                  R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE);
      autoupgrade = SystemPropertyUtil
          .getBoolean(R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK,
                      false);
    } else {
      autoupgrade = SystemPropertyUtil
          .getBoolean(R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE,
                      false);
    }

    Document document;
    // Open config file
    try {
      document = new SAXReader().read(filename);
    } catch (final DocumentException e) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_CANNOT_READ_XML) +
          filename, e); //$NON-NLS-1$
      return false;
    }
    if (document == null) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_CANNOT_READ_XML) +
          filename); //$NON-NLS-1$
      return false;
    }
    configuration = XmlUtil.read(document, configServer);
    hashRootConfig = new XmlRootHash(configuration);
    if (!loadIdentity(config, hashRootConfig)) {
      logger.error(CANNOT_LOAD_IDENTITY);
      return false;
    }
    if (!loadDatabase(config, false)) {
      logger.error(CANNOT_LOAD_DATABASE_CONFIGURATION);
      return false;
    }
    if (!loadDirectory(config)) {
      logger.error(CANNOT_LOAD_DIRECTORY_CONFIGURATION);
      return false;
    }
    if (!loadLimit(config, false)) {
      logger.error(CANNOT_LOAD_LIMIT_CONFIGURATION);
      return false;
    }
    if (config.isSaveTaskRunnerWithNoDb()) {
      // if no database, must load authentication from file
      if (!loadAuthentication(config)) {
        logger.error(CANNOT_LOAD_AUTHENTICATION_CONFIGURATION);
        return false;
      }
    }
    config.setHostAuth(R66Auth.getServerAuth(config.getHostId()));
    if (config.getHostAuth() == null && config.isUseNOSSL()) {
      logger.error(CANNOT_FIND_AUTHENTICATION_FOR_CURRENT_HOST);
      return false;
    }
    if (config.getHostSslId() != null) {
      config.setHostSslAuth(R66Auth.getServerAuth(config.getHostSslId()));
      if (config.getHostSslAuth() == null && config.isUseSSL()) {
        logger.error(CANNOT_FIND_SSL_AUTHENTICATION_FOR_CURRENT_HOST);
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
   *
   * @return True if OK
   */
  public static boolean setConfigurationServerShutdownFromXml(
      Configuration config, String filename) {
    if (!SystemPropertyUtil
        .get(R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK, "")
        .isEmpty()) {
      logger.warn(IS_DEPRECATED_IN_SYSTEM_PROPERTIES_USE_INSTEAD,
                  R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK,
                  R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE);
      autoupgrade = SystemPropertyUtil
          .getBoolean(R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK,
                      false);
    } else {
      autoupgrade = SystemPropertyUtil
          .getBoolean(R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE,
                      false);
    }

    Document document;
    // Open config file
    try {
      document = new SAXReader().read(filename);
    } catch (final DocumentException e) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_CANNOT_READ_XML) +
          filename, e); //$NON-NLS-1$
      return false;
    }
    if (document == null) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_CANNOT_READ_XML) +
          filename); //$NON-NLS-1$
      return false;
    }
    configuration = XmlUtil.read(document, configServer);
    hashRootConfig = new XmlRootHash(configuration);
    // Now read the configuration
    if (!loadIdentity(config, hashRootConfig)) {
      logger.error(CANNOT_LOAD_IDENTITY);
      return false;
    }
    if (!loadDatabase(config, false)) {
      logger.error(CANNOT_LOAD_DATABASE_CONFIGURATION);
      return false;
    }
    if (!loadServerParam(config)) {
      logger.error("Cannot load Server Parameters");
      return false;
    }
    if (!loadDirectory(config)) {
      logger.error(CANNOT_LOAD_DIRECTORY_CONFIGURATION);
      return false;
    }
    if (!loadLimit(config, false)) {
      logger.error(CANNOT_LOAD_LIMIT_CONFIGURATION);
      return false;
    }
    if (config.isUseSSL() && !loadSsl(config, hashRootConfig)) {
      logger.error("Cannot load SSL configuration");
      return false;
    }
    if (!loadNetworkServer(config)) {
      logger.error("Cannot load Network configuration");
      return false;
    }
    if (config.isSaveTaskRunnerWithNoDb()) {
      // if no database, must load authentication from file
      if (!loadAuthentication(config)) {
        logger.error(CANNOT_LOAD_AUTHENTICATION_CONFIGURATION);
        return false;
      }
    }
    config.setHostAuth(R66Auth.getServerAuth(config.getHostId()));
    if (config.getHostAuth() == null && config.isUseNOSSL()) {
      logger.error(CANNOT_FIND_AUTHENTICATION_FOR_CURRENT_HOST);
      return false;
    }
    if (config.getHostSslId() != null) {
      config.setHostSslAuth(R66Auth.getServerAuth(config.getHostSslId()));
      if (config.getHostSslAuth() == null && config.isUseSSL()) {
        logger.error(CANNOT_FIND_SSL_AUTHENTICATION_FOR_CURRENT_HOST);
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
   *
   * @return True if OK
   */
  public static boolean setConfigurationServerFromXml(Configuration config,
                                                      String filename) {
    Document document;
    // Open config file
    try {
      document = new SAXReader().read(filename);
    } catch (final DocumentException e) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_CANNOT_READ_XML) +
          filename, e); //$NON-NLS-1$
      return false;
    }
    if (document == null) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_CANNOT_READ_XML) +
          filename); //$NON-NLS-1$
      return false;
    }
    configuration = XmlUtil.read(document, configServer);
    hashRootConfig = new XmlRootHash(configuration);
    // Now read the configuration
    if (!loadIdentity(config, hashRootConfig)) {
      logger.error(CANNOT_LOAD_IDENTITY);
      return false;
    }
    if (!loadDatabase(config, false)) {
      logger.error(CANNOT_LOAD_DATABASE_CONFIGURATION);
      return false;
    }
    if (!loadServerParam(config)) {
      logger.error("Cannot load Server Parameters");
      return false;
    }
    if (!loadDirectory(config)) {
      logger.error(CANNOT_LOAD_DIRECTORY_CONFIGURATION);
      return false;
    }
    if (!loadLimit(config, false)) {
      logger.error(CANNOT_LOAD_LIMIT_CONFIGURATION);
      return false;
    }
    if (config.isUseSSL() && !loadSsl(config, hashRootConfig)) {
      logger.error("Cannot load SSL configuration");
      return false;
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
        logger.error(CANNOT_LOAD_AUTHENTICATION_CONFIGURATION);
        return false;
      }
    }
    config.setHostAuth(R66Auth.getServerAuth(config.getHostId()));
    if (config.getHostAuth() == null && config.isUseNOSSL()) {
      logger.error(CANNOT_FIND_AUTHENTICATION_FOR_CURRENT_HOST);
      return false;
    }
    if (config.getHostSslId() != null) {
      config.setHostSslAuth(R66Auth.getServerAuth(config.getHostSslId()));
      if (config.getHostSslAuth() == null && config.isUseSSL()) {
        logger.error(CANNOT_FIND_SSL_AUTHENTICATION_FOR_CURRENT_HOST);
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
   *
   * @return True if OK
   */
  public static boolean setClientConfigurationFromXml(Configuration config,
                                                      String filename) {
    if (!SystemPropertyUtil
        .get(R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK, "")
        .isEmpty()) {
      logger.warn(IS_DEPRECATED_IN_SYSTEM_PROPERTIES_USE_INSTEAD,
                  R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK,
                  R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE);
      autoupgrade = SystemPropertyUtil
          .getBoolean(R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK,
                      false);
    } else {
      autoupgrade = SystemPropertyUtil
          .getBoolean(R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE,
                      false);
    }

    Document document;
    // Open config file
    try {
      document = new SAXReader().read(filename);
    } catch (final DocumentException e) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_CANNOT_READ_XML) +
          filename, e); //$NON-NLS-1$
      return false;
    }
    if (document == null) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_CANNOT_READ_XML) +
          filename); //$NON-NLS-1$
      return false;
    }
    configuration = XmlUtil.read(document, configClient);
    hashRootConfig = new XmlRootHash(configuration);
    // Client enables SSL by default but could be reverted later on
    config.setUseSSL(true);
    if (!loadIdentity(config, hashRootConfig)) {
      logger.error(CANNOT_LOAD_IDENTITY);
      return false;
    }
    if (!loadDirectory(config)) {
      logger.error(CANNOT_LOAD_DIRECTORY_CONFIGURATION);
      return false;
    }
    if (!loadDatabase(config, false)) {
      logger.error(CANNOT_LOAD_DATABASE_CONFIGURATION);
      return false;
    }
    if (!loadClientParam(config)) {
      logger.error("Cannot load Client Parameters");
      return false;
    }
    if (!loadLimit(config, false)) {
      logger.error(CANNOT_LOAD_LIMIT_CONFIGURATION);
      return false;
    }
    if (config.isUseSSL() && !loadSsl(config, hashRootConfig)) {
      logger.error("Cannot load SSL configuration");
      return false;
    }
    if (!loadFromDatabase(config)) {
      logger.error("Cannot load configuration from Database");
      return false;
    }
    if (config.isSaveTaskRunnerWithNoDb()) {
      // if no database, must load authentication from file
      if (!loadAuthentication(config)) {
        logger.error(CANNOT_LOAD_AUTHENTICATION_CONFIGURATION);
        return false;
      }
    }
    try {
      config.setHostAuth(new DbHostAuth(config.getHostId()));
    } catch (final WaarpDatabaseException e) {
      logger.error(CANNOT_FIND_AUTHENTICATION_FOR_CURRENT_HOST, e);
      return false;
    }
    if (config.getHostAuth() == null) {
      logger.error(CANNOT_FIND_AUTHENTICATION_FOR_CURRENT_HOST);
      return false;
    }
    if (config.getHostSslId() != null) {
      config.setHostSslAuth(R66Auth.getServerAuth(config.getHostSslId()));
      if (config.getHostSslAuth() == null) {
        logger.error(CANNOT_FIND_SSL_AUTHENTICATION_FOR_CURRENT_HOST);
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
   *
   * @return True if OK
   */
  public static boolean setSubmitClientConfigurationFromXml(
      Configuration config, String filename) {
    if (!SystemPropertyUtil
        .get(R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK, "")
        .isEmpty()) {
      logger.warn(IS_DEPRECATED_IN_SYSTEM_PROPERTIES_USE_INSTEAD,
                  R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK,
                  R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE);
      autoupgrade = SystemPropertyUtil
          .getBoolean(R66SystemProperties.OPENR66_STARTUP_DATABASE_CHECK,
                      false);
    } else {
      autoupgrade = SystemPropertyUtil
          .getBoolean(R66SystemProperties.OPENR66_STARTUP_DATABASE_AUTOUPGRADE,
                      false);
    }

    Document document;
    // Open config file
    try {
      document = new SAXReader().read(filename);
    } catch (final DocumentException e) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_CANNOT_READ_XML) +
          filename, e); //$NON-NLS-1$
      return false;
    }
    if (document == null) {
      logger.error(
          Messages.getString(FILE_BASED_CONFIGURATION_CANNOT_READ_XML) +
          filename); //$NON-NLS-1$
      return false;
    }
    configuration = XmlUtil.read(document, configSubmitClient);
    hashRootConfig = new XmlRootHash(configuration);
    // Client enables SSL by default but could be reverted later on
    config.setUseSSL(true);
    if (!loadIdentity(config, hashRootConfig)) {
      logger.error(CANNOT_LOAD_IDENTITY);
      return false;
    }
    if (!loadDatabase(config, false)) {
      logger.error(CANNOT_LOAD_DATABASE_CONFIGURATION);
      return false;
    }
    if (!loadDirectory(config)) {
      logger.error(CANNOT_LOAD_DIRECTORY_CONFIGURATION);
      return false;
    }
    XmlHash hashConfig = new XmlHash(hashRootConfig.get(XML_LIMIT));
    try {
      final XmlValue value = hashConfig.get(XML_BLOCKSIZE);
      if (value != null && !value.isEmpty()) {
        config.setBlockSize(value.getInteger());
      }
    } finally {
      hashConfig.clear();
      hashConfig = null;
    }
    config.setHostAuth(R66Auth.getServerAuth(config.getHostId()));
    if (config.getHostAuth() == null) {
      logger.error(CANNOT_FIND_AUTHENTICATION_FOR_CURRENT_HOST);
      return false;
    }
    if (config.getHostSslId() != null) {
      config.setHostSslAuth(R66Auth.getServerAuth(config.getHostSslId()));
      if (config.getHostSslAuth() == null) {
        logger.error(CANNOT_FIND_SSL_AUTHENTICATION_FOR_CURRENT_HOST);
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
