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
package org.waarp.gateway.ftp;

import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.gateway.ftp.control.ExecBusinessHandler;
import org.waarp.gateway.ftp.data.FileSystemBasedDataBusinessHandler;
import org.waarp.gateway.ftp.database.DbConstantFtp;

/**
 * Program to initialize the database for Waarp Ftp Exec
 */
public class ServerInitDatabase {
  /**
   * Internal Logger
   */
  static volatile WaarpLogger logger;

  static String sxml;
  static boolean database;

  private ServerInitDatabase() {
  }

  protected static boolean getParams(final String[] args) {
    if (args.length < 1) {
      logger.error(
          "Need at least the configuration file as first argument then optionally\n" +
          "    -initdb");
      return false;
    }
    sxml = args[0];
    for (int i = 1; i < args.length; i++) {
      if ("-initdb".equalsIgnoreCase(args[i])) {
        database = true;
        break;
      }
    }
    return true;
  }

  /**
   * @param args as config_database file [rules_directory host_authent
   *     limit_configuration]
   */
  public static void main(final String[] args) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ServerInitDatabase.class);
    }
    if (!getParams(args)) {
      logger.error(
          "Need at least the configuration file as first argument then optionally\n" +
          "    -initdb");
      if (DbConstantFtp.gatewayAdmin != null) {
        DbConstantFtp.gatewayAdmin.close();
      }
      WaarpSystemUtil.systemExit(1);
      return;
    }
    final FileBasedConfiguration configuration =
        new FileBasedConfiguration(ExecGatewayFtpServer.class,
                                   ExecBusinessHandler.class,
                                   FileSystemBasedDataBusinessHandler.class,
                                   new FilesystemBasedFileParameterImpl());
    try {
      if (!configuration.setConfigurationServerFromXml(args[0])) {
        SysErrLogger.FAKE_LOGGER.syserr("Bad main configuration");
        if (DbConstantFtp.gatewayAdmin != null) {
          DbConstantFtp.gatewayAdmin.close();
        }
        WaarpSystemUtil.systemExit(1);
        return;
      }
      if (database) {
        // Init database
        try {
          initdb();
        } catch (final WaarpDatabaseNoConnectionException e) {
          logger.error("Cannot connect to database");
          return;
        }
        SysErrLogger.FAKE_LOGGER.sysout("End creation");
      }
      SysErrLogger.FAKE_LOGGER.sysout("Load done");
    } finally {
      if (DbConstantFtp.gatewayAdmin != null) {
        DbConstantFtp.gatewayAdmin.close();
      }
    }
  }

  public static void initdb() throws WaarpDatabaseNoConnectionException {
    // Create tables: configuration, hosts, rules, runner, cptrunner
    DbConstantFtp.gatewayAdmin.getDbModel().createTables(
        DbConstantFtp.gatewayAdmin.getSession());
  }

}
