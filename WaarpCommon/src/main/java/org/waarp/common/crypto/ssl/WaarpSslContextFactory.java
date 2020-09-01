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

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import static org.waarp.common.digest.WaarpBC.*;

/**
 * SSL ContextFactory for Netty.
 */
public class WaarpSslContextFactory {
  protected static final int DEFAULT_SESSIONCACHE_TIMEOUTSEC = 60;

  protected static final int DEFAULT_SESSIONCACHE_SIZE = 1024;

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpSslContextFactory.class);

  static {
    initializedTlsContext();
  }

  /**
   *
   */
  private final SSLContext serverContext;

  /**
   *
   */
  private final SSLContext clientContext;

  private boolean needClientAuthentication;

  /**
   * Create both CONTEXT
   *
   * @param ggSecureKeyStore
   */
  public WaarpSslContextFactory(final WaarpSecureKeyStore ggSecureKeyStore) {
    // Both construct Client and Server mode
    serverContext = initSslContextFactory(ggSecureKeyStore, true);
    clientContext = initSslContextFactory(ggSecureKeyStore, false);
  }

  /**
   * Create only one of the CONTEXT
   *
   * @param ggSecureKeyStore
   * @param serverMode
   */
  public WaarpSslContextFactory(final WaarpSecureKeyStore ggSecureKeyStore,
                                final boolean serverMode) {
    if (serverMode) {
      serverContext = initSslContextFactory(ggSecureKeyStore, true);
      clientContext = null;
    } else {
      clientContext = initSslContextFactory(ggSecureKeyStore, false);
      serverContext = null;
    }
  }

  /**
   * @param cacheSize default being 1024
   * @param timeOutInSeconds default being 60s
   */
  public void setSessionCacheTime(final int cacheSize,
                                  final int timeOutInSeconds) {
    if (serverContext != null) {
      final SSLSessionContext sslSessionContext =
          serverContext.getServerSessionContext();
      if (sslSessionContext != null) {
        sslSessionContext.setSessionCacheSize(cacheSize);
        sslSessionContext.setSessionTimeout(timeOutInSeconds);
      }
    }
  }

  /**
   * @param ggSecureKeyStore
   * @param serverMode
   *
   * @return the SSLContext
   */
  private SSLContext initSslContextFactory(
      final WaarpSecureKeyStore ggSecureKeyStore, final boolean serverMode) {
    String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
    if (algorithm == null) {
      algorithm = "“X509";
      try {
        if (KeyManagerFactory.getInstance(algorithm) == null) {
          algorithm = "SunX509";
        }
      } catch (final NoSuchAlgorithmException e) {
        algorithm = "SunX509";
      }
    }

    final SSLContext serverContextNew;
    final SSLContext clientContextNew;
    if (serverMode) {
      try {
        // Initialize the SSLContext to work with our key managers.
        serverContextNew = getInstance();
        final WaarpSecureTrustManagerFactory secureTrustManagerFactory =
            ggSecureKeyStore.getSecureTrustManagerFactory();
        needClientAuthentication =
            secureTrustManagerFactory.needAuthentication();
        if (secureTrustManagerFactory.hasTrustStore()) {
          logger.debug("Has TrustManager");
          serverContextNew
              .init(ggSecureKeyStore.getKeyManagerFactory().getKeyManagers(),
                    secureTrustManagerFactory.getTrustManagers(),
                    getSecureRandom());
        } else {
          logger.debug("No TrustManager");
          serverContextNew
              .init(ggSecureKeyStore.getKeyManagerFactory().getKeyManagers(),
                    null, getSecureRandom());
        }
        final SSLSessionContext sslSessionContext =
            serverContextNew.getServerSessionContext();
        if (sslSessionContext != null) {
          sslSessionContext.setSessionCacheSize(DEFAULT_SESSIONCACHE_SIZE);
          sslSessionContext.setSessionTimeout(DEFAULT_SESSIONCACHE_TIMEOUTSEC);
        }
        return serverContextNew;
      } catch (final Throwable e) {
        logger.error("Failed to initialize the server-side SSLContext", e);
        throw new Error("Failed to initialize the server-side SSLContext", e);
      }
    } else {
      try {
        clientContextNew = getInstance();
        final WaarpSecureTrustManagerFactory secureTrustManagerFactory =
            ggSecureKeyStore.getSecureTrustManagerFactory();
        needClientAuthentication =
            secureTrustManagerFactory.needAuthentication();
        if (secureTrustManagerFactory.hasTrustStore()) {
          logger.debug("Has TrustManager");
          clientContextNew
              .init(ggSecureKeyStore.getKeyManagerFactory().getKeyManagers(),
                    secureTrustManagerFactory.getTrustManagers(),
                    getSecureRandom());
        } else {
          logger.debug("No TrustManager");
          clientContextNew
              .init(ggSecureKeyStore.getKeyManagerFactory().getKeyManagers(),
                    null, getSecureRandom());
        }
        final SSLSessionContext sslSessionContext =
            clientContextNew.getServerSessionContext();
        if (sslSessionContext != null) {
          sslSessionContext.setSessionCacheSize(DEFAULT_SESSIONCACHE_SIZE);
          sslSessionContext.setSessionTimeout(DEFAULT_SESSIONCACHE_TIMEOUTSEC);
        }
        return clientContextNew;
      } catch (final Throwable e) {
        logger.error("Failed to initialize the client-side SSLContext", e);
        throw new Error("Failed to initialize the client-side SSLContext", e);
      }
    }
  }

  /**
   * @return the Server Context
   */
  public SSLContext getServerContext() {
    return serverContext;
  }

  /**
   * @return the Client Context
   */
  public SSLContext getClientContext() {
    return clientContext;
  }

  /**
   * To be called before adding as first entry in the Initializer as<br>
   * pipeline.addLast("ssl", sslhandler);<br>
   *
   * @param serverMode True if in Server Mode, else False in Client
   *     mode
   * @param needClientAuth True if the client needs to be
   *     authenticated
   *     (only if serverMode is True)
   *
   * @return the sslhandler
   */
  public WaarpSslHandler initInitializer(final boolean serverMode,
                                         final boolean needClientAuth) {
    // Add SSL handler first to encrypt and decrypt everything.
    final SSLEngine engine;
    logger.debug("Has TrustManager? " + needClientAuth + " Is ServerMode? " +
                 serverMode);
    if (serverMode) {
      engine = getServerContext().createSSLEngine();
      engine.setUseClientMode(false);
      engine.setNeedClientAuth(needClientAuth);
    } else {
      engine = getClientContext().createSSLEngine();
      engine.setUseClientMode(true);
    }
    return new WaarpSslHandler(engine);
  }

  /**
   * To be called before adding as first entry in the Initializer as<br>
   * pipeline.addLast("ssl", sslhandler);<br>
   *
   * @param serverMode True if in Server Mode, else False in Client
   *     mode
   * @param needClientAuth True if the client needs to be
   *     authenticated
   *     (only if serverMode is True)
   * @param host Host for which a resume is allowed
   * @param port port associated with the host for which a resume is
   *     allowed
   *
   * @return the sslhandler
   */
  public WaarpSslHandler initInitializer(final boolean serverMode,
                                         final boolean needClientAuth,
                                         final String host, final int port) {
    // Add SSL handler first to encrypt and decrypt everything.
    final SSLEngine engine;
    logger.debug("Has TrustManager? " + needClientAuth + " Is ServerMode? " +
                 serverMode);
    if (serverMode) {
      engine = getServerContext().createSSLEngine(host, port);
      engine.setUseClientMode(false);
      engine.setNeedClientAuth(needClientAuth);
    } else {
      engine = getClientContext().createSSLEngine(host, port);
      engine.setUseClientMode(true);
    }
    return new WaarpSslHandler(engine);
  }

  /**
   * @return True if the associated KeyStore has a TrustStore
   */
  public boolean needClientAuthentication() {
    return needClientAuthentication;
  }
}
