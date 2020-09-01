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

package org.waarp.common.utility;

import com.google.common.base.Strings;
import org.waarp.common.exception.InvalidArgumentException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checker for Parameters <br>
 * <br>
 * Can be used for String (testing also emptiness) and for general Object.<br>
 * For null String only, use the special method.
 */
public final class ParametersChecker {
  public static final String DEFAULT_ERROR =
      "Parameter should not be null or empty";
  // Default ASCII for Param check
  public static final Pattern UNPRINTABLE_PATTERN =
      Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");
  public static final List<String> RULES = new ArrayList<String>();
  public static final String[] ZERO_ARRAY_STRING = new String[0];
  // default parameters for XML check
  static final String CDATA_TAG_UNESCAPED = "<![CDATA[";
  static final String CDATA_TAG_ESCAPED = "&lt;![CDATA[";
  static final String ENTITY_TAG_UNESCAPED = "<!ENTITY";
  static final String ENTITY_TAG_ESCAPED = "&lt;!ENTITY";
  // default parameters for Javascript check
  static final String SCRIPT_TAG_UNESCAPED = "<script>";
  static final String SCRIPT_TAG_ESCAPED = "&lt;script&gt;";
  // default parameters for Json check
  private static final String TAG_START =
      "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)\\>";
  private static final String TAG_END = "\\</\\w+\\>";
  private static final String TAG_SELF_CLOSING =
      "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)/\\>";
  private static final String HTML_ENTITY = "&[a-zA-Z][a-zA-Z0-9]+;";
  // Allowed
  public static final Pattern HTML_PATTERN = Pattern.compile(
      '(' + TAG_START + ".*" + TAG_END + ")|(" + TAG_SELF_CLOSING + ")|(" +
      HTML_ENTITY + ')', Pattern.DOTALL);

  static {
    RULES.add(CDATA_TAG_UNESCAPED);
    RULES.add(CDATA_TAG_ESCAPED);
    RULES.add(ENTITY_TAG_UNESCAPED);
    RULES.add(ENTITY_TAG_ESCAPED);
    RULES.add(SCRIPT_TAG_UNESCAPED);
    RULES.add(SCRIPT_TAG_ESCAPED);
  }

  private ParametersChecker() {
    // empty
  }

  private static final String MANDATORY_PARAMETER = " is mandatory parameter";

  /**
   * Check if any parameter are null or empty and if so, throw an
   * IllegalArgumentException
   *
   * @param errorMessage the error message
   * @param parameters parameters to be checked
   *
   * @throws IllegalArgumentException if null or empty
   */
  public static void checkParameter(final String errorMessage,
                                    final String... parameters) {
    if (parameters == null) {
      throw new IllegalArgumentException(errorMessage);
    }
    for (final String parameter : parameters) {
      if (Strings.isNullOrEmpty(parameter) || parameter.trim().isEmpty()) {
        throw new IllegalArgumentException(errorMessage);
      }
    }
  }

  /**
   * Check if any parameter are null or empty and if so, throw an
   * IllegalArgumentException
   *
   * @param errorMessage the error message
   * @param parameters set of parameters
   *
   * @throws IllegalArgumentException if null or empty
   */
  public static void checkParameterDefault(final String errorMessage,
                                           final String... parameters) {
    if (parameters == null) {
      throw new IllegalArgumentException(errorMessage + MANDATORY_PARAMETER);
    }
    for (final String parameter : parameters) {
      if (Strings.isNullOrEmpty(parameter) || parameter.trim().isEmpty()) {
        throw new IllegalArgumentException(errorMessage + MANDATORY_PARAMETER);
      }
    }
  }

  /**
   * Check if any parameter are null or empty and if so, return false
   *
   * @param parameters set of parameters
   *
   * @return True if not null and not empty neither containing only spaces
   */
  public static boolean isNotEmpty(final String... parameters) {
    if (parameters == null) {
      return false;
    }
    for (final String parameter : parameters) {
      if (Strings.isNullOrEmpty(parameter) || parameter.trim().isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if any parameter are null or empty and if so, throw an
   * IllegalArgumentException
   *
   * @param errorMessage the error message
   * @param parameters set of parameters
   *
   * @throws IllegalArgumentException if null or empty
   */
  public static void checkParameterDefault(final String errorMessage,
                                           final Object... parameters) {
    if (parameters == null) {
      throw new IllegalArgumentException(errorMessage + MANDATORY_PARAMETER);
    }
    for (final Object parameter : parameters) {
      if (parameter == null) {
        throw new IllegalArgumentException(errorMessage + MANDATORY_PARAMETER);
      }
    }
  }

  /**
   * Check if any parameter are null and if so, throw an
   * IllegalArgumentException
   *
   * @param errorMessage the error message
   * @param parameters parameters to be checked
   *
   * @throws IllegalArgumentException if null
   */
  public static void checkParameterNullOnly(final String errorMessage,
                                            final String... parameters) {
    if (parameters == null) {
      throw new IllegalArgumentException(errorMessage);
    }
    for (final String parameter : parameters) {
      if (parameter == null) {
        throw new IllegalArgumentException(errorMessage);
      }
    }
  }

  /**
   * Check if any parameter are null and if so, throw an
   * IllegalArgumentException
   *
   * @param errorMessage set of parameters
   * @param parameters set parameters to be checked
   *
   * @throws IllegalArgumentException if null
   */
  public static void checkParameter(final String errorMessage,
                                    final Object... parameters) {
    if (parameters == null) {
      throw new IllegalArgumentException(errorMessage);
    }
    for (final Object parameter : parameters) {
      if (parameter == null) {
        throw new IllegalArgumentException(errorMessage);
      }
    }
  }

  /**
   * Check if an integer parameter is greater or equals to minValue
   *
   * @param name name of the variable
   * @param variable the value of variable to check
   * @param minValue the min value
   */
  public static void checkValue(final String name, final long variable,
                                final long minValue) {
    if (variable < minValue) {
      throw new IllegalArgumentException(
          "Parameter " + name + " is less than " + minValue);
    }
  }

  /**
   * Check external argument to avoid Path Traversal attack
   *
   * @param value to check
   *
   * @throws InvalidArgumentException
   */
  public static String checkSanityString(final String value)
      throws InvalidArgumentException {
    checkSanityString(new String[] { value });
    return value;
  }

  /**
   * Check external argument (null is consider as correct)
   *
   * @param strings
   *
   * @throws InvalidArgumentException
   */
  public static void checkSanityString(final String... strings)
      throws InvalidArgumentException {
    for (final String field : strings) {
      if (field == null || field.isEmpty()) {
        continue;
      }
      if (UNPRINTABLE_PATTERN.matcher(field).find()) {
        throw new InvalidArgumentException("Invalid input bytes");
      }
      for (final String rule : RULES) {
        if (field != null && rule != null && field.contains(rule)) {
          throw new InvalidArgumentException("Invalid tag sanity check");
        }
      }
    }
  }
}