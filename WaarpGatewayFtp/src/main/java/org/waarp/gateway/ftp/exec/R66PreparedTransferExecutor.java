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


package org.waarp.gateway.ftp.exec;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply421Exception;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.client.TransferArgs;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;

import java.io.File;

import static org.waarp.common.database.DbConstant.*;
import static org.waarp.common.file.filesystembased.FilesystemBasedFileImpl.*;

/**
 * R66PreparedTransferExecutor class. If the command starts with "REFUSED", the
 * command will be refused for
 * execution. If "REFUSED" is set, the command "RETR" or "STOR" like operations
 * will be stopped at starting of
 * command.
 * <p>
 * <p>
 * <p>
 * Format is like r66send command in any order except "-info" which should be
 * the last item:<br>
 * "-to Host -file FILE -rule RULE [-md5] [-nolog] [-start yyyyMMddHHmmss or
 * -delay (delay or +delay)] [-info INFO]" <br>
 * <br>
 * INFO is the only one field that can contains blank character and MUST be
 * the last argument.<br>
 * <br>
 * The following replacement are done dynamically before the command is
 * executed:<br>
 * - #BASEPATH# is replaced by the full path for the root of FTP Directory<br>
 * - #FILE# is replaced by the current file path relative to FTP Directory (so
 * #BASEPATH##FILE# is the full
 * path of the file)<br>
 * - #USER# is replaced by the username<br>
 * - #ACCOUNT# is replaced by the account<br>
 * - #COMMAND# is replaced by the command issued for the file<br>
 * - #SPECIALID# is replaced by the FTP id of the transfer (whatever in or
 * out)<br>
 * - #UUID# is replaced by a special UUID globally unique for the transfer, in
 * general to be placed in -info
 * part (for instance ##UUID## giving #uuid#)<br>
 * <br>
 * So for instance "-to Host -file #BASEPATH##FILE# -rule RULE [-md5] [-nolog]
 * [-delay +delay] [-info ##UUID##
 * #USER# #ACCOUNT# #COMMAND# INFO]" <br>
 * will be a standard use of this function.
 */
public class R66PreparedTransferExecutor extends AbstractExecutor {
  private static final String CANNOT_GET_NEW_TASK = "Cannot get new task\n    ";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(R66PreparedTransferExecutor.class);

  protected final WaarpFuture future;
  protected final TransferArgs transferArgs;

  protected boolean nolog;

  /**
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
   * [-info <arg>)    Specify the transfer information (generally in last position)<br>
   * [-nolog]         Specify to not log anything included database once the
   * transfer is done<br>
   * [-notlogWarn |   Specify to log final result as Info if OK<br>
   * -logWarn]        Specify to log final result as Warn if OK<br>
   *
   * @param command transfer arguments
   * @param delay delay
   * @param futureCompletion future for completion
   */
  public R66PreparedTransferExecutor(final String command, final long delay,
                                     final WaarpFuture futureCompletion) {
    final String[] args = BLANK.split(command);
    transferArgs = TransferArgs.getParamsInternal(0, args, false);
    if (transferArgs != null) {
      TransferArgs.getAllInfo(transferArgs, 0, args, null);
      nolog = transferArgs.isNolog();
    }
    future = futureCompletion;
  }

  @Override
  public void run() throws CommandAbstractException {
    if (transferArgs == null) {
      logger
          .error("Mandatory argument is missing: -to  -rule  -file or -to -id");
      throw new Reply421Exception(
          "Mandatory argument is missing: -to  -rule  -file or -to -id");
    }
    if (transferArgs.getRemoteHost() == null ||
        transferArgs.getRulename() == null ||
        transferArgs.getFilename() == null) {
      logger.error(
          "Mandatory argument is missing: -to " + transferArgs.getRemoteHost() +
          " -rule " + transferArgs.getRulename() + " -file " +
          transferArgs.getFilename());
      throw new Reply421Exception("Mandatory argument is missing");
    }
    final String message =
        "R66Prepared with -to " + transferArgs.getRemoteHost() + " -rule " +
        transferArgs.getRulename() + " -file " + transferArgs.getFilename() +
        " -nolog: " + nolog + " -isMD5: " + transferArgs.isMD5() + " -info " +
        transferArgs.getTransferInfo();
    logger.debug(message);
    final DbRule rule;
    try {
      rule = new DbRule(transferArgs.getRulename());
    } catch (final WaarpDatabaseException e) {
      logger.error(
          "Cannot get Rule: " + transferArgs.getRulename() + " since {}\n    " +
          message, e.getMessage());
      throw new Reply421Exception(
          "Cannot get Rule: " + transferArgs.getRulename() + "\n    " +
          message);
    }
    int mode = rule.getMode();
    if (transferArgs.isMD5()) {
      mode = RequestPacket.getModeMD5(mode);
    }
    final String sep =
        PartnerConfiguration.getSeparator(transferArgs.getRemoteHost());
    long originalSize = -1;
    if (RequestPacket.isSendMode(mode) && !RequestPacket.isThroughMode(mode)) {
      final File file = new File(transferArgs.getFilename());
      if (canRead(file)) {
        originalSize = file.length();
      }
    }
    final RequestPacket request =
        new RequestPacket(transferArgs.getRulename(), mode,
                          transferArgs.getFilename(),
                          transferArgs.getBlockSize(), 0, ILLEGALVALUE,
                          transferArgs.getTransferInfo(), originalSize, sep);
    // Not isRecv since it is the requester, so send => isRetrieve is true
    final boolean isRetrieve = !RequestPacket.isRecvMode(request.getMode());
    logger.debug("Will prepare: {}", request);
    final DbTaskRunner taskRunner;
    try {
      taskRunner = new DbTaskRunner(rule, isRetrieve, request,
                                    transferArgs.getRemoteHost(),
                                    transferArgs.getStartTime());
    } catch (final WaarpDatabaseException e) {
      logger.error("Cannot get new task since {}\n    " + message,
                   e.getMessage());
      throw new Reply421Exception(CANNOT_GET_NEW_TASK + message);
    }
    taskRunner.changeUpdatedInfo(UpdatedInfo.TOSUBMIT);
    if (!taskRunner.forceSaveStatus()) {
      try {
        if (!taskRunner.specialSubmit()) {
          logger.error("Cannot prepare task: " + message);
          throw new Reply421Exception(CANNOT_GET_NEW_TASK + message);
        }
      } catch (final WaarpDatabaseException e) {
        logger.error("Cannot prepare task since {}\n    " + message,
                     e.getMessage());
        throw new Reply421Exception(CANNOT_GET_NEW_TASK + message);
      }
    }
    logger.debug("R66PreparedTransfer prepared: {}", request);
    future.setSuccess();
  }
}
