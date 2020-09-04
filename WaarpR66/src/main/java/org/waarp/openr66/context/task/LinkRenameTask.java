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
import java.util.regex.Pattern;

/**
 * Create a link of the current file and make the file pointing to it.
 * <p>
 * The link first tries to be a hard link, then a soft link, and if really not
 * possible (not supported by the
 * filesystem), it does a copy and rename task.
 */
public class LinkRenameTask extends AbstractTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(LinkRenameTask.class);
  private static final Pattern COMPILE = Pattern.compile(" ");

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public LinkRenameTask(final String argRule, final int delay,
                        final String argTransfer, final R66Session session) {
    super(TaskType.LINKRENAME, delay, argRule, argTransfer, session);
  }

  @Override
  public void run() {
    String finalname = argRule;
    finalname = getReplacedValue(finalname, COMPILE.split(argTransfer));
    logger.info("Move and Rename to " + finalname + " with " + argRule + ':' +
                argTransfer + " and {}", session);
    // First try hard link
    // FIXME wait for NIO.2 in JDK7 to have such functions, in the meantime only move...
    final File from = session.getFile().getTrueFile();
    final File to = new File(finalname);
    try {
      FileUtils.copy(from, to, false, false);
    } catch (final Reply550Exception e1) {
      logger.error(
          "Copy and Rename to " + finalname + " with " + argRule + ':' +
          argTransfer + " and " + session, e1);
      futureCompletion.setFailure(new OpenR66ProtocolSystemException(e1));
      return;
    }
    session.getRunner().setFileMoved(finalname, true);
    futureCompletion.setSuccess();
  }

}
