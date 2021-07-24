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
import org.waarp.common.database.model.DbModelAbstract.DbTypeResolver;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Empty DbModel
 */
public class EmptyDbModel implements DbModel {

  /**
   *
   */
  public EmptyDbModel() {
    // nothing
  }

  @Override
  public final Connection getDbConnection(final String server,
                                          final String user,
                                          final String passwd)
      throws SQLException {
    return null;
  }

  @Override
  public final void releaseResources() {
    // nothing
  }

  @Override
  public final int currentNumberOfPooledConnections() {
    return 0;
  }

  @Override
  public final DbType getDbType() {
    return DbType.none;
  }

  @Override
  public final DbTypeResolver getDbTypeResolver() {
    return null;
  }

  @Override
  public final void createTables(final DbSession session)
      throws WaarpDatabaseNoConnectionException {
    // nothing
  }

  @Override
  public final void resetSequence(final DbSession session, final long newvalue)
      throws WaarpDatabaseNoConnectionException {
    // nothing
  }

  @Override
  public final long nextSequence(final DbSession dbSession)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             WaarpDatabaseNoDataException {
    return DbConstant.ILLEGALVALUE;
  }

  @Override
  public final void validConnection(final DbSession dbSession)
      throws WaarpDatabaseNoConnectionException {
    // nothing
  }

  @Override
  public final String limitRequest(final String allfields, final String request,
                                   final int limit) {
    return null;
  }

  @Override
  public final boolean upgradeDb(final DbSession session, final String version)
      throws WaarpDatabaseNoConnectionException {
    return true;
  }

  @Override
  public final boolean needUpgradeDb(final DbSession session,
                                     final String version, final boolean tryFix)
      throws WaarpDatabaseNoConnectionException {
    return false;
  }

}
