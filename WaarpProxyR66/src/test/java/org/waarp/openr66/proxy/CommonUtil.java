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

package org.waarp.openr66.proxy;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.common.utility.Processes;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.server.R66Server;
import org.waarp.openr66.server.ServerInitDatabase;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public abstract class CommonUtil {
  private static final String OPEN_R_66_AUTHENT_THROUGH_PROXY_XML =
      "OpenR66-authent-ThroughProxy.xml";
  private static final String OPEN_R_66_AUTHENT_A_XML = "OpenR66-authent-A.xml";
  private static final String LIMIT_CONFIGA_XML = "limitConfiga.xml";
  static String CONFIG_PROXY_XML = "config-proxy.xml";
  private static final String CONFIG_SERVER_INIT_B_XML =
      "config-serverInitB.xml";
  private static final String CONFIG_CLIENT_B_XML = "config-clientB.xml";
  private static final String CONFIG_SERVER_A_MINIMAL_XML =
      "config-serverA-minimal.xml";
  private static final String CONFIG_SERVER_INIT_A_XML =
      "config-serverInitA.xml";
  protected static WaarpLogger logger;
  static File home;
  static File r66Resources;
  static File resources;
  static File projectHome;
  static Project project;
  static File toSend;
  static File toRecv;
  static NetworkTransaction networkTransaction;
  static Configuration r66Configuration;
  static Configuration proxyConfiguration;
  static int r66pid = 0;
  static boolean testShouldFailed;
  public static WebDriver driver = null;

  public enum DriverType {
    PHANTOMJS // Works for R66
  }

  public static DriverType driverType = DriverType.PHANTOMJS;

  public static void launchServers() throws Exception {
    WaarpLoggerFactory
        .setDefaultFactory(new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    logger = WaarpLoggerFactory.getLogger(CommonUtil.class);
    // Setup directories : /tmp/R66 and sub dirs
    DetectionUtils.setJunit(true);
    setupResources();
    // Setup files needed for test (recv/send)
    toSend = new File(home, "in/toSend.txt");
    generateOutFile(toSend.getAbsolutePath(), 10000);
    toRecv = new File(home, "out/toRecv.txt");
    generateOutFile(toRecv.getAbsolutePath(), 10000);

    // Launch R66 remote server using resources/r66 directory
    setUpDbBeforeClass();
    setUpBeforeClassServer(true, OPEN_R_66_AUTHENT_THROUGH_PROXY_XML);
    // Launch R66Proxy server using resources directory
    Thread.sleep(1000);
    startUpProxy();
    Configuration.configuration.setTimeoutCon(100);
    // Move to clientB
    setUpBeforeClassClient(CONFIG_CLIENT_B_XML);
    Configuration.configuration.setTimeoutCon(100);
  }

  @AfterClass
  public static void stopServers() throws Exception {
    Configuration.configuration.setTimeoutCon(100);
    finalizeDriver();
    // Stop R66 remote server using resources/r66 directory
    Configuration.configuration.setTimeoutCon(100);
    tearDownAfterClass();
    // Stop R66Proxy server using resources directory
    Configuration.configuration.setTimeoutCon(100);
    shutdownProxy();
    // Clean directories
    FileUtils.forceDeleteRecursiveDir(home);
  }

  public static void setupResources() throws Exception {
    final ClassLoader classLoader = CommonUtil.class.getClassLoader();
    File file = new File(
        classLoader.getResource("r66/config-serverInitA.xml").getFile());
    if (file.exists()) {
      // r66 -> resources -> test -> src -> WaarpR66Proxy -> lib -> geckodriver
      initiateWebDriver(file.getParentFile().getParentFile());
    }
    final String newfile = file.getAbsolutePath().replace("target/test-classes",
                                                          "src/test/resources");
    file = new File(newfile);
    if (file.exists()) {
      // R66 Home
      home = new File("/tmp/R66");
      // R66 resources directory
      r66Resources = file.getParentFile();
      // Resources directory
      resources = r66Resources.getParentFile();
      // Project Home directory
      projectHome = resources.getParentFile().getParentFile().getParentFile();
      home.mkdirs();
      FileUtils.forceDeleteRecursiveDir(home);
      new File(home, "in").mkdir();
      new File(home, "out").mkdir();
      new File(home, "arch").mkdir();
      new File(home, "work").mkdir();
      final File conf = new File(home, "conf");
      conf.mkdir();
      // Copy to final home directory
      final File[] copied = FileUtils.copyRecursive(r66Resources, conf, false);
      for (final File fileCopied : copied) {
        System.out.print(fileCopied.getAbsolutePath() + ' ');
      }
      System.out.println(" Done");
    } else {
      System.err
          .println("Cannot find serverInit file: " + file.getAbsolutePath());
      fail("Cannot find serverInit file");
    }
  }

  static File generateOutFile(String name, int size) throws IOException {
    final File file = new File(name);
    final FileWriter fileWriterBig = new FileWriter(file);
    for (int i = 0; i < size / 10; i++) {
      fileWriterBig.write("0123456789");
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    return file;
  }

  static void setUpDbBeforeClass() throws Exception {
    deleteBase();
    logger.warn("Dir {} exists? {}", projectHome, projectHome.isDirectory());

    // global ant project settings
    project = Processes.getProject(projectHome);

    initiateDbB();
    initiateDbA();

    Processes.finalizeProject(project);
    project = null;
  }

  private static void initiateDbB() {
    final File fileAuthProxy =
        new File(r66Resources, OPEN_R_66_AUTHENT_THROUGH_PROXY_XML);
    logger.warn("File {} exists? {}", fileAuthProxy, fileAuthProxy.isFile());
    assertTrue(fileAuthProxy.isFile());
    initiateDb(CONFIG_SERVER_INIT_B_XML, fileAuthProxy);
  }

  private static void initiateDbA() {
    final File fileAuth = new File(r66Resources, OPEN_R_66_AUTHENT_A_XML);
    logger.warn("File {} exists? {}", fileAuth, fileAuth.isFile());
    assertTrue(fileAuth.isFile());
    initiateDb(CONFIG_SERVER_INIT_A_XML, fileAuth);
  }

  private static void initiateDb(String serverInit, File fileAuth) {
    final File file = new File(r66Resources, serverInit);
    logger.warn("File {} exists? {}", file, file.isFile());
    final File fileLimit = new File(r66Resources, LIMIT_CONFIGA_XML);
    logger.warn("File {} exists? {}", fileLimit, fileLimit.isFile());
    assertTrue(file.isFile());
    assertTrue(fileLimit.isFile());

    final String[] args = {
        file.getAbsolutePath(), "-initdb", "-dir",
        r66Resources.getAbsolutePath(), "-auth", fileAuth.getAbsolutePath(),
        "-limit", fileLimit.getAbsolutePath()
    };
    Processes.executeJvm(project, projectHome, ServerInitDatabase.class, args,
                         false);
  }

  public static void deleteBase() {
    final File tmp = new File("/tmp");
    final File[] files = tmp.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return file.getName().startsWith("openr66");
      }
    });
    for (final File file : files) {
      file.delete();
    }
  }

  static void setUpBeforeClassServer(boolean start, String authent)
      throws Exception {
    final File file = new File(r66Resources, CONFIG_SERVER_INIT_B_XML);
    if (file.exists()) {
      System.err.println("Find serverInit file: " + file.getAbsolutePath());
      final File fileAuth = new File(r66Resources, authent);
      final File fileLimit = new File(r66Resources, LIMIT_CONFIGA_XML);
      final String[] args = {
          file.getAbsolutePath(), "-initdb", "-dir",
          r66Resources.getAbsolutePath(), "-auth", fileAuth.getAbsolutePath(),
          "-limit", fileLimit.getAbsolutePath()
      };
      ServerInitDatabase.main(args);
      logger.warn("Init Done");
      if (start) {
        final File file2 = new File(r66Resources, CONFIG_SERVER_A_MINIMAL_XML);
        if (file2.exists()) {
          System.err.println("Find server file: " + file2.getAbsolutePath());
          final String[] argsServer = {
              file2.getAbsolutePath()
          };
          // global ant project settings
          project = Processes.getProject(projectHome);
          r66pid = Processes
              .executeJvm(project, projectHome, R66Server.class, argsServer,
                          true);
          logger.warn("Start Done");
        } else {
          System.err
              .println("Cannot find server file: " + file2.getAbsolutePath());
          fail("Cannot find server file");
        }
      }
    } else {
      System.err
          .println("Cannot find serverInit file: " + file.getAbsolutePath());
      fail("Cannot find serverInit file");
    }
  }

  static void tearDownAfterClass() throws Exception {
    Thread.sleep(200);
    Configuration.configuration.setTimeoutCon(100);
    Processes.kill(r66pid, true);
    Thread.sleep(500);
    if (Processes.exists(r66pid)) {
      Configuration.configuration.setTimeoutCon(100);
      Processes.kill(r66pid, false);
    }
    if (project != null) {
      project.log("finished");
      project.fireBuildFinished(null);
      project = null;
    }
    Configuration.configuration.setTimeoutCon(100);
    tearDownAfterClassClient();
    Configuration.configuration.setTimeoutCon(100);
    tearDownAfterClassServer();
    Thread.sleep(500);
    final File[] list = new File("/tmp").listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return file.getName().startsWith("openr66");
      }
    });
    for (final File file : list) {
      file.delete();
    }
  }

  private static void tearDownAfterClassServer() throws Exception {
    Configuration.configuration.setTimeoutCon(100);
    ChannelUtils.exit();
  }

  static void setUpBeforeClassClient(String clientConfig) throws Exception {
    final File clientConfigFile = new File(r66Resources, clientConfig);
    if (clientConfigFile.isFile()) {
      System.err.println(
          "Find serverInit file: " + clientConfigFile.getAbsolutePath());
      if (!FileBasedConfiguration
          .setClientConfigurationFromXml(Configuration.configuration,
                                         new File(r66Resources, clientConfig)
                                             .getAbsolutePath())) {
        logger.error("Needs a correct configuration file as first argument");
        return;
      }
    } else {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }
    Configuration.configuration.pipelineInit();
    networkTransaction = new NetworkTransaction();
    DbTaskRunner.clearCache();
    TransferDAO transferAccess = null;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();
      transferAccess.deleteAll();
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } finally {
      if (transferAccess != null) {
        transferAccess.close();
      }
    }
  }

  private static void tearDownAfterClassClient() throws Exception {
    networkTransaction.closeAll();
  }

  static void startUpProxy() {
    r66Configuration = Configuration.configuration;
    Configuration.configuration =
        new org.waarp.openr66.proxy.configuration.Configuration();
    final File config = new File(resources, CONFIG_PROXY_XML);
    assertTrue("Configuration file for Proxy must exist", config.isFile());
    if (!org.waarp.openr66.proxy.configuration.FileBasedConfiguration
        .setConfigurationProxyFromXml(Configuration.configuration,
                                      config.getAbsolutePath())) {
      logger.error("Needs a correct configuration file as first argument");
      fail("Needs a correct configuration file as first argument");
    }
    WaarpShutdownHook.removeShutdownHook();
    try {
      Configuration.configuration.serverStartup();
    } catch (final Throwable e) {
      logger.error("Startup of Proxy is in error", e);
      WaarpShutdownHook.terminate(false);
      fail("Needs a correct configuration file as first argument");
    }
    proxyConfiguration = Configuration.configuration;
  }

  static void shutdownProxy() {
    Configuration.configuration.setTimeoutCon(100);
    proxyConfiguration.serverStop();
  }

  @Before
  public void setUp() throws Exception {
    Configuration.configuration.setTimeoutCon(1000);
  }

  @After
  public void tearDown() throws Exception {
    Configuration.configuration.setTimeoutCon(100);
  }

  public static void initiateWebDriver(File file) {
    File libdir = file.getParentFile().getParentFile().getParentFile();
    // test-classes -> target -> WaarpR66 -> lib -> geckodriver (linux x64)
    File libPhnatomJS = new File(libdir, "lib/phantomjs-2.1.1");
    assertTrue(libPhnatomJS.exists());
    System.setProperty("phantomjs.binary.path", libPhnatomJS.getAbsolutePath());
    try {
      driver = initializeDriver();
    } catch (InterruptedException e) {//NOSONAR
      e.printStackTrace();
      fail(e.getMessage());
    }
  }

  public static void reloadDriver() throws InterruptedException {
    if (driver != null) {
      finalizeDriver();
      Thread.sleep(200);
    }
    driver = initializeDriver();
  }

  public static WebDriver initializeDriver() throws InterruptedException {
    WebDriver driver;
    switch (driverType) {
      case PHANTOMJS:
      default:
        driver = createPhantomJSDriver();
    }
    driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
    //driver.manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS);
    Thread.sleep(10);
    return driver;
  }

  private static WebDriver createPhantomJSDriver() {
    DesiredCapabilities desiredCapabilities = DesiredCapabilities.phantomjs();
    desiredCapabilities.setCapability(
        PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
        System.getProperty("phantomjs.binary.path"));
    desiredCapabilities
        .setCapability(CapabilityType.ELEMENT_SCROLL_BEHAVIOR, true);
    desiredCapabilities.setCapability(CapabilityType.TAKES_SCREENSHOT, true);
    desiredCapabilities
        .setCapability(CapabilityType.ENABLE_PROFILING_CAPABILITY, true);
    desiredCapabilities.setCapability(CapabilityType.HAS_NATIVE_EVENTS, true);

    desiredCapabilities.setJavascriptEnabled(true);

    ArrayList<String> cliArgs = new ArrayList<String>();
    cliArgs.add("--web-security=true");
    cliArgs.add("--ignore-ssl-errors=true");
    desiredCapabilities
        .setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, cliArgs);

    return new PhantomJSDriver(desiredCapabilities);
  }

  public static void finalizeDriver() throws InterruptedException {
    // 17 | close |  |  |
    // driver.close();
    driver.quit();
    driver = null;
    Thread.sleep(10);
  }

}