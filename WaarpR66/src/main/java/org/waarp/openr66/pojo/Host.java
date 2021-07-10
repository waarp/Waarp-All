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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.utility.WaarpStringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.sql.Types;

import static org.waarp.common.database.data.AbstractDbData.*;
import static org.waarp.openr66.configuration.FileBasedElements.*;

/**
 * Host data object
 */
@XmlType(name = XML_AUTHENTICATION_ENTRY)
@XmlAccessorType(XmlAccessType.FIELD)
public class Host {
  @XmlElement(name = XML_AUTHENTICATION_HOSTID)
  @JsonProperty("HOSTID")
  private String hostid;

  @XmlElement(name = XML_AUTHENTICATION_ADDRESS)
  @JsonProperty("ADDRESS")
  private String address;

  @XmlElement(name = XML_AUTHENTICATION_PORT)
  @JsonProperty("PORT")
  private int port;

  @XmlTransient
  @JsonProperty("HOSTKEY")
  private byte[] hostkey;

  @XmlElement(name = XML_AUTHENTICATION_ISSSL)
  @JsonProperty("ISSSL")
  private boolean ssl;

  @XmlElement(name = XML_AUTHENTICATION_ISCLIENT)
  @JsonProperty("ISCLIENT")
  private boolean client;

  @XmlElement(name = XML_AUTHENTICATION_ISPROXIFIED)
  @JsonProperty("ISPROXIFIED")
  private boolean proxified;

  @XmlElement(name = XML_AUTHENTICATION_ADMIN)
  @JsonProperty("ADMINROLE")
  private boolean admin = true;

  @XmlElement(name = XML_AUTHENTICATION_ISACTIVE)
  @JsonProperty("ISACTIVE")
  private boolean active = true;

  @JsonProperty("UPDATEDINFO")
  private UpdatedInfo updatedInfo = UpdatedInfo.UNKNOWN;

  /**
   * Empty constructor
   */
  public Host() {
    // Nothing
  }

  public Host(final String hostid, final String address, final int port,
              final byte[] hostkey, final boolean ssl, final boolean client,
              final boolean proxified, final boolean admin,
              final boolean active, final UpdatedInfo updatedInfo)
      throws WaarpDatabaseSqlException {
    this(hostid, address, port, hostkey, ssl, client, proxified, admin, active);
    this.updatedInfo = updatedInfo;
  }

  public Host(final String hostid, final String address, final int port,
              final byte[] hostkey, final boolean ssl, final boolean client,
              final boolean proxified, final boolean admin,
              final boolean active) throws WaarpDatabaseSqlException {
    this.hostid = hostid;
    this.hostkey = hostkey;
    // Force client status if unvalid port
    this.address = address;
    this.port = port;
    this.client = client;
    this.ssl = ssl;
    this.proxified = proxified;
    this.admin = admin;
    this.active = active;
    checkValues();
  }

  public Host(final String hostid, final String address, final int port,
              final byte[] hostkey, final boolean ssl, final boolean client,
              final boolean proxified, final boolean admin)
      throws WaarpDatabaseSqlException {
    this(hostid, address, port, hostkey, ssl, client, proxified, admin, true);
  }

  public Host(final String hostid, final String address, final int port,
              final byte[] hostkey, final boolean ssl, final boolean client)
      throws WaarpDatabaseSqlException {
    this(hostid, address, port, hostkey, ssl, client, false, true, true);
  }

  @JsonIgnore
  public void checkValues() throws WaarpDatabaseSqlException {
    validateLength(hostkey);
    validateLength(Types.NVARCHAR, address, hostid);
  }

  public String getHostid() {
    return hostid;
  }

  public void setHostid(final String hostid) {
    this.hostid = hostid;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(final String address) {
    this.address = address;
  }

  public int getPort() {
    return port;
  }

  public void setPort(final int port) {
    this.port = port;
  }

  public byte[] getHostkey() {
    return hostkey;
  }

  @XmlElement(name = XML_AUTHENTICATION_KEY)
  @JsonIgnore
  public String getKey() {
    return new String(hostkey, WaarpStringUtils.UTF8);
  }

  public void setKey(final String key) {
    hostkey = key.getBytes(WaarpStringUtils.UTF8);
  }

  public void setHostkey(final byte[] hostkey) {
    this.hostkey = hostkey;
  }

  public boolean isSSL() {
    return ssl;
  }

  public void setSSL(final boolean ssl) {
    this.ssl = ssl;
  }

  public boolean isClient() {
    return client;
  }

  public void setClient(final boolean client) {
    this.client = client;
  }

  public boolean isAdmin() {
    return admin;
  }

  public void setAdmin(final boolean admin) {
    this.admin = admin;
  }

  public boolean isProxified() {
    return proxified;
  }

  public void setProxified(final boolean proxified) {
    this.proxified = proxified;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(final boolean active) {
    this.active = active;
  }

  public UpdatedInfo getUpdatedInfo() {
    return updatedInfo;
  }

  public void setUpdatedInfo(final UpdatedInfo info) {
    updatedInfo = info;
  }
}
