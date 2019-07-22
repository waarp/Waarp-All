/**
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation; either version 3.0 of the
 * License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * software; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.waarp.gateway.kernel.exec;

import java.io.File;
import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.waarp.common.command.exception.Reply421Exception;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

/**
 * ExecuteExecutor class. The given argument will be executed after replacements.
 *
 *
 * <br>
 * The following replacement are done dynamically before the command is executed:<br>
 * - #BASEPATH# is replaced by the full path for the root of FTP Directory<br>
 * - #FILE# is replaced by the current file path relative to FTP Directory (so #BASEPATH##FILE# is
 * the full path of the file)<br>
 * - #USER# is replaced by the username<br>
 * - #ACCOUNT# is replaced by the account<br>
 * - #COMMAND# is replaced by the command issued for the file<br>
 * - #SPECIALID# is replaced by the FTP id of the transfer (whatever in or out)<br>
 * - #UUID# is replaced by a special UUID globally unique for the transfer, in general to be placed in -info part (for instance ##UUID## giving #uuid#)<br>
 *
 * @author Frederic Bregier
 *
 */
public class ExecuteExecutor extends AbstractExecutor {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(ExecuteExecutor.class);
    private final String[] args;
    private final String arg;
    private final WaarpFuture futureCompletion;
    private final long delay;

    /**
     *
     * @param command
     * @param delay
     * @param futureCompletion
     */
    public ExecuteExecutor(String command, long delay, WaarpFuture futureCompletion) {
        this.args = command.split(" ");
        this.arg = command;
        this.futureCompletion = futureCompletion;
        this.delay = delay;
    }

    public void run() throws Reply421Exception {
        // Check if the execution will be done through LocalExec daemon
        if (AbstractExecutor.useLocalExec) {
            LocalExecClient localExecClient = new LocalExecClient();
            if (localExecClient.connect()) {
                localExecClient.runOneCommand(arg, delay, futureCompletion);
                localExecClient.disconnect();
                return;
            }// else continue
        }
        // Execution is done internally
        File exec = new File(args[0]);
        if (exec.isAbsolute()) {
            if (!exec.canExecute()) {
                logger.error("Exec command is not executable: " + args[0]);
                throw new Reply421Exception("Pre Exec command is not executable");
            }
        }
        CommandLine commandLine = new CommandLine(args[0]);
        for (int i = 1; i < args.length; i++) {
            commandLine.addArgument(args[i]);
        }
        DefaultExecutor defaultExecutor = new DefaultExecutor();
        PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(null, null);
        defaultExecutor.setStreamHandler(pumpStreamHandler);
        int[] correctValues = {
                0, 1 };
        defaultExecutor.setExitValues(correctValues);
        ExecuteWatchdog watchdog = null;
        if (delay > 0) {
            watchdog = new ExecuteWatchdog(delay);
            defaultExecutor.setWatchdog(watchdog);
        }
        int status = -1;
        try {
            status = defaultExecutor.execute(commandLine);
        } catch (ExecuteException e) {
            if (e.getExitValue() == -559038737) {
                // Cannot run immediately so retry once
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e1) {
                }
                try {
                    status = defaultExecutor.execute(commandLine);
                } catch (ExecuteException e2) {
                    try {
                        pumpStreamHandler.stop();
                    } catch (IOException e1) {
                    }
                    logger.error("System Exception: " + e.getMessage() +
                            "\n    Exec cannot execute command " + commandLine.toString());
                    throw new Reply421Exception("Cannot execute Pre command");
                } catch (IOException e2) {
                    try {
                        pumpStreamHandler.stop();
                    } catch (IOException e1) {
                    }
                    logger.error("Exception: " + e.getMessage() +
                            "\n    Exec in error with " + commandLine.toString());
                    throw new Reply421Exception("Cannot execute Pre command");
                }
                logger.info("System Exception: " + e.getMessage() +
                        " but finally get the command executed " + commandLine.toString());
            } else {
                try {
                    pumpStreamHandler.stop();
                } catch (IOException e1) {
                }
                logger.error("Exception: " + e.getMessage() +
                        "\n    Exec in error with " + commandLine.toString());
                throw new Reply421Exception("Cannot execute Pre command");
            }
        } catch (IOException e) {
            try {
                pumpStreamHandler.stop();
            } catch (IOException e1) {
            }
            logger.error("Exception: " + e.getMessage() +
                    "\n    Exec in error with " + commandLine.toString());
            throw new Reply421Exception("Cannot execute Pre command");
        }
        try {
            pumpStreamHandler.stop();
        } catch (IOException e1) {
        }
        if (watchdog != null &&
                watchdog.killedProcess()) {
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
            logger.debug("Status: " + status + (status == -1 ? " Tiemout" : "")
                    + " Exec in error with " +
                    commandLine.toString());
            throw new Reply421Exception("Pre command executed in error");
        }
    }
}
