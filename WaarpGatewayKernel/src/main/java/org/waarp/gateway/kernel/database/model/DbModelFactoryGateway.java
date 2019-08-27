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

import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.DbConstant;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbRequest;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.database.model.DbModel;
import org.waarp.common.database.model.DbModelFactory;
import org.waarp.common.database.model.DbType;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.gateway.kernel.database.data.DbTransferLog;

import java.sql.SQLException;

/**
 * Factory to store the Database Model object
 */
public class DbModelFactoryGateway extends DbModelFactory {

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
        dbModel = new DbModelH2Kernel(dbserver, dbuser, dbpasswd);
        break;
      case Oracle:
        dbModel = new DbModelOracleKernel(dbserver, dbuser, dbpasswd);
        break;
      case PostGreSQL:
        dbModel = new DbModelPostgresqlKernel();
        break;
      case MySQL:
        dbModel = new DbModelMysqlKernel(dbserver, dbuser, dbpasswd);
        break;
      case MariaDB:
        dbModel = new DbModelMariaDbKernel(dbserver, dbuser, dbpasswd);
        break;
      default:
        throw new WaarpDatabaseNoConnectionException(
            "TypeDriver unknown: " + type);
    }
    return new DbAdmin(dbModel, dbserver, dbuser, dbpasswd, write);
  }

  public static void resetSequenceMonitoring(final DbSession session,
                                             final long newvalue)
      throws WaarpDatabaseNoConnectionException {
    final String action =
        "ALTER SEQUENCE " + DbTransferLog.fieldseq + " MINVALUE " +
        (DbConstant.ILLEGALVALUE + 1) + " RESTART WITH " + newvalue;
    final DbRequest request = new DbRequest(session);
    try {
      request.query(action);
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      return;
    } finally {
      request.close();
    }
    SysErrLogger.FAKE_LOGGER.sysout(action);
  }

  public static long nextSequenceMonitoring(final DbSession dbSession)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             WaarpDatabaseNoDataException {
    long result = DbConstant.ILLEGALVALUE;
    final String action = "SELECT NEXTVAL('" + DbTransferLog.fieldseq + "')";
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
}
