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

package org.waarp.openr66.dao.database.h2;

import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.openr66.dao.database.DBAllDAOTest;
import org.waarp.openr66.dao.database.DBTransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbH2DAOTest extends DBAllDAOTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();


  private final String createScript = "h2/create.sql";
  private final String populateScript = "h2/populate.sql";
  private final String cleanScript = "h2/clean.sql";

  @Override
  public Connection getConnection() throws SQLException {
    return DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                                       "openr66", "openr66");
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

  @Override
  public JdbcDatabaseContainer getJDC() {
    return null;
  }

  @Override
  public String getServerConfigFile() {
    return "Linux/config/config-H2.xml";
  }

  @Override
  public DBTransferDAO getDAO(Connection con) throws DAOConnectionException {
    return new H2TransferDAO(con);
  }

}
