/*
 *  This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright 2009, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 *  individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at your
 *  option) any later version.
 *
 *  Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 *  A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  Waarp . If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.waarp.openr66.protocol.http.restv2.dbhandlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cdap.http.HttpResponder;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.waarp.openr66.dao.LimitDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Limit;
import org.waarp.openr66.protocol.http.restv2.converters.LimitsConverter;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;
import org.waarp.openr66.protocol.http.restv2.utils.JsonUtils;

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
import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static org.waarp.common.role.RoleDefault.ROLE.*;
import static org.waarp.openr66.protocol.configuration.Configuration.configuration;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;
import static org.waarp.openr66.protocol.http.restv2.errors.RestErrors.ALREADY_EXISTING;

/**
 * This is the {@link AbstractRestDbHandler} handling all requests made on
 * the bandwidth limits REST entry point.
 */
@Path(LIMITS_HANDLER_URI)
public class LimitsHandler extends AbstractRestDbHandler {

    /**
     * The content of the 'Allow' header sent when an 'OPTIONS' request is made
     * on the handler.
     */
    private static final HttpHeaders OPTIONS_HEADERS;

    static {
        OPTIONS_HEADERS = new DefaultHttpHeaders();
        List<HttpMethod> allow = new ArrayList<HttpMethod>();
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
    public LimitsHandler(byte crud) {
        super(crud);
    }

    /**
     * Method called to obtain a description of the host's current bandwidth
     * limits.
     *
     * @param request   the HttpRequest made on the resource
     * @param responder the HttpResponder which sends the reply to the request
     */
    @GET
    @Consumes(WILDCARD)
    @RequiredRole(LIMIT)
    public void getLimits(HttpRequest request, HttpResponder responder) {

        LimitDAO limitDAO = null;
        try {
            limitDAO = DAO_FACTORY.getLimitDAO();
            if (limitDAO.exist(SERVER_NAME)) {
                Limit limits = limitDAO.select(SERVER_NAME);
                ObjectNode responseObject = LimitsConverter.limitToNode(limits);
                String responseText = JsonUtils.nodeToString(responseObject);

                responder.sendJson(OK, responseText);
            } else {
                responder.sendStatus(NOT_FOUND);
            }
        } catch (DAOConnectionException e) {
            throw new InternalServerErrorException(e);
        } catch (DAONoDataException e) {
            responder.sendStatus(NOT_FOUND);
        } finally {
            if (limitDAO != null) {
                limitDAO.close();
            }
        }
    }

    /**
     * Method called to initiate the entry for this host in the bandwidth limits
     * database. If the host already has limits set in its configuration,
     * they will be replaced by these new ones.
     *
     * @param request   the HttpRequest made on the resource
     * @param responder the HttpResponder which sends the reply to the request
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @RequiredRole(READONLY)
    public void initializeLimits(HttpRequest request, HttpResponder responder) {

        LimitDAO limitDAO = null;
        try {
            limitDAO = DAO_FACTORY.getLimitDAO();

            if (!limitDAO.exist(SERVER_NAME)) {
                ObjectNode requestObject = JsonUtils.deserializeRequest(request);
                Limit limits = LimitsConverter.nodeToNewLimit(requestObject);
                limitDAO.insert(limits);

                configuration.changeNetworkLimit(limits.getReadGlobalLimit(),
                        limits.getWriteGlobalLimit(), limits.getReadSessionLimit(),
                        limits.getWriteSessionLimit(), limits.getDelayLimit());

                ObjectNode responseObject = LimitsConverter.limitToNode(limits);
                String responseText = JsonUtils.nodeToString(responseObject);
                responder.sendJson(CREATED, responseText);
            } else {
                throw new RestErrorException(ALREADY_EXISTING(SERVER_NAME));
            }
        } catch (DAOConnectionException e) {
            throw new InternalServerErrorException(e);
        } finally {
            if (limitDAO != null) {
                limitDAO.close();
            }
        }
    }

    /**
     * Method called to update this host's bandwidth limits in the database
     * and configuration.
     *
     * @param request   the HttpRequest made on the resource
     * @param responder the HttpResponder which sends the reply to the request
     */
    @PUT
    @Consumes(APPLICATION_JSON)
    @RequiredRole(LIMIT)
    public void updateLimits(HttpRequest request, HttpResponder responder) {

        LimitDAO limitDAO = null;
        try {
            limitDAO = DAO_FACTORY.getLimitDAO();

            if (!limitDAO.exist(SERVER_NAME)) {
                responder.sendStatus(NOT_FOUND);
                return;
            }

            ObjectNode requestObject = JsonUtils.deserializeRequest(request);

            Limit oldLimits = limitDAO.select(SERVER_NAME);
            Limit newLimits = LimitsConverter.nodeToUpdatedLimit(requestObject, oldLimits);

            limitDAO.update(newLimits);

            configuration.changeNetworkLimit(newLimits.getReadGlobalLimit(),
                    newLimits.getWriteGlobalLimit(), newLimits.getReadSessionLimit(),
                    newLimits.getWriteSessionLimit(), newLimits.getDelayLimit());

            ObjectNode responseObject = LimitsConverter.limitToNode(newLimits);
            String responseText = JsonUtils.nodeToString(responseObject);
            responder.sendJson(CREATED, responseText);

        } catch (DAOConnectionException e) {
            throw new InternalServerErrorException(e);
        } catch (DAONoDataException e) {
            responder.sendStatus(NOT_FOUND);
        } finally {
            if (limitDAO != null) {
                limitDAO.close();
            }
        }
    }

    /**
     * Method called to remove any bandwidth limits in place on this host.
     * Also removes any limits set in the configuration.
     *
     * @param request   the HttpRequest made on the resource
     * @param responder the HttpResponder which sends the reply to the request
     */
    @DELETE
    @Consumes(WILDCARD)
    @RequiredRole(LIMIT)
    public void deleteLimits(HttpRequest request, HttpResponder responder) {
        LimitDAO limitDAO = null;
        try {
            limitDAO = DAO_FACTORY.getLimitDAO();

            if (limitDAO.exist(SERVER_NAME)) {
                limitDAO.delete(limitDAO.select(SERVER_NAME));
                configuration.changeNetworkLimit(0, 0, 0, 0, 0);
                responder.sendStatus(NO_CONTENT);
            } else {
                responder.sendStatus(NOT_FOUND);
            }
        } catch (DAOConnectionException e) {
            throw new InternalServerErrorException(e);
        } catch (DAONoDataException e) {
            responder.sendStatus(NOT_FOUND);
        } finally {
            if (limitDAO != null) {
                limitDAO.close();
            }
        }
    }

    /**
     * Method called to get a list of all allowed HTTP methods on this entry
     * point. The HTTP methods are sent as an array in the reply's headers.
     *
     * @param request   the HttpRequest made on the resource
     * @param responder the HttpResponder which sends the reply to the request
     */
    @OPTIONS
    @Consumes(WILDCARD)
    @RequiredRole(NOACCESS)
    public void options(HttpRequest request, HttpResponder responder) {
        responder.sendStatus(OK, OPTIONS_HEADERS);
    }
}
