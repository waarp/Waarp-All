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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cdap.http.HttpResponder;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.HostDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.pojo.Host;
import org.waarp.openr66.protocol.http.restv2.converters.HostConverter;
import org.waarp.openr66.protocol.http.restv2.errors.RestError;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;
import org.waarp.openr66.protocol.http.restv2.utils.JsonUtils;
import org.waarp.openr66.protocol.http.restv2.utils.RestUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static javax.ws.rs.core.HttpHeaders.*;
import static javax.ws.rs.core.MediaType.*;
import static org.waarp.openr66.dao.database.DBHostDAO.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.GetHostsParams.*;
import static org.waarp.openr66.protocol.http.restv2.errors.RestErrors.*;

/**
 * This is the {@link AbstractRestDbHandler} handling all requests made on the
 * host collection REST entry
 * point.
 */
@Path(HOSTS_HANDLER_URI)
public class HostsHandler extends AbstractRestDbHandler {

  /**
   * The content of the 'Allow' header sent when an 'OPTIONS' request is made
   * on the handler.
   */
  private static final HttpHeaders OPTIONS_HEADERS;

  static {
    OPTIONS_HEADERS = new DefaultHttpHeaders();
    final List<HttpMethod> allow = new ArrayList<HttpMethod>();
    allow.add(HttpMethod.GET);
    allow.add(HttpMethod.POST);
    allow.add(HttpMethod.OPTIONS);
    OPTIONS_HEADERS.add(ALLOW, allow);
  }

  /**
   * Initializes the handler with the given CRUD mask.
   *
   * @param crud the CRUD mask for this handler
   */
  public HostsHandler(final byte crud) {
    super(crud);
  }

  /**
   * Method called to get a list of host entries from the server's database,
   * with optional filters applied.
   *
   * @param request The {@link HttpRequest} made on the resource.
   * @param responder The {@link HttpResponder} which sends the reply
   *     to the request.
   * @param limit_str HTTP query parameter, maximum number of entries
   *     allowed in the response.
   * @param offset_str HTTP query parameter, index of the first
   *     accepted entry in the list of all valid
   *     answers.
   * @param order_str HTTP query parameter, the criteria used to sort
   *     the entries and the way of ordering.
   * @param address HTTP query parameter, filter only hosts with this
   *     address.
   * @param isSSL_str HTTP query parameter, filter only hosts that use
   *     SSL, or those that don't. Leave empty
   *     to get both.
   * @param isActive_str HTTP query parameter, filter hosts that are
   *     active, or those that aren't. Leave empty
   *     to get both.
   * @param countOrder if true is specified, it turns out to be a "count"
   *     request only
   */
  @GET
  @Consumes(APPLICATION_FORM_URLENCODED)
  @RequiredRole(ROLE.READONLY)
  public void filterHosts(final HttpRequest request,
                          final HttpResponder responder,
                          @QueryParam(LIMIT) @DefaultValue("20")
                          final String limit_str,
                          @QueryParam(OFFSET) @DefaultValue("0")
                          final String offset_str,
                          @QueryParam(ORDER) @DefaultValue("ascId")
                          final String order_str,
                          @QueryParam(ADDRESS) final String address,
                          @QueryParam(IS_SSL) final String isSSL_str,
                          @QueryParam(IS_ACTIVE) final String isActive_str,
                          @QueryParam(COUNT_ORDER) @DefaultValue("")
                          final String countOrder) {
    checkSanity(limit_str, offset_str, order_str, address, isActive_str,
                isActive_str, countOrder);
    final List<RestError> errors = new ArrayList<RestError>();

    boolean count = false;
    try {
      if (ParametersChecker.isNotEmpty(countOrder) &&
          RestUtils.stringToBoolean(countOrder)) {
        count = true;
      }
    } catch (final Exception ignore) {
      // Ignore
    }
    int limit = 20;
    int offset = 0;
    HostConverter.Order order = HostConverter.Order.ascId;
    try {
      limit = Integer.parseInt(limit_str);
    } catch (final NumberFormatException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(LIMIT, limit_str));
    }
    try {
      order = HostConverter.Order.valueOf(order_str);
    } catch (final IllegalArgumentException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(ORDER, order_str));
    }
    try {
      offset = Integer.parseInt(offset_str);
    } catch (final NumberFormatException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(OFFSET, offset_str));
    }

    if (limit < 0) {
      errors.add(ILLEGAL_PARAMETER_VALUE(LIMIT, limit_str));
    } else if (offset < 0) {
      errors.add(ILLEGAL_PARAMETER_VALUE(OFFSET, offset_str));
    }

    boolean isSSL = false;
    boolean isActive = false;
    if (ParametersChecker.isNotEmpty(isSSL_str)) {
      try {
        isSSL = RestUtils.stringToBoolean(isSSL_str);
      } catch (final IllegalArgumentException e) {
        errors.add(ILLEGAL_PARAMETER_VALUE(IS_SSL, isSSL_str));
      }
    }
    if (ParametersChecker.isNotEmpty(isActive_str)) {
      try {
        isActive = RestUtils.stringToBoolean(isActive_str);
      } catch (final IllegalArgumentException e) {
        errors.add(ILLEGAL_PARAMETER_VALUE(IS_ACTIVE, isActive_str));
      }
    }

    if (!errors.isEmpty()) {
      throw new RestErrorException(errors);
    }

    final List<Filter> filters = new ArrayList<Filter>();
    if (ParametersChecker.isNotEmpty(address)) {
      filters.add(new Filter(ADDRESS_FIELD, "=", address));
    }
    if (ParametersChecker.isNotEmpty(isSSL_str)) {
      filters.add(new Filter(IS_SSL_FIELD, "=", isSSL));
    }
    if (ParametersChecker.isNotEmpty(isActive_str)) {
      filters.add(new Filter(IS_ACTIVE_FIELD, "=", isActive));
    }
    HostDAO hostDAO = null;
    final ObjectNode responseObject = JsonHandler.createObjectNode();
    if (count) {
      long nbCount = -1;
      try {
        hostDAO = DAO_FACTORY.getHostDAO();
        nbCount = hostDAO.count(filters);
      } catch (final DAOConnectionException e) {
        throw new InternalServerErrorException(e);
      } finally {
        DAOFactory.closeDAO(hostDAO);
      }
      responseObject.put("totalResults", nbCount);
    } else {
      List<Host> hosts;
      try {
        hostDAO = DAO_FACTORY.getHostDAO();
        hosts = hostDAO.find(filters);
      } catch (final DAOConnectionException e) {
        throw new InternalServerErrorException(e);
      } finally {
        DAOFactory.closeDAO(hostDAO);
      }

      final int totalResults = hosts.size();
      Collections.sort(hosts, order.comparator);

      final ArrayNode results = JsonHandler.createArrayNode();
      for (int i = offset; i < offset + limit && i < hosts.size(); i++) {
        results.add(HostConverter.hostToNode(hosts.get(i)));
      }

      responseObject.put("totalResults", totalResults);
      responseObject.set("results", results);
    }
    final String responseText = JsonUtils.nodeToString(responseObject);
    responder.sendJson(OK, responseText);
  }

  /**
   * Method called to add a new host authentication entry to the server
   * database.
   *
   * @param request The {@link HttpRequest} made on the resource.
   * @param responder The {@link HttpResponder} which sends the reply
   *     to the request.
   */
  @POST
  @Consumes(APPLICATION_JSON)
  @RequiredRole(ROLE.HOST)
  public void addHost(final HttpRequest request,
                      final HttpResponder responder) {
    final ObjectNode requestObject = JsonUtils.deserializeRequest(request);
    checkSanity(requestObject);
    final Host host = HostConverter.nodeToNewHost(requestObject);

    HostDAO hostDAO = null;
    try {
      hostDAO = DAO_FACTORY.getHostDAO();

      if (!hostDAO.exist(host.getHostid())) {
        throw new RestErrorException(ALREADY_EXISTING(host.getHostid()));
      }

      hostDAO.insert(host);
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } finally {
      DAOFactory.closeDAO(hostDAO);
    }

    final ObjectNode responseObject = HostConverter.hostToNode(host);
    final String responseText = JsonUtils.nodeToString(responseObject);

    final DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add(CONTENT_TYPE, APPLICATION_JSON);
    headers.add("host-uri", HOSTS_HANDLER_URI + host.getHostid());

    responder.sendString(CREATED, responseText, headers);
  }

  /**
   * Method called to get a list of all allowed HTTP methods on this entry
   * point. The HTTP methods are sent as
   * an array in the reply's headers.
   *
   * @param request The {@link HttpRequest} made on the resource.
   * @param responder The {@link HttpResponder} which sends the reply
   *     to the request.
   */
  @OPTIONS
  @Consumes(WILDCARD)
  @RequiredRole(ROLE.NOACCESS)
  public void options(final HttpRequest request,
                      final HttpResponder responder) {
    responder.sendStatus(OK, OPTIONS_HEADERS);
  }
}
