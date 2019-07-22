/**
 * This file is part of Waarp Project (named also Waarp or GG).
 * <p>
 * Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 * tags. See the COPYRIGHT.txt in the distribution for a full listing of
 * individual contributors.
 * <p>
 * All Waarp Project is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 * <p>
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * Waarp . If not, see <http://www.gnu.org/licenses/>.
 */

/**
 *
 */
package org.waarp.openr66.protocol.junit;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Commandline.Argument;
import org.apache.tools.ant.types.Path;
import org.junit.After;
import org.junit.Before;
import org.waarp.common.database.DbPreparedStatement;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoDataException;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.dao.DAOFactory;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.exception.DAOConnectionException;
import org.waarp.openr66.dao.exception.DAONoDataException;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.database.data.DbTaskRunner.Columns;
import org.waarp.openr66.database.data.DbTaskRunner.TASKSTEP;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.server.R66Server;
import org.waarp.openr66.server.ServerInitDatabase;

import java.io.File;
import java.io.FileFilter;

/**
 * @author frederic
 *
 */
public abstract class TestAbstract extends TestAbstractMinimal {

  protected static NetworkTransaction networkTransaction = null;
  private static Project project;
  private static File homeDir;

  public static void setUpDbBeforeClass() throws Exception {
    deleteBase();
    String serverInit = "Linux/config/config-serverInitB.xml";
    setUpBeforeClassMinimal(serverInit);

    ClassLoader classLoader = TestAbstract.class.getClassLoader();
    DetectionUtils.setJunit(true);
    File file = new File(classLoader.getResource(serverInit).getFile());
    String newfile = file.getAbsolutePath().replace("target/test-classes",
                                                    "src/test/resources");
    file = new File(newfile);
    dir = file.getParentFile();
    logger.warn("File {} exists? {}", file, file.isFile());
    homeDir = dir.getParentFile().getParentFile().getParentFile()
                 .getParentFile().getParentFile();
    logger.warn("Dir {} exists? {}", homeDir, homeDir.isDirectory());

    // global ant project settings
    project = new Project();
    project.setBaseDir(homeDir);
    project.init();
    WaarpLoggerListener listener = new WaarpLoggerListener();
    project.addBuildListener(listener);
    listener.setMessageOutputLevel(Project.MSG_WARN);
    project.fireBuildStarted();

    serverInit = "config-serverInitB.xml";
    initiateDb(serverInit);
    serverInit = "config-serverInitA.xml";
    initiateDb(serverInit);

    project.log("finished");
    project.fireBuildFinished(null);
  }


  public static void initiateDb(String serverInit) {
    DetectionUtils.setJunit(true);
    File file = new File(dir, serverInit);
    logger.warn("File {} exists? {}", file, file.isFile());
    File fileAuth = new File(dir, "OpenR66-authent-A.xml");
    File fileLimit = new File(dir, "limitConfiga.xml");
    logger.warn("File {} exists? {}", fileAuth, fileAuth.isFile());
    logger.warn("File {} exists? {}", fileLimit, fileLimit.isFile());

    String[] args = {
        file.getAbsolutePath(), "-initdb", "-dir",
        dir.getAbsolutePath(), "-auth",
        new File(dir, "OpenR66-authent-A.xml").getAbsolutePath(),
        "-limit",
        new File(dir, "limitConfiga.xml").getAbsolutePath()
    };
    executeJvm(homeDir, ServerInitDatabase.class, args);
  }

  public static void executeJvm(File homeDir, Class<?> zclass,
                                String[] args) {
    try {
      /** initialize an java task **/
      Java javaTask = new Java();
      javaTask.setNewenvironment(false);
      javaTask.setTaskName(zclass.getSimpleName());
      javaTask.setProject(project);
      javaTask.setFork(true);
      javaTask.setFailonerror(true);
      javaTask.setClassname(zclass.getName());

      // add some vm args
      Argument jvmArgs = javaTask.createJvmarg();
      jvmArgs.setLine("-Xms512m -Xmx1024m");

      // added some args for to class to launch
      Argument taskArgs = javaTask.createArg();
      StringBuilder builder = new StringBuilder();
      for (String string : args) {
        builder.append(' ').append(string);
      }
      taskArgs.setLine(builder.toString());

      /** set the class path */
      String classpath = System.getProperty("java.class.path");
      Path classPath = javaTask.createClasspath();
      classPath.setPath(classpath);
      javaTask.setClasspath(classPath);

      javaTask.init();
      int ret = javaTask.executeJava();
      System.err
          .println(zclass.getName() + " " + args[0] + " return code: " + ret);
    } catch (BuildException e) {
      e.printStackTrace();
    }
  }

  /**
   * @throws java.lang.Exception
   */
  public static void setUpBeforeClassServer(String serverInit,
                                            String serverConfig,
                                            boolean start) throws Exception {
    ClassLoader classLoader = TestAbstract.class.getClassLoader();
    File file = new File(classLoader.getResource(serverInit).getFile());
    if (file.exists()) {
      dir = file.getParentFile();
      System.err
          .println("Find serverInit file: " + file.getAbsolutePath());
      ServerInitDatabase.main(new String[] {
          file.getAbsolutePath(), "-initdb", "-dir",
          dir.getAbsolutePath(), "-auth",
          new File(dir, "OpenR66-authent-A.xml").getAbsolutePath(),
          "-limit",
          new File(dir, "limitConfiga.xml").getAbsolutePath()
      });
      logger.warn("Init Done");
      if (start) {
        File file2 = new File(dir, serverConfig);
        if (file2.exists()) {
          System.err.println(
              "Find server file: " + file2.getAbsolutePath());
          R66Server.main(new String[] { file2.getAbsolutePath() });
          logger.warn("Start Done");
        } else {
          System.err.println("Cannot find server file: " +
                             file2.getAbsolutePath());
        }
      }
    } else {
      System.err.println(
          "Cannot find serverInit file: " + file.getAbsolutePath());
    }
  }

  public static void tearDownAfterClassServer() throws Exception {
    ChannelUtils.exit();
    deleteBase();
  }

  public static void deleteBase() {
    File tmp = new File("/tmp");
    File[] files = tmp.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return file.getName().startsWith("openr66");
      }
    });
    for (File file : files) {
      file.delete();
    }
  }

  public static void setUpBeforeClassClient(String clientConfig)
      throws Exception {
    File clientConfigFile = new File(dir, clientConfig);
    if (clientConfigFile.isFile()) {
      System.err
          .println(
              "Find serverInit file: " + clientConfigFile.getAbsolutePath());
      if (!FileBasedConfiguration
          .setClientConfigurationFromXml(Configuration.configuration,
                                         new File(dir, clientConfig)
                                             .getAbsolutePath())) {
        logger.error(
            "Needs a correct configuration file as first argument");
        return;
      }
    } else {
      logger.error(
          "Needs a correct configuration file as first argument");
      return;
    }
    Configuration.configuration.pipelineInit();
    networkTransaction = new NetworkTransaction();
    DbTaskRunner.clearCache();
    TransferDAO transferAccess = null;
    try {
      transferAccess = DAOFactory.getInstance().getTransferDAO();
      transferAccess.deleteAll();
    } catch (DAOConnectionException e) {
      throw new WaarpDatabaseException(e);
    } finally {
      if (transferAccess != null) {
        transferAccess.close();
      }
    }
  }

  public static void tearDownAfterClassClient() throws Exception {
    networkTransaction.closeAll();
  }

  @Before
  public void setUp() throws Exception {
    Configuration.configuration.setTIMEOUTCON(10000);
    Configuration.configuration
        .changeNetworkLimit(1000000000, 1000000000, 1000000000, 1000000000,
                            1000);
  }

  @After
  public void tearDown() throws Exception {
    Configuration.configuration.setTIMEOUTCON(100);
  }
}
