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

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.ftp.core.command.AbstractCommand;

import java.util.List;

/**
 * NLST command
 */
public class NLST extends AbstractCommand {
  @Override
  public void exec() throws CommandAbstractException {
    final String path;
    final List<String> files;
    if (!hasArg()) {
      path = getSession().getDir().getPwd();
      files = getSession().getDir().list(path);
    } else {
      path = getArg();
      if (path.startsWith("-l") || path.startsWith("-L")) {
        // This should be a LIST command
        final String[] paths = getArgs();
        if (paths.length > 1) {
          files = getSession().getDir().listFull(paths[1], true);
        } else {
          files = getSession().getDir()
                              .listFull(getSession().getDir().getPwd(), true);
        }
      } else {
        files = getSession().getDir().list(path);
      }
    }
    getSession().openDataConnection();
    getSession().getDataConn().getFtpTransferControl()
                .setNewFtpTransfer(getCode(), files, path);
  }

}
