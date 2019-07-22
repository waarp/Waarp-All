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
package org.waarp.common.file.passthrough;

import org.waarp.common.file.filesystembased.FilesystemBasedOptsMLSxImpl;

/**
 * Class that implements Opts command for MLSx operations. (-1) means not
 * supported, 0 supported but not
 * active, 1 supported and active
 *
 *
 */
public class PassthroughBasedOptsMLSxImpl extends FilesystemBasedOptsMLSxImpl {
  /**
   * Default empty constructor: no support at all of MLSx function
   */
  public PassthroughBasedOptsMLSxImpl() {
  }

  /**
   * (-1) means not supported, 0 supported but not active, 1 supported and
   * active
   *
   * @param optsSize
   * @param optsModify
   * @param optsType
   * @param optsPerm
   * @param optsCreate
   * @param optsUnique
   * @param optsLang
   * @param optsMediaType
   * @param optsCharset
   */
  public PassthroughBasedOptsMLSxImpl(byte optsSize, byte optsModify,
                                      byte optsType, byte optsPerm,
                                      byte optsCreate, byte optsUnique,
                                      byte optsLang, byte optsMediaType,
                                      byte optsCharset) {
  }
}
