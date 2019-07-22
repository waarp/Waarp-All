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

import java.util.concurrent.TimeUnit;

/**
 * Future completed
 *
 *
 */
public abstract class WaarpCompletedFuture extends WaarpFuture {
  /**
   *
   */
  protected WaarpCompletedFuture() {
    super(false);
  }

  @Override
  public WaarpFuture await() throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    return this;
  }

  @Override
  public boolean await(long timeout, TimeUnit unit)
      throws InterruptedException {
    if (Thread.interrupted()) {
      throw new InterruptedException();
    }
    return true;
  }

  @Override
  public WaarpFuture awaitUninterruptibly() {
    return this;
  }

  @Override
  public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
    return true;
  }

  @Override
  public boolean awaitUninterruptibly(long timeoutMillis) {
    return true;
  }

  @Override
  public boolean awaitOrInterruptible() {
    return true;
  }

  @Override
  public boolean awaitOrInterruptible(final long timeoutMilliseconds) {
    return true;
  }

  @Override
  public boolean awaitOrInterruptible(final long timeout, final TimeUnit unit) {
    return true;
  }

  @Override
  public void reset() {
    // nothing
  }

  @Override
  public boolean isDone() {
    return true;
  }

  @Override
  public boolean setFailure(Throwable cause) {
    return false;
  }

  @Override
  public boolean setSuccess() {
    return false;
  }

  @Override
  public boolean cancel() {
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }
}
