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

package org.waarp.http.protocol;

import com.google.common.base.Strings;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

/**
 * Http Helper methods
 */
public class HttpHelper {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpHelper.class);

  private HttpHelper() {
    // Empty constructor
  }

  /**
   * Convert String to long
   *
   * @param value
   * @param def default value
   *
   * @return the long value or def is null or empty or not parsable
   */
  public static long toLong(final String value, final long def) {
    if (Strings.isNullOrEmpty(value)) {
      return def;
    }

    try {
      return Long.parseLong(value);
    } catch (final NumberFormatException e) {
      logger.warn(e);
      return def;
    }
  }

  /**
   * Convert String to int
   *
   * @param value
   * @param def default value
   *
   * @return the int value or def is null or empty or not parsable
   */
  public static int toInt(final String value, final int def) {
    if (Strings.isNullOrEmpty(value)) {
      return def;
    }
    try {
      return Integer.parseInt(value);
    } catch (final NumberFormatException e) {
      logger.warn(e);
      return def;
    }
  }
}
