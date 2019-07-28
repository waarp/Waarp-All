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


package org.waarp.gateway.kernel.exec;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply421Exception;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;

import java.io.File;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.waarp.common.database.DbConstant.*;

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
 * -delay (delay or +delay)] [-info
 * INFO]" <br>
 * <br>
 * INFO is the only one field that can contains blank character.<br>
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

  protected String filename;

  protected String rulename;

  protected String fileinfo;

  protected boolean isMD5;

  protected boolean nolog;

  protected Timestamp timestart;

  protected String remoteHost;

  protected int blocksize = Configuration.configuration.getBlockSize();

  protected DbSession dbsession;

  /**
   * @param command
   * @param delay
   * @param futureCompletion
   */
  public R66PreparedTransferExecutor(String command, long delay,
                                     WaarpFuture futureCompletion) {
    final String[] args = BLANK.split(command);
    for (int i = 0; i < args.length; i++) {
      if ("-to".equalsIgnoreCase(args[i])) {
        i++;
        remoteHost = args[i];
        if (Configuration.configuration.getAliases().containsKey(remoteHost)) {
          remoteHost = Configuration.configuration.getAliases().get(remoteHost);
        }
      } else if ("-file".equalsIgnoreCase(args[i])) {
        i++;
        filename = args[i];
      } else if ("-rule".equalsIgnoreCase(args[i])) {
        i++;
        rulename = args[i];
      } else if ("-info".equalsIgnoreCase(args[i])) {
        i++;
        fileinfo = args[i];
        i++;
        while (i < args.length) {
          fileinfo += ' ' + args[i];
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
      } else if ("-nolog".equalsIgnoreCase(args[i])) {
        nolog = true;
        i++;
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
        if (args[i].charAt(0) == '+') {
          timestart = new Timestamp(System.currentTimeMillis() +
                                    Long.parseLong(args[i].substring(1)));
        } else {
          timestart = new Timestamp(Long.parseLong(args[i]));
        }
      }
    }
    if (fileinfo == null) {
      fileinfo = "noinfo";
    }
    future = futureCompletion;
  }

  /**
   * @param dbsession the dbsession to set
   */
  public void setDbsession(DbSession dbsession) {
    this.dbsession = dbsession;
  }

  @Override
  public void run() throws CommandAbstractException {
    final String message =
        "R66Prepared with -to " + remoteHost + " -rule " + rulename +
        " -file " + filename + " -nolog: " + nolog + " -isMD5: " + isMD5 +
        " -info " + fileinfo;
    if (remoteHost == null || rulename == null || filename == null) {
      logger.error(
          "Mandatory argument is missing: -to " + remoteHost + " -rule " +
          rulename + " -file " + filename);
      throw new Reply421Exception(
          "Mandatory argument is missing\n    " + message);
    }
    logger.debug(message);
    DbRule rule;
    try {
      rule = new DbRule(rulename);
    } catch (final WaarpDatabaseException e) {
      logger.error("Cannot get Rule: " + rulename + " since {}\n    " + message,
                   e.getMessage());
      throw new Reply421Exception(
          "Cannot get Rule: " + rulename + "\n    " + message);
    }
    int mode = rule.getMode();
    if (isMD5) {
      mode = RequestPacket.getModeMD5(mode);
    }
    final String sep = PartnerConfiguration.getSeparator(remoteHost);
    long originalSize = -1;
    if (RequestPacket.isSendMode(mode) && !RequestPacket.isThroughMode(mode)) {
      final File file = new File(filename);
      if (file.canRead()) {
        originalSize = file.length();
      }
    }
    final RequestPacket request =
        new RequestPacket(rulename, mode, filename, blocksize, 0, ILLEGALVALUE,
                          fileinfo, originalSize, sep);
    // Not isRecv since it is the requester, so send => isRetrieve is true
    final boolean isRetrieve = !RequestPacket.isRecvMode(request.getMode());
    logger.debug("Will prepare: {}", request);
    DbTaskRunner taskRunner;
    try {
      taskRunner =
          new DbTaskRunner(rule, isRetrieve, request, remoteHost, timestart);
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
