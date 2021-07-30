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
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;

/**
 * No Op class
 * <p>
 * header = empty middle = empty end = empty
 */
public class NoOpPacket extends AbstractLocalPacket {

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
  public static NoOpPacket createFromBuffer(final int headerLength,
                                            final int middleLength,
                                            final int endLength,
                                            final ByteBuf buf) {
    return new NoOpPacket();
  }

  @Override
  public final boolean hasGlobalBuffer() {
    return true;
  }

  @Override
  public final void createAllBuffers(final LocalChannelReference lcr,
                                     final int networkHeader) {
    final int globalSize = networkHeader + LOCAL_HEADER_SIZE;
    global = ByteBufAllocator.DEFAULT.ioBuffer(globalSize, globalSize);
    end = Unpooled.EMPTY_BUFFER;
    header = Unpooled.EMPTY_BUFFER;
    middle = Unpooled.EMPTY_BUFFER;
  }

  @Override
  public final byte getType() {
    return LocalPacketFactory.NOOPPACKET;
  }

  @Override
  public final String toString() {
    return "NoOpPacket";
  }

}
