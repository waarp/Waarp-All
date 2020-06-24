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
package org.waarp.common.guid;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.TestWatcherJunit4;

import java.util.Arrays;

import static org.junit.Assert.*;

@SuppressWarnings("javadoc")
public class GUIDTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final WaarpLogger LOGGER =
      WaarpLoggerFactory.getInstance(GUIDTest.class);

  private static final String WRONG_ARK3 =
      "ark:/1a/aeasppnwoyafrlybkt3kfuyaaaaac";

  private static final String WRONG_ARK2 =
      "ark:/1aeasppnwoyafrlybkt3kfuyaaaaac";

  private static final String WRONG_ARK1 =
      "ark:/1/aeasppnwoyafrlybkt3kfuyaaaaacaaaaa";

  private static final byte[] WRONG_BYTES = {
      2, 1, 0, 0, 0, 1, 39, -67, -74, 118, 0, 88, -81, 1, 84, -10, -94, -45, 0,
      0, 0, 1
  };
  private static final String WRONG_STRING_ID =
      "02010000000127bdb6760058af0154f6a2d300000001";

  private static final String BASE16 =
      "0100000000000000000020ae016c1e21cc0c000001";
  private static final String BASE32 = "aeaaaaaaaaaaaaaaecxac3a6ehgayaaaae";
  private static final String BASE64 = "AQAAAAAAAAAAACCuAWweIcwMAAAB";
  private static final String BASEARK = "ark:/0/aeaaaaaaaaqk4almdyq4ydaaaaaq";
  private static final byte[] BYTES = {
      1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, -82, 1, 108, 30, 33, -52, 12, 0, 0, 1
  };
  private static final int VERSION = 1 & 0x1F;
  private static final int HEXLENGTH = GUID.KEYSIZE * 2;

  public void printValues() {
    GUID guid = new GUID();
    LOGGER.warn("GUID: {} {} {} {} {} {} {} {} {} {} {} {} {} {} {}",
                guid.getVersion(), guid.toBase32(), guid.getBytes(),
                guid.toHex(), guid.getTenantId(), guid.toString(),
                guid.getCounter(), guid.getTimestamp(), guid.toArk(),
                guid.toBase64(), guid.getId(), guid.toArkName(),
                guid.getMacFragment(), guid.getPlatformId(),
                guid.getProcessId());
  }

  @Test
  public void testStructure() {
    GUID id;
    try {
      id = new GUID(BASE32);
      final String str = id.toHex();

      assertEquals('0', str.charAt(0));
      assertEquals('1', str.charAt(1));
      assertEquals(HEXLENGTH, str.length());
      LOGGER.debug(id.toArk() + " = " + id);
    } catch (final InvalidArgumentException e) {
      LOGGER.debug(e);
      fail("Should not raize an exception");
    }
  }

  @Test
  public void testParsing() {
    for (int i = 0; i < 1000; i++) {
      GUID id1;
      try {
        id1 = new GUID(BASE32);
      } catch (final InvalidArgumentException e) {
        LOGGER.debug(e);
        fail("Should not raize an exception");
        return;
      }
      GUID id2;
      try {
        id2 = new GUID(id1.toHex());
        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
        assertEquals(0, id1.compareTo(id2));

        final GUID id3 = new GUID(id1.getBytes());
        assertEquals(id1, id3);
        assertEquals(id1.hashCode(), id3.hashCode());
        assertEquals(0, id1.compareTo(id3));

        final GUID id4 = new GUID(id1.toBase32());
        assertEquals(id1, id4);
        assertEquals(id1.hashCode(), id4.hashCode());
        assertEquals(0, id1.compareTo(id4));

        final GUID id5 = new GUID(id1.toArk());
        assertEquals(id1, id5);
        assertEquals(id1.hashCode(), id5.hashCode());
        assertEquals(0, id1.compareTo(id5));
      } catch (final InvalidArgumentException e) {
        LOGGER.debug(e);
        fail(e.getMessage());
      }
    }
  }

  @Test
  public void testGetBytesImmutability() {
    GUID id;
    try {
      id = new GUID(BASE32);
    } catch (final InvalidArgumentException e) {
      LOGGER.debug(e);
      fail("Should not raize an exception");
      return;
    }
    final byte[] bytes = id.getBytes();
    final byte[] original = Arrays.copyOf(bytes, bytes.length);
    bytes[0] = 0;
    bytes[1] = 0;
    bytes[2] = 0;

    assertArrayEquals(id.getBytes(), original);
  }

  @Test
  public void testVersionField() {
    try {
      final GUID parsed1 = new GUID(BASE32);
      assertEquals(VERSION, parsed1.getVersion());
    } catch (final InvalidArgumentException e) {
      LOGGER.debug(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void testHexBase32() {
    try {
      final GUID parsed1 = new GUID(BASE32);
      final GUID parsed2 = new GUID(BASE64);
      final GUID parsed0 = new GUID(BASE16);
      final GUID parsed8 = new GUID(BASEARK);
      final GUID parsed9 = new GUID(BYTES);
      assertEquals(parsed1, parsed2);
      assertEquals(parsed1, parsed8);
      assertEquals(parsed1, parsed9);
      assertEquals(parsed1, parsed0);
      final GUID parsed3 = new GUID(parsed9.getBytes());
      final GUID parsed4 = new GUID(parsed9.toBase32());
      final GUID parsed5 = new GUID(parsed9.toHex());
      final GUID parsed6 = new GUID(parsed9.toString());
      final GUID parsed7 = new GUID(parsed9.toBase64());
      assertEquals(parsed9, parsed3);
      assertEquals(parsed9, parsed4);
      assertEquals(parsed9, parsed5);
      assertEquals(parsed9, parsed6);
      assertEquals(parsed9, parsed7);
      final GUID generated = new GUID();
      LOGGER.warn("{}", generated.getVersion());
      assertEquals(1, generated.getVersion());
    } catch (final InvalidArgumentException e) {
      LOGGER.debug(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void testJson() throws InvalidArgumentException {
    GUID guid;
    try {
      guid = new GUID(BASE32);
    } catch (final InvalidArgumentException e) {
      LOGGER.debug(e);
      fail("Should not raize an exception");
      return;
    }
    LOGGER.debug("HEX:" + guid.toHex());
    LOGGER.debug("BASE32: " + guid.toBase32());
    LOGGER.debug("BASE64: " + guid.toBase64());
    final String json = JsonHandler.writeAsString(guid);
    LOGGER.debug(json);
    final GUID uuid2 = JsonHandler.getFromString(json, GUID.class);
    assertEquals("Json check", guid, uuid2);
    final GUID guid2 = new GUID(guid.getId());
    final String json2 = JsonHandler.writeAsString(guid2);
    LOGGER.debug(json2);
    final GUID uuid3 = JsonHandler.getFromString(json, GUID.class);
    assertEquals("Json check", guid, uuid3);
  }

  @Test
  public final void testIllegalArgument() {
    try {
      new GUID(WRONG_ARK1);
      fail("SHOULD_HAVE_AN_EXCEPTION");
    } catch (final InvalidArgumentException e) {
      LOGGER.trace("SHOULD_HAVE_AN_EXCEPTION", e);
    }
    try {
      new GUID(WRONG_ARK2);
      fail("SHOULD_HAVE_AN_EXCEPTION");
    } catch (final InvalidArgumentException e) {
      LOGGER.trace("SHOULD_HAVE_AN_EXCEPTION", e);
    }
    try {
      new GUID(WRONG_ARK3);
      fail("SHOULD_HAVE_AN_EXCEPTION");
    } catch (final InvalidArgumentException e) {
      LOGGER.trace("SHOULD_HAVE_AN_EXCEPTION", e);
    }
    try {
      new GUID(WRONG_BYTES);
      fail("SHOULD_HAVE_AN_EXCEPTION");
    } catch (final InvalidArgumentException e) {
      LOGGER.trace("SHOULD_HAVE_AN_EXCEPTION", e);
    }
    try {
      new GUID(WRONG_STRING_ID);
      fail("SHOULD_HAVE_AN_EXCEPTION");
    } catch (final InvalidArgumentException e) {
      LOGGER.trace("SHOULD_HAVE_AN_EXCEPTION", e);
    }
    GUID guid = null;
    GUID guid2 = null;
    try {
      guid = new GUID(BASE32);
      guid2 = new GUID(BASE16);
    } catch (final InvalidArgumentException e) {
      LOGGER.error("SHOULD_NOT_HAVE_AN_EXCEPTION", e);
      fail("SHOULD_NOT_HAVE_AN_EXCEPTION");
      return;
    }
    assertNotEquals(null, guid);
    assertNotEquals(guid, new Object());
    assertEquals(guid, guid);
    assertEquals(guid, guid2);
  }

}
