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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * DAOFactory for standard SQL databases
 */
public class DBDAOFactory extends DAOFactory {

  private static final String DATA_ACCESS_ERROR = "data access error";

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DBDAOFactory.class);

  private final ConnectionFactory connectionFactory;

  private final FakeConnection fakeConnection;

  public DBDAOFactory(final ConnectionFactory factory) {
    connectionFactory = factory;
    fakeConnection = new FakeConnection(connectionFactory);
  }

  public ConnectionFactory getConnectionFactory() {
    return connectionFactory;
  }

  @Override
  public int getMaxConnections() {
    return connectionFactory.getMaxConnections();
  }

  @Override
  public DBBusinessDAO getBusinessDAO(final boolean isCacheable)
      throws DAOConnectionException {
    try {
      if (isCacheable) {
        return new DBBusinessDAO(fakeConnection);
      }
      return new DBBusinessDAO(connectionFactory.getConnection());
    } catch (final SQLException e) {
      throw new DAOConnectionException(DATA_ACCESS_ERROR, e);
    }
  }

  @Override
  public DBHostDAO getHostDAO(final boolean isCacheable)
      throws DAOConnectionException {
    try {
      if (isCacheable) {
        return new DBHostDAO(fakeConnection);
      }
      return new DBHostDAO(connectionFactory.getConnection());
    } catch (final SQLException e) {
      throw new DAOConnectionException(DATA_ACCESS_ERROR, e);
    }
  }

  @Override
  public DBLimitDAO getLimitDAO(final boolean isCacheable)
      throws DAOConnectionException {
    try {
      if (isCacheable) {
        return new DBLimitDAO(fakeConnection);
      }
      return new DBLimitDAO(connectionFactory.getConnection());
    } catch (final SQLException e) {
      throw new DAOConnectionException(DATA_ACCESS_ERROR, e);
    }
  }

  @Override
  public DBMultipleMonitorDAO getMultipleMonitorDAO(final boolean isCacheable)
      throws DAOConnectionException {
    try {
      if (isCacheable) {
        return new DBMultipleMonitorDAO(fakeConnection);
      }
      return new DBMultipleMonitorDAO(connectionFactory.getConnection());
    } catch (final SQLException e) {
      throw new DAOConnectionException(DATA_ACCESS_ERROR, e);
    }
  }

  @Override
  public DBRuleDAO getRuleDAO(final boolean isCacheable)
      throws DAOConnectionException {
    try {
      if (isCacheable) {
        return new DBRuleDAO(fakeConnection);
      }
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

  /**
   * Fake Connection to allow to setup a real connection for cached capability
   * but not using cache due to lack of data in it
   */
  static class FakeConnection implements Connection {
    private final ConnectionFactory connectionFactory;

    public FakeConnection(ConnectionFactory connectionFactory) {
      this.connectionFactory = connectionFactory;
    }

    /**
     * @return a newly created real connection
     *
     * @throws SQLException
     */
    public Connection getRealConnection() throws SQLException {
      return connectionFactory.getConnection();
    }

    @Override
    public Statement createStatement() throws SQLException {
      return null;
    }

    @Override
    public PreparedStatement prepareStatement(final String s)
        throws SQLException {
      return null;
    }

    @Override
    public CallableStatement prepareCall(final String s) throws SQLException {
      return null;
    }

    @Override
    public String nativeSQL(final String s) throws SQLException {
      return null;
    }

    @Override
    public void setAutoCommit(final boolean b) throws SQLException {
      // Emoty
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
      return false;
    }

    @Override
    public void commit() throws SQLException {
      // Emoty
    }

    @Override
    public void rollback() throws SQLException {
      // Emoty
    }

    @Override
    public void close() throws SQLException {
      // Emoty
    }

    @Override
    public boolean isClosed() throws SQLException {
      return false;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
      return null;
    }

    @Override
    public void setReadOnly(final boolean b) throws SQLException {
      // Emoty
    }

    @Override
    public boolean isReadOnly() throws SQLException {
      return false;
    }

    @Override
    public void setCatalog(final String s) throws SQLException {
      // Emoty
    }

    @Override
    public String getCatalog() throws SQLException {
      return null;
    }

    @Override
    public void setTransactionIsolation(final int i) throws SQLException {
      // Emoty
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
      return 0;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
      return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
      // Emoty
    }

    @Override
    public Statement createStatement(final int i, final int i1)
        throws SQLException {
      return null;
    }

    @Override
    public PreparedStatement prepareStatement(final String s, final int i,
                                              final int i1)
        throws SQLException {
      return null;
    }

    @Override
    public CallableStatement prepareCall(final String s, final int i,
                                         final int i1) throws SQLException {
      return null;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
      return null;
    }

    @Override
    public void setTypeMap(final Map<String, Class<?>> map)
        throws SQLException {
      // Emoty
    }

    @Override
    public void setHoldability(final int i) throws SQLException {
      // Emoty
    }

    @Override
    public int getHoldability() throws SQLException {
      return 0;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
      return null;
    }

    @Override
    public Savepoint setSavepoint(final String s) throws SQLException {
      return null;
    }

    @Override
    public void rollback(final Savepoint savepoint) throws SQLException {
      // Emoty
    }

    @Override
    public void releaseSavepoint(final Savepoint savepoint)
        throws SQLException {
      // Emoty
    }

    @Override
    public Statement createStatement(final int i, final int i1, final int i2)
        throws SQLException {
      return null;
    }

    @Override
    public PreparedStatement prepareStatement(final String s, final int i,
                                              final int i1, final int i2)
        throws SQLException {
      return null;
    }

    @Override
    public CallableStatement prepareCall(final String s, final int i,
                                         final int i1, final int i2)
        throws SQLException {
      return null;
    }

    @Override
    public PreparedStatement prepareStatement(final String s, final int i)
        throws SQLException {
      return null;
    }

    @Override
    public PreparedStatement prepareStatement(final String s, final int[] ints)
        throws SQLException {
      return null;
    }

    @Override
    public PreparedStatement prepareStatement(final String s,
                                              final String[] strings)
        throws SQLException {
      return null;
    }

    @Override
    public Clob createClob() throws SQLException {
      return null;
    }

    @Override
    public Blob createBlob() throws SQLException {
      return null;
    }

    @Override
    public NClob createNClob() throws SQLException {
      return null;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
      return null;
    }

    @Override
    public boolean isValid(final int i) throws SQLException {
      return false;
    }

    @Override
    public void setClientInfo(final String s, final String s1)
        throws SQLClientInfoException {
      // Emoty
    }

    @Override
    public void setClientInfo(final Properties properties)
        throws SQLClientInfoException {
      // Emoty
    }

    @Override
    public String getClientInfo(final String s) throws SQLException {
      return null;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
      return null;
    }

    @Override
    public Array createArrayOf(final String s, final Object[] objects)
        throws SQLException {
      return null;
    }

    @Override
    public Struct createStruct(final String s, final Object[] objects)
        throws SQLException {
      return null;
    }

    @Override
    public void setSchema(final String s) throws SQLException {
      // Emoty
    }

    @Override
    public String getSchema() throws SQLException {
      return null;
    }

    @Override
    public void abort(final Executor executor) throws SQLException {
      // Emoty
    }

    @Override
    public void setNetworkTimeout(final Executor executor, final int i)
        throws SQLException {
      // Emoty
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
      return 0;
    }

    @Override
    public <T> T unwrap(final Class<T> aClass) throws SQLException {
      return null;
    }

    @Override
    public boolean isWrapperFor(final Class<?> aClass) throws SQLException {
      return false;
    }
  }
}
