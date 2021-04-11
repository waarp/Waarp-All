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
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.context.task.extension.AbstractExtendedTaskFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This enum class references all available tasks.
 * <p>
 * If a new task type is to be created, this is the place where it should be
 * referenced.
 */
public enum TaskType {
  LOG, MOVE, MOVERENAME, COPY, COPYRENAME, EXEC, EXECMOVE, LINKRENAME, TRANSFER,
  VALIDFILEPATH, DELETE, TAR, ZIP, EXECOUTPUT, RESCHEDULE, EXECJAVA, TRANSCODE,
  SNMP, FTP, RENAME, RESTART, UNZEROED, CHMOD, CHKFILE, ICAP, EXTENDED;

  public static final String UNVALID_TASK = "Unvalid Task: ";

  public static final String NAME_UNKNOWN = "name unknown: ";

  private static final Map<String, AbstractExtendedTaskFactory>
      extendedTaskType = new HashMap<String, AbstractExtendedTaskFactory>();

  final int type;

  final String name;
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(TaskType.class);

  TaskType() {
    type = ordinal();
    name = name();
  }

  /**
   * Allow to add extended task that will be denoted as EXTENDED
   *
   * @param associatedTaskNames if a previously TASK name was already set, it is replaced by the new Factory, including standard TASK_TYPE
   * @param extendedTaskFactory the Factory to use to create the associated Task with its name
   */
  public static void addExtendedTaskFactory(Set<String> associatedTaskNames,
                                            AbstractExtendedTaskFactory extendedTaskFactory) {
    for (String associatedName : associatedTaskNames) {
      logger.debug("Add {} with {}", associatedName,
                   extendedTaskFactory.getName());
      extendedTaskType.put(associatedName, extendedTaskFactory);
    }
  }

  /**
   * Check if the given Task exists
   *
   * @param task
   *
   * @return True if this task is known
   */
  public static boolean isValidTask(String task) {
    try {
      TaskType.valueOf(task);
      return true;
    } catch (final IllegalArgumentException e) {
      logger
          .debug("{} is in keys: {}", task, extendedTaskType.containsKey(task));
      return extendedTaskType.containsKey(task);
    }
  }

  /**
   * @param type
   * @param argRule
   * @param delay
   * @param session
   *
   * @return the corresponding AbstractTask
   *
   * @throws OpenR66RunnerErrorException
   */
  private static AbstractTask getTaskFromId(final TaskType type,
                                            final String argRule,
                                            final int delay,
                                            final R66Session session)
      throws OpenR66RunnerErrorException {
    switch (type) {
      case LOG:
        return new LogTask(argRule, delay,
                           session.getRunner().getFileInformation(), session);
      case MOVE:
        return new MoveTask(argRule, delay,
                            session.getRunner().getFileInformation(), session);
      case MOVERENAME:
        return new MoveRenameTask(argRule, delay,
                                  session.getRunner().getFileInformation(),
                                  session);
      case COPY:
        return new CopyTask(argRule, delay,
                            session.getRunner().getFileInformation(), session);
      case COPYRENAME:
        return new CopyRenameTask(argRule, delay,
                                  session.getRunner().getFileInformation(),
                                  session);
      case EXEC:
        return new ExecTask(argRule, delay,
                            session.getRunner().getFileInformation(), session);
      case EXECMOVE:
        return new ExecMoveTask(argRule, delay,
                                session.getRunner().getFileInformation(),
                                session);
      case LINKRENAME:
        return new LinkRenameTask(argRule, delay,
                                  session.getRunner().getFileInformation(),
                                  session);
      case TRANSFER:
        return new TransferTask(argRule, delay,
                                session.getRunner().getFileInformation(),
                                session);
      case VALIDFILEPATH:
        return new ValidFilePathTask(argRule, delay,
                                     session.getRunner().getFileInformation(),
                                     session);
      case DELETE:
        return new DeleteTask(argRule, delay,
                              session.getRunner().getFileInformation(),
                              session);
      case TAR:
        return new TarTask(argRule, delay,
                           session.getRunner().getFileInformation(), session);
      case ZIP:
        return new ZipTask(argRule, delay,
                           session.getRunner().getFileInformation(), session);
      case EXECOUTPUT:
        return new ExecOutputTask(argRule, delay,
                                  session.getRunner().getFileInformation(),
                                  session);
      case RESCHEDULE:
        return new RescheduleTransferTask(argRule, delay, session.getRunner()
                                                                 .getFileInformation(),
                                          session);
      case EXECJAVA:
        return new ExecJavaTask(argRule, delay,
                                session.getRunner().getFileInformation(),
                                session);
      case TRANSCODE:
        return new TranscodeTask(argRule, delay,
                                 session.getRunner().getFileInformation(),
                                 session);
      case SNMP:
        return new SnmpTask(argRule, delay,
                            session.getRunner().getFileInformation(), session);
      case FTP:
        return new FtpTransferTask(argRule, delay,
                                   session.getRunner().getFileInformation(),
                                   session);
      case RENAME:
        return new RenameTask(argRule, delay,
                              session.getRunner().getFileInformation(),
                              session);
      case RESTART:
        return new RestartServerTask(argRule, delay,
                                     session.getRunner().getFileInformation(),
                                     session);
      case UNZEROED:
        return new UnzeroedFileTask(argRule, delay,
                                    session.getRunner().getFileInformation(),
                                    session);
      case CHMOD:
        return new ChModTask(argRule, delay,
                             session.getRunner().getFileInformation(), session);
      case CHKFILE:
        return new FileCheckTask(argRule, delay,
                                 session.getRunner().getFileInformation(),
                                 session);
      case ICAP:
        return new IcapTask(argRule, delay,
                            session.getRunner().getFileInformation(), session);
      case EXTENDED:
        // Should not arrived here
      default:
        logger.error(NAME_UNKNOWN + type.name);
        throw new OpenR66RunnerErrorException(UNVALID_TASK + type.name);
    }
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
  public static AbstractTask getTaskFromId(final String name,
                                           final String argRule,
                                           final int delay,
                                           final R66Session session)
      throws OpenR66RunnerErrorException {
    // EXTENDED first
    AbstractExtendedTaskFactory factory = extendedTaskType.get(name);
    if (factory != null) {
      return factory.getTaskFromId(name, argRule, delay, session);
    }
    final TaskType type;
    try {
      type = valueOf(name);
    } catch (final NullPointerException e) {
      logger.error("name empty " + name);
      throw new OpenR66RunnerErrorException(UNVALID_TASK + name);
    } catch (final IllegalArgumentException e) {
      logger.error(NAME_UNKNOWN + name);
      throw new OpenR66RunnerErrorException(UNVALID_TASK + name);
    }
    return getTaskFromId(type, argRule, delay, session);
  }

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
  public static AbstractTask getTaskFromIdForBusiness(final String name,
                                                      final String argRule,
                                                      final int delay,
                                                      final R66Session session)
      throws OpenR66RunnerErrorException {
    // EXTENDED first
    AbstractExtendedTaskFactory factory = extendedTaskType.get(name);
    if (factory != null) {
      return factory.getTaskFromIdForBusiness(name, argRule, delay, session);
    }
    final TaskType type;
    try {
      type = valueOf(name);
    } catch (final NullPointerException e) {
      logger.error("name empty " + name);
      throw new OpenR66RunnerErrorException(UNVALID_TASK + name);
    } catch (final IllegalArgumentException e) {
      // FIXME EXTENDED
      logger.error(NAME_UNKNOWN + name);
      throw new OpenR66RunnerErrorException(UNVALID_TASK + name);
    }
    switch (type) {
      case LOG:
        int newdelay = delay;
        if (newdelay == 0) {
          newdelay = 1;
        }
        return new LogTask(argRule, newdelay, "", session);
      case EXEC:
        return new ExecTask(argRule, delay, "", session);
      case TRANSFER:
        return new TransferTask(argRule, delay, "", session);
      case TAR:
        return new TarTask(argRule, delay, "", session);
      case ZIP:
        return new ZipTask(argRule, delay, "", session);
      case EXECOUTPUT:
        return new ExecOutputTask(argRule, delay, "", session);
      case EXECJAVA:
        return new ExecJavaTask(argRule, delay, "", session);
      case SNMP:
        return new SnmpTask(argRule, delay, "", session);
      case FTP:
        return new FtpTransferTask(argRule, delay, "", session);
      case RESTART:
        return new RestartServerTask(argRule, delay, "", session);
      case ICAP:
        return new IcapTask(argRule, delay, "", session);
      case EXTENDED:
        // Not allowed
      case MOVE:
      case MOVERENAME:
      case COPY:
      case COPYRENAME:
      case EXECMOVE:
      case LINKRENAME:
      case VALIDFILEPATH:
      case DELETE:
      case RESCHEDULE:
      case TRANSCODE:
      case RENAME:
      case UNZEROED:
      case CHMOD:
      case CHKFILE:
        throw new OpenR66RunnerErrorException(UNVALID_TASK + type.name);
      default:
        logger.error(NAME_UNKNOWN + type.name);
        throw new OpenR66RunnerErrorException(UNVALID_TASK + type.name);
    }
  }
}
