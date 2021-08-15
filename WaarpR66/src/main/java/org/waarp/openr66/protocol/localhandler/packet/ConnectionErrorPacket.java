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
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;

/**
 * Connection Error Message class for packet
 * <p>
 * 2 strings: sheader,smiddle
 */
public class ConnectionErrorPacket extends AbstractLocalPacket {

  private final String sheader;

  private final String smiddle;

  /**
   * @param headerLength
   * @param middleLength
   * @param endLength
   * @param buf
   *
   * @return the new ErrorPacket from buffer
   */
  public static ConnectionErrorPacket createFromBuffer(final int headerLength,
                                                       final int middleLength,
                                                       final int endLength,
                                                       final ByteBuf buf) {
    final byte[] bheader = new byte[headerLength - 1];
    final byte[] bmiddle = new byte[middleLength];
    if (headerLength - 1 > 0) {
      buf.readBytes(bheader);
    }
    if (middleLength > 0) {
      buf.readBytes(bmiddle);
    }
    return new ConnectionErrorPacket(new String(bheader, WaarpStringUtils.UTF8),
                                     new String(bmiddle,
                                                WaarpStringUtils.UTF8));
  }

  /**
   * @param header
   * @param middle
   */
  public ConnectionErrorPacket(final String header, final String middle) {
    sheader = header;
    smiddle = middle;
  }

  @Override
  public final boolean hasGlobalBuffer() {
    return true;
  }

  @Override
  public final synchronized void createAllBuffers(
      final LocalChannelReference lcr, final int networkHeader) {
    end = Unpooled.EMPTY_BUFFER;
    final byte[] sheaderByte =
        sheader != null? sheader.getBytes(WaarpStringUtils.UTF8) : EMPTY_ARRAY;
    final int headerSize = sheaderByte.length;
    final byte[] smiddleByte =
        smiddle != null? smiddle.getBytes(WaarpStringUtils.UTF8) : EMPTY_ARRAY;
    final int middleSize = smiddleByte.length;
    final int globalSize =
        networkHeader + LOCAL_HEADER_SIZE + headerSize + middleSize;
    int offset = networkHeader + LOCAL_HEADER_SIZE;
    global = ByteBufAllocator.DEFAULT.ioBuffer(globalSize, globalSize);
    header = WaarpNettyUtil.slice(global, offset, headerSize);
    if (sheader != null) {
      header.writeBytes(sheaderByte);
    }
    offset += headerSize;
    middle = WaarpNettyUtil.slice(global, offset, middleSize);
    if (smiddle != null) {
      middle.writeBytes(smiddleByte);
    }
  }

  @Override
  public final String toString() {
    return "ConnectionErrorPacket: " + sheader + ':' + smiddle;
  }

  @Override
  public final byte getType() {
    return LocalPacketFactory.CONNECTERRORPACKET;
  }

  /**
   * @return the sheader
   */
  public final String getSheader() {
    return sheader;
  }

  /**
   * @return the smiddle
   */
  public final String getSmiddle() {
    return smiddle;
  }
}
