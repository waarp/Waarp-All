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

import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Session;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class is for logging or write to an external file some info:<br>
 * - if delay is 0, no echo at all will be done<br>
 * - if delay is 1, will echo some information in the normal log<br>
 * - if delay is 2, will echo some information in the file (last deduced
 * argument will be the full path for
 * the file output)<br>
 * - if delay is 3, will echo both in the normal log and in the file (last
 * deduced argument will be the full
 * path for the file output)<br>
 * <br>
 * If first word for logging is one of debug, info, warn, error, then the
 * corresponding level of log will be
 * used.
 */
public class LogTask extends AbstractTask {
  private static final String SPACES = "     ";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(LogTask.class);

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public LogTask(final String argRule, final int delay,
                 final String argTransfer, final R66Session session) {
    super(TaskType.LOG, delay, argRule, argTransfer, session);
  }

  @Override
  public void run() {
    String finalValue = argRule;
    finalValue = getReplacedValue(finalValue, BLANK.split(argTransfer));
    final String tempValue = finalValue.toUpperCase();
    WaarpLogLevel finalLevel = WaarpLogLevel.WARN;
    for (final WaarpLogLevel level : WaarpLogLevel.values()) {
      if (tempValue.startsWith(level.name())) {
        finalLevel = level;
        break;
      }
    }
    switch (delay) {
      case 0:
        break;
      case 1:
        logger.log(finalLevel, finalValue + SPACES + session);
        break;
      case 3:
        logger.log(finalLevel, finalValue + SPACES + session);
        if (runFor2Or3(finalValue, finalLevel)) {
          return;
        }
        break;
      case 2:
        if (runFor2Or3(finalValue, finalLevel)) {
          return;
        }
        break;
      default:
    }
    futureCompletion.setSuccess();
  }

  private boolean runFor2Or3(final String finalValue,
                             final WaarpLogLevel finalLevel) {
    final String[] args = BLANK.split(finalValue);
    final String filename = args[args.length - 1];
    final File file = new File(filename);
    if (file.getParentFile() == null || file.exists() && !file.canWrite()) {
      // File cannot be written so revert to log
      session.getRunner().setErrorExecutionStatus(ErrorCode.Warning);
      if (delay == 2) {
        logger.log(finalLevel, finalValue + SPACES + session);
      }
      futureCompletion.setSuccess();
      return true;
    }
    final FileOutputStream outputStream;
    try {
      outputStream = new FileOutputStream(file, true);
    } catch (final FileNotFoundException e) {
      // File cannot be written so revert to log
      session.getRunner().setErrorExecutionStatus(ErrorCode.Warning);
      if (delay == 2) {
        logger.log(finalLevel, finalValue + SPACES + session);
      }
      futureCompletion.setSuccess();
      return true;
    }
    try {
      final int len = args.length - 1;
      for (int i = 0; i < len; i++) {
        outputStream.write(args[i].getBytes());
        outputStream.write(' ');
      }
      outputStream.write(' ');
    } catch (final IOException e) {
      // File cannot be written so revert to log
      FileUtils.close(outputStream);
      file.delete();
      session.getRunner().setErrorExecutionStatus(ErrorCode.Warning);
      if (delay == 2) {
        logger.log(finalLevel, finalValue + SPACES + session);
      }
      futureCompletion.setSuccess();
      return true;
    }
    FileUtils.close(outputStream);
    return false;
  }

}
