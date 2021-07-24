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

import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.BlockRequestPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.ShutdownPacket;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;

import java.net.SocketAddress;

import static org.waarp.common.database.DbConstant.*;

/**
 * Local client to shutdown the server (using network)
 */
public class ServerShutdown {

  protected static final String INFO_ARGS =
      "Needs a correct configuration file as first argument and optionally [-nossl].\n" +
      "If '-block' or '-unblock' is specified, it will only block or unblock new request, but no shutdown will occur.\n" +
      "If '-restart' is specified, the server will shutdown then restart immediately";

  /**
   * @param args the configuration file as first argument
   *
   * @throws OpenR66ProtocolPacketException
   */
  public static void main(final String[] args)
      throws OpenR66ProtocolPacketException {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(null));
    final WaarpLogger logger =
        WaarpLoggerFactory.getLogger(ServerShutdown.class);
    if (args.length < 1) {
      logger.error(INFO_ARGS);
      WaarpSystemUtil.systemExit(1);
      return;
    }
    if (!FileBasedConfiguration.setConfigurationServerShutdownFromXml(
        Configuration.configuration, args[0])) {
      logger.error(INFO_ARGS);
      if (admin != null) {
        admin.close();
      }
      WaarpSystemUtil.systemExit(1);
      return;
    }
    boolean useSsl = true;
    boolean isblock = false;
    boolean isunblock = false;
    boolean isRestart = false;
    if (args.length > 1) {
      for (int i = 1; i < args.length; i++) {
        if ("-nossl".equalsIgnoreCase(args[i])) {
          useSsl = false;
        } else if ("-block".equalsIgnoreCase(args[i])) {
          isblock = true;
        } else if ("-unblock".equalsIgnoreCase(args[i])) {
          isunblock = true;
        } else if ("-restart".equalsIgnoreCase(args[i])) {
          isRestart = true;
        }
      }
    }
    final DbHostAuth host;
    if (useSsl) {
      host = Configuration.configuration.getHostSslAuth();
    } else {
      host = Configuration.configuration.getHostAuth();
    }
    if (host == null) {
      logger.error("Host id not found while SSL mode is : " + useSsl);
      if (admin != null) {
        admin.close();
      }
      WaarpSystemUtil.systemExit(1);
      return;
    }
    if (isunblock && isblock) {
      logger.error("Only one of '-block' or '-unblock' must be specified");
      if (admin != null) {
        admin.close();
      }
      WaarpSystemUtil.systemExit(1);
      return;
    }
    final byte[] key;
    key = FilesystemBasedDigest.passwdCrypt(
        Configuration.configuration.getServerAdminKey());
    final AbstractLocalPacket packet;
    if (isblock || isunblock) {
      packet = new BlockRequestPacket(isblock, key);
    } else {
      if (isRestart) {
        packet = new ShutdownPacket(key, (byte) 1);
      } else {
        packet = new ShutdownPacket(key);
      }
    }
    final SocketAddress socketServerAddress;
    try {
      socketServerAddress = host.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Needs a correct configuration file as first argument");
      WaarpSystemUtil.systemExit(1);
      return;
    }
    Configuration.configuration.pipelineInit();
    final NetworkTransaction networkTransaction = new NetworkTransaction();
    LocalChannelReference localChannelReference = null;
    localChannelReference =
        networkTransaction.createConnectionWithRetry(socketServerAddress,
                                                     useSsl, null);
    if (localChannelReference == null) {
      logger.error("Cannot connect to " + host.getHostid());
      networkTransaction.closeAll();
      return;
    }
    if (isblock || isunblock) {
      localChannelReference.sessionNewState(R66FiniteDualStates.BUSINESSR);
    } else {
      localChannelReference.sessionNewState(R66FiniteDualStates.SHUTDOWN);
    }
    ChannelUtils.writeAbstractLocalPacket(localChannelReference, packet, true);
    localChannelReference.getFutureRequest().awaitOrInterruptible();
    int value = 66;
    if (isblock || isunblock) {
      if (localChannelReference.getFutureRequest().isSuccess()) {
        logger.warn((isblock? "Blocking" : "Unblocking") + " OK");
        value = 0;
      } else {
        final R66Result result =
            localChannelReference.getFutureRequest().getResult();
        logger.error(
            "Cannot " + (isblock? "Blocking" : "Unblocking") + ": " + result,
            localChannelReference.getFutureRequest().getCause());
        value = result.getCode().ordinal();
      }
    } else {
      if (localChannelReference.getFutureRequest().isSuccess()) {
        logger.warn("Shutdown OK");
        value = 0;
      } else {
        final R66Result result =
            localChannelReference.getFutureRequest().getResult();
        if (result.getOther() instanceof ValidPacket &&
            ((ValidPacket) result.getOther()).getTypeValid() ==
            LocalPacketFactory.SHUTDOWNPACKET) {
          logger.warn("Shutdown command OK");
          value = 0;
        } else if (result.getCode() == ErrorCode.Shutdown) {
          logger.warn("Shutdown command done");
          value = 0;
        } else {
          logger.error("Cannot Shutdown: " + result,
                       localChannelReference.getFutureRequest().getCause());
          value = result.getCode().ordinal();
        }
      }
    }
    networkTransaction.closeAll();
    WaarpSystemUtil.systemExit(value);
  }

}
