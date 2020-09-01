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

/**
 * Bandwidth setting or getting JSON packet
 */
public class BandwidthJsonPacket extends JsonPacket {

  protected boolean setter;
  protected long writeglobal = -10;
  protected long readglobal = -10;
  protected long writesession = -10;
  protected long readsession = -10;

  /**
   * @return the setter
   */
  public boolean isSetter() {
    return setter;
  }

  /**
   * @param setter the setter to set
   */
  public void setSetter(final boolean setter) {
    this.setter = setter;
  }

  /**
   * @return the writeglobal
   */
  public long getWriteglobal() {
    return writeglobal;
  }

  /**
   * @param writeglobal the writeglobal to set
   */
  public void setWriteglobal(final long writeglobal) {
    this.writeglobal = writeglobal;
  }

  /**
   * @return the readglobal
   */
  public long getReadglobal() {
    return readglobal;
  }

  /**
   * @param readglobal the readglobal to set
   */
  public void setReadglobal(final long readglobal) {
    this.readglobal = readglobal;
  }

  /**
   * @return the writesession
   */
  public long getWritesession() {
    return writesession;
  }

  /**
   * @param writesession the writesession to set
   */
  public void setWritesession(final long writesession) {
    this.writesession = writesession;
  }

  /**
   * @return the readsession
   */
  public long getReadsession() {
    return readsession;
  }

  /**
   * @param readsession the readsession to set
   */
  public void setReadsession(final long readsession) {
    this.readsession = readsession;
  }

  @Override
  public void fromJson(final JsonPacket other) {
    super.fromJson(other);
    if (other instanceof BandwidthJsonPacket) {
      final BandwidthJsonPacket other2 = (BandwidthJsonPacket) other;
      setter = other2.setter;
      writeglobal = other2.writeglobal;
      readglobal = other2.readglobal;
      writesession = other2.writeglobal;
      readsession = other2.readsession;
    }
  }

  @Override
  public void setRequestUserPacket() {
    setRequestUserPacket(LocalPacketFactory.BANDWIDTHPACKET);
  }

}
