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

import org.waarp.common.exception.CryptoException;

/**
 * This class implements a simple Key Manager from name
 * 
 * @author frederic bregier
 * 
 */
public abstract class KeyManager {
    ConcurrentHashMap<String, KeyObject> keysConcurrentHashMap =
            new ConcurrentHashMap<String, KeyObject>();
    AtomicBoolean isInitialized = new AtomicBoolean(false);

    public abstract KeyObject createKeyObject();

    /**
     * Init the Manager from a list of filename Key, the key name is the basename minus the
     * extension of the key's type
     * 
     * @param keys
     * @return the list of wrong keys
     */
    public List<String> initFromList(List<String> keys) {
        LinkedList<String> wrong = new LinkedList<String>();
        for (String filename : keys) {
            File file = new File(filename);
            if (file.canRead()) {
                String basename = file.getName();
                int lastpos = basename.lastIndexOf('.');
                if (lastpos <= 0) {
                    wrong.add(filename);
                    continue;
                }
                String firstname = basename.substring(0, lastpos);
                int len = (int) file.length();
                byte[] key = new byte[len];
                FileInputStream inputStream = null;
                try {
                    inputStream = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    // should not be
                    wrong.add(filename);
                    continue;
                }
                int read = 0;
                int offset = 0;
                while (read > 0) {
                    try {
                        read = inputStream.read(key, offset, len);
                    } catch (IOException e) {
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
                } catch (IOException e) {
                }
                if (read < -1) {
                    // wrong
                    continue;
                }
                KeyObject keyObject = createKeyObject();
                keyObject.setSecretKey(key);
                this.setKey(firstname, keyObject);
            } else {
                wrong.add(filename);
            }
        }
        this.isInitialized.set(true);
        return wrong;
    }

    public void saveToFiles() throws CryptoException, IOException {
        Enumeration<String> names = keysConcurrentHashMap.keys();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            KeyObject key = keysConcurrentHashMap.get(name);
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
        this.keysConcurrentHashMap.put(name, keyObject);
    }

    /**
     * @param name
     * @return the key associated to the given name
     */
    public KeyObject getKey(String name) {
        return this.keysConcurrentHashMap.get(name);
    }

    /**
     * One method to get the crypted String from the given string and key
     * 
     * @param keyName
     * @param toBeCrypted
     * @return the crypted String
     * @throws Exception
     */
    public String crypt(String keyName, String toBeCrypted) throws Exception {
        KeyObject keyObject = this.getKey(keyName);
        if (keyObject == null) {
            throw new NoSuchAlgorithmException("Key does not exist: " + keyName);
        }
        return keyObject.cryptToHex(toBeCrypted);
    }

    /**
     * One method to get the uncrypted String from the given crypted string and key
     * 
     * @param keyName
     * @param toBeDecrypted
     * @return the uncrypted String
     * @throws Exception
     */
    public String decrypt(String keyName, String toBeDecrypted) throws Exception {
        KeyObject keyObject = this.getKey(keyName);
        if (keyObject == null) {
            throw new NoSuchAlgorithmException("Key does not exist: " + keyName);
        }
        return keyObject.decryptHexInString(toBeDecrypted);
    }

}
