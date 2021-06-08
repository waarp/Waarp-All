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

package org.waarp.openr66.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.protocol.monitoring.MonitorExporterTransfers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class HttpServerExampleHandler
    extends SimpleChannelInboundHandler<FullHttpRequest> {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpServerExampleHandler.class);

  /**
   * Virtual Map of Logs
   */
  public static final Map<String, Map<String, JsonNode>> virtualMap =
      new HashMap<String, Map<String, JsonNode>>();

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx,
                              final FullHttpRequest request) throws Exception {
    HttpResponseStatus status = HttpResponseStatus.OK;
    // Decide whether to close the connection or not.
    final boolean keepAlive = HttpUtil.isKeepAlive(request);
    logger.info("Close ? Keep {} {}", keepAlive, request.method().toString());
    if (request.method() == HttpMethod.POST) {
      logger.debug("Recv {}", request.method());
      final ByteBuf content = request.content();
      final String param = content.toString(WaarpStringUtils.UTF8);
      logger.debug(param);
      ObjectNode objectNode = JsonHandler.getFromString(param);
      ArrayNode arrayNode =
          (ArrayNode) objectNode.get(MonitorExporterTransfers.RESULTS);
      String hostId =
          request.headers().get(MonitorExporterTransfers.HEADER_WAARP_ID);
      Map<String, JsonNode> serverMap = virtualMap.get(hostId);
      if (serverMap == null) {
        serverMap = new HashMap<String, JsonNode>();
        virtualMap.put(hostId, serverMap);
      }
      Iterator<JsonNode> iterator = arrayNode.elements();
      while (iterator.hasNext()) {
        JsonNode jsonNode = iterator.next().deepCopy();
        String uniqueId =
            jsonNode.get(MonitorExporterTransfers.UNIQUE_ID).asText();
        serverMap.put(uniqueId, jsonNode);
      }
      logger.warn("Receive monitoring from {} ({} servers referenced) with " +
                  "{} transfers; now have {} transfers", hostId,
                  virtualMap.size(), arrayNode.size(), serverMap.size());
    } else {
      logger.warn(request.method() + " " + request.uri());
      logger.warn(request.headers().toString());
      status = HttpResponseStatus.BAD_REQUEST;
    }
    // Build the response object.
    final FullHttpResponse response =
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
    response.headers().add(HttpHeaderNames.CONTENT_LENGTH, 0);
    if (keepAlive) {
      response.headers()
              .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }
    final String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
    if (cookieString != null) {
      final Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
      boolean i18nextFound = false;
      if (!cookies.isEmpty()) {
        // Reset the cookies if necessary.
        for (final Cookie cookie : cookies) {
          response.headers().add(HttpHeaderNames.SET_COOKIE,
                                 ServerCookieEncoder.LAX.encode(cookie));
        }
      }
    }
    // Write the response.
    final ChannelFuture future = ctx.writeAndFlush(response);
    // Close the connection after the write operation is done if necessary.
    if (!keepAlive) {
      logger.warn("Close connection");
      future.addListener(ChannelFutureListener.CLOSE);
    }
  }
}
