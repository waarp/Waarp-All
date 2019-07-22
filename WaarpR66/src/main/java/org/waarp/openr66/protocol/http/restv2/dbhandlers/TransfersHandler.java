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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cdap.http.HttpResponder;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.joda.time.DateTime;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.protocol.http.restv2.converters.TransferConverter;
import org.waarp.openr66.protocol.http.restv2.errors.RestError;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;
import org.waarp.openr66.protocol.http.restv2.utils.JsonUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.MediaType.*;
import static org.waarp.openr66.dao.database.DBTransferDAO.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.DAO_FACTORY;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.GetTransfersParams.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.TRANSFERS_HANDLER_URI;
import static org.waarp.openr66.protocol.http.restv2.errors.RestErrors.ILLEGAL_PARAMETER_VALUE;

/**
 * This is the {@link AbstractRestDbHandler} handling all requests made on
 * the transfer collection REST entry point.
 */
@Path(TRANSFERS_HANDLER_URI)
public class TransfersHandler extends AbstractRestDbHandler {

    /**
     * The content of the 'Allow' header sent when an 'OPTIONS' request is made
     * on the handler.
     */
    private static final io.netty.handler.codec.http.HttpHeaders OPTIONS_HEADERS;

    static {
        OPTIONS_HEADERS = new DefaultHttpHeaders();
        List<HttpMethod> allow = new ArrayList<HttpMethod>();
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
    public TransfersHandler(byte crud) {
        super(crud);
    }

    /**
     * Method called to obtain a list of transfer entry matching the different
     * filters given as parameters of the query. The response is sent as a JSON
     * array containing all the requested entries, unless an unexpected error
     * prevents it or if the request is invalid.
     *
     * @param request    the HttpRequest made on the resource
     * @param responder  the HttpResponder which sends the reply to the request
     * @param limit_str  maximum number of entries allowed in the response
     * @param offset_str index of the first accepted entry in the list of all valid answers
     * @param order_str  the criteria used to sort the entries and the way of ordering
     * @param ruleID     filter transfers that use this rule
     * @param partner    filter transfers that have this partner
     * @param status_str filter transfers currently in one of these statuses
     * @param filename   filter transfers of a particular file
     * @param startTrans lower bound for the transfers' starting date
     * @param stopTrans  upper bound for the transfers' starting date
     */
    @GET
    @Consumes(APPLICATION_FORM_URLENCODED)
    @RequiredRole(ROLE.READONLY)
    public void filterTransfer(HttpRequest request, HttpResponder responder,
                               @QueryParam(LIMIT) @DefaultValue("20")
                                           String limit_str,
                               @QueryParam(OFFSET) @DefaultValue("0")
                                           String offset_str,
                               @QueryParam(ORDER) @DefaultValue("ascId")
                                           String order_str,
                               @QueryParam(RULE_ID) @DefaultValue("")
                                           String ruleID,
                               @QueryParam(PARTNER) @DefaultValue("")
                                           String partner,
                               @QueryParam(STATUS) @DefaultValue("")
                                           String status_str,
                               @QueryParam(FILENAME) @DefaultValue("")
                                           String filename,
                               @QueryParam(START_TRANS) @DefaultValue("")
                                           String startTrans,
                               @QueryParam(STOP_TRANS) @DefaultValue("")
                                           String stopTrans) {

        ArrayList<RestError> errors = new ArrayList<RestError>();

        int limit = 20;
        try {
            limit = Integer.parseInt(limit_str);
        } catch (NumberFormatException e) {
            errors.add(ILLEGAL_PARAMETER_VALUE(LIMIT, limit_str));
        }
        int offset = 0;
        try {
            offset = Integer.parseInt(offset_str);
        } catch (NumberFormatException e) {
            errors.add(ILLEGAL_PARAMETER_VALUE(OFFSET, offset_str));
        }
        TransferConverter.Order order = TransferConverter.Order.ascId;
        try {
            order = TransferConverter.Order.valueOf(order_str);
        } catch (IllegalArgumentException e) {
            errors.add(ILLEGAL_PARAMETER_VALUE(ORDER, order_str));
        }

        List<Filter> filters = new ArrayList<Filter>();
        if (!startTrans.isEmpty()) {
            try {
                DateTime start = DateTime.parse(startTrans);
                filters.add(new Filter(TRANSFER_START_FIELD, ">=", start.getMillis()));
            } catch (IllegalArgumentException e) {
                errors.add(ILLEGAL_PARAMETER_VALUE(START_TRANS, startTrans));
            }
        }
        if (!stopTrans.isEmpty()) {
            try {
                DateTime stop = DateTime.parse(stopTrans);
                filters.add(new Filter(TRANSFER_START_FIELD, "<=", stop.getMillis()));
            } catch (IllegalArgumentException e) {
                errors.add(ILLEGAL_PARAMETER_VALUE(STOP_TRANS, stopTrans));
            }
        }
        if (!ruleID.isEmpty()) {
            filters.add(new Filter(ID_RULE_FIELD, "=", ruleID));
        }
        if (!partner.isEmpty()) {
            filters.add(new Filter(REQUESTED_FIELD, "=", partner));
        }
        if (!filename.isEmpty()) {
            filters.add(new Filter(FILENAME_FIELD, "=", filename));
        }
        if (!status_str.isEmpty()) {
            try {
                int status_nbr = AbstractDbData.UpdatedInfo.valueOf(status_str).ordinal();
                filters.add(new Filter(UPDATED_INFO_FIELD, "=", status_nbr));
            } catch (IllegalArgumentException e) {
                errors.add(ILLEGAL_PARAMETER_VALUE(STATUS, status_str));
            }
        }

        if(!errors.isEmpty()) {
            throw new RestErrorException(errors);
        }

        TransferDAO transferDAO = null;
        List<Transfer> transferList;
        try {
            transferDAO = DAO_FACTORY.getTransferDAO();
            transferList = transferDAO.find(filters, order.column, order.ascend,
                    limit, offset);
        } catch (DAOConnectionException e) {
            throw new InternalServerErrorException(e);
        } finally {
            if (transferDAO != null) {
                transferDAO.close();
            }
        }

        ObjectNode responseObject = new ObjectNode(JsonNodeFactory.instance);
        ArrayNode resultList = responseObject.putArray("results");
        for (Transfer transfer : transferList) {
            resultList.add(TransferConverter.transferToNode(transfer));
        }
        responseObject.put("totalResults", transferList.size());
        String responseText = JsonUtils.nodeToString(responseObject);
        responder.sendJson(OK, responseText);
    }

    /**
     * Method called to create a new transfer on the server. The reply will
     * contain the created entry in JSON format, unless an unexpected error
     * prevents it or if the request is invalid.
     *
     * @param request   the HttpRequest made on the resource
     * @param responder the HttpResponder which sends the reply to the request
     */
    @POST
    @Consumes(APPLICATION_JSON)
    @RequiredRole(ROLE.TRANSFER)
    public void createTransfer(HttpRequest request, HttpResponder responder) {

        ObjectNode requestObject = JsonUtils.deserializeRequest(request);
        Transfer transfer = TransferConverter.nodeToNewTransfer(requestObject);

        TransferDAO transferDAO = null;
        try {
            transferDAO = DAO_FACTORY.getTransferDAO();
            transferDAO.insert(transfer);
        } catch (DAOConnectionException e) {
            throw new InternalServerErrorException(e);
        } finally {
            if (transferDAO != null) {
                transferDAO.close();
            }
        }

        ObjectNode responseObject = TransferConverter.transferToNode(transfer);
        String responseText = JsonUtils.nodeToString(responseObject);
        DefaultHttpHeaders headers = new DefaultHttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON);
        headers.add("transfer-uri", TRANSFERS_HANDLER_URI + transfer.getId() +
                "_" + transfer.getRequested());

        responder.sendString(CREATED, responseText, headers);
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
    @RequiredRole(ROLE.NOACCESS)
    public void options(HttpRequest request, HttpResponder responder) {
        responder.sendStatus(OK, OPTIONS_HEADERS);
    }
}

