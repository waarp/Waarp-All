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

package org.waarp.openr66.dao.database;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.dao.BusinessDAO;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.HostDAO;
import org.waarp.openr66.dao.LimitDAO;
import org.waarp.openr66.dao.MultipleMonitorDAO;
import org.waarp.openr66.dao.RuleDAO;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.pojo.Business;
import org.waarp.openr66.pojo.Host;
import org.waarp.openr66.pojo.Limit;
import org.waarp.openr66.pojo.MultipleMonitor;
import org.waarp.openr66.pojo.Rule;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.pojo.UpdatedInfo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;

import static org.junit.Assert.*;

public abstract class DBAllDAOTest {

  private Connection con;
  private DAOFactoryTest factoryTest = new DAOFactoryTest();

  public DAOFactory getDaoFactory() {
    return factoryTest;
  }

  class DAOFactoryTest extends DAOFactory {

    @Override
    public BusinessDAO getBusinessDAO() throws DAOConnectionException {
      try {
        return new DBBusinessDAO(getConnection());
      } catch (SQLException e) {
        fail(e.getMessage());
        return null;
      }
    }

    @Override
    public HostDAO getHostDAO() throws DAOConnectionException {
      try {
        return new DBHostDAO(getConnection());
      } catch (SQLException e) {
        fail(e.getMessage());
        return null;
      }
    }

    @Override
    public LimitDAO getLimitDAO() throws DAOConnectionException {
      try {
        return new DBLimitDAO(getConnection());
      } catch (SQLException e) {
        fail(e.getMessage());
        return null;
      }
    }

    @Override
    public MultipleMonitorDAO getMultipleMonitorDAO()
        throws DAOConnectionException {
      try {
        return new DBMultipleMonitorDAO(getConnection());
      } catch (SQLException e) {
        fail(e.getMessage());
        return null;
      }
    }

    @Override
    public RuleDAO getRuleDAO() throws DAOConnectionException {
      try {
        return new DBRuleDAO(getConnection());
      } catch (SQLException e) {
        fail(e.getMessage());
        return null;
      }
    }

    @Override
    public TransferDAO getTransferDAO() throws DAOConnectionException {
      try {
        return getDAO(getConnection());
      } catch (SQLException e) {
        fail(e.getMessage());
        return null;
      }
    }
  }

  public void runScript(String script) {
    try {
      ScriptRunner runner = new ScriptRunner(con, false, true);
      URL url =
          Thread.currentThread().getContextClassLoader().getResource(script);
      runner.runScript(new BufferedReader(new FileReader(url.getPath())));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Before
  public void setUp() {
    try {
      con = getConnection();
      initDB();
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  public abstract Connection getConnection() throws SQLException;

  public abstract void initDB() throws SQLException;

  @After
  public void wrapUp() {
    try {
      cleanDB();
      if (con != null) {
        con.close();
      }
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  public abstract void cleanDB() throws SQLException;


  /*******************
   * BUSINESS
   *******************/
  @Test
  public void testDeleteAllBusiness() {
    try {
      BusinessDAO dao = getDaoFactory().getBusinessDAO();
      dao.deleteAll();

      ResultSet res = con.createStatement()
                         .executeQuery("SELECT * FROM hostconfig");
      assertEquals(false, res.next());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testDeleteBusiness() {
    try {
      BusinessDAO dao = getDaoFactory().getBusinessDAO();
      dao.delete(new Business("server1", "", "", "", ""));

      ResultSet res = con.createStatement()
                         .executeQuery(
                             "SELECT * FROM hostconfig where hostid = 'server1'");
      assertEquals(false, res.next());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetAllBusiness() {
    try {
      BusinessDAO dao = getDaoFactory().getBusinessDAO();
      assertEquals(5, dao.getAll().size());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testSelectBusiness() {
    try {
      BusinessDAO dao = getDaoFactory().getBusinessDAO();
      Business business = dao.select("server1");

      assertEquals("joyaux", business.getBusiness());
      assertEquals("marchand", business.getRoles());
      assertEquals("le borgne", business.getAliases());
      assertEquals("misc", business.getOthers());
      assertEquals(UpdatedInfo.NOTUPDATED, business.getUpdatedInfo());

      try {
        dao.select("ghost");
        fail("Should raised an exception");
      } catch (DAONoDataException e) {
        // Ignore since OK
      }
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testExistBusiness() {
    try {
      BusinessDAO dao = getDaoFactory().getBusinessDAO();
      assertEquals(true, dao.exist("server1"));
      assertEquals(false, dao.exist("ghost"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }


  @Test
  public void testInsertBusiness() {
    try {
      BusinessDAO dao = getDaoFactory().getBusinessDAO();
      dao.insert(new Business("chacha",
                              "lolo", "lala", "minou", "ect",
                              UpdatedInfo.TOSUBMIT));

      ResultSet res = con.createStatement()
                         .executeQuery(
                             "SELECT COUNT(1) as count FROM hostconfig");
      res.next();
      assertEquals(6, res.getInt("count"));

      ResultSet res2 = con.createStatement()
                          .executeQuery(
                              "SELECT * FROM hostconfig WHERE hostid = 'chacha'");
      res2.next();
      assertEquals("chacha", res2.getString("hostid"));
      assertEquals("lolo", res2.getString("business"));
      assertEquals("lala", res2.getString("roles"));
      assertEquals("minou", res2.getString("aliases"));
      assertEquals("ect", res2.getString("others"));
      assertEquals(UpdatedInfo.TOSUBMIT.ordinal(), res2.getInt("updatedInfo"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testUpdateBusiness() {
    try {
      BusinessDAO dao = getDaoFactory().getBusinessDAO();
      dao.update(new Business("server2",
                              "lolo", "lala", "minou", "ect",
                              UpdatedInfo.RUNNING));

      ResultSet res = con.createStatement()
                         .executeQuery(
                             "SELECT * FROM hostconfig WHERE hostid = 'server2'");
      res.next();
      assertEquals("server2", res.getString("hostid"));
      assertEquals("lolo", res.getString("business"));
      assertEquals("lala", res.getString("roles"));
      assertEquals("minou", res.getString("aliases"));
      assertEquals("ect", res.getString("others"));
      assertEquals(UpdatedInfo.RUNNING.ordinal(), res.getInt("updatedInfo"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }


  @Test
  public void testFindBusiness() {
    ArrayList<Filter> map = new ArrayList<Filter>();
    map.add(new Filter(DBBusinessDAO.BUSINESS_FIELD, "=", "ba"));
    try {
      BusinessDAO dao = getDaoFactory().getBusinessDAO();
      assertEquals(2, dao.find(map).size());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  /*********************
   * HOST
   *********************/

  @Test
  public void testDeleteAllHost() {
    try {
      HostDAO dao = getDaoFactory().getHostDAO();
      dao.deleteAll();

      ResultSet res = con.createStatement()
                         .executeQuery("SELECT * FROM hosts");
      assertEquals(false, res.next());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testDeleteHost() {
    try {
      HostDAO dao = getDaoFactory().getHostDAO();
      dao.delete(new Host("server1", "", 666, null, false, false));

      ResultSet res = con.createStatement()
                         .executeQuery(
                             "SELECT * FROM hosts where hostid = 'server1'");
      assertEquals(false, res.next());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetAllHost() {
    try {
      HostDAO dao = getDaoFactory().getHostDAO();
      assertEquals(3, dao.getAll().size());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testSelectHost() {
    try {
      HostDAO dao = getDaoFactory().getHostDAO();
      Host host = dao.select("server1");

      assertEquals("server1", host.getHostid());
      assertEquals("127.0.0.1", host.getAddress());
      assertEquals(6666, host.getPort());
      //HostKey is tested in Insert and Update
      assertEquals(true, host.isSSL());
      assertEquals(true, host.isClient());
      assertEquals(true, host.isProxified());
      assertEquals(false, host.isAdmin());
      assertEquals(false, host.isActive());
      assertEquals(UpdatedInfo.TOSUBMIT, host.getUpdatedInfo());

      try {
        dao.select("ghost");
        fail("Should raised an exception");
      } catch (DAONoDataException e) {
        // Ignore since OK
      }
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testExistHost() {
    try {
      HostDAO dao = getDaoFactory().getHostDAO();
      assertEquals(true, dao.exist("server1"));
      assertEquals(false, dao.exist("ghost"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }


  @Test
  public void testInsertHost() {
    try {
      HostDAO dao = getDaoFactory().getHostDAO();
      dao.insert(
          new Host("chacha", "address", 666, "aaa".getBytes("utf-8"), false,
                   false));

      ResultSet res = con.createStatement()
                         .executeQuery("SELECT COUNT(1) as count FROM hosts");
      res.next();
      assertEquals(4, res.getInt("count"));

      ResultSet res2 = con.createStatement()
                          .executeQuery(
                              "SELECT * FROM hosts WHERE hostid = 'chacha'");
      res2.next();
      assertEquals("chacha", res2.getString("hostid"));
      assertEquals("address", res2.getString("address"));
      assertEquals(666, res2.getInt("port"));
      assertArrayEquals("aaa".getBytes("utf-8"), res2.getBytes("hostkey"));
      assertEquals(false, res2.getBoolean("isssl"));
      assertEquals(false, res2.getBoolean("isclient"));
      assertEquals(false, res2.getBoolean("isproxified"));
      assertEquals(true, res2.getBoolean("adminrole"));
      assertEquals(true, res2.getBoolean("isactive"));
      assertEquals(0, res2.getInt("updatedinfo"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testUpdateHost() {
    try {
      HostDAO dao = getDaoFactory().getHostDAO();
      dao.update(
          new Host("server2", "address", 666, "password".getBytes("utf-8"),
                   false, false));

      ResultSet res = con.createStatement()
                         .executeQuery(
                             "SELECT * FROM hosts WHERE hostid = 'server2'");
      res.next();
      assertEquals("server2", res.getString("hostid"));
      assertEquals("address", res.getString("address"));
      assertEquals(666, res.getInt("port"));
      assertArrayEquals("password".getBytes("utf-8"), res.getBytes("hostkey"));
      assertEquals(false, res.getBoolean("isssl"));
      assertEquals(false, res.getBoolean("isclient"));
      assertEquals(false, res.getBoolean("isproxified"));
      assertEquals(true, res.getBoolean("adminrole"));
      assertEquals(true, res.getBoolean("isactive"));
      assertEquals(0, res.getInt("updatedinfo"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }


  @Test
  public void testFindHost() {
    ArrayList<Filter> map = new ArrayList<Filter>();
    map.add(new Filter(DBHostDAO.ADDRESS_FIELD, "=", "127.0.0.1"));
    try {
      HostDAO dao = getDaoFactory().getHostDAO();
      assertEquals(2, dao.find(map).size());
    } catch (Exception e) {
      e.printStackTrace();
      fail(e.getMessage());
    }
  }


  /*********************
   * LIMIT
   *********************/

  @Test
  public void testDeleteAllLimit() {
    try {
      LimitDAO dao = getDaoFactory().getLimitDAO();
      dao.deleteAll();

      ResultSet res = con.createStatement()
                         .executeQuery("SELECT * FROM configuration");
      assertEquals(false, res.next());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testDeleteLimit() {
    try {
      LimitDAO dao = getDaoFactory().getLimitDAO();
      dao.delete(new Limit("server1", 0l));

      ResultSet res = con.createStatement()
                         .executeQuery(
                             "SELECT * FROM configuration where hostid = 'server1'");
      assertEquals(false, res.next());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetAllLimit() {
    try {
      LimitDAO dao = getDaoFactory().getLimitDAO();
      assertEquals(3, dao.getAll().size());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testSelectLimit() {
    try {
      LimitDAO dao = getDaoFactory().getLimitDAO();
      Limit limit = dao.select("server1");

      assertEquals("server1", limit.getHostid());
      assertEquals(1, limit.getReadGlobalLimit());
      assertEquals(2, limit.getWriteGlobalLimit());
      assertEquals(3, limit.getReadSessionLimit());
      assertEquals(4, limit.getWriteSessionLimit());
      assertEquals(5, limit.getDelayLimit());
      assertEquals(UpdatedInfo.NOTUPDATED, limit.getUpdatedInfo());

      try {
        dao.select("ghost");
        fail("Should raised an exception");
      } catch (DAONoDataException e) {
        // Ignore since OK
      }
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testExistLimit() {
    try {
      LimitDAO dao = getDaoFactory().getLimitDAO();
      assertEquals(true, dao.exist("server1"));
      assertEquals(false, dao.exist("ghost"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }


  @Test
  public void testInsertLimit() {
    try {
      LimitDAO dao = getDaoFactory().getLimitDAO();
      dao.insert(new Limit("chacha", 4l,
                           1l, 5l, 13l, 12,
                           UpdatedInfo.TOSUBMIT));

      ResultSet res = con.createStatement()
                         .executeQuery(
                             "SELECT COUNT(1) as count FROM configuration");
      res.next();
      assertEquals(4, res.getInt("count"));

      ResultSet res2 = con.createStatement()
                          .executeQuery(
                              "SELECT * FROM configuration WHERE hostid = 'chacha'");
      res2.next();
      assertEquals("chacha", res2.getString("hostid"));
      assertEquals(4, res2.getLong("delaylimit"));
      assertEquals(1, res2.getLong("readGlobalLimit"));
      assertEquals(5, res2.getLong("writeGlobalLimit"));
      assertEquals(13, res2.getLong("readSessionLimit"));
      assertEquals(12, res2.getLong("writeSessionLimit"));
      assertEquals(UpdatedInfo.TOSUBMIT.ordinal(), res2.getInt("updatedInfo"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testUpdateLimit() {
    try {
      LimitDAO dao = getDaoFactory().getLimitDAO();
      dao.update(new Limit("server2", 4l,
                           1l, 5l, 13l, 12l,
                           UpdatedInfo.RUNNING));

      ResultSet res = con.createStatement()
                         .executeQuery(
                             "SELECT * FROM configuration WHERE hostid = 'server2'");
      res.next();
      assertEquals("server2", res.getString("hostid"));
      assertEquals(4, res.getLong("delaylimit"));
      assertEquals(1, res.getLong("readGlobalLimit"));
      assertEquals(5, res.getLong("writeGlobalLimit"));
      assertEquals(13, res.getLong("readSessionLimit"));
      assertEquals(12, res.getLong("writeSessionLimit"));
      assertEquals(UpdatedInfo.RUNNING.ordinal(), res.getInt("updatedInfo"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }


  @Test
  public void testFindLimit() {
    ArrayList<Filter> map = new ArrayList<Filter>();
    map.add(new Filter(DBLimitDAO.READ_SESSION_LIMIT_FIELD, ">", 2));
    try {
      LimitDAO dao = getDaoFactory().getLimitDAO();
      assertEquals(2, dao.find(map).size());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }


  /*********************
   * MULTIPLE MONITOR
   *********************/


  @Test
  public void testDeleteAllMultipleMonitor() {
    try {
      MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO();
      if (dao == null) {
        // ignore since XML
        return;
      }
      dao.deleteAll();

      ResultSet res = con.createStatement()
                         .executeQuery("SELECT * FROM multiplemonitor");
      assertEquals(false, res.next());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testDeleteMultipleMonitor() {
    try {
      MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO();
      if (dao == null) {
        // ignore since XML
        return;
      }
      dao.delete(new MultipleMonitor("server1", 0, 0, 0));

      ResultSet res = con.createStatement()
                         .executeQuery(
                             "SELECT * FROM multiplemonitor where hostid = 'server1'");
      assertEquals(false, res.next());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetAllMultipleMonitor() {
    try {
      MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO();
      if (dao == null) {
        // ignore since XML
        return;
      }
      assertEquals(4, dao.getAll().size());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testSelectMultipleMonitor() {
    try {
      MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO();
      if (dao == null) {
        // ignore since XML
        return;
      }
      MultipleMonitor multiple = dao.select("server1");

      assertEquals("server1", multiple.getHostid());
      assertEquals(11, multiple.getCountConfig());
      assertEquals(29, multiple.getCountRule());
      assertEquals(18, multiple.getCountHost());
      try {
        dao.select("ghost");
        fail("Should raised an exception");
      } catch (DAONoDataException e) {
        // Ignore since OK
      }
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testExistMultipleMonitor() {
    try {
      MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO();
      if (dao == null) {
        // ignore since XML
        return;
      }
      assertEquals(true, dao.exist("server1"));
      assertEquals(false, dao.exist("ghost"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }


  @Test
  public void testInsertMultipleMonitor() {
    try {
      MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO();
      if (dao == null) {
        // ignore since XML
        return;
      }
      dao.insert(new MultipleMonitor("chacha", 31, 19, 98));

      ResultSet res = con.createStatement()
                         .executeQuery(
                             "SELECT COUNT(1) as count FROM multiplemonitor");
      res.next();
      assertEquals(5, res.getInt("count"));

      ResultSet res2 = con.createStatement()
                          .executeQuery(
                              "SELECT * FROM multiplemonitor WHERE hostid = 'chacha'");
      res2.next();
      assertEquals("chacha", res2.getString("hostid"));
      assertEquals(98, res2.getInt("countRule"));
      assertEquals(19, res2.getInt("countHost"));
      assertEquals(31, res2.getInt("countConfig"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testUpdateMultipleMonitor() {
    try {
      MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO();
      if (dao == null) {
        // ignore since XML
        return;
      }
      dao.update(new MultipleMonitor("server2", 31, 19, 98));

      ResultSet res = con.createStatement()
                         .executeQuery(
                             "SELECT * FROM multiplemonitor WHERE hostid = 'server2'");
      res.next();
      assertEquals("server2", res.getString("hostid"));
      assertEquals(98, res.getInt("countRule"));
      assertEquals(19, res.getInt("countHost"));
      assertEquals(31, res.getInt("countConfig"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }


  @Test
  public void testFindMultipleMonitor() {
    ArrayList<Filter> map = new ArrayList<Filter>();
    map.add(new Filter(DBMultipleMonitorDAO.COUNT_CONFIG_FIELD, "=", 0));
    try {
      MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO();
      if (dao == null) {
        // ignore since XML
        return;
      }
      assertEquals(2, dao.find(map).size());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }


  /*********************
   * RULE
   *********************/


  @Test
  public void testDeleteAllRule() {
    try {
      RuleDAO dao = getDaoFactory().getRuleDAO();
      dao.deleteAll();

      ResultSet res = con.createStatement()
                         .executeQuery("SELECT * FROM rules");
      assertEquals(false, res.next());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testDeleteRule() {
    try {
      RuleDAO dao = getDaoFactory().getRuleDAO();
      dao.delete(new Rule("default", 1));

      ResultSet res = con.createStatement()
                         .executeQuery(
                             "SELECT * FROM rules where idrule = 'default'");
      assertEquals(false, res.next());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testGetAllRule() {
    try {
      RuleDAO dao = getDaoFactory().getRuleDAO();
      assertEquals(3, dao.getAll().size());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testSelectRule() {
    try {
      RuleDAO dao = getDaoFactory().getRuleDAO();
      Rule rule = dao.select("dummy");

      assertEquals("dummy", rule.getName());
      assertEquals(1, rule.getMode());
      assertEquals(3, rule.getHostids().size());
      assertEquals("/in", rule.getRecvPath());
      assertEquals("/out", rule.getSendPath());
      assertEquals("/arch", rule.getArchivePath());
      assertEquals("/work", rule.getWorkPath());
      assertEquals(0, rule.getRPreTasks().size());
      assertEquals(1, rule.getRPostTasks().size());
      assertEquals(2, rule.getRErrorTasks().size());
      assertEquals(3, rule.getSPreTasks().size());
      assertEquals(0, rule.getSPostTasks().size());
      assertEquals(0, rule.getSErrorTasks().size());
      assertEquals(UpdatedInfo.UNKNOWN, rule.getUpdatedInfo());
      try {
        dao.select("ghost");
        fail("Should raised an exception");
      } catch (DAONoDataException e) {
        // Ignore since OK
      }
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testExistRule() {
    try {
      RuleDAO dao = getDaoFactory().getRuleDAO();
      assertEquals(true, dao.exist("dummy"));
      assertEquals(false, dao.exist("ghost"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }


  @Test
  public void testInsertRule() {
    try {
      RuleDAO dao = getDaoFactory().getRuleDAO();
      dao.insert(new Rule("chacha", 2));

      ResultSet res = con.createStatement()
                         .executeQuery("SELECT COUNT(1) as count FROM rules");
      res.next();
      assertEquals(4, res.getInt("count"));

      ResultSet res2 = con.createStatement()
                          .executeQuery(
                              "SELECT * FROM rules WHERE idrule = 'chacha'");
      res2.next();
      assertEquals("chacha", res2.getString("idrule"));
      assertEquals(2, res2.getInt("modetrans"));
      assertEquals("<hostids></hostids>", res2.getString("hostids"));
      assertEquals("", res2.getString("recvpath") == null? "" :
          res2.getString("recvpath"));
      assertEquals("", res2.getString("sendpath") == null? "" :
          res2.getString("sendpath"));
      assertEquals("", res2.getString("archivepath") == null? "" :
          res2.getString("archivepath"));
      assertEquals("", res2.getString("workpath") == null? "" :
          res2.getString("workpath"));
      assertEquals("<tasks></tasks>", res2.getString("rpretasks"));
      assertEquals("<tasks></tasks>", res2.getString("rposttasks"));
      assertEquals("<tasks></tasks>", res2.getString("rerrortasks"));
      assertEquals("<tasks></tasks>", res2.getString("spretasks"));
      assertEquals("<tasks></tasks>", res2.getString("sposttasks"));
      assertEquals("<tasks></tasks>", res2.getString("serrortasks"));
      assertEquals(0, res2.getInt("updatedInfo"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }

  @Test
  public void testUpdateRule() {
    try {
      RuleDAO dao = getDaoFactory().getRuleDAO();
      dao.update(new Rule("dummy", 2));

      ResultSet res = con.createStatement()
                         .executeQuery(
                             "SELECT * FROM rules WHERE idrule = 'dummy'");
      res.next();
      assertEquals("dummy", res.getString("idrule"));
      assertEquals(2, res.getInt("modetrans"));
      assertEquals("<hostids></hostids>", res.getString("hostids"));
      assertEquals("", res.getString("recvpath") == null? "" :
          res.getString("recvpath"));
      assertEquals("", res.getString("sendpath") == null? "" :
          res.getString("sendpath"));
      assertEquals("", res.getString("archivepath") == null? "" :
          res.getString("archivepath"));
      assertEquals("", res.getString("workpath") == null? "" :
          res.getString("workpath"));
      assertEquals("<tasks></tasks>", res.getString("rpretasks"));
      assertEquals("<tasks></tasks>", res.getString("rposttasks"));
      assertEquals("<tasks></tasks>", res.getString("rerrortasks"));
      assertEquals("<tasks></tasks>", res.getString("spretasks"));
      assertEquals("<tasks></tasks>", res.getString("sposttasks"));
      assertEquals("<tasks></tasks>", res.getString("serrortasks"));
      assertEquals(0, res.getInt("updatedInfo"));
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }


  @Test
  public void testFindRule() {
    ArrayList<Filter> map = new ArrayList<Filter>();
    map.add(new Filter(DBRuleDAO.MODE_TRANS_FIELD, "=", 1));
    try {
      RuleDAO dao = getDaoFactory().getRuleDAO();
      assertEquals(2, dao.find(map).size());
    } catch (Exception e) {
      fail(e.getMessage());
    }
  }


  /*********************
   * TRANSFER
   *********************/

  @Test
  public void testDeleteAllTransfer() throws Exception {
    TransferDAO dao = getDAO(getConnection());
    dao.deleteAll();

    ResultSet res = con.createStatement()
                       .executeQuery("SELECT * FROM runner");
    assertEquals(false, res.next());
  }

  public abstract TransferDAO getDAO(Connection con) throws
                                                     DAOConnectionException;

  @Test
  public void testDeleteTransfer() throws Exception {
    TransferDAO dao = getDAO(getConnection());
    dao.delete(new Transfer(0l, "", 1, "", "", "", false, 0, false,
                            "server1", "server1", "server2", "",
                            Transfer.TASKSTEP.NOTASK, Transfer.TASKSTEP.NOTASK,
                            0,
                            ErrorCode.Unknown, ErrorCode.Unknown, 0, null,
                            null));

    ResultSet res = con.createStatement()
                       .executeQuery(
                           "SELECT * FROM runner where specialid = 0");
    assertEquals(false, res.next());
  }

  @Test
  public void testGetAllTransfer() throws Exception {
    TransferDAO dao = getDAO(getConnection());
    assertEquals(4, dao.getAll().size());
  }

  @Test
  public void testSelectTransfer() throws Exception {
    TransferDAO dao = getDAO(getConnection());
    Transfer transfer = dao.select(0l, "server1", "server2", "server1");

    assertEquals(0, transfer.getId());
    try {
      dao.select(1l, "server1", "server2", "server1");
      fail("Should raised an exception");
    } catch (DAONoDataException e) {
      // Ignore since OK
    }
  }

  @Test
  public void testExistTransfer() throws Exception {
    TransferDAO dao = getDAO(getConnection());
    assertEquals(true, dao.exist(0l, "server1", "server2", "server1"));
    assertEquals(false, dao.exist(1l, "server1", "server2", "server1"));
  }

  @Test
  public void testInsertTransfer() throws Exception {
    TransferDAO dao = getDAO(getConnection());
    Transfer transfer = new Transfer("server2", "rule", 1, false,
                                     "file", "info", 3);
    // Requester and requested are setup manualy
    transfer.setRequester("dummy");
    transfer.setOwnerRequest("dummy");
    transfer.setStart(new Timestamp(1112242l));
    transfer.setStop(new Timestamp(122l));
    transfer.setTransferInfo("transfer info");
    dao.insert(transfer);

    ResultSet res = con.createStatement()
                       .executeQuery("SELECT COUNT(1) as count FROM runner");
    res.next();
    assertEquals(5, res.getInt("count"));

    ResultSet res2 = con.createStatement()
                        .executeQuery(
                            "SELECT * FROM runner WHERE idrule = 'rule'");
    res2.next();
    assertEquals("rule", res2.getString("idrule"));
    assertEquals(1, res2.getInt("modetrans"));
    assertEquals("file", res2.getString("filename"));
    assertEquals("file", res2.getString("originalname"));
    assertEquals("info", res2.getString("fileinfo"));
    assertEquals(false, res2.getBoolean("ismoved"));
    assertEquals(3, res2.getInt("blocksz"));
  }

  @Test
  public void testUpdateTransfer() throws Exception {
    TransferDAO dao = getDAO(getConnection());

    dao.update(new Transfer(0l, "rule", 13, "test", "testOrig",
                            "testInfo", true, 42, true, "server1", "server1",
                            "server2", "transferInfo",
                            Transfer.TASKSTEP.ERRORTASK,
                            Transfer.TASKSTEP.TRANSFERTASK, 27,
                            ErrorCode.CompleteOk,
                            ErrorCode.Unknown, 64, new Timestamp(192l),
                            new Timestamp(1511l), UpdatedInfo.TOSUBMIT));

    ResultSet res = con.createStatement()
                       .executeQuery(
                           "SELECT * FROM runner WHERE specialid=0 and " +
                           "ownerreq='server1' and requester='server1' and " +
                           "requested='server2'");
    if (!res.next()) {
      fail("Result not found");
    }
    assertEquals(0, res.getLong("specialid"));
    assertEquals("rule", res.getString("idrule"));
    assertEquals(13, res.getInt("modetrans"));
    assertEquals("test", res.getString("filename"));
    assertEquals("testOrig", res.getString("originalname"));
    assertEquals("testInfo", res.getString("fileinfo"));
    assertEquals(true, res.getBoolean("ismoved"));
    assertEquals(42, res.getInt("blocksz"));
    assertEquals(true, res.getBoolean("retrievemode"));
    assertEquals("server1", res.getString("ownerreq"));
    assertEquals("server1", res.getString("requester"));
    assertEquals("server2", res.getString("requested"));
    assertEquals(Transfer.TASKSTEP.ERRORTASK.ordinal(),
                 res.getInt("globalstep"));
    assertEquals(Transfer.TASKSTEP.TRANSFERTASK.ordinal(),
                 res.getInt("globallaststep"));
    assertEquals(27, res.getInt("step"));
    assertEquals(ErrorCode.CompleteOk.code,
                 res.getString("stepstatus").charAt(0));
    assertEquals(ErrorCode.Unknown.code, res.getString("infostatus").charAt(0));
    assertEquals(64, res.getInt("rank"));
    assertEquals(new Timestamp(192l), res.getTimestamp("starttrans"));
    assertEquals(new Timestamp(1511l), res.getTimestamp("stoptrans"));
    assertEquals(UpdatedInfo.TOSUBMIT.ordinal(), res.getInt("updatedInfo"));
  }


  @Test
  public void testFindTransfer() throws Exception {
    ArrayList<Filter> map = new ArrayList<Filter>();
    map.add(new Filter(DBTransferDAO.ID_RULE_FIELD, "=", "default"));
    map.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=", "server1"));

    TransferDAO dao = getDAO(getConnection());
    assertEquals(3, dao.find(map).size());
  }

}

