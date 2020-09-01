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

package org.waarp.openr66.protocol.http.restv2.resthandlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import org.waarp.common.crypto.HmacSha256;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.AbstractRestDbHandler;

import static io.netty.channel.ChannelFutureListener.*;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;
import static javax.ws.rs.core.HttpHeaders.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;

/**
 * Handler checking the REST request signature when signature checking is
 * enabled.
 */
public class RestSignatureHandler
    extends SimpleChannelInboundHandler<FullHttpRequest> {

  /**
   * The logger for all events during the execution.
   */
  private final WaarpLogger logger = WaarpLoggerFactory.getLogger(getClass());

  /**
   * The HMAC key used to create the request's signature.
   */
  private final HmacSha256 hmac;

  /**
   * Initializes the handler with the given HMAC key.
   *
   * @param hmac The REST HMAC signing key.
   */
  public RestSignatureHandler(final HmacSha256 hmac) {
    this.hmac = hmac;
  }

  /**
   * Checks if the request given as parameter by the channel pipeline is
   * properly signed or not. If the
   * signature is valid, the request is forwarded to the corresponding {@link
   * AbstractRestDbHandler}, otherwise
   * a reply is directly sent stating that the request needs to be signed. If
   * an unexpected error occurs during
   * the execution, an error 500 HTTP status is sent instead.
   *
   * @param ctx The context of the Netty channel handler.
   * @param request The original HTTP request.
   */
  @Override
  protected void channelRead0(final ChannelHandlerContext ctx,
                              final FullHttpRequest request) {

    // If the request does not have a body, skip the signature checking.
    if (!request.content().isReadable()) {
      ctx.fireChannelRead(request.retain());
      return;
    }

    final String authent = request.headers().get(AUTHORIZATION);
    final String body = request.content().toString(UTF8_CHARSET);
    final String URI = request.uri();
    final String method = request.method().toString();
    final String sign = request.headers().get(AUTH_SIGNATURE);

    final FullHttpResponse response;

    if (authent == null || sign == null) {
      response = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED);
      ctx.channel().writeAndFlush(response).addListener(CLOSE);
      return;
    }

    final String computedHash;
    try {
      computedHash = hmac.cryptToHex(authent + body + URI + method);
    } catch (final Exception e) {
      logger.error(e);
      response = new DefaultFullHttpResponse(HTTP_1_1, INTERNAL_SERVER_ERROR);
      ctx.channel().writeAndFlush(response).addListener(CLOSE);
      return;
    }

    if (!computedHash.equals(sign)) {
      response = new DefaultFullHttpResponse(HTTP_1_1, UNAUTHORIZED);
      ctx.channel().writeAndFlush(response).addListener(CLOSE);
    }

    ctx.fireChannelRead(request.retain());
  }

}
