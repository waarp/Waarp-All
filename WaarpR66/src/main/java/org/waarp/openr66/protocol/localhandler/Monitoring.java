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
package org.waarp.openr66.protocol.localhandler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.traffic.TrafficCounter;
import org.joda.time.DateTime;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.commander.CommanderNoDb;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.database.data.DbTaskRunner.TASKSTEP;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.snmp.R66PrivateMib;
import org.waarp.snmp.WaarpSnmpAgent;
import org.waarp.snmp.interf.WaarpInterfaceMonitor;
import org.waarp.snmp.r66.WaarpPrivateMib.MibLevel;
import org.waarp.snmp.r66.WaarpPrivateMib.WaarpDetailedValuesIndex;
import org.waarp.snmp.r66.WaarpPrivateMib.WaarpErrorValuesIndex;
import org.waarp.snmp.r66.WaarpPrivateMib.WaarpGlobalValuesIndex;

/**
 * Monitoring class as an helper to get values of interest. Also used by SNMP
 * support.
 *
 *
 */
public class Monitoring implements WaarpInterfaceMonitor {
  /**
   * Internal Logger
   */
  private static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(Monitoring.class);

  public WaarpSnmpAgent agent;

  // global informations
  public long nbNetworkConnection = 0;
  public long secondsRunning = 0;
  public long nbThread = 0;
  public long bandwidthIn = 0;
  public long bandwidthOut = 0;

  // Internal data
  private final long startMonitor = System.currentTimeMillis();
  private long pastLimit = 0;
  private long currentLimit = 0;
  private long minimalDelay = 0;
  private long lastTry = 0;
  private DbSession dbSession = null;
  private final TrafficCounter trafficCounter =
      Configuration.configuration.getGlobalTrafficShapingHandler()
                                 .trafficCounter();

  // Overall status including past, future and current transfers
  private DbPreparedStatement countInfo = null;

  // Current situation of all transfers, running or not
  private DbPreparedStatement countInActiveTransfer = null;
  private DbPreparedStatement countOutActiveTransfer = null;
  private DbPreparedStatement countInTotalTransfer = null;
  private DbPreparedStatement countOutTotalTransfer = null;
  private DbPreparedStatement countInErrorTransfer = null;
  private DbPreparedStatement countOutErrorTransfer = null;
  private DbPreparedStatement countStepAllTransfer = null;
  private DbPreparedStatement countStepNotask = null;
  private DbPreparedStatement countStepPretask = null;
  private DbPreparedStatement countStepTransfer = null;
  private DbPreparedStatement countStepPosttask = null;
  private DbPreparedStatement countStepAllDone = null;
  private DbPreparedStatement countStepError = null;

  // First on Running Transfers only
  private DbPreparedStatement countAllRunningStep = null;
  private DbPreparedStatement countRunningStep = null;
  private DbPreparedStatement countInitOkStep = null;
  private DbPreparedStatement countPreProcessingOkStep = null;
  private DbPreparedStatement countTransferOkStep = null;
  private DbPreparedStatement countPostProcessingOkStep = null;
  private DbPreparedStatement countCompleteOkStep = null;

  // Error Status on all transfers
  private DbPreparedStatement countStatus = null;

  // Overall status including past, future and current transfers
  public long nbCountInfoUnknown = 0;
  public long nbCountInfoNotUpdated = 0;
  public long nbCountInfoInterrupted = 0;
  public long nbCountInfoToSubmit = 0;
  public long nbCountInfoError = 0;
  public long nbCountInfoRunning = 0;
  public long nbCountInfoDone = 0;

  public long nbInActiveTransfer = 0;
  public long nbOutActiveTransfer = 0;
  public long lastInActiveTransfer = System.currentTimeMillis();
  public long lastOutActiveTransfer = System.currentTimeMillis();
  public long nbInTotalTransfer = 0;
  public long nbOutTotalTransfer = 0;
  public long nbInErrorTransfer = 0;
  public long nbOutErrorTransfer = 0;

  // Current situation of all transfers, running or not
  public long nbCountStepAllTransfer = 0;
  public long nbCountStepNotask = 0;
  public long nbCountStepPretask = 0;
  public long nbCountStepTransfer = 0;
  public long nbCountStepPosttask = 0;
  public long nbCountStepAllDone = 0;
  public long nbCountStepError = 0;

  // First on Running Transfers only
  public long nbCountAllRunningStep = 0;
  public long nbCountRunningStep = 0;
  public long nbCountInitOkStep = 0;
  public long nbCountPreProcessingOkStep = 0;
  public long nbCountTransferOkStep = 0;
  public long nbCountPostProcessingOkStep = 0;
  public long nbCountCompleteOkStep = 0;

  // Error Status on all transfers
  public long nbCountStatusConnectionImpossible = 0;
  public long nbCountStatusServerOverloaded = 0;
  public long nbCountStatusBadAuthent = 0;
  public long nbCountStatusExternalOp = 0;
  public long nbCountStatusTransferError = 0;
  public long nbCountStatusMD5Error = 0;
  public long nbCountStatusDisconnection = 0;
  public long nbCountStatusFinalOp = 0;
  public long nbCountStatusUnimplemented = 0;
  public long nbCountStatusInternal = 0;
  public long nbCountStatusWarning = 0;
  public long nbCountStatusQueryAlreadyFinished = 0;
  public long nbCountStatusQueryStillRunning = 0;
  public long nbCountStatusNotKnownHost = 0;
  public long nbCountStatusQueryRemotelyUnknown = 0;
  public long nbCountStatusCommandNotFound = 0;
  public long nbCountStatusPassThroughMode = 0;
  public long nbCountStatusRemoteShutdown = 0;
  public long nbCountStatusShutdown = 0;
  public long nbCountStatusRemoteError = 0;
  public long nbCountStatusStopped = 0;
  public long nbCountStatusCanceled = 0;
  public long nbCountStatusFileNotFound = 0;
  public long nbCountStatusUnknown = 0;

  /**
   * @param pastLimit
   * @param minimalDelay
   * @param session
   */
  public Monitoring(long pastLimit, long minimalDelay, DbSession session) {
    this.pastLimit = pastLimit;
    this.minimalDelay = minimalDelay;
    if (session != null) {
      dbSession = session;
    } else {
      if (DbConstant.admin.isActive()) {
        try {
          dbSession = new DbSession(DbConstant.admin, false);
        } catch (final WaarpDatabaseNoConnectionException e) {
          dbSession = DbConstant.admin.getSession();
        }
      } else {
        dbSession = DbConstant.admin.getSession();
      }
    }
    initialize();
  }

  /**
   * Initialize the Db Requests after constructor or after use of
   * releaseResources
   */
  @Override
  public void initialize() {
    if (dbSession == null || dbSession.isDisActive()) {
      logger.warn("Cannot Initialize monitoring");
      return;
    }
    try {
      logger.debug("Initialize monitoring");
      // Overall status including past, future and current transfers
      countInfo = DbTaskRunner.getCountInfoPrepareStatement(dbSession);
      // Count of Active/All In/Out transfers
      countInActiveTransfer = DbTaskRunner
          .getCountInOutRunningPrepareStatement(dbSession, true, true);
      countOutActiveTransfer = DbTaskRunner
          .getCountInOutRunningPrepareStatement(dbSession, false, true);
      countInTotalTransfer = DbTaskRunner
          .getCountInOutRunningPrepareStatement(dbSession, true, false);
      countOutTotalTransfer = DbTaskRunner
          .getCountInOutRunningPrepareStatement(dbSession, false, false);

      countInErrorTransfer =
          DbTaskRunner.getCountInOutErrorPrepareStatement(dbSession, true);
      countOutErrorTransfer =
          DbTaskRunner.getCountInOutErrorPrepareStatement(dbSession, false);

      // Current situation of all transfers, running or not
      countStepAllTransfer =
          DbTaskRunner.getCountStepPrepareStatement(dbSession, null);
      countStepNotask =
          DbTaskRunner.getCountStepPrepareStatement(dbSession, TASKSTEP.NOTASK);
      countStepPretask = DbTaskRunner
          .getCountStepPrepareStatement(dbSession, TASKSTEP.PRETASK);
      countStepTransfer = DbTaskRunner
          .getCountStepPrepareStatement(dbSession, TASKSTEP.TRANSFERTASK);
      countStepPosttask = DbTaskRunner
          .getCountStepPrepareStatement(dbSession, TASKSTEP.POSTTASK);
      countStepAllDone = DbTaskRunner
          .getCountStepPrepareStatement(dbSession, TASKSTEP.ALLDONETASK);
      countStepError = DbTaskRunner
          .getCountStepPrepareStatement(dbSession, TASKSTEP.ERRORTASK);

      // First on Running Transfers only
      countAllRunningStep =
          DbTaskRunner.getCountStatusRunningPrepareStatement(dbSession, null);
      countRunningStep = DbTaskRunner
          .getCountStatusRunningPrepareStatement(dbSession, ErrorCode.Running);
      countInitOkStep = DbTaskRunner
          .getCountStatusRunningPrepareStatement(dbSession, ErrorCode.InitOk);
      countPreProcessingOkStep = DbTaskRunner
          .getCountStatusRunningPrepareStatement(dbSession,
                                                 ErrorCode.PreProcessingOk);
      countTransferOkStep = DbTaskRunner
          .getCountStatusRunningPrepareStatement(dbSession,
                                                 ErrorCode.TransferOk);
      countPostProcessingOkStep = DbTaskRunner
          .getCountStatusRunningPrepareStatement(dbSession,
                                                 ErrorCode.PostProcessingOk);
      countCompleteOkStep = DbTaskRunner
          .getCountStatusRunningPrepareStatement(dbSession,
                                                 ErrorCode.CompleteOk);

      // Error Status on all transfers
      countStatus = DbTaskRunner.getCountStatusPrepareStatement(dbSession);
    } catch (final WaarpDatabaseNoConnectionException e) {
    } catch (final WaarpDatabaseSqlException e) {
    }
  }

  /**
   * Release all Db Requests
   */
  @Override
  public void releaseResources() {
    if (dbSession == null || dbSession.isDisActive()) {
      return;
    }
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

      // Current situation of all transfers, running or not
      countStepAllTransfer.realClose();
      countStepNotask.realClose();
      countStepPretask.realClose();
      countStepTransfer.realClose();
      countStepPosttask.realClose();
      countStepAllDone.realClose();
      countStepError.realClose();

      // First on Running Transfers only
      countAllRunningStep.realClose();
      countRunningStep.realClose();
      countInitOkStep.realClose();
      countPreProcessingOkStep.realClose();
      countTransferOkStep.realClose();
      countPostProcessingOkStep.realClose();
      countCompleteOkStep.realClose();

      // Error Status on all transfers
      countStatus.realClose();
    } catch (final Exception e) {
    }
    if (!dbSession.equals(DbConstant.admin.getSession())) {
      if (DbAdmin.getNbConnection() > 0) {
        dbSession.forceDisconnect();
      }
      dbSession = null;
    }
  }

  /**
   * @return the last Time in ms of the execution
   */
  public long lastRunTimeMs() {
    return lastTry;
  }

  /**
   * Default execution of testing with default pastLimit
   */
  public void run() {
    this.run(-1, false);
  }

  /**
   * @param nbSecond as specific PastLimit
   */
  public void run(long nbSecond) {
    this.run(nbSecond, false);
  }

  /**
   * Default execution of testing with default pastLimit
   *
   * @param detail as to get detailed information
   */
  public void run(boolean detail) {
    this.run(-1, detail);
  }

  /**
   * @return False if too early, else return True
   */
  private boolean reCompute() {
    final long limitDate = System.currentTimeMillis();
    if (limitDate < lastTry + minimalDelay) {
      // too early
      return false;
    }
    lastTry = limitDate;
    return true;
  }

  /**
   * @param nbSecond as specific PastLimit
   * @param detail as to get detailed information
   */
  public void run(long nbSecond, boolean detail) {
    synchronized (trafficCounter) {
      long limitDate = System.currentTimeMillis();
      long nbMs = pastLimit;
      if (nbSecond <= 0) {
        nbMs = pastLimit;
      } else {
        nbMs = nbSecond * 1000;
      }
      if (dbSession != null && dbSession.isDisActive()) {
        dbSession.checkConnectionNoException();
      }
      if (dbSession == null || dbSession.isDisActive()) {
        nbNetworkConnection =
            Configuration.configuration.getHttpChannelGroup().size() +
            Configuration.configuration.getServerChannelGroup().size();
        bandwidthIn = trafficCounter.lastReadThroughput() >> 7;// B/s -> Kb/s
        bandwidthOut = trafficCounter.lastWriteThroughput() >> 7;
        nbThread = Thread.activeCount();
        secondsRunning = (limitDate - startMonitor) / 1000;

        if (!reCompute()) {
          // too early
          return;
        }
        limitDate -= nbMs;
        currentLimit = limitDate;
        // Update value
        // Overall status including past, future and current transfers
        nbCountInfoToSubmit = CommanderNoDb.todoList.size();
        if (Configuration.configuration.getInternalRunner() != null) {
          nbCountInfoRunning = Configuration.configuration.getInternalRunner()
                                                          .nbInternalRunner();
        } else {
          nbCountInfoRunning =
              Configuration.configuration.getServerChannelGroup().size();
        }
        // Current situation of all transfers, running or not
        nbCountAllRunningStep = nbCountInfoRunning;
      } else {
        nbNetworkConnection = DbAdmin.getNbConnection();
        bandwidthIn = trafficCounter.lastReadThroughput() >> 7;// B/s -> Kb/s
        bandwidthOut = trafficCounter.lastWriteThroughput() >> 7;
        nbThread = Thread.activeCount();
        secondsRunning = (limitDate - startMonitor) / 1000;

        if (!reCompute()) {
          // too early
          return;
        }
        limitDate -= nbMs;
        currentLimit = limitDate;
        // Update value
        try {
          // Overall status including past, future and current transfers
          nbCountInfoUnknown = DbTaskRunner
              .getResultCountPrepareStatement(countInfo, UpdatedInfo.UNKNOWN,
                                              limitDate);
          nbCountInfoNotUpdated = DbTaskRunner
              .getResultCountPrepareStatement(countInfo, UpdatedInfo.NOTUPDATED,
                                              limitDate);
          nbCountInfoInterrupted = DbTaskRunner
              .getResultCountPrepareStatement(countInfo,
                                              UpdatedInfo.INTERRUPTED,
                                              limitDate);
          nbCountInfoToSubmit = DbTaskRunner
              .getResultCountPrepareStatement(countInfo, UpdatedInfo.TOSUBMIT,
                                              limitDate);
          nbCountInfoError = DbTaskRunner
              .getResultCountPrepareStatement(countInfo, UpdatedInfo.INERROR,
                                              limitDate);
          nbCountInfoRunning = DbTaskRunner
              .getResultCountPrepareStatement(countInfo, UpdatedInfo.RUNNING,
                                              limitDate);
          nbCountInfoDone = DbTaskRunner
              .getResultCountPrepareStatement(countInfo, UpdatedInfo.DONE,
                                              limitDate);

          // Current situation of all transfers, running or not
          DbTaskRunner
              .finishSelectOrCountPrepareStatement(countInActiveTransfer,
                                                   limitDate);
          nbInActiveTransfer = DbTaskRunner
              .getResultCountPrepareStatement(countInActiveTransfer);
          DbTaskRunner
              .finishSelectOrCountPrepareStatement(countOutActiveTransfer,
                                                   limitDate);
          nbOutActiveTransfer = DbTaskRunner
              .getResultCountPrepareStatement(countOutActiveTransfer);
          DbTaskRunner.finishSelectOrCountPrepareStatement(countInTotalTransfer,
                                                           limitDate);
          nbInTotalTransfer =
              DbTaskRunner.getResultCountPrepareStatement(countInTotalTransfer);
          DbTaskRunner
              .finishSelectOrCountPrepareStatement(countOutTotalTransfer,
                                                   limitDate);
          nbOutTotalTransfer = DbTaskRunner
              .getResultCountPrepareStatement(countOutTotalTransfer);

          DbTaskRunner
              .finishSelectOrCountPrepareStatement(countOutErrorTransfer,
                                                   limitDate);
          nbOutErrorTransfer = DbTaskRunner
              .getResultCountPrepareStatement(countOutErrorTransfer);
          DbTaskRunner.finishSelectOrCountPrepareStatement(countInErrorTransfer,
                                                           limitDate);
          nbInErrorTransfer =
              DbTaskRunner.getResultCountPrepareStatement(countInErrorTransfer);

          DbTaskRunner.finishSelectOrCountPrepareStatement(countStepAllTransfer,
                                                           limitDate);
          nbCountStepAllTransfer =
              DbTaskRunner.getResultCountPrepareStatement(countStepAllTransfer);
          DbTaskRunner
              .finishSelectOrCountPrepareStatement(countStepNotask, limitDate);
          nbCountStepNotask =
              DbTaskRunner.getResultCountPrepareStatement(countStepNotask);
          DbTaskRunner
              .finishSelectOrCountPrepareStatement(countStepPretask, limitDate);
          nbCountStepPretask =
              DbTaskRunner.getResultCountPrepareStatement(countStepPretask);
          DbTaskRunner.finishSelectOrCountPrepareStatement(countStepTransfer,
                                                           limitDate);
          nbCountStepTransfer =
              DbTaskRunner.getResultCountPrepareStatement(countStepTransfer);
          DbTaskRunner.finishSelectOrCountPrepareStatement(countStepPosttask,
                                                           limitDate);
          nbCountStepPosttask =
              DbTaskRunner.getResultCountPrepareStatement(countStepPosttask);
          DbTaskRunner
              .finishSelectOrCountPrepareStatement(countStepAllDone, limitDate);
          nbCountStepAllDone =
              DbTaskRunner.getResultCountPrepareStatement(countStepAllDone);
          DbTaskRunner
              .finishSelectOrCountPrepareStatement(countStepError, limitDate);
          nbCountStepError =
              DbTaskRunner.getResultCountPrepareStatement(countStepError);

          DbTaskRunner.finishSelectOrCountPrepareStatement(countAllRunningStep,
                                                           limitDate);
          nbCountAllRunningStep =
              DbTaskRunner.getResultCountPrepareStatement(countAllRunningStep);

          if (detail) {
            // First on Running Transfers only
            DbTaskRunner.finishSelectOrCountPrepareStatement(countRunningStep,
                                                             limitDate);
            nbCountRunningStep =
                DbTaskRunner.getResultCountPrepareStatement(countRunningStep);
            DbTaskRunner.finishSelectOrCountPrepareStatement(countInitOkStep,
                                                             limitDate);
            nbCountInitOkStep =
                DbTaskRunner.getResultCountPrepareStatement(countInitOkStep);
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countPreProcessingOkStep,
                                                     limitDate);
            nbCountPreProcessingOkStep = DbTaskRunner
                .getResultCountPrepareStatement(countPreProcessingOkStep);
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countTransferOkStep,
                                                     limitDate);
            nbCountTransferOkStep = DbTaskRunner
                .getResultCountPrepareStatement(countTransferOkStep);
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countPostProcessingOkStep,
                                                     limitDate);
            nbCountPostProcessingOkStep = DbTaskRunner
                .getResultCountPrepareStatement(countPostProcessingOkStep);
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countCompleteOkStep,
                                                     limitDate);
            nbCountCompleteOkStep = DbTaskRunner
                .getResultCountPrepareStatement(countCompleteOkStep);

            // Error Status on all transfers
            nbCountStatusConnectionImpossible = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.ConnectionImpossible,
                                                limitDate);
            nbCountStatusServerOverloaded = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.ServerOverloaded,
                                                limitDate);
            nbCountStatusBadAuthent = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.BadAuthent,
                                                limitDate);
            nbCountStatusExternalOp = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.ExternalOp,
                                                limitDate);
            nbCountStatusTransferError = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.TransferError,
                                                limitDate);
            nbCountStatusMD5Error = DbTaskRunner
                .getResultCountPrepareStatement(countStatus, ErrorCode.MD5Error,
                                                limitDate);
            nbCountStatusDisconnection = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.Disconnection,
                                                limitDate);
            nbCountStatusFinalOp = DbTaskRunner
                .getResultCountPrepareStatement(countStatus, ErrorCode.FinalOp,
                                                limitDate);
            nbCountStatusUnimplemented = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.Unimplemented,
                                                limitDate);
            nbCountStatusInternal = DbTaskRunner
                .getResultCountPrepareStatement(countStatus, ErrorCode.Internal,
                                                limitDate);
            nbCountStatusWarning = DbTaskRunner
                .getResultCountPrepareStatement(countStatus, ErrorCode.Warning,
                                                limitDate);
            nbCountStatusQueryAlreadyFinished = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.QueryAlreadyFinished,
                                                limitDate);
            nbCountStatusQueryStillRunning = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.QueryStillRunning,
                                                limitDate);
            nbCountStatusNotKnownHost = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.NotKnownHost,
                                                limitDate);
            nbCountStatusQueryRemotelyUnknown = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.QueryRemotelyUnknown,
                                                limitDate);
            nbCountStatusCommandNotFound = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.CommandNotFound,
                                                limitDate);
            nbCountStatusPassThroughMode = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.PassThroughMode,
                                                limitDate);
            nbCountStatusRemoteShutdown = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.RemoteShutdown,
                                                limitDate);
            nbCountStatusShutdown = DbTaskRunner
                .getResultCountPrepareStatement(countStatus, ErrorCode.Shutdown,
                                                limitDate);
            nbCountStatusRemoteError = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.RemoteError,
                                                limitDate);
            nbCountStatusStopped = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.StoppedTransfer,
                                                limitDate);
            nbCountStatusCanceled = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.CanceledTransfer,
                                                limitDate);
            nbCountStatusFileNotFound = DbTaskRunner
                .getResultCountPrepareStatement(countStatus,
                                                ErrorCode.FileNotFound,
                                                limitDate);
            nbCountStatusUnknown = DbTaskRunner
                .getResultCountPrepareStatement(countStatus, ErrorCode.Unknown,
                                                limitDate);
          }
        } catch (final WaarpDatabaseNoConnectionException e) {
        } catch (final WaarpDatabaseSqlException e) {
        }
      }
    }
  }

  /**
   * @param detail
   *
   * @return The XML representation of the current status
   */
  public String exportXml(boolean detail) {
    final StringBuilder builder = new StringBuilder("<STATUS>")
        // Global Informations
        .append("<HostID>").append(Configuration.configuration.getHOST_ID())
        .append("</HostID>").append("<Date>").append(new DateTime().toString())
        .append("</Date>").append("<LastRun>")
        .append(new DateTime(lastTry).toString()).append("</LastRun>")
        .append("<FromDate>").append(new DateTime(currentLimit).toString())
        .append("</FromDate>").append("<SecondsRunning>").append(secondsRunning)
        .append("</SecondsRunning>").append("<NetworkConnections>")
        .append(nbNetworkConnection).append("</NetworkConnections>")
        .append("<NbThreads>").append(nbThread).append("</NbThreads>")
        .append("<InBandwidth>").append(bandwidthIn).append("</InBandwidth>")
        .append("<OutBandwidth>").append(bandwidthOut).append("</OutBandwidth>")

        // Overall status including past, future and current transfers
        .append("<OVERALL>").append("<AllTransfer>")
        .append(nbCountStepAllTransfer).append("</AllTransfer>")
        .append("<Unknown>").append(nbCountInfoUnknown).append("</Unknown>")
        .append("<NotUpdated>").append(nbCountInfoNotUpdated)
        .append("</NotUpdated>").append("<Interrupted>")
        .append(nbCountInfoInterrupted).append("</Interrupted>")
        .append("<ToSubmit>").append(nbCountInfoToSubmit).append("</ToSubmit>")
        .append("<Error>").append(nbCountInfoError).append("</Error>")
        .append("<Running>").append(nbCountInfoRunning).append("</Running>")
        .append("<Done>").append(nbCountInfoDone).append("</Done>")
        .append("<InRunning>").append(nbInActiveTransfer).append("</InRunning>")
        .append("<OutRunning>").append(nbOutActiveTransfer)
        .append("</OutRunning>").append("<LastInRunning>")
        .append(new DateTime(lastInActiveTransfer).toString())
        .append("</LastInRunning>").append("<LastOutRunning>")
        .append(new DateTime(lastOutActiveTransfer).toString())
        .append("</LastOutRunning>").append("<InAll>").append(nbInTotalTransfer)
        .append("</InAll>").append("<OutAll>").append(nbOutTotalTransfer)
        .append("</OutAll>").append("<InError>").append(nbInErrorTransfer)
        .append("</InError>").append("<OutError>").append(nbOutErrorTransfer)
        .append("</OutError>").append("</OVERALL>")

        // Current situation of all transfers, running or not
        .append("<STEPS>").append("<Notask>").append(nbCountStepNotask)
        .append("</Notask>").append("<Pretask>").append(nbCountStepPretask)
        .append("</Pretask>").append("<Transfer>").append(nbCountStepTransfer)
        .append("</Transfer>").append("<Posttask>").append(nbCountStepPosttask)
        .append("</Posttask>").append("<AllDone>").append(nbCountStepAllDone)
        .append("</AllDone>").append("<Error>").append(nbCountStepError)
        .append("</Error>").append("</STEPS>")

        // On Running Transfers only
        .append("<RUNNINGSTEPS>").append("<AllRunning>")
        .append(nbCountAllRunningStep).append("</AllRunning>");
    if (detail) {
      builder.append("<Running>").append(nbCountRunningStep)
             .append("</Running>").append("<InitOk>").append(nbCountInitOkStep)
             .append("</InitOk>").append("<PreProcessingOk>")
             .append(nbCountPreProcessingOkStep).append("</PreProcessingOk>")
             .append("<TransferOk>").append(nbCountTransferOkStep)
             .append("</TransferOk>").append("<PostProcessingOk>")
             .append(nbCountPostProcessingOkStep).append("</PostProcessingOk>")
             .append("<CompleteOk>").append(nbCountCompleteOkStep)
             .append("</CompleteOk>");
    }
    builder.append("</RUNNINGSTEPS>");

    if (detail) {
      // Error Status on all transfers
      builder.append("<ERRORTYPES>").append("<ConnectionImpossible>")
             .append(nbCountStatusConnectionImpossible)
             .append("</ConnectionImpossible>").append("<ServerOverloaded>")
             .append(nbCountStatusServerOverloaded)
             .append("</ServerOverloaded>").append("<BadAuthent>")
             .append(nbCountStatusBadAuthent).append("</BadAuthent>")
             .append("<ExternalOp>").append(nbCountStatusExternalOp)
             .append("</ExternalOp>").append("<TransferError>")
             .append(nbCountStatusTransferError).append("</TransferError>")
             .append("<MD5Error>").append(nbCountStatusMD5Error)
             .append("</MD5Error>").append("<Disconnection>")
             .append(nbCountStatusDisconnection).append("</Disconnection>")
             .append("<FinalOp>").append(nbCountStatusFinalOp)
             .append("</FinalOp>").append("<Unimplemented>")
             .append(nbCountStatusUnimplemented).append("</Unimplemented>")
             .append("<Internal>").append(nbCountStatusInternal)
             .append("</Internal>").append("<Warning>")
             .append(nbCountStatusWarning).append("</Warning>")
             .append("<QueryAlreadyFinished>")
             .append(nbCountStatusQueryAlreadyFinished)
             .append("</QueryAlreadyFinished>").append("<QueryStillRunning>")
             .append(nbCountStatusQueryStillRunning)
             .append("</QueryStillRunning>").append("<KnownHost>")
             .append(nbCountStatusNotKnownHost).append("</KnownHost>")
             .append("<RemotelyUnknown>")
             .append(nbCountStatusQueryRemotelyUnknown)
             .append("</RemotelyUnknown>").append("<CommandNotFound>")
             .append(nbCountStatusCommandNotFound).append("</CommandNotFound>")
             .append("<PassThroughMode>").append(nbCountStatusPassThroughMode)
             .append("</PassThroughMode>").append("<RemoteShutdown>")
             .append(nbCountStatusRemoteShutdown).append("</RemoteShutdown>")
             .append("<Shutdown>").append(nbCountStatusShutdown)
             .append("</Shutdown>").append("<RemoteError>")
             .append(nbCountStatusRemoteError).append("</RemoteError>")
             .append("<Stopped>").append(nbCountStatusStopped)
             .append("</Stopped>").append("<Canceled>")
             .append(nbCountStatusCanceled).append("</Canceled>")
             .append("<FileNotFound>").append(nbCountStatusFileNotFound)
             .append("</FileNotFound>").append("<Unknown>")
             .append(nbCountStatusUnknown).append("</Unknown>")
             .append("</ERRORTYPES>");
    }
    builder.append("</STATUS>");
    return builder.toString();
  }

  /**
   * @param detail
   *
   * @return The Json representation of the current status
   */
  public String exportJson(boolean detail) {
    return JsonHandler.prettyPrint(exportAsJson(detail));
  }

  /**
   * @param detail
   *
   * @return The Json representation of the current status
   */
  public ObjectNode exportAsJson(boolean detail) {
    ObjectNode node = JsonHandler.createObjectNode();
    node = node.putObject("STATUS");
    // Global Informations
    node.put("HostID", Configuration.configuration.getHOST_ID());
    node.put("Date", new DateTime().toString());
    node.put("LastRun", new DateTime(lastTry).toString());
    node.put("FromDate", new DateTime(currentLimit).toString());
    node.put("SecondsRunning", secondsRunning);
    node.put("NetworkConnections", nbNetworkConnection);
    node.put("NbThreads", nbThread);
    node.put("InBandwidth", bandwidthIn);
    node.put("OutBandwidth", bandwidthOut);

    // Overall status including past, future and current transfers
    ObjectNode node2 = node.putObject("OVERALL");
    node2.put("AllTransfer", nbCountStepAllTransfer);
    node2.put("Unknown", nbCountInfoUnknown);
    node2.put("NotUpdated", nbCountInfoNotUpdated);
    node2.put("Interrupted", nbCountInfoInterrupted);
    node2.put("ToSubmit", nbCountInfoToSubmit);
    node2.put("Error", nbCountInfoError);
    node2.put("Running", nbCountInfoRunning);
    node2.put("Done", nbCountInfoDone);
    node2.put("InRunning", nbInActiveTransfer);
    node2.put("OutRunning", nbOutActiveTransfer);
    node2.put("LastInRunning", new DateTime(lastInActiveTransfer).toString());
    node2.put("LastOutRunning", new DateTime(lastOutActiveTransfer).toString());
    node2.put("InAll", nbInTotalTransfer);
    node2.put("OutAll", nbOutTotalTransfer);
    node2.put("InError", nbInErrorTransfer);
    node2.put("OutError", nbOutErrorTransfer);

    // Current situation of all transfers, running or not
    node2 = node.putObject("STEPS");
    node2.put("Notask", nbCountStepNotask);
    node2.put("Pretask", nbCountStepPretask);
    node2.put("Data.TransferInitializer", nbCountStepTransfer);
    node2.put("Posttask", nbCountStepPosttask);
    node2.put("AllDone", nbCountStepAllDone);
    node2.put("Error", nbCountStepError);

    // On Running Transfers only
    node2 = node.putObject("RUNNINGSTEPS");
    node2.put("AllRunning", nbCountAllRunningStep);
    if (detail) {
      node2.put("Running", nbCountRunningStep);
      node2.put("InitOk", nbCountInitOkStep);
      node2.put("PreProcessingOk", nbCountPreProcessingOkStep);
      node2.put("TransferOk", nbCountTransferOkStep);
      node2.put("PostProcessingOk", nbCountPostProcessingOkStep);
      node2.put("CompleteOk", nbCountCompleteOkStep);
    }

    if (detail) {
      // Error Status on all transfers
      node2 = node.putObject("ERRORTYPES");
      node2.put("ConnectionImpossible", nbCountStatusConnectionImpossible);
      node2.put("ServerOverloaded", nbCountStatusServerOverloaded);
      node2.put("BadAuthent", nbCountStatusBadAuthent);
      node2.put("ExternalOp", nbCountStatusExternalOp);
      node2.put("TransferError", nbCountStatusTransferError);
      node2.put("MD5Error", nbCountStatusMD5Error);
      node2.put("Disconnection", nbCountStatusDisconnection);
      node2.put("FinalOp", nbCountStatusFinalOp);
      node2.put("Unimplemented", nbCountStatusUnimplemented);
      node2.put("Internal", nbCountStatusInternal);
      node2.put("Warning", nbCountStatusWarning);
      node2.put("QueryAlreadyFinished", nbCountStatusQueryAlreadyFinished);
      node2.put("QueryStillRunning", nbCountStatusQueryStillRunning);
      node2.put("KnownHost", nbCountStatusNotKnownHost);
      node2.put("RemotelyUnknown", nbCountStatusQueryRemotelyUnknown);
      node2.put("CommandNotFound", nbCountStatusCommandNotFound);
      node2.put("PassThroughMode", nbCountStatusPassThroughMode);
      node2.put("RemoteShutdown", nbCountStatusRemoteShutdown);
      node2.put("Shutdown", nbCountStatusShutdown);
      node2.put("RemoteError", nbCountStatusRemoteError);
      node2.put("Stopped", nbCountStatusStopped);
      node2.put("Canceled", nbCountStatusCanceled);
      node2.put("FileNotFound", nbCountStatusFileNotFound);
      node2.put("Unknown", nbCountStatusUnknown);
    }
    return node;
  }

  @Override
  public void setAgent(WaarpSnmpAgent agent) {
    this.agent = agent;
    lastInActiveTransfer = this.agent.getUptimeSystemTime();
    lastOutActiveTransfer = this.agent.getUptimeSystemTime();
  }

  /**
   * Update the value for one particular MIB entry
   *
   * @param type
   * @param entry
   */
  public void run(int type, int entry) {
    final long nbMs =
        Configuration.configuration.getAgentSnmp().getUptime() + 100;
    final MibLevel level = MibLevel.values()[type];
    switch (level) {
      case globalInfo:// Global
        if (((R66PrivateMib) agent.getMib()).rowGlobal != null) {
          run(nbMs, WaarpGlobalValuesIndex.values()[entry]);
        }
        return;
      case detailedInfo:// Detailed
        if (((R66PrivateMib) agent.getMib()).rowDetailed != null) {
          run(nbMs, WaarpDetailedValuesIndex.values()[entry]);
        }
        return;
      case errorInfo:// Error
        if (((R66PrivateMib) agent.getMib()).rowError != null) {
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
    ((R66PrivateMib) agent.getMib()).rowGlobal.setValue(rank, value);
  }

  /**
   * Update a value in Detailed MIB part
   *
   * @param rank
   * @param value
   */
  protected void updateDetailedValue(int rank, long value) {
    ((R66PrivateMib) agent.getMib()).rowDetailed.setValue(rank, value);
  }

  /**
   * Update a value in Error MIB part
   *
   * @param rank
   * @param value
   */
  protected void updateErrorValue(int rank, long value) {
    ((R66PrivateMib) agent.getMib()).rowError.setValue(rank, value);
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
      if (dbSession == null || dbSession.isDisActive()) {
        switch (entry) {
          case applUptime:
            return;
          case applOperStatus:
            return;
          case applLastChange:
            return;
          case applInboundAssociations:
            updateGlobalValue(entry.ordinal(), nbInActiveTransfer);
            return;
          case applOutboundAssociations:
            updateGlobalValue(entry.ordinal(), nbOutActiveTransfer);
            return;
          case applAccumInboundAssociations:
            updateGlobalValue(entry.ordinal(), nbInTotalTransfer);
            return;
          case applAccumOutboundAssociations:
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
            updateGlobalValue(entry.ordinal(), nbInErrorTransfer);
            return;
          case applFailedOutboundAssociations:
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
            updateGlobalValue(entry.ordinal(), nbCountInfoUnknown);
            return;
          case nbInfoNotUpdated:
            updateGlobalValue(entry.ordinal(), nbCountInfoNotUpdated);
            return;
          case nbInfoInterrupted:
            updateGlobalValue(entry.ordinal(), nbCountInfoInterrupted);
            return;
          case nbInfoToSubmit:
            nbCountInfoToSubmit = CommanderNoDb.todoList.size();
            updateGlobalValue(entry.ordinal(), nbCountInfoToSubmit);
            return;
          case nbInfoError:
            updateGlobalValue(entry.ordinal(), nbCountInfoError);
            return;
          case nbInfoRunning:
            nbCountInfoRunning = Configuration.configuration.getInternalRunner()
                                                            .nbInternalRunner();
            updateGlobalValue(entry.ordinal(), nbCountInfoRunning);
            return;
          case nbInfoDone:
            updateGlobalValue(entry.ordinal(), nbCountInfoDone);
            return;
          case nbStepAllTransfer:
            updateGlobalValue(entry.ordinal(), nbCountStepAllTransfer);
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
            nbNetworkConnection =
                Configuration.configuration.getHttpChannelGroup().size() +
                Configuration.configuration.getServerChannelGroup().size();
            updateGlobalValue(entry.ordinal(), nbNetworkConnection);
            return;
        }
        return;
      }
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
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countInActiveTransfer,
                                                     limitDate);
            nbInActiveTransfer = DbTaskRunner
                .getResultCountPrepareStatement(countInActiveTransfer);
            updateGlobalValue(entry.ordinal(), nbInActiveTransfer);
            return;
          case applOutboundAssociations:
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countOutActiveTransfer,
                                                     limitDate);
            nbOutActiveTransfer = DbTaskRunner
                .getResultCountPrepareStatement(countOutActiveTransfer);
            updateGlobalValue(entry.ordinal(), nbOutActiveTransfer);
            return;
          case applAccumInboundAssociations:
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countInTotalTransfer,
                                                     limitDate);
            nbInTotalTransfer = DbTaskRunner
                .getResultCountPrepareStatement(countInTotalTransfer);
            updateGlobalValue(entry.ordinal(), nbInTotalTransfer);
            return;
          case applAccumOutboundAssociations:
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countOutTotalTransfer,
                                                     limitDate);
            nbOutTotalTransfer = DbTaskRunner
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
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countInErrorTransfer,
                                                     limitDate);
            nbInErrorTransfer = DbTaskRunner
                .getResultCountPrepareStatement(countInErrorTransfer);
            updateGlobalValue(entry.ordinal(), nbInErrorTransfer);
            return;
          case applFailedOutboundAssociations:
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countOutErrorTransfer,
                                                     limitDate);
            nbOutErrorTransfer = DbTaskRunner
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
            nbCountInfoUnknown = DbTaskRunner
                .getResultCountPrepareStatement(countInfo, UpdatedInfo.UNKNOWN,
                                                limitDate);
            updateGlobalValue(entry.ordinal(), nbCountInfoUnknown);
            return;
          case nbInfoNotUpdated:
            nbCountInfoNotUpdated = DbTaskRunner
                .getResultCountPrepareStatement(countInfo,
                                                UpdatedInfo.NOTUPDATED,
                                                limitDate);
            updateGlobalValue(entry.ordinal(), nbCountInfoNotUpdated);
            return;
          case nbInfoInterrupted:
            nbCountInfoInterrupted = DbTaskRunner
                .getResultCountPrepareStatement(countInfo,
                                                UpdatedInfo.INTERRUPTED,
                                                limitDate);
            updateGlobalValue(entry.ordinal(), nbCountInfoInterrupted);
            return;
          case nbInfoToSubmit:
            nbCountInfoToSubmit = DbTaskRunner
                .getResultCountPrepareStatement(countInfo, UpdatedInfo.TOSUBMIT,
                                                limitDate);
            updateGlobalValue(entry.ordinal(), nbCountInfoToSubmit);
            return;
          case nbInfoError:
            nbCountInfoError = DbTaskRunner
                .getResultCountPrepareStatement(countInfo, UpdatedInfo.INERROR,
                                                limitDate);
            updateGlobalValue(entry.ordinal(), nbCountInfoError);
            return;
          case nbInfoRunning:
            nbCountInfoRunning = DbTaskRunner
                .getResultCountPrepareStatement(countInfo, UpdatedInfo.RUNNING,
                                                limitDate);
            updateGlobalValue(entry.ordinal(), nbCountInfoRunning);
            return;
          case nbInfoDone:
            nbCountInfoDone = DbTaskRunner
                .getResultCountPrepareStatement(countInfo, UpdatedInfo.DONE,
                                                limitDate);
            updateGlobalValue(entry.ordinal(), nbCountInfoDone);
            return;
          case nbStepAllTransfer:
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countStepAllTransfer,
                                                     limitDate);
            nbCountStepAllTransfer = DbTaskRunner
                .getResultCountPrepareStatement(countStepAllTransfer);
            updateGlobalValue(entry.ordinal(), nbCountStepAllTransfer);
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
            nbNetworkConnection = DbAdmin.getNbConnection();
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
      if (dbSession == null || dbSession.isDisActive()) {
        switch (entry) {
          case nbStepNotask:
            updateDetailedValue(entry.ordinal(), nbCountStepNotask);
            return;
          case nbStepPretask:
            updateDetailedValue(entry.ordinal(), nbCountStepPretask);
            return;
          case nbStepTransfer:
            updateDetailedValue(entry.ordinal(), nbCountStepTransfer);
            return;
          case nbStepPosttask:
            updateDetailedValue(entry.ordinal(), nbCountStepPosttask);
            return;
          case nbStepAllDone:
            updateDetailedValue(entry.ordinal(), nbCountStepAllDone);
            return;
          case nbStepError:
            updateDetailedValue(entry.ordinal(), nbCountStepError);
            return;
          case nbAllRunningStep:
            nbCountAllRunningStep =
                Configuration.configuration.getInternalRunner()
                                           .nbInternalRunner();
            updateDetailedValue(entry.ordinal(), nbCountAllRunningStep);
            return;
          case nbRunningStep:
            updateDetailedValue(entry.ordinal(), nbCountRunningStep);
            return;
          case nbInitOkStep:
            updateDetailedValue(entry.ordinal(), nbCountInitOkStep);
            return;
          case nbPreProcessingOkStep:
            updateDetailedValue(entry.ordinal(), nbCountPreProcessingOkStep);
            return;
          case nbTransferOkStep:
            updateDetailedValue(entry.ordinal(), nbCountTransferOkStep);
            return;
          case nbPostProcessingOkStep:
            updateDetailedValue(entry.ordinal(), nbCountPostProcessingOkStep);
            return;
          case nbCompleteOkStep:
            updateDetailedValue(entry.ordinal(), nbCountCompleteOkStep);
            return;
        }
        return;
      }
      // Detailed
      try {
        switch (entry) {
          case nbStepNotask:
            DbTaskRunner.finishSelectOrCountPrepareStatement(countStepNotask,
                                                             limitDate);
            nbCountStepNotask =
                DbTaskRunner.getResultCountPrepareStatement(countStepNotask);
            updateDetailedValue(entry.ordinal(), nbCountStepNotask);
            return;
          case nbStepPretask:
            DbTaskRunner.finishSelectOrCountPrepareStatement(countStepPretask,
                                                             limitDate);
            nbCountStepPretask =
                DbTaskRunner.getResultCountPrepareStatement(countStepPretask);
            updateDetailedValue(entry.ordinal(), nbCountStepPretask);
            return;
          case nbStepTransfer:
            DbTaskRunner.finishSelectOrCountPrepareStatement(countStepTransfer,
                                                             limitDate);
            nbCountStepTransfer =
                DbTaskRunner.getResultCountPrepareStatement(countStepTransfer);
            updateDetailedValue(entry.ordinal(), nbCountStepTransfer);
            return;
          case nbStepPosttask:
            DbTaskRunner.finishSelectOrCountPrepareStatement(countStepPosttask,
                                                             limitDate);
            nbCountStepPosttask =
                DbTaskRunner.getResultCountPrepareStatement(countStepPosttask);
            updateDetailedValue(entry.ordinal(), nbCountStepPosttask);
            return;
          case nbStepAllDone:
            DbTaskRunner.finishSelectOrCountPrepareStatement(countStepAllDone,
                                                             limitDate);
            nbCountStepAllDone =
                DbTaskRunner.getResultCountPrepareStatement(countStepAllDone);
            updateDetailedValue(entry.ordinal(), nbCountStepAllDone);
            return;
          case nbStepError:
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countStepError, limitDate);
            nbCountStepError =
                DbTaskRunner.getResultCountPrepareStatement(countStepError);
            updateDetailedValue(entry.ordinal(), nbCountStepError);
            return;
          case nbAllRunningStep:
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countAllRunningStep,
                                                     limitDate);
            nbCountAllRunningStep = DbTaskRunner
                .getResultCountPrepareStatement(countAllRunningStep);
            updateDetailedValue(entry.ordinal(), nbCountAllRunningStep);
            return;
          case nbRunningStep:
            DbTaskRunner.finishSelectOrCountPrepareStatement(countRunningStep,
                                                             limitDate);
            nbCountRunningStep =
                DbTaskRunner.getResultCountPrepareStatement(countRunningStep);
            updateDetailedValue(entry.ordinal(), nbCountRunningStep);
            return;
          case nbInitOkStep:
            DbTaskRunner.finishSelectOrCountPrepareStatement(countInitOkStep,
                                                             limitDate);
            nbCountInitOkStep =
                DbTaskRunner.getResultCountPrepareStatement(countInitOkStep);
            updateDetailedValue(entry.ordinal(), nbCountInitOkStep);
            return;
          case nbPreProcessingOkStep:
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countPreProcessingOkStep,
                                                     limitDate);
            nbCountPreProcessingOkStep = DbTaskRunner
                .getResultCountPrepareStatement(countPreProcessingOkStep);
            updateDetailedValue(entry.ordinal(), nbCountPreProcessingOkStep);
            return;
          case nbTransferOkStep:
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countTransferOkStep,
                                                     limitDate);
            nbCountTransferOkStep = DbTaskRunner
                .getResultCountPrepareStatement(countTransferOkStep);
            updateDetailedValue(entry.ordinal(), nbCountTransferOkStep);
            return;
          case nbPostProcessingOkStep:
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countPostProcessingOkStep,
                                                     limitDate);
            nbCountPostProcessingOkStep = DbTaskRunner
                .getResultCountPrepareStatement(countPostProcessingOkStep);
            updateDetailedValue(entry.ordinal(), nbCountPostProcessingOkStep);
            return;
          case nbCompleteOkStep:
            DbTaskRunner
                .finishSelectOrCountPrepareStatement(countCompleteOkStep,
                                                     limitDate);
            nbCountCompleteOkStep = DbTaskRunner
                .getResultCountPrepareStatement(countCompleteOkStep);
            updateDetailedValue(entry.ordinal(), nbCountCompleteOkStep);
            return;
        }
      } catch (final WaarpDatabaseNoConnectionException e) {
        logger
            .info("Database No Connection Error: Cannot execute Monitoring", e);
        try {
          dbSession.getAdmin().getDbModel().validConnection(dbSession);
        } catch (final WaarpDatabaseNoConnectionException e1) {
        }
      } catch (final WaarpDatabaseSqlException e) {
        logger
            .info("Database No Connection Error: Cannot execute Monitoring", e);
        try {
          dbSession.getAdmin().getDbModel().validConnection(dbSession);
        } catch (final WaarpDatabaseNoConnectionException e1) {
        }
      }
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
      if (dbSession == null || dbSession.isDisActive()) {
        return;
      }
      // Error
      switch (entry) {
        case nbStatusConnectionImpossible:
          nbCountStatusConnectionImpossible = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.ConnectionImpossible,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusConnectionImpossible);
          return;
        case nbStatusServerOverloaded:
          nbCountStatusServerOverloaded = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.ServerOverloaded,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusServerOverloaded);
          return;
        case nbStatusBadAuthent:
          nbCountStatusBadAuthent = DbTaskRunner
              .getResultCountPrepareStatement(countStatus, ErrorCode.BadAuthent,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusBadAuthent);
          return;
        case nbStatusExternalOp:
          nbCountStatusExternalOp = DbTaskRunner
              .getResultCountPrepareStatement(countStatus, ErrorCode.ExternalOp,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusExternalOp);
          return;
        case nbStatusTransferError:
          nbCountStatusTransferError = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.TransferError,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusTransferError);
          return;
        case nbStatusMD5Error:
          nbCountStatusMD5Error = DbTaskRunner
              .getResultCountPrepareStatement(countStatus, ErrorCode.MD5Error,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusMD5Error);
          return;
        case nbStatusDisconnection:
          nbCountStatusDisconnection = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.Disconnection,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusDisconnection);
          return;
        case nbStatusFinalOp:
          nbCountStatusFinalOp = DbTaskRunner
              .getResultCountPrepareStatement(countStatus, ErrorCode.FinalOp,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusFinalOp);
          return;
        case nbStatusUnimplemented:
          nbCountStatusUnimplemented = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.Unimplemented,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusUnimplemented);
          return;
        case nbStatusInternal:
          nbCountStatusInternal = DbTaskRunner
              .getResultCountPrepareStatement(countStatus, ErrorCode.Internal,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusInternal);
          return;
        case nbStatusWarning:
          nbCountStatusWarning = DbTaskRunner
              .getResultCountPrepareStatement(countStatus, ErrorCode.Warning,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusWarning);
          return;
        case nbStatusQueryAlreadyFinished:
          nbCountStatusQueryAlreadyFinished = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.QueryAlreadyFinished,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusQueryAlreadyFinished);
          return;
        case nbStatusQueryStillRunning:
          nbCountStatusQueryStillRunning = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.QueryStillRunning,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusQueryStillRunning);
          return;
        case nbStatusNotKnownHost:
          nbCountStatusNotKnownHost = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.NotKnownHost,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusNotKnownHost);
          return;
        case nbStatusQueryRemotelyUnknown:
          nbCountStatusQueryRemotelyUnknown = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.QueryRemotelyUnknown,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusQueryRemotelyUnknown);
          return;
        case nbStatusCommandNotFound:
          nbCountStatusCommandNotFound = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.CommandNotFound,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusCommandNotFound);
          return;
        case nbStatusPassThroughMode:
          nbCountStatusPassThroughMode = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.PassThroughMode,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusPassThroughMode);
          return;
        case nbStatusRemoteShutdown:
          nbCountStatusRemoteShutdown = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.RemoteShutdown,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusRemoteShutdown);
          return;
        case nbStatusShutdown:
          nbCountStatusShutdown = DbTaskRunner
              .getResultCountPrepareStatement(countStatus, ErrorCode.Shutdown,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusShutdown);
          return;
        case nbStatusRemoteError:
          nbCountStatusRemoteError = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.RemoteError, limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusRemoteError);
          return;
        case nbStatusStopped:
          nbCountStatusStopped = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.StoppedTransfer,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusStopped);
          return;
        case nbStatusCanceled:
          nbCountStatusCanceled = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.CanceledTransfer,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusCanceled);
          return;
        case nbStatusFileNotFound:
          nbCountStatusFileNotFound = DbTaskRunner
              .getResultCountPrepareStatement(countStatus,
                                              ErrorCode.FileNotFound,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusFileNotFound);
          return;
        case nbStatusUnknown:
          nbCountStatusUnknown = DbTaskRunner
              .getResultCountPrepareStatement(countStatus, ErrorCode.Unknown,
                                              limitDate);
          updateErrorValue(entry.ordinal(), nbCountStatusUnknown);
          return;
      }
    }
  }
}
