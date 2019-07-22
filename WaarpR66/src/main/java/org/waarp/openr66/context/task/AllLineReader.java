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
package org.waarp.openr66.context.task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;

/**
 * This class is used with external process in order to get the All echo from
 * the stdout of the process.
 *
 *
 */
class AllLineReader implements Runnable {
  private final BufferedReader reader;
  /**
   * This will be the result at the end
   */
  private final StringBuilder lastLine = new StringBuilder();

  public AllLineReader(PipedInputStream inputStream) {
    reader = new BufferedReader(new InputStreamReader(inputStream));
  }

  @Override
  public void run() {
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        line = line.trim();
        if (!line.isEmpty()) {
          getLastLine().append(line).append('\n');
        }
      }
    } catch (final IOException e) {
      // Could be a "Write end dead", which means the end of the thread
      // writer is found
      // before the thread closes the write pipe
    }
    try {
      reader.close();
    } catch (final IOException e) {
    }
  }

  /**
   * @return the lastLine
   */
  public StringBuilder getLastLine() {
    return lastLine;
  }

}
