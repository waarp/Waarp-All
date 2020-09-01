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
package org.waarp.snmp.interf;

import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.MOScope;
import org.snmp4j.agent.mo.snmp.SNMPv2MIB;
import org.snmp4j.smi.OID;
import org.waarp.snmp.WaarpSnmpAgent;
import org.waarp.snmp.utils.WaarpMOScalar;

/**
 * Interface for All MIBs in Waarp project
 */
public interface WaarpInterfaceMib extends MOGroup {
  OID rootEnterpriseMib = new OID(".1.3.6.1.4.1");

  enum TrapLevel {
    /**
     * No Trap/Notification at all
     */
    None,
    /**
     * Trap/Notification only for start and stop
     */
    StartStop,
    /**
     * Trap/Notification up to high alert
     */
    Alert,
    /**
     * Trap/Notification up to warning alert
     */
    Warning,
    /**
     * Trap/Notification for all important elements
     */
    All,
    /**
     * Trap/Notification for all, whatever level
     */
    AllEvents;

    public boolean isLevelValid(final int level) {
      return level >= ordinal();
    }
  }

  /**
   * Set the agent
   *
   * @param agent
   */
  void setAgent(WaarpSnmpAgent agent);

  /**
   * @return the base OID for trap or notification of Start or Shutdown
   */
  OID getBaseOidStartOrShutdown();

  /**
   * @return the SNMPv2MIB associated with this MIB
   */
  SNMPv2MIB getSNMPv2MIB();

  /**
   * Update the row for these services
   *
   * @param scalar
   */
  void updateServices(WaarpMOScalar scalar);

  /**
   * Update the row for these services
   *
   * @param range
   */
  void updateServices(MOScope range);
}
