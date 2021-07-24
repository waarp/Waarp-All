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
package org.waarp.openr66.protocol.localhandler.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.networkhandler.packet.NetworkPacket;

import static org.waarp.openr66.protocol.networkhandler.packet.NetworkPacket.*;

/**
 * This class represents Abstract Packet with its header, middle and end parts.
 * A Packet is composed of one
 * Header part, one Middle part (data), and one End part. Header: length field
 * (4 bytes) = Middle length field
 * (4 bytes), End length field (4 bytes), type field (1 byte), ...<br>
 * Middle: (Middle length field bytes)<br>
 * End: (End length field bytes) = code status field (4 bytes), ...<br>
 */
public abstract class AbstractLocalPacket {
  protected static final byte[] EMPTY_ARRAY = {};
  protected static final int LOCAL_HEADER_SIZE = 4 * 3 + 1;

  protected ByteBuf header;

  protected ByteBuf middle;

  protected ByteBuf end;

  protected ByteBuf global = null;

  protected AbstractLocalPacket() {
    header = null;
    middle = null;
    end = null;
  }

  /**
   * @return True if all buffers fit in one piece
   */
  public abstract boolean hasGlobalBuffer();

  /**
   * Prepare the 3 buffers Header, Middle and End
   *
   * @throws OpenR66ProtocolPacketException
   */
  public abstract void createAllBuffers(final LocalChannelReference lcr,
                                        final int networkHeader)
      throws OpenR66ProtocolPacketException;

  /**
   * Prepare the Header buffer
   *
   * @throws OpenR66ProtocolPacketException
   */
  public void createHeader(final LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    throw new IllegalStateException("Should not be called");
  }

  /**
   * Prepare the Middle buffer
   *
   * @throws OpenR66ProtocolPacketException
   */
  public void createMiddle(final LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    throw new IllegalStateException("Should not be called");
  }

  /**
   * Prepare the End buffer
   *
   * @throws OpenR66ProtocolPacketException
   */
  public void createEnd(final LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    throw new IllegalStateException("Should not be called");
  }

  /**
   * @return the type of Packet
   */
  public abstract byte getType();

  @Override
  public abstract String toString();

  /**
   * @param lcr the LocalChannelReference in use
   *
   * @return the ByteBuf as LocalPacket
   *
   * @throws OpenR66ProtocolPacketException
   */
  public final ByteBuf getLocalPacket(final LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    return getLocalPacketForNetworkPacket(lcr, null);
  }

  /**
   * @param lcr the LocalChannelReference in use
   *
   * @return the ByteBuf as LocalPacket
   *
   * @throws OpenR66ProtocolPacketException
   */
  public final ByteBuf getLocalPacketForNetworkPacket(
      final LocalChannelReference lcr, final NetworkPacket packet)
      throws OpenR66ProtocolPacketException {
    final ByteBuf buf;
    final int globalHeader;
    if (packet != null) {
      globalHeader = NETWORK_HEADER_SIZE;
    } else {
      globalHeader = 0;
    }
    if (hasGlobalBuffer()) {
      if (global == null) {
        createAllBuffers(lcr, globalHeader);
      } else {
        global.readerIndex(0);
        global.writerIndex(0);
      }
      buf = global;
    } else {
      // 3 header lengths+type
      buf = ByteBufAllocator.DEFAULT.ioBuffer(globalHeader + LOCAL_HEADER_SIZE,
                                              globalHeader + LOCAL_HEADER_SIZE);
      if (header == null) {
        createHeader(lcr);
      }
      if (middle == null) {
        createMiddle(lcr);
      }
      if (end == null) {
        createEnd(lcr);
      }
    }
    if (packet != null) {
      final int capacity =
          LOCAL_HEADER_SIZE + (header != null? header.capacity() : 0) +
          (middle != null? middle.capacity() : 0) +
          (end != null? end.capacity() : 0);
      packet.writeNetworkHeader(buf, capacity);
    }
    return getByteBuf(buf);
  }

  private ByteBuf getByteBuf(final ByteBuf buf) {
    final ByteBuf newHeader = header != null? header : Unpooled.EMPTY_BUFFER;
    final int headerLength = LOCAL_HEADER_SIZE - 4 + newHeader.readableBytes();
    final ByteBuf newMiddle = middle != null? middle : Unpooled.EMPTY_BUFFER;
    final int middleLength = newMiddle.readableBytes();
    final ByteBuf newEnd = end != null? end : Unpooled.EMPTY_BUFFER;
    final int endLength = newEnd.readableBytes();
    buf.writeInt(headerLength);
    buf.writeInt(middleLength);
    buf.writeInt(endLength);
    buf.writeByte(getType());
    if (hasGlobalBuffer()) {
      buf.writerIndex(buf.capacity());
      return buf;
    }
    return ByteBufAllocator.DEFAULT.compositeDirectBuffer(4)
                                   .addComponents(buf, newHeader, newMiddle,
                                                  newEnd);
  }

  public void clear() {
    if (WaarpNettyUtil.release(global)) {
      global = null;
    }
    if (hasGlobalBuffer()) {
      return;
    }
    if (WaarpNettyUtil.release(header)) {
      header = null;
    }
    if (WaarpNettyUtil.release(middle)) {
      middle = null;
    }
    if (WaarpNettyUtil.release(end)) {
      end = null;
    }
  }

  public final void retain() {
    if (global != null) {
      global.retain();
    }
    if (hasGlobalBuffer()) {
      return;
    }
    if (header != null) {
      header.retain();
    }
    if (middle != null) {
      middle.retain();
    }
    if (end != null) {
      end.retain();
    }
  }
}
