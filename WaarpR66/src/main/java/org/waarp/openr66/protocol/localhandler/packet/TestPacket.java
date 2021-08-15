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

  public static TestPacket createFromBuffer(final int headerLength,
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
    return new TestPacket(new String(bheader, WaarpStringUtils.UTF8),
                          new String(bmiddle, WaarpStringUtils.UTF8),
                          buf.readInt());
  }

  public TestPacket(final String header, final String middle, final int code) {
    sheader = header;
    smiddle = middle;
    this.code = code;
  }

  @Override
  public final boolean hasGlobalBuffer() {
    return true;
  }

  @Override
  public final synchronized void createAllBuffers(
      final LocalChannelReference lcr, final int networkHeader) {
    final byte[] headerBytes =
        sheader != null? sheader.getBytes(WaarpStringUtils.UTF8) : EMPTY_ARRAY;
    final int headerSize = headerBytes.length;
    final byte[] middleBytes =
        smiddle != null? smiddle.getBytes(WaarpStringUtils.UTF8) : EMPTY_ARRAY;
    final int middleSize = middleBytes.length;
    final int endSize = 4;
    final int globalSize =
        networkHeader + LOCAL_HEADER_SIZE + headerSize + middleSize + endSize;
    int offset = networkHeader + LOCAL_HEADER_SIZE;
    global = ByteBufAllocator.DEFAULT.ioBuffer(globalSize, globalSize);
    header = WaarpNettyUtil.slice(global, offset, headerSize);
    header.writeBytes(headerBytes);
    offset += headerSize;
    middle = WaarpNettyUtil.slice(global, offset, middleSize);
    middle.writeBytes(middleBytes);
    offset += middleSize;
    end = WaarpNettyUtil.slice(global, offset, endSize);
    end.writeInt(code);
  }

  @Override
  public final byte getType() {
    if (code > PINGPONG) {
      return LocalPacketFactory.VALIDPACKET;
    }
    return LocalPacketFactory.TESTPACKET;
  }

  @Override
  public final String toString() {
    return "TestPacket: " + sheader + ':' + smiddle + ':' + code;
  }

  public final synchronized void update() {
    code++;
    end = null;
  }
}
