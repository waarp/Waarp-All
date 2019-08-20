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

import oracle.jdbc.driver.OracleDriver;
import oracle.jdbc.pool.OracleConnectionPoolDataSource;
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
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Timer;

/**
 * Oracle Database Model implementation
 */
public abstract class DbModelOracle extends DbModelAbstract {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbModelOracle.class);

  private static final DbType type = DbType.Oracle;

  protected static class DbTypeResolverOracle
      extends DbModelAbstract.DbTypeResolver {

    @Override
    public String getType(final int sqlType) {
      return DBType.getType(sqlType);
    }
  }

  static {
    dbTypeResolver = new DbTypeResolverOracle();
  }

  protected OracleConnectionPoolDataSource oracleConnectionPoolDataSource;
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
  protected DbModelOracle(String dbserver, String dbuser, String dbpasswd,
                          Timer timer, long delay)
      throws WaarpDatabaseNoConnectionException {
    this();

    try {
      oracleConnectionPoolDataSource = new OracleConnectionPoolDataSource();
    } catch (final SQLException e) {
      // then no pool
      oracleConnectionPoolDataSource = null;
      return;
    }
    oracleConnectionPoolDataSource.setURL(dbserver);
    oracleConnectionPoolDataSource.setUser(dbuser);
    oracleConnectionPoolDataSource.setPassword(dbpasswd);
    pool = new DbConnectionPool(oracleConnectionPoolDataSource, timer, delay);
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
  protected DbModelOracle(String dbserver, String dbuser, String dbpasswd)
      throws WaarpDatabaseNoConnectionException {
    this();

    try {
      oracleConnectionPoolDataSource = new OracleConnectionPoolDataSource();
    } catch (final SQLException e) {
      // then no pool
      oracleConnectionPoolDataSource = null;
      return;
    }
    oracleConnectionPoolDataSource.setURL(dbserver);
    oracleConnectionPoolDataSource.setUser(dbuser);
    oracleConnectionPoolDataSource.setPassword(dbpasswd);
    pool = new DbConnectionPool(oracleConnectionPoolDataSource);
    logger.warn(
        "Some info: MaxConn: " + pool.getMaxConnections() + " LogTimeout: " +
        pool.getLoginTimeout() + " ForceClose: " + pool.getTimeoutForceClose());
  }

  /**
   * Create the object and initialize if necessary the driver
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  protected DbModelOracle() throws WaarpDatabaseNoConnectionException {
    if (DbModelFactory.classLoaded.contains(type.name())) {
      return;
    }
    try {
      DriverManager.registerDriver(new OracleDriver());
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
        // nothing
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
    if (pool == null) {
      return super.getDbConnection(server, user, passwd);
    }
    synchronized (this) {
      try {
        return pool.getConnection();
      } catch (final SQLException e) {
        // try to renew the pool
        oracleConnectionPoolDataSource = new OracleConnectionPoolDataSource();
        oracleConnectionPoolDataSource.setURL(server);
        oracleConnectionPoolDataSource.setUser(user);
        oracleConnectionPoolDataSource.setPassword(passwd);
        pool.resetPoolDataSource(oracleConnectionPoolDataSource);
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

  protected enum DBType {
    CHAR(Types.CHAR, " CHAR(3) "), VARCHAR(Types.VARCHAR, " VARCHAR2(4000) "),
    NVARCHAR(Types.NVARCHAR, " VARCHAR2(1000) "),
    LONGVARCHAR(Types.LONGVARCHAR, " CLOB "), BIT(Types.BIT, " CHAR(1) "),
    TINYINT(Types.TINYINT, " SMALLINT "),
    SMALLINT(Types.SMALLINT, " SMALLINT "), INTEGER(Types.INTEGER, " INTEGER "),
    BIGINT(Types.BIGINT, " NUMBER(38,0) "), REAL(Types.REAL, " REAL "),
    DOUBLE(Types.DOUBLE, " DOUBLE PRECISION "),
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

  @Override
  public void resetSequence(DbSession session, long newvalue)
      throws WaarpDatabaseNoConnectionException {
    final String action = "DROP SEQUENCE " + DbDataModel.fieldseq;
    final String action2 =
        "CREATE SEQUENCE " + DbDataModel.fieldseq + " MINVALUE " +
        (DbConstant.ILLEGALVALUE + 1) + " START WITH " + newvalue;
    final DbRequest request = new DbRequest(session);
    try {
      request.query(action);
      request.query(action2);
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
  public long nextSequence(DbSession dbSession)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException,
             WaarpDatabaseNoDataException {
    long result = DbConstant.ILLEGALVALUE;
    final String action =
        "SELECT " + DbDataModel.fieldseq + ".NEXTVAL FROM DUAL";
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
  protected String validConnectionString() {
    return "select 1 from dual";
  }

  @Override
  public String limitRequest(String allfields, String request, int nb) {
    if (nb == 0) {
      return request;
    }
    return "select " + allfields + " from ( " + request +
           " ) where rownum <= " + nb;
  }
}
