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
package org.waarp.ftp.core.utils;

import ch.qos.logback.classic.LoggerContext;
import io.netty.channel.Channel;
import org.slf4j.LoggerFactory;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.ftp.core.config.FtpConfiguration;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Timer;

/**
 * Some useful functions related to Channel of Netty
 */
public class FtpChannelUtils implements Runnable {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpChannelUtils.class);

  /**
   * Get the Remote InetAddress
   *
   * @param channel
   *
   * @return the remote InetAddress
   */
  public static InetAddress getRemoteInetAddress(final Channel channel) {
    InetSocketAddress socketAddress =
        (InetSocketAddress) channel.remoteAddress();
    if (socketAddress == null) {
      socketAddress = new InetSocketAddress(20);
    }
    return socketAddress.getAddress();
  }

  /**
   * Get the Local InetAddress
   *
   * @param channel
   *
   * @return the local InetAddress
   */
  public static InetAddress getLocalInetAddress(final Channel channel) {
    final InetSocketAddress socketAddress =
        (InetSocketAddress) channel.localAddress();
    return socketAddress.getAddress();
  }

  /**
   * Get the Remote InetSocketAddress
   *
   * @param channel
   *
   * @return the remote InetSocketAddress
   */
  public static InetSocketAddress getRemoteInetSocketAddress(
      final Channel channel) {
    return (InetSocketAddress) channel.remoteAddress();
  }

  /**
   * Get the Local InetSocketAddress
   *
   * @param channel
   *
   * @return the local InetSocketAddress
   */
  public static InetSocketAddress getLocalInetSocketAddress(
      final Channel channel) {
    return (InetSocketAddress) channel.localAddress();
  }

  /**
   * Get the InetSocketAddress corresponding to the FTP format of address
   *
   * @param arg
   *
   * @return the InetSocketAddress or null if an error occurs
   */
  public static InetSocketAddress getInetSocketAddress(final String arg) {
    final String[] elements = arg.split(",");
    if (elements.length != 6) {
      return null;
    }
    final byte[] address = { 0, 0, 0, 0 };
    final int[] iElements = new int[6];
    for (int i = 0; i < 6; i++) {
      try {
        iElements[i] = Integer.parseInt(elements[i]);
      } catch (final NumberFormatException e) {
        return null;
      }
      if (iElements[i] < 0 || iElements[i] > 255) {
        return null;
      }
    }
    for (int i = 0; i < 4; i++) {
      address[i] = (byte) iElements[i];
    }
    final int port = iElements[4] << 8 | iElements[5];
    final InetAddress inetAddress;
    try {
      inetAddress = InetAddress.getByAddress(address);
    } catch (final UnknownHostException e) {
      return null;
    }
    return new InetSocketAddress(inetAddress, port);
  }

  /**
   * Return the Address in the format compatible with FTP argument
   *
   * @param address
   * @param port
   *
   * @return the String representation of the address
   */
  public static String getAddress(final String address, final int port) {
    return address.replace('.', ',') + ',' + (port >> 8) + ',' + (port & 0xFF);
  }

  /**
   * Return the Address in the format compatible with FTP argument
   *
   * @param address
   *
   * @return the String representation of the address
   */
  public static String getAddress(final InetSocketAddress address) {
    final InetAddress servAddr = address.getAddress();
    final int servPort = address.getPort();
    return servAddr.getHostAddress().replace('.', ',') + ',' + (servPort >> 8) +
           ',' + (servPort & 0xFF);
  }

  /**
   * Get the (RFC2428) InetSocketAddress corresponding to the FTP format of
   * address (RFC2428)
   *
   * @param arg
   *
   * @return the InetSocketAddress or null if an error occurs
   */
  public static InetSocketAddress get2428InetSocketAddress(final String arg) {
    // Format: #a#net-addr#tcp-port# where a = 1 IPV4 or 2 IPV6, other will
    // not be supported
    if (arg == null || arg.length() == 0) {
      // bad args
      return null;
    }
    final String delim = arg.substring(0, 1);
    final String[] infos = arg.split('\\' + delim);
    if (infos.length != 3 && infos.length != 4) {
      // bad format
      logger.error("Bad address format: " + infos.length);
      return null;
    }
    int start = 0;
    if (infos.length == 4) {
      start = 1;
    }
    boolean isIPV4 = true;
    if ("1".equals(infos[start])) {
      isIPV4 = true;
    } else if ("2".equals(infos[start])) {
      isIPV4 = false;
    } else {
      // not supported
      logger.error("Bad 1 or 2 format in address: " + infos[start]);
      return null;
    }
    start++;
    final InetAddress inetAddress;
    if (isIPV4) {
      // IPV4
      try {
        inetAddress = InetAddress.getByName(infos[start]);
      } catch (final UnknownHostException e) {
        logger.error("Bad IPV4 format", e);
        return null;
      }
    } else {
      // IPV6
      try {
        inetAddress = InetAddress.getByName(infos[start]);
      } catch (final UnknownHostException e) {
        logger.error("Bad IPV6 format", e);
        return null;
      }
    }
    start++;
    final int port;
    try {
      port = Integer.parseInt(infos[start]);
    } catch (final NumberFormatException e) {
      logger.error("Bad port number format: " + infos[start]);
      return null;
    }
    return new InetSocketAddress(inetAddress, port);
  }

  /**
   * Return the (RFC2428) Address in the format compatible with FTP (RFC2428)
   *
   * @param address
   *
   * @return the String representation of the address
   */
  public static String get2428Address(final InetSocketAddress address) {
    final InetAddress servAddr = address.getAddress();
    final int servPort = address.getPort();
    final StringBuilder builder = new StringBuilder();
    final String hostaddress = servAddr.getHostAddress();
    builder.append('|');
    if (hostaddress.contains(":")) {
      builder.append('2'); // IPV6
    } else {
      builder.append('1'); // IPV4
    }
    builder.append('|').append(hostaddress).append('|').append(servPort)
           .append('|');
    return builder.toString();
  }

  /**
   * Terminate all registered command channels
   *
   * @param configuration
   *
   * @return the number of previously registered command channels
   */
  static int terminateCommandChannels(final FtpConfiguration configuration) {
    final int result =
        configuration.getFtpInternalConfiguration().getCommandChannelGroup()
                     .size();
    configuration.getFtpInternalConfiguration().getCommandChannelGroup()
                 .close();
    return result;
  }

  /**
   * Terminate all registered data channels
   *
   * @param configuration
   *
   * @return the number of previously registered data channels
   */
  private static int terminateDataChannels(
      final FtpConfiguration configuration) {
    final int result =
        configuration.getFtpInternalConfiguration().getDataChannelGroup()
                     .size();
    configuration.getFtpInternalConfiguration().getDataChannelGroup().close();
    return result;
  }

  /**
   * Return the current number of command connections
   *
   * @param configuration
   *
   * @return the current number of command connections
   */
  public static int nbCommandChannels(final FtpConfiguration configuration) {
    return configuration.getFtpInternalConfiguration().getCommandChannelGroup()
                        .size();
  }

  /**
   * Return the current number of data connections
   *
   * @param configuration
   *
   * @return the current number of data connections
   */
  public static int nbDataChannels(final FtpConfiguration configuration) {
    return configuration.getFtpInternalConfiguration().getDataChannelGroup()
                        .size();
  }

  /**
   * Return the number of still positive command connections
   *
   * @param configuration
   *
   * @return the number of positive command connections
   */
  public static int validCommandChannels(final FtpConfiguration configuration) {
    int result = 0;
    Channel channel;
    for (final Channel value : configuration.getFtpInternalConfiguration()
                                            .getCommandChannelGroup()) {
      channel = value;
      if (channel.parent() != null) {
        // Child Channel
        if (channel.isActive()) {
          // Normal channel
          result++;
        } else {
          WaarpSslUtility.closingSslChannel(channel);
        }
      } else {
        // Parent channel
        result++;
      }
    }
    return result;
  }

  /**
   * Exit global ChannelFactory
   *
   * @param configuration
   */
  protected static void exit(final FtpConfiguration configuration) {
    configuration.setShutdown(true);
    final long delay = configuration.getTimeoutCon() / 2;
    logger.warn("Exit: Give a delay of " + delay + " ms");
    configuration.inShutdownProcess();
    try {
      Thread.sleep(delay);
    } catch (final InterruptedException e) {//NOSONAR
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    }
    final Timer timer = new Timer(true);
    final FtpTimerTask timerTask = new FtpTimerTask(FtpTimerTask.TIMER_CONTROL);
    timerTask.setConfiguration(configuration);
    timer.schedule(timerTask, delay / 2);
    configuration.getFtpInternalConfiguration().getGlobalTrafficShapingHandler()
                 .release();
    configuration.releaseResources();
    logger.info("Exit Shutdown Data");
    terminateDataChannels(configuration);
    logger.warn("Exit end of Data Shutdown");
  }

  /**
   * This function is the top function to be called when the server is to be
   * shutdown.
   *
   * @param configuration
   */
  public static void teminateServer(final FtpConfiguration configuration) {
    FtpShutdownHook.configuration = configuration;
    WaarpShutdownHook.terminate(false);
  }

  /**
   * Add a command channel into the list
   *
   * @param channel
   * @param configuration
   */
  public static void addCommandChannel(final Channel channel,
                                       final FtpConfiguration configuration) {
    configuration.getFtpInternalConfiguration().getCommandChannelGroup()
                 .add(channel);
  }

  /**
   * Add a data channel into the list
   *
   * @param channel
   * @param configuration
   */
  public static void addDataChannel(final Channel channel,
                                    final FtpConfiguration configuration) {
    configuration.getFtpInternalConfiguration().getDataChannelGroup()
                 .add(channel);
  }

  /**
   * Used to run Exit command
   */
  private final FtpConfiguration configuration;

  public FtpChannelUtils(final FtpConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void run() {
    exit(configuration);
  }

  public static void stopLogger() {
    if (WaarpSystemUtil.isJunit()) {
      return;
    }
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.NONE));
    if (WaarpLoggerFactory
            .getDefaultFactory() instanceof WaarpSlf4JLoggerFactory &&
        !WaarpSystemUtil.isJunit()) {
      final LoggerContext lc =
          (LoggerContext) LoggerFactory.getILoggerFactory();
      lc.stop();
    }
  }

}
