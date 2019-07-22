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
package org.waarp.gateway.kernel.commonfile;

import org.waarp.common.command.exception.CommandAbstractException;
import org.waarp.common.file.FileInterface;
import org.waarp.common.file.OptsMLSxInterface;
import org.waarp.common.file.SessionInterface;
import org.waarp.common.file.filesystembased.FilesystemBasedDirImpl;

/**
 *
 */
public class CommonDirImpl extends FilesystemBasedDirImpl {

  /**
   * @param session
   * @param optsMLSx
   */
  public CommonDirImpl(SessionInterface session, OptsMLSxInterface optsMLSx) {
    super(session, optsMLSx);
  }

  @Override
  public FileInterface newFile(String path, boolean append)
      throws CommandAbstractException {
    return new CommonFileImpl(session, this, path);
  }

}
