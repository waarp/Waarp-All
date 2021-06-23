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

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.waarp.compress.zstdsafe;

import java.util.Arrays;

class FrameHeader {
  final int headerSize;
  final int windowSize;
  final int contentSize;
  final int dictionaryId;
  final boolean hasChecksum;

  public FrameHeader(final int headerSize, final int windowSize,
                     final int contentSize, final int dictionaryId,
                     final boolean hasChecksum) {
    this.headerSize = headerSize;
    this.windowSize = windowSize;
    this.contentSize = contentSize;
    this.dictionaryId = dictionaryId;
    this.hasChecksum = hasChecksum;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final FrameHeader that = (FrameHeader) o;
    return headerSize == that.headerSize && windowSize == that.windowSize &&
           contentSize == that.contentSize &&
           dictionaryId == that.dictionaryId && hasChecksum == that.hasChecksum;
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[] {
        headerSize, windowSize, contentSize, dictionaryId, hasChecksum
    });
  }

  @Override
  public String toString() {
    return new StringBuilder(FrameHeader.class.getSimpleName() + "[")
        .append("headerSize=").append(headerSize).append(", ")
        .append("windowSize=").append(windowSize).append(", ")
        .append("contentSize=").append(contentSize).append(", ")
        .append("dictionaryId=").append(dictionaryId).append(", ")
        .append("hasChecksum=").append(hasChecksum).append("]").toString();
  }
}
