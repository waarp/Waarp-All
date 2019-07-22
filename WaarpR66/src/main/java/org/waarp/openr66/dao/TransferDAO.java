package org.waarp.openr66.dao;

import java.util.List;

import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Transfer;

/**
 * Interface to interact with Transfer objects in the persistance layer
 */
public interface TransferDAO {

    /**
     * Retrieve all Transfer objects in a List from the persistance layer
     *
     * @throws DAOConnectionException If data access error occurs
     */
    List<Transfer> getAll() throws DAOConnectionException;

    /**
     * Retrieve all Transfer objects to the given filters
     * in a List from the persistance layer
     *
     * @param filters List of filter
     * @throws DAOConnectionException If data access error occurs
     */
    List<Transfer> find(List<Filter> filters) throws DAOConnectionException;

    /**
     * Retrieve all Transfer objects to the given filters
     * in a List from the persistance layer
     *
     * @param filters List of filter
     * @throws DAOConnectionException If data access error occurs
     */
    List<Transfer> find(List<Filter> filters, int limit) throws
                                                         DAOConnectionException;

    /**
     * Retrieve all Transfer objects to the given filters
     * in a List from the persistance layer
     *
     * @param filters List of filter
     * @throws DAOConnectionException If data access error occurs
     */
    List<Transfer> find(List<Filter> filters, int limit, int offset)
        throws DAOConnectionException;


    /**
     * Retrieve all Transfer objects to the given filters
     * in a List from the persistance layer
     *
     * @param filters List of filter
     * @throws DAOConnectionException If data access error occurs
     */
    List<Transfer> find(List<Filter> filters, String column, boolean ascend)
        throws DAOConnectionException;

    /**
     * Retrieve all Transfer objects to the given filters
     * in a List from the persistance layer
     *
     * @param filters List of filter
     * @throws DAOConnectionException If data access error occurs
     */
    List<Transfer> find(List<Filter> filters, String column, boolean ascend,
                        int limit) throws DAOConnectionException;

    /**
     * Retrieve all Transfer objects to the given filters
     * in a List from the persistance layer
     *
     * @param filters List of filter
     * @throws DAOConnectionException If data access error occurs
     */
    List<Transfer> find(List<Filter> filters, String column, boolean ascend,
                        int limit, int offset) throws DAOConnectionException;


    /**
     * Retrieve the Transfer object with the specified Special ID from the persistance layer
     *
     * @param id special ID of the Transfer object requested
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    Transfer select(long id, String requester, String requested, String owner)
        throws DAOConnectionException, DAONoDataException;

    /**
     * Verify if a Transfer object with the specified Special ID exists in
     * the persistance layer
     *
     * @param id special ID of the Transfer object verified
     * @return true if a Transfer object with the specified Special ID exist; false
     * if no Transfer object correspond to the specified Special ID.
     * @throws DAOConnectionException If a data access error occurs
     */
    boolean exist(long id, String requester, String requested, String owner)
        throws DAOConnectionException;

    /**
     * Insert the specified Transfer object in the persistance layer
     *
     * @param transfer Transfer object to insert
     * @throws DAOConnectionException If a data access error occurs
     */
    void insert(Transfer transfer) throws DAOConnectionException;

    /**
     * Update the specified Transfer object in the persistance layer
     *
     * @param transfer Transfer object to update
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    void update(Transfer transfer)
        throws DAOConnectionException, DAONoDataException;

    /**
     * Remove the specified Transfer object from the persistance layer
     *
     * @param transfer Transfer object to insert
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    void delete(Transfer transfer)
        throws DAOConnectionException, DAONoDataException;

    /**
     * Remove all Transfer objects from the persistance layer
     *
     * @throws DAOConnectionException If a data access error occurs
     */
    void deleteAll() throws DAOConnectionException;

    void close();
}
