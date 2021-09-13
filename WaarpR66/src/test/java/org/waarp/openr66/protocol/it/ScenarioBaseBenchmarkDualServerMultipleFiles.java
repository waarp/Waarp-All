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
import org.waarp.openr66.client.NoOpRecvThroughHandler;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.junit.TestAbstract;
import org.waarp.openr66.protocol.test.TestRecvThroughClient;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.server.R66Server;
import org.waarp.openr66.server.ServerInitDatabase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.*;
import static org.waarp.openr66.protocol.it.ScenarioBase.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class ScenarioBaseBenchmarkDualServerMultipleFiles
    extends TestAbstract {

  protected static ScenarioBaseBenchmarkDualServerMultipleFiles scenarioBase;
  private static final String SERVER_1_XML = "R1/conf/server_1_SQLDB.xml";
  private static final String SERVER_1_REWRITTEN_XML = "R1/conf/server1.xml";
  private static final String RESOURCES_SERVER_1_XML =
      "it/scenario_multiple_servers/" + SERVER_1_XML;

  private static final String SERVER_2_XML = "R1/conf/server_2_SQLDB.xml";
  private static final String SERVER_2_REWRITTEN_XML = "R1/conf/server2.xml";
  private static final String RESOURCES_SERVER_2_XML =
      "it/scenario_multiple_servers/" + SERVER_2_XML;

  private static final String SERVER_3_XML = "R1/conf/server_3_SQLDB.xml";
  private static final String SERVER_3_REWRITTEN_XML = "R1/conf/server3.xml";
  private static final String RESOURCES_SERVER_3_XML =
      "it/scenario_multiple_servers/" + SERVER_3_XML;

  private static final String SERVER_4_XML = "R1/conf/server_4_SQLDB.xml";
  private static final String SERVER_4_REWRITTEN_XML = "R1/conf/server4.xml";
  private static final String RESOURCES_SERVER_4_XML =
      "it/scenario_multiple_servers/" + SERVER_4_XML;

  private static final String CLIENT_3_XML = "client_3.xml";

  private static final List<Integer> PIDS = new ArrayList<Integer>();
  private static final String TMP_R66_SCENARIO_R1 =
      "/tmp/R66/scenario_multiple_servers/R1";
  private static final String TMP_R66_CONFIG_R1 =
      "/tmp/R66/scenario_multiple_servers/" + SERVER_1_REWRITTEN_XML;
  private static final String TMP_R66_CONFIG_R2 =
      "/tmp/R66/scenario_multiple_servers/" + SERVER_2_REWRITTEN_XML;
  private static final String TMP_R66_CONFIG_R3 =
      "/tmp/R66/scenario_multiple_servers/" + SERVER_3_REWRITTEN_XML;
  private static final String TMP_R66_CONFIG_R4 =
      "/tmp/R66/scenario_multiple_servers/" + SERVER_4_REWRITTEN_XML;
  public static int NUMBER_FILES = 1;
  private static final int NB_SERVER = 2;
  public static final int IT_NUMBER_FILES = NB_SERVER >= 3? 6000 : 4000;
  public static int BLOCK_SIZE = 1048576;
  public static long MAX_USED_MEMORY = 536870912;

  private static int r66Pid1 = 999999;
  private static int r66Pid2 = 999999;
  private static int r66Pid3 = 999999;
  private static int r66Pid4 = 999999;
  public static boolean SERVER1_IN_JUNIT = false;
  private static final String TMP_CONFIG_XML = "/tmp/config.xml";
  private static File configFile1 = null;
  private static File configFile2 = null;
  private static File configFile3 = null;
  private static File configFile4 = null;
  private static final NoOpRecvThroughHandler handler =
      new NoOpRecvThroughHandler();
  private long usedMemory;

  public static void setUpBeforeClass() throws Exception {
    if (!SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      ResourceLeakDetector.setLevel(Level.PARANOID);
    } else {
      ResourceLeakDetector.setLevel(Level.SIMPLE);
    }
    final ClassLoader classLoader =
        ScenarioBaseBenchmarkDualServerMultipleFiles.class.getClassLoader();
    File file =
        new File(classLoader.getResource(RESOURCES_SERVER_1_XML).getFile());
    dirResources = file.getParentFile().getParentFile().getParentFile();
    logger.warn(dirResources.getAbsolutePath());
    setUpBeforeClassMinimal(RESOURCES_SERVER_1_XML);
    File r1 = new File(dirResources, "R1/conf");
    createBaseR66Directory(r1, TMP_R66_SCENARIO_R1);
    setUp3DbBeforeClass();
    Configuration.configuration.setTimeoutCon(100);
    String xmx = Processes.contentXmx();
    if (xmx == null || "-Xmx1024m".equalsIgnoreCase(xmx)) {
      Processes.setJvmArgsDefault("-Xms1024m -Xmx1024m ");
    } else if ("-Xmx2048m".equalsIgnoreCase(xmx)) {
      Processes.setJvmArgsDefault("-Xms2048m -Xmx2048m ");
    } else {
      Processes.setMemoryAccordingToFreeMemory(
          SERVER1_IN_JUNIT? NB_SERVER - 1 : NB_SERVER);
    }
    if (!SERVER1_IN_JUNIT) {
      r66Pid1 = startServer(configFile1.getAbsolutePath());
    }
    r66Pid2 = startServer(configFile2.getAbsolutePath());
    if (NB_SERVER >= 3) {
      r66Pid3 = startServer(configFile3.getAbsolutePath());
    }
    if (NB_SERVER >= 4) {
      r66Pid4 = startServer(configFile4.getAbsolutePath());
    }
    if (SERVER1_IN_JUNIT) {
      R66Server.main(new String[] { configFile1.getAbsolutePath() });
      setUpBeforeClassClient();
    } else {
      setUpBeforeClassClient(
          configFile1.getParentFile().getAbsolutePath() + CLIENT_3_XML);
      setUpBeforeClassClient(configFile1.getAbsolutePath());
    }
    Thread.sleep(1000);
    Processes.setJvmArgsDefault(null);
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
  }

  private File generateServerConfigFile(final String resources,
                                        final String tmpConfig) {
    logger.warn("Build configFile");
    ClassLoader classLoader =
        ScenarioBaseBenchmarkDualServerMultipleFiles.class.getClassLoader();
    File file = new File(classLoader.getResource(resources).getFile());
    if (!file.exists()) {
      SysErrLogger.FAKE_LOGGER.syserr(
          "Cannot find in  " + file.getAbsolutePath());
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
      SysErrLogger.FAKE_LOGGER.syserr(
          jdbcUrl + " while should be something like " + jdbcUrl);
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
    File fileTo = new File(tmpConfig);
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
    return fileTo;
  }

  // Server 1
  public String getServerConfig1File() {
    if (configFile1 != null) {
      logger.warn("ConfigFile already set to {]", configFile1);
      return configFile1.getAbsolutePath();
    }
    File fileTo =
        generateServerConfigFile(RESOURCES_SERVER_1_XML, TMP_R66_CONFIG_R1);
    configFile1 = fileTo;
    logger.warn("Config file created at {}", fileTo);
    File copy = new File(TMP_CONFIG_XML);
    try {
      FileUtils.copy(configFile1, copy, false, false);
      logger.warn("Copy from {} to {}", configFile1, copy);
    } catch (Reply550Exception e) {
      e.printStackTrace();
    }
    logger.warn("Copy from {} to {} and return {}", configFile1, copy,
                TMP_R66_CONFIG_R1);
    return TMP_R66_CONFIG_R1;
  }

  // Server 2
  public String getServerConfig2File() {
    if (configFile2 != null) {
      logger.warn("ConfigFile already set to {]", configFile2);
      return configFile2.getAbsolutePath();
    }
    File fileTo =
        generateServerConfigFile(RESOURCES_SERVER_2_XML, TMP_R66_CONFIG_R2);
    configFile2 = fileTo;
    logger.warn("Config file created at {}", fileTo);
    File copy = new File(TMP_CONFIG_XML);
    try {
      FileUtils.copy(configFile2, copy, false, false);
      logger.warn("Copy from {} to {}", configFile2, copy);
    } catch (Reply550Exception e) {
      e.printStackTrace();
    }
    logger.warn("Copy from {} to {} and return {}", configFile2, copy,
                TMP_R66_CONFIG_R2);
    return TMP_R66_CONFIG_R2;
  }

  // Server 3
  public String getServerConfig3File() {
    if (configFile3 != null) {
      logger.warn("ConfigFile already set to {]", configFile3);
      return configFile3.getAbsolutePath();
    }
    File fileTo =
        generateServerConfigFile(RESOURCES_SERVER_3_XML, TMP_R66_CONFIG_R3);
    configFile3 = fileTo;
    logger.warn("Config file created at {}", fileTo);
    File copy = new File(TMP_CONFIG_XML);
    try {
      FileUtils.copy(configFile3, copy, false, false);
      logger.warn("Copy from {} to {}", configFile3, copy);
    } catch (Reply550Exception e) {
      e.printStackTrace();
    }
    logger.warn("Copy from {} to {} and return {}", configFile3, copy,
                TMP_R66_CONFIG_R3);
    return TMP_R66_CONFIG_R3;
  }

  // Server 4
  public String getServerConfig4File() {
    if (configFile4 != null) {
      logger.warn("ConfigFile already set to {]", configFile4);
      return configFile4.getAbsolutePath();
    }
    File fileTo =
        generateServerConfigFile(RESOURCES_SERVER_4_XML, TMP_R66_CONFIG_R4);
    configFile4 = fileTo;
    logger.warn("Config file created at {}", fileTo);
    File copy = new File(TMP_CONFIG_XML);
    try {
      FileUtils.copy(configFile4, copy, false, false);
      logger.warn("Copy from {} to {}", configFile4, copy);
    } catch (Reply550Exception e) {
      e.printStackTrace();
    }
    logger.warn("Copy from {} to {} and return {}", configFile4, copy,
                TMP_R66_CONFIG_R4);
    return TMP_R66_CONFIG_R4;
  }

  public abstract JdbcDatabaseContainer getJDC();

  private static File setup1DbBeforeClass(final String serverConfig,
                                          final File srcConfigFile) {
    File configFile = srcConfigFile;
    String fileconf = null;
    try {
      fileconf = serverConfig;
      logger.warn("Config file found {}", fileconf);
      if (fileconf.charAt(0) != '/') {
        configFile = new File(dirResources, fileconf);
      }
    } catch (UnsupportedOperationException e) {
      SysErrLogger.FAKE_LOGGER.syserr(
          "Database not supported by this test Start Stop R66", e);
      Assume.assumeNoException(e);
      return null;
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
    return configFile;
  }

  public static void setUp3DbBeforeClass() throws Exception {
    deleteBase();
    final ClassLoader classLoader =
        ScenarioBaseBenchmarkDualServerMultipleFiles.class.getClassLoader();
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
    configFile1 =
        setup1DbBeforeClass(scenarioBase.getServerConfig1File(), configFile1);
    initiateDb(configFile1.getAbsolutePath());
    configFile2 =
        setup1DbBeforeClass(scenarioBase.getServerConfig2File(), configFile2);
    if (NB_SERVER >= 3) {
      configFile3 =
          setup1DbBeforeClass(scenarioBase.getServerConfig3File(), configFile3);
    }
    if (NB_SERVER >= 4) {
      configFile4 =
          setup1DbBeforeClass(scenarioBase.getServerConfig4File(), configFile4);
    }
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
    Processes.executeJvm(project, ServerInitDatabase.class, args, false);
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
      Processes.executeJvm(project, R66Server.class, argsServer, true);
      int pid = Processes.getPidOfRunnerCommandLinux("java",
                                                     R66Server.class.getName(),
                                                     PIDS);
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
    Assume.assumeNotNull(networkTransaction);
    tearDownAfterClassServer();
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

  private int initFiles() throws IOException {
    int ratio = 250 * 1024 * 2 / NUMBER_FILES;
    Assume.assumeNotNull(networkTransaction);
    Configuration.configuration.changeNetworkLimit(0, 0, 0, 0, 1000);
    File baseDir = new File("/tmp/R66/scenario_multiple_servers/R1/out/");
    for (int i = 0; i < NUMBER_FILES; i++) {
      int size = ratio * i;
      File fileOut = new File(baseDir, "hello" + size);
      generateOutFile(fileOut.getAbsolutePath(), size);
    }
    return ratio;
  }

  private static class RecvThread implements Callable<Boolean> {
    private final int size;
    private final int rank;
    private final boolean tls;
    private final R66Future[] futures;

    RecvThread(int size, int rank, R66Future[] futures, boolean tls) {
      this.size = size;
      this.rank = rank;
      this.futures = futures;
      this.tls = tls;
    }

    @Override
    public Boolean call() throws Exception {
      final R66Future future = new R66Future(true);
      futures[rank] = future;
      final TestRecvThroughClient transaction;
      switch (rank % NB_SERVER) {
        case 0:
          transaction = new TestRecvThroughClient(future, handler,
                                                  tls? "server2-ssl" :
                                                      "server2", "hello" + size,
                                                  "sendany",
                                                  "Test Send " + size +
                                                  " #COMPRESS#", true,
                                                  BLOCK_SIZE,
                                                  networkTransaction);
          break;
        case 1:
          transaction = new TestRecvThroughClient(future, handler,
                                                  tls? "server1-ssl" :
                                                      "server1", "hello" + size,
                                                  "sendany",
                                                  "Test Send " + size +
                                                  " #COMPRESS#", true,
                                                  BLOCK_SIZE,
                                                  networkTransaction);
          break;
        case 2:
          transaction = new TestRecvThroughClient(future, handler,
                                                  tls? "server3-ssl" :
                                                      "server3", "hello" + size,
                                                  "sendany",
                                                  "Test Send " + size +
                                                  " #COMPRESS#", true,
                                                  BLOCK_SIZE,
                                                  networkTransaction);
          break;
        case 3:
        default:
          transaction = new TestRecvThroughClient(future, handler,
                                                  tls? "server4-ssl" :
                                                      "server4", "hello" + size,
                                                  "sendany",
                                                  "Test Send " + size +
                                                  " #COMPRESS#", true,
                                                  BLOCK_SIZE,
                                                  networkTransaction);
          break;
      }
      transaction.setNormalInfoAsWarn(false);
      transaction.run();
      future.awaitOrInterruptible();
      return future.isSuccess();
    }
  }

  private long directRecvThread(R66Future[] futures, boolean tls, int ratio,
                                int factor) throws Exception {
    ExecutorService executorService =
        Executors.newFixedThreadPool(NUMBER_FILES);
    CompletionService<Boolean> completionService =
        new ExecutorCompletionService<Boolean>(executorService);
    for (int i = 0; i < NUMBER_FILES; i++) {
      int size = ratio * i;
      completionService.submit(new RecvThread(size, i * factor, futures, tls));
    }
    long timestart = System.currentTimeMillis();
    int end = 0;
    while (end < NUMBER_FILES) {
      Future<Boolean> future = completionService.take();
      try {
        if (future.get()) {
          end++;
        } else {
          fail("Recv issue");
          break;
        }
      } catch (final Exception e) {
        fail("Issue " + e.getMessage());
        break;
      }
    }
    long timestop = System.currentTimeMillis();
    assertEquals(NUMBER_FILES, end);
    executorService.shutdown();
    return timestop - timestart;
  }

  private void checkFinal(R66Future[] futures, int factor)
      throws InterruptedException {
    for (int i = 0; i < NUMBER_FILES; i++) {
      R66Future future = futures[i * factor];
      future.awaitOrInterruptible();
      assertTrue(future.isSuccess());
    }
  }

  @Test
  public void test01_RecvTlsSyncNoLimit() throws Exception {
    if (SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      NUMBER_FILES = IT_NUMBER_FILES;
    } else {
      NUMBER_FILES = -1;
      logger.warn("Test disabled without IT_LONG_TEST");
      Assume.assumeTrue("If the Long term tests are allowed",
                        SystemPropertyUtil.get(IT_LONG_TEST, false));
      return;
    }
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);

    int ratio = initFiles();
    logger.warn("End of file creations");

    R66Future[] futures = new R66Future[NUMBER_FILES];

    NUMBER_FILES = IT_NUMBER_FILES / 2;
    logger.warn("WarmUp {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    directRecvThread(futures, true, ratio, 1);
    NUMBER_FILES = IT_NUMBER_FILES;
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    long time = directRecvThread(futures, true, ratio, 1);
    logger.warn("Direct {}, Recv {}, LimitBandwidth {} " +
                "({} seconds,  {} nb/s, {} MBPS vs {} " +
                "and {}) of size {} with block size {}", true, false,
                NUMBER_FILES, (time) / 1000,
                NUMBER_FILES * 1000.0 / (1.0 * time),
                NUMBER_FILES * 250 * 1024 / 1000.0 / (time),
                Configuration.configuration.getServerGlobalReadLimit() /
                1000000.0,
                Configuration.configuration.getServerChannelReadLimit() /
                1000000.0, NUMBER_FILES * 250 * 1024, BLOCK_SIZE);
    checkMemory();
    logger.warn("End {}", Processes.getCurrentMethodName());
  }


  @Test
  public void test02_RecvSyncNoLimit() throws Exception {
    if (SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      NUMBER_FILES = IT_NUMBER_FILES;
    } else {
      NUMBER_FILES = -1;
      logger.warn("Test disabled without IT_LONG_TEST");
      Assume.assumeTrue("If the Long term tests are allowed",
                        SystemPropertyUtil.get(IT_LONG_TEST, false));
      return;
    }
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);

    int ratio = initFiles();
    logger.warn("End of file creations");

    R66Future[] futures = new R66Future[NUMBER_FILES];
    NUMBER_FILES = IT_NUMBER_FILES / 2;
    logger.warn("WarmUp {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    directRecvThread(futures, false, ratio, 1);
    NUMBER_FILES = IT_NUMBER_FILES;
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    long time = directRecvThread(futures, false, ratio, 1);
    logger.warn("Direct {}, Recv {}, LimitBandwidth {} " +
                "({} seconds,  {} nb/s, {} MBPS vs {} " +
                "and {}) of size {} with block size {}", true, false,
                NUMBER_FILES, (time) / 1000,
                NUMBER_FILES * 1000.0 / (1.0 * time),
                NUMBER_FILES * 260 * 1024 / 1000.0 / (time),
                Configuration.configuration.getServerGlobalReadLimit() /
                1000000.0,
                Configuration.configuration.getServerChannelReadLimit() /
                1000000.0, NUMBER_FILES * 260 * 1024, BLOCK_SIZE);
    checkMemory();
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test03_RecvSyncNoLimitMono() throws Exception {
    if (SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      NUMBER_FILES = IT_NUMBER_FILES;
    } else {
      NUMBER_FILES = -1;
      logger.warn("Test disabled without IT_LONG_TEST");
      Assume.assumeTrue("If the Long term tests are allowed",
                        SystemPropertyUtil.get(IT_LONG_TEST, false));
      return;
    }
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);

    int ratio = initFiles();
    logger.warn("End of file creations");

    R66Future[] futures = new R66Future[NUMBER_FILES * NB_SERVER];
    NUMBER_FILES = IT_NUMBER_FILES / 2;
    logger.warn("WarmUp {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    directRecvThread(futures, false, ratio, NB_SERVER);
    NUMBER_FILES = IT_NUMBER_FILES;
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    long time = directRecvThread(futures, false, ratio, NB_SERVER);
    logger.warn("Direct {}, Recv {}, LimitBandwidth {} " +
                "({} seconds,  {} nb/s, {} MBPS vs {} " +
                "and {}) of size {} with block size {}", true, false,
                NUMBER_FILES, (time) / 1000,
                NUMBER_FILES * 1000.0 / (1.0 * time),
                NUMBER_FILES * 260 * 1024 / 1000.0 / (time),
                Configuration.configuration.getServerGlobalReadLimit() /
                1000000.0,
                Configuration.configuration.getServerChannelReadLimit() /
                1000000.0, NUMBER_FILES * 260 * 1024, BLOCK_SIZE);
    checkMemory();
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test03_RecvSyncTlsNoLimitMono() throws Exception {
    if (SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      NUMBER_FILES = IT_NUMBER_FILES;
    } else {
      NUMBER_FILES = -1;
      logger.warn("Test disabled without IT_LONG_TEST");
      Assume.assumeTrue("If the Long term tests are allowed",
                        SystemPropertyUtil.get(IT_LONG_TEST, false));
      return;
    }
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);

    int ratio = initFiles();
    logger.warn("End of file creations");

    R66Future[] futures = new R66Future[NUMBER_FILES * NB_SERVER];
    NUMBER_FILES = IT_NUMBER_FILES / 2;
    logger.warn("WarmUp {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    directRecvThread(futures, true, ratio, NB_SERVER);
    NUMBER_FILES = IT_NUMBER_FILES;
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    long time = directRecvThread(futures, true, ratio, NB_SERVER);
    logger.warn("Direct {}, Recv {}, LimitBandwidth {} " +
                "({} seconds,  {} nb/s, {} MBPS vs {} " +
                "and {}) of size {} with block size {}", true, false,
                NUMBER_FILES, (time) / 1000,
                NUMBER_FILES * 1000.0 / (1.0 * time),
                NUMBER_FILES * 260 * 1024 / 1000.0 / (time),
                Configuration.configuration.getServerGlobalReadLimit() /
                1000000.0,
                Configuration.configuration.getServerChannelReadLimit() /
                1000000.0, NUMBER_FILES * 260 * 1024, BLOCK_SIZE);
    checkMemory();
    logger.warn("End {}", Processes.getCurrentMethodName());
  }
}
