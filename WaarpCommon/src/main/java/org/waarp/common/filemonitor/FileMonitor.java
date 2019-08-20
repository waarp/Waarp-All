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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.waarp.common.database.DbConstant;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.common.digest.FilesystemBasedDigest.DigestAlgo;
import org.waarp.common.file.AbstractDir;
import org.waarp.common.future.WaarpFuture;
import org.waarp.common.json.JsonHandler;
import org.waarp.common.logging.SysErrLogger;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.common.utility.WaarpThreadFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This package would like to propose a JSE 6 compatible way to scan a directory
 * for new, deleted and changed
 * files, in order to allow some functions like pooling a directory before
 * actions.
 */
public class FileMonitor {
  /**
   * Internal Logger
   */
  protected static volatile WaarpLogger logger;
  protected static final DigestAlgo defaultDigestAlgo = DigestAlgo.MD5;
  protected static final long MINIMAL_DELAY = 100;
  protected static final long DEFAULT_DELAY = 1000;

  protected WaarpFuture future;
  protected WaarpFuture internalfuture;
  protected boolean stopped;
  protected final String name;
  protected final File statusFile;
  protected final File stopFile;
  protected final List<File> directories = new ArrayList<File>();
  protected final DigestAlgo digest;
  protected long elapseTime = DEFAULT_DELAY; // default to 1s
  protected long elapseWaarpTime = -1; // default set to run after each run
  protected Timer timer;
  protected Timer timerWaarp;
  // used only if elapseWaarpTime > defaultDelay (1s)
  protected final boolean scanSubDir;

  protected boolean initialized;
  protected File checkFile;

  protected final ConcurrentHashMap<String, FileItem> fileItems =
      new ConcurrentHashMap<String, FileItem>();
  protected final ConcurrentHashMap<String, FileItem> lastFileItems =
      new ConcurrentHashMap<String, FileItem>();

  protected FileFilter filter = new FileFilter() {
    @Override
    public boolean accept(File pathname) {
      return pathname.isFile();
    }
  };
  protected final FileMonitorCommandRunnableFuture commandValidFile;
  protected FileMonitorCommandFactory commandValidFileFactory;
  protected ExecutorService executor;
  protected int fixedThreadPool;
  protected final FileMonitorCommandRunnableFuture commandRemovedFile;
  protected FileMonitorCommandRunnableFuture commandCheckIteration;

  protected final ConcurrentLinkedQueue<FileItem> toUse =
      new ConcurrentLinkedQueue<FileItem>();
  protected final ConcurrentLinkedQueue<Future<?>> results =
      new ConcurrentLinkedQueue<Future<?>>();

  protected final AtomicLong globalok = new AtomicLong(0);
  protected final AtomicLong globalerror = new AtomicLong(0);
  protected final AtomicLong todayok = new AtomicLong(0);
  protected final AtomicLong todayerror = new AtomicLong(0);
  protected Date nextDay;

  /**
   * @param name name of this daemon
   * @param statusFile the file where the current status is saved
   *     (current
   *     files)
   * @param stopFile the file when created (.exists()) will stop the
   *     daemon
   * @param directory the directory where files will be monitored
   * @param digest the digest to use (default if null is MD5)
   * @param elapseTime the time to wait in ms for between 2 checks
   *     (default
   *     is 1000ms, minimum is
   *     100ms)
   * @param filter the filter to be applied on selected files (default
   *     is
   *     isFile())
   * @param commandValidFile the commandValidFile to run (may be null,
   *     which
   *     means poll() commandValidFile
   *     has to be used)
   * @param commandRemovedFile the commandRemovedFile to run (may be
   *     null)
   * @param commandCheckIteration the commandCheckIteration to run
   *     (may be
   *     null), runs after each check
   *     (elapseTime)
   */
  public FileMonitor(String name, File statusFile, File stopFile,
                     File directory, DigestAlgo digest, long elapseTime,
                     FileFilter filter, boolean scanSubdir,
                     FileMonitorCommandRunnableFuture commandValidFile,
                     FileMonitorCommandRunnableFuture commandRemovedFile,
                     FileMonitorCommandRunnableFuture commandCheckIteration) {
    if (logger == null) {
      logger = WaarpLoggerFactory.getLogger(FileMonitor.class);
    }
    this.name = name;
    this.statusFile = statusFile;
    this.stopFile = stopFile;
    directories.add(directory);
    scanSubDir = scanSubdir;
    if (digest == null) {
      this.digest = defaultDigestAlgo;
    } else {
      this.digest = digest;
    }
    if (elapseTime >= MINIMAL_DELAY) {
      this.elapseTime = (elapseTime / 10) * 10;
    }
    if (filter != null) {
      this.filter = filter;
    }
    this.commandValidFile = commandValidFile;
    this.commandRemovedFile = commandRemovedFile;
    this.commandCheckIteration = commandCheckIteration;
    if (statusFile != null) {
      checkFile = new File(statusFile.getAbsolutePath() + ".chk");
    }
    reloadStatus();
    setNextDay();
  }

  protected void setNextDay() {
    final Calendar c = new GregorianCalendar();
    c.set(Calendar.HOUR_OF_DAY, 0);
    c.set(Calendar.MINUTE, 0);
    c.set(Calendar.SECOND, 0);
    c.set(Calendar.MILLISECOND, 0);
    c.add(Calendar.DAY_OF_MONTH, 1);
    nextDay = c.getTime();
  }

  /**
   * @param commandCheckIteration the commandCheckIteration to run
   *     (may be
   *     null), runs after each check
   *     (elapseTime)
   */
  public void setCommandCheckIteration(
      FileMonitorCommandRunnableFuture commandCheckIteration) {
    this.commandCheckIteration = commandCheckIteration;
  }

  /**
   * @param factory the factory to used instead of simple instance
   *     (enables
   *     parallelism)
   * @param fixedPool if > 0, set the number of parallel threads
   *     allowed
   */
  public void setCommandValidFileFactory(FileMonitorCommandFactory factory,
                                         int fixedPool) {
    commandValidFileFactory = factory;
    fixedThreadPool = fixedPool;
  }

  /**
   * @return the elapseWaarpTime
   */
  public long getElapseWaarpTime() {
    return elapseWaarpTime;
  }

  /**
   * if set greater than 1000 ms, will be parallel, else will be sequential
   * after each check and ignoring this
   * timer
   *
   * @param elapseWaarpTime the elapseWaarpTime to set
   */
  public void setElapseWaarpTime(long elapseWaarpTime) {
    if (elapseWaarpTime >= DEFAULT_DELAY) {
      this.elapseWaarpTime = (elapseWaarpTime / 10) * 10;
    }
  }

  /**
   * Add a directory to scan
   *
   * @param directory
   */
  public void addDirectory(File directory) {
    synchronized (directories) {
      if (!directories.contains(directory)) {
        directories.add(directory);
      }
    }
  }

  /**
   * Add a directory to scan
   *
   * @param directory
   */
  public void removeDirectory(File directory) {
    synchronized (directories) {
      directories.remove(directory);
    }
  }

  protected void setThreadName() {
    Thread.currentThread().setName("FileMonitor_" + name);
  }

  private boolean testChkFile() {
    if (checkFile.exists()) {
      deleteChkFile();
      final long time = elapseTime * 10;
      logger.warn(
          "Waiting to check if another Monitor is running with the same configuration: " +
          time / 1000 + 's');
      try {
        Thread.sleep(time);
      } catch (final InterruptedException e) {//NOSONAR
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
      }
      return checkFile.exists();
    }
    return false;
  }

  private void createChkFile() {
    try {
      checkFile.createNewFile();
    } catch (final IOException ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
    }
  }

  private void deleteChkFile() {
    checkFile.delete();
  }

  protected void reloadStatus() {
    if (statusFile == null) {
      return;
    }
    if (!statusFile.exists()) {
      initialized = true;
      return;
    }
    if (testChkFile()) {
      // error ! one other monitor is running using the same status file
      logger.warn(
          "Error: One other monitor is probably running using the same status file: " +
          statusFile);
      return;
    }
    try {
      final HashMap<String, FileItem> newHashMap = JsonHandler.mapper
          .readValue(statusFile,
                     new TypeReference<HashMap<String, FileItem>>() {
                     });
      fileItems.putAll(newHashMap);
      initialized = true;
    } catch (final JsonParseException ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
    } catch (final JsonMappingException ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
    } catch (final IOException ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
    }
  }

  /**
   * @return True if the FileMonitor is correctly initialized
   */
  public boolean initialized() {
    return initialized;
  }

  protected void saveStatus() {
    if (statusFile == null) {
      return;
    }
    try {
      JsonHandler.mapper.writeValue(statusFile, fileItems);
      createChkFile();
    } catch (final JsonGenerationException ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
    } catch (final JsonMappingException ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
    } catch (final IOException ignored) {
      SysErrLogger.FAKE_LOGGER.ignoreLog(ignored);
    }
  }

  /**
   * @return the number of fileItems in the current history (active, in error
   *     or
   *     past)
   */
  public long getCurrentHistoryNb() {
    if (fileItems != null) {
      return fileItems.size();
    }
    return -1;
  }

  /**
   * Reset such that next status will be full (not partial)
   */
  public void setNextAsFullStatus() {
    lastFileItems.clear();
  }

  /**
   * @return the status (updated only) in JSON format
   */
  public String getStatus() {
    Set<String> removedFileItems = null;
    final ConcurrentHashMap<String, FileItem> newFileItems =
        new ConcurrentHashMap<String, FileItem>();
    if (!lastFileItems.isEmpty()) {
      removedFileItems = ((Map<String, FileItem>) lastFileItems).keySet();
      removedFileItems.removeAll(((Map<String, FileItem>) fileItems).keySet());
      for (final Entry<String, FileItem> key : fileItems.entrySet()) {
        if (!key.getValue().isStrictlySame(lastFileItems.get(key.getKey()))) {
          newFileItems.put(key.getKey(), key.getValue());
        }
      }
    } else {
      for (final Entry<String, FileItem> key : fileItems.entrySet()) {
        newFileItems.put(key.getKey(), key.getValue());
      }
    }
    final FileMonitorInformation fileMonitorInformation =
        new FileMonitorInformation(name, newFileItems, removedFileItems,
                                   directories, stopFile, statusFile,
                                   elapseTime, scanSubDir, globalok,
                                   globalerror, todayok, todayerror);
    for (final Entry<String, FileItem> key : fileItems.entrySet()) {
      final FileItem clone = key.getValue().clone();
      lastFileItems.put(key.getKey(), clone);
    }
    createChkFile();
    final String status = JsonHandler.writeAsString(fileMonitorInformation);
    if (removedFileItems != null) {
      removedFileItems.clear();
    }
    newFileItems.clear();
    return status;
  }

  /**
   * @return the elapseTime
   */
  public long getElapseTime() {
    return elapseTime;
  }

  /**
   * @param elapseTime the elapseTime to set
   */
  public void setElapseTime(long elapseTime) {
    this.elapseTime = elapseTime;
  }

  /**
   * @param filter the filter to set
   */
  public void setFilter(FileFilter filter) {
    this.filter = filter;
  }

  public void start() {
    if (timer == null) {
      timer = new HashedWheelTimer(
          new WaarpThreadFactory("TimerFileMonitor_" + name), 100,
          TimeUnit.MILLISECONDS, 8);
      future = new WaarpFuture(true);
      internalfuture = new WaarpFuture(true);
      if (commandValidFileFactory != null && executor == null) {
        if (fixedThreadPool > 1) {
          executor = Executors.newFixedThreadPool(fixedThreadPool,
                                                  new WaarpThreadFactory(
                                                      "FileMonitorRunner_" +
                                                      name));
        } else if (fixedThreadPool == 0) {
          executor = Executors.newCachedThreadPool(
              new WaarpThreadFactory("FileMonitorRunner_" + name));
        }
      }
      timer.newTimeout(new FileMonitorTimerTask(this), elapseTime,
                       TimeUnit.MILLISECONDS);
    } // else already started
    if (elapseWaarpTime >= DEFAULT_DELAY && timerWaarp == null &&
        commandCheckIteration != null) {
      timerWaarp = new HashedWheelTimer(
          new WaarpThreadFactory("TimerFileMonitorWaarp_" + name), 100,
          TimeUnit.MILLISECONDS, 8);
      timerWaarp.newTimeout(
          new FileMonitorTimerInformationTask(commandCheckIteration),
          elapseWaarpTime, TimeUnit.MILLISECONDS);
    }
  }

  public void stop() {
    initialized = false;
    stopped = true;
    if (timerWaarp != null) {
      timerWaarp.stop();
    }
    if (internalfuture != null) {
      internalfuture.awaitOrInterruptible(elapseTime * 2);
      internalfuture.setSuccess();
    }
    if (timer != null) {
      timer.stop();
    }
    timer = null;
    timerWaarp = null;
    if (executor != null) {
      executor.shutdown();
      executor = null;
    }
    deleteChkFile();
    if (future != null) {
      future.setSuccess();
    }
  }

  /**
   * @return the head of the File queue but does not remove it
   */
  public File peek() {
    final FileItem item = toUse.peek();
    if (item == null) {
      return null;
    }
    return item.file;
  }

  /**
   * @return the head of the File queue and removes it
   */
  public File poll() {
    final FileItem item = toUse.poll();
    if (item == null) {
      return null;
    }
    return item.file;
  }

  /**
   * Wait until the Stop file is created
   */
  public void waitForStopFile() {
    internalfuture.awaitOrInterruptible();
    stop();
  }

  private boolean checkStop() {
    if (stopped || stopFile.exists()) {
      logger.warn(
          "STOPPING the FileMonitor {} since condition is fullfilled: stop file found ({}): " +
          stopFile.exists(), name, stopFile);
      internalfuture.setSuccess();
      return true;
    }
    return false;
  }

  /**
   * Check Files
   *
   * @return False to stop
   */
  protected boolean checkFiles() {
    setThreadName();
    boolean fileItemsChanged = false;
    if (checkStop()) {
      return false;
    }
    synchronized (directories) {
      for (final File directory : directories) {
        logger.info("Scan: " + directory);
        fileItemsChanged = checkOneDir(fileItemsChanged, directory);
      }
    }
    setThreadName();
    boolean error = false;
    // Wait for all commands to finish before continuing
    for (final Future<?> futureResult : results) {
      createChkFile();
      try {
        futureResult.get();
      } catch (final InterruptedException e) {//NOSONAR
        SysErrLogger.FAKE_LOGGER.ignoreLog(e);
        logger.info("Interruption so exit");
        error = true;
      } catch (final ExecutionException e) {
        logger.error("Exception during execution", e);
        error = true;
      } catch (final Throwable e) {
        logger.error("Exception during execution", e);
        error = true;
      }
    }
    logger.debug("Scan over");
    results.clear();
    if (error) {
      // do not save ?
      // this.saveStatus()
      return false;
    }
    // now check that all existing items are still valid
    final List<FileItem> todel = new LinkedList<FileItem>();
    for (final FileItem item : fileItems.values()) {
      if (item.file != null && item.file.isFile()) {
        continue;
      }
      todel.add(item);
    }
    // remove invalid files
    for (FileItem fileItem : todel) {
      final String newName =
          AbstractDir.normalizePath(fileItem.file.getAbsolutePath());
      fileItems.remove(newName);
      toUse.remove(fileItem);
      if (commandRemovedFile != null) {
        commandRemovedFile.run(fileItem);
      }
      fileItem.file = null;
      fileItem.hash = null;
      fileItemsChanged = true;
    }
    if (fileItemsChanged) {
      saveStatus();
    } else {
      createChkFile();
    }
    if (checkStop()) {
      return false;
    }
    logger.debug("Finishing step");

    if (commandCheckIteration != null && timerWaarp == null) {
      commandCheckIteration.run(null);
    }
    return true;
  }

  /**
   * @param fileItemsChanged
   * @param directory
   *
   * @return True if one file at least has changed
   */
  protected boolean checkOneDir(boolean fileItemsChanged, File directory) {
    try {
      File[] files = directory.listFiles(filter);
      for (final File file : files) {
        if (checkStop()) {
          return false;
        }
        if (file.isDirectory()) {
          continue;
        }
        final String newName =
            AbstractDir.normalizePath(file.getAbsolutePath());
        final FileItem fileItem = fileItems.get(newName);
        if (fileItem == null) {
          // never seen until now
          fileItems.put(newName, new FileItem(file));
          fileItemsChanged = true;
          continue;
        }
        if (fileItem.used) {
          // already used so ignore
          continue;
        }
        final long lastTimeModified = fileItem.file.lastModified();
        if (lastTimeModified != fileItem.lastTime) {
          // changed or second time check
          fileItem.lastTime = lastTimeModified;
          fileItemsChanged = true;
          continue;
        }
        // now check Hash or third time
        try {
          final byte[] hash =
              FilesystemBasedDigest.getHash(fileItem.file, true, digest);
          if (hash == null || fileItem.hash == null) {
            fileItem.hash = hash;
            fileItemsChanged = true;
            continue;
          }
          if (!Arrays.equals(hash, fileItem.hash)) {
            fileItem.hash = hash;
            fileItemsChanged = true;
            continue;
          }
          if (checkStop()) {
            return false;
          }
          // now time and hash are the same so act on it
          fileItem.timeUsed = System.currentTimeMillis();
          if (commandValidFileFactory != null) {
            final FileMonitorCommandRunnableFuture torun =
                commandValidFileFactory.create(fileItem);
            if (executor != null) {
              final Future<?> torunFuture = executor.submit(torun);
              results.add(torunFuture);
            } else {
              torun.run(fileItem);
            }
          } else if (commandValidFile != null) {
            commandValidFile.run(fileItem);
          } else {
            toUse.add(fileItem);
          }
          fileItemsChanged = true;
        } catch (final Throwable e) {
          setThreadName();
          logger.error("Error during final file check", e);
          continue;
        }
      }
      if (scanSubDir) {
        files = directory.listFiles();
        for (final File file : files) {
          if (checkStop()) {
            return false;
          }
          if (file.isDirectory()) {
            fileItemsChanged = checkOneDir(fileItemsChanged, file);
          }
        }
      }
    } catch (final Throwable e) {
      setThreadName();
      logger.error("Issue during Directory and File Checking", e);
      // ignore
    }
    return fileItemsChanged;
  }

  /**
   * Timer task
   */
  protected static class FileMonitorTimerTask implements TimerTask {
    protected final FileMonitor fileMonitor;

    /**
     * @param fileMonitor
     */
    protected FileMonitorTimerTask(FileMonitor fileMonitor) {
      this.fileMonitor = fileMonitor;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
      try {
        if (fileMonitor.checkFiles()) {
          fileMonitor.setThreadName();
          if (fileMonitor.timer != null) {
            try {
              fileMonitor.timer.newTimeout(this, fileMonitor.elapseTime,
                                           TimeUnit.MILLISECONDS);
            } catch (final Throwable e) {
              logger.error("Error while pushing next filemonitor step", e);
              // ignore and stop
              fileMonitor.internalfuture.setSuccess();
            }
          } else {
            logger.warn("No Timer found");
            fileMonitor.internalfuture.setSuccess();
          }
        } else {
          fileMonitor.setThreadName();
          logger.warn("Stop file found");
          fileMonitor.deleteChkFile();
          fileMonitor.internalfuture.setSuccess();
        }
      } catch (final Throwable e) {
        fileMonitor.setThreadName();
        logger.error("Issue during Directory and File Checking", e);
        fileMonitor.internalfuture.setSuccess();
      }
    }

  }

  /**
   * Class to run Waarp Business information in fixed delay rather than after
   * each check
   */
  protected class FileMonitorTimerInformationTask implements TimerTask {
    protected final FileMonitorCommandRunnableFuture informationMonitorCommand;

    /**
     * @param informationMonitorCommand
     */
    protected FileMonitorTimerInformationTask(
        FileMonitorCommandRunnableFuture informationMonitorCommand) {
      this.informationMonitorCommand = informationMonitorCommand;
    }

    @Override
    public void run(Timeout timeout) throws Exception {
      try {
        Thread.currentThread().setName("FileMonitorInformation_" + name);
        if (!checkStop()) {
          informationMonitorCommand.run(null);
          if (timerWaarp != null && !checkStop()) {
            try {
              timerWaarp
                  .newTimeout(this, elapseWaarpTime, TimeUnit.MILLISECONDS);
            } catch (final Throwable e) {
              // stop and ignore
              logger.error("Error during nex filemonitor information step", e);
              internalfuture.setSuccess();
            }
          } else {
            if (timerWaarp != null) {
              logger.warn("Stop file found");
            } else {
              logger.warn("No Timer found");
            }
            internalfuture.setSuccess();
          }
        } else {
          logger.warn("Stop file found");
          internalfuture.setSuccess();
        }
      } catch (final Throwable e) {
        // stop and ignore
        Thread.currentThread().setName("FileMonitorInformation_" + name);
        logger.error("Error during nex filemonitor information step", e);
        internalfuture.setSuccess();
      }
    }
  }

  /**
   * Used by Waarp Business information
   */
  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
  public static class FileMonitorInformation {
    public String name;
    public ConcurrentHashMap<String, FileItem> fileItems;
    public Set<String> removedFileItems;
    public List<File> directories;
    public File stopFile;
    public File statusFile;
    public long elapseTime;
    public boolean scanSubDir;
    public AtomicLong globalok;
    public AtomicLong globalerror;
    public AtomicLong todayok;
    public AtomicLong todayerror;

    public FileMonitorInformation() {
      // empty constructor for JSON
    }

    protected FileMonitorInformation(String name,
                                     ConcurrentHashMap<String, FileItem> fileItems,
                                     Set<String> removedFileItems,
                                     List<File> directories, File stopFile,
                                     File statusFile, long elapseTime,
                                     boolean scanSubDir, AtomicLong globalok,
                                     AtomicLong globalerror, AtomicLong todayok,
                                     AtomicLong todayerror) {
      this.name = name;
      this.fileItems = fileItems;
      this.removedFileItems = removedFileItems;
      this.directories = directories;
      this.stopFile = stopFile;
      this.statusFile = statusFile;
      this.elapseTime = elapseTime;
      this.scanSubDir = scanSubDir;
      this.globalok = globalok;
      this.globalerror = globalerror;
      this.todayok = todayok;
      this.todayerror = todayerror;
    }

  }

  /**
   * One element in the directory
   */
  public static class FileItem implements Cloneable {
    public File file;
    public byte[] hash;
    public long lastTime = Long.MIN_VALUE;
    public long timeUsed = Long.MIN_VALUE;
    public boolean used;
    public long specialId = DbConstant.ILLEGALVALUE;

    public FileItem() {
      // empty constructor for JSON
    }

    /**
     * @param file
     */
    protected FileItem(File file) {
      this.file = file;
    }

    @Override
    public int hashCode() {
      return file.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      // equality is based on file itself
      return obj instanceof FileItem && file.equals(((FileItem) obj).file);
    }

    /**
     * @param item
     *
     * @return True if the fileItem is strictly the same (and not only the
     *     file
     *     as in equals)
     */
    public boolean isStrictlySame(FileItem item) {
      return item != null && file.equals(item.file) &&
             lastTime == item.lastTime && timeUsed == item.timeUsed &&
             used == item.used &&
             (hash != null? Arrays.equals(hash, item.hash) : item.hash == null);
    }

    @Override
    public FileItem clone() { //NOSONAR
      final FileItem clone = new FileItem(file);
      clone.hash = hash;
      clone.lastTime = lastTime;
      clone.timeUsed = timeUsed;
      clone.used = used;
      clone.specialId = specialId;
      return clone;
    }
  }

  public static void main(String[] args) {
    if (args.length < 3) {
      SysErrLogger.FAKE_LOGGER
          .syserr("Need a statusfile, a stopfile and a directory to test");
      return;
    }
    final File file = new File(args[0]);
    if (file.exists() && !file.isFile()) {
      SysErrLogger.FAKE_LOGGER.syserr("Not a correct status file");
      return;
    }
    final File stopfile = new File(args[1]);
    if (file.exists() && !file.isFile()) {
      SysErrLogger.FAKE_LOGGER.syserr("Not a correct stop file");
      return;
    }
    final File dir = new File(args[2]);
    if (!dir.isDirectory()) {
      SysErrLogger.FAKE_LOGGER.syserr("Not a directory");
      return;
    }
    final FileMonitorCommandRunnableFuture filemonitor =
        new FileMonitorCommandRunnableFuture() {
          @Override
          public void run(FileItem file) {
            SysErrLogger.FAKE_LOGGER
                .syserr("File New: " + file.file.getAbsolutePath());
            finalizeValidFile(true, 0);
          }
        };
    final FileMonitor monitor =
        new FileMonitor("test", file, stopfile, dir, null, 0,
                        new RegexFileFilter(
                            RegexFileFilter.REGEX_XML_EXTENSION), false,
                        filemonitor, new FileMonitorCommandRunnableFuture() {
          @Override
          public void run(FileItem file) {
            SysErrLogger.FAKE_LOGGER
                .syserr("File Del: " + file.file.getAbsolutePath());
          }
        }, new FileMonitorCommandRunnableFuture() {
          @Override
          public void run(FileItem unused) {
            SysErrLogger.FAKE_LOGGER.syserr("Check done");
          }
        });
    filemonitor.setMonitor(monitor);
    monitor.start();
    monitor.waitForStopFile();
  }
}
