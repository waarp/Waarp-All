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

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerException;
import org.waarp.openr66.database.data.DbTaskRunner;

/**
 * This task checks some properties relative to the File according to the
 * following argument:<br>
 * - SIZE LT/GT/LTE/GTE/EQ number : to check the file size according to a limit
 * (less than, greater than, less
 * than or equal, greater than or equal, equal)<br>
 * - DFCHECK : to check if the future received file size is compatible with the
 * space left on both working and
 * received directories (from general configuration or rule configuration)<br>
 * <br>
 * For instance "SIZE LT 10000000 SIZE GT 1000 DFCHECK" will test that the
 * current file size is less than 10
 * MB (base 10), and greater than 1000 bytes, and that the working and received
 * directories have enough space
 * to receive the file.
 */
public class FileCheckTask extends AbstractTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FileCheckTask.class);

  private enum FC_COMMAND {
    SIZE, DFCHECK
  }

  private enum FC_ARGSSIZE {
    LT, GT, LTE, GTE, EQ
  }

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public FileCheckTask(final String argRule, final int delay,
                       final String argTransfer, final R66Session session) {
    super(TaskType.CHKFILE, delay, argRule, argTransfer, session);
  }

  @Override
  public void run() {
    final DbTaskRunner runner = session.getRunner();
    if (runner == null) {
      // no information so in error
      logger.error("No Runner task so cannot check file: " + session);
      futureCompletion.setFailure(
          new OpenR66RunnerException("No Runner task so cannot check File"));
      return;
    }
    final long supposelength = runner.getOriginalSize();
    if (supposelength <= 0) {
      // no information but could be not an error so ignore
      logger.warn("No file size known: " + session);
      futureCompletion.setSuccess();
      return;
    }
    final String[] commands = BLANK.split(argRule);
    int current = 0;
    while (current < commands.length) {
      if (commands[current].equalsIgnoreCase(FC_COMMAND.SIZE.name())) {
        current++;
        FC_ARGSSIZE arg;
        try {
          arg = FC_ARGSSIZE.valueOf(commands[current]);
        } catch (final IllegalArgumentException e) {
          arg = null;
        }
        if (arg != null) {
          current++;
          try {
            final long tocompare = Long.parseLong(commands[current]);
            current++;
            final boolean result;
            switch (arg) {
              case EQ:
                result = supposelength == tocompare;
                break;
              case GT:
                result = supposelength > tocompare;
                break;
              case GTE:
                result = supposelength >= tocompare;
                break;
              case LT:
                result = supposelength < tocompare;
                break;
              case LTE:
                result = supposelength <= tocompare;
                break;
              default:
                result = true; // ??
                break;
            }
            logger.debug(
                "DEBUG: " + supposelength + ' ' + arg.name() + ' ' + tocompare);
            if (!result) {
              // error so stop
              logger.error(
                  "File length is incompatible with specified SIZE comparizon: " +
                  supposelength + " NOT " + arg.name() + ' ' + tocompare);
              futureCompletion.setResult(
                  new R66Result(session, false, ErrorCode.SizeNotAllowed,
                                runner));
              futureCompletion.setFailure(
                  new OpenR66RunnerException("File size incompatible"));
              return;
            }
          } catch (final NumberFormatException e) {
            // ignore and continue
          }
        }
      } else if (commands[current]
          .equalsIgnoreCase(FC_COMMAND.DFCHECK.name())) {
        current++;
        long freesize = runner.freespace(session, true);
        if (freesize > 0 && supposelength > freesize) {
          // error so stop
          logger.error(
              "File length is incompatible with available space in Working directory: " +
              supposelength + " > " + freesize);
          futureCompletion.setResult(
              new R66Result(session, false, ErrorCode.SizeNotAllowed, runner));
          futureCompletion.setFailure(new OpenR66RunnerException(
              "File size incompatible with Working directory"));
          return;
        }
        logger.debug("DEBUG: " + supposelength + " < " + freesize);
        freesize = runner.freespace(session, false);
        if (freesize > 0 && supposelength > freesize) {
          // error so stop
          logger.error(
              "File length is incompatible with available space in Recv directory: " +
              supposelength + " > " + freesize);
          futureCompletion.setResult(
              new R66Result(session, false, ErrorCode.SizeNotAllowed, runner));
          futureCompletion.setFailure(new OpenR66RunnerException(
              "File size incompatible with Recv directory"));
          return;
        }
        logger.debug("DEBUG: " + supposelength + " < " + freesize);
      } else {
        // ignore and continue
        current++;
      }
    }
    logger.debug("DEBUG: End of check " + supposelength);
    futureCompletion.setSuccess();
  }

}
