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
package org.waarp.openr66.protocol.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.file.AbstractDir;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.filesystem.R66File;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * File Utils
 */
public final class FileUtils {

  private FileUtils() {
  }

  /**
   * Change or create the R66File associated with the context
   *
   * @param logger
   * @param session
   * @param filenameSrc new filename
   * @param isPreStart
   * @param isSender
   * @param isThrough
   * @param file old R66File if any (might be null)
   *
   * @return the R66File
   *
   * @throws OpenR66RunnerErrorException
   */
  public static final R66File getFile(WaarpLogger logger, R66Session session,
                                      String filenameSrc, boolean isPreStart,
                                      boolean isSender, boolean isThrough,
                                      R66File file)
      throws OpenR66RunnerErrorException {
    String filename;
    logger.debug("PreStart: " + isPreStart);
    logger.debug("Dir is: " + session.getDir().getFullPath());
    logger.debug("File is: " + filenameSrc);
    if (isPreStart) {
      filename = AbstractDir.normalizePath(filenameSrc);
      filename = AbstractDir.pathFromURI(filename);
      logger.debug("File becomes: " + filename);
    } else {
      filename = filenameSrc;
    }
    if (isSender) {
      try {
        if (file == null) {
          try {
            file = (R66File) session.getDir().setFile(filename, false);
          } catch (final CommandAbstractException e) {
            logger.warn("File not placed in normal directory", e);
            // file is not under normal base directory, so is external
            // File should already exist but can be using special code ('*?')
            file = session.getDir().setFileNoCheck(filename);
          }
        }
        if (isThrough) {
          // no test on file since it does not really exist
          logger.debug("File is in through mode: {}", file);
        } else if (!file.canRead()) {
          logger
              .debug("File {} cannot be read, so try external from " + filename,
                     file);
          // file is not under normal base directory, so is external
          // File should already exist but cannot use special code ('*?')
          final R66File file2 =
              new R66File(session, session.getDir(), filename);
          if (!file2.canRead()) {
            throw new OpenR66RunnerErrorException(
                "File cannot be read: " + file.getTrueFile().getAbsolutePath());
          }
          file = file2;
        }
      } catch (final CommandAbstractException e) {
        throw new OpenR66RunnerErrorException(e);
      }
    } else {
      // not sender so file is just registered as is but no test of existence
      file = new R66File(session, session.getDir(), filename);
    }
    return file;
  }

  /**
   * @param buffer
   * @param algo for packet only
   * @param digestGlobal
   *
   * @return the hash from the given Buffer
   */
  public static final ByteBuf getHash(ByteBuf buffer, DigestAlgo algo,
                                      FilesystemBasedDigest digestGlobal) {
    byte[] newkey;
    try {
      if (digestGlobal == null) {
        newkey = FilesystemBasedDigest.getHash(buffer, algo);
      } else {
        FilesystemBasedDigest digestPacket = new FilesystemBasedDigest(algo);
        byte[] bytes = digestGlobal.getBytes(buffer);
        int start = digestGlobal.getOffset(buffer);
        int length = buffer.readableBytes();
        digestPacket.Update(bytes, start, length);
        newkey = digestPacket.Final();
        digestGlobal.Update(bytes, start, length);
      }
    } catch (final IOException e) {
      return Unpooled.EMPTY_BUFFER;
    } catch (NoSuchAlgorithmException e) {
      return Unpooled.EMPTY_BUFFER;
    }
    return Unpooled.wrappedBuffer(newkey);
  }

  /**
   * Compute global hash (if possible)
   *
   * @param digest
   * @param buffer
   */
  public static void computeGlobalHash(final FilesystemBasedDigest digest,
                                       final ByteBuf buffer) {
    if (digest == null) {
      return;
    }
    digest.Update(buffer);
  }

  /**
   * Compute global hash and local hash (if possible)
   *
   * @param digestGlobal
   * @param digestLocal
   * @param buffer
   */
  public static void computeGlobalHash(final FilesystemBasedDigest digestGlobal,
                                       final FilesystemBasedDigest digestLocal,
                                       final ByteBuf buffer) {
    if (digestGlobal != null && digestLocal != null) {
      final byte[] bytes = digestGlobal.getBytes(buffer);
      final int length = buffer.readableBytes();
      final int offset = digestGlobal.getOffset(buffer);
      digestGlobal.Update(bytes, offset, length);
      digestLocal.Update(bytes, offset, length);
      return;
    }
    if (digestGlobal != null) {
      digestGlobal.Update(buffer);
    }
    if (digestLocal != null) {
      digestLocal.Update(buffer);
    }
  }

}
