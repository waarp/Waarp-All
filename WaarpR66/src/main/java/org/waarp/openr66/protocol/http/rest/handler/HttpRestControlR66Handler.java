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
import org.waarp.common.database.data.DbValue;
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
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoDataException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotAuthenticatedException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler.RESTHANDLERS;
import org.waarp.openr66.protocol.localhandler.ServerActions;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.InformationJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.JsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.RestartTransferJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.StopOrCancelJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.TransferRequestJsonPacket;

import java.util.Date;

/**
 * Transfer Http REST interface: http://host/control?... + InformationJsonPacket
 * (should be on Transfer only)
 * RestartTransferJsonPacket StopOrCancelJsonPacket TransferRequestJsonPacket as
 * GET PUT PUT POST
 *
 *
 */
public class HttpRestControlR66Handler extends HttpRestAbstractR66Handler {

  public static final String BASEURI = "control";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpRestControlR66Handler.class);

  public HttpRestControlR66Handler(RestConfiguration config,
                                   METHOD... methods) {
    super(BASEURI, config, METHOD.OPTIONS);
    setIntersectionMethods(methods, METHOD.GET, METHOD.PUT, METHOD.POST);
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
    final R66Session session = serverHandler.getSession();
    // now action according to body
    final JsonPacket json = (JsonPacket) body;
    if (json == null) {
      result.setDetail("not enough information");
      setError(handler, result, HttpResponseStatus.BAD_REQUEST);
      return;
    }
    result.getAnswer()
          .put(AbstractDbData.JSON_MODEL, RESTHANDLERS.Control.name());
    final METHOD method = arguments.getMethod();
    try {
      if (json instanceof InformationJsonPacket && method == METHOD.GET) {//
        final InformationJsonPacket node = (InformationJsonPacket) json;
        if (node.isIdRequest()) {
          result.setCommand(ACTIONS_TYPE.GetTransferInformation.name());
          ValidPacket validPacket = null;
          if (node.isIdRequest()) {
            validPacket = serverHandler
                .informationRequest(node.getId(), node.isTo(),
                                    node.getRulename(), true);
          } else {
            validPacket = serverHandler
                .informationFile(node.getRequest(), node.getRulename(),
                                 node.getFilename(), true);
          }
          if (validPacket != null) {
            final ObjectNode resp =
                JsonHandler.getFromString(validPacket.getSheader());
            handler.setStatus(HttpResponseStatus.OK);
            result.setResult(HttpResponseStatus.OK);
            result.getResults().add(resp);
          } else {
            result.setDetail("Error during information request");
            setError(handler, result, HttpResponseStatus.NOT_ACCEPTABLE);
          }
        } else {
          result.setCommand(ACTIONS_TYPE.GetInformation.name());
          result.setDetail(
              "Error: FileInformation is not applicable with URI " + BASEURI);
          setError(handler, result, HttpResponseStatus.NOT_ACCEPTABLE);
        }
      } else if (json instanceof RestartTransferJsonPacket &&
                 method == METHOD.PUT) {//
        result.setCommand(ACTIONS_TYPE.RestartTransfer.name());
        final RestartTransferJsonPacket node = (RestartTransferJsonPacket) json;
        final R66Result r66result = serverHandler
            .requestRestart(node.getRequested(), node.getRequester(),
                            node.getSpecialid(), node.getRestarttime());
        if (serverHandler.isCodeValid(r66result.getCode())) {
          result.setDetail("Restart Transfer done");
          setOk(handler, result, node, HttpResponseStatus.OK);
        } else {
          result.setDetail("Restart Transfer in error");
          setError(handler, result, node, HttpResponseStatus.NOT_ACCEPTABLE);
        }
      } else if (json instanceof StopOrCancelJsonPacket &&
                 method == METHOD.PUT) {//
        result.setCommand(ACTIONS_TYPE.StopOrCancelTransfer.name());
        final StopOrCancelJsonPacket node = (StopOrCancelJsonPacket) json;
        R66Result resulttest;
        if (node.getRequested() == null || node.getRequester() == null ||
            node.getSpecialid() == DbConstant.ILLEGALVALUE) {
          final ErrorCode code = ErrorCode.CommandNotFound;
          resulttest = new R66Result(session, true, code, session.getRunner());
          result.setDetail("Not enough argument passed to identify a transfer");
          setError(handler, result, node, HttpResponseStatus.NOT_FOUND);
        } else {
          final String reqd = node.getRequested();
          final String reqr = node.getRequester();
          final long id = node.getSpecialid();
          resulttest = serverHandler
              .stopOrCancel(node.getRequestUserPacket(), reqd, reqr, id);
          result.setDetail(resulttest.getCode().getMesg());
          setOk(handler, result, node, HttpResponseStatus.OK);
        }
      } else if (json instanceof TransferRequestJsonPacket &&
                 method == METHOD.POST) {
        result.setCommand(ACTIONS_TYPE.CreateTransfer.name());
        final TransferRequestJsonPacket node = (TransferRequestJsonPacket) json;
        final R66Result r66result = serverHandler.transferRequest(node);
        if (serverHandler.isCodeValid(r66result.getCode())) {
          result.setDetail("New Transfer registered");
          setOk(handler, result, node, HttpResponseStatus.OK);
        } else {
          result.setDetail("New Transfer cannot be registered");
          setError(handler, result, HttpResponseStatus.NOT_ACCEPTABLE);
        }
      } else {
        logger.info("Validation is ignored: " + json);
        result.setDetail("Unknown command");
        setError(handler, result, json, HttpResponseStatus.PRECONDITION_FAILED);
      }
    } catch (final OpenR66ProtocolNotAuthenticatedException e) {
      throw new HttpInvalidAuthenticationException(e);
    } catch (final OpenR66ProtocolPacketException e) {
      throw new HttpIncorrectRequestException(e);
    } catch (final OpenR66ProtocolNoDataException e) {
      throw new HttpIncorrectRequestException(e);
    }
  }

  @Override
  protected ArrayNode getDetailedAllow() {
    final ArrayNode node = JsonHandler.createArrayNode();

    if (methods.contains(METHOD.GET)) {
      final InformationJsonPacket node3 =
          new InformationJsonPacket(Long.MIN_VALUE, false, "remoteHost");
      node3.setComment("Information on Transfer request (GET)");
      final ArrayNode node1 = JsonHandler.createArrayNode();
      final ObjectNode node1b = JsonHandler.createObjectNode();
      node1b.put(AbstractDbData.JSON_MODEL, DbTaskRunner.class.getSimpleName());
      final DbValue[] values = DbTaskRunner.getAllType();
      for (final DbValue dbValue : values) {
        node1b.put(dbValue.getColumn(), dbValue.getType());
      }
      node1.add(node1b);
      ObjectNode node2;
      try {
        node2 = RestArgument.fillDetailedAllow(METHOD.GET, path,
                                               ACTIONS_TYPE.GetTransferInformation
                                                   .name(),
                                               node3.createObjectNode(), node1);
        node.add(node2);
      } catch (final OpenR66ProtocolPacketException e1) {
      }
    }
    if (methods.contains(METHOD.PUT)) {
      final RestartTransferJsonPacket node4 = new RestartTransferJsonPacket();
      node4.setRequestUserPacket();
      node4.setComment("Restart Transfer request (PUT)");
      node4.setRequested("Requested host");
      node4.setRequester("Requester host");
      node4.setRestarttime(new Date());
      ArrayNode node1 = JsonHandler.createArrayNode();
      try {
        node1.add(node4.createObjectNode());
        final ObjectNode node2 = RestArgument
            .fillDetailedAllow(METHOD.PUT, path,
                               ACTIONS_TYPE.RestartTransfer.name(),
                               node4.createObjectNode(), node1);
        node.add(node2);
      } catch (final OpenR66ProtocolPacketException e1) {
      }
      final StopOrCancelJsonPacket node5 = new StopOrCancelJsonPacket();
      node5.setRequestUserPacket();
      node5.setComment("Stop Or Cancel request (PUT)");
      node5.setRequested("Requested host");
      node5.setRequester("Requester host");
      node1 = JsonHandler.createArrayNode();
      try {
        node1.add(node5.createObjectNode());
        final ObjectNode node2 = RestArgument
            .fillDetailedAllow(METHOD.PUT, path,
                               ACTIONS_TYPE.StopOrCancelTransfer.name(),
                               node5.createObjectNode(), node1);
        node.add(node2);
      } catch (final OpenR66ProtocolPacketException e1) {
      }
    }
    if (methods.contains(METHOD.POST)) {
      final TransferRequestJsonPacket node6 = new TransferRequestJsonPacket();
      node6.setRequestUserPacket();
      node6.setComment("Transfer Request (POST)");
      node6.setFilename("Filename");
      node6.setFileInformation("File information");
      node6.setRequested("Requested host");
      node6.setRulename("Rulename");
      node6.setStart(new Date());
      final ArrayNode node1 = JsonHandler.createArrayNode();
      try {
        node1.add(node6.createObjectNode());
        final ObjectNode node2 = RestArgument
            .fillDetailedAllow(METHOD.POST, path,
                               ACTIONS_TYPE.CreateTransfer.name(),
                               node6.createObjectNode(), node1);
        node.add(node2);
      } catch (final OpenR66ProtocolPacketException e1) {
      }
    }

    final ObjectNode node2 = RestArgument
        .fillDetailedAllow(METHOD.OPTIONS, path, COMMAND_TYPE.OPTIONS.name(),
                           null, null);
    node.add(node2);

    return node;
  }
}
