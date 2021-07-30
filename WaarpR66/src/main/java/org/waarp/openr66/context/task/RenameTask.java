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

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;

/**
 * Rename the current file (no move, move or creation should be done elsewhere)
 */
public class RenameTask extends AbstractTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(RenameTask.class);

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public RenameTask(final String argRule, final int delay,
                    final String argTransfer, final R66Session session) {
    super(TaskType.RENAME, delay, argRule, argTransfer, session);
  }

  @Override
  public final void run() {
    final boolean success;
    String finalname = argRule;
    finalname = getReplacedValue(finalname, BLANK.split(argTransfer)).trim()
                                                                     .replace(
                                                                         '\\',
                                                                         '/');
    logger.debug("Rename to {} with {}:{} and {}", finalname, argRule,
                 argTransfer, session);
    try {
      session.getFile().replaceFilename(finalname, true);
      success = true;
    } catch (final CommandAbstractException e) {
      logger.error(
          "Rename to " + finalname + " with " + argRule + ':' + argTransfer +
          " and " + session + ": {}", e.getMessage());
      futureCompletion.setFailure(new OpenR66ProtocolSystemException(e));
      return;
    }
    if (success) {
      session.getRunner().setFileMoved(finalname, success);
      futureCompletion.setSuccess();
    } else {
      logger.error(
          "Cannot Move and Rename to " + finalname + " with " + argRule + ':' +
          argTransfer + " and " + session);
      futureCompletion.setFailure(
          new OpenR66ProtocolSystemException("Cannot move file"));
    }
  }

}
