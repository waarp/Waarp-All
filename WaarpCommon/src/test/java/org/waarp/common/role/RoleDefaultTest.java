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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.common.utility.TestWatcherJunit4;

import static org.junit.Assert.*;

public class RoleDefaultTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();


  @Test
  public void testRoleDefault() {
    final RoleDefault role = new RoleDefault();
    checkRole(role, ROLE.NOACCESS);
  }

  private void checkRole(RoleDefault role, ROLE rolecheck) {
    switch (rolecheck) {
      case HOST:
        assertTrue(role.hasHost());
        assertFalse(role.hasLimit());
        assertFalse(role.hasLogControl());
        assertFalse(role.hasReadOnly());
        assertFalse(role.hasRule());
        assertFalse(role.hasSystem());
        assertFalse(role.hasTransfer());
        assertFalse(role.hasUnused());
        assertFalse(role.isContaining(ROLE.READONLY));
        assertFalse(role.isContaining(ROLE.TRANSFER));
        assertFalse(role.isContaining(ROLE.RULE));
        assertTrue(role.isContaining(ROLE.HOST));
        assertFalse(role.isContaining(ROLE.LIMIT));
        assertFalse(role.isContaining(ROLE.SYSTEM));
        assertFalse(role.isContaining(ROLE.LOGCONTROL));
        assertFalse(role.isContaining(ROLE.UNUSED));
        assertFalse(role.isContaining(ROLE.PARTNER));
        assertFalse(role.isContaining(ROLE.CONFIGADMIN));
        assertFalse(role.isContaining(ROLE.FULLADMIN));
        assertFalse(role.hasNoAccess());
        assertFalse(role.isContaining(ROLE.NOACCESS));
        break;
      case LIMIT:
        assertFalse(role.hasHost());
        assertTrue(role.hasLimit());
        assertFalse(role.hasLogControl());
        assertFalse(role.hasReadOnly());
        assertFalse(role.hasRule());
        assertFalse(role.hasSystem());
        assertFalse(role.hasTransfer());
        assertFalse(role.hasUnused());
        assertFalse(role.isContaining(ROLE.READONLY));
        assertFalse(role.isContaining(ROLE.TRANSFER));
        assertFalse(role.isContaining(ROLE.RULE));
        assertFalse(role.isContaining(ROLE.HOST));
        assertTrue(role.isContaining(ROLE.LIMIT));
        assertFalse(role.isContaining(ROLE.SYSTEM));
        assertFalse(role.isContaining(ROLE.LOGCONTROL));
        assertFalse(role.isContaining(ROLE.UNUSED));
        assertFalse(role.isContaining(ROLE.PARTNER));
        assertFalse(role.isContaining(ROLE.CONFIGADMIN));
        assertFalse(role.isContaining(ROLE.FULLADMIN));
        assertFalse(role.hasNoAccess());
        assertFalse(role.isContaining(ROLE.NOACCESS));
        break;
      case LOGCONTROL:
        assertFalse(role.hasHost());
        assertFalse(role.hasLimit());
        assertTrue(role.hasLogControl());
        assertFalse(role.hasReadOnly());
        assertFalse(role.hasRule());
        assertFalse(role.hasSystem());
        assertFalse(role.hasTransfer());
        assertFalse(role.hasUnused());
        assertFalse(role.isContaining(ROLE.READONLY));
        assertFalse(role.isContaining(ROLE.TRANSFER));
        assertFalse(role.isContaining(ROLE.RULE));
        assertFalse(role.isContaining(ROLE.HOST));
        assertFalse(role.isContaining(ROLE.LIMIT));
        assertFalse(role.isContaining(ROLE.SYSTEM));
        assertTrue(role.isContaining(ROLE.LOGCONTROL));
        assertFalse(role.isContaining(ROLE.UNUSED));
        assertFalse(role.isContaining(ROLE.PARTNER));
        assertFalse(role.isContaining(ROLE.CONFIGADMIN));
        assertFalse(role.isContaining(ROLE.FULLADMIN));
        assertFalse(role.hasNoAccess());
        assertFalse(role.isContaining(ROLE.NOACCESS));
        break;
      case NOACCESS:
        assertFalse(role.hasHost());
        assertFalse(role.hasLimit());
        assertFalse(role.hasLogControl());
        assertFalse(role.hasReadOnly());
        assertFalse(role.hasRule());
        assertFalse(role.hasSystem());
        assertFalse(role.hasTransfer());
        assertFalse(role.hasUnused());
        assertFalse(role.isContaining(ROLE.READONLY));
        assertFalse(role.isContaining(ROLE.TRANSFER));
        assertFalse(role.isContaining(ROLE.RULE));
        assertFalse(role.isContaining(ROLE.HOST));
        assertFalse(role.isContaining(ROLE.LIMIT));
        assertFalse(role.isContaining(ROLE.SYSTEM));
        assertFalse(role.isContaining(ROLE.LOGCONTROL));
        assertFalse(role.isContaining(ROLE.UNUSED));
        assertFalse(role.isContaining(ROLE.PARTNER));
        assertFalse(role.isContaining(ROLE.CONFIGADMIN));
        assertFalse(role.isContaining(ROLE.FULLADMIN));
        assertTrue(role.hasNoAccess());
        assertTrue(role.isContaining(ROLE.NOACCESS));
        break;
      case READONLY:
        assertFalse(role.hasHost());
        assertFalse(role.hasLimit());
        assertFalse(role.hasLogControl());
        assertTrue(role.hasReadOnly());
        assertFalse(role.hasRule());
        assertFalse(role.hasSystem());
        assertFalse(role.hasTransfer());
        assertFalse(role.hasUnused());
        assertTrue(role.isContaining(ROLE.READONLY));
        assertFalse(role.isContaining(ROLE.TRANSFER));
        assertFalse(role.isContaining(ROLE.RULE));
        assertFalse(role.isContaining(ROLE.HOST));
        assertFalse(role.isContaining(ROLE.LIMIT));
        assertFalse(role.isContaining(ROLE.SYSTEM));
        assertFalse(role.isContaining(ROLE.LOGCONTROL));
        assertFalse(role.isContaining(ROLE.UNUSED));
        assertFalse(role.isContaining(ROLE.PARTNER));
        assertFalse(role.isContaining(ROLE.CONFIGADMIN));
        assertFalse(role.isContaining(ROLE.FULLADMIN));
        assertFalse(role.hasNoAccess());
        assertFalse(role.isContaining(ROLE.NOACCESS));
        break;
      case RULE:
        assertFalse(role.hasHost());
        assertFalse(role.hasLimit());
        assertFalse(role.hasLogControl());
        assertFalse(role.hasReadOnly());
        assertTrue(role.hasRule());
        assertFalse(role.hasSystem());
        assertFalse(role.hasTransfer());
        assertFalse(role.hasUnused());
        assertFalse(role.isContaining(ROLE.READONLY));
        assertFalse(role.isContaining(ROLE.TRANSFER));
        assertTrue(role.isContaining(ROLE.RULE));
        assertFalse(role.isContaining(ROLE.HOST));
        assertFalse(role.isContaining(ROLE.LIMIT));
        assertFalse(role.isContaining(ROLE.SYSTEM));
        assertFalse(role.isContaining(ROLE.LOGCONTROL));
        assertFalse(role.isContaining(ROLE.UNUSED));
        assertFalse(role.isContaining(ROLE.PARTNER));
        assertFalse(role.isContaining(ROLE.CONFIGADMIN));
        assertFalse(role.isContaining(ROLE.FULLADMIN));
        assertFalse(role.hasNoAccess());
        assertFalse(role.isContaining(ROLE.NOACCESS));
        break;
      case SYSTEM:
        assertFalse(role.hasHost());
        assertFalse(role.hasLimit());
        assertFalse(role.hasLogControl());
        assertFalse(role.hasReadOnly());
        assertFalse(role.hasRule());
        assertTrue(role.hasSystem());
        assertFalse(role.hasTransfer());
        assertFalse(role.hasUnused());
        assertFalse(role.isContaining(ROLE.READONLY));
        assertFalse(role.isContaining(ROLE.TRANSFER));
        assertFalse(role.isContaining(ROLE.RULE));
        assertFalse(role.isContaining(ROLE.HOST));
        assertFalse(role.isContaining(ROLE.LIMIT));
        assertTrue(role.isContaining(ROLE.SYSTEM));
        assertFalse(role.isContaining(ROLE.LOGCONTROL));
        assertFalse(role.isContaining(ROLE.UNUSED));
        assertFalse(role.isContaining(ROLE.PARTNER));
        assertFalse(role.isContaining(ROLE.CONFIGADMIN));
        assertFalse(role.isContaining(ROLE.FULLADMIN));
        assertFalse(role.hasNoAccess());
        assertFalse(role.isContaining(ROLE.NOACCESS));
        break;
      case TRANSFER:
        assertFalse(role.hasHost());
        assertFalse(role.hasLimit());
        assertFalse(role.hasLogControl());
        assertFalse(role.hasReadOnly());
        assertFalse(role.hasRule());
        assertFalse(role.hasSystem());
        assertTrue(role.hasTransfer());
        assertFalse(role.hasUnused());
        assertFalse(role.isContaining(ROLE.READONLY));
        assertTrue(role.isContaining(ROLE.TRANSFER));
        assertFalse(role.isContaining(ROLE.RULE));
        assertFalse(role.isContaining(ROLE.HOST));
        assertFalse(role.isContaining(ROLE.LIMIT));
        assertFalse(role.isContaining(ROLE.SYSTEM));
        assertFalse(role.isContaining(ROLE.LOGCONTROL));
        assertFalse(role.isContaining(ROLE.UNUSED));
        assertFalse(role.isContaining(ROLE.PARTNER));
        assertFalse(role.isContaining(ROLE.CONFIGADMIN));
        assertFalse(role.isContaining(ROLE.FULLADMIN));
        assertFalse(role.hasNoAccess());
        assertFalse(role.isContaining(ROLE.NOACCESS));
        break;
      case UNUSED:
        assertFalse(role.hasHost());
        assertFalse(role.hasLimit());
        assertFalse(role.hasLogControl());
        assertFalse(role.hasReadOnly());
        assertFalse(role.hasRule());
        assertFalse(role.hasSystem());
        assertFalse(role.hasTransfer());
        assertTrue(role.hasUnused());
        assertFalse(role.isContaining(ROLE.READONLY));
        assertFalse(role.isContaining(ROLE.TRANSFER));
        assertFalse(role.isContaining(ROLE.RULE));
        assertFalse(role.isContaining(ROLE.HOST));
        assertFalse(role.isContaining(ROLE.LIMIT));
        assertFalse(role.isContaining(ROLE.SYSTEM));
        assertFalse(role.isContaining(ROLE.LOGCONTROL));
        assertTrue(role.isContaining(ROLE.UNUSED));
        assertFalse(role.isContaining(ROLE.PARTNER));
        assertFalse(role.isContaining(ROLE.CONFIGADMIN));
        assertFalse(role.isContaining(ROLE.FULLADMIN));
        assertFalse(role.hasNoAccess());
        assertFalse(role.isContaining(ROLE.NOACCESS));
        break;
      case PARTNER:
        assertFalse(role.hasHost());
        assertFalse(role.hasLimit());
        assertFalse(role.hasLogControl());
        assertTrue(role.hasReadOnly());
        assertFalse(role.hasRule());
        assertFalse(role.hasSystem());
        assertTrue(role.hasTransfer());
        assertFalse(role.hasUnused());
        assertTrue(role.isContaining(ROLE.READONLY));
        assertTrue(role.isContaining(ROLE.TRANSFER));
        assertFalse(role.isContaining(ROLE.RULE));
        assertFalse(role.isContaining(ROLE.HOST));
        assertFalse(role.isContaining(ROLE.LIMIT));
        assertFalse(role.isContaining(ROLE.SYSTEM));
        assertFalse(role.isContaining(ROLE.LOGCONTROL));
        assertFalse(role.isContaining(ROLE.UNUSED));
        assertTrue(role.isContaining(ROLE.PARTNER));
        assertFalse(role.isContaining(ROLE.CONFIGADMIN));
        assertFalse(role.isContaining(ROLE.FULLADMIN));
        assertFalse(role.hasNoAccess());
        assertFalse(role.isContaining(ROLE.NOACCESS));
        break;
      case CONFIGADMIN:
        assertTrue(role.hasHost());
        assertFalse(role.hasLimit());
        assertFalse(role.hasLogControl());
        assertTrue(role.hasReadOnly());
        assertTrue(role.hasRule());
        assertFalse(role.hasSystem());
        assertTrue(role.hasTransfer());
        assertFalse(role.hasUnused());
        assertTrue(role.isContaining(ROLE.READONLY));
        assertTrue(role.isContaining(ROLE.TRANSFER));
        assertTrue(role.isContaining(ROLE.RULE));
        assertTrue(role.isContaining(ROLE.HOST));
        assertFalse(role.isContaining(ROLE.LIMIT));
        assertFalse(role.isContaining(ROLE.SYSTEM));
        assertFalse(role.isContaining(ROLE.LOGCONTROL));
        assertFalse(role.isContaining(ROLE.UNUSED));
        assertTrue(role.isContaining(ROLE.PARTNER));
        assertTrue(role.isContaining(ROLE.CONFIGADMIN));
        assertFalse(role.isContaining(ROLE.FULLADMIN));
        assertFalse(role.hasNoAccess());
        assertFalse(role.isContaining(ROLE.NOACCESS));
        break;
      case FULLADMIN:
        assertTrue(role.hasHost());
        assertTrue(role.hasLimit());
        assertTrue(role.hasLogControl());
        assertTrue(role.hasReadOnly());
        assertTrue(role.hasRule());
        assertTrue(role.hasSystem());
        assertTrue(role.hasTransfer());
        assertFalse(role.hasUnused());
        assertTrue(role.isContaining(ROLE.READONLY));
        assertTrue(role.isContaining(ROLE.TRANSFER));
        assertTrue(role.isContaining(ROLE.RULE));
        assertTrue(role.isContaining(ROLE.HOST));
        assertTrue(role.isContaining(ROLE.LIMIT));
        assertTrue(role.isContaining(ROLE.SYSTEM));
        assertTrue(role.isContaining(ROLE.LOGCONTROL));
        assertFalse(role.isContaining(ROLE.UNUSED));
        assertTrue(role.isContaining(ROLE.PARTNER));
        assertTrue(role.isContaining(ROLE.CONFIGADMIN));
        assertTrue(role.isContaining(ROLE.FULLADMIN));
        assertFalse(role.hasNoAccess());
        assertFalse(role.isContaining(ROLE.NOACCESS));
        break;
      default:
        break;
    }
  }

  @Test
  public void testRoleDefaultROLE() {
    for (final ROLE roletoset : ROLE.values()) {
      final RoleDefault role = new RoleDefault(roletoset);
      checkRole(role, roletoset);
      role.clear();
      checkRole(role, ROLE.NOACCESS);
    }
  }

  @Test
  public void testAddRole() {
    for (final ROLE roletoset : ROLE.values()) {
      final RoleDefault role = new RoleDefault();
      checkRole(role, ROLE.NOACCESS);
      role.addRole(roletoset);
      checkRole(role, roletoset);
      role.clear();
      checkRole(role, ROLE.NOACCESS);
    }
  }

  @Test
  public void testSetRole() {
    for (final ROLE roletoset : ROLE.values()) {
      final RoleDefault role = new RoleDefault();
      checkRole(role, ROLE.NOACCESS);
      role.addRole(ROLE.FULLADMIN);
      checkRole(role, ROLE.FULLADMIN);
      role.setRole(roletoset);
      checkRole(role, roletoset);
    }
  }

  @Test
  public void testSetRoleDefault() {
    for (final ROLE roletoset : ROLE.values()) {
      final RoleDefault role = new RoleDefault();
      checkRole(role, ROLE.NOACCESS);
      role.addRole(roletoset);
      checkRole(role, roletoset);
      final RoleDefault role2 = new RoleDefault();
      checkRole(role2, ROLE.NOACCESS);
      role2.setRoleDefault(role);
      checkRole(role2, roletoset);
      role.clear();
      checkRole(role, ROLE.NOACCESS);
      checkRole(role2, roletoset);
    }
  }

  @Test
  public void testRoleIsContained() {
    assertTrue(ROLE.NOACCESS.isContained(ROLE.NOACCESS));
    assertFalse(ROLE.NOACCESS.isContained(ROLE.READONLY));
    assertFalse(ROLE.NOACCESS.isContained(ROLE.TRANSFER));
    assertFalse(ROLE.NOACCESS.isContained(ROLE.RULE));
    assertFalse(ROLE.NOACCESS.isContained(ROLE.HOST));
    assertFalse(ROLE.NOACCESS.isContained(ROLE.LIMIT));
    assertFalse(ROLE.NOACCESS.isContained(ROLE.SYSTEM));
    assertFalse(ROLE.NOACCESS.isContained(ROLE.LOGCONTROL));

    assertFalse(ROLE.READONLY.isContained(ROLE.NOACCESS));
    assertTrue(ROLE.READONLY.isContained(ROLE.READONLY));
    assertFalse(ROLE.READONLY.isContained(ROLE.TRANSFER));
    assertFalse(ROLE.READONLY.isContained(ROLE.RULE));
    assertFalse(ROLE.READONLY.isContained(ROLE.HOST));
    assertFalse(ROLE.READONLY.isContained(ROLE.LIMIT));
    assertFalse(ROLE.READONLY.isContained(ROLE.SYSTEM));
    assertFalse(ROLE.READONLY.isContained(ROLE.LOGCONTROL));

    assertFalse(ROLE.TRANSFER.isContained(ROLE.NOACCESS));
    assertFalse(ROLE.TRANSFER.isContained(ROLE.READONLY));
    assertTrue(ROLE.TRANSFER.isContained(ROLE.TRANSFER));
    assertFalse(ROLE.TRANSFER.isContained(ROLE.RULE));
    assertFalse(ROLE.TRANSFER.isContained(ROLE.HOST));
    assertFalse(ROLE.TRANSFER.isContained(ROLE.LIMIT));
    assertFalse(ROLE.TRANSFER.isContained(ROLE.SYSTEM));
    assertFalse(ROLE.TRANSFER.isContained(ROLE.LOGCONTROL));

    assertFalse(ROLE.RULE.isContained(ROLE.NOACCESS));
    assertFalse(ROLE.RULE.isContained(ROLE.READONLY));
    assertFalse(ROLE.RULE.isContained(ROLE.TRANSFER));
    assertTrue(ROLE.RULE.isContained(ROLE.RULE));
    assertFalse(ROLE.RULE.isContained(ROLE.HOST));
    assertFalse(ROLE.RULE.isContained(ROLE.LIMIT));
    assertFalse(ROLE.RULE.isContained(ROLE.SYSTEM));
    assertFalse(ROLE.RULE.isContained(ROLE.LOGCONTROL));

    assertFalse(ROLE.HOST.isContained(ROLE.NOACCESS));
    assertFalse(ROLE.HOST.isContained(ROLE.READONLY));
    assertFalse(ROLE.HOST.isContained(ROLE.TRANSFER));
    assertFalse(ROLE.HOST.isContained(ROLE.RULE));
    assertTrue(ROLE.HOST.isContained(ROLE.HOST));
    assertFalse(ROLE.HOST.isContained(ROLE.LIMIT));
    assertFalse(ROLE.HOST.isContained(ROLE.SYSTEM));
    assertFalse(ROLE.HOST.isContained(ROLE.LOGCONTROL));

    assertFalse(ROLE.LIMIT.isContained(ROLE.NOACCESS));
    assertFalse(ROLE.LIMIT.isContained(ROLE.READONLY));
    assertFalse(ROLE.LIMIT.isContained(ROLE.TRANSFER));
    assertFalse(ROLE.LIMIT.isContained(ROLE.RULE));
    assertFalse(ROLE.LIMIT.isContained(ROLE.HOST));
    assertTrue(ROLE.LIMIT.isContained(ROLE.LIMIT));
    assertFalse(ROLE.LIMIT.isContained(ROLE.SYSTEM));
    assertFalse(ROLE.LIMIT.isContained(ROLE.LOGCONTROL));

    assertFalse(ROLE.SYSTEM.isContained(ROLE.NOACCESS));
    assertFalse(ROLE.SYSTEM.isContained(ROLE.READONLY));
    assertFalse(ROLE.SYSTEM.isContained(ROLE.TRANSFER));
    assertFalse(ROLE.SYSTEM.isContained(ROLE.RULE));
    assertFalse(ROLE.SYSTEM.isContained(ROLE.HOST));
    assertFalse(ROLE.SYSTEM.isContained(ROLE.LIMIT));
    assertTrue(ROLE.SYSTEM.isContained(ROLE.SYSTEM));
    assertFalse(ROLE.SYSTEM.isContained(ROLE.LOGCONTROL));

    assertFalse(ROLE.LOGCONTROL.isContained(ROLE.NOACCESS));
    assertFalse(ROLE.LOGCONTROL.isContained(ROLE.READONLY));
    assertFalse(ROLE.LOGCONTROL.isContained(ROLE.TRANSFER));
    assertFalse(ROLE.LOGCONTROL.isContained(ROLE.RULE));
    assertFalse(ROLE.LOGCONTROL.isContained(ROLE.HOST));
    assertFalse(ROLE.LOGCONTROL.isContained(ROLE.LIMIT));
    assertFalse(ROLE.LOGCONTROL.isContained(ROLE.SYSTEM));
    assertTrue(ROLE.LOGCONTROL.isContained(ROLE.LOGCONTROL));

    assertFalse(ROLE.NOACCESS.isContained(ROLE.PARTNER));
    assertTrue(ROLE.READONLY.isContained(ROLE.PARTNER));
    assertTrue(ROLE.TRANSFER.isContained(ROLE.PARTNER));
    assertTrue(ROLE.PARTNER.isContained(ROLE.PARTNER));
    assertFalse(ROLE.RULE.isContained(ROLE.PARTNER));
    assertFalse(ROLE.HOST.isContained(ROLE.PARTNER));
    assertFalse(ROLE.LIMIT.isContained(ROLE.PARTNER));
    assertFalse(ROLE.SYSTEM.isContained(ROLE.PARTNER));
    assertFalse(ROLE.LOGCONTROL.isContained(ROLE.PARTNER));

    assertFalse(ROLE.NOACCESS.isContained(ROLE.CONFIGADMIN));
    assertTrue(ROLE.READONLY.isContained(ROLE.CONFIGADMIN));
    assertTrue(ROLE.TRANSFER.isContained(ROLE.CONFIGADMIN));
    assertTrue(ROLE.RULE.isContained(ROLE.CONFIGADMIN));
    assertTrue(ROLE.HOST.isContained(ROLE.CONFIGADMIN));
    assertTrue(ROLE.CONFIGADMIN.isContained(ROLE.CONFIGADMIN));
    assertFalse(ROLE.LIMIT.isContained(ROLE.CONFIGADMIN));
    assertFalse(ROLE.SYSTEM.isContained(ROLE.CONFIGADMIN));
    assertFalse(ROLE.LOGCONTROL.isContained(ROLE.CONFIGADMIN));

    assertFalse(ROLE.NOACCESS.isContained(ROLE.FULLADMIN));
    assertTrue(ROLE.READONLY.isContained(ROLE.FULLADMIN));
    assertTrue(ROLE.TRANSFER.isContained(ROLE.FULLADMIN));
    assertTrue(ROLE.RULE.isContained(ROLE.FULLADMIN));
    assertTrue(ROLE.HOST.isContained(ROLE.FULLADMIN));
    assertTrue(ROLE.LIMIT.isContained(ROLE.FULLADMIN));
    assertTrue(ROLE.SYSTEM.isContained(ROLE.FULLADMIN));
    assertTrue(ROLE.LOGCONTROL.isContained(ROLE.FULLADMIN));
    assertTrue(ROLE.FULLADMIN.isContained(ROLE.FULLADMIN));
  }

  @Test
  public void testRoleContains() {
    assertTrue(ROLE.NOACCESS.contains(ROLE.NOACCESS));
    assertFalse(ROLE.NOACCESS.contains(ROLE.READONLY));
    assertFalse(ROLE.NOACCESS.contains(ROLE.TRANSFER));
    assertFalse(ROLE.NOACCESS.contains(ROLE.RULE));
    assertFalse(ROLE.NOACCESS.contains(ROLE.HOST));
    assertFalse(ROLE.NOACCESS.contains(ROLE.LIMIT));
    assertFalse(ROLE.NOACCESS.contains(ROLE.SYSTEM));
    assertFalse(ROLE.NOACCESS.contains(ROLE.LOGCONTROL));

    assertFalse(ROLE.READONLY.contains(ROLE.NOACCESS));
    assertTrue(ROLE.READONLY.contains(ROLE.READONLY));
    assertFalse(ROLE.READONLY.contains(ROLE.TRANSFER));
    assertFalse(ROLE.READONLY.contains(ROLE.RULE));
    assertFalse(ROLE.READONLY.contains(ROLE.HOST));
    assertFalse(ROLE.READONLY.contains(ROLE.LIMIT));
    assertFalse(ROLE.READONLY.contains(ROLE.SYSTEM));
    assertFalse(ROLE.READONLY.contains(ROLE.LOGCONTROL));

    assertFalse(ROLE.TRANSFER.contains(ROLE.NOACCESS));
    assertFalse(ROLE.TRANSFER.contains(ROLE.READONLY));
    assertTrue(ROLE.TRANSFER.contains(ROLE.TRANSFER));
    assertFalse(ROLE.TRANSFER.contains(ROLE.RULE));
    assertFalse(ROLE.TRANSFER.contains(ROLE.HOST));
    assertFalse(ROLE.TRANSFER.contains(ROLE.LIMIT));
    assertFalse(ROLE.TRANSFER.contains(ROLE.SYSTEM));
    assertFalse(ROLE.TRANSFER.contains(ROLE.LOGCONTROL));

    assertFalse(ROLE.RULE.contains(ROLE.NOACCESS));
    assertFalse(ROLE.RULE.contains(ROLE.READONLY));
    assertFalse(ROLE.RULE.contains(ROLE.TRANSFER));
    assertTrue(ROLE.RULE.contains(ROLE.RULE));
    assertFalse(ROLE.RULE.contains(ROLE.HOST));
    assertFalse(ROLE.RULE.contains(ROLE.LIMIT));
    assertFalse(ROLE.RULE.contains(ROLE.SYSTEM));
    assertFalse(ROLE.RULE.contains(ROLE.LOGCONTROL));

    assertFalse(ROLE.HOST.contains(ROLE.NOACCESS));
    assertFalse(ROLE.HOST.contains(ROLE.READONLY));
    assertFalse(ROLE.HOST.contains(ROLE.TRANSFER));
    assertFalse(ROLE.HOST.contains(ROLE.RULE));
    assertTrue(ROLE.HOST.contains(ROLE.HOST));
    assertFalse(ROLE.HOST.contains(ROLE.LIMIT));
    assertFalse(ROLE.HOST.contains(ROLE.SYSTEM));
    assertFalse(ROLE.HOST.contains(ROLE.LOGCONTROL));

    assertFalse(ROLE.LIMIT.contains(ROLE.NOACCESS));
    assertFalse(ROLE.LIMIT.contains(ROLE.READONLY));
    assertFalse(ROLE.LIMIT.contains(ROLE.TRANSFER));
    assertFalse(ROLE.LIMIT.contains(ROLE.RULE));
    assertFalse(ROLE.LIMIT.contains(ROLE.HOST));
    assertTrue(ROLE.LIMIT.contains(ROLE.LIMIT));
    assertFalse(ROLE.LIMIT.contains(ROLE.SYSTEM));
    assertFalse(ROLE.LIMIT.contains(ROLE.LOGCONTROL));

    assertFalse(ROLE.SYSTEM.contains(ROLE.NOACCESS));
    assertFalse(ROLE.SYSTEM.contains(ROLE.READONLY));
    assertFalse(ROLE.SYSTEM.contains(ROLE.TRANSFER));
    assertFalse(ROLE.SYSTEM.contains(ROLE.RULE));
    assertFalse(ROLE.SYSTEM.contains(ROLE.HOST));
    assertFalse(ROLE.SYSTEM.contains(ROLE.LIMIT));
    assertTrue(ROLE.SYSTEM.contains(ROLE.SYSTEM));
    assertFalse(ROLE.SYSTEM.contains(ROLE.LOGCONTROL));

    assertFalse(ROLE.LOGCONTROL.contains(ROLE.NOACCESS));
    assertFalse(ROLE.LOGCONTROL.contains(ROLE.READONLY));
    assertFalse(ROLE.LOGCONTROL.contains(ROLE.TRANSFER));
    assertFalse(ROLE.LOGCONTROL.contains(ROLE.RULE));
    assertFalse(ROLE.LOGCONTROL.contains(ROLE.HOST));
    assertFalse(ROLE.LOGCONTROL.contains(ROLE.LIMIT));
    assertFalse(ROLE.LOGCONTROL.contains(ROLE.SYSTEM));
    assertTrue(ROLE.LOGCONTROL.contains(ROLE.LOGCONTROL));

    assertFalse(ROLE.PARTNER.contains(ROLE.NOACCESS));
    assertTrue(ROLE.PARTNER.contains(ROLE.READONLY));
    assertTrue(ROLE.PARTNER.contains(ROLE.TRANSFER));
    assertTrue(ROLE.PARTNER.contains(ROLE.PARTNER));
    assertFalse(ROLE.PARTNER.contains(ROLE.RULE));
    assertFalse(ROLE.PARTNER.contains(ROLE.HOST));
    assertFalse(ROLE.PARTNER.contains(ROLE.LIMIT));
    assertFalse(ROLE.PARTNER.contains(ROLE.SYSTEM));
    assertFalse(ROLE.PARTNER.contains(ROLE.LOGCONTROL));

    assertFalse(ROLE.CONFIGADMIN.contains(ROLE.NOACCESS));
    assertTrue(ROLE.CONFIGADMIN.contains(ROLE.READONLY));
    assertTrue(ROLE.CONFIGADMIN.contains(ROLE.TRANSFER));
    assertTrue(ROLE.CONFIGADMIN.contains(ROLE.RULE));
    assertTrue(ROLE.CONFIGADMIN.contains(ROLE.HOST));
    assertTrue(ROLE.CONFIGADMIN.contains(ROLE.CONFIGADMIN));
    assertFalse(ROLE.CONFIGADMIN.contains(ROLE.LIMIT));
    assertFalse(ROLE.CONFIGADMIN.contains(ROLE.SYSTEM));
    assertFalse(ROLE.CONFIGADMIN.contains(ROLE.LOGCONTROL));

    assertFalse(ROLE.FULLADMIN.contains(ROLE.NOACCESS));
    assertTrue(ROLE.FULLADMIN.contains(ROLE.READONLY));
    assertTrue(ROLE.FULLADMIN.contains(ROLE.TRANSFER));
    assertTrue(ROLE.FULLADMIN.contains(ROLE.RULE));
    assertTrue(ROLE.FULLADMIN.contains(ROLE.HOST));
    assertTrue(ROLE.FULLADMIN.contains(ROLE.LIMIT));
    assertTrue(ROLE.FULLADMIN.contains(ROLE.SYSTEM));
    assertTrue(ROLE.FULLADMIN.contains(ROLE.LOGCONTROL));
    assertTrue(ROLE.FULLADMIN.contains(ROLE.FULLADMIN));
  }

  @Test
  public void testRoleValues() {
    assertEquals(0, ROLE.NOACCESS.getAsByte());
    assertEquals(1, ROLE.READONLY.getAsByte());
    assertEquals(2, ROLE.TRANSFER.getAsByte());
    assertEquals(4, ROLE.RULE.getAsByte());
    assertEquals(8, ROLE.HOST.getAsByte());
    assertEquals(16, ROLE.LIMIT.getAsByte());
    assertEquals(32, ROLE.SYSTEM.getAsByte());
    assertEquals(64, ROLE.LOGCONTROL.getAsByte());
    assertEquals(3, ROLE.PARTNER.getAsByte());
    assertEquals(15, ROLE.CONFIGADMIN.getAsByte());
    assertEquals(127, ROLE.FULLADMIN.getAsByte());

    assertEquals(ROLE.NOACCESS, ROLE.fromByte((byte) 0));
    assertEquals(ROLE.READONLY, ROLE.fromByte((byte) 1));
    assertEquals(ROLE.TRANSFER, ROLE.fromByte((byte) 2));
    assertEquals(ROLE.RULE, ROLE.fromByte((byte) 4));
    assertEquals(ROLE.HOST, ROLE.fromByte((byte) 8));
    assertEquals(ROLE.LIMIT, ROLE.fromByte((byte) 16));
    assertEquals(ROLE.SYSTEM, ROLE.fromByte((byte) 32));
    assertEquals(ROLE.LOGCONTROL, ROLE.fromByte((byte) 64));
    assertEquals(ROLE.PARTNER, ROLE.fromByte((byte) 3));
    assertEquals(ROLE.CONFIGADMIN, ROLE.fromByte((byte) 15));
    assertEquals(ROLE.FULLADMIN, ROLE.fromByte((byte) 127));
  }
}
