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

import io.netty.handler.traffic.GlobalChannelTrafficShapingHandler;
import org.waarp.common.file.DataBlock;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Global Traffic Shaping Handler for FTP
 */
public class FtpGlobalTrafficShapingHandler
    extends GlobalChannelTrafficShapingHandler {

  public FtpGlobalTrafficShapingHandler(final ScheduledExecutorService executor,
                                        final long writeGlobalLimit,
                                        final long readGlobalLimit,
                                        final long writeChannelLimit,
                                        final long readChannelLimit,
                                        final long checkInterval,
                                        final long maxTime) {
    super(executor, writeGlobalLimit, readGlobalLimit, writeChannelLimit,
          readChannelLimit, checkInterval, maxTime);
  }

  public FtpGlobalTrafficShapingHandler(final ScheduledExecutorService executor,
                                        final long writeGlobalLimit,
                                        final long readGlobalLimit,
                                        final long writeChannelLimit,
                                        final long readChannelLimit,
                                        final long checkInterval) {
    super(executor, writeGlobalLimit, readGlobalLimit, writeChannelLimit,
          readChannelLimit, checkInterval);
  }

  public FtpGlobalTrafficShapingHandler(final ScheduledExecutorService executor,
                                        final long writeGlobalLimit,
                                        final long readGlobalLimit,
                                        final long writeChannelLimit,
                                        final long readChannelLimit) {
    super(executor, writeGlobalLimit, readGlobalLimit, writeChannelLimit,
          readChannelLimit);
  }

  public FtpGlobalTrafficShapingHandler(final ScheduledExecutorService executor,
                                        final long checkInterval) {
    super(executor, checkInterval);
  }

  public FtpGlobalTrafficShapingHandler(
      final ScheduledExecutorService executor) {
    super(executor);
  }

  @Override
  protected long calculateSize(final Object msg) {
    if (msg instanceof DataBlock) {
      return ((DataBlock) msg).getByteCount();
    }
    return super.calculateSize(msg);
  }

}
