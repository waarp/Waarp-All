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
package org.waarp.common.command.exception;

import org.waarp.common.command.ReplyCode;

/**
 * Abstract class for exception in commands
 */
public abstract class CommandAbstractException extends Exception {
  /**
   *
   */
  private static final long serialVersionUID = 9118222235805967061L;

  /**
   * Associated code
   */
  public final ReplyCode code;

  /**
   * Associated Message if any
   */
  public final String message;

  /**
   * Simplest constructor
   *
   * @param code
   * @param message
   */
  protected CommandAbstractException(final ReplyCode code,
                                     final String message) {
    super(code.getMesg());
    this.code = code;
    this.message = message;
  }

  /**
   * Constructor with Throwable
   *
   * @param code
   * @param message
   * @param e
   */
  protected CommandAbstractException(final ReplyCode code, final String message,
                                     final Throwable e) {
    super(code.getMesg(), e);
    this.code = code;
    this.message = message;
  }

  /**
   *
   */
  @Override
  public final String toString() {
    return "Code: " + code.name() + " Mesg: " +
           (message != null? message : "no specific message");
  }

  @Override
  public final String getMessage() {
    return toString();
  }
}
