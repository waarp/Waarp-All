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
package org.waarp.openr66.protocol.networkhandler.packet;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.packet.KeepAlivePacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketCodec;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.NoOpPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkChannelReference;
import org.waarp.openr66.protocol.networkhandler.NetworkServerHandler;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;

/**
 * Packet Decoder
 * 
 * @author Frederic Bregier
 */
public class NetworkPacketCodec extends ByteToMessageCodec<NetworkPacket> {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory.getLogger(NetworkPacketCodec.class);


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        // Make sure if the length field was received.
        if (buf.readableBytes() < 4) {
            // The length field was not received yet - return null.
            // This method will be invoked again when more packets are
            // received and appended to the buffer.
            return;
        }
        // Mark the current buffer position
        buf.markReaderIndex();
        // Read the length field
        final int length = buf.readInt();
        if (length < 9) {
            throw new OpenR66ProtocolPacketException("Incorrect decode first field in Network Packet: " + length
                    + " < 9");
        }
        if (buf.readableBytes() < length) {
            buf.resetReaderIndex();
            return;
        }
        // Now we can read the two Ids
        final int localId = buf.readInt();
        final int remoteId = buf.readInt();
        final byte code = buf.readByte();
        int readerInder = buf.readerIndex();
        ByteBuf buffer = buf.slice(readerInder, length - 9);
        buffer.retain();
        buf.skipBytes(length - 9);
        NetworkPacket networkPacket = new NetworkPacket(localId, remoteId, code, buffer);
        logger.trace("received network packet {}", networkPacket);
        if (code == LocalPacketFactory.KEEPALIVEPACKET) {
            KeepAlivePacket keepAlivePacket = (KeepAlivePacket)
                    LocalPacketCodec.decodeNetworkPacket(networkPacket.getBuffer());
            if (keepAlivePacket.isToValidate()) {
                keepAlivePacket.validate();
                NetworkPacket response =
                        new NetworkPacket(ChannelUtils.NOCHANNEL,
                                ChannelUtils.NOCHANNEL, keepAlivePacket, null);
                NetworkChannelReference nc = NetworkTransaction.getImmediateNetworkChannel(ctx.channel());
                if (nc != null) {
                    nc.useIfUsed();
                }
                ctx.writeAndFlush(response.getNetworkPacket());
                buffer.release();
            }
            // Replaced by a NoOp packet
            networkPacket = new NetworkPacket(localId, remoteId, new NoOpPacket(), null);
            NetworkServerHandler nsh = (NetworkServerHandler) ctx.pipeline().last();
            nsh.setKeepAlivedSent();
        }
        out.add(networkPacket);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, NetworkPacket msg, ByteBuf out) throws Exception {
        final NetworkPacket packet = msg;
        logger.trace("sending network packet {}", packet);
        final ByteBuf finalBuf = packet.getNetworkPacket();
        out.writeBytes(finalBuf);
        finalBuf.release();
        //msg.getBuffer().release();
    }

}
