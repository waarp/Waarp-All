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

package org.waarp.openr66.protocol.it;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.Processes;
import org.waarp.openr66.client.SubmitTransfer;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.junit.NetworkClientTest;
import org.waarp.openr66.protocol.junit.TestAbstract;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.json.ShutdownOrBlockJsonPacket;
import org.waarp.openr66.protocol.utils.R66Future;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ScenarioIT extends TestAbstract {
  private static final ArrayList<DbTaskRunner> dbTaskRunners =
      new ArrayList<DbTaskRunner>();
  private static final String CONFIG_SERVER_A_MINIMAL_XML =
      "config-serverA-minimal.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML =
      "Linux/config/config-serverInitA.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_B_XML =
      "Linux/config/config-serverInitB.xml";
  private static final String CONFIG_CLIENT_A_XML = "config-clientA.xml";

  /**
   * @throws Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    final ClassLoader classLoader = NetworkClientTest.class.getClassLoader();
    final File file =
        new File(classLoader.getResource("logback-test.xml").getFile());
    setUpBeforeClassMinimal(LINUX_CONFIG_CONFIG_SERVER_INIT_B_XML);
    setUpDbBeforeClass();
    // setUpBeforeClassServer("Linux/config/config-serverInitB.xml", "config-serverB.xml", false);
    setUpBeforeClassServer(LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML,
                           CONFIG_SERVER_A_MINIMAL_XML, true);
    setUpBeforeClassClient(CONFIG_CLIENT_A_XML);
    WaarpLoggerFactory
        .setDefaultFactory(new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
  }

  /**
   * @throws Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    Thread.sleep(100);
    for (final DbTaskRunner dbTaskRunner : dbTaskRunners) {
      try {
        dbTaskRunner.delete();
      } catch (final WaarpDatabaseException e) {
        logger.warn("Cannot apply nolog to " + dbTaskRunner, e);
      }
    }
    final DbHostAuth host = new DbHostAuth("hostas");
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }
    final byte scode = -1;

    // Shutdown server
    logger.warn("Shutdown Server");
    Configuration.configuration.setTimeoutCon(100);
    final R66Future future = new R66Future(true);
    final ShutdownOrBlockJsonPacket node8 = new ShutdownOrBlockJsonPacket();
    node8.setRestartOrBlock(false);
    node8.setShutdownOrBlock(true);
    node8.setKey(FilesystemBasedDigest.passwdCrypt(
        Configuration.configuration.getServerAdminKey()));
    final AbstractLocalPacket valid =
        new JsonCommandPacket(node8, LocalPacketFactory.BLOCKREQUESTPACKET);
    sendInformation(valid, socketServerAddress, future, scode, false,
                    R66FiniteDualStates.SHUTDOWN, true);
    Thread.sleep(200);

    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    tearDownAfterClassServer();
  }

  @Test
  public void test01_SendToItself() throws IOException, InterruptedException {
    logger.warn("Start {}", Processes.getCurrentMethodName());
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 1000);
    final R66Future future = new R66Future(true);
    final SubmitTransfer transaction =
        new SubmitTransfer(future, "hostas", "testTask.txt", "rule3",
                           "Test Send Submit", true, 8192,
                           DbConstantR66.ILLEGALVALUE, null);
    transaction.run();
    future.awaitOrInterruptible();
    assertTrue(future.isSuccess());
    final DbTaskRunner runner = future.getResult().getRunner();
    if (runner != null) {
      logger.warn("Runner: {}", runner.toString());
      waitForAllDone(runner);
      dbTaskRunners.add(runner);
    }
    totest.delete();
    logger.warn("End {}", Processes.getCurrentMethodName());
  }


  private void waitForAllDone(DbTaskRunner runner) {
    while (true) {
      try {
        DbTaskRunner checkedRunner =
            new DbTaskRunner(runner.getSpecialId(), runner.getRequester(),
                             runner.getRequested());
        if (checkedRunner.isAllDone()) {
          logger.warn("DbTaskRunner done");
          return;
        } else if (checkedRunner.isInError()) {
          logger.error("DbTaskRunner in error");
          return;
        }
        Thread.sleep(100);
      } catch (InterruptedException e) {//NOSONAR
        logger.error("Interrupted", e);
        return;
      } catch (WaarpDatabaseException e) {
        logger.error("Cannot found DbTaskRunner", e);
        return;
      }
    }
  }
}
