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

package org.waarp.ftp.client.transaction;

import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.ftp.client.AbstractFtpClientTest;
import org.waarp.ftp.client.FtpClientTest;

import java.io.File;

import static junit.framework.TestCase.*;

/**
 * FTP Thread used to check multiple FTP clients in parallel with the test scenario
 */
public class FtpClientThread implements Runnable {
  /**
   * Internal Logger
   */
  protected static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpClientThread.class);

  private final String id;

  private final String server;

  private int port = 21;

  private final String username;

  private final String passwd;

  private final String account;

  private final String localFilename;

  private final String remoteFilename;

  private int numberIteration = 1;

  private final int type;

  private final int delay;

  private final int isSsl;

  /**
   * @param id
   * @param server
   * @param port
   * @param username
   * @param passwd
   * @param account
   * @param localFilename
   * @param nb
   * @param type
   * @param delay
   */
  public FtpClientThread(String id, String server, int port, String username,
                         String passwd, String account, String localFilename,
                         int nb, int type, int delay, int isSsl) {
    this.id = id;
    this.server = server;
    this.port = port;
    this.username = username;
    this.passwd = passwd;
    this.account = account;
    this.localFilename = localFilename;
    final File local = new File(this.localFilename);
    remoteFilename = local.getName();
    numberIteration = nb;
    this.type = type;
    this.delay = delay;
    this.isSsl = isSsl;
    final File dir = new File("/tmp/GGFTP/T" + id + '/' + account);
    dir.mkdirs();
  }

  @Override
  public void run() {
    final Ftp4JClientTransactionTest client =
        new Ftp4JClientTransactionTest(server, port, username, passwd, account,
                                       isSsl);
    Thread.yield();
    // System.err.println(id+" connect");
    if (!client.connect()) {
      logger.error(id + " Cant connect");
      if (AbstractFtpClientTest.versionJavaCompatible()) {
        FtpClientTest.numberKO.incrementAndGet();
      }
      return;
    }
    try {
      if (numberIteration <= 0) {
        FtpClientTest.numberOK.incrementAndGet();
        return;// Only connect
      }
      // client.makeDir(this.id);
      logger.info(id + " change dir");
      client.changeDir(id);
      Thread.yield();
      String[] results = client.executeCommand("XMKD newtest");
      for (final String string : results) {
        logger.warn("XMKD: {}", string);
      }
      client.makeDir("newtest");
      client.changeDir("newtest");
      results = client.executeCommand("CDUP");
      for (final String string : results) {
        logger.warn("CDUP: {}", string);
      }
      results = client.executeCommand("STAT");
      for (final String string : results) {
        logger.warn("STAT: {}", string);
      }
      results = client.executeCommand("HELP");
      for (final String string : results) {
        logger.warn("HELP: {}", string);
      }
      results = client.executeCommand("OPTS MLST");
      for (final String string : results) {
        logger.warn("OPTS: {}", string);
      }
      results = client.executeCommand("MDTM newtest");
      for (final String string : results) {
        logger.warn("MDTP: {}", string);
      }
      results = client.executeCommand("XCWD newtest");
      for (final String string : results) {
        logger.warn("XCWD: {}", string);
      }
      results = client.executeCommand("XPWD");
      for (final String string : results) {
        logger.warn("XPWD: {}", string);
      }
      results = client.executeCommand("XCUP");
      for (final String string : results) {
        logger.warn("XCUP: {}", string);
      }
      results = client.executeCommand("XRMD newtest");
      for (final String string : results) {
        logger.warn("XRMD: {}", string);
      }
      assertNull(client.executeCommand("UnknownCommand 42"));
      assertNull(client.executeCommand("SMNT UnimplementedCommand"));
      assertNull(client.executeCommand("SITE RNTO IncorrectCommand"));
      results = client.executeCommand("MODE S");
      for (final String string : results) {
        logger.warn("MODE: {}", string);
      }
      results = client.executeCommand("STRU F");
      for (final String string : results) {
        logger.warn("STRU: {}", string);
      }
      results = client.executeCommand("ALLO 100");
      for (final String string : results) {
        logger.warn("ALLO: {}", string);
      }

      if (isSsl == 0) {
        assertNull(client.executeCommand("CCC"));
      }

      logger.info(id + " change type");
      client.changeFileType(true);
      if (type <= 0) {
        logger.warn(id + " change mode passive");
        client.changeMode(true);
        if (type <= -10) {
          for (int i = 0; i < numberIteration; i++) {
            logger.info(id + " transfer passive store " + i);
            if (!client.transferFile(localFilename, remoteFilename, true)) {
              logger.warn("Cant store file passive mode " + id);
              FtpClientTest.numberKO.incrementAndGet();
              return;
            } else {
              System.out.print('.');
              FtpClientTest.numberOK.incrementAndGet();
            }
            try {
              Thread.sleep(delay);
            } catch (InterruptedException e) {
              // Ignore
            }
          }
          System.out.println();
        } else {
          for (int i = 0; i < numberIteration; i++) {
            logger.info(id + " transfer passive store " + i);
            if (!client.transferFile(localFilename, remoteFilename, true)) {
              logger.warn("Cant store file passive mode " + id);
              FtpClientTest.numberKO.incrementAndGet();
              return;
            } else {
              System.out.print('.');
              FtpClientTest.numberOK.incrementAndGet();
            }
            if (!client.deleteFile(remoteFilename)) {
              logger.warn(" Cant delete file passive mode " + id);
              FtpClientTest.numberKO.incrementAndGet();
              return;
            } else {
              FtpClientTest.numberOK.incrementAndGet();
            }
            try {
              Thread.sleep(delay);
            } catch (InterruptedException e) {
              // Ignore
            }
          }
          System.out.println();
          if (!client.transferFile(localFilename, remoteFilename, true)) {
            logger.warn("Cant store file passive mode " + id);
            FtpClientTest.numberKO.incrementAndGet();
            return;
          } else {
            FtpClientTest.numberOK.incrementAndGet();
          }
          Thread.yield();
          for (int i = 0; i < numberIteration; i++) {
            logger.info(id + " transfer passive retr " + i);
            if (!client.transferFile(null, remoteFilename, false)) {
              logger.warn("Cant retrieve file passive mode " + id);
              FtpClientTest.numberKO.incrementAndGet();
              return;
            } else {
              System.out.print('.');
              FtpClientTest.numberOK.incrementAndGet();
            }
            try {
              Thread.sleep(delay);
            } catch (InterruptedException e) {
              // Ignore
            }
          }
          System.out.println();
        }
        Thread.yield();
      }
      if (type >= 0) {
        logger.warn(id + " change mode active");
        client.changeMode(false);
        if (type >= 10) {
          for (int i = 0; i < numberIteration; i++) {
            logger.info(id + " transfer active store " + i);
            if (!client.transferFile(localFilename, remoteFilename, true)) {
              logger.warn("Cant store file active mode " + id);
              FtpClientTest.numberKO.incrementAndGet();
              return;
            } else {
              System.out.print('.');
              FtpClientTest.numberOK.incrementAndGet();
            }
            try {
              Thread.sleep(delay);
            } catch (InterruptedException e) {
              // Ignore
            }
          }
          System.out.println();
        } else {
          for (int i = 0; i < numberIteration; i++) {
            logger.info(id + " transfer active store " + i);
            if (!client.transferFile(localFilename, remoteFilename, true)) {
              logger.warn("Cant store file active mode " + id);
              FtpClientTest.numberKO.incrementAndGet();
              return;
            } else {
              System.out.print('.');
              FtpClientTest.numberOK.incrementAndGet();
            }
            if (!client.deleteFile(remoteFilename)) {
              logger.warn("Cant delete file active mode " + id);
              FtpClientTest.numberKO.incrementAndGet();
              return;
            } else {
              FtpClientTest.numberOK.incrementAndGet();
            }
            try {
              Thread.sleep(delay);
            } catch (InterruptedException e) {
              // Ignore
            }
          }
          System.out.println();
          if (!client.transferFile(localFilename, remoteFilename, true)) {
            logger.warn("Cant store file active mode " + id);
            FtpClientTest.numberKO.incrementAndGet();
            return;
          } else {
            FtpClientTest.numberOK.incrementAndGet();
          }
          Thread.yield();
          for (int i = 0; i < numberIteration; i++) {
            logger.info(id + " transfer active retr " + i);
            if (!client.transferFile(null, remoteFilename, false)) {
              logger.warn("Cant retrieve file active mode " + id);
              FtpClientTest.numberKO.incrementAndGet();
              return;
            } else {
              System.out.print('.');
              FtpClientTest.numberOK.incrementAndGet();
            }
            try {
              Thread.sleep(delay);
            } catch (InterruptedException e) {
              // Ignore
            }
          }
          System.out.println();
        }
      }
      results = client.executeSiteCommand("XCRC " + remoteFilename);
      for (final String string : results) {
        logger.warn("XCRC: {}", string);
      }
      results = client.executeSiteCommand("XMD5 " + remoteFilename);
      for (final String string : results) {
        logger.warn("XMD5: {}", string);
      }
      results = client.executeSiteCommand("XSHA1 " + remoteFilename);
      for (final String string : results) {
        logger.warn("XSHA1: {}", string);
      }
      for (DigestAlgo algo : DigestAlgo.values()) {
        results = client.executeSiteCommand(
            "XDIGEST " + algo.algoName + " " + remoteFilename);
        for (final String string : results) {
          logger.warn("XDIGEST {}: {}", algo.algoName, string);
        }
        results = client.executeSiteCommand(
            "XDIGEST " + algo.name() + " " + remoteFilename);
        for (final String string : results) {
          logger.warn("XDIGEST {}: {}", algo.name(), string);
        }
      }
      results = client.executeCommand("SIZE " + remoteFilename);
      for (final String string : results) {
        logger.warn("SIZE: {}", string);
      }
      client.executeCommand("RNFR " + remoteFilename);
      client.executeCommand("RNTO " + remoteFilename + "2");
      results = client.listFiles();
      for (final String string : results) {
        logger.warn("LIST: {}", string);
      }
      client.executeCommand("REIN");
    } finally {
      logger.warn(id + " disconnect {}:{}", FtpClientTest.numberOK.get(),
                  FtpClientTest.numberKO.get());
      client.logout();
      logger.info(id + " end disconnect");
    }
  }

}
