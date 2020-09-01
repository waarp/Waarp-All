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

/**
 * Copyright Santeri Paavolainen <sjpaavol@cc.helsinki.fi> and Timothy W Macinta
 * (twm@alum.mit.edu)
 * (optimizations and bug fixes) and individual contributors by the @author
 * tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual
 * contributors.
 * <p>
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation; either
 * version 2.1 of the
 * License, or (at your option) any later version.
 * <p>
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License
 * along with this
 * software; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor,
 * Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.waarp.common.digest;

import io.netty.buffer.ByteBuf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Arrays;

import static org.waarp.common.digest.FilesystemBasedDigest.*;

/**
 * Fast implementation of RSA's MD5 hash generator in Java JDK Beta-2 or
 * higher.
 * <p>
 * Originally written by Santeri Paavolainen, Helsinki Finland 1996.<br>
 * (c) Santeri Paavolainen, Helsinki Finland 1996<br>
 * Many changes Copyright (c) 2002 - 2005 Timothy W Macinta<br>
 * <p>
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library
 * General Public License as published by the Free Software Foundation; either
 * version 2.1 of the License, or
 * (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See
 * the GNU Library General Public
 * License for more details.
 * <p>
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not,
 * write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA
 * 02139, USA.
 * <p>
 * See http://www.twmacinta.com/myjava/fast_md5.php for more information on this
 * file and the related files.
 * <p>
 * This was originally a rather straight re-implementation of the reference
 * implementation given in RFC1321 by
 * RSA. It passes the MD5 test suite as defined in RFC1321.
 * <p>
 * Many optimizations made by Timothy W Macinta. Reduced time to checksum a test
 * file in Java alone to roughly
 * half the time taken compared with java.security.MessageDigest (within an
 * intepretter). Also added an
 * optional native method to reduce the time even further. See
 * http://www.twmacinta.com/myjava/fast_md5.php
 * for further information on the time improvements achieved.
 * <p>
 * Some bug fixes also made by Timothy W Macinta.
 * <p>
 * Please note: I (Timothy Macinta) have put this code in the com.twmacinta.util
 * package only because it came
 * without a package. I was not the the original author of the code, although I
 * did optimize it
 * (substantially) and fix some bugs.
 * <p>
 * This Java class has been derived from the RSA Data Security, Inc. MD5
 * Message-Digest Algorithm and its
 * reference implementation.
 * <p>
 * This class will not use the native C version.
 *
 * @author Santeri Paavolainen <sjpaavol@cc.helsinki.fi>
 * @author Timothy W Macinta (twm@alum.mit.edu) (optimizations and bug fixes)
 * @author Frederic Bregier (add NIO support and dynamic library path
 *     loading)
 */

class MD5 {
  /**
   * MD5 state
   */
  private MD5State state;

  /**
   * If Final() has been called, finals is set to the current finals state.
   * Any
   * Update() causes this to be set
   * to null.
   */
  private MD5State finals;

  /**
   * Padding for Final()
   */
  private static final byte[] padding = {
      (byte) 0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
      0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
  };

  /**
   * Initialize MD5 internal state (object can be reused just by calling
   * Init()
   * after every Final()
   */
  synchronized void init() {
    state = new MD5State();
    finals = null;
  }

  /**
   * Class constructor
   */
  MD5() {
    init();
  }

  /**
   * Initialize class, and update hash with ob.toString()
   *
   * @param ob Object, ob.toString() is used to update hash after
   *     initialization
   */
  MD5(final Object ob) {
    this();
    Update(ob.toString());
  }

  private void Decode(final byte[] buffer, final int shift, final int[] out) {
    // unrolled loop (original loop shown above)
    out[0] = buffer[shift] & 0xff | (buffer[shift + 1] & 0xff) << 8 |
             (buffer[shift + 2] & 0xff) << 16 | buffer[shift + 3] << 24;
    out[1] = buffer[shift + 4] & 0xff | (buffer[shift + 5] & 0xff) << 8 |
             (buffer[shift + 6] & 0xff) << 16 | buffer[shift + 7] << 24;
    out[2] = buffer[shift + 8] & 0xff | (buffer[shift + 9] & 0xff) << 8 |
             (buffer[shift + 10] & 0xff) << 16 | buffer[shift + 11] << 24;
    out[3] = buffer[shift + 12] & 0xff | (buffer[shift + 13] & 0xff) << 8 |
             (buffer[shift + 14] & 0xff) << 16 | buffer[shift + 15] << 24;
    out[4] = buffer[shift + 16] & 0xff | (buffer[shift + 17] & 0xff) << 8 |
             (buffer[shift + 18] & 0xff) << 16 | buffer[shift + 19] << 24;
    out[5] = buffer[shift + 20] & 0xff | (buffer[shift + 21] & 0xff) << 8 |
             (buffer[shift + 22] & 0xff) << 16 | buffer[shift + 23] << 24;
    out[6] = buffer[shift + 24] & 0xff | (buffer[shift + 25] & 0xff) << 8 |
             (buffer[shift + 26] & 0xff) << 16 | buffer[shift + 27] << 24;
    out[7] = buffer[shift + 28] & 0xff | (buffer[shift + 29] & 0xff) << 8 |
             (buffer[shift + 30] & 0xff) << 16 | buffer[shift + 31] << 24;
    out[8] = buffer[shift + 32] & 0xff | (buffer[shift + 33] & 0xff) << 8 |
             (buffer[shift + 34] & 0xff) << 16 | buffer[shift + 35] << 24;
    out[9] = buffer[shift + 36] & 0xff | (buffer[shift + 37] & 0xff) << 8 |
             (buffer[shift + 38] & 0xff) << 16 | buffer[shift + 39] << 24;
    out[10] = buffer[shift + 40] & 0xff | (buffer[shift + 41] & 0xff) << 8 |
              (buffer[shift + 42] & 0xff) << 16 | buffer[shift + 43] << 24;
    out[11] = buffer[shift + 44] & 0xff | (buffer[shift + 45] & 0xff) << 8 |
              (buffer[shift + 46] & 0xff) << 16 | buffer[shift + 47] << 24;
    out[12] = buffer[shift + 48] & 0xff | (buffer[shift + 49] & 0xff) << 8 |
              (buffer[shift + 50] & 0xff) << 16 | buffer[shift + 51] << 24;
    out[13] = buffer[shift + 52] & 0xff | (buffer[shift + 53] & 0xff) << 8 |
              (buffer[shift + 54] & 0xff) << 16 | buffer[shift + 55] << 24;
    out[14] = buffer[shift + 56] & 0xff | (buffer[shift + 57] & 0xff) << 8 |
              (buffer[shift + 58] & 0xff) << 16 | buffer[shift + 59] << 24;
    out[15] = buffer[shift + 60] & 0xff | (buffer[shift + 61] & 0xff) << 8 |
              (buffer[shift + 62] & 0xff) << 16 | buffer[shift + 63] << 24;
  }

  private void Transform(final MD5State stat, final byte[] buffer,
                         final int shift, final int[] decodeBuf) {
    int a = stat.state[0];
    int b = stat.state[1];
    int c = stat.state[2];
    int d = stat.state[3];

    Decode(buffer, shift, decodeBuf);

    /* Round 1 */
    a += (b & c | ~b & d) + decodeBuf[0] + 0xd76aa478; /* 1 */
    a = (a << 7 | a >>> 25) + b;
    d += (a & b | ~a & c) + decodeBuf[1] + 0xe8c7b756; /* 2 */
    d = (d << 12 | d >>> 20) + a;
    c += (d & a | ~d & b) + decodeBuf[2] + 0x242070db; /* 3 */
    c = (c << 17 | c >>> 15) + d;
    b += (c & d | ~c & a) + decodeBuf[3] + 0xc1bdceee; /* 4 */
    b = (b << 22 | b >>> 10) + c;

    a += (b & c | ~b & d) + decodeBuf[4] + 0xf57c0faf; /* 5 */
    a = (a << 7 | a >>> 25) + b;
    d += (a & b | ~a & c) + decodeBuf[5] + 0x4787c62a; /* 6 */
    d = (d << 12 | d >>> 20) + a;
    c += (d & a | ~d & b) + decodeBuf[6] + 0xa8304613; /* 7 */
    c = (c << 17 | c >>> 15) + d;
    b += (c & d | ~c & a) + decodeBuf[7] + 0xfd469501; /* 8 */
    b = (b << 22 | b >>> 10) + c;

    a += (b & c | ~b & d) + decodeBuf[8] + 0x698098d8; /* 9 */
    a = (a << 7 | a >>> 25) + b;
    d += (a & b | ~a & c) + decodeBuf[9] + 0x8b44f7af; /* 10 */
    d = (d << 12 | d >>> 20) + a;
    c += (d & a | ~d & b) + decodeBuf[10] + 0xffff5bb1; /* 11 */
    c = (c << 17 | c >>> 15) + d;
    b += (c & d | ~c & a) + decodeBuf[11] + 0x895cd7be; /* 12 */
    b = (b << 22 | b >>> 10) + c;

    a += (b & c | ~b & d) + decodeBuf[12] + 0x6b901122; /* 13 */
    a = (a << 7 | a >>> 25) + b;
    d += (a & b | ~a & c) + decodeBuf[13] + 0xfd987193; /* 14 */
    d = (d << 12 | d >>> 20) + a;
    c += (d & a | ~d & b) + decodeBuf[14] + 0xa679438e; /* 15 */
    c = (c << 17 | c >>> 15) + d;
    b += (c & d | ~c & a) + decodeBuf[15] + 0x49b40821; /* 16 */
    b = (b << 22 | b >>> 10) + c;

    /* Round 2 */
    a += (b & d | c & ~d) + decodeBuf[1] + 0xf61e2562; /* 17 */
    a = (a << 5 | a >>> 27) + b;
    d += (a & c | b & ~c) + decodeBuf[6] + 0xc040b340; /* 18 */
    d = (d << 9 | d >>> 23) + a;
    c += (d & b | a & ~b) + decodeBuf[11] + 0x265e5a51; /* 19 */
    c = (c << 14 | c >>> 18) + d;
    b += (c & a | d & ~a) + decodeBuf[0] + 0xe9b6c7aa; /* 20 */
    b = (b << 20 | b >>> 12) + c;

    a += (b & d | c & ~d) + decodeBuf[5] + 0xd62f105d; /* 21 */
    a = (a << 5 | a >>> 27) + b;
    d += (a & c | b & ~c) + decodeBuf[10] + 0x02441453; /* 22 */
    d = (d << 9 | d >>> 23) + a;
    c += (d & b | a & ~b) + decodeBuf[15] + 0xd8a1e681; /* 23 */
    c = (c << 14 | c >>> 18) + d;
    b += (c & a | d & ~a) + decodeBuf[4] + 0xe7d3fbc8; /* 24 */
    b = (b << 20 | b >>> 12) + c;

    a += (b & d | c & ~d) + decodeBuf[9] + 0x21e1cde6; /* 25 */
    a = (a << 5 | a >>> 27) + b;
    d += (a & c | b & ~c) + decodeBuf[14] + 0xc33707d6; /* 26 */
    d = (d << 9 | d >>> 23) + a;
    c += (d & b | a & ~b) + decodeBuf[3] + 0xf4d50d87; /* 27 */
    c = (c << 14 | c >>> 18) + d;
    b += (c & a | d & ~a) + decodeBuf[8] + 0x455a14ed; /* 28 */
    b = (b << 20 | b >>> 12) + c;

    a += (b & d | c & ~d) + decodeBuf[13] + 0xa9e3e905; /* 29 */
    a = (a << 5 | a >>> 27) + b;
    d += (a & c | b & ~c) + decodeBuf[2] + 0xfcefa3f8; /* 30 */
    d = (d << 9 | d >>> 23) + a;
    c += (d & b | a & ~b) + decodeBuf[7] + 0x676f02d9; /* 31 */
    c = (c << 14 | c >>> 18) + d;
    b += (c & a | d & ~a) + decodeBuf[12] + 0x8d2a4c8a; /* 32 */
    b = (b << 20 | b >>> 12) + c;

    /* Round 3 */
    a += (b ^ c ^ d) + decodeBuf[5] + 0xfffa3942; /* 33 */
    a = (a << 4 | a >>> 28) + b;
    d += (a ^ b ^ c) + decodeBuf[8] + 0x8771f681; /* 34 */
    d = (d << 11 | d >>> 21) + a;
    c += (d ^ a ^ b) + decodeBuf[11] + 0x6d9d6122; /* 35 */
    c = (c << 16 | c >>> 16) + d;
    b += (c ^ d ^ a) + decodeBuf[14] + 0xfde5380c; /* 36 */
    b = (b << 23 | b >>> 9) + c;

    a += (b ^ c ^ d) + decodeBuf[1] + 0xa4beea44; /* 37 */
    a = (a << 4 | a >>> 28) + b;
    d += (a ^ b ^ c) + decodeBuf[4] + 0x4bdecfa9; /* 38 */
    d = (d << 11 | d >>> 21) + a;
    c += (d ^ a ^ b) + decodeBuf[7] + 0xf6bb4b60; /* 39 */
    c = (c << 16 | c >>> 16) + d;
    b += (c ^ d ^ a) + decodeBuf[10] + 0xbebfbc70; /* 40 */
    b = (b << 23 | b >>> 9) + c;

    a += (b ^ c ^ d) + decodeBuf[13] + 0x289b7ec6; /* 41 */
    a = (a << 4 | a >>> 28) + b;
    d += (a ^ b ^ c) + decodeBuf[0] + 0xeaa127fa; /* 42 */
    d = (d << 11 | d >>> 21) + a;
    c += (d ^ a ^ b) + decodeBuf[3] + 0xd4ef3085; /* 43 */
    c = (c << 16 | c >>> 16) + d;
    b += (c ^ d ^ a) + decodeBuf[6] + 0x04881d05; /* 44 */
    b = (b << 23 | b >>> 9) + c;

    a += (b ^ c ^ d) + decodeBuf[9] + 0xd9d4d039; /* 33 */
    a = (a << 4 | a >>> 28) + b;
    d += (a ^ b ^ c) + decodeBuf[12] + 0xe6db99e5; /* 34 */
    d = (d << 11 | d >>> 21) + a;
    c += (d ^ a ^ b) + decodeBuf[15] + 0x1fa27cf8; /* 35 */
    c = (c << 16 | c >>> 16) + d;
    b += (c ^ d ^ a) + decodeBuf[2] + 0xc4ac5665; /* 36 */
    b = (b << 23 | b >>> 9) + c;

    /* Round 4 */
    a += (c ^ (b | ~d)) + decodeBuf[0] + 0xf4292244; /* 49 */
    a = (a << 6 | a >>> 26) + b;
    d += (b ^ (a | ~c)) + decodeBuf[7] + 0x432aff97; /* 50 */
    d = (d << 10 | d >>> 22) + a;
    c += (a ^ (d | ~b)) + decodeBuf[14] + 0xab9423a7; /* 51 */
    c = (c << 15 | c >>> 17) + d;
    b += (d ^ (c | ~a)) + decodeBuf[5] + 0xfc93a039; /* 52 */
    b = (b << 21 | b >>> 11) + c;

    a += (c ^ (b | ~d)) + decodeBuf[12] + 0x655b59c3; /* 53 */
    a = (a << 6 | a >>> 26) + b;
    d += (b ^ (a | ~c)) + decodeBuf[3] + 0x8f0ccc92; /* 54 */
    d = (d << 10 | d >>> 22) + a;
    c += (a ^ (d | ~b)) + decodeBuf[10] + 0xffeff47d; /* 55 */
    c = (c << 15 | c >>> 17) + d;
    b += (d ^ (c | ~a)) + decodeBuf[1] + 0x85845dd1; /* 56 */
    b = (b << 21 | b >>> 11) + c;

    a += (c ^ (b | ~d)) + decodeBuf[8] + 0x6fa87e4f; /* 57 */
    a = (a << 6 | a >>> 26) + b;
    d += (b ^ (a | ~c)) + decodeBuf[15] + 0xfe2ce6e0; /* 58 */
    d = (d << 10 | d >>> 22) + a;
    c += (a ^ (d | ~b)) + decodeBuf[6] + 0xa3014314; /* 59 */
    c = (c << 15 | c >>> 17) + d;
    b += (d ^ (c | ~a)) + decodeBuf[13] + 0x4e0811a1; /* 60 */
    b = (b << 21 | b >>> 11) + c;

    a += (c ^ (b | ~d)) + decodeBuf[4] + 0xf7537e82; /* 61 */
    a = (a << 6 | a >>> 26) + b;
    d += (b ^ (a | ~c)) + decodeBuf[11] + 0xbd3af235; /* 62 */
    d = (d << 10 | d >>> 22) + a;
    c += (a ^ (d | ~b)) + decodeBuf[2] + 0x2ad7d2bb; /* 63 */
    c = (c << 15 | c >>> 17) + d;
    b += (d ^ (c | ~a)) + decodeBuf[9] + 0xeb86d391; /* 64 */
    b = (b << 21 | b >>> 11) + c;

    stat.state[0] += a;
    stat.state[1] += b;
    stat.state[2] += c;
    stat.state[3] += d;
  }

  /**
   * Updates hash with the bytebuffer given (using at maximum length bytes
   * from
   * that buffer)
   *
   * @param stat Which state is updated
   * @param buffer Array of bytes to be hashed
   * @param offset Offset to buffer array
   * @param length Use at maximum `length' bytes (absolute maximum is
   *     buffer.length)
   */
  void Update(final MD5State stat, final byte[] buffer, final int offset,
              final int length) {
    int index;
    int partlen;
    int i;
    final int start;
    finals = null;
    int newlength = length;
    /* Length can be told to be shorter, but not inter */
    if (newlength + offset > buffer.length) {
      newlength = buffer.length - offset;
    }

    /* compute number of bytes mod 64 */

    index = (int) (stat.count & 0x3f);
    stat.count += newlength;

    partlen = 64 - index;

    if (newlength >= partlen) {
      // update state (using only Java) to reflect input

      final int[] decodeBuf = new int[16];
      if (partlen == 64) {
        partlen = 0;
      } else {
        for (i = 0; i < partlen; i++) {
          stat.buffer[i + index] = buffer[i + offset];
        }
        Transform(stat, stat.buffer, 0, decodeBuf);
      }
      for (i = partlen; i + 63 < newlength; i += 64) {
        Transform(stat, buffer, i + offset, decodeBuf);
      }
      index = 0;
    } else {
      i = 0;
    }

    /* buffer remaining input */
    if (i < newlength) {
      start = i;
      for (; i < newlength; i++) {
        stat.buffer[index + i - start] = buffer[i + offset];
      }
    }
  }

  /*
   * Update()s for other datatypes than byte[] also. Update(byte[], int) is only the main driver.
   */

  /**
   * Plain update, updates this object
   *
   * @param buffer
   * @param offset
   * @param length
   */

  void Update(final byte[] buffer, final int offset, final int length) {
    Update(state, buffer, offset, length);
  }

  /**
   * Plain update, updates this object
   *
   * @param buffer
   * @param length
   */
  void Update(final byte[] buffer, final int length) {
    Update(state, buffer, 0, length);
  }

  /**
   * Updates hash with given array of bytes
   *
   * @param buffer Array of bytes to use for updating the hash
   */
  void Update(final byte[] buffer) {
    Update(buffer, 0, buffer.length);
  }

  /**
   * Updates hash with a single byte
   *
   * @param b Single byte to update the hash
   */
  void Update(final byte b) {
    final byte[] buffer = { b };

    Update(buffer, 1);
  }

  /**
   * Update buffer with given string. Note that because the version of the
   * s.getBytes() method without
   * parameters is used to convert the string to a byte array, the results of
   * this method may be different on
   * different platforms. The s.getBytes() method converts the string into a
   * byte array using the current
   * platform's default character set and may therefore have different results
   * on platforms with different
   * default character sets. If a version that works consistently across
   * platforms with different default
   * character sets is desired, use the overloaded version of the Update()
   * method which takes a string and a
   * character encoding.
   *
   * @param s String to be update to hash (is used as s.getBytes())
   */
  void Update(final String s) {
    final byte[] chars = s.getBytes(FilesystemBasedDigest.UTF8);
    Update(chars, chars.length);
  }

  /**
   * Update buffer with given string using the given encoding. If the given
   * encoding is null, the encoding
   * "UTF8" is used.
   *
   * @param s String to be update to hash (is used as
   *     s.getBytes(charset_name))
   * @param charsetName The character set to use to convert s to a
   *     byte
   *     array, or null if the "ISO8859_1"
   *     character set is desired.
   *
   * @throws UnsupportedEncodingException If the named charset
   *     is
   *     not supported.
   */
  void Update(final String s, final String charsetName)
      throws UnsupportedEncodingException {
    Charset newcharset = FilesystemBasedDigest.UTF8;
    if (charsetName != null) {
      newcharset = Charset.forName(charsetName);
    }
    final byte[] chars = s.getBytes(newcharset);
    Update(chars, chars.length);
  }

  /**
   * Update buffer with a single integer (only & 0xff part is used, as a byte)
   *
   * @param i Integer value, which is then converted to byte as i &
   *     0xff
   */

  void Update(final int i) {
    Update((byte) (i & 0xff));
  }

  private byte[] reusableBytes;

  /**
   * Updates hash with given {@link ByteBuf} (from Netty)
   *
   * @param buffer ByteBuf to use for updating the hash and this
   *     buffer will
   *     not be changed
   */
  void Update(final ByteBuf buffer) {
    final byte[] bytes;
    int start = 0;
    final int len = buffer.readableBytes();
    if (buffer.hasArray()) {
      start = buffer.arrayOffset();
      bytes = buffer.array();
    } else {
      if (reusableBytes == null || reusableBytes.length != len) {
        reusableBytes = new byte[len];
      }
      bytes = reusableBytes;
      buffer.getBytes(start, bytes);
    }
    Update(state, bytes, start, len);
  }

  private byte[] Encode(final int[] input, final int len) {
    int i;
    int j;
    final byte[] out;

    out = new byte[len];

    for (i = j = 0; j < len; i++, j += 4) {
      out[j] = (byte) (input[i] & 0xff);
      out[j + 1] = (byte) (input[i] >>> 8 & 0xff);
      out[j + 2] = (byte) (input[i] >>> 16 & 0xff);
      out[j + 3] = (byte) (input[i] >>> 24 & 0xff);
    }

    return out;
  }

  /**
   * Returns array of bytes (16 bytes) representing hash as of the current
   * state
   * of this object. Note: getting a
   * hash does not invalidate the hash object, it only creates a copy of the
   * real state which is finalized.
   *
   * @return Array of 16 bytes, the hash of all updated bytes
   */
  synchronized byte[] Final() {
    final byte[] bits;
    final int index;
    final int padlen;
    final MD5State fin;

    if (finals == null) {
      fin = new MD5State(state);

      final int[] countInts =
          { (int) (fin.count << 3), (int) (fin.count >> 29) };
      bits = Encode(countInts, 8);

      index = (int) (fin.count & 0x3f);
      padlen = index < 56? 56 - index : 120 - index;

      Update(fin, padding, 0, padlen);
      Update(fin, bits, 0, 8);

      /* Update() sets finals to null */
      finals = fin;
    }

    return Encode(finals.state, 16);
  }

  private static final char[] HEX_CHARS = {
      '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
      'f',
  };

  /**
   * Turns array of bytes into string representing each byte as unsigned hex
   * number.
   *
   * @param hash Array of bytes to convert to hex-string
   *
   * @return Generated hex string
   */
  static final String asHex(final byte[] hash) {
    final char[] buf = new char[hash.length * 2];
    for (int i = 0, x = 0; i < hash.length; i++) {
      buf[x++] = HEX_CHARS[hash[i] >>> 4 & 0xf];
      buf[x++] = HEX_CHARS[hash[i] & 0xf];
    }
    return new String(buf);
  }

  /**
   * Turns String into array of bytes representing each couple of unsigned hex
   * number as one byte.
   *
   * @param buf hex string
   *
   * @return Array of bytes converted from hex-string
   */
  static final byte[] asByte(final String buf) {
    final byte[] from = buf.getBytes(FilesystemBasedDigest.UTF8);
    final byte[] hash = new byte[from.length / 2];
    for (int i = 0, x = 0; i < hash.length; i++) {
      byte code1 = from[x++];
      byte code2 = from[x++];
      if (code1 >= HEX_CHARS[10]) {
        code1 -= HEX_CHARS[10] - 10;
      } else {
        code1 -= HEX_CHARS[0];
      }
      if (code2 >= HEX_CHARS[10]) {
        code2 -= HEX_CHARS[10] - 10;
      } else {
        code2 -= HEX_CHARS[0];
      }
      hash[i] = (byte) ((code1 << 4) + (code2 & 0xFF));
    }
    return hash;
  }

  /**
   * Returns 32-character hex representation of this objects hash
   *
   * @return String of this object's hash
   */
  String asHex() {
    return asHex(Final());
  }

  /**
   * Calculates and returns the hash of the contents of the given file.
   *
   * @param f FileInterface to hash
   *
   * @return the hash from the given file
   *
   * @throws IOException
   **/
  static byte[] getHash(final File f) throws IOException {
    FileInputStream in = null;
    try {
      long bufSize = f.length();
      if (bufSize == 0) {
        return EMPTY;
      }
      if (bufSize > ZERO_COPY_CHUNK_SIZE) {
        bufSize = ZERO_COPY_CHUNK_SIZE;
      }
      byte[] buf = new byte[(int) bufSize];
      in = new FileInputStream(f);
      final MD5 md5 = new MD5();
      int read;
      while ((read = in.read(buf)) >= 0) {
        md5.Update(md5.state, buf, 0, read);
      }
      buf = md5.Final();
      return buf;
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (final Exception ignored) {
          // ignored
        }
      }
    }
  }

  /**
   * Calculates and returns the hash of the contents of the given file using
   * Nio
   * file access.
   *
   * @param f for the FileInterface
   *
   * @return the hash from the FileInterface with NIO access
   *
   * @throws IOException
   **/
  static byte[] getHashNio(final File f) throws IOException {
    if (!f.exists()) {
      throw new FileNotFoundException(f.toString());
    }
    FileInputStream in = null;
    FileChannel fileChannel = null;
    try {
      long bufSize = f.length();
      if (bufSize == 0) {
        return EMPTY;
      }
      if (bufSize > ZERO_COPY_CHUNK_SIZE) {
        bufSize = ZERO_COPY_CHUNK_SIZE;
      }
      final byte[] buf = new byte[(int) bufSize];

      in = new FileInputStream(f);
      fileChannel = in.getChannel();
      final ByteBuffer bb = ByteBuffer.wrap(buf);
      int read;
      final MD5 md5 = new MD5();
      read = fileChannel.read(bb);
      while (read > 0) {
        md5.Update(md5.state, buf, 0, read);
        bb.clear();
        read = fileChannel.read(bb);
      }
      return md5.Final();
    } finally {
      if (fileChannel != null) {
        try {
          fileChannel.close();
        } catch (final Exception ignored) {
          // ignored
        }
      }
      if (in != null) {
        try {
          in.close();
        } catch (final Exception ignored) {
          // ignored
        }
      }
    }
  }

  /**
   * Calculates and returns the hash of the contents of the given stream.
   *
   * @param stream Stream to hash
   *
   * @return the hash from the given stream
   *
   * @throws IOException
   **/
  static byte[] getHash(final InputStream stream) throws IOException {
    try {
      final byte[] buf = new byte[(int) ZERO_COPY_CHUNK_SIZE];
      final MD5 md5 = new MD5();
      int read;
      while ((read = stream.read(buf)) >= 0) {
        md5.Update(md5.state, buf, 0, read);
      }
      return md5.Final();
    } finally {
      try {
        stream.close();
      } catch (final Exception ignored) {
        // ignored
      }
    }
  }

  /**
   * Test if both hashes are equal
   *
   * @param hash1
   * @param hash2
   *
   * @return true iff the first 16 bytes of both hash1 and hash2 are equal;
   *     both
   *     hash1 and hash2 are null; or
   *     either hash array is less than 16 bytes in length and their
   *     lengths and
   *     all of their bytes are
   *     equal.
   **/
  static boolean hashesEqual(final byte[] hash1, final byte[] hash2) {
    return Arrays.equals(hash1, hash2);
  }

  private static final byte[] salt =
      { 'G', 'o', 'l', 'd', 'e', 'n', 'G', 'a', 't', 'e' };

  /**
   * Crypt a password
   *
   * @param pwd to crypt
   *
   * @return the crypted password
   */
  static final String passwdCrypt(final String pwd) {
    final MD5 md5 = new MD5();
    final byte[] bpwd = pwd.getBytes(FilesystemBasedDigest.UTF8);
    for (int i = 0; i < 16; i++) {
      md5.Update(md5.state, bpwd, 0, bpwd.length);
      md5.Update(md5.state, salt, 0, salt.length);
    }
    return md5.asHex();
  }

  /**
   * Crypt a password
   *
   * @param bpwd to crypt
   *
   * @return the crypted password
   */
  static final byte[] passwdCrypt(final byte[] bpwd) {
    final MD5 md5 = new MD5();
    for (int i = 0; i < 16; i++) {
      md5.Update(md5.state, bpwd, 0, bpwd.length);
      md5.Update(md5.state, salt, 0, salt.length);
    }
    return md5.Final();
  }

  /**
   * @param pwd
   * @param cryptPwd
   *
   * @return True if the pwd is comparable with the cryptPwd
   */
  static final boolean equalPasswd(final String pwd, final String cryptPwd) {
    final String asHex = passwdCrypt(pwd);
    return cryptPwd.equals(asHex);
  }

  /**
   * @param pwd
   * @param cryptPwd
   *
   * @return True if the pwd is comparable with the cryptPwd
   */
  static final boolean equalPasswd(final byte[] pwd, final byte[] cryptPwd) {
    final byte[] bytes = passwdCrypt(pwd);
    return Arrays.equals(cryptPwd, bytes);
  }
}
