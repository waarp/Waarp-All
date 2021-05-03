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

package org.waarp.openr66.protocol.it;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.joda.time.DateTime;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runners.MethodSorters;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.DbRequest;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.model.DbModelMysql;
import org.waarp.common.database.model.DbType;
import org.waarp.common.file.FileUtils;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.Processes;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.Filter;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.database.DBTransferDAO;
import org.waarp.openr66.dao.database.DBTransferDAOExplain;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.database.model.DbModelFactoryR66;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.pojo.UpdatedInfo;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.http.restv2.converters.TransferConverter;
import org.waarp.openr66.protocol.junit.TestAbstract;
import org.waarp.openr66.protocol.test.TestRecvThroughClient;
import org.waarp.openr66.protocol.test.TestRecvThroughClient.TestRecvThroughHandler;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.server.R66Server;
import org.waarp.openr66.server.ServerInitDatabase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.waarp.openr66.dao.database.DBTransferDAO.*;
import static org.waarp.openr66.database.data.DbTaskRunner.*;
import static org.waarp.openr66.protocol.it.ScenarioBase.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ExplainRequestIT extends TestAbstract {

  private static final int NB_FILES = 10;
  private static final int NB_THREADS = 20;
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  protected static final Map<String, String> TMPFSMAP =
      new HashMap<String, String>();

  static {
    TMPFSMAP.clear();
    TMPFSMAP.put("/tmp/postgresql/data", "rw");
  }

  @ClassRule
  public static PostgreSQLContainer db =
      (PostgreSQLContainer) new PostgreSQLContainer().withCommand(
          "postgres -c fsync=false -c synchronous_commit=off -c " +
          "full_page_writes=false -c wal_level=minimal -c " +
          "max_wal_senders=0").withTmpFs(TMPFSMAP);

  protected static ExplainRequestIT scenarioBase;
  private static final String SERVER_1_XML = "R1/conf/server_1_SQLDB.xml";
  private static final String SERVER_1_REWRITTEN_XML = "R1/conf/server.xml";
  private static final String RESOURCES_SERVER_1_XML =
      "it/scenario_big_file_limitbdw/" + SERVER_1_XML;
  private static final String SERVER_2_XML = "R2/conf/server_2.xml";

  private static final List<Integer> PIDS = new ArrayList<Integer>();
  private static final String TMP_R66_SCENARIO_R1 =
      "/tmp/R66/scenario_big_file_limitbdw/R1";
  private static final String TMP_R66_CONFIG_R1 =
      "/tmp/R66/scenario_big_file_limitbdw/" + SERVER_1_REWRITTEN_XML;

  private static int r66Pid1 = 999999;
  private static int r66Pid2 = 999999;
  public static boolean SERVER1_IN_JUNIT = true;
  private static final String TMP_CONFIG_XML = "/tmp/config.xml";
  private static File configFile = null;
  private long usedMemory;
  private String followId;

  public JdbcDatabaseContainer getJDC() {
    return db;
  }

  @BeforeClass
  public static void setup() throws Exception {
    logger.warn("START PostGreSQL IT TEST");
    scenarioBase = new ExplainRequestIT();
    setUpBeforeClass();
  }

  public static void setUpBeforeClass() throws Exception {
    if (!SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      ResourceLeakDetector.setLevel(Level.PARANOID);
    } else {
      ResourceLeakDetector.setLevel(Level.SIMPLE);
    }
    final ClassLoader classLoader = ExplainRequestIT.class.getClassLoader();
    File file =
        new File(classLoader.getResource(RESOURCES_SERVER_1_XML).getFile());
    dirResources = file.getParentFile().getParentFile().getParentFile();
    logger.warn(dirResources.getAbsolutePath());
    setUpBeforeClassMinimal(RESOURCES_SERVER_1_XML);
    File r1 = new File(dirResources, "R1/conf");
    createBaseR66Directory(r1, TMP_R66_SCENARIO_R1);
    File r2 = new File(dirResources, "R2/conf");
    createBaseR66Directory(r2, "/tmp/R66/scenario_big_file_limitbdw/R2");
    setUp3DbBeforeClass();
    Configuration.configuration.setTimeoutCon(100);
    String xmx = Processes.contentXmx();
    if (xmx == null || "-Xmx1024m".equalsIgnoreCase(xmx)) {
      Processes.setJvmArgsDefault("-Xms1024m -Xmx1024m ");
    } else if ("-Xmx2048m".equalsIgnoreCase(xmx)) {
      Processes.setJvmArgsDefault("-Xms2048m -Xmx2048m ");
    } else {
      Processes.setJvmArgsDefault("-Xms1024m -Xmx1024m ");
    }
    if (!SERVER1_IN_JUNIT) {
      r66Pid1 = startServer(configFile.getAbsolutePath());
    }
    r66Pid2 = startServer(SERVER_2_XML);
    if (SERVER1_IN_JUNIT) {
      R66Server.main(new String[] { configFile.getAbsolutePath() });
      setUpBeforeClassClient();
    } else {
      setUpBeforeClassClient(configFile.getAbsolutePath());
    }
    Thread.sleep(1000);
    Processes.setJvmArgsDefault(null);
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
  }

  public String getServerConfigFile() {
    if (configFile != null) {
      logger.warn("ConfigFile already set to {]", configFile);
      return configFile.getAbsolutePath();
    }
    logger.warn("Build configFile");
    ClassLoader classLoader = ExplainRequestIT.class.getClassLoader();
    File file =
        new File(classLoader.getResource(RESOURCES_SERVER_1_XML).getFile());
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
      jdbcUrl = "jdbc:oracle:thin:@//localhost:1521/test";
      SysErrLogger.FAKE_LOGGER
          .syserr(jdbcUrl + " while should be something like " + jdbcUrl);
      throw new UnsupportedOperationException(
          "Unsupported Test for Oracle since wrong JDBC driver");
    } else if (driver.equalsIgnoreCase("org.postgresql.Driver")) {
      target = "postgresql";
    } else if (DbModelMysql.MYSQL_DRIVER_JRE6.equalsIgnoreCase(driver) ||
               DbModelMysql.MYSQL_DRIVER_JRE8.equalsIgnoreCase(driver)) {
      target = "mysql";
    } else {
      SysErrLogger.FAKE_LOGGER.syserr("Cannot find driver for " + driver);
    }
    content = content.replace("XXXJDBCXXX", jdbcUrl);
    content = content.replace("XXXDRIVERXXX", target);
    SysErrLogger.FAKE_LOGGER.sysout(getJDC().getDriverClassName());
    SysErrLogger.FAKE_LOGGER.sysout(target);
    File fileTo = new File(TMP_R66_CONFIG_R1);
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
    logger.warn("Config file created at {}", fileTo);
    File copy = new File(TMP_CONFIG_XML);
    try {
      FileUtils.copy(configFile, copy, false, false);
      logger.warn("Copy from {} to {}", configFile, copy);
    } catch (Reply550Exception e) {
      e.printStackTrace();
    }
    logger.warn("Copy from {} to {} and return {}", configFile, copy,
                TMP_R66_CONFIG_R1);
    return TMP_R66_CONFIG_R1;
  }

  public static void setUp3DbBeforeClass() throws Exception {
    deleteBase();
    final ClassLoader classLoader = ExplainRequestIT.class.getClassLoader();
    WaarpSystemUtil.setJunit(true);
    File file =
        new File(classLoader.getResource(RESOURCES_SERVER_1_XML).getFile());
    final String newfile = file.getAbsolutePath().replace("target/test-classes",
                                                          "src/test/resources");
    file = new File(newfile);
    dir = file.getParentFile();
    logger.warn("File {} exists? {}", file, file.isFile());
    homeDir =
        dir.getParentFile().getParentFile().getParentFile().getParentFile()
           .getParentFile().getParentFile().getParentFile();
    logger.warn("Dir {} exists? {}", homeDir, homeDir.isDirectory());

    // global ant project settings
    project = Processes.getProject(homeDir);
    String fileconf = null;
    try {
      fileconf = scenarioBase.getServerConfigFile();
      logger.warn("Config file found {}", fileconf);
      if (fileconf.charAt(0) != '/') {
        configFile = new File(dirResources, fileconf);
      }
    } catch (UnsupportedOperationException e) {
      SysErrLogger.FAKE_LOGGER
          .syserr("Database not supported by this test Start Stop R66", e);
      Assume.assumeNoException(e);
      return;
    }
    File copy = new File(TMP_CONFIG_XML);
    if (copy.exists() && configFile != null && !configFile.exists()) {
      try {
        FileUtils.copy(copy, configFile, false, false);
      } catch (Reply550Exception e) {
        //e.printStackTrace();
      }
    } else {
      logger.warn("Could not find {} or {}", copy, configFile);
    }
    initiateDb(configFile.getAbsolutePath());
    initiateDb(SERVER_2_XML);

    Processes.finalizeProject(project);
  }

  public static void initiateDb(String serverInit) {
    WaarpSystemUtil.setJunit(true);
    final File file;
    final File dir2;
    if (serverInit.charAt(0) == '/') {
      file = new File(serverInit);
      dir2 = dir;
    } else {
      file = new File(dirResources, serverInit);
      dir2 = file.getParentFile();
    }
    logger.warn("File {} exists? {}", file, file.isFile());
    final File fileAuth =
        new File(dir2.getParentFile().getParentFile(), "OpenR66-authent.xml");
    logger.warn("File {} exists? {}", fileAuth, fileAuth.isFile());

    final String[] args = {
        file.getAbsolutePath(), "-initdb", "-dir", dir2.getAbsolutePath(),
        "-auth", fileAuth.getAbsolutePath()
    };
    Processes
        .executeJvm(project, homeDir, ServerInitDatabase.class, args, false);
    Configuration.configuration.setTimeoutCon(100);
    // For debug only ServerInitDatabase.main(args);
  }

  public static int startServer(String serverConfig) throws Exception {
    final File file2;
    if (serverConfig.charAt(0) == '/') {
      file2 = new File(serverConfig);
    } else {
      file2 = new File(dirResources, serverConfig);
    }
    if (file2.exists()) {
      System.err.println("Find server file: " + file2.getAbsolutePath());
      final String[] argsServer = {
          file2.getAbsolutePath()
      };
      // global ant project settings
      project = Processes.getProject(homeDir);
      Processes.executeJvm(project, homeDir, R66Server.class, argsServer, true);
      int pid = Processes
          .getPidOfRunnerCommandLinux("java", R66Server.class.getName(), PIDS);
      PIDS.add(pid);
      logger.warn("Start Done: {}", pid);
      return pid;
    } else {
      System.err.println("Cannot find server file: " + file2.getAbsolutePath());
      fail("Cannot find server file");
      return 999999;
    }
  }

  /**
   * @throws Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    WaarpSystemUtil.stopLogger(true);
    for (int pid : PIDS) {
      Processes.kill(pid, true);
    }
    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    tearDownAfterClassServer();
  }

  @Before
  public void setUp() throws Exception {
    Configuration.configuration.setTimeoutCon(1000);
    Runtime runtime = Runtime.getRuntime();
    usedMemory = runtime.totalMemory() - runtime.freeMemory();
    // Fill DbTaskRunner
    Assume.assumeNotNull(networkTransaction);
    Configuration.configuration.changeNetworkLimit(0, 0, 0, 0, 100000);
    File baseDir;
    baseDir = new File("/tmp/R66/scenario_big_file_limitbdw/R2/out/");
    File fileOut = new File(baseDir, "hello");
    final File outHello = generateOutFile(fileOut.getAbsolutePath(), 10);
    long timestart = System.currentTimeMillis();
    final TestRecvThroughHandler handler = new TestRecvThroughHandler();
    final ExecutorService executorService =
        Executors.newFixedThreadPool(NB_THREADS);
    for (int j = 0; j < NB_THREADS; j++) {
      final Runnable runnable = new Runnable() {
        @Override
        public void run() {
          for (int i = 0; i < NB_FILES; i++) {
            final R66Future future = new R66Future(true);
            final TestRecvThroughClient transaction =
                new TestRecvThroughClient(future, handler, "server2", "hello",
                                          "recvthrough", "Fill RecvThrough",
                                          true, BLOCK_SIZE, networkTransaction);
            transaction.setNormalInfoAsWarn(false);
            transaction.run();
            future.awaitOrInterruptible();
          }
        }
      };
      executorService.execute(runnable);
    }
    final R66Future future = new R66Future(true);
    final TestRecvThroughClient transaction =
        new TestRecvThroughClient(future, handler, "server2", "hello",
                                  "recvthrough", "Fill RecvThrough", true,
                                  BLOCK_SIZE, networkTransaction);
    transaction.setNormalInfoAsWarn(false);
    transaction.run();
    future.awaitOrInterruptible();
    future.getRunner().select();
    followId = future.getRunner().getFollowId();
    assertTrue(ParametersChecker.isNotEmpty(followId));
    Thread.sleep(100);
    executorService.shutdown();
    while (!executorService.awaitTermination(1000, TimeUnit.MILLISECONDS)) {

    }
    long timestop = System.currentTimeMillis();
    logger.warn("{} seconds, {} Transfers/s", (timestop - timestart) / 1000,
                NB_THREADS * NB_FILES * 1000.0 / (timestop - timestart));
    outHello.delete();
  }

  @Test
  public void test06_ExplainTransferSql() throws Exception {
    final TransferDAO daoSrc = DAOFactory.getInstance().getTransferDAO();
    final DBTransferDAOExplain daoExplain =
        new DBTransferDAOExplain((DBTransferDAO) daoSrc);

    logger.warn("Is PostgreSQL {}",
                DbModelFactoryR66.containsDbType(DbType.PostGreSQL));
    logger.warn("Not any of H2, MySQL, MariaDB, Oracle {}", DbModelFactoryR66
        .containsDbType(DbType.H2, DbType.MySQL, DbType.MariaDB,
                        DbType.Oracle));
    SysErrLogger.FAKE_LOGGER.sysout("\tINDEX NAME\tDEFINITION");
    DbRequest dbRequest = new DbRequest(DbConstantR66.admin.getSession());
    dbRequest.select(
        "SELECT indexname, indexdef FROM pg_indexes WHERE tablename not like 'pg%'");
    ResultSet resultSet = dbRequest.getResultSet();
    while (resultSet.next()) {
      SysErrLogger.FAKE_LOGGER.sysout(
          "\t" + resultSet.getString(1) + "\t" + resultSet.getString(2));
    }
    resultSet.close();
    dbRequest.close();
    final String host = Configuration.configuration.getHostId();
    // Standard requests
    logger.warn("Standard Request");
    daoExplain.exist(Long.parseLong(followId), "server1", "server2", "server1");

    logger.warn("Standard Request");
    daoExplain
        .select(Long.parseLong(followId), "server1", "server2", "server1");

    // Insert
    logger.warn("Standard Request");
    final Transfer transfer =
        new Transfer("server2", "rule", 1, false, "file", "info", 3);
    transfer.setRequester("dummy");
    transfer.setOwnerRequest("dummy");
    transfer.setStart(new Timestamp(1112242L));
    transfer.setStop(new Timestamp(DateTime.now().getMillis()));
    transfer.setTransferInfo("transfer info");
    daoExplain.insert(transfer);

    // Update
    logger.warn("Standard Request");
    daoExplain.update(
        new Transfer(0L, "rule", 1, "test", "testOrig", "testInfo", true, 42,
                     true, "server1", "server1", "server2", "transferInfo",
                     Transfer.TASKSTEP.ERRORTASK,
                     Transfer.TASKSTEP.TRANSFERTASK, 27, ErrorCode.CompleteOk,
                     ErrorCode.Unknown, 64, new Timestamp(1122242L),
                     new Timestamp(1123242L), UpdatedInfo.TOSUBMIT));

    // Delete
    logger.warn("Standard Request");
    daoExplain.delete(
        new Transfer(0L, "", 1, "", "", "", false, 0, false, "server1",
                     "server1", "server2", "", Transfer.TASKSTEP.NOTASK,
                     Transfer.TASKSTEP.NOTASK, 0, ErrorCode.Unknown,
                     ErrorCode.Unknown, 0, null, null));

    // FollowId
    logger.warn("Follow Id: {}", followId);
    List<Filter> filters = new ArrayList<Filter>(1);
    filters.add(getFollowIdFilter(followId));
    daoExplain.find(filters, 100);
    // Basic example
    daoExplain.find(filters, DBTransferDAO.TRANSFER_START_FIELD, true, 100);
    List<Transfer> list = daoSrc.find(filters);
    for (Transfer transfer1 : list) {
      logger.warn("{}", JsonHandler.prettyPrint(transfer1));
    }
    // Web
    logger.warn("Web");
    filters = new ArrayList<Filter>(2);
    filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                           Configuration.configuration.getHostId()));
    filters.add(new Filter(DBTransferDAO.STEP_STATUS_FIELD, "=",
                           ErrorCode.Running.getCode()));
    daoExplain.find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, 100);
    filters = new ArrayList<Filter>(2);
    filters.add(
        new Filter(UPDATED_INFO_FIELD, "=", UpdatedInfo.INTERRUPTED.ordinal()));
    filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                           Configuration.configuration.getHostId()));
    daoExplain.find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, 100);
    filters = new ArrayList<Filter>(3);
    filters.add(new Filter(DBTransferDAO.UPDATED_INFO_FIELD, "=",
                           UpdatedInfo.INTERRUPTED.ordinal()));
    filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                           Configuration.configuration.getHostId()));
    filters.add(new Filter(DBTransferDAO.TRANSFER_START_FIELD, "<=",
                           new Timestamp(System.currentTimeMillis())));
    daoExplain.find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, 100);
    filters = new ArrayList<Filter>(3);
    filters.add(new Filter(GLOBAL_STEP_FIELD, "=",
                           Transfer.TASKSTEP.ERRORTASK.ordinal()));
    filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                           Configuration.configuration.getHostId()));
    filters.add(new Filter(DBTransferDAO.TRANSFER_START_FIELD, "<=",
                           new Timestamp(System.currentTimeMillis())));
    daoExplain.find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, 100);
    filters = new ArrayList<Filter>(2);
    filters.add(new Filter(GLOBAL_STEP_FIELD, "=",
                           Transfer.TASKSTEP.ERRORTASK.ordinal()));
    filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                           Configuration.configuration.getHostId()));
    daoExplain.find(filters, DBTransferDAO.TRANSFER_START_FIELD, false, 100);

    // Commander
    logger.warn("Commander");
    filters = new ArrayList<Filter>(3);
    filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                           Configuration.configuration.getHostId()));
    filters.add(new Filter(DBTransferDAO.TRANSFER_START_FIELD, "<=",
                           new Timestamp(System.currentTimeMillis())));
    filters.add(new Filter(DBTransferDAO.UPDATED_INFO_FIELD, "=",
                           org.waarp.openr66.pojo.UpdatedInfo
                               .fromLegacy(AbstractDbData.UpdatedInfo.DONE)
                               .ordinal()));
    daoExplain.find(filters, 100);
    daoExplain.find(filters, DBTransferDAO.TRANSFER_START_FIELD, true, 100);

    // Commander, Web Export, Log Purge
    logger.warn("Commander, Web Export, Log Purge");
    filters = new ArrayList<Filter>();
    filters.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=",
                           Configuration.configuration.getHostId()));
    filters.add(new Filter(DBTransferDAO.UPDATED_INFO_FIELD, "<>",
                           AbstractDbData.UpdatedInfo.DONE.ordinal()));
    filters.add(new Filter(DBTransferDAO.UPDATED_INFO_FIELD, ">",
                           AbstractDbData.UpdatedInfo.UNKNOWN.ordinal()));
    filters.add(new Filter(DBTransferDAO.GLOBAL_LAST_STEP_FIELD, "=",
                           Transfer.TASKSTEP.ALLDONETASK.ordinal()));
    filters.add(new Filter(DBTransferDAO.STEP_STATUS_FIELD, ">",
                           ErrorCode.CompleteOk.getCode()));
    daoExplain.find(filters);

    // Log Purge
    logger.warn("Log Purge");
    DbSession admin = DbConstantR66.admin.getSession();
    Timestamp start = new Timestamp(1);
    Timestamp stop = new Timestamp(DateTime.now().getMillis());
    DbPreparedStatement preparedStatement = DbTaskRunner
        .getFilterPrepareStatement(admin, 100, true, "" + Long.MIN_VALUE, "2",
                                   start, stop, "rule", host, false, false,
                                   false, false, true);//1
    Object[] args = { start, stop, Long.MIN_VALUE, 10000L };
    daoExplain.explain(
        DBTransferDAOExplain.replaceArgs(preparedStatement.toString(), args));
    preparedStatement = DbTaskRunner
        .getFilterPrepareStatement(admin, 100, true, "" + Long.MIN_VALUE, "2",
                                   start, stop, "rule", host, false, false,
                                   true, false, false);//1
    daoExplain.explain(
        DBTransferDAOExplain.replaceArgs(preparedStatement.toString(), args));
    preparedStatement = DbTaskRunner
        .getFilterPrepareStatement(admin, 100, true, null, null, start, stop,
                                   null, null, true, false, false, false,
                                   false);//2
    args = new Object[] { start, stop };
    daoExplain.explain(
        DBTransferDAOExplain.replaceArgs(preparedStatement.toString(), args));
    preparedStatement = DbTaskRunner
        .getFilterPrepareStatement(admin, 100, true, null, null, start, stop,
                                   null, null, false, true, true, true,
                                   false);//2
    args = new Object[] { start, stop };
    daoExplain.explain(
        DBTransferDAOExplain.replaceArgs(preparedStatement.toString(), args));
    preparedStatement = DbTaskRunner
        .getFilterPrepareStatement(admin, 100, true, null, null, null, null,
                                   null, null, false, false, false, false,
                                   true);//2
    daoExplain.explain(preparedStatement.toString());

    // Log export
    logger.warn("Web Log");
    preparedStatement = DbTaskRunner.getLogPrepareStatement(admin, start, stop);
    args = new Object[] { start, stop };
    daoExplain.explain(
        DBTransferDAOExplain.replaceArgs(preparedStatement.toString(), args));
    // Log purge
    String request =
        "DELETE FROM " + table + " WHERE " + " " + Columns.OWNERREQ + " = '" +
        Configuration.configuration.getHostId() + "' " + " AND " + "(" +
        Columns.UPDATEDINFO + " = " +
        AbstractDbData.UpdatedInfo.DONE.ordinal() + " OR " +
        Columns.GLOBALLASTSTEP + " = " + TASKSTEP.ALLDONETASK.ordinal() + ") ";
    daoExplain.explain(request);
    // Log purge & Web
    request =
        "DELETE FROM " + table + " WHERE " + " " + Columns.OWNERREQ + " = '" +
        Configuration.configuration.getHostId() + "' " + " AND " + "(" +
        Columns.UPDATEDINFO + " = " +
        AbstractDbData.UpdatedInfo.DONE.ordinal() + " OR " +
        Columns.GLOBALLASTSTEP + " = " + TASKSTEP.ALLDONETASK.ordinal() + ") " +
        " AND " + Columns.STARTTRANS.name() + " >= ? AND " +
        Columns.STOPTRANS.name() + " <= ? ";
    args = new Object[] { start, stop };
    daoExplain.explain(DBTransferDAOExplain.replaceArgs(request, args));

    // Monitoring and SNMP
    logger.warn("Monitoring & SNMP");
    preparedStatement = DbTaskRunner.getCountInfoPrepareStatement(admin);
    args = new Object[] { start, UpdatedInfo.TOSUBMIT.ordinal() };
    daoExplain.explain(
        DBTransferDAOExplain.replaceArgs(preparedStatement.toString(), args));
    preparedStatement =
        DbTaskRunner.getCountStepPrepareStatement(admin, TASKSTEP.ALLDONETASK);
    args = new Object[] { start, TASKSTEP.ALLDONETASK.ordinal() };
    daoExplain.explain(
        DBTransferDAOExplain.replaceArgs(preparedStatement.toString(), args));
    preparedStatement = DbTaskRunner.getCountStatusPrepareStatement(admin);
    args = new Object[] { start, ErrorCode.CompleteOk.getCode() };
    daoExplain.explain(
        DBTransferDAOExplain.replaceArgs(preparedStatement.toString(), args));
    preparedStatement = DbTaskRunner
        .getCountStatusRunningPrepareStatement(admin, ErrorCode.CompleteOk);
    args = new Object[] { start };
    daoExplain.explain(
        DBTransferDAOExplain.replaceArgs(preparedStatement.toString(), args));
    preparedStatement =
        DbTaskRunner.getCountInOutErrorPrepareStatement(admin, true);
    args = new Object[] { start };
    daoExplain.explain(
        DBTransferDAOExplain.replaceArgs(preparedStatement.toString(), args));
    preparedStatement =
        DbTaskRunner.getCountInOutRunningPrepareStatement(admin, true, false);
    args = new Object[] { start };
    daoExplain.explain(
        DBTransferDAOExplain.replaceArgs(preparedStatement.toString(), args));

    // Startup
    logger.warn("Startup");
    request = "UPDATE RUNNER SET " + Columns.UPDATEDINFO.name() + '=' +
              AbstractDbData.UpdatedInfo.TOSUBMIT.ordinal() + " WHERE " + " " +
              Columns.OWNERREQ + " = '" +
              Configuration.configuration.getHostId() + "' " + " AND (" +
              Columns.UPDATEDINFO.name() + " = " +
              AbstractDbData.UpdatedInfo.RUNNING.ordinal() + " OR " +
              Columns.UPDATEDINFO.name() + " = " +
              AbstractDbData.UpdatedInfo.INTERRUPTED.ordinal() + ")";
    daoExplain.explain(request);

    // Monitor JSON Push API
    logger.warn("Monitor Push API");
    DateTime now = new DateTime();
    Timestamp timestamp = new Timestamp(now.getMillis());
    TransferConverter.Order order = TransferConverter.Order.ascId;
    filters = new ArrayList<Filter>(3);
    filters.add(new Filter(OWNER_REQUEST_FIELD, "=",
                           Configuration.configuration.getHostId()));
    filters
        .add(new Filter(TRANSFER_STOP_FIELD, Filter.BETWEEN, start, timestamp));
    daoExplain.find(filters, order.column, order.ascend);

    // Filter on rule and owner (not standard)
    logger.warn("Not Standards");
    final ArrayList<Filter> map = new ArrayList<Filter>();
    map.add(new Filter(DBTransferDAO.ID_RULE_FIELD, "=", "rule"));
    map.add(new Filter(DBTransferDAO.OWNER_REQUEST_FIELD, "=", "server1"));
    daoExplain.find(map);
    // Get All (not standard)
    daoExplain.getAll();
    // Delete All (not standard)
    daoExplain.deleteAll();
  }

}
