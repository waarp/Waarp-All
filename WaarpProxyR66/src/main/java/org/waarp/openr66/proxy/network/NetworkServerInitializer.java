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
import org.waarp.openr66.proxy.configuration.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * NetworkServer pipeline (Requester side)
 *
 *
 */
public class NetworkServerInitializer
    extends org.waarp.openr66.protocol.networkhandler.NetworkServerInitializer {
  public static final String HANDLER = "handler";

  public NetworkServerInitializer(boolean server) {
    super(server);
  }

  @Override
  protected void initChannel(SocketChannel ch) {
    final ChannelPipeline pipeline = ch.pipeline();
    pipeline.addLast("codec", new NetworkPacketCodec());
    pipeline.addLast(TIMEOUT, new IdleStateHandler(0, 0,
                                                   Configuration.configuration
                                                       .getTIMEOUTCON(),
                                                   TimeUnit.MILLISECONDS));
    final GlobalTrafficShapingHandler handler =
        Configuration.configuration.getGlobalTrafficShapingHandler();
    if (handler != null) {
      pipeline.addLast(LIMITGLOBAL, handler);
    }
    pipeline.addLast(LIMITCHANNEL, new ChannelTrafficShapingHandler(
        Configuration.configuration.getServerChannelWriteLimit(),
        Configuration.configuration.getServerChannelReadLimit(),
        Configuration.configuration.getDelayLimit()));
    pipeline.addLast(Configuration.configuration.getHandlerGroup(), HANDLER,
                     new NetworkServerHandler(server));
  }

}
