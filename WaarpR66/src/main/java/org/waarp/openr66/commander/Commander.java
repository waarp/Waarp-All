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
package org.waarp.openr66.commander;

import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.openr66.database.data.DbConfiguration;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbHostConfiguration;
import org.waarp.openr66.database.data.DbMultipleMonitor;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;

import static org.waarp.openr66.database.DbConstantR66.*;

/**
 * Commander is responsible to read from database updated data from time to time
 * in order to achieve new
 * runner or new configuration updates.
 */
public class Commander implements CommanderInterface {
  private static final String DATABASE_ERROR_CANNOT_EXECUTE_COMMANDER =
      "Database Error: Cannot execute Commander";

  private static final String CONFIG = "Config {}";

  private static final String DATABASE_SQL_ERROR_CANNOT_EXECUTE_COMMANDER =
      "Database SQL Error: Cannot execute Commander";

  private static final String
      DATABASE_NO_CONNECTION_ERROR_CANNOT_EXECUTE_COMMANDER =
      "Database No Connection Error: Cannot execute Commander";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(Commander.class);

  public static final int LIMIT_SUBMIT = 1000;

  private InternalRunner internalRunner;
  private DbPreparedStatement preparedStatementLock;
  private long totalRuns = 0;

  /**
   * Prepare requests that will be executed from time to time
   *
   * @param runner
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public Commander(final InternalRunner runner)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    internalConstructor(runner);
  }

  /**
   * Prepare requests that will be executed from time to time
   *
   * @param runner
   * @param fromStartup True if call from startup of the server
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public Commander(final InternalRunner runner, final boolean fromStartup)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    internalConstructor(runner);
    if (fromStartup) {
      // Change RUNNING or INTERRUPTED to TOSUBMIT since they should be ready
      DbTaskRunner.resetToSubmit(admin.getSession());
    }
  }

  private void internalConstructor(final InternalRunner runner)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    try {
      if (Configuration.configuration.getMultipleMonitors() > 1) {
        preparedStatementLock = DbMultipleMonitor
            .getUpdatedPrepareStament(noCommitAdmin.getSession());
      } else {
        preparedStatementLock = null;
      }
      // Clean tasks (CompleteOK and ALLDONE => DONE)
      DbTaskRunner.changeFinishedToDone();
      internalRunner = runner;
    } finally {
      if (internalRunner == null) {
        // An error occurs
        if (preparedStatementLock != null) {
          preparedStatementLock.realClose();
        }
      } else {
        if (preparedStatementLock != null) {
          noCommitAdmin.getSession()
                       .addLongTermPreparedStatement(preparedStatementLock);
        }
      }
    }
  }

  /**
   * Finalize internal data
   */
  @Override
  public void finalizeCommander() {
    if (preparedStatementLock != null) {
      try {
        noCommitAdmin.getSession().commit();
      } catch (final WaarpDatabaseSqlException ignored) {
        // nothing
      } catch (final WaarpDatabaseNoConnectionException ignored) {
        // nothing
      }
      preparedStatementLock.realClose();
      noCommitAdmin.getSession()
                   .removeLongTermPreparedStatements(preparedStatementLock);
      // DbConstant.noCommitAdmin.session.removeLongTermPreparedStatements()
    }
    // DbConstant.admin.session.removeLongTermPreparedStatements()
  }

  @Override
  public void run() {
    Thread.currentThread().setName("OpenR66Commander");
    if (admin.getSession() != null && admin.getSession().isDisActive()) {
      admin.getSession().checkConnectionNoException();
    }
    // each time it is runned, it parses all database for updates
    DbMultipleMonitor multipleMonitor = null;
    // Open a lock to prevent other "HA" monitors to retrieve access as Commander
    try {
      try {
        if (preparedStatementLock != null) {
          preparedStatementLock.executeQuery();
          preparedStatementLock.getNext();
          multipleMonitor =
              DbMultipleMonitor.getFromStatement(preparedStatementLock);
        }
      } catch (final WaarpDatabaseNoConnectionException e) {
        logger.error(DATABASE_NO_CONNECTION_ERROR_CANNOT_EXECUTE_COMMANDER, e);
        try {
          noCommitAdmin.getDbModel()
                       .validConnection(noCommitAdmin.getSession());
        } catch (final WaarpDatabaseNoConnectionException ignored) {
          // nothing
        }
        return;
      } catch (final WaarpDatabaseSqlException e) {
        logger.error(DATABASE_SQL_ERROR_CANNOT_EXECUTE_COMMANDER, e);
        try {
          noCommitAdmin.getDbModel()
                       .validConnection(noCommitAdmin.getSession());
        } catch (final WaarpDatabaseNoConnectionException ignored) {
          // nothing
        }
        return;
      }
      logger.debug("Before {}", multipleMonitor);
      boolean shallReturnInCaseError = true;
      try {
        // First check Configuration
        checkConfiguration(multipleMonitor);
        // check HostConfiguration
        shallReturnInCaseError = false;
        checkHostConfiguration(multipleMonitor);
      } catch (final WaarpDatabaseNoConnectionException e) {
        try {
          admin.getDbModel().validConnection(admin.getSession());
        } catch (final WaarpDatabaseNoConnectionException ignored) {
          // nothing
        }
        logger.error(DATABASE_NO_CONNECTION_ERROR_CANNOT_EXECUTE_COMMANDER, e);
        return;
      } catch (final WaarpDatabaseSqlException e) {
        try {
          admin.getDbModel().validConnection(admin.getSession());
        } catch (final WaarpDatabaseNoConnectionException ignored) {
          // nothing
        }
        logger.error(DATABASE_SQL_ERROR_CANNOT_EXECUTE_COMMANDER, e);
        // XXX no return since table might not be initialized
        if (shallReturnInCaseError) {
          return;
        }
      } catch (final WaarpDatabaseException e) {
        try {
          admin.getDbModel().validConnection(admin.getSession());
        } catch (final WaarpDatabaseNoConnectionException ignored) {
          // nothing
        }
        logger.error(DATABASE_ERROR_CANNOT_EXECUTE_COMMANDER, e);
        // XXX no return since table might not be initialized
        if (shallReturnInCaseError) {
          return;
        }
      }
      // Do not fusion with previous cases since last one could be in error but
      // still continue
      try {
        // ConsistencyCheck HostAuthent
        checkHostAuthent(multipleMonitor);
        // Check Rules
        checkRule(multipleMonitor);
        if (WaarpShutdownHook.isShutdownStarting()) {
          // no more task to submit
          return;
        }

        // Lauch Transfer ready to be submited
        logger.debug("start runner");
        checkTaskRunner(multipleMonitor);
      } catch (final WaarpDatabaseNoConnectionException e) {
        try {
          admin.getDbModel().validConnection(admin.getSession());
        } catch (final WaarpDatabaseNoConnectionException ignored) {
          // nothing
        }
        logger.error(DATABASE_NO_CONNECTION_ERROR_CANNOT_EXECUTE_COMMANDER, e);
        return;
      } catch (final WaarpDatabaseSqlException e) {
        try {
          admin.getDbModel().validConnection(admin.getSession());
        } catch (final WaarpDatabaseNoConnectionException ignored) {
          // nothing
        }
        logger.error(DATABASE_SQL_ERROR_CANNOT_EXECUTE_COMMANDER, e);
        return;
      } catch (final WaarpDatabaseNoDataException e) {
        try {
          admin.getDbModel().validConnection(admin.getSession());
        } catch (final WaarpDatabaseNoConnectionException ignored) {
          // nothing
        }
        logger.error(DATABASE_ERROR_CANNOT_EXECUTE_COMMANDER, e);
        return;
      } catch (final WaarpDatabaseException e) {
        try {
          admin.getDbModel().validConnection(admin.getSession());
        } catch (final WaarpDatabaseNoConnectionException ignored) {
          // nothing
        }
        logger.error(DATABASE_ERROR_CANNOT_EXECUTE_COMMANDER, e);
        return;
      }
      logger.debug("end commander");
    } finally {
      if (multipleMonitor != null) {
        try {
          // Now update and Commit so releasing the lock
          logger.debug("Update {}", multipleMonitor);
          multipleMonitor.update();
          noCommitAdmin.getSession().commit();
        } catch (final WaarpDatabaseException e) {
          try {
            noCommitAdmin.getDbModel()
                         .validConnection(noCommitAdmin.getSession());
          } catch (final WaarpDatabaseNoConnectionException ignored) {
            // nothing
          }
        }
      }
    }
  }

  private void checkConfiguration(DbMultipleMonitor multipleMonitor)
      throws WaarpDatabaseException {
    final DbConfiguration[] configurations =
        DbConfiguration.getUpdatedPrepareStament();
    int i = 0;
    while (i < configurations.length) {
      // should be only one...
      final DbConfiguration configuration = configurations[i];
      if (configuration.isOwnConfiguration()) {
        configuration.updateConfiguration();
      }
      if (multipleMonitor != null) {
        // update the configuration in HA mode
        if (multipleMonitor.checkUpdateConfig()) {
          configuration
              .changeUpdatedInfo(AbstractDbData.UpdatedInfo.NOTUPDATED);
          configuration.update();
          logger.debug(CONFIG, multipleMonitor);
        } else {
          configuration.update();
          logger.debug(CONFIG, multipleMonitor);
        }
      } else {
        configuration.changeUpdatedInfo(AbstractDbData.UpdatedInfo.NOTUPDATED);
        configuration.update();
      }
      i++;
    }
  }

  private void checkHostConfiguration(DbMultipleMonitor multipleMonitor)
      throws WaarpDatabaseException {
    final DbHostConfiguration[] configurations =
        DbHostConfiguration.getUpdatedPrepareStament();
    int i = 0;
    while (i < configurations.length) {
      // should be only one...
      final DbHostConfiguration configuration = configurations[i];
      if (configuration.isOwnConfiguration()) {
        configuration.updateConfiguration();
      }
      if (multipleMonitor != null) {
        // update the configuration in HA mode
        if (multipleMonitor.checkUpdateConfig()) {
          configuration
              .changeUpdatedInfo(AbstractDbData.UpdatedInfo.NOTUPDATED);
          configuration.update();
          logger.debug(CONFIG, multipleMonitor);
        } else {
          configuration.update();
          logger.debug(CONFIG, multipleMonitor);
        }
      } else {
        configuration.changeUpdatedInfo(AbstractDbData.UpdatedInfo.NOTUPDATED);
        configuration.update();
      }
      i++;
    }
  }

  private void checkHostAuthent(DbMultipleMonitor multipleMonitor)
      throws WaarpDatabaseException {
    final DbHostAuth[] auths = DbHostAuth.getUpdatedPreparedStatement();
    int i = 0;
    boolean mm = false;
    boolean lastUpdate = false;
    while (i < auths.length) {
      // Maybe multiple
      final DbHostAuth hostAuth = auths[i];
      if (multipleMonitor != null) {
        if (!mm) {
          // not already set from a previous hostAuth
          mm = true;
          lastUpdate = multipleMonitor.checkUpdateHost();
        } // else already set so no action on multipleMonitor

        // Update the Host configuration in HA mode
        if (lastUpdate) {
          hostAuth.changeUpdatedInfo(AbstractDbData.UpdatedInfo.NOTUPDATED);
        } else {
          // Nothing to do except validate
        }
        hostAuth.update();
        logger.debug("Host {}", multipleMonitor);
      } else {
        // Nothing to do except validate
        hostAuth.changeUpdatedInfo(AbstractDbData.UpdatedInfo.NOTUPDATED);
        hostAuth.update();
      }
      i++;
    }
  }

  private void checkRule(DbMultipleMonitor multipleMonitor)
      throws WaarpDatabaseException {
    final DbRule[] rules = DbRule.getUpdatedPrepareStament();
    int i = 0;
    boolean mm = false;
    boolean lastUpdate = false;
    while (i < rules.length) {
      final DbRule rule = rules[i];
      if (multipleMonitor != null) {
        if (!mm) {
          // not already set from a previous hostAuth
          mm = true;
          lastUpdate = multipleMonitor.checkUpdateRule();
        } // else already set so no action on multipleMonitor
        // Update the Rules in HA mode
        if (lastUpdate) {
          rule.changeUpdatedInfo(AbstractDbData.UpdatedInfo.NOTUPDATED);
        } else {
          // Nothing to do except validate
        }
        rule.update();
        logger.debug("Rule {}", multipleMonitor);
      } else {
        // Nothing to do except validate
        rule.changeUpdatedInfo(AbstractDbData.UpdatedInfo.NOTUPDATED);
        rule.update();
      }
      i++;
    }
  }

  private void checkTaskRunner(DbMultipleMonitor multipleMonitor)
      throws WaarpDatabaseException {
    // No specific HA mode since the other servers will wait for the commit on Lock
    final int maxRunnable =
        Math.min(Configuration.configuration.getRunnerThread(),
                 internalRunner.allowedToSubmit());
    if (maxRunnable > 0) {
      final DbTaskRunner[] tasks = DbTaskRunner
          .getSelectFromInfoPrepareStatement(UpdatedInfo.TOSUBMIT, true,
                                             maxRunnable);
      logger.info("TaskRunner to launch: {} (launched: {}, active: {}) {}",
                  tasks.length, totalRuns, internalRunner.nbInternalRunner(),
                  NetworkTransaction.hashStatus());
      int i = 0;
      while (i < tasks.length) {
        if (WaarpShutdownHook.isShutdownStarting()) {
          logger.info("Will not start transfers, server is in shutdown.");
          return;
        }
        final DbTaskRunner taskRunner = tasks[i];
        i++;

        logger.debug("get a task: {}", taskRunner);
        // Launch if possible this task
        final String key =
            taskRunner.getRequested() + ' ' + taskRunner.getRequester() + ' ' +
            taskRunner.getSpecialId();
        if (Configuration.configuration.getLocalTransaction()
                                       .getFromRequest(key) != null) {
          // already running
          continue;
        }
        if (taskRunner.isSelfRequested()) {
          // cannot schedule a request where the host is the requested host
          taskRunner.changeUpdatedInfo(UpdatedInfo.INTERRUPTED);
          try {
            taskRunner.update();
          } catch (final WaarpDatabaseNoDataException e) {
            logger.warn("Update failed, no transfer found");
          }
          continue;
        }
        internalRunner.submitTaskRunner(taskRunner);
        totalRuns++;
      }
    }
  }
}
