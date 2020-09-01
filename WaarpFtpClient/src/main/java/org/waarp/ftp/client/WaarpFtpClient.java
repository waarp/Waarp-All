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
package org.waarp.ftp.client;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;

/**
 * FTP Client using Apache Commons net FTP client (not working using FTPS or
 * FTPSE)
 */
public class WaarpFtpClient {
  private static final String LOGIN_IN_ERROR = "Login in error";

  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpFtpClient.class);

  final String server;
  int port = 21;
  final String user;
  final String pwd;
  final String acct;
  int timeout;
  boolean isPassive;
  final int ssl; // -1 native, 1 auth
  protected final FTPClient ftpClient;
  protected String result;

  /**
   * WARNING: SSL mode (FTPS and FTPSE) are not working due to a bug in Apache
   * Commons-Net
   *
   * @param server
   * @param port
   * @param user
   * @param pwd
   * @param acct
   * @param isPassive
   * @param ssl
   * @param timeout
   */
  public WaarpFtpClient(final String server, final int port, final String user,
                        final String pwd, final String acct,
                        final boolean isPassive, final int ssl,
                        final int controlTimeout, final int timeout) {
    this.server = server;
    this.port = port;
    this.user = user;
    this.pwd = pwd;
    this.acct = acct;
    this.isPassive = isPassive;
    this.ssl = ssl;
    if (this.ssl != 0) {
      // implicit or explicit
      ftpClient = new FTPSClient(this.ssl == -1);
      ((FTPSClient) ftpClient)
          .setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
    } else {
      ftpClient = new FTPClient();
    }
    if (controlTimeout > 0) {
      ftpClient.setControlKeepAliveTimeout(controlTimeout / 1000);
      ftpClient.setControlKeepAliveReplyTimeout(controlTimeout);
    }
    if (timeout > 0) {
      ftpClient.setDataTimeout(timeout);
    }
    ftpClient.addProtocolCommandListener(
        new PrintCommandListener(new PrintWriter(System.out), true));
  }

  /**
   * Try to connect to the server and goes with the authentication
   *
   * @return True if connected and authenticated, else False
   */
  public boolean connect() {
    result = null;
    boolean isActive = false;
    try {
      try {
        ftpClient.connect(server, port);
      } catch (final SocketException e) {
        result = "Connection in error";
        logger.error(result, e);
        return false;
      } catch (final IOException e) {
        result = "Connection in error";
        logger.error(result, e);
        return false;
      }
      final int reply = ftpClient.getReplyCode();
      if (!FTPReply.isPositiveCompletion(reply)) {
        disconnect();
        result = "Connection in error: " + reply;
        logger.error(result);
        return false;
      }
      try {
        if (acct == null) {
          // no account
          if (!ftpClient.login(user, pwd)) {
            logout();
            result = LOGIN_IN_ERROR;
            logger.error(result);
            return false;
          }
        } else if (!ftpClient.login(user, pwd, acct)) {
          logout();
          result = LOGIN_IN_ERROR;
          logger.error(result);
          return false;
        }
      } catch (final IOException e) {
        result = LOGIN_IN_ERROR;
        logger.error(result, e);
        return false;
      }
      try {
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
      } catch (final IOException e1) {
        result = "Set BINARY in error";
        logger.error(result, e1);
        return false;
      }
      changeMode(isPassive);
      ftpClient.setUseEPSVwithIPv4(false);
      if (ssl == 1) {
        // now send request for PROT (AUTH already sent)
        try {
          ((FTPSClient) ftpClient).execPBSZ(0);
          logger.debug("PBSZ 0");
          ((FTPSClient) ftpClient).execPROT("P");
          logger.debug(
              "Info: " + ((FTPSClient) ftpClient).getEnableSessionCreation());
        } catch (final IOException e) {
          logout();
          result = "Explicit SSL in error";
          logger.error(result, e);
          return false;
        }
      }
      isActive = true;
      return true;
    } finally {
      if (!isActive && ftpClient.getDataConnectionMode() ==
                       FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE) {
        disconnect();
      }
    }
  }

  /**
   * Logout from the Control connection
   */
  public void logout() {
    try {
      ftpClient.logout();
    } catch (final IOException e) {
      if (ftpClient.getDataConnectionMode() ==
          FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE) {
        try {
          ftpClient.disconnect();
        } catch (final IOException f) {
          // do nothing
        }
      }
    }
  }

  /**
   * Disconnect the Ftp Client
   */
  public void disconnect() {
    try {
      ftpClient.disconnect();
    } catch (final IOException e) {
      logger.debug("Disconnection in error", e);
    }
  }

  /**
   * Create a new directory
   *
   * @param newDir
   *
   * @return True if created
   */
  public boolean makeDir(final String newDir) {
    result = null;
    try {
      return ftpClient.makeDirectory(newDir);
    } catch (final IOException e) {
      result = "MKDIR in error";
      logger.info(result, e);
      return false;
    }
  }

  /**
   * Change remote directory
   *
   * @param newDir
   *
   * @return True if the change is OK
   */
  public boolean changeDir(final String newDir) {
    result = null;
    try {
      return ftpClient.changeWorkingDirectory(newDir);
    } catch (final IOException e) {
      result = "CHDIR in error";
      logger.info(result, e);
      return false;
    }
  }

  /**
   * Change the FileType of Transfer (Binary true, ASCII false)
   *
   * @param binaryTransfer
   *
   * @return True if the change is OK
   */
  public boolean changeFileType(final boolean binaryTransfer) {
    result = null;
    try {
      if (binaryTransfer) {
        return ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
      } else {
        return ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
      }
    } catch (final IOException e) {
      result = "FileType in error";
      logger.warn(result, e);
      return false;
    }
  }

  /**
   * Change to passive (true) or active (false) mode
   *
   * @param passive
   */
  public void changeMode(final boolean passive) {
    isPassive = passive;
    if (isPassive) {
      ftpClient.enterLocalPassiveMode();
    } else {
      ftpClient.enterLocalActiveMode();
    }
  }

  /**
   * Ask to transfer a file
   *
   * @param local local filepath (limited to path if get, else full
   *     path)
   * @param remote filename
   * @param getStoreOrAppend -1 = get, 1 = store, 2 = append
   *
   * @return True if the file is correctly transfered
   */
  public boolean transferFile(final String local, final String remote,
                              final int getStoreOrAppend) {
    result = null;
    final boolean status;
    FileOutputStream output = null;
    FileInputStream fileInputStream = null;
    try {
      if (getStoreOrAppend > 0) {
        fileInputStream = new FileInputStream(local);
        if (getStoreOrAppend == 1) {
          status = ftpClient.storeFile(remote, fileInputStream);
        } else {
          // append
          status = ftpClient.appendFile(remote, fileInputStream);
        }
        if (!status) {
          result = "Cannot finalize store like operation";
          logger.error(result);
          return false;
        }
      } else {
        output = new FileOutputStream(new File(local, remote));
        status = ftpClient.retrieveFile(remote, output);
        if (!status) {
          result = "Cannot finalize retrieve like operation";
          logger.error(result);
          return false;
        }
      }
      return true;
    } catch (final IOException e) {
      result = "Cannot finalize operation";
      logger.error(result, e);
      return false;
    } finally {
      FileUtils.close(output);
      FileUtils.close(fileInputStream);
    }
  }

  /**
   * @return the list of files as returned by the FTP command
   */
  public String[] listFiles() {
    try {
      final FTPFile[] list = ftpClient.listFiles();
      final String[] results = new String[list.length];
      int i = 0;
      for (final FTPFile file : list) {
        results[i] = file.toFormattedString();
        i++;
      }
      return results;
    } catch (final IOException e) {
      result = "Cannot finalize transfer operation";
      logger.error(result, e);
      return null;
    }
  }

  /**
   * @param feature
   *
   * @return True if the feature is listed
   */
  public boolean featureEnabled(final String feature) {
    try {
      SysErrLogger.FAKE_LOGGER.sysout(ftpClient.features());
      if (ftpClient.featureValue(feature) == null) {
        final String resultNew = ftpClient.getReplyString();
        return resultNew.contains(feature.toUpperCase());
      }
      return true;
    } catch (final IOException e) {
      result = "Cannot execute operation Feature";
      logger.error(result, e);
      return false;
    }
  }

  /**
   * @param params
   *
   * @return the string lines returned by the command params
   */
  public String[] executeCommand(final String params) {
    result = null;
    try {
      SysErrLogger.FAKE_LOGGER.sysout(params);
      final int pos = params.indexOf(' ');
      String command = params;
      String args = null;
      if (pos > 0) {
        command = params.substring(0, pos);
        args = params.substring(pos + 1);
      }
      String[] results = ftpClient.doCommandAsStrings(command, args);
      if (results == null) {
        results = new String[1];
        results[0] = ftpClient.getReplyString();
      }
      return results;
    } catch (final IOException e) {
      result = "Cannot execute operation Site";
      logger.error(result, e);
      return null;
    }
  }

  /**
   * @param params
   *
   * @return the string lines returned by the SITE command params
   */
  public String[] executeSiteCommand(final String params) {
    result = null;
    try {
      SysErrLogger.FAKE_LOGGER.sysout("SITE " + params);
      String[] results = ftpClient.doCommandAsStrings("SITE", params);
      if (results == null) {
        results = new String[1];
        results[0] = ftpClient.getReplyString();
      }
      return results;
    } catch (final IOException e) {
      result = "Cannot execute operation Site";
      logger.error(result, e);
      return null;
    }
  }

}
