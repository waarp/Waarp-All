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

import com.mysql.jdbc.Driver;
import io.netty.util.internal.PlatformDependent;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.DbConnectionPool;
import org.waarp.common.database.DbConstant;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpSystemUtil;

import javax.sql.ConnectionPoolDataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Timer;

/**
 * MySQL Database Model implementation
 */
public abstract class DbModelMysql extends DbModelCommonMariadbMySql {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbModelMysql.class);

  private static final DbType type = DbType.MySQL;
  private static final String MYSQL_CONNECTION_POOL_DATA_SOURCE_JRE6 =
      "com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource";
  private static final String MYSQL_CONNECTION_POOL_DATA_SOURCE_JRE8 =
      "com.mysql.cj.jdbc.MysqlConnectionPoolDataSource";
  public static final String MYSQL_DRIVER_JRE6 = "com.mysql.jdbc.Driver";
  public static final String MYSQL_DRIVER_JRE8 = "com.mysql.cj.jdbc.Driver";
  private static final String
      CANNOT_INITIALIZE_MYSQL_CONNECTION_POOL_DATA_SOURCE =
      "Cannot initialize MysqlConnectionPoolDataSource";

  protected static class DbTypeResolverMySQL
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
      return "CREATE INDEX ";
    }

    @Override
    public final DbType getDbType() {
      return type;
    }
  }

  static {
    dbTypeResolver = new DbTypeResolverMySQL();
  }

  protected ConnectionPoolDataSource mysqlConnectionPoolDataSource;
  protected DbConnectionPool pool;

  @Override
  public final DbType getDbType() {
    return type;
  }

  /**
   * @return a ConnectionPoolDataSource as MysqlConnectionPoolDataSource
   *
   * @throws WaarpDatabaseNoConnectionException if class not found
   */
  private ConnectionPoolDataSource createMysqlConnectionPoolDataSource(
      final String dbServer, final String dbUser, final String dbPwd)
      throws WaarpDatabaseNoConnectionException {
    Class<?> mysqlConnectionPoolDataSourceClass = null;
    if (PlatformDependent.javaVersion() >= 8) {
      try {
        Class.forName(MYSQL_DRIVER_JRE8);
        mysqlConnectionPoolDataSourceClass =
            Class.forName(MYSQL_CONNECTION_POOL_DATA_SOURCE_JRE8);
      } catch (final Exception e) {
        try {
          mysqlConnectionPoolDataSourceClass =
              Class.forName(MYSQL_CONNECTION_POOL_DATA_SOURCE_JRE6);
        } catch (final ClassNotFoundException classNotFoundException) {
          try {
            mysqlConnectionPoolDataSourceClass =
                Class.forName(MYSQL_CONNECTION_POOL_DATA_SOURCE_JRE8);
          } catch (final ClassNotFoundException e2) {
            logger.error(CANNOT_INITIALIZE_MYSQL_CONNECTION_POOL_DATA_SOURCE);
            throw new WaarpDatabaseNoConnectionException(
                CANNOT_INITIALIZE_MYSQL_CONNECTION_POOL_DATA_SOURCE, e);
          }
        }
      }
    } else {
      try {
        mysqlConnectionPoolDataSourceClass =
            Class.forName(MYSQL_CONNECTION_POOL_DATA_SOURCE_JRE6);
      } catch (final ClassNotFoundException e) {
        logger.error(CANNOT_INITIALIZE_MYSQL_CONNECTION_POOL_DATA_SOURCE);
        throw new WaarpDatabaseNoConnectionException(
            CANNOT_INITIALIZE_MYSQL_CONNECTION_POOL_DATA_SOURCE, e);
      }
    }
    try {
      final ConnectionPoolDataSource cpds =
          (ConnectionPoolDataSource) WaarpSystemUtil.newInstance(
              mysqlConnectionPoolDataSourceClass);
      Method method = mysqlConnectionPoolDataSourceClass.getMethod("setUrl",
                                                                   dbServer.getClass());
      method.invoke(cpds, dbServer);
      method = mysqlConnectionPoolDataSourceClass.getMethod("setUser",
                                                            dbUser.getClass());
      method.invoke(cpds, dbUser);
      method = mysqlConnectionPoolDataSourceClass.getMethod("setPassword",
                                                            dbPwd.getClass());
      method.invoke(cpds, dbPwd);
      return cpds;
    } catch (final Exception e) {
      throw new WaarpDatabaseNoConnectionException(
          CANNOT_INITIALIZE_MYSQL_CONNECTION_POOL_DATA_SOURCE, e);
    }
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
  protected DbModelMysql(final String dbserver, final String dbuser,
                         final String dbpasswd, final Timer timer,
                         final long delay)
      throws WaarpDatabaseNoConnectionException {
    this();
    mysqlConnectionPoolDataSource =
        createMysqlConnectionPoolDataSource(dbserver, dbuser, dbpasswd);
    // Create a pool with no limit
    pool = new DbConnectionPool(mysqlConnectionPoolDataSource, timer, delay);
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
  protected DbModelMysql(final String dbserver, final String dbuser,
                         final String dbpasswd)
      throws WaarpDatabaseNoConnectionException {
    this();
    mysqlConnectionPoolDataSource =
        createMysqlConnectionPoolDataSource(dbserver, dbuser, dbpasswd);
    // Create a pool with no limit
    pool = new DbConnectionPool(mysqlConnectionPoolDataSource);
    logger.warn(
        "Some info: MaxConn: " + pool.getMaxConnections() + " LogTimeout: " +
        pool.getLoginTimeout() + " ForceClose: " + pool.getTimeoutForceClose());
  }

  /**
   * Create the object and initialize if necessary the driver
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  protected DbModelMysql() throws WaarpDatabaseNoConnectionException {
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
  public final void releaseResources() {
    if (pool != null) {
      try {
        pool.dispose();
      } catch (final SQLException ignored) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
      }
    }
    pool = null;
  }

  @Override
  public synchronized int currentNumberOfPooledConnections() {
    if (pool != null) {
      return pool.getActiveConnections();
    }
    return DbAdmin.getNbConnection();
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
          try {
            mysqlConnectionPoolDataSource =
                createMysqlConnectionPoolDataSource(server, user, passwd);
          } catch (final WaarpDatabaseNoConnectionException e1) {
            logger.debug("Cannot found MySQLPooled class", e1);
            pool.dispose();
            pool = null;
            return super.getDbConnection(server, user, passwd);
          }
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

}
