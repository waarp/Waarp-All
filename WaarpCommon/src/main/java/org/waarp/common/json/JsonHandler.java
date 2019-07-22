/**
   This file is part of Waarp Project.

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All Waarp Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Waarp is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Waarp .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.common.json;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

/**
 * Abstract Json Handler
 * 
 * @author "Frederic Bregier"
 *
 */
public class JsonHandler {

    /**
     * JSON parser
     */
    public static final ObjectMapper mapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonGenerator.Feature.ESCAPE_NON_ASCII, true);

    protected JsonHandler() {
    }

    /**
     * 
     * @return an empty ObjectNode
     */
    public static ObjectNode createObjectNode() {
        return mapper.createObjectNode();
    }

    /**
     * 
     * @return an empty ArrayNode
     */
    public static ArrayNode createArrayNode() {
        return mapper.createArrayNode();
    }

    /**
     * Parses a string representation of a JSON object and returns 
     * an ObjectNode.
     * JSON Processing exceptions are kept.
     * 
     * @param value
     * @return the objectNode or null if an error occurs
     * @throws JsonProcessingException
     */
    public static ObjectNode getFromStringExc(String value) throws JsonProcessingException {
        try {
            return (ObjectNode) mapper.readTree(value);
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Parses a string representation of a JSON object and returns 
     * an ObjectNode, swallowing any processing exception.
     *
     * @param value
     * @return the objectNode or null if an error occurs
     */
    public static ObjectNode getFromString(String value) {
        try {
            return (ObjectNode) mapper.readTree(value);
        } catch (JsonProcessingException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     *
     * @param file
     * @return the jsonNode (ObjectNode or ArrayNode)
     * @throws InvalidParseOperationException
     */
    public static final ObjectNode getFromFile(final File file) {
        try {
            return (ObjectNode) mapper.readTree(file);
        } catch (final JsonProcessingException e) {
            return null;
        } catch (final IOException e) {
            return null;
        }
    }
    /**
     *
     * @param value
     * @param clasz
     * @return the object of type clasz
     * @throws InvalidParseOperationException
     */
    public static final <T> T getFromString(final String value, final Class<T> clasz) {
        try {
            return mapper.readValue(value, clasz);
        } catch (IOException e) {
            return null;
        }
    }
    /**
     *
     * @param file 
     * @param clasz
     * @return the corresponding object
     * @throws InvalidParseOperationException
     */
    public static final Object getFromFile(File file, Class<?> clasz) {
        try {
            return mapper.readValue(file, clasz);
        } catch (final IOException e) {
            return null;
        }
    }

    /**
     * 
     * @param object
     * @return the Json representation of the object
     */
    public static String writeAsString(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
    /**
     *
     * @param object
     * @param file 
     * @return True if correctly written
     * @throws InvalidParseOperationException
     */
    public static final boolean writeAsFile(final Object object, File file) {
        try {
            mapper.writeValue(file, object);
            return true;
        } catch (final IOException e) {
            return false;
        }
    }

    /**
     * 
     * @param object
     * @return the Json representation of the object in Pretty Print format
     */
    public static String prettyPrint(Object object) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    /**
     * 
     * @param node
     * @param field
     * @return the String if the field exists, else null
     */
    public final static String getString(ObjectNode node, String field) {
        return getValue(node, field, (String) null);
    }

    /**
     * 
     * @param node
     * @param field
     * @return the String if the field exists, else null
     */
    public final static String getString(ObjectNode node, Enum<?> field) {
        return getValue(node, field.name(), (String) null);
    }

    /**
     * 
     * @param node
     * @param field
     * @param defValue
     * @return the String if the field exists, else defValue
     */
    public final static String getValue(ObjectNode node, String field, String defValue) {
        JsonNode elt = node.get(field);
        if (elt != null) {
            String val = elt.asText();
            if (val.equals("null")) {
                return defValue;
            }
            return val;
        }
        return defValue;
    }

    /**
     * 
     * @param node
     * @param field
     * @param defValue
     * @return the Boolean if the field exists, else defValue
     */
    public final static Boolean getValue(ObjectNode node, String field, boolean defValue) {
        return node.path(field).asBoolean(defValue);
    }

    /**
     * 
     * @param node
     * @param field
     * @param defValue
     * @return the Double if the field exists, else defValue
     */
    public final static Double getValue(ObjectNode node, String field, double defValue) {
        return node.path(field).asDouble(defValue);
    }

    /**
     * 
     * @param node
     * @param field
     * @param defValue
     * @return the Long if the field exists, else defValue
     */
    public final static Long getValue(ObjectNode node, String field, long defValue) {
        return node.path(field).asLong(defValue);
    }

    /**
     * 
     * @param node
     * @param field
     * @param defValue
     * @return the Integer if the field exists, else defValue
     */
    public final static Integer getValue(ObjectNode node, String field, int defValue) {
        return node.path(field).asInt(defValue);
    }

    /**
     * 
     * @param node
     * @param field
     * @param defValue
     * @return the byte array if the field exists, else defValue
     */
    public final static byte[] getValue(ObjectNode node, String field, byte[] defValue) {
        JsonNode elt = node.get(field);
        if (elt != null) {
            try {
                return elt.binaryValue();
            } catch (IOException e) {
                return defValue;
            }
        }
        return defValue;
    }

    /**
     * 
     * @param node
     * @param field
     * @param value
     */
    public final static void setValue(ObjectNode node, String field, boolean value) {
        node.put(field, value);
    }

    /**
     * 
     * @param node
     * @param field
     * @param value
     */
    public final static void setValue(ObjectNode node, String field, double value) {
        node.put(field, value);
    }

    /**
     * 
     * @param node
     * @param field
     * @param value
     */
    public final static void setValue(ObjectNode node, String field, int value) {
        node.put(field, value);
    }

    /**
     * 
     * @param node
     * @param field
     * @param value
     */
    public final static void setValue(ObjectNode node, String field, long value) {
        node.put(field, value);
    }

    /**
     * 
     * @param node
     * @param field
     * @param value
     */
    public final static void setValue(ObjectNode node, String field, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        node.put(field, value);
    }

    /**
     * 
     * @param node
     * @param field
     * @param value
     */
    public final static void setValue(ObjectNode node, String field, byte[] value) {
        if (value == null || value.length == 0) {
            return;
        }
        node.put(field, value);
    }

    /**
     * 
     * @param node
     * @param field
     * @return True if all fields exist
     */
    public final static boolean exist(ObjectNode node, String... field) {
        for (String string : field) {
            if (!node.has(string))
                return false;
        }
        return true;
    }

    /**
     * 
     * @param node
     * @param field
     * @param defValue
     * @return the String if the field exists, else defValue
     */
    public final static String getValue(ObjectNode node, Enum<?> field, String defValue) {
        return getValue(node, field.name(), defValue);
    }

    /**
     * 
     * @param node
     * @param field
     * @param defValue
     * @return the Boolean if the field exists, else defValue
     */
    public final static Boolean getValue(ObjectNode node, Enum<?> field, boolean defValue) {
        return node.path(field.name()).asBoolean(defValue);
    }

    /**
     * 
     * @param node
     * @param field
     * @param defValue
     * @return the Double if the field exists, else defValue
     */
    public final static Double getValue(ObjectNode node, Enum<?> field, double defValue) {
        return node.path(field.name()).asDouble(defValue);
    }

    /**
     * 
     * @param node
     * @param field
     * @param defValue
     * @return the Long if the field exists, else defValue
     */
    public final static Long getValue(ObjectNode node, Enum<?> field, long defValue) {
        return node.path(field.name()).asLong(defValue);
    }

    /**
     * 
     * @param node
     * @param field
     * @param defValue
     * @return the Integer if the field exists, else defValue
     */
    public final static Integer getValue(ObjectNode node, Enum<?> field, int defValue) {
        return node.path(field.name()).asInt(defValue);
    }

    /**
     * 
     * @param node
     * @param field
     * @param defValue
     * @return the byte array if the field exists, else defValue
     */
    public final static byte[] getValue(ObjectNode node, Enum<?> field, byte[] defValue) {
        return getValue(node, field.name(), defValue);
    }

    /**
     * 
     * @param node
     * @param field
     * @param value
     */
    public final static void setValue(ObjectNode node, Enum<?> field, boolean value) {
        node.put(field.name(), value);
    }

    /**
     * 
     * @param node
     * @param field
     * @param value
     */
    public final static void setValue(ObjectNode node, Enum<?> field, double value) {
        node.put(field.name(), value);
    }

    /**
     * 
     * @param node
     * @param field
     * @param value
     */
    public final static void setValue(ObjectNode node, Enum<?> field, int value) {
        node.put(field.name(), value);
    }

    /**
     * 
     * @param node
     * @param field
     * @param value
     */
    public final static void setValue(ObjectNode node, Enum<?> field, long value) {
        node.put(field.name(), value);
    }

    /**
     * 
     * @param node
     * @param field
     * @param value
     */
    public final static void setValue(ObjectNode node, Enum<?> field, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        node.put(field.name(), value);
    }

    /**
     * 
     * @param node
     * @param field
     * @param value
     */
    public final static void setValue(ObjectNode node, Enum<?> field, byte[] value) {
        if (value == null || value.length == 0) {
            return;
        }
        node.put(field.name(), value);
    }

    /**
     * 
     * @param node
     * @param field
     * @return True if all fields exist
     */
    public final static boolean exist(ObjectNode node, Enum<?>... field) {
        for (Enum<?> enm : field) {
            if (!node.has(enm.name()))
                return false;
        }
        return true;
    }

    /**
     * 
     * @param value
     * @return the corresponding HashMap
     */
    public static Map<String, Object> getMapFromString(String value) {
        if (value != null && !value.isEmpty()) {
            Map<String, Object> info = null;
            try {
                info = mapper.readValue(value, new TypeReference<Map<String, Object>>() {});
            } catch (JsonParseException e1) {
            } catch (JsonMappingException e1) {
            } catch (IOException e1) {
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
