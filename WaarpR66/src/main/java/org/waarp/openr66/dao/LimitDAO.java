package org.waarp.openr66.dao;

import java.util.List;

import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Limit;

/**
 * Interface to interact with Limit objects in the persistance layer
 */
public interface LimitDAO {

    /**
     * Retrieve all Limit objects in a List from the persistance layer
     *
     * @throws DAOConnectionException If data access error occurs
     */
    List<Limit> getAll() throws DAOConnectionException;

    /**
     * Retrieve all Limit objects correspondig to the given filters
     * in a List from the persistance layer
     *
     * @param filters List of filter
     * @throws DAOConnectionException If data access error occurs
     */
    List<Limit> find(List<Filter> filters) throws DAOConnectionException;
    /**
     * Retrieve the Limit object with the specified hostid from the persistance layer
     *
     * @param hostid Hostid of the Limit object requested
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    Limit select(String hostid)
        throws DAOConnectionException, DAONoDataException;

    /**
     * Verify if a Limit object with the specified hostid exists in
     * the persistance layer
     *
     * @param hostid Hostid of the Limit object verified
     * @return true if a Limit object with the specified hostid exist; false
     * if no Limit object correspond to the specified hostid.
     * @throws DAOConnectionException If a data access error occurs
     */
    boolean exist(String hostid) throws DAOConnectionException;

    /**
     * Insert the specified Limit object in the persistance layer
     *
     * @param limit Limit object to insert
     * @throws DAOConnectionException If a data access error occurs
     */
    void insert(Limit limit) throws DAOConnectionException;

    /**
     * Update the specified Limit object in the persistance layer
     *
     * @param limit Limit object to update
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    void update(Limit limit) throws DAOConnectionException, DAONoDataException;

    /**
     * Remove the specified Limit object from the persistance layer
     *
     * @param limit Limit object to insert
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    void delete(Limit limit) throws DAOConnectionException, DAONoDataException;

    /**
     * Remove all Limit objects from the persistance layer
     *
     * @throws DAOConnectionException If a data access error occurs
     */
    void deleteAll() throws DAOConnectionException;

    void close();
}
