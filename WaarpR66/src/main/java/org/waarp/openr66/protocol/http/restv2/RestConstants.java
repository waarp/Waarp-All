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

package org.waarp.openr66.protocol.http.restv2;

import io.netty.util.AsciiString;
import org.waarp.common.database.ConnectionFactory;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.ServerHandler;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.TransferIdHandler;

import java.nio.charset.Charset;

/**
 * A list of all constants of the RESTv2 API.
 * <p>
 * This includes the URI of all the entry points, the name of HTTP headers
 * specific to the API, the name of
 * the JSON objects' fields, and a {@link DAOFactory} to create DAOs for access
 * to the database.
 */
public final class RestConstants {

  static {
    DAOFactory.initialize(ConnectionFactory.getInstance());
    DAO_FACTORY = DAOFactory.getInstance();
  }

  /**
   * Makes the default constructor of this utility class inaccessible.
   */
  private RestConstants() throws InstantiationException {
    throw new InstantiationException(
        this.getClass().getName() + " cannot be instantiated.");
  }

  // ########################## SERVER CONSTANTS ##############################

  /**
   * The name of this R66 server instance.
   */
  public static final String SERVER_NAME =
      Configuration.configuration.getHOST_ID();

  /**
   * The DAO_FACTORY to generate connections to the underlying database.
   */
  public static final DAOFactory DAO_FACTORY;

  /**
   * The UTF-8 {@link java.nio.charset.Charset} constant.
   */
  public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

  // ######################### HTTP HEADER NAMES ##############################

  /**
   * Name of the HTTP header used to store the user who made a request.
   */
  public static final AsciiString AUTH_USER = AsciiString.cached("X-Auth-User");

  /**
   * Name of the HTTP header used to store the timestamp of the request.
   */
  public static final AsciiString AUTH_TIMESTAMP =
      AsciiString.cached("X-Auth-Timestamp");

  /**
   * Name of the HTTP header used to store the signature key of a request.
   */
  public static final AsciiString AUTH_SIGNATURE =
      AsciiString.cached("X-Auth-Key");

  // ########################## ENTRY POINTS URI ##############################

  /**
   * Root directory of the API.
   */
  public static final String VERSION_PREFIX = "/v2/";

  /**
   * Name of the URI parameter containing the id of an entry in a collection.
   */
  public static final String URI_ID = "id";

  /**
   * Regex corresponding to the id URI parameter of an entry in a collection.
   */
  private static final String ID_PARAMETER = "/{" + URI_ID + "}";

  /**
   * Access point of the transfers collection.
   */
  public static final String TRANSFERS_HANDLER_URI =
      VERSION_PREFIX + "transfers/";

  /**
   * Access point of a single transfer entry.
   */
  public static final String TRANSFER_ID_HANDLER_URI =
      TRANSFERS_HANDLER_URI + ID_PARAMETER;

  /**
   * Access point of the server commands.
   */
  public static final String SERVER_HANDLER_URI = VERSION_PREFIX + "server/";

  /**
   * Access point of the transfer rules collection.
   */
  public static final String RULES_HANDLER_URI = VERSION_PREFIX + "rules/";

  /**
   * Access point of a single transfer rules entry.
   */
  public static final String RULE_ID_HANDLER_URI =
      RULES_HANDLER_URI + ID_PARAMETER;

  /**
   * Access point of the bandwidth limits.
   */
  public static final String LIMITS_HANDLER_URI = VERSION_PREFIX + "limits/";

  /**
   * Access point of the hosts collection.
   */
  public static final String HOSTS_HANDLER_URI = VERSION_PREFIX + "hosts/";

  /**
   * Access point of a single host entry.
   */
  public static final String HOST_ID_HANDLER_URI =
      HOSTS_HANDLER_URI + ID_PARAMETER;

  /**
   * Access point of the host configuration.
   */
  public static final String CONFIG_HANDLER_URI =
      VERSION_PREFIX + "hostconfig/";

  /**
   * The names of all the sub-paths of the {@link ServerHandler} corresponding
   * to the server commands.
   */
  public static final class ServerCommandsURI {
    public static final String STATUS_URI = "status";
    public static final String DEACTIVATE_URI = "deactivate";
    public static final String SHUTDOWN_URI = "shutdown";
    public static final String RESTART_URI = "restart";
    public static final String LOGS_URI = "logs";
    public static final String CONFIG_URI = "config";
  }

  /**
   * The names of the sub-paths of the {@link TransferIdHandler} corresponding
   * to the transfer commands.
   */
  public static final class TransferCommandsURI {
    public static final String RESTART_URI = "restart";
    public static final String STOP_URI = "stop";
    public static final String CANCEL_URI = "cancel";
  }

  // ######################### JSON FIELDS NAMES ##############################

  /**
   * The names of the fields of a HostConfig JSON object.
   */
  public static final class HostConfigFields {
    public static final String BUSINESS = "business";
    public static final String ROLES = "roles";
    public static final String ALIASES = "aliases";
    public static final String OTHERS = "others";
    public static final String HOST_NAME = "hostName";
    public static final String ROLE_LIST = "roleList";
    public static final String ALIAS_LIST = "aliasList";
  }

  /**
   * The names of the fields of a Host JSON object.
   */
  public static final class HostFields {
    public static final String HOST_NAME = "name";
    public static final String ADDRESS = "address";
    public static final String PORT = "port";
    public static final String PASSWORD = "password";
    public static final String IS_SSL = "isSSL";
    public static final String IS_CLIENT = "isClient";
    public static final String IS_ADMIN = "isAdmin";
    public static final String IS_ACTIVE = "isActive";
    public static final String IS_PROXY = "isProxy";
  }

  /**
   * The names of the fields of a Limits JSON object.
   */
  public static final class LimitsFields {
    public static final String WRITE_GLOBAL_LIMIT = "upGlobalLimit";
    public static final String READ_GLOBAL_LIMIT = "downGlobalLimit";
    public static final String WRITE_SESSION_LIMIT = "upSessionLimit";
    public static final String READ_SESSION_LIMIT = "downSessionLimit";
    public static final String DELAY_LIMIT = "delayLimit";
  }

  /**
   * The names of the fields of a Rule JSON object.
   */
  public static final class RuleFields {
    public static final String RULE_NAME = "name";
    public static final String HOST_IDS = "hostIds";
    public static final String MODE_TRANS = "mode";
    public static final String RECV_PATH = "recvPath";
    public static final String SEND_PATH = "sendPath";
    public static final String ARCHIVE_PATH = "archivePath";
    public static final String WORK_PATH = "workPath";
    public static final String R_PRE_TASKS = "rPreTasks";
    public static final String R_POST_TASKS = "rPostTasks";
    public static final String R_ERROR_TASKS = "sPreTasks";
    public static final String S_PRE_TASKS = "rPreTasks";
    public static final String S_POST_TASKS = "rPostTasks";
    public static final String S_ERROR_TASKS = "sErrorTasks";
    public static final String TASK_TYPE = "type";
    public static final String TASK_ARGUMENTS = "arguments";
    public static final String TASK_DELAY = "delay";
  }

  /**
   * The names of the fields of a Transfer JSON object.
   */
  public static final class TransferFields {
    public static final String TRANSFER_ID = "id";
    public static final String GLOBAL_STEP = "globalStep";
    public static final String GLOBAL_LAST_STEP = "globalLastStep";
    public static final String STEP = "step";
    public static final String RANK = "rank";
    public static final String UPDATED_INFO = "status";
    public static final String STEP_STATUS = "stepStatus";
    public static final String ORIGINAL_FILENAME = "originalFilename";
    public static final String FILENAME = "filename";
    public static final String RULE = "ruleName";
    public static final String BLOCK_SIZE = "blockSize";
    public static final String FILE_INFO = "fileInfo";
    public static final String TRANSFER_INFO = "transferInfo";
    public static final String START = "start";
    public static final String STOP = "stop";
    public static final String REQUESTED = "requested";
    public static final String REQUESTER = "requester";
    public static final String RETRIEVE = "retrieve";
    public static final String ERROR_CODE = "errorCode";
    public static final String ERROR_MESSAGE = "errorMessage";
  }

  // ######################### QUERY PARAM NAMES ##############################

  /**
   * The names of the query parameters of the {@code GET} method on the host
   * collection entry point.
   */
  public static final class GetHostsParams {
    public static final String LIMIT = "limit";
    public static final String OFFSET = "offset";
    public static final String ORDER = "order";
    public static final String ADDRESS = "address";
    public static final String IS_SSL = "isSSL";
    public static final String IS_ACTIVE = "isActive";
  }

  /**
   * The names of the query parameters of the {@code GET} method on the rule
   * collection entry point.
   */
  public static final class GetRulesParams {
    public static final String LIMIT = "limit";
    public static final String OFFSET = "offset";
    public static final String ORDER = "order";
    public static final String MODE_TRANS = "modeTrans";
  }

  /**
   * The names of the query parameters of the {@code GET} method on the server
   * status entry point.
   */
  public static final class GetStatusParams {
    public static final String PERIOD = "period";

  }

  /**
   * The names of the query parameters of the {@code GET} method on the
   * transfer logs entry point.
   */
  public static final class GetLogsParams {
    public static final String PURGE = "purge";
    public static final String CLEAN = "clean";
    public static final String STATUS = "status";
    public static final String RULE_NAME = "ruleName";
    public static final String START = "start";
    public static final String STOP = "stop";
    public static final String START_ID = "startID";
    public static final String STOP_ID = "stopID";
    public static final String REQUESTED = "requester";
  }

  /**
   * The names of the query parameters of the {@code GET} method on the server
   * configuration entry point.
   */
  public static final class ExportConfigParams {
    public static final String EXPORT_HOSTS = "exportHosts";
    public static final String EXPORT_RULES = "exportRules";
    public static final String EXPORT_BUSINESS = "exportBusiness";
    public static final String EXPORT_ALIASES = "exportAliases";
    public static final String EXPORT_ROLES = "exportRoles";
  }

  /**
   * The names of the query parameters of the {@code PUT} method on the server
   * configuration entry point.
   */
  public static final class ImportConfigParams {
    public static final String PURGE_HOST = "purgeHosts";
    public static final String PURGE_RULE = "purgeRules";
    public static final String PURGE_BUSINESS = "purgeBusiness";
    public static final String PURGE_ALIASES = "purgeAliases";
    public static final String PURGE_ROLES = "purgeRoles";
    public static final String HOST_FILE = "hostsFile";
    public static final String RULE_FILE = "rulesFile";
    public static final String BUSINESS_FILE = "businessFile";
    public static final String ALIAS_FILE = "aliasesFile";
    public static final String ROLE_FILE = "rolesFile";
  }

  /**
   * The names of the query parameters of the {@code GET} method on the
   * transfer collection entry point.
   */
  public static final class GetTransfersParams {
    public static final String LIMIT = "limit";
    public static final String OFFSET = "offset";
    public static final String ORDER = "order";
    public static final String RULE_ID = "ruleID";
    public static final String PARTNER = "partner";
    public static final String STATUS = "status";
    public static final String FILENAME = "filename";
    public static final String START_TRANS = "startTrans";
    public static final String STOP_TRANS = "stopTrans";
  }
}
