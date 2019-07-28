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
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import org.waarp.common.database.DbAdmin;
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
import java.sql.Types;
import java.util.Timer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * MySQL Database Model implementation
 */
public abstract class DbModelMysql extends DbModelAbstract {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbModelMysql.class);

  private static final DbType type = DbType.MySQL;

  protected MysqlConnectionPoolDataSource mysqlConnectionPoolDataSource;
  protected DbConnectionPool pool;

  @Override
  public DbType getDbType() {
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
  protected DbModelMysql(String dbserver, String dbuser, String dbpasswd,
                         Timer timer, long delay)
      throws WaarpDatabaseNoConnectionException {
    this();
    mysqlConnectionPoolDataSource = new MysqlConnectionPoolDataSource();
    mysqlConnectionPoolDataSource.setUrl(dbserver);
    mysqlConnectionPoolDataSource.setUser(dbuser);
    mysqlConnectionPoolDataSource.setPassword(dbpasswd);
    // Create a pool with no limit
    pool = new DbConnectionPool(mysqlConnectionPoolDataSource, timer, delay);
    logger.info(
        "Some info: MaxConn: " + pool.getMaxConnections() + " LogTimeout: " +
        pool.getLoginTimeout() + " ForceClose: " + pool.getTimeoutForceClose());
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
  protected DbModelMysql(String dbserver, String dbuser, String dbpasswd)
      throws WaarpDatabaseNoConnectionException {
    this();
    mysqlConnectionPoolDataSource = new MysqlConnectionPoolDataSource();
    mysqlConnectionPoolDataSource.setUrl(dbserver);
    mysqlConnectionPoolDataSource.setUser(dbuser);
    mysqlConnectionPoolDataSource.setPassword(dbpasswd);
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
      DbSession.error(e);
      throw new WaarpDatabaseNoConnectionException(
          "Cannot load database drive:" + type.name(), e);
    }
  }

  @Override
  public void releaseResources() {
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
  public int currentNumberOfPooledConnections() {
    if (pool != null) {
      return pool.getActiveConnections();
    }
    return DbAdmin.getNbConnection();
  }

  @Override
  public Connection getDbConnection(String server, String user, String passwd)
      throws SQLException {
    synchronized (this) {
      if (pool != null) {
        try {
          return pool.getConnection();
        } catch (final SQLException e) {
          // try to renew the pool
          mysqlConnectionPoolDataSource = new MysqlConnectionPoolDataSource();
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

  protected enum DBType {
    CHAR(Types.CHAR, " CHAR(3) "), VARCHAR(Types.VARCHAR, " VARCHAR(8096) "),
    /**
     * Used in replacement of VARCHAR for MYSQL/MARIADB (limitation of size
     * if
     * in Primary Key)
     */
    NVARCHAR(Types.VARCHAR, " VARCHAR(255) "),
    LONGVARCHAR(Types.LONGVARCHAR, " TEXT "), BIT(Types.BIT, " BOOLEAN "),
    TINYINT(Types.TINYINT, " TINYINT "), SMALLINT(Types.SMALLINT, " SMALLINT "),
    INTEGER(Types.INTEGER, " INTEGER "), BIGINT(Types.BIGINT, " BIGINT "),
    REAL(Types.REAL, " FLOAT "), DOUBLE(Types.DOUBLE, " DOUBLE "),
    VARBINARY(Types.VARBINARY, " BLOB "), DATE(Types.DATE, " DATE "),
    TIMESTAMP(Types.TIMESTAMP, " TIMESTAMP ");

    public final int type;

    public final String constructor;

    DBType(int type, String constructor) {
      this.type = type;
      this.constructor = constructor;
    }

    public static String getType(int sqltype) {
      switch (sqltype) {
        case Types.CHAR:
          return CHAR.constructor;
        case Types.VARCHAR:
          return VARCHAR.constructor;
        case Types.NVARCHAR:
          return NVARCHAR.constructor;
        case Types.LONGVARCHAR:
          return LONGVARCHAR.constructor;
        case Types.BIT:
          return BIT.constructor;
        case Types.TINYINT:
          return TINYINT.constructor;
        case Types.SMALLINT:
          return SMALLINT.constructor;
        case Types.INTEGER:
          return INTEGER.constructor;
        case Types.BIGINT:
          return BIGINT.constructor;
        case Types.REAL:
          return REAL.constructor;
        case Types.DOUBLE:
          return DOUBLE.constructor;
        case Types.VARBINARY:
          return VARBINARY.constructor;
        case Types.DATE:
          return DATE.constructor;
        case Types.TIMESTAMP:
          return TIMESTAMP.constructor;
        default:
          return null;
      }
    }
  }

  private final ReentrantLock lock = new ReentrantLock();

  @Override
  public void resetSequence(DbSession session, long newvalue)
      throws WaarpDatabaseNoConnectionException {
    final String action =
        "UPDATE Sequences SET seq = " + newvalue + " WHERE name = '" +
        DbDataModel.fieldseq + '\'';
    final DbRequest request = new DbRequest(session);
    try {
      request.query(action);
    } catch (final WaarpDatabaseNoConnectionException e) {
      logger.warn("ResetSequence Error", e);
    } catch (final WaarpDatabaseSqlException e) {
      logger.warn("ResetSequence Error", e);
    } finally {
      request.close();
    }
    logger.warn(action);
  }

  @Override
  public synchronized long nextSequence(DbSession dbSession)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             WaarpDatabaseNoDataException {
    lock.lock();
    try {
      long result = DbConstant.ILLEGALVALUE;
      String action =
          "SELECT seq FROM Sequences WHERE name = '" + DbDataModel.fieldseq +
          "' FOR UPDATE";
      final DbPreparedStatement preparedStatement =
          new DbPreparedStatement(dbSession);
      try {
        dbSession.getConn().setAutoCommit(false);
      } catch (final SQLException ignored) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
      }
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
        } else {
          throw new WaarpDatabaseNoDataException(
              "No sequence found. Must be initialized first");
        }
      } finally {
        preparedStatement.realClose();
      }
      action =
          "UPDATE Sequences SET seq = " + (result + 1) + " WHERE name = '" +
          DbDataModel.fieldseq + '\'';
      try {
        preparedStatement.createPrepareStatement(action);
        // Limit the search
        preparedStatement.executeUpdate();
      } finally {
        preparedStatement.realClose();
      }
      return result;
    } finally {
      try {
        dbSession.getConn().setAutoCommit(true);
      } catch (final SQLException ignored) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
      }
      lock.unlock();
    }
  }

  @Override
  protected String validConnectionString() {
    return "select 1 from dual";
  }

  @Override
  public String limitRequest(String allfields, String request, int nb) {
    if (nb == 0) {
      return request;
    }
    return request + " LIMIT " + nb;
  }

}
