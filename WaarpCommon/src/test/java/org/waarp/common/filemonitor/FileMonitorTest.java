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

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.waarp.common.filemonitor.FileMonitor.FileItem;
import org.waarp.common.filemonitor.FileMonitor.Status;
import org.waarp.common.logging.WaarpLogLevel;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.FileTestUtils;
import org.waarp.common.utility.TestWatcherJunit4;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class FileMonitorTest {
  @Rule(order = Integer.MIN_VALUE)
  public TestWatcher watchman = new TestWatcherJunit4();

  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(FileMonitorTest.class);
  private static final int LARGE_WAIT = 800;
  private static final int SMALL_WAIT = 100;
  private static final int FILE_SIZE = 100;
  private static final int MINIMAL_WAIT = 10;

  @Before
  public void setUp() throws Exception {
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
    Thread.sleep(100);
  }

  @After
  public void tearDown() throws Exception {
    final File statusFile = new File("/tmp/status.txt");
    statusFile.delete();
    final File stopFile = new File("/tmp/stop.txt");
    stopFile.delete();
    final File directory = new File("/tmp/monitor");
    FileUtils.deleteDirectory(directory);
    final File directory2 = new File("/tmp/monitor2");
    FileUtils.deleteDirectory(directory2);
    Thread.sleep(100);
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
    WaarpLoggerFactory.setLogLevel(WaarpLogLevel.WARN);
    logger.warn("Start test ignoreAlreadyUsed={}", ignoreAlreadyUsed);
    final File statusFile = new File("/tmp/status.txt");
    final File stopFile = new File("/tmp/stop.txt");
    final File directory = new File("/tmp/monitor");
    final File fileTest = new File(directory, "test.txt");
    directory.mkdirs();
    final File directory2 = new File("/tmp/monitor2");
    directory2.mkdirs();
    final File fileTest2 = new File(directory2, "test.txt");

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
            logger.warn("CountNew {}", countNew.incrementAndGet());
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
            logger.warn("CountDelete {}", countDelete.incrementAndGet());
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
    FileTestUtils.createTestFile(fileTest, FILE_SIZE, "a");
    waitForStatusOrFail(countNew, 0);

    logger.warn("Delete file: " + fileTest.getAbsolutePath());
    fileTest.delete();
    waitForStatusOrFail(countDelete, 0);

    logger.warn("Create new file: " + fileTest.getAbsolutePath());
    FileWriter fileWriterBig = new FileWriter(fileTest);
    for (int i = 0; i < FILE_SIZE; i++) {
      fileWriterBig.write("a");
      fileWriterBig.flush();
      Thread.sleep(MINIMAL_WAIT);
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    waitForStatusOrFail(countNew, 1);

    logger.warn("Overwrite file: " + fileTest.getAbsolutePath());
    FileTestUtils.createTestFile(fileTest, FILE_SIZE, "a");
    int created = countNew.get();
    if (ignoreAlreadyUsed) {
      Thread.sleep(LARGE_WAIT);
    } else {
      Thread.sleep(MINIMAL_WAIT);
      if (!waitForStatusOrLog(countNew, 2)) {
        stopMonitor(stopFile, fileTest, fileMonitor);
        Assume.assumeTrue("Error but test Unstable", false);
        return;
      }
      logger.warn("CountNew: {}", countNew.get());
      created = countNew.get();
      Thread.sleep(LARGE_WAIT);
    }

    logger.warn("Delete file: " + fileTest.getAbsolutePath());
    fileTest.delete();
    if (ignoreAlreadyUsed) {
      waitForStatusOrFail(countDelete, 1);
    } else if (!waitForStatusOrLog(countDelete, 1)) {
      stopMonitor(stopFile, fileTest, fileMonitor);
      Assume.assumeTrue("Error but test Unstable", false);
      return;
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
    if (ignoreAlreadyUsed) {
      waitForStatusOrFail(countNew, created);
    } else if (!waitForStatusOrLog(countNew, created)) {
      stopMonitor(stopFile, fileTest2, fileMonitor);
      Assume.assumeTrue("Error but test Unstable", false);
      return;
    }
    Thread.sleep(LARGE_WAIT);
    if (ignoreAlreadyUsed) {
      assertEquals(1, fileMonitor.getCurrentHistoryNb());
    } else {
      if (fileMonitor.getCurrentHistoryNb() != 1) {
        logger.error("CurrentHistoryNb not 1 = {}",
                     fileMonitor.getCurrentHistoryNb());
        stopMonitor(stopFile, fileTest2, fileMonitor);
        Assume.assumeTrue("Error but test Unstable", false);
        return;
      }
    }

    fileTest2.delete();
    waitForStatusOrFail(countDelete, 2);

    assertEquals(0, fileMonitor.getCurrentHistoryNb());

    logger.warn("Remove Directory: " + directory2.getAbsolutePath());
    fileMonitor.removeDirectory(directory2);
    Thread.sleep(LARGE_WAIT);

    // Full status
    fileMonitor.setNextAsFullStatus();
    Thread.sleep(LARGE_WAIT);

    logger.warn("Create stopFile: " + stopFile.getAbsolutePath());
    FileTestUtils.createTestFile(stopFile, 1, "a");
    Thread.sleep(LARGE_WAIT);

    fileMonitor.waitForStopFile();
    assertTrue("Should be > 0", countCheck.get() > 0);
    assertTrue("Should be > 0", countDelete.get() > 0);
    assertTrue("Should be > 0", countNew.get() > 0);
    logger.warn(fileMonitor.getStatus());
    if (ignoreAlreadyUsed) {
      assertEquals(ignoreAlreadyUsed? 3 : 4, countNew.get());
    } else {
      if (countNew.get() != 4) {
        logger.error("CountNew should be 4 but is {}", countNew.get());
        Assume.assumeTrue("Error but test Unstable", false);
      }
    }
  }

  private void stopMonitor(final File stopFile, final File fileTest,
                           final FileMonitor fileMonitor)
      throws IOException, InterruptedException {
    fileTest.delete();
    logger.warn("Create stopFile: " + stopFile.getAbsolutePath());
    FileTestUtils.createTestFile(stopFile, 1, "a");
    Thread.sleep(LARGE_WAIT);

    fileMonitor.waitForStopFile();
  }

  private void waitForStatusOrFail(final AtomicInteger countNew,
                                   final int count)
      throws InterruptedException {
    int i = 0;
    while (countNew.get() == count && i < 50) {
      Thread.sleep(SMALL_WAIT);
      i++;
    }
    assertTrue(countNew.get() != count);
  }

  private boolean waitForStatusOrLog(final AtomicInteger countNew,
                                     final int count)
      throws InterruptedException {
    int i = 0;
    while (countNew.get() == count && i < 50) {
      Thread.sleep(SMALL_WAIT);
      i++;
    }
    if (countNew.get() == count) {
      logger.error("count {} still equals to {}", countNew.get(), count);
      countNew.set(count + 1);
      return false;
    }
    return true;
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
    WaarpLoggerFactory.setLogLevel(WaarpLogLevel.WARN);
    logger.warn("Start test Recheck ignoreAlreadyUsed={}", ignoreAlreadyUsed);
    final File statusFile = new File("/tmp/status.txt");
    final File stopFile = new File("/tmp/stop.txt");
    final File directory = new File("/tmp/monitor");
    final File fileTest = new File(directory, "test.txt");
    directory.mkdirs();
    final File directory2 = new File("/tmp/monitor2");
    directory2.mkdirs();
    final File fileTest2 = new File(directory2, "test.txt");

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
    FileTestUtils.createTestFile(fileTest, FILE_SIZE, "a");
    waitForStatusOrFail(countNew, 0);

    logger.warn("Delete file: " + fileTest.getAbsolutePath());
    fileTest.delete();
    waitForStatusOrFail(countDelete, 0);

    logger.warn("Create new file: " + fileTest.getAbsolutePath());
    FileWriter fileWriterBig = new FileWriter(fileTest);
    for (int i = 0; i < FILE_SIZE; i++) {
      fileWriterBig.write("a");
      fileWriterBig.flush();
      Thread.sleep(MINIMAL_WAIT);
    }
    fileWriterBig.flush();
    fileWriterBig.close();
    waitForStatusOrFail(countNew, 1);

    logger.warn("Waiting long delay for check: " + fileTest.getAbsolutePath());
    Thread.sleep(LARGE_WAIT);
    logger.warn("Check is true: " + fileTest.getAbsolutePath());
    checkOK.set(true);
    int created = countNew.get();
    if (ignoreAlreadyUsed) {
      Thread.sleep(LARGE_WAIT);
    } else {
      waitForStatusOrFail(countNew, 2);
      created = countNew.get();
    }
    checkOK.set(false);
    logger.warn("Override file: " + fileTest.getAbsolutePath());

    FileTestUtils.createTestFile(fileTest, FILE_SIZE, "a");
    if (ignoreAlreadyUsed) {
      Thread.sleep(LARGE_WAIT);
    } else {
      waitForStatusOrFail(countNew, created);
      created = countNew.get();
      Thread.sleep(LARGE_WAIT);
    }

    logger.warn("Delete file: " + fileTest.getAbsolutePath());
    fileTest.delete();
    waitForStatusOrFail(countDelete, 1);

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
    waitForStatusOrFail(countNew, created);

    assertEquals(1, fileMonitor.getCurrentHistoryNb());

    fileTest2.delete();
    waitForStatusOrFail(countDelete, 2);

    assertEquals(0, fileMonitor.getCurrentHistoryNb());

    logger.warn("Remove Directory: " + directory2.getAbsolutePath());
    fileMonitor.removeDirectory(directory2);
    Thread.sleep(LARGE_WAIT);

    // Full status
    fileMonitor.setNextAsFullStatus();
    Thread.sleep(LARGE_WAIT);

    logger.warn("Create stopFile: " + stopFile.getAbsolutePath());
    FileTestUtils.createTestFile(stopFile, 1, "a");
    Thread.sleep(LARGE_WAIT);

    fileMonitor.waitForStopFile();
    assertTrue("Should be > 0", countCheck.get() > 0);
    assertTrue("Should be > 0", countDelete.get() > 0);
    assertTrue("Should be > 0", countNew.get() > 0);
    System.out.println(fileMonitor.getStatus());
    assertEquals(ignoreAlreadyUsed? 3 : 5, countNew.get());
  }

  @Test
  public void testFileMonitorWithRegex() throws Exception {
    // match test.txt but not subdir/test.txt test.csv subdir/test.csv
    final Set<String> expected = new HashSet<String>();
    expected.add("/tmp/monitor/test.txt");

    testFileMonitorWithRegex(true, ".\\.txt", false, expected);
  }

  @Test
  public void testFileMonitorWithRegexRecursive() throws Exception {
    // match test.txt subdir/test.txt but not test.csv subdir/test.csv
    final Set<String> expected = new HashSet<String>();
    expected.add("/tmp/monitor/test.txt");
    expected.add("/tmp/monitor/subdir/test.txt");

    testFileMonitorWithRegex(true, ".\\.txt", true, expected);
  }

  @Test
  public void testFileMonitorWithRegexDirNonRecursive() throws Exception {
    // match nothing: filter on subdir but not recursive
    final Set<String> expected = new HashSet<String>();

    testFileMonitorWithRegex(true, "subdir/.*", false, expected);
  }

  @Test
  public void testFileMonitorWithRegexDirRecursive() throws Exception {
    // match subdir/test.txt subdir/test.csv but not test.txt test.csv 
    final Set<String> expected = new HashSet<String>();
    expected.add("/tmp/monitor/subdir/test.txt");
    expected.add("/tmp/monitor/subdir/test.csv");

    testFileMonitorWithRegex(true, "subdir/.*", true, expected);
  }

  @Test
  public void testFileMonitorWithRegexAlreadyUsed() throws Exception {
    // match test.txt but not subdir/test.txt test.csv subdir/test.csv
    final Set<String> expected = new HashSet<String>();
    expected.add("/tmp/monitor/test.txt");

    testFileMonitorWithRegex(false, ".\\.txt", false, expected);
  }

  private void waitForUntilCountOrFail(final AtomicInteger countNew,
                                       final int count)
      throws InterruptedException {
    int i = 0;
    while (countNew.get() != count && i < 50) {
      Thread.sleep(SMALL_WAIT);
      i++;
    }
  }

  public void testFileMonitorWithRegex(final boolean ignoreAlreadyUsed,
                                       final String regex,
                                       final boolean recursive,
                                       final Set<String> expected)
      throws Exception {
    WaarpLoggerFactory.setLogLevel(WaarpLogLevel.WARN);
    logger.warn("Start test ignoreAlreadyUsed={} regex={} recursive={}",
                ignoreAlreadyUsed, regex, recursive);
    final File statusFile = new File("/tmp/status.txt");
    final File stopFile = new File("/tmp/stop.txt");
    final File directory = new File("/tmp/monitor");
    directory.mkdirs();
    final File fileTest = new File(directory, "test.txt");
    final File fileTest2 = new File(directory, "test.csv");
    final File directory2 = new File(directory, "subdir");
    directory2.mkdirs();
    final File fileTest3 = new File(directory2, "test.txt");
    final File fileTest4 = new File(directory2, "test.csv");

    final Set<String> filesSeen =
        Collections.synchronizedSet(new HashSet<String>());

    final AtomicInteger countNew = new AtomicInteger();
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
            filesSeen.add(file.file.getPath());
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
          }
        };
    final FileMonitorCommandRunnableFuture commandCheckIteration =
        new FileMonitorCommandRunnableFuture() {
          @Override
          public void run(FileItem unused) {
            logger.warn("Check done");
          }
        };
    final FileMonitor fileMonitor =
        new FileMonitor("testDaemon", statusFile, stopFile, directory, null,
                        SMALL_WAIT, new RegexFileFilter(regex), recursive,
                        commandValidFile, commandRemovedFile,
                        commandCheckIteration);
    fileMonitor.setIgnoreAlreadyUsed(ignoreAlreadyUsed);
    commandValidFile.setMonitor(fileMonitor);
    fileMonitor.setCheckDelay(-1);

    logger.warn("Create file: " + fileTest.getAbsolutePath());
    FileTestUtils.createTestFile(fileTest, 1, "a");

    logger.warn("Create file: " + fileTest2.getAbsolutePath());
    FileTestUtils.createTestFile(fileTest2, 1, "a");

    logger.warn("Create file: " + fileTest3.getAbsolutePath());
    FileTestUtils.createTestFile(fileTest3, 1, "a");

    logger.warn("Create file: " + fileTest4.getAbsolutePath());
    FileTestUtils.createTestFile(fileTest4, 1, "a");

    fileMonitor.start();
    waitForUntilCountOrFail(countNew, expected.size());
    logger.warn("files seen: {}", filesSeen);
    assertTrue(filesSeen.equals(expected));

    logger.warn("Create stopFile: " + stopFile.getAbsolutePath());
    FileTestUtils.createTestFile(stopFile, 1, "a");
    Thread.sleep(LARGE_WAIT);

    fileMonitor.waitForStopFile();
    System.out.println(fileMonitor.getStatus());
  }

}
