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

/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.waarp.openr66.protocol.http.rest.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpMethod;
import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.json.JsonHandler;
import org.waarp.gateway.kernel.exception.HttpInvalidAuthenticationException;
import org.waarp.gateway.kernel.rest.RestArgument;
import org.waarp.gateway.kernel.rest.RootOptionsRestMethodHandler;
import org.waarp.gateway.kernel.rest.client.HttpRestClientSimpleResponseHandler;
import org.waarp.gateway.kernel.rest.client.RestFuture;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbHostConfiguration;
import org.waarp.openr66.database.data.DbTaskRunner;
import org.waarp.openr66.protocol.http.rest.HttpRestR66Handler.RESTHANDLERS;
import org.waarp.openr66.protocol.http.rest.client.HttpRestR66ClientResponseHandler;
import org.waarp.openr66.protocol.http.rest.handler.HttpRestAbstractR66Handler.ACTIONS_TYPE;
import org.waarp.openr66.protocol.localhandler.packet.json.BandwidthJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.InformationJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.JsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.RestartTransferJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.StopOrCancelJsonPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.TransferRequestJsonPacket;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test Rest client response handler
 * <p>
 * Note that for testing, result is only the last "json" command, and therefore
 * future is only validated once
 * all items are passed in a chain. In normal condition, each step should
 * produce: setting the RestArgument to
 * the RestFuture and validating (error or ok) to RestFuture.
 */
public class HttpTestResponseHandler extends HttpRestR66ClientResponseHandler {
  /**
   * @param channel
   *
   * @throws HttpInvalidAuthenticationException
   */

  @Override
  protected boolean afterError(Channel channel, RestArgument ra) {
    HttpTestRestR66Client.count.incrementAndGet();
    WaarpSslUtility.closingSslChannel(channel);
    return false;
  }

  @Override
  protected boolean afterDbGet(Channel channel, RestArgument ra)
      throws HttpInvalidAuthenticationException {
    HttpTestRestR66Client.count.incrementAndGet();
    // Update
    HttpTestRestR66Client.updateData(channel, ra);
    return true;
  }

  @Override
  protected boolean afterDbPost(Channel channel, RestArgument ra)
      throws HttpInvalidAuthenticationException {
    HttpTestRestR66Client.count.incrementAndGet();
    if (ra.getAnswer().path(DbHostAuth.Columns.ADMINROLE.name()).asBoolean()) {
      WaarpSslUtility.closingSslChannel(channel);
      return false;
    }
    // Select 1
    HttpTestRestR66Client.readData(channel, ra);
    return true;
  }

  @Override
  protected boolean afterDbPut(Channel channel, RestArgument ra)
      throws HttpInvalidAuthenticationException {
    HttpTestRestR66Client.count.incrementAndGet();
    if ("hosta".equals(
        ra.getAnswer().path(DbHostConfiguration.Columns.HOSTID.name())
          .asText())) {
      WaarpSslUtility.closingSslChannel(channel);
      return false;
    }
    // Delete 1
    HttpTestRestR66Client.deleteData(channel, ra);
    return true;
  }

  @Override
  protected boolean afterDbDelete(Channel channel, RestArgument ra) {
    HttpTestRestR66Client.count.incrementAndGet();
    WaarpSslUtility.closingSslChannel(channel);
    return false;
  }

  @Override
  protected boolean afterDbGetMultiple(Channel channel, RestArgument ra) {
    HttpTestRestR66Client.count.incrementAndGet();
    WaarpSslUtility.closingSslChannel(channel);
    return false;
  }

  @Override
  protected boolean afterDbOptions(Channel channel, RestArgument ra)
      throws HttpInvalidAuthenticationException {
    HttpTestRestR66Client.count.incrementAndGet();
    boolean newMessage = false;
    AtomicInteger counter = null;
    final RestFuture future =
        channel.attr(HttpRestClientSimpleResponseHandler.RESTARGUMENT).get();
    if (future.getOtherObject() == null) {
      counter = new AtomicInteger();
      future.setOtherObject(counter);
      final JsonNode node = ra.getDetailedAllowOption();
      if (!node.isMissingNode()) {
        for (final JsonNode jsonNode : node) {
          final Iterator<String> iterator = jsonNode.fieldNames();
          while (iterator.hasNext()) {
            final String name = iterator.next();
            if (!jsonNode.path(name)
                         .path(RestArgument.REST_FIELD.JSON_PATH.field)
                         .isMissingNode()) {
              break;
            }
            if (name.equals(RootOptionsRestMethodHandler.ROOT)) {
              continue;
            }
            counter.incrementAndGet();
            HttpTestRestR66Client.options(channel, name);
            newMessage = true;
          }
        }
      }
    }
    if (!newMessage) {
      counter = (AtomicInteger) future.getOtherObject();
      newMessage = counter.decrementAndGet() > 0;
      if (!newMessage) {
        future.setOtherObject(null);
      }
    }
    if (!newMessage) {
      WaarpSslUtility.closingSslChannel(channel);
    }
    return newMessage;
  }

  @Override
  protected boolean action(Channel channel, RestArgument ra, ACTIONS_TYPE act) {
    HttpTestRestR66Client.count.incrementAndGet();
    boolean newMessage = false;
    switch (act) {
      case CreateTransfer: {
        // Continue with GetTransferInformation
        TransferRequestJsonPacket recv;
        try {
          recv = (TransferRequestJsonPacket) JsonPacket.createFromBuffer(
              JsonHandler.writeAsString(ra.getResults().get(0)));
        } catch (final Exception e) {
          e.printStackTrace();
          return newMessage;
        }
        final InformationJsonPacket node =
            new InformationJsonPacket(recv.getSpecialId(), false,
                                      recv.getRequested());
        HttpTestRestR66Client.action(channel, HttpMethod.GET,
                                     RESTHANDLERS.Control.uri, node);
        newMessage = true;
        break;
      }
      case ExecuteBusiness:
        // End
        break;
      case ExportConfig:
        // no Import in automatic test
        break;
      case GetBandwidth:
        // End
        break;
      case GetInformation:
        // End
        break;
      case GetLog:
        // End
        break;
      case GetTransferInformation: {
        // Continue with Stop in StopOrCancelTransfer
        final ObjectNode answer = (ObjectNode) ra.getResults().get(0);
        final StopOrCancelJsonPacket node = new StopOrCancelJsonPacket();
        node.setRequestUserPacket();
        node.setStop();
        node.setRequested(
            answer.path(DbTaskRunner.Columns.REQUESTED.name()).asText());
        node.setRequester(
            answer.path(DbTaskRunner.Columns.REQUESTER.name()).asText());
        node.setSpecialid(
            answer.path(DbTaskRunner.Columns.SPECIALID.name()).asLong());
        HttpTestRestR66Client.action(channel, HttpMethod.PUT,
                                     RESTHANDLERS.Control.uri, node);
        newMessage = true;
        break;
      }
      case ImportConfig:
        // End
        break;
      case OPTIONS:
        break;
      case RestartTransfer: {
        // Continue with delete transfer
        RestartTransferJsonPacket recv;
        try {
          recv = (RestartTransferJsonPacket) JsonPacket.createFromBuffer(
              JsonHandler.writeAsString(ra.getResults().get(0)));
        } catch (final Exception e) {
          e.printStackTrace();
          return newMessage;
        }
        try {
          HttpTestRestR66Client.deleteData(channel, recv.getRequested(),
                                           recv.getRequester(),
                                           recv.getSpecialid());
        } catch (final HttpInvalidAuthenticationException e) {
          e.printStackTrace();
        }
        newMessage = true;
        break;
      }
      case SetBandwidth: {
        // Continue with GetBandwidth
        BandwidthJsonPacket recv;
        try {
          recv = (BandwidthJsonPacket) JsonPacket.createFromBuffer(
              JsonHandler.writeAsString(ra.getResults().get(0)));
        } catch (final Exception e) {
          e.printStackTrace();
          return newMessage;
        }
        recv.setSetter(false);
        HttpTestRestR66Client.action(channel, HttpMethod.GET,
                                     RESTHANDLERS.Bandwidth.uri, recv);
        newMessage = true;
        break;
      }
      case ShutdownOrBlock:
        // End
        break;
      case StopOrCancelTransfer: {
        // Continue with RestartTransfer
        StopOrCancelJsonPacket recv;
        try {
          recv = (StopOrCancelJsonPacket) JsonPacket.createFromBuffer(
              JsonHandler.writeAsString(ra.getResults().get(0)));
        } catch (final Exception e) {
          e.printStackTrace();
          return newMessage;
        }
        final RestartTransferJsonPacket node = new RestartTransferJsonPacket();
        node.setRequestUserPacket();
        node.setRequested(recv.getRequested());
        node.setRequester(recv.getRequester());
        node.setSpecialid(recv.getSpecialid());
        HttpTestRestR66Client.action(channel, HttpMethod.PUT,
                                     RESTHANDLERS.Control.uri, node);
        newMessage = true;
        break;
      }
      case GetStatus:
        break;
      default:
        break;

    }
    if (!newMessage) {
      WaarpSslUtility.closingSslChannel(channel);
    }
    return newMessage;
  }
}
