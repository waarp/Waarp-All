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
package org.waarp.common.file.passthrough;

import io.netty.buffer.ByteBuf;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply450Exception;
import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.exception.FileEndOfTransferException;
import org.waarp.common.exception.FileTransferException;
import org.waarp.common.file.AbstractDir;
import org.waarp.common.file.AbstractFile;
import org.waarp.common.file.DataBlock;
import org.waarp.common.file.DirInterface;
import org.waarp.common.file.SessionInterface;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * File implementation for Passthrough Based. It is just an empty shell since in
 * pass through mode, no
 * directories or files really exist.
 * <p>
 * If one wants to implement special actions, he/she just has to extend this
 * class and override the default
 * empty implementation.
 *
 *
 */
public abstract class PassthroughBasedFileImpl extends AbstractFile {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(PassthroughBasedFileImpl.class);

  /**
   * SessionInterface
   */
  protected final SessionInterface session;

  /**
   * DirInterface associated with this file at creation. It is not necessary
   * the
   * directory that owns this file.
   */
  private final PassthroughBasedDirImpl dir;

  /**
   * {@link PassthroughBasedAuthImpl}
   */
  private final PassthroughBasedAuthImpl auth;

  /**
   * Current file if any
   */
  protected String currentFile;

  /**
   * Is this file in append mode
   */
  protected boolean isAppend;

  /**
   * Passthrough object
   */
  protected PassthroughFile pfile;
  /**
   * Factory for PassthroughFile
   */
  protected static PassthroughFileFactory factory;

  /**
   * @param session
   * @param dir It is not necessary the directory that owns this file.
   * @param path
   * @param append
   *
   * @throws CommandAbstractException
   * @throws PassthroughException
   */
  public PassthroughBasedFileImpl(SessionInterface session,
                                  PassthroughBasedDirImpl dir, String path,
                                  boolean append)
      throws CommandAbstractException {
    this.session = session;
    auth = (PassthroughBasedAuthImpl) session.getAuth();
    this.dir = dir;
    currentFile = path;
    isReady = true;
    isAppend = append;
    final File file = getFileFromPath(path);
    try {
      pfile = factory.create(this);
    } catch (final PassthroughException e1) {
      throw new Reply450Exception(e1.getMessage());
    }
    if (append) {
      try {
        setPosition(file.length());
      } catch (final IOException e) {
        logger.error("Error during position:", e);
      }
    } else {
      try {
        setPosition(0);
      } catch (final IOException e) {
      }
    }
  }

  @Override
  public void clear() throws CommandAbstractException {
    super.clear();
    currentFile = null;
    isAppend = false;
  }

  @Override
  public SessionInterface getSession() {
    return session;
  }

  @Override
  public DirInterface getDir() {
    return dir;
  }

  /**
   * Get the File from this path, checking first its validity
   *
   * @param path
   *
   * @return the FileInterface
   *
   * @throws CommandAbstractException
   */
  protected File getFileFromPath(String path) throws CommandAbstractException {
    final String newdir = getDir().validatePath(path);
    if (dir.isAbsolute(newdir)) {
      return new File(newdir);
    }
    final String truedir = auth.getAbsolutePath(newdir);
    return new File(truedir);
  }

  /**
   * Get the relative path (without mount point)
   *
   * @param file
   *
   * @return the relative path
   */
  protected String getRelativePath(File file) {
    return auth
        .getRelativePath(AbstractDir.normalizePath(file.getAbsolutePath()));
  }

  @Override
  public boolean isDirectory() throws CommandAbstractException {
    checkIdentify();
    return pfile.isDirectory();
  }

  @Override
  public boolean isFile() throws CommandAbstractException {
    checkIdentify();
    return pfile.isFile();
  }

  @Override
  public String getFile() throws CommandAbstractException {
    checkIdentify();
    return currentFile;
  }

  @Override
  public boolean closeFile() throws CommandAbstractException {
    try {
      pfile.close();
    } catch (final PassthroughException e) {
      throw new Reply450Exception(e.getMessage());
    }
    position = 0;
    isReady = false;
    // Do not clear the filename itself
    return true;
  }

  @Override
  public boolean abortFile() throws CommandAbstractException {
    if (isInWriting() && ((PassthroughBasedFileParameterImpl) getSession()
        .getFileParameter()).deleteOnAbort) {
      delete();
    }
    closeFile();
    return true;
  }

  @Override
  public long length() throws CommandAbstractException {
    checkIdentify();
    if (!isReady) {
      return -1;
    }
    if (!exists()) {
      return -1;
    }
    return pfile.length();
  }

  @Override
  public boolean isInReading() throws CommandAbstractException {
    if (!isReady) {
      return false;
    }
    return pfile.isInReading();
  }

  @Override
  public boolean isInWriting() throws CommandAbstractException {
    if (!isReady) {
      return false;
    }
    return pfile.isInWriting();
  }

  @Override
  public boolean canRead() throws CommandAbstractException {
    checkIdentify();
    if (!isReady) {
      return false;
    }
    return pfile.canRead();
  }

  @Override
  public boolean canWrite() throws CommandAbstractException {
    checkIdentify();
    if (!isReady) {
      return false;
    }
    return pfile.canWrite();
  }

  @Override
  public boolean exists() throws CommandAbstractException {
    checkIdentify();
    if (!isReady) {
      return false;
    }
    return pfile.exists();
  }

  @Override
  public boolean delete() throws CommandAbstractException {
    checkIdentify();
    if (!isReady) {
      return false;
    }
    if (!exists()) {
      return true;
    }
    closeFile();
    try {
      return pfile.delete();
    } catch (final PassthroughException e) {
      throw new Reply550Exception(e.getMessage());
    }
  }

  @Override
  public boolean renameTo(String path) throws CommandAbstractException {
    checkIdentify();
    if (!isReady) {
      return false;
    }
    try {
      return pfile.renameTo(path);
    } catch (final PassthroughException e) {
      throw new Reply550Exception(e.getMessage());
    }
  }

  @Override
  public DataBlock readDataBlock()
      throws FileTransferException, FileEndOfTransferException {
    if (isReady) {
      final DataBlock dataBlock = new DataBlock();
      ByteBuf buffer = null;
      buffer = getBlock(getSession().getBlockSize());
      if (buffer != null) {
        dataBlock.setBlock(buffer);
        if (dataBlock.getByteCount() < getSession().getBlockSize()) {
          dataBlock.setEOF(true);
        }
        return dataBlock;
      }
    }
    throw new FileTransferException("No file is ready");
  }

  @Override
  public void writeDataBlock(DataBlock dataBlock) throws FileTransferException {
    if (isReady) {
      if (dataBlock.isEOF()) {
        writeBlockEnd(dataBlock.getBlock());
        return;
      }
      writeBlock(dataBlock.getBlock());
      return;
    }
    throw new FileTransferException("No file is ready");
  }

  /**
   * Valid Position of this file
   */
  private long position;

  /**
   * Return the current position in the FileInterface. In write mode, it is
   * the
   * current file length.
   *
   * @return the position
   */
  public long getPosition() {
    return position;
  }

  /**
   * Change the position in the file.
   *
   * @param position the position to set
   *
   * @throws IOException
   */
  @Override
  public void setPosition(long position) throws IOException {
    try {
      pfile.position(position);
    } catch (final PassthroughException e) {
      throw new IOException(e);
    }
    this.position = position;
  }

  /**
   * Try to flush written data if possible
   */
  public void flush() {
    if (isReady) {
      try {
        pfile.flush();
      } catch (final PassthroughException e) {
      }
    }
  }

  /**
   * Write the current FileInterface with the given ByteBuf. The file is not
   * limited to 2^32 bytes since this
   * write operation is in add mode.
   * <p>
   * In case of error, the current already written blocks are maintained and
   * the
   * position is not changed.
   *
   * @param buffer added to the file
   *
   * @throws FileTransferException
   */
  private void writeBlock(ByteBuf buffer) throws FileTransferException {
    if (!isReady) {
      throw new FileTransferException("No file is ready");
    }
    // An empty buffer is allowed
    if (buffer == null) {
      return;// could do FileEndOfTransfer ?
    }
    final int bufferSize = buffer.readableBytes();
    int size;
    try {
      size = pfile.write(buffer);
    } catch (final PassthroughException e) {
      throw new FileTransferException("Cannot write to file");
    }
    final boolean result = size == bufferSize;
    if (!result) {
      try {
        pfile.close();
      } catch (final PassthroughException e) {
      }
      // NO this.realFile.delete(); NO DELETE SINCE BY BLOCK IT CAN BE
      // REDO
      throw new FileTransferException("Internal error, file is not ready");
    }
    position += size;
  }

  /**
   * End the Write of the current FileInterface with the given ByteBuf. The
   * file
   * is not limited to 2^32 bytes
   * since this write operation is in add mode.
   *
   * @param buffer added to the file
   *
   * @throws FileTransferException
   */
  private void writeBlockEnd(ByteBuf buffer) throws FileTransferException {
    writeBlock(buffer);
    try {
      closeFile();
    } catch (final CommandAbstractException e) {
    }
  }

  /**
   * Get the current block ByteBuf of the current FileInterface. There is
   * therefore no limitation of the file
   * size to 2^32 bytes.
   * <p>
   * The returned block is limited to sizeblock. If the returned block is less
   * than sizeblock length, it is the
   * last block to read.
   *
   * @param sizeblock is the limit size for the block array
   *
   * @return the resulting block ByteBuf (even empty)
   *
   * @throws FileTransferException
   * @throws FileEndOfTransferException
   */
  private ByteBuf getBlock(int sizeblock)
      throws FileTransferException, FileEndOfTransferException {
    if (!isReady) {
      throw new FileTransferException("No file is ready");
    }
    ByteBuf buffer;
    try {
      buffer = pfile.read(sizeblock);
    } catch (final PassthroughException e) {
      throw new FileEndOfTransferException("Cannot read the file");
    }
    final int sizeout = buffer.readableBytes();
    if (sizeout < sizeblock) {// last block
      try {
        pfile.close();
      } catch (final PassthroughException e) {
      }
      isReady = false;
    }
    if (sizeout <= 0) {
      throw new FileEndOfTransferException("End of file");
    }
    position += sizeout;
    return buffer;
  }

  /**
   * Write the FileInterface to the fileChannelOut, thus bypassing the
   * limitation of the file size to 2^32
   * bytes.
   * <p>
   * This call closes the fileChannelOut with fileChannelOut.close() if the
   * operation is in success.
   *
   * @param fileChannelOut
   *
   * @return True if OK, False in error.
   */
  protected boolean get(FileChannel fileChannelOut) {
    if (!isReady) {
      return false;
    }
    final long size = pfile.length();
    long transfert;
    try {
      transfert = pfile.transferTo(fileChannelOut);
    } catch (final PassthroughException e) {
      return false;
    }
    if (transfert == size) {
      position += size;
    }
    return transfert == size;
  }
}
