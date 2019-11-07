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

import static org.junit.Assert.*;

/**
 * HttpResumableInfo Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>nov. 23, 2019</pre>
 */
public class HttpResumableInfoTest {
  final int chunkNumber = 2;
  final int chunkSize = 1024;
  final long totalSize = 10000;
  final int toalChunk =
      (int) Math.ceil(((double) totalSize) / ((double) chunkSize));
  final String identifier = "abcdef";
  final String filename = "filename.txt";
  final String relativePath = "relative/path/filename.txt";
  HttpResumableInfo resumableInfo;

  @Before
  public void before() throws Exception {
    resumableInfo =
        new HttpResumableInfo(chunkNumber, chunkSize, totalSize, identifier,
                              filename, relativePath);
  }

  @After
  public void after() throws Exception {
  }

  /**
   * Method: getChunkNumber()
   */
  @Test
  public void testGetChunkNumber() throws Exception {
    assertEquals(chunkNumber, resumableInfo.getChunkNumber());
  }

  /**
   * Method: setChunkNumber(final int chunkNumber)
   */
  @Test
  public void testSetChunkNumber() throws Exception {
    assertEquals(chunkNumber, resumableInfo.getChunkNumber());
    resumableInfo.setChunkNumber(chunkNumber + 1);
    assertEquals(chunkNumber + 1, resumableInfo.getChunkNumber());
  }

  /**
   * Method: getTotalChunks()
   */
  @Test
  public void testGetTotalChunks() throws Exception {
    assertEquals(toalChunk, resumableInfo.getTotalChunks());
  }

  /**
   * Method: setTotalChunks(final int totalChunks)
   */
  @Test
  public void testSetTotalChunks() throws Exception {
    assertEquals(toalChunk, resumableInfo.getTotalChunks());
    resumableInfo.setTotalChunks(toalChunk + 1);
    assertEquals(toalChunk + 1, resumableInfo.getTotalChunks());
  }

  /**
   * Method: getChunkSize()
   */
  @Test
  public void testGetChunkSize() throws Exception {
    assertEquals(chunkSize, resumableInfo.getChunkSize());
  }

  /**
   * Method: setChunkSize(final int chunkSize)
   */
  @Test
  public void testSetChunkSize() throws Exception {
    assertEquals(chunkSize, resumableInfo.getChunkSize());
    resumableInfo.setChunkSize(chunkSize + 1);
    assertEquals(chunkSize + 1, resumableInfo.getChunkSize());
  }

  /**
   * Method: getTotalSize()
   */
  @Test
  public void testGetTotalSize() throws Exception {
    assertEquals(totalSize, resumableInfo.getTotalSize());
  }

  /**
   * Method: setTotalSize(final long totalSize)
   */
  @Test
  public void testSetTotalSize() throws Exception {
    assertEquals(totalSize, resumableInfo.getTotalSize());
    resumableInfo.setTotalSize(totalSize + 1);
    assertEquals(totalSize + 1, resumableInfo.getTotalSize());
  }

  /**
   * Method: getIdentifier()
   */
  @Test
  public void testGetIdentifier() throws Exception {
    assertEquals(identifier, resumableInfo.getIdentifier());
  }

  /**
   * Method: setIdentifier(final String identifier)
   */
  @Test
  public void testSetIdentifier() throws Exception {
    assertEquals(identifier, resumableInfo.getIdentifier());
    resumableInfo.setIdentifier(identifier + identifier);
    assertEquals(identifier + identifier, resumableInfo.getIdentifier());
  }

  /**
   * Method: getFilename()
   */
  @Test
  public void testGetFilename() throws Exception {
    assertEquals(filename, resumableInfo.getFilename());
  }

  /**
   * Method: setFilename(final String filename)
   */
  @Test
  public void testSetFilename() throws Exception {
    assertEquals(filename, resumableInfo.getFilename());
    resumableInfo.setFilename(filename + "tmp");
    assertEquals(filename + "tmp", resumableInfo.getFilename());
  }

  /**
   * Method: getRelativePath()
   */
  @Test
  public void testGetRelativePath() throws Exception {
    assertEquals(relativePath, resumableInfo.getRelativePath());
  }

  /**
   * Method: setRelativePath(final String relativePath)
   */
  @Test
  public void testSetRelativePath() throws Exception {
    assertEquals(relativePath, resumableInfo.getRelativePath());
    resumableInfo.setRelativePath(relativePath + "tmp");
    assertEquals(relativePath + "tmp", resumableInfo.getRelativePath());
  }

  /**
   * Method: isCompatible(HttpResumableInfo resumableInfo)
   */
  @Test
  public void testIsCompatible() throws Exception {
    HttpResumableInfo resumableInfo2 =
        new HttpResumableInfo(chunkNumber, chunkSize, totalSize, identifier,
                              filename, relativePath);
    assertTrue(resumableInfo.isCompatible(resumableInfo2));
    resumableInfo2.setChunkNumber(chunkNumber + 1);
    assertTrue(resumableInfo.isCompatible(resumableInfo2));
    resumableInfo2.setChunkNumber(Integer.MAX_VALUE);
    assertFalse(resumableInfo.isCompatible(resumableInfo2));
    resumableInfo2.setChunkNumber(chunkNumber);
    assertTrue(resumableInfo.isCompatible(resumableInfo2));

    resumableInfo2.setTotalSize(Integer.MAX_VALUE);
    assertFalse(resumableInfo.isCompatible(resumableInfo2));
    resumableInfo2.setTotalSize(totalSize);
    assertTrue(resumableInfo.isCompatible(resumableInfo2));

    resumableInfo2.setChunkSize(Integer.MAX_VALUE);
    assertFalse(resumableInfo.isCompatible(resumableInfo2));
    resumableInfo2.setChunkSize(chunkSize);
    assertTrue(resumableInfo.isCompatible(resumableInfo2));

    resumableInfo2.setTotalChunks(Integer.MAX_VALUE);
    assertFalse(resumableInfo.isCompatible(resumableInfo2));
    resumableInfo2.setTotalChunks(toalChunk);
    assertTrue(resumableInfo.isCompatible(resumableInfo2));

    resumableInfo2.setIdentifier(identifier + identifier);
    assertFalse(resumableInfo.isCompatible(resumableInfo2));
    resumableInfo2.setIdentifier(identifier);
    assertTrue(resumableInfo.isCompatible(resumableInfo2));
  }

  /**
   * Method: toString()
   */
  @Test
  public void testToString() throws Exception {
    assertEquals("RI:{CN:2, TC:10, CS:1024, TS:10000, ID:abcdef, " +
                 "FN:filename.txt, RP:relative/path/filename.txt}",
                 resumableInfo.toString());
  }


} 
