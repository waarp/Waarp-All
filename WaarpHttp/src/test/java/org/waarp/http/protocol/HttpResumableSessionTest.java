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

package org.waarp.http.protocol;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.WaarpSystemUtil;

import static org.junit.Assert.*;

/**
 * HttpResumableSession Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>nov. 23, 2019</pre>
 */
public class HttpResumableSessionTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  final int chunkNumber = 2;
  final int chunkSize = 1024;
  final long totalSize = 10000;
  final int toalChunk =
      (int) Math.ceil(((double) totalSize) / ((double) chunkSize));
  final String identifier = "abcdef";
  final String filename = "filename.txt";
  final String relativePath = "relative/path/filename.txt";
  HttpResumableInfo resumableInfo;
  HttpAuthentDefaultExtended authent;

  @BeforeClass
  public static void beforeClass() {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
  }

  @Before
  public void before() throws Exception {
    WaarpSystemUtil.setJunit(true);
    resumableInfo =
        new HttpResumableInfo(chunkNumber, chunkSize, totalSize, identifier,
                              filename, relativePath);
    authent = new HttpAuthentDefaultExtended();
    authent.setUser("user");
    authent.setKey("key".getBytes());
  }

  @After
  public void after() throws Exception {
  }

  /**
   * Method: getHttpResumableInfo()
   */
  @Test
  public void testGetHttpResumableInfo() throws Exception {
    HttpResumableSession session =
        new HttpResumableSession(resumableInfo, "rule", "comment", authent);
    assertEquals(resumableInfo, session.getHttpResumableInfo());
  }

  /**
   * Method: valid(HttpResumableInfo resumableInfo)
   */
  @Test
  public void testValid() throws Exception {
    HttpResumableSession session =
        new HttpResumableSession(resumableInfo, "rule", "comment", authent);
    assertTrue(session.valid(resumableInfo));
    resumableInfo.setChunkSize(0);
    assertFalse(session.valid(resumableInfo));
    resumableInfo.setChunkSize(chunkSize);
    assertTrue(session.valid(resumableInfo));

    resumableInfo.setTotalSize(0);
    assertFalse(session.valid(resumableInfo));
    resumableInfo.setTotalSize(totalSize);
    assertTrue(session.valid(resumableInfo));

    resumableInfo.setChunkNumber(0);
    assertFalse(session.valid(resumableInfo));
    resumableInfo.setChunkNumber(chunkNumber);
    assertTrue(session.valid(resumableInfo));

    resumableInfo.setIdentifier(null);
    assertFalse(session.valid(resumableInfo));
    resumableInfo.setIdentifier(identifier);
    assertTrue(session.valid(resumableInfo));

    resumableInfo.setFilename(null);
    assertFalse(session.valid(resumableInfo));
    resumableInfo.setFilename(filename);
    assertTrue(session.valid(resumableInfo));

    resumableInfo.setRelativePath(null);
    assertFalse(session.valid(resumableInfo));
    resumableInfo.setRelativePath(relativePath);
    assertTrue(session.valid(resumableInfo));

    resumableInfo.setIdentifier(null);
    assertFalse(session.valid(resumableInfo));
    resumableInfo.setIdentifier(identifier);
    assertTrue(session.valid(resumableInfo));

    HttpResumableInfo resumableInfo2 =
        new HttpResumableInfo(chunkNumber, chunkSize + 1, totalSize + 1,
                              identifier, filename, relativePath);

    assertFalse(session.valid(resumableInfo2));
    assertTrue(session.valid(resumableInfo));
  }

  /**
   * Method: tryWrite(HttpResumableInfo resumableInfo, InputStream stream)
   */
  @Test
  public void testTryWrite() throws Exception {
    HttpResumableSession session =
        new HttpResumableSession(resumableInfo, "rule", "comment", authent);
    assertFalse(session.tryWrite(resumableInfo, null));
  }

  /**
   * Method: contains(HttpResumableInfo resumableInfo)
   */
  @Test
  public void testContains() throws Exception {
    HttpResumableSession session =
        new HttpResumableSession(resumableInfo, "rule", "comment", authent);
    assertFalse(session.contains(resumableInfo));
  }

  /**
   * Method: checkIfUploadFinished()
   */
  @Test
  public void testCheckIfUploadFinished() throws Exception {
    HttpResumableSession session =
        new HttpResumableSession(resumableInfo, "rule", "comment", authent);
    assertFalse(session.checkIfUploadFinished(null));
  }

  /**
   * Method: toString()
   */
  @Test
  public void testToString() throws Exception {
    HttpSessionAbstract session =
        new HttpResumableSession(resumableInfo, "rule", "comment", authent);
    assertEquals("RS: {[], Session: FS[OPENEDCHANNEL] NoStatus  Auth:false " +
                 "no Internal Auth [ NOACCESS ]     Dir: null     no File     no Runner, " +
                 "RI:{CN:2, TC:10, CS:1024, TS:10000, ID:abcdef, FN:filename.txt, " +
                 "RP:relative/path/filename.txt}}", session.toString());
  }

} 
