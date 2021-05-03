package org.waarp.common.utility;

import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;

import java.lang.reflect.InvocationTargetException;

public class WaarpSystemUtil {

  private static final Class[] EMPTY_CLASS_ARRAY = new Class[0];
  static boolean isJunit;

  private WaarpSystemUtil() {
    // Empty
  }

  /**
   * System.exit(value)
   *
   * @param value
   */
  public static void systemExit(int value) {
    if (isJunit()) {
      return;
    }
    stopLogger(false);
    System.exit(value);//NOSONAR
  }

  /**
   * Stop logger
   */
  public static void stopLogger(final boolean force) {
    if (!force && isJunit()) {
      return;
    }
    if (WaarpLoggerFactory.getLogLevel() == WaarpLogLevel.NONE) {
      return;
    }
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.NONE));
    WaarpLoggerFactory.getLogger("io.netty").setLevel(WaarpLogLevel.NONE);
    WaarpLoggerFactory.getLogger("io.netty.channel.AbstractChannel")
                      .setLevel(WaarpLogLevel.NONE);
    WaarpLoggerFactory
        .getLogger("io.netty.util.concurrent.AbstractEventExecutor")
        .setLevel(WaarpLogLevel.NONE);
  }

  /**
   * @param classz
   *
   * @return a new Instance for this Class
   *
   * @throws InvocationTargetException
   */
  public static Object newInstance(Class classz)
      throws InvocationTargetException {
    try {
      return classz.getDeclaredConstructor(EMPTY_CLASS_ARRAY).newInstance();
    } catch (Exception e) {
      throw new InvocationTargetException(e);
    }
  }

  /**
   * @param className
   *
   * @return a new Instance for this Class
   *
   * @throws InvocationTargetException
   */
  public static Object newInstance(String className)
      throws InvocationTargetException {
    try {
      return newInstance(Class.forName(className));
    } catch (Exception e) {
      if (e instanceof InvocationTargetException) {
        throw (InvocationTargetException) e;
      }
      throw new InvocationTargetException(e);
    }
  }

  /**
   * Replacement for Runtime.getRuntime().halt(value)
   *
   * @param value
   */
  public static void runtimeGetRuntimeHalt(final int value) {
    if (!isJunit()) {
      Runtime.getRuntime().halt(value);//NOSONAR
    }
  }

  /**
   * JUnit usage only
   *
   * @return True if in JUnit role
   */
  public static boolean isJunit() {
    return isJunit;
  }

  /**
   * JUnit usage only
   *
   * @param isJunit
   */
  public static void setJunit(final boolean isJunit) {
    WaarpSystemUtil.isJunit = isJunit;
  }
}
