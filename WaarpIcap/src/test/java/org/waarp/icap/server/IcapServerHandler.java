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

package org.waarp.icap.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.icap.IcapClient;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class IcapServerHandler extends SimpleChannelInboundHandler<String> {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(IcapServerHandler.class);

  private static final AtomicBoolean isShutdown = new AtomicBoolean(true);
  private static final AtomicBoolean isPreviewOk = new AtomicBoolean(true);
  private static final AtomicInteger finalStatus = new AtomicInteger(204);
  private static final AtomicInteger intermediaryStatus =
      new AtomicInteger(100);

  private static final String ICAP_HEADER = "ICAP/1.0 ";
  private static final String SHUTDOWN_ANSWER =
      ICAP_HEADER + "500 SHUTDOWN" + IcapClient.TERMINATOR;
  private static final String OK_ANSWER =
      ICAP_HEADER + "200 OK" + IcapClient.TERMINATOR;
  private static final String CONTINUE_ANSWER =
      ICAP_HEADER + "100 CONTINUE" + IcapClient.TERMINATOR;
  private static final String ACCEPTED_ANSWER =
      ICAP_HEADER + "204 ACCEPTED" + IcapClient.TERMINATOR;
  private static final String STANDARD_ICAP_HEADERS =
      "Service: FOO Icap Server 1.0" + IcapClient.TERMINATOR +
      "Max-Connections: 1000" + IcapClient.TERMINATOR + "Allow: 204" +
      IcapClient.TERMINATOR;
  public static final String DEFAULT_HTML =
      "Date: Mon, 10 Jan 2000  09:55:21 GMT\n" +
      "Via: 1.0 icap.example.org (ICAP Example RespMod Service 1.1)\n" +
      "Server: WaarpFakeIcap(Unix)\n" + "ETag: \"63840-1ab7-378d415b\"\n" +
      "Content-Type: text/html\n" + "Content-Length: 92\n\n" + "5c\n" +
      "This is data that was returned by an origin server, but with\n" +
      "value added by an ICAP server.\n";
  private static String htmlContent = DEFAULT_HTML;

  private final long delay;
  private final IcapServerInitializer factory;
  private final String ISTag;

  enum REQUEST {
    REQMOD, RESPMOD, OPTIONS
  }

  enum ICAP_STEP {
    /**
     * Nothing yet
     */
    NONE,
    /**
     * CRLFCRLF on Icap Headers
     */
    ICAP_HEADERS,
    /**
     * CRLFCRLF on Http Query
     */
    HTTP_QUERY,
    /**
     * CRLFCRLF on HTTP Header
     */
    HTTP_HEADERS,
    /**
     * 0: ieofCRLFCRLF after HTTP Body
     */
    BODY_IEOF,
    /**
     * First 0CRLFCRLF after HTTP Body
     */
    BODY,
    /**
     * CRLFCRLF after each HTTP Bodies (chunks)
     */
    CHUNKS,
    /**
     * Last 0CRLFCRLF after all HTTP Bodies (chunks)
     */
    END
  }

  private REQUEST request = null;
  private String service = null;
  private ICAP_STEP icapStep = ICAP_STEP.NONE;

  /**
   * For Junit test
   */
  public static boolean setIsShutdownTest(boolean shutdown) {
    return isShutdown.getAndSet(shutdown);
  }

  /**
   * For Junit test
   */
  public static boolean setIsPreviewOkTest(boolean previewOk) {
    return isPreviewOk.getAndSet(previewOk);
  }

  /**
   * For Junit test
   */
  public static void setFinalStatus(final int status) {
    finalStatus.set(status);
  }

  /**
   * For Junit test
   */
  public static void setHtmlContent(final String htmlContentAfterHttpStatus) {
    htmlContent = htmlContentAfterHttpStatus;
  }

  /**
   * For Junit test
   */
  public static void setIntermediaryStatus(final int status) {
    intermediaryStatus.set(status);
  }

  /**
   * For Junit test
   */
  public static void resetJunitStatus() {
    setFinalStatus(204);
    setIntermediaryStatus(100);
    setIsPreviewOkTest(true);
    setHtmlContent(DEFAULT_HTML);
  }

  public IcapServerHandler(final IcapServerInitializer factory,
                           final long delay) {
    this.delay = delay;
    this.factory = factory;
    long nano = System.nanoTime();
    this.ISTag = Long.toHexString(nano);
  }

  private void checkIcapMethod(final ChannelHandlerContext ctx,
                               final String msg) {
    String first = msg.substring(0, msg.indexOf('\r'));
    String[] split = first.split(" ");
    if (REQUEST.OPTIONS.name().equalsIgnoreCase(split[0])) {
      request = REQUEST.OPTIONS;
    } else if (REQUEST.REQMOD.name().equalsIgnoreCase(split[0])) {
      request = REQUEST.REQMOD;
    } else if (REQUEST.RESPMOD.name().equalsIgnoreCase(split[0])) {
      request = REQUEST.RESPMOD;
    } else {
      // Error FIXME
      logger.error("Could not find REQUEST: {}", first);
      sendError(ctx, "Could not find Request");
      resetStatus();
      return;
    }
    if (!IcapClient.VERSION.equalsIgnoreCase(split[2])) {
      // Error FIXME
      logger.error("Could not find Version: {}", first);
    }
    String[] services = split[1].split("/");
    service = services[services.length - 1];
    logger.debug("Received {} for {} with version {}", request.name(), service,
                 split[2]);
    icapStep = ICAP_STEP.ICAP_HEADERS;
  }

  private void readIcapStructure(final ChannelHandlerContext ctx,
                                 final String msg) {
    logger.debug("RCV:\n{}", msg);
    // msg ends up with CRLFCRLF so
    // WAIT for any ICAP_TERMINATOR
    switch (icapStep) {
      case NONE:
        // Check ICAP method
        checkIcapMethod(ctx, msg);
        if (icapStep != ICAP_STEP.ICAP_HEADERS) {
          resetStatus();
          return;
        }
        break;
      case ICAP_HEADERS:
        icapStep = ICAP_STEP.HTTP_QUERY;
        break;
      case HTTP_QUERY:
        icapStep = ICAP_STEP.HTTP_HEADERS;
        break;
      case HTTP_HEADERS:
        // Check if ieof or not
        if (msg.endsWith("0; ieof" + IcapClient.ICAP_TERMINATOR)) {
          icapStep = ICAP_STEP.BODY_IEOF;
        } else {
          icapStep = ICAP_STEP.BODY;
        }
        break;
      case BODY_IEOF:
        // Should not
        logger.error("Should not be: {}", icapStep);
        break;
      case BODY:
        if (msg.endsWith(IcapClient.HTTP_TERMINATOR)) {
          icapStep = ICAP_STEP.END;
        } else {
          icapStep = ICAP_STEP.CHUNKS;
        }
        break;
      case CHUNKS:
        if (msg.endsWith(IcapClient.HTTP_TERMINATOR)) {
          icapStep = ICAP_STEP.END;
        }
        break;
      case END:
        // Nothing
        break;
    }
    logger.info("REQUEST {} with step {}", request, icapStep);
    if (request == REQUEST.OPTIONS) {
      StringBuilder builder = new StringBuilder();
      if (isPreviewOk.get()) {
        builder.append(OK_ANSWER);
      } else {
        builder.append(ICAP_HEADER).append(finalStatus.get())
               .append(" OtherResult").append(IcapClient.TERMINATOR);
      }
      createIcapHeader(builder, null, 0);
      ctx.writeAndFlush(builder.toString());
      resetStatus();
      return;
    }
    // More than one msg with ICAP_SEPARATOR
    switch (icapStep) {
      case BODY_IEOF:
        // Real end just after preview
        if (request == REQUEST.REQMOD) {
          StringBuilder builder = new StringBuilder(SHUTDOWN_ANSWER);
          String http =
              "HTTP/1.1 403 Forbidden" + IcapClient.TERMINATOR + htmlContent;
          createIcapHeader(builder, http, http.length());
          ctx.writeAndFlush(builder.toString());
        } else {
          StringBuilder builder = new StringBuilder();
          String http;
          if (finalStatus.get() == 204) {
            builder.append(ACCEPTED_ANSWER);
            http =
                "HTTP/1.1 204 Accepted" + IcapClient.TERMINATOR + htmlContent;
          } else {
            builder.append(ICAP_HEADER).append(finalStatus.get())
                   .append(" OtherResult").append(IcapClient.TERMINATOR);
            http = "HTTP/1.1 " + finalStatus.get() + " OtherResult" +
                   IcapClient.TERMINATOR + htmlContent;
          }
          createIcapHeader(builder, http, http.length());
          ctx.writeAndFlush(builder.toString());
        }
        resetStatus();
        return;
      case BODY: {
        StringBuilder builder = new StringBuilder();
        String http = null;
        if (intermediaryStatus.get() == 100) {
          builder.append(CONTINUE_ANSWER);
        } else {
          builder.append(ICAP_HEADER).append(intermediaryStatus.get())
                 .append(" OtherResult").append(IcapClient.TERMINATOR);
          http = "HTTP/1.1 " + intermediaryStatus.get() + " OtherResult" +
                 IcapClient.TERMINATOR + htmlContent;
        }
        createIcapHeader(builder, http, http != null? http.length() : 0);
        ctx.writeAndFlush(builder.toString());
        if (intermediaryStatus.get() != 100) {
          resetStatus();
        }
        return;
      }
      case END: {
        StringBuilder builder = new StringBuilder();
        String http;
        if (finalStatus.get() == 204) {
          builder.append(ACCEPTED_ANSWER);
          http = "HTTP/1.1 204 Accepted" + IcapClient.TERMINATOR + htmlContent;
        } else {
          builder.append(ICAP_HEADER).append(finalStatus.get())
                 .append(" OtherResult").append(IcapClient.TERMINATOR);
          http = "HTTP/1.1 " + finalStatus.get() + " OtherResult" +
                 IcapClient.TERMINATOR + htmlContent;
        }
        createIcapHeader(builder, http, http.length());
        ctx.writeAndFlush(builder.toString());
        resetStatus();
      }
    }
  }

  private void resetStatus() {
    request = null;
    icapStep = ICAP_STEP.NONE;
  }

  private String getServerTime() {
    Calendar calendar = Calendar.getInstance();
    SimpleDateFormat dateFormat =
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    return dateFormat.format(calendar.getTime());
  }

  private void createIcapHeader(final StringBuilder builder, final String html,
                                final int lenHeader) {
    builder.append("Date: ").append(getServerTime())
           .append(IcapClient.TERMINATOR);
    builder.append(STANDARD_ICAP_HEADERS);
    if (html == null) {
      builder.append("Methods: RESPMOD").append(IcapClient.TERMINATOR);
      builder.append("Encapsulated:null-body=0").append(IcapClient.TERMINATOR);
      if (isPreviewOk.get()) {
        builder.append("Preview: 4096").append(IcapClient.TERMINATOR);
      }
      builder.append("Transfer-Complete: asp,bat,exe,com")
             .append(IcapClient.TERMINATOR).append("Transfer-Ignore: html")
             .append(IcapClient.TERMINATOR).append("Transfer-Preview: *")
             .append(IcapClient.TERMINATOR);
    }
    builder.append("ISTag: \"").append(ISTag).append("\"")
           .append(IcapClient.TERMINATOR);
    builder.append("Options-TTL: ").append(delay).append(IcapClient.TERMINATOR);
    if (html != null) {
      builder.append("Encapsulated: res-hdr=0, res-body=").append(lenHeader);
      builder.append(IcapClient.ICAP_TERMINATOR);
      builder.append(html).append(IcapClient.HTTP_TERMINATOR);
    } else {
      builder.append(IcapClient.TERMINATOR);
    }
  }

  private void sendError(ChannelHandlerContext ctx, String msg) {
    StringBuilder builder = new StringBuilder(SHUTDOWN_ANSWER);
    builder.append("X-ICAP-ERROR: ").append(msg).append(IcapClient.TERMINATOR);
    createIcapHeader(builder, null, 0);
    ctx.writeAndFlush(builder.toString());
    WaarpSslUtility.closingSslChannel(ctx.channel());
    resetStatus();
  }

  private boolean isShutdown(ChannelHandlerContext ctx) {
    if (isShutdown.get()) {
      sendError(ctx, "Server is in shutdown");
      return true;
    }
    return false;
  }

  @Override
  protected void channelRead0(final ChannelHandlerContext ctx,
                              final String msg) {
    if (isShutdown(ctx)) {
      logger.error("Could not answer since in shutdown");
      return;
    }
    readIcapStructure(ctx, msg);
  }

  @Override
  public void channelActive(ChannelHandlerContext ctx) {
    if (isShutdown(ctx)) {
      return;
    }
    factory.addChannel(ctx.channel());
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    // Look if Nothing to do since execution will stop later on and
    // an error will occur on client side
    // since no message arrived before close (or partially)
    if (cause instanceof CancelledKeyException ||
        cause instanceof ClosedChannelException) {
      // nothing
      return;
    } else if (cause instanceof NullPointerException) {
      if (ctx.channel().isActive()) {
        WaarpSslUtility.closingSslChannel(ctx.channel());
      }
      return;
    } else if (cause instanceof IOException) {
      if (ctx.channel().isActive()) {
        WaarpSslUtility.closingSslChannel(ctx.channel());
      }
      return;
    } else if (cause instanceof RejectedExecutionException) {
      if (ctx.channel().isActive()) {
        WaarpSslUtility.closingSslChannel(ctx.channel());
      }
      return;
    }
    logger.error("Unexpected exception from outbound.", cause);
  }
}
