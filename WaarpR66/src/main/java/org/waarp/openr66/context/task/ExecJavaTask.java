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
package org.waarp.openr66.context.task;

import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.common.utility.WaarpThreadFactory;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Execute a Java command through Class.forName call
 */
public class ExecJavaTask extends AbstractTask {
  protected boolean businessRequest;

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(ExecJavaTask.class);

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public ExecJavaTask(final String argRule, final int delay,
                      final String argTransfer, final R66Session session) {
    super(TaskType.EXECJAVA, delay, argRule, argTransfer, session);
  }

  /**
   * Set the type
   *
   * @param businessRequest
   */
  public void setBusinessRequest(final boolean businessRequest) {
    this.businessRequest = businessRequest;
  }

  @Override
  public void run() {
    if (argRule == null) {
      logger.error(
          "ExecJava cannot be done with " + argRule + ':' + argTransfer +
          " and " + session);
      futureCompletion.setFailure(
          new OpenR66ProtocolSystemException("Exec Java cannot be done"));
      return;
    }
    /*
     * First apply all replacements and format to argRule from context and argTransfer. Will call exec (from first
     * element of resulting string) with arguments as the following value from the replacements. Return 0 if OK,
     * else 1 for a warning else as an error. No change should be done in the FILENAME
     */
    String finalname = argRule;
    if (argTransfer != null) {
      finalname = getReplacedValue(finalname, BLANK.split(argTransfer));
    }
    // First get the Class Name
    final String[] args = BLANK.split(finalname);
    final String className = args[0];
    try {
      ParametersChecker.checkSanityString(className);
    } catch (final InvalidArgumentException e) {
      logger.error("ExecJava command is not correct", e);
      final R66Result result =
          new R66Result(session, false, ErrorCode.CommandNotFound,
                        session.getRunner());
      futureCompletion.setResult(result);
      futureCompletion.cancel();
      return;
    }
    final boolean isSpooled =
        className.equals(SpooledInformTask.class.getName());
    if (isSpooled) {
      logger.debug("Exec with {}:{} and {}", className, argTransfer, session);
    } else {
      logger.debug("Exec with {}:{} and {}", argRule, argTransfer, session);
    }
    final R66Runnable runnable;
    try {
      runnable = (R66Runnable) WaarpSystemUtil.newInstance(className);//NOSONAR
    } catch (final Exception e) {
      logger.error("ExecJava command is not available: " + className, e);
      final R66Result result =
          new R66Result(session, false, ErrorCode.CommandNotFound,
                        session.getRunner());
      futureCompletion.setResult(result);
      futureCompletion.cancel();
      return;
    }
    if (businessRequest) {
      final boolean istovalidate = Boolean.parseBoolean(args[args.length - 1]);
      runnable
          .setArgs(session, waitForValidation, useLocalExec, delay, className,
                   finalname.substring(finalname.indexOf(' ') + 1,
                                       finalname.lastIndexOf(' ')),
                   businessRequest, istovalidate);
    } else {
      runnable
          .setArgs(session, waitForValidation, useLocalExec, delay, className,
                   finalname.substring(className.length() + 1), businessRequest,
                   false);
    }
    logger.debug("{} {}", className, runnable.getClass().getName());
    if (!waitForValidation) {
      // Do not wait for validation
      futureCompletion.setSuccess();
      logger.info("Exec will start but no WAIT with {}", runnable);
    }
    final int status;
    if (waitForValidation && delay <= 100) {
      runnable.run();
      status = runnable.getFinalStatus();
    } else {
      final ExecutorService executorService = Executors
          .newSingleThreadExecutor(new WaarpThreadFactory("JavaExecutor"));
      executorService.execute(runnable);
      try {
        Thread.yield();
        executorService.shutdown();
        if (waitForValidation) {
          if (delay > 100) {
            if (!executorService
                .awaitTermination(delay, TimeUnit.MILLISECONDS)) {
              executorService.shutdownNow();
              logger.error("Exec is in Time Out");
              status = -1;
            } else {
              status = runnable.getFinalStatus();
            }
          } else {
            while (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
              // nothing
            }
            status = runnable.getFinalStatus();
          }
        } else {
          while (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
            // nothing
          }
          status = runnable.getFinalStatus();
        }
      } catch (final InterruptedException e) {//NOSONAR
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        logger.error(
            "Status: " + e.getMessage() + " \t Exec in error with " + runnable);
        if (waitForValidation) {
          futureCompletion.cancel();
        }
        return;
      }
    }
    if (status == 0) {
      if (waitForValidation) {
        final R66Result result =
            new R66Result(session, true, ErrorCode.CompleteOk, null);
        result.setOther(runnable.toString());
        futureCompletion.setResult(result);
        futureCompletion.setSuccess();
      }
      if (isSpooled) {
        logger.info("Exec OK with {}", className);
      } else {
        logger.info("Exec OK with {}", runnable);
      }
    } else if (status == 1) {
      logger.warn("Exec in warning with " + runnable);
      if (waitForValidation) {
        final R66Result result =
            new R66Result(session, true, ErrorCode.CompleteOk, null);
        result.setOther(runnable.toString());
        futureCompletion.setResult(result);
        futureCompletion.setSuccess();
      }
    } else {
      logger.error("Status: " + status + " Exec in error with " + runnable);
      if (waitForValidation) {
        futureCompletion.cancel();
      }
    }
  }

}
