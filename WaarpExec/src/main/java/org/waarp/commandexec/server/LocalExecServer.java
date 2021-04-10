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
package org.waarp.commandexec.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.waarp.commandexec.utils.LocalExecDefaultResult;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpThreadFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * LocalExec server Main method.
 */
public class LocalExecServer {

  static final EventLoopGroup workerGroup = new NioEventLoopGroup();
  static final EventExecutorGroup executor =
      new DefaultEventExecutorGroup(DetectionUtils.numberThreads(),
                                    new WaarpThreadFactory("LocalExecServer"));

  /**
   * Takes 3 optional arguments:<br>
   * - no argument: implies 127.0.0.1 + 9999 port<br>
   * - arguments:<br>
   * "addresse" "port"<br>
   * "addresse" "port" "default delay"<br>
   *
   * @param args
   *
   * @throws Exception
   */
  public static void main(final String[] args) throws Exception {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    int port = 9999;
    final InetAddress addr;
    long delay = LocalExecDefaultResult.MAXWAITPROCESS;
    if (args.length >= 2) {
      addr = InetAddress.getByName(args[0]);
      port = Integer.parseInt(args[1]);
      if (args.length > 2) {
        delay = Long.parseLong(args[2]);
      }
    } else {
      final byte[] loop = { 127, 0, 0, 1 };
      addr = InetAddress.getByAddress(loop);
    }
    // Configure the server.
    try {
      final ServerBootstrap bootstrap = new ServerBootstrap();
      WaarpNettyUtil
          .setServerBootstrap(bootstrap, workerGroup, workerGroup, 30000);

      // Configure the pipeline factory.
      bootstrap.childHandler(new LocalExecServerInitializer(delay, executor));

      // Bind and start to accept incoming connections only on local address.
      final ChannelFuture future =
          bootstrap.bind(new InetSocketAddress(addr, port));

      // Wait until the server socket is closed.
      future.channel().closeFuture().sync();
    } finally {
      // Shut down all event loops to terminate all threads.
      workerGroup.shutdownGracefully();

      // Wait until all threads are terminated.
      workerGroup.terminationFuture().sync();
    }
  }
}
