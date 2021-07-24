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
package org.waarp.openr66.context.task;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import java.util.concurrent.TimeUnit;

/**
 * Command to Restart the R66 server (for instance after upgrade of jar sent by
 * administrative operations,
 * unzipped in the library directory)
 */
public class RestartServerTask extends AbstractTask {

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(RestartServerTask.class);

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public RestartServerTask(final String argRule, final int delay,
                           final String argTransfer, final R66Session session) {
    super(TaskType.RESTART, delay, argRule, argTransfer, session);
  }

  @Override
  public void run() {
    // check if allowed to do restart
    // SYSTEM authorization
    final boolean isAdmin = session.getAuth().isValidRole(ROLE.SYSTEM);
    if (!isAdmin) {
      // not allowed
      logger.error("Shutdown order asked through task but unallowed: " +
                   session.getAuth().getUser());
      futureCompletion.setFailure(new OpenR66ProtocolSystemException(
          "Shutdown order asked through task but unallowed: " +
          session.getAuth().getUser()));
      return;
    }
    if (!Configuration.configuration.isServer()) {
      logger.error(
          "Shutdown order asked through task but this is a client, not a server");
      futureCompletion.setFailure(new OpenR66ProtocolSystemException(
          "Shutdown order asked through task but this is a client, not a server"));
      return;
    }
    // now start the process
    logger.warn("Shutdown order received and going from: " +
                session.getAuth().getUser());
    WaarpShutdownHook.setRestart(true);
    futureCompletion.setSuccess();
    final Thread thread =
        new Thread(new ChannelUtils(), "R66 Shutdown and Restart Thread");
    thread.setDaemon(true);
    // give time for the task to finish correctly
    Configuration.configuration.launchInFixedDelay(thread, 1000,
                                                   TimeUnit.MILLISECONDS);
  }

}
