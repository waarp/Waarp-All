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

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runners.MethodSorters;
import org.waarp.common.database.exception.WaarpDatabaseSqlException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.Processes;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.server.R66Server;

import java.io.File;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConfigurationTest extends TestAbstract {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(ConfigurationTest.class);
  private static final String CONFIG_BASE = "Linux/config/";
  private static final String CONFIG_SERVER_A_WRONG_XML =
      "config-serverA-WrongPath.xml";
  private static final String CONFIG_CLIENT_A_XML = "config-clientA.xml";

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ResourceLeakDetector.setLevel(Level.PARANOID);
    WaarpSystemUtil.setJunit(true);
    setUpDbBeforeClass();
    new File("/tmp/in").mkdir();
    new File("/tmp/out").mkdir();
    new File("/tmp/conf").mkdir();
    new File("/tmp/work").mkdir();
    new File("/tmp/arch").mkdir();
  }

  /**
   * @throws Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    WaarpSystemUtil.stopLogger(true);
    new File("/tmp/in").delete();
    new File("/tmp/out").delete();
    new File("/tmp/conf").delete();
    new File("/tmp/work").delete();
    new File("/tmp/arch").delete();
    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    tearDownAfterClassServer();
  }

  @Test
  public void testBadFilePath() throws InterruptedException {
    final ClassLoader classLoader = TestAbstract.class.getClassLoader();
    final File file = new File(
        classLoader.getResource(CONFIG_BASE + CONFIG_SERVER_A_WRONG_XML)
                   .getFile());
    if (file.exists()) {
      dir = file.getParentFile();
      R66Server.main(new String[] { file.getAbsolutePath() });
      Thread.sleep(1000);
      int pid;
      if ((pid = Processes.getPidOfRunnerJavaCommandLinux(
          R66Server.class.getName())) != -1) {
        logger.error(
            "####################### R66 SERVER STILL RUNNING But should not " +
            "{} ###################", pid);
        System.err.println(
            "####################### R66 SERVER STILL RUNNING But should not " +
            pid + " ###################");
        Processes.kill(pid, false);
        assertTrue("R66 Server is started but should not", false);
      }
    }
  }

  @Test
  public void testBadRUlePath() throws Exception {
    final ClassLoader classLoader = TestAbstract.class.getClassLoader();
    final File file = new File(
        classLoader.getResource(CONFIG_BASE + CONFIG_SERVER_A_WRONG_XML)
                   .getFile());
    if (file.exists()) {
      dir = file.getParentFile();
    } else {
      fail("Cannot found base directory");
    }
    setUpBeforeClassClient(CONFIG_CLIENT_A_XML);
    new File("/tmp/in").mkdir();
    new File("/tmp/out").mkdir();
    new File("/tmp/conf").mkdir();
    new File("/tmp/work").mkdir();
    new File("/tmp/arch").mkdir();
    try {
      final DbRule dbRuleWrong =
          new DbRule("wrongRule", "", 1, "/tmp/in", "/tmp/out", "/tmp/arch",
                     "/tmp/work", "", "", "", "", "", "");
      fail("Should raized an exception");
    } catch (final WaarpDatabaseSqlException e) {
      // OK
      logger.warn("Exception OK: {}", e.getMessage());
    }
  }
}
