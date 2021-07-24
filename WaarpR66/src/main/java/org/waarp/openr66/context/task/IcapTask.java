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
import org.waarp.icap.IcapScanFile;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;

/**
 * ICAP task:<br>
 * <p>
 * List of arguments will be as follow.<br>
 * -file filename <br>
 * -to hostname <br>
 * [-port port, default 1344] <br>
 * -service name | -model name <br>
 * [-previewSize size, default none] <br>
 * [-blockSize size, default 8192] <br>
 * [-receiveSize size, default 65536] <br>
 * [-maxSize size, default MAX_INTEGER] <br>
 * [-timeout in_ms, default equiv to 10 min] <br>
 * [-keyPreview key -stringPreview string, default none] <br>
 * [-key204 key -string204 string, default none] <br>
 * [-key200 key -string200 string, default none] <br>
 * [-stringHttp string, default none] <br>
 * [-logger DEBUG|INFO|WARN|ERROR, default none] <br>
 * [-errorMove path | -errorDelete | -sendOnError] <br>
 * [-ignoreNetworkError] <br>
 * [-ignoreTooBigFileError] <br>
 * <br>
 * Then if r66send command in case of -sendOnError option is specified, the
 * very last option of ICAP must be "--" then followed by usual r66send
 * options.<br>
 * Format is like r66send command in any order except "-info" which should be
 * the last item, and "-copyinfo"
 * will copy at first place the original transfer information as the new one,
 * while still having the
 * possibility to add new informations through "-info":<br>
 * "-file filepath -to requestedHost -rule rule [-md5] [-start yyyyMMddHHmmss or
 * -delay (delay or +delay)] [-nofollow)
 * [-copyinfo] [-info information]" <br>
 * <br>
 * INFO is the only one field that can contains blank character.<br>
 * <br>
 * Example:<br>
 * -file #TRUEFULLPATH# -to hostname -service name -previewSize size
 * -blockSize size -receiveSize size -maxSize size -timeout in_ms -keyPreview
 * key -stringPreview string -key204 key -string204 string -key200 key
 * -string200 string -stringHttp string -logger WARN -errorDelete
 * -ignoreNetworkError<br>
 * <br>
 * -file #TRUEFULLPATH# -to hostname -model name -previewSize size
 * -blockSize size -receiveSize size -maxSize size -timeout in_ms -keyPreview
 * key -stringPreview string -key204 key -string204 string -key200 key
 * -string200 string -stringHttp string -logger WARN -errorMove path
 * -ignoreNetworkError<br>
 * <br>
 * -file #TRUEFULLPATH# -to hostname -model name -previewSize size
 * -blockSize size -receiveSize size -maxSize size -timeout in_ms -keyPreview
 * key -stringPreview string -key204 key -string204 string -key200 key
 * -string200 string -stringHttp string -logger WARN -sendOnError
 * -ignoreNetworkError -- -file #TRUEFULLPATH# -to
 * requestedHost -rule rule [-copyinfo] [-info information]<br>
 */
public class IcapTask extends AbstractTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(IcapTask.class);

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public IcapTask(final String argRule, final int delay,
                  final String argTransfer, final R66Session session) {
    super(TaskType.ICAP, delay, argRule, argTransfer, session);
  }

  @Override
  public final void run() {
    logger.info("ICAP with " + argRule + ':' + argTransfer + " and {}",
                session);
    String finalname = applyTransferSubstitutions(argRule);
    finalname = finalname.replaceAll("#([A-Z]+)#", "\\${$1}");
    final CommandLine commandLine = new CommandLine("dummy");
    commandLine.setSubstitutionMap(getSubstitutionMap());
    commandLine.addArguments(finalname, false);
    final String[] args = commandLine.getArguments();

    if (args.length < 6) {
      futureCompletion.setFailure(
          new OpenR66RunnerErrorException("Not enough argument in ICAP"));
      return;
    }
    final int status = IcapScanFile.scanFile(args);
    switch (status) {
      case IcapScanFile.STATUS_OK:
        futureCompletion.setSuccess();
        return;
      case IcapScanFile.STATUS_BAD_ARGUMENT:
        logger.error(
            "ICAP Bad argument error with " + argRule + ':' + argTransfer +
            ':' + delay + " and " + session);
        futureCompletion.setFailure(
            new OpenR66RunnerErrorException("ICAP Bad argument error"));
        return;
      case IcapScanFile.STATUS_ICAP_ISSUE:
        logger.error(
            "ICAP Bad protocol error with " + argRule + ':' + argTransfer +
            ':' + delay + " and " + session);
        futureCompletion.setFailure(
            new OpenR66RunnerErrorException("ICAP Bad protocol error"));
        return;
      case IcapScanFile.STATUS_NETWORK_ISSUE:
        logger.error(
            "ICAP Network error with " + argRule + ':' + argTransfer + ':' +
            delay + " and " + session);
        futureCompletion.setFailure(
            new OpenR66RunnerErrorException("ICAP Network error"));
        return;
      case IcapScanFile.STATUS_KO_SCAN:
        if (finalizeIcapOnError(args)) {
          return;
        }
        // No send required so real error
        logger.error(
            "ICAP KO error with " + argRule + ':' + argTransfer + ':' + delay +
            " and " + session);
        futureCompletion.setFailure(
            new OpenR66RunnerErrorException("ICAP KO error"));
        return;
      case IcapScanFile.STATUS_KO_SCAN_POST_ACTION_ERROR:
        logger.error(
            "ICAP KO post error with " + argRule + ':' + argTransfer + ':' +
            delay + " and " + session);
        futureCompletion.setFailure(
            new OpenR66RunnerErrorException("ICAP KO post error"));
        return;
      default:
        logger.error(
            "ICAP Unknown error with " + argRule + ':' + argTransfer + ':' +
            delay + " and " + session);
        futureCompletion.setFailure(
            new OpenR66RunnerErrorException("ICAP Unknown error"));
    }
  }

  /**
   * finalize Icap on Error
   *
   * @param args
   *
   * @return True if OK
   */
  private boolean finalizeIcapOnError(final String[] args) {
    for (int i = 0; i < args.length; i++) {
      if (IcapScanFile.ERROR_SEND_ARG.equalsIgnoreCase(args[i])) {
        for (i++; i < args.length; i++) {
          if (IcapScanFile.SEPARATOR_SEND.equals(args[i])) {
            final StringBuilder newArgs = new StringBuilder();
            for (i++; i < args.length; i++) {
              newArgs.append(args[i]).append(' ');
            }
            // Now launch send
            final TransferTask transferTask =
                new TransferTask(newArgs.toString(), 0, argTransfer, session);
            transferTask.run();
            transferTask.futureCompletion.awaitOrInterruptible();
            if (transferTask.futureCompletion.isSuccess()) {
              futureCompletion.setResult(
                  transferTask.futureCompletion.getResult());
              logger.info(
                  "ICAP ended in KO on file but resend as requested is OK for {}",
                  session.getFile().getTrueFile().getAbsolutePath());
              futureCompletion.setFailure(new OpenR66RunnerErrorException(
                  "ICAP ended in KO on file but resend as requested is OK"));
            } else {
              logger.error(
                  "ICAP KO with Resend in error with " + argRule + ':' +
                  argTransfer + ':' + delay + " and " + session,
                  transferTask.futureCompletion.getCause());
              if (transferTask.futureCompletion.getCause() == null) {
                futureCompletion.setFailure(new OpenR66RunnerErrorException(
                    "ICAP ended in KO on file and Resend is KO too"));
              } else {
                futureCompletion.setFailure(new OpenR66RunnerErrorException(
                    "ICAP ended in KO on file and Resend is KO too",
                    transferTask.futureCompletion.getCause()));
              }
            }
            return true;
          }
        }
      }
    }
    return false;
  }

}
