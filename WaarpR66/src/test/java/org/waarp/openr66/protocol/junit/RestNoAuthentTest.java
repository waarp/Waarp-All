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
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.common.utility.Version;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.http.rest.handler.DbConfigurationR66RestMethodHandler;
import org.waarp.openr66.protocol.http.rest.handler.DbHostAuthR66RestMethodHandler;
import org.waarp.openr66.protocol.http.rest.handler.DbHostConfigurationR66RestMethodHandler;
import org.waarp.openr66.protocol.http.rest.handler.DbRuleR66RestMethodHandler;
import org.waarp.openr66.protocol.http.rest.handler.DbTaskRunnerR66RestMethodHandler;
import org.waarp.openr66.protocol.http.rest.handler.HttpRestBandwidthR66Handler;
import org.waarp.openr66.protocol.http.rest.handler.HttpRestConfigR66Handler;
import org.waarp.openr66.protocol.http.rest.handler.HttpRestControlR66Handler;
import org.waarp.openr66.protocol.http.rest.handler.HttpRestInformationR66Handler;
import org.waarp.openr66.protocol.http.rest.handler.HttpRestLogR66Handler;
import org.waarp.openr66.protocol.http.rest.handler.HttpRestServerR66Handler;
import org.waarp.openr66.protocol.http.rest.test.HttpTestRestR66Client;
import org.waarp.openr66.protocol.http.restv2.converters.HostConfigConverter;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.HostConfigHandler;
import org.waarp.openr66.protocol.http.restv2.dbhandlers.RequiredRole;
import org.waarp.openr66.protocol.junit.NetworkClientTest.RestHandlerHookForTest;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestNoAuthentTest extends TestAbstract {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(RestNoAuthentTest.class);
  private static final String CONFIG_SERVER_A_MINIMAL_XML =
      "config-serverA-minimal.xml";
  private static final String CONFIG_SERVER_A_MINIMAL_RESPONSIVE_XXML =
      "config-serverA-minimal-Responsive.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML =
      "Linux/config/config-serverInitA.xml";

  private static final int WAIT = 300;

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
  public void testRestR66NoAuthent() throws Exception {
    try {
      // Test Rest V1
      // Step # | name | target | value | comment
      // 1 | open | V1 |  |
      final String baseUri = "http://localhost:8088/";
      driver.get(baseUri + DbTaskRunnerR66RestMethodHandler.BASEURI);
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      assertTrue(driver.getPageSource().contains("answer"));
      driver.get(baseUri + DbConfigurationR66RestMethodHandler.BASEURI);
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      assertTrue(driver.getPageSource().contains("answer"));
      driver.get(baseUri + DbHostAuthR66RestMethodHandler.BASEURI);
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      assertTrue(driver.getPageSource().contains("answer"));
      driver.get(baseUri + DbHostConfigurationR66RestMethodHandler.BASEURI);
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      assertTrue(driver.getPageSource().contains("answer"));
      driver.get(baseUri + DbRuleR66RestMethodHandler.BASEURI);
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      assertTrue(driver.getPageSource().contains("answer"));
      driver.get(baseUri + HttpRestBandwidthR66Handler.BASEURI);
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      assertTrue(driver.getPageSource().contains("answer"));
      driver.get(baseUri + HttpRestConfigR66Handler.BASEURI);
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      SysErrLogger.FAKE_LOGGER.syserr(driver.getPageSource());
      // Complex command
      assertTrue(driver.getPageSource().contains("Bad Request"));
      driver.get(baseUri + HttpRestControlR66Handler.BASEURI);
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      SysErrLogger.FAKE_LOGGER.syserr(driver.getPageSource());
      // Complex command
      assertTrue(driver.getPageSource().contains("Bad Request"));
      driver.get(baseUri + HttpRestInformationR66Handler.BASEURI);
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      SysErrLogger.FAKE_LOGGER.syserr(driver.getPageSource());
      // Complex command
      assertTrue(driver.getPageSource().contains("Bad Request"));
      driver.get(baseUri + HttpRestLogR66Handler.BASEURI);
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      SysErrLogger.FAKE_LOGGER.syserr(driver.getPageSource());
      // Complex command
      assertTrue(driver.getPageSource().contains("Bad Request"));
      driver.get(baseUri + HttpRestServerR66Handler.BASEURI);
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      assertTrue(driver.getPageSource().contains("results"));

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
      // Nothing
    }
  }

  @Test
  public void testRestR66NoAuthentExternal() throws Exception {
    HttpTestRestR66Client.keydesfilename =
        new File(dirResources, "certs/test-key.des").getAbsolutePath();
    logger.info("Key filename: {}", HttpTestRestR66Client.keydesfilename);
    HttpTestRestR66Client.main(new String[] { "1" });
  }


  @Test
  public void testAsRestV2Role() {
    List<ROLE> roles = HostConfigConverter.getRoles("hosta");
    assertTrue(roles.contains(ROLE.CONFIGADMIN));
    assertTrue(roles.contains(ROLE.SYSTEM));
    boolean isConfigAdmin = false;
    for (ROLE role : roles) {
      isConfigAdmin |= role.contains(ROLE.CONFIGADMIN);
    }
    assertTrue(isConfigAdmin);
    roles = HostConfigConverter.getRoles("hostas");
    assertTrue(roles.contains(ROLE.FULLADMIN));
    isConfigAdmin = false;
    for (ROLE role : roles) {
      isConfigAdmin |= role.contains(ROLE.CONFIGADMIN);
    }
    assertTrue(isConfigAdmin);
    roles = HostConfigConverter.getRoles("hostb");
    assertTrue(roles.contains(ROLE.PARTNER));
    assertTrue(roles.contains(ROLE.RULE));
    assertTrue(roles.contains(ROLE.HOST));
    assertTrue(roles.contains(ROLE.SYSTEM));
    isConfigAdmin = false;
    for (ROLE role : roles) {
      isConfigAdmin |= role.contains(ROLE.CONFIGADMIN);
    }
    assertTrue(isConfigAdmin);
    roles = HostConfigConverter.getRoles("test");
    assertTrue(roles.contains(ROLE.READONLY));
    assertTrue(roles.contains(ROLE.TRANSFER));
    assertTrue(roles.contains(ROLE.LOGCONTROL));
    isConfigAdmin = false;
    for (ROLE role : roles) {
      isConfigAdmin |= role.contains(ROLE.CONFIGADMIN);
    }
    assertFalse(isConfigAdmin);

    HostConfigHandler hostConfigHandler = new HostConfigHandler((byte) 15);
    Class clasz = hostConfigHandler.getClass();
    Method[] methods = clasz.getMethods();
    RestHandlerHookForTest hook = new RestHandlerHookForTest(true, null, 0);
    for (Method method : methods) {
      if (method.isAnnotationPresent(RequiredRole.class)) {
        boolean isAllowed = hook.testcheckAuthorization("hostas", method);
        SysErrLogger.FAKE_LOGGER.sysout(method.getName() + ":" + isAllowed);
        assertTrue(isAllowed);
      }
    }
    for (Method method : methods) {
      if (method.isAnnotationPresent(RequiredRole.class)) {
        boolean isAllowed = hook.testcheckAuthorization("hostb", method);
        SysErrLogger.FAKE_LOGGER.sysout(method.getName() + ":" + isAllowed);
        assertTrue(isAllowed);
      }
    }
    for (Method method : methods) {
      if (method.isAnnotationPresent(RequiredRole.class)) {
        boolean isAllowed = hook.testcheckAuthorization("test", method);
        SysErrLogger.FAKE_LOGGER.sysout(method.getName() + ":" + isAllowed);
        if (method.getName().equals("options") ||
            method.getName().equals("getConfig")) {
          assertTrue(isAllowed);
        } else {
          assertFalse(isAllowed);
        }
      }
    }
  }

}
