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
package org.waarp.openr66.protocol.utils;

import ch.qos.logback.classic.LoggerContext;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.ChannelGroupFutureListener;
import io.netty.channel.local.LocalChannel;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import io.netty.handler.traffic.TrafficCounter;
import org.slf4j.LoggerFactory;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.file.DataBlock;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.task.localexec.LocalExecClient;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.http.restv2.RestServiceInitializer;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.DataPacket;
import org.waarp.openr66.protocol.localhandler.packet.EndTransferPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.networkhandler.packet.NetworkPacket;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Channel Utils
 * 
 * @author Frederic Bregier
 */
public class ChannelUtils extends Thread {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(ChannelUtils.class);

    public static final Integer NOCHANNEL = Integer.MIN_VALUE;

    /**
     * Get the Remote InetAddress
     * 
     * @param channel
     * @return the remote InetAddress
     */
    public final static InetAddress getRemoteInetAddress(Channel channel) {
        InetSocketAddress socketAddress = (InetSocketAddress) channel.remoteAddress();
        if (socketAddress == null) {
            socketAddress = new InetSocketAddress(20);
        }
        return socketAddress.getAddress();
    }

    /**
     * Get the Local InetAddress
     * 
     * @param channel
     * @return the local InetAddress
     */
    public final static InetAddress getLocalInetAddress(Channel channel) {
        final InetSocketAddress socketAddress = (InetSocketAddress) channel.localAddress();
        return socketAddress.getAddress();
    }

    /**
     * Get the Remote InetSocketAddress
     * 
     * @param channel
     * @return the remote InetSocketAddress
     */
    public final static InetSocketAddress getRemoteInetSocketAddress(Channel channel) {
        return (InetSocketAddress) channel.remoteAddress();
    }

    /**
     * Get the Local InetSocketAddress
     * 
     * @param channel
     * @return the local InetSocketAddress
     */
    public final static InetSocketAddress getLocalInetSocketAddress(Channel channel) {
        return (InetSocketAddress) channel.localAddress();
    }

    /**
     * Finalize resources attached to handlers
     * 
     * @author Frederic Bregier
     */
    private static class R66ChannelGroupFutureListener implements
            ChannelGroupFutureListener {
        String name;
        EventLoopGroup group;

        public R66ChannelGroupFutureListener(String name, EventLoopGroup group) {
            this.name = name;
            this.group = group;
        }

        public void operationComplete(ChannelGroupFuture future)
                throws Exception {
            logger.info("Start with shutdown external resources for " + name);
            if (group != null) {
                group.shutdownGracefully();
            }
            logger.info("Done with shutdown " + name);
        }
    }

    /**
     * Terminate all registered channels
     * 
     * @return the number of previously registered network channels
     */
    private static int terminateCommandChannels() {
        if (Configuration.configuration.getServerChannelGroup() == null) {
            return 0;
        }
        final int result = Configuration.configuration.getServerChannelGroup().size();
        logger.info("ServerChannelGroup: " + result);
        Configuration.configuration.getServerChannelGroup().close()
                .addListener(
                        new R66ChannelGroupFutureListener(
                                "ServerChannelGroup",
                                Configuration.configuration.getHandlerGroup()));
        return result;
    }

    /**
     * Terminate all registered Http channels
     * 
     * @return the number of previously registered http network channels
     */
    private static int terminateHttpChannels() {
        if (Configuration.configuration.getHttpChannelGroup() == null) {
            return 0;
        }
        final int result = Configuration.configuration.getHttpChannelGroup().size();
        logger.debug("HttpChannelGroup: " + result);
        Configuration.configuration.getHttpChannelGroup().close();
        return result;
    }

    /**
     * Return the current number of network connections
     * 
     * @param configuration
     * @return the current number of network connections
     */
    public final static int nbCommandChannels(Configuration configuration) {
        return configuration.getServerChannelGroup().size();
    }

    /**
     * To be used only with LocalChannel (NetworkChannel could be using SSL)
     * 
     * @param channel
     */
    public final static void close(final LocalChannel channel) {
        channel.eventLoop().schedule(new Runnable() {
            public void run() {
                channel.close();
            }
        }, Configuration.WAITFORNETOP, TimeUnit.MILLISECONDS);
    }

    /**
     * 
     * @param localChannelReference
     * @param block
     * @return the ChannelFuture of this write operation
     * @throws OpenR66ProtocolPacketException
     */
    public static ChannelFuture writeBackDataBlock(
            LocalChannelReference localChannelReference, DataBlock block)
            throws OpenR66ProtocolPacketException {
        ByteBuf md5 = Unpooled.EMPTY_BUFFER;
        DbTaskRunner runner = localChannelReference.getSession().getRunner();
        if (RequestPacket.isMD5Mode(runner.getMode())) {
            md5 = FileUtils.getHash(block.getBlock(), Configuration.configuration.getDigest());
        }
        if (runner.getRank() % 100 == 1 || localChannelReference.getSessionState() != R66FiniteDualStates.DATAS) {
            localChannelReference.sessionNewState(R66FiniteDualStates.DATAS);
        }
        logger.trace("sending data block {}", runner.getRank());
        DataPacket data = new DataPacket(runner.getRank(), block.getBlock(), md5);// was block.getBlock().copy()
        ChannelFuture future = writeAbstractLocalPacket(localChannelReference, data, false);
        runner.incrementRank();
        return future;
    }

    /**
     * Write the EndTransfer
     * 
     * @param localChannelReference
     * @throws OpenR66ProtocolPacketException
     */
    public final static void writeEndTransfer(
            LocalChannelReference localChannelReference)
            throws OpenR66ProtocolPacketException {
        EndTransferPacket packet = new EndTransferPacket(LocalPacketFactory.REQUESTPACKET);
        localChannelReference.sessionNewState(R66FiniteDualStates.ENDTRANSFERS);
        writeAbstractLocalPacket(localChannelReference, packet, true);
    }

    /**
     * Write the EndTransfer plus Global Hash
     * 
     * @param localChannelReference
     * @param hash
     * @throws OpenR66ProtocolPacketException
     */
    public final static void writeEndTransfer(
            LocalChannelReference localChannelReference, String hash)
            throws OpenR66ProtocolPacketException {
        EndTransferPacket packet = new EndTransferPacket(
                LocalPacketFactory.REQUESTPACKET, hash);
        localChannelReference.sessionNewState(R66FiniteDualStates.ENDTRANSFERS);
        writeAbstractLocalPacket(localChannelReference, packet, true);
    }

    /**
     * Write an AbstractLocalPacket to the network Channel
     * 
     * @param localChannelReference
     * @param packet
     * @param wait
     * @return the ChannelFuture on write operation
     * @throws OpenR66ProtocolPacketException
     */
    public static ChannelFuture writeAbstractLocalPacket(
            LocalChannelReference localChannelReference, AbstractLocalPacket packet,
            boolean wait)
            throws OpenR66ProtocolPacketException {
        final NetworkPacket networkPacket;
        try {
            networkPacket = new NetworkPacket(localChannelReference
                    .getLocalId(), localChannelReference.getRemoteId(), packet, localChannelReference);
        } catch (OpenR66ProtocolPacketException e) {
            logger.error(Messages.getString("ChannelUtils.6") + packet.toString(), //$NON-NLS-1$
                    e);
            throw e;
        }
        if (wait) {
            ChannelFuture future = localChannelReference.getNetworkChannel().writeAndFlush(networkPacket);
            localChannelReference.getNetworkChannelObject().use();
            try {
                future.await(Configuration.configuration.getTIMEOUTCON());
                return future;
            } catch (InterruptedException e) {
                return future;
            }
        } else {
            return localChannelReference.getNetworkChannel().writeAndFlush(networkPacket);
        }
    }

    /**
     * Write an AbstractLocalPacket to the Local Channel
     * 
     * @param localChannelReference
     * @param packet
     * @return the ChannelFuture on write operation
     * @throws OpenR66ProtocolPacketException
     */
    public final static ChannelFuture writeAbstractLocalPacketToLocal(
            LocalChannelReference localChannelReference, AbstractLocalPacket packet)
            throws OpenR66ProtocolPacketException {
        return localChannelReference.getLocalChannel().writeAndFlush(packet);
    }

    /**
     * Compute Wait for Traffic in Write (ugly turn around)
     * 
     * @param localChannelReference
     * @param size
     * @return the wait in ms
     */
    public static final long willBeWaitingWriting(LocalChannelReference localChannelReference,
            int size) {
        ChannelTrafficShapingHandler cts = localChannelReference.getChannelTrafficShapingHandler();
        return willBeWaitingWriting(cts, size);
    }

    /**
     * Compute Wait for Traffic in Write (ugly turn around)
     * 
     * @param cts
     * @param size
     * @return the wait in ms
     */
    public static final long willBeWaitingWriting(ChannelTrafficShapingHandler cts, int size) {
        long currentTime = System.currentTimeMillis();
        if (cts != null && Configuration.configuration.getServerChannelWriteLimit() > 0) {
            TrafficCounter tc = cts.trafficCounter();
            if (tc != null) {
                long wait = waitTraffic(Configuration.configuration.getServerChannelWriteLimit(),
                        tc.currentWrittenBytes() + size,
                        tc.lastTime(), currentTime);
                if (wait > 0) {
                    return wait;
                }
            }
        }
        if (Configuration.configuration.getServerGlobalWriteLimit() > 0) {
            TrafficCounter tc = Configuration.configuration
                .getGlobalTrafficShapingHandler().trafficCounter();
            if (tc != null) {
                long wait = waitTraffic(Configuration.configuration.getServerGlobalWriteLimit(),
                        tc.currentWrittenBytes() + size,
                        tc.lastTime(), currentTime);
                if (wait > 0) {
                    return wait;
                }
            }
        }
        return 0;
    }

    private static final long waitTraffic(long limit, long bytes, long lastTime,
            long curtime) {
        long interval = curtime - lastTime;
        if (interval == 0) {
            // Time is too short, so just lets continue
            return 0;
        }
        return ((bytes * 1000 / limit - interval) / 10) * 10;
    }

    /**
     * Exit global ChannelFactory
     */
    public static void exit() {
        logger.info("Current launched threads before exit: " + ManagementFactory.getThreadMXBean().getThreadCount());
        if (Configuration.configuration.getConstraintLimitHandler() != null) {
            Configuration.configuration.getConstraintLimitHandler().release();
        }
        // First try to StopAll
        TransferUtils.stopSelectedTransfers(DbConstant.admin.getSession(), 0,
                null, null, null, null, null, null, null, null, null, true, true, true);
        Configuration.configuration.setShutdown(true);
        Configuration.configuration.prepareServerStop();
        final long delay = Configuration.configuration.getTIMEOUTCON();
        // Inform others that shutdown
        if (Configuration.configuration.getLocalTransaction() != null) {
            Configuration.configuration.getLocalTransaction().shutdownLocalChannels();
        }
        logger.info("Unbind server network services");
        Configuration.configuration.unbindServer();
        logger.warn(Messages.getString("ChannelUtils.7") + delay + " ms"); //$NON-NLS-1$
        try {
            Thread.sleep(delay);
        } catch (final InterruptedException e) {
        }
        NetworkTransaction.closeRetrieveExecutors();
        if (Configuration.configuration.getLocalTransaction() != null) {
            Configuration.configuration.getLocalTransaction().debugPrintActiveLocalChannels();
        }
        if (Configuration.configuration.getGlobalTrafficShapingHandler() != null) {
            Configuration.configuration.getGlobalTrafficShapingHandler().release();
        }
        logger.info("Exit Shutdown Http");
        terminateHttpChannels();
        logger.info("Exit Shutdown Local");
        if (Configuration.configuration.getLocalTransaction() != null) {
            Configuration.configuration.getLocalTransaction().closeAll();
        }
        logger.info("Exit Shutdown LocalExec");
        if (Configuration.configuration.isUseLocalExec()) {
            LocalExecClient.releaseResources();
        }
        logger.info("Exit Shutdown Command");
        terminateCommandChannels();
        logger.info("Exit Shutdown Db Connection");
        DbAdmin.closeAllConnection();
        logger.info("Exit Shutdown ServerStop");
        Configuration.configuration.serverStop();
        logger.warn(Messages.getString("ChannelUtils.15")); //$NON-NLS-1$
        System.err.println(Messages.getString("ChannelUtils.15")); //$NON-NLS-1$
        stopLogger();
        //Thread.currentThread().interrupt();
    }

    public final static void stopLogger() {
        if (WaarpLoggerFactory.getDefaultFactory() instanceof WaarpSlf4JLoggerFactory) {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
            lc.stop();
        }
    }

    /**
     * This function is the top function to be called when the server is to be shutdown.
     */
    @Override
    public void run() {
        logger.info("Should restart? " + R66ShutdownHook.isRestart());
        RestServiceInitializer.stopRestService();
        R66ShutdownHook.terminate(false);
    }

    /**
     * Start Shutdown
     */
    public final static void startShutdown() {
        if (R66ShutdownHook.isInShutdown()) {
            return;
        }
        Thread thread = new Thread(new ChannelUtils(), "R66 Shutdown Thread");
        thread.setDaemon(false);
        thread.start();
    }
}
