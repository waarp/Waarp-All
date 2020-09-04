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
package org.waarp.gateway.ftp.snmp;

import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOScope;
import org.snmp4j.agent.MOServer;
import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.snmp.SNMPv2MIB;
import org.snmp4j.agent.mo.snmp.SysUpTime;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.VariableBinding;
import org.waarp.common.command.ReplyCode;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.Version;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.gateway.ftp.database.data.DbTransferLog;
import org.waarp.snmp.WaarpSnmpAgent;
import org.waarp.snmp.interf.WaarpInterfaceMib;
import org.waarp.snmp.r66.WaarpPrivateMib;
import org.waarp.snmp.utils.MemoryGauge32;
import org.waarp.snmp.utils.MemoryGauge32.MemoryType;
import org.waarp.snmp.utils.WaarpEntry;
import org.waarp.snmp.utils.WaarpMORow;
import org.waarp.snmp.utils.WaarpMOScalar;
import org.waarp.snmp.utils.WaarpUptime;

/**
 * FTP Private MIB implementation
 */
public class FtpPrivateMib implements WaarpInterfaceMib {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpPrivateMib.class);

  public static final String SnmpName = "Waarp GW FTP SNMP";

  public static final int SnmpPrivateId = 66666;

  public static final int SnmpFtpId = 21;

  public static final String SnmpDefaultAuthor = "Frederic Bregier";

  public static final String SnmpVersion =
      "Waarp GW FTP " + Version.fullIdentifier();

  public static final String SnmpDefaultLocalization = "Paris, France";

  public static final int SnmpService = 72;

  /**
   * SnmpConstants.sysObjectID
   */
  public final OID ggObjectId; // will be smiPrivateCode.typeWaarp

  /**
   * SnmpConstants.sysUpTime
   */
  public SysUpTime upTime;

  /**
   * need to add ".port" like "6666" Only in TCP (no UDP supported for Waarp)
   * <p>
   * example: rootEnterpriseMib+"66666"+".1.1.4.";
   */
  public final String applicationProtocolBase;

  /**
   * will be = new OID(applicationProtocolBase+port);
   */
  public final OID applicationProtocol;

  /**
   * root OID in String
   */
  public final String srootOIDWaarp;

  /**
   * root OID
   */
  public final OID rootOIDWaarp;

  /**
   * Used in Notify
   */
  public final OID rootOIDWaarpNotif;

  /**
   * Used in Notify Start or Shutdown
   */
  public final OID rootOIDWaarpNotifStartOrShutdown;

  /**
   * Info static part
   */
  public final OID rootOIDWaarpInfo;

  /**
   * Info Row access
   */
  public WaarpMORow rowInfo;

  /**
   * Global dynamic part
   */
  public final OID rootOIDWaarpGlobal;

  /**
   * Global Row access
   */
  public WaarpMORow rowGlobal;

  /**
   * Uptime OID
   */
  public final OID rootOIDWaarpGlobalUptime;

  /**
   * Corresponding UpTime in Mib
   */
  public WaarpMOScalar scalarUptime;

  /**
   * Detailed dynamic part
   */
  public final OID rootOIDWaarpDetailed;

  /**
   * Detailed Row access
   */
  public WaarpMORow rowDetailed;

  /**
   * Error dynamic part
   */
  public final OID rootOIDWaarpError;

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
   * @param port port used by FTP server
   */
  public FtpPrivateMib(final int port) {
    srootOIDWaarp = rootEnterpriseMib + "." + SnmpPrivateId + '.' + SnmpFtpId;
    applicationProtocolBase = srootOIDWaarp + ".1.1.4.";
    ggObjectId = new OID(srootOIDWaarp);
    applicationProtocol = new OID(applicationProtocolBase + port);
    rootOIDWaarp = new OID(srootOIDWaarp);
    rootOIDWaarpInfo = new OID(srootOIDWaarp + ".1");
    rootOIDWaarpGlobal = new OID(srootOIDWaarp + ".2");
    rootOIDWaarpGlobalUptime = new OID(
        rootOIDWaarpGlobal + "." + WaarpGlobalValuesIndex.applUptime.getOID() +
        ".0");
    rootOIDWaarpDetailed = new OID(srootOIDWaarp + ".3");
    rootOIDWaarpError = new OID(srootOIDWaarp + ".4");
    rootOIDWaarpNotif = new OID(srootOIDWaarp + ".5.1");
    rootOIDWaarpNotifStartOrShutdown = new OID(srootOIDWaarp + ".5.1.1.1");
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
    snmpv2 = new SNMPv2MIB(new OctetString(SnmpName), ggObjectId,
                           new Integer32(SnmpService));
    snmpv2.setContact(new OctetString(SnmpDefaultAuthor));
    snmpv2.setLocation(new OctetString(SnmpDefaultLocalization));
    snmpv2.setName(new OctetString(SnmpVersion));
    snmpv2.registerMOs(agent.getServer(), null);
    if (logger.isDebugEnabled()) {
      logger.debug(
          "SNMPV2: " + snmpv2.getContact() + ':' + snmpv2.getDescr() + ':' +
          snmpv2.getLocation() + ':' + snmpv2.getName() + ':' +
          snmpv2.getObjectID() + ':' + snmpv2.getServices() + ':' +
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
  protected void agentRegisterWaarpMib() throws DuplicateRegistrationException {
    logger.debug("registerGGMib");
    // register Static info
    rowInfo = new WaarpMORow(this, rootOIDWaarpInfo, WaarpDefinition,
                             MibLevel.staticInfo.ordinal());
    rowInfo.setValue(WaarpDefinitionIndex.applName.ordinal(),
                     "Waarp " + "GatewayFtp");
    rowInfo.setValue(WaarpDefinitionIndex.applServerName.ordinal(),
                     FileBasedConfiguration.fileBasedConfiguration.getHostId());
    rowInfo.setValue(WaarpDefinitionIndex.applVersion.ordinal(),
                     Version.fullIdentifier());
    rowInfo.setValue(WaarpDefinitionIndex.applDescription.ordinal(),
                     "Waarp Gateway Ftp: FTP Gateway");
    rowInfo.setValue(WaarpDefinitionIndex.applURL.ordinal(),
                     "http://waarp.github.com/Waarp");
    rowInfo.setValue(WaarpDefinitionIndex.applApplicationProtocol.ordinal(),
                     applicationProtocol);

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
  public void registerMOs(final MOServer moServer,
                          final OctetString octetString)
      throws DuplicateRegistrationException {
    agentRegisterSystem();
    agentRegisterWaarpMib();
  }

  @Override
  public void unregisterMOs(final MOServer moServer,
                            final OctetString octetString) {
    agentUnregisterMibs();
  }

  @Override
  public void setAgent(final WaarpSnmpAgent agent) {
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

  @Override
  public void updateServices(final WaarpMOScalar scalar) {
    // Nothing
  }

  @Override
  public void updateServices(final MOScope range) {
    // Nothing
  }

  /**
   * Change the status and the LastChange Timeticks
   *
   * @param status
   */
  public void changeStatus(final OperStatus status) {
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
   * Send a notification (trap or inform) for Shutdown event
   *
   * @param message
   * @param message2
   */
  public void notifyStartStop(final String message, final String message2) {
    if (!TrapLevel.StartStop.isLevelValid(agent.getTrapLevel())) {
      return;
    }
    notify(NotificationElements.TrapShutdown, message, message2);
  }

  /**
   * Send a notification (trap or inform) for Error event
   *
   * @param message
   * @param message2
   */
  public void notifyError(final String message, final String message2) {
    if (!TrapLevel.Alert.isLevelValid(agent.getTrapLevel())) {
      return;
    }
    notify(NotificationElements.TrapError, message, message2);
  }

  /**
   * Send a notification (trap or inform) for Server Overloaded event
   *
   * @param message
   * @param message2
   */
  public void notifyOverloaded(final String message, final String message2) {
    if (!TrapLevel.Warning.isLevelValid(agent.getTrapLevel())) {
      return;
    }
    notify(NotificationElements.TrapOverloaded, message, message2);
  }

  /**
   * Send a notification (trap or inform) for Warning event
   *
   * @param message
   * @param message2
   */
  public void notifyWarning(final String message, final String message2) {
    if (!TrapLevel.Warning.isLevelValid(agent.getTrapLevel())) {
      return;
    }
    notify(NotificationElements.TrapWarning, message, message2);
  }

  /**
   * Send a notification (trap or inform) for Warning/Error event on a single
   * Transfer Task
   *
   * @param message
   * @param runner
   */
  public void notifyInfoTask(final String message, final DbTransferLog runner) {
    if (!TrapLevel.All.isLevelValid(agent.getTrapLevel())) {
      return;
    }
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Notify: " + NotificationElements.InfoTask + ':' + message + ':' +
          runner);
    }
    long delay =
        (runner.getStart().getTime() - agent.getUptimeSystemTime()) / 10;
    if (delay < 0) {
      delay = 0;
    }
    agent.getNotificationOriginator().notify(new OctetString("public"),
                                             NotificationElements.InfoTask
                                                 .getOID(rootOIDWaarpNotif),
                                             new VariableBinding[] {
                                                 new VariableBinding(
                                                     NotificationElements.InfoTask
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             1),
                                                     new OctetString(
                                                         NotificationElements.InfoTask
                                                             .name())),
                                                 new VariableBinding(
                                                     NotificationElements.InfoTask
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             1),
                                                     new OctetString(message)),
                                                 // Start of Task
                                                 new VariableBinding(
                                                     NotificationElements.InfoTask
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             NotificationTasks.filenameInfo
                                                                 .getOID()),
                                                     new OctetString(
                                                         runner.getFilename())),
                                                 new VariableBinding(
                                                     NotificationElements.InfoTask
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             NotificationTasks.modeTransInfo
                                                                 .getOID()),
                                                     new OctetString(
                                                         runner.getMode())),
                                                 new VariableBinding(
                                                     NotificationElements.InfoTask
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             NotificationTasks.startTransInfo
                                                                 .getOID()),
                                                     new TimeTicks(delay)),
                                                 new VariableBinding(
                                                     NotificationElements.InfoTask
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             NotificationTasks.infoStatusInfo
                                                                 .getOID()),
                                                     new OctetString(
                                                         runner.getErrorInfo()
                                                               .getMesg())),
                                                 new VariableBinding(
                                                     NotificationElements.InfoTask
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             NotificationTasks.userIdInfo
                                                                 .getOID()),
                                                     new OctetString(
                                                         runner.getUser())),
                                                 new VariableBinding(
                                                     NotificationElements.InfoTask
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             NotificationTasks.accountId
                                                                 .getOID()),
                                                     new OctetString(
                                                         runner.getAccount())),
                                                 new VariableBinding(
                                                     NotificationElements.InfoTask
                                                         .getOID(
                                                             rootOIDWaarpNotif,
                                                             NotificationTasks.specialIdInfo
                                                                 .getOID()),
                                                     new OctetString(String
                                                                         .valueOf(
                                                                             runner
                                                                                 .getSpecialId()))),
                                                 // End of Task
                                                 new VariableBinding(
                                                     SnmpConstants.sysDescr,
                                                     snmpv2.getDescr()),
                                                 new VariableBinding(
                                                     SnmpConstants.sysObjectID,
                                                     snmpv2.getObjectID()),
                                                 new VariableBinding(
                                                     SnmpConstants.sysContact,
                                                     snmpv2.getContact()),
                                                 new VariableBinding(
                                                     SnmpConstants.sysName,
                                                     snmpv2.getName()),
                                                 new VariableBinding(
                                                     SnmpConstants.sysLocation,
                                                     snmpv2.getLocation())
                                             });
  }

  /**
   * Trap/Notification
   *
   * @param element
   * @param message
   * @param message2
   */
  private void notify(final NotificationElements element, final String message,
                      final String message2) {
    if (logger.isDebugEnabled()) {
      logger.debug("Notify: " + element + ':' + message + ':' + message2);
    }
    agent.getNotificationOriginator()
         .notify(new OctetString("public"), element.getOID(rootOIDWaarpNotif),
                 new VariableBinding[] {
                     new VariableBinding(element.getOID(rootOIDWaarpNotif, 1),
                                         new OctetString(element.name())),
                     new VariableBinding(element.getOID(rootOIDWaarpNotif, 1),
                                         new OctetString(message)),
                     new VariableBinding(element.getOID(rootOIDWaarpNotif, 1),
                                         new OctetString(message2)),
                     new VariableBinding(SnmpConstants.sysDescr,
                                         snmpv2.getDescr()),
                     new VariableBinding(SnmpConstants.sysObjectID,
                                         snmpv2.getObjectID()),
                     new VariableBinding(SnmpConstants.sysContact,
                                         snmpv2.getContact()),
                     new VariableBinding(SnmpConstants.sysName,
                                         snmpv2.getName()),
                     new VariableBinding(SnmpConstants.sysLocation,
                                         snmpv2.getLocation())
                 });
  }

  /**
   * MIB entry levels
   */
  public enum MibLevel {
    staticInfo, globalInfo, detailedInfo, errorInfo, trapInfo
  }

  // From now the MIB definition

  /**
   * Notification Elements
   */
  public enum NotificationElements {
    TrapShutdown(1), TrapError(2), TrapWarning(3), TrapOverloaded(4),
    InfoTask(5);

    public final int[] oid;

    NotificationElements(final int oid) {
      this.oid = new int[] { oid };
    }

    public OID getOID(final OID oidBase) {
      return new OID(oidBase.getValue(), oid);
    }

    public OID getOID(final OID oidBase, final int rank) {
      final int[] ids = { oid[0], rank };
      return new OID(oidBase.getValue(), ids);
    }
  }

  /**
   * Notification for a task trap
   */
  public enum NotificationTasks {
    filenameInfo, modeTransInfo, startTransInfo, infoStatusInfo, userIdInfo,
    accountId, specialIdInfo;

    public int getOID() {
      return ordinal() + 1;
    }
  }

  /**
   * Definition part
   */
  public enum WaarpDefinitionIndex {
    applName, applServerName, applVersion, applDescription, applURL,
    applApplicationProtocol;

    public int getOID() {
      return ordinal() + 1;
    }
  }

  /**
   * Definition part
   */
  public static final WaarpEntry[] WaarpDefinition =
      WaarpPrivateMib.WaarpDefinition;

  /**
   * Global part
   */
  public enum WaarpGlobalValuesIndex {
    applUptime, applOperStatus, applLastChange, applInboundAssociations,
    applOutboundAssociations, applAccumInboundAssociations,
    applAccumOutboundAssociations, applLastInboundActivity,
    applLastOutboundActivity, applRejectedInboundAssociations,
    applFailedOutboundAssociations, applInboundBandwidthKBS,
    applOutboundBandwidthKBS, nbInfoUnknown, nbInfoNotUpdated,
    nbInfoInterrupted, nbInfoToSubmit, nbInfoError, nbInfoRunning, nbInfoDone,
    nbAllTransfer, memoryTotal, memoryFree, memoryUsed, nbThreads,
    nbNetworkConnection;

    public int getOID() {
      return ordinal() + 1;
    }
  }

  /**
   * Global part
   */
  public static final WaarpEntry[] WaarpGlobalValues =
      WaarpPrivateMib.WaarpGlobalValues;

  /**
   * Detailed part
   */
  public enum WaarpDetailedValuesIndex {
    reply_000(ReplyCode.REPLY_000_SPECIAL_NOSTATUS),
    reply_110(ReplyCode.REPLY_110_RESTART_MARKER_REPLY),
    reply_120(ReplyCode.REPLY_120_SERVICE_READY_IN_NNN_MINUTES),
    reply_125(ReplyCode.REPLY_125_DATA_CONNECTION_ALREADY_OPEN),
    reply_150(ReplyCode.REPLY_150_FILE_STATUS_OKAY),
    reply_200(ReplyCode.REPLY_200_COMMAND_OKAY),
    reply_202(ReplyCode.REPLY_202_COMMAND_NOT_IMPLEMENTED),
    reply_211(ReplyCode.REPLY_211_SYSTEM_STATUS_REPLY),
    reply_212(ReplyCode.REPLY_212_DIRECTORY_STATUS),
    reply_213(ReplyCode.REPLY_213_FILE_STATUS),
    reply_214(ReplyCode.REPLY_214_HELP_MESSAGE),
    reply_215(ReplyCode.REPLY_215_NAME_SYSTEM_TYPE),
    reply_220(ReplyCode.REPLY_220_SERVICE_READY),
    reply_221(ReplyCode.REPLY_221_CLOSING_CONTROL_CONNECTION),
    reply_225(ReplyCode.REPLY_225_DATA_CONNECTION_OPEN_NO_TRANSFER_IN_PROGRESS),
    reply_226(ReplyCode.REPLY_226_CLOSING_DATA_CONNECTION),
    reply_227(ReplyCode.REPLY_227_ENTERING_PASSIVE_MODE),
    reply_229(ReplyCode.REPLY_229_ENTERING_PASSIVE_MODE),
    reply_230(ReplyCode.REPLY_230_USER_LOGGED_IN),
    reply_232(ReplyCode.REPLY_232_USER_LOGGED_IN),
    reply_234(ReplyCode.REPLY_234_SECURITY_DATA_EXCHANGE_COMPLETE),
    reply_250(ReplyCode.REPLY_250_REQUESTED_FILE_ACTION_OKAY),
    reply_257(ReplyCode.REPLY_257_PATHNAME_CREATED),
    reply_331(ReplyCode.REPLY_331_USER_NAME_OKAY_NEED_PASSWORD),
    reply_332(ReplyCode.REPLY_332_NEED_ACCOUNT_FOR_LOGIN), reply_350(
        ReplyCode.REPLY_350_REQUESTED_FILE_ACTION_PENDING_FURTHER_INFORMATION);

    public final ReplyCode code;

    WaarpDetailedValuesIndex(final ReplyCode code) {
      this.code = code;
    }

    public int getOID() {
      return ordinal() + 1;
    }
  }

  /**
   * Detailed part
   */
  public static final WaarpEntry[] WaarpDetailedValues = {
      // reply_000,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_110,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_120,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_125,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_150,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_200,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_202,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_211,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_212,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_213,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_214,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_215,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_220,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_221,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_225,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_226,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_227,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_229,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_230,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_232,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_234,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_250,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_257,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_331,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_332,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_350
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32, MOAccessImpl.ACCESS_READ_ONLY)
  };

  /**
   * Error part
   */
  public enum WaarpErrorValuesIndex {
    reply_421(
        ReplyCode.REPLY_421_SERVICE_NOT_AVAILABLE_CLOSING_CONTROL_CONNECTION),
    reply_425(ReplyCode.REPLY_425_CANT_OPEN_DATA_CONNECTION),
    reply_426(ReplyCode.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED),
    reply_431(
        ReplyCode.REPLY_431_NEED_UNAVAILABLE_RESOURCE_TO_PROCESS_SECURITY),
    reply_450(ReplyCode.REPLY_450_REQUESTED_FILE_ACTION_NOT_TAKEN),
    reply_451(ReplyCode.REPLY_451_REQUESTED_ACTION_ABORTED),
    reply_452(ReplyCode.REPLY_452_REQUESTED_ACTION_NOT_TAKEN),
    reply_500(ReplyCode.REPLY_500_SYNTAX_ERROR_COMMAND_UNRECOGNIZED),
    reply_501(ReplyCode.REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS),
    reply_502(ReplyCode.REPLY_502_COMMAND_NOT_IMPLEMENTED),
    reply_503(ReplyCode.REPLY_503_BAD_SEQUENCE_OF_COMMANDS),
    reply_504(ReplyCode.REPLY_504_COMMAND_NOT_IMPLEMENTED_FOR_THAT_PARAMETER),
    reply_522(
        ReplyCode.REPLY_522_EXTENDED_PORT_FAILURE_UNKNOWN_NETWORK_PROTOCOL),
    reply_530(ReplyCode.REPLY_530_NOT_LOGGED_IN),
    reply_532(ReplyCode.REPLY_532_NEED_ACCOUNT_FOR_STORING_FILES), reply_533(
        ReplyCode.REPLY_533_COMMAND_PROTECTION_LEVEL_DENIED_FOR_POLICY_REASONS),
    reply_534(ReplyCode.REPLY_534_REQUEST_DENIED_FOR_POLICY_REASONS),
    reply_535(ReplyCode.REPLY_535_FAILED_SECURITY_CHECK),
    reply_536(ReplyCode.REPLY_536_REQUESTED_PROT_LEVEL_NOT_SUPPORTED),
    reply_550(ReplyCode.REPLY_550_REQUESTED_ACTION_NOT_TAKEN),
    reply_551(ReplyCode.REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN),
    reply_552(
        ReplyCode.REPLY_552_REQUESTED_FILE_ACTION_ABORTED_EXCEEDED_STORAGE),
    reply_553(
        ReplyCode.REPLY_553_REQUESTED_ACTION_NOT_TAKEN_FILE_NAME_NOT_ALLOWED);

    public final ReplyCode code;

    WaarpErrorValuesIndex(final ReplyCode code) {
      this.code = code;
    }

    public int getOID() {
      return ordinal() + 1;
    }
  }

  /**
   * Error part
   */
  public static final WaarpEntry[] WaarpErrorValues = {
      // reply_421,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_425,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_426,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_431,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_450,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_451,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_452,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_500,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_501,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_502,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_503,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_504,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_522,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_530,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_532,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_533,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_534,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_535,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_536,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_550,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_551,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_552,
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32,
                     MOAccessImpl.ACCESS_READ_ONLY),
      // reply_553
      new WaarpEntry(SMIConstants.SYNTAX_GAUGE32, MOAccessImpl.ACCESS_READ_ONLY)
  };

  /**
   * Oper Status (as defined in Net Application SNMP)
   */
  public enum OperStatus {
    up(1), down(2), halted(3), congested(4), restarting(5), quiescing(6);

    public final int status;

    OperStatus(final int status) {
      this.status = status;
    }
  }

}
