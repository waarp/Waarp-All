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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static org.waarp.common.database.DbConstant.*;

/**
 * Abstract class for Transfer operation
 */
public abstract class AbstractTransfer implements Runnable {
  private static final Pattern COMPILE_NEWLINE = Pattern.compile("\n");
  /**
   * Internal Logger
   */
  protected static volatile WaarpLogger logger;

  protected static final String _INFO_ARGS =
      Messages.getString("AbstractTransfer.0") //$NON-NLS-1$
      + Messages.getString("Message.OutputFormat");

  protected static final String NO_INFO_ARGS = "noinfo";

  protected final R66Future future;

  protected final String filename;

  protected final String rulename;

  protected final String fileinfo;

  protected final boolean isMD5;

  protected final String remoteHost;

  protected final int blocksize;

  protected final long id;

  protected final Timestamp startTime;

  protected boolean normalInfoAsWarn = true;

  /**
   * @param clasz Class of Client Transfer
   * @param future
   * @param filename
   * @param rulename
   * @param fileinfo
   * @param isMD5
   * @param remoteHost
   * @param blocksize
   * @param id
   */
  protected AbstractTransfer(Class<?> clasz, R66Future future, String filename,
                             String rulename, String fileinfo, boolean isMD5,
                             String remoteHost, int blocksize, long id,
                             Timestamp timestart) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(clasz);
    }
    this.future = future;
    this.filename = filename;
    this.rulename = rulename;
    this.fileinfo = fileinfo;
    this.isMD5 = isMD5;
    if (Configuration.configuration.getAliases().containsKey(remoteHost)) {
      this.remoteHost =
          Configuration.configuration.getAliases().get(remoteHost);
    } else {
      this.remoteHost = remoteHost;
    }
    this.blocksize = blocksize;
    this.id = id;
    startTime = timestart;
  }

  /**
   * @param host
   * @param localChannelReference
   * @param packet
   * @param future
   *
   * @return True if OK
   */
  public static boolean sendValidPacket(DbHostAuth host,
                                        LocalChannelReference localChannelReference,
                                        AbstractLocalPacket packet,
                                        final R66Future future) {
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
    DbRule dbRule;
    try {
      dbRule = new DbRule(rulename);
    } catch (final WaarpDatabaseException e) {
      logger.error("Cannot get Rule: " + rulename, e);
      future.setResult(
          new R66Result(new OpenR66DatabaseGlobalException(e), null, true,
                        ErrorCode.Internal, null));
      future.setFailure(e);
      return null;
    }
    int mode = dbRule.getMode();
    if (isMD5) {
      mode = RequestPacket.getModeMD5(mode);
    }
    DbTaskRunner taskRunner;
    if (id != ILLEGALVALUE) {
      try {
        taskRunner = new DbTaskRunner(id, remoteHost);
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
      if (fileinfo != null && !fileinfo.equals(NO_INFO_ARGS)) {
        taskRunner.setFileInformation(fileinfo);
      }
      if (startTime != null) {
        taskRunner.setStart(startTime);
      }
    } else {
      long originalSize = -1;
      if (RequestPacket.isSendMode(mode) &&
          !RequestPacket.isThroughMode(mode)) {
        File file = new File(filename);
        // Change dir
        try {
          final R66Session session = new R66Session();
          session.getAuth().specialNoSessionAuth(false,
                                                 Configuration.configuration
                                                     .getHostId());
          session.getDir().changeDirectory(dbRule.getSendPath());
          final R66File filer66 = FileUtils
              .getFile(logger, session, filename, true, true, false, null);
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
      logger.debug("Filesize: " + originalSize);
      final String sep = PartnerConfiguration.getSeparator(remoteHost);
      final RequestPacket request =
          new RequestPacket(rulename, mode, filename, blocksize, 0, id,
                            fileinfo, originalSize, sep);
      // Not isRecv since it is the requester, so send => isRetrieve is true
      final boolean isRetrieve = !RequestPacket.isRecvMode(request.getMode());
      try {
        taskRunner = new DbTaskRunner(dbRule, isRetrieve, request, remoteHost,
                                      startTime);
      } catch (final WaarpDatabaseException e) {
        logger.error("Cannot get task", e);
        future.setResult(
            new R66Result(new OpenR66DatabaseGlobalException(e), null, true,
                          ErrorCode.Internal, null));
        future.setFailure(e);
        return null;
      }
    }
    return taskRunner;
  }

  protected static String rhost;
  protected static String localFilename;
  protected static String rule;
  protected static String fileInfo;
  protected static boolean ismd5;
  protected static int block = 0x10000; // 64K
  // as
  // default
  protected static boolean nolog;
  protected static long idt = ILLEGALVALUE;
  protected static Timestamp ttimestart;
  protected static boolean snormalInfoAsWarn = true;

  /**
   * Parse the parameter and set current values
   *
   * @param args
   * @param submitOnly True if the client is only a submitter (through
   *     database)
   *
   * @return True if all parameters were found and correct
   */
  protected static boolean getParams(String[] args, boolean submitOnly) {
    if (args.length < 2) {
      logger.error(_INFO_ARGS);
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
    // Now set default values from configuration
    block = Configuration.configuration.getBlockSize();
    int i = 1;
    try {
      for (i = 1; i < args.length; i++) {
        if ("-to".equalsIgnoreCase(args[i])) {
          i++;
          rhost = args[i];
          if (Configuration.configuration.getAliases().containsKey(rhost)) {
            rhost = Configuration.configuration.getAliases().get(rhost);
          }
        } else if ("-file".equalsIgnoreCase(args[i])) {
          i++;
          localFilename = args[i];
          localFilename = localFilename.replace('§', '*');
        } else if ("-rule".equalsIgnoreCase(args[i])) {
          i++;
          rule = args[i];
        } else if ("-info".equalsIgnoreCase(args[i])) {
          i++;
          fileInfo = args[i];
        } else if ("-md5".equalsIgnoreCase(args[i])) {
          ismd5 = true;
        } else if ("-logWarn".equalsIgnoreCase(args[i])) {
          snormalInfoAsWarn = true;
        } else if ("-notlogWarn".equalsIgnoreCase(args[i])) {
          snormalInfoAsWarn = false;
        } else if ("-block".equalsIgnoreCase(args[i])) {
          i++;
          block = Integer.parseInt(args[i]);
          if (block < 100) {
            logger.error(
                Messages.getString("AbstractTransfer.1") + block); //$NON-NLS-1$
            return false;
          }
        } else if ("-nolog".equalsIgnoreCase(args[i])) {
          nolog = true;
        } else if ("-id".equalsIgnoreCase(args[i])) {
          i++;
          idt = Long.parseLong(args[i]);
        } else if ("-start".equalsIgnoreCase(args[i])) {
          i++;
          Date date;
          final SimpleDateFormat dateFormat =
              new SimpleDateFormat("yyyyMMddHHmmss");
          try {
            date = dateFormat.parse(args[i]);
            ttimestart = new Timestamp(date.getTime());
          } catch (final ParseException ignored) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
          }
        } else if ("-delay".equalsIgnoreCase(args[i])) {
          i++;
          if (args[i].charAt(0) == '+') {
            ttimestart = new Timestamp(System.currentTimeMillis() +
                                       Long.parseLong(args[i].substring(1)));
          } else {
            ttimestart = new Timestamp(Long.parseLong(args[i]));
          }
        }
      }
      OutputFormat.getParams(args);
    } catch (final NumberFormatException e) {
      logger.error(Messages.getString("AbstractTransfer.20") + i); //$NON-NLS-1$
      return false;
    }
    if (fileInfo == null) {
      fileInfo = NO_INFO_ARGS;
    }
    if (rhost != null && rule != null && localFilename != null) {
      return true;
    } else if (idt != ILLEGALVALUE && rhost != null) {
      try {
        final DbTaskRunner runner = new DbTaskRunner(idt, rhost);
        rule = runner.getRuleId();
        localFilename = runner.getOriginalFilename();
        return true;
      } catch (final WaarpDatabaseException e) {
        logger.error(Messages.getString("AbstractBusinessRequest.NeedMoreArgs",
                                        "(-to -rule -file | -to -id)")
                     //$NON-NLS-1$
            , e);
        return false;
      }

    }
    logger.error(Messages.getString("AbstractBusinessRequest.NeedMoreArgs",
                                    "(-to -rule -file | -to -id)") +
                 //$NON-NLS-1$
                 _INFO_ARGS);
    return false;
  }

  /**
   * Shared code for finalize one Transfer request in error
   *
   * @param runner
   * @param taskRunner
   */
  protected void finalizeInErrorTransferRequest(ClientRunner runner,
                                                DbTaskRunner taskRunner,
                                                ErrorCode code) {
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

  public void setNormalInfoAsWarn(boolean normalInfoAsWarn1) {
    normalInfoAsWarn = normalInfoAsWarn1;
  }

  public List<String> getRemoteFiles(DbRule dbrule, String[] localfilenames,
                                     String requested,
                                     NetworkTransaction networkTransaction) {
    final List<String> files = new ArrayList<String>();
    for (final String filenameNew : localfilenames) {
      if (!(filenameNew.contains("*") || filenameNew.contains("?") ||
            filenameNew.contains("~"))) {
        files.add(filenameNew);
      } else {
        // remote query
        final R66Future futureInfo = new R66Future(true);
        logger.info(Messages.getString("Transfer.3") + filenameNew + " to " +
                    requested); //$NON-NLS-1$
        final RequestInformation info =
            new RequestInformation(futureInfo, requested, rulename, filenameNew,
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

  public List<String> getLocalFiles(DbRule dbrule, String[] localfilenames) {
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
          logger.info("Direct add: " + filenameNew);
          files.add(filenameNew);
        } else {
          // local: must check
          logger.info(
              "Local Ask for " + filenameNew + " from " + dir.getFullPath());
          List<String> list;
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
  public static LocalChannelReference tryConnect(DbHostAuth host,
                                                 R66Future future,
                                                 NetworkTransaction networkTransaction) {
    logger.info("Try RequestTransfer to " + host);
    SocketAddress socketAddress;
    try {
      socketAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.debug("Cannot connect to " + host);
      future.setResult(new R66Result(new OpenR66ProtocolNoConnectionException(
          "Cannot connect to server " + host.getHostid()), null, true,
                                     ErrorCode.ConnectionImpossible, null));
      future.setFailure(future.getResult().getException());
      return null;
    }
    final boolean isSSL = host.isSsl();

    LocalChannelReference localChannelReference = networkTransaction
        .createConnectionWithRetry(socketAddress, isSSL, future);
    if (localChannelReference == null) {
      logger.debug("Cannot connect to " + host);
      future.setResult(new R66Result(new OpenR66ProtocolNoConnectionException(
          "Cannot connect to server " + host.getHostid()), null, true,
                                     ErrorCode.ConnectionImpossible, null));
      future.setFailure(future.getResult().getException());
      return null;
    }
    return localChannelReference;
  }
}
