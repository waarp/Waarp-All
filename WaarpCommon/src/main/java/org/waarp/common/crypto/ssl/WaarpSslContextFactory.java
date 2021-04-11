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

import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import javax.net.ssl.SSLSessionContext;
import java.net.InetSocketAddress;
import java.security.cert.X509Certificate;

import static org.waarp.common.digest.WaarpBC.*;

/**
 * SSL ContextFactory for Netty.
 */
public class WaarpSslContextFactory {

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
  private final SslContext serverContext;

  /**
   *
   */
  private final SslContext clientContext;

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
          serverContext.sessionContext();
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
  private SslContext initSslContextFactory(
      final WaarpSecureKeyStore ggSecureKeyStore, final boolean serverMode) {
    // Initialize the SSLContext to work with our key managers.
    final WaarpSecureTrustManagerFactory secureTrustManagerFactory =
        ggSecureKeyStore.getSecureTrustManagerFactory();
    needClientAuthentication = secureTrustManagerFactory.needAuthentication();
    X509Certificate[] certificates = null;
    if (secureTrustManagerFactory.hasTrustStore()) {
      logger.debug("Has TrustManager");
      certificates = secureTrustManagerFactory.getX509Certificates();
    } else {
      logger.debug("No TrustManager");
    }
    if (serverMode) {
      try {
        return getInstanceForServer(ggSecureKeyStore.getKeyManagerFactory(),
                                    certificates, needClientAuthentication);
      } catch (final Throwable e) {
        logger.error("Failed to initialize the server-side SSLContext", e);
        throw new Error("Failed to initialize the server-side SSLContext", e);
      }
    } else {
      try {
        return getInstanceForClient(ggSecureKeyStore.getKeyManagerFactory(),
                                    certificates);
      } catch (final Throwable e) {
        logger.error("Failed to initialize the client-side SSLContext", e);
        throw new Error("Failed to initialize the client-side SSLContext", e);
      }
    }
  }

  /**
   * @return the Server Context
   */
  public SslContext getServerContext() {
    return serverContext;
  }

  /**
   * @return the Client Context
   */
  public SslContext getClientContext() {
    return clientContext;
  }

  /**
   * To be called before adding as first entry in the Initializer as<br>
   * pipeline.addLast("ssl", sslhandler);<br>
   *
   * @param needClientAuth True if the client needs to be
   *     authenticated
   *     (only if serverMode is True)
   * @param channel the channel needing the SslHandler
   *
   * @return the sslhandler
   */
  public SslHandler createHandlerServer(final boolean needClientAuth,
                                        final Channel channel) {
    logger
        .debug("Has TrustManager? {} Is ServerMode? {}", needClientAuth, true);
    channel.config().setAutoRead(true);
    return getServerContext().newHandler(channel.alloc());
  }

  /**
   * To be called before adding as first entry in the Initializer as<br>
   * pipeline.addLast("ssl", sslhandler);<br>
   *
   * @param channel the channel needing the SslHandler
   *
   * @return the sslhandler
   */
  public SslHandler createHandlerClient(final SocketChannel channel) {
    logger.debug("Has TrustManager? {} Is ServerMode? {}", false, false);
    channel.config().setAutoRead(true);
    InetSocketAddress socketAddress = channel.remoteAddress();
    if (socketAddress != null) {
      logger.debug("socket {} {}", socketAddress.getHostName(),
                   socketAddress.getPort());
      return getClientContext()
          .newHandler(channel.alloc(), socketAddress.getHostName(),
                      socketAddress.getPort());
    } else {
      return getClientContext().newHandler(channel.alloc());
    }
  }

  /**
   * @return True if the associated KeyStore has a TrustStore
   */
  public boolean needClientAuthentication() {
    return needClientAuthentication;
  }
}
