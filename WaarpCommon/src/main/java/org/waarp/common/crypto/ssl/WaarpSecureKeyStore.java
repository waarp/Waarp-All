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
package org.waarp.common.crypto.ssl;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.waarp.common.exception.CryptoException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

/**
 * SecureKeyStore for SLL
 * 
 * @author Frederic Bregier
 * 
 */
public class WaarpSecureKeyStore {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(WaarpSecureKeyStore.class);

    private String keyStoreFilename;
    private KeyStore keyStore;
    private KeyManagerFactory keyManagerFactory;
    private String keyStorePasswd;
    private String keyPassword;
    private WaarpSecureTrustManagerFactory secureTrustManagerFactory;
    private String trustStoreFilename;
    private KeyStore keyTrustStore;
    private String trustStorePasswd;

    /**
     * Initialize empty KeyStore. No TrustStore is internally created.
     * 
     * @param _keyStorePasswd
     * @param _keyPassword
     * @throws CryptoException
     */
    public WaarpSecureKeyStore(String _keyStorePasswd, String _keyPassword) throws CryptoException {
        keyStorePasswd = _keyStorePasswd;
        keyPassword = _keyPassword;
        try {
            keyStore = KeyStore.getInstance("JKS");
        } catch (KeyStoreException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        }
        try {
            // Empty keyStore created so null for the InputStream
            keyStore.load(null,
                    getKeyStorePassword());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        } catch (CertificateException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        } catch (FileNotFoundException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        } catch (IOException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        }
        initKeyManagerFactory();
    }

    /**
     * Initialize the SecureKeyStore with no TrustStore from file
     * 
     * @param keyStoreFilename
     * @param _keyStorePasswd
     * @param _keyPassword
     * @throws CryptoException
     */
    public WaarpSecureKeyStore(
            String keyStoreFilename, String _keyStorePasswd, String _keyPassword)
            throws CryptoException {
        initKeyStore(keyStoreFilename, _keyStorePasswd, _keyPassword);
    }

    /**
     * Initialize the SecureKeyStore and TrustStore from files
     * 
     * @param keyStoreFilename
     * @param _keyStorePasswd
     * @param _keyPassword
     * @param trustStoreFilename
     *            if Null, no TrustKeyStore will be created
     * @param _trustStorePasswd
     * @param needClientAuthent
     *            True if the TrustStore is also used for Client Authentication
     * @throws CryptoException
     */
    public WaarpSecureKeyStore(
            String keyStoreFilename, String _keyStorePasswd, String _keyPassword,
            String trustStoreFilename, String _trustStorePasswd, boolean needClientAuthent)
            throws CryptoException {
        // Create the KeyStore
        initKeyStore(keyStoreFilename, _keyStorePasswd, _keyPassword);
        // Now create the TrustKeyStore
        if (trustStoreFilename != null) {
            initTrustStore(trustStoreFilename, _trustStorePasswd, needClientAuthent);
        } else {
            initEmptyTrustStore();
        }
    }

    /**
     * Initialize the SecureKeyStore with no TrustStore from file
     * 
     * @param _keyStoreFilename
     * @param _keyStorePasswd
     * @param _keyPassword
     * @throws CryptoException
     */
    public void initKeyStore(String _keyStoreFilename, String _keyStorePasswd, String _keyPassword)
            throws CryptoException {
        keyStoreFilename = _keyStoreFilename;
        keyStorePasswd = _keyStorePasswd;
        keyPassword = _keyPassword;
        // First keyStore itself
        try {
            keyStore = KeyStore.getInstance("JKS");
        } catch (KeyStoreException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(keyStoreFilename);
            keyStore.load(inputStream, getKeyStorePassword());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        } catch (CertificateException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        } catch (FileNotFoundException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        } catch (IOException e) {
            logger.error("Cannot create KeyStore Instance", e);
            throw new CryptoException("Cannot create KeyStore Instance", e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
            }
        }
        initKeyManagerFactory();
    }

    /**
     * Init KeyManagerFactory
     * 
     * @throws CryptoException
     */
    void initKeyManagerFactory() throws CryptoException {
        try {
            keyManagerFactory = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            // "SunX509");
        } catch (NoSuchAlgorithmException e) {
            logger.error("Cannot create KeyManagerFactory Instance", e);
            throw new CryptoException("Cannot create KeyManagerFactory Instance", e);
        }
        try {
            keyManagerFactory.init(keyStore, getCertificatePassword());
        } catch (UnrecoverableKeyException e) {
            logger.error("Cannot create KeyManagerFactory Instance", e);
            throw new CryptoException("Cannot create KeyManagerFactory Instance", e);
        } catch (KeyStoreException e) {
            logger.error("Cannot create KeyManagerFactory Instance", e);
            throw new CryptoException("Cannot create KeyManagerFactory Instance", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("Cannot create KeyManagerFactory Instance", e);
            throw new CryptoException("Cannot create KeyManagerFactory Instance", e);
        }
    }

    /**
     * Delete a Key from the KeyStore based on its alias
     * 
     * @param alias
     * @return True if entry is deleted
     */
    public boolean deleteKeyFromKeyStore(String alias) {
        try {
            keyStore.deleteEntry(alias);
        } catch (KeyStoreException e) {
            logger.error("Cannot delete Key from KeyStore Instance", e);
            return false;
        }
        return true;
    }

    /**
     * Add a Key and its certificates into the KeyStore based on its alias
     * 
     * @param alias
     * @param key
     * @param chain
     * @return True if entry is added
     */
    public boolean setKeytoKeyStore(String alias, Key key, Certificate[] chain) {
        try {
            keyStore.setKeyEntry(alias, key, getCertificatePassword(), chain);
        } catch (KeyStoreException e) {
            logger.error("Cannot add Key and Certificates to KeyStore Instance", e);
            return false;
        }
        return true;
    }

    /**
     * Save a KeyStore to a file
     * 
     * @param filename
     * @return True if keyStore is saved to file
     */
    public boolean saveKeyStore(String filename) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filename);
            try {
                keyStore.store(fos, getKeyStorePassword());
            } catch (KeyStoreException e) {
                logger.error("Cannot save to file KeyStore Instance", e);
                return false;
            } catch (NoSuchAlgorithmException e) {
                logger.error("Cannot save to file KeyStore Instance", e);
                return false;
            } catch (CertificateException e) {
                logger.error("Cannot save to file KeyStore Instance", e);
                return false;
            } catch (IOException e) {
                logger.error("Cannot save to file KeyStore Instance", e);
                return false;
            }
        } catch (FileNotFoundException e) {
            logger.error("Cannot save to file KeyStore Instance", e);
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
            }
        }
        return true;
    }

    /**
     * Initialize the TrustStore from a filename and its password
     * 
     * @param _trustStoreFilename
     * @param _trustStorePasswd
     * @param needClientAuthent
     *            True if the TrustStore is also to authenticate clients
     * @throws CryptoException
     */
    public void initTrustStore(String _trustStoreFilename, String _trustStorePasswd,
            boolean needClientAuthent) throws CryptoException {
        trustStoreFilename = _trustStoreFilename;
        trustStorePasswd = _trustStorePasswd;
        try {
            keyTrustStore = KeyStore.getInstance("JKS");
        } catch (KeyStoreException e) {
            logger.error("Cannot create TrustManagerFactory Instance", e);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e);
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(trustStoreFilename);
            keyTrustStore.load(inputStream, getKeyTrustStorePassword());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Cannot create TrustManagerFactory Instance", e);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e);
        } catch (CertificateException e) {
            logger.error("Cannot create TrustManagerFactory Instance", e);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e);
        } catch (FileNotFoundException e) {
            logger.error("Cannot create TrustManagerFactory Instance", e);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e);
        } catch (IOException e) {
            logger.error("Cannot create TrustManagerFactory Instance", e);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e2) {
            }
        }
        TrustManagerFactory trustManagerFactory = null;
        try {
            trustManagerFactory = TrustManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
        } catch (NoSuchAlgorithmException e1) {
            logger.error("Cannot create TrustManagerFactory Instance", e1);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e1);
        }
        try {
            trustManagerFactory.init(keyTrustStore);
        } catch (KeyStoreException e1) {
            logger.error("Cannot create TrustManagerFactory Instance", e1);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e1);
        }
        try {
            secureTrustManagerFactory = new WaarpSecureTrustManagerFactory(trustManagerFactory,
                    needClientAuthent);
        } catch (CryptoException e) {
            logger.error("Cannot create TrustManagerFactory Instance", e);
            throw new CryptoException("Cannot create TrustManagerFactory Instance", e);
        }
    }

    /**
     * Initialize an empty TrustStore
     * 
     * @return True if correctly initialized empty
     */
    public boolean initEmptyTrustStore() {
        trustStorePasswd = "secret";
        try {
            keyTrustStore = KeyStore.getInstance("JKS");
        } catch (KeyStoreException e) {
            logger.error("Cannot create keyTrustStore Instance", e);
            return false;
        }
        try {
            // Empty keyTrustStore created so null for the InputStream
            keyTrustStore.load(null,
                    getKeyTrustStorePassword());
        } catch (NoSuchAlgorithmException e) {
            logger.error("Cannot create keyTrustStore Instance", e);
            return false;
        } catch (CertificateException e) {
            logger.error("Cannot create keyTrustStore Instance", e);
            return false;
        } catch (FileNotFoundException e) {
            logger.error("Cannot create keyTrustStore Instance", e);
            return false;
        } catch (IOException e) {
            logger.error("Cannot create keyTrustStore Instance", e);
            return false;
        }
        secureTrustManagerFactory = new WaarpSecureTrustManagerFactory();
        return true;
    }

    /**
     * Delete a Key from the TrustStore based on its alias
     * 
     * @param alias
     * @return True if entry is deleted
     */
    public boolean deleteKeyFromTrustStore(String alias) {
        try {
            keyStore.deleteEntry(alias);
        } catch (KeyStoreException e) {
            logger.error("Cannot delete Key from keyTrustStore Instance", e);
            return false;
        }
        return true;
    }

    /**
     * Add a Certificate into the TrustStore based on its alias
     * 
     * @param alias
     * @param cert
     * @return True if entry is added
     */
    public boolean setKeytoTrustStore(String alias, Certificate cert) {
        try {
            keyStore.setCertificateEntry(alias, cert);
        } catch (KeyStoreException e) {
            logger.error("Cannot add Certificate to keyTrustStore Instance", e);
            return false;
        }
        return true;
    }

    /**
     * Save the TrustStore to a file
     * 
     * @param filename
     * @return True if keyTrustStore is saved to file
     */
    public boolean saveTrustStore(String filename) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(filename);
            try {
                keyTrustStore.store(fos, getKeyTrustStorePassword());
            } catch (KeyStoreException e) {
                logger.error("Cannot save to file keyTrustStore Instance", e);
                return false;
            } catch (NoSuchAlgorithmException e) {
                logger.error("Cannot save to file keyTrustStore Instance", e);
                return false;
            } catch (CertificateException e) {
                logger.error("Cannot save to file keyTrustStore Instance", e);
                return false;
            } catch (IOException e) {
                logger.error("Cannot save to file keyTrustStore Instance", e);
                return false;
            }
        } catch (FileNotFoundException e) {
            logger.error("Cannot save to file keyTrustStore Instance", e);
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
            }
        }
        return true;
    }

    /**
     * Load a certificate from a filename
     * 
     * @param filename
     * @return the X509 Certificate from filename
     * @throws CertificateException
     * @throws FileNotFoundException
     */
    public static Certificate loadX509Certificate(String filename)
            throws CertificateException, FileNotFoundException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        FileInputStream in = new FileInputStream(filename);
        try {
            return cf.generateCertificate(in);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * @return the certificate Password
     */
    public char[] getCertificatePassword() {
        if (keyPassword != null) {
            return keyPassword.toCharArray();
        }
        return "secret".toCharArray();
    }

    /**
     * @return the KeyStore Password
     */
    public char[] getKeyStorePassword() {
        if (keyStorePasswd != null) {
            return keyStorePasswd.toCharArray();
        }
        return "secret".toCharArray();
    }

    /**
     * @return the KeyTrustStore Password
     */
    public char[] getKeyTrustStorePassword() {
        if (trustStorePasswd != null) {
            return trustStorePasswd.toCharArray();
        }
        return "secret".toCharArray();
    }

    /**
     * @return the KeyStore Filename
     */
    public String getKeyStoreFilename() {
        return keyStoreFilename;
    }

    /**
     * @return the secureTrustManagerFactory
     */
    public WaarpSecureTrustManagerFactory getSecureTrustManagerFactory() {
        return secureTrustManagerFactory;
    }

    /**
     * @return the keyManagerFactory
     */
    public KeyManagerFactory getKeyManagerFactory() {
        return keyManagerFactory;
    }

}
