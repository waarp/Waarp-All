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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
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
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.EndOfDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.NotEnoughDataDecoderException;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.database.DbSession;
import org.waarp.common.database.data.AbstractDbData.UpdatedInfo;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.gateway.kernel.AbstractHttpBusinessRequest;
import org.waarp.gateway.kernel.AbstractHttpField;
import org.waarp.gateway.kernel.AbstractHttpField.FieldPosition;
import org.waarp.gateway.kernel.AbstractHttpField.FieldRole;
import org.waarp.gateway.kernel.HttpBusinessFactory;
import org.waarp.gateway.kernel.HttpPage;
import org.waarp.gateway.kernel.HttpPage.PageRole;
import org.waarp.gateway.kernel.HttpPageHandler;
import org.waarp.gateway.kernel.database.DbConstantGateway;
import org.waarp.gateway.kernel.database.WaarpActionLogger;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.session.DefaultHttpAuth;
import org.waarp.gateway.kernel.session.HttpSession;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public abstract class HttpRequestHandler
    extends SimpleChannelInboundHandler<HttpObject> {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(HttpRequestHandler.class);

  private static final SecureRandom random = new SecureRandom();

  protected final String baseStaticPath;
  protected final String cookieSession;
  protected final HttpPageHandler httpPageHandler;

  /**
   * @param baseStaticPath
   * @param cookieSession
   * @param httpPageHandler
   */
  protected HttpRequestHandler(final String baseStaticPath,
                               final String cookieSession,
                               final HttpPageHandler httpPageHandler) {
    this.baseStaticPath = baseStaticPath;
    this.cookieSession = cookieSession;
    this.httpPageHandler = httpPageHandler;
  }

  protected HttpSession session;
  protected HttpPostRequestDecoder decoder;
  protected HttpPage httpPage;
  protected AbstractHttpBusinessRequest businessRequest;

  protected HttpResponseStatus status = HttpResponseStatus.OK;
  protected String errorMesg;

  protected HttpRequest request;
  protected HttpMethod method;

  protected volatile boolean willClose;

  /**
   * Clean method
   * <p>
   * Override if needed
   */
  protected void clean() {
    if (businessRequest != null) {
      businessRequest.cleanRequest();
      businessRequest = null;
    }
    if (decoder != null) {
      decoder.cleanFiles();
      decoder = null;
    }
    if (session != null) {
      session.setFilename(null);
      session.setLogid(DbConstantGateway.ILLEGALVALUE);
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
    willClose = false;
    status = HttpResponseStatus.OK;
    httpPage = null;
    businessRequest = null;
  }

  /**
   * set values from URI
   *
   * @throws HttpIncorrectRequestException
   */
  protected void getUriArgs() throws HttpIncorrectRequestException {
    final QueryStringDecoder decoderQuery =
        new QueryStringDecoder(request.uri());
    final Map<String, List<String>> uriAttributes = decoderQuery.parameters();
    final Set<String> attributes = uriAttributes.keySet();
    for (final String name : attributes) {
      final List<String> values = uriAttributes.get(name);
      if (values != null) {
        if (values.size() == 1) {
          // only one element is allowed
          httpPage.setValue(businessRequest, name, values.get(0),
                            FieldPosition.URL);
        } else if (values.size() > 1) {
          // more than one element is not allowed
          values.clear();
          throw new HttpIncorrectRequestException(
              "Too many values for " + name);
        }
        values.clear();
      }
    }
  }

  /**
   * set values from Header
   *
   * @throws HttpIncorrectRequestException
   */
  protected void getHeaderArgs() throws HttpIncorrectRequestException {
    final Set<String> headerNames = request.headers().names();
    for (final String name : headerNames) {
      final List<String> values = request.headers().getAll((CharSequence) name);
      if (values != null) {
        if (values.size() == 1) {
          // only one element is allowed
          httpPage.setValue(businessRequest, name, values.get(0),
                            FieldPosition.HEADER);
        } else if (values.size() > 1) {
          // more than one element is not allowed
          try {
            values.clear();
          } catch (final UnsupportedOperationException e) {
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
          }
          throw new HttpIncorrectRequestException(
              "Too many values for " + name);
        }
        try {
          values.clear();
        } catch (final UnsupportedOperationException e) {
          SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        }
      }
    }
  }

  /**
   * set values from Cookies
   *
   * @throws HttpIncorrectRequestException
   */
  protected void getCookieArgs() throws HttpIncorrectRequestException {
    final Set<Cookie> cookies;
    final String value = request.headers().get(HttpHeaderNames.COOKIE);
    if (value == null) {
      cookies = Collections.emptySet();
    } else {
      cookies = ServerCookieDecoder.LAX.decode(value);
    }
    if (!cookies.isEmpty()) {
      for (final Cookie cookie : cookies) {
        if (isCookieValid(cookie)) {
          httpPage.setValue(businessRequest, cookie.name(), cookie.value(),
                            FieldPosition.COOKIE);
        }
      }
    }
    cookies.clear();
  }

  /**
   * To be used for instance to check correctness of connection
   *
   * @param ctx
   */
  protected abstract void checkConnection(ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException;

  /**
   * Called when an error is raised. Note that clean() will be called just
   * after.
   *
   * @param ctx
   */
  protected abstract void error(ChannelHandlerContext ctx);

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx,
                              final HttpObject msg) {
    try {
      if (msg instanceof HttpRequest) {
        initialize();
        request = (HttpRequest) msg;
        method = request.method();
        final QueryStringDecoder queryStringDecoder =
            new QueryStringDecoder(request.uri());
        final String uriRequest = queryStringDecoder.path();
        final HttpPage httpPageTemp;
        try {
          httpPageTemp =
              httpPageHandler.getHttpPage(uriRequest, method.name(), session);
        } catch (final HttpIncorrectRequestException e1) {
          // real error => 400
          status = HttpResponseStatus.BAD_REQUEST;
          errorMesg = e1.getMessage();
          writeErrorPage(ctx);
          return;
          // end of task
        }
        if (httpPageTemp == null) {
          // if Get => standard Get
          if (method == HttpMethod.GET) {
            logger.debug("simple get: {}", request.uri());
            // send content (image for instance)
            HttpWriteCacheEnable
                .writeFile(request, ctx, baseStaticPath + uriRequest,
                           cookieSession);
            // end of task
          } else {
            // real error => 404
            status = HttpResponseStatus.NOT_FOUND;
            writeErrorPage(ctx);
          }
          return;
        }
        httpPage = httpPageTemp;
        session.setCurrentCommand(httpPage.getPagerole());
        final DbSession dbSession = DbConstantGateway.admin != null?
            DbConstantGateway.admin.getSession() : null;
        WaarpActionLogger
            .logCreate(dbSession, "Request received: " + httpPage.getPagename(),
                       session);
        if (httpPageTemp.getPagerole() == PageRole.ERROR) {
          status = HttpResponseStatus.BAD_REQUEST;
          error(ctx);
          clean();
          // order is important: first clean, then create new businessRequest
          businessRequest = httpPage.newRequest(ctx.channel().remoteAddress());
          willClose = true;
          writeSimplePage(ctx);
          WaarpActionLogger
              .logErrorAction(DbConstantGateway.admin.getSession(), session,
                              "Error: " + httpPage.getPagename(), status);
          return;
          // end of task
        }
        businessRequest = httpPage.newRequest(ctx.channel().remoteAddress());
        getUriArgs();
        getHeaderArgs();
        getCookieArgs();
        checkConnection(ctx);
        switch (httpPage.getPagerole()) {
          case DELETE:
            // no body element
            delete(ctx);
            return;
          case GETDOWNLOAD:
            // no body element
            getFile(ctx);
            return;
          case HTML:
          case MENU:
            // no body element
            beforeSimplePage(ctx);
            writeSimplePage(ctx);
            return;
          case POST:
          case POSTUPLOAD:
          case PUT:
            post(ctx);
            return;
          default:
            // real error => 400
            status = HttpResponseStatus.BAD_REQUEST;
            writeErrorPage(ctx);
        }
      } else {
        // New chunk is received: only for Put, Post or PostMulti!
        postChunk(ctx, (HttpContent) msg);
      }
    } catch (final HttpIncorrectRequestException e1) {
      // real error => 400
      if (status == HttpResponseStatus.OK) {
        status = HttpResponseStatus.BAD_REQUEST;
      }
      errorMesg = e1.getMessage();
      logger.warn("Error", e1);
      writeErrorPage(ctx);
    }
  }

  /**
   * Utility to prepare error
   *
   * @param ctx
   * @param message
   *
   * @throws HttpIncorrectRequestException
   */
  protected void prepareError(final ChannelHandlerContext ctx,
                              final String message)
      throws HttpIncorrectRequestException {
    logger.debug("Debug {}", message);
    if (!setErrorPage(ctx)) {
      // really really bad !
      return;
    }
    errorMesg = status.reasonPhrase() + " / " + message;
    throw new HttpIncorrectRequestException(errorMesg);
  }

  /**
   * Instantiate the page and the businessRequest handler
   *
   * @param ctx
   *
   * @return True if initialized
   */
  protected boolean setErrorPage(final ChannelHandlerContext ctx) {
    httpPage = httpPageHandler.getHttpPage(status.code());
    if (httpPage == null) {
      return false;
    }
    businessRequest = httpPage.newRequest(ctx.channel().remoteAddress());
    return true;
  }

  /**
   * Write an error page
   *
   * @param ctx
   */
  protected void writeErrorPage(final ChannelHandlerContext ctx) {
    final DbSession dbSession =
        DbConstantGateway.admin != null? DbConstantGateway.admin.getSession() :
            null;
    WaarpActionLogger.logErrorAction(dbSession, session, "Error: " +
                                                         (httpPage == null?
                                                             "no page" :
                                                             httpPage
                                                                 .getPagename()),
                                     status);
    error(ctx);
    clean();
    willClose = true;
    if (!setErrorPage(ctx)) {
      // really really bad !
      forceClosing(ctx);
      return;
    }
    try {
      writeSimplePage(ctx);
    } catch (final HttpIncorrectRequestException e) {
      // force channel closing
      forceClosing(ctx);
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
      willClose = true;
      final String answer =
          "<html><body>Error " + status.reasonPhrase() + "</body></html>";
      final FullHttpResponse response = getResponse(
          Unpooled.wrappedBuffer(answer.getBytes(WaarpStringUtils.UTF8)));
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
      response.headers().set(HttpHeaderNames.REFERER, request.uri());
      final ChannelFuture future = ctx.writeAndFlush(response);
      logger.debug("Will close");
      future.addListener(WaarpSslUtility.SSLCLOSE);
    }
    WaarpActionLogger
        .logErrorAction(DbConstantGateway.admin.getSession(), session,
                        "Error: " + httpPage.getPagename(), status);
  }

  /**
   * Write a simple page from current httpPage and businessRequest
   *
   * @param ctx
   *
   * @throws HttpIncorrectRequestException
   */
  protected void writeSimplePage(final ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException {
    logger.debug("HttpPage: {} businessRequest: {}",
                 httpPage != null? httpPage.getPagename() : "no page",
                 businessRequest != null? businessRequest.getClass().getName() :
                     "no BR");
    if (httpPage != null && httpPage.getPagerole() == PageRole.ERROR) {
      try {
        httpPage
            .setValue(businessRequest, AbstractHttpField.ERRORINFO, errorMesg,
                      FieldPosition.BODY);
      } catch (final HttpIncorrectRequestException e) {
        // ignore
      }
    }
    final String answer =
        httpPage != null? httpPage.getHtmlPage(businessRequest) : "BAD REQUEST";
    final int length;
    // Convert the response content to a ByteBuf.
    final ByteBuf buf =
        Unpooled.wrappedBuffer(answer.getBytes(WaarpStringUtils.UTF8));
    final FullHttpResponse response = getResponse(buf);
    if (businessRequest == null) {
      response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
    } else {
      response.headers().set(HttpHeaderNames.CONTENT_TYPE,
                             businessRequest.getContentType());
    }
    response.headers().set(HttpHeaderNames.REFERER, request.uri());
    length = buf.readableBytes();
    if (!willClose) {
      // There's no need to add 'Content-Length' header
      // if this is the last response.
      response.headers()
              .set(HttpHeaderNames.CONTENT_LENGTH, String.valueOf(length));
    }
    // Write the response.
    final ChannelFuture future = ctx.writeAndFlush(response);
    // Close the connection after the write operation is done if necessary.
    if (willClose) {
      logger.debug("Will close");
      future.addListener(WaarpSslUtility.SSLCLOSE);
    }
  }

  /**
   * Could be used for other method (as validation of an authent cookie)
   *
   * @param cookie
   *
   * @return True if this cookie is valid
   */
  protected abstract boolean isCookieValid(Cookie cookie);

  /**
   * Method to add specific Cookies from business definition
   * <p>
   * Override if needed
   *
   * @param response
   * @param cookieNames
   */
  protected void addBusinessCookie(final FullHttpResponse response,
                                   final Set<String> cookieNames) {
    if (httpPage != null) {
      for (final AbstractHttpField field : httpPage
          .getFieldsForRequest(businessRequest).values()) {
        if (field.isFieldcookieset() &&
            !cookieNames.contains(field.getFieldname())) {
          response.headers().add(HttpHeaderNames.SET_COOKIE,
                                 ServerCookieEncoder.LAX
                                     .encode(field.getFieldname(),
                                             field.fieldvalue));
        }
      }
    }
  }

  /**
   * Method to set Cookies in response
   *
   * @param response
   */
  protected void setCookieEncoder(final FullHttpResponse response) {
    final Set<Cookie> cookies;
    final String value = request.headers().get(HttpHeaderNames.COOKIE);
    if (value == null) {
      cookies = Collections.emptySet();
    } else {
      cookies = ServerCookieDecoder.LAX.decode(value);
    }
    boolean foundCookieSession = false;
    final Set<String> cookiesName = new HashSet<String>();
    if (!cookies.isEmpty()) {
      // Reset the cookies if necessary.
      for (final Cookie cookie : cookies) {
        if (isCookieValid(cookie)) {
          response.headers().add(HttpHeaderNames.SET_COOKIE,
                                 ServerCookieEncoder.LAX.encode(cookie));
          if (cookie.name().equals(cookieSession)) {
            foundCookieSession = true;
          }
          cookiesName.add(cookie.name());
        }
      }
    }
    if (!foundCookieSession) {
      response.headers().add(HttpHeaderNames.SET_COOKIE, ServerCookieEncoder.LAX
          .encode(cookieSession, session.getCookieSession()));
      cookiesName.add(cookieSession);
    }
    addBusinessCookie(response, cookiesName);
    cookiesName.clear();
  }

  /**
   * @param buf might be null
   *
   * @return the Http Response according to the status
   */
  protected FullHttpResponse getResponse(final ByteBuf buf) {
    // Decide whether to close the connection or not.
    final FullHttpResponse response;
    if (request == null) {
      if (buf != null) {
        response =
            new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, status, buf);
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH,
                               response.content().readableBytes());
      } else {
        response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_0, status);
      }
      setCookieEncoder(response);
      willClose = true;
      return response;
    }
    boolean keepAlive = HttpUtil.isKeepAlive(request);
    willClose |= status != HttpResponseStatus.OK || HttpHeaderValues.CLOSE
        .contentEqualsIgnoreCase(
            request.headers().get(HttpHeaderNames.CONNECTION)) ||
                 request.protocolVersion().equals(HttpVersion.HTTP_1_0) &&
                 !keepAlive;
    if (willClose) {
      keepAlive = false;
    }
    // Build the response object.
    if (buf != null) {
      response =
          new DefaultFullHttpResponse(request.protocolVersion(), status, buf);
      response.headers().add(HttpHeaderNames.CONTENT_LENGTH,
                             response.content().readableBytes());
    } else {
      response = new DefaultFullHttpResponse(request.protocolVersion(), status);
    }
    if (keepAlive) {
      response.headers()
              .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }
    setCookieEncoder(response);
    return response;
  }

  /**
   * @return the filename used for this request
   */
  protected abstract String getFilename();

  /**
   * Called before simple Page is called (Menu or HTML)
   *
   * @param ctx
   *
   * @throws HttpIncorrectRequestException
   */
  protected abstract void beforeSimplePage(ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException;

  /**
   * Method that will use the result and send back the result
   *
   * @param ctx
   *
   * @throws HttpIncorrectRequestException
   */
  protected void finalData(final ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException {
    try {
      businessValidRequestAfterAllDataReceived(ctx);
      if (httpPage == null) {
        // Cached
        return;
      }
      if (!httpPage.isRequestValid(businessRequest)) {
        throw new HttpIncorrectRequestException("Request unvalid");
      }
      final DbSession dbSession = DbConstantGateway.admin != null?
          DbConstantGateway.admin.getSession() : null;
      switch (httpPage.getPagerole()) {
        case DELETE:
          session.setFilename(getFilename());
          finalDelete(ctx);
          WaarpActionLogger.logAction(dbSession, session, "Delete OK", status,
                                      UpdatedInfo.DONE);
          break;
        case GETDOWNLOAD:
          finalGet(ctx);
          WaarpActionLogger.logAction(dbSession, session, "Download OK", status,
                                      UpdatedInfo.DONE);
          break;
        case POST:
          finalPost(ctx);
          WaarpActionLogger.logAction(dbSession, session, "Post OK", status,
                                      UpdatedInfo.DONE);
          break;
        case POSTUPLOAD:
          finalPostUpload(ctx);
          WaarpActionLogger
              .logAction(dbSession, session, "PostUpload OK", status,
                         UpdatedInfo.DONE);
          break;
        case PUT:
          finalPut(ctx);
          WaarpActionLogger.logAction(dbSession, session, "Put OK", status,
                                      UpdatedInfo.DONE);
          break;
        default:
          // real error => 400
          status = HttpResponseStatus.BAD_REQUEST;
          throw new HttpIncorrectRequestException("Unknown request");
      }
    } catch (final HttpIncorrectRequestException e) {
      // real error => 400
      if (status == HttpResponseStatus.OK) {
        status = HttpResponseStatus.BAD_REQUEST;
      }
      throw e;
    }
  }

  /**
   * Method that will use the uploaded file and prepare the result
   *
   * @param ctx
   */
  protected abstract void finalDelete(ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException;

  /**
   * Method that will use the uploaded file and send back the result <br>
   * (this method must send back the answer using for instance a ChunkedInput
   * handler and should try to call
   * clean(), but taking into consideration that it will erase all data, so it
   * must be ensured that all data are
   * sent through the wire before calling it. Note however that when the
   * connection is closed or when a new
   * request on the same connection occurs, the clean method is automatically
   * called. The usage of a
   * HttpCleanChannelFutureListener on the last write might be useful.)
   *
   * @param ctx
   */
  protected abstract void finalGet(ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException;

  /**
   * Method that will use the uploaded file and prepare the result
   *
   * @param ctx
   */
  protected abstract void finalPostUpload(ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException;

  /**
   * Method that will use the post result and prepare the result
   *
   * @param ctx
   */
  protected abstract void finalPost(ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException;

  /**
   * Method that will use the put result and prepare the result
   *
   * @param ctx
   */
  protected abstract void finalPut(ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException;

  /**
   * Validate all data as they should be all received (done before the
   * isRequestValid)
   *
   * @param ctx
   *
   * @throws HttpIncorrectRequestException
   */
  public abstract void businessValidRequestAfterAllDataReceived(
      ChannelHandlerContext ctx) throws HttpIncorrectRequestException;

  /**
   * Method that get "get" data, answer has to be written in the business part
   * finalGet
   *
   * @param ctx
   */
  protected void getFile(final ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException {
    finalData(ctx);
  }

  /**
   * Method that get delete data
   *
   * @param ctx
   */
  protected void delete(final ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException {
    finalData(ctx);
    writeSimplePage(ctx);
    clean();
  }

  /**
   * Method that get post data
   *
   * @param ctx
   *
   * @throws HttpIncorrectRequestException
   */
  protected void post(final ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException {
    try {
      decoder =
          new HttpPostRequestDecoder(HttpBusinessFactory.factory, request);
    } catch (final ErrorDataDecoderException e1) {
      status = HttpResponseStatus.NOT_ACCEPTABLE;
      throw new HttpIncorrectRequestException(e1);
    } catch (final Exception e1) {
      // GETDOWNLOAD Method: should not try to create a HttpPostRequestDecoder
      // So OK but stop here
      status = HttpResponseStatus.NOT_ACCEPTABLE;
      throw new HttpIncorrectRequestException(e1);
    }

    if (request instanceof FullHttpRequest) {
      // Not chunk version
      readHttpDataAllReceive(ctx);
      finalData(ctx);
      writeSimplePage(ctx);
      clean();
    }
  }

  /**
   * Method that get a chunk of data
   *
   * @param ctx
   * @param chunk
   *
   * @throws HttpIncorrectRequestException
   */
  protected void postChunk(final ChannelHandlerContext ctx,
                           final HttpContent chunk)
      throws HttpIncorrectRequestException {
    // New chunk is received: only for Post!
    if (decoder == null) {
      finalData(ctx);
      writeSimplePage(ctx);
      clean();
      return;
    }
    try {
      decoder.offer(chunk);
    } catch (final ErrorDataDecoderException e1) {
      status = HttpResponseStatus.NOT_ACCEPTABLE;
      throw new HttpIncorrectRequestException(e1);
    }
    // example of reading chunk by chunk (minimize memory usage due to
    // Factory)
    readHttpDataChunkByChunk(ctx);
    // example of reading only if at the end
    if (chunk instanceof LastHttpContent) {
      finalData(ctx);
      writeSimplePage(ctx);
      clean();
    }
  }

  @Override
  public void exceptionCaught(final ChannelHandlerContext ctx,
                              final Throwable cause) {
    if (ctx.channel().isActive()) {
      if (cause != null && cause.getMessage() != null) {
        logger.warn("Exception {}", cause.getMessage(), cause);
      } else {
        logger.warn("Exception Received", cause);
      }
      if (cause instanceof ClosedChannelException) {
        return;
      }
      status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
      writeErrorPage(ctx);
    }
  }

  @Override
  public void channelInactive(final ChannelHandlerContext ctx)
      throws Exception {
    super.channelInactive(ctx);
    clean();
  }

  /**
   * Read all InterfaceHttpData from finished transfer
   *
   * @param ctx
   *
   * @throws HttpIncorrectRequestException
   */
  protected void readHttpDataAllReceive(final ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException {
    final List<InterfaceHttpData> datas;
    try {
      datas = decoder.getBodyHttpDatas();
    } catch (final NotEnoughDataDecoderException e1) {
      // Should not be!
      logger.warn("decoder issue", e1);
      status = HttpResponseStatus.NOT_ACCEPTABLE;
      throw new HttpIncorrectRequestException(e1);
    }
    for (final InterfaceHttpData data : datas) {
      readHttpData(data, ctx);
    }
  }

  /**
   * Read request by chunk and getting values from chunk to chunk
   *
   * @param ctx
   *
   * @throws HttpIncorrectRequestException
   */
  protected void readHttpDataChunkByChunk(final ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException {
    try {
      while (decoder.hasNext()) {
        final InterfaceHttpData data = decoder.next();
        if (data != null) {
          // new value
          readHttpData(data, ctx);
        }
      }
    } catch (final EndOfDataDecoderException e1) {
      // end
    }
  }

  /**
   * Read one Data
   *
   * @param data
   * @param ctx
   *
   * @throws HttpIncorrectRequestException
   */
  protected void readHttpData(final InterfaceHttpData data,
                              final ChannelHandlerContext ctx)
      throws HttpIncorrectRequestException {
    if (data.getHttpDataType() == HttpDataType.Attribute) {
      final Attribute attribute = (Attribute) data;
      final String name = attribute.getName();
      try {
        final String value = attribute.getValue();
        httpPage.setValue(businessRequest, name, value, FieldPosition.BODY);
      } catch (final IOException e) {
        // Error while reading data from File, only print name and
        // error
        attribute.delete();
        status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        throw new HttpIncorrectRequestException(e);
      }
      attribute.delete();
    } else if (data.getHttpDataType() == HttpDataType.FileUpload) {
      final FileUpload fileUpload = (FileUpload) data;
      if (fileUpload.isCompleted()) {
        final AbstractHttpField field =
            httpPage.getField(businessRequest, fileUpload.getName());
        if (field != null &&
            field.getFieldtype() == FieldRole.BUSINESS_INPUT_FILE) {
          httpPage.setValue(businessRequest, field.getFieldname(), fileUpload);
        } else {
          logger.warn("File received but no variable for it");
          fileUpload.delete();
        }
      } else {
        logger.warn("File still pending but should not");
        fileUpload.delete();
      }
    } else {
      logger.warn("Unknown element: " + data);
    }
  }

  /**
   * Default Session Cookie generator
   *
   * @return the new session cookie value
   */
  protected String getNewCookieSession() {
    return "Waarp" + Long.toHexString(random.nextLong());
  }

  /**
   * Default session creation
   *
   * @param ctx
   */
  protected void createNewSessionAtConnection(final ChannelHandlerContext ctx) {
    session = new HttpSession();
    session.setHttpAuth(new DefaultHttpAuth(session));
    session.setCookieSession(getNewCookieSession());
    session.setCurrentCommand(PageRole.HTML);
  }

  @Override
  public void channelActive(final ChannelHandlerContext ctx) throws Exception {
    super.channelActive(ctx);
    createNewSessionAtConnection(ctx);
  }

}
