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
import io.netty.handler.codec.http.FullHttpRequest;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;

import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;

/**
 * Handles the dispatching of incoming request between version 1 and 2 of the
 * REST API. By default, the
 * pipeline continues with the v2 handler, but if the request URI does not match
 * the pattern of a v2 entry
 * point, then the pipeline will switch to the v1 handler.
 */
public class RestVersionHandler
    extends SimpleChannelInboundHandler<FullHttpRequest> {

  /**
   * Logger for all unexpected execution events.
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(RestVersionHandler.class);

  /**
   * Name of this handler in the Netty pipeline.
   */
  public static final String HANDLER_NAME = "version_handler";

  /**
   * Name of the RESTv1 handler in the Netty pipeline.
   */
  private static final String V1_HANDLER = "v1_handler";

  /**
   * The RESTv1 handler.
   */
  private final HttpRestR66Handler restV1Handler;

  /**
   * Initializes the REST version splitter and handler with the given {@link
   * RestConfiguration}
   *
   * @param restConfiguration the RestConfiguration object
   */
  public RestVersionHandler(RestConfiguration restConfiguration) {
    super(false);
    HttpRestR66Handler.instantiateHandlers(restConfiguration);
    restV1Handler = new HttpRestR66Handler(restConfiguration);
  }

  /**
   * Dispatches the incoming request to the corresponding v1 or v2 REST
   * handler.
   *
   * @param ctx the Netty pipeline context
   * @param request the incoming request
   */
  @Override
  protected void channelRead0(ChannelHandlerContext ctx,
                              FullHttpRequest request) {
    logger.debug(request.method() + " received on " + request.uri());

    if (request.uri().startsWith(VERSION_PREFIX)) {
      if (ctx.pipeline().get(V1_HANDLER) != null) {
        ctx.pipeline().remove(V1_HANDLER);
      }

      ctx.fireChannelRead(request);
    } else {
      if (ctx.pipeline().get(V1_HANDLER) == null) {
        ctx.pipeline().addAfter(HANDLER_NAME, V1_HANDLER, restV1Handler);
      }
      ctx.fireChannelRead(request);
    }
  }
}
