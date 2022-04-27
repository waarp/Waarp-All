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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.UnhandledAlertException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.NullPrintStream;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.TestWebAbstract;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.junit.NetworkClientTest;
import org.waarp.openr66.protocol.junit.TestAbstract;

import java.io.File;
import java.io.PrintStream;

import static org.junit.Assert.*;
import static org.waarp.openr66.protocol.it.ScenarioBase.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AdminIT extends TestAbstract {
  /**
   * If defined using -DIT_LONG_TEST=true then will execute long term tests
   */
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AdminIT.class);
  private static final String CONFIG_SERVER_A_MINIMAL_XML =
      "config-serverA-minimal-restauthent.xml";
  private static final String CONFIG_SERVER_A_MINIMAL_RESPONSIVE_XXML =
      "config-serverA-minimal-Responsive.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML =
      "Linux/config/config-serverInitA.xml";

  private static final int WAIT = 300;
  public static long MAX_USED_MEMORY = 134217728;

  private static PrintStream err;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    err = System.err;
    System.setErr(new NullPrintStream());
    if (!SystemPropertyUtil.get(IT_LONG_TEST, false)) {
      ResourceLeakDetector.setLevel(Level.PARANOID);
    } else {
      ResourceLeakDetector.setLevel(Level.SIMPLE);
    }
    final ClassLoader classLoader = NetworkClientTest.class.getClassLoader();
    final File file =
        new File(classLoader.getResource("logback-test.xml").getFile());
    if (file.exists()) {
      TestWebAbstract.driverType = TestWebAbstract.DriverType.FIREFOX;
      TestWebAbstract.initiateWebDriver(file.getParentFile());
    }
    setUpDbBeforeClass();
    setUpBeforeClassServer(LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML,
                           CONFIG_SERVER_A_MINIMAL_XML, true);
  }

  /**
   * @throws Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    WaarpSystemUtil.stopLogger(true);
    Thread.sleep(100);
    TestWebAbstract.finalizeDriver();
    // Shutdown server
    logger.warn("Shutdown Server");
    Configuration.configuration.setTimeoutCon(100);
    System.setErr(err);
    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    tearDownAfterClassServer();
  }

  @After
  public void restartDriver() throws InterruptedException {
    TestWebAbstract.reloadDriver();
    Thread.sleep(100);
  }

  private static void checkBigIt() {
    Runtime runtime = Runtime.getRuntime();
    boolean isMemory128 = runtime.totalMemory() <= MAX_USED_MEMORY;
    if (!isMemory128) {
      logger.warn("Memory must be < 128M: {}", runtime.totalMemory());
    }
  }

  @Test
  public void test10_RestR66V2CheckMemoryUsage() throws Exception {
    checkBigIt();
    // Must be executed AFTER testCreateUserAdmin since it will use testb2
    // First with no authent: should fail
    try {
      final String baseUri = "http://localhost:8088/";
      // 2 | type | V2 [  |
      String v2BaseUri = baseUri + "v2/";
      TestWebAbstract.driver.get(v2BaseUri + "transfers");
      TestWebAbstract.driver.getPageSource();
    } catch (NoSuchElementException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } catch (StaleElementReferenceException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } catch (UnhandledAlertException e) {
      // Correct
    } finally {
      // nothing
    }

    try {
      final String baseUri = "http://testb2:testb2@localhost:8088/";
      // 2 | type | V2 [  |
      String v2BaseUri = baseUri + "v2/";
      Runtime runtime = Runtime.getRuntime();
      long usedMemory = runtime.totalMemory() - runtime.freeMemory();
      logger.warn("Used Memory {}", usedMemory / 1048576.0);
      TestWebAbstract.driver.get(v2BaseUri + "transfers");
      SysErrLogger.FAKE_LOGGER.sysout(TestWebAbstract.driver.getCurrentUrl());
      assertTrue(TestWebAbstract.driver.getPageSource().contains("results"));
      int max = 60 * 18;// Roughly 1min
      if (SystemPropertyUtil.get(IT_LONG_TEST, false)) {
        max *= 4; // Roughly 4 min
      }
      for (int i = 0; i < max; i++) {
        TestWebAbstract.driver.get(v2BaseUri + "transfers");
        assertTrue(TestWebAbstract.driver.getPageSource().contains("results"));
      }
      TestWebAbstract.driver.get(v2BaseUri + "server/status");
      SysErrLogger.FAKE_LOGGER.sysout(TestWebAbstract.driver.getCurrentUrl());
      assertTrue(TestWebAbstract.driver.getPageSource().contains("serverName"));
      usedMemory = runtime.totalMemory() - runtime.freeMemory();
      logger.warn("Used Memory {}", usedMemory / 1048576.0);
    } catch (NoSuchElementException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } catch (StaleElementReferenceException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      // nothing
    }
  }

  @Test
  public void test11_BasicMemoryUsage() throws InterruptedException {
    checkBigIt();
    try {
      // Test name: TestResponsiveAdmin
      Runtime runtime = Runtime.getRuntime();
      long usedMemory = runtime.totalMemory() - runtime.freeMemory();
      logger.warn("Used Memory {}", usedMemory / 1048576.0);
      // Step # | name | target | value | comment
      TestWebAbstract.driver.get("https://127.0.0.1:8067/");
      // 4 | type | name=passwd | pwdhttp
      TestWebAbstract.driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 5 | click | name=name |
      TestWebAbstract.driver.findElement(By.name("name")).click();
      // 6 | type | name=name | monadmin
      TestWebAbstract.driver.findElement(By.name("name")).sendKeys("monadmin");
      // 10 | click | name=Logon |
      TestWebAbstract.driver.findElement(By.name("Logon")).click();
      assertTrue(TestWebAbstract.driver.getCurrentUrl().equals(
          "https://127.0.0.1:8067/index.html"));
      // 5 | open | https://127.0.0.1:8867/ |  |
      TestWebAbstract.driver.get("https://127.0.0.1:8067/");
      assertTrue(TestWebAbstract.driver.getCurrentUrl()
                                       .equals("https://127.0.0.1:8067/"));
      // Using content
      // 5 | click | linkText=TRANSFERS |  |
      TestWebAbstract.driver.findElement(By.linkText("TRANSFERS")).click();
      // 6 | click | linkText=LISTING |  |
      TestWebAbstract.driver.findElement(By.linkText("LISTING")).click();
      assertTrue(TestWebAbstract.driver.getCurrentUrl().equals(
          "https://127.0.0.1:8067/Listing.html"));
      int max = 60 * 5;// Roughly 1min
      if (SystemPropertyUtil.get(IT_LONG_TEST, false)) {
        max *= 4; // Roughly 4 min
      }
      for (int i = 0; i < max; i++) {
        // 5 | click | linkText=TRANSFERS |  |
        TestWebAbstract.driver.findElement(By.linkText("TRANSFERS")).click();
        // 6 | click | linkText=LISTING |  |
        TestWebAbstract.driver.findElement(By.linkText("LISTING")).click();
        assertTrue(TestWebAbstract.driver.getCurrentUrl().equals(
            "https://127.0.0.1:8067/Listing.html"));
      }
      // 19 | click | linkText=START |  |
      TestWebAbstract.driver.findElement(By.linkText("START")).click();
      assertTrue(TestWebAbstract.driver.getCurrentUrl().equals(
          "https://127.0.0.1:8067/index.html"));
      usedMemory = runtime.totalMemory() - runtime.freeMemory();
      logger.warn("Used Memory {}", usedMemory / 1048576.0);
    } catch (NoSuchElementException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } catch (StaleElementReferenceException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      // 29 | click | linkText=LOGOUT |  |
      TestWebAbstract.driver.get("https://127.0.0.1:8067/Logout.html");
      assertTrue(TestWebAbstract.driver.getCurrentUrl().equals(
          "https://127.0.0.1:8067/Logout.html"));
    }
  }

  @Test
  public void test01_CreateUser() throws InterruptedException {
    // Test name: TestCreateUser2
    try {
      // Step # | name | target | value
      // 1 | open | / |
      TestWebAbstract.driver.get("https://127.0.0.1:8067/");
      // 4 | type | name=passwd | pwdhttp
      TestWebAbstract.driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 5 | type | name=name | monadmin
      TestWebAbstract.driver.findElement(By.name("name")).sendKeys("monadmin");
      // 6 | click | name=Logon |
      TestWebAbstract.driver.findElement(By.name("Logon")).click();
      // 7 | click | linkText=HOSTS |
      TestWebAbstract.driver.findElement(By.linkText("HOSTS")).click();
      // 9 | type | name=host | testb
      TestWebAbstract.driver.findElement(By.name("host")).sendKeys("testb");
      // 10 | type | name=address | 127.0.0.1
      TestWebAbstract.driver.findElement(By.name("address"))
                            .sendKeys("127.0.0.1");
      // 12 | type | name=port | -1
      TestWebAbstract.driver.findElement(By.name("port")).sendKeys("-1");
      // 14 | type | name=hostkey | testb
      TestWebAbstract.driver.findElement(By.name("hostkey")).sendKeys("testb");
      // 15 | click | name=isclient |
      TestWebAbstract.driver.findElement(By.name("isclient")).click();
      // 16 | click | name=isactive |
      TestWebAbstract.driver.findElement(By.name("isactive")).click();
      // 17 | click | name=ACTION |
      TestWebAbstract.driver.findElement(By.name("ACTION")).click();
      // 18 | click | linkText=LOGOUT |
      TestWebAbstract.driver.findElement(By.linkText("LOGOUT")).click();
      // 20 | type | name=name | testb
      TestWebAbstract.driver.findElement(By.name("name")).sendKeys("testb");
      // 21 | type | name=passwd | testb
      TestWebAbstract.driver.findElement(By.name("passwd")).sendKeys("testb");
      // 22 | click | name=Logon |
      TestWebAbstract.driver.findElement(By.name("Logon")).click();
      // 23 | click | linkText=HOSTS |
      TestWebAbstract.driver.findElement(By.linkText("HOSTS")).click();
      String page = TestWebAbstract.driver.getPageSource();
      assertTrue(page.contains("Your profile does not allow this function."));
      // 24 | click | linkText=RULES |
      TestWebAbstract.driver.findElement(By.linkText("RULES")).click();
      page = TestWebAbstract.driver.getPageSource();
      assertTrue(page.contains("Your profile does not allow this function."));
      // 26 | click | linkText=LOGOUT |
      TestWebAbstract.driver.findElement(By.linkText("LOGOUT")).click();
      // 28 | type | name=passwd | pwdhttp
      TestWebAbstract.driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 29 | type | name=name | monadmin
      TestWebAbstract.driver.findElement(By.name("name")).sendKeys("monadmin");
      // 30 | click | name=Logon |
      TestWebAbstract.driver.findElement(By.name("Logon")).click();
      // 31 | click | linkText=SYSTEM |
      TestWebAbstract.driver.findElement(By.linkText("SYSTEM")).click();
      // 34 | click | name=ROLES |
      TestWebAbstract.driver.findElement(By.name("ROLES")).clear();
      // 35 | type | name=ROLES | <roles><role><roleid>hostas</roleid><roleset>READONLY TRANSFER RULE HOST LIMIT SYSTEM LOGCONTROL PARTNER CONFIGADMIN FULLADMIN</roleset></role><role><roleid>tests</roleid><roleset>READONLY TRANSFER RULE HOST LIMIT SYSTEM LOGCONTROL PARTNER CONFIGADMIN FULLADMIN</roleset></role><role><roleid>hosta</roleid><roleset>READONLY TRANSFER RULE HOST SYSTEM PARTNER CONFIGADMIN</roleset></role><role><roleid>test</roleid><roleset>READONLY TRANSFER LOGCONTROL PARTNER</roleset></role><role><roleid>hostb</roleid><roleset>READONLY TRANSFER RULE HOST SYSTEM PARTNER CONFIGADMIN</roleset></role><role><roleid>testb</roleid><roleset>READONLY TRANSFER RULE HOST</roleset></role></roles>
      TestWebAbstract.driver.findElement(By.name("ROLES")).sendKeys(
          "<roles><role><roleid>hostas</roleid><roleset>READONLY TRANSFER RULE HOST LIMIT SYSTEM LOGCONTROL PARTNER CONFIGADMIN FULLADMIN</roleset></role><role><roleid>tests</roleid><roleset>READONLY TRANSFER RULE HOST LIMIT SYSTEM LOGCONTROL PARTNER CONFIGADMIN FULLADMIN</roleset></role><role><roleid>hosta</roleid><roleset>READONLY TRANSFER RULE HOST SYSTEM PARTNER CONFIGADMIN</roleset></role><role><roleid>test</roleid><roleset>READONLY TRANSFER LOGCONTROL PARTNER</roleset></role><role><roleid>hostb</roleid><roleset>READONLY TRANSFER RULE HOST SYSTEM PARTNER CONFIGADMIN</roleset></role><role><roleid>testb</roleid><roleset>READONLY TRANSFER RULE HOST</roleset></role></roles>");
      // 36 | click | css=form:nth-child(5) |
      TestWebAbstract.driver.findElement(By.cssSelector("form:nth-child(5)"))
                            .click();
      // 37 | click | css=p:nth-child(19) > input |
      TestWebAbstract.driver.findElement(
          By.cssSelector("p:nth-child(19) > input")).click();
      // 38 | click | linkText=LOGOUT |
      TestWebAbstract.driver.findElement(By.linkText("LOGOUT")).click();
      // 40 | type | name=passwd | testb
      TestWebAbstract.driver.findElement(By.name("passwd")).sendKeys("testb");
      // 41 | type | name=name | testb
      TestWebAbstract.driver.findElement(By.name("name")).sendKeys("testb");
      // 42 | click | name=Logon |
      TestWebAbstract.driver.findElement(By.name("Logon")).click();
      // 43 | click | linkText=HOSTS |
      TestWebAbstract.driver.findElement(By.linkText("HOSTS")).click();
      page = TestWebAbstract.driver.getPageSource();
      assertFalse(page.contains("Your profile does not allow this function."));
      // 44 | click | linkText=RULES |
      TestWebAbstract.driver.findElement(By.linkText("RULES")).click();
      page = TestWebAbstract.driver.getPageSource();
      assertFalse(page.contains("Your profile does not allow this function."));
    } finally {
      // 45 | click | linkText=LOGOUT |
      TestWebAbstract.driver.findElement(By.linkText("LOGOUT")).click();
    }
  }

  @Test
  public void test02_CreateUserAdmin() throws InterruptedException {
    // Test name: TestCreateUser2
    try {
      // Step # | name | target | value
      // 1 | open | / |
      TestWebAbstract.driver.get("https://127.0.0.1:8067/");
      // 4 | type | name=passwd | pwdhttp
      TestWebAbstract.driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 5 | type | name=name | monadmin
      TestWebAbstract.driver.findElement(By.name("name")).sendKeys("monadmin");
      // 6 | click | name=Logon |
      TestWebAbstract.driver.findElement(By.name("Logon")).click();
      // 7 | click | linkText=HOSTS |
      TestWebAbstract.driver.findElement(By.linkText("HOSTS")).click();
      // 9 | type | name=host | testb
      TestWebAbstract.driver.findElement(By.name("host")).sendKeys("testb2");
      // 10 | type | name=address | 127.0.0.1
      TestWebAbstract.driver.findElement(By.name("address"))
                            .sendKeys("127.0.0.1");
      // 12 | type | name=port | -1
      TestWebAbstract.driver.findElement(By.name("port")).sendKeys("-1");
      // 14 | type | name=hostkey | testb
      TestWebAbstract.driver.findElement(By.name("hostkey")).sendKeys("testb2");
      // 15 | click | name=isclient |
      TestWebAbstract.driver.findElement(By.name("isclient")).click();
      // 16 | click | name=isactive |
      TestWebAbstract.driver.findElement(By.name("isactive")).click();
      TestWebAbstract.driver.findElement(By.name("admin")).click();
      // 17 | click | name=ACTION |
      TestWebAbstract.driver.findElement(By.name("ACTION")).click();
      // 18 | click | linkText=LOGOUT |
      TestWebAbstract.driver.findElement(By.linkText("LOGOUT")).click();
      // 40 | type | name=passwd | testb
      TestWebAbstract.driver.findElement(By.name("passwd")).sendKeys("testb2");
      // 41 | type | name=name | testb
      TestWebAbstract.driver.findElement(By.name("name")).sendKeys("testb2");
      // 42 | click | name=Logon |
      TestWebAbstract.driver.findElement(By.name("Logon")).click();
      // 43 | click | linkText=HOSTS |
      TestWebAbstract.driver.findElement(By.linkText("HOSTS")).click();
      String page = TestWebAbstract.driver.getPageSource();
      assertFalse(page.contains("Your profile does not allow this function."));
      // 44 | click | linkText=RULES |
      TestWebAbstract.driver.findElement(By.linkText("RULES")).click();
      page = TestWebAbstract.driver.getPageSource();
      assertFalse(page.contains("Your profile does not allow this function."));
    } finally {
      // 45 | click | linkText=LOGOUT |
      TestWebAbstract.driver.findElement(By.linkText("LOGOUT")).click();
    }
  }
}
