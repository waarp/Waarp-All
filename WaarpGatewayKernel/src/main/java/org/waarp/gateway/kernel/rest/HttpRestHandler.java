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
package org.waarp.gateway.kernel.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.DiskAttribute;
import io.netty.handler.codec.http.multipart.DiskFileUpload;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.NotEnoughDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.database.DbSession;
import org.waarp.common.exception.CryptoException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.gateway.kernel.database.DbConstantGateway;
import org.waarp.gateway.kernel.exception.HttpForbiddenRequestException;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.exception.HttpMethodNotAllowedRequestException;
import org.waarp.gateway.kernel.exception.HttpNotFoundRequestException;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * Handler for HTTP Rest support
 */
public abstract class HttpRestHandler
    extends SimpleChannelInboundHandler<HttpObject> {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpRestHandler.class);

  /*
   * Note: Presence de BODY dans toutes les requetes/responses = Content-Length ou Transfer-Encoding HEAD:
   * response pas de BODY
   *
   */

  public enum METHOD {
    /**
     * REST: Standard GET item
     * <p>
     * The GET method means retrieve whatever information (in the form of
     * an
     * entity) is identified by the
     * Request-URI. If the Request-URI refers to a data-producing process,
     * it is
     * the produced data which shall be
     * returned as the entity in the response and not the source text of
     * the
     * process, unless that text happens to
     * be the output of the process.
     */
    GET(HttpMethod.GET),
    /**
     * REST: Update existing item
     * <p>
     * The PUT method requests that the enclosed entity be stored under the
     * supplied Request-URI.
     */
    PUT(HttpMethod.PUT),
    /**
     * REST: Create a new item
     * <p>
     * The POST method is used to request that the origin server accept the
     * entity enclosed in the request as a
     * new subordinate of the resource identified by the Request-URI in the
     * Request-Line.
     */
    POST(HttpMethod.POST),
    /**
     * REST: Delete existing item
     * <p>
     * The DELETE method requests that the origin server delete the resource
     * identified by the Request-URI.
     */
    DELETE(HttpMethod.DELETE),
    /**
     * REST: what options are supported for the URI
     * <p>
     * The OPTIONS method represents a request for information about the
     * communication options available on the
     * request/response chain identified by the Request-URI. This method
     * allows
     * the client to determine the
     * options and/or requirements associated with a resource, or the
     * capabilities of a server, without implying a
     * resource action or initiating a resource retrieval.
     */
    OPTIONS(HttpMethod.OPTIONS),
    /**
     * REST: as GET but no BODY (existence ? metadata ?)
     * <p>
     * The HEAD method is identical to GET except that the server MUST NOT
     * return a message-body in the response.
     */
    HEAD(HttpMethod.HEAD),
    /**
     * REST: should not be used, use POST instead
     * <p>
     * The PATCH method requests that a set of changes described in the
     * request
     * entity be applied to the resource
     * identified by the Request-URI.
     */
    PATCH(HttpMethod.PATCH),
    /**
     * REST: unknown usage
     * <p>
     * The TRACE method is used to invoke a remote, application-layer
     * loop-back
     * of the request message.
     */
    TRACE(HttpMethod.TRACE),
    /**
     * REST: unknown
     * <p>
     * This specification reserves the method name CONNECT for use with a
     * proxy
     * that can dynamically switch to
     * being a tunnel
     */
    CONNECT(HttpMethod.CONNECT);

    public final HttpMethod method;

    METHOD(final HttpMethod method) {
      this.method = method;
    }
  }

  public static final HttpDataFactory factory =
      new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);
  // Disk if size exceed MINSIZE = 16K
  // XXX FIXME TODO to setup outside !
  public static String TempPath = "J:/GG/ARK/TMP";

  public static ChannelGroup group;

  /**
   * Initialize the Disk support
   *
   * @param tempPath system temp directory
   *
   * @throws IOException
   * @throws CryptoException
   */
  public static void initialize(final String tempPath) {
    TempPath = tempPath;
    final File file = new File(tempPath);
    file.mkdirs();//NOSONAR
    DiskFileUpload.deleteOnExitTemporaryFile = true; // should delete file
    // on exit (in normal
    // exit)
    DiskFileUpload.baseDirectory = TempPath; // system temp
    // directory
    DiskAttribute.deleteOnExitTemporaryFile = true; // should delete file on
    // exit (in normal exit)
    DiskAttribute.baseDirectory = TempPath; // system temp directory
  }

  public static final RestConfiguration defaultConfiguration =
      new RestConfiguration();

  public HashMap<String, RestMethodHandler> restHashMap;
  public final RestConfiguration restConfiguration;
  protected final RootOptionsRestMethodHandler rootHandler;

  protected HttpPostRequestDecoder decoder;
  protected HttpResponseStatus status = HttpResponseStatus.OK;

  protected HttpRequest request;
  protected RestMethodHandler handler;

  protected DbSession dbSession;

  private boolean willClose;

  /**
   * Arguments received
   */
  protected RestArgument arguments;
  /**
   * The only structure that might be needed is: ARGS_COOKIE (subset)
   */
  protected RestArgument response;
  /**
   * JSON decoded object
   */
  protected Object jsonObject;
  /**
   * Cumulative chunks
   */
  protected ByteBuf cumulativeBody;

  protected HttpRestHandler(final RestConfiguration config) {
    restConfiguration = config;
    rootHandler = new RootOptionsRestMethodHandler(config);
  }

  protected static class HttpCleanChannelFutureListener
      implements ChannelFutureListener {
    protected final HttpRestHandler handler;

    /**
     * @param handler
     */
    public HttpCleanChannelFutureListener(final HttpRestHandler handler) {
      this.handler = handler;
    }

    @Override
    public void operationComplete(final ChannelFuture future) {
      handler.clean();
    }
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    if (group != null) {
      group.add(ctx.channel());
    }
    super.channelActive(ctx);
  }

  /**
   * Clean method
   * <p>
   * Override if needed
   */
  protected void clean() {
    if (arguments != null) {
      arguments.clean();
      arguments = null;
    }
    if (response != null) {
      response.clean();
      response = null;
    }
    if (decoder != null) {
      decoder.cleanFiles();
      decoder = null;
    }
    handler = null;
    cumulativeBody = null;
    jsonObject = null;
    if (dbSession != null) {
      dbSession.enUseConnectionNoDisconnect();
      dbSession = null;
    }
  }

  /**
   * Called at the beginning of every new request
   * <p>
   * Override if needed
   */
  protected void initialize() {
    // clean previous FileUpload if Any
    clean();
    status = HttpResponseStatus.OK;
    request = null;
    setWillClose(false);
    arguments = new RestArgument(JsonHandler.createObjectNode());
    response = new RestArgument(JsonHandler.createObjectNode());
  }

  /**
   * @return the DbSession associated with the current request (might be Admin
   *     dbSession if none)
   */
  public DbSession getDbSession() {
    return dbSession == null? DbConstantGateway.admin.getSession() : dbSession;
  }

  /**
   * To be used for instance to check correctness of connection<br>
   * Note that ARG_METHOD is only set from current request. It might be also
   * set
   * from URI or HEADER and
   * therefore should be done in this method.
   *
   * @param ctx
   *
   * @throws HttpInvalidAuthenticationException
   */
  protected abstract void checkConnection(ChannelHandlerContext ctx)
      throws HttpInvalidAuthenticationException;

  /**
   * Method to set Cookies in httpResponse from response ObjectNode
   *
   * @param httpResponse
   */
  protected void setCookies(final FullHttpResponse httpResponse) {
    if (response == null) {
      return;
    }
    final ObjectNode cookieON = response.getCookieArgs();
    if (!cookieON.isMissingNode()) {
      final Iterator<Entry<String, JsonNode>> iter = cookieON.fields();
      while (iter.hasNext()) {
        final Entry<String, JsonNode> entry = iter.next();
        httpResponse.headers().add(HttpHeaderNames.SET_COOKIE,
                                   ServerCookieEncoder.LAX
                                       .encode(entry.getKey(),
                                               entry.getValue().asText()));
      }
    }
  }

  /**
   * Could be overwritten if necessary
   *
   * @return RestMethodHandler associated with the current context
   *
   * @throws HttpIncorrectRequestException
   * @throws HttpMethodNotAllowedRequestException
   * @throws HttpForbiddenRequestException
   */
  protected RestMethodHandler getHandler()
      throws HttpMethodNotAllowedRequestException,
             HttpForbiddenRequestException {
    final METHOD method = arguments.getMethod();
    final String uri = arguments.getBaseUri();
    boolean restFound = false;
    RestMethodHandler handlerNew = restHashMap.get(uri);
    if (handlerNew != null) {
      handlerNew.checkHandlerSessionCorrectness(this, arguments, response);
      if (handlerNew.isMethodIncluded(method)) {
        restFound = true;
      }
    }
    if (handlerNew == null && method == METHOD.OPTIONS) {
      handlerNew = rootHandler;
      // use Options default handler
      restFound = true;
    }
    logger.debug("{} {} {}", method, uri, restFound);
    if (!restFound) {
      throw new HttpMethodNotAllowedRequestException(
          "No Method found for that URI: " + uri);
    }
    return handlerNew;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx,
                              final HttpObject msg) {
    logger.debug("Msg Received");
    try {
      if (msg instanceof HttpRequest) {
        initialize();
        request = (HttpRequest) msg;
        arguments.setRequest(request);
        final Iterator<Entry<CharSequence, CharSequence>> iterator =
            request.headers().iteratorCharSequence();
        arguments.setHeaderArgs(iterator);
        arguments.setCookieArgs(request.headers().get(HttpHeaderNames.COOKIE));
        logger.debug("DEBUG: {}", arguments);
        checkConnection(ctx);
        handler = getHandler();
        if (arguments.getMethod() == METHOD.OPTIONS) {
          response.setFromArgument(arguments);
          handler.optionsCommand(this, arguments, response);
          finalizeSend(ctx);
          return;
        }
        if (request instanceof FullHttpRequest) {
          if (handler.isBodyJsonDecoded()) {
            final ByteBuf buffer = ((FullHttpRequest) request).content();
            jsonObject = getBodyJsonArgs(buffer);
          } else {
            // decoder for 1 chunk
            createDecoder();
            // Not chunk version
            readAllHttpData();
          }
          response.setFromArgument(arguments);
          handler.endParsingRequest(this, arguments, response, jsonObject);
          finalizeSend(ctx);
          return;
        }
        // no body yet
        if (!handler.isBodyJsonDecoded()) {
          createDecoder();
        }
      } else {
        // New chunk is received
        if (handler != null) {
          bodyChunk(ctx, (HttpContent) msg);
        }
      }
    } catch (final HttpIncorrectRequestException e1) {
      // real error => 400
      if (handler != null) {
        status =
            handler.handleException(this, arguments, response, jsonObject, e1);
      }
      if (status == HttpResponseStatus.OK) {
        status = HttpResponseStatus.BAD_REQUEST;
      }
      logger.warn("Error: {}", e1.getMessage(), e1);
      if (response.getDetail().isEmpty()) {
        response.setDetail(e1.getMessage());
      }
      if (handler != null) {
        finalizeSend(ctx);
      } else {
        forceClosing(ctx);
      }
    } catch (final HttpMethodNotAllowedRequestException e1) {
      if (handler != null) {
        status =
            handler.handleException(this, arguments, response, jsonObject, e1);
      }
      if (status == HttpResponseStatus.OK) {
        status = HttpResponseStatus.METHOD_NOT_ALLOWED;
      }
      logger.warn("Error: {}", e1.getMessage());
      if (response.getDetail().isEmpty()) {
        response.setDetail(e1.getMessage());
      }
      if (handler != null) {
        finalizeSend(ctx);
      } else {
        forceClosing(ctx);
      }
    } catch (final HttpForbiddenRequestException e1) {
      if (handler != null) {
        status =
            handler.handleException(this, arguments, response, jsonObject, e1);
      }
      if (status == HttpResponseStatus.OK) {
        status = HttpResponseStatus.FORBIDDEN;
      }
      logger.warn("Error: {}", e1.getMessage());
      if (response.getDetail().isEmpty()) {
        response.setDetail(e1.getMessage());
      }
      if (handler != null) {
        finalizeSend(ctx);
      } else {
        forceClosing(ctx);
      }
    } catch (final HttpInvalidAuthenticationException e1) {
      if (handler != null) {
        status =
            handler.handleException(this, arguments, response, jsonObject, e1);
      }
      if (status == HttpResponseStatus.OK) {
        status = HttpResponseStatus.UNAUTHORIZED;
      }
      logger.warn("Error: {}", e1.getMessage());
      if (response.getDetail().isEmpty()) {
        response.setDetail(e1.getMessage());
      }
      if (handler != null) {
        finalizeSend(ctx);
      } else {
        forceClosing(ctx);
      }
    } catch (final HttpNotFoundRequestException e1) {
      if (handler != null) {
        status =
            handler.handleException(this, arguments, response, jsonObject, e1);
      }
      if (status == HttpResponseStatus.OK) {
        status = HttpResponseStatus.NOT_FOUND;
      }
      logger.warn("Error: {}", e1.getMessage());
      if (response.getDetail().isEmpty()) {
        response.setDetail(e1.getMessage());
      }
      if (handler != null) {
        finalizeSend(ctx);
      } else {
        forceClosing(ctx);
      }
    }
  }

  /**
   * Create the decoder
   *
   * @throws HttpIncorrectRequestException
   */
  protected void createDecoder() throws HttpIncorrectRequestException {
    final HttpMethod method = request.method();
    if (!method.equals(HttpMethod.HEAD)) {
      // in order decoder allows to parse
      request.setMethod(HttpMethod.POST);
    }
    try {
      decoder = new HttpPostRequestDecoder(factory, request);
    } catch (final ErrorDataDecoderException e1) {
      status = HttpResponseStatus.NOT_ACCEPTABLE;
      throw new HttpIncorrectRequestException(e1);
    } catch (final Exception e1) {
      // GETDOWNLOAD Method: should not try to create a HttpPostRequestDecoder
      // So OK but stop here
      status = HttpResponseStatus.NOT_ACCEPTABLE;
      throw new HttpIncorrectRequestException(e1);
    }
  }

  /**
   * Read all InterfaceHttpData from finished transfer
   *
   * @throws HttpIncorrectRequestException
   */
  protected void readAllHttpData() throws HttpIncorrectRequestException {
    final List<InterfaceHttpData> datas;
    try {
      datas = decoder.getBodyHttpDatas();
    } catch (final NotEnoughDataDecoderException e1) {
      // Should not be!
      logger.warn("decoder issue: {}", e1.getMessage());
      status = HttpResponseStatus.NOT_ACCEPTABLE;
      throw new HttpIncorrectRequestException(e1);
    }
    for (final InterfaceHttpData data : datas) {
      readHttpData(data);
    }
  }

  /**
   * Read one Data
   *
   * @param data
   *
   * @throws HttpIncorrectRequestException
   */
  protected void readHttpData(final InterfaceHttpData data)
      throws HttpIncorrectRequestException {
    if (data.getHttpDataType() == HttpDataType.Attribute) {
      final ObjectNode body = arguments.getBody();
      try {
        body.put(data.getName(), ((Attribute) data).getValue());
      } catch (final IOException e) {
        throw new HttpIncorrectRequestException("Bad reading", e);
      }
    } else if (data.getHttpDataType() == HttpDataType.FileUpload) {
      final FileUpload fileUpload = (FileUpload) data;
      if (fileUpload.isCompleted()) {
        handler.getFileUpload(this, fileUpload, arguments, response);
      } else {
        logger.warn("File still pending but should not");
        fileUpload.delete();
        status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        throw new HttpIncorrectRequestException(
            "File still pending but should not");
      }
    } else {
      logger.warn("Unknown element: " + data);
    }
  }

  /**
   * To allow quick answer even if in very bad shape
   *
   * @param ctx
   */
  protected void forceClosing(final ChannelHandlerContext ctx) {
    if (status == HttpResponseStatus.OK) {
      status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
    }
    if (ctx.channel().isActive()) {
      setWillClose(true);
      final String answer =
          "<html><body>Error " + status.reasonPhrase() + "</body></html>";
      final FullHttpResponse httpResponse = getResponse(
          Unpooled.wrappedBuffer(answer.getBytes(WaarpStringUtils.UTF8)));
      httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
      httpResponse.headers().set(HttpHeaderNames.REFERER, request.uri());
      final ChannelFuture future = ctx.writeAndFlush(httpResponse);
      logger.debug("Will close");
      future.addListener(WaarpSslUtility.SSLCLOSE);
    }
    clean();
  }

  /**
   * @param content
   *
   * @return the Http Response according to the status and the content if not
   *     null (setting the CONTENT_LENGTH)
   */
  public FullHttpResponse getResponse(final ByteBuf content) {
    // Decide whether to close the connection or not.
    if (request == null) {
      final FullHttpResponse httpResponse;
      if (content == null) {
        httpResponse =
            new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, status);
      } else {
        httpResponse =
            new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, status, content);
        httpResponse.headers().set(HttpHeaderNames.CONTENT_LENGTH,
                                   content.array().length);
      }
      setCookies(httpResponse);
      setWillClose(true);
      return httpResponse;
    }
    boolean keepAlive = HttpUtil.isKeepAlive(request);
    setWillClose(isWillClose() || status != HttpResponseStatus.OK ||
                 HttpHeaderValues.CLOSE.contentEqualsIgnoreCase(
                     request.headers().get(HttpHeaderNames.CONNECTION)) ||
                 request.protocolVersion().equals(HttpVersion.HTTP_1_0) &&
                 !keepAlive);
    if (isWillClose()) {
      keepAlive = false;
    }
    // Build the response object.
    final FullHttpResponse httpResponse;
    if (content != null) {
      httpResponse =
          new DefaultFullHttpResponse(request.protocolVersion(), status,
                                      content);
      httpResponse.headers()
                  .set(HttpHeaderNames.CONTENT_LENGTH, content.array().length);
    } else {
      httpResponse =
          new DefaultFullHttpResponse(request.protocolVersion(), status);
    }
    if (keepAlive) {
      httpResponse.headers()
                  .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }
    setCookies(httpResponse);
    return httpResponse;
  }

  /**
   * Method that get a chunk of data
   *
   * @param ctx
   * @param chunk
   *
   * @throws HttpIncorrectRequestException
   * @throws HttpInvalidAuthenticationException
   * @throws HttpNotFoundRequestException
   */
  protected void bodyChunk(final ChannelHandlerContext ctx,
                           final HttpContent chunk)
      throws HttpIncorrectRequestException, HttpInvalidAuthenticationException,
             HttpNotFoundRequestException {
    // New chunk is received: only for Post!
    if (handler.isBodyJsonDecoded()) {
      final ByteBuf buffer = chunk.content();
      if (cumulativeBody != null) {
        if (buffer.isReadable()) {
          cumulativeBody = Unpooled.wrappedBuffer(cumulativeBody, buffer);
        }
      } else {
        cumulativeBody = buffer.slice();
      }
    } else {
      try {
        decoder.offer(chunk);
      } catch (final ErrorDataDecoderException e1) {
        status = HttpResponseStatus.NOT_ACCEPTABLE;
        throw new HttpIncorrectRequestException(e1);
      }
      // example of reading chunk by chunk (minimize memory usage due to
      // Factory)
      readHttpDataChunkByChunk();
    }
    // example of reading only if at the end
    if (chunk instanceof LastHttpContent) {
      if (handler.isBodyJsonDecoded()) {
        jsonObject = getBodyJsonArgs(cumulativeBody);
        cumulativeBody.release();
        cumulativeBody = null;
      }
      response.setFromArgument(arguments);
      handler.endParsingRequest(this, arguments, response, jsonObject);
      finalizeSend(ctx);
    }
  }

  protected void finalizeSend(final ChannelHandlerContext ctx) {
    final ChannelFuture future;
    if (arguments.getMethod() == METHOD.OPTIONS) {
      future = handler.sendOptionsResponse(this, ctx, response, status);
    } else {
      future = handler
          .sendResponse(this, ctx, arguments, response, jsonObject, status);
    }
    if (future != null) {
      future.addListener(WaarpSslUtility.SSLCLOSE);
    }
    clean();
    logger.debug("Cleaned");
  }

  /**
   * Get Body args as JSON body
   *
   * @param data
   *
   * @throws HttpIncorrectRequestException
   */
  protected Object getBodyJsonArgs(final ByteBuf data)
      throws HttpIncorrectRequestException {
    if (data == null || data.readableBytes() == 0) {
      return null;
    }
    return handler.getBody(this, data, arguments, response);
  }

  /**
   * Read request by chunk and getting values from chunk to chunk
   *
   * @throws HttpIncorrectRequestException
   */
  protected void readHttpDataChunkByChunk()
      throws HttpIncorrectRequestException {
    try {
      while (decoder.hasNext()) {
        final InterfaceHttpData data = decoder.next();
        if (data != null) {
          // new value
          readHttpData(data);
        }
      }
    } catch (final EndOfDataDecoderException e1) {
      // end
    }
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx,
                              final Throwable cause) {
    if (ctx.channel().isActive()) {
      if (cause != null && cause.getMessage() != null) {
        logger.warn("Exception {}", cause.getMessage());
      } else {
        logger.warn("Exception Received", cause);
      }
      if (cause instanceof ClosedChannelException ||
          cause instanceof IOException) {
        return;
      }
      if (handler != null) {
        status = handler.handleException(this, arguments, response, jsonObject,
                                         (Exception) cause);
      }
      if (status == HttpResponseStatus.OK) {
        status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
      }
      if (handler != null) {
        finalizeSend(ctx);
      } else {
        forceClosing(ctx);
      }
    }
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx)
      throws Exception {
    super.channelInactive(ctx);
    clean();
  }

  /**
   * @return the status
   */
  public HttpResponseStatus getStatus() {
    return status;
  }

  /**
   * @param status the status to set
   */
  public void setStatus(final HttpResponseStatus status) {
    this.status = status;
  }

  /**
   * @return the request
   */
  public HttpRequest getRequest() {
    return request;
  }

  /**
   * @return the willClose
   */
  public boolean isWillClose() {
    return willClose;
  }

  /**
   * @param willClose the willClose to set
   */
  public void setWillClose(final boolean willClose) {
    this.willClose = willClose;
  }
}
