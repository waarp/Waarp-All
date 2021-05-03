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

package org.waarp.openr66.protocol.monitoring;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.file.FileUtils;
import org.waarp.common.utility.Processes;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.client.MultipleDirectTransfer;
import org.waarp.openr66.client.SpooledDirectoryTransfer;
import org.waarp.openr66.client.SpooledDirectoryTransfer.Arguments;
import org.waarp.openr66.client.SubmitTransfer;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.junit.TestAbstract;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.json.ShutdownOrBlockJsonPacket;
import org.waarp.openr66.protocol.test.TestTransferNoDb;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.server.R66Server;

import java.io.File;
import java.io.IOException;
import java.net.SocketAddress;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class HttpMonitoringAbstract extends TestAbstract {

  private static final int nbThread = 10;
  private static final ArrayList<DbTaskRunner> dbTaskRunners =
      new ArrayList<DbTaskRunner>();
  private static final String CONFIG_SERVER_A_MINIMAL_RESPONSIVE_XXML =
      "config-serverA-minimal-Responsive.xml";

  static HttpServerExample httpServerExample;
  static final int port = 8999;
  static int numberTransfer = 0;

  /**
   * @throws Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    WaarpSystemUtil.stopLogger(true);
    Thread.sleep(100);
    for (final DbTaskRunner dbTaskRunner : dbTaskRunners) {
      try {
        dbTaskRunner.delete();
      } catch (final WaarpDatabaseException e) {
        logger.warn("Cannot apply nolog to " + dbTaskRunner + " : {}",
                    e.getMessage());
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

    // Stop monitoring shall be implicit
    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    tearDownAfterClassServer();
    // Stop Fake REST Server
    HttpServerExample.group.close().awaitUninterruptibly();
  }

  @After
  public void printCurrentRestMap() {
    Map<String, JsonNode> map =
        HttpServerExampleHandler.virtualMap.get("hosta");
    logger.warn("Map: {}", map == null? "NONE" : map.size());
  }

  @Test
  public void test5_DirectTransfer() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    final ExecutorService executorService = Executors.newCachedThreadPool();
    final int nb = nbThread;
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
    numberTransfer += nb;
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
        logger.error("Cannot found DbTaskRunner: {}", e.getMessage());
        return;
      }
    }
  }

  @Test
  public void test5_SubmitTransfer() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    final int nb = nbThread;
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
    numberTransfer += nb;
    Thread.sleep(100);
    totest.delete();
  }

  @Test
  public void test6_SendUsingTrafficShaping() throws IOException {
    logger.warn("Start Test of Send TrafficShaping Transfer");
    final int size = 20000;
    long bandwidth = 5000;
    final File totestBig =
        generateOutFile("/tmp/R66/out/testTaskBig.txt", size);

    Configuration.configuration
        .changeNetworkLimit(bandwidth, bandwidth, bandwidth, bandwidth, 1000);

    final R66Future future = new R66Future(true);
    final long time1 = System.currentTimeMillis();
    final TestTransferNoDb transaction =
        new TestTransferNoDb(future, "hostas", "testTaskBig.txt", "rule3",
                             "Test SendDirect Big With Traffic Shaping", true,
                             8192, DbConstantR66.ILLEGALVALUE,
                             networkTransaction);
    transaction.run();
    future.awaitOrInterruptible();
    final long time2 = System.currentTimeMillis();
    final long delay = time2 - time1;
    Configuration.configuration.changeNetworkLimit(0, 0, 0, 0, 1000);
    final R66Result result = future.getResult();
    checkFinalResult(future, result, delay, size);
    totestBig.delete();
    bandwidth = 0;
    numberTransfer++;
  }

  @Test
  public void test6_RecvUsingTrafficShaping() throws IOException {
    logger.warn("Start Test of Recv TrafficShaping Transfer");
    final int size = 20000;
    final long bandwidth = 5000;
    final File totestBig =
        generateOutFile("/tmp/R66/out/testTaskBig.txt", size);

    Configuration.configuration
        .changeNetworkLimit(bandwidth, bandwidth, bandwidth, bandwidth, 1000);

    final R66Future future = new R66Future(true);
    final long time1 = System.currentTimeMillis();
    final TestTransferNoDb transaction =
        new TestTransferNoDb(future, "hostas", "testTaskBig.txt", "rule4",
                             "Test RecvDirect Big With Traffic Shaping", true,
                             8192, DbConstantR66.ILLEGALVALUE,
                             networkTransaction);
    transaction.run();
    future.awaitOrInterruptible();
    final long time2 = System.currentTimeMillis();
    final long delay = time2 - time1;
    Configuration.configuration.changeNetworkLimit(0, 0, 0, 0, 1000);
    final R66Result result = future.getResult();
    checkFinalResult(future, result, delay, size);
    totestBig.delete();
    numberTransfer++;
  }

  private void test_Spooled(final SpooledThread spooledThread, final int factor)
      throws IOException, InterruptedException {
    final int size = 200;
    Configuration.configuration.changeNetworkLimit(0, 0, 0, 0, 1000);
    File directory = new File(SpooledThread.TMP_R_66_TEST_OUT_EXAMPLE);
    directory.mkdirs();
    File stop = new File(SpooledThread.TMP_R_66_TEST_STOPOUT_TXT);
    stop.delete();
    final R66Future future = new R66Future(true);
    spooledThread.future = future;
    spooledThread.start();
    Thread.sleep(200);
    logger.warn("1rst file");
    final File totestBig = generateOutFile(
        SpooledThread.TMP_R_66_TEST_OUT_EXAMPLE + "/testTaskBig.txt", size);
    Thread.sleep(4000 / factor);
    logger.warn("1rst file delete");
    totestBig.delete();
    Thread.sleep(2000);
    logger.warn("Second file");
    generateOutFile(
        SpooledThread.TMP_R_66_TEST_OUT_EXAMPLE + "/testTaskBig.txt", size);
    Thread.sleep(4000 / factor);
    logger.warn("Third file");
    generateOutFile(
        SpooledThread.TMP_R_66_TEST_OUT_EXAMPLE + "/testTaskBig.txt", size + 1,
        "abcdefghij");
    Thread.sleep(4000 / factor);
    logger.warn("Third file deleted");
    totestBig.delete();
    Thread.sleep(2000 / factor);
    generateOutFile(stop.getAbsolutePath(), 10);
    future.awaitOrInterruptible();
    assertTrue(future.isSuccess());
    stop.delete();
    File all = new File(SpooledThread.SPOOLED_ROOT);
    FileUtils.forceDeleteRecursiveDir(all);
    logger
        .warn("Launched {}", spooledThread.spooledDirectoryTransfer.getSent());
    logger.warn("Error {}", spooledThread.spooledDirectoryTransfer.getError());
    numberTransfer += 3;
  }

  @Test
  public void test70_Spooled() throws IOException, InterruptedException {
    logger.warn("Start Test of Spooled Transfer");
    SpooledThread spooledThread = new SpooledThread();
    spooledThread.ignoreAlreadyUsed = false;
    spooledThread.submit = false;
    test_Spooled(spooledThread, 2);
    assertEquals(3, spooledThread.spooledDirectoryTransfer.getSent());
    assertEquals(0, spooledThread.spooledDirectoryTransfer.getError());
  }

  @Test
  public void test70_SpooledSubmit() throws IOException, InterruptedException {
    logger.warn("Start Test of Spooled Transfer");
    SpooledThread spooledThread = new SpooledThread();
    spooledThread.ignoreAlreadyUsed = false;
    spooledThread.submit = true;
    test_Spooled(spooledThread, 2);
    assertEquals(3, spooledThread.spooledDirectoryTransfer.getSent());
    assertEquals(0, spooledThread.spooledDirectoryTransfer.getError());
  }

  @Test
  public void test91_MultipleDirectTransfer() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    final DbHostAuth host = new DbHostAuth("hostas");
    try {
      host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }
    final R66Future futureTransfer = new R66Future(true);
    final TestTransferNoDb transaction =
        new TestTransferNoDb(futureTransfer, "hostas", "testTask.txt", "rule3",
                             "Test SendDirect Small", true, 8192,
                             DbConstantR66.ILLEGALVALUE, networkTransaction);
    transaction.run();
    futureTransfer.awaitOrInterruptible();
    assertTrue("File transfer not ok", futureTransfer.isSuccess());
    numberTransfer++;
    final long id = futureTransfer.getRunner().getSpecialId();
    logger.warn("Remote Task: {}", id);

    // MultipleDirectTransfer
    logger.warn("Start MultipleDirectTransfer");
    final R66Future future = new R66Future(true);
    final MultipleDirectTransfer multipleDirectTransfer =
        new MultipleDirectTransfer(future, "hostas,hostas",
                                   "testTask.txt,testTask.txt", "rule3",
                                   "MultipleDirectTransfer", true, 1024,
                                   DbConstantR66.ILLEGALVALUE,
                                   networkTransaction);
    multipleDirectTransfer.run();
    future.awaitOrInterruptible();
    assertTrue("All sends should be OK", future.isSuccess());
    numberTransfer += 4;
    totest.delete();
  }

  @Test
  public void test95_VerifyMap() throws InterruptedException {
    Thread.sleep(3000);
    Map<String, JsonNode> map =
        HttpServerExampleHandler.virtualMap.get("hosta");
    logger.warn("Map: {}", map == null? "NONE" : map.size());
    assertNotNull(map);
    assertEquals(numberTransfer, map.size());
  }

  @Test
  public void test99_SigTermR66() throws InterruptedException {
    // global ant project settings
    final Project project = Processes.getProject(homeDir);
    try {
      File file =
          new File(dirResources, CONFIG_SERVER_A_MINIMAL_RESPONSIVE_XXML);
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
      if (!Processes.exists(pid)) {
        logger.warn("Process {} should be running", pid);
      }
      Processes.kill(pid, true);
      while (Processes.exists(pid)) {
        logger.warn("{} still running", pid);
        Thread.sleep(1000);
      }
    } finally {
      Processes.finalizeProject(project);
    }
  }

  private static class SpooledThread extends Thread {
    private static final String TMP_R_66_TEST_OUT_EXAMPLE =
        "/tmp/R66/test/out/example";
    private static final String TMP_R_66_TEST_STOPOUT_TXT =
        "/tmp/R66/test/stopout.txt";
    private static final String SPOOLED_ROOT = "/tmp/R66/test";
    R66Future future;
    private boolean ignoreAlreadyUsed = false;
    private boolean submit = false;
    private String host = "hostas";
    private final String waarpHost = "hostas";
    private SpooledDirectoryTransfer spooledDirectoryTransfer;

    @Override
    public void run() {
      Arguments arguments = new Arguments();
      arguments.setName("SpooledClient");
      arguments.getLocalDirectory().add(TMP_R_66_TEST_OUT_EXAMPLE);
      arguments.setStatusFile("/tmp/R66/test/statusoutdirect1.json");
      arguments.setStopFile(TMP_R_66_TEST_STOPOUT_TXT);
      arguments.setRule("rule3del");
      arguments.setFileInfo("fileInfo");
      arguments.setMd5(true);
      arguments.getRemoteHosts().add(host);
      if (waarpHost != null) {
        arguments.getWaarpHosts().add(waarpHost);
      }
      arguments.setBlock(5000);
      arguments.setRegex(null);
      arguments.setElapsed(100);
      arguments.setToSubmit(submit);
      arguments.setNoLog(false);
      arguments.setRecursive(false);
      arguments.setElapsedWaarp(100);
      arguments.setParallel(false);
      arguments.setLimitParallel(1);
      arguments.setMinimalSize(100);
      arguments.setLogWarn(false);
      arguments.setIgnoreAlreadyUsed(ignoreAlreadyUsed);

      spooledDirectoryTransfer =
          new SpooledDirectoryTransfer(future, arguments, networkTransaction);
      spooledDirectoryTransfer.run();
    }
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
        logger.warn("Transfer in Error with no Id" + " : {}",
                    future.getCause() != null? future.getCause().getMessage() :
                        "");
        assertTrue("Result should not be null, neither runner", false);
      }
      if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
        logger.warn("Transfer in Warning with Id: " +
                    result.getRunner().getSpecialId() + " : {}",
                    future.getCause() != null? future.getCause().getMessage() :
                        "");
        assertTrue("Transfer in Warning", false);
      } else {
        logger.error(
            "Transfer in Error with Id: " + result.getRunner().getSpecialId() +
            " : {}",
            future.getCause() != null? future.getCause().getMessage() : "");
        assertTrue("Transfer in Error", false);
      }
    }
  }

}
