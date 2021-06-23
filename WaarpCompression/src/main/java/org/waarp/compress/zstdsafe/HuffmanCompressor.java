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

class HuffmanCompressor {
  private HuffmanCompressor() {
  }

  public static int compress4streams(final byte[] outputBase,
                                     final int outputAddress,
                                     final int outputSize,
                                     final byte[] inputBase,
                                     final int inputAddress,
                                     final int inputSize,
                                     final HuffmanCompressionTable table) {
    int input = inputAddress;
    final int inputLimit = inputAddress + inputSize;
    int output = outputAddress;
    final int outputLimit = outputAddress + outputSize;

    final int segmentSize = (inputSize + 3) / 4;

    if (outputSize <
        6 /* jump table */ + 1 /* first stream */ + 1 /* second stream */ +
        1 /* third stream */ +
        8 /* 8 bytes minimum needed by the bitstream encoder */) {
      return 0; // minimum space to compress successfully
    }

    if (inputSize <= 6 + 1 + 1 + 1) { // jump table + one byte per stream
      return 0;  // no saving possible: input too small
    }

    output += SIZE_OF_SHORT + SIZE_OF_SHORT + SIZE_OF_SHORT; // jump table

    int compressedSize;

    // first segment
    compressedSize =
        compressSingleStream(outputBase, output, outputLimit - output,
                             inputBase, input, segmentSize, table);
    if (compressedSize == 0) {
      return 0;
    }
    putShort(outputBase, outputAddress, (short) compressedSize);
    output += compressedSize;
    input += segmentSize;

    // second segment
    compressedSize =
        compressSingleStream(outputBase, output, outputLimit - output,
                             inputBase, input, segmentSize, table);
    if (compressedSize == 0) {
      return 0;
    }
    putShort(outputBase, outputAddress + SIZE_OF_SHORT, (short) compressedSize);
    output += compressedSize;
    input += segmentSize;

    // third segment
    compressedSize =
        compressSingleStream(outputBase, output, outputLimit - output,
                             inputBase, input, segmentSize, table);
    if (compressedSize == 0) {
      return 0;
    }
    putShort(outputBase, outputAddress + SIZE_OF_SHORT + SIZE_OF_SHORT,
             (short) compressedSize);
    output += compressedSize;
    input += segmentSize;

    // fourth segment
    compressedSize =
        compressSingleStream(outputBase, output, outputLimit - output,
                             inputBase, input, inputLimit - input, table);
    if (compressedSize == 0) {
      return 0;
    }
    output += compressedSize;

    return output - outputAddress;
  }

  public static int compressSingleStream(final byte[] outputBase,
                                         final int outputAddress,
                                         final int outputSize,
                                         final byte[] inputBase,
                                         final int inputAddress,
                                         final int inputSize,
                                         final HuffmanCompressionTable table) {
    final BitOutputStream bitstream =
        new BitOutputStream(outputBase, outputAddress, outputSize);

    int n = inputSize & ~3; // join to mod 4

    switch (inputSize & 3) {
      case 3:
        table.encodeSymbol(bitstream, inputBase[inputAddress + n + 2] & 0xFF);
        // fall-through
      case 2:
        table.encodeSymbol(bitstream, inputBase[inputAddress + n + 1] & 0xFF);
        // fall-through
      case 1:
        table.encodeSymbol(bitstream, inputBase[inputAddress + n] & 0xFF);
        bitstream.flush();
        // fall-through
      case 0: /* fall-through */
      default:
        break;
    }

    for (; n > 0; n -= 4) {  // note: n & 3 == 0 at this stage
      table.encodeSymbol(bitstream, inputBase[inputAddress + n - 1] & 0xFF);
      table.encodeSymbol(bitstream, inputBase[inputAddress + n - 2] & 0xFF);
      table.encodeSymbol(bitstream, inputBase[inputAddress + n - 3] & 0xFF);
      table.encodeSymbol(bitstream, inputBase[inputAddress + n - 4] & 0xFF);
      bitstream.flush();
    }

    return bitstream.close();
  }
}
