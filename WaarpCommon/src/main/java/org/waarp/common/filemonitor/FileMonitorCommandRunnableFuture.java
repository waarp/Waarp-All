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

import org.waarp.common.filemonitor.FileMonitor.FileItem;
import org.waarp.common.filemonitor.FileMonitor.Status;

import java.util.Date;

import static org.waarp.common.database.DbConstant.*;

/**
 * Command run when a new file item is validated
 */
public abstract class FileMonitorCommandRunnableFuture implements Runnable {
  private FileItem fileItem;
  private Thread currentThread;
  private FileMonitor monitor;

  /**
   *
   */
  protected FileMonitorCommandRunnableFuture() {
  }

  public void setMonitor(FileMonitor monitor) {
    this.monitor = monitor;
  }

  /**
   * @param fileItem
   */
  protected FileMonitorCommandRunnableFuture(FileItem fileItem) {
    setFileItem(fileItem);
  }

  public void setFileItem(FileItem fileItem) {
    this.fileItem = fileItem;
  }

  @Override
  public void run() {
    currentThread = Thread.currentThread();
    if (getFileItem() != null) {
      run(getFileItem());
    }
  }

  /**
   * @param fileItem fileItem on which the command will be executed.
   */
  public abstract void run(FileItem fileItem);

  /**
   * To be called at the beginning of the primary action (only for
   * commandValidFile).
   *
   * @param ignoreAlreadyUsed
   */
  protected void checkReuse(boolean ignoreAlreadyUsed) {
    if (!ignoreAlreadyUsed && fileItem.used &&
        fileItem.specialId != ILLEGALVALUE && fileItem.status == Status.RESTART) {
      fileItem.used = false;
    }
  }

  protected boolean isReuse() {
    return fileItem.status == Status.RESTART && !fileItem.used &&
           fileItem.specialId != ILLEGALVALUE;
  }

  protected boolean isIgnored(boolean ignoreAlreadyUsed) {
    return !ignoreAlreadyUsed && fileItem.used;
  }

  protected void setValid(FileItem fileItem) {
    fileItem.specialId = ILLEGALVALUE;
    fileItem.used = false;
    fileItem.status = Status.VALID;
  }

  /**
   * To be called at the end of the primary action (only for
   * commandValidFile).
   *
   * @param status
   * @param specialId the specialId associated with the task
   */
  protected void finalizeValidFile(boolean status, long specialId) {
    if (getMonitor() != null) {
      final Date date = new Date();
      if (date.after(getMonitor().nextDay)) {
        // midnight is after last check
        getMonitor().setNextDay();
        getMonitor().todayok.set(0);
        getMonitor().todayerror.set(0);
      }
    }
    if (status) {
      getFileItem().used = true;
      getFileItem().status = Status.DONE;
      // Keep the hash: fileItem.hash = null
      getFileItem().specialId = specialId;
      if (getMonitor() != null) {
        getMonitor().globalok.incrementAndGet();
        getMonitor().todayok.incrementAndGet();
      }
    } else {
      // execution in error, will retry later on
      getFileItem().used = false;
      getFileItem().status = Status.VALID;
      getFileItem().hash = null;
      getFileItem().specialId = specialId;
      if (getMonitor() != null) {
        getMonitor().globalerror.incrementAndGet();
        getMonitor().todayerror.incrementAndGet();
      }
    }
  }

  public void cancel() {
    if (currentThread != null) {
      currentThread.interrupt();
    }
  }

  /**
   * @return the fileItem
   */
  public FileItem getFileItem() {
    return fileItem;
  }

  /**
   * @return the monitor
   */
  public FileMonitor getMonitor() {
    return monitor;
  }
}
