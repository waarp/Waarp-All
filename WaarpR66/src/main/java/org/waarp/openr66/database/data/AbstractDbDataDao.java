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
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.data.DbValue;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.json.JsonHandler;
import org.waarp.openr66.dao.AbstractDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;

import java.util.Iterator;
import java.util.Map.Entry;

/**
 * Abstract database table implementation
 */
public abstract class AbstractDbDataDao<E> extends AbstractDbData {
  private static final String NO_ROW_FOUND = "No row found";
  public static final String JSON_MODEL = "@model";

  protected boolean isSaved;
  protected E pojo;

  /**
   * Abstract constructor
   */
  protected AbstractDbDataDao() {
    // nothing
  }

  protected abstract String getTable();


  protected abstract AbstractDAO<E> getDao() throws DAOConnectionException;

  protected abstract String getPrimaryKey();

  protected abstract String getPrimaryField();

  /**
   * Change UpdatedInfo status
   *
   * @param info
   */
  public abstract void changeUpdatedInfo(AbstractDbData.UpdatedInfo info);

  /**
   * Test the existence of the current object
   *
   * @return True if the object exists
   *
   * @throws WaarpDatabaseException
   */
  @Override
  public boolean exist() throws WaarpDatabaseException {
    AbstractDAO<E> abstractDAO = null;
    try {
      abstractDAO = getDao();
      return abstractDAO.exist(getPrimaryKey());
    } catch (DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } finally {
      if (abstractDAO != null) {
        abstractDAO.close();
      }
    }
  }

  /**
   * Select object from table
   *
   * @throws WaarpDatabaseException
   */
  @Override
  public void select() throws WaarpDatabaseException {
    AbstractDAO<E> abstractDAO = null;
    try {
      abstractDAO = getDao();
      pojo = abstractDAO.select(getPrimaryKey());
      isSaved = true;
    } catch (DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } catch (DAONoDataException e) {
      throw new WaarpDatabaseNoDataException((e));
    } finally {
      if (abstractDAO != null) {
        abstractDAO.close();
      }
    }
  }

  /**
   * Insert object into table
   *
   * @throws WaarpDatabaseException
   */
  @Override
  public void insert() throws WaarpDatabaseException {
    if (isSaved) {
      return;
    }
    AbstractDAO<E> abstractDAO = null;
    try {
      abstractDAO = getDao();
      abstractDAO.insert(pojo);
      isSaved = true;
    } catch (DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } finally {
      if (abstractDAO != null) {
        abstractDAO.close();
      }
    }
  }

  /**
   * Update object to table
   *
   * @throws WaarpDatabaseException
   */
  @Override
  public void update() throws WaarpDatabaseException {
    if (isSaved) {
      return;
    }
    AbstractDAO<E> abstractDAO = null;
    try {
      abstractDAO = getDao();
      abstractDAO.update(pojo);
      isSaved = true;
    } catch (DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } catch (DAONoDataException e) {
      throw new WaarpDatabaseNoDataException((e));
    } finally {
      if (abstractDAO != null) {
        abstractDAO.close();
      }
    }
  }

  /**
   * Delete object from table
   *
   * @throws WaarpDatabaseException
   */
  @Override
  public void delete() throws WaarpDatabaseException {
    AbstractDAO<E> abstractDAO = null;
    try {
      abstractDAO = getDao();
      abstractDAO.delete(pojo);
      isSaved = false;
    } catch (DAOConnectionException e) {
      throw new WaarpDatabaseNoConnectionException(e);
    } catch (DAONoDataException e) {
      throw new WaarpDatabaseNoDataException((e));
    } finally {
      if (abstractDAO != null) {
        abstractDAO.close();
      }
    }
  }

  /**
   * @return the runner as Json
   */
  @Override
  public String asJson() {
    final ObjectNode node = getJson();
    return JsonHandler.writeAsString(node);
  }

  /**
   * Create the equivalent object in Json (no database access)
   *
   * @return The ObjectNode Json equivalent
   */
  @Override
  public ObjectNode getJson() {
    final ObjectNode node = JsonHandler.createObjectNode();
    node.put(JSON_MODEL, getClass().getSimpleName());
    String json = JsonHandler.writeAsString(pojo);
    ObjectNode subnode = JsonHandler.getFromString(json);
    for (Iterator<Entry<String, JsonNode>> it = subnode.fields();
         it.hasNext(); ) {
      final Entry<String, JsonNode> entry = it.next();
      node.set(entry.getKey(), entry.getValue());
    }
    return node;
  }

  protected abstract void setFromJson(String field, JsonNode value);

  /**
   * Set the values from the Json node to the current object (no database
   * access)
   *
   * @param node
   * @param ignorePrimaryKey True will ignore primaryKey from Json
   *
   * @throws WaarpDatabaseSqlException
   */
  @Override
  public void setFromJson(ObjectNode node, boolean ignorePrimaryKey)
      throws WaarpDatabaseSqlException {
    for (Iterator<Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
      final Entry<String, JsonNode> entry = it.next();
      if ("UPDATEDINFO".equalsIgnoreCase(entry.getKey())) {
        continue;
      }
      if (getPrimaryField().equalsIgnoreCase(entry.getKey())) {
        continue;
      }
      setFromJson(entry.getKey(), entry.getValue());
    }
  }

  @Override
  protected void initObject() {
    // nothing
  }

  @Override
  protected String getWherePrimaryKey() {
    throw new UnsupportedOperationException("Should not be called");
  }

  @Override
  protected void setPrimaryKey() {
    throw new UnsupportedOperationException("Should not be called");
  }

  @Override
  protected String getSelectAllFields() {
    throw new UnsupportedOperationException("Should not be called");
  }

  @Override
  protected String getInsertAllValues() {
    throw new UnsupportedOperationException("Should not be called");
  }

  @Override
  protected String getUpdateAllFields() {
    throw new UnsupportedOperationException("Should not be called");
  }

  @Override
  protected void setToArray() {
    throw new UnsupportedOperationException("Should not be called");
  }

  @Override
  protected void setFromArray() throws WaarpDatabaseSqlException {
    throw new UnsupportedOperationException("Should not be called");
  }

  @Override
  protected void getValues(DbPreparedStatement preparedStatement,
                           DbValue[] values)
      throws WaarpDatabaseNoConnectionException, WaarpDatabaseSqlException {
    throw new UnsupportedOperationException("Should not be called");
  }
}