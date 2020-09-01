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

/**
 * Standard ICAP error code
 */
public enum IcapError {
  /**
   * Cannot connect to ICAP server
   */
  ICAP_CANT_CONNECT(1000, "Cannot connect to ICAP server"), // Not used
  /**
   * Not used: ICAP Server closed connection while reading response
   */
  ICAP_SERVER_RESPONSE_CLOSE(1001,
                             "ICAP Server closed connection while reading response"),
  /**
   * Not used: ICAP Server reset connection while reading response
   */
  ICAP_SERVER_RESPONSE_RESET(1002, "ICAP Server reset connection while " +
                                   "reading response"),
  /**
   * ICAP Server sent unknown response code
   */
  ICAP_SERVER_UNKNOWN_CODE(1003, "ICAP Server sent unknown response code"),
  /**
   * Not used: ICAP Server closed connection on 204 without 'Connection: close' header
   */
  ICAP_SERVER_UNEXPECTED_CLOSE_204(1004, "ICAP Server closed connection on " +
                                         "204 without 'Connection: close' header"),
  /**
   * Not used: ICAP Server closed connection as ICAP client wrote body preview
   */
  ICAP_SERVER_UNEXPECTED_CLOSE(1005, "ICAP Server closed connection as ICAP " +
                                     "client wrote body preview"),
  /**
   * ICAP Server response missed some information
   */
  ICAP_SERVER_MISSING_INFO(1006,
                           "ICAP Server response missed some information"),
  /**
   * Network error during communication with ICAP Server
   */
  ICAP_NETWORK_ERROR(1007,
                     "Network error during communication with ICAP Server"),
  /**
   * ICAP Server sends a header without terminator
   */
  ICAP_SERVER_HEADER_WITHOUT_TERMINATOR(1008, "ICAP Server sends a header " +
                                              "without terminator"),
  /**
   * ICAP Server sends a too big header
   */
  ICAP_SERVER_HEADER_EXCEED_CAPACITY(1009,
                                     "ICAP Server sends a too big header"),
  /**
   * Service is unknown by the ICAP Server
   */
  ICAP_SERVER_SERVICE_UNKNOWN(1010, "Service is unknown by the ICAP Server"),
  /**
   * ICAP Client has an internal error
   */
  ICAP_INTERNAL_ERROR(2000, "ICAP Client has an internal error"),
  /**
   * ICAP Client has wrong parameter
   */
  ICAP_ARGUMENT_ERROR(2001, "ICAP Client has wrong parameter"),
  /**
   * ICAP network operation has a timeout
   */
  ICAP_TIMEOUT_ERROR(2002, "ICAP network operation has a timeout"),
  /**
   * ICAP Client has a too big file
   */
  ICAP_FILE_LENGTH_ERROR(2003, "ICAP Client has a too big file");


  private final int code;
  private final String message;

  IcapError(final int code, final String message) {
    this.code = code;
    this.message = message;
  }

  public int getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
