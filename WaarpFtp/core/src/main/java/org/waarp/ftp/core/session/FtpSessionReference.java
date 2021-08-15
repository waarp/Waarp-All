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
package org.waarp.ftp.core.session;

import io.netty.channel.Channel;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.ftp.core.utils.FtpChannelUtils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class that allows to retrieve a session when a connection occurs on the Data
 * network based on the
 * {@link InetAddress} of the remote client and the {@link InetSocketAddress} of
 * the server for Passive and
 * reverse for Active connections. This is particularly useful for Passive mode
 * connection since there is no
 * way to pass the session to the connected channel without this reference.
 */
public class FtpSessionReference {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpSessionReference.class);

  private static final byte[] LOOPBACK = { 0, 0, 0, 0 };
  private static final byte[] LOOPBACK2 = { 127, 0, 0, 1 };
  private static final InetAddress LOOPBACK_ADDRESS;

  static {
    InetAddress address = null;
    try {
      address = InetAddress.getByAddress(LOOPBACK2);
    } catch (final UnknownHostException e) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    }
    LOOPBACK_ADDRESS = address;
  }

  /**
   * Index of FtpSession References
   */
  public static class P2PAddress {
    /**
     * Remote Inet Address (no port)
     */
    public final InetAddress ipOnly;

    /**
     * Local Inet Socket Address (with port)
     */
    public final InetSocketAddress fullIp;

    public final long creationTime = System.currentTimeMillis();

    /**
     * Constructor from Channel
     *
     * @param channel
     */
    public P2PAddress(final Channel channel) {
      InetAddress ip = FtpChannelUtils.getRemoteInetAddress(channel);
      if (isLoopback(ip)) {
        ipOnly = LOOPBACK_ADDRESS;
      } else {
        ipOnly = ip;
      }
      InetSocketAddress isa = (InetSocketAddress) channel.localAddress();
      if (isLoopback(isa.getAddress())) {
        fullIp = new InetSocketAddress(LOOPBACK_ADDRESS, isa.getPort());
      } else {
        fullIp = isa;
      }
    }

    /**
     * Constructor from addresses
     *
     * @param address
     * @param inetSocketAddress
     */
    public P2PAddress(final InetAddress address,
                      final InetSocketAddress inetSocketAddress) {
      if (isLoopback(address)) {
        ipOnly = LOOPBACK_ADDRESS;
      } else {
        ipOnly = address;
      }
      if (isLoopback(inetSocketAddress.getAddress())) {
        fullIp = new InetSocketAddress(LOOPBACK_ADDRESS,
                                       inetSocketAddress.getPort());
      } else {
        fullIp = inetSocketAddress;
      }
    }

    /**
     * @return True if the P2Paddress is valid
     */
    private final boolean isValid() {
      return ipOnly != null && fullIp != null;
    }

    @Override
    public final boolean equals(final Object o) {
      if (o == null) {
        return false;
      }
      if (o instanceof P2PAddress) {
        final P2PAddress p2paddress = (P2PAddress) o;
        if (p2paddress.isValid() && isValid()) {
          boolean equal = p2paddress.fullIp.equals(fullIp) &&
                          p2paddress.ipOnly.equals(ipOnly);
          if (!equal) {
            boolean sameIp =
                p2paddress.fullIp.getAddress().equals(fullIp.getAddress()) ||
                (isLoopback(fullIp.getAddress()) &&
                 isLoopback(p2paddress.fullIp.getAddress()));
            equal = sameIp && p2paddress.fullIp.getPort() == fullIp.getPort();
          }
          return equal;
        }
      }
      return false;
    }

    @Override
    public final int hashCode() {
      return fullIp.hashCode() + ipOnly.hashCode();
    }

    @Override
    public String toString() {
      return ipOnly.toString() + "-" + fullIp.toString();
    }
  }

  private static boolean isLoopback(final InetAddress inetAddress) {
    return Arrays.equals(inetAddress.getAddress(), LOOPBACK2) ||
           Arrays.equals(inetAddress.getAddress(), LOOPBACK) ||
           inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress();
  }

  /**
   * Reference of FtpSession from InetSocketAddress
   */
  private final ConcurrentHashMap<P2PAddress, FtpSession> hashMap =
      new ConcurrentHashMap<P2PAddress, FtpSession>();

  /**
   * Constructor
   */
  public FtpSessionReference() {
    // nothing
  }

  private void cleanFtpSession() {
    // 10 minutes ago
    final long nowInPast = System.currentTimeMillis() - 10 * 60 * 1000;
    ArrayList<P2PAddress> list = new ArrayList<P2PAddress>();
    Enumeration<P2PAddress> enumeration = hashMap.keys();
    while (enumeration.hasMoreElements()) {
      final P2PAddress entry = enumeration.nextElement();
      if (entry.creationTime < nowInPast) {
        list.add(entry);
      }
    }
    for (P2PAddress pAddress : list) {
      hashMap.remove(pAddress);
    }
    list.clear();
  }

  /**
   * Add a session from a couple of addresses
   *
   * @param ipOnly
   * @param fullIp
   * @param session
   */
  public final void setNewFtpSession(final InetAddress ipOnly,
                                     final InetSocketAddress fullIp,
                                     final FtpSession session) {
    cleanFtpSession();
    final P2PAddress pAddress = new P2PAddress(ipOnly, fullIp);
    if (!pAddress.isValid()) {
      logger.error(
          "Couple invalid in setNewFtpSession: " + ipOnly + " : " + fullIp);
      return;
    }
    hashMap.put(pAddress, session);
  }

  /**
   * Return and remove the FtpSession
   *
   * @param channel
   *
   * @return the FtpSession if it exists associated to this channel
   */
  public final FtpSession getPassiveFtpSession(final Channel channel) {
    cleanFtpSession();
    // First check passive connection
    final P2PAddress pAddress = new P2PAddress(channel);
    if (!pAddress.isValid()) {
      logger.error("Couple invalid in getPassiveFtpSession: " + channel);
      return null;
    }
    return hashMap.remove(pAddress);
  }

  /**
   * @param channel
   *
   * @return the associated removed Passive FtpSession or null if not found
   */
  public final FtpSession findPassive(final Channel channel) {
    cleanFtpSession();
    InetAddress remote =
        ((InetSocketAddress) channel.remoteAddress()).getAddress();
    if (isLoopback(remote)) {
      remote = LOOPBACK_ADDRESS;
    }
    final int port = ((InetSocketAddress) channel.localAddress()).getPort();
    P2PAddress entry = null;
    Enumeration<P2PAddress> enumeration = hashMap.keys();
    while (enumeration.hasMoreElements()) {
      entry = enumeration.nextElement();
      if (entry.fullIp.getPort() == port && (entry.ipOnly.equals(remote) ||
                                             Arrays.equals(
                                                 entry.ipOnly.getAddress(),
                                                 remote.getAddress()))) {
        break;
      }
      entry = null;
    }
    if (entry != null) {
      logger.debug("Found from port only: {}", entry);
      return hashMap.remove(entry);
    }
    return null;
  }

  /**
   * Remove one FtpSession if possible
   *
   * @param channel
   */
  public final void delFtpSession(final Channel channel) {
    cleanFtpSession();
    final P2PAddress pAddress;
    pAddress = new P2PAddress(channel);
    if (!pAddress.isValid()) {
      return;
    }
    hashMap.remove(pAddress);
  }

  /**
   * Remove the FtpSession from couple of addresses
   *
   * @param ipOnly
   * @param fullIp
   */
  public final void delFtpSession(final InetAddress ipOnly,
                                  final InetSocketAddress fullIp) {
    cleanFtpSession();
    final P2PAddress pAddress = new P2PAddress(ipOnly, fullIp);
    if (!pAddress.isValid()) {
      logger.error(
          "Couple invalid in delFtpSession: " + ipOnly + " : " + fullIp);
      return;
    }
    hashMap.remove(pAddress);
  }

  /**
   * @return the number of active sessions
   */
  public final int sessionsNumber() {
    cleanFtpSession();
    return hashMap.size();
  }

}
