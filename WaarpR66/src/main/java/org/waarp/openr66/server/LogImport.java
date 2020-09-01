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
import org.waarp.common.utility.DetectionUtils;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import java.io.File;

import static org.waarp.common.database.DbConstant.*;

/**
 * To import logs that were exported (one should not try to import on the same
 * server to prevent strange
 * behavior).
 */
public class LogImport {
  /**
   * Internal Logger
   */
  private static WaarpLogger logger;

  /**
   * @param args
   */
  public static void main(final String[] args) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(LogImport.class);
    }
    if (args.length < 2) {
      SysErrLogger.FAKE_LOGGER
          .syserr("Need configuration file and the logfile to import");
      System.exit(1);//NOSONAR
    }
    try {
      if (!FileBasedConfiguration
          .setConfigurationServerMinimalFromXml(Configuration.configuration,
                                                args[0])) {
        logger.error("Needs a correct configuration file as first argument");
        if (admin != null) {
          admin.close();
        }
        if (DetectionUtils.isJunit()) {
          return;
        }
        ChannelUtils.stopLogger();
        System.exit(1);//NOSONAR
        return;
      }
      final long time1 = System.currentTimeMillis();
      final File logsFile = new File(args[1]);
      try {
        DbTaskRunner.loadXml(logsFile);
      } catch (final OpenR66ProtocolBusinessException e) {
        logger.error("Cannot load the logs from " + logsFile.getAbsolutePath() +
                     " since: " + e.getMessage(), e);
        if (admin != null) {
          admin.close();
        }
        if (DetectionUtils.isJunit()) {
          return;
        }
        ChannelUtils.stopLogger();
        System.exit(1);//NOSONAR
        return;
      }
      final long time2 = System.currentTimeMillis();
      final long delay = time2 - time1;
      logger.warn("LogFile imported in " + delay + " ms");
    } finally {
      if (admin != null) {
        admin.close();
      }
      System.exit(0);//NOSONAR
    }
  }
}
