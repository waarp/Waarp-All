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

package org.waarp.common.tar;

import org.junit.Test;
import org.waarp.common.file.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class ZipUtilityTest {

  @Test
  public void createZipFromDirectory() throws IOException {
    final File dir = new File("/tmp/tests/dir");
    FileUtils.createDir(dir);
    final File test = new File(dir, "test");
    test.createNewFile();
    final File test2 = new File(dir, "test2");
    test2.createNewFile();
    final File zip = new File(dir.getParentFile(), "testzip.zip");
    assertTrue(ZipUtility.createZipFromDirectory(dir.getAbsolutePath(),
                                                 zip.getAbsolutePath(), false));
    FileUtils.forceDeleteRecursiveDir(dir);

    final List<String> list = ZipUtility.unZip(zip, dir);
    assertEquals(2, list.size());
    final File[] arrayFile = FileUtils.getFiles(dir);
    assertEquals(2, arrayFile.length);
    int cpt = 0;
    for (final File file : arrayFile) {
      for (final String name : list) {
        if (name.equals(file.getName())) {
          cpt++;
          break;
        }
      }
    }
    assertEquals(2, cpt);
    FileUtils.forceDeleteRecursiveDir(dir);
    FileUtils.deleteDir(dir);
    FileUtils.delete(zip);
  }

  @Test
  public void createZipFromFiles() throws IOException {
    final File dir = new File("/tmp/tests/dir");
    FileUtils.createDir(dir);
    final File test = new File(dir, "test");
    test.createNewFile();
    final File test2 = new File(dir, "test2");
    test2.createNewFile();
    File[] arrayFile = dir.listFiles();
    final List<File> listFile = new ArrayList<File>();
    Collections.addAll(listFile, arrayFile);
    final File zip = new File(dir.getParentFile(), "testzip.zip");
    assertTrue(ZipUtility.createZipFromFiles(listFile, zip.getAbsolutePath()));
    FileUtils.forceDeleteRecursiveDir(dir);

    List<String> list = ZipUtility.unZip(zip, dir);
    assertEquals(2, list.size());
    arrayFile = FileUtils.getFiles(dir);
    assertEquals(2, arrayFile.length);
    int cpt = 0;
    for (final File file : arrayFile) {
      for (final String name : list) {
        if (name.equals(file.getName())) {
          cpt++;
          break;
        }
      }
    }
    assertEquals(2, cpt);
    FileUtils.delete(zip);
    assertTrue(ZipUtility.createZipFromFiles(arrayFile, zip.getAbsolutePath()));
    FileUtils.forceDeleteRecursiveDir(dir);

    list = ZipUtility.unZip(zip, dir);
    assertEquals(2, list.size());
    final File[] arrayFiles2 = arrayFile = FileUtils.getFiles(dir);
    assertEquals(2, arrayFiles2.length);
    cpt = 0;
    for (final File file : arrayFile) {
      for (final File file2 : arrayFiles2) {
        if (file2.getName().equals(file.getName())) {
          cpt++;
          break;
        }
      }
    }
    assertEquals(2, cpt);

    FileUtils.forceDeleteRecursiveDir(dir);
    FileUtils.deleteDir(dir);
    FileUtils.delete(zip);
  }
}