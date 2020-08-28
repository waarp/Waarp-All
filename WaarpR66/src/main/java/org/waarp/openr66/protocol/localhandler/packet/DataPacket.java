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
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.WaarpNettyUtil;
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

  private final int lengthPacket;

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
  public static DataPacket createFromBuffer(int headerLength, int middleLength,
                                            int endLength, ByteBuf buf)
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
  private DataPacket(int packetRank, ByteBuf data, byte[] key) {
    this.packetRank = packetRank;
    this.dataRecv = data;
    this.data = null;
    this.key = key == null? EMPTY_ARRAY : key;
    lengthPacket = dataRecv.readableBytes();
  }

  /**
   * @param packetRank
   * @param data
   * @param key
   */
  public DataPacket(int packetRank, byte[] data, byte[] key) {
    this.packetRank = packetRank;
    this.data = data;
    this.dataRecv = null;
    this.key = key == null? EMPTY_ARRAY : key;
    lengthPacket = data.length;
  }

  @Override
  public boolean hasGlobalBuffer() {
    return false;
  }

  @Override
  public void createAllBuffers(LocalChannelReference lcr, int networkHeader)
      throws OpenR66ProtocolPacketException {
    throw new IllegalStateException("Should not be called");
  }

  @Override
  public void createEnd(LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    end = Unpooled.wrappedBuffer(key);
  }

  @Override
  public void createHeader(LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    header = ByteBufAllocator.DEFAULT.buffer(4, 4);
    header.writeInt(packetRank);
  }

  @Override
  public void createMiddle(LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    if (dataRecv != null) {
      middle = dataRecv;
    } else {
      middle = Unpooled.wrappedBuffer(data);
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
   * Only for Network incoming buffer
   *
   * @return the Data ByteBuf
   */
  public ByteBuf getRecvData() {
    return dataRecv;
  }

  public void createByteBufFromRecv(byte[] buffer) {
    dataRecv.getBytes(dataRecv.readerIndex(), buffer);
    data = buffer;
  }

  /**
   * @return the data
   */
  public byte[] getData() {
    ParametersChecker.checkParameter("Data is not setup correctly", data);
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
  public boolean isKeyValid(DigestAlgo algo) {
    ParametersChecker.checkParameter("Data is not setup correctly", data);
    if (key == null || key.length == 0) {
      logger.error("Should received a Digest but don't");
      return false;
    }
    final byte[] newkey = FileUtils.getHash(data, algo, null);
    final boolean equal = Arrays.equals(key, newkey);
    if (!equal) {
      logger.error("DIGEST {} != {} for {} bytes using {} at rank {}",
                   FilesystemBasedDigest.getHex(key),
                   FilesystemBasedDigest.getHex(newkey), data.length, algo,
                   packetRank);
    }
    return equal;
  }

  /**
   * @return True if the Hashed key is valid (or no key is set)
   */
  public boolean isKeyValid(DigestAlgo algo, FilesystemBasedDigest digestGlobal,
                            FilesystemBasedDigest digestLocal) {
    ParametersChecker.checkParameter("Data is not setup correctly", data);
    if (key == null || key.length == 0) {
      if (digestGlobal != null || digestLocal != null) {
        FileUtils.computeGlobalHash(digestGlobal, digestLocal, data);
      }
      logger.error("Should received a Digest but don't");
      return false;
    }
    final byte[] newkey;
    if (digestGlobal != null && digestLocal != null) {
      newkey = FileUtils.getHash(data, algo, null);
      FileUtils.computeGlobalHash(digestGlobal, digestLocal, data);
    } else if (digestGlobal == null && digestLocal == null) {
      newkey = FileUtils.getHash(data, algo, null);
    } else if (digestGlobal != null) {
      newkey = FileUtils.getHash(data, algo, digestGlobal);
    } else {
      newkey = FileUtils.getHash(data, algo, digestLocal);
    }
    final boolean equal = Arrays.equals(key, newkey);
    if (!equal) {
      logger.error("DIGEST {} != {} for {} bytes using {} at rank {}",
                   FilesystemBasedDigest.getHex(key),
                   FilesystemBasedDigest.getHex(newkey), data.length, algo,
                   packetRank);
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
  }
}
