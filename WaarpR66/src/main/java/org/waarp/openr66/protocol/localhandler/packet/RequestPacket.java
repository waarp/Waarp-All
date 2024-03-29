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

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;

/**
 * Request class
 * <p>
 * header = "rulename MODETRANS" middle = way+"FILENAME BLOCKSIZE RANK specialId
 * code (optional length)" end =
 * "fileInformation"
 * <p>
 * or
 * <p>
 * header = "{rule:rulename, mode:MODETRANS}" middle = way{filename:FILENAME,
 * block:BLOCKSIZE, rank:RANK,
 * id:specialId, code:code, length:length}" end = "fileInformation"
 */
public class RequestPacket extends AbstractLocalPacket {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(RequestPacket.class);
  private static final String NOT_ENOUGH_DATA = "Not enough data";

  public enum TRANSFERMODE {
    UNKNOWNMODE, SENDMODE, RECVMODE, SENDMD5MODE, RECVMD5MODE, SENDTHROUGHMODE,
    RECVTHROUGHMODE, SENDMD5THROUGHMODE, RECVMD5THROUGHMODE
  }

  protected enum FIELDS {
    rule, mode, filename, block, rank, id, code, length, limit
  }

  protected static final byte REQVALIDATE = 0;

  protected static final byte REQANSWERVALIDATE = 1;

  protected final String rulename;

  protected final int mode;

  protected String filename;

  protected final int blocksize;

  protected int rank;

  protected long specialId;

  protected byte way;

  protected char code;

  protected long originalSize;

  protected long limit;

  protected final String transferInformation;

  protected String separator = PartnerConfiguration.getSEPARATOR_FIELD();

  /**
   * @param mode
   *
   * @return the same mode (RECV or SEND) in MD5 version
   */
  public static int getModeMD5(final int mode) {
    switch (mode) {
      case 1:
      case 2:
      case 5:
      case 6:
        return mode + 2;
      default:
        // nothing
    }
    return mode;
  }

  /**
   * @param mode
   *
   * @return true if this mode is a RECV(MD5) mode
   */
  public static boolean isRecvMode(final int mode) {
    return mode == TRANSFERMODE.RECVMODE.ordinal() ||
           mode == TRANSFERMODE.RECVMD5MODE.ordinal() ||
           mode == TRANSFERMODE.RECVTHROUGHMODE.ordinal() ||
           mode == TRANSFERMODE.RECVMD5THROUGHMODE.ordinal();
  }

  /**
   * @param mode
   * @param isRequested
   *
   * @return True if this mode is a THROUGH (MD5) mode
   */
  public static boolean isSendThroughMode(final int mode,
                                          final boolean isRequested) {
    return !isRequested && isSendThroughMode(mode) ||
           isRequested && isRecvThroughMode(mode);
  }

  /**
   * @param mode
   *
   * @return True if this mode is a SEND THROUGH (MD5) mode
   */
  public static boolean isSendThroughMode(final int mode) {
    return mode == TRANSFERMODE.SENDTHROUGHMODE.ordinal() ||
           mode == TRANSFERMODE.SENDMD5THROUGHMODE.ordinal();
  }

  /**
   * @param mode
   * @param isRequested
   *
   * @return True if this mode is a THROUGH (MD5) mode
   */
  public static boolean isRecvThroughMode(final int mode,
                                          final boolean isRequested) {
    return !isRequested && isRecvThroughMode(mode) ||
           isRequested && isSendThroughMode(mode);
  }

  /**
   * @param mode
   *
   * @return True if this mode is a RECV THROUGH (MD5) mode
   */
  public static boolean isRecvThroughMode(final int mode) {
    return mode == TRANSFERMODE.RECVTHROUGHMODE.ordinal() ||
           mode == TRANSFERMODE.RECVMD5THROUGHMODE.ordinal();
  }

  public static boolean isSendMode(final int mode) {
    return !isRecvMode(mode);
  }

  /**
   * @param mode
   *
   * @return True if this mode is a THROUGH mode (with or without MD5)
   */
  public static boolean isThroughMode(final int mode) {
    return mode >= TRANSFERMODE.SENDTHROUGHMODE.ordinal() &&
           mode <= TRANSFERMODE.RECVMD5THROUGHMODE.ordinal();
  }

  /**
   * @param mode
   *
   * @return true if this mode is a MD5 mode
   */
  public static boolean isMD5Mode(final int mode) {
    return mode == TRANSFERMODE.RECVMD5MODE.ordinal() ||
           mode == TRANSFERMODE.SENDMD5MODE.ordinal() ||
           mode == TRANSFERMODE.SENDMD5THROUGHMODE.ordinal() ||
           mode == TRANSFERMODE.RECVMD5THROUGHMODE.ordinal();
  }

  /**
   * @param mode1
   * @param mode2
   *
   * @return true if both modes are compatible (both send, or both recv)
   */
  public static boolean isCompatibleMode(final int mode1, final int mode2) {
    return isRecvMode(mode1) && isRecvMode(mode2) ||
           !isRecvMode(mode1) && !isRecvMode(mode2);
  }

  /**
   * @param headerLength
   * @param middleLength
   * @param endLength
   * @param buf
   *
   * @return the new RequestPacket from buffer
   *
   * @throws OpenR66ProtocolPacketException
   */
  public static RequestPacket createFromBuffer(final int headerLength,
                                               final int middleLength,
                                               final int endLength,
                                               final ByteBuf buf)
      throws OpenR66ProtocolPacketException {
    if (headerLength - 1 <= 0) {
      throw new OpenR66ProtocolPacketException(NOT_ENOUGH_DATA);
    }
    if (middleLength <= 1) {
      throw new OpenR66ProtocolPacketException(NOT_ENOUGH_DATA);
    }
    final byte[] bheader = new byte[headerLength - 1];
    final byte[] bmiddle = new byte[middleLength - 1];// valid is not in bmiddle
    final byte[] bend = new byte[endLength];
    buf.readBytes(bheader);
    final byte valid = buf.readByte();
    buf.readBytes(bmiddle);
    if (endLength > 0) {
      buf.readBytes(bend);
    }
    final String sheader = new String(bheader, WaarpStringUtils.UTF8);
    final String smiddle = new String(bmiddle, WaarpStringUtils.UTF8);
    final String send = new String(bend, WaarpStringUtils.UTF8);

    // check if JSON on header since it will directly starts with a JSON, in contrary to middle
    if (sheader.startsWith(PartnerConfiguration.BAR_JSON_FIELD)) {
      // JSON
      logger.debug("Request is using JSON");
      final ObjectNode map = JsonHandler.getFromString(sheader);
      final ObjectNode map2 = JsonHandler.getFromString(smiddle);
      return new RequestPacket(map.path(FIELDS.rule.name()).asText(),
                               map.path(FIELDS.mode.name()).asInt(),
                               map2.path(FIELDS.filename.name()).asText(),
                               map2.path(FIELDS.block.name()).asInt(),
                               map2.path(FIELDS.rank.name()).asInt(),
                               map2.path(FIELDS.id.name()).asLong(), valid,
                               send,
                               (char) map2.path(FIELDS.code.name()).asInt(),
                               map2.path(FIELDS.length.name()).asLong(),
                               // Get speed if it exists if not speed is set to 0
                               map2.path(FIELDS.limit.name()).asLong(0),
                               PartnerConfiguration.BAR_JSON_FIELD);
    }

    final String[] aheader =
        sheader.split(PartnerConfiguration.BLANK_SEPARATOR_FIELD);
    if (aheader.length != 2) {
      throw new OpenR66ProtocolPacketException(NOT_ENOUGH_DATA);
    }
    // FIX to check both ' ' and SEPARATOR_FIELD
    String[] amiddle = smiddle.split(PartnerConfiguration.BAR_SEPARATOR_FIELD);
    String sep = PartnerConfiguration.BAR_SEPARATOR_FIELD;
    if (amiddle.length < 5) {
      amiddle = smiddle.split(PartnerConfiguration.BLANK_SEPARATOR_FIELD);
      sep = PartnerConfiguration.BLANK_SEPARATOR_FIELD;
      if (amiddle.length < 5) {
        throw new OpenR66ProtocolPacketException(NOT_ENOUGH_DATA);
      }
    }
    int blocksize = Integer.parseInt(amiddle[1]);
    if (blocksize < 100) {
      blocksize = Configuration.configuration.getBlockSize();
    }
    final int rank = Integer.parseInt(amiddle[2]);
    final long specialId = Long.parseLong(amiddle[3]);
    final char code = amiddle[4].charAt(0);
    long originalSize = -1;
    if (amiddle.length > 5) {
      originalSize = Long.parseLong(amiddle[5]);
    }
    return new RequestPacket(aheader[0], Integer.parseInt(aheader[1]),
                             amiddle[0], blocksize, rank, specialId, valid,
                             send, code, originalSize, sep);
  }

  /**
   * @param rulename
   * @param mode
   * @param filename
   * @param blocksize
   * @param rank
   * @param specialId
   * @param valid
   * @param transferInformation
   * @param code
   * @param originalSize
   */
  private RequestPacket(final String rulename, final int mode,
                        final String filename, final int blocksize,
                        final int rank, final long specialId, final byte valid,
                        final String transferInformation, final char code,
                        final long originalSize, final String separator) {
    this.rulename = rulename;
    this.mode = mode;
    this.filename = filename;
    if (blocksize < 100) {
      this.blocksize = Configuration.configuration.getBlockSize();
    } else {
      this.blocksize = blocksize;
    }
    this.rank = rank;
    this.specialId = specialId;
    way = valid;
    this.transferInformation = transferInformation;
    this.code = code;
    this.originalSize = originalSize;
    this.separator = separator;
  }

  /**
   * @param rulename
   * @param mode
   * @param filename
   * @param blocksize
   * @param rank
   * @param specialId
   * @param transferInformation
   */
  public RequestPacket(final String rulename, final int mode,
                       final String filename, final int blocksize,
                       final int rank, final long specialId,
                       final String transferInformation,
                       final long originalSize, final String separator) {
    this(rulename, mode, filename, blocksize, rank, specialId, REQVALIDATE,
         transferInformation, ErrorCode.InitOk.code, originalSize, separator);
  }

  /**
   * Create a Request packet with a speed negociation
   */
  private RequestPacket(final String rulename, final int mode,
                        final String filename, final int blocksize,
                        final int rank, final long specialId, final byte valid,
                        final String transferInformation, final char code,
                        final long originalSize, final long limit,
                        final String separator) {
    this(rulename, mode, filename, blocksize, rank, specialId, valid,
         transferInformation, code, originalSize, separator);
    this.limit = limit;
  }

  @Override
  public final boolean hasGlobalBuffer() {
    return false;
  }

  @Override
  public final void createAllBuffers(final LocalChannelReference lcr,
                                     final int networkHeader) {
    throw new IllegalStateException("Should not be called");
  }

  @Override
  public final synchronized void createEnd(final LocalChannelReference lcr) {
    if (transferInformation != null) {
      end = WaarpNettyUtil.wrappedBuffer(
          transferInformation.getBytes(WaarpStringUtils.UTF8));
    }
  }

  @Override
  public final synchronized void createHeader(final LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    if (rulename == null || mode <= 0) {
      throw new OpenR66ProtocolPacketException(NOT_ENOUGH_DATA);
    }
    if (lcr.getPartner() != null && lcr.getPartner().useJson()) {
      logger.debug("Request will use JSON {}", lcr.getPartner());
      final ObjectNode node = JsonHandler.createObjectNode();
      JsonHandler.setValue(node, FIELDS.rule, rulename);
      JsonHandler.setValue(node, FIELDS.mode, mode);
      header = WaarpNettyUtil.wrappedBuffer(
          JsonHandler.writeAsString(node).getBytes(WaarpStringUtils.UTF8));
    } else {
      header =
          WaarpNettyUtil.wrappedBuffer(rulename.getBytes(WaarpStringUtils.UTF8),
                                       PartnerConfiguration.BLANK_SEPARATOR_FIELD.getBytes(
                                           WaarpStringUtils.UTF8),
                                       Integer.toString(mode)
                                              .getBytes(WaarpStringUtils.UTF8));
    }
  }

  @Override
  public final synchronized void createMiddle(final LocalChannelReference lcr)
      throws OpenR66ProtocolPacketException {
    if (filename == null) {
      throw new OpenR66ProtocolPacketException(NOT_ENOUGH_DATA);
    }
    final byte[] away = { way };
    if (lcr.getPartner() != null && lcr.getPartner().useJson()) {
      logger.debug("Request {} will use JSON {}", specialId, lcr.getPartner());
      final ObjectNode node = JsonHandler.createObjectNode();
      JsonHandler.setValue(node, FIELDS.filename, filename);
      JsonHandler.setValue(node, FIELDS.block, blocksize);
      JsonHandler.setValue(node, FIELDS.rank, rank);
      JsonHandler.setValue(node, FIELDS.id, specialId);
      JsonHandler.setValue(node, FIELDS.code, code);
      JsonHandler.setValue(node, FIELDS.length, originalSize);
      // Add limit if specified
      JsonHandler.setValue(node, FIELDS.limit, limit);
      middle = WaarpNettyUtil.wrappedBuffer(away,
                                            JsonHandler.writeAsString(node)
                                                       .getBytes(
                                                           WaarpStringUtils.UTF8));
    } else {
      middle = WaarpNettyUtil.wrappedBuffer(away, filename.getBytes(
                                                WaarpStringUtils.UTF8), separator.getBytes(WaarpStringUtils.UTF8),
                                            Integer.toString(blocksize)
                                                   .getBytes(
                                                       WaarpStringUtils.UTF8),
                                            separator.getBytes(
                                                WaarpStringUtils.UTF8),
                                            Integer.toString(rank).getBytes(
                                                WaarpStringUtils.UTF8),
                                            separator.getBytes(
                                                WaarpStringUtils.UTF8),
                                            Long.toString(specialId).getBytes(
                                                WaarpStringUtils.UTF8),
                                            separator.getBytes(
                                                WaarpStringUtils.UTF8),
                                            Character.toString(code).getBytes(
                                                WaarpStringUtils.UTF8),
                                            separator.getBytes(
                                                WaarpStringUtils.UTF8),
                                            Long.toString(originalSize)
                                                .getBytes(
                                                    WaarpStringUtils.UTF8));
    }
  }

  @Override
  public final byte getType() {
    return LocalPacketFactory.REQUESTPACKET;
  }

  @Override
  public final String toString() {
    return "RequestPacket: " + specialId + " : " + rulename + " : " + mode +
           " :  " + filename + " : " + transferInformation + " : " + blocksize +
           " : " + rank + " : " + way + " : " + code + " : " + originalSize +
           " : " + limit;
  }

  /**
   * @return the rulename
   */
  public final String getRulename() {
    return rulename;
  }

  /**
   * @return the filename
   */
  public final String getFilename() {
    return filename;
  }

  /**
   * @return the mode
   */
  public final int getMode() {
    return mode;
  }

  /**
   * @return True if this packet concerns a Retrieve operation
   */
  public final boolean isRetrieve() {
    return isRecvMode(mode);
  }

  /**
   * @return the fifinal Information
   */
  public final String getTransferInformation() {
    return transferInformation;
  }

  /**
   * @return the blocksize
   */
  public final int getBlocksize() {
    return blocksize;
  }

  /**
   * @return the rank
   */
  public final int getRank() {
    return rank;
  }

  /**
   * @param rank the rank to set
   */
  public final void setRank(final int rank) {
    this.rank = rank;
  }

  /**
   * @return the originalSize
   */
  public final long getOriginalSize() {
    return originalSize;
  }

  /**
   * @param originalSize the originalSize to set
   */
  public final void setOriginalSize(final long originalSize) {
    this.originalSize = originalSize;
  }

  /**
   * @param specialId the specialId to set
   */
  public final void setSpecialId(final long specialId) {
    this.specialId = specialId;
  }

  /**
   * @return the specialId
   */
  public final long getSpecialId() {
    return specialId;
  }

  /**
   * @return True if this packet is to be validated
   */
  public final boolean isToValidate() {
    return way == REQVALIDATE;
  }

  /**
   * Validate the request
   */
  public final synchronized void validate() {
    way = REQANSWERVALIDATE;
    middle = null;
  }

  /**
   * @param filename the filename to set
   */
  public void setFilename(final String filename) {
    this.filename = filename;
  }

  /**
   * @return the code
   */
  public final char getCode() {
    return code;
  }

  /**
   * @param code the code to set
   */
  public final void setCode(final char code) {
    this.code = code;
  }

  public final long getLimit() {
    return limit;
  }

  public final void setLimit(final long limit) {
    this.limit = limit;
  }
}
