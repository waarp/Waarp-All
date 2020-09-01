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
package org.waarp.common.exception;

/**
 * Exception for Finite State Machine support
 */
public class IllegalFiniteStateException extends Exception {
  /**
   *
   */
  private static final long serialVersionUID = 8731284958857363751L;

  /**
   *
   */
  public IllegalFiniteStateException() {
  }

  /**
   * @param message
   * @param cause
   */
  public IllegalFiniteStateException(final String message,
                                     final Throwable cause) {
    super(message, cause);
  }

  /**
   * @param s
   */
  public IllegalFiniteStateException(final String s) {
    super(s);
  }

  /**
   * @param cause
   */
  public IllegalFiniteStateException(final Throwable cause) {
    super(cause);
  }

}
