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
package org.waarp.gateway.kernel.exec;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply421Exception;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpThreadFactory;

/**
 * @author "Frederic Bregier"
 * 
 */
public class JavaExecutor extends AbstractExecutor {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(JavaExecutor.class);
    private final String[] args;
    @SuppressWarnings("unused")
    private final String arg;
    private final WaarpFuture futureCompletion;
    private final long delay;

    /**
     * @param command
     * @param delay
     * @param futureCompletion
     */
    public JavaExecutor(String command, long delay, WaarpFuture futureCompletion) {
        this.args = command.split(" ");
        this.arg = command;
        this.futureCompletion = futureCompletion;
        this.delay = delay;
    }

    @Override
    public void run() throws CommandAbstractException {
        String className = this.args[0];
        GatewayRunnable runnable = null;
        try {
            runnable = (GatewayRunnable) Class.forName(className).newInstance();
        } catch (Exception e) {
            logger.error("ExecJava command is not available: " + className, e);
            throw new Reply421Exception("Pre Exec command is not executable");
        }
        runnable.setArgs(true, useLocalExec,
                (int) this.delay, args);
        logger.debug(className + " " + runnable.getClass().getName());
        int status = -1;
        if (delay <= 0) {
            runnable.run();
            status = runnable.getFinalStatus();
        } else {
            ExecutorService executorService = Executors.newFixedThreadPool(1, new WaarpThreadFactory("JavaExecutor"));
            executorService.execute(runnable);
            try {
                Thread.yield();
                executorService.shutdown();
                if (delay > 100) {
                    if (!executorService.awaitTermination(delay, TimeUnit.MILLISECONDS)) {
                        executorService.shutdownNow();
                        logger.error("Exec is in Time Out");
                        status = -1;
                    } else {
                        status = runnable.getFinalStatus();
                    }
                } else {
                    while (!executorService.awaitTermination(30, TimeUnit.SECONDS))
                        ;
                    status = runnable.getFinalStatus();
                }
            } catch (InterruptedException e) {
                logger.error("Status: " + e.getMessage() + "\n\t Exec in error with " +
                        runnable);
                throw new Reply421Exception("Pre Exec command is not correctly executed");
            }
        }
        if (status == 0) {
            futureCompletion.setSuccess();
            logger.info("Exec OK with {}", runnable);
        } else if (status == 1) {
            logger.warn("Exec in warning with " + runnable);
            futureCompletion.setSuccess();
        } else {
            logger.error("Status: " + status + " Exec in error with " +
                    runnable);
            throw new Reply421Exception("Pre command executed in error");
        }
    }

}
