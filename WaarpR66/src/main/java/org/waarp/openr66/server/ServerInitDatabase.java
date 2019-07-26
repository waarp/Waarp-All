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
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.openr66.configuration.AuthenticationFileBasedConfiguration;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.configuration.RuleFileBasedConfiguration;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbHostConfiguration;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import java.io.File;
import java.io.IOException;

/**
 * Utility class to initiate the database for a server
 *
 *
 */
public class ServerInitDatabase {
  /**
   * Internal Logger
   */
  static volatile WaarpLogger logger;

  protected static String _INFO_ARGS =
      Messages.getString("ServerInitDatabase.Help");

  static String sxml = null;
  static boolean database = false;
  static boolean upgradeDb = false;
  static String sbusiness = null;
  static String salias = null;
  static String sroles = null;
  static String sdirconfig = null;
  static String shostauth = null;
  static String slimitconfig = null;

  protected static boolean getParams(String[] args) {
    if (args.length < 1) {
      logger.error(_INFO_ARGS);
      return false;
    }
    sxml = args[0];
    for (int i = 1; i < args.length; i++) {
      if (args[i].equalsIgnoreCase("-initdb")) {
        database = true;
        FileBasedConfiguration.autoupgrade = false;
        upgradeDb = true;
      } else if (args[i].equalsIgnoreCase("-upgradeDb")) {
        upgradeDb = true;
      } else if (args[i].equalsIgnoreCase("-loadBusiness")) {
        i++;
        sbusiness = args[i];
      } else if (args[i].equalsIgnoreCase("-loadAlias")) {
        i++;
        salias = args[i];
      } else if (args[i].equalsIgnoreCase("-loadRoles")) {
        i++;
        sroles = args[i];
      } else if (args[i].equalsIgnoreCase("-dir")) {
        i++;
        sdirconfig = args[i];
      } else if (args[i].equalsIgnoreCase("-limit")) {
        i++;
        slimitconfig = args[i];
      } else if (args[i].equalsIgnoreCase("-auth")) {
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
      System.out.println(_INFO_ARGS);
      if (DbConstant.admin != null) {
        DbConstant.admin.close();
      }
      if (DetectionUtils.isJunit()) {
        return;
      }
      ChannelUtils.stopLogger();
      System.exit(2);
    }

    try {
      if (!FileBasedConfiguration
          .setConfigurationInitDatabase(Configuration.configuration, args[0],
                                        database)) {
        System.err
            .format(Messages.getString("Configuration.NeedCorrectConfig"));
        System.err.println();
        if (DetectionUtils.isJunit()) {
          return;
        }
        ChannelUtils.stopLogger();
        System.exit(2);
      }
      if (database) {
        // Init database
        System.out
            .format(Messages.getString("ServerInitDatabase.Create.Start"));
        System.out.println();
        initdb();
        System.out.format(Messages.getString("ServerInitDatabase.Create.Done"));
        System.out.println();
      }
      if (upgradeDb) {
        // try to upgrade DB schema
        System.out
            .format(Messages.getString("ServerInitDatabase.Upgrade.Start"));
        System.out.println();
        // TODO Split check for update and upgrade actions
        if (!upgradedb()) {
          System.err.println(
              Messages.getString("ServerInitDatabase.SchemaNotUptodate"));
          System.err.println();
          if (DetectionUtils.isJunit()) {
            return;
          }
          System.exit(1);
        }
        System.out
            .format(Messages.getString("ServerInitDatabase.Upgrade.Done"));
        System.out.println();
      }
      if (sdirconfig != null) {
        // load Rules
        System.out
            .format(Messages.getString("ServerInitDatabase.LoadRule.Start"),
                    sdirconfig);
        System.out.println();
        final File dirConfig = new File(sdirconfig);
        if (dirConfig.isDirectory()) {
          if (!loadRules(dirConfig)) {
            System.out.format(
                Messages.getString("ServerInitDatabase.LoadRule.Failed"));
            if (DetectionUtils.isJunit()) {
              return;
            }
            System.exit(1);
          }
        } else {
          System.err.format(
              Messages.getString("ServerInitDatabase.LoadRule.NoDirectory"),
              sdirconfig);
          System.err.println();
          if (DetectionUtils.isJunit()) {
            return;
          }
          System.exit(1);
        }
        System.out
            .format(Messages.getString("ServerInitDatabase.LoadRule.Done"));
        System.out.println();
      }
      if (shostauth != null) {
        // Load Host Authentications
        System.out
            .format(Messages.getString("ServerInitDatabase.LoadAuth.Start"),
                    shostauth);
        System.out.println();
        if (!loadHostAuth(shostauth)) {
          System.err
              .format(Messages.getString("ServerInitDatabase.LoadAuth.Failed"));
          System.err.println();
          if (DetectionUtils.isJunit()) {
            return;
          }
          System.exit(1);
        }
        System.out
            .format(Messages.getString("ServerInitDatabase.LoadAuth.Done"));
        System.out.println();
      }
      if (slimitconfig != null) {
        // Load configuration
        System.out
            .format(Messages.getString("ServerInitDatabase.LoadLimit.Start"),
                    slimitconfig);
        System.out.println();
        if (!FileBasedConfiguration
            .setConfigurationLoadLimitFromXml(Configuration.configuration,
                                              slimitconfig)) {
          System.err.format(
              Messages.getString("ServerInitDatabase.LoadLimit.Failed"));
          System.err.println();
          if (DetectionUtils.isJunit()) {
            return;
          }
          System.exit(1);
        }
        System.out
            .format(Messages.getString("ServerInitDatabase.LoadLimit.Done"));
        System.out.println();
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
              new DbHostConfiguration(Configuration.configuration.getHOST_ID());
          if (!salias.equals("")) {
            hostConfiguration.setAliases(salias);
          }
          if (!sbusiness.equals("")) {
            hostConfiguration.setBusiness(sbusiness);
          }
          if (!sroles.equals("")) {
            hostConfiguration.setRoles(sroles);
          }
          hostConfiguration.update();
        } catch (final WaarpDatabaseException e) {
          hostConfiguration =
              new DbHostConfiguration(Configuration.configuration.getHOST_ID(),
                                      sbusiness, sroles, salias, "");
          hostConfiguration.insert();
        }
      }
      System.out.println(Messages.getString("ServerInitDatabase.LoadDone"));
      System.out.println();
      if (DetectionUtils.isJunit()) {
        return;
      }
      System.exit(0);
    } catch (final WaarpDatabaseException e) {
      System.err.println(Messages.getString("ServerInitDatabase.ErrDatabase"));
      System.err.println();
      if (DetectionUtils.isJunit()) {
        return;
      }
      System.exit(3);
    } finally {
      if (DbConstant.admin != null) {
        DbConstant.admin.close();
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
        e.printStackTrace();
      }
    }
    return res;
  }

  public static void initdb() throws WaarpDatabaseNoConnectionException {
    // Create tables: configuration, hosts, rules, runner, cptrunner
    DbConstant.admin.getSession().getAdmin().getDbModel()
                    .createTables(DbConstant.admin.getSession());
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
        .getVersionDb(Configuration.configuration.getHOST_ID());
    try {
      if (version != null) {
        uptodate = DbConstant.admin.getSession().getAdmin().getDbModel()
                                   .needUpgradeDb(DbConstant.admin.getSession(),
                                                  version, true);
      } else {
        uptodate = DbConstant.admin.getSession().getAdmin().getDbModel()
                                   .needUpgradeDb(DbConstant.admin.getSession(),
                                                  "1.1.0", true);
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
      e.printStackTrace();
      return false;
    }
    return true;
  }

  public static boolean loadHostAuth(String filename) {
    return AuthenticationFileBasedConfiguration
        .loadAuthentication(Configuration.configuration, filename);
  }
}
