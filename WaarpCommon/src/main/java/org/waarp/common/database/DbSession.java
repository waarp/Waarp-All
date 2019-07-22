/**
 * This file is part of Waarp Project.
 *
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 *
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Waarp. If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.common.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ConcurrentModificationException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.netty.util.internal.ConcurrentSet;

import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.database.model.DbModel;
import org.waarp.common.database.model.DbModelFactory;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.UUID;

// Notice, do not import com.mysql.jdbc.*
// or you will have problems!

/**
 * Class to handle session with the SGBD
 *
 * @author Frederic Bregier
 *
 */
public class DbSession {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(DbSession.class);

    /**
     * DbAdmin referent object
     */
    private DbAdmin admin = null;

    /**
     * The internal connection
     */
    private Connection conn = null;

    /**
     * Is this connection Read Only
     */
    private boolean isReadOnly = true;

    /**
     * Is this session using AutoCommit (true by default)
     */
    private boolean autoCommit = true;

    /**
     * Internal Id
     */
    private UUID internalId;

    /**
     * Number of threads using this connection
     */
    private AtomicInteger nbThread = new AtomicInteger(0);

    /**
     * To be used when a local Channel is over
     */
    private volatile boolean isDisActive = true;

    /**
     * List all DbPrepareStatement with long term usage to enable the recreation when the associated
     * connection is reopened
     */
    private final Set<DbPreparedStatement> listPreparedStatement = new ConcurrentSet<DbPreparedStatement>();

    private void initialize(DbModel dbModel, String server, String user, String passwd, boolean isReadOnly,
                            boolean autoCommit) throws WaarpDatabaseNoConnectionException {
        if (!DbModelFactory.classLoaded.contains(dbModel.getDbType().name())) {
            throw new WaarpDatabaseNoConnectionException(
                    "DbAdmin not initialzed");
        }
        if (server == null) {
            setConn(null);
            logger.error("Cannot set a null Server");
            throw new WaarpDatabaseNoConnectionException(
                    "Cannot set a null Server");
        }
        try {
            this.setAutoCommit(autoCommit);
            setConn(dbModel.getDbConnection(server, user, passwd));
            getConn().setAutoCommit(this.isAutoCommit());
            this.setReadOnly(isReadOnly);
            getConn().setReadOnly(this.isReadOnly());
            setInternalId(new UUID());
            logger.debug("Open Db Conn: " + getInternalId());
            DbAdmin.addConnection(getInternalId(), this);
            setDisActive(false);
            checkConnection();
        } catch (SQLException ex) {
            setDisActive(true);
            // handle any errors
            logger.error("Cannot create Connection");
            error(ex);
            if (getConn() != null) {
                try {
                    getConn().close();
                } catch (SQLException e) {
                }
            }
            setConn(null);
            throw new WaarpDatabaseNoConnectionException(
                    "Cannot create Connection", ex);
        }
    }

    /**
     * Create a session and connect the current object to the server using the DbAdmin object. The
     * database access use auto commit.
     *
     * If the initialize is not call before, call it with the default value.
     *
     * @param admin
     * @param isReadOnly
     * @throws WaarpDatabaseSqlException
     */
    public DbSession(DbAdmin admin, boolean isReadOnly)
            throws WaarpDatabaseNoConnectionException {
        try {
            this.setAdmin(admin);
            initialize(admin.getDbModel(), admin.getServer(), admin.getUser(), admin.getPasswd(), isReadOnly, true);
        } catch (NullPointerException ex) {
            // handle any errors
            setDisActive(true);
            logger.error("Cannot create Connection:" + (admin == null), ex);
            if (getConn() != null) {
                try {
                    getConn().close();
                } catch (SQLException e) {
                }
            }
            setConn(null);
            throw new WaarpDatabaseNoConnectionException(
                    "Cannot create Connection", ex);
        }
    }

    /**
     * Create a session and connect the current object to the server using the DbAdmin object.
     *
     * If the initialize is not call before, call it with the default value.
     *
     * @param admin
     * @param isReadOnly
     * @param autoCommit
     * @throws WaarpDatabaseSqlException
     */
    public DbSession(DbAdmin admin, boolean isReadOnly, boolean autoCommit)
            throws WaarpDatabaseNoConnectionException {
        try {
            this.setAdmin(admin);
            initialize(admin.getDbModel(), admin.getServer(), admin.getUser(), admin.getPasswd(), isReadOnly, autoCommit);
        } catch (NullPointerException ex) {
            // handle any errors
            logger.error("Cannot create Connection:" + (admin == null), ex);
            setDisActive(true);
            if (getConn() != null) {
                try {
                    getConn().close();
                } catch (SQLException e) {
                }
            }
            setConn(null);
            throw new WaarpDatabaseNoConnectionException(
                    "Cannot create Connection", ex);
        }
    }

    /**
     * Change the autocommit feature
     *
     * @param autoCommit
     * @throws WaarpDatabaseNoConnectionException
     */
    public void setAutoCommit(boolean autoCommit)
            throws WaarpDatabaseNoConnectionException {
        if (getConn() != null) {
            this.autoCommit = autoCommit;
            try {
                getConn().setAutoCommit(autoCommit);
            } catch (SQLException e) {
                // handle any errors
                logger.error("Cannot create Connection");
                error(e);
                if (getConn() != null) {
                    try {
                        getConn().close();
                    } catch (SQLException e1) {
                    }
                }
                setConn(null);
                setDisActive(true);
                throw new WaarpDatabaseNoConnectionException(
                        "Cannot create Connection", e);
            }
        }
    }

    /**
     * @return the admin
     */
    public DbAdmin getAdmin() {
        return admin;
    }

    /**
     * @param admin
     *            the admin to set
     */
    protected void setAdmin(DbAdmin admin) {
        this.admin = admin;
    }

    /**
     * Print the error from SQLException
     *
     * @param ex
     */
    public static void error(SQLException ex) {
        // handle any errors
        logger.error("SQLException: " + ex.getMessage() + " SQLState: " +
                ex.getSQLState() + "VendorError: " + ex.getErrorCode());
    }

    /**
     * To be called when a client will start to use this DbSession (once by client)
     */
    public void useConnection() {
        int val = nbThread.incrementAndGet();
        synchronized (this) {
            if (isDisActive()) {
                try {
                    initialize(getAdmin().getDbModel(), getAdmin().getServer(), getAdmin().getUser(), getAdmin().getPasswd(), isReadOnly(), isAutoCommit());
                } catch (WaarpDatabaseNoConnectionException e) {
                    logger.error("ThreadUsing: " + nbThread + " but not connected");
                    return;
                }
            }
        }
        logger.debug("ThreadUsing: " + val);
    }

    /**
     * To be called when a client will stop to use this DbSession (once by client)
     */
    public void endUseConnection() {
        int val = nbThread.decrementAndGet();
        logger.debug("ThreadUsing: " + val);
        if (val <= 0) {
            disconnect();
        }
    }

    /**
     * To be called when a client will stop to use this DbSession (once by client).
     * This version is not blocking.
     */
    public void enUseConnectionNoDisconnect() {
        int val = nbThread.decrementAndGet();
        logger.debug("ThreadUsing: " + val);
        if (val <= 0) {
            DbAdmin.dbSessionTimer.newTimeout(new TryDisconnectDbSession(this), DbAdmin.WAITFORNETOP * 10,
                    TimeUnit.MILLISECONDS);
        }
    }

    /**
     * To disconnect in asynchronous way the DbSession
     *
     * @author "Frederic Bregier"
     *
     */
    private static class TryDisconnectDbSession implements TimerTask {
        private final DbSession dbSession;

        private TryDisconnectDbSession(DbSession dbSession) {
            this.dbSession = dbSession;
        }

        public void run(Timeout timeout) throws Exception {
            int val = dbSession.nbThread.get();
            if (val <= 0) {
                dbSession.disconnect();
            }
            logger.debug("ThreadUsing: " + val);
        }
    }

    @Override
    public int hashCode() {
        return this.getInternalId().hashCode();

    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DbSession))
            return false;
        return (this == o) || this.getInternalId().equals(((DbSession) o).getInternalId());
    }

    /**
     * Force the close of the connection
     */
    public void forceDisconnect() {
        if (this.getInternalId().equals(getAdmin().getSession().getInternalId())) {
            logger.debug("Closing internal db connection");
        }
        this.nbThread.set(0);
        if (getConn() == null) {
            logger.debug("Connection already closed");
            return;
        }
        try {
            Thread.sleep(DbAdmin.WAITFORNETOP);
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
        }
        logger.debug("DbConnection still in use: " + nbThread);
        removeLongTermPreparedStatements();
        DbAdmin.removeConnection(getInternalId());
        setDisActive(true);
        try {
            logger.debug("Fore close Db Conn: " + getInternalId());
            if (getConn() != null) {
                getConn().close();
                setConn(null);
            }
        } catch (SQLException e) {
            logger.warn("Disconnection not OK");
            error(e);
        } catch (ConcurrentModificationException e) {
            // ignore
        }
        logger.info("Current cached connection: "
                + getAdmin().getDbModel().currentNumberOfPooledConnections());
    }

    /**
     * Close the connection
     *
     */
    public void disconnect() {
        if (this.getInternalId().equals(getAdmin().getSession().getInternalId())) {
            logger.debug("Closing internal db connection: " + nbThread.get());
        }
        if (getConn() == null || isDisActive()) {
            logger.debug("Connection already closed");
            return;
        }
        try {
            Thread.sleep(DbAdmin.WAITFORNETOP);
        } catch (InterruptedException e1) {
            Thread.currentThread().interrupt();
        }
        logger.debug("DbConnection still in use: " + nbThread);
        if (nbThread.get() > 0) {
            logger.info("Still some clients could use this Database Session: " +
                    nbThread);
            return;
        }
        synchronized (this) {
            removeLongTermPreparedStatements();
            DbAdmin.removeConnection(getInternalId());
            setDisActive(true);
            try {
                logger.debug("Close Db Conn: " + getInternalId());
                if (getConn() != null) {
                    getConn().close();
                    setConn(null);
                }
            } catch (SQLException e) {
                logger.warn("Disconnection not OK");
                error(e);
            } catch (ConcurrentModificationException e) {
                // ignore
            }
        }
        logger.info("Current cached connection: "
                + getAdmin().getDbModel().currentNumberOfPooledConnections());
    }

    /**
     * Check the connection to the Database and try to reopen it if possible
     *
     * @throws WaarpDatabaseNoConnectionException
     */
    public void checkConnection() throws WaarpDatabaseNoConnectionException {
        try {
            getAdmin().getDbModel().validConnection(this);
            setDisActive(false);
            if (getAdmin() != null)
                getAdmin().setActive(true);
        } catch (WaarpDatabaseNoConnectionException e) {
            setDisActive(true);
            if (getAdmin() != null)
                getAdmin().setActive(false);
            throw e;
        }
    }

    /**
     *
     * @return True if the connection was successfully reconnected
     */
    public boolean checkConnectionNoException() {
        try {
            checkConnection();
            return true;
        } catch (WaarpDatabaseNoConnectionException e) {
            return false;
        }
    }

    /**
     * Add a Long Term PreparedStatement
     *
     * @param longterm
     */
    public void addLongTermPreparedStatement(DbPreparedStatement longterm) {
        this.listPreparedStatement.add(longterm);
    }

    /**
     * Due to a reconnection, recreate all associated long term PreparedStatements
     *
     * @throws WaarpDatabaseNoConnectionException
     * @throws WaarpDatabaseSqlException
     */
    public void recreateLongTermPreparedStatements()
            throws WaarpDatabaseNoConnectionException,
            WaarpDatabaseSqlException {
        WaarpDatabaseNoConnectionException elast = null;
        WaarpDatabaseSqlException e2last = null;
        logger.info("RecreateLongTermPreparedStatements: " + listPreparedStatement.size());
        for (DbPreparedStatement longterm : listPreparedStatement) {
            try {
                longterm.recreatePreparedStatement();
            } catch (WaarpDatabaseNoConnectionException e) {
                logger.warn(
                        "Error while recreation of Long Term PreparedStatement",
                        e);
                elast = e;
            } catch (WaarpDatabaseSqlException e) {
                logger.warn(
                        "Error while recreation of Long Term PreparedStatement",
                        e);
                e2last = e;
            }
        }
        if (elast != null) {
            throw elast;
        }
        if (e2last != null) {
            throw e2last;
        }
    }

    /**
     * Remove all Long Term PreparedStatements (closing connection)
     */
    public void removeLongTermPreparedStatements() {
        for (DbPreparedStatement longterm : listPreparedStatement) {
            if (longterm != null) {
                longterm.realClose();
            }
        }
        listPreparedStatement.clear();
    }

    /**
     * Remove one Long Term PreparedStatement
     *
     * @param longterm
     */
    public void removeLongTermPreparedStatements(DbPreparedStatement longterm) {
        listPreparedStatement.remove(longterm);
    }

    /**
     * Commit everything
     *
     * @throws WaarpDatabaseSqlException
     * @throws WaarpDatabaseNoConnectionException
     */
    public void commit() throws WaarpDatabaseSqlException,
            WaarpDatabaseNoConnectionException {
        if (getConn() == null) {
            logger.warn("Cannot commit since connection is null");
            throw new WaarpDatabaseNoConnectionException(
                    "Cannot commit since connection is null");
        }
        if (this.isAutoCommit()) {
            return;
        }
        if (isDisActive()) {
            checkConnection();
        }
        try {
            getConn().commit();
        } catch (SQLException e) {
            logger.error("Cannot Commit");
            error(e);
            throw new WaarpDatabaseSqlException("Cannot commit", e);
        }
    }

    /**
     * Rollback from the savepoint or the last set if null
     *
     * @param savepoint
     * @throws WaarpDatabaseNoConnectionException
     * @throws WaarpDatabaseSqlException
     */
    public void rollback(Savepoint savepoint)
            throws WaarpDatabaseNoConnectionException,
            WaarpDatabaseSqlException {
        if (getConn() == null) {
            logger.warn("Cannot rollback since connection is null");
            throw new WaarpDatabaseNoConnectionException(
                    "Cannot rollback since connection is null");
        }
        if (isDisActive()) {
            checkConnection();
        }
        try {
            if (savepoint == null) {
                getConn().rollback();
            } else {
                getConn().rollback(savepoint);
            }
        } catch (SQLException e) {
            logger.error("Cannot rollback");
            error(e);
            throw new WaarpDatabaseSqlException("Cannot rollback", e);
        }
    }

    /**
     * Make a savepoint
     *
     * @return the new savepoint
     * @throws WaarpDatabaseNoConnectionException
     * @throws WaarpDatabaseSqlException
     */
    public Savepoint savepoint() throws WaarpDatabaseNoConnectionException,
            WaarpDatabaseSqlException {
        if (getConn() == null) {
            logger.warn("Cannot savepoint since connection is null");
            throw new WaarpDatabaseNoConnectionException(
                    "Cannot savepoint since connection is null");
        }
        if (isDisActive()) {
            checkConnection();
        }
        try {
            return getConn().setSavepoint();
        } catch (SQLException e) {
            logger.error("Cannot savepoint");
            error(e);
            throw new WaarpDatabaseSqlException("Cannot savepoint", e);
        }
    }

    /**
     * Release the savepoint
     *
     * @param savepoint
     * @throws WaarpDatabaseNoConnectionException
     * @throws WaarpDatabaseSqlException
     */
    public void releaseSavepoint(Savepoint savepoint)
            throws WaarpDatabaseNoConnectionException,
            WaarpDatabaseSqlException {
        if (getConn() == null) {
            logger.warn("Cannot release savepoint since connection is null");
            throw new WaarpDatabaseNoConnectionException(
                    "Cannot release savepoint since connection is null");
        }
        if (isDisActive()) {
            checkConnection();
        }
        try {
            getConn().releaseSavepoint(savepoint);
        } catch (SQLException e) {
            logger.error("Cannot release savepoint");
            error(e);
            throw new WaarpDatabaseSqlException("Cannot release savepoint", e);
        }
    }

    /**
     * @return the isReadOnly
     */
    public boolean isReadOnly() {
        return isReadOnly;
    }

    /**
     * @param isReadOnly the isReadOnly to set
     */
    public void setReadOnly(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    /**
     * @return the autoCommit
     */
    public boolean isAutoCommit() {
        return autoCommit;
    }

    /**
     * @return the conn
     */
    public Connection getConn() {
        return conn;
    }

    /**
     * @param conn the conn to set
     */
    public void setConn(Connection conn) {
        this.conn = conn;
    }

    /**
     * @return the internalId
     */
    public UUID getInternalId() {
        return internalId;
    }

    /**
     * @param internalId the internalId to set
     */
    private void setInternalId(UUID internalId) {
        this.internalId = internalId;
    }

    /**
     * @return the isDisActive
     */
    public boolean isDisActive() {
        return isDisActive;
    }

    /**
     * @param isDisActive the isDisActive to set
     */
    public void setDisActive(boolean isDisActive) {
        this.isDisActive = isDisActive;
    }
}
