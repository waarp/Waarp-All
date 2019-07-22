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
package org.waarp.openr66.context.task;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.waarp.commandexec.utils.LocalExecResult;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.localexec.LocalExecClient;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Execute an external command and Rename the file (using the new name from the
 * result).<br>
 * <p>
 * The move of the file (if any) should be done by the external command
 * itself.<br>
 * <br>
 * <p>
 * waitForValidation (#NOWAIT#) must not be set since it will prevent to have
 * the MOVE TASK to occur normally.
 * So even if set, the #NOWAIT# will be ignored.
 *
 *
 */
public class ExecMoveTask extends AbstractExecTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(ExecMoveTask.class);

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public ExecMoveTask(String argRule, int delay, String argTransfer,
                      R66Session session) {
    super(TaskType.EXECMOVE, delay, argRule, argTransfer, session);
  }

  @Override
  public void run() {
    /*
     * First apply all replacements and format to argRule from context and argTransfer. Will call exec (from first
     * element of resulting string) with arguments as the following value from the replacements. Return 0 if OK,
     * else 1 for a warning else as an error. The last line of stdout will be the new name given to the R66File in
     * case of status 0. The previous file should be deleted by the script or will be deleted in case of status 0.
     * If the status is 1, no change is made to the file.
     */
    logger.info("ExecMove with " + argRule + ":" + argTransfer + " and {}",
                session);
    final String finalname = applyTransferSubstitutions(argRule);

    // Force the WaitForValidation
    waitForValidation = true;
    if (Configuration.configuration.isUseLocalExec() && useLocalExec) {
      final LocalExecClient localExecClient = new LocalExecClient();
      if (localExecClient.connect()) {
        localExecClient.runOneCommand(finalname, delay, waitForValidation,
                                      futureCompletion);
        final LocalExecResult result = localExecClient.getLocalExecResult();
        move(result.getStatus(), result.getResult(), finalname);
        localExecClient.disconnect();
        return;
      } // else continue
    }

    final CommandLine commandLine = buildCommandLine(finalname);
    if (commandLine == null) {
      return;
    }

    final DefaultExecutor defaultExecutor = new DefaultExecutor();
    final PipedInputStream inputStream = new PipedInputStream();
    PipedOutputStream outputStream = null;
    try {
      outputStream = new PipedOutputStream(inputStream);
    } catch (final IOException e1) {
      try {
        inputStream.close();
      } catch (final IOException e) {
      }
      logger.error("Exception: " + e1.getMessage() + " Exec in error with " +
                   commandLine.toString(), e1);
      futureCompletion.setFailure(e1);
      return;
    }
    final PumpStreamHandler pumpStreamHandler =
        new PumpStreamHandler(outputStream, null);
    defaultExecutor.setStreamHandler(pumpStreamHandler);
    final int[] correctValues = { 0, 1 };
    defaultExecutor.setExitValues(correctValues);
    ExecuteWatchdog watchdog = null;

    if (delay > 0) {
      watchdog = new ExecuteWatchdog(delay);
      defaultExecutor.setWatchdog(watchdog);
    }
    final LastLineReader lastLineReader = new LastLineReader(inputStream);
    final Thread thread = new Thread(lastLineReader, "ExecRename" +
                                                     session.getRunner()
                                                            .getSpecialId());
    thread.setDaemon(true);
    Configuration.configuration.getExecutorService().execute(thread);
    int status = -1;
    try {
      status = defaultExecutor.execute(commandLine);
    } catch (final ExecuteException e) {
      if (e.getExitValue() == -559038737) {
        // Cannot run immediately so retry once
        try {
          Thread.sleep(Configuration.RETRYINMS);
        } catch (final InterruptedException e1) {
        }
        try {
          status = defaultExecutor.execute(commandLine);
        } catch (final ExecuteException e1) {
          try {
            outputStream.close();
          } catch (final IOException e2) {
          }
          thread.interrupt();
          try {
            inputStream.close();
          } catch (final IOException e2) {
          }
          try {
            pumpStreamHandler.stop();
          } catch (final IOException e2) {
          }
          logger.error(
              "ExecuteException: " + e.getMessage() + " . Exec in error with " +
              commandLine.toString());
          futureCompletion.setFailure(e);
          return;
        } catch (final IOException e1) {
          try {
            outputStream.close();
          } catch (final IOException e2) {
          }
          thread.interrupt();
          try {
            inputStream.close();
          } catch (final IOException e2) {
          }
          try {
            pumpStreamHandler.stop();
          } catch (final IOException e2) {
          }
          logger.error(
              "IOException: " + e.getMessage() + " . Exec in error with " +
              commandLine.toString());
          futureCompletion.setFailure(e);
          return;
        }
      } else {
        try {
          outputStream.close();
        } catch (final IOException e1) {
        }
        thread.interrupt();
        try {
          inputStream.close();
        } catch (final IOException e1) {
        }
        try {
          pumpStreamHandler.stop();
        } catch (final IOException e2) {
        }
        logger.error(
            "ExecuteException: " + e.getMessage() + " . Exec in error with " +
            commandLine.toString());
        futureCompletion.setFailure(e);
        return;
      }
    } catch (final IOException e) {
      try {
        outputStream.close();
      } catch (final IOException e1) {
      }
      thread.interrupt();
      try {
        inputStream.close();
      } catch (final IOException e1) {
      }
      try {
        pumpStreamHandler.stop();
      } catch (final IOException e2) {
      }
      logger.error("IOException: " + e.getMessage() + " . Exec in error with " +
                   commandLine.toString());
      futureCompletion.setFailure(e);
      return;
    }
    try {
      outputStream.flush();
    } catch (final IOException e) {
    }
    try {
      outputStream.close();
    } catch (final IOException e) {
    }
    try {
      pumpStreamHandler.stop();
    } catch (final IOException e2) {
    }
    try {
      if (delay > 0) {
        thread.join(delay);
      } else {
        thread.join();
      }
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    try {
      inputStream.close();
    } catch (final IOException e1) {
    }
    String newname = null;
    if (defaultExecutor.isFailure(status) && watchdog != null &&
        watchdog.killedProcess()) {
      // kill by the watchdoc (time out)
      status = -1;
      newname = "TimeOut";
    } else {
      newname = lastLineReader.getLastLine();
      if (status == 0 && (newname == null || newname.isEmpty())) {
        status = 1;
      }
    }
    move(status, newname, commandLine.toString());
  }

  private void move(int status, String newName, String commandLine) {
    final String newname = newName.replace('\\', '/');
    if (status == 0) {
      if (newname.indexOf(' ') > 0) {
        logger.warn("Exec returns a multiple string in final line: " + newname);
        // XXX FIXME: should not split String[] args = newname.split(" ");
        // newname = args[args.length - 1];
      }
      // now test if the previous file was deleted (should be)
      final File file = new File(newname);
      if (!file.exists()) {
        logger
            .warn("New file does not exist at the end of the exec: " + newname);
      }
      // now replace the file with the new one
      try {
        session.getFile().replaceFilename(newname, true);
      } catch (final CommandAbstractException e) {
        logger.warn("Exec in warning with " + commandLine, e);
      }
      session.getRunner().setFileMoved(newname, true);
      final R66Result result =
          new R66Result(session, true, ErrorCode.CompleteOk,
                        session.getRunner());
      result.setOther(newname);
      futureCompletion.setResult(result);
      futureCompletion.setSuccess();
      logger.info("Exec OK with {} returns {}", commandLine, newname);
    } else if (status == 1) {
      logger
          .warn("Exec in warning with " + commandLine + " returns " + newname);
      session.getRunner().setErrorExecutionStatus(ErrorCode.Warning);
      final R66Result result =
          new R66Result(session, true, ErrorCode.Warning, session.getRunner());
      result.setOther(newname);
      futureCompletion.setResult(result);
      futureCompletion.setSuccess();
    } else {
      logger.error("Status: " + status + " Exec in error with " + commandLine +
                   " returns " + newname);
      futureCompletion.cancel();
    }
  }
}
