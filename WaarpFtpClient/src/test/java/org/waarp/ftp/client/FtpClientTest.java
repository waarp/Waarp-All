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
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.ftp.client.transaction.Ftp4JClientTransactionTest;
import org.waarp.ftp.client.transaction.FtpClientThread;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static junit.framework.TestCase.*;

/**
 * Simple test example using predefined scenario (Note: this uses the configuration example for user shutdown
 * command)
 */
public class FtpClientTest {
  public static AtomicLong numberOK = new AtomicLong(0);
  public static AtomicLong numberKO = new AtomicLong(0);
  /**
   * Internal Logger
   */
  protected static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpClientTest.class);

  /**
   * @param args
   */
  public static void main(String[] args) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    System.setProperty("javax.net.debug", "false");

    String server = null;
    int port = 21;
    String username = null;
    String passwd = null;
    String account = null;
    String localFilename = null;
    int numberThread = 1;
    int numberIteration = 1;
    if (args.length < 8) {
      System.err.println("Usage: " + FtpClientTest.class.getSimpleName() +
                         " server port user pwd acct localfilename nbThread nbIter");
      DetectionUtils.systemExit(1);
      return;
    }
    server = args[0];
    port = Integer.parseInt(args[1]);
    username = args[2];
    passwd = args[3];
    account = args[4];
    localFilename = args[5];
    numberThread = Integer.parseInt(args[6]);
    numberIteration = Integer.parseInt(args[7]);
    int type = 0;
    if (args.length > 8) {
      type = Integer.parseInt(args[8]);
    } else {
      System.out.println("Both ways");
    }
    int delay = 0;
    if (args.length > 9) {
      delay = Integer.parseInt(args[9]);
    }
    int isSSL = 0;
    if (args.length > 10) {
      isSSL = Integer.parseInt(args[10]);
    }
    boolean shutdown = false;
    if (args.length > 11) {
      shutdown = Integer.parseInt(args[11]) > 0;
    }
    final FtpClientTest ftpClient = new FtpClientTest();
    ftpClient.testFtp4J(server, port, username, passwd, account, isSSL,
                        localFilename, type, delay, shutdown, numberThread,
                        numberIteration);
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
      FtpClientTest.numberKO.incrementAndGet();
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
      SysErrLogger.FAKE_LOGGER
          .sysout("SITE CRC: " + client.featureEnabled("SITE XCRC"));
      SysErrLogger.FAKE_LOGGER.sysout("CRC: " + client.featureEnabled("XCRC"));
      SysErrLogger.FAKE_LOGGER.sysout("MD5: " + client.featureEnabled("XMD5"));
      SysErrLogger.FAKE_LOGGER
          .sysout("SHA1: " + client.featureEnabled("XSHA1"));
      SysErrLogger.FAKE_LOGGER
          .sysout("DIGEST: " + client.featureEnabled("XDIGEST"));
    } finally {
      logger.warn("Logout");
      client.logout();
    }
    if (isSSL > 0) {
      try {
        Thread.sleep(100);
      } catch (final InterruptedException ignored) {//NOSONAR
      }
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
          final long newdel = ((delay / 3) / 10) * 10;
          if (newdel == 0) {
            Thread.yield();
          } else {
            Thread.sleep(newdel);
          }
        } catch (final InterruptedException ignored) {//NOSONAR
        }
      } else {
        Thread.yield();
      }
    }
    try {
      Thread.sleep(100);
    } catch (final InterruptedException e1) {//NOSONAR
      e1.printStackTrace();
      executorService.shutdownNow();
      // Thread.currentThread().interrupt();
    }
    executorService.shutdown();
    long date2 = 0;
    try {
      if (!executorService.awaitTermination(12000, TimeUnit.SECONDS)) {
        date2 = System.currentTimeMillis() - 120000 * 60;
        executorService.shutdownNow();
        if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
          System.err.println("Really not shutdown normally");
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
        " KO: " + numberKO.get() + " Trf/s: " +
        numberOK.get() * 1000 / (date2 - date1));
    assertEquals("No KO", 0, numberKO.get());
  }

}
