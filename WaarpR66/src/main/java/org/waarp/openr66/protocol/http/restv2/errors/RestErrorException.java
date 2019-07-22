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

package org.waarp.openr66.protocol.http.restv2.errors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Thrown to indicate that the request made to the server is invalid, and lists
 * all the errors found as a list
 * of {@link RestError} objects. Typically, these errors will be sent back as a
 * '400 - Bad Request' HTTP
 * response.
 */
public class RestErrorException extends RuntimeException {

  /**
   *
   */
  private static final long serialVersionUID = 6675092325812345581L;
  /**
   * The list of all {@link RestError} errors found in the request.
   */
  public final List<RestError> errors;

  /**
   * Initializes an exception with a single error.
   *
   * @param error The error to add.
   */
  public RestErrorException(RestError error) {
    errors = new ArrayList<RestError>();
    errors.add(error);
  }

  /**
   * Initializes an exception with a list of errors.
   *
   * @param errors The errors to add.
   */
  public RestErrorException(List<RestError> errors) {
    this.errors = errors;
  }

  /**
   * Returns the exception's list of Error as an {@link ArrayNode} contained
   * in an {@link ObjectNode}.
   *
   * @param lang the language of the error messages.
   *
   * @return the serialized list of errors.
   */
  public ObjectNode makeNode(Locale lang) {
    final ArrayNode errorsArray = new ArrayNode(JsonNodeFactory.instance);
    for (final RestError error : errors) {
      errorsArray.add(error.makeNode(lang));
    }
    final ObjectNode response = new ObjectNode(JsonNodeFactory.instance);
    response.putArray("errors").addAll(errorsArray);

    return response;
  }
}