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
 * You should have received a copy of the GNU General Public License along with Waarp. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.protocol.localhandler;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolRemoteShutdownException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolShutdownException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.packet.ConnectionErrorPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.StartupPacket;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkChannelReference;
import org.waarp.openr66.protocol.networkhandler.NetworkServerHandler;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.networkhandler.packet.NetworkPacket;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.protocol.utils.R66ShutdownHook;

/**
 * This class handles Local Transaction connections
 * 
 * @author frederic bregier
 */
public class LocalTransaction {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory.getLogger(LocalTransaction.class);

    /**
     * HashMap of LocalChannelReference using LocalChannelId
     */
    private final ConcurrentHashMap<Integer, LocalChannelReference> localChannelHashMap = new ConcurrentHashMap<Integer, LocalChannelReference>();

    /**
     * HashMap of LocalChannelReference using requested_requester_specialId
     */
    private final ConcurrentHashMap<String, LocalChannelReference> localChannelHashMapIdBased = new ConcurrentHashMap<String, LocalChannelReference>();

    private final ServerBootstrap serverBootstrap = new ServerBootstrap();

    private final Channel serverChannel;

    private final LocalAddress socketLocalServerAddress = new LocalAddress("0");

    private final Bootstrap clientBootstrap = new Bootstrap();

    private final ChannelGroup localChannelGroup = new DefaultChannelGroup("LocalChannels", Configuration.configuration
            .getSubTaskGroup().next());

    /**
     * Constructor
     */
    public LocalTransaction() {
        serverBootstrap.channel(LocalServerChannel.class);
        serverBootstrap.group(Configuration.configuration.getLocalBossGroup(),
                Configuration.configuration.getLocalWorkerGroup());
        serverBootstrap.childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Configuration.configuration.getTIMEOUTCON());
        serverBootstrap.childHandler(new LocalServerInitializer());
        try {
            serverChannel = serverBootstrap.bind(socketLocalServerAddress).sync().channel();
        } catch (InterruptedException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
        localChannelGroup.add(serverChannel);

        clientBootstrap.channel(LocalChannel.class);
        // Same Group than Network final handler 
        clientBootstrap.group(Configuration.configuration.getLocalWorkerGroup());
        clientBootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Configuration.configuration.getTIMEOUTCON());
        clientBootstrap.handler(new LocalClientInitializer());
    }

    public String hashStatus() {
        return "LocalTransaction: [localChannelHashMap: " + localChannelHashMap.size()
                + " localChannelHashMapIdBased: " + localChannelHashMapIdBased.size() + "] ";
    }

    /**
     * Get the corresponding LocalChannelReference and set the remoteId if different
     * 
     * @param remoteId
     * @param localId
     * @return the LocalChannelReference
     * @throws OpenR66ProtocolSystemException
     */
    public LocalChannelReference getClient(Integer remoteId, Integer localId)
            throws OpenR66ProtocolSystemException {
        LocalChannelReference localChannelReference = getFromId(localId);
        if (localChannelReference != null) {
            if (localChannelReference.getRemoteId().compareTo(remoteId) != 0) {
                localChannelReference.setRemoteId(remoteId);
            }
            return localChannelReference;
        }
        throw new OpenR66ProtocolSystemException("Cannot find LocalChannelReference");
    }

    private static class SendLater extends Thread {
        private static Map<Integer, SendLater> sendLaters = new ConcurrentHashMap<Integer, SendLater>();
        final LocalTransaction lt;
        final Channel networkChannel;
        final SocketAddress remoteAddress;
        Integer remoteId;
        final Integer localId;
        final Queue<NetworkPacket> packets = new ConcurrentLinkedQueue<NetworkPacket>();
        int step = 0;
        
        private SendLater(LocalTransaction lt, Channel nc, Integer remoteId, Integer localId) {
            this.lt = lt;
            this.networkChannel = nc;
            this.remoteId = remoteId;
            this.localId = localId;
            remoteAddress = networkChannel.remoteAddress();
        }
        
        private void add(NetworkPacket packet) {
            if (packets.isEmpty()) {
                packets.add(packet);
                Configuration.configuration.launchInFixedDelay(this, Configuration.WAITFORNETOP, TimeUnit.MILLISECONDS);
            }
            packets.add(packet);
        }

        public void run() {
            synchronized (sendLaters) {
                LocalChannelReference localChannelReference = lt.localChannelHashMap.get(localId);
                if (localChannelReference != null) {
                    if (localChannelReference.getRemoteId().compareTo(remoteId) != 0) {
                        localChannelReference.setRemoteId(remoteId);
                    }
                    NetworkPacket networkPacket = packets.poll();
                    LocalChannel localChannel = localChannelReference.getLocalChannel();
                    while (networkPacket != null) {
                        localChannel.write(networkPacket.getBuffer());
                        networkPacket = packets.poll();
                    }
                    localChannel.flush();
                    sendLaters.remove(localId);
                } else {
                    step ++;
                    if (step > 10000) {
                        if (NetworkTransaction.isShuttingdownNetworkChannel(remoteAddress)
                                || R66ShutdownHook.isShutdownStarting()) {
                            // ignore
                            sendLaters.remove(localId);
                            packets.clear();
                            return;
                        }
                        logger.warn("Cannot get LocalChannel: due to LocalId not found: " + localId);
                        final ConnectionErrorPacket error = new ConnectionErrorPacket(
                                "Cannot get localChannel since localId is not found anymore", "" + localId);
                        NetworkServerHandler.writeError(networkChannel, remoteId, localId, error);
                        sendLaters.remove(localId);
                        packets.clear();
                    } else {
                        Configuration.configuration.launchInFixedDelay(this, Configuration.WAITFORNETOP, TimeUnit.MILLISECONDS);
                    }
                }
            }
        }
    }
    /**
     * Get the corresponding LocalChannelReference and set the remoteId if different
     * 
     * @param remoteId
     * @param localId
     * @throws OpenR66ProtocolSystemException
     */
    public void sendLaterToClient(Channel networkChannel, Integer remoteId, Integer localId, NetworkPacket packet) {
        synchronized (SendLater.sendLaters) {
            SendLater sendLater = SendLater.sendLaters.get(localId);
            if (sendLater == null) {
                sendLater = new SendLater(this, networkChannel, remoteId, localId);
                SendLater.sendLaters.put(localId, sendLater);
                sendLater.setDaemon(true);
            }
            sendLater.remoteId = remoteId;
            sendLater.add(packet);
        }
    }

    /**
     * Create a new Client
     * 
     * @param networkChannelReference
     * @param remoteId
     *            might be set to ChannelUtils.NOCHANNEL (real creation)
     * @param futureRequest
     *            might be null (from NetworkChannel Startup)
     * @return the LocalChannelReference
     * @throws OpenR66ProtocolSystemException
     * @throws OpenR66ProtocolRemoteShutdownException
     * @throws OpenR66ProtocolNoConnectionException
     */
    public LocalChannelReference createNewClient(NetworkChannelReference networkChannelReference,
            Integer remoteId, R66Future futureRequest, boolean fromSsl)
            throws OpenR66ProtocolSystemException, OpenR66ProtocolRemoteShutdownException,
            OpenR66ProtocolNoConnectionException {
        ChannelFuture channelFuture = null;
        logger.debug("Status LocalChannelServer: {} {}", serverChannel
                .getClass().getName(), serverChannel.config()
                .getConnectTimeoutMillis() + " " + serverChannel.isOpen());
        for (int i = 0; i < Configuration.RETRYNB; i++) {
            if (R66ShutdownHook.isShutdownStarting()) {
                // Do not try since already locally in shutdown
                throw new OpenR66ProtocolNoConnectionException(
                        "Cannot connect to local handler: " + socketLocalServerAddress +
                                " " + serverChannel.isOpen() + " " + serverChannel +
                                " since the local server is in shutdown.");
            }
            channelFuture = clientBootstrap.connect(socketLocalServerAddress);
            try {
                channelFuture.await();
                //channelFuture.await(Configuration.configuration.TIMEOUTCON/3);
            } catch (InterruptedException e1) {
                logger.error("LocalChannelServer Interrupted: " +
                        serverChannel.getClass().getName() + " " +
                        serverChannel.config().getConnectTimeoutMillis() +
                        " " + serverChannel.isOpen());
                throw new OpenR66ProtocolSystemException(
                        "Interruption - Cannot connect to local handler: " +
                                socketLocalServerAddress + " " +
                                serverChannel.isOpen() + " " + serverChannel,
                        e1);
            }
            if (channelFuture.isSuccess()) {
                final LocalChannel channel = (LocalChannel) channelFuture.channel();
                localChannelGroup.add(channel);
                logger.debug("Will start localChannelReference and eventually generate a new Db Connection if not-thread-safe");
                final LocalChannelReference localChannelReference = new LocalChannelReference(
                        channel, networkChannelReference, remoteId, futureRequest);
                localChannelHashMap.put(channel.id().hashCode(), localChannelReference);
                logger.debug("Db connection done and Create LocalChannel entry: " + i + " {}",
                        localChannelReference);
                logger.info("Add one localChannel to a Network Channel: " + channel.id());
                // Now send first a Startup message
                StartupPacket startup = new StartupPacket(localChannelReference.getLocalId(), fromSsl);
                channel.writeAndFlush(startup);
                return localChannelReference;
            } else {
                logger.error("Can't connect to local server " + i + " (Done: " + channelFuture.isDone() + ")");
            }
            try {
                Thread.sleep(Configuration.RETRYINMS * 10);
            } catch (InterruptedException e) {
                throw new OpenR66ProtocolSystemException(
                        "Cannot connect to local handler", e);
            }
        }
        logger.error("LocalChannelServer: " +
                serverChannel.getClass().getName() + " " +
                serverChannel.config().getConnectTimeoutMillis() + " " +
                serverChannel.isOpen());
        throw new OpenR66ProtocolSystemException(
                "Cannot connect to local handler: " + socketLocalServerAddress +
                        " " + serverChannel.isOpen() + " " + serverChannel,
                channelFuture.cause());
    }

    /**
     * 
     * @param id
     * @return the LocalChannelReference
     */
    public LocalChannelReference getFromId(Integer id) {
        return localChannelHashMap.get(id);
    }

    /**
     * Remove one local channel
     * 
     * @param localChannelReference
     */
    protected void remove(LocalChannelReference localChannelReference) {
        logger.debug("DEBUG remove: " + localChannelReference.getLocalId());
        localChannelHashMap.remove(localChannelReference.getLocalId());
        if (localChannelReference.getRequestId() != null) {
            localChannelHashMapIdBased.remove(localChannelReference.getRequestId());
        }
    }

    /**
     * 
     * @param runner
     * @param lcr
     */
    public void setFromId(DbTaskRunner runner, LocalChannelReference lcr) {
        String key = runner.getKey();
        lcr.setRequestId(key);
        localChannelHashMapIdBased.put(key, lcr);
    }

    /**
     * 
     * @param key
     *            as "requested requester specialId"
     * @return the LocalChannelReference
     */
    public LocalChannelReference getFromRequest(String key) {
        return localChannelHashMapIdBased.get(key);
    }

    /**
     * 
     * @param key as "requested requester specialId"
     * @return True if the LocalChannelReference exists
     */
    public boolean contained(String key) {
        return localChannelHashMapIdBased.containsKey(key);
    }
    /**
     * 
     * @return the number of active local channels
     */
    public int getNumberLocalChannel() {
        return localChannelHashMap.size();
    }

    /**
     * Debug function (while shutdown for instance)
     */
    public void debugPrintActiveLocalChannels() {
        Collection<LocalChannelReference> collection = localChannelHashMap.values();
        Iterator<LocalChannelReference> iterator = collection.iterator();
        while (iterator.hasNext()) {
            LocalChannelReference localChannelReference = iterator.next();
            logger.debug("Will close local channel: {}", localChannelReference);
            logger.debug(
                    " Containing: {}",
                    (localChannelReference.getSession() != null ? localChannelReference
                            .getSession() : "no session"));
        }
    }

    /**
     * Informs all remote client that the server is shutting down
     */
    public void shutdownLocalChannels() {
        logger.warn("Will inform LocalChannels of Shutdown: " + localChannelHashMap.size());
        Collection<LocalChannelReference> collection = localChannelHashMap.values();
        Iterator<LocalChannelReference> iterator = collection.iterator();
        ValidPacket packet = new ValidPacket("Shutdown forced", null,
                LocalPacketFactory.SHUTDOWNPACKET);
        ByteBuf buffer = null;
        while (iterator.hasNext()) {
            LocalChannelReference localChannelReference = iterator.next();
            logger.info("Inform Shutdown {}", localChannelReference);
            packet.setSmiddle(null);
            packet.retain();
            // If a transfer is running, save the current rank and inform remote
            // host
            if (localChannelReference.getSession() != null) {
                R66Session session = localChannelReference.getSession();
                DbTaskRunner runner = session.getRunner();
                if (runner != null && runner.isInTransfer()) {
                    if (!runner.isSender()) {
                        int newrank = runner.getRank();
                        packet.setSmiddle(Integer.toString(newrank));
                    }
                    // Save File status
                    try {
                        runner.saveStatus();
                    } catch (OpenR66RunnerErrorException e) {
                    }
                }
                if (runner != null && !runner.isFinished()) {
                    R66Result result = new R66Result(
                            new OpenR66ProtocolShutdownException(), session,
                            true, ErrorCode.Shutdown, runner);
                    result.setOther(packet);
                    try {
                        buffer = packet.getLocalPacket(localChannelReference);
                    } catch (OpenR66ProtocolPacketException e1) {
                    }
                    localChannelReference.sessionNewState(R66FiniteDualStates.SHUTDOWN);
                    NetworkPacket message = new NetworkPacket(
                            localChannelReference.getLocalId(),
                            localChannelReference.getRemoteId(),
                            packet.getType(), buffer);
                    try {
                        localChannelReference.getNetworkChannel().writeAndFlush(message)
                                .await(Configuration.WAITFORNETOP);
                    } catch (InterruptedException e1) {
                    }
                    try {
                        session.setFinalizeTransfer(false, result);
                    } catch (OpenR66RunnerErrorException e) {
                    } catch (OpenR66ProtocolSystemException e) {
                    }
                }
                localChannelReference.getLocalChannel().close();
                continue;
            }
            try {
                buffer = packet.getLocalPacket(localChannelReference);
            } catch (OpenR66ProtocolPacketException e1) {
            }
            NetworkPacket message = new NetworkPacket(
                    localChannelReference.getLocalId(),
                    localChannelReference.getRemoteId(), packet.getType(),
                    buffer);
            localChannelReference.getNetworkChannel().writeAndFlush(message);
        }
    }

    /**
     * Close All Local Channels
     */
    public void closeAll() {
        logger.debug("close All Local Channels");
        localChannelGroup.close().awaitUninterruptibly();
    }

}
