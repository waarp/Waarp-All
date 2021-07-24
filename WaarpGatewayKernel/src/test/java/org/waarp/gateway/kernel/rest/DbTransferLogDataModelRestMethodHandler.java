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
package org.waarp.gateway.kernel.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.data.DbValue;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.gateway.kernel.database.DbConstantGateway;
import org.waarp.gateway.kernel.database.data.DbTransferLog;
import org.waarp.gateway.kernel.database.data.DbTransferLog.Columns;
import org.waarp.gateway.kernel.exception.HttpForbiddenRequestException;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.exception.HttpNotFoundRequestException;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;

/**
 *
 */
public class DbTransferLogDataModelRestMethodHandler
    extends DataModelRestMethodHandler<DbTransferLog> {
  public static final String BASEURI = "logs";

  public enum FILTER_ARGS {
    MODETRANS("MODE TRANS name subtext"),
    ACCOUNTID("ACCOUNT id information subtext"),
    USERID("USER id information subtext"),
    FILENAME("FILENAME information subtext"),
    INFOSTATUS("Info status information subtext");

    public final String type;

    FILTER_ARGS(String type) {
      this.type = type;
    }
  }

  public String user;
  public String account;

  /**
   * @param config
   * @param method
   */
  public DbTransferLogDataModelRestMethodHandler(RestConfiguration config,
                                                 METHOD... method) {
    super(BASEURI, config, method);
  }

  @Override
  protected void checkAuthorization(HttpRestHandler handler,
                                    RestArgument arguments, RestArgument result,
                                    METHOD method)
      throws HttpForbiddenRequestException {
    // valid all
  }

  @Override
  protected DbTransferLog getItem(HttpRestHandler handler,
                                  RestArgument arguments, RestArgument result,
                                  Object body)
      throws HttpIncorrectRequestException, HttpInvalidAuthenticationException,
             HttpNotFoundRequestException {
    final ObjectNode arg = arguments.getUriArgs().deepCopy();
    arg.setAll(arguments.getBody());
    try {
      final JsonNode node = RestArgument.getId(arg);
      long id;
      if (node.isMissingNode()) {
        // shall not be but continue however
        id = arg.path(DbTransferLog.Columns.SPECIALID.name()).asLong();
      } else {
        id = node.asLong();
      }
      return new DbTransferLog(handler.getDbSession(), user, account, id);
    } catch (final WaarpDatabaseException e) {
      throw new HttpNotFoundRequestException(
          "Issue while reading from database " + arg, e);
    }
  }

  @Override
  protected DbTransferLog createItem(HttpRestHandler handler,
                                     RestArgument arguments,
                                     RestArgument result, Object body)
      throws HttpIncorrectRequestException, HttpInvalidAuthenticationException {
    final ObjectNode arg = arguments.getUriArgs().deepCopy();
    arg.setAll(arguments.getBody());
    try {
      final DbTransferLog newlog = new DbTransferLog(handler.getDbSession());
      newlog.setFromJson(arg, false);
      if (newlog.getAccount() == null || newlog.getUser() == null ||
          newlog.getSpecialId() == DbConstantGateway.ILLEGALVALUE) {
        throw new WaarpDatabaseSqlException(
            "Not enough argument to create the object");
      }
      newlog.insert();
      return newlog;
    } catch (final WaarpDatabaseException e) {
      throw new HttpIncorrectRequestException(
          "Issue while inserting into database", e);
    }
  }

  @Override
  protected DbPreparedStatement getPreparedStatement(HttpRestHandler handler,
                                                     RestArgument arguments,
                                                     RestArgument result,
                                                     Object body)
      throws HttpIncorrectRequestException, HttpInvalidAuthenticationException {
    final ObjectNode arg = arguments.getUriArgs().deepCopy();
    arg.setAll(arguments.getBody());
    String modetrans = arg.path(FILTER_ARGS.MODETRANS.name()).asText();
    if (ParametersChecker.isEmpty(modetrans)) {
      modetrans = null;
    }
    String accountid = arg.path(FILTER_ARGS.ACCOUNTID.name()).asText();
    if (ParametersChecker.isEmpty(accountid)) {
      accountid = null;
    }
    String userid = arg.path(FILTER_ARGS.USERID.name()).asText();
    if (ParametersChecker.isEmpty(userid)) {
      userid = null;
    }
    String filename = arg.path(FILTER_ARGS.FILENAME.name()).asText();
    if (ParametersChecker.isEmpty(filename)) {
      filename = null;
    }
    String infostatus = arg.path(FILTER_ARGS.INFOSTATUS.name()).asText();
    if (ParametersChecker.isEmpty(infostatus)) {
      infostatus = null;
    }
    try {
      return DbTransferLog.getFilterPrepareStament(handler.getDbSession(),
                                                   modetrans, accountid, userid,
                                                   filename, infostatus);
    } catch (final WaarpDatabaseNoConnectionException e) {
      throw new HttpIncorrectRequestException(
          "Issue while reading from database", e);
    } catch (final WaarpDatabaseSqlException e) {
      throw new HttpIncorrectRequestException(
          "Issue while reading from database", e);
    }
  }

  @Override
  protected DbTransferLog getItemPreparedStatement(
      DbPreparedStatement statement)
      throws HttpIncorrectRequestException, HttpNotFoundRequestException {
    try {
      return DbTransferLog.getFromStatement(statement);
    } catch (final WaarpDatabaseNoConnectionException e) {
      throw new HttpIncorrectRequestException(
          "Issue while selecting from database", e);
    } catch (final WaarpDatabaseSqlException e) {
      throw new HttpNotFoundRequestException(
          "Issue while selecting from database", e);
    }
  }

  @Override
  public String getPrimaryPropertyName() {
    return Columns.SPECIALID.name();
  }

  @Override
  protected ArrayNode getDetailedAllow() {
    final ArrayNode node = JsonHandler.createArrayNode();

    final ObjectNode node1 = JsonHandler.createObjectNode();
    node1.put(AbstractDbData.JSON_MODEL, DbTransferLog.class.getSimpleName());
    final DbTransferLog empty = new DbTransferLog(null);
    final DbValue[] values = empty.getAllFields();
    for (final DbValue dbValue : values) {
      node1.put(dbValue.getColumn(), dbValue.getType());
    }

    ObjectNode node2, node3;
    if (methods.contains(METHOD.GET)) {
      node2 = RestArgument.fillDetailedAllow(METHOD.GET, path + "/id",
                                             COMMAND_TYPE.GET.name(),
                                             JsonHandler.createObjectNode().put(
                                                 DbTransferLog.Columns.SPECIALID.name(),
                                                 "SPECIALID as Long in URI as " +
                                                 path + "/id"), node1);
      node.add(node2);

      node3 = JsonHandler.createObjectNode();
      for (final FILTER_ARGS arg : FILTER_ARGS.values()) {
        node3.put(arg.name(), arg.type);
      }
      node2 = RestArgument.fillDetailedAllow(METHOD.GET, path,
                                             COMMAND_TYPE.MULTIGET.name(),
                                             node3,
                                             JsonHandler.createArrayNode()
                                                        .add(node1));
      node.add(node2);
    }
    if (methods.contains(METHOD.PUT)) {
      node3 = JsonHandler.createObjectNode();
      node3.put(DbTransferLog.Columns.HOSTID.name(),
                "SPECIALID as Long in URI as " + path + "/id");
      for (final DbValue dbValue : values) {
        if (dbValue.getColumn()
                   .equalsIgnoreCase(DbTransferLog.Columns.SPECIALID.name())) {
          continue;
        }
        node3.put(dbValue.getColumn(), dbValue.getType());
      }
      node2 = RestArgument.fillDetailedAllow(METHOD.PUT, path + "/id",
                                             COMMAND_TYPE.UPDATE.name(), node3,
                                             node1);
      node.add(node2);
    }
    if (methods.contains(METHOD.DELETE)) {
      node3 = JsonHandler.createObjectNode();
      node3.put(DbTransferLog.Columns.SPECIALID.name(),
                "SPECIALID as Long in URI as " + path + "/id");
      node2 = RestArgument.fillDetailedAllow(METHOD.DELETE, path + "/id",
                                             COMMAND_TYPE.DELETE.name(), node3,
                                             node1);
      node.add(node2);
    }
    if (methods.contains(METHOD.POST)) {
      node3 = JsonHandler.createObjectNode();
      for (final DbValue dbValue : values) {
        node3.put(dbValue.getColumn(), dbValue.getType());
      }
      node2 = RestArgument.fillDetailedAllow(METHOD.POST, path,
                                             COMMAND_TYPE.CREATE.name(), node3,
                                             node1);
      node.add(node2);
    }
    node2 = RestArgument.fillDetailedAllow(METHOD.OPTIONS, path,
                                           COMMAND_TYPE.OPTIONS.name(), null,
                                           null);
    node.add(node2);

    return node;
  }

}
