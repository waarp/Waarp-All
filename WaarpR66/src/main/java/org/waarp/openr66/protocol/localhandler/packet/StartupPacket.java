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
 * Startup Message class
 * <p>
 * 1 localId (Integer): localId
 */
public class StartupPacket extends AbstractLocalPacket {
  private final Integer localId;
  private final boolean fromSsl;

  /**
   * @param headerLength
   * @param middleLength
   * @param endLength
   * @param buf
   *
   * @return the new ValidPacket from buffer
   */
  public static StartupPacket createFromBuffer(int headerLength,
                                               int middleLength, int endLength,
                                               ByteBuf buf) {
    final Integer newId = buf.readInt();
    final boolean fromSsl = buf.readBoolean();
    return new StartupPacket(newId, fromSsl);
  }

  /**
   * @param newId
   */
  public StartupPacket(Integer newId, boolean fromSsl) {
    localId = newId;
    this.fromSsl = fromSsl;
  }

  @Override
  public boolean hasGlobalBuffer() {
    return true;
  }

  @Override
  public void createAllBuffers(final LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    final int headerSize = 4;
    final int middleSize = 1;
    final int endSize = 0;
    global = ByteBufAllocator.DEFAULT
        .buffer(GLOBAL_HEADER_SIZE + headerSize + middleSize + endSize,
                GLOBAL_HEADER_SIZE + headerSize + middleSize + endSize);
    header = WaarpNettyUtil.slice(global, GLOBAL_HEADER_SIZE, headerSize);
    header.writeInt(localId);
    middle = WaarpNettyUtil
        .slice(global, GLOBAL_HEADER_SIZE + headerSize, middleSize);
    middle.writeBoolean(fromSsl);
    end = Unpooled.EMPTY_BUFFER;
  }

  @Override
  public void createEnd(LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    end = Unpooled.EMPTY_BUFFER;
  }

  @Override
  public void createHeader(LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    header = ByteBufAllocator.DEFAULT.buffer(4, 4);
    header.writeInt(localId);
  }

  @Override
  public void createMiddle(LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    middle = ByteBufAllocator.DEFAULT.buffer(1, 1);
    middle.writeBoolean(fromSsl);
  }

  @Override
  public String toString() {
    return "StartupPacket: " + localId;
  }

  @Override
  public byte getType() {
    return LocalPacketFactory.STARTUPPACKET;
  }

  /**
   * @return the localId
   */
  public Integer getLocalId() {
    return localId;
  }

  public boolean isFromSsl() {
    return fromSsl;
  }

}
