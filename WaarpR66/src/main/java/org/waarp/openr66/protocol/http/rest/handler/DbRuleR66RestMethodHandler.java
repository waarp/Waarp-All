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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.gateway.kernel.exception.HttpForbiddenRequestException;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.exception.HttpNotFoundRequestException;
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler;
import org.waarp.gateway.kernel.rest.HttpRestHandler;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;
import org.waarp.gateway.kernel.rest.RestArgument;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbRule.Columns;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;

/**
 * DbRule Rest handler
 */
public class DbRuleR66RestMethodHandler
    extends DataModelRestMethodHandler<DbRule> {
  private static final String RULE_ID_IN_URI_AS_VARCHAR_AS =
      "RuleId in URI as VARCHAR as ";
  public static final String BASEURI = "rules";

  public enum FILTER_ARGS {
    IDRULE("rule name"), MODETRANS("MODETRANS value");

    public final String type;

    FILTER_ARGS(final String type) {
      this.type = type;
    }
  }

  /**
   * @param config
   * @param method
   */
  public DbRuleR66RestMethodHandler(final RestConfiguration config,
                                    final METHOD... method) {
    super(BASEURI, config, method);
  }

  @Override
  protected DbRule getItem(final HttpRestHandler handler,
                           final RestArgument arguments,
                           final RestArgument result, final Object body)
      throws HttpNotFoundRequestException {
    try {
      HttpRestV1Utils.checkSanity(arguments);
    } catch (InvalidArgumentException e) {
      throw new HttpNotFoundRequestException("Issue on values", e);
    }
    final ObjectNode arg = arguments.getUriArgs().deepCopy();
    arg.setAll(arguments.getBody());
    try {
      final JsonNode node = RestArgument.getId(arg);
      final String id;
      if (node.isMissingNode()) {
        // shall not be but continue however
        id = arg.path(DbRule.Columns.IDRULE.name()).asText();
      } else {
        id = node.asText();
      }
      return new DbRule(id);
    } catch (final WaarpDatabaseException e) {
      throw new HttpNotFoundRequestException(
          "Issue while reading from database " + arg, e);
    }
  }

  @Override
  protected DbRule createItem(final HttpRestHandler handler,
                              final RestArgument arguments,
                              final RestArgument result, final Object body)
      throws HttpIncorrectRequestException {
    try {
      HttpRestV1Utils.checkSanity(arguments);
    } catch (InvalidArgumentException e) {
      throw new HttpIncorrectRequestException("Issue on values", e);
    }
    final ObjectNode arg = arguments.getUriArgs().deepCopy();
    arg.setAll(arguments.getBody());
    try {
      return new DbRule(arg);
    } catch (final WaarpDatabaseException e) {
      throw new HttpIncorrectRequestException(
          "Issue while inserting into database", e);
    }
  }

  @Override
  protected DbPreparedStatement getPreparedStatement(
      final HttpRestHandler handler, final RestArgument arguments,
      final RestArgument result, final Object body)
      throws HttpIncorrectRequestException {
    try {
      HttpRestV1Utils.checkSanity(arguments);
    } catch (InvalidArgumentException e) {
      throw new HttpIncorrectRequestException("Issue on values", e);
    }
    final ObjectNode arg = arguments.getUriArgs().deepCopy();
    arg.setAll(arguments.getBody());
    String rule = arg.path(FILTER_ARGS.IDRULE.name()).asText();
    if (ParametersChecker.isEmpty(rule)) {
      rule = null;
    }
    final int mode = arg.path(FILTER_ARGS.MODETRANS.name()).asInt();
    try {
      return DbRule.getFilterPrepareStament(handler.getDbSession(), rule, mode);
    } catch (final WaarpDatabaseNoConnectionException e) {
      throw new HttpIncorrectRequestException(
          "Issue while reading from database", e);
    } catch (final WaarpDatabaseSqlException e) {
      throw new HttpIncorrectRequestException(
          "Issue while reading from database", e);
    }
  }

  @Override
  protected DbRule getItemPreparedStatement(final DbPreparedStatement statement)
      throws HttpIncorrectRequestException, HttpNotFoundRequestException {
    try {
      return DbRule.getFromStatement(statement);
    } catch (final WaarpDatabaseNoConnectionException e) {
      throw new HttpIncorrectRequestException(
          "Issue while selecting from database", e);
    } catch (final WaarpDatabaseSqlException e) {
      throw new HttpNotFoundRequestException(
          "Issue while selecting from database", e);
    }
  }

  @Override
  protected ArrayNode getDetailedAllow() {
    final ArrayNode node = JsonHandler.createArrayNode();

    final ObjectNode node1 = JsonHandler.createObjectNode();
    node1.put(AbstractDbData.JSON_MODEL, DbRule.class.getSimpleName());
    for (final DbRule.Columns column : DbRule.Columns.values()) {
      node1.put(column.name(), DbRule.dbTypes[column.ordinal()]);
    }

    ObjectNode node2;
    ObjectNode node3;
    if (methods.contains(METHOD.GET)) {
      node2 = RestArgument
          .fillDetailedAllow(METHOD.GET, path + "/id", COMMAND_TYPE.GET.name(),
                             JsonHandler.createObjectNode()
                                        .put(DbRule.Columns.IDRULE.name(),
                                             RULE_ID_IN_URI_AS_VARCHAR_AS +
                                             path + "/id"), node1);
      node.add(node2);

      node3 = JsonHandler.createObjectNode();
      for (final FILTER_ARGS arg : FILTER_ARGS.values()) {
        node3.put(arg.name(), arg.type);
      }
      node2 = RestArgument
          .fillDetailedAllow(METHOD.GET, path, COMMAND_TYPE.MULTIGET.name(),
                             node3, JsonHandler.createArrayNode().add(node1));
      node.add(node2);
    }
    if (methods.contains(METHOD.PUT)) {
      node3 = JsonHandler.createObjectNode();
      node3.put(DbRule.Columns.IDRULE.name(),
                RULE_ID_IN_URI_AS_VARCHAR_AS + path + "/id");
      for (final DbRule.Columns column : DbRule.Columns.values()) {
        if (column.name().equalsIgnoreCase(DbRule.Columns.IDRULE.name())) {
          continue;
        }
        node3.put(column.name(), DbRule.dbTypes[column.ordinal()]);
      }
      node2 = RestArgument.fillDetailedAllow(METHOD.PUT, path + "/id",
                                             COMMAND_TYPE.UPDATE.name(), node3,
                                             node1);
      node.add(node2);
    }
    if (methods.contains(METHOD.DELETE)) {
      node3 = JsonHandler.createObjectNode();
      node3.put(DbRule.Columns.IDRULE.name(),
                RULE_ID_IN_URI_AS_VARCHAR_AS + path + "/id");
      node2 = RestArgument.fillDetailedAllow(METHOD.DELETE, path + "/id",
                                             COMMAND_TYPE.DELETE.name(), node3,
                                             node1);
      node.add(node2);
    }
    if (methods.contains(METHOD.POST)) {
      node3 = JsonHandler.createObjectNode();
      for (final DbRule.Columns column : DbRule.Columns.values()) {
        node3.put(column.name(), DbRule.dbTypes[column.ordinal()]);
      }
      node2 = RestArgument
          .fillDetailedAllow(METHOD.POST, path, COMMAND_TYPE.CREATE.name(),
                             node3, node1);
      node.add(node2);
    }
    node2 = RestArgument
        .fillDetailedAllow(METHOD.OPTIONS, path, COMMAND_TYPE.OPTIONS.name(),
                           null, null);
    node.add(node2);

    return node;
  }

  @Override
  public String getPrimaryPropertyName() {
    return Columns.IDRULE.name();
  }

  @Override
  protected void checkAuthorization(final HttpRestHandler handler,
                                    final RestArgument arguments,
                                    final RestArgument result,
                                    final METHOD method)
      throws HttpForbiddenRequestException {
    try {
      HttpRestV1Utils.checkSanity(arguments);
    } catch (InvalidArgumentException e) {
      throw new HttpForbiddenRequestException("Issue on values", e);
    }
    final HttpRestR66Handler r66handler = (HttpRestR66Handler) handler;
    final R66Session session = r66handler.getServerHandler().getSession();
    if (!session.getAuth().isValidRole(ROLE.RULE)) {
      throw new HttpForbiddenRequestException(
          "Partner must have ConfigAdmin role");
    }
  }

}
