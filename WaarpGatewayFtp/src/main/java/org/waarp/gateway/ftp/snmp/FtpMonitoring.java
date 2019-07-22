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
package org.waarp.gateway.ftp.snmp;

import io.netty.handler.traffic.TrafficCounter;
import org.waarp.common.command.ReplyCode;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.gateway.ftp.database.DbConstant;
import org.waarp.gateway.ftp.database.data.DbTransferLog;
import org.waarp.gateway.ftp.snmp.FtpPrivateMib.MibLevel;
import org.waarp.gateway.ftp.snmp.FtpPrivateMib.WaarpDetailedValuesIndex;
import org.waarp.gateway.ftp.snmp.FtpPrivateMib.WaarpErrorValuesIndex;
import org.waarp.gateway.ftp.snmp.FtpPrivateMib.WaarpGlobalValuesIndex;
import org.waarp.snmp.WaarpSnmpAgent;
import org.waarp.snmp.interf.WaarpInterfaceMonitor;

/**
 * SNMP Monitoring class for FTP Exec
 *
 *
 */
public class FtpMonitoring implements WaarpInterfaceMonitor {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpMonitoring.class);

  public WaarpSnmpAgent agent;

  // global informations
  public long nbNetworkConnection;
  public long secondsRunning;
  public long nbThread;
  public long bandwidthIn;
  public long bandwidthOut;

  // Internal data
  private final DbSession dbSession;
  private final TrafficCounter trafficCounter =
      FileBasedConfiguration.fileBasedConfiguration
          .getFtpInternalConfiguration().getGlobalTrafficShapingHandler()
          .trafficCounter();

  public long nbCountInfoUnknown;
  public long nbCountInfoNotUpdated;
  public long nbCountInfoInterrupted;
  public long nbCountInfoToSubmit;
  public long nbCountInfoError;
  public long nbCountInfoRunning;
  public long nbCountInfoDone;

  public long nbInActiveTransfer;
  public long nbOutActiveTransfer;
  public long lastInActiveTransfer = System.currentTimeMillis();
  public long lastOutActiveTransfer = System.currentTimeMillis();
  public long nbInTotalTransfer;
  public long nbOutTotalTransfer;
  public long nbInErrorTransfer;
  public long nbOutErrorTransfer;

  public long nbCountAllTransfer;

  // Info for other reasons than transfers
  private final long[] reply_info_notransfers =
      new long[WaarpDetailedValuesIndex.reply_350.ordinal() + 1];
  // Error for other reasons than transfers
  private final long[] reply_error_notransfers =
      new long[WaarpErrorValuesIndex.reply_553.ordinal() + 1];

  {
    for (int i = 0; i <= WaarpDetailedValuesIndex.reply_350.ordinal(); i++) {
      reply_info_notransfers[i] = 0;
    }
    for (int i = 0; i <= WaarpErrorValuesIndex.reply_553.ordinal(); i++) {
      reply_error_notransfers[i] = 0;
    }
  }

  // Overall status including past, future and current transfers
  private DbPreparedStatement countInfo;

  // Current situation of all transfers, running or not
  private DbPreparedStatement countInActiveTransfer;
  private DbPreparedStatement countOutActiveTransfer;
  private DbPreparedStatement countInTotalTransfer;
  private DbPreparedStatement countOutTotalTransfer;
  private DbPreparedStatement countInErrorTransfer;
  private DbPreparedStatement countOutErrorTransfer;
  private DbPreparedStatement countAllTransfer;
  // Error Status on all transfers
  private DbPreparedStatement countStatus;

  /**
   * @param session
   */
  public FtpMonitoring(DbSession session) {
    if (session != null) {
      dbSession = session;
    } else {
      dbSession = DbConstant.gatewayAdmin.getSession();
    }
    initialize();
  }

  @Override
  public void setAgent(WaarpSnmpAgent agent) {
    this.agent = agent;
    lastInActiveTransfer = this.agent.getUptimeSystemTime();
    lastOutActiveTransfer = this.agent.getUptimeSystemTime();
  }

  @Override
  public void initialize() {
    logger.debug("Initialize monitoring");
    try {
      // Overall status including past, future and current transfers
      countInfo = DbTransferLog.getCountInfoPrepareStatement(dbSession);
      // Count of Active/All In/Out transfers
      countInActiveTransfer = DbTransferLog
          .getCountInOutRunningPrepareStatement(dbSession, true, true);
      countOutActiveTransfer = DbTransferLog
          .getCountInOutRunningPrepareStatement(dbSession, false, true);
      countInTotalTransfer = DbTransferLog
          .getCountInOutRunningPrepareStatement(dbSession, true, false);
      countOutTotalTransfer = DbTransferLog
          .getCountInOutRunningPrepareStatement(dbSession, false, false);

      countInErrorTransfer =
          DbTransferLog.getCountInOutErrorPrepareStatement(dbSession, true);
      countOutErrorTransfer =
          DbTransferLog.getCountInOutErrorPrepareStatement(dbSession, false);
      // All
      countAllTransfer = DbTransferLog.getCountAllPrepareStatement(dbSession);
      // Error Status on all transfers
      countStatus = DbTransferLog.getCountStatusPrepareStatement(dbSession);
    } catch (final WaarpDatabaseException e) {
    }
  }

  @Override
  public void releaseResources() {
    try {
      logger.debug("Release monitoring");
      // Overall status including past, future and current transfers
      countInfo.realClose();

      countInActiveTransfer.realClose();
      countOutActiveTransfer.realClose();
      countInTotalTransfer.realClose();
      countOutTotalTransfer.realClose();
      countInErrorTransfer.realClose();
      countOutErrorTransfer.realClose();

      countAllTransfer.realClose();
      // Error Status on all transfers
      countStatus.realClose();
    } catch (final NullPointerException e) {
    }
  }

  private static final int ref421 =
      ReplyCode.REPLY_421_SERVICE_NOT_AVAILABLE_CLOSING_CONTROL_CONNECTION
          .ordinal();

  /**
   * Update the reply code counter for other operations than a transfer
   *
   * @param code
   */
  public void updateCodeNoTransfer(ReplyCode code) {
    int i = code.ordinal();
    if (i >= ref421) {
      i -= ref421;
      reply_error_notransfers[i]++;
    } else {
      reply_info_notransfers[i]++;
    }
  }

  /**
   * Update the last InBound connection time
   */
  public void updateLastInBound() {
    lastInActiveTransfer = System.currentTimeMillis();
  }

  /**
   * Update the last OutBound connection time
   */
  public void updateLastOutBand() {
    lastOutActiveTransfer = System.currentTimeMillis();
  }

  /**
   * Update the value for one particular MIB entry
   *
   * @param type
   * @param entry
   */
  public void run(int type, int entry) {
    final long nbMs =
        FileBasedConfiguration.fileBasedConfiguration.agentSnmp.getUptime() +
        100;
    final MibLevel level = MibLevel.values()[type];
    switch (level) {
      case globalInfo:// Global
        if (((FtpPrivateMib) agent.getMib()).rowGlobal != null) {
          run(nbMs, WaarpGlobalValuesIndex.values()[entry]);
        }
        return;
      case detailedInfo:// Detailed
        if (((FtpPrivateMib) agent.getMib()).rowDetailed != null) {
          run(nbMs, WaarpDetailedValuesIndex.values()[entry]);
        }
        return;
      case errorInfo:// Error
        if (((FtpPrivateMib) agent.getMib()).rowError != null) {
          run(nbMs, WaarpErrorValuesIndex.values()[entry]);
        }
        return;
      case staticInfo:
        break;
      case trapInfo:
        break;
      default:
        break;
    }
  }

  /**
   * Update a value in Global MIB part
   *
   * @param rank
   * @param value
   */
  protected void updateGlobalValue(int rank, long value) {
    ((FtpPrivateMib) agent.getMib()).rowGlobal.setValue(rank, value);
  }

  /**
   * Update a value in Detailed MIB part
   *
   * @param rank
   * @param value
   */
  protected void updateDetailedValue(int rank, long value) {
    ((FtpPrivateMib) agent.getMib()).rowDetailed.setValue(rank, value);
  }

  /**
   * Update a value in Error MIB part
   *
   * @param rank
   * @param value
   */
  protected void updateErrorValue(int rank, long value) {
    ((FtpPrivateMib) agent.getMib()).rowError.setValue(rank, value);
  }

  /**
   * Update a value in Global MIB part
   *
   * @param nbMs
   * @param entry
   */
  protected void run(long nbMs, WaarpGlobalValuesIndex entry) {
    synchronized (trafficCounter) {
      long val = 0;
      final long limitDate = System.currentTimeMillis() - nbMs;
      // Global
      try {
        switch (entry) {
          case applUptime:
            return;
          case applOperStatus:
            return;
          case applLastChange:
            return;
          case applInboundAssociations:
            DbTransferLog
                .finishSelectOrCountPrepareStatement(countInActiveTransfer,
                                                     limitDate);
            nbInActiveTransfer = DbTransferLog
                .getResultCountPrepareStatement(countInActiveTransfer);
            updateGlobalValue(entry.ordinal(), nbInActiveTransfer);
            return;
          case applOutboundAssociations:
            DbTransferLog
                .finishSelectOrCountPrepareStatement(countOutActiveTransfer,
                                                     limitDate);
            nbOutActiveTransfer = DbTransferLog
                .getResultCountPrepareStatement(countOutActiveTransfer);
            updateGlobalValue(entry.ordinal(), nbOutActiveTransfer);
            return;
          case applAccumInboundAssociations:
            DbTransferLog
                .finishSelectOrCountPrepareStatement(countInTotalTransfer,
                                                     limitDate);
            nbInTotalTransfer = DbTransferLog
                .getResultCountPrepareStatement(countInTotalTransfer);
            updateGlobalValue(entry.ordinal(), nbInTotalTransfer);
            return;
          case applAccumOutboundAssociations:
            DbTransferLog
                .finishSelectOrCountPrepareStatement(countOutTotalTransfer,
                                                     limitDate);
            nbOutTotalTransfer = DbTransferLog
                .getResultCountPrepareStatement(countOutTotalTransfer);
            updateGlobalValue(entry.ordinal(), nbOutTotalTransfer);
            return;
          case applLastInboundActivity:
            val = (lastInActiveTransfer - agent.getUptimeSystemTime()) / 10;
            if (val < 0) {
              val = 0;
            }
            updateGlobalValue(entry.ordinal(), val);
            return;
          case applLastOutboundActivity:
            val = (lastOutActiveTransfer - agent.getUptimeSystemTime()) / 10;
            if (val < 0) {
              val = 0;
            }
            updateGlobalValue(entry.ordinal(), val);
            return;
          case applRejectedInboundAssociations:
            DbTransferLog
                .finishSelectOrCountPrepareStatement(countInErrorTransfer,
                                                     limitDate);
            nbInErrorTransfer = DbTransferLog
                .getResultCountPrepareStatement(countInErrorTransfer);
            updateGlobalValue(entry.ordinal(), nbInErrorTransfer);
            return;
          case applFailedOutboundAssociations:
            DbTransferLog
                .finishSelectOrCountPrepareStatement(countOutErrorTransfer,
                                                     limitDate);
            nbOutErrorTransfer = DbTransferLog
                .getResultCountPrepareStatement(countOutErrorTransfer);
            updateGlobalValue(entry.ordinal(), nbOutErrorTransfer);
            return;
          case applInboundBandwidthKBS:
            val = trafficCounter.lastReadThroughput() >> 10;// B/s -> KB/s
            updateGlobalValue(entry.ordinal(), val);
            return;
          case applOutboundBandwidthKBS:
            val = trafficCounter.lastWriteThroughput() >> 10;
            updateGlobalValue(entry.ordinal(), val);
            return;
          case nbInfoUnknown:
            nbCountInfoUnknown = DbTransferLog
                .getResultCountPrepareStatement(countInfo, UpdatedInfo.UNKNOWN,
                                                limitDate);
            updateGlobalValue(entry.ordinal(), nbCountInfoUnknown);
            return;
          case nbInfoNotUpdated:
            nbCountInfoNotUpdated = DbTransferLog
                .getResultCountPrepareStatement(countInfo,
                                                UpdatedInfo.NOTUPDATED,
                                                limitDate);
            updateGlobalValue(entry.ordinal(), nbCountInfoNotUpdated);
            return;
          case nbInfoInterrupted:
            nbCountInfoInterrupted = DbTransferLog
                .getResultCountPrepareStatement(countInfo,
                                                UpdatedInfo.INTERRUPTED,
                                                limitDate);
            updateGlobalValue(entry.ordinal(), nbCountInfoInterrupted);
            return;
          case nbInfoToSubmit:
            nbCountInfoToSubmit = DbTransferLog
                .getResultCountPrepareStatement(countInfo, UpdatedInfo.TOSUBMIT,
                                                limitDate);
            updateGlobalValue(entry.ordinal(), nbCountInfoToSubmit);
            return;
          case nbInfoError:
            nbCountInfoError = DbTransferLog
                .getResultCountPrepareStatement(countInfo, UpdatedInfo.INERROR,
                                                limitDate);
            updateGlobalValue(entry.ordinal(), nbCountInfoError);
            return;
          case nbInfoRunning:
            nbCountInfoRunning = DbTransferLog
                .getResultCountPrepareStatement(countInfo, UpdatedInfo.RUNNING,
                                                limitDate);
            updateGlobalValue(entry.ordinal(), nbCountInfoRunning);
            return;
          case nbInfoDone:
            nbCountInfoDone = DbTransferLog
                .getResultCountPrepareStatement(countInfo, UpdatedInfo.DONE,
                                                limitDate);
            updateGlobalValue(entry.ordinal(), nbCountInfoDone);
            return;
          case nbAllTransfer:
            DbTransferLog.finishSelectOrCountPrepareStatement(countAllTransfer,
                                                              limitDate);
            nbCountAllTransfer =
                DbTransferLog.getResultCountPrepareStatement(countAllTransfer);
            updateGlobalValue(entry.ordinal(), nbCountAllTransfer);
            return;
          case memoryTotal:
            return;
          case memoryFree:
            return;
          case memoryUsed:
            return;
          case nbThreads:
            nbThread = Thread.activeCount();
            updateGlobalValue(entry.ordinal(), nbThread);
            return;
          case nbNetworkConnection:
            nbNetworkConnection = FileBasedConfiguration.fileBasedConfiguration
                .getFtpInternalConfiguration().getNumberSessions();
            updateGlobalValue(entry.ordinal(), nbNetworkConnection);
            return;
        }
      } catch (final WaarpDatabaseNoConnectionException e) {
      } catch (final WaarpDatabaseSqlException e) {
      }
    }
  }

  /**
   * Update a value in Detailed MIB part
   *
   * @param nbMs
   * @param entry
   */
  protected void run(long nbMs, WaarpDetailedValuesIndex entry) {
    synchronized (trafficCounter) {
      final long limitDate = System.currentTimeMillis() - nbMs;
      // Detailed
      final long value = DbTransferLog
          .getResultCountPrepareStatement(countStatus, entry.code, limitDate);
      updateDetailedValue(entry.ordinal(),
                          value + reply_info_notransfers[entry.ordinal()]);
    }
  }

  /**
   * Update a value in Error MIB part
   *
   * @param nbMs
   * @param entry
   */
  protected void run(long nbMs, WaarpErrorValuesIndex entry) {
    synchronized (trafficCounter) {
      final long limitDate = System.currentTimeMillis() - nbMs;
      // Error
      final long value = DbTransferLog
          .getResultCountPrepareStatement(countStatus, entry.code, limitDate);
      updateErrorValue(entry.ordinal(),
                       value + reply_error_notransfers[entry.ordinal()]);
    }
  }

}
