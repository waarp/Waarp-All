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

import java.security.Security;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSessionContext;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

/**
 * SSL ContextFactory for Netty.
 * 
 * @author Frederic Bregier
 * 
 */
public class WaarpSslContextFactory {
    protected static final int DEFAULT_SESSIONCACHE_TIMEOUTSEC = 60;

    protected static final int DEFAULT_SESSIONCACHE_SIZE = 1024;

    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(WaarpSslContextFactory.class);

    /**
    *
    */
    private static final String PROTOCOL = "TLS";

    /**
    *
    */
    private final SSLContext SERVER_CONTEXT;

    /**
    *
    */
    private final SSLContext CLIENT_CONTEXT;

    private boolean needClientAuthentication = false;

    /**
     * Create both CONTEXT
     * 
     * @param ggSecureKeyStore
     */
    public WaarpSslContextFactory(WaarpSecureKeyStore ggSecureKeyStore) {
        // Both construct Client and Server mode
        SERVER_CONTEXT = initSslContextFactory(ggSecureKeyStore, true);
        CLIENT_CONTEXT = initSslContextFactory(ggSecureKeyStore, false);
    }

    /**
     * Create only one of the CONTEXT
     * 
     * @param ggSecureKeyStore
     * @param serverMode
     */
    public WaarpSslContextFactory(WaarpSecureKeyStore ggSecureKeyStore, boolean serverMode) {
        if (serverMode) {
            SERVER_CONTEXT = initSslContextFactory(ggSecureKeyStore, serverMode);
            CLIENT_CONTEXT = null;
        } else {
            CLIENT_CONTEXT = initSslContextFactory(ggSecureKeyStore, serverMode);
            SERVER_CONTEXT = null;
        }
    }

    /**
     * 
     * @param cacheSize
     *            default being 1024
     * @param timeOutInSeconds
     *            default being 60s
     */
    public void setSessionCacheTime(int cacheSize, int timeOutInSeconds) {
        if (SERVER_CONTEXT != null) {
            SSLSessionContext sslSessionContext = SERVER_CONTEXT.getServerSessionContext();
            if (sslSessionContext != null) {
                sslSessionContext.setSessionCacheSize(cacheSize);
                sslSessionContext.setSessionTimeout(timeOutInSeconds);
            }
        }
    }

    /**
     * 
     * @param ggSecureKeyStore
     * @param serverMode
     * @return the SSLContext
     */
    private SSLContext initSslContextFactory(WaarpSecureKeyStore ggSecureKeyStore,
            boolean serverMode) {
        String algorithm = Security
                .getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }

        SSLContext serverContext = null;
        SSLContext clientContext = null;
        if (serverMode) {
            try {
                // Initialize the SSLContext to work with our key managers.
                serverContext = SSLContext.getInstance(PROTOCOL);
                WaarpSecureTrustManagerFactory secureTrustManagerFactory =
                        ggSecureKeyStore.getSecureTrustManagerFactory();
                needClientAuthentication = secureTrustManagerFactory.needAuthentication();
                if (secureTrustManagerFactory.hasTrustStore()) {
                    logger.debug("Has TrustManager");
                    serverContext.init(ggSecureKeyStore.getKeyManagerFactory().getKeyManagers(),
                            secureTrustManagerFactory.getTrustManagers(), null);
                } else {
                    logger.debug("No TrustManager");
                    serverContext.init(ggSecureKeyStore.getKeyManagerFactory().getKeyManagers(),
                            null, null);
                }
                SSLSessionContext sslSessionContext = serverContext.getServerSessionContext();
                if (sslSessionContext != null) {
                    sslSessionContext.setSessionCacheSize(DEFAULT_SESSIONCACHE_SIZE);
                    sslSessionContext.setSessionTimeout(DEFAULT_SESSIONCACHE_TIMEOUTSEC);
                }
                return serverContext;
            } catch (Throwable e) {
                logger.error("Failed to initialize the server-side SSLContext", e);
                throw new Error("Failed to initialize the server-side SSLContext",
                        e);
            }
        } else {
            try {
                clientContext = SSLContext.getInstance(PROTOCOL);
                WaarpSecureTrustManagerFactory secureTrustManagerFactory =
                        ggSecureKeyStore.getSecureTrustManagerFactory();
                needClientAuthentication = secureTrustManagerFactory.needAuthentication();
                if (secureTrustManagerFactory.hasTrustStore()) {
                    logger.debug("Has TrustManager");
                    clientContext.init(ggSecureKeyStore.getKeyManagerFactory().getKeyManagers(),
                            secureTrustManagerFactory.getTrustManagers(), null);
                } else {
                    logger.debug("No TrustManager");
                    clientContext.init(ggSecureKeyStore.getKeyManagerFactory().getKeyManagers(),
                            null, null);
                }
                SSLSessionContext sslSessionContext = clientContext.getServerSessionContext();
                if (sslSessionContext != null) {
                    sslSessionContext.setSessionCacheSize(DEFAULT_SESSIONCACHE_SIZE);
                    sslSessionContext.setSessionTimeout(DEFAULT_SESSIONCACHE_TIMEOUTSEC);
                }
                return clientContext;
            } catch (Throwable e) {
                logger.error("Failed to initialize the client-side SSLContext", e);
                throw new Error("Failed to initialize the client-side SSLContext",
                        e);
            }
        }
    }

    /**
     * @return the Server Context
     */
    public SSLContext getServerContext() {
        return SERVER_CONTEXT;
    }

    /**
     * @return the Client Context
     */
    public SSLContext getClientContext() {
        return CLIENT_CONTEXT;
    }

    /**
     * To be called before adding as first entry in the Initializer as<br>
     * pipeline.addLast("ssl", sslhandler);<br>
     * 
     * @param serverMode
     *            True if in Server Mode, else False in Client mode
     * @param needClientAuth
     *            True if the client needs to be authenticated (only if serverMode is True)
     * @return the sslhandler
     */
    public WaarpSslHandler initInitializer(boolean serverMode,
            boolean needClientAuth) {
        // Add SSL handler first to encrypt and decrypt everything.
        SSLEngine engine;
        logger.debug("Has TrustManager? " + needClientAuth + " Is ServerMode? " + serverMode);
        if (serverMode) {
            engine = getServerContext().createSSLEngine();
            engine.setUseClientMode(false);
            engine.setNeedClientAuth(needClientAuth);
        } else {
            engine = getClientContext().createSSLEngine();
            engine.setUseClientMode(true);
        }
        WaarpSslHandler handler = new WaarpSslHandler(engine);
        return handler;
    }

    /**
     * To be called before adding as first entry in the Initializer as<br>
     * pipeline.addLast("ssl", sslhandler);<br>
     * 
     * @param serverMode
     *            True if in Server Mode, else False in Client mode
     * @param needClientAuth
     *            True if the client needs to be authenticated (only if serverMode is True)
     * @param host
     *            Host for which a resume is allowed
     * @param port
     *            port associated with the host for which a resume is allowed
     * @return the sslhandler
     */
    public WaarpSslHandler initInitializer(boolean serverMode,
            boolean needClientAuth, String host, int port) {
        // Add SSL handler first to encrypt and decrypt everything.
        SSLEngine engine;
        logger.debug("Has TrustManager? " + needClientAuth + " Is ServerMode? " + serverMode);
        if (serverMode) {
            engine = getServerContext().createSSLEngine(host, port);
            engine.setUseClientMode(false);
            engine.setNeedClientAuth(needClientAuth);
        } else {
            engine = getClientContext().createSSLEngine(host, port);
            engine.setUseClientMode(true);
        }
        WaarpSslHandler handler = new WaarpSslHandler(engine);
        return handler;
    }

    /**
     * 
     * @return True if the associated KeyStore has a TrustStore
     */
    public boolean needClientAuthentication() {
        return needClientAuthentication;
    }
}
