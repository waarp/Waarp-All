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
package org.waarp.openr66.server;

import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.client.AbstractTransfer;
import org.waarp.openr66.client.DirectTransfer;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoDataException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.json.LogJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.LogResponseJsonPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import java.io.File;
import java.sql.Timestamp;

import static org.waarp.common.database.DbConstant.*;

/**
 * Log Export from a client (local or allowed)
 */
public class LogExtendedExport implements Runnable {
  /**
   * Internal Logger
   */
  static volatile WaarpLogger logger;

  protected static final String INFO_ARGS =
      "Need at least the configuration file as first argument then optionally\n" +
      "    -purge\n    -clean\n    -startid id\n    -stopid id\n    -rule rule\n    -request host\n" +
      "    -pending\n    -transfer\n    -done\n    -error\n" +
      "    -start timestamp in format yyyyMMddHHmmssSSS possibly truncated and where one of ':-. ' can be separators\n" +
      "    -stop timestamp in same format than start\n" +
      "If not start and no stop are given, stop is Today Midnight (00:00:00)\n" +
      "If start is equals or greater than stop, stop is start+24H\n" +
      "    -host host (optional additional options:\n" +
      "      -ruleDownload ruleDownload if set, will try to download the exported logs\n" +
      "      -import that will try to import exported logs using ruleDownload to download the logs)";

  protected final R66Future future;
  protected final boolean clean;
  protected final boolean purgeLog;
  protected final Timestamp start;
  protected final Timestamp stop;
  protected final String startid;
  protected final String stopid;
  protected final String rule;
  protected final String request;
  protected final boolean statuspending;
  protected final boolean statustransfer;
  protected final boolean statusdone;
  protected final boolean statuserror;
  protected String ruleDownload;
  protected boolean tryimport;
  protected final NetworkTransaction networkTransaction;
  protected DbHostAuth host;

  /**
   * @param future
   * @param clean
   * @param purgeLog
   * @param start
   * @param stop
   * @param startid
   * @param stopid
   * @param rule
   * @param request
   * @param statuspending
   * @param statustransfer
   * @param statusdone
   * @param statuserror
   * @param networkTransaction
   * @param host
   */
  public LogExtendedExport(final R66Future future, final boolean clean,
                           final boolean purgeLog, final Timestamp start,
                           final Timestamp stop, final String startid,
                           final String stopid, final String rule,
                           final String request, final boolean statuspending,
                           final boolean statustransfer,
                           final boolean statusdone, final boolean statuserror,
                           final NetworkTransaction networkTransaction,
                           final DbHostAuth host) {
    this.future = future;
    this.clean = clean;
    this.purgeLog = purgeLog;
    this.start = start;
    this.stop = stop;
    this.startid = startid;
    this.stopid = stopid;
    this.rule = rule;
    this.request = request;
    this.statuspending = statuspending;
    this.statustransfer = statustransfer;
    this.statusdone = statusdone;
    this.statuserror = statuserror;
    this.networkTransaction = networkTransaction;
    this.host = host;
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(LogExtendedExport.class);
    }
  }

  /**
   * Try first to download the exported logs, then to import (must be a host
   * different than the source one)
   *
   * @param ruleDownload
   * @param tryimport
   */
  public void setDownloadTryImport(final String ruleDownload,
                                   final boolean tryimport) {
    this.ruleDownload = ruleDownload;
    this.tryimport = ruleDownload != null && tryimport && !host.getHostid()
                                                               .equals(
                                                                   Configuration.configuration
                                                                       .getHostId()) &&
                     !host.getHostid()
                          .equals(Configuration.configuration.getHostSslId());
  }

  /**
   * Prior to call this method, the pipeline and NetworkTransaction must have
   * been initialized. It is the
   * responsibility of the caller to finish all network resources.
   */
  @Override
  public void run() {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(LogExtendedExport.class);
    }
    if (!(statusdone || statuserror || statuspending || statustransfer)) {
      logger.error("No action required");
      future.setResult(new R66Result(
          new OpenR66ProtocolNoDataException("No action required"), null, true,
          ErrorCode.IncorrectCommand, null));
      future.setFailure(future.getResult().getException());
      return;
    }

    final byte type = purgeLog? LocalPacketFactory.LOGPURGEPACKET :
        LocalPacketFactory.LOGPACKET;
    final LogJsonPacket node = new LogJsonPacket();
    node.setClean(clean);
    node.setPurge(purgeLog);
    node.setStart(start);
    node.setStop(stop);
    node.setStartid(startid);
    node.setStopid(stopid);
    node.setRule(rule);
    node.setRequest(request);
    node.setStatuspending(statuspending);
    node.setStatustransfer(statustransfer);
    node.setStatuserror(statuserror);
    node.setStatusdone(statusdone);

    final JsonCommandPacket valid = new JsonCommandPacket(node, type);
    logger.debug("ExtendedLogCommand: " + valid.getRequest());
    final R66Future newFuture = new R66Future(true);
    final LocalChannelReference localChannelReference =
        AbstractTransfer.tryConnect(host, newFuture, networkTransaction);
    if (localChannelReference == null) {
      future.setResult(newFuture.getResult());
      future.setFailure(future.getCause());
      return;
    }
    localChannelReference.sessionNewState(R66FiniteDualStates.VALIDOTHER);
    try {
      ChannelUtils
          .writeAbstractLocalPacket(localChannelReference, valid, false);
    } catch (final OpenR66ProtocolPacketException e) {
      logger.error("Bad Protocol", e);
      localChannelReference.close();
      host = null;
      future.setResult(
          new R66Result(e, null, true, ErrorCode.TransferError, null));
      future.setFailure(e);
      return;
    }
    host = null;
    newFuture.awaitOrInterruptible();
    logger.info(
        "Request done with " + (newFuture.isSuccess()? "success" : "error"));
    if (newFuture.isSuccess() && ruleDownload != null &&
        !ruleDownload.isEmpty()) {
      try {
        importLog(newFuture);
      } catch (final OpenR66ProtocolBusinessException e) {
        localChannelReference.close();
        return;
      }
    }
    future.setFilesize(newFuture.getFilesize());
    future.setRunner(newFuture.getRunner());
    future.setResult(newFuture.getResult());
    if (newFuture.isSuccess()) {
      future.setSuccess();
    } else {
      if (newFuture.getCause() != null) {
        future.setFailure(newFuture.getCause());
      }
      future.cancel();
    }
    localChannelReference.close();
  }

  public void importLog(final R66Future future)
      throws OpenR66ProtocolBusinessException {
    if (future.isSuccess()) {
      final JsonCommandPacket packet =
          (JsonCommandPacket) future.getResult().getOther();
      if (packet != null) {
        final LogResponseJsonPacket res =
            (LogResponseJsonPacket) packet.getJsonRequest();
        final String fileExported = res.getFilename();
        // download logs
        if (fileExported != null && !fileExported.isEmpty()) {
          final String ruleToExport = ruleDownload;
          final R66Future futuretemp = new R66Future(true);
          final DirectTransfer transfer =
              new DirectTransfer(futuretemp, host.getHostid(), fileExported,
                                 ruleToExport, "Get Exported Logs from " +
                                               Configuration.configuration
                                                   .getHostId(), false,
                                 Configuration.configuration.getBlockSize(),
                                 ILLEGALVALUE, networkTransaction);
          transfer.run();
          final File logsFile;
          if (!futuretemp.isSuccess()) {
            if (futuretemp.getCause() != null) {
              throw new OpenR66ProtocolBusinessException(futuretemp.getCause());
            }
            throw new OpenR66ProtocolBusinessException(
                "Download of exported logs in error");
          } else {
            logsFile = futuretemp.getResult().getFile().getTrueFile();
          }
          if (tryimport) {
            try {
              DbTaskRunner.loadXml(logsFile);
            } catch (final OpenR66ProtocolBusinessException e) {
              logger.warn(
                  "Cannot load the logs from " + logsFile.getAbsolutePath() +
                  " since: " + e.getMessage());
              throw new OpenR66ProtocolBusinessException(
                  "Cannot load the logs from " + logsFile.getAbsolutePath(), e);
            }
          }
        } else {
          throw new OpenR66ProtocolBusinessException(
              "Export log with no file result");
        }
      } else {
        throw new OpenR66ProtocolBusinessException(
            "Export log with no file result");
      }
    } else {
      if (future.getCause() != null) {
        throw new OpenR66ProtocolBusinessException(future.getCause());
      }
      throw new OpenR66ProtocolBusinessException("Export log in error");
    }
  }

  protected static boolean sclean;
  protected static boolean spurgeLog;
  protected static Timestamp sstart;
  protected static Timestamp sstop;
  protected static String sstartid;
  protected static String sstopid;
  protected static String srule;
  protected static String srequest;
  protected static boolean sstatuspending;
  protected static boolean sstatustransfer;
  protected static boolean sstatusdone = true;
  protected static boolean sstatuserror;
  protected static String stohost;
  protected static String sruleDownload;
  protected static boolean stryimport;

  protected static boolean getParams(final String[] args) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(LogExtendedExport.class);
    }
    if (args.length < 1) {
      logger.error(INFO_ARGS);
      return false;
    }
    if (!FileBasedConfiguration
        .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
      logger.error(INFO_ARGS);
      return false;
    }
    String ssstart = null;
    String ssstop = null;
    for (int i = 1; i < args.length; i++) {
      if ("-purge".equalsIgnoreCase(args[i])) {
        spurgeLog = true;
      } else if ("-clean".equalsIgnoreCase(args[i])) {
        sclean = true;
      } else if ("-start".equalsIgnoreCase(args[i])) {
        i++;
        ssstart = args[i];
      } else if ("-stop".equalsIgnoreCase(args[i])) {
        i++;
        ssstop = args[i];
      } else if ("-startid".equalsIgnoreCase(args[i])) {
        i++;
        sstartid = args[i];
      } else if ("-stopid".equalsIgnoreCase(args[i])) {
        i++;
        sstopid = args[i];
      } else if ("-rule".equalsIgnoreCase(args[i])) {
        i++;
        srule = args[i];
      } else if ("-request".equalsIgnoreCase(args[i])) {
        i++;
        srequest = args[i];
      } else if ("-pending".equalsIgnoreCase(args[i])) {
        sstatuspending = true;
      } else if ("-transfer".equalsIgnoreCase(args[i])) {
        sstatustransfer = true;
      } else if ("-done".equalsIgnoreCase(args[i])) {
        sstatusdone = true;
      } else if ("-error".equalsIgnoreCase(args[i])) {
        sstatuserror = true;
      } else if ("-import".equalsIgnoreCase(args[i])) {
        stryimport = true;
      } else if ("-host".equalsIgnoreCase(args[i])) {
        i++;
        stohost = args[i];
      } else if ("-ruleDownload".equalsIgnoreCase(args[i])) {
        i++;
        sruleDownload = args[i];
      }
    }
    if (ssstart != null) {
      final Timestamp tstart = WaarpStringUtils.fixDate(ssstart);
      if (tstart != null) {
        sstart = tstart;
      }
    }
    if (ssstop != null) {
      final Timestamp tstop = WaarpStringUtils.fixDate(ssstop, sstart);
      if (tstop != null) {
        sstop = tstop;
      }
    }
    if (ssstart == null && ssstop == null) {
      sstop = WaarpStringUtils.getTodayMidnight();
    }
    if (stohost == null) {
      stryimport = false;
    }
    return true;
  }

  public static void main(final String[] args) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(LogExtendedExport.class);
    }
    if (!getParams(args)) {
      logger.error("Wrong initialization");
      if (admin != null) {
        admin.close();
      }
      System.exit(1);//NOSONAR
    }
    final long time1 = System.currentTimeMillis();
    DbHostAuth dbhost = null;
    if (stohost != null) {
      try {
        dbhost = new DbHostAuth(stohost);
      } catch (final WaarpDatabaseException e) {
        logger.error("Wrong initialization");
        if (admin != null) {
          admin.close();
        }
        System.exit(2);//NOSONAR
      }
    } else {
      dbhost = Configuration.configuration.getHostSslAuth();
      stohost = Configuration.configuration.getHostSslId();
    }
    final R66Future future = new R66Future(true);

    Configuration.configuration.pipelineInit();
    final NetworkTransaction networkTransaction = new NetworkTransaction();
    try {
      final LogExtendedExport transaction =
          new LogExtendedExport(future, sclean, spurgeLog, sstart, sstop,
                                sstartid, sstopid, srule, srequest,
                                sstatuspending, sstatustransfer, sstatusdone,
                                sstatuserror, networkTransaction, dbhost);
      transaction.setDownloadTryImport(sruleDownload, stryimport);
      transaction.run();
      future.awaitOrInterruptible();
      final long time2 = System.currentTimeMillis();
      final long delay = time2 - time1;
      final R66Result result = future.getResult();
      if (future.isSuccess()) {
        if (result.getCode() == ErrorCode.Warning) {
          logger.warn("WARNED on file:     " + (result.getOther() != null?
              ((JsonCommandPacket) result.getOther()).getRequest() :
              "no file") + "     delay: " + delay);
        } else {
          logger.warn("SUCCESS on Final file:     " +
                      (result.getOther() != null?
                          ((JsonCommandPacket) result.getOther()).getRequest() :
                          "no file") + "     delay: " + delay);
        }
      } else {
        if (result.getCode() == ErrorCode.Warning) {
          logger.warn("LogExtendedExport is     WARNED", future.getCause());
        } else {
          logger.error("LogExtendedExport in     FAILURE", future.getCause());
        }
        networkTransaction.closeAll();
        System.exit(result.getCode().ordinal());//NOSONAR
      }
    } finally {
      networkTransaction.closeAll();
      System.exit(0);//NOSONAR
    }
  }

}
