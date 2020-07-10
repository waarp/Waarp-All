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
package org.waarp.openr66.protocol.utils;

import ch.qos.logback.classic.LoggerContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.LoggerFactory;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.file.DataBlock;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.task.localexec.LocalExecClient;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.DataPacket;
import org.waarp.openr66.protocol.localhandler.packet.EndTransferPacket;
import org.waarp.openr66.protocol.localhandler.packet.ErrorPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.networkhandler.packet.NetworkPacket;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.waarp.openr66.database.DbConstantR66.*;

/**
 * Channel Utils
 */
public class ChannelUtils extends Thread {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(ChannelUtils.class);

  public static final Integer NOCHANNEL = Integer.MIN_VALUE;

  /**
   * Get the Remote InetAddress
   *
   * @param channel
   *
   * @return the remote InetAddress
   */
  public static final InetAddress getRemoteInetAddress(Channel channel) {
    InetSocketAddress socketAddress =
        (InetSocketAddress) channel.remoteAddress();
    if (socketAddress == null) {
      socketAddress = new InetSocketAddress(20);
    }
    return socketAddress.getAddress();
  }

  /**
   * Get the Local InetAddress
   *
   * @param channel
   *
   * @return the local InetAddress
   */
  public static final InetAddress getLocalInetAddress(Channel channel) {
    final InetSocketAddress socketAddress =
        (InetSocketAddress) channel.localAddress();
    return socketAddress.getAddress();
  }

  /**
   * Get the Remote InetSocketAddress
   *
   * @param channel
   *
   * @return the remote InetSocketAddress
   */
  public static final InetSocketAddress getRemoteInetSocketAddress(
      Channel channel) {
    return (InetSocketAddress) channel.remoteAddress();
  }

  /**
   * Get the Local InetSocketAddress
   *
   * @param channel
   *
   * @return the local InetSocketAddress
   */
  public static final InetSocketAddress getLocalInetSocketAddress(
      Channel channel) {
    return (InetSocketAddress) channel.localAddress();
  }

  /**
   * Terminate all registered channels
   *
   * @return the number of previously registered network channels
   */
  private static int terminateCommandChannels() {
    if (Configuration.configuration.getServerChannelGroup() == null) {
      return 0;
    }
    final int result =
        Configuration.configuration.getServerChannelGroup().size();
    logger.info("ServerChannelGroup: " + result);
    Configuration.configuration.getServerChannelGroup().close();
    return result;
  }

  /**
   * Terminate all registered Http channels
   *
   * @return the number of previously registered http network channels
   */
  private static int terminateHttpChannels() {
    if (Configuration.configuration.getHttpChannelGroup() == null) {
      return 0;
    }
    final int result = Configuration.configuration.getHttpChannelGroup().size();
    logger.debug("HttpChannelGroup: " + result);
    Configuration.configuration.getHttpChannelGroup().close();
    return result;
  }

  /**
   * Return the current number of network connections
   *
   * @param configuration
   *
   * @return the current number of network connections
   */
  public static final int nbCommandChannels(Configuration configuration) {
    return configuration.getServerChannelGroup().size();
  }

  /**
   * @param localChannelReference
   * @param digestGlobal
   * @param block
   *
   * @return the ChannelFuture of this write operation
   *
   * @throws OpenR66ProtocolPacketException
   */
  public static ChannelFuture writeBackDataBlock(
      LocalChannelReference localChannelReference,
      FilesystemBasedDigest digestGlobal, DataBlock block)
      throws OpenR66ProtocolPacketException {
    ByteBuf md5 = Unpooled.EMPTY_BUFFER;
    final DbTaskRunner runner = localChannelReference.getSession().getRunner();
    if (RequestPacket.isMD5Mode(runner.getMode())) {
      md5 = FileUtils
          .getHash(block.getBlock(), Configuration.configuration.getDigest(),
                   digestGlobal);
    } else if (digestGlobal != null) {
      digestGlobal.Update(block.getBlock());
    }
    if (runner.getRank() % 100 == 1 ||
        localChannelReference.getSessionState() != R66FiniteDualStates.DATAS) {
      localChannelReference.sessionNewState(R66FiniteDualStates.DATAS);
    }
    logger.trace("sending data block {}", runner.getRank());
    final DataPacket data = new DataPacket(runner.getRank(), block.getBlock(),
                                           md5);// was block.getBlock().copy()
    final ChannelFuture future =
        writeAbstractLocalPacket(localChannelReference, data, false);
    runner.incrementRank();
    return future;
  }

  /**
   * Write the EndTransfer
   *
   * @param localChannelReference
   *
   * @throws OpenR66ProtocolPacketException
   */
  public static final void writeEndTransfer(
      LocalChannelReference localChannelReference)
      throws OpenR66ProtocolPacketException {
    final EndTransferPacket packet =
        new EndTransferPacket(LocalPacketFactory.REQUESTPACKET);
    localChannelReference.sessionNewState(R66FiniteDualStates.ENDTRANSFERS);
    writeAbstractLocalPacket(localChannelReference, packet, true);
  }

  /**
   * Write the EndTransfer plus Global Hash
   *
   * @param localChannelReference
   * @param hash
   *
   * @throws OpenR66ProtocolPacketException
   */
  public static final void writeEndTransfer(
      LocalChannelReference localChannelReference, String hash)
      throws OpenR66ProtocolPacketException {
    final EndTransferPacket packet =
        new EndTransferPacket(LocalPacketFactory.REQUESTPACKET, hash);
    localChannelReference.sessionNewState(R66FiniteDualStates.ENDTRANSFERS);
    writeAbstractLocalPacket(localChannelReference, packet, true);
  }

  /**
   * Write an AbstractLocalPacket to the network Channel
   *
   * @param localChannelReference
   * @param packet
   * @param wait
   *
   * @return the ChannelFuture on write operation
   *
   * @throws OpenR66ProtocolPacketException
   */
  public static ChannelFuture writeAbstractLocalPacket(
      final LocalChannelReference localChannelReference,
      AbstractLocalPacket packet, boolean wait)
      throws OpenR66ProtocolPacketException {
    final NetworkPacket networkPacket;
    try {
      logger.trace("TRACE ID {} {} {}", localChannelReference.getLocalId(),
                   localChannelReference.getRemoteId(), packet);
      networkPacket = new NetworkPacket(localChannelReference.getLocalId(),
                                        localChannelReference.getRemoteId(),
                                        packet, localChannelReference);
    } catch (final OpenR66ProtocolPacketException e) {
      logger.error(Messages.getString("ChannelUtils.6") + packet,
                   //$NON-NLS-1$
                   e);
      throw e;
    }
    boolean addListener = packet instanceof ErrorPacket &&
                          ((ErrorPacket) packet).getCode() ==
                          ErrorPacket.FORWARDCLOSECODE;
    ChannelFuture future =
        localChannelReference.getNetworkChannel().writeAndFlush(networkPacket);
    if (addListener) {
      future.addListener(new GenericFutureListener<Future<? super Void>>() {

        @Override
        public void operationComplete(Future<? super Void> future)
            throws Exception {
          localChannelReference.close();
        }
      });
    }
    if (wait) {
      localChannelReference.getNetworkChannelObject().use();
      WaarpNettyUtil.awaitOrInterrupted(future);
    }
    return future;
  }

  /**
   * Exit global ChannelFactory
   */
  public static void exit() {
    logger.info("Current launched threads before exit: " +
                ManagementFactory.getThreadMXBean().getThreadCount());
    if (Configuration.configuration.getConstraintLimitHandler() != null) {
      Configuration.configuration.getConstraintLimitHandler().release();
    }
    // First try to StopAll
    TransferUtils
        .stopSelectedTransfers(admin.getSession(), 0, null, null, null, null,
                               null, null, null, null, null, true, true, true);
    Configuration.configuration.setShutdown(true);
    Configuration.configuration.prepareServerStop();
    long delay = Configuration.configuration.getTimeoutCon();
    // Inform others that shutdown
    if (Configuration.configuration.getLocalTransaction() != null) {
      int nb = Configuration.configuration.getLocalTransaction()
                                          .getNumberLocalChannel();
      Configuration.configuration.getLocalTransaction().shutdownLocalChannels();
      if (nb == 1) {
        delay /= 3;
      }
    }
    logger.info("Unbind server network services");
    Configuration.configuration.unbindServer();
    logger.warn(
        Messages.getString("ChannelUtils.7") + delay + " ms"); //$NON-NLS-1$
    try {
      Thread.sleep(delay);
    } catch (final InterruptedException e) {//NOSONAR
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    }
    NetworkTransaction.stopAllEndRetrieve();
    if (Configuration.configuration.getLocalTransaction() != null) {
      Configuration.configuration.getLocalTransaction()
                                 .debugPrintActiveLocalChannels();
    }
    if (Configuration.configuration.getGlobalTrafficShapingHandler() != null) {
      Configuration.configuration.getGlobalTrafficShapingHandler().release();
    }
    logger.info("Exit Shutdown Http");
    terminateHttpChannels();
    logger.info("Exit Shutdown Local");
    if (Configuration.configuration.getLocalTransaction() != null) {
      Configuration.configuration.getLocalTransaction().closeAll();
    }
    logger.info("Exit Shutdown LocalExec");
    if (Configuration.configuration.isUseLocalExec()) {
      LocalExecClient.releaseResources();
    }
    logger.info("Exit Shutdown Command");
    terminateCommandChannels();
    logger.info("Exit Shutdown Db Connection");
    DbAdmin.closeAllConnection();
    logger.info("Exit Shutdown ServerStop");
    Configuration.configuration.serverStop();
    logger.warn(Messages.getString("ChannelUtils.15")); //$NON-NLS-1$
    SysErrLogger.FAKE_LOGGER
        .syserr(Messages.getString("ChannelUtils.15")); //$NON-NLS-1$
    stopLogger();
  }

  public static final void stopLogger() {
    if (DetectionUtils.isJunit()) {
      return;
    }
    if (WaarpLoggerFactory
            .getDefaultFactory() instanceof WaarpSlf4JLoggerFactory &&
        !DetectionUtils.isJunit()) {
      final LoggerContext lc =
          (LoggerContext) LoggerFactory.getILoggerFactory();
      lc.stop();
    }
  }

  /**
   * This function is the top function to be called when the server is to be
   * shutdown.
   */
  @Override
  public void run() {
    logger.info("Should restart? " + WaarpShutdownHook.isRestart());
    WaarpShutdownHook.terminate(false);
  }

  /**
   * Start Shutdown
   */
  public static final void startShutdown() {
    if (WaarpShutdownHook.isInShutdown()) {
      return;
    }
    final Thread thread = new Thread(new ChannelUtils(), "R66 Shutdown Thread");
    thread.setDaemon(false);
    thread.start();
  }
}
