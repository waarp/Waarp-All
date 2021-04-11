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
package org.waarp.openr66.s3.taskfactory;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.AbstractTask;
import org.waarp.openr66.context.task.S3DeleteTask;
import org.waarp.openr66.context.task.S3GetAndDeleteTask;
import org.waarp.openr66.context.task.S3GetTask;
import org.waarp.openr66.context.task.S3PutAndR66DeleteTask;
import org.waarp.openr66.context.task.S3PutTask;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.context.task.extension.AbstractExtendedTaskFactory;

import java.util.HashSet;
import java.util.Set;

import static org.waarp.openr66.context.task.TaskType.*;

/**
 * Define the following tasks: S3GET, S3PUT, S3DELETE, S3PUTR66DELETE, S3GETDELETE
 */
public class S3TaskFactory extends AbstractExtendedTaskFactory {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(S3TaskType.class);

  public enum S3TaskType {
    S3GET, S3PUT, S3DELETE, S3PUTR66DELETE, S3GETDELETE;

    final String name;

    S3TaskType() {
      name = name();
    }
  }

  /**
   * Added all Tasks to Extended Task Types
   */
  public S3TaskFactory() {
    final S3TaskType[] s3TaskTypes = S3TaskType.values();
    final Set<String> s3TaskNames = new HashSet<>(s3TaskTypes.length);
    for (final S3TaskType s3TaskType : s3TaskTypes) {
      s3TaskNames.add(s3TaskType.name);
    }
    create(s3TaskNames);
  }

  private AbstractTask getTaskFromId(final S3TaskType type,
                                     final String argRule, final int delay,
                                     final R66Session session)
      throws OpenR66RunnerErrorException {
    switch (type) {
      case S3GET:
        return new S3GetTask(argRule, delay,
                             session.getRunner().getFileInformation(), session);
      case S3GETDELETE:
        return new S3GetAndDeleteTask(argRule, delay,
                                      session.getRunner().getFileInformation(),
                                      session);
      case S3PUT:
        return new S3PutTask(argRule, delay,
                             session.getRunner().getFileInformation(), session);
      case S3DELETE:
        return new S3DeleteTask(argRule, delay,
                                session.getRunner().getFileInformation(),
                                session);
      case S3PUTR66DELETE:
        return new S3PutAndR66DeleteTask(argRule, delay, session.getRunner()
                                                                .getFileInformation(),
                                         session);
      default:
        logger.error(NAME_UNKNOWN + type.name);
        throw new OpenR66RunnerErrorException(UNVALID_TASK + type.name);
    }
  }

  @Override
  public String getName() {
    return S3TaskFactory.class.getSimpleName();
  }

  @Override
  public AbstractTask getTaskFromId(final String name, final String argRule,
                                    final int delay, final R66Session session)
      throws OpenR66RunnerErrorException {
    final S3TaskType type;
    try {
      type = S3TaskType.valueOf(name);
    } catch (final NullPointerException e) {
      logger.error("name empty " + name);
      throw new OpenR66RunnerErrorException(UNVALID_TASK + name);
    } catch (final IllegalArgumentException e) {
      logger.error(NAME_UNKNOWN + name);
      throw new OpenR66RunnerErrorException(UNVALID_TASK + name);
    }
    return getTaskFromId(type, argRule, delay, session);
  }

  @Override
  public AbstractTask getTaskFromIdForBusiness(final String name,
                                               final String argRule,
                                               final int delay,
                                               final R66Session session)
      throws OpenR66RunnerErrorException {
    final S3TaskType type;
    try {
      type = S3TaskType.valueOf(name);
    } catch (final NullPointerException e) {
      logger.error("name empty " + name);
      throw new OpenR66RunnerErrorException(UNVALID_TASK + name);
    } catch (final IllegalArgumentException e) {
      logger.error(NAME_UNKNOWN + name);
      throw new OpenR66RunnerErrorException(UNVALID_TASK + name);
    }
    return getTaskFromId(type, argRule, delay, session);
  }
}
