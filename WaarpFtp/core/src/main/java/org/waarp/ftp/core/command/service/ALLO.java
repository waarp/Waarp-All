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
package org.waarp.ftp.core.command.service;

import org.waarp.common.command.ReplyCode;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply452Exception;
import org.waarp.common.command.exception.Reply501Exception;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.ftp.core.command.AbstractCommand;

/**
 * ALLO command: test if enough space is disponible
 */
public class ALLO extends AbstractCommand {
  @Override
  public final void exec() throws CommandAbstractException {
    // First Check if any argument
    if (!hasArg()) {
      throw new Reply501Exception("Need a size as argument");
    }
    final String[] args = getArgs();
    final int size;
    try {
      size = getValue(args[0]);
    } catch (final InvalidArgumentException e) {
      throw new Reply501Exception("Need a valid size as argument: " + args[0]);
    }
    final long free = getSession().getDir().getFreeSpace();
    if (free > 0 && free < size) {
      throw new Reply452Exception("Not enough space left");
    }
    if (free == -1) {
      getSession().setReplyCode(ReplyCode.REPLY_202_COMMAND_NOT_IMPLEMENTED,
                                null);
    } else {
      getSession().setReplyCode(ReplyCode.REPLY_200_COMMAND_OKAY,
                                "ALLO OK: " + free + " bytes available");
    }
  }

}
