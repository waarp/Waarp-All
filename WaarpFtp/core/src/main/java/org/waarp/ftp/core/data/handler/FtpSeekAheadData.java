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
package org.waarp.ftp.core.data.handler;

import io.netty.buffer.ByteBuf;

/**
 * SeekAheadData Class used to optimize access to the incoming buffer
 */
class FtpSeekAheadData {
  /**
   * Exception when NO Backend Array is found
   */
  static class SeekAheadNoBackArrayException extends Exception {
    private static final long serialVersionUID = -630418804938699495L;
  }

  byte[] bytes;

  int readerIndex;

  int pos;

  int limit;

  ByteBuf buffer;

  /**
   * @param buffer
   */
  FtpSeekAheadData(ByteBuf buffer) throws SeekAheadNoBackArrayException {
    if (!buffer.hasArray()) {
      throw new SeekAheadNoBackArrayException();
    }
    this.buffer = buffer;
    bytes = buffer.array();
    pos = readerIndex = buffer.arrayOffset() + buffer.readerIndex();
    limit = buffer.arrayOffset() + buffer.writerIndex();
  }

  /**
   * @param minus this value will be used as (currentPos - minus) to
   *     set the
   *     current readerIndex in the buffer.
   */
  void setReadPosition(int minus) {
    pos -= minus;
    readerIndex = pos;
    buffer.readerIndex(readerIndex);
  }

  void clear() {
    buffer = null;
    bytes = null;
    limit = 0;
    pos = 0;
    readerIndex = 0;
  }
}
