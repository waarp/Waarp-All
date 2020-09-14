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
package org.waarp.openr66.context.task;

import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.ftp.client.WaarpFtp4jClient;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.protocol.configuration.Configuration;
import org.waarp.openr66.protocol.exception.OpenR66ProtocolSystemException;

import java.io.File;
import java.io.IOException;

/**
 * Ftp Transfer task: synchronous<br>
 * <p>
 * Result of arguments will be as FTP command.<br>
 * Format is the following:<br>
 * "-file filepath <br>
 * -to requestedHost <br>
 * -port port <br>
 * -user user <br>
 * -pwd pwd <br>
 * [-account account] <br>
 * [-mode active/passive] <br>
 * [-ssl no/implicit/explicit]<br>
 * [-cwd remotepath] <br>
 * [-digest (crc,md5,sha1)] <br>
 * [-pre extraCommand1 with ',' as separator of arguments] <br>
 * -command command from (get,put,append) <br>
 * [-post extraCommand2 with ',' as separator of arguments]" <br>
 * <br>
 * <br>
 * The order of commands will be:<br>
 * 1) connection to requestHost on port (if ssl native => using native ssl
 * link)<br>
 * 2) User user<br>
 * 3) PASS pwd<br>
 * 4) if account => ACCT account<br>
 * 5) if -ssl & auth => AUTH, PBSZ 0, PROT P <br>
 * 6) if passive => PASV<br>
 * 7) CWD remotepath; if error => MKD remotepath then CWD remotepath (and
 * ignoring any error)<br>
 * 8) if pre => extraCommand1 with ',' replaced by ' ' (note: do not use
 * standard commands from FTP like
 * ACCT,PASS,REIN,USER,APPE,STOR,STOU,RETR,RMD,RNFR,RNTO,ABOR,CWD,CDUP,MODE,PASV,PORT,STRU,TYPE,MDTM,MLSD,MLST,SIZE,AUTH)
 * <br>
 * 9) BINARY (binary format)<br>
 * 10) if get => RETR filepath.basename ; if put => STOR filepath ; if append
 * =>
 * APPE filepath.basename<br>
 * 11) if digest & get/put/append & remote site compatible with XCRC,XMD5,XSHA1
 * => FEAT (parsing if found
 * corresponding XCRC,XMD5,XSHA1) ; then XCRC/XMD5/XSHA1 filepath.basename ;
 * then locally comparing this
 * XCRC/XMD5/XSHA1 with the local file<br>
 * 12) if post => extraCommand2 with ',' replaced by ' ' (note: do not use
 * standard commands from FTP like
 * ACCT,PASS,REIN,USER,APPE,STOR,STOU,RETR,RMD,RNFR,RNTO,ABOR,CWD,CDUP,MODE,PASV,PORT,STRU,TYPE,MDTM,MLSD,MLST,SIZE,AUTH)<br>
 * 13) QUIT<br>
 */
public class FtpTransferTask extends AbstractTask {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FtpTransferTask.class);

  /**
   * @param argRule
   * @param delay
   * @param argTransfer
   * @param session
   */
  public FtpTransferTask(final String argRule, final int delay,
                         final String argTransfer, final R66Session session) {
    super(TaskType.FTP, delay, argRule, argTransfer, session);
  }

  /**
   * "-file filepath <br> -to requestedHost <br> -port port <br> -user user <br> -pwd pwd <br> [-account
   * account] <br> [-mode active/passive] <br> [-ssl no/implicit/explicit]<br> [-cwd remotepath] <br> [-digest
   * (crc,md5,sha1)] <br> [-pre extraCommand1 with ',' as separator of arguments] <br> -command command from
   * (get,put,append) <br> [-post extraCommand2 with ',' as separator of arguments]" <br>
   **/

  @Override
  public void run() {
    logger.info("FtpTransfer with {}:{} and {}", argRule, argTransfer, session);
    if (argRule == null) {
      logger.error(
          "FtpTransfer cannot be done with " + argRule + ':' + argTransfer +
          " and " + session);
      futureCompletion.setFailure(
          new OpenR66ProtocolSystemException("FtpTransfer cannot be done"));
      return;
    }
    String finalname = argRule;
    final String[] argFormat = BLANK.split(argTransfer);
    if (argFormat != null && argFormat.length > 0) {
      try {
        finalname = String.format(finalname, (Object[]) argFormat);
      } catch (final Exception e) {
        // ignored error since bad argument in static rule info
        logger
            .error("Bad format in Rule: {" + finalname + "} " + e.getMessage());
      }
    }
    final String[] args = BLANK.split(finalname);
    for (int i = 0; i < args.length; i++) {
      args[i] = getReplacedValue(args[i], null);
    }
    final FtpArgs ftpArgs;
    try {
      ftpArgs = FtpArgs.getFtpArgs(args);
    } catch (final OpenR66RunnerErrorException e) {
      final OpenR66RunnerErrorException exception =
          new OpenR66RunnerErrorException("Not enough argument in Transfer");
      final R66Result result =
          new R66Result(exception, session, false, ErrorCode.CommandNotFound,
                        session.getRunner());
      logger.error("Not enough arguments: " + e.getMessage());
      futureCompletion.setResult(result);
      futureCompletion.setFailure(exception);
      return;
    }

    final WaarpFtp4jClient ftpClient =
        new WaarpFtp4jClient(ftpArgs.getRequested(), ftpArgs.getPort(),
                             ftpArgs.getUser(), ftpArgs.getPwd(),
                             ftpArgs.getAcct(), ftpArgs.isPassive(),
                             ftpArgs.getSsl(), 5000,
                             (int) Configuration.configuration.getTimeoutCon());
    boolean status = false;
    for (int i = 0; i < Configuration.RETRYNB; i++) {
      if (ftpClient.connect()) {
        status = true;
        break;
      }
    }
    if (!status) {
      final OpenR66RunnerErrorException exception =
          new OpenR66RunnerErrorException("Cannot connect to remote FTP host");
      final R66Result result = new R66Result(exception, session, false,
                                             ErrorCode.ConnectionImpossible,
                                             session.getRunner());
      futureCompletion.setResult(result);
      futureCompletion.setFailure(exception);
      logger.error(ftpClient.getResult());
      return;
    }
    try {
      if (ftpArgs.getCwd() != null && !ftpClient.changeDir(ftpArgs.getCwd())) {
        ftpClient.makeDir(ftpArgs.getCwd());
        if (!ftpClient.changeDir(ftpArgs.getCwd())) {
          logger.warn("Cannot change od directory: " + ftpArgs.getCwd());
        }
      }
      if (ftpArgs.getPreArgs() != null) {
        final String[] result = ftpClient.executeCommand(ftpArgs.getPreArgs());
        for (final String string : result) {
          logger.debug("PRE: {}", string);
        }
      }
      if (!ftpClient.transferFile(ftpArgs.getFilepath(), ftpArgs.getFilename(),
                                  ftpArgs.getCodeCommand())) {
        final OpenR66RunnerErrorException exception =
            new OpenR66RunnerErrorException(
                "Cannot transfert file from/to remote FTP host");
        final R66Result result =
            new R66Result(exception, session, false, ErrorCode.TransferError,
                          session.getRunner());
        futureCompletion.setResult(result);
        futureCompletion.setFailure(exception);
        logger.error(ftpClient.getResult());
        return;
      }
      if (ftpArgs.getDigest() != null) {
        String[] values = ftpClient.executeCommand(ftpArgs.getDigestCommand());
        String hashresult = null;
        if (values != null) {
          values = BLANK.split(values[0]);
          hashresult = values.length > 3? values[1] : values[0];
        }
        if (hashresult == null) {
          final OpenR66RunnerErrorException exception =
              new OpenR66RunnerErrorException(
                  "Hash cannot be computed while FTP transfer is done");
          final R66Result result =
              new R66Result(exception, session, false, ErrorCode.TransferError,
                            session.getRunner());
          futureCompletion.setResult(result);
          futureCompletion.setFailure(exception);
          logger.error("Hash cannot be computed: " + ftpClient.getResult());
          return;
        }
        // now check locally
        String hash;
        try {
          hash = FilesystemBasedDigest.getHex(FilesystemBasedDigest.getHash(
              new File(ftpArgs.getFilepath()), false, ftpArgs.getDigest()));
        } catch (final IOException e) {
          hash = null;
        }
        if (hash == null || !hash.equalsIgnoreCase(hashresult)) {
          final OpenR66RunnerErrorException exception =
              new OpenR66RunnerErrorException(
                  "Hash not equal while FTP transfer is done");
          final R66Result result =
              new R66Result(exception, session, false, ErrorCode.TransferError,
                            session.getRunner());
          futureCompletion.setResult(result);
          futureCompletion.setFailure(exception);
          logger.error("Hash not equal: " + ftpClient.getResult());
          return;
        }
      }
      if (ftpArgs.getPostArgs() != null) {
        final String[] result = ftpClient.executeCommand(ftpArgs.getPostArgs());
        for (final String string : result) {
          logger.debug("POST: {}", string);
        }
      }
    } finally {
      ftpClient.logout();
    }
    final R66Result result = new R66Result(session, false, ErrorCode.TransferOk,
                                           session.getRunner());
    futureCompletion.setResult(result);
    if (logger.isInfoEnabled()) {
      logger.info("FTP transfer in     SUCCESS     " +
                  session.getRunner().toShortString() + "     <REMOTE>" +
                  ftpArgs.getRequested() + "</REMOTE>");
    }
    futureCompletion.setSuccess();
  }

}
