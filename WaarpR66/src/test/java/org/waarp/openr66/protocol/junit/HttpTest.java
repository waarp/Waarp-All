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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.json.ShutdownOrBlockJsonPacket;
import org.waarp.openr66.protocol.utils.R66Future;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;

import static org.junit.Assert.*;

/**
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HttpTest extends TestAbstract {
  private static final ArrayList<DbTaskRunner> dbTaskRunners =
      new ArrayList<DbTaskRunner>();
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML =
      "Linux/config/config-serverInitA.xml";
  private static final String CONFIG_SERVER_A_MINIMAL_RESPONSIVE_XML =
      "config-serverA-minimal-Responsive.xml";
  private static final String CONFIG_CLIENT_A_XML = "config-clientA.xml";

  /**
   * @throws Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    final ClassLoader classLoader = HttpTest.class.getClassLoader();
    final File file =
        new File(classLoader.getResource("logback-test.xml").getFile());
    if (file.exists()) {
      driverType = DriverType.PHANTOMJS;
      initiateWebDriver(file.getParentFile());
    }
    setUpBeforeClassMinimal(LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML);
    setUpDbBeforeClass();
    // setUpBeforeClassServer("Linux/config/config-serverInitB.xml", "config-serverB.xml", false);
    setUpBeforeClassServer(LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML,
                           CONFIG_SERVER_A_MINIMAL_RESPONSIVE_XML, true);
    setUpBeforeClassClient(CONFIG_CLIENT_A_XML);
  }

  /**
   * @throws Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    Thread.sleep(300);
    finalizeDriver();
    for (final DbTaskRunner dbTaskRunner : dbTaskRunners) {
      try {
        dbTaskRunner.delete();
      } catch (final WaarpDatabaseException e) {
        logger.warn("Cannot apply nolog to " + dbTaskRunner, e);
      }
    }
    final DbHostAuth host = new DbHostAuth("hostas");
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = new InetSocketAddress(host.getAddress(), 9667);
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }
    final byte scode = -1;

    // Shutdown server
    logger.warn("Shutdown Server");
    Configuration.configuration.setTimeoutCon(100);
    final R66Future future = new R66Future(true);
    final ShutdownOrBlockJsonPacket node8 = new ShutdownOrBlockJsonPacket();
    node8.setRestartOrBlock(false);
    node8.setShutdownOrBlock(true);
    node8.setKey(FilesystemBasedDigest.passwdCrypt(
        Configuration.configuration.getServerAdminKey()));
    final AbstractLocalPacket valid =
        new JsonCommandPacket(node8, LocalPacketFactory.BLOCKREQUESTPACKET);
    sendInformation(valid, socketServerAddress, future, scode, false,
                    R66FiniteDualStates.SHUTDOWN, true);
    Thread.sleep(200);

    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    tearDownAfterClassServer();
  }

  @Test
  public void test98_HttpResponsive() throws InterruptedException {
    try {
      // Test name: TestResponsiveMonitor
      // Step # | name | target | value | comment
      // 1 | open | / |  |
      driver.get("http://127.0.0.1:8866/");
      // 2 | click | linkText=Active Transfers |  |
      driver.get("http://127.0.0.1:8866/active");
      // 4 | click | linkText=In Error Transfers |  |
      driver.get("http://127.0.0.1:8866/error");
      // 5 | open | / |  |
      driver.get("http://127.0.0.1:8866/");
      // 6 | click | linkText=Finished Transfers |  |
      driver.get("http://127.0.0.1:8866/done");
      // 7 | open | / |  |
      driver.get("http://127.0.0.1:8866/");
      // 8 | click | linkText=All Transfers |  |
      driver.get("http://127.0.0.1:8866/all");
      // 9 | open | / |  |
      driver.get("http://127.0.0.1:8866/");
      // 10 | click | css=li:nth-child(5) > a |  |
      driver.get("http://127.0.0.1:8866/statusxml");
      // 11 | open | / |  |
      driver.get("http://127.0.0.1:8866/");
      // 12 | click | css=li:nth-child(6) > a |  |
      driver.get("http://127.0.0.1:8866/statusjson");
      // 13 | open | / |  |
      driver.get("http://127.0.0.1:8866/");
      // 18 | click | linkText=All Spooled daemons |  |
      driver.get("http://127.0.0.1:8866/spooled");
      // 19 | open | / |  |
      driver.get("http://127.0.0.1:8866/");
      // 20 | click | css=li:nth-child(10) > a |  |
      driver.get("http://127.0.0.1:8866/spooleddetail");
    } catch (NoSuchElementException e) {
      e.printStackTrace();
      reloadDriver();
      fail(e.getMessage());
    } catch (StaleElementReferenceException e) {
      e.printStackTrace();
      reloadDriver();
      fail(e.getMessage());
    } finally {
    }
  }

  @Test
  public void test98_HttpsResponsive() throws InterruptedException {
    try {
      // Test name: TestResponsiveAdmin
      // Step # | name | target | value | comment
      // 1 | open | https://127.0.0.1:8867/ |  |
      driver.get("https://127.0.0.1:8867/");
      // 2 | type | id=passwd | pwdhttp |
      driver.findElement(By.id("passwd")).sendKeys("pwdhttp");
      // 3 | type | id=name | monadmin |
      driver.findElement(By.id("name")).sendKeys("monadmin");
      // 4 | click | id=submit |  |
      driver.findElement(By.id("submit")).click();
      // 5 | open | https://127.0.0.1:8867/ |  |
      driver.get("https://127.0.0.1:8867/");
      // 6 | click | linkText=HOSTS |  |
      driver.get("https://127.0.0.1:8867/Hosts.html");
      // 7 | open | https://127.0.0.1:8867/ |  |
      driver.get("https://127.0.0.1:8867/");
      // 8 | click | linkText=RULES |  |
      driver.get("https://127.0.0.1:8867/Rules.html");
      // 9 | open | https://127.0.0.1:8867/ |  |
      driver.get("https://127.0.0.1:8867/");
      // 10 | click | linkText=SYSTEM |  |
      driver.get("https://127.0.0.1:8867/System.html");
      // 11 | open | https://127.0.0.1:8867/ |  |
      driver.get("https://127.0.0.1:8867/");
      // 24 | click | linkText=START |  |
      driver.get("https://127.0.0.1:8867/index.html");
      // 29 | click | linkText=LOGOUT |  |
      driver.get("https://127.0.0.1:8867/Logout.html");
    } catch (NoSuchElementException e) {
      e.printStackTrace();
      reloadDriver();
      fail(e.getMessage());
    } catch (StaleElementReferenceException e) {
      e.printStackTrace();
      reloadDriver();
      fail(e.getMessage());
    } finally {
    }
  }
}
