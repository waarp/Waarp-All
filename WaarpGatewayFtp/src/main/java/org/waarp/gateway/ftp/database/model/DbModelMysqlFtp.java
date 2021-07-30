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
import org.waarp.common.database.model.DbModelMysql;
import org.waarp.gateway.kernel.database.model.DbModelMysqlKernel;

import java.util.concurrent.locks.ReentrantLock;

/**
 * MySQL Database Model implementation
 */
public class DbModelMysqlFtp extends DbModelMysql {
  /**
   * Create the object and initialize if necessary the driver
   *
   * @param dbserver
   * @param dbuser
   * @param dbpasswd
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public DbModelMysqlFtp(final String dbserver, final String dbuser,
                         final String dbpasswd)
      throws WaarpDatabaseNoConnectionException {
    super(dbserver, dbuser, dbpasswd);
  }

  private final ReentrantLock lock = new ReentrantLock();

  @Override
  public final void createTables(final DbSession session)
      throws WaarpDatabaseNoConnectionException {
    DbModelMysqlKernel.createTableMonitoring(session);
  }

  @Override
  public final void resetSequence(final DbSession session, final long newvalue)
      throws WaarpDatabaseNoConnectionException {
    DbModelMysqlKernel.resetSequenceMonitoring(session, newvalue);
  }

  @Override
  public synchronized long nextSequence(final DbSession dbSession)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             WaarpDatabaseNoDataException {
    return DbModelMysqlKernel.nextSequenceMonitoring(dbSession, lock);
  }

  @Override
  public final boolean upgradeDb(final DbSession session,
                                 final String version) {
    return true;
  }

  @Override
  public final boolean needUpgradeDb(final DbSession session,
                                     final String version,
                                     final boolean tryFix) {
    return false;
  }
}
