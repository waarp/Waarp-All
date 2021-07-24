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
package org.waarp.common.database.data;

import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;

import java.sql.Types;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Example of Table object
 */
public class DbDataModel extends AbstractDbData {
  private static final String NO_ROW_FOUND = "No row found";

  private static final String WHERE = " WHERE ";

  private static final String SELECT = "SELECT ";

  private static final String FROM = " FROM ";

  public enum Columns {
    READGLOBALLIMIT, WRITEGLOBALLIMIT, READSESSIONLIMIT, WRITESESSIONLIMIT,
    DELAYLIMIT, UPDATEDINFO, HOSTID
  }

  public static final int[] dbTypes = {
      Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.BIGINT, Types.BIGINT,
      Types.INTEGER, Types.NVARCHAR
  };

  public static final String table = " CONFIGURATION ";
  public static final String fieldseq = "RUNSEQ";
  public static final Columns[] indexes = {
      Columns.READGLOBALLIMIT, Columns.READSESSIONLIMIT,
      Columns.WRITEGLOBALLIMIT, Columns.WRITESESSIONLIMIT, Columns.HOSTID
  };

  /**
   * HashTable in case of lack of database
   */
  private static final ConcurrentHashMap<String, DbDataModel>
      dbR66ConfigurationHashMap = new ConcurrentHashMap<String, DbDataModel>();

  private String hostid;

  private long readgloballimit;

  private long writegloballimit;

  private long readsessionlimit;

  private long writesessionlimit;

  private long delayllimit;

  private int updatedInfo = UpdatedInfo.UNKNOWN.ordinal();

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
  protected void initObject() {
    primaryKey = new DbValue[] { new DbValue(hostid, Columns.HOSTID.name()) };
    otherFields = new DbValue[] {
        new DbValue(readgloballimit, Columns.READGLOBALLIMIT.name()),
        new DbValue(writegloballimit, Columns.WRITEGLOBALLIMIT.name()),
        new DbValue(readsessionlimit, Columns.READSESSIONLIMIT.name()),
        new DbValue(writesessionlimit, Columns.WRITESESSIONLIMIT.name()),
        new DbValue(delayllimit, Columns.DELAYLIMIT.name()),
        new DbValue(updatedInfo, Columns.UPDATEDINFO.name())
    };
    allFields = new DbValue[] {
        otherFields[0], otherFields[1], otherFields[2], otherFields[3],
        otherFields[4], otherFields[5], primaryKey[0]
    };
  }

  @Override
  protected void setToArray() throws WaarpDatabaseSqlException {
    validateLength(Types.NVARCHAR, hostid);
    allFields[Columns.HOSTID.ordinal()].setValue(hostid);
    allFields[Columns.READGLOBALLIMIT.ordinal()].setValue(readgloballimit);
    allFields[Columns.WRITEGLOBALLIMIT.ordinal()].setValue(writegloballimit);
    allFields[Columns.READSESSIONLIMIT.ordinal()].setValue(readsessionlimit);
    allFields[Columns.WRITESESSIONLIMIT.ordinal()].setValue(writesessionlimit);
    allFields[Columns.DELAYLIMIT.ordinal()].setValue(delayllimit);
    allFields[Columns.UPDATEDINFO.ordinal()].setValue(updatedInfo);
  }

  @Override
  protected void setFromArray() {
    hostid = (String) allFields[Columns.HOSTID.ordinal()].getValue();
    readgloballimit =
        (Long) allFields[Columns.READGLOBALLIMIT.ordinal()].getValue();
    writegloballimit =
        (Long) allFields[Columns.WRITEGLOBALLIMIT.ordinal()].getValue();
    readsessionlimit =
        (Long) allFields[Columns.READSESSIONLIMIT.ordinal()].getValue();
    writesessionlimit =
        (Long) allFields[Columns.WRITESESSIONLIMIT.ordinal()].getValue();
    delayllimit = (Long) allFields[Columns.DELAYLIMIT.ordinal()].getValue();
    updatedInfo = (Integer) allFields[Columns.UPDATEDINFO.ordinal()].getValue();
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
  protected String getWherePrimaryKey() {
    return primaryKey[0].getColumn() + " = ? ";
  }

  /**
   * Set the primary Key as current value
   */
  @Override
  protected void setPrimaryKey() {
    primaryKey[0].setValue(hostid);
  }

  /**
   * @param dbSession
   * @param hostid
   * @param rg Read Global Limit
   * @param wg Write Global Limit
   * @param rs Read Session Limit
   * @param ws Write Session Limit
   * @param del Delay Limit
   */
  public DbDataModel(final DbSession dbSession, final String hostid,
                     final long rg, final long wg, final long rs, final long ws,
                     final long del) throws WaarpDatabaseSqlException {
    super(dbSession);
    this.hostid = hostid;
    readgloballimit = rg;
    writegloballimit = wg;
    readsessionlimit = rs;
    writesessionlimit = ws;
    delayllimit = del;
    setToArray();
    isSaved = false;
  }

  /**
   * @param dbSession
   * @param hostid
   *
   * @throws WaarpDatabaseException
   */
  public DbDataModel(final DbSession dbSession, final String hostid)
      throws WaarpDatabaseException {
    super(dbSession);
    this.hostid = hostid;
    // load from DB
    select();
  }

  @Override
  public void delete() throws WaarpDatabaseException {
    if (dbSession == null) {
      dbR66ConfigurationHashMap.remove(hostid);
      isSaved = false;
      return;
    }
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(dbSession);
    try {
      preparedStatement.createPrepareStatement(
          "DELETE FROM " + table + WHERE + getWherePrimaryKey());
      setPrimaryKey();
      setValues(preparedStatement, primaryKey);
      final int count = preparedStatement.executeUpdate();
      if (count <= 0) {
        throw new WaarpDatabaseNoDataException(NO_ROW_FOUND);
      }
      isSaved = false;
    } finally {
      preparedStatement.realClose();
    }
  }

  @Override
  public void insert() throws WaarpDatabaseException {
    if (isSaved) {
      return;
    }
    if (dbSession == null) {
      dbR66ConfigurationHashMap.put(hostid, this);
      isSaved = true;
      return;
    }
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(dbSession);
    try {
      preparedStatement.createPrepareStatement(
          "INSERT INTO " + table + " (" + selectAllFields + ") VALUES " +
          insertAllValues);
      setValues(preparedStatement, allFields);
      final int count = preparedStatement.executeUpdate();
      if (count <= 0) {
        throw new WaarpDatabaseNoDataException(NO_ROW_FOUND);
      }
      isSaved = true;
    } finally {
      preparedStatement.realClose();
    }
  }

  @Override
  public final boolean exist() throws WaarpDatabaseException {
    if (dbSession == null) {
      return dbR66ConfigurationHashMap.containsKey(hostid);
    }
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(dbSession);
    try {
      preparedStatement.createPrepareStatement(
          SELECT + primaryKey[0].getColumn() + FROM + table + WHERE +
          getWherePrimaryKey());
      setPrimaryKey();
      setValues(preparedStatement, primaryKey);
      preparedStatement.executeQuery();
      return preparedStatement.getNext();
    } finally {
      preparedStatement.realClose();
    }
  }

  @Override
  public final void select() throws WaarpDatabaseException {
    if (dbSession == null) {
      final DbDataModel conf = dbR66ConfigurationHashMap.get(hostid);
      if (conf == null) {
        throw new WaarpDatabaseNoDataException(NO_ROW_FOUND);
      } else {
        // copy info
        for (int i = 0; i < allFields.length; i++) {
          allFields[i].setValue(conf.allFields[i].getValue());
        }
        setFromArray();
        isSaved = true;
        return;
      }
    }
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(dbSession);
    try {
      preparedStatement.createPrepareStatement(
          SELECT + selectAllFields + FROM + table + WHERE +
          getWherePrimaryKey());
      setPrimaryKey();
      setValues(preparedStatement, primaryKey);
      preparedStatement.executeQuery();
      if (preparedStatement.getNext()) {
        getValues(preparedStatement, allFields);
        setFromArray();
        isSaved = true;
      } else {
        throw new WaarpDatabaseNoDataException(NO_ROW_FOUND);
      }
    } finally {
      preparedStatement.realClose();
    }
  }

  @Override
  public final void update() throws WaarpDatabaseException {
    if (isSaved) {
      return;
    }
    if (dbSession == null) {
      dbR66ConfigurationHashMap.put(hostid, this);
      isSaved = true;
      return;
    }
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(dbSession);
    try {
      preparedStatement.createPrepareStatement(
          "UPDATE " + table + " SET " + updateAllFields + WHERE +
          getWherePrimaryKey());
      setValues(preparedStatement, allFields);
      final int count = preparedStatement.executeUpdate();
      if (count <= 0) {
        throw new WaarpDatabaseNoDataException(NO_ROW_FOUND);
      }
      isSaved = true;
    } finally {
      preparedStatement.realClose();
    }
  }

  /**
   * @return the DbPreparedStatement for getting Updated Object
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getUpdatedPrepareStament(
      final DbSession session)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    String request = SELECT + selectAllFields;
    request += FROM + table + WHERE + Columns.UPDATEDINFO.name() + " = " +
               AbstractDbData.UpdatedInfo.TOSUBMIT.ordinal();
    final DbPreparedStatement prep = new DbPreparedStatement(session, request);
    session.addLongTermPreparedStatement(prep);
    return prep;
  }

  @Override
  public void changeUpdatedInfo(final UpdatedInfo info) {
    if (updatedInfo != info.ordinal()) {
      updatedInfo = info.ordinal();
      allFields[Columns.UPDATEDINFO.ordinal()].setValue(updatedInfo);
      isSaved = false;
    }
  }

}
