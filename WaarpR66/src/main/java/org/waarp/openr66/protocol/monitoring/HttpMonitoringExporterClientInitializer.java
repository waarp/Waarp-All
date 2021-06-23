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

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.ssl.SslContext;

public class HttpMonitoringExporterClientInitializer
    extends ChannelInitializer<SocketChannel> {
  private final SslContext sslCtx;
  private final HttpMonitoringExporterClient httpMonitoringExporterClient;

  public HttpMonitoringExporterClientInitializer(final SslContext sslCtx,
                                                 final HttpMonitoringExporterClient httpMonitoringExporterClient) {
    this.sslCtx = sslCtx;
    this.httpMonitoringExporterClient = httpMonitoringExporterClient;
  }

  @Override
  protected void initChannel(final SocketChannel ch) throws Exception {
    ChannelPipeline pipeline = ch.pipeline();

    if (sslCtx != null) {
      pipeline.addLast("ssl", sslCtx.newHandler(ch.alloc()));
    }
    pipeline.addLast("codec", new HttpClientCodec());
    pipeline.addLast("handler", new HttpMonitoringExporterClientHandler(
        httpMonitoringExporterClient));
  }
}
