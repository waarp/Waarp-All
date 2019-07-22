/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.gateway.kernel.rest;

import org.waarp.common.database.DbConstant;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.data.DbValue;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.json.JsonHandler;
import org.waarp.gateway.kernel.database.data.DbTransferLog;
import org.waarp.gateway.kernel.database.data.DbTransferLog.Columns;
import org.waarp.gateway.kernel.exception.HttpForbiddenRequestException;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.exception.HttpNotFoundRequestException;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author "Frederic Bregier"
 *
 */
public class DbTransferLogDataModelRestMethodHandler extends DataModelRestMethodHandler<DbTransferLog> {
    public static final String BASEURI = "logs";

    public static enum FILTER_ARGS {
        MODETRANS("MODE TRANS name subtext"),
        ACCOUNTID("ACCOUNT id information subtext"),
        USERID("USER id information subtext"),
        FILENAME("FILENAME information subtext"),
        INFOSTATUS("Info status information subtext");

        public String type;

        FILTER_ARGS(String type) {
            this.type = type;
        }
    }

    public String user;
    public String account;

    /**
     * @param name
     * @param config
     * @param method
     */
    public DbTransferLogDataModelRestMethodHandler(RestConfiguration config, METHOD... method) {
        super(BASEURI, config, method);
    }

    @Override
    protected void checkAuthorization(HttpRestHandler handler, RestArgument arguments, RestArgument result,
            METHOD method) throws HttpForbiddenRequestException {
        // valid all
    }

    @Override
    protected DbTransferLog getItem(HttpRestHandler handler, RestArgument arguments, RestArgument result, Object body)
            throws HttpIncorrectRequestException, HttpInvalidAuthenticationException, HttpNotFoundRequestException {
        ObjectNode arg = arguments.getUriArgs().deepCopy();
        arg.setAll(arguments.getBody());
        try {
            JsonNode node = RestArgument.getId(arg);
            long id;
            if (node.isMissingNode()) {
                // shall not be but continue however
                id = arg.path(DbTransferLog.Columns.SPECIALID.name()).asLong();
            } else {
                id = node.asLong();
            }
            return new DbTransferLog(handler.getDbSession(), user, account, id);
        } catch (WaarpDatabaseException e) {
            throw new HttpNotFoundRequestException("Issue while reading from database " + arg, e);
        }
    }

    @Override
    protected DbTransferLog createItem(HttpRestHandler handler, RestArgument arguments, RestArgument result,
            Object body) throws HttpIncorrectRequestException, HttpInvalidAuthenticationException {
        ObjectNode arg = arguments.getUriArgs().deepCopy();
        arg.setAll(arguments.getBody());
        try {
            DbTransferLog newlog = new DbTransferLog(handler.getDbSession());
            newlog.setFromJson(arg, false);
            if (newlog.getAccount() == null || newlog.getUser() == null
                    || newlog.getSpecialId() == DbConstant.ILLEGALVALUE) {
                throw new WaarpDatabaseSqlException("Not enough argument to create the object");
            }
            newlog.insert();
            return newlog;
        } catch (WaarpDatabaseException e) {
            throw new HttpIncorrectRequestException("Issue while inserting into database", e);
        }
    }

    @Override
    protected DbPreparedStatement getPreparedStatement(HttpRestHandler handler, RestArgument arguments,
            RestArgument result, Object body) throws HttpIncorrectRequestException,
            HttpInvalidAuthenticationException {
        ObjectNode arg = arguments.getUriArgs().deepCopy();
        arg.setAll(arguments.getBody());
        String modetrans = arg.path(FILTER_ARGS.MODETRANS.name()).asText();
        if (modetrans == null || modetrans.isEmpty()) {
            modetrans = null;
        }
        String accountid = arg.path(FILTER_ARGS.ACCOUNTID.name()).asText();
        if (accountid == null || accountid.isEmpty()) {
            accountid = null;
        }
        String userid = arg.path(FILTER_ARGS.USERID.name()).asText();
        if (userid == null || userid.isEmpty()) {
            userid = null;
        }
        String filename = arg.path(FILTER_ARGS.FILENAME.name()).asText();
        if (filename == null || filename.isEmpty()) {
            filename = null;
        }
        String infostatus = arg.path(FILTER_ARGS.INFOSTATUS.name()).asText();
        if (infostatus == null || infostatus.isEmpty()) {
            infostatus = null;
        }
        try {
            return DbTransferLog.getFilterPrepareStament(handler.getDbSession(),
                    modetrans, accountid, userid, filename, infostatus);
        } catch (WaarpDatabaseNoConnectionException e) {
            throw new HttpIncorrectRequestException("Issue while reading from database", e);
        } catch (WaarpDatabaseSqlException e) {
            throw new HttpIncorrectRequestException("Issue while reading from database", e);
        }
    }

    @Override
    protected DbTransferLog getItemPreparedStatement(DbPreparedStatement statement)
            throws HttpIncorrectRequestException, HttpNotFoundRequestException {
        try {
            return DbTransferLog.getFromStatement(statement);
        } catch (WaarpDatabaseNoConnectionException e) {
            throw new HttpIncorrectRequestException("Issue while selecting from database", e);
        } catch (WaarpDatabaseSqlException e) {
            throw new HttpNotFoundRequestException("Issue while selecting from database", e);
        }
    }

    @Override
    public String getPrimaryPropertyName() {
        return Columns.SPECIALID.name();
    }

    @Override
    protected ArrayNode getDetailedAllow() {
        ArrayNode node = JsonHandler.createArrayNode();

        ObjectNode node1 = JsonHandler.createObjectNode();
        node1.put(DbTransferLog.JSON_MODEL, DbTransferLog.class.getSimpleName());
        DbTransferLog empty = new DbTransferLog(null);
        DbValue[] values = empty.getAllFields();
        for (DbValue dbValue : values) {
            node1.put(dbValue.getColumn(), dbValue.getType());
        }

        ObjectNode node2, node3;
        if (this.methods.contains(METHOD.GET)) {
            node2 = RestArgument.fillDetailedAllow(
                    METHOD.GET,
                    this.path + "/id",
                    COMMAND_TYPE.GET.name(),
                    JsonHandler.createObjectNode().put(DbTransferLog.Columns.SPECIALID.name(),
                            "SPECIALID as Long in URI as " + this.path + "/id"),
                    node1);
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
            node3.put(DbTransferLog.Columns.HOSTID.name(), "SPECIALID as Long in URI as " + this.path + "/id");
            for (DbValue dbValue : values) {
                if (dbValue.getColumn().equalsIgnoreCase(DbTransferLog.Columns.SPECIALID.name())) {
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
            node3.put(DbTransferLog.Columns.SPECIALID.name(), "SPECIALID as Long in URI as " + this.path + "/id");
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

}
