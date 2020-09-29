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
package org.waarp.openr66.protocol.test;

import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.file.DataBlock;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.openr66.client.RecvThroughHandler;
import org.waarp.openr66.client.SendThroughClient;
import org.waarp.openr66.commander.ClientRunner;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.exception.OpenR66DatabaseGlobalException;
import org.waarp.openr66.protocol.exception.OpenR66Exception;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotYetConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.R66Future;

/**
 * <b>WARNING: This class is not functional neither integrated</b><br>
 * <p>
 * Test class for Send Through to another R66 Server as forward<br>
 * Only a subpart of SenThroughClient is to be made since steps 1-2 and steps
 * 7-8 are only for client, not for
 * server.
 * <p>
 * 3) Prepare the request of transfer:<br>
 * <tt>     R66Future futureReq = new R66Future(true);</tt><br>
 * <tt>     TestSendThroughForward transaction = new TestSendThroughForward(futureReq,...);</tt><br>
 * <tt>     if (! transaction.initiateRequest()) { error }</tt><br>
 * <br>
 * 4) Once initiateRequest() gives true, you are ready to send the data in
 * through mode using the
 * TestRecvThroughForwardHandler:<br>
 * <br>
 * 5) Once you have finished, so this is the last block, you have to do the
 * following:<br>
 * If the last block is not empty:<br>
 * <tt>     DataBlock block = transaction.transformToDataBlock(data);</tt><br>
 * <tt>     block.setEOF(true);</tt><br>
 * <tt>     futureWrite = transaction.writeWhenPossible(block);</tt><br>
 * <tt>     futureWrite.awaitUninterruptibly();</tt><br>
 * <br>
 * If the last block is empty, it is already handled by
 * TestRecvThroughForwardHandler<br>
 * <br>
 * 6) If everything is in success:<br>
 * <tt>     transaction.finalizeRequest();</tt><br>
 * <br>
 * And now wait for the transfer to finish:<br>
 * <tt>     futureReq.awaitUninterruptibly();</tt><br>
 * <tt>     R66Result result = futureReq.getResult();</tt><br>
 * <br>
 */
public class TestSendThroughForward extends SendThroughClient {
  public TestRecvThroughForwardHandler handler;
  public DbSession dbSession;
  public volatile boolean foundEOF;
  protected DbTaskRunner sourceRunner;

  /**
   * @param future
   * @param remoteHost
   * @param filename
   * @param rulename
   * @param fileinfo
   * @param isMD5
   * @param blocksize
   * @param networkTransaction
   * @param idt Id Transfer if any temptative already exists
   * @param dbSession
   * @param runner (recv runner)
   */
  public TestSendThroughForward(R66Future future, String remoteHost,
                                String filename, String rulename,
                                String fileinfo, boolean isMD5, int blocksize,
                                NetworkTransaction networkTransaction, long idt,
                                DbSession dbSession, DbTaskRunner runner) {
    super(future, remoteHost, filename, rulename, fileinfo, isMD5, blocksize,
          idt, networkTransaction);
    handler = new TestRecvThroughForwardHandler();
    handler.client = this;
    this.dbSession = dbSession;
    sourceRunner = runner;
  }

  @Override
  public boolean initiateRequest() {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(TestSendThroughForward.class);
    }
    DbRule rule;
    try {
      rule = new DbRule(transferArgs.getRulename());
    } catch (final WaarpDatabaseException e) {
      logger.error("Cannot get Rule: " + transferArgs.getRulename(), e);
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
                          transferArgs.getBlockSize(), sourceRunner.getRank(),
                          transferArgs.getId(), transferArgs.getTransferInfo(),
                          -1, sep);
    // Not isRecv since it is the requester, so send => isSender is true
    final boolean isSender = true;
    try {
      try {
        // no delay
        taskRunner = new DbTaskRunner(rule, isSender, request,
                                      transferArgs.getRemoteHost(), null);
      } catch (final WaarpDatabaseException e) {
        logger.error("Cannot get task", e);
        future.setResult(
            new R66Result(new OpenR66DatabaseGlobalException(e), null, true,
                          ErrorCode.Internal, null));
        future.setFailure(e);
        return false;
      }
      final ClientRunner runner =
          new ClientRunner(networkTransaction, taskRunner, future);
      runner.setRecvThroughHandler(handler);
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
          continue;
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
      if (taskRunner.getRank() < sourceRunner.getRank()) {
        sourceRunner.setRankAtStartup(taskRunner.getRank());
      }
      // now start the send from external data
      return true;
    } finally {
      if (taskRunner != null) {
        // not delete but sourceRunner and taskRunner should be stopped
        // and taskRunner not allowed to be restarted alone
        if (future.isFailed()) {
          taskRunner.changeUpdatedInfo(UpdatedInfo.INERROR);
          taskRunner.forceSaveStatus();
        }
      }
    }
  }

  @Override
  public void finalizeRequest() {
    if (foundEOF) {
      return;
    }
    super.finalizeRequest();
  }

  @Override
  public void transferInError(OpenR66Exception e) {
    super.transferInError(e);
  }

  public static class TestRecvThroughForwardHandler extends RecvThroughHandler {

    protected TestSendThroughForward client;

    @Override
    public void writeBytes(byte[] buffer)
        throws OpenR66ProtocolBusinessException {
      final DataBlock block = new DataBlock();
      if (buffer.length == 0) {
        // last block
        block.setEOF(true);
      } else {
        block.setBlock(buffer);
      }
      try {
        if (!WaarpNettyUtil
            .awaitOrInterrupted(client.writeWhenPossible(block))) {
          client.transferInError(
              new OpenR66ProtocolSystemException("Write impossible"));
        }
      } catch (final OpenR66ProtocolPacketException e) {
        client.transferInError(e);
      }
      if (block.isEOF()) {
        client.finalizeRequest();
        client.foundEOF = true;
      }
    }

  }

}
