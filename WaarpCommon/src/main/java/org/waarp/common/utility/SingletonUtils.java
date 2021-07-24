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
package org.waarp.common.utility;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Singleton utility class
 */
public final class SingletonUtils {
  private static final byte[] SINGLETON_BYTE_ARRAY = {};
  private static final InputStream SINGLETON_INPUTSTREAM =
      new NullInputStream();
  private static final OutputStream SINGLETON_OUTPUTSTREAM =
      new VoidOutputStream();

  private SingletonUtils() {
    // empty
  }


  /**
   * Immutable empty byte array
   *
   * @return a Byte Array Singleton
   */
  public static byte[] getSingletonByteArray() {
    return SINGLETON_BYTE_ARRAY;
  }

  /**
   * Immutable empty List
   *
   * @return an immutable empty List
   */
  public static <E> List<E> singletonList() {
    return Collections.emptyList();
  }

  /**
   * Immutable empty Set
   *
   * @return an immutable empty Set
   */
  public static <E> Set<E> singletonSet() {
    return Collections.emptySet();
  }

  /**
   * Immutable empty Map
   *
   * @return an immutable empty Map
   */
  public static <E, V> Map<E, V> singletonMap() {
    return Collections.emptyMap();
  }

  /**
   * Empty InputStream
   */
  private static final class NullInputStream extends InputStream {
    @Override
    public final int read() {
      return -1;
    }

    @Override
    public final int available() {
      return 0;
    }

    @Override
    public final void close() {
      // Empty
    }

    @Override
    public final void mark(final int arg0) {//NOSONAR
      // Empty
    }

    @Override
    public final boolean markSupported() {
      return true;
    }

    @Override
    public final int read(final byte[] arg0, final int arg1, final int arg2) {
      return -1;
    }

    @Override
    public final int read(final byte[] arg0) {
      return -1;
    }

    @Override
    public final void reset() {//NOSONAR
      // Empty
    }

    @Override
    public final long skip(final long arg0) {
      return 0;
    }
  }

  /**
   * Immutable empty InputStream
   *
   * @return an immutable empty InputStream
   */
  public static InputStream singletonInputStream() {
    return SINGLETON_INPUTSTREAM;
  }

  /**
   * OutputStream discarding all writed elements
   */
  private static final class VoidOutputStream extends OutputStream {
    @Override
    public final void close() {
      // Empty
    }

    @Override
    public final void flush() {
      // Empty
    }

    @Override
    public final void write(final byte[] arg0, final int arg1, final int arg2) {
      // Empty
    }

    @Override
    public final void write(final byte[] arg0) {
      // Empty
    }

    @Override
    public final void write(final int arg0) {
      // Empty
    }
  }

  /**
   * Immutable void OutputStream. Any write elements are discarder (equivalent
   * to /dev/null).
   *
   * @return an immutable empty OutputStream
   */
  public static OutputStream singletonOutputStream() {
    return SINGLETON_OUTPUTSTREAM;
  }

}
