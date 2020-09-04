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
package org.waarp.openr66.database.model;

import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.model.DbModelMariadb;

/**
 * MariaDB Database Model implementation
 */
public class DbModelMariadbR66 extends DbModelMariadb {
  /**
   * Create the object and initialize if necessary the driver
   *
   * @param dbserver
   * @param dbuser
   * @param dbpasswd
   *
   * @throws WaarpDatabaseNoConnectionException
   */
  public DbModelMariadbR66(final String dbserver, final String dbuser,
                           final String dbpasswd)
      throws WaarpDatabaseNoConnectionException {
    super(dbserver, dbuser, dbpasswd);
  }

  @Override
  public void createTables(final DbSession session)
      throws WaarpDatabaseNoConnectionException {
    DbModelFactoryR66.createTableMariaDbMySQL(dbTypeResolver, session);
  }

  @Override
  public boolean upgradeDb(final DbSession session, final String version)
      throws WaarpDatabaseNoConnectionException {
    return DbModelFactoryR66
        .upgradeDbMariaDbMySQL(dbTypeResolver, session, version);
  }

  @Override
  public boolean needUpgradeDb(final DbSession session, final String version,
                               final boolean tryFix)
      throws WaarpDatabaseNoConnectionException {
    return DbModelFactoryR66
        .needUpgradeDbAllDb(dbTypeResolver, session, version);
  }

}
