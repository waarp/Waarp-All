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
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.utils.FileUtils;

import java.util.Arrays;

/**
 * Data packet
 * <p>
 * header = packetRank middle = data end = key
 */
public class DataPacket extends AbstractLocalPacket {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DataPacket.class);

  private final int packetRank;

  private int lengthPacket;

  private byte[] data;
  private ByteBuf dataRecv;

  private byte[] key;

  /**
   * @param headerLength
   * @param middleLength
   * @param endLength
   * @param buf
   *
   * @return the new DataPacket from buffer
   *
   * @throws OpenR66ProtocolPacketException
   */
  public static DataPacket createFromBuffer(final int headerLength,
                                            final int middleLength,
                                            final int endLength,
                                            final ByteBuf buf)
      throws OpenR66ProtocolPacketException {
    if (headerLength - 1 <= 0) {
      throw new OpenR66ProtocolPacketException("Not enough data");
    }
    if (middleLength <= 0) {
      throw new OpenR66ProtocolPacketException("Not enough data");
    }
    final int packetRank = buf.readInt();
    final int index = buf.readerIndex();
    final ByteBuf recvData = buf.retainedSlice(index, middleLength);
    buf.skipBytes(middleLength);
    final byte[] key = endLength > 0? new byte[endLength] : EMPTY_ARRAY;
    if (endLength > 0) {
      buf.readBytes(key);
    }
    return new DataPacket(packetRank, recvData, key);
  }

  /**
   * @param packetRank
   * @param data
   * @param key
   */
  private DataPacket(final int packetRank, final ByteBuf data,
                     final byte[] key) {
    this.packetRank = packetRank;
    this.dataRecv = data;
    this.data = null;
    this.key = key == null? EMPTY_ARRAY : key;
    lengthPacket = dataRecv.readableBytes();
  }

  /**
   * @param packetRank
   * @param data
   * @param length
   * @param key
   */
  public DataPacket(final int packetRank, final byte[] data, final int length,
                    final byte[] key) {
    this.packetRank = packetRank;
    this.data = data;
    this.dataRecv = null;
    this.key = key == null? EMPTY_ARRAY : key;
    lengthPacket = length;
  }

  /**
   * When using compression/decompression, data can be changed
   *
   * @param data
   * @param length
   */
  public void updateFromCompressionCodec(final byte[] data, final int length) {
    this.data = data;
    lengthPacket = length;
  }

  @Override
  public boolean hasGlobalBuffer() {
    return false;
  }

  @Override
  public void createAllBuffers(final LocalChannelReference lcr,
                               final int networkHeader) {
    throw new IllegalStateException("Should not be called");
  }

  @Override
  public void createEnd(final LocalChannelReference lcr) {
    end = WaarpNettyUtil.wrappedBuffer(key);
  }

  @Override
  public void createHeader(final LocalChannelReference lcr) {
    header = ByteBufAllocator.DEFAULT.ioBuffer(4, 4);
    header.writeInt(packetRank);
  }

  @Override
  public void createMiddle(final LocalChannelReference lcr) {
    if (dataRecv != null) {
      middle = dataRecv;
    } else {
      middle = WaarpNettyUtil.wrappedBuffer(data);
      middle.writerIndex(lengthPacket);
    }
  }

  @Override
  public byte getType() {
    return LocalPacketFactory.DATAPACKET;
  }

  @Override
  public String toString() {
    return "DataPacket: " + packetRank + ':' + lengthPacket;
  }

  /**
   * @return the packetRank
   */
  public int getPacketRank() {
    return packetRank;
  }

  /**
   * @return the lengthPacket
   */
  public int getLengthPacket() {
    return lengthPacket;
  }

  /**
   * Transform the DataPacket to have a byte array instead of ByteBuf
   *
   * @param session to allow to get reusable buffer
   */
  public void createByteBufFromRecv(final R66Session session) {
    // Get reusable buffer and set internal content to byte Array
    if (data == null) {
      final byte[] buffer = session.getReusableDataPacketBuffer(lengthPacket);
      dataRecv.getBytes(dataRecv.readerIndex(), buffer, 0, lengthPacket);
      data = buffer;
      dataRecv.release();
      dataRecv = null;
    }
  }

  /**
   * @return the data
   */
  public byte[] getData() {
    ParametersChecker
        .checkParameter("Data is not setup correctly", data, logger);
    return data;
  }

  /**
   * @return the key
   */
  public byte[] getKey() {
    return key;
  }

  /**
   * @return True if the Hashed key is valid (or no key is set)
   */
  public boolean isKeyValid(final FilesystemBasedDigest digestBlock,
                            final FilesystemBasedDigest digestGlobal,
                            final FilesystemBasedDigest digestLocal) {
    ParametersChecker
        .checkParameter("Data is not setup correctly", data, logger);
    if (key == null || key.length == 0) {
      if (digestGlobal != null || digestLocal != null) {
        FileUtils
            .computeGlobalHash(digestGlobal, digestLocal, data, lengthPacket);
      }
      logger.error("Should received a Digest but don't");
      return false;
    }
    digestBlock.Update(data, 0, lengthPacket);
    final byte[] newkey = digestBlock.Final();
    FileUtils.computeGlobalHash(digestGlobal, digestLocal, data, lengthPacket);
    final boolean equal = Arrays.equals(key, newkey);
    if (!equal) {
      logger.error("DIGEST {} != {} for {} bytes using {} at rank {}",
                   FilesystemBasedDigest.getHex(key),
                   FilesystemBasedDigest.getHex(newkey), lengthPacket,
                   digestBlock.getAlgo(), packetRank);
    } else if (logger.isDebugEnabled()) {
      logger.debug("DIGEST {} == {} for {} bytes using {} at rank {}",
                   FilesystemBasedDigest.getHex(key),
                   FilesystemBasedDigest.getHex(newkey), lengthPacket,
                   digestBlock.getAlgo(), packetRank);
    }
    return equal;
  }

  @Override
  public void clear() {
    super.clear();
    WaarpNettyUtil.release(dataRecv);
    dataRecv = null;
    data = null;
    key = null;
    lengthPacket = 0;
  }
}
