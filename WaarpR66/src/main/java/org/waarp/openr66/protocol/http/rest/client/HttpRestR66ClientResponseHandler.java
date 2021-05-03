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
package org.waarp.openr66.protocol.http.rest.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler.COMMAND_TYPE;
import org.waarp.gateway.kernel.rest.RestArgument;
import org.waarp.gateway.kernel.rest.client.HttpRestClientSimpleResponseHandler;
import org.waarp.gateway.kernel.rest.client.RestFuture;
import org.waarp.openr66.protocol.http.rest.handler.HttpRestAbstractR66Handler.ACTIONS_TYPE;

import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.UnsupportedCharsetException;

/**
 * Rest client response handler.
 * <p>
 * Note: by default, no connection are closed except in case of error or if in
 * HTTP 1.0 or explicitly to be
 * closed.
 */
public abstract class HttpRestR66ClientResponseHandler
    extends SimpleChannelInboundHandler<HttpObject> {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpRestR66ClientResponseHandler.class);

  private ByteBuf cumulativeBody;
  protected JsonNode jsonObject;

  protected void addContent(final FullHttpResponse response)
      throws HttpIncorrectRequestException {
    final ByteBuf content = response.content();
    if (content != null && content.isReadable()) {
      content.retain();
      if (cumulativeBody != null) {
        cumulativeBody = Unpooled.wrappedBuffer(cumulativeBody, content);
      } else {
        cumulativeBody = content;
      }
      // get the Json equivalent of the Body
      try {
        final String json = cumulativeBody.toString(WaarpStringUtils.UTF8);
        jsonObject = JsonHandler.getFromString(json);
      } catch (final UnsupportedCharsetException e2) {
        logger.warn("Error" + " : {}", e2.getMessage());
        throw new HttpIncorrectRequestException(e2);
      }
      cumulativeBody = null;
    }
  }

  /**
   * Setting the RestArgument to the RestFuture and validating RestFuture.
   *
   * @param channel
   *
   * @throws HttpInvalidAuthenticationException
   */
  protected void actionFromResponse(final Channel channel)
      throws HttpInvalidAuthenticationException {
    boolean includeValidation = false;
    final RestArgument ra = new RestArgument((ObjectNode) jsonObject);
    if (jsonObject == null) {
      logger.debug("Recv: EMPTY");
    }
    final RestFuture restFuture =
        channel.attr(HttpRestClientSimpleResponseHandler.RESTARGUMENT).get();
    restFuture.setRestArgument(ra);
    switch (ra.getMethod()) {
      case DELETE:
        includeValidation = delete(channel, ra);
        break;
      case GET:
        includeValidation = get(channel, ra);
        break;
      case OPTIONS:
        includeValidation = options(channel, ra);
        break;
      case POST:
        includeValidation = post(channel, ra);
        break;
      case PUT:
        includeValidation = put(channel, ra);
        break;
      default:
        break;
    }
    if (!includeValidation) {
      // finalize the future
      restFuture.setSuccess();
    }
  }

  /**
   * Method calls when a action REST command is raised as answer
   *
   * @param channel
   * @param ra
   * @param act
   *
   * @return if validation is done (or suppose to be)
   *
   * @throws HttpInvalidAuthenticationException
   */
  protected abstract boolean action(Channel channel, RestArgument ra,
                                    ACTIONS_TYPE act)
      throws HttpInvalidAuthenticationException;

  /**
   * Method calls when a REST Get command is raised as answer
   *
   * @param channel
   * @param ra
   *
   * @return if validation is done (or suppose to be)
   *
   * @throws HttpInvalidAuthenticationException
   */
  protected abstract boolean afterDbGet(Channel channel, RestArgument ra)
      throws HttpInvalidAuthenticationException;

  /**
   * Method calls when a REST Post command is raised as answer
   *
   * @param channel
   * @param ra
   *
   * @return if validation is done (or suppose to be)
   *
   * @throws HttpInvalidAuthenticationException
   */
  protected abstract boolean afterDbPost(Channel channel, RestArgument ra)
      throws HttpInvalidAuthenticationException;

  /**
   * Method calls when a REST Put command is raised as answer
   *
   * @param channel
   * @param ra
   *
   * @return if validation is done (or suppose to be)
   *
   * @throws HttpInvalidAuthenticationException
   */
  protected abstract boolean afterDbPut(Channel channel, RestArgument ra)
      throws HttpInvalidAuthenticationException;

  /**
   * Method calls when a REST Delete command is raised as answer
   *
   * @param channel
   * @param ra
   *
   * @return if validation is done (or suppose to be)
   *
   * @throws HttpInvalidAuthenticationException
   */
  protected abstract boolean afterDbDelete(Channel channel, RestArgument ra)
      throws HttpInvalidAuthenticationException;

  /**
   * Method calls when a REST GetMultiple command is raised as answer
   *
   * @param channel
   * @param ra
   *
   * @return if validation is done (or suppose to be)
   *
   * @throws HttpInvalidAuthenticationException
   */
  protected abstract boolean afterDbGetMultiple(Channel channel,
                                                RestArgument ra)
      throws HttpInvalidAuthenticationException;

  /**
   * Method calls when a REST Options command is raised as answer
   *
   * @param channel
   * @param ra
   *
   * @return if validation is done (or suppose to be)
   *
   * @throws HttpInvalidAuthenticationException
   */
  protected abstract boolean afterDbOptions(Channel channel, RestArgument ra)
      throws HttpInvalidAuthenticationException;

  /**
   * Method calls when a REST command is in error
   *
   * @param channel
   * @param ra (might be null)
   *
   * @return if validation is done (or suppose to be)
   *
   * @throws HttpInvalidAuthenticationException
   */
  protected abstract boolean afterError(Channel channel, RestArgument ra)
      throws HttpInvalidAuthenticationException;

  protected boolean get(final Channel channel, final RestArgument ra)
      throws HttpInvalidAuthenticationException {
    if (logger.isDebugEnabled()) {
      logger.debug(ra.prettyPrint());
    }
    if (ra.getCommand() == COMMAND_TYPE.GET) {
      return afterDbGet(channel, ra);
    } else if (ra.getCommand() == COMMAND_TYPE.MULTIGET) {
      return afterDbGetMultiple(channel, ra);
    } else {
      final String cmd = ra.getCommandField();
      try {
        final ACTIONS_TYPE act = ACTIONS_TYPE.valueOf(cmd);
        return action(channel, ra, act);
      } catch (final Exception e) {
        return false;
      }
    }
  }

  protected boolean put(final Channel channel, final RestArgument ra)
      throws HttpInvalidAuthenticationException {
    if (logger.isDebugEnabled()) {
      logger.debug(ra.prettyPrint());
    }
    if (ra.getCommand() == COMMAND_TYPE.UPDATE) {
      return afterDbPut(channel, ra);
    } else {
      final String cmd = ra.getCommandField();
      try {
        final ACTIONS_TYPE act = ACTIONS_TYPE.valueOf(cmd);
        return action(channel, ra, act);
      } catch (final Exception e) {
        return false;
      }
    }
  }

  protected boolean post(final Channel channel, final RestArgument ra)
      throws HttpInvalidAuthenticationException {
    if (logger.isDebugEnabled()) {
      logger.debug(ra.prettyPrint());
    }
    if (ra.getCommand() == COMMAND_TYPE.CREATE) {
      return afterDbPost(channel, ra);
    } else {
      final String cmd = ra.getCommandField();
      try {
        final ACTIONS_TYPE act = ACTIONS_TYPE.valueOf(cmd);
        return action(channel, ra, act);
      } catch (final Exception e) {
        return false;
      }
    }
  }

  protected boolean delete(final Channel channel, final RestArgument ra)
      throws HttpInvalidAuthenticationException {
    if (logger.isDebugEnabled()) {
      logger.debug(ra.prettyPrint());
    }
    if (ra.getCommand() == COMMAND_TYPE.DELETE) {
      return afterDbDelete(channel, ra);
    } else {
      final String cmd = ra.getCommandField();
      try {
        final ACTIONS_TYPE act = ACTIONS_TYPE.valueOf(cmd);
        return action(channel, ra, act);
      } catch (final Exception e) {
        return false;
      }
    }
  }

  protected boolean options(final Channel channel, final RestArgument ra)
      throws HttpInvalidAuthenticationException {
    if (logger.isDebugEnabled()) {
      logger.debug(ra.prettyPrint());
    }
    if (ra.getCommand() == COMMAND_TYPE.OPTIONS) {
      return afterDbOptions(channel, ra);
    } else {
      final String cmd = ra.getCommandField();
      try {
        final ACTIONS_TYPE act = ACTIONS_TYPE.valueOf(cmd);
        return action(channel, ra, act);
      } catch (final Exception e) {
        return false;
      }
    }
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx,
                              final HttpObject msg) throws Exception {
    if (msg instanceof HttpResponse) {
      final HttpResponse response = (HttpResponse) msg;
      final HttpResponseStatus status = response.status();
      logger.debug("{}: {} STATUS: {}", HttpHeaderNames.REFERER,
                   response.headers().get(HttpHeaderNames.REFERER), status);
      if (response.status().code() != 200) {
        if (response instanceof FullHttpResponse) {
          addContent((FullHttpResponse) response);
        }
        RestArgument ra = null;
        if (jsonObject != null) {
          ra = new RestArgument((ObjectNode) jsonObject);
          final RestFuture restFuture = ctx.channel().attr(
              HttpRestClientSimpleResponseHandler.RESTARGUMENT).get();
          restFuture.setRestArgument(ra);
          logger.error("Error: " + response.status().code() + ' ' +
                       response.status().reasonPhrase() + '\n' +
                       ra.prettyPrint());
        } else {
          logger.error("Error: " + response.status().code() + ' ' +
                       response.status().reasonPhrase());
        }
        if (!afterError(ctx.channel(), ra)) {
          final RestFuture restFuture = ctx.channel().attr(
              HttpRestClientSimpleResponseHandler.RESTARGUMENT).get();
          restFuture.cancel();
        }
        if (ctx.channel().isActive()) {
          logger.debug("Will close");
          WaarpSslUtility.closingSslChannel(ctx.channel());
        }
      } else {
        if (response instanceof FullHttpResponse) {
          addContent((FullHttpResponse) response);
          actionFromResponse(ctx.channel());
        }
      }
    } else {
      final HttpContent chunk = (HttpContent) msg;
      if (chunk instanceof LastHttpContent) {
        final ByteBuf content = chunk.content();
        if (content != null && content.isReadable()) {
          content.retain();
          if (cumulativeBody != null) {
            cumulativeBody = Unpooled.wrappedBuffer(cumulativeBody, content);
          } else {
            cumulativeBody = content;
          }
        }
        // get the Json equivalent of the Body
        if (cumulativeBody == null) {
          jsonObject = JsonHandler.createObjectNode();
        } else {
          try {
            final String json = cumulativeBody.toString(WaarpStringUtils.UTF8);
            jsonObject = JsonHandler.getFromString(json);
          } catch (final Throwable e2) {
            logger.warn("Error" + " : {}", e2.getMessage());
            throw new HttpIncorrectRequestException(e2);
          }
          cumulativeBody.release();
          cumulativeBody = null;
        }
        actionFromResponse(ctx.channel());
      } else {
        final ByteBuf content = chunk.content();
        if (content != null && content.isReadable()) {
          content.retain();
          if (cumulativeBody != null) {
            cumulativeBody = Unpooled.wrappedBuffer(cumulativeBody, content);
          } else {
            cumulativeBody = content;
          }
        }
      }
    }
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx,
                              final Throwable cause) {
    final RestFuture restFuture =
        ctx.channel().attr(HttpRestClientSimpleResponseHandler.RESTARGUMENT)
           .get();
    if (cause instanceof ClosedChannelException) {
      logger.debug("Close before ending");
      restFuture.setFailure(cause);
      return;
    } else if (cause instanceof ConnectException) {
      if (ctx.channel().isActive()) {
        logger.debug("Will close");
        restFuture.setFailure(cause);
        WaarpSslUtility.closingSslChannel(ctx.channel());
      }
      return;
    }
    logger.warn("Error: {}", cause.getMessage());
    if (ctx.channel() != null && restFuture != null) {
      restFuture.setFailure(cause);
    }
    logger.debug("Will close");
    WaarpSslUtility.closingSslChannel(ctx.channel());
  }

}
