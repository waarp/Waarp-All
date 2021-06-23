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
package org.waarp.openr66.client;

import io.netty.buffer.ByteBuf;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessException;

/**
 * Class to be implemented for {@link RecvThroughClient}
 */
public abstract class RecvThroughHandler {
  /**
   * This method will be called for each valid packet received to be written
   *
   * @param buffer
   * @param length
   *
   * @throws OpenR66ProtocolBusinessException This exception has to be
   *     throw
   *     if any error occurs during write
   *     in business process.
   */
  public abstract void writeBytes(byte[] buffer, int length)
      throws OpenR66ProtocolBusinessException;

  /**
   * Facility function to read from buffer and transfer to an array of bytes
   *
   * @param buffer
   *
   * @return the array of bytes
   */
  protected byte[] getBytes(final ByteBuf buffer) {
    final byte[] dst = new byte[buffer.readableBytes()];
    buffer.readBytes(dst, 0, dst.length);
    return dst;
  }
}
