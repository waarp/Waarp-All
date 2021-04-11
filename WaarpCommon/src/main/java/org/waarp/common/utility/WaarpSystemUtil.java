package org.waarp.common.utility;

import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;
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
    stopLogger();
    System.exit(value);//NOSONAR
  }

  /**
   * Stop logger
   */
  public static void stopLogger() {
    if (isJunit()) {
      return;
    }
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.NONE));
    if (WaarpLoggerFactory
            .getDefaultFactory() instanceof WaarpSlf4JLoggerFactory &&
        !isJunit()) {
      final LoggerContext lc =
          (LoggerContext) LoggerFactory.getILoggerFactory();
      lc.stop();
    }
  }

  /**
   * @param classz
   *
   * @return a new Instance for this Class
   *
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  public static Object newInstance(Class classz)
      throws NoSuchMethodException, InvocationTargetException,
             InstantiationException, IllegalAccessException {
    return classz.getDeclaredConstructor(EMPTY_CLASS_ARRAY).newInstance();
  }

  /**
   * @param className
   *
   * @return a new Instance for this Class
   *
   * @throws ClassNotFoundException
   * @throws NoSuchMethodException
   * @throws InvocationTargetException
   * @throws InstantiationException
   * @throws IllegalAccessException
   */
  public static Object newInstance(String className)
      throws ClassNotFoundException, NoSuchMethodException,
             InvocationTargetException, InstantiationException,
             IllegalAccessException {
    return newInstance(Class.forName(className));
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
