/*******************************************************************************
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 *  individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at your
 *  option) any later version.
 *
 *  Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 *  A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  Waarp . If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.waarp.openr66.dao.database.oracle;

import org.junit.ClassRule;
import org.testcontainers.containers.OracleContainer;
import org.waarp.openr66.dao.database.DBAllDAOTest;
import org.waarp.openr66.dao.database.DBTransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBOracleDBDAOTest extends DBAllDAOTest {

  @ClassRule
  public static OracleContainer db =
      new OracleContainer("epiclabs/docker-oracle-xe-11g");
  private String createScript = "oracle/create.sql";
  private String populateScript = "oracle/populate.sql";
  private String cleanScript = "oracle/clean.sql";

  @Override
  public DBTransferDAO getDAO(Connection con) throws DAOConnectionException {
    return new OracleTransferDAO(con);
  }

  @Override
  public Connection getConnection() throws SQLException {
    return DriverManager.getConnection(
        db.getJdbcUrl(),
        db.getUsername(),
        db.getPassword());
  }

  @Override
  public void initDB() {
    runScript(createScript);
    runScript(populateScript);
  }

  @Override
  public void cleanDB() {
    runScript(cleanScript);
  }
}

