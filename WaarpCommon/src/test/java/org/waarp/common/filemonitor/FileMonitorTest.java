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
import org.waarp.common.filemonitor.FileMonitor.FileItem;
import org.waarp.common.filemonitor.FileMonitor.Status;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class FileMonitorTest {
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FileMonitorTest.class);
  private static final int LARGE_WAIT = 800;
  private static final int SMALL_WAIT = 100;
  private static final int FILE_SIZE = 100;
  private static final int MINIMAL_WAIT = 10;

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

  public void testFileMonitor(final boolean ignoreAlreadyUsed)
      throws Exception {
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
                        SMALL_WAIT, null, false, commandValidFile,
                        commandRemovedFile, commandCheckIteration);
    fileMonitor.setIgnoreAlreadyUsed(ignoreAlreadyUsed);
    commandValidFile.setMonitor(fileMonitor);
    fileMonitor.setCheckDelay(-1);

    fileMonitor.start();
    Thread.sleep(LARGE_WAIT);
    logger.warn("Create file: " + fileTest.getAbsolutePath());
    FileWriter fileWriterBig = new FileWriter(fileTest);
    for (int i = 0; i < FILE_SIZE; i++) {
      fileWriterBig.write("a");
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    while (countNew.get() == 0) {
      Thread.sleep(SMALL_WAIT);
    }

    logger.warn("Delete file: " + fileTest.getAbsolutePath());
    fileTest.delete();
    while (countDelete.get() == 0) {
      Thread.sleep(SMALL_WAIT);
    }

    logger.warn("Create new file: " + fileTest.getAbsolutePath());
    fileWriterBig = new FileWriter(fileTest);
    for (int i = 0; i < FILE_SIZE; i++) {
      fileWriterBig.write("a");
      fileWriterBig.flush();
      Thread.sleep(MINIMAL_WAIT);
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    while (countNew.get() == 1) {
      Thread.sleep(SMALL_WAIT);
    }

    logger.warn("Overwrite file: " + fileTest.getAbsolutePath());
    fileWriterBig = new FileWriter(fileTest);
    for (int i = 0; i < FILE_SIZE; i++) {
      fileWriterBig.write("a");
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    int created = countNew.get();
    if (ignoreAlreadyUsed) {
      Thread.sleep(LARGE_WAIT);
    } else {
      while (countNew.get() == 2) {
        Thread.sleep(SMALL_WAIT);
      }
      created = countNew.get();
      Thread.sleep(LARGE_WAIT);
    }

    logger.warn("Delete file: " + fileTest.getAbsolutePath());
    fileTest.delete();
    while (countDelete.get() == 1) {
      Thread.sleep(SMALL_WAIT);
    }

    logger.warn("Add Directory: " + directory2.getAbsolutePath());
    fileMonitor.addDirectory(directory2);
    Thread.sleep(MINIMAL_WAIT);
    logger.warn("Create file2: " + fileTest2.getAbsolutePath());
    fileWriterBig = new FileWriter(fileTest2);
    for (int i = 0; i < FILE_SIZE; i++) {
      fileWriterBig.write("a");
      fileWriterBig.flush();
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    while (countNew.get() == created) {
      Thread.sleep(SMALL_WAIT);
    }

    assertEquals(1, fileMonitor.getCurrentHistoryNb());

    fileTest2.delete();
    while (countDelete.get() == 2) {
      Thread.sleep(SMALL_WAIT);
    }

    assertEquals(0, fileMonitor.getCurrentHistoryNb());

    logger.warn("Remove Directory: " + directory2.getAbsolutePath());
    fileMonitor.removeDirectory(directory2);
    Thread.sleep(LARGE_WAIT);

    // Full status
    fileMonitor.setNextAsFullStatus();
    Thread.sleep(LARGE_WAIT);

    logger.warn("Create stopFile: " + stopFile.getAbsolutePath());
    fileWriterBig = new FileWriter(stopFile);
    fileWriterBig.write('a');
    fileWriterBig.flush();
    fileWriterBig.close();
    Thread.sleep(LARGE_WAIT);

    fileMonitor.waitForStopFile();
    assertTrue("Should be > 0", countCheck.get() > 0);
    assertTrue("Should be > 0", countDelete.get() > 0);
    assertTrue("Should be > 0", countNew.get() > 0);
    System.out.println(fileMonitor.getStatus());
    assertEquals(ignoreAlreadyUsed? 3 : 4, countNew.get());
  }

  @Test
  public void testFileMonitorRecheck() throws Exception {
    testFileMonitorRecheck(true);
  }

  @Test
  public void testFileMonitorRecheckAlreadyUsed() throws Exception {
    testFileMonitorRecheck(false);
  }

  public void testFileMonitorRecheck(final boolean ignoreAlreadyUsed)
      throws Exception {
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

    final AtomicBoolean checkOK = new AtomicBoolean(false);
    final AtomicInteger countNew = new AtomicInteger();
    final AtomicInteger countDelete = new AtomicInteger();
    final AtomicInteger countCheck = new AtomicInteger();
    final FileMonitorCommandRunnableFuture commandValidFile =
        new FileMonitorCommandRunnableFuture() {
          @Override
          protected boolean checkFileItemBusiness(final FileItem fileItem) {
            if (super.checkFileItemBusiness(fileItem)) {
              return true;
            }
            if (!ignoreAlreadyUsed && fileItem.used && checkOK.get()) {
              // Fake reused
              fileItem.used = false;
              fileItem.status = Status.VALID;
              return true;
            }
            return false;
          }

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
                        SMALL_WAIT, null, false, commandValidFile,
                        commandRemovedFile, commandCheckIteration);
    fileMonitor.setIgnoreAlreadyUsed(ignoreAlreadyUsed);
    fileMonitor.setCheckDelay(LARGE_WAIT);
    commandValidFile.setMonitor(fileMonitor);

    fileMonitor.start();
    Thread.sleep(LARGE_WAIT);
    logger.warn("Create file: " + fileTest.getAbsolutePath());
    FileWriter fileWriterBig = new FileWriter(fileTest);
    for (int i = 0; i < SMALL_WAIT; i++) {
      fileWriterBig.write("a");
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    while (countNew.get() == 0) {
      Thread.sleep(SMALL_WAIT);
    }

    logger.warn("Delete file: " + fileTest.getAbsolutePath());
    fileTest.delete();
    while (countDelete.get() == 0) {
      Thread.sleep(SMALL_WAIT);
    }

    logger.warn("Create new file: " + fileTest.getAbsolutePath());
    fileWriterBig = new FileWriter(fileTest);
    for (int i = 0; i < FILE_SIZE; i++) {
      fileWriterBig.write("a");
      fileWriterBig.flush();
      Thread.sleep(MINIMAL_WAIT);
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    while (countNew.get() == 1) {
      Thread.sleep(SMALL_WAIT);
    }

    logger.warn("Waiting long delay for check: " + fileTest.getAbsolutePath());
    // FIXME change false to true and wait for 300 more (400), and check redo
    //  is launched if ignore false
    Thread.sleep(LARGE_WAIT);
    logger.warn("Check is true: " + fileTest.getAbsolutePath());
    checkOK.set(true);
    int created = countNew.get();
    if (ignoreAlreadyUsed) {
      Thread.sleep(LARGE_WAIT);
    } else {
      while (countNew.get() == 2) {
        Thread.sleep(SMALL_WAIT);
      }
      created = countNew.get();
    }
    checkOK.set(false);
    logger.warn("Override file: " + fileTest.getAbsolutePath());

    fileWriterBig = new FileWriter(fileTest);
    for (int i = 0; i < FILE_SIZE; i++) {
      fileWriterBig.write("a");
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    if (ignoreAlreadyUsed) {
      Thread.sleep(LARGE_WAIT);
    } else {
      while (countNew.get() == created) {
        Thread.sleep(SMALL_WAIT);
      }
      created = countNew.get();
      Thread.sleep(LARGE_WAIT);
    }

    logger.warn("Delete file: " + fileTest.getAbsolutePath());
    fileTest.delete();
    while (countDelete.get() == 1) {
      Thread.sleep(SMALL_WAIT);
    }

    logger.warn("Add Directory: " + directory2.getAbsolutePath());
    fileMonitor.addDirectory(directory2);
    Thread.sleep(MINIMAL_WAIT);
    logger.warn("Create file2: " + fileTest2.getAbsolutePath());
    fileWriterBig = new FileWriter(fileTest2);
    for (int i = 0; i < FILE_SIZE; i++) {
      fileWriterBig.write("a");
      fileWriterBig.flush();
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    while (countNew.get() == created) {
      Thread.sleep(SMALL_WAIT);
    }

    assertEquals(1, fileMonitor.getCurrentHistoryNb());

    fileTest2.delete();
    while (countDelete.get() == 2) {
      Thread.sleep(SMALL_WAIT);
    }

    assertEquals(0, fileMonitor.getCurrentHistoryNb());

    logger.warn("Remove Directory: " + directory2.getAbsolutePath());
    fileMonitor.removeDirectory(directory2);
    Thread.sleep(LARGE_WAIT);

    // Full status
    fileMonitor.setNextAsFullStatus();
    Thread.sleep(LARGE_WAIT);

    logger.warn("Create stopFile: " + stopFile.getAbsolutePath());
    fileWriterBig = new FileWriter(stopFile);
    fileWriterBig.write('a');
    fileWriterBig.flush();
    fileWriterBig.close();
    Thread.sleep(LARGE_WAIT);

    fileMonitor.waitForStopFile();
    assertTrue("Should be > 0", countCheck.get() > 0);
    assertTrue("Should be > 0", countDelete.get() > 0);
    assertTrue("Should be > 0", countNew.get() > 0);
    System.out.println(fileMonitor.getStatus());
    assertEquals(ignoreAlreadyUsed? 3 : 5, countNew.get());
  }

}
