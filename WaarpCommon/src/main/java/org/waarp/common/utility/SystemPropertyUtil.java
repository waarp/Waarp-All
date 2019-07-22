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

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Properties;

/**
 * A collection of utility methods to retrieve and parse the values of the Java system properties.
 */
public final class SystemPropertyUtil {
    public static final String FILE_ENCODING = "file.encoding";

    private static final Properties props = new Properties();

    // Retrieve all system properties at once so that there's no need to deal with
    // security exceptions from next time.  Otherwise, we might end up with logging every
    // security exceptions on every system property access or introducing more complexity
    // just because of less verbose logging.
    static {
        refresh();
    }

    /**
     * Re-retrieves all system properties so that any post-launch properties updates are retrieved.
     */
    public static void refresh() {
        Properties newProps = null;
        try {
            newProps = System.getProperties();
        } catch (SecurityException e) {
            System.err.println("Unable to retrieve the system properties; default values will be used: "
                    + e.getMessage());
            newProps = new Properties();
        }

        synchronized (props) {
            props.clear();
            props.putAll(newProps);
        }
        if (!contains(FILE_ENCODING) || !get(FILE_ENCODING).equalsIgnoreCase(WaarpStringUtils.UTF_8)) {
            try {
                //logger.info("Try to set UTF-8 as default file encoding: use -Dfile.encoding=UTF-8 as java command argument to ensure correctness");
                System.setProperty(FILE_ENCODING, WaarpStringUtils.UTF_8);
                Field charset = Charset.class.getDeclaredField("defaultCharset");
                charset.setAccessible(true);
                charset.set(null, null);
                synchronized (props) {
                    props.clear();
                    props.putAll(newProps);
                }
            } catch (Exception e1) {
                // ignore since it is a security issue and -Dfile.encoding=UTF-8 should be used
                System.err
                        .println("Issue while trying to set UTF-8 as default file encoding: use -Dfile.encoding=UTF-8 as java command argument: "
                                + e1.getMessage());
                System.err.println("Currently file.encoding is: " + get(FILE_ENCODING));
            }
        }
    }

    /**
     * 
     * @return True if Encoding is Correct
     */
    public static boolean isFileEncodingCorrect() {
        return (contains(FILE_ENCODING) && get(FILE_ENCODING).equalsIgnoreCase(WaarpStringUtils.UTF_8));
    }

    /**
     * Returns {@code true} if and only if the system property with the specified {@code key} exists.
     */
    public final static boolean contains(String key) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        return props.containsKey(key);
    }

    /**
     * Returns the value of the Java system property with the specified {@code key}, while falling back to {@code null} if the
     * property access fails.
     *
     * @return the property value or {@code null}
     */
    public final static String get(String key) {
        return get(key, null);
    }

    /**
     * Returns the value of the Java system property with the specified {@code key}, while falling back to the specified default
     * value if
     * the property access fails.
     *
     * @return the property value. {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public final static String get(String key, String def) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        String value = props.getProperty(key);
        if (value == null) {
            return def;
        }

        return value;
    }

    /**
     * Returns the value of the Java system property with the specified {@code key}, while falling back to the specified default
     * value if
     * the property access fails.
     *
     * @return the property value. {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static boolean getBoolean(String key, boolean def) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        String value = props.getProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.isEmpty()) {
            return true;
        }

        if (value.equals("true") || value.equals("yes") || value.equals("1")) {
            return true;
        }

        if (value.equals("false") || value.equals("no") || value.equals("0")) {
            return false;
        }

        System.err.println(
                "Unable to parse the boolean system property '" + key + "':" + value + " - " +
                        "using the default value: " + def);

        return def;
    }

    /**
     * Returns the value of the Java system property with the specified {@code key}, while falling back to the specified default
     * value if
     * the property access fails.
     *
     * @return the property value. {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static int getInt(String key, int def) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        String value = props.getProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.matches("-?[0-9]+")) {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                // Ignore
            }
        }

        System.err.println(
                "Unable to parse the integer system property '" + key + "':" + value + " - " +
                        "using the default value: " + def);

        return def;
    }

    /**
     * Returns the value of the Java system property with the specified {@code key}, while falling back to the specified default
     * value if
     * the property access fails.
     *
     * @return the property value. {@code def} if there's no such property or if an access to the
     *         specified property is not allowed.
     */
    public static long getLong(String key, long def) {
        if (key == null) {
            throw new NullPointerException("key");
        }

        String value = props.getProperty(key);
        if (value == null) {
            return def;
        }

        value = value.trim().toLowerCase();
        if (value.matches("-?[0-9]+")) {
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                // Ignore
            }
        }

        System.err.println(
                "Unable to parse the long integer system property '" + key + "':" + value + " - " +
                        "using the default value: " + def);

        return def;
    }

    public static void debug() {
        props.list(System.out);
    }

    private SystemPropertyUtil() {
        // Unused
    }
}
