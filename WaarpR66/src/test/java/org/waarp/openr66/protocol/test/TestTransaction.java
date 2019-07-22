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
package org.waarp.openr66.protocol.test;

import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.TestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import java.net.SocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Test class for internal ping pong test
 *
 *
 */
public class TestTransaction implements Runnable {
  /**
   * Internal Logger
   */
  private static WaarpLogger logger;

  final private NetworkTransaction networkTransaction;

  final private R66Future future;

  private final SocketAddress socketAddress;

  final private TestPacket testPacket;

  public TestTransaction(NetworkTransaction networkTransaction,
                         R66Future future, SocketAddress socketAddress,
                         TestPacket packet) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(TestTransaction.class);
    }
    this.networkTransaction = networkTransaction;
    this.future = future;
    this.socketAddress = socketAddress;
    testPacket = packet;
  }

  public static void main(String[] args) {
    WaarpLoggerFactory
        .setDefaultFactory(new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(TestTransaction.class);
    }
    if (args.length < 1) {
      logger.error("Needs at least the configuration file as first argument");
      return;
    }
    if (!FileBasedConfiguration
        .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }
    final DbHostAuth host = Configuration.configuration.getHOST_AUTH();
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }
    Configuration.configuration.pipelineInit();

    final NetworkTransaction networkTransaction = new NetworkTransaction();
    final ExecutorService executorService = Executors.newCachedThreadPool();
    int nb = 100;
    if (args.length > 1) {
      nb = Integer.parseInt(args[1]);
    }
    final R66Future[] arrayFuture = new R66Future[nb];
    logger.info("Start Test of Transaction");
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
    networkTransaction.closeAll();
  }

  @Override
  public void run() {
    final LocalChannelReference localChannelReference = networkTransaction
        .createConnectionWithRetry(socketAddress, false, future);
    if (localChannelReference == null) {
      logger.error("Cannot connect: ", future.getCause());
      future.setResult(null);
      future.setFailure(future.getCause());
      return;
    }
    localChannelReference.sessionNewState(R66FiniteDualStates.TEST);
    try {
      ChannelUtils
          .writeAbstractLocalPacket(localChannelReference, testPacket, false);
    } catch (final OpenR66ProtocolPacketException e) {
      future.setResult(null);
      future.setFailure(e);
      localChannelReference.getLocalChannel().close();
      return;
    }
  }

}
