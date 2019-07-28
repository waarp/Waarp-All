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
package org.waarp.snmp;

import org.snmp4j.TransportMapping;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SNMPv2MIB;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB.SnmpCommunityEntryRow;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.MessageProcessingModel;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.Variable;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.TransportMappings;
import org.snmp4j.util.ThreadPool;
import org.snmp4j.util.WorkerPool;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.snmp.SnmpConfiguration.TargetElement;
import org.waarp.snmp.interf.WaarpInterfaceMib;
import org.waarp.snmp.interf.WaarpInterfaceMib.TrapLevel;
import org.waarp.snmp.interf.WaarpInterfaceMonitor;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * This Agent contains some functionalities for running a version 2c and 3 of
 * SNMP agent.
 */
public class WaarpSnmpAgent extends BaseAgent {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpSnmpAgent.class);

  private String[] address = { SnmpConfiguration.DEFAULTADDRESS };

  private int nbThread = 4;

  private final boolean isFilterAccessEnabled;

  private boolean useTrap = true;

  private int trapLevel;

  private final List<UsmUser> listUsmUser;

  private final List<TargetElement> listTargetElements;

  private final boolean hasV2;

  private final boolean hasV3;

  private final long systemTimeStart = System.currentTimeMillis();

  private final WorkerPool workerPool;
  /**
   * The associated monitor with this Agent
   */
  private WaarpInterfaceMonitor monitor;
  /**
   * The associated MIB with this Agent
   */
  private WaarpInterfaceMib mib;

  /**
   * @param configurationFile XML format
   * @param monitor the associated monitor
   * @param mib the associated MIB
   *
   * @throws IllegalArgumentException
   */
  public WaarpSnmpAgent(File configurationFile, WaarpInterfaceMonitor monitor,
                        WaarpInterfaceMib mib) throws IllegalArgumentException {
    /**
     * Creates a base agent with boot-counter, config file, and a CommandProcessor for processing SNMP requests.
     *
     * Parameters:
     *
     * These files does not exist and are not used but has to be specified. Read snmp4j docs for more info
     *
     * "bootCounterFile" - a file with serialized boot-counter information (read/write). If the file does not
     * exist it is created on shutdown of the agent.
     *
     * "configFile" - a file with serialized configuration information (read/write). If the file does not exist it
     * is created on shutdown of the agent.
     *
     * "commandProcessor" - the CommandProcessor instance that handles the SNMP requests.
     */
    super(new File(configurationFile.getParentFile(), "dummyConf.agent"),
          new File(configurationFile.getParentFile(), "dummyBootCounter.agent"),
          new CommandProcessor(new OctetString(MPv3.createLocalEngineID())));
    if (!SnmpConfiguration.setConfigurationFromXml(configurationFile)) {
      throw new IllegalArgumentException("Cannot load configuration");
    }
    address = SnmpConfiguration.address;
    nbThread = SnmpConfiguration.nbThread;
    isFilterAccessEnabled = SnmpConfiguration.isFilterAccessEnabled;
    useTrap = SnmpConfiguration.isUsingTrap;
    setTrapLevel(SnmpConfiguration.trapLevel);
    listUsmUser = SnmpConfiguration.listUsmUser;
    listTargetElements = SnmpConfiguration.listTargetElements;
    hasV2 = SnmpConfiguration.hasV2;
    hasV3 = SnmpConfiguration.hasV3;

    logger.debug("SNMP Configuration loaded: " + address[0] + ':' + nbThread);
    workerPool = ThreadPool.create("SnmpRequestPool", nbThread);
    agent.setWorkerPool(workerPool);
    setMonitor(monitor);
    getMonitor().setAgent(this);
    setMib(mib);
    getMib().setAgent(this);
  }

  /**
   * @return the monitor
   */
  public WaarpInterfaceMonitor getMonitor() {
    return monitor;
  }

  /**
   * @return the mib
   */
  public WaarpInterfaceMib getMib() {
    return mib;
  }

  /**
   * @return the uptime in ms
   */
  public long getUptime() {
    return getSnmpv2MIB().getUpTime().toMilliseconds();
  }

  /**
   * @return the uptime but in System time in ms
   */
  public long getUptimeSystemTime() {
    return systemTimeStart;
  }

  /**
   * Register additional managed objects at the agent's server.
   */
  @Override
  protected void registerManagedObjects() {
    logger.debug("Registers");
    try {
      getMib().registerMOs(server, null);
    } catch (final DuplicateRegistrationException e) {
      logger.error("Cannot register Mib", e);
    }
  }

  /**
   * Unregister the basic MIB modules from the agent's MOServer.
   */
  @Override
  protected void unregisterManagedObjects() {
    logger.debug("Unregisters");
    getMib().unregisterMOs(server, null);
  }

  /**
   * @param moGroup
   */
  public void unregisterManagedObject(MOGroup moGroup) {
    logger.debug("Unregister " + moGroup);
    moGroup.unregisterMOs(server, getContext(moGroup));
  }

  /**
   * Adds all the necessary initial users to the USM. Only applicable to SNMP
   * V3
   */
  @Override
  protected void addUsmUser(USM usm) {
    for (final UsmUser userlist : listUsmUser) {
      logger.debug("User: " + userlist);
      usm.addUser(userlist.getSecurityName(), usm.getLocalEngineID(), userlist);
    }
    final UsmUser usernotify =
        new UsmUser(new OctetString(SnmpConfiguration.V3NOTIFY), null, null,
                    null, null);
    usm.addUser(usernotify.getSecurityName(), null, usernotify);
  }

  /**
   * Adds initial notification targets and filters.
   */
  @Override
  protected void addNotificationTargets(SnmpTargetMIB targetMIB,
                                        SnmpNotificationMIB notificationMIB) {
    targetMIB.addDefaultTDomains();

    for (final TargetElement element : listTargetElements) {
      logger.debug("AddTarget: " + element);
      targetMIB.addTargetAddress(element.name, element.transportDomain,
                                 element.address, element.timeout,
                                 element.retries, element.tagList,
                                 element.params, element.storageType);
    }
    /**
     * Example
     *
     * targetMIB.addTargetAddress(new OctetString("notificationV2c"), TransportDomains.transportDomainUdpIpv4, new
     * OctetString(new UdpAddress("127.0.0.1/162").getValue()), 200, 1, new OctetString("notify"), new
     * OctetString("v2c"), StorageType.permanent); targetMIB.addTargetAddress(new OctetString("notificationV3"),
     * TransportDomains.transportDomainUdpIpv4, new OctetString(new UdpAddress("127.0.0.1/1162").getValue()), 200,
     * 1, new OctetString("notify"), new OctetString("v3notify"), StorageType.permanent);
     */
    logger.debug("HasV2: " + hasV2 + " HasV3: " + hasV3);
    if (hasV2) {
      targetMIB.addTargetParams(new OctetString(SnmpConfiguration.V2C),
                                MessageProcessingModel.MPv2c,
                                SecurityModel.SECURITY_MODEL_SNMPv2c,
                                new OctetString("cpublic"),
                                SecurityLevel.AUTH_PRIV, StorageType.permanent);
    }
    if (hasV3) {
      targetMIB.addTargetParams(new OctetString(SnmpConfiguration.V3NOTIFY),
                                MessageProcessingModel.MPv3,
                                SecurityModel.SECURITY_MODEL_USM,
                                new OctetString("v3notify"),
                                SecurityLevel.NOAUTH_NOPRIV,
                                StorageType.permanent);
    }
    int trapOrInform = SnmpNotificationMIB.SnmpNotifyTypeEnum.inform;
    if (useTrap) {
      trapOrInform = SnmpNotificationMIB.SnmpNotifyTypeEnum.trap;
    }
    notificationMIB.addNotifyEntry(new OctetString("default"),
                                   new OctetString(SnmpConfiguration.NOTIFY),
                                   trapOrInform, StorageType.permanent);
  }

  /**
   * Minimal View based Access Control
   * <p>
   * http://www.faqs.org/rfcs/rfc2575.html
   */
  @Override
  protected void addViews(VacmMIB vacm) {
    vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv1,
                  new OctetString("cpublic"), new OctetString("v1v2group"),
                  StorageType.nonVolatile);
    vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c,
                  new OctetString("cpublic"), new OctetString("v1v2group"),
                  StorageType.nonVolatile);
    vacm.addGroup(SecurityModel.SECURITY_MODEL_USM, new OctetString("v3notify"),
                  new OctetString("v3group"), StorageType.nonVolatile);

    for (final UsmUser user : listUsmUser) {
      logger.debug("Groups: " + user.getSecurityName() + " Restricted? " +
                   (user.getPrivacyProtocol() == null));
      if (user.getPrivacyProtocol() == null) {
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                      new OctetString(user.getSecurityName()),
                      new OctetString("v3restricted"), StorageType.nonVolatile);
      } else {
        vacm.addGroup(SecurityModel.SECURITY_MODEL_USM,
                      new OctetString(user.getSecurityName()),
                      new OctetString("v3group"), StorageType.nonVolatile);
      }
    }

    vacm.addAccess(new OctetString("v1v2group"), new OctetString("public"),
                   SecurityModel.SECURITY_MODEL_ANY,
                   SecurityLevel.NOAUTH_NOPRIV, MutableVACM.VACM_MATCH_EXACT,
                   new OctetString("fullReadView"),
                   new OctetString("fullWriteView"),
                   new OctetString("fullNotifyView"), StorageType.nonVolatile);
    vacm.addAccess(new OctetString("v3group"), new OctetString(),
                   SecurityModel.SECURITY_MODEL_USM, SecurityLevel.AUTH_PRIV,
                   MutableVACM.VACM_MATCH_EXACT,
                   new OctetString("fullReadView"),
                   new OctetString("fullWriteView"),
                   new OctetString("fullNotifyView"), StorageType.nonVolatile);
    vacm.addAccess(new OctetString("v3restricted"), new OctetString(),
                   SecurityModel.SECURITY_MODEL_USM,
                   SecurityLevel.NOAUTH_NOPRIV, MutableVACM.VACM_MATCH_EXACT,
                   new OctetString("restrictedReadView"),
                   new OctetString("restrictedWriteView"),
                   new OctetString("restrictedNotifyView"),
                   StorageType.nonVolatile);

    vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"),
                           new OctetString(), VacmMIB.vacmViewIncluded,
                           StorageType.nonVolatile);
    vacm.addViewTreeFamily(new OctetString("fullWriteView"), new OID("1.3"),
                           new OctetString(), VacmMIB.vacmViewIncluded,
                           StorageType.nonVolatile);
    vacm.addViewTreeFamily(new OctetString("fullNotifyView"), new OID("1.3"),
                           new OctetString(), VacmMIB.vacmViewIncluded,
                           StorageType.nonVolatile);

    vacm.addViewTreeFamily(new OctetString("restrictedReadView"),
                           new OID("1.3.6.1.2"), new OctetString(),
                           VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
    vacm.addViewTreeFamily(new OctetString("restrictedWriteView"),
                           new OID("1.3.6.1.2.1"), new OctetString(),
                           VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
    vacm.addViewTreeFamily(new OctetString("restrictedNotifyView"),
                           new OID("1.3.6.1.2"), new OctetString(),
                           VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
    vacm.addViewTreeFamily(new OctetString("restrictedNotifyView"),
                           new OID("1.3.6.1.6.3.1"), new OctetString(),
                           VacmMIB.vacmViewIncluded, StorageType.nonVolatile);
  }

  /**
   * The table of community strings configured in the SNMP engine's Local
   * Configuration Datastore (LCD).
   * <p>
   * We only configure one, "public".
   */
  @Override
  protected void addCommunities(SnmpCommunityMIB communityMIB) {
    final Variable[] com2sec = {
        new OctetString("public"), // community name
        new OctetString("cpublic"), // security name
        getAgent().getContextEngineID(), // local engine ID
        new OctetString("public"), // default context name
        new OctetString(), // transport tag
        new Integer32(StorageType.nonVolatile), // storage type
        new Integer32(RowStatus.active) // row status
    };
    final SnmpCommunityEntryRow row = communityMIB.getSnmpCommunityEntry()
                                                  .createRow(new OctetString(
                                                                 "public2public")
                                                                 .toSubIndex(
                                                                     true),
                                                             com2sec);
    communityMIB.getSnmpCommunityEntry().addRow(row);
    if (isFilterAccessEnabled) {
      snmpCommunityMIB.setSourceAddressFiltering(true);
    }
  }

  @Override
  protected void initTransportMappings() throws IOException {
    TransportMapping<?>[] testMappings = new TransportMapping[address.length];
    int nb = 0;
    for (final String addres : address) {
      final Address addr = GenericAddress.parse(addres);
      if (addr != null) {
        logger.info("SNMP Agent InitTransport: {} {}",
                    addr.getClass().getSimpleName(), addr);
        TransportMapping<?> tm;
        try {
          tm = TransportMappings.getInstance().createTransportMapping(addr);
        } catch (final RuntimeException e) {
          continue;
        }
        if (tm != null) {
          testMappings[nb] = tm;
          nb++;
        }
      }
    }
    if (nb > 0) {
      transportMappings = new TransportMapping<?>[nb];
      System.arraycopy(testMappings, 0, transportMappings, 0, nb);
    } else {
      transportMappings = null;
      throw new IOException("No address to connect");
    }
  }

  /**
   * Start method invokes some initialization methods needed to start the
   * agent
   *
   * @throws IOException
   */
  public void start() throws IOException {
    logger.debug(
        "WaarpSnmpAgent starting: " + address[0] + " 1 on " + address.length);
    try {
      init();
    } catch (final IOException e) {
      logger.warn("Error while SNMP starts ", e);
      throw e;
    }
    addShutdownHook();
    getServer().addContext(new OctetString("public"));
    finishInit();
    run();
    if (TrapLevel.StartStop.isLevelValid(getTrapLevel())) {
      sendColdStartNotification();
    }
  }

  @Override
  protected void sendColdStartNotification() {
    logger.debug("ColdStartNotification: {}",
                 getMib().getBaseOidStartOrShutdown());
    final SNMPv2MIB snmpv2 = getMib().getSNMPv2MIB();
    notificationOriginator
        .notify(new OctetString("public"), SnmpConstants.coldStart,
                new VariableBinding[] {
                    new VariableBinding(getMib().getBaseOidStartOrShutdown(),
                                        new OctetString("Startup Service")),
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
   * Send a Notification just before Shutdown the SNMP service.
   */
  protected void sendShutdownNotification() {
    if (getMib() == null || notificationOriginator == null) {
      return;
    }
    final SNMPv2MIB snmpv2 = getMib().getSNMPv2MIB();
    notificationOriginator
        .notify(new OctetString("public"), SnmpConstants.linkDown,
                new VariableBinding[] {
                    new VariableBinding(getMib().getBaseOidStartOrShutdown(),
                                        new OctetString("Shutdown Service")),
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
    try {
      Thread.sleep(100);
    } catch (final InterruptedException e) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    }
  }

  @Override
  public void stop() {
    logger.info("Stopping SNMP support");
    if (TrapLevel.StartStop.isLevelValid(getTrapLevel())) {
      sendShutdownNotification();
    }
    if (session != null) {
      super.stop();
    }
    if (getMonitor() != null) {
      getMonitor().releaseResources();
      try {
        Thread.sleep(100);
      } catch (final InterruptedException e) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      }
      if (workerPool != null) {
        workerPool.cancel();
      }
    }
  }

  /**
   * @return the trapLevel
   */
  public int getTrapLevel() {
    return trapLevel;
  }

  /**
   * @param trapLevel the trapLevel to set
   */
  public void setTrapLevel(int trapLevel) {
    this.trapLevel = trapLevel;
  }

  /**
   * @param monitor the monitor to set
   */
  private void setMonitor(WaarpInterfaceMonitor monitor) {
    this.monitor = monitor;
  }

  /**
   * @param mib the mib to set
   */
  private void setMib(WaarpInterfaceMib mib) {
    this.mib = mib;
  }
}
