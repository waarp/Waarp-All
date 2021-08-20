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

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runners.MethodSorters;
import org.waarp.common.crypto.HmacSha256;
import org.waarp.common.database.DbConstant;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.file.FileUtils;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.Processes;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.icap.server.IcapServer;
import org.waarp.icap.server.IcapServerHandler;
import org.waarp.openr66.client.Message;
import org.waarp.openr66.client.MultipleDirectTransfer;
import org.waarp.openr66.client.MultipleSubmitTransfer;
import org.waarp.openr66.client.NoOpRecvThroughHandler;
import org.waarp.openr66.client.SpooledDirectoryTransfer;
import org.waarp.openr66.client.SpooledDirectoryTransfer.Arguments;
import org.waarp.openr66.client.SubmitTransfer;
import org.waarp.openr66.client.TransferArgs;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.task.test.TestExecJavaTask;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.http.restv2.resthandlers.RestHandlerHook;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.InformationPacket;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.KeepAlivePacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.NoOpPacket;
import org.waarp.openr66.protocol.localhandler.packet.TestPacket;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.BusinessRequestJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.InformationJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.JsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ShutdownOrBlockJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.StopOrCancelJsonPacket;
import org.waarp.openr66.protocol.test.TestBusinessRequest;
import org.waarp.openr66.protocol.test.TestProgressBarTransfer;
import org.waarp.openr66.protocol.test.TestRecvThroughClient;
import org.waarp.openr66.protocol.test.TestSendThroughClient;
import org.waarp.openr66.protocol.test.TestTransaction;
import org.waarp.openr66.protocol.test.TestTransferNoDb;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.server.R66Server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.SocketAddress;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NetworkClientCompressionVs35Test extends TestAbstract {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final boolean SHOULD_COMPRESS = false;

  private static final int nbThread = 10;
  private static final ArrayList<DbTaskRunner> dbTaskRunners =
      new ArrayList<DbTaskRunner>();
  private static final String CONFIG_SERVER_A_MINIMAL_XML =
      "config-serverA-minimal-compress.xml";
  private static final String CONFIG_SERVER_B_NO_COMPRESS_XML =
      "config-serverB-compress.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML =
      "Linux/config/config-serverInitA.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_B_XML =
      "Linux/config/config-serverInitB.xml";
  private static final String CONFIG_CLIENT_A = "config-clientA-compress.xml";
  private static final String URL_3_5_2_WAARP_R66 =
      "https://github.com/waarp/Waarp-All/releases/download/v3.5.2/WaarpR66-3.5.2-jar-with-dependencies.jar";
  private static final String JAR352 = "/tmp/R66/conf/waarpr66-3.5.2.jar";
  private static final List<Integer> PIDS = new ArrayList<Integer>();
  private static int r66Pid1 = 999999;

  public static int startServer(String serverConfig) throws Exception {
    final File file2;
    if (serverConfig.charAt(0) == '/') {
      file2 = new File(serverConfig);
    } else {
      file2 = new File(dirResources, serverConfig);
    }
    if (file2.exists()) {
      System.err.println("Find server file: " + file2.getAbsolutePath());
      final String[] argsServer = {
          file2.getAbsolutePath()
      };
      // global ant project settings
      // First download previous version
      URL url = new URL(URL_3_5_2_WAARP_R66);
      InputStream stream = url.openStream();
      File jar = new File(JAR352);
      FileOutputStream outputStream = new FileOutputStream(jar);
      FileUtils.copy(stream, outputStream);
      project = Processes.getProject(homeDir);
      Processes.executeJvmSpecificClasspath(project, jar, R66Server.class,
                                            argsServer, true);
      int pid = Processes.getPidOfRunnerCommandLinux("java",
                                                     R66Server.class.getName(),
                                                     PIDS);
      PIDS.add(pid);
      logger.warn("Start Done: {}", pid);
      return pid;
    } else {
      System.err.println("Cannot find server file: " + file2.getAbsolutePath());
      fail("Cannot find server file");
      return 999999;
    }
  }

  /**
   * @throws Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ResourceLeakDetector.setLevel(Level.PARANOID);
    final ClassLoader classLoader =
        NetworkClientCompressionVs35Test.class.getClassLoader();
    final File file =
        new File(classLoader.getResource("logback-test.xml").getFile());
    setUpBeforeClassMinimal(LINUX_CONFIG_CONFIG_SERVER_INIT_B_XML);
    setUpDbBeforeClass();
    // setUpBeforeClassServer("Linux/config/config-serverInitB.xml", "config-serverB.xml", false);
    r66Pid1 = startServer(CONFIG_SERVER_B_NO_COMPRESS_XML);
    setUpBeforeClassServer(LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML,
                           CONFIG_SERVER_A_MINIMAL_XML, true);
    setUpBeforeClassClient(CONFIG_CLIENT_A);
  }

  /**
   * @throws Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    WaarpSystemUtil.stopLogger(true);
    for (int pid : PIDS) {
      Processes.kill(pid, true);
    }
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
    for (int pid : PIDS) {
      Processes.kill(pid, false);
    }

    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    tearDownAfterClassServer();
  }

  private boolean isCheckCompressionOk(final DbTaskRunner taskRunner) {
    if (taskRunner == null) {
      return true;
    }
    try {
      taskRunner.select();
      return SHOULD_COMPRESS == taskRunner.isBlockCompression();
    } catch (final WaarpDatabaseException e) {
      logger.error(e.getMessage());
      return true;
    }
  }

  private void checkCompression(final DbTaskRunner taskRunner) {
    if (taskRunner == null) {
      return;
    }
    try {
      taskRunner.select();
      assertEquals(SHOULD_COMPRESS, taskRunner.isBlockCompression());
    } catch (final WaarpDatabaseException e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void test2_PingPongPacket() throws WaarpDatabaseException {
    final DbHostAuth host = new DbHostAuth("hostb");
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }
    // Unit
    R66Future future = new R66Future(true);
    final TestPacket packet1 = new TestPacket("Test", "" + 0, 0);
    final TestTransaction transaction1 =
        new TestTransaction(networkTransaction, future, socketServerAddress,
                            packet1);
    transaction1.run();
    future.awaitOrInterruptible();
    assertTrue(future.isSuccess());

    final ExecutorService executorService = Executors.newCachedThreadPool();
    final int nb = nbThread;
    final R66Future[] arrayFuture = new R66Future[nb];
    logger.warn("Start Test of Transaction");
    final long time1 = System.currentTimeMillis();
    for (int i = 0; i < nb; i++) {
      arrayFuture[i] = new R66Future(true);
      final TestPacket packet = new TestPacket("Test", String.valueOf(i), 0);
      final TestTransaction transaction =
          new TestTransaction(networkTransaction, arrayFuture[i],
                              socketServerAddress, packet);
      executorService.execute(transaction);
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
    final long time2 = System.currentTimeMillis();
    logger.warn("Success: " + success + " Error: " + error + " NB/s: " +
                success * TestPacket.PINGPONG * 1000 / (time2 - time1));
    executorService.shutdown();
    assertEquals("Success should be total", nb, success);
    assertEquals("Errors should be 0", 0, error);
  }

  @Test
  public void test3_ProgressBarTransfer() throws IOException {
    logger.warn("Start Test of ProgressBar Transfer");
    final int size = 10000000;
    final File totestBig =
        generateOutFile("/tmp/R66/out/testTaskBig.txt", size);
    final R66Future future = new R66Future(true);
    final long time1 = System.currentTimeMillis();
    final TestProgressBarTransfer transaction =
        new TestProgressBarTransfer(future, "hostb", "testTaskBig.txt", "rule3",
                                    "Test Send Small ProgressBar #COMPRESS#",
                                    true, 65536, DbConstantR66.ILLEGALVALUE,
                                    networkTransaction, 100);
    transaction.run();
    future.awaitOrInterruptible();
    final long time2 = System.currentTimeMillis();
    final long delay = time2 - time1;
    final R66Result result = future.getResult();
    checkFinalResult(future, result, delay, size);
    totestBig.delete();
  }

  @Test
  public void test4_RecvThroughClient() throws IOException {
    logger.warn("Start Test of Recv Through Transfer");
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    try {
      Thread.sleep(20);
    } catch (InterruptedException e) {
    }
    final NoOpRecvThroughHandler handler = new NoOpRecvThroughHandler();
    R66Future future = new R66Future(true);
    TestRecvThroughClient transaction =
        new TestRecvThroughClient(future, handler, "hostb", "testTask.txt",
                                  "rule6", "Test RecvThrough Small #COMPRESS#",
                                  true, 8192, networkTransaction);
    long time1 = System.currentTimeMillis();
    transaction.run();
    future.awaitOrInterruptible();

    long time2 = System.currentTimeMillis();
    long delay = time2 - time1;
    R66Result result = future.getResult();
    checkFinalResult(future, result, delay, 10);
    totest.delete();

    final int size = 100000000;
    final File totestBig =
        generateOutFile("/tmp/R66/out/testTaskBig.txt", size);
    future = new R66Future(true);
    transaction =
        new TestRecvThroughClient(future, handler, "hostb", "testTaskBig.txt",
                                  "rule6", "Test RecvThrough Big #COMPRESS#",
                                  true, 65536, networkTransaction);
    time1 = System.currentTimeMillis();
    transaction.run();
    future.awaitOrInterruptible();

    time2 = System.currentTimeMillis();
    delay = time2 - time1;
    result = future.getResult();
    checkFinalResult(future, result, delay, size);
    totestBig.delete();
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
      // Check is compression correct
      checkCompression(result.getRunner());
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
        new TestSendThroughClient(future, "hostb", "testTask.txt", "rule5",
                                  "Test SendThrough Small #COMPRESS#", true,
                                  8192, networkTransaction);
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

    final int size = 100000000;
    final File totestBig =
        generateOutFile("/tmp/R66/out/testTaskBig.txt", size);
    future = new R66Future(true);
    transaction =
        new TestSendThroughClient(future, "hostb", "testTaskBig.txt", "rule5",
                                  "Test SendThrough Big #COMPRESS#", true,
                                  65536, networkTransaction);
    time1 = System.currentTimeMillis();
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

    time2 = System.currentTimeMillis();
    delay = time2 - time1;
    result = future.getResult();
    checkFinalResult(future, result, delay, size);
    totestBig.delete();
  }

  @Test
  public void test5_MultipleRecvThroughClient()
      throws IOException, InterruptedException {
    logger.warn("Start Test of Multiple Recv Through Transfer");
    File totest = generateOutFile("/tmp/R66/out/testTask.txt", 100000);
    int NUMBER_FILES = 10;
    ArrayList<R66Future> futures = new ArrayList<R66Future>(NUMBER_FILES);
    ExecutorService executorService =
        Executors.newFixedThreadPool(NUMBER_FILES);
    final NoOpRecvThroughHandler handler = new NoOpRecvThroughHandler();
    long timestart = System.currentTimeMillis();
    for (int i = 0; i < NUMBER_FILES; i++) {
      final R66Future future = new R66Future(true);
      futures.add(future);
      final TestRecvThroughClient transaction =
          new TestRecvThroughClient(future, handler, "hostb", "testTask.txt",
                                    "rule6",
                                    "Test Multiple RecvThrough #COMPRESS#",
                                    true, 8192, networkTransaction);
      executorService.execute(transaction);
    }
    Thread.sleep(100);
    executorService.shutdown();
    for (int i = 0; i < NUMBER_FILES; i++) {
      final R66Future future = futures.remove(0);
      future.awaitOrInterruptible();
      assertTrue(future.isSuccess());
      // Check is compression correct
      checkCompression(future.getRunner());
    }
    long timestop = System.currentTimeMillis();
    logger.warn(
        "RecvThrough {} files from R2" + " ({} seconds,  {} per seconds)",
        NUMBER_FILES, (timestop - timestart) / 1000,
        NUMBER_FILES * 1000 / (timestop - timestart));
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
          new TestTransferNoDb(arrayFuture[i], "hostb", "testTask.txt", "rule3",
                               "Test SendDirect Small #COMPRESS#", true, 8192,
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
      if (arrayFuture[i].isSuccess() &&
          isCheckCompressionOk(arrayFuture[i].getRunner())) {
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

  @Test
  public void test5_DirectTransferNoFollowCheck() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    final R66Future future = new R66Future(true);
    logger.warn("Start Test of DirectTransferNoFollowCheck");
    final long time1 = System.currentTimeMillis();
    final TestTransferNoDb transaction =
        new TestTransferNoDb(future, "hostb", "testTask.txt", "rule3",
                             "Test SendDirect Retransfer -nofollow #COMPRESS#",
                             true, 8192, DbConstantR66.ILLEGALVALUE,
                             networkTransaction);
    transaction.run();
    int success = 0;
    int error = 0;
    future.awaitOrInterruptible();
    String followId = null;
    if (future.getRunner() != null) {
      followId = future.getRunner().getFollowId();
      dbTaskRunners.add(future.getRunner());
    }
    if (future.isSuccess() && isCheckCompressionOk(future.getRunner())) {
      success++;
    } else {
      error++;
    }
    if (ParametersChecker.isEmpty(followId)) {
      success++;
    } else {
      logger.warn("Cannot check FollowId");
    }
    final long time2 = System.currentTimeMillis();
    logger.warn("Success: " + success + " Error: " + error + " NB/s: " +
                success * 1000 / (time2 - time1));
    totest.delete();

    assertEquals("Success should be total", 2, success);

    assertEquals("Errors should be 0", 0, error);
  }

  @Test
  public void test5_DirectTransferMultipleBlockSize() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 1000000);
    final int nb = 10;
    final R66Future[] arrayFuture = new R66Future[nb];
    logger.warn("Start Test of DirectTransfer");
    final long time1 = System.currentTimeMillis();
    for (int i = 0; i < nb; i++) {
      arrayFuture[i] = new R66Future(true);
      final TestTransferNoDb transaction =
          new TestTransferNoDb(arrayFuture[i], "hostb", "testTask.txt", "rule3",
                               "Test SendDirect Small #COMPRESS#", true,
                               8192 * (i + 1) * 2, DbConstantR66.ILLEGALVALUE,
                               networkTransaction);
      transaction.run();
    }
    int success = 0;
    int error = 0;
    for (int i = 0; i < nb; i++) {
      arrayFuture[i].awaitOrInterruptible();
      if (arrayFuture[i].getRunner() != null) {
        logger.warn("{} {}", arrayFuture[i].getRunner().getBlocksize(),
                    8192 * (i + 1) * 2);
        assertTrue(
            arrayFuture[i].getRunner().getBlocksize() <= 8192 * (i + 1) * 2);
        assertTrue(arrayFuture[i].getRunner().getBlocksize() <=
                   Configuration.configuration.getBlockSize());
        checkCompression(arrayFuture[i].getRunner());
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
    totest.delete();
    assertEquals("Success should be total", nb, success);
    assertEquals("Errors should be 0", 0, error);
  }

  @Test
  public void test5_DirectTransferThroughId() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    logger.warn("Start Test of DirectTransfer");
    final long time1 = System.currentTimeMillis();
    int success = 0;
    int error = 0;
    long specialId = DbConstant.ILLEGALVALUE;
    // First transfer
    {
      final R66Future future = new R66Future(true);
      final TestTransferNoDb transaction =
          new TestTransferNoDb(future, "hostb", "testTask.txt", "rule3",
                               "Test SendDirect Small #COMPRESS#", true, 8192,
                               DbConstantR66.ILLEGALVALUE, networkTransaction);
      transaction.run();
      future.awaitOrInterruptible();
      if (future.getRunner() != null) {
        dbTaskRunners.add(future.getRunner());
        specialId = future.getRunner().getSpecialId();
      }
      if (future.isSuccess() && isCheckCompressionOk(future.getRunner())) {
        success++;
        logger.warn("Success for first transfer");
      } else {
        error++;
        fail("Should be able to find out the previous DbTaskRunner");
      }
    }
    // Now second transfer based on ID
    String[] args = { "falsConfigFile", "-id", "" + specialId, "-to", "hostb" };
    TransferArgs transferArgs = SubmitTransferTest.getParamsInternal(1, args);
    if (transferArgs != null) {
      String rule = transferArgs.getRulename();
      String localFilename = transferArgs.getFilename();
      logger.warn("Success for finding previous transfer {} {} {}", specialId,
                  rule, localFilename);
      final R66Future future2 = new R66Future(true);
      final Timestamp newstart = new Timestamp(System.currentTimeMillis());
      logger.warn("Start second submit transfer");
      SubmitTransfer submitTransfer =
          new SubmitTransfer(future2, "hostb", localFilename, rule,
                             "Test Send 2 Submit Small #COMPRESS#", true, 8192,
                             DbConstantR66.ILLEGALVALUE, newstart);
      logger.warn("Running second submit transfer");
      submitTransfer.run();
      logger.warn("Waiting second submit transfer");
      future2.awaitOrInterruptible();
      logger.warn("End wait for second submit transfer {}",
                  future2.isSuccess());
      if (future2.isSuccess()) {
        success++;
      } else {
        error++;
        fail("Should be able to find out the previous DbTaskRunner");
      }
      logger.warn("Prepare transfer Success: " + success + " Error: " + error);
      assertEquals("Success should be 2", 2, success);
      assertEquals("Errors should be 0", 0, error);
      // Now wait for all transfers done
      if (future2.isSuccess()) {
        final DbTaskRunner runner2 = future2.getResult().getRunner();
        logger.warn("Wait for second submit transfer {}", runner2);
        if (runner2 != null) {
          waitForAllDone(runner2);
          dbTaskRunners.add(runner2);
        }
      }
      Thread.sleep(100);
    } else {
      logger.error(Messages.getString("AbstractBusinessRequest.NeedMoreArgs",
                                      "(-to -rule -file | -to -id)")); //$NON-NLS-1$
      error++;
    }
    SubmitTransferTest.clearInternal();
    String[] args2 = { "falsConfigFile", "-id", "1", "-to", "hostb" };
    assertNull(SubmitTransferTest.getParamsInternal(1, args2));
    final long time2 = System.currentTimeMillis();
    logger.warn("Success: " + success + " Error: " + error + " NB/s: " +
                2 * success * 1000 / (time2 - time1));
    Thread.sleep(200);
    totest.delete();
    assertEquals("Success should be 2", 2, success);
    assertEquals("Errors should be 0", 0, error);
  }

  private void waitForAllDone(DbTaskRunner runner) {
    while (true) {
      try {
        runner.select();
        if (runner.isAllDone()) {
          logger.info("DbTaskRunner done");
          if (!isCheckCompressionOk(runner)) {
            logger.error("DbTaskRunner in error for compression");
            fail("Compression shall be " + SHOULD_COMPRESS + " but is " +
                 runner.isBlockCompression());
          }
          return;
        } else if (runner.isInError()) {
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
  public void test5_DirectTransferWrongExecOutput() throws Exception {
    File commandLine = new File("/tmp/R66/conf/bin/wrong-exec.sh");
    if (!commandLine.setExecutable(true)) {
      logger.warn("Cannot set executable property to {}", commandLine);
      return;
    }
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    final R66Future future = new R66Future(true);
    logger.warn("Start Test of DirectTransfer");
    final long time1 = System.currentTimeMillis();
    final TestTransferNoDb transaction =
        new TestTransferNoDb(future, "hostb", "testTask.txt", "rulewrongexec",
                             "Test SendDirect Small #COMPRESS#", true, 8192,
                             DbConstantR66.ILLEGALVALUE, networkTransaction);
    transaction.run();
    int success = 0;
    int error = 0;
    future.awaitOrInterruptible();
    if (future.getRunner() != null) {
      dbTaskRunners.add(future.getRunner());
    }
    if (future.isSuccess()) {
      success++;
    } else {
      error++;
    }
    final long time2 = System.currentTimeMillis();
    logger.warn("Success: " + success + " Error: " + error + " NB/s: " +
                success * 1000 / (time2 - time1));
    Thread.sleep(200);
    totest.delete();
    assertEquals("Success should be 0", 0, success);
    assertEquals("Errors should be 1", 1, error);
  }

  @Test
  public void test5_DirectTransferPullWrong() throws Exception {
    final R66Future future = new R66Future(true);
    logger.warn("Start Test of DirectTransfer");
    final long time1 = System.currentTimeMillis();
    final TestTransferNoDb transaction =
        new TestTransferNoDb(future, "hostb", "testTaskNotExists.txt", "rule4",
                             "Test RecvDirect Small #COMPRESS#", true, 8192,
                             DbConstantR66.ILLEGALVALUE, networkTransaction);
    transaction.run();
    int success = 0;
    int error = 0;
    future.awaitOrInterruptible();
    if (future.getRunner() != null) {
      dbTaskRunners.add(future.getRunner());
    }
    if (future.isSuccess()) {
      success++;
    } else {
      error++;
    }
    final long time2 = System.currentTimeMillis();
    logger.warn("Success: " + success + " Error: " + error + " NB/s: " +
                success * 1000 / (time2 - time1));
    Thread.sleep(200);
    assertEquals("Success should be 0", 0, success);
    assertEquals("Errors should be 1", 1, error);
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
          new SubmitTransfer(arrayFuture[i], "hostb", "testTask.txt", "rule3",
                             "Test Send Submit Small #COMPRESS#", true, 8192,
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
  public void test6_SendUsingTrafficShaping() throws IOException {
    logger.warn("Start Test of Send TrafficShaping Transfer");
    final int size = 20000;
    long bandwidth = 5000;
    final File totestBig =
        generateOutFile("/tmp/R66/out/testTaskBig.txt", size);

    Configuration.configuration.changeNetworkLimit(bandwidth, bandwidth,
                                                   bandwidth, bandwidth, 1000);

    final R66Future future = new R66Future(true);
    final long time1 = System.currentTimeMillis();
    final TestTransferNoDb transaction =
        new TestTransferNoDb(future, "hostb", "testTaskBig.txt", "rule3",
                             "Test SendDirect Big With Traffic Shaping #COMPRESS#",
                             true, 8192, DbConstantR66.ILLEGALVALUE,
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
  }

  @Test
  public void test6_RecvUsingTrafficShaping() throws IOException {
    logger.warn("Start Test of Recv TrafficShaping Transfer");
    final int size = 20000;
    final long bandwidth = 5000;
    final File totestBig =
        generateOutFile("/tmp/R66/out/testTaskBig.txt", size);

    Configuration.configuration.changeNetworkLimit(bandwidth, bandwidth,
                                                   bandwidth, bandwidth, 1000);

    final R66Future future = new R66Future(true);
    final long time1 = System.currentTimeMillis();
    final TestTransferNoDb transaction =
        new TestTransferNoDb(future, "hostb", "testTaskBig.txt", "rule4",
                             "Test RecvDirect Big With Traffic Shaping #COMPRESS#",
                             true, 8192, DbConstantR66.ILLEGALVALUE,
                             networkTransaction);
    transaction.run();
    future.awaitOrInterruptible();
    final long time2 = System.currentTimeMillis();
    final long delay = time2 - time1;
    Configuration.configuration.changeNetworkLimit(0, 0, 0, 0, 1000);
    final R66Result result = future.getResult();
    checkFinalResult(future, result, delay, size);
    totestBig.delete();
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
    logger.warn("Launched {}",
                spooledThread.spooledDirectoryTransfer.getSent());
    logger.warn("Error {}", spooledThread.spooledDirectoryTransfer.getError());
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
  public void test70_SpooledIgnore() throws IOException, InterruptedException {
    logger.warn("Start Test of Spooled Ignored Changed File Transfer");
    SpooledThread spooledThread = new SpooledThread();
    spooledThread.ignoreAlreadyUsed = true;
    spooledThread.submit = false;
    test_Spooled(spooledThread, 2);
    assertEquals(2, spooledThread.spooledDirectoryTransfer.getSent());
    assertEquals(0, spooledThread.spooledDirectoryTransfer.getError());
  }

  @Test
  public void test70_SpooledSubmitIgnore()
      throws IOException, InterruptedException {
    logger.warn("Start Test of Spooled Ignored Changed File Transfer");
    SpooledThread spooledThread = new SpooledThread();
    spooledThread.ignoreAlreadyUsed = true;
    spooledThread.submit = true;
    test_Spooled(spooledThread, 2);
    assertEquals(2, spooledThread.spooledDirectoryTransfer.getSent());
    assertEquals(0, spooledThread.spooledDirectoryTransfer.getError());
  }

  @Test
  public void test72_Transfer_With_Icap_Send() throws Exception {
    IcapServerHandler.resetJunitStatus();
    int success = 0;
    int error = 0;
    try {
      IcapServer.main(new String[] { "127.0.0.1", "9999" });
      final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 100);
      final R66Future future = new R66Future(true);
      logger.warn("Start Test of DirectTransfer with Icap");
      final TestTransferNoDb transaction =
          new TestTransferNoDb(future, "hostb", "testTask.txt", "rule3icap",
                               "Test SendDirect Small #COMPRESS#", true, 8192,
                               DbConstantR66.ILLEGALVALUE, networkTransaction);
      transaction.run();
      future.awaitOrInterruptible();
      DbTaskRunner runner = null;
      if (future.getRunner() != null) {
        runner = future.getRunner();
      }
      if (future.isSuccess() && isCheckCompressionOk(runner)) {
        success++;
      } else {
        error++;
      }
      logger.warn("Success: " + success + " Error: " + error);
      totest.delete();
      if (runner != null) {
        runner.delete();
      }
    } finally {
      IcapServer.shutdown();
      Thread.sleep(100);
      logger.debug("Server stopped");
    }
    assertEquals("Success should be total", 1, success);
    assertEquals("Errors should be 0", 0, error);
  }

  @Test
  public void test80_JsonCommandsBusiness() throws Exception {
    final DbHostAuth host = new DbHostAuth("hostb");
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }

    final byte scode = -1;
    logger.warn("Business");
    final R66Future future = new R66Future(true);
    AbstractLocalPacket valid = null;
    final BusinessRequestJsonPacket node3 = new BusinessRequestJsonPacket();
    node3.setClassName(TestExecJavaTask.class.getName());
    node3.setArguments(" business 0");
    node3.setExtraArguments(" simple business request");
    node3.setDelay(0);
    valid =
        new JsonCommandPacket(node3, LocalPacketFactory.BUSINESSREQUESTPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, false);

  }

  @Test
  public void test80_JsonCommandsTest() throws Exception {
    final DbHostAuth host = new DbHostAuth("hostb");
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }

    final byte scode = -1;
    logger.warn("Test");
    final R66Future future = new R66Future(true);
    AbstractLocalPacket valid = null;
    final JsonPacket node9 = new JsonPacket();
    node9.setComment("Test Message");
    node9.setRequestUserPacket(LocalPacketFactory.TESTPACKET);
    valid = new JsonCommandPacket(node9, LocalPacketFactory.TESTPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, false);

    logger.warn("End Json");
  }

  @Test
  public void test81_JsonCommandsWithTransferId() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    final DbHostAuth host = new DbHostAuth("hostb");
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }
    final R66Future futureTransfer = new R66Future(true);
    final TestTransferNoDb transaction =
        new TestTransferNoDb(futureTransfer, "hostb", "testTask.txt", "rule3",
                             "Test SendDirect Small #COMPRESS#", true, 8192,
                             DbConstantR66.ILLEGALVALUE, networkTransaction);
    transaction.run();
    futureTransfer.awaitOrInterruptible();
    assertTrue("File transfer not ok", futureTransfer.isSuccess());
    final long id = futureTransfer.getResult().getRunner().getSpecialId();
    logger.warn("Remote Task: {}",
                futureTransfer.getResult().getRunner().getSpecialId());

    final byte scode = -1;

    R66Future future;
    AbstractLocalPacket valid;

    logger.warn("Information");
    future = new R66Future(true);
    InformationJsonPacket node4 = new InformationJsonPacket(id, true, "hostb");
    valid = new JsonCommandPacket(node4, LocalPacketFactory.INFORMATIONPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, true);

    logger.warn("Information2");
    future = new R66Future(true);
    node4 = new InformationJsonPacket(
        (byte) InformationPacket.ASKENUM.ASKMLSDETAIL.ordinal(), "rule3",
        "testTask.txt");
    valid = new JsonCommandPacket(node4, LocalPacketFactory.INFORMATIONPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, true);

    logger.warn("Stop");
    future = new R66Future(true);
    final StopOrCancelJsonPacket node5 = new StopOrCancelJsonPacket();
    node5.setRequested("hostb");
    node5.setRequester("hosta");
    node5.setSpecialid(id);
    valid = new JsonCommandPacket(node5, LocalPacketFactory.STOPPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, true);
    logger.warn("Cancel");
    future = new R66Future(true);
    valid = new JsonCommandPacket(node5, LocalPacketFactory.CANCELPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, true);

    logger.warn("End Json");
    totest.delete();
  }

  @Test
  public void test83_BusinessRequest() throws WaarpDatabaseException {
    logger.warn("Start Test of BusinessRequest");
    final DbHostAuth host = new DbHostAuth("hostb");
    assertEquals("Error should be at 0", 0,
                 TestBusinessRequest.runTest(networkTransaction, host, 3));
  }

  @Test
  public void test91_ExtraCommandsInformationPacket() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    try {
      setUpBeforeClassClient("config-clientB.xml");
      final DbHostAuth host = new DbHostAuth("hostb");
      final SocketAddress socketServerAddress;
      try {
        socketServerAddress = host.getSocketAddress();
      } catch (final IllegalArgumentException e) {
        logger.error("Needs a correct configuration file as first argument");
        return;
      }
      final R66Future futureTransfer = new R66Future(true);
      final TestTransferNoDb transaction =
          new TestTransferNoDb(futureTransfer, "hostb", "testTask.txt", "rule3",
                               "Test SendDirect Small #COMPRESS#", true, 8192,
                               DbConstantR66.ILLEGALVALUE, networkTransaction);
      transaction.run();
      futureTransfer.awaitOrInterruptible();
      assertTrue("File transfer not ok", futureTransfer.isSuccess());
      final long id = futureTransfer.getRunner().getSpecialId();
      logger.warn("Remote Task: {}", id);

      // InformationPacket
      logger.warn("Start Extra commands: InformationPacket");
      byte scode = -1;
      InformationPacket informationPacket =
          new InformationPacket(String.valueOf(id), scode, "0");
      R66Future future = new R66Future(true);
      sendInformation(informationPacket, socketServerAddress, future, scode,
                      false, R66FiniteDualStates.INFORMATION, true);

      logger.warn("Start Extra commands: ASKEXIST");
      future = new R66Future(true);
      scode = (byte) InformationPacket.ASKENUM.ASKEXIST.ordinal();
      informationPacket = new InformationPacket("rule3", scode, "testTask.txt");
      sendInformation(informationPacket, socketServerAddress, future, scode,
                      true, R66FiniteDualStates.INFORMATION, true);

      logger.warn("Start Extra commands: ASKLIST");
      future = new R66Future(true);
      scode = (byte) InformationPacket.ASKENUM.ASKLIST.ordinal();
      informationPacket = new InformationPacket("rule3", scode, "testTask.txt");
      sendInformation(informationPacket, socketServerAddress, future, scode,
                      true, R66FiniteDualStates.INFORMATION, true);

      logger.warn("Start Extra commands: ASKMLSDETAIL");
      future = new R66Future(true);
      scode = (byte) InformationPacket.ASKENUM.ASKMLSDETAIL.ordinal();
      informationPacket = new InformationPacket("rule3", scode, "testTask.txt");
      sendInformation(informationPacket, socketServerAddress, future, scode,
                      true, R66FiniteDualStates.INFORMATION, true);

      logger.warn("Start Extra commands: ASKMLSLIST");
      future = new R66Future(true);
      scode = (byte) InformationPacket.ASKENUM.ASKMLSLIST.ordinal();
      informationPacket = new InformationPacket("rule3", scode, "testTask.txt");
      sendInformation(informationPacket, socketServerAddress, future, scode,
                      true, R66FiniteDualStates.INFORMATION, true);

      // ValidPacket BANDWIDTH
      logger.warn("Start Extra commands: BANDWIDTHPACKET)");
      future = new R66Future(true);
      final ValidPacket valid =
          new ValidPacket("-1", "-1", LocalPacketFactory.BANDWIDTHPACKET);
      sendInformation(valid, socketServerAddress, future, scode, true,
                      R66FiniteDualStates.VALIDOTHER, true);
    } finally {
      setUpBeforeClassClient(CONFIG_CLIENT_A);
      totest.delete();
    }
  }

  @Test
  public void test91_MultipleDirectTransfer() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    final DbHostAuth host = new DbHostAuth("hostb");
    try {
      host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }
    final R66Future futureTransfer = new R66Future(true);
    final TestTransferNoDb transaction =
        new TestTransferNoDb(futureTransfer, "hostb", "testTask.txt", "rule3",
                             "Test SendDirect Small #COMPRESS#", true, 8192,
                             DbConstantR66.ILLEGALVALUE, networkTransaction);
    transaction.run();
    futureTransfer.awaitOrInterruptible();
    assertTrue("File transfer not ok", futureTransfer.isSuccess() &&
                                       isCheckCompressionOk(
                                           futureTransfer.getRunner()));
    final long id = futureTransfer.getRunner().getSpecialId();
    logger.warn("Remote Task: {}", id);

    // MultipleDirectTransfer
    logger.warn("Start MultipleDirectTransfer");
    final R66Future future = new R66Future(true);
    final MultipleDirectTransfer multipleDirectTransfer =
        new MultipleDirectTransfer(future, "hostb,hostb",
                                   "testTask.txt,testTask.txt", "rule3",
                                   "MultipleDirectTransfer #COMPRESS#", true,
                                   1024, DbConstantR66.ILLEGALVALUE,
                                   networkTransaction);
    multipleDirectTransfer.run();
    future.awaitOrInterruptible();
    assertTrue("All sends should be OK", future.isSuccess());

    totest.delete();
  }

  @Test
  public void test92_ExtraCommandsOther() throws Exception {
    try {
      setUpBeforeClassClient("config-clientB.xml");
      ValidPacket valid;
      final byte scode = -1;
      final DbHostAuth host = new DbHostAuth("hostb");
      final SocketAddress socketServerAddress;
      try {
        socketServerAddress = host.getSocketAddress();
      } catch (final IllegalArgumentException e) {
        logger.error("Needs a correct configuration file as first argument");
        return;
      }
      R66Future future;

      // Message
      logger.warn("Start Message");
      future = new R66Future(true);
      final TestPacket packet = new TestPacket("MSG", "Message", 2);
      final Message message =
          new Message(networkTransaction, future, "hostb", packet);
      message.run();
      future.awaitOrInterruptible();
      assertTrue("Message should be OK", future.isSuccess());

      // MultipleSubmitTransfer
      logger.warn("Start MultipleSubmitTransfer");
      future = new R66Future(true);
      File file = generateOutFile("/tmp/R66/out/testTask.txt", 10);
      final MultipleSubmitTransfer multipleSubmitTransfer =
          new MultipleSubmitTransfer(future, "hostb",
                                     "/tmp/R66/out/testTask.txt", "rule3",
                                     "Multiple Submit #COMPRESS#", true, 1024,
                                     DbConstantR66.ILLEGALVALUE, null,
                                     networkTransaction);
      multipleSubmitTransfer.run();
      future.awaitOrInterruptible();
      assertTrue("All submits should be OK", future.isSuccess());

      // ValidPacket CONFEXPORTPACKET
      logger.warn("Start Valid CONFEXPORTPACKET");
      future = new R66Future(true);
      valid = new ValidPacket(Boolean.TRUE.toString(), Boolean.TRUE.toString(),
                              LocalPacketFactory.CONFEXPORTPACKET);
      sendInformation(valid, socketServerAddress, future, scode, true,
                      R66FiniteDualStates.VALIDOTHER, true);

      // ValidPacket LOG
      logger.warn("Start Valid LOG");
      future = new R66Future(true);
      valid = new ValidPacket(null, null, LocalPacketFactory.LOGPACKET);
      sendInformation(valid, socketServerAddress, future, scode, true,
                      R66FiniteDualStates.VALIDOTHER, true);

      // ValidPacket LOGPURGE
      logger.warn("Start Valid LOGPURGE");
      future = new R66Future(true);
      valid = new ValidPacket(null, null, LocalPacketFactory.LOGPURGEPACKET);
      sendInformation(valid, socketServerAddress, future, scode, false,
                      R66FiniteDualStates.VALIDOTHER, true);

      // NoOpPacket
      logger.warn("Start NoOp");
      final long timeout = Configuration.configuration.getTimeoutCon();
      Configuration.configuration.setTimeoutCon(100);
      future = new R66Future(true);
      final NoOpPacket noOpPacket = new NoOpPacket();
      sendInformation(noOpPacket, socketServerAddress, future, scode, false,
                      R66FiniteDualStates.INFORMATION, true);

      // KeepAlivePacket
      logger.warn("Start KeepAlive");
      future = new R66Future(true);
      final KeepAlivePacket keepAlivePacket = new KeepAlivePacket();
      sendInformation(keepAlivePacket, socketServerAddress, future, scode, true,
                      R66FiniteDualStates.INFORMATION, true);
      Configuration.configuration.setTimeoutCon(timeout);
    } finally {
      setUpBeforeClassClient(CONFIG_CLIENT_A);
    }
  }

  public static final class RestHandlerHookForTest extends RestHandlerHook {
    public RestHandlerHookForTest(final boolean authenticated,
                                  final HmacSha256 hmac, final long delay) {
      super(authenticated, hmac, delay);
    }

    public boolean testcheckAuthorization(String user, Method method) {
      return checkAuthorization(user, method);
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

  private static class SpooledThread extends Thread {
    private static final String TMP_R_66_TEST_OUT_EXAMPLE =
        "/tmp/R66/test/out/example";
    private static final String TMP_R_66_TEST_STOPOUT_TXT =
        "/tmp/R66/test/stopout.txt";
    private static final String SPOOLED_ROOT = "/tmp/R66/test";
    R66Future future;
    private boolean ignoreAlreadyUsed = false;
    private boolean submit = false;
    private String host = "hostb";
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
      arguments.setFileInfo("fileInfo #COMPRESS#");
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
}
