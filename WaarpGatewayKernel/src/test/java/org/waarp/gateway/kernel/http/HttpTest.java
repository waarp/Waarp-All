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

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.gateway.kernel.AbstractHttpField;
import org.waarp.gateway.kernel.AbstractHttpField.FieldPosition;
import org.waarp.gateway.kernel.AbstractHttpField.FieldRole;
import org.waarp.gateway.kernel.DefaultHttpField;
import org.waarp.gateway.kernel.HttpBusinessFactory;
import org.waarp.gateway.kernel.HttpPage;
import org.waarp.gateway.kernel.HttpPage.PageRole;
import org.waarp.gateway.kernel.HttpPageHandler;
import org.waarp.gateway.kernel.http.saplink.HttpSapBusinessFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static org.junit.Assert.*;

public class HttpTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();


  @Test
  public void testSimpleHttp()
      throws InterruptedException, InstantiationException,
             IllegalAccessException, ClassNotFoundException, IOException {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    File pageFile = new File("/tmp/empty.txt");
    FileOutputStream outputStream = new FileOutputStream(pageFile);
    outputStream.write("test".getBytes());
    outputStream.flush();
    outputStream.close();
    final EventLoopGroup workerGroup = new NioEventLoopGroup();
    // Configure the server.
    final ServerBootstrap httpBootstrap = new ServerBootstrap();
    WaarpNettyUtil.setServerBootstrap(httpBootstrap, workerGroup, 500);
    // Configure the pipeline factory.
    HashMap<String, HttpPage> map = new HashMap<String, HttpPage>();
    PageRole pageRole = PageRole.DELETE;
    String pagename = "empty.txt";
    String uri = "empty.txt";
    String header = "<HTML><HEAD><TITLE>TITLE</TITLE></HEAD><BODY>";
    String footer = "</BODY></HTML>";
    String beginform = "<table border=\"0\"><tr><td><h1>TITLE</h1>";
    String endform =
        "</td></tr></table><br><CENTER><HR WIDTH=\"75%\" NOSHADE color=\"blue\"></CENTER>";
    String nextinform = "</td></tr><tr><td>";
    Class clasz = HttpSapBusinessFactory.class;
    String classname = clasz.getName();
    String errorpage = null;
    LinkedHashMap<String, AbstractHttpField> linkedHashMap =
        new LinkedHashMap<String, AbstractHttpField>();
    String fieldname = "field";
    String fieldinfo = "Field information";
    String fieldvalue = HttpResponseStatus.BAD_REQUEST.reasonPhrase();
    FieldRole fieldRole = FieldRole.BUSINESS_INPUT_TEXT;
    boolean fieldvisibility = true;
    boolean fieldmandatory = false;
    boolean fieldcookieset = false;
    boolean fieldtovalidate = false;
    int fieldrank = 1;
    linkedHashMap.put(fieldname,
                      new DefaultHttpField(fieldname, fieldRole, fieldinfo,
                                           fieldvalue, fieldvisibility,
                                           fieldmandatory, fieldcookieset,
                                           fieldtovalidate, FieldPosition.BODY,
                                           fieldrank));
    pageRole = PageRole.DELETE;
    HttpPage page =
        new HttpPage(pagename, null, header, footer, beginform, endform,
                     nextinform, uri, pageRole, errorpage, classname,
                     linkedHashMap);
    map.put("/DELETE", page);
    pageRole = PageRole.GETDOWNLOAD;
    page = new HttpPage(pagename, null, header, footer, beginform, endform,
                        nextinform, uri, pageRole, errorpage, classname,
                        linkedHashMap);
    map.put("/GET", page);
    pageRole = PageRole.POST;
    page = new HttpPage(pagename, null, header, footer, beginform, endform,
                        nextinform, uri, pageRole, errorpage, classname,
                        linkedHashMap);
    map.put("/POST", page);
    pageRole = PageRole.PUT;
    page = new HttpPage(pagename, null, header, footer, beginform, endform,
                        nextinform, uri, pageRole, errorpage, classname,
                        linkedHashMap);
    map.put("/PUT", page);
    pageRole = PageRole.POST;
    page = new HttpPage(pagename, null, header, footer, beginform, endform,
                        nextinform, uri, pageRole, errorpage, classname,
                        linkedHashMap);
    map.put("/PATCH", page);
    pageRole = PageRole.HTML;
    page = new HttpPage(pagename, null, header, footer, beginform, endform,
                        nextinform, uri, pageRole, errorpage, classname,
                        linkedHashMap);
    map.put("/OPTIONS", page);
    pageRole = PageRole.HTML;
    page = new HttpPage(pagename, null, header, footer, beginform, endform,
                        nextinform, uri, pageRole, errorpage, classname,
                        linkedHashMap);
    map.put("/HEAD", page);
    pageRole = PageRole.HTML;
    page = new HttpPage(pagename, null, header, footer, beginform, endform,
                        nextinform, uri, pageRole, errorpage, classname,
                        linkedHashMap);
    map.put("/TRACE", page);
    HttpPageHandler httpPageHandler = new HttpPageHandler(map);
    HttpBusinessFactory.addDefaultErrorPages(httpPageHandler, "TITLE", clasz);
    httpBootstrap.childHandler(new HttpHttpInitializer(httpPageHandler));
    // Bind and start to accept incoming connections.
    final ChannelFuture future =
        httpBootstrap.bind(new InetSocketAddress(1234));
    try {
      future.await();
    } catch (final InterruptedException e) {//NOSONAR
      e.printStackTrace();
    }
    // Configure the client
    final Bootstrap bootstrap = new Bootstrap();
    WaarpNettyUtil.setBootstrap(bootstrap, workerGroup, 500);
    bootstrap.handler(new HttpHttpClientInitializer());
    Channel clientChannel =
        bootstrap.connect("localhost", 1234).sync().channel();
    HttpRequest request =
        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                                   "/GET");
    request.headers().set(HttpHeaderNames.HOST, "localhost")
           .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
           .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    assertTrue(clientChannel.writeAndFlush(request).awaitUninterruptibly()
                            .isSuccess());
    clientChannel.closeFuture().sync();
    clientChannel.close();

    clientChannel = bootstrap.connect("localhost", 1234).sync().channel();
    request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                                         "/empty.txt");
    request.headers().set(HttpHeaderNames.HOST, "localhost")
           .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
           .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    assertTrue(clientChannel.writeAndFlush(request).awaitUninterruptibly()
                            .isSuccess());
    clientChannel.closeFuture().sync();

    clientChannel = bootstrap.connect("localhost", 1234).sync().channel();
    request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET,
                                         "/empty.txt");
    request.headers().set(HttpHeaderNames.HOST, "localhost")
           .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
           .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP)
           .set(HttpHeaderNames.IF_MODIFIED_SINCE,
                "Sun, 06 Nov 2994 08:49:37 GMT");
    assertTrue(clientChannel.writeAndFlush(request).awaitUninterruptibly()
                            .isSuccess());
    clientChannel.closeFuture().sync();

    clientChannel = bootstrap.connect("localhost", 1234).sync().channel();
    request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST,
                                         "/POST");
    request.headers().set(HttpHeaderNames.HOST, "localhost")
           .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
           .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    assertTrue(clientChannel.writeAndFlush(request).awaitUninterruptibly()
                            .isSuccess());
    clientChannel.closeFuture().sync();


    clientChannel = bootstrap.connect("localhost", 1234).sync().channel();
    request =
        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.DELETE,
                                   "/DELETE");
    request.headers().set(HttpHeaderNames.HOST, "localhost")
           .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
           .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    assertTrue(clientChannel.writeAndFlush(request).awaitUninterruptibly()
                            .isSuccess());
    clientChannel.closeFuture().sync();


    clientChannel = bootstrap.connect("localhost", 1234).sync().channel();
    request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.HEAD,
                                         "/HEAD");
    request.headers().set(HttpHeaderNames.HOST, "localhost")
           .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
           .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    assertTrue(clientChannel.writeAndFlush(request).awaitUninterruptibly()
                            .isSuccess());
    clientChannel.closeFuture().sync();

    clientChannel = bootstrap.connect("localhost", 1234).sync().channel();
    request =
        new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.OPTIONS,
                                   "/OPTIONS");
    request.headers().set(HttpHeaderNames.HOST, "localhost")
           .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
           .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    assertTrue(clientChannel.writeAndFlush(request).awaitUninterruptibly()
                            .isSuccess());
    clientChannel.closeFuture().sync();

    clientChannel = bootstrap.connect("localhost", 1234).sync().channel();
    request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PATCH,
                                         "/PATCH");
    request.headers().set(HttpHeaderNames.HOST, "localhost")
           .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
           .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    assertTrue(clientChannel.writeAndFlush(request).awaitUninterruptibly()
                            .isSuccess());
    clientChannel.closeFuture().sync();

    clientChannel = bootstrap.connect("localhost", 1234).sync().channel();
    request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT,
                                         "/PUT");
    request.headers().set(HttpHeaderNames.HOST, "localhost")
           .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
           .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    assertTrue(clientChannel.writeAndFlush(request).awaitUninterruptibly()
                            .isSuccess());
    clientChannel.closeFuture().sync();

    clientChannel = bootstrap.connect("localhost", 1234).sync().channel();
    request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.TRACE,
                                         "/TRACE");
    request.headers().set(HttpHeaderNames.HOST, "localhost")
           .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
           .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
    assertTrue(clientChannel.writeAndFlush(request).awaitUninterruptibly()
                            .isSuccess());
    clientChannel.closeFuture().sync();

    future.channel().close();
    workerGroup.shutdownGracefully();
  }
}
