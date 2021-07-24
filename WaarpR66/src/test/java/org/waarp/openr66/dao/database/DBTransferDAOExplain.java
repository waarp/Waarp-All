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

import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Transfer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

public class DBTransferDAOExplain extends DBTransferDAO {
  protected static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DBTransferDAOExplain.class);
  private final DBTransferDAO dbTransferDAO;
  private static int rank = 1;
  private int deep = 0;

  public static String replaceArgs(String statement, Object[] params) {
    if (params == null) {
      return statement;
    }
    for (Object object : params) {
      if (object instanceof String) {
        statement =
            statement.replaceFirst("\\?", '\'' + ((String) object) + '\'');
      } else if (object instanceof Boolean) {
        statement =
            statement.replaceFirst("\\?", ((Boolean) object).toString());
      } else if (object instanceof Long) {
        statement = statement.replaceFirst("\\?", ((Long) object).toString());
      } else if (object instanceof Integer) {
        statement =
            statement.replaceFirst("\\?", ((Integer) object).toString());
      } else if (object instanceof Character) {
        statement = statement.replaceFirst("\\?", "'" + object + '\'');
      } else if (object instanceof Byte) {
        statement = statement.replaceFirst("\\?", "" + object);
      } else if (object instanceof Timestamp) {
        statement = statement.replaceFirst("\\?", "'" + object + "'");
      } else {
        statement = statement.replaceFirst("\\?", object.toString());
      }
    }
    return statement;
  }

  public void explainP1(String statement) {
    deep += 2;
    explain(statement);
    deep -= 2;
  }

  public void explain(String statement) {
    int len = 50;
    String lineUp = "\n" + String.join("", Collections.nCopies(len, "#"));
    String lineSub = "\n" + String.join("", Collections.nCopies(len, "="));
    logger.junit(deep, "\nRank: " + rank + lineUp);
    rank++;
    SysErrLogger.FAKE_LOGGER.sysout("Statement: " + statement);
    SysErrLogger.FAKE_LOGGER.sysout(lineSub);
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      ResultSet rs;
      if (statement.startsWith("SELECT")) {
        rs = stmt.executeQuery("EXPLAIN ANALYZE " + statement);
      } else {
        rs = stmt.executeQuery("EXPLAIN " + statement);
      }
      while (rs.next()) {
        SysErrLogger.FAKE_LOGGER.sysout("\t" + rs.getString(1));
      }
      rs.close();
    } catch (SQLException throwables) {
      logger.error(throwables);
    } finally {
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException throwables) {
          // Ignore
        }
      }
    }
    SysErrLogger.FAKE_LOGGER.sysout(lineUp + "\n\n");
  }

  public DBTransferDAOExplain(DBTransferDAO dbTransferDAO) {
    super(dbTransferDAO.connection);
    this.dbTransferDAO = dbTransferDAO;
  }

  public void updateRankExplain(final Transfer e1) {
    explainP1(replaceArgs(getUpdateLimitedRankRequest(),
                          getUpdateLimitedRankValues(e1)));
  }

  public final void updateRankUpdatedInfoStepStatusStopExplain(
      final Transfer e1) throws DAOConnectionException, DAONoDataException {
    explainP1(replaceArgs(getUpdateRankUpdatedInfoStepStatusStopRequest(),
                          getUpdateRankUpdatedInfoStepStatusStopValues(e1)));
  }

  public void deleteExplain(final Transfer transfer)
      throws DAOConnectionException, DAONoDataException {
    explainP1(replaceArgs(getDeleteRequest(), getPrimaryKeyValues(transfer)));
  }

  public void deleteAllExplain() {
    explainP1(getDeleteAllRequest());
  }

  public List<Transfer> getAllExplain() throws DAOConnectionException {
    explainP1(getGetAllRequest());
    return null;
  }

  public List<Transfer> findExplain(final List<Filter> filters)
      throws DAOConnectionException {
    // Create the SQL query
    final StringBuilder query = new StringBuilder(getGetAllRequest());
    if (filters.isEmpty()) {
      explainP1(query.toString());
      return null;
    }
    int len = filters.size();
    for (Filter filter : filters) {
      len += filter.nbAdditionnalParams();
    }
    final Object[] params = new Object[len];
    query.append(" WHERE ");
    String prefix = "";
    int i = 0;
    for (Filter filter : filters) {
      query.append(prefix);
      if (filter.nbAdditionnalParams() > 0) {
        final Object[] objects = (Object[]) filter.append(query);
        for (final Object o : objects) {
          params[i++] = o;
        }
      } else if (filter.nbAdditionnalParams() == 0) {
        params[i] = filter.append(query);
        i++;
      } else {
        filter.append(query);
      }
      prefix = " AND ";
    }
    explainP1(replaceArgs(query.toString(), params));
    return null;
  }

  public List<Transfer> findExplain(final List<Filter> filters, final int limit)
      throws DAOConnectionException {
    // Create the SQL query
    final Object[] params = prepareFindParams(filters);
    final StringBuilder query =
        new StringBuilder(prepareFindQuery(filters, params));
    // Set LIMIT
    if (limit > 0) {
      query.append(" LIMIT ").append(limit);
    }
    explainP1(replaceArgs(query.toString(), params));
    return null;
  }

  public List<Transfer> findExplain(final List<Filter> filters, final int limit,
                                    final int offset)
      throws DAOConnectionException {
    // Create the SQL query
    final Object[] params = prepareFindParams(filters);
    final StringBuilder query =
        new StringBuilder(prepareFindQuery(filters, params));
    // Set LIMIT
    if (limit > 0) {
      query.append(" LIMIT ").append(limit);
    }
    // Set OFFSET
    if (offset > 0) {
      query.append(" OFFSET ").append(offset);
    }
    explainP1(replaceArgs(query.toString(), params));
    return null;
  }

  public List<Transfer> findExplain(final List<Filter> filters,
                                    final String column, final boolean ascend)
      throws DAOConnectionException {
    // Create the SQL query
    final Object[] params = prepareFindParams(filters);
    final StringBuilder query =
        new StringBuilder(prepareFindQuery(filters, params));
    // Set ORDER BY
    if (ParametersChecker.isNotEmpty(column)) {
      query.append(" ORDER BY ").append(column);
      if (!ascend) {
        query.append(" DESC");
      }
    }
    explainP1(replaceArgs(query.toString(), params));
    return null;
  }

  public List<Transfer> findExplain(final List<Filter> filters,
                                    final String column, final boolean ascend,
                                    final int limit)
      throws DAOConnectionException {
    // Create the SQL query
    final Object[] params = prepareFindParams(filters);
    final StringBuilder query =
        new StringBuilder(prepareFindQuery(filters, params));
    // Set ORDER BY
    if (ParametersChecker.isNotEmpty(column)) {
      query.append(" ORDER BY ").append(column);
      if (!ascend) {
        query.append(" DESC");
      }
    }
    // Set LIMIT
    if (limit > 0) {
      query.append(" LIMIT ").append(limit);
    }
    explainP1(replaceArgs(query.toString(), params));
    return null;
  }

  public List<Transfer> findExplain(final List<Filter> filters,
                                    final String column, final boolean ascend,
                                    final int limit, final int offset)
      throws DAOConnectionException {
    // Create the SQL query
    final Object[] params = prepareFindParams(filters);
    final StringBuilder query =
        new StringBuilder(prepareFindQuery(filters, params));
    // Set ORDER BY
    query.append(" ORDER BY ").append(column);
    if (!ascend) {
      query.append(" DESC");
    }
    // Set LIMIT
    if (limit > 0) {
      query.append(" LIMIT ").append(limit);
    }
    // Set OFFSET
    if (offset > 0) {
      query.append(" OFFSET ").append(offset);
    }
    explainP1(replaceArgs(query.toString(), params));
    return null;
  }

  public long countExplain(final List<Filter> filters)
      throws DAOConnectionException {
    // Create the SQL query
    final Object[] params = prepareFindParams(filters);
    final StringBuilder query =
        new StringBuilder(prepareCountQuery(filters, params));
    explainP1(replaceArgs(query.toString(), params));
    return 0;
  }

  public boolean existExplain(final long id, final String requester,
                              final String requested, final String owner)
      throws DAOConnectionException {
    explainP1(replaceArgs(getExistRequest(),
                          getPrimaryKeyValues(id, owner, requester,
                                              requested)));
    return true;
  }

  public final Transfer selectExplain(final long id, final String requester,
                                      final String requested,
                                      final String owner)
      throws DAOConnectionException, DAONoDataException {
    explainP1(replaceArgs(getSelectRequest(),
                          getPrimaryKeyValues(id, owner, requester,
                                              requested)));
    return null;
  }

  protected final long getNextId() throws DAOConnectionException {
    return dbTransferDAO.getNextId();
  }

  public final void insertExplain(final Transfer transfer)
      throws DAOConnectionException {
    Object[] params = getInsertValues(transfer);
    explainP1(replaceArgs(getInsertRequest(), params));
  }

  public final void updateExplain(final Transfer e1)
      throws DAOConnectionException, DAONoDataException {
    Object[] params = getUpdateValues(e1);
    explainP1(replaceArgs(getUpdateRequest(), params));
  }

}
