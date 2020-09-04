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
package org.waarp.openr66.protocol.localhandler.packet;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.json.JsonPacket;

import java.io.IOException;

/**
 * Json Command Message class for packet
 * <p>
 * 2 strings and one byte: request,result,send
 */
public class JsonCommandPacket extends AbstractLocalPacket {
  private final String request;

  private String result;

  private final byte send;

  /**
   * @param headerLength
   * @param middleLength
   * @param endLength
   * @param buf
   *
   * @return the new ValidPacket from buffer
   */
  public static JsonCommandPacket createFromBuffer(final int headerLength,
                                                   final int middleLength,
                                                   final int endLength,
                                                   final ByteBuf buf) {
    final byte[] bheader = new byte[headerLength - 1];
    final byte[] bmiddle = new byte[middleLength];
    final byte bend;
    if (headerLength - 1 > 0) {
      buf.readBytes(bheader);
    }
    if (middleLength > 0) {
      buf.readBytes(bmiddle);
    }
    bend = buf.readByte();
    return new JsonCommandPacket(new String(bheader), new String(bmiddle),
                                 bend);
  }

  /**
   * @param srequest
   * @param sresult
   * @param end
   */
  public JsonCommandPacket(final String srequest, final String sresult,
                           final byte end) {
    request = srequest;
    result = sresult;
    send = end;
  }

  /**
   * @param jrequest
   * @param end
   */
  public JsonCommandPacket(final JsonPacket jrequest, final byte end) {
    request = jrequest.toString();
    result = null;
    send = end;
  }

  /**
   * @param jrequest
   * @param sresult
   * @param end
   */
  public JsonCommandPacket(final JsonPacket jrequest, final String sresult,
                           final byte end) {
    request = jrequest.toString();
    result = sresult;
    send = end;
  }

  @Override
  public boolean hasGlobalBuffer() {
    return true;
  }

  @Override
  public void createAllBuffers(final LocalChannelReference lcr,
                               final int networkHeader)
      throws OpenR66ProtocolPacketException {
    final byte[] headerBytes =
        request != null? request.getBytes() : EMPTY_ARRAY;
    final int headerSize = headerBytes.length;
    final byte[] middleBytes = result != null? result.getBytes() : EMPTY_ARRAY;
    final int middleSize = middleBytes.length;
    final int endSize = 1;
    final int globalSize =
        networkHeader + LOCAL_HEADER_SIZE + headerSize + middleSize + endSize;
    int offset = networkHeader + LOCAL_HEADER_SIZE;
    global = ByteBufAllocator.DEFAULT.buffer(globalSize, globalSize);
    header = WaarpNettyUtil.slice(global, offset, headerSize);
    if (request != null) {
      header.writeBytes(headerBytes);
    }
    offset += headerSize;
    middle = WaarpNettyUtil.slice(global, offset, middleSize);
    if (result != null) {
      middle.writeBytes(middleBytes);
    }
    offset += middleSize;
    end = WaarpNettyUtil.slice(global, offset, endSize);
    end.writeByte(send);
  }

  @Override
  public String toString() {
    return "JsonCommandPacket: " + request + ':' + result + ':' + send;
  }

  @Override
  public byte getType() {
    return LocalPacketFactory.JSONREQUESTPACKET;
  }

  /**
   * @return the JsonPacket from request
   */
  public JsonPacket getJsonRequest() {
    try {
      return JsonPacket.createFromBuffer(request);
    } catch (final JsonParseException e) {
      return null;
    } catch (final JsonMappingException e) {
      return null;
    } catch (final IOException e) {
      return null;
    }
  }

  /**
   * @param result
   */
  public void setResult(final String result) {
    this.result = result;
    middle = null;
  }

  /**
   * @return the request
   */
  public String getRequest() {
    return request;
  }

  /**
   * @return the result
   */
  public String getResult() {
    return result;
  }

  /**
   * @return the type
   */
  public byte getTypeValid() {
    return send;
  }

}
