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

import org.waarp.common.command.ReplyCode;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.database.DbConstant;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.ftp.core.command.FtpCommandCode;
import org.waarp.ftp.core.control.BusinessHandler;
import org.waarp.ftp.core.data.FtpTransfer;
import org.waarp.ftp.core.exception.FtpNoFileException;
import org.waarp.ftp.core.session.FtpSession;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.gateway.ftp.database.data.DbTransferLog;

/**
 * Class to help to log any actions through the interface of Waarp
 */
public final class WaarpActionLogger {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpActionLogger.class);

  private WaarpActionLogger() {
  }

  /**
   * Log the action
   *
   * @param ftpSession
   * @param message
   * @param file
   * @param handler
   */
  public static long logCreate(final DbSession ftpSession, final String message,
                               final String file,
                               final BusinessHandler handler) {
    final FtpSession session = handler.getFtpSession();
    final String sessionContexte = session.toString();
    logger.info(message + ' ' + sessionContexte);
    if (ftpSession != null) {
      final FtpCommandCode code = session.getCurrentCommand().getCode();
      if (FtpCommandCode.isStorOrRetrLikeCommand(code)) {
        final boolean isSender = FtpCommandCode.isRetrLikeCommand(code);
        try {
          // Insert new one
          final DbTransferLog log =
              new DbTransferLog(ftpSession, session.getAuth().getUser(),
                                session.getAuth().getAccount(),
                                DbConstant.ILLEGALVALUE, isSender, file,
                                code.name(),
                                ReplyCode.REPLY_000_SPECIAL_NOSTATUS, message,
                                UpdatedInfo.TOSUBMIT);
          logger.debug("Create FS: " + log);
          if (FileBasedConfiguration.fileBasedConfiguration.getMonitoring() !=
              null) {
            if (isSender) {
              FileBasedConfiguration.fileBasedConfiguration.getMonitoring()
                                                           .updateLastOutBand();
            } else {
              FileBasedConfiguration.fileBasedConfiguration.getMonitoring()
                                                           .updateLastInBound();
            }
          }
          return log.getSpecialId();
        } catch (final WaarpDatabaseException e1) {
          // Do nothing
        }
      }
    }
    return DbConstant.ILLEGALVALUE;
  }

  /**
   * Log the action
   *
   * @param ftpSession
   * @param specialId
   * @param message
   * @param handler
   * @param rcode
   * @param info
   */
  public static long logAction(final DbSession ftpSession, final long specialId,
                               final String message,
                               final BusinessHandler handler,
                               final ReplyCode rcode, final UpdatedInfo info) {
    final FtpSession session = handler.getFtpSession();
    final String sessionContexte = session.toString();
    logger.info(message + ' ' + sessionContexte);
    if (ftpSession != null && specialId != DbConstant.ILLEGALVALUE) {
      final FtpCommandCode code = session.getCurrentCommand().getCode();
      if (FtpCommandCode.isStorOrRetrLikeCommand(code)) {
        try {
          // Try load
          final DbTransferLog log =
              new DbTransferLog(ftpSession, session.getAuth().getUser(),
                                session.getAuth().getAccount(), specialId);
          log.changeUpdatedInfo(info);
          log.setInfotransf(message);
          log.setReplyCodeExecutionStatus(rcode);
          log.update();
          logger.debug("Update FS: " + log);
          return log.getSpecialId();
        } catch (final WaarpDatabaseException e) {
          // Do nothing
        }
      } else {
        if (FileBasedConfiguration.fileBasedConfiguration.getMonitoring() !=
            null) {
          FileBasedConfiguration.fileBasedConfiguration.getMonitoring()
                                                       .updateCodeNoTransfer(
                                                           rcode);
        }
      }
    } else {
      if (FileBasedConfiguration.fileBasedConfiguration.getMonitoring() !=
          null) {
        FileBasedConfiguration.fileBasedConfiguration.getMonitoring()
                                                     .updateCodeNoTransfer(
                                                         rcode);
      }
    }
    return specialId;
  }

  /**
   * Log the action in error
   *
   * @param ftpSession
   * @param specialId
   * @param transfer
   * @param message
   * @param rcode
   * @param handler
   */
  public static void logErrorAction(final DbSession ftpSession,
                                    final long specialId,
                                    final FtpTransfer transfer,
                                    final String message, final ReplyCode rcode,
                                    final BusinessHandler handler) {
    final FtpSession session = handler.getFtpSession();
    final String sessionContexte = session.toString();
    logger.error(rcode.getCode() + ":" + message + ' ' + sessionContexte);
    logger.debug("Log", new Exception("Log"));
    if (ftpSession != null && specialId != DbConstant.ILLEGALVALUE) {
      final FtpCommandCode code = session.getCurrentCommand().getCode();
      if (FtpCommandCode.isStorOrRetrLikeCommand(code)) {
        String file = null;
        if (transfer != null) {
          try {
            file = transfer.getFtpFile().getFile();
          } catch (final CommandAbstractException ignored) {
            // nothing
          } catch (final FtpNoFileException ignored) {
            // nothing
          }
        }
        final UpdatedInfo info = UpdatedInfo.INERROR;
        try {
          // Try load
          final DbTransferLog log =
              new DbTransferLog(ftpSession, session.getAuth().getUser(),
                                session.getAuth().getAccount(), specialId);
          log.changeUpdatedInfo(info);
          log.setInfotransf(message);
          if (rcode.getCode() < 400) {
            log.setReplyCodeExecutionStatus(
                ReplyCode.REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED);
          } else {
            log.setReplyCodeExecutionStatus(rcode);
          }
          if (file != null) {
            log.setFilename(file);
          }
          log.update();
          if (FileBasedConfiguration.fileBasedConfiguration.getFtpMib() !=
              null) {
            FileBasedConfiguration.fileBasedConfiguration.getFtpMib()
                                                         .notifyInfoTask(
                                                             message, log);
          }
          logger.debug("Update FS: " + log);
        } catch (final WaarpDatabaseException e) {
          // Do nothing
        }
      } else {
        if (FileBasedConfiguration.fileBasedConfiguration.getMonitoring() !=
            null) {
          FileBasedConfiguration.fileBasedConfiguration.getMonitoring()
                                                       .updateCodeNoTransfer(
                                                           rcode);
        }
        if (rcode != ReplyCode.REPLY_450_REQUESTED_FILE_ACTION_NOT_TAKEN &&
            rcode != ReplyCode.REPLY_550_REQUESTED_ACTION_NOT_TAKEN) {
          if (FileBasedConfiguration.fileBasedConfiguration.getFtpMib() !=
              null) {
            FileBasedConfiguration.fileBasedConfiguration.getFtpMib()
                                                         .notifyWarning(
                                                             rcode.getMesg(),
                                                             message);
          }
        }
      }
    } else {
      if (FileBasedConfiguration.fileBasedConfiguration.getMonitoring() !=
          null) {
        FileBasedConfiguration.fileBasedConfiguration.getMonitoring()
                                                     .updateCodeNoTransfer(
                                                         rcode);
      }
      if (rcode != ReplyCode.REPLY_450_REQUESTED_FILE_ACTION_NOT_TAKEN &&
          rcode != ReplyCode.REPLY_550_REQUESTED_ACTION_NOT_TAKEN) {
        if (FileBasedConfiguration.fileBasedConfiguration.getFtpMib() != null) {
          FileBasedConfiguration.fileBasedConfiguration.getFtpMib()
                                                       .notifyWarning(
                                                           rcode.getMesg(),
                                                           message);
        }
      }
    }
  }
}
