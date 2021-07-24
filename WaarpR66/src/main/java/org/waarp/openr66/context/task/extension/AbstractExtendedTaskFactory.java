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
package org.waarp.openr66.context.task.extension;

import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.AbstractTask;
import org.waarp.openr66.context.task.TaskType;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;

import java.util.Set;

/**
 * Abstract Factory class to allow addition of new external Tasks
 */
public abstract class AbstractExtendedTaskFactory {
  /**
   * The extended factory must have a constructor with no arguments
   */
  public AbstractExtendedTaskFactory() {
    // Nothing
  }

  /**
   * @return the Task Factory name
   */
  public abstract String getName();

  /**
   * <b>Mandatory</b> call at the end of constructor for the final
   * implementation
   *
   * @param s3TaskNames the Set of Command names that will be associated with
   *     this TaskFactory
   */
  public final void create(final Set<String> s3TaskNames) {
    TaskType.addExtendedTaskFactory(s3TaskNames, this);
  }

  /**
   * @param name
   * @param argRule
   * @param delay
   * @param session
   *
   * @return the corresponding AbstractTask
   *
   * @throws OpenR66RunnerErrorException
   */
  public abstract AbstractTask getTaskFromId(final String name,
                                             final String argRule,
                                             final int delay,
                                             final R66Session session)
      throws OpenR66RunnerErrorException;

  /**
   * For usage in ExecBusinessTask
   *
   * @param name
   * @param argRule
   * @param delay
   * @param session
   *
   * @return the corresponding AbstractTask
   *
   * @throws OpenR66RunnerErrorException
   */
  public abstract AbstractTask getTaskFromIdForBusiness(final String name,
                                                        final String argRule,
                                                        final int delay,
                                                        final R66Session session)
      throws OpenR66RunnerErrorException;
}
