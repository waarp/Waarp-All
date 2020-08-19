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
 * Shutdown Message class for packet
 * <p>
 * 1 string: spassword(or key) + 1 byte (optional) to restart (if not null)
 */
public class ShutdownPacket extends AbstractLocalPacket {
  private final byte[] key;
  private final byte restart;

  /**
   * @param headerLength
   * @param middleLength
   * @param endLength
   * @param buf
   *
   * @return the new ShutdownPacket from buffer
   *
   * @throws OpenR66ProtocolPacketException
   */
  public static ShutdownPacket createFromBuffer(int headerLength,
                                                int middleLength, int endLength,
                                                ByteBuf buf)
      throws OpenR66ProtocolPacketException {
    if (headerLength - 1 <= 0) {
      throw new OpenR66ProtocolPacketException("Not enough data");
    }
    final byte[] bpassword = new byte[headerLength - 1];
    if (headerLength - 1 > 0) {
      buf.readBytes(bpassword);
    }
    byte torestart = 0;
    if (middleLength > 0) {
      torestart = buf.readByte();
    }
    return new ShutdownPacket(bpassword, torestart);
  }

  /**
   * @param spassword
   */
  public ShutdownPacket(byte[] spassword) {
    key = spassword;
    restart = 0;
  }

  /**
   * @param spassword
   * @param restart
   */
  public ShutdownPacket(byte[] spassword, byte restart) {
    key = spassword;
    this.restart = restart;
  }

  @Override
  public boolean hasGlobalBuffer() {
    return true;
  }

  @Override
  public void createAllBuffers(final LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    end = Unpooled.EMPTY_BUFFER;
    int sizeKey = key != null? key.length : 0;
    int sizeMiddle = restart != 0? 1 : 0;
    global = sizeKey + sizeMiddle == 0? Unpooled.EMPTY_BUFFER :
        ByteBufAllocator.DEFAULT
            .buffer(GLOBAL_HEADER_SIZE + sizeKey + sizeMiddle,
                    GLOBAL_HEADER_SIZE + sizeKey + sizeMiddle);
    if (key != null) {
      header = WaarpNettyUtil.slice(global, GLOBAL_HEADER_SIZE, sizeKey);
      header.writeBytes(key);
    } else {
      header = Unpooled.EMPTY_BUFFER;
    }
    if (restart != 0) {
      middle = WaarpNettyUtil
          .slice(global, GLOBAL_HEADER_SIZE + sizeKey, sizeMiddle);
      middle.writeByte(restart);
    } else {
      middle = Unpooled.EMPTY_BUFFER;
    }
  }

  @Override
  public void createEnd(LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    end = Unpooled.EMPTY_BUFFER;
  }

  @Override
  public void createHeader(LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    if (key != null) {
      header = Unpooled.wrappedBuffer(key);
    }
  }

  @Override
  public void createMiddle(LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    if (restart != 0) {
      final byte[] array = { restart };
      middle = Unpooled.wrappedBuffer(array);
    } else {
      middle = Unpooled.EMPTY_BUFFER;
    }
  }

  @Override
  public String toString() {
    return "ShutdownPacket" + (restart != 0? " and restart" : "");
  }

  @Override
  public byte getType() {
    return LocalPacketFactory.SHUTDOWNPACKET;
  }

  /**
   * @return the key
   */
  public byte[] getKey() {
    return key;
  }

  public boolean isRestart() {
    return restart != 0;
  }
}
