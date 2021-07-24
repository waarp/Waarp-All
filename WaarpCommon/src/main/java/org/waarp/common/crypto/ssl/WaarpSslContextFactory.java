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

  private static final long DEFAULT_HANDSHAKE_TIMEOUT = 10000;
  public static final String HAS_TRUST_MANAGER_IS_SERVER_MODE =
      "Has TrustManager? {} Is ServerMode? {}";

  static {
    initializedTlsContext();
  }

  /**
   *
   */
  private final SslContext serverContext;
  private final SslContext serverContextStartTls;

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
    serverContext = initSslContextFactory(ggSecureKeyStore, true, false);
    serverContextStartTls = initSslContextFactory(ggSecureKeyStore, true, true);
    clientContext = initSslContextFactory(ggSecureKeyStore, false, false);
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
      serverContext = initSslContextFactory(ggSecureKeyStore, true, false);
      serverContextStartTls =
          initSslContextFactory(ggSecureKeyStore, true, true);
      clientContext = null;
    } else {
      clientContext = initSslContextFactory(ggSecureKeyStore, false, false);
      serverContext = null;
      serverContextStartTls = null;
    }
  }

  /**
   * @param ggSecureKeyStore
   * @param serverMode
   *
   * @return the SSLContext
   */
  private SslContext initSslContextFactory(
      final WaarpSecureKeyStore ggSecureKeyStore, final boolean serverMode,
      final boolean startTls) {
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
                                    certificates, needClientAuthentication,
                                    startTls);
      } catch (final Throwable e) {//NOSONAR
        logger.error("Failed to initialize the server-side SSLContext {}",
                     e.getMessage());
        throw new Error("Failed to initialize the server-side SSLContext",
                        e);//NOSONAR
      }
    } else {
      try {
        return getInstanceForClient(ggSecureKeyStore.getKeyManagerFactory(),
                                    certificates);
      } catch (final Throwable e) {//NOSONAR
        logger.error("Failed to initialize the client-side SSLContext {}",
                     e.getMessage());
        throw new Error("Failed to initialize the client-side SSLContext",
                        e);//NOSONAR
      }
    }
  }

  /**
   * @return the Server Context
   */
  public final SslContext getServerContext() {
    return serverContext;
  }

  /**
   * @return the Client Context
   */
  public final SslContext getClientContext() {
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
  public final SslHandler createHandlerServer(final boolean needClientAuth,
                                              final Channel channel) {
    logger.debug(HAS_TRUST_MANAGER_IS_SERVER_MODE, needClientAuth, true);
    channel.config().setAutoRead(true);
    final SslHandler sslHandler =
        getServerContext().newHandler(channel.alloc());
    sslHandler.setHandshakeTimeoutMillis(DEFAULT_HANDSHAKE_TIMEOUT);
    return sslHandler;
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
  public final SslHandler createHandlerServer(final boolean needClientAuth,
                                              final boolean startTls,
                                              final Channel channel) {
    logger.debug(HAS_TRUST_MANAGER_IS_SERVER_MODE, needClientAuth, true);
    channel.config().setAutoRead(true);
    final SslHandler sslHandler;
    if (startTls) {
      sslHandler = serverContextStartTls.newHandler(channel.alloc());
    } else {
      sslHandler = getServerContext().newHandler(channel.alloc());
    }
    sslHandler.setHandshakeTimeoutMillis(DEFAULT_HANDSHAKE_TIMEOUT);
    return sslHandler;
  }

  /**
   * To be called before adding as first entry in the Initializer as<br>
   * pipeline.addLast("ssl", sslhandler);<br>
   *
   * @param channel the channel needing the SslHandler
   *
   * @return the sslhandler
   */
  public final SslHandler createHandlerClient(final SocketChannel channel) {
    logger.debug(HAS_TRUST_MANAGER_IS_SERVER_MODE, false, false);
    channel.config().setAutoRead(true);
    final InetSocketAddress socketAddress = channel.remoteAddress();
    final SslHandler sslHandler;
    if (socketAddress != null) {
      logger.debug("socket {} {}", socketAddress.getHostName(),
                   socketAddress.getPort());
      sslHandler = getClientContext().newHandler(channel.alloc(),
                                                 socketAddress.getHostName(),
                                                 socketAddress.getPort());
    } else {
      sslHandler = getClientContext().newHandler(channel.alloc());
    }
    sslHandler.setHandshakeTimeoutMillis(DEFAULT_HANDSHAKE_TIMEOUT);
    return sslHandler;
  }

  /**
   * @return True if the associated KeyStore has a TrustStore
   */
  public final boolean needClientAuthentication() {
    return needClientAuthentication;
  }
}
