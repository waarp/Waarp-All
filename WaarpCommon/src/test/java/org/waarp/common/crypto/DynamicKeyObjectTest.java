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

import org.junit.Test;
import org.waarp.common.crypto.DynamicKeyObject.INSTANCES;
import org.waarp.common.crypto.DynamicKeyObject.INSTANCESMAX;

import static org.junit.Assert.*;

public class DynamicKeyObjectTest {

  @Test
  public void simpleTest() {
    String plaintext = null;
    plaintext =
        "This is a try for a very long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long long String";
    System.out.println("plaintext = " + plaintext);
    System.out.println("=====================================");
    // Can implements with KeyGenerator AES, ARCFOUR, Blowfish, DES, DESede,
    // RC2, RC4
    for (final INSTANCES instance : INSTANCES.values()) {
      try {
        test(plaintext, instance.size, instance.name());
      } catch (final Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
      }
    }
    for (final INSTANCESMAX instance : INSTANCESMAX.values()) {
      try {
        test(plaintext, instance.size, instance.name());
      } catch (final Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
      }
    }
  }

  /**
   * test function
   *
   * @param plaintext
   * @param size
   * @param algo
   *
   * @throws Exception
   */
  private static void test(String plaintext, int size, String algo)
      throws Exception {
    final DynamicKeyObject dyn = new DynamicKeyObject(size, algo, algo, algo);
    // Generate a key
    dyn.generateKey();
    // get the generated key
    final byte[] secretKey = dyn.getSecretKeyInBytes();
    // crypt one text
    final byte[] ciphertext = dyn.crypt(plaintext);
    // print the cipher
    System.out.println("ciphertext = " + dyn.encodeHex(ciphertext));

    // Test the set Key
    dyn.setSecretKey(secretKey);
    // decrypt the cipher
    final String plaintext2 = dyn.decryptInString(ciphertext);
    // print the result
    if (!plaintext2.equals(plaintext)) {
      fail("Error: plaintext2 != plaintext");
    }

    // same on String only
    final int nb = 100;
    final long time1 = System.currentTimeMillis();
    for (int i = 0; i < nb; i++) {
      final String cipherString = dyn.cryptToHex(plaintext);
      final String plaintext3 = dyn.decryptHexInString(cipherString);
      if (!plaintext3.equals(plaintext)) {
        fail("Error: plaintext3 != plaintext");
      }
    }
    final long time2 = System.currentTimeMillis();
    System.out.println(algo + ": Total time: " + (time2 - time1) + " ms, " +
                       nb * 1000 / (time2 + 1 - time1) + " crypt or decrypt/s");
  }
}