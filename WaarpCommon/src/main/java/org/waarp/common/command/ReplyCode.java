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
package org.waarp.common.command;

import org.waarp.common.exception.InvalidArgumentException;

/**
 * Reply code references by different RFC.
 */
public enum ReplyCode {
  /**
   * unofficial reply code for no value
   */
  REPLY_000_SPECIAL_NOSTATUS(0),
  /**
   * 110 Restart marker reply. In this case, the text is exact and not left to
   * the particular implementation),
   * it must read: MARK yyyy (mmmm Where yyyy is User-process data stream
   * marker, and mmmm server's equivalent
   * marker (note the spaces between markers and "=").
   */
  REPLY_110_RESTART_MARKER_REPLY(110),

  /**
   * 120 Service ready in nnn minutes.
   */
  REPLY_120_SERVICE_READY_IN_NNN_MINUTES(120),

  /**
   * 125 Data connection already open), transfer starting.
   */
  REPLY_125_DATA_CONNECTION_ALREADY_OPEN(125),

  /**
   * 150 File status okay), about to open data connection.
   */
  REPLY_150_FILE_STATUS_OKAY(150),

  /**
   * 200 Command okay.
   */
  REPLY_200_COMMAND_OKAY(200),

  /**
   * 202 Command not implemented, superfluous at this site.
   */
  REPLY_202_COMMAND_NOT_IMPLEMENTED(202),

  /**
   * 211 System status, or system help reply.
   */
  REPLY_211_SYSTEM_STATUS_REPLY(211),

  /**
   * 212 Directory status.
   */
  REPLY_212_DIRECTORY_STATUS(212),

  /**
   * 213 File status.
   */
  REPLY_213_FILE_STATUS(213),

  /**
   * 214 Help message. On how to use the server or the meaning of a particular
   * non-standard command. This reply
   * is useful only to the human user.
   */
  REPLY_214_HELP_MESSAGE(214,
                         "This FTP server refers to RFC 959, RFC 775, RFC 2389 and RFC 3659"),

  /**
   * 215 NAME system type. Where NAME is an official system name from the list
   * in the Assigned Numbers document.
   */
  REPLY_215_NAME_SYSTEM_TYPE(215),

  /**
   * 220 Service ready for new user.
   */
  REPLY_220_SERVICE_READY(220),

  /**
   * Service closing control connection. Logged out if appropriate.
   */
  REPLY_221_CLOSING_CONTROL_CONNECTION(221),

  /**
   * 225 Data connection open), no transfer in progress.
   */
  REPLY_225_DATA_CONNECTION_OPEN_NO_TRANSFER_IN_PROGRESS(225),

  /**
   * Closing data connection. Requested file action successful (for example,
   * file transfer or file abort).
   */
  REPLY_226_CLOSING_DATA_CONNECTION(226),

  /**
   * 227 Entering Passive Mode (h1,h2,h3,h4,p1,p2).
   */
  REPLY_227_ENTERING_PASSIVE_MODE(227),

  /**
   * 229 Entering Extended Passive Mode (|n|addr|port|).
   */
  REPLY_229_ENTERING_PASSIVE_MODE(229),

  /**
   * 230 User logged in, proceed.
   */
  REPLY_230_USER_LOGGED_IN(230),

  /**
   * 232 User logged in, authorized by security data exchange.
   */
  REPLY_232_USER_LOGGED_IN(232),

  /**
   * 234 Security data exchange complete.
   */
  REPLY_234_SECURITY_DATA_EXCHANGE_COMPLETE(234),

  /**
   * 250 Requested file action okay, completed.
   */
  REPLY_250_REQUESTED_FILE_ACTION_OKAY(250),

  /**
   * 257 "PATHNAME" created.
   */
  REPLY_257_PATHNAME_CREATED(257),

  /**
   * 331 User name okay, need password.
   */
  REPLY_331_USER_NAME_OKAY_NEED_PASSWORD(331),

  /**
   * 332 Need account for login.
   */
  REPLY_332_NEED_ACCOUNT_FOR_LOGIN(332),

  /**
   * 350 Requested file action pending further information.
   */
  REPLY_350_REQUESTED_FILE_ACTION_PENDING_FURTHER_INFORMATION(350),

  /**
   * 421 Service not available, closing control connection. This may be a
   * reply
   * to any command if the service
   * knows it must shut down.
   */
  REPLY_421_SERVICE_NOT_AVAILABLE_CLOSING_CONTROL_CONNECTION(421),

  /**
   * 425 Can't open data connection.
   */
  REPLY_425_CANT_OPEN_DATA_CONNECTION(425),

  /**
   * 426 Connection closed), transfer aborted.
   */
  REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED(426),

  /**
   * 431 Need some unavailable resource to process security.
   */
  REPLY_431_NEED_UNAVAILABLE_RESOURCE_TO_PROCESS_SECURITY(431),

  /**
   * 450 Requested file action not taken. File unavailable (e.g., file busy).
   */
  REPLY_450_REQUESTED_FILE_ACTION_NOT_TAKEN(450),

  /**
   * 451 Requested action aborted: local error in processing.
   */
  REPLY_451_REQUESTED_ACTION_ABORTED(451),

  /**
   * 452 Requested action not taken. Insufficient storage space in system.
   */
  REPLY_452_REQUESTED_ACTION_NOT_TAKEN(452),

  /**
   * 500 Syntax error, command unrecognized. This may include errors such as
   * command line too long.
   */
  REPLY_500_SYNTAX_ERROR_COMMAND_UNRECOGNIZED(500),

  /**
   * 501 Syntax error in parameters or arguments.
   */
  REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS(501),

  /**
   * 502 Command not implemented.
   */
  REPLY_502_COMMAND_NOT_IMPLEMENTED(502),

  /**
   * 503 Bad sequence of commands.
   */
  REPLY_503_BAD_SEQUENCE_OF_COMMANDS(503),

  /**
   * 504 Command not implemented for that parameter.
   */
  REPLY_504_COMMAND_NOT_IMPLEMENTED_FOR_THAT_PARAMETER(504),

  /**
   * 522 Extended Port Failure - unknown network protocol.
   */
  REPLY_522_EXTENDED_PORT_FAILURE_UNKNOWN_NETWORK_PROTOCOL(522),

  /**
   * 530 Not logged in.
   */
  REPLY_530_NOT_LOGGED_IN(530),

  /**
   * 532 Need account for storing files.
   */
  REPLY_532_NEED_ACCOUNT_FOR_STORING_FILES(532),

  /**
   * 533 Command protection level denied for policy reasons.
   */
  REPLY_533_COMMAND_PROTECTION_LEVEL_DENIED_FOR_POLICY_REASONS(533),

  /**
   * 534 Request denied for policy reasons.
   */
  REPLY_534_REQUEST_DENIED_FOR_POLICY_REASONS(534),

  /**
   * 535 Failed security check (hash, sequence, etc).
   */
  REPLY_535_FAILED_SECURITY_CHECK(535),

  /**
   * 536 Requested PROT level not supported by mechanism.
   */
  REPLY_536_REQUESTED_PROT_LEVEL_NOT_SUPPORTED(536),

  /**
   * 550 Requested action not taken. File unavailable (e.g., file not found,
   * no
   * access).
   */
  REPLY_550_REQUESTED_ACTION_NOT_TAKEN(550),

  /**
   * 551 Requested action aborted: page type unknown.
   */
  REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN(551),

  /**
   * 552 Requested file action aborted. Exceeded storage allocation (for
   * current
   * directory or dataset).
   */
  REPLY_552_REQUESTED_FILE_ACTION_ABORTED_EXCEEDED_STORAGE(552),

  /**
   * 553 Requested action not taken. File name not allowed.
   */
  REPLY_553_REQUESTED_ACTION_NOT_TAKEN_FILE_NAME_NOT_ALLOWED(553);

  /**
   * Numerical code
   */
  private final int code;

  /**
   * Message associated
   */
  private final String mesg;

  /**
   * LF
   */
  public static final String LF = "\n";

  /**
   * CR NUL
   */
  public static final String CRNUL = "\r\0";

  /**
   * CR LF<br>
   * A User Telnet MUST be able to send any of the forms: CR LF, CR NUL, and
   * LF.
   * A User Telnet on an ASCII host
   * SHOULD have a user-controllable mode to send either CR LF or CR NUL when
   * the user presses the "end-of-line"
   * key, and CR LF SHOULD be the default.
   */
  public static final String CRLF = "\r\n";

  /**
   * Construct a Reply code from specific message
   *
   * @param code
   * @param mesg
   */
  ReplyCode(final int code, final String mesg) {
    this.code = code;
    this.mesg = getFinalMsg(code, mesg);
  }

  /**
   * Return the final message formatted as needed from the code and the
   * message
   *
   * @param code
   * @param msg
   *
   * @return the final formatted message
   */
  public static String getFinalMsg(final int code, final String msg) {
    final StringBuilder builder = new StringBuilder();
    builder.append(code);
    if (msg.indexOf('\n') == -1) {
      builder.append(' ').append(msg).append(CRLF);
    } else {
      final String[] lines = msg.split("\n");
      // first line
      builder.append('-').append(lines[0]).append(CRLF);
      // next lines
      for (int i = 1; i < lines.length - 1; i++) {
        final int firstBlank = lines[i].indexOf(' ');
        if (firstBlank > 0) {
          final String firstParam = lines[i].substring(0, firstBlank);
          boolean isInt = false;
          try {
            Integer.parseInt(firstParam);
            isInt = true;
          } catch (final NumberFormatException e) {
            // not a number
          }
          if (isInt) {
            builder.append("  ");
          }
        }
        builder.append(lines[i]).append(CRLF);
      }
      // last line
      builder.append(code).append(' ').append(lines[lines.length - 1])
             .append(CRLF);
    }
    return builder.toString();
  }

  /**
   * Construct a Reply Code from its name in Enum structure
   *
   * @param code
   */
  ReplyCode(final int code) {
    this.code = code;
    mesg = name().substring(6).replace('_', ' ') + CRLF;
  }

  /**
   * @return the code
   */
  public final int getCode() {
    return code;
  }

  /**
   * @return the mesg
   */
  public final String getMesg() {
    return mesg;
  }

  /**
   * @param code
   *
   * @return the associated ReplyCode from the given numerical code
   *
   * @throws InvalidArgumentException
   */
  public static ReplyCode getReplyCode(final int code)
      throws InvalidArgumentException {
    switch (code) {
      case 0:
        return REPLY_000_SPECIAL_NOSTATUS;
      case 110:
        return REPLY_110_RESTART_MARKER_REPLY;
      case 120:
        return REPLY_120_SERVICE_READY_IN_NNN_MINUTES;
      case 125:
        return REPLY_125_DATA_CONNECTION_ALREADY_OPEN;
      case 150:
        return REPLY_150_FILE_STATUS_OKAY;
      case 200:
        return REPLY_200_COMMAND_OKAY;
      case 202:
        return REPLY_202_COMMAND_NOT_IMPLEMENTED;
      case 211:
        return REPLY_211_SYSTEM_STATUS_REPLY;
      case 212:
        return REPLY_212_DIRECTORY_STATUS;
      case 213:
        return REPLY_213_FILE_STATUS;
      case 214:
        return REPLY_214_HELP_MESSAGE;
      case 215:
        return REPLY_215_NAME_SYSTEM_TYPE;
      case 220:
        return REPLY_220_SERVICE_READY;
      case 221:
        return REPLY_221_CLOSING_CONTROL_CONNECTION;
      case 225:
        return REPLY_225_DATA_CONNECTION_OPEN_NO_TRANSFER_IN_PROGRESS;
      case 226:
        return REPLY_226_CLOSING_DATA_CONNECTION;
      case 227:
        return REPLY_227_ENTERING_PASSIVE_MODE;
      case 229:
        return REPLY_229_ENTERING_PASSIVE_MODE;
      case 230:
        return REPLY_230_USER_LOGGED_IN;
      case 232:
        return REPLY_232_USER_LOGGED_IN;
      case 234:
        return REPLY_234_SECURITY_DATA_EXCHANGE_COMPLETE;
      case 250:
        return REPLY_250_REQUESTED_FILE_ACTION_OKAY;
      case 257:
        return REPLY_257_PATHNAME_CREATED;
      case 331:
        return REPLY_331_USER_NAME_OKAY_NEED_PASSWORD;
      case 332:
        return REPLY_332_NEED_ACCOUNT_FOR_LOGIN;
      case 350:
        return REPLY_350_REQUESTED_FILE_ACTION_PENDING_FURTHER_INFORMATION;
      case 421:
        return REPLY_421_SERVICE_NOT_AVAILABLE_CLOSING_CONTROL_CONNECTION;
      case 425:
        return REPLY_425_CANT_OPEN_DATA_CONNECTION;
      case 426:
        return REPLY_426_CONNECTION_CLOSED_TRANSFER_ABORTED;
      case 431:
        return REPLY_431_NEED_UNAVAILABLE_RESOURCE_TO_PROCESS_SECURITY;
      case 450:
        return REPLY_450_REQUESTED_FILE_ACTION_NOT_TAKEN;
      case 451:
        return REPLY_451_REQUESTED_ACTION_ABORTED;
      case 452:
        return REPLY_452_REQUESTED_ACTION_NOT_TAKEN;
      case 500:
        return REPLY_500_SYNTAX_ERROR_COMMAND_UNRECOGNIZED;
      case 501:
        return REPLY_501_SYNTAX_ERROR_IN_PARAMETERS_OR_ARGUMENTS;
      case 502:
        return REPLY_502_COMMAND_NOT_IMPLEMENTED;
      case 503:
        return REPLY_503_BAD_SEQUENCE_OF_COMMANDS;
      case 504:
        return REPLY_504_COMMAND_NOT_IMPLEMENTED_FOR_THAT_PARAMETER;
      case 522:
        return REPLY_522_EXTENDED_PORT_FAILURE_UNKNOWN_NETWORK_PROTOCOL;
      case 530:
        return REPLY_530_NOT_LOGGED_IN;
      case 532:
        return REPLY_532_NEED_ACCOUNT_FOR_STORING_FILES;
      case 533:
        return REPLY_533_COMMAND_PROTECTION_LEVEL_DENIED_FOR_POLICY_REASONS;
      case 534:
        return REPLY_534_REQUEST_DENIED_FOR_POLICY_REASONS;
      case 535:
        return REPLY_535_FAILED_SECURITY_CHECK;
      case 536:
        return REPLY_536_REQUESTED_PROT_LEVEL_NOT_SUPPORTED;
      case 550:
        return REPLY_550_REQUESTED_ACTION_NOT_TAKEN;
      case 551:
        return REPLY_551_REQUESTED_ACTION_ABORTED_PAGE_TYPE_UNKNOWN;
      case 552:
        return REPLY_552_REQUESTED_FILE_ACTION_ABORTED_EXCEEDED_STORAGE;
      case 553:
        return REPLY_553_REQUESTED_ACTION_NOT_TAKEN_FILE_NAME_NOT_ALLOWED;
      default:
        throw new InvalidArgumentException("Unknown ReplyCode " + code);
    }
  }
}
