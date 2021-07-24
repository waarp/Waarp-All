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
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;

/**
 * Information of files class
 * <p>
 * header = "rulename" middle = requestedInfo end = "FILENAME"
 */
public class InformationPacket extends AbstractLocalPacket {

  private static final String NOT_ENOUGH_DATA = "Not enough data";

  public enum ASKENUM {
    ASKEXIST, ASKMLSDETAIL, ASKLIST, ASKMLSLIST
  }

  private final String rulename;

  private final byte requestedInfo;

  private final String filename;

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
  public static InformationPacket createFromBuffer(final int headerLength,
                                                   final int middleLength,
                                                   final int endLength,
                                                   final ByteBuf buf)
      throws OpenR66ProtocolPacketException {
    if (headerLength - 1 <= 0) {
      throw new OpenR66ProtocolPacketException(NOT_ENOUGH_DATA);
    }
    if (middleLength != 1) {
      throw new OpenR66ProtocolPacketException(NOT_ENOUGH_DATA);
    }
    final byte[] bheader = new byte[headerLength - 1];
    final byte[] bend = new byte[endLength];
    buf.readBytes(bheader);
    final byte request = buf.readByte();
    if (endLength > 0) {
      buf.readBytes(bend);
    }
    final String sheader = new String(bheader, WaarpStringUtils.UTF8);
    final String send = new String(bend, WaarpStringUtils.UTF8);
    return new InformationPacket(sheader, request, send);
  }

  /**
   * @param rulename
   * @param request
   * @param filename
   */
  public InformationPacket(final String rulename, final byte request,
                           final String filename) {
    this.rulename = rulename;
    requestedInfo = request;
    this.filename = filename;
  }

  @Override
  public final boolean hasGlobalBuffer() {
    return true;
  }

  @Override
  public final void createAllBuffers(final LocalChannelReference lcr,
                                     final int networkHeader)
      throws OpenR66ProtocolPacketException {
    if (rulename == null) {
      throw new OpenR66ProtocolPacketException(NOT_ENOUGH_DATA);
    }
    final byte[] headerBytes = rulename.getBytes(WaarpStringUtils.UTF8);
    final int headerSize = headerBytes.length;
    final int middleSize = 1;
    final byte[] endBytes =
        filename != null? filename.getBytes(WaarpStringUtils.UTF8) :
            EMPTY_ARRAY;
    final int endSize = endBytes.length;
    final int globalSize =
        networkHeader + LOCAL_HEADER_SIZE + headerSize + middleSize + endSize;
    int offset = networkHeader + LOCAL_HEADER_SIZE;
    global = ByteBufAllocator.DEFAULT.ioBuffer(globalSize, globalSize);
    header = WaarpNettyUtil.slice(global, offset, headerSize);
    header.writeBytes(headerBytes);
    offset += headerSize;
    middle = WaarpNettyUtil.slice(global, offset, middleSize);
    middle.writeByte(requestedInfo);
    offset += middleSize;
    end = WaarpNettyUtil.slice(global, offset, endSize);
    if (filename != null) {
      end.writeBytes(endBytes);
    }
  }

  @Override
  public final byte getType() {
    return LocalPacketFactory.INFORMATIONPACKET;
  }

  @Override
  public final String toString() {
    return "InformationPacket: " + requestedInfo + ' ' + rulename + ' ' +
           filename;
  }

  /**
   * @return the requestId
   */
  public final byte getRequest() {
    return requestedInfo;
  }

  /**
   * @return the rulename
   */
  public final String getRulename() {
    return rulename;
  }

  /**
   * @return the filename
   */
  public final String getFilename() {
    if (filename != null) {
      return filename;
    } else {
      return "";
    }
  }
}
