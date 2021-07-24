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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * FTP Client using Apache Commons net FTP client (not working using FTPS or
 * FTPSE)
 */
public class WaarpFtpClient implements WaarpFtpClientInterface {
  /**
   * Internal Logger
   */
  protected static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpFtpClient.class);
  private static final int DEFAULT_WAIT = 2;

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
  protected String directory = null;

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
    this(server, port, user, pwd, acct, isPassive, ssl, controlTimeout, timeout,
         true);
  }

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
                        final int controlTimeout, final int timeout,
                        final boolean trace) {
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
      ((FTPSClient) ftpClient).setTrustManager(
          TrustManagerUtils.getAcceptAllTrustManager());
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
    ftpClient.setListHiddenFiles(true);
    if (trace) {
      ftpClient.addProtocolCommandListener(
          new PrintCommandListener(new PrintWriter(System.out), true));
    }
  }

  @Override
  public final String getResult() {
    return result;
  }

  private void reconnect() {
    logout();
    waitAfterDataCommand();
    connect();
    if (directory != null) {
      changeDir(directory);
    }
  }

  @Override
  public final boolean connect() {
    result = null;
    boolean isActive = false;
    try {
      for (int j = 0; j < 3; j++) {
        waitAfterDataCommand();
        Exception lastExcemption = null;
        for (int i = 0; i < 5; i++) {
          try {
            if (port > 0) {
              ftpClient.connect(server, port);
            } else {
              ftpClient.connect(server);
            }
            lastExcemption = null;
            break;
          } catch (final Exception e) {
            result = CONNECTION_IN_ERROR;
            lastExcemption = e;
          }
          waitAfterDataCommand();
        }
        if (lastExcemption != null) {
          logger.error(result + ": {}", lastExcemption.getMessage());
          return false;
        }
        final int reply = ftpClient.getReplyCode();
        if (FTPReply.isPositiveCompletion(reply)) {
          break;
        } else {
          disconnect();
          result = CONNECTION_IN_ERROR + ": " + reply;
        }
      }
      final int reply = ftpClient.getReplyCode();
      if (!FTPReply.isPositiveCompletion(reply)) {
        disconnect();
        result = CONNECTION_IN_ERROR + ": " + reply;
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
        logger.error(result + ": {}", e.getMessage());
        return false;
      }
      ftpClient.setUseEPSVwithIPv4(false);
      if (ssl == 1) {
        // now send request for PROT (AUTH already sent)
        try {
          ((FTPSClient) ftpClient).execPBSZ(0);
          logger.debug("PBSZ 0");
          ((FTPSClient) ftpClient).execPROT("P");
          logger.debug("Info: {}",
                       ((FTPSClient) ftpClient).getEnableSessionCreation());
        } catch (final IOException e) {
          logout();
          result = "Explicit SSL in error";
          logger.error(result + ": {}", e.getMessage());
          return false;
        }
      }
      try {
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
      } catch (final IOException e1) {
        result = SET_BINARY_IN_ERROR;
        logger.error(result + ": {}", e1.getMessage());
        return false;
      }
      changeMode(isPassive);
      isActive = true;
      return true;
    } finally {
      if (!isActive && ftpClient.getDataConnectionMode() ==
                       FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE) {
        disconnect();
      }
    }
  }

  @Override
  public final void logout() {
    result = null;
    try {
      ftpClient.logout();
    } catch (final IOException e) {
      // do nothing
    } finally {
      try {
        ftpClient.disconnect();
      } catch (final IOException f) {
        // do nothing
      }
    }
  }

  @Override
  public final void disconnect() {
    result = null;
    try {
      ftpClient.disconnect();
    } catch (final IOException e) {
      logger.debug(DISCONNECTION_IN_ERROR, e);
    }
  }

  @Override
  public final boolean makeDir(final String newDir) {
    result = null;
    try {
      return ftpClient.makeDirectory(newDir);
    } catch (final IOException e) {
      try {
        reconnect();
        return ftpClient.makeDirectory(newDir);
      } catch (final IOException e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = MKDIR_IN_ERROR;
      logger.error(result + ": {}", e.getMessage());
      waitAfterDataCommand();
      return false;
    }
  }

  @Override
  public final boolean changeDir(final String newDir) {
    result = null;
    try {
      directory = newDir;
      return ftpClient.changeWorkingDirectory(newDir);
    } catch (final IOException e) {
      try {
        reconnect();
        return ftpClient.changeWorkingDirectory(newDir);
      } catch (final IOException e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CHDIR_IN_ERROR;
      logger.error(result + ": {}", e.getMessage());
      return false;
    }
  }

  @Override
  public final boolean changeFileType(final boolean binaryTransfer) {
    result = null;
    try {
      return internalChangeFileType(binaryTransfer);
    } catch (final IOException e) {
      try {
        reconnect();
        return internalChangeFileType(binaryTransfer);
      } catch (final IOException e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = FILE_TYPE_IN_ERROR;
      logger.error(result + ": {}", e.getMessage());
      return false;
    }
  }

  private boolean internalChangeFileType(final boolean binaryTransfer)
      throws IOException {
    if (binaryTransfer) {
      if (!ftpClient.setFileType(FTP.BINARY_FILE_TYPE)) {
        reconnect();
        return ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
      }
      return true;
    } else {
      if (!ftpClient.setFileType(FTP.ASCII_FILE_TYPE)) {
        reconnect();
        if (directory != null) {
          changeDir(directory);
        }
        return ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
      }
      return true;
    }
  }

  @Override
  public final void changeMode(final boolean passive) {
    result = null;
    isPassive = passive;
    if (isPassive) {
      ftpClient.enterLocalPassiveMode();
    } else {
      ftpClient.enterLocalActiveMode();
    }
    waitAfterDataCommand();
  }

  @Override
  public final boolean store(final String local, final String remote) {
    return transferFile(local, remote, 1);
  }

  @Override
  public final boolean store(final InputStream local, final String remote) {
    return transferFile(local, remote, 1);
  }

  @Override
  public final boolean append(final String local, final String remote) {
    return transferFile(local, remote, 2);
  }

  @Override
  public final boolean append(final InputStream local, final String remote) {
    return transferFile(local, remote, 2);
  }

  @Override
  public final boolean retrieve(final String local, final String remote) {
    return transferFile(local, remote, -1);
  }

  @Override
  public final boolean retrieve(final OutputStream local, final String remote) {
    return transferFile(local, remote);
  }

  private boolean internalTransferFile(final String local, final String remote,
                                       final int getStoreOrAppend)
      throws FileNotFoundException {
    OutputStream output = null;
    FileInputStream fileInputStream = null;
    try {
      if (getStoreOrAppend > 0) {
        fileInputStream = new FileInputStream(local);
        return transferFile(fileInputStream, remote, getStoreOrAppend);
      } else {
        if (local == null) {
          // test
          logger.debug("Will DLD nullStream: {}", remote);
          output = new NullOutputStream();
        } else {
          logger.debug("Will DLD to local: {} into {}", remote, local);
          output = new FileOutputStream(local);
        }
        return transferFile(output, remote);
      }
    } finally {
      FileUtils.close(output);
      FileUtils.close(fileInputStream);
    }
  }

  @Override
  public final boolean transferFile(final String local, final String remote,
                                    final int getStoreOrAppend) {
    result = null;
    try {
      if (!internalTransferFile(local, remote, getStoreOrAppend)) {
        reconnect();
        return internalTransferFile(local, remote, getStoreOrAppend);
      }
      return true;
    } catch (final IOException e) {
      try {
        reconnect();
        return internalTransferFile(local, remote, getStoreOrAppend);
      } catch (final IOException e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_FINALIZE_TRANSFER_OPERATION;
      logger.error(result + ": {}", e.getMessage());
      return false;
    }
  }

  private boolean internalTransferFile(final InputStream local,
                                       final String remote,
                                       final int getStoreOrAppend)
      throws IOException {
    final boolean status;
    if (getStoreOrAppend == 1) {
      status = ftpClient.storeFile(remote, local);
    } else {
      // append
      status = ftpClient.appendFile(remote, local);
    }
    if (!status) {
      result = CANNOT_FINALIZE_STORE_LIKE_OPERATION;
      logger.error(result);
      return false;
    }
    return true;
  }

  @Override
  public final boolean transferFile(final InputStream local,
                                    final String remote,
                                    final int getStoreOrAppend) {
    result = null;
    try {
      if (!internalTransferFile(local, remote, getStoreOrAppend)) {
        reconnect();
        return internalTransferFile(local, remote, getStoreOrAppend);
      }
      return true;
    } catch (final IOException e) {
      try {
        reconnect();
        return internalTransferFile(local, remote, getStoreOrAppend);
      } catch (final IOException e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_FINALIZE_STORE_LIKE_OPERATION;
      logger.error(result + ": {}", e.getMessage());
      return false;
    } finally {
      waitAfterDataCommand();
    }
  }

  @Override
  public final boolean transferFile(final OutputStream local,
                                    final String remote) {
    result = null;
    boolean status;
    try {
      status = ftpClient.retrieveFile(remote, local);
      if (!status) {
        reconnect();
        status = ftpClient.retrieveFile(remote, local);
        if (!status) {
          result = CANNOT_FINALIZE_RETRIEVE_LIKE_OPERATION;
          logger.error(result);
          return false;
        }
      }
      return true;
    } catch (final IOException e) {
      try {
        reconnect();
        status = ftpClient.retrieveFile(remote, local);
        if (!status) {
          result = CANNOT_FINALIZE_RETRIEVE_LIKE_OPERATION;
          logger.error(result);
          return false;
        }
        return true;
      } catch (final IOException e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_FINALIZE_RETRIEVE_LIKE_OPERATION;
      logger.error(result + ": {}", e.getMessage());
      return false;
    } finally {
      waitAfterDataCommand();
    }
  }

  private String[] internalListFiles(final String remote, final boolean mlist)
      throws IOException {
    final FTPFile[] list;
    if (mlist) {
      list = ftpClient.listFiles(remote);
    } else {
      list = ftpClient.mlistDir(remote);
    }
    final String[] results = new String[list.length];
    int i = 0;
    for (final FTPFile file : list) {
      results[i] = file.toFormattedString();
      i++;
    }
    return results;
  }

  @Override
  public final String[] listFiles(final String remote) {
    result = null;
    try {
      return internalListFiles(remote, false);
    } catch (final IOException e) {
      try {
        reconnect();
        return internalListFiles(remote, false);
      } catch (final IOException e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_FINALIZE_TRANSFER_OPERATION;
      logger.error(result + ": {}", e.getMessage());
      return null;
    } finally {
      waitAfterDataCommand();
    }
  }

  @Override
  public final String[] listFiles() {
    return listFiles((String) null);
  }

  @Override
  public final String[] mlistFiles(final String remote) {
    result = null;
    try {
      return internalListFiles(remote, true);
    } catch (final IOException e) {
      try {
        reconnect();
        return internalListFiles(remote, true);
      } catch (final IOException e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_FINALIZE_TRANSFER_OPERATION;
      logger.error(result + ": {}", e.getMessage());
      return null;
    } finally {
      waitAfterDataCommand();
    }
  }

  @Override
  public final String[] mlistFiles() {
    return mlistFiles((String) null);
  }

  @Override
  public final String[] features() {
    result = null;
    try {
      return internalFeatures();
    } catch (final IOException e) {
      try {
        reconnect();
        return internalFeatures();
      } catch (final IOException e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_EXECUTE_OPERATION_FEATURE;
      logger.error(result + ": {}", e.getMessage());
      return null;
    }
  }

  private String[] internalFeatures() throws IOException {
    if (ftpClient.features()) {
      final String resultNew = ftpClient.getReplyString();
      return resultNew.split("\\n");
    }
    return null;
  }

  @Override
  public final boolean featureEnabled(final String feature) {
    result = null;
    try {
      return internalFeatureEnabled(feature);
    } catch (final IOException e) {
      try {
        reconnect();
        return internalFeatureEnabled(feature);
      } catch (final IOException e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_EXECUTE_OPERATION_FEATURE;
      logger.error(result + ": {}", e.getMessage());
      return false;
    }
  }

  private boolean internalFeatureEnabled(final String feature)
      throws IOException {
    if (ftpClient.featureValue(feature) == null) {
      final String resultNew = ftpClient.getReplyString();
      return resultNew.contains(feature.toUpperCase());
    }
    return true;
  }

  @Override
  public boolean deleteFile(final String remote) {
    result = null;
    try {
      ftpClient.deleteFile(remote);
      return true;
    } catch (final IOException e) {
      try {
        reconnect();
        ftpClient.deleteFile(remote);
        return true;
      } catch (final IOException e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_EXECUTE_OPERATION_SITE;
      logger.error(result + ": {}", e.getMessage());
      return false;
    }
  }

  @Override
  public final String[] executeCommand(final String params) {
    result = null;
    try {
      return internalExecuteCommand(params);
    } catch (final IOException e) {
      try {
        reconnect();
        return internalExecuteCommand(params);
      } catch (final IOException e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_EXECUTE_OPERATION_SITE;
      logger.error(result + ": {}", e.getMessage());
      return null;
    }
  }

  private String[] internalExecuteCommand(final String params)
      throws IOException {
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
  }

  @Override
  public final String[] executeSiteCommand(final String params) {
    result = null;
    try {
      return internalExecuteSiteCommand(params);
    } catch (final IOException e) {
      try {
        reconnect();
        return internalExecuteSiteCommand(params);
      } catch (final IOException e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_EXECUTE_OPERATION_SITE;
      logger.error(result + ": {}", e.getMessage());
      return null;
    }
  }

  private String[] internalExecuteSiteCommand(final String params)
      throws IOException {
    String[] results = ftpClient.doCommandAsStrings("SITE", params);
    if (results == null) {
      results = new String[1];
      results[0] = ftpClient.getReplyString();
    }
    return results;
  }

  @Override
  public final void noop() {
    try {
      ftpClient.noop();
    } catch (final IOException e) {
      try {
        reconnect();
        ftpClient.noop();
      } catch (final IOException e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = NOOP_ERROR;
      logger.error(result + ": {}", e.getMessage());
    }
  }

  /**
   * Used on Data Commands to prevent too fast command iterations
   */
  static void waitAfterDataCommand() {
    if (DEFAULT_WAIT > 0) {
      try {
        Thread.sleep(DEFAULT_WAIT);
      } catch (final InterruptedException e) { //NOSONAR
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      }
    } else {
      Thread.yield();
    }
  }
}
