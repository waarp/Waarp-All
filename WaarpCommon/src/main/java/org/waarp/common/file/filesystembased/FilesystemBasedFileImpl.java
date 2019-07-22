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
package org.waarp.common.file.filesystembased;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.waarp.common.command.exception.CommandAbstractException;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;

/**
 * File implementation for Filesystem Based
 *
 *
 */
public abstract class FilesystemBasedFileImpl extends AbstractFile {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FilesystemBasedFileImpl.class);

  /**
   * SessionInterface
   */
  protected final SessionInterface session;

  /**
   * DirInterface associated with this file at creation. It is not necessary
   * the
   * directory that owns this file.
   */
  private final FilesystemBasedDirImpl dir;

  /**
   * {@link FilesystemBasedAuthImpl}
   */
  private final FilesystemBasedAuthImpl auth;

  /**
   * Current file if any
   */
  protected String currentFile;

  /**
   * Is this file in append mode
   */
  protected boolean isAppend;

  /**
   * @param session
   * @param dir It is not necessary the directory that owns this file.
   * @param path
   * @param append
   *
   * @throws CommandAbstractException
   */
  public FilesystemBasedFileImpl(SessionInterface session,
                                 FilesystemBasedDirImpl dir, String path,
                                 boolean append)
      throws CommandAbstractException {
    this.session = session;
    auth = (FilesystemBasedAuthImpl) session.getAuth();
    this.dir = dir;
    currentFile = path;
    isAppend = append;
    final File file = getFileFromPath(path);
    if (append) {
      try {
        setPosition(file.length());
      } catch (final IOException e) {
        // not ready
        return;
      }
    } else {
      try {
        setPosition(0);
      } catch (final IOException e) {
      }
    }
    isReady = true;
  }

  /**
   * Special constructor for possibly external file
   *
   * @param session
   * @param dir It is not necessary the directory that owns this file.
   * @param path
   */
  public FilesystemBasedFileImpl(SessionInterface session,
                                 FilesystemBasedDirImpl dir, String path) {
    this.session = session;
    auth = (FilesystemBasedAuthImpl) session.getAuth();
    this.dir = dir;
    currentFile = path;
    isReady = true;
    isAppend = false;
    position = 0;
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
    final File file = new File(truedir);
    logger.debug("Final File: " + truedir + " CanRead: " + file.canRead());
    return file;
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
    final File dir1 = getFileFromPath(currentFile);
    return dir1.isDirectory();
  }

  @Override
  public boolean isFile() throws CommandAbstractException {
    checkIdentify();
    return getFileFromPath(currentFile).isFile();
  }

  @Override
  public String getFile() throws CommandAbstractException {
    checkIdentify();
    return currentFile;
  }

  @Override
  public boolean closeFile() throws CommandAbstractException {
    if (bfileChannelIn != null) {
      try {
        bfileChannelIn.close();
      } catch (final IOException e) {
      }
      bfileChannelIn = null;
      bbyteBuffer = null;
    }
    if (fileOutputStream != null) {
      /*
       * try { rafOut.getFD().sync(); } catch (SyncFailedException e1) { } catch (IOException e1) { }
       */
      try {
        fileOutputStream.flush();
        fileOutputStream.close();
      } catch (final ClosedChannelException e) {
        // ignore
      } catch (final IOException e) {
        throw new Reply550Exception("Close in error");
      }
      fileOutputStream = null;
    }
    position = 0;
    isReady = false;
    // Do not clear the filename itself
    return true;
  }

  @Override
  public boolean abortFile() throws CommandAbstractException {
    if (isInWriting() && ((FilesystemBasedFileParameterImpl) getSession()
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
    return getFileFromPath(currentFile).length();
  }

  @Override
  public boolean isInReading() throws CommandAbstractException {
    if (!isReady) {
      return false;
    }
    return bfileChannelIn != null;
  }

  @Override
  public boolean isInWriting() throws CommandAbstractException {
    if (!isReady) {
      return false;
    }
    return fileOutputStream != null;
  }

  @Override
  public boolean canRead() throws CommandAbstractException {
    checkIdentify();
    if (!isReady) {
      return false;
    }
    return getFileFromPath(currentFile).canRead();
  }

  @Override
  public boolean canWrite() throws CommandAbstractException {
    checkIdentify();
    if (!isReady) {
      return false;
    }
    final File file = getFileFromPath(currentFile);
    if (file.exists()) {
      return file.canWrite();
    }
    return file.getParentFile().canWrite();
  }

  @Override
  public boolean exists() throws CommandAbstractException {
    checkIdentify();
    if (!isReady) {
      return false;
    }
    return getFileFromPath(currentFile).exists();
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
    return getFileFromPath(currentFile).delete();
  }

  @Override
  public boolean renameTo(String path) throws CommandAbstractException {
    checkIdentify();
    if (!isReady) {
      logger.warn("File not ready: {}", this);
      return false;
    }
    final File file = getFileFromPath(currentFile);
    if (file.canRead()) {
      final File newFile = getFileFromPath(path);
      if (newFile.exists()) {
        logger.warn("Target file already exists: " + newFile.getAbsolutePath());
        return false;
      }
      if (newFile.getAbsolutePath().equals(file.getAbsolutePath())) {
        // already in the right position
        isReady = true;
        return true;
      }
      if (newFile.getParentFile().canWrite()) {
        if (!file.renameTo(newFile)) {
          FileOutputStream fileOutputStream = null;
          try {
            try {
              fileOutputStream = new FileOutputStream(newFile);
            } catch (final FileNotFoundException e) {
              logger.warn("Cannot find file: " + newFile.getName(), e);
              return false;
            }
            final FileChannel fileChannelOut = fileOutputStream.getChannel();
            if (get(fileChannelOut)) {
              delete();
            } else {
              try {
                fileChannelOut.close();
              } catch (final IOException e) {
              }
              logger.warn("Cannot write file: {}", newFile);
              return false;
            }
          } finally {
            try {
              if (fileOutputStream != null) {
                fileOutputStream.close();
              }
            } catch (final IOException e) {
            }
          }
        }
        currentFile = getRelativePath(newFile);
        isReady = true;
        logger
            .debug("File renamed to: {} and real position: {}", this, newFile);
        return true;
      } else {
        logger.warn("Cannot write file: {} from {}", newFile, file);
        return false;
      }
    }
    logger.warn("Cannot read file: {}", file);
    return false;
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
    throw new FileTransferException(
        "No file is ready while trying to write: " + dataBlock);
  }

  /**
   * Valid Position of this file
   */
  private long position;

  /**
   * FileOutputStream Out
   */
  private FileOutputStream fileOutputStream;
  /**
   * FileChannel In
   */
  private FileChannel bfileChannelIn;

  /**
   * Associated ByteBuffer
   */
  private ByteBuffer bbyteBuffer;

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
    this.position = position;
    if (bfileChannelIn != null) {
      bfileChannelIn = bfileChannelIn.position(position);
    }
    /*
     * if (rafOut != null) { rafOut.seek(position); }
     */
    if (fileOutputStream != null) {
      fileOutputStream.flush();
      fileOutputStream.close();
      fileOutputStream = getFileOutputStream(true);
      if (fileOutputStream == null) {
        throw new IOException("File cannot changed of Position");
      }
    }
  }

  private byte[] reusableBytes;

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
    if (fileOutputStream == null) {
      // rafOut = getRandomFile();
      fileOutputStream = getFileOutputStream(position > 0);
    }
    if (fileOutputStream == null) {
      throw new FileTransferException("Internal error, file is not ready");
    }
    final int bufferSize = buffer.readableBytes();
    int start = 0;
    byte[] newbuf;
    if (buffer.hasArray()) {
      start = buffer.arrayOffset();
      newbuf = buffer.array();
      buffer.readerIndex(buffer.readerIndex() + bufferSize);
    } else {
      if (reusableBytes == null || reusableBytes.length != bufferSize) {
        reusableBytes = new byte[bufferSize];
      }
      newbuf = reusableBytes;
      buffer.readBytes(newbuf);
    }
    try {
      fileOutputStream.write(newbuf, start, bufferSize);
    } catch (final IOException e2) {
      logger.error("Error during write:", e2);
      try {
        closeFile();
      } catch (final CommandAbstractException e1) {
      }
      // NO this.realFile.delete(); NO DELETE SINCE BY BLOCK IT CAN BE
      // REDO
      throw new FileTransferException("Internal error, file is not ready");
    }
    position += bufferSize;
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
      throw new FileTransferException("Close in error", e);
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
    if (bfileChannelIn == null) {
      bfileChannelIn = getFileChannel();
      if (bfileChannelIn != null) {
        if (bbyteBuffer != null) {
          if (bbyteBuffer.capacity() != sizeblock) {
            bbyteBuffer = null;
            bbyteBuffer = ByteBuffer.allocateDirect(sizeblock);
          }
        } else {
          bbyteBuffer = ByteBuffer.allocateDirect(sizeblock);
        }
      }
    }
    if (bfileChannelIn == null) {
      throw new FileTransferException("Internal error, file is not ready");
    }
    int sizeout = 0;
    while (sizeout < sizeblock) {
      try {
        final int sizeread = bfileChannelIn.read(bbyteBuffer);
        if (sizeread <= 0) {
          break;
        }
        sizeout += sizeread;
      } catch (final IOException e) {
        logger.error("Error during get:", e);
        try {
          closeFile();
        } catch (final CommandAbstractException e1) {
        }
        throw new FileTransferException("Internal error, file is not ready");
      }
    }
    if (sizeout <= 0) {
      try {
        closeFile();
      } catch (final CommandAbstractException e1) {
      }
      isReady = false;
      throw new FileEndOfTransferException("End of file");
    }
    bbyteBuffer.flip();
    position += sizeout;
    final ByteBuf buffer = Unpooled.wrappedBuffer(bbyteBuffer);
    bbyteBuffer.clear();
    if (sizeout < sizeblock) {// last block
      try {
        closeFile();
      } catch (final CommandAbstractException e1) {
      }
      isReady = false;
    }
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
    FileChannel fileChannelIn = getFileChannel();
    if (fileChannelIn == null) {
      return false;
    }
    long size = 0;
    long transfert = 0;
    try {
      size = fileChannelIn.size();
      if (size < 0) {
        try {
          size = length();
        } catch (final CommandAbstractException e) {
          logger.error("Error during get:", e);
          return false;
        }
        if (size < 0) {
          logger.error("Error during get, wrong size: " + size);
          return false;
        }
      }
      long chunkSize = size;
      while (transfert < size) {
        chunkSize = size - transfert;
        transfert +=
            fileChannelOut.transferFrom(fileChannelIn, transfert, chunkSize);
      }
      fileChannelOut.force(true);
    } catch (final IOException e) {
      logger.error("Error during get:", e);
      return false;
    } finally {
      try {
        fileChannelOut.close();
      } catch (final IOException e) {
      }
      try {
        fileChannelIn.close();
      } catch (final IOException e) {
      }
      fileChannelIn = null;
    }
    if (transfert == size) {
      position += size;
    }
    return transfert == size;
  }

  /**
   * Returns the FileChannel in In mode associated with the current file.
   *
   * @return the FileChannel (IN mode)
   */
  protected FileChannel getFileChannel() {
    if (!isReady) {
      return null;
    }
    File trueFile;
    try {
      trueFile = getFileFromPath(currentFile);
    } catch (final CommandAbstractException e1) {
      return null;
    }
    FileChannel fileChannel = null;
    try {
      @SuppressWarnings("resource")
      final FileInputStream fileInputStream = new FileInputStream(trueFile);
      fileChannel = fileInputStream.getChannel();
      if (position != 0) {
        fileChannel = fileChannel.position(position);
      }
    } catch (final FileNotFoundException e) {
      logger.error("File not found in getFileChannel:", e);
      return null;
    } catch (final IOException e) {
      if (fileChannel != null) {
        try {
          fileChannel.close();
        } catch (final IOException e1) {
        }
      }
      logger.error("Change position in getFileChannel:", e);
      return null;
    }
    return fileChannel;
  }

  /**
   * Returns the RandomAccessFile in Out mode associated with the current
   * file.
   *
   * @return the RandomAccessFile (OUT="rw")
   */
  protected RandomAccessFile getRandomFile() {
    if (!isReady) {
      return null;
    }
    File trueFile;
    try {
      trueFile = getFileFromPath(currentFile);
    } catch (final CommandAbstractException e1) {
      return null;
    }
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(trueFile, "rw");
      raf.seek(position);
    } catch (final FileNotFoundException e) {
      logger.error("File not found in getRandomFile:", e);
      return null;
    } catch (final IOException e) {
      logger.error("Change position in getRandomFile:", e);
      return null;
    }
    return raf;
  }

  /**
   * Returns the FileOutputStream in Out mode associated with the current
   * file.
   *
   * @param append True if the FileOutputStream should be in append
   *     mode
   *
   * @return the FileOutputStream (OUT)
   */
  protected FileOutputStream getFileOutputStream(boolean append) {
    if (!isReady) {
      return null;
    }
    File trueFile;
    try {
      trueFile = getFileFromPath(currentFile);
    } catch (final CommandAbstractException e1) {
      return null;
    }
    if (position > 0) {
      if (trueFile.length() < position) {
        logger.error(
            "Cannot Change position in getFileOutputStream: file is smaller than required position");
        return null;
      }
      final RandomAccessFile raf = getRandomFile();
      try {
        raf.setLength(position);
        raf.close();
      } catch (final IOException e) {
        logger.error("Change position in getFileOutputStream:", e);
        return null;
      }
      logger.debug("New size: " + trueFile.length() + " : " + position);
    }
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(trueFile, append);
    } catch (final FileNotFoundException e) {
      logger.error("File not found in getRandomFile:", e);
      return null;
    }
    return fos;
  }
}
