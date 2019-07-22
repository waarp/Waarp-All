/*
 *  This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright 2009, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 *  individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at your
 *  option) any later version.
 *
 *  Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 *  A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  Waarp . If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.waarp.openr66.protocol.http.restv2.errors;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * This class represents a user error encountered during the processing of
 * a request. To create a new Error instance, use the factory method that corresponds to
 * the desired error in the {@link RestErrors} factory class.
 * To objectToJson a {@code Error} object as a JSON String to be sent back,
 * use the {@code objectToJson} method with the desired {@code Error}
 * object and {@link Locale}. To objectToJson an entire list of errors, use
 * the {@code serializeErrors} method instead.
 */
public class RestError {

    /**
     * The name of the property in the {@code restmessages} ResourceBundle
     * corresponding to the error message.
     */
    private final String msgKey;

    /**
     * The message arguments (typically field or parameter names) used to give
     * more context on the cause of the error.
     */
    private final String[] args;

    /** The error's code in the REST API specification. */
    private final Integer code;

    /**
     * Creates an object representing the response message to a request which
     * produced an error 401 - Bad Request. Should never be called outside of
     * the {@link RestErrors} factory class.
     *
     * @param msgKey the message's property name
     * @param args   the message's parameters
     * @param code   the error's code
     */
    RestError(String msgKey, String[] args, int code) {
        this.msgKey = msgKey;
        this.args = args;
        this.code = code;
    }

    /**
     * Returns the error as an {@link ObjectNode}.
     *
     * @param lang  the language of the error message
     * @return      the serialized RestError object
     */
    public ObjectNode makeNode(Locale lang) {
        ResourceBundle bundle = ResourceBundle.getBundle("restmessages", lang);
        String message = String.format(lang, bundle.getString(msgKey), (Object[]) args);

        ObjectNode response = new ObjectNode(JsonNodeFactory.instance);
        response.put("message", message);
        response.put("errorCode", code);
        return response;
    }
}
