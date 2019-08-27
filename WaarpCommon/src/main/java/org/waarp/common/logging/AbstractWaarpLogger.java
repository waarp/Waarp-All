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
package org.waarp.common.logging;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * A skeletal implementation of {@link WaarpLogger}. This class implements all
 * methods that have a
 * {@link WaarpLogLevel} parameter by default to call specific logger methods
 * such as {@link #info(String)} or
 * {@link #isInfoEnabled()}.
 */
public abstract class AbstractWaarpLogger implements WaarpLogger, Serializable {

  private static final long serialVersionUID = -6382972526573193470L;

  private static final String EXCEPTION_MESSAGE = "Unexpected exception:";

  private final String name;

  /**
   * Creates a new instance.
   */
  protected AbstractWaarpLogger(final String name) {
    if (name == null) {
      throw new NullPointerException("name");
    }
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public boolean isEnabled(final WaarpLogLevel level) {
    switch (level) {
      case TRACE:
        return isTraceEnabled();
      case DEBUG:
        return isDebugEnabled();
      case INFO:
        return isInfoEnabled();
      case WARN:
        return isWarnEnabled();
      case ERROR:
        return isErrorEnabled();
      default:
        return true;
    }
  }

  @Override
  public void trace(final Throwable t) {
    trace(EXCEPTION_MESSAGE, t);
  }

  @Override
  public void debug(final Throwable t) {
    debug(EXCEPTION_MESSAGE, t);
  }

  @Override
  public void info(final Throwable t) {
    info(EXCEPTION_MESSAGE, t);
  }

  @Override
  public void warn(final Throwable t) {
    warn(EXCEPTION_MESSAGE, t);
  }

  @Override
  public void error(final Throwable t) {
    error(EXCEPTION_MESSAGE, t);
  }

  @Override
  public void log(final WaarpLogLevel level, final String msg,
                  final Throwable cause) {
    switch (level) {
      case TRACE:
        trace(msg, cause);
        break;
      case DEBUG:
        debug(msg, cause);
        break;
      case INFO:
        info(msg, cause);
        break;
      case WARN:
        warn(msg, cause);
        break;
      case ERROR:
      default:
        error(msg, cause);
        break;
    }
  }

  @Override
  public void log(final WaarpLogLevel level, final Throwable cause) {
    switch (level) {
      case TRACE:
        trace(cause);
        break;
      case DEBUG:
        debug(cause);
        break;
      case INFO:
        info(cause);
        break;
      case WARN:
        warn(cause);
        break;
      case ERROR:
      default:
        error(cause);
        break;
    }
  }

  @Override
  public void log(final WaarpLogLevel level, final String msg) {
    switch (level) {
      case TRACE:
        trace(msg);
        break;
      case DEBUG:
        debug(msg);
        break;
      case INFO:
        info(msg);
        break;
      case WARN:
        warn(msg);
        break;
      case ERROR:
      default:
        error(msg);
        break;
    }
  }

  @Override
  public void log(final WaarpLogLevel level, final String format,
                  final Object arg) {
    switch (level) {
      case TRACE:
        trace(format, arg);
        break;
      case DEBUG:
        debug(format, arg);
        break;
      case INFO:
        info(format, arg);
        break;
      case WARN:
        warn(format, arg);
        break;
      case ERROR:
      default:
        error(format, arg);
        break;
    }
  }

  @Override
  public void log(final WaarpLogLevel level, final String format,
                  final Object argA, final Object argB) {
    switch (level) {
      case TRACE:
        trace(format, argA, argB);
        break;
      case DEBUG:
        debug(format, argA, argB);
        break;
      case INFO:
        info(format, argA, argB);
        break;
      case WARN:
        warn(format, argA, argB);
        break;
      case ERROR:
      default:
        error(format, argA, argB);
        break;
    }
  }

  @Override
  public void log(final WaarpLogLevel level, final String format,
                  final Object... arguments) {
    switch (level) {
      case TRACE:
        trace(format, arguments);
        break;
      case DEBUG:
        debug(format, arguments);
        break;
      case INFO:
        info(format, arguments);
        break;
      case WARN:
        warn(format, arguments);
        break;
      case ERROR:
      default:
        error(format, arguments);
        break;
    }
  }

  protected Object readResolve() throws ObjectStreamException {
    return WaarpLoggerFactory.getInstance(name());
  }

  /**
   * @param o
   *
   * @return the simple Class Name
   */
  public static final String simpleClassName(final Object o) {
    if (o == null) {
      return "null_object";
    } else {
      return simpleClassName(o.getClass());
    }
  }

  /**
   * @param clazz
   *
   * @return the simple Class Name
   */
  public static final String simpleClassName(final Class<?> clazz) {
    if (clazz == null) {
      return "null_class";
    }
    final Package pkg = clazz.getPackage();
    if (pkg != null) {
      return clazz.getName().substring(pkg.getName().length() + 1);
    } else {
      return clazz.getName();
    }
  }

  @Override
  public String toString() {
    return simpleClassName(this) + '(' + name() + ')';
  }

  private static final int BASELEVEL;
  private static int LOGLEVEL;

  /**
   * Determine the good level
   *
   * @return the default base level
   */
  private static int detectLoggingBaseLevel() {
    final StackTraceElement[] elt = Thread.currentThread().getStackTrace();
    int i;
    for (i = 0; i < elt.length; i++) {
      if ("detectLoggingBaseLevel".equalsIgnoreCase(elt[i].getMethodName())) {
        break;
      }
    }
    return i;
  }

  static {
    BASELEVEL = detectLoggingBaseLevel();
    LOGLEVEL = BASELEVEL + 2;
  }

  /**
   * To be used in message for logger (rank 2) like
   * logger.warn(code,"message:"+getImmediateMethodAndLine(),null);
   *
   * @return "ClassAndMethodName(FileName:LineNumber)"
   */
  public static final String getImmediateMethodAndLine() {
    final StackTraceElement elt =
        Thread.currentThread().getStackTrace()[BASELEVEL + 1];
    return getMethodAndLine(elt);
  }

  // FIXME TODO for JDK6 IBM add 1 (2->3 and 3->4)

  /**
   * To be used only by Logger (rank 5)
   *
   * @return "MethodName(FileName:LineNumber)"
   */
  public static final String getLoggerMethodAndLine() {
    final StackTraceElement elt =
        Thread.currentThread().getStackTrace()[LOGLEVEL];
    return getMethodAndLine(elt);
  }

  /**
   * @param rank is the current depth of call+1 (immediate = 1+1=2)
   *
   * @return "ClassAndMethodName(FileName:LineNumber)"
   */
  protected static final String getRankMethodAndLine(final int rank) {
    final StackTraceElement elt = Thread.currentThread().getStackTrace()[rank];
    return getMethodAndLine(elt);
  }


  /**
   * @param elt
   *
   * @return "MethodName(FileName:LineNumber) " from elt
   */
  private static String getMethodAndLine(final StackTraceElement elt) {
    final StringBuilder builder =
        new StringBuilder(elt.getClassName()).append('.')
                                             .append(elt.getMethodName())
                                             .append('(')
                                             .append(elt.getFileName())
                                             .append(':')
                                             .append(elt.getLineNumber())
                                             .append(") : ");
    if (WaarpLoggerFactory.localName != null) {
      builder.append('[').append(WaarpLoggerFactory.localName).append("] ");
    }
    return builder.toString();
  }

}
