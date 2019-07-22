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

package org.waarp.openr66.pojo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import static org.waarp.openr66.configuration.AuthenticationFileBasedConfiguration.*;

/**
 * Host data object
 */
@XmlType(name = XML_AUTHENTIFICATION_ENTRY)
@XmlAccessorType(XmlAccessType.FIELD)
public class Host {
  private static final String DEFAULT_CLIENT_ADDRESS = "0.0.0.0";
  private static final int DEFAULT_CLIENT_PORT = 0;

  @XmlElement(name = XML_AUTHENTIFICATION_HOSTID)
  private String hostid;

  @XmlElement(name = XML_AUTHENTIFICATION_ADDRESS)
  private String address;

  @XmlElement(name = XML_AUTHENTIFICATION_PORT)
  private int port;

  @XmlTransient
  private byte[] hostkey;

  @XmlElement(name = XML_AUTHENTIFICATION_ISSSL)
  private boolean ssl;

  @XmlElement(name = XML_AUTHENTIFICATION_ISCLIENT)
  private boolean client;

  @XmlElement(name = XML_AUTHENTIFICATION_ISPROXIFIED)
  private boolean proxified = false;

  @XmlElement(name = XML_AUTHENTIFICATION_ADMIN)
  private boolean admin = true;

  @XmlElement(name = XML_AUTHENTIFICATION_ISACTIVE)
  private boolean active = true;

  private UpdatedInfo updatedInfo = UpdatedInfo.UNKNOWN;

  /**
   * Empty constructor for compatibility issues
   */
  @Deprecated
  public Host() {
  }

  public Host(String hostid, String address, int port, byte[] hostkey,
              boolean ssl, boolean client, boolean proxified, boolean admin,
              boolean active, UpdatedInfo updatedInfo) {
    this(hostid, address, port, hostkey, ssl, client, proxified, admin, active);
    this.updatedInfo = updatedInfo;
  }

  public Host(String hostid, String address, int port, byte[] hostkey,
              boolean ssl, boolean client, boolean proxified, boolean admin,
              boolean active) {
    this.hostid = hostid;
    this.hostkey = hostkey;
    // Force client status if unvalid port
    if (port < 1) {
      this.address = DEFAULT_CLIENT_ADDRESS;
      this.port = DEFAULT_CLIENT_PORT;
      this.client = true;
    } else {
      this.address = address;
      this.port = port;
      this.client = client;
    }
    this.ssl = ssl;
    this.proxified = proxified;
    this.admin = admin;
    this.active = active;
  }

  public Host(String hostid, String address, int port, byte[] hostkey,
              boolean ssl, boolean client, boolean proxified, boolean admin) {
    this(hostid, address, port, hostkey, ssl, client, proxified, admin, true);
  }

  public Host(String hostid, String address, int port, byte[] hostkey,
              boolean ssl, boolean client) {
    this(hostid, address, port, hostkey, ssl, client, false, true);
  }

  public String getHostid() {
    return hostid;
  }

  public void setHostid(String hostid) {
    this.hostid = hostid;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public byte[] getHostkey() {
    return hostkey;
  }

  @XmlElement(name = XML_AUTHENTIFICATION_KEY)
  public String getKey() {
    return new String(hostkey);
  }

  public void setKey(String key) {
    hostkey = key.getBytes();
  }

  public void setHostkey(byte[] hostkey) {
    this.hostkey = hostkey;
  }

  public boolean isSSL() {
    return ssl;
  }

  public void setSSL(boolean ssl) {
    this.ssl = ssl;
  }

  public boolean isClient() {
    return client;
  }

  public void setClient(boolean client) {
    this.client = client;
  }

  public boolean isAdmin() {
    return admin;
  }

  public void setAdmin(boolean admin) {
    this.admin = admin;
  }

  public boolean isProxified() {
    return proxified;
  }

  public void setProxified(boolean proxified) {
    this.proxified = proxified;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public UpdatedInfo getUpdatedInfo() {
    return updatedInfo;
  }

  public void setUpdatedInfo(UpdatedInfo info) {
    updatedInfo = info;
  }
}
