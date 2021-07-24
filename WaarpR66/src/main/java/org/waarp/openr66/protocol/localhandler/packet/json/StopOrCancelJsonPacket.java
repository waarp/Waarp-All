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
package org.waarp.openr66.protocol.localhandler.packet.json;

import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;

import static org.waarp.common.database.DbConstant.*;

/**
 * Stop or Cancel one request JSON packet
 */
public class StopOrCancelJsonPacket extends JsonPacket {

  protected String requester;
  protected String requested;
  protected long specialid = ILLEGALVALUE;

  /**
   * @return the requester
   */
  public final String getRequester() {
    return requester;
  }

  /**
   * @param requester the requester to set
   */
  public final void setRequester(final String requester) {
    this.requester = requester;
  }

  /**
   * @return the requested
   */
  public final String getRequested() {
    return requested;
  }

  /**
   * @param requested the requested to set
   */
  public final void setRequested(final String requested) {
    this.requested = requested;
  }

  /**
   * @return the specialid
   */
  public final long getSpecialid() {
    return specialid;
  }

  /**
   * @param specialid the specialid to set
   */
  public final void setSpecialid(final long specialid) {
    this.specialid = specialid;
  }

  @Override
  public final void setRequestUserPacket() {
    setRequestUserPacket(LocalPacketFactory.STOPPACKET);
  }

  public final void setStop() {
    setRequestUserPacket(LocalPacketFactory.STOPPACKET);
  }

  public final void setCancel() {
    setRequestUserPacket(LocalPacketFactory.CANCELPACKET);
  }
}
