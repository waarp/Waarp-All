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

/*
 * Copyright 2012 The Netty Project
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
/**
 * Copyright (c) 2004-2011 QOS.ch
 * All rights reserved.
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS  IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.waarp.common.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * <a href="http://java.sun.com/javase/6/docs/technotes/guides/logging/index.html">java.util.logging</a>
 * logger.
 */
public class WaarpJdkLogger extends AbstractWaarpLogger {

  private static final long serialVersionUID = -1767272577989225979L;

  final transient Logger logger;

  WaarpJdkLogger(final Logger logger) {
    super(logger.getName());
    this.logger = logger;
  }

  @Override
  public final void setLevel(final WaarpLogLevel level) {
    switch (level) {
      case TRACE:
        logger.setLevel(Level.FINEST);
        break;
      case DEBUG:
        logger.setLevel(Level.FINE);
        break;
      case INFO:
        logger.setLevel(Level.INFO);
        break;
      case WARN:
        logger.setLevel(Level.WARNING);
        break;
      case ERROR:
        logger.setLevel(Level.SEVERE);
        break;
      case NONE:
        logger.setLevel(Level.OFF);
        break;
    }
  }

  /**
   * Is this logger instance enabled for the FINEST level?
   *
   * @return True if this Logger is enabled for level FINEST, false otherwise.
   */
  @Override
  public final boolean isTraceEnabled() {
    return logger.isLoggable(Level.FINEST);
  }

  @Override
  public final void junit(final int callee, final String msg) {
    logger.warning(msg);
  }

  /**
   * Log a message object at level FINEST.
   *
   * @param msg - the message object to be logged
   */
  @Override
  public final void trace(final String msg) {
    if (logger.isLoggable(Level.FINEST)) {
      log(SELF, Level.FINEST, msg, null);
    }
  }

  /**
   * Log a message at level FINEST according to the specified format and
   * argument.
   *
   * <p>
   * This form avoids superfluous object creation when the logger is disabled
   * for level FINEST.
   * </p>
   *
   * @param format the format string
   * @param arg the argument
   */
  @Override
  public final void trace(final String format, final Object arg) {
    if (logger.isLoggable(Level.FINEST)) {
      final FormattingTuple ft = MessageFormatter.format(format, arg);
      log(SELF, Level.FINEST, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
   * Log a message at level FINEST according to the specified format and
   * arguments.
   *
   * <p>
   * This form avoids superfluous object creation when the logger is disabled
   * for the FINEST level.
   * </p>
   *
   * @param format the format string
   * @param argA the first argument
   * @param argB the second argument
   */
  @Override
  public final void trace(final String format, final Object argA,
                          final Object argB) {
    if (logger.isLoggable(Level.FINEST)) {
      final FormattingTuple ft = MessageFormatter.format(format, argA, argB);
      log(SELF, Level.FINEST, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
   * Log a message at level FINEST according to the specified format and
   * arguments.
   *
   * <p>
   * This form avoids superfluous object creation when the logger is disabled
   * for the FINEST level.
   * </p>
   *
   * @param format the format string
   * @param argArray an array of arguments
   */
  @Override
  public final void trace(final String format, final Object... argArray) {
    if (logger.isLoggable(Level.FINEST)) {
      final FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
      log(SELF, Level.FINEST, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
   * Log an exception (throwable) at level FINEST with an accompanying
   * message.
   *
   * @param msg the message accompanying the exception
   * @param t the exception (throwable) to log
   */
  @Override
  public final void trace(final String msg, final Throwable t) {
    if (logger.isLoggable(Level.FINEST)) {
      log(SELF, Level.FINEST, msg, t);
    }
  }

  /**
   * Is this logger instance enabled for the FINE level?
   *
   * @return True if this Logger is enabled for level FINE, false otherwise.
   */
  @Override
  public final boolean isDebugEnabled() {
    return logger.isLoggable(Level.FINE);
  }

  /**
   * Log a message object at level FINE.
   *
   * @param msg - the message object to be logged
   */
  @Override
  public final void debug(final String msg) {
    if (logger.isLoggable(Level.FINE)) {
      log(SELF, Level.FINE, msg, null);
    }
  }

  /**
   * Log a message at level FINE according to the specified format and
   * argument.
   *
   * <p>
   * This form avoids superfluous object creation when the logger is disabled
   * for level FINE.
   * </p>
   *
   * @param format the format string
   * @param arg the argument
   */
  @Override
  public final void debug(final String format, final Object arg) {
    if (logger.isLoggable(Level.FINE)) {
      final FormattingTuple ft = MessageFormatter.format(format, arg);
      log(SELF, Level.FINE, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
   * Log a message at level FINE according to the specified format and
   * arguments.
   *
   * <p>
   * This form avoids superfluous object creation when the logger is disabled
   * for the FINE level.
   * </p>
   *
   * @param format the format string
   * @param argA the first argument
   * @param argB the second argument
   */
  @Override
  public final void debug(final String format, final Object argA,
                          final Object argB) {
    if (logger.isLoggable(Level.FINE)) {
      final FormattingTuple ft = MessageFormatter.format(format, argA, argB);
      log(SELF, Level.FINE, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
   * Log a message at level FINE according to the specified format and
   * arguments.
   *
   * <p>
   * This form avoids superfluous object creation when the logger is disabled
   * for the FINE level.
   * </p>
   *
   * @param format the format string
   * @param argArray an array of arguments
   */
  @Override
  public final void debug(final String format, final Object... argArray) {
    if (logger.isLoggable(Level.FINE)) {
      final FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
      log(SELF, Level.FINE, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
   * Log an exception (throwable) at level FINE with an accompanying message.
   *
   * @param msg the message accompanying the exception
   * @param t the exception (throwable) to log
   */
  @Override
  public final void debug(final String msg, final Throwable t) {
    if (logger.isLoggable(Level.FINE)) {
      log(SELF, Level.FINE, msg, t);
    }
  }

  /**
   * Is this logger instance enabled for the INFO level?
   *
   * @return True if this Logger is enabled for the INFO level, false
   *     otherwise.
   */
  @Override
  public final boolean isInfoEnabled() {
    return logger.isLoggable(Level.INFO);
  }

  /**
   * Log a message object at the INFO level.
   *
   * @param msg - the message object to be logged
   */
  @Override
  public final void info(final String msg) {
    if (logger.isLoggable(Level.INFO)) {
      log(SELF, Level.INFO, msg, null);
    }
  }

  /**
   * Log a message at level INFO according to the specified format and
   * argument.
   *
   * <p>
   * This form avoids superfluous object creation when the logger is disabled
   * for the INFO level.
   * </p>
   *
   * @param format the format string
   * @param arg the argument
   */
  @Override
  public final void info(final String format, final Object arg) {
    if (logger.isLoggable(Level.INFO)) {
      final FormattingTuple ft = MessageFormatter.format(format, arg);
      log(SELF, Level.INFO, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
   * Log a message at the INFO level according to the specified format and
   * arguments.
   *
   * <p>
   * This form avoids superfluous object creation when the logger is disabled
   * for the INFO level.
   * </p>
   *
   * @param format the format string
   * @param argA the first argument
   * @param argB the second argument
   */
  @Override
  public final void info(final String format, final Object argA,
                         final Object argB) {
    if (logger.isLoggable(Level.INFO)) {
      final FormattingTuple ft = MessageFormatter.format(format, argA, argB);
      log(SELF, Level.INFO, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
   * Log a message at level INFO according to the specified format and
   * arguments.
   *
   * <p>
   * This form avoids superfluous object creation when the logger is disabled
   * for the INFO level.
   * </p>
   *
   * @param format the format string
   * @param argArray an array of arguments
   */
  @Override
  public final void info(final String format, final Object... argArray) {
    if (logger.isLoggable(Level.INFO)) {
      final FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
      log(SELF, Level.INFO, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
   * Log an exception (throwable) at the INFO level with an accompanying
   * message.
   *
   * @param msg the message accompanying the exception
   * @param t the exception (throwable) to log
   */
  @Override
  public final void info(final String msg, final Throwable t) {
    if (logger.isLoggable(Level.INFO)) {
      log(SELF, Level.INFO, msg, t);
    }
  }

  /**
   * Is this logger instance enabled for the WARNING level?
   *
   * @return True if this Logger is enabled for the WARNING level, false
   *     otherwise.
   */
  @Override
  public final boolean isWarnEnabled() {
    return logger.isLoggable(Level.WARNING);
  }

  /**
   * Log a message object at the WARNING level.
   *
   * @param msg - the message object to be logged
   */
  @Override
  public final void warn(final String msg) {
    if (logger.isLoggable(Level.WARNING)) {
      log(SELF, Level.WARNING, msg, null);
    }
  }

  /**
   * Log a message at the WARNING level according to the specified format and
   * argument.
   *
   * <p>
   * This form avoids superfluous object creation when the logger is disabled
   * for the WARNING level.
   * </p>
   *
   * @param format the format string
   * @param arg the argument
   */
  @Override
  public final void warn(final String format, final Object arg) {
    if (logger.isLoggable(Level.WARNING)) {
      final FormattingTuple ft = MessageFormatter.format(format, arg);
      log(SELF, Level.WARNING, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
   * Log a message at the WARNING level according to the specified format and
   * arguments.
   *
   * <p>
   * This form avoids superfluous object creation when the logger is disabled
   * for the WARNING level.
   * </p>
   *
   * @param format the format string
   * @param argA the first argument
   * @param argB the second argument
   */
  @Override
  public final void warn(final String format, final Object argA,
                         final Object argB) {
    if (logger.isLoggable(Level.WARNING)) {
      final FormattingTuple ft = MessageFormatter.format(format, argA, argB);
      log(SELF, Level.WARNING, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
   * Log a message at level WARNING according to the specified format and
   * arguments.
   *
   * <p>
   * This form avoids superfluous object creation when the logger is disabled
   * for the WARNING level.
   * </p>
   *
   * @param format the format string
   * @param argArray an array of arguments
   */
  @Override
  public final void warn(final String format, final Object... argArray) {
    if (logger.isLoggable(Level.WARNING)) {
      final FormattingTuple ft = MessageFormatter.arrayFormat(format, argArray);
      log(SELF, Level.WARNING, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
   * Log an exception (throwable) at the WARNING level with an accompanying
   * message.
   *
   * @param msg the message accompanying the exception
   * @param t the exception (throwable) to log
   */
  @Override
  public final void warn(final String msg, final Throwable t) {
    if (logger.isLoggable(Level.WARNING)) {
      log(SELF, Level.WARNING, msg, t);
    }
  }

  /**
   * Is this logger instance enabled for level SEVERE?
   *
   * @return True if this Logger is enabled for level SEVERE, false otherwise.
   */
  @Override
  public final boolean isErrorEnabled() {
    return logger.isLoggable(Level.SEVERE);
  }

  /**
   * Log a message object at the SEVERE level.
   *
   * @param msg - the message object to be logged
   */
  @Override
  public final void error(final String msg) {
    if (logger.isLoggable(Level.SEVERE)) {
      log(SELF, Level.SEVERE, msg, null);
    }
  }

  /**
   * Log a message at the SEVERE level according to the specified format and
   * argument.
   *
   * <p>
   * This form avoids superfluous object creation when the logger is disabled
   * for the SEVERE level.
   * </p>
   *
   * @param format the format string
   * @param arg the argument
   */
  @Override
  public final void error(final String format, final Object arg) {
    if (logger.isLoggable(Level.SEVERE)) {
      final FormattingTuple ft = MessageFormatter.format(format, arg);
      log(SELF, Level.SEVERE, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
   * Log a message at the SEVERE level according to the specified format and
   * arguments.
   *
   * <p>
   * This form avoids superfluous object creation when the logger is disabled
   * for the SEVERE level.
   * </p>
   *
   * @param format the format string
   * @param argA the first argument
   * @param argB the second argument
   */
  @Override
  public final void error(final String format, final Object argA,
                          final Object argB) {
    if (logger.isLoggable(Level.SEVERE)) {
      final FormattingTuple ft = MessageFormatter.format(format, argA, argB);
      log(SELF, Level.SEVERE, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
   * Log a message at level SEVERE according to the specified format and
   * arguments.
   *
   * <p>
   * This form avoids superfluous object creation when the logger is disabled
   * for the SEVERE level.
   * </p>
   *
   * @param format the format string
   * @param arguments an array of arguments
   */
  @Override
  public final void error(final String format, final Object... arguments) {
    if (logger.isLoggable(Level.SEVERE)) {
      final FormattingTuple ft =
          MessageFormatter.arrayFormat(format, arguments);
      log(SELF, Level.SEVERE, ft.getMessage(), ft.getThrowable());
    }
  }

  /**
   * Log an exception (throwable) at the SEVERE level with an accompanying
   * message.
   *
   * @param msg the message accompanying the exception
   * @param t the exception (throwable) to log
   */
  @Override
  public final void error(final String msg, final Throwable t) {
    if (logger.isLoggable(Level.SEVERE)) {
      log(SELF, Level.SEVERE, msg, t);
    }
  }

  /**
   * Log the message at the specified level with the specified throwable if
   * any.
   * This method creates a LogRecord
   * and fills in caller date before calling this instance's JDK14 logger.
   * <p>
   * See bug report #13 for more details.
   */
  private void log(final String callerFQCN, final Level level, final String msg,
                   final Throwable t) {
    // millis and thread are filled by the constructor
    final LogRecord record = new LogRecord(level, msg);
    record.setLoggerName(name());
    record.setThrown(t);
    fillCallerData(callerFQCN, record);
    logger.log(record);
  }

  static final String SELF = WaarpJdkLogger.class.getName();
  static final String SUPER = AbstractWaarpLogger.class.getName();

  /**
   * Fill in caller data if possible.
   *
   * @param record The record to update
   */
  private static void fillCallerData(final String callerFQCN,
                                     final LogRecord record) {
    final StackTraceElement[] steArray = new Throwable().getStackTrace();

    int selfIndex = -1;
    for (int i = 0; i < steArray.length; i++) {
      final String className = steArray[i].getClassName();
      if (className.equals(callerFQCN) || className.equals(SUPER)) {
        selfIndex = i;
        break;
      }
    }

    int found = -1;
    for (int i = selfIndex + 1; i < steArray.length; i++) {
      final String className = steArray[i].getClassName();
      if (!(className.equals(callerFQCN) || className.equals(SUPER))) {
        found = i;
        break;
      }
    }

    if (found != -1) {
      final StackTraceElement ste = steArray[found];
      // setting the class name has the side effect of setting
      // the needToInferCaller variable to false.
      record.setSourceClassName(ste.getClassName());
      record.setSourceMethodName(ste.getMethodName());
    }
  }
}
