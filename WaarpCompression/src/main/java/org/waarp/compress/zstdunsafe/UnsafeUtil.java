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

import org.waarp.compress.IncompatibleJvmException;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteOrder;

import static java.lang.String.*;

public final class UnsafeUtil {
  private static boolean initialized;
  public static final Unsafe UNSAFE;
  private static final Field ADDRESS_ACCESSOR;

  private UnsafeUtil() {
  }

  static {
    Unsafe tempUnsafe = null;
    Field tempField = null;
    try {
      initLittleEndian();
      tempUnsafe = initUnsafe();
      tempField = initAccessor();
      initialized = true;
    } catch (final Exception e) {
      initialized = false;
    }
    UNSAFE = tempUnsafe;
    ADDRESS_ACCESSOR = tempField;
  }

  public static boolean isValid() {
    return initialized;
  }

  /**
   * @throws IncompatibleJvmException if this implementation is not available
   */
  private static final void initLittleEndian() {
    if (initialized) {
      return;
    }
    final ByteOrder order = ByteOrder.nativeOrder();
    if (!order.equals(ByteOrder.LITTLE_ENDIAN)) {
      throw new IncompatibleJvmException(
          format("Zstandard requires a little endian platform (found %s)",
                 order));
    }
  }

  /**
   * @throws IncompatibleJvmException if this implementation is not available
   */
  private static final Unsafe initUnsafe() {
    if (initialized) {
      return UNSAFE;
    }
    try {
      final Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
      theUnsafe.setAccessible(true);
      return (Unsafe) theUnsafe.get(null);
    } catch (final Exception e) {
      throw new IncompatibleJvmException(
          "Zstandard requires access to sun.misc.Unsafe");
    }
  }

  /**
   * @throws IncompatibleJvmException if this implementation is not available
   */
  private static final Field initAccessor() {
    if (initialized) {
      return ADDRESS_ACCESSOR;
    }
    try {
      // Remove warning
      final Class cls =
          Class.forName("jdk.internal.module.IllegalAccessLogger");
      final Field logger = cls.getDeclaredField("logger");
      UNSAFE.putObjectVolatile(cls, UNSAFE.staticFieldOffset(logger), null);

      final Field field = Buffer.class.getDeclaredField("address");
      field.setAccessible(true);
      return field;
    } catch (final Exception e) {
      throw new IncompatibleJvmException(
          "Zstandard requires access to java.nio.Buffer raw address field");
    }
  }

  public static final long getAddress(final Buffer buffer) {
    try {
      return (Long) ADDRESS_ACCESSOR.get(buffer);
    } catch (final IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
