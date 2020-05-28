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

package org.waarp.common.logging;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.waarp.common.utility.WaarpStringUtils;

import javax.management.RuntimeErrorException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.*;

public class SysErrLoggerTest {
  private static final String NOT_EMPTY = "Not empty";
  private static PrintStream err;
  private static final StringBuilder buf = new StringBuilder();

  @BeforeClass
  public static void setUpBeforeClass() {
    err = System.err; // NOSONAR since Logger test
    try {
      System.setErr(new PrintStream(new OutputStream() {
        @Override
        public void write(final int b) {
          buf.append((char) b);
        }
      }, true, WaarpStringUtils.UTF_8));
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeErrorException(new Error(e));
    }
  }

  @AfterClass
  public static void tearDownAfterClass() {
    System.setErr(err);
  }

  @Test
  public void testSyserr() {
    SysErrLogger.FAKE_LOGGER.ignoreLog(new Exception("Fake exception"));
    assertEquals(0, buf.length());
    SysErrLogger.FAKE_LOGGER.syserr(NOT_EMPTY);
    assertTrue(buf.length() > 0);
    buf.setLength(0);
    SysErrLogger.FAKE_LOGGER.syserr();
    assertTrue(buf.length() > 0);
    buf.setLength(0);
    SysErrLogger.FAKE_LOGGER.syserr(NOT_EMPTY, new Exception("Fake exception"));
    assertTrue(buf.length() > NOT_EMPTY.length() + 5);
    buf.setLength(0);
  }
}