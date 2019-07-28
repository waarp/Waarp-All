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
package org.waarp.openr66.protocol.http.rest.test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpMethod;
import org.joda.time.DateTime;
import org.waarp.common.crypto.Des;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.database.data.AbstractDbData;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.exception.CryptoException;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.rest.DataModelRestMethodHandler;
import org.waarp.gateway.kernel.rest.RestArgument;
import org.waarp.gateway.kernel.rest.RestConfiguration;
import org.waarp.gateway.kernel.rest.RestMethodHandler;
import org.waarp.gateway.kernel.rest.client.HttpRestClientSimpleResponseHandler;
import org.waarp.gateway.kernel.rest.client.RestFuture;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.task.test.TestExecJavaTask;
import org.waarp.openr66.database.DbConstantR66;
import org.waarp.openr66.database.data.DbConfiguration;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbHostConfiguration;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.database.data.DbTaskRunner.Columns;
import org.waarp.openr66.database.data.DbTaskRunner.TASKSTEP;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler.RESTHANDLERS;
import org.waarp.openr66.protocol.http.rest.client.HttpRestR66Client;
import org.waarp.openr66.protocol.http.rest.handler.DbConfigurationR66RestMethodHandler;
import org.waarp.openr66.protocol.http.rest.handler.DbHostAuthR66RestMethodHandler;
import org.waarp.openr66.protocol.http.rest.handler.DbHostConfigurationR66RestMethodHandler;
import org.waarp.openr66.protocol.http.rest.handler.DbRuleR66RestMethodHandler;
import org.waarp.openr66.protocol.http.rest.handler.DbTaskRunnerR66RestMethodHandler;
import org.waarp.openr66.protocol.http.restv2.RestServiceInitializer;
import org.waarp.openr66.protocol.localhandler.packet.InformationPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.json.BandwidthJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.BusinessRequestJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ConfigExportJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.InformationJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.JsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.LogJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ShutdownOrBlockJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.TransferRequestJsonPacket;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 *
 */
public class HttpTestRestR66Client implements Runnable {
  public static String keydesfilename = "/opt/R66/certs/test-key.des";
  private static final String baseURI = "/";
  public static String userAuthent = "admin2";
  private static String keyAuthent = "test";
  private static final long limit = 10000000;
  private static final long delaylimit = 5000;
  private static String hostid = "hostZZ";
  private static final String address = "10.10.10.10";
  private static final String hostkey = "ABCDEFGH";
  private static final String business =
      "<business><businessid>hostas</businessid></business>";
  private static final String roles =
      "<roles><role><roleid>hostas</roleid><roleset>FULLADMIN</roleset></role></roles>";
  private static final String aliases =
      "<aliases><alias><realid>hostZZ</realid><aliasid>hostZZ2</aliasid></alias></aliases>";
  private static final String others = "<root><version>2.4.25</version></root>";
  private static final String idRule = "ruleZZ";
  private static final String ids =
      "<hostids><hostid>hosta</hostid><hostid>hostZZ</hostid></hostids>";
  private static final String tasks =
      "<tasks><task><type>LOG</type><path>log</path><delay>0</delay><comment></comment></task></tasks>";
  public static int NB = 2;
  public static int NBPERTHREAD = 10;
  public static boolean DEBUG;
  public static AtomicLong count = new AtomicLong();
  public static int rank = 1;
  private static WaarpLogger logger;
  private static HttpRestR66Client clientHelper;
  private static String host = "127.0.0.1";
  private static boolean isStatus;

  /**
   * @param args
   */
  @SuppressWarnings("unused")
  public static void main(String[] args) {
    if (args.length > 2) {
      WaarpLoggerFactory
          .setDefaultFactory(new WaarpSlf4JLoggerFactory(WaarpLogLevel.DEBUG));
    } else {
      WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
    }
    logger = WaarpLoggerFactory.getLogger(HttpTestRestR66Client.class);
    Configuration.configuration.setHostId(hostid);
    // If RestV2 started
    RestServiceInitializer.stopRestService();
    if (args.length > 0) {
      NB = Integer.parseInt(args[0]);
      if (Configuration.configuration.getClientThread() < NB) {
        Configuration.configuration.setClientThread(NB + 1);
      }
      if (args.length > 1) {
        NBPERTHREAD = Integer.parseInt(args[1]);
      }
    }
    if (NB == 1 && NBPERTHREAD == 1) {
      DEBUG = true;
    }

    try {
      HttpTestR66PseudoMain.config =
          HttpTestR66PseudoMain.getTestConfiguration();
    } catch (final CryptoException e2) {
      e2.printStackTrace();
      fail("Cant connect");
      return;
    } catch (final IOException e2) {
      e2.printStackTrace();
      fail("Cant connect");
      return;
    }
    if (HttpTestR66PseudoMain.config.getRestAddress() != null) {
      host = HttpTestR66PseudoMain.config.getRestAddress();
    }
    final String filename = keydesfilename;
    Configuration.configuration.setCryptoFile(filename);
    final File keyfile = new File(filename);
    final Des des = new Des();
    try {
      des.setSecretKey(keyfile);
    } catch (final CryptoException e) {
      logger.error("Unable to load CryptoKey from Config file");
      fail("Cant Load");
      return;
    } catch (final IOException e) {
      logger.error("Unable to load CryptoKey from Config file");
      fail("Cant Load");
      return;
    }
    Configuration.configuration.setCryptoKey(des);
    Configuration.configuration
        .startJunitRestSupport(HttpTestR66PseudoMain.config);
    // Configure the client.
    clientHelper =
        new HttpRestR66Client(baseURI, new HttpTestRestClientInitializer(null),
                              Configuration.configuration.getClientThread(),
                              Configuration.configuration.getTimeoutCon());
    logger.warn("ClientHelper created");
    try {
      try {
        final long start = System.currentTimeMillis();
        for (int i = 0; i < NBPERTHREAD; i++) {
          options(null);
        }
        final long stop = System.currentTimeMillis();
        final long diff = stop - start == 0? 1 : stop - start;
        logger.warn(
            "Options: " + count.get() * 1000 / diff + " req/s NbPerThread: " +
            NBPERTHREAD + '=' + count.get());
        assertTrue("Options", count.get() > 0);
      } catch (final HttpInvalidAuthenticationException e) {
        e.printStackTrace();
        fail("Cant connect");
      }
      count.set(0);
      long start = System.currentTimeMillis();
      rank = 1;
      RESTHANDLERS handler1 = RESTHANDLERS.DbConfiguration;
      try {
        multiDataRequests(handler1);
      } catch (final HttpInvalidAuthenticationException e) {
        e.printStackTrace();
        fail("Cant connect");
      }
      handler1 = RESTHANDLERS.DbHostAuth;
      try {
        multiDataRequests(handler1);
      } catch (final HttpInvalidAuthenticationException e) {
        e.printStackTrace();
        fail("Cant connect");
      }
      handler1 = RESTHANDLERS.DbHostConfiguration;
      try {
        multiDataRequests(handler1);
      } catch (final HttpInvalidAuthenticationException e) {
        e.printStackTrace();
        fail("Cant connect");
      }
      handler1 = RESTHANDLERS.DbRule;
      try {
        multiDataRequests(handler1);
      } catch (final HttpInvalidAuthenticationException e) {
        e.printStackTrace();
        fail("Cant connect");
      }
      handler1 = RESTHANDLERS.DbTaskRunner;
      try {
        multiDataRequests(handler1);
      } catch (final HttpInvalidAuthenticationException e) {
        e.printStackTrace();
        fail("Cant connect");
      }
      long stop = System.currentTimeMillis();
      long diff = stop - start == 0? 1 : stop - start;
      logger.warn(
          "Create: " + count.get() * 1000 / diff + " req/s " + NBPERTHREAD +
          "=?" + count.get());
      assertTrue("Create", count.get() > 0);
      count.set(0);
      start = System.currentTimeMillis();
      for (final RestMethodHandler methodHandler : HttpTestR66PseudoMain.config.restHashMap
          .values()) {
        if (methodHandler instanceof DataModelRestMethodHandler<?>) {
          final RESTHANDLERS handler =
              RESTHANDLERS.getRESTHANDLER(methodHandler.getPath());
          try {
            realAllData(handler);
          } catch (final HttpInvalidAuthenticationException e) {
            e.printStackTrace();
            fail("Cant connect");
          }
        }
      }
      stop = System.currentTimeMillis();
      diff = stop - start == 0? 1 : stop - start;
      logger.warn(
          "ReadAll: " + count.get() * 1000 / diff + " req/s " + NBPERTHREAD +
          "=?" + count.get());
      assertTrue("ReadAll", count.get() > 0);

      count.set(0);
      start = System.currentTimeMillis();
      for (int i = 0; i < NBPERTHREAD; i++) {
        try {
          multiDataRequests(RESTHANDLERS.DbTaskRunner);
        } catch (final HttpInvalidAuthenticationException e) {
          e.printStackTrace();
          fail("Cant connect");
        }
      }
      stop = System.currentTimeMillis();
      diff = stop - start == 0? 1 : stop - start;
      logger.warn("CreateMultiple: " + count.get() * 1000 / diff + " req/s " +
                  NBPERTHREAD + "=?" + count.get());
      assertTrue("CreateMultiple", count.get() > 0);

      count.set(0);
      start = System.currentTimeMillis();
      launchThreads();
      stop = System.currentTimeMillis();
      diff = stop - start == 0? 1 : stop - start;
      logger.warn(
          "CreateMultipleThread: " + count.get() * 1000 / diff + " req/s " +
          NBPERTHREAD * NB + "=?" + count.get());
      assertTrue("CreateMultipleThread", count.get() > 0);

      // Set usefull item first
      if (RestConfiguration.CRUD.UPDATE.isValid(HttpTestR66PseudoMain.config
                                                    .getResthandlersCrud()[RESTHANDLERS.DbHostConfiguration
          .ordinal()])) {
        rank = 2;

        String key = null, value = null;
        Channel channel = clientHelper
            .getChannel(host, HttpTestR66PseudoMain.config.getRestPort());
        if (channel != null) {
          String buz = null;
          if (HttpTestR66PseudoMain.config.isRestAuthenticated()) {
            key = userAuthent;
            value = keyAuthent;
            // Need business
            buz =
                "<business><businessid>hostas</businessid><businessid>hosta2</businessid><businessid>hostas2</businessid>" +
                "<businessid>hosta</businessid><businessid>test</businessid><businessid>tests</businessid>" +
                "<businessid>" + userAuthent + "</businessid></business>";
          } else {
            // Need business
            buz =
                "<business><businessid>hostas</businessid><businessid>hosta2</businessid><businessid>hostas2</businessid>" +
                "<businessid>hosta</businessid><businessid>test</businessid><businessid>tests</businessid>" +
                "<businessid>monadmin</businessid></business>";
          }
          final ObjectNode node = JsonHandler.createObjectNode();
          node.put(DbHostConfiguration.Columns.BUSINESS.name(), buz);
          logger.warn("Send query: " + RESTHANDLERS.DbHostConfiguration.uri);
          final RestFuture future = clientHelper
              .sendQuery(HttpTestR66PseudoMain.config, channel, HttpMethod.PUT,
                         host, RESTHANDLERS.DbHostConfiguration.uri + "/hosta",
                         key, value, null, JsonHandler.writeAsString(node));
          future.awaitOrInterruptible();
          WaarpSslUtility.closingSslChannel(channel);
          // assertTrue("Action should be ok", future.isSuccess());
        }
        // Need Hostzz
        channel = clientHelper
            .getChannel(host, HttpTestR66PseudoMain.config.getRestPort());
        if (channel != null) {
          AbstractDbData dbData;
          dbData = new DbHostAuth(hostid + rank, address,
                                  HttpTestR66PseudoMain.config.getRestPort(),
                                  false, hostkey.getBytes(), true, false);
          logger.warn("Send query: " + RESTHANDLERS.DbHostAuth.uri);
          final RestFuture future = clientHelper
              .sendQuery(HttpTestR66PseudoMain.config, channel, HttpMethod.POST,
                         host, RESTHANDLERS.DbHostAuth.uri, key, value, null,
                         dbData.asJson());
          future.awaitOrInterruptible();
          WaarpSslUtility.closingSslChannel(channel);
          logger.warn("Sent query: " + RESTHANDLERS.DbHostAuth.uri);
          assertTrue("Action should be ok", future.isSuccess());
        }
      }

      // Other Command as actions
      count.set(0);
      start = System.currentTimeMillis();
      for (final RestMethodHandler methodHandler : HttpTestR66PseudoMain.config.restHashMap
          .values()) {
        if (methodHandler instanceof DataModelRestMethodHandler<?>) {
          final RESTHANDLERS handler =
              RESTHANDLERS.getRESTHANDLER(methodHandler.getPath());
          logger.warn("Send query: " + handler);
          try {
            action(handler);
          } catch (final HttpInvalidAuthenticationException e) {
            e.printStackTrace();
            fail("Cant connect");
          }
        }
      }
      getStatus();
      stop = System.currentTimeMillis();
      diff = stop - start == 0? 1 : stop - start;
      logger.warn("Commands: " + count.get() * 1000 / diff + " req/s " +
                  NBPERTHREAD * NB + "=?" + count.get());
      assertTrue("Commands", count.get() > 0);

      count.set(0);
      start = System.currentTimeMillis();
      logger.warn("Request status");
      for (int i = 0; i < NBPERTHREAD; i++) {
        getStatus();
      }
      stop = System.currentTimeMillis();
      diff = stop - start == 0? 1 : stop - start;
      logger.warn(
          "GetStatusMultiple: " + count.get() * 1000 / diff + " req/s " +
          NBPERTHREAD + "=?" + count.get());
      assertTrue("GetStatusMultiple", count.get() > 0);

      count.set(0);
      isStatus = true;
      start = System.currentTimeMillis();
      launchThreads();
      stop = System.currentTimeMillis();
      diff = stop - start == 0? 1 : stop - start;
      logger.warn(
          "GetStatusMultipleThread: " + count.get() * 1000 / diff + " req/s " +
          NBPERTHREAD * NB + "=?" + count.get());
      assertTrue("GetStatusMultipleThread", count.get() > 0);

      count.set(0);
      start = System.currentTimeMillis();
      if (true) {
        for (final RESTHANDLERS handler : HttpRestR66Handler.RESTHANDLERS
            .values()) {
          try {
            deleteData(handler);
          } catch (final HttpInvalidAuthenticationException e) {
            e.printStackTrace();
            fail("Cant connect");
          }
        }
      }
      stop = System.currentTimeMillis();
      diff = stop - start == 0? 1 : stop - start;
      if (true) {
        logger.warn(
            "Delete: " + count.get() * 1000 / diff + " req/s " + NBPERTHREAD +
            "=?" + count.get());
        assertTrue("Delete", count.get() > 0);
      }

      // Clean
      if (RestConfiguration.CRUD.UPDATE.isValid(HttpTestR66PseudoMain.config
                                                    .getResthandlersCrud()[RESTHANDLERS.DbHostConfiguration
          .ordinal()])) {
        String key = null, value = null;
        Channel channel = clientHelper
            .getChannel(host, HttpTestR66PseudoMain.config.getRestPort());
        if (channel != null) {
          if (HttpTestR66PseudoMain.config.isRestAuthenticated()) {
            key = userAuthent;
            value = keyAuthent;
          }
          // Reset business
          final String buz =
              "<business><businessid>hostas</businessid><businessid>hosta2</businessid><businessid>hostas2</businessid>" +
              "<businessid>hosta</businessid><businessid>test</businessid><businessid>tests</businessid></business>";
          final ObjectNode node = JsonHandler.createObjectNode();
          node.put(DbHostConfiguration.Columns.BUSINESS.name(), buz);
          logger.warn("Send query: " + RESTHANDLERS.DbHostConfiguration.uri);
          final RestFuture future = clientHelper
              .sendQuery(HttpTestR66PseudoMain.config, channel, HttpMethod.PUT,
                         host, RESTHANDLERS.DbHostConfiguration.uri + "/hosta",
                         key, value, null, JsonHandler.writeAsString(node));
          future.awaitOrInterruptible();
          WaarpSslUtility.closingSslChannel(channel);
          // assertTrue("Action should be ok", future.isSuccess());
        }
        // Remove Hostzz
        channel = clientHelper
            .getChannel(host, HttpTestR66PseudoMain.config.getRestPort());
        if (channel != null) {
          RestFuture future = null;
          try {
            future = deleteData(channel, RESTHANDLERS.DbHostAuth);
            future.awaitOrInterruptible();
          } catch (final HttpInvalidAuthenticationException ignored) {
          }
          WaarpSslUtility.closingSslChannel(channel);
          // assertTrue("Action should be ok", future.isSuccess());
        }
        // Shutdown
        channel = clientHelper
            .getChannel(host, HttpTestR66PseudoMain.config.getRestPort());
        if (channel != null) {
          final ShutdownOrBlockJsonPacket shutd =
              new ShutdownOrBlockJsonPacket();
          shutd.setRestartOrBlock(false);
          shutd.setShutdownOrBlock(true);
          shutd.setRequestUserPacket(LocalPacketFactory.SHUTDOWNPACKET);
          final String pwd = "pwdhttp";
          final byte[] bpwd = FilesystemBasedDigest
              .passwdCrypt(pwd.getBytes(WaarpStringUtils.UTF8));
          shutd.setKey(bpwd);
          logger.warn("Send query: " + RESTHANDLERS.Server.uri);
          final RestFuture future =
              action(channel, HttpMethod.PUT, RESTHANDLERS.Server.uri, shutd);
          future.awaitOrInterruptible();
          WaarpSslUtility.closingSslChannel(channel);
          assertTrue("Action should be ok", future.isSuccess());
        }
      }
      try {
        Thread.sleep(100);
      } catch (final InterruptedException e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
    } finally {
      logger.debug("ClientHelper closing");
      // clientHelper.closeAll();
      logger.warn("ClientHelper closed");
    }
  }

  public static void launchThreads() {
    // init thread model
    final ExecutorService pool = Executors.newFixedThreadPool(NB);
    final HttpTestRestR66Client[] clients = new HttpTestRestR66Client[NB];
    for (int i = 0; i < NB; i++) {
      clients[i] = new HttpTestRestR66Client();
    }
    for (int i = 0; i < NB; i++) {
      pool.execute(clients[i]);
    }
    pool.shutdown();
    try {
      while (!pool.awaitTermination(100000, TimeUnit.SECONDS)) {

      }
    } catch (final InterruptedException ignored) {
    }
  }

  public static void options(String uri)
      throws HttpInvalidAuthenticationException {
    final Channel channel = clientHelper
        .getChannel(host, HttpTestR66PseudoMain.config.getRestPort());
    if (channel == null) {
      logger.warn("Cannot connect to: " + host + ':' +
                  HttpTestR66PseudoMain.config.getRestPort());
      fail("Cant connect");
      return;
    }
    final RestFuture future = options(channel, uri);
    future.awaitOrInterruptible();
    logger.debug("Closing Channel");
    WaarpSslUtility.closingSslChannel(channel);
    assertTrue("Action should be ok", future.isSuccess());
    logger.debug("Channel closed");
  }

  protected static RestFuture options(Channel channel, String uri)
      throws HttpInvalidAuthenticationException {
    logger.debug("Send query");
    String key = null, value = null;
    if (HttpTestR66PseudoMain.config.isRestAuthenticated()) {
      key = userAuthent;
      value = keyAuthent;
    }
    final RestFuture future = clientHelper
        .sendQuery(HttpTestR66PseudoMain.config, channel, HttpMethod.OPTIONS,
                   host, uri, key, value, null, null);
    logger.debug("Query sent");
    return future;
  }

  protected static void realAllData(RESTHANDLERS data)
      throws HttpInvalidAuthenticationException {
    if (!RestConfiguration.CRUD.READ.isValid(
        HttpTestR66PseudoMain.config.getResthandlersCrud()[data.ordinal()])) {
      logger.warn("Not allow to READ");
      return;
    }
    final Channel channel = clientHelper
        .getChannel(host, HttpTestR66PseudoMain.config.getRestPort());
    if (channel == null) {
      logger.warn("Cannot connect to: " + host + ':' +
                  HttpTestR66PseudoMain.config.getRestPort());
      fail("Cant connect");
      return;
    }
    final RestFuture future = realAllData(channel, data);
    future.awaitOrInterruptible();
    WaarpSslUtility.closingSslChannel(channel);
    assertTrue("Action should be ok", future.isSuccess());
    logger.debug("Channel closed");
  }

  protected static RestFuture realAllData(Channel channel, RESTHANDLERS data)
      throws HttpInvalidAuthenticationException {
    logger.debug("Send query");
    String key = null, value = null;
    if (HttpTestR66PseudoMain.config.isRestAuthenticated()) {
      key = userAuthent;
      value = keyAuthent;
    }
    final ObjectNode answer = JsonHandler.createObjectNode();
    switch (data) {
      case DbConfiguration:
        answer.put(
            DbConfigurationR66RestMethodHandler.FILTER_ARGS.BANDWIDTH.name(),
            -1);
        break;
      case DbHostAuth:
        answer
            .put(DbHostAuthR66RestMethodHandler.FILTER_ARGS.ISSSL.name(), true);
        answer.put(DbHostAuthR66RestMethodHandler.FILTER_ARGS.ISACTIVE.name(),
                   true);
        break;
      case DbHostConfiguration:
        answer.put(
            DbHostConfigurationR66RestMethodHandler.FILTER_ARGS.BUSINESS.name(),
            "hosta");
        break;
      case DbRule:
        answer.put(DbRuleR66RestMethodHandler.FILTER_ARGS.MODETRANS.name(), 2);
        break;
      case DbTaskRunner:
        answer
            .put(DbTaskRunnerR66RestMethodHandler.FILTER_ARGS.STOPTRANS.name(),
                 new DateTime().toString());
        answer.put(Columns.OWNERREQ.name(),
                   Configuration.configuration.getHostId());
        break;
      default:
        final RestFuture restFuture =
            channel.attr(HttpRestClientSimpleResponseHandler.RESTARGUMENT)
                   .get();
        restFuture.cancel();
        WaarpSslUtility.closingSslChannel(channel);
        return restFuture;
    }
    final RestFuture future = clientHelper
        .sendQuery(HttpTestR66PseudoMain.config, channel, HttpMethod.GET, host,
                   data.uri, key, value, null,
                   JsonHandler.writeAsString(answer));
    logger.debug("Query sent");
    return future;
  }

  protected static void deleteData(RESTHANDLERS data)
      throws HttpInvalidAuthenticationException {
    if (!RestConfiguration.CRUD.DELETE.isValid(
        HttpTestR66PseudoMain.config.getResthandlersCrud()[data.ordinal()])) {
      logger.warn("Not allow to DELETE");
      return;
    }
    final Channel channel = clientHelper
        .getChannel(host, HttpTestR66PseudoMain.config.getRestPort());
    if (channel == null) {
      logger.warn("Cannot connect to: " + host + ':' +
                  HttpTestR66PseudoMain.config.getRestPort());
      fail("Cant connect");
      return;
    }
    final RestFuture future = deleteData(channel, data);
    future.awaitOrInterruptible();
    WaarpSslUtility.closingSslChannel(channel);
    if (!future.isSuccess()) {
      logger.warn("Can't DELETE {}", data);
    }
    // assertTrue("Action should be ok", future.isSuccess());
    logger.debug("Channel closed");
  }

  protected static RestFuture deleteData(Channel channel, RESTHANDLERS data)
      throws HttpInvalidAuthenticationException {
    logger.debug("Send query");
    final AbstractDbData dbData = getItem(data);
    if (dbData == null) {
      final RestFuture future =
          channel.attr(HttpRestClientSimpleResponseHandler.RESTARGUMENT).get();
      future.cancel();
      WaarpSslUtility.closingSslChannel(channel);
      return future;
    }
    final String item = dbData.getJson().path(
        clientHelper.getPrimaryPropertyName(dbData.getClass().getSimpleName()))
                              .asText();
    String key = null, value = null;
    if (HttpTestR66PseudoMain.config.isRestAuthenticated()) {
      key = userAuthent;
      value = keyAuthent;
    }
    Map<String, String> args = null;
    if (dbData instanceof DbTaskRunner) {
      args = new HashMap<String, String>();
      args.put(DbTaskRunner.Columns.REQUESTER.name(), hostid + rank);
      args.put(DbTaskRunner.Columns.REQUESTED.name(), hostid + rank);
      args.put(Columns.OWNERREQ.name(),
               Configuration.configuration.getHostId());
    }
    final RestFuture future = clientHelper
        .sendQuery(HttpTestR66PseudoMain.config, channel, HttpMethod.DELETE,
                   host, data.uri + '/' + item, key, value, args, null);
    logger.debug("Query sent");
    return future;
  }

  protected static RestFuture readData(Channel channel, RestArgument arg)
      throws HttpInvalidAuthenticationException {
    logger.debug("Send query");
    final String base = arg.getBaseUri();
    final String item = clientHelper.getPrimaryProperty(arg);
    String key = null, value = null;
    if (HttpTestR66PseudoMain.config.isRestAuthenticated()) {
      key = userAuthent;
      value = keyAuthent;
    }
    Map<String, String> args = null;
    if (base.equals(HttpRestR66Handler.RESTHANDLERS.DbTaskRunner.uri)) {
      args = new HashMap<String, String>();
      args.put(DbTaskRunner.Columns.REQUESTER.name(), hostid + rank);
      args.put(DbTaskRunner.Columns.REQUESTED.name(), hostid + rank);
      args.put(Columns.OWNERREQ.name(),
               Configuration.configuration.getHostId());
    }
    final RestFuture future = clientHelper
        .sendQuery(HttpTestR66PseudoMain.config, channel, HttpMethod.GET, host,
                   base + '/' + item, key, value, args, null);
    logger.debug("Query sent");
    return future;
  }

  protected static RestFuture updateData(Channel channel, RestArgument arg)
      throws HttpInvalidAuthenticationException {
    logger.debug("Send query");
    final ObjectNode answer = arg.getAnswer();
    final String base = arg.getBaseUri();
    final String item = clientHelper.getPrimaryProperty(arg);
    final RESTHANDLERS dbdata = clientHelper.getRestHandler(arg);
    String key = null, value = null;
    if (HttpTestR66PseudoMain.config.isRestAuthenticated()) {
      key = userAuthent;
      value = keyAuthent;
    }
    Map<String, String> args = null;
    if (base.equals(HttpRestR66Handler.RESTHANDLERS.DbTaskRunner.uri)) {
      args = new HashMap<String, String>();
      args.put(DbTaskRunner.Columns.REQUESTER.name(), hostid + rank);
      args.put(DbTaskRunner.Columns.REQUESTED.name(), hostid + rank);
      args.put(Columns.OWNERREQ.name(),
               Configuration.configuration.getHostId());
    }
    // update
    answer.removeAll();
    switch (dbdata) {
      case DbConfiguration:
        answer.put(DbConfiguration.Columns.READGLOBALLIMIT.name(), 0);
        break;
      case DbHostAuth:
        answer.put(DbHostAuth.Columns.PORT.name(), 100);
        break;
      case DbHostConfiguration:
        answer.put(DbHostConfiguration.Columns.OTHERS.name(), "");
        break;
      case DbRule:
        answer.put(DbRule.Columns.MODETRANS.name(), 4);
        break;
      case DbTaskRunner:
        answer.put(DbTaskRunner.Columns.FILEINFO.name(), "New Fileinfo");
        break;
      default:
        final RestFuture future =
            channel.attr(HttpRestClientSimpleResponseHandler.RESTARGUMENT)
                   .get();
        future.cancel();
        WaarpSslUtility.closingSslChannel(channel);
        return future;
    }
    final RestFuture future = clientHelper
        .sendQuery(HttpTestR66PseudoMain.config, channel, HttpMethod.PUT, host,
                   base + '/' + item, key, value, args,
                   JsonHandler.writeAsString(answer));
    logger.debug("Query sent");
    return future;
  }

  protected static RestFuture deleteData(Channel channel, RestArgument arg)
      throws HttpInvalidAuthenticationException {
    logger.debug("Send query");
    final String base = arg.getBaseUri();
    final String item = clientHelper.getPrimaryProperty(arg);
    String key = null, value = null;
    if (HttpTestR66PseudoMain.config.isRestAuthenticated()) {
      key = userAuthent;
      value = keyAuthent;
    }
    Map<String, String> args = null;
    if (base.equals(HttpRestR66Handler.RESTHANDLERS.DbTaskRunner.uri)) {
      args = new HashMap<String, String>();
      args.put(DbTaskRunner.Columns.REQUESTER.name(), hostid + rank);
      args.put(DbTaskRunner.Columns.REQUESTED.name(), hostid + rank);
      args.put(Columns.OWNERREQ.name(),
               Configuration.configuration.getHostId());
    }
    final RestFuture future = clientHelper
        .sendQuery(HttpTestR66PseudoMain.config, channel, HttpMethod.DELETE,
                   host, base + '/' + item, key, value, args, null);
    logger.debug("Query sent");
    return future;
  }

  protected static RestFuture deleteData(Channel channel, String reqd,
                                         String reqr, long specid)
      throws HttpInvalidAuthenticationException {
    logger.debug("Send query");
    String key = null, value = null;
    if (HttpTestR66PseudoMain.config.isRestAuthenticated()) {
      key = userAuthent;
      value = keyAuthent;
    }
    final Map<String, String> args = new HashMap<String, String>();
    args.put(DbTaskRunner.Columns.REQUESTER.name(), reqr);
    args.put(DbTaskRunner.Columns.REQUESTED.name(), reqd);
    final RestFuture future = clientHelper
        .sendQuery(HttpTestR66PseudoMain.config, channel, HttpMethod.DELETE,
                   host, RESTHANDLERS.DbTaskRunner.uri + '/' + specid, key,
                   value, args, null);
    logger.debug("Query sent");
    return future;
  }

  protected static void action(RESTHANDLERS data)
      throws HttpInvalidAuthenticationException {
    if (!RestConfiguration.CRUD.READ.isValid(
        HttpTestR66PseudoMain.config.getResthandlersCrud()[data.ordinal()])) {
      logger.warn("Not allow to READ");
      return;
    }
    final Channel channel = clientHelper
        .getChannel(host, HttpTestR66PseudoMain.config.getRestPort());
    if (channel == null) {
      logger.warn("Cannot connect to: " + host + ':' +
                  HttpTestR66PseudoMain.config.getRestPort());
      fail("Cant connect");
      return;
    }
    final RestFuture future = action(channel, data);
    future.awaitOrInterruptible();
    WaarpSslUtility.closingSslChannel(channel);
    // assertTrue("Action should be ok", future.isSuccess());
    logger.debug("Channel closed");
  }

  protected static RestFuture action(Channel channel, RESTHANDLERS data)
      throws HttpInvalidAuthenticationException {
    logger.warn("Send query: " + data);
    JsonPacket packet = null;
    HttpMethod method = null;
    switch (data) {
      case Bandwidth: {
        final BandwidthJsonPacket node = new BandwidthJsonPacket();
        node.setReadglobal(0);
        node.setReadsession(0);
        node.setSetter(true);
        node.setRequestUserPacket();
        packet = node;
        method = HttpMethod.PUT;
        break;
      }
      case Business: {
        final BusinessRequestJsonPacket node = new BusinessRequestJsonPacket();
        node.setClassName(TestExecJavaTask.class.getName());
        node.setArguments("business 100 other arguments 0");
        node.setRequestUserPacket();
        node.setToApplied(true);
        packet = node;
        method = HttpMethod.GET;
        break;
      }
      case Config: {
        final ConfigExportJsonPacket node = new ConfigExportJsonPacket();
        node.setHost(true);
        node.setRule(true);
        node.setBusiness(true);
        node.setAlias(true);
        node.setRoles(true);
        node.setRequestUserPacket();
        packet = node;
        method = HttpMethod.GET;
        break;
      }
      case Information: {
        packet = new InformationJsonPacket(
            (byte) InformationPacket.ASKENUM.ASKLIST.ordinal(), "rule4",
            "test*");
        method = HttpMethod.GET;
        break;
      }
      case Log: {
        final LogJsonPacket node = new LogJsonPacket();
        node.setStop(new Date());
        node.setRequestUserPacket();
        packet = node;
        method = HttpMethod.GET;
        break;
      }
      case Server: {
        final ShutdownOrBlockJsonPacket node = new ShutdownOrBlockJsonPacket();
        node.setRestartOrBlock(true);
        node.setShutdownOrBlock(false);
        final String pwd = "pwdhttp";
        final byte[] bpwd = FilesystemBasedDigest
            .passwdCrypt(pwd.getBytes(WaarpStringUtils.UTF8));
        node.setKey(bpwd);
        node.setRequestUserPacket(LocalPacketFactory.BLOCKREQUESTPACKET);
        packet = node;
        method = HttpMethod.PUT;
        break;
      }
      case Control: {
        final TransferRequestJsonPacket node = new TransferRequestJsonPacket();
        node.setRequestUserPacket();
        node.setRulename("rule4");
        node.setRank(0);
        node.setBlocksize(65536);
        node.setFileInformation("file info");
        node.setFilename("filename");
        node.setMode(2);
        node.setSpecialId(DbConstantR66.ILLEGALVALUE);
        node.setRequested(hostid);
        node.setStart(new Date());
        node.setOriginalSize(123L);
        node.setRequestUserPacket();
        packet = node;
        method = HttpMethod.POST;
        break;
      }
      default:
        break;
    }
    return action(channel, method, data.uri, packet);
  }

  protected static RestFuture action(Channel channel, HttpMethod method,
                                     String uri, JsonPacket packet) {
    if (packet == null) {
      final RestFuture future =
          channel.attr(HttpRestClientSimpleResponseHandler.RESTARGUMENT).get();
      future.cancel();
      WaarpSslUtility.closingSslChannel(channel);
      return future;
    }
    String key = null, value = null;
    if (HttpTestR66PseudoMain.config.isRestAuthenticated()) {
      key = userAuthent;
      value = keyAuthent;
    }
    final RestFuture future = clientHelper
        .sendQuery(HttpTestR66PseudoMain.config, channel, method, host, uri,
                   key, value, null, packet.toString());
    logger.debug("Query sent");
    return future;
  }

  @Override
  public void run() {
    if (isStatus) {
      for (int i = 0; i < NBPERTHREAD; i++) {
        getStatus();
      }
    } else {
      for (int i = 0; i < NBPERTHREAD; i++) {
        try {
          multiDataRequests(RESTHANDLERS.DbTaskRunner);
        } catch (final HttpInvalidAuthenticationException e) {
          e.printStackTrace();
        }
      }
    }
  }

  protected static void getStatus() {
    if (!RestConfiguration.CRUD.READ.isValid(
        HttpTestR66PseudoMain.config.getResthandlersCrud()[RESTHANDLERS.Server
            .ordinal()])) {
      logger.warn("Not allow to READ");
      return;
    }
    final Channel channel = clientHelper
        .getChannel(host, HttpTestR66PseudoMain.config.getRestPort());
    if (channel == null) {
      logger.warn("Cannot connect to: " + host + ':' +
                  HttpTestR66PseudoMain.config.getRestPort());
      fail("Cant connect");
      return;
    }
    String key = null, value = null;
    if (HttpTestR66PseudoMain.config.isRestAuthenticated()) {
      key = userAuthent;
      value = keyAuthent;
    }
    final RestFuture future = clientHelper
        .sendQuery(HttpTestR66PseudoMain.config, channel, HttpMethod.GET, host,
                   RESTHANDLERS.Server.uri, key, value, null, null);
    logger.debug("Query sent");
    future.awaitOrInterruptible();
    WaarpSslUtility.closingSslChannel(channel);
    assertTrue("Action should be ok", future.isSuccess());
    logger.debug("Channel closed");
  }

  protected static void multiDataRequests(RESTHANDLERS data)
      throws HttpInvalidAuthenticationException {
    if (!RestConfiguration.CRUD.CREATE.isValid(
        HttpTestR66PseudoMain.config.getResthandlersCrud()[data.ordinal()])) {
      logger.warn("Not allow to CREATE");
      return;
    }
    final Channel channel = clientHelper
        .getChannel(host, HttpTestR66PseudoMain.config.getRestPort());
    if (channel == null) {
      logger.warn("Cannot connect to: " + host + ':' +
                  HttpTestR66PseudoMain.config.getRestPort());
      fail("Cant connect");
      return;
    }
    final RestFuture future = createData(channel, data);
    future.awaitOrInterruptible();
    WaarpSslUtility.closingSslChannel(channel);
    assertTrue("Action should be ok", future.isSuccess());
    logger.debug("Channel closed");
  }

  protected static RestFuture createData(Channel channel, RESTHANDLERS data)
      throws HttpInvalidAuthenticationException {
    logger.debug("Send query");
    final AbstractDbData dbData = getItem(data);
    if (dbData == null) {
      final RestFuture future =
          channel.attr(HttpRestClientSimpleResponseHandler.RESTARGUMENT).get();
      future.cancel();
      WaarpSslUtility.closingSslChannel(channel);
      return future;
    }
    String key = null, value = null;
    if (HttpTestR66PseudoMain.config.isRestAuthenticated()) {
      key = userAuthent;
      value = keyAuthent;
    }
    final Map<String, String> args = null;
    final RestFuture future = clientHelper
        .sendQuery(HttpTestR66PseudoMain.config, channel, HttpMethod.POST, host,
                   data.uri, key, value, args, dbData.asJson());
    logger.debug("Query sent");
    return future;
  }

  protected static AbstractDbData getItem(RESTHANDLERS data)
      throws HttpInvalidAuthenticationException {
    switch (data) {
      case DbConfiguration:
        return new DbConfiguration(hostid + rank, limit, limit, limit, limit,
                                   delaylimit);
      case DbHostAuth:
        return new DbHostAuth(hostid + rank, address,
                              HttpTestR66PseudoMain.config.getRestPort(), false,
                              hostkey.getBytes(), false, false);
      case DbHostConfiguration:
        return new DbHostConfiguration(hostid + rank, business, roles, aliases,
                                       others);
      case DbRule:
        return new DbRule(idRule, ids, 2, "/recv", "/send", "/arch", "/work",
                          tasks, tasks, tasks, tasks, tasks, tasks);
      case DbTaskRunner:
        final ObjectNode source = JsonHandler.createObjectNode();
        source.put(Columns.IDRULE.name(), idRule);
        source.put(Columns.RANK.name(), 0);
        source.put(Columns.BLOCKSZ.name(), 65536);
        source.put(Columns.FILEINFO.name(), "file info");
        source.put(Columns.FILENAME.name(), "filename");
        source.put(Columns.GLOBALLASTSTEP.name(), TASKSTEP.NOTASK.ordinal());
        source.put(Columns.GLOBALSTEP.name(), TASKSTEP.NOTASK.ordinal());
        source.put(Columns.INFOSTATUS.name(), ErrorCode.Unknown.ordinal());
        source.put(Columns.ISMOVED.name(), false);
        source.put(Columns.MODETRANS.name(), 2);
        source.put(Columns.ORIGINALNAME.name(), "original filename");
        source.put(Columns.OWNERREQ.name(),
                   Configuration.configuration.getHostId());
        source.put(Columns.SPECIALID.name(), DbConstantR66.ILLEGALVALUE);
        source.put(Columns.REQUESTED.name(), hostid + rank);
        source.put(Columns.REQUESTER.name(), hostid + rank);
        source.put(Columns.RETRIEVEMODE.name(), true);
        source.put(Columns.STARTTRANS.name(), System.currentTimeMillis());
        source.put(Columns.STOPTRANS.name(), System.currentTimeMillis());
        source.put(Columns.STEP.name(), -1);
        source.put(Columns.STEPSTATUS.name(), ErrorCode.Unknown.ordinal());
        source.put(Columns.TRANSFERINFO.name(), "transfer info");
        source.put(DbTaskRunner.JSON_RESCHEDULE, false);
        source.put(DbTaskRunner.JSON_THROUGHMODE, false);
        source.put(DbTaskRunner.JSON_ORIGINALSIZE, 123L);
        try {
          return new DbTaskRunner(source);
        } catch (final WaarpDatabaseException e) {
          throw new HttpInvalidAuthenticationException(e);
        }
      default:
        break;
    }
    return null;
  }

}
