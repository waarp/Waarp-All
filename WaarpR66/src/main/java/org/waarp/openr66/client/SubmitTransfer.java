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

import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.client.utils.OutputFormat;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66DatabaseGlobalException;
import org.waarp.openr66.protocol.utils.R66Future;

import java.sql.Timestamp;

import static org.waarp.common.database.DbConstant.*;

/**
 * Client to submit a transfer
 */
public class SubmitTransfer extends AbstractTransfer {

  public SubmitTransfer(final R66Future future, final String remoteHost,
                        final String filename, final String rulename,
                        final String transferInfo, final boolean isMD5,
                        final int blocksize, final long id,
                        final Timestamp starttime) {
    super(SubmitTransfer.class, future, filename, rulename, transferInfo, isMD5,
          remoteHost, blocksize, id, starttime);
  }

  @Override
  public void run() {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(SubmitTransfer.class);
    }
    final DbTaskRunner taskRunner = initRequest();
    if (taskRunner == null) {
      logger.info("Cannot prepare task");
      if (future.isFailed() && future.getResult() != null) {
        return;
      }
      final R66Result result =
          new R66Result(new OpenR66DatabaseGlobalException(), null, true,
                        ErrorCode.Internal, null);
      future.setResult(result);
      future.setFailure(result.getException());
      return;
    }
    if (transferArgs.getId() != ILLEGALVALUE) {
      // Resubmit call, some checks are needed
      if (!taskRunner.restart(true)) {
        // cannot be done from there => must be done through IHM
        logger.info("Cannot prepare task from there. IHM must be used");
        final R66Result result = new R66Result(
            new OpenR66DatabaseGlobalException(
                "Cannot prepare task from there. IHM must be used"), null, true,
            ErrorCode.Internal, taskRunner);
        future.setResult(result);
        future.setFailure(result.getException());
        return;
      }
    } else {
      taskRunner.changeUpdatedInfo(AbstractDbData.UpdatedInfo.TOSUBMIT);
    }
    if (!taskRunner.forceSaveStatus()) {
      try {
        if (!taskRunner.specialSubmit()) {
          logger.info("Cannot prepare task");
          final R66Result result = new R66Result(
              new OpenR66DatabaseGlobalException("Cannot prepare Task"), null,
              true, ErrorCode.Internal, taskRunner);
          future.setResult(result);
          future.setFailure(result.getException());
          return;
        }
      } catch (final WaarpDatabaseException e) {
        logger.info("Cannot prepare task");
        final R66Result result = new R66Result(
            new OpenR66DatabaseGlobalException("Cannot prepare Task", e), null,
            true, ErrorCode.Internal, taskRunner);
        future.setResult(result);
        future.setFailure(result.getException());
        return;
      }
    }
    final R66Result result =
        new R66Result(null, false, ErrorCode.InitOk, taskRunner);
    future.setResult(result);
    future.setSuccess();
  }

  /**
   * @param args configuration file, the remoteHost Id, the file to
   *     transfer, the rule, file transfer
   *     information as arguments and optionally isMD5=1 for true or 0 for
   *     false(default) and the
   *     blocksize if different than default
   */
  public static void main(final String[] args) {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(SubmitTransfer.class);
    }
    if (!getParams(args, true)) {
      logger.error(Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
      if (!OutputFormat.isQuiet()) {
        SysErrLogger.FAKE_LOGGER.sysout(
            Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
      }
      if (admin != null) {
        admin.close();
      }
      WaarpSystemUtil.systemExit(1);
      return;
    }
    final R66Future future = new R66Future(true);
    final SubmitTransfer transaction =
        new SubmitTransfer(future, rhost, localFilename, rule, transferInfo,
                           ismd5, block, idt, ttimestart);
    transaction.normalInfoAsWarn = snormalInfoAsWarn;
    transaction.run();
    future.awaitOrInterruptible();
    final DbTaskRunner runner = future.getResult().getRunner();
    final OutputFormat outputFormat =
        new OutputFormat(SubmitTransfer.class.getSimpleName(), args);
    if (future.isSuccess()) {
      prepareSubmitOkOutputFormat(runner, outputFormat);
      if (transaction.normalInfoAsWarn) {
        logger.warn(outputFormat.loggerOut());
      } else if (logger.isInfoEnabled()) {
        logger.info(outputFormat.loggerOut());
      }
      if (!OutputFormat.isQuiet()) {
        outputFormat.sysout();
      }
    } else {
      prepareSubmitKoOutputFormat(future, runner, outputFormat);
      if (!OutputFormat.isQuiet()) {
        outputFormat.sysout();
      }
      if (WaarpSystemUtil.isJunit()) {
        return;
      }
      admin.close();
      WaarpSystemUtil.systemExit(future.getResult().getCode().ordinal());
      return;
    }
    if (!WaarpSystemUtil.isJunit()) {
      admin.close();
      WaarpSystemUtil.systemExit(0);
    }
  }

}
