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
import org.waarp.common.exception.FileEndOfTransferException;
import org.waarp.common.exception.FileTransferException;

import java.io.IOException;

/**
 * Interface for File support
 */
public interface FileInterface {
  /**
   * Set empty this FtpFile, mark it unReady.
   *
   * @throws CommandAbstractException
   */
  void clear() throws CommandAbstractException;

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
   * @return the FtpDir associated at creation with this file
   */
  DirInterface getDir();

  /**
   * Is the current FileInterface a directory and exists
   *
   * @return True if it is a directory and it exists
   *
   * @throws CommandAbstractException
   */
  boolean isDirectory() throws CommandAbstractException;

  /**
   * Is the current FileInterface a file and exists
   *
   * @return True if it is a file and it exists
   *
   * @throws CommandAbstractException
   */
  boolean isFile() throws CommandAbstractException;

  // **************** Unique FileInterface part **************************

  /**
   * @return the path of the current FileInterface (without mount point if
   *     any)
   *
   * @throws CommandAbstractException
   */
  String getFile() throws CommandAbstractException;

  /**
   * Close the current FileInterface
   *
   * @return True if correctly closed
   *
   * @throws CommandAbstractException
   */
  boolean closeFile() throws CommandAbstractException;

  /**
   * @return the length of the current FileInterface
   *
   * @throws CommandAbstractException
   */
  long length() throws CommandAbstractException;

  /**
   * @return True if the current FileInterface is in Writing process
   *
   * @throws CommandAbstractException
   */
  boolean isInWriting() throws CommandAbstractException;

  /**
   * @return True if the current FileInterface is in Reading process
   *
   * @throws CommandAbstractException
   */
  boolean isInReading() throws CommandAbstractException;

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
   * Try to abort the current transfer if any
   *
   * @return True if everything is ok
   *
   * @throws CommandAbstractException
   */
  boolean abortFile() throws CommandAbstractException;

  /**
   * Ask to store the current FileInterface. This command returns quickly
   * since
   * it does not store really. It
   * prepares the object.
   *
   * @return True if everything is ready
   *
   * @throws CommandAbstractException
   */
  boolean store() throws CommandAbstractException;

  /**
   * Ask to retrieve the current FileInterface. This command returns quickly
   * since it does not retrieve really.
   * It prepares the object.
   *
   * @return True if everything is ready
   *
   * @throws CommandAbstractException
   */
  boolean retrieve() throws CommandAbstractException;

  /**
   * Rename the current FileInterface into a new filename from argument
   *
   * @param path the new filename (path could be relative or absolute
   *     -
   *     without mount point)
   *
   * @return True if the operation is done successfully
   *
   * @throws CommandAbstractException
   */
  boolean renameTo(String path) throws CommandAbstractException;

  /**
   * Restart from a Marker for the current FileInterface if any. This function
   * is to be called at the beginning
   * of every transfer so in store and retrieve method.
   *
   * @param restart
   *
   * @return True if the Marker is OK
   *
   * @throws CommandAbstractException
   */
  boolean restartMarker(Restart restart) throws CommandAbstractException;

  /**
   * Create a restart from context for the current FileInterface
   *
   * @return the dataBlock to send to the client
   *
   * @throws CommandAbstractException
   */
  DataBlock getMarker() throws CommandAbstractException;

  /**
   * Delete the current FileInterface.
   *
   * @return True if OK, else False if not (or if the file never exists).
   *
   * @throws CommandAbstractException
   */
  boolean delete() throws CommandAbstractException;

  /**
   * Change the position in the file.
   *
   * @param position the position to set
   *
   * @throws IOException
   */
  void setPosition(long position) throws IOException;

  /**
   * Function called by the DataNetworkHandler when it receives one DataBlock
   * (Store like command)
   *
   * @param dataBlock
   *
   * @throws FileTransferException
   * @throws FileEndOfTransferException
   */
  void writeDataBlock(DataBlock dataBlock) throws FileTransferException;

  /**
   * Read a new block for FileInterface
   *
   * @return dataBlock
   *
   * @throws FileEndOfTransferException
   * @throws FileTransferException
   */
  DataBlock readDataBlock()
      throws FileEndOfTransferException, FileTransferException;
}
