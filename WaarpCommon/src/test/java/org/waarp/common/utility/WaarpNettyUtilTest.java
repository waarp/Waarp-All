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

package org.waarp.common.utility;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.Test;
import org.waarp.common.cpu.WaarpConstraintLimitHandler;
import org.waarp.common.crypto.ssl.WaarpSecureKeyStore;
import org.waarp.common.crypto.ssl.WaarpSslContextFactory;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.exception.CryptoException;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.*;

public class WaarpNettyUtilTest {
  private static final int PORT = 9999;

  @Test
  public void setServerBootstrap() {
    final EventLoopGroup bossGroup = new NioEventLoopGroup();
    final EventLoopGroup workerGroup = new NioEventLoopGroup();
    final Bootstrap clientBootstrap = new Bootstrap();
    WaarpNettyUtil.setBootstrap(clientBootstrap, workerGroup, 10000);
    clientBootstrap.handler(new HttpClientInitializer(null));

    final ServerBootstrap bootstrap = new ServerBootstrap();
    WaarpNettyUtil.setServerBootstrap(bootstrap, bossGroup, workerGroup, 30000);
    bootstrap.childHandler(new HttpServerInitializer(null));
    ChannelFuture future = null;
    try {
      future = bootstrap.bind(PORT).sync();
      future.channel();
      final Channel clientChannel =
          clientBootstrap.connect("localhost", PORT).sync().channel();
      final HttpRequest request =
          new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
      request.headers().set(HttpHeaderNames.HOST, "localhost")
             .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
             .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
      clientChannel.writeAndFlush(request);
      clientChannel.closeFuture().sync();
    } catch (final Exception e) {
      fail("Should not " + e.getMessage());
    } finally {
      if (future != null) {
        final Channel channel = future.channel();
        try {
          channel.close().sync();
        } catch (final InterruptedException e) {
          e.printStackTrace();
        }
      }
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
      System.out.println("NoSsl done");
    }
  }

  @Test
  public void setServerBootstrap1Group() {
    final EventLoopGroup workerGroup = new NioEventLoopGroup();
    final Bootstrap clientBootstrap = new Bootstrap();
    WaarpNettyUtil.setBootstrap(clientBootstrap, workerGroup, 10000);
    clientBootstrap.handler(new HttpClientInitializer(null));

    final ServerBootstrap bootstrap = new ServerBootstrap();
    WaarpNettyUtil.setServerBootstrap(bootstrap, workerGroup, 30000);
    bootstrap.childHandler(new HttpServerInitializer(null));
    ChannelFuture future = null;
    try {
      future = bootstrap.bind(PORT).sync();
      future.channel();
      final Channel clientChannel =
          clientBootstrap.connect("localhost", PORT).sync().channel();
      final HttpRequest request =
          new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
      request.headers().set(HttpHeaderNames.HOST, "localhost")
             .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
             .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
      clientChannel.writeAndFlush(request);
      clientChannel.closeFuture().sync();

    } catch (final Exception e) {
      fail("Should not " + e.getMessage());
    } finally {
      if (future != null) {
        final Channel channel = future.channel();
        try {
          channel.close().sync();
        } catch (final InterruptedException e) {
          e.printStackTrace();
        }
      }
      workerGroup.shutdownGracefully();
      System.out.println("NoSsl 1 group done");
    }
  }

  @Test
  public void setServerSslBootstrap() throws CryptoException {
    // Load the KeyStore (No certificates)
    final String keyStoreFilename = "certs/testsslnocert.jks";
    final ClassLoader classLoader = WaarpNettyUtilTest.class.getClassLoader();
    final URL url = classLoader.getResource(keyStoreFilename);
    assertNotNull(url);
    final File file = new File(url.getFile());
    assertTrue("File Should exists", file.exists());
    final String keyStorePasswd = "testsslnocert";
    final String keyPassword = "testalias";
    final WaarpSecureKeyStore WaarpSecureKeyStore =
        new WaarpSecureKeyStore(file.getAbsolutePath(), keyStorePasswd,
                                keyPassword);
    // Include certificates
    final String trustStoreFilename = "certs/testcert.jks";
    final File file2 =
        new File(classLoader.getResource(trustStoreFilename).getFile());
    assertTrue("File2 Should exists", file2.exists());
    final String trustStorePasswd = "testcert";
    WaarpSecureKeyStore
        .initTrustStore(file2.getAbsolutePath(), trustStorePasswd, true);
    final WaarpSslContextFactory waarpSslContextFactory =
        new WaarpSslContextFactory(WaarpSecureKeyStore);

    final EventLoopGroup bossGroup = new NioEventLoopGroup();
    final EventLoopGroup workerGroup = new NioEventLoopGroup();
    final Bootstrap clientBootstrap = new Bootstrap();
    WaarpNettyUtil.setBootstrap(clientBootstrap, workerGroup, 10000);

    clientBootstrap.handler(new HttpClientInitializer(waarpSslContextFactory));

    final ServerBootstrap bootstrap = new ServerBootstrap();
    WaarpNettyUtil.setServerBootstrap(bootstrap, bossGroup, workerGroup, 30000);
    bootstrap.childHandler(new HttpServerInitializer(waarpSslContextFactory));
    ChannelFuture future = null;
    try {
      future = bootstrap.bind(PORT).sync();
      future.channel();
      final Channel clientChannel =
          clientBootstrap.connect("localhost", PORT).sync().channel();
      WaarpSslUtility.waitForHandshake(clientChannel);
      final HttpRequest request =
          new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
      request.headers().set(HttpHeaderNames.HOST, "localhost")
             .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
             .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
      clientChannel.writeAndFlush(request);
      clientChannel.closeFuture().sync();
    } catch (final Exception e) {
      fail("Should not " + e.getMessage());
    } finally {
      if (future != null) {
        final Channel channel = future.channel();
        try {
          channel.close().sync();
        } catch (final InterruptedException e) {
          e.printStackTrace();
        }
      }
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
      System.out.println("Ssl done");
    }

  }

  @Test
  public void setServerSslBootstrap1Group() throws CryptoException {
    // Load the KeyStore (No certificates)
    final String keyStoreFilename = "certs/testsslnocert.jks";
    final ClassLoader classLoader = WaarpNettyUtilTest.class.getClassLoader();
    final URL url = classLoader.getResource(keyStoreFilename);
    assertNotNull(url);
    final File file = new File(url.getFile());
    assertTrue("File Should exists", file.exists());
    final String keyStorePasswd = "testsslnocert";
    final String keyPassword = "testalias";
    final WaarpSecureKeyStore WaarpSecureKeyStore =
        new WaarpSecureKeyStore(file.getAbsolutePath(), keyStorePasswd,
                                keyPassword);
    // Include certificates
    final String trustStoreFilename = "certs/testcert.jks";
    final File file2 =
        new File(classLoader.getResource(trustStoreFilename).getFile());
    assertTrue("File2 Should exists", file2.exists());
    final String trustStorePasswd = "testcert";
    WaarpSecureKeyStore
        .initTrustStore(file2.getAbsolutePath(), trustStorePasswd, true);
    final WaarpSslContextFactory waarpSslContextFactory =
        new WaarpSslContextFactory(WaarpSecureKeyStore);

    final EventLoopGroup workerGroup = new NioEventLoopGroup();
    final Bootstrap clientBootstrap = new Bootstrap();
    WaarpNettyUtil.setBootstrap(clientBootstrap, workerGroup, 10000);

    clientBootstrap.handler(new HttpClientInitializer(waarpSslContextFactory));

    final ServerBootstrap bootstrap = new ServerBootstrap();
    WaarpNettyUtil.setServerBootstrap(bootstrap, workerGroup, 30000);
    bootstrap.childHandler(new HttpServerInitializer(waarpSslContextFactory));
    ChannelFuture future = null;
    try {
      future = bootstrap.bind(PORT).sync();
      future.channel();
      final ChannelFuture clientFuture =
          clientBootstrap.connect("localhost", PORT).sync();
      final Channel clientChannel =
          WaarpSslUtility.waitforChannelReady(clientFuture);
      assertNotNull(clientChannel);
      WaarpSslUtility.waitForHandshake(clientChannel);
      WaarpSslUtility.addSslOpenedChannel(clientChannel);
      final HttpRequest request =
          new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
      request.headers().set(HttpHeaderNames.HOST, "localhost")
             .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
             .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
      clientChannel.writeAndFlush(request)
                   .addListener(WaarpSslUtility.SSLCLOSE);
      WaarpSslUtility.waitForClosingSslChannel(clientChannel, 100);
      clientChannel.closeFuture().sync();
      WaarpSslUtility.closingSslChannel(clientChannel);
      WaarpSslUtility.forceCloseAllSslChannels();
    } catch (final Exception e) {
      e.printStackTrace();
      fail("Should not " + e.getMessage());
    } finally {
      if (future != null) {
        final Channel channel = future.channel();
        try {
          channel.close().sync();
        } catch (final InterruptedException e) {
          e.printStackTrace();
        }
      }
      workerGroup.shutdownGracefully();
      System.out.println("Ssl done");
    }
  }

  @Test
  public void setServerBootstrap1GroupWithConstraint() {
    ResourceLeakDetector.setLevel(Level.PARANOID);
    final EventLoopGroup workerGroup = new NioEventLoopGroup();
    final GlobalTrafficShapingHandler trafficShapingHandler =
        new GlobalTrafficShapingHandler(workerGroup.next());
    final ExtendedConstraintLimitHandler constraintLimitHandler =
        new ExtendedConstraintLimitHandler(trafficShapingHandler);
    constraintLimitHandler.setServer(true);
    workerGroup.execute(constraintLimitHandler);

    final EventLoopGroup bossGroup = new NioEventLoopGroup();
    final Bootstrap clientBootstrap = new Bootstrap();
    WaarpNettyUtil.setBootstrap(clientBootstrap, workerGroup, 10000);
    clientBootstrap.handler(new HttpClientInitializer(null));

    final ServerBootstrap bootstrap = new ServerBootstrap();
    WaarpNettyUtil.setServerBootstrap(bootstrap, bossGroup, workerGroup, 30000);
    bootstrap.childHandler(new HttpServerInitializer(null));
    ChannelFuture future = null;
    try {
      future = bootstrap.bind(PORT).sync();
      final Channel channel = future.channel();
      channel.pipeline().addFirst("Traffic", trafficShapingHandler);
      Channel clientChannel =
          clientBootstrap.connect("localhost", PORT).sync().channel();
      HttpRequest request =
          new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
      request.headers().set(HttpHeaderNames.HOST, "localhost")
             .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
             .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
      Thread.sleep(10);
      clientChannel.writeAndFlush(request);
      assertFalse(constraintLimitHandler.checkConstraints());
      clientChannel.closeFuture().sync();
      clientChannel =
          clientBootstrap.connect("localhost", PORT).sync().channel();
      request =
          new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
      request.headers().set(HttpHeaderNames.HOST, "localhost")
             .set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
             .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP);
      Thread.sleep(10);
      clientChannel.writeAndFlush(request);
      clientChannel.closeFuture().sync();
    } catch (final Exception e) {
      fail("Should not " + e.getMessage());
    } finally {
      if (future != null) {
        final Channel channel = future.channel();
        try {
          channel.close().sync();
        } catch (final InterruptedException e) {
          e.printStackTrace();
        }
      }
      constraintLimitHandler.release();
      trafficShapingHandler.release();
      bossGroup.shutdownGracefully();
      workerGroup.shutdownGracefully();
      System.out.println("Traffic and Constraint done");
    }

  }

  /*
   * @Test public void setServerBootstrap1GroupWithConstraint() { final EventLoopGroup workerGroup = new
   * NioEventLoopGroup(); final GlobalTrafficShapingHandler trafficShapingHandler = new
   * GlobalTrafficShapingHandler(workerGroup.next()); final ExtendedConstraintLimitHandler
   * constraintLimitHandler = new ExtendedConstraintLimitHandler(trafficShapingHandler);
   * constraintLimitHandler.setServer(true); workerGroup.execute(constraintLimitHandler);
   *
   * final Bootstrap clientBootstrap = new Bootstrap(); WaarpNettyUtil.setBootstrap(clientBootstrap,
   * workerGroup, 10000); clientBootstrap.handler(new HttpClientInitializer(null));
   *
   * ServerBootstrap bootstrap = new ServerBootstrap(); WaarpNettyUtil.setServerBootstrap(bootstrap,
   * workerGroup, 30000); bootstrap.childHandler(new HttpServerInitializer(null)); ChannelFuture future = null;
   * try { future = bootstrap.bind(PORT).sync(); Channel channel = future.channel();
   * channel.pipeline().addFirst("Traffic", trafficShapingHandler); Channel clientChannel =
   * clientBootstrap.connect("localhost", PORT).sync().channel(); HttpRequest request = new
   * DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
   * request.headers().set(HttpHeaderNames.HOST, "localhost") .set(HttpHeaderNames.CONNECTION,
   * HttpHeaderValues.CLOSE) .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP); Thread.sleep(10);
   * clientChannel.writeAndFlush(request); assertFalse(constraintLimitHandler.checkConstraints());
   * clientChannel.closeFuture().sync(); clientChannel = clientBootstrap.connect("localhost",
   * PORT).sync().channel(); request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/");
   * request.headers().set(HttpHeaderNames.HOST, "localhost") .set(HttpHeaderNames.CONNECTION,
   * HttpHeaderValues.CLOSE) .set(HttpHeaderNames.ACCEPT_ENCODING, HttpHeaderValues.GZIP); Thread.sleep(10);
   * clientChannel.writeAndFlush(request); clientChannel.closeFuture().sync(); } catch (Exception e) {
   * fail("Should not " + e.getMessage()); } finally { if (future != null) { Channel channel = future.channel();
   * try { channel.close().sync(); } catch (InterruptedException e) { e.printStackTrace(); } }
   * constraintLimitHandler.release(); trafficShapingHandler.release(); workerGroup.shutdownGracefully();
   * System.out.println("Traffic and Constraint done"); }
   *
   * }
   */

  public class HttpServerHandler
      extends SimpleChannelInboundHandler<HttpObject> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg)
        throws Exception {
      if (msg instanceof LastHttpContent) {
        final ByteBuf content =
            Unpooled.copiedBuffer("Hello World.", CharsetUtil.UTF_8);
        final FullHttpResponse response =
            new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                                        HttpResponseStatus.OK, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain");
        response.headers()
                .set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        ctx.write(response);
      }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
        throws Exception {
      ctx.flush();
    }
  }

  public class HttpServerInitializer extends ChannelInitializer<SocketChannel> {
    private final WaarpSslContextFactory sslContextFactory;

    public HttpServerInitializer(WaarpSslContextFactory sslContextFactory) {
      this.sslContextFactory = sslContextFactory;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
      final ChannelPipeline pipeline = ch.pipeline();
      if (sslContextFactory != null) {
        // Add SSL as first element in the pipeline
        final SslHandler sslhandler = sslContextFactory.initInitializer(true,
                                                                        sslContextFactory
                                                                            .needClientAuthentication());
        pipeline.addLast("ssl", sslhandler);
        WaarpSslUtility.addSslOpenedChannel(ch);
      }
      pipeline.addLast(new HttpServerCodec());
      pipeline.addLast(new HttpServerHandler());
    }
  }

  public class HttpClientHandler
      extends SimpleChannelInboundHandler<HttpObject> {

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
      if (msg instanceof HttpResponse) {
        final HttpResponse response = (HttpResponse) msg;

        System.err.println("STATUS: " + response.status());
        System.err.println("VERSION: " + response.protocolVersion());
        System.err.println();

        if (!response.headers().isEmpty()) {
          for (final CharSequence name : response.headers().names()) {
            for (final CharSequence value : response.headers().getAll(name)) {
              System.err.println("HEADER: " + name + " = " + value);
            }
          }
          System.err.println();
        }

        if (HttpUtil.isTransferEncodingChunked(response)) {
          System.err.println("CHUNKED CONTENT {");
        } else {
          System.err.println("CONTENT {");
        }
      }
      if (msg instanceof HttpContent) {
        final HttpContent content = (HttpContent) msg;

        System.err.print(content.content().toString(CharsetUtil.UTF_8));
        System.err.flush();

        if (content instanceof LastHttpContent) {
          System.err.println("} END OF CONTENT");
          ctx.close();
        }
      }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      cause.printStackTrace();
      ctx.close();
    }
  }

  public class HttpClientInitializer extends ChannelInitializer<SocketChannel> {

    private final WaarpSslContextFactory sslContextFactory;

    public HttpClientInitializer(WaarpSslContextFactory sslContextFactory) {
      this.sslContextFactory = sslContextFactory;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
      final ChannelPipeline pipeline = ch.pipeline();

      // Enable HTTPS if necessary.
      if (sslContextFactory != null) {
        // Add SSL as first element in the pipeline
        final SslHandler sslhandler = sslContextFactory.initInitializer(false,
                                                                        sslContextFactory
                                                                            .needClientAuthentication());
        pipeline.addLast("ssl", sslhandler);
        WaarpSslUtility.addSslOpenedChannel(ch);
      }
      pipeline.addLast(new HttpClientCodec());
      // Remove the following line if you don't want automatic content decompression.
      pipeline.addLast(new HttpContentDecompressor());
      pipeline.addLast(new HttpObjectAggregator(1048576));
      pipeline.addLast(new HttpClientHandler());
    }
  }

  public class ExtendedConstraintLimitHandler
      extends WaarpConstraintLimitHandler {

    /**
     * This constructor enables both Connection check ability and throttling
     * bandwidth with cpu usage
     *
     * @param handler the GlobalTrafficShapingHandler associated
     *     (null to have no proactive cpu limitation)
     */
    public ExtendedConstraintLimitHandler(GlobalTrafficShapingHandler handler) {
      super(1000, 10000, true, true, 0.99, 0, 0, 1, 0.1, handler, 10, 4096);
    }

    @Override
    protected int getNumberLocalChannel() {
      return 0;
    }

    @Override
    protected long getReadLimit() {
      return 0;
    }

    @Override
    protected long getWriteLimit() {
      return 0;
    }
  }

}