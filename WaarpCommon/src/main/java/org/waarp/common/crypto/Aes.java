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

/**
 * This class handles methods to crypt and decrypt messages with AES algorithm
 * (very efficient: 60000/s).<br>
 * <br>
 * Usage:<br>
 * <ul>
 * <li>Create a Aes object: Aes key = new Aes();</li>
 * <li>Create a key:
 * <ul>
 * <li>Generate: key.generateKey();<br>
 * The method key.getSecretKeyInBytes() allow getting the key in Bytes.</li>
 * <li>From an external source: key.setSecretKey(arrayOfBytes);</li>
 * </ul>
 * </li>
 * <li>To crypt a String in a Base64 format: String myStringCrypt =
 * key.cryptToString(myString);</li>
 * <li>To decrypt one string from Base64 format to the original String: String
 * myStringDecrypt =
 * key.decryptStringInString(myStringCrypte);</li>
 * </ul>
 */
public class Aes extends KeyObject {
  /**
   * This value could be between 32 and 128 due to license limitation.
   */
  private static final int KEY_SIZE = 128; // [32..448]
  private static final String ALGO = "AES";
  private static final String INSTANCE = "AES/ECB/PKCS5Padding";
  public static final String EXTENSION = "aes";

  @Override
  public String getAlgorithm() {
    return ALGO;
  }

  @Override
  public String getInstance() {
    return INSTANCE;
  }

  @Override
  public int getKeySize() {
    return KEY_SIZE;
  }

  @Override
  public String getFileExtension() {
    return EXTENSION;
  }
}
