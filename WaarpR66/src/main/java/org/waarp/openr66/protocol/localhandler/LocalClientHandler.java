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
package org.waarp.openr66.protocol.localhandler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66Exception;
import org.waarp.openr66.protocol.exception.OpenR66ExceptionTrappedFactory;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessNoWriteBackException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNetworkException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolShutdownException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.ErrorPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.utils.ChannelCloseTimer;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import static org.waarp.openr66.context.R66FiniteDualStates.*;

/**
 *
 */
class LocalClientHandler
    extends SimpleChannelInboundHandler<AbstractLocalPacket> {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(LocalClientHandler.class);

  /**
   * Local Channel Reference
   */
  private volatile LocalChannelReference localChannelReference = null;

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    logger.debug("Local Client Channel Connected: " + ctx.channel().id());
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    logger.debug("Local Client Channel Closed: {}", ctx.channel().id());
  }

  /**
   * Initiate the LocalChannelReference
   *
   * @param channel
   *
   * @throws InterruptedException
   * @throws OpenR66ProtocolNetworkException
   */
  private void initLocalClientHandler(Channel channel)
      throws InterruptedException, OpenR66ProtocolNetworkException {
    int i = 0;
    if (localChannelReference == null) {
      for (i = 0; i < Configuration.RETRYNB; i++) {
        localChannelReference =
            Configuration.configuration.getLocalTransaction()
                                       .getFromId(channel.id().hashCode());
        if (localChannelReference != null) {
          return;
        }
        Thread.sleep(Configuration.RETRYINMS);
        Thread.yield();
      }
      logger.warn("Cannot find local connection");
    }
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx,
                              AbstractLocalPacket msg) throws Exception {
    if (localChannelReference == null) {
      initLocalClientHandler(ctx.channel());
    }
    // only Startup Packet should arrived here !
    final AbstractLocalPacket packet = msg;
    if (packet.getType() != LocalPacketFactory.STARTUPPACKET) {
      logger.error(
          "Local Client Channel Recv wrong packet: " + ctx.channel().id() +
          " : " + packet.toString());
      throw new OpenR66ProtocolSystemException(
          "Should not be here: Wrong packet received {" + packet.toString() +
          "}");
    }
    logger.debug(
        "LocalClientHandler initialized: " + (localChannelReference != null));
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
      throws Exception {
    // informs network of the problem
    final Channel channel = ctx.channel();
    logger.debug("Local Client Channel Exception: {}", channel.id(), cause);
    if (localChannelReference == null) {
      initLocalClientHandler(channel);
    }
    if (localChannelReference != null) {
      final OpenR66Exception exception = OpenR66ExceptionTrappedFactory
          .getExceptionFromTrappedException(channel, cause);
      localChannelReference.sessionNewState(ERROR);
      if (exception != null) {
        if (exception instanceof OpenR66ProtocolShutdownException) {
          ChannelUtils.startShutdown();
          /*
           * Dont close, thread will do logger.debug("Will close channel"); Channels.close(e.channel());
           */
          return;
        } else if (exception instanceof OpenR66ProtocolBusinessNoWriteBackException) {
          logger.error("Will close channel", exception);
          channel.close();
          return;
        } else if (exception instanceof OpenR66ProtocolNoConnectionException) {
          logger.error("Will close channel", exception);
          channel.close();
          return;
        }
        final ErrorPacket errorPacket = new ErrorPacket(exception.getMessage(),
                                                        ErrorCode.RemoteError
                                                            .getCode(),
                                                        ErrorPacket.FORWARDCLOSECODE);
        ChannelUtils
            .writeAbstractLocalPacket(localChannelReference, errorPacket, true);
        if (!localChannelReference.getFutureRequest().isDone()) {
          localChannelReference.invalidateRequest(
              new R66Result(exception, localChannelReference.getSession(), true,
                            ErrorCode.Internal, null));
        }
      } else {
        // Nothing to do
        return;
      }
    }
    logger.debug("Will close channel");
    ChannelCloseTimer.closeFutureChannel(channel);
  }

}
