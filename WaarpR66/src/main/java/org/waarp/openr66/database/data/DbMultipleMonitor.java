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
package org.waarp.openr66.database.data;

import com.fasterxml.jackson.databind.JsonNode;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.openr66.dao.AbstractDAO;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.MultipleMonitorDAO;
import org.waarp.openr66.dao.database.DBDAOFactory;
import org.waarp.openr66.dao.database.StatementExecutor;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.MultipleMonitor;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.sql.SQLException;
import java.sql.Types;

/**
 * Configuration Table object
 */
public class DbMultipleMonitor extends AbstractDbDataDao<MultipleMonitor> {
  public enum Columns {
    COUNTCONFIG, COUNTHOST, COUNTRULE, HOSTID
  }

  public static final int[] dbTypes =
      { Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.NVARCHAR };

  public static final String table = " MULTIPLEMONITOR ";

  protected static final String selectAllFields =
      Columns.COUNTCONFIG.name() + ',' + Columns.COUNTHOST.name() + ',' +
      Columns.COUNTRULE.name() + ',' + Columns.HOSTID.name();

  @Override
  protected void initObject() {
    // Nothing
  }

  @Override
  protected String getTable() {
    return table;
  }

  @Override
  protected AbstractDAO<MultipleMonitor> getDao(final boolean isCacheable)
      throws DAOConnectionException {
    return DAOFactory.getInstance().getMultipleMonitorDAO(isCacheable);
  }

  @Override
  protected String getPrimaryKey() {
    if (pojo != null) {
      return pojo.getHostid();
    }
    throw new IllegalArgumentException("pojo is null");
  }

  @Override
  protected String getPrimaryField() {
    return Columns.HOSTID.name();
  }

  /**
   * @param hostid
   * @param cc count for Config
   * @param ch count for Host
   * @param cr count for Rule
   */
  public DbMultipleMonitor(final String hostid, final int cc, final int ch,
                           final int cr) throws WaarpDatabaseSqlException {
    pojo = new MultipleMonitor(hostid, cc, ch, cr);
  }

  /**
   * @param hostid
   *
   * @throws WaarpDatabaseException
   */
  public DbMultipleMonitor(final String hostid) throws WaarpDatabaseException {
    MultipleMonitorDAO monitorAccess = null;
    try {
      monitorAccess = DAOFactory.getInstance().getMultipleMonitorDAO(true);
      pojo = monitorAccess.select(hostid);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException("Cannot find " + "MultipleMonitor",
                                             e);
    } finally {
      DBDAOFactory.closeDAO(monitorAccess);
    }
  }

  @Override
  protected void checkValues() throws WaarpDatabaseSqlException {
    pojo.checkValues();
  }

  @Override
  protected void setFromJson(final String field, final JsonNode value) {
    if (value == null) {
      return;
    }
    for (final Columns column : Columns.values()) {
      if (column.name().equalsIgnoreCase(field)) {
        switch (column) {
          case COUNTCONFIG:
            pojo.setCountConfig(value.asInt());
            break;
          case COUNTHOST:
            pojo.setCountHost(value.asInt());
            break;
          case COUNTRULE:
            pojo.setCountRule(value.asInt());
            break;
          case HOSTID:
            pojo.setHostid(value.asText());
            break;
        }
      }
    }
  }

  /**
   * Private constructor for Commander only
   */
  private DbMultipleMonitor() {
    pojo = new MultipleMonitor();
  }

  /**
   * For instance from Commander when getting updated information
   *
   * @param preparedStatement
   *
   * @return the next updated Configuration
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbMultipleMonitor getFromStatement(
      final DbPreparedStatement preparedStatement)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbMultipleMonitor dbMm = new DbMultipleMonitor();
    AbstractDAO<MultipleMonitor> multipleDAO = null;
    try {
      multipleDAO = dbMm.getDao(false);
      dbMm.pojo = ((StatementExecutor<MultipleMonitor>) multipleDAO)
          .getFromResultSet(preparedStatement.getResultSet());
      return dbMm;
    } catch (final SQLException e) {
      DbSession.error(e);
      throw new WaarpDatabaseSqlException("Getting values in error", e);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseSqlException("Getting values in error", e);
    } finally {
      DAOFactory.closeDAO(multipleDAO);
    }
  }

  /**
   * @return the DbPreparedStatement for getting Updated Object in "FOR
   *     UPDATE" mode
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getUpdatedPrepareStament(
      final DbSession session)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbMultipleMonitor multipleMonitor =
        new DbMultipleMonitor(Configuration.configuration.getHostId(), 0, 0, 0);
    try {
      if (!multipleMonitor.exist()) {
        multipleMonitor.insert();
        session.commit();
      }
    } catch (final WaarpDatabaseException ignored) {
      // ignore
    }
    String request = "SELECT " + selectAllFields;
    request += " FROM " + table + " WHERE " + Columns.HOSTID.name() + " = '" +
               Configuration.configuration.getHostId() + '\'' + " FOR UPDATE ";
    return new DbPreparedStatement(session, request);
  }

  /**
   * On Commander side
   *
   * @return True if this is the last update
   */
  public boolean checkUpdateConfig() {
    if (getCountConfig() <= 0) {
      setCountConfig(Configuration.configuration.getMultipleMonitors());
      setCountConfig(getCountConfig() - 1);
      isSaved = false;
    } else {
      setCountConfig(getCountConfig() - 1);
      isSaved = false;
    }
    return getCountConfig() <= 0;
  }

  /**
   * On Commander side
   *
   * @return True if this is the last update
   */
  public boolean checkUpdateHost() {
    if (getCountHost() <= 0) {
      setCountHost(Configuration.configuration.getMultipleMonitors());
      setCountHost(getCountHost() - 1);
      isSaved = false;
    } else {
      setCountHost(getCountHost() - 1);
      isSaved = false;
    }
    return getCountHost() <= 0;
  }

  /**
   * On Commander side
   *
   * @return True if this is the last update
   */
  public boolean checkUpdateRule() {
    if (getCountRule() <= 0) {
      setCountRule(Configuration.configuration.getMultipleMonitors());
      setCountRule(getCountRule() - 1);
      isSaved = false;
    } else {
      setCountRule(getCountRule() - 1);
      isSaved = false;
    }
    return getCountRule() <= 0;
  }

  @Override
  public void changeUpdatedInfo(final UpdatedInfo info) {
    // ignore
  }

  /**
   * return the String representation
   */
  @Override
  public String toString() {
    return "DbMM " + getCountConfig() + ':' + getCountHost() + ':' +
           getCountRule();
  }

  /**
   * @return the countConfig
   */
  public int getCountConfig() {
    return pojo.getCountConfig();
  }

  /**
   * @param countConfig the countConfig to set
   */
  private void setCountConfig(final int countConfig) {
    isSaved = false;
    pojo.setCountConfig(countConfig);
  }

  /**
   * @return the countHost
   */
  public int getCountHost() {
    return pojo.getCountHost();
  }

  /**
   * @param countHost the countHost to set
   */
  private void setCountHost(final int countHost) {
    isSaved = false;
    pojo.setCountHost(countHost);
  }

  /**
   * @return the countRule
   */
  public int getCountRule() {
    return pojo.getCountRule();
  }

  /**
   * @param countRule the countRule to set
   */
  private void setCountRule(final int countRule) {
    isSaved = false;
    pojo.setCountRule(countRule);
  }
}
