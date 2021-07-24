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

/**
 * Future in failure
 */
public class WaarpFailedFuture extends WaarpCompletedFuture {

  private final Throwable cause;

  /**
   * Creates a new instance.
   *
   * @param cause the cause of failure
   */
  public WaarpFailedFuture(final Throwable cause) {
    if (cause == null) {
      throw new NullPointerException("cause");
    }
    this.cause = cause;
  }

  @Override
  public synchronized Throwable getCause() {
    return cause;
  }

  @Override
  public synchronized boolean isSuccess() {
    return false;
  }

  @Override
  public final boolean isFailed() {
    return true;
  }

  @Override
  public final WaarpFuture rethrowIfFailed() throws Exception {
    if (cause instanceof Exception) {
      throw (Exception) cause;
    }

    if (cause instanceof Error) {
      throw (Error) cause;
    }

    throw new RuntimeException(cause);
  }
}
