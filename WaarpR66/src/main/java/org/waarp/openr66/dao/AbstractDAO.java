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

import java.util.List;

/**
 * Interface to interact with objects in the persistance layer
 *
 * @param <E>
 */
public interface AbstractDAO<E> extends Cloneable {

  /**
   * Retrieve all objects in a List from the persistance layer
   *
   * @throws DAOConnectionException If data access error occurs
   */
  List<E> getAll() throws DAOConnectionException;

  /**
   * Retrieve all objects corresponding to the given filters in a List
   * from the persistance layer
   *
   * @param filters List of filter
   *
   * @throws DAOConnectionException If data access error occurs
   */
  List<E> find(List<Filter> filters) throws DAOConnectionException;

  /**
   * Count all objects corresponding to the given filters
   * from the persistance layer
   *
   * @param filters List of filter
   *
   * @throws DAOConnectionException If data access error occurs
   */
  long count(List<Filter> filters) throws DAOConnectionException;

  /**
   * Retrieve the object with the specified id from the persistance
   * layer
   *
   * @param id id of the object requested
   *
   * @throws DAOConnectionException If a data access error occurs
   * @throws DAONoDataException if no data are available
   */
  E select(String id) throws DAOConnectionException, DAONoDataException;

  /**
   * Verify if an object with the specified id exists in the
   * persistance layer
   *
   * @param id id of the object verified
   *
   * @return true if a object with the specified id exist; false if
   *     no object correspond to the
   *     specified id.
   *
   * @throws DAOConnectionException If a data access error occurs
   */
  boolean exist(String id) throws DAOConnectionException;

  /**
   * Insert the specified object in the persistance layer
   *
   * @param object object to insert
   *
   * @throws DAOConnectionException If a data access error occurs
   */
  void insert(E object) throws DAOConnectionException;

  /**
   * Update the specified object in the persistance layer
   *
   * @param object object to update
   *
   * @throws DAOConnectionException If a data access error occurs
   * @throws DAONoDataException if no data are available
   */
  void update(E object) throws DAOConnectionException, DAONoDataException;

  /**
   * Remove the specified object from the persistance layer
   *
   * @param object object to delete
   *
   * @throws DAOConnectionException If a data access error occurs
   * @throws DAONoDataException if no data are available
   */
  void delete(E object) throws DAOConnectionException, DAONoDataException;

  /**
   * Remove all objects from the persistance layer
   *
   * @throws DAOConnectionException If a data access error occurs
   */
  void deleteAll() throws DAOConnectionException;

  void close();
}
