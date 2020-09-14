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
package org.waarp.common.database;

import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Class to handle request
 */
public class DbRequest {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbRequest.class);
  private static final String SQL_EXCEPTION_REQUEST = "SQL Exception Request:";
  private static final String SQL_EXCEPTION_REQUEST1 = "SQL Exception Request:";

  /**
   * Internal Statement
   */
  private Statement stmt;

  /**
   * Internal Result Set
   */
  private ResultSet rs;

  /**
   * Internal DB Session
   */
  private final DbSession ls;

  /**
   * Create a new request from the DbSession
   *
   * @param ls
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public DbRequest(final DbSession ls)
      throws WaarpDatabaseNoConnectionException {
    if (ls.isDisActive()) {
      ls.checkConnection();
    }
    this.ls = ls;
  }

  /**
   * Create a statement with some particular options
   *
   * @return the new Statement
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  private Statement createStatement()
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    if (ls == null) {
      throw new WaarpDatabaseNoConnectionException("No connection");
    }
    if (ls.getConn() == null) {
      throw new WaarpDatabaseNoConnectionException("No connection");
    }
    if (ls.isDisActive()) {
      ls.checkConnection();
    }
    try {
      return ls.getConn().createStatement();
    } catch (final SQLException e) {
      ls.checkConnection();
      try {
        return ls.getConn().createStatement();
      } catch (final SQLException e1) {
        throw new WaarpDatabaseSqlException("Error while Create Statement", e);
      }
    }
  }

  /**
   * Execute a SELECT statement and set of Result. The statement must not be
   * an
   * update/insert/delete. The
   * previous statement and resultSet are closed.
   *
   * @param select
   *
   * @throws WaarpDatabaseSqlException
   * @throws WaarpDatabaseNoConnectionException
   */
  public void select(final String select)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    close();
    stmt = createStatement();
    // or alternatively, if you don't know ahead of time that
    // the query will be a SELECT...
    try {
      if (stmt.execute(select)) {
        rs = stmt.getResultSet();
      }
    } catch (final SQLException e) {
      logger.error(SQL_EXCEPTION_REQUEST + select + ' ' + e.getMessage());
      DbSession.error(e);
      ls.checkConnectionNoException();
      throw new WaarpDatabaseSqlException(SQL_EXCEPTION_REQUEST1 + select, e);
    }
  }

  /**
   * Execute a SELECT statement and set of Result. The statement must not be
   * an
   * update/insert/delete. The
   * previous statement and resultSet are closed. The timeout is applied if >
   * 0.
   *
   * @param select
   * @param timeout in seconds
   *
   * @throws WaarpDatabaseSqlException
   * @throws WaarpDatabaseNoConnectionException
   */
  public void select(final String select, final int timeout)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    close();
    stmt = createStatement();
    if (timeout > 0) {
      try {
        stmt.setQueryTimeout(timeout);
      } catch (final SQLException e1) {
        // ignore
      }
    }
    // or alternatively, if you don't know ahead of time that
    // the query will be a SELECT...
    try {
      if (stmt.execute(select)) {
        rs = stmt.getResultSet();
      }
    } catch (final SQLException e) {
      logger.error(SQL_EXCEPTION_REQUEST1 + select + ' ' + e.getMessage());
      DbSession.error(e);
      ls.checkConnectionNoException();
      throw new WaarpDatabaseSqlException(SQL_EXCEPTION_REQUEST1 + select, e);
    }
  }

  /**
   * Execute a UPDATE/INSERT/DELETE statement and returns the number of row.
   * The
   * previous statement and
   * resultSet are closed.
   *
   * @param query
   *
   * @return the number of row in the query
   *
   * @throws WaarpDatabaseSqlException
   * @throws WaarpDatabaseNoConnectionException
   */
  public int query(final String query)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    close();
    stmt = createStatement();
    try {
      final int rowcount = stmt.executeUpdate(query);
      logger.debug("QUERY({}): {}", rowcount, query);
      return rowcount;
    } catch (final SQLException e) {
      logger.error(SQL_EXCEPTION_REQUEST1 + query + ' ' + e.getMessage());
      DbSession.error(e);
      ls.checkConnectionNoException();
      throw new WaarpDatabaseSqlException(SQL_EXCEPTION_REQUEST1 + query, e);
    }
  }

  /**
   * Finished a Request (ready for a new one)
   */
  public void close() {
    // it is a good idea to release
    // resources in a finally{} block
    // in reverse-order of their creation
    // if they are no-longer needed
    if (rs != null) {
      try {
        rs.close();
      } catch (final SQLException sqlEx) {
        ls.checkConnectionNoException();
      } // ignore
      rs = null;
    }
    if (stmt != null) {
      try {
        stmt.close();
      } catch (final SQLException sqlEx) {
        ls.checkConnectionNoException();
      } // ignore
      stmt = null;
    }
  }

  /**
   * Get the last ID autoincrement from the last request
   *
   * @return the long Id or DbConstant.ILLEGALVALUE (Long.MIN_VALUE) if an
   *     error
   *     occurs.
   *
   * @throws WaarpDatabaseNoDataException
   */
  public long getLastId() throws WaarpDatabaseNoDataException {
    ResultSet rstmp = null;
    long result = DbConstant.ILLEGALVALUE;
    try {
      rstmp = stmt.getGeneratedKeys();
      if (rstmp.next()) {
        result = rstmp.getLong(1);
      }
    } catch (final SQLException e) {
      DbSession.error(e);
      ls.checkConnectionNoException();
      throw new WaarpDatabaseNoDataException("No data found", e);
    } finally {
      if (rstmp != null) {
        try {
          rstmp.close();
        } catch (final SQLException e) {
          // nothing
        }
      }
    }
    return result;
  }

  /**
   * Move the cursor to the next result
   *
   * @return True if there is a next result, else False
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public boolean getNext()
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    if (rs == null) {
      logger.error("SQL ResultSet is Null into getNext");
      throw new WaarpDatabaseNoConnectionException(
          "SQL ResultSet is Null into getNext");
    }
    if (ls.isDisActive()) {
      ls.checkConnection();
      throw new WaarpDatabaseSqlException(
          "Request cannot be executed since connection was recreated between");
    }
    try {
      return rs.next();
    } catch (final SQLException e) {
      logger.warn("SQL Exception to getNextRow" + ' ' + e.getMessage());
      DbSession.error(e);
      ls.checkConnectionNoException();
      throw new WaarpDatabaseSqlException("SQL Exception to getNextRow", e);
    }
  }

  /**
   * @return The resultSet (can be used in conjunction of getNext())
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public ResultSet getResultSet() throws WaarpDatabaseNoConnectionException {
    if (rs == null) {
      throw new WaarpDatabaseNoConnectionException(
          "SQL ResultSet is Null into getResultSet");
    }
    return rs;
  }

  /**
   * Test if value is null and create the string for insert/update
   *
   * @param value
   *
   * @return the string as result
   */
  public static String getIsNull(final String value) {
    return value == null? " is NULL" : " = '" + value + '\'';
  }
}
