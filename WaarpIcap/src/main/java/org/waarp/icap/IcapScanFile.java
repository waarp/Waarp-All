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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/**
 * IcapScanFile command to ask an ICAP server to scan a file
 * through network ICAP protocol.<br>
 * <br>
 * Options:<br>
 * -file path_to_file <br>
 * -to hostname <br>
 * [-port port, default 1344] <br>
 * -service name | -model name <br>
 * [-previewSize size, default none] <br>
 * [-blockSize size, default 8192] <br>
 * [-receiveSize size, default 65536] <br>
 * [-maxSize size, default MAX_INTEGER] <br>
 * [-timeout in_ms, default equiv to 10 min] <br>
 * [-errorMove path | -errorDelete | -sendOnError] <br>
 * [-ignoreNetworkError] <br>
 * [-ignoreTooBigFileError] <br>
 * [-keyPreview key -stringPreview string, default none] <br>
 * [-key204 key -string204 string, default none] <br>
 * [-key200 key -string200 string, default none] <br>
 * [-stringHttp string, default none] <br>
 * [-logger DEBUG|INFO|WARN|ERROR, default none] <br>
 * <br>
 * Exit with values:<br>
 * <ul>
 *   <li>0: Scan OK</li>
 *   <li>1: Bad arguments</li>
 *   <li>2: ICAP protocol error</li>
 *   <li>3: Network error</li>
 *   <li>4: Scan KO</li>
 *   <li>5: Scan KO but post action required in error</li>
 * </ul>
 */
public class IcapScanFile {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(IcapScanFile.class);

  public static final int STATUS_OK = 0;
  public static final int STATUS_BAD_ARGUMENT = 1;
  public static final int STATUS_ICAP_ISSUE = 2;
  public static final int STATUS_NETWORK_ISSUE = 3;
  public static final int STATUS_KO_SCAN = 4;
  public static final int STATUS_KO_SCAN_POST_ACTION_ERROR = 5;

  private static final String ARGUMENTS_CANNOT_BE_EMPTY_OR_NULL =
      "Arguments cannot be empty or null";

  private static final String FILE = "file";
  public static final String FILE_ARG = "-" + FILE;
  private static final Option FILE_OPTION =
      Option.builder(FILE).required(true).hasArg(true)
            .desc("Specify the file path to operate on").build();
  private static final String TO = "to";
  public static final String TO_ARG = "-" + TO;
  private static final Option HOST_OPTION =
      Option.builder(TO).required(true).hasArg(true)
            .desc("Specify the requested Host").build();
  private static final String SERVICE = "service";
  public static final String SERVICE_ARG = "-" + SERVICE;
  private static final Option SERVICE_OPTION =
      Option.builder(SERVICE).required(true).hasArg(true)
            .desc("Specify the service on remote host to use").build();
  private static final String MODEL = "model";
  public static final String MODEL_ARG = "-" + MODEL;
  private static final Option MODEL_OPTION =
      Option.builder(MODEL).required(true).hasArg(true)
            .desc("Specify the model of remote host service to use").build();
  private static final String PORT_FIELD = "port";
  private static final Option PORT_OPTION =
      Option.builder(PORT_FIELD).required(false).hasArg(true)
            .desc("Specify the port on remote host to use").type(Number.class)
            .build();
  private static final String PREVIEW_SIZE = "previewSize";
  private static final Option PREVIEW_OPTION =
      Option.builder(PREVIEW_SIZE).required(false).hasArg(true)
            .desc("Specify the Preview size to use").build();
  private static final String BLOCK_SIZE = "blockSize";
  private static final Option BLOCK_OPTION =
      Option.builder(BLOCK_SIZE).required(false).hasArg(true)
            .desc("Specify the Block size to use").build();
  private static final String RECEIVE_SIZE = "receiveSize";
  private static final Option RECEIVE_OPTION =
      Option.builder(RECEIVE_SIZE).required(false).hasArg(true)
            .desc("Specify the Receive size to use").build();
  private static final String MAX_SIZE = "maxSize";
  private static final Option MAX_SIZE_OPTION =
      Option.builder(MAX_SIZE).required(false).hasArg(true)
            .desc("Specify the Max size to use").build();
  private static final String TIMEOUT_ARG = "timeout";
  private static final Option TIMEOUT_OPTION =
      Option.builder(TIMEOUT_ARG).required(false).hasArg(true)
            .desc("Specify the timeout on socket to use").build();
  private static final String ERROR_MOVE = "errorMove";
  private static final Option ERROR_MOVE_OPTION =
      Option.builder(ERROR_MOVE).required(false).hasArg(true)
            .desc("Specify the path to use if wrong scan").build();
  private static final String ERROR_DELETE = "errorDelete";
  private static final Option ERROR_DELETE_OPTION =
      Option.builder(ERROR_DELETE).required(false).hasArg(false)
            .desc("Specify the error delete action if wrong scan").build();
  private static final String ERROR_SEND = "sendOnError";
  public static final String ERROR_SEND_ARG = "-" + ERROR_SEND;
  private static final Option ERROR_SEND_OPTION =
      Option.builder(ERROR_SEND).required(false).hasArg(false)
            .desc("Specify that scan error should be followed by an r66send")
            .build();
  private static final String IGNORE_NETWORK_CONTINUE = "ignoreNetworkError";
  private static final Option IGNORE_NETWORK_CONTINUE_OPTION =
      Option.builder(IGNORE_NETWORK_CONTINUE).required(false).hasArg(false)
            .desc("Specify that a network error should not be followed by a ko")
            .build();
  private static final String IGNORE_TOO_BIG_FILE_CONTINUE =
      "ignoreTooBigFileError";
  private static final Option IGNORE_TOO_BIG_FILE_CONTINUE_OPTION =
      Option.builder(IGNORE_TOO_BIG_FILE_CONTINUE).required(false).hasArg(false)
            .desc("Specify that a too big file should not be followed by a ko")
            .build();
  private static final String KEY_PREVIEW = "keyPreview";
  private static final Option PREVIEW_KEY_OPTION =
      Option.builder(KEY_PREVIEW).required(false).hasArg(true)
            .desc("Specify the key for Options to validate").build();
  private static final String STRING_PREVIEW = "stringPreview";
  private static final Option PREVIEW_STRING_OPTION =
      Option.builder(STRING_PREVIEW).required(false).hasArg(true)
            .desc("Specify the substring for key for Options to validate")
            .build();
  private static final String KEY_204 = "key204";
  private static final Option ICAP_204_KEY_OPTION =
      Option.builder(KEY_204).required(false).hasArg(true)
            .desc("Specify the key for 204 ICAP to validate").build();
  private static final String STRING_204 = "string204";
  private static final Option ICAP_204_STRING_OPTION =
      Option.builder(STRING_204).required(false).hasArg(true)
            .desc("Specify the substring for key for 204 ICAP to validate")
            .build();
  private static final String KEY_200 = "key200";
  private static final Option ICAP_200_KEY_OPTION =
      Option.builder(KEY_200).required(false).hasArg(true)
            .desc("Specify the key for 200 ICAP to validate").build();
  private static final String STRING_200 = "string200";
  private static final Option ICAP_200_STRING_OPTION =
      Option.builder(STRING_200).required(false).hasArg(true)
            .desc("Specify the substring for key for 200 ICAP to validate")
            .build();
  private static final String STRING_HTTP = "stringHttp";
  private static final Option HTTP_STRING_OPTION =
      Option.builder(STRING_HTTP).required(false).hasArg(true)
            .desc("Specify the substring for HTTP 200 ICAP status to validate")
            .build();
  private static final String LOGGER_ARG = "logger";
  private static final String DEBUG_LEVEL = "DEBUG";
  private static final String INFO_LEVEL = "INFO";
  private static final String WARN_LEVEL = "WARN";
  private static final String ERROR_LEVEL = "ERROR";
  private static final Option LOGGER_OPTION =
      Option.builder(LOGGER_ARG).required(false).hasArg(true).desc(
          "Specify the level of log between " + DEBUG_LEVEL + " | " +
          INFO_LEVEL + " | " + WARN_LEVEL + " | " + ERROR_LEVEL).build();

  private static final OptionGroup ERROR_OPTIONS =
      new OptionGroup().addOption(ERROR_DELETE_OPTION)
                       .addOption(ERROR_MOVE_OPTION)
                       .addOption(ERROR_SEND_OPTION);
  private static final OptionGroup SERVICE_OPTIONS =
      new OptionGroup().addOption(SERVICE_OPTION).addOption(MODEL_OPTION);
  private static final Options ICAP_OPTIONS =
      new Options().addOption(FILE_OPTION).addOption(HOST_OPTION)
                   .addOption(PORT_OPTION).addOptionGroup(SERVICE_OPTIONS)
                   .addOption(PREVIEW_OPTION).addOption(BLOCK_OPTION)
                   .addOption(RECEIVE_OPTION).addOption(MAX_SIZE_OPTION)
                   .addOption(TIMEOUT_OPTION)
                   .addOption(IGNORE_NETWORK_CONTINUE_OPTION)
                   .addOption(IGNORE_TOO_BIG_FILE_CONTINUE_OPTION)
                   .addOption(PREVIEW_KEY_OPTION)
                   .addOption(PREVIEW_STRING_OPTION)
                   .addOption(ICAP_200_KEY_OPTION)
                   .addOption(ICAP_200_STRING_OPTION)
                   .addOption(ICAP_204_KEY_OPTION)
                   .addOption(ICAP_204_STRING_OPTION).addOption(LOGGER_OPTION)
                   .addOption(HTTP_STRING_OPTION).addOptionGroup(ERROR_OPTIONS);
  private static final Options ICAP_MODEL_OPTIONS =
      new Options().addOption(PORT_OPTION).addOption(SERVICE_OPTION)
                   .addOption(PREVIEW_OPTION).addOption(BLOCK_OPTION)
                   .addOption(RECEIVE_OPTION).addOption(MAX_SIZE_OPTION)
                   .addOption(TIMEOUT_OPTION)
                   .addOption(IGNORE_NETWORK_CONTINUE_OPTION)
                   .addOption(IGNORE_TOO_BIG_FILE_CONTINUE_OPTION)
                   .addOption(PREVIEW_KEY_OPTION)
                   .addOption(PREVIEW_STRING_OPTION)
                   .addOption(ICAP_200_KEY_OPTION)
                   .addOption(ICAP_200_STRING_OPTION)
                   .addOption(ICAP_204_KEY_OPTION)
                   .addOption(ICAP_204_STRING_OPTION).addOption(LOGGER_OPTION)
                   .addOption(HTTP_STRING_OPTION).addOptionGroup(ERROR_OPTIONS);
  public static final String SEPARATOR_SEND = "--";

  // Standard configuration
  private String serverIP = null;
  private int port = IcapClient.DEFAULT_ICAP_PORT;
  private String icapService = null;
  private IcapModel icapModel = null;
  private String filepath = null;

  // Extra configuration
  private int receiveLength = IcapClient.STD_RECEIVE_LENGTH;
  private int sendLength = IcapClient.STD_SEND_LENGTH;
  private int timeout = IcapClient.DEFAULT_TIMEOUT;
  private String keyIcapPreview = null;
  private String subStringFromKeyIcapPreview = null;
  private String substringHttpStatus200 = null;
  private String keyIcap200 = null;
  private String subStringFromKeyIcap200 = null;
  private String keyIcap204 = null;
  private String subStringFromKeyIcap204 = null;
  private long maxSize = Integer.MAX_VALUE;
  private int stdPreviewSize = -1;
  private String pathMoveError = null;
  private boolean deleteOnError = false;
  private boolean sendOnError = false;
  private boolean ignoreNetworkError = false;
  private boolean ignoreTooBigFileError = false;
  private WaarpLogLevel logLevel = null;

  private Map<String, String> result = null;

  /**
   * Private constructor
   */
  private IcapScanFile() {
    // Empty
  }

  /**
   * Partial setter from source (not file, host, icapModel)
   *
   * @param from partial source
   */
  private IcapScanFile partialSetFrom(final IcapScanFile from) {
    this.port = from.port;
    this.icapService = from.icapService;
    this.receiveLength = from.receiveLength;
    this.sendLength = from.sendLength;
    this.timeout = from.timeout;
    this.keyIcapPreview = from.keyIcapPreview;
    this.subStringFromKeyIcapPreview = from.subStringFromKeyIcapPreview;
    this.substringHttpStatus200 = from.substringHttpStatus200;
    this.keyIcap200 = from.keyIcap200;
    this.subStringFromKeyIcap200 = from.subStringFromKeyIcap200;
    this.keyIcap204 = from.keyIcap204;
    this.subStringFromKeyIcap204 = from.subStringFromKeyIcap204;
    this.maxSize = from.maxSize;
    this.stdPreviewSize = from.stdPreviewSize;
    this.pathMoveError = from.pathMoveError;
    this.deleteOnError = from.deleteOnError;
    this.sendOnError = from.sendOnError;
    this.ignoreNetworkError = from.ignoreNetworkError;
    this.ignoreTooBigFileError = from.ignoreTooBigFileError;
    this.logLevel = from.getLogLevel();
    return this;
  }

  /**
   * Print to standard output the help of this command
   */
  public static void printHelp() {
    final HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("IcapScanFile", ICAP_OPTIONS);
  }

  /**
   * If file argument are -file EICARTEST, then a EICAR test file will be
   * sent.<br><br>
   * "-file path_to_file <br>
   * -to hostname <br>
   * [-port port, default 1344] <br>
   * -service name | -model name <br>
   * [-previewSize size, default none] <br>
   * [-blockSize size, default 8192] <br>
   * [-receiveSize size, default 65536] <br>
   * [-maxSize size, default MAX_INTEGER] <br>
   * [-timeout in_ms, default equiv to 10 min] <br>
   * [-errorMove path | -errorDelete | -sendOnError] <br>
   * [-ignoreNetworkError] <br>
   * [-ignoreTooBigFileError] <br>
   * [-keyPreview key -stringPreview string, default none] <br>
   * [-key204 key -string204 string, default none] <br>
   * [-key200 key -string200 string, default none] <br>
   * [-stringHttp string, default none] <br>
   * [-logger DEBUG|INFO|WARN|ERROR, default none]"<br>
   * <br>
   *
   * @param args must be already replaced values (getReplacedValue)
   *
   * @return the IcapScanFile
   *
   * @throws IcapException if an error occurs during argument parsing
   */
  public static IcapScanFile getIcapScanFileArgs(final String[] args)
      throws IcapException {
    if (args == null || args.length == 0) {
      throw new IllegalArgumentException(ARGUMENTS_CANNOT_BE_EMPTY_OR_NULL);
    }
    String[] realArgs = args;
    for (int i = 0; i < args.length; i++) {
      if (SEPARATOR_SEND.equals(args[i])) {
        realArgs = Arrays.copyOf(args, i);
        break;
      }
    }
    return getIcapScanFileArgs(realArgs, ICAP_OPTIONS);
  }

  /**
   * @param args must be already replaced values (getReplacedValue)
   * @param options the options matcher to use
   *
   * @return the IcapScanFile
   *
   * @throws IcapException if an error occurs during argument parsing
   */
  private static IcapScanFile getIcapScanFileArgs(final String[] args,
                                                  final Options options)
      throws IcapException {
    final IcapScanFile icapScanFile = new IcapScanFile();
    final CommandLineParser parser = new DefaultParser();
    try {
      final CommandLine cmd = parser.parse(options, args, true);

      if (options != ICAP_MODEL_OPTIONS && cmd.hasOption(MODEL)) {
        getModelParameters(icapScanFile, cmd);
      } else {
        icapScanFile.icapService = cmd.getOptionValue(SERVICE);
      }
      icapScanFile.filepath = cmd.getOptionValue(FILE);
      icapScanFile.serverIP = cmd.getOptionValue(TO);
      getPort(icapScanFile, cmd);
      getNumbers(icapScanFile, cmd);
      getOtherOptions(icapScanFile, cmd);
      if (cmd.hasOption(LOGGER_ARG)) {
        final String level =
            cmd.getOptionValue(LOGGER_ARG).trim().toUpperCase();
        if (DEBUG_LEVEL.equals(level)) {
          icapScanFile.logLevel = WaarpLogLevel.DEBUG;
        } else if (INFO_LEVEL.equals(level)) {
          icapScanFile.logLevel = WaarpLogLevel.INFO;
        } else if (WARN_LEVEL.equals(level)) {
          icapScanFile.logLevel = WaarpLogLevel.WARN;
        } else if (ERROR_LEVEL.equals(level)) {
          icapScanFile.logLevel = WaarpLogLevel.ERROR;
        } else {
          logger.warn("Unknown log level {}", level);
        }
      }
    } catch (final ParseException e) {
      throw new IcapException("Parsing error", e,
                              IcapError.ICAP_ARGUMENT_ERROR);
    }
    return icapScanFile;
  }

  /**
   * @param icapScanFile the original IcapScanFile
   * @param cmd the command to parse
   *
   * @throws IcapException if an error occurs during argument parsing
   */
  private static void getModelParameters(final IcapScanFile icapScanFile,
                                         final CommandLine cmd)
      throws IcapException {
    try {
      icapScanFile.icapModel = IcapModel.valueOf(cmd.getOptionValue(MODEL));
      final IcapScanFile modelIcapScanFile =
          getIcapScanFileArgs(icapScanFile.icapModel.getDefaultArgs(),
                              ICAP_MODEL_OPTIONS);
      icapScanFile.partialSetFrom(modelIcapScanFile);
    } catch (final IllegalArgumentException e) {
      throw new IcapException("Parsing error", e,
                              IcapError.ICAP_ARGUMENT_ERROR);
    }
  }

  /**
   * Get the other options from command
   *
   * @param icapScanFile the current IcapScanFile
   * @param cmd the command to parse from
   */
  private static void getOtherOptions(final IcapScanFile icapScanFile,
                                      final CommandLine cmd) {
    if (cmd.hasOption(ERROR_MOVE)) {
      icapScanFile.pathMoveError = cmd.getOptionValue(ERROR_MOVE);
    }
    if (cmd.hasOption(ERROR_DELETE)) {
      icapScanFile.deleteOnError = true;
    }
    if (cmd.hasOption(ERROR_SEND)) {
      icapScanFile.sendOnError = true;
    }
    if (cmd.hasOption(IGNORE_NETWORK_CONTINUE)) {
      icapScanFile.ignoreNetworkError = true;
    }
    if (cmd.hasOption(IGNORE_TOO_BIG_FILE_CONTINUE)) {
      icapScanFile.ignoreTooBigFileError = true;
    }
    if (cmd.hasOption(KEY_PREVIEW)) {
      icapScanFile.keyIcapPreview = cmd.getOptionValue(KEY_PREVIEW);
    }
    if (cmd.hasOption(STRING_PREVIEW)) {
      icapScanFile.subStringFromKeyIcapPreview =
          cmd.getOptionValue(STRING_PREVIEW);
    }
    if (cmd.hasOption(KEY_204)) {
      icapScanFile.keyIcap204 = cmd.getOptionValue(KEY_204);
    }
    if (cmd.hasOption(STRING_204)) {
      icapScanFile.subStringFromKeyIcap204 = cmd.getOptionValue(STRING_204);
    }
    if (cmd.hasOption(KEY_200)) {
      icapScanFile.keyIcap200 = cmd.getOptionValue(KEY_200);
    }
    if (cmd.hasOption(STRING_200)) {
      icapScanFile.subStringFromKeyIcap200 = cmd.getOptionValue(STRING_200);
    }
    if (cmd.hasOption(STRING_HTTP)) {
      icapScanFile.substringHttpStatus200 = cmd.getOptionValue(STRING_HTTP);
    }
  }

  /**
   * Check the numbers
   *
   * @param icapScanFile the IcapScanFile object
   * @param cmd the command line to check On
   *
   * @throws IcapException
   */
  private static void getNumbers(final IcapScanFile icapScanFile,
                                 final CommandLine cmd) throws IcapException {
    try {
      if (cmd.hasOption(PREVIEW_SIZE)) {
        icapScanFile.stdPreviewSize =
            Integer.parseInt(cmd.getOptionValue(PREVIEW_SIZE));
        if (icapScanFile.stdPreviewSize < 0) {
          throw new NumberFormatException("Preview size must be positive or 0");
        }
      }
      if (cmd.hasOption(BLOCK_SIZE)) {
        icapScanFile.sendLength =
            Integer.parseInt(cmd.getOptionValue(BLOCK_SIZE));
        if (icapScanFile.sendLength < IcapClient.MINIMAL_SIZE) {
          throw new NumberFormatException(
              "Block size must be greater than " + IcapClient.MINIMAL_SIZE);
        }
      }
      if (cmd.hasOption(RECEIVE_SIZE)) {
        icapScanFile.receiveLength =
            Integer.parseInt(cmd.getOptionValue(RECEIVE_SIZE));
        if (icapScanFile.receiveLength < IcapClient.MINIMAL_SIZE) {
          throw new NumberFormatException(
              "Receive size must be greater than " + IcapClient.MINIMAL_SIZE);
        }
      }
      if (cmd.hasOption(MAX_SIZE)) {
        icapScanFile.maxSize = Long.parseLong(cmd.getOptionValue(MAX_SIZE));
        if (icapScanFile.maxSize < IcapClient.MINIMAL_SIZE) {
          throw new NumberFormatException(
              "Max file size must be greater than " + IcapClient.MINIMAL_SIZE);
        }
      }
      if (cmd.hasOption(TIMEOUT_ARG)) {
        icapScanFile.timeout =
            Integer.parseInt(cmd.getOptionValue(TIMEOUT_ARG));
        if (icapScanFile.timeout < IcapClient.MINIMAL_SIZE) {
          throw new NumberFormatException(
              "Timeout must be greater than " + IcapClient.MINIMAL_SIZE);
        }
      }
    } catch (final NumberFormatException e) {
      throw new IcapException("Incorrect Number Format", e,
                              IcapError.ICAP_ARGUMENT_ERROR);
    }
  }

  /**
   * Get the port
   *
   * @param icapScanFile the IcapScanFile object
   * @param cmd the command line to check On
   *
   * @throws IcapException
   */
  private static void getPort(final IcapScanFile icapScanFile,
                              final CommandLine cmd) throws IcapException {
    try {
      if (cmd.hasOption(PORT_FIELD)) {
        icapScanFile.port = Integer.parseInt(cmd.getOptionValue(PORT_FIELD));
        if (icapScanFile.port < 0) {
          throw new NumberFormatException("Port must be positive");
        }
      }
    } catch (final NumberFormatException e) {
      throw new IcapException("Port incorrect", e,
                              IcapError.ICAP_ARGUMENT_ERROR);
    }
  }

  /**
   * Create the IcapClient according to IcapScanFile
   *
   * @param icapScanFile used to setup IcapClient
   *
   * @return the IcapClient
   */
  public static IcapClient getIcapClient(final IcapScanFile icapScanFile) {
    if (icapScanFile == null) {
      throw new IllegalArgumentException(ARGUMENTS_CANNOT_BE_EMPTY_OR_NULL);
    }
    final IcapClient icapClient =
        new IcapClient(icapScanFile.serverIP, icapScanFile.port,
                       icapScanFile.icapService, icapScanFile.stdPreviewSize);
    icapClient.setSendLength(icapScanFile.sendLength)
              .setReceiveLength(icapScanFile.receiveLength)
              .setMaxSize(icapScanFile.maxSize).setTimeout(icapScanFile.timeout)
              .setKeyIcapPreview(icapScanFile.keyIcapPreview)
              .setSubStringFromKeyIcapPreview(
                  icapScanFile.subStringFromKeyIcapPreview)
              .setKeyIcap204(icapScanFile.keyIcap204)
              .setSubStringFromKeyIcap204(icapScanFile.subStringFromKeyIcap204)
              .setKeyIcap200(icapScanFile.keyIcap200)
              .setSubStringFromKeyIcap200(icapScanFile.subStringFromKeyIcap200)
              .setSubstringHttpStatus200(icapScanFile.substringHttpStatus200);
    return icapClient;
  }

  /**
   * Finalize the current ICAP scan when an error occurs
   *
   * @param icapScanFile used to get options for Error tasks
   *
   * @throws IOException if an error occurs during post error tasks
   */
  public static void finalizeOnError(final IcapScanFile icapScanFile)
      throws IOException {
    if (icapScanFile == null) {
      throw new IllegalArgumentException(ARGUMENTS_CANNOT_BE_EMPTY_OR_NULL);
    }
    logger.error("Scan is incorrect: {}",
                 icapScanFile.getResult() == null? "No Result" :
                     icapScanFile.getResult());
    if (icapScanFile.deleteOnError) {
      final File file = new File(icapScanFile.filepath);
      if (!file.delete()) {
        logger.error("File cannot be deleted!");
        throw new IOException("File cannot be deleted!");
      } else {
        logger.warn("File is deleted");
      }
    } else if (icapScanFile.pathMoveError != null) {
      final File file = new File(icapScanFile.filepath);
      final File dir = new File(icapScanFile.pathMoveError);
      if (dir.exists()) {
        if (dir.isDirectory()) {
          try {
            final File to = new File(dir, file.getName());
            FileUtils.copy(file, to, true, false);
            logger.warn("File is moved to " + to.getAbsolutePath());
          } catch (final Reply550Exception e) {
            logger.error("Cannot move to directory: {}", e.getMessage());
            throw new IOException("Cannot move to directory", e);
          }
        } else {
          logger.error("Move path already exists and is not a directory");
          throw new IOException(
              "Move path already exists and is not a directory");
        }
      } else {
        if (dir.getParentFile().isDirectory()) {
          try {
            FileUtils.copy(file, dir, true, false);
            logger.warn("File is moved to " + dir.getAbsolutePath());
          } catch (final Reply550Exception e) {
            logger.error("Cannot move to file: {}", e.getMessage());
          }
        } else {
          logger.error("Move path is not a directory or existing sub-path");
          throw new IOException(
              "Move path is not a directory or existing sub-path");
        }
      }
    }
  }

  /**
   * @return the file path
   */
  public final String getFilePath() {
    return filepath;
  }

  /**
   * @param filePath the file path to use
   *
   * @return This
   */
  public final IcapScanFile setFilePath(final String filePath) {
    this.filepath = filePath;
    return this;
  }

  /**
   * @return the server IP
   */
  public final String getServerIP() {
    return serverIP;
  }

  /**
   * @param serverIP the server IP to use
   *
   * @return This
   */
  public final IcapScanFile setServerIP(final String serverIP) {
    this.serverIP = serverIP;
    return this;
  }

  /**
   * @return the Icap Model if any (null if none)
   */
  public final IcapModel getIcapModel() {
    return icapModel;
  }

  /**
   * @return the path to move in error or null
   */
  public final String getPathMoveError() {
    return pathMoveError;
  }

  /**
   * @return True if the file will be deleted in error
   */
  public final boolean isDeleteOnError() {
    return deleteOnError;
  }

  /**
   * @return True if the send on error option is set
   */
  public final boolean isSendOnError() {
    return sendOnError;
  }

  /**
   * @return True if a network error option is set to ignore such
   */
  public final boolean isIgnoreNetworkError() {
    return ignoreNetworkError;
  }

  /**
   * @return True if a too big file error option is set to ignore such
   */
  public final boolean isIgnoreTooBigFileError() {
    return ignoreTooBigFileError;
  }

  /**
   * @return the Logger Level desired during ICAP operation or null if none
   */
  public final WaarpLogLevel getLogLevel() {
    return logLevel;
  }

  /**
   * @return the Map of key from ICAP if any (null if none)
   */
  public final Map<String, String> getResult() {
    return result;
  }

  /**
   * If file argument are -file EICARTEST, then a EICAR test file will be
   * sent.<br><br>
   * "-file path_to_file <br>
   * -to hostname <br>
   * [-port port, default 1344] <br>
   * -service name | -model name <br>
   * [-previewSize size, default none] <br>
   * [-blockSize size, default 8192] <br>
   * [-receiveSize size, default 65536] <br>
   * [-maxSize size, default MAX_INTEGER] <br>
   * [-timeout in_ms, default equiv to 10 min] <br>
   * [-errorMove path | -errorDelete | -sendOnError] <br>
   * [-ignoreNetworkError] <br>
   * [-ignoreTooBigFileError] <br>
   * [-keyPreview key -stringPreview string, default none] <br>
   * [-key204 key -string204 string, default none] <br>
   * [-key200 key -string200 string, default none] <br>
   * [-stringHttp string, default none] <br>
   * [-logger DEBUG|INFO|WARN|ERROR, default none]"<br>
   * <br>
   * <br>
   * Exit with values:<br>
   * <ul>
   *   <li>0: Scan OK</li>
   *   <li>1: Bad arguments</li>
   *   <li>2: ICAP protocol error</li>
   *   <li>3: Network error</li>
   *   <li>4: Scan KO</li>
   *   <li>5: Scan KO but post action required in error</li>
   * </ul>
   *
   * @param args to get parameters from
   */
  public static void main(final String[] args) {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(null));
    System.exit(scanFile(args));
  }

  /**
   * If file argument are -file EICARTEST, then a EICAR test file will be
   * sent.<br><br>
   * "-file path_to_file <br>
   * -to hostname <br>
   * [-port port, default 1344] <br>
   * Not -service name | -model name (should be set through Model)<br>
   * [-previewSize size, default none] <br>
   * [-blockSize size, default 8192] <br>
   * [-receiveSize size, default 65536] <br>
   * [-maxSize size, default MAX_INTEGER] <br>
   * [-timeout in_ms, default equiv to 10 min] <br>
   * [-errorMove path | -errorDelete | -sendOnError] <br>
   * [-ignoreNetworkError] <br>
   * [-ignoreTooBigFileError] <br>
   * [-keyPreview key -stringPreview string, default none] <br>
   * [-key204 key -string204 string, default none] <br>
   * [-key200 key -string200 string, default none] <br>
   * [-stringHttp string, default none] <br>
   * [-logger DEBUG|INFO|WARN|ERROR, default none]"<br>
   * <br>
   *
   * @param model default model to apply in addition to parameters
   * @param args to get parameters from
   *
   * @return 0 if OK, 1 if arguments are incorrect, 2 if an issue occurs
   *     during ICAP protool, 3 if a nework error occurs, 4 if scan KO, 5 if
   *     the post actions are in error while scan is KO
   */
  public static int scanFile(final String[] model, final String[] args) {
    if (model == null || model.length == 0 || args == null ||
        args.length == 0) {
      throw new IllegalArgumentException(ARGUMENTS_CANNOT_BE_EMPTY_OR_NULL);
    }
    final String[] realArgs = Arrays.copyOf(model, model.length + args.length);
    System.arraycopy(args, 0, realArgs, model.length, args.length);
    return scanFile(realArgs);
  }

  /**
   * If file argument are -file EICARTEST, then a EICAR test file will be
   * sent.<br><br>
   * "-file path_to_file <br>
   * -to hostname <br>
   * [-port port, default 1344] <br>
   * -service name | -model name
   * [-previewSize size, default none] <br>
   * [-blockSize size, default 8192] <br>
   * [-receiveSize size, default 65536] <br>
   * [-maxSize size, default MAX_INTEGER] <br>
   * [-timeout in_ms, default equiv to 10 min] <br>
   * [-errorMove path | -errorDelete | -sendOnError] <br>
   * [-ignoreNetworkError] <br>
   * [-ignoreTooBigFileError] <br>
   * [-keyPreview key -stringPreview string, default none] <br>
   * [-key204 key -string204 string, default none] <br>
   * [-key200 key -string200 string, default none] <br>
   * [-stringHttp string, default none] <br>
   * [-logger DEBUG|INFO|WARN|ERROR, default none]"<br>
   * <br>
   *
   * @param args to get parameters from
   *
   * @return 0 if OK, 1 if arguments are incorrect, 2 if an issue occurs
   *     during ICAP protool, 3 if a nework error occurs, 4 if scan KO, 5 if
   *     the post actions are in error while scan is KO
   */
  public static int scanFile(final String[] args) {
    if (args == null || args.length == 0) {
      throw new IllegalArgumentException(ARGUMENTS_CANNOT_BE_EMPTY_OR_NULL);
    }
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(null));
    final IcapScanFile icapScanFile;
    try {
      icapScanFile = getIcapScanFileArgs(args);
    } catch (final IcapException e) {
      printHelp();
      logger.error("Arguments are incorrect: {}", e.getMessage());
      return STATUS_BAD_ARGUMENT;
    }
    try {
      return icapScanFile.scanFile();
    } catch (final IcapException e) {
      logger.error("Error during scan: {}", e.getMessage());
      if (e.getError() == IcapError.ICAP_CANT_CONNECT ||
          e.getError() == IcapError.ICAP_NETWORK_ERROR ||
          e.getError() == IcapError.ICAP_TIMEOUT_ERROR) {
        if (icapScanFile.ignoreNetworkError) {
          return STATUS_OK;
        }
        return STATUS_NETWORK_ISSUE;
      }
      if (e.getError() == IcapError.ICAP_ARGUMENT_ERROR ||
          e.getError() == IcapError.ICAP_FILE_LENGTH_ERROR) {
        if (icapScanFile.ignoreTooBigFileError &&
            e.getError() == IcapError.ICAP_FILE_LENGTH_ERROR) {
          return STATUS_OK;
        }
        return STATUS_BAD_ARGUMENT;
      }
      return STATUS_ICAP_ISSUE;
    } catch (final IOException e) {
      logger.error("Moving file is in error: {}", e.getMessage());
      return STATUS_KO_SCAN_POST_ACTION_ERROR;
    }
  }

  /**
   * Scan a file through ICAP antivirus server from IcapScanFile
   *
   * @return 0 if scan is OK, 1 if the file is infected
   *
   * @throws IcapException if an error occurs during connection or ICAP protocol
   * @throws IOException if the file is infected and a post error action is
   *     in error
   */
  public final int scanFile() throws IcapException, IOException {
    final WaarpLogLevel waarpLogLevel = getLogLevel();
    WaarpLogLevel oldLevel = null;
    try {
      if (waarpLogLevel != null) {
        if (logger.isTraceEnabled()) {
          oldLevel = WaarpLogLevel.TRACE;
        } else if (logger.isDebugEnabled()) {
          oldLevel = WaarpLogLevel.DEBUG;
        } else if (logger.isInfoEnabled()) {
          oldLevel = WaarpLogLevel.INFO;
        } else if (logger.isWarnEnabled()) {
          oldLevel = WaarpLogLevel.WARN;
        } else if (logger.isErrorEnabled()) {
          oldLevel = WaarpLogLevel.ERROR;
        }
        WaarpLoggerFactory.setLogLevel(waarpLogLevel);
      }
      final IcapClient icapClient = getIcapClient(this);
      if (icapClient.scanFile(filepath)) {
        icapClient.close();
        logger.info("File is OK");
        return STATUS_OK;
      } else {
        icapClient.close();
        result = icapClient.getFinalResult();
        finalizeOnError(this);
        return STATUS_KO_SCAN;
      }
    } finally {
      if (waarpLogLevel != null && oldLevel != null) {
        WaarpLoggerFactory.setLogLevel(oldLevel);
      }
    }
  }
}
