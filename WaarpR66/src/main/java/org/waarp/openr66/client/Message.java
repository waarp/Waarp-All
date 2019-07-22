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
package org.waarp.openr66.client;

import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.openr66.client.utils.OutputFormat;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.authentication.R66Auth;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.TestPacket;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import java.net.SocketAddress;

/**
 * Message testing between two hosts
 *
 *
 */
public class Message implements Runnable {
  /**
   * Internal Logger
   */
  private static WaarpLogger logger;

  protected static String _INFO_ARGS = Messages.getString("Message.0") +
                                       Messages
                                           .getString("Message.OutputFormat");
  //$NON-NLS-1$

  final private NetworkTransaction networkTransaction;

  final private R66Future future;

  private final String requested;

  private final DbHostAuth hostAuth;

  final private TestPacket testPacket;

  static String srequested = null;
  static String smessage = "MESSAGE";

  /**
   * Parse the parameter and set current values
   *
   * @param args
   *
   * @return True if all parameters were found and correct
   */
  protected static boolean getParams(String[] args) {
    _INFO_ARGS = Messages.getString("Message.0") +
                 Messages.getString("Message.OutputFormat"); //$NON-NLS-1$
    if (args.length < 5) {
      logger.error(_INFO_ARGS);
      return false;
    }
    if (!FileBasedConfiguration
        .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
      logger.error(
          Messages.getString("Configuration.NeedCorrectConfig")); //$NON-NLS-1$
      return false;
    }
    for (int i = 1; i < args.length; i++) {
      if (args[i].equalsIgnoreCase("-to")) {
        i++;
        srequested = args[i];
        if (Configuration.configuration.getAliases().containsKey(srequested)) {
          srequested = Configuration.configuration.getAliases().get(srequested);
        }
      } else if (args[i].equalsIgnoreCase("-msg")) {
        i++;
        smessage = args[i];
      }
    }
    OutputFormat.getParams(args);
    if (srequested == null) {
      logger.error(Messages.getString("Message.HostIdMustBeSet") +
                   _INFO_ARGS); //$NON-NLS-1$
      return false;
    }
    return true;
  }

  public Message(NetworkTransaction networkTransaction, R66Future future,
                 String requested, TestPacket packet) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(Message.class);
    }
    this.networkTransaction = networkTransaction;
    this.future = future;
    this.requested = requested;
    testPacket = packet;
    hostAuth = null;
  }

  public Message(NetworkTransaction networkTransaction, R66Future future,
                 DbHostAuth hostAuth, TestPacket packet) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(Message.class);
    }
    this.networkTransaction = networkTransaction;
    this.future = future;
    requested = null;
    testPacket = packet;
    this.hostAuth = hostAuth;
  }

  @Override
  public void run() {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(Message.class);
    }
    // Connection
    DbHostAuth host = null;
    if (hostAuth == null) {
      host = R66Auth.getServerAuth(DbConstant.admin.getSession(), requested);
    } else {
      host = hostAuth;
    }
    if (host == null) {
      logger.debug(
          Messages.getString("Message.HostNotFound") + requested); //$NON-NLS-1$
      final R66Result result =
          new R66Result(null, true, ErrorCode.ConnectionImpossible, null);
      future.setResult(result);
      future.cancel();
      return;
    }
    if (host.isClient()) {
      logger.error(
          Messages.getString("Message.HostIsClient") + requested); //$NON-NLS-1$
      final R66Result result =
          new R66Result(null, true, ErrorCode.ConnectionImpossible, null);
      future.setResult(result);
      future.cancel();
      return;
    }
    SocketAddress socketAddress = host.getSocketAddress();
    final boolean isSSL = host.isSsl();
    LocalChannelReference localChannelReference = null;
    localChannelReference = networkTransaction
        .createConnectionWithRetry(socketAddress, isSSL, future);
    socketAddress = null;
    if (localChannelReference == null) {
      logger.debug(Messages.getString("AdminR66OperationsGui.188") +
                   requested); //$NON-NLS-1$
      final R66Result result =
          new R66Result(null, true, ErrorCode.ConnectionImpossible, null);
      future.setResult(result);
      future.cancel();
      return;
    }
    localChannelReference.sessionNewState(R66FiniteDualStates.TEST);
    try {
      ChannelUtils
          .writeAbstractLocalPacket(localChannelReference, testPacket, false);
    } catch (final OpenR66ProtocolPacketException e) {
      future.setResult(null);
      future.setFailure(e);
      localChannelReference.getLocalChannel().close();
      return;
    }
  }

  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(Message.class);
    }
    if (args.length < 5) {
      logger.error(_INFO_ARGS);
      System.exit(1);
    }
    if (!getParams(args)) {
      logger.error(Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
      if (DbConstant.admin != null && DbConstant.admin.isActive()) {
        DbConstant.admin.close();
      }
      ChannelUtils.stopLogger();
      System.exit(1);
    }
    NetworkTransaction networkTransaction = null;
    int value = 3;
    try {
      Configuration.configuration.pipelineInit();
      networkTransaction = new NetworkTransaction();
      final R66Future result = new R66Future(true);
      final TestPacket packet = new TestPacket("MSG", smessage, 100);
      final Message transaction =
          new Message(networkTransaction, result, srequested, packet);
      transaction.run();
      result.awaitOrInterruptible();
      if (result.isSuccess()) {
        value = 0;
        final R66Result r66result = result.getResult();
        final ValidPacket info = (ValidPacket) r66result.getOther();
        logger.warn(Messages.getString("Message.11") +
                    Messages.getString("RequestInformation.Success") +
                    info.getSheader()); //$NON-NLS-1$
        if (!OutputFormat.isQuiet()) {
          System.out.println(Messages.getString("Message.11") +
                             Messages.getString("RequestInformation.Success") +
                             info.getSheader()); //$NON-NLS-1$
        }
      } else {
        value = 2;
        logger.error(Messages.getString("Message.11") +
                     Messages.getString("RequestInformation.Failure") +
                     //$NON-NLS-1$
                     result.getResult().toString());
        if (!OutputFormat.isQuiet()) {
          System.out.println(Messages.getString("Message.11") +
                             Messages.getString("RequestInformation.Failure") +
                             //$NON-NLS-1$
                             result.getResult().toString());
        }
      }
    } finally {
      if (networkTransaction != null) {
        networkTransaction.closeAll();
      }
      if (DbConstant.admin != null) {
        DbConstant.admin.close();
      }
      System.exit(value);
    }
  }

}
