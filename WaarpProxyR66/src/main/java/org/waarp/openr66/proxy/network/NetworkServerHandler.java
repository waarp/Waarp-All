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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.ReadTimeoutException;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.protocol.exception.OpenR66Exception;
import org.waarp.openr66.protocol.exception.OpenR66ExceptionTrappedFactory;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessNoWriteBackException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.ConnectionErrorPacket;
import org.waarp.openr66.protocol.localhandler.packet.KeepAlivePacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketCodec;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.networkhandler.packet.NetworkPacket;
import org.waarp.openr66.protocol.utils.ChannelCloseTimer;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import java.net.BindException;
import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.waarp.openr66.protocol.configuration.Configuration.*;

/**
 * Network Server Handler (Requester side)
 */
public class NetworkServerHandler
    extends SimpleChannelInboundHandler<NetworkPacket> {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(NetworkServerHandler.class);

  /**
   * The underlying Network Channel
   */
  private Channel networkChannel;
  /**
   * The underlying Proxified associated Channel
   */
  private Channel proxyChannel;
  /**
   * The associated bridge
   */
  private ProxyBridge bridge;
  /**
   * Does this Handler is for SSL
   */
  protected boolean isSSL;
  /**
   * Is this Handler a server side
   */
  protected final boolean isServer;
  /**
   * To handle the keep alive
   */
  private final AtomicInteger keepAlivedSent = new AtomicInteger();
  /**
   * Future to wait for Client to be setup
   */
  protected volatile R66Future clientFuture;

  /**
   * @param isServer
   */
  public NetworkServerHandler(final boolean isServer) {
    this.isServer = isServer;
    if (!this.isServer) {
      clientFuture = new R66Future(true);
    }
  }

  public final void setBridge(final ProxyBridge bridge) {
    this.bridge = bridge;
    if (this.bridge != null) {
      proxyChannel = bridge.getSource().getNetworkChannel();
    }
    clientFuture.setSuccess();
    logger.info("Proxy setBridge: {} {}", isServer, (bridge != null?
        bridge.getProxyEntry() + " proxyChannelId: " + proxyChannel.id() :
        "nobridge"));
  }

  /**
   * @return the networkChannel
   */
  public final Channel getNetworkChannel() {
    return networkChannel;
  }

  public final void close() {
    WaarpSslUtility.closingSslChannel(networkChannel);
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx) {
    if (proxyChannel != null) {
      WaarpSslUtility.closingSslChannel(proxyChannel);
    }
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    try {
      networkChannel = ctx.channel();
      /*
       * The associated Local Address
       */
      final SocketAddress localAddress = networkChannel.localAddress();
      if (isServer) {
        final ProxyEntry entry = ProxyEntry.get(localAddress.toString());
        if (entry == null) {
          // error
          exceptionCaught(ctx, new OpenR66ProtocolNoConnectionException(
              "Cannot found Proxy Entry: connection aborted"));
          // WaarpSslUtility.closingSslChannel(networkChannel);
          logger.error("No proxy configuration found for: " + localAddress);
          return;
        }
        bridge = new ProxyBridge(entry, this);
        bridge.initializeProxy();
        if (!bridge.waitForRemoteConnection()) {
          exceptionCaught(ctx, new OpenR66ProtocolNoConnectionException(
              "Proxy Cannot connect to remote Server: connection aborted"));
          logger.error("No connection for proxy: " + localAddress);
          return;
        }
        proxyChannel = bridge.getProxified().networkChannel;
        logger.warn("Connected: " + isServer + ' ' + bridge.getProxyEntry() +
                    " proxyChannelId: " + proxyChannel.id());
      } else {
        clientFuture.awaitOrInterruptible(configuration.getTimeoutCon());
        if (bridge == null || !clientFuture.isSuccess()) {
          exceptionCaught(ctx, new OpenR66ProtocolNoConnectionException(
              "Proxy Cannot connect to remote Server: connection aborted"));
          logger.error("No connection for proxy: " + localAddress);
          return;
        }
        bridge.remoteConnected();
      }
      logger.debug("Proxy Network Channel Connected: {} ", ctx.channel().id());
    } finally {
      ctx.read();
    }
  }

  @Override
  public void userEventTriggered(final ChannelHandlerContext ctx,
                                 final Object evt) throws Exception {
    if (configuration.isShutdown()) {
      return;
    }
    if (evt instanceof IdleStateEvent) {
      if (keepAlivedSent.get() > 0) {
        if (keepAlivedSent.get() < 5) {
          // ignore this time
          keepAlivedSent.getAndIncrement();
          return;
        }
        logger.error("Proxy Not getting KAlive: closing channel");
        if (configuration.getR66Mib() != null) {
          configuration.getR66Mib()
                       .notifyWarning("Proxy KeepAlive get no answer",
                                      "Closing network connection");
        }
        ChannelCloseTimer.closeFutureChannel(ctx.channel());
      } else {
        keepAlivedSent.set(1);
        final KeepAlivePacket keepAlivePacket = new KeepAlivePacket();
        final NetworkPacket response =
            new NetworkPacket(ChannelUtils.NOCHANNEL, ChannelUtils.NOCHANNEL,
                              keepAlivePacket, null);
        logger.info("Proxy Write KAlive");
        ctx.channel().writeAndFlush(response);
      }
    }
  }

  public final void resetKeepAlive() {
    keepAlivedSent.set(0);
  }

  @Override
  public void channelRead0(final ChannelHandlerContext ctx,
                           final NetworkPacket msg) {
    try {
      if (msg.getCode() == LocalPacketFactory.NOOPPACKET) {
        resetKeepAlive();
        // Will forward
      } else if (msg.getCode() == LocalPacketFactory.CONNECTERRORPACKET) {
        logger.debug("Proxy NetworkRecv: {}", msg);
        // Special code to STOP here
        if (msg.getLocalId() == ChannelUtils.NOCHANNEL) {
          // No way to know what is wrong: close all connections with
          // remote host
          logger.error(
              "Proxy Will close NETWORK channel, Cannot continue connection with remote Host: " +
              msg + " : " + ctx.channel().remoteAddress());
          WaarpSslUtility.closingSslChannel(ctx.channel());
          msg.clear();
          return;
        }
      } else if (msg.getCode() == LocalPacketFactory.KEEPALIVEPACKET) {
        resetKeepAlive();
        try {
          final KeepAlivePacket keepAlivePacket =
              (KeepAlivePacket) LocalPacketCodec.decodeNetworkPacket(
                  msg.getBuffer());
          if (keepAlivePacket.isToValidate()) {
            keepAlivePacket.validate();
            final NetworkPacket response =
                new NetworkPacket(ChannelUtils.NOCHANNEL,
                                  ChannelUtils.NOCHANNEL, keepAlivePacket,
                                  null);
            logger.info("Proxy Answer KAlive");
            ctx.channel().writeAndFlush(response);
          } else {
            logger.info("Proxy Get KAlive");
          }
        } catch (final OpenR66ProtocolPacketException ignored) {
          // nothing
        }
        msg.clear();
        return;
      }
      // forward message
      resetKeepAlive();
      if (proxyChannel != null) {
        proxyChannel.writeAndFlush(msg);
      } else {
        msg.clear();
      }
    } finally {
      ctx.read();
    }
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx,
                              final Throwable cause) {
    final Channel channel = ctx.channel();
    logger.debug("Proxy Network Channel Exception: {}", channel.id(), cause);
    if (cause instanceof ReadTimeoutException) {
      final ReadTimeoutException exception = (ReadTimeoutException) cause;
      // No read for too long
      logger.error("ReadTimeout so Will close NETWORK channel {}",
                   exception.getClass().getName() + " : " +
                   exception.getMessage());
      ChannelCloseTimer.closeFutureChannel(channel);
      return;
    }
    if (cause instanceof BindException) {
      // received when not yet connected
      logger.debug("BindException");
      ChannelCloseTimer.closeFutureChannel(channel);
      return;
    }
    final OpenR66Exception exception =
        OpenR66ExceptionTrappedFactory.getExceptionFromTrappedException(channel,
                                                                        cause);
    if (exception != null) {
      if (exception instanceof OpenR66ProtocolBusinessNoWriteBackException) {
        logger.debug("Will close NETWORK channel");
        ChannelCloseTimer.closeFutureChannel(channel);
        return;
      } else if (exception instanceof OpenR66ProtocolNoConnectionException) {
        logger.info("Connection impossible with NETWORK channel {}",
                    exception.getMessage());
        channel.close();
        return;
      } else {
        logger.info("Network Channel Exception: {} {}", channel.id(),
                    exception.getMessage());
      }
      final ConnectionErrorPacket errorPacket =
          new ConnectionErrorPacket(exception.getMessage(), null);
      writeError(channel, ChannelUtils.NOCHANNEL, ChannelUtils.NOCHANNEL,
                 errorPacket);
      if (proxyChannel != null) {
        writeError(proxyChannel, ChannelUtils.NOCHANNEL, ChannelUtils.NOCHANNEL,
                   errorPacket);
      }
      logger.debug("Will close NETWORK channel: {}", exception.getMessage());
      ChannelCloseTimer.closeFutureChannel(channel);
    } else {
      // Nothing to do
    }
  }

  /**
   * Write error back to remote client
   *
   * @param channel
   * @param remoteId
   * @param localId
   * @param error
   */
  final void writeError(final Channel channel, final Integer remoteId,
                        final Integer localId,
                        final AbstractLocalPacket error) {
    if (channel.isActive()) {
      NetworkPacket networkPacket = null;
      logger.info("Proxy Error to send {}", error);
      try {
        networkPacket = new NetworkPacket(localId, remoteId, error, null);
      } catch (final OpenR66ProtocolPacketException ignored) {
        // nothing
      }
      if (networkPacket != null) {
        final NetworkPacket finalNP = networkPacket;
        channel.eventLoop().submit(new Runnable() {
          @Override
          public final void run() {
            channel.writeAndFlush(finalNP).awaitUninterruptibly();
            finalNP.clear();
          }
        });
      }
    }
  }

  /**
   * @return True if this Handler is for SSL
   */
  public final boolean isSsl() {
    return isSSL;
  }
}
