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
package org.waarp.snmp.r66;

import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.snmp.SNMPv2MIB;
import org.snmp4j.agent.mo.snmp.SysUpTime;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.TimeTicks;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.snmp.WaarpSnmpAgent;
import org.waarp.snmp.interf.WaarpInterfaceMib;
import org.waarp.snmp.utils.MemoryGauge32;
import org.waarp.snmp.utils.MemoryGauge32.MemoryType;
import org.waarp.snmp.utils.WaarpEntry;
import org.waarp.snmp.utils.WaarpMORow;
import org.waarp.snmp.utils.WaarpMOScalar;
import org.waarp.snmp.utils.WaarpUptime;

/**
 * Private MIB for Waarp OpenR66
 *
 *
 */
public abstract class WaarpPrivateMib implements WaarpInterfaceMib {
  /**
   * Internal Logger
   */
  private static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpPrivateMib.class);

  // These are both standard in RFC-1213
  /**
   * SnmpConstants.sysDescr
   */
  public String textualSysDecr = null;

  /**
   * SnmpConstants.sysObjectID
   */
  public OID ggObjectId = null; // will be smiPrivateCode.typeWaarp

  /**
   * SnmpConstants.sysContact
   */
  public String contactName = "Nobody";

  /**
   * SnmpConstants.sysName
   */
  public String textualName = "OpenR66";

  /**
   * SnmpConstants.sysLocation
   */
  public String address = "somewhere";

  /**
   * SnmpConstants.sysServices
   * <p>
   * transport + application
   */
  public int service = 72;

  /**
   * SnmpConstants.sysUpTime
   */
  public SysUpTime upTime = null;

  /**
   * need to add ".port" like "6666" Only in TCP (no UDP supported for Waarp)
   * <p>
   * example: rootEnterpriseMib+"66666"+".1.1.4.";
   */
  public String applicationProtocolBase = null;

  /**
   * will be = new OID(applicationProtocolBase+port);
   */
  public OID applicationProtocol = null;

  /**
   * Private MIB: not published so take an OID probably not attributed
   */
  public int smiPrivateCode = 66666;

  /**
   * identification of Waarp module
   */
  public int smiTypeWaarp = 66; // default = 66 = R66
  /**
   * root OID in String
   */
  public String srootOIDWaarp;
  /**
   * root OID
   */
  public OID rootOIDWaarp;

  /**
   * Used in Notify
   */
  public OID rootOIDWaarpNotif;

  /**
   * Used in Notify Start or Shutdown
   */
  public OID rootOIDWaarpNotifStartOrShutdown;

  /**
   * Info static part
   */
  public OID rootOIDWaarpInfo;
  /**
   * Info Row access
   */
  public WaarpMORow rowInfo;

  /**
   * Global dynamic part
   */
  public OID rootOIDWaarpGlobal;
  /**
   * Global Row access
   */
  public WaarpMORow rowGlobal;

  /**
   * Uptime OID
   */
  public OID rootOIDWaarpGlobalUptime;

  /**
   * Corresponding UpTime in Mib
   */
  public WaarpMOScalar scalarUptime = null;

  /**
   * Detailed dynamic part
   */
  public OID rootOIDWaarpDetailed;
  /**
   * Detailed Row access
   */
  public WaarpMORow rowDetailed;

  /**
   * Error dynamic part
   */
  public OID rootOIDWaarpError;
  /**
   * Error Row access
   */
  public WaarpMORow rowError;

  /**
   * New SNMPV2 MIB
   */
  public SNMPv2MIB snmpv2;

  /**
   * Corresponding agent
   */
  public WaarpSnmpAgent agent;

  /**
   * @param sysdesc The System Description to associate
   * @param port the port to show as used by the application
   * @param smiPrivateCodeFinal the smiPrivateCode (should be 66666)
   * @param typeWaarpObject the type of Waarp Object (should be 66)
   * @param scontactName the contact name to show
   * @param stextualName the textual name to show
   * @param saddress the address to show
   * @param iservice the service to show (should be 72)
   */
  public WaarpPrivateMib(String sysdesc, int port, int smiPrivateCodeFinal,
                         int typeWaarpObject, String scontactName,
                         String stextualName, String saddress, int iservice) {
    textualSysDecr = sysdesc;
    smiPrivateCode = smiPrivateCodeFinal;
    smiTypeWaarp = typeWaarpObject;
    contactName = scontactName;
    textualName = stextualName;
    address = saddress;
    service = iservice;
    srootOIDWaarp = rootEnterpriseMib.toString() + "." + smiPrivateCode + "." +
                    smiTypeWaarp;
    applicationProtocolBase = srootOIDWaarp + ".1.1.4.";
    ggObjectId = new OID(srootOIDWaarp);
    applicationProtocol = new OID(applicationProtocolBase + port);
    rootOIDWaarp = new OID(srootOIDWaarp);
    rootOIDWaarpInfo = new OID(srootOIDWaarp + ".1");
    rootOIDWaarpGlobal = new OID(srootOIDWaarp + ".2");
    rootOIDWaarpGlobalUptime = new OID(rootOIDWaarpGlobal.toString() + "." +
                                       WaarpGlobalValuesIndex.applUptime
                                           .getOID() + ".0");
    rootOIDWaarpDetailed = new OID(srootOIDWaarp + ".3");
    rootOIDWaarpError = new OID(srootOIDWaarp + ".4");
    rootOIDWaarpNotif = new OID(srootOIDWaarp + ".5.1");
    rootOIDWaarpNotifStartOrShutdown = new OID(srootOIDWaarp + ".5.1.1.1");
  }

  @Override
  public void setAgent(WaarpSnmpAgent agent) {
    this.agent = agent;
  }

  @Override
  public OID getBaseOidStartOrShutdown() {
    return rootOIDWaarpNotifStartOrShutdown;
  }

  @Override
  public SNMPv2MIB getSNMPv2MIB() {
    return snmpv2;
  }

  /**
   * Unregister and Register again the SNMPv2MIB with System adapted to this
   * Mib
   *
   * @throws DuplicateRegistrationException
   */
  protected void agentRegisterSystem() throws DuplicateRegistrationException {
    // Since BaseAgent registers some mibs by default we need to unregister
    // one before we register our own sysDescr. Normally you would
    // override that method and register the mibs that you need

    agent.unregisterManagedObject(agent.getSnmpv2MIB());

    // Register a system description, use one from you product environment
    // to test with
    snmpv2 = new SNMPv2MIB(new OctetString(textualSysDecr), ggObjectId,
                           new Integer32(service));
    snmpv2.setContact(new OctetString(contactName));
    snmpv2.setLocation(new OctetString(address));
    snmpv2.setName(new OctetString(textualName));
    snmpv2.registerMOs(agent.getServer(), null);
    if (logger.isDebugEnabled()) {
      logger.debug(
          "SNMPV2: " + snmpv2.getContact() + ":" + snmpv2.getDescr() + ":" +
          snmpv2.getLocation() + ":" + snmpv2.getName() + ":" +
          snmpv2.getObjectID() + ":" + snmpv2.getServices() + ":" +
          snmpv2.getUpTime());
    }
    // Save UpTime reference since used everywhere
    upTime = snmpv2.getSysUpTime();
  }

  /**
   * Register this MIB
   *
   * @throws DuplicateRegistrationException
   */
  protected void defaultAgentRegisterWaarpMib()
      throws DuplicateRegistrationException {
    // register Static info
    rowInfo = new WaarpMORow(this, rootOIDWaarpInfo, WaarpDefinition,
                             MibLevel.staticInfo.ordinal());
    rowInfo.registerMOs(agent.getServer(), null);
    // register General info
    rowGlobal = new WaarpMORow(this, rootOIDWaarpGlobal, WaarpGlobalValues,
                               MibLevel.globalInfo.ordinal());
    WaarpMOScalar memoryScalar =
        rowGlobal.getRow()[WaarpGlobalValuesIndex.memoryTotal.ordinal()];
    memoryScalar.setValue(new MemoryGauge32(MemoryType.TotalMemory));
    memoryScalar =
        rowGlobal.getRow()[WaarpGlobalValuesIndex.memoryFree.ordinal()];
    memoryScalar.setValue(new MemoryGauge32(MemoryType.FreeMemory));
    memoryScalar =
        rowGlobal.getRow()[WaarpGlobalValuesIndex.memoryUsed.ordinal()];
    memoryScalar.setValue(new MemoryGauge32(MemoryType.UsedMemory));
    rowGlobal.registerMOs(agent.getServer(), null);
    // setup UpTime to SysUpTime and change status
    scalarUptime =
        rowGlobal.getRow()[WaarpGlobalValuesIndex.applUptime.ordinal()];
    scalarUptime.setValue(new WaarpUptime(upTime));
    changeStatus(OperStatus.restarting);
    changeStatus(OperStatus.up);
    // register Detailed info
    rowDetailed =
        new WaarpMORow(this, rootOIDWaarpDetailed, WaarpDetailedValues,
                       MibLevel.detailedInfo.ordinal());
    rowDetailed.registerMOs(agent.getServer(), null);
    // register Error info
    rowError = new WaarpMORow(this, rootOIDWaarpError, WaarpErrorValues,
                              MibLevel.errorInfo.ordinal());
    rowError.registerMOs(agent.getServer(), null);
  }

  /**
   * Register this MIB
   *
   * @throws DuplicateRegistrationException
   */
  protected abstract void agentRegisterWaarpMib()
      throws DuplicateRegistrationException;

  /**
   * Unregister this MIB
   */
  protected void agentUnregisterMibs() {
    logger.debug("UnRegisterWaarp");
    rowInfo.unregisterMOs(agent.getServer(), agent.getDefaultContext());
    rowGlobal.unregisterMOs(agent.getServer(), agent.getDefaultContext());
    rowDetailed.unregisterMOs(agent.getServer(), agent.getDefaultContext());
    rowError.unregisterMOs(agent.getServer(), agent.getDefaultContext());
  }

  @Override
  public void registerMOs(MOServer server, OctetString context)
      throws DuplicateRegistrationException {
    agentRegisterSystem();
    agentRegisterWaarpMib();
  }

  @Override
  public void unregisterMOs(MOServer server, OctetString context) {
    agentUnregisterMibs();
  }

  /**
   * Change the status and the LastChange Timeticks
   *
   * @param status
   */
  public void changeStatus(OperStatus status) {
    final WaarpMOScalar statusScalar =
        rowGlobal.getRow()[WaarpGlobalValuesIndex.applOperStatus.ordinal()];
    final Integer32 var = (Integer32) statusScalar.getValue();
    if (var.getValue() != status.status) {
      var.setValue(status.status);
      final WaarpMOScalar lastTimeScalar =
          rowGlobal.getRow()[WaarpGlobalValuesIndex.applLastChange.ordinal()];
      final TimeTicks time = (TimeTicks) lastTimeScalar.getValue();
      time.setValue(upTime.get().getValue());
    }
  }

  /**
   * MIB entry levels
   *
   *
   */
  public static enum MibLevel {
    staticInfo, globalInfo, detailedInfo, errorInfo, trapInfo
  }

  // From now the MIB definition

  /**
   * Notification Elements
   *
   *
   */
  public static enum NotificationElements {
    TrapShutdown(1), TrapError(2), TrapWarning(3), TrapOverloaded(4),
    InfoTask(5);

    public int[] oid;

    private NotificationElements(int oid) {
      this.oid = new int[] { oid };
    }

    public OID getOID(OID oidBase) {
      return new OID(oidBase.getValue(), oid);
    }

    public OID getOID(OID oidBase, int rank) {
      final int[] ids = new int[] { oid[0], rank };
      return new OID(oidBase.getValue(), ids);
    }
  }

  /**
   * Notification for a task trap
   *
   *
   */
  public static enum NotificationTasks {
    globalStepInfo, stepInfo, rankFileInfo, stepStatusInfo, filenameInfo,
    originalNameInfo, idRuleInfo, modeTransInfo, retrieveModeInfo,
    startTransInfo, infoStatusInfo, requesterInfo, requestedInfo, specialIdInfo;

    public int getOID() {
      return ordinal() + 1;
    }
  }

  /**
   * Definition part
   *
   *
   */
  public static enum WaarpDefinitionIndex {
    applName, applServerName, applVersion, applDescription, applURL,
    applApplicationProtocol;

    public int getOID() {
      return ordinal() + 1;
    }
  }

  /**
   * Definition part
   */
  public static WaarpEntry[] WaarpDefinition = {
      // applName
      new WaarpEntry(SMIConstants.SYNTAX_OCTET_STRING,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applServerName
      new WaarpEntry(SMIConstants.SYNTAX_OCTET_STRING,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applVersion
      new WaarpEntry(SMIConstants.SYNTAX_OCTET_STRING,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applDescription
      new WaarpEntry(SMIConstants.SYNTAX_OCTET_STRING,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applURL
      new WaarpEntry(SMIConstants.SYNTAX_OCTET_STRING,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applApplicationProtocol
      new WaarpEntry(SMIConstants.SYNTAX_OBJECT_IDENTIFIER,
                     MOAccessImpl.ACCESS_READ_ONLY)
  };

  /**
   * Global part
   *
   *
   */
  public static enum WaarpGlobalValuesIndex {
    applUptime, applOperStatus, applLastChange, applInboundAssociations,
    applOutboundAssociations, applAccumInboundAssociations,
    applAccumOutboundAssociations, applLastInboundActivity,
    applLastOutboundActivity, applRejectedInboundAssociations,
    applFailedOutboundAssociations, applInboundBandwidthKBS,
    applOutboundBandwidthKBS, nbInfoUnknown, nbInfoNotUpdated,
    nbInfoInterrupted, nbInfoToSubmit, nbInfoError, nbInfoRunning, nbInfoDone,
    nbStepAllTransfer, memoryTotal, memoryFree, memoryUsed, nbThreads,
    nbNetworkConnection;

    public int getOID() {
      return ordinal() + 1;
    }
  }

  /**
   * Global part
   */
  public static WaarpEntry[] WaarpGlobalValues = {
      // applUptime
      new WaarpEntry(SMIConstants.SYNTAX_TIMETICKS,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applOperStatus
      new WaarpEntry(SMIConstants.SYNTAX_INTEGER,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applLastChange
      new WaarpEntry(SMIConstants.SYNTAX_TIMETICKS,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applInboundAssociations
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applOutboundAssociations
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applAccumInboundAssociations
      new WaarpEntry(SMIConstants.SYNTAX_COUNTER32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applAccumOutboundAssociations
      new WaarpEntry(SMIConstants.SYNTAX_COUNTER32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applLastInboundActivity
      new WaarpEntry(SMIConstants.SYNTAX_TIMETICKS,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applLastOutboundActivity
      new WaarpEntry(SMIConstants.SYNTAX_TIMETICKS,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applRejectedInboundAssociations
      new WaarpEntry(SMIConstants.SYNTAX_COUNTER32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applFailedOutboundAssociations
      new WaarpEntry(SMIConstants.SYNTAX_COUNTER32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // Bandwidth
      // applInboundBandwidthKBS
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // applOutboundBandwidthKBS
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // Overall status including past, future and current transfers
      // nbInfoUnknown
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbInfoNotUpdated
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbInfoInterrupted
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbInfoToSubmit
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbInfoError
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbInfoRunning
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbInfoDone
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // Current situation of all transfers, running or not
      // nbStepAllTransfer
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // memoryTotal
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // memoryFree
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // memoryUsed
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbThreads
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbNetworkConnection
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32, MOAccessImpl.ACCESS_READ_ONLY)
  };

  /**
   * Detailed part
   *
   *
   */
  public static enum WaarpDetailedValuesIndex {
    nbStepNotask, nbStepPretask, nbStepTransfer, nbStepPosttask, nbStepAllDone,
    nbStepError, nbAllRunningStep, nbRunningStep, nbInitOkStep,
    nbPreProcessingOkStep, nbTransferOkStep, nbPostProcessingOkStep,
    nbCompleteOkStep;

    public int getOID() {
      return ordinal() + 1;
    }
  }

  /**
   * Detailed part
   */
  public static WaarpEntry[] WaarpDetailedValues = {
      // nbStepNotask
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStepPretask
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStepTransfer
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStepPosttask
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStepAllDone
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStepError
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // First on Running Transfers only
      // nbAllRunningStep
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbRunningStep
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbInitOkStep
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbPreProcessingOkStep
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbTransferOkStep
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbPostProcessingOkStep
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbCompleteOkStep
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32, MOAccessImpl.ACCESS_READ_ONLY)
  };

  /**
   * Error part
   *
   *
   */
  public static enum WaarpErrorValuesIndex {
    nbStatusConnectionImpossible, nbStatusServerOverloaded, nbStatusBadAuthent,
    nbStatusExternalOp, nbStatusTransferError, nbStatusMD5Error,
    nbStatusDisconnection, nbStatusFinalOp, nbStatusUnimplemented,
    nbStatusInternal, nbStatusWarning, nbStatusQueryAlreadyFinished,
    nbStatusQueryStillRunning, nbStatusNotKnownHost,
    nbStatusQueryRemotelyUnknown, nbStatusCommandNotFound,
    nbStatusPassThroughMode, nbStatusRemoteShutdown, nbStatusShutdown,
    nbStatusRemoteError, nbStatusStopped, nbStatusCanceled,
    nbStatusFileNotFound, nbStatusUnknown;

    public int getOID() {
      return ordinal() + 1;
    }
  }

  /**
   * Error part
   */
  public static WaarpEntry[] WaarpErrorValues = {
      // Error Status on all transfers
      // nbStatusConnectionImpossible
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusServerOverloaded
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusBadAuthent
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusExternalOp
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusTransferError
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusMD5Error
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusDisconnection
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusFinalOp
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusUnimplemented
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusInternal
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusWarning
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusQueryAlreadyFinished
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusQueryStillRunning
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusNotKnownHost
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusQueryRemotelyUnknown
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusCommandNotFound
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusPassThroughMode
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusRemoteShutdown
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusShutdown
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusRemoteError
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusStopped
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusCanceled
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusFileNotFound
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // nbStatusUnknown
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32, MOAccessImpl.ACCESS_READ_ONLY)
  };

  /**
   * Oper Status (as defined in Net Application SNMP)
   *
   *
   */
  public static enum OperStatus {
    up(1), down(2), halted(3), congested(4), restarting(5), quiescing(6);

    public int status;

    private OperStatus(int status) {
      this.status = status;
    }
  }
}
