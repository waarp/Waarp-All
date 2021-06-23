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

import org.waarp.compress.IncompatibleJvmException;

import java.nio.ByteOrder;

import static java.lang.String.*;

final class UnsafeUtil {

  private UnsafeUtil() {
  }

  static {
    final ByteOrder order = ByteOrder.nativeOrder();
    if (!order.equals(ByteOrder.LITTLE_ENDIAN)) {
      throw new IncompatibleJvmException(
          format("Zstandard requires a little endian platform (found %s)",
                 order));
    }
  }

  static final void putByte(final byte[] outputBase, final int output,
                            final byte value) {
    outputBase[output] = (byte) (value & 0xff);
  }

  static final void putShort(final byte[] outputBase, final int output,
                             final short value) {
    outputBase[output] = (byte) (value & 0xFF);
    outputBase[output + 1] = (byte) ((value >> 8) & 0xFF);
  }

  static final void putInt(final byte[] outputBase, final int output,
                           final int value) {
    outputBase[output] = (byte) (value & 0xFF);
    outputBase[output + 1] = (byte) ((value >> 8) & 0xFF);
    outputBase[output + 2] = (byte) ((value >> 16) & 0xFF);
    outputBase[output + 3] = (byte) ((value >> 24) & 0xFF);
  }

  static final void putLong(final byte[] outputBase, final int output,
                            final long value) {
    outputBase[output] = (byte) (value & 0xFF);
    outputBase[output + 1] = (byte) ((value >> 8) & 0xFF);
    outputBase[output + 2] = (byte) ((value >> 16) & 0xFF);
    outputBase[output + 3] = (byte) ((value >> 24) & 0xFF);
    outputBase[output + 4] = (byte) ((value >> 32) & 0xFF);
    outputBase[output + 5] = (byte) ((value >> 40) & 0xFF);
    outputBase[output + 6] = (byte) ((value >> 48) & 0xFF);
    outputBase[output + 7] = (byte) ((value >> 56) & 0xFF);
  }

  static final void copyMemory(final byte[] inputBase, final int input,
                               final byte[] outputBase, final int output,
                               final int size) {
    System.arraycopy(inputBase, input, outputBase, output, size);
  }

  static final byte getByte(final byte[] inputBase, final int inputAddress) {
    return (byte) (inputBase[inputAddress] & 0xFF);
  }

  static final short getShort(final byte[] inputBase, final int inputAddress) {
    return (short) (((short) inputBase[inputAddress + 1] & 0xff) << 8 |
                    ((short) inputBase[inputAddress] & 0xff));
  }

  static final int getInt(final byte[] inputBase, final int inputAddress) {
    return ((int) inputBase[inputAddress + 3] & 0xff) << 24 |
           ((int) inputBase[inputAddress + 2] & 0xff) << 16 |
           ((int) inputBase[inputAddress + 1] & 0xff) << 8 |
           ((int) inputBase[inputAddress] & 0xff);
  }

  static final long getLong(final byte[] inputBase, final int inputAddress) {
    return ((long) inputBase[inputAddress + 7] << 56) |
           ((long) inputBase[inputAddress + 6] & 0xff) << 48 |
           ((long) inputBase[inputAddress + 5] & 0xff) << 40 |
           ((long) inputBase[inputAddress + 4] & 0xff) << 32 |
           ((long) inputBase[inputAddress + 3] & 0xff) << 24 |
           ((long) inputBase[inputAddress + 2] & 0xff) << 16 |
           ((long) inputBase[inputAddress + 1] & 0xff) << 8 |
           ((long) inputBase[inputAddress] & 0xff);
  }

}
