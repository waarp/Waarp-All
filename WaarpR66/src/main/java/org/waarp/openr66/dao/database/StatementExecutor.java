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
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.lru.SynchronizedLruCache;
import org.waarp.openr66.dao.AbstractDAO;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.database.DBDAOFactory.FakeConnection;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public abstract class StatementExecutor<E> implements AbstractDAO<E> {

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(StatementExecutor.class);
  protected static final String WHERE = " WHERE ";
  protected static final String PARAMETER = " = ?";
  protected static final String PARAMETER_COMMA = " = ?, ";
  protected static final String SQL_COUNT_ALL_PREFIX =
      "SELECT COUNT(*) AS total FROM ";
  protected Connection connection;

  protected abstract boolean isDbTransfer();

  public abstract E getFromResultSet(ResultSet set)
      throws SQLException, DAOConnectionException;

  protected abstract boolean isCachedEnable();

  protected SynchronizedLruCache<String, E> getCache() {
    return null;
  }

  protected final void clearCache() {
    if (isCachedEnable()) {
      getCache().clear();
    }
  }

  protected final void addToCache(final String key, final E elt) {
    if (isCachedEnable()) {
      getCache().put(key, elt);
    }
  }

  protected final E getFromCache(final String key) {
    if (isCachedEnable()) {
      final E value = getCache().get(key);
      if (value != null) {
        getCache().updateTtl(key);
      }
      return value;
    }
    return null;
  }

  protected final void removeFromCache(final String key) {
    if (isCachedEnable()) {
      getCache().remove(key);
    }
  }

  protected final boolean isInCache(final String key) {
    if (isCachedEnable()) {
      return getCache().contains(key);
    }
    return false;
  }

  protected StatementExecutor(final Connection con) {
    connection = con;
  }

  protected final void setParameters(final PreparedStatement stm,
                                     final Object... values)
      throws SQLException {
    if (values == null) {
      return;
    }
    for (int i = 0; i < values.length; i++) {
      stm.setObject(i + 1, values[i]);
    }
  }

  final void executeUpdate(final PreparedStatement stm) throws SQLException {
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

  protected final void executeAction(final PreparedStatement stm)
      throws SQLException {
    stm.executeUpdate();
  }

  protected final ResultSet executeQuery(final PreparedStatement stm)
      throws SQLException {
    return stm.executeQuery();
  }

  protected final void closeStatement(final Statement stm) {
    if (stm == null) {
      return;
    }
    try {
      stm.close();
    } catch (final SQLException e) {
      logger.warn("An error occurs while closing the statement." + " : {}",
                  e.getMessage());
    }
  }

  protected final void closeResultSet(final ResultSet rs) {
    if (rs == null) {
      return;
    }
    try {
      rs.close();
    } catch (final SQLException e) {
      logger.warn("An error occurs while closing the resultSet." + " : {}",
                  e.getMessage());
    }
  }

  public final void close() {
    try {
      connection.close();
    } catch (final SQLException e) {
      logger.warn("Cannot properly close the database connection" + " : {}",
                  e.getMessage());
    }
  }

  protected abstract String getId(E e1);

  protected abstract String getSelectRequest();

  protected abstract String getCountRequest();

  protected abstract String getGetAllRequest();

  protected abstract String getExistRequest();

  protected abstract Object[] getInsertValues(E e1)
      throws WaarpDatabaseSqlException;

  protected abstract String getInsertRequest();

  protected abstract Object[] getUpdateValues(E e1)
      throws WaarpDatabaseSqlException;

  protected abstract String getUpdateRequest();

  protected abstract String getDeleteRequest();

  protected abstract String getDeleteAllRequest();

  @Override
  public void delete(final E e1)
      throws DAOConnectionException, DAONoDataException {
    if (isCachedEnable()) {
      // Need to check since all does not accept getId
      removeFromCache(getId(e1));
    }
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
  public final void deleteAll() throws DAOConnectionException {
    clearCache();
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
  public final List<E> getAll() throws DAOConnectionException {
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

  final String prepareFindQuery(final List<Filter> filters,
                                final Object[] params) {
    final StringBuilder query = new StringBuilder(getGetAllRequest());
    if (filters.isEmpty()) {
      return query.toString();
    }
    query.append(WHERE);
    String prefix = "";
    int i = 0;
    for (final Filter filter : filters) {
      query.append(prefix);
      if (filter.nbAdditionnalParams() > 0) {
        final Object[] objects = (Object[]) filter.append(query);
        for (final Object o : objects) {
          params[i++] = o;
        }
      } else if (filter.nbAdditionnalParams() == 0) {
        params[i] = filter.append(query);
        i++;
      } else {
        filter.append(query);
      }
      prefix = " AND ";
    }
    return query.toString();
  }

  final Object[] prepareFindParams(final List<Filter> filters) {
    Object[] params = new Object[0];
    if (filters != null) {
      int len = filters.size();
      for (final Filter filter : filters) {
        len += filter.nbAdditionnalParams();
      }
      params = new Object[len];
    }
    return params;
  }

  @Override
  public final List<E> find(final List<Filter> filters)
      throws DAOConnectionException {
    final ArrayList<E> es = new ArrayList<E>();
    // Create the SQL query
    final Object[] params = prepareFindParams(filters);
    final StringBuilder query =
        new StringBuilder(prepareFindQuery(filters, params));
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

  final String prepareCountQuery(final List<Filter> filters,
                                 final Object[] params) {
    final StringBuilder query = new StringBuilder(getCountRequest());
    if (filters.isEmpty()) {
      return query.toString();
    }
    query.append(WHERE);
    String prefix = "";
    int i = 0;
    for (final Filter filter : filters) {
      query.append(prefix);
      if (filter.nbAdditionnalParams() > 0) {
        final Object[] objects = (Object[]) filter.append(query);
        for (final Object o : objects) {
          params[i++] = o;
        }
      } else if (filter.nbAdditionnalParams() == 0) {
        params[i] = filter.append(query);
        i++;
      } else {
        filter.append(query);
      }
      prefix = " AND ";
    }
    return query.toString();
  }

  @Override
  public final long count(final List<Filter> filters)
      throws DAOConnectionException {
    // Create the SQL query
    final Object[] params = prepareFindParams(filters);
    final StringBuilder query =
        new StringBuilder(prepareCountQuery(filters, params));
    // Execute query
    PreparedStatement stm = null;
    ResultSet res = null;
    long total = -1;
    try {
      stm = connection.prepareStatement(query.toString());
      setParameters(stm, params);
      res = executeQuery(stm);
      if (res.next()) {
        total = res.getLong("total");
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
    if (total < 0) {
      throw new DAOConnectionException("Count cannot be retrieved");
    }
    return total;
  }

  @Override
  public final boolean exist(final String id) throws DAOConnectionException {
    if (isDbTransfer()) {
      throw new UnsupportedOperationException();
    }
    if (isInCache(id)) {
      return true;
    }
    if (connection instanceof FakeConnection) {
      try {
        connection = ((FakeConnection) connection).getRealConnection();
      } catch (final SQLException throwables) {
        throw new DAOConnectionException(throwables);
      }
    }
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
  public final E select(final String id)
      throws DAOConnectionException, DAONoDataException {
    if (isDbTransfer()) {
      throw new UnsupportedOperationException();
    }
    if (isCachedEnable()) {
      final E found = getFromCache(id);
      if (found != null) {
        return found;
      }
    }
    if (connection instanceof FakeConnection) {
      try {
        connection = ((FakeConnection) connection).getRealConnection();
      } catch (final SQLException throwables) {
        throw new DAOConnectionException(throwables);
      }
    }
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(getSelectRequest());
      setParameters(stm, id);
      res = executeQuery(stm);
      if (res.next()) {
        final E found = getFromResultSet(res);
        addToCache(id, found);
        return found;
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
    if (isCachedEnable()) {
      // Need to check since all does not accept getId
      addToCache(getId(e1), e1);
    }
    final Object[] params;
    try {
      params = getInsertValues(e1);
    } catch (final WaarpDatabaseSqlException e) {
      throw new DAOConnectionException(e);
    }

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
  public final void update(final E e1)
      throws DAOConnectionException, DAONoDataException {
    if (isCachedEnable()) {
      // Need to check since all does not accept getId
      addToCache(getId(e1), e1);
    }
    final Object[] params;
    try {
      params = getUpdateValues(e1);
    } catch (final WaarpDatabaseSqlException e) {
      throw new DAOConnectionException(e);
    }

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
