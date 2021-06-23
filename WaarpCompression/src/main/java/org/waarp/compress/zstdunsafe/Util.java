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

import org.waarp.compress.MalformedInputException;

import static org.waarp.compress.zstdunsafe.Constants.*;

final class Util {
  private Util() {
  }

  public static int highestBit(final int value) {
    return 31 - Integer.numberOfLeadingZeros(value);
  }

  public static boolean isPowerOf2(final int value) {
    return (value & (value - 1)) == 0;
  }

  public static int mask(final int bits) {
    return (1 << bits) - 1;
  }

  public static void verify(final boolean condition, final long offset,
                            final String reason) {
    if (!condition) {
      throw new MalformedInputException(offset, reason);
    }
  }

  public static void checkArgument(final boolean condition,
                                   final String reason) {
    if (!condition) {
      throw new IllegalArgumentException(reason);
    }
  }

  public static void checkState(final boolean condition, final String reason) {
    if (!condition) {
      throw new IllegalStateException(reason);
    }
  }

  public static MalformedInputException fail(final long offset,
                                             final String reason) {
    throw new MalformedInputException(offset, reason);
  }

  public static int cycleLog(final int hashLog,
                             final CompressionParameters.Strategy strategy) {
    int cycleLog = hashLog;
    if (strategy == CompressionParameters.Strategy.BTLAZY2 ||
        strategy == CompressionParameters.Strategy.BTOPT ||
        strategy == CompressionParameters.Strategy.BTULTRA) {
      cycleLog = hashLog - 1;
    }
    return cycleLog;
  }

  public static void put24BitLittleEndian(final Object outputBase,
                                          final long outputAddress,
                                          final int value) {
    UnsafeUtil.UNSAFE.putShort(outputBase, outputAddress, (short) value);
    UnsafeUtil.UNSAFE.putByte(outputBase, outputAddress + SIZE_OF_SHORT,
                              (byte) (value >>> Short.SIZE));
  }

  // provides the minimum logSize to safely represent a distribution
  public static int minTableLog(final int inputSize, final int maxSymbolValue) {
    if (inputSize <= 1) {
      throw new IllegalArgumentException(
          "Not supported. RLE should be used instead"); // TODO
    }

    final int minBitsSrc = highestBit((inputSize - 1)) + 1;
    final int minBitsSymbols = highestBit(maxSymbolValue) + 2;
    return Math.min(minBitsSrc, minBitsSymbols);
  }
}
