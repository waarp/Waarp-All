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
package org.waarp.commandexec.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.DefaultEventExecutor;
import org.waarp.common.utility.WaarpStringUtils;

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel for
 * LocalExecClientTest.
 */
public class LocalExecClientInitializer
    extends ChannelInitializer<SocketChannel> {

  private final ChannelGroup channelGroup;

  public LocalExecClientInitializer() {
    channelGroup =
        new DefaultChannelGroup("LocalExecClient", new DefaultEventExecutor());
  }

  @Override
  protected void initChannel(final SocketChannel ch) {
    // Create a default pipeline implementation.
    final ChannelPipeline pipeline = ch.pipeline();

    // Add the text line codec combination first,
    pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters
        .lineDelimiter()));
    pipeline.addLast("decoder", new StringDecoder(WaarpStringUtils.UTF8));
    pipeline.addLast("encoder", new StringEncoder(WaarpStringUtils.UTF8));

    // and then business logic.
    final LocalExecClientHandler localExecClientHandler =
        new LocalExecClientHandler(this);
    pipeline.addLast("handler", localExecClientHandler);
  }

  /**
   * Add a channel to the ExecClient Group
   *
   * @param channel
   */
  public void addChannel(final Channel channel) {
    channelGroup.add(channel);
  }

  /**
   * Release internal resources
   */
  public void releaseResources() {
    channelGroup.close();
  }
}
