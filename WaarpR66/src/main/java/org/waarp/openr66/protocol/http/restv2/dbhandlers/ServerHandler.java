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

package org.waarp.openr66.protocol.http.restv2.dbhandlers;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cdap.http.HttpResponder;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.gateway.kernel.rest.RestConfiguration.CRUD;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.dao.BusinessDAO;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.HostDAO;
import org.waarp.openr66.dao.RuleDAO;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Business;
import org.waarp.openr66.pojo.Host;
import org.waarp.openr66.pojo.Rule;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.pojo.UpdatedInfo;
import org.waarp.openr66.protocol.http.restv2.converters.ServerStatusMaker;
import org.waarp.openr66.protocol.http.restv2.errors.RestError;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;
import org.waarp.openr66.protocol.http.restv2.utils.JsonUtils;
import org.waarp.openr66.protocol.http.restv2.utils.RestUtils;
import org.waarp.openr66.protocol.http.restv2.utils.XmlSerializable.Hosts;
import org.waarp.openr66.protocol.http.restv2.utils.XmlSerializable.Rules;
import org.waarp.openr66.protocol.http.restv2.utils.XmlSerializable.Transfers;
import org.waarp.openr66.protocol.http.restv2.utils.XmlUtils;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.lang.Boolean.*;
import static javax.ws.rs.core.HttpHeaders.*;
import static javax.ws.rs.core.MediaType.*;
import static org.waarp.common.role.RoleDefault.ROLE.*;
import static org.waarp.openr66.dao.database.DBTransferDAO.*;
import static org.waarp.openr66.protocol.configuration.Configuration.*;
import static org.waarp.openr66.protocol.http.rest.HttpRestR66Handler.RESTHANDLERS.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.ExportConfigParams.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.GetLogsParams.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.GetStatusParams.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.ImportConfigParams.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.ServerCommandsURI.*;
import static org.waarp.openr66.protocol.http.restv2.errors.RestErrors.*;

/**
 * This is the {@link AbstractRestDbHandler} handling all requests made on the
 * server commands REST entry
 * point.
 */

@Path(SERVER_HANDLER_URI)
public class ServerHandler extends AbstractRestDbHandler {

  /**
   * Stores the path to the archive directory.
   */
  private static final String ARCH_PATH =
      configuration.getBaseDirectory() + configuration.getArchivePath();

  /**
   * Stores the path to the configuration directory.
   */
  private static final String CONFIGS_PATH =
      configuration.getBaseDirectory() + configuration.getConfigPath();

  /**
   * Stores a {@link Map} associating each sub-path of the handler to their
   * respective CRUD configuration.
   */
  private final Map<String, Byte> serverCRUD = new HashMap<String, Byte>();

  /**
   * Instantiates the handler with the given CRUD configuration array.
   *
   * @param crud An array of byte containing all the REST CRUD
   *     configurations.
   */
  public ServerHandler(byte[] crud) {
    super((byte) 0);
    serverCRUD.put(STATUS_URI, crud[Information.ordinal()]);
    serverCRUD.put(DEACTIVATE_URI, crud[Server.ordinal()]);
    serverCRUD.put(SHUTDOWN_URI, crud[Server.ordinal()]);
    serverCRUD.put(RESTART_URI, crud[Server.ordinal()]);
    serverCRUD.put(LOGS_URI, crud[Log.ordinal()]);
    serverCRUD.put(CONFIG_URI, crud[Config.ordinal()]);
  }

  /**
   * Checks if the request can be made in consideration to the handler's CRUD
   * configuration.
   *
   * @param request the HttpRequest made to the handler
   *
   * @return {@code true} if the request is valid, {@code false} if the CRUD
   *     configuration does not allow this
   *     request
   */
  @Override
  public boolean checkCRUD(HttpRequest request) {
    if (request.method().equals(HttpMethod.OPTIONS)) {
      return true;
    }

    final Pattern pattern =
        Pattern.compile('(' + SERVER_HANDLER_URI + ")(\\w+)(\\?.+)?");
    final Matcher matcher = pattern.matcher(request.uri());
    final HttpMethod method = request.method();

    Byte crud;
    if (!matcher.find()) {
      crud = 0;
    } else {
      final String subPath = matcher.group(2);
      crud = serverCRUD.get(subPath);
    }

    if (crud == null) {
      return false;
    } else if (method.equals(HttpMethod.GET)) {
      return CRUD.READ.isValid(crud);
    } else if (method.equals(HttpMethod.POST)) {
      return CRUD.CREATE.isValid(crud);
    } else if (method.equals(HttpMethod.DELETE)) {
      return CRUD.DELETE.isValid(crud);
    } else if (method.equals(HttpMethod.PUT)) {
      return CRUD.UPDATE.isValid(crud);
    } else {
      return false;
    }
  }

  /**
   * Get the general status of the server.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   */
  @Path(STATUS_URI)
  @GET
  @Consumes(WILDCARD)
  @RequiredRole(READONLY)
  public void getStatus(HttpRequest request, HttpResponder responder,
                        @QueryParam(PERIOD) @DefaultValue("P1DT0H0M0S")
                            String periodStr) {
    try {
      final Period period = Period.parse(periodStr);
      final ObjectNode status = ServerStatusMaker.exportAsJson(period);
      final String responseText = JsonUtils.nodeToString(status);
      responder.sendJson(OK, responseText);
    } catch (final IllegalArgumentException e) {
      throw new RestErrorException(ILLEGAL_PARAMETER_VALUE(PERIOD, periodStr));
    } catch (final UnsupportedOperationException e) {
      throw new RestErrorException(ILLEGAL_PARAMETER_VALUE(PERIOD, periodStr));
    }
  }

  /**
   * Method called to get a list of all allowed HTTP methods on the
   * '/server/status' entry point. The HTTP
   * methods are sent as an array in the reply's headers.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request.
   */
  @Path(STATUS_URI)
  @OPTIONS
  @Consumes(WILDCARD)
  @RequiredRole(NOACCESS)
  public void status_options(HttpRequest request, HttpResponder responder) {
    final HttpHeaders allow = new DefaultHttpHeaders();
    final List<HttpMethod> options = new ArrayList<HttpMethod>();
    options.add(HttpMethod.GET);
    options.add(HttpMethod.OPTIONS);
    allow.add(ALLOW, options);
    responder.sendStatus(OK, allow);
  }

  /**
   * Deactivates the server so that it doesn't accept any new transfer
   * request.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   */
  @Path(DEACTIVATE_URI)
  @PUT
  @Consumes(WILDCARD)
  @RequiredRole(FULLADMIN)
  public void deactivate(HttpRequest request, HttpResponder responder) {
    HostDAO hostDAO = null;
    try {
      hostDAO = DAO_FACTORY.getHostDAO();
      final Host host = hostDAO.select(serverName());
      host.setActive(!host.isActive());
      hostDAO.update(host);

      final DefaultHttpHeaders headers = new DefaultHttpHeaders();
      headers.add("active", host.isActive());
      responder.sendStatus(NO_CONTENT, headers);
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } catch (final DAONoDataException e) {
      responder.sendStatus(NOT_FOUND);
    } finally {
      DAOFactory.closeDAO(hostDAO);
    }
  }

  /**
   * Shut down the server.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   */
  @Path(SHUTDOWN_URI)
  @PUT
  @Consumes(WILDCARD)
  @RequiredRole(FULLADMIN)
  public void shutdown(HttpRequest request, HttpResponder responder) {
    WaarpShutdownHook.setRestart(false);
    ChannelUtils.startShutdown();
    responder.sendStatus(NO_CONTENT);
  }

  /**
   * Method called to get a list of all allowed HTTP methods on the
   * '/server/shutdown' entry point. The HTTP
   * methods are sent as an array in the reply's headers.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request.
   */
  @Path(SHUTDOWN_URI)
  @OPTIONS
  @Consumes(WILDCARD)
  @RequiredRole(NOACCESS)
  public void shutdown_options(HttpRequest request, HttpResponder responder) {
    final HttpHeaders allow = new DefaultHttpHeaders();
    final List<HttpMethod> options = new ArrayList<HttpMethod>();
    options.add(HttpMethod.PUT);
    options.add(HttpMethod.OPTIONS);
    allow.add(ALLOW, options);
    responder.sendStatus(OK, allow);
  }

  /**
   * Restart the server.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   */
  @Path(RESTART_URI)
  @PUT
  @Consumes(WILDCARD)
  @RequiredRole(FULLADMIN)
  public void restart(HttpRequest request, HttpResponder responder) {
    WaarpShutdownHook.setRestart(true);
    ChannelUtils.startShutdown();
    responder.sendStatus(NO_CONTENT);
  }

  /**
   * Export the server logs to a file. Only the entries that satisfy the
   * desired filters will be exported.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   * @param purgeStr states whether to delete exported entries or not
   * @param cleanStr states whether to fix the incoherent entries
   * @param statusStr only transfers with this status will be
   *     exported
   * @param rule only transfers using this rule will be exported
   * @param start lower bound for the date of the transfer
   * @param stop upper bound for the date of the transfer
   * @param startID lower bound for the transfer's ID
   * @param stopID upper bound for the transfer's ID
   */
  @Path(LOGS_URI)
  @GET
  @Consumes(APPLICATION_FORM_URLENCODED)
  @RequiredRole(LOGCONTROL)
  public void getLogs(HttpRequest request, HttpResponder responder,
                      @QueryParam(PURGE) @DefaultValue("false") String purgeStr,
                      @QueryParam(CLEAN) @DefaultValue("false") String cleanStr,
                      @QueryParam(STATUS) @DefaultValue("") String statusStr,
                      @QueryParam(RULE_NAME) @DefaultValue("") String rule,
                      @QueryParam(START) @DefaultValue("") String start,
                      @QueryParam(STOP) @DefaultValue("") String stop,
                      @QueryParam(START_ID) @DefaultValue("") String startID,
                      @QueryParam(STOP_ID) @DefaultValue("") String stopID,
                      @QueryParam(REQUESTED) @DefaultValue("")
                          String requester) {

    final List<RestError> errors = new ArrayList<RestError>();
    RestUtils.getLocale(request);
    final List<Filter> filters = new ArrayList<Filter>();
    final String filePath =
        ARCH_PATH + File.separator + serverName() + "_export_" +
        DateTime.now() + ".xml";

    boolean purge = false;
    boolean clean = false;
    try {
      purge = RestUtils.stringToBoolean(purgeStr);
    } catch (final IllegalArgumentException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(PURGE, purgeStr));
    }
    try {
      clean = RestUtils.stringToBoolean(cleanStr);
    } catch (final IllegalArgumentException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(CLEAN, cleanStr));
    }

    if (!start.isEmpty()) {
      try {
        final DateTime lowerDate = DateTime.parse(start);
        filters.add(new Filter(TRANSFER_START_FIELD, ">=", lowerDate));
      } catch (final IllegalArgumentException e) {
        errors.add(ILLEGAL_PARAMETER_VALUE(START, start));
      }
    }
    if (!stop.isEmpty()) {
      try {
        final DateTime upperDate = DateTime.parse(stop);
        filters.add(new Filter(TRANSFER_START_FIELD, "<=", upperDate));
      } catch (final IllegalArgumentException e) {
        errors.add(ILLEGAL_PARAMETER_VALUE(STOP, stop));
      }
    }
    if (!startID.isEmpty()) {
      try {
        final Long lowerID = Long.parseLong(startID);
        filters.add(new Filter(ID_FIELD, ">=", lowerID));
      } catch (final NumberFormatException e) {
        errors.add(ILLEGAL_PARAMETER_VALUE(START_ID, startID));
      }
    }
    if (!stopID.isEmpty()) {
      try {
        final Long upperID = Long.parseLong(stopID);
        filters.add(new Filter(ID_FIELD, "<=", upperID));
      } catch (final NumberFormatException e) {
        errors.add(ILLEGAL_PARAMETER_VALUE(STOP_ID, stopID));
      }
    }
    if (!rule.isEmpty()) {
      filters.add(new Filter(ID_RULE_FIELD, "=", rule));
    }
    if (!statusStr.isEmpty()) {
      try {
        final UpdatedInfo status = UpdatedInfo.valueOf(statusStr);
        filters.add(new Filter(UPDATED_INFO_FIELD, "=", status.ordinal()));
      } catch (final IllegalArgumentException e) {
        errors.add(ILLEGAL_PARAMETER_VALUE(STATUS, statusStr));
      }
    }

    if (!requester.isEmpty()) {
      filters.add(new Filter(REQUESTER_FIELD, "=", requester));
    }

    if (!errors.isEmpty()) {
      throw new RestErrorException(errors);
    }
    TransferDAO transferDAO = null;
    try {
      transferDAO = DAO_FACTORY.getTransferDAO();
      final Transfers transfers = new Transfers(transferDAO.find(filters));
      final int exported = transfers.transfers.size();

      XmlUtils.saveObject(transfers, filePath);
      int purged = 0;
      if (purge) {
        for (final Transfer transfer : transfers.transfers) {
          transferDAO.delete(transfer);
          ++purged;
        }
      }
      // Update all UpdatedInfo to DONE
      // where GlobalLastStep = ALLDONETASK and status = CompleteOk
      if (clean) {
        for (final Transfer transfer : transfers.transfers) {
          if (transfer.getGlobalStep() == Transfer.TASKSTEP.ALLDONETASK &&
              transfer.getInfoStatus() == ErrorCode.CompleteOk) {
            transfer.setUpdatedInfo(UpdatedInfo.DONE);
            transferDAO.update(transfer);
          }
        }
      }

      final ObjectNode responseObject =
          new ObjectNode(JsonNodeFactory.instance);
      responseObject.put("filePath", filePath);
      responseObject.put("exported", exported);
      responseObject.put("purged", purged);
      final String responseText = JsonUtils.nodeToString(responseObject);
      responder.sendJson(OK, responseText);

    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } catch (final DAONoDataException e) {
      responder.sendStatus(NOT_FOUND);
    } finally {
      DAOFactory.closeDAO(transferDAO);
    }
  }

  /**
   * Exports parts of the current server configuration to multiple XML files,
   * depending on the parameters of the
   * request.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   * @param hostStr states whether to export the host database or
   *     not.
   * @param ruleStr states whether to export the rules database or
   *     not.
   * @param businessStr states whether to export the host's business
   *     or not.
   * @param aliasStr states whether to export the host's aliases or
   *     not.
   * @param roleStr states whether to export the host's permission
   *     database or not.
   */
  @Path(CONFIG_URI)
  @GET
  @Consumes(APPLICATION_FORM_URLENCODED)
  @RequiredRole(CONFIGADMIN)
  public void getConfig(HttpRequest request, HttpResponder responder,
                        @QueryParam(EXPORT_HOSTS) @DefaultValue("false")
                            String hostStr,
                        @QueryParam(EXPORT_RULES) @DefaultValue("false")
                            String ruleStr,
                        @QueryParam(EXPORT_BUSINESS) @DefaultValue("false")
                            String businessStr,
                        @QueryParam(EXPORT_ALIASES) @DefaultValue("false")
                            String aliasStr,
                        @QueryParam(EXPORT_ROLES) @DefaultValue("false")
                            String roleStr) {

    final List<RestError> errors = new ArrayList<RestError>();

    boolean host = false;
    boolean rule = false;
    boolean business = false;
    boolean alias = false;
    boolean role = false;

    try {
      host = RestUtils.stringToBoolean(hostStr);
    } catch (final IllegalArgumentException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(EXPORT_HOSTS, hostStr));
    }
    try {
      rule = RestUtils.stringToBoolean(ruleStr);
    } catch (final IllegalArgumentException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(EXPORT_RULES, ruleStr));
    }
    try {
      business = RestUtils.stringToBoolean(businessStr);
    } catch (final IllegalArgumentException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(EXPORT_BUSINESS, businessStr));
    }
    try {
      alias = RestUtils.stringToBoolean(aliasStr);
    } catch (final IllegalArgumentException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(EXPORT_ALIASES, aliasStr));
    }
    try {
      role = RestUtils.stringToBoolean(roleStr);
    } catch (final IllegalArgumentException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(EXPORT_ROLES, roleStr));
    }
    if (!errors.isEmpty()) {
      throw new RestErrorException(errors);
    }

    final String hostsFilePath =
        CONFIGS_PATH + File.separator + serverName() + "_hosts.xml";
    final String rulesFilePath =
        CONFIGS_PATH + File.separator + serverName() + "_rules.xml";
    final String businessFilePath =
        CONFIGS_PATH + File.separator + serverName() + "_business.xml";
    final String aliasFilePath =
        CONFIGS_PATH + File.separator + serverName() + "_aliases.xml";
    final String rolesFilePath =
        CONFIGS_PATH + File.separator + serverName() + "_roles.xml";

    final ObjectNode responseObject = new ObjectNode(JsonNodeFactory.instance);

    HostDAO hostDAO = null;
    RuleDAO ruleDAO = null;
    BusinessDAO businessDAO = null;
    try {
      if (host) {
        hostDAO = DAO_FACTORY.getHostDAO();
        final List<Host> hostList = hostDAO.getAll();

        final Hosts hosts = new Hosts(hostList);

        XmlUtils.saveObject(hosts, hostsFilePath);
        responseObject.put("fileHost", hostsFilePath);
      }
      if (rule) {
        ruleDAO = DAO_FACTORY.getRuleDAO();
        final Rules rules = new Rules(ruleDAO.getAll());

        XmlUtils.saveObject(rules, rulesFilePath);
        responseObject.put("fileRule", rulesFilePath);
      }
      businessDAO = DAO_FACTORY.getBusinessDAO();
      final Business businessEntry = businessDAO.select(serverName());
      if (business) {
        final String businessXML = businessEntry.getBusiness();

        XmlUtils.saveXML(businessXML, businessFilePath);
        responseObject.put("fileBusiness", businessFilePath);
      }
      if (alias) {
        final String aliasXML = businessEntry.getAliases();

        XmlUtils.saveXML(aliasXML, aliasFilePath);
        responseObject.put("fileAlias", aliasFilePath);
      }
      if (role) {
        final String rolesXML = businessEntry.getRoles();

        XmlUtils.saveXML(rolesXML, rolesFilePath);
        responseObject.put("fileRoles", rolesFilePath);
      }

      final String responseText = JsonUtils.nodeToString(responseObject);
      responder.sendJson(OK, responseText);

    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } catch (final DAONoDataException e) {
      responder.sendStatus(NOT_FOUND);
    } finally {
      DAOFactory.closeDAO(hostDAO);
      DAOFactory.closeDAO(ruleDAO);
      DAOFactory.closeDAO(businessDAO);
    }
  }

  /**
   * Imports different parts of the server configuration from the XML files
   * given as parameters of the request.
   * These imported values will replace those already present in the
   * database.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   * @param purgeHostStr states if a new host database should be
   *     imported.
   * @param purgeRuleStr states if a new transfer rule database
   *     should be imported
   * @param purgeBusinessStr states if a new business database should
   *     be imported
   * @param purgeAliasStr states if a new alias database should be
   *     imported
   * @param purgeRoleStr states if a new role database should be
   *     imported
   * @param hostFile path to the XML file containing the host database
   *     to import
   * @param ruleFile path to the XML file containing the rule database
   *     to import
   * @param businessFile path to the XML file containing the business
   *     database to import
   * @param aliasFile path to the XML file containing the alias
   *     database to import
   * @param roleFile path to the XML file containing the role database
   *     to import
   */
  @Path(CONFIG_URI)
  @PUT
  @Consumes(APPLICATION_FORM_URLENCODED)
  @RequiredRole(CONFIGADMIN)
  public void setConfig(HttpRequest request, HttpResponder responder,
                        @QueryParam(PURGE_HOST) @DefaultValue("false")
                            String purgeHostStr,
                        @QueryParam(PURGE_RULE) @DefaultValue("false")
                            String purgeRuleStr,
                        @QueryParam(PURGE_BUSINESS) @DefaultValue("false")
                            String purgeBusinessStr,
                        @QueryParam(PURGE_ALIASES) @DefaultValue("false")
                            String purgeAliasStr,
                        @QueryParam(PURGE_ROLES) @DefaultValue("false")
                            String purgeRoleStr,
                        @QueryParam(HOST_FILE) @DefaultValue("")
                            String hostFile,
                        @QueryParam(RULE_FILE) @DefaultValue("")
                            String ruleFile,
                        @QueryParam(BUSINESS_FILE) @DefaultValue("")
                            String businessFile,
                        @QueryParam(ALIAS_FILE) @DefaultValue("")
                            String aliasFile,
                        @QueryParam(ROLE_FILE) @DefaultValue("")
                            String roleFile) {

    final List<RestError> errors = new ArrayList<RestError>();
    RestUtils.getLocale(request);

    boolean purgeHost = false;
    boolean purgeRule = false;
    boolean purgeBusiness = false;
    boolean purgeAlias = false;
    boolean purgeRole = false;

    try {
      purgeHost = RestUtils.stringToBoolean(purgeHostStr);
    } catch (final IllegalArgumentException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(EXPORT_HOSTS, purgeHostStr));
    }
    try {
      purgeRule = RestUtils.stringToBoolean(purgeRuleStr);
    } catch (final IllegalArgumentException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(EXPORT_RULES, purgeRuleStr));
    }
    try {
      purgeBusiness = RestUtils.stringToBoolean(purgeBusinessStr);
    } catch (final IllegalArgumentException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(EXPORT_BUSINESS, purgeBusinessStr));
    }
    try {
      purgeAlias = RestUtils.stringToBoolean(purgeAliasStr);
    } catch (final IllegalArgumentException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(EXPORT_ALIASES, purgeAliasStr));
    }
    try {
      purgeRole = RestUtils.stringToBoolean(purgeRoleStr);
    } catch (final IllegalArgumentException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(EXPORT_ROLES, purgeRoleStr));
    }
    if (!errors.isEmpty()) {
      throw new RestErrorException(errors);
    }

    HostDAO hostDAO = null;
    RuleDAO ruleDAO = null;
    BusinessDAO businessDAO = null;

    final ObjectNode responseObject = new ObjectNode(JsonNodeFactory.instance);

    try {
      hostDAO = DAO_FACTORY.getHostDAO();
      final Hosts hosts = XmlUtils.loadObject(hostFile, Hosts.class);

      // if a purge is requested, we can add the new entries without
      // checking with 'exist' to gain performance
      if (purgeHost) {
        hostDAO.deleteAll();
        for (final Host host : hosts.hosts) {
          hostDAO.insert(host);
        }
      } else {
        for (final Host host : hosts.hosts) {
          if (hostDAO.exist(host.getHostid())) {
            hostDAO.update(host);
          } else {
            hostDAO.insert(host);
          }
        }
      }
      responseObject.put("purgedHost", TRUE.toString());

      ruleDAO = DAO_FACTORY.getRuleDAO();

      final Rules rules = XmlUtils.loadObject(ruleFile, Rules.class);

      if (purgeRule) {
        ruleDAO.deleteAll();
        for (final Rule rule : rules.rules) {
          ruleDAO.insert(rule);
        }
      } else {
        for (final Rule rule : rules.rules) {
          if (ruleDAO.exist(rule.getName())) {
            ruleDAO.update(rule);
          } else {
            ruleDAO.insert(rule);
          }
        }
      }

      responseObject.put("purgedRule", TRUE.toString());

      businessDAO = DAO_FACTORY.getBusinessDAO();
      if (purgeBusiness) {
        final Business business = businessDAO.select(serverName());

        final String new_business = XmlUtils.loadXML(businessFile);
        business.setBusiness(new_business);
        businessDAO.update(business);
        responseObject.put("purgedBusiness", TRUE.toString());
      }
      if (purgeAlias) {
        final Business business = businessDAO.select(serverName());
        business.setAliases(XmlUtils.loadXML(aliasFile));
        businessDAO.update(business);
        responseObject.put("purgedAlias", TRUE.toString());
      }
      if (purgeRole) {
        final Business business = businessDAO.select(serverName());
        business.setRoles(XmlUtils.loadXML(roleFile));
        businessDAO.update(business);
        responseObject.put("purgedRoles", TRUE.toString());
      }

      if (errors.isEmpty()) {
        responder.sendJson(OK, JsonUtils.nodeToString(responseObject));
      } else {
        throw new RestErrorException(errors);
      }

    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } catch (final DAONoDataException e) {
      responder.sendStatus(NOT_FOUND);
    } finally {
      DAOFactory.closeDAO(hostDAO);
      DAOFactory.closeDAO(ruleDAO);
      DAOFactory.closeDAO(businessDAO);
    }
  }

  /**
   * Method called to get a list of all allowed HTTP methods on the '/server'
   * entry point. The HTTP methods are
   * sent as an array in the reply's headers.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request.
   */
  @OPTIONS
  @Consumes(WILDCARD)
  @RequiredRole(NOACCESS)
  public void options(HttpRequest request, HttpResponder responder) {
    final HttpHeaders allow = new DefaultHttpHeaders();
    allow.add(ALLOW, HttpMethod.OPTIONS);
    responder.sendStatus(OK, allow);
  }

  /**
   * Method called to get a list of all allowed HTTP methods on all sub entry
   * points of the '/server' entry
   * point. The HTTP methods are sent as an array in the reply's headers.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request.
   */
  @Path("{ep}")
  @OPTIONS
  @Consumes(WILDCARD)
  @RequiredRole(NOACCESS)
  public void command_options(HttpRequest request, HttpResponder responder,
                              @PathParam("ep") String ep) {
    final HttpHeaders allow = new DefaultHttpHeaders();
    final List<HttpMethod> options = new ArrayList<HttpMethod>();

    if (ep.equals(STATUS_URI)) {
      options.add(HttpMethod.GET);
    } else if (ep.equals(DEACTIVATE_URI)) {
      options.add(HttpMethod.PUT);
    } else if (ep.equals(SHUTDOWN_URI)) {
      options.add(HttpMethod.PUT);
    } else if (ep.equals(RESTART_URI)) {
      options.add(HttpMethod.PUT);
    } else if (ep.equals(LOGS_URI)) {
      options.add(HttpMethod.GET);
    } else if (ep.equals(CONFIG_URI)) {
      options.add(HttpMethod.PUT);
    } else {
      responder.sendStatus(FOUND);
      return;
    }
    options.add(HttpMethod.OPTIONS);
    allow.add(ALLOW, options);
    responder.sendStatus(OK, allow);
  }
}