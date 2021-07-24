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
package org.waarp.common.transcode;

import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

/**
 * Helper to print in output the Charsets available in the JVM.<br>
 * <br>
 * -html will output HTML format<br>
 * -text (default) will output TEXT format<br>
 * -csv will output CSV (comma separated) format<br>
 * <br>
 * Allow also to transcode one file to another: all arguments mandatory<br>
 * -from filename charset<br>
 * -to filename charset<br>
 */
public final class CharsetsUtil {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(CharsetsUtil.class);

  private CharsetsUtil() {
  }

  /**
   * @param args
   */
  public static void main(final String[] args) {
    int format = 1; // TEXT
    boolean transcode = false;
    String fromFilename = null;
    String fromCharset = null;
    String toFilename = null;
    String toCharset = null;
    if (args.length > 0) {
      for (int i = 0; i < args.length; i++) {
        if ("-html".equalsIgnoreCase(args[i])) {
          format = 0;
        } else if ("-text".equalsIgnoreCase(args[i])) {
          format = 1;
        } else if ("-csv".equalsIgnoreCase(args[i])) {
          format = 2;
        } else if ("-to".equalsIgnoreCase(args[i])) {
          i++;
          toFilename = args[i];
          i++;
          toCharset = args[i];
        } else if ("-from".equalsIgnoreCase(args[i])) {
          i++;
          fromFilename = args[i];
          i++;
          fromCharset = args[i];
        }
      }
      transcode =
          toCharset != null && toFilename != null && fromCharset != null &&
          fromFilename != null;
    }
    if (transcode) {
      final boolean status =
          transcode(fromFilename, fromCharset, toFilename, toCharset, 16384);
      SysErrLogger.FAKE_LOGGER.sysout("Transcode: " + status);
    } else {
      printOutCharsetsAvailable(format);
    }
  }

  /**
   * @param format 0 = html, 1 = text, 2 = csv
   */
  public static void printOutCharsetsAvailable(final int format) {
    final SortedMap<String, Charset> map = Charset.availableCharsets();
    final Set<Entry<String, Charset>> set = map.entrySet();
    switch (format) {
      case 0:
        SysErrLogger.FAKE_LOGGER.sysout(
            "<html><body><table border=1><tr><th>Name</th><th>CanEncode</th><th>IANA Registered</th><th>Aliases</th></tr>");
        break;
      case 1:
        SysErrLogger.FAKE_LOGGER.sysout(
            "Name\tCanEncode\tIANA Registered\tAliases");
        break;
      case 2:
      default:
        SysErrLogger.FAKE_LOGGER.sysout(
            "Name,CanEncode,IANA Registered,Aliases");
        break;
    }
    for (final Entry<String, Charset> entry : set) {
      final Charset charset = entry.getValue();
      final StringBuilder aliases;
      switch (format) {
        case 0:
          aliases = new StringBuilder("<ul>");
          break;
        case 1:
        case 2:
        default:
          aliases = new StringBuilder("[ ");
          break;
      }
      final Set<String> aliasCharset = charset.aliases();
      for (final String string : aliasCharset) {
        switch (format) {
          case 0:
            aliases.append("<li>").append(string).append("</li>");
            break;
          case 1:
          case 2:
          default:
            aliases.append(string).append(' ');
            break;
        }
      }
      switch (format) {
        case 0:
          aliases.append("</ul>");
          break;
        case 1:
        case 2:
        default:
          aliases.append(']');
          break;
      }
      switch (format) {
        case 0:
          SysErrLogger.FAKE_LOGGER.sysout(
              "<tr><td>" + entry.getKey() + "</td><td>" + charset.canEncode() +
              "</td><td>" + charset.isRegistered() + "</td><td>" + aliases +
              "</td>");
          break;
        case 1:
          SysErrLogger.FAKE_LOGGER.sysout(
              entry.getKey() + '\t' + charset.canEncode() + '\t' +
              charset.isRegistered() + '\t' + aliases);
          break;
        case 2:
        default:
          SysErrLogger.FAKE_LOGGER.sysout(
              entry.getKey() + ',' + charset.canEncode() + ',' +
              charset.isRegistered() + ',' + aliases);
          break;
      }
    }
    switch (format) {
      case 0:
        SysErrLogger.FAKE_LOGGER.sysout("</table></body></html>");
        break;
      case 1:
      case 2:
      default:
        break;
    }
  }

  /**
   * Method to transcode one file to another using 2 different charsets
   *
   * @param srcFilename
   * @param fromCharset
   * @param toFilename
   * @param toCharset
   * @param bufferSize
   *
   * @return True if OK, else False (will log the reason)
   */
  public static boolean transcode(final String srcFilename,
                                  final String fromCharset,
                                  final String toFilename,
                                  final String toCharset,
                                  final int bufferSize) {
    boolean success = false;
    final File from = new File(srcFilename);
    final File to = new File(toFilename);
    FileInputStream fileInputStream = null;
    InputStreamReader reader = null;
    FileOutputStream fileOutputStream = null;
    OutputStreamWriter writer = null;
    try {
      fileInputStream = new FileInputStream(from);
      reader = new InputStreamReader(fileInputStream, fromCharset);
      fileOutputStream = new FileOutputStream(to);
      writer = new OutputStreamWriter(fileOutputStream, toCharset);
      final char[] cbuf = new char[bufferSize];
      int read = reader.read(cbuf);
      while (read > 0) {
        writer.write(cbuf, 0, read);
        read = reader.read(cbuf);
      }
      success = true;
    } catch (final FileNotFoundException e) {
      logger.warn("File not found: {}", e.getMessage());
    } catch (final UnsupportedEncodingException e) {
      logger.warn("Unsupported Encoding: {}", e.getMessage());
    } catch (final IOException e) {
      logger.warn("File IOException: {}", e.getMessage());
    } finally {
      FileUtils.close(reader);
      FileUtils.close(fileInputStream);
      FileUtils.close(writer);
      FileUtils.close(fileOutputStream);
    }
    return success;
  }

}
