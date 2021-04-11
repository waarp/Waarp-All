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

package org.waarp.gateway.ftp.control;

import io.netty.channel.Channel;
import org.waarp.common.command.ReplyCode;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply421Exception;
import org.waarp.common.command.exception.Reply451Exception;
import org.waarp.common.command.exception.Reply502Exception;
import org.waarp.common.command.exception.Reply504Exception;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.ftp.core.command.AbstractCommand;
import org.waarp.ftp.core.command.FtpCommandCode;
import org.waarp.ftp.core.command.access.QUIT;
import org.waarp.ftp.core.control.BusinessHandler;
import org.waarp.ftp.core.data.FtpTransfer;
import org.waarp.ftp.core.exception.FtpNoFileException;
import org.waarp.ftp.core.file.FtpFile;
import org.waarp.ftp.core.session.FtpSession;
import org.waarp.ftp.filesystembased.FilesystemBasedFtpRestart;
import org.waarp.gateway.ftp.config.AUTHUPDATE;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.gateway.ftp.database.DbConstantFtp;
import org.waarp.gateway.ftp.exec.AbstractExecutor;
import org.waarp.gateway.ftp.file.FileBasedAuth;
import org.waarp.gateway.ftp.file.FileBasedDir;

import java.io.File;
import java.io.IOException;

/**
 * BusinessHandler implementation that allows pre and post actions on any
 * operations and specifically on
 * transfer operations
 */
public class ExecBusinessHandler extends BusinessHandler {
  private static final String
      TRANSFER_DONE_BUT_FORCE_DISCONNECTION_SINCE_AN_ERROR_OCCURS_ON_POST_OPERATION =
      "Transfer done but force disconnection since an error occurs on PostOperation";

  private static final String
      POST_EXECUTION_IN_ERROR_FOR_TRANSFER_SINCE_NO_FILE_FOUND =
      "PostExecution in Error for Transfer since No File found";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(ExecBusinessHandler.class);

  /**
   * Associated DbFtpSession
   */
  private DbSession dbFtpSession;
  /**
   * Associated DbR66Session
   */
  private DbSession dbR66Session;
  private boolean internalDb;

  @Override
  public void afterTransferDoneBeforeAnswer(final FtpTransfer transfer)
      throws CommandAbstractException {
    // if Admin, do nothing
    if (getFtpSession() == null || getFtpSession().getAuth() == null) {
      return;
    }
    final FileBasedAuth auth = (FileBasedAuth) getFtpSession().getAuth();
    if (auth.isAdmin()) {
      return;
    }
    final long specialId = auth.getSpecialId();
    final ReplyCode replyCode = getFtpSession().getReplyCode();
    logger.debug("Transfer done but action needed: {}", !(replyCode !=
                                                          ReplyCode.REPLY_250_REQUESTED_FILE_ACTION_OKAY &&
                                                          replyCode !=
                                                          ReplyCode.REPLY_226_CLOSING_DATA_CONNECTION));
    if (replyCode != ReplyCode.REPLY_250_REQUESTED_FILE_ACTION_OKAY &&
        replyCode != ReplyCode.REPLY_226_CLOSING_DATA_CONNECTION) {
      // Do nothing
      final String message = "Transfer done with code: " +
                             getFtpSession().getReplyCode().getMesg();
      WaarpActionLogger
          .logErrorAction(dbFtpSession, specialId, transfer, message,
                          getFtpSession().getReplyCode(), this);
      return;
    }
    // if STOR like: get file (can be STOU) and execute external action
    final FtpCommandCode code = transfer.getCommand();
    logger.debug("Checking action vs auth after transfer: {}", code);
    switch (code) {
      case RETR:
        // nothing to do since All done
        WaarpActionLogger
            .logAction(dbFtpSession, specialId, "Retrieve executed: OK", this,
                       getFtpSession().getReplyCode(), UpdatedInfo.RUNNING);
        break;
      case APPE:
      case STOR:
      case STOU:
        // execute the store command
        final WaarpFuture futureCompletion = new WaarpFuture(true);
        final String[] args = new String[6];
        args[0] = auth.getUser();
        args[1] = auth.getAccount();
        args[2] = auth.getBaseDirectory();
        final FtpFile file;
        try {
          file = transfer.getFtpFile();
        } catch (final FtpNoFileException e1) {
          // File cannot be sent
          final String message =
              "PostExecution in Error for Transfer since No File found: " +
              transfer.getCommand() + ' ' + transfer.getStatus() + ' ' +
              transfer.getPath();
          final CommandAbstractException exc = new Reply421Exception(
              POST_EXECUTION_IN_ERROR_FOR_TRANSFER_SINCE_NO_FILE_FOUND);
          WaarpActionLogger
              .logErrorAction(dbFtpSession, specialId, transfer, message,
                              exc.code, this);
          throw exc;
        }
        try {
          args[3] = file.getFile();
          final File newfile = new File(args[2] + args[3]);

          // Here the transfer is successful. If the file does not exist on disk
          // We create it : the transfered file was empty.
          if (!newfile.canRead()) {
            try {
              if (!newfile.createNewFile()) {
                logger.error("Cannot create New Empty File");
              }
            } catch (final IOException e) {
              throw new Reply421Exception(
                  POST_EXECUTION_IN_ERROR_FOR_TRANSFER_SINCE_NO_FILE_FOUND);
            } catch (final SecurityException e) {
              throw new Reply421Exception(
                  POST_EXECUTION_IN_ERROR_FOR_TRANSFER_SINCE_NO_FILE_FOUND);
            }
          }

          if (!newfile.canRead()) {
            // File cannot be sent
            final String message =
                "PostExecution in Error for Transfer since File is not readable: " +
                transfer.getCommand() + ' ' + newfile.getAbsolutePath() + ':' +
                newfile.canRead() + ' ' + transfer.getStatus() + ' ' +
                transfer.getPath();
            final CommandAbstractException exc = new Reply421Exception(
                TRANSFER_DONE_BUT_FORCE_DISCONNECTION_SINCE_AN_ERROR_OCCURS_ON_POST_OPERATION);
            WaarpActionLogger
                .logErrorAction(dbFtpSession, specialId, transfer, message,
                                exc.code, this);
            throw exc;
          }
        } catch (final CommandAbstractException e1) {
          // File cannot be sent
          final String message =
              "PostExecution in Error for Transfer since No File found: " +
              transfer.getCommand() + ' ' + transfer.getStatus() + ' ' +
              transfer.getPath();
          final CommandAbstractException exc = new Reply421Exception(
              TRANSFER_DONE_BUT_FORCE_DISCONNECTION_SINCE_AN_ERROR_OCCURS_ON_POST_OPERATION);
          WaarpActionLogger
              .logErrorAction(dbFtpSession, specialId, transfer, message,
                              exc.code, this);
          throw exc;
        }
        args[4] = transfer.getCommand().toString();
        args[5] = Long.toString(specialId);
        final AbstractExecutor executor = AbstractExecutor
            .createAbstractExecutor(auth, args, true, futureCompletion);
        executor.run();
        futureCompletion.awaitOrInterruptible();
        if (futureCompletion.isSuccess()) {
          // All done
          WaarpActionLogger
              .logAction(dbFtpSession, specialId, "Post-Command executed: OK",
                         this, getFtpSession().getReplyCode(),
                         UpdatedInfo.RUNNING);
        } else {
          // File cannot be sent
          final String message =
              "PostExecution in Error for Transfer: " + transfer.getCommand() +
              ' ' + transfer.getStatus() + ' ' + transfer.getPath() + "\n   " +
              (futureCompletion.getCause() != null?
                  futureCompletion.getCause().getMessage() :
                  "Internal error of PostExecution");
          final CommandAbstractException exc = new Reply421Exception(
              TRANSFER_DONE_BUT_FORCE_DISCONNECTION_SINCE_AN_ERROR_OCCURS_ON_POST_OPERATION);
          WaarpActionLogger
              .logErrorAction(dbFtpSession, specialId, transfer, message,
                              exc.code, this);
          throw exc;
        }
        break;
      default:
        // nothing to do
    }
  }

  @Override
  public void afterRunCommandKo(final CommandAbstractException e) {
    final String message =
        "ExecHandler: KO: " + getFtpSession() + ' ' + e.getMessage();
    final long specialId =
        ((FileBasedAuth) getFtpSession().getAuth()).getSpecialId();
    WaarpActionLogger
        .logErrorAction(dbFtpSession, specialId, null, message, e.code, this);
    ((FileBasedAuth) getFtpSession().getAuth())
        .setSpecialId(org.waarp.common.database.DbConstant.ILLEGALVALUE);
  }

  @Override
  public void afterRunCommandOk() {
    if (!(getFtpSession().getCurrentCommand() instanceof QUIT) &&
        dbR66Session != null) {
      final long specialId =
          ((FileBasedAuth) getFtpSession().getAuth()).getSpecialId();
      WaarpActionLogger.logAction(dbFtpSession, specialId,
                                  "Transfer Command fully executed: OK", this,
                                  getFtpSession().getReplyCode(),
                                  UpdatedInfo.DONE);
      ((FileBasedAuth) getFtpSession().getAuth())
          .setSpecialId(org.waarp.common.database.DbConstant.ILLEGALVALUE);
    }
  }

  @Override
  public void beforeRunCommand() throws CommandAbstractException {
    long specialId = org.waarp.common.database.DbConstant.ILLEGALVALUE;
    // if Admin, do nothing
    if (getFtpSession() == null || getFtpSession().getAuth() == null) {
      return;
    }
    final FileBasedAuth auth = (FileBasedAuth) getFtpSession().getAuth();
    if (auth.isAdmin()) {
      logger.debug("Admin user so all actions are allowed");
      return;
    }
    // Test limits
    final FtpConstraintLimitHandler constraints =
        ((FileBasedConfiguration) getFtpSession().getConfiguration())
            .getConstraintLimitHandler();
    if (constraints != null) {
      if (!auth.isIdentified()) {
        // ignore test since it can be an Admin connection
      } else if (auth.isAdmin()) {
        // ignore test since it is an Admin connection (always valid)
      } else if (!FtpCommandCode
          .isSpecialCommand(getFtpSession().getCurrentCommand().getCode())) {
        // Authenticated, not Admin and not Special Command
        if (constraints.checkConstraintsSleep(1)) {
          if (constraints.checkConstraints()) {
            // Really overload so refuse the command
            logger
                .info("Server overloaded. {} Try later... \n" + getFtpSession(),
                      constraints.lastAlert);
            if (FileBasedConfiguration.fileBasedConfiguration.getFtpMib() !=
                null) {
              FileBasedConfiguration.fileBasedConfiguration.getFtpMib()
                                                           .notifyOverloaded(
                                                               "Server overloaded",
                                                               getFtpSession()
                                                                   .toString());
            }
            throw new Reply451Exception("Server overloaded. Try later...");
          }
        }
      }
    }
    final FtpCommandCode code = getFtpSession().getCurrentCommand().getCode();
    logger.debug("Checking action vs auth before command: {}", code);
    switch (code) {
      case APPE:
      case STOR:
      case STOU:
        auth.setSpecialId(specialId);
        if (!auth.getCommandExecutor().isValidOperation(true)) {
          throw new Reply504Exception("STORe like operations are not allowed");
        }
        // create entry in log
        specialId = WaarpActionLogger
            .logCreate(dbFtpSession, "PrepareTransfer: OK",
                       getFtpSession().getCurrentCommand().getArg(), this);
        auth.setSpecialId(specialId);
        // nothing to do now
        break;
      case RETR:
        auth.setSpecialId(specialId);
        if (!auth.getCommandExecutor().isValidOperation(false)) {
          throw new Reply504Exception(
              "RETRieve like operations are not allowed");
        }
        // create entry in log
        specialId = WaarpActionLogger
            .logCreate(dbFtpSession, "PrepareTransfer: OK",
                       getFtpSession().getCurrentCommand().getArg(), this);
        auth.setSpecialId(specialId);
        // execute the external retrieve command before the execution of RETR
        final WaarpFuture futureCompletion = new WaarpFuture(true);
        final String[] args = new String[6];
        args[0] = auth.getUser();
        args[1] = auth.getAccount();
        args[2] = auth.getBaseDirectory();
        final String filename = getFtpSession().getCurrentCommand().getArg();
        final FtpFile file = getFtpSession().getDir().setFile(filename, false);
        args[3] = file.getFile();
        args[4] = code.toString();
        args[5] = Long.toString(specialId);
        final AbstractExecutor executor = AbstractExecutor
            .createAbstractExecutor(auth, args, false, futureCompletion);
        executor.run();
        futureCompletion.awaitOrInterruptible();
        if (futureCompletion.isSuccess()) {
          // File should be ready
          if (!file.canRead()) {
            logger.error("PreExecution in Error for Transfer since " +
                         "File downloaded but not ready to be retrieved: {} " +
                         " {} \n   " + (futureCompletion.getCause() != null?
                             futureCompletion.getCause().getMessage() :
                             "File downloaded but not ready to be retrieved"), args[4],
                         args[3]);
            throw new Reply421Exception(
                "File downloaded but not ready to be retrieved");
          }
          WaarpActionLogger
              .logAction(dbFtpSession, specialId, "Pre-Command executed: OK",
                         this, getFtpSession().getReplyCode(),
                         UpdatedInfo.RUNNING);
        } else {
          // File cannot be retrieved
          logger.error("PreExecution in Error for Transfer since " +
                       "File cannot be prepared to be retrieved: {} " +
                       " {} \n   " + (futureCompletion.getCause() != null?
              futureCompletion.getCause().getMessage() :
              "File cannot be prepared to be retrieved"), args[4], args[3]);
          throw new Reply421Exception(
              "File cannot be prepared to be retrieved");
        }
        break;
      default:
        // nothing to do
    }
  }

  @Override
  protected void cleanSession() {
    // Nothing
  }

  @Override
  public void exceptionLocalCaught(final Throwable cause) {
    if (FileBasedConfiguration.fileBasedConfiguration.getFtpMib() != null) {
      final String mesg;
      if (cause != null && cause.getMessage() != null) {
        mesg = cause.getMessage();
      } else {
        if (getFtpSession() != null) {
          mesg = "Exception while " + getFtpSession().getReplyCode().getMesg();
        } else {
          mesg = "Unknown Exception";
        }
      }
      FileBasedConfiguration.fileBasedConfiguration.getFtpMib().notifyError(
          "Exception trapped", mesg);
    }
    if (FileBasedConfiguration.fileBasedConfiguration.getMonitoring() != null) {
      if (getFtpSession() != null) {
        FileBasedConfiguration.fileBasedConfiguration.getMonitoring()
                                                     .updateCodeNoTransfer(
                                                         getFtpSession()
                                                             .getReplyCode());
      }
    }
  }

  @Override
  public void executeChannelClosed() {
    if (AbstractExecutor.useDatabase && !internalDb && dbR66Session != null) {
      dbR66Session.disconnect();
      dbR66Session = null;
    }
    if (dbFtpSession != null) {
      dbFtpSession.disconnect();
      dbFtpSession = null;
    }
  }

  @Override
  public void executeChannelConnected(final Channel channel) {
    if (AbstractExecutor.useDatabase) {
      if (org.waarp.common.database.DbConstant.admin != null) {
        try {
          dbR66Session =
              new DbSession(org.waarp.common.database.DbConstant.admin, false);
        } catch (final WaarpDatabaseNoConnectionException e1) {
          logger.warn("Database not ready due to {}", e1.getMessage());
          final QUIT command = (QUIT) FtpCommandCode
              .getFromLine(getFtpSession(), FtpCommandCode.QUIT.name());
          getFtpSession().setNextCommand(command);
          dbR66Session = null;
          internalDb = true;
        }
      }
    }
    if (DbConstantFtp.gatewayAdmin != null) {
      try {
        dbFtpSession = new DbSession(DbConstantFtp.gatewayAdmin, false);
      } catch (final WaarpDatabaseNoConnectionException e1) {
        logger.warn("Database not ready due to {}", e1.getMessage());
        final QUIT command = (QUIT) FtpCommandCode
            .getFromLine(getFtpSession(), FtpCommandCode.QUIT.name());
        getFtpSession().setNextCommand(command);
        dbFtpSession = null;
      }
    }
  }

  @Override
  public FileBasedAuth getBusinessNewAuth() {
    return new FileBasedAuth(getFtpSession());
  }

  @Override
  public FileBasedDir getBusinessNewDir() {
    return new FileBasedDir(getFtpSession());
  }

  @Override
  public FilesystemBasedFtpRestart getBusinessNewRestart() {
    return new FilesystemBasedFtpRestart(getFtpSession());
  }

  @Override
  public String getHelpMessage(final String arg) {
    return
        "This FTP server is only intend as a Gateway. RETRieve actions may be unallowed.\n" +
        "This FTP server refers to RFC 959, 775, 2389, 2428, 3659 and " +
        "supports XDIGEST, XCRC, XMD5 and XSHA1 commands.\n" +
        "XCRC, XMD5 and XSHA1 take a simple filename as argument, XDIGEST " +
        "taking algorithm (among CRC32, ADLER32, MD5, MD2, " +
        "SHA-1, SHA-256, SHA-384, SHA-512) followed by filename " +
        "as arguments, and return 250 digest-value is the digest of filename.";
  }

  @Override
  public String getFeatMessage() {
    final StringBuilder builder =
        new StringBuilder("Extensions supported:").append('\n').append(
            getDefaultFeatMessage());
    if (getFtpSession().getConfiguration().getFtpInternalConfiguration()
                       .isAcceptAuthProt()) {
      builder.append('\n').append(getSslFeatMessage());
    }
    builder.append('\n').append(FtpCommandCode.SITE.name()).append(' ')
           .append("AUTHUPDATE").append("\nEnd");
    return builder.toString();
  }

  @Override
  public String getOptsMessage(final String[] args)
      throws CommandAbstractException {
    if (args.length > 0) {
      if (args[0].equalsIgnoreCase(FtpCommandCode.MLST.name()) ||
          args[0].equalsIgnoreCase(FtpCommandCode.MLSD.name())) {
        return getMLSxOptsMessage(args);
      }
      throw new Reply502Exception("OPTS not implemented for " + args[0]);
    }
    throw new Reply502Exception("OPTS not implemented");
  }

  @Override
  public AbstractCommand getSpecializedSiteCommand(final FtpSession session,
                                                   final String line) {
    if (getFtpSession() == null || getFtpSession().getAuth() == null) {
      return null;
    }
    if (!session.getAuth().isAdmin()) {
      return null;
    }
    if (line == null) {
      return null;
    }
    final String command;
    String arg;
    if (line.indexOf(' ') == -1) {
      command = line;
      arg = null;
    } else {
      command = line.substring(0, line.indexOf(' '));
      arg = line.substring(line.indexOf(' ') + 1);
      if (arg.length() == 0) {
        arg = null;
      }
    }
    final String COMMAND = command.toUpperCase();
    if (!"AUTHUPDATE".equals(COMMAND)) {
      return null;
    }
    final AbstractCommand abstractCommand = new AUTHUPDATE();
    abstractCommand.setArgs(session, COMMAND, arg, FtpCommandCode.SITE);
    return abstractCommand;
  }
}
