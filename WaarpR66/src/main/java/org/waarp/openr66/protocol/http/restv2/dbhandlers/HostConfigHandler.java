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
import org.waarp.openr66.dao.BusinessDAO;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Business;
import org.waarp.openr66.protocol.http.restv2.converters.HostConfigConverter;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;
import org.waarp.openr66.protocol.http.restv2.utils.JsonUtils;
import org.waarp.openr66.protocol.utils.Version;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static javax.ws.rs.core.HttpHeaders.*;
import static javax.ws.rs.core.MediaType.*;
import static org.waarp.common.role.RoleDefault.ROLE.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;
import static org.waarp.openr66.protocol.http.restv2.errors.RestErrors.*;

/**
 * This is the {@link AbstractRestDbHandler} handling all requests made on the
 * host configuration REST entry
 * point.
 */
@Path(CONFIG_HANDLER_URI)
public class HostConfigHandler extends AbstractRestDbHandler {

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
  public HostConfigHandler(final byte crud) {
    super(crud);
  }

  /**
   * Method called to retrieve a host's configuration entry in the database.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   */
  @GET
  @Consumes(WILDCARD)
  @RequiredRole(READONLY)
  public void getConfig(final HttpRequest request,
                        final HttpResponder responder) {
    BusinessDAO businessDAO = null;
    try {
      businessDAO = DAO_FACTORY.getBusinessDAO(true);
      final String host = serverName();
      final Business business = businessDAO.select(host);
      if (business != null) {
        final ObjectNode responseObject =
            HostConfigConverter.businessToNode(business);
        responseObject.put("versionR66", Version.ID);
        responseObject.put("versionBin",
                           org.waarp.common.utility.Version.fullIdentifier());
        final String responseText = JsonUtils.nodeToString(responseObject);
        responder.sendJson(OK, responseText);
      } else {
        responder.sendStatus(NOT_FOUND);
      }
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } catch (final DAONoDataException e) {
      responder.sendStatus(NOT_FOUND);
    } finally {
      DAOFactory.closeDAO(businessDAO);
    }
  }

  /**
   * Method called to initialize a host's configuration database entry if none
   * already exists.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   */
  @POST
  @Consumes(APPLICATION_FORM_URLENCODED)
  @RequiredRole(CONFIGADMIN)
  public void initializeConfig(final HttpRequest request,
                               final HttpResponder responder) {
    BusinessDAO businessDAO = null;
    try {
      businessDAO = DAO_FACTORY.getBusinessDAO(false);

      if (!businessDAO.exist(serverName())) {
        final ObjectNode requestObject = JsonUtils.deserializeRequest(request);
        checkSanity(requestObject);
        final Business config =
            HostConfigConverter.nodeToNewBusiness(requestObject);
        businessDAO.insert(config);

        final ObjectNode responseObject =
            HostConfigConverter.businessToNode(config);
        final String responseText = JsonUtils.nodeToString(responseObject);
        responder.sendJson(CREATED, responseText);
      } else {
        throw new RestErrorException(ALREADY_EXISTING(serverName()));
      }
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } finally {
      DAOFactory.closeDAO(businessDAO);
    }
  }

  /**
   * Method called to update a host's configuration in the database if it
   * exists.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   */
  @PUT
  @Consumes(APPLICATION_JSON)
  @RequiredRole(CONFIGADMIN)
  public void updateConfig(final HttpRequest request,
                           final HttpResponder responder) {
    BusinessDAO businessDAO = null;

    try {
      businessDAO = DAO_FACTORY.getBusinessDAO(false);

      if (!businessDAO.exist(serverName())) {
        responder.sendStatus(NOT_FOUND);
      }

      final ObjectNode requestObject = JsonUtils.deserializeRequest(request);
      checkSanity(requestObject);
      final Business oldConfig = businessDAO.select(serverName());
      final Business newConfig =
          HostConfigConverter.nodeToUpdatedBusiness(requestObject, oldConfig);
      businessDAO.update(newConfig);

      final ObjectNode responseObject =
          HostConfigConverter.businessToNode(newConfig);
      final String responseText = JsonUtils.nodeToString(responseObject);
      responder.sendJson(CREATED, responseText);
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } catch (final DAONoDataException e) {
      responder.sendStatus(NOT_FOUND);
    } finally {
      DAOFactory.closeDAO(businessDAO);
    }
  }

  /**
   * Method called to delete the current host's configuration entry in the database.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   */
  @DELETE
  @Consumes(WILDCARD)
  @RequiredRole(CONFIGADMIN)
  public void deleteConfig(final HttpRequest request,
                           final HttpResponder responder) {
    BusinessDAO businessDAO = null;
    try {
      businessDAO = DAO_FACTORY.getBusinessDAO(false);
      if (businessDAO.exist(serverName())) {
        businessDAO.delete(businessDAO.select(serverName()));
        responder.sendStatus(NO_CONTENT);
      } else {
        responder.sendStatus(NOT_FOUND);
      }
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } catch (final DAONoDataException e) {
      responder.sendStatus(NOT_FOUND);
    } finally {
      DAOFactory.closeDAO(businessDAO);
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
   */
  @OPTIONS
  @Consumes(WILDCARD)
  @RequiredRole(NOACCESS)
  public void options(final HttpRequest request,
                      final HttpResponder responder) {
    responder.sendStatus(OK, OPTIONS_HEADERS);
  }
}
