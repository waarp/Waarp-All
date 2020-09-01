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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.FileRegion;
import io.netty.channel.MessageSizeEstimator;

/**
 * DataBlock size estimator
 */
public class DataBlockSizeEstimator implements MessageSizeEstimator {

  private final Handle handle;

  /**
   *
   */
  public DataBlockSizeEstimator() {
    handle = new HandleImpl();
  }

  private static final class HandleImpl implements Handle {
    private HandleImpl() {
    }

    @Override
    public int size(final Object msg) {
      if (!(msg instanceof DataBlock)) {
        // Type unimplemented
        if (msg instanceof ByteBuf) {
          return ((ByteBuf) msg).readableBytes();
        }
        if (msg instanceof ByteBufHolder) {
          return ((ByteBufHolder) msg).content().readableBytes();
        }
        if (msg instanceof FileRegion) {
          return 0;
        }
      }
      final DataBlock dataBlock = (DataBlock) msg;
      return dataBlock.getByteCount();
    }
  }

  @Override
  public Handle newHandle() {
    return handle;
  }

}
