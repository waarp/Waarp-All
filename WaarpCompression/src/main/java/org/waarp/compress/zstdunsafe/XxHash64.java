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

import static java.lang.Long.*;

// forked from https://github.com/airlift/slice
final class XxHash64 {
  private static final long PRIME64_1 = 0x9E3779B185EBCA87L;
  private static final long PRIME64_2 = 0xC2B2AE3D27D4EB4FL;
  private static final long PRIME64_3 = 0x165667B19E3779F9L;
  private static final long PRIME64_4 = 0x85EBCA77C2b2AE63L;
  private static final long PRIME64_5 = 0x27D4EB2F165667C5L;

  private XxHash64() {
  }

  public static long hash(final long seed, final Object base,
                          final long address, final int length) {
    long hash;
    if (length >= 32) {
      hash = updateBody(seed, base, address, length);
    } else {
      hash = seed + PRIME64_5;
    }

    hash += length;

    // round to the closest 32 byte boundary
    // this is the point up to which updateBody() processed
    final int index = length & 0xFFFFFFE0;

    return updateTail(hash, base, address, index, length);
  }

  private static long updateTail(long hash, final Object base,
                                 final long address, int index,
                                 final int length) {
    while (index <= length - 8) {
      hash = updateTail(hash, UnsafeUtil.UNSAFE.getLong(base, address + index));
      index += 8;
    }

    if (index <= length - 4) {
      hash = updateTail(hash, UnsafeUtil.UNSAFE.getInt(base, address + index));
      index += 4;
    }

    while (index < length) {
      hash = updateTail(hash, UnsafeUtil.UNSAFE.getByte(base, address + index));
      index++;
    }

    hash = finalShuffle(hash);

    return hash;
  }

  private static long updateBody(final long seed, final Object base,
                                 long address, final int length) {
    long v1 = seed + PRIME64_1 + PRIME64_2;
    long v2 = seed + PRIME64_2;
    long v3 = seed;
    long v4 = seed - PRIME64_1;

    int remaining = length;
    while (remaining >= 32) {
      v1 = mix(v1, UnsafeUtil.UNSAFE.getLong(base, address));
      v2 = mix(v2, UnsafeUtil.UNSAFE.getLong(base, address + 8));
      v3 = mix(v3, UnsafeUtil.UNSAFE.getLong(base, address + 16));
      v4 = mix(v4, UnsafeUtil.UNSAFE.getLong(base, address + 24));

      address += 32;
      remaining -= 32;
    }

    long hash = rotateLeft(v1, 1) + rotateLeft(v2, 7) + rotateLeft(v3, 12) +
                rotateLeft(v4, 18);

    hash = update(hash, v1);
    hash = update(hash, v2);
    hash = update(hash, v3);
    hash = update(hash, v4);

    return hash;
  }

  private static long mix(final long current, final long value) {
    return rotateLeft(current + value * PRIME64_2, 31) * PRIME64_1;
  }

  private static long update(final long hash, final long value) {
    final long temp = hash ^ mix(0, value);
    return temp * PRIME64_1 + PRIME64_4;
  }

  private static long updateTail(final long hash, final long value) {
    final long temp = hash ^ mix(0, value);
    return rotateLeft(temp, 27) * PRIME64_1 + PRIME64_4;
  }

  private static long updateTail(final long hash, final int value) {
    final long unsigned = value & 0xFFFFFFFFL;
    final long temp = hash ^ (unsigned * PRIME64_1);
    return rotateLeft(temp, 23) * PRIME64_2 + PRIME64_3;
  }

  private static long updateTail(final long hash, final byte value) {
    final int unsigned = value & 0xFF;
    final long temp = hash ^ (unsigned * PRIME64_5);
    return rotateLeft(temp, 11) * PRIME64_1;
  }

  private static long finalShuffle(long hash) {
    hash ^= hash >>> 33;
    hash *= PRIME64_2;
    hash ^= hash >>> 29;
    hash *= PRIME64_3;
    hash ^= hash >>> 32;
    return hash;
  }
}
