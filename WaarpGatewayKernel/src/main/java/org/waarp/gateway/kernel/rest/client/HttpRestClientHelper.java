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
package org.waarp.gateway.kernel.rest.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringEncoder;
import org.joda.time.DateTime;
import org.waarp.common.crypto.HmacSha256;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.exception.CryptoException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.common.utility.WaarpThreadFactory;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.rest.RestArgument;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Http Rest Client helper
 */
public class HttpRestClientHelper {
  private static final String
      NEED_MORE_ARGUMENTS_HTTP_HOST_PORT_URI_METHOD_USER_SIGN_PATH_NOSIGN_JSON =
      "Need more arguments: http://host:port/uri method user pwd sign=path|nosign [json]";

  private static WaarpLogger logger;

  /**
   * ExecutorService Worker Boss
   */
  private final EventLoopGroup workerGroup;

  private final Bootstrap bootstrap;

  private final HttpHeaders headers;

  private String baseUri = "/";

  /**
   * @param baseUri base of all URI, in general simply "/" (default if
   *     null)
   * @param nbclient max number of client connected at once
   * @param timeout timeout used in connection
   * @param Initializer the associated client pipeline factory
   */
  public HttpRestClientHelper(String baseUri, int nbclient, long timeout,
                              ChannelInitializer<SocketChannel> Initializer) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(HttpRestClientHelper.class);
    }
    if (baseUri != null) {
      this.baseUri = baseUri;
    }
    // Configure the client.
    bootstrap = new Bootstrap();
    workerGroup = new NioEventLoopGroup(nbclient, new WaarpThreadFactory(
        "Rest_" + baseUri + '_'));
    WaarpNettyUtil.setBootstrap(bootstrap, workerGroup, 30000);
    // Configure the pipeline factory.
    bootstrap.handler(Initializer);

    // will ignore real request
    final HttpRequest request =
        new DefaultHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, baseUri);
    headers = request.headers();
    headers.set(HttpHeaderNames.ACCEPT_ENCODING,
                HttpHeaderValues.GZIP + "," + HttpHeaderValues.DEFLATE);
    headers.set(HttpHeaderNames.ACCEPT_CHARSET, "utf-8;q=0.7,*;q=0.7");
    headers.set(HttpHeaderNames.ACCEPT_LANGUAGE, "fr,en");
    headers
        .set(HttpHeaderNames.USER_AGENT, "Netty Simple Http Rest Client side");
    headers.set(HttpHeaderNames.ACCEPT,
                "text/html,text/plain,application/xhtml+xml,application/xml,application/json;q=0.9,*/*;q=0.8");
    // connection will not close but needed
    /*
     * request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE)
     */
    // request.setHeader("Connection","keep-alive")
    // request.setHeader("Keep-Alive","300")
  }

  /**
   * Create one new connection to the remote host using port
   *
   * @param host
   * @param port
   *
   * @return the channel if connected or Null if not
   */
  public Channel getChannel(String host, int port) {
    // Start the connection attempt.
    final ChannelFuture future =
        bootstrap.connect(new InetSocketAddress(host, port));
    // Wait until the connection attempt succeeds or fails.
    final Channel channel = WaarpSslUtility.waitforChannelReady(future);
    if (channel != null) {
      final RestFuture futureChannel = new RestFuture(true);
      channel.attr(HttpRestClientSimpleResponseHandler.RESTARGUMENT)
             .set(futureChannel);
    }
    return channel;
  }

  /**
   * Send an HTTP query using the channel for target, using signature
   *
   * @param hmacSha256 SHA-256 key to create the signature
   * @param channel target of the query
   * @param method HttpMethod to use
   * @param host target of the query (shall be the same as for the
   *     channel)
   * @param addedUri additional uri, added to baseUri (shall include
   *     also
   *     extra arguments) (might be null)
   * @param user user to use in authenticated Rest procedure (might be
   *     null)
   * @param pwd password to use in authenticated Rest procedure (might
   *     be
   *     null)
   * @param uriArgs arguments for Uri if any (might be null)
   * @param json json to send as body in the request (might be null);
   *     Useful
   *     in PUT, POST but should not
   *     in GET, DELETE, OPTIONS
   *
   * @return the RestFuture associated with this request
   */
  public RestFuture sendQuery(HmacSha256 hmacSha256, Channel channel,
                              HttpMethod method, String host, String addedUri,
                              String user, String pwd,
                              Map<String, String> uriArgs, String json) {
    // Prepare the HTTP request.
    logger.debug("Prepare request: " + method + ':' + addedUri + ':' + json);
    final RestFuture future =
        channel.attr(HttpRestClientSimpleResponseHandler.RESTARGUMENT).get();
    QueryStringEncoder encoder;
    if (addedUri != null) {
      encoder = new QueryStringEncoder(baseUri + addedUri);
    } else {
      encoder = new QueryStringEncoder(baseUri);
    }
    // add Form attribute
    if (uriArgs != null) {
      for (final Entry<String, String> elt : uriArgs.entrySet()) {
        encoder.addParam(elt.getKey(), elt.getValue());
      }
    }
    String[] result;
    try {
      result = RestArgument.getBaseAuthent(hmacSha256, encoder, user, pwd);
      logger.debug("Authent encoded");
    } catch (final HttpInvalidAuthenticationException e) {
      logger.error(e.getMessage(), e);
      future.setFailure(e);
      return future;
    }
    URI uri;
    try {
      uri = encoder.toUri();
    } catch (final URISyntaxException e) {
      logger.error(e.getMessage());
      future.setFailure(e);
      return future;
    }
    logger.debug("Uri ready: " + uri.toASCIIString());

    FullHttpRequest request;
    if (json != null) {
      logger.debug("Add body");
      final ByteBuf buffer =
          Unpooled.wrappedBuffer(json.getBytes(WaarpStringUtils.UTF8));
      request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method,
                                           uri.toASCIIString(), buffer);
      request.headers()
             .set(HttpHeaderNames.CONTENT_LENGTH, buffer.readableBytes());
    } else {
      request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method,
                                           uri.toASCIIString());
    }
    // it is legal to add directly header or cookie into the request until finalize
    request.headers().add(headers);
    request.headers().set(HttpHeaderNames.HOST, host);
    if (user != null) {
      request.headers().set(
          (CharSequence) RestArgument.REST_ROOT_FIELD.ARG_X_AUTH_USER.field,
          user);
    }
    request.headers().set(
        (CharSequence) RestArgument.REST_ROOT_FIELD.ARG_X_AUTH_TIMESTAMP.field,
        result[0]);
    request.headers().set(
        (CharSequence) RestArgument.REST_ROOT_FIELD.ARG_X_AUTH_KEY.field,
        result[1]);
    // send request
    logger.debug("Send request");
    channel.writeAndFlush(request);
    logger.debug("Request sent");
    return future;
  }

  /**
   * Send an HTTP query using the channel for target, but without any
   * Signature
   *
   * @param channel target of the query
   * @param method HttpMethod to use
   * @param host target of the query (shall be the same as for the
   *     channel)
   * @param addedUri additional uri, added to baseUri (shall include
   *     also
   *     extra arguments) (might be null)
   * @param user user to use in authenticated Rest procedure (might be
   *     null)
   * @param uriArgs arguments for Uri if any (might be null)
   * @param json json to send as body in the request (might be null);
   *     Useful
   *     in PUT, POST but should not in
   *     GET, DELETE, OPTIONS
   *
   * @return the RestFuture associated with this request
   */
  public RestFuture sendQuery(Channel channel, HttpMethod method, String host,
                              String addedUri, String user,
                              Map<String, String> uriArgs, String json) {
    // Prepare the HTTP request.
    logger.debug("Prepare request: " + method + ':' + addedUri + ':' + json);
    final RestFuture future =
        channel.attr(HttpRestClientSimpleResponseHandler.RESTARGUMENT).get();
    QueryStringEncoder encoder;
    if (addedUri != null) {
      encoder = new QueryStringEncoder(baseUri + addedUri);
    } else {
      encoder = new QueryStringEncoder(baseUri);
    }
    // add Form attribute
    if (uriArgs != null) {
      for (final Entry<String, String> elt : uriArgs.entrySet()) {
        encoder.addParam(elt.getKey(), elt.getValue());
      }
    }
    URI uri;
    try {
      uri = encoder.toUri();
    } catch (final URISyntaxException e) {
      logger.error(e.getMessage());
      future.setFailure(e);
      return future;
    }
    logger.debug("Uri ready: " + uri.toASCIIString());

    FullHttpRequest request;
    if (json != null) {
      logger.debug("Add body");
      final ByteBuf buffer =
          Unpooled.wrappedBuffer(json.getBytes(WaarpStringUtils.UTF8));
      request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method,
                                           uri.toASCIIString(), buffer);
      request.headers()
             .set(HttpHeaderNames.CONTENT_LENGTH, buffer.readableBytes());
    } else {
      request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method,
                                           uri.toASCIIString());
    }

    // it is legal to add directly header or cookie into the request until finalize
    request.headers().add(headers);
    request.headers().set(HttpHeaderNames.HOST, host);
    if (user != null) {
      request.headers().set(
          (CharSequence) RestArgument.REST_ROOT_FIELD.ARG_X_AUTH_USER.field,
          user);
    }
    request.headers().set(
        (CharSequence) RestArgument.REST_ROOT_FIELD.ARG_X_AUTH_TIMESTAMP.field,
        new DateTime().toString());
    // send request
    logger.debug("Send request");
    channel.writeAndFlush(request);
    logger.debug("Request sent");
    return future;
  }

  /**
   * Finalize the HttpRestClientHelper
   */
  public void closeAll() {
    bootstrap.config().group().shutdownGracefully();
  }

  /**
   * @param args as uri (http://host:port/uri method user pwd
   *     sign=path|nosign [json])
   */
  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
    final WaarpLogger logger =
        WaarpLoggerFactory.getLogger(HttpRestClientHelper.class);
    if (args.length < 5) {
      logger.error(
          NEED_MORE_ARGUMENTS_HTTP_HOST_PORT_URI_METHOD_USER_SIGN_PATH_NOSIGN_JSON);
      return;
    }
    final String uri = args[0];
    final String meth = args[1];
    final String user = args[2];
    final String pwd = args[3];
    final boolean sign = args[4].toLowerCase().contains("sign=");
    HmacSha256 hmacSha256 = null;
    if (sign) {
      final String file = args[4].replace("sign=", "");
      hmacSha256 = new HmacSha256();
      try {
        hmacSha256.setSecretKey(new File(file));
      } catch (final CryptoException e) {
        logger.error(
            NEED_MORE_ARGUMENTS_HTTP_HOST_PORT_URI_METHOD_USER_SIGN_PATH_NOSIGN_JSON);
        return;
      } catch (final IOException e) {
        logger.error(
            NEED_MORE_ARGUMENTS_HTTP_HOST_PORT_URI_METHOD_USER_SIGN_PATH_NOSIGN_JSON);
        return;
      }
    }
    String json = null;
    if (args.length > 5) {
      json = args[5].replace("'", "\"");
    }
    final HttpMethod method = HttpMethod.valueOf(meth);
    int port = -1;
    String host;
    String path;
    try {
      final URI realUri = new URI(uri);
      port = realUri.getPort();
      host = realUri.getHost();
      path = realUri.getPath();
    } catch (final URISyntaxException e) {
      logger.error("Error", e);
      return;
    }
    final HttpRestClientHelper client = new HttpRestClientHelper(path, 1, 30000,
                                                                 new HttpRestClientSimpleInitializer());
    final Channel channel = client.getChannel(host, port);
    if (channel == null) {
      client.closeAll();
      logger.error("Cannot connect to " + host + " on port " + port);
      return;
    }
    RestFuture future;
    if (sign) {
      future = client
          .sendQuery(hmacSha256, channel, method, host, null, user, pwd, null,
                     json);
    } else {
      future = client.sendQuery(channel, method, host, null, user, null, json);
    }
    future.awaitOrInterruptible();
    WaarpSslUtility.closingSslChannel(channel);
    if (future.isSuccess()) {
      logger.warn(future.getRestArgument().prettyPrint());
    } else {
      final RestArgument ra = future.getRestArgument();
      if (ra != null) {
        logger.error(ra.prettyPrint());
      } else {
        logger.error("Query in error", future.getCause());
      }
    }
    client.closeAll();
  }
}
