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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileTestUtils {

  public static void createTestFile(final File file, final int sizePer10,
                                    final String content) throws IOException {
    final FileWriter fileWriterBig = new FileWriter(file);
    for (int i = 0; i < sizePer10; i++) {
      fileWriterBig.write(content);
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    while (!file.isFile() && !file.canRead() && file.length() == 0) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        throw new IOException(e);
      }
    }
  }

  public static void createTestFile(final File file, final int sizePer10)
      throws IOException {
    createTestFile(file, sizePer10, "0123456789\n");
  }
}
