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
package org.waarp.common.utility;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import org.waarp.common.logging.SysErrLogger;

/**
 * Utility class for Netty usage
 */
public final class WaarpNettyUtil {

  private static final int TIMEOUT_MILLIS = 1000;
  // Default optimal value for Waarp
  private static final int BUFFER_SIZE_1MB = 1048576;
  // Default optimal value from Netty (tested as correct for Waarp)
  private static final int DEFAULT_LOW_WATER_MARK = 32 * 1024;
  private static final int DEFAULT_HIGH_WATER_MARK = 64 * 1024;

  private WaarpNettyUtil() {
  }

  /**
   * Add default configuration for client bootstrap
   *
   * @param bootstrap
   * @param group
   * @param timeout
   */
  public static void setBootstrap(Bootstrap bootstrap, EventLoopGroup group,
                                  int timeout) {
    bootstrap.channel(NioSocketChannel.class);
    bootstrap.group(group);
    bootstrap.option(ChannelOption.TCP_NODELAY, false);
    bootstrap.option(ChannelOption.SO_REUSEADDR, true);
    bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
    bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);
    bootstrap.option(ChannelOption.SO_RCVBUF, BUFFER_SIZE_1MB);
    bootstrap.option(ChannelOption.SO_SNDBUF, BUFFER_SIZE_1MB);
    bootstrap.option(ChannelOption.WRITE_BUFFER_WATER_MARK,
                     new WriteBufferWaterMark(DEFAULT_LOW_WATER_MARK,
                                              DEFAULT_HIGH_WATER_MARK));
    bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
  }

  /**
   * Add default configuration for server bootstrap
   *
   * @param bootstrap
   * @param group
   * @param timeout
   */
  public static void setServerBootstrap(ServerBootstrap bootstrap,
                                        EventLoopGroup group, int timeout) {
    bootstrap.channel(NioServerSocketChannel.class);
    bootstrap.group(group);
    // bootstrap.option(ChannelOption.TCP_NODELAY, true)
    bootstrap.option(ChannelOption.SO_REUSEADDR, true);
    bootstrap.childOption(ChannelOption.TCP_NODELAY, false);
    bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
    bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
    bootstrap.childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);
    bootstrap.childOption(ChannelOption.SO_RCVBUF, BUFFER_SIZE_1MB);
    bootstrap.childOption(ChannelOption.SO_SNDBUF, BUFFER_SIZE_1MB);
    bootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK,
                          new WriteBufferWaterMark(DEFAULT_LOW_WATER_MARK,
                                                   DEFAULT_HIGH_WATER_MARK));
    bootstrap
        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
  }

  /**
   * @param future
   *
   * @return True if await done, else interruption occurs
   */
  public static boolean awaitOrInterrupted(Future<?> future) {
    try {
      while (!Thread.interrupted()) {
        if (future.await(TIMEOUT_MILLIS)) {
          return true;
        }
      }
    } catch (final InterruptedException e) {//NOSONAR
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    }
    return false;
  }

  /**
   * @param future
   * @param timeMilliseconds
   *
   * @return True if await done, else interruption occurs
   */
  public static boolean awaitOrInterrupted(Future<?> future,
                                           long timeMilliseconds) {
    try {
      if (future.await(timeMilliseconds)) {
        return !Thread.interrupted();
      }
    } catch (final InterruptedException e) {//NOSONAR
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    }
    return false;
  }

  /**
   * @param future
   *
   * @return True if await and isSuccess, else interruption or not success
   *     occurs
   */
  public static boolean awaitIsSuccessOfInterrupted(Future<?> future) {
    try {
      while (!Thread.interrupted()) {
        if (future.await(TIMEOUT_MILLIS)) {
          return future.isSuccess();
        }
      }
    } catch (final InterruptedException e) {//NOSONAR
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    }
    return false;
  }

  /**
   * Shortcut to release a ByteByf
   *
   * @param byteBuf
   *
   * @return True if the ByteBuf is released
   */
  public static boolean release(final ByteBuf byteBuf) {
    if (byteBuf != null && byteBuf.refCnt() > 0) {
      return byteBuf.release();
    }
    return false;
  }

  /**
   * Shortcut to release completely a ByteByf
   *
   * @param byteBuf
   */
  public static void releaseCompletely(final ByteBuf byteBuf) {
    if (byteBuf != null) {
      if (byteBuf.refCnt() != 0) {
        while (!byteBuf.release()) {
          // Nothing
        }
      }
    }
  }

  /**
   * Utility method to return a ByteBuf slice with Write ready.
   * No retain is called on this slice.
   *
   * @param byteBuf
   * @param start
   * @param size
   *
   * @return the ByteBuf sliced
   */
  public static ByteBuf slice(final ByteBuf byteBuf, final int start,
                              final int size) {
    final ByteBuf bufSliced = byteBuf.slice(start, size);
    bufSliced.writerIndex(0);
    return bufSliced;
  }
}
