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
package org.waarp.common.role;

/**
 * Role to be used in Waarp projects
 */
public class RoleDefault {
  public enum ROLE {
    /**
     * No access to any function
     */
    NOACCESS(0),
    /**
     * Read only access, no action
     */
    READONLY(1),
    /**
     * Ability to starts transfer
     */
    TRANSFER(2),
    /**
     * Ability to control rules
     */
    RULE(4),
    /**
     * Ability to control hosts
     */
    HOST(8),
    /**
     * Ability to control bandwidth limitation
     */
    LIMIT(16),
    /**
     * Ability to control the system configuration and to control transfers
     */
    SYSTEM(32),
    /**
     * Ability to control the logging
     */
    LOGCONTROL(64),
    /**
     * Unused Role value
     */
    UNUSED(-128),
    /**
     * Default partner : ability to read and starts transfers
     */
    PARTNER(READONLY, TRANSFER),
    /**
     * Administrator on configuration (partner, rule, host)
     */
    CONFIGADMIN(PARTNER, RULE, HOST),
    /**
     * Administrator on all
     */
    FULLADMIN(CONFIGADMIN, LIMIT, SYSTEM, LOGCONTROL);

    private byte brole;

    ROLE(final int val) {
      brole = (byte) val;
    }

    ROLE(final ROLE... name) {
      for (final ROLE role : name) {
        this.brole |= role.brole;
      }
    }

    public boolean isContained(final byte value) {
      if (this == NOACCESS) {
        return value == NOACCESS.brole;
      }
      return (value & brole) == brole;
    }

    public boolean isContained(final ROLE role) {
      return isContained(role.brole);
    }

    public boolean contains(final ROLE role) {
      return role.isContained(this);
    }

    public byte getAsByte() {
      return brole;
    }

    private static final ROLE[] READONLY_A = new ROLE[] { READONLY };
    private static final ROLE[] TRANSFER_A = new ROLE[] { TRANSFER };
    private static final ROLE[] RULE_A = new ROLE[] { RULE };
    private static final ROLE[] HOST_A = new ROLE[] { HOST };
    private static final ROLE[] LIMIT_A = new ROLE[] { LIMIT };
    private static final ROLE[] SYSTEM_A = new ROLE[] { SYSTEM };
    private static final ROLE[] LOGCONTROL_A = new ROLE[] { LOGCONTROL };
    private static final ROLE[] UNUSED_A = new ROLE[] { UNUSED };
    private static final ROLE[] NOACCESS_A = new ROLE[] { NOACCESS };
    private static final ROLE[] PARTNER_A = new ROLE[] { READONLY, TRANSFER };
    private static final ROLE[] CONFIGADMIN_A =
        new ROLE[] { READONLY, TRANSFER, RULE, HOST };
    private static final ROLE[] FULLADMIN_A =
        new ROLE[] { READONLY, TRANSFER, RULE, LIMIT, SYSTEM, LOGCONTROL };

    public ROLE[] getComposingRoles() {
      switch (brole) {
        case 1:
          return READONLY_A;
        case 2:
          return TRANSFER_A;
        case 3:
          return PARTNER_A;
        case 4:
          return RULE_A;
        case 8:
          return HOST_A;
        case 15:
          return CONFIGADMIN_A;
        case 16:
          return LIMIT_A;
        case 32:
          return SYSTEM_A;
        case 64:
          return LOGCONTROL_A;
        case 127:
          return FULLADMIN_A;
        case -128:
          return UNUSED_A;
        case 0:
        default:
          return NOACCESS_A;
      }
    }

    public static final String toString(final byte fromRole) {
      final StringBuilder result = new StringBuilder("[ ");
      final ROLE[] values = ROLE.values();
      for (final ROLE role : values) {
        if (role.isContained(fromRole)) {
          result.append(role.name()).append(' ');
        }
      }
      result.append(']');
      return result.toString();
    }

    public static final ROLE fromByte(final byte role) {
      switch (role) {
        case 1:
          return READONLY;
        case 2:
          return TRANSFER;
        case 3:
          return PARTNER;
        case 4:
          return RULE;
        case 8:
          return HOST;
        case 15:
          return CONFIGADMIN;
        case 16:
          return LIMIT;
        case 32:
          return SYSTEM;
        case 64:
          return LOGCONTROL;
        case 127:
          return FULLADMIN;
        case -128:
          return UNUSED;
        case 0:
        default:
          return NOACCESS;
      }
    }
  }

  private byte role;

  public RoleDefault() {
    role = ROLE.NOACCESS.brole;
  }

  public RoleDefault(final ROLE role) {
    this.role = role.brole;
  }

  public byte getRoleAsByte() {
    return role;
  }

  @Override
  public String toString() {
    return ROLE.toString(role);
  }

  public RoleDefault addRole(final ROLE newrole) {
    role |= newrole.brole;
    return this;
  }

  public RoleDefault setRole(final ROLE newrole) {
    role = newrole.brole;
    return this;
  }

  public RoleDefault setRoleDefault(final RoleDefault newrole) {
    role = newrole.role;
    return this;
  }

  public void clear() {
    role = ROLE.NOACCESS.brole;
  }

  public boolean isContaining(final ROLE otherrole) {
    return otherrole.isContained(role);
  }

  public boolean hasNoAccess() {
    return role == ROLE.NOACCESS.brole;
  }

  public boolean hasReadOnly() {
    return ROLE.READONLY.isContained(role);
  }

  public boolean hasTransfer() {
    return ROLE.TRANSFER.isContained(role);
  }

  public boolean hasRule() {
    return ROLE.RULE.isContained(role);
  }

  public boolean hasHost() {
    return ROLE.HOST.isContained(role);
  }

  public boolean hasLimit() {
    return ROLE.LIMIT.isContained(role);
  }

  public boolean hasSystem() {
    return ROLE.SYSTEM.isContained(role);
  }

  public boolean hasUnused() {
    return ROLE.UNUSED.isContained(role);
  }

  public boolean hasLogControl() {
    return ROLE.LOGCONTROL.isContained(role);
  }

  public static final boolean hasNoAccess(final byte role) {
    return role == ROLE.NOACCESS.brole;
  }

  public static final boolean hasReadOnly(final byte role) {
    return ROLE.READONLY.isContained(role);
  }

  public static final boolean hasTransfer(final byte role) {
    return ROLE.TRANSFER.isContained(role);
  }

  public static final boolean hasRule(final byte role) {
    return ROLE.RULE.isContained(role);
  }

  public static final boolean hasHost(final byte role) {
    return ROLE.HOST.isContained(role);
  }

  public static final boolean hasLimit(final byte role) {
    return ROLE.LIMIT.isContained(role);
  }

  public static final boolean hasSystem(final byte role) {
    return ROLE.SYSTEM.isContained(role);
  }

  public static final boolean hasUnused(final byte role) {
    return ROLE.UNUSED.isContained(role);
  }

  public static final boolean hasLogControl(final byte role) {
    return ROLE.LOGCONTROL.isContained(role);
  }

}
