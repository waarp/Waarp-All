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

package org.waarp.openr66.protocol.http.restv2.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.openr66.protocol.http.restv2.errors.RestErrorException;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotSupportedException;
import java.io.IOException;

import static javax.ws.rs.core.HttpHeaders.*;
import static javax.ws.rs.core.MediaType.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;
import static org.waarp.openr66.protocol.http.restv2.errors.RestErrors.*;

/**
 * A series of utility methods for serializing and deserializing JSON.
 */
public final class JsonUtils {

  /**
   * Makes the default constructor of this utility class inaccessible.
   */
  private JsonUtils() throws InstantiationException {
    throw new InstantiationException(
        getClass().getName() + " cannot be instantiated.");
  }

  // ######################### PUBLIC METHODS #################################

  /**
   * Converts an ObjectNode into a String.
   *
   * @param object the ObjectNode to convert
   *
   * @return the JSON object as a String
   *
   * @throws InternalServerErrorException if an unexpected error
   *     occurred
   */
  public static String nodeToString(ObjectNode object) {
    try {
      final ObjectMapper mapper = new ObjectMapper();
      return mapper.writeValueAsString(object);
    } catch (final JsonProcessingException e) {
      throw new InternalServerErrorException(e);
    }
  }

  /**
   * Deserializes a request's content as an ObjectNode
   *
   * @param request the request to deserialize
   *
   * @return the deserialized JSON object
   *
   * @throws RestErrorException If the content is not a valid JSON
   *     object.
   * @throws NotSupportedException If the content type is not JSON.
   * @throws InternalServerErrorException if an unexpected error
   *     occurred
   */
  public static ObjectNode deserializeRequest(HttpRequest request) {
    if (!(request instanceof FullHttpRequest)) {
      throw new RestErrorException(MISSING_BODY());
    }

    try {
      final String body =
          ((FullHttpRequest) request).content().toString(UTF8_CHARSET);
      try {
        ParametersChecker.checkSanityString(body);
      } catch (InvalidArgumentException e) {
        throw new RestErrorException(MALFORMED_JSON(0, 0,
                                                    "The root JSON element contains invalid data"));
      }
      final ObjectMapper mapper = new ObjectMapper();
      mapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
      final JsonNode node = mapper.readTree(body);

      if (node.isObject()) {
        return (ObjectNode) node;
      } else {
        throw new RestErrorException(
            MALFORMED_JSON(0, 0, "The root JSON element is not an object"));
      }
    } catch (final JsonParseException e) {
      final String contentType = request.headers().get(CONTENT_TYPE);
      if (contentType == null || contentType.isEmpty()) {
        throw new NotSupportedException(APPLICATION_JSON);
      } else {
        throw new RestErrorException(MALFORMED_JSON(e.getLocation().getLineNr(),
                                                    e.getLocation()
                                                     .getColumnNr(),
                                                    e.getOriginalMessage()));
      }
    } catch (final JsonMappingException e) {
      final JsonParser parser = (JsonParser) e.getProcessor();
      try {
        final String field = parser.getCurrentName();
        if (field == null) {
          throw new RestErrorException(MISSING_BODY());
        } else {
          throw new RestErrorException(DUPLICATE_KEY(field));
        }
      } catch (final IOException ex) {
        throw new InternalServerErrorException(e);
      }
    } catch (final IOException e) {
      throw new InternalServerErrorException(e);
    }
  }
}
