/**
   This file is part of Waarp Project.

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All Waarp Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Waarp is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Waarp .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.commandexec.server;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.RejectedExecutionException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.waarp.commandexec.utils.LocalExecDefaultResult;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.common.utility.WaarpStringUtils;

/**
 * Handles a server-side channel for LocalExec.
 *
 *
 */
public class LocalExecServerHandler extends SimpleChannelInboundHandler<String> {
    // Fixed delay, but could change if necessary at construction
    private long delay = LocalExecDefaultResult.MAXWAITPROCESS;
    protected LocalExecServerInitializer factory = null;
    static protected boolean isShutdown = false;

    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(LocalExecServerHandler.class);

    protected volatile boolean answered = false;

    /**
     * Is the Local Exec Server going Shutdown
     * 
     * @param channel
     *            associated channel
     * @return True if in Shutdown
     */
    public static boolean isShutdown(Channel channel) {
        if (isShutdown) {
            channel.writeAndFlush(LocalExecDefaultResult.ConnectionRefused.getStatus() + " "
                    + LocalExecDefaultResult.ConnectionRefused.getResult() + "\n");
            try {
                channel.writeAndFlush(LocalExecDefaultResult.ENDOFCOMMAND + "\n").await(30000);
            } catch (InterruptedException e) {
            }
            WaarpSslUtility.closingSslChannel(channel);
            return true;
        }
        return false;
    }

    public static void junitSetNotShutdown() {
        isShutdown = false;
    }

    /**
     * Print stack trace
     * 
     * @param thread
     * @param stacks
     */
    static private void printStackTrace(Thread thread, StackTraceElement[] stacks) {
        System.err.print(thread.toString() + " : ");
        for (int i = 0; i < stacks.length - 1; i++) {
            System.err.print(stacks[i].toString() + " ");
        }
        if (stacks.length > 0) {
          System.err.println(stacks[stacks.length - 1].toString());
        } else {
          System.err.println();
        }
    }

    /**
     * Shutdown thread
     * 
     * @author Frederic Bregier
     *
     */
    private static class GGLEThreadShutdown extends Thread {
        long delay = 3000;
        LocalExecServerInitializer factory;

        public GGLEThreadShutdown(LocalExecServerInitializer factory) {
            this.factory = factory;
        }

        @Override
        public void run() {
            Timer timer = null;
            timer = new Timer(true);
            GGLETimerTask ggleTimerTask = new GGLETimerTask();
            timer.schedule(ggleTimerTask, delay);
            factory.releaseResources();
            DetectionUtils.SystemExit(0);
        }

    }

    /**
     * TimerTask to terminate the server
     * 
     * @author Frederic Bregier
     *
     */
    private static class GGLETimerTask extends TimerTask {
        /**
         * Internal Logger
         */
        private static final WaarpLogger logger = WaarpLoggerFactory
                .getLogger(GGLETimerTask.class);

        @Override
        public void run() {
            logger.error("System will force EXIT");
            Map<Thread, StackTraceElement[]> map = Thread
                    .getAllStackTraces();
            for (Thread thread : map.keySet()) {
                printStackTrace(thread, map.get(thread));
            }
            DetectionUtils.SystemExit(0);
        }
    }

    /**
     * Constructor with a specific delay
     * 
     * @param newdelay
     */
    public LocalExecServerHandler(LocalExecServerInitializer factory, long newdelay) {
        this.factory = factory;
        delay = newdelay;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (isShutdown(ctx.channel())) {
            answered = true;
            return;
        }
        answered = false;
        factory.addChannel(ctx.channel());
    }

    /**
     * Change the delay to the specific value. Need to be called before any receive message.
     * 
     * @param newdelay
     */
    public void setNewDelay(long newdelay) {
        delay = newdelay;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        answered = false;
        String request = msg;

        // Generate and write a response.
        String response;
        response = LocalExecDefaultResult.NoStatus.getStatus() + " " +
                LocalExecDefaultResult.NoStatus.getResult();
        ExecuteWatchdog watchdog = null;
        try {
            if (request.length() == 0) {
                // No command
                response = LocalExecDefaultResult.NoCommand.getStatus() + " " +
                        LocalExecDefaultResult.NoCommand.getResult();
            } else {
                String[] args = request.split(" ");
                int cpt = 0;
                long tempDelay;
                try {
                    tempDelay = Long.parseLong(args[0]);
                    cpt++;
                } catch (NumberFormatException e) {
                    tempDelay = delay;
                }
                if (tempDelay < 0) {
                    // Shutdown Order
                    isShutdown = true;
                    logger.warn("Shutdown order received");
                    response = LocalExecDefaultResult.ShutdownOnGoing.getStatus() + " "
                            + LocalExecDefaultResult.ShutdownOnGoing.getResult();
                    Thread thread = new GGLEThreadShutdown(factory);
                    thread.start();
                    return;
                }
                String binary = args[cpt++];
                File exec = new File(binary);
                if (exec.isAbsolute()) {
                    // If true file, is it executable
                    if (!exec.canExecute()) {
                        logger.error("Exec command is not executable: " + request);
                        response = LocalExecDefaultResult.NotExecutable.getStatus() + " " +
                                LocalExecDefaultResult.NotExecutable.getResult();
                        return;
                    }
                }
                // Create command with parameters
                CommandLine commandLine = new CommandLine(binary);
                for (; cpt < args.length; cpt++) {
                    commandLine.addArgument(args[cpt]);
                }
                DefaultExecutor defaultExecutor = new DefaultExecutor();
                ByteArrayOutputStream outputStream;
                outputStream = new ByteArrayOutputStream();
                PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(outputStream);
                defaultExecutor.setStreamHandler(pumpStreamHandler);
                int[] correctValues = { 0, 1 };
                defaultExecutor.setExitValues(correctValues);
                if (tempDelay > 0) {
                    // If delay (max time), then setup Watchdog
                    watchdog = new ExecuteWatchdog(tempDelay);
                    defaultExecutor.setWatchdog(watchdog);
                }
                int status = -1;
                try {
                    // Execute the command
                    status = defaultExecutor.execute(commandLine);
                } catch (ExecuteException e) {
                    if (e.getExitValue() == -559038737) {
                        // Cannot run immediately so retry once
                        try {
                            Thread.sleep(LocalExecDefaultResult.RETRYINMS);
                        } catch (InterruptedException e1) {
                        }
                        try {
                            status = defaultExecutor.execute(commandLine);
                        } catch (ExecuteException e1) {
                            try {
                                pumpStreamHandler.stop();
                            } catch (IOException e3) {
                            }
                            logger.error("Exception: " + e.getMessage() +
                                    " Exec in error with " + commandLine.toString());
                            response = LocalExecDefaultResult.BadExecution.getStatus() + " " +
                                    LocalExecDefaultResult.BadExecution.getResult();
                            try {
                                outputStream.close();
                            } catch (IOException e2) {
                            }
                            return;
                        } catch (IOException e1) {
                            try {
                                pumpStreamHandler.stop();
                            } catch (IOException e3) {
                            }
                            logger.error("Exception: " + e.getMessage() +
                                    " Exec in error with " + commandLine.toString());
                            response = LocalExecDefaultResult.BadExecution.getStatus() + " " +
                                    LocalExecDefaultResult.BadExecution.getResult();
                            try {
                                outputStream.close();
                            } catch (IOException e2) {
                            }
                            return;
                        }
                    } else {
                        try {
                            pumpStreamHandler.stop();
                        } catch (IOException e3) {
                        }
                        logger.error("Exception: " + e.getMessage() +
                                " Exec in error with " + commandLine.toString());
                        response = LocalExecDefaultResult.BadExecution.getStatus() + " " +
                                LocalExecDefaultResult.BadExecution.getResult();
                        try {
                            outputStream.close();
                        } catch (IOException e2) {
                        }
                        return;
                    }
                } catch (IOException e) {
                    try {
                        pumpStreamHandler.stop();
                    } catch (IOException e3) {
                    }
                    logger.error("Exception: " + e.getMessage() +
                            " Exec in error with " + commandLine.toString());
                    response = LocalExecDefaultResult.BadExecution.getStatus() + " " +
                            LocalExecDefaultResult.BadExecution.getResult();
                    try {
                        outputStream.close();
                    } catch (IOException e2) {
                    }
                    return;
                }
                try {
                    pumpStreamHandler.stop();
                } catch (IOException e3) {
                }
                if (defaultExecutor.isFailure(status) && watchdog != null &&
                        watchdog.killedProcess()) {
                    // kill by the watchdoc (time out)
                    logger.error("Exec is in Time Out");
                    response = LocalExecDefaultResult.TimeOutExecution.getStatus() + " " +
                            LocalExecDefaultResult.TimeOutExecution.getResult();
                    try {
                        outputStream.close();
                    } catch (IOException e2) {
                    }
                } else {
                    try {
                        response = status + " " + outputStream.toString(WaarpStringUtils.UTF8.name());
                    } catch (UnsupportedEncodingException e) {
                        response = status + " " + outputStream.toString();
                    }
                    try {
                        outputStream.close();
                    } catch (IOException e2) {
                    }
                }
            }
        } finally {
            // We do not need to write a ByteBuf here.
            // We know the encoder inserted at LocalExecInitializer will do the
            // conversion.
            ctx.channel().writeAndFlush(response + "\n");
            answered = true;
            if (watchdog != null) {
                watchdog.stop();
            }
            logger.info("End of Command: " + request + " : " + response);
            ctx.channel().writeAndFlush(LocalExecDefaultResult.ENDOFCOMMAND + "\n");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (!answered) {
            logger.error("Unexpected exception from Outband while not answered.", cause);
        }
        Throwable e1 = cause;
        // Look if Nothing to do since execution will stop later on and
        // an error will occur on client side
        // since no message arrived before close (or partially)
        if (e1 instanceof CancelledKeyException) {
        } else if (e1 instanceof ClosedChannelException) {
        } else if (e1 instanceof NullPointerException) {
            if (ctx.channel().isActive()) {
                if (answered) {
                    logger.debug("Exception while answered: ", cause);
                }
                WaarpSslUtility.closingSslChannel(ctx.channel());
            }
        } else if (e1 instanceof IOException) {
            if (ctx.channel().isActive()) {
                if (answered) {
                    logger.debug("Exception while answered: ", cause);
                }
                WaarpSslUtility.closingSslChannel(ctx.channel());
            }
        } else if (e1 instanceof RejectedExecutionException) {
            if (ctx.channel().isActive()) {
                if (answered) {
                    logger.debug("Exception while answered: ", cause);
                }
                WaarpSslUtility.closingSslChannel(ctx.channel());
            }
        }
    }
}
