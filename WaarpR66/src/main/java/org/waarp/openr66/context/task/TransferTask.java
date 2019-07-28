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

import org.apache.commons.exec.CommandLine;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.client.SubmitTransfer;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.utils.R66Future;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Transfer task:<br>
 * <p>
 * Result of arguments will be as r66send command.<br>
 * Format is like r66send command in any order except "-info" which should be
 * the last item, and "-copyinfo"
 * will copy at first place the original transfer information as the new one,
 * while still having the
 * possibility to add new informations through "-info":<br>
 * "-file filepath -to requestedHost -rule rule [-md5] [-start yyyyMMddHHmmss or
 * -delay (delay or +delay)]
 * [-copyinfo] [-info information]" <br>
 * <br>
 * INFO is the only one field that can contains blank character.<br>
 */
public class TransferTask extends AbstractExecTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(TransferTask.class);

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public TransferTask(String argRule, int delay, String argTransfer,
                      R66Session session) {
    super(TaskType.TRANSFER, delay, argRule, argTransfer, session);
  }

  @Override
  public void run() {
    logger.info("Transfer with " + argRule + ':' + argTransfer + " and {}",
                session);
    String finalname = applyTransferSubstitutions(argRule);

    finalname = finalname.replaceAll("#([A-Z]+)#", "\\${$1}");
    final CommandLine commandLine = new CommandLine("dummy");
    commandLine.setSubstitutionMap(getSubstitutionMap());
    commandLine.addArguments(finalname, false);
    final String[] args = commandLine.getArguments();

    if (args.length < 6) {
      futureCompletion.setFailure(
          new OpenR66RunnerErrorException("Not enough argument in Transfer"));
      return;
    }
    String filepath = null;
    String requested = null;
    String rule = null;
    StringBuilder information = null;
    String finalInformation = null;
    boolean isMD5 = false;
    int blocksize = Configuration.configuration.getBlockSize();
    Timestamp timestart = null;
    for (int i = 0; i < args.length; i++) {
      if ("-to".equalsIgnoreCase(args[i])) {
        i++;
        requested = args[i];
        if (Configuration.configuration.getAliases().containsKey(requested)) {
          requested = Configuration.configuration.getAliases().get(requested);
        }
      } else if ("-file".equalsIgnoreCase(args[i])) {
        i++;
        filepath = args[i];
      } else if ("-rule".equalsIgnoreCase(args[i])) {
        i++;
        rule = args[i];
      } else if ("-copyinfo".equalsIgnoreCase(args[i])) {
        finalInformation = argTransfer;
      } else if ("-info".equalsIgnoreCase(args[i])) {
        i++;
        information = new StringBuilder(args[i]);
        i++;
        while (i < args.length) {
          information.append(' ').append(args[i]);
          i++;
        }
      } else if ("-md5".equalsIgnoreCase(args[i])) {
        isMD5 = true;
      } else if ("-block".equalsIgnoreCase(args[i])) {
        i++;
        blocksize = Integer.parseInt(args[i]);
        if (blocksize < 100) {
          logger.warn("Block size is too small: " + blocksize);
          blocksize = Configuration.configuration.getBlockSize();
        }
      } else if ("-start".equalsIgnoreCase(args[i])) {
        i++;
        final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyyMMddHHmmss");
        Date date;
        try {
          date = dateFormat.parse(args[i]);
          timestart = new Timestamp(date.getTime());
        } catch (final ParseException ignored) {
          // nothing
        }
      } else if ("-delay".equalsIgnoreCase(args[i])) {
        i++;
        try {
          if (args[i].charAt(0) == '+') {
            timestart = new Timestamp(System.currentTimeMillis() +
                                      Long.parseLong(args[i].substring(1)));
          } else {
            timestart = new Timestamp(Long.parseLong(args[i]));
          }
        } catch (final NumberFormatException ignored) {
          // nothing
        }
      }
    }
    if (information != null) {
      if (finalInformation == null) {
        finalInformation = information.toString();
      } else {
        finalInformation += " " + information;
      }
    } else if (finalInformation == null) {
      finalInformation = "noinfo";
    }
    final R66Future future = new R66Future(true);
    final SubmitTransfer transaction =
        new SubmitTransfer(future, requested, filepath, rule, finalInformation,
                           isMD5, blocksize, DbConstantR66.ILLEGALVALUE,
                           timestart);
    transaction.run();
    future.awaitOrInterruptible();
    final DbTaskRunner runner;
    if (future.isSuccess()) {
      futureCompletion.setResult(future.getResult());
      runner = future.getResult().getRunner();
      logger.info(
          "Prepare transfer in     SUCCESS     " + runner.toShortString() +
          "     <REMOTE>" + requested + "</REMOTE>");
      futureCompletion.setSuccess();
    } else {
      if (future.getResult() != null) {
        futureCompletion.setResult(future.getResult());
        runner = future.getResult().getRunner();
      } else {
        runner = null;
      }
      if (runner != null) {
        if (future.getCause() == null) {
          futureCompletion.cancel();
        } else {
          futureCompletion.setFailure(future.getCause());
        }
        logger.error(
            "Prepare transfer in     FAILURE      " + runner.toShortString() +
            "     <REMOTE>" + requested + "</REMOTE>", future.getCause());
      } else {
        if (future.getCause() == null) {
          futureCompletion.cancel();
        } else {
          futureCompletion.setFailure(future.getCause());
        }
        logger.error("Prepare transfer in     FAILURE without any runner back",
                     future.getCause());
      }
    }
  }

}
