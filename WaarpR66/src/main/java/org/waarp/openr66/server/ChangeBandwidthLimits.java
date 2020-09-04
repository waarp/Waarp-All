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
import org.waarp.openr66.client.AbstractTransfer;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66FiniteDualStates;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.PartnerConfiguration;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.BandwidthJsonPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.R66Future;

import static org.waarp.common.database.DbConstant.*;

/**
 * This command enables the dynamic change of bandwidth limitation. It does not
 * changed the valuesin the
 * database but only dynamic values while the server is running and until it is
 * shutdown.
 */
public class ChangeBandwidthLimits implements Runnable {
  /**
   * Internal Logger
   */
  static volatile WaarpLogger logger;

  protected static final String INFO_ARGS =
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

  public ChangeBandwidthLimits(final R66Future future, final long wgl,
                               final long rgl, final long wsl, final long rsl,
                               final NetworkTransaction networkTransaction) {
    this.future = future;
    writeGlobalLimit = wgl;
    readGlobalLimit = rgl;
    writeSessionLimit = wsl;
    readSessionLimit = rsl;
    this.networkTransaction = networkTransaction;
    host = Configuration.configuration.getHostSslAuth();
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ChangeBandwidthLimits.class);
    }
  }

  public void setHost(final DbHostAuth host) {
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
    final LocalChannelReference localChannelReference =
        AbstractTransfer.tryConnect(host, future, networkTransaction);
    if (localChannelReference == null) {
      return;
    }
    localChannelReference.sessionNewState(R66FiniteDualStates.VALIDOTHER);
    final AbstractLocalPacket valid;
    final boolean useJson = PartnerConfiguration.useJson(host.getHostid());
    logger.debug("UseJson: " + useJson);
    if (useJson) {
      final BandwidthJsonPacket node = new BandwidthJsonPacket();
      if (writeGlobalLimit < 0 && readGlobalLimit < 0 &&
          writeSessionLimit < 0 && readSessionLimit < 0) {
        // will ask current values instead
        node.setSetter(false);
      } else {
        node.setSetter(true);
        node.setWriteglobal(writeGlobalLimit);
        node.setReadglobal(readGlobalLimit);
        node.setWritesession(writeSessionLimit);
        node.setReadsession(readSessionLimit);
      }
      valid = new JsonCommandPacket(node, LocalPacketFactory.BANDWIDTHPACKET);
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
    AbstractTransfer
        .sendValidPacket(host, localChannelReference, valid, future);
    logger
        .info("Request done with " + (future.isSuccess()? "success" : "error"));
  }

  protected static long swriteGlobalLimit = -1;
  protected static long sreadGlobalLimit = -1;
  protected static long swriteSessionLimit = -1;
  protected static long sreadSessionLimit = -1;
  protected static String stohost;

  protected static boolean getParams(final String[] args) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ChangeBandwidthLimits.class);
    }
    if (args.length < 3) {
      logger.error(INFO_ARGS);
      return false;
    }
    if (!FileBasedConfiguration
        .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
      logger.error(INFO_ARGS);
      return false;
    }
    for (int i = 1; i < args.length; i++) {
      try {
        if ("-wglob".equalsIgnoreCase(args[i])) {
          i++;
          swriteGlobalLimit = Long.parseLong(args[i]);
        } else if ("-rglob".equalsIgnoreCase(args[i])) {
          i++;
          sreadGlobalLimit = Long.parseLong(args[i]);
        } else if ("-wsess".equalsIgnoreCase(args[i])) {
          i++;
          swriteSessionLimit = Long.parseLong(args[i]);
        } else if ("-rsess".equalsIgnoreCase(args[i])) {
          i++;
          sreadSessionLimit = Long.parseLong(args[i]);
        } else if ("-host".equalsIgnoreCase(args[i])) {
          i++;
          stohost = args[i];
        }
      } catch (final NumberFormatException ignored) {
        // nothing
      }
    }
    if (swriteGlobalLimit == -1 && sreadGlobalLimit == -1 &&
        swriteSessionLimit == -1 && sreadSessionLimit == -1) {
      logger.error(INFO_ARGS);
      return false;
    }
    return true;
  }

  public static void main(final String[] args) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ChangeBandwidthLimits.class);
    }
    if (!getParams(args)) {
      logger.error("Wrong initialization");
      if (admin != null) {
        admin.close();
      }
      System.exit(1);//NOSONAR
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
          System.exit(10);//NOSONAR
        }
      } else {
        stohost = Configuration.configuration.getHostSslId();
      }
      transaction.run();
      future.awaitOrInterruptible();
      final long time2 = System.currentTimeMillis();
      final long delay = time2 - time1;
      final R66Result result = future.getResult();
      final boolean useJson = PartnerConfiguration.useJson(stohost);
      logger.debug("UseJson: " + useJson);
      if (future.isSuccess()) {
        final String sresult;
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
        } else {
          logger.error("Bandwidth in     FAILURE", future.getCause());
        }
        networkTransaction.closeAll();
        System.exit(result.getCode().ordinal());//NOSONAR
      }
    } finally {
      networkTransaction.closeAll();
      System.exit(0);//NOSONAR
    }
  }

}
