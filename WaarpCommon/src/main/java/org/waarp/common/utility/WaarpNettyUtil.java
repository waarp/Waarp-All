/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.common.utility;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;

/**
 * Utility class for Netty usage
 * 
 * @author "Frederic Bregier"
 *
 */
public class WaarpNettyUtil {

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
        bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);
        bootstrap.option(ChannelOption.SO_RCVBUF, 1048576);
        bootstrap.option(ChannelOption.SO_SNDBUF, 1048576);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    }

    /**
     * Add default configuration for server bootstrap
     *
     * @param bootstrap
     * @param groupBoss
     * @param groupWorker
     * @param timeout
     */
    public static void setServerBootstrap(ServerBootstrap bootstrap,
                                          EventLoopGroup groupBoss,
                                          EventLoopGroup groupWorker,
                                          int timeout) {
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.group(groupBoss, groupWorker);
        // bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);
        bootstrap.childOption(ChannelOption.SO_RCVBUF, 1048576);
        bootstrap.childOption(ChannelOption.SO_SNDBUF, 1048576);
        bootstrap
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    }

    /**
     * Add default configuration for server bootstrap
     *
     * @param bootstrap
     * @param group
     * @param timeout
     */
    public static void setServerBootstrap(ServerBootstrap bootstrap,
                                          EventLoopGroup group,
                                          int timeout) {
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.group(group);
        // bootstrap.option(ChannelOption.TCP_NODELAY, true);
        bootstrap.option(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, true);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout);
        bootstrap.childOption(ChannelOption.SO_RCVBUF, 1048576);
        bootstrap.childOption(ChannelOption.SO_SNDBUF, 1048576);
        bootstrap
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    }

    /**
     * @param future
     *
     * @return True if await done, else interruption occurs
     */
    public static boolean awaitDoneOrInterrupted(Future<?> future) {
        try {
            while (!Thread.interrupted()) {
                if (future.await(10000)) {
                    return true;
                }
            }
        } catch (InterruptedException e) {
        }
        return false;
    }
}
