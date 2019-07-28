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

package org.waarp.common.database.properties;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MySQL Database Model
 */
public class MySQLProperties implements DbProperties {
  public static final String PROTOCOL = "mysql";

  private static final String DRIVER_NAME = "com.mysql.jdbc.Driver";
  private static final String VALIDATION_QUERY = "select 1";
  private static final String MAX_CONNECTION_QUERY =
      "select GLOBAL_VALUE " + "from INFORMATION_SCHEMA.SYSTEM_VARIABLES " +
      "where VARIABLE_NAME LIKE 'max_connections'";

  public MySQLProperties() {
    // nothing
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
  public int getMaximumConnections(Connection connection) throws SQLException {
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
        try {
          rs.close();
        } catch (SQLException ignored) {
          // nothing
        }
      }
      if (stm != null) {
        try {
          stm.close();
        } catch (SQLException ignored) {
          // nothing
        }
      }
    }
  }
}
