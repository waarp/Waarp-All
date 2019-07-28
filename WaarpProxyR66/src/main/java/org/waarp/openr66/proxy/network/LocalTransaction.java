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

import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolRemoteShutdownException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.networkhandler.NetworkChannelReference;
import org.waarp.openr66.protocol.utils.R66Future;

/**
 *
 */
public class LocalTransaction
    extends org.waarp.openr66.protocol.localhandler.LocalTransaction {

  public LocalChannelReference createNewClient(
      NetworkChannelReference networkChannelReference, Integer remoteId,
      R66Future futureRequest) throws OpenR66ProtocolSystemException,
                                      OpenR66ProtocolRemoteShutdownException {
    return null;
  }

  @Override
  protected void remove(LocalChannelReference localChannelReference) {
    // nothing
  }

  /**
   *
   */
  public LocalTransaction() {
    // nothing
  }

  @Override
  public LocalChannelReference getClient(Integer remoteId, Integer localId)
      throws OpenR66ProtocolSystemException {
    return null;
  }

  @Override
  public LocalChannelReference getFromId(Integer id) {
    return null;
  }

  @Override
  public void setFromId(DbTaskRunner runner, LocalChannelReference lcr) {
    // nothing
  }

  @Override
  public LocalChannelReference getFromRequest(String key) {
    return null;
  }

  @Override
  public int getNumberLocalChannel() {
    return ProxyBridge.transaction.getNumberClients();
  }

  @Override
  public void debugPrintActiveLocalChannels() {
    // nothing
  }

  @Override
  public void shutdownLocalChannels() {
    // nothing
  }

  @Override
  public void closeAll() {
    // nothing
  }

}
