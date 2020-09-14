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
import org.waarp.common.tar.TarUtility;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * TAR task
 */
public class TarTask extends AbstractTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(TarTask.class);

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public TarTask(final String argRule, final int delay,
                 final String argTransfer, final R66Session session) {
    super(TaskType.TAR, delay, argRule, argTransfer, session);
  }

  @Override
  public void run() {
    logger
        .info("TAR with {}:{}:{} and {}", argRule, argTransfer, delay, session);
    String finalname = argRule;
    finalname = getReplacedValue(finalname, argTransfer == null? null :
        argTransfer.split(" "));
    boolean tar;
    switch (delay) {
      case 2: {
        // directory: tar finalname where finalname="target directory"
        final String[] args = finalname.split(" ");
        tar = TarUtility.createTarFromDirectory(args[1], args[0], true);
        break;
      }
      case 3: {
        // list of files: tar finalname where finalname="target file1 file2..."
        final String[] args = finalname.split(" ");
        final List<File> files = new ArrayList<File>(args.length - 1);
        for (int i = 1; i < args.length; i++) {
          files.add(new File(args[i]));
        }
        tar = TarUtility.createTarFromFiles(files, args[0]);
        break;
      }
      default:
        // untar
        // directory: untar finalname where finalname="source directory"
        final String[] args = finalname.split(" ");
        final File tarFile = new File(args[0]);
        final File directory = new File(args[1]);
        try {
          TarUtility.unTar(tarFile, directory);
          tar = true;
        } catch (final IOException e) {
          logger.warn("Error while untar", e);
          tar = false;
        }
        break;
    }
    if (!tar) {
      logger.error(
          "Tar error with " + argRule + ':' + argTransfer + ':' + delay +
          " and " + session);
      futureCompletion
          .setFailure(new OpenR66ProtocolSystemException("Tar error"));
      return;
    }
    futureCompletion.setSuccess();
  }

}
