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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.exec.ExecuteException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runners.MethodSorters;
import org.waarp.common.file.FileUtils;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.utility.Processes;
import org.waarp.common.utility.SystemPropertyUtil;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.client.SpooledDirectoryTransfer;
import org.waarp.openr66.client.SpooledDirectoryTransfer.Arguments;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.junit.TestAbstract;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.R66Future;
import org.waarp.openr66.server.R66Server;
import org.waarp.openr66.server.ServerInitDatabase;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.waarp.openr66.protocol.it.ScenarioBase.*;

/**
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class SpooledITAbstract extends TestAbstract {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  static final String CONFIG_SERVER_A_MINIMAL_XML =
      "config-serverA-minimal.xml";
  static final String LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML =
      "Linux/config/config-serverInitA.xml";
  static final String LINUX_CONFIG_CONFIG_SERVER_INIT_B_XML =
      "Linux/config/config-serverInitB.xml";
  static final String CONFIG_CLIENT_A_XML = "config-clientA.xml";
  static final String CONFIG_CLIENT_A_NODB_XML = "config-clientANoDb.xml";

  private static final int MAX_NB =
      SystemPropertyUtil.get(IT_LONG_TEST, false)? 1000 : 100;
  private static int currentNb = 0;
  protected static boolean noDb;
  private static final List<Integer> PIDS = new ArrayList<Integer>();
  static int r66Pid1 = 999999;

  /**
   * @throws Exception
   */
  public static void setUpBeforeClassServer(String serverInit)
      throws Exception {

    if (Configuration.configuration != null &&
        Configuration.configuration.isConfigured()) {
      // Ensure we redo configuration
      Configuration.configuration.setConfigured(false);
      Configuration.configuration = new Configuration();
    }
    final ClassLoader classLoader = TestAbstract.class.getClassLoader();
    final File file;
    if (serverInit.startsWith("/")) {
      file = new File(serverInit);
    } else {
      file = new File(classLoader.getResource(serverInit).getFile());
    }
    if (file.exists()) {
      dir = file.getParentFile();
      System.err.println("Find serverInit file: " + file.getAbsolutePath());
      final String[] argsServer = {
          file.getAbsolutePath(), "-initdb", "-dir", dir.getAbsolutePath(),
          "-auth", new File(dir, "OpenR66-authent-A.xml").getAbsolutePath(),
          "-limit", new File(dir, "limitConfiga.xml").getAbsolutePath()
      };
      Processes.executeJvm(project, ServerInitDatabase.class, argsServer,
                           false);
      logger.warn("Init Done");
    } else {
      System.err.println(
          "Cannot find serverInit file: " + file.getAbsolutePath());
    }
  }

  public static int startServer(String serverConfig) throws Exception {
    final File file2;
    if (serverConfig.charAt(0) == '/') {
      file2 = new File(serverConfig);
    } else {
      file2 = new File(dirResources, serverConfig);
    }
    if (file2.exists()) {
      System.err.println("Find server file: " + file2.getAbsolutePath());
      final String[] argsServer = {
          file2.getAbsolutePath()
      };
      // global ant project settings
      project = Processes.getProject(homeDir);
      Processes.executeJvm(project, R66Server.class, argsServer, true);
      int pid = Processes.getPidOfRunnerCommandLinux("java",
                                                     R66Server.class.getName(),
                                                     PIDS);
      PIDS.add(pid);
      logger.warn("Start Done: {}", pid);
      Thread.sleep(1000);
      return pid;
    } else {
      System.err.println("Cannot find server file: " + file2.getAbsolutePath());
      fail("Cannot find server file");
      return 999999;
    }
  }

  /**
   * @throws Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    WaarpSystemUtil.stopLogger(true);
    for (int pid : PIDS) {
      Processes.kill(pid, true);
    }
    Thread.sleep(100);

    tearDownAfterClassClient();
    tearDownAfterClassMinimal();
    tearDownAfterClassServer();
  }

  private void test_Spooled(final SpooledThread spooledThread, final int nb)
      throws IOException, InterruptedException {
    final int size = 200;
    Configuration.configuration.changeNetworkLimit(0, 0, 0, 0, 1000);
    File directory = new File(SpooledThread.TMP_R_66_TEST_OUT_EXAMPLE);
    directory.mkdirs();
    File stop = new File(SpooledThread.TMP_R_66_TEST_STOPOUT_TXT);
    stop.delete();
    final R66Future future = new R66Future(true);
    spooledThread.future = future;
    spooledThread.start();
    logger.warn("Start IT test");
    for (int i = 0; i < nb; i++) {
      final File totestBig = generateOutFile(
          SpooledThread.TMP_R_66_TEST_OUT_EXAMPLE + "/testTaskBig_" + i +
          ".txt", size);
      Thread.sleep(50);
    }
    logger.warn("Files created {}", nb);
    while (spooledThread.spooledDirectoryTransfer.getSent() < nb) {
      Thread.sleep(100);
    }
    currentNb += nb;
    checkThroughRestV2(nb, currentNb);
    generateOutFile(stop.getAbsolutePath(), 10);
    future.awaitOrInterruptible();
    assertTrue(future.isSuccess());
    stop.delete();
    File all = new File(SpooledThread.SPOOLED_ROOT);
    FileUtils.forceDeleteRecursiveDir(all);
    logger.warn("Launched {}",
                spooledThread.spooledDirectoryTransfer.getSent());
    logger.warn("Error {}", spooledThread.spooledDirectoryTransfer.getError());
  }

  private void checkThroughRestV2(final int nbSpooled, final int finalNb)
      throws IOException, InterruptedException {
    CloseableHttpClient httpClient = null;
    try {
      httpClient = HttpClientBuilder.create().setConnectionManagerShared(true)
                                    .disableAutomaticRetries().build();
      HttpGet request = new HttpGet(
          "http://127.0.0.1:8088/v2/filemonitors?name=SpooledClient&countOrder=true");
      CloseableHttpResponse response = null;
      int nb = 0;
      int every10sec = 10;
      int max = nbSpooled;
      while (nb < max) {
        try {
          response = httpClient.execute(request);
          if (400 <= response.getStatusLine().getStatusCode()) {
            break;
          }
          String content = EntityUtils.toString(response.getEntity());
          ObjectNode node = JsonHandler.getFromString(content);
          if (node != null) {
            JsonNode number = node.findValue("totalSubResults");
            if (number != null) {
              int newNb = number.asInt();
              if (newNb != nb || every10sec == 0) {
                every10sec = 10;
                nb = newNb;
                logger.warn("Found {} spooled files vs {}", nb, max);
              }
            }
            every10sec--;
          }
        } finally {
          if (response != null) {
            response.close();
          }
        }
        Thread.sleep(1000);
      }
      logger.warn("Final found {} spooled files", nb);
      request = new HttpGet(
          "http://127.0.0.1:8088/v2/transfers?status=DONE&countOrder=true");
      HttpGet request2 = new HttpGet(
          "http://127.0.0.1:8088/v2/transfers?status=INERROR&countOrder=true");

      response = null;
      nb = 0;
      every10sec = 10;
      int nb2 = 0;
      int totalNb = 0;
      max = finalNb;
      while (totalNb < max) {
        try {
          response = httpClient.execute(request);
          if (400 <= response.getStatusLine().getStatusCode()) {
            break;
          }
          String content = EntityUtils.toString(response.getEntity());
          ObjectNode node = JsonHandler.getFromString(content);
          if (node != null) {
            JsonNode number = node.findValue("totalResults");
            if (number != null) {
              long newNb = number.asLong();
              if (newNb != nb || every10sec == 0) {
                every10sec = 10;
                nb = (int) newNb;
                totalNb = nb + nb2;
                logger.warn("Found {} Transfers vs {}", nb, max);
              }
            }
          }
        } finally {
          if (response != null) {
            response.close();
          }
        }
        try {
          response = httpClient.execute(request2);
          if (400 <= response.getStatusLine().getStatusCode()) {
            break;
          }
          String content = EntityUtils.toString(response.getEntity());
          ObjectNode node = JsonHandler.getFromString(content);
          if (node != null) {
            JsonNode number = node.findValue("totalResults");
            if (number != null) {
              long newNb = number.asLong();
              if (newNb != nb2 || every10sec == 0) {
                every10sec = 10;
                nb2 = (int) newNb;
                totalNb = nb + nb2;
                logger.warn("Found {} Transfers in Error vs {}", nb2, max);
              }
            }
          }
        } finally {
          if (response != null) {
            response.close();
          }
        }
        every10sec--;
        Thread.sleep(1000);
      }
      logger.warn("Final found {} transfers", totalNb);
    } catch (ExecuteException e) {
      // ignore
    } finally {
      if (httpClient != null) {
        httpClient.close();
      }
    }
  }

  @Test
  public void test7_Spooled() throws IOException, InterruptedException {
    logger.warn("Start Test of Spooled Transfer");
    SpooledThread spooledThread = new SpooledThread();
    spooledThread.ignoreAlreadyUsed = false;
    spooledThread.submit = false;
    test_Spooled(spooledThread, MAX_NB);
    assertEquals(MAX_NB, spooledThread.spooledDirectoryTransfer.getSent());
    assertEquals(0, spooledThread.spooledDirectoryTransfer.getError());
  }

  @Test
  public void test7_SpooledSubmit() throws IOException, InterruptedException {
    logger.warn("Start Test of Spooled Transfer");
    if (noDb) {
      logger.warn("No DB so no Submit");
      return;
    }
    SpooledThread spooledThread = new SpooledThread();
    spooledThread.ignoreAlreadyUsed = false;
    spooledThread.submit = true;
    test_Spooled(spooledThread, MAX_NB);
    assertEquals(MAX_NB, spooledThread.spooledDirectoryTransfer.getSent());
    assertEquals(0, spooledThread.spooledDirectoryTransfer.getError());
  }

  @Test
  public void test7_SpooledIgnore() throws IOException, InterruptedException {
    logger.warn("Start Test of Spooled Ignored Changed File Transfer");
    SpooledThread spooledThread = new SpooledThread();
    spooledThread.ignoreAlreadyUsed = true;
    spooledThread.submit = false;
    test_Spooled(spooledThread, MAX_NB);
    assertEquals(MAX_NB, spooledThread.spooledDirectoryTransfer.getSent());
    assertEquals(0, spooledThread.spooledDirectoryTransfer.getError());
  }

  @Test
  public void test7_SpooledSubmitIgnore()
      throws IOException, InterruptedException {
    logger.warn("Start Test of Spooled Ignored Changed File Transfer");
    if (noDb) {
      logger.warn("No DB so no Submit");
      return;
    }
    SpooledThread spooledThread = new SpooledThread();
    spooledThread.ignoreAlreadyUsed = true;
    spooledThread.submit = true;
    test_Spooled(spooledThread, MAX_NB);
    assertEquals(MAX_NB, spooledThread.spooledDirectoryTransfer.getSent());
    assertEquals(0, spooledThread.spooledDirectoryTransfer.getError());
  }

  private static class SpooledDirectoryTransferTest
      extends SpooledDirectoryTransfer {

    /**
     * @param future
     * @param arguments
     * @param networkTransaction
     */
    public SpooledDirectoryTransferTest(final R66Future future,
                                        final Arguments arguments,
                                        final NetworkTransaction networkTransaction) {
      super(future, arguments, networkTransaction);
    }

    public static boolean getTestParams(final String[] args) {
      arguments.clear();
      return getParams(args);
    }

    public static List<Arguments> getArguments() {
      return arguments;
    }
  }

  private static class SpooledThread extends Thread {
    private static final String TMP_R_66_TEST_OUT_EXAMPLE =
        "/tmp/R66/test/out/example";
    private static final String TMP_R_66_TEST_STOPOUT_TXT =
        "/tmp/R66/test/stopout.txt";
    private static final String SPOOLED_ROOT = "/tmp/R66/test";
    R66Future future;
    private boolean ignoreAlreadyUsed = false;
    private boolean submit = false;
    private String host = "hostas";
    private final String waarpHost = "hostas";
    private SpooledDirectoryTransferTest spooledDirectoryTransfer;

    @Override
    public void run() {
      File file =
          new File(dir, noDb? CONFIG_CLIENT_A_NODB_XML : CONFIG_CLIENT_A_XML);
      String[] args = {
          file.getAbsolutePath(), "-to", host, "-name", "SpooledClient",
          "-directory", TMP_R_66_TEST_OUT_EXAMPLE, "-rule", "rule3del",
          "-statusfile", "/tmp/R66/test/statusoutdirect1.json", "-stopfile",
          TMP_R_66_TEST_STOPOUT_TXT, "-info", "Spooled Info", "-md5", "-block",
          "65536", submit? "-submit" : "-direct", "-waarp", waarpHost,
          "-elapse", "500", "-elapseWaarp", "1000", "-minimalSize", "100",
          "-limitParallel", "1", "-notlogWarn",
          ignoreAlreadyUsed? "-ignoreAlreadyUsed" : "-noarg"
      };
      SpooledDirectoryTransferTest.getTestParams(args);
      Configuration.configuration.setTimeoutCon(100);

      Arguments arguments = SpooledDirectoryTransferTest.getArguments().get(0);

      spooledDirectoryTransfer =
          new SpooledDirectoryTransferTest(future, arguments,
                                           networkTransaction);
      spooledDirectoryTransfer.run();
    }
  }
}
