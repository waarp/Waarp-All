/*******************************************************************************
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 *  individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at your
 *  option) any later version.
 *
 *  Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 *  A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  Waarp . If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

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
import java.util.Arrays;

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
  public void testEncodeBytesByteArray()
      throws IOException, ClassNotFoundException {
    String s = WaarpStringUtils.fillString('a', 100);
    String sencoded = Base64.encodeBytes(s.getBytes());
    byte[] src = Base64.decode(sencoded);
    assertTrue("Should be equals", Arrays.equals(s.getBytes(), src));
    sencoded = Base64.encodeBytes(s.getBytes(), 0, s.getBytes().length);
    src = Base64.decode(sencoded);
    assertTrue("Should be equals", Arrays.equals(s.getBytes(), src));

    ByteBuffer raw = ByteBuffer.wrap(src);
    ByteBuffer encoded = ByteBuffer.allocate(sencoded.getBytes().length + 10);
    Base64.encode(raw, encoded);
    byte[] src2 = Base64.decode(encoded.array());
    assertTrue("Should be equals", Arrays.equals(src, src2));

    src2 = Base64.encodeBytesToBytes(s.getBytes());
    assertTrue("Should be equals", Arrays.equals(sencoded.getBytes(), src2));
    src2 = Base64
        .encodeBytesToBytes(s.getBytes(), 0, s.getBytes().length, Base64.GZIP);
    String sencoded2 = new String(src2);
    assertTrue("Should be equals", Arrays.equals(sencoded2.getBytes(), src2));

    raw = ByteBuffer.wrap(src);
    CharBuffer encode = CharBuffer.allocate(sencoded.getBytes().length + 10);
    Base64.encode(raw, encode);
    char[] chars = encode.array();
    byte[] src3 = new byte[chars.length];
    for (int i = 0; i < chars.length; i++) {
      src3[i] = (byte) chars[i];
    }
    src2 = Base64.decode(src3);
    assertTrue("Should be equals", Arrays.equals(src, src2));

    SerializableObject serializableObject = new SerializableObject();
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
    String s = WaarpStringUtils.fillString('a', 100);
    Base64.encodeToFile(s.getBytes(), "/tmp/base64.txt");
    byte[] src = Base64.decodeFromFile("/tmp/base64.txt");
    assertTrue("Should be equals", Arrays.equals(s.getBytes(), src));

    String s1 = WaarpStringUtils.readFile("/tmp/base64.txt");
    File file = new File("/tmp/source.txt");
    Base64.decodeToFile(s1, "/tmp/source.txt");
    String s0 = WaarpStringUtils.readFile("/tmp/source.txt");
    assertTrue("Should be equals", s.equals(s0));

    Base64.encodeFileToFile(file.getAbsolutePath(), "/tmp/base64.txt");
    String s2 = WaarpStringUtils.readFile("/tmp/base64.txt");
    String s3 = Base64.encodeFromFile("/tmp/source.txt");
    assertTrue("Should be equals", s2.equals(s3));

    Base64.decodeFileToFile("/tmp/base64.txt", "/tmp/source2.txt");
    s2 = WaarpStringUtils.readFile("/tmp/source.txt");
    s3 = WaarpStringUtils.readFile("/tmp/source2.txt");
    assertTrue("Should be equals", s2.equals(s3));
  }

  private static class SerializableObject implements Serializable {
    private int value = 1;
  }

}
