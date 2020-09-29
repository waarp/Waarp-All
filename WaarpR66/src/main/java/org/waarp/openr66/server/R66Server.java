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
package org.waarp.openr66.server;

import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;

/**
 * R66Server startup main class
 */
public class R66Server {
  private static WaarpLogger logger;

  private R66Server() {
  }

  /**
   * @param args as first argument the configuration file
   */
  public static void main(final String[] args) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    logger = WaarpLoggerFactory.getLogger(R66Server.class);
    if (args.length < 1) {
      logger
          .error(Messages.getString("Configuration.NeedConfig")); //$NON-NLS-1$
      return;
    }
    try {
      WaarpShutdownHook.registerMain(R66Server.class, args);
      if (initialize(args[0])) {
        logger.warn(Messages.getString("R66Server.ServerStart") +
                    Configuration.configuration.getHostId() + " : "
                    //$NON-NLS-1$
                    + Configuration.configuration);
        SysErrLogger.FAKE_LOGGER.sysout(
            Messages.getString("R66Server.ServerStart") +
            Configuration.configuration.getHostId()); //$NON-NLS-1$
      } else {
        logger.error(Messages.getString("R66Server.CannotStart") +
                     Configuration.configuration.getHostId()); //$NON-NLS-1$
        SysErrLogger.FAKE_LOGGER.syserr(
            Messages.getString("R66Server.CannotStart") +
            Configuration.configuration.getHostId()); //$NON-NLS-1$
        System.exit(1);//NOSONAR
      }
    } catch (final Throwable e) {
      logger.error(Messages.getString("R66Server.StartError"), e); //$NON-NLS-1$
    }
  }

  public static boolean initialize(final String config) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(R66Server.class);
    }
    if (!FileBasedConfiguration
        .setConfigurationServerFromXml(Configuration.configuration, config)) {
      logger.error(
          Messages.getString("Configuration.NeedCorrectConfig")); //$NON-NLS-1$
      return false;
    }
    try {
      Configuration.configuration.serverStartup();
    } catch (final Throwable e) {
      logger.error(Messages.getString("R66Server.StartError"), e); //$NON-NLS-1$
      WaarpShutdownHook.terminate(false);
      return false;
    }
    return true;
  }
}
