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
package org.waarp.ftp.simpleimpl.control;

import io.netty.channel.Channel;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply502Exception;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.ftp.core.command.AbstractCommand;
import org.waarp.ftp.core.command.FtpCommandCode;
import org.waarp.ftp.core.command.service.MKD;
import org.waarp.ftp.core.control.BusinessHandler;
import org.waarp.ftp.core.data.FtpTransfer;
import org.waarp.ftp.core.session.FtpSession;
import org.waarp.ftp.filesystembased.FilesystemBasedFtpRestart;
import org.waarp.ftp.simpleimpl.file.FileBasedAuth;
import org.waarp.ftp.simpleimpl.file.FileBasedDir;

/**
 * BusinessHandler implementation that allows pre and post actions on any
 * operations and specifically on
 * transfer operations
 */
public class SimpleBusinessHandler extends BusinessHandler {
  private static final String GBBH_TRANSFER = "GBBH: Transfer: {} ";
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(SimpleBusinessHandler.class);

  @Override
  public void afterRunCommandKo(final CommandAbstractException e) {
    // TO DO Auto-generated method stub
    if (getFtpSession().getCurrentCommand() instanceof MKD) {
      // do nothing
    } else {
      logger.debug("GBBH: AFTKO: {} {}", getFtpSession(), e.getMessage());
    }
  }

  @Override
  public void afterRunCommandOk() throws CommandAbstractException {
    // TO DO Auto-generated method stub
    // logger.info("GBBH: AFTOK: {}", getFtpSession())
  }

  @Override
  public void beforeRunCommand() throws CommandAbstractException {
    // TO DO Auto-generated method stub
    // logger.info("GBBH: BEFCD: {}", getFtpSession())
  }

  @Override
  protected void cleanSession() {
    // TO DO Auto-generated method stub
    // logger.info("GBBH: CLNSE: {}", getFtpSession())
  }

  @Override
  public void exceptionLocalCaught(final Throwable e) {
    // TO DO Auto-generated method stub
    logger.warn("GBBH: EXCEP: {} {}", getFtpSession(), e.getMessage());
  }

  @Override
  public void executeChannelClosed() {
    // TO DO Auto-generated method stub
    // logger.info("GBBH: CLOSED: for user {} with session {} ",
    // getFtpSession().getAuth().getUser(), getFtpSession())
  }

  @Override
  public void executeChannelConnected(final Channel channel) {
    // TO DO Auto-generated method stub
    // logger.info("GBBH: CONNEC: {}", getFtpSession())
  }

  @Override
  public final FileBasedAuth getBusinessNewAuth() {
    return new FileBasedAuth(getFtpSession());
  }

  @Override
  public final FileBasedDir getBusinessNewDir() {
    return new FileBasedDir(getFtpSession());
  }

  @Override
  public final FilesystemBasedFtpRestart getBusinessNewRestart() {
    return new FilesystemBasedFtpRestart(getFtpSession());
  }

  @Override
  public final String getHelpMessage(final String arg) {
    return "This FTP server is only intend as a Gateway.\n" +
           "This FTP server refers to RFC 959, 775, 2389, 2428, 3659, 4217 " +
           "and supports XDIGEST, XCRC, XMD5 and XSHA1 commands.\n" +
           "XCRC, XMD5 and XSHA1 take a simple filename as argument, XDIGEST " +
           "taking algorithm (among CRC32, ADLER32, MD5, MD2, " +
           "SHA-1, SHA-256, SHA-384, SHA-512) followed by filename " +
           "as arguments," +
           " and return \"250 digest-value is the digest of filename\".";
  }

  @Override
  public final String getFeatMessage() {
    final StringBuilder builder =
        new StringBuilder("Extensions supported:").append('\n').append(
            getDefaultFeatMessage());
    if (getFtpSession().getConfiguration().getFtpInternalConfiguration()
                       .isAcceptAuthProt()) {
      builder.append('\n').append(getSslFeatMessage());
    }
    builder.append("\nEnd");
    return builder.toString();
  }

  @Override
  public final String getOptsMessage(final String[] args)
      throws CommandAbstractException {
    if (args.length > 0) {
      if (args[0].equalsIgnoreCase(FtpCommandCode.MLST.name()) ||
          args[0].equalsIgnoreCase(FtpCommandCode.MLSD.name())) {
        return getMLSxOptsMessage(args);
      }
      throw new Reply502Exception("OPTS not implemented for " + args[0]);
    }
    throw new Reply502Exception("OPTS not implemented");
  }

  @Override
  public AbstractCommand getSpecializedSiteCommand(final FtpSession session,
                                                   final String line) {
    return null;
  }

  @Override
  public final void afterTransferDoneBeforeAnswer(final FtpTransfer transfer)
      throws CommandAbstractException {
    if (transfer.getCommand() == FtpCommandCode.APPE) {
      logger.info(GBBH_TRANSFER + "{} {}", transfer.getCommand(),
                  transfer.getStatus(), transfer.getPath());
    } else if (transfer.getCommand() == FtpCommandCode.RETR) {
      logger.info(GBBH_TRANSFER + "{} {}", transfer.getCommand(),
                  transfer.getStatus(), transfer.getPath());
    } else if (transfer.getCommand() == FtpCommandCode.STOR) {
      logger.info(GBBH_TRANSFER + "{} {}", transfer.getCommand(),
                  transfer.getStatus(), transfer.getPath());
    } else if (transfer.getCommand() == FtpCommandCode.STOU) {
      logger.info(GBBH_TRANSFER + "{} {}", transfer.getCommand(),
                  transfer.getStatus(), transfer.getPath());
    } else {
      logger.warn("GBBH: Transfer unknown: {} {} {}", transfer.getCommand(),
                  transfer.getStatus(), transfer.getPath());
      // Nothing to do
    }
  }
}
