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

import static org.waarp.compress.zstdunsafe.Constants.*;
import static org.waarp.compress.zstdunsafe.Huffman.*;
import static org.waarp.compress.zstdunsafe.UnsafeUtil.*;
import static org.waarp.compress.zstdunsafe.Util.*;
import static sun.misc.Unsafe.*;

class ZstdFrameCompressor {
  static final int MAX_FRAME_HEADER_SIZE = 14;

  private static final int CHECKSUM_FLAG = 0x4;
  private static final int SINGLE_SEGMENT_FLAG = 0x20;

  private static final int MINIMUM_LITERALS_SIZE = 63;

  // the maximum table log allowed for literal encoding per RFC 8478, section 4.2.1
  private static final int MAX_HUFFMAN_TABLE_LOG = 11;
  public static final String OUTPUT_BUFFER_TOO_SMALL =
      "Output buffer too small";

  private ZstdFrameCompressor() {
  }

  // visible for testing
  static int writeMagic(final Object outputBase, final long outputAddress,
                        final long outputLimit) {
    checkArgument(outputLimit - outputAddress >= SIZE_OF_INT,
                  OUTPUT_BUFFER_TOO_SMALL);

    UNSAFE.putInt(outputBase, outputAddress, MAGIC_NUMBER);
    return SIZE_OF_INT;
  }

  // visible for testing
  static int writeFrameHeader(final Object outputBase, final long outputAddress,
                              final long outputLimit, final int inputSize,
                              final int windowSize) {
    checkArgument(outputLimit - outputAddress >= MAX_FRAME_HEADER_SIZE,
                  OUTPUT_BUFFER_TOO_SMALL);

    long output = outputAddress;

    final int contentSizeDescriptor =
        (inputSize >= 256? 1 : 0) + (inputSize >= 65536 + 256? 1 : 0);
    int frameHeaderDescriptor =
        (contentSizeDescriptor << 6) | CHECKSUM_FLAG; // dictionary ID missing

    final boolean singleSegment = windowSize >= inputSize;
    if (singleSegment) {
      frameHeaderDescriptor |= SINGLE_SEGMENT_FLAG;
    }

    UNSAFE.putByte(outputBase, output, (byte) frameHeaderDescriptor);
    output++;

    if (!singleSegment) {
      final int base = Integer.highestOneBit(windowSize);

      final int exponent = 32 - Integer.numberOfLeadingZeros(base) - 1;
      if (exponent < MIN_WINDOW_LOG) {
        throw new IllegalArgumentException(
            "Minimum window size is " + (1 << MIN_WINDOW_LOG));
      }

      final int remainder = windowSize - base;
      if (remainder % (base / 8) != 0) {
        throw new IllegalArgumentException(
            "Window size of magnitude 2^" + exponent + " must be multiple of " +
            (base / 8));
      }

      // mantissa is guaranteed to be between 0-7
      final int mantissa = remainder / (base / 8);
      final int encoded = ((exponent - MIN_WINDOW_LOG) << 3) | mantissa;

      UNSAFE.putByte(outputBase, output, (byte) encoded);
      output++;
    }

    switch (contentSizeDescriptor) {
      case 0:
        if (singleSegment) {
          UNSAFE.putByte(outputBase, output++, (byte) inputSize);
        }
        break;
      case 1:
        UNSAFE.putShort(outputBase, output, (short) (inputSize - 256));
        output += SIZE_OF_SHORT;
        break;
      case 2:
        UNSAFE.putInt(outputBase, output, inputSize);
        output += SIZE_OF_INT;
        break;
      default:
        throw new AssertionError();
    }

    return (int) (output - outputAddress);
  }

  // visible for testing
  static int writeChecksum(final Object outputBase, final long outputAddress,
                           final long outputLimit, final Object inputBase,
                           final long inputAddress, final long inputLimit) {
    checkArgument(outputLimit - outputAddress >= SIZE_OF_INT,
                  OUTPUT_BUFFER_TOO_SMALL);

    final int inputSize = (int) (inputLimit - inputAddress);

    final long hash = XxHash64.hash(0, inputBase, inputAddress, inputSize);

    UNSAFE.putInt(outputBase, outputAddress, (int) hash);

    return SIZE_OF_INT;
  }

  public static int compress(final Object inputBase, final long inputAddress,
                             final long inputLimit, final Object outputBase,
                             final long outputAddress, final long outputLimit,
                             final int compressionLevel) {
    final int inputSize = (int) (inputLimit - inputAddress);

    final CompressionParameters parameters =
        CompressionParameters.compute(compressionLevel, inputSize);

    long output = outputAddress;

    output += writeMagic(outputBase, output, outputLimit);
    output += writeFrameHeader(outputBase, output, outputLimit, inputSize,
                               1 << parameters.getWindowLog());
    output +=
        compressFrame(inputBase, inputAddress, inputLimit, outputBase, output,
                      outputLimit, parameters);
    output +=
        writeChecksum(outputBase, output, outputLimit, inputBase, inputAddress,
                      inputLimit);

    return (int) (output - outputAddress);
  }

  private static int compressFrame(final Object inputBase,
                                   final long inputAddress,
                                   final long inputLimit,
                                   final Object outputBase,
                                   final long outputAddress,
                                   final long outputLimit,
                                   final CompressionParameters parameters) {
    final int windowSize = 1 << parameters
        .getWindowLog(); // TODO: store window size in parameters directly?
    int blockSize = Math.min(MAX_BLOCK_SIZE, windowSize);

    int outputSize = (int) (outputLimit - outputAddress);
    int remaining = (int) (inputLimit - inputAddress);

    long output = outputAddress;
    long input = inputAddress;

    final CompressionContext context =
        new CompressionContext(parameters, inputAddress, remaining);

    do {
      checkArgument(outputSize >= SIZE_OF_BLOCK_HEADER + MIN_BLOCK_SIZE,
                    OUTPUT_BUFFER_TOO_SMALL);

      final int lastBlockFlag = blockSize >= remaining? 1 : 0;
      blockSize = Math.min(blockSize, remaining);

      int compressedSize = 0;
      if (remaining > 0) {
        compressedSize = compressBlock(inputBase, input, blockSize, outputBase,
                                       output + SIZE_OF_BLOCK_HEADER,
                                       outputSize - SIZE_OF_BLOCK_HEADER,
                                       context, parameters);
      }

      if (compressedSize == 0) { // block is not compressible
        checkArgument(blockSize + SIZE_OF_BLOCK_HEADER <= outputSize,
                      "Output size too small");

        final int blockHeader =
            lastBlockFlag | (RAW_BLOCK << 1) | (blockSize << 3);
        put24BitLittleEndian(outputBase, output, blockHeader);
        UNSAFE.copyMemory(inputBase, input, outputBase,
                          output + SIZE_OF_BLOCK_HEADER, blockSize);
        compressedSize = SIZE_OF_BLOCK_HEADER + blockSize;
      } else {
        final int blockHeader =
            lastBlockFlag | (COMPRESSED_BLOCK << 1) | (compressedSize << 3);
        put24BitLittleEndian(outputBase, output, blockHeader);
        compressedSize += SIZE_OF_BLOCK_HEADER;
      }

      input += blockSize;
      remaining -= blockSize;
      output += compressedSize;
      outputSize -= compressedSize;
    } while (remaining > 0);

    return (int) (output - outputAddress);
  }

  private static int compressBlock(final Object inputBase,
                                   final long inputAddress, final int inputSize,
                                   final Object outputBase,
                                   final long outputAddress,
                                   final int outputSize,
                                   final CompressionContext context,
                                   final CompressionParameters parameters) {
    if (inputSize < MIN_BLOCK_SIZE + SIZE_OF_BLOCK_HEADER + 1) {
      //  don't even attempt compression below a certain input size
      return 0;
    }

    context.blockCompressionState.enforceMaxDistance(inputAddress + inputSize,
                                                     1 <<
                                                     parameters.getWindowLog());
    context.sequenceStore.reset();

    final int lastLiteralsSize = parameters.getStrategy().getCompressor()
                                           .compressBlock(inputBase,
                                                          inputAddress,
                                                          inputSize,
                                                          context.sequenceStore,
                                                          context.blockCompressionState,
                                                          context.offsets,
                                                          parameters);

    final long lastLiteralsAddress =
        inputAddress + inputSize - lastLiteralsSize;

    // append [lastLiteralsAddress .. lastLiteralsSize] to sequenceStore literals buffer
    context.sequenceStore
        .appendLiterals(inputBase, lastLiteralsAddress, lastLiteralsSize);

    // convert length/offsets into codes
    context.sequenceStore.generateCodes();

    final long outputLimit = outputAddress + outputSize;
    long output = outputAddress;

    final int compressedLiteralsSize =
        encodeLiterals(context.huffmanContext, parameters, outputBase, output,
                       (int) (outputLimit - output),
                       context.sequenceStore.literalsBuffer,
                       context.sequenceStore.literalsLength);
    output += compressedLiteralsSize;

    final int compressedSequencesSize = SequenceEncoder
        .compressSequences(outputBase, output, (int) (outputLimit - output),
                           context.sequenceStore, parameters.getStrategy(),
                           context.sequenceEncodingContext);

    final int compressedSize = compressedLiteralsSize + compressedSequencesSize;
    if (compressedSize == 0) {
      // not compressible
      return compressedSize;
    }

    // Check compressibility
    final int maxCompressedSize =
        inputSize - calculateMinimumGain(inputSize, parameters.getStrategy());
    if (compressedSize > maxCompressedSize) {
      return 0; // not compressed
    }

    // confirm repeated offsets and entropy tables
    context.commit();

    return compressedSize;
  }

  private static int encodeLiterals(final HuffmanCompressionContext context,
                                    final CompressionParameters parameters,
                                    final Object outputBase,
                                    final long outputAddress,
                                    final int outputSize, final byte[] literals,
                                    final int literalsSize) {
    // TODO: move this to Strategy
    final boolean bypassCompression =
        (parameters.getStrategy() == CompressionParameters.Strategy.FAST) &&
        (parameters.getTargetLength() > 0);
    if (bypassCompression || literalsSize <= MINIMUM_LITERALS_SIZE) {
      return rawLiterals(outputBase, outputAddress, outputSize, literals,
                         literalsSize);
    }

    final int headerSize =
        3 + (literalsSize >= 1024? 1 : 0) + (literalsSize >= 16384? 1 : 0);

    checkArgument(headerSize + 1 <= outputSize, OUTPUT_BUFFER_TOO_SMALL);

    final int[] counts = new int[MAX_SYMBOL_COUNT]; // TODO: preallocate
    Histogram.count(literals, literalsSize, counts);
    final int maxSymbol = Histogram.findMaxSymbol(counts, MAX_SYMBOL);
    final int largestCount = Histogram.findLargestCount(counts, maxSymbol);

    final long literalsAddress = ARRAY_BYTE_BASE_OFFSET;
    if (largestCount == literalsSize) {
      // all bytes in input are equal
      return rleLiterals(outputBase, outputAddress, literals, literalsSize);
    } else if (largestCount <= (literalsSize >>> 7) + 4) {
      // heuristic: probably not compressible enough
      return rawLiterals(outputBase, outputAddress, outputSize, literals,
                         literalsSize);
    }

    final HuffmanCompressionTable previousTable = context.getPreviousTable();
    final HuffmanCompressionTable table;
    int serializedTableSize;
    final boolean reuseTable;

    final boolean canReuse = previousTable.isValid(counts, maxSymbol);

    // heuristic: use existing table for small inputs if valid
    // TODO: move to Strategy
    final boolean preferReuse = parameters.getStrategy().ordinal() <
                                CompressionParameters.Strategy.LAZY.ordinal() &&
                                literalsSize <= 1024;
    if (preferReuse && canReuse) {
      table = previousTable;
      reuseTable = true;
      serializedTableSize = 0;
    } else {
      final HuffmanCompressionTable newTable = context.borrowTemporaryTable();

      newTable.initialize(counts, maxSymbol, HuffmanCompressionTable
                              .optimalNumberOfBits(MAX_HUFFMAN_TABLE_LOG, literalsSize, maxSymbol),
                          context.getCompressionTableWorkspace());

      serializedTableSize = newTable
          .write(outputBase, outputAddress + headerSize,
                 outputSize - headerSize, context.getTableWriterWorkspace());

      // Check if using previous huffman table is beneficial
      if (canReuse && previousTable.estimateCompressedSize(counts, maxSymbol) <=
                      serializedTableSize +
                      newTable.estimateCompressedSize(counts, maxSymbol)) {
        table = previousTable;
        reuseTable = true;
        serializedTableSize = 0;
        context.discardTemporaryTable();
      } else {
        table = newTable;
        reuseTable = false;
      }
    }

    final int compressedSize;
    final boolean singleStream = literalsSize < 256;
    if (singleStream) {
      compressedSize = HuffmanCompressor.compressSingleStream(outputBase,
                                                              outputAddress +
                                                              headerSize +
                                                              serializedTableSize,
                                                              outputSize -
                                                              headerSize -
                                                              serializedTableSize,
                                                              literals,
                                                              literalsAddress,
                                                              literalsSize,
                                                              table);
    } else {
      compressedSize = HuffmanCompressor.compress4streams(outputBase,
                                                          outputAddress +
                                                          headerSize +
                                                          serializedTableSize,
                                                          outputSize -
                                                          headerSize -
                                                          serializedTableSize,
                                                          literals,
                                                          literalsAddress,
                                                          literalsSize, table);
    }

    final int totalSize = serializedTableSize + compressedSize;
    final int minimumGain =
        calculateMinimumGain(literalsSize, parameters.getStrategy());

    if (totalSize >= literalsSize - minimumGain) {
      // incompressible or no savings

      // discard any temporary table we might have borrowed above
      context.discardTemporaryTable();

      return rawLiterals(outputBase, outputAddress, outputSize, literals,
                         literalsSize);
    }

    final int encodingType =
        reuseTable? TREELESS_LITERALS_BLOCK : COMPRESSED_LITERALS_BLOCK;

    // Build header
    switch (headerSize) {
      case 3: { // 2 - 2 - 10 - 10
        final int header =
            encodingType | ((singleStream? 0 : 1) << 2) | (literalsSize << 4) |
            (totalSize << 14);
        put24BitLittleEndian(outputBase, outputAddress, header);
        break;
      }
      case 4: { // 2 - 2 - 14 - 14
        final int header =
            encodingType | (2 << 2) | (literalsSize << 4) | (totalSize << 18);
        UNSAFE.putInt(outputBase, outputAddress, header);
        break;
      }
      case 5: { // 2 - 2 - 18 - 18
        final int header =
            encodingType | (3 << 2) | (literalsSize << 4) | (totalSize << 22);
        UNSAFE.putInt(outputBase, outputAddress, header);
        UNSAFE.putByte(outputBase, outputAddress + SIZE_OF_INT,
                       (byte) (totalSize >>> 10));
        break;
      }
      default:  // not possible : headerSize is {3,4,5}
        throw new IllegalStateException();
    }

    return headerSize + totalSize;
  }

  private static int rleLiterals(final Object outputBase,
                                 final long outputAddress,
                                 final Object inputBase, final int inputSize) {
    final int headerSize =
        1 + (inputSize > 31? 1 : 0) + (inputSize > 4095? 1 : 0);

    switch (headerSize) {
      case 1: // 2 - 1 - 5
        UNSAFE.putByte(outputBase, outputAddress,
                       (byte) (RLE_LITERALS_BLOCK | (inputSize << 3)));
        break;
      case 2: // 2 - 2 - 12
        UNSAFE.putShort(outputBase, outputAddress,
                        (short) (RLE_LITERALS_BLOCK | (1 << 2) |
                                 (inputSize << 4)));
        break;
      case 3: // 2 - 2 - 20
        UNSAFE.putInt(outputBase, outputAddress,
                      RLE_LITERALS_BLOCK | 3 << 2 | inputSize << 4);
        break;
      default:   // impossible. headerSize is {1,2,3}
        throw new IllegalStateException();
    }

    UNSAFE.putByte(outputBase, outputAddress + headerSize, UNSAFE
        .getByte(inputBase, sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET));

    return headerSize + 1;
  }

  private static int calculateMinimumGain(final int inputSize,
                                          final CompressionParameters.Strategy strategy) {
    // TODO: move this to Strategy to avoid hardcoding a specific strategy here
    final int minLog =
        strategy == CompressionParameters.Strategy.BTULTRA? 7 : 6;
    return (inputSize >>> minLog) + 2;
  }

  private static int rawLiterals(final Object outputBase,
                                 final long outputAddress, final int outputSize,
                                 final Object inputBase, final int inputSize) {
    int headerSize = 1;
    if (inputSize >= 32) {
      headerSize++;
    }
    if (inputSize >= 4096) {
      headerSize++;
    }

    checkArgument(inputSize + headerSize <= outputSize,
                  OUTPUT_BUFFER_TOO_SMALL);

    switch (headerSize) {
      case 1:
        UNSAFE.putByte(outputBase, outputAddress,
                       (byte) (RAW_LITERALS_BLOCK | (inputSize << 3)));
        break;
      case 2:
        UNSAFE.putShort(outputBase, outputAddress,
                        (short) (RAW_LITERALS_BLOCK | (1 << 2) |
                                 (inputSize << 4)));
        break;
      case 3:
        put24BitLittleEndian(outputBase, outputAddress,
                             RAW_LITERALS_BLOCK | (3 << 2) | (inputSize << 4));
        break;
      default:
        throw new AssertionError();
    }

    // TODO: ensure this test is correct
    checkArgument(inputSize + 1 <= outputSize, OUTPUT_BUFFER_TOO_SMALL);

    UNSAFE.copyMemory(inputBase, sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET,
                      outputBase, outputAddress + headerSize, inputSize);

    return headerSize + inputSize;
  }
}
