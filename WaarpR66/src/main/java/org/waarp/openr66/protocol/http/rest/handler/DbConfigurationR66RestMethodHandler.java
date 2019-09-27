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
import org.waarp.common.json.JsonHandler;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.gateway.kernel.exception.HttpForbiddenRequestException;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.exception.HttpNotFoundRequestException;
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler;
import org.waarp.gateway.kernel.rest.HttpRestHandler;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;
import org.waarp.gateway.kernel.rest.RestArgument;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.database.data.DbConfiguration;
import org.waarp.openr66.database.data.DbConfiguration.Columns;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;

/**
 * DBConfiguration Rest Handler
 */
public class DbConfigurationR66RestMethodHandler
    extends DataModelRestMethodHandler<DbConfiguration> {
  private static final String HOST_ID_AS_VARCHAR_IN_URI_AS =
      "HostId as VARCHAR in URI as ";
  public static final String BASEURI = "configurations";

  public enum FILTER_ARGS {
    HOSTID("host name"), BANDWIDTH(
        "<0 for no filter, =0 for no bandwidth, >0 for a limit greater than value");

    public final String type;

    FILTER_ARGS(String type) {
      this.type = type;
    }
  }

  /**
   * @param config
   * @param method
   */
  public DbConfigurationR66RestMethodHandler(RestConfiguration config,
                                             METHOD... method) {
    super(BASEURI, config, method);
  }

  @Override
  protected DbConfiguration getItem(HttpRestHandler handler,
                                    RestArgument arguments, RestArgument result,
                                    Object body)
      throws HttpIncorrectRequestException, HttpInvalidAuthenticationException,
             HttpNotFoundRequestException {
    final ObjectNode arg = arguments.getUriArgs().deepCopy();
    arg.setAll(arguments.getBody());
    try {
      final JsonNode node = RestArgument.getId(arg);
      String id;
      if (node.isMissingNode()) {
        // shall not be but continue however
        id = arg.path(DbConfiguration.Columns.HOSTID.name()).asText();
      } else {
        id = node.asText();
      }
      return new DbConfiguration(id);
    } catch (final WaarpDatabaseException e) {
      throw new HttpNotFoundRequestException(
          "Issue while reading from database " + arg, e);
    }
  }

  @Override
  protected DbConfiguration createItem(HttpRestHandler handler,
                                       RestArgument arguments,
                                       RestArgument result, Object body)
      throws HttpIncorrectRequestException, HttpInvalidAuthenticationException {
    final ObjectNode arg = arguments.getUriArgs().deepCopy();
    arg.setAll(arguments.getBody());
    try {
      return new DbConfiguration(arg);
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
    String host = arg.path(FILTER_ARGS.HOSTID.name()).asText();
    if (host == null || host.isEmpty()) {
      host = null;
    }
    final int limit = arg.path(FILTER_ARGS.BANDWIDTH.name()).asInt(-1);
    try {
      return DbConfiguration
          .getFilterPrepareStament(handler.getDbSession(), host, limit);
    } catch (final WaarpDatabaseNoConnectionException e) {
      throw new HttpIncorrectRequestException(
          "Issue while reading from database", e);
    } catch (final WaarpDatabaseSqlException e) {
      throw new HttpIncorrectRequestException(
          "Issue while reading from database", e);
    }
  }

  @Override
  protected DbConfiguration getItemPreparedStatement(
      DbPreparedStatement statement)
      throws HttpIncorrectRequestException, HttpNotFoundRequestException {
    try {
      return DbConfiguration.getFromStatement(statement);
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
    node1.put(AbstractDbData.JSON_MODEL, DbConfiguration.class.getSimpleName());

    for (DbConfiguration.Columns column : DbConfiguration.Columns.values()) {
      node1.put(column.name(), DbConfiguration.dbTypes[column.ordinal()]);
    }
    ObjectNode node2;
    ObjectNode node3;
    if (methods.contains(METHOD.GET)) {
      node2 = RestArgument
          .fillDetailedAllow(METHOD.GET, path + "/id", COMMAND_TYPE.GET.name(),
                             JsonHandler.createObjectNode().put(
                                 DbConfiguration.Columns.HOSTID.name(),
                                 HOST_ID_AS_VARCHAR_IN_URI_AS + path + "/id"),
                             node1);
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
      node3.put(DbConfiguration.Columns.HOSTID.name(),
                HOST_ID_AS_VARCHAR_IN_URI_AS + path + "/id");
      for (DbConfiguration.Columns column : DbConfiguration.Columns.values()) {
        if (column.name()
                  .equalsIgnoreCase(DbConfiguration.Columns.HOSTID.name())) {
          continue;
        }
        node3.put(column.name(), DbConfiguration.dbTypes[column.ordinal()]);
      }
      node2 = RestArgument.fillDetailedAllow(METHOD.PUT, path + "/id",
                                             COMMAND_TYPE.UPDATE.name(), node3,
                                             node1);
      node.add(node2);
    }
    if (methods.contains(METHOD.DELETE)) {
      node3 = JsonHandler.createObjectNode();
      node3.put(DbConfiguration.Columns.HOSTID.name(),
                HOST_ID_AS_VARCHAR_IN_URI_AS + path + "/id");
      node2 = RestArgument.fillDetailedAllow(METHOD.DELETE, path + "/id",
                                             COMMAND_TYPE.DELETE.name(), node3,
                                             node1);
      node.add(node2);
    }
    if (methods.contains(METHOD.POST)) {
      node3 = JsonHandler.createObjectNode();
      for (DbConfiguration.Columns column : DbConfiguration.Columns.values()) {
        node3.put(column.name(), DbConfiguration.dbTypes[column.ordinal()]);
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
    return Columns.HOSTID.name();
  }

  @Override
  protected void put(HttpRestHandler handler, RestArgument arguments,
                     RestArgument result, Object body)
      throws HttpIncorrectRequestException, HttpInvalidAuthenticationException,
             HttpNotFoundRequestException {
    super.put(handler, arguments, result, body);
    final DbConfiguration item = getItem(handler, arguments, result, body);
    if (item.isOwnConfiguration()) {
      item.updateConfiguration();
    }
  }

  @Override
  protected void checkAuthorization(HttpRestHandler handler,
                                    RestArgument arguments, RestArgument result,
                                    METHOD method)
      throws HttpForbiddenRequestException {
    final HttpRestR66Handler r66handler = (HttpRestR66Handler) handler;
    final R66Session session = r66handler.getServerHandler().getSession();
    if (!session.getAuth().isValidRole(ROLE.CONFIGADMIN)) {
      throw new HttpForbiddenRequestException(
          "Partner must have ConfigAdmin role");
    }
  }

}
