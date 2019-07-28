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

import org.apache.commons.exec.CommandLine;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.ftp.client.WaarpFtp4jClient;
import org.waarp.openr66.context.ErrorCode;
import org.waarp.openr66.context.R66Result;
import org.waarp.openr66.context.R66Session;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;
import org.waarp.openr66.protocol.configuration.Configuration;

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
  public FtpTransferTask(String argRule, int delay, String argTransfer,
                         R66Session session) {
    super(TaskType.FTP, delay, argRule, argTransfer, session);
  }

  @Override
  public void run() {
    logger.info("FtpTransfer with " + argRule + ':' + argTransfer + " and {}",
                session);
    String finalname = argRule;
    final Object[] argFormat = BLANK.split(argTransfer);
    if (argFormat != null && argFormat.length > 0) {
      try {
        finalname = String.format(finalname, argFormat);
      } catch (final Exception e) {
        // ignored error since bad argument in static rule info
        logger
            .error("Bad format in Rule: {" + finalname + "} " + e.getMessage());
      }
    }

    final CommandLine cl = new CommandLine("dummy");
    cl.addArguments(finalname, false);
    final String[] args = cl.getArguments();

    if (args.length < 10) {
      final OpenR66RunnerErrorException exception =
          new OpenR66RunnerErrorException("Not enough argument in Transfer");
      final R66Result result =
          new R66Result(exception, session, false, ErrorCode.CommandNotFound,
                        session.getRunner());
      logger.error("Not enough arguments");
      futureCompletion.setResult(result);
      futureCompletion.setFailure(exception);
      return;
    }
    String filepath = null;
    String filename = null;
    String requested = null;
    int port = 21;
    String user = null;
    String pwd = null;
    String acct = null;
    boolean isPassive = true;
    int ssl = 0; // -1 native, 1 auth
    String cwd = null;
    int digest = 0; // 1 CRC, 2 MD5, 3 SHA1
    String command;
    int codeCommand = 0; // -1 get, 1 put, 2 append
    String preArgs = null;
    String postArgs = null;
    /*
     * "-file filepath <br> -to requestedHost <br> -port port <br> -user user <br> -pwd pwd <br> [-account
     * account] <br> [-mode active/passive] <br> [-ssl no/implicit/explicit]<br> [-cwd remotepath] <br> [-digest
     * (crc,md5,sha1)] <br> [-pre extraCommand1 with ',' as separator of arguments] <br> -command command from
     * (get,put,append) <br> [-post extraCommand2 with ',' as separator of arguments]" <br>
     */
    for (int i = 0; i < args.length; i++) {
      args[i] = getReplacedValue(args[i], null);
    }
    for (int i = 0; i < args.length; i++) {
      if ("-file".equalsIgnoreCase(args[i])) {
        i++;
        filepath = args[i];
        filename = new File(filepath).getName();
      } else if ("-to".equalsIgnoreCase(args[i])) {
        i++;
        requested = args[i];
      } else if ("-port".equalsIgnoreCase(args[i])) {
        i++;
        port = Integer.parseInt(args[i]);
      } else if ("-user".equalsIgnoreCase(args[i])) {
        i++;
        user = args[i];
      } else if ("-pwd".equalsIgnoreCase(args[i])) {
        i++;
        pwd = args[i];
      } else if ("-account".equalsIgnoreCase(args[i])) {
        i++;
        acct = args[i];
      } else if ("-mode".equalsIgnoreCase(args[i])) {
        i++;
        isPassive = "passive".equalsIgnoreCase(args[i]);
      } else if ("-ssl".equalsIgnoreCase(args[i])) {
        i++;
        if ("implicit".equalsIgnoreCase(args[i])) {
          ssl = -1;
        } else if ("explicit".equalsIgnoreCase(args[i])) {
          ssl = 1;
        } else {
          ssl = 0;
        }
      } else if ("-cwd".equalsIgnoreCase(args[i])) {
        i++;
        cwd = args[i];
      } else if ("-digest".equalsIgnoreCase(args[i])) {
        i++;
        if ("crc".equalsIgnoreCase(args[i])) {
          digest = 1;
        } else if ("md5".equalsIgnoreCase(args[i])) {
          digest = 2;
        } else if ("sha1".equalsIgnoreCase(args[i])) {
          digest = 3;
        } else {
          digest = 0;
        }
      } else if ("-pre".equalsIgnoreCase(args[i])) {
        i++;
        preArgs = args[i].replace(',', ' ');
      } else if ("-post".equalsIgnoreCase(args[i])) {
        i++;
        postArgs = args[i].replace(',', ' ');
      } else if ("-command".equalsIgnoreCase(args[i])) {
        i++;
        command = args[i];
        // get,put,append,list
        // -1 get, 1 put, 2 append
        if ("get".equalsIgnoreCase(command)) {
          codeCommand = -1;
        } else if ("put".equalsIgnoreCase(command)) {
          codeCommand = 1;
        } else if ("append".equalsIgnoreCase(command)) {
          codeCommand = 2;
        } else {
          // error
          codeCommand = 0;
        }
      }
    }
    if (filepath == null || requested == null || port <= 0 || user == null ||
        pwd == null || codeCommand == 0) {
      final OpenR66RunnerErrorException exception =
          new OpenR66RunnerErrorException("Not enough argument in Transfer");
      final R66Result result =
          new R66Result(exception, session, false, ErrorCode.CommandNotFound,
                        session.getRunner());
      final int code = (filepath == null? 1 : 0) + (requested == null? 10 : 0) +
                       (port <= 0? 100 : 0) + (user == null? 1000 : 0) +
                       (pwd == null? 10000 : 0) +
                       (codeCommand == 0? 100000 : 0);
      logger.error("Not enough arguments: " + code);
      futureCompletion.setResult(result);
      futureCompletion.setFailure(exception);
      return;
    }
    final WaarpFtp4jClient ftpClient =
        new WaarpFtp4jClient(requested, port, user, pwd, acct, isPassive, ssl,
                             5000,
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
      if (cwd != null && !ftpClient.changeDir(cwd)) {
        ftpClient.makeDir(cwd);
        if (!ftpClient.changeDir(cwd)) {
          logger.warn("Cannot change od directory: " + cwd);
        }
      }
      if (preArgs != null) {
        final String[] result = ftpClient.executeCommand(preArgs);
        for (final String string : result) {
          logger.debug("PRE: " + string);
        }
      }
      if (!ftpClient.transferFile(filepath, filename, codeCommand)) {
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
      if (digest > 0) {
        // digest check
        String params;
        DigestAlgo algo;
        switch (digest) {
          case 1: // CRC
            params = "XCRC ";
            algo = DigestAlgo.CRC32;
            break;
          case 2: // MD5
            params = "XMD5 ";
            algo = DigestAlgo.MD5;
            break;
          case 3: // SHA1
          default:
            params = "XSHA1 ";
            algo = DigestAlgo.SHA1;
            break;
        }
        params += filename;
        String[] values = ftpClient.executeCommand(params);
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
          hash = FilesystemBasedDigest.getHex(
              FilesystemBasedDigest.getHash(new File(filepath), false, algo));
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
      if (postArgs != null) {
        final String[] result = ftpClient.executeCommand(postArgs);
        for (final String string : result) {
          logger.debug("POST: " + string);
        }
      }
    } finally {
      ftpClient.logout();
    }
    final R66Result result = new R66Result(session, false, ErrorCode.TransferOk,
                                           session.getRunner());
    futureCompletion.setResult(result);
    logger.info("FTP transfer in     SUCCESS     " +
                session.getRunner().toShortString() + "     <REMOTE>" +
                requested + "</REMOTE>");
    futureCompletion.setSuccess();
  }

}
