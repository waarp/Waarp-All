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

import org.waarp.compress.MalformedInputException;

import java.util.Arrays;

import static org.waarp.compress.zstdsafe.BitInputStream.*;
import static org.waarp.compress.zstdsafe.Constants.*;
import static org.waarp.compress.zstdsafe.UnsafeUtil.*;
import static org.waarp.compress.zstdsafe.Util.*;

class ZstdFrameDecompressor {
  private static final int[] DEC_32_TABLE = { 4, 1, 2, 1, 4, 4, 4, 4 };
  private static final int[] DEC_64_TABLE = { 0, 0, 0, -1, 0, 1, 2, 3 };

  private static final int V07_MAGIC_NUMBER = 0xFD2FB527;

  private static final int MAX_WINDOW_SIZE = 1 << 23;

  private static final int[] LITERALS_LENGTH_BASE = {
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 18, 20, 22, 24,
      28, 32, 40, 48, 64, 0x80, 0x100, 0x200, 0x400, 0x800, 0x1000, 0x2000,
      0x4000, 0x8000, 0x10000
  };

  private static final int[] MATCH_LENGTH_BASE = {
      3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22,
      23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 37, 39, 41, 43, 47,
      51, 59, 67, 83, 99, 0x83, 0x103, 0x203, 0x403, 0x803, 0x1003, 0x2003,
      0x4003, 0x8003, 0x10003
  };

  private static final int[] OFFSET_CODES_BASE = {
      0, 1, 1, 5, 0xD, 0x1D, 0x3D, 0x7D, 0xFD, 0x1FD, 0x3FD, 0x7FD, 0xFFD,
      0x1FFD, 0x3FFD, 0x7FFD, 0xFFFD, 0x1FFFD, 0x3FFFD, 0x7FFFD, 0xFFFFD,
      0x1FFFFD, 0x3FFFFD, 0x7FFFFD, 0xFFFFFD, 0x1FFFFFD, 0x3FFFFFD, 0x7FFFFFD,
      0xFFFFFFD
  };

  private static final FiniteStateEntropy.Table DEFAULT_LITERALS_LENGTH_TABLE =
      new FiniteStateEntropy.Table(6, new int[] {
          0, 16, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 0,
          32, 0, 0, 32, 0, 32, 0, 32, 0, 0, 32, 0, 32, 0, 32, 0, 0, 16, 32, 0,
          0, 48, 16, 32, 32, 32, 32, 32, 32, 32, 32, 0, 32, 32, 32, 32, 32, 32,
          0, 0, 0, 0
      }, new byte[] {
          0, 0, 1, 3, 4, 6, 7, 9, 10, 12, 14, 16, 18, 19, 21, 22, 24, 25, 26,
          27, 29, 31, 0, 1, 2, 4, 5, 7, 8, 10, 11, 13, 16, 17, 19, 20, 22, 23,
          25, 25, 26, 28, 30, 0, 1, 2, 3, 5, 6, 8, 9, 11, 12, 15, 17, 18, 20,
          21, 23, 24, 35, 34, 33, 32
      }, new byte[] {
          4, 4, 5, 5, 5, 5, 5, 5, 5, 5, 6, 5, 5, 5, 5, 5, 5, 5, 5, 6, 6, 6, 4,
          4, 5, 5, 5, 5, 5, 5, 5, 6, 5, 5, 5, 5, 5, 5, 4, 4, 5, 6, 6, 4, 4, 5,
          5, 5, 5, 5, 5, 5, 5, 6, 5, 5, 5, 5, 5, 5, 6, 6, 6, 6
      });

  private static final FiniteStateEntropy.Table DEFAULT_OFFSET_CODES_TABLE =
      new FiniteStateEntropy.Table(5, new int[] {
          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 16, 0, 0,
          0, 16, 0, 0, 0, 0, 0, 0, 0
      }, new byte[] {
          0, 6, 9, 15, 21, 3, 7, 12, 18, 23, 5, 8, 14, 20, 2, 7, 11, 17, 22, 4,
          8, 13, 19, 1, 6, 10, 16, 28, 27, 26, 25, 24
      }, new byte[] {
          5, 4, 5, 5, 5, 5, 4, 5, 5, 5, 5, 4, 5, 5, 5, 4, 5, 5, 5, 5, 4, 5, 5,
          5, 4, 5, 5, 5, 5, 5, 5, 5
      });

  private static final FiniteStateEntropy.Table DEFAULT_MATCH_LENGTH_TABLE =
      new FiniteStateEntropy.Table(6, new int[] {
          0, 0, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16,
          0, 32, 0, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 48,
          16, 32, 32, 32, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
      }, new byte[] {
          0, 1, 2, 3, 5, 6, 8, 10, 13, 16, 19, 22, 25, 28, 31, 33, 35, 37, 39,
          41, 43, 45, 1, 2, 3, 4, 6, 7, 9, 12, 15, 18, 21, 24, 27, 30, 32, 34,
          36, 38, 40, 42, 44, 1, 1, 2, 4, 5, 7, 8, 11, 14, 17, 20, 23, 26, 29,
          52, 51, 50, 49, 48, 47, 46
      }, new byte[] {
          6, 4, 5, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4,
          4, 5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 4, 4, 4,
          5, 5, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6
      });
  public static final String NOT_ENOUGH_INPUT_BYTES = "Not enough input bytes";
  public static final String OUTPUT_BUFFER_TOO_SMALL =
      "Output buffer too small";
  public static final String EXPECTED_MATCH_LENGTH_TABLE_TO_BE_PRESENT =
      "Expected match length table to be present";
  public static final String INPUT_IS_CORRUPTED = "Input is corrupted";
  public static final String VALUE_EXCEEDS_EXPECTED_MAXIMUM_VALUE =
      "Value exceeds expected maximum value";

  private final byte[] literals = new byte[MAX_BLOCK_SIZE + SIZE_OF_LONG];
  // extra space to allow for long-at-a-time copy

  // current buffer containing literals
  private byte[] literalsBase;
  private int literalsAddress;
  private int literalsLimit;

  private final int[] previousOffsets = new int[3];

  private final FiniteStateEntropy.Table literalsLengthTable =
      new FiniteStateEntropy.Table(LITERAL_LENGTH_TABLE_LOG);
  private final FiniteStateEntropy.Table offsetCodesTable =
      new FiniteStateEntropy.Table(OFFSET_TABLE_LOG);
  private final FiniteStateEntropy.Table matchLengthTable =
      new FiniteStateEntropy.Table(MATCH_LENGTH_TABLE_LOG);

  private FiniteStateEntropy.Table currentLiteralsLengthTable;
  private FiniteStateEntropy.Table currentOffsetCodesTable;
  private FiniteStateEntropy.Table currentMatchLengthTable;

  private final Huffman huffman = new Huffman();
  private final FseTableReader fse = new FseTableReader();

  public int decompress(final byte[] inputBase, final int inputAddress,
                        final int inputLimit, final byte[] outputBase,
                        final int outputAddress, final int outputLimit) {
    if (outputAddress == outputLimit) {
      return 0;
    }

    int input = inputAddress;
    int output = outputAddress;

    while (input < inputLimit) {
      reset();
      final int outputStart = output;
      input += verifyMagic(inputBase, inputAddress, inputLimit);

      final FrameHeader frameHeader =
          readFrameHeader(inputBase, input, inputLimit);
      input += frameHeader.headerSize;

      boolean lastBlock;
      do {
        verify(input + SIZE_OF_BLOCK_HEADER <= inputLimit, input,
               NOT_ENOUGH_INPUT_BYTES);

        // read block header
        final int header = getInt(inputBase, input) & 0xFFFFFF;
        input += SIZE_OF_BLOCK_HEADER;

        lastBlock = (header & 1) != 0;
        final int blockType = (header >>> 1) & 0x3;
        final int blockSize = (header >>> 3) & 0x1FFFFF; // 21 bits

        final int decodedSize;
        switch (blockType) {
          case RAW_BLOCK:
            verify(inputAddress + blockSize <= inputLimit, input,
                   NOT_ENOUGH_INPUT_BYTES);
            decodedSize =
                decodeRawBlock(inputBase, input, blockSize, outputBase, output,
                               outputLimit);
            input += blockSize;
            break;
          case RLE_BLOCK:
            verify(inputAddress + 1 <= inputLimit, input,
                   NOT_ENOUGH_INPUT_BYTES);
            decodedSize =
                decodeRleBlock(blockSize, inputBase, input, outputBase, output,
                               outputLimit);
            input += 1;
            break;
          case COMPRESSED_BLOCK:
            verify(inputAddress + blockSize <= inputLimit, input,
                   NOT_ENOUGH_INPUT_BYTES);
            decodedSize =
                decodeCompressedBlock(inputBase, input, blockSize, outputBase,
                                      output, outputLimit,
                                      frameHeader.windowSize, outputAddress);
            input += blockSize;
            break;
          default:
            throw fail(input, "Invalid block type");
        }

        output += decodedSize;
      } while (!lastBlock);

      if (frameHeader.hasChecksum) {
        final int decodedFrameSize = output - outputStart;

        final long hash =
            XxHash64.hash(0, outputBase, outputStart, decodedFrameSize);

        final int checksum = getInt(inputBase, input);
        if (checksum != (int) hash) {
          throw new MalformedInputException(input, String
              .format("Bad checksum. Expected: %s, actual: %s",
                      Integer.toHexString(checksum),
                      Integer.toHexString((int) hash)));
        }

        input += SIZE_OF_INT;
      }
    }

    return output - outputAddress;
  }

  private void reset() {
    previousOffsets[0] = 1;
    previousOffsets[1] = 4;
    previousOffsets[2] = 8;

    currentLiteralsLengthTable = null;
    currentOffsetCodesTable = null;
    currentMatchLengthTable = null;
  }

  private static int decodeRawBlock(final byte[] inputBase,
                                    final int inputAddress, final int blockSize,
                                    final byte[] outputBase,
                                    final int outputAddress,
                                    final int outputLimit) {
    verify(outputAddress + blockSize <= outputLimit, inputAddress,
           OUTPUT_BUFFER_TOO_SMALL);

    copyMemory(inputBase, inputAddress, outputBase, outputAddress, blockSize);
    return blockSize;
  }

  private static int decodeRleBlock(final int size, final byte[] inputBase,
                                    final int inputAddress,
                                    final byte[] outputBase,
                                    final int outputAddress,
                                    final int outputLimit) {
    verify(outputAddress + size <= outputLimit, inputAddress,
           OUTPUT_BUFFER_TOO_SMALL);

    int output = outputAddress;
    final long value = inputBase[inputAddress] & 0xFFL;

    int remaining = size;
    if (remaining >= SIZE_OF_LONG) {
      final long packed =
          value | (value << 8) | (value << 16) | (value << 24) | (value << 32) |
          (value << 40) | (value << 48) | (value << 56);

      do {
        putLong(outputBase, output, packed);
        output += SIZE_OF_LONG;
        remaining -= SIZE_OF_LONG;
      } while (remaining >= SIZE_OF_LONG);
    }

    for (int i = 0; i < remaining; i++) {
      outputBase[output] = (byte) value;
      output++;
    }

    return size;
  }

  private int decodeCompressedBlock(final byte[] inputBase,
                                    final int inputAddress, final int blockSize,
                                    final byte[] outputBase,
                                    final int outputAddress,
                                    final int outputLimit, final int windowSize,
                                    final int outputAbsoluteBaseAddress) {
    final int inputLimit = inputAddress + blockSize;
    int input = inputAddress;

    verify(blockSize <= MAX_BLOCK_SIZE, input,
           EXPECTED_MATCH_LENGTH_TABLE_TO_BE_PRESENT);
    verify(blockSize >= MIN_BLOCK_SIZE, input,
           "Compressed block size too small");

    // decode literals
    final int literalsBlockType = inputBase[input] & 0x3;

    switch (literalsBlockType) {
      case RAW_LITERALS_BLOCK: {
        input += decodeRawLiterals(inputBase, input, inputLimit);
        break;
      }
      case RLE_LITERALS_BLOCK: {
        input += decodeRleLiterals(inputBase, input, blockSize);
        break;
      }
      case TREELESS_LITERALS_BLOCK:
        verify(huffman.isLoaded(), input, "Dictionary is corrupted");
      case COMPRESSED_LITERALS_BLOCK: {
        input += decodeCompressedLiterals(inputBase, input, blockSize,
                                          literalsBlockType);
        break;
      }
      default:
        throw fail(input, "Invalid literals block encoding type");
    }

    verify(windowSize <= MAX_WINDOW_SIZE, input,
           "Window size too large (not yet supported)");

    return decompressSequences(inputBase, input, inputAddress + blockSize,
                               outputBase, outputAddress, outputLimit,
                               literalsBase, literalsAddress, literalsLimit,
                               outputAbsoluteBaseAddress);
  }

  private int decompressSequences(final byte[] inputBase,
                                  final int inputAddress, final int inputLimit,
                                  final byte[] outputBase,
                                  final int outputAddress,
                                  final int outputLimit,
                                  final byte[] literalsBase,
                                  final int literalsAddress,
                                  final int literalsLimit,
                                  final int outputAbsoluteBaseAddress) {
    final int fastOutputLimit = outputLimit - SIZE_OF_LONG;
    final int fastMatchOutputLimit = fastOutputLimit - SIZE_OF_LONG;

    int input = inputAddress;
    int output = outputAddress;

    int literalsInput = literalsAddress;

    final int size = inputLimit - inputAddress;
    verify(size >= MIN_SEQUENCES_SIZE, input, NOT_ENOUGH_INPUT_BYTES);

    // decode header
    int sequenceCount = inputBase[input++] & 0xFF;
    if (sequenceCount != 0) {
      if (sequenceCount == 255) {
        verify(input + SIZE_OF_SHORT <= inputLimit, input,
               NOT_ENOUGH_INPUT_BYTES);
        sequenceCount =
            (getShort(inputBase, input) & 0xFFFF) + LONG_NUMBER_OF_SEQUENCES;
        input += SIZE_OF_SHORT;
      } else if (sequenceCount > 127) {
        verify(input < inputLimit, input, NOT_ENOUGH_INPUT_BYTES);
        sequenceCount =
            ((sequenceCount - 128) << 8) + (inputBase[input++] & 0xFF);
      }

      verify(input + SIZE_OF_INT <= inputLimit, input, NOT_ENOUGH_INPUT_BYTES);

      final byte type = inputBase[input++];

      final int literalsLengthType = (type & 0xFF) >>> 6;
      final int offsetCodesType = (type >>> 4) & 0x3;
      final int matchLengthType = (type >>> 2) & 0x3;

      input = computeLiteralsTable(literalsLengthType, inputBase, input,
                                   inputLimit);
      input =
          computeOffsetsTable(offsetCodesType, inputBase, input, inputLimit);
      input = computeMatchLengthTable(matchLengthType, inputBase, input,
                                      inputLimit);

      // decompress sequences
      final BitInputStream.Initializer initializer =
          new BitInputStream.Initializer(inputBase, input, inputLimit);
      initializer.initialize();
      int bitsConsumed = initializer.getBitsConsumed();
      long bits = initializer.getBits();
      int currentAddress = initializer.getCurrentAddress();

      final FiniteStateEntropy.Table currentLiteralsLengthTable1 =
          this.currentLiteralsLengthTable;
      final FiniteStateEntropy.Table currentOffsetCodesTable1 =
          this.currentOffsetCodesTable;
      final FiniteStateEntropy.Table currentMatchLengthTable1 =
          this.currentMatchLengthTable;

      int literalsLengthState = (int) peekBits(bitsConsumed, bits,
                                               currentLiteralsLengthTable1.log2Size);
      bitsConsumed += currentLiteralsLengthTable1.log2Size;

      int offsetCodesState =
          (int) peekBits(bitsConsumed, bits, currentOffsetCodesTable1.log2Size);
      bitsConsumed += currentOffsetCodesTable1.log2Size;

      int matchLengthState =
          (int) peekBits(bitsConsumed, bits, currentMatchLengthTable1.log2Size);
      bitsConsumed += currentMatchLengthTable1.log2Size;

      final int[] previousOffsets1 = this.previousOffsets;

      final byte[] literalsLengthNumbersOfBits =
          currentLiteralsLengthTable1.numberOfBits;
      final int[] literalsLengthNewStates =
          currentLiteralsLengthTable1.newState;
      final byte[] literalsLengthSymbols = currentLiteralsLengthTable1.symbol;

      final byte[] matchLengthNumbersOfBits =
          currentMatchLengthTable1.numberOfBits;
      final int[] matchLengthNewStates = currentMatchLengthTable1.newState;
      final byte[] matchLengthSymbols = currentMatchLengthTable1.symbol;

      final byte[] offsetCodesNumbersOfBits =
          currentOffsetCodesTable1.numberOfBits;
      final int[] offsetCodesNewStates = currentOffsetCodesTable1.newState;
      final byte[] offsetCodesSymbols = currentOffsetCodesTable1.symbol;

      while (sequenceCount > 0) {
        sequenceCount--;

        final BitInputStream.Loader loader =
            new BitInputStream.Loader(inputBase, input, currentAddress, bits,
                                      bitsConsumed);
        loader.load();
        bitsConsumed = loader.getBitsConsumed();
        bits = loader.getBits();
        currentAddress = loader.getCurrentAddress();
        if (loader.isOverflow()) {
          verify(sequenceCount == 0, input, "Not all sequences were consumed");
          break;
        }

        // decode sequence
        final int literalsLengthCode =
            literalsLengthSymbols[literalsLengthState];
        final int matchLengthCode = matchLengthSymbols[matchLengthState];
        final int offsetCode = offsetCodesSymbols[offsetCodesState];

        final int literalsLengthBits = LITERALS_LENGTH_BITS[literalsLengthCode];
        final int matchLengthBits = MATCH_LENGTH_BITS[matchLengthCode];

        int offset = OFFSET_CODES_BASE[offsetCode];
        if (offsetCode > 0) {
          offset += peekBits(bitsConsumed, bits, offsetCode);
          bitsConsumed += offsetCode;
        }

        if (offsetCode <= 1) {
          if (literalsLengthCode == 0) {
            offset++;
          }

          if (offset != 0) {
            int temp;
            if (offset == 3) {
              temp = previousOffsets1[0] - 1;
            } else {
              temp = previousOffsets1[offset];
            }

            if (temp == 0) {
              temp = 1;
            }

            if (offset != 1) {
              previousOffsets1[2] = previousOffsets1[1];
            }
            previousOffsets1[1] = previousOffsets1[0];
            previousOffsets1[0] = temp;

            offset = temp;
          } else {
            offset = previousOffsets1[0];
          }
        } else {
          previousOffsets1[2] = previousOffsets1[1];
          previousOffsets1[1] = previousOffsets1[0];
          previousOffsets1[0] = offset;
        }

        int matchLength = MATCH_LENGTH_BASE[matchLengthCode];
        if (matchLengthCode > 31) {
          matchLength += peekBits(bitsConsumed, bits, matchLengthBits);
          bitsConsumed += matchLengthBits;
        }

        int literalsLength = LITERALS_LENGTH_BASE[literalsLengthCode];
        if (literalsLengthCode > 15) {
          literalsLength += peekBits(bitsConsumed, bits, literalsLengthBits);
          bitsConsumed += literalsLengthBits;
        }

        final int totalBits = literalsLengthBits + matchLengthBits + offsetCode;
        if (totalBits > 64 - 7 -
                        (LITERAL_LENGTH_TABLE_LOG + MATCH_LENGTH_TABLE_LOG +
                         OFFSET_TABLE_LOG)) {
          final BitInputStream.Loader loader1 =
              new BitInputStream.Loader(inputBase, input, currentAddress, bits,
                                        bitsConsumed);
          loader1.load();

          bitsConsumed = loader1.getBitsConsumed();
          bits = loader1.getBits();
          currentAddress = loader1.getCurrentAddress();
        }

        int numberOfBits;

        numberOfBits = literalsLengthNumbersOfBits[literalsLengthState];
        literalsLengthState =
            (int) (literalsLengthNewStates[literalsLengthState] +
                   peekBits(bitsConsumed, bits, numberOfBits)); // <= 9 bits
        bitsConsumed += numberOfBits;

        numberOfBits = matchLengthNumbersOfBits[matchLengthState];
        matchLengthState = (int) (matchLengthNewStates[matchLengthState] +
                                  peekBits(bitsConsumed, bits,
                                           numberOfBits)); // <= 9 bits
        bitsConsumed += numberOfBits;

        numberOfBits = offsetCodesNumbersOfBits[offsetCodesState];
        offsetCodesState = (int) (offsetCodesNewStates[offsetCodesState] +
                                  peekBits(bitsConsumed, bits,
                                           numberOfBits)); // <= 8 bits
        bitsConsumed += numberOfBits;

        final int literalOutputLimit = output + literalsLength;
        final int matchOutputLimit = literalOutputLimit + matchLength;

        verify(matchOutputLimit <= outputLimit, input, OUTPUT_BUFFER_TOO_SMALL);
        final int literalEnd = literalsInput + literalsLength;
        verify(literalEnd <= literalsLimit, input, INPUT_IS_CORRUPTED);

        final int matchAddress = literalOutputLimit - offset;
        verify(matchAddress >= outputAbsoluteBaseAddress, input,
               INPUT_IS_CORRUPTED);

        if (literalOutputLimit > fastOutputLimit) {
          executeLastSequence(outputBase, output, literalOutputLimit,
                              matchOutputLimit, fastOutputLimit, literalsInput,
                              matchAddress);
        } else {
          // copy literals. literalOutputLimit <= fastOutputLimit, so we can copy
          // long at a time with over-copy
          output = copyLiterals(outputBase, literalsBase, output, literalsInput,
                                literalOutputLimit);
          copyMatch(outputBase, fastOutputLimit, output, offset,
                    matchOutputLimit, matchAddress, matchLength,
                    fastMatchOutputLimit);
        }
        output = matchOutputLimit;
        literalsInput = literalEnd;
      }
    }

    // last literal segment
    output = copyLastLiteral(outputBase, literalsBase, literalsLimit, output,
                             literalsInput);

    return output - outputAddress;
  }

  private int copyLastLiteral(final byte[] outputBase,
                              final byte[] literalsBase,
                              final int literalsLimit, int output,
                              final int literalsInput) {
    final int lastLiteralsSize = literalsLimit - literalsInput;
    copyMemory(literalsBase, literalsInput, outputBase, output,
               lastLiteralsSize);
    output += lastLiteralsSize;
    return output;
  }

  private void copyMatch(final byte[] outputBase, final int fastOutputLimit,
                         int output, final int offset,
                         final int matchOutputLimit, int matchAddress,
                         int matchLength, final int fastMatchOutputLimit) {
    matchAddress = copyMatchHead(outputBase, output, offset, matchAddress);
    output += SIZE_OF_LONG;
    matchLength -= SIZE_OF_LONG; // first 8 bytes copied above

    copyMatchTail(outputBase, fastOutputLimit, output, matchOutputLimit,
                  matchAddress, matchLength, fastMatchOutputLimit);
  }

  private void copyMatchTail(final byte[] outputBase, final int fastOutputLimit,
                             int output, final int matchOutputLimit,
                             int matchAddress, final int matchLength,
                             final int fastMatchOutputLimit) {
    // fastMatchOutputLimit is just fastOutputLimit - SIZE_OF_LONG. It needs to be passed in so that it can be computed once for the
    // whole invocation to decompressSequences. Otherwise, we'd just compute it here.
    // If matchOutputLimit is < fastMatchOutputLimit, we know that even after the head (8 bytes) has been copied, the output pointer
    // will be within fastOutputLimit, so it's safe to copy blindly before checking the limit condition
    if (matchOutputLimit < fastMatchOutputLimit) {
      int copied = 0;
      do {
        putLong(outputBase, output, getLong(outputBase, matchAddress));
        output += SIZE_OF_LONG;
        matchAddress += SIZE_OF_LONG;
        copied += SIZE_OF_LONG;
      } while (copied < matchLength);
    } else {
      while (output < fastOutputLimit) {
        putLong(outputBase, output, getLong(outputBase, matchAddress));
        matchAddress += SIZE_OF_LONG;
        output += SIZE_OF_LONG;
      }

      while (output < matchOutputLimit) {
        outputBase[output++] = outputBase[matchAddress++];
      }
    }
  }

  private int copyMatchHead(final byte[] outputBase, final int output,
                            final int offset, int matchAddress) {
    // copy match
    if (offset < 8) {
      // 8 bytes apart so that we can copy long-at-a-time below
      final int increment32 = DEC_32_TABLE[offset];
      final int decrement64 = DEC_64_TABLE[offset];

      outputBase[output] = outputBase[matchAddress];
      outputBase[output + 1] = outputBase[matchAddress + 1];
      outputBase[output + 2] = outputBase[matchAddress + 2];
      outputBase[output + 3] = outputBase[matchAddress + 3];
      matchAddress += increment32;

      putInt(outputBase, output + 4, getInt(outputBase, matchAddress));
      matchAddress -= decrement64;
    } else {
      putLong(outputBase, output, getLong(outputBase, matchAddress));
      matchAddress += SIZE_OF_LONG;
    }
    return matchAddress;
  }

  private int copyLiterals(final byte[] outputBase, final byte[] literalsBase,
                           int output, final int literalsInput,
                           final int literalOutputLimit) {
    int literalInput = literalsInput;
    do {
      putLong(outputBase, output, getLong(literalsBase, literalInput));
      output += SIZE_OF_LONG;
      literalInput += SIZE_OF_LONG;
    } while (output < literalOutputLimit);
    output = literalOutputLimit; // correction in case we over-copied
    return output;
  }

  private int computeMatchLengthTable(final int matchLengthType,
                                      final byte[] inputBase, int input,
                                      final int inputLimit) {
    switch (matchLengthType) {
      case SEQUENCE_ENCODING_RLE:
        verify(input < inputLimit, input, NOT_ENOUGH_INPUT_BYTES);

        final byte value = inputBase[input++];
        verify(value <= MAX_MATCH_LENGTH_SYMBOL, input,
               VALUE_EXCEEDS_EXPECTED_MAXIMUM_VALUE);

        FseTableReader.initializeRleTable(matchLengthTable, value);
        currentMatchLengthTable = matchLengthTable;
        break;
      case SEQUENCE_ENCODING_BASIC:
        currentMatchLengthTable = DEFAULT_MATCH_LENGTH_TABLE;
        break;
      case SEQUENCE_ENCODING_REPEAT:
        verify(currentMatchLengthTable != null, input,
               EXPECTED_MATCH_LENGTH_TABLE_TO_BE_PRESENT);
        break;
      case SEQUENCE_ENCODING_COMPRESSED:
        input +=
            fse.readFseTable(matchLengthTable, inputBase, input, inputLimit,
                             MAX_MATCH_LENGTH_SYMBOL, MATCH_LENGTH_TABLE_LOG);
        currentMatchLengthTable = matchLengthTable;
        break;
      default:
        throw fail(input, "Invalid match length encoding type");
    }
    return input;
  }

  private int computeOffsetsTable(final int offsetCodesType,
                                  final byte[] inputBase, int input,
                                  final int inputLimit) {
    switch (offsetCodesType) {
      case SEQUENCE_ENCODING_RLE:
        verify(input < inputLimit, input, NOT_ENOUGH_INPUT_BYTES);

        final byte value = inputBase[input++];
        verify(value <= DEFAULT_MAX_OFFSET_CODE_SYMBOL, input,
               VALUE_EXCEEDS_EXPECTED_MAXIMUM_VALUE);

        FseTableReader.initializeRleTable(offsetCodesTable, value);
        currentOffsetCodesTable = offsetCodesTable;
        break;
      case SEQUENCE_ENCODING_BASIC:
        currentOffsetCodesTable = DEFAULT_OFFSET_CODES_TABLE;
        break;
      case SEQUENCE_ENCODING_REPEAT:
        verify(currentOffsetCodesTable != null, input,
               EXPECTED_MATCH_LENGTH_TABLE_TO_BE_PRESENT);
        break;
      case SEQUENCE_ENCODING_COMPRESSED:
        input +=
            fse.readFseTable(offsetCodesTable, inputBase, input, inputLimit,
                             DEFAULT_MAX_OFFSET_CODE_SYMBOL, OFFSET_TABLE_LOG);
        currentOffsetCodesTable = offsetCodesTable;
        break;
      default:
        throw fail(input, "Invalid offset code encoding type");
    }
    return input;
  }

  private int computeLiteralsTable(final int literalsLengthType,
                                   final byte[] inputBase, int input,
                                   final int inputLimit) {
    switch (literalsLengthType) {
      case SEQUENCE_ENCODING_RLE:
        verify(input < inputLimit, input, NOT_ENOUGH_INPUT_BYTES);

        final byte value = inputBase[input++];
        verify(value <= MAX_LITERALS_LENGTH_SYMBOL, input,
               VALUE_EXCEEDS_EXPECTED_MAXIMUM_VALUE);

        FseTableReader.initializeRleTable(literalsLengthTable, value);
        currentLiteralsLengthTable = literalsLengthTable;
        break;
      case SEQUENCE_ENCODING_BASIC:
        currentLiteralsLengthTable = DEFAULT_LITERALS_LENGTH_TABLE;
        break;
      case SEQUENCE_ENCODING_REPEAT:
        verify(currentLiteralsLengthTable != null, input,
               EXPECTED_MATCH_LENGTH_TABLE_TO_BE_PRESENT);
        break;
      case SEQUENCE_ENCODING_COMPRESSED:
        input +=
            fse.readFseTable(literalsLengthTable, inputBase, input, inputLimit,
                             MAX_LITERALS_LENGTH_SYMBOL,
                             LITERAL_LENGTH_TABLE_LOG);
        currentLiteralsLengthTable = literalsLengthTable;
        break;
      default:
        throw fail(input, "Invalid literals length encoding type");
    }
    return input;
  }

  private void executeLastSequence(final byte[] outputBase, int output,
                                   final int literalOutputLimit,
                                   final int matchOutputLimit,
                                   final int fastOutputLimit, int literalInput,
                                   int matchAddress) {
    // copy literals
    if (output < fastOutputLimit) {
      // wild copy
      do {
        putLong(outputBase, output, getLong(literalsBase, literalInput));
        output += SIZE_OF_LONG;
        literalInput += SIZE_OF_LONG;
      } while (output < fastOutputLimit);

      literalInput -= output - fastOutputLimit;
      output = fastOutputLimit;
    }

    while (output < literalOutputLimit) {
      outputBase[output] = literalsBase[literalInput];
      output++;
      literalInput++;
    }

    // copy match
    while (output < matchOutputLimit) {
      outputBase[output] = outputBase[matchAddress];
      output++;
      matchAddress++;
    }
  }

  private int decodeCompressedLiterals(final byte[] inputBase,
                                       final int inputAddress,
                                       final int blockSize,
                                       final int literalsBlockType) {
    int input = inputAddress;
    verify(blockSize >= 5, input, NOT_ENOUGH_INPUT_BYTES);

    // compressed
    final int compressedSize;
    final int uncompressedSize;
    boolean singleStream = false;
    final int headerSize;
    final int type = (inputBase[input] >> 2) & 0x3;
    switch (type) {
      case 0:
        singleStream = true;
      case 1: {
        final int header = getInt(inputBase, input);

        headerSize = 3;
        uncompressedSize = (header >>> 4) & mask(10);
        compressedSize = (header >>> 14) & mask(10);
        break;
      }
      case 2: {
        final int header = getInt(inputBase, input);

        headerSize = 4;
        uncompressedSize = (header >>> 4) & mask(14);
        compressedSize = (header >>> 18) & mask(14);
        break;
      }
      case 3: {
        // read 5 little-endian bytes
        final long header = inputBase[input] & 0xFF |
                            (getInt(inputBase, input + 1) & 0xFFFFFFFFL) << 8;

        headerSize = 5;
        uncompressedSize = (int) ((header >>> 4) & mask(18));
        compressedSize = (int) ((header >>> 22) & mask(18));
        break;
      }
      default:
        throw fail(input, "Invalid literals header size type");
    }

    verify(uncompressedSize <= MAX_BLOCK_SIZE, input,
           "Block exceeds maximum size");
    verify(headerSize + compressedSize <= blockSize, input, INPUT_IS_CORRUPTED);

    input += headerSize;

    final int inputLimit = input + compressedSize;
    if (literalsBlockType != TREELESS_LITERALS_BLOCK) {
      input += huffman.readTable(inputBase, input, compressedSize);
    }

    literalsBase = literals;
    literalsAddress = 0;
    literalsLimit = uncompressedSize;

    if (singleStream) {
      huffman.decodeSingleStream(inputBase, input, inputLimit, literals,
                                 literalsAddress, literalsLimit);
    } else {
      huffman.decode4Streams(inputBase, input, inputLimit, literals,
                             literalsAddress, literalsLimit);
    }

    return headerSize + compressedSize;
  }

  private int decodeRleLiterals(final byte[] inputBase, final int inputAddress,
                                final int blockSize) {
    int input = inputAddress;
    final int outputSize;

    final int type = (inputBase[input] >> 2) & 0x3;
    switch (type) {
      case 0:
      case 2:
        outputSize = (inputBase[input] & 0xFF) >>> 3;
        input++;
        break;
      case 1:
        outputSize = (getShort(inputBase, input) & 0xFFFF) >>> 4;
        input += 2;
        break;
      case 3:
        // we need at least 4 bytes (3 for the header, 1 for the payload)
        verify(blockSize >= SIZE_OF_INT, input, NOT_ENOUGH_INPUT_BYTES);
        outputSize = (getInt(inputBase, input) & 0xFFFFFF) >>> 4;
        input += 3;
        break;
      default:
        throw fail(input, "Invalid RLE literals header encoding type");
    }

    verify(outputSize <= MAX_BLOCK_SIZE, input,
           "Output exceeds maximum block size");

    final byte value = inputBase[input++];
    Arrays.fill(literals, 0, outputSize + SIZE_OF_LONG, value);

    literalsBase = literals;
    literalsAddress = 0;
    literalsLimit = outputSize;

    return input - inputAddress;
  }

  private int decodeRawLiterals(final byte[] inputBase, final int inputAddress,
                                final int inputLimit) {
    int input = inputAddress;
    final int type = (inputBase[input] >> 2) & 0x3;

    final int literalSize;
    switch (type) {
      case 0:
      case 2:
        literalSize = (inputBase[input] & 0xFF) >>> 3;
        input++;
        break;
      case 1:
        literalSize = (getShort(inputBase, input) & 0xFFFF) >>> 4;
        input += 2;
        break;
      case 3:
        // read 3 little-endian bytes
        final int header = ((inputBase[input] & 0xFF) |
                            ((getShort(inputBase, input + 1) & 0xFFFF) << 8));

        literalSize = header >>> 4;
        input += 3;
        break;
      default:
        throw fail(input, "Invalid raw literals header encoding type");
    }

    verify(input + literalSize <= inputLimit, input, NOT_ENOUGH_INPUT_BYTES);

    // Set literals pointer to [input, literalSize], but only if we can copy 8 bytes at a time during sequence decoding
    // Otherwise, copy literals into buffer that's big enough to guarantee that
    if (literalSize > (inputLimit - input) - SIZE_OF_LONG) {
      literalsBase = literals;
      literalsAddress = 0;
      literalsLimit = literalSize;

      copyMemory(inputBase, input, literals, literalsAddress, literalSize);
      Arrays.fill(literals, literalSize, literalSize + SIZE_OF_LONG, (byte) 0);
    } else {
      literalsBase = inputBase;
      literalsAddress = input;
      literalsLimit = literalsAddress + literalSize;
    }
    input += literalSize;

    return input - inputAddress;
  }

  static FrameHeader readFrameHeader(final byte[] inputBase,
                                     final int inputAddress,
                                     final int inputLimit) {
    int input = inputAddress;
    verify(input < inputLimit, input, NOT_ENOUGH_INPUT_BYTES);

    final int frameHeaderDescriptor = inputBase[input++] & 0xFF;
    final boolean singleSegment = (frameHeaderDescriptor & 0x20) != 0;
    final int dictionaryDescriptor = frameHeaderDescriptor & 0x3;
    final int contentSizeDescriptor = frameHeaderDescriptor >>> 6;

    final int headerSize = 1 + (singleSegment? 0 : 1) +
                           (dictionaryDescriptor == 0? 0 :
                               (1 << (dictionaryDescriptor - 1))) +
                           (contentSizeDescriptor == 0? (singleSegment? 1 : 0) :
                               (1 << contentSizeDescriptor));

    verify(headerSize <= inputLimit - inputAddress, input,
           NOT_ENOUGH_INPUT_BYTES);

    // decode window size
    int windowSize = -1;
    if (!singleSegment) {
      final int windowDescriptor = inputBase[input++] & 0xFF;
      final int exponent = windowDescriptor >>> 3;
      final int mantissa = windowDescriptor & 0x7;

      final int base = 1 << (MIN_WINDOW_LOG + exponent);
      windowSize = base + (base / 8) * mantissa;
    }

    // decode dictionary id
    int dictionaryId = -1;
    switch (dictionaryDescriptor) {
      case 1:
        dictionaryId = inputBase[input] & 0xFF;
        input += SIZE_OF_BYTE;
        break;
      case 2:
        dictionaryId = getShort(inputBase, input) & 0xFFFF;
        input += SIZE_OF_SHORT;
        break;
      case 3:
        dictionaryId = getInt(inputBase, input);
        input += SIZE_OF_INT;
        break;
    }
    verify(dictionaryId == -1, input, "Custom dictionaries not supported");

    // decode content size
    int contentSize = -1;
    switch (contentSizeDescriptor) {
      case 0:
        if (singleSegment) {
          contentSize = inputBase[input] & 0xFF;
          input += SIZE_OF_BYTE;
        }
        break;
      case 1:
        contentSize = getShort(inputBase, input) & 0xFFFF;
        contentSize += 256;
        input += SIZE_OF_SHORT;
        break;
      case 2:
        contentSize = getInt(inputBase, input);
        input += SIZE_OF_INT;
        break;
      case 3:
        contentSize = (int) getLong(inputBase, input);
        input += SIZE_OF_LONG;
        break;
    }

    final boolean hasChecksum = (frameHeaderDescriptor & 0x4) != 0;

    return new FrameHeader(input - inputAddress, windowSize, contentSize,
                           dictionaryId, hasChecksum);
  }

  public static int getDecompressedSize(final byte[] inputBase,
                                        final int inputAddress,
                                        final int inputLimit) {
    int input = inputAddress;
    input += verifyMagic(inputBase, input, inputLimit);
    return readFrameHeader(inputBase, input, inputLimit).contentSize;
  }

  static int verifyMagic(final byte[] inputBase, final int inputAddress,
                         final int inputLimit) {
    verify(inputLimit - inputAddress >= 4, inputAddress,
           NOT_ENOUGH_INPUT_BYTES);

    final int magic = getInt(inputBase, inputAddress);
    if (magic != MAGIC_NUMBER) {
      if (magic == V07_MAGIC_NUMBER) {
        throw new MalformedInputException(inputAddress,
                                          "Data encoded in unsupported ZSTD v0.7 format");
      }
      throw new MalformedInputException(inputAddress, "Invalid magic prefix: " +
                                                      Integer
                                                          .toHexString(magic));
    }

    return SIZE_OF_INT;
  }
}
