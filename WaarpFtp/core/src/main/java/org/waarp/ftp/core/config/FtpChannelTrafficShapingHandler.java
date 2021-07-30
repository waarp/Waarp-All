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
package org.waarp.ftp.core.config;

import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import org.waarp.common.file.DataBlock;

/**
 * Channel Traffic Shaping Handler for FTP
 */
public class FtpChannelTrafficShapingHandler
    extends ChannelTrafficShapingHandler {

  /**
   * @param checkInterval
   */
  public FtpChannelTrafficShapingHandler(final long checkInterval) {
    super(checkInterval);
  }

  /**
   * @param writeLimit
   * @param readLimit
   */
  public FtpChannelTrafficShapingHandler(final long writeLimit,
                                         final long readLimit) {
    super(writeLimit, readLimit);
  }

  /**
   * @param writeLimit
   * @param readLimit
   * @param checkInterval
   */
  public FtpChannelTrafficShapingHandler(final long writeLimit,
                                         final long readLimit,
                                         final long checkInterval) {
    super(writeLimit, readLimit, checkInterval);
  }

  /**
   * @param writeLimit
   * @param readLimit
   * @param checkInterval
   * @param maxTime
   */
  public FtpChannelTrafficShapingHandler(final long writeLimit,
                                         final long readLimit,
                                         final long checkInterval,
                                         final long maxTime) {
    super(writeLimit, readLimit, checkInterval, maxTime);
  }

  @Override
  protected final long calculateSize(final Object msg) {
    if (msg instanceof DataBlock) {
      return ((DataBlock) msg).getByteCount();
    }
    return super.calculateSize(msg);
  }

}
