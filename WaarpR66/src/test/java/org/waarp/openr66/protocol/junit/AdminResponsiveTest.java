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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.io.File;

import static org.junit.Assert.*;

public class AdminResponsiveTest extends TestAbstract {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AdminResponsiveTest.class);
  private static final String CONFIG_SERVER_A_MINIMAL_XML =
      "config-serverA-minimal.xml";
  private static final String CONFIG_SERVER_A_MINIMAL_RESPONSIVE_XXML =
      "config-serverA-minimal-Responsive.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML =
      "Linux/config/config-serverInitA.xml";

  private static final int WAIT = 400;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    final ClassLoader classLoader = NetworkClientTest.class.getClassLoader();
    final File file =
        new File(classLoader.getResource("logback-test.xml").getFile());
    if (file.exists()) {
      driverType = DriverType.PHANTOMJS;
      initiateWebDriver(file.getParentFile());
    }
    setUpDbBeforeClass();
    setUpBeforeClassServer(LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML,
                           CONFIG_SERVER_A_MINIMAL_RESPONSIVE_XXML, true);
  }

  /**
   * @throws Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    Thread.sleep(100);
    finalizeDriver();
    // Shutdown server
    logger.warn("Shutdown Server");
    Configuration.configuration.setTimeoutCon(100);

    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    tearDownAfterClassServer();
  }

  @After
  public void restartDriver() throws InterruptedException {
    reloadDriver();
  }

  @Test
  public void testHttpsBusiness() throws InterruptedException {
    try {
      // Test name: HostConfig
      // Step # | name | target | value
      // 1 | open | / |
      driver.get("https://127.0.0.1:8867/");
      Thread.sleep(WAIT);
      // 2 | type | name=passwd | pwdhttp |
      driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 3 | type | name=name | monadmin |
      driver.findElement(By.name("name")).sendKeys("monadmin");
      // 4 | click | name=Logon |  |
      driver.findElement(By.name("Logon")).click();
      Thread.sleep(WAIT);

      // Alias
      // 4 | click | linkText=HOSTS |
      driver.get("https://127.0.0.1:8867/Hosts.html");
      // 5 | click | css=.odd:nth-child(3) > td:nth-child(11) |
      driver.findElement(By.cssSelector(".odd:nth-child(3) > td:nth-child(11)"))
            .click();
      // 6 | click | css=.selected > td:nth-child(11) |
      driver.findElement(By.cssSelector(".selected > td:nth-child(11)"))
            .click();
      String page = driver.getPageSource();
      assertFalse(page.contains("(ReverseAlias: mytest )"));
      // 7 | click | linkText=SYSTEM |
      driver.get("https://127.0.0.1:8867/System.html");
      // 8 | click | id=ALIASES |
      driver.findElement(By.id("ALIASES")).click();
      Thread.sleep(WAIT);
      // 9 | click | css=#ah_myModal .modal-body > .btn |
      driver.findElement(By.cssSelector("#ah_myModal .modal-body > .btn"))
            .click();
      // 10 | click | id=aid3 |
      driver.findElement(By.id("aid3")).click();
      // 11 | select | id=aid3 | label=test
      {
        WebElement dropdown = driver.findElement(By.id("aid3"));
        dropdown.findElement(By.xpath("//option[. = 'test']")).click();
      }
      // 12 | click | css=#aid3 > option:nth-child(5) |
      driver.findElement(By.cssSelector("#aid3 > option:nth-child(5)")).click();
      // 13 | click | id=newitem3 |
      driver.findElement(By.id("newitem3")).click();
      // 14 | type | id=newitem3 | mytest
      driver.findElement(By.id("newitem3")).sendKeys("mytest");
      // 15 | click | css=tr:nth-child(4) > td:nth-child(4) > .btn |
      driver.findElement(
          By.cssSelector("tr:nth-child(4) > td:nth-child(4) > .btn")).click();
      // 16 | mouseOver | css=tr:nth-child(4) > td:nth-child(4) > .btn |
      {
        WebElement element = driver.findElement(
            By.cssSelector("tr:nth-child(4) > td:nth-child(4) > .btn"));
        Actions builder = new Actions(driver);
        builder.moveToElement(element).perform();
      }
      // 17 | mouseOut | css=tr:nth-child(4) > td:nth-child(4) > .btn |
      {
        WebElement element = driver.findElement(By.tagName("body"));
        Actions builder = new Actions(driver);
        builder.moveToElement(element, 0, 0).perform();
      }
      // 18 | click | css=#ah_myModal .btn-success |
      driver.findElement(By.cssSelector("#ah_myModal .btn-success")).click();
      Thread.sleep(WAIT);
      // 19 | click | css=.btn-success:nth-child(1) |
      driver.findElement(By.cssSelector(".btn-success:nth-child(1)")).click();
      Thread.sleep(WAIT);
      // 20 | click | linkText=HOSTS |
      driver.get("https://127.0.0.1:8867/Hosts.html");
      //driver.findElement(By.linkText("HOSTS")).click();
      // 22 | click | css=.odd:nth-child(3) > td:nth-child(11) |
      driver.findElement(By.cssSelector(".odd:nth-child(3) > td:nth-child(11)"))
            .click();
      page = driver.getPageSource();
      assertTrue(page.contains("(ReverseAlias: mytest )"));

      // Role
      assertFalse(page.contains(
          "(Role: [ READONLY TRANSFER RULE LOGCONTROL PARTNER ])"));
      // 23 | click | linkText=SYSTEM |
      driver.get("https://127.0.0.1:8867/System.html");
      // 24 | click | id=ROLES |
      driver.findElement(By.id("ROLES")).click();
      Thread.sleep(WAIT);
      // 25 | click | css=tr:nth-child(4) .multiselect |
      driver.findElement(By.cssSelector("tr:nth-child(4) .multiselect"))
            .click();
      // 26 | click | css=.open li:nth-child(4) input |
      driver.findElement(By.cssSelector(".open li:nth-child(4) input")).click();
      // 27 | click | css=#rh_myModal .modal-body |
      driver.findElement(By.cssSelector("#rh_myModal .modal-body")).click();
      // 28 | click | css=#rh_myModal .btn-success |
      driver.findElement(By.cssSelector("#rh_myModal .btn-success")).click();
      // 29 | click | css=.btn-success:nth-child(1) |
      driver.findElement(By.cssSelector(".btn-success:nth-child(1)")).click();
      Thread.sleep(WAIT);
      // 30 | click | linkText=HOSTS |
      driver.get("https://127.0.0.1:8867/Hosts.html");
      //driver.findElement(By.linkText("HOSTS")).click();
      Thread.sleep(WAIT);
      // 31 | click | css=.odd:nth-child(3) > td:nth-child(11) |
      driver.findElement(By.cssSelector(".odd:nth-child(3) > td:nth-child(11)"))
            .click();
      page = driver.getPageSource();
      assertTrue(page.contains(
          "(Role: [ READONLY TRANSFER RULE LOGCONTROL PARTNER ])"));

      // Business
      assertFalse(page.contains("(ReverseAlias: mytest ) (Business: Allowed)"));
      // 32 | click | linkText=SYSTEM |
      driver.get("https://127.0.0.1:8867/System.html");
      //driver.findElement(By.linkText("SYSTEM")).click();
      Thread.sleep(WAIT);
      // 33 | click | id=BUSINESS |
      driver.findElement(By.id("BUSINESS")).click();
      Thread.sleep(WAIT);
      // 34 | click | css=.multiselect |
      driver.findElement(By.cssSelector(".multiselect")).click();
      // 35 | click | css=li:nth-child(5) input |
      driver.findElement(By.cssSelector("li:nth-child(5) input")).click();
      // 36 | click | css=#bh_myModal .modal-body |
      driver.findElement(By.cssSelector("#bh_myModal .modal-body")).click();
      // 37 | click | css=#bh_myModal .btn-success |
      driver.findElement(By.cssSelector("#bh_myModal .btn-success")).click();
      // 38 | click | css=.btn-success:nth-child(1) |
      driver.findElement(By.cssSelector(".btn-success:nth-child(1)")).click();
      Thread.sleep(WAIT);
      // 39 | click | linkText=HOSTS |
      driver.get("https://127.0.0.1:8867/Hosts.html");
      //driver.findElement(By.linkText("HOSTS")).click();
      Thread.sleep(WAIT);
      // 40 | click | css=.odd:nth-child(3) |
      driver.findElement(By.cssSelector(".odd:nth-child(3)")).click();
      page = driver.getPageSource();
      assertTrue(page.contains("(ReverseAlias: mytest ) (Business: Allowed)"));

      // Disconnection
      driver.get("https://127.0.0.1:8867/Logout.html");
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
  public void testSystemResponsive() throws InterruptedException {
    try {
      // Test name: SystemResponsive
      // Step # | name | target | value
      // 1 | open | / |
      driver.get("https://127.0.0.1:8867/");
      Thread.sleep(WAIT);
      // 2 | type | name=passwd | pwdhttp |
      driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 3 | type | name=name | monadmin |
      driver.findElement(By.name("name")).sendKeys("monadmin");
      // 4 | click | name=Logon |  |
      driver.findElement(By.name("Logon")).click();
      Thread.sleep(WAIT);

      // 6 | click | linkText=SYSTEM |
      driver.get("https://127.0.0.1:8867/System.html");
      Thread.sleep(WAIT);
      // 7 | click | css=.form-group:nth-child(1) > input:nth-child(5) |
      driver.findElement(
          By.cssSelector(".form-group:nth-child(1) > input:nth-child(5)"))
            .click();
      // 8 | click | css=.btn-info |
      driver.findElement(By.cssSelector(".btn-info")).click();
      Thread.sleep(WAIT);
      String page = driver.getPageSource();
      assertTrue(page.contains("New language is: Web: fr OpenR66: en"));
      // 9 | click | css=.form-group:nth-child(2) > input:nth-child(5) |
      driver.findElement(
          By.cssSelector(".form-group:nth-child(2) > input:nth-child(5)"))
            .click();
      // 10 | click | css=.btn-info |
      driver.findElement(By.cssSelector(".btn-info")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("Le nouveau langage est : Web: fr OpenR66: fr"));
      // 11 | click | name=change |
      driver.findElement(By.name("change")).click();
      // 12 | click | name=changesys |
      driver.findElement(By.name("changesys")).click();
      // 14 | click | css=.btn-info |
      driver.findElement(By.cssSelector(".btn-info")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("New language is: Web: en OpenR66: en"));
      // 15 | click | name=DTRA |
      driver.findElement(By.name("DTRA")).click();
      // 16 | click | name=DTRA |
      driver.findElement(By.name("DTRA")).click();
      // 17 | doubleClick | name=DTRA |
      {
        WebElement element = driver.findElement(By.name("DTRA"));
        Actions builder = new Actions(driver);
        builder.doubleClick(element).perform();
      }
      // 18 | type | name=DTRA | 100
      driver.findElement(By.name("DTRA")).clear();
      driver.findElement(By.name("DTRA")).sendKeys("100");
      // 19 | click | name=DCOM |
      driver.findElement(By.name("DCOM")).click();
      // 20 | click | name=DCOM |
      driver.findElement(By.name("DCOM")).click();
      // 21 | doubleClick | name=DCOM |
      {
        WebElement element = driver.findElement(By.name("DCOM"));
        Actions builder = new Actions(driver);
        builder.doubleClick(element).perform();
      }
      // 22 | type | name=DCOM | 10000
      driver.findElement(By.name("DCOM")).clear();
      driver.findElement(By.name("DCOM")).sendKeys("10000");
      // 23 | click | name=DRET |
      driver.findElement(By.name("DRET")).click();
      // 24 | click | name=DRET |
      driver.findElement(By.name("DRET")).click();
      // 25 | doubleClick | name=DRET |
      {
        WebElement element = driver.findElement(By.name("DRET"));
        Actions builder = new Actions(driver);
        builder.doubleClick(element).perform();
      }
      // 26 | type | name=DRET | 100
      driver.findElement(By.name("DRET")).clear();
      driver.findElement(By.name("DRET")).sendKeys("100");
      // 27 | click | name=BGLOBW |
      driver.findElement(By.name("BGLOBW")).click();
      // 28 | type | name=BGLOBW | 100000000
      driver.findElement(By.name("BGLOBW")).clear();
      driver.findElement(By.name("BGLOBW")).sendKeys("100000000");
      // 29 | click | name=BGLOBR |
      driver.findElement(By.name("BGLOBR")).click();
      // 30 | type | name=BGLOBR | 100000000
      driver.findElement(By.name("BGLOBR")).clear();
      driver.findElement(By.name("BGLOBR")).sendKeys("100000000");
      // 31 | click | name=BSESSW |
      driver.findElement(By.name("BSESSW")).click();
      // 32 | type | name=BSESSW | 100000000
      driver.findElement(By.name("BSESSW")).clear();
      driver.findElement(By.name("BSESSW")).sendKeys("100000000");
      // 33 | click | name=BSESSR |
      driver.findElement(By.name("BSESSR")).click();
      // 34 | type | name=BSESSR | 100000000
      driver.findElement(By.name("BSESSR")).clear();
      driver.findElement(By.name("BSESSR")).sendKeys("100000000");
      // 35 | click | css=.btn-primary:nth-child(1) |
      driver.findElement(By.cssSelector(".btn-primary:nth-child(1)")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("Configuration Saved"));
      // 36 | click | name=loglevel |
      driver.findElement(By.name("loglevel")).click();
      // 37 | click | css=.form-group:nth-child(3) > .btn-warning |
      driver.findElement(
          By.cssSelector(".form-group:nth-child(3) > .btn-warning")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("New language is: DEBUG"));
      // 38 | click | css=input:nth-child(6) |
      driver.findElement(By.cssSelector("input:nth-child(6)")).click();
      // 39 | click | css=.form-group:nth-child(3) > .btn-warning |
      driver.findElement(
          By.cssSelector(".form-group:nth-child(3) > .btn-warning")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("New language is: WARN"));
      // 41 | click | css=.btn-primary:nth-child(4) |
      driver.findElement(By.cssSelector(".btn-primary:nth-child(4)")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("Export Directory: /arch"));
      // 43 | click | name=blocking |
      driver.findElement(By.name("blocking")).click();
      // 44 | click | css=.btn:nth-child(5) |
      driver.findElement(By.cssSelector(".btn:nth-child(5)")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("New request will be blocked"));
      // 46 | click | name=blocking |
      driver.findElement(By.name("blocking")).click();
      // 47 | click | css=.btn:nth-child(5) |
      driver.findElement(By.cssSelector(".btn:nth-child(5)")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("New request will be allowed"));
      // 49 | click | css=.btn-success:nth-child(4) |
      driver.findElement(By.cssSelector(".btn-success:nth-child(4)")).click();
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains(
          "You need to login to access to the OpenR66 Administrator."));
    } finally {
      // Disconnection
      driver.get("https://127.0.0.1:8867/Logout.html");
    }
  }

  @Test
  public void testCreateUser() throws InterruptedException {
    // Test name: TestCreateUser
    try {
      // Step # | name | target | value
      // 1 | open | / |
      driver.get("https://127.0.0.1:8867/");
      Thread.sleep(WAIT);
      // 4 | type | id=passwd | pwdhttp
      driver.findElement(By.id("passwd")).sendKeys("pwdhttp");
      // 5 | type | id=name | monadmin
      driver.findElement(By.id("name")).sendKeys("monadmin");
      // 6 | click | id=submit |
      driver.findElement(By.id("submit")).click();
      Thread.sleep(WAIT);
      // 7 | click | linkText=HOSTS |
      driver.get("https://127.0.0.1:8867/Hosts.html");
      Thread.sleep(WAIT);
      // 9 | type | id=HOSTID | testb
      driver.findElement(By.id("HOSTID")).sendKeys("testb");
      // 10 | type | id=ADDRESS | 127.0.0.1
      driver.findElement(By.id("ADDRESS")).sendKeys("127.0.0.1");
      // 12 | type | id=PORT | -1
      driver.findElement(By.id("PORT")).sendKeys("-1");
      // 13 | click | id=ISACTIVE |
      driver.findElement(By.id("ISACTIVE")).click();
      // 14 | click | id=ISCLIENT |
      driver.findElement(By.id("ISCLIENT")).click();
      // 16 | type | id=HOSTKEY | testb
      driver.findElement(By.id("HOSTKEY")).sendKeys("testb");
      // 17 | click | id=create-button |
      driver.findElement(By.id("create-button")).click();
      Thread.sleep(WAIT);
      // 18 | click | linkText=LOGOUT |
      driver.get("https://127.0.0.1:8867/Logout.html");
      Thread.sleep(WAIT);
      // 20 | type | id=passwd | testb
      driver.findElement(By.id("passwd")).sendKeys("testb");
      // 21 | type | id=name | testb
      driver.findElement(By.id("name")).sendKeys("testb");
      // 22 | click | id=submit |
      driver.findElement(By.id("submit")).click();
      Thread.sleep(WAIT);
      // 23 | click | linkText=HOSTS |
      driver.get("https://127.0.0.1:8867/Hosts.html");
      Thread.sleep(WAIT);
      String page = driver.getPageSource();
      assertTrue(page.contains("Your profile does not allow this function."));
      // 24 | click | linkText=RULES |
      driver.get("https://127.0.0.1:8867/Rules.html");
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertTrue(page.contains("Your profile does not allow this function."));
      // 25 | click | linkText=SYSTEM |
      driver.get("https://127.0.0.1:8867/System.html");
      Thread.sleep(WAIT);
      // 26 | click | css=.btn-success:nth-child(1) |
      driver.findElement(By.cssSelector(".btn-success:nth-child(1)")).click();
      Thread.sleep(WAIT);
      // 30 | click | linkText=LOGOUT |
      driver.get("https://127.0.0.1:8867/Logout.html");
      Thread.sleep(WAIT);
      // 32 | type | id=passwd | pwdhttp
      driver.findElement(By.id("passwd")).sendKeys("pwdhttp");
      // 33 | type | id=name | monadmin
      driver.findElement(By.id("name")).sendKeys("monadmin");
      // 34 | click | id=submit |
      driver.findElement(By.id("submit")).click();
      Thread.sleep(WAIT);
      // 35 | click | linkText=SYSTEM |
      driver.get("https://127.0.0.1:8867/System.html");
      Thread.sleep(WAIT);
      // 36 | click | id=ROLES |
      driver.findElement(By.id("ROLES")).click();
      // 37 | mouseOver | id=ROLES |
      {
        WebElement element = driver.findElement(By.id("ROLES"));
        Actions builder = new Actions(driver);
        builder.moveToElement(element).perform();
      }
      // 38 | mouseOut | id=ROLES |
      {
        WebElement element = driver.findElement(By.tagName("body"));
        Actions builder = new Actions(driver);
        builder.moveToElement(element, 0, 0).perform();
      }
      Thread.sleep(WAIT);
      // 39 | click | css=#rh_myModal .modal-body > .btn |
      driver.findElement(By.cssSelector("#rh_myModal .modal-body > .btn"))
            .click();
      // 40 | mouseOver | css=#rh_myModal .modal-body > .btn |
      {
        WebElement element = driver
            .findElement(By.cssSelector("#rh_myModal .modal-body > .btn"));
        Actions builder = new Actions(driver);
        builder.moveToElement(element).perform();
      }
      // 41 | mouseOut | css=#rh_myModal .modal-body > .btn |
      {
        WebElement element = driver.findElement(By.tagName("body"));
        Actions builder = new Actions(driver);
        builder.moveToElement(element, 0, 0).perform();
      }
      // 42 | click | id=rid5 |
      driver.findElement(By.id("rid5")).click();
      // 43 | select | id=rid5 | label=testb
      {
        WebElement dropdown = driver.findElement(By.id("rid5"));
        dropdown.findElement(By.xpath("//option[. = 'testb']")).click();
      }
      // 44 | click | css=#rid5 > option:nth-child(7) |
      driver.findElement(By.cssSelector("#rid5 > option:nth-child(7)")).click();
      // 45 | click | css=tr:nth-child(6) .multiselect-selected-text |
      driver.findElement(
          By.cssSelector("tr:nth-child(6) .multiselect-selected-text")).click();
      // 46 | click | css=.open li:nth-child(2) .checkbox |
      driver.findElement(By.cssSelector(".open li:nth-child(2) .checkbox"))
            .click();
      // 47 | click | css=.open li:nth-child(3) .checkbox |
      driver.findElement(By.cssSelector(".open li:nth-child(3) .checkbox"))
            .click();
      // 48 | click | css=.open li:nth-child(4) .checkbox |
      driver.findElement(By.cssSelector(".open li:nth-child(4) .checkbox"))
            .click();
      // 49 | click | css=.open li:nth-child(5) .checkbox |
      driver.findElement(By.cssSelector(".open li:nth-child(5) .checkbox"))
            .click();
      // 50 | click | css=#rh_myModal .modal-body |
      driver.findElement(By.cssSelector("#rh_myModal .modal-body")).click();
      // 51 | click | css=#rh_myModal .btn-success |
      driver.findElement(By.cssSelector("#rh_myModal .btn-success")).click();
      Thread.sleep(WAIT);
      // 52 | click | css=.btn-success:nth-child(1) |
      driver.findElement(By.cssSelector(".btn-success:nth-child(1)")).click();
      Thread.sleep(WAIT);
      // 53 | click | linkText=LOGOUT |
      driver.get("https://127.0.0.1:8867/Logout.html");
      Thread.sleep(WAIT);
      // 55 | type | id=passwd | testb
      driver.findElement(By.id("passwd")).sendKeys("testb");
      // 56 | type | id=name | testb
      driver.findElement(By.id("name")).sendKeys("testb");
      // 57 | click | id=submit |
      driver.findElement(By.id("submit")).click();
      Thread.sleep(WAIT);
      // 58 | click | linkText=HOSTS |
      driver.get("https://127.0.0.1:8867/Hosts.html");
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertFalse(page.contains("Your profile does not allow this function."));
      // 59 | click | linkText=RULES |
      driver.get("https://127.0.0.1:8867/Rules.html");
      Thread.sleep(WAIT);
      page = driver.getPageSource();
      assertFalse(page.contains("Your profile does not allow this function."));
      Thread.sleep(WAIT);
    } finally {
      // Disconnection
      driver.get("https://127.0.0.1:8867/Logout.html");
    }
  }
}
