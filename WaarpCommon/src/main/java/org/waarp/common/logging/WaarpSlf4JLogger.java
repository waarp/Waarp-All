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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

/**
 * logger using SLF4J from LOGBACK
 */
public class WaarpSlf4JLogger extends AbstractWaarpLogger {
  private static final long serialVersionUID = -7588688826950608830L;
  /**
   * Internal logger
   */
  private final Logger logger;

  /**
   * @param logger
   */
  public WaarpSlf4JLogger(final Logger logger) {
    super(logger.getName());
    this.logger = logger;
  }

  @Override
  public final void setLevel(final WaarpLogLevel level) {
    switch (level) {
      case TRACE:
        logger.setLevel(Level.TRACE);
        break;
      case DEBUG:
        logger.setLevel(Level.DEBUG);
        break;
      case INFO:
        logger.setLevel(Level.INFO);
        break;
      case WARN:
        logger.setLevel(Level.WARN);
        break;
      case ERROR:
        logger.setLevel(Level.ERROR);
        break;
      case NONE:
        logger.setLevel(Level.OFF);
        break;
    }
  }

  @Override
  public final boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public final void junit(final int callee, final String msg) {
    logger.warn(getLoggerMethodAndLineCallee(callee + 1) + msg);
  }

  @Override
  public final void trace(final String msg) {
    if (logger.isTraceEnabled()) {
      logger.trace(getLoggerMethodAndLine() + msg);
    }
  }

  @Override
  public final void trace(final String format, final Object arg) {
    if (logger.isTraceEnabled()) {
      logger.trace(getLoggerMethodAndLine() + format, arg);
    }
  }

  @Override
  public final void trace(final String format, final Object argA,
                          final Object argB) {
    if (logger.isTraceEnabled()) {
      logger.trace(getLoggerMethodAndLine() + format, argA, argB);
    }
  }

  @Override
  public final void trace(final String format, final Object... argArray) {
    if (logger.isTraceEnabled()) {
      logger.trace(getLoggerMethodAndLine() + format, argArray);
    }
  }

  @Override
  public final void trace(final String msg, final Throwable t) {
    if (logger.isTraceEnabled()) {
      logger.trace(getLoggerMethodAndLine() + msg, t);
    }
  }

  @Override
  public final boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public final void debug(final String msg) {
    if (logger.isDebugEnabled()) {
      logger.debug(getLoggerMethodAndLine() + msg);
    }
  }

  @Override
  public final void debug(final String format, final Object arg) {
    if (logger.isDebugEnabled()) {
      logger.debug(getLoggerMethodAndLine() + format, arg);
    }
  }

  @Override
  public final void debug(final String format, final Object argA,
                          final Object argB) {
    if (logger.isDebugEnabled()) {
      logger.debug(getLoggerMethodAndLine() + format, argA, argB);
    }
  }

  @Override
  public final void debug(final String format, final Object... argArray) {
    if (logger.isDebugEnabled()) {
      logger.debug(getLoggerMethodAndLine() + format, argArray);
    }
  }

  @Override
  public final void debug(final String msg, final Throwable t) {
    if (logger.isDebugEnabled()) {
      logger.debug(getLoggerMethodAndLine() + msg, t);
    }
  }

  @Override
  public final boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  @Override
  public final void info(final String msg) {
    if (logger.isInfoEnabled()) {
      logger.info(getLoggerMethodAndLine() + msg);
    }
  }

  @Override
  public final void info(final String format, final Object arg) {
    if (logger.isInfoEnabled()) {
      logger.info(getLoggerMethodAndLine() + format, arg);
    }
  }

  @Override
  public final void info(final String format, final Object argA,
                         final Object argB) {
    if (logger.isInfoEnabled()) {
      logger.info(getLoggerMethodAndLine() + format, argA, argB);
    }
  }

  @Override
  public final void info(final String format, final Object... argArray) {
    if (logger.isInfoEnabled()) {
      logger.info(getLoggerMethodAndLine() + format, argArray);
    }
  }

  @Override
  public final void info(final String msg, final Throwable t) {
    if (logger.isInfoEnabled()) {
      logger.info(getLoggerMethodAndLine() + msg, t);
    }
  }

  @Override
  public final boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  @Override
  public final void warn(final String msg) {
    if (logger.isWarnEnabled()) {
      logger.warn(getLoggerMethodAndLine() + msg);
    }
  }

  @Override
  public final void warn(final String format, final Object arg) {
    if (logger.isWarnEnabled()) {
      logger.warn(getLoggerMethodAndLine() + format, arg);
    }
  }

  @Override
  public final void warn(final String format, final Object... argArray) {
    if (logger.isWarnEnabled()) {
      logger.warn(getLoggerMethodAndLine() + format, argArray);
    }
  }

  @Override
  public final void warn(final String format, final Object argA,
                         final Object argB) {
    if (logger.isWarnEnabled()) {
      logger.warn(getLoggerMethodAndLine() + format, argA, argB);
    }
  }

  @Override
  public final void warn(final String msg, final Throwable t) {
    if (logger.isWarnEnabled()) {
      logger.warn(getLoggerMethodAndLine() + msg, t);
    }
  }

  @Override
  public final boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  @Override
  public final void error(final String msg) {
    logger.error(getLoggerMethodAndLine() + msg);
  }

  @Override
  public final void error(final String format, final Object arg) {
    logger.error(getLoggerMethodAndLine() + format, arg);
  }

  @Override
  public final void error(final String format, final Object argA,
                          final Object argB) {
    logger.error(getLoggerMethodAndLine() + format, argA, argB);
  }

  @Override
  public final void error(final String format, final Object... argArray) {
    logger.error(getLoggerMethodAndLine() + format, argArray);
  }

  @Override
  public final void error(final String msg, final Throwable t) {
    logger.error(getLoggerMethodAndLine() + msg, t);
  }
}
