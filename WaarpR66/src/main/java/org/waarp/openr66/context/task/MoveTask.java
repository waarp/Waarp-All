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
import org.waarp.common.file.DirInterface;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.R66Session;

import java.io.File;

/**
 * Move the file (without renaming it)
 */
public class MoveTask extends AbstractTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(MoveTask.class);

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public MoveTask(final String argRule, final int delay,
                  final String argTransfer, final R66Session session) {
    super(TaskType.MOVE, delay, argRule, argTransfer, session);
  }

  @Override
  public final void run() {
    logger.info("Move with " + argRule + ':' + argTransfer + " and {}",
                session);
    String directory = argRule;
    directory = getReplacedValue(directory, BLANK.split(argTransfer)).trim()
                                                                     .replace(
                                                                         '\\',
                                                                         '/');
    final String finalname =
        directory + DirInterface.SEPARATOR + session.getFile().getBasename();
    final File from = session.getFile().getTrueFile();
    final File to = new File(finalname);
    try {
      FileUtils.copy(from, to, true, false);
    } catch (final Reply550Exception e) {
      logger.error(
          "Move with " + argRule + ':' + argTransfer + " to " + finalname +
          " and " + session + ": {}", e.getMessage());
      futureCompletion.setFailure(e);
      return;
    }
    session.getRunner().setFileMoved(finalname, true);
    futureCompletion.setSuccess();

  }

}
