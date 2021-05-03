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
import org.waarp.openr66.protocol.exception.OpenR66ProtocolNoDataException;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolPacketException;
import org.waarp.openr66.protocol.localhandler.LocalChannelReference;
import org.waarp.openr66.protocol.localhandler.packet.AbstractLocalPacket;
import org.waarp.openr66.protocol.localhandler.packet.JsonCommandPacket;
import org.waarp.openr66.protocol.localhandler.packet.LocalPacketFactory;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.localhandler.packet.json.ConfigExportJsonPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.ChannelUtils;
import org.waarp.openr66.protocol.utils.R66Future;

import static org.waarp.openr66.database.DbConstantR66.*;

/**
 * Config Export from a local client without database connection
 */
public class ConfigExport implements Runnable {
  /**
   * Internal Logger
   */
  static volatile WaarpLogger logger;

  protected static final String INFO_ARGS =
      "Need at least the configuration file as first argument then at least one from\n" +
      "    -hosts\n" + "    -rules\n" + "    -business (if compatible)\n" +
      "    -alias (if compatible)\n" + "    -role (if compatible)\n" +
      "    -host host (optional)";

  protected final R66Future future;
  protected final boolean host;
  protected final boolean rule;
  protected final boolean business;
  protected final boolean alias;
  protected final boolean role;
  protected final NetworkTransaction networkTransaction;
  protected DbHostAuth dbhost;

  public ConfigExport(final R66Future future, final boolean host,
                      final boolean rule,
                      final NetworkTransaction networkTransaction) {
    this.future = future;
    this.host = host;
    this.rule = rule;
    business = false;
    alias = false;
    role = false;
    this.networkTransaction = networkTransaction;
    dbhost = Configuration.configuration.getHostSslAuth();
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ConfigExport.class);
    }
  }

  public ConfigExport(final R66Future future, final boolean host,
                      final boolean rule, final boolean business,
                      final boolean alias, final boolean role,
                      final NetworkTransaction networkTransaction) {
    this.future = future;
    this.host = host;
    this.rule = rule;
    this.business = business;
    this.alias = alias;
    this.role = role;
    this.networkTransaction = networkTransaction;
    dbhost = Configuration.configuration.getHostSslAuth();
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ConfigExport.class);
    }
  }

  public void setHost(final DbHostAuth host) {
    dbhost = host;
  }

  /**
   * Prior to call this method, the pipeline and NetworkTransaction must have
   * been initialized. It is the
   * responsibility of the caller to finish all network resources.
   */
  @Override
  public void run() {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ConfigExport.class);
    }
    if (!(host || rule || business || alias || role)) {
      logger.error("No action required");
      future.setResult(new R66Result(
          new OpenR66ProtocolNoDataException("No action required"), null, true,
          ErrorCode.IncorrectCommand, null));
      future.setFailure(future.getResult().getException());
      return;
    }
    final LocalChannelReference localChannelReference =
        AbstractTransfer.tryConnect(dbhost, future, networkTransaction);
    if (localChannelReference == null) {
      return;
    }
    localChannelReference.sessionNewState(R66FiniteDualStates.VALIDOTHER);
    final AbstractLocalPacket valid;
    final boolean useJson = PartnerConfiguration.useJson(dbhost.getHostid());
    logger.debug("UseJson: {}", useJson);
    if (useJson) {
      final ConfigExportJsonPacket node = new ConfigExportJsonPacket();
      node.setHost(host);
      node.setRule(rule);
      node.setBusiness(business);
      node.setAlias(alias);
      node.setRoles(role);
      valid = new JsonCommandPacket(node, LocalPacketFactory.CONFEXPORTPACKET);
    } else {
      valid = new ValidPacket(Boolean.toString(host), Boolean.toString(rule),
                              LocalPacketFactory.CONFEXPORTPACKET);
    }
    try {
      ChannelUtils.writeAbstractLocalPacket(localChannelReference, valid, true);
    } catch (final OpenR66ProtocolPacketException e) {
      logger.error("Bad Protocol", e);
      localChannelReference.close();
      dbhost = null;
      future.setResult(
          new R66Result(e, null, true, ErrorCode.TransferError, null));
      future.setFailure(e);
      return;
    }
    dbhost = null;
    future.awaitOrInterruptible();
    String sresult = "no information";
    if (future.isSuccess() && future.getResult() != null &&
        future.getResult().getOther() != null) {
      sresult = useJson?
          ((JsonCommandPacket) future.getResult().getOther()).getRequest() :
          future.getResult().getOther().toString();
    }
    logger.info("Config Export done with {} ({})",
                future.isSuccess()? "success" : "error", sresult);
    localChannelReference.close();
  }

  protected static boolean shost;
  protected static boolean srule;
  protected static boolean sbusiness;
  protected static boolean salias;
  protected static boolean srole;
  protected static String stohost;

  protected static boolean getParams(final String[] args) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ConfigExport.class);
    }
    if (args.length < 2) {
      logger.error(INFO_ARGS);
      return false;
    }
    if (!FileBasedConfiguration
        .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
      logger.error(INFO_ARGS);
      return false;
    }
    for (int i = 1; i < args.length; i++) {
      if ("-hosts".equalsIgnoreCase(args[i])) {
        shost = true;
      } else if ("-rules".equalsIgnoreCase(args[i])) {
        srule = true;
      } else if ("-business".equalsIgnoreCase(args[i])) {
        sbusiness = true;
      } else if ("-alias".equalsIgnoreCase(args[i])) {
        salias = true;
      } else if ("-roles".equalsIgnoreCase(args[i])) {
        srole = true;
      } else if ("-host".equalsIgnoreCase(args[i])) {
        i++;
        stohost = args[i];
      }
    }
    if (!shost && !srule) {
      logger.error("Need at least one of -hosts - rules");
      return false;
    }
    return true;
  }

  public static void main(final String[] args) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ConfigExport.class);
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
      final ConfigExport transaction =
          new ConfigExport(future, shost, srule, sbusiness, salias, srole,
                           networkTransaction);
      if (stohost != null) {
        try {
          transaction.setHost(new DbHostAuth(stohost));
        } catch (final WaarpDatabaseException e) {
          logger.error(
              "COnfigExport in     FAILURE since Host is not found: " + stohost,
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
      if (future.isSuccess()) {
        final boolean useJson = PartnerConfiguration.useJson(stohost);
        logger.debug("UseJson: {}", useJson);
        final String message;
        if (useJson) {
          message = result.getOther() != null?
              ((JsonCommandPacket) result.getOther()).getRequest() : "no file";
        } else {
          message = result.getOther() != null?
              ((ValidPacket) result.getOther()).getSheader() : "no file";
        }
        if (result.getCode() == ErrorCode.Warning) {
          logger
              .warn("WARNED on files:     " + message + "     delay: " + delay);
        } else {
          logger.warn(
              "SUCCESS on Final files:     " + message + "     delay: " +
              delay);
        }
      } else {
        if (result.getCode() == ErrorCode.Warning) {
          logger.warn("ConfigExport is     WARNED" + " : {}",
                      future.getCause() != null?
                          future.getCause().getMessage() : "");
        } else {
          logger.error("ConfigExport in     FAILURE" + " : {}",
                       future.getCause() != null?
                           future.getCause().getMessage() : "");
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
