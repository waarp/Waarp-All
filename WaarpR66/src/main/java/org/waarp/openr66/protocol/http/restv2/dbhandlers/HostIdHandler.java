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
import org.waarp.openr66.dao.HostDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Host;
import org.waarp.openr66.protocol.http.restv2.converters.HostConverter;
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
import static org.waarp.common.role.RoleDefault.ROLE.HOST;
import static org.waarp.common.role.RoleDefault.ROLE.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;

/**
 * This is the {@link AbstractRestDbHandler} handling all requests made on the
 * single host REST entry point.
 */
@Path(HOST_ID_HANDLER_URI)
public class HostIdHandler extends AbstractRestDbHandler {

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
  public HostIdHandler(byte crud) {
    super(crud);
  }

  /**
   * Method called to retrieve a host entry from the database with the id in
   * the request URI.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   * @param id the requested host's name
   */
  @GET
  @Consumes(WILDCARD)
  @RequiredRole(READONLY)
  public void getHost(HttpRequest request, HttpResponder responder,
                      @PathParam(URI_ID) String id) {

    HostDAO hostDAO = null;
    try {
      hostDAO = DAO_FACTORY.getHostDAO();
      final Host host = hostDAO.select(id);
      if (host == null) {
        responder.sendStatus(NOT_FOUND);
        return;
      }
      final ObjectNode responseObject = HostConverter.hostToNode(host);
      final String responseText = JsonUtils.nodeToString(responseObject);
      responder.sendJson(OK, responseText);
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } catch (final DAONoDataException e) {
      responder.sendStatus(NOT_FOUND);
    } finally {
      DAOFactory.closeDAO(hostDAO);
    }
  }

  /**
   * Method called to update the host entry with the given id. The entry is
   * replaced by the one in the request's
   * body.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   * @param id the requested host's name
   */
  @PUT
  @Consumes(APPLICATION_JSON)
  @RequiredRole(HOST)
  public void updateHost(HttpRequest request, HttpResponder responder,
                         @PathParam(URI_ID) String id) {

    HostDAO hostDAO = null;
    try {
      hostDAO = DAO_FACTORY.getHostDAO();
      final Host oldHost = hostDAO.select(id);

      if (oldHost == null) {
        responder.sendStatus(NOT_FOUND);
        return;
      }

      final ObjectNode requestObject = JsonUtils.deserializeRequest(request);
      final Host newHost =
          HostConverter.nodeToUpdatedHost(requestObject, oldHost);

      hostDAO.update(newHost);

      final ObjectNode responseObject = HostConverter.hostToNode(newHost);
      final String responseText = JsonUtils.nodeToString(responseObject);
      responder.sendJson(CREATED, responseText);

    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } catch (final DAONoDataException e) {
      responder.sendStatus(NOT_FOUND);
    } finally {
      DAOFactory.closeDAO(hostDAO);
    }
  }

  /**
   * Method called to delete a host entry from the database.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the
   *     request
   * @param id the requested host's name
   */
  @DELETE
  @Consumes(WILDCARD)
  @RequiredRole(HOST)
  public void deleteHost(HttpRequest request, HttpResponder responder,
                         @PathParam(URI_ID) String id) {

    HostDAO hostDAO = null;
    try {
      hostDAO = DAO_FACTORY.getHostDAO();
      final Host host = hostDAO.select(id);
      if (host == null) {
        responder.sendStatus(NOT_FOUND);
      } else {
        hostDAO.delete(host);
        responder.sendStatus(NO_CONTENT);
      }
    } catch (final DAOConnectionException e) {
      throw new InternalServerErrorException(e);
    } catch (final DAONoDataException e) {
      responder.sendStatus(NOT_FOUND);
    } finally {
      DAOFactory.closeDAO(hostDAO);
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
   * @param id the requested host's name
   */
  @OPTIONS
  @Consumes(WILDCARD)
  @RequiredRole(NOACCESS)
  public void options(HttpRequest request, HttpResponder responder,
                      @PathParam(URI_ID) String id) {
    responder.sendStatus(OK, OPTIONS_HEADERS);
  }
}
