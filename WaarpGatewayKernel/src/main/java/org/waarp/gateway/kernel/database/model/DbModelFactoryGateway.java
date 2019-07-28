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
package org.waarp.gateway.kernel.database.model;

import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.model.DbModel;
import org.waarp.common.database.model.DbModelFactory;
import org.waarp.common.database.model.DbType;

/**
 * Factory to store the Database Model object
 */
public class DbModelFactoryGateway extends DbModelFactory {

  /**
   * Initialize the Database Model according to arguments.
   *
   * @param dbdriver
   * @param dbserver
   * @param dbuser
   * @param dbpasswd
   * @param write
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public static DbAdmin initialize(String dbdriver, String dbserver,
                                   String dbuser, String dbpasswd,
                                   boolean write)
      throws WaarpDatabaseNoConnectionException {
    final DbType type = DbType.getFromDriver(dbdriver);
    DbModel dbModel;
    switch (type) {
      case H2:
        dbModel = new DbModelH2(dbserver, dbuser, dbpasswd);
        break;
      case Oracle:
        dbModel = new DbModelOracle(dbserver, dbuser, dbpasswd);
        break;
      case PostGreSQL:
        dbModel = new DbModelPostgresql();
        break;
      case MySQL:
        dbModel = new DbModelMysql(dbserver, dbuser, dbpasswd);
        break;
      default:
        throw new WaarpDatabaseNoConnectionException(
            "TypeDriver unknown: " + type);
    }
    return new DbAdmin(dbModel, dbserver, dbuser, dbpasswd, write);
  }
}
