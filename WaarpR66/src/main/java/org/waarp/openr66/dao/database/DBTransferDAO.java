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

import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.pojo.UpdatedInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.waarp.openr66.database.DbConstantR66.*;

/**
 * Implementation of TransferDAO for a standard SQL database
 */
public abstract class DBTransferDAO extends StatementExecutor<Transfer>
    implements TransferDAO {

  private static final String LIMIT2 = " LIMIT ";

  // Table name
  protected static final String TABLE = "runner";

  // Field name
  public static final String ID_FIELD = "specialid";
  public static final String GLOBAL_STEP_FIELD = "globalstep";
  public static final String GLOBAL_LAST_STEP_FIELD = "globallaststep";
  public static final String STEP_FIELD = "step";
  public static final String RANK_FIELD = "rank";
  public static final String STEP_STATUS_FIELD = "stepstatus";
  public static final String RETRIEVE_MODE_FIELD = "retrievemode";
  public static final String FILENAME_FIELD = "filename";
  public static final String IS_MOVED_FIELD = "ismoved";
  public static final String ID_RULE_FIELD = "idrule";
  public static final String BLOCK_SIZE_FIELD = "blocksz";
  public static final String ORIGINAL_NAME_FIELD = "originalname";
  public static final String FILE_INFO_FIELD = "fileinfo";
  public static final String TRANSFER_INFO_FIELD = "transferinfo";
  public static final String TRANSFER_MODE_FIELD = "modetrans";
  public static final String TRANSFER_START_FIELD = "starttrans";
  public static final String TRANSFER_STOP_FIELD = "stoptrans";
  public static final String INFO_STATUS_FIELD = "infostatus";
  public static final String OWNER_REQUEST_FIELD = "ownerreq";
  public static final String REQUESTED_FIELD = "requested";
  public static final String REQUESTER_FIELD = "requester";
  public static final String UPDATED_INFO_FIELD = "updatedInfo";

  // CRUD requests
  protected static final String PRIMARY_KEY_WHERE =
      ID_FIELD + " = ? AND " + REQUESTER_FIELD + " = ? AND " + REQUESTED_FIELD +
      " = ? AND " + OWNER_REQUEST_FIELD + " = ? ";
  protected static final String SQL_DELETE =
      "DELETE FROM " + TABLE + WHERE + PRIMARY_KEY_WHERE;
  protected static final String SQL_DELETE_ALL = "DELETE FROM " + TABLE;
  protected static final String SQL_EXIST =
      "SELECT 1 FROM " + TABLE + WHERE + PRIMARY_KEY_WHERE;
  protected static final String SQL_GET_ALL = "SELECT * FROM " + TABLE;
  protected static final String SQL_INSERT =
      "INSERT INTO " + TABLE + " (" + ID_FIELD + ", " + GLOBAL_STEP_FIELD +
      ", " + GLOBAL_LAST_STEP_FIELD + ", " + STEP_FIELD + ", " + RANK_FIELD +
      ", " + BLOCK_SIZE_FIELD + ", " + TRANSFER_MODE_FIELD + ", " +
      UPDATED_INFO_FIELD + ", " + STEP_STATUS_FIELD + ", " + INFO_STATUS_FIELD +
      ", " + RETRIEVE_MODE_FIELD + ", " + IS_MOVED_FIELD + ", " +
      TRANSFER_START_FIELD + ", " + TRANSFER_STOP_FIELD + ", " +
      OWNER_REQUEST_FIELD + ", " + REQUESTED_FIELD + ", " + REQUESTER_FIELD +
      ", " + ID_RULE_FIELD + ", " + FILENAME_FIELD + ", " +
      ORIGINAL_NAME_FIELD + ", " + FILE_INFO_FIELD + ", " +
      TRANSFER_INFO_FIELD +
      ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
  protected static final String SQL_SELECT =
      "SELECT * FROM " + TABLE + WHERE + PRIMARY_KEY_WHERE;
  protected static final String SQL_UPDATE =
      "UPDATE " + TABLE + " SET " + ID_FIELD + PARAMETER_COMMA +
      GLOBAL_STEP_FIELD + PARAMETER_COMMA + GLOBAL_LAST_STEP_FIELD +
      PARAMETER_COMMA + STEP_FIELD + PARAMETER_COMMA + RANK_FIELD +
      PARAMETER_COMMA + BLOCK_SIZE_FIELD + PARAMETER_COMMA +
      TRANSFER_MODE_FIELD + PARAMETER_COMMA + UPDATED_INFO_FIELD +
      PARAMETER_COMMA + STEP_STATUS_FIELD + PARAMETER_COMMA +
      INFO_STATUS_FIELD + PARAMETER_COMMA + RETRIEVE_MODE_FIELD +
      PARAMETER_COMMA + IS_MOVED_FIELD + PARAMETER_COMMA +
      TRANSFER_START_FIELD + PARAMETER_COMMA + TRANSFER_STOP_FIELD +
      PARAMETER_COMMA + OWNER_REQUEST_FIELD + PARAMETER_COMMA +
      REQUESTER_FIELD + PARAMETER_COMMA + REQUESTED_FIELD + PARAMETER_COMMA +
      ID_RULE_FIELD + PARAMETER_COMMA + FILENAME_FIELD + PARAMETER_COMMA +
      ORIGINAL_NAME_FIELD + PARAMETER_COMMA + FILE_INFO_FIELD +
      PARAMETER_COMMA + TRANSFER_INFO_FIELD + " = ?  WHERE " +
      PRIMARY_KEY_WHERE;
  private static final String SQL_UPDATE_LIMITED_RANK =
      "UPDATE " + TABLE + " SET " + RANK_FIELD + PARAMETER_COMMA +
      TRANSFER_STOP_FIELD + " = ?  WHERE " + PRIMARY_KEY_WHERE;

  protected String getDeleteRequest() {
    return SQL_DELETE;
  }

  protected String getDeleteAllRequest() {
    return SQL_DELETE_ALL;
  }

  protected String getExistRequest() {
    return SQL_EXIST;
  }

  protected String getGetAllRequest() {
    return SQL_GET_ALL;
  }

  protected String getInsertRequest() {
    return SQL_INSERT;
  }

  protected String getSelectRequest() {
    return SQL_SELECT;
  }

  protected String getUpdateRequest() {
    return SQL_UPDATE;
  }

  String getUpdateLimitedRankRequest() {
    return SQL_UPDATE_LIMITED_RANK;
  }

  protected DBTransferDAO(final Connection con) {
    super(con);
  }

  @Override
  protected Object[] getInsertValues(final Transfer transfer) {
    return new Object[] {
        transfer.getId(), transfer.getGlobalStep().ordinal(),
        transfer.getLastGlobalStep().ordinal(), transfer.getStep(),
        transfer.getRank(), transfer.getBlockSize(), transfer.getTransferMode(),
        transfer.getUpdatedInfo().ordinal(), transfer.getStepStatus().getCode(),
        transfer.getInfoStatus().getCode(), transfer.getRetrieveMode(),
        transfer.getIsMoved(), transfer.getStart(), transfer.getStop(),
        transfer.getOwnerRequest(), transfer.getRequested(),
        transfer.getRequester(), transfer.getRule(), transfer.getFilename(),
        transfer.getOriginalName(), transfer.getFileInfo(),
        transfer.getTransferInfo()
    };
  }

  @Override
  protected Object[] getUpdateValues(final Transfer transfer) {
    return new Object[] {
        transfer.getId(), transfer.getGlobalStep().ordinal(),
        transfer.getLastGlobalStep().ordinal(), transfer.getStep(),
        transfer.getRank(), transfer.getBlockSize(), transfer.getTransferMode(),
        transfer.getUpdatedInfo().ordinal(), transfer.getStepStatus().getCode(),
        transfer.getInfoStatus().getCode(), transfer.getRetrieveMode(),
        transfer.getIsMoved(), transfer.getStart(), transfer.getStop(),
        transfer.getOwnerRequest(), transfer.getRequester(),
        transfer.getRequested(), transfer.getRule(), transfer.getFilename(),
        transfer.getOriginalName(), transfer.getFileInfo(),
        transfer.getTransferInfo(), transfer.getId(), transfer.getRequester(),
        transfer.getRequested(), transfer.getOwnerRequest()
    };
  }

  Object[] getUpdateLimitedRankValues(final Transfer transfer) {
    return new Object[] {
        transfer.getRank(), transfer.getStop(), transfer.getId(),
        transfer.getRequester(), transfer.getRequested(),
        transfer.getOwnerRequest()
    };
  }

  Object[] getPrimaryKeyValues(final Transfer transfer) {
    return getPrimaryKeyValues(transfer.getId(), transfer.getOwnerRequest(),
                               transfer.getRequester(),
                               transfer.getRequested());
  }

  Object[] getPrimaryKeyValues(long id, String owner, String requester,
                               String requested) {
    return new Object[] { id, requester, requested, owner };
  }

  public void updateRank(final Transfer e1)
      throws DAOConnectionException, DAONoDataException {
    final Object[] params = getUpdateLimitedRankValues(e1);

    PreparedStatement stm = null;
    try {
      stm = connection.prepareStatement(getUpdateLimitedRankRequest());
      setParameters(stm, params);
      try {
        executeUpdate(stm);
      } catch (final SQLException e2) {
        throw new DAONoDataException(e2);
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
    }
  }

  @Override
  public void delete(final Transfer transfer)
      throws DAOConnectionException, DAONoDataException {
    PreparedStatement stm = null;
    final Object[] params = getPrimaryKeyValues(transfer);
    try {
      stm = connection.prepareStatement(getDeleteRequest());
      setParameters(stm, params);
      try {
        executeAction(stm);
      } catch (final SQLException e2) {
        throw new DAONoDataException(e2);
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeStatement(stm);
    }
  }

  @Override
  public List<Transfer> find(final List<Filter> filters, final int limit)
      throws DAOConnectionException {
    final ArrayList<Transfer> transfers = new ArrayList<Transfer>();
    // Create the SQL query
    final Object[] params = prepareFindParams(filters);
    final StringBuilder query =
        new StringBuilder(prepareFindQuery(filters, params));
    // Set LIMIT
    if (limit > 0) {
      query.append(LIMIT2).append(limit);
    }
    // Execute query
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(query.toString());
      setParameters(stm, params);
      res = executeQuery(stm);
      while (res.next()) {
        transfers.add(getFromResultSet(res));
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
    return transfers;
  }

  @Override
  public List<Transfer> find(final List<Filter> filters, final int limit,
                             final int offset) throws DAOConnectionException {
    final ArrayList<Transfer> transfers = new ArrayList<Transfer>();
    // Create the SQL query
    final Object[] params = prepareFindParams(filters);
    final StringBuilder query =
        new StringBuilder(prepareFindQuery(filters, params));
    // Set LIMIT
    if (limit > 0) {
      query.append(LIMIT2).append(limit);
    }
    // Set OFFSET
    if (offset > 0) {
      query.append(" OFFSET ").append(offset);
    }
    // Execute query
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(query.toString());
      setParameters(stm, params);
      res = executeQuery(stm);
      while (res.next()) {
        transfers.add(getFromResultSet(res));
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
    return transfers;
  }

  @Override
  public List<Transfer> find(final List<Filter> filters, final String column,
                             final boolean ascend)
      throws DAOConnectionException {
    final ArrayList<Transfer> transfers = new ArrayList<Transfer>();
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
    // Execute query
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(query.toString());
      setParameters(stm, params);
      res = executeQuery(stm);
      while (res.next()) {
        transfers.add(getFromResultSet(res));
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
    return transfers;
  }

  @Override
  public List<Transfer> find(final List<Filter> filters, final String column,
                             final boolean ascend, final int limit)
      throws DAOConnectionException {
    final ArrayList<Transfer> transfers = new ArrayList<Transfer>();
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
      query.append(LIMIT2).append(limit);
    }
    // Execute query
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(query.toString());
      setParameters(stm, params);
      res = executeQuery(stm);
      while (res.next()) {
        transfers.add(getFromResultSet(res));
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
    return transfers;
  }

  @Override
  public List<Transfer> find(final List<Filter> filters, final String column,
                             final boolean ascend, final int limit,
                             final int offset) throws DAOConnectionException {
    final ArrayList<Transfer> transfers = new ArrayList<Transfer>();
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
      query.append(LIMIT2).append(limit);
    }
    // Set OFFSET
    if (offset > 0) {
      query.append(" OFFSET ").append(offset);
    }
    // Execute query
    PreparedStatement stm = null;
    ResultSet res = null;
    try {
      stm = connection.prepareStatement(query.toString());
      setParameters(stm, params);
      res = executeQuery(stm);
      while (res.next()) {
        transfers.add(getFromResultSet(res));
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
    return transfers;
  }

  @Override
  public boolean exist(final long id, final String requester,
                       final String requested, final String owner)
      throws DAOConnectionException {
    PreparedStatement stm = null;
    ResultSet res = null;
    final Object[] params =
        getPrimaryKeyValues(id, owner, requester, requested);
    try {
      stm = connection.prepareStatement(getExistRequest());
      setParameters(stm, params);
      res = executeQuery(stm);
      return res.next();
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
  }

  @Override
  public Transfer select(final long id, final String requester,
                         final String requested, final String owner)
      throws DAOConnectionException, DAONoDataException {
    PreparedStatement stm = null;
    ResultSet res = null;
    final Object[] params =
        getPrimaryKeyValues(id, owner, requester, requested);
    try {
      stm = connection.prepareStatement(getSelectRequest());
      setParameters(stm, params);
      res = executeQuery(stm);
      if (res.next()) {
        return getFromResultSet(res);
      } else {
        throw new DAONoDataException("No " + getClass().getName() + " found");
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      closeResultSet(res);
      closeStatement(stm);
    }
  }

  protected abstract long getNextId() throws DAOConnectionException;

  @Override
  public void insert(final Transfer transfer) throws DAOConnectionException {
    if (transfer.getId() == ILLEGALVALUE) {
      transfer.setId(getNextId());
    }
    super.insert(transfer);
  }

  @Override
  public Transfer getFromResultSet(final ResultSet set) throws SQLException {
    try {
      return new Transfer(set.getLong(ID_FIELD), set.getString(ID_RULE_FIELD),
                          set.getInt(TRANSFER_MODE_FIELD),
                          set.getString(FILENAME_FIELD),
                          set.getString(ORIGINAL_NAME_FIELD),
                          set.getString(FILE_INFO_FIELD),
                          set.getBoolean(IS_MOVED_FIELD),
                          set.getInt(BLOCK_SIZE_FIELD),
                          set.getBoolean(RETRIEVE_MODE_FIELD),
                          set.getString(OWNER_REQUEST_FIELD),
                          set.getString(REQUESTER_FIELD),
                          set.getString(REQUESTED_FIELD),
                          set.getString(TRANSFER_INFO_FIELD), Transfer.TASKSTEP
                              .valueOf(set.getInt(GLOBAL_STEP_FIELD)),
                          Transfer.TASKSTEP
                              .valueOf(set.getInt(GLOBAL_LAST_STEP_FIELD)),
                          set.getInt(STEP_FIELD), ErrorCode
                              .getFromCode(set.getString(STEP_STATUS_FIELD)),
                          ErrorCode
                              .getFromCode(set.getString(INFO_STATUS_FIELD)),
                          set.getInt(RANK_FIELD),
                          set.getTimestamp(TRANSFER_START_FIELD),
                          set.getTimestamp(TRANSFER_STOP_FIELD),
                          UpdatedInfo.valueOf(set.getInt(UPDATED_INFO_FIELD)));
    } catch (WaarpDatabaseSqlException e) {
      throw new SQLException(e);
    }
  }


  /**
   * {@link UnsupportedOperationException}
   *
   * @param id id of the object requested
   *
   * @return never
   */
  @Override
  public Transfer select(final String id) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@link UnsupportedOperationException}
   *
   * @param id id of the object requested
   *
   * @return never
   */
  @Override
  public boolean exist(final String id) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@link UnsupportedOperationException}
   *
   * @param e1 as Transfer
   *
   * @return never
   */
  @Override
  protected String getId(final Transfer e1) {
    throw new UnsupportedOperationException();
  }
}


