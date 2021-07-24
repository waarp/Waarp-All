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
package org.waarp.commandexec.server;

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
import io.netty.util.concurrent.EventExecutorGroup;
import org.waarp.commandexec.utils.LocalExecDefaultResult;

/**
 * Creates a newly configured {@link ChannelPipeline} for a new channel for
 * LocalExecServer.
 */
public class LocalExecServerInitializer
    extends ChannelInitializer<SocketChannel> {

  private long delay = LocalExecDefaultResult.MAXWAITPROCESS;
  protected final EventExecutorGroup eventExecutorGroup;
  private final ChannelGroup channelGroup;

  /**
   * Constructor with default delay
   *
   * @param eventExecutorGroup
   */
  public LocalExecServerInitializer(
      final EventExecutorGroup eventExecutorGroup) {
    this.eventExecutorGroup = eventExecutorGroup;
    channelGroup =
        new DefaultChannelGroup("LocalExecServer", eventExecutorGroup.next());
  }

  /**
   * Constructor with a specific default delay
   *
   * @param newdelay
   * @param eventExecutorGroup
   */
  public LocalExecServerInitializer(final long newdelay,
                                    final EventExecutorGroup eventExecutorGroup) {
    delay = newdelay;
    this.eventExecutorGroup = eventExecutorGroup;
    channelGroup =
        new DefaultChannelGroup("LocalExecServer", eventExecutorGroup.next());
  }

  @Override
  public void initChannel(final SocketChannel ch) {
    // Create a default pipeline implementation.
    final ChannelPipeline pipeline = ch.pipeline();

    // Add the text line codec combination first,
    pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192,
                                                              Delimiters.lineDelimiter()));
    pipeline.addLast(eventExecutorGroup, "decoder", new StringDecoder());
    pipeline.addLast(eventExecutorGroup, "encoder", new StringEncoder());

    // and then business logic.
    // Could change it with a new fixed delay if necessary at construction
    pipeline.addLast(eventExecutorGroup, "handler",
                     new LocalExecServerHandler(this, delay));
  }

  /**
   * Add a channel to the ExecClient Group
   *
   * @param channel
   */
  public final void addChannel(final Channel channel) {
    channelGroup.add(channel);
  }

  /**
   * Release internal resources
   */
  public final void releaseResources() {
    channelGroup.close();
  }
}
