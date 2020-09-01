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

import static org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket.*;

/**
 * Factory to create Packet according to type from a buffer
 */
public final class LocalPacketFactory {
  public static final byte AUTHENTPACKET = 1;

  public static final byte STARTUPPACKET = 2;

  public static final byte DATAPACKET = 3;

  public static final byte VALIDPACKET = 4;

  public static final byte ERRORPACKET = 5;

  public static final byte CONNECTERRORPACKET = 6;

  public static final byte REQUESTPACKET = 7;

  public static final byte SHUTDOWNPACKET = 8;

  public static final byte STOPPACKET = 9;

  public static final byte CANCELPACKET = 10;

  public static final byte CONFEXPORTPACKET = 11;

  public static final byte CONFIMPORTPACKET = 12;

  public static final byte TESTPACKET = 13;

  public static final byte ENDTRANSFERPACKET = 14;

  public static final byte REQUESTUSERPACKET = 15;

  public static final byte LOGPACKET = 16;

  public static final byte LOGPURGEPACKET = 17;

  public static final byte INFORMATIONPACKET = 18;

  public static final byte BANDWIDTHPACKET = 19;

  public static final byte ENDREQUESTPACKET = 20;

  public static final byte KEEPALIVEPACKET = 21;
  // New Protocol message => Extended protocol
  public static final byte BUSINESSREQUESTPACKET = 22;

  public static final byte NOOPPACKET = 23;

  public static final byte BLOCKREQUESTPACKET = 24;

  public static final byte JSONREQUESTPACKET = 25;

  private LocalPacketFactory() {
  }

  /**
   * This method create a Packet from the ByteBuf.
   *
   * @param headerLength length of the header from the current
   *     position of
   *     the buffer
   * @param middleLength
   * @param endLength
   * @param buf
   *
   * @return the newly created Packet
   *
   * @throws OpenR66ProtocolPacketException
   */
  public static AbstractLocalPacket createPacketFromByteBuf(
      final int headerLength, final int middleLength, final int endLength,
      final ByteBuf buf) throws OpenR66ProtocolPacketException {
    final byte packetType = buf.readByte();
    switch (packetType) {
      case AUTHENTPACKET:
        return AuthentPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case STARTUPPACKET:
        return StartupPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case DATAPACKET:
        return DataPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case VALIDPACKET:
        return ValidPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case ERRORPACKET:
        return ErrorPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case CONNECTERRORPACKET:
        return ConnectionErrorPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case REQUESTPACKET:
        return RequestPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case SHUTDOWNPACKET:
        return ShutdownPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case STOPPACKET:
      case CANCELPACKET:
      case REQUESTUSERPACKET:
      case LOGPACKET:
      case LOGPURGEPACKET:
      case CONFEXPORTPACKET:
      case CONFIMPORTPACKET:
      case BANDWIDTHPACKET:
        throw new OpenR66ProtocolPacketException(
            "Unimplemented Packet Type received: " + packetType);
      case TESTPACKET:
        return TestPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case ENDTRANSFERPACKET:
        return EndTransferPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case INFORMATIONPACKET:
        return InformationPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case ENDREQUESTPACKET:
        return EndRequestPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case KEEPALIVEPACKET:
        return KeepAlivePacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case BUSINESSREQUESTPACKET:
        return BusinessRequestPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case NOOPPACKET:
        return NoOpPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case BLOCKREQUESTPACKET:
        return BlockRequestPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      case JSONREQUESTPACKET:
        return JsonCommandPacket
            .createFromBuffer(headerLength, middleLength, endLength, buf);
      default:
        throw new OpenR66ProtocolPacketException(
            "Unvalid Packet Type received: " + packetType);
    }
  }

  public static final int estimateSize(final Object o) {
    if (!(o instanceof AbstractLocalPacket)) {
      // Type unimplemented
      return -1;
    }
    final AbstractLocalPacket packet = (AbstractLocalPacket) o;
    return packet.header.readableBytes() + packet.middle.readableBytes() +
           packet.end.readableBytes() + LOCAL_HEADER_SIZE;
  }

}
