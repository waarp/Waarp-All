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

package org.waarp.gateway.ftp.client;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.waarp.common.database.DbRequest;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.database.model.DbModelMysql;
import org.waarp.common.file.FileUtils;
import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.FileTestUtils;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.TestWebAbstract;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.gateway.ftp.ExecGatewayFtpServer;
import org.waarp.gateway.ftp.ServerInitDatabase;
import org.waarp.gateway.ftp.client.transaction.Ftp4JClientTransactionTest;
import org.waarp.gateway.ftp.client.transaction.FtpClientThread;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.gateway.ftp.control.ExecBusinessHandler;
import org.waarp.gateway.ftp.data.FileSystemBasedDataBusinessHandler;
import org.waarp.gateway.ftp.database.DbConstantFtp;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.fail;

/**
 * Simple test example using predefined scenario (Note: this uses the configuration example for user shutdown
 * command)
 */
public class FtpClientPostgreTest extends TestWebAbstract {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();
  protected static final Map<String, String> TMPFSMAP =
      new HashMap<String, String>();

  static {
    TMPFSMAP.clear();
    TMPFSMAP.put("/tmp/postgresql/data", "rw");
  }

  @ClassRule
  public static PostgreSQLContainer db =
      (PostgreSQLContainer) new PostgreSQLContainer().withCommand(
          "postgres -c fsync=false -c synchronous_commit=off -c " +
          "full_page_writes=false -c wal_level=minimal -c " +
          "max_wal_senders=0 -c max_connections=1000 ").withTmpFs(TMPFSMAP);

  static FtpClientPostgreTest scenario = new FtpClientPostgreTest();

  public JdbcDatabaseContainer getJDC() {
    return db;
  }

  /**
   * If defined using -DIT_LONG_TEST=true then will execute long term tests
   */
  public static final String IT_LONG_TEST = "IT_LONG_TEST";
  static String key;
  private static int DELAY = 10;

  /**
   * Internal Logger
   */
  protected static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpClientPostgreTest.class);


  public void testFtp4J(String server, int port, String username, String passwd,
                        String account, int isSSL, String localFilename,
                        int type, int delay, boolean shutdown, int numberThread,
                        int numberIteration) {
    // initiate Directories
    final Ftp4JClientTransactionTest client =
        new Ftp4JClientTransactionTest(server, port, username, passwd, account,
                                       isSSL);

    client.setReportActiveExternalIPAddress("127.0.0.1");
    client.setActiveDataTransferPortRange(57000, 63000);
    logger.warn("First connexion");
    if (!client.connect()) {
      logger.error("Can't connect");
      FtpClientTest.numberKO.incrementAndGet();
      assertEquals("No KO", 0, FtpClientTest.numberKO.get());
      return;
    }
    try {
      logger.warn("Create Dirs");
      for (int i = 0; i < numberThread; i++) {
        client.makeDir("T" + i);
      }
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        // Ignore
      }
      logger.warn("Feature commands");
      System.err.println("SITE: " + client.featureEnabled("SITE"));
      System.err.println("SITE CRC: " + client.featureEnabled("SITE XCRC"));
      System.err.println("CRC: " + client.featureEnabled("XCRC"));
      System.err.println("MD5: " + client.featureEnabled("XMD5"));
      System.err.println("SHA1: " + client.featureEnabled("XSHA1"));
      System.err.println("DIGEST: " + client.featureEnabled("XDIGEST"));
    } finally {
      logger.warn("Logout");
      client.logout();
    }
    final ExecutorService executorService = Executors.newCachedThreadPool();
    logger.warn("Will start {} Threads", numberThread);
    final long date1 = System.currentTimeMillis();
    for (int i = 0; i < numberThread; i++) {
      executorService.execute(
          new FtpClientThread("T" + i, server, port, username, passwd, account,
                              localFilename, numberIteration, type, delay,
                              isSSL));
      Thread.yield();
    }
    try {
      Thread.sleep(100);
    } catch (final InterruptedException e1) {
      e1.printStackTrace();
    }
    executorService.shutdown();
    long date2 = 0;
    try {
      if (!executorService.awaitTermination(120000, TimeUnit.MILLISECONDS)) {
        executorService.shutdownNow();
        if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
          System.err.println("Really not shutdown normally");
        }
      }
    } catch (final InterruptedException e) {
      e.printStackTrace();
      executorService.shutdownNow();
    }
    date2 = System.currentTimeMillis();

    logger.warn(
        localFilename + ' ' + numberThread + ' ' + numberIteration + ' ' +
        type + " Real: " + (date2 - date1) + " OK: " +
        FtpClientTest.numberOK.get() + " KO: " + FtpClientTest.numberKO.get() +
        " Trf/s: " + FtpClientTest.numberOK.get() * 1000 / (date2 - date1));
    assertEquals("No KO", 0, FtpClientTest.numberKO.get());
  }

  @BeforeClass
  public static void startServer() throws Exception {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(
        SystemPropertyUtil.get(IT_LONG_TEST, false)? Level.SIMPLE :
            Level.PARANOID);
    WaarpSystemUtil.setJunit(true);
    // R66 Home
    File home = new File("/tmp/FTP");
    home.mkdirs();
    FileUtils.forceDeleteRecursiveDir(home);
    final File localFilename = new File("/tmp/ftpfile.bin");
    FileTestUtils.createTestFile(localFilename, 100);
    final ClassLoader classLoader = FtpClientPostgreTest.class.getClassLoader();
    // Adapt config file to Database
    File file =
        new File(classLoader.getResource("Gg-FTP-postgre.xml").getFile());
    if (!file.exists()) {
      SysErrLogger.FAKE_LOGGER.syserr(
          "Cannot find in  " + file.getAbsolutePath());
      fail("Cannot find " + file.getAbsolutePath());
    }
    String content = WaarpStringUtils.readFile(file.getAbsolutePath());
    SysErrLogger.FAKE_LOGGER.sysout(scenario.getJDC().getJdbcUrl());
    String driver = scenario.getJDC().getDriverClassName();
    String target = "notfound";
    String jdbcUrl = scenario.getJDC().getJdbcUrl();
    if (driver.equalsIgnoreCase("org.mariadb.jdbc.Driver")) {
      target = "mariadb";
    } else if (driver.equalsIgnoreCase("org.h2.Driver")) {
      target = "h2";
    } else if (driver.equalsIgnoreCase("oracle.jdbc.OracleDriver")) {
      target = "oracle";
      jdbcUrl = "jdbc:oracle:thin:@//localhost:1521/test";
      SysErrLogger.FAKE_LOGGER.syserr(
          jdbcUrl + " while should be something like " + jdbcUrl);
      throw new UnsupportedOperationException(
          "Unsupported Test for Oracle since wrong JDBC driver");
    } else if (driver.equalsIgnoreCase("org.postgresql.Driver")) {
      target = "postgresql";
    } else if (DbModelMysql.MYSQL_DRIVER_JRE6.equalsIgnoreCase(driver) ||
               DbModelMysql.MYSQL_DRIVER_JRE8.equalsIgnoreCase(driver)) {
      target = "mysql";
    } else {
      SysErrLogger.FAKE_LOGGER.syserr("Cannot find driver for " + driver);
    }
    content = content.replace("XXXJDBCXXX", jdbcUrl);
    content = content.replace("XXXDRIVERXXX", target);
    SysErrLogger.FAKE_LOGGER.sysout(scenario.getJDC().getDriverClassName());
    SysErrLogger.FAKE_LOGGER.sysout(target);
    File fileTo = new File("/tmp/gg-ftp-db.xml");
    fileTo.getParentFile().mkdirs();
    FileWriter writer = null;
    try {
      writer = new FileWriter(fileTo);
      writer.write(content);
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (writer != null) {
        FileUtils.close(writer);
      }
    }
    if (file.exists()) {
      initiateWebDriver(file.getParentFile());
    }
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
      if (!configuration.setConfigurationServerFromXml(
          fileTo.getAbsolutePath())) {
        System.err.println("Bad main configuration");
        Assert.fail("Bad main configuration");
      }
      // Init database
      try {
        ServerInitDatabase.initdb();
      } catch (final WaarpDatabaseNoConnectionException e) {
        logger.error("Cannot connect to database");
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

    ExecGatewayFtpServer.main(new String[] { fileTo.getAbsolutePath() });
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      // Wait for server started
    }
    System.gc();
    try {
      Thread.sleep(500);
    } catch (InterruptedException e) {
      // Wait for server started
    }
  }

  @AfterClass
  public static void stopServer() throws InterruptedException {
    logger.warn("Will shutdown from client");
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    try {
      Thread.sleep(200);
    } catch (final InterruptedException ignored) {
    }
    finalizeDriver();
    try {
      final Ftp4JClientTransactionTest client =
          new Ftp4JClientTransactionTest("127.0.0.1", 2021, "fredo", key, "a",
                                         0);
      if (!client.connect()) {
        logger.warn("Cant connect");
      } else {
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
      }
    } finally {
      logger.warn("Will stop server");
      WaarpSystemUtil.stopLogger(true);
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

  @Before
  public void before() {
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      // Wait for server started
    }
    System.gc();
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      // Wait for server started
    }
  }

  @Test
  public void testFtp4JSimple()
      throws InterruptedException, WaarpDatabaseNoConnectionException,
             WaarpDatabaseSqlException, SQLException {
    FtpClientTest.numberKO.set(0);
    FtpClientTest.numberOK.set(0);

    DbRequest request = new DbRequest(DbConstantFtp.gatewayAdmin.getSession());
    request.select("SELECT current_setting('max_connections')");
    request.getNext();
    logger.warn("MaxConnection: {}", request.getResultSet().getString(1));
    request.close();

    final File localFilename = new File("/tmp/ftpfile.bin");
    final int nbThread = SystemPropertyUtil.get(IT_LONG_TEST, false)? 10 : 1;
    final int nbPerThread = SystemPropertyUtil.get(IT_LONG_TEST, false)? 10 : 1;
    final int delay = SystemPropertyUtil.get(IT_LONG_TEST, false)? 0 : DELAY;
    testFtp4J("127.0.0.1", 2021, "fred", key, "a", 0,
              localFilename.getAbsolutePath(), 0, delay, true, nbThread,
              nbPerThread);
  }

  @Test
  public void testAdminWeb() throws InterruptedException {
    try {
      // Test name: GWFTP_Admin
      // Step # | name | target | value | comment
      // 1 | open | / |  |
      driver.get("https://127.0.0.1:8067/");
      // 4 | type | name=passwd | pwdhttp |
      driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 5 | type | name=name | monadmin |
      driver.findElement(By.name("name")).sendKeys("monadmin");
      // 6 | click | name=Logon |  |
      driver.findElement(By.name("Logon")).click();
      // 7 | click | id=mvg1_1_img |  |
      driver.get("https://127.0.0.1:8067/User.html");
      // 8 | click | id=mvg1_2_img |  |
      driver.get("https://127.0.0.1:8067/Rule.html");
      // 9 | click | id=mvg1_3_img |  |
      driver.get("https://127.0.0.1:8067/Transfer.html");
      // 10 | click | id=mvg1_4_img |  |
      driver.get("https://127.0.0.1:8067/System.html");
      // 11 | click | id=mvg1_5_img |  |
      driver.get("https://127.0.0.1:8067/Logon.html");
    } catch (NoSuchElementException e) {
      e.printStackTrace();
      reloadDriver();
      fail(e.getMessage());
    } catch (StaleElementReferenceException e) {
      e.printStackTrace();
      reloadDriver();
      fail(e.getMessage());
    } finally {
    }
  }
}
