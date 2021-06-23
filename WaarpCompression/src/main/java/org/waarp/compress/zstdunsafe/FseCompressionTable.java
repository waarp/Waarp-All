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

class FseCompressionTable {
  private final short[] nextState;
  private final int[] deltaNumberOfBits;
  private final int[] deltaFindState;

  private int log2Size;

  public FseCompressionTable(final int maxTableLog, final int maxSymbol) {
    nextState = new short[1 << maxTableLog];
    deltaNumberOfBits = new int[maxSymbol + 1];
    deltaFindState = new int[maxSymbol + 1];
  }

  public static FseCompressionTable newInstance(final short[] normalizedCounts,
                                                final int maxSymbol,
                                                final int tableLog) {
    final FseCompressionTable result =
        new FseCompressionTable(tableLog, maxSymbol);
    result.initialize(normalizedCounts, maxSymbol, tableLog);

    return result;
  }

  public void initializeRleTable(final int symbol) {
    log2Size = 0;

    nextState[0] = 0;
    nextState[1] = 0;

    deltaFindState[symbol] = 0;
    deltaNumberOfBits[symbol] = 0;
  }

  public void initialize(final short[] normalizedCounts, final int maxSymbol,
                         final int tableLog) {
    final int tableSize = 1 << tableLog;

    final byte[] table = new byte[tableSize]; // TODO: allocate in workspace
    int highThreshold = tableSize - 1;

    // TODO: make sure FseCompressionTable has enough size
    log2Size = tableLog;

    // For explanations on how to distribute symbol values over the table:
    // http://fastcompression.blogspot.fr/2014/02/fse-distributing-symbol-values.html

    // symbol start positions
    final int[] cumulative = new int[FiniteStateEntropy.MAX_SYMBOL +
                                     2]; // TODO: allocate in workspace
    cumulative[0] = 0;
    for (int i = 1; i <= maxSymbol + 1; i++) {
      if (normalizedCounts[i - 1] == -1) {  // Low probability symbol
        cumulative[i] = cumulative[i - 1] + 1;
        table[highThreshold--] = (byte) (i - 1);
      } else {
        cumulative[i] = cumulative[i - 1] + normalizedCounts[i - 1];
      }
    }
    cumulative[maxSymbol + 1] = tableSize + 1;

    // Spread symbols
    final int position =
        spreadSymbols(normalizedCounts, maxSymbol, tableSize, highThreshold,
                      table);

    if (position != 0) {
      throw new AssertionError("Spread symbols failed");
    }

    // Build table
    for (int i = 0; i < tableSize; i++) {
      final byte symbol = table[i];
      nextState[cumulative[symbol]++] = (short) (tableSize +
                                                 i);  /* TableU16 : sorted by symbol order; gives next state value */
    }

    // Build symbol transformation table
    int total = 0;
    for (int symbol = 0; symbol <= maxSymbol; symbol++) {
      switch (normalizedCounts[symbol]) {
        case 0:
          deltaNumberOfBits[symbol] = ((tableLog + 1) << 16) - tableSize;
          break;
        case -1:
        case 1:
          deltaNumberOfBits[symbol] = (tableLog << 16) - tableSize;
          deltaFindState[symbol] = total - 1;
          total++;
          break;
        default:
          final int maxBitsOut =
              tableLog - Util.highestBit(normalizedCounts[symbol] - 1);
          final int minStatePlus = normalizedCounts[symbol] << maxBitsOut;
          deltaNumberOfBits[symbol] = (maxBitsOut << 16) - minStatePlus;
          deltaFindState[symbol] = total - normalizedCounts[symbol];
          total += normalizedCounts[symbol];
          break;
      }
    }
  }

  public int begin(final byte symbol) {
    final int outputBits = (deltaNumberOfBits[symbol] + (1 << 15)) >>> 16;
    final int base =
        ((outputBits << 16) - deltaNumberOfBits[symbol]) >>> outputBits;
    return nextState[base + deltaFindState[symbol]];
  }

  public int encode(final BitOutputStream stream, final int state,
                    final int symbol) {
    final int outputBits = (state + deltaNumberOfBits[symbol]) >>> 16;
    stream.addBits(state, outputBits);
    return nextState[(state >>> outputBits) + deltaFindState[symbol]];
  }

  public void finish(final BitOutputStream stream, final int state) {
    stream.addBits(state, log2Size);
    stream.flush();
  }

  private static int calculateStep(final int tableSize) {
    return (tableSize >>> 1) + (tableSize >>> 3) + 3;
  }

  public static int spreadSymbols(final short[] normalizedCounters,
                                  final int maxSymbolValue, final int tableSize,
                                  final int highThreshold,
                                  final byte[] symbols) {
    final int mask = tableSize - 1;
    final int step = calculateStep(tableSize);

    int position = 0;
    for (byte symbol = 0; symbol <= maxSymbolValue; symbol++) {
      for (int i = 0; i < normalizedCounters[symbol]; i++) {
        symbols[position] = symbol;
        do {
          position = (position + step) & mask;
        } while (position > highThreshold);
      }
    }
    return position;
  }
}
