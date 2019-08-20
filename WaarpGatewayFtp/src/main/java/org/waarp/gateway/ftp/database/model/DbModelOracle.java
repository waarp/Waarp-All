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
package org.waarp.gateway.ftp.database.model;

import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;

/**
 * Oracle Database Model implementation
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
    org.waarp.gateway.kernel.database.model.DbModelOracle
        .createTableMonitoring(session);
  }

  @Override
  public void resetSequence(DbSession session, long newvalue)
      throws WaarpDatabaseNoConnectionException {
    org.waarp.gateway.kernel.database.model.DbModelOracle
        .resetSequenceMonitoring(session, newvalue);
  }

  @Override
  public long nextSequence(DbSession dbSession)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             WaarpDatabaseNoDataException {
    return org.waarp.gateway.kernel.database.model.DbModelOracle
        .nextSequenceMonitoring(dbSession);
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
