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

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.waarp.compress.zstdunsafe;

/**
 * Bit streams are encoded as a byte-aligned little-endian stream. Thus, bits are laid out
 * in the following manner, and the stream is read from right to left.
 * <p>
 * <p>
 * ... [16 17 18 19 20 21 22 23] [8 9 10 11 12 13 14 15] [0 1 2 3 4 5 6 7]
 */
class BitInputStream {
  private BitInputStream() {
  }

  public static boolean isEndOfStream(final long startAddress,
                                      final long currentAddress,
                                      final int bitsConsumed) {
    return startAddress == currentAddress && bitsConsumed == Long.SIZE;
  }

  static long readTail(final Object inputBase, final long inputAddress,
                       final int inputSize) {
    long bits = UnsafeUtil.UNSAFE.getByte(inputBase, inputAddress) & 0xFF;

    switch (inputSize) {
      case 7:
        bits |=
            (UnsafeUtil.UNSAFE.getByte(inputBase, inputAddress + 6) & 0xFFL) <<
            48;
      case 6:
        bits |=
            (UnsafeUtil.UNSAFE.getByte(inputBase, inputAddress + 5) & 0xFFL) <<
            40;
      case 5:
        bits |=
            (UnsafeUtil.UNSAFE.getByte(inputBase, inputAddress + 4) & 0xFFL) <<
            32;
      case 4:
        bits |=
            (UnsafeUtil.UNSAFE.getByte(inputBase, inputAddress + 3) & 0xFFL) <<
            24;
      case 3:
        bits |=
            (UnsafeUtil.UNSAFE.getByte(inputBase, inputAddress + 2) & 0xFFL) <<
            16;
      case 2:
        bits |=
            (UnsafeUtil.UNSAFE.getByte(inputBase, inputAddress + 1) & 0xFFL) <<
            8;
    }

    return bits;
  }

  /**
   * @return numberOfBits in the low order bits of a long
   */
  public static long peekBits(final int bitsConsumed, final long bitContainer,
                              final int numberOfBits) {
    return (((bitContainer << bitsConsumed) >>> 1) >>> (63 - numberOfBits));
  }

  /**
   * numberOfBits must be > 0
   *
   * @return numberOfBits in the low order bits of a long
   */
  public static long peekBitsFast(final int bitsConsumed,
                                  final long bitContainer,
                                  final int numberOfBits) {
    return ((bitContainer << bitsConsumed) >>> (64 - numberOfBits));
  }

  static class Initializer {
    private final Object inputBase;
    private final long startAddress;
    private final long endAddress;
    private long bits;
    private long currentAddress;
    private int bitsConsumed;

    public Initializer(final Object inputBase, final long startAddress,
                       final long endAddress) {
      this.inputBase = inputBase;
      this.startAddress = startAddress;
      this.endAddress = endAddress;
    }

    public long getBits() {
      return bits;
    }

    public long getCurrentAddress() {
      return currentAddress;
    }

    public int getBitsConsumed() {
      return bitsConsumed;
    }

    public void initialize() {
      Util.verify(endAddress - startAddress >= 1, startAddress,
                  "Bitstream is empty");

      final int lastByte =
          UnsafeUtil.UNSAFE.getByte(inputBase, endAddress - 1) & 0xFF;
      Util.verify(lastByte != 0, endAddress, "Bitstream end mark not present");

      bitsConsumed = Constants.SIZE_OF_LONG - Util.highestBit(lastByte);

      final int inputSize = (int) (endAddress - startAddress);
      if (inputSize >= Constants.SIZE_OF_LONG) {  /* normal case */
        currentAddress = endAddress - Constants.SIZE_OF_LONG;
        bits = UnsafeUtil.UNSAFE.getLong(inputBase, currentAddress);
      } else {
        currentAddress = startAddress;
        bits = readTail(inputBase, startAddress, inputSize);

        bitsConsumed += (Constants.SIZE_OF_LONG - inputSize) * 8;
      }
    }
  }

  static final class Loader {
    private final Object inputBase;
    private final long startAddress;
    private long bits;
    private long currentAddress;
    private int bitsConsumed;
    private boolean overflow;

    public Loader(final Object inputBase, final long startAddress,
                  final long currentAddress, final long bits,
                  final int bitsConsumed) {
      this.inputBase = inputBase;
      this.startAddress = startAddress;
      this.bits = bits;
      this.currentAddress = currentAddress;
      this.bitsConsumed = bitsConsumed;
    }

    public long getBits() {
      return bits;
    }

    public long getCurrentAddress() {
      return currentAddress;
    }

    public int getBitsConsumed() {
      return bitsConsumed;
    }

    public boolean isOverflow() {
      return overflow;
    }

    public boolean load() {
      if (bitsConsumed > 64) {
        overflow = true;
        return true;
      } else if (currentAddress == startAddress) {
        return true;
      }

      int bytes = bitsConsumed >>> 3; // divide by 8
      if (currentAddress >= startAddress + Constants.SIZE_OF_LONG) {
        if (bytes > 0) {
          currentAddress -= bytes;
          bits = UnsafeUtil.UNSAFE.getLong(inputBase, currentAddress);
        }
        bitsConsumed &= 0x7;
      } else if (currentAddress - bytes < startAddress) {
        bytes = (int) (currentAddress - startAddress);
        currentAddress = startAddress;
        bitsConsumed -= bytes * Constants.SIZE_OF_LONG;
        bits = UnsafeUtil.UNSAFE.getLong(inputBase, startAddress);
        return true;
      } else {
        currentAddress -= bytes;
        bitsConsumed -= bytes * Constants.SIZE_OF_LONG;
        bits = UnsafeUtil.UNSAFE.getLong(inputBase, currentAddress);
      }

      return false;
    }
  }
}
