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
package org.waarp.openr66.protocol.localhandler;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.exception.FileTransferException;
import org.waarp.common.file.DataBlock;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.task.AbstractTask;
import org.waarp.openr66.context.task.TaskType;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.exception.OpenR66DatabaseGlobalException;
import org.waarp.openr66.protocol.exception.OpenR66Exception;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessQueryAlreadyFinishedException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessQueryStillRunningException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoDataException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotAuthenticatedException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotYetConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.packet.DataPacket;
import org.waarp.openr66.protocol.localhandler.packet.EndRequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.EndTransferPacket;
import org.waarp.openr66.protocol.localhandler.packet.ErrorPacket;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.RequestJsonPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelCloseTimer;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.FileUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import java.security.NoSuchAlgorithmException;

import static org.waarp.common.database.DbConstant.*;
import static org.waarp.openr66.context.R66FiniteDualStates.*;

/**
 * Class to implement actions related to real transfer: request initialization,
 * data transfer, end of transfer
 * and of request, changing filename or filesize.
 */
public class TransferActions extends ServerActions {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(TransferActions.class);

  public TransferActions() {
    // nothing
  }

  /**
   * Finalize a request initialization in error
   *
   * @param code
   * @param runner
   * @param e1
   * @param packet
   *
   * @throws OpenR66ProtocolPacketException
   */
  private void endInitRequestInError(ErrorCode code, DbTaskRunner runner,
                                     OpenR66Exception e1, RequestPacket packet)
      throws OpenR66ProtocolPacketException {
    logger.error("TaskRunner initialisation in error: " + code.getMesg() + ' ' +
                 session + " {} runner {}",
                 e1 != null? e1.getMessage() : "no exception",
                 runner != null? runner.toShortString() : "no runner");
    logger.debug("DEBUG Full stack", e1);
    localChannelReference
        .invalidateRequest(new R66Result(e1, session, true, code, null));

    if (packet.isToValidate()) {
      // / answer with a wrong request since runner is not set on remote host
      if (runner != null) {
        if (runner.isSender()) {
          // In case Wildcard was used
          logger.debug("New FILENAME: {}", runner.getOriginalFilename());
          packet.setFilename(runner.getOriginalFilename());
          logger.debug("Rank set: " + runner.getRank());
          packet.setRank(runner.getRank());
        } else {
          logger.debug("Rank set: " + runner.getRank());
          packet.setRank(runner.getRank());
        }
      }
      packet.validate();
      packet.setCode(code.code);
      session.newState(ERROR);
      ChannelUtils
          .writeAbstractLocalPacket(localChannelReference, packet, true);
    } else {
      session.newState(ERROR);
      final ErrorPacket error = new ErrorPacket(
          "TaskRunner initialisation in error: " +
          (e1 != null? e1.getMessage() : "Unknown Error") + " for " + packet +
          " since " + code.getMesg(), code.getCode(),
          ErrorPacket.FORWARDCLOSECODE);
      ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
    }
    session.setStatus(47);
    ChannelCloseTimer.closeFutureTransaction(this);
  }

  /**
   * Receive a request of Transfer
   *
   * @param packet
   *
   * @throws OpenR66ProtocolNoDataException
   * @throws OpenR66ProtocolPacketException
   * @throws OpenR66ProtocolBusinessException
   * @throws OpenR66ProtocolSystemException
   * @throws OpenR66RunnerErrorException
   */
  public void request(RequestPacket packet)
      throws OpenR66ProtocolNoDataException, OpenR66ProtocolPacketException,
             OpenR66RunnerErrorException, OpenR66ProtocolSystemException,
             OpenR66ProtocolBusinessException {
    session.setStatus(99);
    if (!session.isAuthenticated()) {
      session.setStatus(48);
      throw new OpenR66ProtocolNotAuthenticatedException(
          Messages.getString("LocalServerHandler.3")); //$NON-NLS-1$
    }
    if (packet.isToValidate()) {
      session.newState(REQUESTR);
    }
    // XXX validLimit only on requested side
    if (checkRequest(packet)) {
      return;
    }
    DbRule rule;
    try {
      rule = new DbRule(packet.getRulename());
    } catch (final WaarpDatabaseException e) {
      logger.info("Rule is unknown: " + packet.getRulename() + " {}",
                  e.getMessage());
      session.setStatus(49);
      endInitRequestInError(ErrorCode.QueryRemotelyUnknown, null,
                            new OpenR66ProtocolBusinessException(
                                Messages.getString("LocalServerHandler.9") +
                                //$NON-NLS-1$
                                packet.getRulename()), packet);
      return;
    }
    packet = computeBlockSizeFromRequest(packet, rule);
    DbTaskRunner runner;
    // requested
    final boolean isRetrieve = DbTaskRunner.getSenderByRequestPacket(packet);
    if (packet.getSpecialId() != ILLEGALVALUE) {
      runner = getPreviousDbTaskRunnerFromRequest(packet, rule, isRetrieve);
      if (runner == null) {
        return;
      }
    } else {
      // Very new request
      // should not be the case (the requester should always set the id)
      logger.error("NO TransferID specified: SHOULD NOT BE THE CASE");
      try {
        runner = new DbTaskRunner(session, rule, isRetrieve, packet);
      } catch (final WaarpDatabaseException e) {
        session.setStatus(37);
        endInitRequestInError(ErrorCode.QueryRemotelyUnknown, null,
                              new OpenR66DatabaseGlobalException(e), packet);
        return;
      }
      packet.setSpecialId(runner.getSpecialId());
    }
    logger.debug("Runner before any action: {} {}", runner.shallIgnoreSave(),
                 runner);
    // Check now if request is a valid one
    if (packet.getCode() != ErrorCode.InitOk.code) {
      createErrorFromRequestInitKo(packet, runner);
      return;
    }
    // Receiver can specify a rank different from database
    setRankAtStartupFromRequest(packet, runner);
    runner.setBlocksize(packet.getBlocksize());
    try {
      runner.update();
    } catch (WaarpDatabaseException ignored) {
      // Ignore
    }
    logger.debug(
        "Filesize: " + packet.getOriginalSize() + ':' + runner.isSender());
    boolean shouldInformBack = false;
    try {
      session.setRunner(runner);
      // Fix to ensure that recv request are not trying to access to not chroot files
      if (Configuration.configuration.isChrootChecked() &&
          packet.isToValidate() && runner.isSender()) {
        session.startup(true);
      } else {
        session.startup(false);
      }
      if (runner.isSender() && !runner.isSendThrough()) {
        if (packet.getOriginalSize() != runner.getOriginalSize()) {
          packet.setOriginalSize(runner.getOriginalSize());
          shouldInformBack = true;
          logger.debug("Filesize2: " + packet.getOriginalSize() + ':' +
                       runner.isSender());
        }
      }
    } catch (final OpenR66RunnerErrorException e) {
      try {
        runner.saveStatus();
      } catch (final OpenR66RunnerErrorException e1) {
        logger.error("Cannot save Status: " + runner, e1);
      }
      if (runner.getErrorInfo() == ErrorCode.InitOk ||
          runner.getErrorInfo() == ErrorCode.PreProcessingOk ||
          runner.getErrorInfo() == ErrorCode.TransferOk) {
        runner.setErrorExecutionStatus(ErrorCode.ExternalOp);
      }
      logger.error("PreTask in error {}", e.getMessage(), e);
      errorToSend("PreTask in error: " + e.getMessage(), runner.getErrorInfo(),
                  38);
      return;
    }
    setFileSizeFromRequest(packet, runner, shouldInformBack);
    session.setReady(true);
    Configuration.configuration.getLocalTransaction()
                               .setFromId(runner, localChannelReference);

    // Set read/write limit
    final long remoteLimit = packet.getLimit();
    long localLimit = localChannelReference.getChannelLimit(runner.isSender());
    // Take the minimum speed
    logger.trace("Received limit {}", packet.getLimit());
    logger.trace("Local limit {}",
                 localChannelReference.getChannelLimit(runner.isSender()));
    if (localLimit <= 0) {
      localLimit = remoteLimit;
    } else if (remoteLimit > 0 && remoteLimit < localLimit) {
      localLimit = remoteLimit;
    }
    localChannelReference.setChannelLimit(runner.isSender(), localLimit);
    packet.setLimit(localLimit);

    // inform back
    informBackFromRequest(packet, runner);
    // if retrieve => START the retrieve operation except if in Send Through mode
    sendDataFromRequest(runner);
    session.setStatus(39);
  }

  private RequestPacket computeBlockSizeFromRequest(RequestPacket packet,
                                                    final DbRule rule)
      throws OpenR66ProtocolNotAuthenticatedException {
    int blocksize = packet.getBlocksize();
    if (packet.isToValidate()) {
      if (!rule.checkHostAllow(session.getAuth().getUser())) {
        session.setStatus(30);
        throw new OpenR66ProtocolNotAuthenticatedException(
            Messages.getString("LocalServerHandler.10")); //$NON-NLS-1$
      }
      // Check if the blocksize is greater than local value
      if (Configuration.configuration.getBlockSize() < blocksize) {
        logger.warn("Blocksize is greater than allowed {} < {}",
                    Configuration.configuration.getBlockSize(), blocksize);
        blocksize = Configuration.configuration.getBlockSize();
        final String sep = localChannelReference.getPartner().getSeperator();
        packet = new RequestPacket(packet.getRulename(), packet.getMode(),
                                   packet.getFilename(), blocksize,
                                   packet.getRank(), packet.getSpecialId(),
                                   packet.getTransferInformation(),
                                   packet.getOriginalSize(), sep);
      }
    }
    if (!RequestPacket.isCompatibleMode(rule.getMode(), packet.getMode())) {
      // not compatible Rule and mode in request
      throw new OpenR66ProtocolNotAuthenticatedException(
          Messages.getString("LocalServerHandler.12") + rule.getMode() + " vs "
          //$NON-NLS-1$
          + packet.getMode());
    }
    session.setBlockSize(blocksize);
    return packet;
  }

  private boolean checkRequest(final RequestPacket packet)
      throws OpenR66ProtocolPacketException {
    if (packet.isToValidate()) {
      if (Configuration.configuration.isShutdown()) {
        logger.warn(Messages.getString("LocalServerHandler.7") //$NON-NLS-1$
                    + packet.getRulename() + " from " + session.getAuth());
        session.setStatus(100);
        endInitRequestInError(ErrorCode.ServerOverloaded, null,
                              new OpenR66ProtocolNotYetConnectionException(
                                  "All new Request blocked"), packet);
        session.setStatus(100);
        return true;
      }
      if (Configuration.configuration.getConstraintLimitHandler()
                                     .checkConstraints()) {
        requestCheckConstraintsTrue(packet);
        return true;
      }
    } else if (packet.getCode() == ErrorCode.ServerOverloaded.code) {
      // XXX unvalid limit on requested host received
      logger.info("TaskRunner initialisation in error: " +
                  ErrorCode.ServerOverloaded.getMesg());
      localChannelReference.invalidateRequest(
          new R66Result(null, session, true, ErrorCode.ServerOverloaded, null));
      session.setStatus(101);
      ChannelCloseTimer.closeFutureTransaction(this);
      return true;
    }
    return false;
  }

  private static void setRankAtStartupFromRequest(final RequestPacket packet,
                                                  final DbTaskRunner runner) {
    if (runner.isSender()) {
      logger.debug("Rank was: " + runner.getRank() + " -> " + packet.getRank());
      runner.setRankAtStartup(packet.getRank());
    } else {
      if (runner.getRank() > packet.getRank()) {
        logger.debug(
            "Recv Rank was: " + runner.getRank() + " -> " + packet.getRank());
        // if receiver, change only if current rank is upper proposed rank
        runner.setRankAtStartup(packet.getRank());
      }
      if (packet.getOriginalSize() > 0) {
        runner.setOriginalSize(packet.getOriginalSize());
      }
    }
  }

  private void setFileSizeFromRequest(final RequestPacket packet,
                                      final DbTaskRunner runner,
                                      boolean shouldInformBack)
      throws OpenR66ProtocolPacketException {
    logger.debug(
        "Filesize: " + packet.getOriginalSize() + ':' + runner.isSender());
    if (!shouldInformBack) {
      shouldInformBack =
          !packet.getTransferInformation().equals(runner.getFileInformation());
    }
    if (runner.isFileMoved() && runner.isSender() && runner.isInTransfer() &&
        runner.getRank() == 0 && !packet.isToValidate()) {
      // File was moved during PreTask and very beginning of the transfer
      // and the remote host has already received the request packet
      // => Informs the receiver of the new name
      sendFilenameFilesizeChanging(packet, runner,
                                   "Will send a modification of filename due to pretask: ",
                                   "Change Filename by Pre action on sender");
    } else if (!packet.getFilename().equals(runner.getOriginalFilename()) &&
               runner.isSender() && runner.isInTransfer() &&
               runner.getRank() == 0 && !packet.isToValidate()) {
      // File was modify at the very beginning (using wildcards)
      // and the remote host has already received the request packet
      // => Informs the receiver of the new name
      sendFilenameFilesizeChanging(packet, runner,
                                   "Will send a modification of filename due to wildcard: ",
                                   "Change Filename by Wildcard on sender");
    } else if (runner.isSelfRequest() && runner.isSender() &&
               runner.isInTransfer() && runner.getRank() == 0 &&
               !packet.isToValidate()) {
      // FIX SelfRequest
      // File could be modified at the very beginning (using wildcards)
      // and the remote host has already received the request packet
      // => Informs the receiver of the new name
      sendFilenameFilesizeChanging(packet, runner,
                                   "Will send a modification of filename due to wildcard in SelfMode: ",
                                   "Change Filename by Wildcard on sender in SelfMode");
    } else if (shouldInformBack && !packet.isToValidate()) {
      // Was only (shouldInformBack)
      // File length is now known, so inform back
      sendFilenameFilesizeChanging(packet, runner,
                                   "Will send a modification of filesize or fileInfo: ",
                                   "Change Filesize / FileInfo on sender");
    }
  }

  private void informBackFromRequest(final RequestPacket packet,
                                     final DbTaskRunner runner)
      throws OpenR66ProtocolPacketException {
    if (packet.isToValidate()) {
      if (Configuration.configuration.getMonitoring() != null) {
        Configuration.configuration.getMonitoring().lastInActiveTransfer =
            System.currentTimeMillis();
      }
      if (runner.isSender()) {
        // In case Wildcard was used
        logger.debug("New FILENAME: {}", runner.getOriginalFilename());
        packet.setFilename(runner.getOriginalFilename());
        logger.debug("Rank set: " + runner.getRank());
        packet.setRank(runner.getRank());
      } else {
        logger.debug("Rank set: " + runner.getRank());
        packet.setRank(runner.getRank());
      }
      packet.validate();
      session.newState(REQUESTD);
      ChannelUtils
          .writeAbstractLocalPacket(localChannelReference, packet, true);
    } else {
      session.newState(REQUESTD);
      // requester => might be a client
      // Save the runner into the session and validate the request so begin transfer
      session.getLocalChannelReference().getFutureRequest().setRunner(runner);
      localChannelReference.getFutureValidRequest().setSuccess();
      if (Configuration.configuration.getMonitoring() != null) {
        Configuration.configuration.getMonitoring().lastOutActiveTransfer =
            System.currentTimeMillis();
      }
    }
  }

  private void sendDataFromRequest(final DbTaskRunner runner) {
    if (runner.isSender()) {
      if (runner.isSendThrough()) {
        // it is legal to send data from now
        logger.debug("Now ready to continue with send through");
        localChannelReference.validateEndTransfer(
            new R66Result(session, false, ErrorCode.PreProcessingOk, runner));
      } else {
        // Automatically send data now
        logger.debug("Now ready to continue with runRetrieve");
        NetworkTransaction.runRetrieve(session);
      }
    }
  }

  private void createErrorFromRequestInitKo(final RequestPacket packet,
                                            final DbTaskRunner runner)
      throws OpenR66RunnerErrorException, OpenR66ProtocolSystemException,
             OpenR66ProtocolBusinessException {
    // not valid so create an error from there
    final ErrorCode code =
        ErrorCode.getFromCode(String.valueOf(packet.getCode()));
    session.setBadRunner(runner, code);
    if (!runner.shallIgnoreSave()) {
      runner.saveStatus();
    }
    session.newState(ERROR);
    logger.error("Bad runner at startup {} {}", packet, session);
    final ErrorPacket errorPacket =
        new ErrorPacket(code.getMesg(), code.getCode(),
                        ErrorPacket.FORWARDCLOSECODE);
    errorMesg(errorPacket);
  }

  private DbTaskRunner getPreviousDbTaskRunnerFromRequest(
      final RequestPacket packet, final DbRule rule, final boolean isRetrieve)
      throws OpenR66ProtocolPacketException {
    final DbTaskRunner runner;// Reload or create
    final String requested = DbTaskRunner.getRequested(session, packet);
    final String requester = DbTaskRunner.getRequester(session, packet);
    logger.debug("DEBUG: " + packet.getSpecialId() + ':' + isRetrieve);
    if (packet.isToValidate()) {
      // Id could be a creation or a reload
      // Try reload
      runner =
          reloadDbTaskRunner(packet, rule, isRetrieve, requested, requester);

      final LocalChannelReference lcr =
          Configuration.configuration.getLocalTransaction().getFromRequest(
              requested + ' ' + requester + ' ' + packet.getSpecialId());
      if (checkRunnerConsistency(packet, runner, lcr)) {
        return null;
      }

      if (runner.isAllDone()) {
        // truly an error since done
        session.setStatus(31);
        endInitRequestInError(ErrorCode.QueryAlreadyFinished, runner,
                              new OpenR66ProtocolBusinessQueryAlreadyFinishedException(
                                  Messages.getString("LocalServerHandler.13")
                                  //$NON-NLS-1$
                                  + packet.getSpecialId()), packet);
        return null;
      }
      if (lcr != null) {
        // truly an error since still running
        session.setStatus(32);
        endInitRequestInError(ErrorCode.QueryStillRunning, runner,
                              new OpenR66ProtocolBusinessQueryStillRunningException(
                                  Messages.getString("LocalServerHandler.14")
                                  //$NON-NLS-1$
                                  + packet.getSpecialId()), packet);
        return null;
      }
      logger.debug("Runner before any action: {} {}", runner.shallIgnoreSave(),
                   runner);
      // ok to restart
      try {
        if (runner.restart(false)) {
          runner.saveStatus();
        }
      } catch (final OpenR66RunnerErrorException ignored) {
        // nothing
      }
      // Change the SpecialID! => could generate an error ?
      logger.trace("Here was sepSpecialId without condition");
      if (packet.getSpecialId() == ILLEGALVALUE) {
        packet.setSpecialId(runner.getSpecialId());
      }
    } else {
      // Id should be a reload
      runner = reloadDbTaskRunnerFromId(packet, rule, isRetrieve, requested,
                                        requester);
      if (runner == null) {
        return null;
      }
    }
    return runner;
  }

  private boolean checkRunnerConsistency(final RequestPacket packet,
                                         final DbTaskRunner runner,
                                         final LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    // Check correctness of packet received vs current LCR
    if (runner == null) {
      logger.info("Id is unknown: {}}", packet.getSpecialId());
      return true;
    }
    if (lcr != null && localChannelReference != null &&
        !runner.isSelfRequest() &&
        (!lcr.getLocalId().equals(localChannelReference.getLocalId()) ||
         !lcr.getRemoteId().equals(localChannelReference.getRemoteId()))) {
      logger.warn("LocalChannelReference differs: {}\n\t {}\n\tWill while " +
                  "runner is AllDone: {}", localChannelReference, lcr,
                  runner.isAllDone());
      logger.info("Id is unknown: {}}", packet.getSpecialId());
      if (runner.isAllDone()) {
        try {
          lcr.getServerHandler().tryFinalizeRequest(
              new R66Result(lcr.getSession(), false, ErrorCode.Internal,
                            runner));
        } catch (OpenR66RunnerErrorException ignore) {
          SysErrLogger.FAKE_LOGGER.ignoreLog(ignore);
        } catch (OpenR66ProtocolSystemException ignore) {
          SysErrLogger.FAKE_LOGGER.ignoreLog(ignore);
        }
        lcr.close();
        if (localChannelReference.getClientRunner() != null &&
            localChannelReference.getClientRunner().getTaskRunner() != null &&
            localChannelReference.getClientRunner().getTaskRunner()
                                 .isAllDone()) {
          localChannelReference.close();
        }
        return true;
      }
      return false;
    }
    return false;
  }

  private DbTaskRunner reloadDbTaskRunnerFromId(final RequestPacket packet,
                                                final DbRule rule,
                                                final boolean isRetrieve,
                                                final String requested,
                                                final String requester)
      throws OpenR66ProtocolPacketException {
    DbTaskRunner runner;
    try {
      runner = new DbTaskRunner(session, rule, packet.getSpecialId(), requester,
                                requested);
    } catch (final WaarpDatabaseException e) {
      if (localChannelReference.getDbSession() == null) {
        // Special case of no database client
        try {
          runner = new DbTaskRunner(session, rule, isRetrieve, packet);
          logger.debug("Runner before any action: {} {}",
                       runner.shallIgnoreSave(), runner);
        } catch (final WaarpDatabaseException e1) {
          session.setStatus(35);
          endInitRequestInError(ErrorCode.QueryRemotelyUnknown, null,
                                new OpenR66DatabaseGlobalException(e1), packet);
          return null;
        }
      } else {
        endInitRequestInError(ErrorCode.QueryRemotelyUnknown, null,
                              new OpenR66DatabaseGlobalException(e), packet);
        session.setStatus(36);
        return null;
      }
    }
    final LocalChannelReference lcr =
        Configuration.configuration.getLocalTransaction().getFromRequest(
            requested + ' ' + requester + ' ' + packet.getSpecialId());
    if (checkRunnerConsistency(packet, runner, lcr)) {
      return null;
    }
    runner.setSender(isRetrieve);
    // FIX check for SelfRequest
    if (runner.isSelfRequest()) {
      runner.setFilename(runner.getOriginalFilename());
    }
    if (!runner.isSender()) {
      logger.debug("New filename ? :" + packet.getFilename());
      runner.setOriginalFilename(packet.getFilename());
      if (runner.getRank() == 0) {
        runner.setFilename(packet.getFilename());
      }
    }
    logger.debug("Runner before any action: {} {}", runner.shallIgnoreSave(),
                 runner);
    try {
      if (runner.restart(false) && !runner.isSelfRequest()) {
        runner.saveStatus();
      }
    } catch (final OpenR66RunnerErrorException ignored) {
      // nothing
    }
    return runner;
  }

  private DbTaskRunner reloadDbTaskRunner(final RequestPacket packet,
                                          final DbRule rule,
                                          final boolean isRetrieve,
                                          final String requested,
                                          final String requester)
      throws OpenR66ProtocolPacketException {
    DbTaskRunner runner = null;
    try {
      runner = new DbTaskRunner(session, rule, packet.getSpecialId(), requester,
                                requested);
      // Patch to prevent self request to be stored by sender
      final boolean ignoreSave = runner.shallIgnoreSave();
      runner.setSender(isRetrieve);
      logger.debug("DEBUG: " + runner.getSpecialId() + ':' + ignoreSave + ':' +
                   runner.shallIgnoreSave() + ':' + isRetrieve);
      if (ignoreSave && !runner.shallIgnoreSave() &&
          !runner.checkFromDbForSubmit()) {
        // Since status changed, it means that object should be created and not reloaded
        // But in case of submit, item already exist so shall be loaded from database
        throw new WaarpDatabaseNoDataException(
            "False load, must reopen and create DbTaskRunner");
      }
    } catch (final WaarpDatabaseNoDataException e) {
      // Reception of request from requester host
      try {
        runner = new DbTaskRunner(session, rule, isRetrieve, packet);
        logger
            .debug("Runner before any action: {} {}", runner.shallIgnoreSave(),
                   runner);
      } catch (final WaarpDatabaseException e1) {
        session.setStatus(33);
        endInitRequestInError(ErrorCode.QueryRemotelyUnknown, null,
                              new OpenR66DatabaseGlobalException(e), packet);
        return null;
      }
    } catch (final WaarpDatabaseException e) {
      session.setStatus(34);
      endInitRequestInError(ErrorCode.QueryRemotelyUnknown, null,
                            new OpenR66DatabaseGlobalException(e), packet);
      return null;
    }
    return runner;
  }

  private void requestCheckConstraintsTrue(final RequestPacket packet)
      throws OpenR66ProtocolPacketException {
    if (Configuration.configuration.getR66Mib() != null) {
      Configuration.configuration.getR66Mib().notifyOverloaded(
          "Rule: " + packet.getRulename() + " from " + session.getAuth(),
          Configuration.configuration.getConstraintLimitHandler().lastAlert);
    }
    logger.warn(Messages.getString("LocalServerHandler.8") //$NON-NLS-1$
                + packet.getRulename() + " while " + Configuration.configuration
                    .getConstraintLimitHandler().lastAlert + " from " +
                session.getAuth());
    session.setStatus(100);
    endInitRequestInError(ErrorCode.ServerOverloaded, null,
                          new OpenR66ProtocolNotYetConnectionException(
                              "Limit exceeded " + Configuration.configuration
                                  .getConstraintLimitHandler().lastAlert),
                          packet);
    session.setStatus(100);
  }

  /**
   * Send a Filename/Filesize change to the partner
   *
   * @param packet
   * @param runner
   *
   * @throws OpenR66ProtocolPacketException
   */
  private void sendFilenameFilesizeChanging(RequestPacket packet,
                                            DbTaskRunner runner, String debug,
                                            String info)
      throws OpenR66ProtocolPacketException {
    logger.debug(debug + runner.getFilename());
    session.newState(VALID);
    if (localChannelReference.getPartner().useJson()) {
      final RequestJsonPacket request = new RequestJsonPacket();
      request.setComment(info);
      request.setFilename(runner.getFilename());
      request.setFilesize(packet.getOriginalSize());
      final String infoTransfer = runner.getFileInformation();
      if (infoTransfer != null &&
          !infoTransfer.equals(packet.getTransferInformation())) {
        request.setFileInfo(runner.getFileInformation());
      }
      final JsonCommandPacket validPacket =
          new JsonCommandPacket(request, LocalPacketFactory.REQUESTPACKET);
      ChannelUtils
          .writeAbstractLocalPacket(localChannelReference, validPacket, true);
    } else {
      final String infoTransfer = runner.getFileInformation();
      ValidPacket validPacket;
      if (infoTransfer != null &&
          !infoTransfer.equals(packet.getTransferInformation()) &&
          localChannelReference.getPartner().changeFileInfoEnabled()) {
        validPacket = new ValidPacket(info, runner.getFilename() +
                                            PartnerConfiguration.BAR_SEPARATOR_FIELD +
                                            packet.getOriginalSize() +
                                            PartnerConfiguration.BAR_SEPARATOR_FIELD +
                                            packet.getTransferInformation(),
                                      LocalPacketFactory.REQUESTPACKET);
      } else {
        validPacket = new ValidPacket(info, runner.getFilename() +
                                            PartnerConfiguration.BAR_SEPARATOR_FIELD +
                                            packet.getOriginalSize(),
                                      LocalPacketFactory.REQUESTPACKET);
      }
      ChannelUtils
          .writeAbstractLocalPacket(localChannelReference, validPacket, true);
    }
  }

  /**
   * Send an error
   *
   * @param message
   * @param code
   *
   * @throws OpenR66ProtocolPacketException
   */
  private void errorToSend(String message, ErrorCode code, int status)
      throws OpenR66ProtocolPacketException {
    session.newState(ERROR);
    try {
      session.setFinalizeTransfer(false, new R66Result(
          new OpenR66ProtocolPacketException(message), session, true, code,
          session.getRunner()));
    } catch (final OpenR66RunnerErrorException e1) {
      localChannelReference.invalidateRequest(
          new R66Result(e1, session, true, code, session.getRunner()));
    } catch (final OpenR66ProtocolSystemException e1) {
      localChannelReference.invalidateRequest(
          new R66Result(e1, session, true, code, session.getRunner()));
    }
    final ErrorPacket error =
        new ErrorPacket(message, code.getCode(), ErrorPacket.FORWARDCLOSECODE);
    ChannelUtils.writeAbstractLocalPacket(localChannelReference, error, true);
    session.setStatus(status);
    ChannelCloseTimer.closeFutureTransaction(this);
  }

  /**
   * Receive a data block
   *
   * @param packet
   *
   * @throws OpenR66ProtocolNotAuthenticatedException
   * @throws OpenR66ProtocolBusinessException
   * @throws OpenR66ProtocolPacketException
   */
  public void data(DataPacket packet)
      throws OpenR66ProtocolNotAuthenticatedException,
             OpenR66ProtocolBusinessException, OpenR66ProtocolPacketException {
    logger.trace("receiving data block {}", packet.getPacketRank());
    if (!session.isAuthenticated()) {
      logger.debug("Not authenticated while Data received");
      packet.clear();
      throw new OpenR66ProtocolNotAuthenticatedException(
          "Not authenticated while Data received");
    }
    if (!session.isReady()) {
      logger.debug("No request prepared");
      packet.clear();
      throw new OpenR66ProtocolBusinessException("No request prepared");
    }
    if (session.getRunner().isSender()) {
      logger.debug("Not in receive MODE but receive a packet");
      packet.clear();
      throw new OpenR66ProtocolBusinessException(
          "Not in receive MODE but receive a packet");
    }
    if (!session.getRunner().continueTransfer()) {
      logger.debug("EndTransfer failed ? " +
                   localChannelReference.getFutureEndTransfer().isFailed());
      if (localChannelReference.getFutureEndTransfer().isFailed()) {
        // nothing to do since already done
        session.setStatus(94);
        packet.clear();
        return;
      }
      errorToSend("Transfer in error due previously aborted transmission",
                  ErrorCode.TransferError, 95);
      packet.clear();
      return;
    }
    if (packet.getPacketRank() != session.getRunner().getRank()) {
      logger.debug("Issue on rank: " + packet.getPacketRank() + ':' +
                   session.getRunner().getRank());
      if (!session.addError()) {
        // cannot continue
        logger.error(Messages.getString("LocalServerHandler.15") +
                     packet.getPacketRank() + " : " + //$NON-NLS-1$
                     session.getRunner().getRank() + " from {}",
                     session.getRunner());
        errorToSend(
            "Too much Bad Rank in transmission: " + packet.getPacketRank(),
            ErrorCode.TransferError, 96);
        packet.clear();
        return;
      }
      // Fix the rank if possible
      if (packet.getPacketRank() < session.getRunner().getRank()) {
        logger.debug("Bad RANK: " + packet.getPacketRank() + " : " +
                     session.getRunner().getRank());
        session.getRunner().setRankAtStartup(packet.getPacketRank());
        session.getRestart().restartMarker(
            session.getRunner().getBlocksize() * session.getRunner().getRank());
        try {
          session.getFile().restartMarker(session.getRestart());
        } catch (final CommandAbstractException e) {
          logger.error("Bad RANK: " + packet.getPacketRank() + " : " +
                       session.getRunner().getRank());
          errorToSend("Bad Rank in transmission even after retry: " +
                      packet.getPacketRank(), ErrorCode.TransferError, 96);
          packet.clear();
          return;
        }
      } else {
        // really bad
        logger.error("Bad RANK: " + packet.getPacketRank() + " : " +
                     session.getRunner().getRank());
        errorToSend(
            "Bad Rank in transmission: " + packet.getPacketRank() + " > " +
            session.getRunner().getRank(), ErrorCode.TransferError, 20);
        packet.clear();
        return;
      }
    }
    // Check global size
    final long originalSize = session.getRunner().getOriginalSize();
    if (originalSize >= 0) {
      if (session.getRunner().getBlocksize() *
          (session.getRunner().getRank() - 1) > originalSize) {
        // cannot continue
        logger.error(Messages.getString("LocalServerHandler.16") +
                     packet.getPacketRank() + " : " + //$NON-NLS-1$
                     (originalSize / session.getRunner().getBlocksize() + 1) +
                     " from {}", session.getRunner());
        errorToSend("Too much data transferred: " + packet.getPacketRank(),
                    ErrorCode.TransferError, 96);
        packet.clear();
        return;
      }
    }
    // if MD5 check MD5
    if (RequestPacket.isMD5Mode(session.getRunner().getMode())) {
      logger.debug("AlgoDigest: " + (localChannelReference.getPartner() != null?
          localChannelReference.getPartner().getDigestAlgo() : "usual algo"));
      if (!packet
          .isKeyValid(localChannelReference.getPartner().getDigestAlgo())) {
        // Wrong packet
        logger.error(Messages.getString("LocalServerHandler.17"), packet,
                     //$NON-NLS-1$
                     localChannelReference.getPartner()
                                          .getDigestAlgo().algoName);
        errorToSend("Transfer in error due to bad Hash on data packet (" +
                    localChannelReference.getPartner()
                                         .getDigestAlgo().algoName + ')',
                    ErrorCode.MD5Error, 21);
        packet.clear();
        return;
      }
    }
    if (Configuration.configuration.isGlobalDigest()) {
      if (globalDigest == null) {
        try {
          // check if first block, since if not, digest will be only partial
          if (session.getRunner().getRank() > 0) {
            localChannelReference.setPartialHash();
          }
          if (localChannelReference.getPartner() != null &&
              localChannelReference.getPartner().useFinalHash()) {
            final DigestAlgo algo =
                localChannelReference.getPartner().getDigestAlgo();
            if (algo != Configuration.configuration.getDigest()) {
              globalDigest = new FilesystemBasedDigest(algo);
              localDigest = new FilesystemBasedDigest(
                  Configuration.configuration.getDigest());
            }
          }
          if (globalDigest == null) {
            globalDigest = new FilesystemBasedDigest(
                Configuration.configuration.getDigest());
            localDigest = null;
          }
        } catch (final NoSuchAlgorithmException ignored) {
          // nothing
        }
        logger.debug("GlobalDigest: " +
                     localChannelReference.getPartner().getDigestAlgo() +
                     " different? " + (localDigest != null));
      }
      FileUtils.computeGlobalHash(globalDigest, packet.getData());
      if (localDigest != null) {
        FileUtils.computeGlobalHash(localDigest, packet.getData());
      }
    }
    final DataBlock dataBlock = new DataBlock();
    if (session.getRunner().isRecvThrough() &&
        localChannelReference.isRecvThroughMode()) {
      try {
        localChannelReference.getRecvThroughHandler()
                             .writeByteBuf(packet.getData());
        session.getRunner().incrementRank();
        if (packet.getPacketRank() % 100 == 1) {
          logger.debug("Good RANK: " + packet.getPacketRank() + " : " +
                       session.getRunner().getRank());
        }
      } finally {
        packet.clear();
      }
    } else {
      dataBlock.setBlock(packet.getData());
      try {
        session.getFile().writeDataBlock(dataBlock);
        session.getRunner().incrementRank();
        if (packet.getPacketRank() % 100 == 1) {
          logger.debug("Good RANK: " + packet.getPacketRank() + " : " +
                       session.getRunner().getRank());
        }
      } catch (final FileTransferException e) {
        errorToSend("Transfer in error", ErrorCode.TransferError, 22);
      } finally {
        dataBlock.clear();
        packet.clear();
      }
    }
  }

  /**
   * Receive an End of Transfer
   *
   * @param packet
   *
   * @throws OpenR66RunnerErrorException
   * @throws OpenR66ProtocolSystemException
   * @throws OpenR66ProtocolNotAuthenticatedException
   */
  public void endTransfer(EndTransferPacket packet)
      throws OpenR66RunnerErrorException, OpenR66ProtocolSystemException,
             OpenR66ProtocolNotAuthenticatedException {
    if (!session.isAuthenticated()) {
      throw new OpenR66ProtocolNotAuthenticatedException(
          "Not authenticated while EndTransfer received");
    }
    // Check end of transfer
    final long originalSize = session.getRunner().getOriginalSize();
    logger.debug("OSize: " + originalSize + " isSender: " +
                 session.getRunner().isSender());
    if (packet.isToValidate()) {
      // check if possible originalSize
      if (originalSize > 0) {
        if (checkOriginalSize(originalSize)) {
          return;
        }
      }
      // check if possible Global Digest
      if (checkGlobalDigest(packet)) {
        return;
      }
      session.newState(ENDTRANSFERS);
      fromEndTransferSToTransferR(packet);
    } else {
      session.newState(ENDTRANSFERR);
      if (!localChannelReference.getFutureRequest().isDone()) {
        // Validation of end of transfer
        if (endTransferR()) {
          // nothing
        }
      }
    }
  }

  private void fromEndTransferSToTransferR(final EndTransferPacket packet) {
    if (!localChannelReference.getFutureRequest().isDone()) {
      session.newState(ENDTRANSFERR);
      if (endTransferR()) {
        return;
      }
      // Now can send validation
      packet.validate();
      try {
        ChannelUtils
            .writeAbstractLocalPacket(localChannelReference, packet, false);
      } catch (final OpenR66ProtocolPacketException e) {
        // ignore
      }
    } else {
      // in error due to a previous status (like bad MD5)
      logger.error(Messages.getString("LocalServerHandler.20")); //$NON-NLS-1$
      session.setStatus(23);
      localChannelReference.close();
    }
  }

  private boolean checkGlobalDigest(final EndTransferPacket packet) {
    final String hash = packet.getOptional();
    logger.debug(
        "GlobalDigest: " + localChannelReference.getPartner().getDigestAlgo() +
        " different? " + (localDigest != null) + " remoteHash? " +
        (hash != null));
    if (hash != null && globalDigest != null) {
      String localhash = FilesystemBasedDigest.getHex(globalDigest.Final());
      globalDigest = null;
      if (!localhash.equalsIgnoreCase(hash)) {
        // bad global Hash
        final R66Result result = new R66Result(new OpenR66RunnerErrorException(
            Messages.getString("LocalServerHandler.19") + //$NON-NLS-1$
            localChannelReference.getPartner().getDigestAlgo().algoName + ')'),
                                               session, true,
                                               ErrorCode.MD5Error,
                                               session.getRunner());
        try {
          session.setFinalizeTransfer(false, result);
        } catch (final OpenR66RunnerErrorException ignored) {
          // nothing
        } catch (final OpenR66ProtocolSystemException ignored) {
          // nothing
        }
        final ErrorPacket error = new ErrorPacket(
            "Global Hash in error, transfer in error and rank should be reset to 0 (using " +
            localChannelReference.getPartner().getDigestAlgo().algoName + ')',
            ErrorCode.MD5Error.getCode(), ErrorPacket.FORWARDCLOSECODE);
        try {
          ChannelUtils
              .writeAbstractLocalPacket(localChannelReference, error, true);
        } catch (final OpenR66ProtocolPacketException ignored) {
          // nothing
        }
        session.setStatus(23);
        ChannelCloseTimer.closeFutureTransaction(this);
        return true;
      } else {
        if (localDigest != null) {
          localhash = FilesystemBasedDigest.getHex(localDigest.Final());
        }
        localChannelReference.setHashComputeDuringTransfer(localhash);
        logger.debug("Global digest ok");
      }
    } else if (globalDigest != null) {
      String localhash;
      if (localDigest != null) {
        localhash = FilesystemBasedDigest.getHex(localDigest.Final());
      } else {
        localhash = FilesystemBasedDigest.getHex(globalDigest.Final());
      }
      globalDigest = null;
      localChannelReference.setHashComputeDuringTransfer(localhash);
    }
    localDigest = null;
    globalDigest = null;
    return false;
  }

  private boolean checkOriginalSize(final long originalSize) {
    try {
      if (!session.getRunner().isRecvThrough() &&
          session.getFile().length() != originalSize ||
          session.getFile().length() == 0) {
        final R66Result result = new R66Result(new OpenR66RunnerErrorException(
            Messages.getString("LocalServerHandler.18")),
                                               //$NON-NLS-1$
                                               session, true,
                                               ErrorCode.TransferError,
                                               session.getRunner());
        try {
          session.setFinalizeTransfer(false, result);
        } catch (final OpenR66RunnerErrorException ignored) {
          // nothing
        } catch (final OpenR66ProtocolSystemException ignored) {
          // nothing
        }
        final ErrorPacket error = new ErrorPacket(
            "Final size in error, transfer in error and rank should be reset to 0",
            ErrorCode.TransferError.getCode(), ErrorPacket.FORWARDCLOSECODE);
        try {
          ChannelUtils
              .writeAbstractLocalPacket(localChannelReference, error, true);
        } catch (final OpenR66ProtocolPacketException ignored) {
          // nothing
        }
        session.setStatus(23);
        ChannelCloseTimer.closeFutureTransaction(this);
        return true;
      }
    } catch (final CommandAbstractException e) {
      // ignore
    }
    return false;
  }

  private boolean endTransferR() {
    // Finish with post Operation
    R66Result result = new R66Result(session, false, ErrorCode.TransferOk,
                                     session.getRunner());
    try {
      session.setFinalizeTransfer(true, result);
    } catch (final OpenR66RunnerErrorException e) {
      session.newState(ERROR);
      ErrorPacket error;
      if (localChannelReference.getFutureRequest().getResult() != null) {
        result = localChannelReference.getFutureRequest().getResult();
        error = new ErrorPacket(
            "Error while finalizing transfer: " + result.getMessage(),
            result.getCode().getCode(), ErrorPacket.FORWARDCLOSECODE);
      } else {
        error = new ErrorPacket("Error while finalizing transfer",
                                ErrorCode.FinalOp.getCode(),
                                ErrorPacket.FORWARDCLOSECODE);
      }
      try {
        ChannelUtils
            .writeAbstractLocalPacket(localChannelReference, error, true);
      } catch (final OpenR66ProtocolPacketException ignored) {
        // nothing
      }
      session.setStatus(23);
      ChannelCloseTimer.closeFutureTransaction(this);
      return true;
    } catch (final OpenR66ProtocolSystemException e) {
      session.newState(ERROR);
      ErrorPacket error;
      if (localChannelReference.getFutureRequest().getResult() != null) {
        result = localChannelReference.getFutureRequest().getResult();
        error = new ErrorPacket(
            "Error while finalizing transfer: " + result.getMessage(),
            result.getCode().getCode(), ErrorPacket.FORWARDCLOSECODE);
      } else {
        error = new ErrorPacket("Error while finalizing transfer",
                                ErrorCode.FinalOp.getCode(),
                                ErrorPacket.FORWARDCLOSECODE);
      }
      try {
        ChannelUtils
            .writeAbstractLocalPacket(localChannelReference, error, true);
      } catch (final OpenR66ProtocolPacketException ignored) {
        // nothing
      }
      session.setStatus(23);
      ChannelCloseTimer.closeFutureTransaction(this);
      return true;
    }
    return false;
  }

  /**
   * Receive an End of Request
   *
   * @param packet
   *
   * @throws OpenR66RunnerErrorException
   * @throws OpenR66ProtocolSystemException
   * @throws OpenR66ProtocolNotAuthenticatedException
   */
  public void endRequest(EndRequestPacket packet) {
    // Validate the last post action on a transfer from receiver remote host
    logger.info("Valid Request {} Packet {}", localChannelReference, packet);
    final DbTaskRunner runner = session.getRunner();
    logger.debug("Runner endRequest: " + (session.getRunner() != null));
    if (runner != null) {
      runner.setAllDone();
      try {
        runner.saveStatus();
      } catch (final OpenR66RunnerErrorException e) {
        // ignore
      }
      runner.clean();
    }
    String optional = null;
    if (session.getExtendedProtocol()) {
      optional = packet.getOptional();
    }
    if (!localChannelReference.getFutureRequest().isDone()) {
      // end of request
      final R66Future transfer = localChannelReference.getFutureEndTransfer();
      transfer.awaitOrInterruptible();
      if (transfer.isSuccess()) {
        if (session.getExtendedProtocol() &&
            session.getBusinessObject() != null) {
          if (session.getBusinessObject().getInfo(session) == null) {
            session.getBusinessObject().setInfo(session, optional);
          } else {
            final String temp = session.getBusinessObject().getInfo(session);
            session.getBusinessObject().setInfo(session, optional);
            optional = temp;
          }
        } else if (session.getExtendedProtocol() &&
                   transfer.getResult().getOther() == null &&
                   optional != null) {
          transfer.getResult().setOther(optional);
        }
        localChannelReference.validateRequest(transfer.getResult());
      }
    }
    session.setStatus(1);
    if (packet.isToValidate()) {
      session.newState(ENDREQUESTS);
      packet.validate();
      if (session.getExtendedProtocol()) {
        packet.setOptional(optional);
      }
      session.newState(ENDREQUESTR);
      try {
        ChannelUtils
            .writeAbstractLocalPacket(localChannelReference, packet, true);
      } catch (final OpenR66ProtocolPacketException ignored) {
        // nothing
      }
    } else {
      if (session.getState() != CLOSEDCHANNEL) {
        session.newState(ENDREQUESTR);
      }
    }
    if (runner != null &&
        (runner.isSelfRequested() || runner.isSelfRequest())) {
      ChannelCloseTimer.closeFutureTransaction(this);
    }
  }

  /**
   * If newFileInfo is provided and different than current value
   *
   * @param newFileInfo
   *
   * @throws OpenR66RunnerErrorException
   */
  public void requestChangeFileInfo(String newFileInfo)
      throws OpenR66RunnerErrorException {
    final DbTaskRunner runner = session.getRunner();
    logger.debug("NewFileInfo " + newFileInfo);
    runner.setFileInformation(newFileInfo);
    try {
      runner.update();
    } catch (final WaarpDatabaseException e) {
      runner.saveStatus();
      runner.setErrorExecutionStatus(ErrorCode.ExternalOp);
      session.newState(ERROR);
      logger.error("File info changing in error {}", e.getMessage());
      final ErrorPacket error = new ErrorPacket(
          "File changing information in error: " + e.getMessage(),
          runner.getErrorInfo().getCode(), ErrorPacket.FORWARDCLOSECODE);
      try {
        ChannelUtils
            .writeAbstractLocalPacket(localChannelReference, error, true);
      } catch (final OpenR66ProtocolPacketException ignored) {
        // nothing
      }
      try {
        session.setFinalizeTransfer(false, new R66Result(
            new OpenR66RunnerErrorException(e), session, true,
            runner.getErrorInfo(), runner));
      } catch (final OpenR66RunnerErrorException e1) {
        localChannelReference.invalidateRequest(
            new R66Result(new OpenR66RunnerErrorException(e), session, true,
                          runner.getErrorInfo(), runner));
      } catch (final OpenR66ProtocolSystemException e1) {
        localChannelReference.invalidateRequest(
            new R66Result(new OpenR66RunnerErrorException(e), session, true,
                          runner.getErrorInfo(), runner));
      }
      session.setStatus(97);
      ChannelCloseTimer.closeFutureTransaction(this);
    }
  }

  /**
   * Change the filename and the filesize
   *
   * @param newfilename
   * @param newSize
   *
   * @throws OpenR66RunnerErrorException
   */
  public void requestChangeNameSize(String newfilename, long newSize)
      throws OpenR66RunnerErrorException {
    session.newState(VALID);
    final DbTaskRunner runner = session.getRunner();
    logger.debug("NewSize " + newSize + " NewName " + newfilename);
    // The filename or filesize from sender is changed due to PreTask so change it too in receiver
    // comment, filename, filesize
    // Close only if an error occurs!
    if (runner != null) {
      if (newSize > 0) {
        runner.setOriginalSize(newSize);
        // Check if a CHKFILE task was supposely needed to run
        if (checkIfAnyTaskCheckFile(newfilename, newSize, runner)) {
          return;
        }
      }
    }
    // check if send is already on going
    if (runner != null && runner.getRank() > 0) {
      // already started so not changing the filename
      // Success: No write back at all
      return;
    }
    // Pre execution was already done since this packet is only received once
    // the request is already validated by the receiver
    try {
      session.renameReceiverFile(newfilename);
    } catch (final OpenR66RunnerErrorException e) {
      if (runner != null) {
        runner.saveStatus();
        runner.setErrorExecutionStatus(ErrorCode.FileNotFound);
        session.newState(ERROR);
        logger.error("File renaming in error {}", e.getMessage());
        final ErrorPacket error =
            new ErrorPacket("File renaming in error: " + e.getMessage(),
                            runner.getErrorInfo().getCode(),
                            ErrorPacket.FORWARDCLOSECODE);
        try {
          ChannelUtils
              .writeAbstractLocalPacket(localChannelReference, error, true);
        } catch (final OpenR66ProtocolPacketException ignored) {
          // nothing
        }
        try {
          session.setFinalizeTransfer(false, new R66Result(e, session, true,
                                                           runner
                                                               .getErrorInfo(),
                                                           runner));
        } catch (final OpenR66RunnerErrorException e1) {
          localChannelReference.invalidateRequest(
              new R66Result(e, session, true, runner.getErrorInfo(), runner));
        } catch (final OpenR66ProtocolSystemException e1) {
          localChannelReference.invalidateRequest(
              new R66Result(e, session, true, runner.getErrorInfo(), runner));
        }
      }
      session.setStatus(97);
      ChannelCloseTimer.closeFutureTransaction(this);
    }
    // Success: No write back at all
  }

  private boolean checkIfAnyTaskCheckFile(final String newfilename,
                                          final long newSize,
                                          final DbTaskRunner runner)
      throws OpenR66RunnerErrorException {
    final String[][] rpretasks = runner.getRule().getRpreTasksArray();
    if (rpretasks != null) {
      for (final String[] strings : rpretasks) {
        final AbstractTask task = runner.getTask(strings, session);
        if (task.getType() == TaskType.CHKFILE) {
          // re run this in case
          task.run();
          task.getFutureCompletion().awaitOrInterruptible();
          if (!task.getFutureCompletion().isSuccess()) {
            // not valid so create an error from there
            final ErrorCode code = ErrorCode.SizeNotAllowed;
            runner.setErrorExecutionStatus(code);
            runner.saveStatus();
            session.setBadRunner(runner, code);
            session.newState(ERROR);
            logger.error(
                "File length is not compatible with Rule or capacity {} {}",
                newfilename + " : " + newSize, session);
            final ErrorPacket errorPacket = new ErrorPacket(
                "File length is not compatible with Rule or capacity",
                code.getCode(), ErrorPacket.FORWARDCLOSECODE);
            try {
              ChannelUtils
                  .writeAbstractLocalPacket(localChannelReference, errorPacket,
                                            true);
            } catch (final OpenR66ProtocolPacketException ignored) {
              // nothing
            }
            try {
              session.setFinalizeTransfer(false, new R66Result(
                  new OpenR66RunnerErrorException(errorPacket.getSheader()),
                  session, true, runner.getErrorInfo(), runner));
            } catch (final OpenR66RunnerErrorException e1) {
              localChannelReference.invalidateRequest(new R66Result(
                  new OpenR66RunnerErrorException(errorPacket.getSheader()),
                  session, true, runner.getErrorInfo(), runner));
            } catch (final OpenR66ProtocolSystemException e1) {
              localChannelReference.invalidateRequest(new R66Result(
                  new OpenR66RunnerErrorException(errorPacket.getSheader()),
                  session, true, runner.getErrorInfo(), runner));
            }
            session.setStatus(97);
            ChannelCloseTimer.closeFutureTransaction(this);
            return true;
          }
        }
      }
    }
    return false;
  }
}
