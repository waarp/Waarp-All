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
package org.waarp.ftp.filesystembased;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.exception.FileEndOfTransferException;
import org.waarp.common.exception.FileTransferException;
import org.waarp.common.file.DataBlock;
import org.waarp.common.file.filesystembased.FilesystemBasedFileImpl;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.ftp.core.exception.FtpNoConnectionException;
import org.waarp.ftp.core.file.FtpFile;
import org.waarp.ftp.core.session.FtpSession;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Filesystem implementation of a FtpFile
 */
public abstract class FilesystemBasedFtpFile extends FilesystemBasedFileImpl
    implements FtpFile {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FilesystemBasedFtpFile.class);

  /**
   * Retrieve lock to ensure only one call at a time for one file
   */
  private final ReentrantLock retrieveLock = new ReentrantLock();

  /**
   * @param session
   * @param dir It is not necessary the directory that owns this file.
   * @param path
   * @param append
   *
   * @throws CommandAbstractException
   */
  protected FilesystemBasedFtpFile(FtpSession session,
                                   FilesystemBasedFtpDir dir, String path,
                                   boolean append)
      throws CommandAbstractException {
    super(session, dir, path, append);
  }

  @Override
  public long length() throws CommandAbstractException {
    long length = super.length();
    if (((FtpSession) getSession()).getDataConn()
                                   .isFileStreamBlockAsciiImage()) {
      final long block = (long) Math
          .ceil((double) length / (double) getSession().getBlockSize());
      length += (block + 3) * 3;
    }
    return length;
  }

  /**
   * Launch retrieve operation (internal method, should not be called
   * directly)
   */
  @Override
  public void trueRetrieve() {
    retrieveLock.lock();
    try {
      if (!isReady) {
        return;
      }
      // First check if ready to run from Control
      try {
        ((FtpSession) session).getDataConn().getFtpTransferControl()
                              .waitForDataNetworkHandlerReady();
      } catch (final InterruptedException e) {
        // bad thing
        logger.warn("DataNetworkHandler was not ready", e);
        return;
      }
      Channel channel;
      try {
        channel = ((FtpSession) session).getDataConn().getCurrentDataChannel();
      } catch (final FtpNoConnectionException e) {
        if (isInReading()) {
          logger.error("Should not be", e);
          ((FtpSession) session).getDataConn().getFtpTransferControl()
                                .setTransferAbortedFromInternal(true);
        }
        logger.debug(
            "Possible call while channel was on going to be closed once transfer was done",
            e);
        closeFile();
        ((FtpSession) session).getDataConn().getFtpTransferControl()
                              .setPreEndOfTransfer();
        return;
      }
      DataBlock block;
      try {
        block = readDataBlock();
      } catch (final FileEndOfTransferException e) {
        // Last block (in fact, previous block was the last one,
        // but it could be aligned with the block size so not
        // detected)
        closeFile();
        ((FtpSession) session).getDataConn().getFtpTransferControl()
                              .setPreEndOfTransfer();
        return;
      }
      if (block == null) {
        // Last block (in fact, previous block was the last one,
        // but it could be aligned with the block size so not
        // detected)
        closeFile();
        ((FtpSession) session).getDataConn().getFtpTransferControl()
                              .setPreEndOfTransfer();
        return;
      }
      // While not last block
      ChannelFuture future = null;
      while (block != null && !block.isEOF()) {
        future = channel.writeAndFlush(block);
        WaarpNettyUtil.awaitOrInterrupted(future);
        if (!future.isSuccess()) {
          closeFile();
          throw new FileTransferException("File transfer in error");
        }
        try {
          block = readDataBlock();
        } catch (final FileEndOfTransferException e) {
          closeFile();
          // Wait for last write
          if (future.isSuccess()) {
            ((FtpSession) session).getDataConn().getFtpTransferControl()
                                  .setPreEndOfTransfer();
          } else {
            throw new FileTransferException("File transfer in error");
          }
          return;
        }
      }
      // Last block
      closeFile();
      if (block != null) {
        logger.debug("Write " + block.getByteCount());
        future = channel.writeAndFlush(block);
      }
      // Wait for last write
      if (future != null) {
        WaarpNettyUtil.awaitOrInterrupted(future);
        if (future.isSuccess()) {
          ((FtpSession) session).getDataConn().getFtpTransferControl()
                                .setPreEndOfTransfer();
        } else {
          throw new FileTransferException("Write is not successful");
        }
      }
    } catch (final FileTransferException e) {
      // An error occurs!
      ((FtpSession) session).getDataConn().getFtpTransferControl()
                            .setTransferAbortedFromInternal(true);
    } catch (final CommandAbstractException e) {
      logger.error("Should not be", e);
      ((FtpSession) session).getDataConn().getFtpTransferControl()
                            .setTransferAbortedFromInternal(true);
    } finally {
      retrieveLock.unlock();
    }
  }
}
