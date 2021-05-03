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
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.exception.HttpNotFoundRequestException;
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler;
import org.waarp.gateway.kernel.rest.HttpRestHandler;
import org.waarp.gateway.kernel.rest.HttpRestHandler.METHOD;
import org.waarp.gateway.kernel.rest.RestArgument;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.database.data.DbHostConfiguration;
import org.waarp.openr66.database.data.DbHostConfiguration.Columns;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;

/**
 * DbHostConfiguration Rest handler
 */
public class DbHostConfigurationR66RestMethodHandler
    extends DataModelRestMethodHandler<DbHostConfiguration> {
  private static final String HOST_ID_AS_VARCHAR_IN_URI_AS =
      "HostId as VARCHAR in URI as ";
  public static final String BASEURI = "hostconfigs";

  public enum FILTER_ARGS {
    HOSTID("host name subtext"), BUSINESS("BUSINESS information subtext"),
    ROLES("ROLES information subtext"), ALIASES("ALIASES information subtext"),
    OTHERS("OTHERS information subtext");

    public final String type;

    FILTER_ARGS(final String type) {
      this.type = type;
    }
  }

  /**
   * @param config
   * @param method
   */
  public DbHostConfigurationR66RestMethodHandler(final RestConfiguration config,
                                                 final METHOD... method) {
    super(BASEURI, config, method);
  }

  @Override
  protected DbHostConfiguration getItem(final HttpRestHandler handler,
                                        final RestArgument arguments,
                                        final RestArgument result,
                                        final Object body)
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
        id = arg.path(DbHostConfiguration.Columns.HOSTID.name()).asText();
      } else {
        id = node.asText();
      }
      return new DbHostConfiguration(id);
    } catch (final WaarpDatabaseException e) {
      throw new HttpNotFoundRequestException(
          "Issue while reading from database " + arg, e);
    }
  }

  @Override
  protected DbHostConfiguration createItem(final HttpRestHandler handler,
                                           final RestArgument arguments,
                                           final RestArgument result,
                                           final Object body)
      throws HttpIncorrectRequestException {
    try {
      HttpRestV1Utils.checkSanity(arguments);
    } catch (InvalidArgumentException e) {
      throw new HttpIncorrectRequestException("Issue on values", e);
    }
    final ObjectNode arg = arguments.getUriArgs().deepCopy();
    arg.setAll(arguments.getBody());
    try {
      return new DbHostConfiguration(arg);
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
    String hostid = arg.path(FILTER_ARGS.HOSTID.name()).asText();
    if (ParametersChecker.isEmpty(hostid)) {
      hostid = null;
    }
    String business = arg.path(FILTER_ARGS.BUSINESS.name()).asText();
    if (ParametersChecker.isEmpty(business)) {
      business = null;
    }
    String role = arg.path(FILTER_ARGS.ROLES.name()).asText();
    if (ParametersChecker.isEmpty(role)) {
      role = null;
    }
    String alias = arg.path(FILTER_ARGS.ALIASES.name()).asText();
    if (ParametersChecker.isEmpty(alias)) {
      alias = null;
    }
    String other = arg.path(FILTER_ARGS.OTHERS.name()).asText();
    if (ParametersChecker.isEmpty(other)) {
      other = null;
    }
    try {
      return DbHostConfiguration
          .getFilterPrepareStament(handler.getDbSession(), hostid, business,
                                   role, alias, other);
    } catch (final WaarpDatabaseNoConnectionException e) {
      throw new HttpIncorrectRequestException(
          "Issue while reading from database", e);
    } catch (final WaarpDatabaseSqlException e) {
      throw new HttpIncorrectRequestException(
          "Issue while reading from database", e);
    }
  }

  @Override
  protected DbHostConfiguration getItemPreparedStatement(
      final DbPreparedStatement statement)
      throws HttpIncorrectRequestException, HttpNotFoundRequestException {
    try {
      return DbHostConfiguration.getFromStatement(statement);
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
    node1.put(AbstractDbData.JSON_MODEL,
              DbHostConfiguration.class.getSimpleName());
    for (final DbHostConfiguration.Columns column : DbHostConfiguration.Columns
        .values()) {
      node1.put(column.name(), DbHostConfiguration.dbTypes[column.ordinal()]);
    }

    ObjectNode node2;
    ObjectNode node3;
    if (methods.contains(METHOD.GET)) {
      node2 = RestArgument
          .fillDetailedAllow(METHOD.GET, path + "/id", COMMAND_TYPE.GET.name(),
                             JsonHandler.createObjectNode().put(
                                 DbHostConfiguration.Columns.HOSTID.name(),
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
      node3.put(DbHostConfiguration.Columns.HOSTID.name(),
                HOST_ID_AS_VARCHAR_IN_URI_AS + path + "/id");
      for (final DbHostConfiguration.Columns column : DbHostConfiguration.Columns
          .values()) {
        if (column.name().equalsIgnoreCase(
            DbHostConfiguration.Columns.HOSTID.name())) {
          continue;
        }
        node3.put(column.name(), DbHostConfiguration.dbTypes[column.ordinal()]);
      }
      node2 = RestArgument.fillDetailedAllow(METHOD.PUT, path + "/id",
                                             COMMAND_TYPE.UPDATE.name(), node3,
                                             node1);
      node.add(node2);
    }
    if (methods.contains(METHOD.DELETE)) {
      node3 = JsonHandler.createObjectNode();
      node3.put(DbHostConfiguration.Columns.HOSTID.name(),
                HOST_ID_AS_VARCHAR_IN_URI_AS + path + "/id");
      node2 = RestArgument.fillDetailedAllow(METHOD.DELETE, path + "/id",
                                             COMMAND_TYPE.DELETE.name(), node3,
                                             node1);
      node.add(node2);
    }
    if (methods.contains(METHOD.POST)) {
      node3 = JsonHandler.createObjectNode();
      for (final DbHostConfiguration.Columns column : DbHostConfiguration.Columns
          .values()) {
        node3.put(column.name(), DbHostConfiguration.dbTypes[column.ordinal()]);
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
  protected void put(final HttpRestHandler handler,
                     final RestArgument arguments, final RestArgument result,
                     final Object body)
      throws HttpIncorrectRequestException, HttpInvalidAuthenticationException,
             HttpNotFoundRequestException {
    super.put(handler, arguments, result, body);
    // according to what is updated and if concerned
    final DbHostConfiguration item = getItem(handler, arguments, result, body);
    if (item.getHostid().equals(Configuration.configuration.getHostId())) {
      DbHostConfiguration
          .updateHostConfiguration(Configuration.configuration, item);
    }
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
    if (!session.getAuth().isValidRole(ROLE.CONFIGADMIN)) {
      throw new HttpForbiddenRequestException(
          "Partner must have ConfigAdmin role");
    }
  }

}
