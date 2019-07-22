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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class implements a simple Key Manager from name
 *
 *
 */
public abstract class KeyManager {
  ConcurrentHashMap<String, KeyObject> keysConcurrentHashMap =
      new ConcurrentHashMap<String, KeyObject>();
  AtomicBoolean isInitialized = new AtomicBoolean(false);

  public abstract KeyObject createKeyObject();

  /**
   * Init the Manager from a list of filename Key, the key name is the
   * basename
   * minus the extension of the key's
   * type
   *
   * @param keys
   *
   * @return the list of wrong keys
   */
  public List<String> initFromList(List<String> keys) {
    final LinkedList<String> wrong = new LinkedList<String>();
    for (final String filename : keys) {
      final File file = new File(filename);
      if (file.canRead()) {
        final String basename = file.getName();
        final int lastpos = basename.lastIndexOf('.');
        if (lastpos <= 0) {
          wrong.add(filename);
          continue;
        }
        final String firstname = basename.substring(0, lastpos);
        int len = (int) file.length();
        final byte[] key = new byte[len];
        FileInputStream inputStream = null;
        try {
          inputStream = new FileInputStream(file);
        } catch (final FileNotFoundException e) {
          // should not be
          wrong.add(filename);
          continue;
        }
        int read = 0;
        int offset = 0;
        while (read > 0) {
          try {
            read = inputStream.read(key, offset, len);
          } catch (final IOException e) {
            wrong.add(filename);
            read = -2;
            break;
          }
          offset += read;
          if (offset < len) {
            len -= read;
          } else {
            break;
          }
        }
        try {
          inputStream.close();
        } catch (final IOException e) {
        }
        if (read < -1) {
          // wrong
          continue;
        }
        final KeyObject keyObject = createKeyObject();
        keyObject.setSecretKey(key);
        setKey(firstname, keyObject);
      } else {
        wrong.add(filename);
      }
    }
    isInitialized.set(true);
    return wrong;
  }

  public void saveToFiles() throws CryptoException, IOException {
    final Enumeration<String> names = keysConcurrentHashMap.keys();
    while (names.hasMoreElements()) {
      final String name = names.nextElement();
      final KeyObject key = keysConcurrentHashMap.get(name);
      key.saveSecretKey(new File(name + "." + key.getFileExtension()));
    }
  }

  /**
   * Add or set a new key associated to the given name
   *
   * @param name
   * @param keyObject
   */
  public void setKey(String name, KeyObject keyObject) {
    keysConcurrentHashMap.put(name, keyObject);
  }

  /**
   * @param name
   *
   * @return the key associated to the given name
   */
  public KeyObject getKey(String name) {
    return keysConcurrentHashMap.get(name);
  }

  /**
   * One method to get the crypted String from the given string and key
   *
   * @param keyName
   * @param toBeCrypted
   *
   * @return the crypted String
   *
   * @throws Exception
   */
  public String crypt(String keyName, String toBeCrypted) throws Exception {
    final KeyObject keyObject = getKey(keyName);
    if (keyObject == null) {
      throw new NoSuchAlgorithmException("Key does not exist: " + keyName);
    }
    return keyObject.cryptToHex(toBeCrypted);
  }

  /**
   * One method to get the uncrypted String from the given crypted string and
   * key
   *
   * @param keyName
   * @param toBeDecrypted
   *
   * @return the uncrypted String
   *
   * @throws Exception
   */
  public String decrypt(String keyName, String toBeDecrypted) throws Exception {
    final KeyObject keyObject = getKey(keyName);
    if (keyObject == null) {
      throw new NoSuchAlgorithmException("Key does not exist: " + keyName);
    }
    return keyObject.decryptHexInString(toBeDecrypted);
  }

}
