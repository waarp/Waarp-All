/**
 * This file is part of Waarp Project.
 * 
 * Copyright 2009, Frederic Bregier, and individual contributors by the @author tags. See the
 * COPYRIGHT.txt in the distribution for a full listing of individual contributors.
 * 
 * All Waarp Project is free software: you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Waarp is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Waarp . If not, see
 * <http://www.gnu.org/licenses/>.
 */
package org.waarp.common.utility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;

import org.dom4j.Node;
import org.waarp.common.exception.FileTransferException;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

/**
 * Various utilities for reading files, transforming dates, ...
 * 
 * @author Frederic Bregier
 * 
 */
public class WaarpStringUtils {
    public static final String UTF_8 = "UTF-8";

    /**
     * Internal Logger
     */
    private static final WaarpLogger logger = WaarpLoggerFactory
            .getLogger(WaarpStringUtils.class);

    /**
     * Format used for Files
     */
    public static final Charset UTF8 = Charset.forName(UTF_8);

    /**
     * Read a file and return its content in String format
     * 
     * @param filename
     * @return the content of the File in String format
     * @throws InvalidArgumentException
     *             for File not found
     * @throws FileTransferException
     *             for reading exception
     */
    public static String readFileException(String filename) throws InvalidArgumentException,
            FileTransferException {
        File file = new File(filename);
        // Check for size of file
        if (file.length() > Integer.MAX_VALUE) {
            throw new FileTransferException("File is too big for this convenience method (" + file.length()
                    + " bytes).");
        }
        char[] chars = new char[(int) file.length()];
        FileReader fileReader;
        try {
            fileReader = new FileReader(file);
        } catch (FileNotFoundException e) {
            logger.error("File not found while trying to access: " + filename, e);
            throw new InvalidArgumentException("File not found while trying to access", e);
            // return null;
        }
        try {
            fileReader.read(chars);
        } catch (IOException e) {
            try {
                fileReader.close();
            } catch (IOException e1) {
            }
            logger.error("Error on File while trying to read: " + filename, e);
            throw new FileTransferException("Error on File while trying to read", e);
            // return null;
        }
        try {
            fileReader.close();
        } catch (IOException e) {
        }
        return new String(chars);
    }

    /**
     * Read file and return "" if an error occurs
     * 
     * @param filename
     * @return the string associated with the file, or "" if an error occurs
     */
    public static String readFile(String filename) {
        try {
            return readFileException(filename);
        } catch (InvalidArgumentException e) {
            logger.error("Error while trying to open: " + filename, e);
            return "";
        } catch (FileTransferException e) {
            logger.error("Error while trying to read: " + filename, e);
            return "";
        }
    }

    /**
     * Get a date in String and return the corresponding Timestamp
     * 
     * @param date
     * @return the corresponding Timestamp
     */
    public final static Timestamp fixDate(String date) {
        Timestamp tdate = null;
        if (date == null) {
            return tdate;
        }
        String ndate = date.replaceAll("/|:|\\.| |-", "");
        if (!ndate.isEmpty()) {
            if (ndate.length() < 15) {
                int len = ndate.length();
                ndate += "000000000000000".substring(len);
            }
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            try {
                Date ddate = format.parse(ndate);
                tdate = new Timestamp(ddate.getTime());
            } catch (ParseException e) {
                logger.debug("start", e);
            }
        }
        return tdate;
    }

    /**
     * From a date in String format and a Timestamp, return the Timestamp as :<br>
     * if before = null as date<br>
     * if before != null and before < date, as date<br>
     * if before != null and before >= date, as before end of day (23:59:59:9999)
     * 
     * @param date
     * @param before
     * @return the end date
     */
    public final static Timestamp fixDate(String date, Timestamp before) {
        Timestamp tdate = null;
        if (date == null) {
            return tdate;
        }
        String ndate = date.replaceAll("/|:|\\.| |-", "");
        if (!ndate.isEmpty()) {
            if (ndate.length() < 15) {
                int len = ndate.length();
                ndate += "000000000000000".substring(len);
            }
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            try {
                Date ddate = format.parse(ndate);
                if (before != null) {
                    Date bef = new Date(before.getTime());
                    if (bef.compareTo(ddate) >= 0) {
                        ddate = new Date(bef.getTime() + 1000 * 3600 * 24 - 1);
                    }
                }
                tdate = new Timestamp(ddate.getTime());
            } catch (ParseException e) {
                logger.debug("start", e);
            }
        }
        return tdate;
    }

    public final static Timestamp getTodayMidnight() {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.set(GregorianCalendar.HOUR_OF_DAY, 0);
        calendar.set(GregorianCalendar.MINUTE, 0);
        calendar.set(GregorianCalendar.SECOND, 0);
        calendar.set(GregorianCalendar.MILLISECOND, 0);
        return new Timestamp(calendar.getTimeInMillis());
    }

    /**
     * Read a boolean value (0,1,true,false) from a node
     * 
     * @param node
     * @return the corresponding value
     */
    public static boolean getBoolean(Node node) {
        String val = node.getText();
        boolean bval;
        try {
            int ival = Integer.parseInt(val);
            bval = (ival == 1) ? true : false;
        } catch (NumberFormatException e) {
            bval = Boolean.parseBoolean(val);
        }
        return bval;
    }

    /**
     * Read an integer value from a node
     * 
     * @param node
     * @return the corresponding value
     * @throws InvalidArgumentException
     */
    public static int getInteger(Node node) throws InvalidArgumentException {
        if (node == null)
            throw new InvalidArgumentException("Node empty");
        String val = node.getText();
        int ival;
        try {
            ival = Integer.parseInt(val);
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException("Incorrect value");
        }
        return ival;
    }

    /**
     * Make a replacement of first "find" string by "replace" string into the StringBuilder
     * 
     * @param builder
     * @param find
     * @param replace
     * @return True if one element is found
     */
    public final static boolean replace(StringBuilder builder, String find, String replace) {
        if (find == null) {
            return false;
        }
        int start = builder.indexOf(find);
        if (start == -1) {
            return false;
        }
        int end = start + find.length();
        if (replace != null) {
            builder.replace(start, end, replace);
        } else {
            builder.replace(start, end, "");
        }
        return true;
    }

    /**
     * Make replacement of all "find" string by "replace" string into the StringBuilder
     * 
     * @param builder
     * @param find
     * @param replace
     */
    public final static void replaceAll(StringBuilder builder, String find, String replace) {
        while (replace(builder, find, replace)) {
        }
    }

    /**
     * Build a String with count chars using fillChar
     * 
     * @param fillChar
     * @param count
     * @return the String of length count filled with fillChar
     */
    public final static String fillString(char fillChar, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, fillChar);
        return new String(chars);
    }

    /**
     * Clean the String that could contain '\' or '\n', '\r' into something compatible with HTML
     * @param json
     * @return the cleaned String
     */
    public final static String cleanJsonForHtml(String json) {
        return json.replaceAll("([^\\\\])\\\\n", "$1").replaceAll("([^\\\\])\\\\r", "$1")
                .replaceAll("([^\\\\])\\\\\"", "$1")
                .replace("\\\\", "\\\\\\\\");
    }
}
