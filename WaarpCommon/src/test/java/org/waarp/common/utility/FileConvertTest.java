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

package org.waarp.common.utility;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.waarp.common.transcode.CharsetsUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class FileConvertTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testCharsetsUtils() {
    CharsetsUtil.printOutCharsetsAvailable(0);
    CharsetsUtil.printOutCharsetsAvailable(1);
    CharsetsUtil.printOutCharsetsAvailable(2);
    assertTrue(true);
  }

  @Test
  public void testFileConvert() throws IOException {
    final List<File> files = new ArrayList<File>();
    final File filecomp = new File("/tmp/testFile2.txt");
    final File file = new File("/tmp/testFile.txt");
    final FileWriter fileWriterBig = new FileWriter(filecomp);
    final FileWriter fileWriterBig2 = new FileWriter(file);
    for (int i = 0; i < 10; i++) {
      fileWriterBig.write("0123456789\n");
      fileWriterBig2.write("0123456789\n");
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    fileWriterBig2.flush();
    fileWriterBig2.close();
    files.add(file);
    FileConvert fileConvert = new FileConvert(files, true, false, null);
    fileConvert.run();
    fileConvert = new FileConvert(files, false, false, null);
    fileConvert.run();
    assertEquals("Should be equals",
                 WaarpStringUtils.readFile(file.getAbsolutePath()),
                 WaarpStringUtils.readFile(filecomp.getAbsolutePath()));
  }

  @Test
  public void testCharsetsUtil() throws IOException {
    final File fileto = new File("/tmp/to.txt");
    final File filefrom = new File("/tmp/from.txt");
    final FileWriter fileWriterBig = new FileWriter(filefrom);
    for (int i = 0; i < 10; i++) {
      fileWriterBig.write("0123456789\n");
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    final String[] args = {
        "-to", "/tmp/to.txt", "UTF-8", "-from", "/tmp/from.txt", "UTF-8"
    };
    CharsetsUtil.main(args);
    assertTrue(fileto.exists());
  }
}
