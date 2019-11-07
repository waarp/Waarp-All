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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.waarp.common.utility.DetectionUtils;

import static org.junit.Assert.*;

/**
 * HttpSessions Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>nov. 23, 2019</pre>
 */
public class HttpSessionsTest {
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


  @Before
  public void before() throws Exception {
    DetectionUtils.setJunit(true);
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
   * Method: getInstance()
   */
  @Test
  public void testGetInstance() throws Exception {
    HttpSessions sessions = HttpSessions.getInstance();
    assertNotNull(sessions);
  }

  /**
   * Method: getOrCreateSession(final HttpResumableInfo resumableInfo, final
   * String rulename, final HttpAuthent authent)
   */
  @Test
  public void testGetOrCreateSession() throws Exception {
    HttpSessionAbstract session =
        new HttpResumableSession(resumableInfo, "rule", "comment", authent);
    HttpSessions sessions = HttpSessions.getInstance();
    assertFalse(sessions.contains(resumableInfo));
    HttpSessionAbstract session2 = sessions
        .getOrCreateResumableSession(resumableInfo, "rule", "comment", authent);
    assertTrue(sessions.contains(resumableInfo));
    assertFalse(session == session2);
    assertTrue(sessions.removeSession(resumableInfo));
    assertFalse(sessions.removeSession(resumableInfo));

    HttpResumableSession session3 = sessions
        .getOrCreateResumableSession(resumableInfo, "rule", "comment", authent);
    assertTrue(sessions.contains(resumableInfo));
    assertFalse(session == session2);
    assertFalse(session3 == session2);
    assertTrue(sessions.removeSession(session3));
    assertFalse(sessions.removeSession(session3));
  }

} 
