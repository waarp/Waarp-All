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
package org.waarp.openr66.context.filesystem;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.command.exception.Reply502Exception;
import org.waarp.common.file.filesystembased.FilesystemBasedRestartImpl;
import org.waarp.openr66.context.R66Session;

/**
 * Restart implementation
 */
public class R66Restart extends FilesystemBasedRestartImpl {

  /**
   * @param session
   */
  public R66Restart(final R66Session session) {
    super(session);
  }

  @Override
  public final boolean restartMarker(final String marker)
      throws CommandAbstractException {
    final long newposition;
    try {
      newposition = Long.parseLong(marker);
    } catch (final NumberFormatException e) {
      throw new Reply502Exception(
          "Marker must be length in byte as a position");
    }
    return restartMarker(newposition);
  }

  /**
   * Same as restartMarker with String
   *
   * @param newposition
   *
   * @return True if OK
   */
  public final boolean restartMarker(final long newposition) {
    position = newposition;
    setSet(true);
    return true;
  }
}
