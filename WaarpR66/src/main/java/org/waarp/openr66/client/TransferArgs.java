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
import org.waarp.common.json.JsonHandler;
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
import java.util.Map;

import static org.waarp.common.database.DbConstant.*;

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
  private static final String ID_FIELD = "id";
  public static final String ID_ARG = "-" + ID_FIELD;
  private static final Option ID_OPTION =
      Option.builder(ID_FIELD).required(false).hasArg(true)
            .desc("Specify the id of transfer").build();
  private static final String NO_FOLLOW = "nofollow";
  public static final String NO_FOLLOW_ARG = "-" + NO_FOLLOW;
  private static final Option NO_FOLLOW_OPTION =
      Option.builder(NO_FOLLOW).required(false).hasArg(false)
            .desc("Specify if the transfer should not integrate a FOLLOW id")
            .build();
  public static final String FOLLOW_JSON_KEY = "follow";
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
  private static final OptionGroup DELAY_OPTIONS =
      new OptionGroup().addOption(DELAY_OPTION).addOption(START_OPTION);
  private static final Options TRANSFER_OPTIONS =
      new Options().addOption(FILE_OPTION).addOption(TO_OPTION)
                   .addOption(NO_FOLLOW_OPTION).addOption(RULE_OPTION)
                   .addOption(ID_OPTION).addOption(INFO_OPTION)
                   .addOption(HASH_OPTION).addOption(BLOCK_OPTION)
                   .addOptionGroup(DELAY_OPTIONS).addOption(NOTLOG_OPTION)
                   .addOptionGroup(LOGWARN_OPTIONS);

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
    String[] realArgs = getRealArgs(rank, args);

    // Now set default values from configuration
    TransferArgs transferArgs1 = new TransferArgs();
    transferArgs1.setBlockSize(Configuration.configuration.getBlockSize());

    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine cmd = parser.parse(TRANSFER_OPTIONS, realArgs, true);
      if (getTransferMinimalArgs(transferArgs1, cmd)) {
        return null;
      }
      if (checkDelayStart(transferArgs1, cmd)) {
        return null;
      }
      if (checkExtraTransferArgs(transferArgs1, cmd)) {
        return null;
      }
      checkLog(transferArgs1, cmd);
    } catch (ParseException e) {
      printHelp();
      logger.error("Arguments are incorrect", e);
      return null;
    }
    return finalizeTransferArgs(analyseFollow, transferArgs1);
  }

  /**
   * Check extra arguments for Transfer
   *
   * @param transferArgs1
   * @param cmd
   *
   * @return the TransferArgs or null
   */
  private static boolean checkExtraTransferArgs(
      final TransferArgs transferArgs1, final CommandLine cmd) {
    if (!cmd.hasOption(NO_FOLLOW)) {
      transferArgs1.setFollowId("");
    }
    if (cmd.hasOption(INFO)) {
      transferArgs1.setTransferInfo(cmd.getOptionValue(INFO));
    }
    if (cmd.hasOption(HASH)) {
      transferArgs1.setMD5(true);
    }
    if (cmd.hasOption(BLOCK)) {
      try {
        transferArgs1.setBlockSize(Integer.parseInt(cmd.getOptionValue(BLOCK)));
      } catch (NumberFormatException ignored) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
        logger.error(Messages.getString("AbstractTransfer.20") + " block");
        //$NON-NLS-1$
        return true;
      }
      if (transferArgs1.getBlockSize() < 100) {
        logger.error(Messages.getString("AbstractTransfer.1") +
                     transferArgs1.getBlockSize());
        //$NON-NLS-1$
        return true;
      }
    }
    return false;
  }

  /**
   * Check minimal argyments for Transfer
   *
   * @param transferArgs1
   * @param cmd
   *
   * @return True if an error occurs
   */
  private static boolean getTransferMinimalArgs(
      final TransferArgs transferArgs1, final CommandLine cmd) {
    if (cmd.hasOption(TO)) {
      transferArgs1.setRemoteHost(cmd.getOptionValue(TO));
      if (Configuration.configuration.getAliases().containsKey(
          transferArgs1.getRemoteHost())) {
        transferArgs1.setRemoteHost(Configuration.configuration.getAliases()
                                                               .get(
                                                                   transferArgs1
                                                                       .getRemoteHost()));
      }
    }
    if (cmd.hasOption(FILE)) {
      transferArgs1.setFilename(cmd.getOptionValue(FILE));
      transferArgs1.setFilename(transferArgs1.getFilename().replace('§', '*'));
    }
    if (cmd.hasOption(RULE)) {
      transferArgs1.setRulename(cmd.getOptionValue(RULE));
    }
    if (cmd.hasOption(ID_FIELD)) {
      try {
        transferArgs1.setId(Long.parseLong(cmd.getOptionValue(ID_FIELD)));
      } catch (NumberFormatException ignored) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
        logger.error(Messages.getString("AbstractTransfer.20") + " id");
        //$NON-NLS-1$
        return true;
      }
    }
    return false;
  }

  /**
   * Finalize the real arguments to parse (stopping at "--" and starting at
   * rank)
   *
   * @param rank
   * @param args
   *
   * @return the real arguments
   */
  private static String[] getRealArgs(final int rank, final String[] args) {
    String[] realArgs =
        rank == 0? args : Arrays.copyOfRange(args, rank, args.length);
    for (int i = rank; i < args.length; i++) {
      if (SEPARATOR_SEND.equals(args[i])) {
        realArgs = Arrays.copyOfRange(args, rank, i);
        break;
      }
    }
    return realArgs;
  }

  /**
   * Finalize the TransferArgs
   *
   * @param analyseFollow
   * @param transferArgs1
   *
   * @return the TransferArgs or null
   */
  private static TransferArgs finalizeTransferArgs(final boolean analyseFollow,
                                                   final TransferArgs transferArgs1) {
    if (transferArgs1.getTransferInfo() == null) {
      transferArgs1.setTransferInfo(AbstractTransfer.NO_INFO_ARGS);
    }
    if (analyseFollow) {
      analyzeFollow(transferArgs1);
    }
    if (transferArgs1.getRemoteHost() != null &&
        transferArgs1.getRulename() != null &&
        transferArgs1.getFilename() != null) {
      return transferArgs1;
    } else if (transferArgs1.getId() != ILLEGALVALUE &&
               transferArgs1.getRemoteHost() != null) {
      try {
        final DbTaskRunner runner = new DbTaskRunner(transferArgs1.getId(),
                                                     transferArgs1
                                                         .getRemoteHost());
        transferArgs1.setRulename(runner.getRuleId());
        transferArgs1.setFilename(runner.getOriginalFilename());
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
   * Check LOG and NOT LOG options
   *
   * @param transferArgs1
   * @param cmd
   */
  private static void checkLog(final TransferArgs transferArgs1,
                               final CommandLine cmd) {
    if (cmd.hasOption(LOGWARN)) {
      transferArgs1.setNormalInfoAsWarn(true);
    }
    if (cmd.hasOption(NOTLOGWARN)) {
      transferArgs1.setNormalInfoAsWarn(false);
    }
    if (cmd.hasOption(NOTLOG)) {
      transferArgs1.setNolog(true);
    }
  }

  /**
   * Check DELAY or START
   *
   * @param transferArgs1
   * @param cmd
   *
   * @return True if an error occurs
   */
  private static boolean checkDelayStart(final TransferArgs transferArgs1,
                                         final CommandLine cmd) {
    if (cmd.hasOption(START)) {
      Date date;
      final SimpleDateFormat dateFormat =
          new SimpleDateFormat("yyyyMMddHHmmss");
      try {
        date = dateFormat.parse(cmd.getOptionValue(START));
        transferArgs1.setStartTime(new Timestamp(date.getTime()));
      } catch (java.text.ParseException ignored) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
        logger.error(Messages.getString("AbstractTransfer.20") + " StartTime");
        //$NON-NLS-1$
        return true;
      }
    }
    if (cmd.hasOption(DELAY)) {
      String delay = cmd.getOptionValue(DELAY);
      try {
        if (delay.charAt(0) == '+') {
          transferArgs1.setStartTime(new Timestamp(
              System.currentTimeMillis() + Long.parseLong(delay.substring(1))));
        } else {
          transferArgs1.setStartTime(new Timestamp(Long.parseLong(delay)));
        }
      } catch (NumberFormatException ignored) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
        logger.error(Messages.getString("AbstractTransfer.20") + " Delay");
        //$NON-NLS-1$
        return true;
      }
    }
    return false;
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
            builder.append(args[i].trim());
          } else {
            builder.append(' ').append(args[i].trim());
          }
          i++;
          while (i < args.length) {
            builder.append(' ').append(args[i].trim());
            i++;
          }
        }
      }
      transferArgs.setTransferInfo(builder.toString());
      TransferArgs.analyzeFollow(transferArgs);
    }
  }

  /**
   * Analyze Follow option
   *
   * @param transferArgs1 the current TransferArgs
   */
  public static void analyzeFollow(final TransferArgs transferArgs1) {
    if (transferArgs1.getFollowId() != null &&
        transferArgs1.getTransferInfo() != null) {
      Map<String, Object> map =
          DbTaskRunner.getMapFromString(transferArgs1.getTransferInfo());
      if (map.containsKey(FOLLOW_JSON_KEY)) {
        transferArgs1.setFollowId(map.get(FOLLOW_JSON_KEY).toString());
      }
      if (transferArgs1.getFollowId().isEmpty()) {
        LongUuid longUuid = new LongUuid();
        map.put(FOLLOW_JSON_KEY, longUuid.getLong());
        String originalWithoutMap =
            DbTaskRunner.getOutOfMapFromString(transferArgs1.getTransferInfo());
        transferArgs1.setTransferInfo(
            originalWithoutMap.trim() + " " + JsonHandler.writeAsString(map));
        transferArgs1.setFollowId("" + longUuid.getLong());
      } else {
        String originalWithoutMap =
            DbTaskRunner.getOutOfMapFromString(transferArgs1.getTransferInfo());
        transferArgs1.setTransferInfo(
            originalWithoutMap.trim() + " " + JsonHandler.writeAsString(map));
      }
    }
  }

  private String filename;
  private String rulename;
  private String transferInfo;
  private boolean isMD5;
  private String remoteHost;
  private int blocksize = 0x10000; // 64K
  private long id = ILLEGALVALUE;
  private Timestamp startTime;
  private String followId;
  private boolean normalInfoAsWarn = true;
  private boolean nolog = false;

  /**
   * Empty constructor
   */
  public TransferArgs() {
    // Empty
  }

  public String getFilename() {
    return filename;
  }

  public TransferArgs setFilename(String filename) {
    this.filename = filename;
    return this;
  }

  public String getRulename() {
    return rulename;
  }

  public TransferArgs setRulename(String rulename) {
    this.rulename = rulename;
    return this;
  }

  public String getTransferInfo() {
    return transferInfo;
  }

  public TransferArgs setTransferInfo(String transferInfo) {
    this.transferInfo = transferInfo;
    return this;
  }

  public boolean isMD5() {
    return isMD5;
  }

  public TransferArgs setMD5(boolean md5) {
    isMD5 = md5;
    return this;
  }

  public String getRemoteHost() {
    return remoteHost;
  }

  public TransferArgs setRemoteHost(String remoteHost) {
    this.remoteHost = remoteHost;
    return this;
  }

  public int getBlockSize() {
    return blocksize;
  }

  public TransferArgs setBlockSize(int blocksize) {
    this.blocksize = blocksize;
    return this;
  }

  public long getId() {
    return id;
  }

  public TransferArgs setId(long id) {
    this.id = id;
    return this;
  }

  public Timestamp getStartTime() {
    return startTime;
  }

  public TransferArgs setStartTime(Timestamp startTime) {
    this.startTime = startTime;
    return this;
  }

  public String getFollowId() {
    return followId;
  }

  public TransferArgs setFollowId(String followId) {
    this.followId = followId;
    return this;
  }

  public boolean isNormalInfoAsWarn() {
    return normalInfoAsWarn;
  }

  public TransferArgs setNormalInfoAsWarn(boolean normalInfoAsWarn) {
    this.normalInfoAsWarn = normalInfoAsWarn;
    return this;
  }

  public boolean isNolog() {
    return nolog;
  }

  public TransferArgs setNolog(boolean nolog) {
    this.nolog = nolog;
    return this;
  }
}
