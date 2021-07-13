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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.utility.ParametersChecker;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract Json Handler
 */
public class JsonHandler {

  public static final TypeReference<Map<String, Object>>
      typeReferenceMapStringObject = new TypeReference<Map<String, Object>>() {
  };
  /**
   * JSON parser
   */
  public static final ObjectMapper mapper =
      new ObjectMapper().configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                        .configure(
                            JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                        .configure(
                            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                            false)
                        .configure(JsonGenerator.Feature.ESCAPE_NON_ASCII,
                                   true);

  protected JsonHandler() {
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
   * Parses a string representation of a JSON object and returns an
   * ObjectNode.
   * JSON Processing exceptions are
   * kept.
   *
   * @param value
   *
   * @return the objectNode or null if an error occurs
   *
   * @throws JsonProcessingException
   */
  public static ObjectNode getFromStringExc(final String value)
      throws JsonProcessingException {
    try {
      return (ObjectNode) mapper.readTree(value);
    } catch (final JsonProcessingException e) {
      throw e;
    } catch (final Exception e) {
      return null;
    }
  }

  /**
   * Parses a string representation of a JSON object and returns an
   * ObjectNode,
   * swallowing any processing
   * exception.
   *
   * @param value
   *
   * @return the objectNode or null if an error occurs
   */
  public static ObjectNode getFromString(final String value) {
    try {
      return (ObjectNode) mapper.readTree(value);
    } catch (final JsonProcessingException e) {
      return null;
    } catch (final Exception e) {
      return null;
    }
  }

  /**
   * @param file
   *
   * @return the jsonNode (ObjectNode or ArrayNode)
   */
  public static ObjectNode getFromFile(final File file) {
    try {
      return (ObjectNode) mapper.readTree(file);
    } catch (final JsonProcessingException e) {
      return null;
    } catch (final IOException e) {
      return null;
    }
  }

  /**
   * @param value
   * @param clasz
   *
   * @return the object of type clasz
   */
  public static <T> T getFromString(final String value, final Class<T> clasz) {
    try {
      return mapper.readValue(value, clasz);
    } catch (final IOException e) {
      return null;
    }
  }

  /**
   * @param file
   * @param clasz
   *
   * @return the corresponding object
   */
  public static Object getFromFile(final File file, final Class<?> clasz) {
    try {
      return mapper.readValue(file, clasz);
    } catch (final IOException e) {
      return null;
    }
  }

  /**
   * @param object
   *
   * @return the Json representation of the object
   */
  public static String writeAsString(final Object object) {
    try {
      return mapper.writeValueAsString(object);
    } catch (final JsonProcessingException e) {
      return "{}";
    }
  }

  /**
   * @param object
   *
   * @return the Json escaped representation of the object
   */
  public static String writeAsStringEscaped(final Object object) {
    try {
      final String temp = mapper.writeValueAsString(object);
      return temp.replaceAll("[\\\\]+", "\\\\");
    } catch (final JsonProcessingException e) {
      return "{}";
    }
  }

  /**
   * Unespace source string before analyzing it as Json
   *
   * @param source
   *
   * @return the unescaped source
   */
  public static String unEscape(final String source) {
    return source.replace("\\", "");
  }

  /**
   * @param object
   * @param file
   *
   * @return True if correctly written
   */
  public static boolean writeAsFile(final Object object, final File file) {
    try {
      mapper.writeValue(file, object);
      return true;
    } catch (final IOException e) {
      return false;
    }
  }

  /**
   * @param object
   *
   * @return the Json representation of the object in Pretty Print format
   */
  public static String prettyPrint(final Object object) {
    try {
      return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    } catch (final JsonProcessingException e) {
      return "{}";
    }
  }

  /**
   * @param node
   * @param field
   *
   * @return the String if the field exists, else null
   */
  public static String getString(final ObjectNode node, final String field) {
    return getValue(node, field, (String) null);
  }

  /**
   * @param node
   * @param field
   *
   * @return the String if the field exists, else null
   */
  public static String getString(final ObjectNode node, final Enum<?> field) {
    return getValue(node, field.name(), (String) null);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the String if the field exists, else defValue
   */
  public static String getValue(final ObjectNode node, final String field,
                                final String defValue) {
    final JsonNode elt = node.get(field);
    if (elt != null) {
      final String val = elt.asText();
      if ("null".equals(val)) {
        return defValue;
      }
      return val;
    }
    return defValue;
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the Boolean if the field exists, else defValue
   */
  public static Boolean getValue(final ObjectNode node, final String field,
                                 final boolean defValue) {
    return node.path(field).asBoolean(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the Double if the field exists, else defValue
   */
  public static Double getValue(final ObjectNode node, final String field,
                                final double defValue) {
    return node.path(field).asDouble(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the Long if the field exists, else defValue
   */
  public static Long getValue(final ObjectNode node, final String field,
                              final long defValue) {
    return node.path(field).asLong(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the Integer if the field exists, else defValue
   */
  public static Integer getValue(final ObjectNode node, final String field,
                                 final int defValue) {
    return node.path(field).asInt(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the byte array if the field exists, else defValue
   */
  public static byte[] getValue(final ObjectNode node, final String field,
                                final byte[] defValue) {
    final JsonNode elt = node.get(field);
    if (elt != null) {
      try {
        return elt.binaryValue();
      } catch (final IOException e) {
        return defValue;
      }
    }
    return defValue;
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public static void setValue(final ObjectNode node, final String field,
                              final boolean value) {
    node.put(field, value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public static void setValue(final ObjectNode node, final String field,
                              final double value) {
    node.put(field, value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public static void setValue(final ObjectNode node, final String field,
                              final int value) {
    node.put(field, value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public static void setValue(final ObjectNode node, final String field,
                              final long value) {
    node.put(field, value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public static void setValue(final ObjectNode node, final String field,
                              final String value) {
    if (ParametersChecker.isEmpty(value)) {
      return;
    }
    node.put(field, value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public static void setValue(final ObjectNode node, final String field,
                              final byte[] value) {
    if (value == null || value.length == 0) {
      return;
    }
    node.put(field, value);
  }

  /**
   * @param node
   * @param field
   *
   * @return True if all fields exist
   */
  public static boolean exist(final ObjectNode node, final String... field) {
    for (final String string : field) {
      if (!node.has(string)) {
        return false;
      }
    }
    return true;
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the String if the field exists, else defValue
   */
  public static String getValue(final ObjectNode node, final Enum<?> field,
                                final String defValue) {
    return getValue(node, field.name(), defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the Boolean if the field exists, else defValue
   */
  public static Boolean getValue(final ObjectNode node, final Enum<?> field,
                                 final boolean defValue) {
    return node.path(field.name()).asBoolean(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the Double if the field exists, else defValue
   */
  public static Double getValue(final ObjectNode node, final Enum<?> field,
                                final double defValue) {
    return node.path(field.name()).asDouble(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the Long if the field exists, else defValue
   */
  public static Long getValue(final ObjectNode node, final Enum<?> field,
                              final long defValue) {
    return node.path(field.name()).asLong(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the Integer if the field exists, else defValue
   */
  public static Integer getValue(final ObjectNode node, final Enum<?> field,
                                 final int defValue) {
    return node.path(field.name()).asInt(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the byte array if the field exists, else defValue
   */
  public static byte[] getValue(final ObjectNode node, final Enum<?> field,
                                final byte[] defValue) {
    return getValue(node, field.name(), defValue);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public static void setValue(final ObjectNode node, final Enum<?> field,
                              final boolean value) {
    node.put(field.name(), value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public static void setValue(final ObjectNode node, final Enum<?> field,
                              final double value) {
    node.put(field.name(), value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public static void setValue(final ObjectNode node, final Enum<?> field,
                              final int value) {
    node.put(field.name(), value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public static void setValue(final ObjectNode node, final Enum<?> field,
                              final long value) {
    node.put(field.name(), value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public static void setValue(final ObjectNode node, final Enum<?> field,
                              final String value) {
    if (ParametersChecker.isEmpty(value)) {
      return;
    }
    node.put(field.name(), value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public static void setValue(final ObjectNode node, final Enum<?> field,
                              final byte[] value) {
    if (value == null || value.length == 0) {
      return;
    }
    node.put(field.name(), value);
  }

  /**
   * @param node
   * @param field
   *
   * @return True if all fields exist
   */
  public static boolean exist(final ObjectNode node, final Enum<?>... field) {
    for (final Enum<?> enm : field) {
      if (!node.has(enm.name())) {
        return false;
      }
    }
    return true;
  }

  /**
   * @param value
   *
   * @return the corresponding HashMap
   */
  public static Map<String, Object> getMapFromString(final String value) {
    if (ParametersChecker.isNotEmpty(value)) {
      Map<String, Object> info = null;
      try {
        info = mapper.readValue(value, typeReferenceMapStringObject);
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
