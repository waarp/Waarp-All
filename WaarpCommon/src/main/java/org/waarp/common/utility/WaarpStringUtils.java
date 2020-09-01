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
package org.waarp.common.utility;

import com.google.common.io.Files;
import org.dom4j.Node;
import org.waarp.common.exception.FileTransferException;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Pattern;

/**
 * Various utilities for reading files, transforming dates, ...
 */
public final class WaarpStringUtils {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(WaarpStringUtils.class);

  public static final String UTF_8 = "UTF-8";
  /**
   * Format used for Files
   */
  public static final Charset UTF8 = Charset.forName(UTF_8);

  public static final String BLANK_REGEX = "\\s+";
  public static final Pattern BLANK = Pattern.compile(BLANK_REGEX);

  private WaarpStringUtils() {
  }

  /**
   * Read a file and return its content in String format
   *
   * @param filename
   *
   * @return the content of the File in String format
   *
   * @throws InvalidArgumentException for File not found
   * @throws FileTransferException for reading exception
   */
  public static String readFileException(final String filename)
      throws InvalidArgumentException, FileTransferException {
    final File file = new File(filename);
    // Check for size of file
    if (file.length() > Integer.MAX_VALUE) {
      throw new FileTransferException(
          "File is too big for this convenience method (" + file.length() +
          " bytes).");
    }
    try {
      return Files.toString(file, UTF8);
    } catch (final IOException e) {
      logger.error("Error on File while trying to read: " + filename, e);
      throw new FileTransferException("Error on File while trying to read", e);
    }
  }

  /**
   * Read file and return "" if an error occurs
   *
   * @param filename
   *
   * @return the string associated with the file, or "" if an error occurs
   */
  public static String readFile(final String filename) {
    try {
      return readFileException(filename);
    } catch (final InvalidArgumentException e) {
      logger.error("Error while trying to open: " + filename, e);
      return "";
    } catch (final FileTransferException e) {
      logger.error("Error while trying to read: " + filename, e);
      return "";
    }
  }

  /**
   * Get a date in String and return the corresponding Timestamp
   *
   * @param date
   *
   * @return the corresponding Timestamp
   */
  public static Timestamp fixDate(final String date) {
    Timestamp tdate = null;
    if (date == null) {
      return null;
    }
    String ndate = date.replaceAll("/|:|\\.|\\s|-", "");
    if (!ndate.isEmpty()) {
      if (ndate.length() < 15) {
        final int len = ndate.length();
        ndate += "000000000000000".substring(len);
      }
      final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
      try {
        final Date ddate = format.parse(ndate);
        tdate = new Timestamp(ddate.getTime());
      } catch (final ParseException e) {
        logger.debug("start", e);
      }
    }
    return tdate;
  }

  /**
   * From a date in String format and a Timestamp, return the Timestamp as
   * :<br>
   * if before = null as date<br>
   * if before != null and before < date, as date<br>
   * if before != null and before >= date, as before end of day
   * (23:59:59:9999)
   *
   * @param date
   * @param before
   *
   * @return the end date
   */
  public static Timestamp fixDate(final String date, final Timestamp before) {
    Timestamp tdate = null;
    if (date == null) {
      return null;
    }
    String ndate = date.replaceAll("/|:|\\.|\\s|-", "");
    if (!ndate.isEmpty()) {
      if (ndate.length() < 15) {
        final int len = ndate.length();
        ndate += "000000000000000".substring(len);
      }
      final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
      try {
        Date ddate = format.parse(ndate);
        if (before != null) {
          final Date bef = new Date(before.getTime());
          if (bef.compareTo(ddate) >= 0) {
            ddate = new Date(bef.getTime() + 1000 * 3600 * 24 - 1);
          }
        }
        tdate = new Timestamp(ddate.getTime());
      } catch (final ParseException e) {
        logger.debug("start", e);
      }
    }
    return tdate;
  }

  public static Timestamp getTodayMidnight() {
    final GregorianCalendar calendar = new GregorianCalendar();
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return new Timestamp(calendar.getTimeInMillis());
  }

  /**
   * Read a boolean value (0,1,true,false) from a node
   *
   * @param node
   *
   * @return the corresponding value
   */
  public static boolean getBoolean(final Node node) {
    final String val = node.getText();
    boolean bval;
    try {
      final int ival = Integer.parseInt(val);
      bval = ival == 1;
    } catch (final NumberFormatException e) {
      bval = Boolean.parseBoolean(val);
    }
    return bval;
  }

  /**
   * Read an integer value from a node
   *
   * @param node
   *
   * @return the corresponding value
   *
   * @throws InvalidArgumentException
   */
  public static int getInteger(final Node node)
      throws InvalidArgumentException {
    if (node == null) {
      throw new InvalidArgumentException("Node empty");
    }
    final String val = node.getText();
    final int ival;
    try {
      ival = Integer.parseInt(val);
    } catch (final NumberFormatException e) {
      throw new InvalidArgumentException("Incorrect value");
    }
    return ival;
  }

  /**
   * Make a replacement of first "find" string by "replace" string into the
   * StringBuilder
   *
   * @param builder
   * @param find
   * @param replace
   *
   * @return True if one element is found
   */
  public static boolean replace(final StringBuilder builder, final String find,
                                final String replace) {
    if (find == null) {
      return false;
    }
    final int start = builder.indexOf(find);
    if (start == -1) {
      return false;
    }
    final int end = start + find.length();
    if (replace != null) {
      builder.replace(start, end, replace);
    } else {
      builder.replace(start, end, "");
    }
    return true;
  }

  /**
   * Make replacement of all "find" string by "replace" string into the
   * StringBuilder
   *
   * @param builder
   * @param find
   * @param replace
   */
  public static void replaceAll(final StringBuilder builder, final String find,
                                final String replace) {
    while (replace(builder, find, replace)) {
      // nothing
    }
  }

  /**
   * Build a String with count chars using fillChar
   *
   * @param fillChar
   * @param count
   *
   * @return the String of length count filled with fillChar
   */
  public static String fillString(final char fillChar, final int count) {
    final char[] chars = new char[count];
    Arrays.fill(chars, fillChar);
    return new String(chars);
  }

  /**
   * Clean the String that could contain '\n' or '\r' into something
   * compatible with HTML
   *
   * @param json
   *
   * @return the cleaned String
   */
  public static String cleanJsonForHtml(final String json) {
    return json.replaceAll("([^\\\\])\\\\n", "$1")
               .replaceAll("([^\\\\])\\\\r", "$1");
  }
}
