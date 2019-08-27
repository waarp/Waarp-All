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
package org.waarp.openr66.proxy;

import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.openr66.proxy.configuration.ConfigurationProxyR66;
import org.waarp.openr66.proxy.configuration.FileBasedConfiguration;

import static org.waarp.openr66.protocol.configuration.Configuration.*;

/**
 *
 */
public class R66Proxy {
  private static WaarpLogger logger;

  private R66Proxy() {
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
    logger = WaarpLoggerFactory.getLogger(R66Proxy.class);
    if (args.length < 1) {
      logger.error("Needs the configuration file as first argument");
      return;
    }
    configuration = new ConfigurationProxyR66();
    if (initialize(args[0])) {
      logger.warn("Proxy OpenR66 starts for " + configuration.getHostId());
      SysErrLogger.FAKE_LOGGER
          .syserr("Proxy OpenR66 starts for " + configuration.getHostId());
    } else {
      logger
          .error("Cannot start Proxy OpenR66 for " + configuration.getHostId());
      SysErrLogger.FAKE_LOGGER.syserr(
          "Cannot start Proxy OpenR66 for " + configuration.getHostId());
      System.exit(1);//NOSONAR
    }
  }

  public static boolean initialize(String config) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(R66Proxy.class);
    }
    if (!FileBasedConfiguration
        .setConfigurationProxyFromXml(configuration, config)) {
      logger.error("Needs a correct configuration file as first argument");
      return false;
    }
    try {
      configuration.serverStartup();
    } catch (final Throwable e) {
      logger.error("Startup of Proxy is in error", e);
      WaarpShutdownHook.terminate(false);
      return false;
    }
    return true;
  }

}
