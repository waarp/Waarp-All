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

/**
 * Business data object
 */
public class Business {

  private String hostid;

  private String business;

  private String roles;

  private String aliases;

  private String others;

  private UpdatedInfo updatedInfo = UpdatedInfo.UNKNOWN;

  /**
   * Empty constructor
   */
  public Business() {
    // Nothing
  }

  public Business(String hostid, String business, String roles, String aliases,
                  String others, UpdatedInfo updatedInfo) {
    this(hostid, business, roles, aliases, others);
    this.updatedInfo = updatedInfo;
  }

  public Business(String hostid, String business, String roles, String aliases,
                  String others) {
    this.hostid = hostid;
    this.business = business;
    this.roles = roles;
    this.aliases = aliases;
    this.others = others;
  }

  public String getHostid() {
    return hostid;
  }

  public void setHostid(String hostid) {
    this.hostid = hostid;
  }

  public String getBusiness() {
    return business;
  }

  public void setBusiness(String business) {
    this.business = business;
  }

  public String getRoles() {
    return roles;
  }

  public void setRoles(String roles) {
    this.roles = roles;
  }

  public String getAliases() {
    return aliases;
  }

  public void setAliases(String aliases) {
    this.aliases = aliases;
  }

  public String getOthers() {
    return others;
  }

  public void setOthers(String others) {
    this.others = others;
  }

  public UpdatedInfo getUpdatedInfo() {
    return updatedInfo;
  }

  public void setUpdatedInfo(UpdatedInfo info) {
    updatedInfo = info;
  }
}
