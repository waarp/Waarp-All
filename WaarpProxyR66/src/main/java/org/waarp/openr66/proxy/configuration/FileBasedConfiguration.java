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
package org.waarp.openr66.proxy.configuration;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.xml.XmlDecl;
import org.waarp.common.xml.XmlHash;
import org.waarp.common.xml.XmlRootHash;
import org.waarp.common.xml.XmlType;
import org.waarp.common.xml.XmlUtil;
import org.waarp.common.xml.XmlValue;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.proxy.network.ProxyEntry;

import java.net.InetSocketAddress;

import static org.waarp.common.database.DbConstant.*;
import static org.waarp.openr66.configuration.FileBasedConfiguration.*;
import static org.waarp.openr66.configuration.FileBasedElements.*;

/**
 * File Based ConfigurationProxyR66
 */
public class FileBasedConfiguration {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FileBasedConfiguration.class);

  /**
   * SERVER Association entry
   */
  private static final String XML_SERVER_PROXY = "serverproxy";
  /**
   * SERVER Listening address
   */
  private static final String XML_SERVER_LISTENADDR = "serverlistenaddr";
  /**
   * SERVER Listening port
   */
  private static final String XML_SERVER_LISTENPORT = "serverlistenport";
  /**
   * SERVER Listening ssl mode
   */
  private static final String XML_SERVER_LISTENSSL = "serverlistenssl";
  /**
   * SERVER Remote address
   */
  private static final String XML_SERVER_REMOTEADDR = "serverremoteaddr";
  /**
   * SERVER Remote PORT
   */
  private static final String XML_SERVER_REMOTEPORT = "serverremoteport";
  /**
   * SERVER Remote ssl mode
   */
  private static final String XML_SERVER_REMOTESSL = "serverremotessl";

  /**
   * Structure of the ConfigurationProxyR66 file
   */
  private static final XmlDecl[] configServerParamDecls = {
      // server
      new XmlDecl(XmlType.BOOLEAN, XML_USESSL),
      new XmlDecl(XmlType.BOOLEAN, XML_USENOSSL),
      new XmlDecl(XmlType.BOOLEAN, XML_USEHTTPCOMP),
      new XmlDecl(XmlType.STRING, XML_SERVER_ADMIN),
      new XmlDecl(XmlType.STRING, XML_SERVER_PASSWD),
      new XmlDecl(XmlType.STRING, XML_SERVER_PASSWD_FILE),
      new XmlDecl(XmlType.STRING, XML_HTTPADMINPATH),
      new XmlDecl(XmlType.STRING, XML_PATH_ADMIN_KEYPATH),
      new XmlDecl(XmlType.STRING, XML_PATH_ADMIN_KEYSTOREPASS),
      new XmlDecl(XmlType.STRING, XML_PATH_ADMIN_KEYPASS),
      new XmlDecl(XmlType.LONG, XML_MONITOR_PASTLIMIT),
      new XmlDecl(XmlType.LONG, XML_MONITOR_MINIMALDELAY),
      new XmlDecl(XmlType.STRING, XML_MONITOR_SNMP_CONFIG)
  };
  /**
   * Structure of the ConfigurationProxyR66 file
   */
  private static final XmlDecl[] configNetworkProxyDecls = {
      // proxy
      new XmlDecl(XmlType.STRING, XML_SERVER_LISTENADDR),
      new XmlDecl(XmlType.INTEGER, XML_SERVER_LISTENPORT),
      new XmlDecl(XmlType.BOOLEAN, XML_SERVER_LISTENSSL),
      new XmlDecl(XmlType.STRING, XML_SERVER_REMOTEADDR),
      new XmlDecl(XmlType.INTEGER, XML_SERVER_REMOTEPORT),
      new XmlDecl(XmlType.BOOLEAN, XML_SERVER_REMOTESSL)
  };
  /**
   * Structure of the ConfigurationProxyR66 file
   */
  private static final XmlDecl[] configNetworkServerDecls = {
      // network
      new XmlDecl(XML_SERVER_PROXY, XmlType.XVAL, XML_SERVER_PROXY,
                  configNetworkProxyDecls, true),
      new XmlDecl(XmlType.INTEGER, XML_SERVER_HTTPPORT),
      new XmlDecl(XmlType.INTEGER, XML_SERVER_HTTPSPORT)
  };

  /**
   * Structure of the ConfigurationProxyR66 file
   */
  private static final XmlDecl[] configDirectoryDecls = {
      // directory
      new XmlDecl(XmlType.STRING, XML_SERVER_HOME),
      new XmlDecl(XmlType.STRING, XML_ARCHIVEPATH),
      new XmlDecl(XmlType.STRING, XML_CONFIGPATH)
  };

  /**
   * Structure of the ConfigurationProxyR66 file
   */
  private static final XmlDecl[] configLimitDecls = {
      // limit
      new XmlDecl(XmlType.LONG, XML_LIMITSESSION),
      new XmlDecl(XmlType.LONG, XML_LIMITGLOBAL),
      new XmlDecl(XmlType.LONG, XML_LIMITDELAY),
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
      new XmlDecl(XmlType.BOOLEAN, XML_CHECKVERSION)
  };

  /**
   * Global Structure for Server ConfigurationProxyR66
   */
  private static final XmlDecl[] configServer = {
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
                  configLimitDecls, false)
  };

  private static XmlValue[] configuration;
  private static XmlHash hashConfig;

  private FileBasedConfiguration() {
  }

  private static boolean loadServerParam(Configuration config) {
    XmlHash hashConfigSub = new XmlHash(hashConfig.get(XML_SERVER));
    try {
      return org.waarp.openr66.configuration.FileBasedConfiguration
          .loadServerConfig(config, hashConfigSub);
    } finally {
      hashConfigSub.clear();
      hashConfigSub = null;
    }
  }

  private static boolean loadDirectory(Configuration config) {
    XmlHash hashConfigSub = new XmlHash(hashConfig.get(XML_DIRECTORY));
    try {
      if (loadMinimalDirectory(config, hashConfigSub)) {
        return false;
      }
    } finally {
      hashConfigSub.clear();
      hashConfigSub = null;
    }
    return true;
  }

  private static boolean alreadySetLimit;

  private static void loadLimit(Configuration config, boolean updateLimit) {
    if (alreadySetLimit) {
      return;
    }
    XmlHash hashConfigSub = new XmlHash(hashConfig.get(XML_LIMIT));
    try {
      loadCommonLimit(config, hashConfigSub, updateLimit);
      if (config.getRunnerThread() < 10) {
        config.setRunnerThread(10);
      }
      logger.info("Limit of Runner: {}", config.getRunnerThread());
      alreadySetLimit = true;
    } finally {
      hashConfigSub.clear();
    }
  }

  @SuppressWarnings("unchecked")
  private static boolean loadNetworkServer(Configuration config) {
    config.setServerPort(0);
    config.setServerSslPort(0);
    XmlValue value = hashConfig.get(XML_SERVER_HTTPPORT);
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
    // XXX FIXME to change to accept multiple port according to relation local-proxy
    value = hashConfig.get(XML_SERVER_PROXY);
    if (value != null && value.getList() != null) {
      for (final XmlValue[] xml : (Iterable<XmlValue[]>) value.getList()) {
        final XmlHash subHash = new XmlHash(xml);
        value = subHash.get(XML_SERVER_LISTENADDR);
        if (value == null || value.isEmpty()) {
          continue;
        }
        final String listenaddr = value.getString();
        value = subHash.get(XML_SERVER_LISTENPORT);
        if (value == null || value.isEmpty()) {
          continue;
        }
        final int listenport = value.getInteger();
        value = subHash.get(XML_SERVER_LISTENSSL);
        if (value == null || value.isEmpty()) {
          continue;
        }
        final boolean listenssl = value.getBoolean();
        value = subHash.get(XML_SERVER_REMOTEADDR);
        if (value == null || value.isEmpty()) {
          continue;
        }
        final String remoteaddr = value.getString();
        value = subHash.get(XML_SERVER_REMOTEPORT);
        if (value == null || value.isEmpty()) {
          continue;
        }
        final int remoteport = value.getInteger();
        value = subHash.get(XML_SERVER_REMOTESSL);
        if (value == null || value.isEmpty()) {
          continue;
        }
        final boolean remotessl = value.getBoolean();
        final InetSocketAddress local =
            new InetSocketAddress(listenaddr, listenport);
        final InetSocketAddress remote =
            new InetSocketAddress(remoteaddr, remoteport);
        try {
          ProxyEntry.add(local, listenssl, remote, remotessl);
        } catch (final OpenR66ProtocolSystemException e) {
          logger.error("Issue on configuration: {}", e.getMessage());
          return false;
        }
      }
      if (ProxyEntry.proxyEntries.isEmpty()) {
        logger.error("No proxy configuration found");
        return false;
      }
    } else {
      logger.error("No proxy configuration found");
      return false;
    }
    return true;
  }

  /**
   * Load database parameter
   *
   * @param config
   *
   * @return True if OK
   */
  private static boolean loadDatabase(Configuration config) {
    logger.info("Unable to find DBDriver in Config file");
    admin = new DbAdmin(); // no database support
    noCommitAdmin = admin;
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
  public static boolean setConfigurationProxyFromXml(Configuration config,
                                                     String filename) {
    Document document;
    // Open config file
    try {
      document = new SAXReader().read(filename);
    } catch (final DocumentException e) {
      logger.error("Unable to read the XML Config file: " + filename, e);
      return false;
    }
    if (document == null) {
      logger.error("Unable to read the XML Config file: " + filename);
      return false;
    }
    configuration = XmlUtil.read(document, configServer);
    hashConfig = new XmlHash(configuration);
    XmlRootHash xmlRootHash = new XmlRootHash(configuration);
    setXmlRootHash(xmlRootHash);
    // Now read the configuration
    if (!loadIdentity(config, xmlRootHash)) {
      logger.error("Cannot load Identity");
      return false;
    }
    if (!loadDatabase(config)) {
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
    loadLimit(config, false);
    if (config.isUseSSL() && !loadSsl(config, xmlRootHash)) {
      logger.error("Cannot load SSL configuration");
      return false;
    }
    if (!loadNetworkServer(config)) {
      logger.error("Cannot load Network configuration");
      return false;
    }
    hashConfig.clear();
    hashConfig = null;
    xmlRootHash.clear();
    configuration = null;
    return true;
  }

}
