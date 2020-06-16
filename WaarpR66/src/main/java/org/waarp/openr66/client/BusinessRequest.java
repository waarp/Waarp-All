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

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.openr66.client.utils.OutputFormat;
import org.waarp.openr66.client.utils.OutputFormat.FIELDS;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.localhandler.packet.BusinessRequestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import static org.waarp.common.database.DbConstant.*;

/**
 * class for direct Business Request call
 */
public class BusinessRequest extends AbstractBusinessRequest {
  /**
   * Internal Logger
   */
  private static WaarpLogger logger;
  /**
   * Default class
   */
  public static final String DEFAULT_CLASS =
      "org.waarp.openr66.context.task.ExecBusinessTask";

  public BusinessRequest(NetworkTransaction networkTransaction,
                         R66Future future, String remoteHost,
                         BusinessRequestPacket packet) {
    super(BusinessRequest.class, future, remoteHost, networkTransaction,
          packet);
  }

  public static void main(String[] args) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(BusinessRequest.class);
    }
    if (args.length < 5) {
      logger.error(Messages.getString("BusinessRequest.1") + //$NON-NLS-1$
                   _INFO_ARGS);
      return;
    }
    classname = DEFAULT_CLASS;
    if (!getParams(args) || classarg == null) {
      logger.error(Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
      if (admin != null) {
        admin.close();
      }
      if (DetectionUtils.isJunit()) {
        return;
      }
      ChannelUtils.stopLogger();
      System.exit(2);//NOSONAR
    }
    Configuration.configuration.pipelineInit();
    final NetworkTransaction networkTransaction = new NetworkTransaction();
    final R66Future future = new R66Future(true);

    logger.info("Start Test of Transaction");
    final long time1 = System.currentTimeMillis();

    final BusinessRequestPacket packet =
        new BusinessRequestPacket(classname + ' ' + classarg, 0);
    final BusinessRequest transaction =
        new BusinessRequest(networkTransaction, future, rhost, packet);
    transaction.run();
    future.awaitOrInterruptible();

    final long time2 = System.currentTimeMillis();
    logger.debug("Finish Business Request: " + future.isSuccess());
    final long delay = time2 - time1;
    final OutputFormat outputFormat =
        new OutputFormat(BusinessRequest.class.getSimpleName(), args);
    if (future.isSuccess()) {
      outputFormat.setValue(FIELDS.status.name(), 0);
      outputFormat.setValue(FIELDS.statusTxt.name(),
                            Messages.getString("BusinessRequest.6") + Messages
                                .getString(
                                    "RequestInformation.Success")); //$NON-NLS-1$
      outputFormat.setValue(FIELDS.remote.name(), rhost);
      outputFormat.setValue("delay", delay);
      logger.info(outputFormat.loggerOut());
      if (!OutputFormat.isQuiet()) {
        outputFormat.sysout();
      }
    } else {
      outputFormat.setValue(FIELDS.status.name(), 2);
      outputFormat.setValue(FIELDS.statusTxt.name(),
                            Messages.getString("BusinessRequest.6") + Messages
                                .getString(
                                    "RequestInformation.Failure")); //$NON-NLS-1$
      outputFormat.setValue(FIELDS.remote.name(), rhost);
      outputFormat.setValue("delay", delay);
      logger.error(outputFormat.loggerOut(), future.getCause());
      outputFormat.setValue(FIELDS.error.name(), future.getCause().toString());
      if (!OutputFormat.isQuiet()) {
        outputFormat.sysout();
      }
      networkTransaction.closeAll();
      System.exit(ErrorCode.Unknown.ordinal());//NOSONAR
    }
    networkTransaction.closeAll();
    System.exit(0);//NOSONAR
  }

}
