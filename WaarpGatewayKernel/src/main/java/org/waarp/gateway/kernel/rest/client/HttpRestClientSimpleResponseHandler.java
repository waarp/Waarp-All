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
package org.waarp.gateway.kernel.rest.client;

import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.AttributeKey;

import org.waarp.common.crypto.ssl.WaarpSslUtility;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.gateway.kernel.exception.HttpIncorrectRequestException;
import org.waarp.gateway.kernel.rest.RestArgument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 
 * @author Frederic Bregier
 */
public class HttpRestClientSimpleResponseHandler extends SimpleChannelInboundHandler<HttpObject> {
    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(HttpRestClientSimpleResponseHandler.class);

    public static final AttributeKey<RestFuture> RESTARGUMENT = AttributeKey.valueOf("RestClient.Argument");

    private ByteBuf cumulativeBody = null;
    protected JsonNode jsonObject = null;

    protected void actionFromResponse(Channel channel) {
        RestArgument ra = new RestArgument((ObjectNode) jsonObject);
        if (jsonObject == null) {
            logger.warn("Recv: EMPTY");
        } else {
            logger.warn(ra.prettyPrint());
        }
        RestFuture restFuture = channel.attr(RESTARGUMENT).get();
        restFuture.setRestArgument(ra);
        if (ra.getStatusCode() == HttpResponseStatus.OK.code()) {
            restFuture.setSuccess();
        } else {
            logger.error("Error: " + ra.getStatusMessage());
            restFuture.cancel();
            if (channel.isActive()) {
                WaarpSslUtility.closingSslChannel(channel);
            }
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpResponse) {
            HttpResponse response = (HttpResponse) msg;
            HttpResponseStatus status = response.status();
            logger.debug(HttpHeaderNames.REFERER + ": " + response.headers().get(HttpHeaderNames.REFERER) +
                    " STATUS: " + status);
        }
        if (msg instanceof HttpContent) {
            HttpContent chunk = (HttpContent) msg;
            if (chunk instanceof LastHttpContent) {
                ByteBuf content = chunk.content();
                if (content != null && content.isReadable()) {
                    if (cumulativeBody != null) {
                        cumulativeBody = Unpooled.wrappedBuffer(cumulativeBody, content);
                    } else {
                        cumulativeBody = content;
                    }
                }
                // get the Json equivalent of the Body
                if (cumulativeBody == null) {
                    jsonObject = JsonHandler.createObjectNode();
                } else {
                    try {
                        String json = cumulativeBody.toString(WaarpStringUtils.UTF8);
                        jsonObject = JsonHandler.getFromString(json);
                    } catch (Throwable e2) {
                        logger.warn("Error", e2);
                        throw new HttpIncorrectRequestException(e2);
                    }
                    cumulativeBody = null;
                }
                actionFromResponse(ctx.channel());
            } else {
                ByteBuf content = chunk.content();
                if (content != null && content.isReadable()) {
                    if (cumulativeBody != null) {
                        cumulativeBody = Unpooled.wrappedBuffer(cumulativeBody, content);
                    } else {
                        cumulativeBody = content;
                    }
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        RestFuture restFuture = ctx.channel().attr(RESTARGUMENT).get();
        if (cause instanceof ClosedChannelException) {
            restFuture.setFailure(cause);
            logger.debug("Close before ending");
            return;
        } else if (cause instanceof ConnectException) {
            restFuture.setFailure(cause);
            if (ctx.channel().isActive()) {
                logger.debug("Will close");
                WaarpSslUtility.closingSslChannel(ctx.channel());
            }
            return;
        }
        restFuture.setFailure(cause);
        logger.error("Error", cause);
        WaarpSslUtility.closingSslChannel(ctx.channel());
    }

}
