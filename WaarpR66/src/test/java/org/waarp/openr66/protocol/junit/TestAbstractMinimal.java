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

package org.waarp.openr66.protocol.junit;

import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import org.waarp.common.command.exception.Reply550Exception;
import org.waarp.common.file.FileUtils;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.logging.WaarpSlf4JLoggerFactory;
import org.waarp.common.utility.DetectionUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 *
 */
public abstract class TestAbstractMinimal {
  /**
   * Internal Logger
   */
  protected static WaarpLogger logger =
      WaarpLoggerFactory.getLogger(TestAbstractMinimal.class);
  protected static File dir;
  protected static File dirResources;

  /**
   * @throws Exception
   */
  public static void setUpBeforeClassMinimal(String serverInitBaseDirectory)
      throws Exception {
    WaarpLoggerFactory.setDefaultFactoryIfNotSame(
        new WaarpSlf4JLoggerFactory(WaarpLogLevel.WARN));
    ResourceLeakDetector.setLevel(Level.PARANOID);
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(TestAbstractMinimal.class);
    }
    final ClassLoader classLoader = TestAbstractMinimal.class.getClassLoader();
    DetectionUtils.setJunit(true);
    final File file;
    if (serverInitBaseDirectory.charAt(0) == '/') {
      file = new File(
          classLoader.getResource("Linux/config/config-XXDb.xml").getFile());
    } else {
      file =
          new File(classLoader.getResource(serverInitBaseDirectory).getFile());
    }
    if (file.exists()) {
      dir = new File(file.getParentFile().getAbsolutePath()
                         .replace("target/test-classes", "src/test/resources"));
      logger.warn(dir.getAbsolutePath());
      createBaseR66Directory(dir, "/tmp/R66");
    } else {
      System.err.println("Cannot find serverInitBaseDirectory file: " +
                         file.getAbsolutePath());
    }
  }

  public static void createBaseR66Directory(File dirConf, String path)
      throws Reply550Exception {
    final File tmp = new File(path);
    tmp.mkdirs();
    FileUtils.forceDeleteRecursiveDir(tmp);
    new File(tmp, "in").mkdir();
    new File(tmp, "out").mkdir();
    new File(tmp, "arch").mkdir();
    new File(tmp, "work").mkdir();
    File web = new File(tmp, "admin");
    web.mkdir();
    File webResp = new File(tmp, "adminresp");
    webResp.mkdir();
    final File conf = new File(tmp, "conf");
    conf.mkdir();
    logger.warn("Copy from {} to {}", dirConf, conf);
    final File[] copied = FileUtils.copyRecursive(dirConf, conf, false);
    for (final File fileCopied : copied) {
      System.out.print(fileCopied.getAbsolutePath() + ' ');
    }
    System.out.println(" Done");
    File baseWeb =
        dir.getParentFile().getParentFile().getParentFile().getParentFile();
    if (baseWeb.getAbsolutePath().contains("src/test/resources")) {
      baseWeb = baseWeb.getParentFile().getParentFile().getParentFile();
    }
    File webSrc = new File(baseWeb, "src/main/httpadmin/i18n");
    if (webSrc.isDirectory()) {
      logger.warn("Copy Web from {} to {}", webSrc, web);
      final File[] copiedWeb = FileUtils.copyRecursive(webSrc, web, false);
      for (final File fileCopied : copiedWeb) {
        System.out.print(fileCopied.getAbsolutePath() + ' ');
      }
      System.out.println(" Done");
    } else {
      logger.warn("Http dir does not exists: " + webSrc.getAbsolutePath());
    }
    File webRespSrc = new File(webSrc.getParentFile(), "admin-bootstrap");
    if (webRespSrc.isDirectory()) {
      logger.warn("CopyResp Web from {} to {}", webRespSrc, webResp);
      final File[] copiedWebResp =
          FileUtils.copyRecursive(webRespSrc, webResp, false);
      for (final File fileCopied : copiedWebResp) {
        System.out.print(fileCopied.getAbsolutePath() + ' ');
      }
      System.out.println(" Done");
    } else {
      logger.warn("Http dir does not exists: " + webRespSrc.getAbsolutePath());
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

  protected static File generateOutFile(String name, int size)
      throws IOException {
    return generateOutFile(name, size, "0123456789");
  }

  protected static File generateOutFile(String name, int size, String tenchars)
      throws IOException {
    final File file = new File(name);
    final FileWriter fileWriterBig = new FileWriter(file);
    for (int i = 0; i < size / 10; i++) {
      fileWriterBig.write(tenchars);
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    return file;
  }
}
