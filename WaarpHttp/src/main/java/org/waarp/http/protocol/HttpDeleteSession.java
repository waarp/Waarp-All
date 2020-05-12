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
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.http.protocol.servlet.HttpAuthent;
import org.waarp.openr66.context.R66BusinessInterface;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.io.File;

/**
 * Http Resumable session
 */
public class HttpDeleteSession extends HttpSessionAbstract {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpDeleteSession.class);
  private final String identifier;
  private final DbTaskRunner runner;
  private final File file;

  /**
   * Constructor for reading from database only
   *
   * @param identifier
   * @param authent
   *
   * @throws OpenR66RunnerErrorException
   */
  public HttpDeleteSession(final String identifier, final HttpAuthent authent)
      throws WaarpDatabaseException, OpenR66RunnerErrorException {
    super(authent);
    this.identifier = identifier;
    if (!DetectionUtils.isJunit()) {
      checkAuthentR66Business(this, session, authent);
      runner = getDbTaskRunner(authent.getUserId(), identifier);
      try {
        session.getDir().changeDirectory(runner.getRule().getRecvPath());
      } catch (CommandAbstractException e) {
        // Nothing: ignore
      }
      String baseDir = runner.getRule().getRecvPath();
      String path = session.getDir().getFullPath();
      String filepath_in = runner.getFilename();
      String finalPath = filepath_in.replace(baseDir, path).replace("//", "/");
      logger.info("From {} to {} using {} gives {}", baseDir, path, filepath_in,
                  finalPath);
      file = new File(finalPath);
    } else {
      runner = null;
      file = null;
    }
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
    long specialId = Long.parseLong(identifier);
    final String requested = Configuration.configuration.getHostId();
    final String requester = user;
    DbTaskRunner runner = null;

    // Try to reload it
    try {
      logger.debug("{} {} <-> {}", specialId, requester, requested);
      runner = new DbTaskRunner(specialId, requester, requested);
    } catch (WaarpDatabaseException e) {
      logger.debug("{} {} {}", specialId, requester, requested);
      runner = new DbTaskRunner(specialId, requested, requester);
    }
    runner.setSender(true);
    try {
      session.setRunner(runner);
    } catch (OpenR66RunnerErrorException e) {
      logger.debug(e);
    }
    session.setReady(true);
    return runner;
  }

  /**
   * @return the File
   */
  public File getFile() {
    return file;
  }

  /**
   * @return the identifier
   */
  public String getIdentifier() {
    return identifier;
  }

  @Override
  public void error(Exception e, R66BusinessInterface business)
      throws IllegalArgumentException {
    logger.error(e);
    if (business != null) {
      business.checkAtError(session);
    }
    session.newState(R66FiniteDualStates.ERROR);
    throw new IllegalArgumentException(e);
  }

  @Override
  public String toString() {
    return "DS: {" + session.toString() + ", " + identifier + ", " +
           file.getAbsolutePath() + "}";
  }
}
