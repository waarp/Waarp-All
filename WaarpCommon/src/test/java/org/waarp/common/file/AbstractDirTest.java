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

package org.waarp.common.file;

import org.junit.Test;

import static org.junit.Assert.*;

public class AbstractDirTest {

  @Test
  public void testNormalizePath() {
    assertEquals("single relative filename", "test.dat",
                 AbstractDir.normalizePath("test.dat"));
    assertEquals("relative path (unix)", "foo/test.dat",
                 AbstractDir.normalizePath("foo/test.dat"));
    assertEquals("absolute path (unix)", "/foo/test.dat",
                 AbstractDir.normalizePath("/foo/test.dat"));
    assertEquals("absolute uri (unix)", "file:///foo/test.dat",
                 AbstractDir.normalizePath("file:///foo/test.dat"));
    assertEquals("relative uri (unix)", "file://foo/test.dat",
                 AbstractDir.normalizePath("file://foo/test.dat"));

    assertEquals("relative path (win)", "foo/test.dat",
                 AbstractDir.normalizePath("foo\\test.dat"));
    assertEquals("absolute path (win)", "c:/foo/test.dat",
                 AbstractDir.normalizePath("c:\\foo\\test.dat"));
    assertEquals("absolute uri (win)", "file:///c:/foo/test.dat",
                 AbstractDir.normalizePath("file:///c:/foo/test.dat"));
    assertEquals("relative uri (win)", "file://foo/test.dat",
                 AbstractDir.normalizePath("file://foo/test.dat"));
    assertEquals("UNC path (win)", "//server/share/test.dat",
                 AbstractDir.normalizePath("\\\\server\\share\\test.dat"));
    assertEquals("UNC uri (win)", "file:////server/share/test.dat",
                 AbstractDir.normalizePath("file:////server/share/test.dat"));
  }

  @Test
  public void testPathFromUri() {
    assertEquals("single relative filename", "test.dat",
                 AbstractDir.pathFromURI("test.dat"));
    assertEquals("relative path (unix)", "foo/test.dat",
                 AbstractDir.pathFromURI("foo/test.dat"));
    assertEquals("absolute path (unix)", "/foo/test.dat",
                 AbstractDir.pathFromURI("/foo/test.dat"));
    assertEquals("absolute uri (unix)", "/foo/test.dat",
                 AbstractDir.pathFromURI("file:///foo/test.dat"));
    assertEquals("relative uri (unix)", "foo/test.dat",
                 AbstractDir.pathFromURI("file://foo/test.dat"));

    assertEquals("relative path (win)", "foo/test.dat",
                 AbstractDir.pathFromURI("foo/test.dat"));
    assertEquals("absolute path (win)", "c:/foo/test.dat",
                 AbstractDir.pathFromURI("c:/foo/test.dat"));
    assertEquals("absolute uri (win)", "c:/foo/test.dat",
                 AbstractDir.pathFromURI("file:///c:/foo/test.dat"));
    assertEquals("relative uri (win)", "foo/test.dat",
                 AbstractDir.pathFromURI("file://foo/test.dat"));
    assertEquals("UNC path (win)", "//server/share/test.dat",
                 AbstractDir.pathFromURI("//server/share/test.dat"));
    assertEquals("UNC uri (win)", "//server/share/test.dat",
                 AbstractDir.pathFromURI("file:////server/share/test.dat"));

    assertEquals("url to decode", "//server/share/béah @;tré", AbstractDir
        .pathFromURI("file:////server/share/b%C3%A9ah%20%40%3Btr%C3%A9"));
    assertEquals("filename with percent sign", "/tmp/100% accuracy.txt",
                 AbstractDir.pathFromURI("file:///tmp/100% accuracy.txt"));
  }
}
