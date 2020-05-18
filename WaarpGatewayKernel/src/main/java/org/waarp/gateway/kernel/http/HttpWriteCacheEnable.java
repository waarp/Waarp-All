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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import org.waarp.common.logging.SysErrLogger;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import static io.netty.handler.codec.http.HttpVersion.*;

/**
 * Utility class to write external file with cache enable properties
 */
public final class HttpWriteCacheEnable {
  /**
   * US locale - all HTTP dates are in english
   */
  public static final Locale LOCALE_US = Locale.US;

  /**
   * GMT timezone - all HTTP dates are on GMT
   */
  public static final TimeZone GMT_ZONE = TimeZone.getTimeZone("GMT");

  /**
   * format for RFC 1123 date string -- "Sun, 06 Nov 1994 08:49:37 GMT"
   */
  public static final String RFC1123_PATTERN = "EEE, dd MMM yyyyy HH:mm:ss z";

  private static final ArrayList<String> cache_control;

  private static final int MAX_AGE_SECOND = 604800;

  static {
    cache_control = new ArrayList<String>(2);
    cache_control.add(HttpHeaderValues.PUBLIC.toString());
    cache_control.add(HttpHeaderValues.MAX_AGE + "=" + MAX_AGE_SECOND);// 1 week
    cache_control.add(HttpHeaderValues.MUST_REVALIDATE.toString());
  }

  /**
   * set MIME TYPE if possible
   */
  public static final MimetypesFileTypeMap mimetypesFileTypeMap =
      new MimetypesFileTypeMap();

  static {
    mimetypesFileTypeMap.addMimeTypes("text/css css CSS");
    mimetypesFileTypeMap.addMimeTypes("text/javascript js JS");
    // Official but not supported mimetypesFileTypeMap.addMimeTypes("application/javascript js JS")
    mimetypesFileTypeMap.addMimeTypes("application/json json JSON map MAP");
    mimetypesFileTypeMap.addMimeTypes("text/plain txt text TXT");
    mimetypesFileTypeMap.addMimeTypes("text/html htm html HTM HTML htmls htx");
    mimetypesFileTypeMap.addMimeTypes("image/jpeg jpe jpeg jpg JPG");
    mimetypesFileTypeMap.addMimeTypes("image/png png PNG");
    mimetypesFileTypeMap.addMimeTypes("image/gif gif GIF");
    mimetypesFileTypeMap.addMimeTypes("image/x-icon ico ICO");
  }

  private HttpWriteCacheEnable() {
  }

  /**
   * Write a file, taking into account cache enabled and removing session
   * cookie
   *
   * @param request
   * @param ctx
   * @param filename
   * @param cookieNameToRemove
   */
  public static void writeFile(HttpRequest request, ChannelHandlerContext ctx,
                               String filename, String cookieNameToRemove) {
    // Convert the response content to a ByteBuf.
    HttpResponse response;
    final boolean keepAlive = HttpUtil.isKeepAlive(request);
    final File file = new File(filename);
    if (!file.isFile() || !file.canRead()) {
      SysErrLogger.FAKE_LOGGER.syserr("Cannot read " + file.getAbsolutePath());
      sendError(request, ctx, cookieNameToRemove, keepAlive);
      return;
    }
    final DateFormat rfc1123Format =
        new SimpleDateFormat(RFC1123_PATTERN, LOCALE_US);
    rfc1123Format.setTimeZone(GMT_ZONE);
    final Date lastModifDate = new Date(file.lastModified());
    if (request.headers().contains(HttpHeaderNames.IF_MODIFIED_SINCE)) {
      final String sdate =
          request.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
      try {
        final Date ifmodif = rfc1123Format.parse(sdate);
        if (ifmodif.after(lastModifDate)) {
          response = new DefaultHttpResponse(HTTP_1_1,
                                             HttpResponseStatus.NOT_MODIFIED);
          handleCookies(request, response, cookieNameToRemove);
          ctx.writeAndFlush(response);
          return;
        }
      } catch (final ParseException ignored) {
        // nothing
      }
    }
    int fileLength = (int) file.length();
    ByteBuf byteBuf = Unpooled.buffer(fileLength);
    FileInputStream inputStream = null;
    try {
      inputStream = new FileInputStream(file);
      byteBuf.writeBytes(inputStream, fileLength);
    } catch (FileNotFoundException e) {
      byteBuf.release();
      sendError(request, ctx, cookieNameToRemove, keepAlive);
      return;
    } catch (IOException e) {
      byteBuf.release();
      sendError(request, ctx, cookieNameToRemove, keepAlive);
      return;
    }

    response =
        new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK, byteBuf);
    response.headers().set(HttpHeaderNames.CONTENT_LENGTH, fileLength);
    setContentTypeHeader(response, file);
    setDateAndCacheHeaders(response, file, rfc1123Format, lastModifDate);

    if (!keepAlive) {
      response.headers()
              .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
    } else if (request.protocolVersion().equals(HTTP_1_0)) {
      response.headers()
              .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }

    // Write the initial line and the header.
    ChannelFuture sendFileFuture = ctx.writeAndFlush(response);

    // Decide whether to close the connection or not.
    if (!keepAlive) {
      // Close the connection when the whole content is written out.
      sendFileFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  /**
   * Sets the content type header for the HTTP Response
   *
   * @param response HTTP response
   * @param file file to extract content type
   */
  private static void setContentTypeHeader(HttpResponse response, File file) {
    response.headers().set(HttpHeaderNames.CONTENT_TYPE,
                           mimetypesFileTypeMap.getContentType(file.getPath()));
  }

  /**
   * Sets the Date and Cache headers for the HTTP Response
   *
   * @param response HTTP response
   * @param fileToCache file to extract content type
   */
  private static void setDateAndCacheHeaders(HttpResponse response,
                                             File fileToCache,
                                             DateFormat rfc1123Format,
                                             Date lastModifDate) {
    // Date header
    Calendar time = new GregorianCalendar();
    response.headers()
            .set(HttpHeaderNames.DATE, rfc1123Format.format(time.getTime()));

    // Add cache headers
    time.add(Calendar.SECOND, MAX_AGE_SECOND);
    response.headers()
            .set(HttpHeaderNames.EXPIRES, rfc1123Format.format(time.getTime()));
    response.headers().set(HttpHeaderNames.CACHE_CONTROL, cache_control);
    response.headers().set(HttpHeaderNames.LAST_MODIFIED,
                           rfc1123Format.format(lastModifDate));
  }

  private static void sendError(final HttpRequest request,
                                final ChannelHandlerContext ctx,
                                final String cookieNameToRemove,
                                boolean keepAlive) {
    final HttpResponse response;
    response =
        new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_FOUND);
    response.headers().add(HttpHeaderNames.CONTENT_LENGTH, 0);
    if (!keepAlive) {
      response.headers()
              .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
    } else if (request.protocolVersion().equals(HTTP_1_0)) {
      response.headers()
              .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
    }
    handleCookies(request, response, cookieNameToRemove);
    ChannelFuture sendFileFuture = ctx.writeAndFlush(response);
    // Decide whether to close the connection or not.
    if (!keepAlive) {
      // Close the connection when the whole content is written out.
      sendFileFuture.addListener(ChannelFutureListener.CLOSE);
    }
  }

  /**
   * Remove the given named cookie
   *
   * @param request
   * @param response
   * @param cookieNameToRemove
   */
  public static void handleCookies(HttpRequest request, HttpResponse response,
                                   String cookieNameToRemove) {
    final String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
    if (cookieString != null) {
      final Set<Cookie> cookies = ServerCookieDecoder.LAX.decode(cookieString);
      if (!cookies.isEmpty()) {
        // Reset the sessions if necessary.
        // Remove all Session for images
        for (final Cookie cookie : cookies) {
          if (cookie.name().equalsIgnoreCase(cookieNameToRemove)) {
            // nothing
          } else {
            response.headers().add(HttpHeaderNames.SET_COOKIE,
                                   ServerCookieEncoder.LAX.encode(cookie));
          }
        }
      }
    }
  }

}
