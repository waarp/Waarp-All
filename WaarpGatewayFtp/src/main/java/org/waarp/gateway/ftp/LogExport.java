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

import org.waarp.common.command.ReplyCode;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.file.DirInterface;
import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.gateway.ftp.control.ExecBusinessHandler;
import org.waarp.gateway.ftp.data.FileSystemBasedDataBusinessHandler;
import org.waarp.gateway.ftp.database.DbConstantFtp;
import org.waarp.gateway.ftp.database.data.DbTransferLog;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Timestamp;

/**
 * Program to export log history from database to a file and optionally purge
 * exported entries.
 */
public class LogExport {
  /**
   * Command Flags
   */
  private static boolean purge;
  private static boolean correctOnly;
  private static String destinationPath;
  protected static Timestamp start;
  protected static Timestamp stop;

  /**
   * Internal pointer to the destination writer object
   */
  private static Writer destinationWriter;

  /**
   * Internal pointer to the configuration
   */
  private static FileBasedConfiguration config;

  /**
   * Usage for the command
   */
  private static final String USAGE =
      "Need at least the configuration file as first argument then optionally\n" +
      "    -correctOnly      Only exports successful transfers\n" +
      "    -purge            Purge exported transfers\n" +
      "    -out [filepath|-] The path to the file created\n" +
      "                      Use '-' for stdout\n" +
      "    -start timestamp  in format yyyyMMddHHmmssSSS possibly truncated and where one of ':-. ' can be separators\n" +
      "    -stop timestamp   in same format than start\n";

  private LogExport() {
  }

  /**
   * Verifies command line arguments and initialize internals (mainly config)
   *
   * @param args command line arguments
   *
   * @return the result of the initialization. if an error occured at this
   *     stage, return value will be false
   */
  protected static boolean initialize(final String[] args) {
    if (!getParams(args)) {
      return false;
    }

    config = new FileBasedConfiguration(ExecGatewayFtpServer.class,
                                        ExecBusinessHandler.class,
                                        FileSystemBasedDataBusinessHandler.class,
                                        new FilesystemBasedFileParameterImpl());

    if (!config.setConfigurationServerFromXml(args[0])) {
      SysErrLogger.FAKE_LOGGER.syserr("Bad main configuration");
      if (DbConstantFtp.gatewayAdmin != null) {
        DbConstantFtp.gatewayAdmin.close();
      }
      return false;
    }

    FileBasedConfiguration.fileBasedConfiguration = config;

    return setDestinationWriter();
  }

  /**
   * Creates and sets destinationWriter according to the following logic based
   * on the value of the '-out'
   * command line argument: - if no value is given, a default path is computed
   * (the same as online export, i.e.
   * in [data directory]/[admin name]) - if the value '-' is given,
   * destinationWriter is the standard output
   * (System.out) - if any other value is given, then it is considered as the
   * path to the destination file
   *
   * @return a bollean indicating the success in opening the Writer.
   */
  public static boolean setDestinationWriter() {
    if (destinationPath == null) {
      destinationPath = config.getBaseDirectory() + DirInterface.SEPARATOR +
                        config.getAdminName() + DirInterface.SEPARATOR +
                        config.getHostId() + "_logs_" +
                        System.currentTimeMillis() + ".xml";
    }

    if ("-".equalsIgnoreCase(destinationPath)) {
      destinationWriter = new OutputStreamWriter(System.out);
    } else {
      try {
        destinationWriter = new FileWriter(destinationPath);
      } catch (final IOException e) {
        SysErrLogger.FAKE_LOGGER
            .syserr("Cannot open out file " + destinationPath);
        return false;
      }
    }
    return true;
  }

  /**
   * Parses command line arguments
   *
   * @param args command line arguments to parse
   *
   * @return [description]
   */
  protected static boolean getParams(final String[] args) {
    if (args.length < 1) {
      SysErrLogger.FAKE_LOGGER.syserr(USAGE);
      return false;
    }

    for (int i = 1; i < args.length; i++) {
      if ("-purge".equalsIgnoreCase(args[i])) {
        purge = true;
      } else if ("-correctOnly".equalsIgnoreCase(args[i])) {
        correctOnly = true;
      } else if ("-out".equalsIgnoreCase(args[i])) {
        i++;
        if (i >= args.length ||
            args[i].charAt(0) == '-' && args[i].length() > 1) {
          SysErrLogger.FAKE_LOGGER
              .syserr("Error: -out needs a value.\n\n" + USAGE);
          return false;
        }
        destinationPath = args[i].trim();
      } else if ("-start".equalsIgnoreCase(args[i])) {
        i++;
        if (i >= args.length ||
            args[i].charAt(0) == '-' && args[i].length() > 1) {
          SysErrLogger.FAKE_LOGGER
              .syserr("Error: -start needs a value.\n\n" + USAGE);
          return false;
        }
        start = WaarpStringUtils.fixDate(args[i]);
      } else if ("-stop".equalsIgnoreCase(args[i])) {
        i++;
        if (i >= args.length ||
            args[i].charAt(0) == '-' && args[i].length() > 1) {
          SysErrLogger.FAKE_LOGGER
              .syserr("Error: -stop needs a value.\n\n" + USAGE);
          return false;
        }
        stop = WaarpStringUtils.fixDate(args[i]);
      }
    }
    return true;
  }

  /**
   * Main logic for the command.
   *
   * @return an error message or null
   */
  protected static String run() {
    ReplyCode status = null;
    if (correctOnly) {
      status = ReplyCode.REPLY_226_CLOSING_DATA_CONNECTION;
    }

    final DbPreparedStatement preparedStatement;
    try {
      preparedStatement = DbTransferLog
          .getLogPrepareStament(DbConstantFtp.gatewayAdmin.getSession(), start,
                                stop, status);

    } catch (final WaarpDatabaseNoConnectionException e) {
      return "An error occured while connecting to the database: " +
             e.getMessage();

    } catch (final WaarpDatabaseSqlException e) {
      return "An error occured with the database: " + e.getMessage();
    }

    return DbTransferLog
        .saveDbTransferLogFile(preparedStatement, destinationWriter, purge);
  }

  /**
   * Command Entry point
   *
   * @param args command line arguments
   */
  public static void main(final String[] args) {
    try {
      if (!initialize(args)) {
        WaarpSystemUtil.systemExit(1);
        return;
      }

      final String message = run();

      if (message.contains("successfully")) {
        WaarpSystemUtil.systemExit(0);
      } else {
        SysErrLogger.FAKE_LOGGER.syserr(message);
        WaarpSystemUtil.systemExit(1);
      }

    } finally {
      if (DbConstantFtp.gatewayAdmin != null) {
        DbConstantFtp.gatewayAdmin.close();
      }
    }
  }
}