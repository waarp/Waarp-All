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

import it.sauronsoftware.ftp4j.FTPAbortedException;
import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPCommunicationListener;
import it.sauronsoftware.ftp4j.FTPConnector;
import it.sauronsoftware.ftp4j.FTPDataTransferException;
import it.sauronsoftware.ftp4j.FTPException;
import it.sauronsoftware.ftp4j.FTPFile;
import it.sauronsoftware.ftp4j.FTPIllegalReplyException;
import it.sauronsoftware.ftp4j.FTPListParseException;
import it.sauronsoftware.ftp4j.FTPReply;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.SystemPropertyUtil;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import static org.waarp.common.digest.WaarpBC.*;

/**
 * FTP client using FTP4J model (working in all modes)
 */
public class WaarpFtp4jClient implements WaarpFtpClientInterface {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpFtp4jClient.class);
  private static final int DEFAULT_WAIT = 1;

  static {
    initializedTlsContext();
  }

  final String server;
  int port = 21;
  final String user;
  final String pwd;
  final String acct;
  final int timeout;
  final int keepalive;
  boolean isPassive;
  final int ssl; // -1 native, 1 auth
  protected FTPClient ftpClient;
  protected String result;
  protected String directory = null;

  /**
   * @param server
   * @param port
   * @param user
   * @param pwd
   * @param acct
   * @param isPassive
   * @param ssl -1 native, 1 auth
   * @param timeout
   */
  public WaarpFtp4jClient(final String server, final int port,
                          final String user, final String pwd,
                          final String acct, final boolean isPassive,
                          final int ssl, final int keepalive,
                          final int timeout) {
    this.server = server;
    this.port = port;
    this.user = user;
    this.pwd = pwd;
    this.acct = acct;
    this.isPassive = isPassive;
    this.ssl = ssl;
    this.keepalive = keepalive;
    this.timeout = timeout;
    ftpClient = new FTPClient();
    if (this.ssl != 0) {
      // implicit or explicit
      final TrustManager[] trustManager = {
          new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
              return null;
            }

            @Override
            public final void checkClientTrusted(final X509Certificate[] certs,
                                                 //NOSONAR
                                                 final String authType) {
              // nothing
            }

            @Override
            public final void checkServerTrusted(final X509Certificate[] certs,
//NOSONAR
                                                 final String authType) {
              // nothing
            }
          }
      };
      final SSLContext sslContext;
      try {
        sslContext = getInstanceJDK();
        sslContext.init(null, trustManager, getSecureRandom());
      } catch (final NoSuchAlgorithmException e) {
        throw new IllegalArgumentException("Bad algorithm", e);
      } catch (final KeyManagementException e) {
        throw new IllegalArgumentException("Bad KeyManagement", e);
      } catch (final Exception e) {
        throw new IllegalArgumentException("Bad Provider", e);
      }
      final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
      ftpClient.setSSLSocketFactory(sslSocketFactory);
      if (this.ssl < 0) {
        ftpClient.setSecurity(FTPClient.SECURITY_FTPS);
      } else {
        ftpClient.setSecurity(FTPClient.SECURITY_FTPES);
      }
    } else {
      ftpClient = new FTPClient();
    }
    if (timeout > 0) {
      System.setProperty("ftp4j.activeDataTransfer.acceptTimeout",
                         String.valueOf(timeout));
    }
    System.setProperty("ftp4j.activeDataTransfer.hostAddress", "127.0.0.1");

    ftpClient.addCommunicationListener(new FTPCommunicationListener() {
      @Override
      public final void sent(final String arg0) {
        logger.debug("Command: {}", arg0);
      }

      @Override
      public final void received(final String arg0) {
        logger.debug("Answer: {}", arg0);
      }
    });
    final FTPConnector connector = ftpClient.getConnector();
    int timeoutDefault = timeout > 0? timeout / 1000 : 30;
    connector.setCloseTimeout(timeoutDefault);
    connector.setReadTimeout(timeoutDefault);
    connector.setConnectionTimeout(timeoutDefault);
    connector.setUseSuggestedAddressForDataConnections(true);
  }

  @Override
  public void setReportActiveExternalIPAddress(final String ipAddress) {
    if (ipAddress != null) {
      SystemPropertyUtil.set("ftp4j.activeDataTransfer.hostAddress", ipAddress);
    } else {
      SystemPropertyUtil.clear("ftp4j.activeDataTransfer.hostAddress");
    }
  }

  @Override
  public void setActiveDataTransferPortRange(final int from, final int to) {
    if (from <= 0 || to <= 0) {
      SystemPropertyUtil.clear("ftp4j.activeDataTransfer.portRange");
    } else {
      SystemPropertyUtil.set("ftp4j.activeDataTransfer.portRange",
                             from + "-" + to);
    }
  }

  @Override
  public final String getResult() {
    return result;
  }

  private void reconnect() {
    ftpClient.setAutoNoopTimeout(0);
    try {
      ftpClient.logout();
    } catch (final Exception e) {
      // do nothing
    } finally {
      disconnect();
    }
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
      waitAfterDataCommand();
      Exception lastExcemption = null;
      for (int i = 0; i < 5; i++) {
        try {
          ftpClient.connect(server, port);
          lastExcemption = null;
          break;
        } catch (final SocketException e) {
          result = CONNECTION_IN_ERROR;
          lastExcemption = e;
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
      try {
        if (acct == null) {
          // no account
          ftpClient.login(user, pwd);
        } else {
          ftpClient.login(user, pwd, acct);
        }
      } catch (final Exception e) {
        logout();
        result = LOGIN_IN_ERROR;
        logger.error(result);
        return false;
      }
      try {
        ftpClient.setType(FTPClient.TYPE_BINARY);
      } catch (final IllegalArgumentException e1) {
        result = SET_BINARY_IN_ERROR;
        logger.error(result + ": {}", e1.getMessage());
        return false;
      }
      changeMode(isPassive);
      if (keepalive > 0) {
        ftpClient.setAutoNoopTimeout(keepalive);
      }
      isActive = true;
      return true;
    } finally {
      if (!isActive && !ftpClient.isPassive()) {
        disconnect();
      }
    }
  }

  @Override
  public final void logout() {
    result = null;
    ftpClient.setAutoNoopTimeout(0);
    logger.debug("QUIT");
    if (executeCommand("QUIT") == null) {
      try {
        ftpClient.logout();
      } catch (final Exception e) {
        // do nothing
      } finally {
        disconnect();
      }
    }
  }

  @Override
  public final void disconnect() {
    result = null;
    ftpClient.setAutoNoopTimeout(0);
    try {
      ftpClient.disconnect(false);
    } catch (final Exception e) {
      logger.debug(DISCONNECTION_IN_ERROR, e);
    }
  }

  @Override
  public final boolean makeDir(final String newDir) {
    result = null;
    try {
      ftpClient.createDirectory(newDir);
      return true;
    } catch (final Exception e) {
      try {
        reconnect();
        ftpClient.createDirectory(newDir);
        return true;
      } catch (final Exception e1) {
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
      ftpClient.changeDirectory(newDir);
      return true;
    } catch (final IOException e) {
      try {
        reconnect();
        ftpClient.changeDirectory(newDir);
        return true;
      } catch (final Exception e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CHDIR_IN_ERROR;
      logger.error(result + ": {}", e.getMessage());
      return false;
    } catch (final Exception e) {
      try {
        reconnect();
        ftpClient.changeDirectory(newDir);
        return true;
      } catch (final Exception e1) {
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
      if (binaryTransfer) {
        ftpClient.setType(FTPClient.TYPE_BINARY);
      } else {
        ftpClient.setType(FTPClient.TYPE_TEXTUAL);
      }
      return true;
    } catch (final IllegalArgumentException e) {
      result = FILE_TYPE_IN_ERROR;
      logger.error(result + ": {}", e.getMessage());
      return false;
    }
  }

  @Override
  public final void changeMode(final boolean passive) {
    result = null;
    isPassive = passive;
    ftpClient.setPassive(passive);
    waitAfterDataCommand();
  }

  @Override
  public void compressionMode(final boolean compression) {
    if (compression) {
      if (ftpClient.isCompressionSupported()) {
        try {
          ftpClient.setType(FTPClient.TYPE_BINARY);
        } catch (final IllegalArgumentException e1) {
          result = SET_BINARY_IN_ERROR;
          logger.error(result + ": {}", e1.getMessage());
        }
        ftpClient.setCompressionEnabled(true);
      } else {
        logger.warn("Z Compression not supported by Server");
        ftpClient.setCompressionEnabled(false);
      }
    } else {
      ftpClient.setCompressionEnabled(false);
    }
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
      } catch (final Exception e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_FINALIZE_TRANSFER_OPERATION;
      logger.error(result + ": {}", e.getMessage());
      return false;
    }
  }

  private boolean internalTransferFile(final String local, final String remote,
                                       final int getStoreOrAppend)
      throws FileNotFoundException {
    if (getStoreOrAppend > 0) {
      final File from = new File(local);
      logger.debug("Will STOR: {}", from);
      FileInputStream stream = null;
      try {
        stream = new FileInputStream(local);
        return transferFile(stream, remote, getStoreOrAppend);
      } finally {
        FileUtils.close(stream);
      }
    } else {
      OutputStream outputStream = null;
      if (local == null) {
        // test
        logger.debug("Will DLD nullStream: {}", remote);
        outputStream = new NullOutputStream();
      } else {
        logger.debug("Will DLD to local: {} into {}", remote, local);
        outputStream = new FileOutputStream(local);
      }
      try {
        return transferFile(outputStream, remote);
      } finally {
        FileUtils.close(outputStream);
      }
    }
  }

  @Override
  public final boolean transferFile(final InputStream local,
                                    final String remote,
                                    final int getStoreOrAppend) {
    result = null;
    result = CANNOT_FINALIZE_STORE_LIKE_OPERATION;
    logger.debug("Will STOR to: {}", remote);
    try {
      internalTransferFile(local, remote, getStoreOrAppend);
      return true;
    } catch (final Exception e) {
      try {
        reconnect();
        return internalTransferFile(local, remote, getStoreOrAppend);
      } catch (final Exception e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      logger.error(result + ": {}", e.getMessage());
      return false;
    } finally {
      waitAfterDataCommand();
    }
  }

  private boolean internalTransferFile(final InputStream local,
                                       final String remote,
                                       final int getStoreOrAppend)
      throws IOException, FTPIllegalReplyException, FTPException,
             FTPDataTransferException, FTPAbortedException {
    if (getStoreOrAppend == 1) {
      ftpClient.upload(remote, local, 0, 0, null);
    } else {
      // append
      ftpClient.append(remote, local, 0, null);
    }
    result = null;
    return true;
  }

  @Override
  public final boolean transferFile(final OutputStream local,
                                    final String remote) {
    result = null;
    result = CANNOT_FINALIZE_RETRIEVE_LIKE_OPERATION;
    logger.debug("Will DLD nullStream: {}", remote);
    try {
      ftpClient.download(remote, local, 0, null);
      result = null;
      return true;
    } catch (final Exception e) {
      try {
        reconnect();
        ftpClient.download(remote, local, 0, null);
        result = null;
        return true;
      } catch (final Exception e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      logger.error(result + ": {}", e.getMessage(), e);
      return false;
    } finally {
      waitAfterDataCommand();
    }
  }

  @Override
  public final String[] listFiles(final String remote) {
    result = null;
    ftpClient.setMLSDPolicy(FTPClient.MLSD_NEVER);
    try {
      return internalListFiles(remote);
    } catch (final Exception e) {
      try {
        reconnect();
        return internalListFiles(remote);
      } catch (final Exception e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_FINALIZE_TRANSFER_OPERATION;
      logger.error(result + ": {}", e.getMessage());
      return null;
    } finally {
      waitAfterDataCommand();
    }
  }

  private String[] internalListFiles(final String remote)
      throws IOException, FTPIllegalReplyException, FTPException,
             FTPDataTransferException, FTPAbortedException,
             FTPListParseException {
    final FTPFile[] list = ftpClient.list(remote);
    final String[] results = new String[list.length];
    int i = 0;
    for (final FTPFile file : list) {
      results[i] = file.toString();
      i++;
    }
    return results;
  }

  @Override
  public final String[] listFiles() {
    return listFiles((String) null);
  }

  @Override
  public final String[] mlistFiles(final String remote) {
    ftpClient.setMLSDPolicy(FTPClient.MLSD_ALWAYS);
    return listFiles(remote);
  }

  @Override
  public final String[] mlistFiles() {
    return mlistFiles((String) null);
  }


  @Override
  public final String[] features() {
    result = null;
    try {
      final FTPReply reply = ftpClient.sendCustomCommand("FEAT");
      return reply.getMessages();
    } catch (final IOException e) {
      try {
        reconnect();
        final FTPReply reply = ftpClient.sendCustomCommand("FEAT");
        return reply.getMessages();
      } catch (final Exception e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_EXECUTE_OPERATION_FEATURE;
      logger.error(result + ": {}", e.getMessage());
      return null;
    } catch (final Exception e) {
      try {
        reconnect();
        final FTPReply reply = ftpClient.sendCustomCommand("FEAT");
        return reply.getMessages();
      } catch (final Exception e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_EXECUTE_OPERATION_FEATURE;
      logger.error(result + ": {}", e.getMessage());
      return null;
    }
  }

  @Override
  public final boolean featureEnabled(final String feature) {
    result = null;
    try {
      return internalFeatureEnabled(feature);
    } catch (final Exception e) {
      try {
        reconnect();
        return internalFeatureEnabled(feature);
      } catch (final Exception e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_EXECUTE_OPERATION_FEATURE;
      logger.error(result + ": {}", e.getMessage());
      return false;
    }
  }

  private boolean internalFeatureEnabled(final String feature)
      throws IOException, FTPIllegalReplyException {
    final FTPReply reply = ftpClient.sendCustomCommand("FEAT");
    final String[] msg = reply.getMessages();
    for (final String string : msg) {
      if (string.contains(feature.toUpperCase())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean deleteFile(final String remote) {
    result = null;
    try {
      logger.debug("DELE {}", remote);
      ftpClient.deleteFile(remote);
      return true;
    } catch (final Exception e) {
      try {
        reconnect();
        ftpClient.deleteFile(remote);
        return true;
      } catch (final Exception e1) {
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
      logger.debug(params);
      FTPReply reply = ftpClient.sendCustomCommand(params);
      if (!reply.isSuccessCode()) {
        reconnect();
        reply = ftpClient.sendCustomCommand(params);
        if (!reply.isSuccessCode()) {
          result = reply.toString();
          return null;
        }
      }
      return reply.getMessages();
    } catch (final Exception e) {
      try {
        reconnect();
        final FTPReply reply = ftpClient.sendCustomCommand(params);
        if (!reply.isSuccessCode()) {
          result = reply.toString();
          return null;
        }
        return reply.getMessages();
      } catch (final Exception e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_EXECUTE_OPERATION_SITE;
      logger.error(result + ": {}", e.getMessage());
      return null;
    }
  }

  @Override
  public final String[] executeSiteCommand(final String params) {
    result = null;
    try {
      logger.debug("SITE {}", params);
      FTPReply reply = ftpClient.sendSiteCommand(params);
      if (!reply.isSuccessCode()) {
        reconnect();
        reply = ftpClient.sendSiteCommand(params);
        if (!reply.isSuccessCode()) {
          result = reply.toString();
          return null;
        }
      }
      return reply.getMessages();
    } catch (final Exception e) {
      try {
        reconnect();
        final FTPReply reply = ftpClient.sendSiteCommand(params);
        if (!reply.isSuccessCode()) {
          result = reply.toString();
          return null;
        }
        return reply.getMessages();
      } catch (final Exception e1) {
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
      result = CANNOT_EXECUTE_OPERATION_SITE;
      logger.error(result + ": {}", e.getMessage());
      return null;
    }
  }

  @Override
  public final void noop() {
    try {
      ftpClient.noop();
    } catch (final Exception e) {
      try {
        reconnect();
        ftpClient.noop();
      } catch (final Exception e1) {
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
