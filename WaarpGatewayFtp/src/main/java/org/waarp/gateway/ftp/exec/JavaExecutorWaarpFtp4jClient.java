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
package org.waarp.gateway.ftp.exec;

import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.ftp.client.WaarpFtp4jClient;
import org.waarp.openr66.context.task.FtpArgs;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * FTP client compatible for Waarp Gateway Kernel as JavaExecutor<br>
 * <br>
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
 * ACCT,PASS,REIN,USER,APPE,STOR,STOU,RETR,RMD,RNFR,RNTO,ABOR,CWD,CDUP,MODE,PASV,PORT,STRU,TYPE,
 * MDTM,MLSD,MLST,SIZE,AUTH) <br>
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
 * ACCT,PASS,REIN,USER,APPE,STOR,STOU,RETR,RMD,RNFR,RNTO,ABOR,CWD,CDUP,MODE,PASV,PORT,STRU,TYPE
 * ,MDTM,MLSD,MLST,SIZE,AUTH)<br>
 * 13) QUIT<br>
 */
public class JavaExecutorWaarpFtp4jClient implements GatewayRunnable {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(JavaExecutorWaarpFtp4jClient.class);
  private static final Pattern BLANK = WaarpStringUtils.BLANK;

  boolean waitForValidation;
  boolean useLocalExec;
  int delay;
  String[] args;
  int status;

  public JavaExecutorWaarpFtp4jClient() {
    // nothing
  }

  /**
   * "-file filepath <br> -to requestedHost <br> -port port <br> -user user <br> -pwd pwd <br> [-account
   * account] <br> [-mode active/passive] <br> [-ssl no/implicit/explicit]<br> [-cwd remotepath] <br> [-digest
   * (crc,md5,sha1)] <br> [-pre extraCommand1 with ',' as separator of arguments] <br> -command command from
   * (get,put,append) <br> [-post extraCommand2 with ',' as separator of arguments]" <br>
   **/
  @Override
  public void run() {
    logger.info("FtpTransfer with {} arguments", args.length);
    final FtpArgs ftpArgs;
    try {
      ftpArgs = FtpArgs.getFtpArgs(args);
    } catch (final OpenR66RunnerErrorException e) {
      status = -2;
      logger.error("Not enough arguments: {}", e.getMessage());
      return;
    }
    int timeout = 30000;
    if (delay > 1000) {
      timeout = delay;
    }
    final WaarpFtp4jClient ftpClient =
        new WaarpFtp4jClient(ftpArgs.getRequested(), ftpArgs.getPort(),
                             ftpArgs.getUser(), ftpArgs.getPwd(),
                             ftpArgs.getAcct(), ftpArgs.isPassive(),
                             ftpArgs.getSsl(), 5000, timeout);
    boolean connected = false;
    for (int i = 0; i < 3; i++) {
      if (ftpClient.connect()) {
        connected = true;
        break;
      }
    }
    if (!connected) {
      status = -3;
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
        status = -4;
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
          status = -5;
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
          status = -6;
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
    logger.info("FTP transfer in\n    SUCCESS\n    {}\n    <REMOTE>{}</REMOTE>",
                ftpArgs.getFilepath(), ftpArgs.getRequested());
    status = 0;
  }

  @Override
  public void setArgs(final boolean waitForValidation,
                      final boolean useLocalExec, final int delay,
                      final String[] args) {
    this.waitForValidation = waitForValidation;
    this.useLocalExec = useLocalExec;
    this.delay = delay;
    this.args = args;
  }

  @Override
  public int getFinalStatus() {
    return status;
  }

}
