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
import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
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
import org.waarp.common.utility.DetectionUtils;
import org.waarp.ftp.FtpServer;
import org.waarp.ftp.client.transaction.Ftp4JClientTransactionTest;
import org.waarp.ftp.client.transaction.FtpClientThread;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
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
  /**
   * Internal Logger
   */
  protected static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AbstractFtpClient.class);
  private static final int port = 2021;

  public static void startServer0() throws IOException {
    WaarpLoggerFactory
        .setDefaultFactory(new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    DetectionUtils.setJunit(true);
    final File file = new File("/tmp/GGFTP/fred/a");
    file.mkdirs();
    FtpServer.startFtpServer("config.xml", "src/test/resources/sslconfig.xml",
                             SSL_MODE != 0, SSL_MODE < 0);
    final File localFilename = new File("/tmp/ftpfile.bin");
    final FileWriter fileWriterBig = new FileWriter(localFilename);
    for (int i = 0; i < 100; i++) {
      fileWriterBig.write("0123456789");
    }
    fileWriterBig.flush();
    fileWriterBig.close();
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
  public void clean() {
    File file = new File("/tmp/GGFTP");
    FileUtils.forceDeleteRecursiveDir(file);
    file = new File("/tmp/GGFTP/fredo/a");
    file.mkdirs();
  }

  @Test
  public void test1_Ftp4JSimple() throws IOException {
    numberKO.set(0);
    numberOK.set(0);
    final File localFilename = new File("/tmp/ftpfile.bin");
    testFtp4J("127.0.0.1", port, "fred", "fred2", "a", SSL_MODE,
              localFilename.getAbsolutePath(), 0, 50, true, 1, 1);
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
      Assert.assertTrue("No KO", numberKO.get() == 0);
      return;
    }
    try {
      logger.warn("Create Dirs");
      for (int i = 0; i < numberThread; i++) {
        client.makeDir("T" + i);
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
    if (isSSL != 0) {
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
    assertTrue("No KO", numberKO.get() == 0);
  }

  public static void launchFtpClient(String server, int port, String username,
                                     String password, String account,
                                     boolean localActive, String local,
                                     String remote) {
    final boolean mustCallProtP = SSL_MODE > 0;
    final boolean binaryTransfer = true;
    boolean error = false;
    final boolean useEpsvWithIPv4 = false;
    final boolean lenient = false;
    final FTPClient ftp;
    ftp = new FTPClient();

    ftp.setCopyStreamListener(createListener());
    ftp.setControlKeepAliveTimeout(30000);
    ftp.setControlKeepAliveReplyTimeout(30000);
    ftp.setListHiddenFiles(true);
    // suppress login details
    ftp.addProtocolCommandListener(
        new PrintCommandListener(new PrintWriter(System.out), true));

    try {
      int reply;
      if (port > 0) {
        ftp.connect(server, port);
      } else {
        ftp.connect(server);
      }
      System.out.println("Connected to " + server + " on " +
                         (port > 0? port : ftp.getDefaultPort()));

      // After connection attempt, you should check the reply code to verify
      // success.
      reply = ftp.getReplyCode();

      if (!FTPReply.isPositiveCompletion(reply)) {
        ftp.disconnect();
        System.err.println("FTP server refused connection.");
        fail("Can't connect");
        return;
      }
    } catch (final IOException e) {
      if (ftp.getDataConnectionMode() ==
          FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE) {
        try {
          ftp.disconnect();
        } catch (final IOException f) {
          // do nothing
        }
      }
      System.err.println("Could not connect to server.");
      e.printStackTrace();
      fail("Can't connect");
      return;
    }

    __main:
    try {
      if (account == null) {
        if (!ftp.login(username, password)) {
          ftp.logout();
          error = true;
          break __main;
        }
      } else {
        if (!ftp.login(username, password, account)) {
          ftp.logout();
          error = true;
          break __main;
        }
      }
      System.out.println("Remote system is " + ftp.getSystemType());

      if (binaryTransfer) {
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
      }

      // Use passive mode as default because most of us are
      // behind firewalls these days.
      if (localActive) {
        ftp.enterLocalActiveMode();
      } else {
        ftp.enterLocalPassiveMode();
      }

      ftp.setUseEPSVwithIPv4(useEpsvWithIPv4);

      if (mustCallProtP) {
        ((FTPSClient) ftp).execPBSZ(0);
        ((FTPSClient) ftp).execPROT("P");
      }

      InputStream input;

      input = new FileInputStream(local);

      if (!ftp.storeFile(remote, input)) {
        error = true;
        input.close();
        return;
      }
      input.close();

      OutputStream output;

      output = new FileOutputStream(local);

      if (!ftp.retrieveFile(remote, output)) {
        error = true;
        output.close();
        return;
      }
      output.close();
      if (lenient) {
        final FTPClientConfig config = new FTPClientConfig();
        config.setLenientFutureDates(true);
        ftp.configure(config);
      }

      for (final FTPFile f : ftp.listFiles(remote)) {
        System.out.println(f.getRawListing());
        System.out.println(f.toFormattedString());
      }
      for (final FTPFile f : ftp.mlistDir(remote)) {
        System.out.println(f.getRawListing());
        System.out.println(f.toFormattedString());
      }
      final FTPFile f = ftp.mlistFile(remote);
      if (f != null) {
        System.out.println(f.toFormattedString());
      }
      final String[] results = ftp.listNames(remote);
      if (results != null) {
        for (final String s : ftp.listNames(remote)) {
          System.out.println(s);
        }
      }
      if (!ftp.deleteFile(remote)) {
        error = true;
        System.out.println("Failed delete");
      }
      // boolean feature check
      if (ftp.features()) {
        // Command listener has already printed the output
      } else {
        System.out.println("Failed: " + ftp.getReplyString());
        error = true;
      }

      ftp.noop(); // check that control connection is working OK
    } catch (final FTPConnectionClosedException e) {
      error = true;
      System.err.println("Server closed connection.");
      e.printStackTrace();
    } catch (final IOException e) {
      error = true;
      e.printStackTrace();
    } finally {
      if (ftp.getDataConnectionMode() ==
          FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE) {
        try {
          ftp.disconnect();
        } catch (final IOException f) {
          // do nothing
        }
      }
      assertFalse("Error occurs", error);
    }
  }

  private static CopyStreamListener createListener() {
    return new CopyStreamListener() {
      private long megsTotal;

      @Override
      public void bytesTransferred(CopyStreamEvent event) {
        bytesTransferred(event.getTotalBytesTransferred(),
                         event.getBytesTransferred(), event.getStreamSize());
      }

      @Override
      public void bytesTransferred(long totalBytesTransferred,
                                   int bytesTransferred, long streamSize) {
        final long megs = totalBytesTransferred / 1000000;
        for (long l = megsTotal; l < megs; l++) {
          System.err.print("#");
        }
        megsTotal = megs;
      }
    };
  }
}
