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

package org.waarp.openr66.it;

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
import org.waarp.common.utility.Processes;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.client.SubmitTransfer;
import org.waarp.openr66.client.TransferArgs;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNetworkException;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.s3.WaarpR66S3Client;
import org.waarp.openr66.server.R66Server;
import org.waarp.openr66.server.ServerInitDatabase;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class S3ScenarioBaseLoopBenchmark extends S3TestAbstract {

  protected static S3ScenarioBaseLoopBenchmark scenarioBase;
  private static final String PATH_COMMON = "scenario_loop_benchmark";
  private static final String SERVER_1_XML = "R1/conf/server_1_SQLDB.xml";
  private static final String SERVER_1_REWRITTEN_XML = "R1/conf/server.xml";
  // Server 1 uses explicit loading
  private static final String RESOURCES_SERVER_1_XML =
      "it/" + PATH_COMMON + "/" + SERVER_1_XML;
  // Server 2 uses implicit loading
  private static final String SERVER_2_XML = "R2/conf/server_2.xml";

  private static final List<Integer> PIDS = new ArrayList<Integer>();
  private static final String TMP_R66_SCENARIO_R1 =
      "/tmp/R66/" + PATH_COMMON + "/R1";
  private static final String TMP_R66_CONFIG_R1 =
      "/tmp/R66/" + PATH_COMMON + "/" + SERVER_1_REWRITTEN_XML;
  public static int NUMBER_FILES = 1000;
  public static int BLOCK_SIZE = 1048576;
  public static long MAX_USED_MEMORY = 2048 * 1024 * 1024;

  private static int r66Pid1 = 999999;
  private static int r66Pid2 = 999999;
  public static boolean SERVER1_IN_JUNIT = true;
  private static final String TMP_CONFIG_XML = "/tmp/config.xml";
  private static File configFile = null;
  private long usedMemory;

  static URL s3Url = null;
  static final String ACCESS_KEY = "accessKey";
  static final String SECRET_KEY = "secretKey";
  static final String BUCKET = "bucket-test";
  static final String FILEPATHSRC = "/directory1/s3file";
  static String rulename = "loop";

  public static void setUpBeforeClass() throws Exception {
    ResourceLeakDetector.setLevel(Level.SIMPLE);
    final ClassLoader classLoader =
        S3ScenarioBaseLoopBenchmark.class.getClassLoader();
    File file =
        new File(classLoader.getResource(RESOURCES_SERVER_1_XML).getFile());
    dirResources = file.getParentFile().getParentFile().getParentFile();
    logger.warn(dirResources.getAbsolutePath());
    setUpBeforeClassMinimal(RESOURCES_SERVER_1_XML);
    File r1 = new File(dirResources, "R1/conf");
    logger.warn("Dir {} exists ? {}", r1.getAbsolutePath(), r1.exists());
    createBaseR66Directory(r1, TMP_R66_SCENARIO_R1);
    File r2 = new File(dirResources, "R2/conf");
    createBaseR66Directory(r2, "/tmp/R66/" + PATH_COMMON + "/R2");
    setUp3DbBeforeClass();
    Configuration.configuration.setTimeoutCon(30000);
    if (SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      Processes.setMemoryAccordingToFreeMemory(SERVER1_IN_JUNIT? 3 : 4);
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
    Thread.sleep(1000);
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
    ClassLoader classLoader =
        S3ScenarioBaseLoopBenchmark.class.getClassLoader();
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
    final ClassLoader classLoader =
        S3ScenarioBaseLoopBenchmark.class.getClassLoader();
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
    logger.warn("Home {} exists? {}", homeDir, homeDir.isDirectory());
    logger.warn("Dir2 {} exists? {}", dir2, dir2.isDirectory());
    logger.warn("File {} exists? {}", file, file.isFile());
    final File fileAuth =
        new File(dir2.getParentFile().getParentFile(), "OpenR66-authent.xml");
    logger.warn("File {} exists? {}", fileAuth, fileAuth.isFile());

    final String[] args = {
        file.getAbsolutePath(), "-initdb", "-dir", dir2.getAbsolutePath(),
        "-auth", fileAuth.getAbsolutePath(), "-loadExtendedTaskFactory",
        "org.waarp.openr66.s3.taskfactory.S3TaskFactory"
    };
    Processes.executeJvm(project, ServerInitDatabase.class, args, false);
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
      Processes.executeJvm(project, R66Server.class, argsServer, true);
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

  public static void tearDownAfterClass(final Thread delegate)
      throws Exception {
    CloseableHttpClient httpClient = null;
    int max = SystemPropertyUtil.get(IT_LONG_TEST, false)? 8000 : 60;
    int totalTransfers = max;
    int nb = 0;
    int every10sec = 10;
    final long startTime = System.currentTimeMillis();
    try {
      httpClient = HttpClientBuilder.create().setConnectionManagerShared(true)
                                    .disableAutomaticRetries().build();
      final HttpGet request =
          new HttpGet("http://127.0.0.1:8098/v2/transfers?countOrder=true");
      CloseableHttpResponse response = null;
      try {
        response = httpClient.execute(request);
        if (200 == response.getStatusLine().getStatusCode()) {
          final String content = EntityUtils.toString(response.getEntity());
          final ObjectNode node = JsonHandler.getFromString(content);
          if (node != null) {
            final JsonNode number = node.findValue("totalResults");
            final long newNb = number.asLong();
            max += newNb;
            totalTransfers = (int) newNb;
            logger.warn("Found {} transfers", newNb);
          }
        }
      } finally {
        if (response != null) {
          response.close();
        }
      }
      while (nb < max) {
        try {
          delegate.run();
          response = httpClient.execute(request);
          if (200 != response.getStatusLine().getStatusCode()) {
            break;
          }
          final String content = EntityUtils.toString(response.getEntity());
          final ObjectNode node = JsonHandler.getFromString(content);
          if (node != null) {
            final JsonNode number = node.findValue("totalResults");
            final int newNb = number.asInt();
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
    totalTransfers = nb - totalTransfers;
    logger.warn("Duration {} for {} item so {} items/s",
                (stopTime - startTime) / 1000.0, totalTransfers,
                totalTransfers / ((stopTime - startTime) / 1000.0));
    WaarpSystemUtil.stopLogger(true);
    Configuration.configuration.setTimeoutCon(100);
    for (int pid : PIDS) {
      Processes.kill(pid, true);
    }
    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    tearDownAfterClassServer();
    final File base = new File("/tmp/R66");
    FileUtils.deleteRecursiveDir(base);
  }

  @Before
  public void setUp() throws Exception {
    Configuration.configuration.setTimeoutCon(30000);
    final Runtime runtime = Runtime.getRuntime();
    usedMemory = runtime.totalMemory() - runtime.freeMemory();
  }

  protected abstract Thread getDeletgated();

  @After
  public void tearDown() throws Exception {
    getDeletgated().run();
    Thread.sleep(100);
  }

  private void checkMemory() {
    final Runtime runtime = Runtime.getRuntime();
    final long newUsedMemory = runtime.totalMemory() - runtime.freeMemory();
    if (newUsedMemory > MAX_USED_MEMORY) {
      logger.info("Used Memory > 2GB {} {}", usedMemory / 1048576.0,
                  newUsedMemory / 1048576.0);
    }
  }

  private int initBenchmark(final int gap)
      throws IOException, OpenR66ProtocolNetworkException, Reply550Exception {
    NUMBER_FILES = SystemPropertyUtil.get(IT_LONG_TEST, false)? 2000 : 10;
    final int factor = 250 * 1024 * 2 / NUMBER_FILES;
    Assume.assumeNotNull(networkTransaction);
    Configuration.configuration.changeNetworkLimit(0, 0, 0, 0, 1000);
    if (rulename.equalsIgnoreCase("loop")) {
      final File baseDir = new File("/tmp/R66/" + PATH_COMMON + "/R1/test/");
      baseDir.mkdirs();
      final WaarpR66S3Client client =
          new WaarpR66S3Client(ACCESS_KEY, SECRET_KEY, s3Url);
      for (int i = 1; i <= NUMBER_FILES; i++) {
        final int size = (10000 + i * factor) + gap;
        final File fileOut = new File(baseDir, "hello" + size);
        final File outHello = generateOutFile(fileOut.getAbsolutePath(), size);
        client.createFile(BUCKET, FILEPATHSRC + size, outHello, null);
      }
    } else {
      final File baseDir = new File("/tmp/R66/" + PATH_COMMON + "/R1/out/");
      baseDir.mkdirs();
      final File baseDir2 = new File("/tmp/R66/" + PATH_COMMON + "/R2/out/");
      baseDir2.mkdirs();
      for (int i = 1; i <= NUMBER_FILES; i++) {
        final int size = (10000 + i * factor) + gap;
        final File fileOut = new File(baseDir, "hello" + size);
        generateOutFile(fileOut.getAbsolutePath(), size);
        final File fileOut2 = new File(baseDir2, "hello" + size);
        FileUtils.copy(fileOut, fileOut2, false, false);
      }
    }
    logger.warn("End of file creations");
    return factor;
  }

  private void runLoopInit(String ruleName, String serverName, int factor,
                           int gap) {
    try {
      DbRule rule = new DbRule(ruleName);
    } catch (WaarpDatabaseException e) {
      e.printStackTrace();
    }
    R66Future[] futures = new R66Future[NUMBER_FILES];
    for (int i = 1; i <= NUMBER_FILES; i++) {
      int size = (10000 + i * factor) + gap;
      final R66Future future = new R66Future(true);
      futures[i - 1] = future;
      /*
      final TestTransferNoDb transaction =
              new TestTransferNoDb(future, "server2-ssl", "hello" + size, "loop",
                      "Test Loop Send", true, BLOCK_SIZE,
                      DbConstantR66.ILLEGALVALUE,
                      networkTransaction);
      transaction.setNormalInfoAsWarn(false);
      transaction.run();
       */

      final SubmitTransfer transaction =
          new SubmitTransfer(future, serverName, "hello" + size, ruleName,
                             (s3Url == null? "" : s3Url) + " " + FILEPATHSRC +
                             size + " Test Loop Send " + size, true, BLOCK_SIZE,
                             DbConstantR66.ILLEGALVALUE, null);
      TransferArgs.forceAnalyzeFollow(transaction);
      transaction.setNormalInfoAsWarn(false);
      transaction.run();
    }
    for (int i = 0; i < NUMBER_FILES; i++) {
      futures[i].awaitOrInterruptible();
      assertTrue(futures[i].isSuccess());
    }
  }

  @Test
  public void test01_LoopBenchmarkSendsSyncSslNoLimit()
      throws IOException, OpenR66ProtocolNetworkException, Reply550Exception {
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    int factor = initBenchmark(0);
    String serverName = "server2-ssl";
    String ruleName = rulename;
    long timestart = System.currentTimeMillis();
    runLoopInit(ruleName, serverName, factor, 0);
    long timestop = System.currentTimeMillis();
    logger.warn("Direct {}, Recv {}, LimitBandwidth {} " +
                "({} seconds,  {} MBPS vs {} " +
                "and {}) of size {} with block size {}", true, false,
                NUMBER_FILES, (timestop - timestart) / 1000,
                NUMBER_FILES * (10000 + factor * (NUMBER_FILES / 2)) / 1000.0 /
                (timestop - timestart),
                Configuration.configuration.getServerGlobalReadLimit() /
                1000000.0,
                Configuration.configuration.getServerChannelReadLimit() /
                1000000.0, (factor * (NUMBER_FILES / 2)), BLOCK_SIZE);
    checkMemory();
    logger.warn("End {}", Processes.getCurrentMethodName());
  }

  @Test
  public void test02_LoopBenchmarkSendsSyncNoLimit()
      throws IOException, OpenR66ProtocolNetworkException, Reply550Exception {
    logger.warn("Start {} {}", Processes.getCurrentMethodName(), NUMBER_FILES);
    int factor = initBenchmark(1);
    String serverName = "server2";
    String ruleName = rulename;
    long timestart = System.currentTimeMillis();
    runLoopInit(ruleName, serverName, factor, 1);
    long timestop = System.currentTimeMillis();
    logger.warn("Direct {}, Recv {}, LimitBandwidth {} " +
                "({} seconds,  {} MBPS vs {} " +
                "and {}) of size {} with block size {}", true, false,
                NUMBER_FILES, (timestop - timestart) / 1000,
                NUMBER_FILES * (10000 + factor + (NUMBER_FILES / 2)) / 1000.0 /
                (timestop - timestart),
                Configuration.configuration.getServerGlobalReadLimit() /
                1000000.0,
                Configuration.configuration.getServerChannelReadLimit() /
                1000000.0, (factor + (NUMBER_FILES / 2)), BLOCK_SIZE);
    checkMemory();
    logger.warn("End {}", Processes.getCurrentMethodName());
  }
}
