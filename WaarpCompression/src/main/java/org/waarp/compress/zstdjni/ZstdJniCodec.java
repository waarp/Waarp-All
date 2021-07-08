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

package org.waarp.compress.zstdjni;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStream;
import com.google.common.io.ByteStreams;
import org.waarp.common.file.FileUtils;
import org.waarp.compress.CompressorCodec;
import org.waarp.compress.MalformedInputException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * ZSTD JNI Codec implementation
 */
public class ZstdJniCodec implements CompressorCodec {

  @Override
  public int maxCompressedLength(final int uncompressedSize) {
    return (int) Zstd.compressBound(uncompressedSize);
  }

  @Override
  public byte[] compress(final byte[] input, final int length)
      throws MalformedInputException {
    try {
      final int maxDstSize = maxCompressedLength(length);
      final byte[] target = new byte[maxDstSize];
      final int realSize = compress(input, length, target, maxDstSize);
      return Arrays.copyOfRange(target, 0, realSize);
    } catch (final Exception e) {
      throw new MalformedInputException(e);
    }
  }

  @Override
  public int compress(final byte[] input, final int inputLength,
                      final byte[] output, final int maxOutputLength) {
    try {
      return (int) Zstd
          .compressByteArray(output, 0, maxOutputLength, input, 0, inputLength,
                             3);
    } catch (final Exception e) {
      throw new MalformedInputException(e);
    }
  }

  @Override
  public byte[] decompress(final byte[] compressed, final int length)
      throws MalformedInputException {
    try {
      final int len = getDecompressedSize(compressed, length);
      final byte[] target = new byte[len];
      final int finalLen = decompress(compressed, length, target, len);
      if (finalLen != len) {
        throw new IllegalStateException(
            "Issue on suggested decompressed size " + len + " while is " +
            finalLen);
      }
      return target;
    } catch (final Exception e) {
      throw new MalformedInputException(e);
    }
  }

  @Override
  public long compress(final File input, final File output)
      throws MalformedInputException {
    InputStream inputStream = null;
    OutputStream outputStream = null;
    try {
      final byte[] buffer;
      inputStream = new FileInputStream(input);
      buffer = ByteStreams.toByteArray(inputStream);
      outputStream = new FileOutputStream(output);
      final byte[] bufferCompression =
          new byte[maxCompressedLength(buffer.length)];
      final int length = (int) Zstd
          .compressByteArray(bufferCompression, 0, bufferCompression.length,
                             buffer, 0, buffer.length, 3);
      outputStream.write(bufferCompression, 0, length);
      outputStream.flush();
      FileUtils.close(outputStream);
      outputStream = null;
      return output.length();
    } catch (final Exception e) {
      throw new MalformedInputException(e);
    } finally {
      FileUtils.close(inputStream);
      FileUtils.close(outputStream);
    }
  }

  @Override
  public int decompress(final byte[] input, final int inputLength,
                        final byte[] output, final int maxOutputLength) {
    try {
      return (int) Zstd
          .decompressByteArray(output, 0, maxOutputLength, input, 0,
                               inputLength);
    } catch (final Exception e) {
      throw new MalformedInputException(e);
    }
  }

  @Override
  public long decompress(final File input, final File output)
      throws MalformedInputException {
    InputStream inputStream = null;
    OutputStream outputStream = null;
    try {
      final byte[] buffer;
      inputStream = new ZstdInputStream(new FileInputStream(input));
      outputStream = new FileOutputStream(output);
      FileUtils.copy(Zstd.blockSizeMax(), inputStream, outputStream);
      outputStream = null;
      return output.length();
    } catch (final Exception e) {
      throw new MalformedInputException(e);
    } finally {
      FileUtils.close(inputStream);
      FileUtils.close(outputStream);
    }
  }

  @Override
  public int getDecompressedSize(final byte[] compressed, final int length) {
    return (int) Zstd.decompressedSize(compressed, 0, length);
  }

}
