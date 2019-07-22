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
package org.waarp.openr66.protocol.localhandler.packet.json;

import java.io.IOException;

import org.waarp.common.json.JsonHandler;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Json Object Command Message class for JsonCommandPacket
 * 
 * 1 string = comment
 * 
 * @author frederic bregier
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public class JsonPacket {
    /**
     * @return the ObjectNode corresponding to this object
     * @throws OpenR66ProtocolPacketException
     */
    public ObjectNode createObjectNode() throws OpenR66ProtocolPacketException {
        try {
            String value = JsonHandler.mapper.writeValueAsString(this);
            return (ObjectNode) JsonHandler.mapper.readTree(value);
        } catch (JsonProcessingException e) {
            throw new OpenR66ProtocolPacketException("Json exception", e);
        } catch (IOException e) {
            throw new OpenR66ProtocolPacketException("Json exception", e);
        }
    }

    private String comment;
    private byte requestUserPacket;

    /**
     * @return the requestUserPacket
     */
    public byte getRequestUserPacket() {
        return requestUserPacket;
    }

    /**
     * @param requestUserPacket
     *            the requestUserPacket to set
     */
    public void setRequestUserPacket(byte requestUserPacket) {
        this.requestUserPacket = requestUserPacket;
    }

    /**
     * Default value to set
     */
    public void setRequestUserPacket() {
        throw new IllegalArgumentException("Not defined");
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment
     *            the comment to set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return JsonHandler.writeAsString(this);
    }

    /**
     * @param value
     * @return the new JsonPacket from buffer
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    public static JsonPacket createFromBuffer(String value) throws JsonParseException, JsonMappingException,
            IOException {
        if (value != null && !value.isEmpty()) {
            return JsonHandler.mapper.readValue(value, JsonPacket.class);
        }
        return null;
    }

    /**
     * Set from other Json
     * 
     * @param json
     */
    public void fromJson(JsonPacket json) {
        this.comment = json.comment;
        this.requestUserPacket = json.requestUserPacket;
    }
}
