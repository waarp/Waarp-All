/*******************************************************************************
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 *  individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at your
 *  option) any later version.
 *
 *  Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 *  A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  Waarp . If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.waarp.openr66.protocol.test;

import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.openr66.client.AbstractBusinessRequest;
import org.waarp.openr66.client.BusinessRequest;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.task.test.TestExecJavaTask;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.localhandler.packet.BusinessRequestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.R66Future;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Test class for internal Business test
 *
 * @author Frederic Bregier
 */
public class TestBusinessRequest extends AbstractBusinessRequest {
  /**
   * Internal Logger
   */
  private static WaarpLogger logger;

  public TestBusinessRequest(NetworkTransaction networkTransaction,
                             R66Future future, String remoteHost,
                             BusinessRequestPacket packet) {
    super(TestBusinessRequest.class, future, remoteHost, networkTransaction,
          packet);
  }

  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(
        WaarpLogLevel.WARN));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(TestBusinessRequest.class);
    }
    if (args.length < 1) {
      logger
          .error("Needs at least the configuration file as first argument");
      return;
    }
    if (!FileBasedConfiguration
        .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
      logger
          .error("Needs a correct configuration file as first argument");
      return;
    }
    Configuration.configuration.pipelineInit();

    NetworkTransaction networkTransaction = new NetworkTransaction();
    DbHostAuth host = Configuration.configuration.getHOST_AUTH();
    runTest(networkTransaction, host, 1);
    networkTransaction.closeAll();
  }

  public static int runTest(NetworkTransaction networkTransaction,
                            DbHostAuth host, int nbToDo) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(TestBusinessRequest.class);
    }
    ExecutorService executorService = Executors.newCachedThreadPool();
    int nb = nbToDo;

    R66Future[] arrayFuture = new R66Future[nb];
    logger.info("Start Test of Transaction");
    long time1 = System.currentTimeMillis();
    for (int i = 0; i < nb; i++) {
      arrayFuture[i] = new R66Future(true);
      BusinessRequestPacket packet = new BusinessRequestPacket(
          TestExecJavaTask.class.getName() +
          " business 0 simple business request", 0);
      TestBusinessRequest transaction = new TestBusinessRequest(
          networkTransaction, arrayFuture[i], host.getHostid(),
          packet);
      executorService.execute(transaction);
    }
    int success = 0;
    int error = 0;
    for (int i = 0; i < nb; i++) {
      arrayFuture[i].awaitUninterruptibly();
      if (arrayFuture[i].isSuccess()) {
        success++;
      } else {
        error++;
      }
    }
    long time2 = System.currentTimeMillis();
    logger.warn(
        "Simple TestExecJavaTask Success: " + success + " Error: " + error +
        " NB/s: " +
        success * 100 * 1000 / (time2 - time1));
    R66Future future = new R66Future(true);
    classname = BusinessRequest.DEFAULT_CLASS;
    BusinessRequestPacket packet =
        new BusinessRequestPacket(classname + " LOG warn some information 1",
                                  0);
    BusinessRequest transaction = new BusinessRequest(
        networkTransaction, future, host.getHostid(), packet);
    transaction.run();
    future.awaitUninterruptibly();
    if (future.isSuccess()) {
      success++;
    } else {
      error++;
    }
    long time3 = System.currentTimeMillis();
    logger.warn(
        "Simple DefaultClass LOG Success: " + success + " Error: " + error +
        " NB/s: " +
        1000 / (time3 - time2));

    future = new R66Future(true);
    classname = BusinessRequest.DEFAULT_CLASS;
    packet =
        new BusinessRequestPacket(
            classname + " EXECJAVA " + TestExecJavaTask.class.getName()
            + " business 0 execjava business request 0", 0);
    transaction = new BusinessRequest(
        networkTransaction, future, host.getHostid(), packet);
    transaction.run();
    future.awaitUninterruptibly();
    if (future.isSuccess()) {
      success++;
    } else {
      error++;
    }
    long time4 = System.currentTimeMillis();
    logger.warn(
        "Simple ExecJava Success: " + success + " Error: " + error + " NB/s: " +
        1000 / (time4 - time3));

    logger.info("Start Test of Increasing Transaction");
    time1 = System.currentTimeMillis();
    String argsAdd =
        "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
    String value = " business 0 ";
    int lastnb = nb;
    for (int i = 0; i < nb; i++) {
      arrayFuture[i] = new R66Future(true);
      try {
        value += argsAdd + argsAdd + argsAdd + argsAdd + argsAdd +
                 argsAdd + argsAdd + argsAdd + argsAdd
                 + argsAdd;
      } catch (OutOfMemoryError e) {
        logger.warn("Send size: " + value.length());
        lastnb = i;
        break;
      }
      packet = new BusinessRequestPacket(
          TestExecJavaTask.class.getName() + value, 0);
      TestBusinessRequest transaction2 =
          new TestBusinessRequest(networkTransaction, arrayFuture[i],
                                  host.getHostid(), packet);
      executorService.execute(transaction2);
    }
    int success2 = 0;
    int error2 = 0;
    for (int i = 0; i < lastnb; i++) {
      arrayFuture[i].awaitForDoneOrInterruptible();
      if (arrayFuture[i].isSuccess()) {
        success2++;
      } else {
        error2++;
      }
    }
    time2 = System.currentTimeMillis();
    logger.warn(
        "Simple TestExecJavaTask with increasing argument size Success: " +
        success2 + " Error: " + error2
        + " NB/s: " + success2 * nb * 1000 / (time2 - time1));
    executorService.shutdown();
    return error + error2;
  }

}
