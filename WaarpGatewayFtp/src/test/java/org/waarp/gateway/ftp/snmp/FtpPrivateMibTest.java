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

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
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
import org.waarp.common.command.ReplyCode;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.DbConstant;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.file.FileUtils;
import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.gateway.ftp.ExecGatewayFtpServer;
import org.waarp.gateway.ftp.ServerInitDatabase;
import org.waarp.gateway.ftp.client.FtpClientTest;
import org.waarp.gateway.ftp.client.transaction.Ftp4JClientTransactionTest;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.gateway.ftp.control.ExecBusinessHandler;
import org.waarp.gateway.ftp.data.FileSystemBasedDataBusinessHandler;
import org.waarp.gateway.ftp.database.DbConstantFtp;
import org.waarp.gateway.ftp.database.data.DbTransferLog;
import org.waarp.snmp.WaarpMOFactory;
import org.waarp.snmp.WaarpSnmpAgent;
import org.waarp.snmp.utils.WaarpMOScalar;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import static org.junit.Assert.*;

public class FtpPrivateMibTest {
  protected static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpPrivateMibTest.class);

  static WaarpSnmpAgent agent;

  static WaarpSimpleSnmpClient client;

  static FtpPrivateMib test;
  static String key;

  @BeforeClass
  public static void startServer() throws Exception {
    WaarpLoggerFactory
        .setDefaultFactory(new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    DetectionUtils.setJunit(true);
    // R66 Home
    File home = new File("/tmp/FTP");
    home.mkdirs();
    FileUtils.forceDeleteRecursiveDir(home);
    final ClassLoader classLoader = FtpClientTest.class.getClassLoader();
    File file = new File(classLoader.getResource("Gg-FTP.xml").getFile());
    final File tmp = new File("/tmp");
    final File[] files = tmp.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return file.getName().startsWith("ggftp");
      }
    });
    for (final File fileDb : files) {
      fileDb.delete();
    }
    final FileBasedConfiguration configuration =
        new FileBasedConfiguration(ExecGatewayFtpServer.class,
                                   ExecBusinessHandler.class,
                                   FileSystemBasedDataBusinessHandler.class,
                                   new FilesystemBasedFileParameterImpl());
    try {
      if (!configuration
          .setConfigurationServerFromXml(file.getAbsolutePath())) {
        logger.error("Bad main configuration");
        fail("Bad main configuration");
      }
      // Init database
      try {
        ServerInitDatabase.initdb();
      } catch (final WaarpDatabaseNoConnectionException e) {
        SysErrLogger.FAKE_LOGGER.syserr("Cannot connect to database");
        return;
      }
      System.out.println("End creation");
    } finally {
      if (DbConstantFtp.gatewayAdmin != null) {
        DbConstantFtp.gatewayAdmin.close();
      }
    }
    logger.warn("Will start server");
    key = configuration.getCryptoKey().decryptHexInString("c5f4876737cf351a");

    ExecGatewayFtpServer.main(new String[] { file.getAbsolutePath() });
  }

  @AfterClass
  public static void stopServer() throws InterruptedException {
    logger.warn("Will shutdown from client");
    try {
      Thread.sleep(500);
    } catch (final InterruptedException ignored) {
    }
    try {
      final Ftp4JClientTransactionTest client =
          new Ftp4JClientTransactionTest("127.0.0.1", 2021, "fredo", key, "a",
                                         0);
      if (!client.connect()) {
        logger.warn("Cant connect");
        return;
      }
      try {
        final String[] results =
            client.executeSiteCommand("internalshutdown pwdhttp");
        System.err.print("SHUTDOWN: ");
        for (final String string : results) {
          System.err.println(string);
        }
      } finally {
        client.disconnect();
      }
    } finally {
      logger.warn("Will stop server");
      FileBasedConfiguration.fileBasedConfiguration.setShutdown(true);
      FileBasedConfiguration.fileBasedConfiguration.releaseResources();
      try {
        Thread.sleep(1000);
      } catch (final InterruptedException ignored) {
      }
      File home = new File("/tmp/FTP");
      FileUtils.forceDeleteRecursiveDir(home);
    }
  }

  @Test
  public void allTests() throws Exception {
    WaarpLoggerFactory
        .setDefaultFactory(new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    final ClassLoader classLoader = FtpPrivateMibTest.class.getClassLoader();
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
    DbConstant.admin = new DbAdmin();
    FileBasedConfiguration.fileBasedConfiguration
        .setMonitoring(new FtpMonitoring(null));
    // Create a Mib
    test = new FtpPrivateMib(
        FileBasedConfiguration.fileBasedConfiguration.getServerPort());
    // Set the default VariableFactory
    WaarpMOFactory.setFactory(new FtpVariableFactory());
    // Create the agent associated with the monitor and Mib
    agent = new WaarpSnmpAgent(new File(file),
                               FileBasedConfiguration.fileBasedConfiguration
                                   .getMonitoring(), test);
    FileBasedConfiguration.fileBasedConfiguration.setAgentSnmp(agent);
    agent.start();
  }

  /**
   * Simply verifies that we get the same sysDescr as we have registered in
   * our agent
   */
  public static void verifySysDescr() throws IOException {
    assertEquals(FtpPrivateMib.SnmpName,
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

  public static void sendNotification() throws WaarpDatabaseException {
    DbTransferLog log =
        new DbTransferLog(DbConstantFtp.gatewayAdmin.getSession(), "user",
                          "account", 1, true, "filename", "mode",
                          ReplyCode.REPLY_200_COMMAND_OKAY, "info",
                          UpdatedInfo.DONE);

    test.notifyError("Message1", "Message2");
    test.notifyInfoTask("Message3", log);
    test.notifyOverloaded("Message4", "Message5");
    test.notifyStartStop("Message6", "Message7");
    test.notifyWarning("Message9", "Message10");
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