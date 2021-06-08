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
package org.waarp.openr66.proxy.configuration;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.group.DefaultChannelGroup;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.proxy.network.NetworkServerInitializerProxy;
import org.waarp.openr66.proxy.network.ProxyBridge;
import org.waarp.openr66.proxy.network.ProxyEntry;
import org.waarp.openr66.proxy.network.ssl.NetworkSslServerInitializerProxy;
import org.waarp.openr66.proxy.protocol.http.HttpInitializer;
import org.waarp.openr66.proxy.protocol.http.adminssl.HttpSslInitializer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ConfigurationProxyR66 extends Configuration {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(ConfigurationProxyR66.class);

  /**
   *
   */
  public ConfigurationProxyR66() {
    // nothing
  }

  @Override
  public void computeNbThreads() {
    final int nb = Runtime.getRuntime().availableProcessors() + 1;
    setServerThread(nb);
    setClientThread(getServerThread() + 1);
    setRunnerThread(10);
  }

  @Override
  public void serverStartup() {
    setServer(true);
    getShutdownConfiguration().timeout = getTimeoutCon();
    WaarpShutdownHook.addShutdownHook();
    if (!isUseNOSSL() && !isUseSSL()) {
      logger.error(
          "OpenR66 has neither NOSSL nor SSL support included! Stop here!");
      WaarpSystemUtil.systemExit(-1);
      return;
    }
    pipelineInit();
    serverPipelineInit();
    r66Startup();
    startHttpSupport();
    try {
      startMonitoring();
    } catch (final WaarpDatabaseSqlException ignored) {
      // nothing
    }
    logger.warn(toString());
  }

  @Override
  public void r66Startup() {
    logger.debug("Start R66: {}", getHostSslId());
    // add into configuration
    getConstraintLimitHandler().setServer(true);
    // Global Server
    serverChannelGroup =
        new DefaultChannelGroup("OpenR66", subTaskGroup.next());
    if (isUseNOSSL()) {
      serverBootstrap = new ServerBootstrap();
      WaarpNettyUtil
          .setServerBootstrap(serverBootstrap, serverGroup, workerGroup,
                              (int) getTimeoutCon(), getBlockSize() + 64,
                              false);
      networkServerInitializer = new NetworkServerInitializerProxy(true);
      serverBootstrap.childHandler(networkServerInitializer);
      // FIXME take into account multiple address
      final List<ChannelFuture> futures = new ArrayList<ChannelFuture>();
      for (final ProxyEntry entry : ProxyEntry.proxyEntries.values()) {
        if (!entry.isLocalSsl()) {
          logger.debug("Future Activation: {}", entry.getLocalSocketAddress());
          futures.add(serverBootstrap.bind(entry.getLocalSocketAddress()));
        }
      }
      for (final ChannelFuture future : futures) {
        future.syncUninterruptibly();
        if (future.isSuccess()) {
          bindNoSSL = future.channel();
          serverChannelGroup.add(bindNoSSL);
          logger.debug("Activation: {}", bindNoSSL.localAddress());
        } else {
          logger.warn(
              Messages.getString("Configuration.NOSSLDeactivated") + " for " +
              bindNoSSL.localAddress()); //$NON-NLS-2$
        }
      }
    } else {
      networkServerInitializer = null;
      logger.warn(
          Messages.getString("Configuration.NOSSLDeactivated")); //$NON-NLS-1$
    }

    if (isUseSSL() && getHostSslId() != null) {
      serverSslBootstrap = new ServerBootstrap();
      WaarpNettyUtil
          .setServerBootstrap(serverSslBootstrap, serverGroup, workerGroup,
                              (int) getTimeoutCon(), getBlockSize() + 64,
                              false);
      networkSslServerInitializer = new NetworkSslServerInitializerProxy(false);
      serverSslBootstrap.childHandler(networkSslServerInitializer);
      // FIXME take into account multiple address
      final List<ChannelFuture> futures = new ArrayList<ChannelFuture>();
      for (final ProxyEntry entry : ProxyEntry.proxyEntries.values()) {
        if (entry.isLocalSsl()) {
          logger
              .debug("Future SslActivation: " + entry.getLocalSocketAddress());
          futures.add(serverSslBootstrap.bind(entry.getLocalSocketAddress()));
        }
      }
      for (final ChannelFuture future : futures) {
        future.syncUninterruptibly();
        if (future.isSuccess()) {
          bindSSL = future.channel();
          serverChannelGroup.add(bindSSL);
          logger.debug("SslActivation: {}", bindSSL.localAddress());
        } else {
          logger.warn(
              Messages.getString("Configuration.SSLMODEDeactivated") + " for " +
              bindSSL.localAddress()); //$NON-NLS-2$
        }
      }
    } else {
      networkSslServerInitializer = null;
      logger.warn(
          Messages.getString("Configuration.SSLMODEDeactivated")); //$NON-NLS-1$
    }

    // Factory for TrafficShapingHandler
    setupLimitHandler();
    ProxyBridge.initialize();
    setThriftService(null);
  }

  @Override
  public void startHttpSupport() {
    // Now start the HTTP support
    logger.info(
        Messages.getString("Configuration.HTTPStart") + getServerHttpport() +
        //$NON-NLS-1$
        " HTTPS: " + getServerHttpsPort());
    httpChannelGroup =
        new DefaultChannelGroup("HttpOpenR66", subTaskGroup.next());
    if (getServerHttpport() > 0) {
      // Configure the server.
      httpBootstrap = new ServerBootstrap();
      WaarpNettyUtil
          .setServerBootstrap(httpBootstrap, httpWorkerGroup, httpWorkerGroup,
                              (int) getTimeoutCon());
      // Set up the event pipeline factory.
      httpBootstrap.childHandler(new HttpInitializer(isUseHttpCompression()));
      // Bind and start to accept incoming connections.
      final ChannelFuture future =
          httpBootstrap.bind(new InetSocketAddress(getServerHttpport()))
                       .awaitUninterruptibly();
      if (future.isSuccess()) {
        httpChannelGroup.add(future.channel());
      }
    }
    // Now start the HTTPS support
    if (getServerHttpsPort() > 0) {
      // Configure the server.
      httpsBootstrap = new ServerBootstrap();
      // Set up the event pipeline factory.
      WaarpNettyUtil
          .setServerBootstrap(httpsBootstrap, httpWorkerGroup, httpWorkerGroup,
                              (int) getTimeoutCon());
      httpsBootstrap
          .childHandler(new HttpSslInitializer(isUseHttpCompression()));
      // Bind and start to accept incoming connections.
      final ChannelFuture future =
          httpsBootstrap.bind(new InetSocketAddress(getServerHttpsPort()))
                        .awaitUninterruptibly();
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
