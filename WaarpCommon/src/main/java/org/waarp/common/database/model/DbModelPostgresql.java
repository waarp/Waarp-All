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

import org.postgresql.Driver;
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

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Types;

/**
 * PostGreSQL Database Model implementation
 *
 *
 */
public abstract class DbModelPostgresql extends DbModelAbstract {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbModelPostgresql.class);

  private static final DbType type = DbType.PostGreSQL;

  protected Boolean useIsValid;

  @Override
  public DbType getDbType() {
    return type;
  }

  /**
   * Create the object and initialize if necessary the driver
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public DbModelPostgresql() throws WaarpDatabaseNoConnectionException {
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
    // No pooling connection yet available through URL and not for production purpose
    /*
     * Quoting the PostgreSQL documentation: from http://jdbc.postgresql.org/documentation/head/ds-ds.html In
     * general it is not recommended to use the PostgreSQL™ provided connection pool.
     *
     */
    /*
     * PGPoolingDataSource source = new PGPoolingDataSource(); source.setDataSourceName("A Data Source");
     * source.setServerName("localhost"); source.setDatabaseName("test"); source.setUser("testuser");
     * source.setPassword("testpassword"); source.setMaxConnections(10);
     */
  }

  protected enum DBType {
    CHAR(Types.CHAR, " CHAR(3) "), VARCHAR(Types.VARCHAR, " VARCHAR(8096) "),
    NVARCHAR(Types.NVARCHAR, " VARCHAR(8096) "),
    LONGVARCHAR(Types.LONGVARCHAR, " TEXT "), BIT(Types.BIT, " BOOLEAN "),
    TINYINT(Types.TINYINT, " INT2 "), SMALLINT(Types.SMALLINT, " SMALLINT "),
    INTEGER(Types.INTEGER, " INTEGER "), BIGINT(Types.BIGINT, " BIGINT "),
    REAL(Types.REAL, " REAL "), DOUBLE(Types.DOUBLE, " DOUBLE PRECISION "),
    VARBINARY(Types.VARBINARY, " BYTEA "), DATE(Types.DATE, " DATE "),
    TIMESTAMP(Types.TIMESTAMP, " TIMESTAMP ");

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
        default:
          return null;
      }
    }
  }

  @Override
  public void createTables(DbSession session)
      throws WaarpDatabaseNoConnectionException {
    // Create tables: configuration, hosts, rules, runner, cptrunner
    final String createTableH2 = "CREATE TABLE ";
    final String primaryKey = " PRIMARY KEY ";
    final String notNull = " NOT NULL ";

    // Example
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
    action = "CREATE INDEX IDX_RUNNER ON " + DbDataModel.table + "(";
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

    // example of sequence
    action = "CREATE SEQUENCE " + DbDataModel.fieldseq + " MINVALUE " +
             (DbConstant.ILLEGALVALUE + 1) + " RESTART WITH " +
             (DbConstant.ILLEGALVALUE + 1);
    logger.warn(action);
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
  }

  @Override
  public void resetSequence(DbSession session, long newvalue)
      throws WaarpDatabaseNoConnectionException {
    final String action =
        "ALTER SEQUENCE " + DbDataModel.fieldseq + " MINVALUE " +
        (DbConstant.ILLEGALVALUE + 1) + " RESTART WITH " + newvalue;
    final DbRequest request = new DbRequest(session);
    try {
      request.query(action);
    } catch (final WaarpDatabaseNoConnectionException e) {
      logger.warn("ResetSequence Error", e);
      return;
    } catch (final WaarpDatabaseSqlException e) {
      logger.warn("ResetSequence Error", e);
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
