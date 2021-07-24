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

package org.waarp.compress.zlib;

import org.waarp.common.file.FileUtils;
import org.waarp.compress.CompressorCodec;
import org.waarp.compress.MalformedInputException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterOutputStream;

/**
 * ZSTD JNI Codec implementation
 */
public class ZlibCodec implements CompressorCodec {

  @Override
  public final int maxCompressedLength(final int uncompressedSize) {
    return uncompressedSize;
  }

  @Override
  public final byte[] compress(final byte[] input, final int inputLength) {
    ByteArrayOutputStream bos = null;
    DeflaterOutputStream out = null;
    try {
      bos = new ByteArrayOutputStream(inputLength + 1024);
      out = new DeflaterOutputStream(bos);
      out.write(input, 0, inputLength);
      out.close();
      bos.close();
      return bos.toByteArray();
    } catch (final Exception e) {
      throw new MalformedInputException(e);
    } finally {
      FileUtils.close(out);
      FileUtils.close(bos);
    }
  }

  @Override
  public final int compress(final byte[] input, final int inputLength,
                            final byte[] output, final int maxOutputLength)
      throws MalformedInputException {
    try {
      final byte[] bytes = compress(input, inputLength);
      System.arraycopy(bytes, 0, output, 0, bytes.length);
      return bytes.length;
    } catch (final Exception e) {
      throw new MalformedInputException(e);
    }
  }

  @Override
  public final long compress(final File input, final File output)
      throws MalformedInputException {
    InputStream inputStream = null;
    OutputStream outputStream = null;
    DeflaterOutputStream out = null;
    try {
      inputStream = new FileInputStream(input);
      outputStream = new FileOutputStream(output);
      out = new DeflaterOutputStream(outputStream);
      FileUtils.copy(64 * 1024, inputStream, out);
      out = null;
      return output.length();
    } catch (final Exception e) {
      throw new MalformedInputException(e);
    } finally {
      FileUtils.close(out);
      FileUtils.close(inputStream);
      FileUtils.close(outputStream);
    }
  }

  @Override
  public final byte[] decompress(final byte[] compressed, final int length)
      throws MalformedInputException {
    ByteArrayOutputStream bos = null;
    InflaterOutputStream out = null;
    try {
      bos = new ByteArrayOutputStream(length << 2);
      out = new InflaterOutputStream(bos);
      out.write(compressed, 0, length);
      out.close();
      bos.close();
      return bos.toByteArray();
    } catch (final Exception e) {
      throw new MalformedInputException(e);
    } finally {
      FileUtils.close(out);
      FileUtils.close(bos);
    }
  }

  @Override
  public final int decompress(final byte[] input, final int inputLength,
                              final byte[] output, final int maxOutputLength)
      throws MalformedInputException {
    try {
      final byte[] bytes = decompress(input, inputLength);
      System.arraycopy(bytes, 0, output, 0, bytes.length);
      return bytes.length;
    } catch (final Exception e) {
      throw new MalformedInputException(e);
    }
  }

  @Override
  public final long decompress(final File input, final File output)
      throws MalformedInputException {
    InputStream inputStream = null;
    OutputStream outputStream = null;
    InflaterOutputStream out = null;
    try {
      inputStream = new FileInputStream(input);
      outputStream = new FileOutputStream(output);
      out = new InflaterOutputStream(outputStream);
      FileUtils.copy(64 * 1024, inputStream, out);
      out = null;
      return output.length();
    } catch (final Exception e) {
      throw new MalformedInputException(e);
    } finally {
      FileUtils.close(out);
      FileUtils.close(inputStream);
      FileUtils.close(outputStream);
    }
  }

  @Override
  public final int getDecompressedSize(final byte[] compressed,
                                       final int length) {
    return 0;
  }

}
