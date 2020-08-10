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
package org.waarp.openr66.client.utils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.waarp.common.json.JsonHandler;
import org.waarp.openr66.protocol.configuration.Messages;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This method allows to format output for Waarp R66 clients
 */
public class OutputFormat extends JsonHandler {
  private static final String XML = "XML";
  private static final String PROPERTY = "PROPERTY";
  private static final String CSV = "CSV";
  private static final String MESSAGE_CANT_CONVERT = "Message.CantConvert";

  public enum OUTPUTFORMAT {
    QUIET, JSON, XML, PROPERTY, CSV
  }

  static OUTPUTFORMAT defaultOutput = OUTPUTFORMAT.JSON;

  public enum FIELDS {
    command, args, status, statusTxt, transfer, error, remote, statusCode,
    specialid, originalPath, finalPath, ruleid, requester, requested,
    originalSize, fileInformation
  }

  /**
   * Helper to set the output format desired for the command
   *
   * @param args
   */
  public static void getParams(String[] args) {
    for (int i = 1; i < args.length; i++) {
      if ("-quiet".equalsIgnoreCase(args[i])) {
        defaultOutput = OUTPUTFORMAT.QUIET;
      } else if ("-xml".equalsIgnoreCase(args[i])) {
        defaultOutput = OUTPUTFORMAT.XML;
      } else if ("-csv".equalsIgnoreCase(args[i])) {
        defaultOutput = OUTPUTFORMAT.CSV;
      } else if ("-json".equalsIgnoreCase(args[i])) {
        defaultOutput = OUTPUTFORMAT.JSON;
      } else if ("-property".equalsIgnoreCase(args[i])) {
        defaultOutput = OUTPUTFORMAT.PROPERTY;
      }
    }
  }

  /**
   * To set the default OutputFormat
   *
   * @param outputformat
   */
  public static void setDefaultOutput(OUTPUTFORMAT outputformat) {
    defaultOutput = outputformat;
  }

  private OUTPUTFORMAT format = defaultOutput;
  private final ObjectNode node = createObjectNode();

  /**
   * Create an OutputFormat helper using the default Format defined in
   * defaultOutput
   *
   * @param command
   * @param args
   */
  public OutputFormat(String command, String[] args) {
    setValue(FIELDS.command.name(), command);
    if (args != null) {
      final StringBuilder builder = new StringBuilder();
      for (final String string : args) {
        builder.append(string).append(' ');
      }
      setValue(FIELDS.args.name(), builder.toString());
    }
  }

  /**
   * To change the applied format
   *
   * @param format
   */
  public void setFormat(OUTPUTFORMAT format) {
    this.format = format;
  }

  /**
   * @param values
   */
  public void setValue(Map<String, Object> values) {
    final String json = writeAsString(values);
    final ObjectNode temp = getFromString(json);
    node.setAll(temp);
  }

  /**
   * @param values
   */
  public void setValueString(Map<String, String> values) {
    final String json = writeAsString(values);
    final ObjectNode temp = getFromString(json);
    node.setAll(temp);
  }

  /**
   * @param node
   */
  public void setValueString(ObjectNode node) {
    node.setAll(node);
  }

  /**
   * @param field
   * @param value
   */
  public void setValue(String field, boolean value) {
    setValue(node, field, value);
  }

  /**
   * @param field
   * @param value
   */
  public final void setValue(String field, double value) {
    setValue(node, field, value);
  }

  /**
   * @param field
   * @param value
   */
  public final void setValue(String field, int value) {
    setValue(node, field, value);
  }

  /**
   * @param field
   * @param value
   */
  public final void setValue(String field, long value) {
    setValue(node, field, value);
  }

  /**
   * @param field
   * @param value
   */
  public final void setValue(String field, String value) {
    setValue(node, field, value);
  }

  /**
   * @param field
   * @param value
   */
  public final void setValue(String field, byte[] value) {
    setValue(node, field, value);
  }

  /**
   * @param field
   *
   * @return True if all fields exist
   */
  public final boolean exist(String... field) {
    return exist(node, field);
  }

  /**
   * @return True if the current default output format is on QUIET
   */
  public static boolean isQuiet() {
    return defaultOutput == OUTPUTFORMAT.QUIET;
  }

  /**
   * @return True if the current default output format is on XML
   */
  public static boolean isXml() {
    return defaultOutput == OUTPUTFORMAT.XML;
  }

  /**
   * @return True if the current default output format is on CSV
   */
  public static boolean isCsv() {
    return defaultOutput == OUTPUTFORMAT.CSV;
  }

  /**
   * @return True if the current default output format is on JSON
   */
  public static boolean isJson() {
    return defaultOutput == OUTPUTFORMAT.JSON;
  }

  /**
   * @return True if the current default output format is on PROPERTY
   */
  public static boolean isProperty() {
    return defaultOutput == OUTPUTFORMAT.PROPERTY;
  }

  /**
   * Helper for sysOut
   */
  public void sysout() {
    if (format != OUTPUTFORMAT.QUIET) {
      System.out.println(getContext());//NOSONAR
      System.out.println(toString(format));//NOSONAR
    }
  }

  private String getContext() {
    return '[' + getValue(node, FIELDS.command, "") + "] " +
           getValue(node, FIELDS.statusTxt, "");
  }

  /**
   * Helper for Logger
   *
   * @return the String to print in logger
   */
  public String loggerOut() {
    return getContext() + " => " + toString(OUTPUTFORMAT.JSON);
  }

  @Override
  public String toString() {
    return toString(format);
  }

  /**
   * Helper to get string representation of the current object
   *
   * @param format
   *
   * @return the String representation
   */
  public String toString(OUTPUTFORMAT format) {
    final String inString = writeAsString(node);
    switch (format) {
      case CSV:
        try {
          final Map<String, Object> map = mapper
              .readValue(inString, new TypeReference<Map<String, Object>>() {
              });
          final StringBuilder builderKeys = new StringBuilder();
          final StringBuilder builderValues = new StringBuilder();
          boolean next = false;
          for (final Entry<String, Object> entry : map.entrySet()) {
            if (next) {
              builderKeys.append(';');
              builderValues.append(';');
            } else {
              next = true;
            }
            builderKeys.append(entry.getKey());
            builderValues.append(entry.getValue());
          }
          return builderKeys + "\n" + builderValues;
        } catch (final JsonParseException e) {
          return Messages.getString(MESSAGE_CANT_CONVERT, CSV) + inString;
        } catch (final JsonMappingException e) {
          return Messages.getString(MESSAGE_CANT_CONVERT, CSV) + inString;
        } catch (final IOException e) {
          return Messages.getString(MESSAGE_CANT_CONVERT, CSV) + inString;
        }
      case PROPERTY:
        try {
          final Map<String, Object> map = mapper
              .readValue(inString, new TypeReference<Map<String, Object>>() {
              });
          final StringBuilder builder = new StringBuilder();
          boolean next = false;
          for (final Entry<String, Object> entry : map.entrySet()) {
            if (next) {
              builder.append('\n');
            } else {
              next = true;
            }
            builder.append(entry.getKey()).append('=').append(entry.getValue());
          }
          return builder.toString();
        } catch (final JsonParseException e) {
          return Messages.getString(MESSAGE_CANT_CONVERT, PROPERTY) + inString;
        } catch (final JsonMappingException e) {
          return Messages.getString(MESSAGE_CANT_CONVERT, PROPERTY) + inString;
        } catch (final IOException e) {
          return Messages.getString(MESSAGE_CANT_CONVERT, PROPERTY) + inString;
        }
      case XML:
        try {
          final Map<String, Object> map = mapper
              .readValue(inString, new TypeReference<Map<String, Object>>() {
              });
          final StringBuilder builder = new StringBuilder("<xml>");
          for (final Entry<String, Object> entry : map.entrySet()) {
            builder.append('<').append(entry.getKey()).append('>')
                   .append(entry.getValue()).append("</").append(entry.getKey())
                   .append('>');
          }
          builder.append("</xml>");
          return builder.toString();
        } catch (final JsonParseException e) {
          return Messages.getString(MESSAGE_CANT_CONVERT, XML) + inString;
        } catch (final JsonMappingException e) {
          return Messages.getString(MESSAGE_CANT_CONVERT, XML) + inString;
        } catch (final IOException e) {
          return Messages.getString(MESSAGE_CANT_CONVERT, XML) + inString;
        }
      case QUIET:
      case JSON:
      default:
        return inString;
    }
  }
}
