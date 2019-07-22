/**
 * This file is part of Waarp Project.
 *
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 *
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.openr66.protocol.localhandler.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;

/**
 * This class represents Abstract Packet with its header, middle and end parts. A Packet is composed
 * of one Header part, one Middle part (data), and one End part. Header: length field (4 bytes) =
 * Middle length field (4 bytes), End length field (4 bytes), type field (1 byte), ...<br>
 * Middle: (Middle length field bytes)<br>
 * End: (End length field bytes) = code status field (4 bytes), ...<br>
 *
 * @author frederic bregier
 */
public abstract class AbstractLocalPacket {
    protected ByteBuf header;

    protected ByteBuf middle;

    protected ByteBuf end;

    public AbstractLocalPacket(ByteBuf header, ByteBuf middle,
            ByteBuf end) {
        this.header = header;
        this.middle = middle;
        this.end = end;
    }

    public AbstractLocalPacket() {
        header = null;
        middle = null;
        end = null;
    }

    /**
     * Prepare the Header buffer
     *
     * @throws OpenR66ProtocolPacketException
     */
    public abstract void createHeader(LocalChannelReference lcr) throws OpenR66ProtocolPacketException;

    /**
     * Prepare the Middle buffer
     *
     * @throws OpenR66ProtocolPacketException
     */
    public abstract void createMiddle(LocalChannelReference lcr) throws OpenR66ProtocolPacketException;

    /**
     * Prepare the End buffer
     *
     * @throws OpenR66ProtocolPacketException
     */
    public abstract void createEnd(LocalChannelReference lcr) throws OpenR66ProtocolPacketException;

    /**
     *
     * @return the type of Packet
     */
    public abstract byte getType();

    @Override
    public abstract String toString();

    /**
     * @param lcr
     *            the LocalChannelReference in use
     * @return the ByteBuf as LocalPacket
     * @throws OpenR66ProtocolPacketException
     */
    public ByteBuf getLocalPacket(LocalChannelReference lcr) throws OpenR66ProtocolPacketException {
        final ByteBuf buf = Unpooled.buffer(4 * 3 + 1);// 3 header
        // lengths+type
        if (header == null) {
            createHeader(lcr);
        }
        final ByteBuf newHeader = header != null ? header
                : Unpooled.EMPTY_BUFFER;
        final int headerLength = 4 * 2 + 1 + newHeader.readableBytes();
        if (middle == null) {
            createMiddle(lcr);
        }
        final ByteBuf newMiddle = middle != null ? middle
                : Unpooled.EMPTY_BUFFER;
        final int middleLength = newMiddle.readableBytes();
        if (end == null) {
            createEnd(lcr);
        }
        final ByteBuf newEnd = end != null ? end
                : Unpooled.EMPTY_BUFFER;
        final int endLength = newEnd.readableBytes();
        buf.writeInt(headerLength);
        buf.writeInt(middleLength);
        buf.writeInt(endLength);
        buf.writeByte(getType());
        final ByteBuf ByteBuf = Unpooled.wrappedBuffer(
                buf, newHeader, newMiddle, newEnd);
        return ByteBuf;
    }

    public void clear() {
        if (header != null) {
            if (header.release()) {
                header = null;
            }
        }
        if (middle != null) {
            if (middle.release()) {
                middle = null;
            }
        }
        if (end != null) {
            if (end.release()) {
                end = null;
            }
        }
    }

    public void retain() {
        if (header != null) {
            header.retain();
        }
        if (middle != null) {
            middle.retain();
        }
        if (end != null) {
            end.retain();
        }
    }
}
