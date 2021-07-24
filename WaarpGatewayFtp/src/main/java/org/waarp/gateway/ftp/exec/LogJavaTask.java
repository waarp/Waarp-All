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
package org.waarp.gateway.ftp.exec;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

/**
 *
 */
public class LogJavaTask implements GatewayRunnable {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(LogJavaTask.class);

  boolean waitForValidation;
  boolean useLocalExec;
  int delay;
  String[] args;

  /**
   *
   */
  public LogJavaTask() {
    // nothing
  }

  @Override
  public void run() {
    final StringBuilder builder = new StringBuilder();
    for (final String arg : args) {
      builder.append(arg).append(' ');
    }
    switch (delay) {
      case 1:
        logger.debug("{}", builder);
        break;
      case 2:
        logger.info("{}", builder);
        break;
      case 4:
        logger.error("{}", builder);
        break;
      default:
        logger.warn("{}", builder);
        break;
    }
  }

  @Override
  public final void setArgs(final boolean waitForValidation,
                            final boolean useLocalExec, final int delay,
                            final String[] args) {
    this.waitForValidation = waitForValidation;
    this.useLocalExec = useLocalExec;
    this.delay = delay;
    this.args = args;
  }

  @Override
  public final int getFinalStatus() {
    return 0;
  }

}
