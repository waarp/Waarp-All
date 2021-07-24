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

class FseTableReader {
  private final short[] nextSymbol =
      new short[FiniteStateEntropy.MAX_SYMBOL + 1];
  private final short[] normalizedCounters =
      new short[FiniteStateEntropy.MAX_SYMBOL + 1];

  public int readFseTable(final FiniteStateEntropy.Table table,
                          final Object inputBase, final long inputAddress,
                          final long inputLimit, int maxSymbol,
                          final int maxTableLog) {
    // read table headers
    long input = inputAddress;
    Util.verify(inputLimit - inputAddress >= 4, input,
                "Not enough input bytes");

    int threshold;
    int symbolNumber = 0;
    boolean previousIsZero = false;

    int bitStream = UnsafeUtil.UNSAFE.getInt(inputBase, input);

    final int tableLog = (bitStream & 0xF) + FiniteStateEntropy.MIN_TABLE_LOG;

    int numberOfBits = tableLog + 1;
    bitStream >>>= 4;
    int bitCount = 4;

    Util.verify(tableLog <= maxTableLog, input,
                "FSE table size exceeds maximum allowed size");

    int remaining = (1 << tableLog) + 1;
    threshold = 1 << tableLog;

    while (remaining > 1 && symbolNumber <= maxSymbol) {
      if (previousIsZero) {
        int n0 = symbolNumber;
        while ((bitStream & 0xFFFF) == 0xFFFF) {
          n0 += 24;
          if (input < inputLimit - 5) {
            input += 2;
            bitStream =
                (UnsafeUtil.UNSAFE.getInt(inputBase, input) >>> bitCount);
          } else {
            // end of bit stream
            bitStream >>>= 16;
            bitCount += 16;
          }
        }
        while ((bitStream & 3) == 3) {
          n0 += 3;
          bitStream >>>= 2;
          bitCount += 2;
        }
        n0 += bitStream & 3;
        bitCount += 2;

        Util.verify(n0 <= maxSymbol, input, "Symbol larger than max value");

        while (symbolNumber < n0) {
          normalizedCounters[symbolNumber++] = 0;
        }
        if ((input <= inputLimit - 7) ||
            (input + (bitCount >>> 3) <= inputLimit - 4)) {
          input += bitCount >>> 3;
          bitCount &= 7;
          bitStream = UnsafeUtil.UNSAFE.getInt(inputBase, input) >>> bitCount;
        } else {
          bitStream >>>= 2;
        }
      }

      final short max = (short) ((2 * threshold - 1) - remaining);
      short count;

      if ((bitStream & (threshold - 1)) < max) {
        count = (short) (bitStream & (threshold - 1));
        bitCount += numberOfBits - 1;
      } else {
        count = (short) (bitStream & (2 * threshold - 1));
        if (count >= threshold) {
          count -= max;
        }
        bitCount += numberOfBits;
      }
      count--;  // extra accuracy

      remaining -= Math.abs(count);
      normalizedCounters[symbolNumber++] = count;
      previousIsZero = count == 0;
      while (remaining < threshold) {
        numberOfBits--;
        threshold >>>= 1;
      }

      if ((input <= inputLimit - 7) ||
          (input + (bitCount >> 3) <= inputLimit - 4)) {
        input += bitCount >>> 3;
        bitCount &= 7;
      } else {
        bitCount -= (int) (8 * (inputLimit - 4 - input));
        input = inputLimit - 4;
      }
      bitStream =
          UnsafeUtil.UNSAFE.getInt(inputBase, input) >>> (bitCount & 31);
    }

    Util.verify(remaining == 1 && bitCount <= 32, input, "Input is corrupted");

    maxSymbol = symbolNumber - 1;
    Util.verify(maxSymbol <= FiniteStateEntropy.MAX_SYMBOL, input,
                "Max symbol value too large (too many symbols for FSE)");

    input += (bitCount + 7) >> 3;

    // populate decoding table
    final int symbolCount = maxSymbol + 1;
    final int tableSize = 1 << tableLog;
    int highThreshold = tableSize - 1;

    table.log2Size = tableLog;

    for (byte symbol = 0; symbol < symbolCount; symbol++) {
      if (normalizedCounters[symbol] == -1) {
        table.symbol[highThreshold--] = symbol;
        nextSymbol[symbol] = 1;
      } else {
        nextSymbol[symbol] = normalizedCounters[symbol];
      }
    }

    final int position =
        FseCompressionTable.spreadSymbols(normalizedCounters, maxSymbol,
                                          tableSize, highThreshold,
                                          table.symbol);

    // position must reach all cells once, otherwise normalizedCounter is incorrect
    Util.verify(position == 0, input, "Input is corrupted");

    for (int i = 0; i < tableSize; i++) {
      final byte symbol = table.symbol[i];
      final short nextState = nextSymbol[symbol]++;
      table.numberOfBits[i] = (byte) (tableLog - Util.highestBit(nextState));
      table.newState[i] =
          (short) ((nextState << table.numberOfBits[i]) - tableSize);
    }

    return (int) (input - inputAddress);
  }

  public static void initializeRleTable(final FiniteStateEntropy.Table table,
                                        final byte value) {
    table.log2Size = 0;
    table.symbol[0] = value;
    table.newState[0] = 0;
    table.numberOfBits[0] = 0;
  }
}
