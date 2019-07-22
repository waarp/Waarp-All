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

import org.waarp.common.exception.InvalidArgumentException;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;


/**
 * String utils
 */
public final class StringUtils {
  /**
   * Random Generator
   */
  //1.7: private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();
  public static final Random RANDOM = new Random();

  // default parameters for XML check
  private static final String CDATA_TAG_UNESCAPED = "<![CDATA[";
  private static final String CDATA_TAG_ESCAPED = "&lt;![CDATA[";
  private static final String ENTITY_TAG_UNESCAPED = "<!ENTITY";
  private static final String ENTITY_TAG_ESCAPED = "&lt;!ENTITY";
  // default parameters for Javascript check
  private static final String SCRIPT_TAG_UNESCAPED = "<script>";
  private static final String SCRIPT_TAG_ESCAPED = "&lt;script&gt;";
  // default parameters for Json check
  private static final String TAG_START =
      "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)\\>";
  private static final String TAG_END = "\\</\\w+\\>";
  private static final String TAG_SELF_CLOSING =
      "\\<\\w+((\\s+\\w+(\\s*\\=\\s*(?:\".*?\"|'.*?'|[^'\"\\>\\s]+))?)+\\s*|\\s*)/\\>";
  private static final String HTML_ENTITY = "&[a-zA-Z][a-zA-Z0-9]+;";
  public static final Pattern HTML_PATTERN = Pattern.compile(
      "(" + TAG_START + ".*" + TAG_END + ")|(" + TAG_SELF_CLOSING + ")|(" +
      HTML_ENTITY + ")", Pattern.DOTALL);
  // Default ASCII for Param check
  public static final Pattern UNPRINTABLE_PATTERN =
      Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]");
  public static final List<String> RULES = new ArrayList<String>();

  static {
    RULES.add(CDATA_TAG_UNESCAPED);
    RULES.add(CDATA_TAG_ESCAPED);
    RULES.add(ENTITY_TAG_UNESCAPED);
    RULES.add(ENTITY_TAG_ESCAPED);
    RULES.add(SCRIPT_TAG_UNESCAPED);
    RULES.add(SCRIPT_TAG_ESCAPED);
  }

  /**
   * UTF-8 string
   */
  public static final String UTF_8 = "UTF-8";
  /**
   * UTF-8 Charset
   */
  public static final Charset UTF8 = Charset.forName(UTF_8);

  private StringUtils() {
    // empty
  }

  /**
   * Check external argument to avoid Path Traversal attack
   *
   * @param value to check
   *
   * @throws InvalidArgumentException
   */
  public static String checkSanityString(String value)
      throws InvalidArgumentException {
    checkSanityString(new String[] { value });
    return value;
  }

  /**
   * Check external argument
   *
   * @param strings
   *
   * @throws InvalidArgumentException
   */
  public static void checkSanityString(String... strings)
      throws InvalidArgumentException {
    for (String field : strings) {
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

  /**
   * @param length the length of rray
   *
   * @return a byte array with random values
   */
  public static final byte[] getRandom(final int length) {
    if (length <= 0) {
      return SingletonUtils.getSingletonByteArray();
    }
    final byte[] result = new byte[length];
    for (int i = 0; i < result.length; i++) {
      result[i] = (byte) (RANDOM.nextInt(95) + 32);
    }
    return result;
  }

  /**
   * Revert Arrays.toString for bytes
   *
   * @param bytesString the string to transform
   *
   * @return the array of bytes
   *
   * @throws IllegalArgumentException if bytesString is null or empty
   */
  public static final byte[] getBytesFromArraysToString(
      final String bytesString) {
    ParametersChecker
        .checkParameter("Should not be null or empty", bytesString);
    final String[] strings =
        bytesString.replace("[", "").replace("]", "").split(", ");
    final byte[] result = new byte[strings.length];
    try {
      for (int i = 0; i < result.length; i++) {
        result[i] = (byte) (Integer.parseInt(strings[i]) & 0xFF);
      }
    } catch (final NumberFormatException e) {
      throw new IllegalArgumentException(e);
    }
    return result;
  }

  /**
   * @param object to get its class name
   *
   * @return the short name of the Class of this object
   */
  public static final String getClassName(Object object) {
    final Class<?> clasz = object.getClass();
    String name = clasz.getSimpleName();
    if (name != null && !name.isEmpty()) {
      return name;
    } else {
      name = clasz.getName();
      final int pos = name.lastIndexOf('.');
      if (pos < 0) {
        return name;
      }
      return name.substring(pos + 1);
    }
  }

}

