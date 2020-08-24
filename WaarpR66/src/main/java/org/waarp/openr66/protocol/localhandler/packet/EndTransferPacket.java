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
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;

import java.nio.charset.Charset;

/**
 * End of Transfer class
 * <p>
 * header = "request" middle = way end = might be empty
 */
public class EndTransferPacket extends AbstractLocalPacket {
  private static final byte ASKVALIDATE = 0;

  private static final byte ANSWERVALIDATE = 1;

  private final byte request;

  private byte way;

  private String hashOptional;

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
  public static EndTransferPacket createFromBuffer(int headerLength,
                                                   int middleLength,
                                                   int endLength, ByteBuf buf)
      throws OpenR66ProtocolPacketException {
    if (headerLength - 1 != 1) {
      throw new OpenR66ProtocolPacketException("Not enough data");
    }
    if (middleLength != 1) {
      throw new OpenR66ProtocolPacketException("Not enough data");
    }
    final byte bheader = buf.readByte();
    final byte valid = buf.readByte();
    String optional;
    if (endLength > 0) {
      optional =
          buf.toString(buf.readerIndex(), endLength, Charset.defaultCharset());
      buf.skipBytes(endLength);
      return new EndTransferPacket(bheader, valid, optional);
    }
    return new EndTransferPacket(bheader, valid);
  }

  /**
   * @param request
   * @param valid
   * @param hashOptional
   */
  private EndTransferPacket(byte request, byte valid, String hashOptional) {
    this.request = request;
    way = valid;
    this.hashOptional = hashOptional;
  }

  /**
   * @param request
   * @param valid
   */
  private EndTransferPacket(byte request, byte valid) {
    this.request = request;
    way = valid;
  }

  /**
   * @param request
   */
  public EndTransferPacket(byte request) {
    this.request = request;
    way = ASKVALIDATE;
  }

  /**
   * @param request
   * @param hashOptional
   */
  public EndTransferPacket(byte request, String hashOptional) {
    this.request = request;
    way = ASKVALIDATE;
    this.hashOptional = hashOptional;
  }

  @Override
  public boolean hasGlobalBuffer() {
    return true;
  }

  @Override
  public void createAllBuffers(LocalChannelReference lcr, int networkHeader)
      throws OpenR66ProtocolPacketException {
    final int headerSize = 1;
    final int middleSize = 1;
    final byte[] endBytes =
        hashOptional != null? hashOptional.getBytes(WaarpStringUtils.UTF8) :
            EMPTY_ARRAY;
    final int endSize = endBytes.length;
    final int globalSize =
        networkHeader + LOCAL_HEADER_SIZE + headerSize + middleSize + endSize;
    int offset = networkHeader + LOCAL_HEADER_SIZE;
    global = ByteBufAllocator.DEFAULT.buffer(globalSize, globalSize);
    header = WaarpNettyUtil.slice(global, offset, headerSize);
    header.writeByte(request);
    offset += headerSize;
    middle = WaarpNettyUtil.slice(global, offset, middleSize);
    middle.writeByte(way);
    offset += middleSize;
    end = WaarpNettyUtil.slice(global, offset, endSize);
    if (hashOptional != null) {
      end.writeBytes(endBytes);
    }
  }

  @Override
  public void createEnd(LocalChannelReference lcr) {
    if (hashOptional == null) {
      end = Unpooled.EMPTY_BUFFER;
    } else {
      end = Unpooled.copiedBuffer(hashOptional, Charset.defaultCharset());
    }
  }

  @Override
  public void createHeader(LocalChannelReference lcr) {
    final byte[] newbytes = { request };
    header = Unpooled.wrappedBuffer(newbytes);
  }

  @Override
  public void createMiddle(LocalChannelReference lcr) {
    final byte[] newbytes = { way };
    middle = Unpooled.wrappedBuffer(newbytes);
  }

  @Override
  public byte getType() {
    return LocalPacketFactory.ENDTRANSFERPACKET;
  }

  @Override
  public String toString() {
    return "EndTransferPacket: " + request + ' ' + way +
           (hashOptional != null? ' ' + hashOptional : "");
  }

  /**
   * @return the requestId
   */
  public byte getRequest() {
    return request;
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
    clear();
  }

  /**
   * @return the optional
   */
  public String getOptional() {
    return hashOptional;
  }

  /**
   * @param optional the optional to set
   */
  public void setOptional(String optional) {
    hashOptional = optional;
  }
}
