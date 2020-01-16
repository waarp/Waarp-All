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

package org.waarp.openr66.dao.database;

import org.waarp.common.database.ConnectionFactory;
import org.waarp.common.database.properties.DbProperties;
import org.waarp.common.database.properties.H2Properties;
import org.waarp.common.database.properties.MariaDBProperties;
import org.waarp.common.database.properties.MySQLProperties;
import org.waarp.common.database.properties.OracleProperties;
import org.waarp.common.database.properties.PostgreSQLProperties;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.database.h2.H2TransferDAO;
import org.waarp.openr66.dao.database.mariadb.MariaDBTransferDAO;
import org.waarp.openr66.dao.database.oracle.OracleTransferDAO;
import org.waarp.openr66.dao.database.postgres.PostgreSQLTransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;

import java.sql.SQLException;

/**
 * DAOFactory for standard SQL databases
 */
public class DBDAOFactory extends DAOFactory {

  private static final String DATA_ACCESS_ERROR = "data access error";

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DBDAOFactory.class);

  private final ConnectionFactory connectionFactory;

  public DBDAOFactory(ConnectionFactory factory) {
    connectionFactory = factory;
  }

  public ConnectionFactory getConnectionFactory() {
    return connectionFactory;
  }

  @Override
  public int getMaxConnections() {
    return connectionFactory.getMaxConnections();
  }

  @Override
  public DBBusinessDAO getBusinessDAO() throws DAOConnectionException {
    try {
      return new DBBusinessDAO(connectionFactory.getConnection());
    } catch (final SQLException e) {
      throw new DAOConnectionException(DATA_ACCESS_ERROR, e);
    }
  }

  @Override
  public DBHostDAO getHostDAO() throws DAOConnectionException {
    try {
      return new DBHostDAO(connectionFactory.getConnection());
    } catch (final SQLException e) {
      throw new DAOConnectionException(DATA_ACCESS_ERROR, e);
    }
  }

  @Override
  public DBLimitDAO getLimitDAO() throws DAOConnectionException {
    try {
      return new DBLimitDAO(connectionFactory.getConnection());
    } catch (final SQLException e) {
      throw new DAOConnectionException(DATA_ACCESS_ERROR, e);
    }
  }

  @Override
  public DBMultipleMonitorDAO getMultipleMonitorDAO()
      throws DAOConnectionException {
    try {
      return new DBMultipleMonitorDAO(connectionFactory.getConnection());
    } catch (final SQLException e) {
      throw new DAOConnectionException(DATA_ACCESS_ERROR, e);
    }
  }

  @Override
  public DBRuleDAO getRuleDAO() throws DAOConnectionException {
    try {
      return new DBRuleDAO(connectionFactory.getConnection());
    } catch (final SQLException e) {
      throw new DAOConnectionException(DATA_ACCESS_ERROR, e);
    }
  }

  @Override
  public DBTransferDAO getTransferDAO() throws DAOConnectionException {
    try {
      final DbProperties prop = connectionFactory.getProperties();
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
    } catch (final SQLException e) {
      throw new DAOConnectionException(DATA_ACCESS_ERROR, e);
    }
  }

  /**
   * Close the DBDAOFactory and close the ConnectionFactory Warning: You need
   * to close the Connection yourself!
   */
  public void close() {
    logger.debug("Closing DAOFactory.");
    logger.debug("Closing factory ConnectionFactory.");
    connectionFactory.close();
  }
}
