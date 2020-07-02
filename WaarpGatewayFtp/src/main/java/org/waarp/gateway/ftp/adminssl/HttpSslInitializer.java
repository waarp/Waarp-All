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
package org.waarp.gateway.ftp.adminssl;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.waarp.common.crypto.ssl.WaarpSecureKeyStore;
import org.waarp.common.crypto.ssl.WaarpSslContextFactory;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;

/**
 *
 */
public class HttpSslInitializer extends ChannelInitializer<SocketChannel> {
  public static WaarpSslContextFactory waarpSslContextFactory;
  public static WaarpSecureKeyStore waarpSecureKeyStore;
  private final boolean useHttpCompression;

  public HttpSslInitializer(boolean useHttpCompression) {
    this.useHttpCompression = useHttpCompression;
  }

  @Override
  protected void initChannel(SocketChannel ch) {
    final ChannelPipeline pipeline = ch.pipeline();
    // Add SSL handler first to encrypt and decrypt everything.
    final SslHandler sslhandler =
        waarpSslContextFactory.initInitializer(true, false);
    pipeline.addLast("ssl", sslhandler);

    pipeline.addLast("codec", new HttpServerCodec());
    pipeline.addLast("aggregator", new HttpObjectAggregator(
        FileBasedConfiguration.fileBasedConfiguration.getMaxGlobalMemory()));
    pipeline.addLast("streamer", new ChunkedWriteHandler());
    if (useHttpCompression) {
      pipeline.addLast("deflater", new HttpContentCompressor());
    }
    pipeline.addLast(
        FileBasedConfiguration.fileBasedConfiguration.getHttpPipelineExecutor(),
        "handler", new HttpSslHandler());
  }
}
