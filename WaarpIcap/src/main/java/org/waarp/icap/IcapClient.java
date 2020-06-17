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

package org.waarp.icap;

import com.google.common.io.Files;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpStringUtils;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * The IcapClient allows to do 3 actions:</br>
 * <ul>
 *   <li>connect(): which allows to initialize the connection with the ICAP
 *   server</li>
 *   <li>close(): forces the client to disconnect from the ICAP server</li>
 *   <li>scanFile(path): send a file for a scan by the ICAP server</li>
 * </ul>
 * </br>
 * This code is inspired from 2 sources:</br>
 * <ul>
 *   <li>https://github.com/Baekalfen/ICAP-avscan</li>
 *   <li>https://github.com/claudineyns/icap-client</li>
 * </ul>
 * </br>
 * This reflects the RFC 3507 and errata as of 2010/04/17.
 */
public class IcapClient implements Closeable {
  /**
   * Default ICAP port
   */
  public static final int DEFAULT_ICAP_PORT = 1344;
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(IcapClient.class);

  static final int STD_RECEIVE_LENGTH = 64 * 1024;
  static final int STD_SEND_LENGTH = 8192;
  static final int DEFAULT_TIMEOUT = 10 * 60 * 60000;// 10 min
  public static final String VERSION = "ICAP/1.0";
  private static final String USER_AGENT = "Waarp ICAP Client/1.0";
  public static final String TERMINATOR = "\r\n";
  public static final String ICAP_TERMINATOR = TERMINATOR + TERMINATOR;
  public static final String HTTP_TERMINATOR = "0" + TERMINATOR + TERMINATOR;
  private static final String STATUS_CODE = "StatusCode";
  private static final String PREVIEW = "Preview";
  private static final String OPTIONS = "OPTIONS";
  private static final String HOST_HEADER = "Host: ";
  private static final String USER_AGENT_HEADER = "User-Agent: ";
  private static final String RESPMOD = "RESPMOD";
  private static final String ENCAPSULATED_NULL_BODY =
      "Encapsulated: null-body=0";
  static final int MINIMAL_SIZE = 100;
  private static final String GET_REQUEST = "GET /";
  private static final String INCOMPATIBLE_ARGUMENT = "Incompatible argument";
  private static final String TIMEOUT_OCCURS_WITH_THE_SERVER =
      "Timeout occurs with the Server";
  private static final String TIMEOUT_OCCURS_WITH_THE_SERVER_SINCE =
      "Timeout occurs with the Server {}:{} since {}";
  public static final String EICARTEST = "EICARTEST";

  // Standard configuration
  private final String serverIP;
  private final int port;
  private final String icapService;
  private final int setPreviewSize;

  // Extra configuration
  private int receiveLength = STD_RECEIVE_LENGTH;
  private int sendLength = STD_SEND_LENGTH;
  private String keyIcapPreview = null;
  private String subStringFromKeyIcapPreview = null;
  private String substringHttpStatus200 = null;
  private String keyIcap200 = null;
  private String subStringFromKeyIcap200 = null;
  private String keyIcap204 = null;
  private String subStringFromKeyIcap204 = null;
  private long maxSize = Integer.MAX_VALUE;
  private int timeout = DEFAULT_TIMEOUT;
  private int stdPreviewSize = -1;

  // Accessibe data
  private Map<String, String> finalResult = null;

  // Internal data
  private Socket client;
  private OutputStream out;
  InputStream in;
  private int offset;

  /**
   * This creates the ICAP client without connecting immediately to the ICAP
   * server. When the ICAP client will connect, it will ask for the preview
   * size to the ICAP Server.
   *
   * @param serverIP The IP address to connect to.
   * @param port The port in the host to use.
   * @param icapService The service to use (fx "avscan").
   */
  public IcapClient(final String serverIP, final int port,
                    final String icapService) {
    this(serverIP, port, icapService, -1);
  }

  /**
   * This creates the ICAP client without connecting immediately to the ICAP
   * server. When the ICAP client will connect, it will not ask for the preview
   * size to the ICAP Server but uses the default specified value.
   *
   * @param serverIP The IP address to connect to.
   * @param port The port in the host to use.
   * @param icapService The service to use (fx "avscan").
   * @param previewSize Amount of bytes to  send as preview.
   */
  public IcapClient(final String serverIP, final int port,
                    final String icapService, final int previewSize) {
    if (icapService == null || icapService.trim().isEmpty()) {
      throw new IllegalArgumentException("IcapService must not be empty");
    }
    this.icapService = icapService;
    if (serverIP == null || serverIP.trim().isEmpty()) {
      throw new IllegalArgumentException("Server IP must not be empty");
    }
    this.serverIP = serverIP;
    if (port <= 0) {
      this.port = DEFAULT_ICAP_PORT;
    } else {
      this.port = port;
    }
    this.setPreviewSize = previewSize;
    this.stdPreviewSize = Math.max(0, previewSize);
  }

  /**
   * Try to connect to the server and if the preview size is not specified,
   * it will also resolve the options of the ICAP server.</br>
   *
   * If the client is still connected, it will first disconnect before
   * reconnecting to the ICAP Server.</br>
   *
   * Note that every attempts of connection will retry to issue an OPTIONS
   * request if necessary (if preview size is not set to a fixed value already).
   *
   * @throws IcapException if an issue occurs during the connection or
   *     response (the connection is already closed)
   */
  public IcapClient connect() throws IcapException {
    if (finalResult != null) {
      finalResult.clear();
      finalResult = null;
    }
    if (client != null) {
      close();
    }
    logger
        .debug("Try connect to {}:{} service {}", serverIP, port, icapService);
    try {
      // Initialize connection
      client = new Socket(serverIP, port);
      client.setReuseAddress(true);
      client.setKeepAlive(true);
      client.setSoTimeout(timeout);
      client.setTcpNoDelay(false);
      // Opening out stream
      out = client.getOutputStream();
      // Opening in stream
      in = client.getInputStream();
      if (setPreviewSize < 0) {
        getFromServerPreviewSize();
      }
      logger.debug("Connected with Preview Size = {}", stdPreviewSize);
      return this;
    } catch (SocketTimeoutException e) {
      close();
      logger.error(TIMEOUT_OCCURS_WITH_THE_SERVER_SINCE, serverIP, port,
                   e.getMessage());
      throw new IcapException(TIMEOUT_OCCURS_WITH_THE_SERVER, e,
                              IcapError.ICAP_TIMEOUT_ERROR);
    } catch (ConnectException e) {
      close();
      logger.error("Could not connect to server {}:{} since {}", serverIP, port,
                   e.getMessage());
      throw new IcapException("Could not connect with the server", e,
                              IcapError.ICAP_CANT_CONNECT);
    } catch (IOException e) {
      close();
      logger.error("Could not connect to server {}:{} since {}", serverIP, port,
                   e.getMessage());
      throw new IcapException("Could not connect with the server", e,
                              IcapError.ICAP_NETWORK_ERROR);
    } catch (IcapException e) {
      close();
      throw e;
    }
  }

  /**
   * Get the Preview Size from the SERVER using ICAP OPTIONS command
   *
   * @throws IcapException
   */
  private void getFromServerPreviewSize() throws IcapException {
    // Check the preview size from the ICAP Server response to OPTIONS
    String parseMe = getOptions();
    finalResult = parseHeader(parseMe);
    if (checkAgainstIcapHeader(finalResult, STATUS_CODE, "200", false)) {
      String tempString = finalResult.get(PREVIEW);
      if (tempString != null) {
        stdPreviewSize = Integer.parseInt(tempString);
        if (stdPreviewSize < 0) {
          stdPreviewSize = 0;
        }
        if (!checkAgainstIcapHeader(finalResult, keyIcapPreview,
                                    subStringFromKeyIcapPreview, true)) {
          close();
          logger.error("Could not validate preview from server");
          throw new IcapException("Could not validate preview from server",
                                  IcapError.ICAP_SERVER_MISSING_INFO);
        }
      } else {
        close();
        logger.error("Could not get preview size from server");
        throw new IcapException("Could not get preview size from server",
                                IcapError.ICAP_SERVER_MISSING_INFO);
      }
    } else {
      close();
      logger
          .error("Could not get options from server {}:{} service {}", serverIP,
                 port, icapService);
      throw new IcapException("Could not get options from server",
                              IcapError.ICAP_SERVER_MISSING_INFO);
    }
  }

  @Override
  public void close() {
    if (client != null) {
      try {
        client.close();
      } catch (IOException ignored) {
        // Nothing
      }
      client = null;
    }
    if (in != null) {
      try {
        in.close();
      } catch (IOException ignored) {
        // Nothing
      }
      in = null;
    }
    if (out != null) {
      try {
        out.close();
      } catch (IOException ignored) {
        // Nothing
      }
      out = null;
    }
  }

  /**
   * Given a file path, it will send the file to the server and return true,
   * if the server accepts the file. Visa-versa, false if the server rejects
   * it.</br>
   *
   * Note that if the client is not connected, it will first call connect().
   *
   * @param filename Relative or absolute file path to a file. If filename is
   *     EICARTEST, then a build on the fly EICAR test file is sent.
   *
   * @return Returns true when no infection is found.
   *
   * @throws IcapException if an error occurs (network, file reading,
   *     bad headers)
   */
  public boolean scanFile(final String filename) throws IcapException {
    if (filename == null || filename.trim().isEmpty()) {
      throw new IllegalArgumentException("Filename must not be empty");
    }
    if (client == null) {
      connect();
    }
    if (finalResult != null) {
      finalResult.clear();
      finalResult = null;
    }
    String originalFilename = filename;
    InputStream inputStream = null;
    long length;
    if (EICARTEST.equals(filename)) {
      // Special file to test from EICAR Test file
      final ClassLoader classLoader = IcapClient.class.getClassLoader();
      final File fileSrc1 =
          new File(classLoader.getResource("eicar.com-part1.txt").getFile());
      final File fileSrc2 =
          new File(classLoader.getResource("eicar.com-part2.txt").getFile());
      if (fileSrc1.exists() && fileSrc2.exists()) {
        try {
          final byte[] array1 = Files.toByteArray(fileSrc1);
          final byte[] array2 = Files.toByteArray(fileSrc2);
          final byte[] array =
              Arrays.copyOf(array1, array1.length + array2.length);
          System.arraycopy(array2, 0, array, array1.length, array2.length);
          inputStream = new ByteArrayInputStream(array);
          length = array.length;
        } catch (IOException e) {
          logger.error("File EICAR TEST does not exist", e);
          throw new IcapException("File EICAR TEST cannot be found",
                                  IcapError.ICAP_ARGUMENT_ERROR);
        }
      } else {
        logger.error("File EICAR TEST does not exist");
        throw new IcapException("File EICAR TEST cannot be found",
                                IcapError.ICAP_ARGUMENT_ERROR);
      }
    } else {
      File file = new File(filename);
      if (!file.canRead()) {
        logger.error("File does not exist: {}", file.getAbsolutePath());
        throw new IcapException(
            "File cannot be found: " + file.getAbsolutePath(),
            IcapError.ICAP_ARGUMENT_ERROR);
      }
      length = file.length();
      if (length > maxSize) {
        logger.error("File size {} exceed limit of {}: {}", length, maxSize,
                     file.getAbsolutePath());
        throw new IcapException(
            "File exceed limit size: " + file.getAbsolutePath(),
            IcapError.ICAP_FILE_LENGTH_ERROR);
      }
      try {
        inputStream = new FileInputStream(file);
      } catch (FileNotFoundException e) {
        logger.error("Could not find file {} since {}", originalFilename,
                     e.getMessage());
        throw new IcapException("File cannot be found: " + originalFilename, e,
                                IcapError.ICAP_ARGUMENT_ERROR);
      }
    }
    try {
      return scanFile(originalFilename, inputStream, length);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException ignored) {
          // Nothing
        }
      }
      close();
    }
  }

  /**
   * @return the Server IP
   */
  public String getServerIP() {
    return serverIP;
  }

  /**
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * @return the ICAP service
   */
  public String getIcapService() {
    return icapService;
  }

  /**
   * @return the current Preview size
   */
  public int getPreviewSize() {
    return stdPreviewSize;
  }

  /**
   * @param previewSize the receive length to set
   *
   * @return This
   */
  public IcapClient setPreviewSize(final int previewSize) {
    if (previewSize < 0) {
      logger.error(INCOMPATIBLE_ARGUMENT);
      throw new IllegalArgumentException("Preview cannot be 0 or positive");
    }
    this.stdPreviewSize = previewSize;
    return this;
  }

  /**
   * @return the current Receive length
   */
  public int getReceiveLength() {
    return receiveLength;
  }

  /**
   * @param receiveLength the receive length to set
   *
   * @return This
   */
  public IcapClient setReceiveLength(final int receiveLength) {
    if (receiveLength < MINIMAL_SIZE) {
      logger.error(INCOMPATIBLE_ARGUMENT);
      throw new IllegalArgumentException(
          "Receive length cannot be less than " + MINIMAL_SIZE);
    }
    this.receiveLength = receiveLength;
    return this;
  }

  /**
   * @return the current Send length
   */
  public int getSendLength() {
    return sendLength;
  }

  /**
   * @param sendLength the send length to set
   *
   * @return This
   */
  public IcapClient setSendLength(final int sendLength) {
    if (sendLength < MINIMAL_SIZE) {
      logger.error(INCOMPATIBLE_ARGUMENT);
      throw new IllegalArgumentException(
          "Send length cannot be less than " + MINIMAL_SIZE);
    }
    this.sendLength = sendLength;
    return this;
  }

  /**
   * @return the current max file size (default being Integer.MAX_VALUE)
   */
  public long getMaxSize() {
    return maxSize;
  }

  /**
   * @param maxSize the maximum file size to set
   *
   * @return This
   */
  public IcapClient setMaxSize(final long maxSize) {
    if (maxSize < MINIMAL_SIZE) {
      logger.error(INCOMPATIBLE_ARGUMENT);
      throw new IllegalArgumentException(
          "Maximum file size length cannot be less than " + MINIMAL_SIZE);
    }
    this.maxSize = maxSize;
    return this;
  }

  /**
   * @return the current time out for connection
   */
  public long getTimeout() {
    return timeout;
  }

  /**
   * @param timeout the timeout to use on connection
   *
   * @return This
   */
  public IcapClient setTimeout(final int timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
   * @return the current key in ICAP headers to find with 200 status in PREVIEW
   *     (or null if none)
   */
  public String getKeyIcapPreview() {
    return keyIcapPreview;
  }

  /**
   * @param keyIcapPreview the key in ICAP headers to find with 200 status in
   *     PREVIEW (or null if none)
   *
   * @return This
   */
  public IcapClient setKeyIcapPreview(final String keyIcapPreview) {
    if (keyIcapPreview != null && keyIcapPreview.isEmpty()) {
      this.keyIcapPreview = null;
    } else {
      this.keyIcapPreview = keyIcapPreview;
    }
    return this;
  }

  /**
   * @return the current subString to find in key ICAP header with 200 status
   *     in PREVIEW (or null if none)
   */
  public String getSubStringFromKeyIcapPreview() {
    return subStringFromKeyIcapPreview;
  }

  /**
   * @param subStringFromKeyIcapPreview the subString to find in key ICAP header
   *     with 200 status in PREVIEW (or null if none)
   *
   * @return This
   */
  public IcapClient setSubStringFromKeyIcapPreview(
      final String subStringFromKeyIcapPreview) {
    if (subStringFromKeyIcapPreview != null &&
        subStringFromKeyIcapPreview.isEmpty()) {
      this.subStringFromKeyIcapPreview = null;
    } else {
      this.subStringFromKeyIcapPreview = subStringFromKeyIcapPreview;
    }
    return this;
  }

  /**
   * @return the current subString to find in Http with 200 status
   *     (or null if none)
   */
  public String getSubstringHttpStatus200() {
    return substringHttpStatus200;
  }

  /**
   * @param substringHttpStatus200 the subString to find in Http with 200 status
   *     (or null if none)
   *
   * @return This
   */
  public IcapClient setSubstringHttpStatus200(
      final String substringHttpStatus200) {
    if (substringHttpStatus200 != null && substringHttpStatus200.isEmpty()) {
      this.substringHttpStatus200 = null;
    } else {
      this.substringHttpStatus200 = substringHttpStatus200;
    }
    return this;
  }

  /**
   * @return the current key in ICAP headers to find with 200 status
   *     (or null if none)
   */
  public String getKeyIcap200() {
    return keyIcap200;
  }

  /**
   * @param keyIcap200 the key in ICAP headers to find with 200 status
   *     (or null if none)
   *
   * @return This
   */
  public IcapClient setKeyIcap200(final String keyIcap200) {
    if (keyIcap200 != null && keyIcap200.isEmpty()) {
      this.keyIcap200 = null;
    } else {
      this.keyIcap200 = keyIcap200;
    }
    return this;
  }

  /**
   * @return the current subString to find in key ICAP header with 200 status
   *     (or null if none)
   */
  public String getSubStringFromKeyIcap200() {
    return subStringFromKeyIcap200;
  }

  /**
   * @param subStringFromKeyIcap200 the subString to find in key ICAP header with 200 status
   *     (or null if none)
   *
   * @return This
   */
  public IcapClient setSubStringFromKeyIcap200(
      final String subStringFromKeyIcap200) {
    if (subStringFromKeyIcap200 != null && subStringFromKeyIcap200.isEmpty()) {
      this.subStringFromKeyIcap200 = null;
    } else {
      this.subStringFromKeyIcap200 = subStringFromKeyIcap200;
    }
    return this;
  }

  /**
   * @return the current key in ICAP headers to find with 204 status
   *     (or null if none)
   */
  public String getKeyIcap204() {
    return keyIcap204;
  }

  /**
   * @param keyIcap204 the key in ICAP headers to find with 204 status
   *     (or null if none)
   *
   * @return This
   */
  public IcapClient setKeyIcap204(final String keyIcap204) {
    if (keyIcap204 != null && keyIcap204.isEmpty()) {
      this.keyIcap204 = null;
    } else {
      this.keyIcap204 = keyIcap204;
    }
    return this;
  }

  /**
   * @return the current subString to find in key ICAP header with 204 status
   *     (or null if none)
   */
  public String getSubStringFromKeyIcap204() {
    return subStringFromKeyIcap204;
  }

  /**
   * @param subStringFromKeyIcap204 the subString to find in key ICAP header with 204 status
   *     (or null if none)
   *
   * @return This
   */
  public IcapClient setSubStringFromKeyIcap204(
      final String subStringFromKeyIcap204) {
    if (subStringFromKeyIcap204 != null && subStringFromKeyIcap204.isEmpty()) {
      this.subStringFromKeyIcap204 = null;
    } else {
      this.subStringFromKeyIcap204 = subStringFromKeyIcap204;
    }
    return this;
  }

  /**
   * @return the current map of result (null if none)
   */
  public Map<String, String> getFinalResult() {
    return finalResult;
  }

  /**
   * Automatically asks for the servers available options and returns the raw
   * response as a String.
   *
   * @return String of the servers response
   *
   * @throws IcapException if an error occurs (network, bad headers)
   */
  private String getOptions() throws IcapException {
    // Send OPTIONS header and receive response
    // Sending
    StringBuilder builder = new StringBuilder();
    addIcapUri(builder, OPTIONS);
    String requestHeader =
        builder.append(ENCAPSULATED_NULL_BODY).append(ICAP_TERMINATOR)
               .toString();

    sendString(requestHeader, true);
    // Receiving
    return getHeaderIcap();
  }

  /**
   * Real method to send file for scanning through RESPMOD request
   *
   * @param originalFilename the original filename
   * @param fileInStream the file inputStream
   * @param fileSize the file size
   *
   * @return True if the scan is OK, else False if the scan is KO
   *
   * @throws IcapException if an error occurs (network, file reading,
   *     bad headers)
   */
  private boolean scanFile(final String originalFilename,
                           final InputStream fileInStream, final long fileSize)
      throws IcapException {
    int previewSize = sendIcapHttpScanRequest(originalFilename, fileSize);

    // Sending preview or, if smaller than previewSize, the whole file.
    if (previewSize == 0) {
      // Send an empty preview
      logger.debug("Empty PREVIEW");
      StringBuilder builder = new StringBuilder();
      builder.append(Integer.toHexString(previewSize)).append(TERMINATOR);
      builder.append(HTTP_TERMINATOR);
      sendString(builder.toString(), true);
    } else {
      logger.debug("PREVIEW of {}", previewSize);
      byte[] chunk = new byte[previewSize];
      int read = readChunk(fileInStream, chunk, previewSize);
      if (read != previewSize) {
        logger.warn("Read file size {} is less than preview size {}", read,
                    previewSize);
      }
      // Send the preview
      StringBuilder builder = new StringBuilder();
      builder.append(Integer.toHexString(read)).append(TERMINATOR);
      sendString(builder.toString());
      sendBytes(chunk, read);
      sendString(TERMINATOR);
      if (fileSize <= previewSize) {
        logger.debug("PREVIEW and COMPLETE");
        sendString("0; ieof" + ICAP_TERMINATOR, true);
      } else {
        logger.debug("PREVIEW but could send more");
        sendString(HTTP_TERMINATOR, true);
      }
    }
    // Parse the response: It might be "100 continue" as
    // a "go" for the rest of the file or a stop there already.
    if (fileSize > previewSize) {
      final int preview = checkPreview();
      if (preview != 0) {
        logger.debug("PREVIEW is enough and status {}", preview == 1);
        return preview == 1;
      }
      logger.debug("PREVIEW is not enough");
      sendNextFileChunks(fileInStream);
    }
    return checkFinalResponse();
  }

  /**
   * Send the Icap Http Scan Reaquest
   *
   * @param originalFilename
   * @param fileSize
   *
   * @return the preview size
   *
   * @throws IcapException
   */
  private int sendIcapHttpScanRequest(final String originalFilename,
                                      final long fileSize)
      throws IcapException {
    // HTTP part of header
    String resHeader;
    StringBuilder builder = new StringBuilder(GET_REQUEST);
    try {
      builder
          .append(URLEncoder.encode(originalFilename, WaarpStringUtils.UTF_8))
          .append(" HTTP/1.1").append(TERMINATOR);
      builder.append(HOST_HEADER).append(serverIP).append(":").append(port)
             .append(ICAP_TERMINATOR);
      resHeader = builder.toString();
    } catch (UnsupportedEncodingException e) {
      logger.error("Unsupported Encoding: {}", e.getMessage());
      throw new IcapException(e.getMessage(), e, IcapError.ICAP_INTERNAL_ERROR);
    }
    builder.append("HTTP/1.1 200 OK").append(TERMINATOR);
    builder.append("Transfer-Encoding: chunked").append(TERMINATOR);
    builder.append("Content-Length: ").append(fileSize).append(ICAP_TERMINATOR);
    String resBody = builder.toString();

    int previewSize = stdPreviewSize;
    if (fileSize < stdPreviewSize) {
      previewSize = (int) fileSize;
    }

    // ICAP part of header
    builder.setLength(0);
    addIcapUri(builder, RESPMOD);
    builder.append(PREVIEW).append(": ").append(previewSize).append(TERMINATOR);
    builder.append("Encapsulated: req-hdr=0, res-hdr=")
           .append(resHeader.length()).append(", res-body=")
           .append(resBody.length()).append(ICAP_TERMINATOR);
    builder.append(resBody);
    String requestBuffer = builder.toString();

    sendString(requestBuffer);
    return previewSize;
  }

  /**
   * Common part of ICAP URI between OPTIONS and RESPMOD
   *
   * @param builder the empty StringBuilder
   * @param method the method to associate with this ICAP URI
   */
  private void addIcapUri(final StringBuilder builder, final String method) {
    builder.append(method).append(" icap://").append(serverIP).append("/")
           .append(icapService).append(" ").append(VERSION).append(TERMINATOR);
    builder.append(HOST_HEADER).append(serverIP).append(TERMINATOR);
    builder.append(USER_AGENT_HEADER).append(USER_AGENT).append(TERMINATOR);
    builder.append("Allow: 204").append(TERMINATOR);
  }

  /**
   * Check the preview for the file scanning request
   *
   * @return 1 or -1 if the antivirus already validated/invalidated
   *     the file, or 0 if the next chunks are needed
   *
   * @throws IcapException if any error occurs (network, file reading,
   *     bad headers)
   */
  private int checkPreview() throws IcapException {
    final int status;
    String parseMe = getHeaderIcap();
    finalResult = parseHeader(parseMe);

    String tempString = finalResult.get(STATUS_CODE);
    if (tempString != null) {
      status = Integer.parseInt(tempString);
      switch (status) {
        case 100:
          logger.debug("Recv ICAP Preview Status Continue");
          return 0; //Continue transfer
        case 200:
          logger.debug("Recv ICAP Preview Status Abort");
          return -1;
        case 204:
          logger.debug("Recv ICAP Preview Status Accepted");
          return 1;
        case 404:
          logger.error("404: ICAP Service not found");
          throw new IcapException("404: ICAP Service not found",
                                  IcapError.ICAP_SERVER_SERVICE_UNKNOWN);
        default:
          logger.error("Server returned unknown status code: {}", status);
          throw new IcapException(
              "Server returned unknown status code: " + status,
              IcapError.ICAP_SERVER_UNKNOWN_CODE);
      }
    }
    logger.error("Server returned unknown status code");
    throw new IcapException("Server returned unknown status code",
                            IcapError.ICAP_SERVER_UNKNOWN_CODE);
  }

  /**
   * Check the final response for the file scanning request
   *
   * @return True if validated file, False if not
   *
   * @throws IcapException if any error occurs (network, file reading,
   *     bad headers)
   */
  private boolean checkFinalResponse() throws IcapException {
    final int status;
    String parseMe = getHeaderIcap();
    finalResult = parseHeader(parseMe);

    String tempString = finalResult.get(STATUS_CODE);
    if (tempString != null) {
      status = Integer.parseInt(tempString);

      if (status == 204) {
        // Unmodified
        logger.debug("Almost final status is {}", status);
        return checkAgainstIcapHeader(finalResult, keyIcap204,
                                      subStringFromKeyIcap204, true);
      }

      if (status == 200) {
        // OK - The ICAP status is ok, but the encapsulated HTTP status might
        // likely be different or another key in ICAP status
        logger.debug("Almost final status is {}", status);
        boolean finalStatus = checkAgainstIcapHeader(finalResult, keyIcap200,
                                                     subStringFromKeyIcap200,
                                                     false);
        if (substringHttpStatus200 != null &&
            !substringHttpStatus200.isEmpty()) {
          parseMe = getHeaderHttp();
          logger.warn("{} contains {} = {}", parseMe, substringHttpStatus200,
                      parseMe.contains(substringHttpStatus200));
          finalStatus |= parseMe.contains(substringHttpStatus200);
        } else {
          if (logger.isTraceEnabled()) {
            getHeaderHttp();
          }
        }
        logger.debug("Final status with check {}", finalStatus);
        return finalStatus;
      }
    }
    logger.error("Unrecognized or no status code in response header");
    throw new IcapException("Unrecognized or no status code in response header",
                            IcapError.ICAP_SERVER_UNKNOWN_CODE);
  }

  /**
   * @param responseMap the header map
   * @param key the key to find out
   * @param subValue the sub value to find in the value associated with the key
   * @param defaultValue the default Value to return if key or subvalue are null
   *
   * @return True if the key exists and its value contains the subValue or
   *     default value if key or subValue are null
   */
  private boolean checkAgainstIcapHeader(final Map<String, String> responseMap,
                                         final String key,
                                         final String subValue,
                                         final boolean defaultValue) {
    if (key != null && subValue != null) {
      String value = responseMap.get(key);
      return value != null && value.contains(subValue);
    }
    return defaultValue;
  }

  /**
   * Send the next chunks for the file
   *
   * @param fileInStream the file inputStream to read from
   *
   * @throws IcapException if any error occurs (network, file reading,
   *     bad headers)
   */
  private void sendNextFileChunks(final InputStream fileInStream)
      throws IcapException {
    // Sending remaining part of file
    byte[] buffer = new byte[sendLength];
    int len = readChunk(fileInStream, buffer, sendLength);
    while (len != -1) {
      sendString(Integer.toHexString(len) + TERMINATOR);
      sendBytes(buffer, len);
      sendString(TERMINATOR);
      len = readChunk(fileInStream, buffer, sendLength);
    }
    // Ending file transfer
    sendString(HTTP_TERMINATOR, true);
    logger.debug("End of chunks");
  }

  /**
   * Read from inputChannel into the buffer the asked length at most
   *
   * @param fileInputStream the file inputStream to read from
   * @param buffer the buffer to write bytes read
   * @param length the maximum length to read
   *
   * @return -1 if no byte are available, else the size in bytes effectively
   *     read
   *
   * @throws IcapException if an error while reading the file occurs
   */
  int readChunk(final InputStream fileInputStream, final byte[] buffer,
                final int length) throws IcapException {
    if (buffer.length < length) {
      logger.error("Buffer is too small {} for reading file per {}",
                   buffer.length, length);
      throw new IcapException("Buffer is too small for reading file",
                              IcapError.ICAP_INTERNAL_ERROR);
    }
    int sizeOut = 0;
    int toRead = length;
    while (sizeOut < length) {
      try {
        final int read = fileInputStream.read(buffer, sizeOut, toRead);
        if (read <= 0) {
          break;
        }
        sizeOut += read;
        toRead -= read;
      } catch (final IOException e) {
        logger.error("File cannot be read: {}", e.getMessage());
        throw new IcapException("File cannot be read", e,
                                IcapError.ICAP_INTERNAL_ERROR);
      }
    }
    if (sizeOut <= 0) {
      return -1;
    }
    return sizeOut;
  }

  /**
   * @return the header for Http part of Icap
   *
   * @throws IcapException for network errors
   */
  String getHeaderHttp() throws IcapException {
    final byte[] buffer = new byte[receiveLength];
    try {
      return getHeader(HTTP_TERMINATOR, buffer);
    } catch (IcapException e) {
      final String finalHeaders =
          new String(buffer, 0, offset, WaarpStringUtils.UTF8);
      switch (e.getError()) {
        case ICAP_SERVER_HEADER_WITHOUT_TERMINATOR:
          // Returns the buffer as is
          logger.debug("RECV HTTP Headers not ended\n{}", finalHeaders);
          return finalHeaders;
        case ICAP_SERVER_HEADER_EXCEED_CAPACITY:
          // Returns the buffer as is
          logger.debug("RECV HTTP Headers exceed capacity\n{}", finalHeaders);
          return finalHeaders;
        default:
          break;
      }
      throw e;
    }
  }

  /**
   * @return the header for Icap
   *
   * @throws IcapException if the terminator is not found or the buffer is
   *     too small
   */
  String getHeaderIcap() throws IcapException {
    final byte[] buffer = new byte[receiveLength];
    return getHeader(ICAP_TERMINATOR, buffer);
  }

  /**
   * Receive an expected ICAP or HTTP header as response of a request. The
   * returned String should be parsed with parseHeader()
   *
   * @param terminator the terminator to use
   *
   * @return String of the raw response
   *
   * @throws IcapException if a network error is raised or if the header
   *     is wrong
   */
  private String getHeader(final String terminator, final byte[] buffer)
      throws IcapException {
    byte[] endOfHeader = terminator.getBytes(WaarpStringUtils.UTF8);
    int[] endOfHeaderInt = new int[endOfHeader.length];
    int[] marks = new int[endOfHeader.length];
    for (int i = 0; i < endOfHeader.length; i++) {
      endOfHeaderInt[i] = endOfHeader[i];
      marks[i] = -1;
    }

    int reader = -1;
    offset = 0;
    // "in" is read 1 by 1 to ensure we read only ICAP headers or HTTP headers
    try {
      // first part is to secure against DOS
      while ((offset < receiveLength) && ((reader = in.read()) != -1)) {
        marks[0] = marks[1];
        marks[1] = marks[2];
        marks[2] = marks[3];
        if (endOfHeader.length == 4) {
          marks[3] = reader;
        } else {
          marks[3] = marks[4];
          marks[4] = reader;
        }
        buffer[offset] = (byte) reader;
        offset++;
        // 13 is the smallest possible message "ICAP/1.0 xxx "
        if (offset > endOfHeader.length + 13 &&
            Arrays.equals(endOfHeaderInt, marks)) {
          final String finalHeaders =
              new String(buffer, 0, offset, WaarpStringUtils.UTF8);
          logger.debug("RECV {} Headers:{}\n{}",
                       terminator.length() == 4? "ICAP" : "HTTP", offset,
                       finalHeaders);
          return finalHeaders;
        }
      }
    } catch (SocketTimeoutException e) {
      logger.error(TIMEOUT_OCCURS_WITH_THE_SERVER_SINCE, serverIP, port,
                   e.getMessage());
      throw new IcapException(TIMEOUT_OCCURS_WITH_THE_SERVER, e,
                              IcapError.ICAP_TIMEOUT_ERROR);
    } catch (IOException e) {
      logger.error("Response cannot be read: {}", e.getMessage());
      throw new IcapException("Response cannot be read", e,
                              IcapError.ICAP_NETWORK_ERROR);
    }
    if (reader == -1) {
      logger.warn("Response is not complete while reading {}", offset);
      throw new IcapException(
          "Error in getHeader() method: response is not complete: " + offset,
          IcapError.ICAP_SERVER_HEADER_WITHOUT_TERMINATOR);
    }
    logger.warn("Response cannot be read since size exceed maximum {}",
                receiveLength);
    throw new IcapException(
        "Error in getHeader() method: received message too long",
        IcapError.ICAP_SERVER_HEADER_EXCEED_CAPACITY);
  }

  /**
   * Given a raw response header as a String, it will parse through it and return a HashMap of the result
   */
  private Map<String, String> parseHeader(final String response) {
    Map<String, String> headers = new HashMap<String, String>();

    /*
     * SAMPLE:
     * ICAP/1.0 204 Unmodified
     * Server: C-ICAP/0.1.6
     * Connection: keep-alive
     * ISTag: CI0001-000-0978-6918203
     */
    // The status code is located between the first 2 whitespaces.
    // Read status code
    int x = response.indexOf(' ');
    int y = response.indexOf(' ', x + 2);
    String statusCode = response.substring(x + 1, y);
    headers.put(STATUS_CODE, statusCode);

    // Each line in the sample is ended with "\r\n".
    // When (i+2==response.length()) The end of the header have been reached.
    // The +=2 is added to skip the "\r\n".
    // Read headers
    int i = response.indexOf(TERMINATOR, y);
    i += 2;
    while (i + 2 < response.length() && response.substring(i).contains(":")) {
      int n = response.indexOf(':', i);
      String key = response.substring(i, n).trim();

      n += 2;
      i = response.indexOf(TERMINATOR, n);
      String value = response.substring(n, i).trim();

      headers.put(key, value);
      i += 2;
    }
    logger.debug("RECV ICAP Headers:\n{}", headers);
    return headers;
  }

  /**
   * Sends a String through the socket connection. Used for sending ICAP/HTTP headers.
   *
   * @param requestHeader to send
   *
   * @throws IcapException if a network error is raised
   */
  private void sendString(final String requestHeader) throws IcapException {
    sendString(requestHeader, false);
  }

  /**
   * Sends a String through the socket connection. Used for sending ICAP/HTTP headers.
   *
   * @param requestHeader to send
   * @param withFlush if flush is necessary
   *
   * @throws IcapException if a network error is raised
   */
  private void sendString(final String requestHeader, final boolean withFlush)
      throws IcapException {
    try {
      out.write(requestHeader.getBytes(WaarpStringUtils.UTF8));
      if (withFlush) {
        out.flush();
      }
      logger.trace("SEND Request:\n{}", requestHeader);
    } catch (SocketTimeoutException e) {
      logger.error(TIMEOUT_OCCURS_WITH_THE_SERVER_SINCE, serverIP, port,
                   e.getMessage());
      throw new IcapException(TIMEOUT_OCCURS_WITH_THE_SERVER, e,
                              IcapError.ICAP_TIMEOUT_ERROR);
    } catch (IOException e) {
      logger.error("Client cannot communicate with ICAP Server: {}",
                   e.getMessage());
      throw new IcapException("Client cannot communicate with ICAP Server", e,
                              IcapError.ICAP_NETWORK_ERROR);
    }
  }

  /**
   * Sends bytes of data from a byte-array through the socket connection.
   *
   * @param chunk The byte-array to send
   *
   * @throws IcapException if a network error is raised
   */
  private void sendBytes(final byte[] chunk, final int length)
      throws IcapException {
    try {
      out.write(chunk, 0, length);
      logger.trace("SEND {} bytes", length);
    } catch (SocketTimeoutException e) {
      logger.error(TIMEOUT_OCCURS_WITH_THE_SERVER_SINCE, serverIP, port,
                   e.getMessage());
      throw new IcapException(TIMEOUT_OCCURS_WITH_THE_SERVER, e,
                              IcapError.ICAP_TIMEOUT_ERROR);
    } catch (IOException e) {
      logger.error("Client cannot communicate with ICAP Server: {}",
                   e.getMessage());
      throw new IcapException("Writing to ICAP Server cannot be done", e,
                              IcapError.ICAP_NETWORK_ERROR);
    }
  }

}
