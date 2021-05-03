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
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.role.RoleDefault.ROLE;
import org.waarp.common.utility.NullPrintStream;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.Version;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.client.TransferArgsTest;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.dao.TransferDAO;
import org.waarp.openr66.dao.database.DBDAOFactory;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.pojo.Transfer;
import org.waarp.openr66.pojo.Transfer.TASKSTEP;
import org.waarp.openr66.pojo.UpdatedInfo;
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
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestNoAuthentTest extends TestAbstract {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(RestNoAuthentTest.class);
  private static final String CONFIG_SERVER_A_MINIMAL_XML =
      "config-serverA-minimal.xml";
  private static final String CONFIG_SERVER_A_MINIMAL_RESPONSIVE_XXML =
      "config-serverA-minimal-Responsive.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML =
      "Linux/config/config-serverInitA.xml";

  private static final int WAIT = 300;
  private static PrintStream err = System.err;

  private static boolean RUN_TEST = true;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ResourceLeakDetector.setLevel(Level.PARANOID);
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
    System.setErr(err);
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
    if (!RUN_TEST) {
      return;
    }
    reloadDriver();
    Thread.sleep(100);
  }

  @Test
  public void testRestR66NoAuthent() throws Exception {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
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
  public void testRestR66NoAuthentFollowId() throws Exception {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    try {
      final String baseUri = "http://localhost:8088/";
      // 2 | type | V2 [  |
      String v2BaseUri = baseUri + "v2/";
      TransferDAO transferDAO = DBDAOFactory.getInstance().getTransferDAO();
      long id = 1;
      String rule = "rule3";
      int mode = 1;
      String filename = "test.txt";
      String originalName = "test.txt";
      String fileInfo = "";
      boolean isMoved = false;
      int blockSize = 8192;
      boolean retrieveMode = false;
      String ownerReq = "hosta";
      String requester = "hosta";
      String requested = "hosta";
      String followId = "12345";
      String transferInfo =
          TransferArgsTest.FOLLOWARGJSON + " " + followId + "}";
      TASKSTEP globalStep = TASKSTEP.ALLDONETASK;
      TASKSTEP lastGlobalStep = TASKSTEP.ALLDONETASK;
      int step = 0;
      ErrorCode stepStatus = ErrorCode.CompleteOk;
      ErrorCode infoStatus = ErrorCode.CompleteOk;
      int rank = 1;
      Timestamp start = new Timestamp(new Date().getTime());
      Timestamp stop = new Timestamp(new Date().getTime() + 100);
      UpdatedInfo updatedInfo = UpdatedInfo.DONE;
      Transfer transfer =
          new Transfer(id, rule, mode, filename, originalName, fileInfo,
                       isMoved, blockSize, retrieveMode, ownerReq, requester,
                       requested, transferInfo, globalStep, lastGlobalStep,
                       step, stepStatus, infoStatus, rank, start, stop,
                       updatedInfo);
      transferDAO.insert(transfer);
      id++;
      start = new Timestamp(new Date().getTime() + 200);
      stop = new Timestamp(new Date().getTime() + 300);
      Transfer transfer2 =
          new Transfer(id, rule, mode, filename, originalName, fileInfo,
                       isMoved, blockSize, retrieveMode, ownerReq, requester,
                       requested, transferInfo, globalStep, lastGlobalStep,
                       step, stepStatus, infoStatus, rank, start, stop,
                       updatedInfo);
      transferDAO.insert(transfer2);
      id++;
      start = new Timestamp(new Date().getTime() + 400);
      stop = new Timestamp(new Date().getTime() + 500);
      Transfer transfer3 =
          new Transfer(id, rule, mode, filename, originalName, fileInfo,
                       isMoved, blockSize, retrieveMode, ownerReq, requester,
                       requested, transferInfo, globalStep, lastGlobalStep,
                       step, stepStatus, infoStatus, rank, start, stop,
                       updatedInfo);
      transferDAO.insert(transfer3);
      id++;
      start = new Timestamp(new Date().getTime() + 400);
      stop = new Timestamp(new Date().getTime() + 500);
      Transfer transfer4 =
          new Transfer(id, rule, mode, filename, originalName, fileInfo,
                       isMoved, blockSize, retrieveMode, ownerReq, requester,
                       requested, "transferInfo", globalStep, lastGlobalStep,
                       step, stepStatus, infoStatus, rank, start, stop,
                       updatedInfo);
      transferDAO.insert(transfer4);

      driver.get(v2BaseUri + "transfers");
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      String page = driver.getPageSource();
      assertTrue(driver.getPageSource().contains("results"));
      int first = page.indexOf(followId);
      assertTrue(first != -1);
      int second = page.indexOf(followId, first + 1);
      assertTrue(second != -1);
      int third = page.indexOf(followId, second + 1);
      assertTrue(third != -1);
      int fourth = page.indexOf(followId, third + 1);
      assertFalse(fourth != -1);
      first = page.indexOf(rule);
      assertTrue(first != -1);
      second = page.indexOf(rule, first + 1);
      assertTrue(second != -1);
      third = page.indexOf(rule, second + 1);
      assertTrue(third != -1);
      fourth = page.indexOf(rule, third + 1);
      assertTrue(fourth != -1);

      // Now with args
      driver.get(v2BaseUri + "transfers?followId=" + followId);
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      page = driver.getPageSource();
      assertTrue(driver.getPageSource().contains("results"));
      first = page.indexOf(followId);
      assertTrue(first != -1);
      second = page.indexOf(followId, first + 1);
      assertTrue(second != -1);
      third = page.indexOf(followId, second + 1);
      assertTrue(third != -1);
      fourth = page.indexOf(followId, third + 1);
      assertFalse(fourth != -1);
      first = page.indexOf(rule);
      assertTrue(first != -1);
      second = page.indexOf(rule, first + 1);
      assertTrue(second != -1);
      third = page.indexOf(rule, second + 1);
      assertTrue(third != -1);
      fourth = page.indexOf(rule, third + 1);
      assertFalse(fourth != -1);

      // Now directly
      DbTaskRunner[] taskRunners =
          DbTaskRunner.getSelectSameFollowId(followId, true, 20);
      assertEquals(3, taskRunners.length);
      taskRunners = DbTaskRunner.getSelectSameFollowId(followId, false, 20);
      assertEquals(3, taskRunners.length);
      taskRunners = DbTaskRunner.getSelectSameFollowId(followId, true, 2);
      assertEquals(2, taskRunners.length);

      // Now with args
      driver.get(v2BaseUri + "transfers/" + taskRunners[0].getSpecialId());
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      page = driver.getPageSource();
      assertTrue(driver.getPageSource().contains(taskRunners[0].getFollowId()));

      // Now with wrong args
      driver.get(v2BaseUri + "transfers?followId=125");
      SysErrLogger.FAKE_LOGGER.sysout(driver.getCurrentUrl());
      page = driver.getPageSource();
      assertTrue(driver.getPageSource().contains("results"));
      first = page.indexOf(followId);
      assertFalse(first != -1);
      first = page.indexOf(rule);
      assertFalse(first != -1);

      // Now directly
      taskRunners = DbTaskRunner.getSelectSameFollowId("125", true, 20);
      assertEquals(0, taskRunners.length);
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
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    HttpTestRestR66Client.keydesfilename =
        new File(dirResources, "certs/test-key.des").getAbsolutePath();
    logger.info("Key filename: {}", HttpTestRestR66Client.keydesfilename);

    HttpTestRestR66Client.main(new String[] { "1" });
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
  }


  @Test
  public void testAsRestV2Role() {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
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
