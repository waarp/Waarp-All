package org.waarp.common.database.properties;

import java.sql.Connection;

/**
 * H2 Database Model
 */
public class H2Properties extends DbProperties {
    private static final String PROTOCOL = "h2";

    private final String DRIVER_NAME = "org.h2.Driver";
    private final String VALIDATION_QUERY = "select 1";

    public H2Properties() {
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
    public int getMaximumConnections(Connection connection) {
        //TODO
        return 10;
    }
}
