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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.icap.server.IcapServer;
import org.waarp.icap.server.IcapServerHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * IcapScanFile Tester.
 */
public class IcapScanFileTest {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(IcapScanFileTest.class);

  @Before
  public void before() throws Exception {
    IcapServerHandler.resetJunitStatus();
  }

  @After
  public void after() throws Exception {
    IcapServer.shutdown();
  }

  /**
   * Method: getIcapScanFileArgs(String[] args)
   */
  @Test
  public void testGetIcapArgs() throws Exception {
    try {
      IcapScanFile.getIcapScanFileArgs(null);
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Exception intended", e);
    }
    try {
      IcapScanFile.getIcapScanFileArgs(new String[0]);
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Exception intended", e);
    }
    File file = new File("/tmp/toscan.bin");
    String[] fullArgs;
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorMove",
          "/tmp/error.bin", "-keyPreview", "Methods", "-stringPreview",
          "RESPMOD", "-key204", "Options-TTL", "-string204", "600", "-key200",
          "Options-TTL", "-string200", "600", "-stringHttp", "WaarpFakeIcap",
          "-timeout", "100000"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertEquals("/tmp/error.bin", icapScanFile.getPathMoveError());
      assertFalse(icapScanFile.isDeleteOnError());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          IcapScanFile.MODEL_ARG, IcapModel.DEFAULT_MODEL.name(),
          IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
          "-port", "9999", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorMove",
          "/tmp/error.bin", "-keyPreview", "Methods", "-stringPreview",
          "RESPMOD", "-key204", "Options-TTL", "-string204", "600", "-key200",
          "Options-TTL", "-string200", "600", "-stringHttp", "WaarpFakeIcap",
          "-timeout", "100000"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertEquals("/tmp/error.bin", icapScanFile.getPathMoveError());
      assertFalse(icapScanFile.isDeleteOnError());
      assertEquals(IcapModel.DEFAULT_MODEL, icapScanFile.getIcapModel());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          IcapScanFile.MODEL_ARG, IcapModel.ICAP_VIRUS_SCAN.name(),
          IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
          "-port", "9999", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorMove",
          "/tmp/error.bin", "-keyPreview", "Methods", "-stringPreview",
          "RESPMOD", "-key204", "Options-TTL", "-string204", "600", "-key200",
          "Options-TTL", "-string200", "600", "-stringHttp", "WaarpFakeIcap",
          "-timeout", "100000"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertEquals("/tmp/error.bin", icapScanFile.getPathMoveError());
      assertFalse(icapScanFile.isDeleteOnError());
      assertEquals(IcapModel.ICAP_VIRUS_SCAN, icapScanFile.getIcapModel());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          IcapScanFile.MODEL_ARG, IcapModel.ICAP_CLAMAV.name(),
          IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
          "-port", "9999", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorMove",
          "/tmp/error.bin", "-keyPreview", "Methods", "-stringPreview",
          "RESPMOD", "-key204", "Options-TTL", "-string204", "600", "-key200",
          "Options-TTL", "-string200", "600", "-stringHttp", "WaarpFakeIcap",
          "-timeout", "100000"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertEquals("/tmp/error.bin", icapScanFile.getPathMoveError());
      assertFalse(icapScanFile.isDeleteOnError());
      assertEquals(IcapModel.ICAP_CLAMAV, icapScanFile.getIcapModel());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          IcapScanFile.MODEL_ARG, IcapModel.ICAP_AVSCAN.name(),
          IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
          "-port", "9999", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorMove",
          "/tmp/error.bin", "-keyPreview", "Methods", "-stringPreview",
          "RESPMOD", "-key204", "Options-TTL", "-string204", "600", "-key200",
          "Options-TTL", "-string200", "600", "-stringHttp", "WaarpFakeIcap",
          "-timeout", "100000"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertEquals("/tmp/error.bin", icapScanFile.getPathMoveError());
      assertFalse(icapScanFile.isDeleteOnError());
      assertEquals(IcapModel.ICAP_AVSCAN, icapScanFile.getIcapModel());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          IcapScanFile.MODEL_ARG, "DEFAULT_MODEL", IcapScanFile.FILE_ARG,
          file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorMove",
          "/tmp/error.bin", "-keyPreview", "Methods", "-stringPreview",
          "RESPMOD", "-key204", "Options-TTL", "-string204", "600", "-key200",
          "Options-TTL", "-string200", "600", "-stringHttp", "WaarpFakeIcap",
          "-timeout", "100000"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception intended", e);
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorDelete",
          "-keyPreview", "Methods", "-stringPreview", "RESPMOD", "-key204",
          "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
          "-string200", "600", "-stringHttp", "WaarpFakeIcap"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertNull(icapScanFile.getPathMoveError());
      assertTrue(icapScanFile.isDeleteOnError());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorDelete",
          "-keyPreview", "Methods", "-stringPreview", "RESPMOD", "-key204",
          "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
          "-string200", "600", "-stringHttp", "WaarpFakeIcap", "-logger",
          "DEbug"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertNull(icapScanFile.getPathMoveError());
      assertTrue(icapScanFile.isDeleteOnError());
      assertEquals(WaarpLogLevel.DEBUG, icapScanFile.getLogLevel());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorDelete",
          "-keyPreview", "Methods", "-stringPreview", "RESPMOD", "-key204",
          "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
          "-string200", "600", "-stringHttp", "WaarpFakeIcap", "-logger", "Info"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertNull(icapScanFile.getPathMoveError());
      assertTrue(icapScanFile.isDeleteOnError());
      assertEquals(WaarpLogLevel.INFO, icapScanFile.getLogLevel());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorDelete",
          "-keyPreview", "Methods", "-stringPreview", "RESPMOD", "-key204",
          "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
          "-string200", "600", "-stringHttp", "WaarpFakeIcap", "-logger", "Warn"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertNull(icapScanFile.getPathMoveError());
      assertTrue(icapScanFile.isDeleteOnError());
      assertEquals(WaarpLogLevel.WARN, icapScanFile.getLogLevel());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorDelete",
          "-keyPreview", "Methods", "-stringPreview", "RESPMOD", "-key204",
          "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
          "-string200", "600", "-stringHttp", "WaarpFakeIcap", "-logger",
          "Error"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertNull(icapScanFile.getPathMoveError());
      assertTrue(icapScanFile.isDeleteOnError());
      assertEquals(WaarpLogLevel.ERROR, icapScanFile.getLogLevel());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          IcapScanFile.FILE_ARG, file.getAbsolutePath(), IcapScanFile.TO_ARG,
          "127" + ".0.0.1", "-port", "9999", IcapScanFile.SERVICE_ARG, "avscan",
          "-previewSize", "2048", "-blockSize", "2048", "-receiveSize", "2048",
          "-maxSize", "100000", "-errorDelete", "-keyPreview", "Methods",
          "-stringPreview", "RESPMOD", "-key204", "Options-TTL", "-string204",
          "600", "-key200", "Options-TTL", "-string200", "600", "-stringHttp",
          "WaarpFakeIcap", "-logger", "Unknown"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertEquals("127.0.0.1", icapScanFile.getServerIP());
      assertNull(icapScanFile.getPathMoveError());
      assertTrue(icapScanFile.isDeleteOnError());
      assertNull(icapScanFile.getLogLevel());
      assertNull(icapScanFile.setFilePath(null).getFilePath());
      assertNull(icapScanFile.setServerIP(null).getServerIP());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-keyPreview",
          "Methods", "-stringPreview", "RESPMOD", "-key204", "Options-TTL",
          "-string204", "600", "-key200", "Options-TTL", "-string200", "600",
          "-stringHttp", "WaarpFakeIcap", "-timeout", "100000", "-sendOnError"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertEquals(null, icapScanFile.getPathMoveError());
      assertFalse(icapScanFile.isDeleteOnError());
      assertTrue(icapScanFile.isSendOnError());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-keyPreview",
          "Methods", "-stringPreview", "RESPMOD", "-key204", "Options-TTL",
          "-string204", "600", "-key200", "Options-TTL", "-string200", "600",
          "-stringHttp", "WaarpFakeIcap", "-timeout", "100000",
          "-ignoreNetworkError"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertEquals(null, icapScanFile.getPathMoveError());
      assertFalse(icapScanFile.isDeleteOnError());
      assertTrue(icapScanFile.isIgnoreNetworkError());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-keyPreview",
          "Methods", "-stringPreview", "RESPMOD", "-key204", "Options-TTL",
          "-string204", "600", "-key200", "Options-TTL", "-string200", "600",
          "-stringHttp", "WaarpFakeIcap", "-timeout", "100000",
          "-ignoreTooBigFileError"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertEquals(null, icapScanFile.getPathMoveError());
      assertFalse(icapScanFile.isDeleteOnError());
      assertTrue(icapScanFile.isIgnoreTooBigFileError());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorMove",
          "/tmp/error.bin", "-keyPreview", "Methods", "-stringPreview",
          "RESPMOD", "-key204", "Options-TTL", "-string204", "600", "-key200",
          "Options-TTL", "-string200", "600", "-stringHttp", "WaarpFakeIcap",
          "-timeout", "100000", "--", "-test"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertEquals("/tmp/error.bin", icapScanFile.getPathMoveError());
      assertFalse(icapScanFile.isDeleteOnError());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorMove",
          "/tmp/error.bin", "-keyPreview", "Methods", "-stringPreview",
          "RESPMOD", "-key204", "Options-TTL", "-string204", "600", "-key200",
          "Options-TTL", "-string200", "600", "-stringHttp", "WaarpFakeIcap",
          "--", "-timeout", "a"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(file.getAbsolutePath(), icapScanFile.getFilePath());
      assertEquals("/tmp/error.bin", icapScanFile.getPathMoveError());
      assertFalse(icapScanFile.isDeleteOnError());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "-1",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorDelete",
          "-keyPreview", "Methods", "-stringPreview", "RESPMOD", "-key204",
          "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
          "-string200", "600", "-stringHttp", "WaarpFakeIcap"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception intended", e);
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "-1", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorDelete",
          "-keyPreview", "Methods", "-stringPreview", "RESPMOD", "-key204",
          "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
          "-string200", "600", "-stringHttp", "WaarpFakeIcap"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception intended", e);
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "10",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorDelete",
          "-keyPreview", "Methods", "-stringPreview", "RESPMOD", "-key204",
          "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
          "-string200", "600", "-stringHttp", "WaarpFakeIcap"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception intended", e);
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "10", "-maxSize", "100000", "-errorDelete",
          "-keyPreview", "Methods", "-stringPreview", "RESPMOD", "-key204",
          "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
          "-string200", "600", "-stringHttp", "WaarpFakeIcap"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception intended", e);
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "10", "-errorDelete",
          "-keyPreview", "Methods", "-stringPreview", "RESPMOD", "-key204",
          "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
          "-string200", "600", "-stringHttp", "WaarpFakeIcap"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception intended", e);
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "10", "-errorDelete",
          "-keyPreview", "Methods", "-stringPreview", "RESPMOD", "-key204",
          "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
          "-string200", "600", "-stringHttp", "WaarpFakeIcap", "-wrongOption"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception intended", e);
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "10", "-errorDelete",
          "-keyPreview", "Methods", "-stringPreview", "RESPMOD", "-key204",
          "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
          "-string200", "600", "-stringHttp"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception intended", e);
    }
    try {
      fullArgs = new String[] {
          "-file", file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
          "-service", "avscan", "-previewSize", "2048", "-blockSize", "2048",
          "-receiveSize", "2048", "-maxSize", "100000", "-errorMove",
          "/tmp/error.bin", "-keyPreview", "Methods", "-stringPreview",
          "RESPMOD", "-key204", "Options-TTL", "-string204", "600", "-key200",
          "Options-TTL", "-string200", "600", "-stringHttp", "WaarpFakeIcap",
          "-timeout", "10"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      fail("Should raised an exception");
    } catch (IcapException e) {
      logger.debug("Exception intended", e);
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
   * Method: getIcapClient(final IcapScanFile icapScanFile)
   */
  @Test
  public void testGetIcapClient() throws Exception {
    try {
      IcapScanFile.getIcapClient(null);
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Exception intended", e);
    }
    File file = new File("/tmp/toscan.bin");
    try {
      String[] fullArgs = new String[] {
          IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
          "-port", "9999", "-service", "avscan", "-previewSize", "2048",
          "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100000",
          "-errorDelete", "-keyPreview", "Methods", "-stringPreview", "RESPMOD",
          "-key204", "Options-TTL", "-string204", "600", "-key200",
          "Options-TTL", "-string200", "600", "-stringHttp", "WaarpFakeIcap",
          "-timeout", "100000"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      IcapClient icapClient = IcapScanFile.getIcapClient(icapScanFile);
      assertEquals("127.0.0.1", icapClient.getServerIP());
      assertEquals(9999, icapClient.getPort());
      assertEquals("avscan", icapClient.getIcapService());
      assertEquals(2048, icapClient.getPreviewSize());
      assertEquals(2048, icapClient.getReceiveLength());
      assertEquals(2048, icapClient.getSendLength());
      assertEquals(100000, icapClient.getMaxSize());
      assertEquals("Methods", icapClient.getKeyIcapPreview());
      assertEquals("RESPMOD", icapClient.getSubStringFromKeyIcapPreview());
      assertEquals("Options-TTL", icapClient.getKeyIcap204());
      assertEquals("600", icapClient.getSubStringFromKeyIcap204());
      assertEquals("Options-TTL", icapClient.getKeyIcap200());
      assertEquals("600", icapClient.getSubStringFromKeyIcap200());
      assertEquals("WaarpFakeIcap", icapClient.getSubstringHttpStatus200());
      assertEquals(100000, icapClient.getTimeout());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      String[] fullArgs = new String[] {
          IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
          "-service", "avscan"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      IcapClient icapClient = IcapScanFile.getIcapClient(icapScanFile);
      assertEquals("127.0.0.1", icapClient.getServerIP());
      assertEquals(IcapClient.DEFAULT_ICAP_PORT, icapClient.getPort());
      assertEquals("avscan", icapClient.getIcapService());
      assertEquals(0, icapClient.getPreviewSize());
      assertEquals(IcapClient.STD_RECEIVE_LENGTH,
                   icapClient.getReceiveLength());
      assertEquals(IcapClient.STD_SEND_LENGTH, icapClient.getSendLength());
      assertEquals(Integer.MAX_VALUE, icapClient.getMaxSize());
      assertNull(icapClient.getKeyIcapPreview());
      assertNull(icapClient.getSubStringFromKeyIcapPreview());
      assertNull(icapClient.getKeyIcap204());
      assertNull(icapClient.getSubStringFromKeyIcap204());
      assertNull(icapClient.getKeyIcap200());
      assertNull(icapClient.getSubStringFromKeyIcap200());
      assertNull(icapClient.getSubstringHttpStatus200());
      assertEquals(IcapClient.DEFAULT_TIMEOUT, icapClient.getTimeout());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
  }

  /**
   * Method: finalizeOnError(final IcapScanFile icapScanFile)
   */
  @Test
  public void testFinalizeOnError() throws Exception {
    try {
      IcapScanFile.finalizeOnError(null);
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Exception intended", e);
    }
    File file = new File("/tmp/toscan.bin");
    File error = new File("/tmp/error.bin");
    File dirError = new File("/tmp/error");
    dirError.mkdirs();
    File dirFileError = new File("/tmp/error/toscan.bin");
    File dirNotExist = new File("/tmp/errorNotExists/notExist");
    dirNotExist.delete();
    dirNotExist.getParentFile().delete();
    try {
      createFile(file, 1000);
      String[] fullArgs = new String[] {
          IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
          "-port", "9999", "-service", "avscan", "-previewSize", "2048",
          "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100000",
          "-errorDelete", "-keyPreview", "Methods", "-stringPreview", "RESPMOD",
          "-key204", "Options-TTL", "-string204", "600", "-key200",
          "Options-TTL", "-string200", "600", "-stringHttp", "WaarpFakeIcap"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertNull(icapScanFile.getPathMoveError());
      assertTrue(icapScanFile.isDeleteOnError());
      IcapScanFile.finalizeOnError(icapScanFile);
      assertFalse(error.exists());
      assertFalse(dirFileError.exists());
      assertFalse(file.exists());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      createFile(file, 1000);
      String[] fullArgs = new String[] {
          IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
          "-port", "9999", "-service", "avscan", "-previewSize", "2048",
          "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100000",
          "-errorMove", error.getAbsolutePath(), "-keyPreview", "Methods",
          "-stringPreview", "RESPMOD", "-key204", "Options-TTL", "-string204",
          "600", "-key200", "Options-TTL", "-string200", "600", "-stringHttp",
          "WaarpFakeIcap"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(error.getAbsolutePath(), icapScanFile.getPathMoveError());
      assertFalse(icapScanFile.isDeleteOnError());
      IcapScanFile.finalizeOnError(icapScanFile);
      assertTrue(error.exists());
      assertFalse(dirFileError.exists());
      assertFalse(file.exists());
      error.delete();
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      createFile(file, 1000);
      String[] fullArgs = new String[] {
          IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
          "-port", "9999", "-service", "avscan", "-previewSize", "2048",
          "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100000",
          "-errorMove", dirError.getAbsolutePath(), "-keyPreview", "Methods",
          "-stringPreview", "RESPMOD", "-key204", "Options-TTL", "-string204",
          "600", "-key200", "Options-TTL", "-string200", "600", "-stringHttp",
          "WaarpFakeIcap"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(dirError.getAbsolutePath(), icapScanFile.getPathMoveError());
      assertFalse(icapScanFile.isDeleteOnError());
      IcapScanFile.finalizeOnError(icapScanFile);
      assertFalse(error.exists());
      assertTrue(dirFileError.exists());
      assertFalse(file.exists());
      dirFileError.delete();
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
    try {
      createFile(file, 1000);
      String[] fullArgs = new String[] {
          IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
          "-port", "9999", "-service", "avscan", "-previewSize", "2048",
          "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100000",
          "-errorMove", dirNotExist.getAbsolutePath(), "-keyPreview", "Methods",
          "-stringPreview", "RESPMOD", "-key204", "Options-TTL", "-string204",
          "600", "-key200", "Options-TTL", "-string200", "600", "-stringHttp",
          "WaarpFakeIcap"
      };
      IcapScanFile icapScanFile = IcapScanFile.getIcapScanFileArgs(fullArgs);
      assertEquals(dirNotExist.getAbsolutePath(),
                   icapScanFile.getPathMoveError());
      assertFalse(icapScanFile.isDeleteOnError());
      IcapScanFile.finalizeOnError(icapScanFile);
      assertFalse(error.exists());
      assertFalse(dirFileError.exists());
      assertTrue(file.exists());
    } catch (IcapException e) {
      fail("Should not raised an exception");
    }
  }

  /**
   * Method: main(String[] args)
   */
  @Test
  public void testMain() throws Exception {
    try {
      IcapScanFile.scanFile(null);
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Exception intended", e);
    }
    try {
      IcapScanFile.scanFile(new String[] { "notEmpty" }, null);
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Exception intended", e);
    }
    try {
      IcapScanFile.scanFile((String[]) null, new String[] { "notEmpty" });
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Exception intended", e);
    }
    try {
      IcapScanFile.scanFile(new String[0]);
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Exception intended", e);
    }
    try {
      IcapScanFile.scanFile(new String[] { "notEmpty" }, new String[0]);
      fail("Should raised an exception");
    } catch (IllegalArgumentException e) {
      logger.debug("Exception intended", e);
    }
    File file = new File("/tmp/toscan.bin");
    createFile(file, 1000);
    String[] fullArgs = new String[] {
        IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
        "-port", "9999", "-service", "avscan", "-previewSize", "2048",
        "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100000",
        "-errorDelete", "-keyPreview", "Methods", "-stringPreview", "RESPMOD",
        "-key204", "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
        "-string200", "600", "-stringHttp", "WaarpFakeIcap"
    };
    // Server not ready
    assertEquals(IcapScanFile.STATUS_NETWORK_ISSUE,
                 IcapScanFile.scanFile(fullArgs));
    assertTrue(file.exists());
    // Server launch
    IcapServer.main(new String[] { "127.0.0.1", "9999" });
    logger.warn("Server started");
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile.scanFile(fullArgs));
    assertTrue(file.exists());
    fullArgs = new String[] {
        IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
        "-port", "9999", "-service", "avscan", "-previewSize", "2048",
        "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100000",
        "-errorDelete", "-keyPreview", "Methods", "-stringPreview", "RESPMOD",
        "-key204", "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
        "-string200", "600", "-stringHttp",
        "This is data that was returned by an origin server"
    };
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile.scanFile(fullArgs));
    // With bad size
    fullArgs = new String[] {
        IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
        "-port", "9999", "-service", "avscan", "-previewSize", "2048",
        "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100",
        "-errorDelete", "-keyPreview", "Methods", "-stringPreview", "RESPMOD",
        "-key204", "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
        "-string200", "600", "-stringHttp",
        "This is data that was returned by an origin server"
    };
    assertEquals(IcapScanFile.STATUS_BAD_ARGUMENT,
                 IcapScanFile.scanFile(fullArgs));
    fullArgs = new String[] {
        IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
        "-port", "9999", "-service", "avscan", "-previewSize", "2048",
        "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100",
        "-errorDelete", "-keyPreview", "Methods", "-stringPreview", "RESPMOD",
        "-key204", "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
        "-string200", "600", "-stringHttp",
        "This is data that was returned by an origin server",
        "-ignoreTooBigFileError"
    };
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile.scanFile(fullArgs));

    // With TRACE
    logger.warn("With trace activated");
    fullArgs = new String[] {
        IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
        "-port", "9999", "-service", "avscan", "-previewSize", "2048",
        "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100000",
        "-errorDelete", "-keyPreview", "Methods", "-stringPreview", "RESPMOD",
        "-key204", "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
        "-string200", "600", "-stringHttp", "WaarpFakeIcap", "-logger", "Trace"
    };
    assertFalse(WaarpLoggerFactory.getLogLevel() == WaarpLogLevel.DEBUG);
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile.scanFile(fullArgs));
    // With DEBUG
    logger.warn("With debug activated");
    fullArgs = new String[] {
        IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
        "-port", "9999", "-service", "avscan", "-previewSize", "2048",
        "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100000",
        "-errorDelete", "-keyPreview", "Methods", "-stringPreview", "RESPMOD",
        "-key204", "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
        "-string200", "600", "-stringHttp", "WaarpFakeIcap", "-logger", "DEbug"
    };
    assertFalse(WaarpLoggerFactory.getLogLevel() == WaarpLogLevel.DEBUG);
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile.scanFile(fullArgs));
    // With INFO
    logger.warn("With info activated");
    fullArgs = new String[] {
        IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
        "-port", "9999", "-service", "avscan", "-previewSize", "2048",
        "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100000",
        "-errorDelete", "-keyPreview", "Methods", "-stringPreview", "RESPMOD",
        "-key204", "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
        "-string200", "600", "-stringHttp", "WaarpFakeIcap", "-logger", "Info"
    };
    assertFalse(WaarpLoggerFactory.getLogLevel() == WaarpLogLevel.DEBUG);
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile.scanFile(fullArgs));
    // With WARN
    logger.warn("With warn activated");
    fullArgs = new String[] {
        IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
        "-port", "9999", "-service", "avscan", "-previewSize", "2048",
        "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100000",
        "-errorDelete", "-keyPreview", "Methods", "-stringPreview", "RESPMOD",
        "-key204", "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
        "-string200", "600", "-stringHttp", "WaarpFakeIcap", "-logger", "Warn"
    };
    assertFalse(WaarpLoggerFactory.getLogLevel() == WaarpLogLevel.DEBUG);
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile.scanFile(fullArgs));
    // With ERROR
    logger.warn("With error activated");
    fullArgs = new String[] {
        IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
        "-port", "9999", "-service", "avscan", "-previewSize", "2048",
        "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100000",
        "-errorDelete", "-keyPreview", "Methods", "-stringPreview", "RESPMOD",
        "-key204", "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
        "-string200", "600", "-stringHttp", "WaarpFakeIcap", "-logger", "Error"
    };
    assertFalse(WaarpLoggerFactory.getLogLevel() == WaarpLogLevel.DEBUG);
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile.scanFile(fullArgs));
    // With Model
    logger.warn("With Model");
    fullArgs = new String[] {
        IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
        "-port", "9999", "-previewSize", "2048", "-blockSize", "2048",
        "-receiveSize", "2048", "-maxSize", "100000", "-errorDelete",
        "-keyPreview", "Methods", "-stringPreview", "RESPMOD", "-key204",
        "Options-TTL", "-string204", "600", "-key200", "Options-TTL",
        "-string200", "600", "-stringHttp", "WaarpFakeIcap"
    };
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile
        .scanFile(IcapModel.DEFAULT_MODEL.getDefaultArgs(), fullArgs));
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile
        .scanFile(IcapModel.ICAP_AVSCAN.getDefaultArgs(), fullArgs));
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile
        .scanFile(IcapModel.ICAP_CLAMAV.getDefaultArgs(), fullArgs));
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile
        .scanFile(IcapModel.ICAP_VIRUS_SCAN.getDefaultArgs(), fullArgs));
    // With Model name
    fullArgs = new String[] {
        IcapScanFile.MODEL_ARG, "DEFAULT_MODEL", IcapScanFile.FILE_ARG,
        file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
        "-previewSize", "2048", "-blockSize", "2048", "-receiveSize", "2048",
        "-maxSize", "100000", "-errorDelete", "-keyPreview", "Methods",
        "-stringPreview", "RESPMOD", "-key204", "Options-TTL", "-string204",
        "600", "-key200", "Options-TTL", "-string200", "600", "-stringHttp",
        "WaarpFakeIcap"
    };
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile.scanFile(fullArgs));
    fullArgs[1] = "ICAP_AVSCAN";
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile.scanFile(fullArgs));
    fullArgs[1] = "ICAP_CLAMAV";
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile.scanFile(fullArgs));
    fullArgs[1] = "ICAP_VIRUS_SCAN";
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile.scanFile(fullArgs));
    assertTrue(file.exists());
    assertFalse(WaarpLoggerFactory.getLogLevel() == WaarpLogLevel.DEBUG);
    // With error on arguments
    logger.warn("With bad arguments");
    fullArgs = new String[] {
        IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
        "-port", "9999", "-service", "avscan", "-previewSize", "2048",
        "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100000",
        "-errorDelete", "-keyPreview", "Methods", "-stringPreview", "RESPMOD",
        "-key204", "Options-TTL", "-string204", "WRONG", "-key200",
        "Options-TTL", "-string200", "600", "-stringHttp"
    };
    assertEquals(IcapScanFile.STATUS_BAD_ARGUMENT,
                 IcapScanFile.scanFile(fullArgs));
    assertTrue(file.exists());
    // With 404
    logger.warn("With 404");
    IcapServerHandler.setFinalStatus(404);
    fullArgs = new String[] {
        IcapScanFile.MODEL_ARG, "DEFAULT_MODEL", IcapScanFile.FILE_ARG,
        file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
        "-previewSize", "2048", "-blockSize", "2048", "-receiveSize",
        "2048", "-maxSize", "100000", "-errorDelete", "-keyPreview", "Methods",
        "-stringPreview", "RESPMOD", "-key204", "Options-TTL", "-string204",
        "600", "-stringHttp",
        "This is data that was returned by an origin server"
    };
    assertEquals(IcapScanFile.STATUS_ICAP_ISSUE,
                 IcapScanFile.scanFile(fullArgs));
    // With 200 but OK
    logger.warn("With 200 result but OK");
    IcapServerHandler.setFinalStatus(200);
    fullArgs = new String[] {
        IcapScanFile.MODEL_ARG, "DEFAULT_MODEL", IcapScanFile.FILE_ARG,
        file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999", "-service",
        "avscan", "-previewSize", "2048", "-blockSize", "2048", "-receiveSize",
        "2048", "-maxSize", "100000", "-errorDelete", "-keyPreview", "Methods",
        "-stringPreview", "RESPMOD", "-key204", "Options-TTL", "-string204",
        "600", "-stringHttp",
        "This is data that was returned by an origin server"
    };
    assertEquals(IcapScanFile.STATUS_BAD_ARGUMENT,
                 IcapScanFile.scanFile(fullArgs));
    fullArgs = new String[] {
        IcapScanFile.MODEL_ARG, "DEFAULT_MODEL", IcapScanFile.FILE_ARG,
        file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
        "-previewSize", "2048", "-blockSize", "2048", "-receiveSize", "2048",
        "-maxSize", "100000", "-errorDelete", "-keyPreview", "Methods",
        "-stringPreview", "RESPMOD", "-key204", "Options-TTL", "-string204",
        "600", "-stringHttp",
        "This is data that was returned by an origin server"
    };
    assertEquals(IcapScanFile.STATUS_OK, IcapScanFile.scanFile(fullArgs));
    fullArgs = new String[] {
        IcapScanFile.MODEL_ARG, "DEFAULT_MODEL", IcapScanFile.FILE_ARG,
        file.getAbsolutePath(), "-to", "127.0.0.1", "-port", "9999",
        "-previewSize", "2048", "-blockSize", "2048", "-receiveSize", "2048",
        "-maxSize", "100000", "-errorDelete", "-keyPreview", "Methods",
        "-stringPreview", "RESPMOD", "-key204", "Options-TTL", "-string204",
        "600", "-stringHttp",
        "This is data that was returned by the wonderful origin server"
    };
    assertEquals(IcapScanFile.STATUS_KO_SCAN, IcapScanFile.scanFile(fullArgs));
    assertFalse(file.exists());
    createFile(file, 1000);

    // With error on result
    logger.warn("With wrong result");
    IcapServerHandler.resetJunitStatus();
    fullArgs = new String[] {
        IcapScanFile.FILE_ARG, file.getAbsolutePath(), "-to", "127.0.0.1",
        "-port", "9999", "-service", "avscan", "-previewSize", "2048",
        "-blockSize", "2048", "-receiveSize", "2048", "-maxSize", "100000",
        "-errorDelete", "-keyPreview", "Methods", "-stringPreview", "RESPMOD",
        "-key204", "Options-TTL", "-string204", "WRONG", "-key200",
        "Options-TTL", "-string200", "600", "-stringHttp", "WaarpFakeIcap"
    };
    assertEquals(IcapScanFile.STATUS_KO_SCAN, IcapScanFile.scanFile(fullArgs));
    assertFalse(file.exists());
  }

} 
