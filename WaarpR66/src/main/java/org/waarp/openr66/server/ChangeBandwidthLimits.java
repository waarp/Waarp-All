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
package org.waarp.openr66.server;

import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.database.DbConstant;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoConnectionException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.BandwidthJsonPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import java.net.SocketAddress;

/**
 * This command enables the dynamic change of bandwidth limitation. It does not
 * changed the valuesin the
 * database but only dynamic values while the server is running and until it is
 * shutdown.
 *
 *
 */
public class ChangeBandwidthLimits implements Runnable {
  /**
   * Internal Logger
   */
  static volatile WaarpLogger logger;

  protected static String _INFO_ARGS =
      "Need the configuration file as first argument then at least one of\n" +
      "   -wglob limitGlobalWrite\n" + "   -rglob limitGlobalRead\n" +
      "   -wsess limitSessionWrite\n" + "   -rsess limitSessionWrite\n" +
      "   -host host (optional)";

  protected final R66Future future;
  protected final long writeGlobalLimit;
  protected final long readGlobalLimit;
  protected final long writeSessionLimit;
  protected final long readSessionLimit;
  protected final NetworkTransaction networkTransaction;
  protected DbHostAuth host;

  public ChangeBandwidthLimits(R66Future future, long wgl, long rgl, long wsl,
                               long rsl,
                               NetworkTransaction networkTransaction) {
    this.future = future;
    writeGlobalLimit = wgl;
    readGlobalLimit = rgl;
    writeSessionLimit = wsl;
    readSessionLimit = rsl;
    this.networkTransaction = networkTransaction;
    host = Configuration.configuration.getHOST_SSLAUTH();
  }

  public void setHost(DbHostAuth host) {
    this.host = host;
  }

  /**
   * Prior to call this method, the pipeline and NetworkTransaction must have
   * been initialized. It is the
   * responsibility of the caller to finish all network resources.
   */
  @Override
  public void run() {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ChangeBandwidthLimits.class);
    }
    SocketAddress socketAddress;
    try {
      socketAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Cannot Connect to " + host.getHostid());
      future.setResult(new R66Result(new OpenR66ProtocolNoConnectionException(
          "Cannot connect to server " + host.getHostid()), null, true,
                                     ErrorCode.ConnectionImpossible, null));
      host = null;
      future.setFailure(future.getResult().getException());
      return;
    }
    final boolean isSSL = host.isSsl();
    LocalChannelReference localChannelReference = networkTransaction
        .createConnectionWithRetry(socketAddress, isSSL, future);
    socketAddress = null;
    if (localChannelReference == null) {
      host = null;
      logger.error("Cannot Connect to " + host.getHostid());
      future.setResult(new R66Result(new OpenR66ProtocolNoConnectionException(
          "Cannot connect to server " + host.getHostid()), null, true,
                                     ErrorCode.ConnectionImpossible, null));
      future.setFailure(future.getResult().getException());
      return;
    }
    localChannelReference.sessionNewState(R66FiniteDualStates.VALIDOTHER);
    AbstractLocalPacket valid = null;
    final boolean useJson = PartnerConfiguration.useJson(host.getHostid());
    logger.debug("UseJson: " + useJson);
    if (useJson) {
      final BandwidthJsonPacket node = new BandwidthJsonPacket();
      if (writeGlobalLimit < 0 && readGlobalLimit < 0 &&
          writeSessionLimit < 0 && readSessionLimit < 0) {
        // will ask current values instead
        node.setSetter(false);
        valid = new JsonCommandPacket(node, LocalPacketFactory.BANDWIDTHPACKET);
      } else {
        node.setSetter(true);
        node.setWriteglobal(writeGlobalLimit);
        node.setReadglobal(readGlobalLimit);
        node.setWritesession(writeSessionLimit);
        node.setReadsession(readSessionLimit);
        valid = new JsonCommandPacket(node, LocalPacketFactory.BANDWIDTHPACKET);
      }
    } else {
      if (writeGlobalLimit < 0 && readGlobalLimit < 0 &&
          writeSessionLimit < 0 && readSessionLimit < 0) {
        // will ask current values instead
        valid = new ValidPacket("-1", "-1", LocalPacketFactory.BANDWIDTHPACKET);
      } else {
        valid = new ValidPacket(writeGlobalLimit + " " + readGlobalLimit,
                                writeSessionLimit + " " + readSessionLimit,
                                LocalPacketFactory.BANDWIDTHPACKET);
      }
    }
    try {
      ChannelUtils
          .writeAbstractLocalPacket(localChannelReference, valid, false);
    } catch (final OpenR66ProtocolPacketException e) {
      logger.error("Bad Protocol", e);
      localChannelReference.getLocalChannel().close();
      localChannelReference = null;
      host = null;
      valid = null;
      future.setResult(
          new R66Result(e, null, true, ErrorCode.TransferError, null));
      future.setFailure(e);
      return;
    }
    host = null;
    future.awaitOrInterruptible();
    logger
        .info("Request done with " + (future.isSuccess()? "success" : "error"));
    localChannelReference.getLocalChannel().close();
    localChannelReference = null;
  }

  protected static long swriteGlobalLimit = -1;
  protected static long sreadGlobalLimit = -1;
  protected static long swriteSessionLimit = -1;
  protected static long sreadSessionLimit = -1;
  protected static String stohost = null;

  protected static boolean getParams(String[] args) {
    if (args.length < 3) {
      logger.error(_INFO_ARGS);
      return false;
    }
    if (!FileBasedConfiguration
        .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
      logger.error(_INFO_ARGS);
      return false;
    }
    for (int i = 1; i < args.length; i++) {
      try {
        if (args[i].equalsIgnoreCase("-wglob")) {
          i++;
          swriteGlobalLimit = Long.parseLong(args[i]);
        } else if (args[i].equalsIgnoreCase("-rglob")) {
          i++;
          sreadGlobalLimit = Long.parseLong(args[i]);
        } else if (args[i].equalsIgnoreCase("-wsess")) {
          i++;
          swriteSessionLimit = Long.parseLong(args[i]);
        } else if (args[i].equalsIgnoreCase("-rsess")) {
          i++;
          sreadSessionLimit = Long.parseLong(args[i]);
        } else if (args[i].equalsIgnoreCase("-host")) {
          i++;
          stohost = args[i];
        }
      } catch (final NumberFormatException e) {
      }
    }
    if (swriteGlobalLimit == -1 && sreadGlobalLimit == -1 &&
        swriteSessionLimit == -1 && sreadSessionLimit == -1) {
      logger.error(_INFO_ARGS);
      return false;
    }
    return true;
  }

  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ChangeBandwidthLimits.class);
    }
    if (!getParams(args)) {
      logger.error("Wrong initialization");
      if (DbConstant.admin != null && DbConstant.admin.isActive()) {
        DbConstant.admin.close();
      }
      System.exit(1);
    }
    final long time1 = System.currentTimeMillis();
    final R66Future future = new R66Future(true);

    Configuration.configuration.pipelineInit();
    final NetworkTransaction networkTransaction = new NetworkTransaction();
    try {
      final ChangeBandwidthLimits transaction =
          new ChangeBandwidthLimits(future, swriteGlobalLimit, sreadGlobalLimit,
                                    swriteSessionLimit, sreadSessionLimit,
                                    networkTransaction);
      if (stohost != null) {
        try {
          transaction.setHost(new DbHostAuth(stohost));
        } catch (final WaarpDatabaseException e) {
          logger.error(
              "Bandwidth in     FAILURE since Host is not found: " + stohost,
              e);
          networkTransaction.closeAll();
          System.exit(10);
        }
      } else {
        stohost = Configuration.configuration.getHOST_SSLID();
      }
      transaction.run();
      future.awaitOrInterruptible();
      final long time2 = System.currentTimeMillis();
      final long delay = time2 - time1;
      final R66Result result = future.getResult();
      final boolean useJson = PartnerConfiguration.useJson(stohost);
      logger.debug("UseJson: " + useJson);
      if (future.isSuccess()) {
        String sresult = null;
        if (result.getOther() != null) {
          if (useJson) {
            sresult = ((JsonCommandPacket) result.getOther()).getRequest();
          } else {
            sresult = ((ValidPacket) result.getOther()).getSheader();
          }
        } else {
          sresult = "no result";
        }
        if (result.getCode() == ErrorCode.Warning) {
          logger.warn(
              "WARNED on bandwidth:     " + sresult + "     delay: " + delay);
        } else {
          logger.warn(
              "SUCCESS on Bandwidth:     " + sresult + "     delay: " + delay);
        }
      } else {
        if (result.getCode() == ErrorCode.Warning) {
          logger.warn("Bandwidth is     WARNED", future.getCause());
          networkTransaction.closeAll();
          System.exit(result.getCode().ordinal());
        } else {
          logger.error("Bandwidth in     FAILURE", future.getCause());
          networkTransaction.closeAll();
          System.exit(result.getCode().ordinal());
        }
      }
    } finally {
      networkTransaction.closeAll();
      System.exit(0);
    }
  }

}
