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
package org.waarp.commandexec.ssl.client;

import io.netty.channel.ChannelHandlerContext;
import org.waarp.commandexec.client.LocalExecClientHandler;
import org.waarp.commandexec.client.LocalExecClientInitializer;
import org.waarp.commandexec.utils.LocalExecDefaultResult;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import javax.net.ssl.SSLException;

/**
 *
 */
public class LocalExecSslClientHandler extends LocalExecClientHandler {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(LocalExecSslClientHandler.class);

  /**
   * @param factory
   */
  public LocalExecSslClientHandler(final LocalExecClientInitializer factory) {
    super(factory);
  }

  @Override
  public void channelRegistered(final ChannelHandlerContext ctx)
      throws Exception {
    WaarpSslUtility.addSslOpenedChannel(ctx.channel());
    super.channelRegistered(ctx);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx,
                              final Throwable cause) throws Exception {
    logger.warn("Unexpected exception from Outband while get information: " +
                firstMessage, cause);
    if (firstMessage) {
      firstMessage = false;
      result.set(LocalExecDefaultResult.BadTransmition);
      result.setException((Exception) cause);
      back = new StringBuilder("Error in LocalExec: ")
          .append(result.getException().getMessage()).append('\n');
    } else {
      if (cause instanceof SSLException) {
        // ignore ?
        logger.warn("Ignore exception ?", cause);
        return;
      }
      back.append("\nERROR while receiving answer: ");
      result.setException((Exception) cause);
      back.append(result.getException().getMessage()).append('\n');
    }
    actionBeforeClose(ctx.channel());
    WaarpSslUtility.closingSslChannel(ctx.channel());
  }
}
