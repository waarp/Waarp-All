/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.gateway.ftp;

import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.file.filesystembased.FilesystemBasedFileParameterImpl;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.ftp.core.utils.FtpChannelUtils;
import org.waarp.gateway.ftp.config.FileBasedConfiguration;
import org.waarp.gateway.ftp.control.ExecBusinessHandler;
import org.waarp.gateway.ftp.data.FileSystemBasedDataBusinessHandler;
import org.waarp.gateway.ftp.database.DbConstant;

/**
 * Program to initialize the database for Waarp Ftp Exec
 * 
 * @author Frederic Bregier
 * 
 */
public class ServerInitDatabase {
    /**
     * Internal Logger
     */
    static volatile WaarpLogger logger;

    static String sxml = null;
    static boolean database = false;

    protected static boolean getParams(String[] args) {
        if (args.length < 1) {
            logger.error("Need at least the configuration file as first argument then optionally\n"
                    +
                    "    -initdb");
            return false;
        }
        sxml = args[0];
        for (int i = 1; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-initdb")) {
                database = true;
            }
        }
        return true;
    }

    /**
     * @param args
     *            as config_database file [rules_directory host_authent limit_configuration]
     */
    public static void main(String[] args) {
        WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
        if (logger == null) {
            logger = WaarpLoggerFactory.getLogger(ServerInitDatabase.class);
        }
        if (!getParams(args)) {
            logger.error("Need at least the configuration file as first argument then optionally\n"
                    +
                    "    -initdb");
            if (DbConstant.gatewayAdmin != null && DbConstant.gatewayAdmin.isActive()) {
                DbConstant.gatewayAdmin.close();
            }
            if (DetectionUtils.isJunit()) {
                return;
            }
            FtpChannelUtils.stopLogger();
            System.exit(1);
        }
        FileBasedConfiguration configuration = new FileBasedConfiguration(
                ExecGatewayFtpServer.class, ExecBusinessHandler.class,
                FileSystemBasedDataBusinessHandler.class,
                new FilesystemBasedFileParameterImpl());
        try {
            if (!configuration.setConfigurationServerFromXml(args[0])) {
                System.err.println("Bad main configuration");
                if (DbConstant.gatewayAdmin != null) {
                    DbConstant.gatewayAdmin.close();
                }
                if (DetectionUtils.isJunit()) {
                    return;
                }
                FtpChannelUtils.stopLogger();
                System.exit(1);
                return;
            }
            if (database) {
                // Init database
                try {
                    initdb();
                } catch (WaarpDatabaseNoConnectionException e) {
                    logger.error("Cannot connect to database");
                    return;
                }
                System.out.println("End creation");
            }
            System.out.println("Load done");
        } finally {
            if (DbConstant.gatewayAdmin != null) {
                DbConstant.gatewayAdmin.close();
            }
        }
    }

    public static void initdb() throws WaarpDatabaseNoConnectionException {
        // Create tables: configuration, hosts, rules, runner, cptrunner
        DbConstant.gatewayAdmin.getDbModel().createTables(DbConstant.gatewayAdmin.getSession());
    }

}
