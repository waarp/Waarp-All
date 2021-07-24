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

package org.waarp.openr66.dao.database.mariadb;

import org.waarp.openr66.dao.database.DBTransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MariaDBTransferDAO extends DBTransferDAO {

  protected static final String SQL_GET_ID =
      "SELECT seq FROM Sequences " + "WHERE name='RUNSEQ' FOR UPDATE";
  private static final String SQL_UPDATE_ID =
      "UPDATE Sequences SET seq = ? " + "WHERE name='RUNSEQ'";

  public MariaDBTransferDAO(final Connection con)
      throws DAOConnectionException {
    super(con);
  }

  @Override
  protected final long getNextId() throws DAOConnectionException {
    PreparedStatement ps = null;
    PreparedStatement ps2 = null;
    ResultSet rs = null;
    try {
      ps = connection.prepareStatement(SQL_GET_ID);
      rs = ps.executeQuery();
      final long res;
      if (rs.next()) {
        res = rs.getLong(1);
        ps2 = connection.prepareStatement(SQL_UPDATE_ID);
        ps2.setLong(1, res + 1);
        ps2.executeUpdate();
        return res;
      } else {
        throw new DAOConnectionException(
            "Error no id available, you should purge the database.");
      }
    } catch (final SQLException e) {
      throw new DAOConnectionException(e);
    } finally {
      try {
        if (rs != null) {
          rs.close();
        }
      } catch (final SQLException e) {
        // ignore
      }
      closeStatement(ps);
      closeStatement(ps2);
    }
  }
}