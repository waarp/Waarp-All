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

/**
 * H2 Database Model
 */
public class H2Properties implements DbProperties {
  private static final String PROTOCOL = "h2";

  private static final String DRIVER_NAME = "org.h2.Driver";
  private static final String VALIDATION_QUERY = "select 1";

  public H2Properties() {
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
  public int getMaximumConnections(Connection connection) {
    // According to H2 website:
    // There is no limit on the number of database open concurrently per
    // server, or on the number of open connections.
    return 1000;
  }
}
