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
package org.waarp.openr66.commander;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.client.RecvThroughHandler;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.database.data.DbTaskRunner.TASKSTEP;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoSslException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotAuthenticatedException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotYetConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.protocol.utils.TransferUtils;

import java.io.File;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Client Runner from a TaskRunner
 */
public class ClientRunner extends Thread {
  private static final String CANNOT_CONNECT_TO_SERVER =
      "Cannot connect to server ";

  private static final String REQUEST_INFORMATION_FAILURE =
      "RequestInformation.Failure";

  private static final String REQUEST_INFORMATION_SUCCESS =
      "RequestInformation.Success";

  private static final String TRANSFER_STATUS = "Transfer.Status";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(ClientRunner.class);

  private static final ConcurrentHashMap<String, Integer>
      taskRunnerRetryHashMap = new ConcurrentHashMap<String, Integer>();

  public static ConcurrentLinkedQueue<ClientRunner> activeRunners;

  private final NetworkTransaction networkTransaction;

  private final DbTaskRunner taskRunner;

  private final R66Future futureRequest;

  private RecvThroughHandler handler;

  private boolean isSendThroughMode;

  private LocalChannelReference localChannelReference;

  private final String nameTask;

  private boolean limitRetryConnection = true;

  public ClientRunner(final NetworkTransaction networkTransaction,
                      final DbTaskRunner taskRunner,
                      final R66Future futureRequest) {
    this.networkTransaction = networkTransaction;
    this.taskRunner = taskRunner;
    this.futureRequest = futureRequest;
    setDaemon(true);
    nameTask = "Client_Runner_" + taskRunner.getKey();
    setName(nameTask);
  }

  public static String hashStatus() {
    return "ClientRunner: [taskRunnerRetryHashMap: " +
           taskRunnerRetryHashMap.size() + " activeRunners: " +
           (activeRunners != null? activeRunners.size() :
               Configuration.configuration.getInternalRunner()
                                          .nbInternalRunner()) + "] ";
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
    if (Configuration.configuration.isShutdown() || Thread.interrupted()) {
      taskRunner.changeUpdatedInfo(UpdatedInfo.TOSUBMIT);
      taskRunner.forceSaveStatus();
      return;
    }
    boolean status = false;
    try {
      if (activeRunners != null) {
        activeRunners.add(this);
      }
      // fix for SelfRequest
      if (taskRunner.isSelfRequest()) {
        taskRunner.setSenderByRequestToValidate(false);
      }
      // Try to check if file still exists in send not self not through mode
      if (taskRunner.isSender() && !taskRunner.isSelfRequest() &&
          !taskRunner.isSendThrough()) {
        try {
          final R66Session session = new R66Session();
          session.setReady(true);
          final boolean ssl = Configuration.configuration.isUseSSL();
          session.getAuth().specialNoSessionAuth(ssl,
                                                 Configuration.configuration
                                                     .getHostId(ssl));
          final DbTaskRunner reloaded =
              new DbTaskRunner(session, taskRunner.getRule(),
                               taskRunner.getSpecialId(),
                               taskRunner.getRequester(),
                               taskRunner.getRequested());
          reloaded.setSender(taskRunner.isSender());
          session.setRunner(reloaded);
          session.setBlockSize(reloaded.getBlocksize());
          final File file = new File(reloaded.getFullFilePath());
          if (!file.isFile()) {
            logger.warn("File not found: {}", file.getAbsolutePath());
            // File does no more exist => error
            reloaded.changeUpdatedInfo(UpdatedInfo.INERROR);
            reloaded.setErrorExecutionStatus(ErrorCode.FileNotFound);
            logger
                .error("Runner Error: {} {}", ErrorCode.FileNotFound.getMesg(),
                       taskRunner.toShortString());
            reloaded.setErrorTask();
            reloaded.update();
            return;
          }
          status = true;
        } catch (final CommandAbstractException e) {
          if (Configuration.configuration.isShutdown()) {
            // ignore since shutdown
            logger.warn(e.getMessage());
          } else {
            // Wrong path? Ignore
            logger.warn(e);
          }
        } catch (final OpenR66RunnerErrorException e) {
          if (Configuration.configuration.isShutdown()) {
            // ignore since shutdown
            logger.warn(e.getMessage());
          } else {
            // Wrong run error? Ignore
            logger.warn(e);
          }
        } catch (final WaarpDatabaseException e) {
          if (Configuration.configuration.isShutdown()) {
            // ignore since shutdown
            logger.warn(e.getMessage());
          } else {
            // Wrong dbtask? Ignore
            logger.warn(e);
          }
        } catch (final OpenR66ProtocolNoSslException e) {
          if (Configuration.configuration.isShutdown()) {
            // ignore since shutdown
            logger.warn(e.getMessage());
          } else {
            // Wrong ssl? Ignore
            logger.warn(e);
          }
        }
      } else {
        status = true;
      }
      if (Configuration.configuration.isShutdown() || Thread.interrupted() ||
          !status) {
        taskRunner.changeUpdatedInfo(UpdatedInfo.TOSUBMIT);
        taskRunner.forceSaveStatus();
        return;
      }
      final R66Future transfer;
      try {
        transfer = runTransfer();
      } catch (final OpenR66RunnerErrorException e) {
        logger.error("Runner Error: {} {}", e.getMessage(),
                     taskRunner.toShortString());
        return;
      } catch (final OpenR66ProtocolNoConnectionException e) {
        logger.error("No connection Error {}", e.getMessage());
        if (localChannelReference != null) {
          localChannelReference
              .setErrorMessage(ErrorCode.ConnectionImpossible.getMesg(),
                               ErrorCode.ConnectionImpossible);
        }
        taskRunner.setErrorTask();
        try {
          taskRunner.forceSaveStatus();
          taskRunner.run();
        } catch (final OpenR66RunnerErrorException e1) {
          changeUpdatedInfo(UpdatedInfo.INERROR, ErrorCode.ConnectionImpossible,
                            true);
        }
        return;
      } catch (final OpenR66ProtocolPacketException e) {
        logger.error("Protocol Error", e);
        return;
      } catch (final OpenR66ProtocolNotYetConnectionException e) {
        logger.warn("No connection warning {}", e.getMessage());
        return;
      }
      final R66Result result = transfer.getResult();
      if (result != null) {
        if (result.getCode() == ErrorCode.QueryAlreadyFinished) {
          logger.warn(Messages.getString(TRANSFER_STATUS) +
                      (transfer.isSuccess()?
                          Messages.getString(REQUEST_INFORMATION_SUCCESS) :
                          Messages.getString(REQUEST_INFORMATION_FAILURE)) +
                      "     " + ErrorCode.QueryAlreadyFinished.getMesg() + ':' +
                      result.toString());
        } else {
          if (transfer.isSuccess()) {
            logger.info("{}{}     {}", Messages.getString(TRANSFER_STATUS),
                        Messages.getString(REQUEST_INFORMATION_SUCCESS),
                        result);
          } else {
            logger.error(Messages.getString(TRANSFER_STATUS) +
                         Messages.getString(REQUEST_INFORMATION_FAILURE) +
                         "     " + result.toString());
          }
        }
      } else {
        if (transfer.isSuccess()) {
          logger.warn(Messages.getString(TRANSFER_STATUS) +
                      Messages.getString(REQUEST_INFORMATION_SUCCESS) +
                      "     no result");
        } else {
          logger.error(Messages.getString(TRANSFER_STATUS) +
                       Messages.getString(REQUEST_INFORMATION_FAILURE) +
                       "     no result");
        }
      }
    } finally {
      if (activeRunners != null) {
        activeRunners.remove(this);
      }
      setName("Finished_" + nameTask);
    }
  }

  /**
   * @param runner
   * @param limit
   *
   * @return True if the task was run less than limit, else False
   */
  public boolean incrementTaskRunnerTry(final DbTaskRunner runner,
                                        final int limit) {
    if (!isLimitRetryConnection()) {
      return true;
    }
    final String key = runner.getKey();
    Integer tries = taskRunnerRetryHashMap.get(key);
    logger.debug("try to find integer: {}", tries);
    if (tries == null) {
      tries = 1;
    } else {
      tries += 1;
    }
    logger.debug("Check: {} vs {}: {}", tries, limit, limit <= tries);
    if (limit <= tries || Thread.interrupted()) {
      taskRunnerRetryHashMap.remove(key);
      return false;
    } else {
      taskRunnerRetryHashMap.put(key, tries);
      return true;
    }
  }

  /**
   * True transfer run (can be called directly to enable exception outside any
   * executors)
   *
   * @return The R66Future of the transfer operation
   *
   * @throws OpenR66RunnerErrorException
   * @throws OpenR66ProtocolNoConnectionException
   * @throws OpenR66ProtocolPacketException
   * @throws OpenR66ProtocolNotYetConnectionException
   */
  public R66Future runTransfer()
      throws OpenR66RunnerErrorException, OpenR66ProtocolNoConnectionException,
             OpenR66ProtocolPacketException,
             OpenR66ProtocolNotYetConnectionException {
    logger.debug("Start attempt Transfer");
    localChannelReference = initRequest();
    localChannelReference.getFutureValidRequest().awaitOrInterruptible(
        Configuration.configuration.getTimeoutCon());
    if (localChannelReference.getFutureValidRequest().isSuccess()) {
      return finishTransfer(localChannelReference);
    } else if (
        localChannelReference.getFutureValidRequest().getResult() != null &&
        localChannelReference.getFutureValidRequest().getResult().getCode() ==
        ErrorCode.ServerOverloaded) {
      return tryAgainTransferOnOverloaded(true, localChannelReference);
    } else {
      return finishTransfer(localChannelReference);
    }
  }

  /**
   * In case an overloaded signal is returned by the requested
   *
   * @param retry if True, it will retry in case of overloaded remote
   *     server, else it just stops
   * @param localChannelReference
   *
   * @return The R66Future of the transfer operation
   *
   * @throws OpenR66RunnerErrorException
   * @throws OpenR66ProtocolNoConnectionException
   * @throws OpenR66ProtocolPacketException
   * @throws OpenR66ProtocolNotYetConnectionException
   */
  public R66Future tryAgainTransferOnOverloaded(final boolean retry,
                                                final LocalChannelReference localChannelReference)
      throws OpenR66RunnerErrorException, OpenR66ProtocolNoConnectionException,
             OpenR66ProtocolPacketException,
             OpenR66ProtocolNotYetConnectionException {
    if (this.localChannelReference == null) {
      this.localChannelReference = localChannelReference;
    }
    final boolean incRetry =
        incrementTaskRunnerTry(taskRunner, Configuration.RETRYNB);
    logger.debug("tryAgainTransferOnOverloaded: {}:{}", retry, incRetry);
    switch (taskRunner.getUpdatedInfo()) {
      case DONE:
      case INERROR:
      case INTERRUPTED:
        break;
      default:
        changeUpdatedInfo(UpdatedInfo.INERROR, ErrorCode.ServerOverloaded,
                          true);
    }
    // redo if possible
    if (retry && incRetry) {
      try {
        Thread.sleep(Configuration.configuration.getConstraintLimitHandler()
                                                .getSleepTime());
      } catch (final InterruptedException e) {//NOSONAR
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      }
      return runTransfer();
    } else {
      if (localChannelReference == null) {
        taskRunner.setLocalChannelReference(new LocalChannelReference());
      }
      taskRunner.getLocalChannelReference()
                .setErrorMessage(ErrorCode.ConnectionImpossible.getMesg(),
                                 ErrorCode.ConnectionImpossible);
      taskRunner.setErrorTask();
      taskRunner.run();
      throw new OpenR66ProtocolNoConnectionException(
          "End of retry on ServerOverloaded");
    }
  }

  /**
   * Finish the transfer (called at the end of runTransfer)
   *
   * @param localChannelReference
   *
   * @return The R66Future of the transfer operation
   */
  public R66Future finishTransfer(
      final LocalChannelReference localChannelReference) {
    if (this.localChannelReference == null) {
      this.localChannelReference = localChannelReference;
    }
    final R66Future transfer = localChannelReference.getFutureRequest();
    transfer.awaitOrInterruptible();
    taskRunnerRetryHashMap.remove(taskRunner.getKey());
    logger.info("Request done with {}",
                transfer.isSuccess()? "success" : "error");
    localChannelReference.close();
    // now reload TaskRunner if it still exists (light client can forget it)
    final boolean isSender = taskRunner.isSender();
    if (transfer.isSuccess()) {
      try {
        taskRunner.select();
      } catch (final WaarpDatabaseException e) {
        logger.debug("Not a problem but cannot find at the end the task", e);
        taskRunner.setFrom(transfer.getRunner());
      }
      taskRunner.setSender(isSender);
      changeUpdatedInfo(UpdatedInfo.DONE, ErrorCode.CompleteOk, false);
    } else {
      try {
        taskRunner.select();
      } catch (final WaarpDatabaseException e) {
        logger.debug("Not a problem but cannot find at the end the task");
        taskRunner.setFrom(transfer.getRunner());
      }
      taskRunner.setSender(isSender);
      // Case when we were interrupted
      if (transfer.getResult() == null) {
        switch (taskRunner.getUpdatedInfo()) {
          case DONE:
            final R66Result ok =
                new R66Result(null, true, ErrorCode.CompleteOk, taskRunner);
            transfer.setResult(ok);
            transfer.setSuccess();
            changeUpdatedInfo(UpdatedInfo.DONE, ErrorCode.CompleteOk, false);
            break;
          case INERROR:
          case INTERRUPTED:
          default:
            final R66Result error =
                new R66Result(null, true, ErrorCode.Internal, taskRunner);
            transfer.setResult(error);
            transfer.cancel();
            changeUpdatedInfo(UpdatedInfo.INERROR, ErrorCode.Internal, false);
        }
        return transfer;
      }
      if (transfer.getResult().getCode() == ErrorCode.QueryAlreadyFinished) {
        // check if post task to execute
        logger.warn("WARN QueryAlreadyFinished:     " + transfer + "     " +
                    taskRunner.toShortString());
        try {
          TransferUtils
              .finalizeTaskWithNoSession(taskRunner, localChannelReference);
        } catch (final OpenR66RunnerErrorException e) {
          taskRunner.changeUpdatedInfo(UpdatedInfo.INERROR);
          taskRunner.forceSaveStatus();
        }
      } else {
        switch (taskRunner.getUpdatedInfo()) {
          case DONE:
          case INERROR:
          case INTERRUPTED:
          case TOSUBMIT:
            break;
          default:
            changeUpdatedInfo(UpdatedInfo.INERROR,
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
   *
   * @throws OpenR66ProtocolNoConnectionException
   * @throws OpenR66ProtocolPacketException
   * @throws OpenR66ProtocolNotYetConnectionException
   */
  public LocalChannelReference initRequest()
      throws OpenR66ProtocolNoConnectionException,
             OpenR66ProtocolPacketException,
             OpenR66ProtocolNotYetConnectionException {
    changeUpdatedInfo(UpdatedInfo.RUNNING, ErrorCode.Running, true);
    final long id = taskRunner.getSpecialId();
    final String tid;
    if (id == DbConstantR66.ILLEGALVALUE) {
      tid = "Runner_" + taskRunner.getRuleId() + '_' + taskRunner.getMode() +
            "_NEWTRANSFER";
    } else {
      tid = "Runner_" + taskRunner.getRuleId() + '_' + taskRunner.getMode() +
            '_' + id;
    }
    setName(tid);
    logger.debug("Will run {}", taskRunner);
    boolean restartPost = false;
    if (taskRunner.getGloballaststep() == TASKSTEP.POSTTASK.ordinal()) {
      // Send a validation to requested
      if (!taskRunner.isRequestOnRequested()) {
        // restart
        restartPost = true;
      }
    }
    if (taskRunner.isRequestOnRequested()) {
      // Don't have to restart a task for itself (or should use requester)
      logger.warn("Requested host cannot initiate itself the request");
      changeUpdatedInfo(UpdatedInfo.INERROR, ErrorCode.LoopSelfRequestedHost,
                        true);
      throw new OpenR66ProtocolNoConnectionException(
          "Requested host cannot initiate itself the request");
    }
    final DbHostAuth host;
    try {
      host = new DbHostAuth(taskRunner.getRequested());
    } catch (final WaarpDatabaseException e) {
      logger.error(
          "Requested host cannot be found: " + taskRunner.getRequested());
      changeUpdatedInfo(UpdatedInfo.INERROR, ErrorCode.NotKnownHost, true);
      throw new OpenR66ProtocolNoConnectionException(
          "Requested host cannot be found " + taskRunner.getRequested());
    }
    if (host.isClient()) {
      logger.warn("Cannot initiate a connection with a client: {}", host);
      changeUpdatedInfo(UpdatedInfo.INERROR, ErrorCode.ConnectionImpossible,
                        true);
      throw new OpenR66ProtocolNoConnectionException(
          "Cannot connect to client " + host);
    }
    final SocketAddress socketAddress = host.getSocketAddress();
    final boolean isSSL = host.isSsl();

    final LocalChannelReference localChannelReferenceTemp;
    try {
      localChannelReferenceTemp = networkTransaction
          .createConnectionWithRetryWithAuthenticationException(socketAddress,
                                                                isSSL,
                                                                futureRequest);
    } catch (final OpenR66ProtocolNotAuthenticatedException e1) {
      changeUpdatedInfo(UpdatedInfo.INERROR, ErrorCode.BadAuthent, true);
      taskRunner.setLocalChannelReference(new LocalChannelReference());
      throw new OpenR66ProtocolNoConnectionException(
          CANNOT_CONNECT_TO_SERVER + host +
          " cannot be authenticated so stop retry here", e1);
    }
    taskRunner.setLocalChannelReference(localChannelReferenceTemp);
    if (localChannelReferenceTemp == null) {
      // propose to redo
      String retry;
      if (incrementTaskRunnerTry(taskRunner, Configuration.RETRYNB)) {

        logger.debug("Will retry since Cannot connect to {}", host);
        retry = " but will retry";
        // now wait
        try {
          Thread.sleep(Configuration.configuration.getDelayRetry());
        } catch (final InterruptedException e) {//NOSONAR
          SysErrLogger.FAKE_LOGGER.ignoreLog(e);
          logger.info(
              "Will not retry since an interruption occurs while connection to {}",
              host);
          retry = " and retries gets an interruption so stop here";
          changeUpdatedInfo(UpdatedInfo.INERROR, ErrorCode.ConnectionImpossible,
                            true);
          taskRunner.setLocalChannelReference(new LocalChannelReference());
          throw new OpenR66ProtocolNoConnectionException(
              CANNOT_CONNECT_TO_SERVER + host + retry);
        }
        changeUpdatedInfo(UpdatedInfo.TOSUBMIT, ErrorCode.ConnectionImpossible,
                          true);
        throw new OpenR66ProtocolNotYetConnectionException(
            CANNOT_CONNECT_TO_SERVER + host + retry);
      } else {
        logger.info(
            "Will not retry since limit of connection attemtps is reached for {}",
            host);
        retry = " and retries reach step limit so stop here";
        changeUpdatedInfo(UpdatedInfo.INERROR, ErrorCode.ConnectionImpossible,
                          true);
        taskRunner.setLocalChannelReference(new LocalChannelReference());
        throw new OpenR66ProtocolNoConnectionException(
            CANNOT_CONNECT_TO_SERVER + host + retry);
      }
    }
    if (handler != null) {
      localChannelReferenceTemp.setRecvThroughHandler(handler);
    }
    localChannelReferenceTemp.setSendThroughMode(isSendThroughMode);
    if (restartPost) {
      final RequestPacket request = taskRunner.getRequest();
      logger.debug("Will send request {} ", request);
      localChannelReferenceTemp.setClientRunner(this);
      localChannelReferenceTemp.sessionNewState(R66FiniteDualStates.REQUESTR);
      try {
        ChannelUtils
            .writeAbstractLocalPacket(localChannelReferenceTemp, request, true);
      } catch (final OpenR66ProtocolPacketException e) {
        // propose to redo
        logger.warn("Cannot transfer request to " + host);
        changeUpdatedInfo(UpdatedInfo.INTERRUPTED, ErrorCode.Internal, true);
        localChannelReferenceTemp.close();
        throw e;
      }
      logger.debug("Wait for request to {}", host);
      return localChannelReferenceTemp;
    }
    // If Requester is NOT Sender, and if TransferTask then decrease now if
    // possible the rank
    if (!taskRunner.isSender() &&
        taskRunner.getGloballaststep() == TASKSTEP.TRANSFERTASK.ordinal()) {
      logger
          .debug("Requester is not Sender so decrease if possible the rank {}",
                 taskRunner);
      taskRunner.restartRank();
      taskRunner.forceSaveStatus();
      logger.info("Requester is not Sender so new rank is {} {}",
                  taskRunner.getRank(), taskRunner);
    }
    final RequestPacket request = taskRunner.getRequest();
    request.setLimit(
        localChannelReferenceTemp.getChannelLimit(taskRunner.isSender()));
    localChannelReferenceTemp.setClientRunner(this);
    logger.debug("Will send request {} {}", request, localChannelReferenceTemp);
    localChannelReferenceTemp.sessionNewState(R66FiniteDualStates.REQUESTR);
    try {
      ChannelUtils
          .writeAbstractLocalPacket(localChannelReferenceTemp, request, true);
    } catch (final OpenR66ProtocolPacketException e) {
      // propose to redo
      logger.warn("Cannot transfer request to " + host);
      changeUpdatedInfo(UpdatedInfo.INTERRUPTED, ErrorCode.Internal, true);
      localChannelReferenceTemp.close();
      throw e;
    }
    logger
        .debug("Wait for request to {} {} {}", host, localChannelReferenceTemp,
               request);
    return localChannelReferenceTemp;
  }

  /**
   * Change the UpdatedInfo of the current runner
   *
   * @param info
   */
  public void changeUpdatedInfo(final AbstractDbData.UpdatedInfo info,
                                final ErrorCode code, final boolean force) {
    taskRunner.changeUpdatedInfo(info);
    taskRunner.setErrorExecutionStatus(code);
    if (force) {
      taskRunner.forceSaveStatus();
    } else {
      try {
        taskRunner.saveStatus();
      } catch (final OpenR66RunnerErrorException ignored) {
        // nothing
      }
    }
  }

  /**
   * @param handler the handler to set
   */
  public void setRecvThroughHandler(final RecvThroughHandler handler) {
    this.handler = handler;
  }

  public void setSendThroughMode() {
    isSendThroughMode = true;
  }

  public boolean getSendThroughMode() {
    return isSendThroughMode;
  }

  public boolean isLimitRetryConnection() {
    return limitRetryConnection;
  }

  public void setLimitRetryConnection(final boolean limitRetryConnection) {
    this.limitRetryConnection = limitRetryConnection;
  }
}
