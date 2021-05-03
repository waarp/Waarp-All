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
package org.waarp.openr66.proxy.protocol.http;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.traffic.TrafficCounter;
import org.waarp.common.exception.FileTransferException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.ParametersChecker;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.gateway.kernel.http.HttpWriteCacheEnable;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.protocol.exception.OpenR66Exception;
import org.waarp.openr66.protocol.exception.OpenR66ExceptionTrappedFactory;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolBusinessNoWriteBackException;
import org.waarp.openr66.protocol.http.HttpFormattedHandler;

import java.io.IOException;
import java.util.Date;

import static org.waarp.openr66.protocol.configuration.Configuration.*;

/**
 * Handler for HTTP information support
 */
public class HttpFormattedHandlerProxyR66 extends HttpFormattedHandler {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpFormattedHandlerProxyR66.class);

  private enum REQUEST {
    index("index.html"), error("monitoring_header.html", "monitoring_end.html"),
    statusxml("");

    private static final String MONITOR = "monitor/";
    private final String header;
    private final String end;

    /**
     * Constructor for a unique file
     *
     * @param uniquefile
     */
    REQUEST(final String uniquefile) {
      header = uniquefile;
      end = uniquefile;
    }

    /**
     * @param header
     * @param end
     */
    REQUEST(final String header, final String end) {
      this.header = header;
      this.end = end;
    }

    /**
     * Reader for a unique file
     *
     * @return the content of the unique file
     */
    public String readFileUnique(final HttpFormattedHandlerProxyR66 handler) {
      return handler.readFileHeaderInternal(
          configuration.getHttpBasePath() + MONITOR + header);
    }

    public String readHeader(final HttpFormattedHandlerProxyR66 handler) {
      return handler.readFileHeaderInternal(
          configuration.getHttpBasePath() + MONITOR + header);
    }

    public String readEnd() {
      return WaarpStringUtils
          .readFile(configuration.getHttpBasePath() + MONITOR + end);
    }
  }

  final R66Session authentHttp = new R66Session();

  private String readFileHeaderInternal(final String filename) {
    final String value;
    try {
      value = WaarpStringUtils.readFileException(filename);
    } catch (final FileTransferException e) {
      logger.error("Error while trying to read: " + filename + ": {}",
                   e.getMessage());
      return "";
    }
    final StringBuilder builder = new StringBuilder(value);

    WaarpStringUtils.replace(builder, REPLACEMENT.XXXDATEXXX.toString(),
                             new Date().toString());
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXLOCACTIVEXXX.toString(),
                             Integer.toString(
                                 configuration.getLocalTransaction()
                                              .getNumberLocalChannel()));
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXNETACTIVEXXX.toString(),
                             Integer.toString(
                                 configuration.getLocalTransaction()
                                              .getNumberLocalChannel()));
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXHOSTIDXXX.toString(),
                             configuration.getHostId());
    final TrafficCounter trafficCounter =
        configuration.getGlobalTrafficShapingHandler().trafficCounter();
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXBANDWIDTHXXX.toString(),
                             "IN:" +
                             trafficCounter.lastReadThroughput() / 131072 +
                             "Mbits&nbsp;&nbsp;OUT:" +
                             trafficCounter.lastWriteThroughput() / 131072 +
                             "Mbits");
    WaarpStringUtils.replace(builder, REPLACEMENT.XXXLANGXXX.toString(), lang);
    return builder.toString();
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx,
                              final FullHttpRequest msg) {
    isCurrentRequestXml = false;
    isCurrentRequestJson = false;
    status = HttpResponseStatus.OK;
    final FullHttpRequest request = this.request = msg;
    final QueryStringDecoder queryStringDecoder =
        new QueryStringDecoder(request.uri());
    uriRequest = queryStringDecoder.path();
    logger.debug("Msg: {}", uriRequest);
    if (uriRequest.contains("gre/") || uriRequest.contains("img/") ||
        uriRequest.contains("res/") || uriRequest.contains("favicon.ico")) {
      HttpWriteCacheEnable
          .writeFile(request, ctx, configuration.getHttpBasePath() + uriRequest,
                     "XYZR66NOSESSION");
      return;
    }
    char cval = 'z';
    long nb = LIMITROW;
    // check the URI
    if ("/statusxml".equalsIgnoreCase(uriRequest)) {
      cval = '5';
      nb = 0; // since it could be the default or setup by request
      isCurrentRequestXml = true;
    } else if ("/statusjson".equalsIgnoreCase(uriRequest)) {
      cval = '7';
      nb = 0; // since it could be the default or setup by request
      isCurrentRequestJson = true;
    }
    if (request.method() == HttpMethod.GET) {
      params = queryStringDecoder.parameters();
    }
    final boolean getMenu = cval == 'z';
    final boolean extraBoolean = false;
    if (!params.isEmpty()) {
      final String langarg = getTrimValue("setLng");
      if (ParametersChecker.isNotEmpty(langarg)) {
        lang = langarg;
      }
    }
    if (getMenu) {
      responseContent.append(REQUEST.index.readFileUnique(this));
    } else {
      // Use value 0=Active 1=Error 2=Done 3=All
      switch (cval) {
        case '5':
          statusxml(ctx, nb, extraBoolean);
          break;
        case '7':
          statusjson(ctx, nb, extraBoolean);
          break;
        default:
          responseContent.append(REQUEST.index.readFileUnique(this));
      }
    }
    writeResponse(ctx);
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx,
                              final Throwable cause) {
    final OpenR66Exception exception = OpenR66ExceptionTrappedFactory
        .getExceptionFromTrappedException(ctx.channel(), cause);
    if (exception != null) {
      if (!(exception instanceof OpenR66ProtocolBusinessNoWriteBackException)) {
        if (cause instanceof IOException) {
          // Nothing to do
          return;
        }
        logger.warn("Exception in HttpHandler {}", exception.getMessage());
      }
      if (ctx.channel().isActive()) {
        sendError(ctx, HttpResponseStatus.BAD_REQUEST);
      }
    } else {
      // Nothing to do
    }
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx)
      throws Exception {
    super.channelInactive(ctx);
    logger.debug("Closed");
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    logger.debug("Connected");
    authentHttp.getAuth()
               .specialNoSessionAuth(false, configuration.getHostId());
    super.channelActive(ctx);
    final ChannelGroup group = configuration.getHttpChannelGroup();
    if (group != null) {
      group.add(ctx.channel());
    }
  }
}
