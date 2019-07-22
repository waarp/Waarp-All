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
package org.waarp.openr66.protocol.utils;

import java.sql.Timestamp;
import java.util.Map;

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
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.database.data.DbTaskRunner.TASKSTEP;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.ErrorPacket;

/**
 * Utility class for transfers
 *
 * @author Frederic Bregier
 *
 */
public class TransferUtils {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(TransferUtils.class);

    /**
     * Try to restart one Transfer Runner Task
     *
     * @param taskRunner
     * @return the associated Result
     * @throws WaarpDatabaseException
     */
    public static R66Result restartTransfer(DbTaskRunner taskRunner, LocalChannelReference lcr)
            throws WaarpDatabaseException {
        R66Result finalResult = new R66Result(null, true, ErrorCode.InitOk, taskRunner);
        if (lcr != null) {
            finalResult.setCode(ErrorCode.QueryStillRunning);
            finalResult.setOther(Messages.getString("TransferUtils.0")); //$NON-NLS-1$
        } else {
            if (taskRunner.isSendThrough()) {
                // XXX FIXME TODO cannot be restarted... Really?
                /*if (false) {
                	finalResult.code = ErrorCode.PassThroughMode;
                	finalResult.other = "Transfer cannot be restarted since it is in PassThrough mode";
                	return finalResult;
                }*/
            }
            // Transfer is not running
            // but maybe need action on database
            try {
                if (taskRunner.restart(true)) {
                    taskRunner.forceSaveStatus();
                    finalResult.setCode(ErrorCode.PreProcessingOk);
                    finalResult.setOther(Messages.getString("TransferUtils.1")); //$NON-NLS-1$
                } else {
                    if (taskRunner.isSelfRequested() &&
                            (taskRunner.getGloballaststep() < TASKSTEP.POSTTASK.ordinal())) {
                        // send a VALID packet with VALID code to the requester except if client
                        DbHostAuth host = R66Auth.getServerAuth(DbConstant.admin.getSession(),
                                taskRunner.getRequester());
                        if (host == null || host.isClient()) {
                            // cannot be relaunch from there
                            finalResult.setCode(ErrorCode.ConnectionImpossible);
                            finalResult.setOther(Messages.getString("TransferUtils.2")); //$NON-NLS-1$
                            logger.warn(Messages.getString("TransferUtils.3")); //$NON-NLS-1$
                        } else {
                            R66Future result = new R66Future(true);
                            logger.info(Messages.getString("TransferUtils.4") + taskRunner.toShortString()); //$NON-NLS-1$
                            RequestTransfer requestTransfer =
                                    new RequestTransfer(result, taskRunner.getSpecialId(),
                                            taskRunner.getRequested(), taskRunner.getRequester(),
                                            false, false, true,
                                            Configuration.configuration.getInternalRunner().
                                                    getNetworkTransaction());
                            requestTransfer.run();
                            result.awaitUninterruptibly();
                            R66Result finalValue = result.getResult();
                            switch (finalValue.getCode()) {
                                case QueryStillRunning:
                                    finalResult.setCode(ErrorCode.QueryStillRunning);
                                    finalResult.setOther(Messages.getString("TransferUtils.5")); //$NON-NLS-1$
                                    break;
                                case Running:
                                    finalResult.setCode(ErrorCode.Running);
                                    finalResult.setOther(Messages.getString("TransferUtils.6")); //$NON-NLS-1$
                                    break;
                                case PreProcessingOk:
                                    finalResult.setCode(ErrorCode.PreProcessingOk);
                                    finalResult.setOther(Messages.getString("TransferUtils.7")); //$NON-NLS-1$
                                    break;
                                case CompleteOk:
                                    finalResult.setCode(ErrorCode.CompleteOk);
                                    finalResult.setOther(Messages.getString("TransferUtils.8")); //$NON-NLS-1$
                                    taskRunner.setPostTask();
                                    TransferUtils.finalizeTaskWithNoSession(taskRunner, lcr);
                                    taskRunner.setErrorExecutionStatus(ErrorCode.QueryAlreadyFinished);
                                    taskRunner.forceSaveStatus();
                                    break;
                                case RemoteError:
                                    finalResult.setCode(ErrorCode.RemoteError);
                                    finalResult.setOther(Messages.getString("TransferUtils.9")); //$NON-NLS-1$
                                    break;
                                default:
                                    finalResult.setCode(ErrorCode.Internal);
                                    finalResult.setOther(Messages.getString("TransferUtils.10")); //$NON-NLS-1$
                                    break;
                            }
                        }
                    } else {
                        finalResult.setCode(ErrorCode.CompleteOk);
                        finalResult.setOther(Messages.getString("TransferUtils.11")); //$NON-NLS-1$
                        taskRunner.setPostTask();
                        TransferUtils.finalizeTaskWithNoSession(taskRunner, lcr);
                        taskRunner.setErrorExecutionStatus(ErrorCode.QueryAlreadyFinished);
                        taskRunner.forceSaveStatus();
                    }
                }
            } catch (OpenR66RunnerErrorException e) {
                finalResult.setCode(ErrorCode.PreProcessingOk);
                finalResult.setOther(Messages.getString("TransferUtils.1")); //$NON-NLS-1$
            }
        }
        return finalResult;
    }

    /**
     * Finalize a local task since only Post action has to be done
     *
     * @param taskRunner
     * @param localChannelReference
     * @throws OpenR66RunnerErrorException
     */
    public static void finalizeTaskWithNoSession(DbTaskRunner taskRunner,
                                                 LocalChannelReference localChannelReference)
            throws OpenR66RunnerErrorException {
        R66Session session = new R66Session();
        session.setStatus(50);
        String remoteId = taskRunner.isSelfRequested() ?
                taskRunner.getRequester() :
                taskRunner.getRequested();
        session.getAuth().specialNoSessionAuth(false, remoteId);
        session.setNoSessionRunner(taskRunner, localChannelReference);
        if (taskRunner.isSender()) {
            // Change dir
            try {
                session.getDir().changeDirectory(taskRunner.getRule().getSendPath());
            } catch (CommandAbstractException e) {
                throw new OpenR66RunnerErrorException(e);
            }
        } else {
            // Change dir
            try {
                session.getDir().changeDirectory(taskRunner.getRule().getWorkPath());
            } catch (CommandAbstractException e) {
                throw new OpenR66RunnerErrorException(e);
            }
        }
        try {
            try {
                session.setFileAfterPreRunner(false);
            } catch (CommandAbstractException e) {
                throw new OpenR66RunnerErrorException(e);
            }
        } catch (OpenR66RunnerErrorException e) {
            logger.error(Messages.getString("TransferUtils.27"), taskRunner.getFilename()); //$NON-NLS-1$
            taskRunner.changeUpdatedInfo(UpdatedInfo.INERROR);
            taskRunner.setErrorExecutionStatus(ErrorCode.FileNotFound);
            try {
                taskRunner.update();
            } catch (WaarpDatabaseException e1) {
            }
            throw new OpenR66RunnerErrorException(Messages.getString("TransferUtils.28"), e); //$NON-NLS-1$
        }
        R66File file = session.getFile();
        R66Result finalValue = new R66Result(null, true, ErrorCode.CompleteOk, taskRunner);
        finalValue.setFile(file);
        finalValue.setRunner(taskRunner);
        taskRunner.finishTransferTask(ErrorCode.TransferOk);
        try {
            taskRunner.finalizeTransfer(localChannelReference, file, finalValue, true);
        } catch (OpenR66ProtocolSystemException e) {
            logger.error(Messages.getString("TransferUtils.29"), taskRunner.toShortString()); //$NON-NLS-1$
            taskRunner.changeUpdatedInfo(UpdatedInfo.INERROR);
            taskRunner.setErrorExecutionStatus(ErrorCode.Internal);
            try {
                taskRunner.update();
            } catch (WaarpDatabaseException e1) {
            }
            throw new OpenR66RunnerErrorException(Messages.getString("TransferUtils.30"), e); //$NON-NLS-1$
        }
    }

    @SuppressWarnings("unchecked")
    private static void stopOneTransfer(DbTaskRunner taskRunner,
                                        Object map, R66Session session, String body) {
        LocalChannelReference lcr =
                Configuration.configuration.getLocalTransaction().
                        getFromRequest(taskRunner.getKey());
        ErrorCode result;
        ErrorCode code = ErrorCode.StoppedTransfer;
        if (lcr != null) {
            int rank = taskRunner.getRank();
            lcr.sessionNewState(R66FiniteDualStates.ERROR);
            ErrorPacket perror = new ErrorPacket(Messages.getString("TransferUtils.13") + rank, //$NON-NLS-1$
                    code.getCode(), ErrorPacket.FORWARDCLOSECODE);
            try {
                // XXX ChannelUtils.writeAbstractLocalPacket(lcr, perror);
                // inform local instead of remote
                ChannelUtils.writeAbstractLocalPacketToLocal(lcr, perror);
            } catch (Exception e) {
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
                if (taskRunner != null) {
                    if (taskRunner.stopOrCancelRunner(code)) {
                        result = ErrorCode.StoppedTransfer;
                    }
                }
            }
        }
        ErrorCode last = taskRunner.getErrorInfo();
        taskRunner.setErrorExecutionStatus(result);
        if (map != null) {
            if (map instanceof Map) {
                ((Map<String, String>) map).put(taskRunner.getKey(), taskRunner.getJsonAsString());
            } else if (map instanceof StringBuilder) {
                ((StringBuilder) map).append(taskRunner.toSpecializedHtml(
                        session,
                        body,
                        lcr != null ? Messages.getString("HttpSslHandler.Active") : Messages
                                .getString("HttpSslHandler.NotActive")));
            }
        }
        taskRunner.setErrorExecutionStatus(last);
    }

    /**
     * Stop all selected transfers
     *
     * @param dbSession
     * @param limit
     * @param builder
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
     * @return the associated StringBuilder if the one given as parameter is not null
     */
    public static void stopSelectedTransfers(DbSession dbSession, int limit,
                                             Object map, R66Session session, String body,
                                             String startid, String stopid, Timestamp tstart, Timestamp tstop, String rule,
                                             String req, boolean pending, boolean transfer, boolean error) {
        stopSelectedTransfers(dbSession, limit, map, session, body, startid, stopid, tstart, tstop,
                rule, req, pending, transfer, error, null);
    }

    public static void stopSelectedTransfers(DbSession dbSession, int limit,
                                             Object map, R66Session session, String body,
                                             String startid, String stopid, Timestamp tstart, Timestamp tstop, String rule,
                                             String req, boolean pending, boolean transfer, boolean error, String host) {
        if (dbSession == null || dbSession.isDisActive()) {
            // do it without DB
            if (ClientRunner.activeRunners != null) {
                for (ClientRunner runner : ClientRunner.activeRunners) {
                    DbTaskRunner taskRunner = runner.getTaskRunner();
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
            preparedStatement =
                    DbTaskRunner.getFilterPrepareStatement(dbSession, limit, true,
                            startid, stopid, tstart, tstop, rule, req,
                            pending, transfer, error, false, false, host);
            preparedStatement.executeQuery();
            while (preparedStatement.getNext()) {
                DbTaskRunner taskRunner = DbTaskRunner.getFromStatement(preparedStatement);
                stopOneTransfer(taskRunner, map, session, body);
            }
            preparedStatement.realClose();
            return;
        } catch (WaarpDatabaseException e) {
            if (preparedStatement != null) {
                preparedStatement.realClose();
            }
            logger.error(Messages.getString("TransferUtils.14"), e.getMessage()); //$NON-NLS-1$
            return;
        }
    }

    /**
     * Method to delete the temporary file
     *
     * @param taskRunner
     * @param builder
     * @param session
     * @param body
     */
    @SuppressWarnings("unchecked")
    public static void cleanOneTransfer(DbTaskRunner taskRunner, Object map, R66Session session, String body) {
        if (!taskRunner.isSender() && !taskRunner.isAllDone()) {
            String name = null;
            try {
                if (session != null) {
                    session.getDir().changeDirectory("/");
                    session.setBadRunner(taskRunner, ErrorCode.QueryAlreadyFinished);
                    R66File file = session.getFile();
                    if (file != null) {
                        name = file.getFile();
                        if (file.exists()) {
                            logger.info(Messages.getString("TransferUtils.18") + file); //$NON-NLS-1$
                            if (!file.delete()) {
                                logger.warn(Messages.getString("TransferUtils.19") + file); //$NON-NLS-1$
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
            } catch (CommandAbstractException e1) {
                logger.warn(Messages.getString("TransferUtils.19") + name, e1); //$NON-NLS-1$
            } catch (WaarpDatabaseException e) {
            }
        }
        if (map != null) {
            if (map instanceof Map) {
                ((Map<String, String>) map).put(taskRunner.getKey(), taskRunner.getJsonAsString());
            } else if (map instanceof StringBuilder) {
                LocalChannelReference lcr =
                        Configuration.configuration.getLocalTransaction().
                                getFromRequest(taskRunner.getKey());
                ((StringBuilder) map).append(taskRunner.toSpecializedHtml(
                        session,
                        body,
                        lcr != null ? Messages.getString("HttpSslHandler.Active") : Messages
                                .getString("HttpSslHandler.NotActive")));
            }
        }
    }

    /**
     * Clean all selected transfers
     *
     * @param dbSession
     * @param limit
     * @param builder
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
     * @return the associated StringBuilder if the one given as parameter is not null
     */
    public static void cleanSelectedTransfers(DbSession dbSession, int limit,
                                              Object map, R66Session session, String body,
                                              String startid, String stopid, Timestamp tstart, Timestamp tstop, String rule,
                                              String req, boolean pending, boolean transfer, boolean error) {
        cleanSelectedTransfers(dbSession, limit, map, session, body, startid, stopid, tstart, tstop,
                rule, req, pending, transfer, error, null);
    }

    public static void cleanSelectedTransfers(DbSession dbSession, int limit,
                                              Object map, R66Session session, String body,
                                              String startid, String stopid, Timestamp tstart, Timestamp tstop, String rule,
                                              String req, boolean pending, boolean transfer, boolean error, String host) {
        if (dbSession == null || dbSession.isDisActive()) {
            // do it without DB
            if (ClientRunner.activeRunners != null) {
                for (ClientRunner runner : ClientRunner.activeRunners) {
                    DbTaskRunner taskRunner = runner.getTaskRunner();
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
            preparedStatement =
                    DbTaskRunner.getFilterPrepareStatement(dbSession, limit, true,
                            startid, stopid, tstart, tstop, rule, req,
                            pending, transfer, error, false, false, host);
            preparedStatement.executeQuery();
            while (preparedStatement.getNext()) {
                DbTaskRunner taskRunner = DbTaskRunner.getFromStatement(preparedStatement);
                stopOneTransfer(taskRunner, null, session, null);
                cleanOneTransfer(taskRunner, map, session, body);
            }
            preparedStatement.realClose();
            return;
        } catch (WaarpDatabaseException e) {
            if (preparedStatement != null) {
                preparedStatement.realClose();
            }
            logger.error(Messages.getString("TransferUtils.14"), e.getMessage()); //$NON-NLS-1$
            return;
        }
    }

}
