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

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.FileTestUtils;
import org.waarp.ftp.client.transaction.FtpApacheClientTransactionTest;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class AbstractFtpClientWithApache extends AbstractFtpClient {
  /**
   * Internal Logger
   */
  protected static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AbstractFtpClientWithApache.class);
  private static final int port = 2021;

  @Test
  public void test0_FtpApacheClientActive() throws IOException {
    final File localFilename = new File("/tmp/ftpfile.bin");
    FileTestUtils.createTestFile(localFilename, 100);
    logger.warn("Active");
    launchFtpClient("127.0.0.1", port, "fredo", "fred1", "a", true,
                    localFilename.getAbsolutePath(), localFilename.getName());
    logger.warn("End Active");
    try {
      Thread.sleep(100);
    } catch (final InterruptedException e) {//NOSONAR
      // ignore
    }
  }

  @Test
  public void test0_FtpApacheClientPassive() throws IOException {
    final File localFilename = new File("/tmp/ftpfile.bin");
    FileTestUtils.createTestFile(localFilename, 100);
    logger.warn("Passive");
    launchFtpClient("127.0.0.1", port, "fredo", "fred1", "a", false,
                    localFilename.getAbsolutePath(), localFilename.getName());
    logger.warn("End Passive");
    try {
      Thread.sleep(100);
    } catch (final InterruptedException e) {//NOSONAR
      // ignore
    }
  }

  private void internalApacheClient(FtpApacheClientTransactionTest client,
                                    File localFilename, int delay,
                                    boolean mode) {
    final String smode = mode? "passive" : "active";
    logger.info(" transfer {} store ", smode);
    if (!client.transferFile(localFilename.getAbsolutePath(),
                             localFilename.getName(), true)) {
      logger.error("Cant store file {} mode ", smode);
      FtpClientTest.numberKO.incrementAndGet();
      return;
    } else {
      FtpClientTest.numberOK.incrementAndGet();
    }
    if (!client.deleteFile(localFilename.getName())) {
      logger.error(" Cant delete file {} mode ", smode);
      FtpClientTest.numberKO.incrementAndGet();
      return;
    } else {
      FtpClientTest.numberOK.incrementAndGet();
    }
    if (!client.transferFile(localFilename.getAbsolutePath(),
                             localFilename.getName(), true)) {
      logger.error("Cant store file {} mode ", smode);
      FtpClientTest.numberKO.incrementAndGet();
      return;
    } else {
      FtpClientTest.numberOK.incrementAndGet();
    }
    Thread.yield();
    logger.info(" transfer {} retr ", smode);
    if (!client.retrieve((String) null, localFilename.getName())) {
      logger.error("Cant retrieve file {} mode ", smode);
      FtpClientTest.numberKO.incrementAndGet();
    } else {
      FtpClientTest.numberOK.incrementAndGet();
    }
  }

  @Test
  public void test2_FtpSimple() throws IOException {
    numberKO.set(0);
    numberOK.set(0);
    final File localFilename = new File("/tmp/ftpfile.bin");

    final int delay = 50;

    final FtpApacheClientTransactionTest client =
        new FtpApacheClientTransactionTest("127.0.0.1", port, "fred", "fred2",
                                           "a", SSL_MODE);
    if (!client.connect()) {
      logger.error("Can't connect");
      FtpClientTest.numberKO.incrementAndGet();
      assertEquals("No KO", 0, numberKO.get());
      return;
    }
    try {
      logger.warn("Create Dirs");
      client.makeDir("T" + 0);
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        // Ignore
      }
      logger.warn("Feature commands");
      System.out.println("SITE: " + client.featureEnabled("SITE"));
      System.out.println("SITE CRC: " + client.featureEnabled("SITE XCRC"));
      System.out.println("CRC: " + client.featureEnabled("XCRC"));
      System.out.println("MD5: " + client.featureEnabled("XMD5"));
      System.out.println("SHA1: " + client.featureEnabled("XSHA1"));
      System.out.println("DIGEST: " + client.featureEnabled("XDIGEST"));
      client.changeDir("T0");
      client.changeFileType(true);
      client.changeMode(true);
      internalApacheClient(client, localFilename, delay, true);
      client.changeMode(false);
      internalApacheClient(client, localFilename, delay, false);
    } finally {
      logger.warn("Logout");
      client.logout();
      client.disconnect();
    }
    assertEquals("No KO", 0, numberKO.get());
  }
}
