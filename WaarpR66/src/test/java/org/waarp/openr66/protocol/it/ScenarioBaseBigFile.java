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
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.model.DbModelMysql;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.Processes;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.client.SubmitTransfer;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.junit.TestAbstract;
import org.waarp.openr66.protocol.test.TestRecvThroughClient;
import org.waarp.openr66.protocol.test.TestRecvThroughClient.TestRecvThroughHandler;
import org.waarp.openr66.protocol.test.TestTransferNoDb;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.server.R66Server;
import org.waarp.openr66.server.ServerInitDatabase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.waarp.openr66.protocol.it.ScenarioBase.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class ScenarioBaseBigFile extends TestAbstract {

  protected static ScenarioBaseBigFile scenarioBase;
  private static final ArrayList<DbTaskRunner> dbTaskRunners =
      new ArrayList<DbTaskRunner>();
  private static final String SERVER_1_XML = "R1/conf/server_1_SQLDB.xml";
  private static final String SERVER_1_REWRITTEN_XML = "R1/conf/server.xml";
  private static final String RESOURCES_SERVER_1_XML =
      "it/scenario_big_file_limitbdw/" + SERVER_1_XML;
  static final String SERVER_H2_1_XML = "R1/conf/server_1.xml";
  private static final String SERVER_2_XML = "R2/conf/server_2.xml";

  private static final List<Integer> PIDS = new ArrayList<Integer>();
  private static final String TMP_R66_SCENARIO_R1 =
      "/tmp/R66/scenario_big_file_limitbdw/R1";
  private static final String TMP_R66_CONFIG_R1 =
      "/tmp/R66/scenario_big_file_limitbdw/" + SERVER_1_REWRITTEN_XML;
  public static int NUMBER_FILES = 1;
  public static int LARGE_SIZE = 500000000;
  public static int BANDWIDTH = 10000000;
  public static int BLOCK_SIZE = 1048576;
  public static long MAX_USED_MEMORY = 536870912;

  private static int r66Pid1 = 999999;
  private static int r66Pid2 = 999999;
  public static boolean SERVER1_IN_JUNIT = true;
  private static final String TMP_CONFIG_XML = "/tmp/config.xml";
  private static File configFile = null;
  private long usedMemory;

  public static void setUpBeforeClass() throws Exception {
    if (!SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      ResourceLeakDetector.setLevel(Level.PARANOID);
    } else {
      ResourceLeakDetector.setLevel(Level.SIMPLE);
    }
    final ClassLoader classLoader = ScenarioBaseBigFile.class.getClassLoader();
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
    ClassLoader classLoader = ScenarioBaseBigFile.class.getClassLoader();
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

  public abstract JdbcDatabaseContainer getJDC();

  public static void setUp3DbBeforeClass() throws Exception {
    deleteBase();
    final ClassLoader classLoader = ScenarioBaseBigFile.class.getClassLoader();
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

  private static void checkNotOnlyClient() {
    Assume.assumeTrue(SERVER1_IN_JUNIT);
  }

  private static void checkBigIt() {
    Assume.assumeTrue("If the Long term tests are allowed",
                      SystemPropertyUtil.get(IT_LONG_TEST, false));
    Runtime runtime = Runtime.getRuntime();
    boolean isMemory512 = runtime.totalMemory() <= MAX_USED_MEMORY;
    if (!isMemory512) {
      logger.warn("If the Long term tests are allowed, memory must be 512M: {}",
                  runtime.totalMemory());
    }
  }

  @Before
  public void setUp() throws Exception {
    Configuration.configuration.setTimeoutCon(10000);
    Runtime runtime = Runtime.getRuntime();
    usedMemory = runtime.totalMemory() - runtime.freeMemory();
  }

  private void checkMemory() {
    Runtime runtime = Runtime.getRuntime();
    long newUsedMemory = runtime.totalMemory() - runtime.freeMemory();
    if (newUsedMemory > MAX_USED_MEMORY) {
      logger.warn("Used Memory > 512MB {} {}", usedMemory / 1048576.0,
                  newUsedMemory / 1048576.0);
    }
  }

  private void testBigTransfer(boolean limit, String serverName, boolean direct,
                               boolean recv) throws IOException {
    if (!SERVER1_IN_JUNIT && !direct) {
      logger.warn("Only Direct is enabled in Client mode");
      return;
    }
    Assume.assumeNotNull(networkTransaction);
    if (limit) {
      Configuration.configuration
          .changeNetworkLimit(BANDWIDTH * 2, BANDWIDTH * 2, BANDWIDTH,
                              BANDWIDTH, 1000);
    } else {
      Configuration.configuration.changeNetworkLimit(0, 0, 0, 0, 100000);
    }
    File baseDir;
    if (recv && !"server1".equals(serverName)) {
      baseDir = new File("/tmp/R66/scenario_big_file_limitbdw/R2/out/");
    } else {
      baseDir = new File("/tmp/R66/scenario_big_file_limitbdw/R1/out/");
    }
    File fileOut = new File(baseDir, "hello");
    final File outHello =
        generateOutFile(fileOut.getAbsolutePath(), LARGE_SIZE);
    long timestart = System.currentTimeMillis();
    final R66Future future = new R66Future(true);
    if (direct) {
      if (recv) {
        final TestRecvThroughHandler handler = new TestRecvThroughHandler();
        final TestRecvThroughClient transaction =
            new TestRecvThroughClient(future, handler, serverName, "hello",
                                      "recvthrough", "Test Big RecvThrough",
                                      true, BLOCK_SIZE, networkTransaction);
        transaction.setNormalInfoAsWarn(false);
        transaction.run();
      } else {
        final TestTransferNoDb transaction =
            new TestTransferNoDb(future, serverName, "hello", "sendany",
                                 "Test Big Send", true, BLOCK_SIZE,
                                 DbConstantR66.ILLEGALVALUE,
                                 networkTransaction);
        transaction.setNormalInfoAsWarn(false);
        transaction.run();
      }
      future.awaitOrInterruptible();
      assertTrue(future.isSuccess());
    } else {
      String ruleName = recv? "recv" : "sendany";
      final SubmitTransfer transaction =
          new SubmitTransfer(future, serverName, "hello", ruleName,
                             "Test Big " + ruleName, true, BLOCK_SIZE,
                             DbConstantR66.ILLEGALVALUE, null);
      transaction.run();
      future.awaitOrInterruptible();
      assertTrue(future.isSuccess());
      final DbTaskRunner runner = future.getResult().getRunner();
      if (runner != null) {
        logger.info("Runner: {}", runner.toString());
        waitForAllDone(runner);
        dbTaskRunners.add(runner);
      }
    }
    long timestop = System.currentTimeMillis();
    logger.warn("Direct {}, Recv {}, LimitBandwidth {} " +
                "({} seconds,  {} MBPS vs {} " +
                "and {}) of size {} with block size {}", direct, recv, limit,
                (timestop - timestart) / 1000,
                LARGE_SIZE / 1000.0 / (timestop - timestart),
                Configuration.configuration.getServerGlobalReadLimit() /
                1000000.0,
                Configuration.configuration.getServerChannelReadLimit() /
                1000000.0, LARGE_SIZE, BLOCK_SIZE);
    outHello.delete();
    FileUtils.forceDeleteRecursiveDir(baseDir);
    checkMemory();
  }

  @Test
  public void test01_BigRecvSync() throws IOException, InterruptedException {
    checkBigIt();
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(true, "server2", true, true);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test02_BigSendSync() throws IOException, InterruptedException {
    checkBigIt();
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(true, "server2", true, false);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test03_BigRecvSubmit() throws IOException, InterruptedException {
    checkBigIt();
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(true, "server2", false, true);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test04_BigSendSubmit() throws IOException, InterruptedException {
    checkBigIt();
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(true, "server2", false, false);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test05_BigRecvSyncSsl() throws IOException, InterruptedException {
    checkBigIt();
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(true, "server2-ssl", true, true);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test06_BigSendSyncSsl() throws IOException, InterruptedException {
    checkBigIt();
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(true, "server2-ssl", true, false);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test07_BigRecvSubmitSsl()
      throws IOException, InterruptedException {
    checkBigIt();
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(true, "server2-ssl", false, true);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test08_BigSendSubmitSsl()
      throws IOException, InterruptedException {
    checkBigIt();
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(true, "server2-ssl", false, false);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }


  @Test
  public void test01_BigRecvSyncNoLimitSelf()
      throws IOException, InterruptedException {
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(false, "server1", true, true);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test02_BigSendSyncNoLimitSelf()
      throws IOException, InterruptedException {
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(false, "server1", true, false);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test01_BigRecvSyncNoLimit()
      throws IOException, InterruptedException {
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(false, "server2", true, true);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test02_BigSendSyncNoLimit()
      throws IOException, InterruptedException {
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(false, "server2", true, false);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test03_BigRecvSubmitNoLimit()
      throws IOException, InterruptedException {
    checkNotOnlyClient();
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(false, "server2", false, true);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test04_BigSendSubmitNoLimit()
      throws IOException, InterruptedException {
    checkNotOnlyClient();
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(false, "server2", false, false);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test03_BigRecvSubmitNoLimitSelf()
      throws IOException, InterruptedException {
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(false, "server1", false, true);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test04_BigSendSubmitNoLimitSelf()
      throws IOException, InterruptedException {
    checkNotOnlyClient();
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(false, "server1", false, false);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test05_BigRecvSyncSslNoLimit()
      throws IOException, InterruptedException {
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(false, "server2-ssl", true, true);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test06_BigSendSyncSslNoLimit()
      throws IOException, InterruptedException {
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(false, "server2-ssl", true, false);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test07_BigRecvSubmitSslNoLimit()
      throws IOException, InterruptedException {
    checkNotOnlyClient();
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(false, "server2-ssl", false, true);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test08_BigSendSubmitSslNoLimit()
      throws IOException, InterruptedException {
    checkNotOnlyClient();
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(false, "server2-ssl", false, false);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  private void waitForAllDone(DbTaskRunner runner) {
    while (true) {
      try {
        DbTaskRunner checkedRunner = DbTaskRunner.reloadFromDatabase(runner);
        if (checkedRunner.isAllDone()) {
          logger.warn("DbTaskRunner done");
          return;
        } else if (checkedRunner.isInError()) {
          logger.error("DbTaskRunner in error");
          fail("DbTaskRunner in error");
          return;
        }
        Thread.sleep(100);
      } catch (InterruptedException e) {//NOSONAR
        logger.error("Interrupted", e);
        return;
      } catch (WaarpDatabaseException e) {
        logger.error("Cannot found DbTaskRunner", e);
        return;
      }
    }
  }
}
