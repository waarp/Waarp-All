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
import static org.waarp.compress.zstdunsafe.UnsafeUtil.*;
import static sun.misc.Unsafe.*;

class SequenceStore {
  public final byte[] literalsBuffer;
  public int literalsLength;

  public final int[] offsets;
  public final int[] literalLengths;
  public final int[] matchLengths;
  public int sequenceCount;

  public final byte[] literalLengthCodes;
  public final byte[] matchLengthCodes;
  public final byte[] offsetCodes;

  public LongField longLengthField;
  public int longLengthPosition;

  public enum LongField {
    LITERAL, MATCH
  }

  private static final byte[] LITERAL_LENGTH_CODE = {
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 16, 17, 17, 18,
      18, 19, 19, 20, 20, 20, 20, 21, 21, 21, 21, 22, 22, 22, 22, 22, 22, 22,
      22, 23, 23, 23, 23, 23, 23, 23, 23, 24, 24, 24, 24, 24, 24, 24, 24, 24,
      24, 24, 24, 24, 24, 24, 24
  };

  private static final byte[] MATCH_LENGTH_CODE = {
      0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
      21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 32, 33, 33, 34, 34, 35,
      35, 36, 36, 36, 36, 37, 37, 37, 37, 38, 38, 38, 38, 38, 38, 38, 38, 39,
      39, 39, 39, 39, 39, 39, 39, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40, 40,
      40, 40, 40, 40, 40, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41, 41,
      41, 41, 41, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42,
      42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42, 42
  };

  public SequenceStore(final int blockSize, final int maxSequences) {
    offsets = new int[maxSequences];
    literalLengths = new int[maxSequences];
    matchLengths = new int[maxSequences];

    literalLengthCodes = new byte[maxSequences];
    matchLengthCodes = new byte[maxSequences];
    offsetCodes = new byte[maxSequences];

    literalsBuffer = new byte[blockSize];

    reset();
  }

  public void appendLiterals(final Object inputBase, final long inputAddress,
                             final int inputSize) {
    UNSAFE.copyMemory(inputBase, inputAddress, literalsBuffer,
                      ARRAY_BYTE_BASE_OFFSET + literalsLength, inputSize);
    literalsLength += inputSize;
  }

  public void storeSequence(final Object literalBase, final long literalAddress,
                            final int literalLength, final int offsetCode,
                            final int matchLengthBase) {
    long input = literalAddress;
    long output = ARRAY_BYTE_BASE_OFFSET + literalsLength;
    int copied = 0;
    do {
      UNSAFE
          .putLong(literalsBuffer, output, UNSAFE.getLong(literalBase, input));
      input += SIZE_OF_LONG;
      output += SIZE_OF_LONG;
      copied += SIZE_OF_LONG;
    } while (copied < literalLength);

    literalsLength += literalLength;

    if (literalLength > 65535) {
      longLengthField = LongField.LITERAL;
      longLengthPosition = sequenceCount;
    }
    literalLengths[sequenceCount] = literalLength;

    offsets[sequenceCount] = offsetCode + 1;

    if (matchLengthBase > 65535) {
      longLengthField = LongField.MATCH;
      longLengthPosition = sequenceCount;
    }

    matchLengths[sequenceCount] = matchLengthBase;

    sequenceCount++;
  }

  public void reset() {
    literalsLength = 0;
    sequenceCount = 0;
    longLengthField = null;
  }

  public void generateCodes() {
    for (int i = 0; i < sequenceCount; ++i) {
      literalLengthCodes[i] = (byte) literalLengthToCode(literalLengths[i]);
      offsetCodes[i] = (byte) Util.highestBit(offsets[i]);
      matchLengthCodes[i] = (byte) matchLengthToCode(matchLengths[i]);
    }

    if (longLengthField == LongField.LITERAL) {
      literalLengthCodes[longLengthPosition] =
          Constants.MAX_LITERALS_LENGTH_SYMBOL;
    }
    if (longLengthField == LongField.MATCH) {
      matchLengthCodes[longLengthPosition] = Constants.MAX_MATCH_LENGTH_SYMBOL;
    }
  }

  private static int literalLengthToCode(final int literalLength) {
    if (literalLength >= 64) {
      return Util.highestBit(literalLength) + 19;
    } else {
      return LITERAL_LENGTH_CODE[literalLength];
    }
  }

  /*
   * matchLengthBase = matchLength - MINMATCH
   * (that's how it's stored in SequenceStore)
   */
  private static int matchLengthToCode(final int matchLengthBase) {
    if (matchLengthBase >= 128) {
      return Util.highestBit(matchLengthBase) + 36;
    } else {
      return MATCH_LENGTH_CODE[matchLengthBase];
    }
  }
}
