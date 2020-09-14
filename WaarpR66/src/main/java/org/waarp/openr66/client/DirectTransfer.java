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
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.openr66.client.utils.OutputFormat;
import org.waarp.openr66.commander.ClientRunner;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNotYetConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import static org.waarp.common.database.DbConstant.*;

/**
 * Direct Transfer from a client with or without database connection
 */
public class DirectTransfer extends AbstractTransfer {
  protected final NetworkTransaction networkTransaction;

  protected boolean limitRetryConnection = true;

  public DirectTransfer(final R66Future future, final String remoteHost,
                        final String filename, final String rulename,
                        final String transferInfo, final boolean isMD5,
                        final int blocksize, final long id,
                        final NetworkTransaction networkTransaction) {
    // no starttime since it is direct (blocking request, no delay)
    super(DirectTransfer.class, future, filename, rulename, transferInfo, isMD5,
          remoteHost, blocksize, id, null);
    this.networkTransaction = networkTransaction;
  }

  /**
   * @return True if this DirectTransfer should limit the retry of connection
   */
  public boolean isLimitRetryConnection() {
    return limitRetryConnection;
  }

  /**
   * @param limitRetryConnection True (default) for limited retry on
   *     connection, False to have no limit
   */
  public void setLimitRetryConnection(final boolean limitRetryConnection) {
    this.limitRetryConnection = limitRetryConnection;
  }

  /**
   * Prior to call this method, the pipeline and NetworkTransaction must have
   * been initialized. It is the
   * responsibility of the caller to finish all network resources.
   */
  @Override
  public void run() {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(DirectTransfer.class);
    }
    final DbTaskRunner taskRunner = initRequest();
    if (taskRunner == null) {
      // already an error from there
      return;
    }
    final ClientRunner runner =
        new ClientRunner(networkTransaction, taskRunner, future);
    // If retry indefinitely is useful
    runner.setLimitRetryConnection(isLimitRetryConnection());
    OpenR66ProtocolNotYetConnectionException exc = null;
    for (int i = 0; i < Configuration.RETRYNB; i++) {
      try {
        runner.runTransfer();
        exc = null;
        break;
      } catch (final OpenR66RunnerErrorException e) {
        logger.info("Cannot Transfer", e);
        future.setResult(
            new R66Result(e, null, true, ErrorCode.Internal, taskRunner));
        future.setFailure(e);
        return;
      } catch (final OpenR66ProtocolNoConnectionException e) {
        logger.info("Cannot Connect", e);
        future.setResult(
            new R66Result(e, null, true, ErrorCode.ConnectionImpossible,
                          taskRunner));
        finalizeInErrorTransferRequest(runner, taskRunner,
                                       ErrorCode.ConnectionImpossible);
        // since no connection : just forget it
        if (nolog || taskRunner.shallIgnoreSave()) {
          try {
            taskRunner.delete();
          } catch (final WaarpDatabaseException ignored) {
            // nothing
          }
        }
        future.setFailure(e);
        return;
      } catch (final OpenR66ProtocolPacketException e) {
        logger.info("Bad Protocol", e);
        future.setResult(
            new R66Result(e, null, true, ErrorCode.TransferError, taskRunner));
        future.setFailure(e);
        return;
      } catch (final OpenR66ProtocolNotYetConnectionException e) {
        logger.debug("Not Yet Connected", e);
        exc = e;
      }
    }
    if (exc != null) {
      taskRunner.setLocalChannelReference(new LocalChannelReference());
      logger.info("Cannot Connect", exc);
      future.setResult(
          new R66Result(exc, null, true, ErrorCode.ConnectionImpossible,
                        taskRunner));
      // since no connection : just forget it
      if (nolog || taskRunner.shallIgnoreSave()) {
        try {
          taskRunner.delete();
        } catch (final WaarpDatabaseException ignored) {
          // nothing
        }
      }
      future.setFailure(exc);
    }
  }

  public static void main(final String[] args) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(DirectTransfer.class);
    }
    if (!getParams(args, false)) {
      logger.error(Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
      if (!OutputFormat.isQuiet()) {
        SysErrLogger.FAKE_LOGGER.sysout(
            Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
      }
      if (admin != null) {
        admin.close();
      }
      if (DetectionUtils.isJunit()) {
        return;
      }
      ChannelUtils.stopLogger();
      System.exit(2);//NOSONAR
    }
    final long time1 = System.currentTimeMillis();
    final R66Future future = new R66Future(true);

    Configuration.configuration.pipelineInit();
    final NetworkTransaction networkTransaction = new NetworkTransaction();
    try {
      final DirectTransfer transaction =
          new DirectTransfer(future, rhost, localFilename, rule, transferInfo,
                             ismd5, block, idt, networkTransaction);
      transaction.transferArgs.setFollowId(sFollowId);
      transaction.normalInfoAsWarn = snormalInfoAsWarn;
      logger.debug("rhost: {}:{}", rhost,
                   transaction.transferArgs.getRemoteHost());
      transaction.run();
      future.awaitOrInterruptible();
      final long time2 = System.currentTimeMillis();
      logger.debug("finish transfer: {}", future.isSuccess());
      final long delay = time2 - time1;
      final R66Result result = future.getResult();
      final OutputFormat outputFormat =
          new OutputFormat(DirectTransfer.class.getSimpleName(), args);
      if (future.isSuccess()) {
        prepareOkOutputFormat(delay, result, outputFormat);
        if (transaction.normalInfoAsWarn) {
          logger.warn(outputFormat.loggerOut());
        } else if (logger.isInfoEnabled()) {
          logger.info(outputFormat.loggerOut());
        }
        if (!OutputFormat.isQuiet()) {
          outputFormat.sysout();
        }
        if (nolog) {
          // In case of success, delete the runner
          try {
            result.getRunner().delete();
          } catch (final WaarpDatabaseException e) {
            logger.warn("Cannot apply nolog to     " +
                        result.getRunner().toShortString(), e);
          }
        }
        if (DetectionUtils.isJunit()) {
          return;
        }
        networkTransaction.closeAll();
        System.exit(0);//NOSONAR
      } else {
        if (result == null || result.getRunner() == null) {
          prepareKoOutputFormat(future, outputFormat);
          if (!OutputFormat.isQuiet()) {
            outputFormat.sysout();
          }
          if (DetectionUtils.isJunit()) {
            return;
          }
          networkTransaction.closeAll();
          System.exit(ErrorCode.Unknown.ordinal());//NOSONAR
        }
        prepareKoOutputFormat(future, result, outputFormat);
        if (!OutputFormat.isQuiet()) {
          outputFormat.sysout();
        }
        if (DetectionUtils.isJunit()) {
          return;
        }
        networkTransaction.closeAll();
        System.exit(result.getCode().ordinal());//NOSONAR
      }
    } catch (final Throwable e) {
      logger.error("Exception", e);
    } finally {
      logger
          .debug("finish transfer: {}:{}", future.isDone(), future.isSuccess());
      if (!DetectionUtils.isJunit()) {
        networkTransaction.closeAll();
        // In case something wrong append
        if (future.isDone() && future.isSuccess()) {
          System.exit(0);//NOSONAR
        } else {
          System.exit(66);//NOSONAR
        }
      }
    }
  }

}
