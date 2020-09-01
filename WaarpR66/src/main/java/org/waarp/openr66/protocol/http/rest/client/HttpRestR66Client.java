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
package org.waarp.openr66.protocol.http.rest.client;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpMethod;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.json.JsonHandler;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler;
import org.waarp.gateway.kernel.rest.HttpRestHandler;
import org.waarp.gateway.kernel.rest.RestArgument;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.gateway.kernel.rest.client.HttpRestClientHelper;
import org.waarp.gateway.kernel.rest.client.RestFuture;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler.RESTHANDLERS;
import org.waarp.openr66.protocol.localhandler.packet.json.JsonPacket;

import java.util.Map;

/**
 * Http Rest R66 client helper class
 */
public class HttpRestR66Client extends HttpRestClientHelper {

  /**
   * Send an HTTP query using the channel for target
   *
   * @param config configuration for REST service
   * @param channel target of the query
   * @param method HttpMethod to use
   * @param host target of the query (shall be the same as for the
   *     channel)
   * @param addedUri additional uri, added to baseUri (shall include
   *     also
   *     extra arguments) (might be null)
   * @param user user to use in authenticated Rest procedure (might be
   *     null)
   * @param pwd password to use in authenticated Rest procedure (might
   *     be
   *     null)
   * @param uriArgs arguments for Uri if any (might be null)
   * @param json json to send as body in the request (might be null);
   *     Useful
   *     in PUT, POST but should not in
   *     GET, DELETE, OPTIONS
   *
   * @return the RestFuture associated with this request
   */
  public RestFuture sendQuery(final RestConfiguration config,
                              final Channel channel, final HttpMethod method,
                              final String host, final String addedUri,
                              final String user, final String pwd,
                              final Map<String, String> uriArgs,
                              final String json) {
    if (config.isRestSignature()) {
      return sendQuery(config.getHmacSha256(), channel, method, host, addedUri,
                       user, pwd, uriArgs, json);
    } else {
      return sendQuery(channel, method, host, addedUri, user, uriArgs, json);
    }
  }

  /**
   * Prepare the future connection
   *
   * @param baseUri in general = '/'
   * @param initializer the associated Initializer including the REST
   *     handler for client side
   * @param client limit number of concurrent connected clients
   * @param timeout time out for network connection
   */
  public HttpRestR66Client(final String baseUri,
                           final ChannelInitializer<SocketChannel> initializer,
                           final int client, final long timeout) {
    super(baseUri, client, timeout, initializer);
  }

  /**
   * @param bodyResponse
   *
   * @return the associated RESTHANDLERS if any, else null
   */
  public RESTHANDLERS getRestHandler(final RestArgument bodyResponse) {
    final ObjectNode node = bodyResponse.getAnswer();
    final String model = node.path(AbstractDbData.JSON_MODEL).asText();
    try {
      if (model != null && !model.isEmpty()) {
        return RESTHANDLERS.valueOf(model);
      }
    } catch (final Exception ignored) {
      // nothing
    }
    return null;
  }

  /**
   * @param bodyResponse
   *
   * @return the primary property value associated with the Model (from the
   *     bodyResponse), else null
   */
  public String getPrimaryProperty(final RestArgument bodyResponse) {
    final ObjectNode answer = bodyResponse.getAnswer();
    final String model = answer.path(AbstractDbData.JSON_MODEL).asText();
    final String property = getPrimaryPropertyName(model);
    if (property == null) {
      return null;
    }
    return answer.path(property).asText();
  }

  /**
   * @param model
   *
   * @return the primary property name associated with the Model
   */
  public String getPrimaryPropertyName(final String model) {
    try {
      if (model != null && !model.isEmpty()) {
        final RESTHANDLERS dbdata = RESTHANDLERS.valueOf(model);
        final DataModelRestMethodHandler<?> handler =
            (DataModelRestMethodHandler<?>) HttpRestHandler.defaultConfiguration.restHashMap
                .get(dbdata.uri);
        return handler.getPrimaryPropertyName();
      }
    } catch (final Exception ignored) {
      // ignore
    }
    return null;
  }

  /**
   * @param dbSession
   * @param future
   *
   * @return the DbData allocated from result if any, else null
   *
   * @throws HttpIncorrectRequestException
   */
  @SuppressWarnings("unchecked")
  public AbstractDbData getDbDataFromFuture(final DbSession dbSession,
                                            final RestFuture future)
      throws HttpIncorrectRequestException {
    if (future.getRestArgument() != null) {
      final RestArgument arg = future.getRestArgument();
      final ObjectNode node = arg.getAnswer();
      final String model = node.path(AbstractDbData.JSON_MODEL).asText();
      try {
        if (model != null && !model.isEmpty()) {
          final RESTHANDLERS rmodel = RESTHANDLERS.valueOf(model);
          try {
            return (AbstractDbData) rmodel.clasz
                .getConstructor(DbSession.class, ObjectNode.class)
                .newInstance(dbSession, node);
          } catch (final Exception e) {
            throw new HttpIncorrectRequestException(e);
          }
        }
      } catch (final Exception ignored) {
        // ignore
      }
    }
    return null;
  }

  /**
   * @param future
   *
   * @return the JsonPacket from result if any, else null
   *
   * @throws HttpIncorrectRequestException
   */
  public JsonPacket getJsonPacketFromFuture(final RestFuture future)
      throws HttpIncorrectRequestException {
    if (future.getRestArgument() != null) {
      final RestArgument arg = future.getRestArgument();
      final ObjectNode node = arg.getAnswer();
      try {
        return JsonPacket.createFromBuffer(JsonHandler.writeAsString(node));
      } catch (final Exception e) {
        throw new HttpIncorrectRequestException(e);
      }
    }
    return null;
  }

}
