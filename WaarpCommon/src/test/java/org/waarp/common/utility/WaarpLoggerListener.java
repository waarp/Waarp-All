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

package org.waarp.common.utility;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.DateUtils;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

/**
 * WaarpLoggerListerner for Ant
 */
public class WaarpLoggerListener implements BuildListener {
  /**
   * Size of left-hand column for right-justified task name.
   *
   * @see #messageLogged(BuildEvent)
   */
  public static final int LEFT_COLUMN_SIZE = 12;
  /**
   * Line separator
   */
  protected static final String lSep = StringUtils.LINE_SEP;
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpLoggerListener.class);
  /**
   * Lowest level of message to write out
   */
  private int msgOutputLevel = Project.MSG_WARN;
  // CheckStyle:ConstantNameCheck OFF - bc
  private WaarpLogLevel wlevel = WaarpLogLevel.WARN;
  // CheckStyle:ConstantNameCheck ON
  /**
   * Time of the start of the build
   */
  private long startTime = System.currentTimeMillis();

  /**
   * Sets the highest level of message this logger should respond to.
   *
   * Only messages with a message level lower than or equal to the given level should be written to the log.
   * <p>
   * Constants for the message levels are in the {@link Project Project} class. The order of the levels, from
   * least to most verbose, is {@code MSG_ERR}, {@code MSG_WARN}, {@code MSG_INFO},
   * {@code MSG_VERBOSE}, {@code MSG_DEBUG}.
   * <p>
   * The default message level for DefaultLogger is Project.MSG_ERR.
   *
   * @param level the logging level for the logger.
   */
  public void setMessageOutputLevel(int level) {
    msgOutputLevel = level;
    switch (msgOutputLevel) {
      case Project.MSG_DEBUG:
        wlevel = WaarpLogLevel.DEBUG;
        break;
      case Project.MSG_VERBOSE:
        wlevel = WaarpLogLevel.TRACE;
        break;
      case Project.MSG_INFO:
        wlevel = WaarpLogLevel.INFO;
        break;
      case Project.MSG_WARN:
        wlevel = WaarpLogLevel.WARN;
        break;
      case Project.MSG_ERR:
        wlevel = WaarpLogLevel.ERROR;
        break;
      default:
        wlevel = WaarpLogLevel.NONE;
    }
  }

  @Override
  public void buildStarted(BuildEvent event) {
    startTime = System.currentTimeMillis();
  }

  @Override
  public void buildFinished(BuildEvent event) {
    final Throwable error = event.getException();
    final StringBuilder message = new StringBuilder();
    message.append(StringUtils.LINE_SEP);
    if (error == null) {
      message.append(getBuildSuccessfulMessage());
    } else {
      message.append(getBuildFailedMessage());
      message.append(StringUtils.LINE_SEP);

      if (Project.MSG_VERBOSE <= msgOutputLevel ||
          !(error instanceof BuildException)) {
        message
            .append(org.apache.tools.ant.util.StringUtils.getStackTrace(error));
      } else {
        message.append(error).append(lSep);
      }
    }
    message.append(StringUtils.LINE_SEP);
    message.append("Total time: ");
    message.append(formatTime(System.currentTimeMillis() - startTime));

    final String msg = message.toString();
    if (error == null) {
      printMessage(msg, Project.MSG_VERBOSE);
    } else {
      printMessage(msg, Project.MSG_ERR);
    }
  }

  /**
   * This is an override point: the message that indicates that a build succeeded. Subclasses can change/enhance
   * the message.
   *
   * @return The classic "BUILD SUCCESSFUL"
   */
  protected String getBuildSuccessfulMessage() {
    return "BUILD SUCCESSFUL";
  }

  /**
   * This is an override point: the message that indicates whether a build failed. Subclasses can change/enhance
   * the message.
   *
   * @return The classic "BUILD FAILED"
   */
  protected String getBuildFailedMessage() {
    return "BUILD FAILED";
  }

  /**
   * Convenience method to format a specified length of time.
   *
   * @param millis Length of time to format, in milliseconds.
   *
   * @return the time as a formatted string.
   *
   * @see DateUtils#formatElapsedTime(long)
   */
  protected static String formatTime(final long millis) {
    return DateUtils.formatElapsedTime(millis);
  }

  /**
   * Prints a message to log.
   *
   * @param message The message to print. Should not be {@code null}.
   * @param priority The priority of the message. (Ignored in this implementation.)
   */
  protected void printMessage(final String message, final int priority) {
    logger.log(wlevel, message);
  }

  @Override
  public void targetStarted(BuildEvent event) {
    if (Project.MSG_INFO <= msgOutputLevel &&
        event.getTarget().getName() != null &&
        !event.getTarget().getName().isEmpty()) {
      final String msg =
          StringUtils.LINE_SEP + event.getTarget().getName() + ':';

      printMessage(msg, event.getPriority());
    }
  }

  @Override
  public void targetFinished(BuildEvent event) {
  }

  @Override
  public void taskStarted(BuildEvent event) {
  }

  @Override
  public void taskFinished(BuildEvent event) {
  }

  @Override
  public void messageLogged(BuildEvent event) {
    final int priority = event.getPriority();
    // Filter out messages based on priority
    if (priority <= msgOutputLevel) {

      final StringBuilder message = new StringBuilder();
      if (event.getTask() != null) {
        // Print out the name of the task if we're in one
        final String name = event.getTask().getTaskName();
        String label = '[' + name + "] ";
        final int size = LEFT_COLUMN_SIZE - label.length();
        final StringBuilder tmp = new StringBuilder();
        for (int i = 0; i < size; i++) {
          tmp.append(' ');
        }
        tmp.append(label);
        label = tmp.toString();

        try {
          final BufferedReader r =
              new BufferedReader(new StringReader(event.getMessage()));
          String line = r.readLine();
          boolean first = true;
          do {
            if (first) {
              if (line == null) {
                message.append(label);
                break;
              }
            } else {
              message.append(StringUtils.LINE_SEP);
            }
            first = false;
            message.append(label).append(line);
            line = r.readLine();
          } while (line != null);
        } catch (final IOException e) {
          // shouldn't be possible
          message.append(label).append(event.getMessage());
        }
      } else {
        message.append(event.getMessage());
      }
      final Throwable ex = event.getException();
      if (Project.MSG_DEBUG <= msgOutputLevel && ex != null) {
        message.append(org.apache.tools.ant.util.StringUtils.getStackTrace(ex));
      }

      final String msg = message.toString();
      printMessage(msg, priority);
    }
  }
}
