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

package org.waarp.icap;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.icap.server.IcapServer;
import org.waarp.icap.server.IcapServerHandler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * IcapClient Tester.
 */
public class IcapClientTest {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(IcapClientTest.class);

  @BeforeClass
  public static void beforeClass() {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
  }

  @Before
  public void before() throws Exception {
    IcapServerHandler.resetJunitStatus();
  }

  @After
  public void after() throws Exception {
    IcapServer.shutdown();
    logger.debug("Server stopped");
  }

  @Test
  public void testCreation() throws Exception {
    IcapClient icapClient = new IcapClient("127.0.0.1", 1234, "avscan");
    try {
      icapClient = new IcapClient("", 1234, "avscan");
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Intended exception");
    }
    try {
      icapClient = new IcapClient(null, 1234, "avscan");
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Intended exception");
    }
    try {
      icapClient = new IcapClient("", 1234, "avscan", 100);
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Intended exception");
    }
    try {
      icapClient = new IcapClient(null, 1234, "avscan", 100);
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Intended exception");
    }
    try {
      icapClient = new IcapClient("127.0.0.1", 1234, "");
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Intended exception");
    }
    try {
      icapClient = new IcapClient("127.0.0.1", 1234, null);
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Intended exception");
    }
    try {
      icapClient = new IcapClient("127.0.0.1", 1234, "", 100);
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Intended exception");
    }
    try {
      icapClient = new IcapClient("127.0.0.1", 1234, null, 100);
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Intended exception");
    }
    icapClient = new IcapClient("127.0.0.1", 0, "avscan");
    assertEquals(IcapClient.DEFAULT_ICAP_PORT, icapClient.getPort());
    assertEquals(0, icapClient.getPreviewSize());
    assertEquals("avscan", icapClient.getIcapService());
    assertEquals("127.0.0.1", icapClient.getServerIP());
    icapClient = new IcapClient("127.0.0.1", 0, "avscan", 100);
    assertEquals(IcapClient.DEFAULT_ICAP_PORT, icapClient.getPort());
    assertEquals(100, icapClient.getPreviewSize());
    icapClient = new IcapClient("127.0.0.1", 1235, "avscan", -1);
    assertEquals(1235, icapClient.getPort());
    assertEquals(0, icapClient.getPreviewSize());
    assertNull(icapClient.getFinalResult());
  }

  /**
   * Method: connect()
   */
  @Test
  public void testConnect() throws Exception {
    IcapClient icapClient = new IcapClient("127.0.0.1", 1234, "avscan");
    assertNull(icapClient.getSubStringFromKeyIcap200());
    assertNull(icapClient.getKeyIcap200());
    assertNull(icapClient.getSubstringHttpStatus200());
    assertNull(icapClient.getKeyIcap204());
    assertNull(icapClient.getSubStringFromKeyIcap204());
    assertEquals(0, icapClient.getPreviewSize());
    assertEquals(Integer.MAX_VALUE, icapClient.getMaxSize());
    assertEquals(8192, icapClient.getSendLength());
    assertEquals(64 * 1024, icapClient.getReceiveLength());
    icapClient.close();
    try {
      icapClient.connect();
      fail("Should failed");
    } catch (IcapException e) {
      assertTrue(e.getError() == IcapError.ICAP_CANT_CONNECT);
    }
    icapClient.close();
    logger.warn("Disconnection while not connected with no impact");
    IcapServer.main(new String[] { "127.0.0.1", "9999" });
    logger.warn("Server started");
    Thread.sleep(100);
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 2048);
    try {
      icapClient.connect();
      logger.warn("Connection OK");
      assertTrue(icapClient.getPreviewSize() == 2048);
      assertNull(icapClient.getFinalResult());
      icapClient.close();
      logger.warn("Disconnection OK");
      icapClient.connect();
      logger.warn("ReConnection OK");
      icapClient.connect();
      logger.warn("ReConnection without disconnection OK");
      icapClient.close();
      logger.warn("Disconnection OK");
    } catch (IcapException e) {
      logger.error(e);
      fail("Should failed");
    }
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan");
    icapClient.close();
    logger.warn("Disconnection while not connected with no impact");
    try {
      icapClient.connect();
      logger.warn("Connection OK");
      assertTrue(icapClient.getPreviewSize() == 4096);
      assertTrue(icapClient.getFinalResult().containsKey("Preview"));
      icapClient.close();
      assertNotNull(icapClient.getFinalResult());
      logger.warn("Disconnection OK");
      icapClient.close();
      logger.warn("Disconnection repeated with no impact");
      icapClient.connect().connect().close();
      logger.warn("ReConnection without disconnection OK");
    } catch (IcapException e) {
      logger.error(e);
      fail("Should failed");
    }
    logger.warn("No preview");
    IcapServerHandler.setIsPreviewOkTest(false);
    IcapServerHandler.setFinalStatus(200);
    try {
      icapClient.connect();
      fail("Should failed");
    } catch (IcapException e) {
      assertTrue(e.getError() == IcapError.ICAP_SERVER_MISSING_INFO);
    }
    IcapServerHandler.setFinalStatus(500);
    logger.warn("OPTIONS error");
    try {
      icapClient.connect();
      fail("Should failed");
    } catch (IcapException e) {
      assertTrue(e.getError() == IcapError.ICAP_SERVER_MISSING_INFO);
    }
  }

  private void createFile(File file, int len) throws IOException {
    file.delete();
    byte[] from = new byte[len];
    Arrays.fill(from, (byte) 'A');
    FileOutputStream outputStream = new FileOutputStream(file);
    outputStream.write(from);
    outputStream.flush();
    outputStream.close();
  }

  /**
   * Method: scanFile(final String filename)
   */
  @Test
  public void testScanFile() throws Exception {
    File file = new File("/tmp/toscan.bin");
    createFile(file, 1000);

    IcapClient icapClient = new IcapClient("127.0.0.1", 9999, "avscan");
    // Not server yet
    logger.warn("\n=======================\nSERVER NOT YET START " +
                "UP\n=======================");
    try {
      icapClient.scanFile(file.getAbsolutePath());
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception is correct since server is not started yet");
    }

    IcapServer.main(new String[] { "127.0.0.1", "9999" });
    // Not server yet
    logger.warn("\n=======================\nSERVER START UP but no file" +
                "\n=======================");
    try {
      icapClient.scanFile(null);
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Exception is correct since no filename provided");
    }
    logger.warn("\n=======================\nSERVER START UP with no Preview " +
                "set\n=======================");
    // Direct scan
    try {
      assertTrue(icapClient.scanFile(file.getAbsolutePath()));
      assertTrue(icapClient.getFinalResult().containsKey("Allow"));
    } catch (IcapException e) {
      logger.error(e);
      fail("Should not raised an exception");
    }
    icapClient.close();

    // Connection then scan with file size < preview
    logger.warn("\n=======================\nSERVER START UP with original " +
                "PREVIEW > FILE SIZE\n=======================");
    try {
      icapClient.connect();
      assertTrue(icapClient.scanFile(file.getAbsolutePath()));
    } catch (IcapException e) {
      logger.error(e);
      fail("Should not raised an exception");
    }
    icapClient.close();

    // Connection then scan larger file than preview
    logger.warn(
        "\n=======================\nSERVER START UP with new PREVIEW < " +
        "FILE SIZE\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 500);
    try {
      icapClient.connect();
      assertEquals(500, icapClient.getPreviewSize());
      assertTrue(icapClient.scanFile(file.getAbsolutePath()));
    } catch (IcapException e) {
      logger.error(e);
      fail("Should not raised an exception");
    }
    icapClient.close();

    // Connection then scan larger file than preview and chunk number > 1
    logger.warn("\n=======================\nSERVER START UP with new PREVIEW " +
                "and SEND SIZE << FILE SIZE\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 100);
    try {
      icapClient.connect();
      assertEquals(100, icapClient.getPreviewSize());
      icapClient.setSendLength(100);
      assertTrue(icapClient.scanFile(file.getAbsolutePath()));
    } catch (IcapException e) {
      logger.error(e);
      fail("Should not raised an exception");
    }
    icapClient.close();

    // Connection then scan with preview = 0 and send size > file size
    logger.warn(
        "\n=======================\nSERVER START UP with new PREVIEW = 0 but " +
        "SEND SIZE > file " + "size\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 0);
    try {
      icapClient.connect();
      assertEquals(0, icapClient.getPreviewSize());
      assertTrue(icapClient.scanFile(file.getAbsolutePath()));
    } catch (IcapException e) {
      logger.error(e);
      fail("Should not raised an exception");
    }
    icapClient.close();

    // Connection then scan with preview = 0 and send size < file size
    logger.warn(
        "\n=======================\nSERVER START UP with new PREVIEW = " +
        "0 but SEND SIZE < file size\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 0);
    try {
      icapClient.connect();
      assertEquals(0, icapClient.getPreviewSize());
      icapClient.setSendLength(100);
      assertTrue(icapClient.scanFile(file.getAbsolutePath()));
    } catch (IcapException e) {
      logger.error(e);
      fail("Should not raised an exception");
    }
    icapClient.close();

    // Connection then scan with preview = 100 << file size + 204 Preview
    logger.warn(
        "\n=======================\nSERVER START UP with new PREVIEW << " +
        "file size and 204 intermediary status\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 100);
    try {
      icapClient.connect();
      assertEquals(100, icapClient.getPreviewSize());
      icapClient.setSendLength(100);
      IcapServerHandler.setIntermediaryStatus(204);
      assertTrue(icapClient.scanFile(file.getAbsolutePath()));
    } catch (IcapException e) {
      logger.error(e);
      fail("Should not raised an exception");
    }
    icapClient.close();
  }

  /**
   * Method: scanFile(final String filename)
   */
  @Test
  public void testBadScanFile() throws Exception {
    File file = new File("/tmp/toscan.bin");
    createFile(file, 1000);

    IcapServer.main(new String[] { "127.0.0.1", "9999" });

    // Connection then scan with preview = 100 << file size + 200 Preview
    logger.warn(
        "\n=======================\nSERVER START UP with new PREVIEW << " +
        "file size and 200 intermediary status\n=======================");
    IcapClient icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 100);
    try {
      icapClient.connect();
      assertEquals(100, icapClient.getPreviewSize());
      icapClient.setSendLength(100);
      IcapServerHandler.setIntermediaryStatus(200);
      assertFalse(icapClient.scanFile(file.getAbsolutePath()));
    } catch (IcapException e) {
      logger.error(e);
      fail("Should not raised an exception");
    }
    icapClient.close();

    // Connection then scan with preview = 100 << file size + 404 Preview
    logger.warn(
        "\n=======================\nSERVER START UP with new PREVIEW << " +
        "file size and 404 intermediary status\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 100);
    try {
      icapClient.connect();
      assertEquals(100, icapClient.getPreviewSize());
      icapClient.setSendLength(100);
      IcapServerHandler.setIntermediaryStatus(404);
      icapClient.scanFile(file.getAbsolutePath());
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception is intended");
    }
    icapClient.close();

    // Connection then scan with preview = 100 << file size + 500 Preview
    logger.warn(
        "\n=======================\nSERVER START UP with new PREVIEW << " +
        "file size and 500 intermediary status\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 100);
    try {
      icapClient.connect();
      assertEquals(100, icapClient.getPreviewSize());
      icapClient.setSendLength(100);
      IcapServerHandler.setIntermediaryStatus(500);
      icapClient.scanFile(file.getAbsolutePath());
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception is intended");
    }
    icapClient.close();

    // Connection then scan with requested preview with 200 response but
    // incomplete
    logger.warn(
        "\n=======================\nSERVER START UP with requested PREVIEW <<" +
        " and 200 response but incomplete\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan");
    try {
      icapClient.setKeyIcapPreview("Methods");
      icapClient.setSubStringFromKeyIcapPreview("RESPMOD");
      icapClient.connect();
      assertEquals(4096, icapClient.getPreviewSize());
      icapClient.setSubStringFromKeyIcapPreview("INCORRECT");
      icapClient.connect();
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception is intended");
    }
    icapClient.close();

    // Connection then scan with preview = 100 << file size + 100 Preview +
    // 200 Final
    logger.warn(
        "\n=======================\nSERVER START UP with new PREVIEW << " +
        "file size and 100 intermediary status and " +
        "200 final\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 100);
    try {
      icapClient.connect();
      assertEquals(100, icapClient.getPreviewSize());
      icapClient.setSendLength(100);
      IcapServerHandler.setIntermediaryStatus(100);
      IcapServerHandler.setFinalStatus(200);
      assertFalse(icapClient.scanFile(file.getAbsolutePath()));
    } catch (IcapException e) {
      logger.error(e);
      fail("Should not raised an exception");
    }
    icapClient.close();

    // Connection then scan with preview = 100 << file size + 100 Preview +
    // 500 Final
    logger.warn(
        "\n=======================\nSERVER START UP with new PREVIEW << " +
        "file size and 100 intermediary status and " +
        "500 final\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 100);
    try {
      icapClient.connect();
      assertEquals(100, icapClient.getPreviewSize());
      icapClient.setSendLength(100);
      IcapServerHandler.setIntermediaryStatus(100);
      IcapServerHandler.setFinalStatus(500);
      assertFalse(icapClient.scanFile(file.getAbsolutePath()));
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception is intended");
    }
    icapClient.close();

    // Connection then scan with preview = 100 << file size + 100 Preview +
    // Not found ICAP KEY 204
    logger.warn(
        "\n=======================\nSERVER START UP with new PREVIEW << " +
        "file size and 100 intermediary status and " +
        "Not found ICAP Key 204\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 100);
    try {
      icapClient.connect();
      assertEquals(100, icapClient.getPreviewSize());
      icapClient.setSendLength(100);
      icapClient.setKeyIcap204("Options-TTL");
      icapClient.setSubStringFromKeyIcap204("600");
      IcapServerHandler.setIntermediaryStatus(100);
      IcapServerHandler.setFinalStatus(204);
      assertTrue(icapClient.scanFile(file.getAbsolutePath()));
      logger.warn("Now with error");
      icapClient.setSubStringFromKeyIcap204("500");
      assertFalse(icapClient.scanFile(file.getAbsolutePath()));
    } catch (IcapException e) {
      logger.error(e);
      fail("Should not raised an exception");
    }
    icapClient.close();

    // Connection then scan with preview = 100 << file size + 100 Preview +
    // Not found ICAP KEY 200
    logger.warn(
        "\n=======================\nSERVER START UP with new PREVIEW << " +
        "file size and 100 intermediary status and " +
        "Not found ICAP Key 200\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 100);
    try {
      icapClient.connect();
      assertEquals(100, icapClient.getPreviewSize());
      icapClient.setSendLength(100);
      icapClient.setKeyIcap204(null);
      icapClient.setSubStringFromKeyIcap204(null);
      icapClient.setKeyIcap200("Options-TTL");
      icapClient.setSubStringFromKeyIcap200("600");
      IcapServerHandler.setIntermediaryStatus(100);
      IcapServerHandler.setFinalStatus(200);
      assertTrue(icapClient.scanFile(file.getAbsolutePath()));
      logger.warn("Now with error");
      icapClient.setSubStringFromKeyIcap200("500");
      assertFalse(icapClient.scanFile(file.getAbsolutePath()));
    } catch (IcapException e) {
      logger.error(e);
      fail("Should not raised an exception");
    }
    icapClient.close();

    // Connection then scan with preview = 100 << file size + 100 Preview +
    // Not found HTTP substring
    logger.warn(
        "\n=======================\nSERVER START UP with new PREVIEW << " +
        "file size and 100 intermediary status and " +
        "Not found HTTP Substring\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 100);
    try {
      icapClient.connect();
      assertEquals(100, icapClient.getPreviewSize());
      icapClient.setSendLength(100);
      icapClient.setKeyIcap204(null);
      icapClient.setSubStringFromKeyIcap204(null);
      icapClient.setKeyIcap200(null);
      icapClient.setSubStringFromKeyIcap200(null);
      icapClient.setSubstringHttpStatus200("WaarpFakeIcap");
      IcapServerHandler.setIntermediaryStatus(100);
      IcapServerHandler.setFinalStatus(200);
      assertTrue(icapClient.scanFile(file.getAbsolutePath()));
      logger.warn("Now with error");
      icapClient.setSubstringHttpStatus200("WaarpNotIcap");
      assertFalse(icapClient.scanFile(file.getAbsolutePath()));
    } catch (IcapException e) {
      logger.error(e);
      fail("Should not raised an exception");
    }
    icapClient.close();

    // Connection then scan with preview = 100 << file size + 100 Preview +
    // Not found HTTP substring
    logger.warn(
        "\n=======================\nSERVER START UP with new PREVIEW << " +
        "file size and 100 intermediary status and " +
        "204 but EICARTEST specified as file\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 100);
    try {
      icapClient.connect();
      assertEquals(100, icapClient.getPreviewSize());
      icapClient.setSendLength(100);
      IcapServerHandler.resetJunitStatus();
      assertTrue(icapClient.scanFile(IcapClient.EICARTEST));
      logger.warn("Now with error");
      IcapServerHandler.setFinalStatus(200);
      assertFalse(icapClient.scanFile(IcapClient.EICARTEST));
    } catch (IcapException e) {
      logger.error(e);
      fail("Should not raised an exception");
    }
    icapClient.close();

    // Connection then scan too big file
    logger.warn("\n=======================\nSERVER START UP with " +
                "too big file\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 100);
    try {
      icapClient.connect();
      icapClient.setMaxSize(900);
      assertEquals(100, icapClient.getPreviewSize());
      icapClient.setSendLength(100);
      icapClient.scanFile(file.getAbsolutePath());
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception is intended");
    }
    icapClient.close();

    // Connection then scan not existing file
    logger.warn("\n=======================\nSERVER START UP with " +
                "no existing file\n=======================");
    icapClient = new IcapClient("127.0.0.1", 9999, "avscan", 100);
    try {
      icapClient.connect();
      assertEquals(100, icapClient.getPreviewSize());
      icapClient.setSendLength(100);
      file.delete();
      icapClient.scanFile(file.getAbsolutePath());
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception is intended");
    }
    icapClient.close();
  }

  /**
   * Method: getPreviewSize()
   */
  @Test
  public void testGetPreviewSize() throws Exception {
    IcapClient icapClient = new IcapClient("127.0.0.1", 1234, "avscan");
    assertEquals(0, icapClient.getPreviewSize());
    assertEquals(8192, icapClient.setPreviewSize(8192).getPreviewSize());
    assertEquals(0, icapClient.setPreviewSize(0).getPreviewSize());
    try {
      icapClient.setPreviewSize(-1);
      fail("Should failed");
    } catch (IllegalArgumentException e) {
      assertEquals(0, icapClient.getPreviewSize());
    }
  }

  /**
   * Method: getReceiveLength()
   */
  @Test
  public void testGetReceiveLength() throws Exception {
    IcapClient icapClient = new IcapClient("127.0.0.1", 1234, "avscan");
    assertEquals(64 * 1024, icapClient.getReceiveLength());
    assertEquals(8192, icapClient.setReceiveLength(8192).getReceiveLength());
    assertEquals(100, icapClient.setReceiveLength(100).getReceiveLength());
    try {
      icapClient.setReceiveLength(10);
      fail("Should failed");
    } catch (IllegalArgumentException e) {
      assertEquals(100, icapClient.getReceiveLength());
    }
  }

  /**
   * Method: getSendLength()
   */
  @Test
  public void testGetSendLength() throws Exception {
    IcapClient icapClient = new IcapClient("127.0.0.1", 1234, "avscan");
    assertEquals(8192, icapClient.getSendLength());
    assertEquals(4096, icapClient.setSendLength(4096).getSendLength());
    assertEquals(100, icapClient.setSendLength(100).getSendLength());
    try {
      icapClient.setSendLength(10);
      fail("Should failed");
    } catch (IllegalArgumentException e) {
      assertEquals(100, icapClient.getSendLength());
    }
  }

  /**
   * Method: getMaxSize()
   */
  @Test
  public void testGetMaxSize() throws Exception {
    IcapClient icapClient = new IcapClient("127.0.0.1", 1234, "avscan");
    assertEquals(Integer.MAX_VALUE, icapClient.getMaxSize());
    assertEquals(4096, icapClient.setMaxSize(4096).getMaxSize());
    assertEquals(100, icapClient.setMaxSize(100).getMaxSize());
    try {
      icapClient.setMaxSize(10);
      fail("Should failed");
    } catch (IllegalArgumentException e) {
      assertEquals(100, icapClient.getMaxSize());
    }
  }


  /**
   * Method: getSubstringHttpStatus200()
   */
  @Test
  public void testGetSubstringHttpStatus200() throws Exception {
    IcapClient icapClient = new IcapClient("127.0.0.1", 1234, "avscan");
    assertNull(icapClient.getSubstringHttpStatus200());
    assertEquals("test", icapClient.setSubstringHttpStatus200("test")
                                   .getSubstringHttpStatus200());
    assertNull(
        icapClient.setSubstringHttpStatus200(null).getSubstringHttpStatus200());
    assertNull(
        icapClient.setSubstringHttpStatus200("").getSubstringHttpStatus200());
  }

  /**
   * Method: getKeyIcapPreview()
   */
  @Test
  public void testGetKeyIcapPreview() throws Exception {
    IcapClient icapClient = new IcapClient("127.0.0.1", 1234, "avscan");
    assertNull(icapClient.getKeyIcapPreview());
    assertEquals("test",
                 icapClient.setKeyIcapPreview("test").getKeyIcapPreview());
    assertNull(icapClient.setKeyIcapPreview(null).getKeyIcapPreview());
    assertNull(icapClient.setKeyIcapPreview("").getKeyIcapPreview());
  }

  /**
   * Method: getSubStringFromKeyIcapPreview()
   */
  @Test
  public void testGetSubStringFromKeyIcapPreview() throws Exception {
    IcapClient icapClient = new IcapClient("127.0.0.1", 1234, "avscan");
    assertNull(icapClient.getSubStringFromKeyIcapPreview());
    assertEquals("test", icapClient.setSubStringFromKeyIcapPreview("test")
                                   .getSubStringFromKeyIcapPreview());
    assertNull(icapClient.setSubStringFromKeyIcapPreview(null)
                         .getSubStringFromKeyIcapPreview());
    assertNull(icapClient.setSubStringFromKeyIcapPreview("")
                         .getSubStringFromKeyIcapPreview());
  }

  /**
   * Method: getKeyIcap200()
   */
  @Test
  public void testGetKeyIcap200() throws Exception {
    IcapClient icapClient = new IcapClient("127.0.0.1", 1234, "avscan");
    assertNull(icapClient.getKeyIcap200());
    assertEquals("test", icapClient.setKeyIcap200("test").getKeyIcap200());
    assertNull(icapClient.setKeyIcap200(null).getKeyIcap200());
    assertNull(icapClient.setKeyIcap200("").getKeyIcap200());
  }

  /**
   * Method: getSubStringFromKeyIcap200()
   */
  @Test
  public void testGetSubStringFromKeyIcap200() throws Exception {
    IcapClient icapClient = new IcapClient("127.0.0.1", 1234, "avscan");
    assertNull(icapClient.getSubStringFromKeyIcap200());
    assertEquals("test", icapClient.setSubStringFromKeyIcap200("test")
                                   .getSubStringFromKeyIcap200());
    assertNull(icapClient.setSubStringFromKeyIcap200(null)
                         .getSubStringFromKeyIcap200());
    assertNull(
        icapClient.setSubStringFromKeyIcap200("").getSubStringFromKeyIcap200());
  }

  /**
   * Method: getKeyIcap204()
   */
  @Test
  public void testGetKeyIcap204() throws Exception {
    IcapClient icapClient = new IcapClient("127.0.0.1", 1234, "avscan");
    assertNull(icapClient.getKeyIcap204());
    assertEquals("test", icapClient.setKeyIcap204("test").getKeyIcap204());
    assertNull(icapClient.setKeyIcap204(null).getKeyIcap204());
    assertNull(icapClient.setKeyIcap204("").getKeyIcap204());
  }

  /**
   * Method: getSubStringFromKeyIcap204()
   */
  @Test
  public void testGetSubStringFromKeyIcap204() throws Exception {
    IcapClient icapClient = new IcapClient("127.0.0.1", 1234, "avscan");
    assertNull(icapClient.getSubStringFromKeyIcap204());
    assertEquals("test", icapClient.setSubStringFromKeyIcap204("test")
                                   .getSubStringFromKeyIcap204());
    assertNull(icapClient.setSubStringFromKeyIcap204(null)
                         .getSubStringFromKeyIcap204());
    assertNull(
        icapClient.setSubStringFromKeyIcap204("").getSubStringFromKeyIcap204());
  }

  static class IcapClientForTest extends IcapClient {

    public IcapClientForTest(final String serverIP, final int port,
                             final String icapService) {
      super(serverIP, port, icapService);
    }

    public IcapClientForTest(final String serverIP, final int port,
                             final String icapService, final int previewSize) {
      super(serverIP, port, icapService, previewSize);
    }

    public int readChunkForTest(final InputStream fileInputStream,
                                final byte[] buffer, final int length)
        throws IcapException {
      return readChunk(fileInputStream, buffer, length);
    }

    public String getHeaderHttpForTest(final InputStream in)
        throws IcapException {
      this.in = in;
      return getHeaderHttp();
    }

    public String getHeaderIcapForTest(final InputStream in)
        throws IcapException {
      this.in = in;
      return getHeaderIcap();
    }
  }

  /**
   * Method: readChunk(final InputStream fileInputStream, final byte[] buffer, final int length)
   */
  @Test
  public void testPrivateReadChunk() throws Exception {
    IcapClientForTest icapClient =
        new IcapClientForTest("127.0.0.1", 1234, "avscan");
    InputStream in = new ByteArrayInputStream("".getBytes());
    byte[] buffer = new byte[1024];
    int len = 2048;
    try {
      int read = icapClient.readChunkForTest(in, buffer, len);
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Correct exception since len > buffer size: {}",
                   e.getMessage());
    }
    len = 1000;
    try {
      int read = icapClient.readChunkForTest(in, buffer, len);
      assertEquals(-1, read);
    } catch (IcapException e) {
      logger.error(e);
      fail("Incorrect exception raised");
    }
    in.close();
    in = new ByteArrayInputStream("12345".getBytes());
    try {
      int read = icapClient.readChunkForTest(in, buffer, len);
      assertEquals(5, read);
      read = icapClient.readChunkForTest(in, buffer, len);
      assertEquals(-1, read);
    } catch (IcapException e) {
      logger.error(e);
      fail("Incorrect exception raised");
    }
    in.close();
    byte[] from = new byte[1001];
    Arrays.fill(from, (byte) 1);
    in = new ByteArrayInputStream(from);
    try {
      int read = icapClient.readChunkForTest(in, buffer, len);
      assertEquals(1000, read);
      read = icapClient.readChunkForTest(in, buffer, len);
      assertEquals(1, read);
      read = icapClient.readChunkForTest(in, buffer, len);
      assertEquals(-1, read);
    } catch (IcapException e) {
      logger.error(e);
      fail("Incorrect exception raised");
    }
    in.close();
    from = new byte[1050];
    len = 1024;
    Arrays.fill(from, (byte) 1);
    in = new ByteArrayInputStream(from);
    try {
      int read = icapClient.readChunkForTest(in, buffer, len);
      assertEquals(1024, read);
      read = icapClient.readChunkForTest(in, buffer, len);
      assertEquals(26, read);
      read = icapClient.readChunkForTest(in, buffer, len);
      assertEquals(-1, read);
    } catch (IcapException e) {
      logger.error(e);
      fail("Incorrect exception raised");
    }
    in.close();
  }

  /**
   * Method: getHeader(String terminator)
   */
  @Test
  public void testPrivateGetHeader() throws Exception {
    IcapClientForTest icapClient =
        new IcapClientForTest("127.0.0.1", 1234, "avscan");
    InputStream in = new ByteArrayInputStream("".getBytes());
    try {
      String readed = icapClient.getHeaderIcapForTest(in);
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Correct exception: {}", e.getMessage());
    }
    in.close();
    in = new ByteArrayInputStream(
        ("12345" + IcapClient.ICAP_TERMINATOR).getBytes());
    try {
      String readed = icapClient.getHeaderIcapForTest(in);
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Correct exception: {}", e.getMessage());
    }
    in.close();
    in = new ByteArrayInputStream(
        ("12345" + IcapClient.HTTP_TERMINATOR).getBytes());
    try {
      String readed = icapClient.getHeaderHttpForTest(in);
      assertTrue(readed.startsWith("12345" + IcapClient.HTTP_TERMINATOR));
      logger.debug("Correct");
    } catch (IcapException e) {
      logger.warn(e);
      fail("Should not raised an exception");
    }
    in.close();
    in = new ByteArrayInputStream(
        ("123456789012345" + IcapClient.ICAP_TERMINATOR).getBytes());
    try {
      String readed = icapClient.getHeaderIcapForTest(in);
      assertEquals(19, readed.length());
    } catch (IcapException e) {
      logger.error(e);
      fail("Incorrect exception raised");
    }
    in.close();
    in = new ByteArrayInputStream(
        ("123456789012345" + IcapClient.HTTP_TERMINATOR).getBytes());
    try {
      String readed = icapClient.getHeaderHttpForTest(in);
      assertEquals(20, readed.length());
    } catch (IcapException e) {
      logger.error(e);
      fail("Incorrect exception raised");
    }
    in.close();
    in = new ByteArrayInputStream(
        ("123456789012345" + IcapClient.ICAP_TERMINATOR).getBytes());
    try {
      String readed = icapClient.getHeaderHttpForTest(in);
      assertTrue(
          readed.startsWith("123456789012345" + IcapClient.ICAP_TERMINATOR));
      logger.debug("Correct");
    } catch (IcapException e) {
      logger.warn(e);
      fail("Should not raised an exception");
    }
    in.close();
    in = new ByteArrayInputStream(("12345678901234567890").getBytes());
    try {
      String readed = icapClient.getHeaderHttpForTest(in);
      logger.debug("Correct");
    } catch (IcapException e) {
      logger.warn(e);
      fail("Should not raised an exception");
    }
    in.close();
    in = new ByteArrayInputStream(("12345678901234567890").getBytes());
    try {
      String readed = icapClient.getHeaderIcapForTest(in);
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Correct exception: {}", e.getMessage());
    }
    in.close();
    byte[] buffer = new byte[100000];
    Arrays.fill(buffer, (byte) 'a');
    in = new ByteArrayInputStream(buffer);
    try {
      String readed = icapClient.getHeaderIcapForTest(in);
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Correct exception: {}", e.getMessage());
    }
    in.close();
    in = new ByteArrayInputStream(buffer);
    try {
      String readed = icapClient.getHeaderHttpForTest(in);
      logger.debug("Correct");
    } catch (IcapException e) {
      logger.warn(e);
      fail("Should not raised an exception");
    }
    in.close();
  }

} 
