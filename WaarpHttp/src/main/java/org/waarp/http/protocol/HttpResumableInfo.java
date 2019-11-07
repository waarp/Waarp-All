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

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

/**
 * Http Resumable information
 */
public class HttpResumableInfo {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpResumableInfo.class);

  private int chunkNumber;
  private int totalChunks;
  private int chunkSize;
  private long totalSize;
  private String identifier;
  private String filename;
  private String relativePath;


  public HttpResumableInfo(final int chunkNumber, final int chunkSize,
                           final long totalSize, final String identifier,
                           final String filename, final String relativePath) {
    this.chunkNumber = chunkNumber;
    this.chunkSize = chunkSize;
    this.totalSize = totalSize;
    this.identifier = identifier;
    this.filename = filename;
    this.relativePath = relativePath;
    this.totalChunks =
        (int) Math.ceil(((double) totalSize) / ((double) chunkSize));
    logger
        .debug("{} {} {} {} {}", totalSize, chunkSize, (totalSize) / chunkSize,
               (int) Math.ceil(((double) totalSize) / ((double) chunkSize)),
               totalChunks);
  }

  /**
   * The original file name (since a bug in Firefox results in the file name
   * not being transmitted in chunk multipart posts).
   */
  public String getFilename() {
    return filename;
  }

  public HttpResumableInfo setFilename(final String filename) {
    this.filename = filename;
    return this;
  }

  /**
   * The file's relative path when selecting a directory (defaults to file
   * name in all browsers except Chrome).
   */
  public String getRelativePath() {
    return relativePath;
  }

  public HttpResumableInfo setRelativePath(final String relativePath) {
    this.relativePath = relativePath;
    return this;
  }

  /**
   * @param resumableInfo
   *
   * @return True if both HttpResumableInfo are compatible (size, chunk size,
   *     total number of chunks, identifier and current chunk vs max chunk
   *     number)
   */
  public boolean isCompatible(HttpResumableInfo resumableInfo) {
    return resumableInfo.getChunkSize() == getChunkSize() &&
           resumableInfo.getTotalSize() == getTotalSize() &&
           resumableInfo.getIdentifier().equals(getIdentifier()) &&
           resumableInfo.getTotalChunks() == getTotalChunks() &&
           resumableInfo.getChunkNumber() <= getTotalChunks();
  }

  /**
   * The general chunk size. Using this value and resumableTotalSize you can
   * calculate the total number of chunks. Please note that the size of the
   * data received in the HTTP might be higher than resumableChunkSize for
   * the last chunk for a file. Max being 2^31, preferably 2^20 (1 MB)
   */
  public int getChunkSize() {
    return chunkSize;
  }

  public HttpResumableInfo setChunkSize(final int chunkSize) {
    this.chunkSize = chunkSize;
    return this;
  }

  /**
   * The total file size. Max being theoretically 2^63, but in practice being
   * resumableChunkSize x resumableTotalChunks, therefore 2^20 x 2^31 = 2^51
   */
  public long getTotalSize() {
    return totalSize;
  }

  public HttpResumableInfo setTotalSize(final long totalSize) {
    this.totalSize = totalSize;
    return this;
  }

  /**
   * A unique identifier for the file contained in the request.
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * The total number of chunks. Max being 2^31
   */
  public int getTotalChunks() {
    return totalChunks;
  }

  /**
   * The index of the chunk in the current upload. First chunk is 1 (no
   * base-0 counting here). Max being 2^31
   */
  public int getChunkNumber() {
    return chunkNumber;
  }

  public HttpResumableInfo setChunkNumber(final int chunkNumber) {
    this.chunkNumber = chunkNumber;
    return this;
  }

  public HttpResumableInfo setTotalChunks(final int totalChunks) {
    this.totalChunks = totalChunks;
    return this;
  }

  public HttpResumableInfo setIdentifier(final String identifier) {
    this.identifier = identifier;
    return this;
  }

  @Override
  public String toString() {
    return "RI:{CN:" + chunkNumber + ", TC:" + totalChunks + ", CS:" +
           chunkSize + ", TS:" + totalSize + ", ID:" + identifier + ", FN:" +
           filename + ", RP:" + relativePath + "}";
  }
}
