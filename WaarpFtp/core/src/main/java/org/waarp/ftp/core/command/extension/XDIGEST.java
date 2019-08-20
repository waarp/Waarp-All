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
package org.waarp.ftp.core.command.extension;

import org.waarp.common.command.ReplyCode;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply501Exception;
import org.waarp.common.digest.FilesystemBasedDigest;
import org.waarp.ftp.core.command.AbstractCommand;

/**
 * XDIGEST command: takes an algorithme name (CRC32, ADLER32, MD5, MD2,
 * SHA-1, SHA-256, SHA-384, SHA-512) and a filename and returns the
 * digest of the file
 */
public class XDIGEST extends AbstractCommand {
  @Override
  public void exec() throws CommandAbstractException {
    if (!hasArg()) {
      invalidCurrentCommand();
      throw new Reply501Exception(
          "Need an algorihm and a pathname as argument");
    }
    final String[] args = getArgs();
    if (args == null || args.length < 2) {
      throw new Reply501Exception(
          "Need an algorihm and a pathname as argument");
    }
    final String crc = FilesystemBasedDigest
        .getHex(getSession().getDir().getDigest(args[1], args[0]));
    getSession().setReplyCode(ReplyCode.REPLY_250_REQUESTED_FILE_ACTION_OKAY,
                              crc + " \"" + args[1] + "\" " + args[0]);
  }

}
