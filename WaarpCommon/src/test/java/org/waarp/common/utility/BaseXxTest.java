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

import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class BaseXxTest {

  @Test(expected = IllegalArgumentException.class)
  public void testBase16() throws IOException {
    BaseXx.getBase16(null);
    fail("EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBase32() throws FileNotFoundException {
    BaseXx.getBase32(null);
    fail("EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBase64() throws IOException {
    BaseXx.getBase64UrlWithoutPadding(null);
    fail("EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBase64UrlPadding() throws IOException {
    BaseXx.getBase64UrlWithPadding(null);
    fail("EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION");
  }


  @Test(expected = IllegalArgumentException.class)
  public void testFromBase16() throws IOException {
    BaseXx.getFromBase16(null);
    fail("EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromBase32() throws FileNotFoundException {
    BaseXx.getFromBase32(null);
    fail("EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromBase64() throws IOException {
    BaseXx.getFromBase64UrlWithoutPadding(null);
    fail("EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFromBase64Padding() throws IOException {
    BaseXx.getFromBase64UrlPadding(null);
    fail("EXPECTING_EXCEPTION_ILLEGAL_ARGUMENT_EXCEPTION");
  }

  @Test
  public void testBase64UrlPaddingOK() throws IOException {
    final String encoded =
        BaseXx.getBase64UrlWithPadding("WaarpTest64P".getBytes());
    assertNotNull(encoded);
    final byte[] bytes = BaseXx.getFromBase64UrlPadding(encoded);
    assertNotNull(bytes);
    assertTrue(Arrays.equals(bytes, "WaarpTest64P".getBytes()));
  }

  @Test
  public void testBase64PaddingOK() throws IOException {
    final String encoded = BaseXx.getBase64("WaarpTest64P".getBytes());
    assertNotNull(encoded);
    final byte[] bytes = BaseXx.getFromBase64(encoded);
    assertNotNull(bytes);
    assertTrue(Arrays.equals(bytes, "WaarpTest64P".getBytes()));
  }

  @Test
  public void testBase64UrlWithoutPaddingOK() throws IOException {
    final String encoded =
        BaseXx.getBase64UrlWithoutPadding("WaarpTest64".getBytes());
    assertNotNull(encoded);
    final byte[] bytes = BaseXx.getFromBase64UrlWithoutPadding(encoded);
    assertNotNull(bytes);
    assertTrue(Arrays.equals(bytes, "WaarpTest64".getBytes()));
  }

  @Test
  public void testBase32OK() throws IOException {
    final String encoded = BaseXx.getBase32("WaarpTest32".getBytes());
    assertNotNull(encoded);
    final byte[] bytes = BaseXx.getFromBase32(encoded);
    assertNotNull(bytes);
    assertTrue(Arrays.equals(bytes, "WaarpTest32".getBytes()));
  }

  @Test
  public void testBase16OK() throws IOException {
    final String encoded = BaseXx.getBase16("WaarpTest16".getBytes());
    assertNotNull(encoded);
    final byte[] bytes = BaseXx.getFromBase16(encoded);
    assertNotNull(bytes);
    assertTrue(Arrays.equals(bytes, "WaarpTest16".getBytes()));
  }

}
