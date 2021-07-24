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

class DoubleFastBlockCompressor implements BlockCompressor {
  private static final int MIN_MATCH = 3;
  private static final int SEARCH_STRENGTH = 8;
  private static final int REP_MOVE = Constants.REPEATED_OFFSET_COUNT - 1;

  public int compressBlock(final Object inputBase, final long inputAddress,
                           final int inputSize, final SequenceStore output,
                           final BlockCompressionState state,
                           final RepeatedOffsets offsets,
                           final CompressionParameters parameters) {
    final int matchSearchLength = Math.max(parameters.getSearchLength(), 4);

    // Offsets in hash tables are relative to baseAddress. Hash tables can be reused across calls to compressBlock as long as
    // baseAddress is kept constant.
    // We don't want to generate sequences that point before the current window limit, so we "filter" out all results from looking up in the hash tables
    // beyond that point.
    final long baseAddress = state.getBaseAddress();
    final long windowBaseAddress = baseAddress + state.getWindowBaseOffset();

    final int[] longHashTable = state.hashTable;
    final int longHashBits = parameters.getHashLog();

    final int[] shortHashTable = state.chainTable;
    final int shortHashBits = parameters.getChainLog();

    final long inputEnd = inputAddress + inputSize;
    final long inputLimit = inputEnd -
                            Constants.SIZE_OF_LONG; // We read a long at a time for computing the hashes

    long input = inputAddress;
    long anchor = inputAddress;

    int offset1 = offsets.getOffset0();
    int offset2 = offsets.getOffset1();

    int savedOffset = 0;

    if (input - windowBaseAddress == 0) {
      input++;
    }
    final int maxRep = (int) (input - windowBaseAddress);

    if (offset2 > maxRep) {
      savedOffset = offset2;
      offset2 = 0;
    }

    if (offset1 > maxRep) {
      savedOffset = offset1;
      offset1 = 0;
    }

    while (input <
           inputLimit) {   // < instead of <=, because repcode check at (input+1)
      final int shortHash =
          hash(inputBase, input, shortHashBits, matchSearchLength);
      long shortMatchAddress = baseAddress + shortHashTable[shortHash];

      final int longHash =
          hash8(UnsafeUtil.UNSAFE.getLong(inputBase, input), longHashBits);
      long longMatchAddress = baseAddress + longHashTable[longHash];

      // update hash tables
      final int current = (int) (input - baseAddress);
      longHashTable[longHash] = current;
      shortHashTable[shortHash] = current;

      int matchLength;
      final int offset;

      if (offset1 > 0 &&
          UnsafeUtil.UNSAFE.getInt(inputBase, input + 1 - offset1) ==
          UnsafeUtil.UNSAFE.getInt(inputBase, input + 1)) {
        // found a repeated sequence of at least 4 bytes, separated by offset1
        matchLength =
            count(inputBase, input + 1 + Constants.SIZE_OF_INT, inputEnd,
                  input + 1 + Constants.SIZE_OF_INT - offset1) +
            Constants.SIZE_OF_INT;
        input++;
        output.storeSequence(inputBase, anchor, (int) (input - anchor), 0,
                             matchLength - MIN_MATCH);
      } else {
        // check prefix long match
        if (longMatchAddress > windowBaseAddress &&
            UnsafeUtil.UNSAFE.getLong(inputBase, longMatchAddress) ==
            UnsafeUtil.UNSAFE.getLong(inputBase, input)) {
          matchLength =
              count(inputBase, input + Constants.SIZE_OF_LONG, inputEnd,
                    longMatchAddress + Constants.SIZE_OF_LONG) +
              Constants.SIZE_OF_LONG;
          offset = (int) (input - longMatchAddress);
          while (input > anchor && longMatchAddress > windowBaseAddress &&
                 UnsafeUtil.UNSAFE.getByte(inputBase, input - 1) ==
                 UnsafeUtil.UNSAFE.getByte(inputBase, longMatchAddress - 1)) {
            input--;
            longMatchAddress--;
            matchLength++;
          }
        } else {
          // check prefix short match
          if (shortMatchAddress > windowBaseAddress &&
              UnsafeUtil.UNSAFE.getInt(inputBase, shortMatchAddress) ==
              UnsafeUtil.UNSAFE.getInt(inputBase, input)) {
            final int nextOffsetHash =
                hash8(UnsafeUtil.UNSAFE.getLong(inputBase, input + 1),
                      longHashBits);
            long nextOffsetMatchAddress =
                baseAddress + longHashTable[nextOffsetHash];
            longHashTable[nextOffsetHash] = current + 1;

            // check prefix long +1 match
            if (nextOffsetMatchAddress > windowBaseAddress &&
                UnsafeUtil.UNSAFE.getLong(inputBase, nextOffsetMatchAddress) ==
                UnsafeUtil.UNSAFE.getLong(inputBase, input + 1)) {
              matchLength =
                  count(inputBase, input + 1 + Constants.SIZE_OF_LONG, inputEnd,
                        nextOffsetMatchAddress + Constants.SIZE_OF_LONG) +
                  Constants.SIZE_OF_LONG;
              input++;
              offset = (int) (input - nextOffsetMatchAddress);
              while (input > anchor &&
                     nextOffsetMatchAddress > windowBaseAddress &&
                     UnsafeUtil.UNSAFE.getByte(inputBase, input - 1) ==
                     UnsafeUtil.UNSAFE.getByte(inputBase,
                                               nextOffsetMatchAddress - 1)) {
                input--;
                nextOffsetMatchAddress--;
                matchLength++;
              }
            } else {
              // if no long +1 match, explore the short match we found
              matchLength =
                  count(inputBase, input + Constants.SIZE_OF_INT, inputEnd,
                        shortMatchAddress + Constants.SIZE_OF_INT) +
                  Constants.SIZE_OF_INT;
              offset = (int) (input - shortMatchAddress);
              while (input > anchor && shortMatchAddress > windowBaseAddress &&
                     UnsafeUtil.UNSAFE.getByte(inputBase, input - 1) ==
                     UnsafeUtil.UNSAFE.getByte(inputBase,
                                               shortMatchAddress - 1)) {
                input--;
                shortMatchAddress--;
                matchLength++;
              }
            }
          } else {
            input += ((input - anchor) >> SEARCH_STRENGTH) + 1;
            continue;
          }
        }

        offset2 = offset1;
        offset1 = offset;

        output.storeSequence(inputBase, anchor, (int) (input - anchor),
                             offset + REP_MOVE, matchLength - MIN_MATCH);
      }

      input += matchLength;
      anchor = input;

      if (input <= inputLimit) {
        // Fill Table
        longHashTable[hash8(
            UnsafeUtil.UNSAFE.getLong(inputBase, baseAddress + current + 2),
            longHashBits)] = current + 2;
        shortHashTable[hash(inputBase, baseAddress + current + 2, shortHashBits,
                            matchSearchLength)] = current + 2;

        longHashTable[hash8(UnsafeUtil.UNSAFE.getLong(inputBase, input - 2),
                            longHashBits)] = (int) (input - 2 - baseAddress);
        shortHashTable[hash(inputBase, input - 2, shortHashBits,
                            matchSearchLength)] =
            (int) (input - 2 - baseAddress);

        while (input <= inputLimit && offset2 > 0 &&
               UnsafeUtil.UNSAFE.getInt(inputBase, input) ==
               UnsafeUtil.UNSAFE.getInt(inputBase, input - offset2)) {
          final int repetitionLength =
              count(inputBase, input + Constants.SIZE_OF_INT, inputEnd,
                    input + Constants.SIZE_OF_INT - offset2) +
              Constants.SIZE_OF_INT;

          // swap offset2 <=> offset1
          final int temp = offset2;
          offset2 = offset1;
          offset1 = temp;

          shortHashTable[hash(inputBase, input, shortHashBits,
                              matchSearchLength)] = (int) (input - baseAddress);
          longHashTable[hash8(UnsafeUtil.UNSAFE.getLong(inputBase, input),
                              longHashBits)] = (int) (input - baseAddress);

          output.storeSequence(inputBase, anchor, 0, 0,
                               repetitionLength - MIN_MATCH);

          input += repetitionLength;
          anchor = input;
        }
      }
    }

    // save reps for next block
    offsets.saveOffset0(offset1 != 0? offset1 : savedOffset);
    offsets.saveOffset1(offset2 != 0? offset2 : savedOffset);

    // return the last literals size
    return (int) (inputEnd - anchor);
  }

  // TODO: same as LZ4RawCompressor.count

  /**
   * matchAddress must be < inputAddress
   */
  public static int count(final Object inputBase, final long inputAddress,
                          final long inputLimit, final long matchAddress) {
    long input = inputAddress;
    long match = matchAddress;

    final int remaining = (int) (inputLimit - inputAddress);

    // first, compare long at a time
    int count = 0;
    while (count < remaining - (Constants.SIZE_OF_LONG - 1)) {
      final long diff = UnsafeUtil.UNSAFE.getLong(inputBase, match) ^
                        UnsafeUtil.UNSAFE.getLong(inputBase, input);
      if (diff != 0) {
        return count + (Long.numberOfTrailingZeros(diff) >> 3);
      }

      count += Constants.SIZE_OF_LONG;
      input += Constants.SIZE_OF_LONG;
      match += Constants.SIZE_OF_LONG;
    }

    while (count < remaining && UnsafeUtil.UNSAFE.getByte(inputBase, match) ==
                                UnsafeUtil.UNSAFE.getByte(inputBase, input)) {
      count++;
      input++;
      match++;
    }

    return count;
  }

  private static int hash(final Object inputBase, final long inputAddress,
                          final int bits, final int matchSearchLength) {
    switch (matchSearchLength) {
      case 8:
        return hash8(UnsafeUtil.UNSAFE.getLong(inputBase, inputAddress), bits);
      case 7:
        return hash7(UnsafeUtil.UNSAFE.getLong(inputBase, inputAddress), bits);
      case 6:
        return hash6(UnsafeUtil.UNSAFE.getLong(inputBase, inputAddress), bits);
      case 5:
        return hash5(UnsafeUtil.UNSAFE.getLong(inputBase, inputAddress), bits);
      default:
        return hash4(UnsafeUtil.UNSAFE.getInt(inputBase, inputAddress), bits);
    }
  }

  private static final int PRIME_4_BYTES = 0x9E3779B1;
  private static final long PRIME_5_BYTES = 0xCF1BBCDCBBL;
  private static final long PRIME_6_BYTES = 0xCF1BBCDCBF9BL;
  private static final long PRIME_7_BYTES = 0xCF1BBCDCBFA563L;
  private static final long PRIME_8_BYTES = 0xCF1BBCDCB7A56463L;

  private static int hash4(final int value, final int bits) {
    return (value * PRIME_4_BYTES) >>> (Integer.SIZE - bits);
  }

  private static int hash5(final long value, final int bits) {
    return (int) (((value << (Long.SIZE - 40)) * PRIME_5_BYTES) >>>
                  (Long.SIZE - bits));
  }

  private static int hash6(final long value, final int bits) {
    return (int) (((value << (Long.SIZE - 48)) * PRIME_6_BYTES) >>>
                  (Long.SIZE - bits));
  }

  private static int hash7(final long value, final int bits) {
    return (int) (((value << (Long.SIZE - 56)) * PRIME_7_BYTES) >>>
                  (Long.SIZE - bits));
  }

  private static int hash8(final long value, final int bits) {
    return (int) ((value * PRIME_8_BYTES) >>> (Long.SIZE - bits));
  }
}
