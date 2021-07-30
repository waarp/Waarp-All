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
package org.waarp.common.file.filesystembased.specific;

import java.io.File;

/**
 * JDK6 version of specific functions for Filesystem.
 */
public class FilesystemBasedDirJdk6 extends FilesystemBasedDirJdkAbstract {
  /**
   * @param file
   *
   * @return True if the file is executable
   */
  @Override
  public final boolean canExecute(final File file) {
    return file.canExecute();
  }

  /**
   * @param directory
   *
   * @return the free space of the given Directory
   */
  @Override
  public final long getFreeSpace(final File directory) {
    try {
      return directory.getFreeSpace();
    } catch (final Exception e) {
      return -1;
    }
  }
}
