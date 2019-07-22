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

import org.waarp.common.database.DbConstant;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbRequest;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.gateway.kernel.database.data.DbTransferLog;

import java.sql.SQLException;

/**
 * Oracle Database Model implementation
 *
 *
 */
public class DbModelOracle
    extends org.waarp.common.database.model.DbModelOracle {
  /**
   * Create the object and initialize if necessary the driver
   *
   * @param dbserver
   * @param dbuser
   * @param dbpasswd
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public DbModelOracle(String dbserver, String dbuser, String dbpasswd)
      throws WaarpDatabaseNoConnectionException {
    super(dbserver, dbuser, dbpasswd);
  }

  @Override
  public void createTables(DbSession session)
      throws WaarpDatabaseNoConnectionException {
    // Create tables: configuration, hosts, rules, runner, cptrunner
    final String createTableH2 = "CREATE TABLE ";
    final String constraint = " CONSTRAINT ";
    final String primaryKey = " PRIMARY KEY ";
    final String notNull = " NOT NULL ";

    final DbRequest request = new DbRequest(session);
    // TRANSLOG
    String action = createTableH2 + DbTransferLog.table + "(";
    final DbTransferLog.Columns[] acolumns = DbTransferLog.Columns.values();
    for (int i = 0; i < acolumns.length; i++) {
      action += acolumns[i].name() + DBType.getType(DbTransferLog.dbTypes[i]) +
                notNull + ", ";
    }
    // Several columns for primary key
    action += constraint + " TRANSLOG_PK " + primaryKey + "(";
    for (int i = DbTransferLog.NBPRKEY; i > 1; i--) {
      action += acolumns[acolumns.length - i].name() + ",";
    }
    action += acolumns[acolumns.length - 1].name() + "))";
    System.out.println(action);
    try {
      request.query(action);
    } catch (final WaarpDatabaseNoConnectionException e) {
      e.printStackTrace();
      return;
    } catch (final WaarpDatabaseSqlException e) {
      return;
    } finally {
      request.close();
    }
    // Index TRANSLOG
    action = "CREATE INDEX IDX_TRANSLOG ON " + DbTransferLog.table + "(";
    final DbTransferLog.Columns[] icolumns = DbTransferLog.indexes;
    for (int i = 0; i < icolumns.length - 1; i++) {
      action += icolumns[i].name() + ", ";
    }
    action += icolumns[icolumns.length - 1].name() + ")";
    System.out.println(action);
    try {
      request.query(action);
    } catch (final WaarpDatabaseNoConnectionException e) {
      e.printStackTrace();
      return;
    } catch (final WaarpDatabaseSqlException e) {
      return;
    } finally {
      request.close();
    }

    // cptrunner
    action = "CREATE SEQUENCE " + DbTransferLog.fieldseq + " MINVALUE " +
             (DbConstant.ILLEGALVALUE + 1) + " START WITH " +
             (DbConstant.ILLEGALVALUE + 1);
    System.out.println(action);
    try {
      request.query(action);
    } catch (final WaarpDatabaseNoConnectionException e) {
      e.printStackTrace();
      return;
    } catch (final WaarpDatabaseSqlException e) {
      return;
    } finally {
      request.close();
    }
  }

  @Override
  public void resetSequence(DbSession session, long newvalue)
      throws WaarpDatabaseNoConnectionException {
    final String action = "DROP SEQUENCE " + DbTransferLog.fieldseq;
    final String action2 =
        "CREATE SEQUENCE " + DbTransferLog.fieldseq + " MINVALUE " +
        (DbConstant.ILLEGALVALUE + 1) + " START WITH " + newvalue;
    final DbRequest request = new DbRequest(session);
    try {
      request.query(action);
      request.query(action2);
    } catch (final WaarpDatabaseNoConnectionException e) {
      e.printStackTrace();
      return;
    } catch (final WaarpDatabaseSqlException e) {
      e.printStackTrace();
      return;
    } finally {
      request.close();
    }

    System.out.println(action);
  }

  @Override
  public long nextSequence(DbSession dbSession)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             WaarpDatabaseNoDataException {
    long result = DbConstant.ILLEGALVALUE;
    final String action =
        "SELECT " + DbTransferLog.fieldseq + ".NEXTVAL FROM DUAL";
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(dbSession);
    try {
      preparedStatement.createPrepareStatement(action);
      // Limit the search
      preparedStatement.executeQuery();
      if (preparedStatement.getNext()) {
        try {
          result = preparedStatement.getResultSet().getLong(1);
        } catch (final SQLException e) {
          throw new WaarpDatabaseSqlException(e);
        }
        return result;
      } else {
        throw new WaarpDatabaseNoDataException(
            "No sequence found. Must be initialized first");
      }
    } finally {
      preparedStatement.realClose();
    }
  }

  @Override
  public boolean upgradeDb(DbSession session, String version)
      throws WaarpDatabaseNoConnectionException {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean needUpgradeDb(DbSession session, String version,
                               boolean tryFix)
      throws WaarpDatabaseNoConnectionException {
    // TODO Auto-generated method stub
    return false;
  }
}
