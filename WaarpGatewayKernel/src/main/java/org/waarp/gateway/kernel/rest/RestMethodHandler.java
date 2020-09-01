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

import com.fasterxml.jackson.databind.node.ArrayNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.FileUpload;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.gateway.kernel.exception.HttpForbiddenRequestException;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.exception.HttpMethodNotAllowedRequestException;
import org.waarp.gateway.kernel.exception.HttpNotFoundRequestException;
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler.COMMAND_TYPE;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Rest Method handler (used by Http Rest Handler)
 */
public abstract class RestMethodHandler {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(RestMethodHandler.class);
  private static final METHOD[] METHOD_0_LENGTH = new METHOD[0];

  protected final String name;
  protected final String path;
  protected final Set<METHOD> methods;
  protected final boolean isBodyJsonDecode;
  protected final RestConfiguration restConfiguration;

  /**
   * @param name name associated with this Method Handler (to enable
   *     some
   *     HashMap or Enum
   *     classification)
   * @param path associated base Path
   * @param isBodyJsonDecode Is this method Handler using a Json as
   *     Body
   * @param config the associated configuration
   * @param method the associated methods
   */
  protected RestMethodHandler(final String name, final String path,
                              final boolean isBodyJsonDecode,
                              final RestConfiguration config,
                              final METHOD... method) {
    this.name = name;
    this.path = path;
    methods = new HashSet<HttpRestHandler.METHOD>();
    setMethods(method);
    setMethods(METHOD.OPTIONS);
    this.isBodyJsonDecode = isBodyJsonDecode;
    restConfiguration = config;
  }

  protected void setMethods(final METHOD... method) {
    Collections.addAll(methods, method);
  }

  /**
   * Will assign the intersection of both set of Methods
   *
   * @param selectedMethods the selected Methods among available
   * @param validMethod the validMethod for this handler
   */
  protected void setIntersectionMethods(final METHOD[] selectedMethods,
                                        final METHOD... validMethod) {
    final Set<METHOD> set = new HashSet<METHOD>();
    Collections.addAll(set, validMethod);
    final Set<METHOD> set2 = new HashSet<METHOD>();
    Collections.addAll(set2, selectedMethods);
    set.retainAll(set2);
    final METHOD[] methodsToSet = set.toArray(METHOD_0_LENGTH);
    setMethods(methodsToSet);
  }

  public String getName() {
    return name;
  }

  public String getPath() {
    return path;
  }

  /**
   * @param method
   *
   * @return True if the Method is valid for this Handler
   */
  public boolean isMethodIncluded(final METHOD method) {
    return methods.contains(method);
  }

  /**
   * Check the session (arguments, result) vs handler correctness, called
   * before
   * any BODY elements but after URI
   * and HEADER.
   *
   * @param handler
   * @param arguments
   * @param result
   *
   * @throws HttpForbiddenRequestException
   */
  public abstract void checkHandlerSessionCorrectness(HttpRestHandler handler,
                                                      RestArgument arguments,
                                                      RestArgument result)
      throws HttpForbiddenRequestException;

  /**
   * Get a new Http Uploaded File from BODY
   *
   * @param handler
   * @param data
   * @param arguments
   * @param result
   *
   * @throws HttpIncorrectRequestException
   */
  public abstract void getFileUpload(HttpRestHandler handler, FileUpload data,
                                     RestArgument arguments,
                                     RestArgument result)
      throws HttpIncorrectRequestException;

  /**
   * Get data from BODY (supposedly a Json)
   *
   * @param handler
   * @param body
   * @param arguments
   * @param result
   *
   * @return the object related to BODY decoding
   *
   * @throws HttpIncorrectRequestException
   */
  public abstract Object getBody(HttpRestHandler handler, ByteBuf body,
                                 RestArgument arguments, RestArgument result)
      throws HttpIncorrectRequestException;

  /**
   * Called when all Data were passed to the handler
   *
   * @param handler
   * @param arguments
   * @param result
   * @param body
   *
   * @throws HttpIncorrectRequestException
   * @throws HttpNotFoundRequestException
   */
  public abstract void endParsingRequest(HttpRestHandler handler,
                                         RestArgument arguments,
                                         RestArgument result, Object body)
      throws HttpIncorrectRequestException, HttpInvalidAuthenticationException,
             HttpNotFoundRequestException;

  /**
   * Called when an exception occurs
   *
   * @param handler
   * @param arguments
   * @param result
   * @param body
   * @param exception
   *
   * @return the status to used in sendReponse
   */
  public HttpResponseStatus handleException(final HttpRestHandler handler,
                                            final RestArgument arguments,
                                            final RestArgument result,
                                            final Object body,
                                            final Exception exception) {
    if (exception instanceof HttpInvalidAuthenticationException) {
      result.setResult(HttpResponseStatus.UNAUTHORIZED);
      return HttpResponseStatus.UNAUTHORIZED;
    } else if (exception instanceof HttpForbiddenRequestException) {
      result.setResult(HttpResponseStatus.FORBIDDEN);
      return HttpResponseStatus.FORBIDDEN;
    } else if (exception instanceof HttpIncorrectRequestException) {
      result.setResult(HttpResponseStatus.BAD_REQUEST);
      return HttpResponseStatus.BAD_REQUEST;
    } else if (exception instanceof HttpMethodNotAllowedRequestException) {
      result.setResult(HttpResponseStatus.METHOD_NOT_ALLOWED);
      return HttpResponseStatus.METHOD_NOT_ALLOWED;
    } else if (exception instanceof HttpNotFoundRequestException) {
      result.setResult(HttpResponseStatus.NOT_FOUND);
      return HttpResponseStatus.NOT_FOUND;
    } else {
      result.setResult(HttpResponseStatus.INTERNAL_SERVER_ERROR);
      return HttpResponseStatus.INTERNAL_SERVER_ERROR;
    }
  }

  /**
   * Send a response (correct or not)
   *
   * @param handler
   * @param ctx
   * @param arguments
   * @param result
   * @param body
   * @param status
   *
   * @return The ChannelFuture if this response will need the channel to be
   *     closed, else null
   */
  public abstract ChannelFuture sendResponse(HttpRestHandler handler,
                                             ChannelHandlerContext ctx,
                                             RestArgument arguments,
                                             RestArgument result, Object body,
                                             HttpResponseStatus status);

  protected ChannelFuture sendOptionsResponse(final HttpRestHandler handler,
                                              final ChannelHandlerContext ctx,
                                              final RestArgument result,
                                              final HttpResponseStatus status) {
    final String list = result.getAllowOption();
    final String answer = result.toString();
    final ByteBuf buffer =
        Unpooled.wrappedBuffer(answer.getBytes(WaarpStringUtils.UTF8));
    final HttpResponse response = handler.getResponse(buffer);
    if (status == HttpResponseStatus.UNAUTHORIZED) {
      return ctx.writeAndFlush(response);
    }
    response.headers().add(HttpHeaderNames.CONTENT_TYPE, "application/json");
    response.headers().add(HttpHeaderNames.REFERER, handler.getRequest().uri());
    response.headers().add(HttpHeaderNames.ALLOW, list);
    logger.debug("Msg ready");
    final ChannelFuture future = ctx.writeAndFlush(response);
    if (handler.isWillClose()) {
      SysErrLogger.FAKE_LOGGER
          .sysout("Will close session in RestMethodHandler");
      return future;
    }
    return null;
  }

  /**
   * Options command that all handler should implement
   *
   * @param handler
   * @param arguments
   * @param result
   */
  protected void optionsCommand(final HttpRestHandler handler,
                                final RestArgument arguments,
                                final RestArgument result) {
    result.setCommand(COMMAND_TYPE.OPTIONS);
    final METHOD[] realmethods = METHOD.values();
    final boolean[] allMethods = new boolean[realmethods.length];
    for (final METHOD methoditem : methods) {
      allMethods[methoditem.ordinal()] = true;
    }
    StringBuilder allow = null;
    for (int i = 0; i < allMethods.length; i++) {
      if (allMethods[i]) {
        if (allow == null) {
          allow = new StringBuilder(realmethods[i].name());
        } else {
          allow.append(',').append(realmethods[i].name());
        }
      }
    }
    if (allow != null) {
      result.addOptions(allow.toString(), path, getDetailedAllow());
    }
  }

  /**
   * @return the detail of the method handler
   */
  protected abstract ArrayNode getDetailedAllow();

  /**
   * @return the isBodyJson
   */
  public boolean isBodyJsonDecoded() {
    return isBodyJsonDecode;
  }
}
