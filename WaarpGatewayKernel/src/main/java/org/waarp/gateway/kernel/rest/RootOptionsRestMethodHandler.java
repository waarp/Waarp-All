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
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler.COMMAND_TYPE;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;

/**
 * RestMethod handler to implement Root Options handler
 */
public class RootOptionsRestMethodHandler extends RestMethodHandler {

  public static final String ROOT = "root";

  public RootOptionsRestMethodHandler(final RestConfiguration config) {
    super(ROOT, "/", true, config, METHOD.OPTIONS);
  }

  @Override
  public final void checkHandlerSessionCorrectness(
      final HttpRestHandler handler, final RestArgument arguments,
      final RestArgument result) {
    // nothing
  }

  @Override
  public final void getFileUpload(final HttpRestHandler handler,
                                  final FileUpload data,
                                  final RestArgument arguments,
                                  final RestArgument result) {
    // nothing
  }

  @Override
  public final Object getBody(final HttpRestHandler handler, final ByteBuf body,
                              final RestArgument arguments,
                              final RestArgument result) {
    return null;
  }

  @Override
  public final void endParsingRequest(final HttpRestHandler handler,
                                      final RestArgument arguments,
                                      final RestArgument result,
                                      final Object body) {
    // nothing
  }

  @Override
  public final ChannelFuture sendResponse(final HttpRestHandler handler,
                                          final ChannelHandlerContext ctx,
                                          final RestArgument arguments,
                                          final RestArgument result,
                                          final Object body,
                                          final HttpResponseStatus status) {
    return sendOptionsResponse(handler, ctx, result, status);
  }

  @Override
  public final void optionsCommand(final HttpRestHandler handler,
                                   final RestArgument arguments,
                                   final RestArgument result) {
    result.setCommand(COMMAND_TYPE.OPTIONS);
    final METHOD[] realmethods = METHOD.values();
    final boolean[] allMethods = new boolean[realmethods.length];
    for (final RestMethodHandler method : handler.restHashMap.values()) {
      for (final METHOD methoditem : method.methods) {
        allMethods[methoditem.ordinal()] = true;
      }
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
    StringBuilder allowUri = null;
    for (final RestMethodHandler method : handler.restHashMap.values()) {
      if (allowUri == null) {
        allowUri = new StringBuilder(method.path);
      } else {
        allowUri.append(',').append(method.path);
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
    if (allow != null && allowUri != null) {
      result.addOptions(allow.toString(), allowUri.toString(), array);
    }
  }

  @Override
  protected final ArrayNode getDetailedAllow() {
    final ArrayNode node = JsonHandler.createArrayNode();

    final ObjectNode node2 = node.addObject().putObject(METHOD.OPTIONS.name());
    node2.put(RestArgument.REST_FIELD.JSON_PATH.field, path);
    node2.put(RestArgument.REST_ROOT_FIELD.JSON_COMMAND.field,
              COMMAND_TYPE.OPTIONS.name());

    return node;
  }

}
