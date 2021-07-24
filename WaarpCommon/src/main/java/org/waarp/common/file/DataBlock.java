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
package org.waarp.common.file;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Main object implementing Data Block whaveter the mode, type, structure used.
 */
public class DataBlock {
  private static final int EOR = 128;

  private static final int EOF = 64;

  private static final int ERROR = 32;

  private static final int RESTART = 16;

  /**
   * Descriptor
   */
  private int descriptor;

  /**
   * Byte Count
   */
  private int byteCount = -1;

  /**
   * Markers
   */
  private int[] markers;

  /**
   * Byte Array
   */
  private byte[] block;
  private ByteBuf blockBuf;
  private int offsetBuf;

  /**
   * is EOF
   */
  private boolean isEOF;

  /**
   * is EOR
   */
  private boolean isEOR;

  /**
   * is in ERROR (should not be used)
   */
  private boolean isERROR;

  /**
   * is a MARKER RESTART
   */
  private boolean isRESTART;

  /**
   * Create a simple and empty DataBlock
   */
  public DataBlock() {
    // Empty
  }

  /**
   * @return the block
   *
   * @deprecated method, prefer getByteBlock()
   */
  @Deprecated
  public final ByteBuf getBlock() {
    if (blockBuf == null) {
      blockBuf = Unpooled.wrappedBuffer(block);
      offsetBuf = 0;
    }
    return blockBuf;
  }

  /**
   * @return the block
   */
  public final byte[] getByteBlock() {
    return block;
  }

  /**
   * @return the offset of the ByteBlock
   */
  public final int getOffset() {
    return offsetBuf;
  }

  /**
   * Increase the offset position
   *
   * @param offset
   */
  public final void addOffset(final int offset) {
    offsetBuf += offset;
  }

  /**
   * Set the block and the byte count according to the block
   *
   * @param block the block to set
   */
  public final void setBlock(final ByteBuf block) {
    if (isRESTART) {
      this.block = null;
      markers = new int[6];
      for (int i = 0; i < 6; i++) {
        markers[i] = block.readByte();
      }
      byteCount = 6;
      return;
    }
    byteCount = block.readableBytes();
    this.block = new byte[byteCount];
    offsetBuf = 0;
    if (blockBuf != null) {
      blockBuf.release();
      blockBuf = null;
    }
    block.readBytes(this.block);
    block.release();
  }

  /**
   * Set the block and the byte count according to the block
   *
   * @param block the block to set
   */
  public final void setBlock(final byte[] block) {
    setBlock(block, block != null? block.length : 0);
  }

  /**
   * Set the block and the byte count
   *
   * @param block the block to set
   * @param size the real size to set
   */
  public final void setBlock(final byte[] block, final int size) {
    if (isRESTART) {
      this.block = null;
      markers = new int[6];
      for (int i = 0; i < 6; i++) {
        markers[i] = block[i];
      }
      byteCount = 6;
      return;
    }
    this.block = block;
    if (this.block == null) {
      byteCount = 0;
    } else {
      byteCount = size;
    }
    if (blockBuf != null) {
      blockBuf.release();
      blockBuf = null;
    }
    offsetBuf = 0;
  }

  /**
   * @return the byteCount
   */
  public final int getByteCount() {
    return byteCount - offsetBuf;
  }

  /**
   * @param byteCount the byteCount to set
   */
  public final void setByteCount(final int byteCount) {
    this.byteCount = byteCount;
  }

  /**
   * @param upper upper byte of the 2 bytes length
   * @param lower lower byte of the 2 bytes length
   */
  public final void setByteCount(final byte upper, final byte lower) {
    byteCount = upper << 8 | (lower & 0xFF);
  }

  /**
   * @return the Upper byte of the byte count
   */
  public final byte getByteCountUpper() {
    return (byte) (byteCount >> 8 & 0xFF);
  }

  /**
   * @return the Lower byte of the byte count
   */
  public final byte getByteCountLower() {
    return (byte) (byteCount & 0xFF);
  }

  /**
   * @return the descriptor
   */
  public final byte getDescriptor() {
    return (byte) (descriptor & 0xFF);
  }

  /**
   * @param descriptor the descriptor to set
   */
  public final void setDescriptor(final int descriptor) {
    this.descriptor = descriptor & 0xFF;
    isEOF = (this.descriptor & EOF) != 0;
    isEOR = (this.descriptor & EOR) != 0;
    isERROR = (this.descriptor & ERROR) != 0;
    isRESTART = (this.descriptor & RESTART) != 0;
  }

  /**
   * @return the isEOF
   */
  public final boolean isEOF() {
    return isEOF;
  }

  /**
   * @param isEOF the isEOF to set
   */
  public final void setEOF(final boolean isEOF) {
    this.isEOF = isEOF;
    descriptor |= EOF;
  }

  /**
   * @return the isEOR
   */
  public final boolean isEOR() {
    return isEOR;
  }

  /**
   * @param isEOR the isEOR to set
   */
  public final void setEOR(final boolean isEOR) {
    this.isEOR = isEOR;
    descriptor |= EOR;
  }

  /**
   * @return the isERROR
   */
  public final boolean isERROR() {
    return isERROR;
  }

  /**
   * @param isERROR the isERROR to set
   */
  public final void setERROR(final boolean isERROR) {
    this.isERROR = isERROR;
    descriptor |= ERROR;
  }

  /**
   * @return the isRESTART
   */
  public final boolean isRESTART() {
    return isRESTART;
  }

  /**
   * @param isRESTART the isRESTART to set
   */
  public final void setRESTART(final boolean isRESTART) {
    this.isRESTART = isRESTART;
    descriptor |= RESTART;
  }

  /**
   * @return the markers
   */
  public final int[] getMarkers() {
    return markers;
  }

  /**
   * @return the 6 bytes representation of the markers
   */
  public final byte[] getByteMarkers() {
    final byte[] bmarkers = new byte[6];
    if (markers == null) {
      for (int i = 0; i < 6; i++) {
        bmarkers[i] = 0;
      }
    } else {
      for (int i = 0; i < 6; i++) {
        bmarkers[i] = (byte) (markers[i] & 0xFF);
      }
    }
    return bmarkers;
  }

  /**
   * Set the markers and the byte count
   *
   * @param markers the markers to set
   */
  public final void setMarkers(final int[] markers) {
    this.markers = markers;
    byteCount = 6;
  }

  /**
   * Clear the object
   */
  public final void clear() {
    if (blockBuf != null) {
      blockBuf.release();
      blockBuf = null;
    }
    block = null;
    byteCount = -1;
    descriptor = 0;
    isEOF = false;
    isEOR = false;
    isERROR = false;
    isRESTART = false;
    markers = null;
  }

  /**
   * Is this Block cleared
   *
   * @return True if this Block is cleared
   */
  public final boolean isCleared() {
    return byteCount == -1;
  }

  @Override
  public String toString() {
    return "DataBlock Length:" + byteCount + " isEof:" + isEOF + " isEOR:" +
           isEOR + " isERROR:" + isERROR + " isRESTART:" + isRESTART;
  }

  /**
   * Translate the given array of byte into a string in binary format
   *
   * @param bytes
   * @param cutted True if each Byte should be 'blank' separated or
   *     not
   *
   * @return the string
   */
  public static String toBinaryString(final byte[] bytes,
                                      final boolean cutted) {
    final StringBuilder buffer = new StringBuilder();
    boolean first = true;
    for (final byte b : bytes) {
      if (cutted) {
        if (first) {
          first = false;
        } else {
          buffer.append(' ');
        }
      }
      String bin = Integer.toBinaryString(b & 0xFF);
      bin = bin.substring(0, Math.min(bin.length(), 8));
      for (int j = 0; j < 8 - bin.length(); j++) {
        buffer.append('0');
      }
      buffer.append(bin);
    }
    return buffer.toString();
  }
}
