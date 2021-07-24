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

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cdap.http.HttpResponder;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.RuleDAO;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.pojo.Rule;
import org.waarp.openr66.protocol.http.restv2.converters.RuleConverter;
import org.waarp.openr66.protocol.http.restv2.utils.JsonUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static javax.ws.rs.core.HttpHeaders.*;
import static javax.ws.rs.core.MediaType.*;
import static org.waarp.common.role.RoleDefault.ROLE.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;

/**
 * This is the {@link AbstractRestDbHandler} handling all requests made on a
 * single rule REST entry point.
 */
@Path(RULE_ID_HANDLER_URI)
public class RuleIdHandler extends AbstractRestDbHandler {

  /**
   * The content of the 'Allow' header sent when an 'OPTIONS' request is made
   * on the handler.
   */
  private static final HttpHeaders OPTIONS_HEADERS;

  static {
    OPTIONS_HEADERS = new DefaultHttpHeaders();
    final List<HttpMethod> allow = new ArrayList<HttpMethod>();
    allow.add(HttpMethod.GET);
    allow.add(HttpMethod.PUT);
    allow.add(HttpMethod.DELETE);
    allow.add(HttpMethod.OPTIONS);
    OPTIONS_HEADERS.add(ALLOW, allow);
  }

  /**
   * Initializes the handler with the given CRUD mask.
   *
   * @param crud the CRUD mask for this handler
   */
  public RuleIdHandler(final byte crud) {
    super(crud);
  }

  /**
   * Method called to retrieve a transfer rule with the id given in the
   * request URI.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   * @param id the requested rule's name
   */
  @GET
  @Consumes(WILDCARD)
  @RequiredRole(NOACCESS)
  public final void getRule(final HttpRequest request,
                            final HttpResponder responder,
                            @PathParam(URI_ID) final String id) {
    checkSanity(id);
    RuleDAO ruleDAO = null;
    try {
      ruleDAO = DAO_FACTORY.getRuleDAO(true);
      final Rule rule = ruleDAO.select(id);

      if (rule == null) {
        responder.sendStatus(NOT_FOUND);
        return;
      }
      final ObjectNode responseObject = RuleConverter.ruleToNode(rule);
      final String responseText = JsonUtils.nodeToString(responseObject);
      responder.sendJson(OK, responseText);
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } catch (final DAONoDataException e) {
      responder.sendStatus(NOT_FOUND);
    } finally {
      DAOFactory.closeDAO(ruleDAO);
    }
  }

  /**
   * Method called to update a transfer rule.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   * @param id the requested rule's name
   */
  @PUT
  @Consumes(APPLICATION_JSON)
  @RequiredRole(RULE)
  public final void updateRule(final HttpRequest request,
                               final HttpResponder responder,
                               @PathParam(URI_ID) final String id) {
    checkSanity(id);
    RuleDAO ruleDAO = null;
    try {
      ruleDAO = DAO_FACTORY.getRuleDAO(false);
      final Rule oldRule = ruleDAO.select(id);

      if (oldRule == null) {
        responder.sendStatus(NOT_FOUND);
        return;
      }

      final ObjectNode requestObject = JsonUtils.deserializeRequest(request);
      checkSanity(requestObject);
      final Rule newRule =
          RuleConverter.nodeToUpdatedRule(requestObject, oldRule);

      ruleDAO.update(newRule);

      final ObjectNode responseObject = RuleConverter.ruleToNode(newRule);
      final String responseText = JsonUtils.nodeToString(responseObject);
      responder.sendJson(CREATED, responseText);
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } catch (final DAONoDataException e) {
      responder.sendStatus(NOT_FOUND);
    } finally {
      DAOFactory.closeDAO(ruleDAO);
    }
  }

  /**
   * Method called to delete a transfer rule from the database.
   * Note that if a Transfer exists that used this rule, the delete
   * cannot be achieved.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   * @param id the requested rule's name
   */
  @DELETE
  @Consumes(WILDCARD)
  @RequiredRole(RULE)
  public final void deleteRule(final HttpRequest request,
                               final HttpResponder responder,
                               @PathParam(URI_ID) final String id) {
    checkSanity(id);
    TransferDAO transferDAO = null;
    try {
      transferDAO = DAO_FACTORY.getTransferDAO();
      final List<Filter> filters = new ArrayList<Filter>();
      final Filter filter =
          new Filter(DbTaskRunner.Columns.IDRULE.name(), "=", id);
      filters.add(filter);
      final long nb = transferDAO.count(filters);
      if (nb > 0) {
        responder.sendStatus(NOT_FOUND);
      }
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } finally {
      DAOFactory.closeDAO(transferDAO);
    }
    RuleDAO ruleDAO = null;
    try {
      ruleDAO = DAO_FACTORY.getRuleDAO(false);
      final Rule rule = ruleDAO.select(id);
      if (rule == null) {
        responder.sendStatus(NOT_FOUND);
      } else {
        ruleDAO.delete(rule);
        responder.sendStatus(NO_CONTENT);
      }
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } catch (final DAONoDataException e) {
      responder.sendStatus(NOT_FOUND);
    } finally {
      DAOFactory.closeDAO(ruleDAO);
    }
  }

  /**
   * Method called to get a list of all allowed HTTP methods on this entry
   * point. The HTTP methods are sent as
   * an array in the reply's headers.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   * @param id the requested rule's name
   */
  @OPTIONS
  @Consumes(WILDCARD)
  @RequiredRole(NOACCESS)
  public final void options(final HttpRequest request,
                            final HttpResponder responder,
                            @PathParam(URI_ID) final String id) {
    checkSanity(id);
    responder.sendStatus(OK, OPTIONS_HEADERS);
  }
}