package org.waarp.common.database.properties;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * PostgreSQL Database Model
 */
public class PostgreSQLProperties extends DbProperties {
    public static final String PROTOCOL = "postgres";

    private final String DRIVER_NAME = "org.postgresql.Driver";
    private final String VALIDATION_QUERY = "select 1";
    private final String MAX_CONNECTION_QUERY = "SHOW max_connections";

    public PostgreSQLProperties() {
    }

    public static String getProtocolID() {
        return PROTOCOL;
    }

    @Override
    public String getDriverName() {
        return DRIVER_NAME;
    }

    @Override
    public String getValidationQuery() {
        return VALIDATION_QUERY;
    }

    @Override
    public int getMaximumConnections(Connection connection)
            throws SQLException {
        Statement stm = null;
        ResultSet rs = null;
        try {
            stm = connection.createStatement();
            rs = stm.executeQuery(MAX_CONNECTION_QUERY);
            if (!rs.next()) {
                throw new SQLException("Cannot find max connection");
            }
            return rs.getInt(1);
        } finally {
            if (rs != null) {
                rs.close();
            }
            if (stm != null) {
                stm.close();
            }
        }
    }
}
