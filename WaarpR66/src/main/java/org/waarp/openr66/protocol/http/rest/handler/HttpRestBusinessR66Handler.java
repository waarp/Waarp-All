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
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler.COMMAND_TYPE;
import org.waarp.gateway.kernel.rest.HttpRestHandler;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;
import org.waarp.gateway.kernel.rest.RestArgument;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotAuthenticatedException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler.RESTHANDLERS;
import org.waarp.openr66.protocol.localhandler.ServerActions;
import org.waarp.openr66.protocol.localhandler.packet.json.BusinessRequestJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.JsonPacket;
import org.waarp.openr66.protocol.utils.R66Future;

import static org.waarp.openr66.context.R66FiniteDualStates.*;

/**
 * Business Http REST interface: http://host/business?... +
 * BusinessRequestJsonPacket as GET
 */
public class HttpRestBusinessR66Handler extends HttpRestAbstractR66Handler {

  public static final String BASEURI = "business";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpRestBusinessR66Handler.class);

  public HttpRestBusinessR66Handler(final RestConfiguration config,
                                    final METHOD... methods) {
    super(BASEURI, config, METHOD.OPTIONS);
    setIntersectionMethods(methods, METHOD.GET);
  }

  @Override
  public void endParsingRequest(final HttpRestHandler handler,
                                final RestArgument arguments,
                                final RestArgument result, final Object body)
      throws HttpIncorrectRequestException, HttpInvalidAuthenticationException {
    try {
      HttpRestV1Utils.checkSanity(arguments);
    } catch (InvalidArgumentException e) {
      throw new HttpIncorrectRequestException("Issue on values", e);
    }
    logger.debug("debug: {} ### {}", arguments, result);
    if (body != null) {
      logger.debug("Obj: {}", body);
    }
    handler.setWillClose(false);
    final ServerActions serverHandler =
        ((HttpRestR66Handler) handler).getServerHandler();
    final R66Session session = serverHandler.getSession();
    // now action according to body
    final JsonPacket json = (JsonPacket) body;
    if (json == null) {
      result.setDetail("not enough information");
      setError(handler, result, HttpResponseStatus.BAD_REQUEST);
      return;
    }
    result.getAnswer()
          .put(AbstractDbData.JSON_MODEL, RESTHANDLERS.Business.name());
    try {
      if (json instanceof BusinessRequestJsonPacket) {//
        result.setCommand(ACTIONS_TYPE.ExecuteBusiness.name());
        final BusinessRequestJsonPacket node = (BusinessRequestJsonPacket) json;
        final R66Future future = serverHandler
            .businessRequest(node.isToApplied(), node.getClassName(),
                             node.getArguments(), node.getExtraArguments(),
                             node.getDelay());
        if (future != null && !future.isSuccess()) {
          R66Result r66result = future.getResult();
          if (r66result == null) {
            r66result =
                new R66Result(session, false, ErrorCode.ExternalOp, null);
          }
          wrongResult(handler, result, session, node, r66result);
        } else if (future == null) {
          final R66Result r66result =
              new R66Result(session, false, ErrorCode.ExternalOp, null);
          wrongResult(handler, result, session, node, r66result);
        } else {
          final R66Result r66result = future.getResult();
          if (r66result != null && r66result.getOther() != null) {
            result.setDetail(r66result.getOther().toString());
            node.setArguments(r66result.getOther().toString());
          }
          setOk(handler, result, json, HttpResponseStatus.OK);
        }
      } else {
        logger.info("Validation is ignored: {}", json);
        result.setDetail("Unknown command");
        setError(handler, result, json, HttpResponseStatus.PRECONDITION_FAILED);
      }
    } catch (final OpenR66ProtocolNotAuthenticatedException e) {
      throw new HttpInvalidAuthenticationException(e);
    }
  }

  private void wrongResult(final HttpRestHandler handler,
                           final RestArgument result, final R66Session session,
                           final BusinessRequestJsonPacket node,
                           final R66Result r66result) {
    logger.info("Task in Error: {} {}", node.getClassName(), r66result);
    if (!r66result.isAnswered()) {
      node.setValidated(false);
      session.newState(ERROR);
    }
    result.setDetail("Task in Error:" + node.getClassName() + ' ' + r66result);
    setError(handler, result, HttpResponseStatus.NOT_ACCEPTABLE);
  }

  @Override
  protected ArrayNode getDetailedAllow() {
    final ArrayNode node = JsonHandler.createArrayNode();

    if (methods.contains(METHOD.GET)) {
      final BusinessRequestJsonPacket node3 = new BusinessRequestJsonPacket();
      node3.setRequestUserPacket();
      node3.setComment("Business execution request (GET)");
      node3.setClassName("Class name to execute");
      node3.setArguments("Arguments of the execution");
      node3.setExtraArguments("Extra arguments");
      final ObjectNode node2;
      final ArrayNode node1 = JsonHandler.createArrayNode();
      try {
        node1.add(node3.createObjectNode());
        node2 = RestArgument.fillDetailedAllow(METHOD.GET, path,
                                               ACTIONS_TYPE.ExecuteBusiness
                                                   .name(),
                                               node3.createObjectNode(), node1);
        node.add(node2);
      } catch (final OpenR66ProtocolPacketException ignored) {
        // ignore
      }
    }
    final ObjectNode node2 = RestArgument
        .fillDetailedAllow(METHOD.OPTIONS, path, COMMAND_TYPE.OPTIONS.name(),
                           null, null);
    node.add(node2);

    return node;
  }
}
