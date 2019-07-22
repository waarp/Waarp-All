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
package org.waarp.commandexec.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.waarp.commandexec.utils.LocalExecDefaultResult;
import org.waarp.commandexec.utils.LocalExecResult;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

/**
 * Handles a client-side channel for LocalExec
 *
 *
 */
public class LocalExecClientHandler extends SimpleChannelInboundHandler<String> {

    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(LocalExecClientHandler.class);

    protected LocalExecResult result;
    protected StringBuilder back;
    protected boolean firstMessage = true;
    protected WaarpFuture future;
    protected LocalExecClientInitializer factory = null;
    protected long delay;
    protected String command;
    protected Channel channel;
    protected WaarpFuture ready = new WaarpFuture(true);

    /**
     * Constructor
     */
    public LocalExecClientHandler(LocalExecClientInitializer factory) {
        this.factory = factory;
    }

    /**
     * Initialize the client status for a new execution
     * 
     * @param delay
     * @param command
     */
    public void initExecClient(long delay, String command) {
        this.result = new LocalExecResult(LocalExecDefaultResult.NoStatus);
        this.back = new StringBuilder();
        this.firstMessage = true;
        this.future = new WaarpFuture(true);
        this.delay = delay;
        this.command = command;
        // Sends the received line to the server.
        if (channel == null) {
            try {
                ready.await();
            } catch (InterruptedException e) {
            }
        }
        logger.debug("write command: " + this.command);
        try {
            if (this.delay != 0) {
                channel.writeAndFlush(this.delay + " " + this.command + "\n").await(30000);
            } else {
                channel.writeAndFlush(this.command + "\n").await(30000);
            }
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channel = ctx.channel();
        factory.addChannel(channel);
        ready.setSuccess();
        super.channelActive(ctx);
    }

    /**
     * When closed, <br>
     * If no messaged were received => NoMessage error is set to future<br>
     * Else if an error was detected => Set the future to error (with or without exception)<br>
     * Else if no error occurs => Set success to the future<br>
     *
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (future == null || !future.isDone()) {
            // Should not be
            finalizeMessage();
        }
        super.channelInactive(ctx);
    }

    /**
     * Finalize a message
     */
    private void finalizeMessage() {
        if (result == null) {
            if (this.future != null) {
                this.future.cancel();
            }
            return;
        }
        if (firstMessage) {
            result.set(LocalExecDefaultResult.NoMessage);
        } else {
            result.setResult(back.toString());
        }
        if (result.getStatus() < 0) {
            if (result.getException() != null) {
                this.future.setFailure(result.getException());
            } else {
                this.future.cancel();
            }
        } else {
            this.future.setSuccess();
        }
    }

    /**
     * Waiting for the close of the exec
     * 
     * @return The LocalExecResult
     */
    public LocalExecResult waitFor(long delay) {
        if (delay <= 0) {
            this.future.awaitUninterruptibly();
        } else {
            this.future.awaitUninterruptibly(delay);
        }
        result.setSuccess(this.future.isSuccess());
        return result;
    }

    /**
     * Action to do before close
     */
    public void actionBeforeClose(Channel channel) {
        // here nothing to do
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String mesg) throws Exception {
        // Add the line received from the server.
        // If first message, then take the status and then the message
        if (firstMessage) {
            firstMessage = false;
            int pos = mesg.indexOf(' ');
            try {
                result.setStatus(Integer.parseInt(mesg.substring(0, pos)));
            } catch (NumberFormatException e1) {
                // Error
                logger.debug(this.command + ":" + "Bad Transmission: " + mesg + "\n\t" + back.toString());
                result.set(LocalExecDefaultResult.BadTransmition);
                back.append(mesg);
                actionBeforeClose(ctx.channel());
                WaarpSslUtility.closingSslChannel(ctx.channel());
                return;
            }
            mesg = mesg.substring(pos + 1);
            if (mesg.startsWith(LocalExecDefaultResult.ENDOFCOMMAND)) {
                logger.debug(this.command + ":" + "Receive End of Command");
                result.setResult(LocalExecDefaultResult.NoMessage.getResult());
                back.append(result.getResult());
                this.finalizeMessage();
            } else {
                result.setResult(mesg);
                back.append(mesg);
            }
        } else if (mesg.startsWith(LocalExecDefaultResult.ENDOFCOMMAND)) {
            logger.debug(this.command + ":" + "Receive End of Command");
            this.finalizeMessage();
        } else {
            back.append('\n').append(mesg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn(this.command + ":" + "Unexpected exception from Outband while get information: " + firstMessage,
                cause);
        if (firstMessage) {
            firstMessage = false;
            result.set(LocalExecDefaultResult.BadTransmition);
            result.setException((Exception) cause);
            back = new StringBuilder("Error in LocalExec: ").append(result.getException().getMessage()).append('\n');
        } else {
            back.append("\nERROR while receiving answer: ");
            result.setException((Exception) cause);
            back.append(result.getException().getMessage()).append('\n');
        }
        actionBeforeClose(ctx.channel());
        WaarpSslUtility.closingSslChannel(ctx.channel());
    }
}
