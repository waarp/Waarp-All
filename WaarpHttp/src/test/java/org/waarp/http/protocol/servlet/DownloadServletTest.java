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

package org.waarp.http.protocol.servlet;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hamcrest.MatcherAssert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runners.MethodSorters;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DownloadServletTest extends TestAbstract {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(DownloadServletTest.class);

  private static final String CONFIG_SERVER_A_MINIMAL_XML =
      "config-serverA-minimal.xml";
  private static final String LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML =
      "config/config-serverInitA.xml";
  private static final String CONFIG_CLIENT_A_XML = "config-clientA.xml";
  private static final String CONFIG_SERVER_A_MINIMAL_RESPONSIVE_XXML =
      "config-serverA-minimal-Responsive.xml";
  private static final String KEY = "ef99dfce1fdaa787cef4701368f79cfb";
  private static Server server = null;
  private static URI serverUri = null;
  private final String USER_AGENT = "Mozilla/5.0";

  @BeforeClass
  public static void startJetty() throws Exception {
    ResourceLeakDetector.setLevel(Level.PARANOID);
    WaarpLoggerFactory.setLogLevel(WaarpLogLevel.INFO);
    final ClassLoader classLoader = DownloadServletTest.class.getClassLoader();
    URL url = classLoader.getResource("config/logback.xml");
    final File file = new File(url.getFile());
    final File testDir = new File(
        file.getParentFile().getParentFile().getParentFile().getParentFile(),
        "src/test");
    logger.warn("DIR: {}", testDir.getAbsolutePath());
    setUpBeforeClassMinimal(LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML);
    setUpDbBeforeClass();
    setUpBeforeClassServer(LINUX_CONFIG_CONFIG_SERVER_INIT_A_XML,
                           CONFIG_SERVER_A_MINIMAL_XML, false);

    // Create Server
    WaarpSystemUtil.setJunit(false);
    server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setReuseAddress(true);
    connector.setIdleTimeout(1000 * 60);
    connector.setPort(0); // auto-bind to available port
    server.addConnector(connector);
    WebAppContext webAppContext = new WebAppContext();
    webAppContext.setDescriptor(
        new File(testDir, "webapp/WEB-INF/web.xml").getAbsolutePath());
    webAppContext.setResourceBase(
        new File(testDir, "webapp").getAbsolutePath());
    webAppContext.setContextPath("/WaarpHttp");
    webAppContext.setParentLoaderPriority(true);

    server.setHandler(webAppContext);
    server.setStopAtShutdown(true);

    WaarpLoggerFactory.setLogLevel(WaarpLogLevel.INFO);
    // Start Server
    server.start();

    // Determine Base URI for Server
    String host = connector.getHost();
    if (host == null) {
      host = "localhost";
    }
    int port = connector.getLocalPort();
    serverUri = new URI(String.format("http://%s:%d/WaarpHttp", host, port));
    logger.warn("URI = {}", serverUri.toString());
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      // Wait for server started
    }
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
  }

  @AfterClass
  public static void stopJetty() throws Exception {
    WaarpSystemUtil.stopLogger(true);
    try {
      if (server != null) {
        server.stop();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    tearDownAfterClass();
  }

  public static void tearDownAfterClass() throws Exception {
    Thread.sleep(100);
    final DbHostAuth host = new DbHostAuth("hostas");
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      return;
    }
    final byte scode = -1;

    // Shutdown server
    logger.warn("Shutdown Server");
    Configuration.configuration.setTimeoutCon(100);
    tearDownAfterClassClient();
    Configuration.configuration.setTimeoutCon(100);
    ChannelUtils.exit();
  }

  @Test
  public void test_empty() throws WaarpDatabaseException {
    DbHostAuth[] auths = DbHostAuth.getAllHosts();
    for (DbHostAuth auth : auths) {
      byte[] bytes = FilesystemBasedDigest.passwdCrypt(auth.getHostkey());
      logger.warn("{} {} {}", auth.getHostid(), bytes,
                  FilesystemBasedDigest.getHex(bytes));
    }
  }

  @Test
  public void test00InvalidGet() throws IOException {
    // Test GET
    HttpURLConnection http =
        (HttpURLConnection) serverUri.resolve("/").toURL().openConnection();
    http.setRequestMethod("GET");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", WaarpStringUtils.UTF_8);
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);

    try {
      MatcherAssert.assertThat("Response Code", http.getResponseCode(),
                               is(HttpStatus.NOT_FOUND_404));
    } catch (IOException e) {
      logger.warn(e);
    }
    http.disconnect();
  }

  private void printResponse(final HttpURLConnection http, String compare) {
    BufferedReader in = null;
    try {
      in = new BufferedReader(new InputStreamReader(http.getInputStream()));
      String line;
      StringBuffer response = new StringBuffer();

      while ((line = in.readLine()) != null) {
        response.append(line);
      }
      in.close();
      logger.warn("MSG: {}", response.toString());
      if (compare != null) {
        assertEquals(compare, response.toString());
      }
    } catch (IOException e) {
      logger.warn(e.getMessage());
      try {
        logger.warn("MSG2: {} {}", http.getContentLength(),
                    http.getResponseMessage());
      } catch (IOException ex) {
        logger.warn(ex);
      }
    }
  }

  @Test
  public void test01NoRuleGet() throws IOException {
    // Test GET
    String getRequest =
        serverUri.toString() + "/download?user=test&key" + "=" + KEY +
        "&identifier=12345" + "&filename=filename.txt";
    HttpURLConnection http =
        (HttpURLConnection) new URL(getRequest).openConnection();
    http.setRequestMethod("GET");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", WaarpStringUtils.UTF_8);
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);
    MatcherAssert.assertThat("Response Code", http.getResponseCode(),
                             is(HttpStatus.BAD_REQUEST_400));
    http.disconnect();
  }

  @Test
  public void test01NoFileGet() throws IOException {
    File file = new File("/tmp/R66/out/filename.txt");
    file.delete();
    // Test GET
    String getRequest = serverUri.toString() + "/download?";
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("user", "test");
    map.put("key", KEY);
    map.put("rulename", "rule4");
    map.put("comment", "download");
    map.put("identifier", "123456");
    map.put("filename", "filename.txt");
    StringBuilder tokenUri = new StringBuilder();
    for (Entry<String, String> entry : map.entrySet()) {
      if (tokenUri.length() != 0) {
        tokenUri.append('&');
      }
      tokenUri.append(entry.getKey()).append('=').append(entry.getValue());
    }
    HttpURLConnection http = (HttpURLConnection) new URL(
        getRequest + tokenUri.toString()).openConnection();
    http.setRequestMethod("GET");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", WaarpStringUtils.UTF_8);
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);
    MatcherAssert.assertThat("Response Code", http.getResponseCode(),
                             is(HttpStatus.BAD_REQUEST_400));
    http.disconnect();
  }

  @Test
  public void test10Get() throws IOException {
    File file = new File("/tmp/R66/out/filename.txt");
    FileOutputStream outputStream = new FileOutputStream(file);
    String content = "File GET content downloaded";
    outputStream.write(content.getBytes(WaarpStringUtils.UTF8));
    outputStream.flush();
    outputStream.close();

    // Test GET
    String getRequest = serverUri.toString() + "/download?";
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("user", "test");
    map.put("key", KEY);
    map.put("rulename", "rule4");
    map.put("comment", "download");
    map.put("identifier", "1234567");
    map.put("filename", "filename.txt");
    StringBuilder tokenUri = new StringBuilder();
    for (Entry<String, String> entry : map.entrySet()) {
      if (tokenUri.length() != 0) {
        tokenUri.append('&');
      }
      tokenUri.append(entry.getKey()).append('=').append(entry.getValue());
    }
    HttpURLConnection http = (HttpURLConnection) new URL(
        getRequest + tokenUri.toString()).openConnection();
    http.setRequestMethod("GET");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", WaarpStringUtils.UTF_8);
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, content);
    MatcherAssert.assertThat("Response Code", http.getResponseCode(),
                             is(HttpStatus.OK_200));
    String hash = http.getHeaderField(DownloadServlet.X_HASH_SHA_256);
    http.disconnect();
    byte[] bin = FilesystemBasedDigest.getHash(file, false, DigestAlgo.SHA256);
    MatcherAssert.assertThat("Hash Code", hash,
                             is(FilesystemBasedDigest.getHex(bin)));
    file.delete();
  }

  @Test
  public void test12Head() throws IOException {
    File file = new File("/tmp/R66/out/filename.txt");
    FileOutputStream outputStream = new FileOutputStream(file);
    String content = "File GET content downloaded";
    outputStream.write(content.getBytes(WaarpStringUtils.UTF8));
    outputStream.flush();
    outputStream.close();

    // Test HEAD
    String getRequest = serverUri.toString() + "/download?";
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("user", "test");
    map.put("key", KEY);
    map.put("rulename", "rule4");
    map.put("comment", "download");
    map.put("identifier", "123456789");
    map.put("filename", "filename.txt");
    StringBuilder tokenUri = new StringBuilder();
    for (Entry<String, String> entry : map.entrySet()) {
      if (tokenUri.length() != 0) {
        tokenUri.append('&');
      }
      tokenUri.append(entry.getKey()).append('=').append(entry.getValue());
    }
    HttpURLConnection http = (HttpURLConnection) new URL(
        getRequest + tokenUri.toString()).openConnection();
    http.setRequestMethod("HEAD");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", WaarpStringUtils.UTF_8);
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    MatcherAssert.assertThat("Response Code", http.getResponseCode(),
                             is(HttpStatus.NOT_FOUND_404));
    // Test GET
    http = (HttpURLConnection) new URL(
        getRequest + tokenUri.toString()).openConnection();
    http.setRequestMethod("GET");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", WaarpStringUtils.UTF_8);
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, content);
    MatcherAssert.assertThat("Response Code", http.getResponseCode(),
                             is(HttpStatus.OK_200));
    String hash = http.getHeaderField(DownloadServlet.X_HASH_SHA_256);
    http.disconnect();
    byte[] bin = FilesystemBasedDigest.getHash(file, false, DigestAlgo.SHA256);
    MatcherAssert.assertThat("Hash Code", hash,
                             is(FilesystemBasedDigest.getHex(bin)));
    file.delete();

    // Test HEAD 2
    http = (HttpURLConnection) new URL(
        getRequest + tokenUri.toString()).openConnection();
    http.setRequestMethod("HEAD");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", WaarpStringUtils.UTF_8);
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    MatcherAssert.assertThat("Response Code", http.getResponseCode(),
                             is(HttpStatus.OK_200));
  }

  @Test
  public void test11Post() throws IOException {
    File file = new File("/tmp/R66/out/filename.txt");
    FileOutputStream outputStream = new FileOutputStream(file);
    String content = "File POST content downloaded";
    outputStream.write(content.getBytes(WaarpStringUtils.UTF8));
    outputStream.flush();
    outputStream.close();
    // Test GET
    String getRequest = serverUri.toString() + "/download?";
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("user", "test");
    map.put("key", KEY);
    map.put("rulename", "rule4");
    map.put("comment", "download");
    map.put("identifier", "12345678");
    map.put("filename", "filename.txt");
    StringBuilder tokenUri = new StringBuilder();
    for (Entry<String, String> entry : map.entrySet()) {
      if (tokenUri.length() != 0) {
        tokenUri.append('&');
      }
      tokenUri.append(entry.getKey()).append('=').append(entry.getValue());
    }
    HttpURLConnection http = (HttpURLConnection) new URL(
        getRequest + tokenUri.toString()).openConnection();
    http.setRequestMethod("POST");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", WaarpStringUtils.UTF_8);
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, content);
    MatcherAssert.assertThat("Response Code", http.getResponseCode(),
                             is(HttpStatus.OK_200));
    String hash = http.getHeaderField(DownloadServlet.X_HASH_SHA_256);
    http.disconnect();
    byte[] bin = FilesystemBasedDigest.getHash(file, false, DigestAlgo.SHA256);
    MatcherAssert.assertThat("Hash Code", hash,
                             is(FilesystemBasedDigest.getHex(bin)));
    file.delete();
  }

}
