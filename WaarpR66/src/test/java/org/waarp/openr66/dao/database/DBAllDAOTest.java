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

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import io.netty.util.internal.PlatformDependent;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.database.model.DbModelMysql;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.file.FileUtils;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
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
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.pojo.Business;
import org.waarp.openr66.pojo.Host;
import org.waarp.openr66.pojo.Limit;
import org.waarp.openr66.pojo.MultipleMonitor;
import org.waarp.openr66.pojo.Rule;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.pojo.UpdatedInfo;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.junit.TestAbstract;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.json.ShutdownOrBlockJsonPacket;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class DBAllDAOTest extends TestAbstract {

  private static final String TMP_CONFIG_XML = "/tmp/config.xml";
  private Connection con;
  private final DAOFactoryTest factoryTest = new DAOFactoryTest();
  protected File configFile = null;

  static {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
  }

  protected static final Map<String, String> TMPFSMAP =
      new HashMap<String, String>();

  public DAOFactory getDaoFactory() {
    return factoryTest;
  }

  class DAOFactoryTest extends DAOFactory {

    @Override
    public BusinessDAO getBusinessDAO(final boolean isCacheable) throws DAOConnectionException {
      try {
        return new DBBusinessDAO(getConnection());
      } catch (final SQLException e) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
        return null;
      }
    }

    @Override
    public HostDAO getHostDAO(final boolean isCacheable) throws DAOConnectionException {
      try {
        return new DBHostDAO(getConnection());
      } catch (final SQLException e) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
        return null;
      }
    }

    @Override
    public LimitDAO getLimitDAO(final boolean isCacheable) throws DAOConnectionException {
      try {
        return new DBLimitDAO(getConnection());
      } catch (final SQLException e) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
        return null;
      }
    }

    @Override
    public MultipleMonitorDAO getMultipleMonitorDAO(final boolean isCacheable)
        throws DAOConnectionException {
      try {
        return new DBMultipleMonitorDAO(getConnection());
      } catch (final SQLException e) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
        return null;
      }
    }

    @Override
    public RuleDAO getRuleDAO(final boolean isCacheable) throws DAOConnectionException {
      try {
        return new DBRuleDAO(getConnection());
      } catch (final SQLException e) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
        return null;
      }
    }

    @Override
    public TransferDAO getTransferDAO() throws DAOConnectionException {
      try {
        return getDAO(getConnection());
      } catch (final SQLException e) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
        return null;
      }
    }
  }

  public void runScript(String script) {
    try {
      final ScriptRunner runner = new ScriptRunner(con, false, true);
      final URL url =
          Thread.currentThread().getContextClassLoader().getResource(script);
      runner.runScript(new BufferedReader(new FileReader(url.getPath())));
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Before
  public void setUp() {
    try {
      con = getConnection();
      initDB();
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  public abstract Connection getConnection() throws SQLException;

  public abstract void initDB() throws SQLException, WaarpDatabaseSqlException;

  @After
  public void wrapUp() {
    try {
      cleanDB();
      if (con != null) {
        con.close();
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  public abstract void cleanDB() throws SQLException;

  public abstract JdbcDatabaseContainer getJDC();

  public String getServerConfigFile() {
    if (configFile != null) {
      return configFile.getAbsolutePath();
    }
    ClassLoader classLoader = DBAllDAOTest.class.getClassLoader();
    File file = new File(
        classLoader.getResource("Linux/config/config-XXDb.xml").getFile());
    if (!file.exists()) {
      SysErrLogger.FAKE_LOGGER
          .syserr("Cannot find in  " + file.getAbsolutePath());
      fail("Cannot find " + file.getAbsolutePath());
    }
    String content = WaarpStringUtils.readFile(file.getAbsolutePath());
    SysErrLogger.FAKE_LOGGER.sysout(getJDC().getJdbcUrl());
    String driver = getJDC().getDriverClassName();
    String target = "notfound";
    String jdbcUrl = getJDC().getJdbcUrl();
    if (driver.equalsIgnoreCase("org.mariadb.jdbc.Driver")) {
      target = "mariadb";
    } else if (driver.equalsIgnoreCase("org.h2.Driver")) {
      target = "h2";
    } else if (driver.equalsIgnoreCase("oracle.jdbc.OracleDriver")) {
      target = "oracle";
      String shouldJdbcUrl = "jdbc:oracle:thin:@//localhost:1521/test";
      SysErrLogger.FAKE_LOGGER
          .syserr(jdbcUrl + " while should be something like " + shouldJdbcUrl);
      throw new UnsupportedOperationException(
          "Unsupported Test for Oracle since wrong JDBC driver");
    } else if (driver.equalsIgnoreCase("org.postgresql.Driver")) {
      target = "postgresql";
    } else if (DbModelMysql.MYSQL_DRIVER_JRE6.equalsIgnoreCase(driver) ||
               DbModelMysql.MYSQL_DRIVER_JRE8.equalsIgnoreCase(driver)) {
      target = "mysql";
      if (DbModelMysql.MYSQL_DRIVER_JRE8.equalsIgnoreCase(driver) &&
          PlatformDependent.javaVersion() > 8) {
        SysErrLogger.FAKE_LOGGER.syserr(
            "JDBC MySQL not OK using TestContainers under JRE > 8: " + jdbcUrl);
        throw new UnsupportedOperationException(
            "Unsupported Test for MySQL and JRE > 8 using testContainers");
      }
    } else {
      SysErrLogger.FAKE_LOGGER.syserr("Cannot find driver for " + driver);
    }
    content = content.replace("XXXJDBCXXX", jdbcUrl);
    content = content.replace("XXXDRIVERXXX", target);
    SysErrLogger.FAKE_LOGGER.sysout(getJDC().getDriverClassName());
    SysErrLogger.FAKE_LOGGER.sysout(target);
    File fileTo = new File(TMP_R_66_CONF_CONFIG_XML);
    fileTo.getParentFile().mkdirs();
    FileWriter writer = null;
    try {
      writer = new FileWriter(fileTo);
      writer.write(content);
      writer.flush();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (writer != null) {
        FileUtils.close(writer);
      }
    }
    configFile = fileTo;
    File copy = new File(TMP_CONFIG_XML);
    try {
      FileUtils.copy(configFile, copy, false, false);
    } catch (Reply550Exception e) {
      e.printStackTrace();
    }
    return TMP_R_66_CONF_CONFIG_XML;
  }

  @Test
  public void test99_startStopServerR66() throws Exception {
    if (checkXml()) {
      return;
    }
    String fileconf = null;
    try {
      fileconf = getServerConfigFile();
    } catch (UnsupportedOperationException e) {
      SysErrLogger.FAKE_LOGGER.syserr(
          "Database not supported by this test Start Stop R66: " +
          e.getMessage());
      Assume.assumeNoException(e);
      return;
    }
    setUpBeforeClassMinimal(fileconf);
    File copy = new File(TMP_CONFIG_XML);
    if (copy.exists() && configFile != null && !configFile.exists()) {
      try {
        FileUtils.copy(copy, configFile, false, false);
      } catch (Reply550Exception e) {
        //e.printStackTrace();
      }
    }
    String fileserver = fileconf;
    if (!fileconf.startsWith("/")) {
      int pos = fileconf.lastIndexOf('/');
      if (pos >= 0) {
        fileserver = fileconf.substring(pos + 1);
      }
    }
    setUpBeforeClassServer(fileconf, fileserver, true);
    setUpBeforeClassClient(fileserver);
    Configuration.configuration.setTimeoutCon(100);
    final DbHostAuth host = new DbHostAuth("hostas");
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }
    final byte scode = -1;

    // Shutdown server
    logger.warn("Shutdown Server");
    Configuration.configuration.setTimeoutCon(100);
    final R66Future future = new R66Future(true);
    final ShutdownOrBlockJsonPacket node8 = new ShutdownOrBlockJsonPacket();
    node8.setRestartOrBlock(false);
    node8.setShutdownOrBlock(true);
    node8.setKey(FilesystemBasedDigest.passwdCrypt(
        Configuration.configuration.getServerAdminKey()));
    final AbstractLocalPacket valid =
        new JsonCommandPacket(node8, LocalPacketFactory.BLOCKREQUESTPACKET);
    sendInformation(valid, socketServerAddress, future, scode, false,
                    R66FiniteDualStates.SHUTDOWN, true);
    Thread.sleep(200);

    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    ChannelUtils.exit();
  }

  /*******************
   * BUSINESS
   *******************/
  @Test
  public void test01_DeleteAllBusiness() {
    try {
      final BusinessDAO dao = getDaoFactory().getBusinessDAO(false);
      dao.deleteAll();

      checkSql("SELECT * FROM hostconfig");
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test01_DeleteBusiness() {
    try {
      final BusinessDAO dao = getDaoFactory().getBusinessDAO(false);
      dao.delete(new Business("server1", "", "", "", ""));

      checkSql("SELECT * FROM hostconfig where hostid = 'server1'");
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test01_GetAllBusiness() {
    try {
      final BusinessDAO dao = getDaoFactory().getBusinessDAO(false);
      assertEquals(5, dao.getAll().size());
    } catch (final DAOConnectionException e) {
      // Ignore since OK if XML
      if (!checkXml()) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test01_SelectBusiness() {
    try {
      final BusinessDAO dao = getDaoFactory().getBusinessDAO(true);
      final Business business = dao.select("server1");

      assertEquals("joyaux", business.getBusiness());
      assertEquals("marchand", business.getRoles());
      assertEquals("le borgne", business.getAliases());
      assertEquals("misc", business.getOthers());
      assertEquals(UpdatedInfo.NOTUPDATED, business.getUpdatedInfo());

      try {
        dao.select("ghost");
        fail("Should raised an exception");
      } catch (final DAONoDataException e) {
        // Ignore since OK
      } catch (final DAOConnectionException e) {
        // Ignore since OK if XML
        if (!checkXml()) {
          SysErrLogger.FAKE_LOGGER.syserr(e);
          fail(e.getMessage());
        }
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test01_ExistBusiness() {
    try {
      final BusinessDAO dao = getDaoFactory().getBusinessDAO(true);
      assertTrue(dao.exist("server1"));
      assertFalse(dao.exist("ghost"));
    } catch (final DAOConnectionException e) {
      // Ignore since OK if XML
      if (!checkXml()) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test01_InsertBusiness() {
    try {
      final BusinessDAO dao = getDaoFactory().getBusinessDAO(false);
      dao.insert(new Business("chacha", "lolo", "lala", "minou", "ect",
                              UpdatedInfo.TOSUBMIT));

      if (checkXml()) {
        return;
      }
      final ResultSet res = con.createStatement().executeQuery(
          "SELECT COUNT(1) as count FROM hostconfig");
      res.next();
      assertEquals(6, res.getInt("count"));

      final ResultSet res2 = con.createStatement().executeQuery(
          "SELECT * FROM hostconfig WHERE hostid = 'chacha'");
      res2.next();
      assertEquals("chacha", res2.getString("hostid"));
      assertEquals("lolo", res2.getString("business"));
      assertEquals("lala", res2.getString("roles"));
      assertEquals("minou", res2.getString("aliases"));
      assertEquals("ect", res2.getString("others"));
      assertEquals(UpdatedInfo.TOSUBMIT.ordinal(), res2.getInt("updatedInfo"));
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test01_UpdateBusiness() {
    try {
      final BusinessDAO dao = getDaoFactory().getBusinessDAO(false);
      dao.update(new Business("server2", "lolo", "lala", "minou", "ect",
                              UpdatedInfo.RUNNING));
      if (checkXml()) {
        return;
      }
      final ResultSet res = con.createStatement().executeQuery(
          "SELECT * FROM hostconfig WHERE hostid = 'server2'");
      res.next();
      assertEquals("server2", res.getString("hostid"));
      assertEquals("lolo", res.getString("business"));
      assertEquals("lala", res.getString("roles"));
      assertEquals("minou", res.getString("aliases"));
      assertEquals("ect", res.getString("others"));
      assertEquals(UpdatedInfo.RUNNING.ordinal(), res.getInt("updatedInfo"));
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test01_FindBusiness() {
    final ArrayList<Filter> map = new ArrayList<Filter>();
    map.add(new Filter(DBBusinessDAO.BUSINESS_FIELD, "=", "ba"));
    try {
      final BusinessDAO dao = getDaoFactory().getBusinessDAO(false);
      assertEquals(2, dao.find(map).size());
    } catch (final DAOConnectionException e) {
      // Ignore since OK if XML
      if (!checkXml()) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  /*********************
   * HOST
   *********************/

  @Test
  public void test02_DeleteAllHost() {
    try {
      final HostDAO dao = getDaoFactory().getHostDAO(false);
      dao.deleteAll();

      checkSql("SELECT * FROM hosts");
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test02_DeleteHost() {
    try {
      final HostDAO dao = getDaoFactory().getHostDAO(false);
      dao.delete(new Host("server1", "", 666, null, false, false));

      checkSql("SELECT * FROM hosts where hostid = 'server1'");
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test02_GetAllHost() {
    try {
      final HostDAO dao = getDaoFactory().getHostDAO(false);
      assertEquals(3, dao.getAll().size());
    } catch (final DAOConnectionException e) {
      // Ignore since OK if XML
      if (!checkXml()) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test02_SelectHost() {
    try {
      final HostDAO dao = getDaoFactory().getHostDAO(true);
      final Host host = dao.select("server1");

      assertEquals("server1", host.getHostid());
      assertEquals("127.0.0.1", host.getAddress());
      assertEquals(6666, host.getPort());
      // HostKey is tested in Insert and Update
      assertTrue(host.isSSL());
      assertTrue(host.isClient());
      assertTrue(host.isProxified());
      assertFalse(host.isAdmin());
      assertFalse(host.isActive());
      assertEquals(UpdatedInfo.TOSUBMIT, host.getUpdatedInfo());

      try {
        dao.select("ghost");
        fail("Should raised an exception");
      } catch (final DAOConnectionException e) {
        // Ignore since OK if XML
        if (!checkXml()) {
          SysErrLogger.FAKE_LOGGER.syserr(e);
          fail(e.getMessage());
        }
      } catch (final DAONoDataException e) {
        // Ignore since OK
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test02_ExistHost() {
    try {
      final HostDAO dao = getDaoFactory().getHostDAO(true);
      assertTrue(dao.exist("server1"));
      assertFalse(dao.exist("ghost"));
    } catch (final DAOConnectionException e) {
      // Ignore since OK if XML
      if (!checkXml()) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test02_InsertHost() {
    try {
      final HostDAO dao = getDaoFactory().getHostDAO(false);
      dao.insert(new Host("chacha", "address", 666,
                          "aaa".getBytes(WaarpStringUtils.UTF8), false, false));
      if (checkXml()) {
        return;
      }
      final ResultSet res = con.createStatement().executeQuery(
          "SELECT COUNT(1) as count FROM hosts");
      res.next();
      assertEquals(4, res.getInt("count"));

      final ResultSet res2 = con.createStatement().executeQuery(
          "SELECT * FROM hosts WHERE hostid = 'chacha'");
      res2.next();
      assertEquals("chacha", res2.getString("hostid"));
      assertEquals("address", res2.getString("address"));
      assertEquals(666, res2.getInt("port"));
      assertArrayEquals("aaa".getBytes(WaarpStringUtils.UTF8),
                        res2.getBytes("hostkey"));
      assertFalse(res2.getBoolean("isssl"));
      assertFalse(res2.getBoolean("isclient"));
      assertFalse(res2.getBoolean("isproxified"));
      assertTrue(res2.getBoolean("adminrole"));
      assertTrue(res2.getBoolean("isactive"));
      assertEquals(0, res2.getInt("updatedinfo"));
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test02_UpdateHost() {
    try {
      final HostDAO dao = getDaoFactory().getHostDAO(false);
      dao.update(new Host("server2", "address", 666,
                          "password".getBytes(WaarpStringUtils.UTF8), false,
                          false));
      if (checkXml()) {
        return;
      }
      final ResultSet res = con.createStatement().executeQuery(
          "SELECT * FROM hosts WHERE hostid = 'server2'");
      res.next();
      assertEquals("server2", res.getString("hostid"));
      assertEquals("address", res.getString("address"));
      assertEquals(666, res.getInt("port"));
      assertArrayEquals("password".getBytes(WaarpStringUtils.UTF8),
                        res.getBytes("hostkey"));
      assertFalse(res.getBoolean("isssl"));
      assertFalse(res.getBoolean("isclient"));
      assertFalse(res.getBoolean("isproxified"));
      assertTrue(res.getBoolean("adminrole"));
      assertTrue(res.getBoolean("isactive"));
      assertEquals(0, res.getInt("updatedinfo"));
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test02_FindHost() {
    final ArrayList<Filter> map = new ArrayList<Filter>();
    map.add(new Filter(DBHostDAO.ADDRESS_FIELD, "=", "127.0.0.1"));
    try {
      final HostDAO dao = getDaoFactory().getHostDAO(false);
      assertEquals(2, dao.find(map).size());
    } catch (final DAOConnectionException e) {
      // Ignore since OK if XML
      if (!checkXml()) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  /*********************
   * LIMIT
   *********************/

  @Test
  public void test03_DeleteAllLimit() {
    try {
      final LimitDAO dao = getDaoFactory().getLimitDAO(false);
      dao.deleteAll();

      checkSql("SELECT * FROM configuration");
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test03_DeleteLimit() {
    try {
      final LimitDAO dao = getDaoFactory().getLimitDAO(false);
      dao.delete(new Limit("server1", 0L));

      checkSql("SELECT * FROM configuration where hostid = 'server1'");
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test03_GetAllLimit() {
    try {
      final LimitDAO dao = getDaoFactory().getLimitDAO(false);
      assertEquals(3, dao.getAll().size());
    } catch (final DAOConnectionException e) {
      // Ignore since OK if XML
      if (!checkXml()) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test03_SelectLimit() {
    try {
      final LimitDAO dao = getDaoFactory().getLimitDAO(true);
      final Limit limit = dao.select("server1");

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
      } catch (final DAONoDataException e) {
        // Ignore since OK
      } catch (final DAOConnectionException e) {
        // Ignore since OK if XML
        if (!checkXml()) {
          SysErrLogger.FAKE_LOGGER.syserr(e);
          fail(e.getMessage());
        }
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test03_ExistLimit() {
    try {
      final LimitDAO dao = getDaoFactory().getLimitDAO(true);
      assertTrue(dao.exist("server1"));
      assertFalse(dao.exist("ghost"));
    } catch (final DAOConnectionException e) {
      // Ignore since OK if XML
      if (!checkXml()) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test03_InsertLimit() {
    try {
      final LimitDAO dao = getDaoFactory().getLimitDAO(false);
      dao.insert(
          new Limit("chacha", 4L, 1L, 5L, 13L, 12, UpdatedInfo.TOSUBMIT));
      if (checkXml()) {
        return;
      }
      final ResultSet res = con.createStatement().executeQuery(
          "SELECT COUNT(1) as count FROM configuration");
      res.next();
      assertEquals(4, res.getInt("count"));

      final ResultSet res2 = con.createStatement().executeQuery(
          "SELECT * FROM configuration WHERE hostid = 'chacha'");
      res2.next();
      assertEquals("chacha", res2.getString("hostid"));
      assertEquals(4, res2.getLong("delaylimit"));
      assertEquals(1, res2.getLong("readGlobalLimit"));
      assertEquals(5, res2.getLong("writeGlobalLimit"));
      assertEquals(13, res2.getLong("readSessionLimit"));
      assertEquals(12, res2.getLong("writeSessionLimit"));
      assertEquals(UpdatedInfo.TOSUBMIT.ordinal(), res2.getInt("updatedInfo"));
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test03_UpdateLimit() {
    try {
      final LimitDAO dao = getDaoFactory().getLimitDAO(false);
      dao.update(
          new Limit("server2", 4L, 1L, 5L, 13L, 12L, UpdatedInfo.RUNNING));
      if (checkXml()) {
        return;
      }
      final ResultSet res = con.createStatement().executeQuery(
          "SELECT * FROM configuration WHERE hostid = 'server2'");
      res.next();
      assertEquals("server2", res.getString("hostid"));
      assertEquals(4, res.getLong("delaylimit"));
      assertEquals(1, res.getLong("readGlobalLimit"));
      assertEquals(5, res.getLong("writeGlobalLimit"));
      assertEquals(13, res.getLong("readSessionLimit"));
      assertEquals(12, res.getLong("writeSessionLimit"));
      assertEquals(UpdatedInfo.RUNNING.ordinal(), res.getInt("updatedInfo"));
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test03_FindLimit() {
    final ArrayList<Filter> map = new ArrayList<Filter>();
    map.add(new Filter(DBLimitDAO.READ_SESSION_LIMIT_FIELD, ">", 2));
    try {
      final LimitDAO dao = getDaoFactory().getLimitDAO(false);
      assertEquals(2, dao.find(map).size());
    } catch (final DAOConnectionException e) {
      // Ignore since OK if XML
      if (!checkXml()) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  /*********************
   * MULTIPLE MONITOR
   *********************/

  @Test
  public void test04_DeleteAllMultipleMonitor() {
    try {
      final MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO(false);
      if (dao == null) {
        // ignore since XML
        return;
      }
      dao.deleteAll();

      checkSql("SELECT * FROM multiplemonitor");
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test04_DeleteMultipleMonitor() {
    try {
      final MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO(false);
      if (dao == null) {
        // ignore since XML
        return;
      }
      dao.delete(new MultipleMonitor("server1", 0, 0, 0));

      checkSql("SELECT * FROM multiplemonitor where hostid = 'server1'");
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test04_GetAllMultipleMonitor() {
    try {
      final MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO(false);
      if (dao == null) {
        // ignore since XML
        return;
      }
      assertEquals(4, dao.getAll().size());
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test04_SelectMultipleMonitor() {
    try {
      final MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO(true);
      if (dao == null) {
        // ignore since XML
        return;
      }
      final MultipleMonitor multiple = dao.select("server1");

      assertEquals("server1", multiple.getHostid());
      assertEquals(11, multiple.getCountConfig());
      assertEquals(29, multiple.getCountRule());
      assertEquals(18, multiple.getCountHost());
      try {
        dao.select("ghost");
        fail("Should raised an exception");
      } catch (final DAONoDataException e) {
        // Ignore since OK
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test04_ExistMultipleMonitor() {
    try {
      final MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO(true);
      if (dao == null) {
        // ignore since XML
        return;
      }
      assertTrue(dao.exist("server1"));
      assertFalse(dao.exist("ghost"));
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test04_InsertMultipleMonitor() {
    try {
      final MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO(false);
      if (dao == null) {
        // ignore since XML
        return;
      }
      dao.insert(new MultipleMonitor("chacha", 31, 19, 98));

      final ResultSet res = con.createStatement().executeQuery(
          "SELECT COUNT(1) as count FROM multiplemonitor");
      res.next();
      assertEquals(5, res.getInt("count"));

      final ResultSet res2 = con.createStatement().executeQuery(
          "SELECT * FROM multiplemonitor WHERE hostid = 'chacha'");
      res2.next();
      assertEquals("chacha", res2.getString("hostid"));
      assertEquals(98, res2.getInt("countRule"));
      assertEquals(19, res2.getInt("countHost"));
      assertEquals(31, res2.getInt("countConfig"));
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test04_UpdateMultipleMonitor() {
    try {
      final MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO(false);
      if (dao == null) {
        // ignore since XML
        return;
      }
      dao.update(new MultipleMonitor("server2", 31, 19, 98));

      final ResultSet res = con.createStatement().executeQuery(
          "SELECT * FROM multiplemonitor WHERE hostid = 'server2'");
      res.next();
      assertEquals("server2", res.getString("hostid"));
      assertEquals(98, res.getInt("countRule"));
      assertEquals(19, res.getInt("countHost"));
      assertEquals(31, res.getInt("countConfig"));
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test04_FindMultipleMonitor() {
    final ArrayList<Filter> map = new ArrayList<Filter>();
    map.add(new Filter(DBMultipleMonitorDAO.COUNT_CONFIG_FIELD, "=", 0));
    try {
      final MultipleMonitorDAO dao = getDaoFactory().getMultipleMonitorDAO(false);
      if (dao == null) {
        // ignore since XML
        return;
      }
      assertEquals(2, dao.find(map).size());
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  /*********************
   * RULE
   *********************/

  @Test
  public void test05_DeleteAllRule() {
    try {
      final RuleDAO dao = getDaoFactory().getRuleDAO(false);
      dao.deleteAll();

      checkSql("SELECT * FROM rules");
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test05_DeleteRule() {
    try {
      final RuleDAO dao = getDaoFactory().getRuleDAO(false);
      dao.delete(new Rule("default", 1));

      checkSql("SELECT * FROM rules where idrule = 'default'");
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test05_GetAllRule() {
    if (checkXml()) {
      return;
    }
    try {
      final RuleDAO dao = getDaoFactory().getRuleDAO(false);
      assertEquals(3, dao.getAll().size());
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test05_SelectRule() {
    try {
      final RuleDAO dao = getDaoFactory().getRuleDAO(true);
      final Rule rule = dao.select("dummy");

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
      } catch (final DAOConnectionException e) {
        // Ignore since OK if XML
        if (!checkXml()) {
          SysErrLogger.FAKE_LOGGER.syserr(e);
          fail(e.getMessage());
        }
      } catch (final DAONoDataException e) {
        // Ignore since OK
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test05_ExistRule() {
    try {
      final RuleDAO dao = getDaoFactory().getRuleDAO(true);
      assertTrue(dao.exist("dummy"));
      assertFalse(dao.exist("ghost"));
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test05_InsertRule() {
    try {
      final RuleDAO dao = getDaoFactory().getRuleDAO(false);
      dao.insert(new Rule("chacha", 2));
      if (checkXml()) {
        return;
      }
      final ResultSet res = con.createStatement().executeQuery(
          "SELECT COUNT(1) as count FROM rules");
      res.next();
      assertEquals(4, res.getInt("count"));

      final ResultSet res2 = con.createStatement().executeQuery(
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
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test05_UpdateRule() {
    try {
      final RuleDAO dao = getDaoFactory().getRuleDAO(false);
      dao.update(new Rule("dummy", 2));
      if (checkXml()) {
        return;
      }
      final ResultSet res = con.createStatement().executeQuery(
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
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  @Test
  public void test05_FindRule() {
    final ArrayList<Filter> map = new ArrayList<Filter>();
    map.add(new Filter(DBRuleDAO.MODE_TRANS_FIELD, "=", 1));
    try {
      final RuleDAO dao = getDaoFactory().getRuleDAO(false);
      assertEquals(2, dao.find(map).size());
    } catch (final DAOConnectionException e) {
      // Ignore since OK if XML
      if (!checkXml()) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
      }
    } catch (final Exception e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    }
  }

  /*********************
   * TRANSFER
   *********************/

  public abstract TransferDAO getDAO(Connection con)
      throws DAOConnectionException;

  @Test
  public void test06_DeleteAllTransfer() throws Exception {
    final TransferDAO dao = getDAO(getConnection());
    dao.deleteAll();

    checkSql("SELECT * FROM runner");
  }

  @Test
  public void test06_DeleteTransfer() throws Exception {
    final TransferDAO dao = getDAO(getConnection());
    dao.delete(new Transfer(0L, "", 1, "", "", "", false, 0, false, "server1",
                            "server1", "server2", "", Transfer.TASKSTEP.NOTASK,
                            Transfer.TASKSTEP.NOTASK, 0, ErrorCode.Unknown,
                            ErrorCode.Unknown, 0, null, null));
    checkSql("SELECT * FROM runner where specialid = 0");
  }

  @Test
  public void test06_GetAllTransfer() throws Exception {
    final TransferDAO dao = getDAO(getConnection());
    assertEquals(4, dao.getAll().size());
  }

  @Test
  public void test06_SelectTransfer() throws Exception {
    final TransferDAO dao = getDAO(getConnection());
    final Transfer transfer = dao.select(0L, "server1", "server2", "server1");

    assertEquals(0, transfer.getId());
    try {
      dao.select(1L, "server1", "server2", "server1");
      fail("Should raised an exception");
    } catch (final DAONoDataException e) {
      // Ignore since OK
    }
  }

  @Test
  public void test06_ExistTransfer() throws Exception {
    final TransferDAO dao = getDAO(getConnection());
    try {
      assertTrue(dao.exist(0L, "server1", "server2", "server1"));
      assertFalse(dao.exist(1L, "server1", "server2", "server1"));
    } catch (final DAOConnectionException e) {
      // Ignore since OK if XML
      if (!checkXml()) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
      }
    }
  }

  @Test
  public void test06_InsertTransfer() throws Exception {
    final TransferDAO dao = getDAO(getConnection());
    final Transfer transfer =
        new Transfer("server2", "rule", 1, false, "file", "info", 3);
    // Requester and requested are setup manualy
    transfer.setRequester("dummy");
    transfer.setOwnerRequest("dummy");
    transfer.setStart(new Timestamp(1112242L));
    transfer.setStop(new Timestamp(1112342L));
    transfer.setTransferInfo("transfer info");
    dao.insert(transfer);

    if (checkXml()) {
      Transfer transfer1 = dao.select(transfer.getId(), transfer.getRequester(),
                                      transfer.getRequested(),
                                      transfer.getOwnerRequest());
      assertEquals("rule", transfer1.getRule());
      assertEquals(1, transfer1.getTransferMode());
      assertEquals("file", transfer1.getFilename());
      assertEquals("file", transfer1.getOriginalName());
      assertEquals("info", transfer1.getFileInfo());
      assertFalse(transfer1.getIsMoved());
      assertEquals(3, transfer1.getBlockSize());
      return;
    }
    final ResultSet res = con.createStatement().executeQuery(
        "SELECT COUNT(1) as count FROM runner");
    res.next();
    assertEquals(5, res.getInt("count"));

    final ResultSet res2 = con.createStatement().executeQuery(
        "SELECT * FROM runner WHERE idrule = 'rule'");
    res2.next();
    assertEquals("rule", res2.getString("idrule"));
    assertEquals(1, res2.getInt("modetrans"));
    assertEquals("file", res2.getString("filename"));
    assertEquals("file", res2.getString("originalname"));
    assertEquals("info", res2.getString("fileinfo"));
    assertFalse(res2.getBoolean("ismoved"));
    assertEquals(3, res2.getInt("blocksz"));
  }

  @Test
  public void test06_UpdateTransfer() throws Exception {
    final TransferDAO dao = getDAO(getConnection());
    // MySQL JRE11 note: datetime < 1 second 01/01/1970 00:00:01 => DateTime
    // truncated error
    try {
      dao.update(
          new Transfer(0L, "rule", 13, "test", "testOrig", "testInfo", true, 42,
                       true, "server1", "server1", "server2", "transferInfo",
                       Transfer.TASKSTEP.ERRORTASK,
                       Transfer.TASKSTEP.TRANSFERTASK, 27, ErrorCode.CompleteOk,
                       ErrorCode.Unknown, 64, new Timestamp(1122242L),
                       new Timestamp(1123242L), UpdatedInfo.TOSUBMIT));
    } catch (DAOConnectionException e) {
      // Ignore since OK if XML
      if (!checkXml()) {
        SysErrLogger.FAKE_LOGGER.syserr(e);
        fail(e.getMessage());
      }
    }
    if (checkXml()) {
      Transfer transfer1 = dao.select(0, "server1", "server2", "server1");
      assertEquals(0, transfer1.getId());
      assertEquals("rule", transfer1.getRule());
      assertEquals(13, transfer1.getTransferMode());
      assertEquals("test", transfer1.getFilename());
      assertEquals("testOrig", transfer1.getOriginalName());
      assertEquals("testInfo", transfer1.getFileInfo());
      assertEquals("transferInfo", transfer1.getTransferInfo());
      assertTrue(transfer1.getIsMoved());
      assertEquals(42, transfer1.getBlockSize());
      assertTrue(transfer1.getRetrieveMode());
      assertEquals("server1", transfer1.getOwnerRequest());
      assertEquals("server1", transfer1.getRequester());
      assertEquals("server2", transfer1.getRequested());
      assertEquals(Transfer.TASKSTEP.ERRORTASK, transfer1.getGlobalStep());
      assertEquals(Transfer.TASKSTEP.TRANSFERTASK,
                   transfer1.getLastGlobalStep());
      assertEquals(27, transfer1.getStep());
      assertEquals(ErrorCode.CompleteOk, transfer1.getStepStatus());
      assertEquals(ErrorCode.Unknown, transfer1.getInfoStatus());
      assertEquals(64, transfer1.getRank());
      assertEquals(new Timestamp(1122242L), transfer1.getStart());
      assertEquals(new Timestamp(1123242L), transfer1.getStop());
      assertEquals(UpdatedInfo.TOSUBMIT, transfer1.getUpdatedInfo());
      return;
    }

    final ResultSet res = con.createStatement().executeQuery(
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
    assertEquals("transferInfo", res.getString("transferinfo"));
    assertTrue(res.getBoolean("ismoved"));
    assertEquals(42, res.getInt("blocksz"));
    assertTrue(res.getBoolean("retrievemode"));
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
    assertEquals(new Timestamp(1122242L), res.getTimestamp("starttrans"));
    assertEquals(new Timestamp(1123242L), res.getTimestamp("stoptrans"));
    assertEquals(UpdatedInfo.TOSUBMIT.ordinal(), res.getInt("updatedInfo"));
  }

  @Test
  public void test06_FindTransfer() throws Exception {
    if (checkXml()) {
      return;
    }
    final ArrayList<Filter> map = new ArrayList<Filter>();
    map.add(new Filter(DBTransferDAO.ID_RULE_FIELD, "=", "default"));
    map.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=", "server1"));

    final TransferDAO dao = getDAO(getConnection());
    assertEquals(3, dao.find(map).size());
  }

  @Test
  public void test06_FindTransferFollow() throws Exception {
    if (checkXml()) {
      return;
    }
    final TransferDAO dao = getDAO(getConnection());
    Transfer transfer =
        dao.select(-9223372036854775805L, "server1", "server2", "server1");
    logger.warn("Should find this one: {}", JsonHandler.prettyPrint(transfer));
    transfer = dao.select(0L, "server1", "server2", "server1");
    logger.warn("May find this one too: {}", JsonHandler.prettyPrint(transfer));
    final ArrayList<Filter> map = new ArrayList<Filter>();
    map.add(DbTaskRunner.getFollowIdFilter("123456789"));

    int count = dao.find(map).size();
    logger.warn("Should find 1 but possibly 2: {}", count);
    assertTrue(count >= 1);
    map.clear();
    map.add(DbTaskRunner.getFollowIdFilter("1234567890"));
    logger.warn("Must find 1: {}", count);
    assertEquals(1, dao.find(map).size());
  }


  private boolean checkXml() {
    if (con == null) {
      SysErrLogger.FAKE_LOGGER.sysout("XML test: ignore SQL");
      return true;
    }
    return false;
  }

  private void checkSql(String command) {
    if (checkXml()) {
      return;
    }
    Statement stmt = null;
    ResultSet res = null;
    try {
      stmt = con.createStatement();
      res = stmt.executeQuery(command);
      assertFalse(res.next());
    } catch (SQLException e) {
      SysErrLogger.FAKE_LOGGER.syserr(e);
      fail(e.getMessage());
    } finally {
      if (res != null) {
        try {
          res.close();
        } catch (SQLException ignore) {
          SysErrLogger.FAKE_LOGGER.ignoreLog(ignore);
        }
      }
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException ignore) {
          SysErrLogger.FAKE_LOGGER.ignoreLog(ignore);
        }
      }
    }
  }
}
