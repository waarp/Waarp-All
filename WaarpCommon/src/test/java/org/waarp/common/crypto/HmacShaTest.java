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
package org.waarp.common.crypto;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.utility.TestWatcherJunit4;

import static org.junit.Assert.*;

/**
 *
 */
public class HmacShaTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();


  /**
   * Test method
   */
  @Test
  public void testToCrypt() {
    final String plaintext =
        "This is a try for a very long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long String";
    final HmacSha256 hmacSha256 = new HmacSha256();
    // Generate a key
    try {
      hmacSha256.generateKey();
    } catch (final Exception e) {
      fail(e.getMessage());
      return;
    }
    // get the generated key
    byte[] secretKey = hmacSha256.getSecretKeyInBytes();
    assertNotNull(secretKey);
    // crypt one text
    byte[] ciphertext;
    try {
      ciphertext = hmacSha256.crypt(plaintext);
    } catch (final Exception e) {
      fail(e.getMessage());
      return;
    }
    assertNotNull(ciphertext);
    // Test the set Key
    hmacSha256.setSecretKey(secretKey);

    // same on String only
    int nb = 50000;
    int k = 0;
    long time1 = System.currentTimeMillis();
    for (int i = 0; i < nb; i++) {
      String cipherString;
      try {
        cipherString = hmacSha256.cryptToHex(plaintext);
      } catch (final Exception e) {
        fail(e.getMessage());
        return;
      }
      k += cipherString.length();
    }
    long time2 = System.currentTimeMillis();
    System.out.println("SHA256 Total time in ms: " + (time2 - time1) + " or " +
                       nb * 1000 / (time2 - time1) + " crypt/s for " + k / nb);

    final HmacSha1 hmacSha1 = new HmacSha1();
    // Generate a key
    try {
      hmacSha1.generateKey();
    } catch (final Exception e) {
      fail(e.getMessage());
      return;
    }
    // get the generated key
    secretKey = hmacSha1.getSecretKeyInBytes();
    assertNotNull(secretKey);
    // crypt one text
    try {
      ciphertext = hmacSha1.crypt(plaintext);
    } catch (final Exception e) {
      fail(e.getMessage());
      return;
    }
    assertNotNull(ciphertext);
    // Test the set Key
    hmacSha1.setSecretKey(secretKey);
    // same on String only
    nb = 50000;
    k = 0;
    time1 = System.currentTimeMillis();
    for (int i = 0; i < nb; i++) {
      String cipherString;
      try {
        cipherString = hmacSha1.cryptToHex(plaintext);
      } catch (final Exception e) {
        fail(e.getMessage());
        return;
      }
      k += cipherString.length();
    }
    time2 = System.currentTimeMillis();
    System.out.println("SHA1 Total time in ms: " + (time2 - time1) + " or " +
                       nb * 1000 / (time2 - time1) + " crypt/s for " + k / nb);
    assertTrue(true);
  }

}
