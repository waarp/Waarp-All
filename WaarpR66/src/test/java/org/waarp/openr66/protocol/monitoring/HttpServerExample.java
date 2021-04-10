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

package org.waarp.openr66.protocol.monitoring;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.group.ChannelGroup;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.net.InetSocketAddress;

public class HttpServerExample {
  public static ChannelGroup group = null;

  public HttpServerExample(int port) {
    final ServerBootstrap httpBootstrap = new ServerBootstrap();
    WaarpNettyUtil.setServerBootstrap(httpBootstrap, Configuration.configuration
                                          .getHttpWorkerGroup(), Configuration.configuration.getHttpWorkerGroup(),
                                      (int) Configuration.configuration
                                          .getTimeoutCon());
    httpBootstrap.childHandler(new HttpServerExampleInitializer());
    // Bind and start to accept incoming connections.
    final ChannelFuture future;
    future = httpBootstrap.bind(new InetSocketAddress(port));
    try {
      future.await();
    } catch (final InterruptedException e) {//NOSONAR
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    }
    if (group == null) {
      group = Configuration.configuration.getHttpChannelGroup();
    }
    if (future.isSuccess()) {
      group.add(future.channel());
    }
  }
}
