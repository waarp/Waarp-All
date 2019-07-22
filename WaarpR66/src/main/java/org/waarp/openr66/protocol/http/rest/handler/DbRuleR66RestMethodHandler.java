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
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbRule.Columns;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * DbRule Rest handler
 * 
 * @author "Frederic Bregier"
 *
 */
public class DbRuleR66RestMethodHandler extends DataModelRestMethodHandler<DbRule> {
    public static final String BASEURI = "rules";

    public static enum FILTER_ARGS {
        IDRULE("rule name"),
        MODETRANS("MODETRANS value");

        public String type;

        FILTER_ARGS(String type) {
            this.type = type;
        }
    }

    /**
     * @param config
     * @param method
     */
    public DbRuleR66RestMethodHandler(RestConfiguration config, METHOD... method) {
        super(BASEURI, config, method);
    }

    protected DbRule getItem(HttpRestHandler handler, RestArgument arguments,
            RestArgument result, Object body) throws HttpIncorrectRequestException,
            HttpInvalidAuthenticationException, HttpNotFoundRequestException {
        ObjectNode arg = arguments.getUriArgs().deepCopy();
        arg.setAll(arguments.getBody());
        try {
            JsonNode node = RestArgument.getId(arg);
            String id;
            if (node.isMissingNode()) {
                // shall not be but continue however
                id = arg.path(DbRule.Columns.IDRULE.name()).asText();
            } else {
                id = node.asText();
            }
            return new DbRule(id);
        } catch (WaarpDatabaseException e) {
            throw new HttpNotFoundRequestException("Issue while reading from database " + arg, e);
        }
    }

    @Override
    protected DbRule createItem(HttpRestHandler handler, RestArgument arguments,
            RestArgument result, Object body) throws HttpIncorrectRequestException,
            HttpInvalidAuthenticationException {
        ObjectNode arg = arguments.getUriArgs().deepCopy();
        arg.setAll(arguments.getBody());
        try {
            return new DbRule(arg);
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
        String rule = arg.path(FILTER_ARGS.IDRULE.name()).asText();
        if (rule == null || rule.isEmpty()) {
            rule = null;
        }
        int mode = arg.path(FILTER_ARGS.MODETRANS.name()).asInt();
        try {
            return DbRule.getFilterPrepareStament(handler.getDbSession(),
                    rule, mode);
        } catch (WaarpDatabaseNoConnectionException e) {
            throw new HttpIncorrectRequestException("Issue while reading from database", e);
        } catch (WaarpDatabaseSqlException e) {
            throw new HttpIncorrectRequestException("Issue while reading from database", e);
        }
    }

    @Override
    protected DbRule getItemPreparedStatement(DbPreparedStatement statement)
            throws HttpIncorrectRequestException, HttpNotFoundRequestException {
        try {
            return DbRule.getFromStatement(statement);
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
        node1.put(DbRule.JSON_MODEL, DbRule.class.getSimpleName());
        DbValue[] values = DbRule.getAllType();
        for (DbValue dbValue : values) {
            node1.put(dbValue.getColumn(), dbValue.getType());
        }

        ObjectNode node2, node3;
        if (this.methods.contains(METHOD.GET)) {
            node2 = RestArgument.fillDetailedAllow(
                    METHOD.GET,
                    this.path + "/id",
                    COMMAND_TYPE.GET.name(),
                    JsonHandler.createObjectNode().put(DbRule.Columns.IDRULE.name(),
                            "RuleId in URI as VARCHAR as " + this.path + "/id"), node1);
            node.add(node2);

            node3 = JsonHandler.createObjectNode();
            for (FILTER_ARGS arg : FILTER_ARGS.values()) {
                node3.put(arg.name(), arg.type);
            }
            node2 = RestArgument.fillDetailedAllow(METHOD.GET, this.path, COMMAND_TYPE.MULTIGET.name(),
                    node3, JsonHandler.createArrayNode().add(node1));
            node.add(node2);
        }
        if (this.methods.contains(METHOD.PUT)) {
            node3 = JsonHandler.createObjectNode();
            node3.put(DbRule.Columns.IDRULE.name(), "RuleId in URI as VARCHAR as " + this.path + "/id");
            for (DbValue dbValue : values) {
                if (dbValue.getColumn().equalsIgnoreCase(DbRule.Columns.IDRULE.name())) {
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
            node3.put(DbRule.Columns.IDRULE.name(), "RuleId in URI as VARCHAR as " + this.path + "/id");
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
        return Columns.IDRULE.name();
    }

    @Override
    protected void checkAuthorization(HttpRestHandler handler, RestArgument arguments,
            RestArgument result, METHOD method) throws HttpForbiddenRequestException {
        HttpRestR66Handler r66handler = (HttpRestR66Handler) handler;
        R66Session session = r66handler.getServerHandler().getSession();
        if (!session.getAuth().isValidRole(ROLE.CONFIGADMIN)) {
            throw new HttpForbiddenRequestException("Partner must have ConfigAdmin role");
        }
    }

}
