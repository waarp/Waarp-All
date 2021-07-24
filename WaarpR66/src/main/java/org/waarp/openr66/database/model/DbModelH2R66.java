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
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.database.model.DbModelH2;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbHostConfiguration;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.utils.R66Versions;

import static org.waarp.common.database.DbConstant.*;

/**
 * H2 Database Model implementation
 */
public class DbModelH2R66 extends DbModelH2 {
  /**
   * Create the object and initialize if necessary the driver
   *
   * @param dbserver
   * @param dbuser
   * @param dbpasswd
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public DbModelH2R66(final String dbserver, final String dbuser,
                      final String dbpasswd)
      throws WaarpDatabaseNoConnectionException {
    super(dbserver, dbuser, dbpasswd);
  }

  @Override
  public final void createTables(final DbSession session)
      throws WaarpDatabaseNoConnectionException {
    // Create tables: configuration, hosts, rules, runner, cptrunner
    final String createTableH2 = "CREATE TABLE IF NOT EXISTS ";
    final String primaryKey = " PRIMARY KEY ";
    final String notNull = " NOT NULL ";
    final DbRequest request =
        DbModelFactoryR66.subCreateTableMariaDbMySQLH2PostgreSQL(dbTypeResolver,
                                                                 session,
                                                                 createTableH2,
                                                                 primaryKey,
                                                                 notNull);
    if (request == null) {
      return;
    }
    StringBuilder action;

    // cptrunner
    action = new StringBuilder(
        "CREATE SEQUENCE IF NOT EXISTS " + DbTaskRunner.fieldseq +
        " START WITH " + (ILLEGALVALUE + 1) + " MINVALUE " +
        (ILLEGALVALUE + 1));
    SysErrLogger.FAKE_LOGGER.sysout(action);
    try {
      request.query(action.toString());
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      // version <= 1.2.173
      action = new StringBuilder(
          "CREATE SEQUENCE IF NOT EXISTS " + DbTaskRunner.fieldseq +
          " START WITH " + (ILLEGALVALUE + 1));
      SysErrLogger.FAKE_LOGGER.sysout(action);
      try {
        request.query(action.toString());
      } catch (final WaarpDatabaseNoConnectionException e2) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e2);
        return;
      } catch (final WaarpDatabaseSqlException e2) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e2);
        // XXX FIX no return
      }
      // XXX FIX no return
    } finally {
      request.close();
    }

    DbHostConfiguration.updateVersionDb(Configuration.configuration.getHostId(),
                                        R66Versions.V3_1_0.getVersion());
  }

  @Override
  public final boolean upgradeDb(final DbSession session, final String version)
      throws WaarpDatabaseNoConnectionException {
    if (PartnerConfiguration.isVersion2GEQVersion1(
        R66Versions.V3_1_0.getVersion(), version)) {
      return true;
    }
    if (PartnerConfiguration.isVersion2GEQVersion1(version,
                                                   R66Versions.V2_4_13.getVersion())) {
      SysErrLogger.FAKE_LOGGER.sysout(
          version + " to " + R66Versions.V2_4_13.getVersion() + "? " + true);
      final String createTableH2 = "CREATE TABLE IF NOT EXISTS ";
      final String primaryKey = " PRIMARY KEY ";
      final String notNull = " NOT NULL ";

      // HostConfiguration
      final StringBuilder action =
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
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        return false;
      } finally {
        request.close();
      }
    }
    if (PartnerConfiguration.isVersion2GEQVersion1(version,
                                                   R66Versions.V2_4_17.getVersion())) {
      SysErrLogger.FAKE_LOGGER.sysout(
          version + " to " + R66Versions.V2_4_17.getVersion() + "? " + true);
      final String command =
          "ALTER TABLE " + DbTaskRunner.table + " ADD COLUMN IF NOT EXISTS " +
          DbTaskRunner.Columns.TRANSFERINFO.name() + ' ' + DBType.getType(
              DbTaskRunner.dbTypes[DbTaskRunner.Columns.TRANSFERINFO.ordinal()]) +
          " NOT NULL DEFAULT '{}' " + " AFTER " +
          DbTaskRunner.Columns.FILEINFO.name();
      final DbRequest request = new DbRequest(session);
      try {
        SysErrLogger.FAKE_LOGGER.sysout("Command: " + command);
        request.query(command);
      } catch (final WaarpDatabaseSqlException e) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        return false;
      } finally {
        request.close();
      }
    }
    if (PartnerConfiguration.isVersion2GEQVersion1(version,
                                                   R66Versions.V2_4_23.getVersion())) {
      SysErrLogger.FAKE_LOGGER.sysout(
          version + " to " + R66Versions.V2_4_23.getVersion() + "? " + true);
      String command =
          "ALTER TABLE " + DbHostAuth.table + " ADD COLUMN IF NOT EXISTS " +
          DbHostAuth.Columns.ISACTIVE.name() + ' ' + DBType.getType(
              DbHostAuth.dbTypes[DbHostAuth.Columns.ISACTIVE.ordinal()]) +
          " NOT NULL DEFAULT " + true + " AFTER " +
          DbHostAuth.Columns.ISCLIENT.name();
      DbRequest request = new DbRequest(session);
      try {
        SysErrLogger.FAKE_LOGGER.sysout("Command: " + command);
        request.query(command);
      } catch (final WaarpDatabaseSqlException e) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        return false;
      } finally {
        request.close();
      }
      command =
          "ALTER TABLE " + DbHostAuth.table + " ADD COLUMN IF NOT EXISTS " +
          DbHostAuth.Columns.ISPROXIFIED.name() + ' ' + DBType.getType(
              DbHostAuth.dbTypes[DbHostAuth.Columns.ISPROXIFIED.ordinal()]) +
          " NOT NULL DEFAULT " + false + " AFTER " +
          DbHostAuth.Columns.ISACTIVE.name();
      request = new DbRequest(session);
      try {
        SysErrLogger.FAKE_LOGGER.sysout("Command: " + command);
        request.query(command);
      } catch (final WaarpDatabaseSqlException e) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        return false;
      } finally {
        request.close();
      }
    }
    if (PartnerConfiguration.isVersion2GTVersion1(version,
                                                  R66Versions.V2_4_25.getVersion())) {
      SysErrLogger.FAKE_LOGGER.sysout(
          version + " to " + R66Versions.V2_4_25.getVersion() + "? " + true);
      String command = "ALTER TABLE " + DbTaskRunner.table + " ALTER COLUMN " +
                       DbTaskRunner.Columns.FILENAME.name() + ' ' +
                       DBType.getType(
                           DbTaskRunner.dbTypes[DbTaskRunner.Columns.FILENAME.ordinal()]) +
                       " NOT NULL ";
      DbRequest request = new DbRequest(session);
      try {
        SysErrLogger.FAKE_LOGGER.sysout("Command: " + command);
        request.query(command);
      } catch (final WaarpDatabaseSqlException e) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        return false;
      } finally {
        request.close();
      }
      command = "ALTER TABLE " + DbTaskRunner.table + " ALTER COLUMN " +
                DbTaskRunner.Columns.ORIGINALNAME.name() + ' ' + DBType.getType(
          DbTaskRunner.dbTypes[DbTaskRunner.Columns.ORIGINALNAME.ordinal()]) +
                " NOT NULL ";
      request = new DbRequest(session);
      try {
        SysErrLogger.FAKE_LOGGER.sysout("Command: " + command);
        request.query(command);
      } catch (final WaarpDatabaseSqlException e) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        return false;
      } finally {
        request.close();
      }
    }
    if (PartnerConfiguration.isVersion2GTVersion1(version,
                                                  R66Versions.V3_0_4.getVersion())) {
      SysErrLogger.FAKE_LOGGER.sysout(
          version + " to " + R66Versions.V3_0_4.getVersion() + "? " + true);
      final DbRequest request = new DbRequest(session);
      // Change Type for all Tables
      DbModelFactoryR66.upgradeTable30(dbTypeResolver, request,
                                       " ALTER COLUMN ", " ", " NOT NULL ");
      try {
        final String command = "DROP INDEX IF EXISTS IDX_RUNNER ";
        try {
          SysErrLogger.FAKE_LOGGER.sysout("Command: " + command);
          request.query(command);
        } catch (final WaarpDatabaseNoConnectionException e) {
          SysErrLogger.FAKE_LOGGER.ignoreLog(e);
          return false;
        } catch (final WaarpDatabaseSqlException e) {
          SysErrLogger.FAKE_LOGGER.ignoreLog(e);
          // XXX FIX no return
        }
        DbModelFactoryR66.createIndex30(dbTypeResolver, request);
      } finally {
        request.close();
      }
    }
    DbHostConfiguration.updateVersionDb(Configuration.configuration.getHostId(),
                                        R66Versions.V3_1_0.getVersion());
    return true;
  }

  @Override
  public final boolean needUpgradeDb(final DbSession session,
                                     final String version, final boolean tryFix)
      throws WaarpDatabaseNoConnectionException {
    return DbModelFactoryR66.needUpgradeDbAllDb(dbTypeResolver, session,
                                                version);
  }

}
