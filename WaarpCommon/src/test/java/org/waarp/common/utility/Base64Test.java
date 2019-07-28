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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import static org.junit.Assert.*;

public class Base64Test {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testEncodeBytesByteArray() throws Exception {
    final String s = WaarpStringUtils.fillString('a', 100);
    String sencoded = Base64.encodeBytes(s.getBytes());
    byte[] src = Base64.decode(sencoded);
    assertArrayEquals("Should be equals", s.getBytes(), src);
    sencoded = Base64.encodeBytes(s.getBytes(), 0, s.getBytes().length);
    src = Base64.decode(sencoded);
    assertArrayEquals("Should be equals", s.getBytes(), src);

    ByteBuffer raw = ByteBuffer.wrap(src);
    final ByteBuffer encoded =
        ByteBuffer.allocate(sencoded.getBytes().length + 10);
    Base64.encode(raw, encoded);
    byte[] src2 = Base64.decode(encoded.array());
    assertArrayEquals("Should be equals", src, src2);

    src2 = Base64.encodeBytesToBytes(s.getBytes());
    assertArrayEquals("Should be equals", sencoded.getBytes(), src2);
    src2 = Base64
        .encodeBytesToBytes(s.getBytes(), 0, s.getBytes().length, Base64.GZIP);
    final String sencoded2 = new String(src2);
    assertArrayEquals("Should be equals", sencoded2.getBytes(), src2);

    raw = ByteBuffer.wrap(src);
    final CharBuffer encode =
        CharBuffer.allocate(sencoded.getBytes().length + 10);
    Base64.encode(raw, encode);
    final char[] chars = encode.array();
    final byte[] src3 = new byte[chars.length];
    for (int i = 0; i < chars.length; i++) {
      src3[i] = (byte) chars[i];
    }
    src2 = Base64.decode(src3);
    assertArrayEquals("Should be equals", src, src2);

    final SerializableObject serializableObject = new SerializableObject();
    serializableObject.value = 2;
    String sobj = Base64.encodeObject(serializableObject);
    Object obj = Base64.decodeToObject(sobj);
    assertTrue("Should be of correct class", obj instanceof SerializableObject);
    assertEquals("Should be equals", serializableObject.value,
                 ((SerializableObject) obj).value);

    sobj = Base64.encodeObject(serializableObject, Base64.GZIP);
    obj = Base64.decodeToObject(sobj, Base64.GZIP, null);
    assertTrue("Should be of correct class", obj instanceof SerializableObject);
    assertEquals("Should be equals", serializableObject.value,
                 ((SerializableObject) obj).value);

  }

  @Test
  public void testEncodeToFile() throws IOException {
    final String s = WaarpStringUtils.fillString('a', 100);
    Base64.encodeToFile(s.getBytes(), "/tmp/base64.txt");
    final byte[] src = Base64.decodeFromFile("/tmp/base64.txt");
    assertArrayEquals("Should be equals", s.getBytes(), src);

    final String s1 = WaarpStringUtils.readFile("/tmp/base64.txt");
    final File file = new File("/tmp/source.txt");
    Base64.decodeToFile(s1, "/tmp/source.txt");
    final String s0 = WaarpStringUtils.readFile("/tmp/source.txt");
    assertEquals("Should be equals", s, s0);

    Base64.encodeFileToFile(file.getAbsolutePath(), "/tmp/base64.txt");
    String s2 = WaarpStringUtils.readFile("/tmp/base64.txt");
    String s3 = Base64.encodeFromFile("/tmp/source.txt");
    assertEquals("Should be equals", s2, s3);

    Base64.decodeFileToFile("/tmp/base64.txt", "/tmp/source2.txt");
    s2 = WaarpStringUtils.readFile("/tmp/source.txt");
    s3 = WaarpStringUtils.readFile("/tmp/source2.txt");
    assertEquals("Should be equals", s2, s3);
  }

  private static class SerializableObject implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -330624201380075150L;
    private int value = 1;
  }

}
