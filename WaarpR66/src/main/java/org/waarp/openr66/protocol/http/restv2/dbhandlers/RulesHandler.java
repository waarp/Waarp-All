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
import org.waarp.openr66.dao.RuleDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.pojo.Rule;
import org.waarp.openr66.protocol.http.restv2.errors.RestError;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;
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
import static org.waarp.openr66.dao.database.DBRuleDAO.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.GetRulesParams.*;
import static org.waarp.openr66.protocol.http.restv2.converters.RuleConverter.*;
import static org.waarp.openr66.protocol.http.restv2.converters.RuleConverter.Order.*;
import static org.waarp.openr66.protocol.http.restv2.errors.RestErrors.*;
import static org.waarp.openr66.protocol.http.restv2.utils.JsonUtils.*;

/**
 * This is the {@link AbstractRestDbHandler} handling all operations on the
 * host's transfer rule database.
 */
@Path(RULES_HANDLER_URI)
public class RulesHandler extends AbstractRestDbHandler {

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

  public RulesHandler(final byte crud) {
    super(crud);
  }

  /**
   * Method called to obtain a list of transfer rules based on the filters in
   * the query parameters.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   * @param limitStr the maximum number of entries allowed in the
   *     response
   * @param offsetStr the index of the first accepted entry in the
   *     list of all valid answers
   * @param orderStr the criteria used to sort the entries and the
   *     way of ordering them
   * @param modeTransStr only keep rules that use this transfer mode
   */
  @GET
  @Consumes(APPLICATION_FORM_URLENCODED)
  @RequiredRole(ROLE.READONLY)
  public void filterRules(final HttpRequest request,
                          final HttpResponder responder,
                          @QueryParam(LIMIT) @DefaultValue("20")
                          final String limitStr,
                          @QueryParam(OFFSET) @DefaultValue("0")
                          final String offsetStr,
                          @QueryParam(ORDER) @DefaultValue("ascName")
                          final String orderStr,
                          @QueryParam(MODE_TRANS) @DefaultValue("")
                          final String modeTransStr,
                          @QueryParam(COUNT_ORDER) @DefaultValue("")
                          final String countOrder) {
    checkSanity(limitStr, offsetStr, orderStr, modeTransStr, countOrder);
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
    Order order = ascName;
    ModeTrans modeTrans = null;
    try {
      limit = Integer.parseInt(limitStr);
      order = Order.valueOf(orderStr);
    } catch (final NumberFormatException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(LIMIT, limitStr));
    } catch (final IllegalArgumentException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(ORDER, orderStr));
    }
    try {
      offset = Integer.parseInt(offsetStr);
      if (!modeTransStr.isEmpty()) {
        modeTrans = ModeTrans.valueOf(modeTransStr);
      }
    } catch (final NumberFormatException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(OFFSET, offsetStr));
    } catch (final IllegalArgumentException e) {
      errors.add(ILLEGAL_PARAMETER_VALUE(MODE_TRANS, e.getMessage()));
    }

    if (limit <= 0) {
      errors.add(ILLEGAL_PARAMETER_VALUE(LIMIT, limitStr));
    } else if (offset < 0) {
      errors.add(ILLEGAL_PARAMETER_VALUE(OFFSET, offsetStr));
    }

    if (!errors.isEmpty()) {
      throw new RestErrorException(errors);
    }
    final List<Filter> filters = new ArrayList<Filter>();
    if (modeTrans != null) {
      filters.add(new Filter(MODE_TRANS_FIELD, "=",
                             Integer.toString(modeTrans.ordinal())));
    }

    final ObjectNode responseObject = JsonHandler.createObjectNode();
    RuleDAO ruleDAO = null;
    if (count) {
      long nbCount = -1;
      try {
        ruleDAO = DAO_FACTORY.getRuleDAO();
        nbCount = ruleDAO.count(filters);
      } catch (final DAOConnectionException e) {
        throw new InternalServerErrorException(e);
      } finally {
        DAOFactory.closeDAO(ruleDAO);
      }
      responseObject.put("totalResults", nbCount);
    } else {
      List<Rule> rules;
      try {
        ruleDAO = DAO_FACTORY.getRuleDAO();
        rules = ruleDAO.find(filters);
      } catch (final DAOConnectionException e) {
        throw new InternalServerErrorException(e);
      } finally {
        DAOFactory.closeDAO(ruleDAO);
      }
      final int totalResults = rules.size();
      Collections.sort(rules, order.comparator);

      final ArrayNode results = JsonHandler.createArrayNode();
      for (int i = offset; i < offset + limit && i < rules.size(); i++) {
        results.add(ruleToNode(rules.get(i)));
      }
      responseObject.put("totalResults", totalResults);
      responseObject.set("results", results);
    }
    final String responseText = nodeToString(responseObject);
    responder.sendJson(OK, responseText);
  }

  /**
   * Method called to add a new transfer rule in the server database. The
   * reply will contain the created entry
   * in JSON format, unless an unexpected error prevents it or if the request
   * is invalid.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request.
   */
  @POST
  @Consumes(APPLICATION_JSON)
  @RequiredRole(ROLE.RULE)
  public void addRule(final HttpRequest request,
                      final HttpResponder responder) {
    final ObjectNode requestObject = deserializeRequest(request);
    checkSanity(requestObject);
    final Rule rule = nodeToNewRule(requestObject);

    RuleDAO ruleDAO = null;
    try {
      ruleDAO = DAO_FACTORY.getRuleDAO();

      if (ruleDAO.exist(rule.getName())) {
        throw new RestErrorException(ALREADY_EXISTING(rule.getName()));
      }

      ruleDAO.insert(rule);
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } finally {
      DAOFactory.closeDAO(ruleDAO);
    }

    final ObjectNode responseObject = ruleToNode(rule);
    final String responseText = nodeToString(responseObject);

    final DefaultHttpHeaders headers = new DefaultHttpHeaders();
    headers.add(CONTENT_TYPE, APPLICATION_JSON);
    headers.add("ruleURI", RULES_HANDLER_URI + rule.getName());

    responder.sendString(CREATED, responseText, headers);
  }

  /**
   * Method called to get a list of all allowed HTTP methods on this entry
   * point. The HTTP methods are sent as
   * an array in the reply's headers.
   *
   * @param request the HttpRequest made on the resource
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