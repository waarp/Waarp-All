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

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.logging.SysErrLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;

import static com.google.common.base.Preconditions.*;

/**
 * File Utils
 */
public final class FileUtils {
  public static final int ZERO_COPY_CHUNK_SIZE = 64 * 1024;

  private static final File[] FILE_0_LENGTH = new File[0];

  private FileUtils() {
  }

  public static final void close(final Reader stream) {
    if (stream == null) {
      return;
    }
    try {
      stream.close();
    } catch (final Exception ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
    }
  }

  public static final void close(final Writer stream) {
    if (stream == null) {
      return;
    }
    try {
      stream.flush();
    } catch (final Exception ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
    }
    try {
      stream.close();
    } catch (final Exception ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
    }
  }

  public static final void close(final InputStream stream) {
    if (stream == null) {
      return;
    }
    try {
      stream.close();
    } catch (final Exception ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
    }
  }

  public static final void close(final OutputStream stream) {
    if (stream == null) {
      return;
    }
    try {
      stream.flush();
    } catch (final Exception ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
    }
    try {
      stream.close();
    } catch (final Exception ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
    }
  }

  public static final void close(final RandomAccessFile accessFile) {
    if (accessFile == null) {
      return;
    }
    try {
      accessFile.close();
    } catch (final Exception ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
    }
  }

  /**
   * Delete the directory associated with the File as path if empty
   *
   * @param directory
   *
   * @return True if deleted, False else.
   */
  public static final boolean deleteDir(final File directory) {
    if (directory == null) {
      return true;
    }
    if (!directory.exists()) {
      return true;
    }
    if (!directory.isDirectory()) {
      return false;
    }
    return directory.delete();
  }

  /**
   * Delete physically the file
   *
   * @param file
   *
   * @return True if OK, else if not (or if the file never exists).
   */
  public static final boolean delete(final File file) {
    if (!file.exists()) {
      return true;
    }
    return file.delete();
  }

  /**
   * Copy from a directory to a directory with recursivity
   *
   * @param from
   * @param directoryTo
   * @param move True if the copy is in fact a move operation
   *
   * @return the group of copy files or null (partially or totally) if an
   *     error occurs
   *
   * @throws Reply550Exception
   */
  public static File[] copyRecursive(final File from, final File directoryTo,
                                     final boolean move)
      throws Reply550Exception {
    if (from == null || directoryTo == null) {
      return null;
    }
    final ArrayList<File> to = new ArrayList<File>();
    if (createDir(directoryTo)) {
      final File[] subfrom = from.listFiles();
      if (subfrom != null) {
        for (final File element : subfrom) {
          if (element.isFile()) {
            to.add(copyToDir(element, directoryTo, move));
          } else {
            final File newTo = new File(directoryTo, element.getName());
            newTo.mkdirs(); //NOSONAR
            final File[] copied = copyRecursive(element, newTo, move);
            to.addAll(Arrays.asList(copied));
          }
        }
      }
    }
    return to.toArray(FILE_0_LENGTH);
  }

  /**
   * Copy a group of files to a directory (will ignore directories)
   *
   * @param from
   * @param directoryTo
   * @param move True if the copy is in fact a move operation
   *
   * @return the group of copy files or null (partially or totally) if an
   *     error occurs
   *
   * @throws Reply550Exception
   */
  public static File[] copy(final File[] from, final File directoryTo,
                            final boolean move) throws Reply550Exception {
    if (from == null || directoryTo == null) {
      return null;
    }
    File[] to = null;
    if (createDir(directoryTo)) {
      to = new File[from.length];
      for (int i = 0; i < from.length; i++) {
        if (from[i].isFile()) {
          to[i] = copyToDir(from[i], directoryTo, move);
        }
      }
    }
    return to;
  }

  /**
   * Create the directory associated with the File as path
   *
   * @param directory
   *
   * @return True if created, False else.
   */
  public static final boolean createDir(final File directory) {
    if (directory == null) {
      return false;
    }
    if (directory.isDirectory()) {
      return true;
    }
    return directory.mkdirs();
  }

  /**
   * Copy one file to a directory
   *
   * @param from
   * @param directoryTo
   * @param move True if the copy is in fact a move operation
   *
   * @return The copied file or null if an error occurs
   *
   * @throws Reply550Exception
   */
  private static File copyToDir(final File from, final File directoryTo,
                                final boolean move) throws Reply550Exception {
    if (from == null || directoryTo == null) {
      throw new Reply550Exception("Source or Destination is null");
    }
    if (!from.isFile()) {
      throw new Reply550Exception("Source file is a directory");
    }
    if (createDir(directoryTo)) {
      final File to = new File(directoryTo, from.getName());
      copy(from, to, move, false);
      return to;
    }
    throw new Reply550Exception("Cannot access to parent dir of destination");
  }

  /**
   * Copy one file to another one
   *
   * @param from
   * @param to
   * @param move True if the copy is in fact a move operation
   * @param append True if the copy is in append
   *
   * @throws Reply550Exception
   */
  public static void copy(final File from, final File to, final boolean move,
                          final boolean append) throws Reply550Exception {
    if (from == null || to == null) {
      throw new Reply550Exception("Source or Destination is null");
    }
    if (!from.isFile()) {
      throw new Reply550Exception("Source file is a directory");
    }
    final File directoryTo = to.getParentFile();
    if (createDir(directoryTo)) {
      if (move && from.renameTo(to)) {
        return;
      }
      FileInputStream inputStream = null;
      FileOutputStream outputStream = null;
      try {
        try {
          inputStream = new FileInputStream(from.getPath());
        } catch (final FileNotFoundException e) {
          throw new Reply550Exception("Cannot read source file");
        }
        try {
          outputStream = new FileOutputStream(to.getPath(), append);
        } catch (final FileNotFoundException e) {
          throw new Reply550Exception("Cannot write destination file");
        }
        try {
          copy(inputStream, outputStream);
        } catch (final IOException e) {
          throw new Reply550Exception("Cannot copy");
        }
      } finally {
        close(outputStream);
        close(inputStream);
      }
      if (move) {
        // do not test the delete
        from.delete(); // NOSONAR
      }
      return;
    }
    throw new Reply550Exception("Cannot access to parent dir of destination");
  }

  /**
   * Delete the directory and its subdirs associated with the File dir if
   * empty
   *
   * @param dir
   *
   * @return True if deleted, False else.
   */
  private static boolean deleteRecursiveFileDir(final File dir) {
    if (dir == null) {
      return true;
    }
    boolean retour = true;
    if (!dir.exists()) {
      return true;
    }
    final File[] list = dir.listFiles();
    if (list == null || list.length == 0) {
      return dir.delete();
    }
    for (final File file : list) {
      if (file.isDirectory()) {
        if (!deleteRecursiveFileDir(file)) {
          retour = false;
        }
      } else {
        return false;
      }
    }
    if (retour) {
      retour = dir.delete();
    }
    return retour;
  }

  /**
   * Delete all
   *
   * @param directory
   *
   * @return True if all sub directories or files are deleted, False else.
   */
  private static boolean forceDeleteRecursiveSubDir(final File directory) {
    if (directory == null) {
      return true;
    }
    boolean retour = true;
    if (!directory.exists()) {
      return true;
    }
    if (!directory.isDirectory()) {
      return directory.delete();
    }
    final File[] list = directory.listFiles();
    if (list == null || list.length == 0) {
      return true;
    }
    for (final File file : list) {
      if (file.isDirectory()) {
        if (!forceDeleteRecursiveSubDir(file)) {
          retour = false;
        }
      } else {
        if (!file.delete()) {
          retour = false;
        }
      }
    }
    if (retour) {
      retour = directory.delete();
    }
    return retour;
  }

  /**
   * Delete all its subdirs and files associated, not itself
   *
   * @param directory
   *
   * @return True if all sub directories or files are deleted, False else.
   */
  public static boolean forceDeleteRecursiveDir(final File directory) {
    if (directory == null) {
      return true;
    }
    boolean retour = true;
    if (!directory.exists()) {
      return true;
    }
    if (!directory.isDirectory()) {
      return false;
    }
    final File[] list = directory.listFiles();
    if (list == null || list.length == 0) {
      return true;
    }
    for (final File file : list) {
      if (file.isDirectory()) {
        if (!forceDeleteRecursiveSubDir(file)) {
          retour = false;
        }
      } else {
        if (!file.delete()) {
          retour = false;
        }
      }
    }
    return retour;
  }

  /**
   * Delete the directory and its subdirs associated with the File as path if
   * empty
   *
   * @param directory
   *
   * @return True if deleted, False else.
   */
  public static boolean deleteRecursiveDir(final File directory) {
    if (directory == null) {
      return true;
    }
    boolean retour = true;
    if (!directory.exists()) {
      return true;
    }
    if (!directory.isDirectory()) {
      return false;
    }
    final File[] list = directory.listFiles();
    if (list == null || list.length == 0) {
      retour = directory.delete();
      return retour;
    }
    for (final File file : list) {
      if (file.isDirectory()) {
        if (!deleteRecursiveFileDir(file)) {
          retour = false;
        }
      } else {
        retour = false;
      }
    }
    if (retour) {
      retour = directory.delete();
    }
    return retour;
  }

  /**
   * @param fileName
   * @param path
   *
   * @return true if the file exist in the specified path
   */
  public static final boolean fileExist(final String fileName,
                                        final String path) {
    boolean exist = false;
    final String fileString = path + File.separator + fileName;
    final File file = new File(fileString);
    if (file.exists()) {
      exist = true;
    }
    return exist;
  }

  /**
   * Get the list of files from a given directory
   *
   * @param directory
   *
   * @return the list of files (as an array)
   */
  public static final File[] getFiles(final File directory) {
    if (directory == null || !directory.isDirectory()) {
      return FILE_0_LENGTH;
    }
    return directory.listFiles();
  }

  /**
   * Compute global hash (if possible) from a file but up to length
   *
   * @param digest
   * @param file
   * @param length
   */
  public static void computeGlobalHash(final FilesystemBasedDigest digest,
                                       final File file, final int length) {
    if (digest == null) {
      return;
    }
    final byte[] bytes = new byte[ZERO_COPY_CHUNK_SIZE];
    int still = length;
    int len = Math.min(still, ZERO_COPY_CHUNK_SIZE);
    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(file);
      int read = 1;
      while (read > 0) {
        read = inputStream.read(bytes, 0, len);
        if (read <= 0) {
          break;
        }
        digest.Update(bytes, 0, read);
        still -= read;
        if (still <= 0) {
          break;
        }
        len = Math.min(still, ZERO_COPY_CHUNK_SIZE);
      }
    } catch (final FileNotFoundException e) {
      // error
    } catch (final IOException e) {
      // error
    } finally {
      close(inputStream);
    }
  }

  /**
   * Get the list of files from a given directory and a filter
   *
   * @param directory
   * @param filter
   *
   * @return the list of files (as an array)
   */
  public static final File[] getFiles(final File directory,
                                      final FilenameFilter filter) {
    if (directory == null || !directory.isDirectory()) {
      return FILE_0_LENGTH;
    }
    return directory.listFiles(filter);
  }

  /**
   * Read one compressed file and return the associated BufferedReader
   *
   * @param fileIn
   * @param fileOut
   *
   * @return the size of the uncompressed file or -1 if an error occurs
   *
   * @throws Reply550Exception
   */
  public static long uncompressedBz2File(final File fileIn, final File fileOut)
      throws Reply550Exception {
    FileInputStream fin = null;
    CompressorInputStream input = null;
    FileOutputStream output = null;
    try {
      fin = new FileInputStream(fileIn);
      input = new BZip2CompressorInputStream(fin);
      output = new FileOutputStream(fileOut);
      return copy(input, output);
    } catch (final IOException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      return -1;
    } finally {
      FileUtils.close(output);
      FileUtils.close(input);
      FileUtils.close(fin);
    }
  }


  /**
   * From Google Guava copy within ByteStreams
   *
   * Copies all bytes from the input stream to the output stream. Does not close or flush either
   * stream.
   *
   * @param from the input stream to read from
   * @param to the output stream to write to
   *
   * @return the number of bytes copied
   *
   * @throws IOException if an I/O error occurs
   */
  public static long copy(final InputStream from, final OutputStream to)
      throws IOException {
    checkNotNull(from);
    checkNotNull(to);
    final byte[] buf = new byte[ZERO_COPY_CHUNK_SIZE];
    long total = 0;
    while (true) {
      final int r = from.read(buf);
      if (r == -1) {
        break;
      }
      to.write(buf, 0, r);
      total += r;
    }
    return total;
  }
}
