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
package org.waarp.openr66.protocol.localhandler.packet.json;

import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;

import static org.waarp.common.database.DbConstant.*;

/**
 * Import Configuration JSON packet
 */
public class ConfigImportJsonPacket extends JsonPacket {

  protected boolean purgehost;
  protected boolean purgerule;
  protected boolean purgebusiness;
  protected boolean purgealias;
  protected boolean purgeroles;
  protected String host;
  protected String rule;
  protected String business;
  protected String alias;
  protected String roles;
  protected long hostid = ILLEGALVALUE;
  protected long ruleid = ILLEGALVALUE;
  protected long businessid = ILLEGALVALUE;
  protected long aliasid = ILLEGALVALUE;
  protected long rolesid = ILLEGALVALUE;

  /**
   * @return the purgehost
   */
  public final boolean isPurgehost() {
    return purgehost;
  }

  /**
   * @param purgehost the purgehost to set
   */
  public final void setPurgehost(final boolean purgehost) {
    this.purgehost = purgehost;
  }

  /**
   * @return the purgerule
   */
  public final boolean isPurgerule() {
    return purgerule;
  }

  /**
   * @param purgerule the purgerule to set
   */
  public final void setPurgerule(final boolean purgerule) {
    this.purgerule = purgerule;
  }

  /**
   * @return the purgebusiness
   */
  public final boolean isPurgebusiness() {
    return purgebusiness;
  }

  /**
   * @param purgebusiness the purgebusiness to set
   */
  public final void setPurgebusiness(final boolean purgebusiness) {
    this.purgebusiness = purgebusiness;
  }

  /**
   * @return the purgealias
   */
  public final boolean isPurgealias() {
    return purgealias;
  }

  /**
   * @param purgealias the purgealias to set
   */
  public final void setPurgealias(final boolean purgealias) {
    this.purgealias = purgealias;
  }

  /**
   * @return the purgeroles
   */
  public final boolean isPurgeroles() {
    return purgeroles;
  }

  /**
   * @param purgeroles the purgeroles to set
   */
  public final void setPurgeroles(final boolean purgeroles) {
    this.purgeroles = purgeroles;
  }

  /**
   * @return the host
   */
  public final String getHost() {
    return host;
  }

  /**
   * @param host the host to set
   */
  public final void setHost(final String host) {
    this.host = host;
  }

  /**
   * @return the rule
   */
  public String getRule() {
    return rule;
  }

  /**
   * @param rule the rule to set
   */
  public final void setRule(final String rule) {
    this.rule = rule;
  }

  /**
   * @return the business
   */
  public final String getBusiness() {
    return business;
  }

  /**
   * @param business the business to set
   */
  public final void setBusiness(final String business) {
    this.business = business;
  }

  /**
   * @return the alias
   */
  public final String getAlias() {
    return alias;
  }

  /**
   * @param alias the alias to set
   */
  public final void setAlias(final String alias) {
    this.alias = alias;
  }

  /**
   * @return the roles
   */
  public final String getRoles() {
    return roles;
  }

  /**
   * @param roles the roles to set
   */
  public final void setRoles(final String roles) {
    this.roles = roles;
  }

  /**
   * @return the hostid
   */
  public final long getHostid() {
    return hostid;
  }

  /**
   * @param hostid the hostid to set
   */
  public final void setHostid(final long hostid) {
    this.hostid = hostid;
  }

  /**
   * @return the ruleid
   */
  public final long getRuleid() {
    return ruleid;
  }

  /**
   * @param ruleid the ruleid to set
   */
  public final void setRuleid(final long ruleid) {
    this.ruleid = ruleid;
  }

  /**
   * @return the businessid
   */
  public final long getBusinessid() {
    return businessid;
  }

  /**
   * @param businessid the businessid to set
   */
  public final void setBusinessid(final long businessid) {
    this.businessid = businessid;
  }

  /**
   * @return the aliasid
   */
  public final long getAliasid() {
    return aliasid;
  }

  /**
   * @param aliasid the aliasid to set
   */
  public final void setAliasid(final long aliasid) {
    this.aliasid = aliasid;
  }

  /**
   * @return the rolesid
   */
  public final long getRolesid() {
    return rolesid;
  }

  /**
   * @param rolesid the rolesid to set
   */
  public final void setRolesid(final long rolesid) {
    this.rolesid = rolesid;
  }

  @Override
  public final void fromJson(final JsonPacket other) {
    super.fromJson(other);
    if (other instanceof ConfigImportJsonPacket) {
      final ConfigImportJsonPacket other2 = (ConfigImportJsonPacket) other;
      purgehost = other2.purgehost;
      purgerule = other2.purgerule;
      purgebusiness = other2.purgebusiness;
      purgealias = other2.purgealias;
      purgeroles = other2.purgeroles;
      host = other2.host;
      rule = other2.rule;
      business = other2.business;
      alias = other2.alias;
      roles = other2.roles;
      hostid = other2.hostid;
      ruleid = other2.ruleid;
      businessid = other2.businessid;
      aliasid = other2.aliasid;
      rolesid = other2.rolesid;
    }
  }

  @Override
  public void setRequestUserPacket() {
    setRequestUserPacket(LocalPacketFactory.CONFIMPORTPACKET);
  }
}
