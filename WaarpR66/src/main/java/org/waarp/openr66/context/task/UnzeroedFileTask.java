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

import org.waarp.common.file.AbstractDir;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This task add 1 byte to empty file if the current file is empty (0
 * length).<br>
 * <br>
 * <p>
 * The task will be in error only if the file is of length 0 but cannot be
 * unzeroed.<br>
 * The content of PATH, if not empty, will be the content when unzeroed. If
 * empty, the 'blank' character will
 * be used.<br>
 * <p>
 * delay >= 1 will make a log using info level for 1, warn level for 2.
 */
public class UnzeroedFileTask extends AbstractTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(UnzeroedFileTask.class);

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public UnzeroedFileTask(final String argRule, final int delay,
                          final String argTransfer, final R66Session session) {
    super(TaskType.UNZEROED, delay, argRule, argTransfer, session);
  }

  @Override
  public void run() {
    final File currentFile = session.getFile().getTrueFile();
    final String toWrite = argRule == null || argRule.isEmpty()? " " : argRule;
    final String curpath =
        AbstractDir.normalizePath(currentFile.getAbsolutePath());
    if (currentFile.exists() && currentFile.length() == 0) {
      FileOutputStream out = null;
      try {
        out = new FileOutputStream(currentFile);
        out.write(toWrite.getBytes());
        if (delay > 0) {
          if (delay > 1) {
            logger.warn("Unzeroed File: " + curpath + " from " + session);
          } else {
            logger.info("Unzeroed File: {} from {}", curpath, session);
          }
        }
        futureCompletion.setSuccess();
      } catch (final IOException e) {
        logger.error("Cannot unzeroed File: " + curpath + " from " + session);
        futureCompletion
            .setFailure(new OpenR66RunnerException("File not Unzeroed"));
      } finally {
        FileUtils.close(out);
      }
      return;
    }
    if (delay > 0) {
      if (delay > 1) {
        logger.warn(
            "Unzeroed File not applicable to " + curpath + " from " + session);
      } else {
        logger.info("Unzeroed File not applicable to {} from {}", curpath,
                    session);
      }
    }
    futureCompletion.setSuccess();
  }

}
