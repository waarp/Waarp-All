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

import org.mariadb.jdbc.Driver;
import org.mariadb.jdbc.MariaDbDataSource;
import org.waarp.common.database.DbConnectionPool;
import org.waarp.common.database.DbConstant;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbRequest;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.DbDataModel;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Timer;

/**
 * MariaDB Database Model implementation
 */
public abstract class DbModelMariadb extends DbModelCommonMariadbMySql {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbModelMariadb.class);

  private static final DbType type = DbType.MariaDB;

  protected static class DbTypeResolverMariadDb
      extends DbModelAbstract.DbTypeResolver {

    @Override
    public final String getType(final int sqlType) {
      return DBType.getType(sqlType);
    }

    @Override
    public final String getCreateTable() {
      return "CREATE TABLE IF NOT EXISTS ";
    }

    @Override
    public final String getCreateIndex() {
      return "CREATE INDEX IF NOT EXISTS ";
    }

    @Override
    public final DbType getDbType() {
      return type;
    }
  }

  static {
    dbTypeResolver = new DbTypeResolverMariadDb();
  }

  protected MariaDbDataSource mysqlConnectionPoolDataSource;
  protected DbConnectionPool pool;

  @Override
  public final DbType getDbType() {
    return type;
  }

  /**
   * Create the object and initialize if necessary the driver
   *
   * @param dbserver
   * @param dbuser
   * @param dbpasswd
   * @param timer
   * @param delay
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  protected DbModelMariadb(final String dbserver, final String dbuser,
                           final String dbpasswd, final Timer timer,
                           final long delay)
      throws WaarpDatabaseNoConnectionException {
    this();
    mysqlConnectionPoolDataSource = new MariaDbDataSource();
    try {
      mysqlConnectionPoolDataSource.setUrl(dbserver);
    } catch (final SQLException e) {
      throw new WaarpDatabaseNoConnectionException("Url setting is wrong", e);
    }

    try {
      mysqlConnectionPoolDataSource.setUser(dbuser);
      mysqlConnectionPoolDataSource.setPassword(dbpasswd);
    } catch (final SQLException e) {
      throw new WaarpDatabaseNoConnectionException("Wrong username or password",
                                                   e);
    }
    // Create a pool with no limit
    if (timer != null && delay != 0) {
      pool = new DbConnectionPool(mysqlConnectionPoolDataSource, timer, delay);
    } else {
      pool = new DbConnectionPool(mysqlConnectionPoolDataSource);
    }

    logger.info("Some info: MaxConn: {} LogTimeout: {} ForceClose: {}",
                pool.getMaxConnections(), pool.getLoginTimeout(),
                pool.getTimeoutForceClose());
  }

  /**
   * Create the object and initialize if necessary the driver
   *
   * @param dbserver
   * @param dbuser
   * @param dbpasswd
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  protected DbModelMariadb(final String dbserver, final String dbuser,
                           final String dbpasswd)
      throws WaarpDatabaseNoConnectionException {
    this(dbserver, dbuser, dbpasswd, null, 0);
  }

  /**
   * Create the object and initialize if necessary the driver
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  protected DbModelMariadb() throws WaarpDatabaseNoConnectionException {
    if (DbModelFactory.classLoaded.contains(type.name())) {
      return;
    }
    try {
      DriverManager.registerDriver(new Driver());
      DbModelFactory.classLoaded.add(type.name());
    } catch (final SQLException e) {
      // SQLException
      logger.error(
          "Cannot register Driver " + type.name() + ' ' + e.getMessage());
      DbConstant.error(e);
      throw new WaarpDatabaseNoConnectionException(
          "Cannot load database drive:" + type.name(), e);
    }
  }

  @Override
  public final Connection getDbConnection(final String server,
                                          final String user,
                                          final String passwd)
      throws SQLException {
    synchronized (this) {
      if (pool != null) {
        try {
          return pool.getConnection();
        } catch (final SQLException e) {
          // try to renew the pool
          mysqlConnectionPoolDataSource = new MariaDbDataSource();
          mysqlConnectionPoolDataSource.setUrl(server);
          mysqlConnectionPoolDataSource.setUser(user);
          mysqlConnectionPoolDataSource.setPassword(passwd);
          pool.resetPoolDataSource(mysqlConnectionPoolDataSource);
          try {
            return pool.getConnection();
          } catch (final SQLException e2) {
            pool.dispose();
            pool = null;
            return super.getDbConnection(server, user, passwd);
          }
        }
      }
    }
    return super.getDbConnection(server, user, passwd);
  }

  @Override
  public void resetSequence(final DbSession session, final long newvalue)
      throws WaarpDatabaseNoConnectionException {
    final String action =
        "ALTER SEQUENCE IF EXISTS " + DbDataModel.fieldseq + " MINVALUE " +
        (DbConstant.ILLEGALVALUE + 1) + " RESTART WITH " + newvalue;
    final DbRequest request = new DbRequest(session);
    try {
      request.query(action);
    } catch (final WaarpDatabaseNoConnectionException e) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    } catch (final WaarpDatabaseSqlException e) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      // Old way
      super.resetSequence(session, newvalue);
    } finally {
      request.close();
    }
    logger.warn(action);
  }

  @Override
  public long nextSequence(final DbSession dbSession)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             WaarpDatabaseNoDataException {
    long result;
    final String action = "SELECT NEXT VALUE FOR " + DbDataModel.fieldseq;
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
          // Old way
          return super.nextSequence(dbSession);
        }
        return result;
      } else {
        // Old way
        return super.nextSequence(dbSession);
      }
    } finally {
      preparedStatement.realClose();
    }
  }

  @Override
  public synchronized void releaseResources() {
    if (pool != null) {
      try {
        pool.dispose();
      } catch (final SQLException ignored) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
      }
    }
    pool = null;
  }

}
