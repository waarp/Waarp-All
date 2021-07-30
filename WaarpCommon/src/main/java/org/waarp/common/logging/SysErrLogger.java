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

package org.waarp.common.logging;

/**
 * Utility class to be used only in classes where WaarpLogger is not allowed
 */
public final class SysErrLogger {
  /**
   * FAKE LOGGER used where no LOG could be done
   */
  public static final SysErrLogger FAKE_LOGGER = new SysErrLogger();

  private SysErrLogger() {
    // Empty
  }

  /**
   * Utility method to log nothing
   * <p>
   * Used only in classes where WaarpLogger is not allowed
   *
   * @param throwable to log ignore
   */
  public final void ignoreLog(final Throwable throwable) {// NOSONAR
    // Nothing to do
  }

  /**
   * Utility method to log through System.out
   * <p>
   * Used only in classes where WaarpLogger is not allowed
   */
  public final void sysout() {
    System.out.println(); // NOSONAR
  }

  /**
   * Utility method to log through System.out
   * <p>
   * Used only in classes where WaarpLogger is not allowed
   *
   * @param message to write for no error log
   */
  public final void sysout(final Object message) {
    System.out.println(message); // NOSONAR
  }

  /**
   * Utility method to log through System.out
   *
   * @param format
   * @param args
   */
  public final void sysoutFormat(final String format, final Object... args) {
    System.out.format(format, args); // NOSONAR
  }

  /**
   * Utility method to log through System.err
   * <p>
   * Used only in classes where WaarpLogger is not allowed
   *
   * @param message to write for error
   */
  public final void syserrNoLn(final Object message) {
    System.err.print("ERROR " + message); // NOSONAR
  }

  /**
   * Utility method to log through System.err
   * <p>
   * Used only in classes where WaarpLogger is not allowed
   *
   * @param message to write for error
   */
  public final void syserr(final Object message) {
    System.err.println("ERROR " + message); // NOSONAR
  }

  /**
   * Utility method to log through System.err the current Stacktrace
   * <p>
   * Used only in classes where WaarpLogger is not allowed
   */
  public final void syserr() {
    new RuntimeException("ERROR Stacktrace").printStackTrace(); // NOSONAR
  }

  /**
   * Utility method to log through System.err the current Stacktrace
   * <p>
   * Used only in classes where WaarpLogger is not allowed
   *
   * @param message to write for error
   * @param e throw to write as error
   */
  public final void syserr(final String message, final Throwable e) {
    System.err.print("ERROR " + message + ": "); // NOSONAR
    e.printStackTrace(); // NOSONAR
  }

  /**
   * Utility method to log through System.err the current Stacktrace
   * <p>
   * Used only in classes where WaarpLogger is not allowed
   *
   * @param e throw to write as error
   */
  public final void syserr(final Throwable e) {
    System.err.print("ERROR: "); // NOSONAR
    e.printStackTrace(); // NOSONAR
  }
}
