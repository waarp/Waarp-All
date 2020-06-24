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

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.waarp.common.utility.TestWatcherJunit4;

import static org.junit.Assert.*;

public class R66ProxyResponsiveTest extends CommonUtil {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();


  @BeforeClass
  public static void launchConfig() throws Exception {
    CONFIG_PROXY_XML = "config-proxy-Responsive.xml";
    launchServers();
  }

  @Test
  public void test98_Http() throws InterruptedException {
    // Test name: TestMonitorSimple
    try {
      // Step # | name | target | value | comment
      // 1 | open | / |  |
      driver.get("http://127.0.0.1:11186/");
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
  public void test98_Https() throws InterruptedException {
    //Thread.sleep(1000000);
    try {
      // Test name: ProxyAdmin
      // Step # | name | target | value | comment
      // 1 | open | / |  |
      driver.get("https://127.0.0.1:11187/");
      // 7 | click | name=name |  |
      driver.findElement(By.name("name")).click();
      // 8 | type | name=name | monadmin |
      driver.findElement(By.name("name")).sendKeys("monadmin");
      // 9 | type | name=passwd | pwdhttp |
      driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 10 | sendKeys | name=passwd | ${KEY_ENTER} |
      driver.findElement(By.name("passwd")).sendKeys(Keys.ENTER);
      // 11 | click | linkText=SYSTEM |  |
      driver.get("https://127.0.0.1:11187/System.html");
      // 12 | click | linkText=START |  |
      driver.get("https://127.0.0.1:11187/index.html");
    } catch (NoSuchElementException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } catch (StaleElementReferenceException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      driver.get("https://127.0.0.1:11187/Logout.html");
    }
  }
}