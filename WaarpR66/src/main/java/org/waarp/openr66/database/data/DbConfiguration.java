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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.openr66.dao.AbstractDAO;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.LimitDAO;
import org.waarp.openr66.dao.database.DBLimitDAO;
import org.waarp.openr66.dao.database.StatementExecutor;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Limit;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration Table object
 */
public class DbConfiguration extends AbstractDbDataDao<Limit> {

  public enum Columns {
    READGLOBALLIMIT, WRITEGLOBALLIMIT, READSESSIONLIMIT, WRITESESSIONLIMIT,
    DELAYLIMIT, UPDATEDINFO, HOSTID
  }

  public static final int[] dbTypes = {
      Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.BIGINT,
      Types.INTEGER, Types.NVARCHAR
  };

  public static final String table = " CONFIGURATION ";

  // ALL TABLE SHOULD IMPLEMENT THIS
  public static final int NBPRKEY = 1;

  protected static final String selectAllFields =
      Columns.READGLOBALLIMIT.name() + ',' + Columns.WRITEGLOBALLIMIT.name() +
      ',' + Columns.READSESSIONLIMIT.name() + ',' +
      Columns.WRITESESSIONLIMIT.name() + ',' + Columns.DELAYLIMIT.name() + ',' +
      Columns.UPDATEDINFO.name() + ',' + Columns.HOSTID.name();

  protected static final String updateAllFields =
      Columns.READGLOBALLIMIT.name() + "=?," + Columns.WRITEGLOBALLIMIT.name() +
      "=?," + Columns.READSESSIONLIMIT.name() + "=?," +
      Columns.WRITESESSIONLIMIT.name() + "=?," + Columns.DELAYLIMIT.name() +
      "=?," + Columns.UPDATEDINFO.name() + "=?";

  protected static final String insertAllValues = " (?,?,?,?,?,?,?) ";

  @Override
  protected String getTable() {
    return table;
  }

  @Override
  protected AbstractDAO<Limit> getDao() throws DAOConnectionException {
    return DAOFactory.getInstance().getLimitDAO();
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
   * @param rg Read Global Limit
   * @param wg Write Global Limit
   * @param rs Read Session Limit
   * @param ws Write Session Limit
   * @param del Delay Limit
   */
  public DbConfiguration(String hostid, long rg, long wg, long rs, long ws,
                         long del) {
    pojo = new Limit(hostid, rg, wg, rs, ws, del);
  }

  public DbConfiguration(Limit limit) {
    if (limit == null) {
      throw new IllegalArgumentException(
          "Argument in constructor cannot be null");
    }
    this.pojo = limit;
  }

  /**
   * Constructor from Json
   *
   * @param source
   *
   * @throws WaarpDatabaseSqlException
   */
  public DbConfiguration(ObjectNode source) throws WaarpDatabaseSqlException {
    pojo = new Limit();
    setFromJson(source, false);
    if (pojo.getHostid() == null || pojo.getHostid().isEmpty()) {
      throw new WaarpDatabaseSqlException(
          "Not enough argument to create the object");
    }
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
    if (pojo.getHostid() == null || pojo.getHostid().isEmpty()) {
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
    LimitDAO limitAccess = null;
    try {
      limitAccess = DAOFactory.getInstance().getLimitDAO();
      pojo = limitAccess.select(hostid);
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } catch (final DAONoDataException e) {
      pojo = new Limit(hostid, 0L);
    } finally {
      DAOFactory.closeDAO(limitAccess);
    }
  }

  /**
   * Private constructor for Commander only
   */
  private DbConfiguration() {
    pojo = new Limit();
  }

  @Override
  protected void setFromJson(final String field, final JsonNode value) {
    if (value == null) {
      return;
    }
    for (Columns column : Columns.values()) {
      if (column.name().equalsIgnoreCase(field)) {
        switch (column) {
          case READGLOBALLIMIT:
            pojo.setReadGlobalLimit(value.asLong());
            break;
          case WRITEGLOBALLIMIT:
            pojo.setWriteGlobalLimit(value.asLong());
            break;
          case READSESSIONLIMIT:
            pojo.setReadSessionLimit(value.asLong());
            break;
          case WRITESESSIONLIMIT:
            pojo.setWriteSessionLimit(value.asLong());
            break;
          case DELAYLIMIT:
            pojo.setDelayLimit(value.asLong());
            break;
          case UPDATEDINFO:
            pojo.setUpdatedInfo(
                org.waarp.openr66.pojo.UpdatedInfo.valueOf(value.asInt()));
            break;
          case HOSTID:
            pojo.setHostid(value.asText());
            break;
        }
      }
    }
  }

  /**
   * @return the array of DbConfiguration from Updated status
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbConfiguration[] getUpdatedPrepareStament()
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final List<Filter> filters = new ArrayList<Filter>();
    filters.add(new Filter(DBLimitDAO.HOSTID_FIELD, "=",
                           Configuration.configuration.getHostId()));
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
      DAOFactory.closeDAO(limitAccess);
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
            Columns.WRITESESSIONLIMIT + " > " + limitBandwith + ')';
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

  public static DbConfiguration getFromStatement(
      final DbPreparedStatement statement)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbConfiguration dbConfiguration = new DbConfiguration();
    AbstractDAO<Limit> limitDAO = null;
    try {
      limitDAO = dbConfiguration.getDao();
      dbConfiguration.pojo = ((StatementExecutor<Limit>) limitDAO)
          .getFromResultSet(statement.getResultSet());
      return dbConfiguration;
    } catch (SQLException e) {
      DbSession.error(e);
      throw new WaarpDatabaseSqlException("Getting values in error", e);
    } catch (DAOConnectionException e) {
      throw new WaarpDatabaseSqlException("Getting values in error", e);
    } finally {
      DAOFactory.closeDAO(limitDAO);
    }
  }

  @Override
  public void changeUpdatedInfo(UpdatedInfo info) {
    pojo.setUpdatedInfo(org.waarp.openr66.pojo.UpdatedInfo.fromLegacy(info));
  }

  /**
   * Update configuration according to new value of limits
   */
  public void updateConfiguration() {
    Configuration.configuration.changeNetworkLimit(pojo.getWriteGlobalLimit(),
                                                   pojo.getReadGlobalLimit(),
                                                   pojo.getWriteSessionLimit(),
                                                   pojo.getReadSessionLimit(),
                                                   pojo.getDelayLimit());
  }

  /**
   * @return True if this Configuration refers to the current host
   */
  public boolean isOwnConfiguration() {
    return pojo.getHostid().equals(Configuration.configuration.getHostId());
  }

}
