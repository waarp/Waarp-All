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
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.protocol.localhandler.packet.BusinessRequestPacket;
import org.waarp.openr66.protocol.utils.R66Future;

/**
 * Business Execution of a Java Task
 * <p>
 * Fullarg = First argument is the Java class name, Last argument is the delay.
 */
public class ExecBusinessTask extends AbstractExecJavaTask {

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(ExecBusinessTask.class);

  @Override
  public final void run() {
    if (callFromBusiness) {
      // Business Request to validate?
      String validate = "Validated";
      if (isToValidate) {
        logger.debug("DEBUG: {}", fullarg);
        final String[] args = BLANK.split(fullarg);
        final String operation = args[0];
        int newdelay;
        String argRule;
        try {
          newdelay = Integer.parseInt(args[args.length - 1]);
          argRule = fullarg.substring(fullarg.indexOf(' ') + 1,
                                      fullarg.lastIndexOf(' '));
        } catch (final NumberFormatException e) {
          newdelay = 0;
          argRule = fullarg.substring(fullarg.indexOf(' ') + 1);
        }
        try {
          final AbstractTask task =
              TaskType.getTaskFromIdForBusiness(operation, argRule, newdelay,
                                                session);
          if (task != null) {
            task.run();
            task.getFutureCompletion().awaitOrInterruptible();
            final R66Future future = task.getFutureCompletion();
            if (!future.isDone() || future.isFailed()) {
              invalid();
              return;
            }
            if (future.getResult() != null &&
                future.getResult().getOther() != null) {
              validate = future.getResult().getOther().toString();
            }
          } else {
            logger.error("ExecBusiness in error, Task invalid: " + operation);
            invalid();
            return;
          }
        } catch (final OpenR66RunnerErrorException e1) {
          logger.error("ExecBusiness in error: " + e1);
          invalid();
          return;
        }
        final BusinessRequestPacket packet =
            new BusinessRequestPacket(getClass().getName() + " execution ok",
                                      0);
        validate(packet);
        return;
      }
      finalValidate(validate);
    } else {
      // Rule EXECJAVA based should be used instead
      status = 2;
      fullarg = "EXECJAVA should be used instead";
    }
  }
}
