package org.waarp.openr66.dao;

import java.util.List;

import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Host;

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
     * Retrieve all Host objects corresponding to the given filters
     * in a List from the persistance lsayer
     *
     * @param filters List of filter
     * @throws DAOConnectionException If data access error occurs
     */
    List<Host> find(List<Filter> filters) throws DAOConnectionException;
    /**
     * Retrieve the Host object with the specified hostid from the persistance layer
     *
     * @param hostid Hostid of the Host object requested
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    Host select(String hostid) throws DAOConnectionException,
                                      DAONoDataException;

    /**
     * Verify if a Host object with the specified hostid exists in
     * the persistance layer
     *
     * @param hostid Hostid of the Host object verified
     * @return true if a Host object with the specified hostid exist; false
     * if no Host object correspond to the specified hostid.
     * @throws DAOConnectionException If a data access error occurs
     */
    boolean exist(String hostid) throws DAOConnectionException;

    /**
     * Insert the specified Host object in the persistance layer
     *
     * @param host Host object to insert
     * @throws DAOConnectionException If a data access error occurs
     */
    void insert(Host host) throws DAOConnectionException;

    /**
     * Update the specified Host object in the persistance layer
     *
     * @param host Host object to update
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    void update(Host host) throws DAOConnectionException, DAONoDataException;

    /**
     * Remove the specified Host object from the persistance layer
     *
     * @param host Host object to insert
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
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
