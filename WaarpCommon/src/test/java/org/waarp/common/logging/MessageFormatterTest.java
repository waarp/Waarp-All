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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class MessageFormatterTest {

  private static final Object[] OBJECT_ARRAY_0 = new Object[0];

  @Test
  public void testGlobal() {
    boolean boolean0 = MessageFormatter.isDoubleEscaped("ENUM$VALUES", 5);
    assertFalse(boolean0);
    FormattingTuple formattingTuple0 =
        MessageFormatter.format("{}", null, "{}");
    assertNotNull(formattingTuple0);
    boolean0 = MessageFormatter.isEscapedDelimeter("6J]Ulo:L>", 3);
    assertFalse(boolean0);
    Object[] objectArray0 = new Object[5];
    final Throwable throwable0 =
        MessageFormatter.getThrowableCandidate(objectArray0);
    assertNull(throwable0);
    boolean0 = MessageFormatter.isDoubleEscaped("6J]Ulo:L>", 4);
    assertFalse(boolean0);
    boolean0 = MessageFormatter.isDoubleEscaped("R!{}", -918);
    assertFalse(boolean0);
    objectArray0 = new Object[5];
    formattingTuple0 = MessageFormatter.arrayFormat("{}", objectArray0);
    assertNotNull(formattingTuple0);
    formattingTuple0 = MessageFormatter.format(null, null);
    assertNotNull(formattingTuple0);
    objectArray0 = OBJECT_ARRAY_0;
    formattingTuple0 = MessageFormatter.arrayFormat("6J]Ulo:L>", objectArray0);
    assertNotNull(formattingTuple0);
    formattingTuple0 =
        MessageFormatter.arrayFormat("[FAILED toString()]", null);
    assertNotNull(formattingTuple0);
    formattingTuple0 = MessageFormatter.format("", "");
    assertNotNull(formattingTuple0);
    objectArray0 = new Object[5];
    objectArray0[0] = "R!{}";
    formattingTuple0 = MessageFormatter.arrayFormat("R!{}", objectArray0);
    assertNotNull(formattingTuple0);
    formattingTuple0 =
        MessageFormatter.arrayFormat("[FAILED toString()]", new Boolean[1]);
    assertNotNull(formattingTuple0);
  }

  @Test
  public void testAppendParameter() {
    final StringBuilder sbuild = new StringBuilder();
    final Map<Object[], Void> seenMap = new HashMap<Object[], Void>();

    final boolean[] ob = { true };
    MessageFormatter.deeplyAppendParameter(sbuild, ob, seenMap);
    assertTrue(sbuild.length() > 0);
    sbuild.setLength(0);

    final byte[] ob2 = { (byte) 1 };
    MessageFormatter.deeplyAppendParameter(sbuild, ob2, seenMap);
    assertTrue(sbuild.length() > 0);
    sbuild.setLength(0);

    final char[] oc = { 'a' };
    MessageFormatter.deeplyAppendParameter(sbuild, oc, seenMap);
    assertTrue(sbuild.length() > 0);
    sbuild.setLength(0);

    final short[] os = { (short) 1 };
    MessageFormatter.deeplyAppendParameter(sbuild, os, seenMap);
    assertTrue(sbuild.length() > 0);
    sbuild.setLength(0);

    final int[] oi = { 1 };
    MessageFormatter.deeplyAppendParameter(sbuild, oi, seenMap);
    assertTrue(sbuild.length() > 0);
    sbuild.setLength(0);

    final long[] ol = { 1L };
    MessageFormatter.deeplyAppendParameter(sbuild, ol, seenMap);
    assertTrue(sbuild.length() > 0);
    sbuild.setLength(0);

    final float[] of = { 1.5f };
    MessageFormatter.deeplyAppendParameter(sbuild, of, seenMap);
    assertTrue(sbuild.length() > 0);
    sbuild.setLength(0);

    final double[] od = { 2.5 };
    MessageFormatter.deeplyAppendParameter(sbuild, od, seenMap);
    assertTrue(sbuild.length() > 0);
    sbuild.setLength(0);

    final Object[] ob3 = { new Object() };
    MessageFormatter.deeplyAppendParameter(sbuild, ob3, seenMap);
    assertTrue(sbuild.length() > 0);
    sbuild.setLength(0);

  }

  @Test
  public void testError() {
    try {
      MessageFormatter.isEscapedDelimeter(null, -1);
      fail("EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
    }
    try {
      MessageFormatter.isDoubleEscaped(null, 163);
      fail("EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
    }
  }
}
