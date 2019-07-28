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
package org.waarp.common.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import org.waarp.common.logging.SysErrLogger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON handler using Smile default format
 */
public class SmileJsonHandler extends JsonHandler {
  /**
   * JSON SMILE parser
   */
  public static final ObjectMapper mapper = new ObjectMapper(new SmileFactory())
      .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
      .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);

  protected SmileJsonHandler() {
  }

  /**
   * @return an empty ObjectNode
   */
  public static ObjectNode createObjectNode() {
    return mapper.createObjectNode();
  }

  /**
   * @return an empty ArrayNode
   */
  public static ArrayNode createArrayNode() {
    return mapper.createArrayNode();
  }

  /**
   * @param value
   *
   * @return the objectNode or null if an error occurs
   */
  public static ObjectNode getFromString(String value) {
    try {
      return (ObjectNode) mapper.readTree(value);
    } catch (final JsonProcessingException e) {
      return null;
    } catch (final IOException e) {
      return null;
    }
  }

  /**
   * @param object
   *
   * @return the Json representation of the object
   */
  public static String writeAsString(Object object) {
    try {
      return mapper.writeValueAsString(object);
    } catch (final JsonProcessingException e) {
      return "{}";
    }
  }

  /**
   * @param value
   *
   * @return the corresponding HashMap
   */
  public static Map<String, Object> getMapFromString(String value) {
    if (value != null && !value.isEmpty()) {
      Map<String, Object> info = null;
      try {
        info =
            mapper.readValue(value, new TypeReference<Map<String, Object>>() {
            });
      } catch (final JsonParseException ignored) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
      } catch (final JsonMappingException ignored) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
      } catch (final IOException ignored) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
      }
      if (info == null) {
        info = new HashMap<String, Object>();
      }
      return info;
    } else {
      return new HashMap<String, Object>();
    }
  }
}
