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

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.client.utils.OutputFormat;
import org.waarp.openr66.client.utils.OutputFormat.FIELDS;
import org.waarp.openr66.commander.ClientRunner;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.filesystem.R66Dir;
import org.waarp.openr66.context.filesystem.R66File;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.exception.OpenR66DatabaseGlobalException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.InformationPacket.ASKENUM;
import org.waarp.openr66.protocol.localhandler.packet.RequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.FileUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import java.io.File;
import java.net.SocketAddress;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.waarp.common.database.DbConstant.*;

/**
 * Abstract class for Transfer operation
 */
public abstract class AbstractTransfer implements Runnable {
  private static final Pattern COMPILE_NEWLINE = Pattern.compile("\n");
  public static final String TIMESTAMP_FORMAT = "yyyyMMddHHmmss";
  /**
   * Internal Logger
   */
  protected static volatile WaarpLogger logger;

  protected static final String INFO_ARGS =
      Messages.getString("AbstractTransfer.0") //$NON-NLS-1$
      + Messages.getString("Message.OutputFormat");

  protected static final String NO_INFO_ARGS = "noinfo";

  protected final TransferArgs transferArgs = new TransferArgs();
  protected final R66Future future;
  protected boolean normalInfoAsWarn = true;

  /**
   * @param clasz Class of Client Transfer
   * @param future
   * @param transferArgs
   */
  protected AbstractTransfer(final Class<?> clasz, final R66Future future,
                             final TransferArgs transferArgs) {
    this(clasz, future, transferArgs.getFilename(), transferArgs.getRulename(),
         transferArgs.getTransferInfo(), transferArgs.isMD5(),
         transferArgs.getRemoteHost(), transferArgs.getBlockSize(),
         transferArgs.getId(), transferArgs.getStartTime());
  }

  /**
   * @param clasz Class of Client Transfer
   * @param future
   * @param filename
   * @param rulename
   * @param transferInfo
   * @param isMD5
   * @param remoteHost
   * @param blocksize
   * @param id
   */
  protected AbstractTransfer(final Class<?> clasz, final R66Future future,
                             final String filename, final String rulename,
                             final String transferInfo, final boolean isMD5,
                             final String remoteHost, final int blocksize,
                             final long id, final Timestamp timestart) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(clasz);
    }
    this.future = future;
    this.transferArgs.setFilename(filename);
    this.transferArgs.setRulename(rulename);
    this.transferArgs.setTransferInfo(transferInfo);
    this.transferArgs.setMD5(isMD5);
    if (Configuration.configuration.getAliases().containsKey(remoteHost)) {
      this.transferArgs.setRemoteHost(
          Configuration.configuration.getAliases().get(remoteHost));
    } else {
      this.transferArgs.setRemoteHost(remoteHost);
    }
    this.transferArgs.setBlockSize(blocksize);
    this.transferArgs.setId(id);
    transferArgs.setStartTime(timestart);
  }

  /**
   * @param host
   * @param localChannelReference
   * @param packet
   * @param future
   *
   * @return True if OK
   */
  public static boolean sendValidPacket(final DbHostAuth host,
                                        final LocalChannelReference localChannelReference,
                                        final AbstractLocalPacket packet,
                                        final R66Future future) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(AbstractTransfer.class);
    }
    try {
      ChannelUtils
          .writeAbstractLocalPacket(localChannelReference, packet, false);
    } catch (final OpenR66ProtocolPacketException e) {
      RequestTransfer.logger
          .error(Messages.getString("RequestTransfer.63") + host); //$NON-NLS-1$
      localChannelReference.close();
      RequestTransfer.logger.debug("Bad Protocol", e);
      future.setResult(
          new R66Result(e, null, true, ErrorCode.TransferError, null));
      future.setFailure(e);
      return false;
    }
    future.awaitOrInterruptible();

    localChannelReference.close();
    return true;
  }

  /**
   * Initiate the Request and return a potential DbTaskRunner
   *
   * @return null if an error occurs or a DbTaskRunner
   */
  protected DbTaskRunner initRequest() {
    final DbRule dbRule;
    try {
      dbRule = new DbRule(transferArgs.getRulename());
    } catch (final WaarpDatabaseException e) {
      logger.error("Cannot get Rule: " + transferArgs.getRulename(), e);
      future.setResult(
          new R66Result(new OpenR66DatabaseGlobalException(e), null, true,
                        ErrorCode.Internal, null));
      future.setFailure(e);
      return null;
    }
    int mode = dbRule.getMode();
    if (transferArgs.isMD5()) {
      mode = RequestPacket.getModeMD5(mode);
    }
    final DbTaskRunner taskRunner;
    if (transferArgs.getId() != ILLEGALVALUE) {
      try {
        taskRunner = new DbTaskRunner(transferArgs.getId(),
                                      transferArgs.getRemoteHost());
      } catch (final WaarpDatabaseException e) {
        logger.error("Cannot get task", e);
        future.setResult(
            new R66Result(new OpenR66DatabaseGlobalException(e), null, true,
                          ErrorCode.QueryRemotelyUnknown, null));
        future.setFailure(e);
        return null;
      }
      // requested
      taskRunner.setSenderByRequestToValidate(true);
      if (transferArgs.getTransferInfo() != null &&
          !transferArgs.getTransferInfo().equals(NO_INFO_ARGS)) {
        taskRunner.setFileInformation(transferArgs.getTransferInfo());
      }
      if (transferArgs.getStartTime() != null) {
        taskRunner.setStart(transferArgs.getStartTime());
      }
    } else {
      long originalSize = -1;
      if (RequestPacket.isSendMode(mode) &&
          !RequestPacket.isThroughMode(mode)) {
        File file = new File(transferArgs.getFilename());
        // Change dir
        try {
          final R66Session session = new R66Session();
          session.getAuth().specialNoSessionAuth(false,
                                                 Configuration.configuration
                                                     .getHostId());
          session.getDir().changeDirectory(dbRule.getSendPath());
          final R66File filer66 = FileUtils
              .getFile(logger, session, transferArgs.getFilename(), true, true,
                       false, null);
          file = filer66.getTrueFile();
        } catch (final CommandAbstractException ignored) {
          SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
        } catch (final OpenR66RunnerErrorException ignored) {
          SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
        }
        if (file.canRead()) {
          originalSize = file.length();
          if (originalSize == 0) {
            originalSize = -1;
          }
        }
      }
      logger.debug("Filesize: {}", originalSize);
      final String sep =
          PartnerConfiguration.getSeparator(transferArgs.getRemoteHost());
      final RequestPacket request =
          new RequestPacket(transferArgs.getRulename(), mode,
                            transferArgs.getFilename(),
                            transferArgs.getBlockSize(), 0,
                            transferArgs.getId(),
                            transferArgs.getTransferInfo(), originalSize, sep);
      // Not isRecv since it is the requester, so send => isRetrieve is true
      final boolean isRetrieve = !RequestPacket.isRecvMode(request.getMode());
      try {
        taskRunner = new DbTaskRunner(dbRule, isRetrieve, request,
                                      transferArgs.getRemoteHost(),
                                      transferArgs.getStartTime());
      } catch (final WaarpDatabaseException e) {
        logger.error("Cannot get task", e);
        future.setResult(
            new R66Result(new OpenR66DatabaseGlobalException(e), null, true,
                          ErrorCode.Internal, null));
        future.setFailure(e);
        return null;
      }
    }
    try {
      taskRunner.saveStatus();
    } catch (final OpenR66RunnerErrorException e) {
      logger.error("Cannot save task", e);
      future.setResult(
          new R66Result(new OpenR66DatabaseGlobalException(e), null, true,
                        ErrorCode.Internal, null));
      future.setFailure(e);
      return null;
    }
    return taskRunner;
  }

  protected static String rhost;
  protected static String localFilename;
  protected static String rule;
  protected static String transferInfo;
  protected static boolean ismd5;
  protected static int block = 0x10000; // 64K
  // as
  // default
  protected static boolean nolog;
  protected static long idt = ILLEGALVALUE;
  protected static Timestamp ttimestart;
  protected static boolean snormalInfoAsWarn = true;
  protected static String sFollowId;

  protected static void clear() {
    rhost = null;
    localFilename = null;
    rule = null;
    transferInfo = null;
    ismd5 = false;
    block = 0x10000; // 64K
    nolog = false;
    idt = ILLEGALVALUE;
    ttimestart = null;
    snormalInfoAsWarn = true;
    sFollowId = null;
  }

  /**
   * Parse the parameter and set current values
   *
   * @param args
   * @param submitOnly True if the client is only a submitter (through
   *     database)
   *
   * @return True if all parameters were found and correct
   */
  protected static boolean getParams(final String[] args,
                                     final boolean submitOnly) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(AbstractTransfer.class);
    }
    if (args.length < 2) {
      logger.error(INFO_ARGS);
      return false;
    }
    if (submitOnly) {
      if (!FileBasedConfiguration
          .setSubmitClientConfigurationFromXml(Configuration.configuration,
                                               args[0])) {
        logger.error(Messages.getString(
            "Configuration.NeedCorrectConfig")); //$NON-NLS-1$
        return false;
      }
    } else if (!FileBasedConfiguration
        .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
      logger.error(
          Messages.getString("Configuration.NeedCorrectConfig")); //$NON-NLS-1$
      return false;
    }
    final TransferArgs transferArgsLocal = getParamsInternal(1, args);
    if (transferArgsLocal == null) {
      return false;
    }
    rhost = transferArgsLocal.getRemoteHost();
    localFilename = transferArgsLocal.getFilename();
    rule = transferArgsLocal.getRulename();
    transferInfo = transferArgsLocal.getTransferInfo();
    ismd5 = transferArgsLocal.isMD5();
    block = transferArgsLocal.getBlockSize();
    idt = transferArgsLocal.getId();
    ttimestart = transferArgsLocal.getStartTime();
    sFollowId = transferArgsLocal.getFollowId();
    return true;
  }

  /**
   * Internal getParams without configuration initialization, but still as
   * first argument<br>
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
   * [-info <arg>)    Specify the transfer information (generally in last position)<br>
   * [-nolog]         Specify to not log anything included database once the
   * transfer is done<br>
   * [-notlogWarn |   Specify to log final result as Info if OK<br>
   * -logWarn]        Specify to log final result as Warn if OK<br>
   *
   * @param rank which rank to start on args
   * @param args
   *
   * @return TransferArgs if OK, null if wrong initialization
   */
  public static TransferArgs getParamsInternal(final int rank,
                                               final String[] args) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(AbstractTransfer.class);
    }
    final TransferArgs transferArgs1 =
        TransferArgs.getParamsInternal(rank, args, true);
    if (transferArgs1 == null) {
      return null;
    }
    snormalInfoAsWarn = transferArgs1.isNormalInfoAsWarn();
    nolog = transferArgs1.isNolog();
    return transferArgs1;
  }

  /**
   * Shared code for finalize one Transfer request in error
   *
   * @param runner
   * @param taskRunner
   */
  protected void finalizeInErrorTransferRequest(final ClientRunner runner,
                                                final DbTaskRunner taskRunner,
                                                final ErrorCode code) {
    if (runner.getLocalChannelReference() != null) {
      runner.getLocalChannelReference().setErrorMessage(code.getMesg(), code);
    }
    taskRunner.setErrorTask();
    try {
      taskRunner.forceSaveStatus();
      taskRunner.run();
    } catch (final OpenR66RunnerErrorException e1) {
      runner.changeUpdatedInfo(UpdatedInfo.INERROR, code, true);
    }
  }

  public void setNormalInfoAsWarn(final boolean normalInfoAsWarn1) {
    normalInfoAsWarn = normalInfoAsWarn1;
  }

  public List<String> getRemoteFiles(final String[] localfilenames,
                                     final String requested,
                                     final NetworkTransaction networkTransaction) {
    final List<String> files = new ArrayList<String>();
    for (final String filenameNew : localfilenames) {
      if (!(filenameNew.contains("*") || filenameNew.contains("?") ||
            filenameNew.contains("~"))) {
        files.add(filenameNew);
      } else {
        // remote query
        final R66Future futureInfo = new R66Future(true);
        logger
            .info("{} {} to {}", Messages.getString("Transfer.3"), filenameNew,
                  requested); //$NON-NLS-1$
        final RequestInformation info =
            new RequestInformation(futureInfo, requested,
                                   transferArgs.getRulename(), filenameNew,
                                   (byte) ASKENUM.ASKLIST.ordinal(), -1, false,
                                   networkTransaction);
        info.run();
        futureInfo.awaitOrInterruptible();
        if (futureInfo.isSuccess()) {
          final ValidPacket valid =
              (ValidPacket) futureInfo.getResult().getOther();
          if (valid != null) {
            final String line = valid.getSheader();
            final String[] lines = COMPILE_NEWLINE.split(line);
            for (final String string : lines) {
              final File tmpFile = new File(string);
              files.add(tmpFile.getPath());
            }
          }
        } else {
          logger.error(Messages.getString("Transfer.6") + filenameNew + " to " +
                       requested + ": " + (futureInfo.getCause() == null? "" :
              futureInfo.getCause().getMessage())); //$NON-NLS-1$
        }
      }
    }
    return files;
  }

  public List<String> getLocalFiles(final DbRule dbrule,
                                    final String[] localfilenames) {
    final List<String> files = new ArrayList<String>();
    final R66Session session = new R66Session();
    session.getAuth().specialNoSessionAuth(false, Configuration.configuration
        .getHostId());
    final R66Dir dir = new R66Dir(session);
    try {
      dir.changeDirectory(dbrule.getSendPath());
    } catch (final CommandAbstractException ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
    }
    if (localfilenames != null) {
      for (final String filenameNew : localfilenames) {
        if (!(filenameNew.contains("*") || filenameNew.contains("?") ||
              filenameNew.contains("~"))) {
          logger.info("Direct add: {}", filenameNew);
          files.add(filenameNew);
        } else {
          // local: must check
          logger
              .info("Local Ask for {} from {}", filenameNew, dir.getFullPath());
          final List<String> list;
          try {
            list = dir.list(filenameNew);
            if (list != null) {
              files.addAll(list);
            }
          } catch (final CommandAbstractException e) {
            logger.warn(
                Messages.getString("Transfer.14") + filenameNew + " : " +
                e.getMessage()); //$NON-NLS-1$
          }
        }
      }
    }
    return files;
  }

  /**
   * Helper to connect
   *
   * @param host
   * @param future
   * @param networkTransaction
   *
   * @return localChannelReference not null if ok
   */
  public static LocalChannelReference tryConnect(final DbHostAuth host,
                                                 final R66Future future,
                                                 final NetworkTransaction networkTransaction) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(AbstractTransfer.class);
    }
    logger.info("Try RequestTransfer to {}", host);
    final SocketAddress socketAddress;
    try {
      socketAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.debug("Cannot connect to {}", host);
      future.setResult(new R66Result(new OpenR66ProtocolNoConnectionException(
          "Cannot connect to server " + host.getHostid()), null, true,
                                     ErrorCode.ConnectionImpossible, null));
      future.setFailure(future.getResult().getException());
      return null;
    }
    final boolean isSSL = host.isSsl();

    final LocalChannelReference localChannelReference = networkTransaction
        .createConnectionWithRetry(socketAddress, isSSL, future);
    if (localChannelReference == null) {
      logger.debug("Cannot connect to {}", host);
      future.setResult(new R66Result(new OpenR66ProtocolNoConnectionException(
          "Cannot connect to server " + host.getHostid()), null, true,
                                     ErrorCode.ConnectionImpossible, null));
      future.setFailure(future.getResult().getException());
      return null;
    }
    return localChannelReference;
  }


  protected static void prepareKoOutputFormat(final R66Future future,
                                              final R66Result result,
                                              final OutputFormat outputFormat) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(AbstractTransfer.class);
    }
    partialOutputFormat(result.getRunner(), outputFormat);
    if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
      outputFormat.setValue(FIELDS.status.name(), 1);
      outputFormat.setValue(FIELDS.statusTxt.name(),
                            Messages.getString("Transfer.Status") + Messages
                                .getString(
                                    "RequestInformation.Warned")); //$NON-NLS-1$
    } else {
      outputFormat.setValue(FIELDS.status.name(), 2);
      outputFormat.setValue(FIELDS.statusTxt.name(),
                            Messages.getString("Transfer.Status") + Messages
                                .getString(
                                    "RequestInformation.Failure")); //$NON-NLS-1$
    }
    if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
      logger.warn(outputFormat.loggerOut(), future.getCause());
    } else {
      logger.error(outputFormat.loggerOut(), future.getCause());
    }
    if (future.getCause() != null) {
      outputFormat
          .setValue(FIELDS.error.name(), future.getCause().getMessage());
    }
  }

  protected static void prepareKoOutputFormat(final R66Future future,
                                              final OutputFormat outputFormat) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(AbstractTransfer.class);
    }
    outputFormat.setValue(FIELDS.status.name(), 2);
    outputFormat.setValue(FIELDS.statusTxt.name(), Messages
        .getString("Transfer.FailedNoId")); //$NON-NLS-1$
    outputFormat.setValue(FIELDS.remote.name(), rhost);
    logger.error(outputFormat.loggerOut(), future.getCause());
    outputFormat.setValue(FIELDS.error.name(), future.getCause().getMessage());
  }

  protected static void prepareOkOutputFormat(final long delay,
                                              final R66Result result,
                                              final OutputFormat outputFormat) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(AbstractTransfer.class);
    }
    if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
      outputFormat.setValue(FIELDS.status.name(), 1);
      outputFormat.setValue(FIELDS.statusTxt.name(),
                            Messages.getString("Transfer.Status") + Messages
                                .getString(
                                    "RequestInformation.Warned")); //$NON-NLS-1$
    } else {
      outputFormat.setValue(FIELDS.status.name(), 0);
      outputFormat.setValue(FIELDS.statusTxt.name(),
                            Messages.getString("Transfer.Status") + Messages
                                .getString(
                                    "RequestInformation.Success")); //$NON-NLS-1$
    }
    partialOutputFormat(result.getRunner(), outputFormat);
    outputFormat.setValue("filefinal", result.getFile() != null?
        result.getFile().toString() : "no file");
    outputFormat.setValue("delay", delay);
  }

  private static void partialOutputFormat(final DbTaskRunner runner,
                                          final OutputFormat outputFormat) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(AbstractTransfer.class);
    }
    outputFormat.setValue(FIELDS.remote.name(), rhost);
    outputFormat.setValue(FIELDS.ruleid.name(), runner.getRuleId());
    outputFormat.setValueString(runner.getJson());
    outputFormat
        .setValue(FIELDS.statusCode.name(), runner.getErrorInfo().getCode());
    outputFormat.setValue(FIELDS.specialid.name(), runner.getSpecialId());
    outputFormat.setValue(FIELDS.finalPath.name(), runner.getFilename());
    outputFormat.setValue(FIELDS.requested.name(), runner.getRequested());
    outputFormat.setValue(FIELDS.requester.name(), runner.getRequester());
    outputFormat
        .setValue(FIELDS.fileInformation.name(), runner.getFileInformation());
    outputFormat
        .setValue(FIELDS.transferInformation.name(), runner.getTransferInfo());
    outputFormat.setValue(FIELDS.originalSize.name(), runner.getOriginalSize());
    outputFormat
        .setValue(FIELDS.originalPath.name(), runner.getOriginalFilename());
  }

  protected static void prepareSubmitKoOutputFormat(final R66Future future,
                                                    final DbTaskRunner runner,
                                                    final OutputFormat outputFormat) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(AbstractTransfer.class);
    }
    outputFormat.setValue(FIELDS.status.name(), 2);
    if (runner == null) {
      outputFormat.setValue(FIELDS.statusTxt.name(),
                            Messages.getString("SubmitTransfer.3") + Messages
                                .getString(
                                    "Transfer.FailedNoId")); //$NON-NLS-1$
      outputFormat.setValue(FIELDS.remote.name(), rhost);
    } else {
      outputFormat.setValue(FIELDS.statusTxt.name(),
                            Messages.getString("SubmitTransfer.3") + Messages
                                .getString(
                                    "RequestInformation.Failure")); //$NON-NLS-1$
      partialOutputFormat(runner, outputFormat);
    }
    logger.error(outputFormat.loggerOut(), future.getCause());
    if (future.getCause() != null) {
      outputFormat
          .setValue(FIELDS.error.name(), future.getCause().getMessage());
    }
  }

  protected static void prepareSubmitOkOutputFormat(final DbTaskRunner runner,
                                                    final OutputFormat outputFormat) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(AbstractTransfer.class);
    }
    outputFormat.setValue(FIELDS.status.name(), 0);
    outputFormat.setValue(FIELDS.statusTxt.name(),
                          Messages.getString("SubmitTransfer.3") + Messages
                              .getString(
                                  "RequestInformation.Success")); //$NON-NLS-1$
    partialOutputFormat(runner, outputFormat);
  }

}
