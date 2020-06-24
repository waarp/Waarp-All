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

package org.waarp.common.utility;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class StringUtilsTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final byte[] BYTES_0_LENGTH = {};

  @Test
  public void testRandom() {
    final byte[] byteArray0 = StringUtils.getRandom(90);
    assertNotNull(byteArray0);
    final byte[] byteArray1 = StringUtils.getRandom(90);
    assertFalse(Arrays.equals(byteArray0, byteArray1));
    final byte[] byteArray2 = StringUtils.getBytesFromArraysToString(", ");
    assertArrayEquals(BYTES_0_LENGTH, byteArray2);
    final byte[] byteArray3 = StringUtils.getRandom(0);
    assertArrayEquals(BYTES_0_LENGTH, byteArray3);
    final byte[] byteArray4 = StringUtils.getRandom(-10);
    assertArrayEquals(BYTES_0_LENGTH, byteArray4);
  }

  @Test
  public void testBytesFromArrayToString() {
    final byte[] byteArray0 = StringUtils.getBytesFromArraysToString("7");
    assertArrayEquals(new byte[] { (byte) 7 }, byteArray0);
    final byte[] byteArray1 = StringUtils.getRandom(90);
    final String sbyte = Arrays.toString(byteArray1);
    final byte[] byteArray2 = StringUtils.getBytesFromArraysToString(sbyte);
    assertArrayEquals(byteArray1, byteArray2);

  }

  @Test
  public void testException() {
    try {
      StringUtils.getBytesFromArraysToString(null);
      fail("EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
      // Ignore
    }
    try {
      StringUtils.getBytesFromArraysToString("");
      fail("EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
      // Ignore
    }
    try {
      StringUtils.getBytesFromArraysToString("[ 7, a6 ]");
      fail("EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION");
    } catch (final IllegalArgumentException e) { // NOSONAR
      // Ignore
    }
  }

  @Test
  public void testGetClassName() {
    StringUtilsTest test = new StringUtilsTest();
    String name = StringUtils.getClassName(test);
    assertNotEquals(test.getClass().getName(), name);
    assertEquals(test.getClass().getSimpleName(), name);
  }

  @Test
  public void testWaarpStringUtils() {
    Timestamp timestamp = WaarpStringUtils.getTodayMidnight();
    Timestamp timestamp1 = new Timestamp(System.currentTimeMillis());
    assertTrue(timestamp.before(timestamp1));
  }

  @Test
  public void testSingletons() throws IOException {
    final byte[] bytes = SingletonUtils.getSingletonByteArray();
    assertEquals(0, bytes.length);

    final List<StringUtilsTest> emptyList = SingletonUtils.singletonList();
    assertTrue(emptyList.isEmpty());
    assertEquals(0, emptyList.size());
    try {
      emptyList.add(this);
      fail("SHOULD_HAVE_AN_EXCEPTION");
    } catch (final UnsupportedOperationException e) {// NOSONAR
      // ignore
    }
    assertTrue(emptyList.isEmpty());
    assertEquals(0, emptyList.size());
    try {
      emptyList.remove(0);
      fail("SHOULD_HAVE_AN_EXCEPTION");
    } catch (final UnsupportedOperationException e) {// NOSONAR
      // ignore
    }
    assertTrue(emptyList.isEmpty());
    assertEquals(0, emptyList.size());

    final Set<StringUtilsTest> emptySet = SingletonUtils.singletonSet();
    assertTrue(emptySet.isEmpty());
    assertEquals(0, emptySet.size());
    try {
      emptySet.add(this);
      fail("SHOULD_HAVE_AN_EXCEPTION");
    } catch (final UnsupportedOperationException e) {// NOSONAR
      // ignore
    }
    assertTrue(emptySet.isEmpty());
    assertEquals(0, emptySet.size());
    emptySet.remove(this);
    assertTrue(emptySet.isEmpty());
    assertEquals(0, emptySet.size());

    final Map<StringUtilsTest, StringUtilsTest> emptyMap =
        SingletonUtils.singletonMap();
    assertTrue(emptyMap.isEmpty());
    assertEquals(0, emptyMap.size());
    try {
      emptyMap.put(this, this);
      fail("SHOULD_HAVE_AN_EXCEPTION");
    } catch (final UnsupportedOperationException e) {// NOSONAR
      // ignore
    }
    assertTrue(emptyMap.isEmpty());
    assertEquals(0, emptyMap.size());
    emptyMap.remove(this);
    assertTrue(emptyMap.isEmpty());
    assertEquals(0, emptyMap.size());

    final InputStream emptyIS = SingletonUtils.singletonInputStream();
    final byte[] buffer = new byte[10];
    assertEquals(0, emptyIS.available());
    assertEquals(0, emptyIS.skip(10));
    assertEquals(-1, emptyIS.read());
    assertEquals(-1, emptyIS.read(buffer));
    assertEquals(-1, emptyIS.read(buffer, 0, buffer.length));
    assertTrue(emptyIS.markSupported());
    emptyIS.mark(5);
    emptyIS.reset();
    emptyIS.close();

    // No error
    final OutputStream voidOS = SingletonUtils.singletonOutputStream();
    voidOS.write(buffer);
    voidOS.write(1);
    voidOS.write(buffer, 0, buffer.length);
    voidOS.flush();
    voidOS.close();

  }
}
