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

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.waarp.thrift.r66.Action;
import org.waarp.thrift.r66.R66Request;
import org.waarp.thrift.r66.R66Result;
import org.waarp.thrift.r66.R66Service;
import org.waarp.thrift.r66.RequestMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ClientExample implements Runnable {
  private static final boolean isBlocking = true;
  private static final int PORT = 7911;
  private static final int tries = 100000;

  public static void main(String[] args) {
    try {
      TTransport transport = null;
      if (isBlocking) {
        transport = new TSocket("localhost", PORT);
      } else {
        transport = new TFramedTransport(new TSocket("localhost", 7911));
      }
      final TProtocol protocol = new TBinaryProtocol(transport);
      final R66Service.Client client = new R66Service.Client(protocol);
      transport.open();
      final R66Request request = new R66Request(RequestMode.SYNCTRANSFER);
      request.setFromuid("myclient");
      request.setDestuid("mypartner");
      request.setRule("myruletouse");
      request.setFile("pathtomyfile.txt");
      request.setInfo("my info send on the wire");
      request.setMd5(true);

      System.out.println("REQUEST1: " + request);
      R66Result result = client.transferRequestQuery(request);
      System.out.println("RESULT1: " + result);

      final long start = System.currentTimeMillis();
      for (int i = 0; i < tries; i++) {
        result = client.transferRequestQuery(request);
      }
      final long end = System.currentTimeMillis();
      System.out.println(
          "Delay: " + (end - start) + " : " + (tries * 1000) / (end - start));

      request.setMode(RequestMode.INFOREQUEST);
      request.setTid(result.getTid());
      request.setAction(Action.Detail);
      result = client.infoTransferQuery(request);
      System.out.println("RESULT2: " + result);

      System.out.println("Exist: " + client
          .isStillRunning(request.getFromuid(), request.getDestuid(),
                          request.getTid()));

      request.setMode(RequestMode.INFOFILE);
      request.setAction(Action.List);
      result = client.infoTransferQuery(request);
      System.out.println("RESULT3: " + result);

      transport.close();
    } catch (final TTransportException e) {
      e.printStackTrace();
    } catch (final TException e) {
      e.printStackTrace();
    }
    final ExecutorService executorService = Executors.newCachedThreadPool();
    final long start = System.currentTimeMillis();
    final int nb = 5;
    for (int i = 0; i < nb; i++) {
      final ClientExample example = new ClientExample();
      executorService.execute(example);
    }
    executorService.shutdown();
    try {
      executorService.awaitTermination(1000000, TimeUnit.SECONDS);
    } catch (final InterruptedException ignored) {//NOSONAR
    }
    final long end = System.currentTimeMillis();
    System.out.println("Global Delay: " + (end - start) + " : " +
                       (tries * 1000 * nb) / (end - start));
  }

  @Override
  public void run() {
    try {
      TTransport transport = null;
      if (isBlocking) {
        transport = new TSocket("localhost", PORT);
      } else {
        transport = new TFramedTransport(new TSocket("localhost", 7911));
      }
      final TProtocol protocol = new TBinaryProtocol(transport);
      final R66Service.Client client = new R66Service.Client(protocol);
      transport.open();
      final R66Request request = new R66Request(RequestMode.SYNCTRANSFER);
      request.setFromuid("myclient");
      request.setDestuid("mypartner");
      request.setRule("myruletouse");
      request.setFile("pathtomyfile.txt");
      request.setInfo("my info send on the wire");
      request.setMd5(true);

      System.out.println("REQUEST1: " + request);
      R66Result result = client.transferRequestQuery(request);
      System.out.println("RESULT1: " + result);

      final long start = System.currentTimeMillis();
      for (int i = 0; i < tries; i++) {
        result = client.transferRequestQuery(request);
      }
      final long end = System.currentTimeMillis();
      System.out.println(
          "Delay: " + (end - start) + " : " + (tries * 1000) / (end - start));

      transport.close();
    } catch (final TTransportException e) {
      e.printStackTrace();
    } catch (final TException e) {
      e.printStackTrace();
    }
  }

}
