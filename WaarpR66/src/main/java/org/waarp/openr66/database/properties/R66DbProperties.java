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

package org.waarp.openr66.database.properties;

import org.waarp.common.database.properties.DbProperties;
import org.waarp.common.database.properties.H2Properties;
import org.waarp.common.database.properties.MariaDBProperties;
import org.waarp.common.database.properties.MySQLProperties;
import org.waarp.common.database.properties.OracleProperties;
import org.waarp.common.database.properties.PostgreSQLProperties;

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
