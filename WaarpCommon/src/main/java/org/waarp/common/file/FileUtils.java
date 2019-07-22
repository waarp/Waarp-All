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

import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.digest.FilesystemBasedDigest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * File Utils
 *
 *
 */
public class FileUtils {

  /**
   * Delete the directory associated with the File as path if empty
   *
   * @param directory
   *
   * @return True if deleted, False else.
   */
  public static final boolean deleteDir(File directory) {
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
  public static final boolean delete(File file) {
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
  public static File[] copyRecursive(File from, File directoryTo, boolean move)
      throws Reply550Exception {
    if (from == null || directoryTo == null) {
      return null;
    }
    final ArrayList<File> to = new ArrayList<File>();
    if (createDir(directoryTo)) {
      final File[] subfrom = from.listFiles();
      for (final File element : subfrom) {
        if (element.isFile()) {
          try {
            to.add(copyToDir(element, directoryTo, move));
          } catch (final Reply550Exception e) {
            throw e;
          }
        } else {
          final File newTo = new File(directoryTo, element.getName());
          newTo.mkdirs();
          final File[] copied = copyRecursive(element, newTo, move);
          for (final File file : copied) {
            to.add(file);
          }
        }
      }
    }
    return to.toArray(new File[0]);
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
  public static File[] copy(File[] from, File directoryTo, boolean move)
      throws Reply550Exception {
    if (from == null || directoryTo == null) {
      return null;
    }
    File[] to = null;
    if (createDir(directoryTo)) {
      to = new File[from.length];
      for (int i = 0; i < from.length; i++) {
        if (from[i].isFile()) {
          try {
            to[i] = copyToDir(from[i], directoryTo, move);
          } catch (final Reply550Exception e) {
            throw e;
          }
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
  public static final boolean createDir(File directory) {
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
  private static File copyToDir(File from, File directoryTo, boolean move)
      throws Reply550Exception {
    if (from == null || directoryTo == null) {
      throw new Reply550Exception("Source or Destination is null");
    }
    if (!from.isFile()) {
      throw new Reply550Exception("Source file is a directory");
    }
    if (createDir(directoryTo)) {
      final File to = new File(directoryTo, from.getName());
      if (move && from.renameTo(to)) {
        return to;
      }
      final FileChannel fileChannelIn = getFileChannel(from, false, false);
      if (fileChannelIn == null) {
        throw new Reply550Exception("Cannot read source file");
      }
      final FileChannel fileChannelOut = getFileChannel(to, true, false);
      if (fileChannelOut == null) {
        try {
          fileChannelIn.close();
        } catch (final IOException e) {
        }
        throw new Reply550Exception("Cannot write destination file");
      }
      if (write(fileChannelIn, fileChannelOut) > 0) {
        if (move) {
          // do not test the delete
          from.delete();
        }
        return to;
      }
      throw new Reply550Exception("Cannot write destination file");
    }
    throw new Reply550Exception("Cannot access to parent dir of destination");
  }

  /**
   * Returns the FileChannel in Out MODE (if isOut is True) or in In MODE (if
   * isOut is False) associated with
   * the file. In out MODE, it can be in append MODE.
   *
   * @param isOut
   * @param append
   *
   * @return the FileChannel (OUT or IN)
   *
   * @throws Reply550Exception
   */
  private static FileChannel getFileChannel(File file, boolean isOut,
                                            boolean append)
      throws Reply550Exception {
    FileChannel fileChannel = null;
    try {
      if (isOut) {
        @SuppressWarnings("resource")
        final FileOutputStream fileOutputStream =
            new FileOutputStream(file.getPath(), append);
        fileChannel = fileOutputStream.getChannel();
        if (append) {
          // Bug in JVM since it does not respect the API (position
          // should be set as length)
          try {
            fileChannel.position(file.length());
          } catch (final IOException e) {
          }
        }
      } else {
        if (!file.exists()) {
          throw new Reply550Exception("File does not exist");
        }
        @SuppressWarnings("resource")
        final FileInputStream fileInputStream =
            new FileInputStream(file.getPath());
        fileChannel = fileInputStream.getChannel();
      }
    } catch (final FileNotFoundException e) {
      throw new Reply550Exception("File not found", e);
    }
    return fileChannel;
  }

  /**
   * Write one fileChannel to another one. Close the fileChannels
   *
   * @param fileChannelIn source of file
   * @param fileChannelOut destination of file
   *
   * @return The size of copy if is OK
   *
   * @throws Reply550Exception
   */
  private static long write(FileChannel fileChannelIn,
                            FileChannel fileChannelOut)
      throws Reply550Exception {
    if (fileChannelIn == null) {
      if (fileChannelOut != null) {
        try {
          fileChannelOut.close();
        } catch (final IOException e) {
        }
      }
      throw new Reply550Exception("FileChannelIn is null");
    }
    if (fileChannelOut == null) {
      try {
        fileChannelIn.close();
      } catch (final IOException e) {
      }
      throw new Reply550Exception("FileChannelOut is null");
    }
    long size = 0;
    long transfert = 0;
    try {
      transfert = fileChannelOut.position();
      size = fileChannelIn.size();
      long chunkSize = size;
      while (transfert < size) {
        chunkSize = size - transfert;
        transfert +=
            fileChannelOut.transferFrom(fileChannelIn, transfert, chunkSize);
      }
    } catch (final IOException e) {
      try {
        fileChannelOut.close();
        fileChannelIn.close();
      } catch (final IOException e1) {
      }
      throw new Reply550Exception("An error during copy occurs", e);
    }
    try {
      fileChannelOut.close();
      fileChannelIn.close();
    } catch (final IOException e) {// Close error can be ignored
    }
    final boolean retour = size == transfert;
    if (!retour) {
      throw new Reply550Exception(
          "Copy is not complete: " + transfert + " bytes instead of " + size +
          " original bytes");
    }
    return size;
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
  public static void copy(File from, File to, boolean move, boolean append)
      throws Reply550Exception {
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
      final FileChannel fileChannelIn = getFileChannel(from, false, false);
      if (fileChannelIn == null) {
        throw new Reply550Exception("Cannot read source file");
        // return false;
      }
      final FileChannel fileChannelOut = getFileChannel(to, true, append);
      if (fileChannelOut == null) {
        try {
          fileChannelIn.close();
        } catch (final IOException e) {
        }
        throw new Reply550Exception("Cannot write destination file");
      }
      if (write(fileChannelIn, fileChannelOut) > -1) {
        if (move) {
          // do not test the delete
          from.delete();
        }
        return;
      }
      throw new Reply550Exception("Cannot copy");
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
  private static boolean deleteRecursiveFileDir(File dir) {
    if (dir == null) {
      return true;
    }
    boolean retour = true;
    if (!dir.exists()) {
      return true;
    }
    File[] list = dir.listFiles();
    if (list == null || list.length == 0) {
      list = null;
      return dir.delete();
    }
    final int len = list.length;
    for (int i = 0; i < len; i++) {
      if (list[i].isDirectory()) {
        if (!deleteRecursiveFileDir(list[i])) {
          retour = false;
        }
      } else {
        retour = false;
        list = null;
        return retour;
      }
    }
    list = null;
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
  private static boolean forceDeleteRecursiveSubDir(File directory) {
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
    File[] list = directory.listFiles();
    if (list == null || list.length == 0) {
      list = null;
      return true;
    }
    final int len = list.length;
    for (int i = 0; i < len; i++) {
      if (list[i].isDirectory()) {
        if (!forceDeleteRecursiveSubDir(list[i])) {
          retour = false;
        }
      } else {
        if (!list[i].delete()) {
          retour = false;
        }
      }
    }
    list = null;
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
  public static boolean forceDeleteRecursiveDir(File directory) {
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
    File[] list = directory.listFiles();
    if (list == null || list.length == 0) {
      list = null;
      return true;
    }
    final int len = list.length;
    for (int i = 0; i < len; i++) {
      if (list[i].isDirectory()) {
        if (!forceDeleteRecursiveSubDir(list[i])) {
          retour = false;
        }
      } else {
        if (!list[i].delete()) {
          retour = false;
        }
      }
    }
    list = null;
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
  public static boolean deleteRecursiveDir(File directory) {
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
    File[] list = directory.listFiles();
    if (list == null || list.length == 0) {
      list = null;
      retour = directory.delete();
      return retour;
    }
    final int len = list.length;
    for (int i = 0; i < len; i++) {
      if (list[i].isDirectory()) {
        if (!deleteRecursiveFileDir(list[i])) {
          retour = false;
        }
      } else {
        retour = false;
      }
    }
    list = null;
    if (retour) {
      retour = directory.delete();
    }
    return retour;
  }

  /**
   * @param _FileName
   * @param _Path
   *
   * @return true if the file exist in the specified path
   */
  public static final boolean fileExist(String _FileName, String _Path) {
    boolean exist = false;
    final String fileString = _Path + File.separator + _FileName;
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
  public static final File[] getFiles(File directory) {
    if (directory == null || !directory.isDirectory()) {
      return null;
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
  public static void computeGlobalHash(FilesystemBasedDigest digest, File file,
                                       int length) {
    if (digest == null) {
      return;
    }
    final byte[] bytes = new byte[65536];
    int still = length;
    int len = still > 65536? 65536 : still;
    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(file);
      while (inputStream.read(bytes, 0, len) > 0) {
        digest.Update(bytes, 0, len);
        still -= length;
        if (still <= 0) {
          break;
        }
        len = still > 65536? 65536 : still;
      }
    } catch (final FileNotFoundException e) {
      // error
    } catch (final IOException e) {
      // error
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (final IOException e) {
        }
      }
    }
  }

}
