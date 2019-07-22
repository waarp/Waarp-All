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
package org.waarp.thrift.test;

import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.waarp.thrift.r66.R66Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
public class ServerExample implements Runnable {
  /*
   * Benchmark<br> Blocking: JDK6-32 13640/s JDK6-64 7004 JKD7-64 7385<br> NonBlocking: JDK6-32 17125/s JKD7-64
   * 7923<br> Blocking: 5Threads JKD7-64 10436 JDK7-32 6324<br> NonBlocking: 5Threads JKD7-64 12820 JDK7-32
   * 4958<br>
   *
   * New tests: JDK7-32 5Threads: in BinaryProtocol<br> NonBlocking TThreadedSelectorServer: 8908/29561<br>
   * NonBlocking TNonblockingServer: 11285/34148<br> Blocking TThreadPoolServer: 17021/52714<br>
   *
   * Same in TJSONProtocol<br> Blocking TThreadPoolServer: ?/?<br>
   *
   * Same in TCompactProtocol<br> Blocking TThreadPoolServer: ?/49309<br>
   */
  private static boolean serverMode = true;
  private static int PORT = 7911;
  private static TServer server;
  private static Thread runThread;
  private static ExecutorService service;
  private static final Lock lock = new ReentrantLock();

  public static void main(String[] args) throws InterruptedException {
    startServer(true, PORT);
  }

  public static void startServer(boolean mode, int port)
      throws InterruptedException {
    serverMode = mode;
    PORT = port;
    runThread = new Thread(new ServerExample());
    runThread.setDaemon(true);
    service = Executors.newSingleThreadScheduledExecutor();
    service.execute(runThread);
    Thread.sleep(100);
    lock.lock();
    lock.unlock();
  }

  public static void stopServer() {
    if (server != null) {
      server.setShouldStop(true);
      server.stop();
    }
    runThread.interrupt();
    service.shutdownNow();
  }

  @Override
  public void run() {
    lock.lock();
    try {
      TServerTransport serverTransport = null;
      if (serverMode) {
        serverTransport = new TServerSocket(PORT);
      } else {
        serverTransport = new TNonblockingServerSocket(PORT);
      }
      final R66Service.Processor<R66ServiceImpl> processor =
          new R66Service.Processor<R66ServiceImpl>(new R66ServiceImpl());
      server = null;
      if (serverMode) {
        server = new TThreadPoolServer(
            new TThreadPoolServer.Args(serverTransport).processor(processor));
      } else {
        /*
         * server = new TThreadedSelectorServer( new TThreadedSelectorServer.Args((TNonblockingServerTransport)
         * serverTransport) .processor(processor));
         */
        server = new TNonblockingServer(new TNonblockingServer.Args(
            (TNonblockingServerTransport) serverTransport)
                                            .processor(processor));
      }
      System.out.println("Starting server on port " + PORT);
      lock.unlock();
      server.serve();
    } catch (final TTransportException e) {
      e.printStackTrace();
      lock.unlock();
    }
  }
}
