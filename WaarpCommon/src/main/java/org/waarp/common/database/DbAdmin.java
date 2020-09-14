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

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.database.model.DbModel;
import org.waarp.common.database.model.DbModelFactory;
import org.waarp.common.database.model.DbType;
import org.waarp.common.database.model.EmptyDbModel;
import org.waarp.common.guid.GUID;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpThreadFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Class for access to Database
 *
 * Partially deprecated (only in Waarp R66 but not other components)
 */
public class DbAdmin {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbAdmin.class);

  public static final int RETRYNB = 3;

  public static final long WAITFORNETOP = 100;

  /**
   * Database type
   */
  protected final DbType typeDriver;

  /**
   * DbModel
   */
  private final DbModel dbModel;

  /**
   * DB Server
   */
  private final String server;

  /**
   * DB User
   */
  private final String user;

  /**
   * DB Password
   */
  private final String passwd;

  /**
   * Is this DB Admin Read Only
   */
  private boolean isReadOnly;

  /**
   * session is the Session object for all type of requests
   */
  private DbSession session;
  /**
   * Number of HttpSession
   */
  private static int nbHttpSession;

  protected static final Timer dbSessionTimer =
      new HashedWheelTimer(new WaarpThreadFactory("TimerClose"), 50,
                           TimeUnit.MILLISECONDS, 1024);

  /**
   * @return the session
   */
  public DbSession getSession() {
    return session;
  }

  /**
   * @param session the session to set
   */
  public void setSession(final DbSession session) {
    this.session = session;
  }

  /**
   * @return True if the connection is ReadOnly
   */
  public boolean isReadOnly() {
    return isReadOnly;
  }

  /**
   * Validate connection
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public void validConnection() throws WaarpDatabaseNoConnectionException {
    try {
      dbModel.validConnection(getSession());
    } catch (final WaarpDatabaseNoConnectionException e) {
      getSession().setDisActive(true);
      throw e;
    }
    getSession().setDisActive(false);
  }

  /**
   * Use a default server for basic connection. Later on, specific connection
   * to
   * database for the scheme that
   * provides access to the table R66DbIndex for one specific Legacy could be
   * done.
   * <p>
   * A this time, only one driver is possible! If a new driver is needed,
   * then
   * we need to create a new DbSession
   * object. Be aware that DbSession.initialize should be call only once for
   * each driver, whatever the number of
   * DbSession objects that could be created (=> need a hashtable for
   * specific
   * driver when created). Also, don't
   * know if two drivers at the same time (two different DbSession) is
   * allowed
   * by JDBC.
   *
   * @param model
   * @param server
   * @param user
   * @param passwd
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public DbAdmin(final DbModel model, final String server, final String user,
                 final String passwd)
      throws WaarpDatabaseNoConnectionException {
    this.server = server;
    this.user = user;
    this.passwd = passwd;
    dbModel = model;
    typeDriver = model.getDbType();
    if (typeDriver == null) {
      logger.error("Cannot find TypeDriver");
      throw new WaarpDatabaseNoConnectionException(
          "Cannot find database drive");
    }
    setSession(new DbSession(this, false));
    getSession().setAdmin(this);
    isReadOnly = false;
    validConnection();
    getSession().useConnection(); // default since this is the top connection
  }

  /**
   * Use a default server for basic connection. Later on, specific connection
   * to
   * database for the scheme that
   * provides access to the table R66DbIndex for one specific Legacy could be
   * done.
   * <p>
   * A this time, only one driver is possible! If a new driver is needed,
   * then
   * we need to create a new DbSession
   * object. Be aware that DbSession.initialize should be call only once for
   * each driver, whatever the number of
   * DbSession objects that could be created (=> need a hashtable for
   * specific
   * driver when created). Also, don't
   * know if two drivers at the same time (two different DbSession) is
   * allowed
   * by JDBC.
   *
   * @param model
   * @param server
   * @param user
   * @param passwd
   * @param write
   *
   * @throws WaarpDatabaseSqlException
   * @throws WaarpDatabaseNoConnectionException
   */
  public DbAdmin(final DbModel model, final String server, final String user,
                 final String passwd, final boolean write)
      throws WaarpDatabaseNoConnectionException {
    this.server = server;
    this.user = user;
    this.passwd = passwd;
    dbModel = model;
    typeDriver = model.getDbType();
    if (typeDriver == null) {
      logger.error("Cannot find TypeDriver");
      throw new WaarpDatabaseNoConnectionException(
          "Cannot find database driver");
    }
    if (write) {
      for (int i = 0; i < RETRYNB; i++) {
        try {
          setSession(new DbSession(this, false));
        } catch (final WaarpDatabaseNoConnectionException e) {
          logger.warn("Attempt of connection in error: " + i, e);
          continue;
        }
        isReadOnly = false;
        getSession().setAdmin(this);
        validConnection();
        getSession().useConnection(); // default since this is the top
        // connection
        return;
      }
    } else {
      for (int i = 0; i < RETRYNB; i++) {
        try {
          setSession(new DbSession(this, true));
        } catch (final WaarpDatabaseNoConnectionException e) {
          logger.warn("Attempt of connection in error: " + i, e);
          continue;
        }
        isReadOnly = true;
        getSession().setAdmin(this);
        validConnection();
        getSession().useConnection(); // default since this is the top
        // connection
        return;
      }
    }
    setSession(null);
    logger.error("Cannot connect to Database!");
    throw new WaarpDatabaseNoConnectionException("Cannot connect to database");
  }

  /**
   * Empty constructor for no Database support (very thin client)
   */
  public DbAdmin() {
    // not true but to enable pseudo database functions
    typeDriver = DbType.none;
    DbModelFactory.classLoaded.add(DbType.none.name());
    dbModel = new EmptyDbModel();
    server = null;
    user = null;
    passwd = null;
  }

  /**
   * Close the underlying session. Can be call even for connection given from
   * the constructor
   * DbAdmin(Connection, boolean).
   */
  public void close() {
    if (getSession() != null) {
      getSession().endUseConnection(); // default since this is the top
      // connection
      getSession().forceDisconnect();
      setSession(null);
    }
  }

  /**
   * Commit on connection (since in autocommit, should not be used)
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public void commit()
      throws WaarpDatabaseSqlException, WaarpDatabaseNoConnectionException {
    if (getSession() != null) {
      getSession().commit();
    }
  }

  /**
   * @return the server
   */
  public String getServer() {
    return server;
  }

  /**
   * @return the user
   */
  public String getUser() {
    return user;
  }

  /**
   * @return the passwd
   */
  public String getPasswd() {
    return passwd;
  }

  /**
   * @return the associated dbModel
   */
  public DbModel getDbModel() {
    return dbModel;
  }

  /**
   * @return the typeDriver
   */
  public DbType getTypeDriver() {
    return typeDriver;
  }

  @Override
  public String toString() {
    return "Admin: " + typeDriver.name() + ':' + server + ':' + user + ':' +
           passwd.length();
  }

  /**
   * List all Connection to enable the close call on them
   */
  private static final ConcurrentHashMap<GUID, DbSession> listConnection =
      new ConcurrentHashMap<org.waarp.common.guid.GUID, DbSession>();

  /**
   * Increment nb of Http Connection
   */
  public static void incHttpSession() {
    nbHttpSession++;
  }

  /**
   * Decrement nb of Http Connection
   */
  public static void decHttpSession() {
    nbHttpSession--;
  }

  /**
   * @return the nb of Http Connection
   */
  public static int getHttpSession() {
    return nbHttpSession;
  }

  /**
   * Add a Connection into the list
   *
   * @param id
   * @param session
   */
  public static void addConnection(final GUID id, final DbSession session) {
    listConnection.put(id, session);
  }

  /**
   * Remove a Connection from the list
   *
   * @param id Id of the connection
   */
  public static void removeConnection(final GUID id) {
    listConnection.remove(id);
  }

  /**
   * @return the number of connection (so number of network channels)
   */
  public static int getNbConnection() {
    return listConnection.size() - 1;
  }

  /**
   * Close all database connections
   */
  public static void closeAllConnection() {
    for (final DbSession session : listConnection.values()) {
      logger.debug("Close (all) Db Conn: {}", session.getInternalId());
      try {
        final Connection connection = session.getConn();
        if (connection != null) {
          connection.close();
        }
      } catch (final SQLException ignored) {
        // nothing
      } catch (final ConcurrentModificationException ignored) {
        // nothing
      }
    }
    listConnection.clear();
    for (final DbModel dbModel : DbModelFactory.dbModels) {
      if (dbModel != null) {
        dbModel.releaseResources();
      }
    }
    dbSessionTimer.stop();
  }

  /**
   * Check all database connections and try to reopen them if disActive
   */
  public static void checkAllConnections() {
    for (final DbSession session : listConnection.values()) {
      try {
        session.checkConnection();
      } catch (final WaarpDatabaseNoConnectionException e) {
        logger.error("Database Connection cannot be reinitialized");
      }
    }
  }

  /**
   * @return True if this driver allows Thread Shared Connexion (concurrency
   *     usage)
   */
  public boolean isCompatibleWithThreadSharedConnexion() {
    return typeDriver != DbType.MariaDB && typeDriver != DbType.MySQL &&
           typeDriver != DbType.Oracle && typeDriver != DbType.none;
  }
}
