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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.cdap.http.AbstractHttpHandler;
import io.cdap.http.HttpResponder;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.openr66.context.task.SpooledInformTask;
import org.waarp.openr66.protocol.http.restv2.errors.RestError;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;
import org.waarp.openr66.protocol.http.restv2.utils.JsonUtils;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static javax.ws.rs.core.HttpHeaders.*;
import static javax.ws.rs.core.MediaType.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.GetTransfersParams.*;
import static org.waarp.openr66.protocol.http.restv2.errors.RestErrors.*;

/**
 * This is the {@link AbstractHttpHandler} handling all requests made on the
 * FileMonitor collection REST entry point.
 */
@Path(FILE_MONITOR_HANDLER_URI)
public class SpooledHandler extends AbstractRestDbHandler {

  /**
   * The content of the 'Allow' header sent when an 'OPTIONS' request is made
   * on the handler.
   */
  private static final io.netty.handler.codec.http.HttpHeaders OPTIONS_HEADERS;

  static {
    OPTIONS_HEADERS = new DefaultHttpHeaders();
    final List<HttpMethod> allow = new ArrayList<HttpMethod>();
    allow.add(HttpMethod.GET);
    allow.add(HttpMethod.OPTIONS);
    OPTIONS_HEADERS.add(ALLOW, allow);
  }

  /**
   * Initializes the handler with the given CRUD mask.
   *
   * @param crud the CRUD mask for this handler
   */
  public SpooledHandler(final byte crud) {
    super(crud);
  }

  /**
   * Method called to obtain a list of FileMonitors entry matching the different
   * filters given as parameters of the
   * query. The response is sent as a JSON array containing all the requested
   * entries, unless an unexpected
   * error prevents it or if the request is invalid.
   *
   * @param request the HttpRequest made on the resource
   * @param responder the HttpResponder which sends the reply to the request
   * @param nameStr the name of the FileMonitor or empty for all
   * @param statusStr 0 or empty for all, 1 for active ones, 0 for inactive
   *     ones
   */
  @GET
  @Consumes(APPLICATION_FORM_URLENCODED)
  @RequiredRole(ROLE.READONLY)
  public void filterTransfer(final HttpRequest request,
                             final HttpResponder responder,
                             @QueryParam("name") @DefaultValue("")
                             final String nameStr,
                             @QueryParam(STATUS) @DefaultValue("")
                             final String statusStr) {

    final ArrayList<RestError> errors = new ArrayList<RestError>();
    final String argName;
    if (nameStr.trim().isEmpty()) {
      argName = null;
    } else {
      argName = nameStr.trim();
    }
    int argStatus;
    if (statusStr.trim().isEmpty()) {
      argStatus = 0;
    } else {
      try {
        argStatus = Integer.parseInt(statusStr.trim());
      } catch (final NumberFormatException ignored) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
        errors.add(ILLEGAL_PARAMETER_VALUE(STATUS, statusStr));
        argStatus = 0;
      }
    }
    if (!errors.isEmpty()) {
      throw new RestErrorException(errors);
    }

    final ArrayNode arrayNode = JsonHandler.createArrayNode();
    final int nbFiles =
        SpooledInformTask.buildSpooledJson(arrayNode, argStatus, argName);
    final ObjectNode responseObject = new ObjectNode(JsonNodeFactory.instance);
    final ArrayNode resultList = responseObject.putArray("results");
    resultList.addAll(arrayNode);
    responseObject.put("totalResults", arrayNode.size());
    responseObject.put("totalSubResults", nbFiles);
    final String responseText = JsonUtils.nodeToString(responseObject);
    responder.sendJson(OK, responseText);
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
  @RequiredRole(ROLE.NOACCESS)
  public void options(final HttpRequest request,
                      final HttpResponder responder) {
    responder.sendStatus(OK, OPTIONS_HEADERS);
  }
}
