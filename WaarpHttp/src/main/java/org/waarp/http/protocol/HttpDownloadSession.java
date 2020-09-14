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

import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.http.protocol.servlet.HttpAuthent;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66BusinessInterface;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.waarp.openr66.context.R66FiniteDualStates.*;

/**
 * Http Resumable session
 */
public class HttpDownloadSession extends HttpSessionAbstract {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpDownloadSession.class);
  protected static final String HASH = "HASH";
  private final String filename;
  private final String rulename;
  private final String comment;
  private final String identifier;
  private final String finalFilename;
  private final long filesize;
  private final DbTaskRunner runner;

  /**
   * Constructor for an Http Download Session
   *
   * @param filename
   * @param rulename
   * @param identifier
   * @param comment
   * @param authent already initialized
   *
   * @throws IllegalArgumentException if something wrong happens
   */
  public HttpDownloadSession(final String filename, final String rulename,
                             final String identifier, final String comment,
                             final HttpAuthent authent)
      throws IllegalArgumentException {
    super(authent);
    this.filename = filename;
    this.rulename = rulename;
    this.comment = comment;
    this.identifier = identifier;
    if (!DetectionUtils.isJunit()) {
      final R66BusinessInterface business =
          checkAuthentR66Business(this, session, authent);
      runner =
          getDbTaskRunner(authent.getUserId(), rulename, identifier, comment,
                          business);
      final File file = session.getFile().getTrueFile();
      this.finalFilename = identifier + "_" + file.getName();
      filesize = file.length();
      logger.info("START: {}", this);
      preTasks(business, runner);
    } else {
      runner = null;
      this.finalFilename = "NOID" + "_" + filename;
      this.filesize = 0;
    }
  }

  /**
   * Constructor for reading from database only
   *
   * @param identifier
   * @param authent
   */
  public HttpDownloadSession(final String identifier, final HttpAuthent authent)
      throws WaarpDatabaseException {
    super(authent);
    this.filename = "nofile";
    this.rulename = "norule";
    this.comment = "nocomment";
    this.identifier = identifier;
    if (!DetectionUtils.isJunit()) {
      final R66BusinessInterface business =
          checkAuthentR66Business(this, session, authent);

      runner = getDbTaskRunner(authent.getUserId(), identifier);
      this.finalFilename = identifier + "_" + filename;
    } else {
      runner = null;
      this.finalFilename = "NOID" + "_" + filename;
    }
    this.filesize = 0;
  }

  public boolean isFinished() {
    return runner != null && runner.isFinished();
  }

  public boolean isTransmitting() {
    return runner != null &&
           runner.getUpdatedInfo().equals(UpdatedInfo.RUNNING) &&
           this.filesize == 0;
  }

  /**
   * Initialize the DbTaskRunner for this user and rulename
   *
   * @param user
   * @param rulename
   * @param identifier
   * @param comment
   * @param business
   *
   * @return the DbTaskRunner, potentially new
   *
   * @throws IllegalArgumentException
   */
  private DbTaskRunner getDbTaskRunner(final String user, final String rulename,
                                       final String identifier,
                                       final String comment,
                                       final R66BusinessInterface business)
      throws IllegalArgumentException {
    final long specialId = Long.parseLong(identifier);
    return getDbTaskRunner(user, filename, rulename, specialId, comment,
                           Configuration.configuration.getBlockSize(), business,
                           false);
  }

  /**
   * Reload the DbTaskRunner for this user and rulename
   *
   * @param user
   * @param identifier
   *
   * @return the DbTaskRunner, potentially new
   *
   * @throws IllegalArgumentException
   */
  private DbTaskRunner getDbTaskRunner(final String user,
                                       final String identifier)
      throws IllegalArgumentException, WaarpDatabaseException {
    final long specialId = Long.parseLong(identifier);
    final String requested = Configuration.configuration.getHostId();
    DbTaskRunner runner = null;

    // Try to reload it
    try {
      logger.debug("{} {} <-> {}", specialId, user, requested);
      runner = new DbTaskRunner(specialId, user, requested);
    } catch (final WaarpDatabaseException e) {
      logger.debug("{} {} {}", specialId, user, requested);
      runner = new DbTaskRunner(specialId, requested, user);
    }
    runner.setSender(true);
    try {
      session.setRunner(runner);
    } catch (final OpenR66RunnerErrorException e) {
      logger.debug(e);
    }
    session.setReady(true);
    return runner;
  }

  /**
   * @return the final name for download
   */
  public String getFinalName() {
    return finalFilename;
  }

  /**
   * @return the original file size
   */
  public long getFileSize() {
    return filesize;
  }

  /**
   * @return the identifier
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * @return the corresponding Hash (SHA 256) or null if not possible
   */
  public String getHash() {
    final File file = session.getFile().getTrueFile();
    final byte[] bin;
    try {
      bin = FilesystemBasedDigest.getHash(file, false, DigestAlgo.SHA256);
    } catch (final IOException e) {
      logger.warn(e);
      return null;
    }
    final String hash = FilesystemBasedDigest.getHex(bin);
    session.getRunner().addToTransferMap(HASH, hash);
    try {
      session.getRunner().saveStatus();
    } catch (final OpenR66RunnerErrorException e) {
      logger.debug(e);
    }
    return hash;
  }

  /**
   * @return the previously hash if computed, else null
   */
  public String getComputedHadh() {
    final String hash = (String) session.getRunner().getFromTransferMap(HASH);
    logger.debug("Found {}", hash);
    return hash;
  }

  @Override
  public void error(final Exception e, final R66BusinessInterface business)
      throws IllegalArgumentException {
    logger.error(e);
    if (business != null) {
      business.checkAtError(session);
    }
    session.newState(R66FiniteDualStates.ERROR);
    throw new IllegalArgumentException(e);
  }

  /**
   * Try to read and return all bytes from file
   *
   * @param stream
   *
   * @return true if OK
   *
   * @throws IOException
   */
  public boolean tryWrite(final OutputStream stream) throws IOException {
    if (!session.isAuthenticated() || !session.isReady()) {
      logger.error("Not authenticated or not Ready");
      return false;
    }
    write(stream);
    return true;
  }

  /**
   * Real write from the final file
   *
   * @param stream
   *
   * @throws IOException
   */
  private void write(final OutputStream stream) throws IOException {
    startTransfer(runner);
    final File file = session.getFile().getTrueFile();
    final FileInputStream inputStream = new FileInputStream(file);
    try {
      final byte[] bytes = new byte[Configuration.configuration.getBlockSize()];
      while (true) {
        final int r = inputStream.read(bytes);
        if (r < 0) {
          break;
        }
        stream.write(bytes, 0, r);
        session.getRunner().incrementRank();
      }
    } catch (final IOException e) {
      session.getRunner().setErrorExecutionStatus(ErrorCode.TransferError);
      session.getRunner().setErrorTask();
      try {
        session.getRunner().run();
      } catch (final OpenR66RunnerErrorException ex) {
        logger.info(e);
      }
      session.newState(R66FiniteDualStates.ERROR);
      throw e;
    } finally {
      inputStream.close();
      stream.flush();
      stream.close();
    }

  }

  /**
   * Finalize the current download
   */
  public void downloadFinished() {
    if (session.getState() == CLOSEDCHANNEL) {
      return;
    }
    logger.debug("Final block received! {}", session);
    runPostTask();
    authent.finalizeTransfer(this, session);
  }

  @Override
  public String toString() {
    return "DS: {" + session.toString() + ", " + filename + ", " + rulename +
           ", " + identifier + ", " + comment + ", " + finalFilename + "}";
  }
}
