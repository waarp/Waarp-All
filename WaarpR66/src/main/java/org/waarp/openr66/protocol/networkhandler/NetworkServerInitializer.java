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
package org.waarp.openr66.protocol.networkhandler;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNetworkException;
import org.waarp.openr66.protocol.networkhandler.packet.NetworkPacketCodec;

import java.util.concurrent.TimeUnit;

/**
 * NetworkServer pipeline (Requester side)
 */
public class NetworkServerInitializer
    extends ChannelInitializer<SocketChannel> {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(NetworkServerInitializer.class);

  public static final String TIMEOUT = "timeout";
  public static final String LIMITGLOBAL = "GLOBALLIMIT";
  public static final String LIMITCHANNEL = "CHANNELLIMIT";
  public static final String NETWORK_CODEC = "codec";
  public static final String NETWORK_HANDLER = "handler";

  protected final boolean server;

  public NetworkServerInitializer(boolean server) {
    this.server = server;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    final ChannelPipeline pipeline = ch.pipeline();
    logger.debug("Create IdleStateHandler with {} ms",
                 Configuration.configuration.getTimeoutCon());
    pipeline.addLast(TIMEOUT, new IdleStateHandler(true, 0, 0,
                                                   Configuration.configuration
                                                       .getTimeoutCon(),
                                                   TimeUnit.MILLISECONDS));
    // Global limitation
    final GlobalTrafficShapingHandler handler =
        Configuration.configuration.getGlobalTrafficShapingHandler();
    if (handler == null) {
      throw new OpenR66ProtocolNetworkException(
          "Error at pipeline initialization," +
          " GlobalTrafficShapingHandler configured.");
    }
    pipeline.addLast(LIMITGLOBAL, handler);
    // Per channel limitation
    pipeline.addLast(LIMITCHANNEL, new ChannelTrafficShapingHandler(
        Configuration.configuration.getServerChannelWriteLimit(),
        Configuration.configuration.getServerChannelReadLimit(),
        Configuration.configuration.getDelayLimit(),
        Configuration.configuration.getTimeoutCon()));
    pipeline.addLast(NETWORK_CODEC, new NetworkPacketCodec());
    pipeline
        .addLast(Configuration.configuration.getHandlerGroup(), NETWORK_HANDLER,
                 new NetworkServerHandler(server));
  }
}
