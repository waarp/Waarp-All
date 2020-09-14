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
package org.waarp.common.crypto.ssl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.ssl.SslHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import javax.net.ssl.SSLEngine;

/**
 *
 */
public class WaarpSslHandler extends SslHandler {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpSslHandler.class);

  public WaarpSslHandler(final SSLEngine engine) {
    super(engine);
  }

  public WaarpSslHandler(final SSLEngine engine, final boolean startTls) {
    super(engine, startTls);
  }

  @Override
  public void handlerAdded(final ChannelHandlerContext ctx) throws Exception {
    ctx.channel().config().setAutoRead(true);
    super.handlerAdded(ctx);
    logger.debug("Ssl Handler added: {}", ctx.channel());
  }
}
