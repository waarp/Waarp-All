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
package org.waarp.ftp.core.data;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.waarp.common.command.ReplyCode;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply425Exception;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.future.WaarpChannelFuture;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.ftp.core.command.FtpCommandCode;
import org.waarp.ftp.core.command.service.ABOR;
import org.waarp.ftp.core.config.FtpConfiguration;
import org.waarp.ftp.core.config.FtpInternalConfiguration;
import org.waarp.ftp.core.control.NetworkHandler;
import org.waarp.ftp.core.data.handler.DataNetworkHandler;
import org.waarp.ftp.core.exception.FtpNoConnectionException;
import org.waarp.ftp.core.exception.FtpNoFileException;
import org.waarp.ftp.core.exception.FtpNoTransferException;
import org.waarp.ftp.core.file.FtpFile;
import org.waarp.ftp.core.session.FtpSession;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main class that handles transfers and their execution
 */
public class FtpTransferControl {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpTransferControl.class);

  /**
   * SessionInterface
   */
  private final FtpSession session;

  /**
   * Step in order to wait that the DataNetworkHandler is ready
   */
  private final AtomicBoolean isDataNetworkHandlerReady =
      new AtomicBoolean(false);

  /**
   * The associated DataChannel
   */
  private Channel dataChannel;

  /**
   * Waiter for the dataChannel to be opened
   */
  private WaarpChannelFuture waitForOpenedDataChannel =
      new WaarpChannelFuture(true);

  /**
   * Is the current Command Finished (or previously current command)
   */
  private final AtomicBoolean isExecutingCommandFinished =
      new AtomicBoolean(true);
  /**
   * Waiter for the Command to be setup
   */
  private WaarpFuture commandSetup;

  /**
   * Waiter for the Command finishing
   */
  private WaarpFuture commandFinishing;

  /**
   * Current command executed
   */
  private FtpTransfer executingCommand;

  /**
   * Thread pool for execution of transfer command
   */
  private ExecutorService executorService;

  /**
   * Blocking step for the Executor in order to wait for the end of the
   * command
   * (internal wait, not to be used
   * outside).
   */
  private WaarpFuture endOfCommand;

  /**
   * A boolean to know if Check was called once
   */
  private final AtomicBoolean isCheckAlreadyCalled = new AtomicBoolean(false);

  /**
   * @param session
   */
  public FtpTransferControl(final FtpSession session) {
    this.session = session;
    endOfCommand = null;
  }

  // XXX DataNetworkHandler functions

  /**
   * The DataNetworkHandler is ready (from setNewFtpExecuteTransfer)
   */
  private void setDataNetworkHandlerReady() {
    isCheckAlreadyCalled.set(false);
    isDataNetworkHandlerReady.set(true);
  }

  /**
   * Wait for the DataNetworkHandler to be ready (from trueRetrieve of {@link
   * FtpFile})
   *
   * @throws InterruptedException
   */
  public final void waitForDataNetworkHandlerReady()
      throws InterruptedException {
    final AtomicBoolean isDNHReady = isDataNetworkHandlerReady;
    if (!isDNHReady.get()) {
      for (int i = 0; i < FtpInternalConfiguration.RETRYNB * 10; i++) {
        Thread.sleep(FtpInternalConfiguration.RETRYINMS * 10);
        if (isDNHReady.get()) {
          return;
        }
      }
      if (!isDNHReady.get()) {
        throw new InterruptedException("Bad initialization");
      }
    }
  }

  /**
   * Set the new opened Channel (from channelConnected of {@link
   * DataNetworkHandler})
   *
   * @param channel
   * @param dataNetworkHandler
   */
  public final void setOpenedDataChannel(final Channel channel,
                                         final DataNetworkHandler dataNetworkHandler) {
    logger.debug("SetOpenedDataChannel: {}",
                 (channel != null? channel.remoteAddress() : "no channel"));
    final WaarpChannelFuture wait = waitForOpenedDataChannel;
    if (channel != null) {
      session.getDataConn().setDataNetworkHandler(dataNetworkHandler);
      wait.setChannel(channel);
      wait.setSuccess();
    } else {
      wait.cancel();
    }
  }

  /**
   * Wait that the new opened connection is ready (same method in {@link
   * FtpDataAsyncConn} from openConnection)
   *
   * @return the new opened Channel
   *
   * @throws InterruptedException
   */
  public final Channel waitForOpenedDataChannel() throws InterruptedException {
    Channel channel = null;
    final WaarpChannelFuture wait = waitForOpenedDataChannel;
    if (wait.awaitOrInterruptible(FtpConfiguration.getDataTimeoutCon())) {
      if (wait.isSuccess()) {
        channel = wait.channel();
      } else {
        logger.warn("data connection is in error");
        throw new InterruptedException("Cannot get data connection");
      }
      return channel;
    } else {
      wait.cancel();
      final boolean isPassive = session.getDataConn().isPassiveMode();
      final InetSocketAddress address =
          isPassive? session.getDataConn().getLocalAddress() :
              session.getDataConn().getRemoteAddress();
      logger.error(
          "Timeout occurs during data {} connection to {} after timeout {}",
          isPassive? "Passive" : "Active", address,
          FtpConfiguration.getDataTimeoutCon());
      throw new InterruptedException(
          "Cannot get data connection to " + address);
    }
  }

  /**
   * Allow to reset the waitForOpenedDataChannel
   */
  public final synchronized void resetWaitForOpenedDataChannel() {
    final WaarpChannelFuture wait = waitForOpenedDataChannel;
    if (wait != null) {
      wait.setSuccess();
    }
    waitForOpenedDataChannel = new WaarpChannelFuture(true);
  }

  private void removeSession() {
    if (session.getDataConn().isPassiveMode()) {
      session.getConfiguration().delFtpSession(
          session.getDataConn().getRemoteAddress().getAddress(),
          session.getDataConn().getLocalAddress());
    }
    if (waitForOpenedDataChannel != null) {
      resetWaitForOpenedDataChannel();
    }
  }

  /**
   * Wait for the client to be connected (Passive) or Wait for the server to
   * be
   * connected to the client (Active)
   *
   * @throws Reply425Exception
   */
  public final synchronized void openDataConnection() throws Reply425Exception {
    // Prepare this Data channel to be closed ;-)
    // In fact, prepare the future close op which should occur since it is
    // now opened
    final WaarpFuture commandSetupFuture = commandSetup;
    if (commandSetupFuture != null && !commandSetupFuture.isDone()) {
      commandSetupFuture.cancel();
    }
    commandSetup = new WaarpFuture(true);
    final FtpDataAsyncConn dataAsyncConn = session.getDataConn();
    if (!dataAsyncConn.isStreamFile()) {
      // FIXME isActive or isDNHReady ?
      if (dataAsyncConn.isActive()) {
        // Already connected
        logger.debug("Connection already open");
        session.setReplyCode(ReplyCode.REPLY_125_DATA_CONNECTION_ALREADY_OPEN,
                             dataAsyncConn.getType().name() +
                             " mode data connection already open");
        return;
      }
    } else {
      // Stream, Data Connection should not be opened
      if (dataAsyncConn.isActive()) {
        logger.error(
            "Connection already open but should not since in Stream mode");
        setTransferAbortedFromInternal(false);
        throw new Reply425Exception(
            "Connection already open but should not since in Stream mode");
      }
    }
    // Need to open connection
    session.setReplyCode(ReplyCode.REPLY_150_FILE_STATUS_OKAY,
                         "Opening " + dataAsyncConn.getType().name() +
                         " mode data connection");
    if (dataAsyncConn.isPassiveMode()) {
      if (!dataAsyncConn.isBind()) {
        // No passive connection prepared
        removeSession();
        throw new Reply425Exception("No passive data connection prepared");
      }
      // Wait for the connection to be done by the client
      logger.debug("Passive mode standby");
      try {
        dataChannel = waitForOpenedDataChannel();
        dataAsyncConn.setNewOpenedDataChannel(dataChannel);
      } catch (final InterruptedException e) {//NOSONAR
        logger.warn("Connection abort in passive mode: {}", e.getMessage());
        removeSession();
        // Cannot open connection
        throw new Reply425Exception("Cannot open passive data connection");
      }
      logger.debug("Passive mode connected");
    } else {
      // Wait for the server to be connected to the client
      final InetSocketAddress inetSocketAddress =
          dataAsyncConn.getRemoteAddress();
      final InetSocketAddress localAddress = dataAsyncConn.getLocalAddress();
      logger.debug("Active mode standby");
      final Bootstrap bootstrap =
          session.getConfiguration().getFtpInternalConfiguration()
                 .getActiveBootstrap(session.isDataSsl());
      final String mylog = session.toString();
      logger.debug("DataConn for: {} to {}",
                   session.getCurrentCommand().getCommand(), inetSocketAddress);
      ChannelFuture future = null;
      for (int i = 0; i < FtpInternalConfiguration.RETRYNB; i++) {
        future =
            activeInitConnection(bootstrap, inetSocketAddress, localAddress);
        if (future.isSuccess()) {
          break;
        }
        try {
          Thread.sleep(FtpInternalConfiguration.RETRYINMS * 10); //NOSONAR
        } catch (final InterruptedException e) { // NOSONAR
          // ignore
        }
      }
      if (future == null || !future.isSuccess()) {
        logger.warn(
            "Connection abort in active mode from future while session: " +
            "{}\nTrying connect to: {} From: {}\nWas: {}", session,
            inetSocketAddress, localAddress, mylog,
            future != null? future.cause() : null);
        // Cannot open connection
        throw new Reply425Exception("Cannot open active data connection");
      }
      try {
        dataChannel = waitForOpenedDataChannel();
        dataAsyncConn.setNewOpenedDataChannel(dataChannel);
      } catch (final InterruptedException e) {//NOSONAR
        WaarpSslUtility.closingSslChannel(future.channel())
                       .awaitUninterruptibly();
        logger.warn("Connection abort in active mode ({})", e.getMessage());
        // Cannot open connection
        throw new Reply425Exception("Cannot open active data connection");
      }
      logger.debug("Active mode connected");
    }
    if (dataChannel == null) {
      // Cannot have a new Data connection since shutdown
      throw new Reply425Exception("Cannot open data connection, shutting down");
    }
  }

  private ChannelFuture activeInitConnection(final Bootstrap bootstrap,
                                             final InetSocketAddress inetSocketAddress,
                                             final InetSocketAddress localAddress) {
    final ChannelFuture future =
        bootstrap.connect(inetSocketAddress, localAddress);
    // Set the session for the future dataChannel
    future.addListener(new ChannelFutureListener() {
      @Override
      public void operationComplete(final ChannelFuture channelFuture) {
        if (future.isSuccess()) {
          future.channel().attr(FtpConfiguration.FTP_SESSION_ATTRIBUTE_KEY)
                .set(session);
        }
      }
    });
    try {
      future.await(session.getConfiguration().getTimeoutCon());
    } catch (final InterruptedException e1) {//NOSONAR
      SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
    }
    return future;
  }

  // XXX FtpTransfer functions

  /**
   * Run the command from an executor
   */
  private void runExecutor() {
    final WaarpChannelFuture wait = waitForOpenedDataChannel;
    endOfCommand = new WaarpFuture(true);
    final WaarpFuture commandFinishingFuture = commandFinishing;
    final WaarpFuture endCommandFuture = endOfCommand;
    try {
      session.getDataConn().getDataNetworkHandler()
             .setFtpTransfer(executingCommand);
    } catch (final FtpNoConnectionException ignored) {
      // nothing
    }
    for (int i = 0; i < FtpInternalConfiguration.RETRYNB; i++) {
      if (wait.awaitOrInterruptible(FtpConfiguration.getDataTimeoutCon())) {
        break;
      }
    }
    if (!wait.awaitOrInterruptible(FtpConfiguration.getDataTimeoutCon()) &&
        !wait.isDone()) {
      wait.cancel();
      if (!commandFinishingFuture.isDone()) {
        logger.error("Execution time out {}",
                     FtpConfiguration.getDataTimeoutCon());
        commandFinishingFuture.cancel();
      }
      if (!endCommandFuture.isDone()) {
        logger.error("Command cancelled");
        endCommandFuture.cancel();
      }
      return;
    }
    if (wait != null && (!wait.isSuccess() || wait.channel() == null)) {
      if (!commandFinishingFuture.isDone()) {
        logger.error("Connection aborted");
        commandFinishingFuture.cancel();
      }
      if (!endCommandFuture.isDone()) {
        logger.error("Command cancelled");
        endCommandFuture.cancel();
      }
      return;
    }
    wait.channel().config().setAutoRead(true);
    // Run the command
    if (executorService == null) {
      executorService = Executors.newSingleThreadExecutor();
    }
    executorService.execute(new FtpTransferExecutor(session, executingCommand));
    commandFinishingFuture.awaitOrInterruptible();
    if (commandFinishingFuture.isFailed()) {
      endCommandFuture.cancel();
    }
  }

  /**
   * Add a new transfer to be executed. This is to be called from Command
   * after
   * connection is opened and before
   * answering to the client that command is ready to be executed (for Store
   * or
   * Retrieve like operations).
   *
   * @param command
   * @param file
   */
  public final void setNewFtpTransfer(final FtpCommandCode command,
                                      final FtpFile file) {
    final WaarpChannelFuture wait = waitForOpenedDataChannel;
    isExecutingCommandFinished.set(false);
    commandFinishing = new WaarpFuture(true);
    logger.debug("setNewCommand: {}", command);
    setDataNetworkHandlerReady();
    executingCommand = new FtpTransfer(command, file);
    runExecutor();
    commandFinishing = null;
    commandSetup.setSuccess();
    if (!session.getDataConn().isStreamFile()) {
      wait.channel().config().setAutoRead(false);
    }
  }

  /**
   * Add a new transfer to be executed. This is to be called from Command
   * after
   * connection is opened and before
   * answering to the client that command is ready to be executed (for List
   * like
   * operations).
   *
   * @param command
   * @param list
   * @param path as Original Path
   */
  public final void setNewFtpTransfer(final FtpCommandCode command,
                                      final List<String> list,
                                      final String path) {
    final WaarpChannelFuture wait = waitForOpenedDataChannel;
    isExecutingCommandFinished.set(false);
    commandFinishing = new WaarpFuture(true);
    logger.debug("setNewCommand: {}", command);
    setDataNetworkHandlerReady();
    executingCommand = new FtpTransfer(command, list, path);
    runExecutor();
    commandFinishing = null;
    commandSetup.setSuccess();
    if (!session.getDataConn().isStreamFile()) {
      wait.channel().config().setAutoRead(false);
    }
    try {
      session.getDataConn().getDataNetworkHandler().setFtpTransfer(null);
    } catch (final FtpNoConnectionException ignored) {
      // nothing
    }
  }

  /**
   * @return True if transfer is not yet finished
   */
  public final boolean waitFtpTransferExecuting() {
    boolean notFinished = true;
    final WaarpFuture commandFinishingFuture = commandFinishing;
    final AtomicBoolean commandFinished = isExecutingCommandFinished;
    for (int i = 0; i < FtpInternalConfiguration.RETRYNB * 100; i++) {
      if (commandFinished.get() || commandFinishingFuture == null ||
          session.isCurrentCommandFinished() ||
          commandFinishingFuture != null &&
          commandFinishingFuture.awaitOrInterruptible(
              FtpInternalConfiguration.RETRYINMS)) {
        notFinished = false;
        break;
      }
    }
    return notFinished;
  }

  /**
   * Is a command currently executing (called from {@link NetworkHandler} when
   * a
   * message is received to see if
   * another transfer command is already in execution, which is not allowed)
   *
   * @return True if a command is currently executing
   */
  public final boolean isFtpTransferExecuting() {
    return !isExecutingCommandFinished.get();
  }

  /**
   * @return the current executing FtpTransfer
   *
   * @throws FtpNoTransferException
   */
  public final FtpTransfer getExecutingFtpTransfer()
      throws FtpNoTransferException {
    for (int i = 0; i < 100; i++) {
      if (executingCommand != null) {
        return executingCommand;
      }
      try {
        Thread.sleep(WaarpNettyUtil.MINIMAL_DELAY_MS);
      } catch (final InterruptedException e1) {//NOSONAR
        throw new FtpNoTransferException("No Command currently running", e1);
      }
    }

    throw new FtpNoTransferException("No Command currently running");
  }

  /**
   * @return True if the current FtpTransfer is a Retrieve like transfer
   *
   * @throws FtpNoTransferException
   * @throws CommandAbstractException
   * @throws FtpNoFileException
   */
  boolean isExecutingRetrLikeTransfer()
      throws FtpNoTransferException, CommandAbstractException,
             FtpNoFileException {
    return !session.isCurrentCommandFinished() &&
           FtpCommandCode.isRetrLikeCommand(
               getExecutingFtpTransfer().getCommand()) &&
           getExecutingFtpTransfer().getFtpFile().isInReading();
  }

  /**
   * Called when a transfer is finished from setEndOfTransfer
   *
   * @return True if it was already called before
   *
   * @throws FtpNoTransferException
   */
  private boolean checkFtpTransferStatus() throws FtpNoTransferException {
    final WaarpFuture commandFinishingFuture = commandFinishing;
    final AtomicBoolean commandFinished = isExecutingCommandFinished;
    final AtomicBoolean isDNHReady = isDataNetworkHandlerReady;
    if (isCheckAlreadyCalled.get()) {
      logger.warn("Check: ALREADY CALLED");
      return true;
    }
    for (int i = 0; i < 10; i++) {
      if (!commandFinished.get()) {
        break;
      }
      try {
        Thread.sleep(WaarpNettyUtil.MINIMAL_DELAY_MS);
      } catch (InterruptedException e) {//NOSONAR
        throw new FtpNoTransferException("No transfer running and Interrupted",
                                         e);
      }
    }
    if (commandFinished.get()) {
      // already done
      logger.warn("Check: already Finished");
      if (commandFinishingFuture != null && !commandFinishingFuture.isDone()) {
        commandFinishingFuture.cancel();
      }
      throw new FtpNoTransferException("No transfer running");
    }
    for (int i = 0; i < 10; i++) {
      if (isDNHReady.get()) {
        break;
      }
      try {
        Thread.sleep(WaarpNettyUtil.MINIMAL_DELAY_MS);
      } catch (InterruptedException e) {//NOSONAR
        throw new FtpNoTransferException("No connection and Interrupted", e);
      }
    }
    if (!isDNHReady.get()) {
      // already done
      logger.warn("Check: already DNH not ready");
      throw new FtpNoTransferException("No connection");
    }
    isCheckAlreadyCalled.set(true);
    final FtpTransfer executedTransfer = getExecutingFtpTransfer();
    logger.debug("Check: command {}", executedTransfer.getCommand());
    // DNH is ready and Transfer is running
    if (FtpCommandCode.isListLikeCommand(executedTransfer.getCommand())) {
      if (executedTransfer.getStatus()) {
        // Special status for List Like command
        logger.debug("Check: List OK");
        closeTransfer();
        return false;
      }
      logger.info("Check: List Ko");
      abortTransfer();
      return false;
    } else if (FtpCommandCode.isRetrLikeCommand(
        executedTransfer.getCommand())) {
      final FtpFile file;
      try {
        file = executedTransfer.getFtpFile();
      } catch (final FtpNoFileException e) {
        logger.info("Check: Retr no FtpFile for Retr");
        abortTransfer();
        return false;
      }
      try {
        if (file.isInReading()) {
          logger.info("Check: Retr FtpFile still in reading KO");
          abortTransfer();
        } else {
          logger.debug("Check: Retr FtpFile no more in reading OK");
          closeTransfer();
        }
      } catch (final CommandAbstractException e) {
        logger.warn("Retr Test is in Reading problem : {}", e.getMessage());
        closeTransfer();
      }
      return false;
    } else if (FtpCommandCode.isStoreLikeCommand(
        executedTransfer.getCommand())) {
      closeTransfer();
      return false;
    } else {
      logger.warn("Check: Unknown command");
      abortTransfer();
    }
    return false;
  }

  /**
   * Abort the current transfer
   */
  private void abortTransfer() {
    logger.debug("Will abort transfer and write: ",
                 new Exception("trace only"));
    final FtpFile file;
    FtpTransfer current = null;
    try {
      current = getExecutingFtpTransfer();
      file = current.getFtpFile();
      file.abortFile();
    } catch (final FtpNoTransferException e) {
      logger.info("Warning Abort problem in {} : {}", session, e.getMessage());
    } catch (final FtpNoFileException ignored) {
      // nothing
    } catch (final CommandAbstractException e) {
      logger.warn("Abort problem in {} : {}", session, e.getMessage());
    }
    if (current != null) {
      current.setStatus(false);
    }
    endDataConnection();
    session.setReplyCode(ReplyCode.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED,
                         "Transfer aborted for " +
                         (current == null? "Unknown command" :
                             current.toString()));
    if (current != null &&
        !FtpCommandCode.isListLikeCommand(current.getCommand())) {
      try {
        session.getBusinessHandler().afterTransferDoneBeforeAnswer(current);
      } catch (final CommandAbstractException e) {
        session.setReplyCode(e);
      }
    }
    finalizeExecution();
  }

  /**
   * Finish correctly a transfer
   */
  private void closeTransfer() {
    logger.debug("Will close transfer");
    final FtpFile file;
    FtpTransfer current = null;
    try {
      current = getExecutingFtpTransfer();
      file = current.getFtpFile();
      file.closeFile();
    } catch (final FtpNoTransferException e) {
      logger.warn("Close problem" + " : {}", e.getMessage());
    } catch (final FtpNoFileException ignored) {
      // nothing
    } catch (final CommandAbstractException e) {
      logger.warn("Close problem" + " : {}", e.getMessage());
    }
    if (current != null) {
      current.setStatus(true);
    }
    if (session.getDataConn().isStreamFile()) {
      endDataConnection();
    }
    session.setReplyCode(ReplyCode.REPLY_226_CLOSING_DATA_CONNECTION,
                         "Transfer complete for " +
                         (current == null? "Unknown command" :
                             current.toString()));
    if (current != null) {
      if (!FtpCommandCode.isListLikeCommand(current.getCommand())) {
        try {
          session.getBusinessHandler().afterTransferDoneBeforeAnswer(current);
        } catch (final CommandAbstractException e) {
          session.setReplyCode(e);
        }
      } else {
        // Special wait to prevent fast LIST following by STOR or RETR command
        try {
          Thread.sleep(FtpInternalConfiguration.RETRYINMS);
        } catch (final InterruptedException e) {//NOSONAR
          SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
      }
    }
    finalizeExecution();
  }

  /**
   * Set the current transfer as finished. Called from {@link
   * FtpTransferExecutor} when a transfer is over.
   */
  public final void setEndOfTransfer() {
    try {
      checkFtpTransferStatus();
    } catch (final FtpNoTransferException ignored) {
      // nothing
    }
  }

  /**
   * To enable abort from internal error
   *
   * @param write True means the message is write back to the control
   *     command, false it is only prepared
   */
  public final void setTransferAbortedFromInternal(final boolean write) {
    logger.debug("Set transfer aborted internal {}", write);
    abortTransfer();
    if (write && session.getNetworkHandler() != null) {
      session.getNetworkHandler().writeIntermediateAnswer();
    }
    if (endOfCommand != null && !endOfCommand.isDone()) {
      logger.warn("Command cancelled");
      logger.debug(new Exception("TRACE"));
      endOfCommand.cancel();
    }
  }

  /**
   * Called by channelClosed (from {@link DataNetworkHandler} ) or
   * trueRetrieve
   * (from {@link FtpFile}) when the
   * transfer is over
   */
  public final void setPreEndOfTransfer() {
    if (endOfCommand != null) {
      endOfCommand.setSuccess();
      logger.debug("Transfer completed");
    }
  }

  /**
   * Wait for the current transfer to finish, called from {@link
   * FtpTransferExecutor}
   *
   * @throws InterruptedException
   */
  public final void waitForEndOfTransfer() throws InterruptedException {
    if (endOfCommand != null) {
      endOfCommand.awaitOrInterruptible();
      if (endOfCommand.isFailed()) {
        logger.error("Transfer aborted");
        logger.debug(new Exception("TRACE"));
        throw new InterruptedException("Transfer aborted");
      }
    }
  }

  /**
   * Finalize execution
   */
  private void finalizeExecution() {
    if (commandFinishing != null) {
      commandFinishing.setSuccess();
    }
    isExecutingCommandFinished.set(true);
    executingCommand = null;
    resetWaitForOpenedDataChannel();
  }

  /**
   * End the data connection if any
   */
  private synchronized void endDataConnection() {
    logger.debug("End Data connection");
    if (isDataNetworkHandlerReady.get() && dataChannel != null) {
      WaarpNettyUtil.awaitOrInterrupted(
          WaarpSslUtility.closingSslChannel(dataChannel));
      isDataNetworkHandlerReady.set(false);
      dataChannel = null;
    } else if (dataChannel != null) {
      WaarpSslUtility.closingSslChannel(dataChannel);
      isDataNetworkHandlerReady.set(false);
      dataChannel = null;
    }
    isDataNetworkHandlerReady.set(false);
  }

  /**
   * Clear the FtpTransferControl (called when the data connection must be
   * over like from clear of
   * {@link FtpDataAsyncConn}, abort from {@link ABOR} or ending control
   * connection from {@link NetworkHandler}.
   */
  public final void clear() {
    final WaarpFuture commandSetupFuture = commandSetup;
    endDataConnection();
    finalizeExecution();
    if (endOfCommand != null && !endOfCommand.isDone()) {
      logger.error("Command cancelled");
      endOfCommand.cancel();
    }
    final WaarpChannelFuture wait = waitForOpenedDataChannel;
    if (wait != null && !wait.isDone()) {
      wait.cancel();
    }
    if (commandSetupFuture != null && !commandSetupFuture.isDone()) {
      commandSetupFuture.cancel();
    }
    if (executorService != null) {
      executorService.shutdownNow();
      executorService = null;
    }
  }
}
