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
package org.waarp.openr66.protocol.networkhandler;

import org.waarp.common.lru.ConcurrentUtility;

import java.util.Set;

/**
 * Client NetworkChannelReference attached to one HostId.
 * <p>
 * <p>
 * <p>
 * This class is used to keep information for one HostID when it connects as
 * client to the current Host. As
 * one Id can be shared or one can use direct send, so having a connection by
 * request, this class is useful
 * when one wants to know who is connected and how many times.
 */
public class ClientNetworkChannels {

  private final String hostId;
  private final Set<NetworkChannelReference> networkChannelReferences =
      ConcurrentUtility.newConcurrentSet();

  public ClientNetworkChannels(final String hostId) {
    this.hostId = hostId;
  }

  public void add(final NetworkChannelReference networkChannelReference) {
    networkChannelReferences.add(networkChannelReference);
    networkChannelReference.clientNetworkChannels = this;
    networkChannelReference.setHostId(hostId);
  }

  public void remove(final NetworkChannelReference networkChannelReference) {
    networkChannelReferences.remove(networkChannelReference);
  }

  public boolean isEmpty() {
    return networkChannelReferences.isEmpty();
  }

  public int size() {
    return networkChannelReferences.size();
  }

  public boolean shutdownAllNetworkChannels() {
    boolean status = false;
    for (final NetworkChannelReference networkChannelReference : networkChannelReferences) {
      NetworkTransaction.shuttingDownNetworkChannel(networkChannelReference);
      status = true;
    }
    networkChannelReferences.clear();
    return status;
  }

  public String getHostId() {
    return hostId;
  }
}
