package org.waarp.openr66.dao;

import java.util.List;

import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.MultipleMonitor;

/**
 * Interface to interact with MultipleMonitor objects in the persistance layer
 */
public interface MultipleMonitorDAO {

    /**
     * Retrieve all MultipleMonitor objects in a List from the persistance layer
     *
     * @throws DAOConnectionException If data access error occurs
     */
    List<MultipleMonitor> getAll() throws DAOConnectionException;

    /**
     * Retrieve all MultipleMonitor objects corresponding to the given filters
     * in a List from the persistance layer
     *
     * @param filters List of filter
     * @throws DAOConnectionException If data access error occurs
     */
    List<MultipleMonitor> find(List<Filter> filters) throws
                                                     DAOConnectionException;

    /**
     * Retrieve the MultipleMonitor object with the specified hostid from the persistance layer
     *
     * @param hostid Hostid of the MultipleMonitor object requested
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    MultipleMonitor select(String hostid)
        throws DAOConnectionException, DAONoDataException;

    /**
     * Verify if a MultipleMonitor object with the specified hostid exists in
     * the persistance layer
     *
     * @param hostid Hostid of the MultipleMonitor object verified
     * @return true if a MultipleMonitor object with the specified hostid exist; false
     * if no MultipleMonitor object correspond to the specified hostid.
     * @throws DAOConnectionException If a data access error occurs
     */
    boolean exist(String hostid) throws DAOConnectionException;

    /**
     * Insert the specified MultipleMonitor object in the persistance layer
     *
     * @param multipleMonitor MultipleMonitor object to insert
     * @throws DAOConnectionException If a data access error occurs
     */
    void insert(MultipleMonitor multipleMonitor) throws DAOConnectionException;

    /**
     * Update the specified MultipleMonitor object in the persistance layer
     *
     * @param multipleMonitor MultipleMonitor object to update
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    void update(MultipleMonitor multipleMonitor)
        throws DAOConnectionException, DAONoDataException;

    /**
     * Remove the specified MultipleMonitor object from the persistance layer
     *
     * @param multipleMonitor MultipleMonitor object to insert
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    void delete(MultipleMonitor multipleMonitor)
        throws DAOConnectionException, DAONoDataException;

    /**
     * Remove all MultipleMonitor objects from the persistance layer
     *
     * @throws DAOConnectionException If a data access error occurs
     */
    void deleteAll() throws DAOConnectionException;

    void close();
}
