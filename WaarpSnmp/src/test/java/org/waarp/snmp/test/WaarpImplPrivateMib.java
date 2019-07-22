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
package org.waarp.snmp.test;

import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOScope;
import org.snmp4j.smi.Counter64;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.snmp.r66.WaarpPrivateMib;
import org.waarp.snmp.utils.WaarpMOScalar;

/**
 * Example of Implementation of WaarpPrivateMib
 *
 *
 */
public class WaarpImplPrivateMib extends WaarpPrivateMib {
  /**
   * Internal Logger
   */
  private static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpImplPrivateMib.class);

  /**
   * @param sysdesc
   * @param port
   * @param smiPrivateCodeFinal
   * @param typeWaarpObject
   * @param scontactName
   * @param stextualName
   * @param saddress
   * @param iservice
   */
  public WaarpImplPrivateMib(String sysdesc, int port, int smiPrivateCodeFinal,
                             int typeWaarpObject, String scontactName,
                             String stextualName, String saddress,
                             int iservice) {
    super(sysdesc, port, smiPrivateCodeFinal, typeWaarpObject, scontactName,
          stextualName, saddress, iservice);
  }

  @Override
  public void updateServices(WaarpMOScalar scalar) {
    // 3 groups to check
    final OID oid = scalar.getOid();
    if (oid.startsWith(rootOIDWaarpGlobal)) {
      // UpTime
      if (oid.equals(rootOIDWaarpGlobalUptime)) {
        return;
      }
      ((WaarpPrivateMonitor) agent.getMonitor()).generalValuesUpdate();
    } else if (oid.startsWith(rootOIDWaarpDetailed)) {
      ((WaarpPrivateMonitor) agent.getMonitor()).detailedValuesUpdate();
    } else if (oid.startsWith(rootOIDWaarpError)) {
      ((WaarpPrivateMonitor) agent.getMonitor()).errorValuesUpdate();
    }
  }

  @Override
  public void updateServices(MOScope range) {
    // UpTime first
    final OID low = range.getLowerBound();

    boolean okGeneral = true;
    boolean okDetailed = true;
    boolean okError = true;
    if (low != null) {
      logger.debug(
          "low: {}:{} " + rootOIDWaarpGlobal + ":" + rootOIDWaarpDetailed +
          ":" + rootOIDWaarpError, low, range.isLowerIncluded());
      if (low.size() <= rootOIDWaarp.size() && low.startsWith(rootOIDWaarp)) {
        // test for global requests
        okGeneral = okDetailed = okError = true;
      } else {
        // Test for sub requests
        okGeneral &= low.startsWith(rootOIDWaarpGlobal);
        okDetailed &= low.startsWith(rootOIDWaarpDetailed);
        okError &= low.startsWith(rootOIDWaarpError);
      }
    }
    logger.debug(
        "General:" + okGeneral + " Detailed:" + okDetailed + " Error:" +
        okError);
    if (okGeneral) {
      ((WaarpPrivateMonitor) agent.getMonitor()).generalValuesUpdate();
    }
    if (okDetailed) {
      ((WaarpPrivateMonitor) agent.getMonitor()).detailedValuesUpdate();
    }
    if (okError) {
      ((WaarpPrivateMonitor) agent.getMonitor()).errorValuesUpdate();
    }
  }

  @Override
  protected void agentRegisterWaarpMib() throws DuplicateRegistrationException {
    defaultAgentRegisterWaarpMib();
  }

  /**
   * Example of trap for All
   *
   * @param message
   * @param message2
   * @param number
   */
  public void notifyInfo(String message, String message2, int number) {
    if (!TrapLevel.All.isLevelValid(agent.getTrapLevel())) {
      return;
    }
    logger.warn(
        "Notify: " + NotificationElements.InfoTask + ":" + message + ":" +
        number);
    agent.getNotificationOriginator().notify(new OctetString("public"),
                                             NotificationElements.InfoTask
                                                 .getOID(rootOIDWaarpNotif),
                                             new VariableBinding[] {
                                                 new VariableBinding(
                                                     NotificationElements.InfoTask
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             NotificationTasks.stepStatusInfo
                                                                 .getOID()),
                                                     new OctetString(message)),
                                                 new VariableBinding(
                                                     NotificationElements.InfoTask
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             NotificationTasks.filenameInfo
                                                                 .getOID()),
                                                     new OctetString(message2)),
                                                 new VariableBinding(
                                                     NotificationElements.InfoTask
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             NotificationTasks.specialIdInfo
                                                                 .getOID()),
                                                     new Counter64(number)),
                                                 new VariableBinding(
                                                     NotificationElements.InfoTask
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             NotificationTasks.idRuleInfo
                                                                 .getOID()),
                                                     new OctetString(
                                                         NotificationElements.InfoTask
                                                             .name()))
                                             });
  }

  /**
   * Example of trap for Error
   *
   * @param message
   * @param message2
   * @param number
   */
  public void notifyError(String message, String message2, int number) {
    if (!TrapLevel.Alert.isLevelValid(agent.getTrapLevel())) {
      return;
    }
    logger.warn(
        "Notify: " + NotificationElements.TrapError + ":" + message + ":" +
        number);
    agent.getNotificationOriginator().notify(new OctetString("public"),
                                             NotificationElements.TrapError
                                                 .getOID(rootOIDWaarpNotif),
                                             new VariableBinding[] {
                                                 new VariableBinding(
                                                     NotificationElements.TrapError
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             1),
                                                     new OctetString(message)),
                                                 new VariableBinding(
                                                     NotificationElements.TrapError
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             1),
                                                     new OctetString(message2)),
                                                 new VariableBinding(
                                                     NotificationElements.TrapError
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             1),
                                                     new Counter64(number)),
                                                 new VariableBinding(
                                                     NotificationElements.TrapError
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             1),
                                                     new OctetString(
                                                         NotificationElements.TrapError
                                                             .name()))
                                             });
  }
}
