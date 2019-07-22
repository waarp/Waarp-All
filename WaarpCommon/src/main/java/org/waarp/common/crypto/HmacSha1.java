/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.common.crypto;

import javax.crypto.Cipher;
import javax.crypto.Mac;

/**
 * This class handles methods to crypt (not decrypt) messages with HmacSha1 algorithm (very efficient:
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
 * <li>To crypt a String in a Base64 format: String myStringCrypt = key.cryptToString(myString);</li>
 * </ul>
 * 
 * @author frederic bregier
 * 
 */
public class HmacSha1 extends KeyObject {
    private final static int KEY_SIZE = 128;
    private final static String ALGO = "HmacSHA1";
    private final static String INSTANCE = ALGO;
    public final static String EXTENSION = "hs1";

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
        throw new IllegalArgumentException("Cannot be used for HmacSha1");
    }

    @Override
    public byte[] crypt(byte[] plaintext) throws Exception {
        Mac mac = Mac.getInstance(ALGO);
        mac.init(secretKey);
        return mac.doFinal(plaintext);
    }

    @Override
    public Cipher toDecrypt() {
        throw new IllegalArgumentException("Cannot be used for HmacSha1");
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) throws Exception {
        throw new IllegalArgumentException("Cannot be used for HmacSha1");
    }

}
