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

import org.waarp.common.database.DbRequest;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
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

import static org.waarp.common.database.DbConstant.*;

/**
 * MariaDB Database Model implementation
 */
public class DbModelMariadb
    extends org.waarp.common.database.model.DbModelMariadb {
  /**
   * Create the object and initialize if necessary the driver
   *
   * @param dbserver
   * @param dbuser
   * @param dbpasswd
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public DbModelMariadb(String dbserver, String dbuser, String dbpasswd)
      throws WaarpDatabaseNoConnectionException {
    super(dbserver, dbuser, dbpasswd);
  }

  @Override
  public void createTables(DbSession session)
      throws WaarpDatabaseNoConnectionException {
    // Create tables: configuration, hosts, rules, runner, cptrunner
    final String createTableH2 = "CREATE TABLE IF NOT EXISTS ";
    final String primaryKey = " PRIMARY KEY ";
    final String notNull = " NOT NULL ";

    // Multiple Mode
    StringBuilder action =
        new StringBuilder(createTableH2 + DbMultipleMonitor.table + '(');
    final DbMultipleMonitor.Columns[] mcolumns =
        DbMultipleMonitor.Columns.values();
    for (int i = 0; i < mcolumns.length - 1; i++) {
      action.append(mcolumns[i].name())
            .append(DBType.getType(DbMultipleMonitor.dbTypes[i]))
            .append(notNull).append(", ");
    }
    action.append(mcolumns[mcolumns.length - 1].name()).append(
        DBType.getType(DbMultipleMonitor.dbTypes[mcolumns.length - 1]))
          .append(primaryKey).append(')');
    SysErrLogger.FAKE_LOGGER.sysout(action);
    DbRequest request = new DbRequest(session);
    try {
      request.query(action.toString());
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      // XXX FIX no return
    } finally {
      request.close();
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
            .append(DBType.getType(DbConfiguration.dbTypes[i])).append(notNull)
            .append(", ");
    }
    action.append(ccolumns[ccolumns.length - 1].name())
          .append(DBType.getType(DbConfiguration.dbTypes[ccolumns.length - 1]))
          .append(primaryKey).append(')');
    SysErrLogger.FAKE_LOGGER.sysout(action);
    request = new DbRequest(session);
    try {
      request.query(action.toString());
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      // XXX FIX no return
    } finally {
      request.close();
    }

    // HostConfiguration
    action = new StringBuilder(createTableH2 + DbHostConfiguration.table + '(');
    final DbHostConfiguration.Columns[] chcolumns =
        DbHostConfiguration.Columns.values();
    for (int i = 0; i < chcolumns.length - 1; i++) {
      action.append(chcolumns[i].name())
            .append(DBType.getType(DbHostConfiguration.dbTypes[i]))
            .append(notNull).append(", ");
    }
    action.append(chcolumns[chcolumns.length - 1].name()).append(
        DBType.getType(DbHostConfiguration.dbTypes[chcolumns.length - 1]))
          .append(primaryKey).append(')');
    SysErrLogger.FAKE_LOGGER.sysout(action);
    request = new DbRequest(session);
    try {
      request.query(action.toString());
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      // XXX FIX no return
    } finally {
      request.close();
    }

    // hosts
    action = new StringBuilder(createTableH2 + DbHostAuth.table + '(');
    final DbHostAuth.Columns[] hcolumns = DbHostAuth.Columns.values();
    for (int i = 0; i < hcolumns.length - 1; i++) {
      action.append(hcolumns[i].name())
            .append(DBType.getType(DbHostAuth.dbTypes[i])).append(notNull)
            .append(", ");
    }
    action.append(hcolumns[hcolumns.length - 1].name())
          .append(DBType.getType(DbHostAuth.dbTypes[hcolumns.length - 1]))
          .append(primaryKey).append(')');
    SysErrLogger.FAKE_LOGGER.sysout(action);
    try {
      request.query(action.toString());
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      // XXX FIX no return
    } finally {
      request.close();
    }

    // rules
    action = new StringBuilder(createTableH2 + DbRule.table + '(');
    final DbRule.Columns[] rcolumns = DbRule.Columns.values();
    for (int i = 0; i < rcolumns.length - 1; i++) {
      action.append(rcolumns[i].name())
            .append(DBType.getType(DbRule.dbTypes[i])).append(", ");
    }
    action.append(rcolumns[rcolumns.length - 1].name())
          .append(DBType.getType(DbRule.dbTypes[rcolumns.length - 1]))
          .append(primaryKey).append(')');
    SysErrLogger.FAKE_LOGGER.sysout(action);
    try {
      request.query(action.toString());
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      // XXX FIX no return
    } finally {
      request.close();
    }

    // runner
    action = new StringBuilder(createTableH2 + DbTaskRunner.table + '(');
    final DbTaskRunner.Columns[] acolumns = DbTaskRunner.Columns.values();
    for (int i = 0; i < acolumns.length; i++) {
      action.append(acolumns[i].name())
            .append(DBType.getType(DbTaskRunner.dbTypes[i])).append(notNull)
            .append(", ");
    }
    // Several columns for primary key
    action.append(" CONSTRAINT runner_pk " + primaryKey + '(');
    for (int i = DbTaskRunner.NBPRKEY; i > 1; i--) {
      action.append(acolumns[acolumns.length - i].name()).append(',');
    }
    action.append(acolumns[acolumns.length - 1].name()).append("))");
    SysErrLogger.FAKE_LOGGER.sysout(action);
    try {
      request.query(action.toString());
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      // XXX FIX no return
    } finally {
      request.close();
    }
    // Index Runner
    action = new StringBuilder(
        "CREATE INDEX IDX_RUNNER ON " + DbTaskRunner.table + '(');
    final DbTaskRunner.Columns[] icolumns = DbTaskRunner.indexes;
    for (int i = 0; i < icolumns.length - 1; i++) {
      action.append(icolumns[i].name()).append(", ");
    }
    action.append(icolumns[icolumns.length - 1].name()).append(')');
    SysErrLogger.FAKE_LOGGER.sysout(action);
    try {
      request.query(action.toString());
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      // XXX FIX no return
    } finally {
      request.close();
    }

    // cptrunner
    /*
     * # Table to handle any number of sequences
     */
    action = new StringBuilder(
        "CREATE TABLE Sequences (name VARCHAR(22) NOT NULL PRIMARY KEY," +
        "seq BIGINT NOT NULL)");
    SysErrLogger.FAKE_LOGGER.sysout(action);
    try {
      request.query(action.toString());
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      // XXX FIX no return
    } finally {
      request.close();
    }
    action = new StringBuilder(
        "INSERT INTO Sequences (name, seq) VALUES ('" + DbTaskRunner.fieldseq +
        "', " + (ILLEGALVALUE + 1) + ')');
    SysErrLogger.FAKE_LOGGER.sysout(action);
    try {
      request.query(action.toString());
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      // XXX FIX no return
    } finally {
      request.close();
    }

    DbHostConfiguration.updateVersionDb(Configuration.configuration.getHostId(),
                                        R66Versions.V2_4_25.getVersion());
  }

  @Override
  public boolean upgradeDb(DbSession session, String version)
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
              .append(DBType.getType(DbHostConfiguration.dbTypes[i]))
              .append(notNull).append(", ");
      }
      action.append(chcolumns[chcolumns.length - 1].name()).append(
          DBType.getType(DbHostConfiguration.dbTypes[chcolumns.length - 1]))
            .append(primaryKey).append(')');
      SysErrLogger.FAKE_LOGGER.sysout(action);
      final DbRequest request = new DbRequest(session);
      try {
        request.query(action.toString());
      } catch (final WaarpDatabaseSqlException e) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        return false;
      } finally {
        request.close();
      }
    }
    if (PartnerConfiguration
        .isVersion2GEQVersion1(version, R66Versions.V2_4_17.getVersion())) {
      SysErrLogger.FAKE_LOGGER.sysout(
          version + " to " + R66Versions.V2_4_17.getVersion() + "? " + true);
      final DbRequest request = new DbRequest(session);
      try {
        final String command =
            "ALTER TABLE " + DbTaskRunner.table + " ADD COLUMN " +
            DbTaskRunner.Columns.TRANSFERINFO.name() + ' ' + DBType.getType(
                DbTaskRunner.dbTypes[DbTaskRunner.Columns.TRANSFERINFO
                    .ordinal()]) + " AFTER " +
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
        String command = "ALTER TABLE " + DbHostAuth.table + " ADD COLUMN " +
                         DbHostAuth.Columns.ISACTIVE.name() + ' ' + DBType
                             .getType(
                                 DbHostAuth.dbTypes[DbHostAuth.Columns.ISACTIVE
                                     .ordinal()]) + " AFTER " +
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
        String command = "ALTER TABLE " + DbHostAuth.table + " ADD COLUMN " +
                         DbHostAuth.Columns.ISPROXIFIED.name() + ' ' + DBType
                             .getType(
                                 DbHostAuth.dbTypes[DbHostAuth.Columns.ISPROXIFIED
                                     .ordinal()]) + " AFTER " +
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
      String command = "ALTER TABLE " + DbTaskRunner.table + " MODIFY " +
                       DbTaskRunner.Columns.FILENAME.name() + ' ' + DBType
                           .getType(
                               DbTaskRunner.dbTypes[DbTaskRunner.Columns.FILENAME
                                   .ordinal()]) + " NOT NULL ";
      DbRequest request = new DbRequest(session);
      try {
        SysErrLogger.FAKE_LOGGER.sysout("Command: " + command);
        request.query(command);
      } catch (final WaarpDatabaseSqlException e) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        return false;
      } finally {
        request.close();
      }
      command = "ALTER TABLE " + DbTaskRunner.table + " MODIFY " +
                DbTaskRunner.Columns.ORIGINALNAME.name() + ' ' + DBType.getType(
          DbTaskRunner.dbTypes[DbTaskRunner.Columns.ORIGINALNAME.ordinal()]) +
                " NOT NULL ";
      request = new DbRequest(session);
      try {
        SysErrLogger.FAKE_LOGGER.sysout("Command: " + command);
        request.query(command);
      } catch (final WaarpDatabaseSqlException e) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        return false;
      } finally {
        request.close();
      }
    }
    DbHostConfiguration.updateVersionDb(Configuration.configuration.getHostId(),
                                        R66Versions.V2_4_25.getVersion());
    return true;
  }

  @Override
  public boolean needUpgradeDb(DbSession session, String version,
                               boolean tryFix)
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
        return !upgradeDb(session, version);
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
        return !upgradeDb(session, version);
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
        return !upgradeDb(session, version);
      } finally {
        if (request != null) {
          request.close();
        }
      }
    }
    request = null;
    if (PartnerConfiguration
        .isVersion2GTVersion1(version, R66Versions.V2_4_25.getVersion())) {
      if (upgradeDb(session, version)) {
        DbHostConfiguration
            .updateVersionDb(Configuration.configuration.getHostId(),
                             R66Versions.V2_4_25.getVersion());
      } else {
        return true;
      }
    }
    return false;
  }

}
