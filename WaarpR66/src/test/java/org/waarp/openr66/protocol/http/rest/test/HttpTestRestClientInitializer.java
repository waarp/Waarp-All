/*******************************************************************************
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 *  individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at your
 *  option) any later version.
 *
 *  Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 *  A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  Waarp . If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.waarp.openr66.protocol.http.rest.test;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.waarp.common.crypto.ssl.WaarpSslContextFactory;

/**
 * Test Rest client pipeline factory
 */
public class HttpTestRestClientInitializer
    extends ChannelInitializer<SocketChannel> {
  private final WaarpSslContextFactory waarpSslContextFactory;

  public HttpTestRestClientInitializer(
      WaarpSslContextFactory waarpSslContextFactory) {
    this.waarpSslContextFactory = waarpSslContextFactory;
  }

  protected void initChannel(SocketChannel ch) throws Exception {
    // Create a default pipeline implementation.
    ChannelPipeline pipeline = ch.pipeline();

    // Enable HTTPS if necessary.
    if (waarpSslContextFactory != null) {
      SslHandler handler = waarpSslContextFactory.initInitializer(false, false);
      pipeline.addLast("ssl", handler);
    }

    pipeline.addLast("codec", new HttpClientCodec());
    // Remove the following line if you don't want automatic content
    // decompression.
    pipeline.addLast("inflater", new HttpContentDecompressor());

    // to be used since huge file transfer
    pipeline.addLast("streamer", new ChunkedWriteHandler());

    pipeline.addLast("handler", new HttpTestResponseHandler());
  }
}
