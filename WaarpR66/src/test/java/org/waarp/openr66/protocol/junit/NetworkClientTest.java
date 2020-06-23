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

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.tools.ant.Project;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.waarp.common.crypto.HmacSha256;
import org.waarp.common.database.DbConstant;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.file.FileUtils;
import org.waarp.common.role.RoleDefault;
import org.waarp.common.utility.Processes;
import org.waarp.common.utility.Version;
import org.waarp.icap.server.IcapServer;
import org.waarp.icap.server.IcapServerHandler;
import org.waarp.openr66.client.Message;
import org.waarp.openr66.client.MultipleDirectTransfer;
import org.waarp.openr66.client.MultipleSubmitTransfer;
import org.waarp.openr66.client.RequestInformation;
import org.waarp.openr66.client.RequestTransfer;
import org.waarp.openr66.client.SpooledDirectoryTransfer;
import org.waarp.openr66.client.SpooledDirectoryTransfer.Arguments;
import org.waarp.openr66.client.SubmitTransfer;
import org.waarp.openr66.client.TransferArgs;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.task.test.TestExecJavaTask;
import org.waarp.openr66.dao.BusinessDAO;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.pojo.Business;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.http.restv2.resthandlers.RestHandlerHook;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.InformationPacket;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.KeepAlivePacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.NoOpPacket;
import org.waarp.openr66.protocol.localhandler.packet.TestPacket;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.BandwidthJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.BusinessRequestJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ConfigExportJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ConfigImportJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.InformationJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.JsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.LogJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ShutdownOrBlockJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.StopOrCancelJsonPacket;
import org.waarp.openr66.protocol.test.TestBusinessRequest;
import org.waarp.openr66.protocol.test.TestProgressBarTransfer;
import org.waarp.openr66.protocol.test.TestRecvThroughClient;
import org.waarp.openr66.protocol.test.TestRecvThroughClient.TestRecvThroughHandler;
import org.waarp.openr66.protocol.test.TestSendThroughClient;
import org.waarp.openr66.protocol.test.TestTasks;
import org.waarp.openr66.protocol.test.TestTransaction;
import org.waarp.openr66.protocol.test.TestTransferNoDb;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.server.R66Server;
import org.waarp.thrift.r66.Action;
import org.waarp.thrift.r66.R66Request;
import org.waarp.thrift.r66.R66Service;
import org.waarp.thrift.r66.RequestMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.SocketAddress;
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
public class NetworkClientTest extends TestAbstract {

  private static final int nbThread = 10;
  private static final ArrayList<DbTaskRunner> dbTaskRunners =
      new ArrayList<DbTaskRunner>();
  private static final String CONFIG_SERVER_A_MINIMAL_XML =
      "config-serverA-minimal.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML =
      "Linux/config/config-serverInitA.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_B_XML =
      "Linux/config/config-serverInitB.xml";
  private static final String CONFIG_CLIENT_A_XML = "config-clientA.xml";
  private static final String CONFIG_SERVER_A_MINIMAL_RESPONSIVE_XXML =
      "config-serverA-minimal-Responsive.xml";

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
  public void test1_roles() {
    logger.warn("Start of Roles tests");
    RoleDefault roleDefault =
        Configuration.configuration.getRoles().get("test");
    assertTrue(roleDefault.hasReadOnly());
    assertTrue(roleDefault.hasTransfer());
    assertTrue(roleDefault.hasLogControl());
    logger.warn(roleDefault.toString());
    BusinessDAO businessDAO = null;
    try {
      businessDAO = DAOFactory.getInstance().getBusinessDAO();
      Business business =
          businessDAO.select(Configuration.configuration.getHostId());
      logger.warn("HostConfig {} A: {} B: {} R: {} O: {}", business.getHostid(),
                  business.getAliases(), business.getBusiness(),
                  business.getRoles(), business.getOthers());
      assertFalse(business.getAliases().isEmpty());
      assertFalse(business.getBusiness().isEmpty());
      assertFalse(business.getRoles().isEmpty());
      assertFalse(business.getOthers().isEmpty());
    } catch (DAOConnectionException e) {
      logger.error("Issue", e);
      fail(e.getMessage());
    } catch (DAONoDataException e) {
      logger.error("Issue", e);
      try {
        List<Business> list = businessDAO.getAll();
        logger.warn("All {}", list.size());
        for (Business business : list) {
          logger.warn("All HostConfig {} A: {} B: {} R: {} O: {}",
                      business.getHostid(), business.getAliases(),
                      business.getBusiness(), business.getRoles(),
                      business.getOthers());
        }
      } catch (DAOConnectionException daoConnectionException) {
        // ignore
      }
    } finally {
      if (businessDAO != null) {
        businessDAO.close();
      }
    }
  }

  @Test
  public void test1_JsonGenerator() {
    // Simple check of version
    logger.warn(Version.fullIdentifier());
    assertNotNull(Version.fullIdentifier());
    assertTrue(Version.artifactId().equals("WaarpR66"));
    final int nb = 11;
    logger.warn("Start Test Json");
    DbPreparedStatement preparedStatement;
    try {
      preparedStatement = DbTaskRunner
          .getFilterPrepareStatement(DbConstantR66.admin.getSession(), nb,
                                     false, null, null, null, null, null, null,
                                     false, false, false, false, true, null);
      preparedStatement.executeQuery();
      final String tasks = DbTaskRunner.getJson(preparedStatement, nb);
      preparedStatement.realClose();
      assertFalse("Json should not be empty", tasks.isEmpty());
    } catch (final WaarpDatabaseNoConnectionException e) {
      e.printStackTrace();
      fail("Json in Error");
    } catch (final WaarpDatabaseSqlException e) {
      e.printStackTrace();
      fail("Json in Error");
    } catch (final OpenR66ProtocolBusinessException e) {
      e.printStackTrace();
      fail("Json in Error");
    }
  }

  @Test
  public void test1_JsonGeneratorDbHostAuth() {
    final int nb = 11;
    logger.warn("Start Test Json");
    DbPreparedStatement preparedStatement;
    try {
      preparedStatement = DbHostAuth
          .getFilterPrepareStament(DbConstantR66.admin.getSession(), null,
                                   null);
      preparedStatement.executeQuery();
      final String hosts = DbHostAuth.getJson(preparedStatement, nb);
      preparedStatement.realClose();
      assertFalse("Json should not be empty", hosts.isEmpty());
    } catch (final WaarpDatabaseNoConnectionException e) {
      e.printStackTrace();
      assertTrue("Json in Error", false);
    } catch (final WaarpDatabaseSqlException e) {
      e.printStackTrace();
      assertTrue("Json in Error", false);
    } catch (final OpenR66ProtocolBusinessException e) {
      e.printStackTrace();
      assertTrue("Json in Error", false);
    }
  }

  @Test
  public void test1_JsonGeneratorDbRule() {
    final int nb = 11;
    logger.warn("Start Test Json");
    DbPreparedStatement preparedStatement;
    try {
      preparedStatement = DbRule
          .getFilterPrepareStament(DbConstantR66.admin.getSession(), null, -1);
      preparedStatement.executeQuery();
      final String rules = DbRule.getJson(preparedStatement, nb);
      preparedStatement.realClose();
      assertFalse("Json should not be empty", rules.isEmpty());
    } catch (final WaarpDatabaseNoConnectionException e) {
      e.printStackTrace();
      assertTrue("Json in Error", false);
    } catch (final WaarpDatabaseSqlException e) {
      e.printStackTrace();
      assertTrue("Json in Error", false);
    } catch (final OpenR66ProtocolBusinessException e) {
      e.printStackTrace();
      assertTrue("Json in Error", false);
    }
    logger.warn("End Test Json");
  }

  @Test
  public void test2_PingPongPacket() throws WaarpDatabaseException {
    final DbHostAuth host = new DbHostAuth("hosta");
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
  public void test2_ThriftClient() throws IOException {
    logger.warn("Start Test Thrift Client");
    final int PORT = 4266;
    final int tries = 1000;
    TTransport transport = null;
    try {
      final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
      transport = new TSocket("localhost", PORT);
      final TProtocol protocol = new TBinaryProtocol(transport);
      final R66Service.Client client = new R66Service.Client(protocol);
      transport.open();
      R66Request request = new R66Request(RequestMode.INFOFILE);
      request.setDestuid("hostas");
      request.setFromuid("hostas");
      request.setRule("rule3");
      request.setAction(Action.List);

      System.out.println("REQUEST1: " + request);
      List<String> list = client.infoListQuery(request);
      System.out.println("RESULT1: " + list.size());
      for (final String slist : list) {
        System.out.println(slist);
      }
      assertFalse("List should not be empty", list.isEmpty());
      final long start = System.currentTimeMillis();
      for (int i = 0; i < tries; i++) {
        list = client.infoListQuery(request);
      }
      final long end = System.currentTimeMillis();
      System.out.println(
          "Delay: " + (end - start) + " : " + (tries * 1000) / (end - start));

      request.setMode(RequestMode.INFOFILE);
      request.setAction(Action.Mlsx);
      list = client.infoListQuery(request);
      System.out.println("RESULT3: " + list.size());
      for (final String slist : list) {
        System.out.println(slist);
      }
      assertFalse("List should not be empty", list.isEmpty());

      request = new R66Request(RequestMode.ASYNCTRANSFER);
      request.setDestuid("hostas");
      request.setFromuid("hostas");
      request.setRule("rule3");
      request.setFile("testTask.txt");
      request.setInfo("Submitted from Thrift");
      System.out.println("REQUEST4: " + request);
      org.waarp.thrift.r66.R66Result result =
          client.transferRequestQuery(request);
      System.out.println("RESULT4: " + result);

      final long startEx = System.currentTimeMillis();
      boolean dontknow = false;
      final long tid = result.getTid();
      for (int i = 0; i < tries; i++) {
        dontknow = client.isStillRunning("hostas", "hostas", tid);
      }
      final long endEx = System.currentTimeMillis();
      System.out.println("StillRunning: " + dontknow);
      System.out.println("Delay: " + (endEx - startEx) + " : " +
                         (tries * 1000) / (endEx - startEx));

      request.setMode(RequestMode.INFOREQUEST);
      request.setTid(tid);
      request.setAction(Action.Detail);
      result = client.infoTransferQuery(request);
      System.out.println("RESULT2: " + result);
      final long startQu = System.currentTimeMillis();
      for (int i = 0; i < tries; i++) {
        result = client.infoTransferQuery(request);
      }
      final long endQu = System.currentTimeMillis();
      System.out.println("Delay: " + (endQu - startQu) + " : " +
                         (tries * 1000) / (endQu - startQu));

      System.out.println("Exist: " + client
          .isStillRunning(request.getFromuid(), request.getDestuid(),
                          request.getTid()));

      // Wrong request
      request = new R66Request(RequestMode.INFOFILE);

      System.out.println("WRONG REQUEST: " + request);
      list = client.infoListQuery(request);
      System.out.println("RESULT of Wrong Request: " + list.size());
      for (final String slist : list) {
        System.out.println(slist);
      }
      totest.delete();
    } catch (final TTransportException e) {
      e.printStackTrace();
      Assume.assumeNoException("Thrift in Error during connection", e);
    } catch (final TException e) {
      e.printStackTrace();
      assertTrue("Thrift in Error", false);
    } finally {
      if (transport != null) {
        transport.close();
      }
    }
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
        new TestProgressBarTransfer(future, "hostas", "testTaskBig.txt",
                                    "rule3", "Test Send Small ProgressBar",
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

    final int size = 100000000;
    final File totestBig =
        generateOutFile("/tmp/R66/out/testTaskBig.txt", size);
    future = new R66Future(true);
    transaction =
        new TestRecvThroughClient(future, handler, "hostas", "testTaskBig.txt",
                                  "rule6", "Test RecvThrough Big", true, 65536,
                                  networkTransaction);
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
      if (result.getRunner().shallIgnoreSave()) {
        // In case of success, delete the runner
        dbTaskRunners.add(result.getRunner());
      }
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

    final int size = 100000000;
    final File totestBig =
        generateOutFile("/tmp/R66/out/testTaskBig.txt", size);
    future = new R66Future(true);
    transaction =
        new TestSendThroughClient(future, "hostas", "testTaskBig.txt", "rule5",
                                  "Test SendThrough Big", true, 65536,
                                  networkTransaction);
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
    final TestRecvThroughHandler handler = new TestRecvThroughHandler();
    long timestart = System.currentTimeMillis();
    for (int i = 0; i < NUMBER_FILES; i++) {
      final R66Future future = new R66Future(true);
      futures.add(future);
      final TestRecvThroughClient transaction =
          new TestRecvThroughClient(future, handler, "hostas", "testTask.txt",
                                    "rule6", "Test Multiple RecvThrough", true,
                                    8192, networkTransaction);
      executorService.execute(transaction);
    }
    Thread.sleep(100);
    executorService.shutdown();
    for (int i = 0; i < NUMBER_FILES; i++) {
      final R66Future future = futures.remove(0);
      future.awaitOrInterruptible();
      assertTrue(future.isSuccess());
    }
    long timestop = System.currentTimeMillis();
    logger
        .warn("RecvThrough {} files from R2" + " ({} seconds,  {} per seconds)",
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

  @Test
  public void test5_DirectTransferMultipleBlockSize() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 1000000);
    final ExecutorService executorService = Executors.newCachedThreadPool();
    final int nb = 10;
    final R66Future[] arrayFuture = new R66Future[nb];
    logger.warn("Start Test of DirectTransfer");
    final long time1 = System.currentTimeMillis();
    for (int i = 0; i < nb; i++) {
      arrayFuture[i] = new R66Future(true);
      final TestTransferNoDb transaction =
          new TestTransferNoDb(arrayFuture[i], "hostas", "testTask.txt",
                               "rule3", "Test SendDirect Small", true,
                               8192 * (i + 1) * 2, DbConstantR66.ILLEGALVALUE,
                               networkTransaction);
      executorService.execute(transaction);
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
          new TestTransferNoDb(future, "hostas", "testTask.txt", "rule3",
                               "Test SendDirect Small", true, 8192,
                               DbConstantR66.ILLEGALVALUE, networkTransaction);
      transaction.run();
      future.awaitOrInterruptible();
      if (future.getRunner() != null) {
        dbTaskRunners.add(future.getRunner());
        specialId = future.getRunner().getSpecialId();
      }
      if (future.isSuccess()) {
        success++;
        logger.warn("Success for first transfer");
      } else {
        error++;
        fail("Should be able to find out the previous DbTaskRunner");
      }
    }
    // Now second transfer based on ID
    String[] args =
        { "falsConfigFile", "-id", "" + specialId, "-to", "hostas" };
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
          new SubmitTransfer(future2, "hostas", localFilename, rule,
                             "Test Send 2 Submit Small", true, 8192,
                             DbConstantR66.ILLEGALVALUE, newstart);
      logger.warn("Running second submit transfer");
      submitTransfer.run();
      logger.warn("Waiting second submit transfer");
      future2.awaitOrInterruptible();
      logger
          .warn("End wait for second submit transfer {}", future2.isSuccess());
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
    String[] args2 = { "falsConfigFile", "-id", "1", "-to", "hostas" };
    assertNull(SubmitTransferTest.getParamsInternal(1, args2));
    final long time2 = System.currentTimeMillis();
    logger.warn("Success: " + success + " Error: " + error + " NB/s: " +
                2 * success * 1000 / (time2 - time1));
    Thread.sleep(1000);
    totest.delete();
    Thread.sleep(1000);
    assertEquals("Success should be 2", 2, success);
    assertEquals("Errors should be 0", 0, error);
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

  @Test
  public void test5_DirectTransferWrong() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    final R66Future future = new R66Future(true);
    logger.warn("Start Test of DirectTransfer");
    final long time1 = System.currentTimeMillis();
    final TestTransferNoDb transaction =
        new TestTransferNoDb(future, "hostbs", "testTask.txt", "rule3",
                             "Test SendDirect Small", true, 8192,
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
    Thread.sleep(5000);
    totest.delete();
    Thread.sleep(3000);
    assertEquals("Success should be 0", 0, success);
    assertEquals("Errors should be 1", 1, error);
  }

  @Test
  public void test5_DirectTransferWrongHost() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    final R66Future future = new R66Future(true);
    logger.warn("Start Test of DirectTransfer");
    final long time1 = System.currentTimeMillis();
    final TestTransferNoDb transaction =
        new TestTransferNoDb(future, "hostunknowns", "testTask.txt", "rule3",
                             "Test SendDirect Small", true, 8192,
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
    Thread.sleep(5000);
    totest.delete();
    Thread.sleep(3000);
    assertEquals("Success should be 0", 0, success);
    assertEquals("Errors should be 1", 1, error);
  }

  @Test
  public void test5_DirectTransferPullWrong() throws Exception {
    final R66Future future = new R66Future(true);
    logger.warn("Start Test of DirectTransfer");
    final long time1 = System.currentTimeMillis();
    final TestTransferNoDb transaction =
        new TestTransferNoDb(future, "hostbs", "testTaskNew.txt", "rule4",
                             "Test RecvDirect Small", true, 8192,
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
    Thread.sleep(5000);
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
  }

  @Test
  public void test7_Spooled() throws IOException, InterruptedException {
    logger.warn("Start Test of Spooled Transfer");
    SpooledThread spooledThread = new SpooledThread();
    spooledThread.ignoreAlreadyUsed = false;
    spooledThread.submit = false;
    test_Spooled(spooledThread, 2);
    assertEquals(3, spooledThread.spooledDirectoryTransfer.getSent());
    assertEquals(0, spooledThread.spooledDirectoryTransfer.getError());
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
  }

  @Test
  public void test7_SpooledSubmit() throws IOException, InterruptedException {
    logger.warn("Start Test of Spooled Transfer");
    SpooledThread spooledThread = new SpooledThread();
    spooledThread.ignoreAlreadyUsed = false;
    spooledThread.submit = true;
    test_Spooled(spooledThread, 2);
    assertEquals(3, spooledThread.spooledDirectoryTransfer.getSent());
    assertEquals(0, spooledThread.spooledDirectoryTransfer.getError());
  }

  @Test
  public void test7_SpooledIgnore() throws IOException, InterruptedException {
    logger.warn("Start Test of Spooled Ignored Changed File Transfer");
    SpooledThread spooledThread = new SpooledThread();
    spooledThread.ignoreAlreadyUsed = true;
    spooledThread.submit = false;
    test_Spooled(spooledThread, 2);
    assertEquals(2, spooledThread.spooledDirectoryTransfer.getSent());
    assertEquals(0, spooledThread.spooledDirectoryTransfer.getError());
  }

  @Test
  public void test7_SpooledSubmitIgnore()
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
  public void test7_SpooledWrongHost()
      throws IOException, InterruptedException {
    logger.warn("Start Test of Spooled Retry Transfer");
    //WaarpLoggerFactory.setLogLevel(WaarpLogLevel.DEBUG);
    SpooledThread spooledThread = new SpooledThread();
    spooledThread.ignoreAlreadyUsed = false;
    spooledThread.submit = false;
    spooledThread.host = "hostbs";
    // spooledThread.waarpHost = null;
    test_Spooled(spooledThread, 1);
    assertEquals(0, spooledThread.spooledDirectoryTransfer.getSent());
    assertTrue(3 <= spooledThread.spooledDirectoryTransfer.getError());
  }

  @Test
  public void test7_SpooledSubmitWrongHost()
      throws IOException, InterruptedException {
    logger.warn("Start Test of Spooled Retry Transfer");
    SpooledThread spooledThread = new SpooledThread();
    spooledThread.ignoreAlreadyUsed = false;
    spooledThread.host = "hostbs";
    spooledThread.submit = true;
    test_Spooled(spooledThread, 2);
    assertEquals(3, spooledThread.spooledDirectoryTransfer.getSent());
    assertEquals(0, spooledThread.spooledDirectoryTransfer.getError());
  }

  @Test
  public void test7_SpooledWrongHostIgnore()
      throws IOException, InterruptedException {
    logger.warn("Start Test of Spooled Ignored Changed File Transfer");
    SpooledThread spooledThread = new SpooledThread();
    spooledThread.ignoreAlreadyUsed = true;
    spooledThread.host = "hostbs";
    spooledThread.submit = false;
    //spooledThread.waarpHost = null;
    test_Spooled(spooledThread, 1);
    assertEquals(0, spooledThread.spooledDirectoryTransfer.getSent());
    assertTrue(3 <= spooledThread.spooledDirectoryTransfer.getError());
  }

  @Test
  public void test7_SpooledSubmitWrongHostIgnore()
      throws IOException, InterruptedException {
    logger.warn("Start Test of Spooled Ignored Changed File Transfer");
    SpooledThread spooledThread = new SpooledThread();
    spooledThread.ignoreAlreadyUsed = true;
    spooledThread.host = "hostbs";
    spooledThread.submit = true;
    test_Spooled(spooledThread, 2);
    assertEquals(2, spooledThread.spooledDirectoryTransfer.getSent());
    assertEquals(0, spooledThread.spooledDirectoryTransfer.getError());
  }

  @Test
  public void test50_Transfer_Delete_Rule_Wrong() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    final R66Future future = new R66Future(true);
    logger.warn("Start Test of DirectTransfer with future deleted Rule");
    String rulename = "todelete";
    DbRule newRule =
        new DbRule("todelete", null, 3, null, null, null, null, "", "", "", "",
                   "", "");
    newRule.insert();
    final TestTransferNoDb transaction =
        new TestTransferNoDb(future, "hostas", "testTask.txt", rulename,
                             "Test SendDirect Small", true, 8192,
                             DbConstantR66.ILLEGALVALUE, networkTransaction);
    transaction.run();
    int success = 0;
    int error = 0;
    future.awaitOrInterruptible();
    DbTaskRunner runner = null;
    if (future.getRunner() != null) {
      runner = future.getRunner();
    }
    if (future.isSuccess()) {
      success++;
    } else {
      error++;
    }
    logger.warn("Success: " + success + " Error: " + error);
    totest.delete();
    assertEquals("Success should be total", 1, success);
    assertEquals("Errors should be 0", 0, error);
    // Now try to delete the rule
    try {
      newRule.delete();
      fail(
          "Must raized an exception since a transfer exist while we try to delete the rule associated with it");
    } catch (WaarpDatabaseNoDataException e) {
      // Correct behavior
    }
    if (runner != null) {
      runner.delete();
      // This time, the delete shall be ok
      newRule.delete();
    }
  }

  @Test
  public void test51_Transfer_With_Icap_Send() throws Exception {
    IcapServerHandler.resetJunitStatus();
    IcapServer.main(new String[] { "127.0.0.1", "9999" });
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 100);
    final R66Future future = new R66Future(true);
    logger.warn("Start Test of DirectTransfer with Icap");
    final TestTransferNoDb transaction =
        new TestTransferNoDb(future, "hostas", "testTask.txt", "rule3icap",
                             "Test SendDirect Small", true, 8192,
                             DbConstantR66.ILLEGALVALUE, networkTransaction);
    transaction.run();
    int success = 0;
    int error = 0;
    future.awaitOrInterruptible();
    DbTaskRunner runner = null;
    if (future.getRunner() != null) {
      runner = future.getRunner();
    }
    if (future.isSuccess()) {
      success++;
    } else {
      error++;
    }
    logger.warn("Success: " + success + " Error: " + error);
    totest.delete();
    assertEquals("Success should be total", 1, success);
    assertEquals("Errors should be 0", 0, error);
    if (runner != null) {
      runner.delete();
    }
    IcapServer.shutdown();
    logger.debug("Server stopped");
  }

  @Test
  public void test52_Transfer_With_Icap_Send_Virus() throws Exception {
    IcapServerHandler.resetJunitStatus();
    IcapServer.main(new String[] { "127.0.0.1", "9999" });
    IcapServerHandler.setFinalStatus(200);
    try {
      final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 100);
      final R66Future future = new R66Future(true);
      logger.warn("Start Test of DirectTransfer with Icap");
      final TestTransferNoDb transaction =
          new TestTransferNoDb(future, "hostas", "testTask.txt", "rule3icap",
                               "Test SendDirect Small", true, 8192,
                               DbConstantR66.ILLEGALVALUE, networkTransaction);
      transaction.run();
      int success = 0;
      int error = 0;
      future.awaitOrInterruptible();
      DbTaskRunner runner = null;
      if (future.getRunner() != null) {
        runner = future.getRunner();
      }
      if (future.isSuccess()) {
        success++;
      } else {
        if (runner != null) {
          DbTaskRunner dbTaskRunner =
              new DbTaskRunner(runner.getSpecialId() + 1, runner.getRequester(),
                               runner.getRequested());
          logger.warn("Start wait for re-transfer: {}",
                      dbTaskRunner.toShortString());
          waitForAllDone(dbTaskRunner);
          DbTaskRunner checkedRunner =
              new DbTaskRunner(dbTaskRunner.getSpecialId(),
                               dbTaskRunner.getRequester(),
                               dbTaskRunner.getRequested());
          logger.warn("End wait for re-transfer: {}",
                      checkedRunner.toShortString());
          if (checkedRunner.isAllDone()) {
            success++;
          } else {
            error++;
          }
        }
        error++;
      }
      logger.warn("Success: " + success + " Error: " + error);
      assertEquals("Success should be total", 1, success);
      assertEquals("Errors should be 0", 1, error);
      totest.delete();
      if (runner != null) {
        runner.delete();
      }
    } finally {
      IcapServer.shutdown();
      logger.debug("Server stopped");
    }
  }

  @Test
  public void test53_Transfer_With_Icap_Recv() throws Exception {
    IcapServerHandler.resetJunitStatus();
    IcapServer.main(new String[] { "127.0.0.1", "9999" });
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 100);
    final R66Future future = new R66Future(true);
    logger.warn("Start Test of DirectTransfer with Icap");
    final TestTransferNoDb transaction =
        new TestTransferNoDb(future, "hostas", "testTask.txt", "rule4icap",
                             "Test RecvDirect Small", true, 8192,
                             DbConstantR66.ILLEGALVALUE, networkTransaction);
    transaction.run();
    int success = 0;
    int error = 0;
    future.awaitOrInterruptible();
    DbTaskRunner runner = null;
    if (future.getRunner() != null) {
      runner = future.getRunner();
    }
    if (future.isSuccess()) {
      success++;
    } else {
      error++;
    }
    logger.warn("Success: " + success + " Error: " + error);
    totest.delete();
    assertEquals("Success should be total", 1, success);
    assertEquals("Errors should be 0", 0, error);
    if (runner != null) {
      runner.delete();
    }
    IcapServer.shutdown();
    logger.debug("Server stopped");
  }

  @Test
  public void test54_Transfer_With_Icap_Recv_Virus() throws Exception {
    IcapServerHandler.resetJunitStatus();
    IcapServer.main(new String[] { "127.0.0.1", "9999" });
    IcapServerHandler.setFinalStatus(200);
    try {
      final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 100);
      final R66Future future = new R66Future(true);
      logger.warn("Start Test of DirectTransfer with Icap");
      final TestTransferNoDb transaction =
          new TestTransferNoDb(future, "hostas", "testTask.txt", "rule4icap",
                               "Test RecvDirect Small", true, 8192,
                               DbConstantR66.ILLEGALVALUE, networkTransaction);
      transaction.run();
      int success = 0;
      int error = 0;
      future.awaitOrInterruptible();
      DbTaskRunner runner = null;
      if (future.getRunner() != null) {
        runner = future.getRunner();
      }
      if (future.isSuccess()) {
        success++;
      } else {
        if (runner != null) {
          DbTaskRunner dbTaskRunner =
              new DbTaskRunner(runner.getSpecialId() + 1, runner.getRequester(),
                               runner.getRequested());
          logger.warn("Start wait for re-transfer: {}",
                      dbTaskRunner.toShortString());
          waitForAllDone(dbTaskRunner);
          DbTaskRunner checkedRunner =
              new DbTaskRunner(dbTaskRunner.getSpecialId(),
                               dbTaskRunner.getRequester(),
                               dbTaskRunner.getRequested());
          logger.warn("End wait for re-transfer: {}",
                      checkedRunner.toShortString());
          if (checkedRunner.isAllDone()) {
            success++;
          } else {
            error++;
          }
        }
        error++;
      }
      logger.warn("Success: " + success + " Error: " + error);
      assertEquals("Success should be total", 1, success);
      assertEquals("Errors should be 0", 1, error);
      totest.delete();
      if (runner != null) {
        runner.delete();
      }
    } finally {
      IcapServer.shutdown();
      logger.debug("Server stopped");
    }
  }

  @Ignore("Information not OK at the end")
  @Test
  public void test80_JsonCommandsBusiness() throws Exception {
    final DbHostAuth host = new DbHostAuth("hosta");
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

  @Ignore("Information not OK at the end")
  @Test
  public void test80_JsonCommandsTest() throws Exception {
    final DbHostAuth host = new DbHostAuth("hosta");
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
  public void test81_JsonCommandsLogs() throws Exception {
    final DbHostAuth host = new DbHostAuth("hosta");
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }

    final byte scode = -1;
    AbstractLocalPacket valid = null;

    logger.warn("Log");
    R66Future future = new R66Future(true);
    final LogJsonPacket node6 = new LogJsonPacket();
    node6.setClean(true);
    node6.setPurge(false);
    valid = new JsonCommandPacket(node6, LocalPacketFactory.LOGPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, false);
    logger.warn("Log2");
    future = new R66Future(true);
    node6.setPurge(true);
    valid = new JsonCommandPacket(node6, LocalPacketFactory.LOGPURGEPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, false);

  }

  @Test
  public void test81_JsonCommandsWithTransferId() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    final DbHostAuth host = new DbHostAuth("hostas");
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
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
    final long id = futureTransfer.getResult().getRunner().getSpecialId();
    logger.warn("Remote Task: {}",
                futureTransfer.getResult().getRunner().getSpecialId());

    final byte scode = -1;

    R66Future future;
    AbstractLocalPacket valid;

    logger.warn("Information");
    future = new R66Future(true);
    InformationJsonPacket node4 = new InformationJsonPacket(id, true, "hostas");
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
    node5.setRequested("hostas");
    node5.setRequester("hostas");
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
  public void test82_JsonCommandsOthers() throws Exception {
    final DbHostAuth host = new DbHostAuth("hosta");
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }

    final byte scode = -1;

    logger.warn("BandwidthJsonPacket");
    R66Future future = new R66Future(true);
    AbstractLocalPacket valid = null;
    BandwidthJsonPacket node = new BandwidthJsonPacket();
    // will ask current values instead
    node.setSetter(false);
    valid = new JsonCommandPacket(node, LocalPacketFactory.BANDWIDTHPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, false);

    logger.warn("BandwidthUnlimit");
    future = new R66Future(true);
    node = new BandwidthJsonPacket();
    // will ask current values instead
    node.setSetter(true);
    node.setWriteglobal(1000000000);
    node.setReadglobal(1000000000);
    node.setWritesession(1000000000);
    node.setReadsession(1000000000);
    valid = new JsonCommandPacket(node, LocalPacketFactory.BANDWIDTHPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, false);

    logger.warn("Config Export");
    future = new R66Future(true);
    final ConfigExportJsonPacket node7 = new ConfigExportJsonPacket();
    node7.setHost(true);
    node7.setAlias(true);
    node7.setBusiness(true);
    node7.setRoles(true);
    node7.setRule(true);
    valid = new JsonCommandPacket(node7, LocalPacketFactory.CONFEXPORTPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, false);

    logger.warn("Config Import");
    future = new R66Future(true);
    final ConfigImportJsonPacket node8 = new ConfigImportJsonPacket();
    node8.setHost("/tmp/R66/arch/hosta_Authentications.xml");
    node8.setRule("/tmp/R66/arch/hosta.rules.xml");
    valid = new JsonCommandPacket(node8, LocalPacketFactory.CONFIMPORTPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, false);

    logger.warn("BandwidthUnlimit 2");
    future = new R66Future(true);
    node = new BandwidthJsonPacket();
    // will ask current values instead
    node.setSetter(true);
    node.setWriteglobal(1000000000);
    node.setReadglobal(1000000000);
    node.setWritesession(1000000000);
    node.setReadsession(1000000000);
    valid = new JsonCommandPacket(node, LocalPacketFactory.BANDWIDTHPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, false);

    logger.warn("Block only");
    future = new R66Future(true);
    final ShutdownOrBlockJsonPacket node2 = new ShutdownOrBlockJsonPacket();
    node2.setRestartOrBlock(false);
    node2.setShutdownOrBlock(false);
    node2.setKey(FilesystemBasedDigest.passwdCrypt(
        Configuration.configuration.getServerAdminKey()));
    valid = new JsonCommandPacket(node2, LocalPacketFactory.BLOCKREQUESTPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, false);

  }

  @Test
  public void test83_BusinessRequest() throws WaarpDatabaseException {
    logger.warn("Start Test of BusinessRequest");
    final DbHostAuth host = new DbHostAuth("hostas");
    assertEquals("Error should be at 0", 0,
                 TestBusinessRequest.runTest(networkTransaction, host, 3));
  }

  @Test
  public void test91_ExtraCommandsInformationPacket() throws Exception {
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    setUpBeforeClassClient("config-clientB.xml");
    final DbHostAuth host = new DbHostAuth("hostas");
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
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
    sendInformation(informationPacket, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.INFORMATION, true);

    logger.warn("Start Extra commands: ASKLIST");
    future = new R66Future(true);
    scode = (byte) InformationPacket.ASKENUM.ASKLIST.ordinal();
    informationPacket = new InformationPacket("rule3", scode, "testTask.txt");
    sendInformation(informationPacket, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.INFORMATION, true);

    logger.warn("Start Extra commands: ASKMLSDETAIL");
    future = new R66Future(true);
    scode = (byte) InformationPacket.ASKENUM.ASKMLSDETAIL.ordinal();
    informationPacket = new InformationPacket("rule3", scode, "testTask.txt");
    sendInformation(informationPacket, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.INFORMATION, true);

    logger.warn("Start Extra commands: ASKMLSLIST");
    future = new R66Future(true);
    scode = (byte) InformationPacket.ASKENUM.ASKMLSLIST.ordinal();
    informationPacket = new InformationPacket("rule3", scode, "testTask.txt");
    sendInformation(informationPacket, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.INFORMATION, true);

    // ValidPacket BANDWIDTH
    logger.warn("Start Extra commands: BANDWIDTHPACKET)");
    future = new R66Future(true);
    final ValidPacket valid =
        new ValidPacket("-1", "-1", LocalPacketFactory.BANDWIDTHPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, true);

    setUpBeforeClassClient(CONFIG_CLIENT_A_XML);
    totest.delete();
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

    totest.delete();
  }

  @Test
  public void test92_ExtraCommandsOther() throws Exception {
    setUpBeforeClassClient("config-clientB.xml");
    ValidPacket valid;
    final byte scode = -1;
    final DbHostAuth host = new DbHostAuth("hostas");
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
        new Message(networkTransaction, future, "hostas", packet);
    message.run();
    future.awaitOrInterruptible();
    assertTrue("Message should be OK", future.isSuccess());

    // MultipleSubmitTransfer
    logger.warn("Start MultipleSubmitTransfer");
    future = new R66Future(true);
    File file = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    final MultipleSubmitTransfer multipleSubmitTransfer =
        new MultipleSubmitTransfer(future, "hostas",
                                   "/tmp/R66/out/testTask.txt", "rule3",
                                   "Multiple Submit", true, 1024,
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

    // ValidPacket CONFIMPORTPACKET
    logger.warn("Start Valid CONFIMPORTPACKET");
    future = new R66Future(true);
    valid = new ValidPacket("0 /tmp/R66/arch/hostb_Authentications.xml",
                            "0 /tmp/R66/arch/hostb.rules.xml",
                            LocalPacketFactory.CONFIMPORTPACKET);
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

    setUpBeforeClassClient(CONFIG_CLIENT_A_XML);
  }

  @Test
  public void test93_ExtraCommandsOnTransfer() throws Exception {
    setUpBeforeClassClient("config-clientB.xml");
    ValidPacket valid;
    final byte scode = -1;
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
    final DbHostAuth host = new DbHostAuth("hostas");
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
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
    final long id = futureTransfer.getRunner().getSpecialId();
    logger.warn("Remote Task: {}", id);

    // RequestTransfer
    logger.warn("Start RequestTransfer check");
    R66Future future = new R66Future(true);
    RequestTransfer requestTransfer =
        new RequestTransfer(future, id, "hostbs", "hostbs", false, false, false,
                            networkTransaction);
    requestTransfer.run();
    future.awaitOrInterruptible();
    assertTrue("Request of Transfer should be OK", future.isSuccess());

    // RequestTransfer
    logger.warn("Start RequestTransfer stop");
    future = new R66Future(true);
    requestTransfer =
        new RequestTransfer(future, id, "hostas", "hostas", false, true, false,
                            networkTransaction);
    requestTransfer.run();
    future.awaitOrInterruptible();
    assertFalse("Request of Transfer should be KO since done",
                future.isSuccess());

    // RequestTransfer
    logger.warn("Start RequestTransfer cancel");
    future = new R66Future(true);
    requestTransfer =
        new RequestTransfer(future, id, "hostas", "hostas", true, false, false,
                            networkTransaction);
    requestTransfer.run();
    future.awaitOrInterruptible();
    assertFalse("Request of Transfer should be KO since done",
                future.isSuccess());

    // RequestInformation
    logger.warn("Start RequestInformation");
    future = new R66Future(true);
    RequestInformation requestInformation =
        new RequestInformation(future, "hostas", "rule3", "testTask.txt", scode,
                               id, false, networkTransaction);
    requestInformation.run();
    future.awaitOrInterruptible();
    assertTrue("Request of information should be OK", future.isSuccess());

    // ValidPacket VALID
    logger.warn("Start Valid VALID");
    future = new R66Future(true);
    valid = new ValidPacket(null, "hostas hostas " + id,
                            LocalPacketFactory.VALIDPACKET);
    sendInformation(valid, socketServerAddress, future, scode, false,
                    R66FiniteDualStates.VALIDOTHER, true);

    // ValidPacket STOP
    logger.warn("Start Valid STOP");
    future = new R66Future(true);
    valid = new ValidPacket(null, "hostas hostas " + id,
                            LocalPacketFactory.STOPPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, true);

    // ValidPacket CANCEL
    logger.warn("Start Valid CANCEL");
    future = new R66Future(true);
    valid = new ValidPacket(null, "hostas hostas " + id,
                            LocalPacketFactory.CANCELPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, true);

    setUpBeforeClassClient(CONFIG_CLIENT_A_XML);
    totest.delete();
  }

  @Test
  public void test96_Tasks() throws Exception {
    System.err.println("Start Tasks");
    final File totest = new File("/tmp/R66/in/testTask.txt");
    final FileWriter fileWriter = new FileWriter(totest);
    fileWriter.write("Test content");
    fileWriter.flush();
    fileWriter.close();
    TestTasks.main(new String[] {
        new File(dirResources, CONFIG_SERVER_A_MINIMAL_XML).getAbsolutePath(),
        "/tmp/R66/in", "/tmp/R66/out", totest.getName()
    });
    System.err.println("End Tasks");
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


  static class SubmitTransferTest extends SubmitTransfer {

    public SubmitTransferTest(final R66Future future, final String remoteHost,
                              final String filename, final String rulename,
                              final String fileinfo, final boolean isMD5,
                              final int blocksize, final long id,
                              final Timestamp starttime) {
      super(future, remoteHost, filename, rulename, fileinfo, isMD5, blocksize,
            id, starttime);
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
      arguments.setLogWarn(true);
      arguments.setIgnoreAlreadyUsed(ignoreAlreadyUsed);

      spooledDirectoryTransfer =
          new SpooledDirectoryTransfer(future, arguments, networkTransaction);
      spooledDirectoryTransfer.run();
    }
  }
}
