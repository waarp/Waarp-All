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

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.dao.LimitDAO;
import org.waarp.openr66.pojo.Limit;
import org.waarp.openr66.pojo.UpdatedInfo;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Implementation of LimitDAO for standard SQL databases
 */
public class DBLimitDAO extends StatementExecutor<Limit> implements LimitDAO {

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DBLimitDAO.class);

  protected static final String TABLE = "configuration";

  public static final String HOSTID_FIELD = "hostid";
  public static final String READ_GLOBAL_LIMIT_FIELD = "readgloballimit";
  public static final String WRITE_GLOBAL_LIMIT_FIELD = "writegloballimit";
  public static final String READ_SESSION_LIMIT_FIELD = "readsessionlimit";
  public static final String WRITE_SESSION_LIMIT_FIELD = "writesessionlimit";
  public static final String DELAY_LIMIT_FIELD = "delaylimit";
  public static final String UPDATED_INFO_FIELD = "updatedinfo";

  protected static final String SQL_DELETE_ALL = "DELETE FROM " + TABLE;
  protected static final String SQL_DELETE =
      "DELETE FROM " + TABLE + " WHERE " + HOSTID_FIELD + " = ?";
  protected static final String SQL_GET_ALL = "SELECT * FROM " + TABLE;
  protected static final String SQL_EXIST =
      "SELECT 1 FROM " + TABLE + " WHERE " + HOSTID_FIELD + " = ?";
  protected static final String SQL_SELECT =
      "SELECT * FROM " + TABLE + " WHERE " + HOSTID_FIELD + " = ?";
  protected static final String SQL_INSERT =
      "INSERT INTO " + TABLE + " (" + HOSTID_FIELD + ", " +
      READ_GLOBAL_LIMIT_FIELD + ", " + WRITE_GLOBAL_LIMIT_FIELD + ", " +
      READ_SESSION_LIMIT_FIELD + ", " + WRITE_SESSION_LIMIT_FIELD + ", " +
      DELAY_LIMIT_FIELD + ", " + UPDATED_INFO_FIELD +
      ") VALUES (?,?,?,?,?,?,?)";

  protected static final String SQL_UPDATE =
      "UPDATE " + TABLE + " SET " + HOSTID_FIELD + " = ?, " +
      READ_GLOBAL_LIMIT_FIELD + " = ?, " + WRITE_GLOBAL_LIMIT_FIELD + " = ?, " +
      READ_SESSION_LIMIT_FIELD + " = ?, " + WRITE_SESSION_LIMIT_FIELD +
      " = ?, " + DELAY_LIMIT_FIELD + " = ?, " + UPDATED_INFO_FIELD +
      " = ? WHERE " + HOSTID_FIELD + " = ?";

  public DBLimitDAO(Connection con) {
    super(con);
  }

  @Override
  protected String getId(final Limit e1) {
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
  protected Object[] getInsertValues(final Limit limit) {
    return new Object[] {
        limit.getHostid(), limit.getReadGlobalLimit(),
        limit.getWriteGlobalLimit(), limit.getReadSessionLimit(),
        limit.getWriteSessionLimit(), limit.getDelayLimit(),
        limit.getUpdatedInfo().ordinal()
    };
  }

  @Override
  protected String getInsertRequest() {
    return SQL_INSERT;
  }

  @Override
  protected Object[] getUpdateValues(final Limit limit) {
    return new Object[] {
        limit.getHostid(), limit.getReadGlobalLimit(),
        limit.getWriteGlobalLimit(), limit.getReadSessionLimit(),
        limit.getWriteSessionLimit(), limit.getDelayLimit(),
        limit.getUpdatedInfo().ordinal(), limit.getHostid()
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
  public Limit getFromResultSet(ResultSet set) throws SQLException {
    return new Limit(set.getString(HOSTID_FIELD),
                     set.getLong(DELAY_LIMIT_FIELD),
                     set.getLong(READ_GLOBAL_LIMIT_FIELD),
                     set.getLong(WRITE_GLOBAL_LIMIT_FIELD),
                     set.getLong(READ_SESSION_LIMIT_FIELD),
                     set.getLong(WRITE_SESSION_LIMIT_FIELD),
                     UpdatedInfo.valueOf(set.getInt(UPDATED_INFO_FIELD)));
  }
}
