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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.apache.commons.exec.ExecuteException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.After;
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
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.common.utility.Processes;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.client.SubmitTransfer;
import org.waarp.openr66.client.TransferArgs;
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
public abstract class ScenarioBaseLoop extends TestAbstract {

  protected static ScenarioBaseLoop scenarioBase;
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
  public static int BANDWIDTH = 100000000;
  public static int BLOCK_SIZE = 65536;
  public static long MAX_USED_MEMORY = 2048 * 1024 * 1024;

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
    final ClassLoader classLoader = ScenarioBaseLoop.class.getClassLoader();
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
    Configuration.configuration.setTimeoutCon(30000);
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
      Configuration.configuration.setTimeoutCon(30000);
    } else {
      setUpBeforeClassClient(configFile.getAbsolutePath());
    }
    Processes.setJvmArgsDefault(null);
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    // Thread.sleep(10000);// To enable Profiling
  }

  public String getServerConfigFile() {
    if (configFile != null) {
      logger.warn("ConfigFile already set to {]", configFile);
      return configFile.getAbsolutePath();
    }
    logger.warn("Build configFile");
    ClassLoader classLoader = ScenarioBaseLoop.class.getClassLoader();
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
    final ClassLoader classLoader = ScenarioBaseLoop.class.getClassLoader();
    DetectionUtils.setJunit(true);
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
    DetectionUtils.setJunit(true);
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
    Configuration.configuration.setTimeoutCon(30000);
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
    if (NUMBER_FILES == -1) {
      Configuration.configuration.setTimeoutCon(100);
      WaarpLoggerFactory.setLogLevel(WaarpLogLevel.ERROR);
      for (int pid : PIDS) {
        Processes.kill(pid, true);
      }
      tearDownAfterClassClient();
      tearDownAfterClassMinimal();
      tearDownAfterClassServer();
      return;
    }
    CloseableHttpClient httpClient = null;
    int max = SystemPropertyUtil.get(IT_LONG_TEST, false)? 60 : 20;
    if (max < NUMBER_FILES) {
      max = NUMBER_FILES * 3;
    }
    int totalTransfers = max;
    int nb = 0;
    int every10sec = 10;
    HttpGet request =
            new HttpGet("http://127.0.0.1:8098/v2/transfers?limit=100000");
    final long startTime = System.currentTimeMillis();
    try {
      httpClient = HttpClientBuilder.create().setConnectionManagerShared(true)
                                    .disableAutomaticRetries().build();
      CloseableHttpResponse response = null;
      try {
        response = httpClient.execute(request);
        assertEquals(200, response.getStatusLine().getStatusCode());
        String content = EntityUtils.toString(response.getEntity());
        ObjectNode node = JsonHandler.getFromString(content);
        if (node != null) {
          JsonNode number = node.findValue("totalResults");
          int newNb = number.asInt();
          max += newNb;
          totalTransfers = max;
          logger.warn("Found {} transfers", newNb);
        }
      } finally {
        if (response != null) {
          response.close();
        }
      }
      while (nb < max) {
        try {
          response = httpClient.execute(request);
          assertEquals(200, response.getStatusLine().getStatusCode());
          String content = EntityUtils.toString(response.getEntity());
          ObjectNode node = JsonHandler.getFromString(content);
          if (node != null) {
            JsonNode number = node.findValue("totalResults");
            int newNb = number.asInt();
            if (newNb != nb || every10sec == 0) {
              every10sec = 10;
              nb = newNb;
              logger.warn("Found {} transfers", nb);
            }
            every10sec--;
          }
        } finally {
          if (response != null) {
            response.close();
          }
        }
        Thread.sleep(1000);
      }
    } catch (ExecuteException e) {
      // ignore
    } finally {
      if (httpClient != null) {
        httpClient.close();
      }
    }
    final long stopTime = System.currentTimeMillis();
    totalTransfers -= nb;
    logger.warn("Duration {} for {} item so {} items/s", (stopTime - startTime) / 1000.0, totalTransfers,
            totalTransfers / ((stopTime - startTime) / 1000.0));
    Configuration.configuration.setTimeoutCon(100);
    WaarpLoggerFactory.setLogLevel(WaarpLogLevel.ERROR);
    for (int pid : PIDS) {
      Processes.kill(pid, true);
    }
    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    tearDownAfterClassServer();
  }

  @Before
  public void setUp() throws Exception {
    Configuration.configuration.setTimeoutCon(30000);
    Runtime runtime = Runtime.getRuntime();
    usedMemory = runtime.totalMemory() - runtime.freeMemory();
  }

  @After
  public void tearDown() throws Exception {
    Thread.sleep(100);
  }

  private void checkMemory() {
    Runtime runtime = Runtime.getRuntime();
    long newUsedMemory = runtime.totalMemory() - runtime.freeMemory();
    if (newUsedMemory > MAX_USED_MEMORY) {
      logger.info("Used Memory > 2GB {} {}", usedMemory / 1048576.0,
                  newUsedMemory / 1048576.0);
    }
  }

  private void testBigTransfer(boolean limit, String serverName, boolean direct,
                               boolean recv, int size) throws IOException {
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
      Configuration.configuration.changeNetworkLimit(0, 0, 0, 0, 1000);
    }
    File baseDir;
    if (recv && !"server1".equals(serverName)) {
      baseDir = new File("/tmp/R66/scenario_big_file_limitbdw/R2/out/");
    } else {
      baseDir = new File("/tmp/R66/scenario_big_file_limitbdw/R1/out/");
    }
    File fileOut = new File(baseDir, "hello" + size);
    final File outHello = generateOutFile(fileOut.getAbsolutePath(), size);
    long timestart = System.currentTimeMillis();
    final R66Future future = new R66Future(true);
    if (direct) {
      if (recv) {
        final TestRecvThroughHandler handler = new TestRecvThroughHandler();
        final TestRecvThroughClient transaction =
            new TestRecvThroughClient(future, handler, serverName,
                                      "hello" + size, "recvthrough",
                                      "Test Big RecvThrough", true, BLOCK_SIZE,
                                      networkTransaction);
        transaction.setNormalInfoAsWarn(false);
        transaction.run();
      } else {
        final TestTransferNoDb transaction =
            new TestTransferNoDb(future, serverName, "hello" + size, "loop",
                                 "Test Loop Send", true, BLOCK_SIZE,
                                 DbConstantR66.ILLEGALVALUE,
                                 networkTransaction);
        transaction.setNormalInfoAsWarn(false);
        transaction.run();
      }
      future.awaitOrInterruptible();
      assertTrue(future.isSuccess());
      logger.info("Runner: {}", future.getRunner());
    } else {
      String ruleName = recv? "recv" : "sendany";
      final SubmitTransfer transaction =
          new SubmitTransfer(future, serverName, "hello" + size, ruleName,
                             "Test Big " + ruleName, true, BLOCK_SIZE,
                             DbConstantR66.ILLEGALVALUE, null);
      TransferArgs.forceAnalyzeFollow(transaction);
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
                size / 1000.0 / (timestop - timestart),
                Configuration.configuration.getServerGlobalReadLimit() /
                1000000.0,
                Configuration.configuration.getServerChannelReadLimit() /
                1000000.0, size, BLOCK_SIZE);
    checkMemory();
  }

  @Test
  public void test01_LoopBigSendsSyncNoLimit()
      throws IOException, InterruptedException {
    if (SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      NUMBER_FILES = 4;
    } else {
      NUMBER_FILES = 2;
    }
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(false, "server2", true, false, 1024 * 500 * 1024);
    testBigTransfer(false, "server2", true, false, 1024 * 1024 * 1024);
    if (SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      testBigTransfer(false, "server2", true, false, 1024 * 502 * 1024);
      testBigTransfer(false, "server2", true, false, 1024 * 1026 * 1024);
    }
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test02_LoopBigSendsSyncSslNoLimit()
      throws IOException, InterruptedException {
    if (SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      NUMBER_FILES = 2;
    } else {
      NUMBER_FILES = 1;
    }
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    testBigTransfer(false, "server2-ssl", true, false, 1024 * 501 * 1024);
    if (SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      testBigTransfer(false, "server2-ssl", true, false, 1024 * 1025 * 1024);
    }
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test03_LoopBigSendsSyncSslNoLimit()
          throws IOException, InterruptedException {
    if (SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      NUMBER_FILES = 700;
    } else {
      NUMBER_FILES = -1;
      logger.warn("Test disabled without IT_LONG_TEST");
      Assume.assumeTrue("If the Long term tests are allowed",
                        SystemPropertyUtil.get(IT_LONG_TEST, false));
      return;
    }
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    int limit = 700;
    int middleSize = 360 - (limit / 2);

    Assume.assumeNotNull(networkTransaction);
      Configuration.configuration.changeNetworkLimit(0, 0, 0, 0, 1000);
    File baseDir = new File("/tmp/R66/scenario_big_file_limitbdw/R1/out/");
    for (int i = 0; i < limit; i++) {
      int size = (middleSize + i) * 1024;
      File fileOut = new File(baseDir, "hello" + size);
      final File outHello = generateOutFile(fileOut.getAbsolutePath(), size);
    }
    logger.warn("End of file creations");
    long timestart = System.currentTimeMillis();
    R66Future[] futures = new R66Future[limit];
    for (int i = 0; i < limit; i++) {
      int size = (middleSize + i) * 1024;
      final R66Future future = new R66Future(true);
      futures[i] = future;
      final TestTransferNoDb transaction =
                new TestTransferNoDb(future, "server2-ssl", "hello" + size, "loop",
                        "Test Loop Send", true, BLOCK_SIZE,
                        DbConstantR66.ILLEGALVALUE,
                        networkTransaction);
      transaction.setNormalInfoAsWarn(false);
      transaction.run();
    }
    for (int i = 0; i < limit; i++) {
      futures[i].awaitOrInterruptible();
      assertTrue(futures[i].isSuccess());
    }
    //logger.info("Runner: {}", future.getRunner());
    long timestop = System.currentTimeMillis();
    logger.warn("Direct {}, Recv {}, LimitBandwidth {} " +
                    "({} seconds,  {} MBPS vs {} " +
                    "and {}) of size {} with block size {}", true, false, limit,
            (timestop - timestart) / 1000,
            limit * 180*1024 / 1000.0 / (timestop - timestart),
            Configuration.configuration.getServerGlobalReadLimit() /
                    1000000.0,
            Configuration.configuration.getServerChannelReadLimit() /
                    1000000.0, limit *180*1024, BLOCK_SIZE);
    checkMemory();
    logger.warn("End {}", Processes.getCurrentMethodName());
  }


  @Test
  public void test04_LoopBigSendsSyncNoLimit()
          throws IOException, InterruptedException {
    if (SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      NUMBER_FILES = 700;
    } else {
      NUMBER_FILES = -1;
      logger.warn("Test disabled without IT_LONG_TEST");
      Assume.assumeTrue("If the Long term tests are allowed",
                        SystemPropertyUtil.get(IT_LONG_TEST, false));
      return;
    }
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    int limit = 700;
    int middleSize = 360 - (limit / 2);

    Assume.assumeNotNull(networkTransaction);
    Configuration.configuration.changeNetworkLimit(0, 0, 0, 0, 1000);
    File baseDir = new File("/tmp/R66/scenario_big_file_limitbdw/R1/out/");
    for (int i = 0; i < limit; i++) {
      int size = (middleSize + i) * 1024;
      File fileOut = new File(baseDir, "hello" + size);
      final File outHello = generateOutFile(fileOut.getAbsolutePath(), size);
    }
    logger.warn("End of file creations");
    long timestart = System.currentTimeMillis();
    R66Future[] futures = new R66Future[limit];
    for (int i = 0; i < limit; i++) {
      int size = (middleSize + i) * 1024;
      final R66Future future = new R66Future(true);
      futures[i] = future;
      final TestTransferNoDb transaction =
              new TestTransferNoDb(future, "server2", "hello" + size, "loop",
                      "Test Loop Send", true, BLOCK_SIZE,
                      DbConstantR66.ILLEGALVALUE,
                      networkTransaction);
      transaction.setNormalInfoAsWarn(false);
      transaction.run();
    }
    for (int i = 0; i < limit; i++) {
      futures[i].awaitOrInterruptible();
      assertTrue(futures[i].isSuccess());
    }
    //logger.info("Runner: {}", future.getRunner());
    long timestop = System.currentTimeMillis();
    logger.warn("Direct {}, Recv {}, LimitBandwidth {} " +
                    "({} seconds,  {} MBPS vs {} " +
                    "and {}) of size {} with block size {}", true, false, limit,
            (timestop - timestart) / 1000,
            limit * 180*1024 / 1000.0 / (timestop - timestart),
            Configuration.configuration.getServerGlobalReadLimit() /
                    1000000.0,
            Configuration.configuration.getServerChannelReadLimit() /
                    1000000.0, limit *180*1024, BLOCK_SIZE);
    checkMemory();
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
