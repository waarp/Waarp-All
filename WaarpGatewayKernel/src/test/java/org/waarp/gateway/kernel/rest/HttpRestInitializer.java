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
package org.waarp.gateway.kernel.rest;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import org.waarp.common.utility.WaarpThreadFactory;

/**
 *
 */
public class HttpRestInitializer extends ChannelInitializer<SocketChannel> {
  private final RestConfiguration restConfiguration;
  private static final EventExecutorGroup executor =
      new NioEventLoopGroup(10, new WaarpThreadFactory("Handler"));

  /**
   * @param restConfiguration
   */
  public HttpRestInitializer(RestConfiguration restConfiguration) {
    this.restConfiguration = restConfiguration;
  }

  @Override
  protected void initChannel(SocketChannel ch) throws Exception {
    // Create a default pipeline implementation.
    final ChannelPipeline pipeline = ch.pipeline();

    pipeline.addLast("codec", new HttpServerCodec());
    pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
    final HttpRestHandlerTest r66handler =
        new HttpRestHandlerTest(restConfiguration);
    pipeline.addLast(executor, "handler", r66handler);
  }

}
