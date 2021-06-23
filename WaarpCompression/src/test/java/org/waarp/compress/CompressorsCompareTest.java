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
package org.waarp.compress;

import com.github.luben.zstd.Zstd;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.waarp.compress.zlib.ZlibCodec;
import org.waarp.compress.zstdjni.ZstdJniCodec;
import org.waarp.compress.zstdsafe.ZstdSafeCodec;
import org.waarp.compress.zstdunsafe.ZstdUnsafeCodec;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class CompressorsCompareTest {
  private static final int BYTE_SIZE = 128 * 1024;
  private static final int MAX_SIZE = (int) Zstd.compressBound(BYTE_SIZE);
  private static final byte[][] bytess = new byte[10][BYTE_SIZE];
  private static final double REPEAT = 10.0;

  final ZstdUnsafeCodec unsafeCodec = new ZstdUnsafeCodec();
  final ZstdSafeCodec safeCodec = new ZstdSafeCodec();
  final ZstdJniCodec jniCodec = new ZstdJniCodec();
  final ZlibCodec zlibCodec = new ZlibCodec();
  static WaarpZstdCodec waarpZstdCodec;
  private final byte[] bufferc = new byte[MAX_SIZE];
  private final byte[] bufferd = new byte[BYTE_SIZE];

  @BeforeClass
  public static void initialize() throws InterruptedException {
    final Random random = new Random();
    for (int ratio = 1; ratio <= 10; ratio++) {
      for (int i = 0; i < BYTE_SIZE; i++) {
        bytess[ratio - 1][i] = (byte) (random.nextInt(256 / ratio) & 0xFF);
      }
    }
    waarpZstdCodec = new WaarpZstdCodec();
    Thread.sleep(1000);
  }

  byte[] startup(final int variationFactor) {
    return bytess[variationFactor - 1];
  }

  long toWrite = 0;
  long compressedSize = 0;
  long decompressedSize = 0;
  long start = 0;
  long end = 0;

  public void destroy(final String type) {
    end = System.currentTimeMillis();
    try {
      Thread.sleep(100);
    } catch (final InterruptedException e) {
      //
    }
    System.out.println(
        type + " Compressed " + compressedSize + " Decompressed " +
        decompressedSize + " (" + toWrite + ") Ratio " +
        (float) decompressedSize / compressedSize * 100.0 + " factice(" +
        (float) toWrite / compressedSize * 100.0 + ") in " +
        (REPEAT / (end - start)) * 1000.0);
    compressedSize = 0;
    decompressedSize = 0;
    toWrite = 0;
    start = 0;
    end = 0;
  }

  public void test(final CompressorCodec compressor,
                   final CompressorCodec decompressor, final String type) {
    byte[] bytes = startup(1);
    byte[] compressed;
    byte[] decompressed = null;
    for (int j = 0; j < REPEAT / 10; j++) {
      compressed = compress(compressor, bytes);
      decompressed = decompress(decompressor, compressed);
    }
    Assert.assertArrayEquals(bytes, decompressed);
    start = System.currentTimeMillis();
    for (int ratio = 1; ratio <= 10; ratio++) {
      bytes = startup(ratio);
      for (int j = 0; j < REPEAT; j++) {
        compressed = compress(compressor, bytes);
        decompressed = decompress(decompressor, compressed);
      }
      Assert.assertArrayEquals(bytes, decompressed);
    }
    destroy(type);
  }

  @Test
  public void testWaarpBased() {
    test(waarpZstdCodec.getCompressorCodec(),
         waarpZstdCodec.getCompressorCodec(), "Waarp");
    testBuffered(waarpZstdCodec.getCompressorCodec(),
                 waarpZstdCodec.getCompressorCodec(), "WaarpBuffered");
  }

  @Test
  public void testJniBased() {
    test(jniCodec, jniCodec, "Jni");
    testBuffered(jniCodec, jniCodec, "JniBuffered");
  }

  @Test
  public void testZlibBased() {
    test(zlibCodec, zlibCodec, "Zlib");
    testBuffered(zlibCodec, zlibCodec, "ZlibBuffered");
  }

  @Test
  public void testJavaUnsafeBased() {
    test(unsafeCodec, unsafeCodec, "JavaUnsafe");
    testBuffered(unsafeCodec, unsafeCodec, "JavaUnsafeBuffered");
  }

  @Test
  public void testJniJavaUnsafeBased() {
    test(jniCodec, unsafeCodec, "JniJavaUnsafe");
    testBuffered(jniCodec, unsafeCodec, "JniJavaUnsafeBuffered");
  }

  @Test
  public void testJavaUnsafeJniBased() {
    test(unsafeCodec, jniCodec, "JavaUnsafeJni");
    testBuffered(unsafeCodec, jniCodec, "JavaUnsafeJniBuffered");
  }

  @Test
  public void testJavaSafeBased() {
    test(safeCodec, safeCodec, "JavaSafe");
    testBuffered(safeCodec, safeCodec, "JavaSafeBuffered");
  }

  @Test
  public void testJniJavaSafeBased() {
    test(jniCodec, safeCodec, "JniJavaSafe");
    testBuffered(jniCodec, safeCodec, "JniJavaSafeBuffered");
  }

  @Test
  public void testJavaSafeJniBased() {
    test(safeCodec, jniCodec, "JavaSafeJni");
    testBuffered(safeCodec, jniCodec, "JavaSafeJniBuffered");
  }

  @Test
  public void testJavaSafeJavaUnsafeBased() {
    test(safeCodec, unsafeCodec, "JavaSafeJavaUnsafe");
    testBuffered(safeCodec, unsafeCodec, "JavaSafeJavaUnsafeBuffered");
  }

  @Test
  public void testJavaUnsafeJavaSafeBased() {
    test(unsafeCodec, safeCodec, "JavaUnsafeJavaSafe");
    testBuffered(unsafeCodec, safeCodec, "JavaUnsafeJavaSafeBuffered");
  }

  @Test
  public void xtremTest() throws InterruptedException {
    final int run = 100;
    final ExecutorService executorService = Executors.newFixedThreadPool(run);
    for (int i = 0; i < run; i++) {
      executorService.execute(new ToRun(waarpZstdCodec));
    }
    executorService.shutdown();
    while (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
      System.out.println("Continue");
    }
    System.out.println("End");
    Assert.assertEquals("Should have no error", 0, errorToRun);
  }

  byte[] compress(final CompressorCodec compressor, final byte[] bytes) {
    toWrite += bytes.length;
    final byte[] compressed = compressor.compress(bytes, bytes.length);
    compressedSize += compressed.length;
    return compressed;
  }

  byte[] decompress(final CompressorCodec decompressor,
                    final byte[] compressed) {
    final byte[] decompressed =
        decompressor.decompress(compressed, compressed.length);
    decompressedSize += decompressed.length;
    return decompressed;
  }

  public void testBuffered(final CompressorCodec compressor,
                           final CompressorCodec decompressor,
                           final String type) {
    byte[] bytes = startup(1);
    int compressed;
    int decompressed = 0;
    for (int j = 0; j < REPEAT / 10; j++) {
      compressed = compress(compressor, bytes, BYTE_SIZE);
      decompressed = decompress(decompressor, bufferc, compressed);
    }
    Assert.assertEquals(BYTE_SIZE, decompressed);
    Assert.assertTrue(Arrays.equals(bytes, bufferd));
    start = System.currentTimeMillis();
    for (int ratio = 1; ratio <= 10; ratio++) {
      bytes = startup(ratio);
      for (int j = 0; j < REPEAT; j++) {
        compressed = compress(compressor, bytes, BYTE_SIZE);
        decompressed = decompress(decompressor, bufferc, compressed);
      }
      Assert.assertEquals(BYTE_SIZE, decompressed);
      Assert.assertTrue(Arrays.equals(bytes, bufferd));
    }
    destroy(type);
  }

  int compress(final CompressorCodec compressor, final byte[] bytes,
               final int inputLength) {
    toWrite += inputLength;
    final int out = compressor.compress(bytes, inputLength, bufferc, MAX_SIZE);
    compressedSize += out;
    return out;
  }

  int decompress(final CompressorCodec decompressor, final byte[] compressed,
                 final int compressedSize) {
    final int out =
        decompressor.decompress(compressed, compressedSize, bufferd, BYTE_SIZE);
    decompressedSize += out;
    return out;
  }

  static int errorToRun = 0;

  public class ToRun implements Runnable {
    WaarpZstdCodec runnableWaarpZstdCodec;

    public ToRun(final WaarpZstdCodec waarpZstdCodec) {
      runnableWaarpZstdCodec = waarpZstdCodec;
    }

    @Override
    public void run() {
      try {
        byte[] bytes = startup(1);
        byte[] compressed;
        byte[] decompressed = null;
        for (int j = 0; j < REPEAT / 10; j++) {
          compressed = runnableWaarpZstdCodec.getCompressorCodec()
                                             .compress(bytes, bytes.length);
          decompressed = runnableWaarpZstdCodec.getCompressorCodec()
                                               .decompress(compressed,
                                                           compressed.length);
        }
        if (!Arrays.equals(bytes, decompressed)) {
          errorToRun++;
          return;
        }
        start = System.currentTimeMillis();
        for (int ratio = 1; ratio <= 10; ratio++) {
          bytes = startup(ratio);
          for (int j = 0; j < REPEAT; j++) {
            compressed = runnableWaarpZstdCodec.getCompressorCodec()
                                               .compress(bytes, bytes.length);
            decompressed = runnableWaarpZstdCodec.getCompressorCodec()
                                                 .decompress(compressed,
                                                             compressed.length);
          }
          if (!Arrays.equals(bytes, decompressed)) {
            errorToRun++;
            return;
          }
        }
      } catch (final Exception e) {
        System.err.println("Error found: " + e.getMessage());
        errorToRun++;
      }
    }
  }
}
