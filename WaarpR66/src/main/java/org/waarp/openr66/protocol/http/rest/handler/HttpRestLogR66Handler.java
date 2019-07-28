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
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler.COMMAND_TYPE;
import org.waarp.gateway.kernel.rest.HttpRestHandler;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;
import org.waarp.gateway.kernel.rest.RestArgument;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotAuthenticatedException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler.RESTHANDLERS;
import org.waarp.openr66.protocol.localhandler.ServerActions;
import org.waarp.openr66.protocol.localhandler.packet.json.JsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.LogJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.LogResponseJsonPacket;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Log Http REST interface: http://host/log?... + LogJsonPacket as GET
 */
public class HttpRestLogR66Handler extends HttpRestAbstractR66Handler {

  public static final String BASEURI = "log";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpRestLogR66Handler.class);

  public HttpRestLogR66Handler(RestConfiguration config, METHOD... methods) {
    super(BASEURI, config, METHOD.OPTIONS);
    setIntersectionMethods(methods, METHOD.GET);
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
    // now action according to body
    final JsonPacket json = (JsonPacket) body;
    if (json == null) {
      result.setDetail("not enough information");
      setError(handler, result, HttpResponseStatus.BAD_REQUEST);
      return;
    }
    result.getAnswer().put(AbstractDbData.JSON_MODEL, RESTHANDLERS.Log.name());
    try {
      if (json instanceof LogJsonPacket) {//
        result.setCommand(ACTIONS_TYPE.GetLog.name());
        final LogJsonPacket node = (LogJsonPacket) json;
        final boolean purge = node.isPurge();
        final boolean clean = node.isClean();
        final Timestamp start = node.getStart() == null? null :
            new Timestamp(node.getStart().getTime());
        final Timestamp stop = node.getStop() == null? null :
            new Timestamp(node.getStop().getTime());
        final String startid = node.getStartid();
        final String stopid = node.getStopid();
        final String rule = node.getRule();
        final String request = node.getRequest();
        final boolean pending = node.isStatuspending();
        final boolean transfer = node.isStatustransfer();
        final boolean done = node.isStatusdone();
        final boolean error = node.isStatuserror();
        final String[] sresult = serverHandler
            .logPurge(purge, clean, start, stop, startid, stopid, rule, request,
                      pending, transfer, done, error, purge);
        final LogResponseJsonPacket newjson = new LogResponseJsonPacket();
        newjson.fromJson(node);
        // Now answer
        newjson.setCommand(node.getRequestUserPacket());
        newjson.setFilename(sresult[0]);
        newjson.setExported(Long.parseLong(sresult[1]));
        newjson.setPurged(Long.parseLong(sresult[2]));
        setOk(handler, result, newjson, HttpResponseStatus.OK);
      } else {
        logger.info("Validation is ignored: " + json);
        result.setDetail("Unknown command");
        setError(handler, result, json, HttpResponseStatus.PRECONDITION_FAILED);
      }
    } catch (final OpenR66ProtocolNotAuthenticatedException e) {
      throw new HttpInvalidAuthenticationException(e);
    } catch (final OpenR66ProtocolBusinessException e) {
      throw new HttpIncorrectRequestException(e);
    }
  }

  @Override
  protected ArrayNode getDetailedAllow() {
    final ArrayNode node = JsonHandler.createArrayNode();

    if (methods.contains(METHOD.GET)) {
      final LogJsonPacket node3 = new LogJsonPacket();
      node3.setRequestUserPacket();
      node3.setComment("Log export request (GET)");
      node3.setRequest("The requester or requested host name");
      node3.setRule("The rule name");
      node3.setStart(new Date());
      node3.setStop(new Date());
      node3.setStartid("Start id - long -");
      node3.setStopid("Stop id - long -");
      ObjectNode node2;
      final LogResponseJsonPacket resp = new LogResponseJsonPacket();
      resp.setComment("Log export response");
      resp.setFilename("filepath");
      final ArrayNode node1 = JsonHandler.createArrayNode();
      try {
        node1.add(resp.createObjectNode());
        node2 = RestArgument
            .fillDetailedAllow(METHOD.GET, path, ACTIONS_TYPE.GetLog.name(),
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
