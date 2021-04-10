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
package org.waarp.commandexec.client.test;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.commandexec.client.LocalExecClientHandler;
import org.waarp.commandexec.client.LocalExecClientInitializer;
import org.waarp.commandexec.server.LocalExecServerHandler;
import org.waarp.commandexec.server.LocalExecServerInitializer;
import org.waarp.commandexec.utils.LocalExecDefaultResult;
import org.waarp.commandexec.utils.LocalExecResult;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpThreadFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * LocalExec client.
 * <p>
 * This class is an example of client.
 * <p>
 * On a bi-core Centrino2 vPro: 18/s in 50 sequential, 30/s in 10 threads with
 * 50 sequential<br>
 * On a quad-core i7: 29/s in 50 sequential, 187/s in 10 threads with 50
 * sequential
 */
public class LocalExecClientTest extends Thread {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  static int nit = 20;
  static int nth = 4;
  static String command = "echo";
  static int port = 9999;
  static InetSocketAddress address;
  static LocalExecResult result;
  static int ok;
  static int ko;
  static AtomicInteger atomicInteger = new AtomicInteger();
  static EventLoopGroup workerGroup = new NioEventLoopGroup();
  static EventExecutorGroup executor =
      new DefaultEventExecutorGroup(DetectionUtils.numberThreads(),
                                    new WaarpThreadFactory("LocalExecServer"));
  // Configure the client.
  static Bootstrap bootstrap;
  // Configure the pipeline factory.
  static LocalExecClientInitializer localExecClientInitializer;
  private Channel channel;

  {
    DetectionUtils.setJunit(true);
  }

  /**
   * Simple constructor
   */
  public LocalExecClientTest() {
  }

  @Test
  public void testClient() throws Exception {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    DetectionUtils.setJunit(true);
    InetAddress addr;
    final byte[] loop = { 127, 0, 0, 1 };
    try {
      addr = InetAddress.getByAddress(loop);
    } catch (final UnknownHostException e) {
      return;
    }
    address = new InetSocketAddress(addr, port);

    // configure the server
    final ServerBootstrap bootstrapServer = new ServerBootstrap();
    WaarpNettyUtil
        .setServerBootstrap(bootstrapServer, workerGroup, workerGroup, 1000);

    // Configure the pipeline factory.
    final LocalExecServerInitializer localExecServerInitializer =
        new LocalExecServerInitializer(LocalExecDefaultResult.MAXWAITPROCESS,
                                       executor);
    bootstrapServer.childHandler(localExecServerInitializer);

    // Bind and start to accept incoming connections only on local address.
    final ChannelFuture future =
        bootstrapServer.bind(new InetSocketAddress(addr, port));

    // Configure the client.
    bootstrap = new Bootstrap();
    WaarpNettyUtil.setBootstrap(bootstrap, workerGroup, 1000);
    // Configure the pipeline factory.
    localExecClientInitializer = new LocalExecClientInitializer();
    bootstrap.handler(localExecClientInitializer);

    // Wait for the server
    future.sync();

    try {
      // Parse options.
      final LocalExecClientTest client = new LocalExecClientTest();
      // run once
      long first = System.currentTimeMillis();
      if (client.connect()) {
        client.runOnce();
        client.disconnect();
      }
      long second = System.currentTimeMillis();
      // print time for one exec
      System.err.println("1=Total time in ms: " + (second - first) + " or " +
                         1 * 1000 / (second - first) + " exec/s");
      System.err.println("Result: " + ok + ':' + ko);
      assertEquals(0, ko);
      ok = 0;
      ko = 0;
      // Now run multiple within one thread
      first = System.currentTimeMillis();
      for (int i = 0; i < nit; i++) {
        if (client.connect()) {
          client.runOnce();
          client.disconnect();
        }
      }
      second = System.currentTimeMillis();
      // print time for one exec
      System.err.println(
          nit + "=Total time in ms: " + (second - first) + " or " +
          nit * 1000 / (second - first) + " exec/s");
      System.err.println("Result: " + ok + ':' + ko);
      assertEquals(0, ko);
      ok = 0;
      ko = 0;
      // Now run multiple within multiple threads
      // Create multiple threads
      final ExecutorService executorService = Executors.newFixedThreadPool(nth);
      first = System.currentTimeMillis();
      // Starts all thread with a default number of execution
      for (int i = 0; i < nth; i++) {
        executorService.submit(new LocalExecClientTest());
      }
      Thread.sleep(100);
      executorService.shutdown();
      while (!executorService.awaitTermination(200, TimeUnit.MILLISECONDS)) {
        Thread.sleep(50);
      }
      second = System.currentTimeMillis();

      // print time for one exec
      System.err.println(
          nit * nth + "=Total time in ms: " + (second - first) + " or " +
          nit * nth * 1000 / (second - first) + " exec/s");
      System.err.println("Result: " + ok + ':' + ko);
      assertEquals(0, ko);
      ok = 0;
      ko = 0;

      // run once
      first = System.currentTimeMillis();
      if (client.connect()) {
        client.runFinal();
        client.disconnect();
      }
      second = System.currentTimeMillis();
      // print time for one exec
      System.err.println("1=Total time in ms: " + (second - first) + " or " +
                         1 * 1000 / (second - first) + " exec/s");
      System.err.println("Result: " + ok + ':' + ko);
      assertEquals(0, ko);
      ok = 0;
      ko = 0;
    } finally {
      future.channel().close();
      // Shut down all thread pools to exit.
      localExecClientInitializer.releaseResources();
      localExecServerInitializer.releaseResources();
      // Shut down all thread pools to exit.
      workerGroup.shutdownGracefully();
      executor.shutdownGracefully();
      localExecClientInitializer.releaseResources();
      LocalExecServerHandler.junitSetNotShutdown();
    }
  }

  /**
   * Connect to the Server
   */
  private boolean connect() {
    // Start the connection attempt.
    final ChannelFuture future = bootstrap.connect(address);

    // Wait until the connection attempt succeeds or fails.
    try {
      channel = future.await().sync().channel();
    } catch (final InterruptedException ignored) {//NOSONAR
    }
    if (!future.isSuccess()) {
      System.err.println("Client Not Connected");
      future.cause().printStackTrace();
      fail("Cannot connect");
      return false;
    }
    return true;
  }

  /**
   * Run method both for not threaded execution and threaded execution
   */
  public void runOnce() {
    // Initialize the command context
    final LocalExecClientHandler clientHandler =
        (LocalExecClientHandler) channel.pipeline().last();
    // Command to execute
    final String line = command + ' ' + atomicInteger.incrementAndGet();
    clientHandler.initExecClient(0, line);
    // Wait for the end of the exec command
    final LocalExecResult localExecResult = clientHandler.waitFor(10000);
    final int status = localExecResult.getStatus();
    if (status < 0) {
      System.err.println(line + " Status: " + status + "\tResult: " +
                         localExecResult.getResult());
      ko++;
    } else {
      ok++;
      result = localExecResult;
    }
  }

  /**
   * Disconnect from the server
   */
  private void disconnect() {
    // Close the connection. Make sure the close operation ends because
    // all I/O operations are asynchronous in Netty.
    final ChannelFuture closeFuture =
        WaarpSslUtility.closingSslChannel(channel);
    WaarpNettyUtil.awaitOrInterrupted(closeFuture, 30000);
  }

  /**
   * Run method for closing Server
   */
  private void runFinal() {
    // Initialize the command context
    final LocalExecClientHandler clientHandler =
        (LocalExecClientHandler) channel.pipeline().last();
    // Command to execute
    clientHandler.initExecClient(-1000, "stop");
    // Wait for the end of the exec command
    final LocalExecResult localExecResult = clientHandler.waitFor(10000);
    final int status = localExecResult.getStatus();
    if (status < 0) {
      System.err.println("Shutdown Status: " + status + "\nResult: " +
                         localExecResult.getResult());
      ok++;
    } else {
      ok++;
      result = localExecResult;
    }
  }

  /**
   * Run method for thread
   */
  @Override
  public void run() {
    if (connect()) {
      for (int i = 0; i < nit; i++) {
        runOnce();
      }
      disconnect();
    }
  }
}
