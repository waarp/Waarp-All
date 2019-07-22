/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.context.task;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

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
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.context.task.localexec.LocalExecClient;
import org.waarp.openr66.protocol.configuration.Configuration;

/**
 * Execute an external command and Use the output if an error occurs.<br>
 * 
 * The output is ignored if the command has a correct status.<br>
 * In case of error, if the output finishes with <tt>NEWFINALNAME:xxx</tt> then this part is removed from the output
 * and the xxx is used as the last valid name for the file (meaning the file was moved or renamed even in case of error)<br>
 * <br>
 * 
 * waitForValidation (#NOWAIT#) must not be set since it will prevent to have the feedback in case
 * of error. So it is ignored.
 * 
 * @author Frederic Bregier
 * 
 */
public class ExecOutputTask extends AbstractExecTask {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(ExecOutputTask.class);
    /**
     * In the final line output, the filename must prefixed by the following field
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
         * First apply all replacements and format to argRule from context and argTransfer. Will
         * call exec (from first element of resulting string) with arguments as the following value
         * from the replacements. Return 0 if OK, else 1 for a warning else as an error. In case of
         * an error (> 0), all the line from output will be send back to the partner with the Error
         * code. No change is made to the file.
         */
        logger.info("ExecOutput with " + argRule + ":" + argTransfer + " and {}",
                session);
        String finalname = applyTransferSubstitutions(argRule);
        //
        // Force the WaitForValidation
        waitForValidation = true;
        if (Configuration.configuration.isUseLocalExec() && useLocalExec) {
            LocalExecClient localExecClient = new LocalExecClient();
            if (localExecClient.connect()) {
                localExecClient
                        .runOneCommand(finalname, delay, waitForValidation, futureCompletion);
                LocalExecResult result = localExecClient.getLocalExecResult();
                finalize(result.getStatus(), result.getResult(), finalname);
                localExecClient.disconnect();
                return;
            } // else continue
        }

        CommandLine commandLine = buildCommandLine(finalname);
        if (commandLine == null) {
            return;
        }

        DefaultExecutor defaultExecutor = new DefaultExecutor();
        PipedInputStream inputStream = new PipedInputStream();
        PipedOutputStream outputStream = null;
        try {
            outputStream = new PipedOutputStream(inputStream);
        } catch (IOException e1) {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
            logger.error("Exception: " + e1.getMessage() +
                    " Exec in error with " + commandLine.toString(), e1);
            futureCompletion.setFailure(e1);
            return;
        }
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(
                outputStream, null);
        defaultExecutor.setStreamHandler(pumpStreamHandler);
        int[] correctValues = {
                0, 1 };
        defaultExecutor.setExitValues(correctValues);
        ExecuteWatchdog watchdog = null;
        if (delay > 0) {
            watchdog = new ExecuteWatchdog(delay);
            defaultExecutor.setWatchdog(watchdog);
        }
        AllLineReader allLineReader = new AllLineReader(inputStream);
        Thread thread = new Thread(allLineReader, "ExecRename" + session.getRunner().getSpecialId());
        thread.setDaemon(true);
        Configuration.configuration.getExecutorService().execute(thread);
        int status = -1;
        try {
            status = defaultExecutor.execute(commandLine);
        } catch (ExecuteException e) {
            if (e.getExitValue() == -559038737) {
                // Cannot run immediately so retry once
                try {
                    Thread.sleep(Configuration.RETRYINMS);
                } catch (InterruptedException e1) {
                }
                try {
                    status = defaultExecutor.execute(commandLine);
                } catch (ExecuteException e1) {
                    finalizeFromError(outputStream,
                            pumpStreamHandler,
                            inputStream,
                            allLineReader,
                            thread,
                            status,
                            commandLine);
                    return;
                } catch (IOException e1) {
                    try {
                        outputStream.flush();
                    } catch (IOException e2) {
                    }
                    try {
                        outputStream.close();
                    } catch (IOException e2) {
                    }
                    thread.interrupt();
                    try {
                        inputStream.close();
                    } catch (IOException e2) {
                    }
                    try {
                        pumpStreamHandler.stop();
                    } catch (IOException e2) {
                    }
                    logger.error("IOException: " + e.getMessage() +
                            " . Exec in error with " + commandLine.toString());
                    futureCompletion.setFailure(e);
                    return;
                }
            } else {
                finalizeFromError(outputStream,
                        pumpStreamHandler,
                        inputStream,
                        allLineReader,
                        thread,
                        status,
                        commandLine);
                return;
            }
        } catch (IOException e) {
            try {
                outputStream.close();
            } catch (IOException e1) {
            }
            thread.interrupt();
            try {
                inputStream.close();
            } catch (IOException e1) {
            }
            try {
                pumpStreamHandler.stop();
            } catch (IOException e2) {
            }
            logger.error("IOException: " + e.getMessage() +
                    " . Exec in error with " + commandLine.toString());
            futureCompletion.setFailure(e);
            return;
        }
        try {
            outputStream.flush();
        } catch (IOException e) {
        }
        try {
            outputStream.close();
        } catch (IOException e) {
        }
        try {
            pumpStreamHandler.stop();
        } catch (IOException e2) {
        }
        try {
            if (delay > 0) {
                thread.join(delay);
            } else {
                thread.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            inputStream.close();
        } catch (IOException e1) {
        }
        String newname = null;
        if (defaultExecutor.isFailure(status) && watchdog != null &&
                watchdog.killedProcess()) {
            // kill by the watchdoc (time out)
            status = -1;
            newname = "TimeOut";
        } else {
            newname = allLineReader.getLastLine().toString();
        }
        finalize(status, newname, commandLine.toString());
    }

    private void finalize(int status, String newName, String commandLine) {
        String newname = newName;
        if (status == 0) {
            R66Result result = new R66Result(session, true, ErrorCode.CompleteOk, this.session.getRunner());
            result.setOther(newName);
            futureCompletion.setResult(result);
            futureCompletion.setSuccess();
            logger.info("Exec OK with {} returns {}", commandLine,
                    newname);
        } else if (status == 1) {
            R66Result result = new R66Result(session, true, ErrorCode.Warning, this.session.getRunner());
            result.setOther(newName);
            futureCompletion.setResult(result);
            logger.warn("Exec in warning with " + commandLine +
                    " returns " + newname);
            session.getRunner().setErrorExecutionStatus(ErrorCode.Warning);
            futureCompletion.setSuccess();
        } else {
            int pos = newname.lastIndexOf(DELIMITER);
            if (pos >= 0) {
                String newfilename = newname.substring(pos + DELIMITER.length());
                newname = newname.substring(0, pos);
                if (newfilename.indexOf(' ') > 0) {
                    logger.warn("Exec returns a multiple string in final line: " +
                            newfilename);
                    // XXX FIXME: should not split String[] args = newfilename.split(" ");
                    // newfilename = args[args.length - 1];
                }
                // now test if the previous file was deleted (should be)
                File file = new File(newfilename);
                if (!file.exists()) {
                    logger.warn("New file does not exist at the end of the exec: " + newfilename);
                }
                // now replace the file with the new one
                try {
                    session.getFile().replaceFilename(newfilename, true);
                } catch (CommandAbstractException e) {
                    logger
                            .warn("Exec in warning with " + commandLine,
                                    e);
                }
                session.getRunner().setFileMoved(newfilename, true);
            }
            logger.error("Status: " + status + " Exec in error with " +
                    commandLine + " returns " + newname);
            OpenR66RunnerErrorException exc =
                    new OpenR66RunnerErrorException("<STATUS>" + status + "</STATUS><ERROR>"
                            + newname + "</ERROR>");
            futureCompletion.setFailure(exc);
        }
    }

    private void finalizeFromError(PipedOutputStream outputStream,
            PumpStreamHandler pumpStreamHandler,
            PipedInputStream inputStream, AllLineReader allLineReader, Thread thread,
            int status, CommandLine commandLine) {
        try {
            Thread.sleep(Configuration.RETRYINMS);
        } catch (InterruptedException e) {
        }
        try {
            outputStream.flush();
        } catch (IOException e2) {
        }
        try {
            Thread.sleep(Configuration.RETRYINMS);
        } catch (InterruptedException e) {
        }
        try {
            outputStream.close();
        } catch (IOException e1) {
        }
        thread.interrupt();
        try {
            inputStream.close();
        } catch (IOException e1) {
        }
        try {
            Thread.sleep(Configuration.RETRYINMS);
        } catch (InterruptedException e) {
        }
        try {
            pumpStreamHandler.stop();
        } catch (IOException e2) {
        }
        try {
            Thread.sleep(Configuration.RETRYINMS);
        } catch (InterruptedException e) {
        }
        String result = allLineReader.getLastLine().toString();
        logger.error("Status: " + status + " Exec in error with " +
                commandLine + " returns " + result);
        OpenR66RunnerErrorException exc =
                new OpenR66RunnerErrorException("<STATUS>" + status + "</STATUS><ERROR>" + result
                        + "</ERROR>");
        futureCompletion.setFailure(exc);
    }
}
