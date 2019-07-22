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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline.Argument;
import org.apache.tools.ant.types.Path;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.common.utility.WaarpShutdownHook;
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
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.KeepAlivePacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.NoOpPacket;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ShutdownOrBlockJsonPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.server.R66Server;
import org.waarp.openr66.server.ServerInitDatabase;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.junit.Assert.*;

public class R66ProxyTest {
  protected static WaarpLogger logger;
  static File home;
  static File r66Resources;
  static File resources;
  static File projectHome;
  static Project project;
  static File toSend;
  static File toRecv;
  static NetworkTransaction networkTransaction;

  @BeforeClass
  public static void launchServers() throws Exception {
    WaarpLoggerFactory
        .setDefaultFactory(new WaarpSlf4JLoggerFactory(WaarpLogLevel.DEBUG));
    logger = WaarpLoggerFactory.getLogger(R66ProxyTest.class);
    // Setup directories : /tmp/R66 and sub dirs
    DetectionUtils.setJunit(true);
    setupResources();
    // Setup files needed for test (recv/send)
    toSend = new File(home, "in/toSend.txt");
    generateOutFile(toSend.getAbsolutePath(), 10000);
    toRecv = new File(home, "in/toRecv.txt");
    generateOutFile(toRecv.getAbsolutePath(), 10000);

    // Launch R66 remote server using resources/r66 directory
    setUpDbBeforeClass();
    setUpBeforeClassServer(true);
    // Launch R66Proxy server using resources directory
    startUpProxy();
    // Move to clientB
    setUpBeforeClassClient("config-clientB.xml");
  }

  @AfterClass
  public static void stopServers() throws Exception {
    // Stop R66Proxy server using resources directory
    shutdownProxy();
    // Stop R66 remote server using resources/r66 directory
    tearDownAfterClass();
    // Clean directories
    FileUtils.forceDeleteRecursiveDir(home);
  }

  public static void setupResources() throws Exception {
    final ClassLoader classLoader = R66ProxyTest.class.getClassLoader();
    File file = new File(
        classLoader.getResource("r66/config-serverInitA.xml").getFile());
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
        System.out.print(fileCopied.getAbsolutePath() + " ");
      }
      System.out.println(" Done");
    } else {
      System.err
          .println("Cannot find serverInit file: " + file.getAbsolutePath());
      fail("Cannot find serverInit file");
    }
  }

  private static File generateOutFile(String name, int size)
      throws IOException {
    final File file = new File(name);
    final FileWriter fileWriterBig = new FileWriter(file);
    for (int i = 0; i < size / 10; i++) {
      fileWriterBig.write("0123456789");
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    return file;
  }

  private static void setUpDbBeforeClass() throws Exception {
    deleteBase();
    final String serverInit = "config-serverInitB.xml";
    final File file = new File(r66Resources, serverInit);
    logger.warn("File {} exists? {}", file, file.isFile());
    assertTrue(file.isFile());
    logger.warn("Dir {} exists? {}", projectHome, projectHome.isDirectory());

    // global ant project settings
    project = new Project();
    project.setBaseDir(projectHome);
    project.init();
    final WaarpLoggerListener listener = new WaarpLoggerListener();
    project.addBuildListener(listener);
    listener.setMessageOutputLevel(Project.MSG_WARN);
    project.fireBuildStarted();

    initiateDbB();
    initiateDbA();

    project.log("finished");
    project.fireBuildFinished(null);
  }

  private static void initiateDbB() {
    initiateDb("config-serverInitB.xml");
  }

  private static void initiateDbA() {
    initiateDb("config-serverInitA.xml");
  }

  private static void initiateDb(String serverInit) {
    final File file = new File(r66Resources, serverInit);
    logger.warn("File {} exists? {}", file, file.isFile());
    final File fileAuth = new File(r66Resources, "OpenR66-authent-A.xml");
    final File fileLimit = new File(r66Resources, "limitConfiga.xml");
    logger.warn("File {} exists? {}", fileAuth, fileAuth.isFile());
    logger.warn("File {} exists? {}", fileLimit, fileLimit.isFile());
    assertTrue(file.isFile());
    assertTrue(fileAuth.isFile());
    assertTrue(fileLimit.isFile());

    final String[] args = {
        file.getAbsolutePath(), "-initdb", "-dir",
        r66Resources.getAbsolutePath(), "-auth", fileAuth.getAbsolutePath(),
        "-limit", fileLimit.getAbsolutePath()
    };
    executeJvm(projectHome, ServerInitDatabase.class, args);
  }

  private static void executeJvm(File homeDir, Class<?> zclass, String[] args) {
    try {
      /** initialize an java task **/
      final Java javaTask = new Java();
      javaTask.setNewenvironment(false);
      javaTask.setTaskName(zclass.getSimpleName());
      javaTask.setProject(project);
      javaTask.setFork(true);
      javaTask.setFailonerror(true);
      javaTask.setClassname(zclass.getName());

      // add some vm args
      final Argument jvmArgs = javaTask.createJvmarg();
      jvmArgs.setLine("-Xms512m -Xmx1024m");

      // added some args for to class to launch
      final Argument taskArgs = javaTask.createArg();
      final StringBuilder builder = new StringBuilder();
      for (final String string : args) {
        builder.append(' ').append(string);
      }
      taskArgs.setLine(builder.toString());

      /** set the class path */
      final String classpath = System.getProperty("java.class.path");
      final Path classPath = javaTask.createClasspath();
      classPath.setPath(classpath);
      javaTask.setClasspath(classPath);

      javaTask.init();
      final int ret = javaTask.executeJava();
      System.err
          .println(zclass.getName() + " " + args[0] + " return code: " + ret);
    } catch (final BuildException e) {
      e.printStackTrace();
    }
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

  private static void setUpBeforeClassServer(boolean start) throws Exception {
    final String serverInit = "config-serverInitA.xml";
    final String serverConfig = "config-serverA-minimal.xml";
    final File file = new File(r66Resources, serverInit);
    if (file.exists()) {
      System.err.println("Find serverInit file: " + file.getAbsolutePath());
      final File fileAuth = new File(r66Resources, "OpenR66-authent-A.xml");
      final File fileLimit = new File(r66Resources, "limitConfiga.xml");
      final String[] args = {
          file.getAbsolutePath(), "-initdb", "-dir",
          r66Resources.getAbsolutePath(), "-auth", fileAuth.getAbsolutePath(),
          "-limit", fileLimit.getAbsolutePath()
      };
      ServerInitDatabase.main(args);
      logger.warn("Init Done");
      if (start) {
        final File file2 = new File(r66Resources, serverConfig);
        if (file2.exists()) {
          System.err.println("Find server file: " + file2.getAbsolutePath());
          R66Server.main(new String[] { file2.getAbsolutePath() });
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

  private static void tearDownAfterClass() throws Exception {
    Thread.sleep(1000);
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = new InetSocketAddress("127.0.0.1", 6667);
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }
    final byte scode = -1;

    // Shutdown server
    logger.warn("Shutdown Server");
    Configuration.configuration.setTIMEOUTCON(100);
    final R66Future future = new R66Future(true);
    final ShutdownOrBlockJsonPacket nodeShutdown =
        new ShutdownOrBlockJsonPacket();
    nodeShutdown.setRestartOrBlock(false);
    nodeShutdown.setShutdownOrBlock(true);
    nodeShutdown.setKey(
        FilesystemBasedDigest.passwdCrypt("c5f4876737cf351a".getBytes()));
    final AbstractLocalPacket valid = new JsonCommandPacket(nodeShutdown,
                                                            LocalPacketFactory.BLOCKREQUESTPACKET);
    sendInformation(valid, socketServerAddress, future, scode, false,
                    R66FiniteDualStates.SHUTDOWN, true);
    Thread.sleep(1000);

    tearDownAfterClassClient();
    tearDownAfterClassServer();
    Thread.sleep(1000);
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

  private static void sendInformation(AbstractLocalPacket informationPacket,
                                      final SocketAddress socketServerAddress,
                                      R66Future future, byte scode,
                                      boolean check, R66FiniteDualStates state,
                                      boolean isSsl)
      throws OpenR66ProtocolPacketException {
    logger.warn("Start connection for Extra commands {}",
                informationPacket.getClass().getSimpleName());
    final LocalChannelReference localChannelReference = networkTransaction
        .createConnectionWithRetry(socketServerAddress, isSsl, future);
    if (localChannelReference == null) {
      if (state != R66FiniteDualStates.SHUTDOWN) {
        assertTrue("Connection not OK", false);
      } else {
        return;
      }
    }
    localChannelReference.sessionNewState(state);
    logger.warn("Send {}", informationPacket);
    ChannelUtils
        .writeAbstractLocalPacket(localChannelReference, informationPacket,
                                  false);
    if (informationPacket instanceof KeepAlivePacket ||
        informationPacket instanceof NoOpPacket) {
      // do no await
      localChannelReference.close();
      return;
    } else {
      localChannelReference.getFutureRequest().awaitOrInterruptible();
      future.awaitOrInterruptible();
      if (!(localChannelReference.getFutureRequest().isDone() && future.isDone())) {
        fail("Issue while sending information");
      }
    }
    final R66Result r66result = future.getResult();
    if (state == R66FiniteDualStates.VALIDOTHER) {
      logger.warn("feedback: {}", r66result);
    } else {
      final ValidPacket info = (ValidPacket) r66result.getOther();
      if (info != null && scode != -1) {
        logger.warn("nb: {}", Integer.parseInt(info.getSmiddle()));
        final String[] files = info.getSheader().split("\n");
        int i = 0;
        for (final String file : files) {
          i++;
          logger.warn("file: {} {}", i, file);
        }
      } else {
        if (info != null && info.getSheader() != null) {
          try {
            final DbTaskRunner runner =
                DbTaskRunner.fromStringXml(info.getSheader(), false);
            logger.warn("{}", runner.asXML());
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

  private static void tearDownAfterClassServer() throws Exception {
    ChannelUtils.exit();
  }

  private static void setUpBeforeClassClient(String clientConfig)
      throws Exception {
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

  private static void startUpProxy() {
    Configuration.configuration =
        new org.waarp.openr66.proxy.configuration.Configuration();
    final File config = new File(resources, "config-proxy.xml");
    assertTrue("Configuration file for Proxy must exist", config.isFile());
    if (!org.waarp.openr66.proxy.configuration.FileBasedConfiguration
        .setConfigurationProxyFromXml(Configuration.configuration,
                                      config.getAbsolutePath())) {
      logger.error("Needs a correct configuration file as first argument");
      assertTrue("Needs a correct configuration file as first argument", false);
    }
    WaarpShutdownHook.removeShutdownHook();
    try {
      Configuration.configuration.serverStartup();
    } catch (final Throwable e) {
      logger.error("Startup of Proxy is in error", e);
      WaarpShutdownHook.terminate(false);
      assertTrue("Needs a correct configuration file as first argument", false);
    }
  }

  private static void shutdownProxy() {
    Configuration.configuration.serverStop();
  }

  @Before
  public void setUp() throws Exception {
    Configuration.configuration.setTIMEOUTCON(10000);
  }

  @After
  public void tearDown() throws Exception {
    Configuration.configuration.setTIMEOUTCON(100);
  }

  // @Test
  public void r66ProxyNoTlsNoTls() {
    // Using a client NoDB
    r66Send(false, false);
    r66Recv(false, false);
  }

  // @Test
  public void r66ProxyTlsTls() {
    // Using a client NoDB
    r66Send(true, true);
    r66Recv(true, true);
  }

  // @Test
  public void r66ProxyNoTlsTls() {
    // Using a client NoDB
    r66Send(false, true);
    r66Recv(false, true);
  }

  // @Test
  public void r66ProxyTlsNoTls() {
    // Using a client NoDB
    r66Send(true, false);
    r66Recv(true, false);
  }

  public void r66Send(boolean fromTls, boolean toTls) {
    logger.warn("r66send {} {}", fromTls, toTls);
  }

  public void r66Recv(boolean fromTls, boolean toTls) {
    logger.warn("r66recv {} {}", fromTls, toTls);
  }
}