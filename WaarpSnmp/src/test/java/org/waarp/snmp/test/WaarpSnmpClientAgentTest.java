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

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.AuthSHA;
import org.snmp4j.security.PrivAES128;
import org.snmp4j.security.PrivAES192;
import org.snmp4j.security.PrivAES256;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.USM;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.OctetString;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.snmp.WaarpMOFactory;
import org.waarp.snmp.WaarpSnmpAgent;
import org.waarp.snmp.utils.WaarpMOScalar;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Test class for Agent and simple Client
 */
public class WaarpSnmpClientAgentTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  static WaarpSnmpAgent agent;

  static WaarpSimpleSnmpClient client;

  static WaarpImplPrivateMib test;

  @Test
  public void allTests() throws Exception {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    final ClassLoader classLoader =
        WaarpSnmpClientAgentTest.class.getClassLoader();
    final File file =
        new File(classLoader.getResource("snmpconfig.xml").getFile());
    if (!file.exists()) {
      System.err.println("Need SnmpConfig file");
      return;
    }
    setUp(file.getAbsolutePath());
    System.out.println("Test SysDescr");
    verifySysDescr();
    System.out.println("Test Table");
    verifyTableContents();
    Thread.sleep(1000);
    sendNotification();
    Thread.sleep(3000);
    final long uptime = agent.getUptime();
    final long systemUptime = agent.getUptimeSystemTime();
    assertTrue(uptime < systemUptime);
    addUser();
    System.out.println("Stopping");
    tearDown();
  }

  public static void setUp(String file) throws Exception {
    // Setup the client to use our newly started agent
    client = new WaarpSimpleSnmpClient("udp:127.0.0.1/2001", 1162);
    // Create a monitor
    final WaarpPrivateMonitor monitor = new WaarpPrivateMonitor();
    // Create a Mib
    test = new WaarpImplPrivateMib("Waarp Test SNMP", 6666, 66666, 66,
                                   "F. Bregier", "Waarp Test SNMP V1.0",
                                   "Paris, France", 72);
    // Set the default VariableFactory
    WaarpMOFactory.setFactory(new WaarpTestVariableFactory());
    // Create the agent associated with the monitor and Mib
    agent = new WaarpSnmpAgent(new File(file), monitor, test);
    agent.start();
  }

  /**
   * Simply verifies that we get the same sysDescr as we have registered in
   * our agent
   */
  public static void verifySysDescr() throws IOException {
    assertEquals(test.textualSysDecr,
                 client.getAsString(SnmpConstants.sysDescr));
  }

  /**
   * Verify that the table contents is ok.
   */
  public static void verifyTableContents() {
    for (final WaarpMOScalar scalar : test.rowInfo.getRow()) {
      try {
        System.out.println("Read " + scalar.getID() + ':' +
                           client.getAsString(scalar.getID()));
      } catch (final IOException e) {
        System.err.println(scalar.getID() + ":" + e.getMessage());
        continue;
      }
    }
  }

  public static void sendNotification() {
    test.notifyInfo("Une alerte", "un autre texte d'alerte", 1971);
    test.notifyError("Une seconde alerte", "un second texte d'alerte", 20);
  }

  public void addUser() {
    final USM usm = new USM();
    UsmUser user = new UsmUser(new OctetString("TEST"), AuthSHA.ID,
                               new OctetString("maplesyrup"), PrivDES.ID,
                               new OctetString("maplesyrup"));
    usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
    user = new UsmUser(new OctetString("SHA"), AuthSHA.ID,
                       new OctetString("SHAAuthPassword"), null, null);
    usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
    user = new UsmUser(new OctetString("SHADES"), AuthSHA.ID,
                       new OctetString("SHADESAuthPassword"), PrivDES.ID,
                       new OctetString("SHADESPrivPassword"));
    usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
    user = new UsmUser(new OctetString("MD5DES"), AuthMD5.ID,
                       new OctetString("MD5DESAuthPassword"), PrivDES.ID,
                       new OctetString("MD5DESPrivPassword"));
    usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
    user = new UsmUser(new OctetString("SHAAES128"), AuthSHA.ID,
                       new OctetString("SHAAES128AuthPassword"), PrivAES128.ID,
                       new OctetString("SHAAES128PrivPassword"));
    usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
    user = new UsmUser(new OctetString("SHAAES192"), AuthSHA.ID,
                       new OctetString("SHAAES192AuthPassword"), PrivAES192.ID,
                       new OctetString("SHAAES192PrivPassword"));
    usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
    user = new UsmUser(new OctetString("SHAAES256"), AuthSHA.ID,
                       new OctetString("SHAAES256AuthPassword"), PrivAES256.ID,
                       new OctetString("SHAAES256PrivPassword"));
    usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);

    user = new UsmUser(new OctetString("MD5AES128"), AuthMD5.ID,
                       new OctetString("MD5AES128AuthPassword"), PrivAES128.ID,
                       new OctetString("MD5AES128PrivPassword"));
    usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
    user = new UsmUser(new OctetString("MD5AES192"), AuthMD5.ID,
                       new OctetString("MD5AES192AuthPassword"), PrivAES192.ID,
                       new OctetString("MD5AES192PrivPassword"));
    usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);
    user = new UsmUser(new OctetString("MD5AES256"), AuthMD5.ID,
                       new OctetString("MD5AES256AuthPassword"), PrivAES256.ID,
                       new OctetString("MD5AES256PrivPassword"));
    usm.addUser(user.getSecurityName(), usm.getLocalEngineID(), user);

  }

  public static void tearDown() throws Exception {
    agent.stop();
    client.stop();
  }

  private static boolean assertEquals(String a, String b) {
    if (!a.equals(b)) {
      System.err.println("Not Equals! " + a + " and " + b);
      fail("Should be equals");
      return false;
    }
    System.out.println("Equal: " + a);
    return true;
  }

  static class StringResponseListener implements ResponseListener {

    private String value;

    @Override
    public void onResponse(ResponseEvent event) {
      System.out.println(event.getResponse());
      if (event.getResponse() != null) {
        value = WaarpSimpleSnmpClient.extractSingleString(event);
      }
    }

    public String getValue() {
      System.out.println(value);
      return value;
    }

  }
}
