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
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.NullPrintStream;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.io.File;
import java.io.PrintStream;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AdminResponsiveTest extends TestAbstract {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AdminResponsiveTest.class);
  private static final String CONFIG_SERVER_A_MINIMAL_XML =
      "config-serverA-minimal.xml";
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
                           CONFIG_SERVER_A_MINIMAL_RESPONSIVE_XXML, true);
    Thread.sleep(100);
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
    Thread.sleep(500);
  }

  private static final int RETRY = 5;

  private static void get(final String url) throws NoSuchElementException {
    Exception error = null;
    try {
      Thread.sleep(WAIT);
    } catch (final InterruptedException e2) {
      // ignore
    }
    for (int i = 0; i < RETRY; i++) {
      try {
        driver.get(url);
        try {
          Thread.sleep(WAIT);
        } catch (final InterruptedException e2) {
          // ignore
        }
        return;
      } catch (final Exception e) {
        error = e;
        try {
          Thread.sleep(WAIT);
        } catch (final InterruptedException e2) {
          // ignore
        }
      }
    }
    throw new NoSuchElementException("Get Error", error);
  }

  private static boolean currentUrlEquals(final String url) {
    try {
      Thread.sleep(WAIT);
    } catch (final InterruptedException e2) {
      // ignore
    }
    for (int i = 0; i < RETRY; i++) {
      try {
        return driver.getCurrentUrl().equals(url);
      } catch (final Exception e) {
        // ignore
        try {
          Thread.sleep(WAIT);
        } catch (final InterruptedException e2) {
          // ignore
        }
      }
    }
    return false;
  }

  private static WebElement findElement(final By by)
      throws NoSuchElementException {
    try {
      Thread.sleep(WAIT);
    } catch (final InterruptedException e2) {
      // ignore
    }
    Exception error = null;
    for (int i = 0; i < RETRY; i++) {
      try {
        WebElement element = driver.findElement(by);
        try {
          Thread.sleep(WAIT);
        } catch (final InterruptedException e2) {
          // ignore
        }
        return element;
      } catch (final Exception e) {
        error = e;
        try {
          Thread.sleep(WAIT);
        } catch (final InterruptedException e2) {
          // ignore
        }
      }
    }
    throw new NoSuchElementException("Find Element Error", error);
  }

  private static String getPageSource() {
    try {
      Thread.sleep(WAIT);
    } catch (final InterruptedException e2) {
      // ignore
    }
    return driver.getPageSource();
  }

  @Test
  public void test04_WebHttpSimpleAdmin() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    try {
      // Test name: TestResponsiveMonitor
      // Step # | name | target | value | comment
      // 1 | open | / |  |
      get("http://127.0.0.1:8866/");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/"));
      // 2 | click | linkText=Active Transfers |  |
      get("http://127.0.0.1:8866/active");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/active"));
      // 4 | click | linkText=In Error Transfers |  |
      get("http://127.0.0.1:8866/error");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/error"));
      // 5 | open | / |  |
      get("http://127.0.0.1:8866/");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/"));
      // 6 | click | linkText=Finished Transfers |  |
      get("http://127.0.0.1:8866/done");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/done"));
      // 7 | open | / |  |
      get("http://127.0.0.1:8866/");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/"));
      // 8 | click | linkText=All Transfers |  |
      get("http://127.0.0.1:8866/all");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/all"));
      // 9 | open | / |  |
      get("http://127.0.0.1:8866/");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/"));
      // 10 | click | css=li:nth-child(5) > a |  |
      get("http://127.0.0.1:8866/statusxml");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/statusxml"));
      get("http://127.0.0.1:8866/statusxml?DETAIL=1");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/statusxml?DETAIL=1"));
      // 11 | open | / |  |
      get("http://127.0.0.1:8866/");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/"));
      // 12 | click | css=li:nth-child(6) > a |  |
      get("http://127.0.0.1:8866/statusjson");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/statusjson"));
      get("http://127.0.0.1:8866/statusjson?DETAIL=1");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/statusjson?DETAIL=1"));
      // 13 | open | / |  |
      get("http://127.0.0.1:8866/");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/"));
      // 18 | click | linkText=All Spooled daemons |  |
      get("http://127.0.0.1:8866/spooled");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/spooled"));
      // 19 | open | / |  |
      get("http://127.0.0.1:8866/");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/"));
      // 20 | click | css=li:nth-child(10) > a |  |
      get("http://127.0.0.1:8866/spooleddetail");
      assertTrue(currentUrlEquals("http://127.0.0.1:8866/spooleddetail"));
    } catch (NoSuchElementException e) {
      logger.error("Failed but instable", e);
    } catch (StaleElementReferenceException e) {
      logger.error("Failed but instable", e);
    } finally {
    }
  }


  @Test
  public void test05_WebHttpsBasic() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    try {
      // Test name: TestResponsiveAdmin
      // Step # | name | target | value | comment
      // 1 | open | https://127.0.0.1:8867/ |  |
      get("https://127.0.0.1:8867/");
      // 2 | type | id=passwd | pwdhttp |
      findElement(By.id("passwd")).sendKeys("pwdhttp");
      // 3 | type | id=name | monadmin |
      findElement(By.id("name")).sendKeys("monadmin");
      // 4 | click | id=submit |  |
      findElement(By.id("submit")).click();
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/index.html"));
      // 5 | open | https://127.0.0.1:8867/ |  |
      get("https://127.0.0.1:8867/");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/"));
      // 6 | click | linkText=HOSTS |  |
      get("https://127.0.0.1:8867/Hosts.html");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/Hosts.html"));
      // 7 | open | https://127.0.0.1:8867/ |  |
      get("https://127.0.0.1:8867/");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/"));
      // 8 | click | linkText=RULES |  |
      get("https://127.0.0.1:8867/Rules.html");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/Rules.html"));
      // 9 | open | https://127.0.0.1:8867/ |  |
      get("https://127.0.0.1:8867/");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/"));
      // 10 | click | linkText=SYSTEM |  |
      get("https://127.0.0.1:8867/System.html");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/System.html"));
      // 11 | open | https://127.0.0.1:8867/ |  |
      get("https://127.0.0.1:8867/");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/"));
      // 24 | click | linkText=START |  |
      get("https://127.0.0.1:8867/index.html");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/index.html"));
      get("https://127.0.0.1:8867/CancelRestart.html");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/CancelRestart.html"));
      get("https://127.0.0.1:8867/Export.html");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/Export.html"));
      get("https://127.0.0.1:8867/Listing.html");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/Listing.html"));
      get("https://127.0.0.1:8867/Spooled.html");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/Spooled.html"));
      get("https://127.0.0.1:8867/NotExist.html");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/NotExist.html"));
    } catch (NoSuchElementException e) {
      logger.error("Failed but instable", e);
    } catch (StaleElementReferenceException e) {
      logger.error("Failed but instable", e);
    } finally {
      // 29 | click | linkText=LOGOUT |  |
      try {
        get("https://127.0.0.1:8867/Logout.html");
      } catch (Exception e) {
        // ignore
      }
    }
  }

  @Test
  public void test06_WebInteractiveResponsive() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    try {
      // Test name: TestInteractResponsive
      // Step # | name | target | value
      // 1 | open | https://127.0.0.1:8867/ |  |
      get("https://127.0.0.1:8867/");
      // 2 | type | id=passwd | pwdhttp |
      findElement(By.id("passwd")).sendKeys("pwdhttp");
      // 3 | type | id=name | monadmin |
      findElement(By.id("name")).sendKeys("monadmin");
      // 4 | click | id=submit |  |
      findElement(By.id("submit")).click();
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/index.html"));
      // 5 | open | https://127.0.0.1:8867/ |  |
      get("https://127.0.0.1:8867/");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/"));
      get("https://127.0.0.1:8867/Listing.html");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/Listing.html"));
      // 9 | click | css=.btn-primary:nth-child(1) |
      findElement(By.cssSelector(".btn-primary:nth-child(1)")).click();
      // 10 | click | css=.btn-primary:nth-child(2) |
      findElement(By.cssSelector(".btn-primary:nth-child(2)")).click();
      get("https://127.0.0.1:8867/CancelRestart.html");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/CancelRestart.html"));
      // 13 | click | css=.btn-primary:nth-child(1) |
      findElement(By.cssSelector(".btn-primary:nth-child(1)")).click();
      // 14 | click | css=.btn-primary:nth-child(2) |
      findElement(By.cssSelector(".btn-primary:nth-child(2)")).click();
      // 15 | click | css=.btn-warning |
      findElement(By.cssSelector(".btn-warning")).click();
      // 16 | click | css=.btn-info |
      findElement(By.cssSelector(".btn-info")).click();
      get("https://127.0.0.1:8867/Hosts.html");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/Hosts.html"));
      // 18 | click | css=.btn-primary:nth-child(1) |
      findElement(By.cssSelector(".btn-primary:nth-child(1)")).click();
      // 19 | click | css=.odd:nth-child(1) > td:nth-child(1) |
      findElement(By.cssSelector(".odd:nth-child(1) > td:nth-child(1)"))
          .click();
      // 20 | click | css=.btn:nth-child(2) > span |
      findElement(By.cssSelector(".btn:nth-child(2) > span")).click();
      // 21 | click | id=update-button |
      findElement(By.id("update-button")).click();
      // 22 | click | linkText=RULES |
      get("https://127.0.0.1:8867/Rules.html");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/Rules.html"));
      // 23 | click | css=.btn-primary:nth-child(1) |
      findElement(By.cssSelector(".btn-primary:nth-child(1)")).click();
      // 24 | click | css=.even:nth-child(2) > .sorting_1 |
      findElement(By.cssSelector(".even:nth-child(2) > .sorting_1")).click();
      // 25 | click | css=.btn:nth-child(2) > span |
      findElement(By.cssSelector(".btn:nth-child(2) > span")).click();
      // 26 | click | id=update-button |
      findElement(By.id("update-button")).click();
    } catch (NoSuchElementException e) {
      logger.error("Failed but instable", e);
    } catch (StaleElementReferenceException e) {
      logger.error("Failed but instable", e);
    } finally {
      // 29 | click | linkText=LOGOUT |  |
      try {
        get("https://127.0.0.1:8867/Logout.html");
      } catch (Exception e) {
        // ignore
      }
    }
  }

  @Test
  public void test01_WebBusiness() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    try {
      // Test name: HostConfig
      // Step # | name | target | value
      // 1 | open | / |
      get("https://127.0.0.1:8867/");
      // 2 | type | name=passwd | pwdhttp |
      findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 3 | type | name=name | monadmin |
      findElement(By.name("name")).sendKeys("monadmin");
      // 4 | click | name=Logon |  |
      findElement(By.name("Logon")).click();

      // Alias
      // 4 | click | linkText=HOSTS |
      get("https://127.0.0.1:8867/Hosts.html");
      // 5 | click | css=.odd:nth-child(3) > td:nth-child(11) |
      findElement(By.cssSelector(".odd:nth-child(3) > td:nth-child(11)"))
          .click();
      // 6 | click | css=.selected > td:nth-child(11) |
      findElement(By.cssSelector(".selected > td:nth-child(11)")).click();
      String page = getPageSource();
      assertFalse(page.contains("(ReverseAlias: mytest )"));
      // 7 | click | linkText=SYSTEM |
      get("https://127.0.0.1:8867/System.html");
      // 8 | click | id=ALIASES |
      findElement(By.id("ALIASES")).click();
      // 9 | click | css=#ah_myModal .modal-body > .btn |
      findElement(By.cssSelector("#ah_myModal .modal-body > .btn")).click();
      // 10 | click | id=aid3 |
      findElement(By.id("aid3")).click();
      // 11 | select | id=aid3 | label=test
      {
        WebElement dropdown = findElement(By.id("aid3"));
        Thread.sleep(WAIT);
        dropdown.findElement(By.xpath("//option[. = 'test']")).click();
        Thread.sleep(WAIT);
      }
      // 12 | click | css=#aid3 > option:nth-child(5) |
      findElement(By.cssSelector("#aid3 > option:nth-child(5)")).click();
      // 13 | click | id=newitem3 |
      findElement(By.id("newitem3")).click();
      // 14 | type | id=newitem3 | mytest
      findElement(By.id("newitem3")).sendKeys("mytest");
      // 15 | click | css=tr:nth-child(4) > td:nth-child(4) > .btn |
      findElement(By.cssSelector("tr:nth-child(4) > td:nth-child(4) > .btn"))
          .click();
      // 16 | mouseOver | css=tr:nth-child(4) > td:nth-child(4) > .btn |
      {
        WebElement element = findElement(
            By.cssSelector("tr:nth-child(4) > td:nth-child(4) > .btn"));
        Thread.sleep(WAIT);
        Actions builder = new Actions(driver);
        builder.moveToElement(element).perform();
        Thread.sleep(WAIT);
      }
      // 17 | mouseOut | css=tr:nth-child(4) > td:nth-child(4) > .btn |
      {
        WebElement element = findElement(By.tagName("body"));
        Thread.sleep(WAIT);
        Actions builder = new Actions(driver);
        builder.moveToElement(element, 0, 0).perform();
        Thread.sleep(WAIT);
      }
      // 18 | click | css=#ah_myModal .btn-success |
      findElement(By.cssSelector("#ah_myModal .btn-success")).click();
      // 19 | click | css=.btn-success:nth-child(1) |
      findElement(By.cssSelector(".btn-success:nth-child(1)")).click();
      // 20 | click | linkText=HOSTS |
      get("https://127.0.0.1:8867/Hosts.html");
      //findElement(By.linkText("HOSTS")).click();
      // 22 | click | css=.odd:nth-child(3) > td:nth-child(11) |
      findElement(By.cssSelector(".odd:nth-child(3) > td:nth-child(11)"))
          .click();
      page = getPageSource();
      assertTrue(page.contains("(ReverseAlias: mytest )"));

      // Role
      assertFalse(page.contains(
          "(Role: [ READONLY TRANSFER RULE LOGCONTROL PARTNER ])"));
      // 23 | click | linkText=SYSTEM |
      get("https://127.0.0.1:8867/System.html");
      // 24 | click | id=ROLES |
      findElement(By.id("ROLES")).click();
      // 25 | click | css=tr:nth-child(4) .multiselect |
      findElement(By.cssSelector("tr:nth-child(4) .multiselect")).click();
      // 26 | click | css=.open li:nth-child(4) input |
      findElement(By.cssSelector(".open li:nth-child(4) input")).click();
      // 27 | click | css=#rh_myModal .modal-body |
      findElement(By.cssSelector("#rh_myModal .modal-body")).click();
      // 28 | click | css=#rh_myModal .btn-success |
      findElement(By.cssSelector("#rh_myModal .btn-success")).click();
      // 29 | click | css=.btn-success:nth-child(1) |
      findElement(By.cssSelector(".btn-success:nth-child(1)")).click();
      // 30 | click | linkText=HOSTS |
      get("https://127.0.0.1:8867/Hosts.html");
      //findElement(By.linkText("HOSTS")).click();
      // 31 | click | css=.odd:nth-child(3) > td:nth-child(11) |
      findElement(By.cssSelector(".odd:nth-child(3) > td:nth-child(11)"))
          .click();
      page = getPageSource();
      assertTrue(page.contains(
          "(Role: [ READONLY TRANSFER RULE LOGCONTROL PARTNER ])"));

      // Business
      assertFalse(page.contains("(ReverseAlias: mytest ) (Business: Allowed)"));
      // 32 | click | linkText=SYSTEM |
      get("https://127.0.0.1:8867/System.html");
      //findElement(By.linkText("SYSTEM")).click();
      // 33 | click | id=BUSINESS |
      findElement(By.id("BUSINESS")).click();
      // 34 | click | css=.multiselect |
      findElement(By.cssSelector(".multiselect")).click();
      // 35 | click | css=li:nth-child(5) input |
      findElement(By.cssSelector("li:nth-child(5) input")).click();
      // 36 | click | css=#bh_myModal .modal-body |
      findElement(By.cssSelector("#bh_myModal .modal-body")).click();
      // 37 | click | css=#bh_myModal .btn-success |
      findElement(By.cssSelector("#bh_myModal .btn-success")).click();
      // 38 | click | css=.btn-success:nth-child(1) |
      findElement(By.cssSelector(".btn-success:nth-child(1)")).click();
      // 39 | click | linkText=HOSTS |
      get("https://127.0.0.1:8867/Hosts.html");
      //findElement(By.linkText("HOSTS")).click();
      // 40 | click | css=.odd:nth-child(3) |
      findElement(By.cssSelector(".odd:nth-child(3)")).click();
      page = getPageSource();
      assertTrue(page.contains("(ReverseAlias: mytest ) (Business: Allowed)"));
    } catch (NoSuchElementException e) {
      logger.error("Failed but instable", e);
    } catch (StaleElementReferenceException e) {
      logger.error("Failed but instable", e);
    } finally {
      // Disconnection
      try {
        get("https://127.0.0.1:8867/Logout.html");
      } catch (Exception e) {
        // Ignore
      }
    }
  }

  @Test
  public void test07_WebSystem() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    try {
      // Test name: SystemResponsive
      // Step # | name | target | value
      // 1 | open | / |
      get("https://127.0.0.1:8867/");
      // 2 | type | name=passwd | pwdhttp |
      findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 3 | type | name=name | monadmin |
      findElement(By.name("name")).sendKeys("monadmin");
      // 4 | click | name=Logon |  |
      findElement(By.name("Logon")).click();

      // 6 | click | linkText=SYSTEM |
      get("https://127.0.0.1:8867/System.html");
      // 7 | click | css=.form-group:nth-child(1) > input:nth-child(5) |
      findElement(
          By.cssSelector(".form-group:nth-child(1) > input:nth-child(5)"))
          .click();
      // 8 | click | css=.btn-info |
      findElement(By.cssSelector(".btn-info")).click();
      String page = getPageSource();
      assertTrue(page.contains("New language is: Web: fr OpenR66: en"));
      // 9 | click | css=.form-group:nth-child(2) > input:nth-child(5) |
      findElement(
          By.cssSelector(".form-group:nth-child(2) > input:nth-child(5)"))
          .click();
      // 10 | click | css=.btn-info |
      findElement(By.cssSelector(".btn-info")).click();
      page = getPageSource();
      assertTrue(page.contains("Le nouveau langage est : Web: fr OpenR66: fr"));
      // 11 | click | name=change |
      findElement(By.name("change")).click();
      // 12 | click | name=changesys |
      findElement(By.name("changesys")).click();
      // 14 | click | css=.btn-info |
      findElement(By.cssSelector(".btn-info")).click();
      page = getPageSource();
      assertTrue(page.contains("New language is: Web: en OpenR66: en"));
      // 15 | click | name=DTRA |
      findElement(By.name("DTRA")).click();
      // 16 | click | name=DTRA |
      findElement(By.name("DTRA")).click();
      // 17 | doubleClick | name=DTRA |
      {
        WebElement element = findElement(By.name("DTRA"));
        Thread.sleep(WAIT);
        Actions builder = new Actions(driver);
        builder.doubleClick(element).perform();
        Thread.sleep(WAIT);
      }
      // 18 | type | name=DTRA | 100
      findElement(By.name("DTRA")).clear();
      findElement(By.name("DTRA")).sendKeys("100");
      // 19 | click | name=DCOM |
      findElement(By.name("DCOM")).click();
      // 20 | click | name=DCOM |
      findElement(By.name("DCOM")).click();
      // 21 | doubleClick | name=DCOM |
      {
        WebElement element = findElement(By.name("DCOM"));
        Thread.sleep(WAIT);
        Actions builder = new Actions(driver);
        builder.doubleClick(element).perform();
        Thread.sleep(WAIT);
      }
      // 22 | type | name=DCOM | 10000
      findElement(By.name("DCOM")).clear();
      findElement(By.name("DCOM")).sendKeys("10000");
      // 23 | click | name=DRET |
      findElement(By.name("DRET")).click();
      // 24 | click | name=DRET |
      findElement(By.name("DRET")).click();
      // 25 | doubleClick | name=DRET |
      {
        WebElement element = findElement(By.name("DRET"));
        Thread.sleep(WAIT);
        Actions builder = new Actions(driver);
        builder.doubleClick(element).perform();
        Thread.sleep(WAIT);
      }
      // 26 | type | name=DRET | 100
      findElement(By.name("DRET")).clear();
      findElement(By.name("DRET")).sendKeys("100");
      // 27 | click | name=BGLOBW |
      findElement(By.name("BGLOBW")).click();
      // 28 | type | name=BGLOBW | 100000000
      findElement(By.name("BGLOBW")).clear();
      findElement(By.name("BGLOBW")).sendKeys("100000000");
      // 29 | click | name=BGLOBR |
      findElement(By.name("BGLOBR")).click();
      // 30 | type | name=BGLOBR | 100000000
      findElement(By.name("BGLOBR")).clear();
      findElement(By.name("BGLOBR")).sendKeys("100000000");
      // 31 | click | name=BSESSW |
      findElement(By.name("BSESSW")).click();
      // 32 | type | name=BSESSW | 100000000
      findElement(By.name("BSESSW")).clear();
      findElement(By.name("BSESSW")).sendKeys("100000000");
      // 33 | click | name=BSESSR |
      findElement(By.name("BSESSR")).click();
      // 34 | type | name=BSESSR | 100000000
      findElement(By.name("BSESSR")).clear();
      findElement(By.name("BSESSR")).sendKeys("100000000");
      // 35 | click | css=.btn-primary:nth-child(1) |
      findElement(By.cssSelector(".btn-primary:nth-child(1)")).click();
      page = getPageSource();
      assertTrue(page.contains("Configuration Saved"));
      // 36 | click | name=loglevel |
      findElement(By.id("logdebug")).click();
      // 37 | click | css=.form-group:nth-child(3) > .btn-warning |
      findElement(By.cssSelector(".form-group:nth-child(3) > .btn-warning"))
          .click();
      page = getPageSource();
      assertTrue(page.contains("New language is: DEBUG"));
      // 38 | click | css=input:nth-child(6) |
      findElement(By.id("logwarn")).click();
      // 39 | click | css=.form-group:nth-child(3) > .btn-warning |
      findElement(By.cssSelector(".form-group:nth-child(3) > .btn-warning"))
          .click();
      page = getPageSource();
      assertTrue(page.contains("New language is: WARN"));
      // 41 | click | css=.btn-primary:nth-child(4) |
      findElement(By.cssSelector(".btn-primary:nth-child(4)")).click();
      page = getPageSource();
      assertTrue(page.contains("Export Directory: /arch"));
      // 43 | click | name=blocking |
      findElement(By.name("blocking")).click();
      // 44 | click | css=.btn:nth-child(5) |
      findElement(By.cssSelector(".btn:nth-child(5)")).click();
      page = getPageSource();
      assertTrue(page.contains("New request will be blocked"));
      // 46 | click | name=blocking |
      findElement(By.name("blocking")).click();
      // 47 | click | css=.btn:nth-child(5) |
      findElement(By.cssSelector(".btn:nth-child(5)")).click();
      page = getPageSource();
      assertTrue(page.contains("New request will be allowed"));
      // 49 | click | css=.btn-success:nth-child(4) |
      findElement(By.cssSelector(".btn-success:nth-child(4)")).click();
      page = getPageSource();
      assertTrue(page.contains(
          "You need to login to access to the OpenR66 Administrator."));
    } catch (NoSuchElementException e) {
      logger.error("Failed but instable", e);
    } catch (StaleElementReferenceException e) {
      logger.error("Failed but instable", e);
    } finally {
      // Disconnection
      try {
        get("https://127.0.0.1:8867/Logout.html");
      } catch (Exception e) {
        // Ignore
      }
    }
  }

  @Test
  public void test02_WebCreateUser() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    // Test name: TestCreateUser
    try {
      // Step # | name | target | value
      // 1 | open | / |
      get("https://127.0.0.1:8867/");
      // 4 | type | id=passwd | pwdhttp
      findElement(By.id("passwd")).sendKeys("pwdhttp");
      // 5 | type | id=name | monadmin
      findElement(By.id("name")).sendKeys("monadmin");
      // 6 | click | id=submit |
      findElement(By.id("submit")).click();
      // 7 | click | linkText=HOSTS |
      get("https://127.0.0.1:8867/Hosts.html");
      // 9 | type | id=HOSTID | testb
      findElement(By.id("HOSTID")).sendKeys("testb");
      // 10 | type | id=ADDRESS | 127.0.0.1
      findElement(By.id("ADDRESS")).sendKeys("127.0.0.1");
      // 12 | type | id=PORT | -1
      findElement(By.id("PORT")).sendKeys("-1");
      // 13 | click | id=ISACTIVE |
      findElement(By.id("ISACTIVE")).click();
      // 14 | click | id=ISCLIENT |
      findElement(By.id("ISCLIENT")).click();
      // 16 | type | id=HOSTKEY | testb
      findElement(By.id("HOSTKEY")).sendKeys("testb");
      // 17 | click | id=create-button |
      findElement(By.id("create-button")).click();
      // 18 | click | linkText=LOGOUT |
      get("https://127.0.0.1:8867/Logout.html");
      // 20 | type | id=passwd | testb
      findElement(By.id("passwd")).sendKeys("testb");
      // 21 | type | id=name | testb
      findElement(By.id("name")).sendKeys("testb");
      // 22 | click | id=submit |
      findElement(By.id("submit")).click();
      // 23 | click | linkText=HOSTS |
      get("https://127.0.0.1:8867/Hosts.html");
      String page = getPageSource();
      assertTrue(page.contains("Your profile does not allow this function."));
      // 24 | click | linkText=RULES |
      get("https://127.0.0.1:8867/Rules.html");
      page = getPageSource();
      assertTrue(page.contains("Your profile does not allow this function."));
      // 25 | click | linkText=SYSTEM |
      get("https://127.0.0.1:8867/System.html");
      // 26 | click | css=.btn-success:nth-child(1) |
      findElement(By.cssSelector(".btn-success:nth-child(1)")).click();
      // 30 | click | linkText=LOGOUT |
      get("https://127.0.0.1:8867/Logout.html");
      // 32 | type | id=passwd | pwdhttp
      findElement(By.id("passwd")).sendKeys("pwdhttp");
      // 33 | type | id=name | monadmin
      findElement(By.id("name")).sendKeys("monadmin");
      // 34 | click | id=submit |
      findElement(By.id("submit")).click();
      // 35 | click | linkText=SYSTEM |
      get("https://127.0.0.1:8867/System.html");
      // 36 | click | id=ROLES |
      findElement(By.id("ROLES")).click();
      // 37 | mouseOver | id=ROLES |
      {
        WebElement element = findElement(By.id("ROLES"));
        Thread.sleep(WAIT);
        Actions builder = new Actions(driver);
        builder.moveToElement(element).perform();
        Thread.sleep(WAIT);
      }
      // 38 | mouseOut | id=ROLES |
      {
        WebElement element = findElement(By.tagName("body"));
        Thread.sleep(WAIT);
        Actions builder = new Actions(driver);
        builder.moveToElement(element, 0, 0).perform();
        Thread.sleep(WAIT);
      }
      // 39 | click | css=#rh_myModal .modal-body > .btn |
      findElement(By.cssSelector("#rh_myModal .modal-body > .btn")).click();
      // 40 | mouseOver | css=#rh_myModal .modal-body > .btn |
      {
        WebElement element = driver
            .findElement(By.cssSelector("#rh_myModal .modal-body > .btn"));
        Thread.sleep(WAIT);
        Actions builder = new Actions(driver);
        builder.moveToElement(element).perform();
        Thread.sleep(WAIT);
      }
      // 41 | mouseOut | css=#rh_myModal .modal-body > .btn |
      {
        WebElement element = findElement(By.tagName("body"));
        Thread.sleep(WAIT);
        Actions builder = new Actions(driver);
        builder.moveToElement(element, 0, 0).perform();
        Thread.sleep(WAIT);
      }
      // 42 | click | id=rid5 |
      findElement(By.id("rid5")).click();
      // 43 | select | id=rid5 | label=testb
      {
        WebElement dropdown = findElement(By.id("rid5"));
        Thread.sleep(WAIT);
        dropdown.findElement(By.xpath("//option[. = 'testb']")).click();
        Thread.sleep(WAIT);
      }
      // 44 | click | css=#rid5 > option:nth-child(7) |
      findElement(By.cssSelector("#rid5 > option:nth-child(7)")).click();
      // 45 | click | css=tr:nth-child(6) .multiselect-selected-text |
      findElement(By.cssSelector("tr:nth-child(6) .multiselect-selected-text"))
          .click();
      // 46 | click | css=.open li:nth-child(2) .checkbox |
      findElement(By.cssSelector(".open li:nth-child(2) .checkbox")).click();
      // 47 | click | css=.open li:nth-child(3) .checkbox |
      findElement(By.cssSelector(".open li:nth-child(3) .checkbox")).click();
      // 48 | click | css=.open li:nth-child(4) .checkbox |
      findElement(By.cssSelector(".open li:nth-child(4) .checkbox")).click();
      // 49 | click | css=.open li:nth-child(5) .checkbox |
      findElement(By.cssSelector(".open li:nth-child(5) .checkbox")).click();
      // 50 | click | css=#rh_myModal .modal-body |
      findElement(By.cssSelector("#rh_myModal .modal-body")).click();
      // 51 | click | css=#rh_myModal .btn-success |
      findElement(By.cssSelector("#rh_myModal .btn-success")).click();
      // 52 | click | css=.btn-success:nth-child(1) |
      findElement(By.cssSelector(".btn-success:nth-child(1)")).click();
      // 53 | click | linkText=LOGOUT |
      get("https://127.0.0.1:8867/Logout.html");
      // 55 | type | id=passwd | testb
      findElement(By.id("passwd")).sendKeys("testb");
      // 56 | type | id=name | testb
      findElement(By.id("name")).sendKeys("testb");
      // 57 | click | id=submit |
      findElement(By.id("submit")).click();
      // 58 | click | linkText=HOSTS |
      get("https://127.0.0.1:8867/Hosts.html");
      page = getPageSource();
      assertFalse(page.contains("Your profile does not allow this function."));
      // 59 | click | linkText=RULES |
      get("https://127.0.0.1:8867/Rules.html");
      page = getPageSource();
      assertFalse(page.contains("Your profile does not allow this function."));
    } catch (NoSuchElementException e) {
      logger.error("Failed but instable", e);
    } catch (StaleElementReferenceException e) {
      logger.error("Failed but instable", e);
    } finally {
      // Disconnection
      try {
        get("https://127.0.0.1:8867/Logout.html");
      } catch (Exception e) {
        // Ignore
      }
    }
  }

  @Test
  public void test03_WebCreateUserAdmin() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    // Test name: TestCreateUser
    try {
      // Step # | name | target | value
      // 1 | open | / |
      get("https://127.0.0.1:8867/");
      // 4 | type | id=passwd | pwdhttp
      findElement(By.id("passwd")).sendKeys("pwdhttp");
      // 5 | type | id=name | monadmin
      findElement(By.id("name")).sendKeys("monadmin");
      // 6 | click | id=submit |
      findElement(By.id("submit")).click();
      // 7 | click | linkText=HOSTS |
      get("https://127.0.0.1:8867/Hosts.html");
      // 9 | type | id=HOSTID | testb
      findElement(By.id("HOSTID")).sendKeys("testb2");
      // 10 | type | id=ADDRESS | 127.0.0.1
      findElement(By.id("ADDRESS")).sendKeys("127.0.0.1");
      // 12 | type | id=PORT | -1
      findElement(By.id("PORT")).sendKeys("-1");
      // 13 | click | id=ISACTIVE |
      findElement(By.id("ISACTIVE")).click();
      // 14 | click | id=ISCLIENT |
      findElement(By.id("ISCLIENT")).click();
      findElement(By.id("ADMINROLE")).click();
      // 16 | type | id=HOSTKEY | testb
      findElement(By.id("HOSTKEY")).sendKeys("testb2");
      // 17 | click | id=create-button |
      findElement(By.id("create-button")).click();
      // 18 | click | linkText=LOGOUT |
      get("https://127.0.0.1:8867/Logout.html");
      // 55 | type | id=passwd | testb
      findElement(By.id("passwd")).sendKeys("testb2");
      // 56 | type | id=name | testb
      findElement(By.id("name")).sendKeys("testb2");
      // 57 | click | id=submit |
      findElement(By.id("submit")).click();
      // 58 | click | linkText=HOSTS |
      get("https://127.0.0.1:8867/Hosts.html");
      String page = getPageSource();
      assertFalse(page.contains("Your profile does not allow this function."));
      // 59 | click | linkText=RULES |
      get("https://127.0.0.1:8867/Rules.html");
      page = getPageSource();
      assertFalse(page.contains("Your profile does not allow this function."));
    } catch (NoSuchElementException e) {
      logger.error("Failed but instable", e);
    } catch (StaleElementReferenceException e) {
      logger.error("Failed but instable", e);
    } finally {
      // Disconnection
      try {
        get("https://127.0.0.1:8867/Logout.html");
      } catch (Exception e) {
        // Ignore
      }
    }
  }

  @Test
  public void test08_createAndFollow() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    try {
      get("https://127.0.0.1:8867/");
      findElement(By.id("passwd")).sendKeys("pwdhttp");
      findElement(By.id("name")).sendKeys("monadmin");
      findElement(By.id("submit")).click();
      get("https://127.0.0.1:8867/CreateTransfer.html");
      findElement(By.id("IDRULE")).click();
      {
        WebElement dropdown = findElement(By.id("IDRULE"));
        Thread.sleep(WAIT);
        dropdown.findElement(By.xpath("//option[. = 'rule3']")).click();
        Thread.sleep(WAIT);
      }
      findElement(By.cssSelector("#IDRULE > option:nth-child(2)")).click();
      findElement(By.id("REQUESTED")).click();
      {
        WebElement dropdown = findElement(By.id("REQUESTED"));
        Thread.sleep(WAIT);
        dropdown.findElement(By.xpath("//option[. = 'hostas']")).click();
        Thread.sleep(WAIT);
      }
      findElement(By.cssSelector("#REQUESTED > option:nth-child(2)")).click();
      findElement(By.id("FILENAME")).sendKeys("test");
      findElement(By.id("TRANSFERINFO")).sendKeys("mon info");
      findElement(By.id("STARTTRANS")).sendKeys("2200/12/31 23:59");
      findElement(By.id("FOLLOWID")).sendKeys("10");
      findElement(By.id("BLOCKSZ")).sendKeys("65535");
      findElement(By.id("create-button")).click();
      findElement(By.id("FILENAME")).sendKeys("test2");
      findElement(By.id("TRANSFERINFO")).sendKeys("my info");
      findElement(By.id("STARTTRANS")).sendKeys("2300/12/31 23:59");
      findElement(By.id("FOLLOWID")).sendKeys("20");
      findElement(By.id("create-button")).click();
      findElement(By.cssSelector(".odd > td:nth-child(2)")).click();
      findElement(By.cssSelector(".btn:nth-child(2) > span")).click();
      findElement(By.id("FOLLOWID")).sendKeys("25");
      findElement(By.id("update-button")).click();
      get("https://127.0.0.1:8867/CreateTransfer.html");
      findElement(By.id("FollowId0")).sendKeys("20");
      findElement(By.id("TopSearch")).click();
      get("https://127.0.0.1:8867/CreateTransfer.html");
      findElement(By.id("FollowId0")).sendKeys("10");
      findElement(By.id("TopSearch")).click();
      get("https://127.0.0.1:8867/CreateTransfer.html");
      findElement(By.id("FollowId0")).sendKeys("25");
      findElement(By.id("TopSearch")).click();
    } catch (NoSuchElementException e) {
      logger.error("Failed but instable", e);
    } catch (StaleElementReferenceException e) {
      logger.error("Failed but instable", e);
    } finally {
      // Disconnection
      try {
        get("https://127.0.0.1:8867/Logout.html");
      } catch (Exception e) {
        // Ignore
      }
    }
  }

  @Test
  public void test09_WebShutdown() throws InterruptedException {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    // Test name: TestConnectShutdown
    // Step # | name | target | value
    try {
      // Test name: TestInteractResponsive
      // Step # | name | target | value
      // 1 | open | https://127.0.0.1:8867/ |  |
      get("https://127.0.0.1:8867/");
      // 2 | type | id=passwd | pwdhttp |
      findElement(By.id("passwd")).sendKeys("pwdhttp");
      // 3 | type | id=name | monadmin |
      findElement(By.id("name")).sendKeys("monadmin");
      // 4 | click | id=submit |  |
      findElement(By.id("submit")).click();
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/index.html"));
      // 5 | open | https://127.0.0.1:8867/ |  |
      get("https://127.0.0.1:8867/");
      assertTrue(currentUrlEquals("https://127.0.0.1:8867/"));
      // 10 | click | linkText=SYSTEM |
      get("https://127.0.0.1:8867/System.html");
      // 11 | click | css=.text-right:nth-child(3) > .btn |
      findElement(By.cssSelector(".text-right:nth-child(3) > .btn")).click();
    } catch (NoSuchElementException e) {
      logger.error("Failed but instable", e);
    } catch (StaleElementReferenceException e) {
      logger.error("Failed but instable", e);
    } finally {
      // Disconnection
      try {
        get("https://127.0.0.1:8867/Logout.html");
      } catch (Exception e) {
        // Ignore
      }
    }
  }

}
