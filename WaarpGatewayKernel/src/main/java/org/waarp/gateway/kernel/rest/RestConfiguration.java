/**
   This file is part of Waarp Project.

   Copyright 2009, Frederic Bregier, and individual contributors by the @author
   tags. See the COPYRIGHT.txt in the distribution for a full listing of
   individual contributors.

   All Waarp Project is free software: you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as published 
   by the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.

   Waarp is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Waarp .  If not, see <http://www.gnu.org/licenses/>.
 */
package org.waarp.gateway.kernel.rest;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.waarp.common.crypto.HmacSha256;
import org.waarp.common.exception.CryptoException;
import org.waarp.common.utility.WaarpStringUtils;

/**
 * General RestConfiguration model
 * 
 * @author "Frederic Bregier"
 *
 */
public class RestConfiguration {
    public static enum CRUD {
        CREATE(0x01),
        READ(0x02),
        UPDATE(0x04),
        DELETE(0x08),
        ALL(0x0F);

        public byte mask;

        CRUD(int mask) {
            this.mask = (byte) mask;
        }

        public boolean isValid(byte tocheck) {
            return (tocheck & mask) != 0;
        }
    }

    /**
     * SERVER REST interface using explicit address (null means all available)
     */
    public String REST_ADDRESS = null;
    /**
     * Http REST port (SSL or not SSL)
     */
    public int REST_PORT = -1;
    /**
     * SERVER REST interface using SSL
     */
    public boolean REST_SSL = false;
    /**
     * SERVER REST interface using time limit (default: no limit <= 0)
     */
    public long REST_TIME_LIMIT = -1;
    /**
     * SERVER REST interface using authentication
     */
    public boolean REST_AUTHENTICATED = false;
    /**
     * SERVER REST interface using signature
     */
    public boolean REST_SIGNATURE = true;
    /**
     * Key for signature in SHA-256
     *
     */
    public HmacSha256 hmacSha256 = null;
    /**
     * SERVER REST interface allowing one Handler and associated CRUD (or equivalent POST, GET, PUT, DELETE) methods
     * (2^0 for active, 2^1 as Create/POST, 2^2 as Read/GET, 2^3 as Update/PUT, 2^4 as Delete/DELETE)
     */
    public byte[] RESTHANDLERS_CRUD = null;

    /**
     * Associated RestMethod Handlers
     */
    public HashMap<String, RestMethodHandler> restHashMap = new HashMap<String, RestMethodHandler>();

    /**
     * Set Key from String directly
     * 
     * @param authentKey
     */
    public void initializeKey(String authentKey) {
        hmacSha256 = new HmacSha256();
        hmacSha256.setSecretKey(authentKey.getBytes(WaarpStringUtils.UTF8));
    }

    /**
     * Set Key from file
     * 
     * @param authentKey
     * @throws CryptoException
     * @throws IOException
     */
    public void initializeKey(File authentKey) throws CryptoException, IOException {
        hmacSha256 = new HmacSha256();
        hmacSha256.setSecretKey(authentKey);
    }

    public String toString() {
        String result = "{address: " + REST_ADDRESS + ", port: " + REST_PORT + ", ssl: " + REST_SSL + ", time: "
                + REST_TIME_LIMIT +
                ", authent:" + REST_AUTHENTICATED + ", signature: " + REST_SIGNATURE + ", handlers: [";
        for (Entry<String, RestMethodHandler> elt : restHashMap.entrySet()) {
            result += elt.getKey() + "=" + elt.getValue().methods + ", ";
        }
        result += "], crud: [";
        for (byte crud : RESTHANDLERS_CRUD) {
            result += (int) crud + ", ";
        }
        result += "] }";
        return result;
    }
}
