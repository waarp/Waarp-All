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

import org.h2.Driver;
import org.h2.jdbcx.JdbcConnectionPool;
import org.waarp.common.database.DbAdmin;
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

/**
 * H2 Database Model implementation
 *
 *
 */
public abstract class DbModelH2 extends DbModelAbstract {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbModelH2.class);

  private static final DbType type = DbType.H2;

  protected JdbcConnectionPool pool;

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
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public DbModelH2(String dbserver, String dbuser, String dbpasswd)
      throws WaarpDatabaseNoConnectionException {
    this();
    pool = JdbcConnectionPool.create(dbserver, dbuser, dbpasswd);
    pool.setMaxConnections(DbConstant.MAXCONNECTION);
    pool.setLoginTimeout(DbConstant.DELAYMAXCONNECTION);
    logger.info(
        "Some info: MaxConn: " + pool.getMaxConnections() + " LogTimeout: " +
        pool.getLoginTimeout());
  }

  /**
   * Create the object and initialize if necessary the driver
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  protected DbModelH2() throws WaarpDatabaseNoConnectionException {
    if (DbModelFactory.classLoaded.contains(type.name())) {
      return;
    }
    try {
      DriverManager.registerDriver(new Driver());
      DbModelFactory.classLoaded.add(type.name());
    } catch (final SQLException e) {
      // SQLException
      logger.error(
          "Cannot register Driver " + type.name() + " " + e.getMessage());
      DbSession.error(e);
      throw new WaarpDatabaseNoConnectionException(
          "Cannot load database drive:" + type.name(), e);
    }
  }

  @Override
  public void releaseResources() {
    if (pool != null) {
      pool.dispose();
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
          pool.dispose();
          pool = JdbcConnectionPool.create(server, user, passwd);
          pool.setMaxConnections(DbConstant.MAXCONNECTION);
          pool.setLoginTimeout(DbConstant.DELAYMAXCONNECTION);
          logger.info("Some info: MaxConn: " + pool.getMaxConnections() +
                      " LogTimeout: " + pool.getLoginTimeout());
          return pool.getConnection();
        }
      }
    }
    return super.getDbConnection(server, user, passwd);
  }

  protected enum DBType {
    CHAR(Types.CHAR, " CHAR(3) "), VARCHAR(Types.VARCHAR, " VARCHAR(8096) "),
    NVARCHAR(Types.NVARCHAR, " VARCHAR(8096) "),
    LONGVARCHAR(Types.LONGVARCHAR, " LONGVARCHAR "),
    BIT(Types.BIT, " BOOLEAN "), TINYINT(Types.TINYINT, " TINYINT "),
    SMALLINT(Types.SMALLINT, " SMALLINT "), INTEGER(Types.INTEGER, " INTEGER "),
    BIGINT(Types.BIGINT, " BIGINT "), REAL(Types.REAL, " REAL "),
    DOUBLE(Types.DOUBLE, " DOUBLE "), VARBINARY(Types.VARBINARY, " BINARY "),
    DATE(Types.DATE, " DATE "), TIMESTAMP(Types.TIMESTAMP, " TIMESTAMP "),
    CLOB(Types.CLOB, " CLOB "), BLOB(Types.BLOB, " BLOB ");

    public int type;

    public String constructor;

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
        case Types.CLOB:
          return CLOB.constructor;
        case Types.BLOB:
          return BLOB.constructor;
        default:
          return null;
      }
    }
  }

  @Override
  public void createTables(DbSession session)
      throws WaarpDatabaseNoConnectionException {
    // Create tables: configuration, hosts, rules, runner, cptrunner
    final String createTableH2 = "CREATE TABLE IF NOT EXISTS ";
    final String primaryKey = " PRIMARY KEY ";
    final String notNull = " NOT NULL ";

    // Example
    /*

    String action = createTableH2 + DbDataModel.table + "(";
    final DbDataModel.Columns[] ccolumns = DbDataModel.Columns.values();
    for (int i = 0; i < ccolumns.length - 1; i++) {
      action += ccolumns[i].name() + DBType.getType(DbDataModel.dbTypes[i]) +
                notNull + ", ";
    }
    action += ccolumns[ccolumns.length - 1].name() +
              DBType.getType(DbDataModel.dbTypes[ccolumns.length - 1]) +
              primaryKey + ")";
    logger.warn(action);
    final DbRequest request = new DbRequest(session);
    try {
      request.query(action);
    } catch (final WaarpDatabaseNoConnectionException e) {
      logger.warn("CreateTables Error", e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      logger.warn("CreateTables Error", e);
      return;
    } finally {
      request.close();
    }

    // Index example
    action =
        "CREATE INDEX IF NOT EXISTS IDX_RUNNER ON " + DbDataModel.table + "(";
    final DbDataModel.Columns[] icolumns = DbDataModel.indexes;
    for (int i = 0; i < icolumns.length - 1; i++) {
      action += icolumns[i].name() + ", ";
    }
    action += icolumns[icolumns.length - 1].name() + ")";
    logger.warn(action);
    try {
      request.query(action);
    } catch (final WaarpDatabaseNoConnectionException e) {
      logger.warn("CreateTables Error", e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      return;
    } finally {
      request.close();
    }

    // example sequence
    action =
        "CREATE SEQUENCE IF NOT EXISTS " + DbDataModel.fieldseq + " MINVALUE " +
        (DbConstant.ILLEGALVALUE + 1) + " START WITH " +
        (DbConstant.ILLEGALVALUE + 1);
    logger.warn(action);
    try {
      request.query(action);
    } catch (final WaarpDatabaseNoConnectionException e) {
      logger.warn("CreateTables Error", e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      // version 1.3.173
      action = "CREATE SEQUENCE IF NOT EXISTS " + DbDataModel.fieldseq +
               " START WITH " + (DbConstant.ILLEGALVALUE + 1);
      logger.warn(action);
      try {
        request.query(action);
      } catch (final WaarpDatabaseNoConnectionException e2) {
        logger.warn("CreateTables Error", e2);
        return;
      } catch (final WaarpDatabaseSqlException e2) {
        logger.warn("CreateTables Error", e2);
        return;
      }
    } finally {
      request.close();
    }

     */
  }

  @Override
  public void resetSequence(DbSession session, long newvalue)
      throws WaarpDatabaseNoConnectionException {
    final String action =
        "ALTER SEQUENCE " + DbDataModel.fieldseq + " RESTART WITH " + newvalue;
    final DbRequest request = new DbRequest(session);
    try {
      request.query(action);
    } catch (final WaarpDatabaseNoConnectionException e) {
      logger.warn("ResetSequences Error", e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      logger.warn("ResetSequences Error", e);
      return;
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
    final String action = "SELECT NEXTVAL('" + DbDataModel.fieldseq + "')";
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
    return "select 1";
  }

  @Override
  public String limitRequest(String allfields, String request, int nb) {
    if (nb == 0) {
      return request;
    }
    return request + " LIMIT " + nb;
  }

}
