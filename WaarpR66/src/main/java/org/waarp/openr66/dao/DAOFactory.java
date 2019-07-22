package org.waarp.openr66.dao;

import org.waarp.common.database.ConnectionFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.openr66.dao.database.DBDAOFactory;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.xml.XMLDAOFactory;


/**
 * Abstract class to create DAOFactory
 */
public abstract class DAOFactory {

    private static DAOFactory instance;

    public static void initialize() {
        if (instance == null) {
            instance = new XMLDAOFactory();
        }
    }

    public static void initialize(ConnectionFactory factory) {
        if (instance == null) {
            instance = new DBDAOFactory(factory);
        }
    }

    public static DAOFactory getInstance() {
        return instance;
    }

    /**
     * Return a BusinessDAO
     *
     * @return a ready to use BusinessDAO
     * @throws DAOConnectionException if cannot create the DAO
     */
    public abstract BusinessDAO getBusinessDAO() throws DAOConnectionException;

    /**
     * Return a HostDAO
     *
     * @return a ready to use HostDAO
     * @throws DAOConnectionException if cannot create the DAO
     */
    public abstract HostDAO getHostDAO() throws DAOConnectionException;

    /**
     * Return a LimitDAO
     *
     * @return a ready to use LimitDAO
     * @throws DAOConnectionException if cannot create the DAO
     */
    public abstract LimitDAO getLimitDAO() throws DAOConnectionException;

    /**
     * Return a MultipleMonitorDAO
     *
     * @return a ready to use MultipleMonitorDAO
     * @throws DAOConnectionException if cannot create the DAO
     */
    public abstract MultipleMonitorDAO getMultipleMonitorDAO()
        throws DAOConnectionException;

    /**
     * Return a RuleDAO
     *
     * @return a ready to use RuleDAO
     * @throws DAOConnectionException if cannot create the DAO
     */
    public abstract RuleDAO getRuleDAO() throws DAOConnectionException;

    /**
     * Return a TransferDAO
     *
     * @return a ready to use TramsferDAO
     * @throws DAOConnectionException if cannot create the DAO
     */
    public abstract TransferDAO getTransferDAO() throws DAOConnectionException;
}
