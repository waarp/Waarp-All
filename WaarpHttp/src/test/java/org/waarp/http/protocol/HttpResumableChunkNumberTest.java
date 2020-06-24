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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.utility.TestWatcherJunit4;

import static org.junit.Assert.*;

/**
 * HttpResumableChunkNumber Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>nov. 23, 2019</pre>
 */
public class HttpResumableChunkNumberTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();


  @Before
  public void before() throws Exception {
  }

  @After
  public void after() throws Exception {
  }

  /**
   * Method: hashCode()
   */
  @Test
  public void testHashCode() throws Exception {
    HttpResumableChunkNumber resumableChunkNumber =
        new HttpResumableChunkNumber(1);
    assertEquals(1, resumableChunkNumber.hashCode());
  }

  /**
   * Method: equals(final Object obj)
   */
  @Test
  public void testEquals() throws Exception {
    HttpResumableChunkNumber resumableChunkNumber =
        new HttpResumableChunkNumber(1);
    HttpResumableChunkNumber resumableChunkNumber2 =
        new HttpResumableChunkNumber(1);
    HttpResumableChunkNumber resumableChunkNumber3 =
        new HttpResumableChunkNumber(2);
    assertTrue(resumableChunkNumber.equals(resumableChunkNumber2));
    assertFalse(resumableChunkNumber.equals(resumableChunkNumber3));
  }

  /**
   * Method: toString()
   */
  @Test
  public void testToString() throws Exception {
    HttpResumableChunkNumber resumableChunkNumber =
        new HttpResumableChunkNumber(1);
    assertEquals("1", resumableChunkNumber.toString());
  }


} 
