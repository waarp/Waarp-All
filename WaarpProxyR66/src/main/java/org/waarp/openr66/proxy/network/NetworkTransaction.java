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
package org.waarp.openr66.proxy.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipelineException;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.protocol.exception.OpenR66Exception;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNetworkException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.proxy.network.ssl.NetworkSslServerHandler;
import org.waarp.openr66.proxy.network.ssl.NetworkSslServerInitializerProxy;

import java.net.ConnectException;
import java.net.SocketAddress;

import static org.waarp.openr66.protocol.configuration.Configuration.*;

/**
 * This class handles Network Transaction connections
 */
public class NetworkTransaction {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(NetworkTransaction.class);

  private final Bootstrap clientBootstrap;
  private final Bootstrap clientSslBootstrap;
  private final ChannelGroup networkChannelGroup;

  public NetworkTransaction() {
    networkChannelGroup = new DefaultChannelGroup("NetworkChannels",
                                                  configuration
                                                      .getSubTaskGroup()
                                                      .next());
    final NetworkServerInitializerProxy networkServerInitializerProxy =
        new NetworkServerInitializerProxy(false);
    clientBootstrap = new Bootstrap();
    WaarpNettyUtil
        .setBootstrap(clientBootstrap, configuration.getNetworkWorkerGroup(),
                      (int) configuration.getTimeoutCon(),
                      configuration.getBlockSize() + 64, false);
    clientBootstrap.handler(networkServerInitializerProxy);
    clientSslBootstrap = new Bootstrap();
    if (configuration.isUseSSL() && configuration.getHostSslId() != null) {
      final NetworkSslServerInitializerProxy networkSslServerInitializerProxy =
          new NetworkSslServerInitializerProxy(true);
      WaarpNettyUtil.setBootstrap(clientSslBootstrap,
                                  configuration.getNetworkWorkerGroup(),
                                  (int) configuration.getTimeoutCon(),
                                  configuration.getBlockSize() + 64, false);
      clientSslBootstrap.handler(networkSslServerInitializerProxy);
    } else {
      if (configuration.isWarnOnStartup()) {
        logger.warn("No SSL support configured");
      } else {
        logger.info("No SSL support configured");
      }
    }
  }

  /**
   * Create a connection to the specified socketAddress with multiple retries
   *
   * @param socketAddress
   * @param isSSL
   *
   * @return the Channel
   */
  public Channel createConnectionWithRetry(final SocketAddress socketAddress,
                                           final boolean isSSL) {
    Channel channel = null;
    OpenR66Exception lastException = null;
    for (int i = 0; i < RETRYNB; i++) {
      try {
        channel = createConnection(socketAddress, isSSL);
        break;
      } catch (final OpenR66ProtocolNoConnectionException e1) {
        lastException = e1;
        channel = null;
        break;
      } catch (final OpenR66ProtocolNetworkException e1) {
        // Can retry
        lastException = e1;
        channel = null;
        try {
          Thread.sleep(WAITFORNETOP * 5);
        } catch (final InterruptedException e) {//NOSONAR
          SysErrLogger.FAKE_LOGGER.ignoreLog(e);
          break;
        }
      }
    }
    if (channel == null) {
      if (lastException != null) {
        logger.info("Proxy Cannot connect : {}", lastException.getMessage());
      } else {
        logger.info("Proxy Cannot connect!");
      }
    } else if (lastException != null) {
      logger.debug("Proxy Connection retried since {}",
                   lastException.getMessage());
    } else {
      logger.info("Proxy Connected");
    }
    return channel;
  }

  /**
   * Create a connection to the specified socketAddress
   *
   * @param socketAddress
   * @param isSSL
   *
   * @return the channel
   *
   * @throws OpenR66ProtocolNetworkException
   * @throws OpenR66ProtocolNoConnectionException
   */
  private Channel createConnection(final SocketAddress socketAddress,
                                   final boolean isSSL)
      throws OpenR66ProtocolNetworkException,
             OpenR66ProtocolNoConnectionException {
    Channel channel = null;
    boolean ok = false;
    // check valid limit on server side only (could be the initiator but not a client)
    boolean valid = false;
    for (int i = 0; i < RETRYNB * 2; i++) {
      if (configuration.getConstraintLimitHandler().checkConstraintsSleep(i)) {
        logger.info("Proxy Constraints exceeded: {}", i);
      } else {
        logger.debug("Proxy Constraints NOT exceeded");
        valid = true;
        break;
      }
    }
    if (!valid) {
      // Limit is locally exceeded
      logger.debug("Proxy Overloaded local system");
      throw new OpenR66ProtocolNetworkException(
          "Proxy Cannot connect to remote server due to local overload");
    }
    try {
      channel = createNewConnection(socketAddress, isSSL);
      ok = true;
    } finally {
      if (!ok) {
        if (channel != null) {
          if (channel.isOpen()) {
            WaarpSslUtility.closingSslChannel(channel);
          }
          channel = null;
        }
      }
    }
    return channel;
  }

  /**
   * @param socketServerAddress
   * @param isSSL
   *
   * @return the channel
   *
   * @throws OpenR66ProtocolNetworkException
   * @throws OpenR66ProtocolNoConnectionException
   */
  private Channel createNewConnection(final SocketAddress socketServerAddress,
                                      final boolean isSSL)
      throws OpenR66ProtocolNetworkException,

             OpenR66ProtocolNoConnectionException {
    ChannelFuture channelFuture = null;
    for (int i = 0; i < RETRYNB; i++) {
      try {
        if (isSSL) {
          if (configuration.getHostSslId() != null) {
            channelFuture = clientSslBootstrap.connect(socketServerAddress);
          } else {
            throw new OpenR66ProtocolNoConnectionException("No SSL support");
          }
        } else {
          channelFuture = clientBootstrap.connect(socketServerAddress);
        }
      } catch (final ChannelPipelineException e) {
        throw new OpenR66ProtocolNoConnectionException(
            "Proxy Cannot connect to remote server due to a channel exception");
      }
      try {
        channelFuture.await(configuration.getTimeoutCon() / 3);
      } catch (final InterruptedException e1) {//NOSONAR
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      if (channelFuture.isSuccess()) {
        final Channel channel = channelFuture.channel();
        if (isSSL && !NetworkSslServerHandler.isSslConnectedChannel(channel)) {
          logger.info("Proxy KO CONNECT since SSL handshake is over");
          channel.close();
          throw new OpenR66ProtocolNoConnectionException(
              "Proxy Cannot finish connect to remote server");
        }
        networkChannelGroup.add(channel);
        return channel;
      } else {
        try {
          Thread.sleep(WAITFORNETOP * 2);
        } catch (final InterruptedException e) {//NOSONAR
          SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
        if (!channelFuture.isDone()) {
          throw new OpenR66ProtocolNoConnectionException(
              "Proxy Cannot connect to remote server due to interruption");
        }
        if (channelFuture.cause() instanceof ConnectException) {
          logger.debug("Proxy KO CONNECT: {}",
                       channelFuture.cause().getMessage());
          throw new OpenR66ProtocolNoConnectionException(
              "Proxy Cannot connect to remote server", channelFuture.cause());
        } else {
          logger.debug("Proxy KO CONNECT but retry", channelFuture.cause());
        }
      }
    }
    final Throwable cause = channelFuture == null? null : channelFuture.cause();
    throw new OpenR66ProtocolNetworkException(
        "Proxy Cannot connect to remote server", cause);
  }

  /**
   * Close all Network Transaction
   */
  public void closeAll() {
    logger.debug("Proxy close All Network Channels");
    try {
      Thread.sleep(RETRYINMS * 2);
    } catch (final InterruptedException e) {//NOSONAR
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    }
    if (!configuration.isServer()) {
      WaarpShutdownHook.shutdownHook.launchFinalExit();
    }
    for (final Channel channel : networkChannelGroup) {
      WaarpSslUtility.closingSslChannel(channel);
    }
    WaarpNettyUtil.awaitOrInterrupted(networkChannelGroup.close());
    try {
      Thread.sleep(WAITFORNETOP);
    } catch (final InterruptedException e) {//NOSONAR
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    }
    configuration.clientStop();
    logger.debug("Proxy Last action before exit");
    WaarpSystemUtil.stopLogger();
  }

  /**
   * @return The number of Network Channels
   */
  public int getNumberClients() {
    return networkChannelGroup.size();
  }
}
