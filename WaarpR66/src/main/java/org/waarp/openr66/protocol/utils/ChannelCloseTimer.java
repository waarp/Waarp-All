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
  private DbSession noConcurrencyDbSession;

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
  public void run(final Timeout timeout) {
    if (future != null) {
      WaarpNettyUtil.awaitOrInterrupted(future);
    }
    if (connectionActions != null) {
      if (noConcurrencyDbSession != null && admin != null &&
          admin.getSession() != null &&
          !noConcurrencyDbSession.equals(admin.getSession())) {
        noConcurrencyDbSession.forceDisconnect();
        noConcurrencyDbSession = null;
      }
      connectionActions.getLocalChannelReference().close();
    } else if (channel != null) {
      WaarpSslUtility.closingSslChannel(channel);
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
          new ChannelCloseTimer(connectionActions), Configuration.WAITFORNETOP,
          TimeUnit.MILLISECONDS);
    }
  }

  public final void setDbSession(final DbSession dbSession) {
    noConcurrencyDbSession = dbSession;
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

}
