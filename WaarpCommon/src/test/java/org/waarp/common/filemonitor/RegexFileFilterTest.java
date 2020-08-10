/*
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2020, Waarp SAS, and individual contributors by the @author
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
package org.waarp.common.filemonitor;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

/**
 * RegexFileFilterTest regroups unit tests for {@link RegexFileFilter}.
 */
public class RegexFileFilterTest {
  @Test
  public void testRegexFilename() {
    String rx = "test.csv";
    RegexFileFilter rff = new RegexFileFilter(rx);
    String[] shouldMatch = { "test.csv", "subdir/test.csv" };
    String[] shouldNotMatch = { "foo.csv", "subdir/foo.csv" };

    for (String s : shouldMatch) {
      assertTrue(
          "RegexFileFilter with regex '" + rx + "' should match '" + s + "'",
          doTestOnPath(rff, s));
    }

    for (String s : shouldNotMatch) {
      assertFalse(
          "RegexFileFilter with regex '" + rx + "' should match '" + s + "'",
          doTestOnPath(rff, s));
    }
  }

  @Test
  public void testRegexFilenameWildcard() {
    String rx = ".*\\.csv";
    RegexFileFilter rff = new RegexFileFilter(rx);
    String[] shouldMatch = { "test.csv", "subdir/test.csv" };
    String[] shouldNotMatch = { "foo.txt", "subdir/foo.txt" };

    for (String s : shouldMatch) {
      assertTrue(
          "RegexFileFilter with regex '" + rx + "' should match '" + s + "'",
          doTestOnPath(rff, s));
    }

    for (String s : shouldNotMatch) {
      assertFalse(
          "RegexFileFilter with regex '" + rx + "' should match '" + s + "'",
          doTestOnPath(rff, s));
    }
  }

  @Test
  public void testRegexPathPrefix() {
    String rx = "subdir/.*";
    RegexFileFilter rff = new RegexFileFilter(rx);
    String[] shouldMatch = {
        "subdir/test.csv", "subdir/foo.csv", "dir/subdir/foo.txt",
        "othersubdir/test.csv", "subdir/otherdir/test.csv",
    };
    String[] shouldNotMatch = { "foo.csv", "otherdir/foo.csv" };

    for (String s : shouldMatch) {
      assertTrue(
          "RegexFileFilter with regex '" + rx + "' should match '" + s + "'",
          doTestOnPath(rff, s));
    }

    for (String s : shouldNotMatch) {
      assertFalse(
          "RegexFileFilter with regex '" + rx + "' should match '" + s + "'",
          doTestOnPath(rff, s));
    }
  }

  @Test
  public void testRegexPathAbsolutePrefix() {
    String rx = "^subdir/.*";
    RegexFileFilter rff = new RegexFileFilter(rx);
    String[] shouldMatch = {
        "subdir/test.csv", "subdir/foo.csv", "subdir/otherdir/test.csv",
    };
    String[] shouldNotMatch = {
        "foo.csv", "otherdir/foo.csv", "dir/subdir/foo.txt",
        "othersubdir/test.csv",
    };

    for (String s : shouldMatch) {
      assertTrue(
          "RegexFileFilter with regex '" + rx + "' should match '" + s + "'",
          doTestOnPath(rff, s));
    }

    for (String s : shouldNotMatch) {
      assertFalse(
          "RegexFileFilter with regex '" + rx + "' should match '" + s + "'",
          doTestOnPath(rff, s));
    }
  }

  @Test
  public void testRegexWithFlags() {
    String rx = "(?i)subdir/.*";
    RegexFileFilter rff = new RegexFileFilter(rx);
    String[] shouldMatch = {
        "subdir/test.csv", "SUBDIR/foo.csv", "subDiR/otherdir/test.csv",
    };
    String[] shouldNotMatch = {
        "foo.csv", "otherdir/foo.csv",
    };

    for (String s : shouldMatch) {
      assertTrue(
          "RegexFileFilter with regex '" + rx + "' should match '" + s + "'",
          doTestOnPath(rff, s));
    }

    for (String s : shouldNotMatch) {
      assertFalse(
          "RegexFileFilter with regex '" + rx + "' should match '" + s + "'",
          doTestOnPath(rff, s));
    }
  }


  private boolean doTestOnPath(RegexFileFilter rff, String path) {
    try {
      setupStubPath(path);

      return rff.accept(new File(path));

    } catch (IOException e) {
      fail("cannot create stub test file '" + path + "':" + e);
    } finally {
      tearDownStubFile(path);
    }

    return false;
  }

  private void setupStubPath(String path) throws IOException {
    File f = new File(path);

    if (path.contains("/")) {
      f.getParentFile().mkdirs();
    }

    f.createNewFile();
  }

  private void tearDownStubFile(String path) {
    File tmpFile = new File(path);

    if (tmpFile.exists()) {
      tmpFile.delete();
    }

    if (path.contains("/")) {
      tmpFile.getParentFile().delete();
    }
  }
}
