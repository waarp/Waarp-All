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

package org.waarp.gateway.ftp.exec;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.waarp.common.command.exception.Reply421Exception;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * ExecuteExecutor class. The given argument will be executed after
 * replacements.
 *
 *
 * <br>
 * The following replacement are done dynamically before the command is
 * executed:<br>
 * - #BASEPATH# is replaced by the full path for the root of FTP Directory<br>
 * - #FILE# is replaced by the current file path relative to FTP Directory (so
 * #BASEPATH##FILE# is the full
 * path of the file)<br>
 * - #USER# is replaced by the username<br>
 * - #ACCOUNT# is replaced by the account<br>
 * - #COMMAND# is replaced by the command issued for the file<br>
 * - #SPECIALID# is replaced by the FTP id of the transfer (whatever in or
 * out)<br>
 * - #UUID# is replaced by a special UUID globally unique for the transfer, in
 * general to be placed in -info
 * part (for instance ##UUID## giving #uuid#)<br>
 */
public class ExecuteExecutor extends AbstractExecutor {
  private static final String EXCEPTION = "Exception: ";
  private static final String EXEC_IN_ERROR_WITH = "\n    Exec in error with ";
  private static final String CANNOT_EXECUTE_PRE_COMMAND =
      "Cannot execute Pre command";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(ExecuteExecutor.class);
  private static final Pattern BLANK = WaarpStringUtils.BLANK;
  private final String[] args;
  private final String arg;
  private final WaarpFuture futureCompletion;
  private final long delay;

  /**
   * @param command
   * @param delay
   * @param futureCompletion
   */
  public ExecuteExecutor(final String command, final long delay,
                         final WaarpFuture futureCompletion) {
    args = BLANK.split(command);
    arg = command;
    this.futureCompletion = futureCompletion;
    this.delay = delay;
  }

  @Override
  public void run() throws Reply421Exception {
    // Check if the execution will be done through LocalExec daemon
    if (AbstractExecutor.useLocalExec) {
      final LocalExecClient localExecClient = new LocalExecClient();
      if (localExecClient.connect()) {
        localExecClient.runOneCommand(arg, delay, futureCompletion);
        localExecClient.disconnect();
        return;
      } // else continue
    }
    // Execution is done internally
    final File exec = new File(args[0]);
    if (exec.isAbsolute() && !exec.canExecute()) {
      logger.error("Exec command is not executable: " + args[0]);
      throw new Reply421Exception("Pre Exec command is not executable");
    }
    final CommandLine commandLine = new CommandLine(args[0]);
    for (int i = 1; i < args.length; i++) {
      commandLine.addArgument(args[i]);
    }
    final DefaultExecutor defaultExecutor = new DefaultExecutor();
    final PumpStreamHandler pumpStreamHandler =
        new PumpStreamHandler(null, null);
    defaultExecutor.setStreamHandler(pumpStreamHandler);
    final int[] correctValues = { 0, 1 };
    defaultExecutor.setExitValues(correctValues);
    ExecuteWatchdog watchdog = null;
    if (delay > 0) {
      watchdog = new ExecuteWatchdog(delay);
      defaultExecutor.setWatchdog(watchdog);
    }
    int status;
    try {
      status = defaultExecutor.execute(commandLine);//NOSONAR
    } catch (final ExecuteException e) {
      if (e.getExitValue() == -559038737) {
        // Cannot run immediately so retry once
        try {
          Thread.sleep(10);
        } catch (final InterruptedException e1) {//NOSONAR
          SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
        }
        try {
          status = defaultExecutor.execute(commandLine);//NOSONAR
        } catch (final ExecuteException e2) {
          try {
            pumpStreamHandler.stop();
          } catch (final IOException ignored) {
            // nothing
          }
          logger.error("System Exception: " + e.getMessage() +
                       "\n    Exec cannot execute command " + commandLine);
          throw new Reply421Exception(CANNOT_EXECUTE_PRE_COMMAND);
        } catch (final IOException e2) {
          try {
            pumpStreamHandler.stop();
          } catch (final IOException ignored) {
            // nothing
          }
          logger.error(
              EXCEPTION + e.getMessage() + EXEC_IN_ERROR_WITH + commandLine);
          throw new Reply421Exception(CANNOT_EXECUTE_PRE_COMMAND);
        }
        logger.info("System Exception: " + e.getMessage() +
                    " but finally get the command executed " + commandLine);
      } else {
        try {
          pumpStreamHandler.stop();
        } catch (final IOException ignored) {
          // nothing
        }
        logger.error(
            EXCEPTION + e.getMessage() + EXEC_IN_ERROR_WITH + commandLine);
        throw new Reply421Exception(CANNOT_EXECUTE_PRE_COMMAND);
      }
    } catch (final IOException e) {
      try {
        pumpStreamHandler.stop();
      } catch (final IOException ignored) {
        // nothing
      }
      logger
          .error(EXCEPTION + e.getMessage() + EXEC_IN_ERROR_WITH + commandLine);
      throw new Reply421Exception(CANNOT_EXECUTE_PRE_COMMAND);
    }
    try {
      pumpStreamHandler.stop();
    } catch (final IOException ignored) {
      // nothing
    }
    if (watchdog != null && watchdog.killedProcess()) {
      // kill by the watchdoc (time out)
      logger.error("Exec is in Time Out");
      status = -1;
    }
    if (status == 0) {
      futureCompletion.setSuccess();
      logger.info("Exec OK with {}", commandLine);
    } else if (status == 1) {
      logger.warn("Exec in warning with {}", commandLine);
      futureCompletion.setSuccess();
    } else {
      logger.debug("Status: " + status + (status == -1? " Timeout" : "") +
                   " Exec in error with " + commandLine);
      throw new Reply421Exception("Pre command executed in error");
    }
  }
}
