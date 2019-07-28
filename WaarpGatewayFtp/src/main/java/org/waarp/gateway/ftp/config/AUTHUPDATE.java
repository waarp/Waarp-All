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
package org.waarp.gateway.ftp.config;

import org.waarp.common.command.ReplyCode;
import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply500Exception;
import org.waarp.common.command.exception.Reply501Exception;
import org.waarp.common.logging.WaarpLogger;
import org.waarp.common.logging.WaarpLoggerFactory;
import org.waarp.ftp.core.command.AbstractCommand;

import java.io.File;

/**
 * AUTHTUPDATE command: implements the command that will try to update the
 * authentications from the file given
 * as argument or the original one if no argument is given.<br>
 * Two optional arguments exist:<br>
 * - PURGE: empty first the current authentications before applying the
 * update<br>
 * - SAVE: save the final authentications on the original name given at
 * startup.<br>
 */
public class AUTHUPDATE extends AbstractCommand {
  /**
   * Internal Logger
   */
  private static final WaarpLogger logger =
      WaarpLoggerFactory.getLogger(AUTHUPDATE.class);

  @Override
  public void exec() throws CommandAbstractException {
    if (!getSession().getAuth().isAdmin()) {
      // not admin
      throw new Reply500Exception("Command Not Allowed");
    }
    String filename = null;
    boolean purge = false;
    boolean write = false;
    if (!hasArg()) {
      filename =
          ((FileBasedConfiguration) getConfiguration()).getAuthenticationFile();
    } else {
      final String[] authents = getArgs();
      for (final String authent : authents) {
        if ("PURGE".equalsIgnoreCase(authent)) {
          purge = true;
        } else if ("SAVE".equalsIgnoreCase(authent)) {
          write = true;
        } else if (filename == null) {
          filename = authent;
        }
      }
      if (filename == null) {
        filename = ((FileBasedConfiguration) getConfiguration())
            .getAuthenticationFile();
      }
      final File file = new File(filename);
      if (!file.canRead()) {
        throw new Reply501Exception(
            "Filename given as parameter is not found: " + filename);
      }
    }
    if (!((FileBasedConfiguration) getConfiguration())
        .initializeAuthent(filename, purge)) {
      throw new Reply501Exception("Filename given as parameter is not correct");
    }
    if (write && !((FileBasedConfiguration) getConfiguration())
        .saveAuthenticationFile(((FileBasedConfiguration) getConfiguration())
                                    .getAuthenticationFile())) {
      throw new Reply501Exception(
          "Update is done but Write operation is not correct");
    }
    logger.warn("Authentication was updated from " + filename);
    getSession().setReplyCode(ReplyCode.REPLY_200_COMMAND_OKAY,
                              "Authentication is updated");
  }

}
