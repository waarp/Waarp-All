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

package org.waarp.openr66.dao;

public class Filter {
  public static final String LIKE = "LIKE";
  public static final String BETWEEN = "BETWEEN";
  public static final String IS_NOT_NULL = "IS NOT NULL";
  public static final String IN = "IN";
  private static final byte B_NONE = 'n';
  private static final byte B_SINGLE = 's';
  private static final byte B_MULTIPLE = 'm';
  public final String key;
  public final String operand;
  public final Object value;
  private final int nbArgs;
  private final byte specialOperand;

  public Filter(final String key, final String operand,
                final Object... values) {
    this.key = key;
    this.operand = operand;
    nbArgs = values == null? 0 : values.length;
    if (nbArgs == 0) {
      this.value = null;
      specialOperand = B_NONE;
    } else if (nbArgs == 1) {
      this.value = values[0];
      specialOperand = B_SINGLE;
    } else {
      this.value = values;
      specialOperand = B_MULTIPLE;
    }
  }

  /**
   * @return the number of values for the operand (-1 for 0, 0 for 1, 1 for 2
   *     or (n-1) for n)
   */
  public final int nbAdditionnalParams() {
    return nbArgs - 1;
  }

  /**
   * Helper
   *
   * @param builder
   *
   * @return the associated value
   */
  public final Object append(final StringBuilder builder) {
    if (nbArgs == 0) {
      builder.append(key).append(' ').append(operand).append(" ");
    } else if (nbArgs == 1) {
      builder.append(key).append(' ').append(operand).append(" ? ");
    } else {
      if (BETWEEN.equalsIgnoreCase(operand)) {
        // Object is a Object[2]
        builder.append(key).append(' ').append(BETWEEN).append(" ? AND ? ");
      } else if (IN.equalsIgnoreCase((operand))) {
        // Object is a Object[n]
        builder.append(key).append(' ').append(IN).append("(?");
        for (int i = 1; i < nbArgs; i++) {
          builder.append(", ?");
        }
        builder.append(") ");
      } else {
        throw new IllegalArgumentException(
            "Command seems to not support multiple arguments: " + operand);
      }
    }
    return value;
  }
}
