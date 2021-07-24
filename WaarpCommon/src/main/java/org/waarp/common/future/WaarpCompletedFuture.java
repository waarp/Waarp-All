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
 */
public abstract class WaarpCompletedFuture implements WaarpFutureInterface {
  /**
   *
   */
  protected WaarpCompletedFuture() {
    // Empty
  }

  @Override
  public final boolean awaitOrInterruptible() {
    return !Thread.interrupted();
  }

  @Override
  public final boolean awaitOrInterruptible(final long timeoutMilliseconds) {
    return !Thread.interrupted();
  }

  @Override
  public final boolean awaitOrInterruptible(final long timeout,
                                            final TimeUnit unit) {
    return !Thread.interrupted();
  }

  @Override
  public final void reset() {
    // nothing
  }

  @Override
  public final boolean isDone() {
    return true;
  }

  @Override
  public final boolean setFailure(final Throwable cause) {
    return false;
  }

  @Override
  public final boolean setSuccess() {
    return false;
  }

  @Override
  public final boolean cancel() {
    return false;
  }

  @Override
  public final boolean isCancelled() {
    return false;
  }
}
