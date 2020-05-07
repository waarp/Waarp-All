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
package org.waarp.openr66.protocol.exception;

import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpShutdownHook;
import org.waarp.openr66.protocol.configuration.Configuration;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.BindException;
import java.net.ConnectException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.RejectedExecutionException;

/**
 * Class that filter exceptions
 */
public final class OpenR66ExceptionTrappedFactory {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(OpenR66ExceptionTrappedFactory.class);

  private OpenR66ExceptionTrappedFactory() {
  }

  /**
   * @param channel
   * @param throwable
   *
   * @return the OpenR66Exception corresponding to the ExceptionEvent, or null
   *     if the exception should be
   *     ignored
   */
  public static OpenR66Exception getExceptionFromTrappedException(
      Channel channel, Throwable throwable) {
    if (throwable instanceof ConnectException) {
      final ConnectException e2 = (ConnectException) throwable;
      logger.debug("Connection impossible since {} with Channel {}",
                   e2.getMessage(), channel);
      return new OpenR66ProtocolNoConnectionException("Connection impossible",
                                                      e2);
    } else if (throwable instanceof ChannelException) {
      final ChannelException e2 = (ChannelException) throwable;
      logger.info(
          "Connection (example: timeout) impossible since {} with Channel {}",
          e2.getMessage(), channel);
      return new OpenR66ProtocolNetworkException(
          "Connection (example: timeout) impossible", e2);
    } else if (throwable instanceof CancelledKeyException) {
      final CancelledKeyException e2 = (CancelledKeyException) throwable;
      logger.error("Connection aborted since {}", e2.getMessage());
      // Is it really what we should do ?
      // Yes, No action
      return null;
    } else if (throwable instanceof ClosedChannelException) {
      logger.debug("Connection closed before end");
      return new OpenR66ProtocolBusinessNoWriteBackException(
          "Connection closed before end", throwable);
    } else if (throwable instanceof IllegalMonitorStateException) {
      logger.debug("Try to release a lock incorrectly", throwable);
      return new OpenR66ProtocolBusinessNoWriteBackException(
          "Ignored exception", throwable);
    } else if (throwable instanceof OpenR66ProtocolBusinessCancelException) {
      final OpenR66ProtocolBusinessCancelException e2 =
          (OpenR66ProtocolBusinessCancelException) throwable;
      logger.debug("Request is canceled: {}", e2.getMessage());
      return e2;
    } else if (throwable instanceof OpenR66ProtocolNotAuthenticatedException) {
      final OpenR66ProtocolNotAuthenticatedException e2 =
          (OpenR66ProtocolNotAuthenticatedException) throwable;
      logger.debug("Request cannot continue since not authenticated: {}",
                   e2.getMessage());
      return e2;
    } else if (throwable instanceof OpenR66ProtocolBusinessStopException) {
      final OpenR66ProtocolBusinessStopException e2 =
          (OpenR66ProtocolBusinessStopException) throwable;
      logger.debug("Request is stopped: {}", e2.getMessage());
      return e2;
    } else if (throwable instanceof OpenR66ProtocolBusinessQueryAlreadyFinishedException) {
      final OpenR66ProtocolBusinessQueryAlreadyFinishedException e2 =
          (OpenR66ProtocolBusinessQueryAlreadyFinishedException) throwable;
      logger.debug("Request is already finished: {}", e2.getMessage());
      return e2;
    } else if (throwable instanceof OpenR66ProtocolBusinessQueryStillRunningException) {
      final OpenR66ProtocolBusinessQueryStillRunningException e2 =
          (OpenR66ProtocolBusinessQueryStillRunningException) throwable;
      logger.debug("Request is still running: {}", e2.getMessage());
      return e2;
    } else if (throwable instanceof OpenR66ProtocolBusinessRemoteFileNotFoundException) {
      final OpenR66ProtocolBusinessRemoteFileNotFoundException e2 =
          (OpenR66ProtocolBusinessRemoteFileNotFoundException) throwable;
      logger.debug("Remote server did not find file: {}", e2.getMessage());
      return e2;
    } else if (throwable instanceof OpenR66ProtocolBusinessNoWriteBackException) {
      final OpenR66ProtocolBusinessNoWriteBackException e2 =
          (OpenR66ProtocolBusinessNoWriteBackException) throwable;
      logger.error("Command Error Reply: {}", e2.getMessage());
      return e2;
    } else if (throwable instanceof OpenR66ProtocolShutdownException) {
      final OpenR66ProtocolShutdownException e2 =
          (OpenR66ProtocolShutdownException) throwable;
      logger.debug("Command Shutdown {}", e2.getMessage());
      return e2;
    } else if (throwable instanceof OpenR66Exception) {
      final OpenR66Exception e2 = (OpenR66Exception) throwable;
      logger.debug("Command Error Reply: {}", e2.getMessage());
      return e2;
    } else if (throwable instanceof BindException) {
      final BindException e2 = (BindException) throwable;
      logger.debug("Address already in use {}", e2.getMessage());
      return new OpenR66ProtocolNetworkException("Address already in use", e2);
    } else if (throwable instanceof NotYetConnectedException) {
      final NotYetConnectedException e2 = (NotYetConnectedException) throwable;
      logger.debug("Timeout occurs {}", e2.getMessage());
      return new OpenR66ProtocolNetworkException("Timeout occurs", e2);
    } else if (throwable instanceof NullPointerException) {
      final NullPointerException e2 = (NullPointerException) throwable;
      logger.error("Null pointer Exception", e2);
      return new OpenR66ProtocolSystemException("Null Pointer Exception", e2);
    } else if (throwable instanceof SSLException) {
      final SSLException e2 = (SSLException) throwable;
      logger.debug("Connection aborted since SSL Error {} with Channel {}",
                   e2.getMessage(), channel);
      return new OpenR66ProtocolBusinessNoWriteBackException(
          "SSL Connection aborted", e2);
    } else if (throwable instanceof IOException) {
      final IOException e2 = (IOException) throwable;
      logger
          .debug("Connection aborted since {} with Channel {}", e2.getMessage(),
                 channel);
      if (channel.isActive()) {
        return new OpenR66ProtocolSystemException(
            "Connection aborted due to " + e2.getMessage(), e2);
      } else {
        return new OpenR66ProtocolBusinessNoWriteBackException(
            "Connection aborted due to " + e2.getMessage(), e2);
      }
    } else if (throwable instanceof RejectedExecutionException) {
      final RejectedExecutionException e2 =
          (RejectedExecutionException) throwable;
      logger
          .debug("Connection aborted since {} with Channel {}", e2.getMessage(),
                 channel);
      if (channel.isActive()) {
        return new OpenR66ProtocolSystemException("Execution aborted", e2);
      } else {
        return new OpenR66ProtocolBusinessNoWriteBackException(
            "Execution aborted", e2);
      }
    } else if (throwable instanceof OutOfMemoryError) {
      final OpenR66ProtocolShutdownException e2 =
          new OpenR66ProtocolShutdownException("Restart since OOME raized",
                                               throwable);
      logger.debug("Force Shutdown and Restart : {}", e2.getMessage());
      if (Configuration.configuration.getR66Mib() != null) {
        Configuration.configuration.getR66Mib().notifyWarning(
            "OOME so shutdown and restart", throwable.getMessage());
      }
      WaarpShutdownHook.setRestart(true);
      return e2;
    } else {
      logger.error(
          "Unexpected exception from Outband" + " Ref Channel: " + channel,
          throwable);
    }
    if (Configuration.configuration.getR66Mib() != null) {
      Configuration.configuration.getR66Mib()
                                 .notifyWarning("Unexpected exception",
                                                throwable.getMessage());
    }
    return new OpenR66ProtocolSystemException(
        "Unexpected exception: " + throwable.getMessage(), throwable);
  }
}
