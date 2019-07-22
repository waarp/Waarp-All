package org.waarp.common.database.properties;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Oracle Database Model
 */
public class OracleProperties extends DbProperties {
    public static final String PROTOCOL = "h2";

    private final String DRIVER_NAME = "oracle.jdbc.OracleDriver";
    private final String VALIDATION_QUERY = "select 1 from dual";
    private final String MAX_CONNECTION_QUERY = "select limit_value " +
            "from v$resource_limit " +
            "where resource_name='sessions'";

    public OracleProperties() {
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
