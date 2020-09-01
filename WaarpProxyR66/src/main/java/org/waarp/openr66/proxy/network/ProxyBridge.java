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
package org.waarp.openr66.proxy.network;

import io.netty.channel.Channel;
import org.waarp.openr66.protocol.utils.R66Future;

import static org.waarp.openr66.protocol.configuration.Configuration.*;
import static org.waarp.openr66.protocol.networkhandler.NetworkServerInitializer.*;

/**
 * Proxy bridge between a request and a proxified server
 */
public class ProxyBridge {
  public static NetworkTransaction transaction;

  private final ProxyEntry proxyEntry;
  private final NetworkServerHandler source;
  private NetworkServerHandler proxified;
  private final R66Future futureRemoteConnected = new R66Future(true);

  public static void initialize() {
    transaction = new NetworkTransaction();
  }

  /**
   * @param proxyEntry
   * @param source
   */
  public ProxyBridge(final ProxyEntry proxyEntry,
                     final NetworkServerHandler source) {
    this.proxyEntry = proxyEntry;
    this.source = source;
  }

  public void initializeProxy() {
    final Channel proxy = transaction
        .createConnectionWithRetry(proxyEntry.getRemoteSocketAddress(),
                                   proxyEntry.isRemoteSsl());
    if (proxy == null) {
      // Can't connect ?
      futureRemoteConnected.cancel();
      return;
    }
    proxified = (NetworkServerHandler) proxy.pipeline().get(NETWORK_HANDLER);
    proxified.setBridge(this);
  }

  public void remoteConnected() {
    futureRemoteConnected.setSuccess();
  }

  public boolean waitForRemoteConnection() {
    futureRemoteConnected.awaitOrInterruptible(configuration.getTimeoutCon());
    if (!futureRemoteConnected.isSuccess()) {
      futureRemoteConnected.cancel();
      return false;
    }
    return true;
  }

  /**
   * @return the proxyEntry
   */
  public ProxyEntry getProxyEntry() {
    return proxyEntry;
  }

  /**
   * @return the source
   */
  public NetworkServerHandler getSource() {
    return source;
  }

  /**
   * @return the proxified
   */
  public NetworkServerHandler getProxified() {
    return proxified;
  }
}
