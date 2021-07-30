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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

public class HttpMonitoringExporterClientHandler
    extends SimpleChannelInboundHandler<HttpObject> {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpMonitoringExporterClientHandler.class);

  private final HttpMonitoringExporterClient httpMonitoringExporterClient;

  public HttpMonitoringExporterClientHandler(
      final HttpMonitoringExporterClient httpMonitoringExporterClient) {
    this.httpMonitoringExporterClient = httpMonitoringExporterClient;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx,
                              final HttpObject msg) throws Exception {
    if (msg instanceof HttpResponse) {
      final HttpResponse response = (HttpResponse) msg;

      final HttpResponseStatus status = response.status();

      httpMonitoringExporterClient.setStatus(
          response.status().code() == 200 || response.status().code() == 201);
    }
    if (msg instanceof LastHttpContent &&
        !httpMonitoringExporterClient.isKeepConnection()) {
      ctx.close();
    }
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx,
                              final Throwable cause) {
    logger.error(cause);
    httpMonitoringExporterClient.setStatus(false);
    ctx.channel().close();
  }
}
