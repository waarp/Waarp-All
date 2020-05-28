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

/**
 * String utils
 */
public final class StringUtils {
  /**
   * Random Generator
   */
  public static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

  public static final String LINE_SEP;

  static {
    LINE_SEP = SystemPropertyUtil.get("line.separator");
  }

  private StringUtils() {
    // empty
  }

  /**
   * @param length the length of rray
   *
   * @return a byte array with random values
   */
  public static byte[] getRandom(final int length) {
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
  public static byte[] getBytesFromArraysToString(final String bytesString) {
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
  public static String getClassName(Object object) {
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

