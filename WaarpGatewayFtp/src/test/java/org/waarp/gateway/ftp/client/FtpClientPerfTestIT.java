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
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runners.MethodSorters;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.file.FileUtils;
import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.common.utility.FileTestUtils;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.gateway.ftp.ExecGatewayFtpServer;
import org.waarp.gateway.ftp.ServerInitDatabase;
import org.waarp.gateway.ftp.client.transaction.Ftp4JClientTransactionTest;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.gateway.ftp.control.ExecBusinessHandler;
import org.waarp.gateway.ftp.data.FileSystemBasedDataBusinessHandler;
import org.waarp.gateway.ftp.database.DbConstantFtp;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FtpClientPerfTestIT {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static int NUMBER = 200;
  private static final int port = 2021;
  public static AtomicLong numberOK = new AtomicLong(0);
  public static AtomicLong numberKO = new AtomicLong(0);
  static int SSL_MODE = 0; //-1 native, 1 auth
  static String key;

  /**
   * Internal Logger
   */
  protected static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpClientPerfTestIT.class);


  public static void startServer0() throws Exception {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    DetectionUtils.setJunit(true);
    // R66 Home
    File home = new File("/tmp/FTP");
    home.mkdirs();
    FileUtils.forceDeleteRecursiveDir(home);
    final File localFilename = new File("/tmp/ftpfile.bin");
    FileTestUtils.createTestFile(localFilename, 100);
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

    ExecGatewayFtpServer.main(new String[] { file.getAbsolutePath() });
  }

  @AfterClass
  public static void stopServer() {
    logger.warn("Will shutdown from client");
    try {
      Thread.sleep(200);
    } catch (final InterruptedException ignored) {
    }
    try {
      final Ftp4JClientTransactionTest client =
          new Ftp4JClientTransactionTest("127.0.0.1", 2021, "fredo", key, "a",
                                         0);
      if (!client.connect()) {
        logger.warn("Cant connect");
        numberKO.incrementAndGet();
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

  @Before
  public void clean() throws InterruptedException {
    File file = new File("/tmp/GGFTP");
    FileUtils.forceDeleteRecursiveDir(file);
    file = new File("/tmp/GGFTP/fredo/a");
    file.mkdirs();
    Thread.sleep(5000);
  }

  @BeforeClass
  public static void startServer() throws Exception {
    SSL_MODE = 0;
    startServer0();
  }

  @Test
  public void test2_Ftp4JSimple() throws IOException {
    numberKO.set(0);
    numberOK.set(0);
    final File localFilename = new File("/tmp/ftpfile.bin");
    testFtp4J("127.0.0.1", port, "fredo", key, "a", SSL_MODE,
              localFilename.getAbsolutePath(), 0, 2, true, 1, 1);
  }

  public void testFtp4J(String server, int port, String username, String passwd,
                        String account, int isSSL, String localFilename,
                        int type, int delay, boolean shutdown, int numberThread,
                        int numberIteration) {
    // initiate Directories
    final File localFile = new File(localFilename);
    final Ftp4JClientTransactionTest client =
        new Ftp4JClientTransactionTest(server, port, username, passwd, account,
                                       isSSL);
    long start = System.currentTimeMillis();
    if (!client.connect()) {
      logger.error("Can't connect");
      numberKO.incrementAndGet();
      assertEquals("No KO", 0, numberKO.get());
      return;
    }
    long t1, t2, t3, c1, c2, c3;
    try {
      logger.warn("Create Dirs");
      client.makeDir("T" + 0);
      client.changeDir("T0");
      client.changeFileType(true);
      client.changeMode(true);
      t1 = System.currentTimeMillis();
      c1 = numberOK.get();
      for (int i = 0; i < NUMBER; i++) {
        internalFtp4JClient(client, localFile, delay, true);
      }
      client.changeMode(false);
      t2 = System.currentTimeMillis();
      c2 = numberOK.get();
      for (int i = 0; i < NUMBER; i++) {
        internalFtp4JClient(client, localFile, delay, false);
      }
      t3 = System.currentTimeMillis();
      c3 = numberOK.get();
    } finally {
      logger.warn("Logout");
      client.logout();
      client.disconnect();
    }
    long stop = System.currentTimeMillis();
    logger
        .warn("Do Transfer {} at {}/s {} passive {}/s active/s", numberOK.get(),
              numberOK.get() * 1000.0 / (stop - start),
              (c2 - c1) * 1000.0 / (t2 - t1), (c3 - c2) * 1000.0 / (t3 - t2));
    assertEquals("No KO", 0, numberKO.get());
  }

  private void internalFtp4JClient(Ftp4JClientTransactionTest client,
                                   File localFilename, int delay,
                                   boolean mode) {
    final String smode = mode? "passive" : "active";
    logger.info(" transfer {} store ", smode);
    if (!client
        .transferFile(localFilename.getAbsolutePath(), localFilename.getName(),
                      true)) {
      logger.warn("Cant store file {} mode ", smode);
      numberKO.incrementAndGet();
      return;
    } else {
      numberOK.incrementAndGet();
    }
    if (!client.deleteFile(localFilename.getName())) {
      logger.warn(" Cant delete file {} mode ", smode);
      numberKO.incrementAndGet();
      return;
    } else {
      numberOK.incrementAndGet();
    }
    if (!client
        .transferFile(localFilename.getAbsolutePath(), localFilename.getName(),
                      true)) {
      logger.warn("Cant store file {} mode ", smode);
      numberKO.incrementAndGet();
      return;
    } else {
      numberOK.incrementAndGet();
    }
    Thread.yield();
    logger.info(" transfer {} retr ", smode);
    if (!client.transferFile(null, localFilename.getName(), false)) {
      logger.warn("Cant retrieve file {} mode ", smode);
      numberKO.incrementAndGet();
    } else {
      numberOK.incrementAndGet();
    }
  }
}
