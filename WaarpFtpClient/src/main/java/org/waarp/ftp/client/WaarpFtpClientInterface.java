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

import java.io.InputStream;
import java.io.OutputStream;

public interface WaarpFtpClientInterface {
  String CANNOT_EXECUTE_OPERATION_SITE = "Cannot execute operation Site";
  String CANNOT_EXECUTE_OPERATION_FEATURE = "Cannot execute operation Feature";
  String CANNOT_FINALIZE_TRANSFER_OPERATION =
      "Cannot finalize transfer operation";
  String CHDIR_IN_ERROR = "CHDIR in error";
  String MKDIR_IN_ERROR = "MKDIR in error";
  String DISCONNECTION_IN_ERROR = "Disconnection in error";
  String LOGIN_IN_ERROR = "Login in error";
  String CONNECTION_IN_ERROR = "Connection in error";
  String SET_BINARY_IN_ERROR = "Set BINARY in error";
  String CANNOT_FINALIZE_STORE_LIKE_OPERATION =
      "Cannot finalize store like operation";
  String CANNOT_FINALIZE_RETRIEVE_LIKE_OPERATION =
      "Cannot finalize retrieve like operation";
  String FILE_TYPE_IN_ERROR = "FileType in error";
  String NOOP_ERROR = "NoOp error";

  /**
   * @param ipAddress the external IP address to report in EPRT/PORT commands
   *     in active mode.
   */
  void setReportActiveExternalIPAddress(String ipAddress);

  /**
   * @param from the first port to used
   * @param to the last port to used
   */
  void setActiveDataTransferPortRange(int from, int to);

  /**
   * @return the result associated with the last command if any error
   */
  String getResult();

  /**
   * Try to connect to the server and goes with the authentication
   *
   * @return True if connected and authenticated, else False
   */
  boolean connect();

  /**
   * QUIT the control connection
   */
  void logout();

  /**
   * Disconnect the Ftp Client
   */
  void disconnect();

  /**
   * Create a new directory
   *
   * @param newDir
   *
   * @return True if created
   */
  boolean makeDir(String newDir);

  /**
   * Change remote directory
   *
   * @param newDir
   *
   * @return True if the change is OK
   */
  boolean changeDir(String newDir);

  /**
   * Change the FileType of Transfer (Binary true, ASCII false)
   *
   * @param binaryTransfer
   *
   * @return True if the change is OK
   */
  boolean changeFileType(boolean binaryTransfer);

  /**
   * Change to passive (true) or active (false) mode
   *
   * @param passive
   */
  void changeMode(boolean passive);

  /**
   * Change to ZLIB compression (true) or inactive (false) mode.
   * Only if supported.
   *
   * @param compression
   */
  void compressionMode(boolean compression);

  /**
   * Store File
   *
   * @param local local filepath (full path)
   * @param remote filename (basename)
   *
   * @return True if the file is correctly transferred
   */
  boolean store(String local, String remote);

  /**
   * Store File
   *
   * @param local local InputStream
   * @param remote filename (basename)
   *
   * @return True if the file is correctly transferred
   */
  boolean store(InputStream local, String remote);

  /**
   * Store File as Append
   *
   * @param local local filepath (full path)
   * @param remote filename (basename)
   *
   * @return True if the file is correctly transferred
   */
  boolean append(String local, String remote);

  /**
   * Store File as Append
   *
   * @param local local InputStream
   * @param remote filename (basename)
   *
   * @return True if the file is correctly transferred
   */
  boolean append(InputStream local, String remote);

  /**
   * Retrieve File
   *
   * @param local local filepath (full path)
   * @param remote filename (basename)
   *
   * @return True if the file is correctly transferred
   */
  boolean retrieve(String local, String remote);

  /**
   * Retrieve File
   *
   * @param local local OutputStream
   * @param remote filename (basename)
   *
   * @return True if the file is correctly transferred
   */
  boolean retrieve(OutputStream local, String remote);

  /**
   * Ask to transfer a file
   *
   * @param local local filepath (full path)
   * @param remote filename (basename)
   * @param getStoreOrAppend -1 = get, 1 = store, 2 = append
   *
   * @return True if the file is correctly transferred
   */
  boolean transferFile(String local, String remote, int getStoreOrAppend);

  /**
   * Ask to transfer a file as STORE or APPEND
   *
   * @param local local outputStream
   * @param remote filename (basename)
   * @param getStoreOrAppend 1 = store, 2 = append
   *
   * @return True if the file is correctly transferred
   */
  boolean transferFile(InputStream local, String remote, int getStoreOrAppend);

  /**
   * Ask to transfer a file as GET
   *
   * @param local local outputStream (or NullStream)
   * @param remote filename (basename)
   *
   * @return True if the file is correctly transferred
   */
  boolean transferFile(OutputStream local, String remote);

  /**
   * @param remote remote file spec
   *
   * @return the list of Files as given by FTP
   */
  String[] listFiles(String remote);

  /**
   * @return the list of Files as given by FTP
   */
  String[] listFiles();

  /**
   * @param remote remote file spec
   *
   * @return the list of Files as given by FTP in MLSD format
   */
  String[] mlistFiles(String remote);

  /**
   * @return the list of Files as given by FTP in MLSD format
   */
  String[] mlistFiles();

  /**
   * @return the list of Features as String
   */
  String[] features();

  /**
   * @param feature
   *
   * @return True if the given feature is listed
   */
  boolean featureEnabled(String feature);

  /**
   * @param remote
   *
   * @return True if deleted
   */
  boolean deleteFile(String remote);

  /**
   * @param params
   *
   * @return the string lines result for the command params
   */
  String[] executeCommand(String params);

  /**
   * @param params command without SITE in front
   *
   * @return the string lines result for the SITE command params
   */
  String[] executeSiteCommand(String params);

  /**
   * Sends a No Op command
   */
  void noop();
}
