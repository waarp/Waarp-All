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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.guid.LongUuid;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import static org.waarp.common.database.DbConstant.*;

/**
 * Transfer arguments:<br>
 * <br>
 * -to <arg>        Specify the requested Host<br>
 * (-id <arg>|      Specify the id of transfer<br>
 * (-file <arg>     Specify the file path to operate on<br>
 * -rule <arg>))    Specify the Rule<br>
 * [-block <arg>]   Specify the block size<br>
 * [-follow]        Specify the transfer should integrate a FOLLOW id<br>
 * [-md5]           Specify the option to have a hash computed for the
 * transfer<br>
 * [-delay <arg>]   Specify the delay time as an epoch time or '+' a delay in ms<br>
 * [-start <arg>]   Specify the start time as yyyyMMddHHmmss<br>
 * [-info <arg>)    Specify the transfer information (generally in last position)<br>
 * [-nolog]         Specify to not log anything included database once the
 * transfer is done<br>
 * [-notlogWarn |   Specify to log final result as Info if OK<br>
 * -logWarn]        Specify to log final result as Warn if OK<br>
 */
public class TransferArgs {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(TransferArgs.class);

  private static final String FILE = "file";
  public static final String FILE_ARG = "-" + FILE;
  private static final Option FILE_OPTION =
      Option.builder(FILE).required(false).hasArg(true)
            .desc("Specify the file path to operate on").build();
  private static final String TO = "to";
  public static final String TO_ARG = "-" + TO;
  private static final Option TO_OPTION =
      Option.builder(TO).required(true).hasArg(true)
            .desc("Specify the requested Host").build();
  private static final String RULE = "rule";
  public static final String RULE_ARG = "-" + RULE;
  private static final Option RULE_OPTION =
      Option.builder(RULE).required(false).hasArg(true).desc("Specify the Rule")
            .build();
  private static final String ID = "id";
  public static final String ID_ARG = "-" + ID;
  private static final Option ID_OPTION =
      Option.builder(ID).required(false).hasArg(true)
            .desc("Specify the id of transfer").build();
  private static final String FOLLOW = "follow";
  public static final String FOLLOW_ARG = "-" + FOLLOW;
  private static final Option FOLLOW_OPTION =
      Option.builder(FOLLOW).required(false).hasArg(false)
            .desc("Specify the transfer should integrate a FOLLOW id").build();
  public static final String FOLLOWARGJSON = "{'follow':";
  private static final String INFO = "info";
  public static final String INFO_ARG = "-" + INFO;
  private static final Option INFO_OPTION =
      Option.builder(INFO).required(false).hasArg(true)
            .desc("Specify the transfer information").build();
  private static final String HASH = "md5";
  public static final String HASH_ARG = "-" + HASH;
  private static final Option HASH_OPTION =
      Option.builder(HASH).required(false).hasArg(false)
            .desc("Specify the option to have a hash computed for the transfer")
            .build();
  private static final String BLOCK = "block";
  public static final String BLOCK_ARG = "-" + BLOCK;
  private static final Option BLOCK_OPTION =
      Option.builder(BLOCK).required(false).hasArg(true)
            .desc("Specify the block size").build();
  private static final String START = "start";
  public static final String START_ARG = "-" + START;
  private static final Option START_OPTION =
      Option.builder(START).required(false).hasArg(true)
            .desc("Specify the start time as yyyyMMddHHmmss").build();
  private static final String DELAY = "delay";
  public static final String DELAY_ARG = "-" + DELAY;
  private static final Option DELAY_OPTION =
      Option.builder(DELAY).required(false).hasArg(true).desc(
          "Specify the delay time as an epoch time or '+' a delay in ms")
            .build();

  private static final String LOGWARN = "logWarn";
  public static final String LOGWARN_ARG = "-" + LOGWARN;
  private static final Option LOGWARN_OPTION =
      Option.builder(LOGWARN).required(false).hasArg(false)
            .desc("Specify to log final result as Warn if OK").build();
  private static final String NOTLOGWARN = "notlogWarn";
  public static final String NOTLOGWARN_ARG = "-" + NOTLOGWARN;
  private static final Option NOTLOGWARN_OPTION =
      Option.builder(NOTLOGWARN).required(false).hasArg(false)
            .desc("Specify to log final result as Info if OK").build();

  private static final String NOTLOG = "nolog";
  public static final String NOTLOG_ARG = "-" + NOTLOG;
  private static final Option NOTLOG_OPTION =
      Option.builder(NOTLOG).required(false).hasArg(false).desc(
          "Specify to not log anything included database once the " +
          "transfer is done").build();

  private static final OptionGroup LOGWARN_OPTIONS =
      new OptionGroup().addOption(LOGWARN_OPTION).addOption(NOTLOGWARN_OPTION);
  private static final Options TRANSFER_OPTIONS =
      new Options().addOption(FILE_OPTION).addOption(TO_OPTION)
                   .addOption(FOLLOW_OPTION).addOption(RULE_OPTION)
                   .addOption(ID_OPTION).addOption(INFO_OPTION)
                   .addOption(HASH_OPTION).addOption(BLOCK_OPTION)
                   .addOption(START_OPTION).addOption(DELAY_OPTION)
                   .addOption(NOTLOG_OPTION).addOptionGroup(LOGWARN_OPTIONS);

  public static final String SEPARATOR_SEND = "--";


  /**
   * Print to standard output the help of this command
   */
  public static void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Transfer", TRANSFER_OPTIONS);
  }

  /**
   * Analyze the parameters according to TransferArgs options
   * <br><br>
   * Transfer arguments:<br>
   * <br>
   * -to <arg>        Specify the requested Host<br>
   * (-id <arg>|      Specify the id of transfer<br>
   * (-file <arg>     Specify the file path to operate on<br>
   * -rule <arg>))    Specify the Rule<br>
   * [-block <arg>]   Specify the block size<br>
   * [-follow]        Specify the transfer should integrate a FOLLOW id<br>
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
   * @param rank the rank to analyze from
   * @param args the argument to analyze
   * @param analyseFollow if True, will check follow possible argument in info
   *
   * @return the TransferArgs or null if an error occurs
   */
  public static TransferArgs getParamsInternal(final int rank,
                                               final String[] args,
                                               boolean analyseFollow) {
    if (args == null || args.length == 0) {
      logger.error("Arguments cannot be empty or null");
      return null;
    }
    String[] realArgs =
        rank == 0? args : Arrays.copyOfRange(args, rank, args.length);
    for (int i = rank; i < args.length; i++) {
      if (SEPARATOR_SEND.equals(args[i])) {
        realArgs = Arrays.copyOfRange(args, rank, i);
        break;
      }
    }

    // Now set default values from configuration
    TransferArgs transferArgs1 = new TransferArgs();
    transferArgs1.blocksize = Configuration.configuration.getBlockSize();

    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine cmd = parser.parse(TRANSFER_OPTIONS, realArgs, true);
      if (cmd.hasOption(TO)) {
        transferArgs1.remoteHost = cmd.getOptionValue(TO);
        if (Configuration.configuration.getAliases()
                                       .containsKey(transferArgs1.remoteHost)) {
          transferArgs1.remoteHost = Configuration.configuration.getAliases()
                                                                .get(
                                                                    transferArgs1.remoteHost);
        }
      }
      if (cmd.hasOption(FILE)) {
        transferArgs1.filename = cmd.getOptionValue(FILE);
        transferArgs1.filename = transferArgs1.filename.replace('§', '*');
      }
      if (cmd.hasOption(RULE)) {
        transferArgs1.rulename = cmd.getOptionValue(RULE);
      }
      if (cmd.hasOption(ID)) {
        try {
          transferArgs1.id = Long.parseLong(cmd.getOptionValue(ID));
        } catch (NumberFormatException ignored) {
          SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
          logger.error(Messages.getString("AbstractTransfer.20") + " id");
          //$NON-NLS-1$
          return null;
        }
      }
      if (cmd.hasOption(START)) {
        Date date;
        final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyyMMddHHmmss");
        try {
          date = dateFormat.parse(cmd.getOptionValue(START));
          transferArgs1.startTime = new Timestamp(date.getTime());
        } catch (java.text.ParseException ignored) {
          SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
          logger
              .error(Messages.getString("AbstractTransfer.20") + " StartTime");
          //$NON-NLS-1$
          return null;
        }
      }
      if (cmd.hasOption(DELAY)) {
        String delay = cmd.getOptionValue(DELAY);
        try {
          if (delay.charAt(0) == '+') {
            transferArgs1.startTime = new Timestamp(System.currentTimeMillis() +
                                                    Long.parseLong(
                                                        delay.substring(1)));
          } else {
            transferArgs1.startTime = new Timestamp(Long.parseLong(delay));
          }
        } catch (NumberFormatException ignored) {
          SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
          logger.error(Messages.getString("AbstractTransfer.20") + " Delay");
          //$NON-NLS-1$
          return null;
        }
      }
      if (cmd.hasOption(FOLLOW)) {
        transferArgs1.follow = "";
      }
      if (cmd.hasOption(INFO)) {
        transferArgs1.fileinfo = cmd.getOptionValue(INFO);
      }
      if (cmd.hasOption(HASH)) {
        transferArgs1.isMD5 = true;
      }
      if (cmd.hasOption(BLOCK)) {
        try {
          transferArgs1.blocksize = Integer.parseInt(cmd.getOptionValue(BLOCK));
        } catch (NumberFormatException ignored) {
          SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
          logger.error(Messages.getString("AbstractTransfer.20") + " block");
          //$NON-NLS-1$
          return null;
        }
        if (transferArgs1.blocksize < 100) {
          logger.error(Messages.getString("AbstractTransfer.1") +
                       transferArgs1.blocksize);
          //$NON-NLS-1$
          return null;
        }
      }
      if (cmd.hasOption(LOGWARN)) {
        transferArgs1.normalInfoAsWarn = true;
      }
      if (cmd.hasOption(NOTLOGWARN)) {
        transferArgs1.normalInfoAsWarn = false;
      }
      if (cmd.hasOption(NOTLOG)) {
        transferArgs1.nolog = true;
      }

    } catch (ParseException e) {
      printHelp();
      logger.error("Arguments are incorrect", e);
      return null;
    }
    if (transferArgs1.fileinfo == null) {
      transferArgs1.fileinfo = AbstractTransfer.NO_INFO_ARGS;
    }
    if (analyseFollow) {
      analyzeFollow(transferArgs1);
    }
    if (transferArgs1.remoteHost != null && transferArgs1.rulename != null &&
        transferArgs1.filename != null) {
      return transferArgs1;
    } else if (transferArgs1.id != ILLEGALVALUE &&
               transferArgs1.remoteHost != null) {
      try {
        final DbTaskRunner runner =
            new DbTaskRunner(transferArgs1.id, transferArgs1.remoteHost);
        transferArgs1.rulename = runner.getRuleId();
        transferArgs1.filename = runner.getOriginalFilename();
        return transferArgs1;
      } catch (final WaarpDatabaseNoDataException e) {
        logger.error("No transfer found with this id and partner");
        logger.error(Messages.getString("AbstractBusinessRequest.NeedMoreArgs",
                                        "(-to -rule -file | -to -id with " +
                                        "existing id transfer)")
                     //$NON-NLS-1$
            , e);
        return null;
      } catch (final WaarpDatabaseException e) {
        logger.error(Messages.getString("AbstractBusinessRequest.NeedMoreArgs",
                                        "(-to -rule -file | -to -id) with a " +
                                        "correct database connexion")
                     //$NON-NLS-1$
            , e);
        return null;
      }
    }
    logger.error(Messages.getString("AbstractBusinessRequest.NeedMoreArgs",
                                    "(-to -rule -file | -to -id)") +
                 //$NON-NLS-1$
                 AbstractTransfer._INFO_ARGS);
    return null;
  }

  /**
   * Get all Info in case of last argument
   *
   * @param transferArgs the original TransferArgs
   * @param rank the rank to start from in args array
   * @param args the original arguments
   * @param copiedInfo might be null, original information to copy
   */
  public static void getAllInfo(final TransferArgs transferArgs, final int rank,
                                final String[] args, final String copiedInfo) {
    if (transferArgs != null) {
      StringBuilder builder = new StringBuilder();
      if (copiedInfo != null) {
        builder.append(copiedInfo);
      }
      for (int i = rank; i < args.length; i++) {
        if (INFO_ARG.equalsIgnoreCase(args[i])) {
          i++;
          if (builder.length() == 0) {
            builder.append(args[i]);
          } else {
            builder.append(' ').append(args[i]);
          }
          i++;
          while (i < args.length) {
            builder.append(' ').append(args[i]);
            i++;
          }
        }
      }
      transferArgs.fileinfo = builder.toString();
      TransferArgs.analyzeFollow(transferArgs);
    }
  }

  /**
   * Analyze Follow option
   *
   * @param transferArgs1 the current TransferArgs
   */
  public static void analyzeFollow(final TransferArgs transferArgs1) {
    if (transferArgs1.follow != null && transferArgs1.fileinfo != null) {
      String[] split = transferArgs1.fileinfo.split(" ");
      for (int i = 0; i < split.length; i++) {
        if (FOLLOWARGJSON.equalsIgnoreCase(split[i])) {
          i++;
          transferArgs1.follow = split[i].replace("}", "").trim();
          break;
        }
      }
      if (transferArgs1.follow.isEmpty()) {
        LongUuid longUuid = new LongUuid();
        transferArgs1.follow = "" + longUuid.getLong();
        transferArgs1.fileinfo +=
            " " + FOLLOWARGJSON + " " + longUuid.getLong() + "}";
      }
    }
  }

  public String filename;
  public String rulename;
  public String fileinfo;
  public boolean isMD5;
  public String remoteHost;
  public int blocksize = 0x10000; // 64K
  public long id = ILLEGALVALUE;
  public Timestamp startTime;
  public String follow;
  public boolean normalInfoAsWarn = true;
  public boolean nolog = false;
}
