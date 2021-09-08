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

import io.netty.channel.ChannelFuture;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.file.DataBlock;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.filesystem.R66File;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbTaskRunner.TASKSTEP;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66Exception;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.packet.EndRequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.ErrorPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Retrieve transfer runner
 */
public class RetrieveRunner extends Thread {
  private static final String END_RETRIEVE_IN_ERROR = "End Retrieve in Error";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(RetrieveRunner.class);

  private final R66Session session;

  private final LocalChannelReference localChannelReference;

  private boolean done;

  protected final AtomicBoolean running = new AtomicBoolean(true);
  private final String nameThread;

  protected RetrieveRunner() {
    // empty constructor
    session = null;
    localChannelReference = null;
    nameThread = "RetrieveRunner: None";
    setName(nameThread);
    setDaemon(true);
  }

  /**
   * @param session
   */
  public RetrieveRunner(final R66Session session) {
    this.session = session;
    localChannelReference = this.session.getLocalChannelReference();
    nameThread = "RetrieveRunner: " + localChannelReference.getLocalId();
    setName(nameThread);
    setDaemon(true);
  }

  /**
   * Try to stop the runner
   */
  public final void stopRunner() {
    running.set(false);
  }

  @Override
  public void run() {
    boolean requestValidDone = false;
    setName(nameThread);
    try {
      try {
        if (session.getRunner().getGloballaststep() ==
            TASKSTEP.POSTTASK.ordinal()) {
          logger.warn("Restart from POSTTASK: EndTransfer");
          // restart from PostTask global step so just end now
          try {
            ChannelUtils.writeEndTransfer(localChannelReference);
          } catch (final OpenR66ProtocolPacketException e) {
            transferInError(e);
            logger.error(END_RETRIEVE_IN_ERROR);
            return;
          }
        } else {
          logger.debug("Start retrieve operation (send)");
          final R66File r66File = session.getFile();
          if (r66File == null) {
            logger.error("R66File null : {}", r66File);
            transferInError(
                new OpenR66RunnerErrorException("R66File not setup"));
            logger.info(END_RETRIEVE_IN_ERROR);
            return;
          } else {
            r66File.retrieveBlocking(running);
          }
        }
      } catch (final OpenR66RunnerErrorException e) {
        transferInError(e);
        logger.info(END_RETRIEVE_IN_ERROR);
        return;
      } catch (final OpenR66ProtocolSystemException e) {
        transferInError(e);
        logger.info(END_RETRIEVE_IN_ERROR);
        return;
      } catch (final Exception e) {
        logger.info("TRACE for unknown Exception ", e);
        transferInError(new OpenR66RunnerErrorException(e));
        logger.info(END_RETRIEVE_IN_ERROR);
        return;
      }
      localChannelReference.getFutureEndTransfer().awaitOrInterruptible();
      logger.debug("Await future End Transfer done: {}",
                   localChannelReference.getFutureEndTransfer().isSuccess());
      if (localChannelReference.getFutureEndTransfer().isDone() &&
          localChannelReference.getFutureEndTransfer().isSuccess()) {
        // send a validation
        localChannelReference.sessionNewState(R66FiniteDualStates.ENDREQUESTS);
        final EndRequestPacket validPacket =
            new EndRequestPacket(ErrorCode.CompleteOk.ordinal());
        if (session.getExtendedProtocol() &&
            session.getBusinessObject() != null &&
            session.getBusinessObject().getInfo(session) != null) {
          validPacket.setOptional(session.getBusinessObject().getInfo(session));
        }
        try {
          ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                                                validPacket, false);
          requestValidDone = true;
        } catch (final OpenR66ProtocolPacketException ignored) {
          // nothing
        }
        if (!localChannelReference.getFutureRequest().awaitOrInterruptible(
            Configuration.configuration.getTimeoutCon()) ||
            Thread.interrupted()) {
          // valid it however
          finalizeInternal();
        }
        if (session.getRunner() != null &&
            session.getRunner().isRequestOnRequested()) {
          localChannelReference.close();
        }
        done = true;
      } else {
        checkDoneNotAnswered();
        if (!localChannelReference.getFutureRequest().isDone()) {
          R66Result result =
              localChannelReference.getFutureEndTransfer().getResult();
          if (result == null) {
            result = new R66Result(session, false, ErrorCode.TransferError,
                                   session.getRunner());
          }
          localChannelReference.invalidateRequest(result);
        }
        done = true;
        logger.info(END_RETRIEVE_IN_ERROR);
      }
    } finally {
      try {
        if (!done) {
          finalizeRequestDone(requestValidDone);
        }
        NetworkTransaction.normalEndRetrieve(localChannelReference);
      } finally {
        setName("Finished_" + nameThread);
      }
    }
  }

  private void finalizeInternal() {
    session.getRunner().setAllDone();
    try {
      session.getRunner().saveStatus();
    } catch (final OpenR66RunnerErrorException e) {
      // ignore
    }
    localChannelReference.validateRequest(
        localChannelReference.getFutureEndTransfer().getResult());
  }

  private boolean checkDoneNotAnswered() {
    if (localChannelReference.getFutureEndTransfer().isDone()) {
      // Done and Not Success => error
      if (!localChannelReference.getFutureEndTransfer().getResult()
                                .isAnswered()) {
        localChannelReference.sessionNewState(R66FiniteDualStates.ERROR);
        final ErrorPacket error =
            new ErrorPacket(localChannelReference.getErrorMessage(),
                            localChannelReference.getFutureEndTransfer()
                                                 .getResult().getCode()
                                                 .getCode(),
                            ErrorPacket.FORWARDCLOSECODE);
        try {
          ChannelUtils.writeAbstractLocalPacket(localChannelReference, error,
                                                false);
        } catch (final OpenR66ProtocolPacketException ignored) {
          // ignore
        }
      }
      return true;
    }
    return false;
  }

  private void finalizeRequestDone(final boolean requestValidDone) {
    if (localChannelReference.getFutureEndTransfer().isDone() &&
        localChannelReference.getFutureEndTransfer().isSuccess()) {
      if (!requestValidDone) {
        localChannelReference.sessionNewState(R66FiniteDualStates.ENDREQUESTS);
        final EndRequestPacket validPacket =
            new EndRequestPacket(ErrorCode.CompleteOk.ordinal());
        if (session.getExtendedProtocol() &&
            session.getBusinessObject() != null &&
            session.getBusinessObject().getInfo(session) != null) {
          validPacket.setOptional(session.getBusinessObject().getInfo(session));
        }
        try {
          ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                                                validPacket, false);
        } catch (final OpenR66ProtocolPacketException ignored) {
          // nothing
        }
      }
      finalizeInternal();
      if (session.getRunner() != null &&
          session.getRunner().isRequestOnRequested()) {
        localChannelReference.close();
      }
    } else {
      if (!checkDoneNotAnswered()) {
        R66Result result =
            localChannelReference.getFutureEndTransfer().getResult();
        if (result == null) {
          result = new R66Result(session, false, ErrorCode.TransferError,
                                 session.getRunner());
        }
        localChannelReference.invalidateRequest(result);
      }
    }
  }

  private void transferInError(final OpenR66Exception e) {
    final R66Result result =
        new R66Result(e, session, true, ErrorCode.TransferError,
                      session.getRunner());
    logger.error("Transfer in error", e);
    session.newState(R66FiniteDualStates.ERROR);
    final ErrorPacket error =
        new ErrorPacket("Transfer in error", ErrorCode.TransferError.getCode(),
                        ErrorPacket.FORWARDCLOSECODE);
    try {
      ChannelUtils.writeAbstractLocalPacket(localChannelReference, error,
                                            false);
    } catch (final OpenR66ProtocolPacketException ignored) {
      // ignore
    }
    localChannelReference.invalidateRequest(result);
    localChannelReference.close();
    done = true;
  }

  /**
   * Write the next block when the channel is ready to prevent OOM
   *
   * @param block
   * @param localChannelReference
   * @param digestGlobal
   * @param digestBlock
   *
   * @return the ChannelFuture on the write operation
   *
   * @throws OpenR66ProtocolPacketException
   */
  public static ChannelFuture writeWhenPossible(final DataBlock block,
                                                final LocalChannelReference localChannelReference,
                                                final FilesystemBasedDigest digestGlobal,
                                                final FilesystemBasedDigest digestBlock)
      throws OpenR66ProtocolPacketException {
    return ChannelUtils.writeBackDataBlock(localChannelReference, digestGlobal,
                                           block, digestBlock);
  }

  public final int getLocalId() {
    return localChannelReference.getLocalId();
  }


  /**
   * When submit RetrieveRunner cannot be done since Executor is already stopped
   */
  public final void notStartRunner() {
    transferInError(
        new OpenR66RunnerErrorException("Cannot Start Runner: " + session));
    stopRunner();
  }

}
