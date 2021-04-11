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

import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.waarp.common.utility.SingletonUtils;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.openr66.context.ErrorCode;
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
import org.waarp.openr66.pojo.Business;
import org.waarp.openr66.pojo.Host;
import org.waarp.openr66.pojo.Limit;
import org.waarp.openr66.pojo.RuleTask;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.pojo.Transfer.TASKSTEP;
import org.waarp.openr66.pojo.UpdatedInfo;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Not ready for tests
 */
public class DbXmlDAOTest extends DBAllDAOTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  static final String path = "/tmp/R66/arch";
  DAOFactoryTest factoryTest = new DAOFactoryTest();

  @Override
  public DAOFactory getDaoFactory() {
    return factoryTest;
  }

  class DAOFactoryTest extends DAOFactory {

    DAOFactoryTest() {
      XMLTransferDAO.createLruCache(1000, 10000);
      Configuration.configuration.setBaseDirectory("/tmp/R66");
      /*
      Configuration.configuration.setArchivePath("/arch");
      new File(Configuration.configuration.getBaseDirectory() +
               Configuration.configuration.getArchivePath()).mkdirs();
       */
    }

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
    {
      final XMLLimitDAO dao = new XMLLimitDAO(path);
      dao.deleteAll();
      dao.insert(new Limit("server1", 5, 1, 2, 3, 4, UpdatedInfo.valueOf(1)));
      dao.insert(new Limit("server2", 2, 2, 3, 2, 2, UpdatedInfo.valueOf(0)));
      dao.insert(new Limit("server3", 3, 5, 6, 3, 4, UpdatedInfo.valueOf(0)));
    }
    {
      final XMLBusinessDAO dao = new XMLBusinessDAO(path);
      dao.deleteAll();
      dao.insert(
          new Business("server1", "joyaux", "marchand", "le borgne", "misc",
                       UpdatedInfo.NOTUPDATED));
      dao.insert(new Business("server2", "ba", "", "",
                              "<root><version>3.0.9</version></root>",
                              UpdatedInfo.UNKNOWN));
      dao.insert(new Business("server3", "", "", "",
                              "<root><version>3.0.9</version></root>",
                              UpdatedInfo.UNKNOWN));
      dao.insert(new Business("server4", "", "", "",
                              "<root><version>3.0.9</version></root>",
                              UpdatedInfo.UNKNOWN));
      dao.insert(new Business("server5", "", "", "",
                              "<root><version>3.0.9</version></root>",
                              UpdatedInfo.UNKNOWN));
    }

    {
      final XMLHostDAO dao = new XMLHostDAO(path);
      dao.deleteAll();
      dao.insert(new Host("server1", "127.0.0.1", 6666,
                          "303465626439323336346235616136306332396630346461353132616361346265303639646336633661383432653235"
                              .getBytes(), true, true, true, false, false,
                          UpdatedInfo.valueOf(3)));
      dao.insert(new Host("server1-ssl", "127.0.0.1", 6666,
                          "303465626439323336346235616136306332396630346461353132616361346265303639646336633661383432653235"
                              .getBytes(), true, false, false, false, true,
                          UpdatedInfo.valueOf(0)));
      dao.insert(new Host("server2", "127.0.0.1", 6666,
                          "303465626439323336346235616136306332396630346461353132616361346265303639646336633661383432653235"
                              .getBytes(), false, false, false, false, true,
                          UpdatedInfo.valueOf(0)));
    }

    {
      final XMLRuleDAO dao = new XMLRuleDAO(path);
      dao.deleteAll();
      dao.insert(new org.waarp.openr66.pojo.Rule("default", 1, SingletonUtils
          .<String>singletonList(), "/in", "/out", "/arch", "/work",
                                                 SingletonUtils
                                                     .<RuleTask>singletonList(),
                                                 SingletonUtils
                                                     .<RuleTask>singletonList(),
                                                 SingletonUtils
                                                     .<RuleTask>singletonList(),
                                                 SingletonUtils
                                                     .<RuleTask>singletonList(),
                                                 SingletonUtils
                                                     .<RuleTask>singletonList(),
                                                 SingletonUtils
                                                     .<RuleTask>singletonList(),
                                                 UpdatedInfo.UNKNOWN));
      List<String> hosts = new ArrayList<String>();
      hosts.add("blabla");
      hosts.add("blabla2");
      hosts.add("blabla3");
      List<RuleTask> ruleTasks = new ArrayList<RuleTask>();
      ruleTasks.add(new RuleTask("test", "aa", 1));
      List<RuleTask> ruleTasks2 = new ArrayList<RuleTask>();
      ruleTasks2.add(new RuleTask("test", "aa", 1));
      ruleTasks2.add(new RuleTask("test", "aa", 1));
      List<RuleTask> ruleTasks3 = new ArrayList<RuleTask>();
      ruleTasks3.add(new RuleTask("test", "aa", 1));
      ruleTasks3.add(new RuleTask("test", "aa", 1));
      ruleTasks3.add(new RuleTask("test", "aa", 1));
      dao.insert(
          new org.waarp.openr66.pojo.Rule("dummy", 1, hosts, "/in", "/out",
                                          "/arch", "/work", SingletonUtils
                                              .<RuleTask>singletonList(),
                                          ruleTasks, ruleTasks2, ruleTasks3,
                                          SingletonUtils
                                              .<RuleTask>singletonList(),
                                          SingletonUtils
                                              .<RuleTask>singletonList(),
                                          UpdatedInfo.UNKNOWN));
      dao.insert(new org.waarp.openr66.pojo.Rule("dummy2", 3, SingletonUtils
          .<String>singletonList(), "/in", "/out", "/arch", "/work",
                                                 SingletonUtils
                                                     .<RuleTask>singletonList(),
                                                 SingletonUtils
                                                     .<RuleTask>singletonList(),
                                                 SingletonUtils
                                                     .<RuleTask>singletonList(),
                                                 SingletonUtils
                                                     .<RuleTask>singletonList(),
                                                 SingletonUtils
                                                     .<RuleTask>singletonList(),
                                                 SingletonUtils
                                                     .<RuleTask>singletonList(),
                                                 UpdatedInfo.UNKNOWN));
    }

    try {
      final XMLTransferDAO dao = new XMLTransferDAO(path);
      dao.deleteAll();
      DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
      dao.insert(new Transfer(-9223372036854775807L, "default", 1,
                              "data/server1/log/client.log",
                              "data/server1/log/client.log", "noinfo", false,
                              65536, true, "server1", "server1", "server2",
                              "{\"ORIGINALSIZE\":18391}", TASKSTEP.ERRORTASK,
                              TASKSTEP.NOTASK, 0,
                              ErrorCode.ConnectionImpossible,
                              ErrorCode.ConnectionImpossible, 0, new Timestamp(
          dateFormat.parse("2018-06-27 14:31:37.738").getTime()), new Timestamp(
          dateFormat.parse("2018-06-27 14:31:58.042").getTime()),
                              UpdatedInfo.RUNNING));
      dao.insert(new Transfer(-9223372036854775806L, "default", 1,
                              "data/server1/log/client.log",
                              "data/server1/log/client.log", "noinfo", false,
                              65536, true, "server1", "server1", "server2",
                              "{\"ORIGINALSIZE\":52587}", TASKSTEP.ERRORTASK,
                              TASKSTEP.NOTASK, 0,
                              ErrorCode.ConnectionImpossible,
                              ErrorCode.ConnectionImpossible, 0, new Timestamp(
          dateFormat.parse("2018-06-27 14:36:00.116").getTime()), new Timestamp(
          dateFormat.parse("2018-06-27 14:36:20.374").getTime()),
                              UpdatedInfo.INERROR));
      dao.insert(
          new Transfer(-9223372036854775805L, "tintin", 1, "tintin", "tintin",
                       "noinfo", false, 65536, true, "server1", "server1",
                       "server2", "{\"ORIGINALSIZE\":-1}", TASKSTEP.ERRORTASK,
                       TASKSTEP.NOTASK, 0, ErrorCode.ConnectionImpossible,
                       ErrorCode.ConnectionImpossible, 0, new Timestamp(
              dateFormat.parse("2018-06-22 14:39:01.116").getTime()),
                       new Timestamp(dateFormat.parse("2018-06-22 14:39:21.374")
                                               .getTime()),
                       UpdatedInfo.INERROR));
      dao.insert(
          new Transfer(0, "tintin", 1, "tintin", "tintin", "noinfo", false,
                       65536, true, "server1", "server1", "server2",
                       "{\"ORIGINALSIZE\":-1}", TASKSTEP.ERRORTASK,
                       TASKSTEP.NOTASK, 0, ErrorCode.ConnectionImpossible,
                       ErrorCode.ConnectionImpossible, 0, new Timestamp(
              dateFormat.parse("2018-06-24 14:39:01.116").getTime()),
                       new Timestamp(dateFormat.parse("2018-06-24 14:39:21.374")
                                               .getTime()),
                       UpdatedInfo.INERROR));
    } catch (final DAOConnectionException e) {
      e.printStackTrace();
    } catch (ParseException e) {
      e.printStackTrace();
    }

  }

  @Override
  public void cleanDB() {
    // No Clean
  }

  @Override
  public JdbcDatabaseContainer getJDC() {
    return null;
  }

  @Override
  public TransferDAO getDAO(Connection con) throws DAOConnectionException {
    return new XMLTransferDAO(path);
  }

}
