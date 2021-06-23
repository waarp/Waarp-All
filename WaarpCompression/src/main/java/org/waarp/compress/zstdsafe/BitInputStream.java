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
package org.waarp.compress.zstdsafe;

import static org.waarp.compress.zstdsafe.Constants.*;
import static org.waarp.compress.zstdsafe.UnsafeUtil.*;
import static org.waarp.compress.zstdsafe.Util.*;

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

  public static boolean isEndOfStream(final int startAddress,
                                      final int currentAddress,
                                      final int bitsConsumed) {
    return startAddress == currentAddress && bitsConsumed == Long.SIZE;
  }

  static long readTail(final byte[] inputBase, final int inputAddress,
                       final int inputSize) {
    long bits = inputBase[inputAddress] & 0xFF;

    switch (inputSize) {
      case 7:
        bits |= (inputBase[inputAddress + 6] & 0xFFL) << 48;
      case 6:
        bits |= (inputBase[inputAddress + 5] & 0xFFL) << 40;
      case 5:
        bits |= (inputBase[inputAddress + 4] & 0xFFL) << 32;
      case 4:
        bits |= (inputBase[inputAddress + 3] & 0xFFL) << 24;
      case 3:
        bits |= (inputBase[inputAddress + 2] & 0xFFL) << 16;
      case 2:
        bits |= (inputBase[inputAddress + 1] & 0xFFL) << 8;
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
    private final byte[] inputBase;
    private final int startAddress;
    private final int endAddress;
    private long bits;
    private int currentAddress;
    private int bitsConsumed;

    public Initializer(final byte[] inputBase, final int startAddress,
                       final int endAddress) {
      this.inputBase = inputBase;
      this.startAddress = startAddress;
      this.endAddress = endAddress;
    }

    public long getBits() {
      return bits;
    }

    public int getCurrentAddress() {
      return currentAddress;
    }

    public int getBitsConsumed() {
      return bitsConsumed;
    }

    public void initialize() {
      verify(endAddress - startAddress >= 1, startAddress,
             "Bitstream is empty");

      final int lastByte = inputBase[endAddress - 1] & 0xFF;
      verify(lastByte != 0, endAddress, "Bitstream end mark not present");

      bitsConsumed = SIZE_OF_LONG - highestBit(lastByte);

      final int inputSize = endAddress - startAddress;
      if (inputSize >= SIZE_OF_LONG) {  /* normal case */
        currentAddress = endAddress - SIZE_OF_LONG;
        bits = getLong(inputBase, currentAddress);
      } else {
        currentAddress = startAddress;
        bits = readTail(inputBase, startAddress, inputSize);

        bitsConsumed += (SIZE_OF_LONG - inputSize) * 8;
      }
    }
  }

  static final class Loader {
    private final byte[] inputBase;
    private final int startAddress;
    private long bits;
    private int currentAddress;
    private int bitsConsumed;
    private boolean overflow;

    public Loader(final byte[] inputBase, final int startAddress,
                  final int currentAddress, final long bits,
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

    public int getCurrentAddress() {
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
      if (currentAddress >= startAddress + SIZE_OF_LONG) {
        if (bytes > 0) {
          currentAddress -= bytes;
          bits = getLong(inputBase, currentAddress);
        }
        bitsConsumed &= 0x7;
      } else if (currentAddress - bytes < startAddress) {
        bytes = currentAddress - startAddress;
        currentAddress = startAddress;
        bitsConsumed -= bytes * SIZE_OF_LONG;
        bits = getLong(inputBase, startAddress);
        return true;
      } else {
        currentAddress -= bytes;
        bitsConsumed -= bytes * SIZE_OF_LONG;
        bits = getLong(inputBase, currentAddress);
      }

      return false;
    }
  }
}
