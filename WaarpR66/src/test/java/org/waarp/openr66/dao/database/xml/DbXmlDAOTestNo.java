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

package org.waarp.openr66.dao.database.xml;

import org.waarp.openr66.dao.BusinessDAO;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.HostDAO;
import org.waarp.openr66.dao.LimitDAO;
import org.waarp.openr66.dao.MultipleMonitorDAO;
import org.waarp.openr66.dao.RuleDAO;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.database.DBAllDAOTest;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.xml.XMLBusinessDAO;
import org.waarp.openr66.dao.xml.XMLHostDAO;
import org.waarp.openr66.dao.xml.XMLLimitDAO;
import org.waarp.openr66.dao.xml.XMLRuleDAO;
import org.waarp.openr66.dao.xml.XMLTransferDAO;
import org.waarp.openr66.pojo.Host;
import org.waarp.openr66.pojo.Limit;
import org.waarp.openr66.pojo.UpdatedInfo;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.*;

/**
 * Not ready for tests
 */
public abstract class DbXmlDAOTestNo extends DBAllDAOTest {
  static final String path = "/tmp/R66/arch";
  DAOFactoryTest factoryTest = new DAOFactoryTest();

  @Override
  public DAOFactory getDaoFactory() {
    return factoryTest;
  }

  class DAOFactoryTest extends DAOFactory {

    @Override
    public BusinessDAO getBusinessDAO() throws DAOConnectionException {
      return new XMLBusinessDAO(path);
    }

    @Override
    public HostDAO getHostDAO() throws DAOConnectionException {
      return new XMLHostDAO(path);
    }

    @Override
    public LimitDAO getLimitDAO() throws DAOConnectionException {
      return new XMLLimitDAO(path);
    }

    @Override
    public MultipleMonitorDAO getMultipleMonitorDAO()
        throws DAOConnectionException {
      return null;
    }

    @Override
    public RuleDAO getRuleDAO() throws DAOConnectionException {
      return new XMLRuleDAO(path);
    }

    @Override
    public TransferDAO getTransferDAO() throws DAOConnectionException {
      try {
        return getDAO(getConnection());
      } catch (final SQLException e) {
        fail(e.getMessage());
        return null;
      }
    }
  }

  @Override
  public Connection getConnection() throws SQLException {
    // No connection
    return null;
  }

  @Override
  public void initDB() {
    // Init through Map
    try {
      final XMLLimitDAO dao = new XMLLimitDAO(path);
      dao.deleteAll();
      dao.insert(new Limit("server1", 1, 2, 3, 4, 5, UpdatedInfo.valueOf(1)));
      dao.insert(new Limit("server2", 2, 3, 2, 2, 2, UpdatedInfo.valueOf(0)));
      dao.insert(new Limit("server3", 5, 6, 3, 4, 3, UpdatedInfo.valueOf(0)));
    } catch (final DAOConnectionException e) {
      e.printStackTrace();
    }
    try {
      final XMLBusinessDAO dao = new XMLBusinessDAO(path);
      dao.deleteAll();
    } catch (final DAOConnectionException e) {
      e.printStackTrace();
    }
    /*
     * INSERT INTO hostconfig VALUES ('joyaux', 'marchand', 'le borgne', 'misc', 1, 'server1'); INSERT INTO
     * hostconfig VALUES ('ba', '', '', '<root><version>3.0.9</version></root>', 0, 'server2'); INSERT INTO
     * hostconfig VALUES ('ba', '', '', '<root><version>3.0.9</version></root>', 0, 'server3'); INSERT INTO
     * hostconfig VALUES ('', '', '', '<root><version>3.0.9</version></root>', 0, 'server4'); INSERT INTO
     * hostconfig VALUES ('', '', '', '<root><version>3.0.9</version></root>', 0, 'server5');
     */

    try {
      final XMLHostDAO dao = new XMLHostDAO(path);
      dao.deleteAll();
      dao.insert(new Host("server1", "127.0.0.1", 6666,
                          "303465626439323336346235616136306332396630346461353132616361346265303639646336633661383432653235"
                              .getBytes(), true, false, true, false, true,
                          UpdatedInfo.valueOf(3)));
      dao.insert(new Host("server1-ssl", "127.0.0.1", 6666,
                          "303465626439323336346235616136306332396630346461353132616361346265303639646336633661383432653235"
                              .getBytes(), true, false, false, true, false,
                          UpdatedInfo.valueOf(0)));
      dao.insert(new Host("server2", "127.0.0.1", 6666,
                          "303465626439323336346235616136306332396630346461353132616361346265303639646336633661383432653235"
                              .getBytes(), false, false, false, true, false,
                          UpdatedInfo.valueOf(0)));
    } catch (final DAOConnectionException e) {
      e.printStackTrace();
    }

    try {
      final XMLRuleDAO dao = new XMLRuleDAO(path);
      dao.deleteAll();
    } catch (final DAOConnectionException e) {
      e.printStackTrace();
    }
    /*
     * INSERT INTO rules VALUES ('<hostids></hostids>', 1, '/in', '/out', '/arch', '/work', '<tasks></tasks>',
     * '<tasks></tasks>', '<tasks></tasks>', '<tasks></tasks>', '<tasks></tasks>', '<tasks></tasks>', 0,
     * 'default'); INSERT INTO rules VALUES
     * ('<hostids><hostid>blabla</hostid><hostid>blabla2</hostid><hostid>blabla3</hostid></hostids>', 1, '/in',
     * '/out', '/arch', '/work', '<tasks></tasks>',
     * '<tasks><task><type>test</type><path>aa</path><delay>1</delay></task></tasks>',
     * '<tasks><task><type>test</type><path>aa</path><delay>1</delay></task><task><type>test</type><path>aa</path>
     * <delay>1</delay></task></tasks>',
     * '<tasks><task><type>test</type><path>aa</path><delay>1</delay></task><task><type>test</type><path>aa</path>
     * <delay>1</delay></task><task><type>test</type><path>aa</path><delay>1</delay></task></tasks>',
     * '<tasks></tasks>', '<tasks></tasks>', 42, 'dummy'); INSERT INTO rules VALUES ('<hostids></hostids>', 3,
     * '/in', '/out', '/arch', '/work', '<tasks></tasks>', '<tasks></tasks>', '<tasks></tasks>',
     * '<tasks></tasks>', '<tasks></tasks>', '<tasks></tasks>', 0, 'dummy2');
     */

    try {
      final XMLTransferDAO dao = new XMLTransferDAO(path);
      dao.deleteAll();
    } catch (final DAOConnectionException e) {
      e.printStackTrace();
    }
    /*
     * INSERT INTO runner VALUES (5, 0, 0, 0, 'C ', true, 'data/server1/log/client.log', false, 'default', 65536,
     * 'data/server1/log/client.log', 'noinfo', '{"ORIGINALSIZE":18391}', 1, '2018-06-27 14:31:37.738',
     * '2018-06-27 14:31:58.042', 'C ', 5, 'server1', 'server1', 'server2', -9223372036854775807); INSERT INTO
     * runner VALUES (5, 0, 0, 0, 'C ', true, 'data/server1/log/client.log', false, 'default', 65536,
     * 'data/server1/log/client.log', 'noinfo', '{"ORIGINALSIZE":52587}', 1, '2018-06-20 14:36:00.116',
     * '2018-06-20 14:36:20.374', 'C ', 4, 'server1', 'server1', 'server2', -9223372036854775806); INSERT INTO
     * runner VALUES (5, 0, 0, 0, 'C ', true, 'tintin', false, 'tintin', 65536, 'tintin', 'noinfo',
     * '{"ORIGINALSIZE":-1}', 1, '2018-06-22 14:39:01.28', '2018-06-22 14:39:21.518', 'C ', 4, 'server1',
     * 'server1', 'server2', -9223372036854775805); INSERT INTO runner VALUES (5, 0, 0, 0, 'C ', true, 'tintin',
     * false, 'default', 65536, 'tintin', 'noinfo', '{"ORIGINALSIZE":-1}', 1, '2018-06-24 14:39:01.28',
     * '2018-06-24 14:39:21.518', 'C ', 4, 'server1', 'server1', 'server2', 0);
     */

  }

  @Override
  public void cleanDB() {
    // No Clean
  }

  @Override
  public TransferDAO getDAO(Connection con) throws DAOConnectionException {
    return new XMLTransferDAO(path);
  }

}
