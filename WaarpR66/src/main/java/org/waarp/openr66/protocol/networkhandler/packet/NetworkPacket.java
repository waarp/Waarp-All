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
package org.waarp.openr66.protocol.networkhandler.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;

/**
 * Network Packet A Packet is composed of one global length field, two Id (4
 * bytes x 2) and a buffer. The
 * first Id is the localId on receive operation and the remoteId on send
 * operation. The second Id is the
 * reverse.
 */
public class NetworkPacket {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(NetworkPacket.class);
  public static final int NETWORK_HEADER_SIZE = 4 * 3 + 1;
  private ByteBuf buffer;

  private final int remoteId;

  private final int localId;

  private final byte code;

  private boolean uniqueBuffer;

  /**
   * @param localId
   * @param remoteId
   * @param code
   * @param buffer
   */
  public NetworkPacket(final int localId, final int remoteId, final byte code,
                       final ByteBuf buffer) {
    this.remoteId = remoteId;
    this.localId = localId;
    this.code = code;
    this.buffer = buffer;
    this.uniqueBuffer = false;
  }

  /**
   * @param localId
   * @param remoteId
   * @param packet
   * @param lcr the LocalChannelReference in use
   *
   * @throws OpenR66ProtocolPacketException
   */
  public NetworkPacket(final int localId, final int remoteId,
                       final AbstractLocalPacket packet,
                       final LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    this.remoteId = remoteId;
    this.localId = localId;
    code = packet.getType();
    uniqueBuffer = true;
    buffer = packet.getLocalPacketForNetworkPacket(lcr, this);
  }

  /**
   * @return the buffer
   */
  public ByteBuf getBuffer() {
    return buffer;
  }

  /**
   * @return the remoteId
   */
  public int getRemoteId() {
    return remoteId;
  }

  /**
   * @return the localId
   */
  public int getLocalId() {
    return localId;
  }

  /**
   * @return the code
   */
  public byte getCode() {
    return code;
  }

  public void writeNetworkHeader(final ByteBuf buf, final int capacity) {
    buf.writerIndex(0);
    buf.writeInt(capacity + NETWORK_HEADER_SIZE - 4);
    buf.writeInt(remoteId);
    buf.writeInt(localId);
    buf.writeByte(code);
  }

  /**
   * @return The corresponding ByteBuf
   */
  public ByteBuf getNetworkPacket() {
    if (uniqueBuffer) {
      writeNetworkHeader(buffer, buffer.capacity() - NETWORK_HEADER_SIZE);
      buffer.writerIndex(buffer.capacity());
      return buffer;
    }
    final ByteBuf buf = ByteBufAllocator.DEFAULT
        .buffer(NETWORK_HEADER_SIZE, NETWORK_HEADER_SIZE);
    writeNetworkHeader(buf, buffer.capacity());
    buffer = Unpooled.wrappedBuffer(buf, buffer);
    uniqueBuffer = true;
    return buffer;
  }

  @Override
  public String toString() {
    return "RId: " + remoteId + " LId: " + localId + " Code: " + code +
           " Length: " + (buffer != null? (buffer.readableBytes() + (code ==
                                                                     LocalPacketFactory.REQUESTPACKET?
        buffer.toString(buffer.readerIndex(), buffer.readableBytes(),
                        WaarpStringUtils.UTF8) : "")) : "no buffer");
  }

  public void clear() {
    if (WaarpNettyUtil.release(buffer)) {
      buffer = null;
    }
  }
}
