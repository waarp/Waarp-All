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

package org.waarp.openr66.protocol.junit;

import org.apache.tools.ant.Project;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runners.MethodSorters;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.file.FileUtils;
import org.waarp.common.utility.Processes;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.client.SubmitTransfer;
import org.waarp.openr66.client.TransferArgs;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.json.ShutdownOrBlockJsonPacket;
import org.waarp.openr66.protocol.test.TestRecvThroughClient;
import org.waarp.openr66.protocol.test.TestRecvThroughClient.TestRecvThroughHandler;
import org.waarp.openr66.protocol.test.TestSendThroughClient;
import org.waarp.openr66.protocol.test.TestTransferNoDb;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.server.R66Server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NetworkClientMultipleIpsTest extends TestAbstract {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final ArrayList<DbTaskRunner> dbTaskRunners =
      new ArrayList<DbTaskRunner>();
  private static final String CONFIG_SERVER_A_MODEL_XML =
      "config-serverA-multipleIps-model.xml";
  private static final String CONFIG_SERVER_A_MULTIPLE_IPS_XML =
      "/tmp/R66/conf/config-serverA-multipleIps.xml";
  private static final String CONFIG_SERVER_A_WRONG_MULTIPLE_IPS_XML =
      "/tmp/R66/conf/config-serverA-Wrong-multipleIps.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML =
      "Linux/config/config-serverInitA.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_B_XML =
      "Linux/config/config-serverInitB.xml";
  private static final String CONFIG_CLIENT_A_XML = "config-clientA.xml";
  private static final String CONFIG_SERVER_A_MINIMAL_RESPONSIVE_XXML =
      "config-serverA-minimal-Responsive.xml";
  private static File model;

  /**
   * @throws Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    final ClassLoader classLoader =
        NetworkClientMultipleIpsTest.class.getClassLoader();
    final File file =
        new File(classLoader.getResource("logback-test.xml").getFile());
    setUpBeforeClassMinimal(LINUX_CONFIG_CONFIG_SERVER_INIT_B_XML);
    setUpDbBeforeClass();
    // setUpBeforeClassServer("Linux/config/config-serverInitB.xml", "config-serverB.xml", false);
    model = new File(file.getParentFile().getParentFile().getParentFile(),
                     "src/test/resources/Linux/config/" +
                     CONFIG_SERVER_A_MODEL_XML);
    logger.warn("Model file {}", model);
    if (!model.canRead()) {
      logger.error("Cannot access to Model file");
      fail("Cannot access to Model file");
      return;
    }
    String content = WaarpStringUtils.readFile(model.getAbsolutePath());
    List<InetAddress> addrList = new ArrayList<InetAddress>();
    for (Enumeration<NetworkInterface> eni =
         NetworkInterface.getNetworkInterfaces(); eni.hasMoreElements(); ) {
      final NetworkInterface ifc = eni.nextElement();
      if (ifc.isUp()) {
        for (Enumeration<InetAddress> ena = ifc.getInetAddresses();
             ena.hasMoreElements(); ) {
          addrList.add(ena.nextElement());
        }
      }
    }
    String ips = addrList.get(0).getHostAddress();
    for (int i = 1; i < addrList.size(); i++) {
      ips += "," + addrList.get(i).getHostAddress();
    }
    logger.warn("Addresses will be {}", ips);
    content = content.replace("#ADDRESSES#", ips);
    File target = new File(CONFIG_SERVER_A_MULTIPLE_IPS_XML);
    FileWriter writer = null;
    try {
      writer = new FileWriter(target);
      writer.write(content);
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (writer != null) {
        FileUtils.close(writer);
      }
    }
    setUpBeforeClassServer(LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML,
                           CONFIG_SERVER_A_MULTIPLE_IPS_XML, true);
    setUpBeforeClassClient(CONFIG_CLIENT_A_XML);
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
  public void test4_RecvThroughClient() throws IOException {
    logger.warn("Start Test of Recv Through Transfer");
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    try {
      Thread.sleep(20);
    } catch (InterruptedException e) {
    }
    final TestRecvThroughHandler handler = new TestRecvThroughHandler();
    R66Future future = new R66Future(true);
    TestRecvThroughClient transaction =
        new TestRecvThroughClient(future, handler, "hostas", "testTask.txt",
                                  "rule6", "Test RecvThrough Small", true, 8192,
                                  networkTransaction);
    long time1 = System.currentTimeMillis();
    transaction.run();
    future.awaitOrInterruptible();

    long time2 = System.currentTimeMillis();
    long delay = time2 - time1;
    R66Result result = future.getResult();
    checkFinalResult(future, result, delay, 10);
    totest.delete();
  }

  private void checkFinalResult(R66Future future, R66Result result, long delay,
                                long size) {
    if (future.isSuccess()) {
      if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
        logger.warn("Warning with Id: " + result.getRunner().getSpecialId() +
                    " on file: " +
                    (result.getFile() != null? result.getFile().toString() :
                        "no file") + " delay: " + delay + " kbps: " +
                    size * 8 / delay);
      } else {
        logger.warn("Success with Id: " + result.getRunner().getSpecialId() +
                    " on Final file: " +
                    (result.getFile() != null? result.getFile().toString() :
                        "no file") + " delay: " + delay + " kbps: " +
                    size * 8 / delay);
      }
      // In case of success, delete the runner
      dbTaskRunners.add(result.getRunner());
    } else {
      if (result == null || result.getRunner() == null) {
        logger.warn("Transfer in Error with no Id", future.getCause());
        assertTrue("Result should not be null, neither runner", false);
      }
      if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
        logger.warn(
            "Transfer in Warning with Id: " + result.getRunner().getSpecialId(),
            future.getCause());
        assertTrue("Transfer in Warning", false);
      } else {
        logger.error(
            "Transfer in Error with Id: " + result.getRunner().getSpecialId(),
            future.getCause());
        assertTrue("Transfer in Error", false);
      }
    }
  }

  @Test
  public void test4_SendThroughClient() throws IOException {
    logger.warn("Start Test of Send Through Transfer");
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    R66Future future = new R66Future(true);
    TestSendThroughClient transaction =
        new TestSendThroughClient(future, "hostas", "testTask.txt", "rule5",
                                  "Test SendThrough Small", true, 8192,
                                  networkTransaction);
    long time1 = System.currentTimeMillis();
    if (!transaction.initiateRequest()) {
      logger.error("Transfer in Error", future.getCause());
      return;
    }
    if (transaction.sendFile()) {
      transaction.finalizeRequest();
    } else {
      transaction.transferInError(null);
    }
    future.awaitOrInterruptible();

    long time2 = System.currentTimeMillis();
    long delay = time2 - time1;
    R66Result result = future.getResult();
    checkFinalResult(future, result, delay, 10);
    totest.delete();
  }

  @Test
  public void test5_DirectTransfer() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    final ExecutorService executorService = Executors.newCachedThreadPool();
    final int nb = 1;
    final R66Future[] arrayFuture = new R66Future[nb];
    logger.warn("Start Test of DirectTransfer");
    final long time1 = System.currentTimeMillis();
    for (int i = 0; i < nb; i++) {
      arrayFuture[i] = new R66Future(true);
      final TestTransferNoDb transaction =
          new TestTransferNoDb(arrayFuture[i], "hostas", "testTask.txt",
                               "rule3", "Test SendDirect Small", true, 8192,
                               DbConstantR66.ILLEGALVALUE, networkTransaction);
      executorService.execute(transaction);
    }
    int success = 0;
    int error = 0;
    for (int i = 0; i < nb; i++) {
      arrayFuture[i].awaitOrInterruptible();
      if (arrayFuture[i].getRunner() != null) {
        dbTaskRunners.add(arrayFuture[i].getRunner());
      }
      if (arrayFuture[i].isSuccess()) {
        success++;
      } else {
        error++;
      }
    }
    final long time2 = System.currentTimeMillis();
    logger.warn("Success: " + success + " Error: " + error + " NB/s: " +
                success * 1000 / (time2 - time1));
    executorService.shutdown();
    totest.delete();
    assertEquals("Success should be total", nb, success);
    assertEquals("Errors should be 0", 0, error);
  }

  private void waitForAllDone(DbTaskRunner runner) {
    while (true) {
      try {
        DbTaskRunner checkedRunner = DbTaskRunner.reloadFromDatabase(runner);
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

  @Test
  public void test5_SubmitTransfer() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    final int nb = 1;
    final R66Future[] arrayFuture = new R66Future[nb];
    logger.warn("Start Test of SubmitTransfer");

    for (int i = 0; i < nb; i++) {
      arrayFuture[i] = new R66Future(true);
      final Timestamp newstart =
          new Timestamp(System.currentTimeMillis() + i * 10);
      final SubmitTransfer transaction =
          new SubmitTransfer(arrayFuture[i], "hostas", "testTask.txt", "rule3",
                             "Test Send Submit Small", true, 8192,
                             DbConstantR66.ILLEGALVALUE, newstart);
      transaction.run();
    }
    int success = 0;
    int error = 0;
    for (int i = 0; i < nb; i++) {
      arrayFuture[i].awaitOrInterruptible();
      if (arrayFuture[i].isSuccess()) {
        success++;
      } else {
        error++;
      }
    }
    logger.warn("Prepare transfer Success: " + success + " Error: " + error);
    assertEquals("Success should be total", nb, success);
    assertEquals("Errors should be 0", 0, error);
    // Now wait for all transfers done
    for (int i = 0; i < nb; i++) {
      if (arrayFuture[i].isSuccess()) {
        final DbTaskRunner runner = arrayFuture[i].getResult().getRunner();
        if (runner != null) {
          waitForAllDone(runner);
          dbTaskRunners.add(runner);
        }
      }
    }
    Thread.sleep(100);
    totest.delete();
  }

  @Test
  public void test99_WrongMultipleIps() throws InterruptedException {
    String content = WaarpStringUtils.readFile(model.getAbsolutePath());
    String ips = "127.0.0.1,100.100.100.100";
    logger.warn("Addresses will be {}", ips);
    content = content.replace("#ADDRESSES#", ips);
    content = content.replace("6666", "6696");
    File target = new File(CONFIG_SERVER_A_WRONG_MULTIPLE_IPS_XML);
    FileWriter writer = null;
    try {
      writer = new FileWriter(target);
      writer.write(content);
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (writer != null) {
        FileUtils.close(writer);
      }
    }

    // global ant project settings
    final Project project = Processes.getProject(homeDir);
    try {
      File file = new File(CONFIG_SERVER_A_WRONG_MULTIPLE_IPS_XML);
      if (!file.exists()) {
        logger.error("File {} does not exist", file.getAbsolutePath());
        fail("File R66Server (for sigterm) does not exist");
      }
      final String[] argsServer = {
          file.getAbsolutePath()
      };
      int pid = Processes
          .executeJvm(project, homeDir, R66Server.class, argsServer, true);
      Thread.sleep(1000);
      int max = 10;
      while (Processes.exists(pid) && max > 0) {
        logger.warn("{} still running", pid);
        Thread.sleep(1000);
        max--;
      }
      if (Processes.exists(pid)) {
        logger.error("Process {} should not be running", pid);
        Processes.kill(pid, true);
        fail("Process should not be running");
      }
    } finally {
      Processes.finalizeProject(project);
    }
  }


  static class SubmitTransferTest extends SubmitTransfer {

    public SubmitTransferTest(final R66Future future, final String remoteHost,
                              final String filename, final String rulename,
                              final String fileinfo, final boolean isMD5,
                              final int blocksize, final long id,
                              final Timestamp starttime) {
      super(future, remoteHost, filename, rulename, fileinfo, isMD5, blocksize,
            id, starttime);
      if (!fileinfo.contains("-nofollow")) {
        TransferArgs.forceAnalyzeFollow(this);
      }
    }

    static public void clearInternal() {
      clear();
    }
  }
}
