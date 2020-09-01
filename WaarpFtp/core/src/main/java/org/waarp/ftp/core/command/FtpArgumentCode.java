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
package org.waarp.ftp.core.command;

import org.waarp.common.exception.InvalidArgumentException;

import java.nio.charset.Charset;

/**
 * Definition of all Argument of Parameter commands (MODE, STRU, TYPE)
 */
public class FtpArgumentCode {

  private FtpArgumentCode() {
  }

  /**
   * Type of transmission
   *
   * org.waarp.ftp.core.data TransferType
   */
  public enum TransferType {
    /**
     * Ascii TransferType
     */
    ASCII('A', "ASCII"),
    /**
     * Ebcdic TransferType
     */
    EBCDIC('E', "ebcdic-cp-us"), // could be ebcdic-cp-LG where LG is
    // language like fr, gb, ...
    /**
     * Image TransferType
     */
    IMAGE('I'),
    /**
     * Specific Length TransferType
     */
    LENGTH('L');
    /**
     * TransferType
     */
    public final char type;

    /**
     * Charset Name if any
     */
    public final Charset charset;

    TransferType(final char type) {
      this.type = type;
      charset = Charset.defaultCharset();
    }

    TransferType(final char type, final String charsetName) {
      this.type = type;
      charset = Charset.forName(charsetName);
    }
  }

  /**
   * SubType of transmission
   *
   * org.waarp.ftp.core.data TransferSubType
   */
  public enum TransferSubType {
    /**
     * Non-print TransferSubType
     */
    NONPRINT('N'),
    /**
     * Telnet format effectors TransferSubType
     */
    TELNET('T'),
    /**
     * Carriage Control ASA TransferSubType
     */
    CARRIAGE('C');
    /**
     * TransferSubType
     */
    public final char subtype;

    TransferSubType(final char subtype) {
      this.subtype = subtype;
    }
  }

  /**
   * Structure of transmission
   *
   * org.waarp.ftp.core.data TransferStructure
   */
  public enum TransferStructure {
    /**
     * FileInterface TransferStructure
     */
    FILE('F'),
    /**
     * Record TransferStructure
     */
    RECORD('R'),
    /**
     * Page TransferStructure
     */
    PAGE('P');
    /**
     * TransferStructure
     */
    public final char structure;

    TransferStructure(final char structure) {
      this.structure = structure;
    }
  }

  /**
   * Mode of transmission
   *
   * org.waarp.ftp.core.data TransferMode
   */
  public enum TransferMode {
    /**
     * Stream TransferMode
     */
    STREAM('S'),
    /**
     * Block TransferMode
     */
    BLOCK('B'),
    /**
     * Compressed TransferMode
     */
    COMPRESSED('C');
    /**
     * TransferMode
     */
    public final char mode;

    TransferMode(final char mode) {
      this.mode = mode;
    }
  }

  /**
   * Get the TransferType according to the char
   *
   * @param type
   *
   * @return the corresponding TransferType
   *
   * @throws InvalidArgumentException if the type is unknown
   */
  public static TransferType getTransferType(final char type)
      throws InvalidArgumentException {
    switch (type) {
      case 'A':
      case 'a':
        return TransferType.ASCII;
      case 'E':
      case 'e':
        return TransferType.EBCDIC;
      case 'I':
      case 'i':
        return TransferType.IMAGE;
      case 'L':
      case 'l':
        return TransferType.LENGTH;
      default:
        throw new InvalidArgumentException(
            "Argument for TransferType is not allowed: " + type);
    }
  }

  /**
   * Get the TransferSubType according to the char
   *
   * @param subType
   *
   * @return the corresponding TransferSubType
   *
   * @throws InvalidArgumentException if the TransferSubType is
   *     unknown
   */
  public static TransferSubType getTransferSubType(final char subType)
      throws InvalidArgumentException {
    switch (subType) {
      case 'C':
      case 'c':
        return TransferSubType.CARRIAGE;
      case 'N':
      case 'n':
        return TransferSubType.NONPRINT;
      case 'T':
      case 't':
        return TransferSubType.TELNET;
      default:
        throw new InvalidArgumentException(
            "Argument for TransferSubType is not allowed: " + subType);
    }
  }

  /**
   * Get the TransferStructure according to the char
   *
   * @param structure
   *
   * @return the corresponding TransferStructure
   *
   * @throws InvalidArgumentException if the TransferStructure is
   *     unknown
   */
  public static TransferStructure getTransferStructure(final char structure)
      throws InvalidArgumentException {
    switch (structure) {
      case 'P':
      case 'p':
        return TransferStructure.PAGE;
      case 'F':
      case 'f':
        return TransferStructure.FILE;
      case 'R':
      case 'r':
        return TransferStructure.RECORD;
      default:
        throw new InvalidArgumentException(
            "Argument for TransferStructure is not allowed: " + structure);
    }
  }

  /**
   * Get the TransferMode according to the char
   *
   * @param mode
   *
   * @return the corresponding TransferMode
   *
   * @throws InvalidArgumentException if the TransferMode is unknown
   */
  public static TransferMode getTransferMode(final char mode)
      throws InvalidArgumentException {
    switch (mode) {
      case 'B':
      case 'b':
        return TransferMode.BLOCK;
      case 'C':
      case 'c':
        return TransferMode.COMPRESSED;
      case 'S':
      case 's':
        return TransferMode.STREAM;
      default:
        throw new InvalidArgumentException(
            "Argument for TransferMode is not allowed: " + mode);
    }
  }
}
