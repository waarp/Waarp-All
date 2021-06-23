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
import org.waarp.common.utility.ParametersChecker;
import org.waarp.compress.MalformedInputException;
import org.waarp.compress.WaarpZstdCodec;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;

import java.io.File;

/**
 * Compress using ZSTD, Rename and Delete the current file
 */
public class CompressTask extends AbstractTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(CompressTask.class);

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public CompressTask(final String argRule, final int delay,
                      final String argTransfer, final R66Session session) {
    super(TaskType.COMPRESS, delay, argRule, argTransfer, session);
  }

  @Override
  public void run() {
    String finalname = argRule;
    finalname = getReplacedValue(finalname, BLANK.split(argTransfer)).trim()
                                                                     .replace(
                                                                         '\\',
                                                                         '/');
    final File from = session.getFile().getTrueFile();
    if (ParametersChecker.isEmpty(finalname)) {
      finalname = from.getParent() + "/" + session.getFile().getBasename();
      if (delay == 0) {
        finalname += ".zstd";
      } else {
        finalname += ".unzstd";
      }
    }
    logger.debug("{} and " + "Rename to {} with {}:{} and {}",
                 (delay == 1? "Decompress" : "Compress"), finalname, argRule,
                 argTransfer, session);
    final File to = new File(finalname);
    logger.debug("From {} {} to {} {} using {}", from, from.canRead(), to,
                 to.canRead(), (delay == 1? "Decompress" : "Compress"));
    try {
      WaarpZstdCodec zstdCodec = new WaarpZstdCodec();
      if (delay == 1) {
        zstdCodec.decompress(from, to);
      } else {
        zstdCodec.compress(from, to);
      }
      if (!from.delete()) {
        logger.warn("File {} not correctly deleted", from);
      }
    } catch (final MalformedInputException e) {
      logger.error((delay == 1? "Decompress" : "Compress") + " and Rename to " +
                   finalname + " with " + argRule + ':' + argTransfer +
                   " and " + session + ": {}", e.getMessage());
      futureCompletion.setFailure(e);
      return;
    }
    try {
      session.getFile().replaceFilename(finalname, true);
    } catch (CommandAbstractException e) {
      logger.error(
          "Replace with Compressed file as " + finalname + " with " + argRule +
          ':' + argTransfer + " and " + session + ": {}", e.getMessage());
      futureCompletion.setFailure(new OpenR66ProtocolSystemException(e));
      return;
    }
    session.getRunner().setFileMoved(finalname, true);
    logger.debug("From {} {} to {} {} using {}", from, from.canRead(), to,
                 to.canRead(), (delay == 1? "Decompress" : "Compress"));
    futureCompletion.setSuccess();
  }

}
