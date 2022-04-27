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

import org.waarp.openr66.dao.BusinessDAO;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Business;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class XMLBusinessDAO implements BusinessDAO {

  /**
   * HashTable in case of lack of database
   */
  private static final ConcurrentHashMap<String, Business>
      dbR66BusinessHashMap = new ConcurrentHashMap<String, Business>();

  public XMLBusinessDAO() {
    // Empty
  }

  @Override
  public void close() {
    // ignore
  }

  @Override
  public void delete(final Business business) {
    dbR66BusinessHashMap.remove(business.getHostid());
  }

  @Override
  public void deleteAll() {
    dbR66BusinessHashMap.clear();
  }

  @Override
  public List<Business> getAll() throws DAOConnectionException {
    return new ArrayList<Business>(dbR66BusinessHashMap.values());
  }

  @Override
  public boolean exist(final String hostid) throws DAOConnectionException {
    return dbR66BusinessHashMap.containsKey(hostid);
  }

  @Override
  public List<Business> find(final List<Filter> fitlers)
      throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  @Override
  public List<Business> find(final List<Filter> filters, final int limit)
      throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  @Override
  public List<Business> find(final List<Filter> filters, final String field,
                             final boolean asc) throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  @Override
  public List<Business> find(final List<Filter> filters, final String field,
                             final boolean asc, final int limit)
      throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  @Override
  public List<Business> find(final List<Filter> filters, final String field,
                             final boolean asc, final int limit,
                             final int offset) throws DAOConnectionException {
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  @Override
  public void update(final List<Filter> filters, final String toSet)
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
      return dbR66BusinessHashMap.size();
    }
    throw new DAOConnectionException("Operation not supported on XML DAO");
  }

  @Override
  public void insert(final Business business) {
    dbR66BusinessHashMap.put(business.getHostid(), business);
  }

  @Override
  public Business select(final String hostid)
      throws DAOConnectionException, DAONoDataException {
    final Business business = dbR66BusinessHashMap.get(hostid);
    if (business != null) {
      return business;
    }
    throw new DAONoDataException("Business not found");
  }

  @Override
  public void update(final Business business) {
    dbR66BusinessHashMap.put(business.getHostid(), business);
  }
}
