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

import static org.waarp.compress.zstdsafe.Constants.*;

/**
 * ZSTD Safe Codec implementation
 */
public class ZstdSafeCodec implements CompressorCodec {
  private final ZstdFrameDecompressor decompressor =
      new ZstdFrameDecompressor();

  /**
   * Used to get the maximum size of a compressed buffer
   *
   * @param uncompressedSize
   *
   * @return the maximum size
   */
  public static int maxCompressedSize(final int uncompressedSize) {
    int result = uncompressedSize + (uncompressedSize >>> 8);
    if (uncompressedSize < MAX_BLOCK_SIZE) {
      result += (MAX_BLOCK_SIZE - uncompressedSize) >>> 11;
    }
    return result;
  }

  @Override
  public int maxCompressedLength(final int uncompressedSize) {
    return maxCompressedSize(uncompressedSize);
  }

  @Override
  public byte[] compress(final byte[] input, final int length) {
    try {
      final int len = maxCompressedLength(length);
      final byte[] temp = new byte[len];
      final int finalLen = compress(input, input.length, temp, len);
      return Arrays.copyOf(temp, finalLen);
    } catch (final Exception e) {
      throw new MalformedInputException(e);
    }
  }

  @Override
  public int compress(final byte[] input, final int inputLength,
                      final byte[] output, final int maxOutputLength) {
    try {
      return ZstdFrameCompressor
          .compress(input, 0, inputLength, output, 0, maxOutputLength,
                    CompressionParameters.DEFAULT_COMPRESSION_LEVEL);
    } catch (final Exception e) {
      throw new MalformedInputException(e);
    }
  }

  @Override
  public byte[] decompress(final byte[] input, final int length)
      throws MalformedInputException {
    try {
      final int finalLen =
          ZstdFrameDecompressor.getDecompressedSize(input, 0, length);
      final byte[] decompressed = new byte[finalLen];
      decompress(input, input.length, decompressed, finalLen);
      return decompressed;
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
      // Need to store in front the various position of block
      final byte[] bufferCompression =
          new byte[maxCompressedLength(buffer.length)];
      final int length = compress(buffer, buffer.length, bufferCompression,
                                  bufferCompression.length);
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
                        final byte[] output, final int maxOutputLength)
      throws MalformedInputException {
    try {
      return decompressor
          .decompress(input, 0, inputLength, output, 0, maxOutputLength);
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
      inputStream = new FileInputStream(input);
      final byte[] sourceArray = ByteStreams.toByteArray(inputStream);
      outputStream = new FileOutputStream(output);
      // Need to store in front the various position of block
      buffer = new byte[getDecompressedSize(sourceArray, sourceArray.length)];
      final int length =
          decompress(sourceArray, sourceArray.length, buffer, buffer.length);
      outputStream.write(buffer, 0, length);
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
  public int getDecompressedSize(final byte[] input, final int length) {
    return ZstdFrameDecompressor.getDecompressedSize(input, 0, input.length);
  }
}
