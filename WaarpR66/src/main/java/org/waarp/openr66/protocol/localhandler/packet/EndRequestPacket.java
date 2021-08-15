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
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;

import java.nio.charset.Charset;

/**
 * End of Request class
 * <p>
 * header = Error.code middle = way end = might be empty
 */
public class EndRequestPacket extends AbstractLocalPacket {
  private static final byte ASKVALIDATE = 0;

  private static final byte ANSWERVALIDATE = 1;

  private final int code;

  private byte way;

  private String optional;

  /**
   * @param headerLength
   * @param middleLength
   * @param endLength
   * @param buf
   *
   * @return the new EndTransferPacket from buffer
   *
   * @throws OpenR66ProtocolPacketException
   */
  public static EndRequestPacket createFromBuffer(final int headerLength,
                                                  final int middleLength,
                                                  final int endLength,
                                                  final ByteBuf buf)
      throws OpenR66ProtocolPacketException {
    if (headerLength - 1 != 4) {
      throw new OpenR66ProtocolPacketException("Not enough data");
    }
    if (middleLength != 1) {
      throw new OpenR66ProtocolPacketException("Not enough data");
    }
    final int bheader = buf.readInt();
    final byte valid = buf.readByte();
    final String optional;
    if (endLength > 0) {
      optional =
          buf.toString(buf.readerIndex(), endLength, Charset.defaultCharset());
      buf.skipBytes(endLength);
      return new EndRequestPacket(bheader, valid, optional);
    }
    return new EndRequestPacket(bheader, valid);
  }

  /**
   * @param code
   * @param valid
   * @param optional
   */
  private EndRequestPacket(final int code, final byte valid,
                           final String optional) {
    this.code = code;
    way = valid;
    this.optional = optional;
  }

  /**
   * @param code
   * @param valid
   */
  private EndRequestPacket(final int code, final byte valid) {
    this.code = code;
    way = valid;
  }

  /**
   * @param code
   */
  public EndRequestPacket(final int code) {
    this.code = code;
    way = ASKVALIDATE;
  }

  @Override
  public final boolean hasGlobalBuffer() {
    return true;
  }

  @Override
  public final synchronized void createAllBuffers(
      final LocalChannelReference lcr, final int networkHeader) {
    final int headerSize = 4;
    final int middleSize = 1;
    final byte[] endBytes =
        optional != null? optional.getBytes(WaarpStringUtils.UTF8) :
            EMPTY_ARRAY;
    final int endSize = endBytes.length;
    final int globalSize =
        networkHeader + LOCAL_HEADER_SIZE + headerSize + middleSize + endSize;
    int offset = networkHeader + LOCAL_HEADER_SIZE;
    global = ByteBufAllocator.DEFAULT.ioBuffer(globalSize, globalSize);
    header = WaarpNettyUtil.slice(global, offset, headerSize);
    header.writeInt(code);
    offset += headerSize;
    middle = WaarpNettyUtil.slice(global, offset, middleSize);
    middle.writeByte(way);
    offset += middleSize;
    end = WaarpNettyUtil.slice(global, offset, endSize);
    if (optional != null) {
      end.writeBytes(endBytes);
    }
  }

  @Override
  public final byte getType() {
    return LocalPacketFactory.ENDREQUESTPACKET;
  }

  @Override
  public final String toString() {
    return "EndRequestPacket: " + code + ' ' + way +
           (optional != null? ' ' + optional : "");
  }

  /**
   * @return the code
   */
  public final int getCode() {
    return code;
  }

  /**
   * @return True if this packet is to be validated
   */
  public final boolean isToValidate() {
    return way == ASKVALIDATE;
  }

  /**
   * Validate the connection
   */
  public final void validate() {
    way = ANSWERVALIDATE;
    clear();
  }

  /**
   * @return the optional
   */
  public final String getOptional() {
    return optional;
  }

  /**
   * @param optional the optional to set
   */
  public final void setOptional(final String optional) {
    this.optional = optional;
  }

}
