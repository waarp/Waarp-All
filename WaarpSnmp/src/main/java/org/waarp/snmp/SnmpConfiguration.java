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
package org.waarp.snmp;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.TransportDomains;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.Priv3DES;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES192;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.TransportIpAddress;
import org.snmp4j.smi.UdpAddress;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.xml.XmlDecl;
import org.waarp.common.xml.XmlHash;
import org.waarp.common.xml.XmlType;
import org.waarp.common.xml.XmlUtil;
import org.waarp.common.xml.XmlValue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * SnmpConfiguration class from XML file
 */
public final class SnmpConfiguration {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(SnmpConfiguration.class);

  private static final String SNMP_ROOT = "/snmpconfig/";

  private static final String SNMP_CONFIG = "config";

  private static final String SNMP_LOCAL_ADDRESS = "localaddress";

  private static final String SNMP_NBTHREAD = "nbthread";

  private static final String SNMP_FILTERED = "filtered";

  private static final String SNMP_USETRAP = "usetrap";

  private static final String SNMP_TRAPLEVEL = "trapinformlevel";

  private static final XmlDecl[] configConfigDecls = {
      new XmlDecl(SNMP_LOCAL_ADDRESS, XmlType.STRING, SNMP_LOCAL_ADDRESS, true),
      new XmlDecl(XmlType.INTEGER, SNMP_NBTHREAD),
      new XmlDecl(XmlType.BOOLEAN, SNMP_FILTERED),
      new XmlDecl(XmlType.BOOLEAN, SNMP_USETRAP),
      new XmlDecl(XmlType.INTEGER, SNMP_TRAPLEVEL)
  };

  private static final String SNMP_TARGETS = "targets";

  private static final String SNMP_TARGET = "target";

  /**
   * Static String
   */
  static final String NOTIFY = "notify";

  /**
   * Static String
   */
  static final String V3NOTIFY = "v3notify";

  /**
   * Static String
   */
  static final String V2C = "v2c";

  /*
   * addTargetAddress(org.snmp4j.smi.OctetString name, org.snmp4j.smi.OID transportDomain,
   * org.snmp4j.smi.OctetString address, int timeout, int retries, org.snmp4j.smi.OctetString tagList, "notify"
   * org.snmp4j.smi.OctetString params, "v3notify"/"v2c" int storageType) permenant
   */
  private static final String SNMP_TARGET_NAME = "name";

  private static final String SNMP_TARGET_DOMAIN = "domain";

  private static final String SNMP_TARGET_ADDRESS = "address";

  private static final String SNMP_TARGET_TIMEOUT = "timeout";

  private static final String SNMP_TARGET_RETRIES = "retries";

  private static final String SNMP_TARGET_ISV2 = "isv2";

  private static final XmlDecl[] configTargetDecls = {
      new XmlDecl(XmlType.STRING, SNMP_TARGET_NAME),
      new XmlDecl(XmlType.STRING, SNMP_TARGET_DOMAIN),
      new XmlDecl(XmlType.STRING, SNMP_TARGET_ADDRESS),
      new XmlDecl(XmlType.INTEGER, SNMP_TARGET_TIMEOUT),
      new XmlDecl(XmlType.INTEGER, SNMP_TARGET_RETRIES),
      new XmlDecl(XmlType.BOOLEAN, SNMP_TARGET_ISV2)
  };

  /*
   * org.snmp4j.security.UsmUser.UsmUser(OctetString securityName, OID authenticationProtocol, OctetString
   * authenticationPassphrase, OID privacyProtocol, OctetString privacyPassphrase)
   *
   * Creates a USM user.
   *
   * Parameters:
   *
   * -securityName the security name of the user (typically the user name).
   *
   * -authenticationProtocol the authentication protcol ID to be associated with this user. If set to null, this
   * user only supports unauthenticated messages.
   *
   * -authenticationPassphrase the authentication passphrase. If not null, authenticationProtocol must also be
   * not null. RFC3414 §11.2 requires passphrases to have a minimum length of 8 bytes. If the length of
   * authenticationPassphrase is less than 8 bytes an IllegalArgumentException is thrown.
   *
   * -privacyProtocol the privacy protcol ID to be associated with this user. If set to null, this user only
   * supports unencrypted messages.
   *
   * -privacyPassphrase the privacy passphrase. If not null, privacyProtocol must also be not null. RFC3414
   * §11.2 requires passphrases to have a minimum length of 8 bytes. If the length of authenticationPassphrase
   * is less than 8 bytes an IllegalArgumentException is thrown.
   */
  private static final String SNMP_SECURITIES = "securities";

  private static final String SNMP_SECURITY = "security";

  private static final String SNMP_SECURITY_NAME = "securityname";

  private static final String SNMP_SECURITY_AUTH_PROTOCOL =
      "securityauthprotocol";

  private static final String SNMP_SECURITY_AUTH_PSSPHRASE = "securityauthpass";

  private static final String SNMP_SECURITY_PRIV_PROTOCOL =
      "securityprivprotocol";

  private static final String SNMP_SECURITY_PRIV_PSSPHRASE = "securityprivpass";

  private static final XmlDecl[] configSecurityDecls = {
      new XmlDecl(XmlType.STRING, SNMP_SECURITY_NAME),
      new XmlDecl(XmlType.STRING, SNMP_SECURITY_AUTH_PROTOCOL),
      new XmlDecl(XmlType.STRING, SNMP_SECURITY_AUTH_PSSPHRASE),
      new XmlDecl(XmlType.STRING, SNMP_SECURITY_PRIV_PROTOCOL),
      new XmlDecl(XmlType.STRING, SNMP_SECURITY_PRIV_PSSPHRASE)
  };

  private static final XmlDecl[] configSNMP = {
      new XmlDecl(SNMP_CONFIG, XmlType.XVAL, SNMP_ROOT + SNMP_CONFIG,
                  configConfigDecls, false),
      new XmlDecl(SNMP_SECURITY, XmlType.XVAL,
                  SNMP_ROOT + SNMP_SECURITIES + '/' + SNMP_SECURITY,
                  configSecurityDecls, true),
      new XmlDecl(SNMP_TARGET, XmlType.XVAL,
                  SNMP_ROOT + SNMP_TARGETS + '/' + SNMP_TARGET,
                  configTargetDecls, true)
  };

  private static XmlValue[] configuration;

  private static XmlHash hashConfig;
  /**
   * Address from the configuration for the SNMP Agent listening port
   */
  static String[] address;
  /**
   * Number of threads to use in SNMP agent
   */
  static int nbThread = 4;
  /**
   * Do we filter on Targets for SNMP requests
   */
  static boolean isFilterAccessEnabled;
  /**
   * Do we are using Trap or Inform
   */
  static boolean isUsingTrap = true;
  /**
   * Level for Trap/Inform from 0 to 4
   */
  static int trapLevel;
  /**
   * Default address: all in UDP port 161
   */
  public static final String DEFAULTADDRESS = "udp:0.0.0.0/161";

  private SnmpConfiguration() {
  }

  /**
   * @return True if the configuration successfully load
   */
  private static boolean loadConfig() {
    XmlValue value = hashConfig.get(SNMP_LOCAL_ADDRESS);
    @SuppressWarnings("unchecked")
    final List<String> values = (List<String>) value.getList();
    final int length = values.size();
    if (length == 0) {
      address = new String[] { DEFAULTADDRESS };
    } else {
      address = new String[length];
      int nb = 0;
      address = values.toArray(address);
      final String[] tmp = new String[length];
      for (int j = 0; j < length; j++) {
        if (address[j] != null && !address[j].trim().isEmpty()) {
          tmp[nb] = address[j];
          nb++;
        }
      }
      if (nb == 0) {
        address = new String[] { DEFAULTADDRESS };
      } else if (nb < length) {
        // less addresses than intended
        address = new String[nb];
        System.arraycopy(tmp, 0, address, 0, nb);
      }
    }
    value = hashConfig.get(SNMP_NBTHREAD);
    if (value != null && !value.isEmpty()) {
      nbThread = value.getInteger();
      if (nbThread <= 0) {
        nbThread = 4;
      }
    }
    value = hashConfig.get(SNMP_FILTERED);
    if (value != null && !value.isEmpty()) {
      isFilterAccessEnabled = value.getBoolean();
    }
    value = hashConfig.get(SNMP_USETRAP);
    if (value != null && !value.isEmpty()) {
      isUsingTrap = value.getBoolean();
    }
    value = hashConfig.get(SNMP_TRAPLEVEL);
    if (value != null && !value.isEmpty()) {
      trapLevel = value.getInteger();
    }
    return true;
  }

  /**
   * List of all UsmUser
   */
  static final List<UsmUser> listUsmUser = new ArrayList<UsmUser>();

  /**
   * Protocols for Security
   */
  public enum SecurityProtocolList {
    SHA(AuthSHA.ID), MD5(AuthMD5.ID);

    public final OID oid;

    SecurityProtocolList(OID oid) {
      this.oid = oid;
    }
  }

  /**
   * Protocol for Privacy
   */
  public enum PrivacyProtocolList {
    P3DES(Priv3DES.ID), PAES128(PrivAES128.ID), PAES192(PrivAES192.ID),
    PAES256(PrivAES256.ID), PDES(PrivDES.ID);

    public final OID oid;

    PrivacyProtocolList(OID oid) {
      this.oid = oid;
    }
  }

  /**
   * new XmlDecl(XmlType.STRING, SNMP_SECURITY_NAME), new
   * XmlDecl(XmlType.STRING, SNMP_SECURITY_AUTH_PROTOCOL),
   * new XmlDecl(XmlType.STRING, SNMP_SECURITY_AUTH_PASSPHRASE), new
   * XmlDecl(XmlType.STRING,
   * SNMP_SECURITY_PRIV_PROTOCOL), new XmlDecl(XmlType.STRING,
   * SNMP_SECURITY_PRIV_PASSPHRASE)
   *
   * @return True if load successfully
   */
  private static boolean loadSecurity() {
    String securityName;
    String securityProtocol;
    String securityPassphrase;
    String securityPrivProtocol;
    String securityPrivPassphrase;
    XmlValue value = hashConfig.get(SNMP_SECURITY);
    @SuppressWarnings("unchecked")
    final List<XmlValue[]> list = (List<XmlValue[]>) value.getList();
    for (final XmlValue[] xmlValues : list) {
      securityPassphrase = null;
      securityPrivPassphrase = null;
      final XmlHash subHash = new XmlHash(xmlValues);
      value = subHash.get(SNMP_SECURITY_NAME);
      if (value == null || value.isEmpty()) {
        logger.warn("No Security Name found");
        continue;
      }
      securityName = value.getString();
      value = subHash.get(SNMP_SECURITY_AUTH_PROTOCOL);
      SecurityProtocolList secprot = null;
      if (value != null && !value.isEmpty()) {
        securityProtocol = value.getString();
        try {
          secprot = SecurityProtocolList.valueOf(securityProtocol);
        } catch (final IllegalArgumentException e) {
          logger.warn("No Security Protocol found for " + securityName);
          continue;
        }
        value = subHash.get(SNMP_SECURITY_AUTH_PSSPHRASE);
        if (value == null || value.isEmpty()) {
          // not allowed
          securityProtocol = null;
        } else {
          securityPassphrase = value.getString();
        }
      }
      value = subHash.get(SNMP_SECURITY_PRIV_PROTOCOL);
      PrivacyProtocolList privprot = null;
      if (value != null && !value.isEmpty()) {
        securityPrivProtocol = value.getString();
        try {
          privprot = PrivacyProtocolList.valueOf(securityPrivProtocol);
        } catch (final IllegalArgumentException e) {
          logger.warn("No Security Private Protocol found for " + securityName);
          continue;
        }
        value = subHash.get(SNMP_SECURITY_PRIV_PSSPHRASE);
        if (value == null || value.isEmpty()) {
          // not allowed
          securityPrivProtocol = null;
        } else {
          securityPrivPassphrase = value.getString();
        }
      }
      final UsmUser usm = new UsmUser(new OctetString(securityName),
                                      secprot == null? null : secprot.oid,
                                      secprot == null? null :
                                          new OctetString(securityPassphrase),
                                      privprot == null? null : privprot.oid,
                                      privprot == null? null : new OctetString(
                                          securityPrivPassphrase));
      listUsmUser.add(usm);
    }
    return true;
  }

  private enum TransportDomain {
    UdpIpv4(TransportDomains.transportDomainUdpIpv4),
    UdpIpv6(TransportDomains.transportDomainUdpIpv6),
    UdpIpv4z(TransportDomains.transportDomainUdpIpv4z),
    UdpIpv6z(TransportDomains.transportDomainUdpIpv6z),
    TcpIpv4(TransportDomains.transportDomainTcpIpv4),
    TcpIpv6(TransportDomains.transportDomainTcpIpv6),
    TcpIpv4z(TransportDomains.transportDomainTcpIpv4z),
    TcpIpv6z(TransportDomains.transportDomainTcpIpv6z);

    public final OID oid;

    TransportDomain(OID oid) {
      this.oid = oid;
    }
  }

  /**
   * Target entry
   */
  public static class TargetElement {
    public final OctetString name;

    public final OID transportDomain;

    public final OctetString address;

    public final int timeout;

    public final int retries;

    public final OctetString tagList;

    public final OctetString params;

    public final int storageType;

    /**
     * @param name
     * @param transportDomain
     * @param address
     * @param timeout
     * @param retries
     * @param tagList
     * @param params
     * @param storageType
     */
    private TargetElement(OctetString name, OID transportDomain,
                          OctetString address, int timeout, int retries,
                          OctetString tagList, OctetString params,
                          int storageType) {
      this.name = name;
      this.transportDomain = transportDomain;
      this.address = address;
      this.timeout = timeout;
      this.retries = retries;
      this.tagList = tagList;
      this.params = params;
      this.storageType = storageType;
    }

    @Override
    public String toString() {
      return "Name: " + name + " TD: " + transportDomain + " Add: " + address +
             " TO: " + timeout + " RT: " + retries + " TL: " + tagList +
             " PM: " + params + " ST: " + storageType;
    }
  }

  /**
   * List of Target Element
   */
  static final List<TargetElement> listTargetElements =
      new ArrayList<TargetElement>();
  /**
   * Do we use SNMP V2c
   */
  static boolean hasV2;
  /**
   * Do we use SNMP V3
   */
  static boolean hasV3;

  /**
   * new XmlDecl(XmlType.STRING, SNMP_TARGET_NAME), free name new
   * XmlDecl(XmlType.STRING, SNMP_TARGET_DOMAIN),
   * one of (Udp/Tcp)Ipv(4/6)[z] new XmlDecl(XmlType.STRING,
   * SNMP_TARGET_ADDRESS), new XmlDecl(XmlType.INTEGER,
   * SNMP_TARGET_TIMEOUT), new XmlDecl(XmlType.INTEGER, SNMP_TARGET_RETRIES),
   * new XmlDecl(XmlType.BOOLEAN,
   * SNMP_TARGET_ISV2) True => v2, else v3
   * <p>
   * new OctetString("notificationV2c"), TransportDomains.transportDomainUdpIpv4,
   * new OctetString( new
   * UdpAddress(toAddressV2).getValue()), 200, 1, new OctetString("notify"),
   * new
   * OctetString("v2c"),
   * StorageType.permanent
   * <p>
   * new OctetString("notificationV3"), TransportDomains.transportDomainUdpIpv4,
   * new OctetString( new
   * UdpAddress(toAddressV3).getValue()), 200, 1, new OctetString("notify"),
   * new
   * OctetString("v3notify"),
   * StorageType.permanent
   *
   * @return True if successfully loaded
   */
  private static boolean loadTarget() {
    String targetName;
    String targetDomain;
    OID oTargetDomain;
    String targetAddress;
    int targetTimeout;
    int targetRetries;
    String targetParams;
    XmlValue value = hashConfig.get(SNMP_TARGET);
    @SuppressWarnings("unchecked")
    final List<XmlValue[]> list = (List<XmlValue[]>) value.getList();
    for (final XmlValue[] xmlValues : list) {
      final XmlHash subHash = new XmlHash(xmlValues);
      value = subHash.get(SNMP_TARGET_NAME);
      if (value == null || value.isEmpty()) {
        logger.warn("No Target Name found");
        continue;
      }
      targetName = value.getString();
      value = subHash.get(SNMP_TARGET_DOMAIN);
      if (value == null || value.isEmpty()) {
        logger.warn("No Target Domain found for " + targetName);
        continue;
      }
      targetDomain = value.getString();
      TransportDomain domain;
      try {
        domain = TransportDomain.valueOf(targetDomain);
        oTargetDomain = domain.oid;
      } catch (final IllegalArgumentException e) {
        logger.warn("No Target Domain correctly found for " + targetName);
        continue;
      }
      value = subHash.get(SNMP_TARGET_ADDRESS);
      if (value == null || value.isEmpty()) {
        logger.warn("No Target Address found for " + targetName);
        continue;
      }
      targetAddress = value.getString();
      TransportIpAddress address = null;
      try {
        switch (domain) {
          case UdpIpv4:
          case UdpIpv6:
          case UdpIpv4z:
          case UdpIpv6z:
            address = new UdpAddress(targetAddress);
            break;
          case TcpIpv4:
          case TcpIpv6:
          case TcpIpv4z:
          case TcpIpv6z:
            address = new TcpAddress(targetAddress);
            break;
        }
      } catch (final IllegalArgumentException e) {
        logger.warn("No Correct Target Address found for " + targetName);
        continue;
      }
      if (address != null) {
        logger.debug("Addr: {} {}", address.getClass(), targetAddress);
      }
      value = subHash.get(SNMP_TARGET_TIMEOUT);
      if (value == null || value.isEmpty()) {
        targetTimeout = 200;
      } else {
        targetTimeout = value.getInteger();
      }
      if (targetTimeout <= 100) {
        targetTimeout = 100;
      }
      value = subHash.get(SNMP_TARGET_RETRIES);
      if (value == null || value.isEmpty()) {
        targetRetries = 1;
      } else {
        targetRetries = value.getInteger();
      }
      if (targetRetries <= 0) {
        targetRetries = 1;
      }
      value = subHash.get(SNMP_TARGET_ISV2);
      boolean isV2 = true;
      if (value == null || value.isEmpty()) {
        isV2 = true;
      } else {
        isV2 = value.getBoolean();
      }
      if (isV2) {
        hasV2 = true;
        targetParams = V2C;
      } else {
        hasV3 = true;
        targetParams = V3NOTIFY;
      }
      final TargetElement element =
          new TargetElement(new OctetString(targetName), oTargetDomain,
                            new OctetString(address.getValue()), targetTimeout,
                            targetRetries, new OctetString(NOTIFY),
                            new OctetString(targetParams),
                            StorageType.permanent);
      listTargetElements.add(element);
    }
    return true;
  }

  /**
   * Initiate the configuration from the xml file for SNMP agent
   *
   * @param file
   *
   * @return True if OK
   */
  public static boolean setConfigurationFromXml(File file) {
    Document document;
    // Open config file
    try {
      document = new SAXReader().read(file);
    } catch (final DocumentException e) {
      logger.error(
          "Unable to read the XML Config file: " + file.getAbsolutePath(), e);
      return false;
    }
    if (document == null) {
      logger.error(
          "Unable to read the XML Config file: " + file.getAbsolutePath());
      return false;
    }
    configuration = XmlUtil.read(document, configSNMP);
    hashConfig = new XmlHash(configuration);
    address = new String[] { DEFAULTADDRESS };
    nbThread = 4;
    listUsmUser.clear();
    listTargetElements.clear();
    try {
      // Now read the configuration
      if (!loadConfig()) {
        return false;
      }
      if (!loadSecurity()) {
        return false;
      }
      if (!loadTarget()) {
        return false;
      }
    } finally {
      hashConfig.clear();
      hashConfig = null;
      configuration = null;
    }
    return true;
  }
}
