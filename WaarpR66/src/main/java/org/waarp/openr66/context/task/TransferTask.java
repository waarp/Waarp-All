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
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.client.SubmitTransfer;
import org.waarp.openr66.client.TransferArgs;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.utils.R66Future;

import java.util.HashMap;
import java.util.Map;

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
 * INFO is the only one field that can contains blank character and must be
 * the last field.<br>
 * <br>
 * Transfer arguments:<br>
 * <br>
 * -to <arg>        Specify the requested Host<br>
 * (-id <arg>|      Specify the id of transfer<br>
 * (-file <arg>     Specify the file path to operate on<br>
 * -rule <arg>))    Specify the Rule<br>
 * [-block <arg>]   Specify the block size<br>
 * [-nofollow]      Specify the transfer should not integrate a FOLLOW id<br>
 * [-md5]           Specify the option to have a hash computed for the
 * transfer<br>
 * [-delay <arg>]   Specify the delay time as an epoch time or '+' a delay in ms<br>
 * [-start <arg>]   Specify the start time as yyyyMMddHHmmss<br>
 * [-copyinfo]      Specify to copy the original transfer information in
 * front position back to the new transfer information (eventually in
 * addition an added transfer information from -info option)<br>
 * [-info <arg>)    Specify the transfer information (generally in last position)<br>
 * [-nolog]         Specify to not log anything included database once the
 * transfer is done<br>
 * [-notlogWarn |   Specify to log final result as Info if OK<br>
 * -logWarn]        Specify to log final result as Warn if OK<br>
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
  public TransferTask(final String argRule, final int delay,
                      final String argTransfer, final R66Session session) {
    super(TaskType.TRANSFER, delay, argRule, argTransfer, session);
  }

  @Override
  public void run() {
    logger.info("Transfer with {}}:{} and {}", argRule, argTransfer, session);
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
    final TransferArgs transferArgs =
        TransferArgs.getParamsInternal(0, args, false);
    if (transferArgs != null) {
      String copied = null;
      for (final String arg : args) {
        if ("-copyinfo".equalsIgnoreCase(arg)) {
          copied = argTransfer;
          break;
        }
      }
      // Force to get follow Id if present (and other elements from map) if
      // not already copied
      final DbTaskRunner taskRunner = session.getRunner();
      final String follow = taskRunner.getFollowId();
      if (copied == null && follow != null && !follow.isEmpty() &&
          !transferArgs.getTransferInfo()
                       .contains(TransferArgs.FOLLOW_JSON_KEY)) {
        transferArgs.setFollowId(follow);
        final Map<String, String> map = new HashMap<String, String>();
        map.put(TransferArgs.FOLLOW_JSON_KEY, follow);
        copied = JsonHandler.writeAsStringEscaped(map);
      }
      TransferArgs.getAllInfo(transferArgs, 0, args, copied);
    } else {
      futureCompletion.setFailure(
          new OpenR66RunnerErrorException("Not enough argument in Transfer"));
      return;
    }
    final R66Future future = new R66Future(true);
    final SubmitTransfer transaction =
        new SubmitTransfer(future, transferArgs.getRemoteHost(),
                           transferArgs.getFilename(),
                           transferArgs.getRulename(),
                           transferArgs.getTransferInfo(), transferArgs.isMD5(),
                           transferArgs.getBlockSize(),
                           DbConstantR66.ILLEGALVALUE,
                           transferArgs.getStartTime());
    transaction.run();
    future.awaitOrInterruptible();
    final DbTaskRunner runner;
    if (future.isSuccess()) {
      futureCompletion.setResult(future.getResult());
      runner = future.getResult().getRunner();
      logger.info(
          "Prepare transfer in     SUCCESS     " + runner.toShortString() +
          "     <REMOTE>" + transferArgs.getRemoteHost() + "</REMOTE>");
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
            "     <REMOTE>" + transferArgs.getRemoteHost() + "</REMOTE>",
            future.getCause());
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
