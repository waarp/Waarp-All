package org.waarp.openr66.database.properties;

import org.waarp.common.database.properties.*;

public abstract class R66DbProperties {

    public abstract String getCreateQuery();

    public static R66DbProperties getInstance(DbProperties prop) {
        if (prop instanceof H2Properties) {
            return new R66H2Properties();
        } else if (prop instanceof MariaDBProperties) {
            return new R66MariaDBProperties();
        } else if (prop instanceof MySQLProperties) {
            return new R66MySQLProperties();
        } else if (prop instanceof PostgreSQLProperties) {
            return new R66PostgreSQLProperties();
        } else if (prop instanceof OracleProperties) {
            return new R66OracleProperties();
        } else {
            return null;
        }
    }
}
