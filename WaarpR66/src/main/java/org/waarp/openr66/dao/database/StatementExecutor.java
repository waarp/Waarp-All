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
import org.waarp.openr66.dao.AbstractDAO;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class StatementExecutor<E> implements AbstractDAO<E> {

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(StatementExecutor.class);
  protected final Connection connection;

  public abstract E getFromResultSet(ResultSet set)
      throws SQLException, DAOConnectionException;

  protected StatementExecutor(final Connection con) {
    connection = con;
  }

  public void setParameters(final PreparedStatement stm, final Object... values)
      throws SQLException {
    for (int i = 0; i < values.length; i++) {
      stm.setObject(i + 1, values[i]);
    }
  }

  public void executeUpdate(final PreparedStatement stm) throws SQLException {
    final int res;
    res = stm.executeUpdate();
    if (res < 1) {
      logger.error("Update failed, no record updated.");
      //FIXME should be throw new SQLDataException("Update failed, no record
      // updated.");
    } else {
      logger.debug("{} records updated.", res);
    }
  }

  public void executeAction(final PreparedStatement stm) throws SQLException {
    stm.executeUpdate();
  }

  public ResultSet executeQuery(final PreparedStatement stm)
      throws SQLException {
    return stm.executeQuery();
  }

  public void closeStatement(final Statement stm) {
    if (stm == null) {
      return;
    }
    try {
      stm.close();
    } catch (final SQLException e) {
      logger.warn("An error occurs while closing the statement.", e);
    }
  }

  public void closeResultSet(final ResultSet rs) {
    if (rs == null) {
      return;
    }
    try {
      rs.close();
    } catch (final SQLException e) {
      logger.warn("An error occurs while closing the resultSet.", e);
    }
  }

  public void close() {
    try {
      connection.close();
    } catch (final SQLException e) {
      logger.warn("Cannot properly close the database connection", e);
    }
  }

  protected abstract String getId(E e1);

  protected abstract String getSelectRequest();

  protected abstract String getGetAllRequest();

  protected abstract String getExistRequest();

  protected abstract Object[] getInsertValues(E e1);

  protected abstract String getInsertRequest();

  protected abstract Object[] getUpdateValues(E e1);

  protected abstract String getUpdateRequest();

  protected abstract String getDeleteRequest();

  protected abstract String getDeleteAllRequest();

  @Override
  public void delete(final E e1)
      throws DAOConnectionException, DAONoDataException {
    PreparedStatement stm = null;
    try {
      stm = connection.prepareStatement(getDeleteRequest());
      setParameters(stm, getId(e1));
      try {
        executeAction(stm);
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
      stm = connection.prepareStatement(getDeleteAllRequest());
      executeAction(stm);
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
    }
  }

  @Override
  public List<E> getAll() throws DAOConnectionException {
    final ArrayList<E> es = new ArrayList<E>();
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(getGetAllRequest());
      res = executeQuery(stm);
      while (res.next()) {
        es.add(getFromResultSet(res));
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
    return es;
  }

  @Override
  public List<E> find(final List<Filter> filters)
      throws DAOConnectionException {
    final ArrayList<E> es = new ArrayList<E>();
    // Create the SQL query
    final StringBuilder query = new StringBuilder(getGetAllRequest());
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
      query.append(filter.key).append(' ').append(filter.operand).append(" ?");
      params[i] = filter.value;
      i++;
      prefix = " AND ";
    }
    // Execute query
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(query.toString());
      setParameters(stm, params);
      res = executeQuery(stm);
      while (res.next()) {
        es.add(getFromResultSet(res));
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
    return es;
  }

  @Override
  public boolean exist(final String id) throws DAOConnectionException {
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(getExistRequest());
      setParameters(stm, id);
      res = executeQuery(stm);
      return res.next();
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
  }

  @Override
  public E select(final String id)
      throws DAOConnectionException, DAONoDataException {
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(getSelectRequest());
      setParameters(stm, id);
      res = executeQuery(stm);
      if (res.next()) {
        return getFromResultSet(res);
      } else {
        throw new DAONoDataException("No " + getClass().getName() + " found");
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
  }

  @Override
  public void insert(final E e1) throws DAOConnectionException {
    final Object[] params = getInsertValues(e1);

    PreparedStatement stm = null;
    try {
      stm = connection.prepareStatement(getInsertRequest());
      setParameters(stm, params);
      executeUpdate(stm);
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
    }
  }

  @Override
  public void update(final E e1)
      throws DAOConnectionException, DAONoDataException {
    final Object[] params = getUpdateValues(e1);

    PreparedStatement stm = null;
    try {
      stm = connection.prepareStatement(getUpdateRequest());
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

}
