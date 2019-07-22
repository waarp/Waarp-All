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

/**
 *
 */
package org.waarp.openr66.protocol.junit;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.openr66.client.Message;
import org.waarp.openr66.client.MultipleDirectTransfer;
import org.waarp.openr66.client.MultipleSubmitTransfer;
import org.waarp.openr66.client.RequestInformation;
import org.waarp.openr66.client.RequestTransfer;
import org.waarp.openr66.client.SubmitTransfer;
import org.waarp.openr66.client.utils.OutputFormat.FIELDS;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.task.test.TestExecJavaTask;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.http.rest.test.HttpTestRestR66Client;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
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
import org.waarp.openr66.protocol.test.TestTransaction;
import org.waarp.openr66.protocol.test.TestTransferNoDb;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.thrift.r66.Action;
import org.waarp.thrift.r66.R66Request;
import org.waarp.thrift.r66.R66Service;
import org.waarp.thrift.r66.RequestMode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketAddress;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

/**
 *
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NetworkClientTest extends TestAbstract {

  private static final int nbThread = 10;
  private static final ArrayList<DbTaskRunner> dbTaskRunners =
      new ArrayList<DbTaskRunner>();

  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    setUpBeforeClassMinimal("Linux/config/config-serverInitB.xml");
    setUpDbBeforeClass();
    // setUpBeforeClassServer("Linux/config/config-serverInitB.xml", "config-serverB.xml", false);
    setUpBeforeClassServer("Linux/config/config-serverInitA.xml",
                           "config-serverA-minimal.xml", true);
    setUpBeforeClassClient("config-clientA.xml");
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    Thread.sleep(1000);
    for (final DbTaskRunner dbTaskRunner : dbTaskRunners) {
      try {
        dbTaskRunner.delete();
      } catch (final WaarpDatabaseException e) {
        logger.warn("Cannot apply nolog to " + dbTaskRunner.toString(), e);
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
    Configuration.configuration.setTIMEOUTCON(100);
    final R66Future future = new R66Future(true);
    final ShutdownOrBlockJsonPacket node8 = new ShutdownOrBlockJsonPacket();
    node8.setRestartOrBlock(false);
    node8.setShutdownOrBlock(true);
    node8.setKey(FilesystemBasedDigest.passwdCrypt(
        Configuration.configuration.getSERVERADMINKEY()));
    final AbstractLocalPacket valid =
        new JsonCommandPacket(node8, LocalPacketFactory.BLOCKREQUESTPACKET);
    sendInformation(valid, socketServerAddress, future, scode, false,
                    R66FiniteDualStates.SHUTDOWN, true);
    Thread.sleep(1000);

    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    tearDownAfterClassServer();
  }

  private static void sendInformation(AbstractLocalPacket informationPacket,
                                      final SocketAddress socketServerAddress,
                                      R66Future future, byte scode,
                                      boolean check, R66FiniteDualStates state,
                                      boolean isSsl)
      throws OpenR66ProtocolPacketException {
    logger.warn("Start connection for Extra commands {} \n\t{}",
                informationPacket.getClass().getSimpleName(),
                informationPacket);
    final LocalChannelReference localChannelReference = networkTransaction
        .createConnectionWithRetry(socketServerAddress, isSsl, future);
    if (localChannelReference == null) {
      if (state != R66FiniteDualStates.SHUTDOWN) {
        assertTrue("Connection not OK", false);
      } else {
        return;
      }
    }
    localChannelReference.sessionNewState(state);
    logger.warn("Send {}", informationPacket);
    ChannelUtils
        .writeAbstractLocalPacket(localChannelReference, informationPacket,
                                  false);
    if (informationPacket instanceof KeepAlivePacket ||
        informationPacket instanceof NoOpPacket) {
      // do no await
      localChannelReference.close();
      return;
    } else {
      localChannelReference.getFutureRequest().awaitOrInterruptible();
      future.awaitOrInterruptible();
      if (! (localChannelReference.getFutureRequest().isDone() && future.isDone())) {
        fail("Cannot send information");
      }
    }
    final R66Result r66result = future.getResult();
    if (state == R66FiniteDualStates.VALIDOTHER) {
      logger.warn("feedback: {}", r66result);
    } else {
      final ValidPacket info = (ValidPacket) r66result.getOther();
      if (info != null && scode != -1) {
        logger.warn("nb: {}", Integer.parseInt(info.getSmiddle()));
        final String[] files = info.getSheader().split("\n");
        int i = 0;
        for (final String file : files) {
          i++;
          logger.warn("file: {} {}", i, file);
        }
      } else {
        if (info != null && info.getSheader() != null) {
          try {
            final DbTaskRunner runner =
                DbTaskRunner.fromStringXml(info.getSheader(), false);
            logger.warn("{}", runner.getJson());
          } catch (final OpenR66ProtocolBusinessException e) {
            logger.warn("{}: {}", FIELDS.transfer.name(), info.getSheader());
          }
        }
      }
    }
    localChannelReference.close();
    if (check) {
      assertTrue("Information not OK", future.isSuccess());
    } else {
      logger.warn("Get {} {}", future.isSuccess(), future.isSuccess()? "OK" :
          future.getCause() != null? future.getCause().getMessage() :
              "No Cause");
    }
  }

  @Test
  public void test1_PingPongPacket() throws WaarpDatabaseException {
    final DbHostAuth host = new DbHostAuth("hosta");
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }
    final ExecutorService executorService = Executors.newCachedThreadPool();
    final int nb = nbThread;
    final R66Future[] arrayFuture = new R66Future[nb];
    logger.warn("Start Test of Transaction");
    final long time1 = System.currentTimeMillis();
    for (int i = 0; i < nb; i++) {
      arrayFuture[i] = new R66Future(true);
      final TestPacket packet = new TestPacket("Test", "" + i, 0);
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
                success * TestPacket.pingpong * 1000 / (time2 - time1));
    executorService.shutdown();
    assertEquals("Success should be total", nb, success);
    assertEquals("Errors should be 0", 0, error);
  }

  @Test
  public void test83_BusinessRequest() throws WaarpDatabaseException {
    logger.warn("Start Test of BusinessRequest");
    final DbHostAuth host = new DbHostAuth("hostas");
    assertEquals("Error should be at 0", 0,
                 TestBusinessRequest.runTest(networkTransaction, host, 3));
  }

  @Test
  public void test4_RecvThroughClient() throws IOException {
    logger.warn("Start Test of Recv Through Transfer");
    final File totest = generateOutFile("/tmp/R66/out/testTask.txt", 10);
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

  private File generateOutFile(String name, int size) throws IOException {
    final File file = new File(name);
    final FileWriter fileWriterBig = new FileWriter(file);
    for (int i = 0; i < size / 10; i++) {
      fileWriterBig.write("0123456789");
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    return file;
  }

  private void checkFinalResult(R66Future future, R66Result result, long delay,
                                long size) {
    if (future.isSuccess()) {
      if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
        logger.warn("Warning with Id: " + result.getRunner().getSpecialId() +
                    " on file: " +
                    (result.getFile() != null? result.getFile().toString() :
                        "no file") + " delay: " + delay + " kbps: " +
                    (size * 8 / delay));
      } else {
        logger.warn("Success with Id: " + result.getRunner().getSpecialId() +
                    " on Final file: " +
                    (result.getFile() != null? result.getFile().toString() :
                        "no file") + " delay: " + delay + " kbps: " +
                    (size * 8 / delay));
      }
      if (result.getRunner().shallIgnoreSave()) {
        // In case of success, delete the runner
        dbTaskRunners.add(result.getRunner());
      }
    } else {
      if (result == null || result.getRunner() == null) {
        logger.warn("Transfer in Error with no Id", future.getCause());
        assertEquals("Result should not be null, neither runner", true, false);
      }
      if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
        logger.warn(
            "Transfer in Warning with Id: " + result.getRunner().getSpecialId(),
            future.getCause());
        assertEquals("Transfer in Warning", true, false);
      } else {
        logger.error(
            "Transfer in Error with Id: " + result.getRunner().getSpecialId(),
            future.getCause());
        assertEquals("Transfer in Error", true, false);
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
                                    true, 65536, DbConstant.ILLEGALVALUE,
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
                             8192, DbConstant.ILLEGALVALUE, networkTransaction);
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
                             8192, DbConstant.ILLEGALVALUE, networkTransaction);
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
  public void test5_DirectTransfer()
      throws WaarpDatabaseException, IOException {
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
                               DbConstant.ILLEGALVALUE, networkTransaction);
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
  public void test5_SubmitTransfer() throws IOException, InterruptedException {
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
                             DbConstant.ILLEGALVALUE, newstart);
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
        final DbTaskRunner runner = arrayFuture[i].getRunner();
        if (runner != null) {
          waitForAllDone(runner);
          dbTaskRunners.add(runner);
        }
      }
    }
    Thread.sleep(100);
    totest.delete();
  }

  private void waitForAllDone(DbTaskRunner runner) {
    while (true) {
      final R66Future futureInfo = new R66Future(true);
      final RequestInformation requestInformation =
          new RequestInformation(futureInfo, "hostas", null, null, (byte) -1,
                                 runner.getSpecialId(), true,
                                 networkTransaction);
      requestInformation.run();
      futureInfo.awaitOrInterruptible();
      if (futureInfo.isSuccess()) {
        final R66Result r66result = futureInfo.getResult();
        final ValidPacket info = (ValidPacket) r66result.getOther();
        final String xml = info.getSheader();
        try {
          runner = DbTaskRunner.fromStringXml(xml, true);
          logger.info("Get Runner from remote: {}", runner);
          if (runner.getSpecialId() == DbConstant.ILLEGALVALUE ||
              !runner.isSender()) {
            logger
                .error(Messages.getString("RequestTransfer.18")); //$NON-NLS-1$
            break;
          }
          if (runner.isAllDone()) {
            break;
          }
        } catch (final OpenR66ProtocolBusinessException e1) {
          logger.error(Messages.getString("RequestTransfer.18")); //$NON-NLS-1$
          break;
        }
      } else {
        logger.error(Messages.getString("RequestTransfer.18")); //$NON-NLS-1$
        break;
      }
    }
  }

  @Test
  public void test1_JsonGenerator() {
    final int nb = 11;
    logger.warn("Start Test Json");
    DbPreparedStatement preparedStatement;
    try {
      preparedStatement = DbTaskRunner
          .getFilterPrepareStatement(DbConstant.admin.getSession(), nb, false,
                                     null, null, null, null, null, null, false,
                                     false, false, false, true, null);
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
          .getFilterPrepareStament(DbConstant.admin.getSession(), null, null);
      preparedStatement.executeQuery();
      final String hosts = DbHostAuth.getJson(preparedStatement, nb);
      preparedStatement.realClose();
      assertFalse("Json should not be empty", hosts.isEmpty());
    } catch (final WaarpDatabaseNoConnectionException e) {
      e.printStackTrace();
      assertEquals("Json in Error", true, false);
    } catch (final WaarpDatabaseSqlException e) {
      e.printStackTrace();
      assertEquals("Json in Error", true, false);
    } catch (final OpenR66ProtocolBusinessException e) {
      e.printStackTrace();
      assertEquals("Json in Error", true, false);
    }
  }

  @Test
  public void test1_JsonGeneratorDbRule() {
    final int nb = 11;
    logger.warn("Start Test Json");
    DbPreparedStatement preparedStatement;
    try {
      preparedStatement = DbRule
          .getFilterPrepareStament(DbConstant.admin.getSession(), null, -1);
      preparedStatement.executeQuery();
      final String rules = DbRule.getJson(preparedStatement, nb);
      preparedStatement.realClose();
      assertFalse("Json should not be empty", rules.isEmpty());
    } catch (final WaarpDatabaseNoConnectionException e) {
      e.printStackTrace();
      assertEquals("Json in Error", true, false);
    } catch (final WaarpDatabaseSqlException e) {
      e.printStackTrace();
      assertEquals("Json in Error", true, false);
    } catch (final OpenR66ProtocolBusinessException e) {
      e.printStackTrace();
      assertEquals("Json in Error", true, false);
    }
    logger.warn("End Test Json");
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
                             DbConstant.ILLEGALVALUE, networkTransaction);
    transaction.run();
    futureTransfer.awaitOrInterruptible();
    assertTrue("File transfer not ok", futureTransfer.isSuccess());
    final long id = futureTransfer.getRunner().getSpecialId();
    logger.warn("Remote Task: {}", id);

    // InformationPacket
    logger.warn("Start Extra commands: InformationPacket");
    byte scode = -1;
    InformationPacket informationPacket =
        new InformationPacket("" + id, scode, "0");
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

    setUpBeforeClassClient("config-clientA.xml");
    totest.delete();
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
                             DbConstant.ILLEGALVALUE, networkTransaction);
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
    assertFalse("Request of Transfer should be OK", future.isSuccess());

    // RequestTransfer
    logger.warn("Start RequestTransfer cancel");
    future = new R66Future(true);
    requestTransfer =
        new RequestTransfer(future, id, "hostas", "hostas", true, false, false,
                            networkTransaction);
    requestTransfer.run();
    future.awaitOrInterruptible();
    assertFalse("Request of Transfer should be OK", future.isSuccess());

    // RequestInformation
    logger.warn("Start RequestInformation");
    future = new R66Future(true);
    final RequestInformation requestInformation =
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

    setUpBeforeClassClient("config-clientA.xml");
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
    final MultipleSubmitTransfer multipleSubmitTransfer =
        new MultipleSubmitTransfer(future, "hostas", "testTask.txt", "rule3",
                                   "Multiple Submit", true, 1024,
                                   DbConstant.ILLEGALVALUE, null,
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
    final long timeout = Configuration.configuration.getTIMEOUTCON();
    Configuration.configuration.setTIMEOUTCON(100);
    future = new R66Future(true);
    final NoOpPacket noOpPacket = new NoOpPacket();
    sendInformation(noOpPacket, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.INFORMATION, true);

    // KeepAlivePacket
    logger.warn("Start KeepAlive");
    future = new R66Future(true);
    final KeepAlivePacket keepAlivePacket = new KeepAlivePacket();
    sendInformation(keepAlivePacket, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.INFORMATION, true);
    Configuration.configuration.setTIMEOUTCON(timeout);

    setUpBeforeClassClient("config-clientA.xml");
  }

  @Test
  public void test91_MultipleDirectTransfer()
      throws WaarpDatabaseException, IOException, InterruptedException,
             OpenR66ProtocolPacketException {
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
                             DbConstant.ILLEGALVALUE, networkTransaction);
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
                                   DbConstant.ILLEGALVALUE, networkTransaction);
    multipleDirectTransfer.run();
    future.awaitOrInterruptible();
    assertTrue("All sends should be OK", future.isSuccess());

    totest.delete();
  }

  @Test
  public void test81_JsonCommandsLogs()
      throws IOException, WaarpDatabaseException, InterruptedException,
             OpenR66ProtocolPacketException {
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
  public void test82_JsonCommandsOthers()
      throws IOException, WaarpDatabaseException, InterruptedException,
             OpenR66ProtocolPacketException {
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
        Configuration.configuration.getSERVERADMINKEY()));
    valid = new JsonCommandPacket(node2, LocalPacketFactory.BLOCKREQUESTPACKET);
    sendInformation(valid, socketServerAddress, future, scode, true,
                    R66FiniteDualStates.VALIDOTHER, false);

  }

  @Ignore("Information not OK at the end")
  @Test
  public void test80_JsonCommandsBusiness()
      throws IOException, WaarpDatabaseException, InterruptedException,
             OpenR66ProtocolPacketException {
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
  public void test80_JsonCommandsTest()
      throws IOException, WaarpDatabaseException, InterruptedException,
             OpenR66ProtocolPacketException {
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
  public void test81_JsonCommandsWithTransferId()
      throws IOException, WaarpDatabaseException, InterruptedException,
             OpenR66ProtocolPacketException {
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
                             DbConstant.ILLEGALVALUE, networkTransaction);
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
  public void test1_ThriftClient() throws IOException {
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

      System.out.println("REQUEST1: " + request.toString());
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
          "Delay: " + (end - start) + " : " + ((tries * 1000) / (end - start)));

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
      System.out.println("REQUEST4: " + request.toString());
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
                         ((tries * 1000) / (endEx - startEx)));

      request.setMode(RequestMode.INFOREQUEST);
      request.setTid(tid);
      request.setAction(Action.Detail);
      result = client.infoTransferQuery(request);
      System.out.println("RESULT2: " + result.toString());
      final long startQu = System.currentTimeMillis();
      for (int i = 0; i < tries; i++) {
        result = client.infoTransferQuery(request);
      }
      final long endQu = System.currentTimeMillis();
      System.out.println("Delay: " + (endQu - startQu) + " : " +
                         ((tries * 1000) / (endQu - startQu)));

      System.out.println("Exist: " + client
          .isStillRunning(request.getFromuid(), request.getDestuid(),
                          request.getTid()));

      // Wrong request
      request = new R66Request(RequestMode.INFOFILE);

      System.out.println("WRONG REQUEST: " + request.toString());
      list = client.infoListQuery(request);
      System.out.println("RESULT of Wrong Request: " + list.size());
      for (final String slist : list) {
        System.out.println(slist);
      }
      totest.delete();
    } catch (final TTransportException e) {
      e.printStackTrace();
      assertEquals("Thrift in Error", true, false);
    } catch (final TException e) {
      e.printStackTrace();
      assertEquals("Thrift in Error", true, false);
    }
    transport.close();
  }

  @Ignore("Issue on checkBaseAuthent")
  @Test
  public void test90_RestR66() throws Exception {
    HttpTestRestR66Client.keydesfilename =
        new File(dir, "certs/test-key.des").getAbsolutePath();
    logger.info("Key filename: {}", HttpTestRestR66Client.keydesfilename);
    HttpTestRestR66Client.main(new String[] { "1" });
  }
}
