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
package org.waarp.openr66.proxy.configuration;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;

import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.utils.R66ShutdownHook;
import org.waarp.openr66.proxy.network.NetworkServerInitializer;
import org.waarp.openr66.proxy.network.ProxyBridge;
import org.waarp.openr66.proxy.network.ProxyEntry;
import org.waarp.openr66.proxy.network.ssl.NetworkSslServerInitializer;
import org.waarp.openr66.proxy.protocol.http.HttpInitializer;
import org.waarp.openr66.proxy.protocol.http.adminssl.HttpSslInitializer;

/**
 * @author "Frederic Bregier"
 *
 */
public class Configuration extends org.waarp.openr66.protocol.configuration.Configuration {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(Configuration.class);

    /**
	 *
	 */
    public Configuration() {
        super();
    }

    @Override
    public void computeNbThreads() {
        int nb = Runtime.getRuntime().availableProcessors() + 1;
        setSERVER_THREAD(nb);
        setCLIENT_THREAD(getSERVER_THREAD() + 1);
        setRUNNER_THREAD(10);
    }

    @Override
    public void serverStartup() {
        setServer(true);
        getShutdownConfiguration().timeout = getTIMEOUTCON();
        R66ShutdownHook.addShutdownHook();
        if ((!isUseNOSSL()) && (!isUseSSL())) {
            logger.error("OpenR66 has neither NOSSL nor SSL support included! Stop here!");
            if (DetectionUtils.isJunit()) {
                return;
            }
            System.exit(-1);
        }
        pipelineInit();
        serverPipelineInit();
        r66Startup();
        startHttpSupport();
        try {
            startMonitoring();
        } catch (WaarpDatabaseSqlException e) {
        }
        logger.warn(this.toString());
    }

    @Override
    public void r66Startup() {
        logger.debug("Start R66: " + getHOST_SSLID());
        // add into configuration
        this.getConstraintLimitHandler().setServer(true);
        // Global Server
        serverChannelGroup = new DefaultChannelGroup("OpenR66", subTaskGroup.next());
        if (isUseNOSSL()) {
            serverBootstrap = new ServerBootstrap();
            WaarpNettyUtil.setServerBootstrap(serverBootstrap, bossGroup, workerGroup, (int) getTIMEOUTCON());
            networkServerInitializer = new NetworkServerInitializer(true);
            serverBootstrap.childHandler(networkServerInitializer);
            // FIXME take into account multiple address
            List<ChannelFuture> futures = new ArrayList<ChannelFuture>();
            for (ProxyEntry entry : ProxyEntry.proxyEntries.values()) {
                if (!entry.isLocalSsl()) {
                    logger.debug("Future Activation: " + entry.getLocalSocketAddress());
                    futures.add(serverBootstrap.bind(entry.getLocalSocketAddress()));
                }
            }
            for (ChannelFuture future : futures) {
                future.syncUninterruptibly();
                if (future.isSuccess()) {
                    bindNoSSL = future.channel();
                    serverChannelGroup.add(bindNoSSL);
                    logger.debug("Activation: " + bindNoSSL.localAddress());
                } else {
                    logger.warn(Messages.getString("Configuration.NOSSLDeactivated")
                            + " for " + bindNoSSL.localAddress()); //$NON-NLS-1$
                }
            }
        } else {
            networkServerInitializer = null;
            logger.warn(Messages.getString("Configuration.NOSSLDeactivated")); //$NON-NLS-1$
        }

        if (isUseSSL() && getHOST_SSLID() != null) {
            serverSslBootstrap = new ServerBootstrap();
            WaarpNettyUtil.setServerBootstrap(serverSslBootstrap, bossGroup, workerGroup, (int) getTIMEOUTCON());
            networkSslServerInitializer = new NetworkSslServerInitializer(false);
            serverSslBootstrap.childHandler(networkSslServerInitializer);
            // FIXME take into account multiple address
            List<ChannelFuture> futures = new ArrayList<ChannelFuture>();
            for (ProxyEntry entry : ProxyEntry.proxyEntries.values()) {
                if (entry.isLocalSsl()) {
                    logger.debug("Future SslActivation: " + entry.getLocalSocketAddress());
                    futures.add(serverSslBootstrap.bind(entry.getLocalSocketAddress()));
                }
            }
            for (ChannelFuture future : futures) {
                future.syncUninterruptibly();
                if (future.isSuccess()) {
                    bindSSL = future.channel();
                    serverChannelGroup.add(bindSSL);
                    logger.debug("SslActivation: " + bindSSL.localAddress());
                } else {
                    logger.warn(Messages.getString("Configuration.SSLMODEDeactivated")
                            + " for " + bindSSL.localAddress()); //$NON-NLS-1$
                }
            }
        } else {
            networkSslServerInitializer = null;
            logger.warn(Messages.getString("Configuration.SSLMODEDeactivated")); //$NON-NLS-1$
        }

        // Factory for TrafficShapingHandler
        setupLimitHandler();
        ProxyBridge.initialize();
        setThriftService(null);
    }

    @Override
    public void startHttpSupport() {
        // Now start the HTTP support
        logger.info(Messages.getString("Configuration.HTTPStart") + getSERVER_HTTPPORT() + //$NON-NLS-1$
                " HTTPS: " + getSERVER_HTTPSPORT());
        httpChannelGroup = new DefaultChannelGroup("HttpOpenR66", subTaskGroup.next());
        // Configure the server.
        httpBootstrap = new ServerBootstrap();
        WaarpNettyUtil.setServerBootstrap(httpBootstrap, httpBossGroup, httpWorkerGroup, (int) getTIMEOUTCON());
        // Set up the event pipeline factory.
        httpBootstrap.childHandler(new HttpInitializer(isUseHttpCompression()));
        // Bind and start to accept incoming connections.
        if (getSERVER_HTTPPORT() > 0) {
            ChannelFuture future = httpBootstrap.bind(new InetSocketAddress(getSERVER_HTTPPORT())).awaitUninterruptibly();
            if (future.isSuccess()) {
                httpChannelGroup.add(future.channel());
            }
        }
        // Now start the HTTPS support
        // Configure the server.
        httpsBootstrap = new ServerBootstrap();
        // Set up the event pipeline factory.
        WaarpNettyUtil.setServerBootstrap(httpsBootstrap, httpBossGroup, httpWorkerGroup, (int) getTIMEOUTCON());
        httpsBootstrap.childHandler(new HttpSslInitializer(isUseHttpCompression()));
        // Bind and start to accept incoming connections.
        if (getSERVER_HTTPSPORT() > 0) {
            ChannelFuture future = httpsBootstrap.bind(new InetSocketAddress(getSERVER_HTTPSPORT())).awaitUninterruptibly();
            if (future.isSuccess()) {
                httpChannelGroup.add(future.channel());
            }
        }
    }

    @Override
    public void serverStop() {
        super.serverStop();
        ProxyBridge.transaction.closeAll();
    }

}
