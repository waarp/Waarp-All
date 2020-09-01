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

import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;

import java.net.SocketAddress;
import java.util.HashMap;

/**
 * Proxy Entry
 */
public class ProxyEntry {
  public static final HashMap<String, ProxyEntry> proxyEntries =
      new HashMap<String, ProxyEntry>();

  private final String name;
  private final SocketAddress localSocketAddress;
  private final boolean localIsSsl;
  private final SocketAddress remoteSocketAddress;
  private final boolean remoteIsSsl;

  public static void add(final SocketAddress localAddress,
                         final boolean localIsSsl,
                         final SocketAddress remoteAddress,
                         final boolean remoteIsSsl)
      throws OpenR66ProtocolSystemException {
    final String sla = localAddress.toString();
    if (proxyEntries.containsKey(sla)) {
      throw new OpenR66ProtocolSystemException(
          "Error in the configuration: Two entries with the same localAdress");
    }
    proxyEntries.put(sla, new ProxyEntry(sla, localAddress, localIsSsl,
                                         remoteAddress, remoteIsSsl));
  }

  public static ProxyEntry get(final String name) {
    synchronized (proxyEntries) {
      if (proxyEntries.containsKey(name)) {
        return proxyEntries.get(name);
      } else {
        // error
        return null;
      }
    }
  }

  /**
   * @param name
   * @param localSocketAddress
   * @param localIsSsl
   * @param remoteSocketAddress
   * @param remoteIsSsl
   */
  private ProxyEntry(final String name, final SocketAddress localSocketAddress,
                     final boolean localIsSsl,
                     final SocketAddress remoteSocketAddress,
                     final boolean remoteIsSsl) {
    this.name = name;
    this.localSocketAddress = localSocketAddress;
    this.localIsSsl = localIsSsl;
    this.remoteSocketAddress = remoteSocketAddress;
    this.remoteIsSsl = remoteIsSsl;
  }

  /**
   * @return the localSocketAddress
   */
  public SocketAddress getLocalSocketAddress() {
    return localSocketAddress;
  }

  /**
   * @return the localIsSsl
   */
  public boolean isLocalSsl() {
    return localIsSsl;
  }

  /**
   * @return the remoteSocketAddress
   */
  public SocketAddress getRemoteSocketAddress() {
    return remoteSocketAddress;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the isSsl
   */
  public boolean isRemoteSsl() {
    return remoteIsSsl;
  }

  @Override
  public String toString() {
    return "from: " + localSocketAddress + ':' + localIsSsl + " to: " +
           remoteSocketAddress + ':' + remoteIsSsl;
  }
}
