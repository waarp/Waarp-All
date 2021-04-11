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
  public static StartupPacket createFromBuffer(final int headerLength,
                                               final int middleLength,
                                               final int endLength,
                                               final ByteBuf buf) {
    final Integer newId = buf.readInt();
    final boolean fromSsl = buf.readBoolean();
    return new StartupPacket(newId, fromSsl);
  }

  /**
   * @param newId
   */
  public StartupPacket(final Integer newId, final boolean fromSsl) {
    localId = newId;
    this.fromSsl = fromSsl;
  }

  @Override
  public boolean hasGlobalBuffer() {
    return true;
  }

  @Override
  public void createAllBuffers(final LocalChannelReference lcr,
                               final int networkHeader) {
    final int headerSize = 4;
    final int middleSize = 1;
    final int endSize = 0;
    final int globalSize =
        networkHeader + LOCAL_HEADER_SIZE + headerSize + middleSize + endSize;
    int offset = networkHeader + LOCAL_HEADER_SIZE;
    global = ByteBufAllocator.DEFAULT.ioBuffer(globalSize, globalSize);
    header = WaarpNettyUtil.slice(global, offset, headerSize);
    header.writeInt(localId);
    offset += headerSize;
    middle = WaarpNettyUtil.slice(global, offset, middleSize);
    middle.writeBoolean(fromSsl);
    end = Unpooled.EMPTY_BUFFER;
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
