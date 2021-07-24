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
package org.waarp.common.crypto.ssl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.DefaultEventExecutor;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpThreadFactory;

import java.util.NoSuchElementException;

/**
 * Utilities for SSL support
 */
public final class WaarpSslUtility {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpSslUtility.class);

  /**
   * EventExecutor associated with Ssl utility
   */
  private static final EventExecutor SSL_EVENT_EXECUTOR =
      new DefaultEventExecutor(new WaarpThreadFactory("SSLEVENT"));
  /**
   * ChannelGroup for SSL
   */
  private static final ChannelGroup sslChannelGroup =
      new DefaultChannelGroup("SslChannelGroup", SSL_EVENT_EXECUTOR);

  /**
   * Closing channel with SSL close at first step
   */
  public static final ChannelFutureListener SSLCLOSE =
      new ChannelFutureListener() {

        @Override
        public final void operationComplete(final ChannelFuture future) {
          if (future.channel().isActive()) {
            future.channel().eventLoop()
                  .submit(new SslThread(future.channel()));
          }
        }
      };

  private WaarpSslUtility() {
  }

  /**
   * Add the Channel as SSL handshake will start soon
   *
   * @param channel
   */
  public static void addSslOpenedChannel(final Channel channel) {
    sslChannelGroup.add(channel);
  }

  /**
   * Add a SslHandler in a pipeline when the channel is already active
   *
   * @param future might be null, condition to start to add the
   *     handler to
   *     the pipeline
   * @param pipeline
   * @param sslHandler
   * @param listener action once the handshake is done
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void addSslHandler(final ChannelFuture future,
                                   final ChannelPipeline pipeline,
                                   final ChannelHandler sslHandler,
                                   final GenericFutureListener<? extends Future<? super Channel>> listener) {
    if (future == null) {
      logger.debug("Add SslHandler: {}", pipeline.channel());
      pipeline.channel().config().setAutoRead(true);
      pipeline.addFirst("SSL", sslHandler);
      ((SslHandler) sslHandler).handshakeFuture().addListener(listener);
    } else {
      future.addListener(new GenericFutureListener() {
        @Override
        public final void operationComplete(final Future future) {
          logger.debug("Add SslHandler: {}", pipeline.channel());
          pipeline.channel().config().setAutoRead(true);
          pipeline.addFirst("SSL", sslHandler);
          ((SslHandler) sslHandler).handshakeFuture().addListener(listener);
        }
      });
    }
    logger.debug("Checked Ssl Handler to be added: {}", pipeline.channel());
  }

  /**
   * Wait for the handshake on the given channel (better to use addSslHandler
   * when handler is added after
   * channel is active)
   *
   * @param channel
   *
   * @return True if the Handshake is done correctly
   */
  public static boolean waitForHandshake(final Channel channel) {
    final ChannelHandler handler = channel.pipeline().first();
    if (handler instanceof SslHandler) {
      logger.debug("Start handshake SSL: {}", channel);
      final SslHandler sslHandler = (SslHandler) handler;
      // Get the SslHandler and begin handshake ASAP.
      // Get notified when SSL handshake is done.
      final Future<Channel> handshakeFuture = sslHandler.handshakeFuture();
      WaarpNettyUtil.awaitOrInterrupted(handshakeFuture,
                                        sslHandler.getHandshakeTimeoutMillis() +
                                        1000);
      logger.debug("Handshake: {}:{}", handshakeFuture.isSuccess(), channel,
                   handshakeFuture.cause());
      if (!handshakeFuture.isSuccess()) {
        channel.close().awaitUninterruptibly(100);
        return false;
      }
    } else {
      logger.info("SSL Not found but connected: {} {}",
                  handler.getClass().getName());
    }
    return true;
  }

  /**
   * Waiting for the channel to be opened and ready (Client side) (blocking
   * call)
   *
   * @param future a future on connect only
   *
   * @return the channel if correctly associated, else return null
   */
  public static Channel waitforChannelReady(final ChannelFuture future) {
    // Wait until the connection attempt succeeds or fails.
    WaarpNettyUtil.awaitOrInterrupted(future);
    if (!future.isSuccess()) {
      logger.error("Channel not connected", future.cause());
      return null;
    }
    final Channel channel = future.channel();
    if (waitForHandshake(channel)) {
      return channel;
    }
    return null;
  }

  /**
   * Utility to force all channels to be closed
   */
  public static void forceCloseAllSslChannels() {
    if (SSL_EVENT_EXECUTOR.isShutdown()) {
      for (final Channel channel : sslChannelGroup) {
        closingSslChannel(channel);
      }
      WaarpNettyUtil.awaitOrInterrupted(sslChannelGroup.close());
      SSL_EVENT_EXECUTOR.shutdownGracefully();
    }
  }

  /**
   * Utility method to close a channel in SSL mode correctly (if any)
   *
   * @param channel
   */
  public static ChannelFuture closingSslChannel(final Channel channel) {
    if (channel.isActive()) {
      removingSslHandler(null, channel, true);
      logger.debug(
          "Close the channel and returns the ChannelFuture: " + channel);
      return channel.closeFuture();
    }
    if (channel.closeFuture().isDone()) {
      return channel.closeFuture();
    }
    logger.debug("Already closed");
    return channel.newSucceededFuture();
  }

  /**
   * Remove the SslHandler (if any) cleanly
   *
   * @param future if not null, wait for this future to be done to
   *     removed
   *     the sslhandler
   * @param channel
   * @param close True to close the channel, else to only remove the SslHandler
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static void removingSslHandler(final ChannelFuture future,
                                        final Channel channel,
                                        final boolean close) {
    if (channel.isActive()) {
      channel.config().setAutoRead(true);
      final ChannelHandler handler = channel.pipeline().first();
      if (handler instanceof SslHandler) {
        final SslHandler sslHandler = (SslHandler) handler;
        if (future != null) {
          future.addListener(new GenericFutureListener() {
            @Override
            public final void operationComplete(final Future future) {
              waitForSslClose(channel, sslHandler, close);
            }
          });
        } else {
          waitForSslClose(channel, sslHandler, close);
        }
      } else {
        if (close) {
          channel.close();
        }
      }
    }
  }

  private static void waitForSslClose(final Channel channel,
                                      final SslHandler sslHandler,
                                      final boolean close) {
    logger.debug("Found SslHandler and wait for Ssl.closeOutbound() : {}",
                 channel);
    if (channel.isActive()) {
      sslHandler.closeOutbound()
                .addListener(new GenericFutureListener<Future<? super Void>>() {
                  @Override
                  public final void operationComplete(
                      final Future<? super Void> future) {
                    logger.debug("Ssl closed: {}", channel);
                    channel.pipeline().remove(sslHandler);
                    if (close && channel.isActive()) {
                      channel.close();
                    }
                  }
                });
    }
  }

  /**
   * Wait for the channel with SSL to be closed
   *
   * @param channel
   * @param delay
   *
   * @return True if an error occurs as an interruption
   */
  public static boolean waitForClosingSslChannel(final Channel channel,
                                                 final long delay) {
    if (!WaarpNettyUtil.awaitOrInterrupted(channel.closeFuture(), delay)) {
      try {
        channel.pipeline().remove(SslHandler.class);
        logger.debug("try to close anyway");
        if (channel.isActive()) {
          WaarpNettyUtil.awaitOrInterrupted(channel.close(), delay);
        }
        return false;
      } catch (final NoSuchElementException e) {
        // ignore
        if (channel.isActive()) {
          WaarpNettyUtil.awaitOrInterrupted(channel.closeFuture(), delay);
        }
      }
    }
    return true;
  }

  /**
   * Thread used to ensure we are not in IO thread when waiting
   */
  private static class SslThread implements Runnable {
    private final Channel channel;

    /**
     * @param channel
     */
    private SslThread(final Channel channel) {
      this.channel = channel;
    }

    @Override
    public void run() {
      closingSslChannel(channel);
    }

  }

}
