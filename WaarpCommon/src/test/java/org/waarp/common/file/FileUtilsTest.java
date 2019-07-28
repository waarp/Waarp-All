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
import org.junit.Test;
import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import static org.junit.Assert.*;

public class FileUtilsTest {
  private static File file1;
  private static File file2;
  private static File dirSource;

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
    final FileWriter fileWriterBig = new FileWriter(file1);
    final FileWriter fileWriterBig2 = new FileWriter(file2);
    for (int i = 0; i < 10; i++) {
      fileWriterBig.write("0123456789\n");
      fileWriterBig2.write("0123456789\n");
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    fileWriterBig2.flush();
    fileWriterBig2.close();
  }

  @Test
  public void copy() throws IOException {
    generateFiles();
    File dirTo = new File(dirSource, "test2");
    dirTo.mkdirs();
    try {
      File[] files = FileUtils.copy(new File[] { file1, file2 }, dirTo, true);
      assertNotNull(files);
      assertEquals(2, files.length);
    } catch (Reply550Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  @Test
  public void deleteRecursiveDir() throws IOException {
    generateFiles();
    assertTrue(FileUtils.fileExist(file1.getName(), file1.getParent()));
    assertFalse(FileUtils.deleteRecursiveDir(dirSource));
    assertTrue(FileUtils.forceDeleteRecursiveDir(dirSource));
    generateFiles();
    file1.delete();
    file2.delete();
    assertTrue(FileUtils.deleteRecursiveDir(dirSource));
  }

  @Test
  public void computeGlobalHash() throws IOException, NoSuchAlgorithmException {
    generateFiles();
    FilesystemBasedDigest digest = new FilesystemBasedDigest(DigestAlgo.SHA512);
    FileUtils.computeGlobalHash(digest, file1, (int) file1.length());
    FilesystemBasedDigest digest2 =
        new FilesystemBasedDigest(DigestAlgo.SHA512);
    FileUtils.computeGlobalHash(digest2, file1, (int) file1.length() - 10);
    assertFalse(
        FilesystemBasedDigest.digestEquals(digest.Final(), digest2.Final()));
  }
}