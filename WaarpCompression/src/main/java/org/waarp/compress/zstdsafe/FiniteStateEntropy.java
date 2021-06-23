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

import static org.waarp.compress.zstdsafe.BitInputStream.*;
import static org.waarp.compress.zstdsafe.Constants.*;
import static org.waarp.compress.zstdsafe.UnsafeUtil.*;
import static org.waarp.compress.zstdsafe.Util.*;

class FiniteStateEntropy {
  public static final int MAX_SYMBOL = 255;
  public static final int MAX_TABLE_LOG = 12;
  public static final int MIN_TABLE_LOG = 5;

  private static final int[] REST_TO_BEAT =
      new int[] { 0, 473195, 504333, 520860, 550000, 700000, 750000, 830000 };
  private static final short UNASSIGNED = -2;
  public static final String OUTPUT_BUFFER_TOO_SMALL =
      "Output buffer too small";

  private FiniteStateEntropy() {
  }

  public static int decompress(final FiniteStateEntropy.Table table,
                               final byte[] inputBase, final int inputAddress,
                               final int inputLimit,
                               final byte[] outputBuffer) {
    final int outputAddress = 0;
    final int outputLimit = outputAddress + outputBuffer.length;

    int output = outputAddress;

    // initialize bit stream
    final BitInputStream.Initializer initializer =
        new BitInputStream.Initializer(inputBase, inputAddress, inputLimit);
    initializer.initialize();
    int bitsConsumed = initializer.getBitsConsumed();
    int currentAddress = initializer.getCurrentAddress();
    long bits = initializer.getBits();

    // initialize first FSE stream
    int state1 = (int) peekBits(bitsConsumed, bits, table.log2Size);
    bitsConsumed += table.log2Size;

    BitInputStream.Loader loader =
        new BitInputStream.Loader(inputBase, inputAddress, currentAddress, bits,
                                  bitsConsumed);
    loader.load();
    bits = loader.getBits();
    bitsConsumed = loader.getBitsConsumed();
    currentAddress = loader.getCurrentAddress();

    // initialize second FSE stream
    int state2 = (int) peekBits(bitsConsumed, bits, table.log2Size);
    bitsConsumed += table.log2Size;

    loader =
        new BitInputStream.Loader(inputBase, inputAddress, currentAddress, bits,
                                  bitsConsumed);
    loader.load();
    bits = loader.getBits();
    bitsConsumed = loader.getBitsConsumed();
    currentAddress = loader.getCurrentAddress();

    final byte[] symbols = table.symbol;
    final byte[] numbersOfBits = table.numberOfBits;
    final int[] newStates = table.newState;

    // decode 4 symbols per loop
    while (output <= outputLimit - 4) {
      int numberOfBits;

      outputBuffer[output] = symbols[state1];
      numberOfBits = numbersOfBits[state1];
      state1 = (int) (newStates[state1] +
                      peekBits(bitsConsumed, bits, numberOfBits));
      bitsConsumed += numberOfBits;

      outputBuffer[output + 1] = symbols[state2];
      numberOfBits = numbersOfBits[state2];
      state2 = (int) (newStates[state2] +
                      peekBits(bitsConsumed, bits, numberOfBits));
      bitsConsumed += numberOfBits;

      outputBuffer[output + 2] = symbols[state1];
      numberOfBits = numbersOfBits[state1];
      state1 = (int) (newStates[state1] +
                      peekBits(bitsConsumed, bits, numberOfBits));
      bitsConsumed += numberOfBits;

      outputBuffer[output + 3] = symbols[state2];
      numberOfBits = numbersOfBits[state2];
      state2 = (int) (newStates[state2] +
                      peekBits(bitsConsumed, bits, numberOfBits));
      bitsConsumed += numberOfBits;

      output += SIZE_OF_INT;

      loader =
          new BitInputStream.Loader(inputBase, inputAddress, currentAddress,
                                    bits, bitsConsumed);
      final boolean done = loader.load();
      bitsConsumed = loader.getBitsConsumed();
      bits = loader.getBits();
      currentAddress = loader.getCurrentAddress();
      if (done) {
        break;
      }
    }

    while (true) {
      verify(output <= outputLimit - 2, inputAddress,
             "Output buffer is too small");
      outputBuffer[output++] = symbols[state1];
      final int numberOfBits = numbersOfBits[state1];
      state1 = (int) (newStates[state1] +
                      peekBits(bitsConsumed, bits, numberOfBits));
      bitsConsumed += numberOfBits;

      loader =
          new BitInputStream.Loader(inputBase, inputAddress, currentAddress,
                                    bits, bitsConsumed);
      loader.load();
      bitsConsumed = loader.getBitsConsumed();
      bits = loader.getBits();
      currentAddress = loader.getCurrentAddress();

      if (loader.isOverflow()) {
        outputBuffer[output++] = symbols[state2];
        break;
      }

      verify(output <= outputLimit - 2, inputAddress,
             "Output buffer is too small");
      outputBuffer[output++] = symbols[state2];
      final int numberOfBits1 = numbersOfBits[state2];
      state2 = (int) (newStates[state2] +
                      peekBits(bitsConsumed, bits, numberOfBits1));
      bitsConsumed += numberOfBits1;

      loader =
          new BitInputStream.Loader(inputBase, inputAddress, currentAddress,
                                    bits, bitsConsumed);
      loader.load();
      bitsConsumed = loader.getBitsConsumed();
      bits = loader.getBits();
      currentAddress = loader.getCurrentAddress();

      if (loader.isOverflow()) {
        outputBuffer[output++] = symbols[state1];
        break;
      }
    }

    return output - outputAddress;
  }

  public static int compress(final byte[] outputBase, final int outputAddress,
                             final int outputSize, final byte[] input,
                             final int inputSize,
                             final FseCompressionTable table) {
    return compress(outputBase, outputAddress, outputSize, input, 0, inputSize,
                    table);
  }

  public static int compress(final byte[] outputBase, final int outputAddress,
                             final int outputSize, final byte[] inputBase,
                             final int inputAddress, int inputSize,
                             final FseCompressionTable table) {
    checkArgument(outputSize >= SIZE_OF_LONG, OUTPUT_BUFFER_TOO_SMALL);

    int input = inputAddress + inputSize;

    if (inputSize <= 2) {
      return 0;
    }

    final BitOutputStream stream =
        new BitOutputStream(outputBase, outputAddress, outputSize);

    int state1;
    int state2;

    if ((inputSize & 1) != 0) {
      input--;
      state1 = table.begin(inputBase[input]);

      input--;
      state2 = table.begin(inputBase[input]);

      input--;
      state1 = table.encode(stream, state1, inputBase[input]);

      stream.flush();
    } else {
      input--;
      state2 = table.begin(inputBase[input]);

      input--;
      state1 = table.begin(inputBase[input]);
    }

    // join to mod 4
    inputSize -= 2;

    if ((inputSize & 2) != 0) {  /* test bit 2 */
      input--;
      state2 = table.encode(stream, state2, inputBase[input]);

      input--;
      state1 = table.encode(stream, state1, inputBase[input]);

      stream.flush();
    }

    // 2 or 4 encoding per loop
    while (input > inputAddress) {
      input--;
      state2 = table.encode(stream, state2, inputBase[input]);

      input--;
      state1 = table.encode(stream, state1, inputBase[input]);

      input--;
      state2 = table.encode(stream, state2, inputBase[input]);

      input--;
      state1 = table.encode(stream, state1, inputBase[input]);

      stream.flush();
    }

    table.finish(stream, state2);
    table.finish(stream, state1);

    return stream.close();
  }

  public static int optimalTableLog(final int maxTableLog, final int inputSize,
                                    final int maxSymbol) {
    if (inputSize <= 1) {
      throw new IllegalArgumentException(); // not supported. Use RLE instead
    }

    int result = maxTableLog;

    result = Math.min(result, Util.highestBit((inputSize - 1)) -
                              2); // we may be able to reduce accuracy if input is small

    // Need a minimum to safely represent all symbol values
    result = Math.max(result, Util.minTableLog(inputSize, maxSymbol));

    result = Math.max(result, MIN_TABLE_LOG);
    result = Math.min(result, MAX_TABLE_LOG);

    return result;
  }

  public static void normalizeCounts(final short[] normalizedCounts,
                                     final int tableLog, final int[] counts,
                                     final int total, final int maxSymbol) {
    checkArgument(tableLog >= MIN_TABLE_LOG, "Unsupported FSE table size");
    checkArgument(tableLog <= MAX_TABLE_LOG, "FSE table size too large");
    checkArgument(tableLog >= Util.minTableLog(total, maxSymbol),
                  "FSE table size too small");

    final int scale = 62 - tableLog;
    final long step = (1L << 62) / total;
    final long vstep = 1L << (scale - 20);

    int stillToDistribute = 1 << tableLog;

    int largest = 0;
    short largestProbability = 0;
    final int lowThreshold = total >>> tableLog;

    for (int symbol = 0; symbol <= maxSymbol; symbol++) {
      if (counts[symbol] == total) {
        throw new IllegalArgumentException(); // TODO: should have been RLE-compressed by upper layers
      }
      if (counts[symbol] == 0) {
        normalizedCounts[symbol] = 0;
        continue;
      }
      if (counts[symbol] <= lowThreshold) {
        normalizedCounts[symbol] = -1;
        stillToDistribute--;
      } else {
        short probability = (short) ((counts[symbol] * step) >>> scale);
        if (probability < 8) {
          final long restToBeat = vstep * REST_TO_BEAT[probability];
          final long delta =
              counts[symbol] * step - (((long) probability) << scale);
          if (delta > restToBeat) {
            probability++;
          }
        }
        if (probability > largestProbability) {
          largestProbability = probability;
          largest = symbol;
        }
        normalizedCounts[symbol] = probability;
        stillToDistribute -= probability;
      }
    }

    if (-stillToDistribute >= (normalizedCounts[largest] >>> 1)) {
      // corner case. Need another normalization method
      // TODO size_t const errorCode = FSE_normalizeM2(normalizedCounter, tableLog, count, total, maxSymbolValue);
      normalizeCounts2(normalizedCounts, tableLog, counts, total, maxSymbol);
    } else {
      normalizedCounts[largest] += (short) stillToDistribute;
    }

  }

  private static void normalizeCounts2(final short[] normalizedCounts,
                                       final int tableLog, final int[] counts,
                                       int total, final int maxSymbol) {
    int distributed = 0;

    final int lowThreshold = total >>>
                             tableLog; // minimum count below which frequency in the normalized table is "too small" (~ < 1)
    int lowOne = (total * 3) >>> (tableLog +
                                  1); // 1.5 * lowThreshold. If count in (lowThreshold, lowOne] => assign frequency 1

    for (int i = 0; i <= maxSymbol; i++) {
      if (counts[i] == 0) {
        normalizedCounts[i] = 0;
      } else if (counts[i] <= lowThreshold) {
        normalizedCounts[i] = -1;
        distributed++;
        total -= counts[i];
      } else if (counts[i] <= lowOne) {
        normalizedCounts[i] = 1;
        distributed++;
        total -= counts[i];
      } else {
        normalizedCounts[i] = UNASSIGNED;
      }
    }

    final int normalizationFactor = 1 << tableLog;
    int toDistribute = normalizationFactor - distributed;

    if ((total / toDistribute) > lowOne) {
      /* risk of rounding to zero */
      lowOne = ((total * 3) / (toDistribute * 2));
      for (int i = 0; i <= maxSymbol; i++) {
        if ((normalizedCounts[i] == UNASSIGNED) && (counts[i] <= lowOne)) {
          normalizedCounts[i] = 1;
          distributed++;
          total -= counts[i];
        }
      }
      toDistribute = normalizationFactor - distributed;
    }

    if (distributed == maxSymbol + 1) {
      // all values are pretty poor;
      // probably incompressible data (should have already been detected);
      // find max, then give all remaining points to max
      int maxValue = 0;
      int maxCount = 0;
      for (int i = 0; i <= maxSymbol; i++) {
        if (counts[i] > maxCount) {
          maxValue = i;
          maxCount = counts[i];
        }
      }
      normalizedCounts[maxValue] += (short) toDistribute;
      return;
    }

    if (total == 0) {
      // all of the symbols were low enough for the lowOne or lowThreshold
      for (int i = 0; toDistribute > 0; i = (i + 1) % (maxSymbol + 1)) {
        if (normalizedCounts[i] > 0) {
          toDistribute--;
          normalizedCounts[i]++;
        }
      }
      return;
    }

    // TODO: simplify/document this code
    final long vStepLog = 62 - tableLog;
    final long mid = (1L << (vStepLog - 1)) - 1;
    final long rStep = (((1L << vStepLog) * toDistribute) + mid) /
                       total;   /* scale on remaining */
    long tmpTotal = mid;
    for (int i = 0; i <= maxSymbol; i++) {
      if (normalizedCounts[i] == UNASSIGNED) {
        final long end = tmpTotal + (counts[i] * rStep);
        final int sStart = (int) (tmpTotal >>> vStepLog);
        final int sEnd = (int) (end >>> vStepLog);
        final int weight = sEnd - sStart;

        if (weight < 1) {
          throw new AssertionError();
        }
        normalizedCounts[i] = (short) weight;
        tmpTotal = end;
      }
    }

  }

  public static int writeNormalizedCounts(final byte[] outputBase,
                                          final int outputAddress,
                                          final int outputSize,
                                          final short[] normalizedCounts,
                                          final int maxSymbol,
                                          final int tableLog) {
    checkArgument(tableLog <= MAX_TABLE_LOG, "FSE table too large");
    checkArgument(tableLog >= MIN_TABLE_LOG, "FSE table too small");

    int output = outputAddress;
    final int outputLimit = outputAddress + outputSize;

    final int tableSize = 1 << tableLog;

    int bitCount = 0;

    // encode table size
    int bitStream = (tableLog - MIN_TABLE_LOG);
    bitCount += 4;

    int remaining = tableSize + 1; // +1 for extra accuracy
    int threshold = tableSize;
    int tableBitCount = tableLog + 1;

    int symbol = 0;

    boolean previousIs0 = false;
    while (remaining > 1) {
      if (previousIs0) {
        // From RFC 8478, section 4.1.1:
        //   When a symbol has a probability of zero, it is followed by a 2-bit
        //   repeat flag.  This repeat flag tells how many probabilities of zeroes
        //   follow the current one.  It provides a number ranging from 0 to 3.
        //   If it is a 3, another 2-bit repeat flag follows, and so on.
        int start = symbol;

        // find run of symbols with count 0
        while (normalizedCounts[symbol] == 0) {
          symbol++;
        }

        // encode in batches if 8 repeat sequences in one shot (representing 24 symbols total)
        while (symbol >= start + 24) {
          start += 24;
          bitStream |= (0xFFFF << bitCount);
          checkArgument(output + SIZE_OF_SHORT <= outputLimit,
                        OUTPUT_BUFFER_TOO_SMALL);

          putShort(outputBase, output, (short) bitStream);
          output += SIZE_OF_SHORT;

          // flush now, so no need to increase bitCount by 16
          bitStream >>>= Short.SIZE;
        }

        // encode remaining in batches of 3 symbols
        while (symbol >= start + 3) {
          start += 3;
          bitStream |= 0x3 << bitCount;
          bitCount += 2;
        }

        // encode tail
        bitStream |= (symbol - start) << bitCount;
        bitCount += 2;

        // flush bitstream if necessary
        if (bitCount > 16) {
          checkArgument(output + SIZE_OF_SHORT <= outputLimit,
                        OUTPUT_BUFFER_TOO_SMALL);

          putShort(outputBase, output, (short) bitStream);
          output += SIZE_OF_SHORT;

          bitStream >>>= Short.SIZE;
          bitCount -= Short.SIZE;
        }
      }

      int count = normalizedCounts[symbol++];
      final int max = (2 * threshold - 1) - remaining;
      remaining -= count < 0? -count : count;
      count++;   /* +1 for extra accuracy */
      if (count >= threshold) {
        count += max;
      }
      bitStream |= count << bitCount;
      bitCount += tableBitCount;
      bitCount -= (count < max? 1 : 0);
      previousIs0 = (count == 1);

      if (remaining < 1) {
        throw new AssertionError();
      }

      while (remaining < threshold) {
        tableBitCount--;
        threshold >>= 1;
      }

      // flush bitstream if necessary
      if (bitCount > 16) {
        checkArgument(output + SIZE_OF_SHORT <= outputLimit,
                      OUTPUT_BUFFER_TOO_SMALL);

        putShort(outputBase, output, (short) bitStream);
        output += SIZE_OF_SHORT;

        bitStream >>>= Short.SIZE;
        bitCount -= Short.SIZE;
      }
    }

    // flush remaining bitstream
    checkArgument(output + SIZE_OF_SHORT <= outputLimit,
                  OUTPUT_BUFFER_TOO_SMALL);
    putShort(outputBase, output, (short) bitStream);
    output += (bitCount + 7) / 8;

    checkArgument(symbol <= maxSymbol + 1, "Error"); // TODO

    return output - outputAddress;
  }

  public static final class Table {
    int log2Size;
    final int[] newState;
    final byte[] symbol;
    final byte[] numberOfBits;

    public Table(final int log2Capacity) {
      final int capacity = 1 << log2Capacity;
      newState = new int[capacity];
      symbol = new byte[capacity];
      numberOfBits = new byte[capacity];
    }

    public Table(final int log2Size, final int[] newState, final byte[] symbol,
                 final byte[] numberOfBits) {
      final int size = 1 << log2Size;
      if (newState.length != size || symbol.length != size ||
          numberOfBits.length != size) {
        throw new IllegalArgumentException(
            "Expected arrays to match provided size");
      }

      this.log2Size = log2Size;
      this.newState = newState;
      this.symbol = symbol;
      this.numberOfBits = numberOfBits;
    }
  }
}
