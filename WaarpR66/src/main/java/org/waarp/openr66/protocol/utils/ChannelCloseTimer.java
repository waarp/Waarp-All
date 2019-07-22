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
package org.waarp.openr66.protocol.utils;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.openr66.protocol.configuration.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * TimerTask to Close a Channel in the future
 *
 *
 */
public class ChannelCloseTimer implements TimerTask {

  private final Channel channel;
  private ChannelFuture future = null;

  public ChannelCloseTimer(Channel channel) {
    this.channel = channel;
  }

  public ChannelCloseTimer(Channel channel, ChannelFuture future) {
    this.channel = channel;
    this.future = future;
  }

  @Override
  public void run(Timeout timeout) throws Exception {
    if (future != null) {
      future.awaitUninterruptibly(Configuration.configuration.getTIMEOUTCON());
    }
    WaarpSslUtility.closingSslChannel(channel);
  }

  /**
   * Close in the future this channel
   *
   * @param channel
   */
  public static void closeFutureChannel(Channel channel) {
    Configuration.configuration.getTimerClose()
                               .newTimeout(new ChannelCloseTimer(channel),
                                           Configuration.WAITFORNETOP * 2,
                                           TimeUnit.MILLISECONDS);
  }

  /**
   * Close in the future this channel
   *
   * @param channel
   * @param future future to wait in addition to other constraints
   */
  public static void closeFutureChannel(Channel channel, ChannelFuture future) {
    Configuration.configuration.getTimerClose().newTimeout(
        new ChannelCloseTimer(channel, future), Configuration.WAITFORNETOP * 2,
        TimeUnit.MILLISECONDS);
  }
}
