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
package org.waarp.openr66.proxy.network.ssl;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import org.waarp.openr66.protocol.networkhandler.ssl.NetworkSslServerInitializer;
import org.waarp.openr66.proxy.network.NetworkPacketCodec;

import java.util.concurrent.TimeUnit;

import static org.waarp.openr66.protocol.configuration.Configuration.*;
import static org.waarp.openr66.protocol.networkhandler.NetworkServerInitializer.*;

/**
 *
 */
public class NetworkSslServerInitializerProxy
    extends NetworkSslServerInitializer {
  /**
   * @param isClient True if this Factory is to be used in Client mode
   */
  public NetworkSslServerInitializerProxy(final boolean isClient) {
    super(isClient);
  }

  @Override
  protected void initChannel(final SocketChannel ch) throws Exception {
    final ChannelPipeline pipeline = ch.pipeline();
    // Add SSL handler first to encrypt and decrypt everything.
    final SslHandler sslHandler;
    if (isClient) {
      // Not server: no clientAuthent, no renegotiation
      sslHandler = getWaarpSslContextFactory().initInitializer(false, false);
    } else {
      // Server: no renegotiation still, but possible clientAuthent
      sslHandler = getWaarpSslContextFactory().initInitializer(true,
                                                               getWaarpSslContextFactory()
                                                                   .needClientAuthentication());
    }
    pipeline.addLast(SSL_HANDLER, sslHandler);

    pipeline.addLast(TIMEOUT, new IdleStateHandler(true, 0, 0, configuration
        .getTimeoutCon(), TimeUnit.MILLISECONDS));
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
                     new NetworkSslServerHandler(!isClient));
  }
}
