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
 * You should have received a copy of the GNU General Public License along with Waarp. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.commander;

import java.net.SocketAddress;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.client.RecvThroughHandler;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.authentication.R66Auth;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.database.data.DbTaskRunner.TASKSTEP;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotYetConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.protocol.utils.TransferUtils;

/**
 * Client Runner from a TaskRunner
 * 
 * @author Frederic Bregier
 * 
 */
public class ClientRunner extends Thread {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(ClientRunner.class);

    private static final ConcurrentHashMap<String, Integer> taskRunnerRetryHashMap = new ConcurrentHashMap<String, Integer>();

    public static ConcurrentLinkedQueue<ClientRunner> activeRunners = null;

    private final NetworkTransaction networkTransaction;

    private final DbTaskRunner taskRunner;

    private final R66Future futureRequest;

    private RecvThroughHandler handler = null;

    private boolean isSendThroughMode = false;

    private LocalChannelReference localChannelReference = null;

    public ClientRunner(NetworkTransaction networkTransaction,
            DbTaskRunner taskRunner, R66Future futureRequest) {
        this.networkTransaction = networkTransaction;
        this.taskRunner = taskRunner;
        this.futureRequest = futureRequest;
    }

    public static String hashStatus() {
        return "ClientRunner: [taskRunnerRetryHashMap: " + taskRunnerRetryHashMap.size() + " activeRunners: "
                + (activeRunners != null ? activeRunners.size() : 0) + "] ";
    }

    /**
     * @return the networkTransaction
     */
    public NetworkTransaction getNetworkTransaction() {
        return networkTransaction;
    }

    /**
     * @return the taskRunner
     */
    public DbTaskRunner getTaskRunner() {
        return taskRunner;
    }

    /**
     * @return the localChannelReference
     */
    public LocalChannelReference getLocalChannelReference() {
        return localChannelReference;
    }

    @Override
    public void run() {
        if (Configuration.configuration.isShutdown()) {
            taskRunner.changeUpdatedInfo(UpdatedInfo.TOSUBMIT);
            taskRunner.forceSaveStatus();
            return;
        }
        try {
            if (activeRunners != null) {
                activeRunners.add(this);
            }
            // fix for SelfRequest
            if (taskRunner.isSelfRequest()) {
                taskRunner.setSenderByRequestToValidate(false);
            }
            R66Future transfer;
            try {
                transfer = this.runTransfer();
            } catch (OpenR66RunnerErrorException e) {
                logger.error("Runner Error: {} {}", e.getMessage(),
                        taskRunner.toShortString());
                return;
            } catch (OpenR66ProtocolNoConnectionException e) {
                logger.error("No connection Error {}", e.getMessage());
                if (localChannelReference != null) {
                    localChannelReference.setErrorMessage(
                            ErrorCode.ConnectionImpossible.getMesg(),
                            ErrorCode.ConnectionImpossible);
                }
                taskRunner.setErrorTask(localChannelReference);
                try {
                    taskRunner.forceSaveStatus();
                    taskRunner.run();
                } catch (OpenR66RunnerErrorException e1) {
                    this.changeUpdatedInfo(UpdatedInfo.INERROR,
                            ErrorCode.ConnectionImpossible, true);
                }
                return;
            } catch (OpenR66ProtocolPacketException e) {
                logger.error("Protocol Error", e);
                return;
            } catch (OpenR66ProtocolNotYetConnectionException e) {
                logger.warn("No connection warning {}", e.getMessage());
                return;
            }
            R66Result result = transfer.getResult();
            if (result != null) {
                if (result.getCode() == ErrorCode.QueryAlreadyFinished) {
                    logger.warn(Messages.getString("Transfer.Status")
                            +
                            (transfer.isSuccess() ? Messages.getString("RequestInformation.Success") : Messages
                                    .getString("RequestInformation.Failure")) +
                            "     " + ErrorCode.QueryAlreadyFinished.getMesg() +
                            ":" +
                            (result != null ? result.toString() : "no result"));
                } else {
                    if (transfer.isSuccess()) {
                        logger.info(Messages.getString("Transfer.Status")
                                + Messages.getString("RequestInformation.Success") + "     " +
                                (result != null ? result.toString()
                                        : "no result"));
                    } else {
                        logger.error(Messages.getString("Transfer.Status")
                                + Messages.getString("RequestInformation.Failure") + "     " +
                                (result != null ? result.toString()
                                        : "no result"));
                    }
                }
            } else {
                if (transfer.isSuccess()) {
                    logger.warn(Messages.getString("Transfer.Status")
                            + Messages.getString("RequestInformation.Success") + "     no result");
                } else {
                    logger.error(Messages.getString("Transfer.Status")
                            + Messages.getString("RequestInformation.Failure") + "     no result");
                }
            }
            transfer = null;
            Thread.currentThread().setName(
                    "Finished_" + Thread.currentThread().getName());
        } finally {
            if (activeRunners != null) {
                activeRunners.remove(this);
            }
        }
    }

    /**
     * 
     * @param runner
     * @param limit
     * @return True if the task was run less than limit, else False
     */
    public boolean incrementTaskRunnerTry(DbTaskRunner runner, int limit) {
        String key = runner.getKey();
        Integer tries = taskRunnerRetryHashMap.get(key);
        logger.debug("try to find integer: " + tries);
        if (tries == null) {
            tries = Integer.valueOf(1);
        } else {
            tries = tries + 1;
        }
        if (limit <= tries) {
            taskRunnerRetryHashMap.remove(key);
            return false;
        } else {
            taskRunnerRetryHashMap.put(key, tries);
            return true;
        }
    }

    /**
     * True transfer run (can be called directly to enable exception outside any executors)
     * 
     * @return The R66Future of the transfer operation
     * @throws OpenR66RunnerErrorException
     * @throws OpenR66ProtocolNoConnectionException
     * @throws OpenR66ProtocolPacketException
     * @throws OpenR66ProtocolNotYetConnectionException
     */
    public R66Future runTransfer() throws OpenR66RunnerErrorException,
            OpenR66ProtocolNoConnectionException,
            OpenR66ProtocolPacketException,
            OpenR66ProtocolNotYetConnectionException {
        logger.debug("Start attempt Transfer");
        localChannelReference = initRequest();
        try {
            localChannelReference.getFutureValidRequest().await();
        } catch (InterruptedException e) {
        }
        if (localChannelReference.getFutureValidRequest().isSuccess()) {
            return finishTransfer(localChannelReference);
        } else if (localChannelReference.getFutureValidRequest().getResult() != null &&
                localChannelReference.getFutureValidRequest().getResult().getCode() == ErrorCode.ServerOverloaded) {
            return tryAgainTransferOnOverloaded(true, localChannelReference);
        } else
            return finishTransfer(localChannelReference);
    }

    /**
     * In case an overloaded signal is returned by the requested
     * 
     * @param retry
     *            if True, it will retry in case of overloaded remote server, else it just stops
     * @param localChannelReference
     * @return The R66Future of the transfer operation
     * @throws OpenR66RunnerErrorException
     * @throws OpenR66ProtocolNoConnectionException
     * @throws OpenR66ProtocolPacketException
     * @throws OpenR66ProtocolNotYetConnectionException
     */
    public R66Future tryAgainTransferOnOverloaded(boolean retry,
            LocalChannelReference localChannelReference)
            throws OpenR66RunnerErrorException,
            OpenR66ProtocolNoConnectionException,
            OpenR66ProtocolPacketException,
            OpenR66ProtocolNotYetConnectionException {
        if (this.localChannelReference == null) {
            this.localChannelReference = localChannelReference;
        }
        boolean incRetry = incrementTaskRunnerTry(taskRunner,
                Configuration.RETRYNB);
        logger.debug("tryAgainTransferOnOverloaded: " + retry + ":" + incRetry);
        switch (taskRunner.getUpdatedInfo()) {
            case DONE:
            case INERROR:
            case INTERRUPTED:
                break;
            default:
                this.changeUpdatedInfo(UpdatedInfo.INERROR,
                        ErrorCode.ServerOverloaded, true);
        }
        // redo if possible
        if (retry && incRetry) {
            try {
                Thread.sleep(Configuration.configuration.getConstraintLimitHandler()
                        .getSleepTime());
            } catch (InterruptedException e) {
            }
            return runTransfer();
        } else {
            if (localChannelReference == null) {
                taskRunner
                        .setLocalChannelReference(new LocalChannelReference());
            }
            taskRunner.getLocalChannelReference().setErrorMessage(
                    ErrorCode.ConnectionImpossible.getMesg(),
                    ErrorCode.ConnectionImpossible);
            this.taskRunner.setErrorTask(localChannelReference);
            this.taskRunner.run();
            throw new OpenR66ProtocolNoConnectionException(
                    "End of retry on ServerOverloaded");
        }
    }

    /**
     * Finish the transfer (called at the end of runTransfer)
     * 
     * @param localChannelReference
     * @return The R66Future of the transfer operation
     * @throws OpenR66ProtocolNotYetConnectionException
     * @throws OpenR66ProtocolPacketException
     * @throws OpenR66ProtocolNoConnectionException
     * @throws OpenR66RunnerErrorException
     */
    public R66Future finishTransfer(LocalChannelReference localChannelReference)
            throws OpenR66RunnerErrorException {
        if (this.localChannelReference == null) {
            this.localChannelReference = localChannelReference;
        }
        R66Future transfer = localChannelReference.getFutureRequest();
        try {
            transfer.await();
        } catch (InterruptedException e1) {
        }
        taskRunnerRetryHashMap.remove(taskRunner.getKey());
        logger.info("Request done with {}", (transfer.isSuccess() ? "success"
                : "error"));
        localChannelReference.getLocalChannel().close();
        // now reload TaskRunner if it still exists (light client can forget it)
        boolean isSender = taskRunner.isSender();
        if (transfer.isSuccess()) {
            try {
                taskRunner.select();
            } catch (WaarpDatabaseException e) {
                logger.debug("Not a problem but cannot find at the end the task", e);
                taskRunner.setFrom(transfer.getRunner());
            }
            taskRunner.setSender(isSender);
            this.changeUpdatedInfo(UpdatedInfo.DONE, ErrorCode.CompleteOk, false);
        } else {
            try {
                taskRunner.select();
            } catch (WaarpDatabaseException e) {
                logger.debug("Not a problem but cannot find at the end the task");
                taskRunner.setFrom(transfer.getRunner());
            }
            taskRunner.setSender(isSender);
            // Case when we were interrupted
            if (transfer.getResult() == null) {
                switch (taskRunner.getUpdatedInfo()) {
                    case DONE:
                        R66Result ok = new R66Result(null, true,
                                ErrorCode.CompleteOk, taskRunner);
                        transfer.setResult(ok);
                        transfer.setSuccess();
                        this.changeUpdatedInfo(UpdatedInfo.DONE,
                                ErrorCode.CompleteOk, false);
                        break;
                    case INERROR:
                    case INTERRUPTED:
                    default:
                        R66Result error = new R66Result(null, true,
                                ErrorCode.Internal, taskRunner);
                        transfer.setResult(error);
                        transfer.cancel();
                        this.changeUpdatedInfo(UpdatedInfo.INERROR,
                                ErrorCode.Internal, false);
                }
                return transfer;
            }
            if (transfer.getResult().getCode() == ErrorCode.QueryAlreadyFinished) {
                // check if post task to execute
                logger.warn("WARN QueryAlreadyFinished:     " +
                        transfer.toString() + "     " +
                        taskRunner.toShortString());
                try {
                    TransferUtils.finalizeTaskWithNoSession(taskRunner,
                            localChannelReference);
                } catch (OpenR66RunnerErrorException e) {
                    this.taskRunner.changeUpdatedInfo(UpdatedInfo.INERROR);
                    this.taskRunner.forceSaveStatus();
                }
            } else {
                switch (taskRunner.getUpdatedInfo()) {
                    case DONE:
                    case INERROR:
                    case INTERRUPTED:
                    case TOSUBMIT:
                        break;
                    default:
                        this.changeUpdatedInfo(UpdatedInfo.INERROR,
                                transfer.getResult().getCode(), false);
                }
            }
        }
        return transfer;
    }

    /**
     * Initialize the request
     * 
     * @return the localChannelReference holding the transfer request
     * @throws OpenR66ProtocolNoConnectionException
     * @throws OpenR66RunnerErrorException
     * @throws OpenR66ProtocolPacketException
     * @throws OpenR66ProtocolNotYetConnectionException
     */
    public LocalChannelReference initRequest()
            throws OpenR66ProtocolNoConnectionException,
            OpenR66RunnerErrorException, OpenR66ProtocolPacketException,
            OpenR66ProtocolNotYetConnectionException {
        this.changeUpdatedInfo(UpdatedInfo.RUNNING, ErrorCode.Running, true);
        long id = taskRunner.getSpecialId();
        String tid;
        if (id == DbConstant.ILLEGALVALUE) {
            tid = taskRunner.getRuleId() + "_" + taskRunner.getMode() +
                    "_NEWTRANSFER";
        } else {
            tid = taskRunner.getRuleId() + "_" + taskRunner.getMode() + "_" +
                    id;
        }
        Thread.currentThread().setName(tid);
        logger.debug("Will run {}", this.taskRunner);
        boolean restartPost = false;
        if (taskRunner.getGloballaststep() == TASKSTEP.POSTTASK.ordinal()) {
            // Send a validation to requested
            if (!taskRunner.isSelfRequested()) {
                // restart
                restartPost = true;
            }
        }
        if (taskRunner.isSelfRequested()) {
            // Don't have to restart a task for itself (or should use requester)
            logger.warn("Requested host cannot initiate itself the request");
            this.changeUpdatedInfo(UpdatedInfo.INERROR,
                    ErrorCode.LoopSelfRequestedHost, true);
            throw new OpenR66ProtocolNoConnectionException(
                    "Requested host cannot initiate itself the request");
        }
        DbHostAuth host = null;
        try {
            host = new DbHostAuth(taskRunner.getRequested());
        } catch (WaarpDatabaseException e) {
            logger.error("Requested host cannot be found: " +
                    taskRunner.getRequested());
            this.changeUpdatedInfo(UpdatedInfo.INERROR, ErrorCode.NotKnownHost, true);
            throw new OpenR66ProtocolNoConnectionException(
                    "Requested host cannot be found " +
                            taskRunner.getRequested());
        }
        if (host.isClient()) {
            logger.warn("Cannot initiate a connection with a client: {}", host);
            this.changeUpdatedInfo(UpdatedInfo.INERROR,
                    ErrorCode.ConnectionImpossible, true);
            throw new OpenR66ProtocolNoConnectionException(
                    "Cannot connect to client " + host.toString());
        }
        SocketAddress socketAddress = host.getSocketAddress();
        boolean isSSL = host.isSsl();

        LocalChannelReference localChannelReference = networkTransaction
                .createConnectionWithRetry(socketAddress, isSSL, futureRequest);
        taskRunner.setLocalChannelReference(localChannelReference);
        if (localChannelReference == null) {
            // propose to redo
            // See if reprogramming is ok (not too many tries)
            String retry;
            if (incrementTaskRunnerTry(taskRunner, Configuration.RETRYNB)) {
                logger.debug("Will retry since Cannot connect to {}", host);
                retry = " but will retry";
                // now wait
                try {
                    Thread.sleep(Configuration.configuration.getDelayRetry());
                } catch (InterruptedException e) {
                    logger.debug(
                            "Will not retry since limit of connection attemtps is reached for {}",
                            host);
                    retry = " and retries limit is reached so stop here";
                    this.changeUpdatedInfo(UpdatedInfo.INERROR,
                            ErrorCode.ConnectionImpossible, true);
                    taskRunner
                            .setLocalChannelReference(new LocalChannelReference());
                    throw new OpenR66ProtocolNoConnectionException(
                            "Cannot connect to server " + host.toString() + retry);
                }
                this.changeUpdatedInfo(UpdatedInfo.TOSUBMIT,
                        ErrorCode.ConnectionImpossible, true);
                throw new OpenR66ProtocolNotYetConnectionException(
                        "Cannot connect to server " + host.toString() + retry);
            } else {
                logger.debug(
                        "Will not retry since limit of connection attemtps is reached for {}",
                        host);
                retry = " and retries limit is reached so stop here";
                this.changeUpdatedInfo(UpdatedInfo.INERROR,
                        ErrorCode.ConnectionImpossible, true);
                taskRunner
                        .setLocalChannelReference(new LocalChannelReference());
                // set this server as being in shutdown status
                NetworkTransaction.proposeShutdownNetworkChannel(socketAddress);
                throw new OpenR66ProtocolNoConnectionException(
                        "Cannot connect to server " + host.toString() + retry);
            }
        }
        socketAddress = null;
        if (handler != null) {
            localChannelReference.setRecvThroughHandler(handler);
        }
        localChannelReference.setSendThroughMode(isSendThroughMode);
        if (restartPost) {
            RequestPacket request = taskRunner.getRequest();
            logger.debug("Will send request {} ", request);
            localChannelReference.setClientRunner(this);
            localChannelReference.sessionNewState(R66FiniteDualStates.REQUESTR);
            try {
                ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                        request, true);
            } catch (OpenR66ProtocolPacketException e) {
                // propose to redo
                logger.warn("Cannot transfer request to " + host.toString());
                this.changeUpdatedInfo(UpdatedInfo.INTERRUPTED,
                        ErrorCode.Internal, true);
                localChannelReference.getLocalChannel().close();
                localChannelReference = null;
                host = null;
                request = null;
                throw e;
            }
            logger.debug("Wait for request to {}", host);
            request = null;
            host = null;
            return localChannelReference;
        }
        // If Requester is NOT Sender, and if TransferTask then decrease now if
        // possible the rank
        if (!taskRunner.isSender() &&
                (taskRunner.getGloballaststep() == TASKSTEP.TRANSFERTASK
                        .ordinal())) {
            logger.debug(
                    "Requester is not Sender so decrease if possible the rank {}",
                    taskRunner);
            taskRunner.restartRank();
            taskRunner.forceSaveStatus();
            logger.debug(
                    "Requester is not Sender so new rank is " +
                            taskRunner.getRank() + " {}", taskRunner);
        }
        RequestPacket request = taskRunner.getRequest();
        request.setLimit(localChannelReference.getChannelLimit(
                    taskRunner.isSender()));
        logger.debug("Will send request {} {}", request, localChannelReference);
        localChannelReference.setClientRunner(this);
        localChannelReference.sessionNewState(R66FiniteDualStates.REQUESTR);
        try {
            ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                    request, true);
        } catch (OpenR66ProtocolPacketException e) {
            // propose to redo
            logger.warn("Cannot transfer request to " + host.toString());
            this.changeUpdatedInfo(UpdatedInfo.INTERRUPTED, ErrorCode.Internal, true);
            localChannelReference.getLocalChannel().close();
            localChannelReference = null;
            host = null;
            request = null;
            throw e;
        }
        logger.debug("Wait for request to {}", host);
        request = null;
        host = null;
        return localChannelReference;
    }

    /**
     * Change the UpdatedInfo of the current runner
     * 
     * @param info
     */
    public void changeUpdatedInfo(AbstractDbData.UpdatedInfo info,
            ErrorCode code, boolean force) {
        this.taskRunner.changeUpdatedInfo(info);
        this.taskRunner.setErrorExecutionStatus(code);
        if (force) {
            this.taskRunner.forceSaveStatus();
        } else {
            try {
                this.taskRunner.saveStatus();
            } catch (OpenR66RunnerErrorException e) {
            }
        }
    }

    /**
     * @param handler
     *            the handler to set
     */
    public void setRecvThroughHandler(RecvThroughHandler handler) {
        this.handler = handler;
    }

    public void setSendThroughMode() {
        isSendThroughMode = true;
    }

    public boolean getSendThroughMode() {
        return isSendThroughMode;
    }
}
