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

/**
 * Exporting Configuration JSON packet
 */
public class ConfigExportJsonPacket extends JsonPacket {

  protected boolean host;
  protected boolean rule;
  protected boolean business;
  protected boolean alias;
  protected boolean roles;

  /**
   * @return the host
   */
  public final boolean isHost() {
    return host;
  }

  /**
   * @param host the host to set
   */
  public final void setHost(final boolean host) {
    this.host = host;
  }

  /**
   * @return the rule
   */
  public final boolean isRule() {
    return rule;
  }

  /**
   * @param rule the rule to set
   */
  public final void setRule(final boolean rule) {
    this.rule = rule;
  }

  /**
   * @return the business
   */
  public final boolean isBusiness() {
    return business;
  }

  /**
   * @param business the business to set
   */
  public final void setBusiness(final boolean business) {
    this.business = business;
  }

  /**
   * @return the alias
   */
  public final boolean isAlias() {
    return alias;
  }

  /**
   * @param alias the alias to set
   */
  public final void setAlias(final boolean alias) {
    this.alias = alias;
  }

  /**
   * @return the roles
   */
  public final boolean isRoles() {
    return roles;
  }

  /**
   * @param roles the roles to set
   */
  public final void setRoles(final boolean roles) {
    this.roles = roles;
  }

  @Override
  public final void fromJson(final JsonPacket other) {
    super.fromJson(other);
    if (other instanceof ConfigExportJsonPacket) {
      final ConfigExportJsonPacket other2 = (ConfigExportJsonPacket) other;
      host = other2.host;
      rule = other2.rule;
      business = other2.business;
      alias = other2.alias;
      roles = other2.roles;
    }
  }

  @Override
  public void setRequestUserPacket() {
    setRequestUserPacket(LocalPacketFactory.CONFEXPORTPACKET);
  }
}
