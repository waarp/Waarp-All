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

package org.waarp.openr66.dao.xml;

import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.LimitDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Limit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class XMLLimitDAO implements LimitDAO {

  /**
   * HashTable in case of lack of database
   */
  private static final ConcurrentHashMap<String, Limit>
      dbR66ConfigurationHashMap = new ConcurrentHashMap<String, Limit>();

  public XMLLimitDAO() {
    // Empty
  }

  @Override
  public void close() {
    // ignore
  }

  @Override
  public void delete(final Limit limit) {
    dbR66ConfigurationHashMap.remove(limit.getHostid());
  }

  @Override
  public void deleteAll() {
    dbR66ConfigurationHashMap.clear();
  }

  @Override
  public List<Limit> getAll() throws DAOConnectionException {
    return new ArrayList<Limit>(dbR66ConfigurationHashMap.values());
  }

  @Override
  public boolean exist(final String hostid) throws DAOConnectionException {
    return dbR66ConfigurationHashMap.containsKey(hostid);
  }

  @Override
  public List<Limit> find(final List<Filter> fitlers)
      throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  /**
   * {@link DAOConnectionException}
   *
   * @return count only if filters is empty or null
   */
  @Override
  public long count(final List<Filter> fitlers) throws DAOConnectionException {
    if (fitlers == null || fitlers.isEmpty()) {
      return dbR66ConfigurationHashMap.size();
    }
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  @Override
  public void insert(final Limit limit) {
    dbR66ConfigurationHashMap.put(limit.getHostid(), limit);
  }

  @Override
  public Limit select(final String hostid)
      throws DAOConnectionException, DAONoDataException {
    Limit limit = dbR66ConfigurationHashMap.get(hostid);
    if (limit != null) {
      return limit;
    }
    throw new DAONoDataException("Limit not found");
  }

  @Override
  public void update(final Limit limit) {
    dbR66ConfigurationHashMap.put(limit.getHostid(), limit);
  }
}
