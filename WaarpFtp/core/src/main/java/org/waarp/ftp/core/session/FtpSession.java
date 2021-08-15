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
package org.waarp.ftp.core.session;

import io.netty.channel.Channel;
import org.waarp.common.command.CommandInterface;
import org.waarp.common.command.ReplyCode;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply425Exception;
import org.waarp.common.file.FileParameterInterface;
import org.waarp.common.file.Restart;
import org.waarp.common.file.SessionInterface;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.ftp.core.command.AbstractCommand;
import org.waarp.ftp.core.command.FtpArgumentCode;
import org.waarp.ftp.core.command.FtpArgumentCode.TransferSubType;
import org.waarp.ftp.core.command.internal.ConnectionCommand;
import org.waarp.ftp.core.config.FtpConfiguration;
import org.waarp.ftp.core.control.BusinessHandler;
import org.waarp.ftp.core.control.NetworkHandler;
import org.waarp.ftp.core.data.FtpDataAsyncConn;
import org.waarp.ftp.core.exception.FtpNoConnectionException;
import org.waarp.ftp.core.file.FtpAuth;
import org.waarp.ftp.core.file.FtpDir;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main class that stores any information that must be accessible from anywhere
 * during the connection of one
 * user.
 */
public class FtpSession implements SessionInterface {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpSession.class);
  /**
   * Business Handler
   */
  private final BusinessHandler businessHandler;

  /**
   * Associated global configuration
   */
  private final FtpConfiguration configuration;

  /**
   * Associated Binary connection
   */
  private FtpDataAsyncConn dataConn;

  /**
   * Ftp Authentication
   */
  private FtpAuth ftpAuth;

  /**
   * Ftp DirInterface configuration and access
   */
  private FtpDir ftpDir;

  /**
   * Previous Command
   */
  private AbstractCommand previousCommand;

  /**
   * Current Command
   */
  private AbstractCommand currentCommand;
  /**
   * Is the current command finished
   */
  private final AtomicBoolean isCurrentCommandFinished =
      new AtomicBoolean(true);

  /**
   * Associated Reply Code
   */
  private ReplyCode replyCode;

  /**
   * Real text for answer
   */
  private String answer;

  /**
   * Current Restart information
   */
  private Restart restart;

  /**
   * Is the control ready to accept command
   */
  private final WaarpFuture isReady = new WaarpFuture(true);

  /**
   * Is the current session using SSL on Control
   */
  private final AtomicBoolean isSsl = new AtomicBoolean(false);
  /**
   * Is the current session will using SSL on Control
   */
  private WaarpFuture waitForSsl;
  /**
   * WIll all data be using SSL
   */
  private final AtomicBoolean isDataSsl = new AtomicBoolean(false);

  /**
   * Constructor
   *
   * @param configuration
   * @param handler
   */
  public FtpSession(final FtpConfiguration configuration,
                    final BusinessHandler handler) {
    this.configuration = configuration;
    businessHandler = handler;
  }

  /**
   * @return the businessHandler
   */
  public final BusinessHandler getBusinessHandler() {
    return businessHandler;
  }

  /**
   * Get the configuration
   *
   * @return the configuration
   */
  public final FtpConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public final FtpDir getDir() {
    return ftpDir;
  }

  /**
   * @return the Data Connection
   */
  public final FtpDataAsyncConn getDataConn() {
    return dataConn;
  }

  @Override
  public final FtpAuth getAuth() {
    return ftpAuth;
  }

  @Override
  public final Restart getRestart() {
    return restart;
  }

  /**
   * This function is called when the Command Channel is connected (from
   * channelConnected of the NetworkHandler)
   */
  public final synchronized void setControlConnected() {
    dataConn = new FtpDataAsyncConn(this);
    // AuthInterface must be done before FtpFile
    ftpAuth = businessHandler.getBusinessNewAuth();
    ftpDir = businessHandler.getBusinessNewDir();
    restart = businessHandler.getBusinessNewRestart();
  }

  /**
   * Special initialization (FtpExec with Https session)
   *
   * @param auth
   * @param dir
   * @param restart
   */
  public final void setSpecialInit(final FtpAuth auth, final FtpDir dir,
                                   final Restart restart) {
    ftpAuth = auth;
    ftpDir = dir;
    this.restart = restart;
  }

  /**
   * @return the Control channel
   */
  public final Channel getControlChannel() {
    final NetworkHandler networkHandler = getNetworkHandler();
    if (networkHandler != null) {
      return networkHandler.getControlChannel();
    }
    return null;
  }

  /**
   * @return The network handler associated with control
   */
  public final NetworkHandler getNetworkHandler() {
    if (businessHandler != null) {
      return businessHandler.getNetworkHandler();
    }
    return null;
  }

  /**
   * Set the new current command
   *
   * @param command
   */
  public final void setNextCommand(final CommandInterface command) {
    previousCommand = currentCommand;
    currentCommand = (AbstractCommand) command;
    isCurrentCommandFinished.set(false);
  }

  /**
   * @return the currentCommand
   */
  public final AbstractCommand getCurrentCommand() {
    return currentCommand;
  }

  /**
   * @return the previousCommand
   */
  public final AbstractCommand getPreviousCommand() {
    return previousCommand;
  }

  /**
   * Set the previous command as the new current command (used after a
   * incorrect
   * sequence of commands or unknown
   * command)
   */
  public final void setPreviousAsCurrentCommand() {
    currentCommand = previousCommand;
    isCurrentCommandFinished.set(true);
  }

  /**
   * @return True if the Current Command is already Finished (ready to accept
   *     a
   *     new one)
   */
  public final boolean isCurrentCommandFinished() {
    return isCurrentCommandFinished.get();
  }

  /**
   * Set the Current Command as finished
   */
  public final void setCurrentCommandFinished() {
    isCurrentCommandFinished.set(true);
  }

  /**
   * @return the answer
   */
  public final String getAnswer() {
    if (answer == null) {
      if (replyCode == null) {
        answer = ReplyCode.REPLY_000_SPECIAL_NOSTATUS.getMesg();
      } else {
        answer = replyCode.getMesg();
      }
    }
    return answer;
  }

  /**
   * @param replyCode the replyCode to set
   * @param answer
   */
  public final void setReplyCode(final ReplyCode replyCode,
                                 final String answer) {
    this.replyCode = replyCode;
    if (answer != null) {
      this.answer = ReplyCode.getFinalMsg(replyCode.getCode(), answer);
    } else {
      this.answer = replyCode.getMesg();
    }
  }

  /**
   * @param exception
   */
  public final void setReplyCode(final CommandAbstractException exception) {
    setReplyCode(exception.code, exception.message);
  }

  /**
   * Set Exit code after an error
   *
   * @param answer
   */
  public final void setExitErrorCode(final String answer) {
    setReplyCode(
        ReplyCode.REPLY_421_SERVICE_NOT_AVAILABLE_CLOSING_CONTROL_CONNECTION,
        answer);
  }

  /**
   * Set Exit normal code
   *
   * @param answer
   */
  public final void setExitNormalCode(final String answer) {
    setReplyCode(ReplyCode.REPLY_221_CLOSING_CONTROL_CONNECTION, answer);
  }

  /**
   * @return the replyCode
   */
  public final ReplyCode getReplyCode() {
    return replyCode;
  }

  @Override
  public final void clear() {
    if (dataConn != null) {
      dataConn.clear();
    }
    if (ftpDir != null) {
      ftpDir.clear();
    }
    if (ftpAuth != null) {
      ftpAuth.clear();
    }
    previousCommand = null;
    replyCode = null;
    answer = null;
    isReady.cancel();
  }

  /**
   * @return True if the Control is ready to accept command
   */
  public final boolean isReady() {
    isReady.awaitOrInterruptible();
    return isReady.isSuccess();
  }

  /**
   * @param isReady the isReady to set
   */
  public final void setReady(final boolean isReady) {
    if (isReady) {
      this.isReady.setSuccess();
    } else {
      this.isReady.cancel();
    }
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder("FtpSession: ");
    if (ftpAuth != null) {
      builder.append("User: ").append(ftpAuth.getUser()).append('/')
             .append(ftpAuth.getAccount()).append(' ');
    }
    if (previousCommand != null) {
      builder.append("PRVCMD: ").append(previousCommand.getCommand())
             .append(' ').append(previousCommand.getArg()).append(' ');
    }
    if (currentCommand != null) {
      builder.append("CMD: ").append(currentCommand.getCommand()).append(' ')
             .append(currentCommand.getArg()).append(' ');
    }
    if (replyCode != null) {
      builder.append("Reply: ")
             .append(answer != null? answer : replyCode.getMesg()).append(' ');
    }
    if (dataConn != null) {
      builder.append(dataConn);
    }
    if (ftpDir != null) {
      try {
        builder.append(" PWD: ").append(ftpDir.getPwd());
      } catch (final CommandAbstractException ignored) {
        // nothing
      }
    }
    if (getControlChannel() != null) {
      builder.append(" Control: ").append(getControlChannel());
    }
    try {
      if (getDataConn().getCurrentDataChannel() != null) {
        builder.append(" Data: ").append(getDataConn().getCurrentDataChannel());
      }
    } catch (final FtpNoConnectionException ignored) {
      // nothing
    }
    return builder.append('\n').toString();
  }

  @Override
  public final int getBlockSize() {
    return restart.getMaxSize(configuration.getBlocksize());
  }

  @Override
  public final FileParameterInterface getFileParameter() {
    return configuration.getFileParameter();
  }

  /**
   * @param path
   *
   * @return the basename from the given path
   */
  public static String getBasename(final String path) {
    final File file = new File(path);
    return file.getName();
  }

  /**
   * Reinitialize the authentication to the connection step
   */
  public final void reinitFtpAuth() {
    final AbstractCommand connectioncommand = new ConnectionCommand(this);
    setNextCommand(connectioncommand);
    getAuth().clear();
    getDataConn().clear();
    getDataConn().getFtpTransferControl().resetWaitForOpenedDataChannel();
  }

  /**
   * Reinitialize all connection parameters, including authentification
   */
  public final void rein() {
    // reset to default
    // Previous mode could be Passive so remove the current configuration
    final InetSocketAddress local = getDataConn().getLocalAddress();
    final InetAddress remote = getDataConn().getRemoteAddress().getAddress();
    getConfiguration().delFtpSession(remote, local);
    getDataConn().unbindData();
    getDataConn().setMode(FtpArgumentCode.TransferMode.STREAM);
    getDataConn().setStructure(FtpArgumentCode.TransferStructure.FILE);
    getDataConn().setType(FtpArgumentCode.TransferType.ASCII);
    getDataConn().setSubType(TransferSubType.NONPRINT);
    reinitFtpAuth();
  }

  /**
   * Try to open a connection. Do the intermediate reply if any (150) and the
   * final one (125)
   *
   * @throws Reply425Exception if the connection cannot be opened
   */
  public final void openDataConnection() throws Reply425Exception {
    getDataConn().getFtpTransferControl().openDataConnection();
    final NetworkHandler networkHandler = getNetworkHandler();
    if (networkHandler != null) {
      networkHandler.writeIntermediateAnswer();
    }
  }

  @Override
  public final String getUniqueExtension() {
    return configuration.getUniqueExtension();
  }

  public final boolean isSsl() {
    return isSsl.get();
  }

  public final void setSsl(final boolean isSsl) {
    this.isSsl.set(isSsl);
    if (waitForSsl != null) {
      if (isSsl) {
        waitForSsl.setSuccess();
      } else {
        waitForSsl.cancel();
      }
    }
  }

  public synchronized void prepareSsl() {
    waitForSsl = new WaarpFuture(true);
  }

  public final boolean isSslReady() {
    if (waitForSsl != null) {
      for (int i = 0; i < 20; i++) {
        if (waitForSsl.awaitOrInterruptible(100)) {
          break;
        }
        Thread.yield();
      }
      logger.debug("DEBUG : {}:{}:{}", waitForSsl.isDone(), isSsl.get(),
                   getControlChannel());
    }
    return isSsl.get();
  }

  public final boolean isDataSsl() {
    return isDataSsl.get();
  }

  public final void setDataSsl(final boolean isDataSsl) {
    this.isDataSsl.set(isDataSsl);
  }

}
