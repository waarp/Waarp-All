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

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Waarp X509 Trust Manager implementation
 */
public class WaarpX509TrustManager implements X509TrustManager {
  private static final X509Certificate[] X_509_CERTIFICATES_0_LENGTH =
      new X509Certificate[0];
  /**
   * First using default X509TrustManager returned by the global TrustManager.
   * Then delegate decisions to it,
   * and fall back to the logic in this class if the default doesn't trust it.
   */
  private final X509TrustManager defaultX509TrustManager;

  /**
   * Create an "always-valid" X509TrustManager
   */
  public WaarpX509TrustManager() {
    defaultX509TrustManager = null;
  }

  /**
   * Create a "default" X509TrustManager
   *
   * @param tmf
   *
   * @throws CryptoException
   */
  public WaarpX509TrustManager(final TrustManagerFactory tmf)
      throws CryptoException {
    final TrustManager[] tms = tmf.getTrustManagers();
    /*
     * Iterate over the returned trustmanagers, look for an instance of X509TrustManager and use it as the default
     */
    for (final TrustManager tm : tms) {
      if (tm instanceof X509TrustManager) {
        defaultX509TrustManager = (X509TrustManager) tm;
        return;
      }
    }
    /*
     * Could not initialize, maybe try to build it from scratch?
     */
    throw new CryptoException("Cannot initialize the WaarpX509TrustManager");
  }

  @Override
  public void checkClientTrusted(final X509Certificate[] arg0,
                                 final String arg1)
      throws CertificateException {
    if (defaultX509TrustManager == null) {
      return; // valid
    }
    defaultX509TrustManager.checkClientTrusted(arg0, arg1);
  }

  @Override
  public void checkServerTrusted(final X509Certificate[] arg0,
                                 final String arg1)
      throws CertificateException {
    if (defaultX509TrustManager == null) {
      return; // valid
    }
    defaultX509TrustManager.checkServerTrusted(arg0, arg1);
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    if (defaultX509TrustManager == null) {
      return X_509_CERTIFICATES_0_LENGTH; // none valid
    }
    return defaultX509TrustManager.getAcceptedIssuers();
  }

}
