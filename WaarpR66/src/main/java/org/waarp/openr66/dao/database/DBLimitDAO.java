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
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.LimitDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Limit;
import org.waarp.openr66.pojo.UpdatedInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of LimitDAO for standard SQL databases
 */
public class DBLimitDAO extends StatementExecutor implements LimitDAO {

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
  public void delete(Limit limit)
      throws DAOConnectionException, DAONoDataException {
    PreparedStatement stm = null;
    try {
      stm = connection.prepareStatement(SQL_DELETE);
      setParameters(stm, limit.getHostid());
      try {
        executeUpdate(stm);
      } catch (final SQLException e2) {
        throw new DAONoDataException(e2);
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
    }
  }

  @Override
  public void deleteAll() throws DAOConnectionException {
    PreparedStatement stm = null;
    try {
      stm = connection.prepareStatement(SQL_DELETE_ALL);
      executeUpdate(stm);
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
    }
  }

  @Override
  public List<Limit> getAll() throws DAOConnectionException {
    final ArrayList<Limit> limits = new ArrayList<Limit>();
    ResultSet res = null;
    PreparedStatement stm = null;
    try {
      stm = connection.prepareStatement(SQL_GET_ALL);
      res = executeQuery(stm);
      while (res.next()) {
        limits.add(getFromResultSet(res));
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
      closeResultSet(res);
    }
    return limits;
  }

  @Override
  public List<Limit> find(List<Filter> filters) throws DAOConnectionException {
    final ArrayList<Limit> limits = new ArrayList<Limit>();
    // Create the SQL query
    final StringBuilder query = new StringBuilder(SQL_GET_ALL);
    final Object[] params = new Object[filters.size()];
    final Iterator<Filter> it = filters.listIterator();
    if (it.hasNext()) {
      query.append(" WHERE ");
    }
    String prefix = "";
    int i = 0;
    while (it.hasNext()) {
      query.append(prefix);
      final Filter filter = it.next();
      query.append(filter.key + ' ' + filter.operand + " ?");
      params[i] = filter.value;
      i++;
      prefix = " AND ";
    }
    // Execute query
    ResultSet res = null;
    PreparedStatement stm = null;
    try {
      stm = connection.prepareStatement(query.toString());
      setParameters(stm, params);
      res = executeQuery(stm);
      while (res.next()) {
        limits.add(getFromResultSet(res));
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
      closeResultSet(res);
    }
    return limits;
  }

  @Override
  public boolean exist(String hostid) throws DAOConnectionException {
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(SQL_EXIST);
      setParameters(stm, hostid);
      res = executeQuery(stm);
      return res.next();
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
      closeResultSet(res);
    }
  }

  @Override
  public Limit select(String hostid)
      throws DAOConnectionException, DAONoDataException {
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(SQL_SELECT);
      setParameters(stm, hostid);
      res = executeQuery(stm);
      if (res.next()) {
        return getFromResultSet(res);
      } else {
        throw new DAONoDataException("No " + getClass().getName() + " found");
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
      closeResultSet(res);
    }
  }

  @Override
  public void insert(Limit limit) throws DAOConnectionException {
    final Object[] params = {
        limit.getHostid(), limit.getReadGlobalLimit(),
        limit.getWriteGlobalLimit(), limit.getReadSessionLimit(),
        limit.getWriteSessionLimit(), limit.getDelayLimit(),
        limit.getUpdatedInfo().ordinal()
    };

    PreparedStatement stm = null;
    try {
      stm = connection.prepareStatement(SQL_INSERT);
      setParameters(stm, params);
      executeUpdate(stm);
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
    }
  }

  @Override
  public void update(Limit limit)
      throws DAOConnectionException, DAONoDataException {
    final Object[] params = {
        limit.getHostid(), limit.getReadGlobalLimit(),
        limit.getWriteGlobalLimit(), limit.getReadSessionLimit(),
        limit.getWriteSessionLimit(), limit.getDelayLimit(),
        limit.getUpdatedInfo().ordinal(), limit.getHostid()
    };

    PreparedStatement stm = null;
    try {
      stm = connection.prepareStatement(SQL_UPDATE);
      setParameters(stm, params);
      try {
        executeUpdate(stm);
      } catch (final SQLException e2) {
        throw new DAONoDataException(e2);
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
    }
  }

  private Limit getFromResultSet(ResultSet set) throws SQLException {
    return new Limit(set.getString(HOSTID_FIELD),
                     set.getLong(DELAY_LIMIT_FIELD),
                     set.getLong(READ_GLOBAL_LIMIT_FIELD),
                     set.getLong(WRITE_GLOBAL_LIMIT_FIELD),
                     set.getLong(READ_SESSION_LIMIT_FIELD),
                     set.getLong(WRITE_SESSION_LIMIT_FIELD),
                     UpdatedInfo.valueOf(set.getInt(UPDATED_INFO_FIELD)));
  }
}
