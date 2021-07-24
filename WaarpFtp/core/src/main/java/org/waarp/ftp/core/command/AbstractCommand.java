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
package org.waarp.ftp.core.command;

import org.waarp.common.command.CommandInterface;
import org.waarp.common.exception.InvalidArgumentException;
import org.waarp.common.file.SessionInterface;
import org.waarp.common.utility.WaarpStringUtils;
import org.waarp.ftp.core.config.FtpConfiguration;
import org.waarp.ftp.core.session.FtpSession;

import java.util.regex.Pattern;

/**
 * Abstract definition of an FTP Command
 */
public abstract class AbstractCommand implements CommandInterface {
  private static final Pattern COMPILE_BLANK = WaarpStringUtils.BLANK;
  /**
   * Code of Command
   */
  private FtpCommandCode code;

  /**
   * String attached to the command
   */
  private String command;

  /**
   * Argument attached to this command
   */
  private String arg;

  /**
   * The Ftp SessionInterface
   */
  private FtpSession session;

  /**
   * Internal Object (whatever the used). This has to be clean by Business
   * Handler cleanSession.
   */
  private Object object;

  /**
   * Extra allowed nextCommand
   */
  private FtpCommandCode extraNextCommand;

  @Override
  public final void setArgs(final SessionInterface session,
                            final String command, final String arg,
                            @SuppressWarnings("rawtypes") final Enum code) {
    this.session = (FtpSession) session;
    this.command = command;
    this.arg = arg;
    this.code = (FtpCommandCode) code;
  }

  @Override
  public final void setExtraNextCommand(
      @SuppressWarnings("rawtypes") final Enum extraNextCommand) {
    if (extraNextCommand != FtpCommandCode.NOOP) {
      this.extraNextCommand = (FtpCommandCode) extraNextCommand;
    } else {
      this.extraNextCommand = null;
    }
  }

  @Override
  public final boolean isNextCommandValid(
      final CommandInterface newCommandArg) {
    final AbstractCommand newCommand = (AbstractCommand) newCommandArg;
    final Class<? extends AbstractCommand> newClass = newCommand.getClass();
    // Special commands: QUIT ABORT STAT NOP
    if (FtpCommandCode.isSpecialCommand(newCommand.getCode())) {
      return true;
    }
    if (code == null) {
      return false;
    }
    if (extraNextCommand != null) {
      if (extraNextCommand.command == newClass) {
        return true;
      }
      if (code.nextValids != null && code.nextValids.length > 0) {
        for (final Class<?> nextValid : code.nextValids) {
          if (nextValid == newClass) {
            return true;
          }
        }
      }
      return false;
    }
    if (code.nextValids == null || code.nextValids.length == 0) {
      // Any command is allowed
      return true;
    }
    for (final Class<?> nextValid : code.nextValids) {
      if (nextValid == newClass) {
        return true;
      }
    }
    return false;
  }

  @Override
  public final Object getObject() {
    return object;
  }

  @Override
  public final void setObject(final Object object) {
    this.object = object;
  }

  @Override
  public final String getArg() {
    return arg;
  }

  @Override
  public final String[] getArgs() {
    return COMPILE_BLANK.split(arg);
  }

  @Override
  public final int getValue(final String argx) throws InvalidArgumentException {
    final int i;
    try {
      i = Integer.parseInt(argx);
    } catch (final NumberFormatException e) {
      throw new InvalidArgumentException("Not an integer", e);
    }
    return i;
  }

  @Override
  public final String getCommand() {
    return command;
  }

  @Override
  public final boolean hasArg() {
    return arg != null && arg.length() != 0;
  }

  /**
   * @return the current FtpSession
   */
  @Override
  public final FtpSession getSession() {
    return session;
  }

  // some helpful functions

  /**
   * @return The current configuration object
   */
  public final FtpConfiguration getConfiguration() {
    return session.getConfiguration();
  }

  @Override
  public final void invalidCurrentCommand() {
    session.getRestart().setSet(false);
    session.setPreviousAsCurrentCommand();
  }

  /**
   * @return The FtpCommandCode associated with this command
   */
  @Override
  public final FtpCommandCode getCode() {
    return code;
  }
}
