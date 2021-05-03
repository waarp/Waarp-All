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

import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;

public class WaarpBC {
  public static final String PROTOCOL = "TLS";
  public static final long DEFAULT_SESSIONCACHE_TIMEOUTSEC = 60;
  public static final long DEFAULT_SESSIONCACHE_SIZE = 20480;
  private static volatile boolean initialized = false;
  private static boolean specialSecureRandom = false;

  static {
    initializedTlsContext();
  }

  public static void initializedTlsContext() {
    try {
      if (!initialized) {
        addBcProvider();
        registerRandomSecure();
        initialized = true;
      }
    } catch (final Throwable throwable) {//NOSONAR
      throwable.printStackTrace();//NOSONAR
      System.err //NOSONAR
                 .println("Error occurs at startup: " +//NOSONAR
                          throwable.getMessage());//NOSONAR
    }
  }

  /**
   * Called at first
   */
  private static void addBcProvider() {
    OpenSsl.isAvailable();
  }

  /**
   * To fix issue on SecureRandom using bad algotithm
   * </br>
   * Called at second place
   */
  private static void registerRandomSecure() {
    if (System.getProperty("os.name").contains("Windows")) {
      final Provider provider = Security.getProvider("SunMSCAPI");
      if (provider != null) {
        Security.removeProvider(provider.getName());
        Security.insertProviderAt(provider, 1);
        specialSecureRandom = true;
      }
    } else {
      System.setProperty("java.security.egd", "file:/dev/./urandom");
      final Provider provider = Security.getProvider("SUN");
      final String type = "SecureRandom";
      final String alg = "NativePRNGNonBlocking";
      if (provider != null) {
        final String name = String.format("%s.%s", type, alg);
        final Provider.Service service = provider.getService(type, alg);
        if (service != null) {
          Security.insertProviderAt(new Provider(name, provider.getVersion(),
                                                 "Waarp quick fix for SecureRandom using urandom") {
            private static final long serialVersionUID = 1001L;

            {
              System.setProperty(name, service.getClassName());
            }

          }, 1);
          specialSecureRandom = true;
        }
      }
    }
  }

  public static SecureRandom getSecureRandom() {
    if (!specialSecureRandom) {
      return new SecureRandom();
    }
    if (System.getProperty("os.name").contains("Windows")) {
      try {
        return SecureRandom.getInstance("Windows-PRNG", "SunMSCAPI");
      } catch (final NoSuchAlgorithmException e) {
        return new SecureRandom();
      } catch (final NoSuchProviderException e) {
        return new SecureRandom();
      }
    } else {
      try {
        return SecureRandom.getInstance("NativePRNGNonBlocking", "SUN");
      } catch (final NoSuchAlgorithmException e) {
        return new SecureRandom();
      } catch (final NoSuchProviderException e) {
        return new SecureRandom();
      }
    }
  }

  public static SslContext getInstanceForServer(
      final KeyManagerFactory keyManagerFactory,
      final X509Certificate[] x509Certificates,
      final boolean clientNeedAuthentication, final boolean startTls)
      throws SSLException {
    final SslContextBuilder builder =
        SslContextBuilder.forServer(keyManagerFactory)
                         .sslProvider(SslContext.defaultServerProvider());
    if (x509Certificates != null) {
      builder.trustManager(x509Certificates);
    }
    builder.clientAuth(
        clientNeedAuthentication? ClientAuth.REQUIRE : ClientAuth.NONE);
    builder.sessionCacheSize(DEFAULT_SESSIONCACHE_SIZE)
           .sessionTimeout(DEFAULT_SESSIONCACHE_TIMEOUTSEC).startTls(startTls);
    return builder.build();
  }

  public static SslContext getInstanceForClient(
      final KeyManagerFactory keyManagerFactory,
      final X509Certificate[] x509Certificates)
      throws NoSuchAlgorithmException, NoSuchProviderException, SSLException {
    final SslContextBuilder builder = SslContextBuilder.forClient().sslProvider(
        SslContext.defaultClientProvider()).keyManager(keyManagerFactory);
    if (x509Certificates != null) {
      builder.trustManager(x509Certificates);
    }
    builder.sessionCacheSize(DEFAULT_SESSIONCACHE_SIZE)
           .sessionTimeout(DEFAULT_SESSIONCACHE_TIMEOUTSEC);
    return builder.build();
  }

  public static SSLContext getInstanceJDK() throws NoSuchAlgorithmException {
    return SSLContext.getInstance(PROTOCOL);
  }

  private WaarpBC() {
    // Nothing
  }
}
