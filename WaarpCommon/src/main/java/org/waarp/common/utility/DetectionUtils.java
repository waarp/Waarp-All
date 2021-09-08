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

/**
 * Utility that detects various properties specific to the current runtime
 * environment, such as Java version.
 */
public final class DetectionUtils {
  private static final int JAVA_VERSION = javaVersion0();
  private static final boolean IS_WINDOWS;
  private static final boolean IS_UNIX_IBM;
  private static final int NUMBERTHREAD;

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

  private static boolean isAndroid0() {
    // Idea: Sometimes java binaries include Android classes on the classpath, even if it isn't actually Android.
    // Rather than check if certain classes are present, just check the VM, which is tied to the JDK.

    // Optional improvement: check if `android.os.Build.VERSION` is >= 24. On later versions of Android, the
    // OpenJDK is used, which means `Unsafe` will actually work as expected.

    // Android sets this property to Dalvik, regardless of whether it actually is.
    final String vmName = SystemPropertyUtil.get("java.vm.name");
    return "Dalvik".equals(vmName);
  }

  private static int javaVersion0() {
    final int majorVersion;

    if (isAndroid0()) {
      majorVersion = 6;
    } else {
      majorVersion = majorVersionFromJavaSpecificationVersion();
    }

    return majorVersion;
  }

  // Package-private for testing only
  static int majorVersionFromJavaSpecificationVersion() {
    return majorVersion(
        SystemPropertyUtil.get("java.specification.version", "1.6"));
  }

  // Package-private for testing only
  static int majorVersion(final String javaSpecVersion) {
    final String[] components = javaSpecVersion.split("\\.");
    final int[] version = new int[components.length];
    for (int i = 0; i < components.length; i++) {
      version[i] = Integer.parseInt(components[i]);
    }

    if (version[0] == 1) {
      assert version[1] >= 6;
      return version[1];
    } else {
      return version[0];
    }
  }
}
