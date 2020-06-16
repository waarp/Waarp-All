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
import org.waarp.common.utility.DetectionUtils;
import org.waarp.openr66.client.utils.OutputFormat;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.authentication.R66Auth;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.BusinessRequestPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import java.net.SocketAddress;

import static org.waarp.openr66.database.DbConstantR66.*;

/**
 * Abstract class for internal Business Request
 */
public abstract class AbstractBusinessRequest implements Runnable {
  /**
   * Internal Logger
   */
  protected static volatile WaarpLogger logger;

  protected static final String _INFO_ARGS =
      Messages.getString("AbstractBusinessRequest.0") //$NON-NLS-1$
      + Messages.getString("Message.OutputFormat");

  protected final R66Future future;

  protected final String remoteHost;

  protected final NetworkTransaction networkTransaction;

  private final BusinessRequestPacket businessPacket;

  private LocalChannelReference localChannelReference;

  protected AbstractBusinessRequest(Class<?> clasz, R66Future future,
                                    String remoteHost,
                                    NetworkTransaction networkTransaction,
                                    BusinessRequestPacket packet) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(clasz);
    }
    this.future = future;
    this.remoteHost = remoteHost;
    this.networkTransaction = networkTransaction;
    businessPacket = packet;
  }

  @Override
  public void run() {
    try {
      initRequest();
      sendRequest();
    } catch (final OpenR66ProtocolNoConnectionException ignored) {
      // notning
    }
  }

  public void initRequest() throws OpenR66ProtocolNoConnectionException {
    final DbHostAuth host = R66Auth.getServerAuth(remoteHost);
    if (host == null) {
      future.setResult(null);
      final OpenR66ProtocolNoConnectionException e2 =
          new OpenR66ProtocolNoConnectionException(
              Messages.getString("AdminR66OperationsGui.188") +
              remoteHost); //$NON-NLS-1$
      future.setFailure(e2);
      throw e2;
    }
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      future.setResult(null);
      final OpenR66ProtocolNoConnectionException e2 =
          new OpenR66ProtocolNoConnectionException(
              Messages.getString("AdminR66OperationsGui.188") +
              host); //$NON-NLS-1$
      future.setFailure(e2);
      throw e2;
    }
    final boolean isSSL = host.isSsl();
    localChannelReference = networkTransaction
        .createConnectionWithRetry(socketServerAddress, isSSL, future);
    if (localChannelReference == null) {
      future.setResult(null);
      final OpenR66ProtocolNoConnectionException e =
          new OpenR66ProtocolNoConnectionException(
              Messages.getString("AdminR66OperationsGui.188") +
              host); //$NON-NLS-1$
      future.setFailure(e);
      throw e;
    }
    localChannelReference.sessionNewState(R66FiniteDualStates.BUSINESSR);
  }

  public void sendRequest() {
    try {
      ChannelUtils
          .writeAbstractLocalPacket(localChannelReference, businessPacket,
                                    false);
    } catch (final OpenR66ProtocolPacketException e) {
      future.setResult(null);
      future.setFailure(e);
      localChannelReference.close();
    }

  }

  /**
   * Dummy Main method
   *
   * @param args
   */
  public static void main(String[] args) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(AbstractBusinessRequest.class);
    }
    if (!getParams(args)) {
      logger.error("Wrong initialization");
      if (admin != null) {
        admin.close();
      }
      if (DetectionUtils.isJunit()) {
        return;
      }
      ChannelUtils.stopLogger();
      System.exit(2);//NOSONAR
    }

    Configuration.configuration.pipelineInit();
    final NetworkTransaction networkTransaction = new NetworkTransaction();
    final R66Future future = new R66Future(true);

    logger.info("Start Test of Transaction");
    final long time1 = System.currentTimeMillis();

    new BusinessRequestPacket(classname + ' ' + classarg, 0);
    // XXX FIXME this has to be adapted
    /*
     * AbstractBusinessRequest transaction = new AbstractBusinessRequest( AbstractBusinessRequest.class, future,
     * rhost, networkTransaction, packet); transaction.run(); future.awaitUninterruptibly()
     */
    final long time2 = System.currentTimeMillis();
    logger.debug("Finish Business Request: " + future.isSuccess());
    final long delay = time2 - time1;
    if (future.isSuccess()) {
      logger.info(
          "Business Request in status: SUCCESS" + "    <REMOTE>" + rhost +
          "</REMOTE>" + "    delay: " + delay);
    } else {
      logger.info(
          "Business Request in status: FAILURE" + "    <REMOTE>" + rhost +
          "</REMOTE>" + "    <ERROR>" + future.getCause() + "</ERROR>" +
          "    delay: " + delay);
      if (DetectionUtils.isJunit()) {
        return;
      }
      networkTransaction.closeAll();
      System.exit(ErrorCode.Unknown.ordinal());//NOSONAR
    }
    networkTransaction.closeAll();
  }

  protected static String rhost;
  protected static String classname;
  protected static String classarg;
  protected static boolean nolog;

  /**
   * Parse the parameter and set current values
   *
   * @param args
   *
   * @return True if all parameters were found and correct
   */
  protected static boolean getParams(String[] args) {
    if (args.length < 3) {
      logger.error(_INFO_ARGS);
      return false;
    }
    if (!FileBasedConfiguration
        .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
      logger.error(
          Messages.getString("Configuration.NeedCorrectConfig")); //$NON-NLS-1$
      return false;
    }
    // Now set default values from configuration
    for (int i = 1; i < args.length; i++) {
      if ("-to".equalsIgnoreCase(args[i])) {
        i++;
        rhost = args[i];
        if (Configuration.configuration.getAliases().containsKey(rhost)) {
          rhost = Configuration.configuration.getAliases().get(rhost);
        }
      } else if ("-class".equalsIgnoreCase(args[i])) {
        i++;
        classname = args[i];
      } else if ("-arg".equalsIgnoreCase(args[i])) {
        i++;
        classarg = args[i];
      } else if ("-nolog".equalsIgnoreCase(args[i])) {
        nolog = true;
        i++;
      }
    }
    OutputFormat.getParams(args);
    if (rhost != null && classname != null) {
      return true;
    }
    logger.error(Messages.getString("AbstractBusinessRequest.NeedMoreArgs",
                                    "(-to -class)") + _INFO_ARGS); //$NON-NLS-1$
    return false;
  }

}
