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

package org.waarp.gateway.kernel.rest;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.TestWatcherJunit4;
import org.waarp.gateway.kernel.http.saplink.HttpGerenateJsonConfiguration;
import org.waarp.gateway.kernel.rest.client.RestFuture;

import java.io.File;

import static org.junit.Assert.*;

public class HttpRestTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();


  @Test
  public void testSimpleRest() {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    RestConfiguration configuration =
        HttpRestTestHandler.getTestConfiguration();
    HttpRestTestHandler.initialize("/tmp");
    HttpRestTestHandler.initializeService(configuration);
    RestClient client =
        new RestClient("/", 10, 1000, new HttpRestClientTestInitializer());
    Channel channel =
        client.getChannel(HttpRestTestHandler.HOST, HttpRestTestHandler.PORT);
    SysErrLogger.FAKE_LOGGER.sysout("OPTIONS");
    RestFuture future = client
        .sendQuery(channel, HttpMethod.OPTIONS, HttpRestTestHandler.HOST,
                   DbTransferLogDataModelRestMethodHandler.BASEURI, null, null,
                   null);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());

    String json = "{'MODETRANS':'1', 'ACCOUNTID':'accid', " +
                  "'USERID':'userid', 'FILENAME':'filename', " +
                  "'INFOSTATUS':'0'}";
    SysErrLogger.FAKE_LOGGER.sysout("POST");
    future = client
        .sendQuery(channel, HttpMethod.POST, HttpRestTestHandler.HOST,
                   DbTransferLogDataModelRestMethodHandler.BASEURI, null, null,
                   json);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());
    SysErrLogger.FAKE_LOGGER.sysout("GET");
    future = client.sendQuery(channel, HttpMethod.GET, HttpRestTestHandler.HOST,
                              DbTransferLogDataModelRestMethodHandler.BASEURI,
                              null, null, json);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());
    SysErrLogger.FAKE_LOGGER.sysout("PUT");
    future = client.sendQuery(channel, HttpMethod.PUT, HttpRestTestHandler.HOST,
                              DbTransferLogDataModelRestMethodHandler.BASEURI +
                              "/id", null, null, json);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());
    SysErrLogger.FAKE_LOGGER.sysout("PATCH");
    future = client
        .sendQuery(channel, HttpMethod.PATCH, HttpRestTestHandler.HOST,
                   DbTransferLogDataModelRestMethodHandler.BASEURI, null, null,
                   json);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());
    SysErrLogger.FAKE_LOGGER.sysout("TRACE");
    future = client
        .sendQuery(channel, HttpMethod.TRACE, HttpRestTestHandler.HOST,
                   DbTransferLogDataModelRestMethodHandler.BASEURI, null, null,
                   json);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());
    SysErrLogger.FAKE_LOGGER.sysout("DELETE");
    future = client
        .sendQuery(channel, HttpMethod.DELETE, HttpRestTestHandler.HOST,
                   DbTransferLogDataModelRestMethodHandler.BASEURI + "/id",
                   null, null, json);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());

    // Wrong query
    SysErrLogger.FAKE_LOGGER.sysout("GET ROOT");
    future = client
        .sendQuery(channel, HttpMethod.GET, HttpRestTestHandler.HOST, null,
                   null, null, json);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());
    channel.close();
    HttpRestTestHandler.group.close();
  }

  @Test
  public void testJson() {
    File file = new File("/tmp/sapLink.json");
    HttpGerenateJsonConfiguration.main(new String[] { file.getAbsolutePath() });
    assertTrue(file.exists());
    file.delete();
  }
}
