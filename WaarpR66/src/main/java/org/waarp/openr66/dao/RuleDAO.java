package org.waarp.openr66.dao;

import java.util.List;

import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Rule;

/**
 * Interface to interact with Rule objects in the persistance layer
 */
public interface RuleDAO {

    /**
     * Retrieve all Rule objects in a List from the persistance layer
     *
     * @throws DAOConnectionException If data access error occurs
     */
    List<Rule> getAll() throws DAOConnectionException;

    /**
     * Retrieve all Rule objects corresponding to the given filters
     * a List from the persistance layer
     *
     * @param filters List of filter
     * @throws DAOConnectionException If data access error occurs
     */
    List<Rule> find(List<Filter> filters) throws DAOConnectionException;

    /**
     * Retrieve the Rule object with the specified Rulename from the
     * persistance layer
     *
     * @param rulename rulename of the Rule object requested
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    Rule select(String rulename)
        throws DAOConnectionException, DAONoDataException;

    /**
     * Verify if a Rule object with the specified Rulename exists in
     * the persistance layer
     *
     * @param rulename rulename of the Rule object verified
     * @return true if a Rule object with the specified Rulename exist; false
     * if no Rule object correspond to the specified Rulename.
     * @throws DAOConnectionException If a data access error occurs
     */
    boolean exist(String rulename) throws DAOConnectionException;

    /**
     * Insert the specified Rule object in the persistance layer
     *
     * @param rule Rule object to insert
     * @throws DAOConnectionException If a data access error occurs
     */
    void insert(Rule rule) throws DAOConnectionException;

    /**
     * Update the specified Rule object in the persistance layer
     *
     * @param rule Rule object to update
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    void update(Rule rule) throws DAOConnectionException, DAONoDataException;

    /**
     * Remove the specified Rule object from the persistance layer
     *
     * @param rule Rule object to insert
     * @throws DAOConnectionException If a data access error occurs
     * @throws  DAONoDataException if no data are available
     */
    void delete(Rule rule) throws DAOConnectionException, DAONoDataException;

    /**
     * Remove all Rule objects from the persistance layer
     *
     * @throws DAOConnectionException If a data access error occurs
     */
    void deleteAll() throws DAOConnectionException;

    void close();
}
