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
 * Copyright © 2017-2019 Cask Data, Inc.
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

package io.cdap.http.internal;

import io.cdap.http.AbstractHttpResponder;
import io.cdap.http.BodyProducer;
import io.cdap.http.ChunkResponder;
import io.cdap.http.HandlerHook;
import io.cdap.http.HttpResponder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Wrap HttpResponder to call post handler hook.
 */
final class WrappedHttpResponder extends AbstractHttpResponder {
  private static final Logger LOG =
      LoggerFactory.getLogger(WrappedHttpResponder.class);

  private final HttpResponder delegate;
  private final Iterable<? extends HandlerHook> handlerHooks;
  private final HttpRequest httpRequest;
  private final HandlerInfo handlerInfo;

  WrappedHttpResponder(HttpResponder delegate,
                       Iterable<? extends HandlerHook> handlerHooks,
                       HttpRequest httpRequest, HandlerInfo handlerInfo) {
    this.delegate = delegate;
    this.handlerHooks = handlerHooks;
    this.httpRequest = httpRequest;
    this.handlerInfo = handlerInfo;
  }

  @Override
  public ChunkResponder sendChunkStart(final HttpResponseStatus status,
                                       HttpHeaders headers) {
    final ChunkResponder chunkResponder =
        delegate.sendChunkStart(status, headers);
    return new ChunkResponder() {
      @Override
      public void sendChunk(ByteBuffer chunk) throws IOException {
        chunkResponder.sendChunk(chunk);
      }

      @Override
      public void sendChunk(ByteBuf chunk) throws IOException {
        chunkResponder.sendChunk(chunk);
      }

      @Override
      public void close() throws IOException {
        chunkResponder.close();
        runHook(status);
      }
    };
  }

  @Override
  public void sendContent(HttpResponseStatus status, ByteBuf content,
                          HttpHeaders headers) {
    delegate.sendContent(status, content, headers);
    runHook(status);
  }

  @Override
  public void sendFile(File file, HttpHeaders headers) throws Throwable {
    delegate.sendFile(file, headers);
    runHook(HttpResponseStatus.OK);
  }

  @Override
  public void sendContent(HttpResponseStatus status, BodyProducer bodyProducer,
                          HttpHeaders headers) {
    delegate.sendContent(status, bodyProducer, headers);
    runHook(status);
  }

  private void runHook(HttpResponseStatus status) {
    for (final HandlerHook hook : handlerHooks) {
      try {
        hook.postCall(httpRequest, status, handlerInfo);
      } catch (final Throwable t) {
        LOG.error("Post handler hook threw exception: ", t);
      }
    }
  }
}
