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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.xml.XmlHash;
import org.waarp.common.xml.XmlUtil;
import org.waarp.common.xml.XmlValue;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import static org.waarp.openr66.configuration.FileBasedElements.*;

/**
 * Authentication from File support
 */
public class AuthenticationFileBasedConfiguration {
  private static final String CANNOT_READ_KEY_FOR_HOST_ID =
      "Cannot read key for hostId ";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AuthenticationFileBasedConfiguration.class);

  private AuthenticationFileBasedConfiguration() {
  }

  /**
   * Load Authentication from File
   *
   * @param filename
   *
   * @return True if OK
   */
  @SuppressWarnings("unchecked")
  public static boolean loadAuthentication(final Configuration config,
                                           final String filename) {
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
    final XmlValue[] values = XmlUtil.read(document, authentElements);
    final XmlHash hash = new XmlHash(values);
    XmlValue value = hash.get(XML_AUTHENTIFICATION_ENTRY);
    final List<XmlValue[]> list = (List<XmlValue[]>) value.getList();
    final Iterator<XmlValue[]> iterator = list.iterator();
    File key;
    byte[] byteKeys;
    while (iterator.hasNext()) {
      final XmlValue[] subvalues = iterator.next();
      final XmlHash subHash = new XmlHash(subvalues);
      value = subHash.get(XML_AUTHENTIFICATION_HOSTID);
      if (value == null || value.isEmpty()) {
        continue;
      }
      final String refHostId = value.getString();
      value = subHash.get(XML_AUTHENTIFICATION_KEYFILE);
      if (value == null || value.isEmpty()) {
        value = subHash.get(XML_AUTHENTIFICATION_KEY);
        if (value == null || value.isEmpty()) {
          // Allow empty key
          byteKeys = null;
        } else {
          final String skey = value.getString();
          // key is crypted
          if (!skey.isEmpty()) {
            try {
              byteKeys = config.getCryptoKey().decryptHexInBytes(skey);
            } catch (final Exception e) {
              logger
                  .error(CANNOT_READ_KEY_FOR_HOST_ID + refHostId + ':' + skey);
              continue;
            }
          } else {
            byteKeys = null;
          }
        }
      } else {
        final String skey = value.getString();
        // load key from file
        key = new File(skey);
        if (!key.canRead()) {
          logger.error(CANNOT_READ_KEY_FOR_HOST_ID + refHostId + ':' + skey);
          continue;
        }
        try {
          byteKeys = config.getCryptoKey().decryptHexFile(key);
        } catch (final Exception e2) {
          logger.error(CANNOT_READ_KEY_FOR_HOST_ID + refHostId, e2);
          continue;
        }
      }
      boolean isAdmin = false;
      value = subHash.get(XML_AUTHENTIFICATION_ADMIN);
      if (value != null && !value.isEmpty()) {
        isAdmin = value.getBoolean();
      }
      value = subHash.get(XML_AUTHENTIFICATION_ADDRESS);
      if (value == null || value.isEmpty()) {
        continue;
      }
      final String address = value.getString();
      final int port;
      value = subHash.get(XML_AUTHENTIFICATION_PORT);
      if (value != null && !value.isEmpty()) {
        port = value.getInteger();
      } else {
        continue;
      }
      boolean isSsl = false;
      value = subHash.get(XML_AUTHENTIFICATION_ISSSL);
      if (value != null && !value.isEmpty()) {
        isSsl = value.getBoolean();
      }
      boolean isClient = false;
      value = subHash.get(XML_AUTHENTIFICATION_ISCLIENT);
      if (value != null && !value.isEmpty()) {
        isClient = value.getBoolean();
      }
      boolean isActive = true;
      value = subHash.get(XML_AUTHENTIFICATION_ISACTIVE);
      if (value != null && !value.isEmpty()) {
        isActive = value.getBoolean();
      }
      boolean isProxified = false;
      value = subHash.get(XML_AUTHENTIFICATION_ISPROXIFIED);
      if (value != null && !value.isEmpty()) {
        isProxified = value.getBoolean();
      }
      final DbHostAuth auth =
          new DbHostAuth(refHostId, address, port, isSsl, byteKeys, isAdmin,
                         isClient);
      auth.setActive(isActive);
      auth.setProxified(isProxified);
      try {
        if (auth.exist()) {
          auth.update();
        } else {
          auth.insert();
        }
      } catch (final WaarpDatabaseException e) {
        logger.error("Cannot create Authentication for hostId {}", refHostId);
        continue;
      }
      logger.debug("Add {} {}", refHostId, auth);
    }
    hash.clear();
    return true;
  }

  /**
   * Construct a new Element with value
   *
   * @param name
   * @param value
   *
   * @return the new Element
   */
  private static Element newElement(final String name, final String value) {
    final Element node = new DefaultElement(name);
    node.addText(value);
    return node;
  }

  /**
   * Write all authentication to a file with filename
   *
   * @param filename
   *
   * @throws OpenR66ProtocolSystemException
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static void writeXML(final Configuration config, final String filename)
      throws OpenR66ProtocolSystemException,
             WaarpDatabaseNoConnectionException {
    final Document document = DocumentHelper.createDocument();
    final Element root = document.addElement(XML_AUTHENTIFICATION_ROOT);
    final DbHostAuth[] hosts = DbHostAuth.getAllHosts();
    logger.debug("Will write DbHostAuth: {} in {}", hosts.length, filename);
    for (final DbHostAuth auth : hosts) {
      logger.debug("Will write DbHostAuth: {}", auth.getHostid());
      final Element entry = new DefaultElement(XML_AUTHENTIFICATION_ENTRY);
      entry.add(newElement(XML_AUTHENTIFICATION_HOSTID, auth.getHostid()));
      final byte[] key = auth.getHostkey();
      String encode;
      try {
        encode = config.getCryptoKey().cryptToHex(key);
      } catch (final Exception e) {
        encode = "";
      }
      entry.add(newElement(XML_AUTHENTIFICATION_KEY, encode));
      entry.add(newElement(XML_AUTHENTIFICATION_ADMIN,
                           Boolean.toString(auth.isAdminrole())));
      entry.add(newElement(XML_AUTHENTIFICATION_ADDRESS, auth.getAddress()));
      entry.add(newElement(XML_AUTHENTIFICATION_PORT,
                           Integer.toString(auth.getPort())));
      entry.add(newElement(XML_AUTHENTIFICATION_ISSSL,
                           Boolean.toString(auth.isSsl())));
      entry.add(newElement(XML_AUTHENTIFICATION_ISCLIENT,
                           Boolean.toString(auth.isClient())));
      entry.add(newElement(XML_AUTHENTIFICATION_ISACTIVE,
                           Boolean.toString(auth.isActive())));
      entry.add(newElement(XML_AUTHENTIFICATION_ISPROXIFIED,
                           Boolean.toString(auth.isProxified())));
      root.add(entry);
    }
    try {
      XmlUtil.writeXML(filename, null, document);
    } catch (final IOException e) {
      throw new OpenR66ProtocolSystemException("Cannot write file: " + filename,
                                               e);
    }
  }
}
