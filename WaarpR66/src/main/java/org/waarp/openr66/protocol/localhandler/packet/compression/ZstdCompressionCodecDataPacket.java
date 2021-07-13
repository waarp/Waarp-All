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

package org.waarp.openr66.protocol.localhandler.packet.compression;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.compress.WaarpZstdCodec;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.packet.DataPacket;

import java.util.Arrays;

/**
 * ZSTD efficient compression mode (JNI, Unsafe or Safe mode)
 */
public class ZstdCompressionCodecDataPacket {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(ZstdCompressionCodecDataPacket.class);
  private final WaarpZstdCodec waarpZstdCodec = new WaarpZstdCodec();

  /**
   * @param dataPacket the DataPacket to compress in place
   */
  public void compress(final DataPacket dataPacket, final R66Session session)
      throws OpenR66ProtocolPacketException {
    final byte[] data = dataPacket.getData();
    final int originalSize = dataPacket.getLengthPacket();
    if (originalSize == 0) {
      return;
    }
    byte[] toCompress =
        session.getSessionReusableCompressionBuffer(originalSize);
    try {
      // Try using default half max buffer
      final int length = waarpZstdCodec.getCompressorCodec()
                                       .compress(data, originalSize, toCompress,
                                                 toCompress.length);
      if (length < toCompress.length) {
        final byte[] bytes = Arrays.copyOfRange(toCompress, 0, length);
        toCompress = bytes;
      }
      logger.debug("DataPacket Compression is {} on {} for BlockSize {}",
                   toCompress.length, originalSize, data.length);
      dataPacket.updateFromCompressionCodec(toCompress, length);
    } catch (final Exception e) {
      // Might be because toCompress is not enough wide
      logger.error("DataPacket Compression to {} on {} for BlockSize {}",
                   toCompress.length, dataPacket.getLengthPacket(),
                   dataPacket.getData().length, e);
      throw new OpenR66ProtocolPacketException(e);
    }
  }

  /**
   * @param dataPacket the DataPacket to uncompress in place
   */
  public void uncompress(final DataPacket dataPacket, final R66Session session)
      throws OpenR66ProtocolPacketException {
    final byte[] data = dataPacket.getData();
    final int originalSize = dataPacket.getLengthPacket();
    if (originalSize == 0) {
      return;
    }
    try {
      byte[] toDeCompress = session.getReusableBuffer(
          waarpZstdCodec.getCompressorCodec()
                        .getDecompressedSize(data, originalSize));
      final int length = waarpZstdCodec.getCompressorCodec()
                                       .decompress(data, originalSize,
                                                   toDeCompress,
                                                   toDeCompress.length);
      if (length != toDeCompress.length) {
        toDeCompress = Arrays.copyOf(toDeCompress, length);
      }
      logger.debug("DataPacket Decompression is {} on {} for BlockSize {}",
                   originalSize, length, data.length);
      dataPacket.updateFromCompressionCodec(toDeCompress, length);
    } catch (final Exception e) {
      logger.error("DataPacket Decompression was {} on {} for BlockSize {}",
                   originalSize, data.length, session.getBlockSize(), e);
      throw new OpenR66ProtocolPacketException(e);
    }
  }
}
