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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.WaarpStringUtils;

import javax.management.RuntimeErrorException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class JdkLoggerTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final Exception e = new Exception();
  private static final PrintStream out = System.out;
  // NOSONAR since Logger test
  private static final StringBuilder buf = new StringBuilder();

  @BeforeClass
  public static void setUpBeforeClass() {
    try {
      System.setOut(new PrintStream(new OutputStream() {
        @Override
        public void write(final int b) {
          buf.append((char) b);
        }
      }, true, WaarpStringUtils.UTF_8));
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeErrorException(new Error(e));
    }
    e.setStackTrace(new StackTraceElement[] {
        new StackTraceElement("n1", "n2", "n3", 4)
    });
  }

  @AfterClass
  public static void tearDownAfterClass() {
    System.setOut(out);
  }

  @Test
  public void testCreation() {
    // Note: partially automatically generated
    new WaarpJdkLoggerFactory(WaarpLogLevel.TRACE);
    Logger logger0 = Logger.getLogger("'a([(j92O]Xnk>*F) ");
    WaarpJdkLogger jdkLogger0 = new WaarpJdkLogger(logger0);
    jdkLogger0.debug("'a([(j92O]Xnk>*F) ", "'a([(j92O]Xnk>*F) ", jdkLogger0);
    assertEquals("'a([(j92O]Xnk>*F) ", jdkLogger0.name());
    logger0 = Logger.getLogger("sun.reflect.NativeMethodAccessorImpl");
    jdkLogger0 = new WaarpJdkLogger(logger0);
    final WaarpLogLevel WaarpLogLevel0 = WaarpLogLevel.TRACE;
    jdkLogger0.log(WaarpLogLevel0, "", jdkLogger0);
    assertEquals("sun.reflect.NativeMethodAccessorImpl", jdkLogger0.name());
    logger0 = Logger.getGlobal();
    jdkLogger0 = new WaarpJdkLogger(logger0);
    boolean boolean0 = jdkLogger0.isInfoEnabled();
    logger0 = Logger.getLogger("sun.reflect.NativeMethodAccessorImpl");
    jdkLogger0 = new WaarpJdkLogger(logger0);
    Exception exception0 =
        new Exception("sun.reflect.NativeMethodAccessorImpl");
    exception0.setStackTrace(new StackTraceElement[] {
        new StackTraceElement("n1", "n2", "n3", 4)
    });
    jdkLogger0.error("sun.reflect.NativeMethodAccessorImpl", exception0);
    assertEquals("sun.reflect.NativeMethodAccessorImpl", jdkLogger0.name());
    logger0 = Logger.getGlobal();
    jdkLogger0 = new WaarpJdkLogger(logger0);
    Object[] objectArray0 = new Object[0];
    jdkLogger0.error("", objectArray0);
    assertEquals("global", jdkLogger0.name());
    logger0 = Logger.getGlobal();
    jdkLogger0 = new WaarpJdkLogger(logger0);
    jdkLogger0
        .error("k'n(W*WnB(ip7/: ", "sun.reflect.GeneratedMethodAccessor11",
               "k'n(W*WnB(ip7/: ");
    assertEquals("global", jdkLogger0.name());
    logger0 = Logger.getGlobal();
    jdkLogger0 = new WaarpJdkLogger(logger0);
    jdkLogger0.error("sun.reflect.GeneratedMethodAccessor39",
                     "sun.reflect.GeneratedMethodAccessor39");
    assertEquals("global", jdkLogger0.name());
    logger0 = Logger.getGlobal();
    jdkLogger0 = new WaarpJdkLogger(logger0);
    jdkLogger0.error("DEBUG");
    assertEquals("global", jdkLogger0.name());
    logger0 = Logger.getAnonymousLogger();
    Logger logger1 = logger0.getParent();
    jdkLogger0 = new WaarpJdkLogger(logger1);
    exception0 = new Exception("g.6+Eh`xw;&+MV(z");
    exception0.setStackTrace(new StackTraceElement[] {
        new StackTraceElement("n1", "n2", "n3", 4)
    });
    jdkLogger0.warn("g.6+Eh`xw;&+MV(z", exception0);
    assertEquals("g.6+Eh`xw;&+MV(z", exception0.getMessage());
    logger0 = Logger.getGlobal();
    jdkLogger0 = new WaarpJdkLogger(logger0);
    objectArray0 = new Object[1];
    jdkLogger0.warn("DEBUG", objectArray0);
    assertEquals("global", jdkLogger0.name());
    logger0 = Logger.getGlobal();
    jdkLogger0 = new WaarpJdkLogger(logger0);
    jdkLogger0.warn("", "");
    assertEquals("global", jdkLogger0.name());
    logger0 = Logger.getGlobal();
    jdkLogger0 = new WaarpJdkLogger(logger0);
    jdkLogger0.warn("P!1G14s4`");
    assertEquals("global", jdkLogger0.name());
    logger0 = Logger.getGlobal();
    jdkLogger0 = new WaarpJdkLogger(logger0);
    exception0 = new Exception("y&]>", null);
    exception0.setStackTrace(new StackTraceElement[] {
        new StackTraceElement("n1", "n2", "n3", 4)
    });
    jdkLogger0.info(exception0);
    assertEquals("global", jdkLogger0.name());
    logger0 = Logger.getGlobal();
    jdkLogger0 = new WaarpJdkLogger(logger0);
    objectArray0 = new Object[4];
    jdkLogger0.info("g", objectArray0);
    assertEquals("global", jdkLogger0.name());
    logger0 = Logger.getGlobal();
    jdkLogger0 = new WaarpJdkLogger(logger0);
    final Object object0 = new Object();
    jdkLogger0.info("5!F H", object0, jdkLogger0);
    assertEquals("global", jdkLogger0.name());
    logger0 = Logger.getAnonymousLogger();
    logger1 = logger0.getParent();
    jdkLogger0 = new WaarpJdkLogger(logger1);
    jdkLogger0.info("", "");
    assertEquals("", jdkLogger0.name());
    logger0 = Logger.getAnonymousLogger();
    logger1 = logger0.getParent();
    jdkLogger0 = new WaarpJdkLogger(logger1);
    jdkLogger0.info("TB3RPuq#i");
    assertEquals("", jdkLogger0.name());
    logger0 = Logger.getGlobal();
    jdkLogger0 = new WaarpJdkLogger(logger0);
    exception0 = new Exception();
    exception0.setStackTrace(new StackTraceElement[] {
        new StackTraceElement("n1", "n2", "n3", 4)
    });
    jdkLogger0.debug("", exception0);
    assertEquals("java.lang.Exception", exception0.toString());
    logger0 = Logger.getAnonymousLogger();
    logger1 = logger0.getParent();
    jdkLogger0 = new WaarpJdkLogger(logger1);
    objectArray0 = new Object[5];
    jdkLogger0.debug("", objectArray0);
    assertEquals("", jdkLogger0.name());
    logger0 = Logger.getLogger("detectLoggingBaseLevel");
    jdkLogger0 = new WaarpJdkLogger(logger0);
    jdkLogger0.debug("", "detectLoggingBaseLevel");
    assertEquals("detectLoggingBaseLevel", jdkLogger0.name());
    logger0 = Logger.getGlobal();
    jdkLogger0 = new WaarpJdkLogger(logger0);
    jdkLogger0.debug("#");
    assertEquals("global", jdkLogger0.name());
    logger0 = Logger.getAnonymousLogger();
    logger1 = logger0.getParent();
    jdkLogger0 = new WaarpJdkLogger(logger1);
    exception0 = new Exception((Throwable) null);
    exception0.setStackTrace(new StackTraceElement[] {
        new StackTraceElement("n1", "n2", "n3", 4)
    });
    jdkLogger0.trace("null", exception0);
    assertEquals("", jdkLogger0.name());
    logger0 = Logger.getLogger("detectLoggingBaseLevel");
    jdkLogger0 = new WaarpJdkLogger(logger0);
    objectArray0 = new Object[2];
    jdkLogger0.trace("detectLoggingBaseLevel", objectArray0);
    assertEquals("detectLoggingBaseLevel", jdkLogger0.name());
    logger0 = Logger.getAnonymousLogger();
    logger1 = logger0.getParent();
    jdkLogger0 = new WaarpJdkLogger(logger1);
    exception0 = new Exception();
    final StackTraceElement[] stackTraceElementArray0 =
        exception0.getStackTrace();
    jdkLogger0.trace("};Dg`Me}$_cLd}O(}$", (Object[]) stackTraceElementArray0);
    assertEquals("", jdkLogger0.name());
    logger0 = Logger.getAnonymousLogger();
    logger1 = logger0.getParent();
    jdkLogger0 = new WaarpJdkLogger(logger1);
    exception0 = new Exception("g.6+Eh`xw;&+MV(z");
    exception0.setStackTrace(new StackTraceElement[] {
        new StackTraceElement("n1", "n2", "n3", 4)
    });
    jdkLogger0.trace("g.6+Eh`xw;&+MV(z", jdkLogger0, exception0);
    assertEquals("java.lang.Exception: g.6+Eh`xw;&+MV(z",
                 exception0.toString());
    logger0 = Logger.getLogger("");
    jdkLogger0 = new WaarpJdkLogger(logger0);
    jdkLogger0.trace("", jdkLogger0);
    assertEquals("", jdkLogger0.name());
    logger0 = Logger.getAnonymousLogger();
    logger1 = logger0.getParent();
    jdkLogger0 = new WaarpJdkLogger(logger1);
    jdkLogger0.trace("g.6+Eh`xw;&+MV(z");
    assertEquals("", jdkLogger0.name());
    logger0 = Logger.getGlobal();
    jdkLogger0 = new WaarpJdkLogger(logger0);
    jdkLogger0.warn("", jdkLogger0, "");
    assertEquals("global", jdkLogger0.name());
    logger0 = Logger.getAnonymousLogger();
    logger1 = logger0.getParent();
    jdkLogger0 = new WaarpJdkLogger(logger1);
    boolean0 = jdkLogger0.isWarnEnabled();
    assertTrue(boolean0);
    logger0 = Logger.getAnonymousLogger();
    logger1 = logger0.getParent();
    jdkLogger0 = new WaarpJdkLogger(logger1);
    boolean0 = jdkLogger0.isErrorEnabled();
    assertTrue(boolean0);
    logger0 = Logger.getLogger("");
    jdkLogger0 = new WaarpJdkLogger(logger0);
    exception0 = new Exception();
    exception0.setStackTrace(new StackTraceElement[] {
        new StackTraceElement("n1", "n2", "n3", 4)
    });
    jdkLogger0.info("v>S.;58(1\"", exception0);
    assertEquals("java.lang.Exception", exception0.toString());
  }

  @Test
  public void testIsInfoEnabled() {
    WaarpLoggerFactory
        .setDefaultFactory(new WaarpJdkLoggerFactory(WaarpLogLevel.INFO));
    final WaarpLogger logger = WaarpLoggerFactory.getInstance("foo3");
    assertTrue(logger.isInfoEnabled());
    buf.setLength(0);
    logger.info("a");
    buf.setLength(0);
    logger.info("a", e);
    buf.setLength(0);
    logger.info("", new Object());
    logger.info("", new Object(), new Object());
    logger.info("", new Object(), new Object(), new Object());
    buf.setLength(0);
  }

  @Test
  public void testIsWarnEnabled() {
    WaarpLoggerFactory
        .setDefaultFactory(new WaarpJdkLoggerFactory(WaarpLogLevel.WARN));
    final WaarpLogger logger = WaarpLoggerFactory.getInstance("foo4");
    assertTrue(logger.isWarnEnabled());
    buf.setLength(0);
    logger.warn("a");
    buf.setLength(0);
    logger.warn("a", e);
    buf.setLength(0);
    logger.warn("", new Object());
    logger.warn("", new Object(), new Object());
    logger.warn("", new Object(), new Object(), new Object());
    buf.setLength(0);
  }

  @Test
  public void testIsErrorEnabled() {
    WaarpLoggerFactory
        .setDefaultFactory(new WaarpJdkLoggerFactory(WaarpLogLevel.ERROR));
    final WaarpLogger logger = WaarpLoggerFactory.getInstance("foo5");
    assertTrue(logger.isErrorEnabled());
    buf.setLength(0);
    logger.error("a");
    buf.setLength(0);
    logger.error("a", e);
    buf.setLength(0);
    logger.error("", new Object());
    logger.error("", new Object(), new Object());
    logger.error("", new Object(), new Object(), new Object());
    buf.setLength(0);
  }

}
