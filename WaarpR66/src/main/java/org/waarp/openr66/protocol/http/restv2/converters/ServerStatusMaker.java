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

package org.waarp.openr66.protocol.http.restv2.converters;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.waarp.common.json.JsonHandler;
import org.waarp.openr66.protocol.localhandler.Monitoring;

import static org.waarp.openr66.protocol.configuration.Configuration.*;
import static org.waarp.openr66.protocol.http.restv2.RestConstants.*;

/**
 * A POJO representing the general status of the R66 server.
 */
public final class ServerStatusMaker {

  /**
   * Date of the last time the server status was requested.
   */
  private static DateTime lastRun;

  /**
   * Makes the default constructor of this utility class inaccessible.
   */
  private ServerStatusMaker() throws InstantiationException {
    throw new InstantiationException(
        getClass().getName() + " cannot be instantiated.");
  }

  // ########################## PUBLIC METHODS ################################

  /**
   * Creates an {@link ObjectNode} listing general information about the R66
   * server. The {@link Period}
   * parameters specifies the time period on which the information is
   * collected.
   *
   * @param period the time period analysed
   *
   * @return the server status ObjectNode
   */
  public static ObjectNode exportAsJson(final Period period) {
    final int seconds = period.toStandardSeconds().getSeconds();
    final Monitoring mon = configuration.getMonitoring();
    mon.run(seconds, true);
    final ObjectNode server = JsonHandler.createObjectNode();

    server.put("serverName", serverName());
    server.put("date", DateTime.now().toString());
    server.put("lastRun", lastRun == null? null : lastRun.toString());
    server.put("fromDate", DateTime.now().minus(period).toString());
    server.put("secondsRunning", mon.secondsRunning);
    server.put("networkConnections", mon.nbNetworkConnection);
    server.put("nbThreads", mon.nbThread);
    server.put("inBandwidth", mon.bandwidthIn);
    server.put("outBandwidth", mon.bandwidthOut);

    final ObjectNode overall = server.putObject("overall");
    overall.put("allTransfers", mon.nbCountStepAllTransfer);
    overall.put("unknown", mon.nbCountInfoUnknown);
    overall.put("notUpdated", mon.nbCountInfoNotUpdated);
    overall.put("interrupted", mon.nbCountInfoInterrupted);
    overall.put("toSubmit", mon.nbCountInfoToSubmit);
    overall.put("inError", mon.nbCountInfoError);
    overall.put("running", mon.nbCountInfoRunning);
    overall.put("done", mon.nbCountInfoDone);
    overall.put("runningIn", mon.nbInActiveTransfer);
    overall.put("runningOut", mon.nbOutActiveTransfer);
    overall.put("lastRunningIn",
                new DateTime(mon.lastInActiveTransfer).toString());
    overall.put("lastRunningOut",
                new DateTime(mon.lastOutActiveTransfer).toString());
    overall.put("allIn", mon.nbInTotalTransfer);
    overall.put("allOut", mon.nbOutTotalTransfer);
    overall.put("errorsIn", mon.nbInErrorTransfer);
    overall.put("errorsOut", mon.nbOutErrorTransfer);

    final ObjectNode globalSteps = server.putObject("globalSteps");
    globalSteps.put("noTask", mon.nbCountStepNotask);
    globalSteps.put("preTask", mon.nbCountStepPretask);
    globalSteps.put("transferTask", mon.nbCountStepTransfer);
    globalSteps.put("postTask", mon.nbCountStepPosttask);
    globalSteps.put("allDoneTask", mon.nbCountStepAllDone);
    globalSteps.put("errorTask", mon.nbCountStepError);

    final ObjectNode runningSteps = server.putObject("runningSteps");
    runningSteps.put("allRunning", mon.nbCountAllRunningStep);
    runningSteps.put("running", mon.nbCountRunningStep);
    runningSteps.put("initOK", mon.nbCountInitOkStep);
    runningSteps.put("preProcessingOk", mon.nbCountPreProcessingOkStep);
    runningSteps.put("transferOk", mon.nbCountTransferOkStep);
    runningSteps.put("postProcessingOk", mon.nbCountPostProcessingOkStep);
    runningSteps.put("completeOk", mon.nbCountCompleteOkStep);

    final ObjectNode errors = server.putObject("errors");
    errors.put("connectionImpossible", mon.nbCountStatusConnectionImpossible);
    errors.put("serverOverloaded", mon.nbCountStatusServerOverloaded);
    errors.put("badAuthent", mon.nbCountStatusBadAuthent);
    errors.put("externalOp", mon.nbCountStatusExternalOp);
    errors.put("transferError", mon.nbCountStatusTransferError);
    errors.put("md5Error", mon.nbCountStatusMD5Error);
    errors.put("disconnection", mon.nbCountStatusDisconnection);
    errors.put("finalOp", mon.nbCountStatusFinalOp);
    errors.put("unimplemented", mon.nbCountStatusUnimplemented);
    errors.put("internal", mon.nbCountStatusInternal);
    errors.put("warning", mon.nbCountStatusWarning);
    errors.put("queryAlreadyFinished", mon.nbCountStatusQueryAlreadyFinished);
    errors.put("queryStillRunning", mon.nbCountStatusQueryStillRunning);
    errors.put("unknownHost", mon.nbCountStatusNotKnownHost);
    errors.put("remotelyUnknown", mon.nbCountStatusQueryRemotelyUnknown);
    errors.put("commandNotFound", mon.nbCountStatusCommandNotFound);
    errors.put("passThroughMode", mon.nbCountStatusPassThroughMode);
    errors.put("remoteShutdown", mon.nbCountStatusRemoteShutdown);
    errors.put("shutdown", mon.nbCountStatusShutdown);
    errors.put("remoteError", mon.nbCountStatusRemoteError);
    errors.put("stopped", mon.nbCountStatusStopped);
    errors.put("canceled", mon.nbCountStatusCanceled);
    errors.put("fileNotFound", mon.nbCountStatusFileNotFound);
    errors.put("unknown", mon.nbCountStatusUnknown);

    lastRun = DateTime.now();

    return server;
  }
}
