package org.waarp.openr66.dao.database;

import java.sql.SQLException;

import org.waarp.common.database.ConnectionFactory;
import org.waarp.common.database.properties.*;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.database.h2.H2TransferDAO;
import org.waarp.openr66.dao.database.mariadb.MariaDBTransferDAO;
import org.waarp.openr66.dao.database.oracle.OracleTransferDAO;
import org.waarp.openr66.dao.database.postgres.PostgreSQLTransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;

/**
 * DAOFactory for standard SQL databases
 */
public class DBDAOFactory extends DAOFactory {

    private static WaarpLogger logger = WaarpLoggerFactory.getLogger(DBDAOFactory.class);

    private ConnectionFactory connectionFactory;

    public DBDAOFactory(ConnectionFactory factory) { 
        this.connectionFactory = factory;
    }

    @Override
    public DBBusinessDAO getBusinessDAO() throws DAOConnectionException {
        try {
            return new DBBusinessDAO(connectionFactory.getConnection());
        } catch (SQLException e) {
            throw new DAOConnectionException("data access error", e);
        }
    }

    @Override
    public DBHostDAO getHostDAO() throws DAOConnectionException {
        try {
            return new DBHostDAO(connectionFactory.getConnection());
        } catch (SQLException e) {
            throw new DAOConnectionException("data access error", e);
        }
    }

    @Override
    public DBLimitDAO getLimitDAO() throws DAOConnectionException {
        try {
            return new DBLimitDAO(connectionFactory.getConnection());
        } catch (SQLException e) {
            throw new DAOConnectionException("data access error", e);
        }
    }

    @Override
    public DBMultipleMonitorDAO getMultipleMonitorDAO() throws
                                                        DAOConnectionException {
        try {
            return new DBMultipleMonitorDAO(connectionFactory.getConnection());
        } catch (SQLException e) {
            throw new DAOConnectionException("data access error", e);
        }
    }

    @Override
    public DBRuleDAO getRuleDAO() throws DAOConnectionException {
        try {
            return new DBRuleDAO(connectionFactory.getConnection());
        } catch (SQLException e) {
            throw new DAOConnectionException("data access error", e);
        }
    }

    @Override
    public DBTransferDAO getTransferDAO() throws DAOConnectionException {
        try {
	     DbProperties prop = connectionFactory.getProperties();
	     if (prop instanceof H2Properties) {
                 return new H2TransferDAO(connectionFactory.getConnection());
	     } else if (prop instanceof MariaDBProperties) {
                 return new MariaDBTransferDAO(connectionFactory.getConnection());
	     } else if (prop instanceof MySQLProperties) {
                 return new MariaDBTransferDAO(connectionFactory.getConnection());
	     } else if (prop instanceof OracleProperties) {
                 return new OracleTransferDAO(connectionFactory.getConnection());
	     } else if (prop instanceof PostgreSQLProperties) {
                 return new PostgreSQLTransferDAO(connectionFactory.getConnection());
	     } else {
	         throw new DAOConnectionException("Unsupported database");
	     }
        } catch (SQLException e) {
            throw new DAOConnectionException("data access error", e);
        }
    }

    /**
     * Close the DBDAOFactory and close the ConnectionFactory
     * Warning: You need to close the Connection yourself!
     */
    public void close() {
        logger.debug("Closing DAOFactory.");
        logger.debug("Closing factory ConnectionFactory.");
        connectionFactory.close();
    }
}

