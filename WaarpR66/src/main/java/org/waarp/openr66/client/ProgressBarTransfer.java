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

import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
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
 * Through API Transfer from a client with or without database connection, and
 * enabling access to statistic of
 * the transfer (unblocking transfer)
 *
 *
 */
public abstract class ProgressBarTransfer extends AbstractTransfer {
  protected final NetworkTransaction networkTransaction;
  protected long INTERVALCALLBACK = 100;
  protected long filesize = 0;

  public ProgressBarTransfer(R66Future future, String remoteHost,
                             String filename, String rulename, String fileinfo,
                             boolean isMD5, int blocksize, long id,
                             NetworkTransaction networkTransaction,
                             long callbackdelay) {
    // no delay so starttime = null
    super(ProgressBarTransfer.class, future, filename, rulename, fileinfo,
          isMD5, remoteHost, blocksize, id, null);
    this.networkTransaction = networkTransaction;
    INTERVALCALLBACK = callbackdelay;
  }

  /**
   * This function will be called every 100ms (or other fixed value in
   * INTERVALCALLBACK). Note that final rank
   * is unknown.
   *
   * @param currentBlock the current block rank (from 0 to n-1)
   * @param blocksize blocksize of 1 block
   */
  abstract public void callBack(int currentBlock, int blocksize);

  /**
   * This function will be called only once when the transfer is over
   *
   * @param success True if the transfer is successful
   * @param currentBlock
   * @param blocksize
   */
  abstract public void lastCallBack(boolean success, int currentBlock,
                                    int blocksize);

  /**
   * Prior to call this method, the pipeline and NetworkTransaction must have
   * been initialized. It is the
   * responsibility of the caller to finish all network resources.
   */
  @Override
  public void run() {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ProgressBarTransfer.class);
    }
    final DbTaskRunner taskRunner = initRequest();
    if (taskRunner == null) {
      // already an error from there
      lastCallBack(false, 0, blocksize);
      return;
    }
    final ClientRunner runner =
        new ClientRunner(networkTransaction, taskRunner, future);
    OpenR66ProtocolNotYetConnectionException exc = null;
    for (int i = 0; i < Configuration.RETRYNB; i++) {
      try {
        logger.debug("starting connection done on progressBarTransfer");
        final LocalChannelReference localChannelReference =
            runner.initRequest();
        localChannelReference.getFutureValidRequest()
                             .awaitOrInterruptible();
        if ((!localChannelReference.getFutureValidRequest().isSuccess()) &&
            (localChannelReference.getFutureValidRequest() != null &&
             localChannelReference.getFutureValidRequest().getResult()
                                  .getCode() == ErrorCode.ServerOverloaded)) {
          switch (taskRunner.getUpdatedInfo()) {
            case DONE:
            case INERROR:
            case INTERRUPTED:
              break;
            default:
              runner.changeUpdatedInfo(UpdatedInfo.INERROR,
                                       ErrorCode.ServerOverloaded, true);
          }
          // redo if possible
          if (runner
              .incrementTaskRunnerTry(taskRunner, Configuration.RETRYNB)) {
            try {
              Thread.sleep(
                  Configuration.configuration.getConstraintLimitHandler()
                                             .getSleepTime());
            } catch (final InterruptedException e) {
            }
            i--;
            continue;
          } else {
            throw new OpenR66ProtocolNotYetConnectionException(
                "End of retry on ServerOverloaded");
          }
        }
        logger.debug("connection done on progressBarTransfer");
        filesize = future.getFilesize();
        while (!future.awaitOrInterruptible(INTERVALCALLBACK)) {
          if (future.isDone()) {
            break;
          }
          callBack(future.getRunner().getRank(),
                   future.getRunner().getBlocksize());
        }
        logger.debug("transfer done on progressBarTransfer");
        runner.finishTransfer(localChannelReference);
        lastCallBack(future.isSuccess(), future.getRunner().getRank(),
                     future.getRunner().getBlocksize());
        exc = null;
        break;
      } catch (final OpenR66RunnerErrorException e) {
        logger.error("Cannot Transfer", e);
        future.setResult(
            new R66Result(e, null, true, ErrorCode.Internal, taskRunner));
        future.setFailure(e);
        lastCallBack(false, taskRunner.getRank(), taskRunner.getBlocksize());
        return;
      } catch (final OpenR66ProtocolNoConnectionException e) {
        logger.error("Cannot Connect", e);
        future.setResult(
            new R66Result(e, null, true, ErrorCode.ConnectionImpossible,
                          taskRunner));
        finalizeInErrorTransferRequest(runner, taskRunner,
                                       ErrorCode.ConnectionImpossible);
        // since no connection : just forget it
        if (nolog || taskRunner.shallIgnoreSave()) {
          try {
            taskRunner.delete();
          } catch (final WaarpDatabaseException e1) {
          }
        }
        future.setFailure(e);
        lastCallBack(false, taskRunner.getRank(), taskRunner.getBlocksize());
        return;
      } catch (final OpenR66ProtocolPacketException e) {
        logger.error("Bad Protocol", e);
        future.setResult(
            new R66Result(e, null, true, ErrorCode.TransferError, taskRunner));
        future.setFailure(e);
        lastCallBack(false, taskRunner.getRank(), taskRunner.getBlocksize());
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
      lastCallBack(false, taskRunner.getRank(), taskRunner.getBlocksize());
      // since no connection : just forget it
      if (nolog || taskRunner.shallIgnoreSave()) {
        try {
          taskRunner.delete();
        } catch (final WaarpDatabaseException e1) {
        }
      }
      future.setFailure(exc);
      return;
    }
  }
}
