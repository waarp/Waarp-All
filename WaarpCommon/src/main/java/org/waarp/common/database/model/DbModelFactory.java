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
package org.waarp.common.database.model;

import org.waarp.common.database.DbAdmin;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Factory to store the Database Model object
 *
 *
 */
public class DbModelFactory {

  /**
   * Info on JDBC Class is already loaded or not
   */
  public static final Set<String> classLoaded = new HashSet<String>();
  /**
   * Database Model Object list
   */
  public static final List<DbModel> dbModels = new ArrayList<DbModel>();

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
  @SuppressWarnings("unused")
  public static DbAdmin initialize(String dbdriver, String dbserver,
                                   String dbuser, String dbpasswd,
                                   boolean write)
      throws WaarpDatabaseNoConnectionException {
    final DbType type = DbType.getFromDriver(dbdriver);
    final DbModel dbModel = null;
    switch (type) {
      case H2:
        // dbModel = new DbModelH2(dbserver, dbuser, dbpasswd);
        break;
      case Oracle:
        // dbModel = new DbModelOracle(dbserver, dbuser, dbpasswd);
        break;
      case PostGreSQL:
        // dbModel = new DbModelPostgresql();
        break;
      case MySQL:
        // dbModel = new DbModelMysql(dbserver, dbuser, dbpasswd);
        break;
      case MariaDB:
        // dbModel = new DbModelMariadb(dbserver, dbuser, dbpasswd);
        break;
      default:
        throw new WaarpDatabaseNoConnectionException(
            "TypeDriver unknown: " + type);
    }
    if (dbModel == null) {
      throw new WaarpDatabaseNoConnectionException(
          "TypeDriver not allocated: " + type);
    }
    dbModels.add(dbModel);
    return new DbAdmin(dbModel, dbserver, dbuser, dbpasswd, write);
  }
}
