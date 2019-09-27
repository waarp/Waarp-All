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
package org.waarp.common.future;

import org.waarp.common.logging.SysErrLogger;

import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.*;

/**
 * Ftp Future operation<br>
 * Completely inspired from the excellent ChannelFuture of Netty, but without
 * any channel inside.
 */
public class WaarpFuture {
  private static final Throwable CANCELLED = new Throwable();

  private final boolean cancellable;

  private volatile boolean done;

  private volatile Throwable cause;

  private int waiters;

  /**
   * Creates a new instance.
   */
  public WaarpFuture() {
    cancellable = false;
  }

  /**
   * Creates a new instance.
   *
   * @param cancellable {@code true} if and only if this future can be
   *     canceled
   */
  public WaarpFuture(boolean cancellable) {
    this.cancellable = cancellable;
  }

  /**
   * Returns {@code true} if and only if this future is complete, regardless
   * of
   * whether the operation was
   * successful, failed, or canceled.
   *
   * @return True if the future is complete
   */
  public boolean isDone() {
    return done;
  }

  /**
   * Returns {@code true} if and only if the operation was completed
   * successfully.
   *
   * @return True if the future is successful
   */
  public boolean isSuccess() {
    return done && cause == null;
  }

  /**
   * Returns {@code true} if and only if the operation was completed but
   * unsuccessfully.
   *
   * @return True if the future is done but unsuccessful
   */
  public boolean isFailed() {
    return cause != null;
  }

  /**
   * Returns the cause of the failed operation if the operation has failed.
   *
   * @return the cause of the failure. {@code null} if succeeded or this
   *     future
   *     is not completed yet.
   */
  public Throwable getCause() {
    if (cause != CANCELLED) {
      return cause;
    }
    return null;
  }

  /**
   * Returns {@code true} if and only if this future was canceled by a {@link
   * #cancel()} method.
   *
   * @return True if the future was canceled
   */
  public boolean isCancelled() {
    return cause == CANCELLED;
  }

  /**
   * Rethrows the exception that caused this future fail if this future is
   * complete and failed.
   */
  public WaarpFuture rethrowIfFailed() throws Exception {
    if (!isDone()) {
      return this;
    }

    final Throwable causeNew = getCause();
    if (causeNew == null) {
      return this;
    }

    if (causeNew instanceof Exception) {
      throw (Exception) causeNew;
    }

    if (causeNew instanceof Error) {
      throw (Error) causeNew;
    }

    throw new RuntimeException(causeNew);
  }

  /**
   * @return True if the Future is done or False if interrupted
   */
  public boolean awaitOrInterruptible() {
    while (!Thread.interrupted()) {
      if (awaitOrInterruptible(1, SECONDS)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param timeoutMilliseconds
   *
   * @return True if the Future is done or False if interrupted
   */
  public boolean awaitOrInterruptible(long timeoutMilliseconds) {
    return awaitOrInterruptible(MILLISECONDS.toNanos(timeoutMilliseconds),
                                false);
  }

  /**
   * @param timeout
   * @param unit
   *
   * @return True if the Future is done or False if interrupted
   */
  public boolean awaitOrInterruptible(long timeout, TimeUnit unit) {
    return awaitOrInterruptible(unit.toNanos(timeout), false);
  }

  /**
   * @param timeoutNanos
   * @param interruptable
   *
   * @return True if the Future is done or False if interrupted
   */
  private boolean awaitOrInterruptible(long timeoutNanos,
                                       boolean interruptable) {
    try {
      if (await0(timeoutNanos, interruptable) && !Thread.interrupted()) {
        return true;
      }
    } catch (final InterruptedException e) {//NOSONAR
      SysErrLogger.FAKE_LOGGER.ignoreLog(e);
    }
    return false;
  }

  private boolean await0(long timeoutNanos, boolean interruptable)
      throws InterruptedException {
    if (done) {
      return done;
    }
    if (timeoutNanos <= 0) {
      return done;
    }
    if (interruptable && Thread.interrupted()) {
      throw new InterruptedException();
    }

    long startTime = System.nanoTime();
    long waitTime = timeoutNanos;
    boolean interrupted = false;

    try {
      for (; ; ) {
        synchronized (this) {
          if (done) {
            return done;
          }
          waiters++;

          try {
            wait(waitTime / 1000000, (int) (waitTime % 1000000));
          } catch (InterruptedException e) {//NOSONAR
            SysErrLogger.FAKE_LOGGER.ignoreLog(e);
            if (interruptable) {
              throw e;
            } else {
              interrupted = true;
            }
          } finally {
            waiters--;
          }
        }
        if (done) {
          return true;
        }
        waitTime = timeoutNanos - (System.nanoTime() - startTime);
        if (waitTime <= 0) {
          return done;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  /**
   * Marks this future as a success and notifies all listeners.
   *
   * @return {@code true} if and only if successfully marked this future as a
   *     success. Otherwise {@code false}
   *     because this future is already marked as either a success or a
   *     failure.
   */
  public boolean setSuccess() {
    synchronized (this) {
      // Allow only once.
      if (done) {
        return false;
      }

      done = true;
      if (waiters > 0) {
        notifyAll();
      }
    }
    return true;
  }

  /**
   * Marks this future as a failure and notifies all listeners.
   *
   * @param cause
   *
   * @return {@code true} if and only if successfully marked this future as a
   *     failure. Otherwise {@code false}
   *     because this future is already marked as either a success or a
   *     failure.
   */
  public boolean setFailure(Throwable cause) {
    synchronized (this) {
      // Allow only once.
      if (done) {
        return false;
      }

      this.cause = cause;
      done = true;
      if (waiters > 0) {
        notifyAll();
      }
    }
    return true;
  }

  /**
   * Cancels the operation associated with this future and notifies all
   * listeners if canceled successfully.
   *
   * @return {@code true} if and only if the operation has been canceled.
   *     {@code
   *     false} if the operation can't
   *     be canceled or is already completed.
   */
  public boolean cancel() {
    if (!cancellable) {
      return false;
    }
    synchronized (this) {
      // Allow only once.
      if (done) {
        return false;
      }

      cause = CANCELLED;
      done = true;
      if (waiters > 0) {
        notifyAll();
      }
    }
    return true;
  }

  /**
   * Experimental: try to re-enable the future
   */
  public void reset() {
    synchronized (this) {
      done = false;
      cause = null;
    }
  }
}
