/**
   This file is part of Waarp Project.

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All Waarp Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Waarp is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Waarp .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.protocol.http.rest.handler;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.rest.HttpRestHandler;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler.COMMAND_TYPE;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;
import org.waarp.gateway.kernel.rest.RestArgument;
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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Configuration Http REST interface: http://host/config?... + ConfigExportJsonPacket or ConfigImportJsonPacket as GET or PUT
 * 
 * @author "Frederic Bregier"
 *
 */
public class HttpRestConfigR66Handler extends HttpRestAbstractR66Handler {

    public static final String BASEURI = "config";
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(HttpRestConfigR66Handler.class);

    public HttpRestConfigR66Handler(RestConfiguration config, METHOD... methods) {
        super(BASEURI, config, METHOD.OPTIONS);
        setIntersectionMethods(methods, METHOD.GET, METHOD.PUT);
    }

    @Override
    public void endParsingRequest(HttpRestHandler handler, RestArgument arguments, RestArgument result, Object body)
            throws HttpIncorrectRequestException, HttpInvalidAuthenticationException {
        logger.debug("debug: {} ### {}", arguments, result);
        if (body != null) {
            logger.debug("Obj: {}", body);
        }
        handler.setWillClose(false);
        ServerActions serverHandler = ((HttpRestR66Handler) handler).getServerHandler();
        // now action according to body
        JsonPacket json = (JsonPacket) body;
        if (json == null) {
            result.setDetail("not enough information");
            setError(handler, result, HttpResponseStatus.BAD_REQUEST);
            return;
        }
        result.getAnswer().put(AbstractDbData.JSON_MODEL, RESTHANDLERS.Config.name());
        try {
            if (json instanceof ConfigExportJsonPacket && arguments.getMethod() == METHOD.GET) {//
                result.setCommand(ACTIONS_TYPE.ExportConfig.name());
                // host, rule, business, alias, roles
                ConfigExportJsonPacket node = (ConfigExportJsonPacket) json;
                boolean bhost = node.isHost();
                boolean brule = node.isRule();
                boolean bbusiness = node.isBusiness();
                boolean balias = node.isAlias();
                boolean broles = node.isRoles();
                String sresult[] = serverHandler.configExport(bhost, brule, bbusiness, balias, broles);
                // Now answer
                ConfigExportResponseJsonPacket resp = new ConfigExportResponseJsonPacket();
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
            } else if (json instanceof ConfigImportJsonPacket && arguments.getMethod() == METHOD.PUT) {//
                result.setCommand(ACTIONS_TYPE.ImportConfig.name());
                ConfigImportResponseJsonPacket resp = serverHandler.configImport((ConfigImportJsonPacket) json);
                if (resp.isImportedhost() || resp.isImportedrule() ||
                        resp.isImportedbusiness() || resp.isImportedalias() ||
                        resp.isImportedroles()) {
                    setOk(handler, result, resp, HttpResponseStatus.OK);
                } else {
                    result.setDetail("Import configuration in error");
                    setError(handler, result, resp, HttpResponseStatus.NOT_ACCEPTABLE);
                }
            } else {
                logger.info("Validation is ignored: " + json);
                result.setDetail("Unknown command");
                setError(handler, result, json, HttpResponseStatus.PRECONDITION_FAILED);
            }
        } catch (OpenR66ProtocolNotAuthenticatedException e) {
            throw new HttpInvalidAuthenticationException(e);
        } catch (OpenR66ProtocolSystemException e) {
            throw new HttpIncorrectRequestException(e);
        }
    }

    protected ArrayNode getDetailedAllow() {
        ArrayNode node = JsonHandler.createArrayNode();

        if (this.methods.contains(METHOD.GET)) {
            ConfigExportJsonPacket node3 = new ConfigExportJsonPacket();
            node3.setRequestUserPacket();
            node3.setComment("ConfigExport request (GET)");
            ObjectNode node2;
            ArrayNode node1 = JsonHandler.createArrayNode();
            ConfigExportResponseJsonPacket resp = new ConfigExportResponseJsonPacket();
            resp.setComment("ConfigExport response");
            resp.setFilealias("filepath");
            resp.setFilebusiness("filepath");
            resp.setFilehost("filepath");
            resp.setFileroles("filepath");
            resp.setFilerule("filepath");
            resp.setRequestUserPacket();
            try {
                node1.add(resp.createObjectNode());
                node2 = RestArgument.fillDetailedAllow(METHOD.GET, this.path, ACTIONS_TYPE.ExportConfig.name(),
                        node3.createObjectNode(), node1);
                node.add(node2);
            } catch (OpenR66ProtocolPacketException e1) {
            }
        }
        if (this.methods.contains(METHOD.PUT)) {
            ConfigImportJsonPacket node4 = new ConfigImportJsonPacket();
            node4.setRequestUserPacket();
            node4.setComment("ConfigImport request (PUT) where items are either set through transfer Id, either set directly with a filename");
            node4.setAlias("AliasFilename if not through TransferId");
            node4.setBusiness("BusinessFilename if not through TransferId");
            node4.setHost("HostFilename if not through TransferId");
            node4.setRoles("RolesFilename if not through TransferId");
            node4.setRule("RuleFilename if not through TransferId");
            ConfigImportResponseJsonPacket resp2 = new ConfigImportResponseJsonPacket();
            resp2.setComment("ConfigImport response");
            resp2.setAlias("filepath");
            resp2.setBusiness("filepath");
            resp2.setHost("filepath");
            resp2.setRoles("filepath");
            resp2.setRule("filepath");
            resp2.setRequestUserPacket();
            ArrayNode node1 = JsonHandler.createArrayNode();
            try {
                node1.add(resp2.createObjectNode());
                ObjectNode node2 = RestArgument.fillDetailedAllow(METHOD.PUT, this.path,
                        ACTIONS_TYPE.ImportConfig.name(), node4.createObjectNode(), node1);
                node.add(node2);
            } catch (OpenR66ProtocolPacketException e1) {
            }
        }

        ObjectNode node2 = RestArgument.fillDetailedAllow(METHOD.OPTIONS, this.path, COMMAND_TYPE.OPTIONS.name(), null,
                null);
        node.add(node2);

        return node;
    }
}
