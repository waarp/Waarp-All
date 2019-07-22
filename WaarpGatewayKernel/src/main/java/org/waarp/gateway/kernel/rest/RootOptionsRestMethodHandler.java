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
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.FileUpload;
import org.waarp.common.json.JsonHandler;
import org.waarp.gateway.kernel.exception.HttpForbiddenRequestException;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler.COMMAND_TYPE;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;

/**
 * RestMethod handler to implement Root Options handler
 *
 *
 */
public class RootOptionsRestMethodHandler extends RestMethodHandler {

  public static final String ROOT = "root";

  public RootOptionsRestMethodHandler(RestConfiguration config) {
    super(ROOT, "/", true, config, METHOD.OPTIONS);
  }

  @Override
  public void checkHandlerSessionCorrectness(HttpRestHandler handler,
                                             RestArgument arguments,
                                             RestArgument result)
      throws HttpForbiddenRequestException {
  }

  @Override
  public void getFileUpload(HttpRestHandler handler, FileUpload data,
                            RestArgument arguments, RestArgument result)
      throws HttpIncorrectRequestException {
  }

  @Override
  public Object getBody(HttpRestHandler handler, ByteBuf body,
                        RestArgument arguments, RestArgument result)
      throws HttpIncorrectRequestException {
    return null;
  }

  @Override
  public void endParsingRequest(HttpRestHandler handler, RestArgument arguments,
                                RestArgument result, Object body)
      throws HttpIncorrectRequestException, HttpInvalidAuthenticationException {
  }

  @Override
  public ChannelFuture sendResponse(HttpRestHandler handler,
                                    ChannelHandlerContext ctx,
                                    RestArgument arguments, RestArgument result,
                                    Object body, HttpResponseStatus status) {
    return sendOptionsResponse(handler, ctx, result, status);
  }

  @Override
  public void optionsCommand(HttpRestHandler handler, RestArgument arguments,
                             RestArgument result) {
    result.setCommand(COMMAND_TYPE.OPTIONS);
    final METHOD[] realmethods = METHOD.values();
    final boolean[] allMethods = new boolean[realmethods.length];
    for (final RestMethodHandler method : handler.restHashMap.values()) {
      for (final METHOD methoditem : method.methods) {
        allMethods[methoditem.ordinal()] = true;
      }
    }
    String allow = null;
    for (int i = 0; i < allMethods.length; i++) {
      if (allMethods[i]) {
        if (allow == null) {
          allow = realmethods[i].name();
        } else {
          allow += "," + realmethods[i].name();
        }
      }
    }
    String allowUri = null;
    for (final RestMethodHandler method : handler.restHashMap.values()) {
      if (allowUri == null) {
        allowUri = method.path;
      } else {
        allowUri += "," + method.path;
      }
    }
    final ArrayNode array = JsonHandler.createArrayNode();
    for (final RestMethodHandler method : handler.restHashMap.values()) {
      final ArrayNode array2 = method.getDetailedAllow();
      if (method != this) {
        array.addObject().putArray(method.path).addAll(array2);
      } else {
        array.addObject().putArray(ROOT).addAll(array2);
      }
    }
    result.addOptions(allow, allowUri, array);
  }

  @Override
  protected ArrayNode getDetailedAllow() {
    final ArrayNode node = JsonHandler.createArrayNode();

    final ObjectNode node2 = node.addObject().putObject(METHOD.OPTIONS.name());
    node2.put(RestArgument.REST_FIELD.JSON_PATH.field, path);
    node2.put(RestArgument.REST_ROOT_FIELD.JSON_COMMAND.field,
              COMMAND_TYPE.OPTIONS.name());

    return node;
  }

}
