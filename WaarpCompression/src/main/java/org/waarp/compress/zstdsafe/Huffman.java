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

import java.util.Arrays;

import static org.waarp.compress.zstdsafe.BitInputStream.*;
import static org.waarp.compress.zstdsafe.Constants.*;
import static org.waarp.compress.zstdsafe.UnsafeUtil.*;
import static org.waarp.compress.zstdsafe.Util.*;

class Huffman {
  public static final int MAX_SYMBOL = 255;
  public static final int MAX_SYMBOL_COUNT = MAX_SYMBOL + 1;

  public static final int MAX_TABLE_LOG = 12;
  public static final int MIN_TABLE_LOG = 5;
  public static final int MAX_FSE_TABLE_LOG = 6;
  public static final String NOT_ENOUGH_INPUT_BYTES = "Not enough input bytes";
  public static final String INPUT_IS_CORRUPTED = "Input is corrupted";

  // stats
  private final byte[] weights = new byte[MAX_SYMBOL + 1];
  private final int[] ranks = new int[MAX_TABLE_LOG + 1];

  // table
  private int tableLog = -1;
  private final byte[] symbols = new byte[1 << MAX_TABLE_LOG];
  private final byte[] numbersOfBits = new byte[1 << MAX_TABLE_LOG];

  private final FseTableReader reader = new FseTableReader();
  private final FiniteStateEntropy.Table fseTable =
      new FiniteStateEntropy.Table(MAX_FSE_TABLE_LOG);

  public boolean isLoaded() {
    return tableLog != -1;
  }

  public int readTable(final byte[] inputBase, final int inputAddress,
                       final int size) {
    Arrays.fill(ranks, 0);
    int input = inputAddress;

    // read table header
    verify(size > 0, input, NOT_ENOUGH_INPUT_BYTES);
    int inputSize = inputBase[input++] & 0xFF;

    final int outputSize;
    if (inputSize >= 128) {
      outputSize = inputSize - 127;
      inputSize = ((outputSize + 1) / 2);

      verify(inputSize + 1 <= size, input, NOT_ENOUGH_INPUT_BYTES);
      verify(outputSize <= MAX_SYMBOL + 1, input, INPUT_IS_CORRUPTED);

      for (int i = 0; i < outputSize; i += 2) {
        final int value = inputBase[input + i / 2] & 0xFF;
        weights[i] = (byte) (value >>> 4);
        weights[i + 1] = (byte) (value & 0xF);
      }
    } else {
      verify(inputSize + 1 <= size, input, NOT_ENOUGH_INPUT_BYTES);

      final int inputLimit = input + inputSize;
      input += reader.readFseTable(fseTable, inputBase, input, inputLimit,
                                   FiniteStateEntropy.MAX_SYMBOL,
                                   MAX_FSE_TABLE_LOG);
      outputSize =
          FiniteStateEntropy.decompress(fseTable, inputBase, input, inputLimit,
                                        weights);
    }

    int totalWeight = 0;
    for (int i = 0; i < outputSize; i++) {
      ranks[weights[i]]++;
      totalWeight +=
          (1 << weights[i]) >> 1;   // TODO same as 1 << (weights[n] - 1)?
    }
    verify(totalWeight != 0, input, INPUT_IS_CORRUPTED);

    tableLog = Util.highestBit(totalWeight) + 1;
    verify(tableLog <= MAX_TABLE_LOG, input, INPUT_IS_CORRUPTED);

    final int total = 1 << tableLog;
    final int rest = total - totalWeight;
    verify(isPowerOf2(rest), input, INPUT_IS_CORRUPTED);

    final int lastWeight = Util.highestBit(rest) + 1;

    weights[outputSize] = (byte) lastWeight;
    ranks[lastWeight]++;

    final int numberOfSymbols = outputSize + 1;

    // populate table
    int nextRankStart = 0;
    for (int i = 1; i < tableLog + 1; ++i) {
      final int current = nextRankStart;
      nextRankStart += ranks[i] << (i - 1);
      ranks[i] = current;
    }

    for (int n = 0; n < numberOfSymbols; n++) {
      final int weight = weights[n];
      final int length = (1 << weight) >> 1;  // TODO: 1 << (weight - 1) ??

      final byte symbol = (byte) n;
      final byte numberOfBits = (byte) (tableLog + 1 - weight);
      for (int i = ranks[weight]; i < ranks[weight] + length; i++) {
        symbols[i] = symbol;
        numbersOfBits[i] = numberOfBits;
      }
      ranks[weight] += length;
    }

    verify(ranks[1] >= 2 && (ranks[1] & 1) == 0, input, INPUT_IS_CORRUPTED);

    return inputSize + 1;
  }

  public void decodeSingleStream(final byte[] inputBase, final int inputAddress,
                                 final int inputLimit, final byte[] outputBase,
                                 final int outputAddress,
                                 final int outputLimit) {
    final BitInputStream.Initializer initializer =
        new BitInputStream.Initializer(inputBase, inputAddress, inputLimit);
    initializer.initialize();

    long bits = initializer.getBits();
    int bitsConsumed = initializer.getBitsConsumed();
    int currentAddress = initializer.getCurrentAddress();

    final int tableLog1 = this.tableLog;
    final byte[] numbersOfBits1 = this.numbersOfBits;
    final byte[] symbols1 = this.symbols;

    // 4 symbols at a time
    int output = outputAddress;
    final int fastOutputLimit = outputLimit - 4;
    while (output < fastOutputLimit) {
      final BitInputStream.Loader loader =
          new BitInputStream.Loader(inputBase, inputAddress, currentAddress,
                                    bits, bitsConsumed);
      final boolean done = loader.load();
      bits = loader.getBits();
      bitsConsumed = loader.getBitsConsumed();
      currentAddress = loader.getCurrentAddress();
      if (done) {
        break;
      }

      bitsConsumed =
          decodeSymbol(outputBase, output, bits, bitsConsumed, tableLog1,
                       numbersOfBits1, symbols1);
      bitsConsumed =
          decodeSymbol(outputBase, output + 1, bits, bitsConsumed, tableLog1,
                       numbersOfBits1, symbols1);
      bitsConsumed =
          decodeSymbol(outputBase, output + 2, bits, bitsConsumed, tableLog1,
                       numbersOfBits1, symbols1);
      bitsConsumed =
          decodeSymbol(outputBase, output + 3, bits, bitsConsumed, tableLog1,
                       numbersOfBits1, symbols1);
      output += SIZE_OF_INT;
    }

    decodeTail(inputBase, inputAddress, currentAddress, bitsConsumed, bits,
               outputBase, output, outputLimit);
  }

  public void decode4Streams(final byte[] inputBase, final int inputAddress,
                             final int inputLimit, final byte[] outputBase,
                             final int outputAddress, final int outputLimit) {
    verify(inputLimit - inputAddress >= 10, inputAddress,
           INPUT_IS_CORRUPTED); // jump table + 1 byte per stream

    final int start1 =
        inputAddress + 3 * SIZE_OF_SHORT; // for the shorts we read below
    final int start2 = start1 + (getShort(inputBase, inputAddress) & 0xFFFF);
    final int start3 =
        start2 + (getShort(inputBase, inputAddress + 2) & 0xFFFF);
    final int start4 =
        start3 + (getShort(inputBase, inputAddress + 4) & 0xFFFF);

    BitInputStream.Initializer initializer =
        new BitInputStream.Initializer(inputBase, start1, start2);
    initializer.initialize();
    int stream1bitsConsumed = initializer.getBitsConsumed();
    int stream1currentAddress = initializer.getCurrentAddress();
    long stream1bits = initializer.getBits();

    initializer = new BitInputStream.Initializer(inputBase, start2, start3);
    initializer.initialize();
    int stream2bitsConsumed = initializer.getBitsConsumed();
    int stream2currentAddress = initializer.getCurrentAddress();
    long stream2bits = initializer.getBits();

    initializer = new BitInputStream.Initializer(inputBase, start3, start4);
    initializer.initialize();
    int stream3bitsConsumed = initializer.getBitsConsumed();
    int stream3currentAddress = initializer.getCurrentAddress();
    long stream3bits = initializer.getBits();

    initializer = new BitInputStream.Initializer(inputBase, start4, inputLimit);
    initializer.initialize();
    int stream4bitsConsumed = initializer.getBitsConsumed();
    int stream4currentAddress = initializer.getCurrentAddress();
    long stream4bits = initializer.getBits();

    final int segmentSize = (outputLimit - outputAddress + 3) / 4;

    final int outputStart2 = outputAddress + segmentSize;
    final int outputStart3 = outputStart2 + segmentSize;
    final int outputStart4 = outputStart3 + segmentSize;

    int output1 = outputAddress;
    int output2 = outputStart2;
    int output3 = outputStart3;
    int output4 = outputStart4;

    final int fastOutputLimit = outputLimit - 7;
    final int tableLog1 = this.tableLog;
    final byte[] numbersOfBits1 = this.numbersOfBits;
    final byte[] symbols1 = this.symbols;

    while (output4 < fastOutputLimit) {
      stream1bitsConsumed =
          decodeSymbol(outputBase, output1, stream1bits, stream1bitsConsumed,
                       tableLog1, numbersOfBits1, symbols1);
      stream2bitsConsumed =
          decodeSymbol(outputBase, output2, stream2bits, stream2bitsConsumed,
                       tableLog1, numbersOfBits1, symbols1);
      stream3bitsConsumed =
          decodeSymbol(outputBase, output3, stream3bits, stream3bitsConsumed,
                       tableLog1, numbersOfBits1, symbols1);
      stream4bitsConsumed =
          decodeSymbol(outputBase, output4, stream4bits, stream4bitsConsumed,
                       tableLog1, numbersOfBits1, symbols1);

      stream1bitsConsumed = decodeSymbol(outputBase, output1 + 1, stream1bits,
                                         stream1bitsConsumed, tableLog1,
                                         numbersOfBits1, symbols1);
      stream2bitsConsumed = decodeSymbol(outputBase, output2 + 1, stream2bits,
                                         stream2bitsConsumed, tableLog1,
                                         numbersOfBits1, symbols1);
      stream3bitsConsumed = decodeSymbol(outputBase, output3 + 1, stream3bits,
                                         stream3bitsConsumed, tableLog1,
                                         numbersOfBits1, symbols1);
      stream4bitsConsumed = decodeSymbol(outputBase, output4 + 1, stream4bits,
                                         stream4bitsConsumed, tableLog1,
                                         numbersOfBits1, symbols1);

      stream1bitsConsumed = decodeSymbol(outputBase, output1 + 2, stream1bits,
                                         stream1bitsConsumed, tableLog1,
                                         numbersOfBits1, symbols1);
      stream2bitsConsumed = decodeSymbol(outputBase, output2 + 2, stream2bits,
                                         stream2bitsConsumed, tableLog1,
                                         numbersOfBits1, symbols1);
      stream3bitsConsumed = decodeSymbol(outputBase, output3 + 2, stream3bits,
                                         stream3bitsConsumed, tableLog1,
                                         numbersOfBits1, symbols1);
      stream4bitsConsumed = decodeSymbol(outputBase, output4 + 2, stream4bits,
                                         stream4bitsConsumed, tableLog1,
                                         numbersOfBits1, symbols1);

      stream1bitsConsumed = decodeSymbol(outputBase, output1 + 3, stream1bits,
                                         stream1bitsConsumed, tableLog1,
                                         numbersOfBits1, symbols1);
      stream2bitsConsumed = decodeSymbol(outputBase, output2 + 3, stream2bits,
                                         stream2bitsConsumed, tableLog1,
                                         numbersOfBits1, symbols1);
      stream3bitsConsumed = decodeSymbol(outputBase, output3 + 3, stream3bits,
                                         stream3bitsConsumed, tableLog1,
                                         numbersOfBits1, symbols1);
      stream4bitsConsumed = decodeSymbol(outputBase, output4 + 3, stream4bits,
                                         stream4bitsConsumed, tableLog1,
                                         numbersOfBits1, symbols1);

      output1 += SIZE_OF_INT;
      output2 += SIZE_OF_INT;
      output3 += SIZE_OF_INT;
      output4 += SIZE_OF_INT;

      BitInputStream.Loader loader =
          new BitInputStream.Loader(inputBase, start1, stream1currentAddress,
                                    stream1bits, stream1bitsConsumed);
      boolean done = loader.load();
      stream1bitsConsumed = loader.getBitsConsumed();
      stream1bits = loader.getBits();
      stream1currentAddress = loader.getCurrentAddress();

      if (done) {
        break;
      }

      loader =
          new BitInputStream.Loader(inputBase, start2, stream2currentAddress,
                                    stream2bits, stream2bitsConsumed);
      done = loader.load();
      stream2bitsConsumed = loader.getBitsConsumed();
      stream2bits = loader.getBits();
      stream2currentAddress = loader.getCurrentAddress();

      if (done) {
        break;
      }

      loader =
          new BitInputStream.Loader(inputBase, start3, stream3currentAddress,
                                    stream3bits, stream3bitsConsumed);
      done = loader.load();
      stream3bitsConsumed = loader.getBitsConsumed();
      stream3bits = loader.getBits();
      stream3currentAddress = loader.getCurrentAddress();
      if (done) {
        break;
      }

      loader =
          new BitInputStream.Loader(inputBase, start4, stream4currentAddress,
                                    stream4bits, stream4bitsConsumed);
      done = loader.load();
      stream4bitsConsumed = loader.getBitsConsumed();
      stream4bits = loader.getBits();
      stream4currentAddress = loader.getCurrentAddress();
      if (done) {
        break;
      }
    }

    verify(output1 <= outputStart2 && output2 <= outputStart3 &&
           output3 <= outputStart4, inputAddress, INPUT_IS_CORRUPTED);

    /// finish streams one by one
    decodeTail(inputBase, start1, stream1currentAddress, stream1bitsConsumed,
               stream1bits, outputBase, output1, outputStart2);
    decodeTail(inputBase, start2, stream2currentAddress, stream2bitsConsumed,
               stream2bits, outputBase, output2, outputStart3);
    decodeTail(inputBase, start3, stream3currentAddress, stream3bitsConsumed,
               stream3bits, outputBase, output3, outputStart4);
    decodeTail(inputBase, start4, stream4currentAddress, stream4bitsConsumed,
               stream4bits, outputBase, output4, outputLimit);
  }

  private void decodeTail(final byte[] inputBase, final int startAddress,
                          int currentAddress, int bitsConsumed, long bits,
                          final byte[] outputBase, int outputAddress,
                          final int outputLimit) {
    final int tableLog1 = this.tableLog;
    final byte[] numbersOfBits1 = this.numbersOfBits;
    final byte[] symbols1 = this.symbols;

    // closer to the end
    while (outputAddress < outputLimit) {
      final BitInputStream.Loader loader =
          new BitInputStream.Loader(inputBase, startAddress, currentAddress,
                                    bits, bitsConsumed);
      final boolean done = loader.load();
      bitsConsumed = loader.getBitsConsumed();
      bits = loader.getBits();
      currentAddress = loader.getCurrentAddress();
      if (done) {
        break;
      }

      bitsConsumed =
          decodeSymbol(outputBase, outputAddress++, bits, bitsConsumed,
                       tableLog1, numbersOfBits1, symbols1);
    }

    // not more data in bit stream, so no need to reload
    while (outputAddress < outputLimit) {
      bitsConsumed =
          decodeSymbol(outputBase, outputAddress++, bits, bitsConsumed,
                       tableLog1, numbersOfBits1, symbols1);
    }

    verify(isEndOfStream(startAddress, currentAddress, bitsConsumed),
           startAddress, "Bit stream is not fully consumed");
  }

  private static int decodeSymbol(final byte[] outputBase,
                                  final int outputAddress,
                                  final long bitContainer,
                                  final int bitsConsumed, final int tableLog,
                                  final byte[] numbersOfBits,
                                  final byte[] symbols) {
    final int value = (int) peekBitsFast(bitsConsumed, bitContainer, tableLog);
    outputBase[outputAddress] = symbols[value];
    return bitsConsumed + numbersOfBits[value];
  }
}
