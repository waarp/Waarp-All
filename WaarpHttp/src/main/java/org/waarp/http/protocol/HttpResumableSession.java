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

package org.waarp.http.protocol;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.lru.ConcurrentUtility;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.http.protocol.servlet.HttpAuthent;
import org.waarp.openr66.context.R66BusinessInterface;
import org.waarp.openr66.context.filesystem.R66Dir;
import org.waarp.openr66.context.filesystem.R66File;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbTaskRunner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Set;
import java.util.UUID;

import static org.waarp.openr66.context.R66FiniteDualStates.*;

/**
 * Http Resumable session
 */
public class HttpResumableSession extends HttpSessionAbstract {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpResumableSession.class);

  private final Set<HttpResumableChunkNumber> uploadedChunks =
      ConcurrentUtility.newConcurrentSet();
  private final HttpResumableInfo httpResumableInfo;
  private long size = 0L;

  /**
   * Constructor for an Http Resumable Session
   *
   * @param resumableInfo
   * @param rulename
   * @param comment
   * @param authent already initialized
   *
   * @throws IllegalArgumentException if something wrong happens
   */
  HttpResumableSession(final HttpResumableInfo resumableInfo,
                       final String rulename, final String comment,
                       final HttpAuthent authent)
      throws IllegalArgumentException {
    super(authent);
    this.httpResumableInfo = resumableInfo;
    if (!WaarpSystemUtil.isJunit()) {
      final R66BusinessInterface business =
          checkAuthentR66Business(this, session, authent);
      final DbTaskRunner runner =
          getDbTaskRunner(authent.getUserId(), rulename, comment, business);
      preTasks(business, runner);
    }
  }

  /**
   * Adapted method to resumable context
   *
   * @param user
   * @param rulename
   * @param comment
   * @param business
   *
   * @return the DbTaskRunner
   */
  private DbTaskRunner getDbTaskRunner(final String user, final String rulename,
                                       final String comment,
                                       final R66BusinessInterface business) {
    final long uuid = UUID.nameUUIDFromBytes(
        httpResumableInfo.getIdentifier().getBytes(WaarpStringUtils.UTF8))
                          .getMostSignificantBits();
    return getDbTaskRunner(user, httpResumableInfo.getFilename(), rulename,
                           uuid, comment, httpResumableInfo.getChunkSize(),
                           business, true);
  }

  /**
   * @return the current HttpResumableInfo
   */
  public HttpResumableInfo getHttpResumableInfo() {
    return httpResumableInfo;
  }

  /**
   * Try to write the data according to resumbaleInfo
   *
   * @param resumableInfo
   * @param stream
   *
   * @return true if Write is OK
   *
   * @throws IOException
   */
  public boolean tryWrite(final HttpResumableInfo resumableInfo,
                          final InputStream stream) throws IOException {
    if (!session.isAuthenticated() || !session.isReady()) {
      logger.error("Not authenticated or not Ready");
      return false;
    }
    if (!valid(resumableInfo)) {
      return false;
    }
    final HttpResumableChunkNumber chunkNumber =
        new HttpResumableChunkNumber(resumableInfo.getChunkNumber());
    if (uploadedChunks.contains(chunkNumber)) {
      return false;
    }
    write(resumableInfo, stream);
    uploadedChunks.add(chunkNumber);
    session.getRunner().incrementRank();
    return true;
  }

  /**
   * Check if the resumableInfo is valid compared to current session
   *
   * @param resumableInfo
   *
   * @return true if ok
   */
  public boolean valid(final HttpResumableInfo resumableInfo) {
    return (resumableInfo.getChunkSize() > 0 &&
            resumableInfo.getTotalSize() > 0 &&
            resumableInfo.getChunkNumber() > 0 &&
            ParametersChecker.isNotEmpty(resumableInfo.getIdentifier()) &&
            ParametersChecker.isNotEmpty(resumableInfo.getFilename()) &&
            ParametersChecker.isNotEmpty(resumableInfo.getRelativePath()) &&
            httpResumableInfo.isCompatible(resumableInfo));
  }

  /**
   * Real write to the final file
   *
   * @param info
   * @param stream
   *
   * @throws IOException
   */
  private void write(final HttpResumableInfo info, final InputStream stream)
      throws IOException {
    final File file = session.getFile().getTrueFile();
    RandomAccessFile raf = null;
    try {
      raf = new RandomAccessFile(file, "rw");
      //Seek to position
      raf.seek((info.getChunkNumber() - 1) * (long) info.getChunkSize());
      final byte[] bytes = new byte[info.getChunkSize()];
      while (true) {
        final int r = stream.read(bytes);
        if (r < 0) {
          break;
        }
        raf.write(bytes, 0, r);
        size += r;
      }
    } finally {
      if (raf != null) {
        raf.close();
      }
      stream.close();
    }

  }

  /**
   * Check if the resumableInfo block is already written (previously received)
   *
   * @param resumableInfo
   *
   * @return True if contained
   */
  public boolean contains(final HttpResumableInfo resumableInfo) {
    final HttpResumableChunkNumber chunkNumber =
        new HttpResumableChunkNumber(resumableInfo.getChunkNumber());
    return uploadedChunks.contains(chunkNumber);
  }

  /**
   * Check if the current upload is finished or not
   *
   * @param sha256 if not empty, contains the sha256 in hex format
   *
   * @return True if finished
   */
  public boolean checkIfUploadFinished(final String sha256) {
    logger.debug("Write until now: {} for {}", size,
                 httpResumableInfo.getTotalSize());
    //check if upload finished
    if (size != httpResumableInfo.getTotalSize()) {
      return false;
    }
    if (session.getState() == CLOSEDCHANNEL) {
      return true;
    }
    logger.debug("Final block received! {}", session);
    session.newState(ENDTRANSFERS);
    //Upload finished, change filename.
    final R66File r66File = session.getFile();
    final File file = r66File.getTrueFile();
    if (file.isFile()) {
      // Now if sha256 is given, compute it and compare
      if (ParametersChecker.isNotEmpty(sha256)) {
        try {
          final byte[] bin =
              FilesystemBasedDigest.getHash(file, false, DigestAlgo.SHA256);
          if (!FilesystemBasedDigest.digestEquals(sha256, bin)) {
            logger.error("Digests differs: {} {}", sha256,
                         FilesystemBasedDigest.getHex(bin));
            error(new OpenR66RunnerErrorException("Digest differs"),
                  session.getBusinessObject());
          }
          logger.info("Digest OK");
        } catch (final IOException ignore) {
          logger.warn(ignore);
        }
      } else {
        logger.info("NO DIGEST given");
      }
      final DbTaskRunner runner = session.getRunner();
      try {
        final String finalpath = R66Dir.getFinalUniqueFilename(r66File);
        logger.debug("File to move from {} to {}",
                     r66File.getTrueFile().getAbsolutePath(), finalpath);
        if (!r66File.renameTo(runner.getRule().setRecvPath(finalpath))) {
          logger.error("Cannot move file to final position {}",
                       runner.getRule().setRecvPath(finalpath));
        }
        runner.setFilename(r66File.getFile());
        runner.saveStatus();
        runPostTask();
        authent.finalizeTransfer(this, session);
      } catch (final OpenR66RunnerErrorException e) {
        error(e, session.getBusinessObject());
      } catch (final CommandAbstractException e) {
        error(e, session.getBusinessObject());
      }
      return true;
    }
    return true;
  }

  @Override
  public String toString() {
    return "RS: {" + uploadedChunks.toString() + ", " + session.toString() +
           ", " + httpResumableInfo + "}";
  }
}
