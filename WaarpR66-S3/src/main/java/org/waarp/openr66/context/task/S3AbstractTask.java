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

import org.apache.commons.exec.CommandLine;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.s3.taskfactory.S3TaskFactory.S3TaskType;

/**
 * Abstract Class for S3 Tasks<br>
 * <p>
 * Arguments will comme from both Rule argument and Transfer information,
 * using substitution such that arguments can be passed through transfer
 * and not set statically into rule.</p>
 */
public abstract class S3AbstractTask extends AbstractTask {
  static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(S3AbstractTask.class);

  S3TaskArgs taskUtil = null;

  /**
   * Constructor
   *
   * @param type
   * @param delay
   * @param argRule
   * @param argTransfer
   * @param session
   */
  S3AbstractTask(final TaskType type, final int delay, final String argRule,
                 final String argTransfer, final R66Session session) {
    super(type, delay, argRule, argTransfer, session);
  }

  public boolean getParams() {
    logger.info("Transfer with {}:{} and {}", argRule, argTransfer, session);
    try {
      String finalname = applyTransferSubstitutions(argRule);

      finalname = finalname.replaceAll("#([A-Z]+)#", "\\${$1}");
      final CommandLine commandLine = new CommandLine("dummy");
      commandLine.setSubstitutionMap(getSubstitutionMap());
      commandLine.addArguments(finalname, false);
      final String[] args = commandLine.getArguments();
      taskUtil = S3TaskArgs.getS3Params(this.getS3TaskType(), 0, args);
      return taskUtil != null;
    } catch (final Exception e) {
      finalizeInError(e, "Not enough argument in Transfer");
      return false;
    }
  }

  protected void finalizeInError(final Exception e, final String message) {
    logger.error("{} {}", getS3TaskType().name(), message, e);
    futureCompletion.setFailure(
        new OpenR66RunnerErrorException(getS3TaskType() + " " + message, e));
  }

  public abstract S3TaskType getS3TaskType();

}
