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

/**
 * Shutdown or Block JSON packet
 */
public class ShutdownOrBlockJsonPacket extends JsonPacket {

  protected byte[] key;
  /**
   * Shutdown: isRestart, Block: block/unblock
   */
  protected boolean isRestartOrBlock;
  /**
   * True: Shutdown, False: Block
   */
  protected boolean isShutdownOrBlock;

  /**
   * @return the key
   */
  public byte[] getKey() {
    return key;
  }

  /**
   * @param key the key to set
   */
  public void setKey(final byte[] key) {
    this.key = key;
  }

  /**
   * @return True if isRestart Or False for Block
   */
  public boolean isRestartOrBlock() {
    return isRestartOrBlock;
  }

  /**
   * @param isRestart the isRestart to set
   */
  public void setRestartOrBlock(final boolean isRestart) {
    isRestartOrBlock = isRestart;
  }

  /**
   * @return True if isShutdown Or False for Block
   */
  public boolean isShutdownOrBlock() {
    return isShutdownOrBlock;
  }

  /**
   * @param isShutdown the isShutdown to set
   */
  public void setShutdownOrBlock(final boolean isShutdown) {
    isShutdownOrBlock = isShutdown;
  }
}
