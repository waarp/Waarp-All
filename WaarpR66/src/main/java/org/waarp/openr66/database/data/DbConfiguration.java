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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.data.DbValue;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.openr66.commander.CommanderNoDb;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.LimitDAO;
import org.waarp.openr66.dao.database.DBLimitDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Limit;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration Table object
 *
 *
 */
public class DbConfiguration extends AbstractDbData {
  public static enum Columns {
    READGLOBALLIMIT, WRITEGLOBALLIMIT, READSESSIONLIMIT, WRITESESSIONLIMIT,
    DELAYLIMIT, UPDATEDINFO, HOSTID
  }

  public static final int[] dbTypes = {
      Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.BIGINT,
      Types.INTEGER, Types.NVARCHAR
  };

  public static final String table = " CONFIGURATION ";

  private Limit limit = null;

  // ALL TABLE SHOULD IMPLEMENT THIS
  public static final int NBPRKEY = 1;

  protected static final String selectAllFields =
      Columns.READGLOBALLIMIT.name() + "," + Columns.WRITEGLOBALLIMIT.name() +
      "," + Columns.READSESSIONLIMIT.name() + "," +
      Columns.WRITESESSIONLIMIT.name() + "," + Columns.DELAYLIMIT.name() + "," +
      Columns.UPDATEDINFO.name() + "," + Columns.HOSTID.name();

  protected static final String updateAllFields =
      Columns.READGLOBALLIMIT.name() + "=?," + Columns.WRITEGLOBALLIMIT.name() +
      "=?," + Columns.READSESSIONLIMIT.name() + "=?," +
      Columns.WRITESESSIONLIMIT.name() + "=?," + Columns.DELAYLIMIT.name() +
      "=?," + Columns.UPDATEDINFO.name() + "=?";

  protected static final String insertAllValues = " (?,?,?,?,?,?,?) ";

  @Override
  protected void initObject() {
    primaryKey = new DbValue[] { new DbValue("", Columns.HOSTID.name()) };
    otherFields = new DbValue[] {
        new DbValue(0l, Columns.READGLOBALLIMIT.name()),
        new DbValue(0l, Columns.WRITEGLOBALLIMIT.name()),
        new DbValue(0l, Columns.READSESSIONLIMIT.name()),
        new DbValue(0l, Columns.WRITESESSIONLIMIT.name()),
        new DbValue(0l, Columns.DELAYLIMIT.name()),
        new DbValue(0, Columns.UPDATEDINFO.name())
    };
    allFields = new DbValue[] {
        otherFields[0], otherFields[1], otherFields[2], otherFields[3],
        otherFields[4], otherFields[5], primaryKey[0]
    };

  }

  @Override
  protected String getSelectAllFields() {
    return selectAllFields;
  }

  @Override
  protected String getTable() {
    return table;
  }

  @Override
  protected String getInsertAllValues() {
    return insertAllValues;
  }

  @Override
  protected String getUpdateAllFields() {
    return updateAllFields;
  }

  @Override
  protected void setToArray() {
    allFields[Columns.HOSTID.ordinal()].setValue(limit.getHostid());
    allFields[Columns.READGLOBALLIMIT.ordinal()]
        .setValue(limit.getReadGlobalLimit());
    allFields[Columns.WRITEGLOBALLIMIT.ordinal()]
        .setValue(limit.getWriteGlobalLimit());
    allFields[Columns.READSESSIONLIMIT.ordinal()]
        .setValue(limit.getReadSessionLimit());
    allFields[Columns.WRITESESSIONLIMIT.ordinal()]
        .setValue(limit.getWriteSessionLimit());
    allFields[Columns.DELAYLIMIT.ordinal()].setValue(limit.getDelayLimit());
    allFields[Columns.UPDATEDINFO.ordinal()]
        .setValue(limit.getUpdatedInfo().ordinal());
  }

  @Override
  protected void setFromArray() throws WaarpDatabaseSqlException {
    limit.setHostid((String) allFields[Columns.HOSTID.ordinal()].getValue());
    limit.setReadGlobalLimit(
        (Long) allFields[Columns.READGLOBALLIMIT.ordinal()].getValue());
    limit.setWriteGlobalLimit(
        (Long) allFields[Columns.WRITEGLOBALLIMIT.ordinal()].getValue());
    limit.setReadSessionLimit(
        (Long) allFields[Columns.READSESSIONLIMIT.ordinal()].getValue());
    limit.setWriteSessionLimit(
        (Long) allFields[Columns.WRITESESSIONLIMIT.ordinal()].getValue());
    limit.setDelayLimit(
        (Long) allFields[Columns.DELAYLIMIT.ordinal()].getValue());
    limit.setUpdatedInfo(org.waarp.openr66.pojo.UpdatedInfo.valueOf(
        (Integer) allFields[Columns.UPDATEDINFO.ordinal()].getValue()));
  }

  @Override
  protected String getWherePrimaryKey() {
    return primaryKey[0].getColumn() + " = ? ";
  }

  @Override
  protected void setPrimaryKey() {
    primaryKey[0].setValue(limit.getHostid());
  }

  /**
   * @param hostid
   * @param rg Read Global Limit
   * @param wg Write Global Limit
   * @param rs Read Session Limit
   * @param ws Write Session Limit
   * @param del Delay Limit
   */
  public DbConfiguration(String hostid, long rg, long wg, long rs, long ws,
                         long del) {
    super();
    limit = new Limit(hostid, rg, wg, rs, ws, del);
    setToArray();
  }

  public DbConfiguration(Limit limit) {
    super();
    if (limit == null) {
      throw new IllegalArgumentException(
          "Argument in constructor cannot be null");
    }
    this.limit = limit;
    setToArray();
  }

  /**
   * Constructor from Json
   *
   * @param source
   *
   * @throws WaarpDatabaseSqlException
   */
  public DbConfiguration(ObjectNode source) throws WaarpDatabaseSqlException {
    super();
    limit = new Limit();
    setFromJson(source, false);
    if (limit.getHostid() == null || limit.getHostid().isEmpty()) {
      throw new WaarpDatabaseSqlException(
          "Not enough argument to create the object");
    }
    setToArray();
  }

  /**
   * Read json object into Array then setFromArray
   *
   * @param node
   * @param ignorePrimaryKey
   *
   * @throws WaarpDatabaseSqlException
   */
  @Override
  public void setFromJson(ObjectNode node, boolean ignorePrimaryKey)
      throws WaarpDatabaseSqlException {
    super.setFromJson(node, ignorePrimaryKey);
    if (limit.getHostid() == null || limit.getHostid().isEmpty()) {
      throw new WaarpDatabaseSqlException(
          "Not enough argument to create the object");
    }
  }

  /**
   * @param hostid
   *
   * @throws WaarpDatabaseException
   */
  public DbConfiguration(String hostid) throws WaarpDatabaseException {
    super();
    LimitDAO limitAccess = null;
    try {
      limitAccess = DAOFactory.getInstance().getLimitDAO();
      limit = limitAccess.select(hostid);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      limit = new Limit(hostid, 0l);
    } finally {
      if (limitAccess != null) {
        limitAccess.close();
      }
    }
    setToArray();
  }

  @Override
  public void delete() throws WaarpDatabaseException {
    LimitDAO limitAccess = null;
    try {
      limitAccess = DAOFactory.getInstance().getLimitDAO();
      limitAccess.delete(limit);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException("Cannot find " + "Configuration",
                                             e);
    } finally {
      if (limitAccess != null) {
        limitAccess.close();
      }
    }
  }

  @Override
  public void insert() throws WaarpDatabaseException {
    LimitDAO limitAccess = null;
    if (limit.getUpdatedInfo().equals(UpdatedInfo.TOSUBMIT)) {
      CommanderNoDb.todoList.add(this);
    }
    try {
      limitAccess = DAOFactory.getInstance().getLimitDAO();
      limitAccess.insert(limit);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } finally {
      if (limitAccess != null) {
        limitAccess.close();
      }
    }
  }

  @Override
  public boolean exist() throws WaarpDatabaseException {
    LimitDAO limitAccess = null;
    try {
      limitAccess = DAOFactory.getInstance().getLimitDAO();
      return limitAccess.exist(limit.getHostid());
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } finally {
      if (limitAccess != null) {
        limitAccess.close();
      }
    }
  }

  @Override
  public void select() throws WaarpDatabaseException {
    LimitDAO limitAccess = null;
    try {
      limitAccess = DAOFactory.getInstance().getLimitDAO();
      limit = limitAccess.select(limit.getHostid());
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException("Cannot find " + "Configuration",
                                             e);
    } finally {
      if (limitAccess != null) {
        limitAccess.close();
      }
    }
  }

  @Override
  public void update() throws WaarpDatabaseException {
    LimitDAO limitAccess = null;
    if (limit.getUpdatedInfo().equals(UpdatedInfo.TOSUBMIT)) {
      CommanderNoDb.todoList.add(this);
    }
    try {
      limitAccess = DAOFactory.getInstance().getLimitDAO();
      limitAccess.update(limit);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      throw new WaarpDatabaseNoDataException("Cannot find " + "Configuration",
                                             e);
    } finally {
      if (limitAccess != null) {
        limitAccess.close();
      }
    }
  }

  /**
   * Private constructor for Commander only
   */
  private DbConfiguration() {
    super();
    limit = new Limit();
  }

  public static DbConfiguration getFromStatement(
      DbPreparedStatement preparedStatement)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbConfiguration dbConfiguration = new DbConfiguration();
    dbConfiguration.getValues(preparedStatement, dbConfiguration.allFields);
    dbConfiguration.setToArray();
    dbConfiguration.isSaved = true;
    return dbConfiguration;
  }

  /**
   * @return the DbPreparedStatement for getting Updated Object
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbConfiguration[] getUpdatedPrepareStament()
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final List<Filter> filters = new ArrayList<Filter>();
    filters.add(new Filter(DBLimitDAO.HOSTID_FIELD, "=",
                           Configuration.configuration.getHOST_ID()));
    filters.add(new Filter(DBLimitDAO.UPDATED_INFO_FIELD, "=",
                           UpdatedInfo.TOSUBMIT.ordinal()));

    LimitDAO limitAccess = null;
    List<Limit> limits;
    try {
      limitAccess = DAOFactory.getInstance().getLimitDAO();
      limits = limitAccess.find(filters);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } finally {
      if (limitAccess != null) {
        limitAccess.close();
      }
    }
    final DbConfiguration[] res = new DbConfiguration[limits.size()];
    int i = 0;
    for (final Limit limit : limits) {
      res[i] = new DbConfiguration(limit);
      i++;
    }
    return res;
  }

  /**
   * @param session
   * @param hostid
   * @param limitBandwith 0 for no limit, > 0 for one limit, < 0 for
   *     no
   *     filter
   *
   * @return the preparedStatement with the filter
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getFilterPrepareStament(DbSession session,
                                                            String hostid,
                                                            long limitBandwith)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(session);
    final String request = "SELECT " + selectAllFields + " FROM " + table;
    String condition = null;
    if (hostid != null) {
      condition =
          " WHERE " + Columns.HOSTID.name() + " LIKE '%" + hostid + "%' ";
    }
    if (limitBandwith >= 0) {
      if (condition == null) {
        condition = " WHERE ";
      } else {
        condition += " AND ";
      }
      if (limitBandwith == 0) {
        condition += "(" + Columns.READGLOBALLIMIT + " == 0 AND " +
                     Columns.READSESSIONLIMIT + " == 0 AND " +
                     Columns.WRITEGLOBALLIMIT + " == 0 AND " +
                     Columns.WRITESESSIONLIMIT + " == 0)";
      } else {
        condition +=
            "(" + Columns.READGLOBALLIMIT + " > " + limitBandwith + " OR " +
            Columns.READSESSIONLIMIT + " > " + limitBandwith + " OR " +
            Columns.WRITEGLOBALLIMIT + " > " + limitBandwith + " OR " +
            Columns.WRITESESSIONLIMIT + " > " + limitBandwith + ")";
      }
    }
    if (condition != null) {
      preparedStatement.createPrepareStatement(
          request + condition + " ORDER BY " + Columns.HOSTID.name());
    } else {
      preparedStatement.createPrepareStatement(
          request + " ORDER BY " + Columns.HOSTID.name());
    }
    return preparedStatement;
  }

  @Override
  public void changeUpdatedInfo(UpdatedInfo info) {
    limit.setUpdatedInfo(org.waarp.openr66.pojo.UpdatedInfo.fromLegacy(info));
  }

  /**
   * Update configuration according to new value of limits
   */
  public void updateConfiguration() {
    Configuration.configuration.changeNetworkLimit(limit.getWriteGlobalLimit(),
                                                   limit.getReadGlobalLimit(),
                                                   limit.getWriteSessionLimit(),
                                                   limit.getReadSessionLimit(),
                                                   limit.getDelayLimit());
  }

  /**
   * @return True if this Configuration refers to the current host
   */
  public boolean isOwnConfiguration() {
    return limit.getHostid().equals(Configuration.configuration.getHOST_ID());
  }

  /**
   * @return the DbValue associated with this table
   */
  public static DbValue[] getAllType() {
    final DbConfiguration item = new DbConfiguration();
    return item.allFields;
  }
}
