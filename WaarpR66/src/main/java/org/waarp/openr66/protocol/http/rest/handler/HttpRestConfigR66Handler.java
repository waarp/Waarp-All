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
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoCorrectAuthenticationException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotAuthenticatedException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler.RESTHANDLERS;
import org.waarp.openr66.protocol.localhandler.ServerActions;
import org.waarp.openr66.protocol.localhandler.packet.json.ConfigExportJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ConfigExportResponseJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ConfigImportJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ConfigImportResponseJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.JsonPacket;

/**
 * Configuration Http REST interface: http://host/config?... +
 * ConfigExportJsonPacket or
 * ConfigImportJsonPacket as GET or PUT
 */
public class HttpRestConfigR66Handler extends HttpRestAbstractR66Handler {

  public static final String BASEURI = "config";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpRestConfigR66Handler.class);

  public HttpRestConfigR66Handler(final RestConfiguration config,
                                  final METHOD... methods) {
    super(BASEURI, config, METHOD.OPTIONS);
    setIntersectionMethods(methods, METHOD.GET, METHOD.PUT);
  }

  @Override
  public final void endParsingRequest(final HttpRestHandler handler,
                                      final RestArgument arguments,
                                      final RestArgument result,
                                      final Object body)
      throws HttpIncorrectRequestException, HttpInvalidAuthenticationException {
    try {
      HttpRestV1Utils.checkSanity(arguments);
    } catch (final InvalidArgumentException e) {
      throw new HttpIncorrectRequestException("Issue on values", e);
    }
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
    result.getAnswer()
          .put(AbstractDbData.JSON_MODEL, RESTHANDLERS.Config.name());
    try {
      if (json instanceof ConfigExportJsonPacket &&
          arguments.getMethod() == METHOD.GET) {//
        result.setCommand(ACTIONS_TYPE.ExportConfig.name());
        // host, rule, business, alias, roles
        final ConfigExportJsonPacket node = (ConfigExportJsonPacket) json;
        final boolean bhost = node.isHost();
        final boolean brule = node.isRule();
        final boolean bbusiness = node.isBusiness();
        final boolean balias = node.isAlias();
        final boolean broles = node.isRoles();
        final String[] sresult =
            serverHandler.configExport(bhost, brule, bbusiness, balias, broles);
        // Now answer
        final ConfigExportResponseJsonPacket resp =
            new ConfigExportResponseJsonPacket();
        resp.fromJson(node);
        resp.setFilehost(sresult[0]);
        resp.setFilerule(sresult[1]);
        resp.setFilebusiness(sresult[2]);
        resp.setFilealias(sresult[3]);
        resp.setFileroles(sresult[4]);
        if (resp.getFilerule() != null || resp.getFilehost() != null ||
            resp.getFilebusiness() != null || resp.getFilealias() != null ||
            resp.getFileroles() != null) {
          setOk(handler, result, resp, HttpResponseStatus.OK);
        } else {
          result.setDetail("Export configuration in error");
          setError(handler, result, resp, HttpResponseStatus.NOT_ACCEPTABLE);
        }
      } else if (json instanceof ConfigImportJsonPacket &&
                 arguments.getMethod() == METHOD.PUT) {//
        result.setCommand(ACTIONS_TYPE.ImportConfig.name());
        final ConfigImportResponseJsonPacket resp =
            serverHandler.configImport((ConfigImportJsonPacket) json);
        if (resp.isImportedhost() || resp.isImportedrule() ||
            resp.isImportedbusiness() || resp.isImportedalias() ||
            resp.isImportedroles()) {
          setOk(handler, result, resp, HttpResponseStatus.OK);
        } else {
          result.setDetail("Import configuration in error");
          setError(handler, result, resp, HttpResponseStatus.NOT_ACCEPTABLE);
        }
      } else {
        logger.info("Validation is ignored: {}", json);
        result.setDetail("Unknown command");
        setError(handler, result, json, HttpResponseStatus.PRECONDITION_FAILED);
      }
    } catch (final OpenR66ProtocolNotAuthenticatedException e) {
      throw new HttpInvalidAuthenticationException(e);
    } catch (final OpenR66ProtocolSystemException e) {
      throw new HttpIncorrectRequestException(e);
    } catch (OpenR66ProtocolNoCorrectAuthenticationException e) {
      throw new HttpInvalidAuthenticationException(e);
    }
  }

  @Override
  protected final ArrayNode getDetailedAllow() {
    final ArrayNode node = JsonHandler.createArrayNode();

    if (methods.contains(METHOD.GET)) {
      final ConfigExportJsonPacket node3 = new ConfigExportJsonPacket();
      node3.setRequestUserPacket();
      node3.setComment("ConfigExport request (GET)");
      final ObjectNode node2;
      final ArrayNode node1 = JsonHandler.createArrayNode();
      final ConfigExportResponseJsonPacket resp =
          new ConfigExportResponseJsonPacket();
      resp.setComment("ConfigExport response");
      resp.setFilealias("filepath");
      resp.setFilebusiness("filepath");
      resp.setFilehost("filepath");
      resp.setFileroles("filepath");
      resp.setFilerule("filepath");
      resp.setRequestUserPacket();
      try {
        node1.add(resp.createObjectNode());
        node2 = RestArgument.fillDetailedAllow(METHOD.GET, path,
                                               ACTIONS_TYPE.ExportConfig.name(),
                                               node3.createObjectNode(), node1);
        node.add(node2);
      } catch (final OpenR66ProtocolPacketException ignored) {
        // ignore
      }
    }
    if (methods.contains(METHOD.PUT)) {
      final ConfigImportJsonPacket node4 = new ConfigImportJsonPacket();
      node4.setRequestUserPacket();
      node4.setComment(
          "ConfigImport request (PUT) where items are either set through transfer Id, either set directly with a filename");
      node4.setAlias("AliasFilename if not through TransferId");
      node4.setBusiness("BusinessFilename if not through TransferId");
      node4.setHost("HostFilename if not through TransferId");
      node4.setRoles("RolesFilename if not through TransferId");
      node4.setRule("RuleFilename if not through TransferId");
      final ConfigImportResponseJsonPacket resp2 =
          new ConfigImportResponseJsonPacket();
      resp2.setComment("ConfigImport response");
      resp2.setAlias("filepath");
      resp2.setBusiness("filepath");
      resp2.setHost("filepath");
      resp2.setRoles("filepath");
      resp2.setRule("filepath");
      resp2.setRequestUserPacket();
      final ArrayNode node1 = JsonHandler.createArrayNode();
      try {
        node1.add(resp2.createObjectNode());
        final ObjectNode node2 =
            RestArgument.fillDetailedAllow(METHOD.PUT, path,
                                           ACTIONS_TYPE.ImportConfig.name(),
                                           node4.createObjectNode(), node1);
        node.add(node2);
      } catch (final OpenR66ProtocolPacketException ignored) {
        // ignore
      }
    }

    final ObjectNode node2 =
        RestArgument.fillDetailedAllow(METHOD.OPTIONS, path,
                                       COMMAND_TYPE.OPTIONS.name(), null, null);
    node.add(node2);

    return node;
  }
}
