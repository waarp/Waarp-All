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
package org.waarp.openr66.proxy.network;

import java.net.SocketAddress;
import java.util.HashMap;

import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;

/**
 * Proxy Entry
 * @author "Frederic Bregier"
 * 
 */
public class ProxyEntry {
    public static final HashMap<String, ProxyEntry> proxyEntries = new HashMap<String, ProxyEntry>();

    private String name;
    private SocketAddress localSocketAddress;
    private boolean localIsSsl;
    private SocketAddress remoteSocketAddress;
    private boolean remoteIsSsl;

    public static void add(SocketAddress localAddress, boolean localIsSsl,
            SocketAddress remoteAddress, boolean remoteIsSsl) throws OpenR66ProtocolSystemException {
        String sla = localAddress.toString();
        if (proxyEntries.containsKey(sla)) {
            throw new OpenR66ProtocolSystemException(
                    "Error in the configuration: Two entries with the same localAdress");
        }
        proxyEntries.put(sla,
                new ProxyEntry(sla, localAddress, localIsSsl, remoteAddress,
                        remoteIsSsl));
    }

    public static ProxyEntry get(String name) {
        synchronized (proxyEntries) {
            if (proxyEntries.containsKey(name)) {
                ProxyEntry proxyEntry = proxyEntries.get(name);
                return proxyEntry;
            } else {
                // error
                return null;
            }
        }
    }

    /**
     * @param name
     * @param localSocketAddress
     * @param localIsSsl
     * @param remoteSocketAddress
     * @param remoteIsSsl
     */
    private ProxyEntry(String name, SocketAddress localSocketAddress, boolean localIsSsl,
            SocketAddress remoteSocketAddress, boolean remoteIsSsl) {
        this.name = name;
        this.localSocketAddress = localSocketAddress;
        this.localIsSsl = localIsSsl;
        this.remoteSocketAddress = remoteSocketAddress;
        this.remoteIsSsl = remoteIsSsl;
    }

    /**
     * @return the localSocketAddress
     */
    public SocketAddress getLocalSocketAddress() {
        return localSocketAddress;
    }

    /**
     * @return the localIsSsl
     */
    public boolean isLocalSsl() {
        return localIsSsl;
    }

    /**
     * @return the remoteSocketAddress
     */
    public SocketAddress getRemoteSocketAddress() {
        return remoteSocketAddress;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the isSsl
     */
    public boolean isRemoteSsl() {
        return remoteIsSsl;
    }

    public String toString() {
        return "from: " + localSocketAddress.toString() + ":" + localIsSsl + " to: "
                + remoteSocketAddress.toString() + ":" + remoteIsSsl;
    }
}
