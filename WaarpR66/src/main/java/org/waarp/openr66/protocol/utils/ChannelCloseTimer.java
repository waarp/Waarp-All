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
import org.waarp.common.database.DbSession;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.localhandler.ConnectionActions;

import java.util.concurrent.TimeUnit;

import static org.waarp.common.database.DbConstant.*;

/**
 * TimerTask to Close a Channel in the future
 */
public class ChannelCloseTimer implements TimerTask {

  private Channel channel;
  private ChannelFuture future;
  private ConnectionActions connectionActions;
  private DbSession noconcurrencyDbSession;

  public ChannelCloseTimer(final Channel channel) {
    this.channel = channel;
  }

  public ChannelCloseTimer(final Channel channel, final ChannelFuture future) {
    this.channel = channel;
    this.future = future;
  }

  public ChannelCloseTimer(final ConnectionActions connectionActions) {
    this.connectionActions = connectionActions;
  }

  @Override
  public void run(final Timeout timeout) throws Exception {
    if (future != null) {
      WaarpNettyUtil.awaitOrInterrupted(future);
    }
    if (channel != null) {
      WaarpSslUtility.closingSslChannel(channel);
    } else if (connectionActions != null) {
      if (noconcurrencyDbSession != null && admin != null &&
          admin.getSession() != null &&
          !noconcurrencyDbSession.equals(admin.getSession())) {
        noconcurrencyDbSession.forceDisconnect();
        noconcurrencyDbSession = null;
      }
      connectionActions.getLocalChannelReference().close();
    }
  }

  /**
   * Close in the future this transaction (may need more than 1 WAITFORNETOP)
   *
   * @param connectionActions
   */
  public static void closeFutureTransaction(
      final ConnectionActions connectionActions) {
    if (Configuration.configuration.isTimerCloseReady()) {
      Configuration.configuration.getTimerClose().newTimeout(
          new ChannelCloseTimer(connectionActions),
          Configuration.WAITFORNETOP * 2, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Close in the future this transaction
   *
   * @param connectionActions
   * @param noconcurrencyDbSession
   */
  public static void closeFutureTransaction(
      final ConnectionActions connectionActions,
      final DbSession noconcurrencyDbSession) {
    if (Configuration.configuration.isTimerCloseReady()) {
      final ChannelCloseTimer cct = new ChannelCloseTimer(connectionActions);
      cct.setDbSession(noconcurrencyDbSession);
      Configuration.configuration.getTimerClose()
                                 .newTimeout(cct, Configuration.WAITFORNETOP,
                                             TimeUnit.MILLISECONDS);
    }
  }

  public void setDbSession(final DbSession dbSession) {
    noconcurrencyDbSession = dbSession;
  }

  /**
   * Close in the future this channel
   *
   * @param channel
   */
  public static void closeFutureChannel(final Channel channel) {
    if (Configuration.configuration.isTimerCloseReady()) {
      Configuration.configuration.getTimerClose()
                                 .newTimeout(new ChannelCloseTimer(channel),
                                             Configuration.WAITFORNETOP,
                                             TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Close in the future this channel
   *
   * @param channel
   * @param future future to wait in addition to other constraints
   */
  public static void closeFutureChannel(final Channel channel,
                                        final ChannelFuture future) {
    if (Configuration.configuration.isTimerCloseReady()) {
      Configuration.configuration.getTimerClose().newTimeout(
          new ChannelCloseTimer(channel, future), Configuration.WAITFORNETOP,
          TimeUnit.MILLISECONDS);
    }
  }
}
