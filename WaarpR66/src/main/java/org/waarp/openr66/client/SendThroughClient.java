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
package org.waarp.openr66.client;

import io.netty.channel.ChannelFuture;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.file.DataBlock;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.commander.ClientRunner;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.exception.OpenR66DatabaseGlobalException;
import org.waarp.openr66.protocol.exception.OpenR66Exception;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotYetConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.RetrieveRunner;
import org.waarp.openr66.protocol.localhandler.packet.EndRequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.ErrorPacket;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

/**
 * Class for Send Through client
 * <p>
 * This class does not included the real file transfer since it is up to the
 * business project to implement how
 * to read new data to be sent to the remote host. If an error occurs, no
 * transfer log is kept.
 * <p>
 * 1) Configuration must have been loaded<br>
 * <br>
 * 2) Pipeline and NetworkTransaction must have been initiated:<br>
 * <tt>     Configuration.configuration.pipelineInit();</tt><br>
 * <tt>     NetworkTransaction networkTransaction = new
 * NetworkTransaction();</tt><br>
 * <br>
 * 3) Prepare the request of transfer:<br>
 * <tt>     R66Future futureReq = new R66Future(true);</tt><br>
 * <tt>     SendThroughClient transaction = new SendThroughClient(futureReq,...);</tt><br>
 * <tt>     if (! transaction.initiateRequest()) { error }</tt><br>
 * <br>
 * 4) Once initiateRequest() gives true, you are ready to send the data in
 * through mode like:<br>
 * <tt>     byte[] data = readOrGetInSomeWayData();</tt><br>
 * <tt>     DataBlock block = transaction.transformToDataBlock(data);</tt><br>
 * <tt>     futureWrite = transaction.writeWhenPossible(block);</tt><br>
 * <br>
 * 5) Once you have finished, so this is the last block, you have to do the
 * following:<br>
 * If the last block is not empty:<br>
 * <tt>     DataBlock block = transaction.transformToDataBlock(data);</tt><br>
 * <tt>     block.setEOF(true);</tt><br>
 * Or if the last block is empty:<br>
 * <tt>     DataBlock block = transaction.transformToDataBlock(null);</tt><br>
 * Then <br>
 * <tt>     futureWrite = transaction.writeWhenPossible(block);</tt><br>
 * <tt>     futureWrite.awaitUninterruptibly();</tt><br>
 * <br>
 * 6) If everything is in success:<br>
 * <tt>     transaction.finalizeRequest();</tt><br>
 * <br>
 * And now wait for the transfer to finish:<br>
 * <tt>     futureReq.awaitUninterruptibly();</tt><br>
 * <tt>     R66Result result = futureReq.getResult();</tt><br>
 * <br>
 * 7) If there is the need to re-do, just re-execute the steps from 3 to 6.<br>
 * Don't forget at the very end to finish the global structure (steps 3 to 6 no
 * more executed):<br>
 * <tt>     networkTransaction.closeAll();</tt><br>
 * <br>
 * 8) In case of errors during steps 4 or 5 (and only those), call the
 * following:<br>
 * <tr>
 * transaction.transferInError(openR66Exception);
 * </tr>
 * <br>
 * <br>
 *
 * @see TestSendThroughClient Class as example of usage in test part
 */
public abstract class SendThroughClient extends AbstractTransfer {
  protected final NetworkTransaction networkTransaction;
  protected LocalChannelReference localChannelReference;
  protected DbTaskRunner taskRunner;

  /**
   * @param future
   * @param remoteHost
   * @param filename
   * @param rulename
   * @param fileinfo
   * @param isMD5
   * @param blocksize
   * @param networkTransaction
   * @param id
   */
  protected SendThroughClient(final R66Future future, final String remoteHost,
                              final String filename, final String rulename,
                              final String fileinfo, final boolean isMD5,
                              final int blocksize, final long id,
                              final NetworkTransaction networkTransaction) {
    super(SendThroughClient.class, future, filename, rulename, fileinfo, isMD5,
          remoteHost, blocksize, id, null);
    this.networkTransaction = networkTransaction;
  }

  /**
   * DO NOT CALL THIS!
   */
  @Override
  public void run() {
    logger.error("DO NOT call this method for this class");
  }

  /**
   * Prior to call this method, the pipeline and NetworkTransaction must have
   * been initialized. It is the
   * responsibility of the caller to finish all network resources. Note that
   * this is only the first part of the
   * execution for this client.
   *
   * @return True if the initiate of the request is OK, else False
   */
  public boolean initiateRequest() {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(SendThroughClient.class);
    }
    final DbRule rule;
    try {
      rule = new DbRule(transferArgs.getRulename());
    } catch (final WaarpDatabaseException e) {
      logger.error("Cannot get Rule: " + transferArgs.getRulename() + ": {}",
                   e.getMessage());
      future.setResult(
          new R66Result(new OpenR66DatabaseGlobalException(e), null, true,
                        ErrorCode.Internal, null));
      future.setFailure(e);
      return false;
    }
    int mode = rule.getMode();
    if (transferArgs.isMD5()) {
      mode = RequestPacket.getModeMD5(mode);
    }
    final String sep =
        PartnerConfiguration.getSeparator(transferArgs.getRemoteHost());
    final RequestPacket request =
        new RequestPacket(transferArgs.getRulename(), mode,
                          transferArgs.getFilename(),
                          transferArgs.getBlockSize(), 0, transferArgs.getId(),
                          transferArgs.getTransferInfo(), -1, sep);
    // Not isRecv since it is the requester, so send => isSender is true
    final boolean isSender = true;
    try {
      try {
        // no starttime since immediate
        taskRunner = new DbTaskRunner(rule, isSender, request,
                                      transferArgs.getRemoteHost(), null);
      } catch (final WaarpDatabaseException e) {
        logger.error("Cannot get task: {}", e.getMessage());
        future.setResult(
            new R66Result(new OpenR66DatabaseGlobalException(e), null, true,
                          ErrorCode.Internal, null));
        future.setFailure(e);
        return false;
      }
      final ClientRunner runner =
          new ClientRunner(networkTransaction, taskRunner, future);
      runner.setSendThroughMode();
      OpenR66ProtocolNotYetConnectionException exc = null;
      for (int i = 0; i < Configuration.RETRYNB; i++) {
        try {
          localChannelReference = runner.initRequest();
          exc = null;
          break;
        } catch (final OpenR66ProtocolNoConnectionException e) {
          logger.error("Cannot Connect", e);
          future.setResult(
              new R66Result(e, null, true, ErrorCode.ConnectionImpossible,
                            taskRunner));
          finalizeInErrorTransferRequest(runner, taskRunner,
                                         ErrorCode.ConnectionImpossible);
          future.setFailure(e);
          return false;
        } catch (final OpenR66ProtocolPacketException e) {
          logger.error("Bad Protocol", e);
          future.setResult(new R66Result(e, null, true, ErrorCode.TransferError,
                                         taskRunner));
          future.setFailure(e);
          return false;
        } catch (final OpenR66ProtocolNotYetConnectionException e) {
          logger.debug("Not Yet Connected", e);
          exc = e;
        }
      }
      if (exc != null) {
        taskRunner.setLocalChannelReference(new LocalChannelReference());
        logger.error("Cannot Connect", exc);
        future.setResult(
            new R66Result(exc, null, true, ErrorCode.ConnectionImpossible,
                          taskRunner));
        future.setFailure(exc);
        return false;
      }
      try {
        localChannelReference.waitReadyForSendThrough();
      } catch (final OpenR66Exception e) {
        logger.error("Cannot Transfer", e);
        future.setResult(
            new R66Result(e, null, true, ErrorCode.Internal, taskRunner));
        future.setFailure(e);
        return false;
      }
      // now start the send from external data
      return true;
    } finally {
      if (taskRunner != null && (future.isFailed() || nolog)) {
        try {
          taskRunner.delete();
        } catch (final WaarpDatabaseException ignored) {
          // nothing
        }
      }
    }
  }

  /**
   * Finalize the request
   */
  public void finalizeRequest() {
    try {
      try {
        ChannelUtils.writeEndTransfer(localChannelReference);
      } catch (final OpenR66ProtocolPacketException e) {
        // An error occurs!
        try {
          localChannelReference.getSession().setFinalizeTransfer(false,
                                                                 new R66Result(
                                                                     e,
                                                                     localChannelReference
                                                                         .getSession(),
                                                                     false,
                                                                     ErrorCode.Internal,
                                                                     taskRunner));
        } catch (final OpenR66RunnerErrorException e1) {
          transferInError(e1);
          return;
        } catch (final OpenR66ProtocolSystemException e1) {
          transferInError(e1);
          return;
        }
      }
      localChannelReference.getFutureEndTransfer().awaitOrInterruptible();
      logger.debug("Await future End Transfer done: {}",
                   localChannelReference.getFutureEndTransfer().isSuccess());
      if (localChannelReference.getFutureEndTransfer().isSuccess()) {
        // send a validation
        localChannelReference.sessionNewState(R66FiniteDualStates.ENDREQUESTS);
        final EndRequestPacket validPacket =
            new EndRequestPacket(ErrorCode.CompleteOk.ordinal());
        final R66Session session = localChannelReference.getSession();
        if (session != null && session.getExtendedProtocol() &&
            session.getBusinessObject() != null &&
            session.getBusinessObject().getInfo(session) != null) {
          validPacket.setOptional(session.getBusinessObject().getInfo(session));
        }
        try {
          ChannelUtils
              .writeAbstractLocalPacket(localChannelReference, validPacket,
                                        false);
        } catch (final OpenR66ProtocolPacketException ignored) {
          // nothing
        }
        if (!localChannelReference.getFutureRequest().awaitOrInterruptible()) {
          // valid it however
          localChannelReference.validateRequest(
              localChannelReference.getFutureEndTransfer().getResult());
        }
        if (taskRunner != null && taskRunner.isRequestOnRequested()) {
          localChannelReference.close();
        }
      } else {
        transferInError(null);
      }
    } finally {
      if (taskRunner != null) {
        if (future.isDone() && !future.isSuccess() || nolog) {
          try {
            taskRunner.delete();
          } catch (final WaarpDatabaseException ignored) {
            // nothing
          }
        }
      }
    }
  }

  /**
   * To be used in case of error after a correct initiate of the request
   *
   * @param e
   */
  public void transferInError(final OpenR66Exception e) {
    if (localChannelReference != null) {
      if (!localChannelReference.getFutureEndTransfer().getResult()
                                .isAnswered()) {
        final R66Result result =
            new R66Result(e, localChannelReference.getSession(), true,
                          ErrorCode.TransferError, taskRunner);
        logger.error("Transfer in error", e);
        localChannelReference.sessionNewState(R66FiniteDualStates.ERROR);
        final ErrorPacket error = new ErrorPacket("Transfer in error",
                                                  ErrorCode.TransferError
                                                      .getCode(),
                                                  ErrorPacket.FORWARDCLOSECODE);
        try {
          ChannelUtils
              .writeAbstractLocalPacket(localChannelReference, error, false);
        } catch (final OpenR66ProtocolPacketException ignored) {
          // nothing
        }
        localChannelReference.invalidateRequest(result);
      }
      localChannelReference.close();
    }
  }

  /**
   * Write the next block when the channel is ready to prevent OOM
   *
   * @param block
   *
   * @return the ChannelFuture on the write operation
   *
   * @throws OpenR66ProtocolPacketException
   */
  public ChannelFuture writeWhenPossible(final DataBlock block)
      throws OpenR66ProtocolPacketException {
    return RetrieveRunner
        .writeWhenPossible(block, localChannelReference, null, null);
  }

  /**
   * Utility method for send through mode
   *
   * @param data the data byte, if null it is the last block
   * @param length length of data
   *
   * @return the DataBlock associated to the data
   */
  public DataBlock transformToDataBlock(final byte[] data, final int length) {
    final DataBlock block = new DataBlock();
    if (data == null) {
      // last block
      block.setEOF(true);
    } else {
      block.setBlock(data, length);
    }
    return block;
  }
}
