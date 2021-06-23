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
import org.waarp.openr66.dao.MultipleMonitorDAO;
import org.waarp.openr66.pojo.MultipleMonitor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Implementation of MultipleMonitorDAO for standard SQL databases
 */
public class DBMultipleMonitorDAO extends StatementExecutor<MultipleMonitor>
    implements MultipleMonitorDAO {

  protected static final String TABLE = "multiplemonitor";

  public static final String HOSTID_FIELD = "hostid";
  public static final String COUNT_CONFIG_FIELD = "countconfig";
  public static final String COUNT_HOST_FIELD = "counthost";
  public static final String COUNT_RULE_FIELD = "countrule";

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
      "INSERT INTO " + TABLE + " (" + HOSTID_FIELD + ", " + COUNT_CONFIG_FIELD +
      ", " + COUNT_HOST_FIELD + ", " + COUNT_RULE_FIELD + ") VALUES (?,?,?,?)";

  protected static final String SQL_UPDATE =
      "UPDATE " + TABLE + " SET " + HOSTID_FIELD + PARAMETER_COMMA +
      COUNT_CONFIG_FIELD + PARAMETER_COMMA + COUNT_HOST_FIELD +
      PARAMETER_COMMA + COUNT_RULE_FIELD + " = ? WHERE " + HOSTID_FIELD +
      PARAMETER;


  public DBMultipleMonitorDAO(final Connection con) {
    super(con);
  }

  @Override
  protected String getId(final MultipleMonitor e1) {
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
  protected String getCountRequest() {
    return SQL_COUNT_ALL;
  }

  @Override
  protected String getExistRequest() {
    return SQL_EXIST;
  }

  @Override
  protected Object[] getInsertValues(final MultipleMonitor multipleMonitor) {
    return new Object[] {
        multipleMonitor.getHostid(), multipleMonitor.getCountConfig(),
        multipleMonitor.getCountHost(), multipleMonitor.getCountRule()
    };
  }

  @Override
  protected String getInsertRequest() {
    return SQL_INSERT;
  }

  @Override
  protected Object[] getUpdateValues(final MultipleMonitor multipleMonitor) {
    return new Object[] {
        multipleMonitor.getHostid(), multipleMonitor.getCountConfig(),
        multipleMonitor.getCountHost(), multipleMonitor.getCountRule(),
        multipleMonitor.getHostid()
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
  public MultipleMonitor getFromResultSet(final ResultSet set)
      throws SQLException {
    try {
      return new MultipleMonitor(set.getString(HOSTID_FIELD),
                                 set.getInt(COUNT_CONFIG_FIELD),
                                 set.getInt(COUNT_HOST_FIELD),
                                 set.getInt(COUNT_RULE_FIELD));
    } catch (WaarpDatabaseSqlException e) {
      throw new SQLException(e);
    }
  }
}
