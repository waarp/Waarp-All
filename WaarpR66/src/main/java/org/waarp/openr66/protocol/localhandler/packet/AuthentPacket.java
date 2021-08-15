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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoSslException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.utils.R66Versions;

/**
 * Request Authentication class
 * <p>
 * header = "hostId" middle = "key bytes" end = localId + way + (optional
 * version: could be a JSON on the form
 * version.{})
 */
public class AuthentPacket extends AbstractLocalPacket {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AuthentPacket.class);

  private static final String NOT_ENOUGH_DATA = "Not enough data";

  private static final byte ASKVALIDATE = 0;

  private static final byte ANSWERVALIDATE = 1;

  private final Integer localId;

  private byte way;

  private String version;

  private String hostId;

  private byte[] key;

  /**
   * @param headerLength
   * @param middleLength
   * @param endLength
   * @param buf
   *
   * @return the new AuthentPacket from buffer
   *
   * @throws OpenR66ProtocolPacketException
   */
  public static AuthentPacket createFromBuffer(final int headerLength,
                                               final int middleLength,
                                               final int endLength,
                                               final ByteBuf buf)
      throws OpenR66ProtocolPacketException {
    if (headerLength - 1 <= 0) {
      throw new OpenR66ProtocolPacketException(NOT_ENOUGH_DATA);
    }
    if (middleLength <= 0) {
      throw new OpenR66ProtocolPacketException(NOT_ENOUGH_DATA);
    }
    if (endLength < 5) {
      throw new OpenR66ProtocolPacketException(NOT_ENOUGH_DATA);
    }
    final byte[] bheader = new byte[headerLength - 1];
    final byte[] bmiddle = new byte[middleLength];
    buf.readBytes(bheader);
    buf.readBytes(bmiddle);
    // end part
    final Integer newId = buf.readInt();
    final byte valid = buf.readByte();
    String version =
        R66Versions.V2_4_12.getVersion(); // first base reference where it is unacceptable
    if (endLength > 5) {
      // version
      final byte[] bversion = new byte[endLength - 5];
      buf.readBytes(bversion);
      version = new String(bversion, WaarpStringUtils.UTF8);
    }
    final String sheader = new String(bheader, WaarpStringUtils.UTF8);
    return new AuthentPacket(sheader, bmiddle, newId, valid, version);
  }

  /**
   * @param hostId
   * @param key
   * @param newId
   * @param valid
   * @param version
   */
  private AuthentPacket(final String hostId, final byte[] key,
                        final Integer newId, final byte valid,
                        final String version) {
    this.hostId = hostId;
    this.key = key;
    localId = newId;
    way = valid;
    Configuration.configuration.getVersions().put(hostId,
                                                  new PartnerConfiguration(
                                                      hostId, version));
    logger.debug("Receive version {}", version);
    this.version = version;
  }

  /**
   * @param hostId
   * @param key
   * @param newId
   */
  public AuthentPacket(final String hostId, final byte[] key,
                       final Integer newId) {
    this.hostId = hostId;
    this.key = key;
    localId = newId;
    way = ASKVALIDATE;
    if (!Configuration.configuration.getVersions().containsKey(hostId)) {
      Configuration.configuration.getVersions().putIfAbsent(hostId,
                                                            new PartnerConfiguration(
                                                                hostId));
    }
    version = Configuration.configuration.getVersions().get(hostId).toString();
    logger.debug("Will send version {}", version);
  }

  @Override
  public final boolean hasGlobalBuffer() {
    return true;
  }

  @Override
  public final synchronized void createAllBuffers(
      final LocalChannelReference lcr, final int networkHeader)
      throws OpenR66ProtocolPacketException {
    if (hostId == null || key == null) {
      throw new OpenR66ProtocolPacketException(NOT_ENOUGH_DATA);
    }
    final byte[] hostIdByte = hostId.getBytes(WaarpStringUtils.UTF8);
    final int hostIdSize = hostIdByte.length;
    final int keySize = key.length;
    final byte[] bversion =
        version != null? version.getBytes(WaarpStringUtils.UTF8) : null;
    final int endSize = 5 + (version != null? bversion.length : 0);
    final int globalSize =
        networkHeader + hostIdSize + keySize + endSize + LOCAL_HEADER_SIZE;
    int offset = networkHeader + LOCAL_HEADER_SIZE;
    global = ByteBufAllocator.DEFAULT.ioBuffer(globalSize, globalSize);
    header = WaarpNettyUtil.slice(global, offset, hostIdSize);
    header.writeBytes(hostIdByte);
    offset += hostIdSize;
    middle = WaarpNettyUtil.slice(global, offset, keySize);
    middle.writeBytes(key);
    offset += keySize;
    end = WaarpNettyUtil.slice(global, offset, endSize);
    end.writeInt(localId);
    end.writeByte(way);
    if (version != null) {
      end.writeBytes(bversion);
    }
  }

  @Override
  public final byte getType() {
    return LocalPacketFactory.AUTHENTPACKET;
  }

  @Override
  public final String toString() {
    return "AuthentPacket: " + hostId + ' ' + localId + ' ' + way + ' ' +
           version;
  }

  /**
   * @return the hostId
   */
  public final String getHostId() {
    return hostId;
  }

  /**
   * @return the key
   */
  public final byte[] getKey() {
    return key;
  }

  /**
   * @return the localId
   */
  public final Integer getLocalId() {
    return localId;
  }

  /**
   * @return True if this packet is to be validated
   */
  public final boolean isToValidate() {
    return way == ASKVALIDATE;
  }

  /**
   * Validate the connection
   */
  public final void validate(final boolean isSSL) {
    way = ANSWERVALIDATE;
    DbHostAuth auth = isSSL? Configuration.configuration.getHostSslAuth() :
        Configuration.configuration.getHostAuth();
    try {
      hostId = Configuration.configuration.getHostId(isSSL);
    } catch (final OpenR66ProtocolNoSslException e) {
      hostId = Configuration.configuration.getHostId();
      auth = Configuration.configuration.getHostAuth();
    }
    key = FilesystemBasedDigest.passwdCrypt(auth.getHostkey());
    if (!Configuration.configuration.getVersions().containsKey(hostId)) {
      Configuration.configuration.getVersions().putIfAbsent(hostId,
                                                            new PartnerConfiguration(
                                                                hostId));
    }
    version = Configuration.configuration.getVersions().get(hostId).toString();
    logger.debug("Validate version {}", version);
    clear();
  }
}
