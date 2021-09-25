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
package org.waarp.gateway.kernel.database.model;

import org.waarp.common.database.DbRequest;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.database.model.DbModelPostgresql;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.gateway.kernel.database.DbConstantGateway;
import org.waarp.gateway.kernel.database.data.DbTransferLog;

/**
 * PostGreSQL Database Model implementation
 */
public class DbModelPostgresqlKernel extends DbModelPostgresql {
  /**
   * Create the object and initialize if necessary the driver
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public DbModelPostgresqlKernel() throws WaarpDatabaseNoConnectionException {
    // nothing
  }

  @Override
  public final void createTables(final DbSession session)
      throws WaarpDatabaseNoConnectionException {
    createTableMonitoring(session);
  }

  public static void createTableMonitoring(final DbSession session)
      throws WaarpDatabaseNoConnectionException {
    // Create tables: logs
    final String createTableH2 = "CREATE TABLE ";
    final String primaryKey = " PRIMARY KEY ";
    final String notNull = " NOT NULL ";

    final DbRequest request = new DbRequest(session);
    // TRANSLOG
    StringBuilder action =
        new StringBuilder(createTableH2 + DbTransferLog.table + '(');
    final DbTransferLog.Columns[] acolumns = DbTransferLog.Columns.values();
    for (int i = 0; i < acolumns.length; i++) {
      action.append(acolumns[i].name())
            .append(DBType.getType(DbTransferLog.dbTypes[i])).append(notNull)
            .append(", ");
    }
    // Several columns for primary key
    action.append(" CONSTRAINT TRANSLOG_PK " + primaryKey + '(');
    for (int i = DbTransferLog.NBPRKEY; i > 1; i--) {
      action.append(acolumns[acolumns.length - i].name()).append(',');
    }
    action.append(acolumns[acolumns.length - 1].name()).append("))");
    SysErrLogger.FAKE_LOGGER.sysout(action);
    try {
      request.query(action.toString());
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      // XXX FIX No return
    } finally {
      request.close();
    }
    // Index TRANSLOG
    action = new StringBuilder(
        "CREATE INDEX IDX_TRANSLOG ON " + DbTransferLog.table + '(');
    final DbTransferLog.Columns[] icolumns = DbTransferLog.indexes;
    for (int i = 0; i < icolumns.length - 1; i++) {
      action.append(icolumns[i].name()).append(", ");
    }
    action.append(icolumns[icolumns.length - 1].name()).append(')');
    SysErrLogger.FAKE_LOGGER.sysout(action);
    try {
      request.query(action.toString());
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      // XXX FIX No return
    } finally {
      request.close();
    }

    // cptrunner
    final long minimalValue = System.currentTimeMillis() + 1;
    action = new StringBuilder(
        "CREATE SEQUENCE " + DbTransferLog.fieldseq + " MINVALUE " +
        (DbConstantGateway.ILLEGALVALUE + 1) + " RESTART WITH " + minimalValue);
    SysErrLogger.FAKE_LOGGER.sysout(action);
    try {
      request.query(action.toString());
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    } finally {
      request.close();
    }
  }

  @Override
  public final void resetSequence(final DbSession session, final long newvalue)
      throws WaarpDatabaseNoConnectionException {
    DbModelFactoryGateway.resetSequenceMonitoring(session, newvalue);
  }

  @Override
  public final long nextSequence(final DbSession dbSession)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             WaarpDatabaseNoDataException {
    return DbModelFactoryGateway.nextSequenceMonitoring(dbSession);
  }

  @Override
  public final boolean upgradeDb(final DbSession session,
                                 final String version) {
    return false;
  }

  @Override
  public final boolean needUpgradeDb(final DbSession session,
                                     final String version,
                                     final boolean tryFix) {
    return false;
  }
}
