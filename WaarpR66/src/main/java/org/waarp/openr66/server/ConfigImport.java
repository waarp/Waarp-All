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
import org.waarp.openr66.protocol.localhandler.packet.json.ConfigImportJsonPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.R66Future;

import static org.waarp.common.database.DbConstant.*;

/**
 * Config Import from a local client without database connection
 */
public class ConfigImport implements Runnable {
  /**
   * Internal Logger
   */
  static volatile WaarpLogger logger;

  protected static final String _INFO_ARGS =
      "Need at least the configuration file as first argument then at least one from\n" +
      "    -hosts file\n" + "    -rules file\n" +
      "    -business file (if compatible)\n" +
      "    -alias file (if compatible)\n" +
      "    -roles file (if compatible)\n" + "    -purgehosts\n" +
      "    -purgerules\n" + "    -purgebusiness (if compatible)\n" +
      "    -purgealias (if compatible)\n" +
      "    -purgeroles (if compatible)\n" +
      "    -hostid file transfer id (if compatible)\n" +
      "    -ruleid file transfer id (if compatible)\n" +
      "    -businessid file transfer id (if compatible)\n" +
      "    -aliasid file transfer id (if compatible)\n" +
      "    -roleid file transfer id (if compatible)\n" +
      "    -host host (optional)";

  protected final R66Future future;
  protected final String host;
  protected final boolean hostPurge;
  protected final String rule;
  protected final boolean rulePurge;
  protected final String business;
  protected final boolean businessPurge;
  protected final String alias;
  protected final boolean aliasPurge;
  protected final String role;
  protected final boolean rolePurge;
  protected long hostid = ILLEGALVALUE;
  protected long ruleid = ILLEGALVALUE;
  protected long businessid = ILLEGALVALUE;
  protected long aliasid = ILLEGALVALUE;
  protected long roleid = ILLEGALVALUE;
  protected final NetworkTransaction networkTransaction;
  protected DbHostAuth dbhost;

  public ConfigImport(R66Future future, boolean hostPurge, boolean rulePurge,
                      String host, String rule,
                      NetworkTransaction networkTransaction) {
    this.future = future;
    this.host = host;
    this.rule = rule;
    this.hostPurge = hostPurge;
    this.rulePurge = rulePurge;
    business = null;
    businessPurge = false;
    alias = null;
    aliasPurge = false;
    role = null;
    rolePurge = false;
    this.networkTransaction = networkTransaction;
    dbhost = Configuration.configuration.getHostSslAuth();
  }

  public ConfigImport(R66Future future, boolean hostPurge, boolean rulePurge,
                      boolean businessPurge, boolean aliasPurge,
                      boolean rolePurge, String host, String rule,
                      String business, String alias, String role,
                      NetworkTransaction networkTransaction) {
    this.future = future;
    this.host = host;
    this.rule = rule;
    this.hostPurge = hostPurge;
    this.rulePurge = rulePurge;
    this.business = business;
    this.businessPurge = businessPurge;
    this.alias = alias;
    this.aliasPurge = aliasPurge;
    this.role = role;
    this.rolePurge = rolePurge;
    this.networkTransaction = networkTransaction;
    dbhost = Configuration.configuration.getHostSslAuth();
  }

  /**
   * Used when the filenames are not compliant with remote filenames.
   *
   * @param hostid
   * @param ruleid
   * @param businessid
   * @param aliasid
   * @param roleid
   */
  public void setSpecialIds(long hostid, long ruleid, long businessid,
                            long aliasid, long roleid) {
    this.hostid = hostid;
    this.ruleid = ruleid;
    this.businessid = businessid;
    this.aliasid = aliasid;
    this.roleid = roleid;
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
      logger = WaarpLoggerFactory.getLogger(ConfigImport.class);
    }
    LocalChannelReference localChannelReference =
        AbstractTransfer.tryConnect(dbhost, future, networkTransaction);
    if (localChannelReference == null) {
      return;
    }
    localChannelReference.sessionNewState(R66FiniteDualStates.VALIDOTHER);
    final boolean useJson = PartnerConfiguration.useJson(dbhost.getHostid());
    logger.debug("UseJson: " + useJson);
    AbstractLocalPacket valid;
    if (useJson) {
      final ConfigImportJsonPacket node = new ConfigImportJsonPacket();
      node.setHost(host);
      node.setRule(rule);
      node.setBusiness(business);
      node.setAlias(alias);
      node.setRoles(role);
      node.setPurgehost(hostPurge);
      node.setPurgerule(rulePurge);
      node.setPurgebusiness(businessPurge);
      node.setPurgealias(aliasPurge);
      node.setPurgeroles(rolePurge);
      node.setHostid(hostid);
      node.setRuleid(ruleid);
      node.setBusinessid(businessid);
      node.setAliasid(aliasid);
      node.setRolesid(roleid);
      valid = new JsonCommandPacket(node, LocalPacketFactory.CONFIMPORTPACKET);
    } else {
      valid = new ValidPacket((hostPurge? "1 " : "0 ") + host,
                              (rulePurge? "1 " : "0 ") + rule,
                              LocalPacketFactory.CONFIMPORTPACKET);
    }
    AbstractTransfer
        .sendValidPacket(dbhost, localChannelReference, valid, future);
    logger.debug(
        "Request done with " + (future.isSuccess()? "success" : "error"));
  }

  protected static String shost;
  protected static String srule;
  protected static String sbusiness;
  protected static String salias;
  protected static String srole;
  protected static boolean shostpurge;
  protected static boolean srulepurge;
  protected static boolean sbusinesspurge;
  protected static boolean saliaspurge;
  protected static boolean srolepurge;
  protected static String stohost;
  protected static long lhost = ILLEGALVALUE;
  protected static long lrule = ILLEGALVALUE;
  protected static long lbusiness = ILLEGALVALUE;
  protected static long lalias = ILLEGALVALUE;
  protected static long lrole = ILLEGALVALUE;

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
      if ("-hosts".equalsIgnoreCase(args[i])) {
        i++;
        if (args.length <= i) {
          return false;
        }
        shost = args[i];
      } else if ("-rules".equalsIgnoreCase(args[i])) {
        i++;
        if (args.length <= i) {
          return false;
        }
        srule = args[i];
      } else if ("-business".equalsIgnoreCase(args[i])) {
        i++;
        if (args.length <= i) {
          return false;
        }
        sbusiness = args[i];
      } else if ("-alias".equalsIgnoreCase(args[i])) {
        i++;
        if (args.length <= i) {
          return false;
        }
        salias = args[i];
      } else if ("-roles".equalsIgnoreCase(args[i])) {
        i++;
        if (args.length <= i) {
          return false;
        }
        srole = args[i];
      } else if ("-purgehosts".equalsIgnoreCase(args[i])) {
        shostpurge = true;
      } else if ("-purgerules".equalsIgnoreCase(args[i])) {
        srulepurge = true;
      } else if ("-purgebusiness".equalsIgnoreCase(args[i])) {
        sbusinesspurge = true;
      } else if ("-purgealias".equalsIgnoreCase(args[i])) {
        saliaspurge = true;
      } else if ("-purgeroles".equalsIgnoreCase(args[i])) {
        srolepurge = true;
      } else if ("-host".equalsIgnoreCase(args[i])) {
        i++;
        stohost = args[i];
      } else if ("-hostid".equalsIgnoreCase(args[i])) {
        i++;
        if (args.length <= i) {
          return false;
        }
        try {
          lhost = Long.parseLong(args[i]);
        } catch (final NumberFormatException e) {
          return false;
        }
      } else if ("-ruleid".equalsIgnoreCase(args[i])) {
        i++;
        if (args.length <= i) {
          return false;
        }
        try {
          lrule = Long.parseLong(args[i]);
        } catch (final NumberFormatException e) {
          return false;
        }
      } else if ("-businessid".equalsIgnoreCase(args[i])) {
        i++;
        if (args.length <= i) {
          return false;
        }
        try {
          lbusiness = Long.parseLong(args[i]);
        } catch (final NumberFormatException e) {
          return false;
        }
      } else if ("-aliasid".equalsIgnoreCase(args[i])) {
        i++;
        if (args.length <= i) {
          return false;
        }
        try {
          lalias = Long.parseLong(args[i]);
        } catch (final NumberFormatException e) {
          return false;
        }
      } else if ("-roleid".equalsIgnoreCase(args[i])) {
        i++;
        if (args.length <= i) {
          return false;
        }
        try {
          lrole = Long.parseLong(args[i]);
        } catch (final NumberFormatException e) {
          return false;
        }
      }
    }
    return true;
  }

  public static void main(String[] args) {
    WaarpLoggerFactory.setDefaultFactory(new WaarpSlf4JLoggerFactory(null));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(ConfigImport.class);
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
      final ConfigImport transaction =
          new ConfigImport(future, shostpurge, srulepurge, sbusinesspurge,
                           saliaspurge, srolepurge, shost, srule, sbusiness,
                           salias, srole, networkTransaction);
      transaction.setSpecialIds(lhost, lrule, lbusiness, lalias, lrole);
      if (stohost != null) {
        try {
          transaction.setHost(new DbHostAuth(stohost));
        } catch (final WaarpDatabaseException e) {
          logger.error(
              "ConfigImport in     FAILURE since Host is not found: " + stohost,
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
        logger.debug("UseJson: " + useJson);
        String message;
        if (useJson) {
          message = result.getOther() != null?
              ((JsonCommandPacket) result.getOther()).getRequest() : "no file";
        } else {
          message = result.getOther() != null?
              ((ValidPacket) result.getOther()).getSheader() : "no file";
        }
        if (result.getCode() == ErrorCode.Warning) {
          logger.warn(
              "WARNED on import:     " + message + "     delay: " + delay);
        } else {
          logger.warn(
              "SUCCESS on import:     " + message + "     delay: " + delay);
        }
      } else {
        if (result.getCode() == ErrorCode.Warning) {
          logger.warn("ConfigImport is     WARNED", future.getCause());
        } else {
          logger.error("ConfigImport in     FAILURE", future.getCause());
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
