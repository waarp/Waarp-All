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

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.ftp.core.config.FtpConfiguration;
import org.waarp.ftp.core.config.FtpInternalConfiguration;
import org.waarp.ftp.core.control.ftps.FtpsInitializer;
import org.waarp.ftp.core.session.FtpSession;

/**
 *
 */
class FtpsTemporaryFirstHandler extends ChannelDuplexHandler {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpsTemporaryFirstHandler.class);
  final FtpConfiguration configuration;
  final boolean active;
  /**
   * Internal store for the SessionInterface
   */
  FtpSession session;

  FtpsTemporaryFirstHandler(final FtpConfiguration configuration,
                            final boolean active) {
    this.configuration = configuration;
    this.active = active;
  }

  @Override
  public void channelRegistered(final ChannelHandlerContext ctx)
      throws Exception {
    ctx.channel().config().setAutoRead(false);
    super.channelRegistered(ctx);
  }

  protected void setSession(final Channel channel) {
    // First get the ftpSession from inetaddresses
    for (int i = 0; i < FtpInternalConfiguration.RETRYNB; i++) {
      session = configuration.getFtpSessionNoRemove(channel, active);
      if (session == null) {
        logger.debug("Session not found at try " + i);
        try {
          Thread.sleep(FtpInternalConfiguration.RETRYINMS);
        } catch (final InterruptedException e1) {//NOSONAR
          SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
          break;
        }
      } else {
        break;
      }
    }
    if (session == null) {
      // Not found !!!
      logger.error("Session not found!");
      WaarpSslUtility.closingSslChannel(channel);
      // Problem: control connection could not be directly informed!!!
      // Only timeout will occur
    }
  }

  private void superChannelActive(final ChannelHandlerContext ctx)
      throws Exception {
    super.channelActive(ctx);
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) {
    // Get the SslHandler in the current pipeline.
    final Channel channel = ctx.channel();

    if (session == null) {
      setSession(channel);
    }
    if (session == null) {
      logger.error("Cannot find session for SSL");
      return;
    }
    // Server: no renegotiation still, but possible clientAuthent
    // Mode is always as SSL Server mode.
    final SslHandler sslHandler = FtpsInitializer.waarpSslContextFactory
        .createHandlerServer(
            FtpsInitializer.waarpSslContextFactory.needClientAuthentication(),
            ctx.channel());
    WaarpSslUtility.addSslOpenedChannel(channel);
    // Get the SslHandler and begin handshake ASAP.
    logger.debug("SSL found but need handshake: {}", ctx.channel());
    final FtpsTemporaryFirstHandler myself = this;
    WaarpSslUtility.addSslHandler(null, ctx.pipeline(), sslHandler,
                                  new GenericFutureListener<Future<? super Channel>>() {
                                    @Override
                                    public void operationComplete(
                                        final Future<? super Channel> future) {
                                      try {
                                        logger.debug("Handshake: {}:{}",
                                                     future.isSuccess(),
                                                     future.get(),
                                                     future.cause());
                                        if (future.isSuccess()) {
                                          logger.debug(
                                              "End of initialization of SSL and data channel");
                                          myself.superChannelActive(ctx);
                                          ctx.pipeline().remove(myself);
                                        } else {
                                          ctx.close();
                                        }
                                      } catch (final Exception e) {
                                        ctx.close();
                                      }
                                    }
                                  });
  }

}
