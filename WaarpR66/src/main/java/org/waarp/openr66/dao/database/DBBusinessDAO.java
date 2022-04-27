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
import org.waarp.openr66.dao.BusinessDAO;
import org.waarp.openr66.pojo.Business;
import org.waarp.openr66.pojo.UpdatedInfo;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Implementation of BusinessDAO for standard SQL databases
 */
public class DBBusinessDAO extends StatementExecutor<Business>
    implements BusinessDAO {

  protected static final String TABLE = "hostconfig";

  public static final String HOSTID_FIELD = "hostid";
  public static final String BUSINESS_FIELD = "business";
  public static final String ROLES_FIELD = "roles";
  public static final String ALIASES_FIELD = "aliases";
  public static final String OTHERS_FIELD = "others";
  public static final String UPDATED_INFO_FIELD = "updatedInfo";

  protected static final String SQL_DELETE_ALL = "DELETE FROM " + TABLE;
  protected static final String SQL_DELETE =
      "DELETE FROM " + TABLE + WHERE + HOSTID_FIELD + PARAMETER;
  protected static final String SQL_GET_ALL = "SELECT * FROM " + TABLE;
  protected static final String SQL_COUNT_ALL = SQL_COUNT_ALL_PREFIX + TABLE;
  protected static final String SQL_EXIST =
      "SELECT 1 FROM " + TABLE + WHERE + HOSTID_FIELD + PARAMETER;
  protected static final String SQL_SELECT =
      "SELECT * FROM " + TABLE + WHERE + HOSTID_FIELD + PARAMETER;
  protected static final String SQL_INSERT =
      "INSERT INTO " + TABLE + " (" + HOSTID_FIELD + ", " + BUSINESS_FIELD +
      ", " + ROLES_FIELD + ", " + ALIASES_FIELD + ", " + OTHERS_FIELD + ", " +
      UPDATED_INFO_FIELD + ") VALUES (?,?,?,?,?,?)";
  protected static final String SQL_UPDATE =
      "UPDATE " + TABLE + " SET " + HOSTID_FIELD + PARAMETER_COMMA +
      BUSINESS_FIELD + PARAMETER_COMMA + ROLES_FIELD + PARAMETER_COMMA +
      ALIASES_FIELD + PARAMETER_COMMA + OTHERS_FIELD + PARAMETER_COMMA +
      UPDATED_INFO_FIELD + " = ? WHERE " + HOSTID_FIELD + PARAMETER;

  /**
   * Hashmap for Business
   */
  private static final SynchronizedLruCache<String, Business>
      reentrantConcurrentHashMap =
      new SynchronizedLruCache<String, Business>(500, 180000);

  public DBBusinessDAO(final Connection con) {
    super(con);
  }

  @Override
  protected final boolean isCachedEnable() {
    return Configuration.configuration.getMultipleMonitors() <= 1;
  }

  @Override
  protected final SynchronizedLruCache<String, Business> getCache() {
    return reentrantConcurrentHashMap;
  }

  @Override
  protected final String getId(final Business e1) {
    return e1.getHostid();
  }

  @Override
  protected final String getTable() {
    return TABLE;
  }

  @Override
  protected final String getSelectRequest() {
    return SQL_SELECT;
  }

  @Override
  protected final String getGetAllRequest() {
    return SQL_GET_ALL;
  }

  @Override
  protected final String getCountRequest() {
    return SQL_COUNT_ALL;
  }

  @Override
  protected final String getExistRequest() {
    return SQL_EXIST;
  }

  @Override
  protected final Object[] getInsertValues(final Business business)
      throws WaarpDatabaseSqlException {
    business.checkValues();
    return new Object[] {
        business.getHostid(), business.getBusiness(), business.getRoles(),
        business.getAliases(), business.getOthers(),
        business.getUpdatedInfo().ordinal()
    };
  }

  @Override
  protected final String getInsertRequest() {
    return SQL_INSERT;
  }

  @Override
  protected final Object[] getUpdateValues(final Business business)
      throws WaarpDatabaseSqlException {
    business.checkValues();
    return new Object[] {
        business.getHostid(), business.getBusiness(), business.getRoles(),
        business.getAliases(), business.getOthers(),
        business.getUpdatedInfo().ordinal(), business.getHostid()
    };
  }

  @Override
  protected final String getUpdateRequest() {
    return SQL_UPDATE;
  }

  @Override
  protected final String getDeleteRequest() {
    return SQL_DELETE;
  }

  @Override
  protected final String getDeleteAllRequest() {
    return SQL_DELETE_ALL;
  }

  @Override
  public final Business getFromResultSet(final ResultSet set)
      throws SQLException {
    try {
      return new Business(set.getString(HOSTID_FIELD),
                          set.getString(BUSINESS_FIELD),
                          set.getString(ROLES_FIELD),
                          set.getString(ALIASES_FIELD),
                          set.getString(OTHERS_FIELD),
                          UpdatedInfo.valueOf(set.getInt(UPDATED_INFO_FIELD)));
    } catch (final WaarpDatabaseSqlException e) {
      throw new SQLException(e);
    }
  }

  @Override
  protected final boolean isDbTransfer() {
    return false;
  }
}
