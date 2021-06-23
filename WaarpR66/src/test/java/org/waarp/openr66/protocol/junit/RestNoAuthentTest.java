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
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
import java.io.IOException;
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

  private CloseableHttpClient httpClient = null;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    ResourceLeakDetector.setLevel(Level.PARANOID);
    System.setErr(new NullPrintStream());
    final ClassLoader classLoader = NetworkClientTest.class.getClassLoader();
    final File file =
        new File(classLoader.getResource("logback-test.xml").getFile());
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
    Thread.sleep(100);
  }

  private void createHttpClient() {
    if (httpClient != null) {
      try {
        httpClient.close();
      } catch (IOException e) {
        // ignore
      }
    }
    httpClient = HttpClientBuilder.create().setConnectionManagerShared(true)
                                  .disableAutomaticRetries().build();
  }

  private String restAccess(String baseuri, String url) {
    HttpGet request = new HttpGet(baseuri + url);
    CloseableHttpResponse response = null;
    try {
      response = httpClient.execute(request);
      SysErrLogger.FAKE_LOGGER.sysout(
          baseuri + url + " = " + response.getStatusLine().getStatusCode());
      String result = EntityUtils.toString(response.getEntity());
      if (response.getStatusLine().getStatusCode() >= 400) {
        createHttpClient();
        logger.warn("Error message: {}", result);
      }
      return result;
    } catch (ClientProtocolException e) {
      fail("Error on: " + baseuri + url + " = " + e.getMessage());
    } catch (IOException e) {
      fail("Error on: " + baseuri + url + " = " + e.getMessage());
    } finally {
      if (response != null) {
        try {
          response.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }

  @Test
  public void testRestR66NoAuthent() throws Exception {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    try {
      createHttpClient();
      // Test Rest V1
      // Step # | name | target | value | comment
      // 1 | open | V1 |  |
      final String baseUri = "http://localhost:8088/";
      String page =
          restAccess(baseUri, DbTaskRunnerR66RestMethodHandler.BASEURI);
      assertTrue(page.contains("answer"));
      page = restAccess(baseUri, DbConfigurationR66RestMethodHandler.BASEURI);
      assertTrue(page.contains("answer"));
      page = restAccess(baseUri, DbHostAuthR66RestMethodHandler.BASEURI);
      assertTrue(page.contains("answer"));
      page =
          restAccess(baseUri, DbHostConfigurationR66RestMethodHandler.BASEURI);
      assertTrue(page.contains("answer"));
      page = restAccess(baseUri, DbRuleR66RestMethodHandler.BASEURI);
      assertTrue(page.contains("answer"));
      page = restAccess(baseUri, HttpRestBandwidthR66Handler.BASEURI);
      assertTrue(page.contains("answer"));
      page = restAccess(baseUri, HttpRestConfigR66Handler.BASEURI);
      // Complex command
      assertTrue(page.contains("Bad Request"));
      page = restAccess(baseUri, HttpRestControlR66Handler.BASEURI);
      // Complex command
      assertTrue(page.contains("Bad Request"));
      page = restAccess(baseUri, HttpRestInformationR66Handler.BASEURI);
      // Complex command
      assertTrue(page.contains("Bad Request"));
      page = restAccess(baseUri, HttpRestLogR66Handler.BASEURI);
      // Complex command
      assertTrue(page.contains("Bad Request"));
      page = restAccess(baseUri, HttpRestServerR66Handler.BASEURI);
      assertTrue(page.contains("results"));

      // 2 | type | V2 [  |
      String v2BaseUri = baseUri + "v2/";
      page = restAccess(v2BaseUri, "transfers");
      assertTrue(page.contains("results"));
      page = restAccess(v2BaseUri, "hostconfig");
      assertTrue(page.contains("business"));
      assertTrue(page.contains(Version.fullIdentifier()));
      assertTrue(page.contains(org.waarp.openr66.protocol.utils.Version.ID));
      page = restAccess(v2BaseUri, "hosts");
      assertTrue(page.contains("results"));
      page = restAccess(v2BaseUri, "rules");
      assertTrue(page.contains("results"));
      page = restAccess(v2BaseUri, "filemonitors");
      assertTrue(page.contains("results"));
      page = restAccess(v2BaseUri, "limits");
      // Not found assertTrue(page.contains("results"))
      page = restAccess(v2BaseUri, "server/status");
      assertTrue(page.contains("serverName"));

      page = restAccess(v2BaseUri, "transfers?countOrder=true");
      assertTrue(page.contains("totalResults"));
      assertFalse(page.contains("results"));

      page = restAccess(v2BaseUri, "hosts?countOrder=true");
      assertTrue(page.contains("totalResults"));
      assertFalse(page.contains("results"));

      page = restAccess(v2BaseUri, "rules?countOrder=true");
      assertTrue(page.contains("totalResults"));
      assertFalse(page.contains("results"));

      page = restAccess(v2BaseUri, "filemonitors?countOrder=true");
      assertTrue(page.contains("totalResults"));
      assertFalse(page.contains("results"));
    } catch (NoSuchElementException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } catch (StaleElementReferenceException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      httpClient.close();
      httpClient = null;
    }
  }

  @Test
  public void testRestR66NoAuthentFollowId() throws Exception {
    Assume.assumeTrue("Driver not loaded", RUN_TEST);
    try {
      createHttpClient();
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
      String ownerReq = "test";
      String requester = "test";
      String requested = "hosta";
      String followId = "12345";
      String transferInfo = TransferArgsTest.FOLLOWARGJSON + followId + "}";
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

      String page = restAccess(v2BaseUri, "transfers");
      assertTrue(page.contains("results"));
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
      page = restAccess(v2BaseUri, "transfers?followId=" + followId);
      assertTrue(page.contains("results"));
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
          DbTaskRunner.getSelectSameFollowId(followId, true, 20, true);
      assertEquals(3, taskRunners.length);
      taskRunners =
          DbTaskRunner.getSelectSameFollowId(followId, false, 20, true);
      assertEquals(3, taskRunners.length);
      taskRunners = DbTaskRunner.getSelectSameFollowId(followId, true, 2, true);
      assertEquals(2, taskRunners.length);

      // Now with args
      page = restAccess(v2BaseUri,
                        "transfers/" + taskRunners[0].getSpecialId() + "_" +
                        requested);
      assertTrue(page.contains(taskRunners[0].getFollowId()));

      // Now with wrong args
      page = restAccess(v2BaseUri, "transfers?followId=125");
      assertTrue(page.contains("results"));
      first = page.indexOf(followId);
      assertFalse(first != -1);
      first = page.indexOf(rule);
      assertFalse(first != -1);

      // Now directly
      taskRunners = DbTaskRunner.getSelectSameFollowId("125", true, 20, true);
      assertEquals(0, taskRunners.length);

      // Using count
      page = restAccess(v2BaseUri, "transfers?countOrder=true");
      assertTrue(page.contains("totalResults\":4"));

      page = restAccess(v2BaseUri,
                        "transfers?countOrder=true&followId=" + followId);
      assertTrue(page.contains("totalResults\":3"));

    } catch (NoSuchElementException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } catch (StaleElementReferenceException e) {
      e.printStackTrace();
      fail(e.getMessage());
    } finally {
      httpClient.close();
      httpClient = null;
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
