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

import java.sql.Timestamp;

import org.joda.time.DateTime;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.data.DbValue;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.gateway.kernel.exception.HttpForbiddenRequestException;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.exception.HttpNotFoundRequestException;
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler;
import org.waarp.gateway.kernel.rest.HttpRestHandler;
import org.waarp.gateway.kernel.rest.RestArgument;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.database.data.DbTaskRunner.Columns;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * DbTaskRunner Rest handler
 * 
 * @author "Frederic Bregier"
 *
 */
public class DbTaskRunnerR66RestMethodHandler extends DataModelRestMethodHandler<DbTaskRunner> {
    public static final String BASEURI = "transfers";

    public static enum FILTER_ARGS {
        LIMIT("number"),
        ORDERBYID("boolean"),
        STARTID("transfer id"),
        STOPID("transfer id"),
        IDRULE("rule name"),
        PARTNER("partner (requester or requested) name"),
        PENDING("boolean"),
        INTRANSFER("boolean"),
        INERROR("boolean"),
        DONE("boolean"),
        ALLSTATUS("boolean"),
        STARTTRANS("Date in ISO 8601 format or ms"),
        STOPTRANS("Date in ISO 8601 format or ms");

        public String type;

        FILTER_ARGS(String type) {
            this.type = type;
        }
    }

    /**
     * @param config
     * @param method
     */
    public DbTaskRunnerR66RestMethodHandler(RestConfiguration config, METHOD... method) {
        super(BASEURI, config, method);
    }

    protected DbTaskRunner getItem(HttpRestHandler handler, RestArgument arguments,
            RestArgument result, Object body) throws HttpIncorrectRequestException,
            HttpInvalidAuthenticationException, HttpNotFoundRequestException {
        ObjectNode arg = arguments.getUriArgs().deepCopy();
        arg.setAll(arguments.getBody());
        try {
            JsonNode node = RestArgument.getId(arg);
            long id;
            if (node.isMissingNode()) {
                // shall not be but continue however
                id = arg.path(DbTaskRunner.Columns.SPECIALID.name()).asLong();
            } else {
                id = node.asLong();
            }
            return new DbTaskRunner(id,
                    arg.path(DbTaskRunner.Columns.REQUESTER.name()).asText(),
                    arg.path(DbTaskRunner.Columns.REQUESTED.name()).asText(),
                    arg.path(DbTaskRunner.Columns.OWNERREQ.name()).asText());
        } catch (WaarpDatabaseException e) {
            throw new HttpNotFoundRequestException("Issue while reading from database " + arg, e);
        }
    }

    @Override
    protected DbTaskRunner createItem(HttpRestHandler handler, RestArgument arguments,
            RestArgument result, Object body) throws HttpIncorrectRequestException,
            HttpInvalidAuthenticationException {
        ObjectNode arg = arguments.getUriArgs().deepCopy();
        arg.setAll(arguments.getBody());
        try {
            return new DbTaskRunner(arg);
        } catch (WaarpDatabaseException e) {
            throw new HttpIncorrectRequestException("Issue while inserting into database", e);
        }
    }

    @Override
    protected DbPreparedStatement getPreparedStatement(HttpRestHandler handler,
            RestArgument arguments, RestArgument result, Object body)
            throws HttpIncorrectRequestException, HttpInvalidAuthenticationException {
        ObjectNode arg = arguments.getUriArgs().deepCopy();
        arg.setAll(arguments.getBody());
        int limit = arg.path(FILTER_ARGS.LIMIT.name()).asInt(0);
        boolean orderBySpecialId = arg.path(FILTER_ARGS.ORDERBYID.name()).asBoolean(false);
        JsonNode node = arg.path(FILTER_ARGS.STARTID.name());
        String startid = null;
        if (!node.isMissingNode()) {
            startid = node.asText();
        }
        if (startid == null || startid.isEmpty()) {
            startid = null;
        }
        node = arg.path(FILTER_ARGS.STOPID.name());
        String stopid = null;
        if (!node.isMissingNode()) {
            stopid = node.asText();
        }
        if (stopid == null || stopid.isEmpty()) {
            stopid = null;
        }
        String rule = arg.path(FILTER_ARGS.IDRULE.name()).asText();
        if (rule == null || rule.isEmpty()) {
            rule = null;
        }
        String req = arg.path(FILTER_ARGS.PARTNER.name()).asText();
        if (req == null || req.isEmpty()) {
            req = null;
        }
        String owner = arg.path(DbTaskRunner.Columns.OWNERREQ.name()).asText();
        if (owner == null || owner.isEmpty()) {
            owner = null;
        }
        boolean pending = arg.path(FILTER_ARGS.PENDING.name()).asBoolean(false);
        boolean transfer = arg.path(FILTER_ARGS.INTRANSFER.name()).asBoolean(false);
        boolean error = arg.path(FILTER_ARGS.INERROR.name()).asBoolean(false);
        boolean done = arg.path(FILTER_ARGS.DONE.name()).asBoolean(false);
        boolean all = arg.path(FILTER_ARGS.ALLSTATUS.name()).asBoolean(false);
        Timestamp start = null;
        node = arg.path(FILTER_ARGS.STARTTRANS.name());
        if (!node.isMissingNode()) {
            long val = node.asLong();
            if (val == 0) {
                DateTime received = DateTime.parse(node.asText());
                val = received.getMillis();
            }
            start = new Timestamp(val);
        }
        Timestamp stop = null;
        node = arg.path(FILTER_ARGS.STOPTRANS.name());
        if (!node.isMissingNode()) {
            long val = node.asLong();
            if (val == 0) {
                DateTime received = DateTime.parse(node.asText());
                val = received.getMillis();
            }
            stop = new Timestamp(val);
        }
        try {
            return DbTaskRunner.getFilterPrepareStatement(handler.getDbSession(),
                    limit, orderBySpecialId, startid, stopid, start, stop, rule, req,
                    pending, transfer, error, done, all, owner);
        } catch (WaarpDatabaseNoConnectionException e) {
            throw new HttpIncorrectRequestException("Issue while reading from database", e);
        } catch (WaarpDatabaseSqlException e) {
            throw new HttpIncorrectRequestException("Issue while reading from database", e);
        }
    }

    @Override
    protected DbTaskRunner getItemPreparedStatement(DbPreparedStatement statement)
            throws HttpIncorrectRequestException, HttpNotFoundRequestException {
        try {
            return DbTaskRunner.getFromStatementNoDbRule(statement);
        } catch (WaarpDatabaseNoConnectionException e) {
            throw new HttpIncorrectRequestException("Issue while selecting from database", e);
        } catch (WaarpDatabaseSqlException e) {
            throw new HttpNotFoundRequestException("Issue while selecting from database", e);
        }
    }

    @Override
    protected ArrayNode getDetailedAllow() {
        ArrayNode node = JsonHandler.createArrayNode();

        ObjectNode node1 = JsonHandler.createObjectNode();
        node1.put(DbTaskRunner.JSON_MODEL, DbTaskRunner.class.getSimpleName());
        DbValue[] values = DbTaskRunner.getAllType();
        for (DbValue dbValue : values) {
            node1.put(dbValue.getColumn(), dbValue.getType());
        }

        ObjectNode node2;
        ObjectNode node3 = JsonHandler.createObjectNode();
        if (this.methods.contains(METHOD.GET)) {
            node3.put(DbTaskRunner.Columns.SPECIALID.name(), "Special Id as LONG in URI as " + this.path + "/id");
            node3.put(DbTaskRunner.Columns.REQUESTER.name(), "Partner as requester as VARCHAR");
            node3.put(DbTaskRunner.Columns.REQUESTED.name(), "Partner as requested as VARCHAR");
            node3.put(DbTaskRunner.Columns.OWNERREQ.name(), "Owner of this request (optional) as VARCHAR");
            node2 = RestArgument.fillDetailedAllow(METHOD.GET, this.path + "/id", COMMAND_TYPE.GET.name(),
                    node3, node1);
            node.add(node2);

            node3 = JsonHandler.createObjectNode();
            for (FILTER_ARGS arg : FILTER_ARGS.values()) {
                node3.put(arg.name(), arg.type);
            }
            node3.put(DbTaskRunner.Columns.OWNERREQ.name(), "Owner of this request (optional) as VARCHAR");
            node2 = RestArgument.fillDetailedAllow(METHOD.GET, this.path, COMMAND_TYPE.MULTIGET.name(),
                    node3, JsonHandler.createArrayNode().add(node1));
            node.add(node2);
        }
        if (this.methods.contains(METHOD.PUT)) {
            node3 = JsonHandler.createObjectNode();
            node3.put(DbTaskRunner.Columns.SPECIALID.name(), "Special Id as LONG in URI as " + this.path + "/id");
            node3.put(DbTaskRunner.Columns.REQUESTER.name(), "Partner as requester as VARCHAR");
            node3.put(DbTaskRunner.Columns.REQUESTED.name(), "Partner as requested as VARCHAR");
            node3.put(DbTaskRunner.Columns.OWNERREQ.name(), "Owner of this request (optional) as VARCHAR");
            for (DbValue dbValue : values) {
                if (dbValue.getColumn().equalsIgnoreCase(DbTaskRunner.Columns.IDRULE.name())) {
                    continue;
                }
                node3.put(dbValue.getColumn(), dbValue.getType());
            }
            node2 = RestArgument.fillDetailedAllow(METHOD.PUT, this.path + "/id", COMMAND_TYPE.UPDATE.name(),
                    node3, node1);
            node.add(node2);
        }
        if (this.methods.contains(METHOD.DELETE)) {
            node3 = JsonHandler.createObjectNode();
            node3.put(DbTaskRunner.Columns.SPECIALID.name(), "Special Id as LONG in URI as " + this.path + "/id");
            node3.put(DbTaskRunner.Columns.REQUESTER.name(), "Partner as requester as VARCHAR");
            node3.put(DbTaskRunner.Columns.REQUESTED.name(), "Partner as requested as VARCHAR");
            node3.put(DbTaskRunner.Columns.OWNERREQ.name(), "Owner of this request (optional) as VARCHAR");
            node2 = RestArgument.fillDetailedAllow(METHOD.DELETE, this.path + "/id", COMMAND_TYPE.DELETE.name(),
                    node3, node1);
            node.add(node2);
        }
        if (this.methods.contains(METHOD.POST)) {
            node3 = JsonHandler.createObjectNode();
            for (DbValue dbValue : values) {
                node3.put(dbValue.getColumn(), dbValue.getType());
            }
            node2 = RestArgument.fillDetailedAllow(METHOD.POST, this.path, COMMAND_TYPE.CREATE.name(),
                    node3, node1);
            node.add(node2);
        }
        node2 = RestArgument.fillDetailedAllow(METHOD.OPTIONS, this.path, COMMAND_TYPE.OPTIONS.name(), null, null);
        node.add(node2);

        return node;
    }

    @Override
    public String getPrimaryPropertyName() {
        return Columns.SPECIALID.name();
    }

    @Override
    protected void checkAuthorization(HttpRestHandler handler, RestArgument arguments,
            RestArgument result, METHOD method) throws HttpForbiddenRequestException {
        HttpRestR66Handler r66handler = (HttpRestR66Handler) handler;
        R66Session session = r66handler.getServerHandler().getSession();
        if (!session.getAuth().isValidRole(ROLE.SYSTEM)) {
            throw new HttpForbiddenRequestException("Partner must have System role");
        }
    }

}
