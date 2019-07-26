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
package org.waarp.ftp.core.utils;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.ftp.core.config.FtpConfiguration;

import java.util.TimerTask;

/**
 * Timer Task used mainly when the server is going to shutdown in order to be
 * sure the program exit.
 *
 *
 */
public class FtpTimerTask extends TimerTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpTimerTask.class);

  /**
   * EXIT type (System.exit(1))
   */
  public static final int TIMER_EXIT = 1;
  /**
   * Finalize Control connection
   */
  public static final int TIMER_CONTROL = 2;

  /**
   * Type of execution in run() method
   */
  private final int type;
  /**
   * Configuration
   */
  private FtpConfiguration configuration;

  /**
   * Constructor from type
   *
   * @param type
   */
  public FtpTimerTask(int type) {
    this.type = type;
  }

  @Override
  public void run() {
    switch (type) {
      case TIMER_EXIT:
        logger.error("System will force EXIT");
        //FBGEXIT DetectionUtils.SystemExit(0);
        break;
      case TIMER_CONTROL:
        logger.info("Exit Shutdown Command");
        FtpChannelUtils.terminateCommandChannels(getConfiguration());
        logger.warn("Exit end of Command Shutdown");
        // FtpChannelUtils.stopLogger();
        break;
      default:
        logger.info("Type unknown in TimerTask");
    }
  }

  /**
   * @return the configuration
   */
  public FtpConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * @param configuration the configuration to set
   */
  public void setConfiguration(FtpConfiguration configuration) {
    this.configuration = configuration;
  }
}
