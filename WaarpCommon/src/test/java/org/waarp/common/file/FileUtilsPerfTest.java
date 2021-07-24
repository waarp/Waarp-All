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

package org.waarp.common.file;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.utility.FileTestUtils;
import org.waarp.common.utility.TestWatcherJunit4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import static org.junit.Assert.*;

public class FileUtilsPerfTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static File file1;
  private static File file2;
  private static File dirSource;
  private static byte[] buffer;
  private static final long SIZE_FROM_BUF = 1024;//64MB

  @AfterClass
  public static void terminate() {
    FileUtils.forceDeleteRecursiveDir(dirSource);
  }

  public static void generateFiles() throws IOException {
    dirSource = new File("/tmp/testfile");
    final File dir = new File(dirSource, "test");
    dir.mkdirs();
    file1 = new File(dir, "testFile2.txt");
    file2 = new File(dir, "testFile.txt");
    StringBuilder builder = new StringBuilder(FileUtils.ZERO_COPY_CHUNK_SIZE);
    for (int i = 0; i < 1024; i++) {
      builder.append(
          "01234567890123456789012345678901234567890123456789012345678901234");
    }
    builder.setLength(FileUtils.ZERO_COPY_CHUNK_SIZE);
    String content = builder.toString();
    buffer = content.getBytes();
    FileTestUtils.createTestFile(file1, (int) SIZE_FROM_BUF, content);
  }

  @Test
  public void copy() throws IOException {
    generateFiles();
    for (int i = 0; i < 5; i++) {
      try {
        long start = System.currentTimeMillis();
        FileUtils.copy(file1, file2, false, false);
        long stop = System.currentTimeMillis();
        SysErrLogger.FAKE_LOGGER.sysout("Copy Time: " + (stop - start));
        assertEquals(file1.length(), file2.length());
        file2.delete();
        start = System.currentTimeMillis();
        FileChannel fileChannelIn = new FileInputStream(file1).getChannel();
        FileChannel fileChannelOut = new FileOutputStream(file2).getChannel();
        long size = file1.length();
        long position = 0;
        while (true) {
          long read = fileChannelIn.transferTo(position, size, fileChannelOut);
          if (read == 0) {
            break;
          }
          size -= read;
          position += read;
        }
        fileChannelIn.close();
        fileChannelOut.force(false);
        fileChannelOut.close();
        stop = System.currentTimeMillis();
        SysErrLogger.FAKE_LOGGER.sysout("Copy FC Time: " + (stop - start));
        assertEquals(file1.length(), file2.length());
        assertEquals(SIZE_FROM_BUF * FileUtils.ZERO_COPY_CHUNK_SIZE, position);
        file2.delete();
        start = System.currentTimeMillis();
        fileChannelIn = new FileInputStream(file1).getChannel();
        fileChannelOut = new FileOutputStream(file2).getChannel();
        size = file1.length();
        position = 0;
        while (true) {
          long read =
              fileChannelOut.transferFrom(fileChannelIn, position, size);
          if (read == 0) {
            break;
          }
          size -= read;
          position += read;
        }
        fileChannelIn.close();
        fileChannelOut.force(false);
        fileChannelOut.close();
        stop = System.currentTimeMillis();
        SysErrLogger.FAKE_LOGGER.sysout("Copy FC from Time: " + (stop - start));
        assertEquals(file1.length(), file2.length());
        assertEquals(SIZE_FROM_BUF * FileUtils.ZERO_COPY_CHUNK_SIZE, position);
        file2.delete();
        start = System.currentTimeMillis();
        fileChannelIn = new FileInputStream(file1).getChannel();
        fileChannelOut = new FileOutputStream(file2).getChannel();
        size = file1.length();
        position = 0;
        ByteBuffer byteBuf = ByteBuffer.wrap(buffer);
        while (true) {
          byteBuf.clear();
          int read = fileChannelIn.read(byteBuf);
          if (read <= 0) {
            break;
          }
          byteBuf.flip();
          int write = fileChannelOut.write(byteBuf);
          if (read != write) {
            SysErrLogger.FAKE_LOGGER.syserr(
                "Not same size " + read + " != " + write);
          }
          size -= read;
          position += read;
        }
        fileChannelIn.close();
        fileChannelOut.force(false);
        fileChannelOut.close();
        stop = System.currentTimeMillis();
        SysErrLogger.FAKE_LOGGER.sysout(
            "Copy FC Buffer Time: " + (stop - start));
        assertEquals(file1.length(), file2.length());
        assertEquals(SIZE_FROM_BUF * FileUtils.ZERO_COPY_CHUNK_SIZE, position);
        file2.delete();
        start = System.currentTimeMillis();
        position = 0;
        FileInputStream inputStream = new FileInputStream(file1);
        FileOutputStream outputStream = new FileOutputStream(file2);
        while (true) {
          final int r = inputStream.read(buffer);
          if (r == -1) {
            break;
          }
          outputStream.write(buffer, 0, r);
          position += r;
        }
        inputStream.close();
        outputStream.flush();
        outputStream.close();
        stop = System.currentTimeMillis();
        SysErrLogger.FAKE_LOGGER.sysout("Copy Stream Time: " + (stop - start));
        assertEquals(file1.length(), file2.length());
        assertEquals(SIZE_FROM_BUF * FileUtils.ZERO_COPY_CHUNK_SIZE, position);
        file2.delete();
      } catch (Reply550Exception e) {
        e.printStackTrace();
        fail(e.getMessage());
      }
    }
  }

}