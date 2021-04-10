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

package org.waarp.ftp.client;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runners.MethodSorters;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.common.utility.FileTestUtils;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.ftp.FtpServer;
import org.waarp.ftp.client.transaction.Ftp4JClientTransactionTest;
import org.waarp.ftp.client.transaction.FtpApacheClientTransactionTest;

import java.io.File;
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

  /**
   * Internal Logger
   */
  protected static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpClientPerfTestIT.class);


  public static void startServer0() throws IOException {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    DetectionUtils.setJunit(true);
    final File file = new File("/tmp/GGFTP/fred/a");
    file.mkdirs();
    FtpServer.startFtpServer("config.xml", "src/test/resources/sslconfig.xml",
                             SSL_MODE != 0, SSL_MODE < 0);
    final File localFilename = new File("/tmp/ftpfile.bin");
    FileTestUtils.createTestFile(localFilename, 100);
    logger.warn("Will start server");
  }

  @AfterClass
  public static void stopServer() {
    logger.warn("Will shutdown from client");
    try {
      Thread.sleep(200);
    } catch (final InterruptedException ignored) {//NOSONAR
    }
    final Ftp4JClientTransactionTest client =
        new Ftp4JClientTransactionTest("127.0.0.1", port, "fredo", "fred1", "a",
                                       SSL_MODE);
    if (!client.connect()) {
      logger.warn("Cant connect");
      FtpClientTest.numberKO.incrementAndGet();
      return;
    }
    try {
      final String[] results =
          client.executeSiteCommand("internalshutdown abcdef");
      System.err.print("SHUTDOWN: ");
      for (final String string : results) {
        System.err.println(string);
      }
    } finally {
      client.disconnect();
    }
    logger.warn("Will stop server");
    FtpServer.stopFtpServer();
    final File file = new File("/tmp/GGFTP");
    FileUtils.forceDeleteRecursiveDir(file);

    try {
      Thread.sleep(1000);
    } catch (final InterruptedException ignored) {//NOSONAR
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
  public static void startServer() throws IOException {
    SSL_MODE = 0;
    startServer0();
  }


  @Test
  public void test1_FtpApache() throws IOException, InterruptedException {
    numberKO.set(0);
    numberOK.set(0);
    final File localFilename = new File("/tmp/ftpfile.bin");

    final int delay = 2;

    final FtpApacheClientTransactionTest client =
        new FtpApacheClientTransactionTest("127.0.0.1", port, "fred", "fred2",
                                           "a", SSL_MODE, false);
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
        internalApacheClient(client, localFilename, delay, true);
        Thread.sleep(delay);
      }
      t2 = System.currentTimeMillis();
      c2 = numberOK.get();
      client.changeMode(false);
      for (int i = 0; i < NUMBER; i++) {
        internalApacheClient(client, localFilename, delay, false);
        Thread.sleep(delay);
      }
      t3 = System.currentTimeMillis();
      c3 = numberOK.get();
    } finally {
      logger.warn("Logout");
      client.logout();
      client.disconnect();
    }
    long stop = System.currentTimeMillis();
    logger.warn("Do Transfer {} at {}/s {} passive {}/s active/s",
                numberOK.get(),
                numberOK.get() * 1000.0 / (stop - start),
                (c2-c1)*1000.0/(t2-t1), (c3-c2)*1000.0/(t3-t2));
    assertEquals("No KO", 0, numberKO.get());
  }

  private void internalApacheClient(FtpApacheClientTransactionTest client,
                                    File localFilename, int delay,
                                    boolean mode) {
    final String smode = mode? "passive" : "active";
    logger.info(" transfer {} store ", smode);
    if (!client.simpleTransferFile(localFilename.getAbsolutePath(),
                                   localFilename.getName(), true)) {
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
      FtpClientTest.numberOK.incrementAndGet();
    }
    if (!client.simpleTransferFile(localFilename.getAbsolutePath(),
                                   localFilename.getName(), true)) {
      logger.warn("Cant store file {} mode ", smode);
      numberKO.incrementAndGet();
      return;
    } else {
      numberOK.incrementAndGet();
    }
    Thread.yield();
    logger.info(" transfer {} retr ", smode);
    if (!client.simpleTransferFile(null, localFilename.getName(), false)) {
      logger.warn("Cant retrieve file {} mode ", smode);
      numberKO.incrementAndGet();
    } else {
      numberOK.incrementAndGet();
    }
  }

  @Test
  public void test2_Ftp4JSimple() throws IOException {
    numberKO.set(0);
    numberOK.set(0);
    final File localFilename = new File("/tmp/ftpfile.bin");
    testFtp4J("127.0.0.1", port, "fred", "fred2", "a", SSL_MODE,
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
    logger.warn("Do Transfer {} at {}/s {} passive {}/s active/s",
                numberOK.get(),
                numberOK.get() * 1000.0 / (stop - start),
                (c2-c1)*1000.0/(t2-t1), (c3-c2)*1000.0/(t3-t2));
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
