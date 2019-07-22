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
package org.waarp.openr66.thrift;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.thrift.r66.R66Service;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 * Main Thrift server service
 *
 *
 */
public class R66ThriftServerService implements Runnable {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(R66ThriftServerService.class);

  protected int port = 4266;
  protected TServerTransport serverTransport = null;
  protected TServer server = null;
  protected WaarpFuture serviceReady;

  public R66ThriftServerService(WaarpFuture serviceReady, int port) {
    this.serviceReady = serviceReady;
    this.port = port;
  }

  public boolean awaitInitialization() {
    if (serviceReady != null) {
      serviceReady.awaitOrInterruptible();
      return serviceReady.isSuccess();
    }
    return true;
  }

  @Override
  public void run() {
    try {
      logger.warn("Will start Thrift service on port: " + port);
      final byte[] local = { 127, 0, 0, 1 };
      InetAddress addr;
      try {
        addr = InetAddress.getByAddress(local);
      } catch (final UnknownHostException e) {
        try {
          addr = InetAddress.getLocalHost();
        } catch (final UnknownHostException e1) {
          logger.error("Cannot start the Thrift service", e1);
          serviceReady.setFailure(e);
          releaseResources();
          return;
        }
      }
      final InetSocketAddress address = new InetSocketAddress(addr, port);
      serverTransport = new TServerSocket(address);
      final R66Service.Processor<R66EmbeddedServiceImpl> processor =
          new R66Service.Processor<R66EmbeddedServiceImpl>(
              new R66EmbeddedServiceImpl());
      server = new TThreadPoolServer(
          new TThreadPoolServer.Args(serverTransport).processor(processor));
      serviceReady.setSuccess();
      server.serve();
    } catch (final TTransportException e) {
      logger
          .error("An error occurs during initialization of Thrift support", e);
      serviceReady.setFailure(e);
      releaseResources();
    }
  }

  public void releaseResources() {
    if (server != null) {
      logger.debug("Stop Thrift Server");
      server.stop();
    }
    if (serverTransport != null) {
      logger.debug("Stop Thrift Transport");
      serverTransport.close();
    }
    logger.debug("Thrift stopped");
  }
}
