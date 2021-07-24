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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.database.model.DbModelAbstract;
import org.waarp.common.json.JsonHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * Abstract database table implementation without explicit COMMIT.<br>
 * <br>
 * <p>
 * If the connection is in autocommit, this is the right abstract to
 * extend.<br>
 * If the connection is not in autocommit, one could use this implementation to
 * explicitly commit when needed.
 */
public abstract class AbstractDbData {
  private static final String NO_ROW_FOUND = "No row found";
  public static final String JSON_MODEL = "@model";

  /**
   * UpdatedInfo status
   */
  public enum UpdatedInfo {
    /**
     * Unknown run status
     */
    UNKNOWN,
    /**
     * Not updated run status
     */
    NOTUPDATED,
    /**
     * Interrupted status (stop or cancel)
     */
    INTERRUPTED,
    /**
     * Updated run status meaning ready to be submitted
     */
    TOSUBMIT,
    /**
     * In error run status
     */
    INERROR,
    /**
     * Running status
     */
    RUNNING,
    /**
     * All done run status
     */
    DONE
  }

  /**
   * To be implemented
   */
  // public static String table
  // public static final int NBPRKEY
  // protected static String selectAllFields
  // protected static String updateAllFields
  // protected static String insertAllValues
  protected DbValue[] primaryKey;
  protected DbValue[] otherFields;
  protected DbValue[] allFields;

  protected boolean isSaved;
  /**
   * The DbSession to use
   */
  protected final DbSession dbSession;

  /**
   * Abstract constructor to set the DbSession to use
   *
   * @param dbSession Deprecated for Waarp R66
   */
  protected AbstractDbData(final DbSession dbSession) {
    this.dbSession = dbSession;
    initObject();
  }

  /**
   * Abstract constructor to set the DbSession to use
   */
  protected AbstractDbData() {
    dbSession = null;
    initObject();
  }

  /**
   * To setup primaryKey, otherFields, allFields. Note this initObject is
   * called
   * within constructor of
   * AbstractDbData. Be careful that no data is actually initialized at this
   * stage.
   */
  protected abstract void initObject();

  /**
   * @return The Where condition on Primary Key
   */
  protected abstract String getWherePrimaryKey();

  /**
   * Set the primary Key as current value
   */
  protected abstract void setPrimaryKey();

  protected abstract String getSelectAllFields();

  protected abstract String getTable();

  protected abstract String getInsertAllValues();

  protected abstract String getUpdateAllFields();

  /**
   * Test the existence of the current object
   *
   * @return True if the object exists
   *
   * @throws WaarpDatabaseException
   */
  public boolean exist() throws WaarpDatabaseException {
    if (dbSession == null) {
      return false;
    }
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(dbSession);
    try {
      preparedStatement.createPrepareStatement(
          "SELECT " + primaryKey[0].getColumn() + " FROM " + getTable() +
          " WHERE " + getWherePrimaryKey());
      setPrimaryKey();
      setValues(preparedStatement, primaryKey);
      preparedStatement.executeQuery();
      return preparedStatement.getNext();
    } finally {
      preparedStatement.realClose();
    }
  }

  /**
   * Select object from table
   *
   * @throws WaarpDatabaseException
   */
  public void select() throws WaarpDatabaseException {
    if (dbSession == null) {
      throw new WaarpDatabaseNoDataException(NO_ROW_FOUND);
    }
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(dbSession);
    try {
      preparedStatement.createPrepareStatement(
          "SELECT " + getSelectAllFields() + " FROM " + getTable() + " WHERE " +
          getWherePrimaryKey());
      setPrimaryKey();
      setValues(preparedStatement, primaryKey);
      preparedStatement.executeQuery();
      if (preparedStatement.getNext()) {
        getValues(preparedStatement, allFields);
        isSaved = true;
      } else {
        throw new WaarpDatabaseNoDataException(NO_ROW_FOUND);
      }
    } finally {
      preparedStatement.realClose();
    }
  }

  /**
   * Insert object into table
   *
   * @throws WaarpDatabaseException
   */
  public void insert() throws WaarpDatabaseException {
    if (isSaved) {
      return;
    }
    if (dbSession == null) {
      isSaved = true;
      return;
    }
    setToArray();
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(dbSession);
    try {
      preparedStatement.createPrepareStatement(
          "INSERT INTO " + getTable() + " (" + getSelectAllFields() +
          ") VALUES " + getInsertAllValues());
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
   * Update object to table
   *
   * @throws WaarpDatabaseException
   */
  public void update() throws WaarpDatabaseException {
    if (isSaved) {
      return;
    }
    if (dbSession == null) {
      isSaved = true;
      return;
    }
    setToArray();
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(dbSession);
    try {
      preparedStatement.createPrepareStatement(
          "UPDATE " + getTable() + " SET " + getUpdateAllFields() + " WHERE " +
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

  public final DbValue[] getAllFields() {
    return allFields;
  }

  /**
   * Delete object from table
   *
   * @throws WaarpDatabaseException
   */
  public void delete() throws WaarpDatabaseException {
    if (dbSession == null) {
      return;
    }
    final DbPreparedStatement preparedStatement =
        new DbPreparedStatement(dbSession);
    try {
      preparedStatement.createPrepareStatement(
          "DELETE FROM " + getTable() + " WHERE " + getWherePrimaryKey());
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

  /**
   * Change UpdatedInfo status
   *
   * @param info
   */
  public abstract void changeUpdatedInfo(UpdatedInfo info);

  /**
   * Internal function to set to Array used to push data to database
   */
  protected abstract void setToArray() throws WaarpDatabaseSqlException;

  /**
   * Internal function to retrieve data from Array to pull data from database
   *
   * @throws WaarpDatabaseSqlException
   */
  protected abstract void setFromArray() throws WaarpDatabaseSqlException;

  /**
   * Validate Byte array max length
   *
   * @param values the values to check against Types.VARBINARY
   *
   * @throws WaarpDatabaseSqlException if length is not acceptable
   */
  public static void validateLength(final byte[]... values)
      throws WaarpDatabaseSqlException {
    for (final byte[] value : values) {
      if (value != null && value.length > DbModelAbstract.MAX_BINARY * 2) {
        throw new WaarpDatabaseSqlException(
            "BINARY value exceed max size: " + value.length + " (" +
            DbModelAbstract.MAX_BINARY + ")");
      }
    }
  }

  /**
   * Validate String max length
   *
   * @param type between Types.VARCHAR, NVARCHAR, LONGVARCHAR
   * @param values the values to check against same type
   *
   * @throws WaarpDatabaseSqlException if length is not acceptable
   */
  public static void validateLength(final int type, final String... values)
      throws WaarpDatabaseSqlException {
    for (final String value : values) {
      if (value == null) {
        continue;
      }
      switch (type) {
        case Types.VARCHAR:
          if (value.length() > DbModelAbstract.MAX_VARCHAR) {
            throw new WaarpDatabaseSqlException(
                "VARCHAR value exceed max size: " + value.length() + " (" +
                DbModelAbstract.MAX_VARCHAR + ")");
          }
          break;
        case Types.NVARCHAR:
          if (value.length() > DbModelAbstract.MAX_KEY_VARCHAR) {
            throw new WaarpDatabaseSqlException(
                "VARCHAR as KEY value exceed max size: " + value.length() +
                " (" + DbModelAbstract.MAX_KEY_VARCHAR + ")");
          }
          break;
        case Types.LONGVARCHAR:
          if (value.length() > DbModelAbstract.MAX_LONGVARCHAR) {
            throw new WaarpDatabaseSqlException(
                "LONGVARCHAR value exceed max size: " + value.length() + " (" +
                DbModelAbstract.MAX_LONGVARCHAR + ")");
          }
          break;
        default:
          break;
      }
    }
  }

  /**
   * Set Value into PreparedStatement
   *
   * @param ps
   * @param value
   * @param rank >= 1
   *
   * @throws WaarpDatabaseSqlException
   */
  public static void setTrueValue(final PreparedStatement ps,
                                  final DbValue value, final int rank)
      throws WaarpDatabaseSqlException {
    try {
      final String svalue;
      switch (value.type) {
        case Types.VARCHAR:
          if (value.getValue() == null) {
            ps.setNull(rank, Types.VARCHAR);
            break;
          }
          svalue = (String) value.getValue();
          validateLength(Types.VARCHAR, svalue);
          ps.setString(rank, svalue);
          break;
        case Types.NVARCHAR:
          if (value.getValue() == null) {
            ps.setNull(rank, Types.VARCHAR);
            break;
          }
          svalue = (String) value.getValue();
          validateLength(Types.NVARCHAR, svalue);
          ps.setString(rank, svalue);
          break;
        case Types.LONGVARCHAR:
          if (value.getValue() == null) {
            ps.setNull(rank, Types.LONGVARCHAR);
            break;
          }
          svalue = (String) value.getValue();
          validateLength(Types.LONGVARCHAR, svalue);
          ps.setString(rank, svalue);
          break;
        case Types.BIT:
          if (value.getValue() == null) {
            ps.setNull(rank, Types.BIT);
            break;
          }
          ps.setBoolean(rank, (Boolean) value.getValue());
          break;
        case Types.TINYINT:
          if (value.getValue() == null) {
            ps.setNull(rank, Types.TINYINT);
            break;
          }
          ps.setByte(rank, (Byte) value.getValue());
          break;
        case Types.SMALLINT:
          if (value.getValue() == null) {
            ps.setNull(rank, Types.SMALLINT);
            break;
          }
          ps.setShort(rank, (Short) value.getValue());
          break;
        case Types.INTEGER:
          if (value.getValue() == null) {
            ps.setNull(rank, Types.INTEGER);
            break;
          }
          ps.setInt(rank, (Integer) value.getValue());
          break;
        case Types.BIGINT:
          if (value.getValue() == null) {
            ps.setNull(rank, Types.BIGINT);
            break;
          }
          ps.setLong(rank, (Long) value.getValue());
          break;
        case Types.REAL:
          if (value.getValue() == null) {
            ps.setNull(rank, Types.REAL);
            break;
          }
          ps.setFloat(rank, (Float) value.getValue());
          break;
        case Types.DOUBLE:
          if (value.getValue() == null) {
            ps.setNull(rank, Types.DOUBLE);
            break;
          }
          ps.setDouble(rank, (Double) value.getValue());
          break;
        case Types.VARBINARY:
          if (value.getValue() == null) {
            ps.setNull(rank, Types.VARBINARY);
            break;
          }
          final byte[] bvalue = (byte[]) value.getValue();
          validateLength(bvalue);
          ps.setBytes(rank, bvalue);
          break;
        case Types.DATE:
          if (value.getValue() == null) {
            ps.setNull(rank, Types.DATE);
            break;
          }
          ps.setDate(rank, (Date) value.getValue());
          break;
        case Types.TIMESTAMP:
          if (value.getValue() == null) {
            ps.setNull(rank, Types.TIMESTAMP);
            break;
          }
          ps.setTimestamp(rank, (Timestamp) value.getValue());
          break;
        case Types.CLOB:
          if (value.getValue() == null) {
            ps.setNull(rank, Types.CLOB);
            break;
          }
          ps.setClob(rank, (Reader) value.getValue());
          break;
        case Types.BLOB:
          if (value.getValue() == null) {
            ps.setNull(rank, Types.BLOB);
            break;
          }
          ps.setBlob(rank, (InputStream) value.getValue());
          break;
        default:
          throw new WaarpDatabaseSqlException(
              "Type not supported: " + value.type + " at " + rank);
      }
    } catch (final ClassCastException e) {
      throw new WaarpDatabaseSqlException(
          "Setting values casting error: " + value.type + " at " + rank, e);
    } catch (final SQLException e) {
      DbSession.error(e);
      throw new WaarpDatabaseSqlException(
          "Setting values in error: " + value.type + " at " + rank, e);
    }
  }

  /**
   * Set several values to a DbPreparedStatement
   *
   * @param preparedStatement
   * @param values
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  protected final void setValues(final DbPreparedStatement preparedStatement,
                                 final DbValue[] values)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final PreparedStatement ps = preparedStatement.getPreparedStatement();
    for (int i = 0; i < values.length; i++) {
      final DbValue value = values[i];
      setTrueValue(ps, value, i + 1);
    }
  }

  /**
   * Get one value into DbValue from ResultSet
   *
   * @param rs
   * @param value
   *
   * @throws WaarpDatabaseSqlException
   */
  public static void getTrueValue(final ResultSet rs, final DbValue value)
      throws WaarpDatabaseSqlException {
    try {
      switch (value.type) {
        case Types.VARCHAR:
        case Types.NVARCHAR:
        case Types.LONGVARCHAR:
          value.setValue(rs.getString(value.getColumn()));
          break;
        case Types.BIT:
          value.setValue(rs.getBoolean(value.getColumn()));
          break;
        case Types.TINYINT:
          value.setValue(rs.getByte(value.getColumn()));
          break;
        case Types.SMALLINT:
          value.setValue(rs.getShort(value.getColumn()));
          break;
        case Types.INTEGER:
          value.setValue(rs.getInt(value.getColumn()));
          break;
        case Types.BIGINT:
          value.setValue(rs.getLong(value.getColumn()));
          break;
        case Types.REAL:
          value.setValue(rs.getFloat(value.getColumn()));
          break;
        case Types.DOUBLE:
          value.setValue(rs.getDouble(value.getColumn()));
          break;
        case Types.VARBINARY:
          value.setValue(rs.getBytes(value.getColumn()));
          break;
        case Types.DATE:
          value.setValue(rs.getDate(value.getColumn()));
          break;
        case Types.TIMESTAMP:
          value.setValue(rs.getTimestamp(value.getColumn()));
          break;
        case Types.CLOB:
          value.setValue(rs.getClob(value.getColumn()).getCharacterStream());
          break;
        case Types.BLOB:
          value.setValue(rs.getBlob(value.getColumn()).getBinaryStream());
          break;
        default:
          throw new WaarpDatabaseSqlException(
              "Type not supported: " + value.type + " for " +
              value.getColumn());
      }
    } catch (final SQLException e) {
      DbSession.error(e);
      throw new WaarpDatabaseSqlException(
          "Getting values in error: " + value.type + " for " +
          value.getColumn(), e);
    }
  }

  /**
   * Get several values into DbValue from DbPreparedStatement
   *
   * @param preparedStatement
   * @param values
   *
   * @throws WaarpDatabaseNoConnectionException
   * @throws WaarpDatabaseSqlException
   */
  protected void getValues(final DbPreparedStatement preparedStatement,
                           final DbValue[] values)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    final ResultSet rs = preparedStatement.getResultSet();
    for (final DbValue value : values) {
      getTrueValue(rs, value);
    }
    setFromArray();
  }

  /**
   * @return the object as Json
   */
  public String asJson() {
    final ObjectNode node = getJson();
    return JsonHandler.writeAsString(node);
  }

  /**
   * Create the equivalent object in Json (no database access)
   *
   * @return The ObjectNode Json equivalent
   */
  public ObjectNode getJson() {
    final ObjectNode node = JsonHandler.createObjectNode();
    node.put(JSON_MODEL, getClass().getSimpleName());
    for (final DbValue value : allFields) {
      switch (value.type) {
        case Types.VARCHAR:
        case Types.NVARCHAR:
        case Types.LONGVARCHAR:
          node.put(value.getColumn(), (String) value.getValue());
          break;
        case Types.BIT:
          node.put(value.getColumn(), (Boolean) value.getValue());
          break;
        case Types.TINYINT:
          node.put(value.getColumn(), (Byte) value.getValue());
          break;
        case Types.SMALLINT:
          node.put(value.getColumn(), (Short) value.getValue());
          break;
        case Types.INTEGER:
          node.put(value.getColumn(), (Integer) value.getValue());
          break;
        case Types.BIGINT:
          node.put(value.getColumn(), (Long) value.getValue());
          break;
        case Types.REAL:
          node.put(value.getColumn(), (Float) value.getValue());
          break;
        case Types.DOUBLE:
          node.put(value.getColumn(), (Double) value.getValue());
          break;
        case Types.VARBINARY:
          node.put(value.getColumn(), (byte[]) value.getValue());
          break;
        case Types.DATE:
          node.put(value.getColumn(), ((Date) value.getValue()).getTime());
          break;
        case Types.TIMESTAMP:
          node.put(value.getColumn(), ((Timestamp) value.getValue()).getTime());
          break;
        case Types.CLOB:
        case Types.BLOB:
        default:
          node.put(value.getColumn(), "Unsupported type=" + value.type);
      }
    }
    return node;
  }

  /**
   * Set the values from the Json node to the current object (no database
   * access)
   *
   * @param node
   * @param ignorePrimaryKey True will ignore primaryKey from Json
   *
   * @throws WaarpDatabaseSqlException
   */
  public void setFromJson(final ObjectNode node, final boolean ignorePrimaryKey)
      throws WaarpDatabaseSqlException {
    DbValue[] list = allFields;
    if (ignorePrimaryKey) {
      list = otherFields;
    }
    for (final DbValue value : list) {
      if ("UPDATEDINFO".equalsIgnoreCase(value.getColumn())) {
        continue;
      }
      final JsonNode item = node.get(value.getColumn());
      if (item != null && !item.isMissingNode() && !item.isNull()) {
        isSaved = false;
        switch (value.type) {
          case Types.VARCHAR:
          case Types.NVARCHAR:
          case Types.LONGVARCHAR:
            final String svalue = item.asText();
            validateLength(value.type, svalue);
            value.setValue(svalue);
            break;
          case Types.BIT:
            value.setValue(item.asBoolean());
            break;
          case Types.TINYINT:
          case Types.SMALLINT:
          case Types.INTEGER:
            value.setValue(item.asInt());
            break;
          case Types.BIGINT:
            value.setValue(item.asLong());
            break;
          case Types.REAL:
          case Types.DOUBLE:
            value.setValue(item.asDouble());
            break;
          case Types.VARBINARY:
            try {
              final byte[] bvalue = item.binaryValue();
              validateLength(bvalue);
              value.setValue(bvalue);
            } catch (final IOException e) {
              throw new WaarpDatabaseSqlException(
                  "Issue while assigning array of bytes", e);
            }
            break;
          case Types.DATE:
            value.setValue(new Date(item.asLong()));
            break;
          case Types.TIMESTAMP:
            value.setValue(new Timestamp(item.asLong()));
            break;
          case Types.CLOB:
          case Types.BLOB:
          default:
            throw new WaarpDatabaseSqlException(
                "Unsupported type: " + value.type);
        }
      }
    }
    setFromArray();
  }
}
