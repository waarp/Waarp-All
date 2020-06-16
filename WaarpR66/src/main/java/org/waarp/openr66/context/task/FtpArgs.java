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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.openr66.context.task.exception.OpenR66RunnerErrorException;

import java.io.File;

/**
 * Class to parse arguments for FTP command
 */
public class FtpArgs {
  private static final Option FILE_OPTION =
      Option.builder("file").required(true).hasArg(true)
            .desc("Specify the file path to operate on").build();
  private static final Option HOST_OPTION =
      Option.builder("to").required(true).hasArg(true)
            .desc("Specify the requested Host").build();
  private static final Option PORT_OPTION =
      Option.builder("port").required(true).hasArg(true)
            .desc("Specify the port on remote host to use").type(Number.class)
            .build();
  private static final Option USER_OPTION =
      Option.builder("user").required(true).hasArg(true)
            .desc("Specify the user on remote host to use").build();
  private static final Option PWD_OPTION =
      Option.builder("pwd").required(true).hasArg(true)
            .desc("Specify the password on remote host to use").build();
  private static final Option ACCOUNT_OPTION =
      Option.builder("account").hasArg(true)
            .desc("Specify the account on remote host to use").build();
  private static final Option MODE_OPTION = Option.builder("mode").hasArg(true)
                                                  .desc(
                                                      "Specify the mode active of" +
                                                      " passive to use")
                                                  .build();
  private static final Option SSL_OPTION = Option.builder("ssl").hasArg(true)
                                                 .desc(
                                                     "Specify the ssl mode to use " +
                                                     "between no / explicit / implicit")
                                                 .build();
  private static final Option CWD_OPTION = Option.builder("cwd").hasArg(true)
                                                 .desc(
                                                     "Specify the remote path on " +
                                                     "remote host to use")
                                                 .build();
  private static final Option DIGEST_OPTION =
      Option.builder("digest").hasArg(true).desc(
          "Specify the digest to use between crc, md5, " +
          "sha1, sha256, sha384, sha512").build();
  private static final Option PRE_OPTION = Option.builder("pre").hasArg(true)
                                                 .desc(
                                                     "Specify the extra command as " +
                                                     "pre operation to use, using ',' as " +
                                                     "separator of arguments")
                                                 .build();
  private static final Option POST_OPTION = Option.builder("post").hasArg(true)
                                                  .desc(
                                                      "Specify the extra command " +
                                                      "as post operation to use, " +
                                                      "using ',' as" +
                                                      " separator of arguments")
                                                  .build();
  private static final Option CMD_OPTION =
      Option.builder("command").hasArg(true)
            .desc("Specify the command as one of get, put, append ").build();

  private static final Options FTP_OPTIONS =
      new Options().addOption(FILE_OPTION).addOption(HOST_OPTION)
                   .addOption(PORT_OPTION).addOption(USER_OPTION)
                   .addOption(PWD_OPTION).addOption(ACCOUNT_OPTION)
                   .addOption(MODE_OPTION).addOption(SSL_OPTION)
                   .addOption(CWD_OPTION).addOption(DIGEST_OPTION)
                   .addOption(PRE_OPTION).addOption(CMD_OPTION)
                   .addOption(POST_OPTION);
  private String filepath = null;
  private String filename = null;
  private String requested = null;
  private int port = 21;
  private String user = null;
  private String pwd = null;
  private String acct = null;
  private boolean isPassive = true;
  private int ssl = 0; // -1 native, 1 auth
  private String cwd = null;
  private DigestAlgo digest = null; // 1 CRC, 2 MD5, 3 SHA1, 4 SHA256, 5 SHA384,
  // 6 SHA512
  private String digestCommand = null;
  private String command;
  private int codeCommand = 0; // -1 get, 1 put, 2 append
  private String preArgs = null;
  private String postArgs = null;

  /**
   * Print to standard output the help of this command
   */
  public static void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("FtpArgs", FTP_OPTIONS);
  }

  /**
   * "-file filepath <br> -to requestedHost <br> -port port <br> -user user <br> -pwd pwd <br> [-account
   * account] <br> [-mode active/passive] <br> [-ssl no/implicit/explicit]<br> [-cwd remotepath] <br> [-digest
   * (crc,md5,sha1)] <br> [-pre extraCommand1 with ',' as separator of arguments] <br> -command command from
   * (get,put,append) <br> [-post extraCommand2 with ',' as separator of arguments]" <br>
   * <br>
   *
   * @param args must be already replaced values (getReplacedValue)
   */
  public static FtpArgs getFtpArgs(String[] args)
      throws OpenR66RunnerErrorException {
    FtpArgs ftpArgs = new FtpArgs();
    CommandLineParser parser = new DefaultParser();
    try {
      CommandLine cmd = parser.parse(FTP_OPTIONS, args, true);

      ftpArgs.setFilepath(cmd.getOptionValue("file"));
      ftpArgs.setFilename(new File(ftpArgs.getFilepath()).getName());
      ftpArgs.setRequested(cmd.getOptionValue("to"));
      try {
        ftpArgs.setPort(Integer.parseInt(cmd.getOptionValue("port")));
        if (ftpArgs.getPort() < 0) {
          throw new NumberFormatException("Port must be positive");
        }
      } catch (NumberFormatException e) {
        throw new OpenR66RunnerErrorException(e);
      }
      ftpArgs.setUser(cmd.getOptionValue("user"));
      ftpArgs.setPwd(cmd.getOptionValue("pwd"));
      if (cmd.hasOption("account")) {
        ftpArgs.setAcct(cmd.getOptionValue("account"));
      }
      if (cmd.hasOption("mode")) {
        ftpArgs
            .setPassive("passive".equalsIgnoreCase(cmd.getOptionValue("mode")));
      }
      if (cmd.hasOption("ssl")) {
        String ssl = cmd.getOptionValue("ssl");
        if ("implicit".equalsIgnoreCase(ssl)) {
          ftpArgs.setSsl(-1);
        } else if ("explicit".equalsIgnoreCase(ssl)) {
          ftpArgs.setSsl(1);
        } else {
          ftpArgs.setSsl(0);
        }
      }
      if (cmd.hasOption("cwd")) {
        ftpArgs.setCwd(cmd.getOptionValue("cwd"));
      }
      if (cmd.hasOption("digest")) {
        String digest = cmd.getOptionValue("digest");
        if ("crc".equalsIgnoreCase(digest)) {
          ftpArgs.setDigest(DigestAlgo.CRC32);
          ftpArgs.setDigestCommand("XCRC " + ftpArgs.getFilename());
        } else if ("md5".equalsIgnoreCase(digest)) {
          ftpArgs.setDigest(DigestAlgo.MD5);
          ftpArgs.setDigestCommand("XMD5 " + ftpArgs.getFilename());
        } else if ("sha1".equalsIgnoreCase(digest)) {
          ftpArgs.setDigest(DigestAlgo.SHA1);
          ftpArgs.setDigestCommand("XSHA1 " + ftpArgs.getFilename());
        } else if ("sha256".equalsIgnoreCase(digest)) {
          ftpArgs.setDigest(DigestAlgo.SHA256);
          ftpArgs.setDigestCommand("XSHA256 " + ftpArgs.getFilename());
        } else if ("sha384".equalsIgnoreCase(digest)) {
          ftpArgs.setDigest(DigestAlgo.SHA384);
          ftpArgs.setDigestCommand("XSHA384 " + ftpArgs.getFilename());
        } else if ("sha512".equalsIgnoreCase(digest)) {
          ftpArgs.setDigest(DigestAlgo.SHA512);
          ftpArgs.setDigestCommand("XSHA512 " + ftpArgs.getFilename());
        } else {
          ftpArgs.setDigest(null);
        }
      }
      if (cmd.hasOption("pre")) {
        ftpArgs.setPreArgs(cmd.getOptionValue("pre").replace(',', ' '));
      }
      if (cmd.hasOption("post")) {
        ftpArgs.setPostArgs(cmd.getOptionValue("post").replace(',', ' '));
      }
      if (cmd.hasOption("command")) {
        ftpArgs.setCommand(cmd.getOptionValue("command"));
        // get,put,append,list
        // -1 get, 1 put, 2 append
        if ("get".equalsIgnoreCase(ftpArgs.getCommand())) {
          ftpArgs.setCodeCommand(-1);
        } else if ("put".equalsIgnoreCase(ftpArgs.getCommand())) {
          ftpArgs.setCodeCommand(1);
        } else if ("append".equalsIgnoreCase(ftpArgs.getCommand())) {
          ftpArgs.setCodeCommand(2);
        } else {
          // error
          ftpArgs.setCodeCommand(0);
          throw new OpenR66RunnerErrorException(
              "Command not known: " + ftpArgs.getCommand());
        }
      }
    } catch (ParseException e) {
      throw new OpenR66RunnerErrorException(e);
    }

    return ftpArgs;
  }

  public String getFilepath() {
    return filepath;
  }

  public FtpArgs setFilepath(String filepath) {
    this.filepath = filepath;
    return this;
  }

  public String getFilename() {
    return filename;
  }

  public FtpArgs setFilename(String filename) {
    this.filename = filename;
    return this;
  }

  public String getRequested() {
    return requested;
  }

  public FtpArgs setRequested(String requested) {
    this.requested = requested;
    return this;
  }

  public int getPort() {
    return port;
  }

  public FtpArgs setPort(int port) {
    this.port = port;
    return this;
  }

  public String getUser() {
    return user;
  }

  public FtpArgs setUser(String user) {
    this.user = user;
    return this;
  }

  public String getPwd() {
    return pwd;
  }

  public FtpArgs setPwd(String pwd) {
    this.pwd = pwd;
    return this;
  }

  public String getAcct() {
    return acct;
  }

  public FtpArgs setAcct(String acct) {
    this.acct = acct;
    return this;
  }

  public boolean isPassive() {
    return isPassive;
  }

  public FtpArgs setPassive(boolean passive) {
    isPassive = passive;
    return this;
  }

  public int getSsl() {
    return ssl;
  }

  public FtpArgs setSsl(int ssl) {
    this.ssl = ssl;
    return this;
  }

  public String getCwd() {
    return cwd;
  }

  public FtpArgs setCwd(String cwd) {
    this.cwd = cwd;
    return this;
  }

  public DigestAlgo getDigest() {
    return digest;
  }

  public FtpArgs setDigest(DigestAlgo digest) {
    this.digest = digest;
    return this;
  }

  public String getDigestCommand() {
    return digestCommand;
  }

  public FtpArgs setDigestCommand(String digestCommand) {
    this.digestCommand = digestCommand;
    return this;
  }

  public String getCommand() {
    return command;
  }

  public FtpArgs setCommand(String command) {
    this.command = command;
    return this;
  }

  public int getCodeCommand() {
    return codeCommand;
  }

  public FtpArgs setCodeCommand(int codeCommand) {
    this.codeCommand = codeCommand;
    return this;
  }

  public String getPreArgs() {
    return preArgs;
  }

  public FtpArgs setPreArgs(String preArgs) {
    this.preArgs = preArgs;
    return this;
  }

  public String getPostArgs() {
    return postArgs;
  }

  public FtpArgs setPostArgs(String postArgs) {
    this.postArgs = postArgs;
    return this;
  }
}
