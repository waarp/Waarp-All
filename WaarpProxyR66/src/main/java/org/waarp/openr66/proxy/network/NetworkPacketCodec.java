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
package org.waarp.openr66.proxy.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.openr66.protocol.localhandler.packet.KeepAlivePacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketCodec;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.NoOpPacket;
import org.waarp.openr66.protocol.networkhandler.packet.NetworkPacket;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import java.util.List;

/**
 * Packet Decoder
 */
public class NetworkPacketCodec extends ByteToMessageCodec<NetworkPacket> {
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf buf,
                        List<Object> out) throws Exception {
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
    if (buf.readableBytes() < length) {
      buf.resetReaderIndex();
      return;
    }
    // Now we can read the two Ids
    // Slight change in Proxy = first is remote and second is local!
    final int remoteId = buf.readInt();
    final int localId = buf.readInt();
    final byte code = buf.readByte();
    final int index = buf.readerIndex();
    final ByteBuf buffer = buf.retainedSlice(index, length - 9);
    buf.skipBytes(length - 9);
    NetworkPacket networkPacket =
        new NetworkPacket(localId, remoteId, code, buffer);
    if (code == LocalPacketFactory.KEEPALIVEPACKET) {
      final KeepAlivePacket keepAlivePacket =
          (KeepAlivePacket) LocalPacketCodec.decodeNetworkPacket(buffer);
      buffer.release();
      if (keepAlivePacket.isToValidate()) {
        keepAlivePacket.validate();
        final NetworkPacket response =
            new NetworkPacket(ChannelUtils.NOCHANNEL, ChannelUtils.NOCHANNEL,
                              keepAlivePacket, null);
        ctx.writeAndFlush(response.getNetworkPacket());
      }
      // Replaced by a NoOp packet
      networkPacket =
          new NetworkPacket(localId, remoteId, new NoOpPacket(), null);
      final NetworkServerHandler nsh =
          (NetworkServerHandler) ctx.pipeline().last();
      nsh.resetKeepAlive();
    }
    out.add(networkPacket);
  }

  @Override
  protected void encode(ChannelHandlerContext ctx, NetworkPacket msg,
                        ByteBuf out) throws Exception {
    final ByteBuf finalBuf = msg.getNetworkPacket();
    out.writeBytes(finalBuf);
    WaarpNettyUtil.releaseCompletely(finalBuf);
  }
}
