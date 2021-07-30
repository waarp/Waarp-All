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
package org.waarp.ftp.core.data;

import io.netty.channel.Channel;
import org.waarp.common.command.exception.Reply425Exception;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.ftp.core.command.FtpArgumentCode;
import org.waarp.ftp.core.command.FtpArgumentCode.TransferMode;
import org.waarp.ftp.core.command.FtpArgumentCode.TransferStructure;
import org.waarp.ftp.core.command.FtpArgumentCode.TransferType;
import org.waarp.ftp.core.config.FtpConfiguration;
import org.waarp.ftp.core.data.handler.DataNetworkHandler;
import org.waarp.ftp.core.exception.FtpNoConnectionException;
import org.waarp.ftp.core.session.FtpSession;
import org.waarp.ftp.core.utils.FtpChannelUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main class that handles Data connection using asynchronous connection with
 * Netty
 */
public class FtpDataAsyncConn {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpDataAsyncConn.class);
  /**
   * SessionInterface
   */
  private final FtpSession session;

  /**
   * Current Data Network Handler
   */
  private DataNetworkHandler dataNetworkHandler;

  /**
   * Data Channel with the client
   */
  private Channel dataChannel;

  /**
   * External address of the client (active)
   */
  private InetSocketAddress remoteAddress;

  /**
   * Local listening address for the server (passive)
   */
  private InetSocketAddress localAddress;

  /**
   * Active: the connection is done from the Server to the Client on this
   * remotePort Passive: not used
   */
  private int remotePort = -1;

  /**
   * Active: the connection is done from the Server from this localPort to the
   * Client Passive: the connection is
   * done from the Client to the Server on this localPort
   */
  private int localPort = -1;

  /**
   * Is the connection passive
   */
  private final AtomicBoolean passiveMode = new AtomicBoolean(false);

  /**
   * Is the server binded (active or passive, but mainly passive)
   */
  private final AtomicBoolean isBind = new AtomicBoolean(false);

  /**
   * The FtpTransferControl
   */
  private final FtpTransferControl transferControl;

  /**
   * Current TransferType. Default ASCII
   */
  private FtpArgumentCode.TransferType transferType =
      FtpArgumentCode.TransferType.ASCII;

  /**
   * Current TransferSubType. Default NONPRINT
   */
  private FtpArgumentCode.TransferSubType transferSubType =
      FtpArgumentCode.TransferSubType.NONPRINT;

  /**
   * Current TransferStructure. Default FILE
   */
  private FtpArgumentCode.TransferStructure transferStructure =
      FtpArgumentCode.TransferStructure.FILE;

  /**
   * Current TransferMode. Default Stream
   */
  private FtpArgumentCode.TransferMode transferMode =
      FtpArgumentCode.TransferMode.STREAM;

  /**
   * Constructor for Active session by default
   *
   * @param session
   */
  public FtpDataAsyncConn(final FtpSession session) {
    this.session = session;
    dataChannel = null;
    remoteAddress = FtpChannelUtils.getRemoteInetSocketAddress(
        this.session.getControlChannel());
    remotePort = remoteAddress.getPort();
    setDefaultLocalPort();
    resetLocalAddress();
    passiveMode.set(false);
    isBind.set(false);
    transferControl = new FtpTransferControl(session);
  }

  /**
   * @param channel
   *
   * @return True if the given channel is the same as the one currently
   *     registered
   */
  public final boolean checkCorrectChannel(final Channel channel) {
    if (dataChannel == null || channel == null) {
      return false;
    }
    return dataChannel.compareTo(channel) == 0;
  }

  /**
   * Clear the Data Connection
   */
  public synchronized void clear() {
    unbindPassive();
    transferControl.clear();
    passiveMode.set(false);
    remotePort = -1;
    localPort = -1;
  }

  /**
   * Set the local port to the default (20)
   */
  private void setDefaultLocalPort() {
    setLocalPort(session.getConfiguration().getServerPort() - 1);
    // Default L-1
  }

  /**
   * Set the Local Port (Active or Passive)
   *
   * @param localPort
   */
  public synchronized void setLocalPort(final int localPort) {
    this.localPort = localPort;
  }

  /**
   * @return the local address
   */
  public final InetSocketAddress getLocalAddress() {
    return localAddress;
  }

  /**
   * @return the remote address
   */
  public final InetSocketAddress getRemoteAddress() {
    return remoteAddress;
  }

  /**
   * @return the remotePort
   */
  public final int getRemotePort() {
    return remotePort;
  }

  /**
   * @return the localPort
   */
  public synchronized int getLocalPort() {
    return localPort;
  }

  private synchronized void resetLocalAddress() {
    localAddress = new InetSocketAddress(
        FtpChannelUtils.getLocalInetAddress(session.getControlChannel()),
        localPort);
  }

  /**
   * Change to active connection (reset localPort to default)
   *
   * @param address remote address
   */
  public synchronized void setActive(final InetSocketAddress address) {
    unbindPassive();
    setDefaultLocalPort();
    resetLocalAddress();
    remoteAddress = address;
    passiveMode.set(false);
    isBind.set(false);
    remotePort = remoteAddress.getPort();
    logger.debug("SetActive: {}", this);
  }

  /**
   * Change to passive connection (all necessaries informations like local
   * port
   * should have been set)
   */
  public final void setPassive() {
    unbindPassive();
    resetLocalAddress();
    passiveMode.set(true);
    isBind.set(false);
    logger.debug("SetPassive: {}", this);
  }

  /**
   * @return the passiveMode
   */
  public final boolean isPassiveMode() {
    return passiveMode.get();
  }

  /**
   * @return True if the connection is bind (active = connected, passive = not
   *     necessarily connected)
   */
  public final boolean isBind() {
    return isBind.get();
  }

  /**
   * Is the Data dataChannel connected
   *
   * @return True if the dataChannel is connected
   */
  public synchronized boolean isActive() {
    return dataChannel != null && dataChannel.isActive();
  }

  /**
   * @return the transferMode
   */
  public synchronized FtpArgumentCode.TransferMode getMode() {
    return transferMode;
  }

  /**
   * @param transferMode the transferMode to set
   */
  public synchronized void setMode(
      final FtpArgumentCode.TransferMode transferMode) {
    this.transferMode = transferMode;
    setCorrectCodec();
  }

  /**
   * @return the transferStructure
   */
  public synchronized FtpArgumentCode.TransferStructure getStructure() {
    return transferStructure;
  }

  /**
   * @param transferStructure the transferStructure to set
   */
  public synchronized void setStructure(
      final FtpArgumentCode.TransferStructure transferStructure) {
    this.transferStructure = transferStructure;
    setCorrectCodec();
  }

  /**
   * @return the transferSubType
   */
  public synchronized FtpArgumentCode.TransferSubType getSubType() {
    return transferSubType;
  }

  /**
   * @param transferSubType the transferSubType to set
   */
  public synchronized void setSubType(
      final FtpArgumentCode.TransferSubType transferSubType) {
    this.transferSubType = transferSubType;
    setCorrectCodec();
  }

  /**
   * @return the transferType
   */
  public synchronized FtpArgumentCode.TransferType getType() {
    return transferType;
  }

  /**
   * @param transferType the transferType to set
   */
  public synchronized void setType(
      final FtpArgumentCode.TransferType transferType) {
    this.transferType = transferType;
    setCorrectCodec();
  }

  /**
   * @return True if the current mode for data connection is FileInterface +
   *     (Stream or Block) + (Ascii or
   *     Image)
   */
  public final boolean isFileStreamBlockAsciiImage() {
    return transferStructure == TransferStructure.FILE &&
           (transferMode == TransferMode.STREAM ||
            transferMode == TransferMode.BLOCK) &&
           (transferType == TransferType.ASCII ||
            transferType == TransferType.IMAGE);
  }

  /**
   * @return True if the current mode for data connection is Stream
   */
  public final boolean isStreamFile() {
    return transferMode == TransferMode.STREAM &&
           transferStructure == TransferStructure.FILE;
  }

  /**
   * This function must be called after any changes of parameters, ie after
   * MODE, STRU, TYPE
   */
  private void setCorrectCodec() {
    try {
      getDataNetworkHandler().setCorrectCodec();
    } catch (final FtpNoConnectionException ignored) {
      // nothing
    }
  }

  /**
   * Unbind passive connection when close the Data Channel (from
   * channelInactive())
   */
  public synchronized void unbindPassive() {
    if (isBind.get() && passiveMode.get()) {
      isBind.set(false);
      final InetSocketAddress local = getLocalAddress();
      if (dataChannel != null && dataChannel.isActive()) {
        WaarpSslUtility.closingSslChannel(dataChannel);
      }
      session.getConfiguration().getFtpInternalConfiguration()
             .unbindPassive(local);
      // Previous mode was Passive so remove the current configuration if
      // any
      final InetAddress remote = remoteAddress.getAddress();
      session.getConfiguration().delFtpSession(remote, local);
    }
    dataChannel = null;
    dataNetworkHandler = null;
  }

  /**
   * Initialize the socket from Server side (only used in Passive)
   *
   * @return True if OK
   *
   * @throws Reply425Exception
   */
  public final boolean initPassiveConnection() throws Reply425Exception {
    unbindPassive();
    if (passiveMode.get()) {
      // Connection is enable but the client will do the real connection
      session.getConfiguration().getFtpInternalConfiguration()
             .bindPassive(getLocalAddress(), session.isDataSsl());
      isBind.set(true);
      return true;
    }
    // Connection is already prepared
    return true;
  }

  /**
   * Return the current Data Channel
   *
   * @return the current Data Channel
   *
   * @throws FtpNoConnectionException
   */
  public final Channel getCurrentDataChannel() throws FtpNoConnectionException {
    if (dataChannel == null) {
      throw new FtpNoConnectionException("No Data Connection active");
    }
    return dataChannel;
  }

  /**
   * @return the DataNetworkHandler
   *
   * @throws FtpNoConnectionException
   */
  public synchronized DataNetworkHandler getDataNetworkHandler()
      throws FtpNoConnectionException {
    if (dataNetworkHandler == null) {
      throw new FtpNoConnectionException("No Data Connection active");
    }
    return dataNetworkHandler;
  }

  /**
   * @param dataNetworkHandler the {@link DataNetworkHandler} to set
   */
  public synchronized void setDataNetworkHandler(
      final DataNetworkHandler dataNetworkHandler) {
    this.dataNetworkHandler = dataNetworkHandler;
  }

  /**
   * @param configuration
   *
   * @return a new Passive Port
   */
  public static int getNewPassivePort(final FtpConfiguration configuration) {
    return configuration.getNextRangePort();
  }

  /**
   * @return The current status in String of the different parameters
   */
  public final String getStatus() {
    final StringBuilder builder = new StringBuilder("Data connection: ").append(
        isActive()? "connected " : "not connected ").append(
        isBind()? "bind " : "not bind ").append(
        isPassiveMode()? "passive mode" : "active mode").append('\n').append(
        "Mode: ").append(transferMode.name()).append(" localPort: ").append(
        getLocalPort()).append(" remotePort: ").append(getRemotePort()).append(
        '\n').append("Structure: ").append(transferStructure.name()).append(
        '\n').append("Type: ").append(transferType.name()).append(' ').append(
        transferSubType.name());
    return builder.toString();
  }

  /**
   *
   */
  @Override
  public String toString() {
    return getStatus().replace('\n', ' ');
  }

  /**
   * @return the FtpTransferControl
   */
  public final FtpTransferControl getFtpTransferControl() {
    return transferControl;
  }

  /**
   * Set the new connected Data Channel
   *
   * @param dataChannel the new Data Channel
   *
   * @throws Reply425Exception
   */
  public synchronized void setNewOpenedDataChannel(final Channel dataChannel)
      throws Reply425Exception {
    this.dataChannel = dataChannel;
    if (dataChannel == null) {
      final String curmode;
      if (isPassiveMode()) {
        curmode = "passive";
      } else {
        curmode = "active";
      }
      // Cannot open connection
      throw new Reply425Exception(
          "Cannot open " + curmode + " data connection");
    }
    isBind.set(true);
  }
}
