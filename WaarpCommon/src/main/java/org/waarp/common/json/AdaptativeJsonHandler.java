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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.format.DataFormatDetector;
import com.fasterxml.jackson.core.format.DataFormatMatcher;
import com.fasterxml.jackson.core.format.MatchStrength;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import org.waarp.common.logging.SysErrLogger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON handler using adaptative format (Smile or Json - in that order -)
 */
public class AdaptativeJsonHandler {

  public enum JsonCodec {
    SMILE(new SmileFactory()), JSON(new JsonFactory());

    public final JsonFactory factory;
    public final ObjectMapper mapper;

    JsonCodec(JsonFactory factory) {
      this.factory = factory;
      mapper = new ObjectMapper(factory);
      mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
      mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
      mapper
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      mapper.configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);
    }

    private static List<JsonFactory> getFactories() {
      final List<JsonFactory> factories = new ArrayList<JsonFactory>();
      final JsonCodec[] codecs = JsonCodec.values();
      for (final JsonCodec jsonCodec : codecs) {
        factories.add(jsonCodec.factory);
      }
      return factories;
    }

    private static HashMap<String, JsonCodec> getHashMap() {
      final HashMap<String, JsonCodec> hashmap =
          new HashMap<String, JsonCodec>();
      final JsonCodec[] codecs = JsonCodec.values();
      for (final JsonCodec jsonCodec : codecs) {
        hashmap.put(jsonCodec.factory.getFormatName(), jsonCodec);
      }
      return hashmap;
    }
  }

  /**
   * Data Format Detector
   */
  private static final DataFormatDetector detector =
      new DataFormatDetector(JsonCodec.getFactories())
          .withMinimalMatch(MatchStrength.WEAK_MATCH)
          .withOptimalMatch(MatchStrength.SOLID_MATCH);
  /**
   * HashMap getting Codec from Factory name
   */
  private static final HashMap<String, JsonCodec> factoryForName =
      JsonCodec.getHashMap();

  ObjectMapper mapper;
  private JsonCodec codec;

  public AdaptativeJsonHandler(JsonCodec codec) {
    this.codec = codec;
    mapper = codec.mapper;
  }

  public AdaptativeJsonHandler(byte[] source) throws IOException {
    final DataFormatMatcher match = detector.findFormat(source);
    if (match == null) {
      codec = JsonCodec.JSON;
      mapper = JsonCodec.JSON.mapper; // default
    } else {
      final JsonCodec codec2 = factoryForName.get(match.getMatchedFormatName());
      if (codec2 != null) {
        this.codec = codec2;
        mapper = codec2.mapper;
      } else {
        this.codec = JsonCodec.JSON;
        mapper = JsonCodec.JSON.mapper; // default
      }
    }
  }

  public AdaptativeJsonHandler(InputStream source) throws IOException {
    final DataFormatMatcher match = detector.findFormat(source);
    if (match == null) {
      codec = JsonCodec.JSON;
      mapper = JsonCodec.JSON.mapper; // default
    } else {
      final JsonCodec codec2 = factoryForName.get(match.getMatchedFormatName());
      if (codec != null) {
        this.codec = codec2;
        mapper = codec.mapper;
      } else {
        this.codec = JsonCodec.JSON;
        mapper = JsonCodec.JSON.mapper; // default
      }
    }
  }

  /**
   * Change the JsonCodec: warning, change should be done before any usage to
   * preserve consistency
   *
   * @param codec
   */
  public void changeHandler(JsonCodec codec) {
    this.codec = codec;
    mapper = codec.mapper;
  }

  /**
   * @return the associated codec
   */
  public JsonCodec getCodec() {
    return codec;
  }

  /**
   * @return an empty ObjectNode
   */
  public final ObjectNode createObjectNode() {
    return mapper.createObjectNode();
  }

  /**
   * @return an empty ArrayNode
   */
  public final ArrayNode createArrayNode() {
    return mapper.createArrayNode();
  }

  /**
   * @param value
   *
   * @return the objectNode or null if an error occurs
   */
  public final ObjectNode getFromString(String value) {
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
  public final String writeAsString(Object object) {
    try {
      return mapper.writeValueAsString(object);
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
  public final String getString(ObjectNode node, String field) {
    return getValue(node, field, (String) null);
  }

  /**
   * @param node
   * @param field
   *
   * @return the String if the field exists, else null
   */
  public final String getString(ObjectNode node, Enum<?> field) {
    return getValue(node, field.name(), (String) null);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the String if the field exists, else defValue
   */
  public final String getValue(ObjectNode node, String field, String defValue) {
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
  public final Boolean getValue(ObjectNode node, String field,
                                boolean defValue) {
    return node.path(field).asBoolean(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the Double if the field exists, else defValue
   */
  public final Double getValue(ObjectNode node, String field, double defValue) {
    return node.path(field).asDouble(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the Long if the field exists, else defValue
   */
  public final Long getValue(ObjectNode node, String field, long defValue) {
    return node.path(field).asLong(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the Integer if the field exists, else defValue
   */
  public final Integer getValue(ObjectNode node, String field, int defValue) {
    return node.path(field).asInt(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the byte array if the field exists, else defValue
   */
  public final byte[] getValue(ObjectNode node, String field, byte[] defValue) {
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
  public final void setValue(ObjectNode node, String field, boolean value) {
    node.put(field, value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public final void setValue(ObjectNode node, String field, double value) {
    node.put(field, value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public final void setValue(ObjectNode node, String field, int value) {
    node.put(field, value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public final void setValue(ObjectNode node, String field, long value) {
    node.put(field, value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public final void setValue(ObjectNode node, String field, String value) {
    if (value == null || value.isEmpty()) {
      return;
    }
    node.put(field, value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public final void setValue(ObjectNode node, String field, byte[] value) {
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
  public final boolean exist(ObjectNode node, String... field) {
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
  public final String getValue(ObjectNode node, Enum<?> field,
                               String defValue) {
    return getValue(node, field.name(), defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the Boolean if the field exists, else defValue
   */
  public final Boolean getValue(ObjectNode node, Enum<?> field,
                                boolean defValue) {
    return node.path(field.name()).asBoolean(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the Double if the field exists, else defValue
   */
  public final Double getValue(ObjectNode node, Enum<?> field,
                               double defValue) {
    return node.path(field.name()).asDouble(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the Long if the field exists, else defValue
   */
  public final Long getValue(ObjectNode node, Enum<?> field, long defValue) {
    return node.path(field.name()).asLong(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the Integer if the field exists, else defValue
   */
  public final Integer getValue(ObjectNode node, Enum<?> field, int defValue) {
    return node.path(field.name()).asInt(defValue);
  }

  /**
   * @param node
   * @param field
   * @param defValue
   *
   * @return the byte array if the field exists, else defValue
   */
  public final byte[] getValue(ObjectNode node, Enum<?> field,
                               byte[] defValue) {
    return getValue(node, field.name(), defValue);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public final void setValue(ObjectNode node, Enum<?> field, boolean value) {
    node.put(field.name(), value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public final void setValue(ObjectNode node, Enum<?> field, double value) {
    node.put(field.name(), value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public final void setValue(ObjectNode node, Enum<?> field, int value) {
    node.put(field.name(), value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public final void setValue(ObjectNode node, Enum<?> field, long value) {
    node.put(field.name(), value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public final void setValue(ObjectNode node, Enum<?> field, String value) {
    if (value == null || value.isEmpty()) {
      return;
    }
    node.put(field.name(), value);
  }

  /**
   * @param node
   * @param field
   * @param value
   */
  public final void setValue(ObjectNode node, Enum<?> field, byte[] value) {
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
  public final boolean exist(ObjectNode node, Enum<?>... field) {
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
  public final Map<String, Object> getMapFromString(String value) {
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
