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

import com.google.common.base.Strings;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.logging.SysErrLogger;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.Properties;

import static org.waarp.common.digest.WaarpBC.*;

/**
 * A collection of utility methods to retrieve and parse the values of the Java
 * system properties.
 */
public final class SystemPropertyUtil {
  // Since logger could be not available yet, one must not declare there a Logger

  private static final String USING_THE_DEFAULT_VALUE2 =
      "using the default value: ";
  private static final String FIND_0_9 = "-?[0-9]+";
  private static final String USING_THE_DEFAULT_VALUE =
      USING_THE_DEFAULT_VALUE2;
  public static final String FILE_ENCODING = "file.encoding";
  /**
   * Maximum Connections to the Database. Waarp will manage by default up
   * to 10 connections, except if this is set. The minimum value is 2.
   */
  public static final String WAARP_DATABASE_CONNECTION_MAX =
      "waarp.database.connection.max";
  private static final String IO_NETTY_ALLOCATOR_TYPE =
      "io.netty.allocator.type";
  private static final String IO_POOLED = "pooled";

  private static final Properties PROPS = new Properties();
  private static final String INVALID_PROPERTY = "Invalid property ";
  private static Platform mOs;

  // Retrieve all system properties at once so that there's no need to deal with
  // security exceptions from next time. Otherwise, we might end up with logging every
  // security exceptions on every system property access or introducing more complexity
  // just because of less verbose logging.
  static {
    initializedTlsContext();
    refresh();
  }

  /**
   * Re-retrieves all system properties so that any post-launch properties
   * updates are retrieved.
   */
  public static void refresh() {
    Properties newProps;
    try {
      newProps = System.getProperties();
    } catch (final SecurityException e) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      SysErrLogger.FAKE_LOGGER.syserr(
          "Unable to retrieve the system properties; default values will be used: " +
          e.getMessage());
      newProps = new Properties();
    }

    synchronized (PROPS) {
      PROPS.clear();
      PROPS.putAll(newProps);
    }
    if (!contains(IO_NETTY_ALLOCATOR_TYPE) ||
        Strings.isNullOrEmpty(get(IO_NETTY_ALLOCATOR_TYPE))) {
      try {
        System.setProperty(IO_NETTY_ALLOCATOR_TYPE, IO_POOLED);
        synchronized (PROPS) {
          PROPS.clear();
          PROPS.putAll(newProps);
        }
      } catch (final Throwable e1) {//NOSONAR
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
      }
    }
    if (!contains(FILE_ENCODING) ||
        !WaarpStringUtils.UTF_8.equalsIgnoreCase(get(FILE_ENCODING))) {
      try {
        System.setProperty(FILE_ENCODING, WaarpStringUtils.UTF_8);
        final Field charset = Charset.class.getDeclaredField("defaultCharset");
        charset.setAccessible(true); //NOSONAR
        charset.set(null, null); //NOSONAR
        synchronized (PROPS) {
          PROPS.clear();
          PROPS.putAll(newProps);
        }
      } catch (final Throwable e1) {//NOSONAR
        // ignore since it is a security issue and -Dfile.encoding=UTF-8 should be used
        SysErrLogger.FAKE_LOGGER.ignoreLog(e1);
        SysErrLogger.FAKE_LOGGER.syserr(
            "Issue while trying to set UTF-8 as default file encoding: use -Dfile.encoding=UTF-8 as java command argument: " +
            e1.getMessage());
        SysErrLogger.FAKE_LOGGER
            .syserr("Currently file.encoding is: " + get(FILE_ENCODING));
      }
    }
  }

  /**
   * @return True if Encoding is Correct
   */
  public static boolean isFileEncodingCorrect() {
    return contains(FILE_ENCODING) &&
           WaarpStringUtils.UTF_8.equalsIgnoreCase(get(FILE_ENCODING));
  }

  /**
   * Returns {@code true} if and only if the system property with the
   * specified
   * {@code key} exists.
   */
  public static boolean contains(String key) {
    ParametersChecker.checkParameter("Key", key);
    return PROPS.containsKey(key);
  }

  /**
   * Returns the value of the Java system property with the specified {@code
   * key}, while falling back to
   * {@code null} if the property access fails.
   *
   * @return the property value or {@code null}
   */
  public static String get(String key) {
    return get(key, null);
  }

  /**
   * Returns the value of the Java system property with the specified {@code
   * key}, while falling back to the specified
   * default value if the property access fails.
   *
   * @param key of system property
   * @param def the default value
   *
   * @return the property value. {@code def} if there's no such property or if
   *     an access to the specified property is
   *     not allowed.
   *
   * @throws IllegalArgumentException key null
   */
  public static String get(final String key, final String def) {
    ParametersChecker.checkParameter("Key", key);
    String value = PROPS.getProperty(key);
    if (value == null) {
      return def;
    }

    try {
      value = ParametersChecker.checkSanityString(value);
    } catch (InvalidArgumentException e) {
      SysErrLogger.FAKE_LOGGER.syserr(INVALID_PROPERTY + key, e);
      return def;
    }

    return value;
  }

  /**
   * Returns the value of the Java system property with the specified {@code
   * key}, while falling back to the specified
   * default value if the property access fails.
   *
   * @param key of system property
   * @param def the default value
   *
   * @return the property value. {@code def} if there's no such property or if
   *     an access to the specified property is
   *     not allowed.
   *
   * @throws IllegalArgumentException key null
   */
  public static boolean get(final String key, final boolean def) {
    ParametersChecker.checkParameter("Key", key);
    String value = PROPS.getProperty(key);
    if (value == null) {
      return def;
    }

    value = value.trim().toLowerCase();
    if (value.isEmpty()) {
      return true;
    }

    if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
      return true;
    }

    if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
      return false;
    }
    try {
      ParametersChecker.checkSanityString(value);
    } catch (InvalidArgumentException e) {
      SysErrLogger.FAKE_LOGGER.syserr(INVALID_PROPERTY + key, e);
      return def;
    }
    SysErrLogger.FAKE_LOGGER.syserr(
        "Unable to parse the boolean system property '" + key + "':" + value +
        " - " + USING_THE_DEFAULT_VALUE + def);

    return def;
  }

  /**
   * Returns the value of the Java system property with the specified {@code
   * key}, while falling back to the specified
   * default value if the property access fails.
   *
   * @param key the system property
   * @param def the default value
   *
   * @return the property value. {@code def} if there's no such property or if
   *     an access to the specified property is
   *     not allowed.
   *
   * @throws IllegalArgumentException key null
   */
  public static int get(final String key, final int def) {
    ParametersChecker.checkParameter("Key", key);
    String value = PROPS.getProperty(key);
    if (value == null) {
      return def;
    }

    value = value.trim().toLowerCase();
    if (value.matches(FIND_0_9)) {
      try {
        return Integer.parseInt(value);
      } catch (final Exception e) {
        // Since logger could be not available yet
        // Ignore
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      }
    }
    try {
      ParametersChecker.checkSanityString(value);
    } catch (InvalidArgumentException e) {
      SysErrLogger.FAKE_LOGGER.syserr(INVALID_PROPERTY + key, e);
      return def;
    }
    SysErrLogger.FAKE_LOGGER.syserr(
        "Unable to parse the integer system property '" + key + "':" + value +
        " - " + USING_THE_DEFAULT_VALUE + def);

    return def;
  }

  /**
   * Returns the value of the Java system property with the specified {@code
   * key}, while falling back to the specified
   * default value if the property access fails.
   *
   * @param key of system property
   * @param def the default value
   *
   * @return the property value. {@code def} if there's no such property or if
   *     an access to the specified property is
   *     not allowed.
   *
   * @throws IllegalArgumentException key null
   */
  public static long get(final String key, final long def) {
    ParametersChecker.checkParameter("Key", key);
    String value = PROPS.getProperty(key);
    if (value == null) {
      return def;
    }

    value = value.trim().toLowerCase();
    if (value.matches(FIND_0_9)) {
      try {
        return Long.parseLong(value);
      } catch (final Exception e) {
        // Since logger could be not available yet
        // Ignore
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      }
    }
    try {
      ParametersChecker.checkSanityString(value);
    } catch (InvalidArgumentException e) {
      SysErrLogger.FAKE_LOGGER.syserr(INVALID_PROPERTY + key, e);
      return def;
    }

    SysErrLogger.FAKE_LOGGER.syserr(
        "Unable to parse the long integer system property '" + key + "':" +
        value + " - " + USING_THE_DEFAULT_VALUE + def);

    return def;
  }

  /**
   * Returns the value of the Java system property with the specified {@code
   * key}, while falling back to the
   * specified default value if the property access fails.
   *
   * @return the property value. {@code def} if there's no such property or if
   *     an access to the specified
   *     property is not allowed.
   */
  public static boolean getBoolean(String key, boolean def) {
    ParametersChecker.checkParameter("Key", key);

    String value = PROPS.getProperty(key);
    if (value == null) {
      return def;
    }

    value = value.trim().toLowerCase();
    if (value.isEmpty()) {
      return true;
    }

    if ("true".equals(value) || "yes".equals(value) || "1".equals(value)) {
      return true;
    }

    if ("false".equals(value) || "no".equals(value) || "0".equals(value)) {
      return false;
    }

    SysErrLogger.FAKE_LOGGER.syserr(
        "Unable to parse the boolean system property '" + key + "':" + value +
        " - " + USING_THE_DEFAULT_VALUE2 + def);

    return def;
  }

  /**
   * Returns the value of the Java system property with the specified {@code
   * key}, while falling back to the
   * specified default value if the property access fails.
   *
   * @return the property value. {@code def} if there's no such property or if
   *     an access to the specified
   *     property is not allowed.
   */
  public static int getInt(String key, int def) {
    ParametersChecker.checkParameter("Key", key);

    String value = PROPS.getProperty(key);
    if (value == null) {
      return def;
    }

    value = value.trim().toLowerCase();
    if (value.matches(FIND_0_9)) {
      try {
        return Integer.parseInt(value);
      } catch (final Exception e) {
        // Ignore
      }
    }

    SysErrLogger.FAKE_LOGGER.syserr(
        "Unable to parse the integer system property '" + key + "':" + value +
        " - " + USING_THE_DEFAULT_VALUE2 + def);

    return def;
  }

  /**
   * Returns the value of the Java system property with the specified {@code
   * key}, while falling back to the
   * specified default value if the property access fails.
   *
   * @return the property value. {@code def} if there's no such property or if
   *     an access to the specified
   *     property is not allowed.
   */
  public static long getLong(String key, long def) {
    ParametersChecker.checkParameter("Key", key);

    String value = PROPS.getProperty(key);
    if (value == null) {
      return def;
    }

    value = value.trim().toLowerCase();
    if (value.matches(FIND_0_9)) {
      try {
        return Long.parseLong(value);
      } catch (final Exception e) {
        // Ignore
      }
    }

    SysErrLogger.FAKE_LOGGER.syserr(
        "Unable to parse the long integer system property '" + key + "':" +
        value + " - " + USING_THE_DEFAULT_VALUE2 + def);

    return def;
  }

  /**
   * Returns the value of the Java system property with the specified {@code
   * key}, while falling back to the specified
   * default value if the property access fails.
   *
   * @param key of system property
   * @param def the default value
   *
   * @return the property value. {@code def} if there's no such property or if
   *     an access to the specified property is
   *     not allowed.
   *
   * @throws IllegalArgumentException key or def null
   */
  public static String getAndSet(String key, String def) {
    ParametersChecker.checkParameter("Key", key);
    if (def == null) {
      throw new IllegalArgumentException("Def cannot be null");
    }
    if (!PROPS.containsKey(key)) {
      System.setProperty(key, def);
      refresh();
      return def;
    }
    return PROPS.getProperty(key);
  }

  /**
   * Returns the value of the Java system property with the specified {@code
   * key}, while falling back to the specified
   * default value if the property access fails.
   *
   * @param key of system property
   * @param def the default value
   *
   * @return the property value. {@code def} if there's no such property or if
   *     an access to the specified property is
   *     not allowed.
   *
   * @throws IllegalArgumentException key null
   */
  public static boolean getAndSet(String key, boolean def) {
    ParametersChecker.checkParameter("Key", key);
    if (!PROPS.containsKey(key)) {
      System.setProperty(key, Boolean.toString(def));
      refresh();
      return def;
    }
    return get(key, def);
  }

  /**
   * Returns the value of the Java system property with the specified {@code
   * key}, while falling back to the specified
   * default value if the property access fails.
   *
   * @param key of system property
   * @param def the default value
   *
   * @return the property value. {@code def} if there's no such property or if
   *     an access to the specified property is
   *     not allowed.
   *
   * @throws IllegalArgumentException key null
   */
  public static int getAndSet(String key, int def) {
    ParametersChecker.checkParameter("Key", key);
    if (!PROPS.containsKey(key)) {
      System.setProperty(key, Integer.toString(def));
      refresh();
      return def;
    }
    return get(key, def);
  }

  /**
   * Returns the value of the Java system property with the specified {@code
   * key}, while falling back to the specified
   * default value if the property access fails.
   *
   * @param key of system property
   * @param def the default value
   *
   * @return the property value. {@code def} if there's no such property or if
   *     an access to the specified property is
   *     not allowed.
   *
   * @throws IllegalArgumentException key null
   */
  public static long getAndSet(String key, long def) {
    ParametersChecker.checkParameter("Key", key);
    if (!PROPS.containsKey(key)) {
      System.setProperty(key, Long.toString(def));
      refresh();
      return def;
    }
    return get(key, def);
  }

  /**
   * Set the value of the Java system property with the specified {@code key}
   * to the specified default value.
   *
   * @param key of system property
   * @param def the default value
   *
   * @return the ancient value.
   *
   * @throws IllegalArgumentException key or def null
   */
  public static String set(String key, String def) {
    ParametersChecker.checkParameter("Key", key);
    if (def == null) {
      throw new IllegalArgumentException("Def cannot be null");
    }
    String old = null;
    if (PROPS.containsKey(key)) {
      old = PROPS.getProperty(key);
    }
    System.setProperty(key, def);
    refresh();
    return old;
  }

  /**
   * Set the value of the Java system property with the specified {@code key}
   * to the specified default value.
   *
   * @param key of system property
   * @param def the default value
   *
   * @return the ancient value.
   *
   * @throws IllegalArgumentException key null
   */
  public static boolean set(String key, boolean def) {
    ParametersChecker.checkParameter("Key", key);
    boolean old = false;
    if (PROPS.containsKey(key)) {
      old = get(key, def);
    }
    System.setProperty(key, Boolean.toString(def));
    refresh();
    return old;
  }

  /**
   * Set the value of the Java system property with the specified {@code key}
   * to the specified default value.
   *
   * @param key of system property
   * @param def the default value
   *
   * @return the ancient value.
   *
   * @throws IllegalArgumentException key null
   */
  public static int set(String key, int def) {
    ParametersChecker.checkParameter("Key", key);
    int old = 0;
    if (PROPS.containsKey(key)) {
      old = get(key, def);
    }
    System.setProperty(key, Integer.toString(def));
    refresh();
    return old;
  }

  /**
   * Set the value of the Java system property with the specified {@code key}
   * to the specified default value.
   *
   * @param key of system property
   * @param def the default value
   *
   * @return the ancient value.
   *
   * @throws IllegalArgumentException key null
   */
  public static long set(String key, long def) {
    ParametersChecker.checkParameter("Key", key);
    long old = 0;
    if (PROPS.containsKey(key)) {
      old = get(key, def);
    }
    System.setProperty(key, Long.toString(def));
    refresh();
    return old;
  }

  /**
   * Remove the key of the Java system property with the specified {@code
   * key}.
   *
   * @param key of system property
   *
   * @throws IllegalArgumentException key null
   */
  public static void clear(String key) {
    ParametersChecker.checkParameter("Key", key);
    PROPS.remove(key);
    System.clearProperty(key);
    refresh();
  }

  /**
   * Print to System.out the content of the properties
   *
   * @param out the output stream to be used
   *
   * @throws IllegalArgumentException out null
   */
  public static void debug(PrintStream out) {
    ParametersChecker.checkParameter("Out", out);
    PROPS.list(out);
  }

  /**
   * Inspired from http://commons.apache.org/lang/api-2.4/org/apache/commons/lang/
   * SystemUtils.html
   */
  public enum Platform {
    /**
     * Windows
     */
    WINDOWS,
    /**
     * Mac
     */
    MAC,
    /**
     * Unix
     */
    UNIX,
    /**
     * Solaris
     */
    SOLARIS,
    /**
     * Unsupported
     */
    UNSUPPORTED
  }

  /**
   * @return the Platform
   */
  public static Platform getOS() {
    if (mOs == null) {
      mOs = Platform.UNSUPPORTED;
      String os = "";
      try {
        os = System.getProperty("os.name").toLowerCase();
      } catch (final Exception e) {
        // ignore
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      }
      if (os.contains("win")) {
        mOs = Platform.WINDOWS;
        // Windows
      }
      if (os.contains("mac")) {
        mOs = Platform.MAC;
        // Mac
      }
      if (os.contains("nux")) {
        mOs = Platform.UNIX;
        // Linux
      }
      if (os.contains("nix")) {
        mOs = Platform.UNIX;
        // Unix
      }
      if (os.contains("sunos")) {
        mOs = Platform.SOLARIS;
        // Solaris
      }
    }
    return mOs;
  }

  /**
   * @return True if Windows
   */
  public static boolean isWindows() {
    return getOS() == Platform.WINDOWS;
  }

  /**
   * @return True if Mac
   */
  public static boolean isMac() {
    return getOS() == Platform.MAC;
  }

  /**
   * @return True if Unix
   */
  public static boolean isUnix() {
    return getOS() == Platform.UNIX;
  }

  /**
   * @return True if Solaris
   */
  public static boolean isSolaris() {
    return getOS() == Platform.SOLARIS;
  }

  public static void debug() {
    PROPS.list(System.out);//NOSONAR
  }

  private SystemPropertyUtil() {
    // Unused
  }
}
