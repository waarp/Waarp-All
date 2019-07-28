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

import org.postgresql.util.PSQLException;
import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.DbConstant;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ConcurrentModificationException;

/**
 * This Abstract class regroups common methods for all implementation classes.
 */
public abstract class DbModelAbstract implements DbModel {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbModelAbstract.class);
  private static final String CANNOT_CONNECT_TO_DATABASE =
      "Cannot connect to database";

  /**
   * Recreate the disActive session
   *
   * @param dbSession
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  private void recreateSession(DbSession dbSession)
      throws WaarpDatabaseNoConnectionException {
    DbAdmin admin = dbSession.getAdmin();
    if (admin == null) {
      if (dbSession.isAutoCommit()) {
        admin = DbConstant.admin;
      } else {
        admin = DbConstant.noCommitAdmin;
      }
    }
    DbSession newdbSession;
    newdbSession = new DbSession(admin, dbSession.isReadOnly());
    try {
      if (dbSession.getConn() != null) {
        dbSession.getConn().close();
      }
    } catch (final SQLException ignored) {
      // nothing
    } catch (final ConcurrentModificationException ignored) {
      // nothing
    }
    dbSession.setConn(newdbSession.getConn());
    DbAdmin.addConnection(dbSession.getInternalId(), dbSession);
    DbAdmin.removeConnection(newdbSession.getInternalId());
    logger.warn("Database Connection lost: database connection reopened");
  }

  /**
   * Internal use for closing connection while validating it
   *
   * @param dbSession
   */
  protected void closeInternalConnection(DbSession dbSession) {
    try {
      if (dbSession.getConn() != null) {
        dbSession.getConn().close();
      }
    } catch (final SQLException ignored) {
      // nothing
    } catch (final ConcurrentModificationException ignored) {
      // nothing
    }
    dbSession.setDisActive(true);
    if (dbSession.getAdmin() != null) {
      dbSession.getAdmin().setActive(false);
    }
    DbAdmin.removeConnection(dbSession.getInternalId());
  }

  @Override
  public void validConnection(DbSession dbSession)
      throws WaarpDatabaseNoConnectionException {
    // try to limit the number of check!
    synchronized (dbSession) {
      if (dbSession.getConn() == null) {
        throw new WaarpDatabaseNoConnectionException(
            CANNOT_CONNECT_TO_DATABASE);
      }
      try {
        if (!dbSession.getConn().isClosed() &&
            !dbSession.getConn().isValid(DbConstant.VALIDTESTDURATION)) {
          // Give a try by closing the current connection
          throw new SQLException(CANNOT_CONNECT_TO_DATABASE);
        }
        dbSession.setDisActive(false);
        if (dbSession.getAdmin() != null) {
          dbSession.getAdmin().setActive(true);
        }
      } catch (final SQLException e2) {
        dbSession.setDisActive(true);
        if (dbSession.getAdmin() != null) {
          dbSession.getAdmin().setActive(false);
        }
        // Might be unsupported so switch to SELECT 1 way
        if (e2 instanceof PSQLException) {
          validConnectionSelect(dbSession);
          return;
        }
        if (subValidationConnection(dbSession, e2)) {
          return;
        }
        closeInternalConnection(dbSession);
        throw new WaarpDatabaseNoConnectionException(CANNOT_CONNECT_TO_DATABASE,
                                                     e2);
      }
    }
  }

  private boolean subValidationConnection(final DbSession dbSession,
                                          final SQLException e2)
      throws WaarpDatabaseNoConnectionException {
    try {
      try {
        recreateSession(dbSession);
      } catch (final WaarpDatabaseNoConnectionException e) {
        closeInternalConnection(dbSession);
        throw e;
      }
      try {
        if (!dbSession.getConn().isValid(DbConstant.VALIDTESTDURATION)) {
          // Not ignored
          closeInternalConnection(dbSession);
          throw new WaarpDatabaseNoConnectionException(
              CANNOT_CONNECT_TO_DATABASE, e2);
        }
      } catch (final SQLException e) {
        closeInternalConnection(dbSession);
        throw new WaarpDatabaseNoConnectionException(CANNOT_CONNECT_TO_DATABASE,
                                                     e);
      }
      dbSession.setDisActive(false);
      if (dbSession.getAdmin() != null) {
        dbSession.getAdmin().setActive(true);
      }
      dbSession.recreateLongTermPreparedStatements();
      return true;
    } catch (final WaarpDatabaseSqlException e1) {
      // ignore and will send a No Connection error
    }
    return false;
  }

  protected void validConnectionSelect(DbSession dbSession)
      throws WaarpDatabaseNoConnectionException {
    // try to limit the number of check!
    synchronized (dbSession) {
      Statement stmt = null;
      try {
        stmt = dbSession.getConn().createStatement();//NOSONAR
        if (stmt.execute(validConnectionString())) {
          ResultSet set = null;
          try {
            set = stmt.getResultSet();
            if (!set.next()) {
              closingStatement(stmt);
              // Give a try by closing the current connection
              throw new SQLException(CANNOT_CONNECT_TO_DATABASE);
            }
          } finally {
            if (set != null) {
              set.close();
            }
          }
        }
        dbSession.setDisActive(false);
        if (dbSession.getAdmin() != null) {
          dbSession.getAdmin().setActive(true);
        }
      } catch (final SQLException e2) {
        dbSession.setDisActive(true);
        if (dbSession.getAdmin() != null) {
          dbSession.getAdmin().setActive(false);
        }
        stmt = subValidConnectionSelect(dbSession, stmt);
        if (stmt == null) {
          return;
        }
        closeInternalConnection(dbSession);
        throw new WaarpDatabaseNoConnectionException(CANNOT_CONNECT_TO_DATABASE,
                                                     e2);
      } finally {
        closingStatement(stmt);
      }
    }
  }

  private void closingStatement(Statement stmt) {
    try {
      if (stmt != null) {
        stmt.close();
      }
    } catch (final SQLException e) {
      // ignore
    }
  }

  private Statement subValidConnectionSelect(final DbSession dbSession,
                                             Statement stmt)
      throws WaarpDatabaseNoConnectionException {
    try {
      try {
        recreateSession(dbSession);
      } catch (final WaarpDatabaseNoConnectionException e) {
        closeInternalConnection(dbSession);
        throw e;
      }
      closingStatement(stmt);
      try {
        stmt = dbSession.getConn().createStatement();//NOSONAR
      } catch (final SQLException e) {
        // Not ignored
        closeInternalConnection(dbSession);
        throw new WaarpDatabaseNoConnectionException(CANNOT_CONNECT_TO_DATABASE,
                                                     e);
      }
      try {
        if (stmt.execute(validConnectionString())) {
          ResultSet set = null;
          try {
            set = stmt.getResultSet();
            if (!set.next()) {
              closingStatement(stmt);
              closeInternalConnection(dbSession);
              throw new WaarpDatabaseNoConnectionException(
                  CANNOT_CONNECT_TO_DATABASE);
            }
          } finally {
            if (set != null) {
              set.close();
            }
          }
        }
      } catch (final SQLException e) {
        closingStatement(stmt);
        closeInternalConnection(dbSession);
        throw new WaarpDatabaseNoConnectionException(CANNOT_CONNECT_TO_DATABASE,
                                                     e);
      }
      dbSession.setDisActive(false);
      if (dbSession.getAdmin() != null) {
        dbSession.getAdmin().setActive(true);
      }
      dbSession.recreateLongTermPreparedStatements();
      closingStatement(stmt);
      return null;
    } catch (final WaarpDatabaseSqlException e1) {
      // ignore and will send a No Connection error
    }
    return stmt;
  }

  /**
   * @return the associated String to validate the connection (as "select 1
   *     from
   *     dual")
   */
  protected abstract String validConnectionString();

  @Override
  public Connection getDbConnection(String server, String user, String passwd)
      throws SQLException {
    // Default implementation
    return DriverManager.getConnection(server, user, passwd);
  }

  @Override
  public void releaseResources() {
  }

  @Override
  public int currentNumberOfPooledConnections() {
    return DbAdmin.getNbConnection();
  }

}
