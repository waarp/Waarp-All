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
package org.waarp.openr66.database.model;

import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.DbRequest;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.database.model.DbModel;
import org.waarp.common.database.model.DbModelAbstract.DbTypeResolver;
import org.waarp.common.database.model.DbModelFactory;
import org.waarp.common.database.model.DbType;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.openr66.database.data.DbConfiguration;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbHostConfiguration;
import org.waarp.openr66.database.data.DbMultipleMonitor;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.utils.R66Versions;

import java.sql.Types;

import static org.waarp.common.database.DbConstant.*;

/**
 * Factory to store the Database Model object
 */
public class DbModelFactoryR66 extends DbModelFactory {

  private static final String ALTER_TABLE = "ALTER TABLE ";
  private static final String ADD_COLUMN = " ADD COLUMN ";
  private static final String AFTER = " AFTER ";

  /**
   * Initialize the Database Model according to arguments.
   *
   * @param dbdriver
   * @param dbserver
   * @param dbuser
   * @param dbpasswd
   * @param write
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public static DbAdmin initialize(String dbdriver, String dbserver,
                                   String dbuser, String dbpasswd,
                                   boolean write)
      throws WaarpDatabaseNoConnectionException {
    final DbType type = DbType.getFromDriver(dbdriver);
    DbModel dbModel;
    switch (type) {
      case H2:
        dbModel = new DbModelH2R66(dbserver, dbuser, dbpasswd);
        break;
      case Oracle:
        dbModel = new DbModelOracleR66(dbserver, dbuser, dbpasswd);
        break;
      case PostGreSQL:
        dbModel = new DbModelPostgresqlR66();
        break;
      case MySQL:
        dbModel = new DbModelMysqlR66(dbserver, dbuser, dbpasswd);
        break;
      case MariaDB:
        dbModel = new DbModelMariadbR66(dbserver, dbuser, dbpasswd);
        break;
      default:
        throw new WaarpDatabaseNoConnectionException(
            "TypeDriver unknown: " + type);
    }
    dbModels.add(dbModel);
    return new DbAdmin(dbModel, dbserver, dbuser, dbpasswd, write);
  }

  static boolean needUpgradeDbAllDb(final DbTypeResolver dbTypeResolver,
                                    final DbSession session,
                                    final String version)
      throws WaarpDatabaseNoConnectionException {
    // Check if the database is up to date
    DbRequest request = null;
    if (PartnerConfiguration
        .isVersion2GEQVersion1(version, R66Versions.V2_4_13.getVersion())) {
      try {
        request = new DbRequest(session);
        request.select(
            "select " + DbHostConfiguration.Columns.HOSTID.name() + " from " +
            DbHostConfiguration.table + " where " +
            DbHostConfiguration.Columns.HOSTID + " = '" +
            Configuration.configuration.getHostId() + '\'');
        request.close();
        DbHostConfiguration
            .updateVersionDb(Configuration.configuration.getHostId(),
                             R66Versions.V2_4_13.getVersion());
      } catch (final WaarpDatabaseSqlException e) {
        return !upgradeDbMariaDbMySQL(dbTypeResolver, session, version);
      } finally {
        if (request != null) {
          request.close();
        }
      }
    }
    request = null;
    if (PartnerConfiguration
        .isVersion2GEQVersion1(version, R66Versions.V2_4_17.getVersion())) {
      try {
        request = new DbRequest(session);
        request.select(
            "select " + DbTaskRunner.Columns.TRANSFERINFO.name() + " from " +
            DbTaskRunner.table + " where " + DbTaskRunner.Columns.SPECIALID +
            " = " + ILLEGALVALUE);
        request.close();
        DbHostConfiguration
            .updateVersionDb(Configuration.configuration.getHostId(),
                             R66Versions.V2_4_17.getVersion());
      } catch (final WaarpDatabaseSqlException e) {
        return !upgradeDbMariaDbMySQL(dbTypeResolver, session, version);
      } finally {
        if (request != null) {
          request.close();
        }
      }
    }
    request = null;
    if (PartnerConfiguration
        .isVersion2GEQVersion1(version, R66Versions.V2_4_23.getVersion())) {
      try {
        request = new DbRequest(session);
        request.select(
            "select " + DbHostAuth.Columns.ISACTIVE.name() + " from " +
            DbHostAuth.table + " where " + DbHostAuth.Columns.PORT + " = " + 0);
        request.close();
        DbHostConfiguration
            .updateVersionDb(Configuration.configuration.getHostId(),
                             R66Versions.V2_4_23.getVersion());
      } catch (final WaarpDatabaseSqlException e) {
        return !upgradeDbMariaDbMySQL(dbTypeResolver, session, version);
      } finally {
        if (request != null) {
          request.close();
        }
      }
    }
    request = null;
    if (PartnerConfiguration
        .isVersion2GTVersion1(version, R66Versions.V2_4_25.getVersion())) {
      if (upgradeDbMariaDbMySQL(dbTypeResolver, session, version)) {
        DbHostConfiguration
            .updateVersionDb(Configuration.configuration.getHostId(),
                             R66Versions.V2_4_25.getVersion());
      } else {
        return true;
      }
    }
    return false;
  }

  static DbRequest subCreateTableMariaDbMySQLPostgreSQL(
      final DbTypeResolver dbTypeResolver, final DbSession session,
      final String createTableH2, final String primaryKey, final String notNull)
      throws WaarpDatabaseNoConnectionException {
    // Multiple Mode
    StringBuilder action =
        new StringBuilder(createTableH2 + DbMultipleMonitor.table + '(');
    final DbMultipleMonitor.Columns[] mcolumns =
        DbMultipleMonitor.Columns.values();
    for (int i = 0; i < mcolumns.length - 1; i++) {
      action.append(mcolumns[i].name())
            .append(dbTypeResolver.getType(DbMultipleMonitor.dbTypes[i]))
            .append(notNull).append(", ");
    }
    action.append(mcolumns[mcolumns.length - 1].name()).append(
        dbTypeResolver.getType(DbMultipleMonitor.dbTypes[mcolumns.length - 1]))
          .append(primaryKey).append(')');
    SysErrLogger.FAKE_LOGGER.sysout(action);
    if (executeRequestAction(session, action)) {
      return null;
    }
    final DbMultipleMonitor multipleMonitor =
        new DbMultipleMonitor(Configuration.configuration.getHostId(), 0, 0, 0);
    try {
      if (!multipleMonitor.exist()) {
        multipleMonitor.insert();
      }
    } catch (final WaarpDatabaseException e1) {
      SysErrLogger.FAKE_LOGGER.syserr(e1);
    }

    // Configuration
    action = new StringBuilder(createTableH2 + DbConfiguration.table + '(');
    final DbConfiguration.Columns[] ccolumns = DbConfiguration.Columns.values();
    for (int i = 0; i < ccolumns.length - 1; i++) {
      action.append(ccolumns[i].name())
            .append(dbTypeResolver.getType(DbConfiguration.dbTypes[i]))
            .append(notNull).append(", ");
    }
    action.append(ccolumns[ccolumns.length - 1].name()).append(
        dbTypeResolver.getType(DbConfiguration.dbTypes[ccolumns.length - 1]))
          .append(primaryKey).append(')');
    SysErrLogger.FAKE_LOGGER.sysout(action);
    if (executeRequestAction(session, action)) {
      return null;
    }

    // HostConfiguration
    action = new StringBuilder(createTableH2 + DbHostConfiguration.table + '(');
    final DbHostConfiguration.Columns[] chcolumns =
        DbHostConfiguration.Columns.values();
    for (int i = 0; i < chcolumns.length - 1; i++) {
      action.append(chcolumns[i].name())
            .append(dbTypeResolver.getType(DbHostConfiguration.dbTypes[i]))
            .append(notNull).append(", ");
    }
    action.append(chcolumns[chcolumns.length - 1].name()).append(dbTypeResolver
                                                                     .getType(
                                                                         DbHostConfiguration.dbTypes[
                                                                             chcolumns.length -
                                                                             1]))
          .append(primaryKey).append(')');
    SysErrLogger.FAKE_LOGGER.sysout(action);
    if (executeRequestAction(session, action)) {
      return null;
    }

    // hosts
    action = new StringBuilder(createTableH2 + DbHostAuth.table + '(');
    final DbHostAuth.Columns[] hcolumns = DbHostAuth.Columns.values();
    for (int i = 0; i < hcolumns.length - 1; i++) {
      action.append(hcolumns[i].name())
            .append(dbTypeResolver.getType(DbHostAuth.dbTypes[i]))
            .append(notNull).append(", ");
    }
    action.append(hcolumns[hcolumns.length - 1].name()).append(
        dbTypeResolver.getType(DbHostAuth.dbTypes[hcolumns.length - 1]))
          .append(primaryKey).append(')');
    SysErrLogger.FAKE_LOGGER.sysout(action);
    if (executeRequestAction(session, action)) {
      return null;
    }

    // rules
    action = new StringBuilder(createTableH2 + DbRule.table + '(');
    final DbRule.Columns[] rcolumns = DbRule.Columns.values();
    for (int i = 0; i < rcolumns.length - 1; i++) {
      action.append(rcolumns[i].name())
            .append(dbTypeResolver.getType(DbRule.dbTypes[i])).append(", ");
    }
    action.append(rcolumns[rcolumns.length - 1].name())
          .append(dbTypeResolver.getType(DbRule.dbTypes[rcolumns.length - 1]))
          .append(primaryKey).append(')');
    SysErrLogger.FAKE_LOGGER.sysout(action);
    if (executeRequestAction(session, action)) {
      return null;
    }

    // runner
    action = new StringBuilder(createTableH2 + DbTaskRunner.table + '(');
    final DbTaskRunner.Columns[] acolumns = DbTaskRunner.Columns.values();
    for (int i = 0; i < acolumns.length; i++) {
      action.append(acolumns[i].name())
            .append(dbTypeResolver.getType(DbTaskRunner.dbTypes[i]))
            .append(notNull);
      if (DbTaskRunner.dbTypes[i] == Types.TIMESTAMP) {
        action.append(" DEFAULT CURRENT_TIMESTAMP(3)");
      }
      action.append(", ");
    }
    // Several columns for primary key
    action.append(" CONSTRAINT runner_pk " + primaryKey + '(');
    for (int i = DbTaskRunner.NBPRKEY; i > 1; i--) {
      action.append(acolumns[acolumns.length - i].name()).append(',');
    }
    action.append(acolumns[acolumns.length - 1].name()).append("))");
    SysErrLogger.FAKE_LOGGER.sysout(action);
    if (executeRequestAction(session, action)) {
      return null;
    }
    return new DbRequest(session);
  }

  private static boolean executeRequestAction(final DbSession session,
                                              final StringBuilder action)
      throws WaarpDatabaseNoConnectionException {
    return executeRequestAction(session, action.toString());
  }

  private static boolean executeRequestAction(final DbSession session,
                                              final String action)
      throws WaarpDatabaseNoConnectionException {
    final DbRequest request;
    request = new DbRequest(session);
    try {
      request.query(action);
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      return true;
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      // XXX FIX no return
    } finally {
      request.close();
    }
    return false;
  }

  static void createTableMariaDbMySQL(final DbTypeResolver dbTypeResolver,
                                      final DbSession session)
      throws WaarpDatabaseNoConnectionException {
    // Create tables: configuration, hosts, rules, runner, cptrunner
    final String createTableH2 = "CREATE TABLE IF NOT EXISTS ";
    final String primaryKey = " PRIMARY KEY ";
    final String notNull = " NOT NULL ";
    DbRequest request =
        subCreateTableMariaDbMySQLPostgreSQL(dbTypeResolver, session,
                                             createTableH2, primaryKey,
                                             notNull);
    if (request == null) {
      return;
    }
    StringBuilder action;
    // Index Runner
    action = new StringBuilder(
        "CREATE INDEX IDX_RUNNER ON " + DbTaskRunner.table + '(');
    final DbTaskRunner.Columns[] icolumns = DbTaskRunner.indexes;
    for (int i = 0; i < icolumns.length - 1; i++) {
      action.append(icolumns[i].name()).append(", ");
    }
    action.append(icolumns[icolumns.length - 1].name()).append(')');
    SysErrLogger.FAKE_LOGGER.sysout(action);
    if (executeRequestAction(session, action)) {
      return;
    }
    // cptrunner
    /*
     * # Table to handle any number of sequences
     */
    action = new StringBuilder(
        "CREATE TABLE Sequences (name VARCHAR(22) NOT NULL PRIMARY KEY," +
        "seq BIGINT NOT NULL)");
    SysErrLogger.FAKE_LOGGER.sysout(action);
    if (executeRequestAction(session, action)) {
      return;
    }
    action = new StringBuilder(
        "INSERT INTO Sequences (name, seq) VALUES ('" + DbTaskRunner.fieldseq +
        "', " + (ILLEGALVALUE + 1) + ')');
    SysErrLogger.FAKE_LOGGER.sysout(action);
    if (executeRequestAction(session, action)) {
      return;
    }

    DbHostConfiguration.updateVersionDb(Configuration.configuration.getHostId(),
                                        R66Versions.V2_4_25.getVersion());
  }

  static boolean upgradeDbMariaDbMySQL(final DbTypeResolver dbTypeResolver,
                                       final DbSession session,
                                       final String version)
      throws WaarpDatabaseNoConnectionException {
    if (PartnerConfiguration
        .isVersion2GEQVersion1(version, R66Versions.V2_4_13.getVersion())) {
      SysErrLogger.FAKE_LOGGER.sysout(
          version + " to " + R66Versions.V2_4_13.getVersion() + "? " + true);
      final String createTableH2 = "CREATE TABLE IF NOT EXISTS ";
      final String primaryKey = " PRIMARY KEY ";
      final String notNull = " NOT NULL ";

      // HostConfiguration
      StringBuilder action =
          new StringBuilder(createTableH2 + DbHostConfiguration.table + '(');
      final DbHostConfiguration.Columns[] chcolumns =
          DbHostConfiguration.Columns.values();
      for (int i = 0; i < chcolumns.length - 1; i++) {
        action.append(chcolumns[i].name())
              .append(dbTypeResolver.getType(DbHostConfiguration.dbTypes[i]))
              .append(notNull).append(", ");
      }
      action.append(chcolumns[chcolumns.length - 1].name()).append(
          dbTypeResolver
              .getType(DbHostConfiguration.dbTypes[chcolumns.length - 1]))
            .append(primaryKey).append(')');
      SysErrLogger.FAKE_LOGGER.sysout(action);
      if (executeRequestAction(session, action)) {
        return false;
      }
    }
    if (PartnerConfiguration
        .isVersion2GEQVersion1(version, R66Versions.V2_4_17.getVersion())) {
      SysErrLogger.FAKE_LOGGER.sysout(
          version + " to " + R66Versions.V2_4_17.getVersion() + "? " + true);
      final DbRequest request = new DbRequest(session);
      try {
        final String command = ALTER_TABLE + DbTaskRunner.table + ADD_COLUMN +
                               DbTaskRunner.Columns.TRANSFERINFO.name() + ' ' +
                               dbTypeResolver.getType(
                                   DbTaskRunner.dbTypes[DbTaskRunner.Columns.TRANSFERINFO
                                       .ordinal()]) + AFTER +
                               DbTaskRunner.Columns.FILEINFO.name();
        request.query(command);
      } catch (final WaarpDatabaseSqlException e) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        // return false
      } finally {
        request.close();
      }
    }
    if (PartnerConfiguration
        .isVersion2GEQVersion1(version, R66Versions.V2_4_23.getVersion())) {
      SysErrLogger.FAKE_LOGGER.sysout(
          version + " to " + R66Versions.V2_4_23.getVersion() + "? " + true);
      final DbRequest request = new DbRequest(session);
      try {
        String command = ALTER_TABLE + DbHostAuth.table + ADD_COLUMN +
                         DbHostAuth.Columns.ISACTIVE.name() + ' ' +
                         dbTypeResolver.getType(
                             DbHostAuth.dbTypes[DbHostAuth.Columns.ISACTIVE
                                 .ordinal()]) + AFTER +
                         DbHostAuth.Columns.ISCLIENT.name();
        request.query(command);
        command = "UPDATE " + DbHostAuth.table + " SET " +
                  DbHostAuth.Columns.ISACTIVE.name() + " = " + true;
        request.query(command);
      } catch (final WaarpDatabaseSqlException e) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        // return false
      } finally {
        request.close();
      }
      try {
        String command = ALTER_TABLE + DbHostAuth.table + ADD_COLUMN +
                         DbHostAuth.Columns.ISPROXIFIED.name() + ' ' +
                         dbTypeResolver.getType(
                             DbHostAuth.dbTypes[DbHostAuth.Columns.ISPROXIFIED
                                 .ordinal()]) + AFTER +
                         DbHostAuth.Columns.ISACTIVE.name();
        request.query(command);
        command = "UPDATE " + DbHostAuth.table + " SET " +
                  DbHostAuth.Columns.ISPROXIFIED.name() + " = " + false;
        request.query(command);
      } catch (final WaarpDatabaseSqlException e) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        // return false
      } finally {
        request.close();
      }
    }
    if (PartnerConfiguration
        .isVersion2GTVersion1(version, R66Versions.V2_4_25.getVersion())) {
      SysErrLogger.FAKE_LOGGER.sysout(
          version + " to " + R66Versions.V2_4_25.getVersion() + "? " + true);
      String command = ALTER_TABLE + DbTaskRunner.table + " MODIFY " +
                       DbTaskRunner.Columns.FILENAME.name() + ' ' +
                       dbTypeResolver.getType(
                           DbTaskRunner.dbTypes[DbTaskRunner.Columns.FILENAME
                               .ordinal()]) + " NOT NULL ";
      if (executeRequestAction(session, command)) {
        return false;
      }
      command = ALTER_TABLE + DbTaskRunner.table + " MODIFY " +
                DbTaskRunner.Columns.ORIGINALNAME.name() + ' ' + dbTypeResolver
                    .getType(
                        DbTaskRunner.dbTypes[DbTaskRunner.Columns.ORIGINALNAME
                            .ordinal()]) + " NOT NULL ";
      if (executeRequestAction(session, command)) {
        return false;
      }
    }
    DbHostConfiguration.updateVersionDb(Configuration.configuration.getHostId(),
                                        R66Versions.V2_4_25.getVersion());
    return true;
  }
}
