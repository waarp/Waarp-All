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
 * Test class for packet
 * <p>
 * 3 strings: sheader,smiddle,send
 */
public class TestPacket extends AbstractLocalPacket {
  public static final int PINGPONG = 100;

  private final String sheader;

  private final String smiddle;

  private int code;

  public static TestPacket createFromBuffer(int headerLength, int middleLength,
                                            int endLength, ByteBuf buf) {
    final byte[] bheader = new byte[headerLength - 1];
    final byte[] bmiddle = new byte[middleLength];
    if (headerLength - 1 > 0) {
      buf.readBytes(bheader);
    }
    if (middleLength > 0) {
      buf.readBytes(bmiddle);
    }
    return new TestPacket(new String(bheader), new String(bmiddle),
                          buf.readInt());
  }

  public TestPacket(String header, String middle, int code) {
    sheader = header;
    smiddle = middle;
    this.code = code;
  }

  @Override
  public boolean hasGlobalBuffer() {
    return true;
  }

  @Override
  public void createAllBuffers(final LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    final byte[] headerBytes =
        sheader != null? sheader.getBytes() : EMPTY_ARRAY;
    final int headerSize = headerBytes.length;
    final byte[] middleBytes =
        smiddle != null? smiddle.getBytes() : EMPTY_ARRAY;
    final int middleSize = middleBytes.length;
    final int endSize = 4;
    global = ByteBufAllocator.DEFAULT
        .buffer(GLOBAL_HEADER_SIZE + headerSize + middleSize + endSize,
                GLOBAL_HEADER_SIZE + headerSize + middleSize + endSize);
    header = WaarpNettyUtil.slice(global, GLOBAL_HEADER_SIZE, headerSize);
    header.writeBytes(headerBytes);
    middle = WaarpNettyUtil
        .slice(global, GLOBAL_HEADER_SIZE + headerSize, middleSize);
    middle.writeBytes(middleBytes);
    end = WaarpNettyUtil
        .slice(global, GLOBAL_HEADER_SIZE + headerSize + middleSize, endSize);
    end.writeInt(code);
  }

  @Override
  public void createEnd(LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    end = ByteBufAllocator.DEFAULT.buffer(4, 4);
    end.writeInt(code);
  }

  @Override
  public void createHeader(LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    header = Unpooled.wrappedBuffer(sheader.getBytes());
  }

  @Override
  public void createMiddle(LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    middle = Unpooled.wrappedBuffer(smiddle.getBytes());
  }

  @Override
  public byte getType() {
    if (code > PINGPONG) {
      return LocalPacketFactory.VALIDPACKET;
    }
    return LocalPacketFactory.TESTPACKET;
  }

  @Override
  public String toString() {
    return "TestPacket: " + sheader + ':' + smiddle + ':' + code;
  }

  public void update() {
    code++;
    end = null;
  }
}
