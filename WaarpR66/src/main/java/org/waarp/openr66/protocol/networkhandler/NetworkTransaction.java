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
package org.waarp.openr66.protocol.networkhandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelPipelineException;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.future.WaarpLock;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.lru.ConcurrentUtility;
import org.waarp.common.lru.SynchronizedLruCache;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBlackListedException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNetworkException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoDataException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoSslException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotAuthenticatedException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolRemoteShutdownException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.RetrieveRunner;
import org.waarp.openr66.protocol.localhandler.packet.AuthentPacket;
import org.waarp.openr66.protocol.localhandler.packet.ConnectionErrorPacket;
import org.waarp.openr66.protocol.networkhandler.packet.NetworkPacket;
import org.waarp.openr66.protocol.networkhandler.ssl.NetworkSslServerHandler;
import org.waarp.openr66.protocol.networkhandler.ssl.NetworkSslServerInitializer;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import static org.waarp.common.utility.WaarpShutdownHook.*;
import static org.waarp.openr66.context.R66FiniteDualStates.*;
import static org.waarp.openr66.protocol.configuration.Configuration.*;

/**
 * This class handles Network Transaction connections
 */
public class NetworkTransaction {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(NetworkTransaction.class);

  /**
   * To protect access to socketLocks when no address associated
   */
  private static final WaarpLock emptyLock = new WaarpLock();
  /**
   * Lock for Lock management operations
   */
  private static final ReentrantLock lockOfLock = new ReentrantLock();
  /**
   * Hashmap for lock based on remote address
   */
  private static final SynchronizedLruCache<Integer, WaarpLock>
      reentrantLockOnSocketAddressConcurrentHashMap =
      new SynchronizedLruCache<Integer, WaarpLock>(20000, 180000);
  /**
   * Hashmap for Currently Shutdown remote host based on
   * socketAddress.hashCode()
   */
  private static final ConcurrentHashMap<Integer, NetworkChannelReference>
      networkChannelShutdownOnSocketAddressConcurrentHashMap =
      new ConcurrentHashMap<Integer, NetworkChannelReference>();
  /**
   * Hashmap for Currently blacklisted remote host based on IP
   * address(String).hashCode()
   */
  private static final ConcurrentHashMap<Integer, NetworkChannelReference>
      networkChannelBlacklistedOnInetSocketAddressConcurrentHashMap =
      new ConcurrentHashMap<Integer, NetworkChannelReference>();

  /**
   * Hashmap for currently active remote host based on
   * socketAddress.hashCode()
   */
  private static final ConcurrentHashMap<Integer, NetworkChannelReference>
      networkChannelOnSocketAddressConcurrentHashMap =
      new ConcurrentHashMap<Integer, NetworkChannelReference>();
  /**
   * Remote Client NetworkChannels: used to centralize remote requester hosts
   * (possible different address used)
   */
  private static final ConcurrentHashMap<String, ClientNetworkChannels>
      clientNetworkChannelsPerHostId =
      new ConcurrentHashMap<String, ClientNetworkChannels>();
  /**
   * Hashmap for currently active Retrieve Runner (sender)
   */
  private static final ConcurrentHashMap<Integer, RetrieveRunner>
      retrieveRunnerConcurrentHashMap =
      new ConcurrentHashMap<Integer, RetrieveRunner>();

  private final Bootstrap clientBootstrap;
  private final Bootstrap clientSslBootstrap;
  private ChannelGroup networkChannelGroup;

  public NetworkTransaction() {
    networkChannelGroup = new DefaultChannelGroup("NetworkChannels",
                                                  Configuration.configuration.getSubTaskGroup()
                                                                             .next());
    Configuration.configuration.setupLimitHandler();
    clientBootstrap = new Bootstrap();
    if (Configuration.configuration.isUseNOSSL() &&
        Configuration.configuration.getHostId() != null) {
      final NetworkServerInitializer networkServerInitializer =
          new NetworkServerInitializer(false);
      WaarpNettyUtil.setBootstrap(clientBootstrap,
                                  Configuration.configuration.getNetworkWorkerGroup(),
                                  (int) Configuration.configuration.getTimeoutCon(),
                                  configuration.getBlockSize() + 64, false);
      clientBootstrap.handler(networkServerInitializer);
    }
    clientSslBootstrap = new Bootstrap();
    if (Configuration.configuration.isUseSSL() &&
        Configuration.configuration.getHostSslId() != null) {
      final NetworkSslServerInitializer networkSslServerInitializer =
          new NetworkSslServerInitializer(true);
      WaarpNettyUtil.setBootstrap(clientSslBootstrap,
                                  Configuration.configuration.getNetworkWorkerGroup(),
                                  (int) Configuration.configuration.getTimeoutCon(),
                                  configuration.getBlockSize() + 64, false);
      clientSslBootstrap.handler(networkSslServerInitializer);
    } else {
      if (Configuration.configuration.isWarnOnStartup()) {
        logger.warn("No SSL support configured");
      } else {
        logger.info("No SSL support configured");
      }
    }
  }

  public static String hashStatus() {
    final StringBuilder partial =
        new StringBuilder("NetworkTransaction: [InShutdown: ");
    partial.append(
               networkChannelShutdownOnSocketAddressConcurrentHashMap.size())
           .append(" Blacklisted: ").append(
               networkChannelBlacklistedOnInetSocketAddressConcurrentHashMap.size())
           .append("\n RetrieveRunner: ")
           .append(retrieveRunnerConcurrentHashMap.size())
           .append(" ClientNetworkChannels: ")
           .append(clientNetworkChannelsPerHostId.size());
    int nb = 0;
    for (final ClientNetworkChannels clientNetworkChannels : clientNetworkChannelsPerHostId.values()) {
      nb += clientNetworkChannels.size();
    }
    partial.append(" Sum of ClientNetworkChannels NetworkClients: ").append(nb);
    nb = 0;
    for (final NetworkChannelReference ncr : networkChannelOnSocketAddressConcurrentHashMap.values()) {
      nb += ncr.nbLocalChannels();
      partial.append("\n NetworkChannels: ").append(ncr.toString());
    }
    partial.append("\n NetworkChannels: ")
           .append(networkChannelOnSocketAddressConcurrentHashMap.size())
           .append(" LockOnSocketAddress: ")
           .append(reentrantLockOnSocketAddressConcurrentHashMap.size())
           .append(" Sum of NetworkChannels LocalClients: ").append(nb)
           .append("] ");
    return partial.toString();
  }

  private static WaarpLock getLockNCR(final SocketAddress sa) {
    final int key = sa.hashCode();
    final WaarpLock value =
        reentrantLockOnSocketAddressConcurrentHashMap.get(key);
    if (value != null) {
      reentrantLockOnSocketAddressConcurrentHashMap.updateTtl(key);
    }
    return value;
  }

  private static void addLockNCR(final SocketAddress sa, final WaarpLock lock) {
    reentrantLockOnSocketAddressConcurrentHashMap.put(sa.hashCode(), lock);
  }

  private static void addNCR(final NetworkChannelReference ncr) {
    networkChannelOnSocketAddressConcurrentHashMap.put(ncr.getSocketHashCode(),
                                                       ncr);
  }

  private static NetworkChannelReference removeNCR(
      final NetworkChannelReference ncr) {
    return networkChannelOnSocketAddressConcurrentHashMap.remove(
        ncr.getSocketHashCode());
  }

  private static NetworkChannelReference getNCR(final SocketAddress sa) {
    return networkChannelOnSocketAddressConcurrentHashMap.get(sa.hashCode());
  }

  private static boolean containsNCR(final SocketAddress address) {
    return networkChannelOnSocketAddressConcurrentHashMap.containsKey(
        address.hashCode());
  }

  private static void addShutdownNCR(final NetworkChannelReference ncr) {
    networkChannelShutdownOnSocketAddressConcurrentHashMap.put(
        ncr.getSocketHashCode(), ncr);
  }

  private static NetworkChannelReference removeShutdownNCR(
      final NetworkChannelReference ncr) {
    return networkChannelShutdownOnSocketAddressConcurrentHashMap.remove(
        ncr.getSocketHashCode());
  }

  private static boolean containsShutdownNCR(
      final NetworkChannelReference ncr) {
    return networkChannelShutdownOnSocketAddressConcurrentHashMap.containsKey(
        ncr.getSocketHashCode());
  }

  private static boolean containsShutdownNCR(final SocketAddress sa) {
    return networkChannelShutdownOnSocketAddressConcurrentHashMap.containsKey(
        sa.hashCode());
  }

  private static NetworkChannelReference getShutdownNCR(
      final SocketAddress sa) {
    return networkChannelShutdownOnSocketAddressConcurrentHashMap.get(
        sa.hashCode());
  }

  private static void addBlacklistNCR(final NetworkChannelReference ncr) {
    networkChannelBlacklistedOnInetSocketAddressConcurrentHashMap.put(
        ncr.getAddressHashCode(), ncr);
  }

  private static NetworkChannelReference removeBlacklistNCR(
      final NetworkChannelReference ncr) {
    return networkChannelBlacklistedOnInetSocketAddressConcurrentHashMap.remove(
        ncr.getAddressHashCode());
  }

  private static boolean containsBlacklistNCR(
      final NetworkChannelReference ncr) {
    return networkChannelBlacklistedOnInetSocketAddressConcurrentHashMap.containsKey(
        ncr.getAddressHashCode());
  }

  private static boolean containsBlacklistNCR(final SocketAddress address) {
    return networkChannelBlacklistedOnInetSocketAddressConcurrentHashMap.containsKey(
        address.hashCode());
  }

  private static NetworkChannelReference getBlacklistNCR(
      final SocketAddress sa) {
    final InetAddress address = ((InetSocketAddress) sa).getAddress();
    if (address == null) {
      return null;
    }

    return networkChannelBlacklistedOnInetSocketAddressConcurrentHashMap.get(
        address.getHostAddress().hashCode());
  }

  private static WaarpLock getChannelLock(final SocketAddress socketAddress) {
    lockOfLock.lock();
    try {
      if (socketAddress == null) {
        // should not
        logger.info("SocketAddress empty here !");
        return emptyLock;
      }
      WaarpLock socketLock = getLockNCR(socketAddress);
      if (socketLock == null) {
        socketLock = new WaarpLock(true);
      }
      // update TTL
      addLockNCR(socketAddress, socketLock);
      return socketLock;
    } finally {
      lockOfLock.unlock();
    }
  }

  private static void removeChannelLock() {
    lockOfLock.lock();
    try {
      reentrantLockOnSocketAddressConcurrentHashMap.forceClearOldest();
    } finally {
      lockOfLock.unlock();
    }
  }

  /**
   * Create a connection to the specified socketAddress with multiple retries
   *
   * @param socketAddress
   * @param isSSL
   * @param futureRequest
   *
   * @return the LocalChannelReference
   */
  public final LocalChannelReference createConnectionWithRetry(
      final SocketAddress socketAddress, final boolean isSSL,
      final R66Future futureRequest) {
    try {
      return createConnectionWithRetryWithAuthenticationException(socketAddress,
                                                                  isSSL,
                                                                  futureRequest);
    } catch (final OpenR66ProtocolNotAuthenticatedException e) {
      // Correct return!
      return null;
    }
  }

  /**
   * Create a connection to the specified socketAddress with multiple retries
   *
   * @param socketAddress
   * @param isSSL
   * @param futureRequest
   *
   * @return the LocalChannelReference
   *
   * @throws OpenR66ProtocolNotAuthenticatedException
   */
  public final LocalChannelReference createConnectionWithRetryWithAuthenticationException(
      final SocketAddress socketAddress, final boolean isSSL,
      final R66Future futureRequest)
      throws OpenR66ProtocolNotAuthenticatedException {
    LocalChannelReference localChannelReference = null;
    for (int i = 0; i < Configuration.RETRYNB; i++) {
      if (WaarpShutdownHook.isShutdownStarting()) {
        logger.error("Cannot connect : Local system in shutdown");
        break;
      }
      try {
        localChannelReference =
            createConnection(socketAddress, isSSL, futureRequest);
        break;
      } catch (final OpenR66ProtocolRemoteShutdownException e) {
        logger.warn("Cannot connect : {}", e.getMessage());
        logger.debug(e);
        break;
      } catch (final OpenR66ProtocolBlackListedException e) {
        logger.error("Cannot connect : {}", e.getMessage());
        logger.debug(e);
        break;
      } catch (final OpenR66ProtocolNoConnectionException e) {
        logger.error("Cannot connect : {}", e.getMessage());
        logger.debug(e);
        break;
      } catch (final OpenR66ProtocolNotAuthenticatedException e) {
        logger.error("Cannot be authenticated : {}", e.getMessage());
        logger.debug(e);
        throw e;
      } catch (final OpenR66ProtocolNetworkException e) {
        // Can retry
        logger.error("Cannot connect : {}. Will retry", e.getMessage());
        logger.debug(e);
        try {
          Thread.sleep(Configuration.configuration.getDelayRetry());
        } catch (final InterruptedException e1) {//NOSONAR
          SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
        }
      }
    }
    if (localChannelReference != null) {
      logger.info("Connected");
    }
    return localChannelReference;
  }

  /**
   * Create a connection to the specified socketAddress
   *
   * @param socketAddress
   * @param isSSL
   * @param futureRequest
   *
   * @return the LocalChannelReference
   *
   * @throws OpenR66ProtocolNetworkException
   * @throws OpenR66ProtocolRemoteShutdownException
   * @throws OpenR66ProtocolBlackListedException
   * @throws OpenR66ProtocolNoConnectionException
   * @throws OpenR66ProtocolNotAuthenticatedException
   */
  private LocalChannelReference createConnection(
      final SocketAddress socketAddress, final boolean isSSL,
      final R66Future futureRequest) throws OpenR66ProtocolNetworkException,
                                            OpenR66ProtocolRemoteShutdownException,
                                            OpenR66ProtocolNoConnectionException,
                                            OpenR66ProtocolNotAuthenticatedException,
                                            OpenR66ProtocolBlackListedException {
    NetworkChannelReference networkChannelReference = null;
    LocalChannelReference localChannelReference;
    boolean ok = false;
    // check valid limit on server side only (could be the initiator but not a client)
    final DbHostAuth auth =
        isSSL? Configuration.configuration.getHostSslAuth() :
            Configuration.configuration.getHostAuth();
    if (!auth.isClient()) {
      boolean valid = false;
      for (int i = 0; i < Configuration.RETRYNB * 2; i++) {
        if (Configuration.configuration.getConstraintLimitHandler()
                                       .checkConstraintsSleep(i)) {
          logger.info("Constraints exceeded: {}", i);
        } else {
          logger.debug("Constraints NOT exceeded");
          valid = true;
          break;
        }
      }
      if (!valid) {
        // Limit is locally exceeded
        throw new OpenR66ProtocolNetworkException(
            "Cannot connect to remote server due to local overload");
      }
    }
    try {
      // Ensure networkChannelReference and localChannelReference are linked
      networkChannelReference = createNewConnection(socketAddress, isSSL);
      try {
        localChannelReference =
            Configuration.configuration.getLocalTransaction()
                                       .createNewClient(networkChannelReference,
                                                        ChannelUtils.NOCHANNEL,
                                                        futureRequest, isSSL);
      } catch (final NullPointerException e) {
        throw new OpenR66ProtocolNetworkException(e);
      }
      ok = true;
    } finally {
      if (!ok && networkChannelReference != null) {
        checkClosingNetworkChannel(networkChannelReference, null);
      }
    }
    if (localChannelReference.getFutureValidateStartup().isSuccess()) {
      sendValidationConnection(localChannelReference);
    } else {
      final OpenR66ProtocolNetworkException exc =
          new OpenR66ProtocolNetworkException("Startup is invalid");
      logger.info("Startup is Invalid", exc);
      final R66Result finalValue =
          new R66Result(exc, null, true, ErrorCode.ConnectionImpossible, null);
      localChannelReference.invalidateRequest(finalValue);
      localChannelReference.close();
      throw exc;
    }
    return localChannelReference;
  }

  /**
   * @param socketServerAddress
   * @param isSSL
   *
   * @return the NetworkChannelReference
   *
   * @throws OpenR66ProtocolNetworkException
   * @throws OpenR66ProtocolRemoteShutdownException
   * @throws OpenR66ProtocolBlackListedException
   * @throws OpenR66ProtocolNoConnectionException
   */
  private NetworkChannelReference createNewConnection(
      final SocketAddress socketServerAddress, final boolean isSSL)
      throws OpenR66ProtocolNetworkException,
             OpenR66ProtocolRemoteShutdownException,
             OpenR66ProtocolBlackListedException,
             OpenR66ProtocolNoConnectionException {
    final WaarpLock socketLock = getChannelLock(socketServerAddress);
    NetworkChannelReference networkChannelReference;
    socketLock.lock();
    try {
      try {
        networkChannelReference = getRemoteChannel(socketServerAddress);
      } catch (final OpenR66ProtocolNoDataException e1) {
        networkChannelReference = null;
      }
      if (networkChannelReference != null) {
        networkChannelReference.use();
        logger.info("Already Connected: {}", networkChannelReference);
        return networkChannelReference;
      }
      logger.debug("NEW PHYSICAL CONNECTION REQUIRED");
      ChannelFuture channelFuture = null;
      for (int i = 0; i < Configuration.RETRYNB; i++) {
        if (WaarpShutdownHook.isShutdownStarting()) {
          throw new OpenR66ProtocolNoConnectionException(
              "Local system in shutdown");
        }
        try {
          if (isSSL) {
            if (Configuration.configuration.getHostSslId() != null) {
              channelFuture = clientSslBootstrap.connect(socketServerAddress);
            } else {
              throw new OpenR66ProtocolNoConnectionException("No SSL support");
            }
          } else {
            channelFuture = clientBootstrap.connect(socketServerAddress);
          }
        } catch (final ChannelPipelineException e) {
          throw new OpenR66ProtocolNoConnectionException(
              "Cannot connect to remote server due to a channel exception");
        }
        WaarpNettyUtil.awaitOrInterrupted(channelFuture,
                                          Configuration.configuration.getTimeoutCon());
        if (channelFuture.isSuccess()) {
          final Channel channel = channelFuture.channel();
          if (isSSL &&
              !NetworkSslServerHandler.isSslConnectedChannel(channel)) {
            logger.info("KO CONNECT since SSL handshake is over");
            channel.close();
            throw new OpenR66ProtocolNoConnectionException(
                "Cannot finish connect to remote server");
          }
          if (networkChannelGroup != null) {
            networkChannelGroup.add(channel);
          }
          networkChannelReference =
              new NetworkChannelReference(channel, socketLock, isSSL);
          addNCR(networkChannelReference);
          logger.info("New Real Connection: {}", networkChannelReference);
          return networkChannelReference;
        } else {
          try {
            Thread.sleep(Configuration.RETRYINMS);
          } catch (final InterruptedException e) {//NOSONAR
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
          }
          if (!channelFuture.isDone()) {
            throw new OpenR66ProtocolNoConnectionException(
                "Cannot connect to remote server since received an " +
                "interruption");
          }
          if (channelFuture.cause() instanceof ConnectException) {
            logger.debug("KO CONNECT: {}", channelFuture.cause().getMessage());
            throw new OpenR66ProtocolNoConnectionException(
                channelFuture.cause().getMessage(), channelFuture.cause());
          } else {
            logger.debug("KO CONNECT but retry", channelFuture.cause());
          }
        }
      }
      if (channelFuture != null) {
        throw new OpenR66ProtocolNetworkException(
            "Cannot connect to remote server", channelFuture.cause());
      } else {
        throw new OpenR66ProtocolNetworkException(
            "Cannot connect to remote server");
      }
    } finally {
      socketLock.unlock();
    }
  }

  /**
   * Create the LocalChannelReference when a remote local channel starts its
   * connection
   *
   * @param networkChannelReference
   * @param startupPacket
   *
   * @return TransferReference
   */
  public static LocalChannelReference createConnectionFromNetworkChannelStartup(
      final NetworkChannelReference networkChannelReference,
      final NetworkPacket startupPacket, final boolean fromSsl) {
    logger.debug("Startup {}", startupPacket);
    final Channel channel = networkChannelReference.channel();
    try {
      return Configuration.configuration.getLocalTransaction().createNewClient(
          networkChannelReference, startupPacket.getRemoteId(), null, fromSsl);
    } catch (final OpenR66ProtocolRemoteShutdownException e1) {
      logger.info("Will Close Local from Network Channel");
      WaarpSslUtility.closingSslChannel(channel);
      startupPacket.clear();
    } catch (final OpenR66ProtocolNoConnectionException e1) {
      logger.error(
          "Cannot create LocalChannel for: " + startupPacket + " due to " +
          e1.getMessage());
      final ConnectionErrorPacket error = new ConnectionErrorPacket(
          "Cannot connect to localChannel since cannot create it", null);
      NetworkServerHandler.writeError(channel, startupPacket.getRemoteId(),
                                      startupPacket.getLocalId(), error);
      checkClosingNetworkChannel(networkChannelReference, null);
      startupPacket.clear();
    }
    return null;
  }

  /**
   * Send a validation of connection with Authentication
   *
   * @param localChannelReference
   *
   * @throws OpenR66ProtocolNetworkException
   * @throws OpenR66ProtocolNotAuthenticatedException
   */
  private void sendValidationConnection(
      final LocalChannelReference localChannelReference)
      throws OpenR66ProtocolNetworkException,
             OpenR66ProtocolNotAuthenticatedException {
    final AuthentPacket authent;

    try {
      final DbHostAuth auth =
          localChannelReference.getNetworkServerHandler().isSsl()?
              Configuration.configuration.getHostSslAuth() :
              Configuration.configuration.getHostAuth();
      authent = new AuthentPacket(Configuration.configuration.getHostId(
          localChannelReference.getNetworkServerHandler().isSsl()),
                                  FilesystemBasedDigest.passwdCrypt(
                                      auth.getHostkey()),
                                  localChannelReference.getLocalId());
    } catch (final OpenR66ProtocolNoSslException e1) {
      final R66Result finalValue = new R66Result(
          new OpenR66ProtocolSystemException("No SSL support", e1),
          localChannelReference.getSession(), true,
          ErrorCode.ConnectionImpossible, null);
      logger.error("Authent is Invalid due to no SSL: {}", e1.getMessage());
      if (localChannelReference.getRemoteId()
                               .compareTo(ChannelUtils.NOCHANNEL) == 0) {
        final ConnectionErrorPacket error = new ConnectionErrorPacket(
            "Cannot connect to localChannel since SSL is not supported", null);
        try {
          ChannelUtils.writeAbstractLocalPacket(localChannelReference, error,
                                                false);
        } catch (final OpenR66ProtocolPacketException ignored) {
          // nothing
        }
      }
      localChannelReference.invalidateRequest(finalValue);
      localChannelReference.close();
      throw new OpenR66ProtocolNetworkException(e1);
    }
    logger.debug("Will send request of connection validation");
    localChannelReference.sessionNewState(AUTHENTR);
    try {
      ChannelUtils.writeAbstractLocalPacket(localChannelReference, authent,
                                            false);
    } catch (final OpenR66ProtocolPacketException e) {
      final R66Result finalValue = new R66Result(
          new OpenR66ProtocolSystemException("Wrong Authent Protocol", e),
          localChannelReference.getSession(), true,
          ErrorCode.ConnectionImpossible, null);
      logger.error("Authent is Invalid due to protocol: {}", e.getMessage());
      localChannelReference.invalidateRequest(finalValue);
      if (!localChannelReference.getRemoteId().equals(ChannelUtils.NOCHANNEL)) {
        final ConnectionErrorPacket error = new ConnectionErrorPacket(
            "Cannot connect to localChannel since Authent Protocol is invalid",
            null);
        try {
          ChannelUtils.writeAbstractLocalPacket(localChannelReference, error,
                                                false);
        } catch (final OpenR66ProtocolPacketException ignored) {
          // nothing
        }
      }
      localChannelReference.close();
      throw new OpenR66ProtocolNetworkException("Bad packet", e);
    }
    final R66Future future =
        localChannelReference.getFutureValidateConnection();
    if (future.isFailed()) {
      logger.debug("Will close NETWORK channel since Future cancelled: {}",
                   future);
      final R66Result finalValue = new R66Result(
          new OpenR66ProtocolSystemException(
              "Connection invalid during Authentication"),
          localChannelReference.getSession(), true,
          ErrorCode.ConnectionImpossible, null);
      logger.info("Authent is Invalid due to: {} {}",
                  finalValue.getException().getMessage(), future.toString());
      localChannelReference.invalidateRequest(finalValue);
      if (!localChannelReference.getRemoteId().equals(ChannelUtils.NOCHANNEL)) {
        final ConnectionErrorPacket error = new ConnectionErrorPacket(
            "Cannot connect to localChannel with Out of Time", null);
        try {
          ChannelUtils.writeAbstractLocalPacket(localChannelReference, error,
                                                false);
        } catch (final OpenR66ProtocolPacketException ignored) {
          // nothing
        }
      }
      localChannelReference.close();
      throw new OpenR66ProtocolNotAuthenticatedException(
          "Cannot validate connection: " + future.getResult(),
          future.getCause());
    }
  }

  /**
   * Add a new NetworkChannel from connection
   *
   * @param channel
   * @param isSsl
   *
   * @throws OpenR66ProtocolRemoteShutdownException
   * @throws OpenR66ProtocolBlackListedException
   */
  public static NetworkChannelReference addNetworkChannel(final Channel channel,
                                                          final boolean isSsl)
      throws OpenR66ProtocolRemoteShutdownException,
             OpenR66ProtocolBlackListedException {
    final SocketAddress socketAddress = channel.remoteAddress();
    final WaarpLock socketLock = getChannelLock(socketAddress);
    socketLock.lock();
    try {
      NetworkChannelReference nc = null;
      try {
        nc = getRemoteChannel(socketAddress);
      } catch (final OpenR66ProtocolNoDataException ignored) {
        // nothing
      }
      if (nc == null) {
        // not an issue: needs to be created
        nc = new NetworkChannelReference(channel, socketLock, isSsl);
        addNCR(nc);
      }
      return nc;
    } finally {
      socketLock.unlock();
    }
  }

  /**
   * To be called when a remote server seems to be down for a while, so to not
   * retry immediately
   *
   * @param socketAddress
   */
  public static void proposeShutdownNetworkChannel(
      final SocketAddress socketAddress, final boolean isSSL) {
    final WaarpLock lock = getChannelLock(socketAddress);
    lock.lock(Configuration.WAITFORNETOP, TimeUnit.MILLISECONDS);
    try {
      logger.info("Seem Shutdown: {}", socketAddress);
      if (containsShutdownNCR(socketAddress)) {
        // already done
        logger.debug("Already set as shutdown");
        return;
      }
      if (containsBlacklistNCR(socketAddress)) {
        // already done
        logger.debug("Already set as blocked");
        return;
      }
      if (containsNCR(socketAddress)) {
        // already done
        logger.debug("Still existing so shutdown is refused");
        return;
      }
      logger.warn(
          "This host address will be set as unavailable for 3xTIMEOUT since not reacheable multiple times: {}",
          socketAddress);
      final NetworkChannelReference networkChannelReference =
          new NetworkChannelReference(socketAddress, lock, isSSL);
      addShutdownNCR(networkChannelReference);
      if (Configuration.configuration.isTimerCloseReady()) {
        final R66ShutdownNetworkChannelTimerTask timerTask;
        try {
          timerTask =
              new R66ShutdownNetworkChannelTimerTask(networkChannelReference,
                                                     false);
          Configuration.configuration.getTimerClose().newTimeout(timerTask,
                                                                 Configuration.configuration.getTimeoutCon(),
                                                                 TimeUnit.MILLISECONDS);
        } catch (final OpenR66RunnerErrorException e) {
          // ignore
        }
      }
    } finally {
      lock.unlock();
    }
  }

  /**
   * Shutdown one Network Channel
   *
   * @param networkChannelReference
   */
  private static void shuttingDownNetworkChannelInternal(
      final NetworkChannelReference networkChannelReference) {
    logger.info("Shutdown: {}", networkChannelReference);
    if (networkChannelReference != null &&
        containsShutdownNCR(networkChannelReference)) {
      // already done
      logger.debug("Already set as shutdown");
      return;
    }
    logger.debug("Set as shutdown");
    if (networkChannelReference != null) {
      addShutdownNCR(networkChannelReference);
      if (!networkChannelReference.isShuttingDown) {
        networkChannelReference.shutdownAllLocalChannels();
      }
      if (Configuration.configuration.isTimerCloseReady()) {
        final R66ShutdownNetworkChannelTimerTask timerTask;
        try {
          timerTask =
              new R66ShutdownNetworkChannelTimerTask(networkChannelReference,
                                                     false);
          Configuration.configuration.getTimerClose().newTimeout(timerTask,
                                                                 Configuration.configuration.getTimeoutCon(),
                                                                 TimeUnit.MILLISECONDS);
        } catch (final OpenR66RunnerErrorException e) {
          // ignore
        }
      }
    }
  }

  /**
   * Shutdown one Network Channel
   *
   * @param networkChannelReference
   */
  public static void shuttingDownNetworkChannel(
      final NetworkChannelReference networkChannelReference) {
    shuttingDownNetworkChannelInternal(networkChannelReference);
  }

  /**
   * Shutdown a NetworkChannel and add it to BlaclList
   *
   * @param networkChannelReference
   *
   * @return True if this channel is now blacklisted for a while
   */
  public static boolean shuttingDownNetworkChannelBlackList(
      final NetworkChannelReference networkChannelReference) {
    shuttingDownNetworkChannelInternal(networkChannelReference);
    if (!Configuration.configuration.isBlacklistBadAuthent()) {
      return false;
    }
    if (containsBlacklistNCR(networkChannelReference)) {
      return false;
    }
    addBlacklistNCR(networkChannelReference);
    if (Configuration.configuration.isTimerCloseReady()) {
      final R66ShutdownNetworkChannelTimerTask timerTask;
      try {
        timerTask =
            new R66ShutdownNetworkChannelTimerTask(networkChannelReference,
                                                   true);
        Configuration.configuration.getTimerClose().newTimeout(timerTask,
                                                               Configuration.configuration.getTimeoutCon() *
                                                               5,
                                                               TimeUnit.MILLISECONDS);
      } catch (final OpenR66RunnerErrorException e) {
        // ignore
      }
    }
    return true;
  }

  /**
   * @param channel
   *
   * @return True if this channel is blacklisted
   */
  public static boolean isBlacklisted(final Channel channel) {
    if (!Configuration.configuration.isBlacklistBadAuthent()) {
      return false;
    }
    final SocketAddress address = channel.remoteAddress();
    if (address == null) {
      return false;
    }

    final NetworkChannelReference networkChannelReference =
        getBlacklistNCR(address);
    return networkChannelReference != null;
  }

  /**
   * @param address
   *
   * @return True if this address (associated channel) is currently in
   *     shutdown (or if this channel is not valid)
   */
  public static boolean isShuttingdownNetworkChannel(
      final SocketAddress address) {
    return !isAddressValid(address);
  }

  /**
   * Shutdown NetworkChannelReference as client
   *
   * @param requester
   *
   * @return True if shutdown occurs
   */
  public static boolean shuttingdownNetworkChannelsPerHostID(
      final String requester) {
    if (requester == null) {
      return false;
    }
    final ClientNetworkChannels clientNetworkChannels =
        clientNetworkChannelsPerHostId.get(requester);
    logger.debug("AddClient: shutdown previous exist? " +
                 (clientNetworkChannels != null) + " for :" + requester);
    if (clientNetworkChannels != null) {
      return clientNetworkChannels.shutdownAllNetworkChannels();
    }
    return false;
  }

  /**
   * Add a requester channel
   *
   * @param networkChannelReference
   * @param requester
   */
  public static void addClient(
      final NetworkChannelReference networkChannelReference,
      final String requester) {
    synchronized (clientNetworkChannelsPerHostId) {
      if (networkChannelReference != null && requester != null) {
        ClientNetworkChannels clientNetworkChannels =
            clientNetworkChannelsPerHostId.get(requester);
        if (clientNetworkChannels == null) {
          clientNetworkChannels = new ClientNetworkChannels(requester);
          clientNetworkChannelsPerHostId.put(requester, clientNetworkChannels);
        }
        clientNetworkChannels.add(networkChannelReference);
        logger.debug("AddClient: add count? {} for {}",
                     clientNetworkChannels.size(), requester);
      }
    }
  }

  private static void removeClient(
      final NetworkChannelReference networkChannelReference,
      final String requester,
      final ClientNetworkChannels clientNetworkChannels) {
    if (networkChannelReference != null && clientNetworkChannels != null &&
        requester != null) {
      clientNetworkChannels.remove(networkChannelReference);
      logger.debug("removeClient: remove for :{} still {}", requester,
                   clientNetworkChannels.size());
      if (clientNetworkChannels.isEmpty()) {
        clientNetworkChannelsPerHostId.remove(requester);
      }
    }
  }

  /**
   * @param requester
   *
   * @return The number of NetworkChannels associated with this requester
   */
  public static int getNumberClients(final String requester) {
    final ClientNetworkChannels clientNetworkChannels =
        clientNetworkChannelsPerHostId.get(requester);
    if (clientNetworkChannels != null) {
      return clientNetworkChannels.size();
    }
    return 0;
  }

  /**
   * Force remove of NetworkChannelReference when it is closed
   *
   * @param networkChannelReference
   */
  public static void closedNetworkChannel(
      final NetworkChannelReference networkChannelReference) {
    if (networkChannelReference == null) {
      return;
    }
    try {
      if (!networkChannelReference.isShuttingDown) {
        networkChannelReference.shutdownAllLocalChannels();
      }
      logger.debug("NC left: {}", networkChannelReference);
      removeNCR(networkChannelReference);
      if (networkChannelReference.clientNetworkChannels != null) {
        final String requester =
            networkChannelReference.clientNetworkChannels.getHostId();
        removeClient(networkChannelReference, requester,
                     networkChannelReference.clientNetworkChannels);
      } else if (networkChannelReference.getHostId() != null) {
        final String requester = networkChannelReference.getHostId();
        final ClientNetworkChannels clientNetworkChannels =
            clientNetworkChannelsPerHostId.get(requester);
        if (clientNetworkChannels != null) {
          removeClient(networkChannelReference, requester,
                       clientNetworkChannels);
        }
      }
    } finally {
      removeChannelLock();
    }
  }

  /**
   * Force remove of NetworkChannelReference when it is closed
   *
   * @param address
   */
  public static void closedNetworkChannel(final SocketAddress address) {
    if (address == null) {
      return;
    }
    final NetworkChannelReference networkChannelReference =
        networkChannelOnSocketAddressConcurrentHashMap.get(address.hashCode());
    closedNetworkChannel(networkChannelReference);
  }

  /**
   * Class to close the Network Channel if after some delays it has really no
   * Local Channel attached
   */
  private static class CloseFutureChannel implements TimerTask {

    private static final Set<ChannelId> inCloseRunning =
        ConcurrentUtility.newConcurrentSet();
    private final NetworkChannelReference networkChannelReference;

    /**
     * @param networkChannelReference
     *
     * @throws OpenR66RunnerErrorException
     */
    private CloseFutureChannel(
        final NetworkChannelReference networkChannelReference)
        throws OpenR66RunnerErrorException {
      if (!inCloseRunning.add(networkChannelReference.channel.id())) {
        throw new OpenR66RunnerErrorException("Already scheduled");
      }
      this.networkChannelReference = networkChannelReference;
    }

    @Override
    public void run(final Timeout timeout) {
      final long time = networkChannelReference.shutdownAllowed();
      if (time > 0) {
        Configuration.configuration.getTimerClose().newTimeout(this, time,
                                                               TimeUnit.MILLISECONDS);
        return;
      } else if (time == 0) {
        networkChannelReference.isShuttingDown = true;
        WaarpSslUtility.closingSslChannel(networkChannelReference.channel);
      }
    }

  }

  /**
   * Check if closing of the localChannel will bring future closing of
   * NetworkChannel
   *
   * @param networkChannelReference
   * @param localChannelReference
   *
   * @return the number of local channel still connected to this channel
   */
  public static int checkClosingNetworkChannel(
      final NetworkChannelReference networkChannelReference,
      final LocalChannelReference localChannelReference) {
    networkChannelReference.lock.lock(Configuration.WAITFORNETOP,
                                      TimeUnit.MILLISECONDS);
    try {
      logger.debug("Close con: {}", networkChannelReference);
      if (localChannelReference != null) {
        networkChannelReference.closeAndRemove(localChannelReference);
      }
      final int count = networkChannelReference.nbLocalChannels();
      if (count <= 0) {
        logger.debug("Will try to Close con: {}", networkChannelReference);
        final CloseFutureChannel cfc;
        try {
          cfc = new CloseFutureChannel(networkChannelReference);
          Configuration.configuration.getTimerClose().newTimeout(cfc,
                                                                 Configuration.configuration.getTimeoutCon() *
                                                                 2,
                                                                 TimeUnit.MILLISECONDS);
        } catch (final OpenR66RunnerErrorException ignored) {
          // nothing
        } catch (final IllegalStateException ignored) {
          // nothing
        }
      } else {
        networkChannelReference.use();
        logger.debug("Ignore shutdown after checking");
      }
      logger.debug("NC left: {}", networkChannelReference);
      return count;
    } finally {
      networkChannelReference.lock.unlock();
    }
  }

  /**
   * @param address
   * @param host
   *
   * @return a number > 0 if a connection is still active on this socket or
   *     for
   *     this host
   */
  public static int nbAttachedConnection(final SocketAddress address,
                                         final String host) {
    if (logger.isDebugEnabled()) {
      logger.debug("nbAttachedConnection: {}:{}",
                   networkChannelOnSocketAddressConcurrentHashMap.containsKey(
                       address.hashCode()), getNumberClients(host));
    }
    return (networkChannelOnSocketAddressConcurrentHashMap.containsKey(
        address.hashCode())? 1 : 0) + getNumberClients(host);
  }

  /**
   * @param address
   *
   * @return True if this socket Address is currently valid for connection
   */
  private static boolean isAddressValid(final SocketAddress address) {
    if (WaarpShutdownHook.isShutdownStarting()) {
      logger.debug("IS IN SHUTDOWN");
      return false;
    }
    if (address == null) {
      logger.debug("ADDRESS IS NULL");
      return false;
    }
    try {
      final NetworkChannelReference networkChannelReference =
          getRemoteChannel(address);
      logger.debug("IS IN SHUTDOWN: {}",
                   networkChannelReference.isShuttingDown);
      return !networkChannelReference.isShuttingDown;
    } catch (final OpenR66ProtocolRemoteShutdownException e) {
      logger.debug("ALREADY IN SHUTDOWN");
      return false;
    } catch (final OpenR66ProtocolNoDataException e) {
      logger.debug("NOT FOUND SO NO SHUTDOWN");
      return true;
    } catch (final OpenR66ProtocolBlackListedException e) {
      logger.warn("BLACK LISTED HOST");
      return false;
    }
  }

  /**
   * Returns the NetworkChannelReference if it exists associated with this
   * address
   *
   * @param address
   *
   * @return NetworkChannelReference
   *
   * @throws OpenR66ProtocolRemoteShutdownException
   * @throws OpenR66ProtocolNoDataException
   * @throws OpenR66ProtocolBlackListedException
   */
  private static NetworkChannelReference getRemoteChannel(
      final SocketAddress address)
      throws OpenR66ProtocolRemoteShutdownException,
             OpenR66ProtocolNoDataException,
             OpenR66ProtocolBlackListedException {
    if (WaarpShutdownHook.isShutdownStarting()) {
      logger.debug("IS IN SHUTDOWN");
      throw new OpenR66ProtocolRemoteShutdownException(
          "Local Host already in shutdown");
    }
    if (address == null) {
      logger.debug("ADDRESS IS NULL");
      throw new OpenR66ProtocolRemoteShutdownException(
          "Cannot connect to remote server since address is not specified");
    }
    NetworkChannelReference nc = getShutdownNCR(address);
    if (nc != null) {
      logger.debug("HOST STILL IN SHUTDOWN STATUS: {}", address);
      throw new OpenR66ProtocolRemoteShutdownException(
          "Remote Host already in shutdown");
    }
    nc = getBlacklistNCR(address);
    if (nc != null) {
      logger.debug("HOST IN BLACKLISTED STATUS: {}", address);
      throw new OpenR66ProtocolBlackListedException(
          "Remote Host is blacklisted");
    }
    nc = getNCR(address);
    if (nc != null && (nc.isShuttingDown || !nc.channel().isActive())) {
      logger.debug("HOST IS DisActive: {}", address);
      throw new OpenR66ProtocolRemoteShutdownException(
          "Remote Host is disActive");
    }
    if (nc == null) {
      throw new OpenR66ProtocolNoDataException("Channel not found");
    }
    return nc;
  }

  /**
   * @param channel
   *
   * @return the associated NetworkChannelReference immediately (if known)
   */
  public static NetworkChannelReference getImmediateNetworkChannel(
      final Channel channel) {
    if (channel.remoteAddress() != null) {
      return getNCR(channel.remoteAddress());
    }
    return null;
  }

  /**
   * Remover of Shutdown Remote Host
   */
  private static class R66ShutdownNetworkChannelTimerTask implements TimerTask {
    private static final Set<ChannelId> inShutdownRunning =
        ConcurrentUtility.newConcurrentSet();
    private static final Set<ChannelId> inBlacklistedRunning =
        ConcurrentUtility.newConcurrentSet();
    /**
     * NCR to remove
     */
    private final NetworkChannelReference ncr;
    private final boolean isBlacklisted;

    /**
     * Constructor from type
     *
     * @throws OpenR66RunnerErrorException
     */
    private R66ShutdownNetworkChannelTimerTask(
        final NetworkChannelReference ncr, final boolean blackListed)
        throws OpenR66RunnerErrorException {
      if (blackListed) {
        if (!inBlacklistedRunning.add(ncr.channel.id())) {
          throw new OpenR66RunnerErrorException("Already scheduled");
        }
      } else {
        if (ncr.channel != null && !inShutdownRunning.add(ncr.channel.id())) {
          throw new OpenR66RunnerErrorException("Already scheduled");
        }
      }
      this.ncr = ncr;
      isBlacklisted = blackListed;
    }

    @Override
    public void run(final Timeout timeout) {
      if (isBlacklisted) {
        logger.debug("Will remove Blacklisted for : {}", ncr);
        removeBlacklistNCR(ncr);
        inBlacklistedRunning.remove(ncr.channel.id());
        return;
      }
      logger.debug("Will remove Shutdown for : {}", ncr);
      if (ncr.channel != null && ncr.channel.isActive()) {
        WaarpSslUtility.closingSslChannel(ncr.channel);
      }
      removeShutdownNCR(ncr);
      if (ncr.channel != null) {
        inShutdownRunning.remove(ncr.channel.id());
      }
    }
  }

  /**
   * @return the current number of active RetrieveRunner
   */
  public static int getRetrieveRunnerActive() {
    return Configuration.configuration.getRetrieveRunnerGroup()
                                      .getActiveCount();
  }

  /**
   * Start retrieve operation
   *
   * @param session
   */
  public static void runRetrieve(final R66Session session) {
    final RetrieveRunner retrieveRunner = new RetrieveRunner(session);
    retrieveRunnerConcurrentHashMap.put(
        session.getLocalChannelReference().getLocalId(), retrieveRunner);
    Configuration.configuration.getRetrieveRunnerGroup()
                               .execute(retrieveRunner);
  }

  /**
   * Stop a retrieve operation
   *
   * @param localChannelReference
   */
  public static void stopRetrieve(
      final LocalChannelReference localChannelReference) {
    final RetrieveRunner retrieveRunner =
        retrieveRunnerConcurrentHashMap.remove(
            localChannelReference.getLocalId());
    if (retrieveRunner != null) {
      retrieveRunner.stopRunner();
    }
  }

  /**
   * Normal end of a Retrieve Operation
   *
   * @param localChannelReference
   */
  public static void normalEndRetrieve(
      final LocalChannelReference localChannelReference) {
    retrieveRunnerConcurrentHashMap.remove(localChannelReference.getLocalId());
  }

  /**
   * Close all Network Ttransaction
   */
  public final void closeAll() {
    closeAll(true);
  }

  /**
   * Close all Network Ttransaction
   */
  public final void closeAll(final boolean quickShutdown) {
    logger.debug("close All Network Channels");
    if (!Configuration.configuration.isServer()) {
      if (shutdownHook != null) {
        shutdownHook.launchFinalExit();
      }
    }
    if (networkChannelGroup != null) {
      WaarpNettyUtil.awaitOrInterrupted(networkChannelGroup.close());
      networkChannelGroup = null;
    }
    try {
      Thread.sleep(Configuration.WAITFORNETOP);
    } catch (final InterruptedException e) {//NOSONAR
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    }
    stopAllEndRetrieve();
    DbAdmin.closeAllConnection();
    Configuration.configuration.clientStop(quickShutdown);
    if (!Configuration.configuration.isServer()) {
      logger.debug("Last action before exit");
      WaarpSystemUtil.stopLogger(false);
    }
  }


  public static void stopAllEndRetrieve() {
    final Enumeration<RetrieveRunner> enumeration =
        retrieveRunnerConcurrentHashMap.elements();
    while (enumeration.hasMoreElements()) {
      final RetrieveRunner runner = enumeration.nextElement();
      runner.stopRunner();
    }
    retrieveRunnerConcurrentHashMap.clear();
  }
}
