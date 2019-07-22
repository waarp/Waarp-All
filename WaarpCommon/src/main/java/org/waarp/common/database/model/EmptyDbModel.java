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
package org.waarp.common.database.model;

import org.waarp.common.database.DbConstant;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Empty DbModel
 *
 *
 */
public class EmptyDbModel implements DbModel {

  /**
   *
   */
  public EmptyDbModel() {
  }

  @Override
  public Connection getDbConnection(String server, String user, String passwd)
      throws SQLException {
    return null;
  }

  @Override
  public void releaseResources() {
  }

  @Override
  public int currentNumberOfPooledConnections() {
    return 0;
  }

  @Override
  public DbType getDbType() {
    return DbType.none;
  }

  @Override
  public void createTables(DbSession session)
      throws WaarpDatabaseNoConnectionException {
  }

  @Override
  public void resetSequence(DbSession session, long newvalue)
      throws WaarpDatabaseNoConnectionException {
  }

  @Override
  public long nextSequence(DbSession dbSession)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             WaarpDatabaseNoDataException {
    return DbConstant.ILLEGALVALUE;
  }

  @Override
  public void validConnection(DbSession dbSession)
      throws WaarpDatabaseNoConnectionException {
  }

  @Override
  public String limitRequest(String allfields, String request, int limit) {
    return null;
  }

  @Override
  public boolean upgradeDb(DbSession session, String version)
      throws WaarpDatabaseNoConnectionException {
    return true;
  }

  @Override
  public boolean needUpgradeDb(DbSession session, String version,
                               boolean tryFix)
      throws WaarpDatabaseNoConnectionException {
    return false;
  }

}
