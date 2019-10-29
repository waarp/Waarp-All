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

package org.waarp.common.filemonitor;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.waarp.common.database.DbConstant;
import org.waarp.common.filemonitor.FileMonitor.FileItem;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class FileMonitorTest {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FileMonitorTest.class);

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    final File statusFile = new File("/tmp/status.txt");
    statusFile.delete();
    final File stopFile = new File("/tmp/stop.txt");
    stopFile.delete();
    final File directory = new File("/tmp/monitor");
    final File fileTest = new File(directory, "test.txt");
    directory.mkdirs();
    fileTest.delete();
    final File directory2 = new File("/tmp/monitor2");
    directory2.mkdirs();
    final File fileTest2 = new File(directory2, "test.txt");
    fileTest2.delete();
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
    final File statusFile = new File("/tmp/status.txt");
    statusFile.delete();
    final File stopFile = new File("/tmp/stop.txt");
    stopFile.delete();
    final File directory = new File("/tmp/monitor");
    final File fileTest = new File(directory, "test.txt");
    fileTest.delete();
    directory.delete();
    final File directory2 = new File("/tmp/monitor2");
    final File fileTest2 = new File(directory2, "test.txt");
    fileTest2.delete();
    directory2.delete();
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testFileMonitor() throws Exception {
    testFileMonitor(true);
  }

  @Test
  public void testFileMonitorAlreadyUsed() throws Exception {
    testFileMonitor(false);
  }

  public void testFileMonitor(final boolean ignoreAlreadyUsed) throws Exception {
    final File statusFile = new File("/tmp/status.txt");
    statusFile.delete();
    final File stopFile = new File("/tmp/stop.txt");
    stopFile.delete();
    final File directory = new File("/tmp/monitor");
    final File fileTest = new File(directory, "test.txt");
    directory.mkdirs();
    fileTest.delete();
    final File directory2 = new File("/tmp/monitor2");
    directory2.mkdirs();
    final File fileTest2 = new File(directory2, "test.txt");
    fileTest2.delete();

    final AtomicInteger countNew = new AtomicInteger();
    final AtomicInteger countDelete = new AtomicInteger();
    final AtomicInteger countCheck = new AtomicInteger();
    final FileMonitorCommandRunnableFuture commandValidFile =
        new FileMonitorCommandRunnableFuture() {
          @Override
          public void run(FileItem file) {
            setFileItem(file);
            checkReuse(ignoreAlreadyUsed);
            if (isIgnored(ignoreAlreadyUsed)) {
              logger.warn("RUN Ignore: " + file);
              return;
            } else if (isReuse()) {
              logger.warn("RUN on File REnew: " + file);
            } else {
              logger.warn("RUN on File New: " + file);
            }
            countNew.incrementAndGet();
            setFileItem(file);
            finalizeValidFile(true, 0);
            logger.warn("Final state: " + file);
          }
        };
    final FileMonitorCommandRunnableFuture commandRemovedFile =
        new FileMonitorCommandRunnableFuture() {
          @Override
          public void run(FileItem file) {
            logger.warn("File Del: " + file.file.getAbsolutePath());
            setFileItem(file);
            countDelete.incrementAndGet();
          }
        };
    final FileMonitorCommandRunnableFuture commandCheckIteration =
        new FileMonitorCommandRunnableFuture() {
          @Override
          public void run(FileItem unused) {
            logger.warn("Check done");
            countCheck.incrementAndGet();
          }
        };
    final FileMonitor fileMonitor =
        new FileMonitor("testDaemon", statusFile, stopFile, directory, null,
                        100, null, false, commandValidFile, commandRemovedFile,
                        commandCheckIteration);
    fileMonitor.setIgnoreAlreadyUsed(ignoreAlreadyUsed);
    commandValidFile.setMonitor(fileMonitor);

    fileMonitor.start();
    Thread.sleep(500);
    logger.warn("Create file: " + fileTest.getAbsolutePath());
    FileWriter fileWriterBig = new FileWriter(fileTest);
    for (int i = 0; i < 100; i++) {
      fileWriterBig.write("a");
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    Thread.sleep(1400);

    logger.warn("Delete file: " + fileTest.getAbsolutePath());
    fileTest.delete();
    Thread.sleep(300);

    logger.warn("Create new file: " + fileTest.getAbsolutePath());
    fileWriterBig = new FileWriter(fileTest);
    for (int i = 0; i < 100; i++) {
      fileWriterBig.write("a");
      fileWriterBig.flush();
      Thread.sleep(10);
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    Thread.sleep(1200);

    logger.warn("Overwrite file: " + fileTest.getAbsolutePath());
    fileWriterBig = new FileWriter(fileTest);
    for (int i = 0; i < 100; i++) {
      fileWriterBig.write("a");
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    Thread.sleep(900);

    logger.warn("Delete file: " + fileTest.getAbsolutePath());
    fileTest.delete();
    Thread.sleep(300);

    logger.warn("Add Directory: " + directory2.getAbsolutePath());
    fileMonitor.addDirectory(directory2);
    Thread.sleep(10);
    logger.warn("Create file2: " + fileTest2.getAbsolutePath());
    fileWriterBig = new FileWriter(fileTest2);
    for (int i = 0; i < 100; i++) {
      fileWriterBig.write("a");
      fileWriterBig.flush();
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    Thread.sleep(1200);

    assertEquals(1, fileMonitor.getCurrentHistoryNb());

    fileTest2.delete();
    Thread.sleep(1000);

    assertEquals(0, fileMonitor.getCurrentHistoryNb());

    logger.warn("Remove Directory: " + directory2.getAbsolutePath());
    fileMonitor.removeDirectory(directory2);
    Thread.sleep(500);

    // Full status
    fileMonitor.setNextAsFullStatus();
    Thread.sleep(500);

    logger.warn("Create stopFile: " + stopFile.getAbsolutePath());
    fileWriterBig = new FileWriter(stopFile);
    fileWriterBig.write('a');
    fileWriterBig.flush();
    fileWriterBig.close();
    Thread.sleep(500);

    fileMonitor.waitForStopFile();
    assertTrue("Should be > 0", countCheck.get() > 0);
    assertTrue("Should be > 0", countDelete.get() > 0);
    assertTrue("Should be > 0", countNew.get() > 0);
    System.out.println(fileMonitor.getStatus());
    assertEquals(ignoreAlreadyUsed? 3 : 4, countNew.get());
  }

}
