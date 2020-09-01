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

import io.netty.util.internal.SystemPropertyUtil;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;

/**
 * Utility that detects various properties specific to the current runtime
 * environment, such as Java version.
 */
public final class DetectionUtils {
  private static final int JAVA_VERSION = javaVersion0();
  private static final boolean IS_WINDOWS;
  private static final boolean IS_UNIX_IBM;
  private static final int NUMBERTHREAD;
  private static boolean isJunit;

  static {
    final String os = SystemPropertyUtil.get("os.name").toLowerCase();
    // windows
    IS_WINDOWS = os.contains("win");
    if (!IS_WINDOWS) {
      String vendor = SystemPropertyUtil.get("java.vm.vendor");
      vendor = vendor.toLowerCase();
      IS_UNIX_IBM = vendor.contains("ibm");
    } else {
      IS_UNIX_IBM = false;
    }
    NUMBERTHREAD = Math.max(1, SystemPropertyUtil.getInt("org.waarp.numThreads",
                                                         Runtime.getRuntime()
                                                                .availableProcessors() *
                                                         2));
  }

  private DetectionUtils() {
  }

  /**
   * Replacement for System.exit(value)
   *
   * @param value
   */
  public static void systemExit(final int value) {
    if (!isJunit()) {
      System.exit(value);//NOSONAR
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
    DetectionUtils.isJunit = isJunit;
  }

  /**
   * @return the default number of threads (core * 2)
   */
  public static int numberThreads() {
    return NUMBERTHREAD;
  }

  /**
   * Return {@code true} if the JVM is running on Windows
   */
  public static boolean isWindows() {
    return IS_WINDOWS;
  }

  /**
   * Return {@code true} if the JVM is running on IBM UNIX JVM
   */
  public static boolean isUnixIBM() {
    return IS_UNIX_IBM;
  }

  public static int javaVersion() {
    return JAVA_VERSION;
  }

  private static int javaVersion0() {
    try {
      // Check if its android, if so handle it the same way as java6.
      //
      // See https://github.com/netty/netty/issues/282
      Class.forName("android.app.Application");
      return 6;
    } catch (final ClassNotFoundException e) {
      // Ignore
    }

    try {
      Class.forName("java.time.Clock", false, Object.class.getClassLoader());
      return 8;
    } catch (final Exception e) {
      // Ignore
    }

    try {
      Class.forName("java.util.concurrent.LinkedTransferQueue", false,
                    BlockingQueue.class.getClassLoader());
      return 7;
    } catch (final Exception e) {
      // Ignore
    }

    try {
      Class
          .forName("java.util.ArrayDeque", false, Queue.class.getClassLoader());
      return 6;
    } catch (final Exception e) {
      // Ignore
    }

    return 5;
  }
}
