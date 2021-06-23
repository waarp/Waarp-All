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
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;

/**
 * Local Packet Decoder
 */
public class LocalPacketCodec {

  public static AbstractLocalPacket decodeNetworkPacket(final ByteBuf buf)
      throws OpenR66ProtocolPacketException {
    // Mark the current buffer position
    buf.markReaderIndex();
    // Read the length field
    final int length = buf.readInt();
    if (buf.readableBytes() < length) {
      buf.resetReaderIndex();
      return null;
    }
    // Now we can read the header
    // Header: Header length field (4 bytes) = Middle length field (4
    // bytes), End length field (4 bytes), type field (1 byte), ...
    final int middleLength = buf.readInt();
    final int endLength = buf.readInt();
    // check if the packet is complete
    if (middleLength + endLength + length - 8 > buf.readableBytes()) {
      buf.resetReaderIndex();
      return null;
    }
    // createPacketFromByteBuf read the buffer
    return LocalPacketFactory
        .createPacketFromByteBuf(length - 8, middleLength, endLength, buf);
  }

}
