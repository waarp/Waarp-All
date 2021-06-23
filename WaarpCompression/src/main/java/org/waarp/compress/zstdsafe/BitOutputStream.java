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
import static org.waarp.compress.zstdsafe.Util.*;

class BitOutputStream {
  private static final int[] BIT_MASK = {
      0x0, 0x1, 0x3, 0x7, 0xF, 0x1F, 0x3F, 0x7F, 0xFF, 0x1FF, 0x3FF, 0x7FF,
      0xFFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF, 0x1FFFF, 0x3FFFF, 0x7FFFF, 0xFFFFF,
      0x1FFFFF, 0x3FFFFF, 0x7FFFFF, 0xFFFFFF, 0x1FFFFFF, 0x3FFFFFF, 0x7FFFFFF,
      0xFFFFFFF, 0x1FFFFFFF, 0x3FFFFFFF, 0x7FFFFFFF
  }; // up to 31 bits

  private final byte[] outputBase;
  private final int outputAddress;
  private final int outputLimit;

  private long container;
  private int bitCount;
  private int currentAddress;

  public BitOutputStream(final byte[] outputBase, final int outputAddress,
                         final int outputSize) {
    checkArgument(outputSize >= SIZE_OF_LONG, "Output buffer too small");

    this.outputBase = outputBase;
    this.outputAddress = outputAddress;
    outputLimit = this.outputAddress + outputSize - SIZE_OF_LONG;

    currentAddress = this.outputAddress;
  }

  public void addBits(final int value, final int bits) {
    container |= (long) (value & BIT_MASK[bits]) << bitCount;
    bitCount += bits;
  }

  /**
   * Note: leading bits of value must be 0
   */
  public void addBitsFast(final int value, final int bits) {
    container |= ((long) value) << bitCount;
    bitCount += bits;
  }

  public void flush() {
    final int bytes = bitCount >>> 3;

    putLong(outputBase, currentAddress, container);
    currentAddress += bytes;

    if (currentAddress > outputLimit) {
      currentAddress = outputLimit;
    }

    bitCount &= 7;
    container >>>= bytes * 8L;
  }

  public int close() {
    addBitsFast(1, 1); // end mark
    flush();

    checkState(currentAddress < outputLimit, "Overflow detected");

    return (currentAddress - outputAddress) + (bitCount > 0? 1 : 0);
  }
}
