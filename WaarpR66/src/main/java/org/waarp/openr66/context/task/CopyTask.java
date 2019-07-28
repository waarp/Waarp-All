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

import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;

import java.io.File;

/**
 * Copy task
 */
public class CopyTask extends AbstractTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(CopyTask.class);

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public CopyTask(String argRule, int delay, String argTransfer,
                  R66Session session) {
    super(TaskType.COPY, delay, argRule, argTransfer, session);
  }

  @Override
  public void run() {
    logger
        .info("Copy with " + argRule + ':' + argTransfer + " and {}", session);
    final File from = session.getFile().getTrueFile();
    final String directory = argRule.replace('\\', '/');
    final File to = new File(directory, session.getFile().getBasename());
    try {
      FileUtils.copy(from, to, false, false);
    } catch (final Reply550Exception e1) {
      logger
          .error("Copy with " + argRule + ':' + argTransfer + " and " + session,
                 e1);
      futureCompletion.setFailure(new OpenR66ProtocolSystemException(e1));
      return;
    }
    futureCompletion.setSuccess();
  }

}
