package org.waarp.openr66.dao;

import java.util.List;

import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Business;

/**
 * Interface to interact with Business objects in the persistance layer
 */
public interface BusinessDAO {

    /**
     * Retrieve all Business objects in a List from the persistance layer
     *
     * @throws DAOConnectionException If data access error occurs
     */
    List<Business> getAll() throws DAOConnectionException;

    /**
     * Retrieve all Business objects corresponding to the given filters
     * in a List from the persistance layer
     *
     * @param filters List of filter
     * @throws DAOConnectionException If data access error occurs
     */
    List<Business> find(List<Filter> filters) throws DAOConnectionException;

    /**
     * Retrieve the Business object with the specified hostid from the
     * persistance layer
     *
     * @param hostid Hostid of the Business object requested
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    Business select(String hostid)
        throws DAOConnectionException, DAONoDataException;

    /**
     * Verify if a Business object with the specified hostid exists in
     * the persistance layer
     *
     * @param hostid Hostid of the Business object verified
     * @return true if a Business object with the specified hostid exist; false
     * if no Business object correspond to the specified hostid.
     * @throws DAOConnectionException If a data access error occurs
     */
    boolean exist(String hostid) throws DAOConnectionException;

    /**
     * Insert the specified Business object in the persistance layer
     *
     * @param business Business object to insert
     * @throws DAOConnectionException If a data access error occurs
     */
    void insert(Business business) throws DAOConnectionException;

    /**
     * Update the specified Business object in the persistance layer
     *
     * @param business Business object to update
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    void update(Business business)
        throws DAOConnectionException, DAONoDataException;

    /**
     * Remove the specified Business object from the persistance layer
     *
     * @param business Business object to insert
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    void delete(Business business)
        throws DAOConnectionException, DAONoDataException;

    /**
     * Remove all Business objects from the persistance layer
     *
     * @throws DAOConnectionException If a data access error occurs
     */
    void deleteAll() throws DAOConnectionException;

    void close();
}
