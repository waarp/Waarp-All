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

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.pojo.Limit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class StatementExecutor<E> {

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(StatementExecutor.class);
  protected final Connection connection;

  public abstract E getFromResultSet(ResultSet set) throws SQLException,
                                                           DAOConnectionException;

  StatementExecutor(Connection con) {
    connection = con;
  }

  public void setParameters(PreparedStatement stm, Object... values)
      throws SQLException {
    for (int i = 0; i < values.length; i++) {
      stm.setObject(i + 1, values[i]);
    }
  }

  public void executeUpdate(PreparedStatement stm) throws SQLException {
    int res;
    res = stm.executeUpdate();
    if (res < 1) {
      logger.error("Update failed, no record updated.");
      //FIXME should be throw new SQLDataException("Update failed, no record
      // updated.");
    } else {
      logger.debug("{} records updated.", res);
    }
  }

  public void executeAction(PreparedStatement stm) throws SQLException {
    stm.executeUpdate();
  }

  public ResultSet executeQuery(PreparedStatement stm) throws SQLException {
    return stm.executeQuery();
  }

  public void closeStatement(Statement stm) {
    if (stm == null) {
      return;
    }
    try {
      stm.close();
    } catch (final SQLException e) {
      logger.warn("An error occurs while closing the statement.", e);
    }
  }

  public void closeResultSet(ResultSet rs) {
    if (rs == null) {
      return;
    }
    try {
      rs.close();
    } catch (final SQLException e) {
      logger.warn("An error occurs while closing the resultSet.", e);
    }
  }

  public void close() {
    try {
      connection.close();
    } catch (final SQLException e) {
      logger.warn("Cannot properly close the database connection", e);
    }
  }
}
