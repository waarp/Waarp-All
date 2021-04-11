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
package org.waarp.common.crypto.ssl;

import org.waarp.common.exception.CryptoException;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
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

import static org.waarp.common.digest.WaarpBC.*;

/**
 * SecureKeyStore for SLL
 */
public class WaarpSecureKeyStore {
  private static final String CANNOT_SAVE_TO_FILE_KEY_STORE_INSTANCE =
      "Cannot save to file KeyStore Instance";

  private static final String CANNOT_CREATE_KEY_MANAGER_FACTORY_INSTANCE =
      "Cannot create KeyManagerFactory Instance";

  private static final String CANNOT_CREATE_KEY_STORE_INSTANCE =
      "Cannot create KeyStore Instance";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpSecureKeyStore.class);
  private static final String CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE =
      "Cannot create TrustManagerFactory Instance";
  private static final String CANNOT_CREATE_KEY_TRUST_STORE_INSTANCE =
      "Cannot create keyTrustStore Instance";
  private static final String CANNOT_SAVE_TO_FILE_KEY_TRUST_STORE_INSTANCE =
      "Cannot save to file keyTrustStore Instance";

  static {
    initializedTlsContext();
  }

  private String keyStoreFilename;
  private KeyStore keyStore;
  private KeyManagerFactory keyManagerFactory;
  private String keyStorePasswd;
  private String keyPassword;
  private WaarpSecureTrustManagerFactory secureTrustManagerFactory;
  private KeyStore keyTrustStore;
  private String trustStorePasswd;

  /**
   * Initialize empty KeyStore. No TrustStore is internally created.
   *
   * @param keyStorePasswd
   * @param keyPassword
   *
   * @throws CryptoException
   */
  public WaarpSecureKeyStore(final String keyStorePasswd,
                             final String keyPassword) throws CryptoException {
    this.keyStorePasswd = keyStorePasswd;
    this.keyPassword = keyPassword;
    try {
      keyStore = KeyStore.getInstance("JKS");
    } catch (final KeyStoreException e) {
      logger.error(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
    }
    try {
      // Empty keyStore created so null for the InputStream
      keyStore.load(null, getKeyStorePassword());
    } catch (final NoSuchAlgorithmException e) {
      logger.error(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
    } catch (final CertificateException e) {
      logger.error(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
    } catch (final FileNotFoundException e) {
      logger.error(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
    } catch (final IOException e) {
      logger.error(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
    }
    initKeyManagerFactory();
  }

  /**
   * Initialize the SecureKeyStore with no TrustStore from file
   *
   * @param keyStoreFilename
   * @param keyStorePasswd
   * @param keyPassword
   *
   * @throws CryptoException
   */
  public WaarpSecureKeyStore(final String keyStoreFilename,
                             final String keyStorePasswd,
                             final String keyPassword) throws CryptoException {
    initKeyStore(keyStoreFilename, keyStorePasswd, keyPassword);
  }

  /**
   * Initialize the SecureKeyStore and TrustStore from files
   *
   * @param keyStoreFilename
   * @param keyStorePasswd
   * @param keyPassword
   * @param trustStoreFilename if Null, no TrustKeyStore will be
   *     created
   * @param trustStorePasswd
   * @param needClientAuthent True if the TrustStore is also used for
   *     Client
   *     Authentication
   *
   * @throws CryptoException
   */
  public WaarpSecureKeyStore(final String keyStoreFilename,
                             final String keyStorePasswd,
                             final String keyPassword,
                             final String trustStoreFilename,
                             final String trustStorePasswd,
                             final boolean needClientAuthent)
      throws CryptoException {
    // Create the KeyStore
    initKeyStore(keyStoreFilename, keyStorePasswd, keyPassword);
    // Now create the TrustKeyStore
    if (trustStoreFilename != null) {
      initTrustStore(trustStoreFilename, trustStorePasswd, needClientAuthent);
    } else {
      initEmptyTrustStore();
    }
  }

  /**
   * Initialize the SecureKeyStore with no TrustStore from file
   *
   * @param keystoreFilename
   * @param keystorePasswd
   * @param keyPasswordNew
   *
   * @throws CryptoException
   */
  public void initKeyStore(final String keystoreFilename,
                           final String keystorePasswd,
                           final String keyPasswordNew) throws CryptoException {
    keyStoreFilename = keystoreFilename;
    keyStorePasswd = keystorePasswd;
    keyPassword = keyPasswordNew;
    // First keyStore itself
    try {
      keyStore = KeyStore.getInstance("JKS");
    } catch (final KeyStoreException e) {
      logger.error(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
    }
    FileInputStream inputStream = null;
    try {
      final File temp = new File(keystoreFilename).getAbsoluteFile();
      inputStream = new FileInputStream(temp);
      keyStore.load(inputStream, getKeyStorePassword());
    } catch (final NoSuchAlgorithmException e) {
      logger.error(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
    } catch (final CertificateException e) {
      logger.error(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
    } catch (final FileNotFoundException e) {
      logger.error(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
    } catch (final IOException e) {
      logger.error(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_KEY_STORE_INSTANCE, e);
    } finally {
      FileUtils.close(inputStream);
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
      keyManagerFactory = KeyManagerFactory
          .getInstance(KeyManagerFactory.getDefaultAlgorithm());
    } catch (final NoSuchAlgorithmException e) {
      logger.error(CANNOT_CREATE_KEY_MANAGER_FACTORY_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_KEY_MANAGER_FACTORY_INSTANCE, e);
    }
    try {
      keyManagerFactory.init(keyStore, getCertificatePassword());
    } catch (final UnrecoverableKeyException e) {
      logger.error(CANNOT_CREATE_KEY_MANAGER_FACTORY_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_KEY_MANAGER_FACTORY_INSTANCE, e);
    } catch (final KeyStoreException e) {
      logger.error(CANNOT_CREATE_KEY_MANAGER_FACTORY_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_KEY_MANAGER_FACTORY_INSTANCE, e);
    } catch (final NoSuchAlgorithmException e) {
      logger.error(CANNOT_CREATE_KEY_MANAGER_FACTORY_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_KEY_MANAGER_FACTORY_INSTANCE, e);
    }
  }

  /**
   * Delete a Key from the KeyStore based on its alias
   *
   * @param alias
   *
   * @return True if entry is deleted
   */
  public boolean deleteKeyFromKeyStore(final String alias) {
    try {
      keyStore.deleteEntry(alias);
    } catch (final KeyStoreException e) {
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
   *
   * @return True if entry is added
   */
  public boolean setKeytoKeyStore(final String alias, final Key key,
                                  final Certificate[] chain) {
    try {
      keyStore.setKeyEntry(alias, key, getCertificatePassword(), chain);
    } catch (final KeyStoreException e) {
      logger.error("Cannot add Key and Certificates to KeyStore Instance", e);
      return false;
    }
    return true;
  }

  /**
   * Save a KeyStore to a file
   *
   * @param filename
   *
   * @return True if keyStore is saved to file
   */
  public boolean saveKeyStore(final String filename) {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(filename);
      try {
        keyStore.store(fos, getKeyStorePassword());
      } catch (final KeyStoreException e) {
        logger.error(CANNOT_SAVE_TO_FILE_KEY_STORE_INSTANCE, e);
        return false;
      } catch (final NoSuchAlgorithmException e) {
        logger.error(CANNOT_SAVE_TO_FILE_KEY_STORE_INSTANCE, e);
        return false;
      } catch (final CertificateException e) {
        logger.error(CANNOT_SAVE_TO_FILE_KEY_STORE_INSTANCE, e);
        return false;
      } catch (final IOException e) {
        logger.error(CANNOT_SAVE_TO_FILE_KEY_STORE_INSTANCE, e);
        return false;
      }
    } catch (final FileNotFoundException e) {
      logger.error(CANNOT_SAVE_TO_FILE_KEY_STORE_INSTANCE, e);
      return false;
    } finally {
      FileUtils.close(fos);
    }
    return true;
  }

  /**
   * Initialize the TrustStore from a filename and its password
   *
   * @param truststoreFilename
   * @param truststorePasswd
   * @param needClientAuthent True if the TrustStore is also to
   *     authenticate
   *     clients
   *
   * @throws CryptoException
   */
  public void initTrustStore(final String truststoreFilename,
                             final String truststorePasswd,
                             final boolean needClientAuthent)
      throws CryptoException {
    trustStorePasswd = truststorePasswd;
    try {
      keyTrustStore = KeyStore.getInstance("JKS");
    } catch (final KeyStoreException e) {
      logger.error(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE,
                                e);
    }
    FileInputStream inputStream = null;
    try {
      final File temp = new File(truststoreFilename).getAbsoluteFile();
      inputStream = new FileInputStream(temp);
      keyTrustStore.load(inputStream, getKeyTrustStorePassword());
    } catch (final NoSuchAlgorithmException e) {
      logger.error(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE,
                                e);
    } catch (final CertificateException e) {
      logger.error(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE,
                                e);
    } catch (final FileNotFoundException e) {
      logger.error(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE,
                                e);
    } catch (final IOException e) {
      logger.error(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE,
                                e);
    } finally {
      FileUtils.close(inputStream);
    }
    final TrustManagerFactory trustManagerFactory;
    try {
      trustManagerFactory = TrustManagerFactory
          .getInstance(KeyManagerFactory.getDefaultAlgorithm());
    } catch (final NoSuchAlgorithmException e1) {
      logger.error(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE, e1);
      throw new CryptoException(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE,
                                e1);
    }
    try {
      trustManagerFactory.init(keyTrustStore);
    } catch (final KeyStoreException e1) {
      logger.error(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE, e1);
      throw new CryptoException(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE,
                                e1);
    }
    try {
      secureTrustManagerFactory =
          new WaarpSecureTrustManagerFactory(trustManagerFactory,
                                             needClientAuthent);
    } catch (final CryptoException e) {
      logger.error(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE, e);
      throw new CryptoException(CANNOT_CREATE_TRUST_MANAGER_FACTORY_INSTANCE,
                                e);
    }
  }

  /**
   * Initialize an empty TrustStore
   *
   * @return True if correctly initialized empty
   */
  public boolean initEmptyTrustStore() {
    trustStorePasswd = "secret";//NOSONAR
    try {
      keyTrustStore = KeyStore.getInstance("JKS");
    } catch (final KeyStoreException e) {
      logger.error(CANNOT_CREATE_KEY_TRUST_STORE_INSTANCE, e);
      return false;
    }
    try {
      // Empty keyTrustStore created so null for the InputStream
      keyTrustStore.load(null, getKeyTrustStorePassword());
    } catch (final NoSuchAlgorithmException e) {
      logger.error(CANNOT_CREATE_KEY_TRUST_STORE_INSTANCE, e);
      return false;
    } catch (final CertificateException e) {
      logger.error(CANNOT_CREATE_KEY_TRUST_STORE_INSTANCE, e);
      return false;
    } catch (final FileNotFoundException e) {
      logger.error(CANNOT_CREATE_KEY_TRUST_STORE_INSTANCE, e);
      return false;
    } catch (final IOException e) {
      logger.error(CANNOT_CREATE_KEY_TRUST_STORE_INSTANCE, e);
      return false;
    }
    secureTrustManagerFactory = new WaarpSecureTrustManagerFactory();
    return true;
  }

  /**
   * Delete a Key from the TrustStore based on its alias
   *
   * @param alias
   *
   * @return True if entry is deleted
   */
  public boolean deleteKeyFromTrustStore(final String alias) {
    try {
      keyStore.deleteEntry(alias);
    } catch (final KeyStoreException e) {
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
   *
   * @return True if entry is added
   */
  public boolean setKeytoTrustStore(final String alias,
                                    final Certificate cert) {
    try {
      keyStore.setCertificateEntry(alias, cert);
    } catch (final KeyStoreException e) {
      logger.error("Cannot add Certificate to keyTrustStore Instance", e);
      return false;
    }
    return true;
  }

  /**
   * Save the TrustStore to a file
   *
   * @param filename
   *
   * @return True if keyTrustStore is saved to file
   */
  public boolean saveTrustStore(final String filename) {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(filename);
      try {
        keyTrustStore.store(fos, getKeyTrustStorePassword());
      } catch (final KeyStoreException e) {
        logger.error(CANNOT_SAVE_TO_FILE_KEY_TRUST_STORE_INSTANCE, e);
        return false;
      } catch (final NoSuchAlgorithmException e) {
        logger.error(CANNOT_SAVE_TO_FILE_KEY_TRUST_STORE_INSTANCE, e);
        return false;
      } catch (final CertificateException e) {
        logger.error(CANNOT_SAVE_TO_FILE_KEY_TRUST_STORE_INSTANCE, e);
        return false;
      } catch (final IOException e) {
        logger.error(CANNOT_SAVE_TO_FILE_KEY_TRUST_STORE_INSTANCE, e);
        return false;
      }
    } catch (final FileNotFoundException e) {
      logger.error(CANNOT_SAVE_TO_FILE_KEY_TRUST_STORE_INSTANCE, e);
      return false;
    } finally {
      FileUtils.close(fos);
    }
    return true;
  }

  /**
   * Load a certificate from a filename
   *
   * @param filename
   *
   * @return the X509 Certificate from filename
   *
   * @throws CertificateException
   * @throws FileNotFoundException
   */
  public static Certificate loadX509Certificate(final String filename)
      throws CertificateException, FileNotFoundException {
    final CertificateFactory cf = CertificateFactory.getInstance("X.509");
    final FileInputStream in = new FileInputStream(filename);
    try {
      return cf.generateCertificate(in);
    } finally {
      FileUtils.close(in);
    }
  }

  /**
   * @return the certificate Password
   */
  public char[] getCertificatePassword() {
    if (keyPassword != null) {
      return keyPassword.toCharArray();
    }
    return "nopwd".toCharArray();
  }

  /**
   * @return the KeyStore Password
   */
  public char[] getKeyStorePassword() {
    if (keyStorePasswd != null) {
      return keyStorePasswd.toCharArray();
    }
    return "nopwd".toCharArray();
  }

  /**
   * @return the KeyTrustStore Password
   */
  public char[] getKeyTrustStorePassword() {
    if (trustStorePasswd != null) {
      return trustStorePasswd.toCharArray();
    }
    return "nopwd".toCharArray();
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
