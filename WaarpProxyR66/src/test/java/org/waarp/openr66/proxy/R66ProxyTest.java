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
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.waarp.openr66.client.DirectTransfer;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.protocol.utils.R66Future;

import static org.junit.Assert.*;

public class R66ProxyTest extends CommonUtil {

  @BeforeClass
  public static void launchConfig() throws Exception {
    CONFIG_PROXY_XML = "config-proxy.xml";
    launchServers();
  }

  @Test
  public void r66ProxyNoTlsNoTls() throws Exception {
    testShouldFailed = false;
    // Using a client NoDB
    r66Send(false, false);
    r66Recv(false, false);
  }

  @Test
  public void r66ProxyTlsTls() throws Exception {
    testShouldFailed = false;
    // Using a client NoDB
    r66Send(true, true);
    r66Recv(true, true);
  }

  @Test
  @Ignore("Test failed due to bad configuration on remote host name")
  public void r66ProxyNoTlsTls() throws Exception {
    testShouldFailed = true;
    // Using a client NoDB
    r66Send(false, true);
    r66Recv(false, true);
  }

  @Test
  @Ignore("Test failed due to bad configuration on remote host name")
  public void r66ProxyTlsNoTls() throws Exception {
    testShouldFailed = true;
    // Using a client NoDB
    r66Send(true, false);
    r66Recv(true, false);
  }

  @Test
  public void test98_Http() throws InterruptedException {
    // Test name: TestMonitorSimple
    try {
      // Step # | name | target | value | comment
      // 1 | open | / |  |
      driver.get("http://127.0.0.1:10086/");
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
  public void test98_Https() throws InterruptedException {
    try {
      // Test name: ProxyAdmin
      // Step # | name | target | value | comment
      // 1 | open | / |  |
      driver.get("https://127.0.0.1:10087/");
      // 7 | click | name=name |  |
      driver.findElement(By.name("name")).click();
      // 8 | type | name=name | monadmin |
      driver.findElement(By.name("name")).sendKeys("monadmin");
      // 9 | type | name=passwd | pwdhttp |
      driver.findElement(By.name("passwd")).sendKeys("pwdhttp");
      // 10 | sendKeys | name=passwd | ${KEY_ENTER} |
      driver.findElement(By.name("passwd")).sendKeys(Keys.ENTER);
      // 11 | click | linkText=SYSTEM |  |
      driver.get("https://127.0.0.1:10087/System.html");
      // 12 | click | linkText=START |  |
      driver.get("https://127.0.0.1:10087/index.html");
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

  public void r66Send(boolean fromTls, boolean toTls) {
    logger.warn("r66send {} {}", fromTls, toTls);
    R66Future future = new R66Future(true);
    String remoteHost;
    if (fromTls) {
      remoteHost = "hostas";
    } else {
      remoteHost = "hosta";
    }

    String filename = toSend.getAbsolutePath();
    String rulename = "rule3";
    String fileInfo = "file Info";
    boolean isMD5 = true;
    int blocksize = 1024;
    long id = DbConstantR66.ILLEGALVALUE;
    DirectTransfer directTransfer =
        new DirectTransfer(future, remoteHost, filename, rulename, fileInfo,
                           isMD5, blocksize, id, networkTransaction);
    directTransfer.run();
    future.awaitOrInterruptible();
    assertEquals(!testShouldFailed, future.isSuccess());
  }

  public void r66Recv(boolean fromTls, boolean toTls) {
    logger.warn("r66recv {} {}", fromTls, toTls);
    R66Future future = new R66Future(true);
    String remoteHost;
    if (fromTls) {
      remoteHost = toTls? "hostas" : "hostas2nossl";
    } else {
      remoteHost = toTls? "hosta2ssl" : "hosta";
    }

    String filename = toRecv.getAbsolutePath();
    String rulename = "rule4";
    String fileInfo = "file Info";
    boolean isMD5 = true;
    int blocksize = 1024;
    long id = DbConstantR66.ILLEGALVALUE;
    DirectTransfer directTransfer =
        new DirectTransfer(future, remoteHost, filename, rulename, fileInfo,
                           isMD5, blocksize, id, networkTransaction);
    directTransfer.run();
    future.awaitOrInterruptible();
    assertEquals(!testShouldFailed, future.isSuccess());
  }
}