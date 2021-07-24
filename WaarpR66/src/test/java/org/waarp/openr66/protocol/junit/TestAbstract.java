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

package org.waarp.openr66.protocol.junit;

import org.apache.tools.ant.Project;
import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.file.FileUtils;
import org.waarp.common.utility.Processes;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.client.utils.OutputFormat.FIELDS;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.KeepAlivePacket;
import org.waarp.openr66.protocol.localhandler.packet.NoOpPacket;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.server.R66Server;
import org.waarp.openr66.server.ServerInitDatabase;

import java.io.File;
import java.io.FileFilter;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static org.junit.Assert.*;

/**
 *
 */
public abstract class TestAbstract extends TestAbstractMinimal {

  protected static final String TMP_R_66_CONF_CONFIG_XML =
      "/tmp/R66/conf/config.xml";

  protected static NetworkTransaction networkTransaction;
  protected static Project project;
  protected static File homeDir;
  public static WebDriver driver = null;

  public enum DriverType {
    PHANTOMJS
  }

  public static DriverType driverType = DriverType.PHANTOMJS;

  public static void setUpDbBeforeClass() throws Exception {
    deleteBase();
    String serverInit = "Linux/config/config-serverInitB.xml";
    setUpBeforeClassMinimal(serverInit);
    final ClassLoader classLoader = TestAbstract.class.getClassLoader();
    WaarpSystemUtil.setJunit(true);
    File file = new File(classLoader.getResource(serverInit).getFile());
    final String newfile = file.getAbsolutePath().replace("target/test-classes",
                                                          "src/test/resources");
    file = new File(newfile);
    dir = file.getParentFile();
    dirResources = dir;
    logger.warn("File {} exists? {}", file, file.isFile());
    homeDir =
        dir.getParentFile().getParentFile().getParentFile().getParentFile()
           .getParentFile();
    logger.warn("Dir {} exists? {}", homeDir, homeDir.isDirectory());

    // global ant project settings
    project = Processes.getProject(homeDir);
    serverInit = "config-serverInitB.xml";
    initiateDb(serverInit);
    serverInit = "config-serverInitA.xml";
    initiateDb(serverInit);

    Processes.finalizeProject(project);
  }

  protected static void sendInformation(AbstractLocalPacket informationPacket,
                                        final SocketAddress socketServerAddress,
                                        R66Future future, byte scode,
                                        boolean check,
                                        R66FiniteDualStates state,
                                        boolean isSsl)
      throws OpenR66ProtocolPacketException {
    logger.warn("Start connection for Extra commands {} \n\t{}",
                informationPacket.getClass().getSimpleName(),
                informationPacket);
    final LocalChannelReference localChannelReference =
        networkTransaction.createConnectionWithRetry(socketServerAddress, isSsl,
                                                     future);
    if (localChannelReference == null) {
      if (state != R66FiniteDualStates.SHUTDOWN) {
        fail("Connection not OK");
      } else {
        return;
      }
    }
    localChannelReference.sessionNewState(state);
    logger.warn("Send {}", informationPacket);
    ChannelUtils.writeAbstractLocalPacket(localChannelReference,
                                          informationPacket, false);
    if (informationPacket instanceof KeepAlivePacket ||
        informationPacket instanceof NoOpPacket) {
      // do no await
      localChannelReference.getFutureValidRequest().setSuccess();
      localChannelReference.close();
      return;
    } else {
      localChannelReference.getFutureRequest().awaitOrInterruptible();
      future.awaitOrInterruptible();
      if (!(localChannelReference.getFutureRequest().isDone() &&
            future.isDone())) {
        fail("Cannot send information");
      }
    }
    final R66Result r66result = future.getResult();
    if (state == R66FiniteDualStates.VALIDOTHER) {
      logger.warn("feedback: {}", r66result);
    } else {
      final ValidPacket info = (ValidPacket) r66result.getOther();
      if (info != null && scode != -1) {
        logger.debug("nb: {}", Integer.parseInt(info.getSmiddle()));
        final String[] files = info.getSheader().split("\n");
        int i = 0;
        for (final String file : files) {
          i++;
          logger.debug("file: {} {}", i, file);
        }
      } else {
        if (info != null && info.getSheader() != null) {
          try {
            final DbTaskRunner runner =
                DbTaskRunner.fromStringXml(info.getSheader(), false);
            logger.warn("{}", runner.getJson());
          } catch (final OpenR66ProtocolBusinessException e) {
            logger.warn("{}: {}", FIELDS.transfer.name(), info.getSheader());
          }
        }
      }
    }
    localChannelReference.close();
    if (check) {
      assertTrue("Information not OK", future.isSuccess());
    } else {
      logger.warn("Get {} {}", future.isSuccess(), future.isSuccess()? "OK" :
          future.getCause() != null? future.getCause().getMessage() :
              "No Cause");
    }
  }

  public static void initiateWebDriver(File file) {
    File libdir = file.getParentFile().getParentFile().getParentFile();
    // test-classes -> target -> WaarpR66 -> lib -> geckodriver (linux x64)
    File libPhantomJS = new File("/tmp/phantomjs-2.1.1");
    if (!libPhantomJS.canRead()) {
      File libPhantomJSZip = new File(libdir, "lib/phantomjs-2.1.1.bz2");
      if (libPhantomJSZip.canRead()) {
        FileUtils.uncompressedBz2File(libPhantomJSZip, libPhantomJS);
        libPhantomJS.setExecutable(true);
      }
    }
    assertTrue(libPhantomJS.exists());
    System.setProperty("phantomjs.binary.path", libPhantomJS.getAbsolutePath());
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
    driver.manage().window().setSize(new Dimension(1920, 1080));
    //driver.manage().timeouts().pageLoadTimeout(10, TimeUnit.SECONDS);
    Thread.sleep(10);
    return driver;
  }

  private static WebDriver createPhantomJSDriver() {
    DesiredCapabilities desiredCapabilities =
        new DesiredCapabilities("phantomjs", "", Platform.ANY);
    desiredCapabilities.setCapability(
        PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY,
        System.getProperty("phantomjs.binary.path"));
    desiredCapabilities.setCapability(CapabilityType.ELEMENT_SCROLL_BEHAVIOR,
                                      true);
    desiredCapabilities.setCapability(CapabilityType.TAKES_SCREENSHOT, false);
    desiredCapabilities.setCapability(
        CapabilityType.ENABLE_PROFILING_CAPABILITY, false);
    LoggingPreferences logPrefs = new LoggingPreferences();
    logPrefs.enable(LogType.BROWSER, Level.OFF);
    logPrefs.enable(LogType.CLIENT, Level.OFF);
    logPrefs.enable(LogType.DRIVER, Level.OFF);
    logPrefs.enable(LogType.PERFORMANCE, Level.OFF);
    logPrefs.enable(LogType.PROFILER, Level.OFF);
    logPrefs.enable(LogType.SERVER, Level.OFF);
    desiredCapabilities.setCapability(CapabilityType.LOGGING_PREFS, logPrefs);
    desiredCapabilities.setCapability(CapabilityType.HAS_NATIVE_EVENTS, true);
    desiredCapabilities.setCapability(
        PhantomJSDriverService.PHANTOMJS_GHOSTDRIVER_CLI_ARGS,
        "--webdriver-loglevel=NONE");
    desiredCapabilities.setJavascriptEnabled(true);

    ArrayList<String> cliArgs = new ArrayList<String>();
    cliArgs.add("--web-security=true");
    cliArgs.add("--ignore-ssl-errors=true");
    cliArgs.add("--webdriver-loglevel=NONE");
    desiredCapabilities.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS,
                                      cliArgs);

    PhantomJSDriver phantomJSDriver = new PhantomJSDriver(desiredCapabilities);
    phantomJSDriver.setLogLevel(Level.OFF);
    return phantomJSDriver;
  }

  public static void finalizeDriver() throws InterruptedException {
    // 17 | close |  |  |
    // driver.close();
    if (driver != null) {
      try {
        driver.quit();
      } catch (Exception e) {
        // Ignore
      }
      driver = null;
    }
    Thread.sleep(10);
  }

  public static void initiateDb(String serverInit) {
    WaarpSystemUtil.setJunit(true);
    final File file = new File(dir, serverInit);
    logger.warn("File {} exists? {}", file, file.isFile());
    final File fileAuth = new File(dir, "OpenR66-authent-A.xml");
    final File fileLimit = new File(dir, "limitConfiga.xml");
    logger.warn("File {} exists? {}", fileAuth, fileAuth.isFile());
    logger.warn("File {} exists? {}", fileLimit, fileLimit.isFile());

    final String[] args = {
        file.getAbsolutePath(), "-initdb", "-dir", dir.getAbsolutePath(),
        "-auth", new File(dir, "OpenR66-authent-A.xml").getAbsolutePath(),
        "-limit", new File(dir, "limitConfiga.xml").getAbsolutePath()
    };
    Processes.executeJvm(project, ServerInitDatabase.class, args, false);
  }

  /**
   * @throws Exception
   */
  public static void setUpBeforeClassServer(String serverInit,
                                            String serverConfig, boolean start)
      throws Exception {

    if (Configuration.configuration != null &&
        Configuration.configuration.isConfigured()) {
      // Ensure we redo configuration
      Configuration.configuration.setConfigured(false);
      Configuration.configuration = new Configuration();
    }
    final ClassLoader classLoader = TestAbstract.class.getClassLoader();
    final File file;
    if (serverInit.startsWith("/")) {
      file = new File(serverInit);
    } else {
      file = new File(classLoader.getResource(serverInit).getFile());
    }
    if (file.exists()) {
      dir = file.getParentFile();
      logger.debug("Find serverInit file: " + file.getAbsolutePath());
      ServerInitDatabase.main(new String[] {
          file.getAbsolutePath(), "-initdb", "-dir", dir.getAbsolutePath(),
          "-auth", new File(dir, "OpenR66-authent-A.xml").getAbsolutePath(),
          "-limit", new File(dir, "limitConfiga.xml").getAbsolutePath()
      });
      Configuration.configuration.setTimeoutCon(100);
      logger.warn("Init Done");
      if (start) {
        FileBasedConfiguration.resetAlreadySetLimit();
        final File file2;
        if (serverConfig.startsWith("/")) {
          file2 = new File(serverConfig);
        } else {
          file2 = new File(dir, serverConfig);
        }
        if (file2.exists()) {
          logger.warn("Find server file: " + file2.getAbsolutePath());
          R66Server.main(new String[] { file2.getAbsolutePath() });
          logger.warn("Start Done");
          Thread.sleep(1000);
        } else {
          logger.error("Cannot find server file: " + file2.getAbsolutePath());
        }
      }
    } else {
      logger.error("Cannot find serverInit file: " + file.getAbsolutePath());
    }
  }

  public static void tearDownAfterClassServer() throws Exception {
    int pid;
    if ((pid =
        Processes.getPidOfRunnerJavaCommandLinux(R66Server.class.getName())) !=
        -1) {
      logger.error(
          "####################### R66 SERVER STILL RUNNING {} ###################",
          pid);
      System.err.println(
          "####################### R66 SERVER STILL RUNNING " + pid +
          " ###################");
      Processes.kill(pid, false);
    } else {
      logger.warn(
          "####################### NO R66 SERVER RUNNING ###################");
      System.out.println(
          "####################### NO R66 SERVER RUNNING ###################");
    }
    WaarpSystemUtil.stopLogger(true);
    Configuration.configuration.setTimeoutCon(100);
    ChannelUtils.exit();
    deleteBase();
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

  public static void setUpBeforeClassClient(String clientConfig)
      throws Exception {
    final File clientConfigFile;
    if (clientConfig.startsWith("/")) {
      clientConfigFile = new File(clientConfig);
    } else {
      clientConfigFile = new File(dir, clientConfig);
    }
    logger.warn("Will try to load {}", clientConfigFile);
    if (clientConfigFile.isFile()) {
      logger.debug(
          "Find serverInit file: " + clientConfigFile.getAbsolutePath());
      if (!FileBasedConfiguration.setClientConfigurationFromXml(
          Configuration.configuration, clientConfigFile.getAbsolutePath())) {
        logger.error("Needs a correct configuration file as first argument");
        return;
      }
    } else {
      logger.error("Needs a correct configuration file as first argument: {}",
                   clientConfigFile.getAbsolutePath());
      return;
    }
    Configuration.configuration.setTimeoutCon(100);
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
      DAOFactory.closeDAO(transferAccess);
    }
  }

  public static void setUpBeforeClassClient() throws Exception {
    Configuration.configuration.setTimeoutCon(100);
    networkTransaction =
        Configuration.configuration.getInternalRunner().getNetworkTransaction();
    DbTaskRunner.clearCache();
    TransferDAO transferAccess = null;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();
      transferAccess.deleteAll();
    } catch (final DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } finally {
      DAOFactory.closeDAO(transferAccess);
    }
  }

  public static void tearDownAfterClassClient() throws Exception {
    WaarpSystemUtil.stopLogger(true);
    Configuration.configuration.setTimeoutCon(100);
    if (networkTransaction != null) {
      networkTransaction.closeAll();
    }
  }

  @Before
  public void setUp() throws Exception {
    Configuration.configuration.setTimeoutCon(10000);
    Configuration.configuration.changeNetworkLimit(1000000000, 1000000000,
                                                   1000000000, 1000000000,
                                                   1000);
  }

  @After
  public void tearDown() throws Exception {
    Configuration.configuration.setTimeoutCon(100);
    Thread.sleep(100);
  }
}
