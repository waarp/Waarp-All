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
package org.waarp.openr66.protocol.http.rest.handler;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.FileUpload;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.rest.HttpRestHandler;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;
import org.waarp.gateway.kernel.rest.RestArgument;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.gateway.kernel.rest.RestMethodHandler;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;
import org.waarp.openr66.protocol.localhandler.packet.json.JsonPacket;

import java.io.IOException;
import java.nio.charset.UnsupportedCharsetException;

import static org.waarp.openr66.context.R66FiniteDualStates.*;

/**
 * Common method implementation for Action Rest R66 handlers
 */
public abstract class HttpRestAbstractR66Handler extends RestMethodHandler {

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpRestAbstractR66Handler.class);

  public enum ACTIONS_TYPE {
    OPTIONS, GetBandwidth, SetBandwidth, ExecuteBusiness, ExportConfig,
    ImportConfig, GetInformation, GetTransferInformation, GetLog,
    ShutdownOrBlock, GetStatus, RestartTransfer, StopOrCancelTransfer,
    CreateTransfer
  }

  /**
   * @param path
   * @param method
   */
  protected HttpRestAbstractR66Handler(final String path,
                                       final RestConfiguration config,
                                       final METHOD... method) {
    super(path, path, true, config, method);
  }

  @Override
  public void checkHandlerSessionCorrectness(final HttpRestHandler handler,
                                             final RestArgument arguments,
                                             final RestArgument result) {
    // no check to do here ?
    logger.debug("debug");
  }

  @Override
  public void getFileUpload(final HttpRestHandler handler,
                            final FileUpload data, final RestArgument arguments,
                            final RestArgument result)
      throws HttpIncorrectRequestException {
    // should not be
    logger.debug(
        "debug: " + data.getName() + ':' + data.getHttpDataType().name());
  }

  protected void setError(final HttpRestHandler handler,
                          final RestArgument result,
                          final HttpResponseStatus code) {
    handler.setStatus(HttpResponseStatus.BAD_REQUEST);
    handler.setWillClose(true);
    result.setResult(code);
  }

  protected void setError(final HttpRestHandler handler,
                          final RestArgument result, final JsonPacket packet,
                          final HttpResponseStatus code) {
    handler.setStatus(HttpResponseStatus.BAD_REQUEST);
    result.setResult(code);
    if (packet != null) {
      try {
        result.addResult(packet.createObjectNode());
      } catch (final OpenR66ProtocolPacketException ignored) {
        // ignore
      }
    }
  }

  protected void setOk(final HttpRestHandler handler, final RestArgument result,
                       final JsonPacket packet, final HttpResponseStatus code) {
    handler.setStatus(HttpResponseStatus.OK);
    result.setResult(code);
    if (packet != null) {
      try {
        result.addResult(packet.createObjectNode());
      } catch (final OpenR66ProtocolPacketException e) {
        result.setDetail("serialization impossible");
      }
    }
  }

  @Override
  public HttpResponseStatus handleException(final HttpRestHandler handler,
                                            final RestArgument arguments,
                                            final RestArgument result,
                                            final Object body,
                                            final Exception exception) {
    ((HttpRestR66Handler) handler).getServerHandler().getSession()
                                  .newState(ERROR);
    return super.handleException(handler, arguments, result, body, exception);
  }

  @Override
  public ChannelFuture sendResponse(final HttpRestHandler handler,
                                    final ChannelHandlerContext ctx,
                                    final RestArgument arguments,
                                    final RestArgument result,
                                    final Object body,
                                    final HttpResponseStatus status) {
    final String answer = result.toString();
    final ByteBuf buffer =
        Unpooled.wrappedBuffer(answer.getBytes(WaarpStringUtils.UTF8));
    final HttpResponse response = handler.getResponse(buffer);
    response.headers()
            .set(HttpHeaderNames.CONTENT_LENGTH, buffer.readableBytes());
    if (status == HttpResponseStatus.UNAUTHORIZED) {
      return ctx.writeAndFlush(response);
    }
    response.headers().add(HttpHeaderNames.CONTENT_TYPE, "application/json");
    response.headers().add(HttpHeaderNames.REFERER, handler.getRequest().uri());
    logger.debug("Will write: {}", body);
    final ChannelFuture future = ctx.writeAndFlush(response);
    if (handler.isWillClose()) {
      logger.debug("Will close session in HttpRestAbstractR66Handler");
      return future;
    }
    return null;
  }

  @Override
  public Object getBody(final HttpRestHandler handler, final ByteBuf body,
                        final RestArgument arguments, final RestArgument result)
      throws HttpIncorrectRequestException {
    final JsonPacket packet;
    try {
      final String json = body.toString(WaarpStringUtils.UTF8);
      packet = JsonPacket.createFromBuffer(json);
    } catch (final JsonParseException e) {
      logger.warn("Error: " + body.toString(WaarpStringUtils.UTF8), e);
      throw new HttpIncorrectRequestException(e);
    } catch (final JsonMappingException e) {
      logger.warn("Error", e);
      throw new HttpIncorrectRequestException(e);
    } catch (final IOException e) {
      logger.warn("Error", e);
      throw new HttpIncorrectRequestException(e);
    } catch (final UnsupportedCharsetException e) {
      logger.warn("Error", e);
      throw new HttpIncorrectRequestException(e);
    }
    return packet;
  }
}
