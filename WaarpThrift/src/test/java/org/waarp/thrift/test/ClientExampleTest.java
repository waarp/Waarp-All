/*******************************************************************************
 * This file is part of Waarp Project (named also Waarp or GG).
 *
 *  Copyright (c) 2019, Waarp SAS, and individual contributors by the @author
 *  tags. See the COPYRIGHT.txt in the distribution for a full listing of
 *  individual contributors.
 *
 *  All Waarp Project is free software: you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or (at your
 *  option) any later version.
 *
 *  Waarp is distributed in the hope that it will be useful, but WITHOUT ANY
 *  WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 *  A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with
 *  Waarp . If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.waarp.thrift.test;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.waarp.thrift.r66.Action;
import org.waarp.thrift.r66.R66Request;
import org.waarp.thrift.r66.R66Result;
import org.waarp.thrift.r66.R66Service;
import org.waarp.thrift.r66.RequestMode;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author "Frederic Bregier"
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ClientExampleTest implements Runnable {
  private static final int tries = 1000;
  private static boolean isBlocking = true;
  private static boolean isBlockingServer = true;
  private static int PORT = 7911;

  public static void main(String[] args) throws InterruptedException {
    new ClientExampleTest().test4_Client_Sync();
  }

  @Test
  public void test4_Client_Sync() throws InterruptedException {
    test_Client(true, true);
    PORT++;
  }

  public void test_Client(boolean clientAsync, boolean serverAsync)
      throws InterruptedException {
    System.out
        .println("Start Client: " + clientAsync + " Server: " + serverAsync);
    isBlocking = clientAsync;
    isBlockingServer = serverAsync;
    initServer();
    try {
      TTransport transport = null;
      try {
        if (isBlocking) {
          transport = new TSocket("localhost", PORT);
        } else {
          transport = new TFramedTransport(new TSocket("localhost", PORT));
        }
        TProtocol protocol = new TBinaryProtocol(transport);
        R66Service.Client client =
            new R66Service.Client.Factory().getClient(protocol);
        transport.open();
        R66Request request = new R66Request(RequestMode.SYNCTRANSFER);
        request.setFromuid("myclient");
        request.setDestuid("mypartner");
        request.setRule("myruletouse");
        request.setFile("pathtomyfile.txt");
        request.setInfo("my info send on the wire");
        request.setMd5(true);

        System.out.println("REQUEST1: " + request.toString());
        R66Result result = client.transferRequestQuery(request);
        System.out.println("RESULT1: " + result.toString());

        R66Request request2 = new R66Request(request);
        assertTrue(request.equals(request2));
        assertEquals(0, request.compareTo(request2));
        R66Result result2 = null;
        long start = System.currentTimeMillis();
        for (int i = 0; i < tries; i++) {
          result2 = client.transferRequestQuery(request2);
        }
        long end = System.currentTimeMillis();
        System.out.println("RESULT2: " + result2.toString());
        System.out.println("Delay: " + (end - start) + " : " +
                           ((tries * 1000) / (end - start)));
        // Generic tests
        result2.setTid(result.getTid());
        result2.setStart(result.getStart());
        assertTrue(result.equals(result2));
        assertEquals(0, result.compareTo(result2));
        result2 = new R66Result(result);
        assertTrue(result.equals(result2));
        assertEquals(0, result.compareTo(result2));
        for (R66Result._Fields field : R66Result._Fields.values()) {
          Object obj1 = result.getFieldValue(field);
          Object obj2 =
              result.getFieldValue(
                  R66Result._Fields.findByThriftId(field.getThriftFieldId()));
          assertEquals(obj1, obj2);
          result2.setFieldValue(field, null);
          if (result.isSet(field)) {
            result2.setFieldValue(field, result.getFieldValue(field));
          } else {
            result2.setFieldValue(field, null);
          }
        }
        assertTrue(result.equals(result2));
        for (R66Request._Fields field : R66Request._Fields.values()) {
          Object obj1 = request.getFieldValue(field);
          Object obj2 =
              request.getFieldValue(
                  R66Request._Fields.findByThriftId(field.getThriftFieldId()));
          assertEquals(obj1, obj2);
          request2.setFieldValue(field, null);
          if (request.isSet(field)) {
            request2.setFieldValue(field, request.getFieldValue(field));
          } else {
            request2.setFieldValue(field, null);
          }
        }
        assertTrue(request.equals(request2));


        request.setMode(RequestMode.ASYNCTRANSFER);
        System.out.println("REQUEST1A: " + request.toString());
        result = client.transferRequestQuery(request);
        System.out.println("RESULT1A: " + result.toString());

        R66Request requestCopy = request.deepCopy();
        assertTrue("Should be equals", request.equals(requestCopy));
        System.out.println("REQUEST1B: " + requestCopy.toString());
        R66Result resultCopy = client.transferRequestQuery(requestCopy);
        System.out.println("RESULT1B: " + resultCopy.toString());
        // assertTrue("Should be equals", result.equals(resultCopy));
        requestCopy.clear();
        assertFalse("Should not be equals", request.equals(requestCopy));
        resultCopy.clear();
        assertFalse("Should not be equals", result.equals(resultCopy));

        request.setMode(RequestMode.INFOREQUEST);
        request.setTid(result.getTid());
        request.setAction(Action.Detail);
        result = client.infoTransferQuery(request);
        System.out.println("RESULT2: " + result.toString());

        System.out.println("Exist: "
                           + client.isStillRunning(request.getFromuid(),
                                                   request.getDestuid(),
                                                   request.getTid()));

        request.setAction(Action.Exist);
        result = client.infoTransferQuery(request);
        System.out.println("RESULT2B: " + result.toString());

        request.setAction(Action.Stop);
        result = client.infoTransferQuery(request);
        System.out.println("RESULT2C: " + result.toString());

        request.setAction(Action.Restart);
        request.setBlocksize(1024);
        request.setStart(new Date().toString());
        request.setDelay(new Date().toString());
        request.setNotrace(false);
        result = client.infoTransferQuery(request);
        System.out.println("RESULT2D: " + result.toString());

        request.setAction(Action.Cancel);
        result = client.infoTransferQuery(request);
        System.out.println("RESULT2D: " + result.toString());

        request.setMode(RequestMode.INFOFILE);
        request.setAction(Action.List);
        result = client.infoTransferQuery(request);
        System.out.println("RESULT3: " + result.toString());

        request.setAction(Action.Mlsx);
        result = client.infoTransferQuery(request);
        System.out.println("RESULT3B: " + result.toString());

        request.setMode(RequestMode.INFOFILE);
        request.setAction(Action.List);
        request.setFile("MyDirectory");
        List<String> list = client.infoListQuery(request);
        System.out.println("RESULT4: " + list.toString());

        request.setAction(Action.Mlsx);
        list = client.infoListQuery(request);
        System.out.println("RESULT4B: " + list.toString());

        request.setAction(Action.Detail);
        list = client.infoListQuery(request);
        System.out.println("RESULT4C: " + list.toString());

        request.setAction(Action.Exist);
        list = client.infoListQuery(request);
        System.out.println("RESULT4D: " + list.toString());
      } catch (TTransportException e) {
        e.printStackTrace();
        assertFalse("Should not", true);
      } catch (TException e) {
        e.printStackTrace();
        assertFalse("Should not", true);
      } finally {
        if (transport != null) {
          transport.close();
        }
      }
      ExecutorService executorService = Executors.newCachedThreadPool();
      long start = System.currentTimeMillis();
      int nb = 5;
      for (int i = 0; i < nb; i++) {
        ClientExampleTest example = new ClientExampleTest();
        executorService.execute(example);
      }
      executorService.shutdown();
      try {
        executorService.awaitTermination(1000000, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
      }
      long end = System.currentTimeMillis();
      executorService.shutdownNow();
      System.out.println("Global Delay: " + (end - start) + " : " +
                         ((tries * 1000 * nb) / (end - start)));
    } finally {
      stopServer();
    }
  }

  public void initServer() throws InterruptedException {
    ServerExample.startServer(isBlockingServer, PORT);
    Thread.sleep(1000);
  }

  public void stopServer() throws InterruptedException {
    ServerExample.stopServer();
    Thread.sleep(1000);
  }

  @Test
  public void test2_Client_ASync() throws InterruptedException {
    test_Client(false, false);
    PORT++;
  }

  //@Test Does not complete
  public void test1_Client_Mixed1() throws InterruptedException {
    test_Client(false, true);
    PORT++;
  }

  //@Test Does not complete
  public void test0_Client_Mixed2() throws InterruptedException {
    test_Client(true, false);
    PORT++;
  }

  public void run() {
    TTransport transport = null;
    try {
      if (isBlocking) {
        transport = new TSocket("localhost", PORT);
      } else {
        transport = new TFramedTransport(new TSocket("localhost", PORT));
      }
      TProtocol protocol = new TBinaryProtocol(transport);
      R66Service.Client client =
          new R66Service.Client.Factory().getClient(protocol);
      transport.open();
      R66Request request = new R66Request(RequestMode.SYNCTRANSFER);
      request.setFromuid("myclient");
      request.setDestuid("mypartner");
      request.setRule("myruletouse");
      request.setFile("pathtomyfile.txt");
      request.setInfo("my info send on the wire");
      request.setMd5(true);

      System.out.println("REQUEST1THREAD: " + request.toString());
      R66Result result = client.transferRequestQuery(request);
      System.out.println("RESULT1THREAD: " + result.toString());

      long start = System.currentTimeMillis();
      for (int i = 0; i < tries; i++) {
        result = client.transferRequestQuery(request);
        System.out.print('.');
      }
      long end = System.currentTimeMillis();
      System.out.println();
      System.out.println(
          "Delay: " + (end - start) + " : " + ((tries * 1000) / (end - start)));
    } catch (TTransportException e) {
      e.printStackTrace();
    } catch (TException e) {
      e.printStackTrace();
    } finally {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (transport != null) {
        transport.close();
      }
    }
  }

}
