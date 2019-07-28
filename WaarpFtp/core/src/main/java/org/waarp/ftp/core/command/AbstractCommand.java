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
import org.waarp.ftp.core.config.FtpConfiguration;
import org.waarp.ftp.core.session.FtpSession;

import java.util.regex.Pattern;

/**
 * Abstract definition of an FTP Command
 */
public abstract class AbstractCommand implements CommandInterface {
  private static final Pattern COMPILE_BLANK = Pattern.compile(" ");
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
  public void setArgs(SessionInterface session, String command, String arg,
                      @SuppressWarnings("rawtypes") Enum code) {
    this.session = (FtpSession) session;
    this.command = command;
    this.arg = arg;
    this.code = (FtpCommandCode) code;
  }

  @Override
  public void setExtraNextCommand(
      @SuppressWarnings("rawtypes") Enum extraNextCommand) {
    if (extraNextCommand != FtpCommandCode.NOOP) {
      this.extraNextCommand = (FtpCommandCode) extraNextCommand;
    } else {
      this.extraNextCommand = null;
    }
  }

  @Override
  public boolean isNextCommandValid(CommandInterface newCommandArg) {
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
  public Object getObject() {
    return object;
  }

  @Override
  public void setObject(Object object) {
    this.object = object;
  }

  @Override
  public String getArg() {
    return arg;
  }

  @Override
  public String[] getArgs() {
    return COMPILE_BLANK.split(arg);
  }

  @Override
  public int getValue(String argx) throws InvalidArgumentException {
    int i;
    try {
      i = Integer.parseInt(argx);
    } catch (final NumberFormatException e) {
      throw new InvalidArgumentException("Not an integer", e);
    }
    return i;
  }

  @Override
  public String getCommand() {
    return command;
  }

  @Override
  public boolean hasArg() {
    return arg != null && arg.length() != 0;
  }

  /**
   * @return the current FtpSession
   */
  @Override
  public FtpSession getSession() {
    return session;
  }

  // some helpful functions

  /**
   * @return The current configuration object
   */
  public FtpConfiguration getConfiguration() {
    return session.getConfiguration();
  }

  @Override
  public void invalidCurrentCommand() {
    session.getRestart().setSet(false);
    session.setPreviousAsCurrentCommand();
  }

  /**
   * @return The FtpCommandCode associated with this command
   */
  @Override
  public FtpCommandCode getCode() {
    return code;
  }
}
