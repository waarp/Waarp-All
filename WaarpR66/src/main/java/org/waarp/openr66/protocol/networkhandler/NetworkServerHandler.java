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
package org.waarp.openr66.protocol.networkhandler;

import java.net.BindException;
import java.net.SocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.ReadTimeoutException;

import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66Exception;
import org.waarp.openr66.protocol.exception.OpenR66ExceptionTrappedFactory;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessNoWriteBackException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolRemoteShutdownException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.ConnectionErrorPacket;
import org.waarp.openr66.protocol.localhandler.packet.KeepAlivePacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketCodec;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.networkhandler.packet.NetworkPacket;
import org.waarp.openr66.protocol.utils.ChannelCloseTimer;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66ShutdownHook;

/**
 * Network Server Handler (Requester side)
 * 
 * @author frederic bregier
 */
public class NetworkServerHandler extends SimpleChannelInboundHandler<NetworkPacket> {
    // extends SimpleChannelHandler {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory.getLogger(NetworkServerHandler.class);

    /**
     * The associated Remote Address
     */
    private SocketAddress remoteAddress;
    /**
     * The associated NetworkChannelReference
     */
    private NetworkChannelReference networkChannelReference;
    /**
     * The Database connection attached to this NetworkChannelReference shared among all associated
     * LocalChannels
     */
    private DbSession dbSession;
    /**
     * Does this Handler is for SSL
     */
    protected boolean isSSL = false;
    /**
     * Is this Handler a server side
     */
    protected boolean isServer = false;
    /**
     * To handle the keep alive
     */
    private volatile int keepAlivedSent = 0;
    /**
     * Is this network connection being refused (black listed)
     */
    protected volatile boolean isBlackListed = false;

    /**
     * 
     * @param isServer
     */
    public NetworkServerHandler(boolean isServer) {
        this.isServer = isServer;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (networkChannelReference != null) {
            if (networkChannelReference.nbLocalChannels() > 0) {
                logger.info("Network Channel Closed: {} LocalChannels Left: {}",
                        ctx.channel().id(),
                        networkChannelReference.nbLocalChannels());
                // Give an extra time if necessary to let the local channel being closed
                try {
                    Thread.sleep(Configuration.RETRYINMS * 2);
                } catch (InterruptedException e1) {
                }
            }
            NetworkTransaction.closedNetworkChannel(networkChannelReference);
        } else {
            if (remoteAddress == null) {
                remoteAddress = ctx.channel().remoteAddress();
            }
            NetworkTransaction.closedNetworkChannel(remoteAddress);
        }
        // Now force the close of the database after a wait
        if (dbSession != null && DbConstant.admin != null && DbConstant.admin.getSession() != null
                && !dbSession.equals(DbConstant.admin.getSession())) {
            dbSession.forceDisconnect();
            dbSession = null;
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel netChannel = ctx.channel();
        this.remoteAddress = netChannel.remoteAddress();
        logger.debug("Will the Connection be refused if Partner is BlackListed from " + remoteAddress.toString());
        if (NetworkTransaction.isBlacklisted(netChannel)) {
            logger.warn("Connection refused since Partner is BlackListed from " + remoteAddress.toString());
            isBlackListed = true;
            if (Configuration.configuration.getR66Mib() != null) {
                Configuration.configuration.getR66Mib().notifyError(
                        "Black Listed connection temptative", "During connection");
            }
            // close immediately the connection
            WaarpSslUtility.closingSslChannel(netChannel);
            return;
        }
        try {
            this.networkChannelReference = NetworkTransaction.addNetworkChannel(netChannel);
        } catch (OpenR66ProtocolRemoteShutdownException e2) {
            logger.warn("Connection refused since Partner is in Shutdown from " + remoteAddress.toString() + " : {}",
                    e2.getMessage());
            isBlackListed = true;
            // close immediately the connection
            WaarpSslUtility.closingSslChannel(netChannel);
            return;
        }
        try {
            if (DbConstant.admin.isActive()) {
                if (DbConstant.admin.isCompatibleWithThreadSharedConnexion()) {
                    this.dbSession = new DbSession(DbConstant.admin, false);
                    this.dbSession.useConnection();
                } else {
                    logger.debug("DbSession will be adjusted on LocalChannelReference");
                    this.dbSession = DbConstant.admin.getSession();
                }
            }
        } catch (WaarpDatabaseNoConnectionException e1) {
            // Cannot connect so use default connection
            logger.warn("Use default database connection");
            this.dbSession = DbConstant.admin.getSession();
        }
        logger.debug("Network Channel Connected: {} ", ctx.channel().id());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (Configuration.configuration.isShutdown())
            return;
        if (evt instanceof IdleStateEvent) {
            if (this.networkChannelReference != null
                    && this.networkChannelReference.checkLastTime(Configuration.configuration.getTIMEOUTCON() * 2) <= 0) {
                keepAlivedSent = 0;
                return;
            }
            if (keepAlivedSent > 0) {
                if (this.networkChannelReference != null) {
                    if (this.networkChannelReference.nbLocalChannels() > 0 && keepAlivedSent < 5) {
                        // ignore this time
                        keepAlivedSent++;
                        return;
                    }
                }
                logger.error("Not getting KAlive: closing channel");
                if (Configuration.configuration.getR66Mib() != null) {
                    Configuration.configuration.getR66Mib().notifyWarning(
                            "KeepAlive get no answer", "Closing network connection");
                }
                ChannelCloseTimer.closeFutureChannel(ctx.channel());
            } else {
                keepAlivedSent = 1;
                KeepAlivePacket keepAlivePacket = new KeepAlivePacket();
                NetworkPacket response =
                        new NetworkPacket(ChannelUtils.NOCHANNEL,
                                ChannelUtils.NOCHANNEL, keepAlivePacket, null);
                logger.info("Write KAlive");
                ctx.channel().writeAndFlush(response);
            }
        }
    }

    public void setKeepAlivedSent() {
        keepAlivedSent = 0;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, NetworkPacket msg) throws Exception {
        if (isBlackListed) {
            // ignore message since close on going
            return;
        }
        final NetworkPacket packet = msg;
        Channel channel = ctx.channel();
        if (packet.getCode() == LocalPacketFactory.NOOPPACKET) {
            if (networkChannelReference != null) {
                networkChannelReference.useIfUsed();
            }
            packet.clear();
            // Do nothing
            return;
        } else if (packet.getCode() == LocalPacketFactory.CONNECTERRORPACKET) {
            logger.debug("NetworkRecv: {}", packet);
            // Special code to STOP here
            if (packet.getLocalId() == ChannelUtils.NOCHANNEL) {
                int nb = this.networkChannelReference.nbLocalChannels();
                if (nb > 0) {
                    logger.warn("Temptative of connection failed but still some connection are there so not closing the server channel immediately: "
                            + nb);
                    NetworkTransaction.shuttingDownNetworkChannel(networkChannelReference);
                    return;
                }
                // No way to know what is wrong: close all connections with
                // remote host
                logger.error("Will close NETWORK channel, Cannot continue connection with remote Host: "
                        +
                        packet.toString() +
                        " : " +
                        channel.remoteAddress() + " : " + nb);
                WaarpSslUtility.closingSslChannel(channel);
                packet.clear();
                return;
            }
        } else if (packet.getCode() == LocalPacketFactory.KEEPALIVEPACKET) {
            if (networkChannelReference != null) {
                networkChannelReference.useIfUsed();
            }
            keepAlivedSent = 0;
            try {
                KeepAlivePacket keepAlivePacket = (KeepAlivePacket)
                        LocalPacketCodec.decodeNetworkPacket(packet.getBuffer());
                if (keepAlivePacket.isToValidate()) {
                    keepAlivePacket.validate();
                    NetworkPacket response =
                            new NetworkPacket(ChannelUtils.NOCHANNEL,
                                    ChannelUtils.NOCHANNEL, keepAlivePacket, null);
                    logger.info("Answer KAlive");
                    ctx.channel().writeAndFlush(response);
                } else {
                    logger.info("Get KAlive");
                }
            } catch (OpenR66ProtocolPacketException e1) {
            }
            packet.clear();
            return;
        }
        logger.trace("GET MSG: {}", packet);
        this.networkChannelReference.use();
        LocalChannelReference localChannelReference = null;
        if (packet.getLocalId() == ChannelUtils.NOCHANNEL) {
            logger.debug("NetworkRecv Create: {} {}", packet,
                    channel.id());
            NetworkTransaction.createConnectionFromNetworkChannelStartup(
                    this.networkChannelReference, packet, isSSL);
            return;
        } else {
            if (packet.getCode() == LocalPacketFactory.ENDREQUESTPACKET) {
                // Not a local error but a remote one
                try {
                    localChannelReference = Configuration.configuration
                            .getLocalTransaction().getClient(packet.getRemoteId(),
                                    packet.getLocalId());
                } catch (OpenR66ProtocolSystemException e1) {
                    // do not send anything since the packet is external
                    try {
                        logger.debug("Cannot get LocalChannel while an end of request comes: {}",
                                LocalPacketCodec.decodeNetworkPacket(packet.getBuffer()));
                    } catch (OpenR66ProtocolPacketException e2) {
                        logger.debug("Cannot get LocalChannel while an end of request comes: {}",
                                packet.toString());
                    }
                    packet.clear();
                    return;
                }
                // OK continue and send to the local channel
            } else if (packet.getCode() == LocalPacketFactory.CONNECTERRORPACKET) {
                // Not a local error but a remote one
                try {
                    localChannelReference = Configuration.configuration
                            .getLocalTransaction().getClient(packet.getRemoteId(),
                                    packet.getLocalId());
                } catch (OpenR66ProtocolSystemException e1) {
                    // do not send anything since the packet is external
                    try {
                        logger.debug("Cannot get LocalChannel while an external error comes: {}",
                                LocalPacketCodec.decodeNetworkPacket(packet.getBuffer()));
                    } catch (OpenR66ProtocolPacketException e2) {
                        logger.debug("Cannot get LocalChannel while an external error comes: {}",
                                packet.toString());
                    }
                    packet.clear();
                    return;
                }
                // OK continue and send to the local channel
            } else {
                try {
                    localChannelReference = Configuration.configuration
                            .getLocalTransaction().getClient(packet.getRemoteId(),
                                    packet.getLocalId());
                } catch (OpenR66ProtocolSystemException e1) {
                    if (remoteAddress == null) {
                        remoteAddress = channel.remoteAddress();
                    }
                    if (NetworkTransaction.isShuttingdownNetworkChannel(remoteAddress)
                            || R66ShutdownHook.isShutdownStarting()) {
                        // ignore
                        packet.clear();
                        return;
                    }
                    packet.clear();
                    return;
                }
            }
        }
        // check if not already in shutdown or closed
        if (NetworkTransaction.isShuttingdownNetworkChannel(remoteAddress)
                || R66ShutdownHook.isShutdownStarting() ||
                ! localChannelReference.getLocalChannel().isActive()) {
            logger.debug("Cannot use LocalChannel since already in shutdown: " + packet);
            // ignore
            packet.clear();
            return;
        }
        ByteBuf buf = packet.getBuffer();
        localChannelReference.getLocalChannel().writeAndFlush(buf);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Channel channel = ctx.channel();
        if (isBlackListed) {
            logger.info("While partner is blacklisted, Network Channel Exception: {}", channel.id(), cause.getClass()
                    .getName() + " : " + cause);
            // ignore
            return;
        }
        logger.debug("Network Channel Exception: {}", channel.id(), cause);
        if (cause instanceof ReadTimeoutException) {
            ReadTimeoutException exception = (ReadTimeoutException) cause;
            // No read for too long
            logger.error("ReadTimeout so Will close NETWORK channel {}", exception.getClass().getName() + " : "
                    + exception.getMessage());
            ChannelCloseTimer.closeFutureChannel(channel);
            return;
        }
        if (cause instanceof BindException) {
            // received when not yet connected
            logger.debug("BindException");
            ChannelCloseTimer.closeFutureChannel(channel);
            return;
        }
        OpenR66Exception exception = OpenR66ExceptionTrappedFactory
                .getExceptionFromTrappedException(channel, cause);
        if (exception != null) {
            if (exception instanceof OpenR66ProtocolBusinessNoWriteBackException) {
                if (this.networkChannelReference != null && this.networkChannelReference.nbLocalChannels() > 0) {
                    logger.debug(
                            "Network Channel Exception: {} {}", channel.id(),
                            exception.getClass().getName() + " : " + exception.getMessage());
                }
                logger.debug("Will close NETWORK channel");
                ChannelCloseTimer.closeFutureChannel(channel);
                return;
            } else if (exception instanceof OpenR66ProtocolNoConnectionException) {
                logger.debug("Connection impossible with NETWORK channel {}",
                        exception.getClass().getName() + " : " + exception.getMessage());
                channel.close();
                return;
            } else {
                logger.debug(
                        "Network Channel Exception: {} {}", channel.id(),
                        exception.getClass().getName() + " : " + exception.getMessage());
            }
            final ConnectionErrorPacket errorPacket = new ConnectionErrorPacket(
                    exception.getClass().getName() + " : " + exception.getMessage(), null);
            writeError(channel, ChannelUtils.NOCHANNEL,
                    ChannelUtils.NOCHANNEL, errorPacket);
            logger.debug("Will close NETWORK channel: {}",
                    exception.getClass().getName() + " : " + exception.getMessage());
            ChannelCloseTimer.closeFutureChannel(channel);
        } else {
            // Nothing to do
            return;
        }
    }

    /**
     * Write error back to remote client
     * 
     * @param channel
     * @param remoteId
     * @param localId
     * @param error
     */
    public static void writeError(Channel channel, Integer remoteId, Integer localId, AbstractLocalPacket error) {
        NetworkPacket networkPacket = null;
        try {
            networkPacket = new NetworkPacket(localId, remoteId, error, null);
        } catch (OpenR66ProtocolPacketException e) {
        }
        try {
            if (channel.isActive()) {
                channel.writeAndFlush(networkPacket).await(Configuration.WAITFORNETOP);
            }
        } catch (InterruptedException e) {
        }
    }

    /**
     * @return the dbSession
     */
    public DbSession getDbSession() {
        return dbSession;
    }

    /**
     * 
     * @return True if this Handler is for SSL
     */
    public boolean isSsl() {
        return isSSL;
    }
}
