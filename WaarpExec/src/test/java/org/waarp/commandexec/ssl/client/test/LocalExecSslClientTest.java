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
package org.waarp.commandexec.ssl.client.test;

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
import org.junit.Test;
import org.waarp.commandexec.server.LocalExecServerHandler;
import org.waarp.commandexec.ssl.client.LocalExecSslClientHandler;
import org.waarp.commandexec.ssl.client.LocalExecSslClientInitializer;
import org.waarp.commandexec.ssl.server.LocalExecSslServerInitializer;
import org.waarp.commandexec.utils.LocalExecDefaultResult;
import org.waarp.commandexec.utils.LocalExecResult;
import org.waarp.common.crypto.ssl.WaarpSecureKeyStore;
import org.waarp.common.crypto.ssl.WaarpSslContextFactory;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;
import org.waarp.common.utility.WaarpNettyUtil;
import org.waarp.common.utility.WaarpThreadFactory;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * LocalExecSsl client.
 * <p>
 * This class is an example of client.
 * <p>
 * No client authentication On a bi-core Centrino2 vPro: 5/s in 50 sequential,
 * 29/s in 10 threads with 50
 * sequential<br>
 * With client authentication On a bi-core Centrino2 vPro: 3/s in 50 sequential,
 * 27/s in 10 threads with 50
 * sequential<br>
 * No client authentication On a quad-core i7: 20/s in 50 sequential, 178/s in
 * 10 threads with 50
 * sequential<br>
 * With client authentication On a quad-core i7: 17/s in 50 sequential, 176/s in
 * 10 threads with 50
 * sequential<br>
 */
public class LocalExecSslClientTest extends Thread {
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
  static LocalExecSslClientInitializer localExecClientInitializer;
  private Channel channel;

  {
    DetectionUtils.setJunit(true);
  }

  /**
   * Simple constructor
   */
  public LocalExecSslClientTest() {
  }

  @Test
  public void testSslClient() throws Exception {
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
    // Configure the client.
    bootstrap = new Bootstrap();
    WaarpNettyUtil.setBootstrap(bootstrap, workerGroup, 1000);
    // Configure the pipeline factory.
    // First create the SSL part
    // Load the KeyStore (No certificates)
    final ClassLoader classLoader =
        LocalExecSslClientTest.class.getClassLoader();
    final String keyStoreFilename = "certs/testsslnocert.jks";
    final URL url = classLoader.getResource(keyStoreFilename);
    assertNotNull(url);
    final File file = new File(url.getFile());
    assertTrue("File Should exists", file.exists());
    final String keyStorePasswd = "testsslnocert";
    final String keyPassword = "testalias";
    final WaarpSecureKeyStore waarpSecureKeyStore =
        new WaarpSecureKeyStore(file.getAbsolutePath(), keyStorePasswd,
                                keyPassword);
    final WaarpSecureKeyStore waarpSecureKeyStoreClient =
        new WaarpSecureKeyStore(file.getAbsolutePath(), keyStorePasswd,
                                keyPassword);
    // Include certificates
    final String trustStoreFilename = "certs/testcert.jks";
    final File file2 =
        new File(classLoader.getResource(trustStoreFilename).getFile());
    assertTrue("File2 Should exists", file2.exists());
    final String trustStorePasswd = "testcert";
    waarpSecureKeyStore
        .initTrustStore(file2.getAbsolutePath(), trustStorePasswd, true);

    // configure the server
    final ServerBootstrap bootstrapServer = new ServerBootstrap();
    WaarpNettyUtil.setServerBootstrap(bootstrapServer, workerGroup, 1000);

    // Configure the pipeline factory.
    final WaarpSslContextFactory waarpSslContextFactoryServer =
        new WaarpSslContextFactory(waarpSecureKeyStore, true);
    final LocalExecSslServerInitializer localExecServerInitializer =
        new LocalExecSslServerInitializer(waarpSslContextFactoryServer,
                                          LocalExecDefaultResult.MAXWAITPROCESS,
                                          executor);
    bootstrapServer.childHandler(localExecServerInitializer);

    // Bind and start to accept incoming connections only on local address.
    final ChannelFuture future =
        bootstrapServer.bind(new InetSocketAddress(addr, port));

    // Finalize client configuration
    waarpSecureKeyStoreClient
        .initTrustStore(file2.getAbsolutePath(), trustStorePasswd, false);
    final WaarpSslContextFactory waarpSslContextFactoryClient =
        new WaarpSslContextFactory(waarpSecureKeyStoreClient);

    localExecClientInitializer =
        new LocalExecSslClientInitializer(waarpSslContextFactoryClient);
    bootstrap.handler(localExecClientInitializer);

    // Wait for the server
    future.sync();

    try {
      // Parse options.
      final LocalExecSslClientTest client = new LocalExecSslClientTest();
      // run once
      long first = System.currentTimeMillis();
      if (client.connect()) {
        client.runOnce();
        client.disconnect();
      }
      long second = System.currentTimeMillis();
      // print time for one exec
      SysErrLogger.FAKE_LOGGER.syserr(
          "1=Total time in ms: " + (second - first) + " or " +
          1 * 1000 / (second - first) + " exec/s");
      SysErrLogger.FAKE_LOGGER.syserr("Result: " + ok + ':' + ko);
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
      ok = 0;
      ko = 0;

      // Now run multiple within multiple threads
      // Create multiple threads
      final ExecutorService executorService = Executors.newFixedThreadPool(nth);
      first = System.currentTimeMillis();
      // Starts all thread with a default number of execution
      for (int i = 0; i < nth; i++) {
        executorService.submit(new LocalExecSslClientTest());
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
    channel = WaarpSslUtility.waitforChannelReady(future);
    if (channel == null) {
      System.err.println("Client Not Connected");
      if (future.cause() != null) {
        future.cause().printStackTrace();
      }
      fail("Cannot connect");
      return false;
    }
    return true;
  }

  /**
   * Run method both for not threaded execution and threaded execution
   */
  private void runOnce() {
    // Initialize the command context
    final LocalExecSslClientHandler clientHandler =
        (LocalExecSslClientHandler) channel.pipeline().last();
    // Command to execute
    final String line = command + ' ' + atomicInteger.incrementAndGet();
    clientHandler.initExecClient(0, line);
    // Wait for the end of the exec command
    final LocalExecResult localExecResult = clientHandler.waitFor(10000);
    final int status = localExecResult.getStatus();
    if (status < 0) {
      System.err.println(
          "Status: " + status + "\nResult: " + localExecResult.getResult());
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
    WaarpSslUtility.closingSslChannel(channel);
    WaarpSslUtility.waitForClosingSslChannel(channel, 10000);
  }

  /**
   * Run method for closing Server
   */
  private void runFinal() {
    // Initialize the command context
    final LocalExecSslClientHandler clientHandler =
        (LocalExecSslClientHandler) channel.pipeline().last();
    // Command to execute
    clientHandler.initExecClient(-1000, "stop");
    // Wait for the end of the exec command
    final LocalExecResult localExecResult = clientHandler.waitFor(10000);
    final int status = localExecResult.getStatus();
    if (status < 0) {
      System.err.println(
          "Status: " + status + "\nResult: " + localExecResult.getResult());
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
