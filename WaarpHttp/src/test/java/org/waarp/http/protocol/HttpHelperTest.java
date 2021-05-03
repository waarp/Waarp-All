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

package org.waarp.http.protocol;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.TestWatcherJunit4;

import static org.junit.Assert.*;

/**
 * HttpHelper Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>nov. 23, 2019</pre>
 */
public class HttpHelperTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  @BeforeClass
  public static void beforeClass() {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
  }

  @Before
  public void before() throws Exception {
  }

  @After
  public void after() throws Exception {
  }

  /**
   * Method: toLong(String value, long def)
   */
  @Test
  public void testToLong() throws Exception {
    assertEquals(1234567890123L, HttpHelper.toLong("1234567890123", 0));
    assertEquals(-1234567890123L, HttpHelper.toLong("-1234567890123", 0));
    assertEquals(0, HttpHelper.toLong("1a", 0));
    assertEquals(0, HttpHelper.toLong("a", 0));
  }

  /**
   * Method: toInt(String value, int def)
   */
  @Test
  public void testToInt() throws Exception {
    assertEquals(1, HttpHelper.toInt("1", 0));
    assertEquals(-1, HttpHelper.toInt("-1", 0));
    assertEquals(0, HttpHelper.toInt("1a", 0));
    assertEquals(0, HttpHelper.toInt("a", 0));
  }


} 
