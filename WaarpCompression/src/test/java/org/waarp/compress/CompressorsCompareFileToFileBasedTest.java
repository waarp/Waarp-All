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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.waarp.compress.zlib.ZlibCodec;
import org.waarp.compress.zstdjni.ZstdJniCodec;
import org.waarp.compress.zstdsafe.ZstdSafeCodec;
import org.waarp.compress.zstdunsafe.ZstdUnsafeCodec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CompressorsCompareFileToFileBasedTest {
  private static final List<File> files = new ArrayList<File>();
  private static final int RUN = 1;
  final ZstdUnsafeCodec unsafeCodec = new ZstdUnsafeCodec();
  final ZstdSafeCodec safeCodec = new ZstdSafeCodec();
  final ZstdJniCodec jniCodec = new ZstdJniCodec();
  final ZlibCodec zlibCodec = new ZlibCodec();
  static WaarpZstdCodec waarpZstdCodec;

  @BeforeClass
  public static void initialize() {
    final File dir = new File(".");
    final File directory = new File(dir.getParent(), "src/test/testdata");
    System.err.println(directory.getAbsolutePath());
    final File[] subFiles = directory.listFiles();
    if (subFiles != null) {
      for (final File file : subFiles) {
        if (file.isFile()) {
          files.add(file);
        } else {
          final File[] subsubFiles = file.listFiles();
          if (subsubFiles != null) {
            for (final File subfile : subsubFiles) {
              if (subfile.isFile()) {
                files.add(subfile);
              }
            }
          }
        }
      }
    }
    waarpZstdCodec = new WaarpZstdCodec();
  }

  void startup() {
    start = System.currentTimeMillis();
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
        (files.size() * 1000.0) / (end - start));
    compressedSize = 0;
    decompressedSize = 0;
    toWrite = 0;
    start = 0;
    end = 0;
  }

  public void test(final CompressorCodec compressor,
                   final CompressorCodec decompressor, final String type)
      throws IOException {
    File source = files.get(0);
    File dest = File.createTempFile("testC", source.getName());
    File destSrc = File.createTempFile("testD", source.getName());
    compress(compressor, source, dest);
    decompress(decompressor, dest, destSrc);
    Assert.assertEquals(source.length(), destSrc.length());
    destSrc.delete();
    dest.delete();
    startup();
    for (final File src : files) {
      dest = File.createTempFile("testC", source.getName());
      destSrc = File.createTempFile("testD", source.getName());
      for (int i = 0; i < RUN; i++) {
        compress(compressor, src, dest);
        decompress(decompressor, dest, destSrc);
      }
      Assert.assertEquals(src.length(), destSrc.length());
      destSrc.delete();
      dest.delete();
    }
    destroy(type);
  }

  @Test
  public void testWaarpBased() throws IOException {
    test(waarpZstdCodec.getCompressorCodec(),
         waarpZstdCodec.getCompressorCodec(), "Waarp");
  }

  @Test
  public void testZlibBased() throws IOException {
    test(zlibCodec, zlibCodec, "Zlib");
  }

  @Test
  public void testJniBased() throws IOException {
    test(jniCodec, jniCodec, "Jni");
  }

  @Test
  public void testJavaUnsafeBased() throws IOException {
    test(unsafeCodec, unsafeCodec, "JavaUnsafe");
  }

  @Test
  public void testJniJavaUnsafeBased() throws IOException {
    test(jniCodec, unsafeCodec, "JniJavaUnsafe");
  }

  @Test
  public void testJavaUnsafeJniBased() throws IOException {
    test(unsafeCodec, jniCodec, "JavaUnsafeJni");
  }

  @Test
  public void testJavaSafeBased() throws IOException {
    test(safeCodec, safeCodec, "JavaSafe");
  }

  @Test
  public void testJniJavaSafeBased() throws IOException {
    test(jniCodec, safeCodec, "JniJavaSafe");
  }

  @Test
  public void testJavaSafeJniBased() throws IOException {
    test(safeCodec, jniCodec, "JavaSafeJni");
  }

  @Test
  public void testJavaSafeJavaUnsafeBased() throws IOException {
    test(safeCodec, unsafeCodec, "JavaSafeJavaUnsafe");
  }

  @Test
  public void testJavaUnsafeJavaSafeBased() throws IOException {
    test(unsafeCodec, safeCodec, "JavaUnsafeJavaSafe");
  }

  long compress(final CompressorCodec compressor, final File source,
                final File dest) {
    toWrite += source.length();
    long length = compressor.compress(source, dest);
    compressedSize += length;
    return length;
  }

  long decompress(final CompressorCodec decompressor, final File source,
                  final File dest) {
    long length = decompressor.decompress(source, dest);
    decompressedSize += length;
    return length;
  }
}
