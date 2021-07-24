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
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.NullPrintStream;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.Version;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.io.File;
import java.io.PrintStream;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AdminTest extends TestAbstract {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AdminTest.class);
  private static final String CONFIG_SERVER_A_MINIMAL_XML =
      "config-serverA-minimal-restauthent.xml";
  private static final String CONFIG_SERVER_A_MINIMAL_RESPONSIVE_XXML =
      "config-serverA-minimal-Responsive.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML =
      "Linux/config/config-serverInitA.xml";

  private static final int WAIT = 300;
  private static PrintStream err;

  private static boolean RUN_TEST = true;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ResourceLeakDetector.setLevel(Level.PARANOID);
    err = System.err;
    System.setErr(new NullPrintStream());
    final ClassLoader classLoader = NetworkClientTest.class.getClassLoader();
    final File file =
        new File(classLoader.getResource("logback-test.xml").getFile());
    if (file.exists()) {
      driverType = DriverType.PHANTOMJS;
      try {
        initiateWebDriver(file.getParentFile());
      } catch (NoSuchMethodError e) {
        RUN_TEST = false;
        return;
      }
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
    if (!RUN_TEST) {
      return;
    }
    Thread.sleep(100);
    finalizeDriver();
    System.setErr(err);
    // Shutdown server
    logger.warn("Shutdown Server");
    Configuration.configuration.setTimeoutCon(100);

    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    tearDownAfterClassServer();
  }

  @After
  public void restartDriver() throws InterruptedException {
    if (!RUN_TEST) {
      return;
    }
    reloadDriver();
    Thread.sleep(100);
  }

  @Test
  public void test05_HttpSimpleAdmin() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    try {
      // Test name: TestResponsiveMonitor
      // Step # | name | target | value | comment
      // 1 | open | / |  |
      driver.get("http://127.0.0.1:8066/");
      assertTrue(driver.getCurrentUrl().equals("http://127.0.0.1:8066/"));
      // 2 | click | linkText=Active Transfers |  |
      driver.get("http://127.0.0.1:8066/active");
      assertTrue(driver.getCurrentUrl().equals("http://127.0.0.1:8066/active"));
      // 4 | click | linkText=In Error Transfers |  |
      driver.get("http://127.0.0.1:8066/error");
      assertTrue(driver.getCurrentUrl().equals("http://127.0.0.1:8066/error"));
      // 5 | open | / |  |
      driver.get("http://127.0.0.1:8066/");
      assertTrue(driver.getCurrentUrl().equals("http://127.0.0.1:8066/"));
      // 6 | click | linkText=Finished Transfers |  |
      driver.get("http://127.0.0.1:8066/done");
      assertTrue(driver.getCurrentUrl().equals("http://127.0.0.1:8066/done"));
      // 7 | open | / |  |
      driver.get("http://127.0.0.1:8066/");
      assertTrue(driver.getCurrentUrl().equals("http://127.0.0.1:8066/"));
      // 8 | click | linkText=All Transfers |  |
      driver.get("http://127.0.0.1:8066/all");
      assertTrue(driver.getCurrentUrl().equals("http://127.0.0.1:8066/all"));
      // 9 | open | / |  |
      driver.get("http://127.0.0.1:8066/");
      assertTrue(driver.getCurrentUrl().equals("http://127.0.0.1:8066/"));
      // 10 | click | css=li:nth-child(5) > a |  |
      driver.get("http://127.0.0.1:8066/statusxml");
      assertTrue(
          driver.getCurrentUrl().equals("http://127.0.0.1:8066/statusxml"));
      driver.get("http://127.0.0.1:8066/statusxml?DETAIL=1");
      assertTrue(driver.getCurrentUrl()
                       .equals("http://127.0.0.1:8066/statusxml?DETAIL=1"));
      // 11 | open | / |  |
      driver.get("http://127.0.0.1:8066/");
      assertTrue(driver.getCurrentUrl().equals("http://127.0.0.1:8066/"));
      // 12 | click | css=li:nth-child(6) > a |  |
      driver.get("http://127.0.0.1:8066/statusjson");
      assertTrue(
          driver.getCurrentUrl().equals("http://127.0.0.1:8066/statusjson"));
      driver.get("http://127.0.0.1:8066/statusjson?DETAIL=1");
      assertTrue(driver.getCurrentUrl()
                       .equals("http://127.0.0.1:8066/statusjson?DETAIL=1"));
      // 13 | open | / |  |
      driver.get("http://127.0.0.1:8066/");
      assertTrue(driver.getCurrentUrl().equals("http://127.0.0.1:8066/"));
      // 18 | click | linkText=All Spooled daemons |  |
      driver.get("http://127.0.0.1:8066/spooled");
      assertTrue(
          driver.getCurrentUrl().equals("http://127.0.0.1:8066/spooled"));
      // 19 | open | / |  |
      driver.get("http://127.0.0.1:8066/");
      assertTrue(driver.getCurrentUrl().equals("http://127.0.0.1:8066/"));
      // 20 | click | css=li:nth-child(10) > a |  |
      driver.get("http://127.0.0.1:8066/spooleddetail");
      assertTrue(
          driver.getCurrentUrl().equals("http://127.0.0.1:8066/spooleddetail"));
    } catch (NoSuchElementException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } catch (StaleElementReferenceException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
    }
  }

  @Test
  public void test01_Basic() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    try {
      // Test name: TestResponsiveAdmin
      // Step # | name | target | value | comment
      driver.get("https://127.0.0.1:8067/");
      Thread.sleep(WAIT);
      // 4 | type | name=passwd | pwdhttp
      driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 5 | click | name=name |
      driver.findElement(By.name("name")).click();
      // 6 | type | name=name | monadmin
      driver.findElement(By.name("name")).sendKeys("monadmin");
      // 10 | click | name=Logon |
      driver.findElement(By.name("Logon")).click();
      Thread.sleep(WAIT);
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/index.html"));
      // 5 | open | https://127.0.0.1:8867/ |  |
      driver.get("https://127.0.0.1:8067/");
      assertTrue(driver.getCurrentUrl().equals("https://127.0.0.1:8067/"));
      // 6 | click | linkText=HOSTS |  |
      driver.get("https://127.0.0.1:8067/Hosts.html");
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/Hosts.html"));
      // 7 | open | https://127.0.0.1:8867/ |  |
      driver.get("https://127.0.0.1:8067/");
      assertTrue(driver.getCurrentUrl().equals("https://127.0.0.1:8067/"));
      // 8 | click | linkText=RULES |  |
      driver.get("https://127.0.0.1:8067/Rules.html");
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/Rules.html"));
      // 9 | open | https://127.0.0.1:8867/ |  |
      driver.get("https://127.0.0.1:8067/");
      assertTrue(driver.getCurrentUrl().equals("https://127.0.0.1:8067/"));
      // 10 | click | linkText=SYSTEM |  |
      driver.get("https://127.0.0.1:8067/System.html");
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/System.html"));
      // 11 | open | https://127.0.0.1:8867/ |  |
      driver.get("https://127.0.0.1:8067/");
      assertTrue(driver.getCurrentUrl().equals("https://127.0.0.1:8067/"));
      // 24 | click | linkText=START |  |
      driver.get("https://127.0.0.1:8067/index.html");
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/index.html"));
      driver.get("https://127.0.0.1:8067/CancelRestart.html");
      assertTrue(driver.getCurrentUrl()
                       .equals("https://127.0.0.1:8067/CancelRestart.html"));
      driver.get("https://127.0.0.1:8067/Export.html");
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/Export.html"));
      driver.get("https://127.0.0.1:8067/Listing.html");
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/Listing.html"));
      driver.get("https://127.0.0.1:8067/Spooled.html");
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/Spooled.html"));
      // Using content
      // 5 | click | linkText=TRANSFERS |  |
      driver.findElement(By.linkText("TRANSFERS")).click();
      // 6 | click | linkText=LISTING |  |
      driver.findElement(By.linkText("LISTING")).click();
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/Listing.html"));
      // 7 | click | name=ACTION |  |
      driver.findElement(By.name("ACTION")).click();
      // 8 | click | linkText=CANCEL-RESTART |  |
      driver.findElement(By.linkText("CANCEL-RESTART")).click();
      assertTrue(driver.getCurrentUrl()
                       .equals("https://127.0.0.1:8067/CancelRestart.html"));
      // 9 | click | name=ACTION |  |
      driver.findElement(By.name("ACTION")).click();
      // 10 | click | linkText=EXPORT |  |
      driver.findElement(By.linkText("EXPORT")).click();
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/Export.html"));
      // 11 | click | name=ACTION |  |
      driver.findElement(By.name("ACTION")).click();
      // 12 | click | linkText=SPOOLED DIRECTORY |  |
      driver.findElement(By.linkText("SPOOLED DIRECTORY")).click();
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/Spooled.html"));
      // 13 | click | linkText=SpooledDirectory daemons information |  |
      driver.findElement(By.linkText("SpooledDirectory daemons information"))
            .click();
      // 14 | click | linkText=HOSTS |  |
      driver.findElement(By.linkText("HOSTS")).click();
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/Hosts.html"));
      // 15 | click | css=input:nth-child(4) |  |
      driver.findElement(By.cssSelector("input:nth-child(4)")).click();
      // 16 | click | linkText=RULES |  |
      driver.findElement(By.linkText("RULES")).click();
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/Rules.html"));
      // 17 | click | css=p:nth-child(3) > input:nth-child(4) |  |
      driver.findElement(By.cssSelector("p:nth-child(3) > input:nth-child(4)"))
            .click();
      // 18 | click | linkText=SYSTEM |  |
      driver.findElement(By.linkText("SYSTEM")).click();
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/System.html"));
      // 19 | click | linkText=START |  |
      driver.findElement(By.linkText("START")).click();
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/index.html"));
    } catch (NoSuchElementException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } catch (StaleElementReferenceException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      // 29 | click | linkText=LOGOUT |  |
      driver.get("https://127.0.0.1:8067/Logout.html");
      assertTrue(
          driver.getCurrentUrl().equals("https://127.0.0.1:8067/Logout.html"));
    }
  }

  @Test
  public void test06_RestR66V2Simple() throws Exception {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    // Must be executed AFTER testCreateUserAdmin since it will use testb2
    // First with no authent: should fail
    try {
      final String baseUri = "http://localhost:8088/";
      // 2 | type | V2 [  |
      String v2BaseUri = baseUri + "v2/";
      driver.get(v2BaseUri + "transfers");
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      assertTrue(driver.getPageSource()
                       .equals("<html><head></head><body></body></html>"));
    } catch (NoSuchElementException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } catch (StaleElementReferenceException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      // nothing
    }

    try {
      final String baseUri = "http://testb2:testb2@localhost:8088/";
      // 2 | type | V2 [  |
      String v2BaseUri = baseUri + "v2/";
      driver.get(v2BaseUri + "transfers");
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      assertTrue(driver.getPageSource().contains("results"));
      driver.get(v2BaseUri + "hostconfig");
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      assertTrue(driver.getPageSource().contains("business"));
      assertTrue(driver.getPageSource().contains(Version.fullIdentifier()));
      assertTrue(driver.getPageSource()
                       .contains(org.waarp.openr66.protocol.utils.Version.ID));
      driver.get(v2BaseUri + "hosts");
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      assertTrue(driver.getPageSource().contains("results"));
      driver.get(v2BaseUri + "limits");
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      assertTrue(driver.getPageSource().contains("results"));
      driver.get(v2BaseUri + "rules");
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      assertTrue(driver.getPageSource().contains("results"));
      driver.get(v2BaseUri + "server/status");
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      assertTrue(driver.getPageSource().contains("serverName"));
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
  public void test02_Business() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    try {
      // Test name: HostConfig
      // Step # | name | target | value
      // 1 | open | / |
      driver.get("https://127.0.0.1:8067/");
      Thread.sleep(WAIT);
      // 4 | type | name=passwd | pwdhttp
      driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 5 | click | name=name |
      driver.findElement(By.name("name")).click();
      // 6 | type | name=name | monadmin
      driver.findElement(By.name("name")).sendKeys("monadmin");
      // 10 | click | name=Logon |
      driver.findElement(By.name("Logon")).click();
      Thread.sleep(WAIT);

      // Alias Testing
      // 11 | click | linkText=HOSTS |
      driver.findElement(By.linkText("HOSTS")).click();
      // 12 | click | css=input:nth-child(4) |
      driver.findElement(By.cssSelector("input:nth-child(4)")).click();
      // 13 | click | css=tr:nth-child(4) > td:nth-child(11) |
      driver.findElement(By.cssSelector("tr:nth-child(4) > td:nth-child(11)"))
            .click();
      String page = driver.getPageSource();
      assertFalse(page.contains("(ReverseAlias: mytest )"));
      // 14 | click | linkText=SYSTEM |
      driver.findElement(By.linkText("SYSTEM")).click();
      // 15 | click | name=ALIASES |
      driver.findElement(By.name("ALIASES")).click();
      // 17 | type | name=ALIASES | alias update
      driver.findElement(By.name("ALIASES")).clear();
      driver.findElement(By.name("ALIASES")).sendKeys(
          "<aliases><alias><realid>hostas</realid><aliasid>myhostas " +
          "mygreathostas</aliasid></alias><alias><realid>hosta</realid" +
          "><aliasid>myhosta mygreathosta</aliasid></alias><alias><realid" +
          ">hostb</realid><aliasid>myhostb " +
          "mygreathostb</aliasid></alias><alias><realid>test</realid><aliasid" +
          ">mytest</aliasid></alias></aliases>");
      // 18 | click | css=p:nth-child(19) > input |
      driver.findElement(By.cssSelector("p:nth-child(19) > input")).click();
      Thread.sleep(WAIT);
      // 19 | click | linkText=HOSTS |
      driver.findElement(By.linkText("HOSTS")).click();
      // 20 | click | css=input:nth-child(4) |
      driver.findElement(By.cssSelector("input:nth-child(4)")).click();
      // 21 | click | id=Comp1_sys_doctext |
      driver.findElement(By.id("Comp1_sys_doctext")).click();
      page = driver.getPageSource();
      assertTrue(page.contains("(ReverseAlias: mytest )"));

      // Role testing
      assertFalse(page.contains(
          "(Role: [ READONLY TRANSFER RULE LOGCONTROL PARTNER ])"));
      // 17 | click | linkText=SYSTEM |
      driver.findElement(By.linkText("SYSTEM")).click();
      Thread.sleep(WAIT);
      // 18 | click | name=ROLES |
      driver.findElement(By.name("ROLES")).click();
      driver.findElement(By.name("ROLES")).clear();
      // 20 | type | name=ROLES | <roles><role><roleid>hostas</roleid><roleset>READONLY TRANSFER RULE HOST LIMIT SYSTEM LOGCONTROL PARTNER CONFIGADMIN FULLADMIN</roleset></role><role><roleid>tests</roleid><roleset>READONLY TRANSFER RULE HOST LIMIT SYSTEM LOGCONTROL PARTNER CONFIGADMIN FULLADMIN</roleset></role><role><roleid>hosta</roleid><roleset>READONLY TRANSFER RULE HOST SYSTEM PARTNER CONFIGADMIN</roleset></role><role><roleid>test</roleid><roleset>READONLY TRANSFER RULE LOGCONTROL PARTNER</roleset></role><role><roleid>hostb</roleid><roleset>READONLY TRANSFER RULE HOST SYSTEM PARTNER CONFIGADMIN</roleset></role></roles>
      driver.findElement(By.name("ROLES")).sendKeys(
          "<roles><role><roleid>hostas</roleid><roleset>READONLY TRANSFER RULE HOST LIMIT SYSTEM LOGCONTROL PARTNER CONFIGADMIN FULLADMIN</roleset></role><role><roleid>tests</roleid><roleset>READONLY TRANSFER RULE HOST LIMIT SYSTEM LOGCONTROL PARTNER CONFIGADMIN FULLADMIN</roleset></role><role><roleid>hosta</roleid><roleset>READONLY TRANSFER RULE HOST SYSTEM PARTNER CONFIGADMIN</roleset></role><role><roleid>test</roleid><roleset>READONLY TRANSFER RULE LOGCONTROL PARTNER</roleset></role><role><roleid>hostb</roleid><roleset>READONLY TRANSFER RULE HOST SYSTEM PARTNER CONFIGADMIN</roleset></role></roles>");
      // 21 | click | css=p:nth-child(19) > input |
      driver.findElement(By.cssSelector("p:nth-child(19) > input")).click();
      Thread.sleep(WAIT);
      // 22 | click | linkText=HOSTS |
      driver.findElement(By.linkText("HOSTS")).click();
      // 23 | click | css=input:nth-child(4) |
      driver.findElement(By.cssSelector("input:nth-child(4)")).click();
      // 24 | click | css=tr:nth-child(4) |
      driver.findElement(By.cssSelector("tr:nth-child(4)")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains(
          "(Role: [ READONLY TRANSFER RULE LOGCONTROL PARTNER ])"));

      // Business
      assertFalse(page.contains("(ReverseAlias: mytest ) (Business: Allowed)"));
      // 25 | click | linkText=SYSTEM |
      driver.findElement(By.linkText("SYSTEM")).click();
      Thread.sleep(WAIT);
      // 26 | click | name=BUSINESS |
      driver.findElement(By.name("BUSINESS")).click();
      driver.findElement(By.name("BUSINESS")).clear();
      // 27 | type | name=BUSINESS | <business><businessid>hostas</businessid><businessid>hostbs</businessid><businessid>hosta</businessid><businessid>hostb</businessid><businessid>test</businessid></business>
      driver.findElement(By.name("BUSINESS")).sendKeys(
          "<business><businessid>hostas</businessid><businessid>hostbs</businessid><businessid>hosta</businessid><businessid>hostb</businessid><businessid>test</businessid></business>");
      // 28 | click | css=p:nth-child(19) > input |
      driver.findElement(By.cssSelector("p:nth-child(19) > input")).click();
      Thread.sleep(WAIT);
      // 29 | click | linkText=HOSTS |
      driver.findElement(By.linkText("HOSTS")).click();
      // 30 | click | css=input:nth-child(4) |
      driver.findElement(By.cssSelector("input:nth-child(4)")).click();
      // 31 | click | css=tr:nth-child(4) |
      driver.findElement(By.cssSelector("tr:nth-child(4)")).click();
      page = driver.getPageSource();
      assertTrue(page.contains("(ReverseAlias: mytest ) (Business: Allowed)"));
      Thread.sleep(WAIT);

    } catch (NoSuchElementException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } catch (StaleElementReferenceException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      // Disconnection
      driver.get("https://127.0.0.1:8867/Logout.html");
    }
  }

  @Test
  public void test08_System() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    try {
      // Step # | name | target | value
      // 1 | open | / |
      driver.get("https://127.0.0.1:8067/");
      Thread.sleep(WAIT);
      // 3 | click | name=name |
      driver.findElement(By.name("name")).click();
      // 4 | type | name=passwd | pwdhttp
      driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 5 | click | name=name |
      driver.findElement(By.name("name")).click();
      // 6 | type | name=name | monadmin
      driver.findElement(By.name("name")).sendKeys("monadmin");
      // 10 | click | name=Logon |
      driver.findElement(By.name("Logon")).click();
      Thread.sleep(WAIT);

      // 7 | click | linkText=SYSTEM |
      driver.findElement(By.linkText("SYSTEM")).click();
      Thread.sleep(WAIT);
      // 8 | click | css=td > input:nth-child(4) |
      driver.findElement(By.cssSelector("td > input:nth-child(4)")).click();
      // 9 | click | name=ACTION |
      driver.findElement(By.name("ACTION")).click();
      Thread.sleep(WAIT);
      String page = driver.getPageSource();
      assertTrue(page.contains("New language is: Web: fr OpenR66: en"));
      // 10 | click | css=td > input:nth-child(10) |
      driver.findElement(By.cssSelector("td > input:nth-child(10)")).click();
      // 11 | click | name=ACTION |
      driver.findElement(By.name("ACTION")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("Le nouveau langage est : Web: fr OpenR66: fr"));
      // 12 | click | name=change |
      driver.findElement(By.name("change")).click();
      // 13 | click | name=changesys |
      driver.findElement(By.name("changesys")).click();
      // 14 | click | name=ACTION |
      driver.findElement(By.name("ACTION")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("New language is: Web: en OpenR66: en"));
      // 15 | click | name=BSESSR |
      driver.findElement(By.name("BSESSR")).clear();
      // 16 | type | name=BSESSR | 100000000
      driver.findElement(By.name("BSESSR")).sendKeys("100000000");
      // 17 | click | name=BSESSW |
      driver.findElement(By.name("BSESSW")).clear();
      // 18 | type | name=BSESSW | 100000000
      driver.findElement(By.name("BSESSW")).sendKeys("100000000");
      // 19 | click | name=BGLOBR |
      driver.findElement(By.name("BGLOBR")).clear();
      // 20 | type | name=BGLOBR | 100000000
      driver.findElement(By.name("BGLOBR")).sendKeys("100000000");
      // 21 | click | name=BGLOBW |
      driver.findElement(By.name("BGLOBW")).clear();
      // 22 | type | name=BGLOBW | 100000000
      driver.findElement(By.name("BGLOBW")).sendKeys("100000000");
      // 23 | click | name=DTRA |
      driver.findElement(By.name("DTRA")).clear();
      // 24 | type | name=DTRA | 100
      driver.findElement(By.name("DTRA")).sendKeys("100");
      // 25 | click | name=DCOM |
      driver.findElement(By.name("DCOM")).clear();
      // 26 | type | name=DCOM | 100
      driver.findElement(By.name("DCOM")).sendKeys("100");
      // 27 | click | name=DRET |
      driver.findElement(By.name("DRET")).clear();
      // 28 | type | name=DRET | 100
      driver.findElement(By.name("DRET")).sendKeys("100");
      // 29 | click | css=p:nth-child(35) > input:nth-child(1) |
      driver.findElement(By.cssSelector("p:nth-child(35) > input:nth-child(1)"))
            .click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("Configuration Saved"));
      // 30 | click | css=form:nth-child(7) > input |
      driver.findElement(By.cssSelector("form:nth-child(7) > input")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("Export Directory: /tmp/R66//arch"));
      // 31 | click | name=blocking |
      driver.findElement(By.name("blocking")).click();
      // 32 | click | css=form:nth-child(14) > input:nth-child(4) |
      driver.findElement(
          By.cssSelector("form:nth-child(14) > input:nth-child(4)")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("New request will be blocked<"));
      // 33 | click | name=blocking |
      driver.findElement(By.name("blocking")).click();
      // 34 | click | css=form:nth-child(14) > input:nth-child(4) |
      driver.findElement(
          By.cssSelector("form:nth-child(14) > input:nth-child(4)")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("New request will be allowed"));
      // 35 | click | name=loglevel |
      driver.findElement(By.name("loglevel")).click();
      // 36 | click | css=form > input:nth-child(10) |
      driver.findElement(By.cssSelector("form > input:nth-child(10)")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("New language is: DEBUG"));
      // 37 | click | css=input:nth-child(6) |
      driver.findElement(By.cssSelector("input:nth-child(6)")).click();
      // 38 | click | css=form > input:nth-child(10) |
      driver.findElement(By.cssSelector("form > input:nth-child(10)")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("New language is: WARN"));
      // 39 | click | css=form:nth-child(9) > input |
      driver.findElement(By.cssSelector("form:nth-child(9) > input")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains(
          "You need to login to access to the OpenR66 Administrator"));
    } finally {
      // Disconnection
      driver.get("https://127.0.0.1:8867/Logout.html");
    }
  }

  @Test
  public void test03_CreateUser() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    // Test name: TestCreateUser2
    try {
      // Step # | name | target | value
      // 1 | open | / |
      driver.get("https://127.0.0.1:8067/");
      Thread.sleep(WAIT);
      // 4 | type | name=passwd | pwdhttp
      driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 5 | type | name=name | monadmin
      driver.findElement(By.name("name")).sendKeys("monadmin");
      // 6 | click | name=Logon |
      driver.findElement(By.name("Logon")).click();
      Thread.sleep(WAIT);
      // 7 | click | linkText=HOSTS |
      driver.findElement(By.linkText("HOSTS")).click();
      Thread.sleep(WAIT);
      // 9 | type | name=host | testb
      driver.findElement(By.name("host")).sendKeys("testb");
      // 10 | type | name=address | 127.0.0.1
      driver.findElement(By.name("address")).sendKeys("127.0.0.1");
      // 12 | type | name=port | -1
      driver.findElement(By.name("port")).sendKeys("-1");
      // 14 | type | name=hostkey | testb
      driver.findElement(By.name("hostkey")).sendKeys("testb");
      // 15 | click | name=isclient |
      driver.findElement(By.name("isclient")).click();
      // 16 | click | name=isactive |
      driver.findElement(By.name("isactive")).click();
      // 17 | click | name=ACTION |
      driver.findElement(By.name("ACTION")).click();
      Thread.sleep(WAIT);
      // 18 | click | linkText=LOGOUT |
      driver.findElement(By.linkText("LOGOUT")).click();
      Thread.sleep(WAIT);
      // 20 | type | name=name | testb
      driver.findElement(By.name("name")).sendKeys("testb");
      // 21 | type | name=passwd | testb
      driver.findElement(By.name("passwd")).sendKeys("testb");
      // 22 | click | name=Logon |
      driver.findElement(By.name("Logon")).click();
      Thread.sleep(WAIT);
      // 23 | click | linkText=HOSTS |
      driver.findElement(By.linkText("HOSTS")).click();
      Thread.sleep(WAIT);
      String page = driver.getPageSource();
      assertTrue(page.contains("Your profile does not allow this function."));
      // 24 | click | linkText=RULES |
      driver.findElement(By.linkText("RULES")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("Your profile does not allow this function."));
      // 26 | click | linkText=LOGOUT |
      driver.findElement(By.linkText("LOGOUT")).click();
      Thread.sleep(WAIT);
      // 28 | type | name=passwd | pwdhttp
      driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 29 | type | name=name | monadmin
      driver.findElement(By.name("name")).sendKeys("monadmin");
      // 30 | click | name=Logon |
      driver.findElement(By.name("Logon")).click();
      Thread.sleep(WAIT);
      // 31 | click | linkText=SYSTEM |
      driver.findElement(By.linkText("SYSTEM")).click();
      Thread.sleep(WAIT);
      // 34 | click | name=ROLES |
      driver.findElement(By.name("ROLES")).clear();
      // 35 | type | name=ROLES | <roles><role><roleid>hostas</roleid><roleset>READONLY TRANSFER RULE HOST LIMIT SYSTEM LOGCONTROL PARTNER CONFIGADMIN FULLADMIN</roleset></role><role><roleid>tests</roleid><roleset>READONLY TRANSFER RULE HOST LIMIT SYSTEM LOGCONTROL PARTNER CONFIGADMIN FULLADMIN</roleset></role><role><roleid>hosta</roleid><roleset>READONLY TRANSFER RULE HOST SYSTEM PARTNER CONFIGADMIN</roleset></role><role><roleid>test</roleid><roleset>READONLY TRANSFER LOGCONTROL PARTNER</roleset></role><role><roleid>hostb</roleid><roleset>READONLY TRANSFER RULE HOST SYSTEM PARTNER CONFIGADMIN</roleset></role><role><roleid>testb</roleid><roleset>READONLY TRANSFER RULE HOST</roleset></role></roles>
      driver.findElement(By.name("ROLES")).sendKeys(
          "<roles><role><roleid>hostas</roleid><roleset>READONLY TRANSFER RULE HOST LIMIT SYSTEM LOGCONTROL PARTNER CONFIGADMIN FULLADMIN</roleset></role><role><roleid>tests</roleid><roleset>READONLY TRANSFER RULE HOST LIMIT SYSTEM LOGCONTROL PARTNER CONFIGADMIN FULLADMIN</roleset></role><role><roleid>hosta</roleid><roleset>READONLY TRANSFER RULE HOST SYSTEM PARTNER CONFIGADMIN</roleset></role><role><roleid>test</roleid><roleset>READONLY TRANSFER LOGCONTROL PARTNER</roleset></role><role><roleid>hostb</roleid><roleset>READONLY TRANSFER RULE HOST SYSTEM PARTNER CONFIGADMIN</roleset></role><role><roleid>testb</roleid><roleset>READONLY TRANSFER RULE HOST</roleset></role></roles>");
      // 36 | click | css=form:nth-child(5) |
      driver.findElement(By.cssSelector("form:nth-child(5)")).click();
      // 37 | click | css=p:nth-child(19) > input |
      driver.findElement(By.cssSelector("p:nth-child(19) > input")).click();
      Thread.sleep(WAIT);
      // 38 | click | linkText=LOGOUT |
      driver.findElement(By.linkText("LOGOUT")).click();
      Thread.sleep(WAIT);
      // 40 | type | name=passwd | testb
      driver.findElement(By.name("passwd")).sendKeys("testb");
      // 41 | type | name=name | testb
      driver.findElement(By.name("name")).sendKeys("testb");
      // 42 | click | name=Logon |
      driver.findElement(By.name("Logon")).click();
      Thread.sleep(WAIT);
      // 43 | click | linkText=HOSTS |
      driver.findElement(By.linkText("HOSTS")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertFalse(page.contains("Your profile does not allow this function."));
      // 44 | click | linkText=RULES |
      driver.findElement(By.linkText("RULES")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertFalse(page.contains("Your profile does not allow this function."));
    } finally {
      // 45 | click | linkText=LOGOUT |
      driver.findElement(By.linkText("LOGOUT")).click();
    }
  }

  @Test
  public void test04_CreateUserAdmin() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    // Test name: TestCreateUser2
    try {
      // Step # | name | target | value
      // 1 | open | / |
      driver.get("https://127.0.0.1:8067/");
      Thread.sleep(WAIT);
      // 4 | type | name=passwd | pwdhttp
      driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 5 | type | name=name | monadmin
      driver.findElement(By.name("name")).sendKeys("monadmin");
      // 6 | click | name=Logon |
      driver.findElement(By.name("Logon")).click();
      Thread.sleep(WAIT);
      // 7 | click | linkText=HOSTS |
      driver.findElement(By.linkText("HOSTS")).click();
      Thread.sleep(WAIT);
      // 9 | type | name=host | testb
      driver.findElement(By.name("host")).sendKeys("testb2");
      // 10 | type | name=address | 127.0.0.1
      driver.findElement(By.name("address")).sendKeys("127.0.0.1");
      // 12 | type | name=port | -1
      driver.findElement(By.name("port")).sendKeys("-1");
      // 14 | type | name=hostkey | testb
      driver.findElement(By.name("hostkey")).sendKeys("testb2");
      // 15 | click | name=isclient |
      driver.findElement(By.name("isclient")).click();
      // 16 | click | name=isactive |
      driver.findElement(By.name("isactive")).click();
      driver.findElement(By.name("admin")).click();
      // 17 | click | name=ACTION |
      driver.findElement(By.name("ACTION")).click();
      Thread.sleep(WAIT);
      // 18 | click | linkText=LOGOUT |
      driver.findElement(By.linkText("LOGOUT")).click();
      Thread.sleep(WAIT);
      // 40 | type | name=passwd | testb
      driver.findElement(By.name("passwd")).sendKeys("testb2");
      // 41 | type | name=name | testb
      driver.findElement(By.name("name")).sendKeys("testb2");
      // 42 | click | name=Logon |
      driver.findElement(By.name("Logon")).click();
      Thread.sleep(WAIT);
      // 43 | click | linkText=HOSTS |
      driver.findElement(By.linkText("HOSTS")).click();
      Thread.sleep(WAIT);
      String page = driver.getPageSource();
      assertFalse(page.contains("Your profile does not allow this function."));
      // 44 | click | linkText=RULES |
      driver.findElement(By.linkText("RULES")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertFalse(page.contains("Your profile does not allow this function."));
    } finally {
      // 45 | click | linkText=LOGOUT |
      driver.findElement(By.linkText("LOGOUT")).click();
    }
  }

  @Test
  public void test09_InteractiveShutdown() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    try {
      // Test name: TestInteract
      // Step # | name | target | value
      // 1 | open | / |
      driver.get("https://127.0.0.1:8067/");
      Thread.sleep(WAIT);
      // 4 | type | name=passwd | pwdhttp
      driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 5 | type | name=name | monadmin
      driver.findElement(By.name("name")).sendKeys("monadmin");
      // 6 | click | name=Logon |
      driver.findElement(By.name("Logon")).click();
      Thread.sleep(WAIT);
      // 7 | click | linkText=TRANSFERS |
      driver.findElement(By.linkText("TRANSFERS")).click();
      // 8 | click | linkText=LISTING |
      driver.findElement(By.linkText("LISTING")).click();
      // 9 | click | name=ACTION |
      driver.findElement(By.name("ACTION")).click();
      // 10 | click | css=input:nth-child(5) |
      driver.findElement(By.cssSelector("input:nth-child(5)")).click();
      // 11 | click | css=p > input:nth-child(6) |
      driver.findElement(By.cssSelector("p > input:nth-child(6)")).click();
      // 12 | click | linkText=CANCEL-RESTART |
      driver.findElement(By.linkText("CANCEL-RESTART")).click();
      // 13 | click | name=ACTION |
      driver.findElement(By.name("ACTION")).click();
      // 14 | click | css=input:nth-child(5) |
      driver.findElement(By.cssSelector("input:nth-child(5)")).click();
      // 15 | click | css=input:nth-child(1) |
      driver.findElement(By.cssSelector("input:nth-child(1)")).click();
      // 16 | click | css=p:nth-child(39) > input:nth-child(2) |
      driver.findElement(By.cssSelector("p:nth-child(39) > input:nth-child(2)"))
            .click();
      // 17 | click | linkText=HOSTS |
      driver.findElement(By.linkText("HOSTS")).click();
      // 18 | click | css=input:nth-child(4) |
      driver.findElement(By.cssSelector("input:nth-child(4)")).click();
      // 19 | click | css=tr:nth-child(2) > td:nth-child(12) > input |
      driver.findElement(
          By.cssSelector("tr:nth-child(2) > td:nth-child(12) > input")).click();
      // 20 | click | css=td:nth-child(14) > input |
      driver.findElement(By.cssSelector("td:nth-child(14) > input")).click();
      // 21 | click | linkText=RULES |
      driver.findElement(By.linkText("RULES")).click();
      // 22 | click | css=p:nth-child(3) > input:nth-child(4) |
      driver.findElement(By.cssSelector("p:nth-child(3) > input:nth-child(4)"))
            .click();
      // 23 | click | css=p:nth-child(3) > input:nth-child(5) |
      driver.findElement(By.cssSelector("p:nth-child(3) > input:nth-child(5)"))
            .click();
      // 24 | click | css=#AutoNumber1 tr:nth-child(4) input:nth-child(1) |
      driver.findElement(
                By.cssSelector("#AutoNumber1 tr:nth-child(4) input:nth-child(1)"))
            .click();
      // 25 | click | linkText=SYSTEM |
      driver.findElement(By.linkText("SYSTEM")).click();
      // 26 | click | css=form:nth-child(11) > input |
      driver.findElement(By.cssSelector("form:nth-child(11) > input")).click();
      Thread.sleep(WAIT);
    } finally {
      // 45 | click | linkText=LOGOUT |
      driver.findElement(By.linkText("LOGOUT")).click();
    }
  }

}
