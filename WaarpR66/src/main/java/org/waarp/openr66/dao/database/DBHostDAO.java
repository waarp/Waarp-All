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

package org.waarp.openr66.dao.database;

import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.lru.SynchronizedLruCache;
import org.waarp.openr66.dao.HostDAO;
import org.waarp.openr66.pojo.Host;
import org.waarp.openr66.pojo.UpdatedInfo;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Implementation of HostDAO for standard SQL databases
 */
public class DBHostDAO extends StatementExecutor<Host> implements HostDAO {

  protected static final String TABLE = "hosts";

  public static final String HOSTID_FIELD = "hostid";
  public static final String ADDRESS_FIELD = "address";
  public static final String PORT_FIELD = "port";
  public static final String IS_SSL_FIELD = "isssl";
  public static final String IS_CLIENT_FIELD = "isclient";
  public static final String IS_ACTIVE_FIELD = "isactive";
  public static final String IS_PROXIFIED_FIELD = "isproxified";
  public static final String HOSTKEY_FIELD = "hostkey";
  public static final String ADMINROLE_FIELD = "adminrole";
  public static final String UPDATED_INFO_FIELD = "updatedinfo";

  protected static final String SQL_DELETE_ALL = "DELETE FROM " + TABLE;
  protected static final String SQL_DELETE =
      "DELETE FROM " + TABLE + WHERE + HOSTID_FIELD + PARAMETER;
  protected static final String SQL_GET_ALL = "SELECT * FROM " + TABLE;
  protected static final String SQL_EXIST =
      "SELECT 1 FROM " + TABLE + WHERE + HOSTID_FIELD + PARAMETER;
  protected static final String SQL_SELECT =
      "SELECT * FROM " + TABLE + WHERE + HOSTID_FIELD + PARAMETER;
  protected static final String SQL_INSERT =
      "INSERT INTO " + TABLE + " (" + HOSTID_FIELD + ", " + ADDRESS_FIELD +
      ", " + PORT_FIELD + ", " + IS_SSL_FIELD + ", " + IS_CLIENT_FIELD + ", " +
      IS_ACTIVE_FIELD + ", " + IS_PROXIFIED_FIELD + ", " + HOSTKEY_FIELD +
      ", " + ADMINROLE_FIELD + ", " + UPDATED_INFO_FIELD +
      ") VALUES (?,?,?,?,?,?,?,?,?,?)";
  protected static final String SQL_UPDATE =
      "UPDATE " + TABLE + " SET " + HOSTID_FIELD + PARAMETER_COMMA +
      ADDRESS_FIELD + PARAMETER_COMMA + PORT_FIELD + PARAMETER_COMMA +
      IS_SSL_FIELD + PARAMETER_COMMA + IS_CLIENT_FIELD + PARAMETER_COMMA +
      IS_ACTIVE_FIELD + PARAMETER_COMMA + IS_PROXIFIED_FIELD + PARAMETER_COMMA +
      HOSTKEY_FIELD + PARAMETER_COMMA + ADMINROLE_FIELD + PARAMETER_COMMA +
      UPDATED_INFO_FIELD + " = ? WHERE " + HOSTID_FIELD + PARAMETER;


  /**
   * Hashmap for Host
   */
  private static final SynchronizedLruCache<String, Host>
      reentrantConcurrentHashMap =
      new SynchronizedLruCache<String, Host>(500, 180000);

  public DBHostDAO(final Connection con) {
    super(con);
  }

  @Override
  protected boolean isCachedEnable() {
    return Configuration.configuration.getMultipleMonitors() <= 1;
  }

  @Override
  protected SynchronizedLruCache<String, Host> getCache() {
    return reentrantConcurrentHashMap;
  }

  @Override
  protected String getId(final Host e1) {
    return e1.getHostid();
  }

  @Override
  protected String getSelectRequest() {
    return SQL_SELECT;
  }

  @Override
  protected String getGetAllRequest() {
    return SQL_GET_ALL;
  }

  @Override
  protected String getExistRequest() {
    return SQL_EXIST;
  }

  @Override
  protected Object[] getInsertValues(final Host host)
      throws WaarpDatabaseSqlException {
    host.checkValues();
    return new Object[] {
        host.getHostid(), host.getAddress(), host.getPort(), host.isSSL(),
        host.isClient(), host.isActive(), host.isProxified(), host.getHostkey(),
        host.isAdmin(), host.getUpdatedInfo().ordinal()
    };
  }

  @Override
  protected String getInsertRequest() {
    return SQL_INSERT;
  }

  @Override
  protected Object[] getUpdateValues(final Host host)
      throws WaarpDatabaseSqlException {
    host.checkValues();
    return new Object[] {
        host.getHostid(), host.getAddress(), host.getPort(), host.isSSL(),
        host.isClient(), host.isActive(), host.isProxified(), host.getHostkey(),
        host.isAdmin(), host.getUpdatedInfo().ordinal(), host.getHostid()
    };
  }

  @Override
  protected String getUpdateRequest() {
    return SQL_UPDATE;
  }

  @Override
  protected String getDeleteRequest() {
    return SQL_DELETE;
  }

  @Override
  protected String getDeleteAllRequest() {
    return SQL_DELETE_ALL;
  }

  @Override
  public Host getFromResultSet(final ResultSet set) throws SQLException {
    try {
      return new Host(set.getString(HOSTID_FIELD), set.getString(ADDRESS_FIELD),
                      set.getInt(PORT_FIELD), set.getBytes(HOSTKEY_FIELD),
                      set.getBoolean(IS_SSL_FIELD),
                      set.getBoolean(IS_CLIENT_FIELD),
                      set.getBoolean(IS_PROXIFIED_FIELD),
                      set.getBoolean(ADMINROLE_FIELD),
                      set.getBoolean(IS_ACTIVE_FIELD),
                      UpdatedInfo.valueOf(set.getInt(UPDATED_INFO_FIELD)));
    } catch (WaarpDatabaseSqlException e) {
      throw new SQLException(e);
    }
  }
}
