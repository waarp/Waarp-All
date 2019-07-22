/**
   This file is part of Waarp Project.

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All Waarp Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Waarp is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Waarp .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.protocol.localhandler.packet.json;

import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;

/**
 * Import Configuration JSON packet
 * 
 * @author "Frederic Bregier"
 *
 */
public class ConfigImportJsonPacket extends JsonPacket {

    protected boolean purgehost, purgerule, purgebusiness, purgealias, purgeroles;
    protected String host, rule, business, alias, roles;
    protected long hostid = DbConstant.ILLEGALVALUE, ruleid = DbConstant.ILLEGALVALUE,
            businessid = DbConstant.ILLEGALVALUE, aliasid = DbConstant.ILLEGALVALUE, rolesid = DbConstant.ILLEGALVALUE;

    /**
     * @return the purgehost
     */
    public boolean isPurgehost() {
        return purgehost;
    }

    /**
     * @param purgehost
     *            the purgehost to set
     */
    public void setPurgehost(boolean purgehost) {
        this.purgehost = purgehost;
    }

    /**
     * @return the purgerule
     */
    public boolean isPurgerule() {
        return purgerule;
    }

    /**
     * @param purgerule
     *            the purgerule to set
     */
    public void setPurgerule(boolean purgerule) {
        this.purgerule = purgerule;
    }

    /**
     * @return the purgebusiness
     */
    public boolean isPurgebusiness() {
        return purgebusiness;
    }

    /**
     * @param purgebusiness
     *            the purgebusiness to set
     */
    public void setPurgebusiness(boolean purgebusiness) {
        this.purgebusiness = purgebusiness;
    }

    /**
     * @return the purgealias
     */
    public boolean isPurgealias() {
        return purgealias;
    }

    /**
     * @param purgealias
     *            the purgealias to set
     */
    public void setPurgealias(boolean purgealias) {
        this.purgealias = purgealias;
    }

    /**
     * @return the purgeroles
     */
    public boolean isPurgeroles() {
        return purgeroles;
    }

    /**
     * @param purgeroles
     *            the purgeroles to set
     */
    public void setPurgeroles(boolean purgeroles) {
        this.purgeroles = purgeroles;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host
     *            the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the rule
     */
    public String getRule() {
        return rule;
    }

    /**
     * @param rule
     *            the rule to set
     */
    public void setRule(String rule) {
        this.rule = rule;
    }

    /**
     * @return the business
     */
    public String getBusiness() {
        return business;
    }

    /**
     * @param business
     *            the business to set
     */
    public void setBusiness(String business) {
        this.business = business;
    }

    /**
     * @return the alias
     */
    public String getAlias() {
        return alias;
    }

    /**
     * @param alias
     *            the alias to set
     */
    public void setAlias(String alias) {
        this.alias = alias;
    }

    /**
     * @return the roles
     */
    public String getRoles() {
        return roles;
    }

    /**
     * @param roles
     *            the roles to set
     */
    public void setRoles(String roles) {
        this.roles = roles;
    }

    /**
     * @return the hostid
     */
    public long getHostid() {
        return hostid;
    }

    /**
     * @param hostid
     *            the hostid to set
     */
    public void setHostid(long hostid) {
        this.hostid = hostid;
    }

    /**
     * @return the ruleid
     */
    public long getRuleid() {
        return ruleid;
    }

    /**
     * @param ruleid
     *            the ruleid to set
     */
    public void setRuleid(long ruleid) {
        this.ruleid = ruleid;
    }

    /**
     * @return the businessid
     */
    public long getBusinessid() {
        return businessid;
    }

    /**
     * @param businessid
     *            the businessid to set
     */
    public void setBusinessid(long businessid) {
        this.businessid = businessid;
    }

    /**
     * @return the aliasid
     */
    public long getAliasid() {
        return aliasid;
    }

    /**
     * @param aliasid
     *            the aliasid to set
     */
    public void setAliasid(long aliasid) {
        this.aliasid = aliasid;
    }

    /**
     * @return the rolesid
     */
    public long getRolesid() {
        return rolesid;
    }

    /**
     * @param rolesid
     *            the rolesid to set
     */
    public void setRolesid(long rolesid) {
        this.rolesid = rolesid;
    }

    @Override
    public void fromJson(JsonPacket other) {
        super.fromJson(other);
        if (other instanceof ConfigImportJsonPacket) {
            ConfigImportJsonPacket other2 = (ConfigImportJsonPacket) other;
            this.purgehost = other2.purgehost;
            this.purgerule = other2.purgerule;
            this.purgebusiness = other2.purgebusiness;
            this.purgealias = other2.purgealias;
            this.purgeroles = other2.purgeroles;
            this.host = other2.host;
            this.rule = other2.rule;
            this.business = other2.business;
            this.alias = other2.alias;
            this.roles = other2.roles;
            this.hostid = other2.hostid;
            this.ruleid = other2.ruleid;
            this.businessid = other2.businessid;
            this.aliasid = other2.aliasid;
            this.rolesid = other2.rolesid;
        }
    }

    public void setRequestUserPacket() {
        super.setRequestUserPacket(LocalPacketFactory.CONFIMPORTPACKET);
    }
}
