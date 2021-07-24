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
import org.joda.time.DateTime;
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
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.database.data.DbTaskRunner.Columns;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;

import java.sql.Timestamp;

/**
 * DbTaskRunner Rest handler
 */
public class DbTaskRunnerR66RestMethodHandler
    extends DataModelRestMethodHandler<DbTaskRunner> {
  private static final String OWNER_OF_THIS_REQUEST_OPTIONAL_AS_VARCHAR =
      "Owner of this request (optional) as VARCHAR";
  private static final String PARTNER_AS_REQUESTED_AS_VARCHAR =
      "Partner as requested as VARCHAR";
  private static final String PARTNER_AS_REQUESTER_AS_VARCHAR =
      "Partner as requester as VARCHAR";
  private static final String SPECIAL_ID_AS_LONG_IN_URI_AS =
      "Special Id as LONG in URI as ";
  public static final String BASEURI = "transfers";

  public enum FILTER_ARGS {
    LIMIT("number"), ORDERBYID("boolean"), STARTID("transfer id"),
    STOPID("transfer id"), IDRULE("rule name"),
    PARTNER("partner (requester or requested) name"), PENDING("boolean"),
    INTRANSFER("boolean"), INERROR("boolean"), DONE("boolean"),
    ALLSTATUS("boolean"), STARTTRANS("Date in ISO 8601 format or ms"),
    STOPTRANS("Date in ISO 8601 format or ms");

    public final String type;

    FILTER_ARGS(final String type) {
      this.type = type;
    }
  }

  /**
   * @param config
   * @param method
   */
  public DbTaskRunnerR66RestMethodHandler(final RestConfiguration config,
                                          final METHOD... method) {
    super(BASEURI, config, method);
  }

  @Override
  protected final DbTaskRunner getItem(final HttpRestHandler handler,
                                       final RestArgument arguments,
                                       final RestArgument result,
                                       final Object body)
      throws HttpNotFoundRequestException {
    try {
      HttpRestV1Utils.checkSanity(arguments);
    } catch (final InvalidArgumentException e) {
      throw new HttpNotFoundRequestException("Issue on values", e);
    }
    final ObjectNode arg = arguments.getUriArgs().deepCopy();
    arg.setAll(arguments.getBody());
    try {
      final JsonNode node = RestArgument.getId(arg);
      final long id;
      if (node.isMissingNode()) {
        // shall not be but continue however
        id = arg.path(DbTaskRunner.Columns.SPECIALID.name()).asLong();
      } else {
        id = node.asLong();
      }
      return new DbTaskRunner(id,
                              arg.path(DbTaskRunner.Columns.REQUESTER.name())
                                 .asText(),
                              arg.path(DbTaskRunner.Columns.REQUESTED.name())
                                 .asText(),
                              arg.path(DbTaskRunner.Columns.OWNERREQ.name())
                                 .asText());
    } catch (final WaarpDatabaseException e) {
      throw new HttpNotFoundRequestException(
          "Issue while reading from database " + arg, e);
    }
  }

  @Override
  protected final DbTaskRunner createItem(final HttpRestHandler handler,
                                          final RestArgument arguments,
                                          final RestArgument result,
                                          final Object body)
      throws HttpIncorrectRequestException {
    try {
      HttpRestV1Utils.checkSanity(arguments);
    } catch (final InvalidArgumentException e) {
      throw new HttpIncorrectRequestException("Issue on values", e);
    }
    final ObjectNode arg = arguments.getUriArgs().deepCopy();
    arg.setAll(arguments.getBody());
    try {
      return new DbTaskRunner(arg);
    } catch (final WaarpDatabaseException e) {
      throw new HttpIncorrectRequestException(
          "Issue while inserting into database", e);
    }
  }

  @Override
  protected final DbPreparedStatement getPreparedStatement(
      final HttpRestHandler handler, final RestArgument arguments,
      final RestArgument result, final Object body)
      throws HttpIncorrectRequestException {
    try {
      HttpRestV1Utils.checkSanity(arguments);
    } catch (final InvalidArgumentException e) {
      throw new HttpIncorrectRequestException("Issue on values", e);
    }
    final ObjectNode arg = arguments.getUriArgs().deepCopy();
    arg.setAll(arguments.getBody());
    final int limit = arg.path(FILTER_ARGS.LIMIT.name()).asInt(0);
    final boolean orderBySpecialId =
        arg.path(FILTER_ARGS.ORDERBYID.name()).asBoolean(false);
    JsonNode node = arg.path(FILTER_ARGS.STARTID.name());
    String startid = null;
    if (!node.isMissingNode()) {
      startid = node.asText();
    }
    if (ParametersChecker.isEmpty(startid)) {
      startid = null;
    }
    node = arg.path(FILTER_ARGS.STOPID.name());
    String stopid = null;
    if (!node.isMissingNode()) {
      stopid = node.asText();
    }
    if (ParametersChecker.isEmpty(stopid)) {
      stopid = null;
    }
    String rule = arg.path(FILTER_ARGS.IDRULE.name()).asText();
    if (ParametersChecker.isEmpty(rule)) {
      rule = null;
    }
    String req = arg.path(FILTER_ARGS.PARTNER.name()).asText();
    if (ParametersChecker.isEmpty(req)) {
      req = null;
    }
    String owner = arg.path(DbTaskRunner.Columns.OWNERREQ.name()).asText();
    if (ParametersChecker.isEmpty(owner)) {
      owner = null;
    }
    final boolean pending =
        arg.path(FILTER_ARGS.PENDING.name()).asBoolean(false);
    final boolean transfer =
        arg.path(FILTER_ARGS.INTRANSFER.name()).asBoolean(false);
    final boolean error = arg.path(FILTER_ARGS.INERROR.name()).asBoolean(false);
    final boolean done = arg.path(FILTER_ARGS.DONE.name()).asBoolean(false);
    final boolean all = arg.path(FILTER_ARGS.ALLSTATUS.name()).asBoolean(false);
    Timestamp start = null;
    node = arg.path(FILTER_ARGS.STARTTRANS.name());
    if (!node.isMissingNode()) {
      long val = node.asLong();
      if (val == 0) {
        final DateTime received = DateTime.parse(node.asText());
        val = received.getMillis();
      }
      start = new Timestamp(val);
    }
    Timestamp stop = null;
    node = arg.path(FILTER_ARGS.STOPTRANS.name());
    if (!node.isMissingNode()) {
      long val = node.asLong();
      if (val == 0) {
        final DateTime received = DateTime.parse(node.asText());
        val = received.getMillis();
      }
      stop = new Timestamp(val);
    }
    try {
      return DbTaskRunner.getFilterPrepareStatement(handler.getDbSession(),
                                                    limit, orderBySpecialId,
                                                    startid, stopid, start,
                                                    stop, rule, req, pending,
                                                    transfer, error, done, all,
                                                    owner);
    } catch (final WaarpDatabaseNoConnectionException e) {
      throw new HttpIncorrectRequestException(
          "Issue while reading from database", e);
    } catch (final WaarpDatabaseSqlException e) {
      throw new HttpIncorrectRequestException(
          "Issue while reading from database", e);
    }
  }

  @Override
  protected final DbTaskRunner getItemPreparedStatement(
      final DbPreparedStatement statement)
      throws HttpIncorrectRequestException, HttpNotFoundRequestException {
    try {
      return DbTaskRunner.getFromStatementNoDbRule(statement);
    } catch (final WaarpDatabaseNoConnectionException e) {
      throw new HttpIncorrectRequestException(
          "Issue while selecting from database", e);
    } catch (final WaarpDatabaseSqlException e) {
      throw new HttpNotFoundRequestException(
          "Issue while selecting from database", e);
    }
  }

  @Override
  protected final ArrayNode getDetailedAllow() {
    final ArrayNode node = JsonHandler.createArrayNode();

    final ObjectNode node1 = JsonHandler.createObjectNode();
    node1.put(AbstractDbData.JSON_MODEL, DbTaskRunner.class.getSimpleName());
    for (final DbTaskRunner.Columns column : DbTaskRunner.Columns.values()) {
      node1.put(column.name(), DbTaskRunner.dbTypes[column.ordinal()]);
    }

    ObjectNode node2;
    ObjectNode node3 = JsonHandler.createObjectNode();
    if (methods.contains(METHOD.GET)) {
      node3.put(DbTaskRunner.Columns.SPECIALID.name(),
                SPECIAL_ID_AS_LONG_IN_URI_AS + path + "/id");
      node3.put(DbTaskRunner.Columns.REQUESTER.name(),
                PARTNER_AS_REQUESTER_AS_VARCHAR);
      node3.put(DbTaskRunner.Columns.REQUESTED.name(),
                PARTNER_AS_REQUESTED_AS_VARCHAR);
      node3.put(DbTaskRunner.Columns.OWNERREQ.name(),
                OWNER_OF_THIS_REQUEST_OPTIONAL_AS_VARCHAR);
      node2 = RestArgument.fillDetailedAllow(METHOD.GET, path + "/id",
                                             COMMAND_TYPE.GET.name(), node3,
                                             node1);
      node.add(node2);

      node3 = JsonHandler.createObjectNode();
      for (final FILTER_ARGS arg : FILTER_ARGS.values()) {
        node3.put(arg.name(), arg.type);
      }
      node3.put(DbTaskRunner.Columns.OWNERREQ.name(),
                OWNER_OF_THIS_REQUEST_OPTIONAL_AS_VARCHAR);
      node2 = RestArgument.fillDetailedAllow(METHOD.GET, path,
                                             COMMAND_TYPE.MULTIGET.name(),
                                             node3,
                                             JsonHandler.createArrayNode()
                                                        .add(node1));
      node.add(node2);
    }
    if (methods.contains(METHOD.PUT)) {
      node3 = JsonHandler.createObjectNode();
      node3.put(DbTaskRunner.Columns.SPECIALID.name(),
                SPECIAL_ID_AS_LONG_IN_URI_AS + path + "/id");
      node3.put(DbTaskRunner.Columns.REQUESTER.name(),
                PARTNER_AS_REQUESTER_AS_VARCHAR);
      node3.put(DbTaskRunner.Columns.REQUESTED.name(),
                PARTNER_AS_REQUESTED_AS_VARCHAR);
      node3.put(DbTaskRunner.Columns.OWNERREQ.name(),
                OWNER_OF_THIS_REQUEST_OPTIONAL_AS_VARCHAR);
      for (final DbTaskRunner.Columns column : DbTaskRunner.Columns.values()) {
        if (column.name()
                  .equalsIgnoreCase(DbTaskRunner.Columns.IDRULE.name())) {
          continue;
        }
        node3.put(column.name(), DbTaskRunner.dbTypes[column.ordinal()]);
      }
      node2 = RestArgument.fillDetailedAllow(METHOD.PUT, path + "/id",
                                             COMMAND_TYPE.UPDATE.name(), node3,
                                             node1);
      node.add(node2);
    }
    if (methods.contains(METHOD.DELETE)) {
      node3 = JsonHandler.createObjectNode();
      node3.put(DbTaskRunner.Columns.SPECIALID.name(),
                SPECIAL_ID_AS_LONG_IN_URI_AS + path + "/id");
      node3.put(DbTaskRunner.Columns.REQUESTER.name(),
                PARTNER_AS_REQUESTER_AS_VARCHAR);
      node3.put(DbTaskRunner.Columns.REQUESTED.name(),
                PARTNER_AS_REQUESTED_AS_VARCHAR);
      node3.put(DbTaskRunner.Columns.OWNERREQ.name(),
                OWNER_OF_THIS_REQUEST_OPTIONAL_AS_VARCHAR);
      node2 = RestArgument.fillDetailedAllow(METHOD.DELETE, path + "/id",
                                             COMMAND_TYPE.DELETE.name(), node3,
                                             node1);
      node.add(node2);
    }
    if (methods.contains(METHOD.POST)) {
      node3 = JsonHandler.createObjectNode();
      for (final DbTaskRunner.Columns column : DbTaskRunner.Columns.values()) {
        node3.put(column.name(), DbTaskRunner.dbTypes[column.ordinal()]);
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

  @Override
  public final String getPrimaryPropertyName() {
    return Columns.SPECIALID.name();
  }

  @Override
  protected final void checkAuthorization(final HttpRestHandler handler,
                                          final RestArgument arguments,
                                          final RestArgument result,
                                          final METHOD method)
      throws HttpForbiddenRequestException {
    try {
      HttpRestV1Utils.checkSanity(arguments);
    } catch (final InvalidArgumentException e) {
      throw new HttpForbiddenRequestException("Issue on values", e);
    }
    final HttpRestR66Handler r66handler = (HttpRestR66Handler) handler;
    final R66Session session = r66handler.getServerHandler().getSession();
    if (!session.getAuth().isValidRole(ROLE.SYSTEM)) {
      throw new HttpForbiddenRequestException("Partner must have System role");
    }
  }

}
