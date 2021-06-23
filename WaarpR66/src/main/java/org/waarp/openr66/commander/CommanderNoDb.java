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

import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.openr66.configuration.ExtensionFilter;
import org.waarp.openr66.database.data.DbConfiguration;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Pattern;

/**
 * Commander is responsible to read list of updated data from time to time in
 * order to achieve new runner or
 * new configuration updates.
 * <p>
 * Based on no Database support
 */
public class CommanderNoDb implements CommanderInterface {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(CommanderNoDb.class);
  private static final Pattern COMPILE_ = Pattern.compile("_");

  private InternalRunner internalRunner;
  public static final ConcurrentLinkedQueue<AbstractDbData> todoList =
      new ConcurrentLinkedQueue<AbstractDbData>();

  /**
   * Prepare requests that will be executed from time to time
   *
   * @param runner
   */
  public CommanderNoDb(final InternalRunner runner) {
    internalConstructor(runner);
  }

  /**
   * Prepare requests that will be executed from time to time
   *
   * @param runner
   * @param fromStartup True if call from startup of the server
   */
  public CommanderNoDb(final InternalRunner runner, final boolean fromStartup) {
    internalConstructor(runner);
    if (fromStartup) {
      ClientRunner.activeRunners = new ConcurrentLinkedQueue<ClientRunner>();
      // Change RUNNING or INTERRUPTED to TOSUBMIT since they should be ready
      final File directory = new File(
          Configuration.configuration.getBaseDirectory() +
          Configuration.configuration.getArchivePath());
      final File[] files = FileUtils
          .getFiles(directory, new ExtensionFilter(DbTaskRunner.XMLEXTENSION));
      for (final File file : files) {
        final String shortname = file.getName();
        final String[] info = COMPILE_.split(shortname);
        if (info.length < 5) {
          continue;
        }
        final DbRule rule;
        try {
          rule = new DbRule(info[2]);
        } catch (final WaarpDatabaseException e) {
          logger.warn("Cannot find the rule named: " + info[2]);
          continue;
        }
        final long id = Long.parseLong(info[3]);
        try {
          final DbTaskRunner task =
              new DbTaskRunner(null, rule, id, info[0], info[1]);
          final UpdatedInfo status = task.getUpdatedInfo();
          if (status == UpdatedInfo.RUNNING ||
              status == UpdatedInfo.INTERRUPTED) {
            task.changeUpdatedInfo(UpdatedInfo.TOSUBMIT);
            task.update();
          }
        } catch (final WaarpDatabaseException e) {
          logger.warn("Cannot reload the task named: " + shortname);
        }
      }
    }
  }

  private void internalConstructor(final InternalRunner runner) {
    internalRunner = runner;
  }

  /**
   * Finalize internal data
   */
  @Override
  public void finalizeCommander() {
    // no since it will be reloaded
    // todoList.clear()
  }

  @Override
  public void run() {
    Thread.currentThread().setName("OpenR66Commander");
    while (!todoList.isEmpty()) {
      try {
        final AbstractDbData data = todoList.poll();
        // First check Configuration
        if (data instanceof DbConfiguration) {
          // should be only one...
          final DbConfiguration configuration = (DbConfiguration) data;
          if (configuration.isOwnConfiguration()) {
            configuration.updateConfiguration();
          }
          configuration.changeUpdatedInfo(UpdatedInfo.NOTUPDATED);
          configuration.update();
        }
        // Check HostAuthent
        else if (data instanceof DbHostAuth) {
          final DbHostAuth hostAuth = (DbHostAuth) data;
          // Nothing to do except validate
          hostAuth.changeUpdatedInfo(UpdatedInfo.NOTUPDATED);
          hostAuth.update();
        }
        // Check Rules
        else if (data instanceof DbRule) {
          // Nothing to do except validate
          final DbRule rule = (DbRule) data;
          rule.changeUpdatedInfo(UpdatedInfo.NOTUPDATED);
          rule.update();
        }
        // Check TaskRunner
        else if (data instanceof DbTaskRunner) {
          final DbTaskRunner taskRunner = (DbTaskRunner) data;
          logger.debug("get a task: {}", taskRunner);
          // Launch if possible this task
          final String key =
              taskRunner.getRequested() + ' ' + taskRunner.getRequester() +
              ' ' + taskRunner.getSpecialId();
          if (Configuration.configuration.getLocalTransaction()
                                         .getFromRequest(key) != null) {
            // already running
            continue;
          }
          if (taskRunner.isRequestOnRequested()) {
            // cannot schedule a request where the host is the requested host
            taskRunner.changeUpdatedInfo(UpdatedInfo.INTERRUPTED);
            taskRunner.update();
            continue;
          }
          taskRunner.changeUpdatedInfo(UpdatedInfo.RUNNING);
          taskRunner.update();
          internalRunner.submitTaskRunner(taskRunner);
          try {
            Thread.sleep(Configuration.RETRYINMS);
          } catch (final InterruptedException e) {//NOSONAR
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
          }
        }
        if (WaarpShutdownHook.isShutdownStarting()) {
          // no more task to submit
          return;
        }
      } catch (final WaarpDatabaseException e) {
        logger.error("Error in Commander: {}", e.getMessage());
      }
    }
  }

}
