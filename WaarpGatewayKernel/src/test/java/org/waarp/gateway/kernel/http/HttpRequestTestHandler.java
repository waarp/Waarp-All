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

package org.waarp.gateway.kernel.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.cookie.Cookie;
import org.waarp.gateway.kernel.HttpPageHandler;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;

public class HttpRequestTestHandler extends HttpRequestHandler {
  /**
   * @param baseStaticPath
   * @param cookieSession
   * @param httpPageHandler
   */
  protected HttpRequestTestHandler(final String baseStaticPath,
                                   final String cookieSession,
                                   final HttpPageHandler httpPageHandler) {
    super(baseStaticPath, cookieSession, httpPageHandler);
  }

  @Override
  protected void checkConnection(final ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException {

  }

  @Override
  protected void error(final ChannelHandlerContext ctx) {

  }

  @Override
  protected boolean isCookieValid(final Cookie cookie) {
    return true;
  }

  @Override
  protected String getFilename() {
    return "/tmp/content";
  }

  @Override
  protected void beforeSimplePage(final ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException {

  }

  @Override
  protected void finalDelete(final ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException {

  }

  @Override
  protected void finalGet(final ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException {

  }

  @Override
  protected void finalPostUpload(final ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException {

  }

  @Override
  protected void finalPost(final ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException {

  }

  @Override
  protected void finalPut(final ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException {

  }

  @Override
  public void businessValidRequestAfterAllDataReceived(
      final ChannelHandlerContext ctx) throws HttpIncorrectRequestException {

  }
}
