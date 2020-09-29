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
import org.waarp.common.file.FileUtils;
import org.waarp.common.utility.WaarpStringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

/**
 * Can implements AES, ARCFOUR, Blowfish, DES, DESede, RC2, RC4<br>
 * <br>
 * The time ratio are: RC4,ARCFOUR=1; AES,RC2=1,5; DES=2; Blowfish,DESede=4<br>
 * <b>AES is the best compromise in term of security and efficiency.</b>
 */
public class DynamicKeyManager extends KeyManager {
  /**
   * Manager of Dynamic Key
   */
  private static final DynamicKeyManager manager = new DynamicKeyManager();
  /**
   * Extra information file extension
   */
  private static final String INFEXTENSION = ".inf";

  /**
   * @return the current KeyManager
   */
  public static KeyManager getInstance() {
    return manager;
  }

  @Override
  public KeyObject createKeyObject() {
    throw new InstantiationError(
        "DynamicKeyManager does not implement this function");
  }

  @Override
  public List<String> initFromList(final List<String> keys) {
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
        final String extension = basename.substring(lastpos + 1);
        int len = (int) file.length();
        final byte[] key = new byte[len];
        FileInputStream inputStream;
        try {
          inputStream = new FileInputStream(file);
        } catch (final FileNotFoundException e) {
          // should not be
          wrong.add(filename);
          continue;
        }
        int read = 1;
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
        FileUtils.close(inputStream);
        if (read < -1) {
          // wrong
          continue;
        }
        final String infFilename = filename + INFEXTENSION;
        final File infFile = new File(infFilename);
        inputStream = null;
        try {
          inputStream = new FileInputStream(infFile);
        } catch (final FileNotFoundException e) {
          // should not be
          wrong.add(filename);
          continue;
        }
        KeyObject keyObject;
        try {
          final int keySize = inputStream.read();
          final String algo = readString(inputStream);
          if (algo == null) {
            wrong.add(filename);
            continue;
          }
          final String instance = readString(inputStream);
          if (instance == null) {
            wrong.add(filename);
            continue;
          }
          keyObject = new DynamicKeyObject(keySize, algo, instance, extension);
        } catch (final IOException e1) {
          wrong.add(filename);
          continue;
        } finally {
          FileUtils.close(inputStream);
        }
        keyObject.setSecretKey(key);
        setKey(firstname, keyObject);
      } else {
        wrong.add(filename);
      }
    }
    isInitialized.set(true);
    return wrong;
  }

  /**
   * Specific functions to ease the process of reading the "inf" file
   *
   * @param inputStream
   *
   * @return the String that should be read
   */
  private String readString(final FileInputStream inputStream) {
    final int len;
    try {
      len = inputStream.read();
    } catch (final IOException e1) {
      return null;
    }
    final byte[] readbyte = new byte[len];
    for (int i = 0; i < len; i++) {
      try {
        readbyte[i] = (byte) inputStream.read();
      } catch (final IOException e) {
        return null;
      }
    }
    return new String(readbyte, WaarpStringUtils.UTF8);
  }

  @Override
  public void saveToFiles() throws CryptoException, IOException {
    final Enumeration<String> names = keysConcurrentHashMap.keys();
    while (names.hasMoreElements()) {
      final String name = names.nextElement();
      final KeyObject key = keysConcurrentHashMap.get(name);
      key.saveSecretKey(new File(name + '.' + key.getFileExtension()));
      final FileOutputStream outputStream = new FileOutputStream(
          new File(name + '.' + key.getFileExtension() + INFEXTENSION));
      try {
        outputStream.write(key.getKeySize());
        final String algo = key.getAlgorithm();
        final String instance = key.getInstance();
        outputStream.write(algo.length());
        outputStream.write(algo.getBytes(WaarpStringUtils.UTF8));
        outputStream.write(instance.length());
        outputStream.write(instance.getBytes(WaarpStringUtils.UTF8));
      } finally {
        FileUtils.close(outputStream);
      }
    }
  }

}
