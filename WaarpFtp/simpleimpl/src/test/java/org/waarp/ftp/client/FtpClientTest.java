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
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.WaarpSystemUtil;

import java.io.IOException;

/**
 * Simple test example using predefined scenario (Note: this uses the configuration example for user shutdown
 * command)
 */
public class FtpClientTest extends AbstractFtpClientTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

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
      SysErrLogger.FAKE_LOGGER.syserr(
          "Usage: " + FtpClientTest.class.getSimpleName() +
          " server port user pwd acct localfilename nbThread nbIter");
      WaarpSystemUtil.systemExit(1);
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
                        localFilename, type, delay, numberThread,
                        numberIteration);
  }

  @BeforeClass
  public static void startServer() throws IOException {
    ResourceLeakDetector.setLevel(Level.PARANOID);
    SSL_MODE = 0;
    DELAY = 5;
    startServer0();
  }

}
