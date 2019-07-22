package org.waarp.common.database;

import org.apache.commons.dbcp.BasicDataSource;
import org.waarp.common.database.properties.*;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * A singleton wrapper of Datasource to get database connection object
 */
public class ConnectionFactory {

    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
        .getLogger(ConnectionFactory.class);

    /**
     * The singleton instance
     */
    private static ConnectionFactory instance;

    /**
     * DbModel
     */
    private final DbProperties properties;

    /**
     * The connection url to be passed to the JDBC driver to establish a connection.
     */
    private final String server;

    /**
     * The connection username to be passed to the JDBC driver to establish a connection.
     */
    private final String user;

    /**
     * The connection password to be passed to the JDBC driver to establish a connection.
     */
    private final String password;

    private final boolean autocommit = true;

    private final boolean readonly = false;

    private int maxconnections = 50;

    /**
     * The datasource for connection pooling
     */
    private BasicDataSource ds;

    /**
     * @param server the connection url of the database
     * @return the DbProperties Object associated with the requested URL
     * @throws UnsupportedOperationException if the requested database
     * is not supported
     */
    protected static DbProperties propertiesFor(String server)
            throws UnsupportedOperationException {
        if(server.contains(H2Properties.getProtocolID())) {
            return new H2Properties();
        } else if(server.contains(MariaDBProperties.getProtocolID())) {
            return new MariaDBProperties();
        } else if(server.contains(MySQLProperties.getProtocolID())) {
            return new MySQLProperties();
        } else if(server.contains(OracleProperties.getProtocolID())) {
            return new OracleProperties();
        } else if(server.contains(PostgreSQLProperties.getProtocolID())) {
            return new PostgreSQLProperties();
        } else {
            throw new UnsupportedOperationException(
                "The requested database is not supported");
        }
    }

    /**
     * Initialize the ConnectionFactory
     * @throws UnsupportedOperationException if the requested database
     * is not supported
     * @throws SQLException if a error occurs while connecting to the database
     */
    public static void initialize(String server, String user, String password)
            throws SQLException, UnsupportedOperationException {
        if (instance == null) {
             instance = new ConnectionFactory(propertiesFor(server), server,
                 user, password);
        }
    }

    /**
     * @return the initialized ConnectionFactory or
     * null if the ConnectionFactory is not initialized
     */
    public static ConnectionFactory getInstance() {
        return instance;
    }

    /**
     * @return a connection to the Database
     * @throws SQLException if the ConnectionPool is not initialized
     * or if an error occurs while accessing the database
     */
    public Connection getConnection() throws SQLException {
        logger.trace("Active: {}, Idle: {}", ds.getNumActive(), ds.getNumIdle());
        if (ds == null) {
            throw new SQLException("ConnectionFactory is not inialized.");
        }
        try {
            Connection con = ds.getConnection();
            return con;
        } catch (SQLException e) {
            throw new SQLException("Cannot access database", e);
        }
    }

    /**
     * @param model
     * @param server
     * @param user
     * @param password
     */
    private ConnectionFactory(DbProperties properties, String server,
                          String user, String password) throws SQLException {
        this.server = server;
        this.user = user;
        this.password = password;
        this.properties = properties;

        ds = new BasicDataSource();

        // Initialise DataSource
        ds.setDriverClassName(this.properties.getDriverName());
        ds.setUrl(this.server);
        ds.setUsername(this.user);
        ds.setPassword(this.password);
        ds.setDefaultAutoCommit(autocommit);
        ds.setDefaultReadOnly(readonly);
        ds.setValidationQuery(this.properties.getValidationQuery());

        // Get maximum connections
        Connection con = null;
        try {
            con = getConnection();
            maxconnections = properties.getMaximumConnections(con);
        } catch (SQLException e) {
            logger.warn(
                    "Cannot fetch maximum connection allowed from database", e);
        } finally {
            con.close();
        }
        ds.setMaxActive(maxconnections);
        ds.setMaxIdle(maxconnections);
        logger.info(this.toString());
    }

    /**
     * Closes and releases all registered connections and connection pool
     */
    public void close() {
        logger.info("Closing ConnectionFactory");
        try {
            ds.close();
        } catch (SQLException e) {
            logger.debug("Cannot close properly the connection pool", e);
        }
    }

    /**
     * @return the JDBC connection url property
     */
    public String getServer() {
        return server;
    }

    /**
     * @return the JDBC connection username property
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the JDBC connection password property
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the associated DbProperties
     */
    public DbProperties getProperties() {
        return properties;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Datapool:");
        sb.append(this.server);
        sb.append(", with user:");
        sb.append(this.user);
        sb.append(", AutoCommit:");
        sb.append(this.autocommit);
        sb.append(", DefaultReadOnly:");
        sb.append(this.readonly);
        sb.append(", max connecions:");
        sb.append(this.maxconnections);

        return sb.toString();
    }
}
