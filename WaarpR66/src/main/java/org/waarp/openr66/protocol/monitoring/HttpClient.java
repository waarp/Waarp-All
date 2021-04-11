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

package org.waarp.openr66.protocol.monitoring;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.internal.SocketUtils;
import org.joda.time.DateTime;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.http.restv2.utils.JsonUtils;

import javax.net.ssl.SSLException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.waarp.openr66.protocol.configuration.Configuration.*;

/**
 * HttpClient used by the MonitorExporterTransfers
 */
public class HttpClient {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpClient.class);

  private static final String HEADER_WAARP_ID = "X-WAARP-ID";
  private static final String HEADER_WAARP_START = "X-WAARP-START";
  private static final String HEADER_WAARP_STOP = "X-WAARP-STOP";

  private final URI finalUri;
  private final String host;
  private final boolean keepConnection;
  private final Bootstrap bootstrap;
  private int port;
  private Channel remoteRestChannel = null;

  private WaarpFuture futurePost = null;

  /**
   * @param remoteBaseUrl as 'http://myhost.com:8080' or 'https://myhost.com:8443'
   * @param endpoint as '/waarpr66monitor' or simply '/'
   * @param keepConnection True to keep the connexion opened, False to release the connexion each time
   * @param group the EventLoopGroup to use
   */
  public HttpClient(final String remoteBaseUrl, final String endpoint,
                    final boolean keepConnection, final EventLoopGroup group) {
    this.keepConnection = keepConnection;
    String uri;
    if (remoteBaseUrl.endsWith("/")) {
      uri = remoteBaseUrl + endpoint;
    } else {
      uri = remoteBaseUrl + "/" + endpoint;
    }

    try {
      finalUri = new URI(uri);
    } catch (URISyntaxException e) {
      logger.error("URI syntax error", e);
      throw new IllegalArgumentException(e);
    }
    String scheme = finalUri.getScheme() == null? "http" : finalUri.getScheme();
    host = finalUri.getHost() == null? "127.0.0.1" : finalUri.getHost();
    port = finalUri.getPort();
    if (port == -1) {
      if ("http".equalsIgnoreCase(scheme)) {
        port = 80;
      } else if ("https".equalsIgnoreCase(scheme)) {
        port = 443;
      }
    }

    if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
      logger.error("Only HTTP(S) is supported.");
      throw new IllegalArgumentException("Only HTTP(S) is supported.");
    }

    boolean ssl = "https".equalsIgnoreCase(scheme);
    final SslContext sslCtx;
    if (ssl) {
      try {
        sslCtx = SslContextBuilder.forClient().trustManager(
            InsecureTrustManagerFactory.INSTANCE).build();
      } catch (SSLException e) {
        logger.error("SslContext error", e);
        throw new IllegalArgumentException(e);
      }
    } else {
      sslCtx = null;
    }

    // Configure the client.
    bootstrap = new Bootstrap();
    WaarpNettyUtil.setBootstrap(bootstrap, group,
                                (int) Configuration.configuration
                                    .getTimeoutCon(),
                                configuration.getBlockSize() + 64, true);
    bootstrap.handler(new HttpClientInitializer(sslCtx, this));
  }

  /**
   * @param monitoredTransfers the Json objet to push as POST
   * @param start the DateTime for the 'from' interval
   * @param stop the DateTime for the 'to' interval
   * @param serverId the serverId that is sending this monitoring information
   *
   * @return True if the POST succeeded
   */
  public boolean post(ObjectNode monitoredTransfers, DateTime start,
                      DateTime stop, String serverId) {
    logger.debug("Start Post from {} to {} as {}", start, stop, serverId);
    if (keepConnection) {
      if (remoteRestChannel != null && !remoteRestChannel.isActive()) {
        remoteRestChannel.close();
        remoteRestChannel = null;
      }
    }
    if (remoteRestChannel == null) {
      ChannelFuture future =
          bootstrap.connect(SocketUtils.socketAddress(host, port));
      try {
        remoteRestChannel = future.sync().channel();
      } catch (InterruptedException e) {
        logger.error(e);
        return false;
      }
    }
    futurePost = new WaarpFuture(true);
    // Prepare Body
    final String body = JsonUtils.nodeToString(monitoredTransfers);
    final int length;
    final byte[] bbody;
    try {
      bbody = body.getBytes(WaarpStringUtils.UTF_8);
      length = body.length();
    } catch (UnsupportedEncodingException e) {
      logger.error(e);
      return false;
    }
    final ByteBuf buf = Unpooled.wrappedBuffer(bbody);
    HttpHeaders headers = new DefaultHttpHeaders(true);
    // Header set
    headers.set(HttpHeaderNames.HOST, host);
    if (keepConnection) {
      headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    } else {
      headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
    }
    headers.set(HEADER_WAARP_ID, serverId);
    headers.set(HEADER_WAARP_START, start == null? "" : start.toString());
    headers.set(HEADER_WAARP_STOP, stop.toString());
    headers.set(HttpHeaderNames.CONTENT_LENGTH, length);
    headers
        .set(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON);
    final HttpRequest request =
        new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
                               finalUri.toASCIIString(), headers);
    // Body set
    logger.debug("Request from {} to {} as {} length {}", start, stop, serverId,
                 length);
    logger.debug("{} {} {} {}", request.method(), request.uri(),
                 request.protocolVersion(), request.headers());
    remoteRestChannel.write(request);
    remoteRestChannel.write(buf);
    remoteRestChannel.writeAndFlush(DefaultLastHttpContent.EMPTY_LAST_CONTENT)
                     .awaitUninterruptibly();
    logger.debug("Ending Post from {} to {} as {}", start, stop, serverId);

    if (!keepConnection) {
      try {
        logger.debug("Wait for Close connection");
        remoteRestChannel.closeFuture().sync();
        remoteRestChannel = null;
      } catch (InterruptedException e) {
        logger.error(e);
        // ignore
      }
    }
    futurePost.awaitOrInterruptible();
    boolean result = futurePost.isSuccess();
    logger.info("End Post from {} to {} as {} with {}", start, stop, serverId,
                result);
    return result;
  }

  public boolean isKeepConnection() {
    return keepConnection;
  }

  public void setStatus(boolean ok) {
    if (ok) {
      futurePost.setSuccess();
    } else {
      futurePost.cancel();
    }
  }
}
