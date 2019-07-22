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

/**
 * Checker for Parameters <br>
 * <br>
 * Can be used for String (testing also emptiness) and for general Object.<br>
 * For null String only, use the special method.
 */
public final class ParametersChecker {

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
  public static final void checkParameter(String errorMessage,
                                          String... parameters) {
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
  public static final void checkParameterDefault(String errorMessage,
                                                 String... parameters) {
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
  public static final boolean isNotEmpty(String... parameters) {
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
  public static final void checkParameterDefault(String errorMessage,
                                                 Object... parameters) {
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
  public static final void checkParameterNullOnly(String errorMessage,
                                                  String... parameters) {
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
  public static final void checkParameter(String errorMessage,
                                          Object... parameters) {
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
  public static final void checkValue(String name, long variable,
                                      long minValue) {
    if (variable < minValue) {
      throw new IllegalArgumentException(
          "Parameter " + name + " is less than " + minValue);
    }
  }
}