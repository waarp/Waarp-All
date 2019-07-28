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

package org.waarp.common.digest;

import io.netty.handler.ssl.OpenSsl;

import javax.net.ssl.SSLContext;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;

public class WaarpBC {
  public static final String PROTOCOL = "TLS";
  private static volatile boolean initialized = false;

  static {
    initializedTlsContext();
  }

  public static void initializedTlsContext() {
    if (!initialized) {
      addBcProvider();
      registerRandomSecure();
      initialized = true;
    }
  }

  /**
   * Called at first
   */
  private static final void addBcProvider() {
    OpenSsl.isAvailable();
  }

  /**
   * To fix issue on SecureRandom using bad algotithm
   * </br>
   * Called at second place
   */
  private static void registerRandomSecure() {
    if (System.getProperty("os.name").contains("Windows")) {
      Provider provider = Security.getProvider("SunMSCAPI");
      if (provider != null) {
        Security.removeProvider(provider.getName());
        Security.insertProviderAt(provider, 1);
      }
    } else {
      System.setProperty("java.security.egd", "file:/dev/./urandom");
      Provider provider = Security.getProvider("SUN");
      final String type = "SecureRandom";
      final String alg = "NativePRNGNonBlocking";
      if (provider != null) {
        final String name = String.format("%s.%s", type, alg);
        final Provider.Service service = provider.getService(type, alg);
        Security.insertProviderAt(new Provider(name, provider.getVersion(),
                                               "Waarp quick fix for SecureRandom using urandom") {
          {
            System.setProperty(name, service.getClassName());
          }

        }, 1);
      }
    }
  }

  public static SecureRandom getSecureRandom() {
    if (System.getProperty("os.name").contains("Windows")) {
      try {
        return SecureRandom.getInstance("Windows-PRNG", "SunMSCAPI");
      } catch (NoSuchAlgorithmException e) {
        return new SecureRandom();
      } catch (NoSuchProviderException e) {
        return new SecureRandom();
      }
    } else {
      try {
        return SecureRandom.getInstance("NativePRNGNonBlocking", "SUN");
      } catch (NoSuchAlgorithmException e) {
        return new SecureRandom();
      } catch (NoSuchProviderException e) {
        return new SecureRandom();
      }
    }
  }

  public static SSLContext getInstance()
      throws NoSuchAlgorithmException, NoSuchProviderException {
    return SSLContext.getInstance(PROTOCOL);
  }
}
