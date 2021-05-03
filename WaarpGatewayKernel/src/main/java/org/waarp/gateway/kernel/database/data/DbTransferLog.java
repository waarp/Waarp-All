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
package org.waarp.gateway.kernel.database.data;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.dom4j.Document;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.data.DbValue;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.xml.XmlDecl;
import org.waarp.common.xml.XmlType;
import org.waarp.common.xml.XmlUtil;
import org.waarp.common.xml.XmlValue;
import org.waarp.gateway.kernel.HttpPage.PageRole;
import org.waarp.gateway.kernel.HttpPageHandler;
import org.waarp.gateway.kernel.database.DbConstantGateway;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Transfer Log for Gateway for Http
 */
public class DbTransferLog extends AbstractDbData {
  private static final String ERROR_DURING_PURGE = "Error during purge";

  private static final String NO_ROW_FOUND2 = "No row found";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DbTransferLog.class);

  public enum Columns {
    FILENAME, MODETRANS, STARTTRANS, STOPTRANS, TRANSINFO, INFOSTATUS,
    UPDATEDINFO, USERID, ACCOUNTID, HOSTID, SPECIALID
  }

  public static final int[] dbTypes = {
      Types.VARCHAR, Types.NVARCHAR, Types.TIMESTAMP, Types.TIMESTAMP,
      Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.NVARCHAR,
      Types.NVARCHAR, Types.NVARCHAR, Types.BIGINT
  };

  public static final String table = " TRANSFLOG ";

  public static final String fieldseq = "TRANSSEQ";

  public static final Columns[] indexes =
      { Columns.STARTTRANS, Columns.UPDATEDINFO, Columns.INFOSTATUS };

  public static final String XMLRUNNERS = "transferlogs";
  public static final String XMLRUNNER = "log";

  // Values
  private String user;

  private String account;

  private long specialId;

  private boolean isSender;

  private String filename;

  private String mode;

  private Timestamp start;

  private Timestamp stop;

  private String infotransf;

  private String hostid;

  /**
   * Info status error code
   */
  private HttpResponseStatus infostatus = HttpResponseStatus.OK;

  /**
   * The global status for running
   */
  private int updatedInfo = UpdatedInfo.UNKNOWN.ordinal();

  /**
   * Special For DbTransferLog
   */
  public static final int NBPRKEY = 4;
  // ALL TABLE SHOULD IMPLEMENT THIS

  protected static final String selectAllFields =
      Columns.FILENAME.name() + ',' + Columns.MODETRANS.name() + ',' +
      Columns.STARTTRANS.name() + ',' + Columns.STOPTRANS.name() + ',' +
      Columns.TRANSINFO.name() + ',' + Columns.INFOSTATUS.name() + ',' +
      Columns.UPDATEDINFO.name() + ',' + Columns.USERID.name() + ',' +
      Columns.ACCOUNTID.name() + ',' + Columns.HOSTID.name() + ',' +
      Columns.SPECIALID.name();

  protected static final String updateAllFields =
      Columns.FILENAME.name() + "=?," + Columns.MODETRANS.name() + "=?," +
      Columns.STARTTRANS.name() + "=?," + Columns.STOPTRANS.name() + "=?," +
      Columns.TRANSINFO.name() + "=?," + Columns.INFOSTATUS.name() + "=?," +
      Columns.UPDATEDINFO.name() + "=?";

  protected static final String insertAllValues = " (?,?,?,?,?,?,?,?,?,?,?) ";

  private static final Set<Long> clientNoDbSpecialId = new HashSet<Long>();

  /**
   * Insert into database
   *
   * @param dbSession
   * @param user
   * @param account
   * @param specialId
   * @param isSender
   * @param filename
   * @param mode
   * @param infostatus
   * @param info
   * @param updatedInfo
   *
   * @throws WaarpDatabaseException
   */
  public DbTransferLog(final DbSession dbSession, final String user,
                       final String account, final long specialId,
                       final boolean isSender, final String filename,
                       final String mode, final HttpResponseStatus infostatus,
                       final String info, final UpdatedInfo updatedInfo)
      throws WaarpDatabaseException {
    super(dbSession);
    this.user = user;
    this.account = account;
    this.specialId = specialId;
    this.isSender = isSender;
    this.filename = filename;
    this.mode = mode;
    start = new Timestamp(System.currentTimeMillis());
    this.infostatus = infostatus;
    infotransf = info;
    this.updatedInfo = updatedInfo.ordinal();
    hostid = HttpPageHandler.hostid;
    setToArray();
    isSaved = false;
    insert();
  }

  /**
   * Load from database
   *
   * @param dbSession
   * @param user
   * @param account
   * @param specialId
   *
   * @throws WaarpDatabaseException
   */
  public DbTransferLog(final DbSession dbSession, final String user,
                       final String account, final long specialId)
      throws WaarpDatabaseException {
    super(dbSession);
    this.user = user;
    this.account = account;
    this.specialId = specialId;
    hostid = HttpPageHandler.hostid;
    select();
  }

  @Override
  protected void initObject() {
    primaryKey = new DbValue[] {
        new DbValue(user, Columns.USERID.name()),
        new DbValue(account, Columns.ACCOUNTID.name()),
        new DbValue(hostid, Columns.HOSTID.name()),
        new DbValue(specialId, Columns.SPECIALID.name())
    };
    otherFields = new DbValue[] {
        // FILENAME, MODETRANS,
        // STARTTRANS, STOPTRANS, TRANSINFO
        // INFOSTATUS, UPDATEDINFO
        new DbValue(filename, Columns.FILENAME.name()),
        new DbValue(mode, Columns.MODETRANS.name()),
        new DbValue(start, Columns.STARTTRANS.name()),
        new DbValue(stop, Columns.STOPTRANS.name()),
        new DbValue(infotransf, Columns.TRANSINFO.name()),
        new DbValue(HttpResponseStatus.OK.code(), Columns.INFOSTATUS.name()),
        // infostatus.getCode()
        new DbValue(updatedInfo, Columns.UPDATEDINFO.name())
    };
    allFields = new DbValue[] {
        otherFields[0], otherFields[1], otherFields[2], otherFields[3],
        otherFields[4], otherFields[5], otherFields[6], primaryKey[0],
        primaryKey[1], primaryKey[2], primaryKey[3]
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
  protected void setToArray() throws WaarpDatabaseSqlException {
    // FILENAME, MODETRANS,
    // STARTTRANS, STOPTRANS, TRANSINFO
    // INFOSTATUS, UPDATEDINFO
    // USERID, ACCOUNTID, SPECIALID
    validateLength(Types.VARCHAR, filename, infotransf);
    validateLength(Types.NVARCHAR, mode, user, account, hostid);
    allFields[Columns.FILENAME.ordinal()].setValue(filename);
    allFields[Columns.MODETRANS.ordinal()].setValue(mode);
    allFields[Columns.STARTTRANS.ordinal()].setValue(start);
    stop = new Timestamp(System.currentTimeMillis());
    allFields[Columns.STOPTRANS.ordinal()].setValue(stop);
    allFields[Columns.TRANSINFO.ordinal()].setValue(infotransf);
    allFields[Columns.INFOSTATUS.ordinal()].setValue(infostatus.code());
    allFields[Columns.UPDATEDINFO.ordinal()].setValue(updatedInfo);
    allFields[Columns.USERID.ordinal()].setValue(user);
    allFields[Columns.ACCOUNTID.ordinal()].setValue(account);
    allFields[Columns.HOSTID.ordinal()].setValue(hostid);
    allFields[Columns.SPECIALID.ordinal()].setValue(specialId);
  }

  @Override
  protected void setFromArray() {
    filename = (String) allFields[Columns.FILENAME.ordinal()].getValue();
    mode = (String) allFields[Columns.MODETRANS.ordinal()].getValue();
    start = (Timestamp) allFields[Columns.STARTTRANS.ordinal()].getValue();
    stop = (Timestamp) allFields[Columns.STOPTRANS.ordinal()].getValue();
    infostatus = HttpResponseStatus
        .valueOf((Integer) allFields[Columns.INFOSTATUS.ordinal()].getValue());
    infotransf = (String) allFields[Columns.TRANSINFO.ordinal()].getValue();
    updatedInfo = (Integer) allFields[Columns.UPDATEDINFO.ordinal()].getValue();
    user = (String) allFields[Columns.USERID.ordinal()].getValue();
    account = (String) allFields[Columns.ACCOUNTID.ordinal()].getValue();
    hostid = (String) allFields[Columns.HOSTID.ordinal()].getValue();
    specialId = (Long) allFields[Columns.SPECIALID.ordinal()].getValue();
  }

  /**
   * @return The Where condition on Primary Key
   */
  @Override
  protected String getWherePrimaryKey() {
    return primaryKey[0].getColumn() + " = ? AND " + primaryKey[1].getColumn() +
           " = ? AND " + primaryKey[2].getColumn() + " = ? AND " +
           primaryKey[3].getColumn() + " = ? ";
  }

  /**
   * Set the primary Key as current value
   */
  @Override
  protected void setPrimaryKey() {
    primaryKey[0].setValue(user);
    primaryKey[1].setValue(account);
    primaryKey[2].setValue(hostid);
    primaryKey[3].setValue(specialId);
  }

  /**
   * @return the condition to limit access to the row concerned by the Host
   */
  private static String getLimitWhereCondition() {
    return " " + Columns.HOSTID + " = '" + HttpPageHandler.hostid + "' ";
  }

  /**
   * Create a Special Id for NoDb client
   */
  private void createNoDbSpecialId() {
    synchronized (clientNoDbSpecialId) {
      // New SpecialId is not possible with No Database Model
      specialId = System.currentTimeMillis();
      Long newOne = specialId;
      while (clientNoDbSpecialId.contains(newOne)) {
        newOne = specialId++;
      }
      clientNoDbSpecialId.add(newOne);
    }
  }

  /**
   * Remove a Special Id for NoDb Client
   */
  private void removeNoDbSpecialId() {
    synchronized (clientNoDbSpecialId) {
      final Long oldOne = specialId;
      clientNoDbSpecialId.remove(oldOne);
    }
  }

  @Override
  public void delete() throws WaarpDatabaseException {
    if (dbSession == null) {
      removeNoDbSpecialId();
      return;
    }
    super.delete();
  }

  @Override
  public void insert() throws WaarpDatabaseException {
    if (isSaved) {
      return;
    }
    if (dbSession == null) {
      if (specialId == DbConstantGateway.ILLEGALVALUE) {
        // New SpecialId is not possible with No Database Model
        createNoDbSpecialId();
      }
      isSaved = true;
      return;
    }
    // First need to find a new id if id is not ok
    if (specialId == DbConstantGateway.ILLEGALVALUE) {
      specialId = dbSession.getAdmin().getDbModel().nextSequence(dbSession);
      logger.debug("Try Insert create a new Id from sequence: {}", specialId);
      setPrimaryKey();
    }
    super.insert();
  }

  /**
   * As insert but with the ability to change the SpecialId
   *
   * @throws WaarpDatabaseException
   */
  public void create() throws WaarpDatabaseException {
    if (isSaved) {
      return;
    }
    if (dbSession == null) {
      if (specialId == DbConstantGateway.ILLEGALVALUE) {
        // New SpecialId is not possible with No Database Model
        createNoDbSpecialId();
      }
      isSaved = true;
      return;
    }
    // First need to find a new id if id is not ok
    if (specialId == DbConstantGateway.ILLEGALVALUE) {
      specialId = dbSession.getAdmin().getDbModel().nextSequence(dbSession);
      logger.debug("Try Insert create a new Id from sequence: {}", specialId);
      setPrimaryKey();
    }
    setToArray();
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(dbSession);
    try {
      preparedStatement.createPrepareStatement(
          "INSERT INTO " + table + " (" + selectAllFields + ") VALUES " +
          insertAllValues);
      setValues(preparedStatement, allFields);
      try {
        final int count = preparedStatement.executeUpdate();
        if (count <= 0) {
          throw new WaarpDatabaseNoDataException(NO_ROW_FOUND2);
        }
      } catch (final WaarpDatabaseSqlException e) {
        logger.error("Problem while inserting: {}", e.getMessage());
        final DbPreparedStatement find = new DbPreparedStatement(dbSession);
        try {
          find.createPrepareStatement(
              "SELECT MAX(" + primaryKey[3].getColumn() + ") FROM " + table +
              " WHERE " + primaryKey[0].getColumn() + " = ? AND " +
              primaryKey[1].getColumn() + " = ? AND " +
              primaryKey[2].getColumn() + " = ? AND " +
              primaryKey[3].getColumn() + " != ? ");
          setPrimaryKey();
          setValues(find, primaryKey);
          find.executeQuery();
          if (find.getNext()) {
            final long result;
            try {
              result = find.getResultSet().getLong(1);
            } catch (final SQLException e1) {
              throw new WaarpDatabaseSqlException(e1);
            }
            specialId = result + 1;
            dbSession.getAdmin().getDbModel()
                     .resetSequence(dbSession, specialId + 1);
            setToArray();
            preparedStatement.close();
            setValues(preparedStatement, allFields);
            final int count = preparedStatement.executeUpdate();
            if (count <= 0) {
              throw new WaarpDatabaseNoDataException(NO_ROW_FOUND2);
            }
          } else {
            throw new WaarpDatabaseNoDataException(NO_ROW_FOUND2);
          }
        } finally {
          find.realClose();
        }
      }
      isSaved = true;
    } finally {
      preparedStatement.realClose();
    }
  }

  /**
   * Private constructor
   *
   * @param dBsession
   */
  public DbTransferLog(final DbSession dBsession) {
    super(dBsession);
  }

  /**
   * For instance when getting updated information
   *
   * @param preparedStatement
   *
   * @return the next updated DbTaskRunner
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbTransferLog getFromStatement(
      final DbPreparedStatement preparedStatement)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbTransferLog dbTaskRunner =
        new DbTransferLog(preparedStatement.getDbSession());
    dbTaskRunner.getValues(preparedStatement, dbTaskRunner.allFields);
    dbTaskRunner.setFromArray();
    dbTaskRunner.isSaved = true;
    return dbTaskRunner;
  }

  /**
   * @param session
   * @param status
   * @param limit limit the number of rows
   *
   * @return the DbPreparedStatement for getting TransferLog according to
   *     status
   *     ordered by start
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getStatusPrepareStament(
      final DbSession session, final HttpResponseStatus status, final int limit)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    String request = "SELECT " + selectAllFields + " FROM " + table;
    if (status != null) {
      request += " WHERE " + Columns.INFOSTATUS.name() + " = " + status.code() +
                 " AND " + getLimitWhereCondition();
    } else {
      request += " WHERE " + getLimitWhereCondition();
    }
    request += " ORDER BY " + Columns.STARTTRANS.name() + " DESC ";
    if (limit > 0) {
      request = session.getAdmin().getDbModel()
                       .limitRequest(selectAllFields, request, limit);
    }
    return new DbPreparedStatement(session, request);
  }

  public static DbPreparedStatement getFilterPrepareStament(
      final DbSession session, final String modetrans, final String accountid,
      final String userid, final String filename, final String status)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    String request = "SELECT " + selectAllFields + " FROM " + table;
    String where = null;
    if (status != null) {
      where = " WHERE " + Columns.INFOSTATUS.name() + " = " + status;
    }
    if (modetrans != null) {
      if (where == null) {
        where = " WHERE " + Columns.MODETRANS.name() + " = " + modetrans;
      } else {
        where = " AND " + Columns.MODETRANS.name() + " = " + modetrans;
      }
    }
    if (accountid != null) {
      if (where == null) {
        where = " WHERE " + Columns.ACCOUNTID.name() + " = " + accountid;
      } else {
        where = " AND " + Columns.ACCOUNTID.name() + " = " + accountid;
      }
    }
    if (userid != null) {
      if (where == null) {
        where = " WHERE " + Columns.USERID.name() + " = " + userid;
      } else {
        where = " AND " + Columns.USERID.name() + " = " + userid;
      }
    }
    if (filename != null) {
      if (where == null) {
        where = " WHERE " + Columns.FILENAME.name() + " = " + filename;
      } else {
        where = " AND " + Columns.FILENAME.name() + " = " + filename;
      }
    }
    if (where == null) {
      request += " WHERE " + getLimitWhereCondition();
    } else {
      request += where + ' ' + getLimitWhereCondition();
    }
    request += " ORDER BY " + Columns.STARTTRANS.name() + " DESC ";
    return new DbPreparedStatement(session, request);
  }

  /**
   * @param session
   * @param start
   * @param stop
   *
   * @return the DbPreparedStatement for getting Selected Object, whatever
   *     their
   *     status
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getLogPrepareStament(
      final DbSession session, final Timestamp start, final Timestamp stop)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(session);
    String request = "SELECT " + selectAllFields + " FROM " + table;
    if (start != null && stop != null) {
      request += " WHERE " + Columns.STARTTRANS.name() + " >= ? AND " +
                 Columns.STARTTRANS.name() + " <= ? AND " +
                 getLimitWhereCondition() + " ORDER BY " +
                 Columns.SPECIALID.name() + " DESC ";
      preparedStatement.createPrepareStatement(request);
      try {
        preparedStatement.getPreparedStatement().setTimestamp(1, start);
        preparedStatement.getPreparedStatement().setTimestamp(2, stop);
      } catch (final SQLException e) {
        preparedStatement.realClose();
        throw new WaarpDatabaseSqlException(e);
      }
    } else if (start != null) {
      request += " WHERE " + Columns.STARTTRANS.name() + " >= ? AND " +
                 getLimitWhereCondition() + " ORDER BY " +
                 Columns.SPECIALID.name() + " DESC ";
      preparedStatement.createPrepareStatement(request);
      try {
        preparedStatement.getPreparedStatement().setTimestamp(1, start);
      } catch (final SQLException e) {
        preparedStatement.realClose();
        throw new WaarpDatabaseSqlException(e);
      }
    } else if (stop != null) {
      request += " WHERE " + Columns.STARTTRANS.name() + " <= ? AND " +
                 getLimitWhereCondition() + " ORDER BY " +
                 Columns.SPECIALID.name() + " DESC ";
      preparedStatement.createPrepareStatement(request);
      try {
        preparedStatement.getPreparedStatement().setTimestamp(1, stop);
      } catch (final SQLException e) {
        preparedStatement.realClose();
        throw new WaarpDatabaseSqlException(e);
      }
    } else {
      request += " WHERE " + getLimitWhereCondition() + " ORDER BY " +
                 Columns.SPECIALID.name() + " DESC ";
      preparedStatement.createPrepareStatement(request);
    }
    return preparedStatement;
  }

  /**
   * @param session
   *
   * @return the DbPreparedStatement for getting Updated Object
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getCountInfoPrepareStatement(
      final DbSession session)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final String request =
        "SELECT COUNT(" + Columns.SPECIALID.name() + ") FROM " + table +
        " WHERE " + Columns.STARTTRANS.name() + " >= ? AND " +
        getLimitWhereCondition() + " AND " + Columns.UPDATEDINFO.name() +
        " = ? ";
    final DbPreparedStatement pstt = new DbPreparedStatement(session, request);
    session.addLongTermPreparedStatement(pstt);
    return pstt;
  }

  /**
   * @param pstt
   * @param info
   * @param time
   *
   * @return the number of elements (COUNT) from the statement
   */
  public static long getResultCountPrepareStatement(
      final DbPreparedStatement pstt, final UpdatedInfo info, final long time) {
    long result = 0;
    try {
      finishSelectOrCountPrepareStatement(pstt, time);
      pstt.getPreparedStatement().setInt(2, info.ordinal());
      pstt.executeQuery();
      if (pstt.getNext()) {
        result = pstt.getResultSet().getLong(1);
      }
    } catch (final WaarpDatabaseNoConnectionException ignored) {
      // nothing
    } catch (final WaarpDatabaseSqlException ignored) {
      // nothing
    } catch (final SQLException ignored) {
      // nothing
    } finally {
      pstt.close();
    }
    return result;
  }

  /**
   * @param session
   *
   * @return the DbPreparedStatement for getting Runner according to status
   *     ordered by start
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getCountStatusPrepareStatement(
      final DbSession session)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    String request =
        "SELECT COUNT(" + Columns.SPECIALID.name() + ") FROM " + table;
    request += " WHERE " + Columns.STARTTRANS.name() + " >= ? ";
    request += " AND " + Columns.INFOSTATUS.name() + " = ? AND " +
               getLimitWhereCondition();
    final DbPreparedStatement prep = new DbPreparedStatement(session, request);
    session.addLongTermPreparedStatement(prep);
    return prep;
  }

  /**
   * @param session
   *
   * @return the DbPreparedStatement for getting All according to status
   *     ordered
   *     by start
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getCountAllPrepareStatement(
      final DbSession session)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    String request =
        "SELECT COUNT(" + Columns.SPECIALID.name() + ") FROM " + table;
    request += " WHERE " + Columns.STARTTRANS.name() + " >= ? ";
    request += " AND " + getLimitWhereCondition();
    final DbPreparedStatement prep = new DbPreparedStatement(session, request);
    session.addLongTermPreparedStatement(prep);
    return prep;
  }

  /**
   * @param pstt
   * @param error
   * @param time
   *
   * @return the number of elements (COUNT) from the statement
   */
  public static long getResultCountPrepareStatement(
      final DbPreparedStatement pstt, final HttpResponseStatus error,
      final long time) {
    long result = 0;
    try {
      finishSelectOrCountPrepareStatement(pstt, time);
      pstt.getPreparedStatement().setInt(2, error.code());
      pstt.executeQuery();
      if (pstt.getNext()) {
        result = pstt.getResultSet().getLong(1);
      }
    } catch (final WaarpDatabaseNoConnectionException ignored) {
      // nothing
    } catch (final WaarpDatabaseSqlException ignored) {
      // nothing
    } catch (final SQLException ignored) {
      // nothing
    } finally {
      pstt.close();
    }
    return result;
  }

  /**
   * @param pstt
   *
   * @return the number of elements (COUNT) from the statement
   */
  public static long getResultCountPrepareStatement(
      final DbPreparedStatement pstt) {
    long result = 0;
    try {
      pstt.executeQuery();
      if (pstt.getNext()) {
        result = pstt.getResultSet().getLong(1);
      }
    } catch (final WaarpDatabaseNoConnectionException ignored) {
      // nothing
    } catch (final WaarpDatabaseSqlException ignored) {
      // nothing
    } catch (final SQLException ignored) {
      // nothing
    } finally {
      pstt.close();
    }
    return result;
  }

  /**
   * Set the current time in the given updatedPreparedStatement
   *
   * @param pstt
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static void finishSelectOrCountPrepareStatement(
      final DbPreparedStatement pstt)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    finishSelectOrCountPrepareStatement(pstt, System.currentTimeMillis());
  }

  /**
   * Set the current time in the given updatedPreparedStatement
   *
   * @param pstt
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static void finishSelectOrCountPrepareStatement(
      final DbPreparedStatement pstt, final long time)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final Timestamp startlimit = new Timestamp(time);
    try {
      pstt.getPreparedStatement().setTimestamp(1, startlimit);
    } catch (final SQLException e) {
      logger.error("Database SQL Error: Cannot set timestamp: {}",
                   e.getMessage());
      throw new WaarpDatabaseSqlException("Cannot set timestamp", e);
    }
  }

  /**
   * Running or not transfers are concerned
   *
   * @param session
   * @param in True for Incoming, False for Outgoing
   *
   * @return the DbPreparedStatement for getting Runner according to in or out
   *     going way and Error
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getCountInOutErrorPrepareStatement(
      final DbSession session, final boolean in)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    String request =
        "SELECT COUNT(" + Columns.SPECIALID.name() + ") FROM " + table;
    final String inCond;
    if (in) {
      inCond =
          " (" + Columns.MODETRANS.name() + " = '" + PageRole.DELETE.name() +
          "' OR " + Columns.MODETRANS.name() + " = '" + PageRole.PUT.name() +
          "' OR " + Columns.MODETRANS.name() + " = '" + PageRole.POST.name() +
          "' OR " + Columns.MODETRANS.name() + " = '" +
          PageRole.POSTUPLOAD.name() + "') ";
    } else {
      inCond = " (" + Columns.MODETRANS.name() + " = '" +
               PageRole.GETDOWNLOAD.name() + "') ";
    }
    request += " WHERE " + inCond;
    request += " AND " + getLimitWhereCondition() + ' ';
    request += " AND " + Columns.STARTTRANS.name() + " >= ? ";
    request += " AND " + Columns.UPDATEDINFO.name() + " = " +
               UpdatedInfo.INERROR.ordinal();
    final DbPreparedStatement prep = new DbPreparedStatement(session, request);
    session.addLongTermPreparedStatement(prep);
    return prep;
  }

  /**
   * Running or not transfers are concerned
   *
   * @param session
   * @param in True for Incoming, False for Outgoing
   * @param running True for Running only, False for all
   *
   * @return the DbPreparedStatement for getting Runner according to in or out
   *     going way
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  public static DbPreparedStatement getCountInOutRunningPrepareStatement(
      final DbSession session, final boolean in, final boolean running)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    String request =
        "SELECT COUNT(" + Columns.SPECIALID.name() + ") FROM " + table;
    final String inCond;
    if (in) {
      inCond =
          " (" + Columns.MODETRANS.name() + " = '" + PageRole.DELETE.name() +
          "' OR " + Columns.MODETRANS.name() + " = '" + PageRole.PUT.name() +
          "' OR " + Columns.MODETRANS.name() + " = '" + PageRole.POST.name() +
          "' OR " + Columns.MODETRANS.name() + " = '" +
          PageRole.POSTUPLOAD.name() + "') ";
    } else {
      inCond = " (" + Columns.MODETRANS.name() + " = '" +
               PageRole.GETDOWNLOAD.name() + "') ";
    }
    request += " WHERE " + inCond;
    request += " AND " + getLimitWhereCondition() + ' ';
    request += " AND " + Columns.STARTTRANS.name() + " >= ? ";
    if (running) {
      request += " AND " + Columns.UPDATEDINFO.name() + " = " +
                 UpdatedInfo.RUNNING.ordinal();
    }
    final DbPreparedStatement prep = new DbPreparedStatement(session, request);
    session.addLongTermPreparedStatement(prep);
    return prep;
  }

  @Override
  public void changeUpdatedInfo(final UpdatedInfo info) {
    updatedInfo = info.ordinal();
    allFields[Columns.UPDATEDINFO.ordinal()].setValue(updatedInfo);
    isSaved = false;
  }

  /**
   * Set the HttpResponseStatus for the UpdatedInfo
   *
   * @param code
   */
  public void setReplyCodeExecutionStatus(final HttpResponseStatus code) {
    if (infostatus != code) {
      infostatus = code;
      allFields[Columns.INFOSTATUS.ordinal()].setValue(infostatus.code());
      isSaved = false;
    }
  }

  /**
   * @return The current UpdatedInfo value
   */
  public UpdatedInfo getUpdatedInfo() {
    return UpdatedInfo.values()[updatedInfo];
  }

  /**
   * @return the HttpResponseStatus code associated with the Updated Info
   */
  public HttpResponseStatus getErrorInfo() {
    return infostatus;
  }

  /**
   * @param filename the filename to set
   */
  public void setFilename(final String filename) {
    if (this.filename == null || !this.filename.equals(filename)) {
      this.filename = filename;
      allFields[Columns.FILENAME.ordinal()].setValue(this.filename);
      isSaved = false;
    }
  }

  /**
   * @return the isSender
   */
  public boolean isSender() {
    return isSender;
  }

  /**
   * @return the filename
   */
  public String getFilename() {
    return filename;
  }

  /**
   * @return the specialId
   */
  public long getSpecialId() {
    return specialId;
  }

  /**
   * @return the infotransf
   */
  public String getInfotransf() {
    return infotransf;
  }

  /**
   * @param infotransf the infotransf to set
   */
  public void setInfotransf(final String infotransf) {
    this.infotransf = infotransf;
  }

  /**
   * @return the user
   */
  public String getUser() {
    return user;
  }

  /**
   * @return the account
   */
  public String getAccount() {
    return account;
  }

  /**
   * @param stop the stop to set
   */
  public void setStop(final Timestamp stop) {
    this.stop = stop;
  }

  /**
   * @return the mode
   */
  public String getMode() {
    return mode;
  }

  /**
   * This method is to be called each time an operation is happening on Runner
   *
   * @throws WaarpDatabaseException
   */
  public void saveStatus() throws WaarpDatabaseException {
    update();
  }

  /**
   * Clear the runner
   */
  public void clear() {
    // nothing
  }

  @Override
  public String toString() {
    final Map<String, Object> map = new HashMap<String, Object>();
    for (final Columns col : Columns.values()) {
      map.put(col.name(), allFields[col.ordinal()].getValue());
    }
    return JsonHandler.writeAsString(map);
  }

  /**
   * @return the start
   */
  public Timestamp getStart() {
    return start;
  }

  /**
   * @return the stop
   */
  public Timestamp getStop() {
    return stop;
  }

  /*
   * XXXIDXXX XXXUSERXXX XXXACCTXXX XXXFILEXXX XXXMODEXXX XXXSTATUSXXX XXXINFOXXX XXXUPINFXXX XXXSTARTXXX
   * XXXSTOPXXX
   */
  private static final String XML_IDX = "IDX";
  private static final String XML_USER = "USER";
  private static final String XML_ACCT = "ACCT";
  private static final String XML_FILE = "FILE";
  private static final String XML_MODE = "MODE";
  private static final String XML_STATUS = "STATUS";
  private static final String XML_INFO = "INFO";
  private static final String XML_UPDINFO = "UPDINFO";
  private static final String XML_START = "START";
  private static final String XML_STOP = "STOP";
  private static final String XML_ROOT = "LOGS";
  private static final String XML_ENTRY = "LOG";
  /**
   * Structure of the Configuration file
   */
  private static final XmlDecl[] logDecls = {
      // identity
      new XmlDecl(XmlType.STRING, XML_IDX),
      new XmlDecl(XmlType.STRING, XML_USER),
      new XmlDecl(XmlType.STRING, XML_ACCT),
      new XmlDecl(XmlType.STRING, XML_FILE),
      new XmlDecl(XmlType.STRING, XML_MODE),
      new XmlDecl(XmlType.STRING, XML_STATUS),
      new XmlDecl(XmlType.STRING, XML_INFO),
      new XmlDecl(XmlType.STRING, XML_UPDINFO),
      new XmlDecl(XmlType.STRING, XML_START),
      new XmlDecl(XmlType.STRING, XML_STOP),
  };
  /**
   * Global Structure for Server Configuration
   */
  private static final XmlDecl[] logsElements = {
      new XmlDecl(XML_ENTRY, XmlType.XVAL, XML_ROOT + '/' + XML_ENTRY, logDecls,
                  true)
  };

  /**
   * @return the associated XmlValue
   */
  private XmlValue[] saveIntoXmlValue() {
    final XmlValue[] values = new XmlValue[logDecls.length];
    for (int i = 0; i < logDecls.length; i++) {
      values[i] = new XmlValue(logDecls[i]);
    }
    try {
      values[0].setFromString(Long.toString(specialId));
      values[1].setFromString(user);
      values[2].setFromString(account);
      values[3].setFromString(filename);
      values[4].setFromString(mode);
      values[5].setFromString(getErrorInfo().reasonPhrase());
      values[6].setFromString(infotransf);
      values[7].setFromString(getUpdatedInfo().name());
      values[8].setFromString(start.toString());
      values[9].setFromString(stop.toString());
    } catch (final InvalidArgumentException e) {
      return null;
    }
    return values;
  }

  /**
   * Save the current DbTransferLog to a file
   *
   * @param filename
   *
   * @return The message for the HTTPS interface
   */
  public String saveDbTransferLog(final String filename) {
    final Document document = XmlUtil.createEmptyDocument();
    final XmlValue[] roots = new XmlValue[1];
    final XmlValue root = new XmlValue(logsElements[0]);
    roots[0] = root;
    String message;
    final XmlValue[] values = saveIntoXmlValue();
    if (values == null) {
      return "Error during export";
    }
    try {
      root.addValue(values);
    } catch (final InvalidObjectException e) {
      logger.error("Error during Write DbTransferLog file: {}", e.getMessage());
      return ERROR_DURING_PURGE;
    }
    try {
      delete();
      message = "Purge Correct Logs successful";
    } catch (final WaarpDatabaseException e) {
      message = ERROR_DURING_PURGE;
    }
    XmlUtil.write(document, roots);
    try {
      XmlUtil.saveDocument(filename, document);
    } catch (final IOException e1) {
      logger.error("Cannot write to file: " + filename + " since {}",
                   e1.getMessage());
      return message + " but cannot save file as export";
    }
    return message;
  }

  /**
   * Export DbTransferLogs to a file and purge the corresponding
   * DbTransferLogs
   *
   * @param preparedStatement the DbTransferLog as SELECT command to
   *     export
   *     (and purge)
   * @param filename the filename where the DbLogs will be exported
   *
   * @return The message for the HTTPS interface
   */
  public static String saveDbTransferLogFile(
      final DbPreparedStatement preparedStatement, final String filename) {
    final Document document = XmlUtil.createEmptyDocument();
    final XmlValue[] roots = new XmlValue[1];
    final XmlValue root = new XmlValue(logsElements[0]);
    roots[0] = root;
    String message;
    try {
      try {
        preparedStatement.executeQuery();
        while (preparedStatement.getNext()) {
          final DbTransferLog log = getFromStatement(preparedStatement);
          final XmlValue[] values = log.saveIntoXmlValue();
          if (values == null) {
            return "Error during export";
          }
          try {
            root.addValue(values);
          } catch (final InvalidObjectException e) {
            logger.error("Error during Write DbTransferLog file: {}",
                         e.getMessage());
            return ERROR_DURING_PURGE;
          }
          log.delete();
        }
        message = "Purge Correct Logs successful";
      } catch (final WaarpDatabaseNoConnectionException e) {
        message = ERROR_DURING_PURGE;
      } catch (final WaarpDatabaseSqlException e) {
        message = ERROR_DURING_PURGE;
      } catch (final WaarpDatabaseException e) {
        message = ERROR_DURING_PURGE;
      }
    } finally {
      preparedStatement.realClose();
    }
    XmlUtil.write(document, roots);
    try {
      XmlUtil.saveDocument(filename, document);
    } catch (final IOException e1) {
      logger.error("Cannot write to file: " + filename + " since {}",
                   e1.getMessage());
      return message + " but cannot save file as export";
    }
    return message;
  }
}
