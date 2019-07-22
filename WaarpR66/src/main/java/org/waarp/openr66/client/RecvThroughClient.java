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

import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.commander.ClientRunner;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotYetConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.R66Future;

/**
 * Class for Recv Through client
 * <p>
 * This class does not included the real file transfer since it is up to the
 * business project to implement how
 * to write new data received from the remote host. If an error occurs, no
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
 * <tt>     RecvThroughHandler rth = new RecvThroughHandler(...);</tt><br>
 * <tt>     RecvThroughClient transaction = new RecvThroughClient(futureReq,
 * rth, ...);</tt><br>
 * <tt>     transaction.run();</tt><br>
 * <br>
 * 4) If everything is in success, wait for the transfer to finish:<br>
 * <tt>     futureReq.awaitUninterruptibly();</tt><br>
 * <tt>     R66Result result = futureReq.getResult();</tt><br>
 * <br>
 * 5) If there is the need to re-do, just re-execute the steps from 3 to 4.<br>
 * Don't forget at the very end to finish the global structure (steps 3 to 4 no
 * more executed):<br>
 * <tt>     networkTransaction.closeAll();</tt><br>
 * <br>
 * <br>
 *
 *
 * @see TestRecvThroughClient Class as example of usage in test part
 */
public class RecvThroughClient extends AbstractTransfer {
  protected final NetworkTransaction networkTransaction;
  protected LocalChannelReference localChannelReference;
  protected final RecvThroughHandler handler;

  /**
   * @param future
   * @param remoteHost
   * @param filename
   * @param rulename
   * @param fileinfo
   * @param isMD5
   * @param blocksize
   * @param id
   * @param networkTransaction
   */
  public RecvThroughClient(R66Future future, RecvThroughHandler handler,
                           String remoteHost, String filename, String rulename,
                           String fileinfo, boolean isMD5, int blocksize,
                           long id, NetworkTransaction networkTransaction) {
    // timestart since immediate
    super(RecvThroughClient.class, future, filename, rulename, fileinfo, isMD5,
          remoteHost, blocksize, id, null);
    this.networkTransaction = networkTransaction;
    this.handler = handler;
  }

  /**
   * Prior to call this method, the pipeline and NetworkTransaction must have
   * been initialized. It is the
   * responsibility of the caller to finish all network resources.
   */
  @Override
  public void run() {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(RecvThroughClient.class);
    }
    final DbTaskRunner taskRunner = initRequest();
    if (taskRunner == null) {
      // already an error from there
      return;
    }
    try {
      final ClientRunner runner =
          new ClientRunner(networkTransaction, taskRunner, future);
      runner.setRecvThroughHandler(handler);
      OpenR66ProtocolNotYetConnectionException exc = null;
      for (int i = 0; i < Configuration.RETRYNB; i++) {
        try {
          runner.runTransfer();
          exc = null;
          break;
        } catch (final OpenR66RunnerErrorException e) {
          logger.error("Cannot Transfer", e);
          future.setResult(
              new R66Result(e, null, true, ErrorCode.Internal, taskRunner));
          future.setFailure(e);
          return;
        } catch (final OpenR66ProtocolNoConnectionException e) {
          logger.error("Cannot Connect", e);
          future.setResult(
              new R66Result(e, null, true, ErrorCode.ConnectionImpossible,
                            taskRunner));
          finalizeInErrorTransferRequest(runner, taskRunner,
                                         ErrorCode.ConnectionImpossible);
          future.setFailure(e);
          return;
        } catch (final OpenR66ProtocolPacketException e) {
          logger.error("Bad Protocol", e);
          future.setResult(new R66Result(e, null, true, ErrorCode.TransferError,
                                         taskRunner));
          future.setFailure(e);
          return;
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
        return;
      }
    } finally {
      if (taskRunner != null) {
        if (future.isFailed() || nolog || taskRunner.shallIgnoreSave()) {
          try {
            taskRunner.delete();
          } catch (final WaarpDatabaseException e) {
          }
        }
      }
    }
  }

}
