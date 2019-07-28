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
package org.waarp.common.file;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply502Exception;
import org.waarp.common.command.exception.Reply530Exception;
import org.waarp.common.exception.NoRestartException;

import java.io.IOException;

/**
 *
 */
public abstract class AbstractFile implements FileInterface {
  /**
   * Is this Document ready to be accessed
   */
  protected boolean isReady;

  @Override
  public void clear() throws CommandAbstractException {
    closeFile();
    isReady = false;
  }

  @Override
  public void checkIdentify() throws Reply530Exception {
    if (!getSession().getAuth().isIdentified()) {
      throw new Reply530Exception("User not authentified");
    }
  }

  @Override
  public DataBlock getMarker() throws CommandAbstractException {
    throw new Reply502Exception("No marker implemented");
  }

  @Override
  public boolean restartMarker(Restart restart)
      throws CommandAbstractException {
    try {
      final long newposition = restart.getPosition();
      try {
        setPosition(newposition);
      } catch (final IOException e) {
        throw new Reply502Exception("Cannot set the marker position");
      }
      return true;
    } catch (final NoRestartException ignored) {
      // nothing
    }
    return false;
  }

  @Override
  public boolean retrieve() throws CommandAbstractException {
    checkIdentify();
    if (isReady) {
      restartMarker(getSession().getRestart());
      return canRead();
    }
    return false;
  }

  @Override
  public boolean store() throws CommandAbstractException {
    checkIdentify();
    if (isReady) {
      restartMarker(getSession().getRestart());
      return canWrite();
    }
    return false;
  }

}
