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
package org.waarp.openr66.r66gui;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.waarp.common.database.exception.WaarpDatabaseException;
import org.waarp.common.database.exception.WaarpDatabaseNoConnectionException;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.Version;
import org.waarp.common.utility.WaarpSystemUtil;
import org.waarp.openr66.client.Message;
import org.waarp.openr66.configuration.FileBasedConfiguration;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.database.data.DbHostAuth;
import org.waarp.openr66.database.data.DbRule;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.configuration.Messages;
import org.waarp.openr66.protocol.localhandler.packet.TestPacket;
import org.waarp.openr66.protocol.localhandler.packet.ValidPacket;
import org.waarp.openr66.protocol.networkhandler.NetworkTransaction;
import org.waarp.openr66.protocol.utils.R66Future;

import javax.swing.JEditorPane;
import javax.swing.JProgressBar;

import static org.waarp.common.database.DbConstant.*;

/**
 *
 */
public class R66Environment {
  /**
   * Internal Logger
   */
  protected static volatile WaarpLogger logger;

  public String ruleId;
  public String hostId;
  public String information;
  public String filePath;
  public boolean isMD5;
  public boolean isInRequest;
  public boolean isClientSending;
  public NetworkTransaction networkTransaction;
  public String guiResultat;

  public void initLog() {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(R66ClientGui.class);
    }
  }

  public void initialize(final String[] args) {
    WaarpLoggerFactory
        .setDefaultFactoryIfNotSame(new WaarpSlf4JLoggerFactory(null));
    initLog();
    if (args.length < 1) {
      SysErrLogger.FAKE_LOGGER
          .syserr(Messages.getString("Configuration.WrongInit")); //$NON
      // -NLS-1$
      System.exit(2);//NOSONAR
    }
    if (!FileBasedConfiguration
        .setClientConfigurationFromXml(Configuration.configuration, args[0])) {
      logger.error(Messages.getString("Configuration.WrongInit")); //$NON-NLS-1$
      if (admin != null) {
        admin.close();
      }
      if (WaarpSystemUtil.isJunit()) {
        return;
      }
      WaarpSystemUtil.stopLogger(false);
      System.exit(2);//NOSONAR
    }
    Configuration.configuration.pipelineInit();
    networkTransaction = new NetworkTransaction();
  }

  public void exit() {
    if (networkTransaction != null) {
      networkTransaction.closeAll();
      networkTransaction = null;
    }
    // System.exit(0)
  }

  public void debug(final boolean isDebug) {
    final Logger logger =
        (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    if (isDebug) {
      logger.setLevel(Level.DEBUG); //NOSONAR
    } else {
      logger.setLevel(Level.WARN); //NOSONAR
    }
  }

  public boolean checkConnection() {
    final R66Future result = new R66Future(true);
    final TestPacket packet = new TestPacket("MSG", "TestConnection", 100);
    final Message transaction =
        new Message(networkTransaction, result, hostId, packet);
    transaction.run();
    result.awaitOrInterruptible(30000);
    if (result.isSuccess()) {
      final R66Result r66result = result.getResult();
      final ValidPacket info = (ValidPacket) r66result.getOther();
      guiResultat = Messages.getString("R66Environment.2") + //$NON-NLS-1$
                    info.getSheader();
    } else {
      guiResultat = Messages.getString("R66Environment.3") + //$NON-NLS-1$
                    result.getResult();
    }
    return result.isSuccess();
  }

  public boolean startsTransfer(final JProgressBar progressBar,
                                final JEditorPane textFieldStatus) {
    logger.debug("start startTransfer2");
    final long time1 = System.currentTimeMillis();
    final R66Future future = new R66Future(true);
    final ProgressDirectTransfer transaction =
        new ProgressDirectTransfer(future, hostId, filePath, ruleId,
                                   information, isMD5,
                                   Configuration.configuration.getBlockSize(),
                                   ILLEGALVALUE, networkTransaction, 500,
                                   progressBar, textFieldStatus);
    logger.info("Launch transfer: " + hostId + ':' + ruleId + ':' + filePath);
    transaction.run();
    future.awaitOrInterruptible();
    progressBar.setIndeterminate(true);
    progressBar.setValue(0);
    progressBar.setVisible(false);
    final long time2 = System.currentTimeMillis();
    final long delay = time2 - time1;
    final R66Result result = future.getResult();
    if (future.isSuccess()) {
      if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
        guiResultat = Messages.getString("R66Environment.8") + //$NON-NLS-1$
                      result.getRunner().toShortNoHtmlString("<br>") +
                      Messages.getString("R66Environment.10") + //$NON-NLS-2$
                      hostId + (result.getFile() != null?
            String.valueOf(result.getFile()) :
            Messages.getString("R66ClientGui.30"))
                      //$NON-NLS-2$
                      + Messages.getString("R66Environment.13") +
                      //$NON-NLS-1$
                      delay;
      } else {
        guiResultat = Messages.getString("R66Environment.14") + //$NON-NLS-1$
                      result.getRunner().toShortNoHtmlString("<br>") +
                      Messages.getString("R66Environment.10") + //$NON-NLS-2$
                      hostId + (result.getFile() != null?
            String.valueOf(result.getFile()) :
            Messages.getString("R66ClientGui.30"))
                      //$NON-NLS-2$
                      + Messages.getString("R66Environment.13") +
                      //$NON-NLS-1$
                      delay;
      }
    } else {
      if (result == null || result.getRunner() == null) {
        guiResultat = Messages.getString("R66Environment.20") + //$NON-NLS-1$
                      Messages.getString("R66Environment.10") + //$NON-NLS-1$
                      hostId + "     " + future.getCause().getMessage();
      } else if (result.getRunner().getErrorInfo() == ErrorCode.Warning) {
        guiResultat = Messages.getString("R66Environment.23") + //$NON-NLS-1$
                      result.getRunner().toShortNoHtmlString("<br>") +
                      Messages.getString("R66Environment.10") + //$NON-NLS-2$
                      hostId + "    " + future.getCause().getMessage();
      } else {
        guiResultat = Messages.getString("R66Environment.27") + //$NON-NLS-1$
                      result.getRunner().toShortNoHtmlString("<br>") +
                      Messages.getString("R66Environment.10") + //$NON-NLS-2$
                      hostId + "    " + future.getCause().getMessage();
      }
    }
    return future.isSuccess();
  }

  public static String[] getHostIds() {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(R66ClientGui.class);
    }
    final String[] results;
    final DbHostAuth[] dbHostAuths;
    try {
      dbHostAuths = DbHostAuth.getAllHosts();
    } catch (final WaarpDatabaseNoConnectionException e) {
      results = new String[1];
      results[0] = Messages.getString("R66Environment.31"); //$NON-NLS-1$
      return results;
    }
    if (dbHostAuths.length == 0) {
      results = new String[1];
      results[0] = Messages.getString("R66Environment.31"); //$NON-NLS-1$
      return results;
    }
    results = new String[dbHostAuths.length];
    for (int i = 0; i < dbHostAuths.length; i++) {
      results[i] = dbHostAuths[i].getHostid();
    }
    return results;
  }

  public static String[] getRules() {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(R66ClientGui.class);
    }
    final String[] results;
    final DbRule[] dbRules;
    try {
      dbRules = DbRule.getAllRules();
    } catch (final WaarpDatabaseNoConnectionException e) {
      results = new String[1];
      results[0] = Messages.getString("R66Environment.34"); //$NON-NLS-1$
      return results;
    }
    if (dbRules.length == 0) {
      results = new String[1];
      results[0] = Messages.getString("R66Environment.34"); //$NON-NLS-1$
      return results;
    }
    results = new String[dbRules.length];
    for (int i = 0; i < dbRules.length; i++) {
      results[i] = dbRules[i].getIdRule();
    }
    return results;
  }

  public static String[] getRules(final boolean sendMode) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(R66ClientGui.class);
    }
    final String[] results;
    final DbRule[] dbRules;
    try {
      dbRules = DbRule.getAllRules();
    } catch (final WaarpDatabaseNoConnectionException e) {
      results = new String[1];
      results[0] = Messages.getString("R66Environment.34"); //$NON-NLS-1$
      return results;
    }
    int len = 0;
    for (final DbRule rule : dbRules) {
      if (sendMode) {
        if (rule.isSendMode()) {
          len++;
        }
      } else {
        if (rule.isRecvMode()) {
          len++;
        }
      }
    }
    if (len == 0) {
      results = new String[1];
      results[0] = Messages.getString("R66Environment.34"); //$NON-NLS-1$
      return results;
    }
    results = new String[len];
    int i = 0;
    for (final DbRule rule : dbRules) {
      if (sendMode) {
        if (rule.isSendMode()) {
          results[i] = rule.getIdRule();
          i++;
        }
      } else {
        if (rule.isRecvMode()) {
          results[i] = rule.getIdRule();
          i++;
        }
      }
    }
    return results;
  }

  public static String getHost(final String id) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(R66ClientGui.class);
    }
    DbHostAuth host = null;
    try {
      host = new DbHostAuth(id);
    } catch (final WaarpDatabaseException ignored) {
      // nothing
    }
    if (host != null) {
      final String hosthtml =
          "<table border=1 cellpadding=0 cellspacing=0 style=border-collapse: collapse bordercolor=#111111 width=100% id=AutoNumber1>" +
          "<tr><td width=13% align=center><b>Host ID</b></td><td width=13% align=center><b>Address</b></td>" +
          "<td width=13% align=center><b>Port</b></td><td width=5% align=center><b>SSL</b></td>" +
          "<td width=13% align=center><b>HostKey</b></td><td width=5% align=center><b>Admin Role</b></td>" +
          "<td width=5% align=center><b>IsClient</b></td></tr>" +
          "<tr><td width=13% align=center>XXXHOSTXXX</td>" +
          "<td width=13% align=center>XXXADDRXXX</td>" +
          "<td width=13% align=center>XXXPORTXXX</td>" +
          "<td width=5% align=center><input type=checkbox name=ssl value=on XXXSSLXXX disabled readonly></td>" +
          "<td width=13% align=center>XXXKEYXXX</td>" +
          "<td width=5% align=center><input type=checkbox name=admin value=on XXXADMXXX disabled readonly></td>" +
          "<td width=5% align=center><input type=checkbox name=isclient value=on XXXISCXXX disabled readonly></td></tr></table>";
      return host.toSpecializedHtml(null, hosthtml, true);
    }
    return "HostId: " + id;
  }

  public static String getRule(final String id) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(R66ClientGui.class);
    }
    DbRule rule = null;
    try {
      rule = new DbRule(id);
    } catch (final WaarpDatabaseException ignored) {
      // nothing
    }
    if (rule != null) {
      final String rulehtml =
          "<table border=1 cellpadding=0 cellspacing=0 style=border-collapse: collapse bordercolor=#111111 width=100% id=AutoNumber1>" +
          "<tr><td width=6% align=center><b>Rule Id</b></td><td width=6% align=center><b>Mode</b></td>" +
          "<td width=6% align=center><b>Host Ids</b></td><td width=6% align=center><b>RecvPath</b></td>" +
          "<td width=6% align=center><b>SendPath</b></td><td width=7% align=center><b>ArchivePath</b></td>" +
          "<td width=7% align=center><b>WorkPath</b></td></tr><tr>" +
          "<td width=6% align=center>XXXRULEXXX</td>" +
          "<td width=6% align=center><input type=radio value=send name=mode XXXSENDXXX disabled>SEND" +
          "<input type=radio name=mode value=recv XXXRECVXXX disabled readonly>RECV<br>" +
          "<input type=radio name=mode value=sendmd5 XXXSENDMXXX disabled readonly>SENDMD5" +
          "<input type=radio name=mode value=recvmd5 XXXRECVMXXX disabled readonly>RECVMD5<br>" +
          "<input type=radio value=sendth name=mode XXXSENDTXXX disabled readonly>SENDTHROUGH<br>" +
          "<input type=radio name=mode value=recvth XXXRECVTXXX disabled readonly>RECVTHROUGH<br>" +
          "<input type=radio name=mode value=sendthmd5 XXXSENDMTXXX disabled readonly>SENDMD5THROUGH<br>" +
          "<input type=radio name=mode value=recvthmd5 XXXRECVMTXXX disabled readonly>RECVMD5THROUGH</td>" +
          "<td width=6% align=center><PRE>XXXIDSXXX</PRE></td>" +
          "<td width=6% align=center><PRE>XXXRPXXX</PRE></td>" +
          "<td width=6% align=center><PRE>XXXSPXXX</PRE></td>" +
          "<td width=7% align=center><PRE>XXXAPXXX</PRE></td>" +
          "<td width=7% align=center><PRE>XXXWPXXX</PRE></td></tr><tr>" +
          "<td width=7% align=center><b>Recv Pre</b></td><td width=7% align=center><b>Recv Post</b></td>" +
          "<td width=7% align=center><b>Recv Error</b></td><td width=7% align=center><b>Send Pre</b></td>" +
          "<td width=7% align=center><b>Send Post</b></td><td width=7% align=center><b>Send Error</b></td>" +
          "<td width=8%>&nbsp;</td></tr><tr>" +
          "<td width=7%><PRE>XXXRPTXXX</PRE></td>" +
          "<td width=7%><PRE>XXXRSTXXX</PRE></td>" +
          "<td width=7%><PRE>XXXRETXXX</PRE></td>" +
          "<td width=7%><PRE>XXXSPTXXX</PRE></td>" +
          "<td width=8%><PRE>XXXSSTXXX</PRE></td>" +
          "<td width=8%><PRE>XXXSETXXX</PRE></td>" + "</tr></table>";
      return rule.toSpecializedHtml(null, rulehtml);
    }
    return "RuleId: " + id;
  }

  public void about() {
    guiResultat =
        "<HTML><P ALIGN=CENTER><FONT SIZE=5 STYLE=\"font-size: 22pt\"><SPAN>R66 Client GUI Version: " +
        Version.fullIdentifier() + "</SPAN></FONT></P>" +
        "<P ALIGN=CENTER><FONT SIZE=4 STYLE=\"font-size: 16pt\"><SPAN>This graphical user interface is intend to provide an easy way to use R66 for:</SPAN></FONT></P>" +
        "<UL><LI><P ALIGN=LEFT><FONT SIZE=4 STYLE=\"font-size: 16pt\"><SPAN>Testing new Rules, Hosts or connectivity</SPAN></FONT></P>" +
        "<LI><P ALIGN=LEFT<FONT SIZE=4 STYLE=\"font-size: 16pt\"><SPAN>Exchanging files between a PC and a R66 Server</SPAN></FONT></P>" +
        "<LI><P ALIGN=LEFT<FONT SIZE=4 STYLE=\"font-size: 16pt\"><SPAN>Provide an example on how to use the R66 API in an application</SPAN></FONT></P>" +
        "</UL>";
  }

  @Override
  public String toString() {
    return "Env: " + ruleId + ':' + hostId + ':' + filePath + ':' + isMD5 +
           ':' + isInRequest + ':' + isClientSending + ':' + information + ':' +
           guiResultat;
  }
}
