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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.utility.TestWatcherJunit4;

import static org.junit.Assert.*;

public class JdkLoggerFactoryTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();


  @Test
  public void testCreation() {
    final WaarpLogger logger =
        new WaarpJdkLoggerFactory(null).newInstance("foo");
    assertTrue(logger instanceof WaarpJdkLogger);
    assertEquals("foo", logger.name());
    assertEquals(WaarpLogLevel.DEBUG, new WaarpJdkLoggerFactory(
        WaarpLogLevel.DEBUG).getLevelSpecific());
    assertEquals(WaarpLogLevel.ERROR, new WaarpJdkLoggerFactory(
        WaarpLogLevel.ERROR).getLevelSpecific());
    assertEquals(WaarpLogLevel.INFO, new WaarpJdkLoggerFactory(
        WaarpLogLevel.INFO).getLevelSpecific());
    assertEquals(WaarpLogLevel.TRACE, new WaarpJdkLoggerFactory(
        WaarpLogLevel.TRACE).getLevelSpecific());
    assertEquals(WaarpLogLevel.WARN, new WaarpJdkLoggerFactory(
        WaarpLogLevel.WARN).getLevelSpecific());
  }
}
