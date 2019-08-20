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

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply530Exception;

import java.util.List;

/**
 * Interface for Directory support
 */
public interface DirInterface {
  /**
   * FileInterface separator for external
   */
  String SEPARATOR = "/";

  /**
   * FileInterface separator for external
   */
  char SEPARATORCHAR = '/';

  /**
   * @return the current value of Options for MLSx
   */
  OptsMLSxInterface getOptsMLSx();

  /**
   * Set empty this FtpDir, mark it unReady.
   */
  void clear();

  /**
   * Init DirInterface after authentication is done
   */
  void initAfterIdentification();

  /**
   * Check if the authentication is correct
   *
   * @throws Reply530Exception
   */
  void checkIdentify() throws Reply530Exception;

  /**
   * @return the FtpSession
   */
  SessionInterface getSession();

  // **************** Directory part **************************

  /**
   * Construct and Check if the given path is valid from business point of
   * view
   * (see {@link AuthInterface})
   *
   * @param path
   *
   * @return the construct and validated path (could be different than the one
   *     given as argument, example: '..'
   *     are removed)
   *
   * @throws CommandAbstractException
   */
  String validatePath(String path) throws CommandAbstractException;

  /**
   * Check if the given path is valid in the sens starting from the current
   * directory
   *
   * @param path
   *
   * @return True if OK
   */
  boolean isPathInCurrentDir(String path);

  /**
   * @return the current PWD
   *
   * @throws CommandAbstractException
   */
  String getPwd() throws CommandAbstractException;

  /**
   * Change directory with the one given as argument
   *
   * @param path
   *
   * @return True if the change is valid
   *
   * @throws CommandAbstractException
   */
  boolean changeDirectory(String path) throws CommandAbstractException;

  /**
   * Change directory with the one given as argument without checking
   * existence
   *
   * @param path
   *
   * @return True if the change is valid
   *
   * @throws CommandAbstractException
   */
  boolean changeDirectoryNotChecked(String path)
      throws CommandAbstractException;

  /**
   * Change for parent directory
   *
   * @return True if the change is valid
   *
   * @throws CommandAbstractException
   */
  boolean changeParentDirectory() throws CommandAbstractException;

  /**
   * Create the directory associated with the String as path
   *
   * @param directory
   *
   * @return the full path of the new created directory
   *
   * @throws CommandAbstractException
   */
  String mkdir(String directory) throws CommandAbstractException;

  /**
   * Delete the directory associated with the String as path
   *
   * @param directory
   *
   * @return the full path of the new deleted directory
   *
   * @throws CommandAbstractException
   */
  String rmdir(String directory) throws CommandAbstractException;

  /**
   * Is the given path a directory and exists
   *
   * @param path
   *
   * @return True if it is a directory and it exists
   *
   * @throws CommandAbstractException
   */
  boolean isDirectory(String path) throws CommandAbstractException;

  /**
   * Is the given path a file and exists
   *
   * @param path
   *
   * @return True if it is a file and it exists
   *
   * @throws CommandAbstractException
   */
  boolean isFile(String path) throws CommandAbstractException;

  /**
   * Return the Modification time for the path
   *
   * @param path
   *
   * @return the Modification time as a String YYYYMMDDHHMMSS.sss
   *
   * @throws CommandAbstractException
   */
  String getModificationTime(String path) throws CommandAbstractException;

  /**
   * List all files from the given path (could be a file or a directory)
   *
   * @param path
   *
   * @return the list of paths
   *
   * @throws CommandAbstractException
   */
  List<String> list(String path) throws CommandAbstractException;

  /**
   * List all files with other informations from the given path (could be a
   * file
   * or a directory)
   *
   * @param path
   * @param lsFormat True if ls Format, else MLSx format
   *
   * @return the list of paths and other informations
   *
   * @throws CommandAbstractException
   */
  List<String> listFull(String path, boolean lsFormat)
      throws CommandAbstractException;

  /**
   * Give for 1 file all informations from the given path (could be a file or
   * a
   * directory)
   *
   * @param path
   * @param lsFormat True if ls Format, else MLSx format
   *
   * @return the path and other informations
   *
   * @throws CommandAbstractException
   */
  String fileFull(String path, boolean lsFormat)
      throws CommandAbstractException;

  /**
   * @return the free space of the current Directory
   *
   * @throws CommandAbstractException
   */
  long getFreeSpace() throws CommandAbstractException;

  // **************** Unique FileInterface part **************************

  /**
   * Create a new File
   *
   * @param path
   * @param append
   *
   * @return the new FileInterface
   *
   * @throws CommandAbstractException
   */
  FileInterface newFile(String path, boolean append)
      throws CommandAbstractException;

  /**
   * Set a path as the current FileInterface
   *
   * @param path
   * @param append True if this file is supposed to be in append mode
   *     (APPE), False in any other cases
   *
   * @return the FileInterface if it is correctly initiate
   *
   * @throws CommandAbstractException
   */
  FileInterface setFile(String path, boolean append)
      throws CommandAbstractException;

  /**
   * Set a new unique path as the current FileInterface from the current
   * Directory (STOU)
   *
   * @return the FileInterface if it is correctly initiate
   *
   * @throws CommandAbstractException
   */
  FileInterface setUniqueFile() throws CommandAbstractException;

  /**
   * @return True if the current FileInterface is ready for reading
   *
   * @throws CommandAbstractException
   */
  boolean canRead() throws CommandAbstractException;

  /**
   * @return True if the current FileInterface is ready for writing
   *
   * @throws CommandAbstractException
   */
  boolean canWrite() throws CommandAbstractException;

  /**
   * @return True if the current FileInterface exists
   *
   * @throws CommandAbstractException
   */
  boolean exists() throws CommandAbstractException;

  /**
   * Get the CRC of the given FileInterface
   *
   * @param path
   *
   * @return the CRC
   *
   * @throws CommandAbstractException
   */
  long getCRC(String path) throws CommandAbstractException;

  /**
   * Get the MD5 of the given FileInterface
   *
   * @param path
   *
   * @return the MD5
   *
   * @throws CommandAbstractException
   */
  byte[] getMD5(String path) throws CommandAbstractException;

  /**
   * Get the SHA-1 of the given FileInterface
   *
   * @param path
   *
   * @return the SHA-1
   *
   * @throws CommandAbstractException
   */
  byte[] getSHA1(String path) throws CommandAbstractException;

  /**
   * Get the Digest of the given FileInterface
   *
   * @param path
   * @param algo algorithm of Digest among CRC32, ADLER32, MD5, MD2,
   *     SHA-1, SHA-256, SHA-384, SHA-512
   *
   * @return the Digest
   *
   * @throws CommandAbstractException
   */
  byte[] getDigest(String path, String algo) throws CommandAbstractException;
}
