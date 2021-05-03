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
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.regex.Pattern;

/**
 * Execute an external command
 * <p>
 * It provides some common functionalities.
 */
public abstract class AbstractExecTask extends AbstractTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AbstractExecTask.class);
  private static final Pattern COMPILE_REPLACE_ALL =
      Pattern.compile("#([A-Z]+)#");

  /**
   * Constructor
   *
   * @param type
   * @param delay
   * @param argRule
   * @param session
   */
  AbstractExecTask(final TaskType type, final int delay, final String argRule,
                   final String argTransfer, final R66Session session) {
    super(type, delay, argRule, argTransfer, session);
  }

  /**
   * Generates a Command line object from rule and transfer data
   *
   * @param line the command to process as a string
   */
  protected CommandLine buildCommandLine(final String line) {
    if (line.contains(NOWAIT)) {
      waitForValidation = false;
    }
    if (line.contains(LOCALEXEC)) {
      useLocalExec = true;
    }

    final String replacedLine =
        COMPILE_REPLACE_ALL.matcher(line).replaceAll("\\${$1}");

    final CommandLine commandLine =
        CommandLine.parse(replacedLine, getSubstitutionMap());

    final File exec = new File(commandLine.getExecutable());
    if (exec.isAbsolute() && !exec.canExecute()) {
      logger.error("Exec command is not executable: " + line);
      final R66Result result =
          new R66Result(session, false, ErrorCode.CommandNotFound,
                        session.getRunner());
      futureCompletion.setResult(result);
      futureCompletion.cancel();
      return null;
    }

    return commandLine;
  }

  /**
   * For External command execution
   */
  class PrepareCommandExec {
    private final boolean noOutput;
    private final boolean waitForValidation;
    private final String finalname;
    private boolean myResult;
    private CommandLine commandLine;
    private DefaultExecutor defaultExecutor;
    private PipedInputStream inputStream;
    private PipedOutputStream outputStream;
    private PumpStreamHandler pumpStreamHandler;
    private ExecuteWatchdog watchdog;

    PrepareCommandExec(final String finalname, final boolean noOutput,
                       final boolean waitForValidation) {
      this.finalname = finalname;
      this.noOutput = noOutput;
      this.waitForValidation = waitForValidation;
    }

    boolean isError() {
      return myResult;
    }

    public CommandLine getCommandLine() {
      return commandLine;
    }

    public DefaultExecutor getDefaultExecutor() {
      return defaultExecutor;
    }

    public PipedInputStream getInputStream() {
      return inputStream;
    }

    public PipedOutputStream getOutputStream() {
      return outputStream;
    }

    public PumpStreamHandler getPumpStreamHandler() {
      return pumpStreamHandler;
    }

    public ExecuteWatchdog getWatchdog() {
      return watchdog;
    }

    public PrepareCommandExec invoke() {
      commandLine = buildCommandLine(finalname);
      if (commandLine == null) {
        myResult = true;
        return this;
      }

      defaultExecutor = new DefaultExecutor();
      if (noOutput) {
        pumpStreamHandler = new PumpStreamHandler(null, null);
      } else {
        inputStream = new PipedInputStream();
        outputStream = null;
        try {
          outputStream = new PipedOutputStream(inputStream);
        } catch (final IOException e1) {
          FileUtils.close(inputStream);
          logger.error(
              "Exception: " + e1.getMessage() + " Exec in error with " +
              commandLine + ": {}", e1.getMessage());
          futureCompletion.setFailure(e1);
          myResult = true;
          return this;
        }
        pumpStreamHandler = new PumpStreamHandler(outputStream, null);
      }
      defaultExecutor.setStreamHandler(pumpStreamHandler);
      final int[] correctValues = { 0, 1 };
      defaultExecutor.setExitValues(correctValues);
      watchdog = null;
      if (delay > 0 && waitForValidation) {
        watchdog = new ExecuteWatchdog(delay);
        defaultExecutor.setWatchdog(watchdog);
      }
      myResult = false;
      return this;
    }
  }

  /**
   * For External command execution
   */
  class ExecuteCommand {
    private final CommandLine commandLine;
    private final DefaultExecutor defaultExecutor;
    private final PipedInputStream inputStream;
    private final PipedOutputStream outputStream;
    private final PumpStreamHandler pumpStreamHandler;
    private final Thread thread;
    private boolean myResult;
    private int status;

    ExecuteCommand(final CommandLine commandLine,
                   final DefaultExecutor defaultExecutor,
                   final PipedInputStream inputStream,
                   final PipedOutputStream outputStream,
                   final PumpStreamHandler pumpStreamHandler,
                   final Thread thread) {
      this.commandLine = commandLine;
      this.defaultExecutor = defaultExecutor;
      this.inputStream = inputStream;
      this.outputStream = outputStream;
      this.pumpStreamHandler = pumpStreamHandler;
      this.thread = thread;
    }

    boolean isError() {
      return myResult;
    }

    public int getStatus() {
      return status;
    }

    public ExecuteCommand invoke() {
      status = -1;
      try {
        status = defaultExecutor.execute(commandLine);//NOSONAR
      } catch (final ExecuteException e) {
        if (e.getExitValue() == -559038737) {
          // Cannot run immediately so retry once
          try {
            Thread.sleep(Configuration.RETRYINMS);
          } catch (final InterruptedException e1) {//NOSONAR
            SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
          }
          try {
            status = defaultExecutor.execute(commandLine);//NOSONAR
          } catch (final ExecuteException e1) {
            closeAllForExecution(true);
            finalizeFromError(thread, status, commandLine, e1);
            myResult = true;
            return this;
          } catch (final IOException e1) {
            closeAllForExecution(true);
            logger.error(
                "IOException: " + e.getMessage() + " . Exec in error with " +
                commandLine);
            futureCompletion.setFailure(e);
            myResult = true;
            return this;
          }
        } else {
          closeAllForExecution(true);
          finalizeFromError(thread, status, commandLine, e);
          myResult = true;
          return this;
        }
      } catch (final IOException e) {
        closeAllForExecution(true);
        logger.error(
            "IOException: " + e.getMessage() + " . Exec in error with " +
            commandLine);
        futureCompletion.setFailure(e);
        myResult = true;
        return this;
      }
      closeAllForExecution(false);
      if (thread != null) {
        try {
          if (delay > 0) {
            thread.join(delay);
          } else {
            thread.join();
          }
        } catch (final InterruptedException e) {//NOSONAR
          SysErrLogger.FAKE_LOGGER.ignoreLog(e);
          Thread.currentThread().interrupt();
        }
      }
      FileUtils.close(inputStream);
      myResult = false;
      return this;
    }

    private void closeAllForExecution(final boolean interrupt) {
      FileUtils.close(outputStream);
      if (interrupt && thread != null) {
        thread.interrupt();
      }
      FileUtils.close(inputStream);
      try {
        pumpStreamHandler.stop();
      } catch (final IOException ignored) {
        // nothing
      }
    }
  }

  void finalizeFromError(final Runnable threadReader, final int status,
                         final CommandLine commandLine, final Exception e) {
    logger.error("Status: " + status + " Exec in error with " + commandLine +
                 " returns " + e.getMessage());
    final OpenR66RunnerErrorException exc = new OpenR66RunnerErrorException(
        "<STATUS>" + status + "</STATUS><ERROR>" + e.getMessage() + "</ERROR>");
    futureCompletion.setFailure(exc);
  }
}
