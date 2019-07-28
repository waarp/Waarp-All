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
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.waarp.commandexec.utils.LocalExecResult;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.context.task.localexec.LocalExecClient;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.io.File;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

/**
 * Execute an external command and Use the output if an error occurs.<br>
 * <p>
 * The output is ignored if the command has a correct status.<br>
 * In case of error, if the output finishes with <tt>NEWFINALNAME:xxx</tt> then
 * this part is removed from the
 * output and the xxx is used as the last valid name for the file (meaning the
 * file was moved or renamed even
 * in case of error)<br>
 * <br>
 * <p>
 * waitForValidation (#NOWAIT#) must not be set since it will prevent to have
 * the feedback in case of error.
 * So it is ignored.
 */
public class ExecOutputTask extends AbstractExecTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(ExecOutputTask.class);
  /**
   * In the final line output, the filename must prefixed by the following
   * field
   */
  public static final String DELIMITER = "NEWFINALNAME:";

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public ExecOutputTask(String argRule, int delay, String argTransfer,
                        R66Session session) {
    super(TaskType.EXECOUTPUT, delay, argRule, argTransfer, session);
  }

  @Override
  public void run() {
    /*
     * First apply all replacements and format to argRule from context and argTransfer. Will call exec (from first
     * element of resulting string) with arguments as the following value from the replacements. Return 0 if OK,
     * else 1 for a warning else as an error. In case of an error (> 0), all the line from output will be send
     * back to the partner with the Error code. No change is made to the file.
     */
    logger.info("ExecOutput with " + argRule + ':' + argTransfer + " and {}",
                session);
    final String finalname = applyTransferSubstitutions(argRule);
    //
    // Force the WaitForValidation
    waitForValidation = true;
    if (Configuration.configuration.isUseLocalExec() && useLocalExec) {
      final LocalExecClient localExecClient = new LocalExecClient();
      if (localExecClient.connect()) {
        localExecClient.runOneCommand(finalname, delay, waitForValidation,
                                      futureCompletion);
        final LocalExecResult result = localExecClient.getLocalExecResult();
        finalizeExec(result.getStatus(), result.getResult(), finalname);
        localExecClient.disconnect();
        return;
      } // else continue
    }

    PrepareCommandExec prepareCommandExec =
        new PrepareCommandExec(finalname, false, waitForValidation).invoke();
    if (prepareCommandExec.isError()) {
      return;
    }
    CommandLine commandLine = prepareCommandExec.getCommandLine();
    DefaultExecutor defaultExecutor = prepareCommandExec.getDefaultExecutor();
    PipedInputStream inputStream = prepareCommandExec.getInputStream();
    PipedOutputStream outputStream = prepareCommandExec.getOutputStream();
    PumpStreamHandler pumpStreamHandler =
        prepareCommandExec.getPumpStreamHandler();
    ExecuteWatchdog watchdog = prepareCommandExec.getWatchdog();

    final AllLineReader allLineReader = new AllLineReader(inputStream);
    final Thread thread = new Thread(allLineReader, "ExecRename" +
                                                    session.getRunner()
                                                           .getSpecialId());
    thread.setDaemon(true);
    Configuration.configuration.getExecutorService().execute(thread);
    ExecuteCommand executeCommand =
        new ExecuteCommand(commandLine, defaultExecutor, inputStream,
                           outputStream, pumpStreamHandler, thread).invoke();
    if (executeCommand.isError()) {
      return;
    }
    int status = executeCommand.getStatus();
    String newname;
    if (defaultExecutor.isFailure(status) && watchdog != null &&
        watchdog.killedProcess()) {
      // kill by the watchdoc (time out)
      status = -1;
      newname = "TimeOut";
    } else {
      newname = allLineReader.getLastLine().toString();
    }
    finalizeExec(status, newname, commandLine.toString());
  }

  private void finalizeExec(int status, String newName, String commandLine) {
    String newname = newName;
    if (status == 0) {
      final R66Result result =
          new R66Result(session, true, ErrorCode.CompleteOk,
                        session.getRunner());
      result.setOther(newName);
      futureCompletion.setResult(result);
      futureCompletion.setSuccess();
      logger.info("Exec OK with {} returns {}", commandLine, newname);
    } else if (status == 1) {
      final R66Result result =
          new R66Result(session, true, ErrorCode.Warning, session.getRunner());
      result.setOther(newName);
      futureCompletion.setResult(result);
      logger
          .warn("Exec in warning with " + commandLine + " returns " + newname);
      session.getRunner().setErrorExecutionStatus(ErrorCode.Warning);
      futureCompletion.setSuccess();
    } else {
      final int pos = newname.lastIndexOf(DELIMITER);
      if (pos >= 0) {
        final String newfilename = newname.substring(pos + DELIMITER.length());
        newname = newname.substring(0, pos);
        if (newfilename.indexOf(' ') > 0) {
          logger.warn(
              "Exec returns a multiple string in final line: " + newfilename);
          // XXX FIXME: should not split String[] args = newfilename.split(" ")
          // newfilename = args[args.length - 1]
        }
        // now test if the previous file was deleted (should be)
        final File file = new File(newfilename);
        if (!file.exists()) {
          logger.warn(
              "New file does not exist at the end of the exec: " + newfilename);
        }
        // now replace the file with the new one
        try {
          session.getFile().replaceFilename(newfilename, true);
        } catch (final CommandAbstractException e) {
          logger.warn("Exec in warning with " + commandLine, e);
        }
        session.getRunner().setFileMoved(newfilename, true);
      }
      logger.error("Status: " + status + " Exec in error with " + commandLine +
                   " returns " + newname);
      final OpenR66RunnerErrorException exc = new OpenR66RunnerErrorException(
          "<STATUS>" + status + "</STATUS><ERROR>" + newname + "</ERROR>");
      futureCompletion.setFailure(exc);
    }
  }

  @Override
  void finalizeFromError(Runnable threadReader, int status,
                         CommandLine commandLine, Exception e) {
    try {
      Thread.sleep(Configuration.RETRYINMS);
    } catch (final InterruptedException e2) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(e2);
    }
    final String result =
        ((AllLineReader) threadReader).getLastLine().toString();
    logger.error("Status: " + status + " Exec in error with " + commandLine +
                 " returns " + result);
    final OpenR66RunnerErrorException exc = new OpenR66RunnerErrorException(
        "<STATUS>" + status + "</STATUS><ERROR>" + result + "</ERROR>");
    futureCompletion.setFailure(exc);
  }
}
