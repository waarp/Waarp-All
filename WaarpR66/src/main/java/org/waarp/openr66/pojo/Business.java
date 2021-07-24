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

import java.sql.Types;

import static org.waarp.common.database.data.AbstractDbData.*;

/**
 * Business data object
 */
public class Business {

  @JsonProperty("HOSTID")
  private String hostid;

  @JsonProperty("BUSINESS")
  private String business;

  @JsonProperty("ROLES")
  private String roles;

  @JsonProperty("ALIASES")
  private String aliases;

  @JsonProperty("OTHERS")
  private String others;

  @JsonProperty("UPDATEDINFO")
  private UpdatedInfo updatedInfo = UpdatedInfo.UNKNOWN;

  /**
   * Empty constructor
   */
  public Business() {
    // Nothing
  }

  public Business(final String hostid, final String business,
                  final String roles, final String aliases, final String others,
                  final UpdatedInfo updatedInfo)
      throws WaarpDatabaseSqlException {
    this(hostid, business, roles, aliases, others);
    this.updatedInfo = updatedInfo;
  }

  public Business(final String hostid, final String business,
                  final String roles, final String aliases, final String others)
      throws WaarpDatabaseSqlException {
    this.hostid = hostid;
    this.business = business;
    this.roles = roles;
    this.aliases = aliases;
    this.others = others;
    checkValues();
  }

  @JsonIgnore
  public final void checkValues() throws WaarpDatabaseSqlException {
    validateLength(Types.LONGVARCHAR, business, roles, aliases, others);
    validateLength(Types.NVARCHAR, hostid);
  }

  public final String getHostid() {
    return hostid;
  }

  public final void setHostid(final String hostid) {
    this.hostid = hostid;
  }

  public final String getBusiness() {
    return business;
  }

  public final void setBusiness(final String business) {
    this.business = business;
  }

  public final String getRoles() {
    return roles;
  }

  public final void setRoles(final String roles) {
    this.roles = roles;
  }

  public final String getAliases() {
    return aliases;
  }

  public final void setAliases(final String aliases) {
    this.aliases = aliases;
  }

  public final String getOthers() {
    return others;
  }

  public final void setOthers(final String others) {
    this.others = others;
  }

  public final UpdatedInfo getUpdatedInfo() {
    return updatedInfo;
  }

  public final void setUpdatedInfo(final UpdatedInfo info) {
    updatedInfo = info;
  }
}
