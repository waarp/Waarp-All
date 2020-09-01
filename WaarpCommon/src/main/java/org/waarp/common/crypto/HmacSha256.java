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

import org.waarp.common.exception.CryptoException;
import org.waarp.common.logging.SysErrLogger;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import java.io.File;
import java.io.IOException;

/**
 * This class handles methods to crypt (not decrypt) messages with HmacSha256
 * algorithm (very efficient:
 * 105000/s).<br>
 * <br>
 * Usage:<br>
 * <ul>
 * <li>Create a HmacSha256 object: HmacSha256 key = new HmacSha256();</li>
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
public class HmacSha256 extends KeyObject {
  private static final String ERROR = "Error: ";
  private static final String CANNOT_BE_USED_FOR_HMAC_SHA256 =
      "Cannot be used for HmacSha256";
  private static final int KEY_SIZE = 128;
  private static final String ALGO = "HmacSHA256";
  private static final String INSTANCE = ALGO;
  public static final String EXTENSION = "hs2";

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

  @Override
  public Cipher toCrypt() {
    throw new IllegalArgumentException(CANNOT_BE_USED_FOR_HMAC_SHA256);
  }

  @Override
  public byte[] crypt(final byte[] plaintext) throws Exception {
    final Mac mac = Mac.getInstance(ALGO);
    mac.init(secretKey);
    return mac.doFinal(plaintext);
  }

  @Override
  public Cipher toDecrypt() {
    throw new IllegalArgumentException(CANNOT_BE_USED_FOR_HMAC_SHA256);
  }

  @Override
  public byte[] decrypt(final byte[] ciphertext) throws Exception {
    throw new IllegalArgumentException(CANNOT_BE_USED_FOR_HMAC_SHA256);
  }

  /**
   * Generates a HmacSha256 key and saves it into the file given as argument
   *
   * @param args
   */
  public static void main(final String[] args) {
    if (args.length == 0) {
      SysErrLogger.FAKE_LOGGER.syserr("Filename is needed as argument");
    }
    final HmacSha256 key = new HmacSha256();
    try {
      key.generateKey();
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(ERROR + e.getMessage());
      return;
    }
    try {
      key.saveSecretKey(new File(args[0]));
    } catch (final CryptoException e) {
      SysErrLogger.FAKE_LOGGER.syserr(ERROR + e.getMessage());
      return;
    } catch (final IOException e) {
      SysErrLogger.FAKE_LOGGER.syserr(ERROR + e.getMessage());
      return;
    }
    SysErrLogger.FAKE_LOGGER
        .sysout("New HmacSha256 key file is generated: " + args[0]);
  }
}
