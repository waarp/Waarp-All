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

public class FormattingTupleTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final String NON_SENSICAL_EMPTY_OR_NULL_ARGUMENT_ARRAY =
      "non-sensical empty or null argument array";
  private static final Object[] OBJECT_ARRAY_0 = new Object[0];

  @Test
  public void testTuple() {
    Object[] objectArray0 = new Object[1];
    FormattingTuple formattingTuple0 =
        new FormattingTuple(null, objectArray0, null);
    String string0 = formattingTuple0.getMessage();
    assertNull(string0);
    objectArray0 = new Object[1];
    formattingTuple0 = new FormattingTuple(null, objectArray0, null);
    Object[] objectArray1 = formattingTuple0.getArgArray();
    assertSame(objectArray0, objectArray1);
    objectArray0 = OBJECT_ARRAY_0;
    formattingTuple0 = new FormattingTuple("@2tO", objectArray0, null);
    objectArray1 = formattingTuple0.getArgArray();
    assertSame(objectArray0, objectArray1);
    objectArray0 = new Object[9];
    objectArray1 = FormattingTuple.trimmedCopy(objectArray0);
    assertNotSame(objectArray1, objectArray0);
    objectArray0 = new Object[1];
    objectArray1 = FormattingTuple.trimmedCopy(objectArray0);
    assertNotSame(objectArray1, objectArray0);
    formattingTuple0 =
        new FormattingTuple(NON_SENSICAL_EMPTY_OR_NULL_ARGUMENT_ARRAY);
    string0 = formattingTuple0.getMessage();
    assertEquals(NON_SENSICAL_EMPTY_OR_NULL_ARGUMENT_ARRAY, string0);
    objectArray0 = new Object[1];
    formattingTuple0 = new FormattingTuple(null, objectArray0, null);
    final Throwable throwable0 = formattingTuple0.getThrowable();
    assertNull(throwable0);
    formattingTuple0 =
        new FormattingTuple(NON_SENSICAL_EMPTY_OR_NULL_ARGUMENT_ARRAY);
    objectArray0 = formattingTuple0.getArgArray();
    assertNull(objectArray0);
    objectArray0 = new Object[1];
    formattingTuple0 =
        new FormattingTuple(null, objectArray0, new Throwable("test"));
    assertNotNull(formattingTuple0.getArgArray());
  }

  @Test
  public void testError() {
    final Object[] objectArray0 = OBJECT_ARRAY_0;
    try {
      FormattingTuple.trimmedCopy(objectArray0);
      fail("Expecting exception: IllegalStateException");
    } catch (final IllegalStateException e) {// NOSONAR
      // Ignore
    }
    try {
      FormattingTuple.trimmedCopy(null);
      fail("Expecting exception: IllegalStateException");
    } catch (final IllegalStateException e) {// NOSONAR
      // Ignore
    }
  }
}
