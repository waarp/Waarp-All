package org.waarp.common.database.properties;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class DbProperties {

    /**
     * @return the driver class name associated with the DbModel
     */
    abstract public String getDriverName();

    /**
     * @return the validation query associated with the DbModel
     */
    abstract public String getValidationQuery();

    /**
     * @param connection a database connection
     *
     * @return the number of connections allowed by the database
     */
    abstract public int getMaximumConnections(Connection connection)
            throws SQLException;
}
