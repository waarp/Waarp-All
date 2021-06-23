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

package org.waarp.compress;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.github.luben.zstd.util.Native;
import com.google.common.io.ByteStreams;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.compress.zstdjni.ZstdJniCodec;
import org.waarp.compress.zstdsafe.ZstdSafeCodec;
import org.waarp.compress.zstdunsafe.UnsafeUtil;
import org.waarp.compress.zstdunsafe.ZstdUnsafeCodec;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Waarp Compression Codec using byte array.<br>
 * This will try to use one of the three implementations, according to
 * environment, in the following order:<br>
 * <ul>
 *   <li>JNI based: the fastest, best compression ratio and less CPU</li>
 *   <li>Unsafe based: 2 times slower than JNI but almost same in ratio and a
 *   bit more CPU</li>
 *   <li>Safe based: 4 times slower than JNI but almost same in ratio and a
 *   bit more CPU</li>
 * </ul>
 */
public class WaarpZstdCodec {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpZstdCodec.class);
  private final CompressorCodec codec;

  public WaarpZstdCodec() {
    boolean jniLoaded;
    try {
      Native.load();
      jniLoaded = true;
    } catch (final Exception e) {
      jniLoaded = false;
      logger.warn("Cannot load JNI based ZSTD codec: {}", e.getMessage());
    }
    if (jniLoaded) {
      codec = new ZstdJniCodec();
    } else {
      if (UnsafeUtil.isValid()) {
        codec = new ZstdUnsafeCodec();
      } else {
        codec = new ZstdSafeCodec();
      }
    }
  }

  /**
   * @return the current Compressor Codec implementation
   */
  public CompressorCodec getCompressorCodec() {
    return codec;
  }

  /**
   * @param bufferSize
   *
   * @return the maximum size of a compressed buffer of size given
   */
  public static int getMaxCompressedSize(final int bufferSize) {
    return ZstdSafeCodec.maxCompressedSize(bufferSize);
  }

  /**
   * Compress the file input to the file output
   *
   * @param input
   * @param output
   *
   * @return the new length
   *
   * @throws MalformedInputException
   */
  public long compress(final File input, final File output)
      throws MalformedInputException {
    InputStream inputStream = null;
    OutputStream outputStream = null;
    try {
      final byte[] buffer;
      if (codec instanceof ZstdJniCodec) {
        inputStream = new FileInputStream(input);
        outputStream = new ZstdOutputStream(new FileOutputStream(output), 1);
        buffer = new byte[Zstd.blockSizeMax()];
        while (true) {
          final int r = inputStream.read(buffer);
          if (r == -1) {
            break;
          }
          outputStream.write(buffer, 0, r);
        }
      } else {
        inputStream = new FileInputStream(input);
        buffer = ByteStreams.toByteArray(inputStream);
        outputStream = new FileOutputStream(output);
        // Need to store in front the various position of block
        final byte[] bufferCompression =
            new byte[getMaxCompressedSize(buffer.length)];
        final int length = codec
            .compress(buffer, buffer.length, bufferCompression,
                      bufferCompression.length);
        outputStream.write(bufferCompression, 0, length);
      }
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

  /**
   * Decompress the file input to the file output
   *
   * @param input
   * @param output
   *
   * @return the new length
   *
   * @throws MalformedInputException
   */
  public long decompress(final File input, final File output)
      throws MalformedInputException {
    InputStream inputStream = null;
    OutputStream outputStream = null;
    try {
      final byte[] buffer;
      if (codec instanceof ZstdJniCodec) {
        inputStream = new ZstdInputStream(new FileInputStream(input));
        outputStream = new FileOutputStream(output);
        buffer = new byte[Zstd.blockSizeMax()];
        while (true) {
          final int r = inputStream.read(buffer);
          if (r == -1) {
            break;
          }
          outputStream.write(buffer, 0, r);
        }
      } else {
        inputStream = new FileInputStream(input);
        final byte[] sourceArray = ByteStreams.toByteArray(inputStream);
        outputStream = new FileOutputStream(output);
        // Need to store in front the various position of block
        buffer = new byte[codec
            .getDecompressedSize(sourceArray, sourceArray.length)];
        final int length = codec
            .decompress(sourceArray, sourceArray.length, buffer, buffer.length);
        outputStream.write(buffer, 0, length);
      }
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
}
