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

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.common.utility.Processes;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.client.MultipleSubmitTransfer;
import org.waarp.openr66.client.SubmitTransfer;
import org.waarp.openr66.commander.InternalRunner;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.junit.TestAbstract;
import org.waarp.openr66.protocol.test.TestRecvThroughClient;
import org.waarp.openr66.protocol.test.TestRecvThroughClient.TestRecvThroughHandler;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.server.R66Server;
import org.waarp.openr66.server.ServerInitDatabase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class ScenarioBase extends TestAbstract {
  /**
   * If defined using -DIT_LONG_TEST=true then will execute long term tests
   */
  public static final String IT_LONG_TEST = "IT_LONG_TEST";

  protected static ScenarioBase scenarioBase;
  private static final ArrayList<DbTaskRunner> dbTaskRunners =
      new ArrayList<DbTaskRunner>();
  private static final String SERVER_1_XML = "R1/conf/server_1_SQLDB.xml";
  private static final String SERVER_1_REWRITTEN_XML = "R1/conf/server.xml";
  private static final String RESOURCES_SERVER_1_XML =
      "it/scenario_1_2_3/" + SERVER_1_XML;
  static final String SERVER_H2_1_XML = "R1/conf/server_1.xml";
  private static final String SERVER_2_XML = "R2/conf/server_2.xml";
  private static final String SERVER_3_XML = "R3/conf/server_3.xml";

  private static final List<Integer> PIDS = new ArrayList<Integer>();
  private static final String TMP_R66_SCENARIO_R1 =
      "/tmp/R66/scenario_1_2_3/R1";
  private static final String TMP_R66_CONFIG_R1 =
      "/tmp/R66/scenario_1_2_3/" + SERVER_1_REWRITTEN_XML;
  public static int NUMBER_FILES = 50;
  public static int LARGE_SIZE = 2000000;
  public static int BLOCK_SIZE = 8192;

  private static int r66Pid1 = 999999;
  private static int r66Pid2 = 999999;
  private static int r66Pid3 = 999999;
  private static final boolean SERVER1_IN_JUNIT = true;
  private static final String TMP_CONFIG_XML = "/tmp/config.xml";
  private static File configFile = null;

  public static void setUpBeforeClass() throws Exception {
    final ClassLoader classLoader = ScenarioBase.class.getClassLoader();
    File file =
        new File(classLoader.getResource(RESOURCES_SERVER_1_XML).getFile());
    dirResources = file.getParentFile().getParentFile().getParentFile();
    logger.warn(dirResources.getAbsolutePath());
    setUpBeforeClassMinimal(RESOURCES_SERVER_1_XML);
    File r1 = new File(dirResources, "R1/conf");
    createBaseR66Directory(r1, TMP_R66_SCENARIO_R1);
    File r2 = new File(dirResources, "R2/conf");
    createBaseR66Directory(r2, "/tmp/R66/scenario_1_2_3/R2");
    File r3 = new File(dirResources, "R3/conf");
    createBaseR66Directory(r3, "/tmp/R66/scenario_1_2_3/R3");
    setUp3DbBeforeClass();
    Configuration.configuration.setTimeoutCon(100);
    Processes.setJvmArgsDefault("-Xms2048m -Xmx2048m ");
    if (!SERVER1_IN_JUNIT) {
      r66Pid1 = startServer(configFile.getAbsolutePath());
    }
    r66Pid2 = startServer(SERVER_2_XML);
    r66Pid3 = startServer(SERVER_3_XML);
    if (SERVER1_IN_JUNIT) {
      R66Server.main(new String[] { configFile.getAbsolutePath() });
      setUpBeforeClassClient();
    } else {
      setUpBeforeClassClient(configFile.getAbsolutePath());
    }
    Processes.setJvmArgsDefault(null);
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
  }

  public String getServerConfigFile() {
    if (configFile != null) {
      return configFile.getAbsolutePath();
    }
    ClassLoader classLoader = ScenarioBase.class.getClassLoader();
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
    } else if (driver.equalsIgnoreCase("com.mysql.jdbc.Driver")) {
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
    File copy = new File(TMP_CONFIG_XML);
    try {
      FileUtils.copy(configFile, copy, false, false);
    } catch (Reply550Exception e) {
      e.printStackTrace();
    }
    return TMP_R66_CONFIG_R1;
  }

  public abstract JdbcDatabaseContainer getJDC();

  public static void setUp3DbBeforeClass() throws Exception {
    deleteBase();
    final ClassLoader classLoader = ScenarioBase.class.getClassLoader();
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
    }
    initiateDb(configFile.getAbsolutePath());
    initiateDb(SERVER_2_XML);
    initiateDb(SERVER_3_XML);

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
    for (int pid : PIDS) {
      Processes.kill(pid, true);
    }
    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    tearDownAfterClassServer();
  }

  private void checkBigIt() {
    Assume.assumeTrue("If the Long term tests are allowed",
                      SystemPropertyUtil.get(IT_LONG_TEST, false));
    Runtime runtime = Runtime.getRuntime();
    boolean isMemory1GB = runtime.totalMemory() <= 1024 * 1024 * 1024;
    if (isMemory1GB) {
      logger.warn("If the Long term tests are allowed, memory must be " +
                  "greater than 1G: {}", runtime.totalMemory());
    }
  }

  @Test
  public void test010_MultipleSends() throws IOException, InterruptedException {
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    Assume.assumeNotNull(networkTransaction);
    File baseDir = new File("/tmp/R66/scenario_1_2_3/R1/out/");
    for (int i = 0; i < NUMBER_FILES; i++) {
      File fileOut = new File(baseDir, "hello" + i);
      final File outHello = generateOutFile(fileOut.getAbsolutePath(), 100);
    }
    final R66Future future = new R66Future(true);
    final MultipleSubmitTransfer transaction =
        new MultipleSubmitTransfer(future, "server2", "hello*", "first",
                                   "test multiple with jump", true, 1024,
                                   DbConstantR66.ILLEGALVALUE, null,
                                   networkTransaction);
    transaction.setNormalInfoAsWarn(false);
    transaction.run();
    future.awaitOrInterruptible();
    assertTrue(future.isSuccess());
    long timestart = System.currentTimeMillis();
    File dirR2 = new File("/tmp/R66/scenario_1_2_3/R2/in");
    File dirR3 = new File("/tmp/R66/scenario_1_2_3/R3/in");
    int max = 0;
    long timestop;
    if (SERVER1_IN_JUNIT) {
      InternalRunner internalRunner =
          Configuration.configuration.getInternalRunner();
      for (int i = 0; i < NUMBER_FILES * 10; i++) {
        Thread.sleep(100);
        if (internalRunner != null) {
          max = Math.max(max, internalRunner.nbInternalRunner());
        }
        int count = dirR3.list().length;
        if (count == NUMBER_FILES) {
          break;
        }
      }
      timestop = System.currentTimeMillis();
      Thread.sleep(1000);
      logger.warn(
          "Sent {} files to R2, then {} to R3, using at most {} parallel clients" +
          " ({} seconds,  {} per seconds)", dirR2.list().length,
          dirR3.list().length, max, (timestop - timestart) / 1000,
          NUMBER_FILES * 1000 / (timestop - timestart));
    } else {
      for (int i = 0; i < NUMBER_FILES * 10; i++) {
        Thread.sleep(200);
        int count = dirR3.list().length;
        if (count == NUMBER_FILES) {
          break;
        }
      }
      timestop = System.currentTimeMillis();
      Thread.sleep(1000);
      logger.warn(
          "Sent {} files to R2, then {} to R3 ({} seconds, {} per seconds)",
          dirR2.list().length, dirR3.list().length,
          (timestop - timestart) / 1000,
          NUMBER_FILES * 1000 / (timestop - timestart));
    }
    FileUtils.forceDeleteRecursiveDir(dirR2);
    FileUtils.forceDeleteRecursiveDir(dirR3);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test011_SendToItself() throws IOException, InterruptedException {
    logger.warn("Start {}", Processes.getCurrentMethodName());
    Assume.assumeNotNull(networkTransaction);
    File baseDir = new File("/tmp/R66/scenario_1_2_3/R1/out/");
    final File totest =
        generateOutFile(baseDir.getAbsolutePath() + "/testTask.txt", 100);
    final R66Future future = new R66Future(true);
    final SubmitTransfer transaction =
        new SubmitTransfer(future, "server1-ssl", "testTask.txt", "rule3",
                           "Test Send Submit", true, 8192,
                           DbConstantR66.ILLEGALVALUE, null);
    transaction.run();
    future.awaitOrInterruptible();
    assertTrue(future.isSuccess());
    final DbTaskRunner runner = future.getResult().getRunner();
    if (runner != null) {
      logger.warn("Runner: {}", runner.toString());
      waitForAllDone(runner);
      dbTaskRunners.add(runner);
    }
    totest.delete();
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test012_MultipleRecvsSync()
      throws IOException, InterruptedException {
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    Assume.assumeNotNull(networkTransaction);
    File baseDir = new File("/tmp/R66/scenario_1_2_3/R2/out/");
    File fileOut = new File(baseDir, "hello");
    final File outHello =
        generateOutFile(fileOut.getAbsolutePath(), LARGE_SIZE);
    ArrayList<R66Future> futures = new ArrayList<R66Future>(NUMBER_FILES);
    ExecutorService executorService =
        Executors.newFixedThreadPool(NUMBER_FILES);
    final TestRecvThroughHandler handler = new TestRecvThroughHandler();
    long timestart = System.currentTimeMillis();
    for (int i = 0; i < NUMBER_FILES; i++) {
      final R66Future future = new R66Future(true);
      futures.add(future);
      final TestRecvThroughClient transaction =
          new TestRecvThroughClient(future, handler, "server2", "hello",
                                    "recvthrough", "Test Multiple RecvThrough",
                                    true, BLOCK_SIZE, networkTransaction);
      transaction.setNormalInfoAsWarn(false);
      executorService.execute(transaction);
    }
    Thread.sleep(100);
    executorService.shutdown();
    for (int i = 0; i < NUMBER_FILES; i++) {
      final R66Future future = futures.remove(0);
      future.awaitOrInterruptible();
      assertTrue(future.isSuccess());
    }
    long timestop = System.currentTimeMillis();
    logger.warn(
        "RecvThrough {} files from R2 ({} seconds,  {} per seconds) of " +
        "size {} with block size {}", NUMBER_FILES,
        (timestop - timestart) / 1000,
        NUMBER_FILES * 1000 / (timestop - timestart), LARGE_SIZE, BLOCK_SIZE);
    outHello.delete();
    FileUtils.forceDeleteRecursiveDir(baseDir);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test020_MultipleSends_Through_Itself()
      throws IOException, InterruptedException {
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    Assume.assumeNotNull(networkTransaction);
    final R66Future future = new R66Future(true);
    final MultipleSubmitTransfer transaction =
        new MultipleSubmitTransfer(future, "server1", "hello*", "third",
                                   "test multiple with jump through itself",
                                   true, 1024, DbConstantR66.ILLEGALVALUE, null,
                                   networkTransaction);
    transaction.setNormalInfoAsWarn(false);
    transaction.run();
    future.awaitOrInterruptible();
    assertTrue(future.isSuccess());
    long timestart = System.currentTimeMillis();
    File dirR1 = new File("/tmp/R66/scenario_1_2_3/R1/in");
    File dirR3 = new File("/tmp/R66/scenario_1_2_3/R3/in");
    int max = 0;
    long timestop;
    if (SERVER1_IN_JUNIT) {
      InternalRunner internalRunner =
          Configuration.configuration.getInternalRunner();
      for (int i = 0; i < NUMBER_FILES * 10; i++) {
        Thread.sleep(100);
        if (internalRunner != null) {
          max = Math.max(max, internalRunner.nbInternalRunner());
        }
        int count = dirR3.list().length;
        if (count == NUMBER_FILES) {
          break;
        }
      }
      timestop = System.currentTimeMillis();
      Thread.sleep(1000);
      logger.warn(
          "Sent {} files to R2, then {} to R3, using at most {} parallel clients" +
          " ({} seconds, {} per seconds)", dirR1.list().length,
          dirR3.list().length, max, (timestop - timestart) / 1000,
          NUMBER_FILES * 1000 / (timestop - timestart));
    } else {
      for (int i = 0; i < NUMBER_FILES * 10; i++) {
        Thread.sleep(200);
        int count = dirR3.list().length;
        if (count == NUMBER_FILES) {
          break;
        }
      }
      timestop = System.currentTimeMillis();
      Thread.sleep(1000);
      logger.warn(
          "Sent {} files to R1, then {} to R3 ({} seconds, {} per seconds)",
          dirR1.list().length, dirR3.list().length,
          (timestop - timestart) / 1000,
          NUMBER_FILES * 1000 / (timestop - timestart));
    }
    FileUtils.forceDeleteRecursiveDir(dirR1);
    FileUtils.forceDeleteRecursiveDir(dirR3);
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test04_5000_MultipleSends()
      throws IOException, InterruptedException {
    checkBigIt();
    int lastNumber = NUMBER_FILES;
    NUMBER_FILES = 5000;
    test010_MultipleSends();
    // Extra sleep to check correctness if necessary on Logs
    Thread.sleep(1000);
    // Ensure the last send is ok
    test011_SendToItself();
    // Extra sleep to check correctness if necessary on Logs
    Thread.sleep(1000);
    test020_MultipleSends_Through_Itself();
    // Extra sleep to check correctness if necessary on Logs
    Thread.sleep(1000);
    // Ensure the last send is ok
    test011_SendToItself();
    NUMBER_FILES = lastNumber;
    // Extra sleep to check correctness if necessary on Logs
    Thread.sleep(5000);
  }

  @Test
  public void test04_5000_MultipleSends_ChangingBlockSize()
      throws IOException, InterruptedException {
    checkBigIt();
    int lastNumber = NUMBER_FILES;
    NUMBER_FILES = 800;
    BLOCK_SIZE = 16 * 1024;
    test012_MultipleRecvsSync();
    // Extra sleep to check correctness if necessary on Logs
    Thread.sleep(1000);
    // Ensure the last send is ok
    test011_SendToItself();
    // Extra sleep to check correctness if necessary on Logs
    Thread.sleep(1000);
    BLOCK_SIZE = 64 * 1024;
    test012_MultipleRecvsSync();
    // Extra sleep to check correctness if necessary on Logs
    Thread.sleep(1000);
    // Ensure the last send is ok
    test011_SendToItself();
    // Extra sleep to check correctness if necessary on Logs
    Thread.sleep(1000);
    BLOCK_SIZE = 128 * 1024;
    test012_MultipleRecvsSync();
    // Extra sleep to check correctness if necessary on Logs
    Thread.sleep(1000);
    // Ensure the last send is ok
    test011_SendToItself();
    // Extra sleep to check correctness if necessary on Logs
    Thread.sleep(1000);
    BLOCK_SIZE = 512 * 1024;
    test012_MultipleRecvsSync();
    // Extra sleep to check correctness if necessary on Logs
    Thread.sleep(1000);
    // Ensure the last send is ok
    test011_SendToItself();
    // Extra sleep to check correctness if necessary on Logs
    Thread.sleep(5000);
    NUMBER_FILES = lastNumber;
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
