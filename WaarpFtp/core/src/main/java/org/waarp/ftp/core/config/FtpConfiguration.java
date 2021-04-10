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

import io.netty.channel.Channel;
import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;
import org.waarp.common.file.FileParameterInterface;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpShutdownHook.ShutdownConfiguration;
import org.waarp.ftp.core.control.BusinessHandler;
import org.waarp.ftp.core.data.handler.DataBusinessHandler;
import org.waarp.ftp.core.exception.FtpNoConnectionException;
import org.waarp.ftp.core.exception.FtpUnknownFieldException;
import org.waarp.ftp.core.session.FtpSession;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Abstract class for configuration
 */
public abstract class FtpConfiguration {
  private static final String STOU = ".stou";
  private static final String PROPERTY_HAS_NO_VALUE = "Property has no value: ";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpConfiguration.class);
  /**
   * Global Accessor
   */
  public static Object ftpConfiguration;

  // FTP Configuration: Externals
  /**
   * Default session limit 64Mbit, so up to 8 full simultaneous clients
   */
  static final long DEFAULT_SESSION_LIMIT = 0x800000L;

  /**
   * Default global limit 512Mbit
   */
  static final long DEFAULT_GLOBAL_LIMIT = 0x4000000L;

  /**
   * Nb of milliseconds after pending data transfer is in timeout
   */
  private static long dataTimeoutCon = 5000;

  /**
   * PASSWORD for SHUTDOWN
   */
  private static final String FTP_PASSWORD = "FTP_PASSWORD";//NOSONAR

  // END OF STATIC VALUES
  /**
   * Internal configuration
   */
  private final FtpInternalConfiguration internalConfiguration;

  /**
   * SERVER PORT
   */
  private int serverPort = 21;
  /**
   * Default Address if any
   */
  private String serverAddress;

  /**
   * Base Directory
   */
  private String baseDirectory;

  /**
   * Associated FileParameterInterface
   */
  private final FileParameterInterface fileParameter;

  /**
   * True if the service is going to shutdown
   */
  private volatile boolean isShutdown;

  /**
   * Default number of threads in pool for Server. The default value is for
   * client for Executor in the Pipeline
   * for Business logic. Server will change this value on startup if not set.
   * Default 0 means in proportion of
   * real core number.
   */
  private int serverThread;

  /**
   * Default number of threads in pool for Client part.
   */
  private int clientThread = 80;

  /**
   * Which class owns this configuration
   */
  final Class<?> fromClass;

  /**
   * Which class will be used for DataBusinessHandler
   */
  final Class<? extends DataBusinessHandler> dataBusinessHandler;

  /**
   * Which class will be used for BusinessHandler
   */
  final Class<? extends BusinessHandler> businessHandler;

  /**
   * Internal Lock
   */
  private final ReentrantLock lock = new ReentrantLock();

  /**
   * Nb of milliseconds after connection is in timeout
   */
  private long timeoutCon = 30000;

  /**
   * Size by default of block size for receive/sending files. Should be a
   * multiple of 8192 (maximum = 64K due to
   * block limitation to 2 bytes)
   */
  private int blocksize = 0x10000; // 64K

  /**
   * Limit in Write byte/s to apply globally to the FTP Server
   */
  protected long serverGlobalWriteLimit = DEFAULT_GLOBAL_LIMIT;

  /**
   * Limit in Read byte/s to apply globally to the FTP Server
   */
  protected long serverGlobalReadLimit = DEFAULT_GLOBAL_LIMIT;

  /**
   * Limit in Write byte/s to apply by session to the FTP Server
   */
  protected long serverChannelWriteLimit = DEFAULT_SESSION_LIMIT;

  /**
   * Limit in Read byte/s to apply by session to the FTP Server
   */
  protected long serverChannelReadLimit = DEFAULT_SESSION_LIMIT;

  /**
   * Delay in ms between two checks
   */
  protected long delayLimit = 1000;

  /**
   * Should the file be deleted when the transfer is aborted on STOR like
   * commands
   */
  private boolean deleteOnAbort;

  /**
   * Max global memory limit: default is 1GB
   */
  private int maxGlobalMemory = 1073741824;

  /**
   * 1 = Active, -1 = Passive, 0 = Both
   */
  private int activePassiveMode = 0;

  /**
   * General Configuration Object
   */
  private final HashMap<String, Object> properties =
      new HashMap<String, Object>();

  /**
   * Use by ShutdownHook
   */
  private final ShutdownConfiguration shutdownConfiguration =
      new ShutdownConfiguration();

  /**
   * Simple constructor
   *
   * @param classtype Owner
   * @param businessHandler class that will be used for
   *     BusinessHandler
   * @param dataBusinessHandler class that will be used for
   *     DataBusinessHandler
   * @param fileParameter the FileParameterInterface to used
   */
  protected FtpConfiguration(final Class<?> classtype,
                             final Class<? extends BusinessHandler> businessHandler,
                             final Class<? extends DataBusinessHandler> dataBusinessHandler,
                             final FileParameterInterface fileParameter) {
    fromClass = classtype;
    this.dataBusinessHandler = dataBusinessHandler;
    this.businessHandler = businessHandler;
    internalConfiguration = new FtpInternalConfiguration(this);
    this.fileParameter = fileParameter;
    ftpConfiguration = this;
  }

  /**
   * @param key
   *
   * @return The String property associated to the key
   *
   * @throws FtpUnknownFieldException
   */
  public String getStringProperty(final String key)
      throws FtpUnknownFieldException {
    final String s = (String) properties.get(key);
    if (s == null) {
      throw new FtpUnknownFieldException(PROPERTY_HAS_NO_VALUE + key);
    }
    return s;
  }

  /**
   * @param key
   *
   * @return The Integer property associated to the key
   *
   * @throws FtpUnknownFieldException
   */
  public int getIntProperty(final String key) throws FtpUnknownFieldException {
    final Integer i = (Integer) properties.get(key);
    if (i == null) {
      throw new FtpUnknownFieldException(PROPERTY_HAS_NO_VALUE + key);
    }
    return i;
  }

  /**
   * @param key
   *
   * @return The File associated to the key
   *
   * @throws FtpUnknownFieldException
   */
  public File getFileProperty(final String key)
      throws FtpUnknownFieldException {
    final File f = (File) properties.get(key);
    if (f == null) {
      throw new FtpUnknownFieldException(PROPERTY_HAS_NO_VALUE + key);
    }
    return f;
  }

  /**
   * @param key
   *
   * @return The Object property associated to the key
   *
   * @throws FtpUnknownFieldException
   */
  public Object getProperty(final String key) throws FtpUnknownFieldException {
    final Object o = properties.get(key);
    if (o == null) {
      throw new FtpUnknownFieldException(PROPERTY_HAS_NO_VALUE + key);
    }
    return o;
  }

  /**
   * @return the TCP Port to listen in the Ftp Server
   */
  public int getServerPort() {
    return serverPort;
  }

  /**
   * @return the Address of the Ftp Server if any (may be null)
   */
  public String getServerAddress() {
    return serverAddress;
  }

  /**
   * @return the limit in Write byte/s to apply globally to the Ftp Server
   */
  public long getServerGlobalWriteLimit() {
    return serverGlobalWriteLimit;
  }

  /**
   * @return the limit in Write byte/s to apply for each session to the Ftp
   *     Server
   */
  public long getServerChannelWriteLimit() {
    return serverChannelWriteLimit;
  }

  /**
   * @return the limit in Read byte/s to apply globally to the Ftp Server
   */
  public long getServerGlobalReadLimit() {
    return serverGlobalReadLimit;
  }

  /**
   * @return the limit in Read byte/s to apply for each session to the Ftp
   *     Server
   */
  public long getServerChannelReadLimit() {
    return serverChannelReadLimit;
  }

  /**
   * @return the delayLimit to apply between two check
   */
  public long getDelayLimit() {
    return delayLimit;
  }

  /**
   * Check the password for Shutdown
   *
   * @param password
   *
   * @return True if the password is OK
   */
  public boolean checkPassword(final String password) {
    final String serverpassword;
    try {
      serverpassword = getStringProperty(FTP_PASSWORD);
    } catch (final FtpUnknownFieldException e) {
      return false;
    }
    return serverpassword.equals(password);
  }

  /**
   * Return the next available port for passive connections.
   *
   * @return the next available Port for Passive connections
   */
  public abstract int getNextRangePort();

  /**
   * @return the Base Directory of this Ftp Server
   */
  public String getBaseDirectory() {
    return baseDirectory;
  }

  /**
   * @param key
   * @param s
   */
  public void setStringProperty(final String key, final String s) {
    properties.put(key, s);
  }

  /**
   * @param key
   * @param i
   */
  public void setIntProperty(final String key, final int i) {
    properties.put(key, i);
  }

  /**
   * @param key
   * @param f
   */
  public void setFileProperty(final String key, final File f) {
    properties.put(key, f);
  }

  /**
   * @param key
   * @param o
   */
  public void setProperty(final String key, final Object o) {
    properties.put(key, o);
  }

  /**
   * @param port the new port
   */
  public void setServerPort(final int port) {
    serverPort = port;
  }

  /**
   * @param address the address to use while answering for address
   */
  public void setServerAddress(final String address) {
    serverAddress = address;
  }

  /**
   * @param dir the new base directory
   */
  public void setBaseDirectory(final String dir) {
    baseDirectory = dir;
  }

  /**
   * @param password the new password for shutdown
   */
  public void setPassword(final String password) {
    setStringProperty(FTP_PASSWORD, password);
  }

  /**
   * @return the dataBusinessHandler
   */
  public Class<? extends DataBusinessHandler> getDataBusinessHandler() {
    return dataBusinessHandler;
  }

  /**
   * Init internal configuration
   *
   * @throws FtpNoConnectionException
   */
  public void serverStartup() throws FtpNoConnectionException {
    logger.debug("Server Startup");
    internalConfiguration.serverStartup();
    logger.debug("Server Startup done");
  }

  /**
   * Reset the global monitor for bandwidth limitation and change future
   * channel
   * monitors with values divided by
   * 10 (channel = global / 10)
   *
   * @param writeLimit
   * @param readLimit
   */
  public void changeNetworkLimit(final long writeLimit, final long readLimit) {
    long newWriteLimit = writeLimit > 1024? writeLimit : serverGlobalWriteLimit;
    if (writeLimit <= 0) {
      newWriteLimit = 0;
    }
    long newReadLimit = readLimit > 1024? readLimit : serverGlobalReadLimit;
    if (readLimit <= 0) {
      newReadLimit = 0;
    }
    final FtpGlobalTrafficShapingHandler fgts =
        internalConfiguration.getGlobalTrafficShapingHandler();
    fgts.configure(newWriteLimit, newReadLimit);
    serverChannelReadLimit = newReadLimit / 10;
    serverChannelWriteLimit = newWriteLimit / 10;
    if (fgts instanceof GlobalChannelTrafficShapingHandler) {
      fgts.configureChannel(serverChannelWriteLimit, serverChannelReadLimit);
    }
  }

  /**
   * Compute number of threads for both client and server from the real number
   * of available processors (double +
   * 1) if the value is less than 64 threads.
   */
  public void computeNbThreads() {
    int nb = Runtime.getRuntime().availableProcessors() * 2 + 1;
    if (nb > 32) {
      nb = Runtime.getRuntime().availableProcessors() + 1;
    }
    if (getServerThread() < nb) {
      setServerThread(nb);
      setClientThread(getServerThread() * 10);
    } else if (getClientThread() < nb) {
      setClientThread(nb * 10);
    }
  }

  /**
   * In bind/unbind operation, lock
   */
  public void bindLock() {
    lock.lock();
  }

  /**
   * In bind/unbind operation, unlock
   */
  public void bindUnlock() {
    lock.unlock();
  }

  /**
   * @return the FtpInternalConfiguration
   */
  public FtpInternalConfiguration getFtpInternalConfiguration() {
    return internalConfiguration;
  }

  /**
   * Add a session from a couple of addresses
   *
   * @param ipOnly
   * @param fullIp
   * @param session
   */
  public void setNewFtpSession(final InetAddress ipOnly,
                               final InetSocketAddress fullIp,
                               final FtpSession session) {
    internalConfiguration.setNewFtpSession(ipOnly, fullIp, session);
  }

  /**
   * Return and remove the FtpSession
   *
   * @param channel
   * @param active
   *
   * @return the FtpSession if it exists associated to this channel
   */
  public FtpSession getFtpSession(final Channel channel, final boolean active) {
    return internalConfiguration.getFtpSession(channel, active, true);
  }

  /**
   * Return the FtpSession
   *
   * @param channel
   * @param active
   *
   * @return the FtpSession if it exists associated to this channel
   */
  public FtpSession getFtpSessionNoRemove(final Channel channel,
                                          final boolean active) {
    return internalConfiguration.getFtpSession(channel, active, false);
  }

  /**
   * Remove the FtpSession
   *
   * @param ipOnly
   * @param fullIp
   */
  public void delFtpSession(final InetAddress ipOnly,
                            final InetSocketAddress fullIp) {
    internalConfiguration.delFtpSession(ipOnly, fullIp);
  }

  /**
   * Test if the couple of addresses is already in the context
   *
   * @param ipOnly
   * @param fullIp
   *
   * @return True if the couple is present
   */
  public boolean hasFtpSession(final InetAddress ipOnly,
                               final InetSocketAddress fullIp) {
    return internalConfiguration.hasFtpSession(ipOnly, fullIp);
  }

  /**
   * @return the fileParameter
   */
  public FileParameterInterface getFileParameter() {
    return fileParameter;
  }

  public String getUniqueExtension() {
    // Can be overridden if necessary
    return STOU;
  }

  /**
   * To use if any external resources are to be released when shutting down
   */
  public void releaseResources() {
    internalConfiguration.releaseResources();
  }

  /**
   * Shutdown process is on going
   */
  public abstract void inShutdownProcess();

  /**
   * @return the isShutdown
   */
  public boolean isShutdown() {
    return isShutdown;
  }

  /**
   * @param isShutdown the isShutdown to set
   */
  public void setShutdown(final boolean isShutdown) {
    this.isShutdown = isShutdown;
  }

  /**
   * @return the sERVER_THREAD
   */
  public int getServerThread() {
    return serverThread;
  }

  /**
   * @param serverThread0 the sERVER_THREAD to set
   */
  public void setServerThread(final int serverThread0) {
    serverThread = serverThread0;
  }

  /**
   * @return the cLIENT_THREAD
   */
  public int getClientThread() {
    return clientThread;
  }

  /**
   * @param clientThread0 the cLIENT_THREAD to set
   */
  public void setClientThread(final int clientThread0) {
    clientThread = clientThread0;
  }

  /**
   * @return the tIMEOUTCON
   */
  public long getTimeoutCon() {
    return timeoutCon;
  }

  /**
   * @param tIMEOUTCON the tIMEOUTCON to set
   */
  public void setTimeoutCon(final long tIMEOUTCON) {
    timeoutCon = tIMEOUTCON;
  }

  /**
   * @return the bLOCKSIZE
   */
  public int getBlocksize() {
    return blocksize;
  }

  /**
   * @param bLOCKSIZE the bLOCKSIZE to set
   */
  public void setBlocksize(final int bLOCKSIZE) {
    blocksize = bLOCKSIZE;
  }

  /**
   * @return the deleteOnAbort
   */
  public boolean isDeleteOnAbort() {
    return deleteOnAbort;
  }

  /**
   * @param deleteOnAbort the deleteOnAbort to set
   */
  public void setDeleteOnAbort(final boolean deleteOnAbort) {
    this.deleteOnAbort = deleteOnAbort;
  }

  /**
   * @return the dATATIMEOUTCON
   */
  public static long getDataTimeoutCon() {
    return dataTimeoutCon;
  }

  /**
   * @param dATATIMEOUTCON the dATATIMEOUTCON to set
   */
  public static void setDataTimeoutCon(final long dATATIMEOUTCON) {
    dataTimeoutCon = dATATIMEOUTCON;
  }

  /**
   * @return the active (1) or passive (-1) or both (0) mode
   */
  public int getActivePassiveMode() {
    return activePassiveMode;
  }

  /**
   * @param activePassiveModeArg the mode to set (1 = Active, -1 = Passive, 0 = Both - default -)
   */
  public void setActivePassiveMode(final int activePassiveModeArg) {
    activePassiveMode =
        activePassiveModeArg < 0? -1 : (activePassiveModeArg > 0? 1 : 0);
  }

  /**
   * @return the maxGlobalMemory
   */
  public int getMaxGlobalMemory() {
    return maxGlobalMemory;
  }

  /**
   * @param maxGlobalMemory the maxGlobalMemory to set
   */
  public void setMaxGlobalMemory(final int maxGlobalMemory) {
    this.maxGlobalMemory = maxGlobalMemory;
  }

  /**
   * @return the shutdownConfiguration
   */
  public ShutdownConfiguration getShutdownConfiguration() {
    return shutdownConfiguration;
  }
}
