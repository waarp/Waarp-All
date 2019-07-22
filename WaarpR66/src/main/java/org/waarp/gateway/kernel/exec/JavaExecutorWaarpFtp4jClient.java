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
package org.waarp.gateway.kernel.exec;

import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.ftp.client.WaarpFtp4jClient;

import java.io.File;
import java.io.IOException;

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
 *
 *
 */
public class JavaExecutorWaarpFtp4jClient implements GatewayRunnable {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(JavaExecutorWaarpFtp4jClient.class);

  boolean waitForValidation;
  boolean useLocalExec;
  int delay;
  String[] args;
  int status = 0;

  public JavaExecutorWaarpFtp4jClient() {
  }

  @Override
  public void run() {
    logger.info("FtpTransfer with " + args.length + " arguments");
    if (args.length < 10) {
      status = -1;
      logger.error("Not enough arguments");
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
    String command = null;
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
      if (args[i].equalsIgnoreCase("-file")) {
        i++;
        filepath = args[i];
        filename = new File(filepath).getName();
      } else if (args[i].equalsIgnoreCase("-to")) {
        i++;
        requested = args[i];
      } else if (args[i].equalsIgnoreCase("-port")) {
        i++;
        port = Integer.parseInt(args[i]);
      } else if (args[i].equalsIgnoreCase("-user")) {
        i++;
        user = args[i];
      } else if (args[i].equalsIgnoreCase("-pwd")) {
        i++;
        pwd = args[i];
      } else if (args[i].equalsIgnoreCase("-account")) {
        i++;
        acct = args[i];
      } else if (args[i].equalsIgnoreCase("-mode")) {
        i++;
        isPassive = (args[i].equalsIgnoreCase("passive"));
      } else if (args[i].equalsIgnoreCase("-ssl")) {
        i++;
        if (args[i].equalsIgnoreCase("implicit")) {
          ssl = -1;
        } else if (args[i].equalsIgnoreCase("explicit")) {
          ssl = 1;
        } else {
          ssl = 0;
        }
      } else if (args[i].equalsIgnoreCase("-cwd")) {
        i++;
        cwd = args[i];
      } else if (args[i].equalsIgnoreCase("-digest")) {
        i++;
        if (args[i].equalsIgnoreCase("crc")) {
          digest = 1;
        } else if (args[i].equalsIgnoreCase("md5")) {
          digest = 2;
        } else if (args[i].equalsIgnoreCase("sha1")) {
          digest = 3;
        } else {
          digest = 0;
        }
      } else if (args[i].equalsIgnoreCase("-pre")) {
        i++;
        preArgs = args[i].replace(',', ' ');
      } else if (args[i].equalsIgnoreCase("-post")) {
        i++;
        postArgs = args[i].replace(',', ' ');
      } else if (args[i].equalsIgnoreCase("-command")) {
        i++;
        command = args[i];
        // get,put,append,list
        // -1 get, 1 put, 2 append
        if (command.equalsIgnoreCase("get")) {
          codeCommand = -1;
        } else if (command.equalsIgnoreCase("put")) {
          codeCommand = 1;
        } else if (command.equalsIgnoreCase("append")) {
          codeCommand = 2;
        } else {
          // error
          codeCommand = 0;
        }
      }
    }
    if (filepath == null || requested == null || port <= 0 || user == null ||
        pwd == null || codeCommand == 0) {
      status = -2;
      final int code =
          0 + (filepath == null? 1 : 0) + (requested == null? 10 : 0) +
          (port <= 0? 100 : 0) + (user == null? 1000 : 0) +
          (pwd == null? 10000 : 0) + (codeCommand == 0? 100000 : 0);
      logger.error("Not enough arguments: " + code);
      return;
    }
    int timeout = 30000;
    if (delay > 1000) {
      timeout = delay;
    }
    final WaarpFtp4jClient ftpClient =
        new WaarpFtp4jClient(requested, port, user, pwd, acct, isPassive, ssl,
                             5000, timeout);
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
        status = -4;
        logger.error(ftpClient.getResult());
        return;
      }
      if (digest > 0) {
        // digest check
        String params = null;
        DigestAlgo algo = null;
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
          values = values[0].split(" ");
          hashresult = (values.length > 3? values[1] : values[0]);
        }
        if (hashresult == null) {
          status = -5;
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
        if (hash == null || (!hash.equalsIgnoreCase(hashresult))) {
          status = -6;
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
    logger.info(
        "FTP transfer in\n    SUCCESS\n    " + filepath + "\n    <REMOTE>" +
        requested + "</REMOTE>");
    status = 0;
  }

  @Override
  public void setArgs(boolean waitForValidation, boolean useLocalExec,
                      int delay, String[] args) {
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
