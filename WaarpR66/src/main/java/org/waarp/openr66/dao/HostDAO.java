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

package org.waarp.openr66.dao;

import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Host;

import java.util.List;

/**
 * Interface to interact with Host objects in the persistance layer
 */
public interface HostDAO {

  /**
   * Retrieve all Host objects in a List from the persistance layer
   *
   * @throws DAOConnectionException If data access error occurs
   */
  List<Host> getAll() throws DAOConnectionException;

  /**
   * Retrieve all Host objects corresponding to the given filters in a List
   * from the persistance lsayer
   *
   * @param filters List of filter
   *
   * @throws DAOConnectionException If data access error occurs
   */
  List<Host> find(List<Filter> filters) throws DAOConnectionException;

  /**
   * Retrieve the Host object with the specified hostid from the persistance
   * layer
   *
   * @param hostid Hostid of the Host object requested
   *
   * @throws DAOConnectionException If a data access error occurs
   * @throws DAONoDataException if no data are available
   */
  Host select(String hostid) throws DAOConnectionException, DAONoDataException;

  /**
   * Verify if a Host object with the specified hostid exists in the
   * persistance layer
   *
   * @param hostid Hostid of the Host object verified
   *
   * @return true if a Host object with the specified hostid exist; false if
   *     no Host object correspond to the
   *     specified hostid.
   *
   * @throws DAOConnectionException If a data access error occurs
   */
  boolean exist(String hostid) throws DAOConnectionException;

  /**
   * Insert the specified Host object in the persistance layer
   *
   * @param host Host object to insert
   *
   * @throws DAOConnectionException If a data access error occurs
   */
  void insert(Host host) throws DAOConnectionException;

  /**
   * Update the specified Host object in the persistance layer
   *
   * @param host Host object to update
   *
   * @throws DAOConnectionException If a data access error occurs
   * @throws DAONoDataException if no data are available
   */
  void update(Host host) throws DAOConnectionException, DAONoDataException;

  /**
   * Remove the specified Host object from the persistance layer
   *
   * @param host Host object to insert
   *
   * @throws DAOConnectionException If a data access error occurs
   * @throws DAONoDataException if no data are available
   */
  void delete(Host host) throws DAOConnectionException, DAONoDataException;

  /**
   * Remove all Host objects from the persistance layer
   *
   * @throws DAOConnectionException If a data access error occurs
   */
  void deleteAll() throws DAOConnectionException;

  void close();
}
