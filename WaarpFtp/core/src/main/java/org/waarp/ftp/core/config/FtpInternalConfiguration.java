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
package org.waarp.ftp.core.config;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import org.waarp.common.command.exception.Reply425Exception;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.common.utility.WaarpThreadFactory;
import org.waarp.ftp.core.control.FtpInitializer;
import org.waarp.ftp.core.control.ftps.FtpsInitializer;
import org.waarp.ftp.core.data.handler.FtpDataInitializer;
import org.waarp.ftp.core.data.handler.ftps.FtpsDataInitializer;
import org.waarp.ftp.core.exception.FtpNoConnectionException;
import org.waarp.ftp.core.session.FtpSession;
import org.waarp.ftp.core.session.FtpSessionReference;
import org.waarp.ftp.core.utils.FtpChannelUtils;
import org.waarp.ftp.core.utils.FtpShutdownHook;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Internal configuration of the FTP server, related to Netty
 */
public class FtpInternalConfiguration {
  // Static values
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpInternalConfiguration.class);

  // Network Internals
  /**
   * Time elapse for retry in ms
   */
  public static final long RETRYINMS = 10;

  /**
   * Number of retry before error
   */
  public static final int RETRYNB = 3;

  /**
   * Time elapse for WRITE OR CLOSE WAIT elaps in ms
   */
  public static final long WAITFORNETOP = 1000;
  /**
   * Hack to say Windows or Unix (USR1 not OK on Windows)
   */
  static Boolean isUnix;

  /**
   * Default size for buffers (NIO)
   */
  public static final int BUFFERSIZEDEFAULT = 0x10000; // 64K

  // Dynamic values
  /**
   * List of all Command Channels to enable the close call on them using Netty
   * ChannelGroup
   */
  private ChannelGroup commandChannelGroup;

  /**
   * ExecutorService Worker
   */
  private final EventLoopGroup execWorker;

  /**
   * Bootstrap for Command server
   */
  private ServerBootstrap serverBootstrap;

  /**
   * List of all Data Channels to enable the close call on them using Netty
   * ChannelGroup
   */
  private ChannelGroup dataChannelGroup;

  /**
   * ExecutorService Command Event Loop
   */
  private final EventLoopGroup execCommandEvent;

  /**
   * ExecutorService Data Event Loop
   */
  private final EventLoopGroup execDataEvent;

  /**
   * ExecutorService Data Active Worker
   */
  private final EventLoopGroup execDataWorker;

  /**
   * FtpSession references used by Data Connection process
   */
  private final FtpSessionReference ftpSessionReference =
      new FtpSessionReference();

  /**
   * Bootstrap for Active connections
   */
  private Bootstrap activeBootstrap;

  /**
   * ServerBootStrap for Passive connections
   */
  private ServerBootstrap passiveBootstrap;

  /**
   * Scheduler for TrafficCounter
   */
  private final ScheduledExecutorService executorService = Executors
      .newScheduledThreadPool(2, new WaarpThreadFactory("TimerTrafficFtp"));

  /**
   * Global TrafficCounter (set from global configuration)
   */
  private FtpGlobalTrafficShapingHandler globalTrafficShapingHandler;

  /**
   * Does the FTP will be SSL native based (990 989 port)
   */
  private boolean usingNativeSsl;

  /**
   * Does the FTP accept AUTH and PROT
   */
  private boolean acceptAuthProt;
  /**
   * Bootstrap for Active Ssl connections
   */
  private Bootstrap activeSslBootstrap;

  /**
   * ServerBootStrap for Passive Ssl connections
   */
  private ServerBootstrap passiveSslBootstrap;

  /**
   * org.waarp.ftp.core.config BindAddress
   */
  public static class BindAddress {
    /**
     * Parent passive channel
     */
    public final Channel parent;

    /**
     * Number of binded Data connections
     */
    public final AtomicInteger nbBind = new AtomicInteger();

    /**
     * Constructor
     *
     * @param channel
     */
    public BindAddress(Channel channel) {
      parent = channel;
      nbBind.set(0);
    }
  }

  /**
   * List of already bind local addresses for Passive connections
   */
  private final ConcurrentHashMap<InetSocketAddress, BindAddress>
      hashBindPassiveDataConn =
      new ConcurrentHashMap<InetSocketAddress, BindAddress>();

  /**
   * Global Configuration
   */
  private final FtpConfiguration configuration;

  /**
   * Constructor
   *
   * @param configuration
   */
  public FtpInternalConfiguration(FtpConfiguration configuration) {
    this.configuration = configuration;
    isUnix = !DetectionUtils.isWindows();
    configuration.getShutdownConfiguration().timeout =
        configuration.getTimeoutCon();
    new FtpShutdownHook(configuration.getShutdownConfiguration(),
                        configuration);
    execCommandEvent = new NioEventLoopGroup(configuration.getClientThread(),
                                             new WaarpThreadFactory("Command"));
    execDataEvent = new NioEventLoopGroup(configuration.getClientThread(),
                                          new WaarpThreadFactory("Data"));
    execWorker = new NioEventLoopGroup(configuration.getClientThread(),
                                       new WaarpThreadFactory("CommandWorker"));
    execDataWorker = new NioEventLoopGroup(configuration.getClientThread() * 2,
                                           new WaarpThreadFactory(
                                               "DataWorker"));
  }

  /**
   * Startup the server
   *
   * @throws FtpNoConnectionException
   */
  public void serverStartup() throws FtpNoConnectionException {
    logger.debug("Start groups");
    // Command
    commandChannelGroup =
        new DefaultChannelGroup(configuration.fromClass.getName(),
                                execWorker.next());
    // Data
    dataChannelGroup =
        new DefaultChannelGroup(configuration.fromClass.getName() + ".data",
                                execWorker.next());

    logger.debug("Start data connections");
    // Passive Data Connections
    passiveBootstrap = new ServerBootstrap();
    WaarpNettyUtil.setServerBootstrap(passiveBootstrap, execDataWorker,
                                      (int) configuration.getTimeoutCon());
    if (usingNativeSsl) {
      passiveBootstrap.childHandler(
          new FtpsDataInitializer(configuration.dataBusinessHandler,
                                  configuration, false));
    } else {
      passiveBootstrap.childHandler(
          new FtpDataInitializer(configuration.dataBusinessHandler,
                                 configuration, false));
    }
    if (acceptAuthProt) {
      passiveSslBootstrap = new ServerBootstrap();
      WaarpNettyUtil.setServerBootstrap(passiveSslBootstrap, execDataWorker,
                                        (int) configuration.getTimeoutCon());
      passiveSslBootstrap.childHandler(
          new FtpsDataInitializer(configuration.dataBusinessHandler,
                                  configuration, false));
    } else {
      passiveSslBootstrap = passiveBootstrap;
    }

    // Active Data Connections
    activeBootstrap = new Bootstrap();
    WaarpNettyUtil.setBootstrap(activeBootstrap, execDataWorker,
                                (int) configuration.getTimeoutCon());
    if (usingNativeSsl) {
      activeBootstrap.handler(
          new FtpsDataInitializer(configuration.dataBusinessHandler,
                                  configuration, true));
    } else {
      activeBootstrap.handler(
          new FtpDataInitializer(configuration.dataBusinessHandler,
                                 configuration, true));
    }
    if (acceptAuthProt) {
      activeSslBootstrap = new Bootstrap();
      WaarpNettyUtil.setBootstrap(activeSslBootstrap, execDataWorker,
                                  (int) configuration.getTimeoutCon());
      activeSslBootstrap.handler(
          new FtpsDataInitializer(configuration.dataBusinessHandler,
                                  configuration, true));
    } else {
      activeSslBootstrap = activeBootstrap;
    }

    logger.debug("Start command connections {}", configuration.getServerPort());
    // Main Command server
    serverBootstrap = new ServerBootstrap();
    WaarpNettyUtil.setServerBootstrap(serverBootstrap, execWorker,
                                      (int) configuration.getTimeoutCon());
    if (usingNativeSsl) {
      serverBootstrap.childHandler(
          new FtpsInitializer(configuration.businessHandler, configuration));
    } else {
      serverBootstrap.childHandler(
          new FtpInitializer(configuration.businessHandler, configuration));
    }
    InetSocketAddress socketAddress =
        new InetSocketAddress(configuration.getServerPort());
    ChannelFuture future = serverBootstrap.bind(socketAddress);
    try {
      future = future.sync();
    } catch (InterruptedException e) {//NOSONAR
      logger.error("Cannot start command conections", e);
      throw new FtpNoConnectionException("Can't initiate the FTP server", e);
    }
    if (!future.isSuccess()) {
      logger.error("Cannot start command conections");
      throw new FtpNoConnectionException("Can't initiate the FTP server");
    }
    FtpChannelUtils.addCommandChannel(future.channel(), configuration);

    // Init Shutdown Hook handler
    configuration.getShutdownConfiguration().timeout =
        configuration.getTimeoutCon();
    WaarpShutdownHook.addShutdownHook();
    // Factory for TrafficShapingHandler
    globalTrafficShapingHandler =
        new FtpGlobalTrafficShapingHandler(executorService, configuration
            .getServerGlobalWriteLimit(), configuration
                                               .getServerGlobalReadLimit(),
                                           configuration
                                               .getServerChannelWriteLimit(),
                                           configuration
                                               .getServerChannelReadLimit(),
                                           configuration.getDelayLimit());
  }

  /**
   * @return an ExecutorService
   */
  public ExecutorService getWorker() {
    return execWorker;
  }

  /**
   * Add a session from a couple of addresses
   *
   * @param ipOnly
   * @param fullIp
   * @param session
   */
  public void setNewFtpSession(InetAddress ipOnly, InetSocketAddress fullIp,
                               FtpSession session) {
    ftpSessionReference.setNewFtpSession(ipOnly, fullIp, session);
  }

  /**
   * Return and remove the FtpSession
   *
   * @param channel
   * @param active
   *
   * @return the FtpSession if it exists associated to this channel
   */
  public FtpSession getFtpSession(Channel channel, boolean active,
                                  boolean remove) {
    if (active) {
      return ftpSessionReference.getActiveFtpSession(channel, remove);
    } else {
      return ftpSessionReference.getPassiveFtpSession(channel, remove);
    }
  }

  /**
   * Remove the FtpSession
   *
   * @param ipOnly
   * @param fullIp
   */
  public void delFtpSession(InetAddress ipOnly, InetSocketAddress fullIp) {
    ftpSessionReference.delFtpSession(ipOnly, fullIp);
  }

  /**
   * Test if the couple of addresses is already in the context
   *
   * @param ipOnly
   * @param fullIp
   *
   * @return True if the couple is present
   */
  public boolean hasFtpSession(InetAddress ipOnly, InetSocketAddress fullIp) {
    return ftpSessionReference.contains(ipOnly, fullIp);
  }

  /**
   * @return the number of Active Sessions
   */
  public int getNumberSessions() {
    return ftpSessionReference.sessionsNumber();
  }

  /**
   * Try to add a Passive Channel listening to the specified local address
   *
   * @param address
   * @param ssl
   *
   * @throws Reply425Exception in case the channel cannot be opened
   */
  public void bindPassive(InetSocketAddress address, boolean ssl)
      throws Reply425Exception {
    configuration.bindLock();
    try {
      BindAddress bindAddress = hashBindPassiveDataConn.get(address);
      if (bindAddress == null) {
        logger.debug("Bind really to {}", address);
        Channel parentChannel;
        try {
          ChannelFuture future;
          if (ssl) {
            future = passiveSslBootstrap.bind(address);
          } else {
            future = passiveBootstrap.bind(address);
          }
          if (future.await(configuration.getTimeoutCon())) {
            parentChannel = future.sync().channel();
          } else {
            logger.warn("Cannot open passive connection due to Timeout");
            throw new Reply425Exception(
                "Cannot open a Passive Connection due to Timeout");
          }
        } catch (final ChannelException e) {
          logger.warn("Cannot open passive connection {}", e.getMessage());
          throw new Reply425Exception("Cannot open a Passive Connection");
        } catch (final InterruptedException e) {//NOSONAR
          logger.warn("Cannot open passive connection {}", e.getMessage());
          throw new Reply425Exception("Cannot open a Passive Connection");
        }
        bindAddress = new BindAddress(parentChannel);
        FtpChannelUtils.addDataChannel(parentChannel, configuration);
        hashBindPassiveDataConn.put(address, bindAddress);
      }
      bindAddress.nbBind.getAndIncrement();
      logger.debug("Bind number to {} is {}", address, bindAddress.nbBind);
    } finally {
      configuration.bindUnlock();
    }
  }

  /**
   * Try to unbind (closing the parent channel) the Passive Channel listening
   * to
   * the specified local address if
   * the last one. It returns only when the underlying parent channel is
   * closed
   * if this was the last session
   * that wants to open on this local address.
   *
   * @param address
   */
  public void unbindPassive(InetSocketAddress address) {
    configuration.bindLock();
    try {
      final BindAddress bindAddress = hashBindPassiveDataConn.get(address);
      if (bindAddress != null) {
        bindAddress.nbBind.getAndDecrement();
        logger
            .debug("Bind number to {} left is {}", address, bindAddress.nbBind);
        if (bindAddress.nbBind.get() == 0) {
          WaarpSslUtility.closingSslChannel(bindAddress.parent);
          hashBindPassiveDataConn.remove(address);
        }
      } else {
        logger.warn("No Bind to {}", address);
      }
    } finally {
      configuration.bindUnlock();
    }
  }

  /**
   * @return the number of Binded Passive Connections
   */
  public int getNbBindedPassive() {
    return hashBindPassiveDataConn.size();
  }

  /**
   * Return the associated Executor for Command Event
   *
   * @return the Command Event Executor
   */
  public EventExecutorGroup getExecutor() {
    return execCommandEvent;
  }

  /**
   * Return the associated Executor for Data Event
   *
   * @return the Data Event Executor
   */
  public EventExecutorGroup getDataExecutor() {
    return execDataEvent;
  }

  /**
   * @param ssl
   *
   * @return the ActiveBootstrap
   */
  public Bootstrap getActiveBootstrap(boolean ssl) {
    if (ssl) {
      return activeSslBootstrap;
    } else {
      return activeBootstrap;
    }
  }

  /**
   * @return the commandChannelGroup
   */
  public ChannelGroup getCommandChannelGroup() {
    return commandChannelGroup;
  }

  /**
   * @return the dataChannelGroup
   */
  public ChannelGroup getDataChannelGroup() {
    return dataChannelGroup;
  }

  /**
   * @return The TrafficCounterFactory
   */
  public FtpGlobalTrafficShapingHandler getGlobalTrafficShapingHandler() {
    return globalTrafficShapingHandler;
  }

  /**
   * @return a new ChannelTrafficShapingHandler
   */
  public ChannelTrafficShapingHandler newChannelTrafficShapingHandler() {
    if (configuration.getServerChannelWriteLimit() == 0 &&
        configuration.getServerChannelReadLimit() == 0) {
      return null;
    }
    if (globalTrafficShapingHandler instanceof GlobalChannelTrafficShapingHandler) {
      return null;
    }
    return new FtpChannelTrafficShapingHandler(
        configuration.getServerChannelWriteLimit(),
        configuration.getServerChannelReadLimit(),
        configuration.getDelayLimit());
  }

  public void releaseResources() {
    WaarpSslUtility.forceCloseAllSslChannels();
    execWorker.shutdownGracefully();
    execDataWorker.shutdownGracefully();
    if (globalTrafficShapingHandler != null) {
      globalTrafficShapingHandler.release();
    }
    executorService.shutdown();
  }

  public boolean isAcceptAuthProt() {
    return acceptAuthProt;
  }

  /**
   * @return the usingNativeSsl
   */
  public boolean isUsingNativeSsl() {
    return usingNativeSsl;
  }

  /**
   * @param usingNativeSsl the usingNativeSsl to set
   */
  public void setUsingNativeSsl(boolean usingNativeSsl) {
    this.usingNativeSsl = usingNativeSsl;
  }

  /**
   * @param acceptAuthProt the acceptAuthProt to set
   */
  public void setAcceptAuthProt(boolean acceptAuthProt) {
    this.acceptAuthProt = acceptAuthProt;
  }

}
