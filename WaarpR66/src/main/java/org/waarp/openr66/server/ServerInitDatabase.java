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

import org.apache.commons.io.FileUtils;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.openr66.configuration.AuthenticationFileBasedConfiguration;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.configuration.RuleFileBasedConfiguration;
import org.waarp.openr66.database.data.DbHostConfiguration;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import java.io.File;
import java.io.IOException;

import static org.waarp.common.database.DbConstant.*;

/**
 * Utility class to initiate the database for a server
 */
public class ServerInitDatabase {
  /**
   * Internal Logger
   */
  static volatile WaarpLogger logger;

  protected static final String _INFO_ARGS =
      Messages.getString("ServerInitDatabase.Help");

  static String sxml;
  static boolean database;
  static boolean upgradeDb;
  static String sbusiness;
  static String salias;
  static String sroles;
  static String sdirconfig;
  static String shostauth;
  static String slimitconfig;

  private ServerInitDatabase() {
  }

  protected static boolean getParams(String[] args) {
    if (args.length < 1) {
      logger.error(_INFO_ARGS);
      return false;
    }
    sxml = args[0];
    for (int i = 1; i < args.length; i++) {
      if ("-initdb".equalsIgnoreCase(args[i])) {
        database = true;
        FileBasedConfiguration.autoupgrade = false;
        upgradeDb = true;
      } else if ("-upgradeDb".equalsIgnoreCase(args[i])) {
        upgradeDb = true;
      } else if ("-loadBusiness".equalsIgnoreCase(args[i])) {
        i++;
        sbusiness = args[i];
      } else if ("-loadAlias".equalsIgnoreCase(args[i])) {
        i++;
        salias = args[i];
      } else if ("-loadRoles".equalsIgnoreCase(args[i])) {
        i++;
        sroles = args[i];
      } else if ("-dir".equalsIgnoreCase(args[i])) {
        i++;
        sdirconfig = args[i];
      } else if ("-limit".equalsIgnoreCase(args[i])) {
        i++;
        slimitconfig = args[i];
      } else if ("-auth".equalsIgnoreCase(args[i])) {
        i++;
        shostauth = args[i];
      }
    }
    return true;
  }

  /**
   * @param args as config_database file [rules_directory host_authent
   *     limit_configuration]
   */
  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ServerInitDatabase.class);
    }
    if (!getParams(args)) {
      SysErrLogger.FAKE_LOGGER.sysout(_INFO_ARGS);
      if (admin != null) {
        admin.close();
      }
      if (DetectionUtils.isJunit()) {
        return;
      }
      ChannelUtils.stopLogger();
      System.exit(2);//NOSONAR
    }

    try {
      if (!FileBasedConfiguration
          .setConfigurationInitDatabase(Configuration.configuration, args[0],
                                        database)) {
        System.err
            .format(Messages.getString("Configuration.NeedCorrectConfig"));
        SysErrLogger.FAKE_LOGGER.syserr();
        if (DetectionUtils.isJunit()) {
          return;
        }
        ChannelUtils.stopLogger();
        System.exit(2);//NOSONAR
      }
      if (database) {
        // Init database
        System.out
            .format(Messages.getString("ServerInitDatabase.Create.Start"));
        SysErrLogger.FAKE_LOGGER.sysout();
        initdb();
        System.out.format(Messages.getString("ServerInitDatabase.Create.Done"));
        SysErrLogger.FAKE_LOGGER.sysout();
      }
      if (upgradeDb) {
        // try to upgrade DB schema
        System.out
            .format(Messages.getString("ServerInitDatabase.Upgrade.Start"));
        SysErrLogger.FAKE_LOGGER.sysout();
        // TODO Split check for update and upgrade actions
        if (!upgradedb()) {
          SysErrLogger.FAKE_LOGGER.syserr(
              Messages.getString("ServerInitDatabase.SchemaNotUptodate"));
          SysErrLogger.FAKE_LOGGER.syserr();
          if (DetectionUtils.isJunit()) {
            return;
          }
          System.exit(1);//NOSONAR
        }
        System.out
            .format(Messages.getString("ServerInitDatabase.Upgrade.Done"));
        SysErrLogger.FAKE_LOGGER.sysout();
      }
      // Try to load some element directly into database from first
      // configuration file
      FileBasedConfiguration
          .setConfigurationServerFromXml(Configuration.configuration, args[0]);
      if (sdirconfig != null) {
        // load Rules
        System.out
            .format(Messages.getString("ServerInitDatabase.LoadRule.Start"),
                    sdirconfig);
        SysErrLogger.FAKE_LOGGER.sysout();
        final File dirConfig = new File(sdirconfig);
        if (dirConfig.isDirectory()) {
          if (!loadRules(dirConfig)) {
            System.out.format(
                Messages.getString("ServerInitDatabase.LoadRule.Failed"));
            if (DetectionUtils.isJunit()) {
              return;
            }
            System.exit(1);//NOSONAR
          }
        } else {
          System.err.format(
              Messages.getString("ServerInitDatabase.LoadRule.NoDirectory"),
              sdirconfig);
          SysErrLogger.FAKE_LOGGER.syserr();
          if (DetectionUtils.isJunit()) {
            return;
          }
          System.exit(1);//NOSONAR
        }
        System.out
            .format(Messages.getString("ServerInitDatabase.LoadRule.Done"));
        SysErrLogger.FAKE_LOGGER.sysout();
      }
      if (shostauth != null) {
        // Load Host Authentications
        System.out
            .format(Messages.getString("ServerInitDatabase.LoadAuth.Start"),
                    shostauth);
        SysErrLogger.FAKE_LOGGER.sysout();
        if (!loadHostAuth(shostauth)) {
          System.err
              .format(Messages.getString("ServerInitDatabase.LoadAuth.Failed"));
          SysErrLogger.FAKE_LOGGER.syserr();
          if (DetectionUtils.isJunit()) {
            return;
          }
          System.exit(1);//NOSONAR
        }
        System.out
            .format(Messages.getString("ServerInitDatabase.LoadAuth.Done"));
        SysErrLogger.FAKE_LOGGER.sysout();
      }
      if (slimitconfig != null) {
        // Load configuration
        System.out
            .format(Messages.getString("ServerInitDatabase.LoadLimit.Start"),
                    slimitconfig);
        SysErrLogger.FAKE_LOGGER.sysout();
        if (!FileBasedConfiguration
            .setConfigurationLoadLimitFromXml(Configuration.configuration,
                                              slimitconfig)) {
          System.err.format(
              Messages.getString("ServerInitDatabase.LoadLimit.Failed"));
          SysErrLogger.FAKE_LOGGER.syserr();
          if (DetectionUtils.isJunit()) {
            return;
          }
          System.exit(1);//NOSONAR
        }
        System.out
            .format(Messages.getString("ServerInitDatabase.LoadLimit.Done"));
        SysErrLogger.FAKE_LOGGER.sysout();
      }
      if (sbusiness != null || salias != null || sroles != null) {
        if (sbusiness != null) {
          sbusiness = getXMLFromFile(sbusiness);
        }
        if (salias != null) {
          salias = getXMLFromFile(salias);
        }
        if (sroles != null) {
          sroles = getXMLFromFile(sroles);
        }
        DbHostConfiguration hostConfiguration;
        try {
          hostConfiguration =
              new DbHostConfiguration(Configuration.configuration.getHostId());
          if (salias != null && !salias.isEmpty()) {
            hostConfiguration.setAliases(salias);
          }
          if (sbusiness != null && !sbusiness.isEmpty()) {
            hostConfiguration.setBusiness(sbusiness);
          }
          if (sroles != null && !sroles.isEmpty()) {
            hostConfiguration.setRoles(sroles);
          }
          hostConfiguration.update();
        } catch (final WaarpDatabaseException e) {
          hostConfiguration =
              new DbHostConfiguration(Configuration.configuration.getHostId(),
                                      sbusiness, sroles, salias, "");
          hostConfiguration.insert();
        }
      }
      SysErrLogger.FAKE_LOGGER
          .sysout(Messages.getString("ServerInitDatabase.LoadDone"));
      SysErrLogger.FAKE_LOGGER.sysout();
      if (DetectionUtils.isJunit()) {
        return;
      }
      System.exit(0);//NOSONAR
    } catch (final WaarpDatabaseException e) {
      SysErrLogger.FAKE_LOGGER
          .syserr(Messages.getString("ServerInitDatabase.ErrDatabase"));
      SysErrLogger.FAKE_LOGGER.syserr();
      if (DetectionUtils.isJunit()) {
        return;
      }
      System.exit(3);//NOSONAR
    } finally {
      if (admin != null) {
        admin.close();
      }
    }
  }

  private static String getXMLFromFile(String path) {
    String res = "";
    final File file = new File(path);
    if (file.canRead()) {
      try {
        final String value = FileUtils.readFileToString(file);
        if (value != null && !value.trim().isEmpty()) {
          res = value.replaceAll("\r|\n|  ", " ").trim();
        }
      } catch (final IOException e) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
      }
    }
    return res;
  }

  public static void initdb() throws WaarpDatabaseNoConnectionException {
    // Create tables: configuration, hosts, rules, runner, cptrunner
    admin.getSession().getAdmin().getDbModel().createTables(admin.getSession());
  }

  /**
   * @return True if the base is up to date, else False (need Upgrade)
   */
  public static boolean upgradedb() throws WaarpDatabaseException {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ServerInitDatabase.class);
    }
    // Update tables: runner
    boolean uptodate = true;
    // Check if the database is up to date
    final String version = DbHostConfiguration
        .getVersionDb(Configuration.configuration.getHostId());
    try {
      if (version != null) {
        uptodate = admin.getSession().getAdmin().getDbModel()
                        .needUpgradeDb(admin.getSession(), version, true);
      } else {
        uptodate = admin.getSession().getAdmin().getDbModel()
                        .needUpgradeDb(admin.getSession(), "1.1.0", true);
      }
      if (uptodate) {
        logger.error(Messages.getString(
            "ServerInitDatabase.SchemaNotUptodate")); //$NON-NLS-1$
        return false;
      } else {
        logger.debug(Messages.getString(
            "ServerInitDatabase.SchemaUptodate")); //$NON-NLS-1$
      }
    } catch (final WaarpDatabaseNoConnectionException e) {
      logger
          .error(Messages.getString("Database.CannotConnect"), e); //$NON-NLS-1$
      return false;
    }
    return !uptodate;
  }

  public static boolean loadRules(File dirConfig)
      throws WaarpDatabaseException {
    try {
      RuleFileBasedConfiguration.importRules(dirConfig);
    } catch (final OpenR66ProtocolSystemException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      return false;
    }
    return true;
  }

  public static boolean loadHostAuth(String filename) {
    return AuthenticationFileBasedConfiguration
        .loadAuthentication(Configuration.configuration, filename);
  }
}
