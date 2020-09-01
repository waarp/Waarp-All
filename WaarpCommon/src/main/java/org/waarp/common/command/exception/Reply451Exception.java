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
package org.waarp.common.command.exception;

import org.waarp.common.command.ReplyCode;

/**
 * 451 Requested action aborted: local error in processing.
 */
public class Reply451Exception extends CommandAbstractException {

  /**
   * serialVersionUID of long:
   */
  private static final long serialVersionUID = 451L;

  /**
   * 451 Requested action aborted: local error in processing.
   *
   * @param message
   */
  public Reply451Exception(final String message) {
    super(ReplyCode.REPLY_451_REQUESTED_ACTION_ABORTED, message);
  }

}
