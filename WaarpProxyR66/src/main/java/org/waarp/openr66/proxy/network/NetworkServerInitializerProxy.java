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

import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import org.waarp.openr66.protocol.networkhandler.NetworkServerInitializer;

import java.util.concurrent.TimeUnit;

import static org.waarp.openr66.protocol.configuration.Configuration.*;

/**
 * NetworkServer pipeline (Requester side)
 */
public class NetworkServerInitializerProxy extends NetworkServerInitializer {

  public NetworkServerInitializerProxy(final boolean server) {
    super(server);
  }

  @Override
  protected void initChannel(final SocketChannel ch) {
    final ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast(TIMEOUT, new IdleStateHandler(true, 0, 0,
                                                   configuration.getTimeoutCon(),
                                                   TimeUnit.MILLISECONDS));
    final GlobalTrafficShapingHandler handler =
        configuration.getGlobalTrafficShapingHandler();
    if (handler != null) {
      pipeline.addLast(LIMITGLOBAL, handler);
    }
    pipeline.addLast(LIMITCHANNEL, new ChannelTrafficShapingHandler(
        configuration.getServerChannelWriteLimit(),
        configuration.getServerChannelReadLimit(),
        configuration.getDelayLimit(), configuration.getTimeoutCon()));
    pipeline.addLast(NETWORK_CODEC, new NetworkPacketCodec());
    pipeline.addLast(configuration.getHandlerGroup(), NETWORK_HANDLER,
                     new NetworkServerHandler(server));
  }

}
