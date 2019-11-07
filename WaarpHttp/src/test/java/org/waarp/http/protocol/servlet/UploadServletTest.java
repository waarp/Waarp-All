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

import com.google.common.base.Charsets;
import com.google.common.io.ByteSource;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;

import static org.hamcrest.core.Is.*;
import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UploadServletTest extends TestAbstract {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(UploadServletTest.class);

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
  private static String host = null;
  private static int port;
  private final String USER_AGENT = "Mozilla/5.0";

  @BeforeClass
  public static void startJetty() throws Exception {
    WaarpLoggerFactory.setLogLevel(WaarpLogLevel.INFO);
    final ClassLoader classLoader = UploadServletTest.class.getClassLoader();
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
    DetectionUtils.setJunit(false);
    server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setReuseAddress(true);
    connector.setIdleTimeout(1000 * 60);
    connector.setPort(0); // auto-bind to available port
    server.addConnector(connector);
    WebAppContext webAppContext = new WebAppContext();
    webAppContext.setDescriptor(
        new File(testDir, "webapp/WEB-INF/web.xml").getAbsolutePath());
    webAppContext
        .setResourceBase(new File(testDir, "webapp").getAbsolutePath());
    webAppContext.setContextPath("/WaarpHttp");
    webAppContext.setParentLoaderPriority(true);

    server.setHandler(webAppContext);
    server.setStopAtShutdown(true);

    WaarpLoggerFactory.setLogLevel(WaarpLogLevel.INFO);
    // Start Server
    server.start();

    // Determine Base URI for Server
    host = connector.getHost();
    if (host == null) {
      host = "localhost";
    }
    port = connector.getLocalPort();
    serverUri = new URI(String.format("http://%s:%d/WaarpHttp", host, port));
    logger.warn("URI = {}", serverUri.toString());
  }

  @AfterClass
  public static void stopJetty() throws Exception {
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
  public void test00InvalidGet() throws IOException {
    // Test GET
    HttpURLConnection http =
        (HttpURLConnection) serverUri.resolve("/").toURL().openConnection();
    http.setRequestMethod("GET");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);

    try {
      assertThat("Response Code", http.getResponseCode(),
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
  public void test01NotYetGet() throws IOException {
    // Test GET
    String getRequest =
        serverUri.toString() + "/resumable?user=test&key" + "=" + KEY +
        "&rulename=rule3" + "&resumableChunkNumber=1&resumableChunkSize=1024" +
        "&resumableTotalSize=100&resumableIdentifier=abcdab" +
        "&resumableFilename=filename" +
        ".txt&resumableRelativePath=filename.txt";
    HttpURLConnection http =
        (HttpURLConnection) new URL(getRequest).openConnection();
    http.setRequestMethod("GET");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);
    assertThat("Response Code", http.getResponseCode(),
               is(HttpStatus.NOT_FOUND_404));
    http.disconnect();
  }

  @Test
  public void test02InvalidUser() throws IOException {
    String getRequest = serverUri.toString() + "/resumable?";
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("user", "test");
    map.put("key", "aa" + KEY);
    map.put("rulename", "rule3");
    map.put("comment", "1Chunk");
    map.put("resumableChunkNumber", "1");
    map.put("resumableChunkSize", "1024");
    map.put("resumableTotalSize", "100");
    map.put("resumableIdentifier", "abcdabee");
    map.put("resumableFilename", "filename.txt");
    map.put("resumableRelativePath", "filename.txt");
    StringBuilder tokenUri = new StringBuilder();
    for (Entry<String, String> entry : map.entrySet()) {
      if (tokenUri.length() != 0) {
        tokenUri.append('&');
      }
      tokenUri.append(entry.getKey()).append('=').append(entry.getValue());
    }

    // POST
    HttpURLConnection http =
        (HttpURLConnection) new URL(getRequest + tokenUri.toString())
            .openConnection();
    http.setRequestMethod("POST");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    http.setDoOutput(true);
    StringBuilder builder = new StringBuilder(100);
    for (int i = 0; i < 100; i++) {
      builder.append('C');
    }
    String content = builder.toString();
    writeBinaryContent(http, content);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);
    assertThat("Response Code", http.getResponseCode(),
               is(HttpStatus.INTERNAL_SERVER_ERROR_500));
    http.disconnect();
  }

  private void writeBinaryContent(HttpURLConnection http, String filecontent)
      throws IOException {
    http.setRequestProperty("Connection", "Keep-Alive");
    http.setRequestProperty("Cache-Control", "no-cache");
    http.setRequestProperty("Content-Type", "application/octet-stream");
    http.setRequestProperty("Content-Length",
                            Integer.toString(filecontent.getBytes().length));
    DataOutputStream request = new DataOutputStream(http.getOutputStream());
    request.write(filecontent.getBytes());
    request.flush();
    request.close();
  }

  @Test
  public void test10Chunk() throws IOException {
    // Test GET
    String getRequest = serverUri.toString() + "/resumable?";
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("user", "test");
    map.put("key", KEY);
    map.put("rulename", "rule3");
    map.put("comment", "1Chunk");
    map.put("resumableChunkNumber", "1");
    map.put("resumableChunkSize", "1024");
    map.put("resumableTotalSize", "100");
    map.put("resumableIdentifier", "abcdabdd");
    map.put("resumableFilename", "filename.txt");
    map.put("resumableRelativePath", "filename.txt");
    StringBuilder tokenUri = new StringBuilder();
    for (Entry<String, String> entry : map.entrySet()) {
      if (tokenUri.length() != 0) {
        tokenUri.append('&');
      }
      tokenUri.append(entry.getKey()).append('=').append(entry.getValue());
    }
    HttpURLConnection http =
        (HttpURLConnection) new URL(getRequest + tokenUri.toString())
            .openConnection();
    http.setRequestMethod("GET");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);
    assertThat("Response Code", http.getResponseCode(),
               is(HttpStatus.NOT_FOUND_404));
    http.disconnect();

    // POST
    http = (HttpURLConnection) new URL(getRequest + tokenUri.toString())
        .openConnection();
    http.setRequestMethod("POST");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    http.setDoOutput(true);
    StringBuilder builder = new StringBuilder(100);
    for (int i = 0; i < 100; i++) {
      builder.append('C');
    }
    String content = builder.toString();
    writeBinaryContent(http, content);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, "All finished.");
    assertThat("Response Code", http.getResponseCode(), is(HttpStatus.OK_200));
    http.disconnect();
  }

  @Test
  public void test11ChunkWrongId() throws IOException {
    // Test GET
    String getRequest = serverUri.toString() + "/resumable?";
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("user", "test");
    map.put("key", KEY + "bb");
    map.put("rulename", "rule3");
    map.put("resumableChunkNumber", "1");
    map.put("resumableChunkSize", "1024");
    map.put("resumableTotalSize", "100");
    map.put("resumableIdentifier", "abcdabaa");
    map.put("resumableFilename", "filename.txt");
    map.put("resumableRelativePath", "filename.txt");
    StringBuilder tokenUri = new StringBuilder();
    for (Entry<String, String> entry : map.entrySet()) {
      if (tokenUri.length() != 0) {
        tokenUri.append('&');
      }
      tokenUri.append(entry.getKey()).append('=').append(entry.getValue());
    }
    HttpURLConnection http =
        (HttpURLConnection) new URL(getRequest + tokenUri.toString())
            .openConnection();
    http.setRequestMethod("GET");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);
    assertThat("Response Code", http.getResponseCode(),
               is(HttpStatus.INTERNAL_SERVER_ERROR_500));
    http.disconnect();

    // POST
    http = (HttpURLConnection) new URL(getRequest + tokenUri.toString())
        .openConnection();
    http.setRequestMethod("POST");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    http.setDoOutput(true);
    StringBuilder builder = new StringBuilder(100);
    for (int i = 0; i < 100; i++) {
      builder.append('C');
    }
    String content = builder.toString();
    writeBinaryContent(http, content);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);
    assertThat("Response Code", http.getResponseCode(),
               is(HttpStatus.INTERNAL_SERVER_ERROR_500));
    http.disconnect();
  }

  @Test
  public void test11ChunkWrongChunk() throws IOException {
    // Test GET
    String getRequest = serverUri.toString() + "/resumable?";
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("user", "test");
    map.put("key", KEY);
    map.put("rulename", "rule3");
    map.put("resumableChunkNumber", "0");
    map.put("resumableChunkSize", "1024");
    map.put("resumableTotalSize", "100");
    map.put("resumableIdentifier", "abcdabac");
    map.put("resumableFilename", "filename.txt");
    map.put("resumableRelativePath", "filename.txt");
    StringBuilder tokenUri = new StringBuilder();
    for (Entry<String, String> entry : map.entrySet()) {
      if (tokenUri.length() != 0) {
        tokenUri.append('&');
      }
      tokenUri.append(entry.getKey()).append('=').append(entry.getValue());
    }
    HttpURLConnection http =
        (HttpURLConnection) new URL(getRequest + tokenUri.toString())
            .openConnection();
    http.setRequestMethod("GET");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);
    assertThat("Response Code", http.getResponseCode(),
               is(HttpStatus.INTERNAL_SERVER_ERROR_500));
    http.disconnect();

    // POST
    http = (HttpURLConnection) new URL(getRequest + tokenUri.toString())
        .openConnection();
    http.setRequestMethod("POST");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    http.setDoOutput(true);
    StringBuilder builder = new StringBuilder(100);
    for (int i = 0; i < 100; i++) {
      builder.append('C');
    }
    String content = builder.toString();
    writeBinaryContent(http, content);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);
    assertThat("Response Code", http.getResponseCode(),
               is(HttpStatus.INTERNAL_SERVER_ERROR_500));
    http.disconnect();
  }

  @Test
  public void test12ChunkWrongSHA() throws IOException {
    // Test GET
    String getRequest = serverUri.toString() + "/resumable?";
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("user", "test");
    map.put("key", KEY);
    map.put("rulename", "rule3");
    map.put("sha256", "72bb112c25550df94e763170196be56b7774cab8bb54a4a4dacd6cb6f2c9c48f");
    map.put("resumableChunkNumber", "1");
    map.put("resumableChunkSize", "1024");
    map.put("resumableTotalSize", "100");
    map.put("resumableIdentifier", "abcdabacff");
    map.put("resumableFilename", "filename.txt");
    map.put("resumableRelativePath", "filename.txt");
    StringBuilder tokenUri = new StringBuilder();
    for (Entry<String, String> entry : map.entrySet()) {
      if (tokenUri.length() != 0) {
        tokenUri.append('&');
      }
      tokenUri.append(entry.getKey()).append('=').append(entry.getValue());
    }
    HttpURLConnection http =
        (HttpURLConnection) new URL(getRequest + tokenUri.toString())
            .openConnection();
    http.setRequestMethod("GET");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);
    assertThat("Response Code", http.getResponseCode(),
               is(HttpStatus.NOT_FOUND_404));
    http.disconnect();

    // POST 1/2
    http = (HttpURLConnection) new URL(getRequest + tokenUri.toString())
        .openConnection();
    http.setRequestMethod("POST");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    http.setDoOutput(true);
    StringBuilder builder = new StringBuilder(100);
    for (int i = 0; i < 100; i++) {
      builder.append('C');
    }
    String content = builder.toString();
    writeBinaryContent(http, content);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);
    assertThat("Response Code", http.getResponseCode(),
               is(HttpStatus.INTERNAL_SERVER_ERROR_500));
    http.disconnect();

    ByteArrayInputStream stream =
        new ByteArrayInputStream(content.getBytes());
    String sha =
        FilesystemBasedDigest.getHex(FilesystemBasedDigest.getHash(stream,
                                                        DigestAlgo.SHA256));
    stream.close();
    map.put("resumableIdentifier", "abcdabacffff");
    map.put("sha256", sha);
    tokenUri = new StringBuilder();
    for (Entry<String, String> entry : map.entrySet()) {
      if (tokenUri.length() != 0) {
        tokenUri.append('&');
      }
      tokenUri.append(entry.getKey()).append('=').append(entry.getValue());
    }
    http = (HttpURLConnection) new URL(getRequest + tokenUri.toString())
        .openConnection();
    http.setRequestMethod("POST");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    http.setDoOutput(true);
    writeBinaryContent(http, content);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);
    assertThat("Response Code", http.getResponseCode(),
               is(HttpStatus.OK_200));
    http.disconnect();
  }

  @Test
  public void test20Chunks() throws IOException {
    // Test GET
    String getRequest = serverUri.toString() + "/resumable?";
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("user", "test");
    map.put("key", KEY);
    map.put("rulename", "rule3");
    map.put("comment", "2Chunks");
    map.put("resumableChunkNumber", "1");
    map.put("resumableChunkSize", "1024");
    map.put("resumableTotalSize", "1025");
    map.put("resumableIdentifier", "abcdabab");
    map.put("resumableFilename", "filename.txt");
    map.put("resumableRelativePath", "filename.txt");
    StringBuilder tokenUri = new StringBuilder();
    for (Entry<String, String> entry : map.entrySet()) {
      if (tokenUri.length() != 0) {
        tokenUri.append('&');
      }
      tokenUri.append(entry.getKey()).append('=').append(entry.getValue());
    }
    HttpURLConnection http =
        (HttpURLConnection) new URL(getRequest + tokenUri.toString())
            .openConnection();
    http.setRequestMethod("GET");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);
    assertThat("Response Code", http.getResponseCode(),
               is(HttpStatus.NOT_FOUND_404));
    http.disconnect();

    // POST 1/2
    http = (HttpURLConnection) new URL(getRequest + tokenUri.toString())
        .openConnection();
    http.setRequestMethod("POST");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    http.setDoOutput(true);
    StringBuilder builder = new StringBuilder(1024);
    for (int i = 0; i < 1024; i++) {
      builder.append('C');
    }
    String content = builder.toString();
    writeBinaryContent(http, content);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, "Upload");
    assertThat("Response Code", http.getResponseCode(), is(HttpStatus.CREATED_201));
    http.disconnect();

    // GET 1/2
    http = (HttpURLConnection) new URL(getRequest + tokenUri.toString())
        .openConnection();
    http.setRequestMethod("GET");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, "Uploaded.");
    assertThat("Response Code", http.getResponseCode(), is(HttpStatus.OK_200));
    http.disconnect();

    // POST 1/2 bis
    http = (HttpURLConnection) new URL(getRequest + tokenUri.toString())
        .openConnection();
    http.setRequestMethod("POST");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    http.setDoOutput(true);
    writeBinaryContent(http, content);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, null);
    assertThat("Response Code", http.getResponseCode(),
               is(HttpStatus.INTERNAL_SERVER_ERROR_500));
    http.disconnect();

    // POST 2/2
    map.put("resumableChunkNumber", "2");
    tokenUri = new StringBuilder();
    for (Entry<String, String> entry : map.entrySet()) {
      if (tokenUri.length() != 0) {
        tokenUri.append('&');
      }
      tokenUri.append(entry.getKey()).append('=').append(entry.getValue());
    }
    http = (HttpURLConnection) new URL(getRequest + tokenUri.toString())
        .openConnection();
    http.setRequestMethod("POST");
    http.setRequestProperty("User-Agent", USER_AGENT);
    http.setRequestProperty("Accept-Charset", "UTF-8");
    http.setDoInput(true);
    http.setDoOutput(true);
    content = "A";
    writeBinaryContent(http, content);
    try {
      http.connect();
    } catch (IOException e) {
      fail(e.getMessage());
    }
    printResponse(http, "All finished.");
    assertThat("Response Code", http.getResponseCode(), is(HttpStatus.OK_200));
    http.disconnect();
  }

  @Test
  public void test30MultipartChunks() throws IOException, URISyntaxException {
    // Test GET
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("user", "test");
    map.put("key", KEY);
    map.put("rulename", "rule3");
    map.put("comment", "2MultipartChunks");
    map.put("resumableChunkNumber", "1");
    map.put("resumableChunkSize", "1024");
    map.put("resumableTotalSize", "1025");
    map.put("resumableIdentifier", "abcdababddf");
    map.put("resumableFilename", "filename.txt");
    map.put("resumableRelativePath", "filename.txt");
    URIBuilder uriBuilder = new URIBuilder();
    uriBuilder.setScheme("http").setHost(String.format("%s:%d", host, port))
              .setPath("/WaarpHttp/resumable");
    for (Entry<String, String> entry : map.entrySet()) {
      uriBuilder.setParameter(entry.getKey(), entry.getValue());
    }
    URI uri = uriBuilder.build();
    CloseableHttpClient client = HttpClients.createDefault();
    HttpGet get = new HttpGet(uri);
    final HttpResponse response = client.execute(get);
    ByteSource byteSource = new ByteSource() {
      @Override
      public InputStream openStream() throws IOException {
        return response.getEntity().getContent();
      }
    };
    String valueText = byteSource.asCharSource(Charsets.UTF_8).read();
    logger.debug("GET {}", valueText);
    assertThat("Response Code", response.getStatusLine().getStatusCode(),
               is(HttpStatus.NOT_FOUND_404));

    // POST 1/2
    StringBuilder builder = new StringBuilder(1024);
    for (int i = 0; i < 1024; i++) {
      builder.append('C');
    }
    String content = builder.toString();
    MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
    for (Entry<String, String> entry : map.entrySet()) {
      entityBuilder.addTextBody(entry.getKey(), entry.getValue());
    }
    entityBuilder.addBinaryBody(UploadServlet.FIELD_FILE, content.getBytes(),
                                ContentType.create("application/octet-stream"),
                                "filename");
    HttpEntity entity = entityBuilder.build();
    String postRequest = serverUri.toString() + "/resumable";
    HttpPost post = new HttpPost(postRequest);
    post.setEntity(entity);
    final HttpResponse response2 = client.execute(post);
    byteSource = new ByteSource() {
      @Override
      public InputStream openStream() throws IOException {
        return response2.getEntity().getContent();
      }
    };
    valueText = byteSource.asCharSource(Charsets.UTF_8).read();
    logger.debug("POST {}", valueText);
    assertThat("Response Code", response2.getStatusLine().getStatusCode(),
               is(HttpStatus.CREATED_201));

    // GET 1/2
    final HttpResponse response3 = client.execute(get);
    byteSource = new ByteSource() {
      @Override
      public InputStream openStream() throws IOException {
        return response3.getEntity().getContent();
      }
    };
    valueText = byteSource.asCharSource(Charsets.UTF_8).read();
    logger.debug("GET {}", valueText);
    assertThat("Response Code", response3.getStatusLine().getStatusCode(),
               is(HttpStatus.OK_200));

    // POST 1/2 bis
    MultipartEntityBuilder entityBuilder2 = MultipartEntityBuilder.create();
    entityBuilder.addBinaryBody(UploadServlet.FIELD_FILE, content.getBytes(),
                                ContentType.create("application/octet-stream"),
                                "filename");
    HttpEntity entity2 = entityBuilder2.build();
    post = new HttpPost(uri);
    post.setEntity(entity2);
    final HttpResponse response4 = client.execute(post);
    byteSource = new ByteSource() {
      @Override
      public InputStream openStream() throws IOException {
        return response4.getEntity().getContent();
      }
    };
    valueText = byteSource.asCharSource(Charsets.UTF_8).read();
    logger.debug("POST {}", valueText);
    assertThat("Response Code", response4.getStatusLine().getStatusCode(),
               is(HttpStatus.INTERNAL_SERVER_ERROR_500));

    // POST 1/2 ter
    post = new HttpPost(postRequest);
    post.setEntity(entity);
    final HttpResponse response5 = client.execute(post);
    byteSource = new ByteSource() {
      @Override
      public InputStream openStream() throws IOException {
        return response5.getEntity().getContent();
      }
    };
    valueText = byteSource.asCharSource(Charsets.UTF_8).read();
    logger.debug("POST {}", valueText);
    assertThat("Response Code", response5.getStatusLine().getStatusCode(),
               is(HttpStatus.INTERNAL_SERVER_ERROR_500));

    // POST 2/2
    map.put("resumableChunkNumber", "2");
    entityBuilder = MultipartEntityBuilder.create();
    for (Entry<String, String> entry : map.entrySet()) {
      entityBuilder.addTextBody(entry.getKey(), entry.getValue());
    }
    content = "A";
    entityBuilder.addBinaryBody(UploadServlet.FIELD_FILE, content.getBytes(),
                                ContentType.create("application/octet-stream"),
                                "filename");
    entity = entityBuilder.build();
    post = new HttpPost(postRequest);
    post.setEntity(entity);
    final HttpResponse response6 = client.execute(post);
    byteSource = new ByteSource() {
      @Override
      public InputStream openStream() throws IOException {
        return response6.getEntity().getContent();
      }
    };
    valueText = byteSource.asCharSource(Charsets.UTF_8).read();
    logger.debug("POST {}", valueText);
    assertEquals("All finished.", valueText);
    assertThat("Response Code", response6.getStatusLine().getStatusCode(),
               is(HttpStatus.OK_200));
    client.close();
  }

}
