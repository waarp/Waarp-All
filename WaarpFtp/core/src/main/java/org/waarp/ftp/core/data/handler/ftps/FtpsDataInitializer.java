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
package org.waarp.ftp.core.data.handler.ftps;

import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.ftp.core.command.FtpArgumentCode.TransferMode;
import org.waarp.ftp.core.command.FtpArgumentCode.TransferStructure;
import org.waarp.ftp.core.config.FtpConfiguration;
import org.waarp.ftp.core.control.ftps.FtpsInitializer;
import org.waarp.ftp.core.data.handler.DataBusinessHandler;
import org.waarp.ftp.core.data.handler.DataNetworkHandler;
import org.waarp.ftp.core.data.handler.FtpDataInitializer;
import org.waarp.ftp.core.data.handler.FtpDataModeCodec;

/**
 *
 */
public class FtpsDataInitializer extends FtpDataInitializer {

  /**
   * Constructor which Initializes some data
   *
   * @param dataBusinessHandler
   * @param configuration
   * @param active
   */
  public FtpsDataInitializer(
      final Class<? extends DataBusinessHandler> dataBusinessHandler,
      final FtpConfiguration configuration, final boolean active) {
    super(dataBusinessHandler, configuration, active);
  }

  /**
   * Create the pipeline with Handler, ObjectDecoder, ObjectEncoder.
   */
  @Override
  public void initChannel(final SocketChannel ch) throws Exception {
    final ChannelPipeline pipeline = ch.pipeline();
    // Add SSL as first element in the pipeline
    final SslHandler sslHandler =
        FtpsInitializer.waarpSslContextFactory.createHandlerServer(
            FtpsInitializer.waarpSslContextFactory.needClientAuthentication(),
            ch);
    WaarpSslUtility.addSslOpenedChannel(ch);
    pipeline.addLast("ssl", sslHandler);
    // Add default codec but they will change during the channelActive
    pipeline.addLast(FtpDataInitializer.CODEC_MODE,
                     new FtpDataModeCodec(TransferMode.STREAM,
                                          TransferStructure.FILE));
    pipeline.addLast(FtpDataInitializer.CODEC_LIMIT,
                     configuration.getFtpInternalConfiguration()
                                  .getGlobalTrafficShapingHandler());
    final ChannelTrafficShapingHandler limitChannel =
        configuration.getFtpInternalConfiguration()
                     .newChannelTrafficShapingHandler();
    if (limitChannel != null) {
      pipeline.addLast(FtpDataInitializer.CODEC_LIMIT + "CHANNEL",
                       limitChannel);
    }
    pipeline.addLast(FtpDataInitializer.CODEC_TYPE, ftpDataTypeCodec);
    pipeline.addLast(FtpDataInitializer.CODEC_STRUCTURE, ftpDataStructureCodec);
    // and then business logic. New one on every connection
    final DataBusinessHandler newbusiness =
        dataBusinessHandler.getDeclaredConstructor().newInstance();
    final DataNetworkHandler newNetworkHandler =
        new DataNetworkHandler(configuration, newbusiness, isActive);
    pipeline.addLast(
        configuration.getFtpInternalConfiguration().getDataExecutor(),
        FtpDataInitializer.HANDLER, newNetworkHandler);
  }
}
