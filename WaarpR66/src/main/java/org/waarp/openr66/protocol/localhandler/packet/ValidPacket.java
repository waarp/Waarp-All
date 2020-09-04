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

/**
 * Validation Message class for packet
 * <p>
 * 2 strings and one byte: sheader,smiddle,send
 */
public class ValidPacket extends AbstractLocalPacket {
  private final String sheader;

  private String smiddle;

  private final byte send;

  /**
   * @param headerLength
   * @param middleLength
   * @param endLength
   * @param buf
   *
   * @return the new ValidPacket from buffer
   */
  public static ValidPacket createFromBuffer(final int headerLength,
                                             final int middleLength,
                                             final int endLength,
                                             final ByteBuf buf) {
    final byte[] bheader = new byte[headerLength - 1];
    final byte[] bmiddle = new byte[middleLength];
    final byte bend;
    if (headerLength - 1 > 0) {
      buf.readBytes(bheader);
    }
    if (middleLength > 0) {
      buf.readBytes(bmiddle);
    }
    bend = buf.readByte();
    return new ValidPacket(new String(bheader), new String(bmiddle), bend);
  }

  /**
   * @param header
   * @param middle
   * @param end
   */
  public ValidPacket(final String header, final String middle, final byte end) {
    sheader = header;
    smiddle = middle;
    send = end;
  }

  @Override
  public boolean hasGlobalBuffer() {
    return true;
  }

  @Override
  public void createAllBuffers(final LocalChannelReference lcr,
                               final int networkHeader)
      throws OpenR66ProtocolPacketException {
    final byte[] headerBytes =
        sheader != null? sheader.getBytes() : EMPTY_ARRAY;
    final int headerSize = headerBytes.length;
    final byte[] middleBytes =
        smiddle != null? smiddle.getBytes() : EMPTY_ARRAY;
    final int middleSize = middleBytes.length;
    final int endSize = 1;
    final int globalSize =
        networkHeader + LOCAL_HEADER_SIZE + headerSize + middleSize + endSize;
    int offset = networkHeader + LOCAL_HEADER_SIZE;
    global = ByteBufAllocator.DEFAULT.buffer(globalSize, globalSize);
    header = WaarpNettyUtil.slice(global, offset, headerSize);
    header.writeBytes(headerBytes);
    offset += headerSize;
    middle = WaarpNettyUtil.slice(global, offset, middleSize);
    middle.writeBytes(middleBytes);
    offset += middleSize;
    end = WaarpNettyUtil.slice(global, offset, endSize);
    end.writeByte(send);
  }

  @Override
  public String toString() {
    return "ValidPacket: " + sheader + ':' + smiddle + ':' + send;
  }

  @Override
  public byte getType() {
    return LocalPacketFactory.VALIDPACKET;
  }

  /**
   * @return the sheader
   */
  public String getSheader() {
    return sheader;
  }

  /**
   * @return the smiddle
   */
  public String getSmiddle() {
    return smiddle;
  }

  /**
   * @param smiddle
   */
  public void setSmiddle(final String smiddle) {
    this.smiddle = smiddle;
    middle = null;
  }

  /**
   * @return the type
   */
  public byte getTypeValid() {
    return send;
  }

}
