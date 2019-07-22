package org.waarp.common.database.properties;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MariaDB Database Model
 */
public class MariaDBProperties extends DbProperties {
    private static final String PROTOCOL = "mariadb";

    private final String DRIVER_NAME = "org.mariadb.jdbc.Driver";
    private final String VALIDATION_QUERY = "select 1";
    private final String MAX_CONNECTION_QUERY = "select GLOBAL_VALUE " +
            "from INFORMATION_SCHEMA.SYSTEM_VARIABLES " +
            "where VARIABLE_NAME LIKE 'max_connections'";

    public MariaDBProperties() {
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
