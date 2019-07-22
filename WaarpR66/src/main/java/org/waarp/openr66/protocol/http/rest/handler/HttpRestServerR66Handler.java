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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler.COMMAND_TYPE;
import org.waarp.gateway.kernel.rest.HttpRestHandler;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;
import org.waarp.gateway.kernel.rest.RestArgument;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotAuthenticatedException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolShutdownException;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler.RESTHANDLERS;
import org.waarp.openr66.protocol.localhandler.ServerActions;
import org.waarp.openr66.protocol.localhandler.packet.json.JsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ShutdownOrBlockJsonPacket;
import org.waarp.openr66.protocol.utils.ChannelUtils;

/**
 * Server Http REST interface: http://host/server?... +
 * ShutdownOrBlockJsonPacket as PUT or no Json body but
 * GET to get Current Status in Json
 *
 *
 */
public class HttpRestServerR66Handler extends HttpRestAbstractR66Handler {

  public static final String BASEURI = "server";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpRestServerR66Handler.class);

  public HttpRestServerR66Handler(RestConfiguration config, METHOD... methods) {
    super(BASEURI, config, METHOD.OPTIONS);
    setIntersectionMethods(methods, METHOD.GET, METHOD.PUT);
  }

  @Override
  public void endParsingRequest(HttpRestHandler handler, RestArgument arguments,
                                RestArgument result, Object body)
      throws HttpIncorrectRequestException, HttpInvalidAuthenticationException {
    logger.debug("debug: {} ### {}", arguments, result);
    if (body != null) {
      logger.debug("Obj: {}", body);
    }
    handler.setWillClose(false);
    final ServerActions serverHandler =
        ((HttpRestR66Handler) handler).getServerHandler();
    if (arguments.getMethod() == METHOD.GET) {
      // status Json
      result.setCommand(ACTIONS_TYPE.GetStatus.name());
      Configuration.configuration.getMonitoring().run(0, true);
      final ObjectNode node =
          Configuration.configuration.getMonitoring().exportAsJson(true);
      result.setDetail("Current Waarp R66 status");
      result.addResult(node);
      setOk(handler, result, null, HttpResponseStatus.OK);
      return;
    }
    // now action according to body
    final JsonPacket json = (JsonPacket) body;
    if (json == null) {
      result.setDetail("not enough information");
      setError(handler, result, HttpResponseStatus.BAD_REQUEST);
      return;
    }
    result.getAnswer()
          .put(AbstractDbData.JSON_MODEL, RESTHANDLERS.Server.name());
    try {
      if (json instanceof ShutdownOrBlockJsonPacket) {//
        final ShutdownOrBlockJsonPacket node = (ShutdownOrBlockJsonPacket) json;
        result.setCommand(ACTIONS_TYPE.ShutdownOrBlock.name());
        final byte[] key = node.getKey();
        if (node.isShutdownOrBlock()) {
          // Shutdown
          serverHandler.shutdown(key, node.isRestartOrBlock());
          result.setDetail("Shutdown on going");
          setOk(handler, result, json, HttpResponseStatus.OK);
        } else {
          // Block
          serverHandler.blockRequest(key, node.isRestartOrBlock());
          node.setComment(
              (node.isRestartOrBlock()? "Block" : "Unblock") + " new request");
          result.setDetail(
              (node.isRestartOrBlock()? "Block" : "Unblock") + " new request");
          setOk(handler, result, json, HttpResponseStatus.OK);
        }
      } else {
        logger.info("Validation is ignored: " + json);
        result.setDetail("Unknown command");
        setError(handler, result, json, HttpResponseStatus.PRECONDITION_FAILED);
      }
    } catch (final OpenR66ProtocolNotAuthenticatedException e) {
      throw new HttpInvalidAuthenticationException(e);
    } catch (final OpenR66ProtocolBusinessException e) {
      throw new HttpIncorrectRequestException(e);
    } catch (final OpenR66ProtocolShutdownException e) {
      WaarpShutdownHook.shutdownWillStart();
      logger.warn(Messages.getString("LocalServerHandler.0") + //$NON-NLS-1$
                  serverHandler.getSession().getAuth().getUser());
      // dont'close, thread will do
      ChannelUtils.startShutdown();
    }
  }

  @Override
  protected ArrayNode getDetailedAllow() {
    final ArrayNode node = JsonHandler.createArrayNode();

    if (methods.contains(METHOD.PUT)) {
      final ShutdownOrBlockJsonPacket node3 = new ShutdownOrBlockJsonPacket();
      node3.setComment("Shutdown Or Block request (PUT)");
      node3.setKey("Key".getBytes());
      ObjectNode node2;
      final ArrayNode node1 = JsonHandler.createArrayNode();
      try {
        node1.add(node3.createObjectNode());
        node2 = RestArgument.fillDetailedAllow(METHOD.PUT, path,
                                               ACTIONS_TYPE.ShutdownOrBlock
                                                   .name(),
                                               node3.createObjectNode(), node1);
        node.add(node2);
      } catch (final OpenR66ProtocolPacketException e1) {
      }
    }
    if (methods.contains(METHOD.GET)) {
      final ArrayNode node1 = JsonHandler.createArrayNode();
      node1.add(Configuration.configuration.getMonitoring().exportAsJson(true));
      final ObjectNode node2 = RestArgument
          .fillDetailedAllow(METHOD.GET, path, ACTIONS_TYPE.GetStatus.name(),
                             null, node1);
      node.add(node2);
    }

    final ObjectNode node2 = RestArgument
        .fillDetailedAllow(METHOD.OPTIONS, path, COMMAND_TYPE.OPTIONS.name(),
                           null, null);
    node.add(node2);

    return node;
  }
}
