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
import org.waarp.openr66.pojo.Transfer;

import java.util.List;

/**
 * Interface to interact with Transfer objects in the persistance layer
 */
public interface TransferDAO extends AbstractDAO<Transfer> {

  /**
   * Retrieve all Transfer objects to the given filters in a List from the
   * persistance layer
   *
   * @param filters List of filter
   *
   * @throws DAOConnectionException If data access error occurs
   */
  List<Transfer> find(List<Filter> filters, int limit)
      throws DAOConnectionException;

  /**
   * Retrieve all Transfer objects to the given filters in a List from the
   * persistance layer
   *
   * @param filters List of filter
   *
   * @throws DAOConnectionException If data access error occurs
   */
  List<Transfer> find(List<Filter> filters, int limit, int offset)
      throws DAOConnectionException;

  /**
   * Retrieve all Transfer objects to the given filters in a List from the
   * persistance layer
   *
   * @param filters List of filter
   *
   * @throws DAOConnectionException If data access error occurs
   */
  List<Transfer> find(List<Filter> filters, String column, boolean ascend)
      throws DAOConnectionException;

  /**
   * Retrieve all Transfer objects to the given filters in a List from the
   * persistance layer
   *
   * @param filters List of filter
   *
   * @throws DAOConnectionException If data access error occurs
   */
  List<Transfer> find(List<Filter> filters, String column, boolean ascend,
                      int limit) throws DAOConnectionException;

  /**
   * Retrieve all Transfer objects to the given filters in a List from the
   * persistance layer
   *
   * @param filters List of filter
   *
   * @throws DAOConnectionException If data access error occurs
   */
  List<Transfer> find(List<Filter> filters, String column, boolean ascend,
                      int limit, int offset) throws DAOConnectionException;

  /**
   * Retrieve the Transfer object with the specified Special ID from the
   * persistance layer
   *
   * @param id special ID of the Transfer object requested
   *
   * @throws DAOConnectionException If a data access error occurs
   * @throws DAONoDataException if no data are available
   */
  Transfer select(long id, String requester, String requested, String owner)
      throws DAOConnectionException, DAONoDataException;

  /**
   * Verify if a Transfer object with the specified Special ID exists in the
   * persistance layer
   *
   * @param id special ID of the Transfer object verified
   *
   * @return true if a Transfer object with the specified Special ID exist;
   *     false if no Transfer object
   *     correspond to the specified Special ID.
   *
   * @throws DAOConnectionException If a data access error occurs
   */
  boolean exist(long id, String requester, String requested, String owner)
      throws DAOConnectionException;
}
