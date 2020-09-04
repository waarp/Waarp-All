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
 * Business Request Message class for packet
 * <p>
 * 1 string and on integer and one byte:<br>
 * - sheader = full text with class at first place, following (space separated)
 * by extra arguments - smiddle =
 * integer - send = byte
 */
public class BusinessRequestPacket extends AbstractLocalPacket {
  private static final byte ASKVALIDATE = 0;

  private static final byte ANSWERVALIDATE = 1;
  private static final byte ANSWERINVALIDATE = 2;

  private final String sheader;

  private int delay;

  private byte way;

  public static BusinessRequestPacket createFromBuffer(final int headerLength,
                                                       final int middleLength,
                                                       final int endLength,
                                                       final ByteBuf buf)
      throws OpenR66ProtocolPacketException {
    final byte[] bheader = new byte[headerLength - 1];
    if (headerLength - 1 > 0) {
      buf.readBytes(bheader);
    }
    if (middleLength != 4) {
      throw new OpenR66ProtocolPacketException("Packet not correct");
    }
    final int delay = buf.readInt();
    if (endLength != 1) {
      throw new OpenR66ProtocolPacketException("Packet not correct");
    }
    final byte valid = buf.readByte();
    return new BusinessRequestPacket(new String(bheader), delay, valid);
  }

  public BusinessRequestPacket(final String header, final int delay,
                               final byte way) {
    sheader = header;
    this.delay = delay;
    this.way = way;
  }

  public BusinessRequestPacket(final String header, final int delay) {
    sheader = header;
    this.delay = delay;
    way = ASKVALIDATE;
  }

  @Override
  public boolean hasGlobalBuffer() {
    return true;
  }

  @Override
  public void createAllBuffers(final LocalChannelReference lcr,
                               final int networkHeader)
      throws OpenR66ProtocolPacketException {
    final byte[] headerBytes = sheader.getBytes();
    final int headerSize = headerBytes.length;
    final int middleSize = 4;
    final int endSize = 1;
    final int globalSize =
        networkHeader + LOCAL_HEADER_SIZE + headerSize + middleSize + endSize;
    int offset = networkHeader + LOCAL_HEADER_SIZE;
    global = ByteBufAllocator.DEFAULT.buffer(globalSize, globalSize);
    header = WaarpNettyUtil.slice(global, offset, headerSize);
    header.writeBytes(headerBytes);
    offset += headerSize;
    middle = WaarpNettyUtil.slice(global, offset, middleSize);
    middle.writeInt(delay);
    offset += middleSize;
    end = WaarpNettyUtil.slice(global, offset, endSize);
    end.writeByte(way);
    global.retain();
  }

  @Override
  public byte getType() {
    return LocalPacketFactory.BUSINESSREQUESTPACKET;
  }

  @Override
  public String toString() {
    return "BusinessRequestPacket: " + sheader + ':' + delay + ':' + way;
  }

  /**
   * @return True if this packet is to be validated
   */
  public boolean isToValidate() {
    return way == ASKVALIDATE;
  }

  /**
   * Validate the request
   */
  public void validate() {
    way = ANSWERVALIDATE;
    clear();
  }

  /**
   * Invalidate the request
   */
  public void invalidate() {
    way = ANSWERINVALIDATE;
    clear();
  }

  /**
   * @return the sheader
   */
  public String getSheader() {
    return sheader;
  }

  /**
   * @return the delay
   */
  public int getDelay() {
    return delay;
  }

  /**
   * @param delay the delay to set
   */
  public void setDelay(final int delay) {
    this.delay = delay;
    WaarpNettyUtil.release(middle);
    middle = null;
  }
}
