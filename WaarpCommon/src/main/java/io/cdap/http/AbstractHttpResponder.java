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
 * Copyright © 2014-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.http;

import io.cdap.http.internal.InternalUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Base implementation of {@link HttpResponder} to simplify child
 * implementations.
 */
public abstract class AbstractHttpResponder implements HttpResponder {

  protected static final String OCTET_STREAM_TYPE = "application/octet-stream";

  @Override
  public void sendJson(HttpResponseStatus status, String jsonString) {
    sendString(status, jsonString, new DefaultHttpHeaders()
        .add(HttpHeaderNames.CONTENT_TYPE.toString(), "application/json"));
  }

  @Override
  public void sendString(HttpResponseStatus status, String data) {
    sendString(status, data, EmptyHttpHeaders.INSTANCE);
  }

  @Override
  public void sendString(HttpResponseStatus status, String data,
                         HttpHeaders headers) {
    if (data == null) {
      sendStatus(status, headers);
      return;
    }
    final ByteBuf buffer =
        Unpooled.wrappedBuffer(InternalUtil.UTF_8.encode(data));
    sendContent(status, buffer,
                addContentTypeIfMissing(new DefaultHttpHeaders().add(headers),
                                        "text/plain; charset=utf-8"));
  }

  @Override
  public void sendStatus(HttpResponseStatus status) {
    sendContent(status, Unpooled.EMPTY_BUFFER, EmptyHttpHeaders.INSTANCE);
  }

  @Override
  public void sendStatus(HttpResponseStatus status, HttpHeaders headers) {
    sendContent(status, Unpooled.EMPTY_BUFFER, headers);
  }

  @Override
  public void sendByteArray(HttpResponseStatus status, byte[] bytes,
                            HttpHeaders headers) {
    final ByteBuf buffer = Unpooled.wrappedBuffer(bytes);
    sendContent(status, buffer, headers);
  }

  @Override
  public void sendBytes(HttpResponseStatus status, ByteBuffer buffer,
                        HttpHeaders headers) {
    sendContent(status, Unpooled.wrappedBuffer(buffer), headers);
  }

  @Override
  public void sendFile(File file) throws Throwable {
    sendFile(file, EmptyHttpHeaders.INSTANCE);
  }

  @Override
  public ChunkResponder sendChunkStart(HttpResponseStatus status) {
    return sendChunkStart(status, EmptyHttpHeaders.INSTANCE);
  }

  protected final HttpHeaders addContentTypeIfMissing(HttpHeaders headers,
                                                      String contentType) {
    if (!headers.contains(HttpHeaderNames.CONTENT_TYPE)) {
      headers.set(HttpHeaderNames.CONTENT_TYPE, contentType);
    }

    return headers;
  }
}
