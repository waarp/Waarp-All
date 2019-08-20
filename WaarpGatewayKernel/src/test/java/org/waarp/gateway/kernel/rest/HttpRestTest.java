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
import org.junit.Test;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.gateway.kernel.http.saplink.HttpGerenateJsonConfiguration;
import org.waarp.gateway.kernel.rest.client.RestFuture;

import java.io.File;

import static org.junit.Assert.*;

public class HttpRestTest {

  @Test
  public void testSimpleRest() {
    WaarpLoggerFactory
        .setDefaultFactory(new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    RestConfiguration configuration =
        HttpRestHandlerTest.getTestConfiguration();
    HttpRestHandlerTest.initialize("/tmp");
    HttpRestHandlerTest.initializeService(configuration);
    RestClient client =
        new RestClient("/", 10, 1000, new HttpRestClientTestInitializer());
    Channel channel =
        client.getChannel(HttpRestHandlerTest.HOST, HttpRestHandlerTest.PORT);
    SysErrLogger.FAKE_LOGGER.syserr("OPTIONS");
    RestFuture future = client
        .sendQuery(channel, HttpMethod.OPTIONS, HttpRestHandlerTest.HOST,
                   DbTransferLogDataModelRestMethodHandler.BASEURI, null, null,
                   null);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());

    String json = "{'MODETRANS':'1', 'ACCOUNTID':'accid', " +
                  "'USERID':'userid', 'FILENAME':'filename', " +
                  "'INFOSTATUS':'0'}";
    SysErrLogger.FAKE_LOGGER.syserr("POST");
    future = client
        .sendQuery(channel, HttpMethod.POST, HttpRestHandlerTest.HOST,
                   DbTransferLogDataModelRestMethodHandler.BASEURI, null, null,
                   json);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());
    SysErrLogger.FAKE_LOGGER.syserr("GET");
    future = client.sendQuery(channel, HttpMethod.GET, HttpRestHandlerTest.HOST,
                              DbTransferLogDataModelRestMethodHandler.BASEURI,
                              null, null, json);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());
    SysErrLogger.FAKE_LOGGER.syserr("PUT");
    future = client.sendQuery(channel, HttpMethod.PUT, HttpRestHandlerTest.HOST,
                              DbTransferLogDataModelRestMethodHandler.BASEURI +
                              "/id", null, null, json);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());
    SysErrLogger.FAKE_LOGGER.syserr("PATCH");
    future = client
        .sendQuery(channel, HttpMethod.PATCH, HttpRestHandlerTest.HOST,
                   DbTransferLogDataModelRestMethodHandler.BASEURI, null, null,
                   json);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());
    SysErrLogger.FAKE_LOGGER.syserr("TRACE");
    future = client
        .sendQuery(channel, HttpMethod.TRACE, HttpRestHandlerTest.HOST,
                   DbTransferLogDataModelRestMethodHandler.BASEURI, null, null,
                   json);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());
    SysErrLogger.FAKE_LOGGER.syserr("DELETE");
    future = client
        .sendQuery(channel, HttpMethod.DELETE, HttpRestHandlerTest.HOST,
                   DbTransferLogDataModelRestMethodHandler.BASEURI + "/id",
                   null, null, json);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());

    // Wrong query
    SysErrLogger.FAKE_LOGGER.syserr("GET ROOT");
    future = client
        .sendQuery(channel, HttpMethod.GET, HttpRestHandlerTest.HOST, null,
                   null, null, json);
    assertTrue(future.awaitOrInterruptible());
    Assert.assertFalse(future.isSuccess());
    channel.close();
    HttpRestHandlerTest.group.close();
  }

  @Test
  public void testJson() {
    File file = new File("/tmp/sapLink.json");
    HttpGerenateJsonConfiguration.main(new String[] { file.getAbsolutePath() });
    assertTrue(file.exists());
    file.delete();
  }
}
