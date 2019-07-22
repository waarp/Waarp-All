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
 * Abstract class to allow specific function depending on the underlying JDK to
 * used.
 *
 *
 */
public abstract class FilesystemBasedDirJdkAbstract {
  /**
   * Should the Ftp Server use the Apache Commons Io or not: if not wildcard
   * and
   * freespace (ALLO) will not be
   * supported.
   */
  public static boolean ueApacheCommonsIo = true;

  /**
   * @param directory
   *
   * @return the free space of the given Directory
   */
  public abstract long getFreeSpace(File directory);

  /**
   * Result of ls on FileInterface
   *
   * @param file
   *
   * @return True if the file is executable
   */
  public abstract boolean canExecute(File file);
}
