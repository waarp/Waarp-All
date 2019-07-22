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

import java.net.SocketAddress;

/**
 * Config Export from a local client without database connection
 *
 *
 */
public class ConfigExport implements Runnable {
  /**
   * Internal Logger
   */
  static volatile WaarpLogger logger;

  protected static String _INFO_ARGS =
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

  public ConfigExport(R66Future future, boolean host, boolean rule,
                      NetworkTransaction networkTransaction) {
    this.future = future;
    this.host = host;
    this.rule = rule;
    business = false;
    alias = false;
    role = false;
    this.networkTransaction = networkTransaction;
    dbhost = Configuration.configuration.getHOST_SSLAUTH();
  }

  public ConfigExport(R66Future future, boolean host, boolean rule,
                      boolean business, boolean alias, boolean role,
                      NetworkTransaction networkTransaction) {
    this.future = future;
    this.host = host;
    this.rule = rule;
    this.business = business;
    this.alias = alias;
    this.role = role;
    this.networkTransaction = networkTransaction;
    dbhost = Configuration.configuration.getHOST_SSLAUTH();
  }

  public void setHost(DbHostAuth host) {
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
    SocketAddress socketAddress;
    try {
      socketAddress = dbhost.getSocketAddress();
    } catch (final IllegalArgumentException e) {
      logger.error("Cannot Connect to " + dbhost.getHostid());
      future.setResult(new R66Result(new OpenR66ProtocolNoConnectionException(
          "Cannot connect to server " + dbhost.getHostid()), null, true,
                                     ErrorCode.ConnectionImpossible, null));
      dbhost = null;
      future.setFailure(future.getResult().getException());
      return;
    }
    final boolean isSSL = dbhost.isSsl();

    LocalChannelReference localChannelReference = networkTransaction
        .createConnectionWithRetry(socketAddress, isSSL, future);
    socketAddress = null;
    if (localChannelReference == null) {
      logger.error("Cannot Connect to " + dbhost.getHostid());
      future.setResult(new R66Result(new OpenR66ProtocolNoConnectionException(
          "Cannot connect to server " + dbhost.getHostid()), null, true,
                                     ErrorCode.ConnectionImpossible, null));
      dbhost = null;
      future.setFailure(future.getResult().getException());
      return;
    }
    localChannelReference.sessionNewState(R66FiniteDualStates.VALIDOTHER);
    AbstractLocalPacket valid = null;
    final boolean useJson = PartnerConfiguration.useJson(dbhost.getHostid());
    logger.debug("UseJson: " + useJson);
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
      ChannelUtils
          .writeAbstractLocalPacket(localChannelReference, valid, false);
    } catch (final OpenR66ProtocolPacketException e) {
      logger.error("Bad Protocol", e);
      localChannelReference.getLocalChannel().close();
      localChannelReference = null;
      dbhost = null;
      valid = null;
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
      sresult = (useJson?
          ((JsonCommandPacket) future.getResult().getOther()).getRequest() :
          ((ValidPacket) future.getResult().getOther()).toString());
    }
    logger.info(
        "Config Export done with " + (future.isSuccess()? "success" : "error") +
        " (" + sresult + ")");
    localChannelReference.getLocalChannel().close();
    localChannelReference = null;
  }

  protected static boolean shost = false;
  protected static boolean srule = false;
  protected static boolean sbusiness = false;
  protected static boolean salias = false;
  protected static boolean srole = false;
  protected static String stohost = null;

  protected static boolean getParams(String[] args) {
    if (args.length < 2) {
      logger.error(_INFO_ARGS);
      return false;
    }
    if (!FileBasedConfiguration
        .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
      logger.error(_INFO_ARGS);
      return false;
    }
    for (int i = 1; i < args.length; i++) {
      if (args[i].equalsIgnoreCase("-hosts")) {
        shost = true;
      } else if (args[i].equalsIgnoreCase("-rules")) {
        srule = true;
      } else if (args[i].equalsIgnoreCase("-business")) {
        sbusiness = true;
      } else if (args[i].equalsIgnoreCase("-alias")) {
        salias = true;
      } else if (args[i].equalsIgnoreCase("-roles")) {
        srole = true;
      } else if (args[i].equalsIgnoreCase("-host")) {
        i++;
        stohost = args[i];
      }
    }
    if ((!shost) && (!srule)) {
      logger.error("Need at least one of -hosts - rules");
      return false;
    }
    return true;
  }

  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ConfigExport.class);
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
      if (future.isSuccess()) {
        final boolean useJson = PartnerConfiguration.useJson(stohost);
        logger.debug("UseJson: " + useJson);
        String message = null;
        if (useJson) {
          message = (result.getOther() != null?
              ((JsonCommandPacket) result.getOther()).getRequest() : "no file");
        } else {
          message = (result.getOther() != null?
              ((ValidPacket) result.getOther()).getSheader() : "no file");
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
          logger.warn("ConfigExport is     WARNED", future.getCause());
          networkTransaction.closeAll();
          System.exit(result.getCode().ordinal());
        } else {
          logger.error("ConfigExport in     FAILURE", future.getCause());
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
