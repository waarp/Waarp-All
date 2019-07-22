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

package org.waarp.openr66.protocol.http.restv2.resthandlers;

import io.cdap.http.ExceptionHandler;
import io.cdap.http.HttpResponder;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;
import org.waarp.openr66.protocol.http.restv2.utils.JsonUtils;
import org.waarp.openr66.protocol.http.restv2.utils.RestUtils;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotSupportedException;
import java.util.Locale;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static javax.ws.rs.core.HttpHeaders.*;

/**
 * Handles all exceptions thrown by handlers during the processing of an HTTP
 * request.
 */
public class RestExceptionHandler extends ExceptionHandler {

  /**
   * The logger for all events.
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(RestExceptionHandler.class);

  /**
   * Method called when an exception is thrown during the processing of a
   * request.
   *
   * @param t the exception thrown during execution
   * @param request the HttpRequest that failed
   * @param responder the HttpResponder for the request
   */
  @Override
  public void handle(Throwable t, HttpRequest request,
                     HttpResponder responder) {
    if (t instanceof RestErrorException) {
      final RestErrorException userErrors = (RestErrorException) t;
      try {
        final Locale lang = RestUtils.getLocale(request);
        final String errorText =
            JsonUtils.nodeToString(userErrors.makeNode(lang));
        responder.sendJson(BAD_REQUEST, errorText);
      } catch (final InternalServerErrorException e) {
        logger.error(e);
        responder.sendStatus(INTERNAL_SERVER_ERROR);
      }
    } else if (t instanceof NotSupportedException) {
      final DefaultHttpHeaders headers = new DefaultHttpHeaders();
      headers.add(ACCEPT, t.getMessage());
      responder.sendStatus(UNSUPPORTED_MEDIA_TYPE, headers);
    } else {
      logger.error(t);
      responder.sendStatus(INTERNAL_SERVER_ERROR);
    }
  }
}
