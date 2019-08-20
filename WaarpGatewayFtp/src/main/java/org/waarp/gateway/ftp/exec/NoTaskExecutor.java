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

package org.waarp.gateway.ftp.exec;

import org.waarp.common.command.exception.Reply421Exception;
import org.waarp.common.future.WaarpFuture;

/**
 * NoTaskExecutor class. No action will be taken.
 */
public class NoTaskExecutor extends AbstractExecutor {
  private final WaarpFuture futureCompletion;

  /**
   * @param command
   * @param delay
   * @param futureCompletion
   */
  public NoTaskExecutor(String command, long delay,
                        WaarpFuture futureCompletion) {
    this.futureCompletion = futureCompletion;
  }

  @Override
  public void run() throws Reply421Exception {
    futureCompletion.setSuccess();
  }
}
