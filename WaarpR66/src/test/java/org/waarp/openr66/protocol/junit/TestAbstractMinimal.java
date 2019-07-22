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

/**
 *
 */
package org.waarp.openr66.protocol.junit;

import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;

import java.io.File;

/**
 *
 *
 */
public abstract class TestAbstractMinimal {
  /**
   * Internal Logger
   */
  protected static WaarpLogger logger;
  protected static File dir;

  /**
   * @throws java.lang.Exception
   */
  public static void setUpBeforeClassMinimal(String serverInit)
      throws Exception {
    WaarpLoggerFactory
        .setDefaultFactory(new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(TestAbstractMinimal.class);
    }
    final ClassLoader classLoader = TestAbstractMinimal.class.getClassLoader();
    DetectionUtils.setJunit(true);
    final File file = new File(classLoader.getResource(serverInit).getFile());
    if (file.exists()) {
      dir = file.getParentFile();
      final File tmp = new File("/tmp/R66");
      tmp.mkdirs();
      FileUtils.forceDeleteRecursiveDir(tmp);
      new File(tmp, "in").mkdir();
      new File(tmp, "out").mkdir();
      new File(tmp, "arch").mkdir();
      new File(tmp, "work").mkdir();
      final File conf = new File(tmp, "conf");
      conf.mkdir();
      final File[] copied = FileUtils.copyRecursive(dir, conf, false);
      for (final File fileCopied : copied) {
        System.out.print(fileCopied.getAbsolutePath() + " ");
      }
      System.out.println(" Done");
    } else {
      System.err
          .println("Cannot find serverInit file: " + file.getAbsolutePath());
    }
  }

  /**
   *
   */
  public static void tearDownAfterClassMinimal() {
    final File tmp = new File("/tmp/R66");
    tmp.mkdirs();
    FileUtils.forceDeleteRecursiveDir(tmp);
  }

}
