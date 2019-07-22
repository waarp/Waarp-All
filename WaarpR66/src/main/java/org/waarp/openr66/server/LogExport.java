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
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import java.net.SocketAddress;
import java.sql.Timestamp;

/**
 * Log Export from a local client without database connection
 *
 *
 */
public class LogExport implements Runnable {
  /**
   * Internal Logger
   */
  static volatile WaarpLogger logger;

  protected static String _INFO_ARGS =
      "Need at least the configuration file as first argument then optionally\n" +
      "    -purge\n" + "    -clean\n" +
      "    -start timestamp in format yyyyMMddHHmmssSSS possibly truncated and where one of ':-. ' can be separators\n" +
      "    -stop timestamp in same format than start\n" +
      "If not start and no stop are given, stop is Today Midnight (00:00:00)\n" +
      "If start is equals or greater than stop, stop is start+24H\n" +
      "    -host host (optional)";

  protected final R66Future future;
  protected final boolean purgeLog;
  protected final Timestamp start;
  protected final Timestamp stop;
  protected final boolean clean;
  protected final NetworkTransaction networkTransaction;
  protected DbHostAuth host;

  public LogExport(R66Future future, boolean purgeLog, boolean clean,
                   Timestamp start, Timestamp stop,
                   NetworkTransaction networkTransaction) {
    this.future = future;
    this.purgeLog = purgeLog;
    this.clean = clean;
    this.start = start;
    this.stop = stop;
    this.networkTransaction = networkTransaction;
    host = Configuration.configuration.getHOST_SSLAUTH();
  }

  public void setHost(DbHostAuth host) {
    this.host = host;
  }

  /**
   * Prior to call this method, the pipeline and NetworkTransaction must have
   * been initialized. It is the
   * responsibility of the caller to finish all network resources.
   */
  @Override
  public void run() {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(LogExport.class);
    }
    final String lstart = (start != null)? start.toString() : null;
    final String lstop = (stop != null)? stop.toString() : null;
    final byte type = (purgeLog)? LocalPacketFactory.LOGPURGEPACKET :
        LocalPacketFactory.LOGPACKET;
    ValidPacket valid = new ValidPacket(lstart, lstop, type);
    SocketAddress socketAddress;
    try {
      socketAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Cannot Connect to " + host.getHostid());
      future.setResult(new R66Result(new OpenR66ProtocolNoConnectionException(
          "Cannot connect to server " + host.getHostid()), null, true,
                                     ErrorCode.ConnectionImpossible, null));
      host = null;
      future.setFailure(future.getResult().getException());
      return;
    }
    final boolean isSSL = host.isSsl();

    // first clean if ask
    if (clean &&
        (host.getHostid().equals(Configuration.configuration.getHOST_ID()) ||
         host.getHostid()
             .equals(Configuration.configuration.getHOST_SSLID()))) {
      // Update all UpdatedInfo to DONE
      // where GlobalLastStep = ALLDONETASK and status = CompleteOk
      try {
        DbTaskRunner.changeFinishedToDone();
      } catch (final WaarpDatabaseNoConnectionException e) {
        logger.warn("Clean cannot be done {}", e.getMessage());
      }
    }
    LocalChannelReference localChannelReference = networkTransaction
        .createConnectionWithRetry(socketAddress, isSSL, future);
    socketAddress = null;
    if (localChannelReference == null) {
      logger.error("Cannot Connect to " + host.getHostid());
      future.setResult(new R66Result(new OpenR66ProtocolNoConnectionException(
          "Cannot connect to server " + host.getHostid()), null, true,
                                     ErrorCode.ConnectionImpossible, null));
      host = null;
      future.setFailure(future.getResult().getException());
      return;
    }
    localChannelReference.sessionNewState(R66FiniteDualStates.VALIDOTHER);
    try {
      ChannelUtils
          .writeAbstractLocalPacket(localChannelReference, valid, false);
    } catch (final OpenR66ProtocolPacketException e) {
      logger.error("Bad Protocol", e);
      localChannelReference.getLocalChannel().close();
      localChannelReference = null;
      host = null;
      valid = null;
      future.setResult(
          new R66Result(e, null, true, ErrorCode.TransferError, null));
      future.setFailure(e);
      return;
    }
    host = null;
    future.awaitOrInterruptible();
    logger
        .info("Request done with " + (future.isSuccess()? "success" : "error"));
    localChannelReference.getLocalChannel().close();
    localChannelReference = null;
  }

  protected static boolean spurgeLog = false;
  protected static Timestamp sstart = null;
  protected static Timestamp sstop = null;
  protected static boolean sclean = false;
  protected static String stohost = null;

  protected static boolean getParams(String[] args) {
    if (args.length < 1) {
      logger.error(_INFO_ARGS);
      return false;
    }
    if (!FileBasedConfiguration
        .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
      logger.error(_INFO_ARGS);
      return false;
    }
    String ssstart = null;
    String ssstop = null;
    for (int i = 1; i < args.length; i++) {
      if (args[i].equalsIgnoreCase("-purge")) {
        spurgeLog = true;
      } else if (args[i].equalsIgnoreCase("-clean")) {
        sclean = true;
      } else if (args[i].equalsIgnoreCase("-start")) {
        i++;
        ssstart = args[i];
      } else if (args[i].equalsIgnoreCase("-stop")) {
        i++;
        ssstop = args[i];
      } else if (args[i].equalsIgnoreCase("-host")) {
        i++;
        stohost = args[i];
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
    return true;
  }

  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(LogExport.class);
    }
    if (!getParams(args)) {
      logger.error("Wrong initialization");
      if (DbConstant.admin != null && DbConstant.admin.isActive()) {
        DbConstant.admin.close();
      }
      System.exit(1);
    }
    final long time1 = System.currentTimeMillis();
    final R66Future future = new R66Future(true);

    Configuration.configuration.pipelineInit();
    final NetworkTransaction networkTransaction = new NetworkTransaction();
    try {
      final LogExport transaction =
          new LogExport(future, spurgeLog, sclean, sstart, sstop,
                        networkTransaction);
      if (stohost != null) {
        try {
          transaction.setHost(new DbHostAuth(stohost));
        } catch (final WaarpDatabaseException e) {
          logger.error(
              "LogExport in     FAILURE since Host is not found: " + stohost,
              e);
          networkTransaction.closeAll();
          System.exit(10);
        }
      } else {
        stohost = Configuration.configuration.getHOST_SSLID();
      }
      transaction.run();
      future.awaitOrInterruptible();
      final long time2 = System.currentTimeMillis();
      final long delay = time2 - time1;
      final R66Result result = future.getResult();
      if (future.isSuccess()) {
        if (result.getCode() == ErrorCode.Warning) {
          logger.warn("WARNED on file:     " + (result.getOther() != null?
              ((ValidPacket) result.getOther()).getSheader() : "no file") +
                      "     delay: " + delay);
        } else {
          logger.warn("SUCCESS on Final file:     " +
                      (result.getOther() != null?
                          ((ValidPacket) result.getOther()).getSheader() :
                          "no file") + "     delay: " + delay);
        }
      } else {
        if (result.getCode() == ErrorCode.Warning) {
          logger.warn("LogExport is     WARNED", future.getCause());
          networkTransaction.closeAll();
          System.exit(result.getCode().ordinal());
        } else {
          logger.error("LogExport in     FAILURE", future.getCause());
          networkTransaction.closeAll();
          System.exit(result.getCode().ordinal());
        }
      }
    } finally {
      networkTransaction.closeAll();
      System.exit(0);
    }
  }

}
