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
package org.waarp.openr66.protocol.utils;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.client.RequestTransfer;
import org.waarp.openr66.commander.ClientRunner;
import org.waarp.openr66.commander.CommanderNoDb;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.authentication.R66Auth;
import org.waarp.openr66.context.filesystem.R66File;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.database.data.DbTaskRunner.TASKSTEP;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.LocalServerHandler;
import org.waarp.openr66.protocol.localhandler.packet.ErrorPacket;

import java.sql.Timestamp;
import java.util.Map;

/**
 * Utility class for transfers
 */
public final class TransferUtils {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(TransferUtils.class);

  private TransferUtils() {
  }

  /**
   * Try to restart one Transfer Runner Task
   *
   * @param taskRunner
   *
   * @return the associated Result
   *
   * @throws WaarpDatabaseException
   */
  public static R66Result restartTransfer(final DbTaskRunner taskRunner,
                                          final LocalChannelReference lcr)
      throws WaarpDatabaseException {
    final R66Result finalResult =
        new R66Result(null, true, ErrorCode.InitOk, taskRunner);
    if (lcr != null) {
      finalResult.setCode(ErrorCode.QueryStillRunning);
      finalResult.setOther(Messages.getString("TransferUtils.0")); //$NON-NLS-1$
    } else {
      if (taskRunner.isSendThrough()) {
        // XXX FIXME TODO cannot be restarted... Really?
      }
      // Transfer is not running
      // but maybe need action on database
      try {
        if (taskRunner.restart(true)) {
          taskRunner.forceSaveStatus();
          finalResult.setCode(ErrorCode.PreProcessingOk);
          finalResult
              .setOther(Messages.getString("TransferUtils.1")); //$NON-NLS-1$
        } else {
          if (taskRunner.isSelfRequested() &&
              taskRunner.getGloballaststep() < TASKSTEP.POSTTASK.ordinal()) {
            // send a VALID packet with VALID code to the requester except if client
            final DbHostAuth host =
                R66Auth.getServerAuth(taskRunner.getRequester());
            if (host == null || host.isClient()) {
              // cannot be relaunch from there
              finalResult.setCode(ErrorCode.ConnectionImpossible);
              finalResult.setOther(
                  Messages.getString("TransferUtils.2")); //$NON-NLS-1$
              logger.warn(Messages.getString("TransferUtils.3")); //$NON-NLS-1$
            } else {
              final R66Future result = new R66Future(true);
              logger.info(Messages.getString("TransferUtils.4") +
                          taskRunner.toShortString()); //$NON-NLS-1$
              final RequestTransfer requestTransfer =
                  new RequestTransfer(result, taskRunner.getSpecialId(),
                                      taskRunner.getRequested(),
                                      taskRunner.getRequester(), false, false,
                                      true, Configuration.configuration
                                          .getInternalRunner()
                                          .getNetworkTransaction());
              requestTransfer.run();
              result.awaitOrInterruptible();
              if (!result.isDone()) {
                finalResult.setCode(ErrorCode.Internal);
                finalResult.setOther(
                    Messages.getString("TransferUtils.10")); //$NON-NLS-1$
              } else {
                final R66Result finalValue = result.getResult();
                switch (finalValue.getCode()) {
                  case QueryStillRunning:
                    finalResult.setCode(ErrorCode.QueryStillRunning);
                    finalResult.setOther(
                        Messages.getString("TransferUtils.5")); //$NON-NLS-1$
                    break;
                  case Running:
                    finalResult.setCode(ErrorCode.Running);
                    finalResult.setOther(
                        Messages.getString("TransferUtils.6")); //$NON-NLS-1$
                    break;
                  case PreProcessingOk:
                    finalResult.setCode(ErrorCode.PreProcessingOk);
                    finalResult.setOther(
                        Messages.getString("TransferUtils.7")); //$NON-NLS-1$
                    break;
                  case CompleteOk:
                    finalResult.setCode(ErrorCode.CompleteOk);
                    finalResult.setOther(
                        Messages.getString("TransferUtils.8")); //$NON-NLS-1$
                    taskRunner.setPostTask();
                    finalizeTaskWithNoSession(taskRunner, null);
                    taskRunner.setErrorExecutionStatus(
                        ErrorCode.QueryAlreadyFinished);
                    taskRunner.forceSaveStatus();
                    break;
                  case RemoteError:
                    finalResult.setCode(ErrorCode.RemoteError);
                    finalResult.setOther(
                        Messages.getString("TransferUtils.9")); //$NON-NLS-1$
                    break;
                  default:
                    finalResult.setCode(ErrorCode.Internal);
                    finalResult.setOther(
                        Messages.getString("TransferUtils.10")); //$NON-NLS-1$
                    break;
                }
              }
            }
          } else {
            finalResult.setCode(ErrorCode.CompleteOk);
            finalResult
                .setOther(Messages.getString("TransferUtils.11")); //$NON-NLS-1$
            taskRunner.setPostTask();
            finalizeTaskWithNoSession(taskRunner, null);
            taskRunner.setErrorExecutionStatus(ErrorCode.QueryAlreadyFinished);
            taskRunner.forceSaveStatus();
          }
        }
      } catch (final OpenR66RunnerErrorException e) {
        finalResult.setCode(ErrorCode.PreProcessingOk);
        finalResult
            .setOther(Messages.getString("TransferUtils.1")); //$NON-NLS-1$
      }
    }
    return finalResult;
  }

  /**
   * Finalize a local task since only Post action has to be done
   *
   * @param taskRunner
   * @param localChannelReference
   *
   * @throws OpenR66RunnerErrorException
   */
  public static void finalizeTaskWithNoSession(final DbTaskRunner taskRunner,
                                               final LocalChannelReference localChannelReference)
      throws OpenR66RunnerErrorException {
    final R66Session session = new R66Session();
    session.setStatus(50);
    final String remoteId =
        taskRunner.isSelfRequested()? taskRunner.getRequester() :
            taskRunner.getRequested();
    session.getAuth().specialNoSessionAuth(false, remoteId);
    session.setNoSessionRunner(taskRunner, localChannelReference);
    if (taskRunner.isSender()) {
      // Change dir
      try {
        session.getDir().changeDirectory(taskRunner.getRule().getSendPath());
      } catch (final CommandAbstractException e) {
        throw new OpenR66RunnerErrorException(e);
      }
    } else {
      // Change dir
      try {
        session.getDir().changeDirectory(taskRunner.getRule().getWorkPath());
      } catch (final CommandAbstractException e) {
        throw new OpenR66RunnerErrorException(e);
      }
    }
    try {
      try {
        session.setFileAfterPreRunner(false);
      } catch (final CommandAbstractException e) {
        throw new OpenR66RunnerErrorException(e);
      }
    } catch (final OpenR66RunnerErrorException e) {
      logger.error(Messages.getString("TransferUtils.27"),
                   taskRunner.getFilename()); //$NON-NLS-1$
      taskRunner.changeUpdatedInfo(UpdatedInfo.INERROR);
      taskRunner.setErrorExecutionStatus(ErrorCode.FileNotFound);
      try {
        taskRunner.update();
      } catch (final WaarpDatabaseException ignored) {
        // nothing
      }
      throw new OpenR66RunnerErrorException(
          Messages.getString("TransferUtils.28"), e); //$NON-NLS-1$
    }
    final R66File file = session.getFile();
    final R66Result finalValue =
        new R66Result(null, true, ErrorCode.CompleteOk, taskRunner);
    finalValue.setFile(file);
    finalValue.setRunner(taskRunner);
    taskRunner.finishTransferTask(ErrorCode.TransferOk);
    try {
      taskRunner
          .finalizeTransfer(localChannelReference, file, finalValue, true);
    } catch (final OpenR66ProtocolSystemException e) {
      logger.error(Messages.getString("TransferUtils.29"),
                   taskRunner.toShortString()); //$NON-NLS-1$
      taskRunner.changeUpdatedInfo(UpdatedInfo.INERROR);
      taskRunner.setErrorExecutionStatus(ErrorCode.Internal);
      try {
        taskRunner.update();
      } catch (final WaarpDatabaseException ignored) {
        // nothing
      }
      throw new OpenR66RunnerErrorException(
          Messages.getString("TransferUtils.30"), e); //$NON-NLS-1$
    }
  }

  @SuppressWarnings("unchecked")
  private static void stopOneTransfer(final DbTaskRunner taskRunner,
                                      final Object map,
                                      final R66Session session,
                                      final String body) {
    final LocalChannelReference lcr =
        Configuration.configuration.getLocalTransaction()
                                   .getFromRequest(taskRunner.getKey());
    ErrorCode result;
    final ErrorCode code = ErrorCode.StoppedTransfer;
    if (lcr != null) {
      final int rank = taskRunner.getRank();
      lcr.sessionNewState(R66FiniteDualStates.ERROR);
      final ErrorPacket perror =
          new ErrorPacket(Messages.getString("TransferUtils.13") + rank,
                          //$NON-NLS-1$
                          code.getCode(), ErrorPacket.FORWARDCLOSECODE);
      try {
        // inform local instead of remote
        LocalServerHandler.channelRead0(lcr, perror);
      } catch (final Exception e) {
        logger.warn("Write local packet error", e);
      }
      result = ErrorCode.StoppedTransfer;
    } else {
      // Transfer is not running
      // if in ERROR already just ignore it
      if (taskRunner.getUpdatedInfo() == UpdatedInfo.INERROR) {
        result = ErrorCode.TransferError;
      } else {
        // the database saying it is not stopped
        result = ErrorCode.TransferError;
        if (taskRunner.stopOrCancelRunner(code)) {
          result = ErrorCode.StoppedTransfer;
        }
      }
    }
    final ErrorCode last = taskRunner.getErrorInfo();
    taskRunner.setErrorExecutionStatus(result);
    if (map != null) {
      if (map instanceof Map) {
        ((Map<String, String>) map)
            .put(taskRunner.getKey(), taskRunner.getJsonAsString());
      } else if (map instanceof StringBuilder) {
        ((StringBuilder) map).append(taskRunner.toSpecializedHtml(session, body,
                                                                  lcr != null?
                                                                      Messages
                                                                          .getString(
                                                                              "HttpSslHandler.Active") :
                                                                      Messages
                                                                          .getString(
                                                                              "HttpSslHandler.NotActive")));
      }
    }
    taskRunner.setErrorExecutionStatus(last);
  }

  /**
   * Stop all selected transfers
   *
   * @param dbSession
   * @param limit
   * @param session
   * @param body
   * @param startid
   * @param stopid
   * @param tstart
   * @param tstop
   * @param rule
   * @param req
   * @param pending
   * @param transfer
   * @param error
   *
   * @return the associated StringBuilder if the one given as parameter is not
   *     null
   */
  public static void stopSelectedTransfers(final DbSession dbSession,
                                           final int limit, final Object map,
                                           final R66Session session,
                                           final String body,
                                           final String startid,
                                           final String stopid,
                                           final Timestamp tstart,
                                           final Timestamp tstop,
                                           final String rule, final String req,
                                           final boolean pending,
                                           final boolean transfer,
                                           final boolean error) {
    stopSelectedTransfers(dbSession, limit, map, session, body, startid, stopid,
                          tstart, tstop, rule, req, pending, transfer, error,
                          null);
  }

  public static void stopSelectedTransfers(final DbSession dbSession,
                                           final int limit, final Object map,
                                           final R66Session session,
                                           final String body,
                                           final String startid,
                                           final String stopid,
                                           final Timestamp tstart,
                                           final Timestamp tstop,
                                           final String rule, final String req,
                                           final boolean pending,
                                           final boolean transfer,
                                           final boolean error,
                                           final String host) {
    if (dbSession == null || dbSession.isDisActive()) {
      // do it without DB
      if (ClientRunner.activeRunners != null) {
        for (final ClientRunner runner : ClientRunner.activeRunners) {
          final DbTaskRunner taskRunner = runner.getTaskRunner();
          stopOneTransfer(taskRunner, map, session, body);
        }
      }
      if (CommanderNoDb.todoList != null) {
        CommanderNoDb.todoList.clear();
      }
      return;
    }
    DbPreparedStatement preparedStatement = null;
    try {
      preparedStatement = DbTaskRunner
          .getFilterPrepareStatement(dbSession, limit, true, startid, stopid,
                                     tstart, tstop, rule, req, pending,
                                     transfer, error, false, false, host);
      preparedStatement.executeQuery();
      while (preparedStatement.getNext()) {
        final DbTaskRunner taskRunner =
            DbTaskRunner.getFromStatement(preparedStatement);
        stopOneTransfer(taskRunner, map, session, body);
      }
      preparedStatement.realClose();
    } catch (final WaarpDatabaseException e) {
      if (preparedStatement != null) {
        preparedStatement.realClose();
      }
      logger.error(Messages.getString("TransferUtils.14"),
                   e.getMessage()); //$NON-NLS-1$
    }
  }

  /**
   * Method to delete the temporary file
   *
   * @param taskRunner
   * @param map
   * @param session
   * @param body
   */
  @SuppressWarnings("unchecked")
  public static void cleanOneTransfer(final DbTaskRunner taskRunner,
                                      final Object map,
                                      final R66Session session,
                                      final String body) {
    if (!taskRunner.isSender() && !taskRunner.isAllDone()) {
      String name = null;
      try {
        if (session != null) {
          session.getDir().changeDirectory("/");
          session.setBadRunner(taskRunner, ErrorCode.QueryAlreadyFinished);
          final R66File file = session.getFile();
          if (file != null) {
            name = file.getFile();
            if (file.exists()) {
              logger.info(
                  Messages.getString("TransferUtils.18") + file); //$NON-NLS-1$
              if (!file.delete()) {
                logger.warn(Messages.getString("TransferUtils.19") +
                            file); //$NON-NLS-1$
              } else {
                taskRunner.setRankAtStartup(0);
                taskRunner.setFilename("###FILE DELETED### " + name);
                taskRunner.update();
              }
            } else if (!name.contains("###FILE DELETED### ")) {
              taskRunner.setRankAtStartup(0);
              taskRunner.setFilename("###FILE DELETED### " + name);
              taskRunner.update();
            }
          }
        }
      } catch (final CommandAbstractException e1) {
        logger.warn(Messages.getString("TransferUtils.19") + name,
                    e1); //$NON-NLS-1$
      } catch (final WaarpDatabaseException ignored) {
        // nothing
      }
    }
    if (map != null) {
      if (map instanceof Map) {
        ((Map<String, String>) map)
            .put(taskRunner.getKey(), taskRunner.getJsonAsString());
      } else if (map instanceof StringBuilder) {
        final LocalChannelReference lcr =
            Configuration.configuration.getLocalTransaction()
                                       .getFromRequest(taskRunner.getKey());
        ((StringBuilder) map).append(taskRunner.toSpecializedHtml(session, body,
                                                                  lcr != null?
                                                                      Messages
                                                                          .getString(
                                                                              "HttpSslHandler.Active") :
                                                                      Messages
                                                                          .getString(
                                                                              "HttpSslHandler.NotActive")));
      }
    }
  }

  /**
   * Clean all selected transfers
   *
   * @param dbSession
   * @param limit
   * @param map
   * @param session
   * @param body
   * @param startid
   * @param stopid
   * @param tstart
   * @param tstop
   * @param rule
   * @param req
   * @param pending
   * @param transfer
   * @param error
   *
   * @return the associated StringBuilder if the one given as parameter is not
   *     null
   */
  public static void cleanSelectedTransfers(final DbSession dbSession,
                                            final int limit, final Object map,
                                            final R66Session session,
                                            final String body,
                                            final String startid,
                                            final String stopid,
                                            final Timestamp tstart,
                                            final Timestamp tstop,
                                            final String rule, final String req,
                                            final boolean pending,
                                            final boolean transfer,
                                            final boolean error) {
    cleanSelectedTransfers(dbSession, limit, map, session, body, startid,
                           stopid, tstart, tstop, rule, req, pending, transfer,
                           error, null);
  }

  public static void cleanSelectedTransfers(final DbSession dbSession,
                                            final int limit, final Object map,
                                            final R66Session session,
                                            final String body,
                                            final String startid,
                                            final String stopid,
                                            final Timestamp tstart,
                                            final Timestamp tstop,
                                            final String rule, final String req,
                                            final boolean pending,
                                            final boolean transfer,
                                            final boolean error,
                                            final String host) {
    if (dbSession == null || dbSession.isDisActive()) {
      // do it without DB
      if (ClientRunner.activeRunners != null) {
        for (final ClientRunner runner : ClientRunner.activeRunners) {
          final DbTaskRunner taskRunner = runner.getTaskRunner();
          stopOneTransfer(taskRunner, null, session, null);
          cleanOneTransfer(taskRunner, map, session, body);
        }
      }
      if (CommanderNoDb.todoList != null) {
        CommanderNoDb.todoList.clear();
      }
      return;
    }
    DbPreparedStatement preparedStatement = null;
    try {
      preparedStatement = DbTaskRunner
          .getFilterPrepareStatement(dbSession, limit, true, startid, stopid,
                                     tstart, tstop, rule, req, pending,
                                     transfer, error, false, false, host);
      preparedStatement.executeQuery();
      while (preparedStatement.getNext()) {
        final DbTaskRunner taskRunner =
            DbTaskRunner.getFromStatement(preparedStatement);
        stopOneTransfer(taskRunner, null, session, null);
        cleanOneTransfer(taskRunner, map, session, body);
      }
      preparedStatement.realClose();
    } catch (final WaarpDatabaseException e) {
      if (preparedStatement != null) {
        preparedStatement.realClose();
      }
      logger.error(Messages.getString("TransferUtils.14"),
                   e.getMessage()); //$NON-NLS-1$
    }
  }

}
