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
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.http.protocol.servlet.HttpAuthent;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66BusinessInterface;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.database.data.DbTaskRunner.TASKSTEP;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.protocol.configuration.Configuration;

import static org.waarp.openr66.context.R66FiniteDualStates.*;

/**
 * Common part for HttpSession for both download and upload
 */
public class HttpSessionAbstract implements HttpSession {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpSessionAbstract.class);

  protected final R66Session session;
  protected final HttpAuthent authent;

  public HttpSessionAbstract(final HttpAuthent authent) {
    this.session = new R66Session(false);
    this.authent = authent;
  }


  /**
   * Method to check authentication
   *
   * @param httpSession
   * @param session
   * @param authent
   *
   * @return the Business associated with this Authent
   *
   * @throws IllegalArgumentException
   */
  protected R66BusinessInterface checkAuthentR66Business(
      final HttpSession httpSession, final R66Session session,
      final HttpAuthent authent) throws IllegalArgumentException {
    session.setBusinessObject(
        Configuration.configuration.getR66BusinessFactory()
                                   .getBusinessInterface(session));
    final R66BusinessInterface business = session.getBusinessObject();
    session.newState(R66FiniteDualStates.STARTUP);
    try {
      if (business != null) {
        business.checkAtStartup(session);
      }
    } catch (final OpenR66RunnerErrorException e) {
      httpSession.error(e, business);
    }
    authent.checkAuthent(this, session);
    try {
      if (business != null) {
        business.checkAtAuthentication(session);
      }
    } catch (final OpenR66RunnerErrorException e) {
      httpSession.error(e, business);
    }
    session.newState(R66FiniteDualStates.AUTHENTR);
    session.newState(R66FiniteDualStates.AUTHENTD);
    return business;
  }

  /**
   * Initialize the DbTaskRunner for this user and rulename
   *
   * @param user
   * @param filename
   * @param rulename
   * @param identifier
   * @param comment
   * @param chunkSize
   * @param business
   * @param uploadMode
   *
   * @return the DbTaskRunner, potentially new
   *
   * @throws IllegalArgumentException
   */
  protected DbTaskRunner getDbTaskRunner(final String user,
                                         final String filename,
                                         final String rulename,
                                         final long identifier,
                                         final String comment,
                                         final int chunkSize,
                                         final R66BusinessInterface business,
                                         final boolean uploadMode)
      throws IllegalArgumentException {
    session.newState(REQUESTR);
    session.setBlockSize(chunkSize);

    DbRule rule = null;
    try {
      rule = new DbRule(rulename);
    } catch (final WaarpDatabaseException e) {
      error(e, business);
      throw new IllegalArgumentException(e);
    }
    final String requested = Configuration.configuration.getHostId();
    DbTaskRunner runner = null;
    try {
      // Try to reload it
      runner = new DbTaskRunner(session, rule, identifier, user, requested);
      runner.setSender(!uploadMode);
      logger.debug("{} {} {}", identifier, user, requested);
      if (runner.isAllDone()) {
        error(new IllegalArgumentException("Already finished"), business);
      }
    } catch (final WaarpDatabaseNoDataException e) {
      // Not found so create it
      try {
        final Transfer transfer =
            new Transfer(user, rulename, rule.getMode(), !uploadMode, filename,
                         comment, chunkSize);
        transfer.setRequested(requested);
        transfer.setRequester(user);
        transfer.setId(identifier);
        runner = new DbTaskRunner(transfer);
        runner.insert();
        runner.select();
        runner.setSender(!uploadMode);
        logger.debug("{} {} {}", identifier, user, requested);
      } catch (final WaarpDatabaseException ex) {
        error(ex, business);
        throw new IllegalArgumentException(e);
      }
    } catch (final WaarpDatabaseException e) {
      error(e, business);
      throw new IllegalArgumentException(e);
    }
    runner.setOriginalFilename(filename);
    runner.setFilename(filename);
    runner.restart(false);
    try {
      session.setRunner(runner);
      session.setFileBeforePreRunner();
    } catch (final OpenR66RunnerErrorException e) {
      if (runner.getErrorInfo() == ErrorCode.InitOk ||
          runner.getErrorInfo() == ErrorCode.PreProcessingOk ||
          runner.getErrorInfo() == ErrorCode.TransferOk) {
        runner.setErrorExecutionStatus(ErrorCode.ExternalOp);
      }
      try {
        runner.saveStatus();
      } catch (final OpenR66RunnerErrorException e1) {
        logger.error("Cannot save Status: " + runner + ": {}", e1.getMessage());
      }
      runner.clean();
      error(e, business);
    }
    session.setReady(true);
    session.newState(REQUESTD);
    try {
      runner.saveStatus();
    } catch (final OpenR66RunnerErrorException e1) {
      logger.error("Cannot save Status: " + runner + ": {}", e1.getMessage());
    }
    return runner;
  }

  @Override
  public void error(final Exception e, final R66BusinessInterface business)
      throws IllegalArgumentException {
    logger.error(e.getMessage());
    if (business != null) {
      business.checkAtError(session);
    }
    session.newState(R66FiniteDualStates.ERROR);
    if (session.getRunner() != null) {
      final DbTaskRunner runner = session.getRunner();
      runner.setErrorExecutionStatus(ErrorCode.TransferError);
      runner.setErrorTask();
      try {
        runner.run();
        runner.saveStatus();
      } catch (final OpenR66RunnerErrorException e1) {
        logger.debug(e1);
      }
    }
    throw new IllegalArgumentException(e);
  }

  /**
   * Runs pre tasks
   *
   * @param business
   * @param runner
   *
   * @throws IllegalArgumentException
   */
  protected void preTasks(final R66BusinessInterface business,
                          final DbTaskRunner runner)
      throws IllegalArgumentException {
    runner.reset();
    runner.changeUpdatedInfo(UpdatedInfo.RUNNING);
    try {
      runner.saveStatus();
    } catch (final OpenR66RunnerErrorException e) {
      error(e, business);
    }
    // Now create the associated file
    try {
      session.setFileBeforePreRunner();
      if (runner.getStep() <= TASKSTEP.PRETASK.ordinal()) {
        runner.setPreTask();
        runner.saveStatus();
        runner.run();
        session.setFileAfterPreRunner(true);
      }
    } catch (final OpenR66RunnerErrorException e) {
      error(e, business);
    } catch (final CommandAbstractException e) {
      error(e, business);
    }
    session.newState(DATAR);
    try {
      runner.saveStatus();
    } catch (final OpenR66RunnerErrorException e1) {
      logger.error("Cannot save Status: " + runner + ": {}", e1.getMessage());
    }
  }

  /**
   * Set the transfer to be starting
   *
   * @param runner
   */
  protected void startTransfer(final DbTaskRunner runner) {
    runner.setTransferTask(0);
    try {
      runner.saveStatus();
    } catch (final OpenR66RunnerErrorException e1) {
      logger.error("Cannot save Status: " + runner + ": {}", e1.getMessage());
    }
  }

  /**
   * Run post tasks on finished transfer
   */
  protected void runPostTask() {
    final DbTaskRunner runner = session.getRunner();
    runner.setPostTask();
    try {
      session.newState(ENDTRANSFERR);
      logger.debug("Post actions {}", session);
      runner.run();
      runner.saveStatus();
      session.newState(ENDREQUESTS);
    } catch (final OpenR66RunnerErrorException e) {
      error(e, session.getBusinessObject());
    }
    session.newState(CLOSEDCHANNEL);
    session.partialClear();
    runner.setAllDone();
    try {
      runner.saveStatus();
    } catch (final OpenR66RunnerErrorException ignored) {
      // nothing
    }
    runner.clean();
  }

  /**
   * @return the current DbTaskRunner if any (null if not)
   */
  public DbTaskRunner getDbTaskRunner() {
    return session.getRunner();
  }
}
