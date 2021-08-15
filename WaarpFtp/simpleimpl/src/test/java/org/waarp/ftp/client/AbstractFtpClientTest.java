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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.waarp.common.logging.SysErrLogger;
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

import static junit.framework.TestCase.*;

/**
 * Simple test example using predefined scenario (Note: this uses the configuration example for user shutdown
 * command)
 */
public abstract class AbstractFtpClientTest {
  public static AtomicLong numberOK = new AtomicLong(0);
  public static AtomicLong numberKO = new AtomicLong(0);
  static int SSL_MODE = 0; //-1 native, 1 auth
  static int DELAY = 10;
  /**
   * Internal Logger
   */
  protected static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AbstractFtpClientTest.class);

  public void testFtp4J(String server, int port, String username, String passwd,
                        String account, int isSSL, String localFilename,
                        int type, int delay, int numberThread,
                        int numberIteration) {
    // initiate Directories
    final Ftp4JClientTransactionTest client =
        new Ftp4JClientTransactionTest(server, port, username, passwd, account,
                                       isSSL);

    logger.warn("First connexion");
    if (!client.connect()) {
      logger.error("Can't connect");
      if (versionJavaCompatible()) {
        numberKO.incrementAndGet();
      }
      assertEquals("No KO", 0, numberKO.get());
      return;
    }
    try {
      logger.warn("Create Dirs");
      for (int i = 0; i < numberThread; i++) {
        client.makeDir("T" + i);
      }
      logger.warn("Feature commands");
      SysErrLogger.FAKE_LOGGER.sysout("SITE: " + client.featureEnabled("SITE"));
      SysErrLogger.FAKE_LOGGER.sysout(
          "SITE CRC: " + client.featureEnabled("SITE XCRC"));
      SysErrLogger.FAKE_LOGGER.sysout("CRC: " + client.featureEnabled("XCRC"));
      SysErrLogger.FAKE_LOGGER.sysout("MD5: " + client.featureEnabled("XMD5"));
      SysErrLogger.FAKE_LOGGER.sysout(
          "DIGEST: " + client.featureEnabled("XDIGEST"));
      SysErrLogger.FAKE_LOGGER.sysout(
          "SHA1: " + client.featureEnabled("XSHA1"));
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
      if (delay > 0) {
        try {
          final long newdel = delay;
          if (newdel == 0) {
            Thread.sleep(2);
          } else {
            Thread.sleep(newdel);
          }
        } catch (final InterruptedException ignored) {//NOSONAR
          break;
        }
      } else {
        Thread.yield();
      }
    }
    try {
      Thread.sleep(100);
    } catch (final InterruptedException e1) {//NOSONAR
      SysErrLogger.FAKE_LOGGER.syserr(e1);
    }
    executorService.shutdown();
    long date2 = 0;
    try {
      if (!executorService.awaitTermination(120, TimeUnit.SECONDS)) {
        date2 = System.currentTimeMillis() - 120000;
        executorService.shutdownNow();
        if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
          SysErrLogger.FAKE_LOGGER.syserr("Really not shutdown normally");
        }
      } else {
        date2 = System.currentTimeMillis();
      }
    } catch (final InterruptedException e) {//NOSONAR
      SysErrLogger.FAKE_LOGGER.syserr(e);
      executorService.shutdownNow();
      date2 = System.currentTimeMillis();
      // Thread.currentThread().interrupt();
    }

    logger.warn(
        localFilename + ' ' + numberThread + ' ' + numberIteration + ' ' +
        type + " Real: " + (date2 - date1) + " OK: " + numberOK.get() +
        " KO: " + numberKO.get() + " Trf/s: " +
        numberOK.get() * 1000 / (date2 - date1));
    assertEquals("No KO", 0, numberKO.get());
  }

  public static void startServer0() throws IOException {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);

    FtpServer.startFtpServer("config.xml", "src/test/resources/sslconfig.xml",
                             SSL_MODE != 0, SSL_MODE < 0);
    final File localFilename = new File("/tmp/ftpfile.bin");
    FileTestUtils.createTestFile(localFilename, 1000);
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
        new Ftp4JClientTransactionTest("127.0.0.1", 2021, "fredo", "fred1", "a",
                                       SSL_MODE);
    if (!client.connect()) {
      logger.warn("Cant connect");
    } else {
      try {
        final String[] results =
            client.executeSiteCommand("internalshutdown abcdef");
        SysErrLogger.FAKE_LOGGER.syserrNoLn("SHUTDOWN: ");
        for (final String string : results) {
          SysErrLogger.FAKE_LOGGER.syserr(string);
        }
      } finally {
        client.disconnect();
      }
    }
    logger.warn("Will stop server");
    WaarpSystemUtil.stopLogger(true);
    FtpServer.stopFtpServer();
    try {
      Thread.sleep(1000);
    } catch (final InterruptedException ignored) {//NOSONAR
    }
  }

  @After
  public void after() {
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

  public static boolean versionJavaCompatible() {
    boolean valid = Version.artifactId().contains("-jre") || SSL_MODE != 1;
    logger.warn("Java {} SSL_MODE {} = Valid {}", Version.version(), SSL_MODE,
                valid);
    return valid;
  }

  @Test
  public void testFtp4JSimple() throws IOException {
    numberKO.set(0);
    numberOK.set(0);
    final File localFilename = new File("/tmp/ftpfile.bin");
    testFtp4J("127.0.0.1", 2021, "fred", "fred2", "a", SSL_MODE,
              localFilename.getAbsolutePath(), 0, DELAY, 1, 10);
  }

}
