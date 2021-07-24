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

import javax.crypto.Cipher;
import javax.crypto.Mac;

/**
 * This class handles methods to crypt (not decrypt) messages with HmacSha1
 * algorithm (very efficient:
 * 136000/s).<br>
 * <br>
 * Usage:<br>
 * <ul>
 * <li>Create a HmacSha1 object: HmacSha1 key = new HmacSha1();</li>
 * <li>Create a key:
 * <ul>
 * <li>Generate: key.generateKey();<br>
 * The method key.getSecretKeyInBytes() allow getting the key in Bytes.</li>
 * <li>From an external source: key.setSecretKey(arrayOfBytes);</li>
 * </ul>
 * </li>
 * <li>To crypt a String in a Base64 format: String myStringCrypt =
 * key.cryptToString(myString);</li>
 * </ul>
 */
public class HmacSha1 extends KeyObject {
  private static final String CANNOT_BE_USED_FOR_HMAC_SHA1 =
      "Cannot be used for HmacSha1";
  private static final int KEY_SIZE = 128;
  private static final String ALGO = "HmacSHA1";
  private static final String INSTANCE = ALGO;
  public static final String EXTENSION = "hs1";

  @Override
  public final String getAlgorithm() {
    return ALGO;
  }

  @Override
  public final String getInstance() {
    return INSTANCE;
  }

  @Override
  public final int getKeySize() {
    return KEY_SIZE;
  }

  @Override
  public final String getFileExtension() {
    return EXTENSION;
  }

  @Override
  public final Cipher toCrypt() {
    throw new IllegalArgumentException(CANNOT_BE_USED_FOR_HMAC_SHA1);
  }

  @Override
  public final byte[] crypt(final byte[] plaintext) throws Exception {
    final Mac mac = Mac.getInstance(ALGO);
    mac.init(secretKey);
    return mac.doFinal(plaintext);
  }

  @Override
  public final Cipher toDecrypt() {
    throw new IllegalArgumentException(CANNOT_BE_USED_FOR_HMAC_SHA1);
  }

  @Override
  public final byte[] decrypt(final byte[] ciphertext) throws Exception {
    throw new IllegalArgumentException(CANNOT_BE_USED_FOR_HMAC_SHA1);
  }

}
