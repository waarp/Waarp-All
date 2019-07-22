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
import io.netty.buffer.Unpooled;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;

import java.nio.charset.Charset;

/**
 * End of Request class
 * <p>
 * header = Error.code middle = way end = might be empty
 *
 *
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
  public static EndRequestPacket createFromBuffer(int headerLength,
                                                  int middleLength,
                                                  int endLength, ByteBuf buf)
      throws OpenR66ProtocolPacketException {
    if (headerLength - 1 != 4) {
      throw new OpenR66ProtocolPacketException("Not enough data");
    }
    if (middleLength != 1) {
      throw new OpenR66ProtocolPacketException("Not enough data");
    }
    final int bheader = buf.readInt();
    final byte valid = buf.readByte();
    String optional;
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
  private EndRequestPacket(int code, byte valid, String optional) {
    this.code = code;
    way = valid;
    this.optional = optional;
  }

  /**
   * @param code
   * @param valid
   */
  private EndRequestPacket(int code, byte valid) {
    this.code = code;
    way = valid;
  }

  /**
   * @param code
   */
  public EndRequestPacket(int code) {
    this.code = code;
    way = ASKVALIDATE;
  }

  @Override
  public void createEnd(LocalChannelReference lcr) {
    if (optional == null) {
      end = Unpooled.EMPTY_BUFFER;
    } else {
      end = Unpooled.copiedBuffer(optional, Charset.defaultCharset());
    }
  }

  @Override
  public void createHeader(LocalChannelReference lcr) {
    header = Unpooled.buffer(4);
    header.writeInt(code);
  }

  @Override
  public void createMiddle(LocalChannelReference lcr) {
    final byte[] newbytes = { way };
    middle = Unpooled.wrappedBuffer(newbytes);
  }

  @Override
  public byte getType() {
    return LocalPacketFactory.ENDREQUESTPACKET;
  }

  @Override
  public String toString() {
    return "EndRequestPacket: " + code + " " + way +
           (optional != null? " " + optional : "");
  }

  /**
   * @return the code
   */
  public int getCode() {
    return code;
  }

  /**
   * @return True if this packet is to be validated
   */
  public boolean isToValidate() {
    return way == ASKVALIDATE;
  }

  /**
   * Validate the connection
   */
  public void validate() {
    way = ANSWERVALIDATE;
    header = null;
    middle = null;
    end = null;
  }

  /**
   * @return the optional
   */
  public String getOptional() {
    return optional;
  }

  /**
   * @param optional the optional to set
   */
  public void setOptional(String optional) {
    this.optional = optional;
  }

}
