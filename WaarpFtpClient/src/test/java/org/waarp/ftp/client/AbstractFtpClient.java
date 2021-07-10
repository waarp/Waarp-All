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
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.FileTestUtils;
import org.waarp.common.utility.Version;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.ftp.FtpServer;
import org.waarp.ftp.client.transaction.Ftp4JClientTransactionTest;
import org.waarp.ftp.client.transaction.FtpClientThread;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class AbstractFtpClient {
  public static AtomicLong numberOK = new AtomicLong(0);
  public static AtomicLong numberKO = new AtomicLong(0);
  static int SSL_MODE = 0; //-1 native, 1 auth
  protected static int DELAY = 10;
  /**
   * Internal Logger
   */
  protected static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AbstractFtpClient.class);
  private static final int port = 2021;

  public static void startServer0() throws IOException {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    WaarpSystemUtil.setJunit(true);
    final File file = new File("/tmp/GGFTP/fred/a");
    file.mkdirs();
    FtpServer.startFtpServer("config.xml", "src/test/resources/sslconfig.xml",
                             SSL_MODE != 0, SSL_MODE < 0);
    final File localFilename = new File("/tmp/ftpfile.bin");
    FileTestUtils.createTestFile(localFilename, 100);
    logger.warn("Will start server");
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
  public static void stopServer() {
    logger.warn("Will shutdown from client");
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    try {
      Thread.sleep(200);
    } catch (final InterruptedException ignored) {//NOSONAR
    }
    final Ftp4JClientTransactionTest client =
        new Ftp4JClientTransactionTest("127.0.0.1", port, "fredo", "fred1", "a",
                                       SSL_MODE);
    if (!client.connect()) {
      logger.warn("Cant connect");
    } else {
      try {
        final String[] results =
            client.executeSiteCommand("internalshutdown abcdef");
        System.out.print("SHUTDOWN: ");
        for (final String string : results) {
          System.out.println(string);
        }
      } finally {
        client.disconnect();
      }
    }
    logger.warn("Will stop server");
    WaarpSystemUtil.stopLogger(true);
    FtpServer.stopFtpServer();
    final File file = new File("/tmp/GGFTP");
    FileUtils.forceDeleteRecursiveDir(file);

    try {
      Thread.sleep(1000);
    } catch (final InterruptedException ignored) {//NOSONAR
    }
  }

  public static boolean versionJavaCompatible() {
    boolean valid = Version.artifactId().contains("-jre") || SSL_MODE != 1;
    logger.warn("Java {} SSL_MODE {} = Valid {}", Version.version(), SSL_MODE,
                valid);
    return valid;
  }

  @Before
  public void clean() throws InterruptedException {
    File file = new File("/tmp/GGFTP");
    FileUtils.forceDeleteRecursiveDir(file);
    file = new File("/tmp/GGFTP/fredo/a");
    file.mkdirs();
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
  public void test1_Ftp4JSimple() throws IOException {
    numberKO.set(0);
    numberOK.set(0);
    final File localFilename = new File("/tmp/ftpfile.bin");
    testFtp4J("127.0.0.1", port, "fred", "fred2", "a", SSL_MODE,
              localFilename.getAbsolutePath(), 0, DELAY, true, 1, 1);
  }

  public void testFtp4J(String server, int port, String username, String passwd,
                        String account, int isSSL, String localFilename,
                        int type, int delay, boolean shutdown, int numberThread,
                        int numberIteration) {
    // initiate Directories
    final Ftp4JClientTransactionTest client =
        new Ftp4JClientTransactionTest(server, port, username, passwd, account,
                                       isSSL);

    logger.warn("First connexion");
    if (!client.connect()) {
      logger.error("Can't connect");
      if (AbstractFtpClient.versionJavaCompatible()) {
        numberKO.incrementAndGet();
      }
      Assert.assertEquals("No KO", 0, numberKO.get());
      return;
    }
    try {
      logger.warn("Create Dirs");
      for (int i = 0; i < numberThread; i++) {
        System.out.print('.');
        client.makeDir("T" + i);
      }
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        // Ignore
      }
      System.out.println();
      logger.warn("Feature commands");
      System.out.println("SITE: " + client.featureEnabled("SITE"));
      System.out.println("SITE CRC: " + client.featureEnabled("SITE XCRC"));
      System.out.println("CRC: " + client.featureEnabled("XCRC"));
      System.out.println("MD5: " + client.featureEnabled("XMD5"));
      System.out.println("SHA1: " + client.featureEnabled("XSHA1"));
      System.out.println("DIGEST: " + client.featureEnabled("XDIGEST"));
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
    } catch (final InterruptedException e1) {//NOSONAR
      e1.printStackTrace();
    }
    executorService.shutdown();
    long date2 = 0;
    try {
      if (!executorService.awaitTermination(12000, TimeUnit.SECONDS)) {
        date2 = System.currentTimeMillis() - 120000 * 60;
        executorService.shutdownNow();
        if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
          logger.error("Really not shutdown normally");
        }
      } else {
        date2 = System.currentTimeMillis();
      }
    } catch (final InterruptedException e) {//NOSONAR
      e.printStackTrace();
      executorService.shutdownNow();
      date2 = System.currentTimeMillis();
      // Thread.currentThread().interrupt();
    }

    logger.warn(
        localFilename + ' ' + numberThread + ' ' + numberIteration + ' ' +
        type + " Real: " + (date2 - date1) + " OK: " + numberOK.get() +
        " KO: " + FtpClientTest.numberOK.get() + " Trf/s: " +
        FtpClientTest.numberOK.get() * 1000 / (date2 - date1));
    assertEquals("No KO", 0, FtpClientTest.numberKO.get());
  }

  public static void launchFtpClient(String server, int port, String username,
                                     String password, String account,
                                     boolean localActive, String local,
                                     String remote) {
    final WaarpFtpClient waarpFtpClient =
        new WaarpFtpClient(server, port, username, password, account,
                           !localActive, SSL_MODE, 30000, 30000, true);
    boolean error = false;

    boolean connected = waarpFtpClient.connect();
    // After connection attempt, you should check the reply code to verify
    // success.
    if (!connected) {
      logger.error("FTP server refused connection.");
      fail("Can't connect");
      return;
    }

    try {
      if (!waarpFtpClient.store(local, remote)) {
        error = true;
        return;
      }
      if (!waarpFtpClient.retrieve(local, remote)) {
        error = true;
        return;
      }

      for (final String f : waarpFtpClient.listFiles(remote)) {
        System.out.println(f);
      }
      for (final String f : waarpFtpClient.mlistFiles(remote)) {
        System.out.println(f);
      }
      if (!waarpFtpClient.deleteFile(remote)) {
        error = true;
        logger.error("Failed delete");
      }
      // boolean feature check
      String[] features = waarpFtpClient.features();
      if (features != null) {
        // Command listener has already printed the output
      } else {
        logger.error("Failed: " + waarpFtpClient.getResult());
        error = true;
      }

      waarpFtpClient.noop(); // check that control connection is working OK
    } finally {
      waarpFtpClient.disconnect();
      assertFalse("Error occurs", error);
    }
  }

}
